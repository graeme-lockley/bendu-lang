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

object CacheManager {
    private val cache = openCache()

    fun getEntry(sourceFile: File): CacheEntry =
        cache.getEntry(sourceFile)

    fun useExpression(sourceFile: File, expression: String): CacheEntry =
        cache.useExpression(sourceFile, expression)
}

private fun openCache(): Cache {
    val home = System.getProperty("user.home")
    val cacheHome = File(home).resolve(".bendu")

    if (!cacheHome.exists()) {
        cacheHome.mkdirs()
    }

    return Cache(cacheHome)
}

private class Cache(private val home: File) {
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
            errors.printErrors(true)
        }

        return declarations
    }

    fun compile(errors: Errors = Errors()): CompiledScript {
        val startTime = System.currentTimeMillis()

        if (errors.hasNoErrors()) {
            val script = infer(this, errors = errors)
            val compiled = compile(this, script, errors)

            if (errors.hasNoErrors()) {
                writeImage(compiled)
                reportCompileStats(this, startTime, System.currentTimeMillis(), true, BenduOptions.showCompile)
                return compiled
            }
        }

        reportCompileStats(this, startTime, System.currentTimeMillis(), success = false, BenduOptions.showCompile)
        errors.printErrors(true)

        TODO("Should never reach here")
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

    fun sourceFile(): File =
        File(srcDir, "$name.bendu")

    fun byteCodeFile(): File =
        File(dir, "$name.bc")

    protected fun depFile(): File =
        File(dir, "$name.dep")

    fun sigFile(): File =
        File(dir, "$name.sig")

    fun byteCodeFileName(): String =
        byteCodeFile().absolutePath

    abstract fun includeDependencies(dependencies: MutableSet<ScriptDependency>)
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

    override fun includeDependencies(dependencies: MutableSet<ScriptDependency>) {}
}

class FileCacheEntry(override val srcDir: File, override val dir: File, override val name: String) :
    CacheEntry(srcDir, dir, name) {

    private var dependencies: Set<ScriptDependency>? = null

    override fun script(): String =
        sourceFile().readText()

    override fun isUptoDate(): Boolean {
        if (depFile().exists()) {
            val deps = loadDependencies()
            deps.forEach {
                val depScript = CacheManager.getEntry(File(it.name))

                if (depScript.sourceFile().lastModified() != it.timestamp || !depScript.byteCodeFile().exists()) {
                    return false
                }
            }

            this.dependencies = deps

            return true
        } else {
            return false
        }
    }

    override fun includeDependencies(dependencies: MutableSet<ScriptDependency>) {
        if (this.isUptoDate()) {
            dependencies.addAll(this.dependencies!!)
        } else {
            dependencies.add(ScriptDependency.from(this))
        }
    }

    private fun loadDependencies(): Set<ScriptDependency> {
        val result = mutableSetOf<ScriptDependency>()
        val depFileContent = depFile().readText().trim()

        if (depFileContent.isNotBlank()) {
            depFileContent.lines().forEach {
                val (name, timestamp) = it.split(" ")
                result.add(ScriptDependency(name, timestamp.toLong()))
            }
        }

        return result
    }
}

fun reportCompileStats(entry: CacheEntry, startTime: Long, endTime: Long, success: Boolean, showCompile: Boolean) {
    if (showCompile) {
        val delta = endTime - startTime

        if (BenduOptions.colours) {
            println("\u001B[32mCompiling\u001B[0m \u001B[37m${entry.sourceFile().canonicalFile} (${delta}ms)\u001B[0m")
        } else {
            println("Compiling ${entry.sourceFile().canonicalFile} (${delta}ms)")
        }

        if (success && BenduOptions.showExportedSignature) {
            entry.sigFile().readText().trim().lines().filter { it.isNotBlank() }.forEach {
                val indexOfEqual = it.indexOf('=')
                if (indexOfEqual == -1) {
                    println(it)
                } else {
                    println(it.substring(0, indexOfEqual).trim())
                }
            }
        }
    }
}