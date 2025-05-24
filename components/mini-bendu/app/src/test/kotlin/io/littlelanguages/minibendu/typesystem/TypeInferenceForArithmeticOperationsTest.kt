package io.littlelanguages.minibendu.typesystem

import io.littlelanguages.minibendu.*
import io.littlelanguages.scanpiler.LocationCoordinate
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Comprehensive tests for type inference of arithmetic operations.
 * Task 25 of Phase 1 - Type Inference for Basic Expressions.
 * 
 * Tests cover:
 * - Addition, subtraction, multiplication, division operations
 * - Type checking for operations with compatible types
 * - Error messages for type mismatches
 * - Type inference with variables and literals
 * - Operator precedence and associativity
 * - Nested arithmetic expressions
 */
class TypeInferenceForArithmeticOperationsTest {
    
    private fun createStringLocation(value: String): StringLocation {
        return StringLocation(value, LocationCoordinate(0, 1, 1))
    }
    
    private fun createIntLocation(value: Int): IntLocation {
        return IntLocation(value, LocationCoordinate(0, 1, 1))
    }
    
    private fun createLocation(): LocationCoordinate = LocationCoordinate(0, 1, 1)
    
    @Test
    fun `infer type for simple addition`() {
        // Test: 5 + 3 should infer as Int
        val left = LiteralIntExpr(createIntLocation(5))
        val right = LiteralIntExpr(createIntLocation(3))
        val addition = BinaryOpExpr(left, BinaryOp.Plus, right, createLocation())
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(addition)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for addition")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.Int, finalType, "Addition of integers should result in Int")
    }
    
    @Test
    fun `infer type for subtraction`() {
        // Test: 10 - 4 should infer as Int
        val left = LiteralIntExpr(createIntLocation(10))
        val right = LiteralIntExpr(createIntLocation(4))
        val subtraction = BinaryOpExpr(left, BinaryOp.Minus, right, createLocation())
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(subtraction)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for subtraction")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.Int, finalType, "Subtraction of integers should result in Int")
    }
    
    @Test
    fun `infer type for multiplication`() {
        // Test: 6 * 7 should infer as Int
        val left = LiteralIntExpr(createIntLocation(6))
        val right = LiteralIntExpr(createIntLocation(7))
        val multiplication = BinaryOpExpr(left, BinaryOp.Star, right, createLocation())
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(multiplication)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for multiplication")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.Int, finalType, "Multiplication of integers should result in Int")
    }
    
    @Test
    fun `infer type for division`() {
        // Test: 20 / 4 should infer as Int
        val left = LiteralIntExpr(createIntLocation(20))
        val right = LiteralIntExpr(createIntLocation(4))
        val division = BinaryOpExpr(left, BinaryOp.Slash, right, createLocation())
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(division)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for division")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.Int, finalType, "Division of integers should result in Int")
    }
    
    @Test
    fun `arithmetic operations with variables`() {
        // Test: x + y where x and y are Int variables
        val xVar = VarExpr(createStringLocation("x"))
        val yVar = VarExpr(createStringLocation("y"))
        val addition = BinaryOpExpr(xVar, BinaryOp.Plus, yVar, createLocation())
        
        val env = TypeEnvironment.empty()
            .extend("x", Types.Int)
            .extend("y", Types.Int)
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(addition)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for variable addition")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.Int, finalType, "Addition of Int variables should result in Int")
    }
    
    @Test
    fun `type error for string addition`() {
        // Test: "hello" + "world" should produce type error (strings not supported for arithmetic)
        val left = LiteralStringExpr(createStringLocation("hello"))
        val right = LiteralStringExpr(createStringLocation("world"))
        val addition = BinaryOpExpr(left, BinaryOp.Plus, right, createLocation())
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(addition)
        
        assertTrue(result.isSuccess(), "Constraint generation should succeed even with incompatible types")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Failure, "Should fail to solve constraints for string arithmetic")
        
        val failure = solverResult as ConstraintSolverResult.Failure
        assertNotNull(failure.error, "Should provide meaningful error message")
        assertTrue(failure.error.contains("Int") || failure.error.contains("String"), 
                  "Error should mention type conflict")
    }
    
    @Test
    fun `type error for mixed type arithmetic`() {
        // Test: 5 + "hello" should produce type error
        val left = LiteralIntExpr(createIntLocation(5))
        val right = LiteralStringExpr(createStringLocation("hello"))
        val addition = BinaryOpExpr(left, BinaryOp.Plus, right, createLocation())
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(addition)
        
        assertTrue(result.isSuccess(), "Constraint generation should succeed")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Failure, "Should fail to solve constraints for mixed types")
        
        val failure = solverResult as ConstraintSolverResult.Failure
        assertNotNull(failure.error, "Should provide meaningful error message")
    }
    
    @Test
    fun `type error for boolean arithmetic`() {
        // Test: true + false should produce type error
        val left = LiteralBoolExpr(BoolLocation(true, createLocation()))
        val right = LiteralBoolExpr(BoolLocation(false, createLocation()))
        val addition = BinaryOpExpr(left, BinaryOp.Plus, right, createLocation())
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(addition)
        
        assertTrue(result.isSuccess(), "Constraint generation should succeed")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Failure, "Should fail to solve constraints for boolean arithmetic")
        
        val failure = solverResult as ConstraintSolverResult.Failure
        assertNotNull(failure.error, "Should provide meaningful error message")
    }
    
    @Test
    fun `variable type inference through arithmetic constraints`() {
        // Test: x + 5 should infer x as Int
        val xVar = VarExpr(createStringLocation("x"))
        val literal = LiteralIntExpr(createIntLocation(5))
        val addition = BinaryOpExpr(xVar, BinaryOp.Plus, literal, createLocation())
        
        val xType = TypeVariable.fresh()
        val env = TypeEnvironment.empty().extend("x", xType)
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(addition)
        
        assertTrue(result.isSuccess(), "Should successfully generate constraints")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Should solve constraints successfully")
        
        val substitution = (solverResult as ConstraintSolverResult.Success).substitution
        val xFinalType = substitution.apply(xType)
        
        assertEquals(Types.Int, xFinalType, "Variable x should be inferred as Int through arithmetic constraint")
    }
    
    @Test
    fun `nested arithmetic expressions`() {
        // Test: (x + y) * (z - w) where all variables are Int
        val xVar = VarExpr(createStringLocation("x"))
        val yVar = VarExpr(createStringLocation("y"))
        val zVar = VarExpr(createStringLocation("z"))
        val wVar = VarExpr(createStringLocation("w"))
        
        val addition = BinaryOpExpr(xVar, BinaryOp.Plus, yVar, createLocation())
        val subtraction = BinaryOpExpr(zVar, BinaryOp.Minus, wVar, createLocation())
        val multiplication = BinaryOpExpr(addition, BinaryOp.Star, subtraction, createLocation())
        
        val env = TypeEnvironment.empty()
            .extend("x", Types.Int)
            .extend("y", Types.Int)
            .extend("z", Types.Int)
            .extend("w", Types.Int)
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(multiplication)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for nested arithmetic")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.Int, finalType, "Nested arithmetic should result in Int")
    }
    
    @Test
    fun `arithmetic operator precedence constraints`() {
        // Test: 2 + 3 * 4 should be parsed as 2 + (3 * 4) and infer correctly
        // This tests that the constraint generation works with proper precedence
        val two = LiteralIntExpr(createIntLocation(2))
        val three = LiteralIntExpr(createIntLocation(3))
        val four = LiteralIntExpr(createIntLocation(4))
        
        // Build as parsed: 2 + (3 * 4)
        val multiplication = BinaryOpExpr(three, BinaryOp.Star, four, createLocation())
        val addition = BinaryOpExpr(two, BinaryOp.Plus, multiplication, createLocation())
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(addition)
        
        assertTrue(result.isSuccess(), "Should successfully handle precedence in constraints")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.Int, finalType, "Precedence expression should result in Int")
    }
    
    @Test
    fun `arithmetic with type variable propagation`() {
        // Test: If a + b = c and c is constrained to Int, then a and b should be Int
        val aVar = VarExpr(createStringLocation("a"))
        val bVar = VarExpr(createStringLocation("b"))
        val addition = BinaryOpExpr(aVar, BinaryOp.Plus, bVar, createLocation())
        
        val aType = TypeVariable.fresh()
        val bType = TypeVariable.fresh()
        val env = TypeEnvironment.empty()
            .extend("a", aType)
            .extend("b", bType)
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(addition)
        
        assertTrue(result.isSuccess(), "Should successfully generate constraints for type variables")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val substitution = (solverResult as ConstraintSolverResult.Success).substitution
        val aFinalType = substitution.apply(aType)
        val bFinalType = substitution.apply(bType)
        val resultFinalType = substitution.apply(result.type)
        
        assertEquals(Types.Int, aFinalType, "Variable a should be inferred as Int")
        assertEquals(Types.Int, bFinalType, "Variable b should be inferred as Int")
        assertEquals(Types.Int, resultFinalType, "Result should be Int")
    }
    
    @Test
    fun `constraint generation preserves source location`() {
        // Test: Source location should be preserved in arithmetic constraints
        val location = LocationCoordinate(0, 10, 5) // line 10, column 5
        val left = LiteralIntExpr(createIntLocation(42))
        val right = LiteralIntExpr(createIntLocation(24))
        val addition = BinaryOpExpr(left, BinaryOp.Plus, right, location)
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(addition)
        
        assertTrue(result.isSuccess(), "Should succeed with proper source location")
        
        // Check that constraints preserve source location
        val constraints = result.constraints.all()
        val hasLocationConstraints = constraints.any { constraint ->
            constraint.sourceLocation != null &&
            constraint.sourceLocation!!.line == 10 &&
            constraint.sourceLocation!!.column == 5
        }
        
        assertTrue(hasLocationConstraints, "Should preserve source location in constraints")
    }
    
    @Test
    fun `arithmetic operations in complex context`() {
        // Test: Arithmetic operations used in function return types
        // simulate: function that returns the result of arithmetic
        val param1 = VarExpr(createStringLocation("x"))
        val param2 = VarExpr(createStringLocation("y"))
        val arithmetic = BinaryOpExpr(param1, BinaryOp.Star, param2, createLocation())
        
        val env = TypeEnvironment.empty()
            .extend("x", Types.Int)
            .extend("y", Types.Int)
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(arithmetic)
        
        assertTrue(result.isSuccess(), "Should handle arithmetic in complex contexts")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Should solve in complex context")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.Int, finalType, "Should maintain Int type in complex context")
    }
    
    @Test
    fun `performance test with many arithmetic operations`() {
        // Test: Efficient constraint generation for complex arithmetic expressions
        // Build expression: ((((1 + 2) * 3) - 4) / 5) + 6
        var expr: Expr = LiteralIntExpr(createIntLocation(1))
        
        val operations = listOf(
            BinaryOp.Plus to 2,
            BinaryOp.Star to 3,
            BinaryOp.Minus to 4,
            BinaryOp.Slash to 5,
            BinaryOp.Plus to 6
        )
        
        for ((op, value) in operations) {
            val rightOperand = LiteralIntExpr(createIntLocation(value))
            expr = BinaryOpExpr(expr, op, rightOperand, createLocation())
        }
        
        val generator = ConstraintGenerator()
        val startTime = System.currentTimeMillis()
        
        val result = generator.generateConstraints(expr)
        
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        
        assertTrue(result.isSuccess(), "Should handle complex arithmetic expressions")
        assertTrue(duration < 100, "Should generate constraints efficiently (${duration}ms)")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Should solve complex arithmetic")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.Int, finalType, "Complex arithmetic should result in Int")
    }
} 