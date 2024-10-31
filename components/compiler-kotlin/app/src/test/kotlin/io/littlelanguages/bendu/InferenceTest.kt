package io.littlelanguages.bendu

import kotlin.test.Test
import kotlin.test.assertEquals

class InferenceTest {
    @Test
    fun `infer literal int`() {
        assertInferExpressionEquals("1", "Int")
    }
}

private fun assertInferExpressionEquals(expr: String, expected: String) {
    val ast = parseExpression(expr)
    inferExpression(ast)

    assertEquals(expected, ast.type!!.toString())
}