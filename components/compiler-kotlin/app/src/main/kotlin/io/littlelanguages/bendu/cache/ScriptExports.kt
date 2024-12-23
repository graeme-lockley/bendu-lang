package io.littlelanguages.bendu.cache

import io.littlelanguages.bendu.typeinference.Scheme

class ScriptExports(val exports: List<ScriptExport>)

sealed class ScriptExport(open val name: String)

data class ValueExport(override val name: String, val mutable: Boolean, val scheme: Scheme, val frameOffset: Int) :
    ScriptExport(name) {
    override fun toString(): String {
        return "let $name${if (mutable) "!" else ""}: $scheme = $frameOffset"
    }
}

data class FunctionExport(override val name: String, val mutable: Boolean, val scheme: Scheme, val codeOffset: Int, val frameOffset: Int?) :
    ScriptExport(name) {
    override fun toString(): String {
        return "fn $name: $scheme = $codeOffset${if (frameOffset != null) " $frameOffset" else ""}"
    }
}
