package io.littlelanguages.bendu.compiler

enum class Instructions(val op: Byte) {
    PUSH_BOOL_TRUE(0),
    PUSH_BOOL_FALSE(1),
    PUSH_I32_LITERAL(2),
    PUSH_I32_STACK(3),
    ADD_I32(4),
    SUB_I32(5),
    MUL_I32(6),
    DIV_I32(7),
    MOD_I32(8),
    POW_I32(9),
    PRINT_I32(10),
    PRINTLN(11),
}
