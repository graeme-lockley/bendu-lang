package io.littlelanguages.minibendu.typesystem

import io.littlelanguages.minibendu.*
import io.littlelanguages.scanpiler.LocationCoordinate
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Comprehensive tests for type inference of literal values.
 * Task 21 of Phase 1 - Type Inference for Basic Expressions.
 * 
 * Tests cover:
 * - Integer literals: 42, -10, 0, large values
 * - String literals: "hello", "", "multi word", special characters  
 * - Boolean literals: true, false
 * - String literal types: "success" as a type vs "success" as a value
 * - Union types with string literals: "pending" | "fulfilled" | "rejected"
 * - Type compatibility and assignability
 * - Performance with many literals
 */
class TypeInferenceForLiteralsTest {
    
    private fun createStringLocation(value: String): StringLocation {
        return StringLocation(value, LocationCoordinate(0, 1, 1))
    }
    
    private fun createIntLocation(value: Int): IntLocation {
        return IntLocation(value, LocationCoordinate(0, 1, 1))
    }
    
    private fun createBoolLocation(value: Boolean): BoolLocation {
        return BoolLocation(value, LocationCoordinate(0, 1, 1))
    }
    
    @Test
    fun `infer type for positive integer literal`() {
        // Test: 42 should infer as Int
        val literal = LiteralIntExpr(createIntLocation(42))
        val generator = ConstraintGenerator()
        
        val result = generator.generateConstraints(literal)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for positive integer")
        val constraints = result.constraints
        val inferredType = result.type
        
        // Should have exactly one constraint: inferredType ~ Int
        assertEquals(1, constraints.size(), "Should have exactly one constraint")
        val constraint = constraints.all().first()
        assertTrue(constraint is EqualityConstraint, "Should be an equality constraint")
        
        val equalityConstraint = constraint as EqualityConstraint
        assertEquals(Types.Int, equalityConstraint.type2, "Should constrain to Int type")
        assertEquals(inferredType, equalityConstraint.type1, "Should constrain the inferred type")
        
        // Solve constraints to get final type
        val solver = ConstraintSolver()
        val solverResult = solver.solve(constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(inferredType)
        assertEquals(Types.Int, finalType, "Final type should be Int")
    }
    
    @Test
    fun `infer type for negative integer literal`() {
        // Test: -42 should infer as Int
        val literal = LiteralIntExpr(createIntLocation(-42))
        val generator = ConstraintGenerator()
        
        val result = generator.generateConstraints(literal)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for negative integer")
        val constraints = result.constraints
        val inferredType = result.type
        
        // Solve constraints
        val solver = ConstraintSolver()
        val solverResult = solver.solve(constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(inferredType)
        assertEquals(Types.Int, finalType, "Negative integer should infer as Int type")
    }
    
    @Test
    fun `infer type for zero integer literal`() {
        // Test: 0 should infer as Int
        val literal = LiteralIntExpr(createIntLocation(0))
        val generator = ConstraintGenerator()
        
        val result = generator.generateConstraints(literal)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for zero")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.Int, finalType, "Zero should infer as Int type")
    }
    
    @Test
    fun `infer type for large integer literal`() {
        // Test: Very large integers should still infer as Int
        val literal = LiteralIntExpr(createIntLocation(2147483647)) // Max int
        val generator = ConstraintGenerator()
        
        val result = generator.generateConstraints(literal)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for large integer")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.Int, finalType, "Large integer should infer as Int type")
    }
    
    @Test
    fun `infer type for simple string literal`() {
        // Test: "hello" should infer as literal string type "hello"
        val literal = LiteralStringExpr(createStringLocation("hello"))
        val generator = ConstraintGenerator()
        
        val result = generator.generateConstraints(literal)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for string literal")
        val constraints = result.constraints
        val inferredType = result.type
        
        // Should have exactly one constraint: inferredType ~ "hello"
        assertEquals(1, constraints.size(), "Should have exactly one constraint")
        val constraint = constraints.all().first()
        assertTrue(constraint is EqualityConstraint, "Should be an equality constraint")
        
        val equalityConstraint = constraint as EqualityConstraint
        val expectedType = LiteralStringType("hello")
        assertEquals(expectedType, equalityConstraint.type2, "Should constrain to literal string type 'hello'")
        assertEquals(inferredType, equalityConstraint.type1, "Should constrain the inferred type")
        
        // Solve constraints to get final type
        val solver = ConstraintSolver()
        val solverResult = solver.solve(constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(inferredType)
        assertEquals(expectedType, finalType, "Final type should be literal string type 'hello'")
    }
    
    @Test
    fun `infer type for empty string literal`() {
        // Test: "" should infer as literal string type ""
        val literal = LiteralStringExpr(createStringLocation(""))
        val generator = ConstraintGenerator()
        
        val result = generator.generateConstraints(literal)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for empty string")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        val expectedType = LiteralStringType("")
        assertEquals(expectedType, finalType, "Empty string should infer as literal string type ''")
    }
    
    @Test
    fun `infer type for multi-word string literal`() {
        // Test: "hello world" should infer as literal string type "hello world"
        val literal = LiteralStringExpr(createStringLocation("hello world"))
        val generator = ConstraintGenerator()
        
        val result = generator.generateConstraints(literal)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for multi-word string")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        val expectedType = LiteralStringType("hello world")
        assertEquals(expectedType, finalType, "Multi-word string should infer as literal string type 'hello world'")
    }
    
    @Test
    fun `infer type for string with special characters`() {
        // Test: "hello\nworld\t!" should infer as literal string type "hello\nworld\t!"
        val literal = LiteralStringExpr(createStringLocation("hello\nworld\t!"))
        val generator = ConstraintGenerator()
        
        val result = generator.generateConstraints(literal)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for string with special chars")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        val expectedType = LiteralStringType("hello\nworld\t!")
        assertEquals(expectedType, finalType, "String with special characters should infer as literal string type")
    }
    
    @Test
    fun `infer type for true boolean literal`() {
        // Test: true should infer as Bool
        val literal = LiteralBoolExpr(createBoolLocation(true))
        val generator = ConstraintGenerator()
        
        val result = generator.generateConstraints(literal)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for true literal")
        val constraints = result.constraints
        val inferredType = result.type
        
        // Should have exactly one constraint: inferredType ~ Bool
        assertEquals(1, constraints.size(), "Should have exactly one constraint")
        val constraint = constraints.all().first()
        assertTrue(constraint is EqualityConstraint, "Should be an equality constraint")
        
        val equalityConstraint = constraint as EqualityConstraint
        assertEquals(Types.Bool, equalityConstraint.type2, "Should constrain to Bool type")
        assertEquals(inferredType, equalityConstraint.type1, "Should constrain the inferred type")
        
        // Solve constraints to get final type
        val solver = ConstraintSolver()
        val solverResult = solver.solve(constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(inferredType)
        assertEquals(Types.Bool, finalType, "Final type should be Bool")
    }
    
    @Test
    fun `infer type for false boolean literal`() {
        // Test: false should infer as Bool
        val literal = LiteralBoolExpr(createBoolLocation(false))
        val generator = ConstraintGenerator()
        
        val result = generator.generateConstraints(literal)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for false literal")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.Bool, finalType, "false should infer as Bool type")
    }
    
    @Test
    fun `string literal types as discriminated unions`() {
        // Test: String literal types for discriminated unions
        // This tests the foundation for string literal types, even though full implementation is in Task 22
        
        val successLiteral = Types.literal("success")
        val errorLiteral = Types.literal("error")
        val pendingLiteral = Types.literal("pending")
        
        // Test that literal types are distinct
        assertFalse(successLiteral.structurallyEquivalent(errorLiteral), 
                   "Different string literal types should not be equivalent")
        assertFalse(errorLiteral.structurallyEquivalent(pendingLiteral), 
                   "Different string literal types should not be equivalent")
        
        // Test that literal types have correct string values
        assertEquals("success", successLiteral.value, "Success literal should have correct value")
        assertEquals("error", errorLiteral.value, "Error literal should have correct value")
        assertEquals("pending", pendingLiteral.value, "Pending literal should have correct value")
        
        // Test string representation
        assertEquals("\"success\"", successLiteral.toString(), "Success literal should have correct string representation")
        assertEquals("\"error\"", errorLiteral.toString(), "Error literal should have correct string representation")
    }
    
    @Test
    fun `union types with string literals`() {
        // Test: Union of string literal types ["pending", "fulfilled", "rejected"]
        val pendingType = Types.literal("pending")
        val fulfilledType = Types.literal("fulfilled")
        val rejectedType = Types.literal("rejected")
        
        // Create union type
        val statusUnion = UnionType(setOf(pendingType, fulfilledType, rejectedType))
        
        // Test union properties
        assertEquals(3, statusUnion.alternatives.size, "Union should have 3 alternatives")
        assertTrue(statusUnion.alternatives.contains(pendingType), "Union should contain pending")
        assertTrue(statusUnion.alternatives.contains(fulfilledType), "Union should contain fulfilled")
        assertTrue(statusUnion.alternatives.contains(rejectedType), "Union should contain rejected")
        
        // Test union string representation
        val unionStr = statusUnion.toString()
        assertTrue(unionStr.contains("pending"), "Union string should contain pending")
        assertTrue(unionStr.contains("fulfilled"), "Union string should contain fulfilled")
        assertTrue(unionStr.contains("rejected"), "Union string should contain rejected")
    }
    
    @Test
    fun `mixed union type with literal and primitive types`() {
        // Test: Union of string literal and boolean: "loading" | Bool
        val loadingType = Types.literal("loading")
        val mixedUnion = UnionType(setOf(loadingType, Types.Bool))
        
        // Test union properties
        assertEquals(2, mixedUnion.alternatives.size, "Mixed union should have 2 alternatives")
        assertTrue(mixedUnion.alternatives.contains(loadingType), "Union should contain loading literal")
        assertTrue(mixedUnion.alternatives.contains(Types.Bool), "Union should contain Bool type")
        
        // Test structural equivalence
        val sameUnion = UnionType(setOf(Types.Bool, loadingType)) // Different order
        assertTrue(mixedUnion.structurallyEquivalent(sameUnion), 
                  "Union types should be equivalent regardless of order")
    }
    
    @Test
    fun `literal types are distinct from each other`() {
        // Test: "success" ≠ "error" as literal types
        val successType = Types.literal("success")
        val errorType = Types.literal("error")
        
        assertFalse(successType.structurallyEquivalent(errorType), 
                   "Different string literal types should not be equal")
        assertNotEquals(successType, errorType, 
                       "Different string literal types should not be equal")
        assertNotEquals(successType.hashCode(), errorType.hashCode(), 
                       "Different string literal types should have different hash codes")
    }
    
    @Test
    fun `literal types have no free variables`() {
        // Test: String literal types should have no free type variables
        val successType = Types.literal("success")
        val errorType = Types.literal("error")
        
        assertTrue(successType.freeTypeVariables().isEmpty(), 
                  "String literal types should have no free variables")
        assertTrue(errorType.freeTypeVariables().isEmpty(), 
                  "String literal types should have no free variables")
    }
    
    @Test
    fun `constraint generation preserves source location`() {
        // Test: Constraint generation should preserve source location information
        val location = LocationCoordinate(5, 10, 15) // offset=5, line=10, column=15
        val literal = LiteralIntExpr(IntLocation(42, location))
        val generator = ConstraintGenerator()
        
        val result = generator.generateConstraints(literal)
        
        assertTrue(result.isSuccess(), "Should successfully generate constraints")
        val constraint = result.constraints.all().first() as EqualityConstraint
        
        assertNotNull(constraint.sourceLocation, "Constraint should have source location")
        assertEquals(10, constraint.sourceLocation!!.line, "Should preserve line number")
        assertEquals(15, constraint.sourceLocation!!.column, "Should preserve column number")
    }
    
    @Test
    fun `multiple literals with different types`() {
        // Test: Generate constraints for different literal types in sequence
        val intLiteral = LiteralIntExpr(createIntLocation(123))
        val stringLiteral = LiteralStringExpr(createStringLocation("test"))
        val boolLiteral = LiteralBoolExpr(createBoolLocation(true))
        
        val generator = ConstraintGenerator()
        val solver = ConstraintSolver()
        
        // Test integer literal
        val intResult = generator.generateConstraints(intLiteral)
        assertTrue(intResult.isSuccess(), "Int literal constraint generation should succeed")
        val intSolved = solver.solve(intResult.constraints)
        assertTrue(intSolved is ConstraintSolverResult.Success, "Int literal solving should succeed")
        val intType = (intSolved as ConstraintSolverResult.Success).substitution.apply(intResult.type)
        assertEquals(Types.Int, intType, "Int literal should infer as Int")
        
        // Test string literal
        val stringResult = generator.generateConstraints(stringLiteral)
        assertTrue(stringResult.isSuccess(), "String literal constraint generation should succeed")
        val stringSolved = solver.solve(stringResult.constraints)
        assertTrue(stringSolved is ConstraintSolverResult.Success, "String literal solving should succeed")
        val stringType = (stringSolved as ConstraintSolverResult.Success).substitution.apply(stringResult.type)
        val expectedStringType = LiteralStringType("test")
        assertEquals(expectedStringType, stringType, "String literal should infer as literal string type 'test'")
        
        // Test boolean literal
        val boolResult = generator.generateConstraints(boolLiteral)
        assertTrue(boolResult.isSuccess(), "Bool literal constraint generation should succeed")
        val boolSolved = solver.solve(boolResult.constraints)
        assertTrue(boolSolved is ConstraintSolverResult.Success, "Bool literal solving should succeed")
        val boolType = (boolSolved as ConstraintSolverResult.Success).substitution.apply(boolResult.type)
        assertEquals(Types.Bool, boolType, "Bool literal should infer as Bool")
    }
    
    @Test
    fun `performance test with many literal inferences`() {
        // Test: Type inference should handle many literals efficiently
        val generator = ConstraintGenerator()
        val solver = ConstraintSolver()
        
        val startTime = System.currentTimeMillis()
        
        // Generate and solve constraints for 100 different literals
        for (i in 1..100) {
            val intLiteral = LiteralIntExpr(createIntLocation(i))
            val stringLiteral = LiteralStringExpr(createStringLocation("literal_$i"))
            val boolLiteral = LiteralBoolExpr(createBoolLocation(i % 2 == 0))
            
            val intResult = generator.generateConstraints(intLiteral)
            assertTrue(intResult.isSuccess(), "Should successfully infer type for int literal $i")
            val intSolved = solver.solve(intResult.constraints)
            assertTrue(intSolved is ConstraintSolverResult.Success, "Should solve constraints for int literal $i")
            
            val stringResult = generator.generateConstraints(stringLiteral)
            assertTrue(stringResult.isSuccess(), "Should successfully infer type for string literal $i")
            val stringSolved = solver.solve(stringResult.constraints)
            assertTrue(stringSolved is ConstraintSolverResult.Success, "Should solve constraints for string literal $i")
            
            val boolResult = generator.generateConstraints(boolLiteral)
            assertTrue(boolResult.isSuccess(), "Should successfully infer type for bool literal $i")
            val boolSolved = solver.solve(boolResult.constraints)
            assertTrue(boolSolved is ConstraintSolverResult.Success, "Should solve constraints for bool literal $i")
        }
        
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        
        assertTrue(duration < 2000, "Should infer 300 literal types within reasonable time (${duration}ms)")
    }
    
    @Test
    fun `literal type constraints are well-formed`() {
        // Test: Generated constraints should be well-formed and solvable
        val literals = listOf(
            LiteralIntExpr(createIntLocation(42)),
            LiteralStringExpr(createStringLocation("hello")),
            LiteralBoolExpr(createBoolLocation(true))
        )
        
        val generator = ConstraintGenerator()
        
        for ((index, literal) in literals.withIndex()) {
            val result = generator.generateConstraints(literal)
            assertTrue(result.isSuccess(), "Constraint generation should succeed for literal $index")
            
            val constraints = result.constraints
            assertEquals(1, constraints.size(), "Should have exactly one constraint for literal $index")
            
            val constraint = constraints.all().first()
            assertTrue(constraint is EqualityConstraint, "Should be equality constraint for literal $index")
            
            val eqConstraint = constraint as EqualityConstraint
            assertTrue(eqConstraint.type1 is TypeVariable, "First type should be a type variable for literal $index")
            // Note: String literals now produce LiteralStringType, not PrimitiveType
            val isExpectedType = when (literal) {
                is LiteralIntExpr -> eqConstraint.type2 is PrimitiveType
                is LiteralBoolExpr -> eqConstraint.type2 is PrimitiveType
                is LiteralStringExpr -> eqConstraint.type2 is LiteralStringType
                else -> false
            }
            assertTrue(isExpectedType, "Second type should be appropriate for literal type $index")
            
            // Verify no inconsistencies
            val inconsistency = constraints.findInconsistency()
            assertNull(inconsistency, "Should have no constraint inconsistencies for literal $index")
        }
    }
} 