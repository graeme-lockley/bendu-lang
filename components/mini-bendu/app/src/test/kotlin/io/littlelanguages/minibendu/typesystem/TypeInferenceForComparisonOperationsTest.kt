package io.littlelanguages.minibendu.typesystem

import io.littlelanguages.minibendu.*
import io.littlelanguages.scanpiler.LocationCoordinate
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Comprehensive tests for type inference of comparison operations.
 * Task 27 of Phase 1 - Type Inference for Basic Expressions.
 * 
 * Tests cover:
 * - Equality and inequality operations (==, !=)
 * - Type checking for comparisons with compatible types
 * - Type checking for comparisons with incompatible types
 * - Structural equality for complex types
 * - Result type inference (should always be Bool)
 * - Error handling for unsupported comparisons
 */
class TypeInferenceForComparisonOperationsTest {
    
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
    fun `infer type for integer equality`() {
        // Test: 5 == 3 should infer as Bool
        val left = LiteralIntExpr(createIntLocation(5))
        val right = LiteralIntExpr(createIntLocation(3))
        val equality = BinaryOpExpr(left, BinaryOp.EqualEqual, right, createLocation())
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(equality)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for integer equality")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.Bool, finalType, "Integer equality should result in Bool")
    }
    
    @Test
    fun `infer type for integer inequality`() {
        // Test: 10 != 4 should infer as Bool
        val left = LiteralIntExpr(createIntLocation(10))
        val right = LiteralIntExpr(createIntLocation(4))
        val inequality = BinaryOpExpr(left, BinaryOp.NotEqual, right, createLocation())
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(inequality)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for integer inequality")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.Bool, finalType, "Integer inequality should result in Bool")
    }
    
    @Test
    fun `infer type for string equality`() {
        // Test: "hello" == "world" should infer as Bool
        val left = LiteralStringExpr(createStringLocation("hello"))
        val right = LiteralStringExpr(createStringLocation("world"))
        val equality = BinaryOpExpr(left, BinaryOp.EqualEqual, right, createLocation())
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(equality)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for string equality")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.Bool, finalType, "String equality should result in Bool")
    }
    
    @Test
    fun `infer type for boolean equality`() {
        // Test: true == false should infer as Bool
        val left = LiteralBoolExpr(createBoolLocation(true))
        val right = LiteralBoolExpr(createBoolLocation(false))
        val equality = BinaryOpExpr(left, BinaryOp.EqualEqual, right, createLocation())
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(equality)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for boolean equality")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.Bool, finalType, "Boolean equality should result in Bool")
    }
    
    @Test
    fun `comparison with variables`() {
        // Test: x == y where x and y are Int variables
        val xVar = VarExpr(createStringLocation("x"))
        val yVar = VarExpr(createStringLocation("y"))
        val equality = BinaryOpExpr(xVar, BinaryOp.EqualEqual, yVar, createLocation())
        
        val env = TypeEnvironment.empty()
            .extend("x", Types.Int)
            .extend("y", Types.Int)
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(equality)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for variable equality")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.Bool, finalType, "Variable equality should result in Bool")
    }
    
    @Test
    fun `type error for mixed type comparison`() {
        // Test: 5 == "hello" should produce type error
        val left = LiteralIntExpr(createIntLocation(5))
        val right = LiteralStringExpr(createStringLocation("hello"))
        val equality = BinaryOpExpr(left, BinaryOp.EqualEqual, right, createLocation())
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(equality)
        
        assertTrue(result.isSuccess(), "Constraint generation should succeed")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Failure, "Should fail to solve constraints for mixed type comparison")
        
        val failure = solverResult as ConstraintSolverResult.Failure
        assertNotNull(failure.error, "Should provide meaningful error message")
        assertTrue(failure.error.contains("Int") || failure.error.contains("String"), 
                  "Error should mention the conflicting types")
    }
    
    @Test
    fun `type error for incompatible variable comparison`() {
        // Test: x == y where x is Int and y is String should produce type error
        val xVar = VarExpr(createStringLocation("x"))
        val yVar = VarExpr(createStringLocation("y"))
        val equality = BinaryOpExpr(xVar, BinaryOp.EqualEqual, yVar, createLocation())
        
        val env = TypeEnvironment.empty()
            .extend("x", Types.Int)
            .extend("y", Types.String)
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(equality)
        
        assertTrue(result.isSuccess(), "Constraint generation should succeed")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Failure, "Should fail to solve constraints for incompatible variable types")
        
        val failure = solverResult as ConstraintSolverResult.Failure
        assertNotNull(failure.error, "Should provide meaningful error message")
    }
    
    @Test
    fun `variable type unification through comparison`() {
        // Test: x == 5 should unify x to Int type
        val xVar = VarExpr(createStringLocation("x"))
        val literal = LiteralIntExpr(createIntLocation(5))
        val equality = BinaryOpExpr(xVar, BinaryOp.EqualEqual, literal, createLocation())
        
        val xType = TypeVariable.fresh()
        val env = TypeEnvironment.empty().extend("x", xType)
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(equality)
        
        assertTrue(result.isSuccess(), "Should successfully generate constraints")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Should solve constraints successfully")
        
        val substitution = (solverResult as ConstraintSolverResult.Success).substitution
        val xFinalType = substitution.apply(xType)
        val resultType = substitution.apply(result.type)
        
        assertEquals(Types.Int, xFinalType, "Variable x should be unified to Int through comparison")
        assertEquals(Types.Bool, resultType, "Comparison result should be Bool")
    }
    
    @Test
    fun `bidirectional type unification in comparison`() {
        // Test: x == y should unify both x and y to the same type when both are type variables
        val xVar = VarExpr(createStringLocation("x"))
        val yVar = VarExpr(createStringLocation("y"))
        val equality = BinaryOpExpr(xVar, BinaryOp.EqualEqual, yVar, createLocation())
        
        val xType = TypeVariable.fresh()
        val yType = TypeVariable.fresh()
        val env = TypeEnvironment.empty()
            .extend("x", xType)
            .extend("y", yType)
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(equality)
        
        assertTrue(result.isSuccess(), "Should successfully generate constraints")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Should solve constraints successfully")
        
        val substitution = (solverResult as ConstraintSolverResult.Success).substitution
        val xFinalType = substitution.apply(xType)
        val yFinalType = substitution.apply(yType)
        val resultType = substitution.apply(result.type)
        
        assertEquals(xFinalType, yFinalType, "Variables x and y should be unified to the same type")
        assertEquals(Types.Bool, resultType, "Comparison result should be Bool")
    }
    
    @Test
    fun `string literal type equality`() {
        // Test: Equality of string literal types
        val left = LiteralStringExpr(createStringLocation("success"))
        val right = LiteralStringExpr(createStringLocation("success"))
        val equality = BinaryOpExpr(left, BinaryOp.EqualEqual, right, createLocation())
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(equality)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for string literal equality")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.Bool, finalType, "String literal equality should result in Bool")
    }
    
    @Test
    fun `different string literal types comparison`() {
        // Test: "success" == "error" should still type check as Bool
        val left = LiteralStringExpr(createStringLocation("success"))
        val right = LiteralStringExpr(createStringLocation("error"))
        val equality = BinaryOpExpr(left, BinaryOp.EqualEqual, right, createLocation())
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(equality)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for different string literal equality")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.Bool, finalType, "Different string literal equality should result in Bool")
    }
    
    @Test
    fun `chained comparison operations`() {
        // Test: x == y && y != z (combination of comparison and logical operations)
        val xVar = VarExpr(createStringLocation("x"))
        val yVar1 = VarExpr(createStringLocation("y"))
        val yVar2 = VarExpr(createStringLocation("y"))
        val zVar = VarExpr(createStringLocation("z"))
        
        val equality = BinaryOpExpr(xVar, BinaryOp.EqualEqual, yVar1, createLocation())
        val inequality = BinaryOpExpr(yVar2, BinaryOp.NotEqual, zVar, createLocation())
        val logical = BinaryOpExpr(equality, BinaryOp.And, inequality, createLocation())
        
        val env = TypeEnvironment.empty()
            .extend("x", Types.Int)
            .extend("y", Types.Int)
            .extend("z", Types.Int)
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(logical)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for chained comparisons")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.Bool, finalType, "Chained comparisons should result in Bool")
    }
    
    @Test
    fun `comparison in conditional context`() {
        // Test: Comparison used as condition in if expression
        val xVar = VarExpr(createStringLocation("x"))
        val literal = LiteralIntExpr(createIntLocation(0))
        val comparison = BinaryOpExpr(xVar, BinaryOp.EqualEqual, literal, createLocation())
        
        val env = TypeEnvironment.empty().extend("x", Types.Int)
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(comparison)
        
        assertTrue(result.isSuccess(), "Should handle comparison in conditional context")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Should solve comparison in conditional context")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.Bool, finalType, "Comparison should produce Bool for conditional")
    }
    
    @Test
    fun `constraint generation preserves source location for comparisons`() {
        // Test: Source location should be preserved in comparison constraints
        val location = LocationCoordinate(0, 8, 12) // line 8, column 12
        val left = LiteralIntExpr(createIntLocation(42))
        val right = LiteralIntExpr(createIntLocation(24))
        val equality = BinaryOpExpr(left, BinaryOp.EqualEqual, right, location)
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(equality)
        
        assertTrue(result.isSuccess(), "Should succeed with proper source location")
        
        // Check that constraints preserve source location
        val constraints = result.constraints.all()
        val hasLocationConstraints = constraints.any { constraint ->
            constraint.sourceLocation != null &&
            constraint.sourceLocation!!.line == 8 &&
            constraint.sourceLocation!!.column == 12
        }
        
        assertTrue(hasLocationConstraints, "Should preserve source location in comparison constraints")
    }
    
    @Test
    fun `comparison of complex expressions`() {
        // Test: (x + y) == (z * w) should type check correctly
        val xVar = VarExpr(createStringLocation("x"))
        val yVar = VarExpr(createStringLocation("y"))
        val zVar = VarExpr(createStringLocation("z"))
        val wVar = VarExpr(createStringLocation("w"))
        
        val addition = BinaryOpExpr(xVar, BinaryOp.Plus, yVar, createLocation())
        val multiplication = BinaryOpExpr(zVar, BinaryOp.Star, wVar, createLocation())
        val equality = BinaryOpExpr(addition, BinaryOp.EqualEqual, multiplication, createLocation())
        
        val env = TypeEnvironment.empty()
            .extend("x", Types.Int)
            .extend("y", Types.Int)
            .extend("z", Types.Int)
            .extend("w", Types.Int)
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(equality)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for complex expression comparison")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.Bool, finalType, "Complex expression comparison should result in Bool")
    }
    
    @Test
    fun `inequality operations behave like equality`() {
        // Test: All inequality operators should have same typing behavior as equality
        val operators = listOf(BinaryOp.EqualEqual, BinaryOp.NotEqual)
        
        for (op in operators) {
            val left = LiteralIntExpr(createIntLocation(10))
            val right = LiteralIntExpr(createIntLocation(20))
            val comparison = BinaryOpExpr(left, op, right, createLocation())
            
            val generator = ConstraintGenerator()
            val result = generator.generateConstraints(comparison)
            
            assertTrue(result.isSuccess(), "Should successfully infer type for $op")
            
            val solver = ConstraintSolver()
            val solverResult = solver.solve(result.constraints)
            assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed for $op")
            
            val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
            assertEquals(Types.Bool, finalType, "Operator $op should result in Bool")
        }
    }
    
    @Test
    fun `performance test with many comparison operations`() {
        // Test: Efficient constraint generation for chained comparisons
        // Build expression: x1 == x2 && x2 != x3 && x3 == x4 && ...
        val variables = (1..20).map { i ->
            VarExpr(createStringLocation("x$i"))
        }
        
        var expr: Expr = BinaryOpExpr(variables[0], BinaryOp.EqualEqual, variables[1], createLocation())
        
        for (i in 2 until variables.size) {
            val op = if (i % 2 == 0) BinaryOp.EqualEqual else BinaryOp.NotEqual
            val comparison = BinaryOpExpr(variables[i-1], op, variables[i], createLocation())
            expr = BinaryOpExpr(expr, BinaryOp.And, comparison, createLocation())
        }
        
        var env = TypeEnvironment.empty()
        for (i in 1..20) {
            env = env.extend("x$i", Types.Int)
        }
        
        val generator = ConstraintGenerator(env)
        val startTime = System.currentTimeMillis()
        
        val result = generator.generateConstraints(expr)
        
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        
        assertTrue(result.isSuccess(), "Should handle many comparison operations")
        assertTrue(duration < 200, "Should generate constraints efficiently (${duration}ms)")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Should solve many comparisons")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.Bool, finalType, "Chained comparisons should result in Bool")
    }
} 