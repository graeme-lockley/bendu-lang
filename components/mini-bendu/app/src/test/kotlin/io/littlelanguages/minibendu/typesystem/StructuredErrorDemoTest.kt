package io.littlelanguages.minibendu.typesystem

import io.littlelanguages.minibendu.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Demonstration of the structured error system.
 * Shows how different types of errors are handled and can be processed programmatically.
 */
class StructuredErrorDemoTest {

    @Test
    fun `demonstrate comprehensive error system`() {
        println("\n=== MINI-BENDU STRUCTURED ERROR SYSTEM DEMONSTRATION ===\n")
        
        // 1. Demonstrate different error categories
        demonstrateErrorCategories()
        
        // 2. Demonstrate error conversion
        demonstrateErrorConversion()
        
        // 3. Demonstrate error handling in the compiler
        demonstrateCompilerErrorHandling()
        
        println("=== DEMONSTRATION COMPLETE ===\n")
    }

    @Test
    fun `enhanced unification result preserves structured error information`() {
        println("\n=== Enhanced UnificationResult Demo ===")
        
        // Demonstrate that UnificationResult now preserves structured errors
        // instead of converting them to strings at internal boundaries
        
        println("\n1. Type Mismatch Errors:")
        val intType = Types.Int
        val stringType = Types.String
        val result = Unification.unify(intType, stringType)
        
        assertTrue(result.isFailure(), "Should fail to unify Int and String")
        
        // The structured error is preserved!
        val compilerError = result.getCompilerError()
        assertNotNull(compilerError, "Should have structured error")
        assertTrue(compilerError is TypeError.TypeMismatch, "Should be TypeMismatch")
        
        val typeMismatch = compilerError as TypeError.TypeMismatch
        println("   Expected: ${typeMismatch.expected}")
        println("   Actual: ${typeMismatch.actual}")
        println("   Error Category: ${typeMismatch.getCategory()}")
        println("   Error Severity: ${typeMismatch.getSeverity()}")
        
        // Backward compatibility still works
        println("   Legacy string: ${result.getError()}")
        
        println("\n2. Occurs Check Failures:")
        val typeVar = TypeVariable.fresh()
        val recursiveType = FunctionType(typeVar, Types.Int)
        val occursResult = Unification.unify(typeVar, recursiveType)
        
        assertTrue(occursResult.isFailure(), "Occurs check should fail")
        val occursError = occursResult.getCompilerError()
        assertTrue(occursError is TypeError.OccursCheckFailure, "Should be OccursCheckFailure")
        
        val occurs = occursError as TypeError.OccursCheckFailure
        println("   Type Variable: ${occurs.typeVariable}")
        println("   Containing Type: ${occurs.containingType}")
        println("   Error Category: ${occurs.getCategory()}")
        println("   Legacy string: ${occursResult.getError()}")
        
        println("\n3. Tuple Length Mismatches:")
        val tuple2 = TupleType(listOf(Types.Int, Types.String))
        val tuple3 = TupleType(listOf(Types.Int, Types.String, Types.Bool))
        val tupleResult = Unification.unify(tuple2, tuple3)
        
        assertTrue(tupleResult.isFailure(), "Tuple length mismatch should fail")
        val tupleError = tupleResult.getCompilerError()
        assertTrue(tupleError is TypeError.TypeMismatch, "Should be TypeMismatch for tuples")
        
        println("   Tuple Error: ${tupleResult.getError()}")
        println("   Error Category: ${tupleError!!.getCategory()}")
        
        println("\n4. Error Processing Pipeline:")
        // Demonstrate how errors flow through the system while preserving structure
        val errors = listOf(result, occursResult, tupleResult)
        
        val typeErrors = errors.mapNotNull { it.getCompilerError() as? TypeError }
        println("   Found ${typeErrors.size} type errors")
        
        val typeMismatches = typeErrors.filterIsInstance<TypeError.TypeMismatch>()
        println("   Found ${typeMismatches.size} type mismatches")
        
        val occursErrors = typeErrors.filterIsInstance<TypeError.OccursCheckFailure>()
        println("   Found ${occursErrors.size} occurs check failures")
        
        // All errors maintain their structured information!
        typeErrors.forEach { error ->
            println("   - ${error::class.simpleName}: ${error.getMessage()}")
        }
        
        println("\n=== Structured Error Information Preserved Throughout Pipeline ===")
    }

    @Test
    fun `enhanced constraint solver result preserves structured error information`() {
        println("\n=== Enhanced ConstraintSolverResult Demo ===")
        
        // Demonstrate that ConstraintSolverResult now preserves structured errors
        // instead of converting them to strings at internal boundaries
        
        println("\n1. Type Mismatch in Constraint Solving:")
        val typeVar = TypeVariable.fresh()
        val constraint1 = EqualityConstraint(typeVar, Types.Int, SourceLocation(10, 5, "test.bendu"))
        val constraint2 = EqualityConstraint(typeVar, Types.String, SourceLocation(15, 8, "test.bendu"))
        val constraintSet = ConstraintSet.of(constraint1, constraint2)
        
        val solver = ConstraintSolver()
        val result = solver.solve(constraintSet)
        
        assertTrue(result is ConstraintSolverResult.Failure, "Should fail to solve contradictory constraints")
        val failure = result as ConstraintSolverResult.Failure
        
        println("  - Result type: ${failure::class.simpleName}")
        println("  - Compiler error type: ${failure.compilerError::class.simpleName}")
        println("  - Error message: ${failure.error}")
        
        // The structured error is preserved with location information!
        assertTrue(failure.compilerError is LocatedError, "Should be wrapped with location")
        val locatedError = failure.compilerError as LocatedError
        
        assertTrue(locatedError.originalError is TypeError.TypeMismatch, "Original error should be TypeMismatch")
        val typeMismatch = locatedError.originalError as TypeError.TypeMismatch
        
        println("  - Expected type: ${typeMismatch.expected}")
        println("  - Actual type: ${typeMismatch.actual}")
        println("  - Location: ${locatedError.location}")
        
        assertEquals(Types.Int, typeMismatch.expected, "Expected type preserved")
        assertEquals(Types.String, typeMismatch.actual, "Actual type preserved")
        
        println("\n2. Occurs Check Failure in Constraint Solving:")
        val typeVar2 = TypeVariable.fresh()
        val recursiveType = FunctionType(typeVar2, Types.String)
        val constraint3 = EqualityConstraint(typeVar2, recursiveType, SourceLocation(20, 10, "test.bendu"))
        val constraintSet2 = ConstraintSet.of(constraint3)
        
        val result2 = solver.solve(constraintSet2)
        
        assertTrue(result2 is ConstraintSolverResult.Failure, "Should fail occurs check")
        val failure2 = result2 as ConstraintSolverResult.Failure
        
        println("  - Result type: ${failure2::class.simpleName}")
        println("  - Compiler error type: ${failure2.compilerError::class.simpleName}")
        println("  - Error message: ${failure2.error}")
        
        // The occurs check error is also preserved with location!
        assertTrue(failure2.compilerError is LocatedError, "Should be wrapped with location")
        val locatedError2 = failure2.compilerError as LocatedError
        
        assertTrue(locatedError2.originalError is TypeError.OccursCheckFailure, "Original error should be OccursCheckFailure")
        val occursError = locatedError2.originalError as TypeError.OccursCheckFailure
        
        println("  - Type variable: ${occursError.typeVariable}")
        println("  - Containing type: ${occursError.containingType}")
        println("  - Location: ${locatedError2.location}")
        
        assertEquals(typeVar2, occursError.typeVariable, "Type variable preserved")
        assertEquals(recursiveType, occursError.containingType, "Containing type preserved")
        
        println("\n3. Error Processing Pipeline:")
        val results = listOf(failure, failure2)
        
        // Process errors by type - we can now access rich structured information
        val originalErrors = results.map { 
            val error = it.compilerError
            if (error is LocatedError) error.originalError else error
        }
        
        val typeMismatches = originalErrors.filterIsInstance<TypeError.TypeMismatch>()
        val occursErrors = originalErrors.filterIsInstance<TypeError.OccursCheckFailure>()
        
        println("  - Processed ${results.size} constraint solver failures")
        println("  - Found ${typeMismatches.size} type mismatches")
        println("  - Found ${occursErrors.size} occurs check failures")
        
        // We can access detailed information from each error type
        typeMismatches.forEach { error ->
            println("    * Type mismatch: ${error.expected} vs ${error.actual}")
        }
        
        occursErrors.forEach { error ->
            println("    * Occurs check: ${error.typeVariable} in ${error.containingType}")
        }
        
        println("\n✅ ConstraintSolverResult now preserves rich structured error information!")
        println("✅ Source location context is maintained through LocatedError wrapper!")
        println("✅ Error type semantics are preserved for programmatic processing!")
    }

    private fun demonstrateErrorCategories() {
        println("1. ERROR CATEGORIES AND TYPES:")
        println("------------------------------")
        
        // Syntax Errors
        val syntaxError = SyntaxError.UnexpectedToken("identifier", "number", "line 1:5")
        printErrorInfo("Syntax Error", syntaxError)
        
        val parseError = SyntaxError.GenericSyntaxError("Parse error: unexpected end of input")
        printErrorInfo("Parse Error", parseError)
        
        // Type Errors
        val undefinedVar = TypeError.UndefinedVariable("unknownVar")
        printErrorInfo("Undefined Variable", undefinedVar)
        
        val typeMismatch = TypeError.TypeMismatch(Types.Int, Types.String, "function application")
        printErrorInfo("Type Mismatch", typeMismatch)
        
        val nonExhaustive = TypeError.NonExhaustivePatternMatch(listOf("None", "Some(_)"))
        printErrorInfo("Non-Exhaustive Pattern", nonExhaustive)
        
        // Semantic Errors
        val duplicateDef = SemanticError.DuplicateDefinition("myFunction", "function")
        printErrorInfo("Duplicate Definition", duplicateDef)
        
        // Internal Errors
        val compilerBug = InternalError.CompilerBug("Unexpected null pointer", NullPointerException("test"))
        printErrorInfo("Compiler Bug", compilerBug)
        
        // Warnings
        val unusedVar = CompilerWarning.UnusedVariable("tempVar")
        printErrorInfo("Unused Variable Warning", unusedVar)
        
        println()
    }

    private fun demonstrateErrorConversion() {
        println("2. ERROR CONVERSION FROM STRINGS:")
        println("---------------------------------")
        
        val stringErrors = listOf(
            "Undefined variable: testVar",
            "Cannot unify Int with String in function application",
            "non-exhaustive pattern match. Missing patterns: None",
            "Parse error: unexpected token",
            "Internal compiler error: stack overflow",
            "Some unknown error message"
        )
        
        stringErrors.forEach { errorString ->
            val structuredError = errorString.toCompilerError()
            println("String: \"$errorString\"")
            println("  -> Converted to: ${structuredError::class.simpleName}")
            println("  -> Category: ${structuredError.getCategory()}")
            println("  -> Message: ${structuredError.getMessage()}")
            println()
        }
    }

    private fun demonstrateCompilerErrorHandling() {
        println("3. COMPILER ERROR HANDLING:")
        println("---------------------------")
        
        // Test undefined variable error
        val result1 = parseAndTypeCheck("undefinedVariable")
        if (result1 is TypeCheckResult.Failure) {
            println("Undefined Variable Error:")
            println("  String Error: ${result1.error}")
            println("  Structured Error: ${result1.structuredError?.let { it::class.simpleName } ?: "none"}")
            println("  Location: ${result1.location}")
            println()
        }
        
        // Test syntax error
        val syntaxResult = TypeCheckResult.Failure(
            structuredError = SyntaxError.UnterminatedString("line 2:10"),
            location = SourceLocation(2, 10)
        )
        println("Syntax Error Example:")
        println("  String Error: ${syntaxResult.error}")
        println("  Structured Error: ${syntaxResult.structuredError?.let { it::class.simpleName }}")
        println("  Category: ${syntaxResult.structuredError?.getCategory()}")
        println("  Severity: ${syntaxResult.structuredError?.getSeverity()}")
        println()
        
        // Show how errors can be processed programmatically
        demonstrateErrorProcessing(listOf(syntaxResult, result1))
    }

    private fun demonstrateErrorProcessing(errors: List<TypeCheckResult>) {
        println("4. PROGRAMMATIC ERROR PROCESSING:")
        println("---------------------------------")
        
        val errorsByCategory = errors.filterIsInstance<TypeCheckResult.Failure>()
            .groupBy { it.structuredError?.getCategory() ?: ErrorCategory.INTERNAL }
        
        errorsByCategory.forEach { (category, categoryErrors) ->
            println("${category.name} ERRORS (${categoryErrors.size}):")
            categoryErrors.forEach { error ->
                println("  - ${error.error}")
                if (error.structuredError != null) {
                    println("    Type: ${error.structuredError::class.simpleName}")
                    println("    Severity: ${error.structuredError.getSeverity()}")
                }
            }
            println()
        }
        
        // Show how to filter by severity
        val criticalErrors = errors.filterIsInstance<TypeCheckResult.Failure>()
            .filter { it.structuredError?.getSeverity() == CompilerErrorSeverity.ERROR }
        
        println("CRITICAL ERRORS: ${criticalErrors.size}")
        criticalErrors.forEach { error ->
            println("  - ${error.error}")
        }
        println()
    }

    private fun printErrorInfo(name: String, error: CompilerError) {
        println("$name:")
        println("  Type: ${error::class.simpleName}")
        println("  Category: ${error.getCategory()}")
        println("  Severity: ${error.getSeverity()}")
        println("  Message: ${error.getMessage()}")
        println()
    }

    private fun parseAndTypeCheck(source: String): TypeCheckResult {
        return try {
            val errors = Errors()
            val program = parse(source, errors)

            if (errors.hasErrors()) {
                val errorMessages = mutableListOf<String>()
                for (error in errors) {
                    errorMessages.add(error.toString())
                }
                TypeCheckResult.Failure(
                    structuredError = SyntaxError.GenericSyntaxError("Parse error: ${errorMessages.joinToString("; ")}"),
                    location = SourceLocation(1, 1)
                )
            } else {
                val typeChecker = TypeChecker()
                val incrementalResult = typeChecker.typeCheckProgram(program)

                if (incrementalResult.hasErrors) {
                    incrementalResult.errors.first()
                } else {
                    val expressionResults = incrementalResult.results.zip(program.topLevels)
                        .filter { (_, topLevel) -> topLevel is ExprStmt }
                        .map { (result, _) -> result }

                    if (expressionResults.isEmpty()) {
                        TypeCheckResult.Failure(
                            structuredError = InternalError.CompilerBug("No expressions found in program"),
                            location = SourceLocation(1, 1)
                        )
                    } else {
                        expressionResults.last()
                    }
                }
            }
        } catch (e: Exception) {
            TypeCheckResult.Failure(
                structuredError = SyntaxError.GenericSyntaxError("Parse error: ${e.message}"),
                location = SourceLocation(1, 1)
            )
        }
    }
} 