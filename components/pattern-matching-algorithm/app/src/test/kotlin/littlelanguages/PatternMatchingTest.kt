package littlelanguages

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EnvironmentTest {
    @Test
    fun `arity of constructor`() {
        val env = Environment(
            listOf(
                AbstractDataType(
                    "List", listOf(
                        "Nil" to 0,
                        "Cons" to 2
                    )
                )
            )
        )

        assertEquals(0, env.arity("Nil"))
        assertEquals(2, env.arity("Cons"))
    }

    @Test
    fun `constructors of name`() {
        val env = Environment(
            listOf(
                AbstractDataType(
                    "List", listOf(
                        "Nil" to 0,
                        "Cons" to 2
                    )
                )
            )
        )

        assertEquals(listOf("Nil", "Cons"), env.constructors("Nil"))
        assertEquals(listOf("Nil", "Cons"), env.constructors("Cons"))
    }
}

class MatchTest {
    private val env = Environment(
        listOf(
            AbstractDataType("List", listOf("Nil" to 0, "Cons" to 2)),
            AbstractDataType("Pair", listOf("Pair" to 2)),
        )
    )

    @Test
    fun factorial() {
        val variables = listOf("_v1")
        val equations = listOf(
            Equation(
                listOf(LiteralPattern("0")),
                Literal("1")
            ),
            Equation(
                listOf(VarPattern("n")),
                App(Var("F"), Var("n"))
            )
        )

        assertFullMatch(
            variables, equations,
            "if (_v1 == \"0\") then \"1\" else (F _v1)"
        )
    }

    @Test
    fun removeLiteralTest() {
        val variables = listOf("_v1")
        val equations = listOf(
            Equation(
                listOf(LiteralPattern("1")),
                Var("x")
            )
        )

        assertFullMatch(variables, equations, "if (_v1 == \"1\") then x else ERROR")
    }

    @Test
    fun removeMultipleLiteralTest() {
        val variables = listOf("_v1")
        val equations = listOf(
            Equation(
                listOf(
                    ConPattern("Cons", listOf(LiteralPattern("hello"), LiteralPattern("world"))),
                ),
                Var("x")
            ),
            Equation(
                listOf(
                    ConPattern("Cons", listOf(LiteralPattern("bye"), LiteralPattern("world"))),
                ),
                Var("y")
            ),
        )

        assertFullMatch(
            variables, equations,
            "case _v1 of Nil -> ERROR | Cons _u0 _u1 -> if ((_u1 == \"world\") && (_u0 == \"hello\")) then x else if ((_u1 == \"world\") && (_u0 == \"bye\")) then y else ERROR",
        )
    }

    @Test
    fun removeMultipleLiteral2Test() {
        val variables = listOf("_v1")
        val equations = listOf(
            Equation(
                listOf(
                    ConPattern("Cons", listOf(LiteralPattern("hello"), VarPattern("x"))),
                ),
                Literal("1")
            ),
            Equation(
                listOf(
                    ConPattern("Cons", listOf(LiteralPattern("bye"), LiteralPattern("world"))),
                ),
                Var("y")
            ),
            Equation(
                listOf(
                    ConPattern("Cons", listOf(LiteralPattern("hello"), VarPattern("z"))),
                ),
                Var("z")
            ),
            Equation(
                listOf(
                    ConPattern("Cons", listOf(VarPattern("a"), VarPattern("b"))),
                ),
                App(App(Var("A"), Var("a")), Var("b"))
            ),
        )

        assertFullMatch(
            variables, equations,
            "case _v1 of Nil -> ERROR | Cons _u0 _u1 -> if (_u0 == \"hello\") then \"1\" else if ((_u1 == \"world\") && (_u0 == \"bye\")) then y else if (_u0 == \"hello\") then _u1 else ((A _u0) _u1)",
        )
    }

    @Test
    fun unwieldy() {
        val variables = listOf("_v1", "_v2")
        val equations = listOf(
            Equation(
                listOf(
                    ConPattern("Nil", listOf()),
                    ConPattern("Nil", listOf()),
                ),
                Var("A")
            ),
            Equation(
                listOf(
                    VarPattern("xs"),
                    VarPattern("ys"),
                ),
                App(App(Var("B"), Var("xs")), Var("ys"))
            )
        )

        assertFullMatch(
            variables, equations,
            "(case _v1 of Nil -> case _v2 of Nil -> A | Cons null null -> FAIL | Cons null null -> FAIL) [] ((B _v1) _v2)",
        )
    }

    @Test
    fun demo() {
        val variables = listOf("_v1", "_v2", "_v3")
        val equations = listOf(
            Equation(
                listOf(
                    VarPattern("f"),
                    ConPattern("Nil", listOf()),
                    VarPattern("ys")
                ),
                App(App(Var("A"), Var("f")), Var("ys"))
            ),
            Equation(
                listOf(
                    VarPattern("f"),
                    ConPattern("Cons", listOf(VarPattern("x"), VarPattern("xs"))),
                    ConPattern("Nil", listOf())
                ),
                App(App(App(Var("B"), Var("f")), Var("x")), Var("xs"))
            ),
            Equation(
                listOf(
                    VarPattern("f"),
                    ConPattern("Cons", listOf(VarPattern("x"), VarPattern("xs"))),
                    ConPattern("Cons", listOf(VarPattern("y"), VarPattern("ys"))),
                ),
                App(App(App(App(App(Var("C"), Var("f")), Var("x")), Var("xs")), Var("y")), Var("ys"))
            )
        )

        assertFullMatch(
            variables, equations,
            "case _v2 of Nil -> ((A _v1) _v3) | Cons _u0 _u1 -> case _v3 of Nil -> (((B _v1) _u0) _u1) | Cons _u2 _u3 -> (((((C _v1) _u0) _u1) _u2) _u3)",
        )
    }

    @Test
    fun pair() {
        val variables = listOf("_v1")
        val equations = listOf(
            Equation(
                listOf(
                    ConPattern("Pair", listOf(VarPattern("a"), VarPattern("b"))),
                ),
                App(App(Var("A"), Var("a")), Var("b"))
            ),
        )

        assertFullMatch(
            variables, equations,
            "case _v1 of Pair _u0 _u1 -> ((A _u0) _u1)",
        )
    }

    @Test
    fun pairWithLiteral() {
        val variables = listOf("_v1")
        val equations = listOf(
            Equation(
                listOf(
                    ConPattern("Pair", listOf(LiteralPattern("100"), VarPattern("b"))),
                ),
                App(Var("A"), Var("b"))
            ),
        )

        assertFullMatch(
            variables, equations,
            "case _v1 of Pair _u0 _u1 -> if (_u0 == \"100\") then (A _u1) else ERROR",
        )
    }

    @Test
    fun pairWithAFreeField() {
        val variables = listOf("_v1")
        val equations = listOf(
            Equation(
                listOf(
                    ConPattern("Pair", listOf(VarPattern("a"), VarPattern("b"))),
                ),
                App(Var("A"), Var("a"))
            ),
        )

        assertFullMatch(
            variables, equations,
            "case _v1 of Pair _u0 null -> (A _u0)",
        )
    }

    private fun assertFullMatch(variables: List<String>, equations: List<Equation>, expected: String) {
        assertEquals(expected, prettyPrint(fullMatch(variables, equations, ERROR, env)))
    }
}