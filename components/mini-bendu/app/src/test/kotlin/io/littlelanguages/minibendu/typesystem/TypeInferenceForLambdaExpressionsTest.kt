package io.littlelanguages.minibendu.typesystem

import io.littlelanguages.minibendu.*
import io.littlelanguages.scanpiler.LocationCoordinate
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Comprehensive tests for type inference of lambda expressions.
 * Task 33 of Phase 1 - Type Inference for Complex Expressions.
 * 
 * Tests cover:
 * - Simple lambda functions: \x -> x + 1
 * - Lambdas with explicit parameter types: \(x: Int) -> x * 2
 * - Lambdas with complex body expressions: \x -> if x > 0 then x else -x
 * - Higher-order functions: \f -> \x -> f(f(x))
 * - Lambda type inference in various contexts
 * - Error handling for type mismatches in lambda bodies
 * - Nested lambda expressions and currying
 * - Lambda functions with multiple operations
 */
class TypeInferenceForLambdaExpressionsTest {
    
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
    fun `simple identity lambda`() {
        // Test: \x -> x
        val xVar = VarExpr(createStringLocation("x"))
        val lambda = LambdaExpr(
            null, // no type params
            createStringLocation("x"),
            null, // no explicit param type
            xVar,
            createLocation()
        )
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(lambda)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for identity lambda")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertTrue(finalType is FunctionType, "Identity lambda should be a function type")
        val funcType = finalType as FunctionType
        assertEquals(funcType.domain, funcType.codomain, "Identity function should have same input and output types")
    }
    
    @Test
    fun `lambda with arithmetic operation`() {
        // Test: \x -> x + 1
        val xVar = VarExpr(createStringLocation("x"))
        val one = LiteralIntExpr(createIntLocation(1))
        val addition = BinaryOpExpr(xVar, BinaryOp.Plus, one, createLocation())
        
        val lambda = LambdaExpr(
            null,
            createStringLocation("x"),
            null,
            addition,
            createLocation()
        )
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(lambda)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for arithmetic lambda")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertTrue(finalType is FunctionType, "Lambda should be a function type")
        val funcType = finalType as FunctionType
        assertEquals(Types.Int, funcType.domain, "Parameter should be Int")
        assertEquals(Types.Int, funcType.codomain, "Result should be Int")
    }
    
    @Test
    fun `lambda with explicit parameter type`() {
        // Test: \(x: Int) -> x * 2
        val xVar = VarExpr(createStringLocation("x"))
        val two = LiteralIntExpr(createIntLocation(2))
        val multiplication = BinaryOpExpr(xVar, BinaryOp.Star, two, createLocation())
        val paramType = BaseTypeExpr(createStringLocation("Int"), null)
        
        val lambda = LambdaExpr(
            null,
            createStringLocation("x"),
            paramType, // explicit param type
            multiplication,
            createLocation()
        )
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(lambda)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for explicitly typed lambda")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertTrue(finalType is FunctionType, "Lambda should be a function type")
        val funcType = finalType as FunctionType
        assertEquals(Types.Int, funcType.domain, "Parameter should be Int")
        assertEquals(Types.Int, funcType.codomain, "Result should be Int")
    }
    
    @Test
    fun `lambda with boolean logic`() {
        // Test: \flag -> flag && true
        val flagVar = VarExpr(createStringLocation("flag"))
        val trueLit = LiteralBoolExpr(createBoolLocation(true))
        val logicalAnd = BinaryOpExpr(flagVar, BinaryOp.And, trueLit, createLocation())
        
        val lambda = LambdaExpr(
            null,
            createStringLocation("flag"),
            null,
            logicalAnd,
            createLocation()
        )
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(lambda)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for boolean lambda")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertTrue(finalType is FunctionType, "Lambda should be a function type")
        val funcType = finalType as FunctionType
        assertEquals(Types.Bool, funcType.domain, "Parameter should be Bool")
        assertEquals(Types.Bool, funcType.codomain, "Result should be Bool")
    }
    
    @Test
    fun `lambda with conditional expression`() {
        // Test: \x -> if x == 0 then 1 else x
        val xVar1 = VarExpr(createStringLocation("x"))
        val zero = LiteralIntExpr(createIntLocation(0))
        val condition = BinaryOpExpr(xVar1, BinaryOp.EqualEqual, zero, createLocation())
        
        val one = LiteralIntExpr(createIntLocation(1))
        val xVar2 = VarExpr(createStringLocation("x"))
        val ifExpr = IfExpr(condition, one, xVar2, createLocation())
        
        val lambda = LambdaExpr(
            null,
            createStringLocation("x"),
            null,
            ifExpr,
            createLocation()
        )
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(lambda)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for conditional lambda")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertTrue(finalType is FunctionType, "Lambda should be a function type")
        val funcType = finalType as FunctionType
        assertEquals(Types.Int, funcType.domain, "Parameter should be Int")
        assertEquals(Types.Int, funcType.codomain, "Result should be Int")
    }
    
    @Test
    fun `nested lambda expressions - currying`() {
        // Test: \x -> \y -> x + y (curried addition)
        val xVar = VarExpr(createStringLocation("x"))
        val yVar = VarExpr(createStringLocation("y"))
        val addition = BinaryOpExpr(xVar, BinaryOp.Plus, yVar, createLocation())
        
        val innerLambda = LambdaExpr(
            null,
            createStringLocation("y"),
            null,
            addition,
            createLocation()
        )
        
        val outerLambda = LambdaExpr(
            null,
            createStringLocation("x"),
            null,
            innerLambda,
            createLocation()
        )
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(outerLambda)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for curried lambda")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertTrue(finalType is FunctionType, "Outer lambda should be a function type")
        val outerFunc = finalType as FunctionType
        assertEquals(Types.Int, outerFunc.domain, "First parameter should be Int")
        
        assertTrue(outerFunc.codomain is FunctionType, "Result should be another function type")
        val innerFunc = outerFunc.codomain as FunctionType
        assertEquals(Types.Int, innerFunc.domain, "Second parameter should be Int")
        assertEquals(Types.Int, innerFunc.codomain, "Final result should be Int")
    }
    
    @Test
    fun `lambda with complex arithmetic expression`() {
        // Test: \x -> (x + 1) * (x - 1)
        val xVar1 = VarExpr(createStringLocation("x"))
        val xVar2 = VarExpr(createStringLocation("x"))
        val one1 = LiteralIntExpr(createIntLocation(1))
        val one2 = LiteralIntExpr(createIntLocation(1))
        
        val addition = BinaryOpExpr(xVar1, BinaryOp.Plus, one1, createLocation())
        val subtraction = BinaryOpExpr(xVar2, BinaryOp.Minus, one2, createLocation())
        val multiplication = BinaryOpExpr(addition, BinaryOp.Star, subtraction, createLocation())
        
        val lambda = LambdaExpr(
            null,
            createStringLocation("x"),
            null,
            multiplication,
            createLocation()
        )
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(lambda)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for complex arithmetic lambda")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertTrue(finalType is FunctionType, "Lambda should be a function type")
        val funcType = finalType as FunctionType
        assertEquals(Types.Int, funcType.domain, "Parameter should be Int")
        assertEquals(Types.Int, funcType.codomain, "Result should be Int")
    }
    
    @Test
    fun `lambda with string operations`() {
        // Test: \s -> s == "test"
        val sVar = VarExpr(createStringLocation("s"))
        val testStr = LiteralStringExpr(createStringLocation("test"))
        val comparison = BinaryOpExpr(sVar, BinaryOp.EqualEqual, testStr, createLocation())
        
        val lambda = LambdaExpr(
            null,
            createStringLocation("s"),
            null,
            comparison,
            createLocation()
        )
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(lambda)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for string lambda")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertTrue(finalType is FunctionType, "Lambda should be a function type")
        val funcType = finalType as FunctionType
        assertEquals(Types.String, funcType.domain, "Parameter should be String")
        assertEquals(Types.Bool, funcType.codomain, "Result should be Bool")
    }
    
    @Test
    fun `higher-order lambda - function composition`() {
        // Test: \f -> \g -> \x -> f(g(x))
        val fVar = VarExpr(createStringLocation("f"))
        val gVar = VarExpr(createStringLocation("g"))
        val xVar = VarExpr(createStringLocation("x"))
        
        val gApplication = ApplicationExpr(gVar, listOf(xVar), createLocation())
        val fApplication = ApplicationExpr(fVar, listOf(gApplication), createLocation())
        
        val xLambda = LambdaExpr(null, createStringLocation("x"), null, fApplication, createLocation())
        val gLambda = LambdaExpr(null, createStringLocation("g"), null, xLambda, createLocation())
        val fLambda = LambdaExpr(null, createStringLocation("f"), null, gLambda, createLocation())
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(fLambda)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for higher-order lambda")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertTrue(finalType is FunctionType, "Should be a function type")
        
        // The type should be: (a -> b) -> (c -> a) -> c -> b
        // This is a complex polymorphic type, so we'll just verify it's a function
        val outerFunc = finalType as FunctionType
        assertTrue(outerFunc.domain is FunctionType, "First parameter should be a function")
        assertTrue(outerFunc.codomain is FunctionType, "Result should be a function")
    }
    
    @Test
    fun `lambda with type annotation mismatch should fail`() {
        // Test: \(x: Bool) -> x + 1 (should fail - can't add to Bool)
        val xVar = VarExpr(createStringLocation("x"))
        val one = LiteralIntExpr(createIntLocation(1))
        val addition = BinaryOpExpr(xVar, BinaryOp.Plus, one, createLocation())
        val paramType = BaseTypeExpr(createStringLocation("Bool"), null)
        
        val lambda = LambdaExpr(
            null,
            createStringLocation("x"),
            paramType, // Bool type annotation
            addition, // but trying to do arithmetic
            createLocation()
        )
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(lambda)
        
        assertTrue(result.isSuccess(), "Constraint generation should succeed")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Failure, "Constraint solving should fail due to type mismatch")
    }
    
    @Test
    fun `lambda in let binding context`() {
        // Test: \x -> x in the context of a let binding
        val xVar = VarExpr(createStringLocation("x"))
        val lambda = LambdaExpr(null, createStringLocation("x"), null, xVar, createLocation())
        
        val fVar = VarExpr(createStringLocation("f"))
        val five = LiteralIntExpr(createIntLocation(5))
        val application = ApplicationExpr(fVar, listOf(five), createLocation())
        
        val letExpr = LetExpr(
            false,
            createStringLocation("f"),
            null, null, null,
            lambda,
            application
        )
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(letExpr)
        
        assertTrue(result.isSuccess(), "Should successfully infer lambda in let context")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.Int, finalType, "Lambda application should result in Int")
    }
    
    @Test
    fun `lambda with multiple parameter references`() {
        // Test: \x -> x + x + x (parameter used multiple times)
        val xVar1 = VarExpr(createStringLocation("x"))
        val xVar2 = VarExpr(createStringLocation("x"))
        val xVar3 = VarExpr(createStringLocation("x"))
        
        val addition1 = BinaryOpExpr(xVar1, BinaryOp.Plus, xVar2, createLocation())
        val addition2 = BinaryOpExpr(addition1, BinaryOp.Plus, xVar3, createLocation())
        
        val lambda = LambdaExpr(
            null,
            createStringLocation("x"),
            null,
            addition2,
            createLocation()
        )
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(lambda)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for multiple parameter usage")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertTrue(finalType is FunctionType, "Lambda should be a function type")
        val funcType = finalType as FunctionType
        assertEquals(Types.Int, funcType.domain, "Parameter should be Int")
        assertEquals(Types.Int, funcType.codomain, "Result should be Int")
    }
    
    @Test
    fun `lambda capturing external variables should fail`() {
        // Test: \x -> x + y (where y is not defined)
        val xVar = VarExpr(createStringLocation("x"))
        val yVar = VarExpr(createStringLocation("y")) // undefined variable
        val addition = BinaryOpExpr(xVar, BinaryOp.Plus, yVar, createLocation())
        
        val lambda = LambdaExpr(
            null,
            createStringLocation("x"),
            null,
            addition,
            createLocation()
        )
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(lambda)
        
        assertTrue(result.isFailure(), "Should fail when lambda references undefined variable")
        val failure = result as ConstraintGenerationResult.Failure
        assertTrue(failure.error.contains("y"), "Error should mention undefined variable y")
    }
    
    @Test
    fun `lambda with tuple construction`() {
        // Test: \x -> (x, x + 1)
        val xVar1 = VarExpr(createStringLocation("x"))
        val xVar2 = VarExpr(createStringLocation("x"))
        val one = LiteralIntExpr(createIntLocation(1))
        val addition = BinaryOpExpr(xVar2, BinaryOp.Plus, one, createLocation())
        val tuple = TupleExpr(listOf(xVar1, addition), createLocation())
        
        val lambda = LambdaExpr(
            null,
            createStringLocation("x"),
            null,
            tuple,
            createLocation()
        )
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(lambda)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for tuple-constructing lambda")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertTrue(finalType is FunctionType, "Lambda should be a function type")
        val funcType = finalType as FunctionType
        assertEquals(Types.Int, funcType.domain, "Parameter should be Int")
        assertTrue(funcType.codomain is TupleType, "Result should be a tuple type")
        val tupleType = funcType.codomain as TupleType
        assertEquals(2, tupleType.elements.size, "Tuple should have 2 elements")
        assertEquals(Types.Int, tupleType.elements[0], "First element should be Int")
        assertEquals(Types.Int, tupleType.elements[1], "Second element should be Int")
    }
    
    @Test
    fun `performance test with deeply nested lambda body`() {
        // Test: \x -> x + 1 + 1 + 1 + ... (many operations)
        val startTime = System.currentTimeMillis()
        
        val xVar = VarExpr(createStringLocation("x"))
        var expr: Expr = xVar
        
        // Build nested additions: x + 1 + 1 + 1 + ...
        for (i in 1..20) {
            val one = LiteralIntExpr(createIntLocation(1))
            expr = BinaryOpExpr(expr, BinaryOp.Plus, one, createLocation())
        }
        
        val lambda = LambdaExpr(
            null,
            createStringLocation("x"),
            null,
            expr,
            createLocation()
        )
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(lambda)
        
        assertTrue(result.isSuccess(), "Should handle complex lambda body")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Should solve complex lambda constraints")
        
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        
        assertTrue(duration < 2000, "Should handle complex lambda within reasonable time (${duration}ms)")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertTrue(finalType is FunctionType, "Lambda should be a function type")
        val funcType = finalType as FunctionType
        assertEquals(Types.Int, funcType.domain, "Parameter should be Int")
        assertEquals(Types.Int, funcType.codomain, "Result should be Int")
    }
    
    @Test
    fun `lambda preserves source location information`() {
        // Test: Source location preservation in constraint generation
        val location = LocationCoordinate(5, 10, 15)
        val xVar = VarExpr(createStringLocation("x"))
        val one = LiteralIntExpr(createIntLocation(1))
        val addition = BinaryOpExpr(xVar, BinaryOp.Plus, one, createLocation())
        
        val lambda = LambdaExpr(
            null,
            StringLocation("x", location),
            null,
            addition,
            location
        )
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(lambda)
        
        assertTrue(result.isSuccess(), "Should successfully generate constraints")
        
        // Check that constraints preserve source location information
        val constraints = result.constraints.all()
        val hasLocationInfo = constraints.any { constraint ->
            constraint.sourceLocation != null &&
            (constraint.sourceLocation!!.line == 10 || constraint.sourceLocation!!.column == 15)
        }
        assertTrue(hasLocationInfo, "Should preserve source location information in constraints")
    }
} 