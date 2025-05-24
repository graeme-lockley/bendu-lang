package io.littlelanguages.minibendu.typesystem

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.test.assertNull

/**
 * Comprehensive tests for Substitution class - Tasks 3 & 4 Complete
 * 
 * This test suite verifies the complete substitution functionality for
 * unification and type inference in the mini-bendu type system.
 */
class SubstitutionTest {
    
    @Test
    fun `empty substitution is identity`() {
        val empty = Substitution.empty
        val typeVar = TypeVariable.fresh()
        val intType = Types.Int
        
        // Empty substitution should not change any type
        assertEquals(typeVar, empty.apply(typeVar), "Empty substitution should not change type variables")
        assertEquals(intType, empty.apply(intType), "Empty substitution should not change primitive types")
        
        // Empty substitution should have empty domain and range
        assertTrue(empty.domain().isEmpty(), "Empty substitution should have empty domain")
        assertTrue(empty.range().isEmpty(), "Empty substitution should have empty range")
    }
    
    @Test
    fun `single variable substitution`() {
        val typeVar = TypeVariable.fresh()
        val intType = Types.Int
        val substitution = Substitution.single(typeVar, intType)
        
        // Should substitute the mapped variable
        assertEquals(intType, substitution.apply(typeVar), "Should substitute mapped type variable")
        
        // Should not affect other variables
        val otherVar = TypeVariable.fresh()
        assertEquals(otherVar, substitution.apply(otherVar), "Should not affect unmapped type variables")
        
        // Should not affect non-variable types
        assertEquals(Types.String, substitution.apply(Types.String), "Should not affect primitive types")
        
        // Domain and range should contain the mapping
        assertEquals(setOf(typeVar), substitution.domain(), "Domain should contain mapped variable")
        assertEquals(setOf(intType), substitution.range(), "Range should contain target type")
    }
    
    @Test
    fun `multiple variable substitutions using builder`() {
        val var1 = TypeVariable.fresh()
        val var2 = TypeVariable.fresh()
        val var3 = TypeVariable.fresh()
        
        val substitution = Substitution.builder()
            .add(var1, Types.Int)
            .add(var2, Types.String)
            .build()
        
        // Should substitute mapped variables
        assertEquals(Types.Int, substitution.apply(var1), "Should substitute first variable")
        assertEquals(Types.String, substitution.apply(var2), "Should substitute second variable")
        
        // Should not affect unmapped variables
        assertEquals(var3, substitution.apply(var3), "Should not affect unmapped variable")
        
        // Domain and range should be correct
        assertEquals(setOf(var1, var2), substitution.domain(), "Domain should contain all mapped variables")
        assertEquals(setOf(Types.Int, Types.String), substitution.range(), "Range should contain all target types")
    }
    
    @Test
    fun `composition of substitutions`() {
        val var1 = TypeVariable.fresh()
        val var2 = TypeVariable.fresh()
        val var3 = TypeVariable.fresh()
        
        // First substitution: var1 -> var2, var3 -> Int
        val sub1 = Substitution.builder()
            .add(var1, var2)
            .add(var3, Types.Int)
            .build()
        
        // Second substitution: var2 -> String
        val sub2 = Substitution.single(var2, Types.String)
        
        // Composition: sub2 ∘ sub1 should map var1 -> String (through var2)
        val composed = sub2.compose(sub1)
        
        assertEquals(Types.String, composed.apply(var1), "Composition should chain substitutions")
        assertEquals(Types.Int, composed.apply(var3), "Composition should preserve direct mappings")
        assertEquals(Types.String, composed.apply(var2), "Composition should include second substitution")
    }
    
    @Test
    fun `application to function types`() {
        val paramVar = TypeVariable.fresh()
        val returnVar = TypeVariable.fresh()
        val functionType = FunctionType(paramVar, returnVar)
        
        val substitution = Substitution.builder()
            .add(paramVar, Types.Int)
            .add(returnVar, Types.String)
            .build()
        
        val result = substitution.apply(functionType)
        val expected = FunctionType(Types.Int, Types.String)
        
        assertTrue(result.structurallyEquivalent(expected), "Substitution should apply to function types")
    }
    
    @Test
    fun `application to record types`() {
        val nameVar = TypeVariable.fresh()
        val ageVar = TypeVariable.fresh()
        
        val recordType = RecordType(mapOf(
            "name" to nameVar,
            "age" to ageVar
        ))
        
        val substitution = Substitution.builder()
            .add(nameVar, Types.String)
            .add(ageVar, Types.Int)
            .build()
        
        val result = substitution.apply(recordType)
        val expected = RecordType(mapOf(
            "name" to Types.String,
            "age" to Types.Int
        ))
        
        assertTrue(result.structurallyEquivalent(expected), "Substitution should apply to record types")
    }
    
    @Test
    fun `application to tuple types`() {
        val firstVar = TypeVariable.fresh()
        val secondVar = TypeVariable.fresh()
        val tupleType = TupleType(listOf(firstVar, secondVar, Types.Bool))
        
        val substitution = Substitution.builder()
            .add(firstVar, Types.Int)
            .add(secondVar, Types.String)
            .build()
        
        val result = substitution.apply(tupleType)
        val expected = TupleType(listOf(Types.Int, Types.String, Types.Bool))
        
        assertTrue(result.structurallyEquivalent(expected), "Substitution should apply to tuple types")
    }
    
    @Test
    fun `application to union types`() {
        val var1 = TypeVariable.fresh()
        val var2 = TypeVariable.fresh()
        val unionType = UnionType(setOf(var1, var2, Types.Unit))
        
        val substitution = Substitution.builder()
            .add(var1, Types.Int)
            .add(var2, Types.String)
            .build()
        
        val result = substitution.apply(unionType)
        val expected = UnionType(setOf(Types.Int, Types.String, Types.Unit))
        
        assertTrue(result.structurallyEquivalent(expected), "Substitution should apply to union types")
    }
    
    @Test
    fun `restriction operations`() {
        val var1 = TypeVariable.fresh()
        val var2 = TypeVariable.fresh()
        val var3 = TypeVariable.fresh()
        
        val substitution = Substitution.builder()
            .add(var1, Types.Int)
            .add(var2, Types.String)
            .add(var3, Types.Bool)
            .build()
        
        // Restrict to specific variables
        val restricted = substitution.restrictTo(setOf(var1, var3))
        
        assertEquals(Types.Int, restricted.apply(var1), "Restricted substitution should include var1")
        assertEquals(var2, restricted.apply(var2), "Restricted substitution should not include var2")
        assertEquals(Types.Bool, restricted.apply(var3), "Restricted substitution should include var3")
        
        assertEquals(setOf(var1, var3), restricted.domain(), "Restricted domain should be correct")
    }
    
    @Test
    fun `removal operations`() {
        val var1 = TypeVariable.fresh()
        val var2 = TypeVariable.fresh()
        val var3 = TypeVariable.fresh()
        
        val substitution = Substitution.builder()
            .add(var1, Types.Int)
            .add(var2, Types.String)
            .add(var3, Types.Bool)
            .build()
        
        // Remove specific variable
        val withoutVar2 = substitution.remove(var2)
        
        assertEquals(Types.Int, withoutVar2.apply(var1), "Should keep var1 mapping")
        assertEquals(var2, withoutVar2.apply(var2), "Should remove var2 mapping")
        assertEquals(Types.Bool, withoutVar2.apply(var3), "Should keep var3 mapping")
        
        assertEquals(setOf(var1, var3), withoutVar2.domain(), "Domain should exclude removed variable")
    }
    
    @Test
    fun `lookup operations`() {
        val var1 = TypeVariable.fresh()
        val var2 = TypeVariable.fresh()
        val var3 = TypeVariable.fresh()
        
        val substitution = Substitution.builder()
            .add(var1, Types.Int)
            .add(var2, Types.String)
            .build()
        
        // Lookup existing mappings
        assertEquals(Types.Int, substitution.lookup(var1), "Should find existing mapping")
        assertEquals(Types.String, substitution.lookup(var2), "Should find existing mapping")
        
        // Lookup non-existing mapping
        assertNull(substitution.lookup(var3), "Should return null for non-existing mapping")
    }
    
    @Test
    fun `substitution equality and hashCode`() {
        val var1 = TypeVariable.fresh()
        val var2 = TypeVariable.fresh()
        
        val sub1 = Substitution.builder()
            .add(var1, Types.Int)
            .add(var2, Types.String)
            .build()
        
        val sub2 = Substitution.builder()
            .add(var1, Types.Int)
            .add(var2, Types.String)
            .build()
        
        val sub3 = Substitution.builder()
            .add(var1, Types.String)  // Different mapping
            .add(var2, Types.String)
            .build()
        
        // Equal substitutions
        assertEquals(sub1, sub2, "Substitutions with same mappings should be equal")
        assertEquals(sub1.hashCode(), sub2.hashCode(), "Equal substitutions should have same hash code")
        
        // Different substitutions
        assertNotEquals(sub1, sub3, "Substitutions with different mappings should not be equal")
        assertNotEquals(Substitution.empty, sub1, "Empty and non-empty substitutions should not be equal")
    }
    
    @Test
    fun `substitution toString representation`() {
        val var1 = TypeVariable.fresh()
        val var2 = TypeVariable.fresh()
        
        // Empty substitution
        assertEquals("[]", Substitution.empty.toString(), "Empty substitution should display as []")
        
        // Single mapping
        val single = Substitution.single(var1, Types.Int)
        assertTrue(single.toString().contains("→"), "Single substitution should show mapping with arrow")
        assertTrue(single.toString().contains(var1.toString()), "Should contain variable")
        assertTrue(single.toString().contains("Int"), "Should contain target type")
        
        // Multiple mappings
        val multiple = Substitution.builder()
            .add(var1, Types.Int)
            .add(var2, Types.String)
            .build()
        assertTrue(multiple.toString().contains("→"), "Multiple substitutions should show mappings with arrows")
        assertTrue(multiple.toString().contains(","), "Multiple substitutions should separate with commas")
    }
    
    @Test
    fun `application to literal string types unchanged`() {
        val var1 = TypeVariable.fresh()
        val substitution = Substitution.single(var1, Types.Int)
        
        val literal = Types.literal("test")
        val result = substitution.apply(literal)
        
        assertEquals(literal, result, "Literal string types should remain unchanged")
        assertTrue(result.structurallyEquivalent(literal), "Should be structurally equivalent")
    }
    
    @Test
    fun `record type with row variables`() {
        val nameVar = TypeVariable.fresh()
        val rowVar = TypeVariable.fresh()
        
        val recordType = RecordType(
            mapOf("name" to nameVar),
            rowVar
        )
        
        val substitution = Substitution.builder()
            .add(nameVar, Types.String)
            .add(rowVar, TypeVariable.fresh())
            .build()
        
        val result = substitution.apply(recordType)
        
        assertTrue(result is RecordType, "Result should be a RecordType")
        val resultRecord = result
        assertEquals(Types.String, resultRecord.fields["name"], "Name field should be substituted")
        assertNotEquals(rowVar, resultRecord.rowVar, "Row variable should be substituted")
    }
}
