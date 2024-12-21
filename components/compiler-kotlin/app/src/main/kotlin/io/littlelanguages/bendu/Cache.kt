package io.littlelanguages.bendu

import io.littlelanguages.bendu.cache.ScriptDependency
import io.littlelanguages.bendu.cache.ScriptExports
import io.littlelanguages.bendu.compiler.ByteBuilder
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.PrintWriter

private const val UPPER_VERSION: Byte = 0
private const val LOWER_VERSION: Byte = 1

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
        writeSignatures(image.exports)
        writeByteCode(image)
        writeDependencies(image.dependencies)
    }

    private fun writeByteCode(image: CompiledScript) {
        val importsBB = ByteBuilder()

        importsBB.appendInt(image.imports.size)
        image.imports.forEach { importsBB.appendString(it.name ) ; importsBB.appendLong(it.timestamp) }
        val imports = importsBB.toByteArray()

        BufferedOutputStream(FileOutputStream(byteCodeFile())).use {
            it.write('H'.code)
            it.write('W'.code)
            it.write(UPPER_VERSION.toInt())
            it.write(LOWER_VERSION.toInt())

            it.write(imports)
            it.write(image.bytecode)
        }
    }

    private fun writeSignatures(signatures: ScriptExports) {
        val file = File(dir, "$name.sig")

        file.writeText(signatures.exports.joinToString(";\n") { it.toString() })
    }

    private fun writeDependencies(dependencies: List<ScriptDependency>) {
        val file = File(dir, "$name.dep")

        PrintWriter(file).use { stream ->
            dependencies.forEach {
                stream.print(it.name)
                stream.print(" ")
                stream.println(it.timestamp)
            }
        }
    }

    private fun byteCodeFile(): File =
        File(dir, "$name.bc")

    fun byteCodeFileName(): String =
        byteCodeFile().absolutePath
}