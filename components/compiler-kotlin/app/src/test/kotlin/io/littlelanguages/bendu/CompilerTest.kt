package io.littlelanguages.bendu

import io.littlelanguages.bendu.compiler.Instructions
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertTrue

class CompilerTest {
    @Test
    fun `let x = 1 let y = x`() {
        val bc = successfulCompile("let x = 1 ; let y = x")

        val expected = byteArrayOf(
            Instructions.PUSH_I32_LITERAL.op,
            0, 0, 0, 1, // 1
            Instructions.PUSH_I32_STACK.op, // PUSH_I32_STACK
            0, 0, 0, 0 // x
        )

        assertContentEquals(expected, bc)
    }

    @Test
    fun `print()`() {
        val bc = successfulCompile("print()")

        val expected = byteArrayOf()

        assertContentEquals(expected, bc)
    }


    @Test
    fun `print(1, 2, 3)`() {
        val bc = successfulCompile("print(1, 2, 3)")

        val expected = byteArrayOf(
            Instructions.PUSH_I32_LITERAL.op,
            0, 0, 0, 1, // 1
            Instructions.PRINT_I32.op,
            Instructions.PUSH_I32_LITERAL.op,
            0, 0, 0, 2, // 2
            Instructions.PRINT_I32.op,
            Instructions.PUSH_I32_LITERAL.op,
            0, 0, 0, 3, // 3
            Instructions.PRINT_I32.op
        )

        assertContentEquals(expected, bc)
    }

    @Test
    fun `println(1, 2, 3)`() {
        val bc = successfulCompile("println(1, 2, 3)")

        val expected = byteArrayOf(
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
        )

        assertContentEquals(expected, bc)
    }

    @Test
    fun `println()`() {
        val bc = successfulCompile("println()")

        val expected = byteArrayOf(Instructions.PRINTLN.op)

        assertContentEquals(expected, bc)
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
            val bc = successfulCompile(input.first)

            val expected = byteArrayOf(
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 1, // 1
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 2, // 2
                input.second.op,
                Instructions.PRINT_I32.op
            )

            assertContentEquals(expected, bc)
        }
    }
}

private fun successfulCompile(input: String): ByteArray {
    val errors = Errors()
    val statements = parse(input, errors)

    assertTrue(!errors.hasErrors())

    return compile(statements)
}