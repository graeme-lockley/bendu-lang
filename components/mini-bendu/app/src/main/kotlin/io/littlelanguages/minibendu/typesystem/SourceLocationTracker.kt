package io.littlelanguages.minibendu.typesystem

import io.littlelanguages.minibendu.*
import io.littlelanguages.scanpiler.Location
import io.littlelanguages.scanpiler.LocationCoordinate
import io.littlelanguages.scanpiler.LocationRange

/**
 * Enhanced source location information for type system integration.
 * 
 * This provides richer location information than the basic SourceLocation,
 * including support for ranges, nested contexts, and error recovery.
 */
data class EnhancedSourceLocation(
    val line: Int,
    val column: Int,
    val endLine: Int? = null,
    val endColumn: Int? = null,
    val filename: String? = null,
    val context: LocationContext? = null
) {
    /**
     * Create a location range if end coordinates are available.
     */
    fun toRange(): Pair<SourceLocation, SourceLocation?> {
        val start = SourceLocation(line, column)
        val end = if (endLine != null && endColumn != null) {
            SourceLocation(endLine, endColumn)
        } else null
        return Pair(start, end)
    }
    
    /**
     * Check if this location contains another location.
     */
    fun contains(other: EnhancedSourceLocation): Boolean {
        return when {
            endLine == null || endColumn == null -> false
            other.endLine == null || other.endColumn == null -> {
                // Point inside range
                other.line >= line && other.line <= endLine &&
                (other.line > line || other.column >= column) &&
                (other.line < endLine || other.column <= endColumn)
            }
            else -> {
                // Range inside range
                (other.line > line || (other.line == line && other.column >= column)) &&
                (other.endLine < endLine || (other.endLine == endLine && other.endColumn <= endColumn))
            }
        }
    }
    
    override fun toString(): String {
        val base = "${filename ?: ""}:$line:$column"
        return if (endLine != null && endColumn != null) {
            "$base-$endLine:$endColumn"
        } else base
    }
}

/**
 * Context information for source locations.
 * Provides additional semantic information about where an error occurred.
 */
sealed class LocationContext {
    data class FunctionApplication(val functionName: String?) : LocationContext()
    data class PatternMatch(val patternType: String) : LocationContext() 
    data class FieldAccess(val fieldName: String, val recordType: String?) : LocationContext()
    data class VariableReference(val variableName: String) : LocationContext()
    data class TypeAnnotation(val annotationType: String) : LocationContext()
    data class LetBinding(val bindingName: String, val isRecursive: Boolean) : LocationContext()
    data class LambdaParameter(val parameterName: String) : LocationContext()
    
    override fun toString(): String = when (this) {
        is FunctionApplication -> "in function application${functionName?.let { " to $it" } ?: ""}"
        is PatternMatch -> "in pattern match ($patternType)"
        is FieldAccess -> "in field access to '$fieldName'${recordType?.let { " on $it" } ?: ""}"
        is VariableReference -> "in reference to variable '$variableName'"
        is TypeAnnotation -> "in type annotation '$annotationType'"
        is LetBinding -> "in ${if (isRecursive) "recursive " else ""}let binding '$bindingName'"
        is LambdaParameter -> "in lambda parameter '$parameterName'"
    }
}

/**
 * Source location tracker that maintains location information throughout
 * the type checking process.
 * 
 * This class is responsible for:
 * - Converting between different location representations
 * - Tracking location context through transformations
 * - Providing location-aware error messages
 * - Supporting error recovery with location information
 */
class SourceLocationTracker {
    
    private val locationStack = mutableListOf<EnhancedSourceLocation>()
    private val errorLocations = mutableMapOf<String, EnhancedSourceLocation>()
    
    /**
     * Convert a scanpiler Location to our enhanced location format.
     */
    fun convertLocation(location: Location, context: LocationContext? = null): EnhancedSourceLocation {
        return when (location) {
            is LocationCoordinate -> EnhancedSourceLocation(
                line = location.line,
                column = location.column,
                context = context
            )
            is LocationRange -> EnhancedSourceLocation(
                line = location.start.line,
                column = location.start.column,
                endLine = location.end.line,
                endColumn = location.end.column,
                context = context
            )
        }
    }
    
    /**
     * Push a location onto the context stack.
     * Used for tracking nested expression contexts.
     */
    fun pushLocation(location: EnhancedSourceLocation) {
        locationStack.add(location)
    }
    
    /**
     * Pop the most recent location from the context stack.
     */
    fun popLocation(): EnhancedSourceLocation? {
        return if (locationStack.isNotEmpty()) {
            locationStack.removeAt(locationStack.size - 1)
        } else null
    }
    
    /**
     * Get the current location context (top of stack).
     */
    fun getCurrentLocation(): EnhancedSourceLocation? {
        return locationStack.lastOrNull()
    }
    
    /**
     * Track a type error at a specific location.
     */
    fun recordError(errorId: String, location: EnhancedSourceLocation) {
        errorLocations[errorId] = location
    }
    
    /**
     * Retrieve the location of a previously recorded error.
     */
    fun getErrorLocation(errorId: String): EnhancedSourceLocation? {
        return errorLocations[errorId]
    }
    
    /**
     * Create an enhanced error message with location information.
     */
    fun enhanceErrorMessage(
        baseMessage: String, 
        location: EnhancedSourceLocation,
        suggestions: List<String> = emptyList()
    ): String {
        val builder = StringBuilder()
        
        // Add location information
        builder.append("$location: $baseMessage")
        
        // Add context if available
        location.context?.let { context ->
            builder.append(" $context")
        }
        
        // Add stack trace if available
        if (locationStack.isNotEmpty()) {
            builder.append("\n  Location stack:")
            locationStack.reversed().take(3).forEach { stackLocation ->
                builder.append("\n    at $stackLocation")
                stackLocation.context?.let { context ->
                    builder.append(" ($context)")
                }
            }
        }
        
        // Add suggestions
        if (suggestions.isNotEmpty()) {
            builder.append("\n  Suggestions:")
            suggestions.forEach { suggestion ->
                builder.append("\n    - $suggestion")
            }
        }
        
        return builder.toString()
    }
    
    /**
     * Extract location information from an AST expression.
     */
    fun extractExpressionLocation(expr: Expr): EnhancedSourceLocation {
        val context = determineExpressionContext(expr)
        return convertLocation(expr.location(), context)
    }
    
    /**
     * Determine the appropriate context for an expression.
     */
    private fun determineExpressionContext(expr: Expr): LocationContext? {
        return when (expr) {
            is VarExpr -> LocationContext.VariableReference(expr.id.value)
            is ApplicationExpr -> {
                val functionName = when (expr.function) {
                    is VarExpr -> expr.function.id.value
                    else -> null
                }
                LocationContext.FunctionApplication(functionName)
            }
            is ProjectionExpr -> {
                val recordType = null // Could be enhanced to include record type
                LocationContext.FieldAccess(expr.field.value, recordType)
            }
            is LetExpr -> LocationContext.LetBinding(expr.id.value, expr.recursive)
            is LambdaExpr -> LocationContext.LambdaParameter(expr.param.value)
            else -> null
        }
    }
    
    /**
     * Create a location-aware constraint with source information.
     */
    fun createLocationAwareConstraint(
        constraint: TypeConstraint,
        location: EnhancedSourceLocation
    ): LocationAwareConstraint {
        return LocationAwareConstraint(constraint, location)
    }
    
    /**
     * Perform location-aware error recovery.
     * Attempts to provide meaningful error messages even when constraint solving fails.
     */
    fun performErrorRecovery(
        constraints: ConstraintSet,
        solverError: String,
        expressionLocation: EnhancedSourceLocation
    ): ErrorRecoveryResult {
        val recoveryStrategies = mutableListOf<RecoveryStrategy>()
        
        // Strategy 1: Identify the constraint that likely caused the failure
        val problematicConstraint = findProblematicConstraint(constraints, solverError)
        problematicConstraint?.let { constraint ->
            recoveryStrategies.add(RecoveryStrategy.ConstraintRelaxation(constraint))
        }
        
        // Strategy 2: Check for common error patterns
        when {
            solverError.contains("occurs check") -> {
                recoveryStrategies.add(RecoveryStrategy.InfiniteTypeDetection(expressionLocation))
            }
            solverError.contains("unify") -> {
                recoveryStrategies.add(RecoveryStrategy.TypeMismatch(expressionLocation))
            }
            solverError.contains("undefined") -> {
                recoveryStrategies.add(RecoveryStrategy.UndefinedVariable(expressionLocation))
            }
        }
        
        // Strategy 3: Suggest fixes based on location context
        expressionLocation.context?.let { context ->
            val contextualStrategy = createContextualRecoveryStrategy(context, expressionLocation)
            contextualStrategy?.let { recoveryStrategies.add(it) }
        }
        
        return ErrorRecoveryResult(
            originalError = solverError,
            location = expressionLocation,
            strategies = recoveryStrategies,
            suggestedFixes = generateSuggestedFixes(recoveryStrategies)
        )
    }
    
    private fun findProblematicConstraint(constraints: ConstraintSet, error: String): TypeConstraint? {
        // Simplified implementation - in practice would need more sophisticated analysis
        return constraints.all().firstOrNull()
    }
    
    private fun createContextualRecoveryStrategy(
        context: LocationContext,
        location: EnhancedSourceLocation
    ): RecoveryStrategy? {
        return when (context) {
            is LocationContext.FunctionApplication -> {
                RecoveryStrategy.FunctionApplicationError(context.functionName, location)
            }
            is LocationContext.FieldAccess -> {
                RecoveryStrategy.FieldAccessError(context.fieldName, location)
            }
            is LocationContext.VariableReference -> {
                RecoveryStrategy.VariableReferenceError(context.variableName, location)
            }
            else -> null
        }
    }
    
    private fun generateSuggestedFixes(strategies: List<RecoveryStrategy>): List<String> {
        return strategies.flatMap { strategy ->
            when (strategy) {
                is RecoveryStrategy.TypeMismatch -> listOf(
                    "Check that the types of your expressions match",
                    "Consider adding explicit type annotations"
                )
                is RecoveryStrategy.UndefinedVariable -> listOf(
                    "Make sure the variable is defined before use",
                    "Check for typos in variable names"
                )
                is RecoveryStrategy.InfiniteTypeDetection -> listOf(
                    "This appears to be a recursive type - consider using explicit recursive type definitions",
                    "Check for unintended circular references"
                )
                is RecoveryStrategy.FunctionApplicationError -> listOf(
                    "Check that the function is called with the correct number and types of arguments",
                    "Verify that '${strategy.functionName ?: "the function"}' is indeed a function"
                )
                is RecoveryStrategy.FieldAccessError -> listOf(
                    "Check that the field '${strategy.fieldName}' exists on the record",
                    "Verify the record type has the expected fields"
                )
                is RecoveryStrategy.VariableReferenceError -> listOf(
                    "Check that variable '${strategy.variableName}' is in scope",
                    "Verify the variable is defined before this point"
                )
                else -> emptyList()
            }
        }
    }
}

/**
 * A constraint with associated location information.
 */
data class LocationAwareConstraint(
    val constraint: TypeConstraint,
    val location: EnhancedSourceLocation
)

/**
 * Result of error recovery processing.
 */
data class ErrorRecoveryResult(
    val originalError: String,
    val location: EnhancedSourceLocation,
    val strategies: List<RecoveryStrategy>,
    val suggestedFixes: List<String>
)

/**
 * Different strategies for recovering from type errors.
 */
sealed class RecoveryStrategy {
    data class TypeMismatch(val location: EnhancedSourceLocation) : RecoveryStrategy()
    data class UndefinedVariable(val location: EnhancedSourceLocation) : RecoveryStrategy()
    data class InfiniteTypeDetection(val location: EnhancedSourceLocation) : RecoveryStrategy()
    data class ConstraintRelaxation(val constraint: TypeConstraint) : RecoveryStrategy()
    data class FunctionApplicationError(val functionName: String?, val location: EnhancedSourceLocation) : RecoveryStrategy()
    data class FieldAccessError(val fieldName: String, val location: EnhancedSourceLocation) : RecoveryStrategy()
    data class VariableReferenceError(val variableName: String, val location: EnhancedSourceLocation) : RecoveryStrategy()
}

/**
 * Utility functions for working with source locations.
 */
object SourceLocationUtils {
    
    /**
     * Merge multiple source locations into a single range.
     */
    fun mergeLocations(locations: List<EnhancedSourceLocation>): EnhancedSourceLocation? {
        if (locations.isEmpty()) return null
        
        val sorted = locations.sortedWith(compareBy({ it.line }, { it.column }))
        val first = sorted.first()
        val last = sorted.last()
        
        return EnhancedSourceLocation(
            line = first.line,
            column = first.column,
            endLine = last.endLine ?: last.line,
            endColumn = last.endColumn ?: last.column,
            filename = first.filename
        )
    }
    
    /**
     * Find the closest location to a given position.
     */
    fun findClosestLocation(
        target: EnhancedSourceLocation,
        candidates: List<EnhancedSourceLocation>
    ): EnhancedSourceLocation? {
        return candidates.minByOrNull { candidate ->
            val lineDiff = kotlin.math.abs(candidate.line - target.line)
            val columnDiff = kotlin.math.abs(candidate.column - target.column)
            lineDiff * 1000 + columnDiff  // Weight lines more heavily than columns
        }
    }
    
    /**
     * Create a location that spans from start to end.
     */
    fun createSpan(start: EnhancedSourceLocation, end: EnhancedSourceLocation): EnhancedSourceLocation {
        return EnhancedSourceLocation(
            line = start.line,
            column = start.column,
            endLine = end.line,
            endColumn = end.column,
            filename = start.filename ?: end.filename
        )
    }
} 