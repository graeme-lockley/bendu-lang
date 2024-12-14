package io.littlelanguages.bendu

import io.littlelanguages.bendu.parser.TToken
import io.littlelanguages.bendu.parser.Token
import io.littlelanguages.bendu.typeinference.Type
import io.littlelanguages.scanpiler.Location
import io.littlelanguages.scanpiler.LocationCoordinate
import io.littlelanguages.scanpiler.LocationRange

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
}

sealed class BenduError {
    abstract fun printError(colours: Boolean = true)
}

data class IdentifierRedefinitionError(val id: StringLocation, val otherLocation: Location) : BenduError() {
    override fun printError(colours: Boolean) {
        printMessage("Identifier Redefinition", "${id.value} declared at ${locationToString(otherLocation)} redeclared at ${locationToString(id.location)}", colours)
    }
}

data class InvalidLiteralError(val value: String, val location: Location) : BenduError() {
    override fun printError(colours: Boolean) {
        printMessage("Invalid literal", "$value at ${locationToString(location)}", colours)
    }
}

data class OperatorOperandTypeError(
    val operator: Op,
    val found: Type,
    val expected: Set<Type>,
    val location: Location
) : BenduError() {
    override fun printError(colours: Boolean) {
        printMessage(
            "Operator Operand Type Error",
            "$operator, found $found, expected $expected at ${locationToString(location)}",
            colours
        )
    }
}

data class ParsingError(val found: Token, val expected: Set<TToken>) : BenduError() {
    override fun printError(colours: Boolean) {
        printMessage(
            "Parsing Error",
            "found ${found.lexeme} at ${locationToString(found.location)}, expected $expected",
            colours
        )
    }
}

data class SingleUnificationError(val e1: Type, val e2: Type) : BenduError() {
    override fun printError(colours: Boolean) {
        printMessage(
            "Unification Error",
            "$e1${if (e1.location == null) "" else " " + locationToString(e1.location!!)}, $e2 ${
                if (e2.location == null) "" else " " + locationToString(e2.location!!)
            }",
            colours
        )
    }
}

data class MultipleUnificationError(val e1: List<Type>, val e2: List<Type>) : BenduError() {
    override fun printError(colours: Boolean) {
        printMessage("Unification Error", "$e1, $e2", colours)
    }
}

data class UnificationError(val found: Type, val expected: Set<Type>) : BenduError() {
    override fun printError(colours: Boolean) {
        printMessage("Unification Error", "found $found${if (found.location == null) "" else " ${locationToString(found.location!!)}"}, expected $expected", colours)
    }
}

data class UnknownIdentifierError(val id: StringLocation) : BenduError() {
    override fun printError(colours: Boolean) {
        printMessage("Unknown Identifier", "${id.value} at ${locationToString(id.location)}", colours)
    }
}

private fun printMessage(kind: String, message: String, colours: Boolean) =
    if (colours) {
        System.err.println("\u001B[31m$kind:\u001B[0m $message")
    } else {
        System.err.println("$kind: $message")
    }


private fun locationToString(location: Location): String =
    when (location) {
        is LocationCoordinate -> "${location.line}:${location.column}"
        is LocationRange -> "${locationToString(location.start)}-${locationToString(location.end)}"
    }