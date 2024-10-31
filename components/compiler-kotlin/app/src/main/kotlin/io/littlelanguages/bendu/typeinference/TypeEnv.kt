package io.littlelanguages.bendu.typeinference

data class TypeEnv(private val items: Map<String, Scheme>) {
    private var ftv: Set<Var>? = null

    fun extend(name: String, scheme: Scheme): TypeEnv =
        TypeEnv(items + Pair(name, scheme))

    operator fun plus(v: Pair<String, Scheme>): TypeEnv =
        this.extend(v.first, v.second)

    operator fun plus(v: List<Pair<String, Scheme>>): TypeEnv =
        v.fold(this) { acc, p -> acc + p }

    fun apply(s: Subst): TypeEnv =
        TypeEnv(items.mapValues { it.value.apply(s) })

    operator fun get(name: String): Scheme? = items[name]

    fun generalise(type: Type): Scheme {
        val typeFtv = type.ftv()

        if (typeFtv.isEmpty()) {
            return Scheme(emptySet(), type)
        }

        if (ftv == null) {
            ftv = items.toList().flatMap { it.second.ftv() }.toSet()
        }

        return Scheme(typeFtv - ftv!!, type)
    }
}

val emptyTypeEnv = TypeEnv(emptyMap())
