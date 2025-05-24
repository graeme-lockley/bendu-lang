package io.littlelanguages.minibendu.typesystem

import io.littlelanguages.minibendu.*
import io.littlelanguages.scanpiler.LocationCoordinate
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Comprehensive tests for type inference of conditional expressions.
 * Task 37 of Phase 1 - Type Inference for Complex Expressions.
 * 
 * Tests cover:
 * - Type inference for if-then-else expressions: if condition then expr1 else expr2
 * - Type checking for condition expressions: condition must be Bool
 * - Type unification of then/else branches: both branches must have same type
 * - Nested conditional expressions: if-then-else within if-then-else
 * - Conditional expressions with complex sub-expressions
 * - Error handling for type mismatches in conditions and branches
 * - Conditional expressions in various contexts (function arguments, let bindings, etc.)
 */
class TypeInferenceForConditionalExpressionsTest {
    
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
    fun `simple conditional with literal values`() {
        // Test: if true then 42 else 24
        val condition = LiteralBoolExpr(createBoolLocation(true))
        val thenBranch = LiteralIntExpr(createIntLocation(42))
        val elseBranch = LiteralIntExpr(createIntLocation(24))
        val ifExpr = IfExpr(condition, thenBranch, elseBranch, createLocation())
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(ifExpr)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for simple conditional")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.Int, finalType, "Conditional with Int branches should result in Int")
    }
    
    @Test
    fun `conditional with variable condition`() {
        // Test: if flag then "yes" else "no"
        val flagVar = VarExpr(createStringLocation("flag"))
        val thenBranch = LiteralStringExpr(createStringLocation("yes"))
        val elseBranch = LiteralStringExpr(createStringLocation("no"))
        val ifExpr = IfExpr(flagVar, thenBranch, elseBranch, createLocation())
        
        val env = TypeEnvironment.empty().bind("flag", TypeScheme.monomorphic(Types.Bool))
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(ifExpr)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for conditional with variable condition")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.String, finalType, "Conditional with String branches should result in String")
    }
    
    @Test
    fun `conditional with complex condition expression`() {
        // Test: if x == 0 then 1 else x
        val xVar1 = VarExpr(createStringLocation("x"))
        val zero = LiteralIntExpr(createIntLocation(0))
        val condition = BinaryOpExpr(xVar1, BinaryOp.EqualEqual, zero, createLocation())
        
        val one = LiteralIntExpr(createIntLocation(1))
        val xVar2 = VarExpr(createStringLocation("x"))
        val ifExpr = IfExpr(condition, one, xVar2, createLocation())
        
        val env = TypeEnvironment.empty().bind("x", TypeScheme.monomorphic(Types.Int))
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(ifExpr)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for conditional with complex condition")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.Int, finalType, "Conditional should result in Int")
    }
    
    @Test
    fun `conditional with arithmetic expressions in branches`() {
        // Test: if condition then x + 1 else x - 1
        val conditionVar = VarExpr(createStringLocation("condition"))
        val xVar1 = VarExpr(createStringLocation("x"))
        val xVar2 = VarExpr(createStringLocation("x"))
        val one1 = LiteralIntExpr(createIntLocation(1))
        val one2 = LiteralIntExpr(createIntLocation(1))
        
        val thenBranch = BinaryOpExpr(xVar1, BinaryOp.Plus, one1, createLocation())
        val elseBranch = BinaryOpExpr(xVar2, BinaryOp.Minus, one2, createLocation())
        val ifExpr = IfExpr(conditionVar, thenBranch, elseBranch, createLocation())
        
        val env = TypeEnvironment.empty()
            .bind("condition", TypeScheme.monomorphic(Types.Bool))
            .bind("x", TypeScheme.monomorphic(Types.Int))
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(ifExpr)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for conditional with arithmetic branches")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.Int, finalType, "Conditional with arithmetic branches should result in Int")
    }
    
    @Test
    fun `nested conditional expressions`() {
        // Test: if a then (if b then 1 else 2) else 3
        val aVar = VarExpr(createStringLocation("a"))
        val bVar = VarExpr(createStringLocation("b"))
        val one = LiteralIntExpr(createIntLocation(1))
        val two = LiteralIntExpr(createIntLocation(2))
        val three = LiteralIntExpr(createIntLocation(3))
        
        val innerIf = IfExpr(bVar, one, two, createLocation())
        val outerIf = IfExpr(aVar, innerIf, three, createLocation())
        
        val env = TypeEnvironment.empty()
            .bind("a", TypeScheme.monomorphic(Types.Bool))
            .bind("b", TypeScheme.monomorphic(Types.Bool))
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(outerIf)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for nested conditionals")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.Int, finalType, "Nested conditionals should result in Int")
    }
    
    @Test
    fun `conditional with function calls in branches`() {
        // Test: if condition then f(x) else g(y)
        val conditionVar = VarExpr(createStringLocation("condition"))
        val fVar = VarExpr(createStringLocation("f"))
        val gVar = VarExpr(createStringLocation("g"))
        val xVar = VarExpr(createStringLocation("x"))
        val yVar = VarExpr(createStringLocation("y"))
        
        val fCall = ApplicationExpr(fVar, listOf(xVar), createLocation())
        val gCall = ApplicationExpr(gVar, listOf(yVar), createLocation())
        val ifExpr = IfExpr(conditionVar, fCall, gCall, createLocation())
        
        val fType = FunctionType(Types.Int, Types.String)
        val gType = FunctionType(Types.Bool, Types.String)
        val env = TypeEnvironment.empty()
            .bind("condition", TypeScheme.monomorphic(Types.Bool))
            .bind("f", TypeScheme.monomorphic(fType))
            .bind("g", TypeScheme.monomorphic(gType))
            .bind("x", TypeScheme.monomorphic(Types.Int))
            .bind("y", TypeScheme.monomorphic(Types.Bool))
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(ifExpr)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for conditional with function calls")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.String, finalType, "Conditional with String-returning functions should result in String")
    }
    
    @Test
    fun `conditional with lambda expressions in branches`() {
        // Test: if condition then (\x -> x + 1) else (\x -> x - 1)
        val conditionVar = VarExpr(createStringLocation("condition"))
        
        val xVar1 = VarExpr(createStringLocation("x"))
        val one1 = LiteralIntExpr(createIntLocation(1))
        val addition = BinaryOpExpr(xVar1, BinaryOp.Plus, one1, createLocation())
        val thenLambda = LambdaExpr(null, createStringLocation("x"), null, addition, createLocation())
        
        val xVar2 = VarExpr(createStringLocation("x"))
        val one2 = LiteralIntExpr(createIntLocation(1))
        val subtraction = BinaryOpExpr(xVar2, BinaryOp.Minus, one2, createLocation())
        val elseLambda = LambdaExpr(null, createStringLocation("x"), null, subtraction, createLocation())
        
        val ifExpr = IfExpr(conditionVar, thenLambda, elseLambda, createLocation())
        
        val env = TypeEnvironment.empty().bind("condition", TypeScheme.monomorphic(Types.Bool))
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(ifExpr)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for conditional with lambda branches")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertTrue(finalType is FunctionType, "Conditional with lambda branches should result in function type")
        val funcType = finalType as FunctionType
        assertEquals(Types.Int, funcType.domain, "Function should take Int")
        assertEquals(Types.Int, funcType.codomain, "Function should return Int")
    }
    
    @Test
    fun `conditional with tuple construction in branches`() {
        // Test: if condition then (1, "a") else (2, "b")
        val conditionVar = VarExpr(createStringLocation("condition"))
        
        val one = LiteralIntExpr(createIntLocation(1))
        val a = LiteralStringExpr(createStringLocation("a"))
        val thenTuple = TupleExpr(listOf(one, a), createLocation())
        
        val two = LiteralIntExpr(createIntLocation(2))
        val b = LiteralStringExpr(createStringLocation("b"))
        val elseTuple = TupleExpr(listOf(two, b), createLocation())
        
        val ifExpr = IfExpr(conditionVar, thenTuple, elseTuple, createLocation())
        
        val env = TypeEnvironment.empty().bind("condition", TypeScheme.monomorphic(Types.Bool))
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(ifExpr)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for conditional with tuple branches")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertTrue(finalType is TupleType, "Conditional with tuple branches should result in tuple type")
        val tupleType = finalType as TupleType
        assertEquals(2, tupleType.elements.size, "Tuple should have 2 elements")
        assertEquals(Types.Int, tupleType.elements[0], "First element should be Int")
        assertEquals(Types.String, tupleType.elements[1], "Second element should be String")
    }
    
    @Test
    fun `conditional with non-boolean condition should fail`() {
        // Test: if 42 then "yes" else "no" (should fail)
        val condition = LiteralIntExpr(createIntLocation(42))
        val thenBranch = LiteralStringExpr(createStringLocation("yes"))
        val elseBranch = LiteralStringExpr(createStringLocation("no"))
        val ifExpr = IfExpr(condition, thenBranch, elseBranch, createLocation())
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(ifExpr)
        
        assertTrue(result.isSuccess(), "Constraint generation should succeed")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Failure, "Constraint solving should fail due to non-boolean condition")
    }
    
    @Test
    fun `conditional with mismatched branch types should fail`() {
        // Test: if true then 42 else "hello" (should fail)
        val condition = LiteralBoolExpr(createBoolLocation(true))
        val thenBranch = LiteralIntExpr(createIntLocation(42))
        val elseBranch = LiteralStringExpr(createStringLocation("hello"))
        val ifExpr = IfExpr(condition, thenBranch, elseBranch, createLocation())
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(ifExpr)
        
        assertTrue(result.isSuccess(), "Constraint generation should succeed")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Failure, "Constraint solving should fail due to mismatched branch types")
    }
    
    @Test
    fun `conditional in function argument context`() {
        // Test: f(if condition then x else y)
        val fVar = VarExpr(createStringLocation("f"))
        val conditionVar = VarExpr(createStringLocation("condition"))
        val xVar = VarExpr(createStringLocation("x"))
        val yVar = VarExpr(createStringLocation("y"))
        
        val ifExpr = IfExpr(conditionVar, xVar, yVar, createLocation())
        val application = ApplicationExpr(fVar, listOf(ifExpr), createLocation())
        
        val fType = FunctionType(Types.Int, Types.String)
        val env = TypeEnvironment.empty()
            .bind("f", TypeScheme.monomorphic(fType))
            .bind("condition", TypeScheme.monomorphic(Types.Bool))
            .bind("x", TypeScheme.monomorphic(Types.Int))
            .bind("y", TypeScheme.monomorphic(Types.Int))
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(application)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for conditional in function argument")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.String, finalType, "Function application with conditional argument should work correctly")
    }
    
    @Test
    fun `conditional in let binding context`() {
        // Test: let result = if condition then x else y in result + 1
        val conditionVar = VarExpr(createStringLocation("condition"))
        val xVar = VarExpr(createStringLocation("x"))
        val yVar = VarExpr(createStringLocation("y"))
        val ifExpr = IfExpr(conditionVar, xVar, yVar, createLocation())
        
        val resultVar = VarExpr(createStringLocation("result"))
        val one = LiteralIntExpr(createIntLocation(1))
        val addition = BinaryOpExpr(resultVar, BinaryOp.Plus, one, createLocation())
        
        val letExpr = LetExpr(
            false,
            createStringLocation("result"),
            null, null, null,
            ifExpr,
            addition
        )
        
        val env = TypeEnvironment.empty()
            .bind("condition", TypeScheme.monomorphic(Types.Bool))
            .bind("x", TypeScheme.monomorphic(Types.Int))
            .bind("y", TypeScheme.monomorphic(Types.Int))
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(letExpr)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for conditional in let binding")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.Int, finalType, "Let with conditional should result in Int")
    }
    
    @Test
    fun `conditional with type variables in branches`() {
        // Test: if condition then x else y where x and y have unknown types
        val conditionVar = VarExpr(createStringLocation("condition"))
        val xVar = VarExpr(createStringLocation("x"))
        val yVar = VarExpr(createStringLocation("y"))
        val ifExpr = IfExpr(conditionVar, xVar, yVar, createLocation())
        
        val xType = TypeVariable.fresh()
        val yType = TypeVariable.fresh()
        val env = TypeEnvironment.empty()
            .bind("condition", TypeScheme.monomorphic(Types.Bool))
            .bind("x", TypeScheme.monomorphic(xType))
            .bind("y", TypeScheme.monomorphic(yType))
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(ifExpr)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for conditional with type variables")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val substitution = (solverResult as ConstraintSolverResult.Success).substitution
        
        // x and y should be unified to the same type
        val xResolved = substitution.apply(xType)
        val yResolved = substitution.apply(yType)
        assertEquals(xResolved, yResolved, "Branch types should be unified")
        
        val finalType = substitution.apply(result.type)
        assertEquals(xResolved, finalType, "Result type should match branch types")
    }
    
    @Test
    fun `deeply nested conditional expressions`() {
        // Test: if a then (if b then (if c then 1 else 2) else 3) else 4
        val aVar = VarExpr(createStringLocation("a"))
        val bVar = VarExpr(createStringLocation("b"))
        val cVar = VarExpr(createStringLocation("c"))
        val one = LiteralIntExpr(createIntLocation(1))
        val two = LiteralIntExpr(createIntLocation(2))
        val three = LiteralIntExpr(createIntLocation(3))
        val four = LiteralIntExpr(createIntLocation(4))
        
        val innermost = IfExpr(cVar, one, two, createLocation())
        val middle = IfExpr(bVar, innermost, three, createLocation())
        val outermost = IfExpr(aVar, middle, four, createLocation())
        
        val env = TypeEnvironment.empty()
            .bind("a", TypeScheme.monomorphic(Types.Bool))
            .bind("b", TypeScheme.monomorphic(Types.Bool))
            .bind("c", TypeScheme.monomorphic(Types.Bool))
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(outermost)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for deeply nested conditionals")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.Int, finalType, "Deeply nested conditionals should result in Int")
    }
    
    @Test
    fun `conditional with logical operations in condition`() {
        // Test: if (a && b) || c then x else y
        val aVar = VarExpr(createStringLocation("a"))
        val bVar = VarExpr(createStringLocation("b"))
        val cVar = VarExpr(createStringLocation("c"))
        val xVar = VarExpr(createStringLocation("x"))
        val yVar = VarExpr(createStringLocation("y"))
        
        val andExpr = BinaryOpExpr(aVar, BinaryOp.And, bVar, createLocation())
        val orExpr = BinaryOpExpr(andExpr, BinaryOp.Or, cVar, createLocation())
        val ifExpr = IfExpr(orExpr, xVar, yVar, createLocation())
        
        val env = TypeEnvironment.empty()
            .bind("a", TypeScheme.monomorphic(Types.Bool))
            .bind("b", TypeScheme.monomorphic(Types.Bool))
            .bind("c", TypeScheme.monomorphic(Types.Bool))
            .bind("x", TypeScheme.monomorphic(Types.String))
            .bind("y", TypeScheme.monomorphic(Types.String))
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(ifExpr)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for conditional with logical condition")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.String, finalType, "Conditional with logical condition should result in String")
    }
    
    @Test
    fun `conditional preserves source location information`() {
        // Test: Source location preservation in constraint generation
        val location = LocationCoordinate(9, 18, 27)
        val condition = LiteralBoolExpr(createBoolLocation(true))
        val thenBranch = LiteralIntExpr(createIntLocation(1))
        val elseBranch = LiteralIntExpr(createIntLocation(2))
        val ifExpr = IfExpr(condition, thenBranch, elseBranch, location)
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(ifExpr)
        
        assertTrue(result.isSuccess(), "Should successfully generate constraints")
        
        // Check that constraints preserve source location information
        val constraints = result.constraints.all()
        val hasLocationInfo = constraints.any { constraint ->
            constraint.sourceLocation != null &&
            (constraint.sourceLocation!!.line == 18 || constraint.sourceLocation!!.column == 27)
        }
        assertTrue(hasLocationInfo, "Should preserve source location information in constraints")
    }
    
    @Test
    fun `performance test with deeply nested conditionals`() {
        // Test: Deeply nested if-then-else chain
        val startTime = System.currentTimeMillis()
        
        var expr: Expr = LiteralIntExpr(createIntLocation(0))
        
        // Build nested conditionals: if c1 then (if c2 then ... else 0) else 0
        for (i in 1..10) {
            val condition = VarExpr(createStringLocation("c$i"))
            val value = LiteralIntExpr(createIntLocation(i))
            expr = IfExpr(condition, value, expr, createLocation())
        }
        
        var env = TypeEnvironment.empty()
        for (i in 1..10) {
            env = env.bind("c$i", TypeScheme.monomorphic(Types.Bool))
        }
        
        val generator = ConstraintGenerator(env)
        val result = generator.generateConstraints(expr)
        
        assertTrue(result.isSuccess(), "Should handle deeply nested conditionals")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Should solve deeply nested conditional constraints")
        
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        
        assertTrue(duration < 2000, "Should handle 10 nested conditionals within reasonable time (${duration}ms)")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.Int, finalType, "Deeply nested conditionals should preserve type")
    }
} 