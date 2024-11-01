package io.littlelanguages.bendu

import io.littlelanguages.bendu.typeinference.Pump
import io.littlelanguages.bendu.typeinference.TypeEnv
import io.littlelanguages.bendu.typeinference.emptyTypeEnv
import io.littlelanguages.bendu.typeinference.typeInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class InferenceTest {
    @Test
    fun `infer literal int`() {
        assertInferExpressionEquals("1", "Int")
    }

    @Test
    fun `infer known lower ID`() {
        assertInferExpressionEquals("a", "Int", emptyTypeEnv + ("a" to emptyTypeEnv.generalise(typeInt)))
    }

    @Test
    fun `unknown lower ID error`() {
        val errors = inferErrorExpression("a", emptyTypeEnv)

        assertEquals(1, errors.size())
        assertIs<UnknownIdentifierError>(errors[0])
    }
}

private fun assertInferExpressionEquals(
    expr: String,
    expected: String,
    typeEnv: TypeEnv = emptyTypeEnv,
    pump: Pump = Pump()
) {
    val errors = Errors()
    val ast = parseExpression(expr)
    inferExpression(ast, Environment(typeEnv, pump, errors))

    assertTrue(!errors.hasErrors())
    assertEquals(expected, ast.type!!.toString())
}

private fun inferErrorExpression(expr: String, typeEnv: TypeEnv): Errors {
    val errors = Errors()
    val ast = parseExpression(expr)
    inferExpression(ast, Environment(typeEnv, Pump(), errors))

    assertTrue(errors.hasErrors())

    return errors
}