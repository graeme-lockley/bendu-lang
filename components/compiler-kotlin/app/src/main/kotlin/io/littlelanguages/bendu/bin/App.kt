package io.littlelanguages.bendu.bin

import io.littlelanguages.bendu.compiler.Args
import io.littlelanguages.bendu.compiler.Instructions
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.isNotEmpty() && args[0] == "test") {
        processTests(args.drop(1).toTypedArray())
    } else if (args.isNotEmpty() && args[0] == "dis") {
        processDis(args.drop(1).toTypedArray())
    } else {
        val parser = ArgParser("bendu-compiler")
        val script by parser.option(ArgType.String, description = "Script file to compiler")
        val expression by parser.option(ArgType.String, description = "Expression to be compiled")
            .default("println(\"Hello, World!\")")
        val outputName by parser.option(ArgType.String, description = "Script file to compiler")

        parser.parse(args)

        if (script == null) {
            compileExpression(expression, if (outputName == null) "out.bc" else outputName!!)
        } else if (!script!!.endsWith(".bendu")) {
            println("Unknown file type")
        } else {
            compileScript(script!!, if (outputName == null) script!!.replace(".bendu", ".bc") else outputName!!)
        }
    }
}

private fun processTests(args: Array<String>) {
    val parser = ArgParser("bendu-compiler test")
    val expression by parser.option(ArgType.String, description = "Test expression to be compiled and executed")
        .default("> 1 + 1\\n2: Int")
    val line by parser.option(ArgType.Int, description = "Line number into file where the test is located")
        .default(1)
    val bc by parser.option(ArgType.String, description = "Bytecode interpreter").default("bci-zig")
    val verbose by parser.option(ArgType.Boolean, description = "Show the generated script").default(false)

    parser.parse(args)

    val script = assembleScript(expression, line).joinToString("\n")

    if (verbose) {
        println(script)
    }

    compileExpression(script, "test.bc", false)

    executeTest(bc)
}

private fun assembleScript(expression: String, startLineNumber: Int): List<String> {
    val script = mutableListOf<String>()

    var counter = 0
    var state = 0 // 0: start of expression, 1: inside expression
    var variableName = "vvvv"

    expression.split("\\n").forEachIndexed { index, it ->
        if (it.startsWith(">")) {
            if (it.substring(1).trim().startsWith("let")) {
                variableName = it.substring(1).trim().split(" ")[1]
                script.add(it.substring(1).trim())
                state = 1
            } else {
                variableName = "vvvv${counter++}"
                script.add("let $variableName = ${it.substring(1).trim()}")
                state = 1
            }
        } else if (it.startsWith(".")) {
            if (state == 0) {
                println("Error: Unexpected line $it")
                exitProcess(1)
            }
            script.add(it.substring(1))
        } else if (it.isNotEmpty()) {
            if (state == 1) {
                val line = it.trim()
                if (line.contains(":")) {
                    val indexOfColon = line.indexOf(':')
                    val valuePart = line.substring(0, indexOfColon).trim()
                    val typePart = line.substring(indexOfColon + 1).trim()
                    if (valuePart == "fn") {
                        script.add("if @$variableName != \"$typePart\" -> abort(\"Error: Line ${index + startLineNumber}: Expected ${markupLiteral(line)}, got fn: \", @$variableName)")
                    } else {
                        script.add(
                            "if $variableName != $valuePart || @$variableName != \"$typePart\" -> abort(\"Error: Line ${index + startLineNumber}: Expected ${
                                markupLiteral(
                                    line
                                )
                            }, got \", $variableName, \": \", @$variableName)"
                        )
                    }
                } else {
                    script.add(
                        "if $variableName != $line -> abort(\"Error: Line ${index + startLineNumber}: Expected ${
                            markupLiteral(
                                line
                            )
                        }, got \", $variableName, \": \", @$variableName)"
                    )
                }
            } else {
                println("Error: Unexpected line $it")
                exitProcess(1)
            }
        }
    }

    return script
}

private fun markupLiteral(value: String): String {
    val result = StringBuilder()

    for (c in value) {
        if (c == '"') {
            result.append('\\')
        } else if (c == '\\') {
            result.append('\\')
        }
        result.append(c)
    }

    return result.toString()
}

private fun executeTest(bc: String) {
    val process = ProcessBuilder(bc, "test.bc")
        .redirectOutput(ProcessBuilder.Redirect.INHERIT)
        .redirectError(ProcessBuilder.Redirect.INHERIT)
        .start()

    val exitCode = process.waitFor()

    if (exitCode != 0) {
        exitProcess(exitCode)
    }
}

private fun compileScript(scriptName: String, outputName: String) =
    compileExpression(File(scriptName).readText(), outputName)

private fun compileExpression(expression: String, outputName: String, showExpression: Boolean = false) =
    File(outputName).writeBytes(compileExpression(expression, showExpression))

private fun processDis(args: Array<String>) {
    val parser = ArgParser("bendu-compiler dis")
    val expression by parser.option(ArgType.String, description = "Test expression to be compiled and disassembled")
        .default("1 + 1")

    parser.parse(args)

    val script = assembleDisScript(expression).joinToString("\n")

    disassembleExpression(script)
}

private fun assembleDisScript(expression: String): List<String> {
    val script = mutableListOf<String>()

    expression.split("\\n").forEachIndexed { index, it ->
        script.add(it.trim())
    }

    return script
}

private fun disassembleExpression(expression: String) {
    val bc = compileExpression(expression)

    var offset = 0

    while (offset < bc.size) {
        print(offset.toString().padStart(4, ' '))
        print(": ")

        val op = getInstructionByOp(bc[offset])

        if (op == null) {
            println("Unknown opcode: ${bc[offset]}")
            exitProcess(1)
        } else {
            print(op.name)
        }
        offset += 1

        for (arg in op.args) {
            print(" ")
            when (arg) {
                Args.U32 -> {
                    val value = readU32(bc, offset)
                    print(value)
                    offset += 4
                }

                Args.U8 -> {
                    val value = readU8(bc, offset)
                    print(value)
                    offset += 1
                }

                Args.F32 -> {
                    val value = readF32(bc, offset)
                    print(value)
                    offset += 4
                }

                Args.I32 -> {
                    val value = readI32(bc, offset)
                    print(value)
                    offset += 4
                }

                Args.STRING -> {
                    val (value, length) = readString(bc, offset)
                    print(value)
                    offset += length
                }
            }
        }

        println()

    }
}

private fun readU8(bc: ByteArray, offset: Int): Int = bc[offset].toInt()

private fun readI32(bc: ByteArray, offset: Int): Int =
    (bc[offset].toInt() shl 24) or (bc[offset + 1].toInt() shl 16) or (bc[offset + 2].toInt() shl 8) or bc[offset + 3].toInt()

private fun readU32(bc: ByteArray, offset: Int): Int =
    (bc[offset].toInt() shl 24) or (bc[offset + 1].toInt() shl 16) or (bc[offset + 2].toInt() shl 8) or bc[offset + 3].toInt()

private fun readF32(bc: ByteArray, offset: Int): Float =
    java.lang.Float.intBitsToFloat(readI32(bc, offset))

private fun readString(bc: ByteArray, offset: Int): Pair<String, Int> {
    val length = readU32(bc, offset)

    val result = StringBuilder()
    for (i in 0 until length) {
        result.append(bc[offset + 4 + i].toInt().toChar())
    }

    return Pair(result.toString(), length + 4)
}

private fun getInstructionByOp(op: Byte): Instructions? {
    return Instructions.entries.find { it.op == op }
}

private fun compileExpression(expression: String, showExpression: Boolean = false): ByteArray {
    val errors = io.littlelanguages.bendu.Errors()
    val script = io.littlelanguages.bendu.infer(expression, errors = errors)
    val bc = io.littlelanguages.bendu.compile(script, errors)

    if (errors.hasErrors()) {
        if (showExpression) {
            println(expression)
        }

        for (e in errors) {
            e.printError(false)
        }
        exitProcess(1)
    }

    return bc
}

