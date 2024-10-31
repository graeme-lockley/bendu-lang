package io.littlelanguages.bendu

import io.littlelanguages.bendu.typeinference.Pump
import io.littlelanguages.bendu.typeinference.TypeEnv
import io.littlelanguages.bendu.typeinference.emptyTypeEnv
import io.littlelanguages.bendu.typeinference.typeInt
import kotlin.test.Test
import kotlin.test.assertEquals

class InferenceTest {
    @Test
    fun `infer literal int`() {
        assertInferExpressionEquals("1", "Int")
    }

    @Test
    fun `infer known lower ID`() {
        assertInferExpressionEquals("a", "Int", typeEnv = emptyTypeEnv + ("a" to emptyTypeEnv.generalise(typeInt)))
    }
}

private fun assertInferExpressionEquals(
    expr: String,
    expected: String,
    typeEnv: TypeEnv = emptyTypeEnv,
    pump: Pump = Pump()
) {
    val ast = parseExpression(expr)
    inferExpression(ast, typeEnv, pump)

    assertEquals(expected, ast.type!!.toString())
}