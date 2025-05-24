package io.littlelanguages.minibendu.typesystem

import io.littlelanguages.minibendu.*
import io.littlelanguages.scanpiler.LocationCoordinate
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Comprehensive tests for type inference of function application expressions.
 * Task 35 of Phase 1 - Type Inference for Complex Expressions.
 * 
 * Tests cover:
 * - Application of functions to arguments: f(x)
 * - Application with too few/many arguments: f() or f(x, y, z)
 * - Application of polymorphic functions: id(42), id("hello")
 * - Error reporting for type mismatches: intFunc("string")
 * - Curried function applications: add(1)(2)
 * - Higher-order function applications: map(f, list)
 * - Nested function applications: f(g(h(x)))
 * - Function application with lambda expressions
 */
class TypeInferenceForFunctionApplicationTest {
    
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
    fun `simple function application with known types`() {
        // Test: f(42) where f: Int -> String
        val funcVar = VarExpr(createStringLocation("f"))
        val arg = LiteralIntExpr(createIntLocation(42))
        val application = ApplicationExpr(funcVar, listOf(arg), createLocation())
        
        val funcType = FunctionType(Types.Int, Types.String)
        val env = TypeEnvironment.empty().bind("f", TypeScheme.monomorphic(funcType))
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(application)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for simple function application")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.String, finalType, "Function application should result in String")
    }
    
    @Test
    fun `function application with type variables`() {
        // Test: f(x) where f and x have unknown types
        val funcVar = VarExpr(createStringLocation("f"))
        val argVar = VarExpr(createStringLocation("x"))
        val application = ApplicationExpr(funcVar, listOf(argVar), createLocation())
        
        val fType = TypeVariable.fresh()
        val xType = TypeVariable.fresh()
        val env = TypeEnvironment.empty()
            .bind("f", TypeScheme.monomorphic(fType))
            .bind("x", TypeScheme.monomorphic(xType))
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(application)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for application with type variables")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val substitution = (solverResult as ConstraintSolverResult.Success).substitution
        
        // f should be unified with (x_type -> result_type)
        val fResolved = substitution.apply(fType)
        assertTrue(fResolved is FunctionType, "f should be resolved to a function type")
        val funcType = fResolved as FunctionType
        
        // The domain of f should equal the type of x
        val xResolved = substitution.apply(xType)
        assertEquals(funcType.domain, xResolved, "Function domain should match argument type")
    }
    
    @Test
    fun `polymorphic function application - identity function`() {
        // Test: id(42) where id: ∀a. a -> a
        val idVar = VarExpr(createStringLocation("id"))
        val arg = LiteralIntExpr(createIntLocation(42))
        val application = ApplicationExpr(idVar, listOf(arg), createLocation())
        
        val polyVar = TypeVariable.fresh()
        val identityType = FunctionType(polyVar, polyVar)
        val polyScheme = TypeScheme(setOf(polyVar), identityType)
        val env = TypeEnvironment.empty().bind("id", polyScheme)
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(application)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for polymorphic function application")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.Int, finalType, "Identity applied to Int should result in Int")
    }
    
    @Test
    fun `curried function application`() {
        // Test: add(1)(2) where add: Int -> Int -> Int
        val addVar = VarExpr(createStringLocation("add"))
        val one = LiteralIntExpr(createIntLocation(1))
        val firstApp = ApplicationExpr(addVar, listOf(one), createLocation())
        
        val two = LiteralIntExpr(createIntLocation(2))
        val secondApp = ApplicationExpr(firstApp, listOf(two), createLocation())
        
        val addType = FunctionType(Types.Int, FunctionType(Types.Int, Types.Int))
        val env = TypeEnvironment.empty().bind("add", TypeScheme.monomorphic(addType))
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(secondApp)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for curried function application")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.Int, finalType, "Curried addition should result in Int")
    }
    
    @Test
    fun `multiple argument function application`() {
        // Test: f(x, y) treated as curried application f(x)(y)
        val funcVar = VarExpr(createStringLocation("f"))
        val xVar = VarExpr(createStringLocation("x"))
        val yVar = VarExpr(createStringLocation("y"))
        val application = ApplicationExpr(funcVar, listOf(xVar, yVar), createLocation())
        
        val funcType = FunctionType(Types.Int, FunctionType(Types.String, Types.Bool))
        val env = TypeEnvironment.empty()
            .bind("f", TypeScheme.monomorphic(funcType))
            .bind("x", TypeScheme.monomorphic(Types.Int))
            .bind("y", TypeScheme.monomorphic(Types.String))
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(application)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for multiple argument application")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.Bool, finalType, "Multiple argument application should result in Bool")
    }
    
    @Test
    fun `function application with type mismatch should fail`() {
        // Test: f("hello") where f: Int -> String (should fail)
        val funcVar = VarExpr(createStringLocation("f"))
        val arg = LiteralStringExpr(createStringLocation("hello"))
        val application = ApplicationExpr(funcVar, listOf(arg), createLocation())
        
        val funcType = FunctionType(Types.Int, Types.String)
        val env = TypeEnvironment.empty().bind("f", TypeScheme.monomorphic(funcType))
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(application)
        
        assertTrue(result.isSuccess(), "Constraint generation should succeed")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Failure, "Constraint solving should fail due to type mismatch")
    }
    
    @Test
    fun `applying non-function should fail`() {
        // Test: x(42) where x: Int (should fail)
        val xVar = VarExpr(createStringLocation("x"))
        val arg = LiteralIntExpr(createIntLocation(42))
        val application = ApplicationExpr(xVar, listOf(arg), createLocation())
        
        val env = TypeEnvironment.empty().bind("x", TypeScheme.monomorphic(Types.Int))
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(application)
        
        assertTrue(result.isSuccess(), "Constraint generation should succeed")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Failure, "Constraint solving should fail when applying non-function")
    }
    
    @Test
    fun `nested function application`() {
        // Test: f(g(h(42)))
        val fVar = VarExpr(createStringLocation("f"))
        val gVar = VarExpr(createStringLocation("g"))
        val hVar = VarExpr(createStringLocation("h"))
        val fortyTwo = LiteralIntExpr(createIntLocation(42))
        
        val hApplication = ApplicationExpr(hVar, listOf(fortyTwo), createLocation())
        val gApplication = ApplicationExpr(gVar, listOf(hApplication), createLocation())
        val fApplication = ApplicationExpr(fVar, listOf(gApplication), createLocation())
        
        val hType = FunctionType(Types.Int, Types.String)
        val gType = FunctionType(Types.String, Types.Bool)
        val fType = FunctionType(Types.Bool, Types.Int)
        
        val env = TypeEnvironment.empty()
            .bind("f", TypeScheme.monomorphic(fType))
            .bind("g", TypeScheme.monomorphic(gType))
            .bind("h", TypeScheme.monomorphic(hType))
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(fApplication)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for nested function application")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.Int, finalType, "Nested function application should result in Int")
    }
    
    @Test
    fun `application of lambda expression`() {
        // Test: (\x -> x + 1)(5)
        val xVar = VarExpr(createStringLocation("x"))
        val one = LiteralIntExpr(createIntLocation(1))
        val addition = BinaryOpExpr(xVar, BinaryOp.Plus, one, createLocation())
        val lambda = LambdaExpr(null, createStringLocation("x"), null, addition, createLocation())
        
        val five = LiteralIntExpr(createIntLocation(5))
        val application = ApplicationExpr(lambda, listOf(five), createLocation())
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(application)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for lambda application")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.Int, finalType, "Lambda application should result in Int")
    }
    
    @Test
    fun `higher-order function application`() {
        // Test: map(f, list) - simplified without full list types
        val mapVar = VarExpr(createStringLocation("map"))
        val fVar = VarExpr(createStringLocation("f"))
        val listVar = VarExpr(createStringLocation("list"))
        val application = ApplicationExpr(mapVar, listOf(fVar, listVar), createLocation())
        
        // map: (a -> b) -> [a] -> [b]
        // Using tuples as simple list representation
        val aVar = TypeVariable.fresh()
        val bVar = TypeVariable.fresh()
        val funcType = FunctionType(aVar, bVar)
        val listAType = TupleType(listOf(aVar))
        val listBType = TupleType(listOf(bVar))
        val mapType = FunctionType(funcType, FunctionType(listAType, listBType))
        
        val fType = FunctionType(Types.Int, Types.String)
        val listType = TupleType(listOf(Types.Int))
        
        val env = TypeEnvironment.empty()
            .bind("map", TypeScheme(setOf(aVar, bVar), mapType))
            .bind("f", TypeScheme.monomorphic(fType))
            .bind("list", TypeScheme.monomorphic(listType))
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(application)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for higher-order function application")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertTrue(finalType is TupleType, "Map result should be a tuple type")
        val tupleType = finalType as TupleType
        assertEquals(1, tupleType.elements.size, "Result should be single-element tuple")
        assertEquals(Types.String, tupleType.elements[0], "Result element should be String")
    }
    
    @Test
    fun `application with undefined function should fail`() {
        // Test: unknownFunc(42) (should fail)
        val unknownVar = VarExpr(createStringLocation("unknownFunc"))
        val arg = LiteralIntExpr(createIntLocation(42))
        val application = ApplicationExpr(unknownVar, listOf(arg), createLocation())
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(application)
        
        assertTrue(result.isFailure(), "Should fail when applying undefined function")
        val failure = result as ConstraintGenerationResult.Failure
        assertTrue(failure.error.contains("unknownFunc"), "Error should mention undefined function")
    }
    
    @Test
    fun `application with no arguments - nullary function`() {
        // Test: f() where f: () -> Int (Unit -> Int)
        val funcVar = VarExpr(createStringLocation("f"))
        val application = ApplicationExpr(funcVar, emptyList(), createLocation())
        
        val funcType = FunctionType(Types.Unit, Types.Int)
        val env = TypeEnvironment.empty().bind("f", TypeScheme.monomorphic(funcType))
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(application)
        
        assertTrue(result.isSuccess(), "Should handle nullary function application")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.Int, finalType, "Nullary function should return its result type")
    }
    
    @Test
    fun `application with partial application behavior`() {
        // Test: Partial application of a 3-argument function: f(x) where f: a -> b -> c -> d
        val funcVar = VarExpr(createStringLocation("f"))
        val arg = LiteralIntExpr(createIntLocation(1))
        val application = ApplicationExpr(funcVar, listOf(arg), createLocation())
        
        val funcType = FunctionType(Types.Int, FunctionType(Types.String, FunctionType(Types.Bool, Types.Unit)))
        val env = TypeEnvironment.empty().bind("f", TypeScheme.monomorphic(funcType))
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(application)
        
        assertTrue(result.isSuccess(), "Should handle partial application")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertTrue(finalType is FunctionType, "Partial application should result in function type")
        val partialFunc = finalType as FunctionType
        assertEquals(Types.String, partialFunc.domain, "Remaining function should expect String")
        assertTrue(partialFunc.codomain is FunctionType, "Should still be curried")
    }
    
    @Test
    fun `application with complex argument expressions`() {
        // Test: f(x + y * z)
        val funcVar = VarExpr(createStringLocation("f"))
        val xVar = VarExpr(createStringLocation("x"))
        val yVar = VarExpr(createStringLocation("y"))
        val zVar = VarExpr(createStringLocation("z"))
        
        val multiplication = BinaryOpExpr(yVar, BinaryOp.Star, zVar, createLocation())
        val addition = BinaryOpExpr(xVar, BinaryOp.Plus, multiplication, createLocation())
        val application = ApplicationExpr(funcVar, listOf(addition), createLocation())
        
        val funcType = FunctionType(Types.Int, Types.String)
        val env = TypeEnvironment.empty()
            .bind("f", TypeScheme.monomorphic(funcType))
            .bind("x", TypeScheme.monomorphic(Types.Int))
            .bind("y", TypeScheme.monomorphic(Types.Int))
            .bind("z", TypeScheme.monomorphic(Types.Int))
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(application)
        
        assertTrue(result.isSuccess(), "Should handle application with complex argument expressions")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.String, finalType, "Application with complex argument should work correctly")
    }
    
    @Test
    fun `application with conditional argument`() {
        // Test: f(if condition then x else y)
        val funcVar = VarExpr(createStringLocation("f"))
        val conditionVar = VarExpr(createStringLocation("condition"))
        val xVar = VarExpr(createStringLocation("x"))
        val yVar = VarExpr(createStringLocation("y"))
        
        val ifExpr = IfExpr(conditionVar, xVar, yVar, createLocation())
        val application = ApplicationExpr(funcVar, listOf(ifExpr), createLocation())
        
        val funcType = FunctionType(Types.Int, Types.String)
        val env = TypeEnvironment.empty()
            .bind("f", TypeScheme.monomorphic(funcType))
            .bind("condition", TypeScheme.monomorphic(Types.Bool))
            .bind("x", TypeScheme.monomorphic(Types.Int))
            .bind("y", TypeScheme.monomorphic(Types.Int))
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(application)
        
        assertTrue(result.isSuccess(), "Should handle application with conditional argument")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.String, finalType, "Application with conditional argument should work correctly")
    }
    
    @Test
    fun `application preserves source location information`() {
        // Test: Source location preservation in constraint generation
        val location = LocationCoordinate(7, 14, 21)
        val funcVar = VarExpr(createStringLocation("f"))
        val arg = LiteralIntExpr(createIntLocation(42))
        val application = ApplicationExpr(funcVar, listOf(arg), location)
        
        val funcType = FunctionType(Types.Int, Types.String)
        val env = TypeEnvironment.empty().bind("f", TypeScheme.monomorphic(funcType))
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(application)
        
        assertTrue(result.isSuccess(), "Should successfully generate constraints")
        
        // Check that constraints preserve source location information
        val constraints = result.constraints.all()
        val hasLocationInfo = constraints.any { constraint ->
            constraint.sourceLocation != null &&
            (constraint.sourceLocation!!.line == 14 || constraint.sourceLocation!!.column == 21)
        }
        assertTrue(hasLocationInfo, "Should preserve source location information in constraints")
    }
    
    @Test
    fun `performance test with deeply nested applications`() {
        // Test: f(f(f(f(...f(x)...))))
        val startTime = System.currentTimeMillis()
        
        val xVar = VarExpr(createStringLocation("x"))
        var expr: Expr = xVar
        
        // Build nested applications: f(f(f(...f(x)...)))
        for (i in 1..15) {
            val funcVar = VarExpr(createStringLocation("f"))
            expr = ApplicationExpr(funcVar, listOf(expr), createLocation())
        }
        
        val funcType = FunctionType(Types.Int, Types.Int)
        val env = TypeEnvironment.empty()
            .bind("f", TypeScheme.monomorphic(funcType))
            .bind("x", TypeScheme.monomorphic(Types.Int))
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(expr)
        
        assertTrue(result.isSuccess(), "Should handle deeply nested applications")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Should solve deeply nested constraints")
        
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        
        assertTrue(duration < 2000, "Should handle 15 nested applications within reasonable time (${duration}ms)")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.Int, finalType, "Deeply nested applications should preserve type")
    }
    
    @Test
    fun `application with polymorphic function in different contexts`() {
        // Test: id(42) and id("hello") with same polymorphic id function
        val idVar1 = VarExpr(createStringLocation("id"))
        val idVar2 = VarExpr(createStringLocation("id"))
        val intArg = LiteralIntExpr(createIntLocation(42))
        val stringArg = LiteralStringExpr(createStringLocation("hello"))
        
        val intApp = ApplicationExpr(idVar1, listOf(intArg), createLocation())
        val stringApp = ApplicationExpr(idVar2, listOf(stringArg), createLocation())
        
        // Create a tuple containing both applications to test in same context
        val tupleExpr = TupleExpr(listOf(intApp, stringApp), createLocation())
        
        val polyVar = TypeVariable.fresh()
        val identityType = FunctionType(polyVar, polyVar)
        val polyScheme = TypeScheme(setOf(polyVar), identityType)
        val env = TypeEnvironment.empty().bind("id", polyScheme)
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(tupleExpr)
        
        assertTrue(result.isSuccess(), "Should handle polymorphic function in different contexts")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertTrue(finalType is TupleType, "Result should be a tuple")
        val tupleType = finalType as TupleType
        assertEquals(2, tupleType.elements.size, "Should have 2 elements")
        assertEquals(Types.Int, tupleType.elements[0], "First element should be Int")
        assertEquals(Types.String, tupleType.elements[1], "Second element should be String")
    }
} 