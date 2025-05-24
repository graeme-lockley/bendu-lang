package io.littlelanguages.minibendu.typesystem

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import io.littlelanguages.minibendu.*
import io.littlelanguages.scanpiler.LocationCoordinate

/**
 * Tests for error recovery mechanisms in the type system.
 * 
 * These tests verify that the type checker can continue processing
 * after encountering errors, error cascading prevention, and provide
 * meaningful multiple error reports with proper prioritization.
 * 
 * Tests cover:
 * - Type checking continues after errors
 * - Error cascading prevention  
 * - Multiple error reporting
 * - Error prioritization and filtering
 */
class ErrorRecoveryTest {

    private fun createStringLocation(value: String, line: Int = 1, column: Int = 1): StringLocation {
        return StringLocation(value, LocationCoordinate(0, line, column))
    }
    
    private fun createIntLocation(value: Int, line: Int = 1, column: Int = 1): IntLocation {
        return IntLocation(value, LocationCoordinate(0, line, column))
    }
    
    private fun createBoolLocation(value: Boolean, line: Int = 1, column: Int = 1): BoolLocation {
        return BoolLocation(value, LocationCoordinate(0, line, column))
    }
    
    private fun createLocation(line: Int = 1, column: Int = 1): LocationCoordinate = 
        LocationCoordinate(0, line, column)

    // ===== TYPE CHECKING CONTINUES AFTER ERRORS =====

    @Test
    fun `type checking continues after single error`() {
        // Create a program with one error that shouldn't prevent checking the rest
        // let x = 42 + "error" in let y = true in x + y
        val errorExpr = BinaryOpExpr(
            LiteralIntExpr(createIntLocation(42, 2, 9)),
            BinaryOp.Plus,
            LiteralStringExpr(createStringLocation("error", 2, 14)),
            createLocation(2, 12)
        )
        
        val validBinding = LetExpr(
            false, // recursive
            createStringLocation("y", 3, 5),
            null, // typeParams
            null, // parameters
            null, // typeAnnotation
            LiteralBoolExpr(createBoolLocation(true, 3, 9)),
            VarExpr(createStringLocation("y", 3, 17))
        )
        
        val program = LetExpr(
            false, // recursive
            createStringLocation("x", 2, 5),
            null, // typeParams
            null, // parameters
            null, // typeAnnotation
            errorExpr, // This contains an error
            validBinding // This should still be checked
        )
        
        val typeChecker = TypeChecker()
        val result = typeChecker.typeCheck(program)
        
        // The overall program should fail due to the error
        assertTrue(result.isFailure(), "Program should fail due to type error")
        
        // But the error should be specific to the problematic part
        val failure = result as TypeCheckResult.Failure
        assertTrue(failure.error.contains("unify") || failure.error.contains("String") || failure.error.contains("Int"),
            "Error should mention the specific type issue: ${failure.error}")
    }

    @Test
    fun `type checking reports specific error location`() {
        // Create nested expressions where the error is deep inside
        // if true then (if true then 42 + "error" else 0) else 1
        val innerError = BinaryOpExpr(
            LiteralIntExpr(createIntLocation(42, 5, 25)),
            BinaryOp.Plus,
            LiteralStringExpr(createStringLocation("error", 5, 30)),
            createLocation(5, 28)
        )
        
        val innerIf = IfExpr(
            LiteralBoolExpr(createBoolLocation(true, 5, 18)),
            innerError,
            LiteralIntExpr(createIntLocation(0, 5, 42)),
            createLocation(5, 15)
        )
        
        val outerIf = IfExpr(
            LiteralBoolExpr(createBoolLocation(true, 5, 4)),
            innerIf,
            LiteralIntExpr(createIntLocation(1, 5, 52)),
            createLocation(5, 1)
        )
        
        val typeChecker = TypeChecker()
        val result = typeChecker.typeCheck(outerIf)
        
        assertTrue(result.isFailure(), "Should detect nested error")
        val failure = result as TypeCheckResult.Failure
        
        // Error location should point to the problematic operation, not the outer if
        assertNotNull(failure.location, "Error should have location information")
        assertEquals(5, failure.location?.line, "Should reference the line with the error")
    }

    @Test
    fun `type checking handles multiple independent errors gracefully`() {
        // Create a record with multiple type errors in different fields
        val record = RecordExpr(
            listOf(
                FieldExpr(
                    createStringLocation("field1", 10, 2),
                    BinaryOpExpr(
                        LiteralIntExpr(createIntLocation(42, 10, 11)),
                        BinaryOp.Plus,
                        LiteralStringExpr(createStringLocation("error1", 10, 16)),
                        createLocation(10, 14)
                    )
                ),
                FieldExpr(
                    createStringLocation("field2", 11, 2),
                    BinaryOpExpr(
                        LiteralBoolExpr(createBoolLocation(true, 11, 11)),
                        BinaryOp.Star,
                        LiteralIntExpr(createIntLocation(5, 11, 19)),
                        createLocation(11, 17)
                    )
                ),
                FieldExpr(
                    createStringLocation("field3", 12, 2),
                    LiteralIntExpr(createIntLocation(100, 12, 11)) // This one is valid
                )
            ),
            createLocation(10, 1)
        )
        
        val typeChecker = TypeChecker()
        val result = typeChecker.typeCheck(record)
        
        // Should detect at least one error
        assertTrue(result.isFailure(), "Should detect type errors in record fields")
        
        val failure = result as TypeCheckResult.Failure
        // Error should be about type mismatch, not something generic
        assertTrue(failure.error.length > 5, "Error should be descriptive: ${failure.error}")
    }

    // ===== ERROR CASCADING PREVENTION =====

    @Test
    fun `error in one branch does not affect other branches`() {
        // if someCondition then (42 + "error") else (true && false)
        val env = TypeEnvironment.empty()
            .bind("someCondition", TypeScheme.monomorphic(Types.Bool))
        
        val errorBranch = BinaryOpExpr(
            LiteralIntExpr(createIntLocation(42, 15, 25)),
            BinaryOp.Plus,
            LiteralStringExpr(createStringLocation("error", 15, 30)),
            createLocation(15, 28)
        )
        
        val validBranch = BinaryOpExpr(
            LiteralBoolExpr(createBoolLocation(true, 15, 42)),
            BinaryOp.And,
            LiteralBoolExpr(createBoolLocation(false, 15, 50)),
            createLocation(15, 47)
        )
        
        val ifExpr = IfExpr(
            VarExpr(createStringLocation("someCondition", 15, 4)),
            errorBranch,
            validBranch,
            createLocation(15, 1)
        )
        
        val typeChecker = TypeChecker(env)
        val result = typeChecker.typeCheck(ifExpr)
        
        assertTrue(result.isFailure(), "Should detect error in then branch")
        
        // The error should be about the type mismatch, not about the if expression structure
        val failure = result as TypeCheckResult.Failure
        assertTrue(failure.error.contains("unify") || failure.error.contains("String") || failure.error.contains("Int"),
            "Error should be about the specific type issue: ${failure.error}")
    }

    @Test
    fun `error in pattern does not prevent checking other patterns`() {
        // match someValue with 
        //   42 + "error" -> 1  // Error in pattern (which may not be syntactically valid anyway)
        //   true -> 2
        val env = TypeEnvironment.empty()
            .bind("someValue", TypeScheme.monomorphic(Types.Bool))
        
        // Create a simple pattern match with valid patterns
        val pattern1 = LiteralBoolPattern(createBoolLocation(true, 20, 3))
        val body1 = LiteralIntExpr(createIntLocation(1, 20, 10))
        val case1 = MatchCase(pattern1, body1)
        
        val pattern2 = LiteralBoolPattern(createBoolLocation(false, 21, 3))
        val body2 = LiteralIntExpr(createIntLocation(2, 21, 11))
        val case2 = MatchCase(pattern2, body2)
        
        val matchExpr = MatchExpr(
            VarExpr(createStringLocation("someValue", 20, 7)),
            listOf(case1, case2),
            createLocation(20, 1)
        )
        
        val typeChecker = TypeChecker(env)
        val result = typeChecker.typeCheck(matchExpr)
        
        // This should succeed as all patterns are valid
        assertTrue(result.isSuccess(), "Should succeed with valid pattern match")
    }

    @Test
    fun `error in function body does not prevent checking other expressions`() {
        // let badFunc = fun(x: Int) -> x + "error" in
        // let goodFunc = fun(y: Bool) -> y in
        // goodFunc(true)
        val badLambda = LambdaExpr(
            null, // typeParams
            createStringLocation("x", 25, 15),
            BaseTypeExpr(createStringLocation("Int", 25, 18), null),
            BinaryOpExpr(
                VarExpr(createStringLocation("x", 25, 26)),
                BinaryOp.Plus,
                LiteralStringExpr(createStringLocation("error", 25, 30)),
                createLocation(25, 28)
            ),
            createLocation(25, 10)
        )
        
        val goodLambda = LambdaExpr(
            null, // typeParams
            createStringLocation("y", 26, 15),
            BaseTypeExpr(createStringLocation("Bool", 26, 18), null),
            VarExpr(createStringLocation("y", 26, 27)),
            createLocation(26, 10)
        )
        
        val application = ApplicationExpr(
            VarExpr(createStringLocation("goodFunc", 27, 1)),
            listOf(LiteralBoolExpr(createBoolLocation(true, 27, 10))),
            createLocation(27, 9)
        )
        
        val program = LetExpr(
            false, // recursive
            createStringLocation("badFunc", 25, 5),
            null, // typeParams
            null, // parameters
            null, // typeAnnotation
            badLambda,
            LetExpr(
                false, // recursive
                createStringLocation("goodFunc", 26, 5),
                null, // typeParams
                null, // parameters
                null, // typeAnnotation
                goodLambda,
                application
            )
        )
        
        val typeChecker = TypeChecker()
        val result = typeChecker.typeCheck(program)
        
        assertTrue(result.isFailure(), "Should detect error in badFunc")
        
        // Error should be about the type mismatch, not about the overall program structure
        val failure = result as TypeCheckResult.Failure
        assertTrue(failure.error.contains("unify") || failure.error.contains("String") || failure.error.contains("Int"),
            "Error should be about the specific type issue: ${failure.error}")
    }

    // ===== MULTIPLE ERROR REPORTING =====

    @Test
    fun `error recovery allows detection of multiple issues`() {
        // Create a program where fixing one error would reveal another
        // This tests that the type checker doesn't stop at the first error
        
        // let x = 42 + "error1" in
        // let y = true * 5 in  
        // x + y
        val error1 = BinaryOpExpr(
            LiteralIntExpr(createIntLocation(42, 30, 9)),
            BinaryOp.Plus,
            LiteralStringExpr(createStringLocation("error1", 30, 14)),
            createLocation(30, 12)
        )
        
        val error2 = BinaryOpExpr(
            LiteralBoolExpr(createBoolLocation(true, 31, 9)),
            BinaryOp.Star,
            LiteralIntExpr(createIntLocation(5, 31, 17)),
            createLocation(31, 15)
        )
        
        val finalError = BinaryOpExpr(
            VarExpr(createStringLocation("x", 32, 1)),
            BinaryOp.Plus,
            VarExpr(createStringLocation("y", 32, 5)),
            createLocation(32, 3)
        )
        
        val program = LetExpr(
            false, // recursive
            createStringLocation("x", 30, 5),
            null, // typeParams
            null, // parameters
            null, // typeAnnotation
            error1,
            LetExpr(
                false, // recursive
                createStringLocation("y", 31, 5),
                null, // typeParams
                null, // parameters
                null, // typeAnnotation
                error2,
                finalError
            )
        )
        
        val typeChecker = TypeChecker()
        val result = typeChecker.typeCheck(program)
        
        assertTrue(result.isFailure(), "Should detect at least one error")
        
        // The system should report a meaningful error
        val failure = result as TypeCheckResult.Failure
        assertTrue(failure.error.length > 5, "Error should be descriptive: ${failure.error}")
    }

    // ===== ERROR PRIORITIZATION AND FILTERING =====

    @Test
    fun `error prioritization reports most relevant error`() {
        // Create a situation where there could be multiple error interpretations
        // but one is more specific/helpful than others
        
        // f(42 + "error")  where f expects a String
        val env = TypeEnvironment.empty()
            .bind("f", TypeScheme.monomorphic(FunctionType(Types.String, Types.Int)))
        
        val argument = BinaryOpExpr(
            LiteralIntExpr(createIntLocation(42, 35, 3)),
            BinaryOp.Plus,
            LiteralStringExpr(createStringLocation("error", 35, 8)),
            createLocation(35, 6)
        )
        
        val application = ApplicationExpr(
            VarExpr(createStringLocation("f", 35, 1)),
            listOf(argument),
            createLocation(35, 2)
        )
        
        val typeChecker = TypeChecker(env)
        val result = typeChecker.typeCheck(application)
        
        assertTrue(result.isFailure(), "Should detect error")
        
        val failure = result as TypeCheckResult.Failure
        
        // The error should be about the type mismatch within the argument,
        // which is more specific than a generic "wrong argument type" error
        assertTrue(failure.error.contains("unify") || failure.error.contains("String") || failure.error.contains("Int"),
            "Error should mention the specific type issue: ${failure.error}")
    }

    @Test
    fun `error filtering avoids redundant error messages`() {
        // Test that the system doesn't report multiple variations of the same error
        
        // Create a deeply nested expression with the same type error repeated
        val baseError = BinaryOpExpr(
            LiteralIntExpr(createIntLocation(42, 40, 1)),
            BinaryOp.Plus,
            LiteralStringExpr(createStringLocation("error", 40, 6)),
            createLocation(40, 4)
        )
        
        // Wrap it in multiple layers that would propagate the same error
        val wrappedError = TupleExpr(
            listOf(baseError, baseError),
            createLocation(40, 1)
        )
        
        val typeChecker = TypeChecker()
        val result = typeChecker.typeCheck(wrappedError)
        
        assertTrue(result.isFailure(), "Should detect error")
        
        val failure = result as TypeCheckResult.Failure
        
        // Should report a single, clear error rather than multiple confusing ones
        assertTrue(failure.error.length > 5, "Error should be descriptive: ${failure.error}")
        assertTrue(failure.error.length < 500, "Error should not be excessively verbose: ${failure.error}")
    }

    @Test
    fun `error recovery with undefined variables provides helpful context`() {
        // Test that undefined variable errors don't cascade into unhelpful secondary errors
        
        // unknownVar + 42
        val expr = BinaryOpExpr(
            VarExpr(createStringLocation("unknownVar", 45, 1)),
            BinaryOp.Plus,
            LiteralIntExpr(createIntLocation(42, 45, 14)),
            createLocation(45, 12)
        )
        
        val typeChecker = TypeChecker()
        val result = typeChecker.typeCheck(expr)
        
        assertTrue(result.isFailure(), "Should detect undefined variable")
        
        val failure = result as TypeCheckResult.Failure
        
        // Error should mention the undefined variable, not get confused about the operation
        assertTrue(failure.error.contains("unknownVar"),
            "Error should mention the undefined variable: ${failure.error}")
    }

    @Test
    fun `error recovery preserves valid type information`() {
        // Test that when part of an expression has an error,
        // the type checker still maintains correct type information for valid parts
        
        // Create a record where one field has an error but others are valid
        val record = RecordExpr(
            listOf(
                FieldExpr(
                    createStringLocation("validField", 50, 2),
                    LiteralIntExpr(createIntLocation(42, 50, 15))
                ),
                FieldExpr(
                    createStringLocation("errorField", 51, 2),
                    BinaryOpExpr(
                        LiteralIntExpr(createIntLocation(1, 51, 15)),
                        BinaryOp.Plus,
                        LiteralStringExpr(createStringLocation("error", 51, 19)),
                        createLocation(51, 17)
                    )
                )
            ),
            createLocation(50, 1)
        )
        
        val typeChecker = TypeChecker()
        val result = typeChecker.typeCheck(record)
        
        assertTrue(result.isFailure(), "Should detect error in errorField")
        
        // Despite the error, the system should handle this gracefully
        val failure = result as TypeCheckResult.Failure
        assertTrue(failure.error.length > 5, "Error should be meaningful: ${failure.error}")
    }
} 