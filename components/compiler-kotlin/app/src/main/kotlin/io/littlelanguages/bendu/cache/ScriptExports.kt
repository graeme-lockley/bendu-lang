package io.littlelanguages.bendu.cache

import io.littlelanguages.bendu.typeinference.*

class ScriptExports(val exports: List<ScriptExport>)

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

data class CustomTypeExport(
    override val name: String,
    override val mutable: Boolean,
    val parameters: List<Var>,
    val constructors: List<ConstructorExport>
) : ScriptExport(name, mutable) {
    override fun toString(): String {
        val env = ToStringHelper()
        val ps = if (parameters.isEmpty()) "" else "[${parameters.joinToString(", ") { env.variable(it) }}]"

        return if (constructors.isEmpty())
            "type $name$ps"
        else
            "type $name$ps = ${constructors.joinToString(" | ") { it.toStringHelper(env) }}"
    }

    fun constructorExports(): List<FunctionExport> {
        val returnType = parameters.map { p -> TVar(p) }

        return constructors.map { c ->
            FunctionExport(
                c.name,
                false,
                Scheme(parameters.toSet(), TArr(c.parameters, TCon(name, returnType))),
                c.codeOffset,
                null
            )
        }
    }
}

data class ConstructorExport(val name: String, val parameters: List<Type>, val codeOffset: Int) {
    fun toStringHelper(env: ToStringHelper): String =
        if (parameters.isEmpty())
            "$name = $codeOffset"
        else
            "$name[${parameters.joinToString(", ") { it.toStringHelper(env) }}] = $codeOffset"
}