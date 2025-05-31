package io.littlelanguages.minibendu.typesystem

/**
 * Type Alias Registry for Mini-Bendu Type System
 * 
 * This module provides type alias management including:
 * - Type alias definition and storage
 * - Type alias expansion with parameter substitution
 * - Type normalization (recursive expansion)
 * - Circular dependency detection
 * - Performance optimizations for complex alias chains
 */

/**
 * Result of type alias operations
 */
sealed class TypeAliasResult {
    /**
     * Successful type alias operation
     */
    object Success : TypeAliasResult() {
        fun isSuccess(): Boolean = true
        fun isFailure(): Boolean = false
    }
    
    /**
     * Failed type alias operation with error message
     */
    data class Failure(val error: String) : TypeAliasResult() {
        fun isSuccess(): Boolean = false
        fun isFailure(): Boolean = true
    }
}

// Extension properties for easier testing
val TypeAliasResult.isSuccess: Boolean get() = this is TypeAliasResult.Success
val TypeAliasResult.isFailure: Boolean get() = this is TypeAliasResult.Failure  
val TypeAliasResult.error: String get() = (this as TypeAliasResult.Failure).error

/**
 * Represents a type alias definition
 */
data class TypeAliasDefinition(
    val name: String,
    val typeParameters: List<TypeVariable>,
    val aliasedType: Type
) {
    /**
     * Expand this alias with concrete type arguments
     */
    fun expand(typeArguments: List<Type>): Type? {
        if (typeArguments.size != typeParameters.size) {
            return null // Parameter count mismatch
        }
        
        if (typeArguments.isEmpty()) {
            return aliasedType
        }
        
        // Create substitution from type parameters to arguments
        val substitution = Substitution.builder()
        for ((param, arg) in typeParameters.zip(typeArguments)) {
            substitution.add(param, arg)
        }
        
        return substitution.build().apply(aliasedType)
    }
    
    /**
     * Check if this alias directly references another alias
     */
    fun directlyReferences(aliasName: String): Boolean {
        return findTypeAliasReferences(aliasedType).contains(aliasName)
    }
    
    /**
     * Find all type alias references in a type
     */
    private fun findTypeAliasReferences(type: Type): Set<String> {
        return when (type) {
            is TypeAlias -> setOf(type.name) + type.typeArguments.flatMap { findTypeAliasReferences(it) }.toSet()
            is FunctionType -> findTypeAliasReferences(type.domain) + findTypeAliasReferences(type.codomain)
            is RecordType -> {
                val fieldRefs = type.fields.values.flatMap { findTypeAliasReferences(it) }.toSet()
                val rowRefs = type.rowVar?.let { findTypeAliasReferences(it) } ?: emptySet()
                fieldRefs + rowRefs
            }
            is TupleType -> type.elements.flatMap { findTypeAliasReferences(it) }.toSet()
            is UnionType -> type.alternatives.flatMap { findTypeAliasReferences(it) }.toSet()
            else -> emptySet()
        }
    }
}

/**
 * Registry for managing type aliases
 */
class TypeAliasRegistry {
    private val aliases = mutableMapOf<String, TypeAliasDefinition>()
    private val normalizationCache = mutableMapOf<TypeAlias, Type>()
    
    /**
     * Define a new type alias
     */
    fun defineAlias(name: String, typeParameters: List<TypeVariable>, aliasedType: Type): TypeAliasResult {
        val definition = TypeAliasDefinition(name, typeParameters, aliasedType)
        
        // Check for invalid direct self-reference first
        if (definition.directlyReferences(name) && !isValidRecursiveType(definition)) {
            return TypeAliasResult.Failure("Invalid recursive type alias '$name': direct self-reference without structural protection")
        }
        
        // Check for circular dependencies (but allow valid recursive types)
        if (wouldCreateCycle(name, definition) && !isValidRecursiveType(definition)) {
            return TypeAliasResult.Failure("circular dependency detected in type alias '$name'")
        }
        
        aliases[name] = definition
        
        // Clear normalization cache as new alias might affect existing normalizations
        normalizationCache.clear()
        
        return TypeAliasResult.Success
    }
    
    /**
     * Check if an alias exists
     */
    fun hasAlias(name: String): Boolean {
        return aliases.containsKey(name)
    }
    
    /**
     * Expand a type alias with given type arguments
     */
    fun expandAlias(name: String, typeArguments: List<Type>): Type? {
        val definition = aliases[name] ?: return null
        return definition.expand(typeArguments)
    }
    
    /**
     * Expand a type alias to a RecursiveType if it's recursive, or expand normally if not
     */
    fun expandAliasToRecursiveType(name: String, typeArguments: List<Type>): Type? {
        val definition = aliases[name] ?: return null
        
        // Check if this is a recursive type alias
        if (definition.directlyReferences(name) && isValidRecursiveType(definition)) {
            // Create a RecursiveType for this recursive alias
            val recursiveVar = TypeVariable.fresh()
            
            // First, substitute type parameters with arguments if any
            val parameterSubstitution = if (typeArguments.isEmpty()) {
                Substitution.empty
            } else {
                val substitutionBuilder = Substitution.builder()
                for ((param, arg) in definition.typeParameters.zip(typeArguments)) {
                    substitutionBuilder.add(param, arg)
                }
                substitutionBuilder.build()
            }
            
            // Apply parameter substitution to get the body with parameters replaced
            val bodyWithParams = parameterSubstitution.apply(definition.aliasedType)
            
            // Now substitute recursive references with the recursive variable
            val bodyWithRecursion = substituteRecursiveReferences(bodyWithParams, name, typeArguments, recursiveVar)
            
            return RecursiveType(name, recursiveVar, bodyWithRecursion)
        } else {
            // Not recursive, expand normally
            return definition.expand(typeArguments)
        }
    }
    
    /**
     * Substitute recursive type alias references with a recursive variable
     */
    private fun substituteRecursiveReferences(type: Type, aliasName: String, aliasArgs: List<Type>, recursiveVar: TypeVariable): Type {
        return when (type) {
            is TypeAlias -> {
                if (type.name == aliasName && type.typeArguments == aliasArgs) {
                    // This is the recursive reference - replace with recursive variable
                    recursiveVar
                } else {
                    // Different type alias - recursively substitute in its arguments
                    TypeAlias(type.name, type.typeArguments.map { substituteRecursiveReferences(it, aliasName, aliasArgs, recursiveVar) })
                }
            }
            is FunctionType -> FunctionType(
                substituteRecursiveReferences(type.domain, aliasName, aliasArgs, recursiveVar),
                substituteRecursiveReferences(type.codomain, aliasName, aliasArgs, recursiveVar)
            )
            is RecordType -> {
                val substitutedFields = type.fields.mapValues { (_, fieldType) ->
                    substituteRecursiveReferences(fieldType, aliasName, aliasArgs, recursiveVar)
                }
                val substitutedRowVar = type.rowVar?.let { 
                    substituteRecursiveReferences(it, aliasName, aliasArgs, recursiveVar) as? TypeVariable 
                } ?: type.rowVar
                RecordType(substitutedFields, substitutedRowVar)
            }
            is TupleType -> TupleType(type.elements.map { 
                substituteRecursiveReferences(it, aliasName, aliasArgs, recursiveVar) 
            })
            is UnionType -> UnionType(type.alternatives.map { 
                substituteRecursiveReferences(it, aliasName, aliasArgs, recursiveVar) 
            }.toSet())
            is IntersectionType -> IntersectionType(type.members.map { 
                substituteRecursiveReferences(it, aliasName, aliasArgs, recursiveVar) 
            }.toSet())
            else -> type // Primitive types, variables, etc. don't need substitution
        }
    }
    
    /**
     * Normalize a type by recursively expanding all type aliases
     */
    fun normalizeType(type: Type): Type {
        return when (type) {
            is TypeAlias -> {
                // Check cache first
                normalizationCache[type]?.let { return it }
                
                // Try to expand as recursive type first
                val expanded = expandAliasToRecursiveType(type.name, type.typeArguments)
                if (expanded == null) {
                    // Undefined alias - return as-is
                    type
                } else if (expanded is RecursiveType) {
                    // This is a recursive type - cache and return it
                    normalizationCache[type] = expanded
                    expanded
                } else {
                    // Regular type alias - recursively normalize the expanded type
                    val normalized = normalizeType(expanded)
                    normalizationCache[type] = normalized
                    normalized
                }
            }
            is FunctionType -> FunctionType(normalizeType(type.domain), normalizeType(type.codomain))
            is RecordType -> {
                val normalizedFields = type.fields.mapValues { normalizeType(it.value) }
                val normalizedRowVar = type.rowVar?.let { normalizeType(it) as? TypeVariable }
                RecordType(normalizedFields, normalizedRowVar)
            }
            is TupleType -> TupleType(type.elements.map { normalizeType(it) })
            is UnionType -> UnionType(type.alternatives.map { normalizeType(it) }.toSet())
            else -> type // Primitive types, variables, etc. don't need normalization
        }
    }
    
    /**
     * Check if adding a new alias would create a cycle
     */
    private fun wouldCreateCycle(newAliasName: String, definition: TypeAliasDefinition): Boolean {
        // Get all aliases referenced by this new definition
        val referencedAliases = findAllReferencedAliases(definition.aliasedType)
        
        // If the new alias doesn't reference any existing aliases, no cycle
        if (referencedAliases.isEmpty()) {
            return false
        }
        
        // Temporarily add the new alias to check for cycles
        val tempAliases = aliases.toMutableMap()
        tempAliases[newAliasName] = definition
        
        // Check if any of the referenced aliases eventually reference back to this one
        return referencedAliases.any { refAlias ->
            checkForCyclicDependency(refAlias, newAliasName, tempAliases, mutableSetOf())
        }
    }
    
    /**
     * Recursively check for cyclic dependencies
     */
    private fun checkForCyclicDependency(
        currentAlias: String, 
        targetAlias: String, 
        aliasMap: Map<String, TypeAliasDefinition>, 
        visited: MutableSet<String>
    ): Boolean {
        if (currentAlias == targetAlias) {
            return true // Found a cycle back to the target
        }
        
        if (currentAlias in visited) {
            return false // Already visited this alias in this path
        }
        
        visited.add(currentAlias)
        
        val definition = aliasMap[currentAlias]
        if (definition != null) {
            val referencedAliases = findAllReferencedAliases(definition.aliasedType)
            
            for (refAlias in referencedAliases) {
                if (checkForCyclicDependency(refAlias, targetAlias, aliasMap, visited)) {
                    return true
                }
            }
        }
        
        visited.remove(currentAlias)
        return false
    }
    
    /**
     * Find all type alias references in a type
     */
    private fun findAllReferencedAliases(type: Type): Set<String> {
        return when (type) {
            is TypeAlias -> setOf(type.name) + type.typeArguments.flatMap { findAllReferencedAliases(it) }.toSet()
            is FunctionType -> findAllReferencedAliases(type.domain) + findAllReferencedAliases(type.codomain)
            is RecordType -> {
                val fieldRefs = type.fields.values.flatMap { findAllReferencedAliases(it) }.toSet()
                val rowRefs = type.rowVar?.let { findAllReferencedAliases(it) } ?: emptySet()
                fieldRefs + rowRefs
            }
            is TupleType -> type.elements.flatMap { findAllReferencedAliases(it) }.toSet()
            is UnionType -> type.alternatives.flatMap { findAllReferencedAliases(it) }.toSet()
            else -> emptySet()
        }
    }
    
    /**
     * Check if a recursive type definition is valid
     * Valid recursive types must have structural protection (e.g., through records, functions, etc.)
     */
    private fun isValidRecursiveType(definition: TypeAliasDefinition): Boolean {
        // A recursive type is valid only if it has structural protection
        // Direct type alias references (T = U) are never valid recursive types
        return when (val aliased = definition.aliasedType) {
            is TypeAlias -> false // Type alias to type alias is never valid recursion
            is RecordType, is FunctionType, is TupleType, is UnionType -> true // Has structural protection
            else -> false
        }
    }
    
    /**
     * Get all defined aliases (for debugging/testing)
     */
    internal fun getAllAliases(): Map<String, TypeAliasDefinition> {
        return aliases.toMap()
    }
    
    /**
     * Clear all aliases (for testing)
     */
    internal fun clear() {
        aliases.clear()
        normalizationCache.clear()
    }
} 