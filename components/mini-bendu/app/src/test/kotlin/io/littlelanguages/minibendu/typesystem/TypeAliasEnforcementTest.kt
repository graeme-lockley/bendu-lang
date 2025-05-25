package io.littlelanguages.minibendu.typesystem

import io.littlelanguages.minibendu.*
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals

/**
 * Comprehensive tests for type alias enforcement during type checking.
 * 
 * These tests validate that:
 * 1. Type aliases are properly enforced when used in type annotations
 * 2. Type mismatches are detected and reported correctly
 * 3. Type alias constraints propagate through the type system
 * 4. Complex type alias scenarios work correctly
 */
class TypeAliasEnforcementTest {
    
    private fun parseAndTypeCheck(source: String): TypeCheckResult {
        return try {
            val errors = Errors()
            val program = parse(source, errors)

            if (errors.hasErrors()) {
                val errorMessages = mutableListOf<String>()
                for (error in errors) {
                    errorMessages.add(error.toString())
                }
                TypeCheckResult.Failure("Parse error: ${errorMessages.joinToString("; ")}", SourceLocation(1, 1))
            } else {
                val typeChecker = TypeChecker()
                val incrementalResult = typeChecker.typeCheckProgram(program)
                
                if (incrementalResult.hasErrors) {
                    incrementalResult.errors.first()
                } else {
                    val expressionResults = incrementalResult.results.zip(program.topLevels)
                        .filter { (_, topLevel) -> topLevel is ExprStmt }
                        .map { (result, _) -> result }
                    
                    if (expressionResults.isEmpty()) {
                        TypeCheckResult.Failure("No expressions found in program", SourceLocation(1, 1))
                    } else {
                        expressionResults.last()
                    }
                }
            }
        } catch (e: Exception) {
            TypeCheckResult.Failure("Parse error: ${e.message}", SourceLocation(1, 1))
        }
    }
    
    private fun assertTypeCheckSuccess(source: String, expectedType: String? = null) {
        val result = parseAndTypeCheck(source)
        assertTrue(result is TypeCheckResult.Success, "Expected type check to succeed for: $source. Got: ${if (result is TypeCheckResult.Failure) result.error else ""}")
        if (expectedType != null) {
            assertEquals(expectedType, result.getFinalType().toString())
        }
    }
    
    private fun assertTypeCheckFailure(source: String, expectedErrorContains: String? = null) {
        val result = parseAndTypeCheck(source)
        assertTrue(result is TypeCheckResult.Failure, "Expected type check to fail for: $source")
        if (expectedErrorContains != null) {
            assertTrue(
                result.error.contains(expectedErrorContains, ignoreCase = true),
                "Expected error to contain '$expectedErrorContains' but got: ${result.error}"
            )
        }
    }
    
    @Test
    fun `simple type alias usage should succeed`() {
        assertTypeCheckSuccess(
            """
                type UserId = String
                
                let id: UserId = "user123"
                
                id
            """.trimIndent(), "String")
    }
    
    @Test
    fun `record type alias usage should succeed`() {
        assertTypeCheckSuccess(
            """
                type Person = { name: String, age: Int }
                
                let person: Person = { name = "Alice", age = 30 }
                
                person.age
            """.trimIndent(), "Int")
    }
    
    @Test
    fun `nested type alias usage should succeed`() {
        assertTypeCheckSuccess(
            """
                type Name = String
                type Age = Int
                type Person = { name: Name, age: Age }
                
                let person: Person = { name = "Bob", age = 25 }
                
                person.name
            """.trimIndent(), "String")
    }
    
    @Test
    fun `function type alias usage should succeed`() {
        assertTypeCheckSuccess(
            """
                type Adder = Int -> Int
                
                let addOne: Adder = \x => x + 1
                
                addOne(4)
            """.trimIndent(), "Int")
    }
    
    @Test
    fun `tuple type alias usage should succeed`() {
        assertTypeCheckSuccess(
            """
                type Point = (Int, Int)
                
                let origin: Point = (0, 0)
                
                origin
            """.trimIndent(), "(Int, Int)")
    }
    
    // ===== NEGATIVE TESTS (should fail) =====
    
    @Test
    fun `simple type alias mismatch should fail`() {
        assertTypeCheckFailure(
            """
                type UserId = String
                
                let id: UserId = 123
                
                id
            """.trimIndent(), "cannot unify")
    }
    
    @Test
    fun `record type alias field type mismatch should fail`() {
        assertTypeCheckFailure(
            """
                type Person = { name: String, age: String }
                
                let person: Person = { name = "Alice", age = 30 }
                
                person.age
            """.trimIndent(), "cannot unify")
    }
    
    @Test
    fun `function type alias parameter type mismatch should fail`() {
        assertTypeCheckFailure(
            """
                type StringProcessor = String -> String
                
                let processor: StringProcessor = \x => x + 1
                
                processor("hello")
            """.trimIndent(), "cannot unify")
    }
    
    @Test
    fun `function type alias return type mismatch should fail`() {
        assertTypeCheckFailure(
            """
                type NumberProducer = String -> Int
                
                let producer: NumberProducer = \x => x + "!"
                
                producer("test")
            """.trimIndent(), "cannot unify")
    }
    
    @Test
    fun `tuple type alias size mismatch should fail`() {
        assertTypeCheckFailure(
            """
                type Point = (Int, Int)
                
                let point: Point = (1, 2, 3)
                
                point
            """.trimIndent(), "cannot unify")
    }
    
    @Test
    fun `tuple type alias element type mismatch should fail`() {
        assertTypeCheckFailure(
            """
                type Point = (Int, Int)
                
                let point: Point = (1, "two")
                
                point
            """.trimIndent(), "cannot unify")
    }
    
    // ===== COMPLEX SCENARIOS =====
    
    @Test
    fun `nested type alias mismatch should fail`() {
        assertTypeCheckFailure(
            """
                type Name = String
                type Age = Int
                type Person = { name: Name, age: Age }
                
                let person: Person = { name = 123, age = 25 }
                
                person.name
            """.trimIndent(), "cannot unify")
    }
    
    @Test
    fun `type alias in function parameter should work`() {
        assertTypeCheckSuccess(
            """
                type UserId = String
                
                let getUser = \id: UserId => id
                
                getUser("user123")
            """.trimIndent(), "String")
    }
    
    @Test
    fun `type alias in function parameter mismatch should fail`() {
        assertTypeCheckFailure(
            """
                type UserId = String
                
                let getUser = \id: UserId => id
                
                getUser(123)
            """.trimIndent(), "cannot unify")
    }
    
    @Test
    fun `type alias in let binding should propagate constraints`() {
        assertTypeCheckSuccess(
            """
                type Score = Int
                
                let calculateTotal = \score1: Score => \score2: Score => score1 + score2
                
                calculateTotal(85)(92)
            """.trimIndent(), "Int")
    }
    
    @Test
    fun `type alias with record spread should work`() {
        assertTypeCheckSuccess(
            """
                type Person = { name: String, age: Int }
                type Employee = { name: String, age: Int, department: String }
                
                let person: Person = { name = "Alice", age = 30 }
                let employee: Employee = { ...person, department = "Engineering" }
                
                employee.department
            """.trimIndent(), "String")
    }
    
    @Test
    fun `type alias with incompatible record spread should fail`() {
        assertTypeCheckFailure(
            """
                type Person = { name: String, age: String }
                type Employee = { name: String, age: Int, department: String }
                
                let person: Person = { name = "Alice", age = "30" }
                let employee: Employee = { ...person, department = "Engineering" }
                
                employee.age
            """.trimIndent(), "cannot unify")
    }
    
    // ===== PARAMETRIC TYPE ALIAS TESTS =====
    
    @Test
    fun `parameterized type alias should work`() {
        assertTypeCheckSuccess(
            """
                type Pair[A, B] = (A, B)
                
                let coordinates: Pair[Int, String] = (10, "north")
                
                coordinates
            """.trimIndent(), "(Int, String)")
    }
    
    @Test
    fun `parameterized type alias mismatch should fail`() {
        assertTypeCheckFailure(
            """
                type Pair[A, B] = (A, B)
                
                let coordinates: Pair[Int, String] = (10, 20)
                
                coordinates
            """.trimIndent(), "cannot unify")
    }
    
    @Test
    fun `recursive type alias should work`() {
        assertTypeCheckSuccess(
            """
                type List[A] = { head: A, tail: List[A] }
                
                let numbers: List[Int] = { head = 1, tail = { head = 2, tail = { head = 3, tail = {} } } }
                
                numbers.head
            """.trimIndent(), "Int")
    }

    // ===== SOUND TYPE CHECKING WITH TYPE ALIASES =====
    
    @Test
    fun `record type alias with missing field should fail`() {
        // This should fail because the record literal is missing required fields
        assertTypeCheckFailure(
            """
                type Person = { name: String, age: Int }
                
                let person: Person = { name = "Alice" }
                
                person.name
            """.trimIndent(), "missing required field")
    }

    @Test
    fun `accessing missing field from incomplete record should fail`() {
        // This test checks that accessing a field that was missing from the literal fails
        assertTypeCheckFailure(
            """
                type Person = { name: String, age: Int }
                
                let person: Person = { name = "Alice" }
                
                person.age
            """.trimIndent(), "missing required field")
    }
    
    @Test
    fun `record type alias with extra field fails due to structural constraints`() {
        // Cannot assign a record with extra fields to a closed type alias
        // { name: String, age: Int, active: Bool } cannot unify with { name: String, age: Int }
        assertTypeCheckFailure(
            """
                type Person = { name: String, age: Int }
                
                let person: Person = { name = "Alice", age = 30, active = True }
                
                person.age
            """.trimIndent(), "extra field")
    }
}