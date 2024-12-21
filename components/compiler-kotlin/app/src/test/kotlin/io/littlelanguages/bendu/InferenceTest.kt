package io.littlelanguages.bendu

import io.littlelanguages.bendu.typeinference.*
import io.littlelanguages.scanpiler.LocationCoordinate
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class InferenceTest {
    private val location = LocationCoordinate(0, 0, 0)

    @Test
    fun `infer array element projection`() {
        assertInferExpressionEquals("[1, 2]!1", "Int")
        assertInferExpressionEquals("[1.1, 2.2]!1", "Float")
        assertInferExpressionEquals("[]!1", "[a] a")

        assertInferExpressionEquals("[1.1, 2.2]!1 + 1.4", "Float")
    }

    @Test
    fun `infer array range projection`() {
        assertInferExpressionEquals("[1, 2]!1:3", "Array[Int]")
        assertInferExpressionEquals("[1.1, 2.2]!1:3", "Array[Float]")
        assertInferExpressionEquals("[]!1:2", "[a] Array[a]")

        assertInferExpressionEquals("[1, 2]!:3", "Array[Int]")
        assertInferExpressionEquals("[1, 2]!1:", "Array[Int]")
        assertInferExpressionEquals("[1, 2]!:", "Array[Int]")
    }

    @Test
    fun `infer literal array`() {
        assertInferExpressionEquals("[]", "[a] Array[a]")
        assertInferExpressionEquals("[1, 2, 3]", "Array[Int]")
        assertInferExpressionEquals("[1.0, 2.1, 3.2]", "Array[Float]")
    }

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
        assertInferExpressionEquals("fn(a, b) = a + b", "[a] (a, a) -> a")

        assertInferExpressionEquals("fn(a, b) = a : Int + b", "(Int, Int) -> Int")
        assertInferExpressionEquals("fn(a: Int, b) = a + b", "(Int, Int) -> Int")
        assertInferExpressionEquals("fn(a, b: Int) = a + b", "(Int, Int) -> Int")
        assertInferExpressionEquals("fn(a, b): Int = a + b", "(Int, Int) -> Int")

        // Good old composition function
        assertInferExpressionEquals("fn(a, b) = fn(c) = a(b(c))", "[a, b, c] ((a) -> b, (c) -> a) -> (c) -> b")
        assertInferExpressionEquals("(fn(a, b) = fn(c) = a(b(c)))(fn(n) = n + 1, fn(n) = n * 2)", "(Int) -> Int")

        assertInferExpressionEquals(
            "let compose(f, g) = fn(n) = f(g(n)) ;" +
                    "compose(fn(n) = 2 * n, fn(n) n + 1)", "(Int) -> Int"
        )

        assertInferExpressionEquals(
            "fn[a](f: (a) -> Int, g: (Int) -> a) = fn(n): Int f(g(n): Int)",
            "((Int) -> Int, (Int) -> Int) -> (Int) -> Int"
        )
        assertInferExpressionEquals(
            "fn(f, g) = fn(n: Int): Int f(g(n): Int)",
            "((Int) -> Int, (Int) -> Int) -> (Int) -> Int"
        )
        assertInferExpressionEquals(
            "fn[a](f, g: (a) -> Int) = fn(n: Int): Int f(g(n))",
            "((Int) -> Int, (Int) -> Int) -> (Int) -> Int"
        )

        assertInferFunctionEquals("let identity[a](n: a): a = n", "[a] (a) -> a")

        assertInferExpressionEquals(
            "let identity[a](n: a): a = n ; if identity(\"Hello\") == \"Hello\" -> identity(1) | identity(2)",
            "Int"
        )
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
    fun `infer literal tuple`() {
        assertInferExpressionEquals("(\"Hello\", 1)", "String * Int")

        assertInferExpressionEquals("(let a = 1, 2)", "Unit * Int")
        assertInferExpressionEquals("({ let a = 1 ; a }, 2)", "Int * Int")
    }

    @Test
    fun `infer literal unit`() {
        assertInferExpressionEquals("()", "Unit")
    }

    @Test
    fun `infer known lower ID`() {
        assertInferExpressionEquals(
            "a",
            "Int",
            emptyTypeEnv + ("a" to Binding(location, false, emptyTypeEnv.generalise(typeInt)))
        )
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
            "[1, 2, 3] << 4",
            "0 >> [1, 2, 3]",
            "[1, 2, 3] <! 4",
            "0 >! [1, 2, 3]",
        ).forEach { expr ->
            assertInferExpressionEquals(expr, "Array[Int]")
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
            emptyTypeEnv + ("a" to Binding(location, false, emptyTypeEnv.generalise(typeInt)))
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
        assertInferFunctionEquals(
            "let f(x) = g(x)",
            "(Int) -> Int",
            emptyTypeEnv + ("g" to Binding(location, false, Scheme(setOf(), TArr(listOf(typeInt), typeInt))))
        )
        assertInferFunctionEquals(
            "let f(x) = g(1) + x",
            "(Int) -> Int",
            emptyTypeEnv + ("g" to Binding(location, false, Scheme(setOf(), TArr(listOf(typeInt), typeInt))))
        )

        assertInferFunctionEquals("let f((a, b)): Int = a + b", "(Int * Int) -> Int")
        assertInferFunctionEquals("let f((a, _)) = a + 1", "[a] (Int * a) -> Int")
        assertInferFunctionEquals("let f((a, _: Int)) = a + 1", "(Int * Int) -> Int")
        assertInferFunctionEquals("let f((a, _): Int * Int) = a + 1", "(Int * Int) -> Int")
        assertInferFunctionEquals("let f((a, (c, d))): Int = a + c + d", "(Int * (Int * Int)) -> Int")

        assertInferFunctionEquals("let add((a, b): Int * Int, c: Int): Int = a + b + c", "(Int * Int, Int) -> Int")
    }

    @Test
    fun `infer value declaration`() {
        assertInferFunctionEquals("let a = 1", "Int")
        assertInferFunctionEquals("let a = 1.0", "Float")
        assertInferFunctionEquals("let a = 'x'", "Char")
        assertInferFunctionEquals("let a = \"Hello\"", "String")
        assertInferFunctionEquals("let a = True", "Bool")
        assertInferFunctionEquals("let a = ()", "Unit")

        inferErrorExpression("let a: Int = 1.0")
    }

    @Test
    fun `infer assignment`() {
        assertInferExpressionEquals("let a! = 1 ; a := 2; a", "Int")
        assertInferExpressionEquals("let a! = 1.0 ; a := 2.0; a", "Float")
        assertInferExpressionEquals("let a! = 'x' ; a := 'y'; a", "Char")
        assertInferExpressionEquals("let a! = \"Hello\" ; a := \"World\"; a", "String")
        assertInferExpressionEquals("let a! = True ; a := False; a", "Bool")
        assertInferExpressionEquals("let a! = () ; a := (); a", "Unit")

        assertInferExpressionEquals("let a = [1, 2, 3]; a!1 := 3; a", "Array[Int]")
    }
}


private fun assertInferExpressionEquals(expr: String, expected: String, typeEnv: TypeEnv = emptyTypeEnv) {
    val errors = Errors()
    val ast = infer(openCache().useExpression(File("test.bc"), expr), errors = errors, typeEnv = typeEnv)

    assertTrue(errors.hasNoErrors())
    assertEquals(expected, ast.last().type.toString())
}

private fun assertInferFunctionEquals(expr: String, expected: String, typeEnv: TypeEnv = emptyTypeEnv) {
    val errors = Errors()
    val ast = infer(openCache().useExpression(File("test.bc"), expr), errors = errors, typeEnv = typeEnv)

    assertTrue(errors.hasNoErrors())
    assertEquals(expected, (ast.last() as LetStatement).terms[0].type!!.toString())
}

private fun inferErrorExpression(expr: String, typeEnv: TypeEnv = emptyTypeEnv): Errors {
    val errors = Errors()

    infer(openCache().useExpression(File("test.bc"), expr), typeEnv = typeEnv, errors = errors)

    assertTrue(errors.hasErrors())

    return errors
}