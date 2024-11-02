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
    fun `let x = 1 let y = x`() {
        assertCompiledBC(
            byteArrayOf(
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 1, // 1
                Instructions.PUSH_I32_STACK.op,
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
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 2, // 2
                Instructions.PRINT_I32.op,
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 3, // 3
                Instructions.PRINT_I32.op
            ),
            "print(1, 2, 3)"
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
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 2, // 2
                Instructions.PRINT_I32.op,
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 3, // 3
                Instructions.PRINT_I32.op,
                Instructions.PRINTLN.op
            ),
            "println(1, 2, 3)"
        )
    }

    @Test
    fun `println(1 op 3)`() {
        listOf(
            Pair("print(1 + 2)", Instructions.ADD_I32),
            Pair("print(1 - 2)", Instructions.SUB_I32),
            Pair("print(1 * 2)", Instructions.MUL_I32),
            Pair("print(1 / 2)", Instructions.DIV_I32),
            Pair("print(1 % 2)", Instructions.MOD_I32),
            Pair("print(1 ** 2)", Instructions.POW_I32),
        ).forEach { input ->
            assertCompiledBC(
                byteArrayOf(
                    Instructions.PUSH_I32_LITERAL.op,
                    0, 0, 0, 1, // 1
                    Instructions.PUSH_I32_LITERAL.op,
                    0, 0, 0, 2, // 2
                    input.second.op,
                    Instructions.PRINT_I32.op
                ),
                input.first
            )
        }
    }
}

private fun successfulCompile(input: String): ByteArray {
    val errors = Errors()
    val statements = parse(input, errors)

    assertTrue(errors.hasNoErrors())

    return compile(statements)
}

private fun assertCompiledBC(expected: ByteArray, input: String) {
    assertContentEquals(expected, successfulCompile(input))
}