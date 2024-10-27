package io.littlelanguages.bendu.bin

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import java.io.File

fun main(args: Array<String>) {
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

private fun compileExpression(expression: String, outputName: String) {
    val script = io.littlelanguages.bendu.parse(expression)
    val bc = io.littlelanguages.bendu.compile(script)

    File(outputName).writeBytes(bc)
}

private fun compileScript(scriptName: String, outputName: String) {
    val script = io.littlelanguages.bendu.parse(File(scriptName).readText())
    val bc = io.littlelanguages.bendu.compile(script)

    File(outputName).writeBytes(bc)
}

