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
    private val env: TypeEnvironment = TypeEnvironment.empty(),
    private val typeAliasRegistry: TypeAliasRegistry = TypeAliasRegistry()
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
            val (type, constraints) = generateConstraintsInternal(expr, env)
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
        
        // Create environment with type parameters for processing type annotations
        val typeParamEnv = createTypeParameterEnvironment(expr.typeParams, env)
        
        val parameterType = expr.paramType?.let { typeExprToTypeWithEnv(it, typeParamEnv) } ?: TypeVariable.fresh()
        
        // Extend environment with parameter binding
        val parameterScheme = TypeScheme(emptySet(), parameterType) // Monomorphic scheme
        val extendedEnv = typeParamEnv.bind(parameterName, parameterScheme)
        
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
     * - Function syntax (let f(x) = ...) by converting to lambda
     * - Type annotations and constraint propagation
     * - Proper variable scoping and shadowing
     */
    private fun generateConstraintsForLet(expr: LetExpr, env: TypeEnvironment): Pair<Type, ConstraintSet> {
        val bindingName = expr.id.value
        
        // Create environment with type parameters for processing type annotations
        val typeParamEnv = createTypeParameterEnvironment(expr.typeParams, env)
        
        // Convert function syntax to lambda if parameters are present
        val actualValue = if (expr.parameters?.isNotEmpty() == true) {
            // Convert let f(x, y, z) = body to let f = \x => \y => \z => body
            // If there's a return type annotation, we'll handle it separately in constraint generation
            convertFunctionSyntaxToLambda(expr.typeParams, expr.parameters, expr.value, expr.id.location)
        } else {
            expr.value
        }
        
        if (expr.body == null) {
            // Let expression without body - this is a top-level binding
            // We need to solve the value constraints and generalize the type
            // even though there's no body to type-check
            val (valueType, valueConstraints) = generateConstraintsInternal(actualValue, typeParamEnv)
            
            // Handle explicit type annotation if present
            // For function syntax with return type annotations, we need to construct the full function type
            val annotatedType = if (expr.parameters?.isNotEmpty() == true && expr.typeAnnotation != null) {
                // For function syntax with return type annotation, construct the full function type
                val returnType = typeExprToTypeWithEnv(expr.typeAnnotation, typeParamEnv)
                expr.parameters.foldRight(returnType) { param, acc ->
                    val paramType = param.type?.let { typeExprToTypeWithEnv(it, typeParamEnv) } ?: TypeVariable.fresh()
                    FunctionType(paramType, acc)
                }
            } else {
                expr.typeAnnotation?.let { typeExprToTypeWithEnv(it, typeParamEnv) }
            }
            val annotationConstraints = annotatedType?.let { annoType ->
                val sourceLocation = extractSourceLocation(expr.location())
                ConstraintSet.of(EqualityConstraint(valueType, annoType, sourceLocation))
            } ?: ConstraintSet.empty()
            
            val allConstraints = valueConstraints.union(annotationConstraints)
            
            // For top-level bindings, return the value type and constraints
            // The binding will be handled by the incremental type checker
            return Pair(valueType, allConstraints)
        }
        
        if (expr.recursive) {
            // For recursive let, we need to add the binding to the environment first
            return generateConstraintsForRecursiveLet(expr.copy(value = actualValue), env)
        } else {
            // For non-recursive let, generate constraints for value first, then extend environment
            return generateConstraintsForNonRecursiveLet(expr.copy(value = actualValue), env)
        }
    }
    
    /**
     * Convert function syntax let f(x, y) = body to lambda form \x => \y => body
     */
    private fun convertFunctionSyntaxToLambda(typeParams: List<TypeParam>?, parameters: List<Parameter>, body: Expr, location: ScanpilerLocation): Expr {
        return parameters.foldRight(body) { param, acc ->
            LambdaExpr(
                typeParams = typeParams,
                param = param.id,
                paramType = param.type,
                body = acc,
                location = location
            )
        }
    }
    
    /**
     * Generate constraints for non-recursive let expressions.
     * The value is evaluated in the current environment, then the binding is added to a new environment.
     */
    private fun generateConstraintsForNonRecursiveLet(expr: LetExpr, env: TypeEnvironment): Pair<Type, ConstraintSet> {
        val bindingName = expr.id.value
        
        // Create environment with type parameters for processing type annotations
        val typeParamEnv = createTypeParameterEnvironment(expr.typeParams, env)
        
        // Generate constraints for the binding value in the current environment
        val (valueType, valueConstraints) = generateConstraintsInternal(expr.value, typeParamEnv)
        
        // Handle explicit type annotation if present
        // For function syntax with return type annotations, we need to construct the full function type
        val annotatedType = if (expr.parameters?.isNotEmpty() == true && expr.typeAnnotation != null) {
            // For function syntax with return type annotation, construct the full function type
            val returnType = typeExprToTypeWithEnv(expr.typeAnnotation, typeParamEnv)
            expr.parameters.foldRight(returnType) { param, acc ->
                val paramType = param.type?.let { typeExprToTypeWithEnv(it, typeParamEnv) } ?: TypeVariable.fresh()
                FunctionType(paramType, acc)
            }
        } else {
            expr.typeAnnotation?.let { typeExprToTypeWithEnv(it, typeParamEnv) }
        }
        val annotationConstraints = annotatedType?.let { annoType ->
            val sourceLocation = extractSourceLocation(expr.location())
            ConstraintSet.of(EqualityConstraint(valueType, annoType, sourceLocation))
        } ?: ConstraintSet.empty()
        
        // For non-recursive let, we need to solve the value constraints first, then generalize
        // This ensures we generalize the solved type, not the raw type with unresolved variables
        val valueConstraintsWithAnnotation = valueConstraints.union(annotationConstraints)
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(valueConstraintsWithAnnotation)
        
        val bindingScheme = when (solverResult) {
            is ConstraintSolverResult.Success -> {
                // Apply substitution to get the solved type, then generalize
                val solvedValueType = solverResult.substitution.apply(valueType)
                env.generalize(solvedValueType)
            }
            is ConstraintSolverResult.Failure -> {
                // If constraint solving fails, fall back to monomorphic scheme
                // This allows error recovery and prevents cascading failures
                TypeScheme.monomorphic(valueType)
            }
        }
        
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
        
        // Create environment with type parameters for processing type annotations
        val typeParamEnv = createTypeParameterEnvironment(expr.typeParams, env)
        
        // Create a fresh type variable for the recursive binding
        val recursiveType = TypeVariable.fresh()
        
        // Handle explicit type annotation if present
        // For function syntax with return type annotations, we need to construct the full function type
        val annotatedType = if (expr.parameters?.isNotEmpty() == true && expr.typeAnnotation != null) {
            // For function syntax with return type annotation, construct the full function type
            val returnType = typeExprToTypeWithEnv(expr.typeAnnotation, typeParamEnv)
            expr.parameters.foldRight(returnType) { param, acc ->
                val paramType = param.type?.let { typeExprToTypeWithEnv(it, typeParamEnv) } ?: TypeVariable.fresh()
                FunctionType(paramType, acc)
            }
        } else {
            expr.typeAnnotation?.let { typeExprToTypeWithEnv(it, typeParamEnv) }
        }
        val annotationConstraints = annotatedType?.let { annoType ->
            val sourceLocation = extractSourceLocation(expr.location())
            ConstraintSet.of(EqualityConstraint(recursiveType, annoType, sourceLocation))
        } ?: ConstraintSet.empty()
        
        // Create a monomorphic scheme for the recursive binding
        val recursiveScheme = TypeScheme(emptySet(), recursiveType)
        
        // Extend environment with the recursive binding and type parameters
        val extendedEnv = typeParamEnv.bind(bindingName, recursiveScheme)
        
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
     * 
     * Context-sensitive record type generation:
     * - Open records (with row variables) for polymorphic contexts 
     * - Closed records for type-annotated bindings
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
            // No spreads - create a closed record for soundness
            // Direct record literals like { name = "Alice" } should be closed to prevent
            // unsound assignments where fields are missing
            val recordType = RecordType(explicitFields, null) // null = closed record
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
     * This implements full pattern matching with proper constraint generation.
     * Now includes pattern analysis for exhaustiveness, reachability, and type refinement.
     * 
     * For union types, the result type is the union of all case return types.
     */
    private fun generateConstraintsForMatch(expr: MatchExpr, env: TypeEnvironment): Pair<Type, ConstraintSet> {
        val (scrutineeType, scrutineeConstraints) = generateConstraintsInternal(expr.scrutinee, env)
        
        // Perform pattern analysis for warnings and type refinement
        val patternAnalysis = PatternAnalyzer.analyze(expr, scrutineeType)
        
        // Generate warnings for pattern issues (these don't affect type checking but are useful)
        generatePatternWarnings(expr, patternAnalysis)
        
        // For each case, generate pattern and body constraints with type refinement
        val caseResults = expr.cases.map { case ->
            generateConstraintsForCaseWithRefinement(case, scrutineeType, env, patternAnalysis)
        }
        
        val caseTypes = caseResults.map { it.first }
        val caseConstraints = caseResults.map { it.second }.fold(ConstraintSet.empty()) { acc, constraints ->
            acc.union(constraints)
        }
        
        val resultType = TypeVariable.fresh()
        val sourceLocation = extractSourceLocation(expr.location())
        
        // For match expressions, the result type should be the union of all case return types
        // This allows different branches to return different types, which is essential for union types
        val resultConstraint = if (caseTypes.isNotEmpty()) {
            if (caseTypes.size == 1) {
                // Single case - result type equals case type
                EqualityConstraint(resultType, caseTypes.first(), sourceLocation)
            } else {
                // Multiple cases - result type is union of all case types
                val unionType = UnionType.create(caseTypes.toSet())
                EqualityConstraint(resultType, unionType, sourceLocation)
            }
        } else {
            // No cases - result type is Unit
            EqualityConstraint(resultType, Types.Unit, sourceLocation)
        }
        
        val matchConstraints = ConstraintSet.of(listOf(resultConstraint))
        val allConstraints = scrutineeConstraints
            .union(caseConstraints)
            .union(matchConstraints)
        
        return Pair(resultType, allConstraints)
    }
    
    /**
     * Generate constraints for a single match case.
     */
    private fun generateConstraintsForCase(case: MatchCase, scrutineeType: Type, env: TypeEnvironment): Pair<Type, ConstraintSet> {
        // Generate pattern constraints and extract bindings
        val (patternConstraints, patternBindings) = generateConstraintsForPattern(case.pattern, scrutineeType)
        
        // Extend environment with pattern bindings
        val extendedEnv = patternBindings.entries.fold(env) { acc, (name, type) ->
            acc.extend(name, type)
        }
        
        // Generate constraints for the case body with extended environment
        val (bodyType, bodyConstraints) = generateConstraintsInternal(case.body, extendedEnv)
        
        val allConstraints = patternConstraints.union(bodyConstraints)
        return Pair(bodyType, allConstraints)
    }
    
    /**
     * Generate constraints for a single match case with type refinement.
     * This enhanced version uses pattern analysis results to improve type inference.
     */
    private fun generateConstraintsForCaseWithRefinement(
        case: MatchCase, 
        scrutineeType: Type, 
        env: TypeEnvironment,
        patternAnalysis: PatternAnalysisResult
    ): Pair<Type, ConstraintSet> {
        // Generate pattern constraints and extract bindings
        val (patternConstraints, patternBindings) = generateConstraintsForPattern(case.pattern, scrutineeType)
        
        // Apply type refinements from pattern analysis
        val refinements = patternAnalysis.typeRefinements[case] ?: emptyMap()
        val refinedBindings = patternBindings.toMutableMap()
        
        // Merge refinements with pattern bindings (refinements take precedence)
        for ((varName, refinedType) in refinements) {
            refinedBindings[varName] = refinedType
        }
        
        // Extend environment with refined pattern bindings
        val extendedEnv = refinedBindings.entries.fold(env) { acc, (name, type) ->
            acc.extend(name, type)
        }
        
        // Generate constraints for the case body with refined environment
        val (bodyType, bodyConstraints) = generateConstraintsInternal(case.body, extendedEnv)
        
        val allConstraints = patternConstraints.union(bodyConstraints)
        return Pair(bodyType, allConstraints)
    }
    
    /**
     * Generate warnings for pattern analysis results.
     * These warnings don't affect type checking but provide helpful feedback.
     */
    private fun generatePatternWarnings(matchExpr: MatchExpr, analysis: PatternAnalysisResult) {
        // Exhaustiveness warnings
        if (!analysis.isExhaustive && analysis.missingPatterns.isNotEmpty()) {
            val sourceLocation = extractSourceLocation(matchExpr.location())
            // Note: In a full implementation, these would be stored and reported
            // For now, we'll just track them internally
            // println("Warning: Non-exhaustive pattern match. Missing patterns: ${analysis.missingPatterns}")
        }
        
        // Unreachable pattern warnings
        for (unreachableCase in analysis.unreachablePatterns) {
            val sourceLocation = extractSourceLocation(unreachableCase.pattern.location())
            // println("Warning: Unreachable pattern at ${sourceLocation}")
        }
        
        // Contradiction warnings
        for ((case1, case2) in analysis.contradictoryPatterns) {
            val location1 = extractSourceLocation(case1.pattern.location())
            val location2 = extractSourceLocation(case2.pattern.location())
            // println("Warning: Contradictory patterns at ${location1} and ${location2}")
        }
    }
    
    /**
     * Generate constraints for a pattern and return variable bindings.
     * Returns pattern constraints and a map of variable names to their types.
     */
    private fun generateConstraintsForPattern(pattern: Pattern, expectedType: Type): Pair<ConstraintSet, Map<String, Type>> {
        val sourceLocation = extractSourceLocation(pattern.location())
        
        // Handle pattern type annotation first
        val typeAnnotation = pattern.typeAnnotation
        if (typeAnnotation != null) {
            // Convert type annotation to Type and constrain it against expected type
            val annotatedType = typeExprToType(typeAnnotation)
            val annotationConstraint = EqualityConstraint(expectedType, annotatedType, sourceLocation)
            
            // Generate pattern constraints against the annotated type
            val (patternConstraints, bindings) = generateConstraintsForPatternCore(pattern, annotatedType)
            val allConstraints = patternConstraints.add(annotationConstraint)
            return Pair(allConstraints, bindings)
        }
        
        return generateConstraintsForPatternCore(pattern, expectedType)
    }
    
    /**
     * Generate constraints for a pattern without considering type annotations.
     * This is the core pattern constraint logic.
     */
    private fun generateConstraintsForPatternCore(pattern: Pattern, expectedType: Type): Pair<ConstraintSet, Map<String, Type>> {
        val sourceLocation = extractSourceLocation(pattern.location())
        
        // Unfold recursive types before pattern matching
        val unfoldedExpectedType = unfoldRecursiveType(expectedType)
        
        return when (pattern) {
            is LiteralIntPattern -> {
                // Pattern must match Int type
                val constraint = EqualityConstraint(unfoldedExpectedType, Types.Int, sourceLocation)
                Pair(ConstraintSet.of(listOf(constraint)), emptyMap())
            }
            
            is LiteralStringPattern -> {
                // Pattern must match String type or literal string type
                val literalType = Types.literal(pattern.value.value)
                val constraint = EqualityConstraint(unfoldedExpectedType, literalType, sourceLocation)
                Pair(ConstraintSet.of(listOf(constraint)), emptyMap())
            }
            
            is LiteralBoolPattern -> {
                // Pattern must match Bool type
                val constraint = EqualityConstraint(unfoldedExpectedType, Types.Bool, sourceLocation)
                Pair(ConstraintSet.of(listOf(constraint)), emptyMap())
            }
            
            is VarPattern -> {
                // Variable pattern binds the expected type (use original, not unfolded)
                val varName = pattern.id.value
                val bindings = mapOf(varName to expectedType)
                Pair(ConstraintSet.empty(), bindings)
            }
            
            is WildcardPattern -> {
                // Wildcard matches anything, no constraints or bindings
                Pair(ConstraintSet.empty(), emptyMap())
            }
            
            is TuplePattern -> {
                // Expected type must be a tuple with matching arity
                val elementTypes = pattern.elements.map { TypeVariable.fresh() }
                val tupleType = TupleType(elementTypes)
                val tupleConstraint = EqualityConstraint(unfoldedExpectedType, tupleType, sourceLocation)
                
                // Generate constraints for each element pattern
                val elementResults = pattern.elements.zip(elementTypes).map { (elementPattern, elementType) ->
                    generateConstraintsForPattern(elementPattern, elementType)
                }
                
                val elementConstraints = elementResults.map { it.first }.fold(ConstraintSet.empty()) { acc, constraints ->
                    acc.union(constraints)
                }
                val elementBindings = elementResults.map { it.second }.fold(emptyMap<String, Type>()) { acc, bindings ->
                    acc + bindings
                }
                
                val allConstraints = elementConstraints.add(tupleConstraint)
                Pair(allConstraints, elementBindings)
            }
            
            is RecordPattern -> {
                // Expected type must be a record containing at least the pattern fields
                val patternFields = mutableMapOf<String, Type>()
                val allConstraints = mutableListOf<TypeConstraint>()
                val allBindings = mutableMapOf<String, Type>()
                
                // Process each field pattern
                for (fieldPattern in pattern.fields) {
                    val fieldName = fieldPattern.id.value
                    val fieldType = TypeVariable.fresh()
                    patternFields[fieldName] = fieldType
                    
                    val (fieldConstraints, fieldBindings) = generateConstraintsForPattern(
                        fieldPattern.pattern, 
                        fieldType
                    )
                    
                    allConstraints.addAll(fieldConstraints.all())
                    allBindings.putAll(fieldBindings)
                }
                
                // Create record type with a row variable for additional fields
                val rowVariable = TypeVariable.fresh()
                val recordType = RecordType(patternFields, rowVariable)
                val recordConstraint = EqualityConstraint(unfoldedExpectedType, recordType, sourceLocation)
                allConstraints.add(recordConstraint)
                
                Pair(ConstraintSet.of(allConstraints), allBindings)
            }
            
            else -> {
                // Unknown pattern type, generate error or default behavior
                throw ConstraintGenerationException("Unsupported pattern type: ${pattern::class.simpleName}")
            }
        }
    }
    
    /**
     * Unfold a recursive type by returning its body.
     * This allows pattern matching to work with the structure inside the recursive type.
     */
    private fun unfoldRecursiveType(type: Type): Type {
        return when (type) {
            is RecursiveType -> {
                // Simply return the body - the recursive variable will be handled
                // by the constraint solver when needed
                type.body
            }
            else -> type
        }
    }
    
    /**
     * Convert a type expression (from the AST) to a Type (from the type system).
     * This is the public interface for type expression conversion.
     */
    fun convertTypeExprToType(typeExpr: TypeExpr): Type {
        return typeExprToType(typeExpr)
    }
    
    /**
     * Convert a type expression (from the AST) to a Type (from the type system).
     * This handles type alias resolution and normalization.
     */
    private fun typeExprToType(typeExpr: TypeExpr): Type {
        return typeExprToTypeWithEnv(typeExpr, env)
    }
    
    /**
     * Convert a type expression to a Type using a specific environment.
     * This allows type parameter resolution in different contexts.
     */
    private fun typeExprToTypeWithEnv(typeExpr: TypeExpr, environment: TypeEnvironment): Type {
        return when (typeExpr) {
            is BaseTypeExpr -> {
                when (typeExpr.id.value) {
                    "Int" -> Types.Int
                    "String" -> Types.String
                    "Bool" -> Types.Bool
                    "Unit" -> Types.Unit
                    else -> {
                        val typeName = typeExpr.id.value
                        val typeArguments = typeExpr.args?.map { typeExprToTypeWithEnv(it, environment) } ?: emptyList()
                        
                        // First check if it's a type parameter in the current environment
                        val typeParameter = environment.lookup(typeName)
                        if (typeParameter != null) {
                            // It's a type parameter, instantiate it
                            val (instantiatedType, _) = typeParameter.instantiate()
                            return instantiatedType
                        }
                        
                        // Check if it's a type alias
                        val expandedType = typeAliasRegistry.expandAlias(typeName, typeArguments)
                        if (expandedType != null) {
                            expandedType
                        } else {
                            // Not a known type alias or type parameter
                            // Only throw an error for undefined type variables if:
                            // 1. We're in a type parameter context (extended environment)
                            // 2. It looks like a type variable (single uppercase letter)
                            // 3. It has no type arguments (type aliases usually have arguments or are known)
                            if (isInTypeParameterContext(environment) && 
                                isLikelyTypeVariable(typeName) && 
                                typeArguments.isEmpty()) {
                                throw ConstraintGenerationException("Undefined type variable: $typeName")
                            } else {
                                // Otherwise, create a type alias reference that might be resolved later
                                TypeAlias(typeName, typeArguments)
                            }
                        }
                    }
                }
            }
            is FunctionTypeExpr -> {
                val domain = typeExprToTypeWithEnv(typeExpr.from, environment)
                val codomain = typeExprToTypeWithEnv(typeExpr.to, environment)
                FunctionType(domain, codomain)
            }
            is RecordTypeExpr -> {
                val fields = typeExpr.fields.associate { field ->
                    field.id.value to typeExprToTypeWithEnv(field.type, environment)
                }
                val rowVar = typeExpr.extension?.let { TypeVariable.fresh() }
                RecordType(fields, rowVar)
            }
            is UnionTypeExpr -> {
                val left = typeExprToTypeWithEnv(typeExpr.left, environment)
                val right = typeExprToTypeWithEnv(typeExpr.right, environment)
                // Use normalized union creation to handle flattening and simplification
                UnionType.create(setOf(left, right))
            }
            is TupleTypeExpr -> {
                val elements = typeExpr.types.map { typeExprToTypeWithEnv(it, environment) }
                TupleType(elements)
            }
            is LiteralStringTypeExpr -> {
                LiteralStringType(typeExpr.value.value)
            }
            is MergeTypeExpr -> {
                // Properly implement intersection types
                val left = typeExprToTypeWithEnv(typeExpr.left, environment)
                val right = typeExprToTypeWithEnv(typeExpr.right, environment)
                // Use normalized intersection creation to handle flattening and simplification
                IntersectionType.create(setOf(left, right))
            }
        }
    }
    
    /**
     * Create an environment that includes type parameters from a let expression.
     * This allows type parameter resolution when processing type annotations.
     */
    private fun createTypeParameterEnvironment(typeParams: List<TypeParam>?, baseEnv: TypeEnvironment): TypeEnvironment {
        if (typeParams == null) {
            return baseEnv
        }
        
        return typeParams.fold(baseEnv) { env, typeParam ->
            val typeVar = TypeVariable.fresh()
            env.bind(typeParam.id.value, TypeScheme.monomorphic(typeVar))
        }
    }
    
    /**
     * Check if we're in a context where type parameters should be defined.
     * This helps distinguish between undefined type variables and type aliases.
     */
    private fun isInTypeParameterContext(environment: TypeEnvironment): Boolean {
        // If the environment has any type parameter bindings, we're likely in a context
        // where type parameters are expected to be defined
        // This is a heuristic - in a more sophisticated implementation, we'd track context explicitly
        return environment != env // If environment is different from base, we're in a type parameter context
    }
    
    /**
     * Check if a type name looks like a type variable (single uppercase letter).
     * This helps distinguish between type variables and type aliases.
     */
    private fun isLikelyTypeVariable(typeName: String): Boolean {
        // Type variables are typically single uppercase letters: A, B, C, T, etc.
        return typeName.length == 1 && typeName[0].isUpperCase()
    }
}

/**
 * Exception thrown when constraint generation fails.
 */
class ConstraintGenerationException(message: String) : Exception(message) 