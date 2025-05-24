package io.littlelanguages.minibendu.typesystem

import io.littlelanguages.minibendu.*
import io.littlelanguages.scanpiler.LocationCoordinate
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Comprehensive tests for type inference of intersection types.
 * Task 45 of Phase 1 - Type Inference for Intersection Types.
 * 
 * Tests cover:
 * - Defining intersection types
 * - Inferring intersection types
 * - Subtyping with intersection types
 * - Intersection type simplification
 */
class TypeInferenceForIntersectionTypesTest {
    
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
    fun `basic intersection type creation and structure`() {
        // Test: Int & String intersection type
        val intersectionType = IntersectionType(setOf(Types.Int, Types.String))
        
        assertEquals(2, intersectionType.members.size, "Intersection should have 2 members")
        assertTrue(intersectionType.members.contains(Types.Int), "Intersection should contain Int")
        assertTrue(intersectionType.members.contains(Types.String), "Intersection should contain String")
        
        // Test string representation
        val intersectionStr = intersectionType.toString()
        assertTrue(intersectionStr.contains("Int"), "Intersection string should contain Int")
        assertTrue(intersectionStr.contains("String"), "Intersection string should contain String")
        assertTrue(intersectionStr.contains("&"), "Intersection string should contain ampersand separator")
    }
    
    @Test
    fun `intersection type with function types`() {
        // Test: (Int -> String) & (String -> Bool) intersection type
        val func1Type = FunctionType(Types.Int, Types.String)
        val func2Type = FunctionType(Types.String, Types.Bool)
        
        val funcIntersection = IntersectionType(setOf(func1Type, func2Type))
        
        assertEquals(2, funcIntersection.members.size, "Function intersection should have 2 members")
        assertTrue(funcIntersection.members.contains(func1Type), "Intersection should contain first function")
        assertTrue(funcIntersection.members.contains(func2Type), "Intersection should contain second function")
        
        // Test free type variables
        assertEquals(emptySet<TypeVariable>(), funcIntersection.freeTypeVariables(), 
                    "Function intersection should have no free type variables")
    }
    
    @Test
    fun `intersection type with type variables`() {
        // Test: A & B where A and B are type variables
        val varA = TypeVariable.fresh()
        val varB = TypeVariable.fresh()
        val intersectionType = IntersectionType(setOf(varA, varB))
        
        assertEquals(2, intersectionType.members.size, "Intersection should have 2 members")
        assertEquals(setOf(varA, varB), intersectionType.freeTypeVariables(), 
                    "Intersection should contain both type variables as free variables")
    }
    
    @Test
    fun `intersection type with record types`() {
        // Test: {name: String} & {age: Int} intersection type
        val nameRecord = RecordType(mapOf("name" to Types.String))
        val ageRecord = RecordType(mapOf("age" to Types.Int))
        
        val recordIntersection = IntersectionType(setOf(nameRecord, ageRecord))
        
        assertEquals(2, recordIntersection.members.size, "Record intersection should have 2 members")
        assertTrue(recordIntersection.members.contains(nameRecord), "Intersection should contain name record")
        assertTrue(recordIntersection.members.contains(ageRecord), "Intersection should contain age record")
    }
    
    @Test
    fun `intersection type structural equivalence`() {
        // Test: Int & String should be equivalent to String & Int (order doesn't matter)
        val intersection1 = IntersectionType(setOf(Types.Int, Types.String))
        val intersection2 = IntersectionType(setOf(Types.String, Types.Int))
        
        assertTrue(intersection1.structurallyEquivalent(intersection2), 
                  "Intersection types should be equivalent regardless of member order")
        
        // Test: Int & String should not be equivalent to Int & Bool
        val intersection3 = IntersectionType(setOf(Types.Int, Types.Bool))
        assertFalse(intersection1.structurallyEquivalent(intersection3), 
                   "Intersection types with different members should not be equivalent")
    }
    
    @Test
    fun `intersection type unification with identical intersections`() {
        // Test: (Int & String) unifies with (Int & String)
        val intersection1 = IntersectionType(setOf(Types.Int, Types.String))
        val intersection2 = IntersectionType(setOf(Types.Int, Types.String))
        
        val result = Unification.unify(intersection1, intersection2)
        
        assertTrue(result.isSuccess(), "Identical intersection types should unify successfully")
        assertEquals(Substitution.empty, result.getSubstitution(), 
                    "Unifying identical intersections should produce empty substitution")
    }
    
    @Test
    fun `intersection type unification with different intersections fails`() {
        // Test: (Int & String) should not unify with (Int & Bool)
        val intersection1 = IntersectionType(setOf(Types.Int, Types.String))
        val intersection2 = IntersectionType(setOf(Types.Int, Types.Bool))
        
        val result = Unification.unify(intersection1, intersection2)
        
        assertTrue(result.isFailure(), "Different intersection types should fail to unify")
        assertNotNull(result.getError(), "Failed unification should provide error message")
    }
    
    @Test
    fun `intersection type unification with reordered members`() {
        // Test: (Int & String) unifies with (String & Int)
        val intersection1 = IntersectionType(setOf(Types.Int, Types.String))
        val intersection2 = IntersectionType(setOf(Types.String, Types.Int))
        
        val result = Unification.unify(intersection1, intersection2)
        
        assertTrue(result.isSuccess(), "Intersection types with reordered members should unify")
        assertEquals(Substitution.empty, result.getSubstitution(), 
                    "Unifying reordered intersections should produce empty substitution")
    }
    
    @Test
    fun `intersection type with type variable unification`() {
        // Test: (A & String) unifies with (Int & String) where A becomes Int
        val varA = TypeVariable.fresh()
        val intersection1 = IntersectionType(setOf(varA, Types.String))
        val intersection2 = IntersectionType(setOf(Types.Int, Types.String))
        
        val result = Unification.unify(intersection1, intersection2)
        
        assertTrue(result.isSuccess(), "Intersection unification with type variables should succeed")
        val substitution = result.getSubstitution()
        
        // The type variable A should be unified with Int
        val resolvedA = substitution.apply(varA)
        assertEquals(Types.Int, resolvedA, "Type variable A should be unified with Int")
        
        // Verify the substitution works correctly
        val resolvedIntersection1 = substitution.apply(intersection1)
        assertTrue(resolvedIntersection1.structurallyEquivalent(intersection2), 
                  "Resolved intersection should be equivalent to target intersection")
    }
    
    @Test
    fun `intersection type subtyping relationships`() {
        // Test: (A & B) <: A and (A & B) <: B - intersection is subtype of its members
        val aType = Types.Int
        val bType = Types.String
        val intersectionType = IntersectionType(setOf(aType, bType))
        
        // Test that intersection contains both subtypes
        assertTrue(intersectionType.members.contains(aType), 
                  "Intersection should contain its subtype A")
        assertTrue(intersectionType.members.contains(bType), 
                  "Intersection should contain its subtype B")
        
        // Test subtyping relationships
        assertTrue(intersectionType.isSubtypeOf(aType), 
                  "Intersection should be subtype of its member A")
        assertTrue(intersectionType.isSubtypeOf(bType), 
                  "Intersection should be subtype of its member B")
    }
    
    @Test
    fun `intersection type as function parameter`() {
        // Test: f: (Int & String) -> Bool
        val paramIntersection = IntersectionType(setOf(Types.Int, Types.String))
        val funcType = FunctionType(paramIntersection, Types.Bool)
        
        // Test that function type is created correctly
        assertEquals(paramIntersection, funcType.domain, "Function domain should be intersection type")
        assertEquals(Types.Bool, funcType.codomain, "Function codomain should be Bool")
        
        // Test free type variables
        assertEquals(emptySet<TypeVariable>(), funcType.freeTypeVariables(), 
                    "Function with concrete intersection should have no free variables")
    }
    
    @Test
    fun `intersection type as function return type`() {
        // Test: f: Int -> (String & Bool)
        val returnIntersection = IntersectionType(setOf(Types.String, Types.Bool))
        val funcType = FunctionType(Types.Int, returnIntersection)
        
        assertEquals(Types.Int, funcType.domain, "Function domain should be Int")
        assertEquals(returnIntersection, funcType.codomain, "Function codomain should be intersection type")
    }
    
    @Test
    fun `intersection type with record field constraints`() {
        // Test: {readable: Bool} & {writable: Bool} representing file permissions
        val readableRecord = RecordType(mapOf("readable" to Types.Bool))
        val writableRecord = RecordType(mapOf("writable" to Types.Bool))
        
        val permissionIntersection = IntersectionType(setOf(readableRecord, writableRecord))
        
        assertEquals(2, permissionIntersection.members.size, "Permission intersection should have 2 members")
        assertTrue(permissionIntersection.members.contains(readableRecord), 
                  "Should contain readable constraint")
        assertTrue(permissionIntersection.members.contains(writableRecord), 
                  "Should contain writable constraint")
    }
    
    @Test
    fun `intersection type in tuple elements`() {
        // Test: (Int & String, Bool & Unit)
        val firstIntersection = IntersectionType(setOf(Types.Int, Types.String))
        val secondIntersection = IntersectionType(setOf(Types.Bool, Types.Unit))
        
        val tupleType = TupleType(listOf(firstIntersection, secondIntersection))
        
        assertEquals(2, tupleType.elements.size, "Tuple should have 2 elements")
        assertEquals(firstIntersection, tupleType.elements[0], "First element should be first intersection")
        assertEquals(secondIntersection, tupleType.elements[1], "Second element should be second intersection")
    }
    
    @Test
    fun `nested intersection types`() {
        // Test: (Int & String) & Bool - nested intersections should be flattened
        val innerIntersection = IntersectionType(setOf(Types.Int, Types.String))
        val nestedIntersection = IntersectionType(setOf(innerIntersection, Types.Bool))
        
        // Using normalized creation should flatten this
        val normalized = IntersectionType.create(setOf(innerIntersection, Types.Bool))
        
        assertTrue(normalized is IntersectionType, "Result should be intersection type")
        val normalizedIntersection = normalized as IntersectionType
        assertEquals(3, normalizedIntersection.members.size, "Flattened intersection should have 3 members")
        assertTrue(normalizedIntersection.members.contains(Types.Int), "Should contain Int")
        assertTrue(normalizedIntersection.members.contains(Types.String), "Should contain String")
        assertTrue(normalizedIntersection.members.contains(Types.Bool), "Should contain Bool")
    }
    
    @Test
    fun `intersection type with polymorphic types`() {
        // Test: Readable[A] & Writable[A] pattern
        val varA = TypeVariable.fresh()
        val readableType = TypeAlias("Readable", listOf(varA))
        val writableType = TypeAlias("Writable", listOf(varA))
        
        val rwIntersection = IntersectionType(setOf(readableType, writableType))
        
        assertEquals(2, rwIntersection.members.size, "RW intersection should have 2 members")
        assertEquals(setOf(varA), rwIntersection.freeTypeVariables(), 
                    "RW intersection should have A as free variable")
    }
    
    @Test
    fun `intersection type substitution application`() {
        // Test: Apply substitution to (A & String) where A := Int
        val varA = TypeVariable.fresh()
        val intersectionType = IntersectionType(setOf(varA, Types.String))
        
        val substitution = Substitution.single(varA, Types.Int)
        val result = substitution.apply(intersectionType)
        
        assertTrue(result is IntersectionType, "Result should be intersection type")
        val resultIntersection = result as IntersectionType
        
        assertEquals(2, resultIntersection.members.size, "Result should have 2 members")
        assertTrue(resultIntersection.members.contains(Types.Int), "Should contain Int after substitution")
        assertTrue(resultIntersection.members.contains(Types.String), "Should still contain String")
        assertFalse(resultIntersection.members.contains(varA), "Should no longer contain original variable")
    }
    
    @Test
    fun `intersection type equality and hash code`() {
        // Test: Equals and hashCode for intersection types
        val intersection1 = IntersectionType(setOf(Types.Int, Types.String))
        val intersection2 = IntersectionType(setOf(Types.Int, Types.String))
        val intersection3 = IntersectionType(setOf(Types.String, Types.Int)) // Different order
        val intersection4 = IntersectionType(setOf(Types.Int, Types.Bool))   // Different type
        
        // Equality tests
        assertEquals(intersection1, intersection2, "Identical intersections should be equal")
        assertEquals(intersection1, intersection3, "Order-different intersections should be equal (sets)")
        assertNotEquals(intersection1, intersection4, "Different intersections should not be equal")
        
        // Hash code tests
        assertEquals(intersection1.hashCode(), intersection2.hashCode(), "Equal intersections should have same hash code")
        assertEquals(intersection1.hashCode(), intersection3.hashCode(), "Order-different intersections should have same hash code")
    }
    
    @Test
    fun `intersection type minimum members requirement`() {
        // Test: Intersection types must have at least 2 members
        assertThrows(IllegalArgumentException::class.java) {
            IntersectionType(emptySet())
        }
        
        assertThrows(IllegalArgumentException::class.java) {
            IntersectionType(setOf(Types.Int))
        }
        
        // Should succeed with 2 or more
        assertDoesNotThrow {
            IntersectionType(setOf(Types.Int, Types.String))
            IntersectionType(setOf(Types.Int, Types.String, Types.Bool))
        }
    }
    
    @Test
    fun `intersection type simplification and normalization`() {
        // Test: Intersection type normalization (removing duplicates, flattening nested intersections)
        
        // Direct duplicate removal is handled by Set
        val duplicateTypes = setOf(Types.Int, Types.String, Types.Int)
        val noDuplicatesTypes = setOf(Types.Int, Types.String)
        assertEquals(noDuplicatesTypes, duplicateTypes, "Set should automatically remove duplicates")
        
        // Test intersection with itself
        val selfIntersection = IntersectionType(setOf(Types.Int, Types.String))
        assertTrue(selfIntersection.structurallyEquivalent(selfIntersection), "Intersection should be equivalent to itself")
        
        // Test simplification
        val simplified = selfIntersection.simplify()
        assertTrue(simplified is IntersectionType, "Simplified should remain intersection with multiple members")
        assertEquals(2, (simplified as IntersectionType).members.size, "Should maintain both members")
    }
    
    @Test
    fun `intersection type with constraint pattern`() {
        // Test: Constraint[A] = Readable[A] & Writable[A] & Comparable[A] pattern
        val varA = TypeVariable.fresh()
        val readableType = TypeAlias("Readable", listOf(varA))
        val writableType = TypeAlias("Writable", listOf(varA))
        val comparableType = TypeAlias("Comparable", listOf(varA))
        
        val constraintIntersection = IntersectionType(setOf(readableType, writableType, comparableType))
        
        assertEquals(3, constraintIntersection.members.size, "Constraint intersection should have 3 members")
        assertEquals(setOf(varA), constraintIntersection.freeTypeVariables(), 
                    "Constraint intersection should have A as free variable")
        
        // Test structural equivalence with reordered members
        val reorderedConstraint = IntersectionType(setOf(comparableType, readableType, writableType))
        assertTrue(constraintIntersection.structurallyEquivalent(reorderedConstraint), 
                  "Constraint intersections should be equivalent regardless of order")
    }
    
    @Test
    fun `intersection type bottom elimination`() {
        // Test: Intersection with impossible combinations
        // Note: This would be caught by the type checker as an error
        // For now, we just test the structural representation
        
        val stringInt = IntersectionType(setOf(Types.String, Types.Int))
        
        // These are logically impossible but structurally valid
        assertEquals(2, stringInt.members.size, "Impossible intersection should still be structurally valid")
        assertTrue(stringInt.members.contains(Types.String), "Should contain String")
        assertTrue(stringInt.members.contains(Types.Int), "Should contain Int")
    }
    
    @Test
    fun `intersection type performance with many members`() {
        // Test: Performance with intersections containing many members
        val startTime = System.currentTimeMillis()
        
        val manyTypes = (1..15).map { Types.literal("type$it") }.toSet()
        val largeIntersection = IntersectionType(manyTypes)
        
        assertEquals(15, largeIntersection.members.size, "Large intersection should have 15 members")
        
        // Test structural equivalence performance
        val sameIntersection = IntersectionType(manyTypes)
        assertTrue(largeIntersection.structurallyEquivalent(sameIntersection), 
                  "Large intersections should check equivalence efficiently")
        
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        
        assertTrue(duration < 1000, "Large intersection operations should complete quickly (${duration}ms)")
    }
    
    @Test
    fun `intersection type with trait pattern`() {
        // Test: Common trait intersection patterns
        
        // Serializable pattern: ToJson & FromJson
        val toJsonType = TypeAlias("ToJson", emptyList())
        val fromJsonType = TypeAlias("FromJson", emptyList())
        val serializableIntersection = IntersectionType(setOf(toJsonType, fromJsonType))
        
        assertEquals(2, serializableIntersection.members.size, "Serializable should have 2 members")
        assertEquals(emptySet<TypeVariable>(), serializableIntersection.freeTypeVariables(), 
                    "Serializable should have no free variables")
        
        // Bounded type parameter pattern: Display[A] & Debug[A] & Clone[A]
        val varA = TypeVariable.fresh()
        val displayType = TypeAlias("Display", listOf(varA))
        val debugType = TypeAlias("Debug", listOf(varA))
        val cloneType = TypeAlias("Clone", listOf(varA))
        val boundedIntersection = IntersectionType(setOf(displayType, debugType, cloneType))
        
        assertEquals(3, boundedIntersection.members.size, "Bounded should have 3 members")
        assertEquals(setOf(varA), boundedIntersection.freeTypeVariables(), 
                    "Bounded should have A as free variable")
    }
} 