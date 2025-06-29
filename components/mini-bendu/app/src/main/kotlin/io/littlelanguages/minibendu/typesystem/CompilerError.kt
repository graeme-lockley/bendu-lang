package io.littlelanguages.minibendu.typesystem

/**
 * Comprehensive error system for the Mini-Bendu compiler.
 * Covers syntax, type, semantic, and internal errors with structured information.
 */

enum class ErrorCategory {
    SYNTAX,      // Parsing/syntax errors
    TYPE,        // Type checking errors  
    SEMANTIC,    // Semantic analysis errors
    INTERNAL     // Internal compiler errors
}

enum class CompilerErrorSeverity {
    ERROR,       // Blocks compilation
    WARNING,     // Compilation continues
    INFO         // Informational
}

/**
 * Base class for all compiler errors.
 */
sealed class CompilerError {
    abstract fun getMessage(): String
    abstract fun getCategory(): ErrorCategory
    abstract fun getSeverity(): CompilerErrorSeverity
    
    /**
     * Get a detailed error report with context.
     */
    open fun getDetailedMessage(): String = getMessage()
}

/**
 * Wrapper class that adds location information to any CompilerError.
 * This preserves the original error type while adding source location context.
 */
data class LocatedError(
    val originalError: CompilerError,
    val location: SourceLocation
) : CompilerError() {
    override fun getMessage(): String = "${originalError.getMessage()} at $location"
    override fun getCategory(): ErrorCategory = originalError.getCategory()
    override fun getSeverity(): CompilerErrorSeverity = originalError.getSeverity()
    override fun getDetailedMessage(): String = "${originalError.getDetailedMessage()} at $location"
}

// =============================================================================
// SYNTAX ERRORS
// =============================================================================

sealed class SyntaxError : CompilerError() {
    override fun getCategory(): ErrorCategory = ErrorCategory.SYNTAX
    override fun getSeverity(): CompilerErrorSeverity = CompilerErrorSeverity.ERROR
    
    data class UnexpectedToken(
        val expected: String,
        val actual: String,
        val position: String? = null
    ) : SyntaxError() {
        override fun getMessage(): String = 
            "Expected $expected but found '$actual'" + 
            (position?.let { " at $it" } ?: "")
    }
    
    data class UnterminatedString(
        val position: String? = null
    ) : SyntaxError() {
        override fun getMessage(): String = 
            "Unterminated string literal" + (position?.let { " at $it" } ?: "")
    }
    
    data class InvalidNumber(
        val value: String,
        val position: String? = null
    ) : SyntaxError() {
        override fun getMessage(): String = 
            "Invalid number format: '$value'" + (position?.let { " at $it" } ?: "")
    }
    
    data class UnbalancedParentheses(
        val position: String? = null
    ) : SyntaxError() {
        override fun getMessage(): String = 
            "Unbalanced parentheses" + (position?.let { " at $it" } ?: "")
    }
    
    data class GenericSyntaxError(
        val errorMessage: String,
        val position: String? = null
    ) : SyntaxError() {
        override fun getMessage(): String = 
            errorMessage + (position?.let { " at $it" } ?: "")
    }
}

// =============================================================================
// TYPE ERRORS
// =============================================================================

sealed class TypeError : CompilerError() {
    override fun getCategory(): ErrorCategory = ErrorCategory.TYPE
    override fun getSeverity(): CompilerErrorSeverity = CompilerErrorSeverity.ERROR
    
    data class TypeMismatch(
        val expected: Type,
        val actual: Type,
        val context: String? = null
    ) : TypeError() {
        override fun getMessage(): String {
            val contextMsg = if (context != null) " in $context" else ""
            return "Cannot unify $expected with $actual$contextMsg"
        }
    }
    
    data class UndefinedVariable(
        val variableName: String
    ) : TypeError() {
        override fun getMessage(): String = "Undefined variable: $variableName"
    }
    
    data class NonExhaustivePatternMatch(
        val missingPatterns: List<String>
    ) : TypeError() {
        override fun getMessage(): String {
            val patterns = missingPatterns.joinToString(", ")
            return "non-exhaustive pattern match. Missing patterns: $patterns"
        }
    }
    
    data class OverlappingPatterns(
        val pattern1: String,
        val pattern2: String
    ) : TypeError() {
        override fun getMessage(): String = "Overlapping patterns: $pattern1 and $pattern2"
    }
    
    data class UndefinedConstructor(
        val constructorName: String
    ) : TypeError() {
        override fun getMessage(): String = "Undefined constructor: $constructorName"
    }
    
    data class FunctionApplicationError(
        val functionType: Type,
        val argumentTypes: List<Type>,
        val details: String? = null
    ) : TypeError() {
        override fun getMessage(): String {
            return details ?: "Function application error: cannot apply $functionType to ${argumentTypes.joinToString(", ")}"
        }
    }
    
    data class OccursCheckFailure(
        val typeVariable: TypeVariable,
        val containingType: Type
    ) : TypeError() {
        override fun getMessage(): String = "Cannot construct infinite type: $typeVariable = $containingType"
    }
    
    data class InvalidTypeAnnotation(
        val annotation: String,
        val reason: String? = null
    ) : TypeError() {
        override fun getMessage(): String = 
            "Invalid type annotation: $annotation" + (reason?.let { " ($it)" } ?: "")
    }
    
    data class UndefinedTypeVariable(
        val typeVariableName: String
    ) : TypeError() {
        override fun getMessage(): String = "Undefined type variable: $typeVariableName"
    }
    
    data class SubtypingError(
        val subtype: Type,
        val supertype: Type,
        val reason: String? = null
    ) : TypeError() {
        override fun getMessage(): String = 
            "Cannot establish subtyping relationship $subtype <: $supertype" +
            (reason?.let { " ($it)" } ?: "")
    }
    
    data class RecordFieldMismatch(
        val fieldName: String,
        val expectedType: Type? = null,
        val actualType: Type? = null,
        val operation: String = "access"
    ) : TypeError() {
        override fun getMessage(): String = when {
            expectedType != null && actualType != null -> 
                "Field '$fieldName' type mismatch in $operation: expected $expectedType, got $actualType"
            expectedType == null && actualType != null -> 
                "Missing field '$fieldName' in $operation"
            else -> 
                "Field '$fieldName' error in $operation"
        }
    }
    
    data class TypeClassConstraintError(
        val typeClass: String,
        val type: Type,
        val reason: String? = null
    ) : TypeError() {
        override fun getMessage(): String = 
            "Type $type does not satisfy constraint $typeClass" +
            (reason?.let { " ($it)" } ?: "")
    }
    
    data class UnionCompatibilityError(
        val type1: Type,
        val type2: Type,
        val context: String? = null
    ) : TypeError() {
        override fun getMessage(): String = 
            "Cannot establish union compatibility between $type1 and $type2" +
            (context?.let { " in $context" } ?: "")
    }
    
    data class MergeOperationError(
        val details: String,
        val conflictingTypes: Pair<Type, Type>? = null
    ) : TypeError() {
        override fun getMessage(): String = 
            "Merge operation error: $details" +
            (conflictingTypes?.let { " (conflicting types: ${it.first} vs ${it.second})" } ?: "")
    }
}

// =============================================================================
// SEMANTIC ERRORS
// =============================================================================

sealed class SemanticError : CompilerError() {
    override fun getCategory(): ErrorCategory = ErrorCategory.SEMANTIC
    override fun getSeverity(): CompilerErrorSeverity = CompilerErrorSeverity.ERROR
    
    data class DuplicateDefinition(
        val name: String,
        val kind: String = "variable"
    ) : SemanticError() {
        override fun getMessage(): String = "Duplicate $kind definition: $name"
    }
    
    data class CircularTypeDefinition(
        val typeName: String
    ) : SemanticError() {
        override fun getMessage(): String = "Circular type definition: $typeName"
    }
    
    data class InvalidRecursion(
        val functionName: String
    ) : SemanticError() {
        override fun getMessage(): String = "Invalid recursion in function: $functionName"
    }
}

// =============================================================================
// INTERNAL ERRORS
// =============================================================================

sealed class InternalError : CompilerError() {
    override fun getCategory(): ErrorCategory = ErrorCategory.INTERNAL
    override fun getSeverity(): CompilerErrorSeverity = CompilerErrorSeverity.ERROR
    
    data class CompilerBug(
        val errorMessage: String,
        val exception: Throwable? = null
    ) : InternalError() {
        override fun getMessage(): String = 
            "Internal compiler error: $errorMessage" + 
            (exception?.let { " (${it.javaClass.simpleName}: ${it.message})" } ?: "")
    }
    
    data class UnimplementedFeature(
        val feature: String
    ) : InternalError() {
        override fun getMessage(): String = "Unimplemented feature: $feature"
    }
}

// =============================================================================
// WARNINGS
// =============================================================================

sealed class CompilerWarning : CompilerError() {
    override fun getSeverity(): CompilerErrorSeverity = CompilerErrorSeverity.WARNING
    
    data class UnusedVariable(
        val variableName: String
    ) : CompilerWarning() {
        override fun getCategory(): ErrorCategory = ErrorCategory.SEMANTIC
        override fun getMessage(): String = "Unused variable: $variableName"
    }
    
    data class UnreachablePattern(
        val pattern: String
    ) : CompilerWarning() {
        override fun getCategory(): ErrorCategory = ErrorCategory.TYPE
        override fun getMessage(): String = "Unreachable pattern: $pattern"
    }
    
    data class ShadowedVariable(
        val variableName: String
    ) : CompilerWarning() {
        override fun getCategory(): ErrorCategory = ErrorCategory.SEMANTIC
        override fun getMessage(): String = "Variable shadows previous definition: $variableName"
    }
}

// =============================================================================
// HELPER FUNCTIONS
// =============================================================================

/**
 * Convert legacy string errors to structured errors.
 */
fun String.toCompilerError(): CompilerError {
    return when {
        contains("Parse error", ignoreCase = true) -> 
            SyntaxError.GenericSyntaxError(this)
        contains("Cannot unify", ignoreCase = true) -> {
            // Try to extract type information
            TypeError.TypeMismatch(
                expected = Types.Unit, // Fallback
                actual = Types.Unit,   // Fallback
                context = this
            )
        }
        contains("Undefined variable", ignoreCase = true) -> {
            val varName = Regex("Undefined variable: (\\w+)").find(this)?.groupValues?.get(1) ?: "unknown"
            TypeError.UndefinedVariable(varName)
        }
        contains("Undefined type variable", ignoreCase = true) -> {
            val typeName = Regex("Undefined type variable: (\\w+)").find(this)?.groupValues?.get(1) ?: "unknown"
            TypeError.UndefinedTypeVariable(typeName)
        }
        contains("non-exhaustive pattern match", ignoreCase = true) -> {
            TypeError.NonExhaustivePatternMatch(emptyList()) // Could parse patterns from message
        }
        contains("Internal", ignoreCase = true) -> 
            InternalError.CompilerBug(this)
        else -> 
            InternalError.CompilerBug("Unknown error: $this")
    }
}

/**
 * Unified exception class for all compiler errors.
 * This replaces the various specific exception types (ConstraintGenerationException, UnificationException, etc.)
 * with a single, consistent approach that preserves structured error information.
 */
class CompilerErrorException(val compilerError: CompilerError) : Exception(compilerError.getMessage()) {
    // Backward compatibility constructor for string messages
    constructor(message: String) : this(InternalError.CompilerBug(message))
    
    companion object {
        // Factory methods for common error types
        fun undefinedVariable(name: String): CompilerErrorException =
            CompilerErrorException(TypeError.UndefinedVariable(name))
            
        fun typeMismatch(expected: Type, actual: Type, context: String? = null): CompilerErrorException =
            CompilerErrorException(TypeError.TypeMismatch(expected, actual, context))
            
        fun unificationFailure(message: String): CompilerErrorException =
            CompilerErrorException(TypeError.TypeMismatch(Types.Unit, Types.Unit, message))
            
        // Better factory methods for specific unification failures
        fun cannotUnifyTypes(type1: Type, type2: Type, context: String? = null): CompilerErrorException =
            CompilerErrorException(TypeError.TypeMismatch(type1, type2, context))
            
        fun occursCheckFailure(typeVar: TypeVariable, containingType: Type): CompilerErrorException =
            CompilerErrorException(TypeError.OccursCheckFailure(typeVar, containingType))
            
        fun tupleLengthMismatch(expected: Int, actual: Int): CompilerErrorException =
            CompilerErrorException(TypeError.TypeMismatch(
                Types.Unit, // Placeholder - we don't have tuple type constructors here
                Types.Unit, // Placeholder
                "Cannot unify tuples of different lengths: $expected vs $actual"
            ))
            
        fun recordFieldMismatch(fieldName: String, expected: Type, actual: Type): CompilerErrorException =
            CompilerErrorException(TypeError.RecordFieldMismatch(fieldName, expected, actual))
            
        fun nonExhaustivePattern(missingPatterns: List<String>): CompilerErrorException =
            CompilerErrorException(TypeError.NonExhaustivePatternMatch(missingPatterns))
            
        fun syntaxError(message: String, position: String? = null): CompilerErrorException =
            CompilerErrorException(SyntaxError.GenericSyntaxError(message, position))
        
        // New factory methods for constraint solver errors
        fun subtypingError(subtype: Type, supertype: Type, reason: String? = null): CompilerErrorException =
            CompilerErrorException(TypeError.SubtypingError(subtype, supertype, reason))
        
        fun missingRecordField(fieldName: String, operation: String = "record subtyping"): CompilerErrorException =
            CompilerErrorException(TypeError.RecordFieldMismatch(fieldName, operation = operation))
        
        fun recordFieldConflict(fieldName: String, expected: Type, actual: Type, operation: String = "record operation"): CompilerErrorException =
            CompilerErrorException(TypeError.RecordFieldMismatch(fieldName, expected, actual, operation))
        
        fun typeClassConstraintError(typeClass: String, type: Type, reason: String? = null): CompilerErrorException =
            CompilerErrorException(TypeError.TypeClassConstraintError(typeClass, type, reason))
        
        fun unionCompatibilityError(type1: Type, type2: Type, context: String? = null): CompilerErrorException =
            CompilerErrorException(TypeError.UnionCompatibilityError(type1, type2, context))
        
        fun mergeOperationError(details: String, conflictingTypes: Pair<Type, Type>? = null): CompilerErrorException =
            CompilerErrorException(TypeError.MergeOperationError(details, conflictingTypes))
        
        fun cannotConstrainToRecordType(type: Type): CompilerErrorException =
            CompilerErrorException(TypeError.TypeMismatch(
                expected = Types.Unit, // Placeholder for "record type"
                actual = type,
                context = "cannot be constrained to record type"
            ))
            
        fun cannotSpreadNonRecordType(type: Type): CompilerErrorException =
            CompilerErrorException(TypeError.MergeOperationError(
                "Cannot spread non-record type $type",
                null
            ))
    }
} 