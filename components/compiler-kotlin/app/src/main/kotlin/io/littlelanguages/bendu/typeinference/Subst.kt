package io.littlelanguages.bendu.typeinference

data class Subst(private val items: Map<Var, Type>) {
    infix fun compose(s: Subst): Subst =
        Subst(s.items.mapValues { it.value.apply(this) } + items)

    operator fun get(v: Var): Type? = items[v]

    operator fun minus(names: Set<Var>): Subst =
        Subst(items - names)
}

val nullSubst = Subst(emptyMap())
