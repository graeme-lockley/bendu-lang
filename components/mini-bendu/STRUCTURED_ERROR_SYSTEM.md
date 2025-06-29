# Mini-Bendu Structured Error System

## Overview

This document describes the comprehensive structured error system implemented for the Mini-Bendu compiler. The system provides both backward compatibility with existing string-based errors and new structured error types for improved error handling and programmatic processing.

## Architecture

### Core Components

1. **CompilerError.kt** - Defines the complete error hierarchy
2. **TypeChecker.kt** - Updated to support structured errors with backward compatibility
3. **ConstraintGenerator.kt** - Enhanced to generate structured errors
4. **MarkdownTestRunner.kt** - Updated to handle both string and structured errors

### Error Hierarchy

```kotlin
sealed class CompilerError {
    abstract fun getMessage(): String
    abstract fun getCategory(): ErrorCategory
    abstract fun getSeverity(): CompilerErrorSeverity
}
```

#### Error Categories

- **SYNTAX** - Parsing and syntax errors
- **TYPE** - Type checking errors
- **SEMANTIC** - Semantic analysis errors  
- **INTERNAL** - Internal compiler errors

#### Error Severities

- **ERROR** - Blocks compilation
- **WARNING** - Compilation continues
- **INFO** - Informational messages

## Error Types

### Syntax Errors

```kotlin
sealed class SyntaxError : CompilerError()
```

- `UnexpectedToken` - Wrong token found during parsing
- `UnterminatedString` - String literal not properly closed
- `InvalidNumber` - Malformed numeric literal
- `UnbalancedParentheses` - Mismatched parentheses
- `GenericSyntaxError` - General syntax error with message

### Type Errors

```kotlin
sealed class TypeError : CompilerError()
```

- `TypeMismatch` - Types cannot be unified
- `UndefinedVariable` - Variable not found in scope
- `NonExhaustivePatternMatch` - Pattern match missing cases
- `OverlappingPatterns` - Pattern match has unreachable cases
- `UndefinedConstructor` - Constructor not found
- `FunctionApplicationError` - Function cannot be applied to arguments
- `OccursCheckFailure` - Infinite type detected
- `InvalidTypeAnnotation` - Type annotation is malformed
- `UndefinedTypeVariable` - Type variable not found in scope

### Semantic Errors

```kotlin
sealed class SemanticError : CompilerError()
```

- `DuplicateDefinition` - Name defined multiple times
- `CircularTypeDefinition` - Type definition refers to itself
- `InvalidRecursion` - Invalid recursive function

### Internal Errors

```kotlin
sealed class InternalError : CompilerError()
```

- `CompilerBug` - Internal compiler error with optional exception
- `UnimplementedFeature` - Feature not yet implemented

### Warnings

```kotlin
sealed class CompilerWarning : CompilerError()
```

- `UnusedVariable` - Variable defined but not used
- `UnreachablePattern` - Pattern will never match
- `ShadowedVariable` - Variable shadows previous definition

## Backward Compatibility

The system maintains full backward compatibility through a dual-error approach:

```kotlin
data class Failure(
    val error: String,                    // Legacy string error
    val location: SourceLocation?,
    val structuredError: CompilerError? = null  // New structured error
) : TypeCheckResult() {
    // Constructor for structured errors with backward compatibility
    constructor(structuredError: CompilerError, location: SourceLocation?) : this(
        error = structuredError.getMessage(),
        location = location,
        structuredError = structuredError
    )
}
```

### String to Structured Error Conversion

A conversion function transforms legacy string errors to structured errors:

```kotlin
fun String.toCompilerError(): CompilerError {
    return when {
        contains("Parse error", ignoreCase = true) -> 
            SyntaxError.GenericSyntaxError(this)
        contains("Cannot unify", ignoreCase = true) -> 
            TypeError.TypeMismatch(Types.Unit, Types.Unit, context = this)
        contains("Undefined variable", ignoreCase = true) -> {
            val varName = Regex("Undefined variable: (\\w+)").find(this)?.groupValues?.get(1) ?: "unknown"
            TypeError.UndefinedVariable(varName)
        }
        contains("non-exhaustive pattern match", ignoreCase = true) -> 
            TypeError.NonExhaustivePatternMatch(emptyList())
        else -> 
            InternalError.CompilerBug("Unknown error: $this")
    }
}
```

## Implementation Details

### Enhanced Test Runner

The `MarkdownTestRunner` now supports both string and structured error matching:

```kotlin
val matches = when {
    expectedErrorPart.contains("non-exhaustive pattern match") -> 
        result.structuredError is TypeError.NonExhaustivePatternMatch || 
        result.error.lowercase().contains("non-exhaustive")
    expectedErrorPart.contains("cannot unify") -> 
        result.structuredError is TypeError.TypeMismatch || 
        result.error.lowercase().contains("cannot unify")
    // ... more cases
    else -> 
        result.error.lowercase().contains(expectedErrorPart)
}
```

### Error Generation

Key locations where structured errors are generated:

1. **ConstraintGenerator** - Undefined variable and type variable errors
2. **ConstraintSolver** - Non-exhaustive pattern match errors  
3. **TypeChecker** - Internal errors and exception handling
4. **MarkdownTestRunner** - Parse errors and test failures

### Enhanced ConstraintGenerationException

The `ConstraintGenerationException` now takes `CompilerError` objects instead of strings, providing better error context and type safety during constraint generation:

```kotlin
class ConstraintGenerationException(val compilerError: CompilerError) : Exception(compilerError.getMessage()) {
    // Backward compatibility constructor for string messages
    constructor(message: String) : this(InternalError.CompilerBug(message))
}
```

This ensures that all constraint generation errors are properly structured and categorized. The exception also flows through to `ConstraintGenerationResult.Failure` which now includes structured error support.

### Enhanced UnificationResult

The `UnificationResult.Failure` class now preserves structured error information instead of converting to strings at internal boundaries:

```kotlin
sealed class UnificationResult {
    // ...
    data class Failure(private val compilerError: CompilerError) : UnificationResult() {
        override fun getError(): String = compilerError.getMessage()
        override fun getCompilerError(): CompilerError = compilerError
        
        // Backward compatibility constructor for string messages
        constructor(error: String) : this(InternalError.CompilerBug(error))
    }
}
```

This change ensures that structured error information flows through the unification pipeline without loss of semantic information. The unification system now preserves rich error details like type mismatches, occurs check failures, and tuple length mismatches as structured `CompilerError` objects rather than converting them to strings prematurely.

### Enhanced ConstraintSolverResult

The `ConstraintSolverResult.Failure` class now preserves structured error information from unification while adding source location context:

```kotlin
sealed class ConstraintSolverResult {
    // ...
    data class Failure(val compilerError: CompilerError) : ConstraintSolverResult() {
        // Backward compatibility - get error message as string
        val error: String get() = compilerError.getMessage()
        
        // Backward compatibility constructor for string messages
        constructor(error: String) : this(InternalError.CompilerBug(error))
    }
}
```

### Location-Aware Error Wrapping

To preserve both structured error information AND source location context, the system uses a `LocatedError` wrapper:

```kotlin
data class LocatedError(
    val originalError: CompilerError,
    val location: SourceLocation
) : CompilerError() {
    override fun getMessage(): String = "${originalError.getMessage()} at $location"
    // Delegates other methods to originalError
}
```

This approach ensures that:
- **Error type semantics are preserved** (e.g., `TypeError.OccursCheckFailure` remains distinct from `TypeError.TypeMismatch`)
- **Location information is included** in error messages for debugging
- **Structured information remains accessible** via the `originalError` property
- **Backward compatibility is maintained** for all existing error handling code

The constraint solver now preserves structured errors from unification while enhancing them with source location information, creating a complete error context without losing semantic meaning.

## Benefits

### For Developers

1. **Type Safety** - Errors are strongly typed, preventing runtime issues
2. **Rich Information** - Structured errors contain more context than strings
3. **Programmatic Processing** - Errors can be filtered, grouped, and processed
4. **Better IDE Support** - IDEs can provide better error handling assistance

### For Users

1. **Consistent Formatting** - All errors follow the same structure
2. **Better Error Messages** - More context and suggestions
3. **Categorized Errors** - Users can understand error types at a glance
4. **Improved Debugging** - Source locations and context information

### For Tools

1. **Error Analysis** - Tools can analyze error patterns
2. **Custom Handling** - Different error types can be handled differently
3. **Reporting** - Generate structured error reports
4. **Integration** - Easy integration with IDEs and other tools

## Usage Examples

### Basic Error Handling

```kotlin
when (result) {
    is TypeCheckResult.Success -> {
        // Handle success
    }
    is TypeCheckResult.Failure -> {
        // Legacy string error
        println("Error: ${result.error}")
        
        // Structured error processing
        when (val structuredError = result.structuredError) {
            is TypeError.UndefinedVariable -> {
                println("Undefined variable: ${structuredError.variableName}")
                // Suggest similar variables
            }
            is TypeError.TypeMismatch -> {
                println("Type mismatch: expected ${structuredError.expected}, got ${structuredError.actual}")
            }
            is SyntaxError -> {
                println("Syntax error: ${structuredError.getMessage()}")
            }
            // ... handle other error types
        }
    }
}
```

### Error Filtering and Processing

```kotlin
val errors: List<TypeCheckResult.Failure> = // ... get errors

// Group by category
val errorsByCategory = errors.groupBy { 
    it.structuredError?.getCategory() ?: ErrorCategory.INTERNAL 
}

// Filter by severity
val criticalErrors = errors.filter { 
    it.structuredError?.getSeverity() == CompilerErrorSeverity.ERROR 
}

// Find specific error types
val undefinedVarErrors = errors.mapNotNull { 
    it.structuredError as? TypeError.UndefinedVariable 
}
```

## Migration Path

The structured error system is designed for gradual adoption:

1. **Phase 1** - All existing code continues to work with string errors
2. **Phase 2** - New code can optionally use structured errors
3. **Phase 3** - Gradually migrate existing code to use structured errors
4. **Phase 4** - Eventually deprecate string-only error handling

## Testing

The system includes comprehensive tests:

- **StructuredErrorTest** - Tests core error functionality
- **StructuredErrorDemoTest** - Demonstrates the system capabilities
- **Pattern matching tests** - Verify error handling in real scenarios

All existing tests continue to pass, ensuring no regressions.

## Future Enhancements

Potential improvements to the error system:

1. **Error Recovery** - Enhanced error recovery with structured information
2. **Error Suggestions** - AI-powered error suggestions based on error types
3. **Error Metrics** - Collect metrics on error frequencies and patterns
4. **Custom Error Types** - Allow plugins to define custom error types
5. **Error Localization** - Support for multiple languages in error messages

## Conclusion

The structured error system provides a robust foundation for error handling in Mini-Bendu while maintaining complete backward compatibility. It enables better tooling, improved user experience, and more maintainable code. 