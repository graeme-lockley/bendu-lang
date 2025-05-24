package io.littlelanguages.minibendu.typesystem

import io.littlelanguages.minibendu.*

/**
 * Error recovery system for the type checker.
 * 
 * Provides mechanisms to continue type checking after encountering errors,
 * prevent error cascading, collect multiple errors, and prioritize error reporting.
 */
class ErrorRecovery {

    /**
     * Collection of errors encountered during type checking.
     */
    private val errors = mutableListOf<TypeCheckError>()
    
    /**
     * Collection of warnings encountered during type checking.
     */
    private val warnings = mutableListOf<TypeCheckWarning>()
    
    /**
     * Set of type variables that have been marked as error types.
     * Used to prevent cascading errors from the same source.
     */
    private val errorTypeVariables = mutableSetOf<TypeVariable>()

    /**
     * Records a type checking error and returns an error type to continue processing.
     */
    fun recordError(
        message: String,
        location: SourceLocation? = null,
        context: String = "",
        severity: ErrorSeverity = ErrorSeverity.ERROR
    ): Type {
        val error = TypeCheckError(
            message = message,
            location = location,
            context = context,
            severity = severity,
            timestamp = System.currentTimeMillis()
        )
        
        errors.add(error)
        
        // Return a fresh error type variable to continue type checking
        val errorVar = TypeVariable.fresh()
        errorTypeVariables.add(errorVar)
        return errorVar
    }

    /**
     * Records a type checking warning.
     */
    fun recordWarning(
        message: String,
        location: SourceLocation? = null,
        context: String = ""
    ) {
        val warning = TypeCheckWarning(
            message = message,
            location = location,
            context = context,
            timestamp = System.currentTimeMillis()
        )
        
        warnings.add(warning)
    }

    /**
     * Checks if a type variable represents an error type.
     */
    fun isErrorType(type: Type): Boolean {
        return when (type) {
            is TypeVariable -> errorTypeVariables.contains(type)
            is FunctionType -> isErrorType(type.domain) || isErrorType(type.codomain)
            is RecordType -> type.fields.values.any { isErrorType(it) } || 
                           (type.rowVar != null && errorTypeVariables.contains(type.rowVar))
            is TupleType -> type.elements.any { isErrorType(it) }
            is UnionType -> type.alternatives.any { isErrorType(it) }
            is IntersectionType -> type.members.any { isErrorType(it) }
            else -> false
        }
    }

    /**
     * Attempts to recover from a unification failure by creating an error type.
     */
    fun recoverFromUnificationFailure(
        type1: Type,
        type2: Type,
        location: SourceLocation? = null
    ): Type {
        // Don't report cascading errors if one of the types is already an error type
        if (isErrorType(type1) || isErrorType(type2)) {
            val errorVar = TypeVariable.fresh()
            errorTypeVariables.add(errorVar)
            return errorVar
        }
        
        val message = "Cannot unify ${formatType(type1)} with ${formatType(type2)}"
        return recordError(message, location, "type unification")
    }

    /**
     * Attempts to recover from a constraint solving failure.
     */
    fun recoverFromConstraintFailure(
        constraint: TypeConstraint,
        reason: String = ""
    ): Type {
        val location = constraint.sourceLocation
        val message = if (reason.isNotEmpty()) {
            "Constraint solving failed: $reason"
        } else {
            "Cannot solve constraint: $constraint"
        }
        
        return recordError(message, location, "constraint solving")
    }

    /**
     * Attempts to recover from an undefined variable error.
     */
    fun recoverFromUndefinedVariable(
        variableName: String,
        availableVariables: Set<String>,
        location: SourceLocation? = null
    ): Type {
        val message = "Undefined variable: '$variableName'"
        
        // Add suggestions for similar variable names
        val suggestions = findSimilarNames(variableName, availableVariables)
        val fullMessage = if (suggestions.isNotEmpty()) {
            "$message. Did you mean: ${suggestions.take(3).joinToString(", ")}?"
        } else {
            message
        }
        
        return recordError(fullMessage, location, "variable resolution")
    }

    /**
     * Attempts to recover from a pattern matching error.
     */
    fun recoverFromPatternMatchError(
        patternType: Type,
        expressionType: Type,
        location: SourceLocation? = null
    ): Type {
        // Don't report cascading errors
        if (isErrorType(patternType) || isErrorType(expressionType)) {
            val errorVar = TypeVariable.fresh()
            errorTypeVariables.add(errorVar)
            return errorVar
        }
        
        val message = "Pattern of type ${formatType(patternType)} cannot match expression of type ${formatType(expressionType)}"
        return recordError(message, location, "pattern matching")
    }

    /**
     * Attempts to recover from a function application error.
     */
    fun recoverFromApplicationError(
        functionType: Type,
        argumentTypes: List<Type>,
        location: SourceLocation? = null
    ): Type {
        // Don't report cascading errors
        if (isErrorType(functionType) || argumentTypes.any { isErrorType(it) }) {
            val errorVar = TypeVariable.fresh()
            errorTypeVariables.add(errorVar)
            return errorVar
        }
        
        val message = when {
            functionType !is FunctionType -> {
                "Cannot apply arguments to non-function type ${formatType(functionType)}"
            }
            else -> {
                "Function application error: ${formatType(functionType)} applied to ${argumentTypes.map { formatType(it) }}"
            }
        }
        
        return recordError(message, location, "function application")
    }

    /**
     * Gets all recorded errors, optionally filtered by severity.
     */
    fun getErrors(minSeverity: ErrorSeverity = ErrorSeverity.ERROR): List<TypeCheckError> {
        return errors.filter { it.severity.ordinal >= minSeverity.ordinal }
    }

    /**
     * Gets all recorded warnings.
     */
    fun getWarnings(): List<TypeCheckWarning> {
        return warnings.toList()
    }

    /**
     * Checks if any errors have been recorded.
     */
    fun hasErrors(): Boolean = errors.isNotEmpty()

    /**
     * Checks if any warnings have been recorded.
     */
    fun hasWarnings(): Boolean = warnings.isNotEmpty()

    /**
     * Gets the total number of errors.
     */
    fun errorCount(): Int = errors.size

    /**
     * Gets the total number of warnings.
     */
    fun warningCount(): Int = warnings.size

    /**
     * Clears all recorded errors and warnings.
     */
    fun clear() {
        errors.clear()
        warnings.clear()
        errorTypeVariables.clear()
    }

    /**
     * Gets a prioritized list of errors for reporting.
     * Errors are sorted by severity, then by timestamp.
     */
    fun getPrioritizedErrors(): List<TypeCheckError> {
        return errors.sortedWith(compareBy<TypeCheckError> { -it.severity.ordinal }.thenBy { it.timestamp })
    }

    /**
     * Filters out redundant errors that are likely caused by the same underlying issue.
     */
    fun getFilteredErrors(): List<TypeCheckError> {
        val filtered = mutableListOf<TypeCheckError>()
        val seenMessages = mutableSetOf<String>()
        
        for (error in getPrioritizedErrors()) {
            // Simple deduplication based on message similarity
            val normalizedMessage = normalizeErrorMessage(error.message)
            if (normalizedMessage !in seenMessages) {
                filtered.add(error)
                seenMessages.add(normalizedMessage)
            }
        }
        
        return filtered
    }

    /**
     * Creates a summary report of all errors and warnings.
     */
    fun createSummaryReport(): String {
        val builder = StringBuilder()
        
        val filteredErrors = getFilteredErrors()
        val warnings = getWarnings()
        
        if (filteredErrors.isNotEmpty()) {
            builder.append("Errors (${filteredErrors.size}):\n")
            filteredErrors.forEachIndexed { index, error ->
                builder.append("${index + 1}. ${error.message}")
                error.location?.let { loc ->
                    builder.append(" at ${loc.line}:${loc.column}")
                }
                if (error.context.isNotEmpty()) {
                    builder.append(" (${error.context})")
                }
                builder.append("\n")
            }
        }
        
        if (warnings.isNotEmpty()) {
            if (filteredErrors.isNotEmpty()) builder.append("\n")
            builder.append("Warnings (${warnings.size}):\n")
            warnings.forEachIndexed { index, warning ->
                builder.append("${index + 1}. ${warning.message}")
                warning.location?.let { loc ->
                    builder.append(" at ${loc.line}:${loc.column}")
                }
                if (warning.context.isNotEmpty()) {
                    builder.append(" (${warning.context})")
                }
                builder.append("\n")
            }
        }
        
        if (filteredErrors.isEmpty() && warnings.isEmpty()) {
            builder.append("No errors or warnings.")
        }
        
        return builder.toString()
    }

    /**
     * Formats a type for error messages.
     */
    private fun formatType(type: Type): String {
        return when (type) {
            is PrimitiveType -> type.name
            is TypeVariable -> if (errorTypeVariables.contains(type)) "<error>" else "α${type.id}"
            is FunctionType -> "(${formatType(type.domain)} -> ${formatType(type.codomain)})"
            is RecordType -> {
                val fields = type.fields.entries.joinToString(", ") { "${it.key}: ${formatType(it.value)}" }
                val rowStr = if (type.rowVar != null) " | ${type.rowVar}" else ""
                "{$fields$rowStr}"
            }
            is TupleType -> "(${type.elements.joinToString(", ") { formatType(it) }})"
            is UnionType -> "(${type.alternatives.joinToString(" | ") { formatType(it) }})"
            is IntersectionType -> "(${type.members.joinToString(" & ") { formatType(it) }})"
            is LiteralStringType -> "\"${type.value}\""
            else -> type.toString()
        }
    }

    /**
     * Finds variable names similar to the target name.
     */
    private fun findSimilarNames(target: String, available: Set<String>): List<String> {
        return available
            .map { it to levenshteinDistance(target, it) }
            .filter { (_, distance) -> distance <= 2 && distance < target.length }
            .sortedBy { (_, distance) -> distance }
            .map { (name, _) -> name }
    }

    /**
     * Calculates the Levenshtein distance between two strings.
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val m = s1.length
        val n = s2.length
        val dp = Array(m + 1) { IntArray(n + 1) }
        
        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j
        
        for (i in 1..m) {
            for (j in 1..n) {
                if (s1[i - 1] == s2[j - 1]) {
                    dp[i][j] = dp[i - 1][j - 1]
                } else {
                    dp[i][j] = 1 + minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1])
                }
            }
        }
        
        return dp[m][n]
    }

    /**
     * Normalizes an error message for deduplication.
     */
    private fun normalizeErrorMessage(message: String): String {
        // Remove specific type variable names and locations for comparison
        return message
            .replace(Regex("α\\d+"), "α")
            .replace(Regex("at \\d+:\\d+"), "")
            .trim()
    }
}

/**
 * Represents a type checking error with context information.
 */
data class TypeCheckError(
    val message: String,
    val location: SourceLocation? = null,
    val context: String = "",
    val severity: ErrorSeverity = ErrorSeverity.ERROR,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Represents a type checking warning.
 */
data class TypeCheckWarning(
    val message: String,
    val location: SourceLocation? = null,
    val context: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Error severity levels.
 */
enum class ErrorSeverity {
    INFO,
    WARNING,
    ERROR,
    FATAL
} 