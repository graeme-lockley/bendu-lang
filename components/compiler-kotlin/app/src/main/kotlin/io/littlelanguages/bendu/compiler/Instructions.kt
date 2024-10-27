package io.littlelanguages.bendu.compiler

enum class Instructions(val op: Byte) {
    PUSH_I32_LITERAL(0),
    PUSH_I32_STACK(1)
}
