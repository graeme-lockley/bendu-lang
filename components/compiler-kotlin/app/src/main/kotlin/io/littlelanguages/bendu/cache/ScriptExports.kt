package io.littlelanguages.bendu.cache

import io.littlelanguages.bendu.typeinference.*

class ScriptExports(val exports: List<ScriptExport>) {
    private val declarations = lazy { exports.associateBy { it.name } }
    private val constructors =
        lazy {
            exports.filterIsInstance<CustomTypeExport>().flatMap { it.constructors.map { c -> Pair(it, c) } }
                .associateBy { it.second.name }
        }

    fun find(name: String): ScriptExport? =
        declarations.value[name]

    fun findConstructor(name: String): Pair<CustomTypeExport, ConstructorExport>? =
        constructors.value[name]
}

sealed class ScriptExport(open val name: String, open val mutable: Boolean)

data class ValueExport(
    override val name: String,
    override val mutable: Boolean,
    val scheme: Scheme,
    val frameOffset: Int
) : ScriptExport(name, mutable) {
    override fun toString(): String {
        return "let $name${if (mutable) "!" else ""}: $scheme = $frameOffset"
    }
}

data class FunctionExport(
    override val name: String,
    override val mutable: Boolean,
    val scheme: Scheme,
    val codeOffset: Int,
    val frameOffset: Int?
) : ScriptExport(name, mutable) {
    override fun toString(): String {
        return "fn $name${if (mutable) "!" else ""}: $scheme = $codeOffset${if (frameOffset != null) " $frameOffset" else ""}"
    }
}

class CustomTypeExport(
    override val name: String,
    override val mutable: Boolean,
    val parameters: List<Var>,
    constructors: List<ConstructorExportData>
) : ScriptExport(name, mutable), CustomDataType {
    val constructors = constructors.map { ConstructorExport(this, it.name, it.parameters, it.codeOffset) }

    override fun toString(): String {
        val env = ToStringHelper()
        val ps = if (parameters.isEmpty()) "" else "[${parameters.joinToString(", ") { env.variable(it) }}]"

        return if (constructors.isEmpty())
            "type $name$ps"
        else
            "type $name$ps = ${constructors.joinToString(" | ") { it.toStringHelper(env) }}"
    }

    fun constructorExports(): ScriptExports {
        val returnType = parameters.map { p -> TVar(p) }

        return ScriptExports(constructors.map { c ->
            FunctionExport(
                c.name,
                false,
                Scheme(parameters.toSet(), TArr(c.parameters, TCon(name, returnType))),
                c.codeOffset,
                null
            )
        })
    }

    override fun parameters(): List<Var> =
        parameters

    override fun type(pump: Pump): Type =
        TCon(name, pump.nextN(parameters.size))
}

data class ConstructorExportData(val name: String, val parameters: List<Type>, val codeOffset: Int)

data class ConstructorExport(
    val customTypeExport: CustomTypeExport,
    override val name: String,
    val parameters: List<Type>,
    val codeOffset: Int
) : Constructor {
    fun toStringHelper(env: ToStringHelper): String =
        if (parameters.isEmpty())
            "$name = $codeOffset"
        else
            "$name[${parameters.joinToString(", ") { it.toStringHelper(env) }}] = $codeOffset"

    override fun constructors(): List<Constructor> =
        customTypeExport.constructors

    override fun parameters(): List<Type> =
        parameters

    override fun arity(): Int =
        parameters.size
}
