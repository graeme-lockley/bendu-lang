package io.littlelanguages.minibendu.typesystem

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.assertThrows

/**
 * Comprehensive tests for the unification algorithm.
 * Tests cover basic types, type variables, function types, complex nested types,
 * occurs check, and error handling.
 */
class UnificationTest {
    
    @Test
    fun `unification of identical basic types succeeds`() {
        val intType1 = Types.Int
        val intType2 = Types.Int
        
        val result = Unification.unify(intType1, intType2)
        
        assertTrue(result.isSuccess(), "Unifying identical types should succeed")
        assertEquals(Substitution.empty, result.getSubstitution(), "Should return empty substitution")
    }
    
    @Test
    fun `unification of different basic types fails`() {
        val intType = Types.Int
        val stringType = Types.String
        
        val result = Unification.unify(intType, stringType)
        
        assertTrue(result.isFailure(), "Unifying different basic types should fail")
        assertNotNull(result.getError(), "Should have error message")
    }
    
    @Test
    fun `unification of type variable with concrete type`() {
        val var1 = TypeVariable.fresh()
        val intType = Types.Int
        
        val result = Unification.unify(var1, intType)
        
        assertTrue(result.isSuccess(), "Unifying type variable with concrete type should succeed")
        val substitution = result.getSubstitution()
        assertEquals(intType, substitution.apply(var1), "Variable should be mapped to concrete type")
    }
    
    @Test
    fun `unification of concrete type with type variable`() {
        val intType = Types.Int
        val var1 = TypeVariable.fresh()
        
        val result = Unification.unify(intType, var1)
        
        assertTrue(result.isSuccess(), "Unifying concrete type with type variable should succeed")
        val substitution = result.getSubstitution()
        assertEquals(intType, substitution.apply(var1), "Variable should be mapped to concrete type")
    }
    
    @Test
    fun `unification of two different type variables`() {
        val var1 = TypeVariable.fresh()
        val var2 = TypeVariable.fresh()
        
        val result = Unification.unify(var1, var2)
        
        assertTrue(result.isSuccess(), "Unifying two type variables should succeed")
        val substitution = result.getSubstitution()
        // One variable should be mapped to the other
        assertTrue(
            substitution.apply(var1) == var2 || substitution.apply(var2) == var1,
            "One variable should be mapped to the other"
        )
    }
    
    @Test
    fun `unification of same type variable with itself`() {
        val var1 = TypeVariable.fresh()
        
        val result = Unification.unify(var1, var1)
        
        assertTrue(result.isSuccess(), "Unifying variable with itself should succeed")
        assertEquals(Substitution.empty, result.getSubstitution(), "Should return empty substitution")
    }
    
    @Test
    fun `unification of simple function types`() {
        // Int -> String and Int -> String
        val func1 = FunctionType(Types.Int, Types.String)
        val func2 = FunctionType(Types.Int, Types.String)
        
        val result = Unification.unify(func1, func2)
        
        assertTrue(result.isSuccess(), "Unifying identical function types should succeed")
        assertEquals(Substitution.empty, result.getSubstitution(), "Should return empty substitution")
    }
    
    @Test
    fun `unification of function types with different domains fails`() {
        // Int -> String and Bool -> String
        val func1 = FunctionType(Types.Int, Types.String)
        val func2 = FunctionType(Types.Bool, Types.String)
        
        val result = Unification.unify(func1, func2)
        
        assertTrue(result.isFailure(), "Unifying function types with different domains should fail")
    }
    
    @Test
    fun `unification of function types with different codomains fails`() {
        // Int -> String and Int -> Bool
        val func1 = FunctionType(Types.Int, Types.String)
        val func2 = FunctionType(Types.Int, Types.Bool)
        
        val result = Unification.unify(func1, func2)
        
        assertTrue(result.isFailure(), "Unifying function types with different codomains should fail")
    }
    
    @Test
    fun `unification of function types with type variables`() {
        // a -> b and Int -> String
        val var1 = TypeVariable.fresh()
        val var2 = TypeVariable.fresh()
        val func1 = FunctionType(var1, var2)
        val func2 = FunctionType(Types.Int, Types.String)
        
        val result = Unification.unify(func1, func2)
        
        assertTrue(result.isSuccess(), "Unifying function types with variables should succeed")
        val substitution = result.getSubstitution()
        assertEquals(Types.Int, substitution.apply(var1), "Domain variable should be unified with Int")
        assertEquals(Types.String, substitution.apply(var2), "Codomain variable should be unified with String")
    }
    
    @Test
    fun `unification of tuple types with same structure`() {
        // (Int, String) and (Int, String)
        val tuple1 = TupleType(listOf(Types.Int, Types.String))
        val tuple2 = TupleType(listOf(Types.Int, Types.String))
        
        val result = Unification.unify(tuple1, tuple2)
        
        assertTrue(result.isSuccess(), "Unifying identical tuple types should succeed")
        assertEquals(Substitution.empty, result.getSubstitution(), "Should return empty substitution")
    }
    
    @Test
    fun `unification of tuple types with different lengths fails`() {
        // (Int, String) and (Int, String, Bool)
        val tuple1 = TupleType(listOf(Types.Int, Types.String))
        val tuple2 = TupleType(listOf(Types.Int, Types.String, Types.Bool))
        
        val result = Unification.unify(tuple1, tuple2)
        
        assertTrue(result.isFailure(), "Unifying tuples with different lengths should fail")
    }
    
    @Test
    fun `unification of tuple types with type variables`() {
        // (a, b) and (Int, String)
        val var1 = TypeVariable.fresh()
        val var2 = TypeVariable.fresh()
        val tuple1 = TupleType(listOf(var1, var2))
        val tuple2 = TupleType(listOf(Types.Int, Types.String))
        
        val result = Unification.unify(tuple1, tuple2)
        
        assertTrue(result.isSuccess(), "Unifying tuple types with variables should succeed")
        val substitution = result.getSubstitution()
        assertEquals(Types.Int, substitution.apply(var1), "First variable should be unified with Int")
        assertEquals(Types.String, substitution.apply(var2), "Second variable should be unified with String")
    }
    
    @Test
    fun `unification with substitutions is applied correctly`() {
        // Given: a already mapped to Int in existing substitution
        // Unify: a with String (should fail)
        val var1 = TypeVariable.fresh()
        val existingSubst = Substitution.single(var1, Types.Int)
        
        val result = Unification.unify(var1, Types.String, existingSubst)
        
        assertTrue(result.isFailure(), "Unifying already-substituted variable with incompatible type should fail")
    }
    
    @Test
    fun `occurs check prevents infinite types`() {
        // Try to unify: a with (a -> Int)
        val var1 = TypeVariable.fresh()
        val funcType = FunctionType(var1, Types.Int)
        
        val result = Unification.unify(var1, funcType)
        
        assertTrue(result.isFailure(), "Occurs check should prevent infinite types")
        assertTrue(
            result.getError()?.contains("occurs") == true || 
            result.getError()?.contains("infinite") == true,
            "Error should mention occurs check or infinite type"
        )
    }
    
    @Test
    fun `occurs check with nested types`() {
        // Try to unify: a with ((a -> Int) -> String)
        val var1 = TypeVariable.fresh()
        val innerFunc = FunctionType(var1, Types.Int)
        val outerFunc = FunctionType(innerFunc, Types.String)
        
        val result = Unification.unify(var1, outerFunc)
        
        assertTrue(result.isFailure(), "Occurs check should catch nested infinite types")
    }
    
    @Test
    fun `unification of complex nested types`() {
        // Unify: (a -> b) -> c with (Int -> String) -> Bool
        val var1 = TypeVariable.fresh()
        val var2 = TypeVariable.fresh()
        val var3 = TypeVariable.fresh()
        
        val innerFunc1 = FunctionType(var1, var2)
        val outerFunc1 = FunctionType(innerFunc1, var3)
        
        val innerFunc2 = FunctionType(Types.Int, Types.String)
        val outerFunc2 = FunctionType(innerFunc2, Types.Bool)
        
        val result = Unification.unify(outerFunc1, outerFunc2)
        
        assertTrue(result.isSuccess(), "Unifying complex nested types should succeed")
        val substitution = result.getSubstitution()
        assertEquals(Types.Int, substitution.apply(var1), "a should be unified with Int")
        assertEquals(Types.String, substitution.apply(var2), "b should be unified with String") 
        assertEquals(Types.Bool, substitution.apply(var3), "c should be unified with Bool")
    }
    
    @Test
    fun `unification of union types with same alternatives`() {
        // Int | String and Int | String
        val union1 = UnionType(setOf(Types.Int, Types.String))
        val union2 = UnionType(setOf(Types.Int, Types.String))
        
        val result = Unification.unify(union1, union2)
        
        assertTrue(result.isSuccess(), "Unifying identical union types should succeed")
        assertEquals(Substitution.empty, result.getSubstitution(), "Should return empty substitution")
    }
    
    @Test
    fun `unification of union types with different alternatives fails`() {
        // Int | String and Int | Bool
        val union1 = UnionType(setOf(Types.Int, Types.String))
        val union2 = UnionType(setOf(Types.Int, Types.Bool))
        
        val result = Unification.unify(union1, union2)
        
        assertTrue(result.isFailure(), "Unifying union types with different alternatives should fail")
    }
    
    @Test
    fun `unification of record types with same fields`() {
        // {name: String, age: Int} and {name: String, age: Int}
        val record1 = RecordType(mapOf("name" to Types.String, "age" to Types.Int))
        val record2 = RecordType(mapOf("name" to Types.String, "age" to Types.Int))
        
        val result = Unification.unify(record1, record2)
        
        assertTrue(result.isSuccess(), "Unifying identical record types should succeed")
        assertEquals(Substitution.empty, result.getSubstitution(), "Should return empty substitution")
    }
    
    @Test
    fun `unification of record types with different fields fails`() {
        // {name: String} and {age: Int}
        val record1 = RecordType(mapOf("name" to Types.String))
        val record2 = RecordType(mapOf("age" to Types.Int))
        
        val result = Unification.unify(record1, record2)
        
        assertTrue(result.isFailure(), "Unifying record types with different fields should fail")
    }
    
    @Test
    fun `unification of record types with type variables`() {
        // {name: a, age: b} and {name: String, age: Int}
        val var1 = TypeVariable.fresh()
        val var2 = TypeVariable.fresh()
        val record1 = RecordType(mapOf("name" to var1, "age" to var2))
        val record2 = RecordType(mapOf("name" to Types.String, "age" to Types.Int))
        
        val result = Unification.unify(record1, record2)
        
        assertTrue(result.isSuccess(), "Unifying record types with variables should succeed")
        val substitution = result.getSubstitution()
        assertEquals(Types.String, substitution.apply(var1), "name variable should be unified with String")
        assertEquals(Types.Int, substitution.apply(var2), "age variable should be unified with Int")
    }
    
    @Test
    fun `unification error messages are informative`() {
        val result = Unification.unify(Types.Int, Types.String)
        
        assertTrue(result.isFailure(), "Should fail")
        val error = result.getError()
        assertNotNull(error, "Should have error message")
        assertTrue(error!!.contains("Int"), "Error should mention Int")
        assertTrue(error.contains("String"), "Error should mention String")
    }
    
    @Test
    fun `unification with literal string types`() {
        // "success" and "success"
        val literal1 = Types.Success
        val literal2 = Types.Success
        
        val result = Unification.unify(literal1, literal2)
        
        assertTrue(result.isSuccess(), "Unifying identical literal string types should succeed")
        assertEquals(Substitution.empty, result.getSubstitution(), "Should return empty substitution")
    }
    
    @Test
    fun `unification of different literal string types fails`() {
        // "success" and "error"
        val success = Types.Success
        val error = Types.Error
        
        val result = Unification.unify(success, error)
        
        assertTrue(result.isFailure(), "Unifying different literal string types should fail")
    }
    
    @Test
    fun `unification transitivity through multiple variables`() {
        // Given: unify a with b, then b with Int
        // Result: both a and b should be unified with Int
        val var1 = TypeVariable.fresh()
        val var2 = TypeVariable.fresh()
        
        val result1 = Unification.unify(var1, var2)
        assertTrue(result1.isSuccess(), "First unification should succeed")
        
        val result2 = Unification.unify(var2, Types.Int, result1.getSubstitution())
        assertTrue(result2.isSuccess(), "Second unification should succeed")
        
        val finalSubst = result2.getSubstitution()
        assertEquals(Types.Int, finalSubst.apply(var1), "First variable should be transitively unified with Int")
        assertEquals(Types.Int, finalSubst.apply(var2), "Second variable should be unified with Int")
    }
    
    @Test
    fun `debug unification transitivity`() {
        val var1 = TypeVariable.fresh()
        val var2 = TypeVariable.fresh()
        
        // Step 1: unify var1 with var2
        val result1 = Unification.unify(var1, var2)
        assertTrue(result1.isSuccess(), "First unification should succeed")
        
        val subst1 = result1.getSubstitution()
        // After unifying var1 with var2, one should map to the other
        // Let's check what the substitution contains
        assertTrue(subst1.domain().isNotEmpty(), "First substitution should have mappings")
        
        // The substitution should make var1 and var2 equivalent
        val var1Applied = subst1.apply(var1)
        val var2Applied = subst1.apply(var2)
        assertTrue(var1Applied == var2Applied, "After first unification, var1 and var2 should be equivalent: var1Applied=$var1Applied, var2Applied=$var2Applied")
        
        // Step 2: unify var2 with Int using existing substitution
        val result2 = Unification.unify(var2, Types.Int, subst1)
        assertTrue(result2.isSuccess(), "Second unification should succeed")
        
        val subst2 = result2.getSubstitution()
        
        // Debug: let's see what's in the final substitution
        val finalVar1 = subst2.apply(var1)
        val finalVar2 = subst2.apply(var2)
        
        // These should both be Int due to transitivity
        assertEquals(Types.Int, finalVar2, "var2 should be unified with Int")
        assertEquals(Types.Int, finalVar1, "var1 should be transitively unified with Int: substitution=$subst2, finalVar1=$finalVar1")
    }
    
    @Test
    fun `test substitution composition for transitivity`() {
        val var1 = TypeVariable.fresh()
        val var2 = TypeVariable.fresh()
        
        // Create first substitution: var1 → var2
        val subst1 = Substitution.single(var1, var2)
        
        // Create second substitution: var2 → Int
        val subst2 = Substitution.single(var2, Types.Int)
        
        // Compose: subst2 ∘ subst1 should give var1 → Int, var2 → Int
        val composed = subst2.compose(subst1)
        
        assertEquals(Types.Int, composed.apply(var1), "Composition should resolve var1 → var2 → Int")
        assertEquals(Types.Int, composed.apply(var2), "Composition should map var2 → Int")
    }

    @Test
    fun `unification of open record with closed record`() {
        // {name: String | r} and {name: String, age: Int}
        // Expect r to unify with {age: Int} (a closed record)
        val rowVar = TypeVariable.fresh()
        val openRecord = RecordType(mapOf("name" to Types.String), rowVar)
        val closedRecord = RecordType(mapOf("name" to Types.String, "age" to Types.Int))

        val result = Unification.unify(openRecord, closedRecord)

        assertTrue(result.isSuccess(), "Unification of open record with compatible closed record should succeed. Error: ${result.getError()}")
        val substitution = result.getSubstitution()
        
        val expectedRowType = RecordType(mapOf("age" to Types.Int)) // r should become {age: Int}
        val actualRowType = substitution.apply(rowVar)

        assertEquals(expectedRowType, actualRowType, "Row variable 'r' should be unified with {age: Int}")

        // Verify the full type of openRecord after substitution
        val unifiedOpenRecord = substitution.apply(openRecord)
        // It should effectively be {name: String, age: Int}
        // This means it should be unifiable with the original closedRecord with an empty substitution
        val finalCheck = Unification.unify(unifiedOpenRecord, closedRecord)
        assertTrue(finalCheck.isSuccess(), "Unified open record should be equivalent to the closed record")
        assertEquals(Substitution.empty, finalCheck.getSubstitution(), "Final check should yield empty substitution")
    }

    @Test
    fun `unification of two open records with different fields`() {
        // {name: String | r1} and {age: Int | r2}
        // Expect: r1 unifies to {age: Int | r3}, r2 unifies to {name: String | r3}
        // The unified type is effectively {name: String, age: Int | r3}
        val r1 = TypeVariable.fresh()
        val r2 = TypeVariable.fresh()

        val record1 = RecordType(mapOf("name" to Types.String), r1)
        val record2 = RecordType(mapOf("age" to Types.Int), r2)

        val result = Unification.unify(record1, record2)
        assertTrue(result.isSuccess(), "Unification of two open records should succeed. Error: ${result.getError()}")
        
        val substitution = result.getSubstitution()

        // After unification, applying substitution to r1 and r2 should yield new record types
        // r1 -> {age: Int | r3}
        // r2 -> {name: String | r3}
        // where r3 is a fresh variable introduced during unification of row variables.

        val r1Unified = substitution.apply(r1)
        val r2Unified = substitution.apply(r2)

        assertTrue(r1Unified is RecordType, "r1 unified should be a RecordType. Was: $r1Unified")
        assertTrue(r2Unified is RecordType, "r2 unified should be a RecordType. Was: $r2Unified")

        val r1UnifiedRecord = r1Unified as RecordType
        val r2UnifiedRecord = r2Unified as RecordType

        assertEquals(mapOf("age" to Types.Int), r1UnifiedRecord.fields, "r1 unified record fields should be {age: Int}")
        assertNotNull(r1UnifiedRecord.rowVar, "r1 unified record should have a new row variable (r3)")
        
        assertEquals(mapOf("name" to Types.String), r2UnifiedRecord.fields, "r2 unified record fields should be {name: String}")
        assertNotNull(r2UnifiedRecord.rowVar, "r2 unified record should have a new row variable (r3)")

        // The new row variables from both unified records should be the same (r3)
        assertEquals(r1UnifiedRecord.rowVar, r2UnifiedRecord.rowVar, "The new row variables (r3) from both unified records must be identical")

        // The original records, when applied with the substitution, should become compatible
        val appliedRecord1 = substitution.apply(record1)
        val appliedRecord2 = substitution.apply(record2)

        assertTrue(appliedRecord1 is RecordType)
        assertTrue(appliedRecord2 is RecordType)

        val finalRecord1 = appliedRecord1 as RecordType
        val finalRecord2 = appliedRecord2 as RecordType
        
        // Both should now look like {name: String, age: Int | r3}
        val expectedFields = mapOf("name" to Types.String, "age" to Types.Int)
        assertEquals(expectedFields, finalRecord1.fields, "Applied record1 should have combined fields")
        assertEquals(expectedFields, finalRecord2.fields, "Applied record2 should have combined fields")
        assertEquals(r1UnifiedRecord.rowVar, finalRecord1.rowVar, "Applied record1 row var should be r3")
        assertEquals(r2UnifiedRecord.rowVar, finalRecord2.rowVar, "Applied record2 row var should be r3")
    }
    
    @Test
    fun `unification of open record with itself`() {
        val r = TypeVariable.fresh()
        val record = RecordType(mapOf("x" to Types.Int), r)

        val result = Unification.unify(record, record)
        assertTrue(result.isSuccess(), "Unifying an open record with itself should succeed. Error: ${result.getError()}")
        assertEquals(Substitution.empty, result.getSubstitution(), "Substitution should be empty")
    }

    @Test
    fun `unification of open record with identical open record (different row var instance)`() {
        val r1 = TypeVariable.fresh()
        val r2 = TypeVariable.fresh()
        val record1 = RecordType(mapOf("x" to Types.Int), r1)
        val record2 = RecordType(mapOf("x" to Types.Int), r2) // Same structure, different row var instance

        val result = Unification.unify(record1, record2)
        assertTrue(result.isSuccess(), "Unifying open records with same structure but different row vars should succeed. Error: ${result.getError()}")
        
        val sub = result.getSubstitution()
        // Expect r1 to be unified with r2 (or vice-versa)
        assertTrue(sub.apply(r1) == r2 || sub.apply(r2) == r1, "One row variable should be unified with the other")
    }

    @Test
    fun `unification of open record fails if fields conflict`() {
        // {x: Int | r1} and {x: String | r2}
        val r1 = TypeVariable.fresh()
        val r2 = TypeVariable.fresh()
        val record1 = RecordType(mapOf("x" to Types.Int), r1)
        val record2 = RecordType(mapOf("x" to Types.String), r2)

        val result = Unification.unify(record1, record2)
        assertTrue(result.isFailure(), "Unification should fail due to conflicting field types for 'x'")
    }

    @Test
    fun `unification of open record with closed record (open has extra field)`() {
        // {name: String, extra: Bool | r} and {name: String}
        // This should fail because 'extra: Bool' cannot be matched in the closed record,
        // and 'r' cannot become negative.
        val r = TypeVariable.fresh()
        val openRecord = RecordType(mapOf("name" to Types.String, "extra" to Types.Bool), r)
        val closedRecord = RecordType(mapOf("name" to Types.String))

        val result = Unification.unify(openRecord, closedRecord)
        assertTrue(result.isFailure(), "Unification should fail if open record has unmatched concrete fields against a closed record. Error: ${result.getError()}")
    }

    @Test
    fun `unification of open record row variable with empty record`() {
        // {x: Int | r} and {x: Int}
        // This is similar to `unification of open record with closed record` but tests r -> {} explicitly
        val r = TypeVariable.fresh()
        val openRecord = RecordType(mapOf("x" to Types.Int), r)
        val closedRecord = RecordType(mapOf("x" to Types.Int)) // No row var, implies empty row

        val result = Unification.unify(openRecord, closedRecord)
        assertTrue(result.isSuccess(), "Unification should succeed. Error: ${result.getError()}")
        val sub = result.getSubstitution()
        val rUnified = sub.apply(r)
        
        // r should unify to an empty record type
        assertEquals(RecordType(emptyMap()), rUnified, "Row variable 'r' should unify to an empty record.")
    }

    @Test
    fun `occurs check with row variable in record unification`() {
        // r and {x: Int | r}
        val r = TypeVariable.fresh()
        val record = RecordType(mapOf("x" to Types.Int), r)

        val result = Unification.unify(r, record)
        assertTrue(result.isFailure(), "Occurs check should fail for r ~ {x: Int | r}")
        assertTrue(result.getError()?.contains("occurs check", ignoreCase = true) == true || result.getError()?.contains("infinite type", ignoreCase = true) == true, "Error message should indicate an occurs check failure.")
    }

}
