package io.littlelanguages.bendu.compiler

import io.littlelanguages.bendu.*
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertTrue

class MatchTransformerTest {
    @Test
    fun `literal array`() {
        assertTransformation(
            listOf(
                "{",
                "  let _u0: Int = 1",
                "  [1, 2, 3]",
                "}"
            ), "match 1 with _ -> [1, 2, 3]"
        )

    }

    @Test
    fun `sample mappairs`() {
        assertTransformation(
            listOf(
                "{",
                "  let _u0: [a, b] a * b = Tuple(Nil(), Nil())",
                "  case _u0 with",
                "  | _Tuple2(_u1, _u2) -> case _u1 with",
                "    | Nil() -> 0",
                "    | Cons(_u3, _) -> case _u2 with",
                "      | Nil() -> 1",
                "      | Cons(_u5, _) -> (_u3 Plus _u5)",
                "}"
            ), "type List[a] = Nil | Cons[a, List[a]] ; match (Nil(), Nil()) with\n" +
                    "   | (Nil(), ys) -> 0\n" +
                    "   | (Cons(x, xs), Nil()) -> 1\n" +
                    "   | (Cons(x, xs), Cons(y, ys)) -> x + y"
        )
    }

    @Test
    fun `sample nodups`() {
        assertTransformation(
            listOf(
                "{",
                "  let _u0: List[Int] = Nil()",
                "  case _u0 with",
                "  | Nil() -> 1",
                "  | Cons(_u1, _u2) -> case _u2 with",
                "    | Nil() -> _u1",
                "    | Cons(_u3, _) -> (_u3 Plus _u1)",
                "}"
            ), "type List[a] = Nil | Cons[a, List[a]] ; match Nil() with " +
                    "| Nil() -> 1" +
                    "| Cons(x, Nil()) -> x" +
                    "| Cons(y, Cons(x, xs)) -> x + y"
        )
    }

    @Test
    fun `sample mappairs prime`() {
        assertTransformation(
            listOf(
                "{",
                "  let _u0: [a, b] a * b = Tuple(Nil(), Nil())",
                "  case _u0 with",
                "  | _Tuple2(_u1, _u2) -> case _u1 with",
                "    | Nil() -> 0",
                "    | Cons(_, _) -> case _u2 with",
                "      | Nil() -> 1",
                "      | Cons(_, _) -> case _u1 with",
                "        | Nil() -> Error",
                "        | Cons(_u3, _) -> case _u2 with",
                "          | Nil() -> Error",
                "          | Cons(_u5, _) -> (_u3 Plus _u5)",
                "}"
            ), "type List[a] = Nil | Cons[a, List[a]] ; match (Nil(), Nil()) with\n" +
                    "   | (Nil(), ys) -> 0\n" +
                    "   | (xs, Nil()) -> 1\n" +
                    "   | (Cons(x, xs), Cons(y, ys)) -> x + y"
        )
    }

    @Test
    fun `pattern type qualifier`() {
        assertTransformation(
            listOf(
                "{",
                "  let _u0: Int = 123",
                "  _u0",
                "}"
            ), "match 123 with v: Int -> v"
        )
    }

    @Test
    fun `pattern identifier`() {
        assertTransformation(
            listOf(
                "{",
                "  let _u0: Int = 123",
                "  {",
                "    let v: Int = _u0",
                "    v",
                "  }",
                "}"
            ), "match 123 with _ @ v -> v"
        )
    }


    @Test
    fun `tidy fails on left and right`() {
        assertTransformation(
            listOf(
                "{",
                "  let _u0: [a, b] a * b = Tuple(Nil(), Nil())",
                "  case _u0 with",
                "  | _Tuple2(_u1, _u2) -> case _u1 with",
                "    | Nil() -> true",
                "    | Cons(_, _) -> case _u2 with",
                "      | Nil() -> true",
                "      | Cons(_, _) -> false",
                "}"
            ), "type List[a] = Nil | Cons[a, List[a]] ; match (Nil(), Nil()) with " +
                    "| (Nil(), _) -> True " +
                    "| (_, Nil()) -> True " +
                    "| _ -> False"
        )

//        assertTransformation(listOf(""), "match 1 with 0 -> 1 | 1 -> 2 | n -> n * n")

//        assertTransformation(
//            listOf(
//                "{",
//                "  let _x: Tuple[Int, Int] = Tuple(3, 4)",
//                "  [] case _x with",
//                "  | Tuple(_u0, _u1) -> if",
//                "      | if",
//                "    | (_u0 EqualEqual 1) -> (_u1 EqualEqual 2) -> \"One\" -> \"Other\"",
//                "}"
//            ),
//            "match (3, 4) with (1, 2) -> \"One\" | (3, 4) -> \"Two\" | _ -> \"Other\""
//        )

//        assertTransformation(
//            listOf(
//                "{",
//                "  let _x: Tuple[Int, Int] = Tuple(3, 4)",
//                "  [] case _x with",
//                "  | Tuple(_u0, _u1) -> if",
//                "      | if",
//                "    | (_u0 EqualEqual 1) -> (_u1 EqualEqual 2) -> \"One\" -> \"Other\"",
//                "}"
//            ),
//            "type Tuple[a, b] = Tuple[a, b] match Tuple(3, 4) with Tuple(1, 2) -> \"One\" | Tuple(3, 4) -> \"Two\" | _ -> \"Other\""
//        )

//        assertTransformation(
//            listOf(), "type Optional[a] = None | Some[a] ; match None() with\n" +
//                    "   | None() -> 0\n" +
//                    "   | Some(10) -> 121\n" +
//                    "   | _ -> 100\n"
//
//        )

//        assertTransformation(listOf(), "type List[a] = Nil | Cons[a, List[a]] ; match (Nil(), Nil()) with\n" +
//                "   | (Nil(), ys) -> 0\n" +
//                "   | (xs, Nil()) -> 1\n" +
//                "   | (Cons(x, xs), Cons(y, ys)) -> x + y\n" +
//                ""
//        )
    }
}

/*
{
    let _x: Tuple[Int, Int] = Tuple(3, 4)
    [] case _x with
       | Tuple(_u0, _u1) ->
          | if (if (_u0 EqualEqual 1) -> (_u1 EqualEqual 2) | false) -> "One"
          | (if (_u0 EqualEqual 3) -> (_u1 EqualEqual 4) | false) -> "Two"
          | fail
    => "Other"
}
*/


fun assertTransformation(expected: List<String>, input: String) {
    val entry = CacheManager.useExpression(File("test.bc"), input)
    val errors = Errors()
    val script = infer(entry, errors = errors)
    val amendedScript = transform(script)
    val result = pp(amendedScript)

    if (errors.hasErrors()) {
        errors.printErrors(false)
    }

    assertTrue(errors.hasNoErrors())

    println(result.joinToString("\n"))

    assertContentEquals(expected, result)
}

private fun transform(script: Script): Script =
    Script(
        script.imports,
        script.decs.map { if (it is DeclarationExpression) DeclarationExpression(transform(it.e, TransformState())) else it }
    )
