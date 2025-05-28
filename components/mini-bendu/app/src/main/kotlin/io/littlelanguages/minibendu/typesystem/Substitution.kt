package io.littlelanguages.minibendu.typesystem

/**
 * Represents a substitution mapping from type variables to types.
 * This is a foundational component for unification and type inference.
 * 
 * A substitution is an immutable mapping from type variables to types that can be
 * applied to types to replace variables with their mapped types.
 */
class Substitution internal constructor(
    private val mappings: Map<TypeVariable, Type> = emptyMap()
) {
    
    companion object {
        /**
         * The empty substitution (identity).
         */
        val empty = Substitution()
        
        /**
         * Create a substitution with a single mapping.
         */
        fun single(typeVar: TypeVariable, type: Type): Substitution {
            return Substitution(mapOf(typeVar to type))
        }
        
        /**
         * Create a builder for constructing substitutions.
         */
        fun builder(): SubstitutionBuilder {
            return SubstitutionBuilder()
        }
    }
    
    /**
     * Apply this substitution to a type, replacing all mapped type variables.
     */
    fun apply(type: Type): Type {
        return when (type) {
            is TypeVariable -> mappings[type] ?: type
            is PrimitiveType -> type
            is LiteralStringType -> type
            is FunctionType -> FunctionType(apply(type.domain), apply(type.codomain))
            is RecordType -> applyToRecordType(type)
            is TupleType -> TupleType(type.elements.map { apply(it) })
            is RecursiveType -> applyToRecursiveType(type)
            is UnionType -> {
                val substitutedAlternatives = type.alternatives.map { apply(it) }.toSet()
                UnionType.create(substitutedAlternatives)
            }
            is IntersectionType -> IntersectionType(type.members.map { apply(it) }.toSet())
            is TypeAlias -> TypeAlias(type.name, type.typeArguments.map { apply(it) })
        }
    }
    
    /**
     * Apply substitution to a record type, handling row variable substitution properly.
     */
    private fun applyToRecordType(recordType: RecordType): RecordType {
        val substitutedFields = recordType.fields.mapValues { (_, fieldType) -> apply(fieldType) }
        
        if (recordType.rowVar == null) {
            // Closed record - just apply substitution to fields
            return RecordType(substitutedFields)
        }
        
        // Open record - need to handle row variable substitution
        val substitutedRowVar = apply(recordType.rowVar)
        
        return when (substitutedRowVar) {
            is TypeVariable -> {
                // Row variable is still a variable - keep it as the row variable
                RecordType(substitutedFields, substitutedRowVar)
            }
            is RecordType -> {
                // Row variable was substituted with a record - merge the fields
                val mergedFields = substitutedFields + substitutedRowVar.fields
                RecordType(mergedFields, substitutedRowVar.rowVar)
            }
            else -> {
                // Row variable was substituted with something else (shouldn't happen in well-formed substitutions)
                // For safety, treat as closed record
                RecordType(substitutedFields)
            }
        }
    }
    
    /**
     * Apply substitution to a recursive type, being careful about variable scoping.
     * The recursive variable is bound within the type body, so we must avoid
     * substituting it if it's bound to something else outside.
     */
    private fun applyToRecursiveType(recursiveType: RecursiveType): RecursiveType {
        // Remove the recursive variable from the substitution to avoid conflicts
        // with the binding within the recursive type
        val restrictedSubst = this.remove(recursiveType.recursiveVar)
        
        // Apply the restricted substitution to the body
        val substitutedBody = restrictedSubst.apply(recursiveType.body)
        
        return RecursiveType(recursiveType.name, recursiveType.recursiveVar, substitutedBody)
    }
    
    /**
     * Compose this substitution with another: (this ∘ other)
     * The result applies other first, then this.
     */
    fun compose(other: Substitution): Substitution {
        val newMappings = mutableMapOf<TypeVariable, Type>()
        
        // Apply this substitution to all mappings in other
        for ((variable, type) in other.mappings) {
            newMappings[variable] = this.apply(type)
        }
        
        // Add mappings from this substitution that aren't in other
        for ((variable, type) in this.mappings) {
            if (variable !in other.mappings) {
                newMappings[variable] = type
            }
        }
        
        return Substitution(newMappings)
    }
    
    /**
     * Get the domain (set of mapped type variables) of this substitution.
     */
    fun domain(): Set<TypeVariable> {
        return mappings.keys.toSet()
    }
    
    /**
     * Get the range (set of target types) of this substitution.
     */
    fun range(): Set<Type> {
        return mappings.values.toSet()
    }
    
    /**
     * Look up the mapping for a specific type variable.
     * Returns null if the variable is not mapped.
     */
    fun lookup(variable: TypeVariable): Type? {
        return mappings[variable]
    }
    
    /**
     * Create a new substitution restricted to the given set of variables.
     */
    fun restrictTo(variables: Set<TypeVariable>): Substitution {
        val restrictedMappings = mappings.filterKeys { it in variables }
        return Substitution(restrictedMappings)
    }
    
    /**
     * Create a new substitution with the given variable removed.
     */
    fun remove(variable: TypeVariable): Substitution {
        val newMappings = mappings.toMutableMap()
        newMappings.remove(variable)
        return Substitution(newMappings)
    }
    
    /**
     * Returns true if this substitution is empty (no mappings).
     */
    fun isEmpty(): Boolean = mappings.isEmpty()
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Substitution) return false
        return mappings == other.mappings
    }
    
    override fun hashCode(): Int {
        return mappings.hashCode()
    }
    
    override fun toString(): String {
        if (mappings.isEmpty()) {
            return "[]"
        }
        val mappingStrings = mappings.map { (variable, type) -> "$variable → $type" }
        return "[${mappingStrings.joinToString(", ")}]"
    }
}

/**
 * Builder for constructing substitutions with multiple mappings.
 */
class SubstitutionBuilder {
    private val mappings = mutableMapOf<TypeVariable, Type>()
    
    /**
     * Add a mapping from a type variable to a type.
     */
    fun add(typeVar: TypeVariable, type: Type): SubstitutionBuilder {
        mappings[typeVar] = type
        return this
    }
    
    /**
     * Build the final substitution.
     */
    fun build(): Substitution {
        return Substitution(mappings.toMap())
    }
}
