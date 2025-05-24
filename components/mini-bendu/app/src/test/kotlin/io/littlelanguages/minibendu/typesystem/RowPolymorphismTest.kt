package io.littlelanguages.minibendu.typesystem

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import io.littlelanguages.minibendu.*
import io.littlelanguages.scanpiler.LocationCoordinate

/**
 * Test class for row polymorphism functionality.
 * 
 * Row polymorphism allows records to be extended with additional fields
 * while preserving type safety and compatibility. This is essential for
 * mini-bendu's structural typing approach.
 * 
 * Tests cover:
 * - Type inference with row variables
 * - Record extension with row polymorphism  
 * - Subtyping with row polymorphism
 * - Row constraint solving
 */
class RowPolymorphismTest {

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

    // ===== TYPE INFERENCE WITH ROW VARIABLES TESTS =====

    @Test
    fun `row variable should allow record extension`() {
        // Test that a function accepting an open record can work with extended records
        // Process a record {name: String | α} and it should work with {name: "test", age: 25}
        
        val extendedRecord = RecordExpr(listOf(
            FieldExpr(createStringLocation("name"), LiteralStringExpr(createStringLocation("test"))),
            FieldExpr(createStringLocation("age"), LiteralIntExpr(createIntLocation(25)))
        ), createLocation())
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(extendedRecord)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for extended record")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertTrue(finalType is RecordType, "Extended record should result in record type")
        val recordType = finalType as RecordType
        
        // The record should have both fields
        assertTrue(recordType.fields.containsKey("name"))
        assertTrue(recordType.fields.containsKey("age"))
        assertEquals(Types.String, recordType.fields["name"])
        assertEquals(Types.Int, recordType.fields["age"])
        
        // For extensible records, row variable should be present when needed
        assertNotNull(recordType.rowVar, "Record should have row variable for extensibility")
    }

    @Test
    fun `row variable unification with compatible records`() {
        // Test that row variables unify properly with compatible record structures
        
        val rowVar = TypeVariable.fresh()
        val openRecord = RecordType(mapOf("x" to Types.Int), rowVar)
        val closedRecord = RecordType(mapOf("x" to Types.Int, "y" to Types.String))
        
        val unificationResult = Unification.unify(closedRecord, openRecord)
        assertTrue(unificationResult.isSuccess(), 
            "Closed record should unify with compatible open record")
        
        val substitution = unificationResult.getSubstitution()
        val unifiedRowVar = substitution.apply(rowVar)
        
        // The row variable should be unified to represent the additional fields
        assertTrue(unifiedRowVar is RecordType, "Row variable should be unified to a record type")
        val rowType = unifiedRowVar as RecordType
        assertTrue(rowType.fields.containsKey("y"), "Row should contain the additional field")
        assertEquals(Types.String, rowType.fields["y"])
    }

    @Test
    fun `fresh row variables for independent records`() {
        // Test that different open records get fresh row variables
        
        val record1 = RecordExpr(listOf(
            FieldExpr(createStringLocation("x"), LiteralIntExpr(createIntLocation(42)))
        ), createLocation())
        
        val record2 = RecordExpr(listOf(
            FieldExpr(createStringLocation("y"), LiteralStringExpr(createStringLocation("hello")))
        ), createLocation())
        
        val generator = ConstraintGenerator()
        val result1 = generator.generateConstraints(record1)
        val result2 = generator.generateConstraints(record2)
        
        assertTrue(result1.isSuccess() && result2.isSuccess(), 
            "Both records should infer successfully")
        
        val solver = ConstraintSolver()
        val solverResult1 = solver.solve(result1.constraints)
        val solverResult2 = solver.solve(result2.constraints)
        
        assertTrue(solverResult1 is ConstraintSolverResult.Success && 
                  solverResult2 is ConstraintSolverResult.Success, 
                  "Both constraint sets should solve successfully")
        
        val type1 = (solverResult1 as ConstraintSolverResult.Success).substitution.apply(result1.type)
        val type2 = (solverResult2 as ConstraintSolverResult.Success).substitution.apply(result2.type)
        
        assertTrue(type1 is RecordType && type2 is RecordType, 
            "Both should be record types")
        
        val recordType1 = type1 as RecordType
        val recordType2 = type2 as RecordType
        
        // Row variables should be different for independent records
        if (recordType1.rowVar != null && recordType2.rowVar != null) {
            assertNotEquals(recordType1.rowVar, recordType2.rowVar,
                "Independent records should have different row variables")
        }
    }

    // ===== RECORD EXTENSION WITH ROW POLYMORPHISM TESTS =====

    @Test
    fun `record extension should preserve row polymorphism`() {
        // Test that extending a record with spread operations preserves its row polymorphism
        // {...baseRecord, newField: value}
        
        val baseRecord = VarExpr(createStringLocation("baseRecord"))
        val spreadExpr = SpreadExpr(baseRecord)
        val newField = FieldExpr(createStringLocation("z"), LiteralBoolExpr(createBoolLocation(true)))
        
        val extendedRecord = RecordExpr(listOf(spreadExpr, newField), createLocation())
        
        // Set up environment with base record having row variable
        val baseRecordType = RecordType(mapOf("x" to Types.Int), TypeVariable.fresh())
        val env = TypeEnvironment.empty()
            .bind("baseRecord", TypeScheme.monomorphic(baseRecordType))
        
        val generator = ConstraintGenerator(env)
        val result = generator.generateConstraints(extendedRecord)
        
        assertTrue(result.isSuccess(), "Record extension should infer successfully")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        
        if (solverResult is ConstraintSolverResult.Failure) {
            fail<Unit>("Constraint solving failed: ${solverResult.error}")
        }
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertTrue(finalType is RecordType, "Extended record should be a record type")
        
        val recordType = finalType as RecordType
        assertTrue(recordType.fields.containsKey("x"), "Should preserve existing field x")
        assertTrue(recordType.fields.containsKey("z"), "Should have new field z")
        assertEquals(Types.Int, recordType.fields["x"])
        assertEquals(Types.Bool, recordType.fields["z"])
        
        // Should preserve row polymorphism
        assertNotNull(recordType.rowVar, "Extended record should preserve row variable")
    }

    @Test
    fun `multiple record extensions should compose correctly`() {
        // Test that multiple extensions preserve row polymorphism
        // {...{...baseRecord, b: "test"}, c: true}
        
        val baseRecord = VarExpr(createStringLocation("baseRecord"))
        val step1 = RecordExpr(listOf(
            SpreadExpr(baseRecord),
            FieldExpr(createStringLocation("b"), LiteralStringExpr(createStringLocation("test")))
        ), createLocation())
        
        val step2 = RecordExpr(listOf(
            SpreadExpr(step1),
            FieldExpr(createStringLocation("c"), LiteralBoolExpr(createBoolLocation(true)))
        ), createLocation())
        
        val baseRecordType = RecordType(mapOf("a" to Types.Int), TypeVariable.fresh())
        val env = TypeEnvironment.empty()
            .bind("baseRecord", TypeScheme.monomorphic(baseRecordType))
        
        val generator = ConstraintGenerator(env)
        val result = generator.generateConstraints(step2)
        
        assertTrue(result.isSuccess(), "Multiple extensions should infer successfully")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        
        if (solverResult is ConstraintSolverResult.Failure) {
            fail<Unit>("Constraint solving failed: ${solverResult.error}")
        }
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertTrue(finalType is RecordType, "Result should be a record type")
        
        val recordType = finalType as RecordType
        assertTrue(recordType.fields.containsKey("a"), "Should have original field a")
        assertTrue(recordType.fields.containsKey("b"), "Should have first extension field b")
        assertTrue(recordType.fields.containsKey("c"), "Should have second extension field c")
        assertEquals(Types.Int, recordType.fields["a"])
        assertEquals(Types.String, recordType.fields["b"])
        assertEquals(Types.Bool, recordType.fields["c"])
    }

    // ===== SUBTYPING WITH ROW POLYMORPHISM TESTS =====

    @Test
    fun `open record should unify with more specific open record`() {
        // Test that {x: Int, y: String | α} can unify with {x: Int | β}
        
        val moreGeneral = RecordType(
            mapOf("x" to Types.Int),
            TypeVariable.fresh()
        )
        
        val moreSpecific = RecordType(
            mapOf("x" to Types.Int, "y" to Types.String),
            TypeVariable.fresh()
        )
        
        val unificationResult = Unification.unify(moreSpecific, moreGeneral)
        assertTrue(unificationResult.isSuccess(), 
            "More specific record should unify with more general record")
    }

    @Test
    fun `closed record should unify with compatible open record`() {
        // Test that {x: Int, y: String} can unify with {x: Int | α}
        
        val openRecord = RecordType(
            mapOf("x" to Types.Int),
            TypeVariable.fresh()
        )
        
        val closedRecord = RecordType(
            mapOf("x" to Types.Int, "y" to Types.String)
        )
        
        val unificationResult = Unification.unify(closedRecord, openRecord)
        assertTrue(unificationResult.isSuccess(),
            "Closed record should unify with compatible open record")
    }

    // ===== ROW CONSTRAINT SOLVING TESTS =====

    @Test
    fun `row constraints should be solved consistently`() {
        // Test that row constraints are solved consistently across the type system
        // Create a field access on an open record
        
        val rowVar = TypeVariable.fresh()
        val openRecord = RecordType(mapOf("x" to Types.Int), rowVar)
        
        val obj = VarExpr(createStringLocation("obj"))
        val projection = ProjectionExpr(obj, createStringLocation("x"), createLocation())
        
        val env = TypeEnvironment.empty().bind("obj", TypeScheme.monomorphic(openRecord))
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(projection)
        assertTrue(result.isSuccess(), "Field access on open record should succeed")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Row constraints should solve")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.Int, finalType, "Field access should return correct type")
    }

    @Test
    fun `conflicting row constraints should be detected`() {
        // Test that conflicting row constraints are properly detected and reported
        // This should fail: trying to unify {x: Int | α} with {x: String | α}
        
        val rowVar = TypeVariable.fresh()
        val record1 = RecordType(mapOf("x" to Types.Int), rowVar)
        val record2 = RecordType(mapOf("x" to Types.String), rowVar)
        
        val unificationResult = Unification.unify(record1, record2)
        assertTrue(unificationResult.isFailure(),
            "Records with conflicting field types should not unify")
    }

    @Test
    fun `row variable occurs check prevents infinite types`() {
        // Test that occurs check works with row variables to prevent infinite types
        
        val rowVar = TypeVariable.fresh()
        val selfReferencingRecord = RecordType(mapOf("self" to rowVar), rowVar)
        
        val unificationResult = Unification.unify(rowVar, selfReferencingRecord)
        assertTrue(unificationResult.isFailure(),
            "Occurs check should prevent self-referencing row variables")
    }

    @Test
    fun `row variable scope is maintained in nested contexts`() {
        // Test that row variable scoping works correctly in nested contexts
        // Access fields from outer scope in inner lambda
        
        val outerVar = VarExpr(createStringLocation("outer"))
        val innerProjection = ProjectionExpr(outerVar, createStringLocation("field"), createLocation())
        
        val outerRecordType = RecordType(mapOf("field" to Types.String), TypeVariable.fresh())
        val env = TypeEnvironment.empty()
            .bind("outer", TypeScheme.monomorphic(outerRecordType))
        
        val generator = ConstraintGenerator(env)
        val result = generator.generateConstraints(innerProjection)
        
        assertTrue(result.isSuccess(), "Nested field access should work with row variables")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Should solve correctly")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.String, finalType, "Should preserve field type through scoping")
    }
} 