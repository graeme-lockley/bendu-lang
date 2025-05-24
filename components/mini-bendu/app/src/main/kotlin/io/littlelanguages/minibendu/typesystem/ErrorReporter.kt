package io.littlelanguages.minibendu.typesystem

import io.littlelanguages.minibendu.*
import io.littlelanguages.scanpiler.Location
import io.littlelanguages.scanpiler.LocationCoordinate

/**
 * Enhanced error reporting system for the type checker.
 * 
 * Provides detailed, helpful error messages with source location tracking,
 * contextual information, and suggestions for common mistakes.
 */
class ErrorReporter {

    /**
     * Formats a detailed error message for type mismatches.
     */
    fun formatTypeMismatchError(
        expected: Type,
        actual: Type,
        context: String = "",
        location: SourceLocation? = null
    ): String {
        val builder = StringBuilder()
        
        // Main error message
        builder.append("Type mismatch")
        
        // Add context if provided
        if (context.isNotEmpty()) {
            builder.append(" in $context")
        }
        
        builder.append(":\n")
        builder.append("  Expected: ${formatTypeForUser(expected)}\n")
        builder.append("  Actual:   ${formatTypeForUser(actual)}")
        
        // Add location if available
        location?.let { loc ->
            builder.append(" at ${loc.line}:${loc.column}")
        }
        
        // Add helpful suggestions
        val suggestion = generateTypeMismatchSuggestion(expected, actual)
        if (suggestion.isNotEmpty()) {
            builder.append("\n\nSuggestion: $suggestion")
        }
        
        return builder.toString()
    }

    /**
     * Formats an error message for undefined variables with suggestions.
     */
    fun formatUndefinedVariableError(
        variableName: String,
        availableVariables: Set<String>,
        location: SourceLocation? = null
    ): String {
        val builder = StringBuilder()
        
        builder.append("Undefined variable: '$variableName'")
        
        location?.let { loc ->
            builder.append(" at ${loc.line}:${loc.column}")
        }
        
        // Find similar variable names
        val suggestions = findSimilarVariableNames(variableName, availableVariables)
        
        if (suggestions.isNotEmpty()) {
            builder.append("\n\nDid you mean:")
            suggestions.take(3).forEach { suggestion ->
                builder.append("\n  - $suggestion")
            }
        } else if (availableVariables.isNotEmpty()) {
            builder.append("\n\nAvailable variables in scope:")
            availableVariables.take(5).forEach { variable ->
                builder.append("\n  - $variable")
            }
        }
        
        return builder.toString()
    }

    /**
     * Formats an error message for function application errors.
     */
    fun formatFunctionApplicationError(
        functionType: Type,
        argumentTypes: List<Type>,
        location: SourceLocation? = null
    ): String {
        val builder = StringBuilder()
        
        when {
            functionType !is FunctionType -> {
                builder.append("Cannot call a value of type ${formatTypeForUser(functionType)} as a function")
                location?.let { loc ->
                    builder.append(" at ${loc.line}:${loc.column}")
                }
                
                // Add suggestion for common mistakes
                if (functionType is PrimitiveType && functionType.name == "String") {
                    builder.append("\n\nSuggestion: Strings are not callable. Did you mean to use string interpolation or concatenation?")
                }
            }
            
            argumentTypes.size != getArityOf(functionType) -> {
                val expectedArity = getArityOf(functionType)
                val actualArity = argumentTypes.size
                
                builder.append("Function expects $expectedArity argument(s) but received $actualArity")
                location?.let { loc ->
                    builder.append(" at ${loc.line}:${loc.column}")
                }
                
                if (actualArity < expectedArity) {
                    builder.append("\n\nNote: In mini-bendu, functions are curried. Partial application returns a function.")
                }
            }
            
            else -> {
                builder.append("Function application error")
                location?.let { loc ->
                    builder.append(" at ${loc.line}:${loc.column}")
                }
            }
        }
        
        return builder.toString()
    }

    /**
     * Formats an error message for pattern matching errors.
     */
    fun formatPatternMatchError(
        patternType: Type,
        expressionType: Type,
        missingPatterns: List<String> = emptyList(),
        location: SourceLocation? = null
    ): String {
        val builder = StringBuilder()
        
        if (missingPatterns.isNotEmpty()) {
            builder.append("Non-exhaustive pattern match")
            location?.let { loc ->
                builder.append(" at ${loc.line}:${loc.column}")
            }
            
            builder.append("\n\nMissing patterns:")
            missingPatterns.forEach { pattern ->
                builder.append("\n  - $pattern")
            }
        } else {
            builder.append("Pattern match error: ")
            builder.append("Cannot match pattern of type ${formatTypeForUser(patternType)} ")
            builder.append("against expression of type ${formatTypeForUser(expressionType)}")
            
            location?.let { loc ->
                builder.append(" at ${loc.line}:${loc.column}")
            }
        }
        
        return builder.toString()
    }

    /**
     * Formats an error message for occurs check failures.
     */
    fun formatOccursCheckError(
        typeVariable: TypeVariable,
        containingType: Type,
        location: SourceLocation? = null
    ): String {
        val builder = StringBuilder()
        
        builder.append("Cannot construct infinite type: ")
        builder.append("${typeVariable} = ${formatTypeForUser(containingType)}")
        
        location?.let { loc ->
            builder.append(" at ${loc.line}:${loc.column}")
        }
        
        builder.append("\n\nThis error occurs when a type tries to contain itself infinitely.")
        builder.append("\nFor example: a list that contains itself as an element.")
        
        return builder.toString()
    }

    /**
     * Formats an error message for constraint solving failures.
     */
    fun formatConstraintSolvingError(
        constraint: EqualityConstraint,
        reason: String,
        location: SourceLocation? = null
    ): String {
        val builder = StringBuilder()
        
        builder.append("Cannot solve type constraint: ")
        builder.append("${formatTypeForUser(constraint.type1)} ~ ${formatTypeForUser(constraint.type2)}")
        
        location?.let { loc ->
            builder.append(" at ${loc.line}:${loc.column}")
        }
        
        if (reason.isNotEmpty()) {
            builder.append("\n\nReason: $reason")
        }
        
        return builder.toString()
    }

    /**
     * Formats a type for user-friendly display.
     */
    private fun formatTypeForUser(type: Type): String {
        return when (type) {
            is PrimitiveType -> type.name
            is TypeVariable -> "α${type.id}"
            is FunctionType -> {
                val param = formatTypeForUser(type.domain)
                val ret = formatTypeForUser(type.codomain)
                "($param -> $ret)"
            }
            is RecordType -> {
                if (type.fields.isEmpty()) {
                    "{}"
                } else {
                    val fields = type.fields.entries.joinToString(", ") { (name, fieldType) ->
                        "$name: ${formatTypeForUser(fieldType)}"
                    }
                    val rowStr = if (type.rowVar != null) " | ${type.rowVar}" else ""
                    "{$fields$rowStr}"
                }
            }
            is TupleType -> {
                val elements = type.elements.joinToString(", ") { formatTypeForUser(it) }
                "($elements)"
            }
            is UnionType -> {
                val variants = type.alternatives.joinToString(" | ") { formatTypeForUser(it) }
                "($variants)"
            }
            is IntersectionType -> {
                val types = type.members.joinToString(" & ") { formatTypeForUser(it) }
                "($types)"
            }
            is RecursiveType -> {
                "μ${type.name}.${formatTypeForUser(type.body)}"
            }
            is LiteralStringType -> "\"${type.value}\""
            else -> type.toString()
        }
    }

    /**
     * Generates helpful suggestions for type mismatches.
     */
    private fun generateTypeMismatchSuggestion(expected: Type, actual: Type): String {
        return when {
            // String vs Int in arithmetic
            expected.toString().contains("Int") && actual.toString().contains("String") -> {
                "Strings cannot be used in arithmetic operations. Did you mean to convert the string to a number?"
            }
            
            // Bool vs other types in conditions
            expected.toString().contains("Bool") && !actual.toString().contains("Bool") -> {
                "Expected a boolean value for condition. Use comparison operators (==, !=, <, >) to create boolean expressions."
            }
            
            // Function type mismatches
            expected is FunctionType && actual !is FunctionType -> {
                "Expected a function but got ${formatTypeForUser(actual)}. Did you forget to call a function?"
            }
            
            actual is FunctionType && expected !is FunctionType -> {
                "Got a function but expected ${formatTypeForUser(expected)}. Did you forget to apply the function to arguments?"
            }
            
            // Record field mismatches
            expected is RecordType && actual is RecordType -> {
                val missingFields = expected.fields.keys - actual.fields.keys
                val extraFields = actual.fields.keys - expected.fields.keys
                
                when {
                    missingFields.isNotEmpty() -> "Missing fields: ${missingFields.joinToString(", ")}"
                    extraFields.isNotEmpty() -> "Extra fields: ${extraFields.joinToString(", ")}"
                    else -> ""
                }
            }
            
            else -> ""
        }
    }

    /**
     * Finds variable names similar to the undefined variable.
     */
    private fun findSimilarVariableNames(target: String, available: Set<String>): List<String> {
        return available
            .map { it to calculateLevenshteinDistance(target, it) }
            .filter { (_, distance) -> distance <= 2 && distance < target.length }
            .sortedBy { (_, distance) -> distance }
            .map { (name, _) -> name }
    }

    /**
     * Calculates the Levenshtein distance between two strings.
     */
    private fun calculateLevenshteinDistance(s1: String, s2: String): Int {
        val m = s1.length
        val n = s2.length
        
        // Create a matrix to store the distances
        val dp = Array(m + 1) { IntArray(n + 1) }
        
        // Initialize base cases
        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j
        
        // Fill the matrix
        for (i in 1..m) {
            for (j in 1..n) {
                if (s1[i - 1] == s2[j - 1]) {
                    dp[i][j] = dp[i - 1][j - 1]
                } else {
                    dp[i][j] = 1 + minOf(
                        dp[i - 1][j],     // deletion
                        dp[i][j - 1],     // insertion
                        dp[i - 1][j - 1]  // substitution
                    )
                }
            }
        }
        
        return dp[m][n]
    }

    /**
     * Gets the arity (number of parameters) of a function type.
     */
    private fun getArityOf(type: Type): Int {
        return when (type) {
            is FunctionType -> 1 + getArityOf(type.codomain)
            else -> 0
        }
    }

    /**
     * Extracts contextual information from an expression for better error messages.
     */
    fun extractContext(expr: Expr): String {
        return when (expr) {
            is BinaryOpExpr -> "binary operation (${expr.op})"
            is ApplicationExpr -> "function application"
            is IfExpr -> "if expression"
            is MatchExpr -> "pattern match"
            is LetExpr -> "let expression"
            is LambdaExpr -> "lambda expression"
            is ProjectionExpr -> "field access"
            is RecordExpr -> "record construction"
            is TupleExpr -> "tuple construction"
            else -> "expression"
        }
    }

    /**
     * Creates a source location from coordinates.
     */
    fun createSourceLocation(line: Int, column: Int): SourceLocation {
        return SourceLocation(line, column)
    }

    /**
     * Extracts source location from AST node location.
     */
    fun extractSourceLocation(location: Location?): SourceLocation? {
        return when (location) {
            is LocationCoordinate -> SourceLocation(location.line, location.column)
            else -> null
        }
    }
} 