package io.littlelanguages.bendu

class Errors {
    private val errors = mutableListOf<BenduError>()

    fun addError(error: BenduError) {
        errors.add(error)
    }

    fun hasErrors(): Boolean {
        return errors.isNotEmpty()
    }

    fun size(): Int {
        return errors.size
    }

    operator fun get(index: Int): BenduError {
        return errors[index]
    }
}

sealed class BenduError

data class UnknownIdentifierError(val id: StringLocation) : BenduError()