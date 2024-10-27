package io.littlelanguages.bendu

import kotlin.test.Test
import kotlin.test.assertContentEquals

class CompilerTest {
    @Test
    fun `let x = 1 let y = x`() {
        val bc = compile(parse("let x = 1 ; let y = x"))

        val expected = byteArrayOf(
            1, // PUSH_I32_LITERAL
            0, 0, 0, 1, // 1
            2, // PUSH_I32_STACK
            0, 0, 0, 0 // x
        )

        assertContentEquals(expected, bc)
    }
}
