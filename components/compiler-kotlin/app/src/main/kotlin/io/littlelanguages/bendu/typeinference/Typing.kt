package io.littlelanguages.bendu.typeinference

typealias Var = Int

sealed class Type {
    abstract fun apply(s: Subst): Type
    abstract fun ftv(): Set<Var>

    open fun isBool(): Boolean = false
    open fun isInt(): Boolean = false
}

data class TVar(val name: Var) : Type() {
    override fun apply(s: Subst): Type =
        s[name] ?: this

    override fun ftv(): Set<Var> =
        setOf(name)

    override fun toString(): String = "'$name"
}

data class TCon(val name: String, val args: List<Type> = emptyList()) : Type() {
    override fun apply(s: Subst): Type =
        if (args.isEmpty()) this else TCon(name, args.map { it.apply(s) })

    override fun ftv(): Set<Var> = args.fold(emptySet()) { acc, t -> acc + t.ftv() }

    override fun toString(): String = if (args.isEmpty()) name else "$name ${
        args.joinToString(" ") { if (it is TCon && it.args.isNotEmpty() || it is TArr) "($it)" else "$it" }
    }"

    override fun isBool(): Boolean =
        name == "Bool"

    override fun isInt(): Boolean =
        name == "Int"
}

data class TTuple(val types: List<Type>) : Type() {
    override fun apply(s: Subst): Type =
        TTuple(types.map { it.apply(s) })

    override fun ftv(): Set<Var> =
        types.fold(emptySet()) { acc, t -> acc + t.ftv() }

    override fun toString(): String = "(${types.joinToString(" * ")})"
}

data class TArr(val domain: Type, val range: Type) : Type() {
    override fun apply(s: Subst): Type =
        TArr(domain.apply(s), range.apply(s))

    override fun ftv(): Set<Var> =
        domain.ftv() + range.ftv()

    override fun toString(): String =
        if (domain is TArr) "($domain) -> $range" else "$domain -> $range"
}

val typeError = TCon("Error")
val typeInt = TCon("Int")
val typeBool = TCon("Bool")
