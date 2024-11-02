package io.littlelanguages.bendu

import io.littlelanguages.bendu.typeinference.Constraints
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

    @Test
    fun `infer binary operator`() {
        listOf(
            "1 + 2",
            "1 - 2",
            "1 * 2",
            "1 / 2",
            "1 % 2",
            "1 ** 2",
        ).forEach { expr ->
            assertInferExpressionEquals(expr, "Int")
        }
    }
}

private fun assertInferExpressionEquals(
    expr: String,
    expected: String,
    typeEnv: TypeEnv = emptyTypeEnv,
    pump: Pump = Pump()
) {
    val errors = Errors()
    val constraints = Constraints()
    val ast = parseExpression(expr, errors)
    inferExpression(ast, Environment(typeEnv, pump, errors, constraints))

    val subst = constraints.solve()
    ast.apply(subst)

    assertTrue(errors.hasNoErrors())
    assertEquals(expected, ast.type!!.toString())
}

private fun inferErrorExpression(expr: String, typeEnv: TypeEnv): Errors {
    val errors = Errors()
    val constraints = Constraints()
    val ast = parseExpression(expr, errors)
    assertTrue(errors.hasNoErrors())
    inferExpression(ast, Environment(typeEnv, Pump(), errors, constraints))

    assertTrue(errors.hasErrors())

    return errors
}