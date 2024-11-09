package io.littlelanguages.bendu

import io.littlelanguages.bendu.compiler.Instructions
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertTrue

class CompilerTest {
    @Test
    fun `literal bool`() {
        assertCompiledBC(
            byteArrayOf(Instructions.PUSH_BOOL_TRUE.op),
            "True"
        )

        assertCompiledBC(
            byteArrayOf(Instructions.PUSH_BOOL_FALSE.op),
            "False"
        )
    }

    @Test
    fun `literal char`() {
        assertCompiledBC(
            byteArrayOf(
                Instructions.PUSH_U8_LITERAL.op,
                120 // 'x'
            ),
            "'x'"
        )
    }

    @Test
    fun `literal float`() {
        assertCompiledBC(
            byteArrayOf(
                Instructions.PUSH_F32_LITERAL.op,
                64, 73, 15, -38 // 3.1415926
            ),
            "3.1415926"
        )
    }

    @Test
    fun `literal int`() {
        assertCompiledBC(
            byteArrayOf(
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 1 // 1
            ),
            "1"
        )
    }

    @Test
    fun `let x = 1 let y = x`() {
        assertCompiledBC(
            byteArrayOf(
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 1, // 1
                Instructions.PUSH_STACK.op,
                0, 0, 0, 0 // x
            ),
            "let x = 1 ; let y = x"
        )
    }

    @Test
    fun `print( dotdotdot )`() {
        assertCompiledBC(
            byteArrayOf(),
            "print()"
        )

        assertCompiledBC(
            byteArrayOf(
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 1, // 1
                Instructions.PRINT_I32.op,
                Instructions.PUSH_BOOL_TRUE.op,
                Instructions.PRINT_BOOL.op,
                Instructions.PUSH_U8_LITERAL.op,
                120, // 'x'
                Instructions.PRINT_U8.op,
                Instructions.PUSH_F32_LITERAL.op,
                64, 73, 6, 37, // 3.141
                Instructions.PRINT_F32.op,
            ),
            "print(1, True, 'x', 3.141)"
        )
    }

    @Test
    fun `println( dotdotdot )`() {
        assertCompiledBC(
            byteArrayOf(Instructions.PRINTLN.op),
            "println()"
        )

        assertCompiledBC(
            byteArrayOf(
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 1, // 1
                Instructions.PRINT_I32.op,
                Instructions.PUSH_BOOL_FALSE.op,
                Instructions.PRINT_BOOL.op,
                Instructions.PUSH_U8_LITERAL.op,
                120, // 'x'
                Instructions.PRINT_U8.op,
                Instructions.PUSH_F32_LITERAL.op,
                64, 73, 6, 37, // 3.141
                Instructions.PRINT_F32.op,
                Instructions.PRINTLN.op
            ),
            "println(1, False, 'x', 3.141)"
        )
    }

    @Test
    fun `v op v`() {
        listOf(
            Pair("1 == 2", Instructions.EQ_I32),
            Pair("1 != 2", Instructions.NEQ_I32),
            Pair("1 < 2", Instructions.LT_I32),
            Pair("1 <= 2", Instructions.LE_I32),
            Pair("1 > 2", Instructions.GT_I32),
            Pair("1 >= 2", Instructions.GE_I32),
            Pair("1 + 2", Instructions.ADD_I32),
            Pair("1 - 2", Instructions.SUB_I32),
            Pair("1 * 2", Instructions.MUL_I32),
            Pair("1 / 2", Instructions.DIV_I32),
            Pair("1 % 2", Instructions.MOD_I32),
            Pair("1 ** 2", Instructions.POW_I32),
        ).forEach { input ->
            assertCompiledBC(
                byteArrayOf(
                    Instructions.PUSH_I32_LITERAL.op,
                    0, 0, 0, 1, // 1
                    Instructions.PUSH_I32_LITERAL.op,
                    0, 0, 0, 2, // 2
                    input.second.op,
                ),
                input.first
            )
        }

        listOf(
            Pair("True == False", Instructions.EQ_BOOL),
            Pair("True != False", Instructions.NEQ_BOOL),
        ).forEach { input ->
            assertCompiledBC(
                byteArrayOf(
                    Instructions.PUSH_BOOL_TRUE.op,
                    Instructions.PUSH_BOOL_FALSE.op,
                    input.second.op,
                ),
                input.first
            )
        }

        assertCompiledBC(
            byteArrayOf(
                Instructions.PUSH_BOOL_TRUE.op,
                Instructions.JMP_DUP_FALSE.op,
                0, 0, 0, 8,
                Instructions.DISCARD.op,
                Instructions.PUSH_BOOL_TRUE.op,
            ),
            "True && True"
        )

        assertCompiledBC(
            byteArrayOf(
                Instructions.PUSH_BOOL_TRUE.op,
                Instructions.JMP_DUP_TRUE.op,
                0, 0, 0, 8,
                Instructions.DISCARD.op,
                Instructions.PUSH_BOOL_TRUE.op,
            ),
            "True || True"
        )

        listOf(
            "True + False",
            "False < True"
        ).forEach { input ->
            unsuccessfulCompile(input)
        }
    }

    @Test
    fun `op v`() {
        assertCompiledBC(
            byteArrayOf(
                Instructions.PUSH_BOOL_TRUE.op,
                Instructions.NOT_BOOL.op,
            ),
            "!True"
        )

    }
}

private fun successfulCompile(input: String): ByteArray {
    val errors = Errors()
    val statements = infer(input, errors = errors)

    val result = compile(statements, errors)

    assertTrue(errors.hasNoErrors())

    return result
}

private fun unsuccessfulCompile(input: String) {
    val errors = Errors()
    val statements = infer(input, errors = errors)
    compile(statements, errors)

    assertTrue(errors.hasErrors())
}

private fun assertCompiledBC(expected: ByteArray, input: String) {
    assertContentEquals(expected, successfulCompile(input))
}