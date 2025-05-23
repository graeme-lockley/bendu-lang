package io.littlelanguages.bendu

import io.littlelanguages.bendu.parser.TToken
import io.littlelanguages.bendu.parser.Token
import io.littlelanguages.bendu.typeinference.Type
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

data class IdentifierRedefinitionError(val id: StringLocation, val otherLocation: Location) : BenduError() {
    override fun printError() {
        printMessage(
            "Identifier Redefinition",
            "${id.value} declared at ${locationToString(otherLocation)} redeclared at ${locationToString(id.location)}"
        )
    }
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

data class SingleUnificationError(val e1: Type, val e2: Type) : BenduError() {
    override fun printError() {
        printMessage(
            "Unification Error", "$e1${if (e1.location == null) "" else " " + locationToString(e1.location!!)}, $e2${
                if (e2.location == null) "" else " ${locationToString(e2.location!!)}"
            }"
        )
    }
}

data class MultipleUnificationError(val e1: List<Type>, val e2: List<Type>) : BenduError() {
    override fun printError() {
        printMessage("Unification Error", "$e1, $e2")
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