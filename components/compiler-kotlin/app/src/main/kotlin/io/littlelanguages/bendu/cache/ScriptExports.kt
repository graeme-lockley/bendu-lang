package io.littlelanguages.bendu.cache

import io.littlelanguages.bendu.typeinference.Type

class ScriptExports(val exports: List<ScriptExport>)

sealed class ScriptExport(open val name: String)

data class ValueExport(override val name: String, val mutable: Boolean, val type: Type, val frameOffset: Int) :
    ScriptExport(name) {
    override fun toString(): String {
        return "let $name${if (mutable) "!" else ""}: $type = $frameOffset"
    }
}

data class FunctionExport(override val name: String, val type: Type, val codeOffset: Int, val frameOffset: Int?) :
    ScriptExport(name) {
    override fun toString(): String {
        return "fun $name: $type = $codeOffset${if (frameOffset != null) " $frameOffset" else ""}"
    }
}
