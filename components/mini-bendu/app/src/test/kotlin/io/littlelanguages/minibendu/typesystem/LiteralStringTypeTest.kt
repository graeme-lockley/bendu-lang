package io.littlelanguages.minibendu.typesystem

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Tests for LiteralStringType class
 */
class LiteralStringTypeTest {
    
    @Test
    fun `creation of literal string types`() {
        val successType = LiteralStringType("success")
        val errorType = LiteralStringType("error")
        val customType = LiteralStringType("custom-value")
        
        assertEquals("success", successType.value, "Literal string type should store the value")
        assertEquals("error", errorType.value, "Literal string type should store the value")
        assertEquals("custom-value", customType.value, "Literal string type should store the value")
    }
    
    @Test
    fun `equality based on string value`() {
        val success1 = LiteralStringType("success")
        val success2 = LiteralStringType("success")
        val error = LiteralStringType("error")
        
        // Same values should be equal
        assertEquals(success1, success2, "Literal string types with same value should be equal")
        assertEquals(success1.hashCode(), success2.hashCode(), "Equal types should have same hash code")
        
        // Different values should not be equal
        assertNotEquals(success1, error, "Literal string types with different values should not be equal")
        assertNotEquals(success1.hashCode(), error.hashCode(), "Different types should have different hash codes")
    }
    
    @Test
    fun `structural equivalence`() {
        val success1 = LiteralStringType("success")
        val success2 = LiteralStringType("success")
        val error = LiteralStringType("error")
        val stringType = Types.String
        
        // Same literal values are structurally equivalent
        assertTrue(success1.structurallyEquivalent(success2), "Same literal string types should be structurally equivalent")
        
        // Different literal values are not structurally equivalent
        assertFalse(success1.structurallyEquivalent(error), "Different literal string types should not be structurally equivalent")
        
        // Literal string types are not equivalent to general String type
        assertFalse(success1.structurallyEquivalent(stringType), "Literal string type should not be equivalent to general String type")
        assertFalse(stringType.structurallyEquivalent(success1), "General String type should not be equivalent to literal string type")
    }
    
    @Test
    fun `string representation`() {
        val success = LiteralStringType("success")
        val error = LiteralStringType("error")
        val withSpaces = LiteralStringType("hello world")
        
        assertEquals("\"success\"", success.toString(), "String representation should include quotes")
        assertEquals("\"error\"", error.toString(), "String representation should include quotes")
        assertEquals("\"hello world\"", withSpaces.toString(), "String representation should include quotes and preserve spaces")
    }
    
    @Test
    fun `factory methods from Types object`() {
        val custom = Types.literal("custom")
        val success = Types.Success
        val error = Types.Error
        val ok = Types.Ok
        
        assertEquals("custom", custom.value, "Factory method should create literal string type")
        assertEquals("success", success.value, "Predefined Success should have correct value")
        assertEquals("error", error.value, "Predefined Error should have correct value")
        assertEquals("ok", ok.value, "Predefined Ok should have correct value")
        
        // Test they're all LiteralStringType instances
        assertTrue(custom is LiteralStringType, "Factory method should create LiteralStringType")
        assertTrue(success is LiteralStringType, "Predefined Success should be LiteralStringType")
        assertTrue(error is LiteralStringType, "Predefined Error should be LiteralStringType")
        assertTrue(ok is LiteralStringType, "Predefined Ok should be LiteralStringType")
    }
    
    @Test
    fun `use in union types`() {
        val success = Types.Success
        val error = Types.Error
        val statusUnion = UnionType(setOf(success, error))
        
        assertEquals(2, statusUnion.alternatives.size, "Union should contain both alternatives")
        assertTrue(statusUnion.alternatives.contains(success), "Union should contain success literal")
        assertTrue(statusUnion.alternatives.contains(error), "Union should contain error literal")
        
        // Test string representation
        val unionStr = statusUnion.toString()
        assertTrue(unionStr.contains("\"success\""), "Union string should contain success literal")
        assertTrue(unionStr.contains("\"error\""), "Union string should contain error literal")
    }
}
