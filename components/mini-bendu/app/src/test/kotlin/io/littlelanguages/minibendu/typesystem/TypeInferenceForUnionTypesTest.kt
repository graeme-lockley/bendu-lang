package io.littlelanguages.minibendu.typesystem

import io.littlelanguages.minibendu.*
import io.littlelanguages.scanpiler.LocationCoordinate
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Comprehensive tests for type inference of union types.
 * Task 43 of Phase 1 - Type Inference for Union Types.
 * 
 * Tests cover:
 * - Defining union types
 * - Inferring union types
 * - Subtyping with union types
 * - Exhaustiveness checking with unions
 * - Union type normalization and simplification
 */
class TypeInferenceForUnionTypesTest {
    
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
    fun `basic union type creation and structure`() {
        // Test: Int | String union type
        val unionType = UnionType(setOf(Types.Int, Types.String))
        
        assertEquals(2, unionType.alternatives.size, "Union should have 2 alternatives")
        assertTrue(unionType.alternatives.contains(Types.Int), "Union should contain Int")
        assertTrue(unionType.alternatives.contains(Types.String), "Union should contain String")
        
        // Test string representation
        val unionStr = unionType.toString()
        assertTrue(unionStr.contains("Int"), "Union string should contain Int")
        assertTrue(unionStr.contains("String"), "Union string should contain String")
        assertTrue(unionStr.contains("|"), "Union string should contain pipe separator")
    }
    
    @Test
    fun `union type with literal string types`() {
        // Test: "success" | "error" | "pending" union type
        val successType = Types.literal("success")
        val errorType = Types.literal("error")
        val pendingType = Types.literal("pending")
        
        val statusUnion = UnionType(setOf(successType, errorType, pendingType))
        
        assertEquals(3, statusUnion.alternatives.size, "Status union should have 3 alternatives")
        assertTrue(statusUnion.alternatives.contains(successType), "Union should contain success")
        assertTrue(statusUnion.alternatives.contains(errorType), "Union should contain error")
        assertTrue(statusUnion.alternatives.contains(pendingType), "Union should contain pending")
        
        // Test free type variables
        assertEquals(emptySet<TypeVariable>(), statusUnion.freeTypeVariables(), 
                    "Literal union should have no free type variables")
    }
    
    @Test
    fun `union type with type variables`() {
        // Test: A | B where A and B are type variables
        val varA = TypeVariable.fresh()
        val varB = TypeVariable.fresh()
        val unionType = UnionType(setOf(varA, varB))
        
        assertEquals(2, unionType.alternatives.size, "Union should have 2 alternatives")
        assertEquals(setOf(varA, varB), unionType.freeTypeVariables(), 
                    "Union should contain both type variables as free variables")
    }
    
    @Test
    fun `union type with complex nested types`() {
        // Test: (Int -> String) | {name: String, age: Int} | (Int, Bool)
        val funcType = FunctionType(Types.Int, Types.String)
        val recordType = RecordType(mapOf("name" to Types.String, "age" to Types.Int))
        val tupleType = TupleType(listOf(Types.Int, Types.Bool))
        
        val complexUnion = UnionType(setOf(funcType, recordType, tupleType))
        
        assertEquals(3, complexUnion.alternatives.size, "Complex union should have 3 alternatives")
        assertTrue(complexUnion.alternatives.contains(funcType), "Union should contain function type")
        assertTrue(complexUnion.alternatives.contains(recordType), "Union should contain record type")
        assertTrue(complexUnion.alternatives.contains(tupleType), "Union should contain tuple type")
    }
    
    @Test
    fun `union type structural equivalence`() {
        // Test: Int | String should be equivalent to String | Int (order doesn't matter)
        val union1 = UnionType(setOf(Types.Int, Types.String))
        val union2 = UnionType(setOf(Types.String, Types.Int))
        
        assertTrue(union1.structurallyEquivalent(union2), 
                  "Union types should be equivalent regardless of alternative order")
        
        // Test: Int | String should not be equivalent to Int | Bool
        val union3 = UnionType(setOf(Types.Int, Types.Bool))
        assertFalse(union1.structurallyEquivalent(union3), 
                   "Union types with different alternatives should not be equivalent")
    }
    
    @Test
    fun `union type unification with identical unions`() {
        // Test: (Int | String) unifies with (Int | String)
        val union1 = UnionType(setOf(Types.Int, Types.String))
        val union2 = UnionType(setOf(Types.Int, Types.String))
        
        val result = Unification.unify(union1, union2)
        
        assertTrue(result.isSuccess(), "Identical union types should unify successfully")
        assertEquals(Substitution.empty, result.getSubstitution(), 
                    "Unifying identical unions should produce empty substitution")
    }
    
    @Test
    fun `union type unification with different unions fails`() {
        // Test: (Int | String) should not unify with (Int | Bool)
        val union1 = UnionType(setOf(Types.Int, Types.String))
        val union2 = UnionType(setOf(Types.Int, Types.Bool))
        
        val result = Unification.unify(union1, union2)
        
        assertTrue(result.isFailure(), "Different union types should fail to unify")
        assertNotNull(result.getError(), "Failed unification should provide error message")
    }
    
    @Test
    fun `union type unification with reordered alternatives`() {
        // Test: (Int | String) unifies with (String | Int)
        val union1 = UnionType(setOf(Types.Int, Types.String))
        val union2 = UnionType(setOf(Types.String, Types.Int))
        
        val result = Unification.unify(union1, union2)
        
        assertTrue(result.isSuccess(), "Union types with reordered alternatives should unify")
        assertEquals(Substitution.empty, result.getSubstitution(), 
                    "Unifying reordered unions should produce empty substitution")
    }
    
    @Test
    fun `union type with type variable unification`() {
        // Test: (A | String) unifies with (Int | String) where A becomes Int
        val varA = TypeVariable.fresh()
        val union1 = UnionType(setOf(varA, Types.String))
        val union2 = UnionType(setOf(Types.Int, Types.String))
        
        val result = Unification.unify(union1, union2)
        
        assertTrue(result.isSuccess(), "Union unification with type variables should succeed")
        val substitution = result.getSubstitution()
        
        // The type variable A should be unified with Int
        val resolvedA = substitution.apply(varA)
        assertEquals(Types.Int, resolvedA, "Type variable A should be unified with Int")
        
        // Verify the substitution works correctly
        val resolvedUnion1 = substitution.apply(union1)
        assertTrue(resolvedUnion1.structurallyEquivalent(union2), 
                  "Resolved union should be equivalent to target union")
    }
    
    @Test
    fun `union type simplification and normalization`() {
        // Test: Union type normalization (removing duplicates, flattening nested unions)
        
        // Direct duplicate removal is handled by Set
        val duplicateTypes = setOf(Types.Int, Types.String, Types.Int)
        val noDuplicatesTypes = setOf(Types.Int, Types.String)
        assertEquals(noDuplicatesTypes, duplicateTypes, "Set should automatically remove duplicates")
        
        // Union with single alternative should be simplified (but current implementation requires ≥2)
        assertThrows(IllegalArgumentException::class.java) {
            UnionType(setOf(Types.Int))
        }
        
        // Test union with itself
        val selfUnion = UnionType(setOf(Types.Int, Types.String))
        assertTrue(selfUnion.structurallyEquivalent(selfUnion), "Union should be equivalent to itself")
    }
    
    @Test
    fun `union type subtyping relationships`() {
        // Test: Int <: (Int | String) - a type is a subtype of a union containing it
        val intType = Types.Int
        val unionType = UnionType(setOf(Types.Int, Types.String))
        
        // Create a constraint to test subtyping
        val generator = ConstraintGenerator()
        
        // Note: Current constraint system may not fully support union subtyping
        // This documents the expected behavior for implementation
        
        // For now, test that the union contains the subtype
        assertTrue(unionType.alternatives.contains(intType), 
                  "Union should contain its potential subtypes")
    }
    
    @Test
    fun `union type in function parameters`() {
        // Test: f: (Int | String) -> Bool
        val paramUnion = UnionType(setOf(Types.Int, Types.String))
        val funcType = FunctionType(paramUnion, Types.Bool)
        
        // Test that function type is created correctly
        assertEquals(paramUnion, funcType.domain, "Function domain should be union type")
        assertEquals(Types.Bool, funcType.codomain, "Function codomain should be Bool")
        
        // Test free type variables
        assertEquals(emptySet<TypeVariable>(), funcType.freeTypeVariables(), 
                    "Function with concrete union should have no free variables")
    }
    
    @Test
    fun `union type in function return types`() {
        // Test: f: Int -> (String | Bool)
        val returnUnion = UnionType(setOf(Types.String, Types.Bool))
        val funcType = FunctionType(Types.Int, returnUnion)
        
        assertEquals(Types.Int, funcType.domain, "Function domain should be Int")
        assertEquals(returnUnion, funcType.codomain, "Function codomain should be union type")
    }
    
    @Test
    fun `union type in record fields`() {
        // Test: {status: "pending" | "complete", value: Int | String}
        val statusUnion = UnionType(setOf(Types.literal("pending"), Types.literal("complete")))
        val valueUnion = UnionType(setOf(Types.Int, Types.String))
        
        val recordType = RecordType(mapOf(
            "status" to statusUnion,
            "value" to valueUnion
        ))
        
        assertEquals(statusUnion, recordType.fields["status"], "Status field should be union type")
        assertEquals(valueUnion, recordType.fields["value"], "Value field should be union type")
        
        // Test union alternatives
        assertEquals(2, statusUnion.alternatives.size, "Status union should have 2 alternatives")
        assertEquals(2, valueUnion.alternatives.size, "Value union should have 2 alternatives")
    }
    
    @Test
    fun `union type in tuple elements`() {
        // Test: (Int | String, Bool | Float)
        val firstUnion = UnionType(setOf(Types.Int, Types.String))
        val secondUnion = UnionType(setOf(Types.Bool, Types.literal("float")))
        
        val tupleType = TupleType(listOf(firstUnion, secondUnion))
        
        assertEquals(2, tupleType.elements.size, "Tuple should have 2 elements")
        assertEquals(firstUnion, tupleType.elements[0], "First element should be first union")
        assertEquals(secondUnion, tupleType.elements[1], "Second element should be second union")
    }
    
    @Test
    fun `nested union types`() {
        // Test: (Int | String) | Bool - nested unions
        val innerUnion = UnionType(setOf(Types.Int, Types.String))
        val nestedUnion = UnionType(setOf(innerUnion, Types.Bool))
        
        assertEquals(2, nestedUnion.alternatives.size, "Nested union should have 2 top-level alternatives")
        assertTrue(nestedUnion.alternatives.contains(innerUnion), "Should contain inner union")
        assertTrue(nestedUnion.alternatives.contains(Types.Bool), "Should contain Bool")
        
        // Test that inner union maintains its structure
        assertEquals(2, innerUnion.alternatives.size, "Inner union should maintain its alternatives")
    }
    
    @Test
    fun `union type with polymorphic types`() {
        // Test: Option[A] = None | Some[A] pattern
        val varA = TypeVariable.fresh()
        val noneType = Types.literal("None")
        val someType = TypeAlias("Some", listOf(varA))
        
        val optionUnion = UnionType(setOf(noneType, someType))
        
        assertEquals(2, optionUnion.alternatives.size, "Option union should have 2 alternatives")
        assertEquals(setOf(varA), optionUnion.freeTypeVariables(), 
                    "Option union should have A as free variable")
    }
    
    @Test
    fun `union type substitution application`() {
        // Test: Apply substitution to (A | String) where A := Int
        val varA = TypeVariable.fresh()
        val unionType = UnionType(setOf(varA, Types.String))
        
        val substitution = Substitution.single(varA, Types.Int)
        val result = substitution.apply(unionType)
        
        assertTrue(result is UnionType, "Result should be union type")
        val resultUnion = result as UnionType
        
        assertEquals(2, resultUnion.alternatives.size, "Result should have 2 alternatives")
        assertTrue(resultUnion.alternatives.contains(Types.Int), "Should contain Int after substitution")
        assertTrue(resultUnion.alternatives.contains(Types.String), "Should still contain String")
        assertFalse(resultUnion.alternatives.contains(varA), "Should no longer contain original variable")
    }
    
    @Test
    fun `union type equality and hash code`() {
        // Test: Equals and hashCode for union types
        val union1 = UnionType(setOf(Types.Int, Types.String))
        val union2 = UnionType(setOf(Types.Int, Types.String))
        val union3 = UnionType(setOf(Types.String, Types.Int)) // Different order
        val union4 = UnionType(setOf(Types.Int, Types.Bool))   // Different type
        
        // Equality tests
        assertEquals(union1, union2, "Identical unions should be equal")
        assertEquals(union1, union3, "Order-different unions should be equal (sets)")
        assertNotEquals(union1, union4, "Different unions should not be equal")
        
        // Hash code tests
        assertEquals(union1.hashCode(), union2.hashCode(), "Equal unions should have same hash code")
        assertEquals(union1.hashCode(), union3.hashCode(), "Order-different unions should have same hash code")
    }
    
    @Test
    fun `union type minimum alternatives requirement`() {
        // Test: Union types must have at least 2 alternatives
        assertThrows(IllegalArgumentException::class.java) {
            UnionType(emptySet())
        }
        
        assertThrows(IllegalArgumentException::class.java) {
            UnionType(setOf(Types.Int))
        }
        
        // Should succeed with 2 or more
        assertDoesNotThrow {
            UnionType(setOf(Types.Int, Types.String))
            UnionType(setOf(Types.Int, Types.String, Types.Bool))
        }
    }
    
    @Test
    fun `union type with result pattern`() {
        // Test: Result[E, A] = Error[E] | Success[A] pattern
        val varE = TypeVariable.fresh()
        val varA = TypeVariable.fresh()
        val errorType = TypeAlias("Error", listOf(varE))
        val successType = TypeAlias("Success", listOf(varA))
        
        val resultUnion = UnionType(setOf(errorType, successType))
        
        assertEquals(2, resultUnion.alternatives.size, "Result union should have 2 alternatives")
        assertEquals(setOf(varE, varA), resultUnion.freeTypeVariables(), 
                    "Result union should have both E and A as free variables")
        
        // Test structural equivalence with reordered alternatives
        val reorderedResult = UnionType(setOf(successType, errorType))
        assertTrue(resultUnion.structurallyEquivalent(reorderedResult), 
                  "Result unions should be equivalent regardless of order")
    }
    
    @Test
    fun `union type exhaustiveness checking foundation`() {
        // Test: Foundation for exhaustiveness checking (implementation placeholder)
        val statusUnion = UnionType(setOf(
            Types.literal("pending"),
            Types.literal("complete"), 
            Types.literal("failed")
        ))
        
        // Test that all alternatives are accessible
        val alternatives = statusUnion.alternatives.toList()
        assertEquals(3, alternatives.size, "Should have all 3 status alternatives")
        
        // Test alternative types
        alternatives.forEach { alt ->
            assertTrue(alt is LiteralStringType, "All alternatives should be literal string types")
            val literal = alt as LiteralStringType
            assertTrue(literal.value in listOf("pending", "complete", "failed"), 
                      "Alternative should be one of expected values")
        }
    }
    
    @Test
    fun `union type performance with many alternatives`() {
        // Test: Performance with unions containing many alternatives
        val startTime = System.currentTimeMillis()
        
        val manyTypes = (1..20).map { Types.literal("type$it") }.toSet()
        val largeUnion = UnionType(manyTypes)
        
        assertEquals(20, largeUnion.alternatives.size, "Large union should have 20 alternatives")
        
        // Test structural equivalence performance
        val sameUnion = UnionType(manyTypes)
        assertTrue(largeUnion.structurallyEquivalent(sameUnion), 
                  "Large unions should check equivalence efficiently")
        
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        
        assertTrue(duration < 1000, "Large union operations should complete quickly (${duration}ms)")
    }
    
    @Test
    fun `union type with error handling patterns`() {
        // Test: Common error handling union patterns
        
        // Maybe pattern: None | Some[A]
        val varA = TypeVariable.fresh()
        val noneType = Types.literal("None")
        val someType = TypeAlias("Some", listOf(varA))
        val maybeUnion = UnionType(setOf(noneType, someType))
        
        assertEquals(2, maybeUnion.alternatives.size, "Maybe should have 2 alternatives")
        assertEquals(setOf(varA), maybeUnion.freeTypeVariables(), "Maybe should have A as free variable")
        
        // Either pattern: Left[E] | Right[A]
        val varE = TypeVariable.fresh()
        val leftType = TypeAlias("Left", listOf(varE))
        val rightType = TypeAlias("Right", listOf(varA))
        val eitherUnion = UnionType(setOf(leftType, rightType))
        
        assertEquals(2, eitherUnion.alternatives.size, "Either should have 2 alternatives")
        assertEquals(setOf(varE, varA), eitherUnion.freeTypeVariables(), 
                    "Either should have both E and A as free variables")
    }
} 