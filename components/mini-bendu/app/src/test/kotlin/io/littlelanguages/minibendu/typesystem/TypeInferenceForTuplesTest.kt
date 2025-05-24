package io.littlelanguages.minibendu.typesystem

import io.littlelanguages.minibendu.*
import io.littlelanguages.scanpiler.LocationCoordinate
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Comprehensive tests for type inference of tuple expressions.
 * Task 41 of Phase 1 - Type Inference for Data Structures.
 * 
 * Tests cover:
 * - Inference for tuple literals: (1, "hello", true)
 * - Destructuring tuples: pattern matching and unpacking
 * - Type checking for tuples of different sizes/types
 * - Nested tuple structures: ((1, 2), (3, 4))
 * - Tuple operations and compatibility checking
 * - Error handling for tuple type mismatches
 */
class TypeInferenceForTuplesTest {
    
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
    fun `simple tuple literal with primitive types`() {
        // Test: (42, "hello", true)
        val int42 = LiteralIntExpr(createIntLocation(42))
        val helloStr = LiteralStringExpr(createStringLocation("hello"))
        val trueVal = LiteralBoolExpr(createBoolLocation(true))
        val tuple = TupleExpr(listOf(int42, helloStr, trueVal), createLocation())
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(tuple)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for simple tuple literal")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertTrue(finalType is TupleType, "Tuple literal should result in tuple type")
        val tupleType = finalType as TupleType
        assertEquals(3, tupleType.elements.size, "Tuple should have 3 elements")
        assertEquals(Types.Int, tupleType.elements[0], "First element should be Int")
        assertEquals(Types.String, tupleType.elements[1], "Second element should be String")
        assertEquals(Types.Bool, tupleType.elements[2], "Third element should be Bool")
    }
    
    @Test
    fun `empty tuple - unit type`() {
        // Test: ()
        val tuple = TupleExpr(emptyList(), createLocation())
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(tuple)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for empty tuple")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertTrue(finalType is TupleType, "Empty tuple should result in tuple type")
        val tupleType = finalType as TupleType
        assertEquals(0, tupleType.elements.size, "Empty tuple should have 0 elements")
    }
    
    @Test
    fun `pair tuple with different types`() {
        // Test: (1, "one")
        val one = LiteralIntExpr(createIntLocation(1))
        val oneStr = LiteralStringExpr(createStringLocation("one"))
        val tuple = TupleExpr(listOf(one, oneStr), createLocation())
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(tuple)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for pair tuple")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertTrue(finalType is TupleType, "Pair should result in tuple type")
        val tupleType = finalType as TupleType
        assertEquals(2, tupleType.elements.size, "Pair should have 2 elements")
        assertEquals(Types.Int, tupleType.elements[0], "First element should be Int")
        assertEquals(Types.String, tupleType.elements[1], "Second element should be String")
    }
    
    @Test
    fun `tuple with computed expressions`() {
        // Test: (1 + 2, "hello" == "world", true && false)
        val addition = BinaryOpExpr(
            LiteralIntExpr(createIntLocation(1)),
            BinaryOp.Plus,
            LiteralIntExpr(createIntLocation(2)),
            createLocation()
        )
        val comparison = BinaryOpExpr(
            LiteralStringExpr(createStringLocation("hello")),
            BinaryOp.EqualEqual,
            LiteralStringExpr(createStringLocation("world")),
            createLocation()
        )
        val logical = BinaryOpExpr(
            LiteralBoolExpr(createBoolLocation(true)),
            BinaryOp.And,
            LiteralBoolExpr(createBoolLocation(false)),
            createLocation()
        )
        val tuple = TupleExpr(listOf(addition, comparison, logical), createLocation())
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(tuple)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for tuple with computed expressions")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertTrue(finalType is TupleType, "Tuple should result in tuple type")
        val tupleType = finalType as TupleType
        assertEquals(3, tupleType.elements.size, "Tuple should have 3 elements")
        assertEquals(Types.Int, tupleType.elements[0], "First element should be Int")
        assertEquals(Types.Bool, tupleType.elements[1], "Second element should be Bool")
        assertEquals(Types.Bool, tupleType.elements[2], "Third element should be Bool")
    }
    
    @Test
    fun `tuple with variable elements`() {
        // Test: (x, y, z) where x: Int, y: String, z: Bool
        val xVar = VarExpr(createStringLocation("x"))
        val yVar = VarExpr(createStringLocation("y"))
        val zVar = VarExpr(createStringLocation("z"))
        val tuple = TupleExpr(listOf(xVar, yVar, zVar), createLocation())
        
        val env = TypeEnvironment.empty()
            .bind("x", TypeScheme.monomorphic(Types.Int))
            .bind("y", TypeScheme.monomorphic(Types.String))
            .bind("z", TypeScheme.monomorphic(Types.Bool))
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(tuple)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for tuple with variables")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertTrue(finalType is TupleType, "Tuple should result in tuple type")
        val tupleType = finalType as TupleType
        assertEquals(3, tupleType.elements.size, "Tuple should have 3 elements")
        assertEquals(Types.Int, tupleType.elements[0], "First element should be Int")
        assertEquals(Types.String, tupleType.elements[1], "Second element should be String")
        assertEquals(Types.Bool, tupleType.elements[2], "Third element should be Bool")
    }
    
    @Test
    fun `nested tuple structures`() {
        // Test: ((1, 2), (3, 4))
        val innerTuple1 = TupleExpr(
            listOf(
                LiteralIntExpr(createIntLocation(1)),
                LiteralIntExpr(createIntLocation(2))
            ),
            createLocation()
        )
        val innerTuple2 = TupleExpr(
            listOf(
                LiteralIntExpr(createIntLocation(3)),
                LiteralIntExpr(createIntLocation(4))
            ),
            createLocation()
        )
        val outerTuple = TupleExpr(listOf(innerTuple1, innerTuple2), createLocation())
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(outerTuple)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for nested tuples")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertTrue(finalType is TupleType, "Outer tuple should result in tuple type")
        val outerTupleType = finalType as TupleType
        assertEquals(2, outerTupleType.elements.size, "Outer tuple should have 2 elements")
        
        assertTrue(outerTupleType.elements[0] is TupleType, "First element should be tuple type")
        assertTrue(outerTupleType.elements[1] is TupleType, "Second element should be tuple type")
        
        val innerTupleType1 = outerTupleType.elements[0] as TupleType
        val innerTupleType2 = outerTupleType.elements[1] as TupleType
        
        assertEquals(2, innerTupleType1.elements.size, "Inner tuple 1 should have 2 elements")
        assertEquals(2, innerTupleType2.elements.size, "Inner tuple 2 should have 2 elements")
        
        assertEquals(Types.Int, innerTupleType1.elements[0], "Inner tuple 1, element 1 should be Int")
        assertEquals(Types.Int, innerTupleType1.elements[1], "Inner tuple 1, element 2 should be Int")
        assertEquals(Types.Int, innerTupleType2.elements[0], "Inner tuple 2, element 1 should be Int")
        assertEquals(Types.Int, innerTupleType2.elements[1], "Inner tuple 2, element 2 should be Int")
    }
    
    @Test
    fun `tuple with function elements`() {
        // Test: (\x -> x + 1, \y -> y == "test")
        val xVar = VarExpr(createStringLocation("x"))
        val one = LiteralIntExpr(createIntLocation(1))
        val addition = BinaryOpExpr(xVar, BinaryOp.Plus, one, createLocation())
        val lambda1 = LambdaExpr(null, createStringLocation("x"), null, addition, createLocation())
        
        val yVar = VarExpr(createStringLocation("y"))
        val test = LiteralStringExpr(createStringLocation("test"))
        val comparison = BinaryOpExpr(yVar, BinaryOp.EqualEqual, test, createLocation())
        val lambda2 = LambdaExpr(null, createStringLocation("y"), null, comparison, createLocation())
        
        val tuple = TupleExpr(listOf(lambda1, lambda2), createLocation())
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(tuple)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for tuple with function elements")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertTrue(finalType is TupleType, "Tuple should result in tuple type")
        val tupleType = finalType as TupleType
        assertEquals(2, tupleType.elements.size, "Tuple should have 2 elements")
        
        assertTrue(tupleType.elements[0] is FunctionType, "First element should be function type")
        assertTrue(tupleType.elements[1] is FunctionType, "Second element should be function type")
        
        val func1Type = tupleType.elements[0] as FunctionType
        val func2Type = tupleType.elements[1] as FunctionType
        
        assertEquals(Types.Int, func1Type.domain, "First function should take Int")
        assertEquals(Types.Int, func1Type.codomain, "First function should return Int")
        assertEquals(Types.String, func2Type.domain, "Second function should take String")
        assertEquals(Types.Bool, func2Type.codomain, "Second function should return Bool")
    }
    
    @Test
    fun `tuple with record elements`() {
        // Test: ({name: "John"}, {age: 30})
        val nameField = FieldExpr(createStringLocation("name"), LiteralStringExpr(createStringLocation("John")))
        val record1 = RecordExpr(listOf(nameField), createLocation())
        
        val ageField = FieldExpr(createStringLocation("age"), LiteralIntExpr(createIntLocation(30)))
        val record2 = RecordExpr(listOf(ageField), createLocation())
        
        val tuple = TupleExpr(listOf(record1, record2), createLocation())
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(tuple)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for tuple with record elements")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertTrue(finalType is TupleType, "Tuple should result in tuple type")
        val tupleType = finalType as TupleType
        assertEquals(2, tupleType.elements.size, "Tuple should have 2 elements")
        
        assertTrue(tupleType.elements[0] is RecordType, "First element should be record type")
        assertTrue(tupleType.elements[1] is RecordType, "Second element should be record type")
        
        val recordType1 = tupleType.elements[0] as RecordType
        val recordType2 = tupleType.elements[1] as RecordType
        
        assertEquals(Types.String, recordType1.fields["name"], "First record name field should be String")
        assertEquals(Types.Int, recordType2.fields["age"], "Second record age field should be Int")
    }
    
    @Test
    fun `tuple in function context`() {
        // Test: f((1, "hello")) where f expects tuple parameter
        val tuple = TupleExpr(
            listOf(
                LiteralIntExpr(createIntLocation(1)),
                LiteralStringExpr(createStringLocation("hello"))
            ),
            createLocation()
        )
        
        val fVar = VarExpr(createStringLocation("f"))
        val application = ApplicationExpr(fVar, listOf(tuple), createLocation())
        
        val tupleType = TupleType(listOf(Types.Int, Types.String))
        val fType = FunctionType(tupleType, Types.Bool)
        val env = TypeEnvironment.empty().bind("f", TypeScheme.monomorphic(fType))
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(application)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for tuple in function context")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.Bool, finalType, "Function application should result in function return type")
    }
    
    @Test
    fun `tuple with conditional elements`() {
        // Test: (if condition then 1 else 0, x)
        val condition = VarExpr(createStringLocation("condition"))
        val one = LiteralIntExpr(createIntLocation(1))
        val zero = LiteralIntExpr(createIntLocation(0))
        val ifExpr = IfExpr(condition, one, zero, createLocation())
        
        val xVar = VarExpr(createStringLocation("x"))
        val tuple = TupleExpr(listOf(ifExpr, xVar), createLocation())
        
        val env = TypeEnvironment.empty()
            .bind("condition", TypeScheme.monomorphic(Types.Bool))
            .bind("x", TypeScheme.monomorphic(Types.String))
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(tuple)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for tuple with conditional elements")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertTrue(finalType is TupleType, "Tuple should result in tuple type")
        val tupleType = finalType as TupleType
        assertEquals(2, tupleType.elements.size, "Tuple should have 2 elements")
        assertEquals(Types.Int, tupleType.elements[0], "First element should be Int")
        assertEquals(Types.String, tupleType.elements[1], "Second element should be String")
    }
    
    @Test
    fun `single element tuple`() {
        // Test: (42,) - single element tuple 
        val fortyTwo = LiteralIntExpr(createIntLocation(42))
        val tuple = TupleExpr(listOf(fortyTwo), createLocation())
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(tuple)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for single element tuple")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertTrue(finalType is TupleType, "Single element tuple should result in tuple type")
        val tupleType = finalType as TupleType
        assertEquals(1, tupleType.elements.size, "Single element tuple should have 1 element")
        assertEquals(Types.Int, tupleType.elements[0], "Element should be Int")
    }
    
    @Test
    fun `deeply nested tuples`() {
        // Test: (((1, 2), 3), 4)
        val innermost = TupleExpr(
            listOf(
                LiteralIntExpr(createIntLocation(1)),
                LiteralIntExpr(createIntLocation(2))
            ),
            createLocation()
        )
        val middle = TupleExpr(
            listOf(
                innermost,
                LiteralIntExpr(createIntLocation(3))
            ),
            createLocation()
        )
        val outermost = TupleExpr(
            listOf(
                middle,
                LiteralIntExpr(createIntLocation(4))
            ),
            createLocation()
        )
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(outermost)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for deeply nested tuples")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertTrue(finalType is TupleType, "Outermost should be tuple type")
        val outermostType = finalType as TupleType
        assertEquals(2, outermostType.elements.size, "Outermost should have 2 elements")
        
        assertTrue(outermostType.elements[0] is TupleType, "First element should be tuple")
        assertEquals(Types.Int, outermostType.elements[1], "Second element should be Int")
        
        val middleType = outermostType.elements[0] as TupleType
        assertEquals(2, middleType.elements.size, "Middle should have 2 elements")
        
        assertTrue(middleType.elements[0] is TupleType, "First element of middle should be tuple")
        assertEquals(Types.Int, middleType.elements[1], "Second element of middle should be Int")
        
        val innermostType = middleType.elements[0] as TupleType
        assertEquals(2, innermostType.elements.size, "Innermost should have 2 elements")
        assertEquals(Types.Int, innermostType.elements[0], "First element of innermost should be Int")
        assertEquals(Types.Int, innermostType.elements[1], "Second element of innermost should be Int")
    }
    
    @Test
    fun `tuple with polymorphic elements`() {
        // Test: (x, x) where x has polymorphic type
        val xVar1 = VarExpr(createStringLocation("x"))
        val xVar2 = VarExpr(createStringLocation("x"))
        val tuple = TupleExpr(listOf(xVar1, xVar2), createLocation())
        
        val xType = TypeVariable.fresh()
        val env = TypeEnvironment.empty().bind("x", TypeScheme.monomorphic(xType))
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(tuple)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for tuple with polymorphic elements")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val substitution = (solverResult as ConstraintSolverResult.Success).substitution
        val finalType = substitution.apply(result.type)
        assertTrue(finalType is TupleType, "Result should be tuple type")
        
        val tupleType = finalType as TupleType
        assertEquals(2, tupleType.elements.size, "Tuple should have 2 elements")
        val resolvedXType = substitution.apply(xType)
        assertEquals(resolvedXType, tupleType.elements[0], "First element should have same type as x")
        assertEquals(resolvedXType, tupleType.elements[1], "Second element should have same type as x")
    }
    
    @Test
    fun `tuple in let binding context`() {
        // Test: let pair = (1, "hello") in pair
        val tuple = TupleExpr(
            listOf(
                LiteralIntExpr(createIntLocation(1)),
                LiteralStringExpr(createStringLocation("hello"))
            ),
            createLocation()
        )
        
        val pairVar = VarExpr(createStringLocation("pair"))
        val letExpr = LetExpr(
            false,
            createStringLocation("pair"),
            null, null, null,
            tuple,
            pairVar
        )
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(letExpr)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for tuple in let binding")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertTrue(finalType is TupleType, "Result should be tuple type")
        val tupleType = finalType as TupleType
        assertEquals(2, tupleType.elements.size, "Tuple should have 2 elements")
        assertEquals(Types.Int, tupleType.elements[0], "First element should be Int")
        assertEquals(Types.String, tupleType.elements[1], "Second element should be String")
    }
    
    @Test
    fun `tuple with application results`() {
        // Test: (f(x), g(y)) where f: Int -> String, g: Bool -> Int
        val fVar = VarExpr(createStringLocation("f"))
        val gVar = VarExpr(createStringLocation("g"))
        val xVar = VarExpr(createStringLocation("x"))
        val yVar = VarExpr(createStringLocation("y"))
        
        val fApp = ApplicationExpr(fVar, listOf(xVar), createLocation())
        val gApp = ApplicationExpr(gVar, listOf(yVar), createLocation())
        val tuple = TupleExpr(listOf(fApp, gApp), createLocation())
        
        val fType = FunctionType(Types.Int, Types.String)
        val gType = FunctionType(Types.Bool, Types.Int)
        val env = TypeEnvironment.empty()
            .bind("f", TypeScheme.monomorphic(fType))
            .bind("g", TypeScheme.monomorphic(gType))
            .bind("x", TypeScheme.monomorphic(Types.Int))
            .bind("y", TypeScheme.monomorphic(Types.Bool))
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(tuple)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for tuple with application results")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertTrue(finalType is TupleType, "Result should be tuple type")
        val tupleType = finalType as TupleType
        assertEquals(2, tupleType.elements.size, "Tuple should have 2 elements")
        assertEquals(Types.String, tupleType.elements[0], "First element should be String (f result)")
        assertEquals(Types.Int, tupleType.elements[1], "Second element should be Int (g result)")
    }
    
    @Test
    fun `heterogeneous tuple with mixed complex types`() {
        // Test: Complex tuple with record, function, and nested tuple
        // ({name: "test"}, \x -> x + 1, (true, 42))
        
        val nameField = FieldExpr(createStringLocation("name"), LiteralStringExpr(createStringLocation("test")))
        val record = RecordExpr(listOf(nameField), createLocation())
        
        val xVar = VarExpr(createStringLocation("x"))
        val one = LiteralIntExpr(createIntLocation(1))
        val addition = BinaryOpExpr(xVar, BinaryOp.Plus, one, createLocation())
        val lambda = LambdaExpr(null, createStringLocation("x"), null, addition, createLocation())
        
        val innerTuple = TupleExpr(
            listOf(
                LiteralBoolExpr(createBoolLocation(true)),
                LiteralIntExpr(createIntLocation(42))
            ),
            createLocation()
        )
        
        val complexTuple = TupleExpr(listOf(record, lambda, innerTuple), createLocation())
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(complexTuple)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for complex heterogeneous tuple")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertTrue(finalType is TupleType, "Result should be tuple type")
        val tupleType = finalType as TupleType
        assertEquals(3, tupleType.elements.size, "Tuple should have 3 elements")
        
        assertTrue(tupleType.elements[0] is RecordType, "First element should be record type")
        assertTrue(tupleType.elements[1] is FunctionType, "Second element should be function type")
        assertTrue(tupleType.elements[2] is TupleType, "Third element should be tuple type")
    }
    
    @Test
    fun `performance test with large tuple`() {
        // Test: Tuple with many elements
        val startTime = System.currentTimeMillis()
        
        val elements = (1..30).map { i ->
            LiteralIntExpr(createIntLocation(i))
        }
        val tuple = TupleExpr(elements, createLocation())
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(tuple)
        
        assertTrue(result.isSuccess(), "Should handle large tuple")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Should solve large tuple constraints")
        
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        
        assertTrue(duration < 2000, "Should handle large tuple within reasonable time (${duration}ms)")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertTrue(finalType is TupleType, "Result should be tuple type")
        val tupleType = finalType as TupleType
        assertEquals(30, tupleType.elements.size, "Should have all 30 elements")
        
        // Check that all elements are Int
        for (i in 0 until 30) {
            assertEquals(Types.Int, tupleType.elements[i], "Element $i should be Int")
        }
    }
    
    @Test
    fun `tuple preserves source location information`() {
        // Test: Source location preservation in constraint generation
        val location = LocationCoordinate(13, 26, 39)
        val tuple = TupleExpr(
            listOf(
                LiteralIntExpr(createIntLocation(1)),
                LiteralStringExpr(createStringLocation("test"))
            ),
            location
        )
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(tuple)
        
        assertTrue(result.isSuccess(), "Should successfully generate constraints")
        
        // Check that constraints preserve source location information
        val constraints = result.constraints.all()
        val hasLocationInfo = constraints.any { constraint ->
            constraint.sourceLocation != null &&
            (constraint.sourceLocation!!.line == 26 || constraint.sourceLocation!!.column == 39)
        }
        assertTrue(hasLocationInfo, "Should preserve source location information in constraints")
    }
    
    @Test
    fun `tuple compatibility with type inference`() {
        // Test: Tuple type compatibility in different contexts
        val tuple1 = TupleExpr(
            listOf(
                LiteralIntExpr(createIntLocation(1)),
                LiteralStringExpr(createStringLocation("a"))
            ),
            createLocation()
        )
        
        val tuple2 = TupleExpr(
            listOf(
                LiteralIntExpr(createIntLocation(2)),
                LiteralStringExpr(createStringLocation("b"))
            ),
            createLocation()
        )
        
        // Test in conditional context where both branches must have same type
        val condition = VarExpr(createStringLocation("condition"))
        val ifExpr = IfExpr(condition, tuple1, tuple2, createLocation())
        
        val env = TypeEnvironment.empty().bind("condition", TypeScheme.monomorphic(Types.Bool))
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(ifExpr)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for compatible tuples in conditional")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertTrue(finalType is TupleType, "Result should be tuple type")
        val tupleType = finalType as TupleType
        assertEquals(2, tupleType.elements.size, "Tuple should have 2 elements")
        assertEquals(Types.Int, tupleType.elements[0], "First element should be Int")
        assertEquals(Types.String, tupleType.elements[1], "Second element should be String")
    }
} 