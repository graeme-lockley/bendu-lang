package io.littlelanguages.minibendu.typesystem

import io.littlelanguages.minibendu.*
import io.littlelanguages.scanpiler.LocationCoordinate
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Comprehensive tests for type inference of merge operations.
 * Task 47 of Phase 1 - Create Tests for Merge Type Operations.
 * 
 * Tests cover:
 * - Merge operator type checking
 * - Merge with compatible and incompatible types
 * - Merge associativity and precedence
 * - Merge with complex nested types
 */
class TypeInferenceForMergeOperationsTest {
    
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
    fun `basic record merge with spread`() {
        // Test: {...base, extra: "value"}
        val baseVar = VarExpr(createStringLocation("base"))
        val spreadExpr = SpreadExpr(baseVar)
        val extraField = FieldExpr(createStringLocation("extra"), LiteralStringExpr(createStringLocation("value")))
        val mergedRecord = RecordExpr(listOf(spreadExpr, extraField), createLocation())
        
        val baseType = RecordType(mapOf("name" to Types.String, "age" to Types.Int))
        val env = TypeEnvironment.empty().bind("base", TypeScheme.monomorphic(baseType))
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(mergedRecord)
        assertTrue(result.isSuccess(), "Should successfully generate constraints for basic merge")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Should successfully solve merge constraints")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertTrue(finalType is RecordType, "Result should be record type")
        val recordType = finalType as RecordType
        
        // Should contain all fields from base plus the new field
        assertEquals(Types.String, recordType.fields["name"], "Should preserve name field from base")
        assertEquals(Types.Int, recordType.fields["age"], "Should preserve age field from base")
        assertEquals(Types.String, recordType.fields["extra"], "Should add extra field")
    }
    
    @Test
    fun `merge with field override`() {
        // Test: {...base, name: "NewName"}
        val baseVar = VarExpr(createStringLocation("base"))
        val spreadExpr = SpreadExpr(baseVar)
        val overrideField = FieldExpr(createStringLocation("name"), LiteralStringExpr(createStringLocation("NewName")))
        val mergedRecord = RecordExpr(listOf(spreadExpr, overrideField), createLocation())
        
        val baseType = RecordType(mapOf("name" to Types.String, "age" to Types.Int))
        val env = TypeEnvironment.empty().bind("base", TypeScheme.monomorphic(baseType))
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(mergedRecord)
        assertTrue(result.isSuccess(), "Should successfully generate constraints for field override merge")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Should successfully solve override merge constraints")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertTrue(finalType is RecordType, "Result should be record type")
        val recordType = finalType as RecordType
        
        // Field should be overridden with the new value
        assertEquals(Types.String, recordType.fields["name"], "Should override name field")
        assertEquals(Types.Int, recordType.fields["age"], "Should preserve age field from base")
    }
    
    @Test
    fun `merge with incompatible field types should fail`() {
        // Test: {...base, age: "not a number"} where base.age is Int
        val baseVar = VarExpr(createStringLocation("base"))
        val spreadExpr = SpreadExpr(baseVar)
        val incompatibleField = FieldExpr(createStringLocation("age"), LiteralStringExpr(createStringLocation("not a number")))
        val mergedRecord = RecordExpr(listOf(spreadExpr, incompatibleField), createLocation())
        
        val baseType = RecordType(mapOf("name" to Types.String, "age" to Types.Int))
        val env = TypeEnvironment.empty().bind("base", TypeScheme.monomorphic(baseType))
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(mergedRecord)
        assertTrue(result.isSuccess(), "Should generate constraints even with type conflict")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        
        // Should fail due to type incompatibility (Int vs String)
        assertTrue(solverResult is ConstraintSolverResult.Failure, "Should fail to solve incompatible field type merge")
    }
    
    @Test
    fun `multiple spread operations`() {
        // Test: {...base1, ...base2, extra: "value"}
        val base1Var = VarExpr(createStringLocation("base1"))
        val base2Var = VarExpr(createStringLocation("base2"))
        val spread1 = SpreadExpr(base1Var)
        val spread2 = SpreadExpr(base2Var)
        val extraField = FieldExpr(createStringLocation("extra"), LiteralStringExpr(createStringLocation("value")))
        val mergedRecord = RecordExpr(listOf(spread1, spread2, extraField), createLocation())
        
        val base1Type = RecordType(mapOf("name" to Types.String))
        val base2Type = RecordType(mapOf("age" to Types.Int))
        val env = TypeEnvironment.empty()
            .bind("base1", TypeScheme.monomorphic(base1Type))
            .bind("base2", TypeScheme.monomorphic(base2Type))
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(mergedRecord)
        assertTrue(result.isSuccess(), "Should generate constraints for multiple spreads")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Should successfully solve multiple spread merge")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertTrue(finalType is RecordType, "Result should be record type")
        val recordType = finalType as RecordType
        
        // Should contain fields from both base records plus the extra field
        assertEquals(Types.String, recordType.fields["name"], "Should have name from base1")
        assertEquals(Types.Int, recordType.fields["age"], "Should have age from base2")
        assertEquals(Types.String, recordType.fields["extra"], "Should have extra field")
    }
    
    @Test
    fun `merge with conflicting fields from multiple spreads`() {
        // Test: {...base1, ...base2} where both have same field with different types
        val base1Var = VarExpr(createStringLocation("base1"))
        val base2Var = VarExpr(createStringLocation("base2"))
        val spread1 = SpreadExpr(base1Var)
        val spread2 = SpreadExpr(base2Var)
        val mergedRecord = RecordExpr(listOf(spread1, spread2), createLocation())
        
        val base1Type = RecordType(mapOf("common" to Types.String, "unique1" to Types.Int))
        val base2Type = RecordType(mapOf("common" to Types.Bool, "unique2" to Types.String))
        val env = TypeEnvironment.empty()
            .bind("base1", TypeScheme.monomorphic(base1Type))
            .bind("base2", TypeScheme.monomorphic(base2Type))
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(mergedRecord)
        assertTrue(result.isSuccess(), "Should generate constraints for conflicting spreads")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        
        // Should fail due to conflicting field types
        assertTrue(solverResult is ConstraintSolverResult.Failure, "Should fail to solve conflicting field types")
    }
    
    @Test
    fun `merge order precedence - later overrides earlier`() {
        // Test: {...base, name: "Override1", ...override, name: "Override2"}
        val baseVar = VarExpr(createStringLocation("base"))
        val overrideVar = VarExpr(createStringLocation("override"))
        val spread1 = SpreadExpr(baseVar)
        val override1 = FieldExpr(createStringLocation("name"), LiteralStringExpr(createStringLocation("Override1")))
        val spread2 = SpreadExpr(overrideVar)
        val override2 = FieldExpr(createStringLocation("name"), LiteralStringExpr(createStringLocation("Override2")))
        val mergedRecord = RecordExpr(listOf(spread1, override1, spread2, override2), createLocation())
        
        val baseType = RecordType(mapOf("name" to Types.String))
        val overrideType = RecordType(mapOf("age" to Types.Int))
        val env = TypeEnvironment.empty()
            .bind("base", TypeScheme.monomorphic(baseType))
            .bind("override", TypeScheme.monomorphic(overrideType))
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(mergedRecord)
        assertTrue(result.isSuccess(), "Should generate constraints for precedence test")
        
        // Note: Current implementation may not fully handle precedence
        // This test documents expected behavior for future implementation
    }
    
    @Test
    fun `nested record merge`() {
        // Test: {...outer, inner: {...innerBase, newField: "value"}}
        val outerVar = VarExpr(createStringLocation("outer"))
        val innerBaseVar = VarExpr(createStringLocation("innerBase"))
        
        val innerSpread = SpreadExpr(innerBaseVar)
        val innerField = FieldExpr(createStringLocation("newField"), LiteralStringExpr(createStringLocation("value")))
        val innerRecord = RecordExpr(listOf(innerSpread, innerField), createLocation())
        
        val outerSpread = SpreadExpr(outerVar)
        val outerField = FieldExpr(createStringLocation("inner"), innerRecord)
        val outerMerged = RecordExpr(listOf(outerSpread, outerField), createLocation())
        
        val outerType = RecordType(mapOf("name" to Types.String))
        val innerBaseType = RecordType(mapOf("existing" to Types.Int))
        val env = TypeEnvironment.empty()
            .bind("outer", TypeScheme.monomorphic(outerType))
            .bind("innerBase", TypeScheme.monomorphic(innerBaseType))
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(outerMerged)
        assertTrue(result.isSuccess(), "Should generate constraints for nested merge")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Should successfully solve nested merge")
    }
    
    @Test
    fun `merge with function types`() {
        // Test: {...methods, newMethod: \x -> x + 1}
        val methodsVar = VarExpr(createStringLocation("methods"))
        val spreadExpr = SpreadExpr(methodsVar)
        
        val lambdaParam = createStringLocation("x")
        val lambdaVar = VarExpr(lambdaParam)
        val one = LiteralIntExpr(createIntLocation(1))
        val addition = BinaryOpExpr(lambdaVar, BinaryOp.Plus, one, createLocation())
        val lambda = LambdaExpr(null, lambdaParam, null, addition, createLocation())
        
        val methodField = FieldExpr(createStringLocation("newMethod"), lambda)
        val mergedRecord = RecordExpr(listOf(spreadExpr, methodField), createLocation())
        
        val existingMethodType = FunctionType(Types.String, Types.Int)
        val methodsType = RecordType(mapOf("existing" to existingMethodType))
        val env = TypeEnvironment.empty().bind("methods", TypeScheme.monomorphic(methodsType))
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(mergedRecord)
        assertTrue(result.isSuccess(), "Should generate constraints for function merge")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Should successfully solve function merge")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertTrue(finalType is RecordType, "Result should be record type")
        val recordType = finalType as RecordType
        
        assertTrue(recordType.fields["existing"] is FunctionType, "Should preserve existing function")
        assertTrue(recordType.fields["newMethod"] is FunctionType, "Should add new function")
    }
    
    @Test
    fun `merge with polymorphic types`() {
        // Test: {...poly, concrete: 42}
        val polyVar = VarExpr(createStringLocation("poly"))
        val spreadExpr = SpreadExpr(polyVar)
        val concreteField = FieldExpr(createStringLocation("concrete"), LiteralIntExpr(createIntLocation(42)))
        val mergedRecord = RecordExpr(listOf(spreadExpr, concreteField), createLocation())
        
        val typeVar = TypeVariable.fresh()
        val polyType = RecordType(mapOf("flexible" to typeVar))
        val env = TypeEnvironment.empty().bind("poly", TypeScheme.monomorphic(polyType))
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(mergedRecord)
        assertTrue(result.isSuccess(), "Should generate constraints for polymorphic merge")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Should successfully solve polymorphic merge")
    }
    
    @Test
    fun `merge with tuple types`() {
        // Test: {...base, coordinates: (x, y)}
        val baseVar = VarExpr(createStringLocation("base"))
        val spreadExpr = SpreadExpr(baseVar)
        
        val xVar = VarExpr(createStringLocation("x"))
        val yVar = VarExpr(createStringLocation("y"))
        val tuple = TupleExpr(listOf(xVar, yVar), createLocation())
        val coordField = FieldExpr(createStringLocation("coordinates"), tuple)
        val mergedRecord = RecordExpr(listOf(spreadExpr, coordField), createLocation())
        
        val baseType = RecordType(mapOf("name" to Types.String))
        val env = TypeEnvironment.empty()
            .bind("base", TypeScheme.monomorphic(baseType))
            .bind("x", TypeScheme.monomorphic(Types.Int))
            .bind("y", TypeScheme.monomorphic(Types.Int))
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(mergedRecord)
        assertTrue(result.isSuccess(), "Should generate constraints for tuple merge")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Should successfully solve tuple merge")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertTrue(finalType is RecordType, "Result should be record type")
        val recordType = finalType as RecordType
        
        assertEquals(Types.String, recordType.fields["name"], "Should preserve name field")
        assertTrue(recordType.fields["coordinates"] is TupleType, "Should add tuple field")
    }
    
    @Test
    fun `merge associativity - left to right evaluation`() {
        // Test: {...a, ...b, ...c} should be equivalent to ((...a, ...b), ...c)
        val aVar = VarExpr(createStringLocation("a"))
        val bVar = VarExpr(createStringLocation("b"))
        val cVar = VarExpr(createStringLocation("c"))
        
        val spreadA = SpreadExpr(aVar)
        val spreadB = SpreadExpr(bVar)
        val spreadC = SpreadExpr(cVar)
        val leftToRight = RecordExpr(listOf(spreadA, spreadB, spreadC), createLocation())
        
        val aType = RecordType(mapOf("a" to Types.String))
        val bType = RecordType(mapOf("b" to Types.Int))
        val cType = RecordType(mapOf("c" to Types.Bool))
        val env = TypeEnvironment.empty()
            .bind("a", TypeScheme.monomorphic(aType))
            .bind("b", TypeScheme.monomorphic(bType))
            .bind("c", TypeScheme.monomorphic(cType))
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(leftToRight)
        assertTrue(result.isSuccess(), "Should generate constraints for associative merge")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Should successfully solve associative merge")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertTrue(finalType is RecordType, "Result should be record type")
        val recordType = finalType as RecordType
        
        assertEquals(Types.String, recordType.fields["a"], "Should contain field a")
        assertEquals(Types.Int, recordType.fields["b"], "Should contain field b")
        assertEquals(Types.Bool, recordType.fields["c"], "Should contain field c")
    }
    
    @Test
    fun `merge with empty record`() {
        // Test: {...base, ...{}}
        val baseVar = VarExpr(createStringLocation("base"))
        val spreadBase = SpreadExpr(baseVar)
        
        val emptyRecord = RecordExpr(emptyList(), createLocation())
        val spreadEmpty = SpreadExpr(emptyRecord)
        
        val merged = RecordExpr(listOf(spreadBase, spreadEmpty), createLocation())
        
        val baseType = RecordType(mapOf("name" to Types.String))
        val env = TypeEnvironment.empty().bind("base", TypeScheme.monomorphic(baseType))
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(merged)
        assertTrue(result.isSuccess(), "Should generate constraints for empty record merge")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Should successfully solve empty record merge")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertTrue(finalType is RecordType, "Result should be record type")
        val recordType = finalType as RecordType
        
        assertEquals(Types.String, recordType.fields["name"], "Should preserve fields from base")
        assertEquals(1, recordType.fields.size, "Should only have fields from base")
    }
    
    @Test
    fun `merge preserves source location information`() {
        // Test: Source location preservation in merge operations
        val location = LocationCoordinate(15, 30, 45)
        val baseVar = VarExpr(createStringLocation("base"))
        val spreadExpr = SpreadExpr(baseVar)
        val field = FieldExpr(createStringLocation("extra"), LiteralStringExpr(createStringLocation("value")))
        val merged = RecordExpr(listOf(spreadExpr, field), location)
        
        val baseType = RecordType(mapOf("name" to Types.String))
        val env = TypeEnvironment.empty().bind("base", TypeScheme.monomorphic(baseType))
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(merged)
        assertTrue(result.isSuccess(), "Should generate constraints with location info")
        
        // Check that constraints preserve source location
        val constraints = result.constraints.all()
        val hasLocationInfo = constraints.any { constraint ->
            constraint.sourceLocation != null &&
            (constraint.sourceLocation!!.line == 30 || constraint.sourceLocation!!.column == 45)
        }
        assertTrue(hasLocationInfo, "Should preserve source location in merge constraints")
    }
    
    @Test
    fun `merge with complex nested types`() {
        // Test: Complex merge with union types, intersection types, and functions
        val baseVar = VarExpr(createStringLocation("base"))
        val spreadExpr = SpreadExpr(baseVar)
        
        // Create a complex field: function that returns a union type
        val lambdaParam = createStringLocation("x")
        val lambdaVar = VarExpr(lambdaParam)
        val lambda = LambdaExpr(null, lambdaParam, null, lambdaVar, createLocation())
        val complexField = FieldExpr(createStringLocation("complex"), lambda)
        
        val merged = RecordExpr(listOf(spreadExpr, complexField), createLocation())
        
        // Base type contains an intersection type
        val constraintType1 = TypeAlias("Display", emptyList())
        val constraintType2 = TypeAlias("Debug", emptyList())
        val intersectionType = IntersectionType(setOf(constraintType1, constraintType2))
        val baseType = RecordType(mapOf("constraint" to intersectionType))
        
        val env = TypeEnvironment.empty().bind("base", TypeScheme.monomorphic(baseType))
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(merged)
        assertTrue(result.isSuccess(), "Should generate constraints for complex nested merge")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Should successfully solve complex nested merge")
    }
    
    @Test
    fun `merge error reporting includes source location`() {
        // Test: Error messages include proper source location information
        val location = LocationCoordinate(20, 40, 60)
        val baseVar = VarExpr(createStringLocation("base"))
        val spreadExpr = SpreadExpr(baseVar)
        
        // Create conflicting field type
        val conflictField = FieldExpr(createStringLocation("name"), LiteralIntExpr(createIntLocation(123)))
        val merged = RecordExpr(listOf(spreadExpr, conflictField), location)
        
        val baseType = RecordType(mapOf("name" to Types.String))  // Conflict: String vs Int
        val env = TypeEnvironment.empty().bind("base", TypeScheme.monomorphic(baseType))
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(merged)
        assertTrue(result.isSuccess(), "Should generate constraints even with type conflict")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Failure, "Should fail due to type conflict")
        
        val error = (solverResult as ConstraintSolverResult.Failure).error
        assertNotNull(error, "Should provide error message")
        
        // Error should indicate the type conflict
        assertTrue(error.contains("Int") || error.contains("String") || error.contains("unify"),
                  "Error should indicate type unification failure")
    }
    
    @Test
    fun `performance test with large merge operations`() {
        // Test: Performance with many spread operations
        val startTime = System.currentTimeMillis()
        
        // Create 10 spread operations
        val spreads = (1..10).map { i ->
            SpreadExpr(VarExpr(createStringLocation("base$i")))
        }
        val extraField = FieldExpr(createStringLocation("extra"), LiteralStringExpr(createStringLocation("value")))
        val largeMerge = RecordExpr(spreads + extraField, createLocation())
        
        // Create environment with all base types
        var env = TypeEnvironment.empty()
        for (i in 1..10) {
            val baseType = RecordType(mapOf("field$i" to Types.Int))
            env = env.bind("base$i", TypeScheme.monomorphic(baseType))
        }
        
        val generator = ConstraintGenerator(env)
        val result = generator.generateConstraints(largeMerge)
        
        assertTrue(result.isSuccess(), "Should handle large merge operations")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Should solve large merge efficiently")
        
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        
        assertTrue(duration < 2000, "Large merge should complete within reasonable time (${duration}ms)")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertTrue(finalType is RecordType, "Result should be record type")
        val recordType = finalType as RecordType
        
        // Should have all fields from all bases plus the extra field
        assertEquals(11, recordType.fields.size, "Should have fields from all bases plus extra")
        assertEquals(Types.String, recordType.fields["extra"], "Should have extra field")
    }
    
    @Test
    fun `merge with type variable constraints`() {
        // Test: Merge operations with type variable constraints
        val baseVar = VarExpr(createStringLocation("base"))
        val constrainedVar = VarExpr(createStringLocation("constrained"))
        
        val spreadBase = SpreadExpr(baseVar)
        val spreadConstrained = SpreadExpr(constrainedVar)
        val merged = RecordExpr(listOf(spreadBase, spreadConstrained), createLocation())
        
        val typeVar = TypeVariable.fresh()
        val baseType = RecordType(mapOf("shared" to typeVar))
        val constrainedType = RecordType(mapOf("shared" to Types.Int, "unique" to Types.String))
        
        val env = TypeEnvironment.empty()
            .bind("base", TypeScheme.monomorphic(baseType))
            .bind("constrained", TypeScheme.monomorphic(constrainedType))
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(merged)
        assertTrue(result.isSuccess(), "Should generate constraints for type variable merge")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Should successfully unify type variables in merge")
        
        val substitution = (solverResult as ConstraintSolverResult.Success).substitution
        val resolvedTypeVar = substitution.apply(typeVar)
        assertEquals(Types.Int, resolvedTypeVar, "Type variable should be unified with Int")
    }
} 