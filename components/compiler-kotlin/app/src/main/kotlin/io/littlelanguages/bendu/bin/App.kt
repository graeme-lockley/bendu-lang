package io.littlelanguages.bendu.bin

import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import java.io.File
import kotlin.system.exitProcess

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

private fun compileScript(scriptName: String, outputName: String) =
    compileExpression(File(scriptName).readText(), outputName)

private fun compileExpression(expression: String, outputName: String) {
    val errors = io.littlelanguages.bendu.Errors()
    val script = io.littlelanguages.bendu.infer(expression, errors = errors)
    val bc = io.littlelanguages.bendu.compile(script, errors)

    if (errors.hasErrors()) {
        for (e in errors) {
            e.printError()
        }
        exitProcess(1)
    }

    File(outputName).writeBytes(bc)
}
