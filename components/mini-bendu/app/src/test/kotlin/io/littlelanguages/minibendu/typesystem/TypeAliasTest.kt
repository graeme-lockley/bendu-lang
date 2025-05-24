package io.littlelanguages.minibendu.typesystem

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull

/**
 * Comprehensive tests for type alias support.
 * Tests cover simple definitions, recursive detection, expansion, and cycle detection.
 * 
 * Type aliases allow creating named abbreviations for complex types:
 * - type Name = String
 * - type Person = {name: String, age: Int}
 * - type Tree[A] = {value: A, left: Tree[A], right: Tree[A]}
 */
class TypeAliasTest {
    
    @Test
    fun `define simple type alias`() {
        // type Name = String
        val aliasRegistry = TypeAliasRegistry()
        val stringType = Types.String
        
        val result = aliasRegistry.defineAlias("Name", emptyList(), stringType)
        
        assertTrue(result.isSuccess, "Should successfully define simple alias")
        assertTrue(aliasRegistry.hasAlias("Name"), "Registry should contain the alias")
        assertEquals(stringType, aliasRegistry.expandAlias("Name", emptyList()), "Should expand to String type")
    }
    
    @Test
    fun `define parameterized type alias`() {
        // type List[A] = {head: A, tail: List[A]} (will be recursive but simplified for test)
        val aliasRegistry = TypeAliasRegistry()
        val typeParam = TypeVariable.fresh()
        val listType = RecordType(mapOf(
            "head" to typeParam,
            "isEmpty" to Types.Bool
        ))
        
        val result = aliasRegistry.defineAlias("List", listOf(typeParam), listType)
        
        assertTrue(result.isSuccess, "Should successfully define parameterized alias")
        assertTrue(aliasRegistry.hasAlias("List"), "Registry should contain the parameterized alias")
    }
    
    @Test
    fun `expand simple type alias`() {
        // type UserId = String
        val aliasRegistry = TypeAliasRegistry()
        aliasRegistry.defineAlias("UserId", emptyList(), Types.String)
        
        val expanded = aliasRegistry.expandAlias("UserId", emptyList())
        
        assertEquals(Types.String, expanded, "UserId should expand to String")
    }
    
    @Test
    fun `expand parameterized type alias with concrete types`() {
        // type Pair[A, B] = {first: A, second: B}
        val aliasRegistry = TypeAliasRegistry()
        val typeParamA = TypeVariable.fresh()
        val typeParamB = TypeVariable.fresh()
        val pairType = RecordType(mapOf(
            "first" to typeParamA,
            "second" to typeParamB
        ))
        
        aliasRegistry.defineAlias("Pair", listOf(typeParamA, typeParamB), pairType)
        
        // Expand Pair[String, Int]
        val expanded = aliasRegistry.expandAlias("Pair", listOf(Types.String, Types.Int))
        
        assertTrue(expanded is RecordType, "Should expand to record type")
        val expandedRecord = expanded as RecordType
        assertEquals(Types.String, expandedRecord.fields["first"], "First field should be String")
        assertEquals(Types.Int, expandedRecord.fields["second"], "Second field should be Int")
    }
    
    @Test
    fun `normalize nested type aliases`() {
        // type Name = String
        // type Person = {name: Name, age: Int}
        val aliasRegistry = TypeAliasRegistry()
        
        aliasRegistry.defineAlias("Name", emptyList(), Types.String)
        val personType = RecordType(mapOf(
            "name" to TypeAlias("Name", emptyList()),
            "age" to Types.Int
        ))
        aliasRegistry.defineAlias("Person", emptyList(), personType)
        
        val normalized = aliasRegistry.normalizeType(TypeAlias("Person", emptyList()))
        
        assertTrue(normalized is RecordType, "Should normalize to record type")
        val normalizedRecord = normalized as RecordType
        assertEquals(Types.String, normalizedRecord.fields["name"], "Name field should be normalized to String")
        assertEquals(Types.Int, normalizedRecord.fields["age"], "Age field should remain Int")
    }
    
    @Test
    fun `detect simple circular dependency`() {
        // type A = B
        // type B = A
        val aliasRegistry = TypeAliasRegistry()
        
        aliasRegistry.defineAlias("A", emptyList(), TypeAlias("B", emptyList()))
        val result = aliasRegistry.defineAlias("B", emptyList(), TypeAlias("A", emptyList()))
        
        assertTrue(result.isFailure, "Should fail to define circular alias")
        val error = result.error
        assertTrue(error.contains("circular") || error.contains("cycle"), 
                  "Error should mention circular dependency: $error")
    }
    
    @Test
    fun `detect indirect circular dependency`() {
        // type A = B
        // type B = C  
        // type C = A
        val aliasRegistry = TypeAliasRegistry()
        
        aliasRegistry.defineAlias("A", emptyList(), TypeAlias("B", emptyList()))
        aliasRegistry.defineAlias("B", emptyList(), TypeAlias("C", emptyList()))
        val result = aliasRegistry.defineAlias("C", emptyList(), TypeAlias("A", emptyList()))
        
        assertTrue(result.isFailure, "Should fail to define indirect circular alias")
        val error = result.error
        assertTrue(error.contains("circular") || error.contains("cycle"),
                  "Error should mention circular dependency: $error")
    }
    
    @Test
    fun `detect recursive type alias (valid case)`() {
        // type Tree[A] = {value: A, left: Option[Tree[A]], right: Option[Tree[A]]}
        // This should be allowed as it's a valid recursive data structure
        val aliasRegistry = TypeAliasRegistry()
        val typeParam = TypeVariable.fresh()
        
        // First define Option as a union type (simplified)
        val optionType = TypeVariable.fresh() // Placeholder for Option[T]
        aliasRegistry.defineAlias("Option", listOf(typeParam), optionType)
        
        val treeType = RecordType(mapOf(
            "value" to typeParam,
            "left" to TypeAlias("Option", listOf(TypeAlias("Tree", listOf(typeParam)))),
            "right" to TypeAlias("Option", listOf(TypeAlias("Tree", listOf(typeParam))))
        ))
        
        val result = aliasRegistry.defineAlias("Tree", listOf(typeParam), treeType)
        
        assertTrue(result.isSuccess, "Should allow valid recursive data structure")
    }
    
    @Test
    fun `detect invalid recursive type alias`() {
        // type BadType = BadType (direct self-reference without structure)
        val aliasRegistry = TypeAliasRegistry()
        
        val result = aliasRegistry.defineAlias("BadType", emptyList(), TypeAlias("BadType", emptyList()))
        
        assertTrue(result.isFailure, "Should fail for direct self-reference")
        val error = result.error
        assertTrue(error.contains("recursive") || error.contains("circular"),
                  "Error should mention recursion problem: $error")
    }
    
    @Test
    fun `handle undefined type alias reference`() {
        val aliasRegistry = TypeAliasRegistry()
        
        val result = aliasRegistry.expandAlias("UndefinedType", emptyList())
        
        assertNull(result, "Should return null for undefined alias")
    }
    
    @Test
    fun `type alias parameter count mismatch`() {
        // type Pair[A, B] = {first: A, second: B}
        val aliasRegistry = TypeAliasRegistry()
        val typeParamA = TypeVariable.fresh()
        val typeParamB = TypeVariable.fresh()
        val pairType = RecordType(mapOf(
            "first" to typeParamA,
            "second" to typeParamB
        ))
        
        aliasRegistry.defineAlias("Pair", listOf(typeParamA, typeParamB), pairType)
        
        // Try to expand with wrong number of parameters
        val result = aliasRegistry.expandAlias("Pair", listOf(Types.String)) // Only 1 param instead of 2
        
        assertNull(result, "Should return null for parameter count mismatch")
    }
    
    @Test
    fun `normalize complex nested aliases`() {
        // type UserId = String
        // type UserName = String  
        // type UserRecord = {id: UserId, name: UserName, age: Int}
        // type Users = List[UserRecord]
        val aliasRegistry = TypeAliasRegistry()
        
        aliasRegistry.defineAlias("UserId", emptyList(), Types.String)
        aliasRegistry.defineAlias("UserName", emptyList(), Types.String)
        
        val userRecordType = RecordType(mapOf(
            "id" to TypeAlias("UserId", emptyList()),
            "name" to TypeAlias("UserName", emptyList()),
            "age" to Types.Int
        ))
        aliasRegistry.defineAlias("UserRecord", emptyList(), userRecordType)
        
        val listTypeParam = TypeVariable.fresh()
        val listType = RecordType(mapOf("elements" to listTypeParam)) // Simplified list
        aliasRegistry.defineAlias("List", listOf(listTypeParam), listType)
        
        val usersType = TypeAlias("List", listOf(TypeAlias("UserRecord", emptyList())))
        aliasRegistry.defineAlias("Users", emptyList(), usersType)
        
        val normalized = aliasRegistry.normalizeType(TypeAlias("Users", emptyList()))
        
        // The final result should have all aliases expanded
        assertTrue(normalized is RecordType, "Should normalize to record type")
        val normalizedRecord = normalized as RecordType
        assertTrue(normalizedRecord.fields["elements"] is RecordType, "Elements should be normalized record")
        val elementRecord = normalizedRecord.fields["elements"] as RecordType
        assertEquals(Types.String, elementRecord.fields["id"], "id should be normalized to String")
        assertEquals(Types.String, elementRecord.fields["name"], "name should be normalized to String")
        assertEquals(Types.Int, elementRecord.fields["age"], "age should remain Int")
    }
    
    @Test
    fun `type alias expansion in substitution`() {
        // Test that type aliases are properly handled during substitution
        val aliasRegistry = TypeAliasRegistry()
        
        // type Name = String
        aliasRegistry.defineAlias("Name", emptyList(), Types.String)
        
        val typeVar = TypeVariable.fresh()
        val aliasType = TypeAlias("Name", emptyList())
        
        val substitution = Substitution.single(typeVar, aliasType)
        val functionType = FunctionType(typeVar, Types.Int)
        
        val result = substitution.apply(functionType)
        
        assertTrue(result is FunctionType, "Should remain function type")
        val resultFunc = result as FunctionType
        assertTrue(resultFunc.domain is TypeAlias, "Domain should be type alias")
        assertEquals("Name", (resultFunc.domain as TypeAlias).name, "Should preserve alias name")
    }
    
    @Test
    fun `performance test with many aliases`() {
        // Test that alias operations scale reasonably
        val aliasRegistry = TypeAliasRegistry()
        
        // Create a chain of 100 aliases: Alias0 -> Alias1 -> ... -> Alias99 -> String
        val startTime = System.currentTimeMillis()
        
        for (i in 99 downTo 1) {
            aliasRegistry.defineAlias("Alias$i", emptyList(), TypeAlias("Alias${i-1}", emptyList()))
        }
        aliasRegistry.defineAlias("Alias0", emptyList(), Types.String)
        
        // Normalize the deeply nested alias
        val normalized = aliasRegistry.normalizeType(TypeAlias("Alias99", emptyList()))
        
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        
        assertEquals(Types.String, normalized, "Should fully normalize to String")
        assertTrue(duration < 1000, "Should complete normalization within reasonable time (${duration}ms)")
    }
} 