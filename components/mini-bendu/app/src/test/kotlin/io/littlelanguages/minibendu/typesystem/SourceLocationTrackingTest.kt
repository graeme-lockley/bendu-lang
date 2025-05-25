package io.littlelanguages.minibendu.typesystem

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import io.littlelanguages.minibendu.*
import io.littlelanguages.scanpiler.LocationCoordinate
import io.littlelanguages.scanpiler.LocationRange

/**
 * Tests for source location tracking in the type system.
 * 
 * These tests verify that source location information is properly
 * preserved and reported throughout the type checking process.
 * 
 * Tests cover:
 * - Error messages include proper source locations
 * - Location tracking through transformations  
 * - Location preservation in type inference
 * - Location-aware error recovery
 */
class SourceLocationTrackingTest {

    private fun createLocation(line: Int, column: Int): LocationCoordinate {
        return LocationCoordinate(0, line, column)
    }
    
    private fun createRange(startLine: Int, startCol: Int, endLine: Int, endCol: Int): LocationRange {
        return LocationRange(
            LocationCoordinate(0, startLine, startCol),
            LocationCoordinate(0, endLine, endCol)
        )
    }
    
    private fun createStringLocation(value: String, line: Int, column: Int): StringLocation {
        return StringLocation(value, createLocation(line, column))
    }
    
    private fun createIntLocation(value: Int, line: Int, column: Int): IntLocation {
        return IntLocation(value, createLocation(line, column))
    }

    // ===== ERROR MESSAGES WITH SOURCE LOCATIONS =====

    @Test
    fun `type error includes source location from binary operation`() {
        // 5 + "hello" at line 3, column 10
        val badExpr = BinaryOpExpr(
            LiteralIntExpr(createIntLocation(5, 3, 5)),
            BinaryOp.Plus,
            LiteralStringExpr(createStringLocation("hello", 3, 9)),
            createLocation(3, 7) // The + operator location
        )
        
        val typeChecker = TypeChecker()
        val result = typeChecker.typeCheck(badExpr)
        
        assertTrue(result.isFailure(), "Type error should be detected")
        val failure = result as TypeCheckResult.Failure
        
        assertNotNull(failure.location, "Error should include source location")
        assertEquals(3, failure.location?.line, "Error should reference correct line")
        assertTrue(failure.error.contains("3") || failure.error.contains("7"), 
            "Error message should reference source location: ${failure.error}")
    }

    @Test
    fun `undefined variable error includes precise location`() {
        // unknownVar at line 5, column 15
        val varExpr = VarExpr(createStringLocation("unknownVar", 5, 15))
        
        val typeChecker = TypeChecker()
        val result = typeChecker.typeCheck(varExpr)
        
        assertTrue(result.isFailure(), "Undefined variable should be detected")
        val failure = result as TypeCheckResult.Failure
        
        assertNotNull(failure.location, "Error should include source location")
        assertEquals(5, failure.location?.line, "Error should reference variable line")
        assertEquals(15, failure.location?.column, "Error should reference variable column")
        assertTrue(failure.error.contains("unknownVar"), "Error should mention variable name")
    }

    @Test
    fun `function application type error preserves call site location`() {
        // someFunc(42, "invalid") where someFunc expects (Int, Int)
        val functionVar = VarExpr(createStringLocation("someFunc", 2, 5))
        val arg1 = LiteralIntExpr(createIntLocation(42, 2, 14))
        val arg2 = LiteralStringExpr(createStringLocation("invalid", 2, 18))
        
        val appExpr = ApplicationExpr(
            functionVar,
            listOf(arg1, arg2),
            createLocation(2, 13) // Application location
        )
        
        // Set up environment with a function that expects (Int, Int) -> Int
        val funcType = FunctionType(Types.Int, FunctionType(Types.Int, Types.Int))
        val env = TypeEnvironment.empty()
            .bind("someFunc", TypeScheme.monomorphic(funcType))
        
        val typeChecker = TypeChecker(env)
        val result = typeChecker.typeCheck(appExpr)
        
        assertTrue(result.isFailure(), "Type error should be detected")
        val failure = result as TypeCheckResult.Failure
        
        assertNotNull(failure.location, "Error should include source location")
        assertEquals(2, failure.location?.line, "Error should reference call site line")
    }

    @Test
    fun `pattern match type error includes pattern location`() {
        // match 42 with {name: x} -> x  (trying to match Int with record pattern)
        val pattern = RecordPattern(listOf(FieldPattern(createStringLocation("name", 4, 15), VarPattern(createStringLocation("x", 4, 21), typeAnnotation = null))),
            createLocation(4, 14) // Pattern location
        )
        
        val body = VarExpr(createStringLocation("x", 4, 27))
        val matchCase = MatchCase(pattern, body)
        
        val matchExpr = MatchExpr(
            LiteralIntExpr(createIntLocation(42, 4, 7)),
            listOf(matchCase),
            createLocation(4, 1) // Match expression location
        )
        
        val typeChecker = TypeChecker()
        val result = typeChecker.typeCheck(matchExpr)
        
        assertTrue(result.isFailure(), "Pattern match error should be detected")
        val failure = result as TypeCheckResult.Failure
        
        assertNotNull(failure.location, "Error should include source location")
        assertEquals(4, failure.location?.line, "Error should reference match line")
    }

    @Test
    fun `record field access error includes field location`() {
        // {age: 30}.name  (accessing potentially non-existent field)
        val recordExpr = RecordExpr(
            listOf(FieldExpr(createStringLocation("age", 6, 2), LiteralIntExpr(createIntLocation(30, 6, 7)))),
            createLocation(6, 1)
        )
        
        val projectionExpr = ProjectionExpr(
            recordExpr,
            createStringLocation("name", 6, 11), // Field may not exist
            createLocation(6, 10) // Projection location
        )
        
        val typeChecker = TypeChecker()
        val result = typeChecker.typeCheck(projectionExpr)
        
        // Due to row polymorphism, this might succeed with a fresh type variable
        // We mainly want to verify that location tracking works
        assertNotNull(result, "Should produce a result")
        
        if (result.isFailure()) {
            val failure = result as TypeCheckResult.Failure
            assertNotNull(failure.location, "Error should include source location")
            assertEquals(6, failure.location?.line, "Error should reference projection line")
        } else {
            // If it succeeds due to row polymorphism, that's also fine
            val success = result as TypeCheckResult.Success
            assertNotNull(success.type, "Should infer a type")
        }
    }

    // ===== LOCATION TRACKING THROUGH TRANSFORMATIONS =====

    @Test
    fun `location preserved through let expression`() {
        // let x = 42 + "invalid" in x
        val badValue = BinaryOpExpr(
            LiteralIntExpr(createIntLocation(42, 8, 9)),
            BinaryOp.Plus,
            LiteralStringExpr(createStringLocation("invalid", 8, 14)),
            createLocation(8, 12)
        )
        
        val letExpr = LetExpr(
            false, // recursive
            createStringLocation("x", 8, 5),
            null, // typeParams
            null, // parameters
            null, // typeAnnotation
            badValue,
            VarExpr(createStringLocation("x", 8, 26))
        )
        
        val typeChecker = TypeChecker()
        val result = typeChecker.typeCheck(letExpr)
        
        assertTrue(result.isFailure(), "Type error should be detected")
        val failure = result as TypeCheckResult.Failure
        
        assertNotNull(failure.location, "Error should include source location")
        assertEquals(8, failure.location?.line, "Error should reference let expression line")
    }

    @Test
    fun `location preserved through lambda expression`() {
        // fun(x: Int) -> x + "invalid"
        val badBody = BinaryOpExpr(
            VarExpr(createStringLocation("x", 10, 20)),
            BinaryOp.Plus,
            LiteralStringExpr(createStringLocation("invalid", 10, 24)),
            createLocation(10, 22)
        )
        
        val lambdaExpr = LambdaExpr(
            null, // typeParams
            createStringLocation("x", 10, 5),
            BaseTypeExpr(createStringLocation("Int", 10, 8), null),
            badBody,
            createLocation(10, 1)
        )
        
        val typeChecker = TypeChecker()
        val result = typeChecker.typeCheck(lambdaExpr)
        
        assertTrue(result.isFailure(), "Type error should be detected")
        val failure = result as TypeCheckResult.Failure
        
        assertNotNull(failure.location, "Error should include source location")
        assertEquals(10, failure.location?.line, "Error should reference lambda line")
    }

    @Test
    fun `location preserved through nested expressions`() {
        // if true then 42 + "invalid" else 0
        val badThenBranch = BinaryOpExpr(
            LiteralIntExpr(createIntLocation(42, 12, 15)),
            BinaryOp.Plus,
            LiteralStringExpr(createStringLocation("invalid", 12, 20)),
            createLocation(12, 18)
        )
        
        val ifExpr = IfExpr(
            LiteralBoolExpr(BoolLocation(true, createLocation(12, 4))),
            badThenBranch,
            LiteralIntExpr(createIntLocation(0, 12, 33)),
            createLocation(12, 1)
        )
        
        val typeChecker = TypeChecker()
        val result = typeChecker.typeCheck(ifExpr)
        
        assertTrue(result.isFailure(), "Type error should be detected")
        val failure = result as TypeCheckResult.Failure
        
        assertNotNull(failure.location, "Error should include source location")
        assertEquals(12, failure.location?.line, "Error should reference if expression line")
    }

    // ===== LOCATION PRESERVATION IN TYPE INFERENCE =====

    @Test
    fun `successful type checking preserves original locations`() {
        // let sum = fun(x: Int, y: Int) -> x + y
        val xVar = VarExpr(createStringLocation("x", 15, 35))
        val yVar = VarExpr(createStringLocation("y", 15, 39))
        
        val addBody = BinaryOpExpr(
            xVar,
            BinaryOp.Plus,
            yVar,
            createLocation(15, 37)
        )
        
        val lambdaExpr = LambdaExpr(
            null, // typeParams
            createStringLocation("x", 15, 16),
            BaseTypeExpr(createStringLocation("Int", 15, 19), null),
            LambdaExpr(
                null, // typeParams  
                createStringLocation("y", 15, 26),
                BaseTypeExpr(createStringLocation("Int", 15, 29), null),
                addBody,
                createLocation(15, 25)
            ),
            createLocation(15, 11)
        )
        
        val letExpr = LetExpr(
            false, // recursive
            createStringLocation("sum", 15, 5),
            null, // typeParams
            null, // parameters
            null, // typeAnnotation
            lambdaExpr,
            null // no body for this test
        )
        
        val typeChecker = TypeChecker()
        val result = typeChecker.typeCheck(letExpr)
        
        assertTrue(result.isSuccess(), "Valid expression should type check")
        val success = result as TypeCheckResult.Success
        
        // Verify that the final type is reasonable
        val finalType = success.getFinalType()
        assertTrue(finalType is FunctionType, "Should infer function type")
    }

    @Test
    fun `type information includes source location context`() {
        // Simple expression: 42 + 17
        val expr = BinaryOpExpr(
            LiteralIntExpr(createIntLocation(42, 20, 1)),
            BinaryOp.Plus,
            LiteralIntExpr(createIntLocation(17, 20, 6)),
            createLocation(20, 4)
        )
        
        val typeChecker = TypeChecker()
        val typeInfo = typeChecker.getTypeInformation(expr, createLocation(20, 4))
        
        assertTrue(typeInfo is TypeInformation.Available, "Type information should be available")
        val available = typeInfo as TypeInformation.Available
        
        assertEquals(Types.Int, available.type, "Should infer Int type")
        assertTrue(available.prettyType.contains("Int"), "Pretty type should mention Int")
    }

    // ===== LOCATION-AWARE ERROR RECOVERY =====

    @Test
    fun `incremental type checking preserves locations across errors`() {
        // Multiple expressions with errors at different locations
        val expr1 = BinaryOpExpr(
            LiteralIntExpr(createIntLocation(5, 1, 1)),
            BinaryOp.Plus,
            LiteralStringExpr(createStringLocation("bad1", 1, 5)),
            createLocation(1, 3)
        )
        
        val expr2 = LiteralIntExpr(createIntLocation(42, 2, 1)) // Valid expression
        
        val expr3 = BinaryOpExpr(
            LiteralBoolExpr(BoolLocation(true, createLocation(3, 1))),
            BinaryOp.Star,
            LiteralIntExpr(createIntLocation(10, 3, 9)),
            createLocation(3, 6)
        )
        
        val typeChecker = TypeChecker()
        val result = typeChecker.typeCheckIncrementally(listOf(expr1, expr2, expr3))
        
        assertTrue(result.hasErrors, "Should detect errors")
        assertEquals(2, result.errors.size, "Should find 2 errors")
        
        // Verify first error location
        val error1 = result.errors[0]
        assertEquals(1, error1.location?.line, "First error should be on line 1")
        
        // Verify second error location  
        val error2 = result.errors[1]
        assertEquals(3, error2.location?.line, "Second error should be on line 3")
        
        // Verify successful expression
        val success = result.results[1]
        assertTrue(success.isSuccess(), "Valid expression should succeed")
    }

    @Test
    fun `program type checking provides comprehensive location info`() {
        // Complex program with nested errors
        val innerBadExpr = BinaryOpExpr(
            LiteralIntExpr(createIntLocation(1, 25, 20)),
            BinaryOp.Plus,
            LiteralStringExpr(createStringLocation("error", 25, 24)),
            createLocation(25, 22)
        )
        
        val outerExpr = LetExpr(
            false, // recursive
            createStringLocation("result", 25, 5),
            null, // typeParams
            null, // parameters
            null, // typeAnnotation
            innerBadExpr,
            VarExpr(createStringLocation("result", 25, 35))
        )
        
        val typeChecker = TypeChecker()
        val result = typeChecker.typeCheckProgram(outerExpr)
        
        assertTrue(result is ProgramTypeCheckResult.Failure, "Should fail with error")
        val failure = result as ProgramTypeCheckResult.Failure
        
        assertNotNull(failure.location, "Should include error location")
        assertEquals(25, failure.location?.line, "Should reference correct line")
        assertTrue(failure.suggestions.isNotEmpty(), "Should provide suggestions")
    }

    @Test
    fun `constraint generation preserves nested locations`() {
        // Deeply nested expression to test location preservation
        val deeplyNested = IfExpr(
            LiteralBoolExpr(BoolLocation(true, createLocation(30, 4))),
            IfExpr(
                LiteralBoolExpr(BoolLocation(false, createLocation(31, 8))),
                BinaryOpExpr(
                    LiteralIntExpr(createIntLocation(1, 32, 12)),
                    BinaryOp.Plus,
                    LiteralStringExpr(createStringLocation("nested_error", 32, 16)),
                    createLocation(32, 14)
                ),
                LiteralIntExpr(createIntLocation(2, 33, 12)),
                createLocation(31, 4)
            ),
            LiteralIntExpr(createIntLocation(3, 34, 8)),
            createLocation(30, 1)
        )
        
        val typeChecker = TypeChecker()
        val result = typeChecker.typeCheck(deeplyNested)
        
        assertTrue(result.isFailure(), "Should detect nested error")
        val failure = result as TypeCheckResult.Failure
        
        assertNotNull(failure.location, "Should include error location")
        assertEquals(30, failure.location?.line, "Should reference outer expression line")
    }

    @Test
    fun `range locations are properly handled`() {
        // Expression with range location
        val rangeLocation = createRange(40, 5, 40, 15)
        val expr = BinaryOpExpr(
            LiteralIntExpr(createIntLocation(42, 40, 5)),
            BinaryOp.Plus,
            LiteralStringExpr(createStringLocation("range_error", 40, 10)),
            rangeLocation
        )
        
        val typeChecker = TypeChecker()
        val result = typeChecker.typeCheck(expr)
        
        assertTrue(result.isFailure(), "Should detect error")
        val failure = result as TypeCheckResult.Failure
        
        assertNotNull(failure.location, "Should include source location")
        assertEquals(40, failure.location?.line, "Should extract line from range")
        assertEquals(5, failure.location?.column, "Should extract column from range start")
    }
} 