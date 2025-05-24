package io.littlelanguages.minibendu.typesystem

/**
 * Utility functions for working with union types.
 * 
 * This module provides helper functions for:
 * - Union type analysis and manipulation
 * - Exhaustiveness checking foundation
 * - Union type simplification and normalization
 * - Subtyping relationships with unions
 */
object UnionTypeUtils {
    
    /**
     * Flatten nested union types into a single set of alternatives.
     * Example: (A | (B | C)) | D becomes {A, B, C, D}
     */
    fun flatten(type: Type): Set<Type> {
        return when (type) {
            is UnionType -> type.alternatives.flatMap { flatten(it) }.toSet()
            else -> setOf(type)
        }
    }
    
    /**
     * Check if a set of types covers all alternatives in a union type.
     * This is the foundation for exhaustiveness checking in pattern matching.
     */
    fun isExhaustive(unionType: UnionType, coveredTypes: Set<Type>): Boolean {
        val flattened = flatten(unionType)
        return flattened.all { alternative ->
            coveredTypes.any { covered -> 
                covered.structurallyEquivalent(alternative) || 
                (covered is UnionType && covered.contains(alternative))
            }
        }
    }
    
    /**
     * Find missing alternatives that need to be covered for exhaustiveness.
     */
    fun findMissingAlternatives(unionType: UnionType, coveredTypes: Set<Type>): Set<Type> {
        val flattened = flatten(unionType)
        return flattened.filter { alternative ->
            !coveredTypes.any { covered ->
                covered.structurallyEquivalent(alternative) ||
                (covered is UnionType && covered.contains(alternative))
            }
        }.toSet()
    }
    
    /**
     * Compute the intersection of two union types.
     * Returns the types that appear in both unions.
     */
    fun intersect(union1: UnionType, union2: UnionType): Set<Type> {
        return union1.alternatives.filter { alt1 ->
            union2.alternatives.any { alt2 -> alt1.structurallyEquivalent(alt2) }
        }.toSet()
    }
    
    /**
     * Compute the difference between two union types.
     * Returns alternatives in the first union that are not in the second.
     */
    fun difference(union1: UnionType, union2: UnionType): Set<Type> {
        return union1.alternatives.filter { alt1 ->
            !union2.alternatives.any { alt2 -> alt1.structurallyEquivalent(alt2) }
        }.toSet()
    }
    
    /**
     * Check if a union type is a discriminated union (all alternatives are literal string types).
     * This is useful for enum-like type checking and exhaustiveness analysis.
     */
    fun isDiscriminatedUnion(unionType: UnionType): Boolean {
        return unionType.alternatives.all { it is LiteralStringType }
    }
    
    /**
     * Extract literal string values from a discriminated union.
     */
    fun getDiscriminatedValues(unionType: UnionType): Set<String> {
        return unionType.alternatives
            .filterIsInstance<LiteralStringType>()
            .map { it.value }
            .toSet()
    }
    
    /**
     * Simplify a union type by removing redundant alternatives.
     * This includes:
     * - Removing duplicates
     * - Removing alternatives that are subtypes of other alternatives
     * - Flattening nested unions
     */
    fun simplify(unionType: UnionType): Type {
        val flattened = flatten(unionType)
        
        // Remove duplicates (handled by Set)
        val deduplicated = flattened.toSet()
        
        // TODO: Add more sophisticated simplification
        // - Remove alternatives that are subtypes of others
        // - Merge compatible record types
        // - Simplify nested function types
        
        return when {
            deduplicated.isEmpty() -> throw IllegalStateException("Cannot simplify union to empty set")
            deduplicated.size == 1 -> deduplicated.first()
            else -> UnionType(deduplicated)
        }
    }
    
    /**
     * Check if a type can be considered a member of a union.
     * This is more permissive than exact equality and considers subtyping.
     */
    fun isMemberOf(type: Type, unionType: UnionType): Boolean {
        return unionType.alternatives.any { alternative ->
            type.structurallyEquivalent(alternative) ||
            // Could add more sophisticated subtype checking here
            (type is LiteralStringType && alternative == Types.String)
        }
    }
    
    /**
     * Create a union type from a collection of types with automatic normalization.
     */
    fun createUnion(types: Collection<Type>): Type {
        return when {
            types.isEmpty() -> throw IllegalArgumentException("Cannot create union from empty collection")
            types.size == 1 -> types.first()
            else -> UnionType.create(types.toSet())
        }
    }
    
    /**
     * Convert a union type to a human-readable string with proper formatting.
     */
    fun formatUnion(unionType: UnionType): String {
        val sortedAlternatives = unionType.alternatives.sortedBy { it.toString() }
        return sortedAlternatives.joinToString(" | ")
    }
    
    /**
     * Check if two union types have overlapping alternatives.
     */
    fun hasOverlap(union1: UnionType, union2: UnionType): Boolean {
        return union1.alternatives.any { alt1 ->
            union2.alternatives.any { alt2 -> alt1.structurallyEquivalent(alt2) }
        }
    }
    
    /**
     * Merge two union types, combining their alternatives.
     */
    fun merge(union1: UnionType, union2: UnionType): UnionType {
        val combinedAlternatives = union1.alternatives + union2.alternatives
        return UnionType.create(combinedAlternatives) as UnionType
    }
    
    /**
     * Extract all type variables from a union type.
     */
    fun extractTypeVariables(unionType: UnionType): Set<TypeVariable> {
        return unionType.alternatives.flatMap { it.freeTypeVariables() }.toSet()
    }
    
    /**
     * Check if a union type contains only concrete types (no type variables).
     */
    fun isConcrete(unionType: UnionType): Boolean {
        return extractTypeVariables(unionType).isEmpty()
    }
} 