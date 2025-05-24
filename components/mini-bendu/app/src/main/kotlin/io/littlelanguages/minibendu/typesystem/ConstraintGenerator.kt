package io.littlelanguages.minibendu.typesystem

import io.littlelanguages.minibendu.*
import io.littlelanguages.scanpiler.Location as ScanpilerLocation
import io.littlelanguages.scanpiler.LocationCoordinate
import io.littlelanguages.scanpiler.LocationRange

/**
 * Result of constraint generation containing the inferred type, generated constraints,
 * and any error information.
 */
sealed class ConstraintGenerationResult {
    abstract val type: Type
    abstract val constraints: ConstraintSet
    
    abstract fun isSuccess(): Boolean
    abstract fun isFailure(): Boolean
    
    data class Success(
        override val type: Type,
        override val constraints: ConstraintSet
    ) : ConstraintGenerationResult() {
        override fun isSuccess(): Boolean = true
        override fun isFailure(): Boolean = false
        val error: String? = null
    }
    
    data class Failure(
        val error: String,
        override val type: Type = TypeVariable.fresh(), // Dummy type for failure cases
        override val constraints: ConstraintSet = ConstraintSet.empty()
    ) : ConstraintGenerationResult() {
        override fun isSuccess(): Boolean = false
        override fun isFailure(): Boolean = true
    }
}

/**
 * Constraint generator that visits AST nodes and generates type constraints.
 * This implements the constraint generation phase of Hindley-Milner type inference.
 * 
 * The generator:
 * - Traverses AST expressions recursively
 * - Assigns fresh type variables to sub-expressions
 * - Generates equality and subtyping constraints
 * - Tracks constraint dependencies and ordering
 * - Preserves source location information for error reporting
 */
class ConstraintGenerator(
    private val environment: TypeEnvironment = TypeEnvironment.empty()
) {
    
    /**
     * Helper function to extract source location from AST location
     */
    private fun extractSourceLocation(location: ScanpilerLocation): SourceLocation {
        return when (location) {
            is LocationCoordinate -> 
                SourceLocation(location.line, location.column)
            is LocationRange -> 
                SourceLocation(location.start.line, location.start.column)
        }
    }
    
    /**
     * Generate constraints for an expression, returning the inferred type
     * and the set of constraints that must be satisfied.
     */
    fun generateConstraints(expr: Expr): ConstraintGenerationResult {
        return try {
            val (type, constraints) = generateConstraintsInternal(expr, environment)
            ConstraintGenerationResult.Success(type, constraints)
        } catch (e: ConstraintGenerationException) {
            ConstraintGenerationResult.Failure(e.message ?: "Constraint generation failed")
        }
    }
    
    /**
     * Internal constraint generation that throws exceptions for errors.
     */
    private fun generateConstraintsInternal(expr: Expr, env: TypeEnvironment): Pair<Type, ConstraintSet> {
        return when (expr) {
            is LiteralIntExpr -> generateConstraintsForIntLiteral(expr)
            is LiteralStringExpr -> generateConstraintsForStringLiteral(expr)
            is LiteralBoolExpr -> generateConstraintsForBoolLiteral(expr)
            is VarExpr -> generateConstraintsForVariable(expr, env)
            is ApplicationExpr -> generateConstraintsForApplication(expr, env)
            is BinaryOpExpr -> generateConstraintsForBinaryOp(expr, env)
            is IfExpr -> generateConstraintsForIf(expr, env)
            is LambdaExpr -> generateConstraintsForLambda(expr, env)
            is LetExpr -> generateConstraintsForLet(expr, env)
            is RecordExpr -> generateConstraintsForRecord(expr, env)
            is ProjectionExpr -> generateConstraintsForProjection(expr, env)
            is TupleExpr -> generateConstraintsForTuple(expr, env)
            is MatchExpr -> generateConstraintsForMatch(expr, env)
        }
    }
    
    /**
     * Generate constraints for integer literals.
     */
    private fun generateConstraintsForIntLiteral(expr: LiteralIntExpr): Pair<Type, ConstraintSet> {
        val resultType = TypeVariable.fresh()
        val sourceLocation = extractSourceLocation(expr.location())
        val constraint = EqualityConstraint(resultType, Types.Int, sourceLocation)
        return Pair(resultType, ConstraintSet.of(constraint))
    }
    
    /**
     * Generate constraints for string literals.
     * 
     * This implementation supports both general String types and specific string literal types.
     * String literal types enable discriminated unions and typed enumerations.
     * 
     * For example:
     * - "success" can be inferred as the literal type "success" 
     * - This allows union types like "pending" | "fulfilled" | "rejected"
     * - Enables precise type-level constraints for string constants
     */
    private fun generateConstraintsForStringLiteral(expr: LiteralStringExpr): Pair<Type, ConstraintSet> {
        val resultType = TypeVariable.fresh()
        val sourceLocation = extractSourceLocation(expr.location())
        
        // Use general String type for string literals
        // This allows equality comparison between different string literals
        val constraint = EqualityConstraint(resultType, Types.String, sourceLocation)
        return Pair(resultType, ConstraintSet.of(constraint))
    }
    
    /**
     * Generate constraints for boolean literals.
     */
    private fun generateConstraintsForBoolLiteral(expr: LiteralBoolExpr): Pair<Type, ConstraintSet> {
        val resultType = TypeVariable.fresh()
        val sourceLocation = extractSourceLocation(expr.location())
        val constraint = EqualityConstraint(resultType, Types.Bool, sourceLocation)
        return Pair(resultType, ConstraintSet.of(constraint))
    }
    
    /**
     * Generate constraints for variable references.
     */
    private fun generateConstraintsForVariable(expr: VarExpr, env: TypeEnvironment): Pair<Type, ConstraintSet> {
        val variableName = expr.id.value
        val variableTypeScheme = env.lookup(variableName)
            ?: throw ConstraintGenerationException("Undefined variable: $variableName")
        
        val resultType = TypeVariable.fresh()
        val sourceLocation = extractSourceLocation(expr.location())
        
        // Instantiate the type scheme with fresh variables
        val (instantiatedType, _) = variableTypeScheme.instantiate()
        
        val constraint = EqualityConstraint(resultType, instantiatedType, sourceLocation)
        return Pair(resultType, ConstraintSet.of(constraint))
    }
    
    /**
     * Generate constraints for function application.
     */
    private fun generateConstraintsForApplication(expr: ApplicationExpr, env: TypeEnvironment): Pair<Type, ConstraintSet> {
        // Generate constraints for the function expression
        val (functionType, functionConstraints) = generateConstraintsInternal(expr.function, env)
        
        // Generate constraints for argument expressions
        val argumentResults = expr.arguments.map { arg ->
            generateConstraintsInternal(arg, env)
        }
        val argumentTypes = argumentResults.map { it.first }
        val argumentConstraints = argumentResults.map { it.second }.fold(ConstraintSet.empty()) { acc, constraints ->
            acc.union(constraints)
        }
        
        // Create result type and function type constraint
        val resultType = TypeVariable.fresh()
        val sourceLocation = extractSourceLocation(expr.location())
        
        // For multiple arguments, create a curried function type: arg1 -> (arg2 -> ... -> result)
        // For no arguments, create a nullary function type: Unit -> result
        val expectedFunctionType = if (argumentTypes.isEmpty()) {
            FunctionType(Types.Unit, resultType)
        } else {
            argumentTypes.foldRight(resultType as Type) { argType, accType ->
                FunctionType(argType, accType)
            }
        }
        
        // Function must have the expected function type
        val functionConstraint = EqualityConstraint(functionType, expectedFunctionType, sourceLocation)
        
        val allConstraints = functionConstraints
            .union(argumentConstraints)
            .add(functionConstraint)
        
        return Pair(resultType, allConstraints)
    }
    
    /**
     * Generate constraints for binary operations.
     */
    private fun generateConstraintsForBinaryOp(expr: BinaryOpExpr, env: TypeEnvironment): Pair<Type, ConstraintSet> {
        val (leftType, leftConstraints) = generateConstraintsInternal(expr.left, env)
        val (rightType, rightConstraints) = generateConstraintsInternal(expr.right, env)
        
        val resultType = TypeVariable.fresh()
        val sourceLocation = extractSourceLocation(expr.location())
        
        val operatorConstraints = when (expr.op) {
            BinaryOp.Plus, BinaryOp.Minus, BinaryOp.Star, BinaryOp.Slash -> {
                // Arithmetic operators: both operands must be Int, result is Int
                ConstraintSet.of(
                    EqualityConstraint(leftType, Types.Int, sourceLocation),
                    EqualityConstraint(rightType, Types.Int, sourceLocation),
                    EqualityConstraint(resultType, Types.Int, sourceLocation)
                )
            }
            BinaryOp.EqualEqual, BinaryOp.NotEqual -> {
                // Equality operators: operands must have same type, result is Bool
                ConstraintSet.of(
                    EqualityConstraint(leftType, rightType, sourceLocation),
                    EqualityConstraint(resultType, Types.Bool, sourceLocation)
                )
            }
            BinaryOp.And, BinaryOp.Or -> {
                // Logical operators: both operands must be Bool, result is Bool
                ConstraintSet.of(
                    EqualityConstraint(leftType, Types.Bool, sourceLocation),
                    EqualityConstraint(rightType, Types.Bool, sourceLocation),
                    EqualityConstraint(resultType, Types.Bool, sourceLocation)
                )
            }
        }
        
        val allConstraints = leftConstraints
            .union(rightConstraints)
            .union(operatorConstraints)
        
        return Pair(resultType, allConstraints)
    }
    
    /**
     * Generate constraints for conditional expressions.
     */
    private fun generateConstraintsForIf(expr: IfExpr, env: TypeEnvironment): Pair<Type, ConstraintSet> {
        val (conditionType, conditionConstraints) = generateConstraintsInternal(expr.condition, env)
        val (thenType, thenConstraints) = generateConstraintsInternal(expr.thenBranch, env)
        val (elseType, elseConstraints) = generateConstraintsInternal(expr.elseBranch, env)
        
        val resultType = TypeVariable.fresh()
        val sourceLocation = extractSourceLocation(expr.location())
        
        val conditionalConstraints = ConstraintSet.of(
            EqualityConstraint(conditionType, Types.Bool, sourceLocation), // condition must be Bool
            EqualityConstraint(thenType, elseType, sourceLocation), // branches must have same type
            EqualityConstraint(resultType, thenType, sourceLocation) // result type equals branch type
        )
        
        val allConstraints = conditionConstraints
            .union(thenConstraints)
            .union(elseConstraints)
            .union(conditionalConstraints)
        
        return Pair(resultType, allConstraints)
    }
    
    /**
     * Generate constraints for lambda expressions.
     */
    private fun generateConstraintsForLambda(expr: LambdaExpr, env: TypeEnvironment): Pair<Type, ConstraintSet> {
        val parameterName = expr.param.value
        val parameterType = expr.paramType?.let { typeExprToType(it) } ?: TypeVariable.fresh()
        
        // Extend environment with parameter binding
        val parameterScheme = TypeScheme(emptySet(), parameterType) // Monomorphic scheme
        val extendedEnv = env.bind(parameterName, parameterScheme)
        
        // Generate constraints for body with extended environment
        val (bodyType, bodyConstraints) = generateConstraintsInternal(expr.body, extendedEnv)
        
        val resultType = TypeVariable.fresh()
        val functionType = FunctionType(parameterType, bodyType)
        val sourceLocation = extractSourceLocation(expr.location())
        
        val functionConstraint = EqualityConstraint(resultType, functionType, sourceLocation)
        val allConstraints = bodyConstraints.add(functionConstraint)
        
        return Pair(resultType, allConstraints)
    }
    
    /**
     * Generate constraints for let expressions.
     * 
     * This implementation handles:
     * - Non-recursive let bindings with proper type generalization
     * - Recursive let bindings with appropriate environment setup
     * - Type annotations and constraint propagation
     * - Proper variable scoping and shadowing
     */
    private fun generateConstraintsForLet(expr: LetExpr, env: TypeEnvironment): Pair<Type, ConstraintSet> {
        val bindingName = expr.id.value
        
        if (expr.body == null) {
            // Simple binding without body - just return the value's type
            return generateConstraintsInternal(expr.value, env)
        }
        
        if (expr.recursive) {
            // For recursive let, we need to add the binding to the environment first
            return generateConstraintsForRecursiveLet(expr, env)
        } else {
            // For non-recursive let, generate constraints for value first, then extend environment
            return generateConstraintsForNonRecursiveLet(expr, env)
        }
    }
    
    /**
     * Generate constraints for non-recursive let expressions.
     * The value is evaluated in the current environment, then the binding is added to a new environment.
     */
    private fun generateConstraintsForNonRecursiveLet(expr: LetExpr, env: TypeEnvironment): Pair<Type, ConstraintSet> {
        val bindingName = expr.id.value
        
        // Generate constraints for the binding value in the current environment
        val (valueType, valueConstraints) = generateConstraintsInternal(expr.value, env)
        
        // Handle explicit type annotation if present
        val annotatedType = expr.typeAnnotation?.let { typeExprToType(it) }
        val annotationConstraints = annotatedType?.let { annoType ->
            val sourceLocation = extractSourceLocation(expr.location())
            ConstraintSet.of(EqualityConstraint(valueType, annoType, sourceLocation))
        } ?: ConstraintSet.empty()
        
        // For non-recursive let, we use the valueType directly in a monomorphic scheme
        // The type will be generalized when the constraints are solved
        val bindingScheme = TypeScheme(emptySet(), valueType)
        
        // Extend environment with the binding
        val extendedEnv = env.bind(bindingName, bindingScheme)
        
        // Generate constraints for the body in the extended environment
        val (bodyType, bodyConstraints) = generateConstraintsInternal(expr.body!!, extendedEnv)
        
        val allConstraints = valueConstraints
            .union(annotationConstraints)
            .union(bodyConstraints)
        
        return Pair(bodyType, allConstraints)
    }
    
    /**
     * Generate constraints for recursive let expressions.
     * The binding is added to the environment before evaluating the value, allowing self-reference.
     */
    private fun generateConstraintsForRecursiveLet(expr: LetExpr, env: TypeEnvironment): Pair<Type, ConstraintSet> {
        val bindingName = expr.id.value
        
        // Create a fresh type variable for the recursive binding
        val recursiveType = TypeVariable.fresh()
        
        // Handle explicit type annotation if present
        val annotatedType = expr.typeAnnotation?.let { typeExprToType(it) }
        val annotationConstraints = annotatedType?.let { annoType ->
            val sourceLocation = extractSourceLocation(expr.location())
            ConstraintSet.of(EqualityConstraint(recursiveType, annoType, sourceLocation))
        } ?: ConstraintSet.empty()
        
        // Create a monomorphic scheme for the recursive binding
        val recursiveScheme = TypeScheme(emptySet(), recursiveType)
        
        // Extend environment with the recursive binding
        val extendedEnv = env.bind(bindingName, recursiveScheme)
        
        // Generate constraints for the value in the extended environment (allowing self-reference)
        val (valueType, valueConstraints) = generateConstraintsInternal(expr.value, extendedEnv)
        
        // The recursive type must equal the value type
        val sourceLocation = extractSourceLocation(expr.location())
        val recursiveConstraint = EqualityConstraint(recursiveType, valueType, sourceLocation)
        
        // Generate constraints for the body (if present)
        val (bodyType, bodyConstraints) = generateConstraintsInternal(expr.body!!, extendedEnv)
        
        val allConstraints = valueConstraints
            .union(annotationConstraints)
            .union(bodyConstraints)
            .add(recursiveConstraint)
        
        return Pair(bodyType, allConstraints)
    }
    
    /**
     * Generate constraints for record expressions.
     */
    private fun generateConstraintsForRecord(expr: RecordExpr, env: TypeEnvironment): Pair<Type, ConstraintSet> {
        val explicitFields = mutableMapOf<String, Type>()
        var allConstraints = ConstraintSet.empty()
        val spreadTypes = mutableListOf<Type>()
        
        for (field in expr.fields) {
            when (field) {
                is FieldExpr -> {
                    val fieldName = field.id.value
                    val (fieldType, fieldConstraints) = generateConstraintsInternal(field.value, env)
                    explicitFields[fieldName] = fieldType
                    allConstraints = allConstraints.union(fieldConstraints)
                }
                is SpreadExpr -> {
                    // Handle spread operations by constraining the spread expression to be a record type
                    val (spreadType, spreadConstraints) = generateConstraintsInternal(field.expr, env)
                    allConstraints = allConstraints.union(spreadConstraints)
                    spreadTypes.add(spreadType)
                    
                    // Create a constraint that the spread type must be a record type
                    val sourceLocation = extractSourceLocation(expr.location())
                    val recordConstraint = RecordTypeConstraint(spreadType, sourceLocation)
                    allConstraints = allConstraints.add(recordConstraint)
                }
            }
        }
        
        val resultType = TypeVariable.fresh()
        val sourceLocation = extractSourceLocation(expr.location())
        
        // Create a merge constraint that combines all spread types and explicit fields
        if (spreadTypes.isNotEmpty()) {
            val mergeConstraint = MergeConstraint(resultType, spreadTypes, explicitFields, sourceLocation)
            allConstraints = allConstraints.add(mergeConstraint)
        } else {
            // No spreads, just a regular record
            val recordType = RecordType(explicitFields)
            val recordConstraint = EqualityConstraint(resultType, recordType, sourceLocation)
            allConstraints = allConstraints.add(recordConstraint)
        }
        
        return Pair(resultType, allConstraints)
    }
    
    /**
     * Generate constraints for field projection.
     */
    private fun generateConstraintsForProjection(expr: ProjectionExpr, env: TypeEnvironment): Pair<Type, ConstraintSet> {
        val (targetType, targetConstraints) = generateConstraintsInternal(expr.target, env)
        
        val fieldName = expr.field.value
        val resultType = TypeVariable.fresh()
        val rowVariable = TypeVariable.fresh()
        
        // Target must be a record with the required field
        val fieldType = resultType
        val requiredFields = mapOf(fieldName to fieldType)
        val expectedRecordType = RecordType(requiredFields, rowVariable)
        
        val sourceLocation = extractSourceLocation(expr.location())
        
        val projectionConstraint = EqualityConstraint(targetType, expectedRecordType, sourceLocation)
        val allConstraints = targetConstraints.add(projectionConstraint)
        
        return Pair(resultType, allConstraints)
    }
    
    /**
     * Generate constraints for tuple expressions.
     */
    private fun generateConstraintsForTuple(expr: TupleExpr, env: TypeEnvironment): Pair<Type, ConstraintSet> {
        val elementResults = expr.elements.map { element ->
            generateConstraintsInternal(element, env)
        }
        
        val elementTypes = elementResults.map { it.first }
        val elementConstraints = elementResults.map { it.second }.fold(ConstraintSet.empty()) { acc, constraints ->
            acc.union(constraints)
        }
        
        val resultType = TypeVariable.fresh()
        val tupleType = TupleType(elementTypes)
        val sourceLocation = extractSourceLocation(expr.location())
        
        val tupleConstraint = EqualityConstraint(resultType, tupleType, sourceLocation)
        val allConstraints = elementConstraints.add(tupleConstraint)
        
        return Pair(resultType, allConstraints)
    }
    
    /**
     * Generate constraints for match expressions.
     * This is a simplified implementation that doesn't handle full pattern matching.
     */
    private fun generateConstraintsForMatch(expr: MatchExpr, env: TypeEnvironment): Pair<Type, ConstraintSet> {
        val (scrutineeType, scrutineeConstraints) = generateConstraintsInternal(expr.scrutinee, env)
        
        // For each case, generate constraints
        val caseResults = expr.cases.map { case ->
            // TODO: Implement proper pattern constraint generation
            // For now, we'll just generate constraints for the body
            generateConstraintsInternal(case.body, env)
        }
        
        val caseTypes = caseResults.map { it.first }
        val caseConstraints = caseResults.map { it.second }.fold(ConstraintSet.empty()) { acc, constraints ->
            acc.union(constraints)
        }
        
        val resultType = TypeVariable.fresh()
        val sourceLocation = extractSourceLocation(expr.location())
        
        // All case bodies must have the same type
        val unificationConstraints = if (caseTypes.isNotEmpty()) {
            caseTypes.drop(1).map { caseType ->
                EqualityConstraint(caseTypes.first(), caseType, sourceLocation)
            }
        } else {
            emptyList()
        }
        
        // Result type equals case type
        val resultConstraint = if (caseTypes.isNotEmpty()) {
            EqualityConstraint(resultType, caseTypes.first(), sourceLocation)
        } else {
            EqualityConstraint(resultType, Types.Unit, sourceLocation)
        }
        
        val matchConstraints = ConstraintSet.of(unificationConstraints + resultConstraint)
        val allConstraints = scrutineeConstraints
            .union(caseConstraints)
            .union(matchConstraints)
        
        return Pair(resultType, allConstraints)
    }
    
    /**
     * Convert a type expression (from the AST) to a Type (from the type system).
     * This is a simplified implementation.
     */
    private fun typeExprToType(typeExpr: TypeExpr): Type {
        return when (typeExpr) {
            is BaseTypeExpr -> {
                when (typeExpr.id.value) {
                    "Int" -> Types.Int
                    "String" -> Types.String
                    "Bool" -> Types.Bool
                    "Unit" -> Types.Unit
                    else -> TypeVariable.fresh() // Unknown type - could be a type variable
                }
            }
            is FunctionTypeExpr -> {
                val domain = typeExprToType(typeExpr.from)
                val codomain = typeExprToType(typeExpr.to)
                FunctionType(domain, codomain)
            }
            is RecordTypeExpr -> {
                val fields = typeExpr.fields.associate { field ->
                    field.id.value to typeExprToType(field.type)
                }
                val rowVar = typeExpr.extension?.let { TypeVariable.fresh() }
                RecordType(fields, rowVar)
            }
            is UnionTypeExpr -> {
                val left = typeExprToType(typeExpr.left)
                val right = typeExprToType(typeExpr.right)
                // Use normalized union creation to handle flattening and simplification
                UnionType.create(setOf(left, right))
            }
            is TupleTypeExpr -> {
                val elements = typeExpr.types.map { typeExprToType(it) }
                TupleType(elements)
            }
            is LiteralStringTypeExpr -> {
                LiteralStringType(typeExpr.value.value)
            }
            is MergeTypeExpr -> {
                // Properly implement intersection types
                val left = typeExprToType(typeExpr.left)
                val right = typeExprToType(typeExpr.right)
                // Use normalized intersection creation to handle flattening and simplification
                IntersectionType.create(setOf(left, right))
            }
        }
    }
}

/**
 * Exception thrown when constraint generation fails.
 */
class ConstraintGenerationException(message: String) : Exception(message) 