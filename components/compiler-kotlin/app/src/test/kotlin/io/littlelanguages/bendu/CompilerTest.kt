package io.littlelanguages.bendu

import io.littlelanguages.bendu.compiler.Instructions
import kotlin.test.Test
import kotlin.test.assertContentEquals

class CompilerTest {
    @Test
    fun `let x = 1 let y = x`() {
        val bc = compile(parse("let x = 1 ; let y = x"))

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
        val bc = compile(parse("print()"))

        val expected = byteArrayOf()

        assertContentEquals(expected, bc)
    }


    @Test
    fun `print(1, 2, 3)`() {
        val bc = compile(parse("print(1, 2, 3)"))

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
        val bc = compile(parse("println(1, 2, 3)"))

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
        val bc = compile(parse("println()"))

        val expected = byteArrayOf(Instructions.PRINTLN.op)

        assertContentEquals(expected, bc)
    }
}
