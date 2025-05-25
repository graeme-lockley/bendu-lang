package io.littlelanguages.minibendu.typesystem

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import io.littlelanguages.minibendu.*
import io.littlelanguages.scanpiler.LocationCoordinate

/**
 * Integration tests for type checking complete AST nodes.
 * 
 * These tests verify that the type system works correctly when
 * integrated with complete programs and complex AST structures.
 * 
 * Tests cover:
 * - Type checking complete AST nodes
 * - Type inference on complete programs
 * - Error reporting with source locations
 * - Incremental type checking
 */
class TypeCheckerIntegrationTest {

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

    // ===== COMPLETE PROGRAM TYPE CHECKING TESTS =====

    @Test
    fun `type check simple arithmetic program`() {
        // let result = 42 + 17 * 3
        val multiplicationExpr = BinaryOpExpr(
            LiteralIntExpr(createIntLocation(17)),
            BinaryOp.Star,
            LiteralIntExpr(createIntLocation(3)),
            createLocation()
        )
        
        val additionExpr = BinaryOpExpr(
            LiteralIntExpr(createIntLocation(42)),
            BinaryOp.Plus,
            multiplicationExpr,
            createLocation()
        )
        
        val letExpr = LetExpr(
            false, // recursive
            createStringLocation("result"),
            null, // typeParams
            null, // parameters
            null, // typeAnnotation
            additionExpr,
            null // No body, just a binding
        )
        
        val env = TypeEnvironment.empty()
        val generator = ConstraintGenerator(env)
        val result = generator.generateConstraints(letExpr)
        
        assertTrue(result.isSuccess(), "Simple arithmetic program should type check successfully")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.Int, finalType, "Result should be Int type")
    }

    @Test
    fun `type check function definition and application`() {
        // let add = fun(x: Int) -> fun(y: Int) -> x + y in add(5)(3)
        val xVar = VarExpr(createStringLocation("x"))
        val yVar = VarExpr(createStringLocation("y"))
        
        val additionBody = BinaryOpExpr(
            xVar,
            BinaryOp.Plus,
            yVar,
            createLocation()
        )
        
        val innerLambda = LambdaExpr(
            null, // typeParams
            createStringLocation("y"),
            BaseTypeExpr(createStringLocation("Int"), null),
            additionBody,
            createLocation()
        )
        
        val outerLambda = LambdaExpr(
            null, // typeParams
            createStringLocation("x"),
            BaseTypeExpr(createStringLocation("Int"), null),
            innerLambda,
            createLocation()
        )
        
        val addVar = VarExpr(createStringLocation("add"))
        val firstApp = ApplicationExpr(
            addVar,
            listOf(LiteralIntExpr(createIntLocation(5))),
            createLocation()
        )
        
        val secondApp = ApplicationExpr(
            firstApp,
            listOf(LiteralIntExpr(createIntLocation(3))),
            createLocation()
        )
        
        val letExpr = LetExpr(
            false, // recursive
            createStringLocation("add"),
            null, // typeParams
            null, // parameters
            null, // typeAnnotation
            outerLambda,
            secondApp
        )
        
        val env = TypeEnvironment.empty()
        val generator = ConstraintGenerator(env)
        val result = generator.generateConstraints(letExpr)
        
        assertTrue(result.isSuccess(), "Function definition and application should type check")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.Int, finalType, "Final result should be Int")
    }

    @Test
    fun `type check recursive function`() {
        // let rec factorial = fun(n: Int) -> n * 42 (simplified)
        val nVar = VarExpr(createStringLocation("n"))
        
        val multiplication = BinaryOpExpr(
            nVar,
            BinaryOp.Star,
            LiteralIntExpr(createIntLocation(42)),
            createLocation()
        )
        
        val lambda = LambdaExpr(
            null, // typeParams
            createStringLocation("n"),
            BaseTypeExpr(createStringLocation("Int"), null),
            multiplication,
            createLocation()
        )
        
        val recursiveLet = LetExpr(
            true, // recursive
            createStringLocation("factorial"),
            null, // typeParams
            null, // parameters
            null, // typeAnnotation
            lambda,
            null // no body for this test
        )
        
        val env = TypeEnvironment.empty()
        val generator = ConstraintGenerator(env)
        val result = generator.generateConstraints(recursiveLet)
        
        assertTrue(result.isSuccess(), "Recursive function should type check")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
    }

    @Test
    fun `type check record operations`() {
        // let person = {name: "Alice", age: 30} in person.name
        val recordExpr = RecordExpr(
            listOf(
                FieldExpr(createStringLocation("name"), LiteralStringExpr(createStringLocation("Alice"))),
                FieldExpr(createStringLocation("age"), LiteralIntExpr(createIntLocation(30)))
            ),
            createLocation()
        )
        
        val personVar = VarExpr(createStringLocation("person"))
        val projection = ProjectionExpr(
            personVar,
            createStringLocation("name"),
            createLocation()
        )
        
        val letExpr = LetExpr(
            false, // recursive
            createStringLocation("person"),
            null, // typeParams
            null, // parameters
            null, // typeAnnotation
            recordExpr,
            projection
        )
        
        val env = TypeEnvironment.empty()
        val generator = ConstraintGenerator(env)
        val result = generator.generateConstraints(letExpr)
        
        assertTrue(result.isSuccess(), "Record operations should type check")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.String, finalType, "Projection result should be String")
    }

    @Test
    fun `type check pattern matching with records`() {
        // match {x: 5, y: "hello"} with
        // | {x: n, y: s} -> n + 1
        val recordExpr = RecordExpr(
            listOf(
                FieldExpr(createStringLocation("x"), LiteralIntExpr(createIntLocation(5))),
                FieldExpr(createStringLocation("y"), LiteralStringExpr(createStringLocation("hello")))
            ), createLocation()
        )
        
        val pattern = RecordPattern(listOf(
                FieldPattern(createStringLocation("x"), VarPattern(createStringLocation("n"))),
                FieldPattern(createStringLocation("y"), VarPattern(createStringLocation("s")))
            ),
            createLocation()
        )
        
        val nVar = VarExpr(createStringLocation("n"))
        val body = BinaryOpExpr(
            nVar,
            BinaryOp.Plus,
            LiteralIntExpr(createIntLocation(1)),
            createLocation()
        )
        
        val matchCase = MatchCase(pattern, body)
        val matchExpr = MatchExpr(recordExpr, listOf(matchCase), createLocation())
        
        val env = TypeEnvironment.empty()
        val generator = ConstraintGenerator(env)
        val result = generator.generateConstraints(matchExpr)
        
        assertTrue(result.isSuccess(), "Pattern matching with records should type check")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.Int, finalType, "Match result should be Int")
    }

    @Test
    fun `type check tuple operations`() {
        // let coords = (10, 20, "point") in match coords with (x, y, name) -> x + y
        val tupleExpr = TupleExpr(
            listOf(
                LiteralIntExpr(createIntLocation(10)),
                LiteralIntExpr(createIntLocation(20)), LiteralStringExpr(createStringLocation("point"))
            ), createLocation()
        )
        
        val coordsVar = VarExpr(createStringLocation("coords"))
        
        val pattern = TuplePattern(
            listOf(
                VarPattern(createStringLocation("x")), VarPattern(createStringLocation("y")),
                VarPattern(createStringLocation("name"))
            ),
            createLocation()
        )
        
        val xVar = VarExpr(createStringLocation("x"))
        val yVar = VarExpr(createStringLocation("y"))
        val body = BinaryOpExpr(xVar, BinaryOp.Plus, yVar, createLocation())
        
        val matchCase = MatchCase(pattern, body)
        val matchExpr = MatchExpr(coordsVar, listOf(matchCase), createLocation())
        
        val letExpr = LetExpr(
            false, // recursive
            createStringLocation("coords"),
            null, // typeParams
            null, // parameters
            null, // typeAnnotation
            tupleExpr,
            matchExpr
        )
        
        val env = TypeEnvironment.empty()
        val generator = ConstraintGenerator(env)
        val result = generator.generateConstraints(letExpr)
        
        assertTrue(result.isSuccess(), "Tuple operations should type check")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.Int, finalType, "Tuple match result should be Int")
    }

    // ===== TYPE INFERENCE ON COMPLETE PROGRAMS TESTS =====

    @Test
    fun `infer types in polymorphic function`() {
        // let identity = fun(x) -> x
        val lambda = LambdaExpr(
            null, // typeParams
            createStringLocation("x"),
            null, // No type annotation - should be inferred
            VarExpr(createStringLocation("x")),
            createLocation()
        )
        
        val letExpr = LetExpr(
            false, // recursive
            createStringLocation("identity"),
            null, // typeParams
            null, // parameters
            null, // typeAnnotation
            lambda,
            null // no body for this test
        )
        
        val env = TypeEnvironment.empty()
        val generator = ConstraintGenerator(env)
        val result = generator.generateConstraints(letExpr)
        
        assertTrue(result.isSuccess(), "Polymorphic function should type check")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        
        // Should infer a function type α -> α
        assertTrue(finalType is FunctionType, "Identity should have function type")
        val funcType = finalType as FunctionType
        assertEquals(funcType.domain, funcType.codomain, "Domain and codomain should be equal (polymorphic)")
    }

    @Test
    fun `infer types in complex expression`() {
        // let f = fun(x) -> fun(y) -> if x then y + 1 else y - 1
        val xVar = VarExpr(createStringLocation("x"))
        val yVar = VarExpr(createStringLocation("y"))
        
        val yPlus1 = BinaryOpExpr(yVar, BinaryOp.Plus, LiteralIntExpr(createIntLocation(1)), createLocation())
        val yMinus1 = BinaryOpExpr(yVar, BinaryOp.Minus, LiteralIntExpr(createIntLocation(1)), createLocation())
        
        val ifExpr = IfExpr(xVar, yPlus1, yMinus1, createLocation())
        
        val innerLambda = LambdaExpr(
            null, // typeParams
            createStringLocation("y"),
            null, // Type should be inferred as Int
            ifExpr,
            createLocation()
        )
        
        val outerLambda = LambdaExpr(
            null, // typeParams
            createStringLocation("x"),
            null, // Type should be inferred as Bool
            innerLambda,
            createLocation()
        )
        
        val letExpr = LetExpr(
            false, // recursive
            createStringLocation("f"),
            null, // typeParams
            null, // parameters
            null, // typeAnnotation
            outerLambda,
            null // no body for this test
        )
        
        val env = TypeEnvironment.empty()
        val generator = ConstraintGenerator(env)
        val result = generator.generateConstraints(letExpr)
        
        assertTrue(result.isSuccess(), "Complex expression should type check")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        
        // Should infer Bool -> Int -> Int
        assertTrue(finalType is FunctionType, "Should have function type")
        val outerFunc = finalType as FunctionType
        assertEquals(Types.Bool, outerFunc.domain, "First parameter should be Bool")
        
        assertTrue(outerFunc.codomain is FunctionType, "Should return function")
        val innerFunc = outerFunc.codomain as FunctionType
        assertEquals(Types.Int, innerFunc.domain, "Second parameter should be Int")
        assertEquals(Types.Int, innerFunc.codomain, "Result should be Int")
    }

    @Test
    fun `infer record types with row polymorphism`() {
        // let addAge = fun(person) -> {age: 25, ...person}
        val personVar = VarExpr(createStringLocation("person"))
        
        val recordExpr = RecordExpr(
            listOf(
                FieldExpr(createStringLocation("age"), LiteralIntExpr(createIntLocation(25))),
                SpreadExpr(personVar)
            ),
            createLocation()
        )
        
        val lambda = LambdaExpr(
            null, // typeParams
            createStringLocation("person"),
            null, // Type should be inferred with row variable
            recordExpr,
            createLocation()
        )
        
        val letExpr = LetExpr(
            false, // recursive
            createStringLocation("addAge"),
            null, // typeParams
            null, // parameters
            null, // typeAnnotation
            lambda,
            null // no body for this test
        )
        
        val env = TypeEnvironment.empty()
        val generator = ConstraintGenerator(env)
        val result = generator.generateConstraints(letExpr)
        
        assertTrue(result.isSuccess(), "Row polymorphic function should type check")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
    }

    // ===== ERROR REPORTING WITH SOURCE LOCATIONS TESTS =====

    @Test
    fun `report type mismatch with source location`() {
        // 5 + "hello" - should fail with type error at specific location
        val badExpr = BinaryOpExpr(
            LiteralIntExpr(createIntLocation(5)),
            BinaryOp.Plus,
            LiteralStringExpr(createStringLocation("hello")),
            LocationCoordinate(42, 10, 15) // Specific location for error reporting
        )
        
        val env = TypeEnvironment.empty()
        val generator = ConstraintGenerator(env)
        val result = generator.generateConstraints(badExpr)
        
        assertTrue(result.isSuccess(), "Constraint generation should succeed even with type error")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        
        assertTrue(solverResult is ConstraintSolverResult.Failure, "Should fail with type error")
        val error = (solverResult as ConstraintSolverResult.Failure).error
        
        // Check that error contains source location information
        assertTrue(error.contains("10") || error.contains("15"), 
            "Error should contain source location information: $error")
    }

    @Test
    fun `report undefined variable with source location`() {
        // unknownVar + 5 - should fail with undefined variable error
        val undefinedVarExpr = BinaryOpExpr(
            VarExpr(StringLocation("unknownVar", LocationCoordinate(15, 5, 8))),
            BinaryOp.Plus,
            LiteralIntExpr(createIntLocation(5)),
            createLocation()
        )
        
        val env = TypeEnvironment.empty()
        val generator = ConstraintGenerator(env)
        val result = generator.generateConstraints(undefinedVarExpr)
        
        assertTrue(result.isFailure(), "Should fail with undefined variable error")
        val error = (result as ConstraintGenerationResult.Failure).error
        
        assertTrue(error.contains("unknownVar"), "Error should mention undefined variable")
        assertTrue(error.contains("Undefined"), "Error should indicate variable is undefined")
    }

    @Test
    fun `report pattern match error with source location`() {
        // match 42 with {x: n} -> n - should fail because Int cannot match record pattern
        val pattern = RecordPattern(listOf(FieldPattern(createStringLocation("x"), VarPattern(createStringLocation("n")))),
            LocationCoordinate(20, 3, 7)
        )
        
        val body = VarExpr(createStringLocation("n"))
        val matchCase = MatchCase(pattern, body)
        
        val matchExpr = MatchExpr(
            LiteralIntExpr(createIntLocation(42)),
            listOf(matchCase),
            createLocation()
        )
        
        val env = TypeEnvironment.empty()
        val generator = ConstraintGenerator(env)
        val result = generator.generateConstraints(matchExpr)
        
        assertTrue(result.isSuccess(), "Constraint generation should succeed")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        
        assertTrue(solverResult is ConstraintSolverResult.Failure, "Should fail with pattern match error")
    }

    // ===== INCREMENTAL TYPE CHECKING TESTS =====

    @Test
    fun `incremental type checking with environment updates`() {
        // Simulate incremental checking by building up environment step by step
        
        // Step 1: Define a variable
        val env1 = TypeEnvironment.empty()
            .bind("x", TypeScheme.monomorphic(Types.Int))
        
        // Step 2: Use the variable
        val expr1 = BinaryOpExpr(
            VarExpr(createStringLocation("x")),
            BinaryOp.Plus,
            LiteralIntExpr(createIntLocation(10)),
            createLocation()
        )
        
        val generator1 = ConstraintGenerator(env1)
        val result1 = generator1.generateConstraints(expr1)
        
        assertTrue(result1.isSuccess(), "First incremental step should succeed")
        
        // Step 3: Add another variable and check again
        val env2 = env1.bind("y", TypeScheme.monomorphic(Types.String))
        
        val expr2 = TupleExpr(
            listOf(
                VarExpr(createStringLocation("x")),
                VarExpr(createStringLocation("y"))
            ),
            createLocation()
        )
        
        val generator2 = ConstraintGenerator(env2)
        val result2 = generator2.generateConstraints(expr2)
        
        assertTrue(result2.isSuccess(), "Second incremental step should succeed")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result2.constraints)
        
        assertTrue(solverResult is ConstraintSolverResult.Success, "Incremental type checking should succeed")
    }

    @Test
    fun `incremental type checking with constraint accumulation`() {
        // Test that constraints can be accumulated incrementally
        
        val env = TypeEnvironment.empty()
        val generator = ConstraintGenerator(env)
        
        // Generate constraints for multiple expressions
        val expr1 = LiteralIntExpr(createIntLocation(42))
        val result1 = generator.generateConstraints(expr1)
        
        val expr2 = LiteralStringExpr(createStringLocation("hello"))
        val result2 = generator.generateConstraints(expr2)
        
        assertTrue(result1.isSuccess() && result2.isSuccess(), "Both expressions should type check")
        
        // Combine constraints
        val combinedConstraints = result1.constraints.union(result2.constraints)
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(combinedConstraints)
        
        assertTrue(solverResult is ConstraintSolverResult.Success, "Combined constraints should solve successfully")
    }

    @Test
    fun `type check nested let expressions`() {
        // let x = 10 in let y = x + 5 in let z = y * 2 in z
        val xVar = VarExpr(createStringLocation("x"))
        val yVar = VarExpr(createStringLocation("y"))
        val zVar = VarExpr(createStringLocation("z"))
        
        val yExpr = BinaryOpExpr(xVar, BinaryOp.Plus, LiteralIntExpr(createIntLocation(5)), createLocation())
        val zExpr = BinaryOpExpr(yVar, BinaryOp.Star, LiteralIntExpr(createIntLocation(2)), createLocation())
        
        val innerLet = LetExpr(
            false, // recursive
            createStringLocation("z"),
            null, // typeParams
            null, // parameters
            null, // typeAnnotation
            zExpr,
            zVar
        )
        
        val middleLet = LetExpr(
            false, // recursive
            createStringLocation("y"),
            null, // typeParams
            null, // parameters
            null, // typeAnnotation
            yExpr,
            innerLet
        )
        
        val outerLet = LetExpr(
            false, // recursive
            createStringLocation("x"),
            null, // typeParams
            null, // parameters
            null, // typeAnnotation
            LiteralIntExpr(createIntLocation(10)),
            middleLet
        )
        
        val env = TypeEnvironment.empty()
        val generator = ConstraintGenerator(env)
        val result = generator.generateConstraints(outerLet)
        
        assertTrue(result.isSuccess(), "Nested let expressions should type check")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.Int, finalType, "Final result should be Int")
    }
} 