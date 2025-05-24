package io.littlelanguages.minibendu.typesystem

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Tests for IntersectionTypeUtils utility functions.
 * Verifies the functionality added for Task 46 - Intersection Type Implementation.
 */
class IntersectionTypeUtilsTest {
    
    @Test
    fun `flatten nested intersection types`() {
        // Test: (A & (B & C)) & D should flatten to {A, B, C, D}
        val innerIntersection = IntersectionType(setOf(Types.String, Types.Bool))
        val middleIntersection = IntersectionType(setOf(Types.Int, innerIntersection))
        val outerIntersection = IntersectionType(setOf(middleIntersection, Types.Unit))
        
        val flattened = IntersectionTypeUtils.flatten(outerIntersection)
        
        assertEquals(4, flattened.size, "Should flatten to 4 distinct types")
        assertTrue(flattened.contains(Types.Int), "Should contain Int")
        assertTrue(flattened.contains(Types.String), "Should contain String")
        assertTrue(flattened.contains(Types.Bool), "Should contain Bool")
        assertTrue(flattened.contains(Types.Unit), "Should contain Unit")
    }
    
    @Test
    fun `constraint satisfaction with compatible types`() {
        // Test: Type satisfies all constraints
        val readableConstraint = TypeAlias("Readable", emptyList())
        val writableConstraint = TypeAlias("Writable", emptyList())
        val constraintIntersection = IntersectionType(setOf(readableConstraint, writableConstraint))
        
        // Create a type that should satisfy the constraints
        val compatibleType = IntersectionType(setOf(readableConstraint, writableConstraint, Types.String))
        
        assertTrue(IntersectionTypeUtils.satisfiesAllConstraints(compatibleType, constraintIntersection), 
                  "Compatible type should satisfy all constraints")
    }
    
    @Test
    fun `constraint satisfaction with incompatible types`() {
        // Test: Type doesn't satisfy all constraints
        val readableConstraint = TypeAlias("Readable", emptyList())
        val writableConstraint = TypeAlias("Writable", emptyList())
        val constraintIntersection = IntersectionType(setOf(readableConstraint, writableConstraint))
        
        // Type that only satisfies one constraint
        val partialType = IntersectionType(setOf(readableConstraint, Types.String))
        
        assertFalse(IntersectionTypeUtils.satisfiesAllConstraints(partialType, constraintIntersection), 
                   "Partial type should not satisfy all constraints")
        
        val unsatisfied = IntersectionTypeUtils.findUnsatisfiedConstraints(partialType, constraintIntersection)
        assertEquals(1, unsatisfied.size, "Should find 1 unsatisfied constraint")
        assertTrue(unsatisfied.contains(writableConstraint), "Should identify writable as unsatisfied")
    }
    
    @Test
    fun `constraint intersection detection`() {
        // Test: Intersection of type aliases is constraint intersection
        val constraint1 = TypeAlias("Display", emptyList())
        val constraint2 = TypeAlias("Debug", emptyList())
        val constraint3 = TypeAlias("Clone", emptyList())
        val constraintIntersection = IntersectionType(setOf(constraint1, constraint2, constraint3))
        
        assertTrue(IntersectionTypeUtils.isConstraintIntersection(constraintIntersection), 
                  "Type alias intersection should be constraint intersection")
        
        val names = IntersectionTypeUtils.getConstraintNames(constraintIntersection)
        assertEquals(setOf("Display", "Debug", "Clone"), names, 
                    "Should extract correct constraint names")
    }
    
    @Test
    fun `non-constraint intersection detection`() {
        // Test: Intersection with mixed types is not constraint intersection
        val mixedIntersection = IntersectionType(setOf(
            TypeAlias("Readable", emptyList()),
            Types.Int
        ))
        
        assertFalse(IntersectionTypeUtils.isConstraintIntersection(mixedIntersection), 
                   "Mixed type intersection should not be constraint intersection")
    }
    
    @Test
    fun `intersection type intersection`() {
        // Test: Intersection of overlapping intersections
        val intersection1 = IntersectionType(setOf(Types.Int, Types.String, Types.Bool))
        val intersection2 = IntersectionType(setOf(Types.String, Types.Bool, Types.Unit))
        
        val combined = IntersectionTypeUtils.intersect(intersection1, intersection2)
        
        assertEquals(4, combined.members.size, "Combined intersection should have 4 unique members")
        assertTrue(combined.members.contains(Types.Int), "Should contain Int")
        assertTrue(combined.members.contains(Types.String), "Should contain String")
        assertTrue(combined.members.contains(Types.Bool), "Should contain Bool")
        assertTrue(combined.members.contains(Types.Unit), "Should contain Unit")
    }
    
    @Test
    fun `intersection difference`() {
        // Test: Difference between intersections
        val intersection1 = IntersectionType(setOf(Types.Int, Types.String, Types.Bool))
        val intersection2 = IntersectionType(setOf(Types.String, Types.Bool, Types.Unit))
        
        val difference = IntersectionTypeUtils.difference(intersection1, intersection2)
        
        assertEquals(1, difference.size, "Difference should have 1 element")
        assertTrue(difference.contains(Types.Int), "Should contain Int")
    }
    
    @Test
    fun `intersection simplification`() {
        // Test: Simplification removes redundancy
        val duplicatedIntersection = IntersectionType(setOf(Types.Int, Types.String))
        val simplified = IntersectionTypeUtils.simplify(duplicatedIntersection)
        
        assertTrue(simplified is IntersectionType, "Should remain intersection with multiple members")
        val simplifiedIntersection = simplified as IntersectionType
        assertEquals(2, simplifiedIntersection.members.size, "Should maintain both members")
    }
    
    @Test
    fun `intersection type compatibility checking`() {
        // Test: Compatibility with constraint considerations
        val stringIntersection = IntersectionType(setOf(Types.String, Types.Int))
        
        assertTrue(IntersectionTypeUtils.isCompatibleWith(Types.String, Types.String), 
                  "Direct member should be compatible")
        assertTrue(IntersectionTypeUtils.isCompatibleWith(stringIntersection, Types.String), 
                  "Intersection containing type should be compatible")
    }
    
    @Test
    fun `intersection normalization and creation`() {
        // Test: Creating intersections with automatic normalization
        val types = listOf(Types.Int, Types.String, Types.Bool)
        val intersection = IntersectionTypeUtils.createIntersection(types)
        
        assertTrue(intersection is IntersectionType, "Should create intersection type")
        val intersectionType = intersection as IntersectionType
        assertEquals(3, intersectionType.members.size, "Should have all 3 members")
    }
    
    @Test
    fun `single type intersection creation`() {
        // Test: Single type should not create intersection
        val singleType = IntersectionTypeUtils.createIntersection(listOf(Types.Int))
        
        assertEquals(Types.Int, singleType, "Single type should return the type itself")
    }
    
    @Test
    fun `intersection overlap checking`() {
        // Test: Checking if intersections have overlapping members
        val intersection1 = IntersectionType(setOf(Types.Int, Types.String))
        val intersection2 = IntersectionType(setOf(Types.String, Types.Bool))
        val intersection3 = IntersectionType(setOf(Types.Unit, Types.Bool))
        
        assertTrue(IntersectionTypeUtils.hasOverlap(intersection1, intersection2), 
                  "Intersections with String in common should overlap")
        assertFalse(IntersectionTypeUtils.hasOverlap(intersection1, intersection3), 
                   "Intersections without common types should not overlap")
    }
    
    @Test
    fun `intersection merging`() {
        // Test: Merging two intersections
        val intersection1 = IntersectionType(setOf(Types.Int, Types.String))
        val intersection2 = IntersectionType(setOf(Types.Bool, Types.Unit))
        
        val merged = IntersectionTypeUtils.merge(intersection1, intersection2)
        
        assertEquals(4, merged.members.size, "Merged intersection should have 4 members")
        assertTrue(merged.members.contains(Types.Int), "Should contain Int")
        assertTrue(merged.members.contains(Types.String), "Should contain String")
        assertTrue(merged.members.contains(Types.Bool), "Should contain Bool")
        assertTrue(merged.members.contains(Types.Unit), "Should contain Unit")
    }
    
    @Test
    fun `type variable extraction`() {
        // Test: Extracting type variables from intersection
        val varA = TypeVariable.fresh()
        val varB = TypeVariable.fresh()
        val intersectionWithVars = IntersectionType(setOf(varA, Types.String, varB))
        
        val extracted = IntersectionTypeUtils.extractTypeVariables(intersectionWithVars)
        
        assertEquals(2, extracted.size, "Should extract 2 type variables")
        assertTrue(extracted.contains(varA), "Should contain varA")
        assertTrue(extracted.contains(varB), "Should contain varB")
    }
    
    @Test
    fun `concrete type checking`() {
        // Test: Checking if intersection contains only concrete types
        val concreteIntersection = IntersectionType(setOf(Types.Int, Types.String))
        val intersectionWithVars = IntersectionType(setOf(TypeVariable.fresh(), Types.String))
        
        assertTrue(IntersectionTypeUtils.isConcrete(concreteIntersection), 
                  "Intersection with only concrete types should be concrete")
        assertFalse(IntersectionTypeUtils.isConcrete(intersectionWithVars), 
                   "Intersection with type variables should not be concrete")
    }
    
    @Test
    fun `intersection satisfiability checking`() {
        // Test: Satisfiable intersections
        val satisfiableIntersection = IntersectionType(setOf(
            TypeAlias("Readable", emptyList()),
            TypeAlias("Writable", emptyList())
        ))
        
        assertTrue(IntersectionTypeUtils.isSatisfiable(satisfiableIntersection), 
                  "Constraint intersection should be satisfiable")
        
        // Test: Unsatisfiable intersections (multiple different primitives)
        val unsatisfiableIntersection = IntersectionType(setOf(Types.Int, Types.String))
        
        assertFalse(IntersectionTypeUtils.isSatisfiable(unsatisfiableIntersection), 
                   "Multiple primitive intersection should be unsatisfiable")
    }
    
    @Test
    fun `intersection normalization through companion object`() {
        // Test: Using IntersectionType.create for normalization
        val nestedTypes = setOf(
            Types.Int,
            IntersectionType(setOf(Types.String, Types.Bool)),
            Types.Unit
        )
        
        val normalized = IntersectionType.create(nestedTypes)
        
        assertTrue(normalized is IntersectionType, "Should create normalized intersection")
        val intersectionType = normalized as IntersectionType
        assertEquals(4, intersectionType.members.size, "Should flatten nested intersection")
        assertTrue(intersectionType.members.contains(Types.Int), "Should contain Int")
        assertTrue(intersectionType.members.contains(Types.String), "Should contain String")
        assertTrue(intersectionType.members.contains(Types.Bool), "Should contain Bool")
        assertTrue(intersectionType.members.contains(Types.Unit), "Should contain Unit")
    }
    
    @Test
    fun `most specific type finding`() {
        // Test: Finding most specific types in intersection
        val intersection = IntersectionType(setOf(
            Types.String,
            Types.literal("specific"),
            TypeAlias("Readable", emptyList())
        ))
        
        val mostSpecific = IntersectionTypeUtils.findMostSpecific(intersection)
        
        // String literal should be more specific than general String
        assertTrue(mostSpecific.contains(Types.literal("specific")), 
                  "Should include specific string literal")
        assertTrue(mostSpecific.contains(TypeAlias("Readable", emptyList())), 
                  "Should include unrelated constraint")
        // General String should be filtered out as redundant
        assertEquals(2, mostSpecific.size, "Should have 2 most specific types")
    }
    
    @Test
    fun `upper bounds extraction`() {
        // Test: Getting upper bounds from intersection
        val constraint1 = TypeAlias("Display", emptyList())
        val constraint2 = TypeAlias("Debug", emptyList())
        val intersection = IntersectionType(setOf(constraint1, constraint2))
        
        val upperBounds = IntersectionTypeUtils.getUpperBounds(intersection)
        
        assertEquals(2, upperBounds.size, "Should have 2 upper bounds")
        assertTrue(upperBounds.contains(constraint1), "Should contain Display constraint")
        assertTrue(upperBounds.contains(constraint2), "Should contain Debug constraint")
    }
    
    @Test
    fun `valid constraint checking`() {
        // Test: Valid constraints
        val validConstraint = IntersectionType(setOf(
            TypeAlias("Readable", emptyList()),
            TypeAlias("Writable", emptyList())
        ))
        
        assertTrue(IntersectionTypeUtils.isValidConstraint(validConstraint), 
                  "Satisfiable constraint intersection should be valid")
        
        // Test: Invalid constraints (unsatisfiable)
        val invalidConstraint = IntersectionType(setOf(Types.Int, Types.String))
        
        assertFalse(IntersectionTypeUtils.isValidConstraint(invalidConstraint), 
                   "Unsatisfiable intersection should be invalid constraint")
    }
    
    @Test
    fun `intersection formatting`() {
        // Test: Formatting intersection types
        val intersection = IntersectionType(setOf(Types.String, Types.Int, Types.Bool))
        val formatted = IntersectionTypeUtils.formatIntersection(intersection)
        
        // Should contain all types and separators
        assertTrue(formatted.contains("&"), "Should contain ampersand separators")
        assertTrue(formatted.contains("Int"), "Should contain Int")
        assertTrue(formatted.contains("String"), "Should contain String")
        assertTrue(formatted.contains("Bool"), "Should contain Bool")
    }
} 