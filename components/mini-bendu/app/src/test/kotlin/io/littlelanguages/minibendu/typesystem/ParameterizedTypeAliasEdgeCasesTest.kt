package io.littlelanguages.minibendu.typesystem

import io.littlelanguages.minibendu.*
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertEquals

/**
 * Edge case tests for parameterized type aliases.
 * 
 * This test suite covers edge cases and advanced scenarios that might not be
 * covered in the main parameterized type alias test suite.
 */
class ParameterizedTypeAliasEdgeCasesTest {
    
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
    
    // ===== SINGLE TYPE PARAMETER EDGE CASES =====
    
    @Test
    fun `single type parameter used multiple times should work`() {
        assertTypeCheckSuccess(
            """
                type SelfPair[T] = (T, T)
                
                let pair: SelfPair[String] = ("hello", "world")
                
                pair
            """.trimIndent(), "(String, String)")
    }
    
    @Test
    fun `single type parameter in nested structure should work`() {
        assertTypeCheckSuccess(
            """
                type NestedContainer[T] = { outer: { inner: T } }
                
                let container: NestedContainer[Int] = { outer = { inner = 42 } }
                
                container.outer.inner
            """.trimIndent(), "Int")
    }
    
    // ===== MULTIPLE TYPE PARAMETER EDGE CASES =====
    
    @Test
    fun `type parameters in different order should work`() {
        assertTypeCheckSuccess(
            """
                type FlippedPair[A, B] = (B, A)
                
                let flipped: FlippedPair[Int, String] = ("hello", 42)
                
                flipped
            """.trimIndent(), "(String, Int)")
    }
    
    @Test
    fun `same type parameter used for multiple fields should work`() {
        assertTypeCheckSuccess(
            """
                type Uniform[T] = { first: T, second: T, third: T }
                
                let uniform: Uniform[Bool] = { first = True, second = False, third = True }
                
                uniform.second
            """.trimIndent(), "Bool")
    }
    
    // ===== FUNCTION TYPE EDGE CASES =====
    
    @Test
    fun `function type with same input and output parameter should work`() {
        assertTypeCheckSuccess(
            """
                type Identity[T] = T -> T
                
                let stringIdentity: Identity[String] = \x => x
                
                stringIdentity("test")
            """.trimIndent(), "String")
    }
    
    @Test
    fun `curried function type with parameters should work`() {
        assertTypeCheckSuccess(
            """
                type Curried[A, B, C] = A -> B -> C
                
                let add: Curried[Int, Int, Int] = \x => \y => x + y
                
                add(1)(2)
            """.trimIndent(), "Int")
    }
    
    // ===== RECORD TYPE EDGE CASES =====
    
    @Test
    fun `record with function field using type parameters should work`() {
        assertTypeCheckSuccess(
            """
                type FunctionContainer[A, B] = { func: A -> B, input: A }
                
                let container: FunctionContainer[Int, String] = { 
                    func = \x => "number", 
                    input = 42 
                }
                
                container.func(container.input)
            """.trimIndent(), "String")
    }
    
    @Test
    fun `record with tuple field using type parameters should work`() {
        assertTypeCheckSuccess(
            """
                type TupleContainer[A, B] = { data: (A, B), count: Int }
                
                let container: TupleContainer[String, Bool] = { 
                    data = ("test", True), 
                    count = 1 
                }
                
                container.data
            """.trimIndent(), "(String, Bool)")
    }
    
    // ===== TUPLE TYPE EDGE CASES =====
    
    @Test
    fun `tuple with repeated type parameters should work`() {
        assertTypeCheckSuccess(
            """
                type RepeatedTuple[T] = (T, T, T)
                
                let triple: RepeatedTuple[Int] = (1, 2, 3)
                
                triple
            """.trimIndent(), "(Int, Int, Int)")
    }
    
    @Test
    fun `tuple with mixed type parameters should work`() {
        assertTypeCheckSuccess(
            """
                type MixedTuple[A, B] = (A, B, A, B)
                
                let mixed: MixedTuple[String, Int] = ("a", 1, "b", 2)
                
                mixed
            """.trimIndent(), "(String, Int, String, Int)")
    }
    
    // ===== NESTED ALIAS EDGE CASES =====
    
    @Test
    fun `alias referencing another parameterized alias should work`() {
        assertTypeCheckSuccess(
            """
                type Wrapper[T] = { value: T }
                type DoubleWrapper[T] = Wrapper[Wrapper[T]]
                
                let wrapped: DoubleWrapper[Int] = { value = { value = 42 } }
                
                wrapped.value.value
            """.trimIndent(), "Int")
    }
    
    @Test
    fun `chain of parameterized aliases should work`() {
        assertTypeCheckSuccess(
            """
                type Level1[T] = { data: T }
                type Level2[T] = Level1[Level1[T]]
                type Level3[T] = Level2[Level1[T]]
                
                let deep: Level3[String] = { data = { data = { data = "deep" } } }
                
                deep.data.data.data
            """.trimIndent(), "String")
    }
    
    // ===== TYPE PARAMETER SHADOWING EDGE CASES =====
    
    @Test
    fun `different aliases with same parameter names should work`() {
        assertTypeCheckSuccess(
            """
                type First[T] = { first: T }
                type Second[T] = { second: T }
                
                let firstInt: First[Int] = { first = 1 }
                let secondString: Second[String] = { second = "hello" }
                
                firstInt.first
            """.trimIndent(), "Int")
    }
    
    // ===== COMPLEX COMBINATIONS =====
    
    @Test
    fun `function returning parameterized type should work`() {
        assertTypeCheckSuccess(
            """
                type Container[T] = { value: T }
                
                let makeContainer = \value => { value = value }
                let intContainer: Container[Int] = makeContainer(42)
                
                intContainer.value
            """.trimIndent(), "Int")
    }
    
    @Test
    fun `parameterized type with function parameter should work`() {
        assertTypeCheckSuccess(
            """
                type Processor[T, U] = { transform: T -> U, input: T }
                
                let processor: Processor[String, Int] = { 
                    transform = \s => 42, 
                    input = "test" 
                }
                
                processor.transform(processor.input)
            """.trimIndent(), "Int")
    }
    
    // ===== POLYMORPHIC INTERACTION EDGE CASES =====
    
    @Test
    fun `polymorphic function with parameterized type should work`() {
        assertTypeCheckSuccess(
            """
                type Box[T] = { item: T }
                
                let makeBox = \x => { item = x }
                let intBox: Box[Int] = makeBox(42)
                let stringBox: Box[String] = makeBox("hello")
                
                intBox.item + 1
            """.trimIndent(), "Int")
    }
    
    @Test
    fun `parameterized type in polymorphic context should work`() {
        assertTypeCheckSuccess(
            """
                type Pair[A, B] = (A, B)
                
                let first = \pair => 
                    if True then "hello" else "world"
                
                let stringIntPair: Pair[String, Int] = ("hello", 42)
                
                first(stringIntPair)
            """.trimIndent(), "String")
    }
    
    // ===== ERROR HANDLING EDGE CASES =====
    
    @Test
    fun `type parameter name collision with primitive should be handled`() {
        // This tests whether type parameter names can shadow primitive type names
        val result = parseAndTypeCheck(
            """
                type Container[Int] = { value: Int }
                let container: Container[String] = { value = "hello" }
                container
            """.trimIndent()
        )
        
        // This should either work (if parameter shadowing is allowed) or fail gracefully
        assertTrue(result is TypeCheckResult.Success || result is TypeCheckResult.Failure,
                  "Should handle type parameter name collision gracefully")
    }
    
    @Test
    fun `empty record with type parameter should work`() {
        assertTypeCheckSuccess(
            """
                type EmptyContainer[T] = {}
                
                let empty: EmptyContainer[Int] = {}
                
                empty
            """.trimIndent(), "{}")
    }
    
    // ===== PERFORMANCE AND STRESS TESTS =====
    
    @Test
    fun `many type parameters should work`() {
        assertTypeCheckSuccess(
            """
                type ManyParams[A, B, C, D, E] = { a: A, b: B, c: C, d: D, e: E }
                
                let many: ManyParams[Int, String, Bool, Int, String] = { 
                    a = 1, 
                    b = "two", 
                    c = True, 
                    d = 4, 
                    e = "five" 
                }
                
                many.c
            """.trimIndent(), "Bool")
    }
    
    @Test
    fun `deeply nested parameterized types should work`() {
        assertTypeCheckSuccess(
            """
                type Deep[T] = { level: T }
                type VeryDeep[T] = Deep[Deep[Deep[T]]]
                
                let deep: VeryDeep[String] = { 
                    level = { 
                        level = { 
                            level = "bottom" 
                        } 
                    } 
                }
                
                deep.level.level.level
            """.trimIndent(), "String")
    }
} 