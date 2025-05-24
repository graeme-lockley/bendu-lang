package io.littlelanguages.minibendu.typesystem

import io.littlelanguages.minibendu.*
import io.littlelanguages.scanpiler.LocationCoordinate
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Comprehensive tests for type inference of logical operations.
 * Task 29 of Phase 1 - Type Inference for Basic Expressions.
 * 
 * Tests cover:
 * - AND (&&) and OR (||) operations
 * - Type checking ensuring boolean operands
 * - Error handling for non-boolean operands
 * - Short-circuit evaluation typing implications
 * - Logical operations with variables and literals
 * - Nested logical expressions
 */
class TypeInferenceForLogicalOperationsTest {
    
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
    fun `infer type for logical AND with boolean literals`() {
        // Test: true && false should infer as Bool
        val left = LiteralBoolExpr(createBoolLocation(true))
        val right = LiteralBoolExpr(createBoolLocation(false))
        val andOp = BinaryOpExpr(left, BinaryOp.And, right, createLocation())
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(andOp)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for logical AND")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.Bool, finalType, "Logical AND should result in Bool")
    }
    
    @Test
    fun `infer type for logical OR with boolean literals`() {
        // Test: true || false should infer as Bool
        val left = LiteralBoolExpr(createBoolLocation(true))
        val right = LiteralBoolExpr(createBoolLocation(false))
        val orOp = BinaryOpExpr(left, BinaryOp.Or, right, createLocation())
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(orOp)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for logical OR")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.Bool, finalType, "Logical OR should result in Bool")
    }
    
    @Test
    fun `logical operations with boolean variables`() {
        // Test: x && y where x and y are Bool variables
        val xVar = VarExpr(createStringLocation("x"))
        val yVar = VarExpr(createStringLocation("y"))
        val andOp = BinaryOpExpr(xVar, BinaryOp.And, yVar, createLocation())
        
        val env = TypeEnvironment.empty()
            .extend("x", Types.Bool)
            .extend("y", Types.Bool)
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(andOp)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for variable logical AND")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.Bool, finalType, "Logical AND of Bool variables should result in Bool")
    }
    
    @Test
    fun `type error for logical AND with integer operands`() {
        // Test: 5 && 3 should produce type error
        val left = LiteralIntExpr(createIntLocation(5))
        val right = LiteralIntExpr(createIntLocation(3))
        val andOp = BinaryOpExpr(left, BinaryOp.And, right, createLocation())
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(andOp)
        
        assertTrue(result.isSuccess(), "Constraint generation should succeed")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Failure, "Should fail to solve constraints for integer logical AND")
        
        val failure = solverResult as ConstraintSolverResult.Failure
        assertNotNull(failure.error, "Should provide meaningful error message")
        assertTrue(failure.error.contains("Bool") || failure.error.contains("Int"), 
                  "Error should mention type conflict")
    }
    
    @Test
    fun `type error for logical OR with string operands`() {
        // Test: "hello" || "world" should produce type error
        val left = LiteralStringExpr(createStringLocation("hello"))
        val right = LiteralStringExpr(createStringLocation("world"))
        val orOp = BinaryOpExpr(left, BinaryOp.Or, right, createLocation())
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(orOp)
        
        assertTrue(result.isSuccess(), "Constraint generation should succeed")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Failure, "Should fail to solve constraints for string logical OR")
        
        val failure = solverResult as ConstraintSolverResult.Failure
        assertNotNull(failure.error, "Should provide meaningful error message")
    }
    
    @Test
    fun `type error for mixed type logical operations`() {
        // Test: true && "hello" should produce type error
        val left = LiteralBoolExpr(createBoolLocation(true))
        val right = LiteralStringExpr(createStringLocation("hello"))
        val andOp = BinaryOpExpr(left, BinaryOp.And, right, createLocation())
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(andOp)
        
        assertTrue(result.isSuccess(), "Constraint generation should succeed")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Failure, "Should fail to solve constraints for mixed type logical operation")
        
        val failure = solverResult as ConstraintSolverResult.Failure
        assertNotNull(failure.error, "Should provide meaningful error message")
    }
    
    @Test
    fun `variable type inference through logical constraints`() {
        // Test: x && true should infer x as Bool
        val xVar = VarExpr(createStringLocation("x"))
        val literal = LiteralBoolExpr(createBoolLocation(true))
        val andOp = BinaryOpExpr(xVar, BinaryOp.And, literal, createLocation())
        
        val xType = TypeVariable.fresh()
        val env = TypeEnvironment.empty().extend("x", xType)
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(andOp)
        
        assertTrue(result.isSuccess(), "Should successfully generate constraints")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Should solve constraints successfully")
        
        val substitution = (solverResult as ConstraintSolverResult.Success).substitution
        val xFinalType = substitution.apply(xType)
        
        assertEquals(Types.Bool, xFinalType, "Variable x should be inferred as Bool through logical constraint")
    }
    
    @Test
    fun `nested logical expressions`() {
        // Test: (x && y) || (z && w) where all variables are Bool
        val xVar = VarExpr(createStringLocation("x"))
        val yVar = VarExpr(createStringLocation("y"))
        val zVar = VarExpr(createStringLocation("z"))
        val wVar = VarExpr(createStringLocation("w"))
        
        val leftAnd = BinaryOpExpr(xVar, BinaryOp.And, yVar, createLocation())
        val rightAnd = BinaryOpExpr(zVar, BinaryOp.And, wVar, createLocation())
        val orOp = BinaryOpExpr(leftAnd, BinaryOp.Or, rightAnd, createLocation())
        
        val env = TypeEnvironment.empty()
            .extend("x", Types.Bool)
            .extend("y", Types.Bool)
            .extend("z", Types.Bool)
            .extend("w", Types.Bool)
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(orOp)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for nested logical operations")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.Bool, finalType, "Nested logical operations should result in Bool")
    }
    
    @Test
    fun `logical operations with comparison results`() {
        // Test: (x == y) && (z != w) where variables are Int
        val xVar = VarExpr(createStringLocation("x"))
        val yVar = VarExpr(createStringLocation("y"))
        val zVar = VarExpr(createStringLocation("z"))
        val wVar = VarExpr(createStringLocation("w"))
        
        val equality = BinaryOpExpr(xVar, BinaryOp.EqualEqual, yVar, createLocation())
        val inequality = BinaryOpExpr(zVar, BinaryOp.NotEqual, wVar, createLocation())
        val andOp = BinaryOpExpr(equality, BinaryOp.And, inequality, createLocation())
        
        val env = TypeEnvironment.empty()
            .extend("x", Types.Int)
            .extend("y", Types.Int)
            .extend("z", Types.Int)
            .extend("w", Types.Int)
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(andOp)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for logical operation with comparisons")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.Bool, finalType, "Logical operation with comparisons should result in Bool")
    }
    
    @Test
    fun `logical operator precedence`() {
        // Test: x || y && z should be parsed as x || (y && z) and type correctly
        // This tests that the constraint generation works with proper precedence
        val xVar = VarExpr(createStringLocation("x"))
        val yVar = VarExpr(createStringLocation("y"))
        val zVar = VarExpr(createStringLocation("z"))
        
        // Build as parsed: x || (y && z)
        val andOp = BinaryOpExpr(yVar, BinaryOp.And, zVar, createLocation())
        val orOp = BinaryOpExpr(xVar, BinaryOp.Or, andOp, createLocation())
        
        val env = TypeEnvironment.empty()
            .extend("x", Types.Bool)
            .extend("y", Types.Bool)
            .extend("z", Types.Bool)
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(orOp)
        
        assertTrue(result.isSuccess(), "Should successfully handle precedence in logical constraints")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.Bool, finalType, "Precedence expression should result in Bool")
    }
    
    @Test
    fun `logical operations in conditional context`() {
        // Test: Logical operations used as condition in if expression
        val condition1 = VarExpr(createStringLocation("condition1"))
        val condition2 = VarExpr(createStringLocation("condition2"))
        val logicalAnd = BinaryOpExpr(condition1, BinaryOp.And, condition2, createLocation())
        
        val env = TypeEnvironment.empty()
            .extend("condition1", Types.Bool)
            .extend("condition2", Types.Bool)
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(logicalAnd)
        
        assertTrue(result.isSuccess(), "Should handle logical operations in conditional context")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Should solve logical operations in conditional context")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.Bool, finalType, "Logical operations should produce Bool for conditional")
    }
    
    @Test
    fun `constraint generation preserves source location for logical operations`() {
        // Test: Source location should be preserved in logical operation constraints
        val location = LocationCoordinate(0, 15, 20) // line 15, column 20
        val left = LiteralBoolExpr(createBoolLocation(true))
        val right = LiteralBoolExpr(createBoolLocation(false))
        val andOp = BinaryOpExpr(left, BinaryOp.And, right, location)
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(andOp)
        
        assertTrue(result.isSuccess(), "Should succeed with proper source location")
        
        // Check that constraints preserve source location
        val constraints = result.constraints.all()
        val hasLocationConstraints = constraints.any { constraint ->
            constraint.sourceLocation != null &&
            constraint.sourceLocation!!.line == 15 &&
            constraint.sourceLocation!!.column == 20
        }
        
        assertTrue(hasLocationConstraints, "Should preserve source location in logical operation constraints")
    }
    
    @Test
    fun `both logical operators have same typing behavior`() {
        // Test: All logical operators should require Bool operands and result in Bool
        val operators = listOf(BinaryOp.And, BinaryOp.Or)
        
        for (op in operators) {
            val left = LiteralBoolExpr(createBoolLocation(true))
            val right = LiteralBoolExpr(createBoolLocation(false))
            val logicalOp = BinaryOpExpr(left, op, right, createLocation())
            
            val generator = ConstraintGenerator()
            val result = generator.generateConstraints(logicalOp)
            
            assertTrue(result.isSuccess(), "Should successfully infer type for $op")
            
            val solver = ConstraintSolver()
            val solverResult = solver.solve(result.constraints)
            assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed for $op")
            
            val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
            assertEquals(Types.Bool, finalType, "Operator $op should result in Bool")
        }
    }
    
    @Test
    fun `logical operations with type variable propagation`() {
        // Test: If a && b and both a and b are type variables, they should be unified to Bool
        val aVar = VarExpr(createStringLocation("a"))
        val bVar = VarExpr(createStringLocation("b"))
        val andOp = BinaryOpExpr(aVar, BinaryOp.And, bVar, createLocation())
        
        val aType = TypeVariable.fresh()
        val bType = TypeVariable.fresh()
        val env = TypeEnvironment.empty()
            .extend("a", aType)
            .extend("b", bType)
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(andOp)
        
        assertTrue(result.isSuccess(), "Should successfully generate constraints for type variables")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val substitution = (solverResult as ConstraintSolverResult.Success).substitution
        val aFinalType = substitution.apply(aType)
        val bFinalType = substitution.apply(bType)
        val resultFinalType = substitution.apply(result.type)
        
        assertEquals(Types.Bool, aFinalType, "Variable a should be inferred as Bool")
        assertEquals(Types.Bool, bFinalType, "Variable b should be inferred as Bool")
        assertEquals(Types.Bool, resultFinalType, "Result should be Bool")
    }
    
    @Test
    fun `error for logical operation with one incompatible operand`() {
        // Test: x && 5 where x is Bool should produce type error
        val xVar = VarExpr(createStringLocation("x"))
        val intLiteral = LiteralIntExpr(createIntLocation(5))
        val andOp = BinaryOpExpr(xVar, BinaryOp.And, intLiteral, createLocation())
        
        val env = TypeEnvironment.empty().extend("x", Types.Bool)
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(andOp)
        
        assertTrue(result.isSuccess(), "Constraint generation should succeed")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Failure, "Should fail to solve constraints for mixed logical operation")
        
        val failure = solverResult as ConstraintSolverResult.Failure
        assertNotNull(failure.error, "Should provide meaningful error message")
    }
    
    @Test
    fun `complex logical expression with all boolean sub-expressions`() {
        // Test: ((a && b) || (c && d)) && ((e || f) && (g || h))
        val variables = listOf("a", "b", "c", "d", "e", "f", "g", "h").map { name ->
            VarExpr(createStringLocation(name))
        }
        
        val a = variables[0]
        val b = variables[1]
        val c = variables[2]
        val d = variables[3]
        val e = variables[4]
        val f = variables[5]
        val g = variables[6]
        val h = variables[7]
        
        val ab = BinaryOpExpr(a, BinaryOp.And, b, createLocation())
        val cd = BinaryOpExpr(c, BinaryOp.And, d, createLocation())
        val ef = BinaryOpExpr(e, BinaryOp.Or, f, createLocation())
        val gh = BinaryOpExpr(g, BinaryOp.Or, h, createLocation())
        
        val left = BinaryOpExpr(ab, BinaryOp.Or, cd, createLocation())
        val right = BinaryOpExpr(ef, BinaryOp.And, gh, createLocation())
        val complex = BinaryOpExpr(left, BinaryOp.And, right, createLocation())
        
        var env = TypeEnvironment.empty()
        for (varName in listOf("a", "b", "c", "d", "e", "f", "g", "h")) {
            env = env.extend(varName, Types.Bool)
        }
        
        val generator = ConstraintGenerator(env)
        val result = generator.generateConstraints(complex)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for complex logical expression")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.Bool, finalType, "Complex logical expression should result in Bool")
    }
    
    @Test
    fun `performance test with many logical operations`() {
        // Test: Efficient constraint generation for long chain of logical operations
        // Build expression: x1 && x2 && x3 && ... && x50
        val variables = (1..50).map { i ->
            VarExpr(createStringLocation("x$i"))
        }
        
        var expr: Expr = variables[0]
        
        for (i in 1 until variables.size) {
            val op = if (i % 2 == 0) BinaryOp.And else BinaryOp.Or
            expr = BinaryOpExpr(expr, op, variables[i], createLocation())
        }
        
        var env = TypeEnvironment.empty()
        for (i in 1..50) {
            env = env.extend("x$i", Types.Bool)
        }
        
        val generator = ConstraintGenerator(env)
        val startTime = System.currentTimeMillis()
        
        val result = generator.generateConstraints(expr)
        
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        
        assertTrue(result.isSuccess(), "Should handle many logical operations")
        assertTrue(duration < 200, "Should generate constraints efficiently (${duration}ms)")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Should solve many logical operations")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.Bool, finalType, "Chain of logical operations should result in Bool")
    }
} 