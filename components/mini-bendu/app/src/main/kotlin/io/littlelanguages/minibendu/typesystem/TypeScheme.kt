package io.littlelanguages.minibendu.typesystem

/**
 * Type Scheme represents a polymorphic type with quantified type variables.
 * 
 * A type scheme consists of:
 * - A set of quantified type variables (the ∀ variables)
 * - A type expression that may contain these variables
 * 
 * Examples:
 * - ∀a. a -> a (identity function)
 * - ∀a,b. (a -> b) -> [a] -> [b] (map function)
 * - Int -> String (monomorphic function, no quantified variables)
 * 
 * Type schemes are used for:
 * - Generalization: Converting a type with free variables to a polymorphic scheme
 * - Instantiation: Creating fresh instances of polymorphic types
 * - Type environment storage: Storing polymorphic function types
 */
data class TypeScheme(
    val quantifiedVariables: Set<TypeVariable>,
    val body: Type
) {
    // Alias for backward compatibility with existing code
    val type: Type get() = body
    companion object {
        /**
         * Creates a monomorphic type scheme with no quantified variables
         */
        fun monomorphic(type: Type): TypeScheme {
            return TypeScheme(emptySet(), type)
        }
        
        /**
         * Generalizes a type by quantifying over the given free variables
         * 
         * @param type The type to generalize
         * @param freeVariables The variables to quantify over
         * @return A type scheme quantifying over the free variables
         */
        fun generalize(type: Type, freeVariables: Set<TypeVariable>): TypeScheme {
            return TypeScheme(freeVariables, type)
        }
    }
    
    /**
     * Returns true if this scheme has no quantified variables (is monomorphic)
     */
    fun isMonomorphic(): Boolean = quantifiedVariables.isEmpty()
    
    /**
     * Returns true if this scheme has quantified variables (is polymorphic)
     */
    fun isPolymorphic(): Boolean = quantifiedVariables.isNotEmpty()
    
    /**
     * Instantiates this type scheme by creating fresh type variables
     * for all quantified variables and applying the substitution.
     * 
     * @return Pair of (instantiated type, substitution used)
     */
    fun instantiate(): Pair<Type, Substitution> {
        if (isMonomorphic()) {
            return Pair(type, Substitution.empty)
        }
        
        // Create fresh variables for each quantified variable
        val substitutionBuilder = Substitution.builder()
        
        for (quantifiedVar in quantifiedVariables) {
            val freshVar = TypeVariable.fresh(quantifiedVar.level)
            substitutionBuilder.add(quantifiedVar, freshVar)
        }
        
        val substitution = substitutionBuilder.build()
        val instantiatedType = substitution.apply(body)
        
        return Pair(instantiatedType, substitution)
    }
    
    /**
     * Calculates the free type variables in this scheme
     * (variables that appear in the type but are not quantified)
     */
    fun freeTypeVariables(): Set<TypeVariable> {
        val typeVars = body.freeTypeVariables()
        return typeVars - quantifiedVariables
    }
    
    /**
     * Legacy alias for freeTypeVariables()
     */
    fun freeVariables(): Set<TypeVariable> = freeTypeVariables()
    
    /**
     * Applies a substitution to this type scheme.
     * Only affects free variables (not quantified ones).
     * 
     * @param substitution The substitution to apply
     * @return A new type scheme with the substitution applied to free variables
     */
    fun apply(substitution: Substitution): TypeScheme {
        if (substitution.isEmpty()) {
            return this
        }
        
        // Filter out substitutions for quantified variables
        val filteredSubstitution = substitution.restrictTo(freeVariables())
        
        if (filteredSubstitution.isEmpty()) {
            return this
        }
        
        val substitutedType = filteredSubstitution.apply(body)
        return TypeScheme(quantifiedVariables, substitutedType)
    }
    
    /**
     * Checks if this type scheme is alpha-equivalent to another
     * (represents the same polymorphic type up to variable renaming)
     */
    fun isAlphaEquivalent(other: TypeScheme): Boolean {
        // Must have same number of quantified variables
        if (quantifiedVariables.size != other.quantifiedVariables.size) {
            return false
        }
        
        // If both are monomorphic, just compare types
        if (isMonomorphic() && other.isMonomorphic()) {
            return body == other.body
        }
        
        // For polymorphic schemes, we need to check if they're the same up to renaming
        // Create a renaming from our variables to their variables
        val ourVars = quantifiedVariables.toList()
        val theirVars = other.quantifiedVariables.toList()
        
        if (ourVars.size != theirVars.size) {
            return false
        }
        
        // Try to create a consistent renaming
        val substitutionBuilder = Substitution.builder()
        for (i in ourVars.indices) {
            substitutionBuilder.add(ourVars[i], theirVars[i])
        }
        
        val renaming = substitutionBuilder.build()
        val renamedType = renaming.apply(body)
        
        return renamedType == other.body
    }
    
    override fun toString(): String {
        return if (isMonomorphic()) {
            body.toString()
        } else {
            val vars = quantifiedVariables.joinToString(",") { it.toString() }
            "∀$vars. $body"
        }
    }
}
