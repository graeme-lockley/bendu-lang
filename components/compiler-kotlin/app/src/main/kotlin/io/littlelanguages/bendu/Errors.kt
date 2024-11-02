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
}

sealed class BenduError

data class OperatorOperandTypeError(
    val operator: Op,
    val found: Type,
    val expected: Set<Type>,
    val location: Location
) : BenduError()

data class ParsingError(val found: Token, val expected: Set<TToken>) : BenduError()
data class UnificationError(val found: Type, val expected: Set<Type>) : BenduError()
data class SingleUnificationError(val e1: Type, val e2: Type) : BenduError()
data class MultipleUnificationError(val e1: List<Type>, val e2: List<Type>) : BenduError()
data class UnknownIdentifierError(val id: StringLocation) : BenduError()