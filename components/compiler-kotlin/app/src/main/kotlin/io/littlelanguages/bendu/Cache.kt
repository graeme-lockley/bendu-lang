package io.littlelanguages.bendu

import io.littlelanguages.bendu.cache.ScriptExports
import java.io.File

fun openCache(): Cache {
    val home = System.getProperty("user.home")
    val cacheHome = File(home).resolve(".bendu")

    if (!cacheHome.exists()) {
        cacheHome.mkdirs()
    }

    return Cache(cacheHome)
}

class Cache(private val home: File) {
    fun getEntry(sourceFile: File): CacheEntry {
        val entryDir = File("$home/${sourceFile.canonicalFile.parent}")
        val name = sourceFile.nameWithoutExtension

        if (!entryDir.exists()) {
            entryDir.mkdirs()
        }

        return CacheEntry(entryDir, name)
    }
}

class CacheEntry(private val dir: File, private val name: String) {
    fun writeImage(image: CompiledScript) {
        byteCodeFile().writeBytes(image.bytecode)
        writeSignatures(image.exports)
    }

    private fun writeSignatures(signatures: ScriptExports) {
        val file = File(dir, "$name.sig")

        file.writeText(signatures.exports.joinToString(";\n") { it.toString() })
    }

    private fun byteCodeFile(): File =
        File(dir, "$name.bc")

    fun byteCodeFileName(): String =
        byteCodeFile().absolutePath
}