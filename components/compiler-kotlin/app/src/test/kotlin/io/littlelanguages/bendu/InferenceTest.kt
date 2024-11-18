package io.littlelanguages.bendu

import io.littlelanguages.bendu.typeinference.TypeEnv
import io.littlelanguages.bendu.typeinference.emptyTypeEnv
import io.littlelanguages.bendu.typeinference.typeInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class InferenceTest {
    @Test
    fun `infer literal char`() {
        assertInferExpressionEquals("'x'", "Char")
    }

    @Test
    fun `infer literal float`() {
        assertInferExpressionEquals("1.2", "Float")
    }

    @Test
    fun `infer literal int`() {
        assertInferExpressionEquals("1", "Int")
    }

    @Test
    fun `infer literal string`() {
        assertInferExpressionEquals("\"Hello\"", "String")
    }

    @Test
    fun `infer literal unit`() {
        assertInferExpressionEquals("()", "Unit")
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

        listOf(
            "True && True",
            "True || True",
            "1 == 1",
            "1 != 1",
            "1 < 1",
            "1 <= 1",
            "1 > 1",
            "1 >= 1",
        ).forEach { expr ->
            assertInferExpressionEquals(expr, "Bool")
        }

        listOf(
            "1 && 1",
            "1 || 1",
            "1 == True",
            "1 != False",
            "1 < True",
            "1 <= False",
            "1 > True",
            "1 >= False",
        ).forEach { expr ->
            inferErrorExpression(expr)
        }
    }

    @Test
    fun `infer unary operator`() {
        assertInferExpressionEquals("!True", "Bool")
        inferErrorExpression("!1")
    }

    @Test
    fun `infer if expression`() {
        assertInferExpressionEquals("if True -> 1 | 2", "Int")
        assertInferExpressionEquals("if True -> ()", "Unit")
        assertInferExpressionEquals("if a == 0 -> 1 | a == 2 -> 2 | a == 3 -> 4 | -1", "Int", emptyTypeEnv + ("a" to emptyTypeEnv.generalise(typeInt)))
        inferErrorExpression("if 1 -> 1 | 2")
        inferErrorExpression("if True -> 1 | 'x'")
    }
}

private fun assertInferExpressionEquals(expr: String, expected: String, typeEnv: TypeEnv = emptyTypeEnv) {
    val errors = Errors()
    val ast = infer(expr, errors = errors, typeEnv = typeEnv)

    assertTrue(errors.hasNoErrors())
    assertEquals(expected, ast[0].type.toString())
}

private fun inferErrorExpression(expr: String, typeEnv: TypeEnv = emptyTypeEnv): Errors {
    val errors = Errors()

    infer(expr, typeEnv = typeEnv, errors = errors)

    assertTrue(errors.hasErrors())

    return errors
}