package io.littlelanguages.bendu.compiler

enum class Instructions(val op: Int) {
    PUSH_I32_LITERAL(1),
    PUSH_I32_STACK(2)
}
