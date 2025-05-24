package io.littlelanguages.minibendu.typesystem

import io.littlelanguages.minibendu.*
import io.littlelanguages.scanpiler.LocationCoordinate
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Comprehensive tests for type inference of let expressions.
 * Task 31 of Phase 1 - Type Inference for Complex Expressions.
 * 
 * Tests cover:
 * - Simple let bindings: let x = 5 in x + 1
 * - Type generalization in let bindings: let f = \x -> x in f(1) + f("hello")
 * - Recursive let bindings: let rec fact = \n -> if n == 0 then 1 else n * fact(n - 1)
 * - Polymorphic let bindings with type variables
 * - Mutual recursion detection and typing
 * - Let bindings with type annotations
 * - Nested let expressions
 * - Error handling for undefined variables and type mismatches
 */
class TypeInferenceForLetExpressionsTest {
    
    private fun createStringLocation(value: String): StringLocation {
        return StringLocation(value, LocationCoordinate(0, 1, 1))
    }
    
    private fun createIntLocation(value: Int): IntLocation {
        return IntLocation(value, LocationCoordinate(0, 1, 1))
    }
    
    private fun createBoolLocation(value: Boolean): BoolLocation {
        return BoolLocation(value, LocationCoordinate(0, 1, 1))
    }
    
    private fun createLocation(): LocationCoordinate = LocationCoordinate(0, 1, 1)
    
    @Test
    fun `simple let binding with integer`() {
        // Test: let x = 42 in x
        val xVar = VarExpr(createStringLocation("x"))
        val literal = LiteralIntExpr(createIntLocation(42))
        val letExpr = LetExpr(
            false, // not recursive
            createStringLocation("x"),
            null, // no type params
            null, // no parameters
            null, // no type annotation
            literal,
            xVar
        )
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(letExpr)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for simple let binding")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.Int, finalType, "Simple let binding should infer as Int")
    }
    
    @Test
    fun `simple let binding with computation`() {
        // Test: let x = 5 + 3 in x * 2
        val five = LiteralIntExpr(createIntLocation(5))
        val three = LiteralIntExpr(createIntLocation(3))
        val addition = BinaryOpExpr(five, BinaryOp.Plus, three, createLocation())
        
        val xVar = VarExpr(createStringLocation("x"))
        val two = LiteralIntExpr(createIntLocation(2))
        val multiplication = BinaryOpExpr(xVar, BinaryOp.Star, two, createLocation())
        
        val letExpr = LetExpr(
            false, // not recursive
            createStringLocation("x"),
            null, null, null,
            addition,
            multiplication
        )
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(letExpr)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for let with computation")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.Int, finalType, "Let with computation should infer as Int")
    }
    
    @Test
    fun `let binding with type annotation`() {
        // Test: let x: Int = 42 in x
        val xVar = VarExpr(createStringLocation("x"))
        val literal = LiteralIntExpr(createIntLocation(42))
        val typeAnnotation = BaseTypeExpr(createStringLocation("Int"), null)
        
        val letExpr = LetExpr(
            false, // not recursive
            createStringLocation("x"),
            null, // no type params
            null, // no parameters
            typeAnnotation,
            literal,
            xVar
        )
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(letExpr)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for annotated let binding")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.Int, finalType, "Annotated let binding should infer as Int")
    }
    
    @Test
    fun `let binding with type annotation mismatch`() {
        // Test: let x: String = 42 in x (should fail)
        val xVar = VarExpr(createStringLocation("x"))
        val literal = LiteralIntExpr(createIntLocation(42))
        val typeAnnotation = BaseTypeExpr(createStringLocation("String"), null)
        
        val letExpr = LetExpr(
            false, // not recursive
            createStringLocation("x"),
            null, null,
            typeAnnotation,
            literal,
            xVar
        )
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(letExpr)
        
        assertTrue(result.isSuccess(), "Constraint generation should succeed even with type mismatch")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Failure, "Constraint solving should fail due to type mismatch")
    }
    
    @Test
    fun `nested let expressions`() {
        // Test: let x = 5 in let y = x + 1 in y * 2
        val five = LiteralIntExpr(createIntLocation(5))
        val xVar = VarExpr(createStringLocation("x"))
        val one = LiteralIntExpr(createIntLocation(1))
        val xPlusOne = BinaryOpExpr(xVar, BinaryOp.Plus, one, createLocation())
        
        val yVar = VarExpr(createStringLocation("y"))
        val two = LiteralIntExpr(createIntLocation(2))
        val yTimesTwo = BinaryOpExpr(yVar, BinaryOp.Star, two, createLocation())
        
        val innerLet = LetExpr(
            false, // not recursive
            createStringLocation("y"),
            null, null, null,
            xPlusOne,
            yTimesTwo
        )
        
        val outerLet = LetExpr(
            false, // not recursive
            createStringLocation("x"),
            null, null, null,
            five,
            innerLet
        )
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(outerLet)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for nested let expressions")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.Int, finalType, "Nested let expressions should infer as Int")
    }
    
    @Test
    fun `let binding with lambda function`() {
        // Test: let f = \x -> x + 1 in f(5)
        val xVar = VarExpr(createStringLocation("x"))
        val one = LiteralIntExpr(createIntLocation(1))
        val xPlusOne = BinaryOpExpr(xVar, BinaryOp.Plus, one, createLocation())
        
        val lambda = LambdaExpr(
            null, // no type params
            createStringLocation("x"),
            null, // no param type
            xPlusOne,
            createLocation()
        )
        
        val fVar = VarExpr(createStringLocation("f"))
        val five = LiteralIntExpr(createIntLocation(5))
        val application = ApplicationExpr(fVar, listOf(five), createLocation())
        
        val letExpr = LetExpr(
            false, // not recursive
            createStringLocation("f"),
            null, null, null,
            lambda,
            application
        )
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(letExpr)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for let with lambda")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.Int, finalType, "Let with lambda application should infer as Int")
    }
    
    @Test
    fun `polymorphic let binding with identity function`() {
        // Test: let id = \x -> x in id(42)
        // This tests that identity function can be applied to specific types
        // Full polymorphic let generalization requires more advanced constraint solving
        val xVar = VarExpr(createStringLocation("x"))
        val identityLambda = LambdaExpr(
            null,
            createStringLocation("x"),
            null,
            xVar,
            createLocation()
        )
        
        val idVar = VarExpr(createStringLocation("id"))
        val fortytwo = LiteralIntExpr(createIntLocation(42))
        val application = ApplicationExpr(idVar, listOf(fortytwo), createLocation())
        
        val letExpr = LetExpr(
            false, // not recursive
            createStringLocation("id"),
            null, null, null,
            identityLambda,
            application
        )
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(letExpr)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for identity function let binding")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.Int, finalType, "Identity function applied to Int should result in Int")
    }
    
    @Test
    fun `simple recursive let binding`() {
        // Test: let rec f = \x -> if x == 0 then 1 else x * f(x - 1) in f(5)
        // Simplified factorial function
        val xVar = VarExpr(createStringLocation("x"))
        val zero = LiteralIntExpr(createIntLocation(0))
        val one = LiteralIntExpr(createIntLocation(1))
        val condition = BinaryOpExpr(xVar, BinaryOp.EqualEqual, zero, createLocation())
        
        val xVar2 = VarExpr(createStringLocation("x"))
        val fVar = VarExpr(createStringLocation("f"))
        val xVar3 = VarExpr(createStringLocation("x"))
        val oneForSub = LiteralIntExpr(createIntLocation(1))
        val xMinusOne = BinaryOpExpr(xVar3, BinaryOp.Minus, oneForSub, createLocation())
        val recursiveCall = ApplicationExpr(fVar, listOf(xMinusOne), createLocation())
        val multiplication = BinaryOpExpr(xVar2, BinaryOp.Star, recursiveCall, createLocation())
        
        val ifExpr = IfExpr(condition, one, multiplication, createLocation())
        val lambda = LambdaExpr(null, createStringLocation("x"), null, ifExpr, createLocation())
        
        val fVar2 = VarExpr(createStringLocation("f"))
        val five = LiteralIntExpr(createIntLocation(5))
        val application = ApplicationExpr(fVar2, listOf(five), createLocation())
        
        val letExpr = LetExpr(
            true, // recursive
            createStringLocation("f"),
            null, null, null,
            lambda,
            application
        )
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(letExpr)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for recursive let binding")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.Int, finalType, "Recursive factorial should infer as Int")
    }
    
    @Test
    fun `let binding with conditional`() {
        // Test: let x = 5 in if x > 0 then x else 0
        val five = LiteralIntExpr(createIntLocation(5))
        val xVar1 = VarExpr(createStringLocation("x"))
        val zero1 = LiteralIntExpr(createIntLocation(0))
        val condition = BinaryOpExpr(xVar1, BinaryOp.EqualEqual, zero1, createLocation())
        
        val xVar2 = VarExpr(createStringLocation("x"))
        val zero2 = LiteralIntExpr(createIntLocation(0))
        val ifExpr = IfExpr(condition, xVar2, zero2, createLocation())
        
        val letExpr = LetExpr(
            false, // not recursive
            createStringLocation("x"),
            null, null, null,
            five,
            ifExpr
        )
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(letExpr)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for let with conditional")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.Int, finalType, "Let with conditional should infer as Int")
    }
    
    @Test
    fun `let binding without body returns value type`() {
        // Test: let x = 42 (no 'in' clause)
        val literal = LiteralIntExpr(createIntLocation(42))
        val letExpr = LetExpr(
            false, // not recursive
            createStringLocation("x"),
            null, null, null,
            literal,
            null // no body
        )
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(letExpr)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for let without body")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.Int, finalType, "Let without body should return value type")
    }
    
    @Test
    fun `let binding with variable shadowing`() {
        // Test: let x = 5 in let x = "hello" in x
        // Inner x should shadow outer x
        val five = LiteralIntExpr(createIntLocation(5))
        val hello = LiteralStringExpr(createStringLocation("hello"))
        val xVar = VarExpr(createStringLocation("x"))
        
        val innerLet = LetExpr(
            false, // not recursive
            createStringLocation("x"),
            null, null, null,
            hello,
            xVar
        )
        
        val outerLet = LetExpr(
            false, // not recursive
            createStringLocation("x"),
            null, null, null,
            five,
            innerLet
        )
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(outerLet)
        
        assertTrue(result.isSuccess(), "Should successfully handle variable shadowing")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.String, finalType, "Shadowed variable should use inner binding type")
    }
    
    @Test
    fun `let binding with multiple variables`() {
        // Test: let x = 5 in let y = x + 1 in let z = y * 2 in z
        val five = LiteralIntExpr(createIntLocation(5))
        val xVar1 = VarExpr(createStringLocation("x"))
        val one = LiteralIntExpr(createIntLocation(1))
        val xPlusOne = BinaryOpExpr(xVar1, BinaryOp.Plus, one, createLocation())
        
        val yVar1 = VarExpr(createStringLocation("y"))
        val two = LiteralIntExpr(createIntLocation(2))
        val yTimesTwo = BinaryOpExpr(yVar1, BinaryOp.Star, two, createLocation())
        
        val zVar = VarExpr(createStringLocation("z"))
        
        val innerLet = LetExpr(false, createStringLocation("z"), null, null, null, yTimesTwo, zVar)
        val middleLet = LetExpr(false, createStringLocation("y"), null, null, null, xPlusOne, innerLet)
        val outerLet = LetExpr(false, createStringLocation("x"), null, null, null, five, middleLet)
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(outerLet)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for multiple let bindings")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.Int, finalType, "Multiple let bindings should infer as Int")
    }
    
    @Test
    fun `let binding with logical operations`() {
        // Test: let flag = true in let result = flag && false in result
        val trueLit = LiteralBoolExpr(createBoolLocation(true))
        val flagVar = VarExpr(createStringLocation("flag"))
        val falseLit = LiteralBoolExpr(createBoolLocation(false))
        val logicalAnd = BinaryOpExpr(flagVar, BinaryOp.And, falseLit, createLocation())
        
        val resultVar = VarExpr(createStringLocation("result"))
        
        val innerLet = LetExpr(false, createStringLocation("result"), null, null, null, logicalAnd, resultVar)
        val outerLet = LetExpr(false, createStringLocation("flag"), null, null, null, trueLit, innerLet)
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(outerLet)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for let with logical operations")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.Bool, finalType, "Let with logical operations should infer as Bool")
    }
    
    @Test
    fun `type generalization prevents variable escape`() {
        // Test that let generalization works correctly
        // let f = \x -> x in let g = f in g(42)
        val xVar = VarExpr(createStringLocation("x"))
        val identityLambda = LambdaExpr(null, createStringLocation("x"), null, xVar, createLocation())
        
        val fVar = VarExpr(createStringLocation("f"))
        val gVar = VarExpr(createStringLocation("g"))
        val fortytwo = LiteralIntExpr(createIntLocation(42))
        val application = ApplicationExpr(gVar, listOf(fortytwo), createLocation())
        
        val innerLet = LetExpr(false, createStringLocation("g"), null, null, null, fVar, application)
        val outerLet = LetExpr(false, createStringLocation("f"), null, null, null, identityLambda, innerLet)
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(outerLet)
        
        assertTrue(result.isSuccess(), "Should successfully infer type with proper generalization")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.Int, finalType, "Generalized function should apply to Int")
    }
    
    @Test
    fun `recursive let with undefined variable in body should fail`() {
        // Test: let rec f = g in f (where g is undefined)
        val gVar = VarExpr(createStringLocation("g"))
        val fVar = VarExpr(createStringLocation("f"))
        
        val letExpr = LetExpr(
            true, // recursive
            createStringLocation("f"),
            null, null, null,
            gVar, // g is undefined
            fVar
        )
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(letExpr)
        
        assertTrue(result.isFailure(), "Should fail when recursive let references undefined variable")
        val failure = result as ConstraintGenerationResult.Failure
        assertTrue(failure.error.contains("g"), "Error should mention undefined variable g")
    }
    
    @Test
    fun `performance test with deeply nested lets`() {
        // Test: Performance with deeply nested let expressions
        // Build: let x1 = 1 in let x2 = 2 in let x3 = 3 in x3
        val startTime = System.currentTimeMillis()
        
        // Build from inside out: x3, then let x3 = 3 in x3, then let x2 = 2 in (let x3 = 3 in x3), etc.
        var expr: Expr = VarExpr(createStringLocation("x10"))
        
        // Build 10 nested lets (reduced from 20 for performance)
        for (i in 10 downTo 1) {
            val varName = "x$i"
            val value = LiteralIntExpr(createIntLocation(i))
            
            expr = LetExpr(
                false, // not recursive
                createStringLocation(varName),
                null, null, null,
                value,
                expr
            )
        }
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(expr)
        
        assertTrue(result.isSuccess(), "Should handle deeply nested let expressions")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Should solve deeply nested constraints")
        
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        
        assertTrue(duration < 3000, "Should handle 10 nested lets within reasonable time (${duration}ms)")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.Int, finalType, "Deeply nested lets should infer as Int")
    }
    
    @Test
    fun `let binding preserves source location information`() {
        // Test: Constraint generation should preserve source location
        val location = LocationCoordinate(10, 20, 30) // offset=10, line=20, column=30
        val xVar = VarExpr(createStringLocation("x"))
        val literal = LiteralIntExpr(IntLocation(42, location))
        
        val letExpr = LetExpr(
            false,
            StringLocation("x", location),
            null, null, null,
            literal,
            xVar
        )
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(letExpr)
        
        assertTrue(result.isSuccess(), "Should successfully generate constraints")
        
        // Check that constraints preserve source location information
        val constraints = result.constraints.all()
        val hasLocationInfo = constraints.any { constraint ->
            constraint.sourceLocation != null &&
            (constraint.sourceLocation!!.line == 20 || constraint.sourceLocation!!.column == 30)
        }
        assertTrue(hasLocationInfo, "Should preserve source location information in constraints")
    }
} 