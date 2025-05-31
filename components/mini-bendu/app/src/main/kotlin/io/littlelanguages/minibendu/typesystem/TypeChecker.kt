package io.littlelanguages.minibendu.typesystem

import io.littlelanguages.minibendu.*
import io.littlelanguages.scanpiler.Location

/**
 * Result of type checking operation.
 * Contains either a successful type with substitution or detailed error information.
 */
sealed class TypeCheckResult {
    abstract fun isSuccess(): Boolean
    abstract fun isFailure(): Boolean
    
    data class Success(
        val type: Type,
        val substitution: Substitution,
        val environment: TypeEnvironment
    ) : TypeCheckResult() {
        override fun isSuccess(): Boolean = true
        override fun isFailure(): Boolean = false
        
        fun getFinalType(): Type = substitution.apply(type)
    }
    
    data class Failure(
        val error: String,
        val location: SourceLocation?
    ) : TypeCheckResult() {
        override fun isSuccess(): Boolean = false
        override fun isFailure(): Boolean = true
    }
}

/**
 * Main type checker class that coordinates the type checking process.
 * 
 * This class provides the main entry point for type checking mini-bendu expressions.
 * It integrates constraint generation, constraint solving, and error reporting into
 * a unified interface.
 * 
 * Features:
 * - Visitor pattern for traversing AST nodes
 * - Integration with constraint generation and solving
 * - Comprehensive error reporting with source locations
 * - Environment management for bindings
 * - Support for incremental type checking
 * - Type alias support and resolution
 */
class TypeChecker(
    private val initialEnvironment: TypeEnvironment = TypeEnvironment.empty(),
    private val typeAliasRegistry: TypeAliasRegistry = TypeAliasRegistry()
) {
    
    /**
     * Type check a single expression.
     * Returns either the inferred type or detailed error information.
     */
    fun typeCheck(expr: Expr): TypeCheckResult {
        return typeCheckWithEnvironment(expr, initialEnvironment)
    }
    
    /**
     * Type check an expression with a specific environment.
     * This enables incremental type checking and reuse of existing bindings.
     */
    fun typeCheckWithEnvironment(expr: Expr, environment: TypeEnvironment): TypeCheckResult {
        return try {
            // Step 1: Generate constraints
            val generator = ConstraintGenerator(environment, typeAliasRegistry)
            val constraintResult = generator.generateConstraints(expr)
            
            if (constraintResult.isFailure()) {
                val failure = constraintResult as ConstraintGenerationResult.Failure
                return TypeCheckResult.Failure(
                    error = failure.error,
                    location = extractSourceLocation(expr.location())
                )
            }
            
            val success = constraintResult as ConstraintGenerationResult.Success
            
            // Step 2: Solve constraints
            val solver = ConstraintSolver(typeAliasRegistry)
            val solverResult = solver.solve(success.constraints)
            
            when (solverResult) {
                is ConstraintSolverResult.Success -> {
                    TypeCheckResult.Success(
                        type = success.type,
                        substitution = solverResult.substitution,
                        environment = environment
                    )
                }
                is ConstraintSolverResult.Failure -> {
                    TypeCheckResult.Failure(
                        error = solverResult.error,
                        location = extractSourceLocation(expr.location())
                    )
                }
            }
        } catch (e: Exception) {
            TypeCheckResult.Failure(
                error = "Internal type checker error: ${e.message}",
                location = extractSourceLocation(expr.location())
            )
        }
    }
    
    /**
     * Type check a program (top-level expression) and return comprehensive results.
     * This method provides additional program-level analysis and validation.
     */
    fun typeCheckProgram(program: Expr): ProgramTypeCheckResult {
        val result = typeCheck(program)
        
        return when (result) {
            is TypeCheckResult.Success -> {
                ProgramTypeCheckResult.Success(
                    programType = result.getFinalType(),
                    environment = result.environment,
                    substitution = result.substitution,
                    warnings = extractWarnings(program, result.environment)
                )
            }
            is TypeCheckResult.Failure -> {
                ProgramTypeCheckResult.Failure(
                    error = result.error,
                    location = result.location,
                    suggestions = generateSuggestions(result.error, program)
                )
            }
        }
    }
    
    /**
     * Type check multiple expressions incrementally.
     * Builds up the environment progressively, useful for REPL or module checking.
     */
    fun typeCheckIncrementally(expressions: List<Expr>): IncrementalTypeCheckResult {
        var currentEnvironment = initialEnvironment
        val results = mutableListOf<TypeCheckResult>()
        val errors = mutableListOf<TypeCheckResult.Failure>()
        
        for (expr in expressions) {
            val result = typeCheckWithEnvironment(expr, currentEnvironment)
            results.add(result)
            
            when (result) {
                is TypeCheckResult.Success -> {
                    // Update environment with successful bindings
                    currentEnvironment = updateEnvironmentFromExpression(expr, result, currentEnvironment)
                }
                is TypeCheckResult.Failure -> {
                    errors.add(result)
                    // Continue with the current environment (error recovery)
                }
            }
        }
        
        return IncrementalTypeCheckResult(
            results = results,
            finalEnvironment = currentEnvironment,
            errors = errors,
            hasErrors = errors.isNotEmpty()
        )
    }
    
    /**
     * Type check a program with top-level declarations including type aliases.
     * Processes type aliases first, then expressions incrementally.
     */
    fun typeCheckProgram(program: Program): IncrementalTypeCheckResult {
        var currentEnvironment = initialEnvironment
        val results = mutableListOf<TypeCheckResult>()
        val errors = mutableListOf<TypeCheckResult.Failure>()
        
        for (topLevel in program.topLevels) {
            when (topLevel) {
                is TypeAliasDecl -> {
                    // Process type alias declaration
                    val aliasResult = processTypeAliasDeclaration(topLevel)
                    if (aliasResult.isFailure) {
                        val failure = TypeCheckResult.Failure(
                            error = (aliasResult as TypeAliasResult.Failure).error,
                            location = extractSourceLocation(topLevel.id.location)
                        )
                        errors.add(failure)
                        results.add(failure)
                    } else {
                        // Type alias processed successfully - no result type needed
                        val success = TypeCheckResult.Success(
                            type = Types.Unit, // Dummy type for type alias declarations
                            substitution = Substitution.empty,
                            environment = currentEnvironment
                        )
                        results.add(success)
                    }
                }
                is ExprStmt -> {
                    // Process expression statement
                    val expr = topLevel.expr
                    val result = typeCheckWithEnvironment(expr, currentEnvironment)
                    results.add(result)
                    
                    when (result) {
                        is TypeCheckResult.Success -> {
                            // Update environment with successful bindings
                            currentEnvironment = updateEnvironmentFromExpression(expr, result, currentEnvironment)
                        }
                        is TypeCheckResult.Failure -> {
                            errors.add(result)
                            // Continue with the current environment (error recovery)
                        }
                    }
                }
            }
        }
        
        return IncrementalTypeCheckResult(
            results = results,
            finalEnvironment = currentEnvironment,
            errors = errors,
            hasErrors = errors.isNotEmpty()
        )
    }
    
    /**
     * Process a type alias declaration and add it to the registry.
     */
    private fun processTypeAliasDeclaration(typeAliasDecl: TypeAliasDecl): TypeAliasResult {
        val aliasName = typeAliasDecl.id.value
        
        // Create mapping from type parameter names to TypeVariable instances
        val typeParameterMapping = mutableMapOf<String, TypeVariable>()
        val typeParameters = typeAliasDecl.typeParams?.map { param ->
            val typeVar = TypeVariable.fresh()
            typeParameterMapping[param.id.value] = typeVar
            typeVar
        } ?: emptyList()
        
        // Create temporary environment with type parameters
        val tempEnvironment = typeParameterMapping.entries.fold(initialEnvironment) { env, (name, typeVar) ->
            env.bind(name, TypeScheme.monomorphic(typeVar))
        }
        
        // Create constraint generator with the environment that knows about type parameters
        val tempGenerator = ConstraintGenerator(tempEnvironment, typeAliasRegistry)
        val aliasedType = tempGenerator.convertTypeExprToType(typeAliasDecl.typeExpr)
        
        // Define the alias in the registry
        return typeAliasRegistry.defineAlias(aliasName, typeParameters, aliasedType)
    }
    
    /**
     * Validate type annotations in expressions.
     * Checks that explicit type annotations are consistent with inferred types.
     */
    fun validateTypeAnnotations(expr: Expr): AnnotationValidationResult {
        val visitor = TypeAnnotationValidator()
        return visitor.validate(expr, initialEnvironment)
    }
    
    /**
     * Extract type information for IDE support.
     * Returns hover information, completions, and other IDE features.
     */
    fun getTypeInformation(expr: Expr, location: Location): TypeInformation {
        val result = typeCheck(expr)
        return when (result) {
            is TypeCheckResult.Success -> {
                TypeInformation.Available(
                    type = result.getFinalType(),
                    prettyType = formatTypeForDisplay(result.getFinalType()),
                    documentation = generateTypeDocumentation(result.getFinalType())
                )
            }
            is TypeCheckResult.Failure -> {
                TypeInformation.Error(result.error)
            }
        }
    }
    
    // Helper methods
    
    private fun extractSourceLocation(location: Location): SourceLocation {
        return when (location) {
            is io.littlelanguages.scanpiler.LocationCoordinate -> 
                SourceLocation(location.line, location.column)
            is io.littlelanguages.scanpiler.LocationRange -> 
                SourceLocation(location.start.line, location.start.column)
        }
    }
    
    private fun extractWarnings(program: Expr, environment: TypeEnvironment): List<TypeWarning> {
        val warnings = mutableListOf<TypeWarning>()
        
        // Check for unused variables
        val unusedVars = findUnusedVariables(program, environment)
        warnings.addAll(unusedVars.map { varName ->
            TypeWarning.UnusedVariable(varName, extractSourceLocation(program.location()))
        })
        
        // Check for pattern exhaustiveness
        val matchExpressions = findMatchExpressions(program)
        for (matchExpr in matchExpressions) {
            val analysis = PatternAnalyzer.analyze(matchExpr, Types.Unit) // Placeholder type
            if (!analysis.isExhaustive) {
                warnings.add(TypeWarning.NonExhaustiveMatch(
                    extractSourceLocation(matchExpr.location()),
                    analysis.missingPatterns.map { it.toString() }
                ))
            }
        }
        
        return warnings
    }
    
    private fun generateSuggestions(error: String, program: Expr): List<String> {
        val suggestions = mutableListOf<String>()
        
        when {
            error.contains("Undefined variable") -> {
                val varName = extractVariableName(error)
                if (varName != null) {
                    suggestions.add("Did you mean to define '$varName' first?")
                    suggestions.add("Check for typos in variable name '$varName'")
                }
            }
            error.contains("Cannot unify") -> {
                suggestions.add("Check that the expression types match")
                suggestions.add("Consider adding explicit type annotations")
            }
            error.contains("occurs check") -> {
                suggestions.add("Recursive type definition detected - use explicit recursive types")
            }
        }
        
        return suggestions
    }
    
    private fun updateEnvironmentFromExpression(
        expr: Expr, 
        result: TypeCheckResult.Success,
        currentEnv: TypeEnvironment
    ): TypeEnvironment {
        return when (expr) {
            is LetExpr -> {
                val varName = expr.id.value
                val finalType = result.getFinalType()
                
                // For let expressions without bodies (top-level bindings),
                // we need to generalize the type to enable polymorphism
                val scheme = if (expr.body == null) {
                    currentEnv.generalize(finalType)
                } else {
                    // For let expressions with bodies, the type is already properly handled
                    // during constraint generation, so we can use it as-is
                    TypeScheme.monomorphic(finalType)
                }
                
                currentEnv.bind(varName, scheme)
            }
            else -> currentEnv
        }
    }
    
    private fun findUnusedVariables(expr: Expr, environment: TypeEnvironment): List<String> {
        // Simplified implementation - in practice would need more sophisticated analysis
        return emptyList()
    }
    
    private fun findMatchExpressions(expr: Expr): List<MatchExpr> {
        val matches = mutableListOf<MatchExpr>()
        
        fun visitExpr(e: Expr) {
            when (e) {
                is MatchExpr -> matches.add(e)
                is LetExpr -> {
                    visitExpr(e.value)
                    e.body?.let { visitExpr(it) }
                }
                is LambdaExpr -> visitExpr(e.body)
                is ApplicationExpr -> {
                    visitExpr(e.function)
                    e.arguments.forEach { visitExpr(it) }
                }
                is BinaryOpExpr -> {
                    visitExpr(e.left)
                    visitExpr(e.right)
                }
                is IfExpr -> {
                    visitExpr(e.condition)
                    visitExpr(e.thenBranch)
                    visitExpr(e.elseBranch)
                }
                is RecordExpr -> {
                    e.fields.forEach { field ->
                        when (field) {
                            is FieldExpr -> visitExpr(field.value)
                            is SpreadExpr -> visitExpr(field.expr)
                        }
                    }
                }
                is ProjectionExpr -> visitExpr(e.target)
                is TupleExpr -> e.elements.forEach { visitExpr(it) }
                // Literals don't contain subexpressions
                else -> {}
            }
        }
        
        visitExpr(expr)
        return matches
    }
    
    private fun extractVariableName(error: String): String? {
        val regex = Regex("Undefined variable: (\\w+)")
        return regex.find(error)?.groupValues?.get(1)
    }
    
    private fun formatTypeForDisplay(type: Type): String {
        return type.toString() // Could be enhanced with prettier formatting
    }
    
    private fun generateTypeDocumentation(type: Type): String {
        return when (type) {
            is FunctionType -> "Function from ${type.domain} to ${type.codomain}"
            is RecordType -> "Record with fields: ${type.fields.keys.joinToString(", ")}"
            is TupleType -> "Tuple with ${type.elements.size} elements"
            is UnionType -> "Union type with alternatives: ${type.alternatives.joinToString(" | ")}"
            else -> "Type: $type"
        }
    }
}

/**
 * Result of program-level type checking with additional analysis.
 */
sealed class ProgramTypeCheckResult {
    data class Success(
        val programType: Type,
        val environment: TypeEnvironment,
        val substitution: Substitution,
        val warnings: List<TypeWarning>
    ) : ProgramTypeCheckResult()
    
    data class Failure(
        val error: String,
        val location: SourceLocation?,
        val suggestions: List<String>
    ) : ProgramTypeCheckResult()
}

/**
 * Result of incremental type checking across multiple expressions.
 */
data class IncrementalTypeCheckResult(
    val results: List<TypeCheckResult>,
    val finalEnvironment: TypeEnvironment,
    val errors: List<TypeCheckResult.Failure>,
    val hasErrors: Boolean
)

/**
 * Type information for IDE support.
 */
sealed class TypeInformation {
    data class Available(
        val type: Type,
        val prettyType: String,
        val documentation: String
    ) : TypeInformation()
    
    data class Error(val message: String) : TypeInformation()
}

/**
 * Type warnings for additional analysis.
 */
sealed class TypeWarning {
    data class UnusedVariable(
        val variableName: String,
        val location: SourceLocation
    ) : TypeWarning()
    
    data class NonExhaustiveMatch(
        val location: SourceLocation,
        val missingPatterns: List<String>
    ) : TypeWarning()
    
    data class ShadowedVariable(
        val variableName: String,
        val location: SourceLocation
    ) : TypeWarning()
}

/**
 * Result of type annotation validation.
 */
data class AnnotationValidationResult(
    val isValid: Boolean,
    val errors: List<String>,
    val warnings: List<String>
)

/**
 * Visitor for validating type annotations.
 */
class TypeAnnotationValidator {
    fun validate(expr: Expr, environment: TypeEnvironment): AnnotationValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        fun validateExpr(e: Expr) {
            when (e) {
                is LambdaExpr -> {
                    e.paramType?.let { paramType ->
                        // Validate that the parameter type annotation is reasonable
                        val convertedType = convertTypeExprToType(paramType)
                        if (convertedType == null) {
                            errors.add("Invalid type annotation: $paramType")
                        }
                    }
                    validateExpr(e.body)
                }
                is LetExpr -> {
                    e.typeAnnotation?.let { typeAnnotation ->
                        val convertedType = convertTypeExprToType(typeAnnotation)
                        if (convertedType == null) {
                            errors.add("Invalid type annotation: $typeAnnotation")
                        }
                    }
                    validateExpr(e.value)
                    e.body?.let { validateExpr(it) }
                }
                // Add more cases as needed
                else -> {
                    // Recursively validate subexpressions
                    // This is a simplified implementation
                }
            }
        }
        
        validateExpr(expr)
        
        return AnnotationValidationResult(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings
        )
    }
    
    private fun convertTypeExprToType(typeExpr: TypeExpr): Type? {
        return try {
            when (typeExpr) {
                is BaseTypeExpr -> {
                    when (typeExpr.id.value) {
                        "Int" -> Types.Int
                        "String" -> Types.String
                        "Bool" -> Types.Bool
                        "Unit" -> Types.Unit
                        else -> null
                    }
                }
                is FunctionTypeExpr -> {
                    val domain = convertTypeExprToType(typeExpr.from)
                    val codomain = convertTypeExprToType(typeExpr.to)
                    if (domain != null && codomain != null) {
                        FunctionType(domain, codomain)
                    } else null
                }
                // Add more cases as needed
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
} 