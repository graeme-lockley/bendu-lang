package io.littlelanguages.bendu

import io.littlelanguages.bendu.compiler.Instructions
import java.io.File
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertTrue

class CompilerTest {
    @Test
    fun `assign array element`() {
        assertCompiledBC(
            byteArrayOf(
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 1, // 1
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 2, // 2
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 3, // 3
                Instructions.PUSH_ARRAY.op,
                0, 0, 0, 3, // 3
                Instructions.STORE.op,
                0, 0, 0, 0, // 0
                0, 0, 0, 0, // 0
                Instructions.LOAD.op,
                0, 0, 0, 0, // 0
                0, 0, 0, 0, // 0
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 1, // 1
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 20, // 20
                Instructions.STORE_ARRAY_ELEMENT.op,
            ),
            "let x = [1, 2, 3]; x!1 := 20"
        )
    }

    @Test
    fun `assign array range`() {
        assertCompiledBC(
            byteArrayOf(
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 1, // 1
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 2, // 2
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 3, // 3
                Instructions.PUSH_ARRAY.op,
                0, 0, 0, 3, // 3
                Instructions.STORE.op,
                0, 0, 0, 0, // 0
                0, 0, 0, 0, // 0
                Instructions.LOAD.op,
                0, 0, 0, 0, // 0
                0, 0, 0, 0, // 0
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 1, // 1
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 2, // 2
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 20, // 20
                Instructions.PUSH_ARRAY.op,
                0, 0, 0, 1, // 1
                Instructions.STORE_ARRAY_RANGE.op,
            ),
            "let x = [1, 2, 3]; x!1:2 := [20]"
        )

        assertCompiledBC(
            byteArrayOf(
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 1, // 1
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 2, // 2
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 3, // 3
                Instructions.PUSH_ARRAY.op,
                0, 0, 0, 3, // 3
                Instructions.STORE.op,
                0, 0, 0, 0, // 0
                0, 0, 0, 0, // 0
                Instructions.LOAD.op,
                0, 0, 0, 0, // 0
                0, 0, 0, 0, // 0
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 2, // 2
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 20, // 20
                Instructions.PUSH_ARRAY.op,
                0, 0, 0, 1, // 1
                Instructions.STORE_ARRAY_RANGE_TO.op,
            ),
            "let x = [1, 2, 3]; x!:2 := [20]"
        )

        assertCompiledBC(
            byteArrayOf(
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 1, // 1
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 2, // 2
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 3, // 3
                Instructions.PUSH_ARRAY.op,
                0, 0, 0, 3, // 3
                Instructions.STORE.op,
                0, 0, 0, 0, // 0
                0, 0, 0, 0, // 0
                Instructions.LOAD.op,
                0, 0, 0, 0, // 0
                0, 0, 0, 0, // 0
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 1, // 1
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 20, // 20
                Instructions.PUSH_ARRAY.op,
                0, 0, 0, 1, // 1
                Instructions.STORE_ARRAY_RANGE_FROM.op,
            ),
            "let x = [1, 2, 3]; x!1: := [20]"
        )

        assertCompiledBC(
            byteArrayOf(
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 1, // 1
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 2, // 2
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 3, // 3
                Instructions.PUSH_ARRAY.op,
                0, 0, 0, 3, // 3
                Instructions.STORE.op,
                0, 0, 0, 0, // 0
                0, 0, 0, 0, // 0
                Instructions.LOAD.op,
                0, 0, 0, 0, // 0
                0, 0, 0, 0, // 0
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 0, // 0
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 20, // 20
                Instructions.PUSH_ARRAY.op,
                0, 0, 0, 1, // 1
                Instructions.STORE_ARRAY_RANGE_FROM.op,
            ),
            "let x = [1, 2, 3]; x!: := [20]"
        )
    }

    @Test
    fun `get array element`() {
        assertCompiledBC(
            byteArrayOf(
                Instructions.PUSH_ARRAY.op,
                0, 0, 0, 0, // 0
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 5, // 5
                Instructions.PUSH_ARRAY_ELEMENT.op,
            ),
            "[]!5"
        )
    }

    @Test
    fun `get array range`() {
        assertCompiledBC(
            byteArrayOf(
                Instructions.PUSH_ARRAY.op,
                0, 0, 0, 0, // 0
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 2, // 2
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 5, // 5
                Instructions.PUSH_ARRAY_RANGE.op,
            ),
            "[]!2:5"
        )

        assertCompiledBC(
            byteArrayOf(
                Instructions.PUSH_ARRAY.op,
                0, 0, 0, 0, // 0
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 2, // 2
                Instructions.PUSH_ARRAY_RANGE_FROM.op,
            ),
            "[]!2:"
        )

        assertCompiledBC(
            byteArrayOf(
                Instructions.PUSH_ARRAY.op,
                0, 0, 0, 0, // 0
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 5, // 5
                Instructions.PUSH_ARRAY_RANGE_TO.op,
            ),
            "[]!:5"
        )
    }

    @Test
    fun `literal array`() {
        assertCompiledBC(
            byteArrayOf(
                Instructions.PUSH_ARRAY.op,
                0, 0, 0, 0, // 0
            ),
            "[]"
        )

        assertCompiledBC(
            byteArrayOf(
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 1, // 1
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 2, // 2
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 3, // 3
                Instructions.PUSH_ARRAY.op,
                0, 0, 0, 3, // 3
            ),
            "[1, 2, 3]"
        )

        assertCompiledBC(
            byteArrayOf(
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 1, // 1
                Instructions.PUSH_ARRAY.op,
                0, 0, 0, 1, // 1
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 20, // 20
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 30, // 30
                Instructions.PUSH_ARRAY.op,
                0, 0, 0, 2, // 2
                Instructions.ARRAY_APPEND_ARRAY.op,
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 3, // 3
                Instructions.ARRAY_APPEND_ELEMENT.op,
            ),
            "[1, ...[20, 30], 3]"
        )
    }

    @Test
    fun `literal bool`() {
        assertCompiledBC(
            byteArrayOf(Instructions.PUSH_BOOL_TRUE.op),
            "True"
        )

        assertCompiledBC(
            byteArrayOf(Instructions.PUSH_BOOL_FALSE.op),
            "False"
        )
    }

    @Test
    fun `literal char`() {
        assertCompiledBC(
            byteArrayOf(
                Instructions.PUSH_U8_LITERAL.op,
                120 // 'x'
            ),
            "'x'"
        )
    }

    @Test
    fun `literal float`() {
        assertCompiledBC(
            byteArrayOf(
                Instructions.PUSH_F32_LITERAL.op,
                64, 73, 15, -38 // 3.1415926
            ),
            "3.1415926"
        )
    }

    @Test
    fun `literal int`() {
        assertCompiledBC(
            byteArrayOf(
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 1 // 1
            ),
            "1"
        )

        assertCompiledBC(
            byteArrayOf(
                Instructions.PUSH_I32_LITERAL.op,
                -1, -1, -1, -1 // -1
            ),
            "-1"
        )
    }

    @Test
    fun `literal string`() {
        assertCompiledBC(
            byteArrayOf(
                Instructions.PUSH_STRING_LITERAL.op,
                0, 0, 0, 5, // 5
                'H'.code.toByte(), 'e'.code.toByte(), 'l'.code.toByte(), 'l'.code.toByte(), 'o'.code.toByte()
            ),
            "\"Hello\""
        )
    }

    @Test
    fun `literal tuple`() {
        assertCompiledBC(
            byteArrayOf(
                Instructions.PUSH_F32_LITERAL.op,
                64, 73, 6, 37, // 3.141
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 1, // 1
                Instructions.PUSH_TUPLE.op,
                0, 0, 0, 2, // 2
            ),
            "(3.141, 1)"
        )
    }

    @Test
    fun `let x = 1 let y = x`() {
        assertCompiledBC(
            byteArrayOf(
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 1, // 1
                Instructions.STORE.op,
                0, 0, 0, 0, // 0
                0, 0, 0, 0, // x
                Instructions.LOAD.op,
                0, 0, 0, 0, // 0
                0, 0, 0, 0, // x
                Instructions.STORE.op,
                0, 0, 0, 0, // 0
                0, 0, 0, 1, // x
                Instructions.PUSH_UNIT_LITERAL.op,
            ),
            "let x = 1 ; let y = x"
        )
    }

    @Test
    fun `print( dotdotdot )`() {
        assertCompiledBC(
            byteArrayOf(
                Instructions.PUSH_UNIT_LITERAL.op,
            ),
            "print()"
        )

        assertCompiledBC(
            byteArrayOf(
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 1, // 1
                Instructions.PUSH_BOOL_TRUE.op,
                Instructions.PUSH_TUPLE.op,
                0, 0, 0, 2, // 2
                Instructions.PRINT.op,
                Instructions.PUSH_UNIT_LITERAL.op,
            ),
            "print((1, True))"
        )
    }

    @Test
    fun `println( dotdotdot )`() {
        assertCompiledBC(
            byteArrayOf(
                Instructions.PRINTLN.op,
                Instructions.PUSH_UNIT_LITERAL.op,
            ),
            "println()"
        )

        assertCompiledBC(
            byteArrayOf(
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 1, // 1
                Instructions.PRINT_I32.op,
                Instructions.PUSH_BOOL_FALSE.op,
                Instructions.PRINT_BOOL.op,
                Instructions.PUSH_U8_LITERAL.op,
                120, // 'x'
                Instructions.PRINT_U8.op,
                Instructions.PUSH_F32_LITERAL.op,
                64, 73, 6, 37, // 3.141
                Instructions.PRINT_F32.op,
                Instructions.PUSH_STRING_LITERAL.op,
                0, 0, 0, 2, // 2
                'h'.code.toByte(), 'i'.code.toByte(),
                Instructions.PRINT_STRING.op,
                Instructions.PUSH_UNIT_LITERAL.op,
                Instructions.PRINT_UNIT.op,
                Instructions.PRINTLN.op,
                Instructions.PUSH_UNIT_LITERAL.op,
            ),
            "println(1, False, 'x', 3.141, \"hi\", ())"
        )
    }

    @Test
    fun `v op v`() {
        listOf(
            Pair("'x' == 'y'", Instructions.EQ_U8),
            Pair("'x' != 'y'", Instructions.NEQ_U8),
            Pair("'x' < 'y'", Instructions.LT_U8),
            Pair("'x' <= 'y'", Instructions.LE_U8),
            Pair("'x' > 'y'", Instructions.GT_U8),
            Pair("'x' >= 'y'", Instructions.GE_U8),
            Pair("'x' + 'y'", Instructions.ADD_U8),
            Pair("'x' - 'y'", Instructions.SUB_U8),
            Pair("'x' * 'y'", Instructions.MUL_U8),
            Pair("'x' / 'y'", Instructions.DIV_U8),
        ).forEach { input ->
            assertCompiledBC(
                byteArrayOf(
                    Instructions.PUSH_U8_LITERAL.op,
                    120, // 'x'
                    Instructions.PUSH_U8_LITERAL.op,
                    121, // 'y'
                    input.second.op,
                ),
                input.first
            )
        }

        listOf(
            Pair("1.0 == 2.0", Instructions.EQ_F32),
            Pair("1.0 != 2.0", Instructions.NEQ_F32),
            Pair("1.0 < 2.0", Instructions.LT_F32),
            Pair("1.0 <= 2.0", Instructions.LE_F32),
            Pair("1.0 > 2.0", Instructions.GT_F32),
            Pair("1.0 >= 2.0", Instructions.GE_F32),
            Pair("1.0 + 2.0", Instructions.ADD_F32),
            Pair("1.0 - 2.0", Instructions.SUB_F32),
            Pair("1.0 * 2.0", Instructions.MUL_F32),
            Pair("1.0 / 2.0", Instructions.DIV_F32),
            Pair("1.0 ** 2.0", Instructions.POW_F32),
        ).forEach { input ->
            assertCompiledBC(
                byteArrayOf(
                    Instructions.PUSH_F32_LITERAL.op,
                    63, -128, 0, 0, // 1.0
                    Instructions.PUSH_F32_LITERAL.op,
                    64, 0, 0, 0, // 2
                    input.second.op,
                ),
                input.first
            )
        }

        listOf(
            Pair("1 == 2", Instructions.EQ_I32),
            Pair("1 != 2", Instructions.NEQ_I32),
            Pair("1 < 2", Instructions.LT_I32),
            Pair("1 <= 2", Instructions.LE_I32),
            Pair("1 > 2", Instructions.GT_I32),
            Pair("1 >= 2", Instructions.GE_I32),
            Pair("1 + 2", Instructions.ADD_I32),
            Pair("1 - 2", Instructions.SUB_I32),
            Pair("1 * 2", Instructions.MUL_I32),
            Pair("1 / 2", Instructions.DIV_I32),
            Pair("1 % 2", Instructions.MOD_I32),
            Pair("1 ** 2", Instructions.POW_I32),
        ).forEach { input ->
            assertCompiledBC(
                byteArrayOf(
                    Instructions.PUSH_I32_LITERAL.op,
                    0, 0, 0, 1, // 1
                    Instructions.PUSH_I32_LITERAL.op,
                    0, 0, 0, 2, // 2
                    input.second.op,
                ),
                input.first
            )
        }

        listOf(
            Pair("\"a\" == \"b\"", Instructions.EQ_STRING),
            Pair("\"a\" != \"b\"", Instructions.NEQ_STRING),
            Pair("\"a\" < \"b\"", Instructions.LT_STRING),
            Pair("\"a\" <= \"b\"", Instructions.LE_STRING),
            Pair("\"a\" > \"b\"", Instructions.GT_STRING),
            Pair("\"a\" >= \"b\"", Instructions.GE_STRING),
            Pair("\"a\" + \"b\"", Instructions.ADD_STRING),
        ).forEach { input ->
            assertCompiledBC(
                byteArrayOf(
                    Instructions.PUSH_STRING_LITERAL.op,
                    0, 0, 0, 1, // 1
                    'a'.code.toByte(),
                    Instructions.PUSH_STRING_LITERAL.op,
                    0, 0, 0, 1, // 1
                    'b'.code.toByte(),
                    input.second.op,
                ),
                input.first
            )
        }

        listOf(
            Pair("() == ()", Instructions.EQ_UNIT),
            Pair("() != ()", Instructions.NEQ_UNIT),
            Pair("() < ()", Instructions.NEQ_UNIT),
            Pair("() <= ()", Instructions.EQ_UNIT),
            Pair("() > ()", Instructions.NEQ_UNIT),
            Pair("() >= ()", Instructions.EQ_UNIT),
        ).forEach { input ->
            assertCompiledBC(
                byteArrayOf(
                    Instructions.PUSH_UNIT_LITERAL.op,
                    Instructions.PUSH_UNIT_LITERAL.op,
                    input.second.op,
                ),
                input.first
            )
        }
        listOf(
            Pair("True == False", Instructions.EQ_BOOL),
            Pair("True != False", Instructions.NEQ_BOOL),
        ).forEach { input ->
            assertCompiledBC(
                byteArrayOf(
                    Instructions.PUSH_BOOL_TRUE.op,
                    Instructions.PUSH_BOOL_FALSE.op,
                    input.second.op,
                ),
                input.first
            )
        }

        assertCompiledBC(
            byteArrayOf(
                Instructions.PUSH_BOOL_TRUE.op,
                Instructions.JMP_DUP_FALSE.op,
                0, 0, 0, 8,
                Instructions.DISCARD.op,
                Instructions.PUSH_BOOL_TRUE.op,
            ),
            "True && True"
        )

        assertCompiledBC(
            byteArrayOf(
                Instructions.PUSH_BOOL_TRUE.op,
                Instructions.JMP_DUP_TRUE.op,
                0, 0, 0, 8,
                Instructions.DISCARD.op,
                Instructions.PUSH_BOOL_TRUE.op,
            ),
            "True || True"
        )

        listOf(
            "True + False",
            "False < True"
        ).forEach { input ->
            unsuccessfulCompile(input)
        }

        assertCompiledBC(
            byteArrayOf(
                Instructions.PUSH_ARRAY.op,
                0, 0, 0, 0,
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 1,
                Instructions.ARRAY_APPEND_ELEMENT_DUPLICATE.op
            ),
            "[] << 1"
        )

        assertCompiledBC(
            byteArrayOf(
                Instructions.PUSH_ARRAY.op,
                0, 0, 0, 0,
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 1,
                Instructions.ARRAY_APPEND_ELEMENT.op
            ),
            "[] <! 1"
        )

        assertCompiledBC(
            byteArrayOf(
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 1,
                Instructions.PUSH_ARRAY.op,
                0, 0, 0, 0,
                Instructions.ARRAY_PREPEND_ELEMENT_DUPLICATE.op
            ),
            "1 >> []"
        )

        assertCompiledBC(
            byteArrayOf(
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 1,
                Instructions.PUSH_ARRAY.op,
                0, 0, 0, 0,
                Instructions.ARRAY_PREPEND_ELEMENT.op
            ),
            "1 >! []"
        )
    }

    @Test
    fun `op v`() {
        assertCompiledBC(
            byteArrayOf(
                Instructions.PUSH_BOOL_TRUE.op,
                Instructions.NOT_BOOL.op,
            ),
            "!True"
        )

    }

    @Test
    fun `if expression`() {
        assertCompiledBC(
            byteArrayOf(
                Instructions.PUSH_BOOL_TRUE.op,
                Instructions.JMP_FALSE.op,
                0, 0, 0, 16,
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 1, // 1
                Instructions.JMP.op,
                0, 0, 0, 21,
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 2, // 2
            ),
            "if True -> 1 | 2"
        )

        assertCompiledBC(
            byteArrayOf(
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 10, // 10
                Instructions.STORE.op,
                0, 0, 0, 0, // 0
                0, 0, 0, 0, // 0
                Instructions.LOAD.op,
                0, 0, 0, 0, // 0
                0, 0, 0, 0, // x
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 1, // 1
                Instructions.EQ_I32.op,
                Instructions.JMP_FALSE.op,
                0, 0, 0, 44,
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 10, // 10
                Instructions.JMP.op,
                0, 0, 0, 79,
                Instructions.LOAD.op,
                0, 0, 0, 0, // 0
                0, 0, 0, 0, // x
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 2, // 1
                Instructions.EQ_I32.op,
                Instructions.JMP_FALSE.op,
                0, 0, 0, 74,
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 20, // 20
                Instructions.JMP.op,
                0, 0, 0, 79,
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 30, // 30
            ),
            "let x = 10 ; if x == 1 -> 10 | x == 2 -> 20 | 30"
        )
    }

    @Test
    fun functions() {
        assertCompiledBC(
            byteArrayOf(
                Instructions.JMP.op,
                0, 0, 0, 21,
                Instructions.LOAD.op,
                0, 0, 0, 0,
                0, 0, 0, 0, // a
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 1,
                Instructions.ADD_I32.op,
                Instructions.RET.op,
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 1,
                Instructions.CALL.op,
                0, 0, 0, 5, // offset: 5
                0, 0, 0, 1, // arity: 1
                0, 0, 0, 0, // depth: 0
            ),
            "let inc(a) = a + 1 ; inc(1)"
        )

        assertCompiledBC(
            byteArrayOf(
                Instructions.JMP.op,
                0, 0, 0, 34,
                Instructions.LOAD.op,
                0, 0, 0, 0,
                0, 0, 0, 0, // a
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 1,
                Instructions.ADD_I32.op,
                Instructions.CALL.op,
                0, 0, 0, 5, // offset: 5
                0, 0, 0, 1, // arity: 1
                0, 0, 0, 1, // depth: 1
                Instructions.RET.op,
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 1,
                Instructions.CALL.op,
                0, 0, 0, 5, // offset: 5
                0, 0, 0, 1, // arity: 1
                0, 0, 0, 0, // depth: 0
            ),
            "let inc(a) = inc(a + 1) ; inc(1)"
        )

        assertCompiledBC(
            byteArrayOf(
                Instructions.JMP.op,
                0, 0, 0, 21,
                Instructions.LOAD.op,
                0, 0, 0, 0,
                0, 0, 0, 1,
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 1,
                Instructions.ADD_I32.op,
                Instructions.RET.op,
                Instructions.PUSH_UNIT_LITERAL.op
            ),
            "let add(_, a) = a + 1"
        )

        assertCompiledBC(
            byteArrayOf(
                Instructions.JMP.op,
                0, 0, 0, 73,
                Instructions.LOAD.op,
                0, 0, 0, 0,
                0, 0, 0, 0,
                Instructions.DUP.op,
                Instructions.PUSH_TUPLE_COMPONENT.op,
                0, 0, 0, 0,
                Instructions.STORE.op,
                0, 0, 0, 0,
                0, 0, 0, 2,
                Instructions.PUSH_TUPLE_COMPONENT.op,
                0, 0, 0, 1,
                Instructions.STORE.op,
                0, 0, 0, 0,
                0, 0, 0, 3,
                Instructions.LOAD.op,
                0, 0, 0, 0,
                0, 0, 0, 2,
                Instructions.LOAD.op,
                0, 0, 0, 0,
                0, 0, 0, 3,
                Instructions.ADD_I32.op,
                Instructions.LOAD.op,
                0, 0, 0, 0,
                0, 0, 0, 1,
                Instructions.ADD_I32.op,
                Instructions.RET.op,
                Instructions.PUSH_UNIT_LITERAL.op
            ),
            "let add((a, b): Int * Int, c: Int): Int = a + b + c"
        )
    }

    @Test
    fun `import all`() {
        assertCompiledBC(
            byteArrayOf(
                Instructions.LOAD_PACKAGE.op,
                -1, -1, -1, -1, // -1
                0, 0, 0, 0, // valueA
            ),
            "import \"test/test.bendu\" ; valueA"
        )

        assertCompiledBC(
            byteArrayOf(
                Instructions.LOAD_PACKAGE.op,
                -1, -1, -1, -1, // -1
                0, 0, 0, 1, // valueB
            ),
            "import \"test/test.bendu\" ; valueB"
        )

        assertCompiledBC(
            byteArrayOf(
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 1, // 1
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 2, // 2
                Instructions.CALL_PACKAGE.op,
                -1, -1, -1, -1, // -1
                0, 0, 0, 19, // funA
                0, 0, 0, 2, // 2
            ),
            "import \"test/test.bendu\" ; funA(1, 2)"
        )

        assertCompiledBC(
            byteArrayOf(
                Instructions.LOAD_PACKAGE.op,
                -1, -1, -1, -1, // -1
                0, 0, 0, 2, // funB
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 1, // 1
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 2, // 2
                Instructions.CALL_CLOSURE.op,
                0, 0, 0, 2, // 2
            ),
            "import \"test/test.bendu\"; funB(1, 2)"
        )

        assertCompiledBC(
            byteArrayOf(
                Instructions.PUSH_PACKAGE_CLOSURE.op,
                -1, -1, -1, -1, // -1
                0, 0, 0, 19, // funA
            ),
            "import \"test/test.bendu\" ; funA"
        )

        assertCompiledBC(
            byteArrayOf(
                Instructions.LOAD_PACKAGE.op,
                -1, -1, -1, -1, // -1
                0, 0, 0, 2, // funB
            ),
            "import \"test/test.bendu\"; funB"
        )
    }

    @Test
    fun `import ID`() {
        assertCompiledBC(
            byteArrayOf(
                Instructions.LOAD_PACKAGE.op,
                -1, -1, -1, -1, // -1
                0, 0, 0, 0, // valueA
            ),
            "import \"test/test.bendu\" as T ; T.valueA"
        )

        assertCompiledBC(
            byteArrayOf(
                Instructions.LOAD_PACKAGE.op,
                -1, -1, -1, -1, // -1
                0, 0, 0, 1, // valueB
            ),
            "import \"test/test.bendu\" as T ; T.valueB"
        )

        assertCompiledBC(
            byteArrayOf(
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 1, // 1
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 2, // 2
                Instructions.CALL_PACKAGE.op,
                -1, -1, -1, -1, // -1
                0, 0, 0, 19, // funA
                0, 0, 0, 2, // 2
            ),
            "import \"test/test.bendu\" as T; T.funA(1, 2)"
        )

        assertCompiledBC(
            byteArrayOf(
                Instructions.LOAD_PACKAGE.op,
                -1, -1, -1, -1, // -1
                0, 0, 0, 2, // funB
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 1, // 1
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 2, // 2
                Instructions.CALL_CLOSURE.op,
                0, 0, 0, 2, // 2
            ),
            "import \"test/test.bendu\" as T; T.funB(1, 2)"
        )

        assertCompiledBC(
            byteArrayOf(
                Instructions.PUSH_PACKAGE_CLOSURE.op,
                -1, -1, -1, -1, // -1
                0, 0, 0, 19, // funA
            ),
            "import \"test/test.bendu\" as T; T.funA"
        )

        assertCompiledBC(
            byteArrayOf(
                Instructions.LOAD_PACKAGE.op,
                -1, -1, -1, -1, // -1
                0, 0, 0, 2, // funB
            ),
            "import \"test/test.bendu\" as T; T.funB"
        )
    }

    @Test
    fun `import exposing`() {
        assertCompiledBC(
            byteArrayOf(
                Instructions.LOAD_PACKAGE.op,
                -1, -1, -1, -1, // -1
                0, 0, 0, 0, // valueA
            ),
            "import \"test/test.bendu\" exposing (valueA); valueA"
        )

        assertCompiledBC(
            byteArrayOf(
                Instructions.LOAD_PACKAGE.op,
                -1, -1, -1, -1, // -1
                0, 0, 0, 1, // valueB
            ),
            "import \"test/test.bendu\" exposing (valueB); valueB"
        )

        assertCompiledBC(
            byteArrayOf(
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 1, // 1
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 2, // 2
                Instructions.CALL_PACKAGE.op,
                -1, -1, -1, -1, // -1
                0, 0, 0, 19, // funA
                0, 0, 0, 2, // 2
            ),
            "import \"test/test.bendu\" exposing (funA); funA(1, 2)"
        )

        assertCompiledBC(
            byteArrayOf(
                Instructions.LOAD_PACKAGE.op,
                -1, -1, -1, -1, // -1
                0, 0, 0, 2, // funB
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 1, // 1
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 2, // 2
                Instructions.CALL_CLOSURE.op,
                0, 0, 0, 2, // 2
            ),
            "import \"test/test.bendu\" exposing (funB); funB(1, 2)"
        )

        assertCompiledBC(
            byteArrayOf(
                Instructions.PUSH_PACKAGE_CLOSURE.op,
                -1, -1, -1, -1, // -1
                0, 0, 0, 19, // funA
            ),
            "import \"test/test.bendu\" exposing (funA); funA"
        )

        assertCompiledBC(
            byteArrayOf(
                Instructions.LOAD_PACKAGE.op,
                -1, -1, -1, -1, // -1
                0, 0, 0, 2, // funB
            ),
            "import \"test/test.bendu\" exposing (funB); funB"
        )
    }

    @Test
    fun `import exposing alias`() {
        assertCompiledBC(
            byteArrayOf(
                Instructions.LOAD_PACKAGE.op,
                -1, -1, -1, -1, // -1
                0, 0, 0, 0, // valueA
            ),
            "import \"test/test.bendu\" exposing (valueA as fff); fff"
        )

        assertCompiledBC(
            byteArrayOf(
                Instructions.LOAD_PACKAGE.op,
                -1, -1, -1, -1, // -1
                0, 0, 0, 1, // valueB
            ),
            "import \"test/test.bendu\" exposing (valueB as fff); fff"
        )

        assertCompiledBC(
            byteArrayOf(
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 1, // 1
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 2, // 2
                Instructions.CALL_PACKAGE.op,
                -1, -1, -1, -1, // -1
                0, 0, 0, 19, // funA
                0, 0, 0, 2, // 2
            ),
            "import \"test/test.bendu\" exposing (funA as fff); fff(1, 2)"
        )

        assertCompiledBC(
            byteArrayOf(
                Instructions.LOAD_PACKAGE.op,
                -1, -1, -1, -1, // -1
                0, 0, 0, 2, // funB
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 1, // 1
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 2, // 2
                Instructions.CALL_CLOSURE.op,
                0, 0, 0, 2, // 2
            ),
            "import \"test/test.bendu\" exposing (funB as fff); fff(1, 2)"
        )

        assertCompiledBC(
            byteArrayOf(
                Instructions.PUSH_PACKAGE_CLOSURE.op,
                -1, -1, -1, -1, // -1
                0, 0, 0, 19, // funA
            ),
            "import \"test/test.bendu\" exposing (funA as fff); fff"
        )

        assertCompiledBC(
            byteArrayOf(
                Instructions.LOAD_PACKAGE.op,
                -1, -1, -1, -1, // -1
                0, 0, 0, 2, // funB
            ),
            "import \"test/test.bendu\" exposing (funB as fff); fff"
        )
    }

    @Test
    fun `assign import ID declaration`() {
        assertCompiledBC(
            byteArrayOf(
                Instructions.PUSH_I32_LITERAL.op,
                0, 0, 0, 30, // valueA
                Instructions.DUP.op, // at a top-level we keep the result
                Instructions.STORE_PACKAGE.op,
                -1, -1, -1, -1, // -1
                0, 0, 0, 1, // valueB
            ),
            "import \"test/test.bendu\" as T; T.valueB := 30"
        )
    }
}

private fun successfulCompile(input: String): ByteArray {
    val entry = CacheManager.useExpression(File("test.bc"), input)
    val errors = Errors()
    val statements = infer(entry, errors = errors)

    val result = compile(entry, statements, errors)

    entry.writeImage(result)

    assertTrue(errors.hasNoErrors())

    return result.bytecode
}

private fun unsuccessfulCompile(input: String) {
    val errors = Errors()
    val entry = CacheManager.useExpression(File("test.bc"), input)
    val statements = infer(entry, errors = errors)
    compile(entry, statements, errors)

    assertTrue(errors.hasErrors())
}

private fun assertCompiledBC(expected: ByteArray, input: String) {
    assertContentEquals(expected, successfulCompile(input))
}