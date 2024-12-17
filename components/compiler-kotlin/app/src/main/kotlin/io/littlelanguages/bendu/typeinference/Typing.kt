package io.littlelanguages.bendu.typeinference

import io.littlelanguages.scanpiler.Location

typealias Var = Int

data class ToStringHelper(
    private val bindings: MutableMap<Var, String> = mutableMapOf(),
    private var counter: Int = 0
) {
    fun names(): List<String> =
        bindings.entries.map { it.value }.sorted()

    fun variable(name: Var): String {
        val v = bindings[name]

        if (v == null) {
            val newV = if (counter < 26)
                ('a' + counter).toString()
            else
                "t$counter"

            counter += 1
            bindings[name] = newV
            return newV
        } else
            return v
    }
}

sealed class Type(open val location: Location?) {
    abstract fun apply(s: Subst): Type
    abstract fun ftv(): Set<Var>

    abstract fun withLocation(location: Location?): Type

    abstract fun isSimilar(other: Type): Boolean

    open fun isBool(): Boolean = false
    open fun isChar(): Boolean = false
    open fun isError(): Boolean = false
    open fun isFloat(): Boolean = false
    open fun isFunction(): Boolean = false
    open fun isInt(): Boolean = false
    open fun isString(): Boolean = false
    open fun isUnit(): Boolean = false

    override fun toString(): String {
        val helper = ToStringHelper()
        val type = toStringHelper(helper)

        val names = helper.names()
        return if (names.isEmpty())
            type
        else
            "[${names.joinToString(", ")}] $type"
    }

    abstract fun toStringHelper(env: ToStringHelper): String
}

data class TArr(val domain: List<Type>, val range: Type, override val location: Location? = null) : Type(location) {
    override fun apply(s: Subst): Type =
        TArr(domain.map { it.apply(s) }, range.apply(s))

    override fun ftv(): Set<Var> =
        domain.map { it.ftv() }.fold(emptySet<Var>()) { a, b -> a + b } + range.ftv()

    override fun withLocation(location: Location?): Type =
        TArr(domain, range, location)

    override fun isSimilar(other: Type): Boolean =
        other is TArr && domain.zip(other.domain).all { it.first.isSimilar(it.second) } && range.isSimilar(other.range)

    override fun toString(): String =
        super.toString()

    override fun toStringHelper(env: ToStringHelper): String =
        "(${domain.joinToString(", ") { it.toStringHelper(env) }}) -> ${range.toStringHelper(env)}"

    override fun isFunction(): Boolean =
        true
}

data class TCon(val name: String, val args: List<Type> = emptyList(), override val location: Location? = null) :
    Type(location) {
    override fun apply(s: Subst): Type =
        if (args.isEmpty()) this else TCon(name, args.map { it.apply(s) })

    override fun ftv(): Set<Var> = args.fold(emptySet()) { acc, t -> acc + t.ftv() }

    override fun withLocation(location: Location?): Type =
        TCon(name, args, location)

    override fun isSimilar(other: Type): Boolean =
        other is TCon && other.name == name && other.args.size == args.size && args.zip(other.args)
            .all { (a, b) -> a.isSimilar(b) }

    override fun toString(): String =
        super.toString()

    override fun toStringHelper(env: ToStringHelper): String =
        if (args.isEmpty())
            name
        else
            "$name[${args.joinToString(", ") { it.toStringHelper(env) }}]"

    override fun isBool(): Boolean =
        name == "Bool"

    override fun isChar(): Boolean =
        name == "Char"

    override fun isError(): Boolean =
        name == "Error"

    override fun isFloat(): Boolean =
        name == "Float"

    override fun isInt(): Boolean =
        name == "Int"

    override fun isString(): Boolean =
        name == "String"

    override fun isUnit(): Boolean =
        name == "Unit"
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

    override fun toString(): String =
        super.toString()

    override fun toStringHelper(env: ToStringHelper): String =
        types.joinToString(" * ") { if (it is TTuple) "(${it.toStringHelper(env)})" else it.toStringHelper(env) }
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

    override fun toString(): String =
        super.toString()

    override fun toStringHelper(env: ToStringHelper): String =
        env.variable(name)
}

val typeError = TCon("Error")
val typeBool = TCon("Bool")
val typeChar = TCon("Char")
val typeFloat = TCon("Float")
val typeInt = TCon("Int")
val typeString = TCon("String")
val typeUnit = TCon("Unit")
