package io.littlelanguages.bendu

import io.littlelanguages.bendu.cache.ScriptDependency
import io.littlelanguages.bendu.cache.ScriptExport
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
        val srcDir = sourceFile.canonicalFile.parentFile
        val entryDir = File("$home/${srcDir}")
        val name = sourceFile.nameWithoutExtension

        if (!entryDir.exists()) {
            entryDir.mkdirs()
        }

        return FileCacheEntry(srcDir, entryDir, name)
    }

    fun useExpression(sourceFile: File, expression: String): CacheEntry {
        val entryDir = File("$home/${sourceFile.canonicalFile.parent}")
        val name = sourceFile.nameWithoutExtension

        if (!entryDir.exists()) {
            entryDir.mkdirs()
        }

        return ExpressionCacheEntry(sourceFile.canonicalFile.parentFile, entryDir, name, expression)
    }
}

sealed class CacheEntry(open val srcDir: File, open val dir: File, open val name: String) {
    val declarations: List<ScriptExport> by lazy { exportDeclarations() }

    abstract fun script(): String
    abstract fun isUptoDate(): Boolean

    operator fun get(name: String): ScriptExport? =
        declarations.find { it.name == name }

    fun relativeEntry(name: String): CacheEntry {
        val nameFile = File(name)
        val srcDir = File("$srcDir/$name").canonicalFile.parentFile
        val entryDir = File("$dir/$name").canonicalFile.parentFile

        if (!entryDir.exists()) {
            entryDir.mkdirs()
        }

        return FileCacheEntry(srcDir, entryDir, nameFile.nameWithoutExtension)
    }

    private fun exportDeclarations(): List<ScriptExport> {
        val script = sigFile().readText()
        val errors = Errors()
        val declarations = io.littlelanguages.bendu.cache.parse(script, errors)

        if (errors.hasErrors()) {
            errors.printErrors(true, true)
        }

        return declarations
    }

    fun compile(errors: Errors) {
        if (errors.hasNoErrors()) {
            val script = infer(this, errors = errors)
            val compiled = compile(script, errors)

            if (errors.hasNoErrors()) {
                writeImage(compiled)
            }
        }
    }

    fun writeImage(image: CompiledScript) {
        writeSignatures(image.exports)
        writeByteCode(image)
        writeDependencies(image.dependencies)
    }

    private fun writeByteCode(image: CompiledScript) {
        val importsBB = ByteBuilder()

        importsBB.appendInt(image.imports.size)
        image.imports.forEach { importsBB.appendString(it) }
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

    private fun writeSignatures(signatures: ScriptExports) =
        sigFile().writeText(signatures.exports.joinToString(";\n") { it.toString() })

    private fun writeDependencies(dependencies: List<ScriptDependency>) =
        PrintWriter(depFile()).use { stream ->
            dependencies.forEach {
                stream.print(it.name)
                stream.print(" ")
                stream.println(it.timestamp)
            }
        }

    protected fun srcFile(): File =
        File(srcDir, "$name.bendu")

    private fun byteCodeFile(): File =
        File(dir, "$name.bc")

    private fun depFile(): File =
        File(dir, "$name.dep")

    private fun sigFile(): File =
        File(dir, "$name.sig")

    fun byteCodeFileName(): String =
        byteCodeFile().absolutePath
}

class ExpressionCacheEntry(
    override val srcDir: File,
    override val dir: File,
    override val name: String,
    val expression: String
) :
    CacheEntry(srcDir, dir, name) {
    override fun script(): String =
        expression

    override fun isUptoDate(): Boolean =
        false
}

class FileCacheEntry(override val srcDir: File, override val dir: File, override val name: String) :
    CacheEntry(srcDir, dir, name) {
    override fun script(): String =
        srcFile().readText()

    override fun isUptoDate(): Boolean {
        val sourceFile = File(srcDir, "$name.bendu")
        val bcFile = File(dir, "$name.bc")

        return bcFile.exists() && bcFile.lastModified() > sourceFile.lastModified()
    }
}