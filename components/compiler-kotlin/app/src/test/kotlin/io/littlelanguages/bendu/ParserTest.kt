package io.littlelanguages.bendu

import kotlin.test.*

class ParserTest {
    @Test
    fun `let declaration`() {
        listOf(
            "let x = 1; let y = x",
            "let x = (1); let y = x",
            "let x = (((1))); let y = ((x))",
            "let x = 1 let y = x",
            "let x = (1) let y = x",
            "let x = (((1))) let y = ((x))"
        ).forEach { input ->
            val statements = successfulParse(input, 2)

            assertIs<LetStatement>(statements[0])
            assertEquals((statements[0] as LetStatement).terms[0].id.value, "x")
            assertIs<LiteralIntExpression>((statements[0] as LetStatement).terms[0].e)
            assertEquals(((statements[0] as LetStatement).terms[0].e as LiteralIntExpression).v.value, 1)

            assertIs<LetStatement>(statements[1])
            assertEquals((statements[1] as LetStatement).terms[0].id.value, "y")
            assertIs<LowerIDExpression>((statements[1] as LetStatement).terms[0].e)
            assertEquals(((statements[1] as LetStatement).terms[0].e as LowerIDExpression).v.value, "x")
        }

        listOf(
            "let bob() = 1"
        ).forEach { input ->
            val statements = successfulParse(input, 1)

            assertIs<LetStatement>(statements[0])
            assertEquals("bob", (statements[0] as LetStatement).terms[0].id.value)
            assertIs<LiteralFunctionExpression>((statements[0] as LetStatement).terms[0].e)
            assertEquals(0, ((statements[0] as LetStatement).terms[0].e as LiteralFunctionExpression).parameters.size)
            assertIs<LiteralIntExpression>(((statements[0] as LetStatement).terms[0].e as LiteralFunctionExpression).body)
            assertEquals(
                1,
                (((statements[0] as LetStatement).terms[0].e as LiteralFunctionExpression).body as LiteralIntExpression).v.value
            )
        }

        listOf(
            "let add(a, b) = 1"
        ).forEach { input ->
            val statements = successfulParse(input, 1)

            assertIs<LetStatement>(statements[0])
            assertEquals("add", (statements[0] as LetStatement).terms[0].id.value)
            assertEquals(2, ((statements[0] as LetStatement).terms[0].e as LiteralFunctionExpression).parameters.size)
            assertEquals("a", ((statements[0] as LetStatement).terms[0].e as LiteralFunctionExpression).parameters[0].value)
            assertEquals("b", ((statements[0] as LetStatement).terms[0].e as LiteralFunctionExpression).parameters[1].value)
            assertIs<LiteralIntExpression>(((statements[0] as LetStatement).terms[0].e as LiteralFunctionExpression).body)
            assertEquals(
                1,
                (((statements[0] as LetStatement).terms[0].e as LiteralFunctionExpression).body as LiteralIntExpression).v.value
            )
        }
    }

    @Test
    fun `apply expression`() {
        listOf(
            "f()",
            "f(1)",
            "f(1, 2)",
            "f(1, 2, 3)",
            "f(1, 2, 3, 4)",
            "f(1, 2, 3, 4, 5)",
        ).forEachIndexed { idx, input ->
            val statements = successfulParse(input, 1)

            assertIs<ApplyExpression>(statements[0])
            assertIs<LowerIDExpression>((statements[0]  as ApplyExpression).f)
            assertEquals(
                "f",
                ((statements[0] as ApplyExpression).f as LowerIDExpression).v.value
            )

            assertEquals(idx, (statements[0] as ApplyExpression).arguments.size)
            (statements[0] as ApplyExpression).arguments.forEachIndexed { i, argument ->
                assertIs<LiteralIntExpression>(argument)
                assertEquals(i + 1, argument.v.value)
            }
        }
    }

    @Test
    fun `binary op expressions`() {
        listOf(
            Pair("print(1 + 2)", Op.Plus),
            Pair("print(1 - 2)", Op.Minus),
            Pair("print(1 * 2)", Op.Multiply),
            Pair("print(1 / 2)", Op.Divide),
            Pair("print(1 % 2)", Op.Modulo),
            Pair("print(1 ** 2)", Op.Power),
            Pair("print(\"a\" + \"b\")", Op.Plus),
        ).forEach { input ->
            val statements = successfulParse(input.first, 1)

            assertIs<PrintStatement>(statements[0])
            assertEquals((statements[0] as PrintStatement).es.size, 1)
            assertIs<BinaryExpression>((statements[0] as PrintStatement).es[0])
            assertEquals(((statements[0] as PrintStatement).es[0] as BinaryExpression).op.op, input.second)
        }
    }

    @Test
    fun `parser error`() {
        val input = "let x = (1; let y = z"
        val errors = Errors()
        parse(input, errors)

        assertTrue(errors.hasErrors())
        assertIs<ParsingError>(errors[0])
    }

    @Test
    fun `literal over and under flows`() {
        listOf(
            "2147483648",
            "-2147483649"
        ).forEach { input ->
            val errors = Errors()
            parse(input, errors)
            assertTrue(errors.hasErrors())
            assertIs<InvalidLiteralError>(errors[0])
        }
    }

    @Test
    fun `literal char`() {
        listOf(
            Pair("'x'", 'x'),
            Pair("'\\n'", '\n'),
            Pair("'\\\\'", '\\'),
            Pair("'\\''", '\''),
        ).forEach { input ->
            val statements = successfulParse(input.first, 1)

            assertIs<LiteralCharExpression>(statements[0])
            assertEquals((statements[0] as LiteralCharExpression).v.value, input.second)
        }
    }

    @Test
    fun `literal float`() {
        listOf(
            Pair("1.0", 1.0.toFloat()),
            Pair("1.0e5", 100000.0.toFloat()),
            Pair("1.0e-5", 0.00001.toFloat()),
            Pair("-3.14159265", (-3.14159265).toFloat()),
        ).forEach { input ->
            val statements = successfulParse(input.first, 1)

            assertIs<LiteralFloatExpression>(statements[0])
            assertEquals((statements[0] as LiteralFloatExpression).v.value, input.second)
        }
    }

    @Test
    fun `literal string`() {
        listOf(
            Pair("\"\"", ""),
            Pair("\"'hello'\"", "'hello'"),
            Pair("\"hello world\"", "hello world"),
            Pair("\"[\\n]\"", "[\n]"),
            Pair("\"[\\\\]\"", "[\\]"),
            Pair("\"[\\\"]\"", "[\"]"),
            Pair("\"[\\x32;]\"", "[ ]"),
        ).forEach { input ->
            val statements = successfulParse(input.first, 1)

            assertIs<LiteralStringExpression>(statements[0])
            assertEquals((statements[0]  as LiteralStringExpression).v.value, input.second)
        }

    }

    @Test
    fun `literal unit`() {
        val statements = successfulParse("()", 1)

        assertIs<LiteralUnitExpression>(statements[0])
    }
}

private fun successfulParse(input: String, numberOfStatements: Int): List<Expression> {
    val errors = Errors()
    val statements = parse(input, errors)
    assertTrue(errors.hasNoErrors())
    assertEquals(statements.size, numberOfStatements)

    return statements
}