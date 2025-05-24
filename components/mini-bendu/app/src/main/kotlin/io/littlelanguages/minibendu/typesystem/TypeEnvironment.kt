package io.littlelanguages.minibendu.typesystem

/**
 * Type environment for managing variable bindings and scoping in the type inference system.
 * Task 12 of Phase 1 - TypeEnvironment implementation.
 * 
 * Supports:
 * - Variable lookup with proper scoping
 * - Type generalization respecting environment context
 * - Nested scopes with shadowing behavior
 * - Substitution application
 * - Immutable environment operations
 */
data class TypeEnvironment(
    private val bindings: Map<String, TypeScheme> = emptyMap(),
    private val scopes: List<Map<String, TypeScheme>> = emptyList()
) {
    companion object {
        /**
         * Creates an empty type environment
         */
        fun empty(): TypeEnvironment = TypeEnvironment()
    }
    
    /**
     * Binds a variable to a type scheme in the current scope
     */
    fun bind(name: String, scheme: TypeScheme): TypeEnvironment {
        return if (scopes.isEmpty()) {
            // No scopes - bind to the main environment
            TypeEnvironment(bindings + (name to scheme), scopes)
        } else {
            // In a scope - add to the top scope
            val topScope = scopes.last() + (name to scheme)
            val newScopes = scopes.dropLast(1) + topScope
            TypeEnvironment(bindings, newScopes)
        }
    }
    
    /**
     * Looks up a variable in the environment, checking scopes from innermost to outermost
     */
    fun lookup(name: String): TypeScheme? {
        // Check scopes from innermost (last) to outermost (first)
        for (scope in scopes.reversed()) {
            scope[name]?.let { return it }
        }
        
        // Check main bindings
        return bindings[name]
    }
    
    /**
     * Generalizes a type by quantifying over all type variables that are free in the type
     * but not free in the environment
     */
    fun generalize(type: Type): TypeScheme {
        val typeFreeVars = type.freeTypeVariables()
        val envFreeVars = freeTypeVariables()
        
        val quantifiableVars = typeFreeVars - envFreeVars
        
        return TypeScheme(quantifiableVars, type)
    }
    
    /**
     * Opens a new scope for variable bindings
     */
    fun openScope(): TypeEnvironment {
        return TypeEnvironment(bindings, scopes + emptyMap())
    }
    
    /**
     * Closes the current scope, removing all bindings from the innermost scope
     */
    fun closeScope(): TypeEnvironment {
        return if (scopes.isEmpty()) {
            this // No scopes to close
        } else {
            TypeEnvironment(bindings, scopes.dropLast(1))
        }
    }
    
    /**
     * Applies a substitution to all type schemes in the environment
     */
    fun apply(substitution: Substitution): TypeEnvironment {
        val newBindings = bindings.mapValues { (_, scheme) ->
            scheme.apply(substitution)
        }
        
        val newScopes = scopes.map { scope ->
            scope.mapValues { (_, scheme) ->
                scheme.apply(substitution)
            }
        }
        
        return TypeEnvironment(newBindings, newScopes)
    }
    
    /**
     * Returns all free type variables in the environment
     */
    fun freeTypeVariables(): Set<TypeVariable> {
        val bindingsFreeVars = bindings.values.flatMap { it.freeTypeVariables() }.toSet()
        val scopesFreeVars = scopes.flatMap { scope ->
            scope.values.flatMap { it.freeTypeVariables() }
        }.toSet()
        
        return bindingsFreeVars + scopesFreeVars
    }
    
    /**
     * Returns the domain (set of variable names) accessible in the current environment
     */
    fun domain(): Set<String> {
        val bindingNames = bindings.keys
        val scopeNames = scopes.flatMap { it.keys }.toSet()
        
        return bindingNames + scopeNames
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TypeEnvironment) return false
        
        return bindings == other.bindings && scopes == other.scopes
    }
    
    override fun hashCode(): Int {
        return 31 * bindings.hashCode() + scopes.hashCode()
    }
    
    override fun toString(): String {
        val allBindings = mutableMapOf<String, TypeScheme>()
        allBindings.putAll(bindings)
        
        // Add scope bindings (inner scopes override outer ones)
        for (scope in scopes) {
            allBindings.putAll(scope)
        }
        
        return "TypeEnvironment(${allBindings.entries.joinToString(", ") { "${it.key}: ${it.value}" }})"
    }
}
