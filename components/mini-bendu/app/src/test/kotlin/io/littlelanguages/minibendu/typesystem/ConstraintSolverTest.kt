package io.littlelanguages.minibendu.typesystem

import io.littlelanguages.scanpiler.LocationCoordinate
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Comprehensive tests for constraint solving.
 * Tests cover simple cases, variables, functions, error handling, and performance.
 */
class ConstraintSolverTest {
    
    private fun createSourceLocation(): SourceLocation {
        return SourceLocation(1, 1, "test.bendu")
    }
    
    @Test
    fun `solve simple equality constraint with concrete types`() {
        // Constraint: Int ~ Int (should be trivially satisfied)
        val constraint = EqualityConstraint(Types.Int, Types.Int, createSourceLocation())
        val constraintSet = ConstraintSet.of(constraint)
        val solver = ConstraintSolver()
        
        val result = solver.solve(constraintSet)
        
        assertTrue(result is ConstraintSolverResult.Success, "Should successfully solve trivial constraint")
        val solution = (result as ConstraintSolverResult.Success).substitution
        assertTrue(solution.isEmpty(), "Should have empty substitution for trivial constraint")
    }
    
    @Test
    fun `solve equality constraint with type variable`() {
        // Constraint: a ~ Int (should bind a to Int)
        val typeVar = TypeVariable.fresh()
        val constraint = EqualityConstraint(typeVar, Types.Int, createSourceLocation())
        val constraintSet = ConstraintSet.of(constraint)
        val solver = ConstraintSolver()
        
        val result = solver.solve(constraintSet)
        
        assertTrue(result is ConstraintSolverResult.Success, "Should successfully solve variable constraint")
        val solution = (result as ConstraintSolverResult.Success).substitution
        assertEquals(Types.Int, solution.apply(typeVar), "Should bind type variable to Int")
    }
    
    @Test
    fun `solve multiple connected constraints`() {
        // Constraints: a ~ Int, b ~ a (should bind both a and b to Int)
        val typeVarA = TypeVariable.fresh()
        val typeVarB = TypeVariable.fresh()
        val constraint1 = EqualityConstraint(typeVarA, Types.Int, createSourceLocation())
        val constraint2 = EqualityConstraint(typeVarB, typeVarA, createSourceLocation())
        val constraintSet = ConstraintSet.of(constraint1, constraint2)
        val solver = ConstraintSolver()
        
        val result = solver.solve(constraintSet)
        
        assertTrue(result is ConstraintSolverResult.Success, "Should successfully solve connected constraints")
        val solution = (result as ConstraintSolverResult.Success).substitution
        assertEquals(Types.Int, solution.apply(typeVarA), "Should bind a to Int")
        assertEquals(Types.Int, solution.apply(typeVarB), "Should bind b to Int")
    }
    
    @Test
    fun `solve function type constraints`() {
        // Constraint: f ~ (Int -> String)
        val funcVar = TypeVariable.fresh()
        val funcType = FunctionType(Types.Int, Types.String)
        val constraint = EqualityConstraint(funcVar, funcType, createSourceLocation())
        val constraintSet = ConstraintSet.of(constraint)
        val solver = ConstraintSolver()
        
        val result = solver.solve(constraintSet)
        
        assertTrue(result is ConstraintSolverResult.Success, "Should successfully solve function constraint")
        val solution = (result as ConstraintSolverResult.Success).substitution
        val solvedType = solution.apply(funcVar)
        assertTrue(solvedType is FunctionType, "Should resolve to function type")
        val solvedFunc = solvedType as FunctionType
        assertEquals(Types.Int, solvedFunc.domain, "Should have correct domain")
        assertEquals(Types.String, solvedFunc.codomain, "Should have correct codomain")
    }
    
    @Test
    fun `solve complex function application constraint system`() {
        // Simulating: f(x) where f : a -> b, x : a, result : b
        val funcVar = TypeVariable.fresh()
        val argVar = TypeVariable.fresh()
        val resultVar = TypeVariable.fresh()
        
        // f ~ (a -> b)
        val functionConstraint = EqualityConstraint(
            funcVar, 
            FunctionType(argVar, resultVar), 
            createSourceLocation()
        )
        // x ~ Int
        val argConstraint = EqualityConstraint(argVar, Types.Int, createSourceLocation())
        
        val constraintSet = ConstraintSet.of(functionConstraint, argConstraint)
        val solver = ConstraintSolver()
        
        val result = solver.solve(constraintSet)
        
        assertTrue(result is ConstraintSolverResult.Success, "Should successfully solve function application constraints")
        val solution = (result as ConstraintSolverResult.Success).substitution
        
        // Check that argVar was bound to Int
        assertEquals(Types.Int, solution.apply(argVar), "Argument should be bound to Int")
        
        // Check that funcVar was bound to (Int -> b) for some b
        val solvedFunc = solution.apply(funcVar)
        assertTrue(solvedFunc is FunctionType, "Function should be resolved to function type")
        val funcType = solvedFunc as FunctionType
        assertEquals(Types.Int, funcType.domain, "Function domain should be Int")
        
        // resultVar should be bound to the same type as the function's codomain
        assertEquals(solution.apply(resultVar), funcType.codomain, "Result should match function codomain")
    }
    
    @Test
    fun `solve record type constraints`() {
        // Constraint: r ~ {name: String, age: Int}
        val recordVar = TypeVariable.fresh()
        val recordType = RecordType(mapOf(
            "name" to Types.String,
            "age" to Types.Int
        ))
        val constraint = EqualityConstraint(recordVar, recordType, createSourceLocation())
        val constraintSet = ConstraintSet.of(constraint)
        val solver = ConstraintSolver()
        
        val result = solver.solve(constraintSet)
        
        assertTrue(result is ConstraintSolverResult.Success, "Should successfully solve record constraint")
        val solution = (result as ConstraintSolverResult.Success).substitution
        val solvedType = solution.apply(recordVar)
        assertTrue(solvedType is RecordType, "Should resolve to record type")
        val solvedRecord = solvedType as RecordType
        assertEquals(Types.String, solvedRecord.fields["name"], "Should have correct name field type")
        assertEquals(Types.Int, solvedRecord.fields["age"], "Should have correct age field type")
    }
    
    @Test
    fun `fail to solve contradictory constraints`() {
        // Contradictory constraints: a ~ Int, a ~ String
        val typeVar = TypeVariable.fresh()
        val constraint1 = EqualityConstraint(typeVar, Types.Int, createSourceLocation())
        val constraint2 = EqualityConstraint(typeVar, Types.String, createSourceLocation())
        val constraintSet = ConstraintSet.of(constraint1, constraint2)
        val solver = ConstraintSolver()
        
        val result = solver.solve(constraintSet)
        
        assertTrue(result is ConstraintSolverResult.Failure, "Should fail to solve contradictory constraints")
        val failure = result as ConstraintSolverResult.Failure
        assertNotNull(failure.error, "Should have error message")
        assertTrue(failure.error.contains("Int") && failure.error.contains("String"), 
                  "Error should mention both conflicting types")
    }
    
    @Test
    fun `fail to solve infinite type (occurs check)`() {
        // Infinite type: a ~ (a -> String) 
        val typeVar = TypeVariable.fresh()
        val functionType = FunctionType(typeVar, Types.String)
        val constraint = EqualityConstraint(typeVar, functionType, createSourceLocation())
        val constraintSet = ConstraintSet.of(constraint)
        val solver = ConstraintSolver()
        
        val result = solver.solve(constraintSet)
        
        assertTrue(result is ConstraintSolverResult.Failure, "Should fail occurs check")
        val failure = result as ConstraintSolverResult.Failure
        assertNotNull(failure.error, "Should have error message")
        assertTrue(failure.error.contains("occurs") || failure.error.contains("infinite"), 
                  "Error should mention occurs check or infinite type")
    }
    
    @Test
    fun `fail to solve incompatible function types`() {
        // Incompatible constraints: f ~ (Int -> String), f ~ (Bool -> Int)
        val funcVar = TypeVariable.fresh()
        val funcType1 = FunctionType(Types.Int, Types.String)
        val funcType2 = FunctionType(Types.Bool, Types.Int)
        val constraint1 = EqualityConstraint(funcVar, funcType1, createSourceLocation())
        val constraint2 = EqualityConstraint(funcVar, funcType2, createSourceLocation())
        val constraintSet = ConstraintSet.of(constraint1, constraint2)
        val solver = ConstraintSolver()
        
        val result = solver.solve(constraintSet)
        
        assertTrue(result is ConstraintSolverResult.Failure, "Should fail to solve incompatible function types")
        val failure = result as ConstraintSolverResult.Failure
        assertNotNull(failure.error, "Should have error message")
    }
    
    @Test
    fun `solve constraints with proper priority ordering`() {
        // Mix of equality and subtyping constraints
        val typeVar = TypeVariable.fresh()
        val recordSuper = RecordType(mapOf("x" to Types.Int))
        val recordSub = RecordType(mapOf("x" to Types.Int, "y" to Types.String))
        
        // Higher priority: a ~ {x: Int, y: String}
        val equalityConstraint = EqualityConstraint(typeVar, recordSub, createSourceLocation())
        // Lower priority: a <: {x: Int}
        val subtypingConstraint = SubtypingConstraint(typeVar, recordSuper, createSourceLocation())
        
        val constraintSet = ConstraintSet.of(subtypingConstraint, equalityConstraint)
        val solver = ConstraintSolver()
        
        val result = solver.solve(constraintSet)
        
        assertTrue(result is ConstraintSolverResult.Success, "Should successfully solve with priority ordering")
        val solution = (result as ConstraintSolverResult.Success).substitution
        val solvedType = solution.apply(typeVar)
        assertEquals(recordSub, solvedType, "Should bind to the more specific type from equality constraint")
    }
    
    @Test
    fun `solve constraint system with dependency tracking`() {
        // Chain of dependencies: a ~ b, b ~ c, c ~ Int
        val varA = TypeVariable.fresh()
        val varB = TypeVariable.fresh()
        val varC = TypeVariable.fresh()
        
        val constraint1 = EqualityConstraint(varA, varB, createSourceLocation())
        val constraint2 = EqualityConstraint(varB, varC, createSourceLocation())
        val constraint3 = EqualityConstraint(varC, Types.Int, createSourceLocation())
        
        val constraintSet = ConstraintSet.of(constraint1, constraint2, constraint3)
        val solver = ConstraintSolver()
        
        val result = solver.solve(constraintSet)
        
        assertTrue(result is ConstraintSolverResult.Success, "Should successfully solve dependency chain")
        val solution = (result as ConstraintSolverResult.Success).substitution
        assertEquals(Types.Int, solution.apply(varA), "Should propagate through dependency chain to a")
        assertEquals(Types.Int, solution.apply(varB), "Should propagate through dependency chain to b")
        assertEquals(Types.Int, solution.apply(varC), "Should propagate through dependency chain to c")
    }
    
    @Test
    fun `performance test on complex constraint set`() {
        // Create a large constraint system to test performance
        val variables = (1..100).map { TypeVariable.fresh() }
        val constraints = mutableListOf<TypeConstraint>()
        
        // Create chain constraints: v1 ~ v2, v2 ~ v3, ..., v99 ~ v100, v100 ~ Int
        for (i in 0 until variables.size - 1) {
            constraints.add(EqualityConstraint(variables[i], variables[i + 1], createSourceLocation()))
        }
        constraints.add(EqualityConstraint(variables.last(), Types.Int, createSourceLocation()))
        
        // Add some cross-links for complexity
        for (i in 0 until variables.size step 10) {
            val j = (i + 5) % variables.size
            constraints.add(EqualityConstraint(variables[i], variables[j], createSourceLocation()))
        }
        
        val constraintSet = ConstraintSet.of(constraints)
        val solver = ConstraintSolver()
        
        val startTime = System.currentTimeMillis()
        val result = solver.solve(constraintSet)
        val endTime = System.currentTimeMillis()
        
        assertTrue(result is ConstraintSolverResult.Success, "Should successfully solve complex constraint set")
        
        // Performance check - should complete within reasonable time (10 seconds is very generous)
        val timeElapsed = endTime - startTime
        assertTrue(timeElapsed < 10000, "Should solve complex constraints in reasonable time (${timeElapsed}ms)")
        
        // Verify all variables are bound to Int
        val solution = (result as ConstraintSolverResult.Success).substitution
        variables.forEach { variable ->
            assertEquals(Types.Int, solution.apply(variable), "All variables should be bound to Int")
        }
    }
    
    @Test
    fun `solve constraints with source location tracking`() {
        // Test that error messages include source location information
        val typeVar = TypeVariable.fresh()
        val location1 = SourceLocation(10, 5, "file1.bendu")
        val location2 = SourceLocation(20, 10, "file2.bendu")
        
        val constraint1 = EqualityConstraint(typeVar, Types.Int, location1)
        val constraint2 = EqualityConstraint(typeVar, Types.String, location2)
        val constraintSet = ConstraintSet.of(constraint1, constraint2)
        val solver = ConstraintSolver()
        
        val result = solver.solve(constraintSet)
        
        assertTrue(result is ConstraintSolverResult.Failure, "Should fail to solve contradictory constraints")
        val failure = result as ConstraintSolverResult.Failure
        
        // Error should include source location information
        assertTrue(failure.error.contains("file1.bendu") || failure.error.contains("file2.bendu") ||
                  failure.error.contains("10:5") || failure.error.contains("20:10"),
                  "Error message should include source location information")
    }
} 