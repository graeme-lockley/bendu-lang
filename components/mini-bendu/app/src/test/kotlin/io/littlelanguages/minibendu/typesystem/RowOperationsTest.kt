package io.littlelanguages.minibendu.typesystem

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import io.littlelanguages.minibendu.*
import io.littlelanguages.scanpiler.LocationCoordinate

/**
 * Test class for row operations functionality.
 * 
 * Row operations are the core operations that enable row polymorphism,
 * allowing for flexible record composition and decomposition while
 * maintaining type safety.
 * 
 * Tests cover:
 * - Row concatenation and extension
 * - Row restriction and projection
 * - Row unification with conflicts
 * - Row polymorphic function types
 */
class RowOperationsTest {

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

    // ===== ROW CONCATENATION AND EXTENSION TESTS =====

    @Test
    fun `row concatenation through record merging`() {
        // Test concatenating two open records: {...r1, ...r2}
        
        val record1 = VarExpr(createStringLocation("r1"))
        val record2 = VarExpr(createStringLocation("r2"))
        
        val mergedRecord = RecordExpr(listOf(
            SpreadExpr(record1),
            SpreadExpr(record2)
        ), createLocation())
        
        // Set up environment with open record types
        val r1Type = RecordType(mapOf("a" to Types.Int), TypeVariable.fresh())
        val r2Type = RecordType(mapOf("b" to Types.String), TypeVariable.fresh())
        
        val env = TypeEnvironment.empty()
            .bind("r1", TypeScheme.monomorphic(r1Type))
            .bind("r2", TypeScheme.monomorphic(r2Type))
        
        val generator = ConstraintGenerator(env)
        val result = generator.generateConstraints(mergedRecord)
        
        assertTrue(result.isSuccess(), "Row concatenation should succeed")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        
        if (solverResult is ConstraintSolverResult.Failure) {
            fail<Unit>("Constraint solving failed: ${solverResult.error}")
        }
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertTrue(finalType is RecordType, "Result should be a record type")
        
        val recordType = finalType as RecordType
        assertTrue(recordType.fields.containsKey("a"), "Should contain field 'a' from r1")
        assertTrue(recordType.fields.containsKey("b"), "Should contain field 'b' from r2")
        assertEquals(Types.Int, recordType.fields["a"])
        assertEquals(Types.String, recordType.fields["b"])
        
        // Should preserve row polymorphism
        assertNotNull(recordType.rowVar, "Concatenated record should preserve row polymorphism")
    }

    @Test
    fun `row extension with field override`() {
        // Test extending a record and overriding a field: {...r, a: newValue}
        
        val baseRecord = VarExpr(createStringLocation("base"))
        val extendedRecord = RecordExpr(listOf(
            SpreadExpr(baseRecord),
            FieldExpr(createStringLocation("a"), LiteralStringExpr(createStringLocation("overridden")))
        ), createLocation())
        
        // Base record has 'a: Int' - we're overriding with 'a: String'
        val baseType = RecordType(mapOf("a" to Types.Int, "b" to Types.Bool), TypeVariable.fresh())
        val env = TypeEnvironment.empty()
            .bind("base", TypeScheme.monomorphic(baseType))
        
        val generator = ConstraintGenerator(env)
        val result = generator.generateConstraints(extendedRecord)
        
        assertTrue(result.isSuccess(), "Row extension with override should be analyzed")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        
        if (solverResult is ConstraintSolverResult.Failure) {
            // This should fail because we're trying to override Int with String
            assertTrue(solverResult.error.contains("Field 'a' type mismatch") || 
                      solverResult.error.contains("field override"),
                      "Should report field override conflict: ${solverResult.error}")
        } else {
            fail<Unit>("Field override with incompatible types should fail")
        }
    }

    @Test
    fun `row extension with compatible field override`() {
        // Test extending a record with compatible field override
        
        val baseRecord = VarExpr(createStringLocation("base"))
        val extendedRecord = RecordExpr(listOf(
            SpreadExpr(baseRecord),
            FieldExpr(createStringLocation("a"), LiteralIntExpr(createIntLocation(42)))
        ), createLocation())
        
        // Base record has 'a: Int' - we're overriding with another Int value
        val baseType = RecordType(mapOf("a" to Types.Int, "b" to Types.Bool), TypeVariable.fresh())
        val env = TypeEnvironment.empty()
            .bind("base", TypeScheme.monomorphic(baseType))
        
        val generator = ConstraintGenerator(env)
        val result = generator.generateConstraints(extendedRecord)
        
        assertTrue(result.isSuccess(), "Row extension with compatible override should succeed")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        
        if (solverResult is ConstraintSolverResult.Failure) {
            fail<Unit>("Constraint solving failed: ${solverResult.error}")
        }
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertTrue(finalType is RecordType, "Result should be a record type")
        
        val recordType = finalType as RecordType
        assertTrue(recordType.fields.containsKey("a"), "Should contain overridden field 'a'")
        assertTrue(recordType.fields.containsKey("b"), "Should contain original field 'b'")
        assertEquals(Types.Int, recordType.fields["a"])
        assertEquals(Types.Bool, recordType.fields["b"])
    }

    @Test
    fun `multiple record spreads with field conflicts`() {
        // Test merging multiple records with conflicting fields
        
        val record1 = VarExpr(createStringLocation("r1"))
        val record2 = VarExpr(createStringLocation("r2"))
        val record3 = VarExpr(createStringLocation("r3"))
        
        val mergedRecord = RecordExpr(listOf(
            SpreadExpr(record1),
            SpreadExpr(record2),
            SpreadExpr(record3)
        ), createLocation())
        
        // All records have field 'x' with different types
        val r1Type = RecordType(mapOf("x" to Types.Int, "a" to Types.String), TypeVariable.fresh())
        val r2Type = RecordType(mapOf("x" to Types.Bool, "b" to Types.String), TypeVariable.fresh())
        val r3Type = RecordType(mapOf("x" to Types.String, "c" to Types.String), TypeVariable.fresh())
        
        val env = TypeEnvironment.empty()
            .bind("r1", TypeScheme.monomorphic(r1Type))
            .bind("r2", TypeScheme.monomorphic(r2Type))
            .bind("r3", TypeScheme.monomorphic(r3Type))
        
        val generator = ConstraintGenerator(env)
        val result = generator.generateConstraints(mergedRecord)
        
        assertTrue(result.isSuccess(), "Constraint generation should succeed")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        
        assertTrue(solverResult is ConstraintSolverResult.Failure,
            "Multiple conflicting field types should cause solver failure")
        
        val error = (solverResult as ConstraintSolverResult.Failure).error
        assertTrue(error.contains("Cannot merge field") || error.contains("type mismatch"),
            "Error should mention field conflict: $error")
    }

    // ===== ROW RESTRICTION AND PROJECTION TESTS =====

    @Test
    fun `field projection creates appropriate row constraints`() {
        // Test that field projection generates correct row constraints
        
        val obj = VarExpr(createStringLocation("obj"))
        val projection = ProjectionExpr(obj, createStringLocation("field"), createLocation())
        
        // obj should be constrained to be a record with at least 'field'
        val objType = TypeVariable.fresh()
        val env = TypeEnvironment.empty()
            .bind("obj", TypeScheme.monomorphic(objType))
        
        val generator = ConstraintGenerator(env)
        val result = generator.generateConstraints(projection)
        
        assertTrue(result.isSuccess(), "Field projection should generate constraints successfully")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        
        if (solverResult is ConstraintSolverResult.Failure) {
            fail<Unit>("Constraint solving failed: ${solverResult.error}")
        }
        
        val substitution = (solverResult as ConstraintSolverResult.Success).substitution
        val resolvedObjType = substitution.apply(objType)
        
        assertTrue(resolvedObjType is RecordType, "obj should be resolved to a record type")
        val recordType = resolvedObjType as RecordType
        assertTrue(recordType.fields.containsKey("field"), "Record should have the projected field")
        assertNotNull(recordType.rowVar, "Record should have row variable for additional fields")
    }

    @Test
    fun `multiple projections from same record`() {
        // Test multiple field accesses from the same record variable
        
        val obj = VarExpr(createStringLocation("obj"))
        val field1 = ProjectionExpr(obj, createStringLocation("a"), createLocation())
        val field2 = ProjectionExpr(obj, createStringLocation("b"), createLocation())
        
        // Create a tuple containing both projections
        val tupleExpr = TupleExpr(listOf(field1, field2), createLocation())
        
        val objType = TypeVariable.fresh()
        val env = TypeEnvironment.empty()
            .bind("obj", TypeScheme.monomorphic(objType))
        
        val generator = ConstraintGenerator(env)
        val result = generator.generateConstraints(tupleExpr)
        
        assertTrue(result.isSuccess(), "Multiple projections should succeed")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        
        if (solverResult is ConstraintSolverResult.Failure) {
            fail<Unit>("Constraint solving failed: ${solverResult.error}")
        }
        
        val substitution = (solverResult as ConstraintSolverResult.Success).substitution
        val resolvedObjType = substitution.apply(objType)
        
        assertTrue(resolvedObjType is RecordType, "obj should be resolved to a record type")
        val recordType = resolvedObjType as RecordType
        assertTrue(recordType.fields.containsKey("a"), "Record should have field 'a'")
        assertTrue(recordType.fields.containsKey("b"), "Record should have field 'b'")
        assertNotNull(recordType.rowVar, "Record should maintain row polymorphism")
    }

    // ===== ROW UNIFICATION WITH CONFLICTS TESTS =====

    @Test
    fun `row unification with field type conflicts`() {
        // Test unifying records with conflicting field types
        
        val rowVar1 = TypeVariable.fresh()
        val rowVar2 = TypeVariable.fresh()
        
        val record1 = RecordType(mapOf("x" to Types.Int, "y" to Types.String), rowVar1)
        val record2 = RecordType(mapOf("x" to Types.Bool, "z" to Types.Int), rowVar2)
        
        val unificationResult = Unification.unify(record1, record2)
        
        assertTrue(unificationResult.isFailure(),
            "Records with conflicting field types should not unify")
        
        val error = unificationResult.getError()!!
        assertTrue(error.contains("x") && (error.contains("Int") || error.contains("Bool")),
            "Error should mention the conflicting field and types: $error")
    }

    @Test
    fun `row unification with compatible field extension`() {
        // Test unifying records where one extends the other
        
        val rowVar1 = TypeVariable.fresh()
        val rowVar2 = TypeVariable.fresh()
        
        val smallerRecord = RecordType(mapOf("x" to Types.Int), rowVar1)
        val largerRecord = RecordType(mapOf("x" to Types.Int, "y" to Types.String), rowVar2)
        
        val unificationResult = Unification.unify(largerRecord, smallerRecord)
        
        assertTrue(unificationResult.isSuccess(),
            "Compatible record extension should unify successfully")
        
        val substitution = unificationResult.getSubstitution()
        
        // The row variable from the smaller record should be unified with a record containing 'y'
        val resolvedRowVar1 = substitution.apply(rowVar1)
        assertTrue(resolvedRowVar1 is RecordType, "Row variable should be unified to a record")
        
        val rowType = resolvedRowVar1 as RecordType
        assertTrue(rowType.fields.containsKey("y"), "Row should contain the additional field 'y'")
        assertEquals(Types.String, rowType.fields["y"])
    }

    @Test
    fun `symmetric row unification`() {
        // Test that row unification is symmetric
        
        val rowVar1 = TypeVariable.fresh()
        val rowVar2 = TypeVariable.fresh()
        
        val record1 = RecordType(mapOf("a" to Types.Int), rowVar1)
        val record2 = RecordType(mapOf("a" to Types.Int, "b" to Types.String), rowVar2)
        
        val result1 = Unification.unify(record1, record2)
        val result2 = Unification.unify(record2, record1)
        
        assertEquals(result1.isSuccess(), result2.isSuccess(),
            "Unification should be symmetric")
        
        if (result1.isSuccess() && result2.isSuccess()) {
            val subst1 = result1.getSubstitution()
            val subst2 = result2.getSubstitution()
            
            // Both should result in equivalent unified types
            val unified1_r1 = subst1.apply(record1)
            val unified1_r2 = subst1.apply(record2)
            val unified2_r1 = subst2.apply(record1)
            val unified2_r2 = subst2.apply(record2)
            
            // The unified forms should be structurally equivalent
            assertTrue(unified1_r1.structurallyEquivalent(unified2_r1) ||
                      unified1_r1.structurallyEquivalent(unified2_r2),
                      "Symmetric unification should produce equivalent results")
        }
    }

    // ===== ROW POLYMORPHIC FUNCTION TYPES TESTS =====

    @Test
    fun `row polymorphic function parameter`() {
        // Test a function that accepts any record with at least certain fields
        // fn process(r: {name: String | α}) -> String = r.name
        
        val paramVar = VarExpr(createStringLocation("r"))
        val functionBody = ProjectionExpr(paramVar, createStringLocation("name"), createLocation())
        
        // Create function with open record parameter type
        val rowVar = TypeVariable.fresh()
        val paramType = RecordType(mapOf("name" to Types.String), rowVar)
        
        val env = TypeEnvironment.empty()
            .bind("r", TypeScheme.monomorphic(paramType))
        
        val generator = ConstraintGenerator(env)
        val result = generator.generateConstraints(functionBody)
        
        assertTrue(result.isSuccess(), "Row polymorphic function body should type check")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        
        if (solverResult is ConstraintSolverResult.Failure) {
            fail<Unit>("Constraint solving failed: ${solverResult.error}")
        }
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.String, finalType, "Function should return String")
    }

    @Test
    fun `row polymorphic function application`() {
        // Test applying a row polymorphic function to different record types
        
        // Function that accepts {x: Int | α} and returns the x field
        val funcVar = VarExpr(createStringLocation("func"))
        val argVar = VarExpr(createStringLocation("arg"))
        val application = ApplicationExpr(funcVar, listOf(argVar), createLocation())
        
        // Function type: {x: Int | α} -> Int
        val paramRowVar = TypeVariable.fresh()
        val funcParamType = RecordType(mapOf("x" to Types.Int), paramRowVar)
        val funcType = FunctionType(funcParamType, Types.Int)
        
        // Argument type: {x: Int, y: String}
        val argType = RecordType(mapOf("x" to Types.Int, "y" to Types.String))
        
        val env = TypeEnvironment.empty()
            .bind("func", TypeScheme.monomorphic(funcType))
            .bind("arg", TypeScheme.monomorphic(argType))
        
        val generator = ConstraintGenerator(env)
        val result = generator.generateConstraints(application)
        
        assertTrue(result.isSuccess(), "Row polymorphic function application should succeed")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        
        if (solverResult is ConstraintSolverResult.Failure) {
            fail<Unit>("Constraint solving failed: ${solverResult.error}")
        }
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.Int, finalType, "Application should return Int")
    }

    @Test
    fun `higher order row polymorphic functions`() {
        // Test functions that take row polymorphic functions as parameters
        
        // mapper: ({a: T | α} -> U) -> {a: T | α} -> U
        val mapperVar = VarExpr(createStringLocation("mapper"))
        val funcVar = VarExpr(createStringLocation("func"))
        val recordVar = VarExpr(createStringLocation("record"))
        
        // mapper(func)(record)
        val partialApp = ApplicationExpr(mapperVar, listOf(funcVar), createLocation())
        val fullApp = ApplicationExpr(partialApp, listOf(recordVar), createLocation())
        
        // Set up types:
        // func: {a: Int | α} -> String
        val funcRowVar = TypeVariable.fresh()
        val funcParamType = RecordType(mapOf("a" to Types.Int), funcRowVar)
        val funcType = FunctionType(funcParamType, Types.String)
        
        // mapper: ({a: Int | α} -> String) -> ({a: Int | α} -> String)
        val mapperType = FunctionType(funcType, funcType)
        
        // record: {a: Int, b: Bool}
        val recordType = RecordType(mapOf("a" to Types.Int, "b" to Types.Bool))
        
        val env = TypeEnvironment.empty()
            .bind("mapper", TypeScheme.monomorphic(mapperType))
            .bind("func", TypeScheme.monomorphic(funcType))
            .bind("record", TypeScheme.monomorphic(recordType))
        
        val generator = ConstraintGenerator(env)
        val result = generator.generateConstraints(fullApp)
        
        assertTrue(result.isSuccess(), "Higher-order row polymorphic function should work")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        
        if (solverResult is ConstraintSolverResult.Failure) {
            fail<Unit>("Constraint solving failed: ${solverResult.error}")
        }
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.String, finalType, "Higher-order application should return String")
    }

    @Test
    fun `row polymorphic function with multiple constraints`() {
        // Test function with multiple row polymorphic parameters
        // fn combine(r1: {a: T | α}, r2: {b: U | β}) -> {a: T, b: U}
        
        val r1Var = VarExpr(createStringLocation("r1"))
        val r2Var = VarExpr(createStringLocation("r2"))
        
        val field1 = ProjectionExpr(r1Var, createStringLocation("a"), createLocation())
        val field2 = ProjectionExpr(r2Var, createStringLocation("b"), createLocation())
        
        val resultRecord = RecordExpr(listOf(
            FieldExpr(createStringLocation("a"), field1),
            FieldExpr(createStringLocation("b"), field2)
        ), createLocation())
        
        // Set up parameter types with different row variables
        val rowVar1 = TypeVariable.fresh()
        val rowVar2 = TypeVariable.fresh()
        val r1Type = RecordType(mapOf("a" to Types.Int), rowVar1)
        val r2Type = RecordType(mapOf("b" to Types.String), rowVar2)
        
        val env = TypeEnvironment.empty()
            .bind("r1", TypeScheme.monomorphic(r1Type))
            .bind("r2", TypeScheme.monomorphic(r2Type))
        
        val generator = ConstraintGenerator(env)
        val result = generator.generateConstraints(resultRecord)
        
        assertTrue(result.isSuccess(), "Multi-parameter row polymorphic function should work")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        
        if (solverResult is ConstraintSolverResult.Failure) {
            fail<Unit>("Constraint solving failed: ${solverResult.error}")
        }
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertTrue(finalType is RecordType, "Result should be a record type")
        
        val recordType = finalType as RecordType
        assertTrue(recordType.fields.containsKey("a"), "Result should have field 'a'")
        assertTrue(recordType.fields.containsKey("b"), "Result should have field 'b'")
        assertEquals(Types.Int, recordType.fields["a"])
        assertEquals(Types.String, recordType.fields["b"])
    }
} 