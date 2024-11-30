package io.littlelanguages.bendu.bin

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    if (args.isNotEmpty() && args[0] == "test") {
        processTests(args.drop(1).toTypedArray())
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
    val verbose by parser.option(ArgType.Boolean, description = "Show the generate scripts").default(false)

    parser.parse(args)

    val script = assembleScript(expression, line).joinToString("\n")

    if (verbose) {
        println(script)
    }

    compileExpression(script, "test.bc", true)

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
        } else if (!it.isEmpty()) {
            if (state == 1) {
                val line = it.trim()
                if (line.contains(":")) {
                    val indexOfColon = line.indexOf(':')
                    val valuePart = line.substring(0, indexOfColon).trim()
                    val typePart = line.substring(indexOfColon + 1).trim()
                    script.add(
                        "if $variableName != $valuePart || @$variableName != \"$typePart\" -> abort(\"Error: Line ${index + startLineNumber}: Expected ${
                            markupLiteral(
                                line
                            )
                        }, got \", $variableName, \": \", @$variableName)"
                    )
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

private fun compileExpression(expression: String, outputName: String, showExpression: Boolean = false) {
    val errors = io.littlelanguages.bendu.Errors()
    val script = io.littlelanguages.bendu.infer(expression, errors = errors)
    val bc = io.littlelanguages.bendu.compile(script, errors)

    if (errors.hasErrors()) {
        if (showExpression) {
            println(expression)
        }

        for (e in errors) {
            e.printError()
        }
        exitProcess(1)
    }

    File(outputName).writeBytes(bc)
}
