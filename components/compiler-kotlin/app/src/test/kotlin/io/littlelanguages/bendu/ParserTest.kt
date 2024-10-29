package io.littlelanguages.bendu

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ParserTest {
    @Test
    fun parserParses() {
        listOf(
            "let x = 1; let y = x",
            "let x = (1); let y = x",
            "let x = (((1))); let y = ((x))",
            "let x = 1 let y = x",
            "let x = (1) let y = x",
            "let x = (((1))) let y = ((x))"
        ).forEach { input ->
            val statements = parse(input)

            assertEquals(statements.size, 2)

            assertIs<LetStatement>(statements[0])
            assertEquals((statements[0] as LetStatement).id.value, "x")
            assertIs<LiteralIntExpression>((statements[0] as LetStatement).e)
            assertEquals(((statements[0] as LetStatement).e as LiteralIntExpression).v.value, 1)

            assertIs<LetStatement>(statements[1])
            assertEquals((statements[1] as LetStatement).id.value, "y")
            assertIs<LowerIDExpression>((statements[1] as LetStatement).e)
            assertEquals(((statements[1] as LetStatement).e as LowerIDExpression).v.value, "x")
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
        ).forEach { input ->
            val statements = parse(input.first)

            assertEquals(statements.size, 1)

            assertIs<PrintStatement>(statements[0])
            assertEquals((statements[0] as PrintStatement).es.size, 1)
            assertIs<BinaryExpression>((statements[0] as PrintStatement).es[0])
            assertEquals(((statements[0] as PrintStatement).es[0] as BinaryExpression).op.op, input.second)
        }
    }
}
