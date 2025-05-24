package io.littlelanguages.minibendu.typesystem

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Tests for TypeVariable class - Task 1 of Phase 1
 * 
 * This test suite verifies:
 * - Creation of type variables with unique IDs
 * - Equality based on ID, not object identity
 * - Fresh variable generation for instantiation
 * - Type variable levels for rank-n polymorphism (Phase 2 preparation)
 */
class TypeVariableTest {
    
    @Test
    fun `creation of type variables with unique IDs`() {
        val var1 = TypeVariable.fresh()
        val var2 = TypeVariable.fresh() 
        val var3 = TypeVariable.fresh()
        
        // Each type variable should have a unique ID
        assertNotEquals(var1.id, var2.id, "Type variables should have unique IDs")
        assertNotEquals(var2.id, var3.id, "Type variables should have unique IDs")
        assertNotEquals(var1.id, var3.id, "Type variables should have unique IDs")
        
        // IDs should be positive integers
        assertTrue(var1.id > 0, "Type variable ID should be positive")
        assertTrue(var2.id > 0, "Type variable ID should be positive")
        assertTrue(var3.id > 0, "Type variable ID should be positive")
    }
    
    @Test
    fun `equality based on ID not object identity`() {
        val var1 = TypeVariable.fresh()
        val var2 = TypeVariable.fresh()
        
        // Different variables should not be equal
        assertNotEquals(var1, var2, "Different type variables should not be equal")
        
        // Same variable should be equal to itself
        assertEquals(var1, var1, "Type variable should be equal to itself")
        assertEquals(var1.hashCode(), var1.hashCode(), "Hash code should be consistent")
    }
    
    @Test
    fun `fresh variable generation for instantiation`() {
        val variables = mutableSetOf<TypeVariable>()
        
        // Generate many fresh variables
        repeat(100) {
            val fresh = TypeVariable.fresh()
            assertTrue(variables.add(fresh), "Fresh variables should always be unique")
        }
        
        assertEquals(100, variables.size, "Should generate 100 unique variables")
        
        // Test fresh generation with specific level
        val leveledVar1 = TypeVariable.fresh(level = 5)
        val leveledVar2 = TypeVariable.fresh(level = 5)
        
        assertEquals(5, leveledVar1.level, "Variable should have specified level")
        assertEquals(5, leveledVar2.level, "Variable should have specified level")
        assertNotEquals(leveledVar1.id, leveledVar2.id, "Variables should have different IDs even with same level")
    }
    
    @Test
    fun `type variable levels for rank-n polymorphism preparation`() {
        // Test default level (0 for top-level)
        val defaultVar = TypeVariable.fresh()
        assertEquals(0, defaultVar.level, "Default level should be 0")
        
        // Test explicit levels
        val level1Var = TypeVariable.fresh(level = 1)
        val level2Var = TypeVariable.fresh(level = 2)
        val level10Var = TypeVariable.fresh(level = 10)
        
        assertEquals(1, level1Var.level, "Variable should have level 1")
        assertEquals(2, level2Var.level, "Variable should have level 2") 
        assertEquals(10, level10Var.level, "Variable should have level 10")
        
        // Level comparison and instantiation methods will be added in later tasks
    }
    
    @Test
    fun `type variable properties and methods`() {
        val var1 = TypeVariable.fresh()
        
        // Test that it's a proper Type
        assertTrue(var1 is Type, "TypeVariable should be a Type")
        
        // Test free type variables (should return itself)
        val freeVars = var1.freeTypeVariables()
        assertEquals(setOf(var1), freeVars, "TypeVariable should return itself as free variable")
        
        // Test structural equivalence (only equal to same variable)
        val var2 = TypeVariable.fresh()
        assertTrue(var1.structurallyEquivalent(var1), "Variable should be structurally equivalent to itself")
        assertFalse(var1.structurallyEquivalent(var2), "Different variables should not be structurally equivalent")
    }
    
    @Test
    fun `string representation`() {
        val var1 = TypeVariable.fresh()
        val var2 = TypeVariable.fresh(level = 3)
        
        // Test toString includes ID and level information
        val str1 = var1.toString()
        val str2 = var2.toString()
        
        assertTrue(str1.contains(var1.id.toString()), "String representation should include ID")
        assertTrue(str2.contains(var2.id.toString()), "String representation should include ID")
        assertTrue(str2.contains("3"), "String representation should include level")
        
        // Different variables should have different string representations
        assertNotEquals(str1, str2, "Different variables should have different string representations")
    }
    
    @Test
    fun `instantiation method preparation`() {
        val var1 = TypeVariable.fresh(level = 2)
        
        // Test basic level property
        assertEquals(2, var1.level, "Variable should have specified level")
        
        // Instantiation methods will be implemented in later tasks
        // For now, just verify we can create variables with different levels
        val var2 = TypeVariable.fresh(level = 0)
        val var3 = TypeVariable.fresh(level = 1)
        
        assertEquals(0, var2.level, "Variable should have level 0")
        assertEquals(1, var3.level, "Variable should have level 1")
        
        // All should have different IDs
        assertNotEquals(var1.id, var2.id, "Variables should have different IDs")
        assertNotEquals(var1.id, var3.id, "Variables should have different IDs")
        assertNotEquals(var2.id, var3.id, "Variables should have different IDs")
    }
}
