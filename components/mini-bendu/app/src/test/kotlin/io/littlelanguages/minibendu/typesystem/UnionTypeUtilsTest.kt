package io.littlelanguages.minibendu.typesystem

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Tests for UnionTypeUtils utility functions.
 * Verifies the functionality added for Task 44 - Union Type Implementation.
 */
class UnionTypeUtilsTest {
    
    @Test
    fun `flatten nested union types`() {
        // Test: (A | (B | C)) | D should flatten to {A, B, C, D}
        val innerUnion = UnionType(setOf(Types.String, Types.Bool))
        val middleUnion = UnionType(setOf(Types.Int, innerUnion))
        val outerUnion = UnionType(setOf(middleUnion, Types.Unit))
        
        val flattened = UnionTypeUtils.flatten(outerUnion)
        
        assertEquals(4, flattened.size, "Should flatten to 4 distinct types")
        assertTrue(flattened.contains(Types.Int), "Should contain Int")
        assertTrue(flattened.contains(Types.String), "Should contain String")
        assertTrue(flattened.contains(Types.Bool), "Should contain Bool")
        assertTrue(flattened.contains(Types.Unit), "Should contain Unit")
    }
    
    @Test
    fun `exhaustiveness checking with complete coverage`() {
        // Test: Status union with all alternatives covered
        val statusUnion = UnionType(setOf(
            Types.literal("pending"),
            Types.literal("complete"),
            Types.literal("failed")
        ))
        
        val coveredTypes = setOf(
            Types.literal("pending"),
            Types.literal("complete"),
            Types.literal("failed")
        )
        
        assertTrue(UnionTypeUtils.isExhaustive(statusUnion, coveredTypes), 
                  "Should be exhaustive when all alternatives are covered")
    }
    
    @Test
    fun `exhaustiveness checking with incomplete coverage`() {
        // Test: Status union with missing alternatives
        val statusUnion = UnionType(setOf(
            Types.literal("pending"),
            Types.literal("complete"),
            Types.literal("failed")
        ))
        
        val coveredTypes = setOf(
            Types.literal("pending"),
            Types.literal("complete")
            // Missing "failed"
        )
        
        assertFalse(UnionTypeUtils.isExhaustive(statusUnion, coveredTypes), 
                   "Should not be exhaustive when alternatives are missing")
        
        val missing = UnionTypeUtils.findMissingAlternatives(statusUnion, coveredTypes)
        assertEquals(1, missing.size, "Should find 1 missing alternative")
        assertTrue(missing.contains(Types.literal("failed")), "Should identify failed as missing")
    }
    
    @Test
    fun `discriminated union detection`() {
        // Test: Union of string literals is discriminated
        val statusUnion = UnionType(setOf(
            Types.literal("success"),
            Types.literal("error"),
            Types.literal("pending")
        ))
        
        assertTrue(UnionTypeUtils.isDiscriminatedUnion(statusUnion), 
                  "String literal union should be discriminated")
        
        val values = UnionTypeUtils.getDiscriminatedValues(statusUnion)
        assertEquals(setOf("success", "error", "pending"), values, 
                    "Should extract correct literal values")
    }
    
    @Test
    fun `non-discriminated union detection`() {
        // Test: Union with mixed types is not discriminated
        val mixedUnion = UnionType(setOf(
            Types.literal("success"),
            Types.Int
        ))
        
        assertFalse(UnionTypeUtils.isDiscriminatedUnion(mixedUnion), 
                   "Mixed type union should not be discriminated")
    }
    
    @Test
    fun `union intersection`() {
        // Test: Intersection of overlapping unions
        val union1 = UnionType(setOf(Types.Int, Types.String, Types.Bool))
        val union2 = UnionType(setOf(Types.String, Types.Bool, Types.Unit))
        
        val intersection = UnionTypeUtils.intersect(union1, union2)
        
        assertEquals(2, intersection.size, "Intersection should have 2 elements")
        assertTrue(intersection.contains(Types.String), "Should contain String")
        assertTrue(intersection.contains(Types.Bool), "Should contain Bool")
    }
    
    @Test
    fun `union difference`() {
        // Test: Difference between unions
        val union1 = UnionType(setOf(Types.Int, Types.String, Types.Bool))
        val union2 = UnionType(setOf(Types.String, Types.Bool, Types.Unit))
        
        val difference = UnionTypeUtils.difference(union1, union2)
        
        assertEquals(1, difference.size, "Difference should have 1 element")
        assertTrue(difference.contains(Types.Int), "Should contain Int")
    }
    
    @Test
    fun `union simplification`() {
        // Test: Simplification removes redundancy
        val duplicatedUnion = UnionType(setOf(Types.Int, Types.String))
        val simplified = UnionTypeUtils.simplify(duplicatedUnion)
        
        assertTrue(simplified is UnionType, "Should remain union with multiple alternatives")
        val simplifiedUnion = simplified as UnionType
        assertEquals(2, simplifiedUnion.alternatives.size, "Should maintain both alternatives")
    }
    
    @Test
    fun `union membership checking`() {
        // Test: Membership with subtype considerations
        val stringUnion = UnionType(setOf(Types.String, Types.Int))
        
        assertTrue(UnionTypeUtils.isMemberOf(Types.String, stringUnion), 
                  "Direct member should be recognized")
        assertTrue(UnionTypeUtils.isMemberOf(Types.literal("test"), stringUnion), 
                  "String literal should be member of String union")
    }
    
    @Test
    fun `union normalization and creation`() {
        // Test: Creating unions with automatic normalization
        val types = listOf(Types.Int, Types.String, Types.Bool)
        val union = UnionTypeUtils.createUnion(types)
        
        assertTrue(union is UnionType, "Should create union type")
        val unionType = union as UnionType
        assertEquals(3, unionType.alternatives.size, "Should have all 3 alternatives")
    }
    
    @Test
    fun `single type union creation`() {
        // Test: Single type should not create union
        val singleType = UnionTypeUtils.createUnion(listOf(Types.Int))
        
        assertEquals(Types.Int, singleType, "Single type should return the type itself")
    }
    
    @Test
    fun `union overlap checking`() {
        // Test: Checking if unions have overlapping alternatives
        val union1 = UnionType(setOf(Types.Int, Types.String))
        val union2 = UnionType(setOf(Types.String, Types.Bool))
        val union3 = UnionType(setOf(Types.Unit, Types.Bool))
        
        assertTrue(UnionTypeUtils.hasOverlap(union1, union2), 
                  "Unions with String in common should overlap")
        assertFalse(UnionTypeUtils.hasOverlap(union1, union3), 
                   "Unions without common types should not overlap")
    }
    
    @Test
    fun `union merging`() {
        // Test: Merging two unions
        val union1 = UnionType(setOf(Types.Int, Types.String))
        val union2 = UnionType(setOf(Types.Bool, Types.Unit))
        
        val merged = UnionTypeUtils.merge(union1, union2)
        
        assertEquals(4, merged.alternatives.size, "Merged union should have 4 alternatives")
        assertTrue(merged.alternatives.contains(Types.Int), "Should contain Int")
        assertTrue(merged.alternatives.contains(Types.String), "Should contain String")
        assertTrue(merged.alternatives.contains(Types.Bool), "Should contain Bool")
        assertTrue(merged.alternatives.contains(Types.Unit), "Should contain Unit")
    }
    
    @Test
    fun `type variable extraction`() {
        // Test: Extracting type variables from union
        val varA = TypeVariable.fresh()
        val varB = TypeVariable.fresh()
        val unionWithVars = UnionType(setOf(varA, Types.String, varB))
        
        val extracted = UnionTypeUtils.extractTypeVariables(unionWithVars)
        
        assertEquals(2, extracted.size, "Should extract 2 type variables")
        assertTrue(extracted.contains(varA), "Should contain varA")
        assertTrue(extracted.contains(varB), "Should contain varB")
    }
    
    @Test
    fun `concrete type checking`() {
        // Test: Checking if union contains only concrete types
        val concreteUnion = UnionType(setOf(Types.Int, Types.String))
        val unionWithVars = UnionType(setOf(TypeVariable.fresh(), Types.String))
        
        assertTrue(UnionTypeUtils.isConcrete(concreteUnion), 
                  "Union with only concrete types should be concrete")
        assertFalse(UnionTypeUtils.isConcrete(unionWithVars), 
                   "Union with type variables should not be concrete")
    }
    
    @Test
    fun `union normalization through companion object`() {
        // Test: Using UnionType.create for normalization
        val nestedTypes = setOf(
            Types.Int,
            UnionType(setOf(Types.String, Types.Bool)),
            Types.Unit
        )
        
        val normalized = UnionType.create(nestedTypes)
        
        assertTrue(normalized is UnionType, "Should create normalized union")
        val unionType = normalized as UnionType
        assertEquals(4, unionType.alternatives.size, "Should flatten nested union")
        assertTrue(unionType.alternatives.contains(Types.Int), "Should contain Int")
        assertTrue(unionType.alternatives.contains(Types.String), "Should contain String")
        assertTrue(unionType.alternatives.contains(Types.Bool), "Should contain Bool")
        assertTrue(unionType.alternatives.contains(Types.Unit), "Should contain Unit")
    }
} 