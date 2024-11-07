package io.littlelanguages.bendu.typeinference

import io.littlelanguages.scanpiler.Location

typealias Var = Int

sealed class Type(open val location: Location?) {
    abstract fun apply(s: Subst): Type
    abstract fun ftv(): Set<Var>

    abstract fun withLocation(location: Location?): Type

    abstract fun isSimilar(other: Type): Boolean

    open fun isBool(): Boolean = false
    open fun isInt(): Boolean = false
}

data class TVar(val name: Var, override val location: Location? = null) : Type(location) {
    override fun apply(s: Subst): Type =
        s[name] ?: this

    override fun ftv(): Set<Var> =
        setOf(name)

    override fun withLocation(location: Location?): Type =
        TVar(name, location)

    override fun isSimilar(other: Type): Boolean =
        other is TVar && other.name == name

    override fun toString(): String = "'$name"
}

data class TCon(val name: String, val args: List<Type> = emptyList(), override val location: Location? = null) :
    Type(location) {
    override fun apply(s: Subst): Type =
        if (args.isEmpty()) this else TCon(name, args.map { it.apply(s) })

    override fun ftv(): Set<Var> = args.fold(emptySet()) { acc, t -> acc + t.ftv() }

    override fun withLocation(location: Location?): Type =
        TCon(name, args, location)

    override fun isSimilar(other: Type): Boolean =
        other is TCon && other.name == name && other.args.size == args.size && args.zip(other.args).all { (a, b) -> a.isSimilar(b) }

    override fun toString(): String = if (args.isEmpty()) name else "$name ${
        args.joinToString(" ") { if (it is TCon && it.args.isNotEmpty() || it is TArr) "($it)" else "$it" }
    }"

    override fun isBool(): Boolean =
        name == "Bool"

    override fun isInt(): Boolean =
        name == "Int"
}

data class TTuple(val types: List<Type>, override val location: Location? = null) : Type(location) {
    override fun apply(s: Subst): Type =
        TTuple(types.map { it.apply(s) })

    override fun ftv(): Set<Var> =
        types.fold(emptySet()) { acc, t -> acc + t.ftv() }

    override fun withLocation(location: Location?): Type =
        TTuple(types, location)

    override fun isSimilar(other: Type): Boolean =
        other is TTuple && other.types.size == types.size && types.zip(other.types).all { (a, b) -> a.isSimilar(b) }

    override fun toString(): String = "(${types.joinToString(" * ")})"
}

data class TArr(val domain: Type, val range: Type, override val location: Location? = null) : Type(location) {
    override fun apply(s: Subst): Type =
        TArr(domain.apply(s), range.apply(s))

    override fun ftv(): Set<Var> =
        domain.ftv() + range.ftv()

    override fun withLocation(location: Location?): Type =
        TArr(domain, range, location)

    override fun isSimilar(other: Type): Boolean =
        other is TArr && domain.isSimilar(other.domain) && range.isSimilar(other.range)

    override fun toString(): String =
        if (domain is TArr) "($domain) -> $range" else "$domain -> $range"
}

val typeError = TCon("Error")
val typeBool = TCon("Bool")
val typeChar = TCon("Char")
val typeFloat = TCon("Float")
val typeInt = TCon("Int")
val typeString = TCon("String")
