package io.littlelanguages.minibendu.typesystem

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Tests for TypeScheme class - Task 5 of Phase 1
 * 
 * This test suite defines the requirements for type schemes which represent
 * polymorphic types with quantified type variables. Type schemes are essential
 * for generalization and instantiation in the type inference system.
 * 
 * A type scheme consists of:
 * - A set of quantified type variables (the ∀ variables)
 * - A type expression that may contain these variables
 * 
 * Example: ∀a. a -> a (identity function type)
 */
class TypeSchemeTest {
    
    @Test
    fun `monomorphic type scheme with no quantified variables`() {
        // A monomorphic type like Int should create a scheme with no quantified variables
        val intType = Types.Int
        val scheme = TypeScheme.monomorphic(intType)
        
        assertTrue(scheme.quantifiedVariables.isEmpty(), "Monomorphic scheme should have no quantified variables")
        assertEquals(intType, scheme.type, "Monomorphic scheme should preserve the type")
        assertTrue(scheme.isMonomorphic(), "Should be identified as monomorphic")
        assertFalse(scheme.isPolymorphic(), "Should not be identified as polymorphic")
    }
    
    @Test
    fun `polymorphic type scheme with quantified variables`() {
        // Create ∀a. a -> a (identity function)
        val typeVar = TypeVariable.fresh()
        val functionType = FunctionType(typeVar, typeVar)
        val quantifiedVars = setOf(typeVar)
        val scheme = TypeScheme(quantifiedVars, functionType)
        
        assertEquals(quantifiedVars, scheme.quantifiedVariables, "Should store quantified variables")
        assertEquals(functionType, scheme.type, "Should store the type expression")
        assertFalse(scheme.isMonomorphic(), "Should not be identified as monomorphic")
        assertTrue(scheme.isPolymorphic(), "Should be identified as polymorphic")
    }
    
    @Test
    fun `type scheme instantiation creates fresh variables`() {
        // Create ∀a. a -> a and instantiate it twice
        val originalVar = TypeVariable.fresh()
        val functionType = FunctionType(originalVar, originalVar)
        val scheme = TypeScheme(setOf(originalVar), functionType)
        
        val (instantiatedType1, substitution1) = scheme.instantiate()
        val (instantiatedType2, substitution2) = scheme.instantiate()
        
        // Both instantiations should be function types
        assertTrue(instantiatedType1 is FunctionType, "First instantiation should be a function type")
        assertTrue(instantiatedType2 is FunctionType, "Second instantiation should be a function type")
        
        val funcType1 = instantiatedType1
        val funcType2 = instantiatedType2
        
        // Parameter and return types should be the same variable in each instantiation
        assertEquals(funcType1.domain, funcType1.codomain, "First instantiation parameter and return should be same variable")
        assertEquals(funcType2.domain, funcType2.codomain, "Second instantiation parameter and return should be same variable")
        
        // But the variables should be different between instantiations
        assertNotEquals(funcType1.domain, funcType2.domain, "Different instantiations should have different variables")
        
        // Substitutions should map original variable to fresh variable
        assertEquals(1, substitution1.domain().size, "First substitution should have one mapping")
        assertEquals(1, substitution2.domain().size, "Second substitution should have one mapping")
        assertTrue(substitution1.domain().contains(originalVar), "First substitution should map original variable")
        assertTrue(substitution2.domain().contains(originalVar), "Second substitution should map original variable")
    }
    
    @Test
    fun `type scheme generalization from type and free variables`() {
        // Create a type Int -> a where 'a' is free
        val freeVar = TypeVariable.fresh()
        val intToA = FunctionType(Types.Int, freeVar)
        val freeVars = setOf(freeVar)
        
        val scheme = TypeScheme.generalize(intToA, freeVars)
        
        assertEquals(freeVars, scheme.quantifiedVariables, "Should quantify over free variables")
        assertEquals(intToA, scheme.type, "Should preserve the original type")
        assertTrue(scheme.isPolymorphic(), "Should be polymorphic with quantified variables")
    }
    
    @Test
    fun `type scheme generalization with no free variables creates monomorphic scheme`() {
        // Create a type Int -> String with no free variables
        val intToString = FunctionType(Types.Int, Types.String)
        val noFreeVars = emptySet<TypeVariable>()
        
        val scheme = TypeScheme.generalize(intToString, noFreeVars)
        
        assertTrue(scheme.quantifiedVariables.isEmpty(), "Should have no quantified variables")
        assertEquals(intToString, scheme.type, "Should preserve the original type")
        assertTrue(scheme.isMonomorphic(), "Should be monomorphic")
    }
    
    @Test
    fun `type scheme equality based on alpha equivalence`() {
        // Create ∀a. a -> a and ∀b. b -> b (should be equivalent)
        val varA = TypeVariable.fresh()
        val varB = TypeVariable.fresh()
        val schemeA = TypeScheme(setOf(varA), FunctionType(varA, varA))
        val schemeB = TypeScheme(setOf(varB), FunctionType(varB, varB))
        
        // They should be alpha-equivalent (representing the same polymorphic type)
        assertTrue(schemeA.isAlphaEquivalent(schemeB), "Alpha-equivalent schemes should be equal")
        assertTrue(schemeB.isAlphaEquivalent(schemeA), "Alpha-equivalence should be symmetric")
    }
    
    @Test
    fun `type scheme alpha equivalence with different structures`() {
        // Create ∀a. a -> a and ∀b. b -> Int (should not be equivalent)
        val varA = TypeVariable.fresh()
        val varB = TypeVariable.fresh()
        val schemeA = TypeScheme(setOf(varA), FunctionType(varA, varA))
        val schemeB = TypeScheme(setOf(varB), FunctionType(varB, Types.Int))
        
        assertFalse(schemeA.isAlphaEquivalent(schemeB), "Different structures should not be alpha-equivalent")
        assertFalse(schemeB.isAlphaEquivalent(schemeA), "Alpha-equivalence should be symmetric")
    }
    
    @Test
    fun `type scheme with complex nested types`() {
        // Create ∀a,b. (a -> b) -> [a] -> [b] (map function type)
        val varA = TypeVariable.fresh()
        val varB = TypeVariable.fresh()
        val funcType = FunctionType(varA, varB)
        val listA = TupleType(listOf(varA)) // Using tuple as simple list representation
        val listB = TupleType(listOf(varB))
        val mapType = FunctionType(funcType, FunctionType(listA, listB))
        
        val scheme = TypeScheme(setOf(varA, varB), mapType)
        
        assertEquals(2, scheme.quantifiedVariables.size, "Should have two quantified variables")
        assertTrue(scheme.quantifiedVariables.contains(varA), "Should quantify over varA")
        assertTrue(scheme.quantifiedVariables.contains(varB), "Should quantify over varB")
        assertTrue(scheme.isPolymorphic(), "Should be polymorphic")
        
        // Instantiate and verify structure is preserved
        val (instantiated, substitution) = scheme.instantiate()
        assertTrue(instantiated is FunctionType, "Instantiated type should be a function")
        assertEquals(2, substitution.domain().size, "Substitution should map both variables")
    }
    
    @Test
    fun `type scheme free variables calculation`() {
        // Create ∀a. a -> b where 'b' is not quantified (free)
        val varA = TypeVariable.fresh()
        val varB = TypeVariable.fresh() // This will be free
        val funcType = FunctionType(varA, varB)
        val scheme = TypeScheme(setOf(varA), funcType)
        
        val freeVars = scheme.freeVariables()
        
        assertEquals(1, freeVars.size, "Should have one free variable")
        assertTrue(freeVars.contains(varB), "Should contain the unquantified variable")
        assertFalse(freeVars.contains(varA), "Should not contain quantified variables")
    }
    
    @Test
    fun `type scheme substitution application`() {
        // Create ∀a. a -> b and apply substitution b := Int
        val varA = TypeVariable.fresh()
        val varB = TypeVariable.fresh()
        val funcType = FunctionType(varA, varB)
        val scheme = TypeScheme(setOf(varA), funcType)
        
        val substitution = Substitution.single(varB, Types.Int)
        val substitutedScheme = scheme.apply(substitution)
        
        // Should substitute free variables but not quantified ones
        assertEquals(scheme.quantifiedVariables, substitutedScheme.quantifiedVariables, "Quantified variables should be unchanged")
        
        val expectedType = FunctionType(varA, Types.Int)
        assertEquals(expectedType, substitutedScheme.type, "Should substitute free variables in type")
    }
}
