package io.littlelanguages.bendu.cache

import io.littlelanguages.bendu.typeinference.Type

class ScriptExports(val exports: List<ScriptExport>)

sealed class ScriptExport(open val name: String)

data class ValueExport(override val name: String, val mutable: Boolean, val type: Type, val offset: Int) :
    ScriptExport(name)

data class FunctionExport(override val name: String, val type: Type, val offset: Int) : ScriptExport(name)
