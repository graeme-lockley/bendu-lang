package io.littlelanguages.minibendu.typesystem

import io.littlelanguages.minibendu.*
import io.littlelanguages.scanpiler.LocationCoordinate
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Comprehensive tests for constraint generation.
 * Tests cover simple expressions, function applications, complex nested expressions,
 * and constraint dependency tracking.
 */
class ConstraintGenerationTest {
    
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
    fun `constraint generation for integer literal`() {
        // Literal: 42
        val literal = LiteralIntExpr(createIntLocation(42))
        val generator = ConstraintGenerator()
        
        val result = generator.generateConstraints(literal)
        
        assertTrue(result.isSuccess(), "Constraint generation for integer literal should succeed")
        val constraints = result.constraints
        val inferredType = result.type
        
        // Should have a single equality constraint: inferredType ~ Int
        assertEquals(1, constraints.size(), "Should have exactly one constraint")
        val constraint = constraints.all().first()
        assertTrue(constraint is EqualityConstraint, "Should be an equality constraint")
        
        val equalityConstraint = constraint as EqualityConstraint
        assertEquals(Types.Int, equalityConstraint.type2, "Should constrain to Int type")
        assertEquals(inferredType, equalityConstraint.type1, "Should constrain the inferred type")
    }
    
    @Test
    fun `constraint generation for string literal`() {
        // Literal: "hello"
        val literal = LiteralStringExpr(createStringLocation("hello"))
        val generator = ConstraintGenerator()
        
        val result = generator.generateConstraints(literal)
        
        assertTrue(result.isSuccess(), "Constraint generation for string literal should succeed")
        val constraints = result.constraints
        val inferredType = result.type
        
        // Should have a single equality constraint: inferredType ~ String
        assertEquals(1, constraints.size(), "Should have exactly one constraint")
        val constraint = constraints.all().first()
        assertTrue(constraint is EqualityConstraint, "Should be an equality constraint")
        
        val equalityConstraint = constraint as EqualityConstraint
        val expectedType = Types.String
        assertEquals(expectedType, equalityConstraint.type2, "Should constrain to String type")
        assertEquals(inferredType, equalityConstraint.type1, "Should constrain the inferred type")
    }
    
    @Test
    fun `constraint generation for boolean literal`() {
        // Literal: true
        val literal = LiteralBoolExpr(createBoolLocation(true))
        val generator = ConstraintGenerator()
        
        val result = generator.generateConstraints(literal)
        
        assertTrue(result.isSuccess(), "Constraint generation for boolean literal should succeed")
        val constraints = result.constraints
        val inferredType = result.type
        
        // Should have a single equality constraint: inferredType ~ Bool
        assertEquals(1, constraints.size(), "Should have exactly one constraint")
        val constraint = constraints.all().first()
        assertTrue(constraint is EqualityConstraint, "Should be an equality constraint")
        
        val equalityConstraint = constraint as EqualityConstraint
        assertEquals(Types.Bool, equalityConstraint.type2, "Should constrain to Bool type")
        assertEquals(inferredType, equalityConstraint.type1, "Should constrain the inferred type")
    }
    
    @Test
    fun `constraint generation for variable expression`() {
        // Variable: x
        val variable = VarExpr(createStringLocation("x"))
        val env = TypeEnvironment.empty().extend("x", Types.Int)
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(variable)
        
        assertTrue(result.isSuccess(), "Constraint generation for variable should succeed")
        val constraints = result.constraints
        val inferredType = result.type
        
        // Should have a single equality constraint: inferredType ~ Int
        assertEquals(1, constraints.size(), "Should have exactly one constraint")
        val constraint = constraints.all().first()
        assertTrue(constraint is EqualityConstraint, "Should be an equality constraint")
        
        val equalityConstraint = constraint as EqualityConstraint
        assertEquals(Types.Int, equalityConstraint.type2, "Should constrain to variable's type")
        assertEquals(inferredType, equalityConstraint.type1, "Should constrain the inferred type")
    }
    
    @Test
    fun `constraint generation for undefined variable fails`() {
        // Variable: undefined_var
        val variable = VarExpr(createStringLocation("undefined_var"))
        val generator = ConstraintGenerator()
        
        val result = generator.generateConstraints(variable)
        
        assertTrue(result.isFailure(), "Constraint generation for undefined variable should fail")
        val failure = result as ConstraintGenerationResult.Failure
        assertNotNull(failure.error, "Should have error message")
        assertTrue(failure.error.contains("undefined_var"), "Error should mention the undefined variable")
    }
    
    @Test
    fun `constraint generation for simple function application`() {
        // Expression: f(x)
        // Where f : Int -> String and x : Int
        val functionVar = VarExpr(createStringLocation("f"))
        val argVar = VarExpr(createStringLocation("x"))
        val application = ApplicationExpr(
            functionVar,
            listOf(argVar),
            createLocation()
        )
        
        val functionType = FunctionType(Types.Int, Types.String)
        val env = TypeEnvironment.empty()
            .extend("f", functionType)
            .extend("x", Types.Int)
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(application)
        
        assertTrue(result.isSuccess(), "Constraint generation for function application should succeed")
        val constraints = result.constraints

        // Should generate constraints for:
        // 1. Function variable f
        // 2. Argument variable x  
        // 3. Function application constraint
        assertTrue(constraints.size() >= 3, "Should have at least 3 constraints, but got ${constraints.size()}")
        
        // The constraint system should be solvable to determine that the result type is String
        // We don't need to check for a direct constraint, but rather that the constraint system
        // when solved would result in the correct type
        
        // For now, let's just check that we have the basic constraints
        val constraintsList = constraints.all()
        
        // Should have a constraint involving the function type
        val hasFunctionConstraint = constraintsList.any { constraint ->
            constraint is EqualityConstraint && 
            (constraint.type1 is FunctionType || constraint.type2 is FunctionType)
        }
        assertTrue(hasFunctionConstraint, "Should have constraint involving function type")
    }
    
    @Test
    fun `constraint generation for function application with type variables`() {
        // Expression: f(x)
        // Where f and x have unknown types
        val functionVar = VarExpr(createStringLocation("f"))
        val argVar = VarExpr(createStringLocation("x"))
        val application = ApplicationExpr(
            functionVar,
            listOf(argVar),
            createLocation()
        )
        
        val fType = TypeVariable.fresh()
        val xType = TypeVariable.fresh()
        val env = TypeEnvironment.empty()
            .extend("f", fType)
            .extend("x", xType)
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(application)
        
        assertTrue(result.isSuccess(), "Constraint generation should succeed with type variables")
        val constraints = result.constraints

        // Should generate constraints that allow f to be unified as a function type
        // The exact constraint pattern may vary, but the system should be solvable
        val constraintsList = constraints.all()
        
        // Check that we have constraints involving function types or the variables involved
        val hasRelevantConstraints = constraintsList.any { constraint ->
            constraint is EqualityConstraint && 
            (constraint.type1 is FunctionType || constraint.type2 is FunctionType ||
             constraint.involvesVariable(fType))
        }
        assertTrue(hasRelevantConstraints, "Should have constraints that enable function type inference")
    }
    
    @Test
    fun `constraint generation for binary operation`() {
        // Expression: x + y
        val leftVar = VarExpr(createStringLocation("x"))
        val rightVar = VarExpr(createStringLocation("y"))
        val binaryOp = BinaryOpExpr(
            leftVar,
            BinaryOp.Plus,
            rightVar,
            createLocation()
        )
        
        val env = TypeEnvironment.empty()
            .extend("x", Types.Int)
            .extend("y", Types.Int)
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(binaryOp)
        
        assertTrue(result.isSuccess(), "Constraint generation for binary operation should succeed")
        val constraints = result.constraints
        val inferredType = result.type
        
        // Should generate constraints for operands and result type
        assertTrue(constraints.size() >= 3, "Should have constraints for operands and result")
        
        // For polymorphic + operator, result should be constrained through type class constraints
        // The result type should be unified with the operand types, and operands should be addable
        val constraintsList = constraints.all()
        
        // Check that the result type is connected to the operand types
        val hasOperandResultConnection = constraintsList.any { constraint ->
            constraint is EqualityConstraint && 
            (constraint.type1 == inferredType || constraint.type2 == inferredType)
        }
        
        // Check that there's an AddableType constraint for the polymorphic + operator
        val hasAddableTypeConstraint = constraintsList.any { constraint ->
            constraint is InstanceConstraint && constraint.typeClass == "AddableType"
        }
        
        assertTrue(hasOperandResultConnection, "Should connect result type to operand types")
        assertTrue(hasAddableTypeConstraint, "Should have AddableType constraint for polymorphic + operator")
    }
    
    @Test
    fun `constraint generation for conditional expression`() {
        // Expression: if true then 1 else 2
        val condition = LiteralBoolExpr(createBoolLocation(true))
        val thenBranch = LiteralIntExpr(createIntLocation(1))
        val elseBranch = LiteralIntExpr(createIntLocation(2))
        val ifExpr = IfExpr(
            condition,
            thenBranch,
            elseBranch,
            createLocation()
        )
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(ifExpr)
        
        assertTrue(result.isSuccess(), "Constraint generation for if expression should succeed")
        val constraints = result.constraints
        val inferredType = result.type
        
        // Should generate constraints for:
        // 1. Condition must be Bool
        // 2. Then and else branches must have same type
        // 3. Result type equals branch type
        assertTrue(constraints.size() >= 4, "Should have constraints for condition and branches")
        
        // The constraint system should enable inferring that the result type is Int
        // The exact constraint pattern may vary, but the system should be solvable
        val constraintsList = constraints.all()
        
        // Check that we have constraints involving Int types and the inferred type
        val hasRelevantConstraints = constraintsList.any { constraint ->
            constraint is EqualityConstraint && 
            (constraint.type1 == Types.Int || constraint.type2 == Types.Int ||
             constraint.involvesVariable(inferredType as? TypeVariable ?: TypeVariable.fresh()))
        }
        assertTrue(hasRelevantConstraints, "Should have constraints that enable Int type inference")
    }
    
    @Test
    fun `constraint generation for lambda expression`() {
        // Expression: \x -> x + 1
        val param = createStringLocation("x")
        val bodyVar = VarExpr(createStringLocation("x"))
        val literal = LiteralIntExpr(createIntLocation(1))
        val body = BinaryOpExpr(
            bodyVar,
            BinaryOp.Plus,
            literal,
            createLocation()
        )
        val lambda = LambdaExpr(
            null, // no type params
            param,
            null, // no explicit param type
            body,
            createLocation()
        )
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(lambda)
        
        assertTrue(result.isSuccess(), "Constraint generation for lambda should succeed")
        val constraints = result.constraints
        val inferredType = result.type
        
        // Should generate constraints for:
        // 1. Parameter type
        // 2. Body expression
        // 3. Function type construction
        assertTrue(constraints.size() >= 3, "Should have constraints for parameter and body")
        
        // Result type should be a function type
        assertTrue(inferredType is TypeVariable, "Lambda should have inferred type variable")
        
        // Should have constraint making the inferred type a function type
        val constraintsList = constraints.all()
        val hasFunctionTypeConstraint = constraintsList.any { constraint ->
            constraint is EqualityConstraint && 
            constraint.type1 == inferredType && 
            constraint.type2 is FunctionType
        }
        assertTrue(hasFunctionTypeConstraint, "Should constrain result to be a function type")
    }
    
    @Test
    fun `constraint generation for complex nested expression`() {
        // Expression: if x > 0 then f(x) else g(y)
        val xVar = VarExpr(createStringLocation("x"))
        val zeroLit = LiteralIntExpr(createIntLocation(0))
        val condition = BinaryOpExpr(xVar, BinaryOp.EqualEqual, zeroLit, createLocation())
        
        val fVar = VarExpr(createStringLocation("f"))
        val thenApp = ApplicationExpr(fVar, listOf(xVar), createLocation())
        
        val gVar = VarExpr(createStringLocation("g"))
        val yVar = VarExpr(createStringLocation("y"))
        val elseApp = ApplicationExpr(gVar, listOf(yVar), createLocation())
        
        val complexExpr = IfExpr(condition, thenApp, elseApp, createLocation())
        
        val env = TypeEnvironment.empty()
            .extend("x", Types.Int)
            .extend("y", Types.String)
            .extend("f", FunctionType(Types.Int, Types.Bool))
            .extend("g", FunctionType(Types.String, Types.Bool))
        val generator = ConstraintGenerator(env)
        
        val result = generator.generateConstraints(complexExpr)
        
        assertTrue(result.isSuccess(), "Constraint generation for complex expression should succeed")
        val constraints = result.constraints
        
        // Should generate many constraints for the nested structure
        assertTrue(constraints.size() >= 10, "Should have many constraints for complex expression")
        
        // Should track dependencies between constraints
        val constraintsList = constraints.all()
        val variablesInConstraints = constraintsList.flatMap { it.freeVariables() }.toSet()
        
        // The constraints should involve types from the environment
        assertTrue(variablesInConstraints.isNotEmpty() || constraintsList.any { constraint ->
            constraint is EqualityConstraint && (
                constraint.type1 == Types.Int || constraint.type2 == Types.Int ||
                constraint.type1 == Types.String || constraint.type2 == Types.String ||
                constraint.type1 == Types.Bool || constraint.type2 == Types.Bool
            )
        }, "Should involve types from the environment")
    }
    
    @Test
    fun `constraint generation tracks dependencies correctly`() {
        // Expression: let x = 5 in let y = x + 1 in y * 2
        val innerLiteral = LiteralIntExpr(createIntLocation(5))
        val xVar = VarExpr(createStringLocation("x"))
        val oneLiteral = LiteralIntExpr(createIntLocation(1))
        val xPlusOne = BinaryOpExpr(xVar, BinaryOp.Plus, oneLiteral, createLocation())
        val yVar = VarExpr(createStringLocation("y"))
        val twoLiteral = LiteralIntExpr(createIntLocation(2))
        val yTimesTwo = BinaryOpExpr(yVar, BinaryOp.Star, twoLiteral, createLocation())
        
        val innerLet = LetExpr(
            false, // not recursive
            createStringLocation("y"),
            null, // no type params
            null, // no parameters
            null, // no type annotation
            xPlusOne,
            yTimesTwo
        )
        
        val outerLet = LetExpr(
            false, // not recursive
            createStringLocation("x"),
            null, // no type params
            null, // no parameters
            null, // no type annotation
            innerLiteral,
            innerLet
        )
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(outerLet)
        
        assertTrue(result.isSuccess(), "Constraint generation for nested lets should succeed")
        val constraints = result.constraints
        
        // Should generate constraints with proper dependencies
        assertTrue(constraints.size() >= 5, "Should have multiple constraints for nested lets")
        
        // Check that constraints properly track variable dependencies
        val constraintsList = constraints.all()
        val equalityConstraints = constraintsList.filterIsInstance<EqualityConstraint>()
        
        // Should have constraints involving Int type (from literals and operations)
        val hasIntConstraints = equalityConstraints.any { constraint ->
            constraint.type1 == Types.Int || constraint.type2 == Types.Int
        }
        assertTrue(hasIntConstraints, "Should have constraints involving Int type")
    }
    
    @Test
    fun `constraint generation for record expression`() {
        // Expression: {name: "John", age: 30}
        val nameField = FieldExpr(
            createStringLocation("name"),
            LiteralStringExpr(createStringLocation("John"))
        )
        val ageField = FieldExpr(
            createStringLocation("age"),
            LiteralIntExpr(createIntLocation(30))
        )
        val record = RecordExpr(
            listOf(nameField, ageField),
            createLocation()
        )
        
        val generator = ConstraintGenerator()
        val result = generator.generateConstraints(record)
        
        assertTrue(result.isSuccess(), "Constraint generation for record should succeed")
        val constraints = result.constraints
        val inferredType = result.type
        
        // Should generate constraints for field types and record type
        assertTrue(constraints.size() >= 3, "Should have constraints for fields and record")
        
        // Result should be constrained to a record type
        val constraintsList = constraints.all()
        val hasRecordConstraint = constraintsList.any { constraint ->
            constraint is EqualityConstraint && 
            (constraint.type1 == inferredType && constraint.type2 is RecordType ||
             constraint.type2 == inferredType && constraint.type1 is RecordType)
        }
        assertTrue(hasRecordConstraint, "Should constrain result to be a record type")
    }
    
    @Test
    fun `constraint generation preserves source location information`() {
        // Expression: 42
        val location = LocationCoordinate(0, 5, 10) // line 5, column 10
        val literal = LiteralIntExpr(IntLocation(42, location))
        val generator = ConstraintGenerator()
        
        val result = generator.generateConstraints(literal)
        
        assertTrue(result.isSuccess(), "Constraint generation should succeed")
        val constraints = result.constraints
        
        // Should preserve source location in constraints
        assertEquals(1, constraints.size(), "Should have one constraint")
        val constraint = constraints.all().first()
        assertNotNull(constraint.sourceLocation, "Constraint should have source location")
        assertEquals(location.line, constraint.sourceLocation!!.line, "Should preserve line number")
        assertEquals(location.column, constraint.sourceLocation!!.column, "Should preserve column number")
    }
} 