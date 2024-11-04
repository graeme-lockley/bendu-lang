package io.littlelanguages.bendu

import io.littlelanguages.bendu.parser.TToken
import io.littlelanguages.bendu.parser.Token
import io.littlelanguages.bendu.typeinference.Type
import io.littlelanguages.scanpiler.Location

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
    abstract fun printError()
}

data class InvalidLiteralError(val value: String, val location: Location) : BenduError() {
    override fun printError() {
        System.err.println("\u001B[31mInvalid literal:\u001B[0m $value at $location")
    }
}

data class OperatorOperandTypeError(
    val operator: Op,
    val found: Type,
    val expected: Set<Type>,
    val location: Location
) : BenduError() {
    override fun printError() {
        System.err.println("\u001B[31mOperator operand type error:\u001B[0m $operator, found $found, expected $expected at $location")
    }
}

data class ParsingError(val found: Token, val expected: Set<TToken>) : BenduError() {
    override fun printError() {
        System.err.println("\u001B[31mParsing error:\u001B[0m found ${found.lexeme} at ${found.location}, expected $expected")
    }
}

data class SingleUnificationError(val e1: Type, val e2: Type) : BenduError() {
    override fun printError() {
        System.err.println("\u001B[31mUnification error:\u001B[0m $e1 ${e1.location}, $e2 ${e2.location}")
    }
}

data class MultipleUnificationError(val e1: List<Type>, val e2: List<Type>) : BenduError() {
    override fun printError() {
        System.err.println("\u001B[31mUnification error:\u001B[0m $e1, $e2")
    }
}

data class UnificationError(val found: Type, val expected: Set<Type>) : BenduError() {
    override fun printError() {
        System.err.println("\u001B[31mUnification error:\u001B[0m found $found, expected $expected")
    }
}

data class UnknownIdentifierError(val id: StringLocation) : BenduError() {
    override fun printError() {
        System.err.println("\u001B[31mUnknown identifier:\u001B[0m ${id.value} at ${id.location}")
    }
}
