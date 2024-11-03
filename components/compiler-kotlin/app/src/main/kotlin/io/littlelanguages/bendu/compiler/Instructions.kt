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
    EQ_I32(10),
    NEQ_I32(11),
    LT_I32(12),
    LE_I32(13),
    GT_I32(14),
    GE_I32(15),
    PRINTLN(16),
    PRINT_BOOL(17),
    PRINT_I32(18),
}
