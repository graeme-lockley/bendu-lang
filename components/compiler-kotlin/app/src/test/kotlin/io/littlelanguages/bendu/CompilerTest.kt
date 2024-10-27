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
}
