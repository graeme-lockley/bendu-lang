package io.littlelanguages.bendu

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
    fun writeBytecode(bytecode: ByteArray) =
        byteCodeFile().writeBytes(bytecode)

    private fun byteCodeFile(): File =
        File(dir, "$name.bc")

    fun byteCodeFileName(): String =
        byteCodeFile().absolutePath
}