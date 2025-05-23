package io.littlelanguages.bendu.typeinference

import io.littlelanguages.scanpiler.Location

data class TypeEnv(private val items: Map<String, Binding>) {
    private var ftv: Set<Var>? = null

    private fun extend(name: String, binding: Binding): TypeEnv =
        TypeEnv(items + Pair(name, binding))

    operator fun plus(v: Pair<String, Binding>): TypeEnv =
        this.extend(v.first, v.second)

    operator fun plus(v: List<Pair<String, Binding>>): TypeEnv =
        v.fold(this) { acc, p -> acc + p }

    operator fun get(name: String): Binding? = items[name]

    fun generalise(type: Type): Scheme {
        val typeFtv = type.ftv()

        if (typeFtv.isEmpty()) {
            return Scheme(emptySet(), type)
        }

        if (ftv == null) {
            ftv = items.toList().flatMap { it.second.scheme.ftv() }.toSet()
        }

        return Scheme(typeFtv - ftv!!, type)
    }
}

data class Binding(val location: Location, val mutable: Boolean, val scheme: Scheme)

val emptyTypeEnv = TypeEnv(emptyMap())
