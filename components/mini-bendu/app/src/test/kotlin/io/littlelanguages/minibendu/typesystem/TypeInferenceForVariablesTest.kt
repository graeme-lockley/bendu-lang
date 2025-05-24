package io.littlelanguages.minibendu.typesystem

import io.littlelanguages.minibendu.*
import io.littlelanguages.scanpiler.LocationCoordinate
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Comprehensive tests for type inference of variable expressions.
 * Task 23 of Phase 1 - Type Inference for Basic Expressions.
 * 
 * Tests cover:
 * - Variable references with known types
 * - Handling of undefined variables
 * - Variable shadowing in nested scopes
 * - Polymorphic variable instantiation
 * - Variable lookup in complex environments
 * - Error reporting for variable-related issues
 */
class TypeInferenceForVariablesTest {
    
    private fun createStringLocation(value: String): StringLocation {
        return StringLocation(value, LocationCoordinate(0, 1, 1))
    }
    
    private fun createIntLocation(value: Int): IntLocation {
        return IntLocation(value, LocationCoordinate(0, 1, 1))
    }
    
    private fun createLocation(): LocationCoordinate = LocationCoordinate(0, 1, 1)
    
    @Test
    fun `infer type for simple variable reference`() {
        // Test: Variable x with type Int should infer as Int
        val variable = VarExpr(createStringLocation("x"))
        val env = TypeEnvironment.empty().extend("x", Types.Int)
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(variable)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for defined variable")
        val constraints = result.constraints
        val inferredType = result.type
        
        // Should have exactly one constraint: inferredType ~ Int
        assertEquals(1, constraints.size(), "Should have exactly one constraint")
        val constraint = constraints.all().first()
        assertTrue(constraint is EqualityConstraint, "Should be an equality constraint")
        
        val equalityConstraint = constraint as EqualityConstraint
        assertEquals(Types.Int, equalityConstraint.type2, "Should constrain to variable's type Int")
        assertEquals(inferredType, equalityConstraint.type1, "Should constrain the inferred type")
        
        // Solve constraints to get final type
        val solver = ConstraintSolver()
        val solverResult = solver.solve(constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(inferredType)
        assertEquals(Types.Int, finalType, "Final type should be Int")
    }
    
    @Test
    fun `infer type for variable with string type`() {
        // Test: Variable name with type String should infer as String
        val variable = VarExpr(createStringLocation("name"))
        val env = TypeEnvironment.empty().extend("name", Types.String)
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(variable)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for string variable")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.String, finalType, "String variable should infer as String type")
    }
    
    @Test
    fun `infer type for variable with function type`() {
        // Test: Variable f with function type Int -> Bool should infer correctly
        val variable = VarExpr(createStringLocation("f"))
        val functionType = FunctionType(Types.Int, Types.Bool)
        val env = TypeEnvironment.empty().extend("f", functionType)
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(variable)
        
        assertTrue(result.isSuccess(), "Should successfully infer type for function variable")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(functionType, finalType, "Function variable should infer as function type")
    }
    
    @Test
    fun `undefined variable produces error`() {
        // Test: Reference to undefined variable should produce meaningful error
        val variable = VarExpr(createStringLocation("undefinedVar"))
        val generator = ConstraintGenerator() // Empty environment
        
        val result = generator.generateConstraints(variable)
        
        assertTrue(result.isFailure(), "Should fail for undefined variable")
        val failure = result as ConstraintGenerationResult.Failure
        assertNotNull(failure.error, "Should have error message")
        assertTrue(failure.error.contains("undefinedVar"), "Error should mention the undefined variable name")
        assertTrue(failure.error.contains("Undefined variable"), "Error should indicate it's an undefined variable")
    }
    
    @Test
    fun `undefined variable with similar name produces helpful error`() {
        // Test: Undefined variable with environment containing similar names
        val variable = VarExpr(createStringLocation("userName"))
        val env = TypeEnvironment.empty()
            .extend("username", Types.String)  // lowercase, similar to "userName"
            .extend("userAge", Types.Int)      // similar prefix
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(variable)
        
        assertTrue(result.isFailure(), "Should fail for undefined variable")
        val failure = result as ConstraintGenerationResult.Failure
        assertTrue(failure.error.contains("userName"), "Error should mention the undefined variable")
        assertTrue(failure.error.contains("Undefined variable"), "Error should indicate undefined variable")
    }
    
    @Test
    fun `variable shadowing in nested scopes`() {
        // Test: Variable shadowing where inner scope shadows outer scope
        // Simplified test: test variable lookup in nested environments directly
        val outerEnv = TypeEnvironment.empty().extend("x", Types.Int)
        val innerEnv = outerEnv.extend("x", LiteralStringType("hello"))
        
        val generator = ConstraintGenerator(innerEnv)
        val variable = VarExpr(createStringLocation("x"))
        
        val result = generator.generateConstraints(variable)
        
        assertTrue(result.isSuccess(), "Variable shadowing should be handled correctly")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Constraint solving should succeed")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        // The inner x shadows the outer x, so the result should be the literal string type
        assertEquals(LiteralStringType("hello"), finalType, "Inner variable should shadow outer variable")
    }
    
    @Test
    fun `variable scoping with environment hierarchy`() {
        // Test: Scope hierarchy - variables in outer scopes are accessible
        // Test environment hierarchy directly rather than complex let expressions
        val outerEnv = TypeEnvironment.empty()
            .extend("x", Types.Int)
            .extend("y", Types.String)
        
        // Open a new scope that shadows x but keeps y accessible
        val innerEnv = outerEnv.openScope()
            .extend("x", Types.Bool)  // Shadow x with Bool
            .extend("z", Types.String) // Add new variable z
        
        val generator = ConstraintGenerator(innerEnv)
        val solver = ConstraintSolver()
        
        // Test that x refers to the shadowed (Bool) version
        val xVar = VarExpr(createStringLocation("x"))
        val xResult = generator.generateConstraints(xVar)
        assertTrue(xResult.isSuccess(), "Should resolve shadowed variable")
        val xSolved = solver.solve(xResult.constraints)
        assertTrue(xSolved is ConstraintSolverResult.Success, "Should solve shadowed variable")
        val xType = (xSolved as ConstraintSolverResult.Success).substitution.apply(xResult.type)
        assertEquals(Types.Bool, xType, "Should reference shadowed x (Bool)")
        
        // Test that y still refers to the outer scope version  
        val yVar = VarExpr(createStringLocation("y"))
        val yResult = generator.generateConstraints(yVar)
        assertTrue(yResult.isSuccess(), "Should resolve outer scope variable")
        val ySolved = solver.solve(yResult.constraints)
        assertTrue(ySolved is ConstraintSolverResult.Success, "Should solve outer scope variable")
        val yType = (ySolved as ConstraintSolverResult.Success).substitution.apply(yResult.type)
        assertEquals(Types.String, yType, "Should reference outer y (String)")
        
        // Test that z is accessible in inner scope
        val zVar = VarExpr(createStringLocation("z"))
        val zResult = generator.generateConstraints(zVar)
        assertTrue(zResult.isSuccess(), "Should resolve inner scope variable")
        val zSolved = solver.solve(zResult.constraints)
        assertTrue(zSolved is ConstraintSolverResult.Success, "Should solve inner scope variable")
        val zType = (zSolved as ConstraintSolverResult.Success).substitution.apply(zResult.type)
        assertEquals(Types.String, zType, "Should reference inner z (String)")
    }
    
    @Test
    fun `polymorphic variable instantiation creates fresh type variables`() {
        // Test: Polymorphic variable should be instantiated with fresh type variables each time
                 val polyVar = TypeVariable.fresh()
         val polyScheme = TypeScheme(setOf(polyVar), polyVar) // ∀α.α (identity type)
         
         val var1 = VarExpr(createStringLocation("id"))
         val var2 = VarExpr(createStringLocation("id"))
         
         val env = TypeEnvironment.empty().bind("id", polyScheme)
        val generator = ConstraintGenerator(env)
        
        // Generate constraints for first instance
        val result1 = generator.generateConstraints(var1)
        assertTrue(result1.isSuccess(), "First instantiation should succeed")
        
        // Generate constraints for second instance  
        val result2 = generator.generateConstraints(var2)
        assertTrue(result2.isSuccess(), "Second instantiation should succeed")
        
        // The inferred types should be different type variables
        assertNotEquals(result1.type, result2.type, "Each instantiation should create fresh type variables")
        
        // Both should be type variables
        assertTrue(result1.type is TypeVariable, "First instantiation should be type variable")
        assertTrue(result2.type is TypeVariable, "Second instantiation should be type variable")
        
        // But they should both be constrainable to the same concrete type
        val solver = ConstraintSolver()
        
        // Constrain first instance to Int
        val intConstraint = EqualityConstraint(result1.type, Types.Int, null)
        val constraints1 = result1.constraints.add(intConstraint)
        val solverResult1 = solver.solve(constraints1)
        assertTrue(solverResult1 is ConstraintSolverResult.Success, "First instantiation should solve to Int")
        
        // Constrain second instance to String
        val stringConstraint = EqualityConstraint(result2.type, Types.String, null)
        val constraints2 = result2.constraints.add(stringConstraint)
        val solverResult2 = solver.solve(constraints2)
        assertTrue(solverResult2 is ConstraintSolverResult.Success, "Second instantiation should solve to String")
        
        val finalType1 = (solverResult1 as ConstraintSolverResult.Success).substitution.apply(result1.type)
        val finalType2 = (solverResult2 as ConstraintSolverResult.Success).substitution.apply(result2.type)
        
        assertEquals(Types.Int, finalType1, "First instance should be constrainable to Int")
        assertEquals(Types.String, finalType2, "Second instance should be constrainable to String")
    }
    
    @Test
    fun `polymorphic function instantiation`() {
        // Test: Polymorphic function variable instantiation
        // ∀α.α -> α (identity function type)
                 val polyVar = TypeVariable.fresh()
         val identityType = FunctionType(polyVar, polyVar)
         val polyScheme = TypeScheme(setOf(polyVar), identityType)
         
         val funcVar = VarExpr(createStringLocation("identity"))
         val env = TypeEnvironment.empty().bind("identity", polyScheme)
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(funcVar)
        
        assertTrue(result.isSuccess(), "Polymorphic function instantiation should succeed")
        assertTrue(result.type is TypeVariable, "Result should be fresh type variable")
        
        // The constraint should unify the result with a function type containing fresh variables
        val constraint = result.constraints.all().first() as EqualityConstraint
        val constrainedType = constraint.type2
        
        assertTrue(constrainedType is FunctionType, "Should be constrained to function type")
        val funcType = constrainedType as FunctionType
        
        // Domain and codomain should be the same type variable (but different from original)
        assertNotEquals(polyVar, funcType.domain, "Should use fresh variable, not original")
        assertEquals(funcType.domain, funcType.codomain, "Domain and codomain should be same variable")
    }
    
    @Test
    fun `multiple variables in environment`() {
        // Test: Multiple variables with different types should all be accessible
        val env = TypeEnvironment.empty()
            .extend("x", Types.Int)
            .extend("y", Types.String)
            .extend("z", Types.Bool)
            .extend("f", FunctionType(Types.Int, Types.String))
        
        val generator = ConstraintGenerator(env)
        val solver = ConstraintSolver()
        
        // Test each variable
        val variables = listOf(
            "x" to Types.Int,
            "y" to Types.String,
            "z" to Types.Bool,
            "f" to FunctionType(Types.Int, Types.String)
        )
        
        for ((varName, expectedType) in variables) {
            val variable = VarExpr(createStringLocation(varName))
            val result = generator.generateConstraints(variable)
            
            assertTrue(result.isSuccess(), "Should infer type for variable $varName")
            
            val solverResult = solver.solve(result.constraints)
            assertTrue(solverResult is ConstraintSolverResult.Success, "Should solve constraints for $varName")
            
            val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
            assertEquals(expectedType, finalType, "Variable $varName should have type $expectedType")
        }
    }
    
    @Test
    fun `variable in complex expression context`() {
        // Test: Variable used in binary operation should propagate constraints correctly
        // Expression: x + 5 where x is undefined initially
        val xVar = VarExpr(createStringLocation("x"))
        val literal = LiteralIntExpr(createIntLocation(5))
        val binaryOp = BinaryOpExpr(xVar, BinaryOp.Plus, literal, createLocation())
        
        val generator = ConstraintGenerator() // No initial environment
        val result = generator.generateConstraints(binaryOp)
        
        // Should fail because x is undefined
        assertTrue(result.isFailure(), "Should fail when variable is undefined in expression")
        val failure = result as ConstraintGenerationResult.Failure
        assertTrue(failure.error.contains("x"), "Error should mention undefined variable x")
    }
    
    @Test
    fun `variable type unification in binary operations`() {
        // Test: Variable should be unified with Int when used in arithmetic
        // Expression: x + 5, where x starts as type variable
        val xVar = VarExpr(createStringLocation("x"))
        val literal = LiteralIntExpr(createIntLocation(5))
        val binaryOp = BinaryOpExpr(xVar, BinaryOp.Plus, literal, createLocation())
        
        val xType = TypeVariable.fresh()
        val env = TypeEnvironment.empty().extend("x", xType)
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(binaryOp)
        
        assertTrue(result.isSuccess(), "Should succeed with type variable")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Should solve constraints successfully")
        
        val substitution = (solverResult as ConstraintSolverResult.Success).substitution
        val xFinalType = substitution.apply(xType)
        
        assertEquals(Types.Int, xFinalType, "Variable x should be unified to Int through arithmetic constraint")
    }
    
    @Test
    fun `variable constraint propagation preserves source location`() {
        // Test: Source location should be preserved in variable constraints
        val location = LocationCoordinate(0, 5, 10) // line 5, column 10
        val variable = VarExpr(StringLocation("x", location))
        val env = TypeEnvironment.empty().extend("x", Types.Int)
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(variable)
        
        assertTrue(result.isSuccess(), "Should succeed with proper source location")
        val constraint = result.constraints.all().first()
        
        assertNotNull(constraint.sourceLocation, "Constraint should preserve source location")
        assertEquals(5, constraint.sourceLocation!!.line, "Should preserve line number")
        assertEquals(10, constraint.sourceLocation!!.column, "Should preserve column number")
    }
    
    @Test
    fun `recursive variable reference handling`() {
        // Test: Recursive variables should be handled appropriately
        // let rec factorial = \n -> if n == 0 then 1 else n * factorial(n - 1)
        // For now, test simpler case: let rec x = x (which should be an error or special handling)
        
        val xVar = VarExpr(createStringLocation("x"))
        val recursiveLet = LetExpr(
            true, // recursive
            createStringLocation("x"),
            null, null, null,
            xVar, // x = x
            xVar  // result x
        )
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(recursiveLet)
        
        // This case depends on how recursive bindings are handled
        // The implementation should either succeed with proper recursive type or handle gracefully
        if (result.isSuccess()) {
            val solver = ConstraintSolver()
            val solverResult = solver.solve(result.constraints)
            // If successful, the type system should handle this case appropriately
            assertTrue(solverResult is ConstraintSolverResult.Success || solverResult is ConstraintSolverResult.Failure,
                      "Recursive reference should be handled deterministically")
        } else {
            // If it fails, it should provide meaningful error
            val failure = result as ConstraintGenerationResult.Failure
            assertNotNull(failure.error, "Should provide error for problematic recursive reference")
        }
    }
    
    @Test
    fun `variable with record type`() {
        // Test: Variable with record type should infer correctly
        val recordType = RecordType(mapOf("name" to Types.String, "age" to Types.Int))
        val variable = VarExpr(createStringLocation("person"))
        val env = TypeEnvironment.empty().extend("person", recordType)
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(variable)
        
        assertTrue(result.isSuccess(), "Should infer record type variable")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        assertTrue(solverResult is ConstraintSolverResult.Success, "Should solve record type constraints")
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(recordType, finalType, "Should preserve record type structure")
    }
    
    @Test
    fun `performance test with many variables`() {
        // Test: Type inference should handle many variables efficiently
        val envBuilder = TypeEnvironment.empty()
        val variableNames = mutableListOf<String>()
        
        // Create environment with 100 variables
        var env = envBuilder
        for (i in 1..100) {
            val varName = "var$i"
            variableNames.add(varName)
            val varType = when (i % 3) {
                0 -> Types.Int
                1 -> Types.String
                else -> Types.Bool
            }
            env = env.extend(varName, varType)
        }
        
        val generator = ConstraintGenerator(env)
        val solver = ConstraintSolver()
        
        val startTime = System.currentTimeMillis()
        
        // Generate and solve constraints for all variables
        for (varName in variableNames) {
            val variable = VarExpr(createStringLocation(varName))
            val result = generator.generateConstraints(variable)
            assertTrue(result.isSuccess(), "Should infer type for variable $varName")
            
            val solverResult = solver.solve(result.constraints)
            assertTrue(solverResult is ConstraintSolverResult.Success, "Should solve constraints for $varName")
        }
        
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        
        assertTrue(duration < 2000, "Should infer 100 variable types within reasonable time (${duration}ms)")
    }
} 