package io.littlelanguages.bendu.compiler

enum class Instructions(val op: Byte) {
    PUSH_I32_LITERAL(0),
    PUSH_I32_STACK(1),
    ADD_I32(2),
    SUB_I32(3),
    MUL_I32(4),
    DIV_I32(5),
    MOD_I32(6),
    POW_I32(7),
    PRINT_I32(8),
    PRINTLN(9),
}
