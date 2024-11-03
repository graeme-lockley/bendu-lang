package io.littlelanguages.bendu.compiler

enum class Instructions(val op: Byte) {
    PUSH_BOOL_TRUE(0),
    PUSH_BOOL_FALSE(1),
    PUSH_I32_LITERAL(2),
    PUSH_I32_STACK(3),

    DISCARD(4),

    JMP_DUP_FALSE(5),
    JMP_DUP_TRUE(6),

    NOT_BOOL(7),

    ADD_I32(8),
    SUB_I32(9),
    MUL_I32(10),
    DIV_I32(11),
    MOD_I32(12),
    POW_I32(13),
    EQ_BOOL(14),
    EQ_I32(15),
    NEQ_BOOL(16),
    NEQ_I32(17),
    LT_I32(18),
    LE_I32(19),
    GT_I32(20),
    GE_I32(21),

    PRINTLN(22),
    PRINT_BOOL(23),
    PRINT_I32(24),
}
