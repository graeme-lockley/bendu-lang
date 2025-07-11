package io.littlelanguages.minibendu

import io.littlelanguages.minibendu.parser.TToken
import io.littlelanguages.minibendu.parser.Token
import io.littlelanguages.minibendu.typesystem.*
import io.littlelanguages.scanpiler.Location
import io.littlelanguages.scanpiler.LocationCoordinate
import io.littlelanguages.scanpiler.LocationRange
import kotlin.system.exitProcess

const val COLOURS = true

class Errors {
    private val errors = mutableListOf<BenduError>()

    fun addError(error: BenduError) {
        errors.add(error)
    }
    
    fun addCompilerError(error: CompilerError, location: Location? = null) {
        errors.add(CompilerErrorWrapper(error, location))
    }

    fun hasErrors(): Boolean {
        return errors.isNotEmpty()
    }

    fun hasNoErrors(): Boolean {
        return errors.isEmpty()
    }

    fun size(): Int {
        return errors.size
    }

    operator fun get(index: Int): BenduError {
        return errors[index]
    }

    operator fun iterator(): Iterator<BenduError> {
        return errors.iterator()
    }

    fun printErrors(exit: Boolean = false) {
        for (e in errors) {
            e.printError()
        }

        if (exit) {
            exitProcess(1)
        }
    }
}

sealed class BenduError {
    abstract fun printError()
}

data class InvalidLiteralError(val value: String, val location: Location) : BenduError() {
    override fun printError() {
        printMessage("Invalid literal", "$value at ${locationToString(location)}")
    }
}

data class ParsingError(val found: Token, val expected: Set<TToken>) : BenduError() {
    override fun printError() {
        printMessage(
            "Parsing Error", "found ${found.lexeme} at ${locationToString(found.location)}, expected $expected"
        )
    }
}

data class CompilerErrorWrapper(val error: CompilerError, val location: Location? = null) : BenduError() {
    override fun printError() {
        val categoryName = when (error.getCategory()) {
            ErrorCategory.SYNTAX -> "Syntax Error"
            ErrorCategory.TYPE -> "Type Error"
            ErrorCategory.SEMANTIC -> "Semantic Error"
            ErrorCategory.INTERNAL -> "Internal Error"
        }
        
        val locationStr = location?.let { " at ${locationToString(it)}" } ?: ""
        printMessage(categoryName, "${error.getMessage()}$locationStr")
    }
}

private fun printMessage(kind: String, message: String) = if (COLOURS) {
    println("\u001B[31m$kind:\u001B[0m $message")
} else {
    println("$kind: $message")
}

private fun locationToString(location: Location): String = when (location) {
    is LocationCoordinate -> "${location.line}:${location.column}"
    is LocationRange -> if (location.start.line == location.end.line) "${location.start.line}:${location.start.column}-${location.end.column}"
    else "${locationToString(location.start)}-${locationToString(location.end)}"
}
