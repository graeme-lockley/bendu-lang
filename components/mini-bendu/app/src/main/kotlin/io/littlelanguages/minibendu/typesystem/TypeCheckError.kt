package io.littlelanguages.minibendu.typesystem

/**
 * Represents different types of errors that can occur during type checking.
 * This provides a structured way to handle errors instead of using plain strings.
 */
sealed class TypeCheckErrorKind {
    abstract fun getMessage(): String
    
    /**
     * Type mismatch between expected and actual types.
     */
    data class TypeMismatch(
        val expected: Type,
        val actual: Type,
        val context: String? = null
    ) : TypeCheckErrorKind() {
        override fun getMessage(): String {
            val contextMsg = if (context != null) " in $context" else ""
            return "Cannot unify $expected with $actual$contextMsg"
        }
    }
    
    /**
     * Variable is used but not defined in the current scope.
     */
    data class UndefinedVariable(
        val variableName: String
    ) : TypeCheckErrorKind() {
        override fun getMessage(): String = "Undefined variable: $variableName"
    }
    
    /**
     * Pattern match is not exhaustive - some cases are missing.
     */
    data class NonExhaustivePatternMatch(
        val missingPatterns: List<String>
    ) : TypeCheckErrorKind() {
        override fun getMessage(): String {
            val patterns = missingPatterns.joinToString(", ")
            return "non-exhaustive pattern match. Missing patterns: $patterns"
        }
    }
    
    /**
     * Two or more patterns overlap and would match the same values.
     */
    data class OverlappingPatterns(
        val pattern1: String,
        val pattern2: String
    ) : TypeCheckErrorKind() {
        override fun getMessage(): String = "Overlapping patterns: $pattern1 and $pattern2"
    }
    
    /**
     * Constructor used in pattern but not defined.
     */
    data class UndefinedConstructor(
        val constructorName: String
    ) : TypeCheckErrorKind() {
        override fun getMessage(): String = "Undefined constructor: $constructorName"
    }
    
    /**
     * Function called with wrong number or types of arguments.
     */
    data class FunctionApplicationError(
        val functionType: Type,
        val argumentTypes: List<Type>,
        val details: String? = null
    ) : TypeCheckErrorKind() {
        override fun getMessage(): String {
            return details ?: "Function application error: cannot apply $functionType to ${argumentTypes.joinToString(", ")}"
        }
    }
    
    /**
     * Error during parsing phase.
     */
    data class ParseError(
        val errorMessage: String
    ) : TypeCheckErrorKind() {
        override fun getMessage(): String = "Parse error: $errorMessage"
    }
    
    /**
     * Internal error in the type checker implementation.
     */
    data class InternalError(
        val errorMessage: String
    ) : TypeCheckErrorKind() {
        override fun getMessage(): String = "Internal type checker error: $errorMessage"
    }
    
    /**
     * Occurs check failure - infinite type detected.
     */
    data class OccursCheckFailure(
        val typeVariable: TypeVariable,
        val containingType: Type
    ) : TypeCheckErrorKind() {
        override fun getMessage(): String = "Cannot construct infinite type: $typeVariable = $containingType"
    }
    
    /**
     * Generic error for cases not covered by specific error types.
     */
    data class GenericError(
        val errorMessage: String
    ) : TypeCheckErrorKind() {
        override fun getMessage(): String = errorMessage
    }
} 