package io.littlelanguages.bendu.cache

import io.littlelanguages.bendu.CacheEntry

data class ScriptDependency(val name: String, val timestamp: Long) {
    companion object {
        fun from(e: CacheEntry): ScriptDependency {
            val file = e.sourceFile()
            return ScriptDependency(file.absolutePath, file.lastModified())
        }
    }
}
