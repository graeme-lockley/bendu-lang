package io.littlelanguages.bendu

import io.littlelanguages.bendu.typeinference.*
import io.littlelanguages.scanpiler.LocationCoordinate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class InferenceTest {
    val location = LocationCoordinate(0, 0, 0)

    @Test
    fun `infer literal char`() {
        assertInferExpressionEquals("'x'", "Char")
    }

    @Test
    fun `infer literal float`() {
        assertInferExpressionEquals("1.2", "Float")
    }

    @Test
    fun `infer literal function`() {
        assertInferExpressionEquals("fn(n) = n + 1", "(Int) -> Int")
        assertInferExpressionEquals("fn(n) = 2 * n", "(Int) -> Int")
        assertInferExpressionEquals("fn(a, b) = a + b", "('5, '5) -> '5")

        // Good old composition function
        assertInferExpressionEquals("fn(a, b) = fn(c) = a(b(c))", "(('6) -> '7, ('5) -> '6) -> ('5) -> '7")
        assertInferExpressionEquals("(fn(a, b) = fn(c) = a(b(c)))(fn(n) = n + 1, fn(n) = n * 2)", "(Int) -> Int")

        assertInferExpressionEquals("let compose(f, g) = fn(n) = f(g(n)) ;" +
                "compose(fn(n) = 2 * n, fn(n) n + 1)", "(Int) -> Int")
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
        assertInferExpressionEquals("a", "Int", emptyTypeEnv + ("a" to Binding(location, emptyTypeEnv.generalise(typeInt))))
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
        assertInferExpressionEquals(
            "if a == 0 -> 1 | a == 2 -> 2 | a == 3 -> 4 | -1",
            "Int",
            emptyTypeEnv + ("a" to Binding(location, emptyTypeEnv.generalise(typeInt)))
        )
        inferErrorExpression("if 1 -> 1 | 2")
        inferErrorExpression("if True -> 1 | 'x'")
    }

    @Test
    fun `infer function declaration`() {
        assertInferFunctionEquals("let inc() = 1", "() -> Int")
        assertInferFunctionEquals("let inc(a) = a + 1", "(Int) -> Int")
        assertInferFunctionEquals("let add(a, b) = a + b + 1", "(Int, Int) -> Int")
        assertInferFunctionEquals("let add(a, b) = a + b + 1.0", "(Float, Float) -> Float")
        assertInferFunctionEquals("let f(x) = if x == 0 -> 0 | x + f(x - 1)", "(Int) -> Int")
        assertInferFunctionEquals("let f(x) = g(x)", "(Int) -> Int", emptyTypeEnv + ("g" to Binding(location, Scheme(setOf(), TArr(listOf(typeInt), typeInt)))))
        assertInferFunctionEquals("let f(x) = g(1) + x", "(Int) -> Int", emptyTypeEnv + ("g" to Binding(location, Scheme(setOf(), TArr(listOf(typeInt), typeInt)))))
    }
}

private fun assertInferExpressionEquals(expr: String, expected: String, typeEnv: TypeEnv = emptyTypeEnv) {
    val errors = Errors()
    val ast = infer(expr, errors = errors, typeEnv = typeEnv)

    assertTrue(errors.hasNoErrors())
    assertEquals(expected, ast.last().type.toString())
}

private fun assertInferFunctionEquals(expr: String, expected: String, typeEnv: TypeEnv = emptyTypeEnv) {
    val errors = Errors()
    val ast = infer(expr, errors = errors, typeEnv = typeEnv)

    assertTrue(errors.hasNoErrors())
    assertEquals(expected, (ast.last() as LetStatement).terms[0].e.type.toString())
}

private fun inferErrorExpression(expr: String, typeEnv: TypeEnv = emptyTypeEnv): Errors {
    val errors = Errors()

    infer(expr, typeEnv = typeEnv, errors = errors)

    assertTrue(errors.hasErrors())

    return errors
}