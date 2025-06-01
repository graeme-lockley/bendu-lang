package io.littlelanguages.minibendu.typesystem

/**
 * Utility functions for working with intersection types.
 * 
 * This module provides helper functions for:
 * - Intersection type analysis and manipulation
 * - Constraint satisfaction checking
 * - Intersection type simplification and normalization
 * - Subtyping relationships with intersections
 */
object IntersectionTypeUtils {
    
    /**
     * Flatten nested intersection types into a single set of members.
     * Example: (A & (B & C)) & D becomes {A, B, C, D}
     */
    fun flatten(type: Type): Set<Type> {
        return when (type) {
            is IntersectionType -> type.members.flatMap { flatten(it) }.toSet()
            else -> setOf(type)
        }
    }
    
    /**
     * Check if a type satisfies all constraints in an intersection type.
     * This is the foundation for constraint satisfaction checking.
     */
    fun satisfiesAllConstraints(candidateType: Type, intersectionType: IntersectionType): Boolean {
        return intersectionType.members.all { constraint ->
            // Check if the candidate type can be considered compatible with the constraint
            candidateType.structurallyEquivalent(constraint) ||
            isCompatibleWith(candidateType, constraint)
        }
    }
    
    /**
     * Find unsatisfied constraints for a given type against an intersection.
     */
    fun findUnsatisfiedConstraints(candidateType: Type, intersectionType: IntersectionType): Set<Type> {
        return intersectionType.members.filter { constraint ->
            !candidateType.structurallyEquivalent(constraint) &&
            !isCompatibleWith(candidateType, constraint)
        }.toSet()
    }
    
    /**
     * Compute the intersection of two intersection types.
     * Returns the combined constraints from both intersections.
     */
    fun intersect(intersection1: IntersectionType, intersection2: IntersectionType): IntersectionType {
        val combinedMembers = intersection1.members + intersection2.members
        return IntersectionType.create(combinedMembers) as IntersectionType
    }
    
    /**
     * Compute the difference between two intersection types.
     * Returns members in the first intersection that are not in the second.
     */
    fun difference(intersection1: IntersectionType, intersection2: IntersectionType): Set<Type> {
        return intersection1.members.filter { member1 ->
            !intersection2.members.any { member2 -> member1.structurallyEquivalent(member2) }
        }.toSet()
    }
    
    /**
     * Check if an intersection type is a constraint intersection (all members are trait-like).
     * This is useful for type constraint analysis and bound checking.
     */
    fun isConstraintIntersection(intersectionType: IntersectionType): Boolean {
        return intersectionType.members.all { it is TypeAlias }
    }
    
    /**
     * Extract constraint names from a constraint intersection.
     */
    fun getConstraintNames(intersectionType: IntersectionType): Set<String> {
        return intersectionType.members
            .filterIsInstance<TypeAlias>()
            .map { it.name }
            .toSet()
    }
    
    /**
     * Simplify an intersection type by removing redundant members.
     * This includes:
     * - Removing duplicates
     * - Removing members that are supertypes of other members
     * - Flattening nested intersections
     */
    fun simplify(intersectionType: IntersectionType): Type {
        val flattened = flatten(intersectionType)
        
        // Remove duplicates (handled by Set)
        val deduplicated = flattened.toSet()
        
        // Remove members that are supertypes of others
        val simplified = deduplicated.filter { type ->
            !deduplicated.any { other ->
                other != type && other.isSupertypeOf(type)
            }
        }.toSet()
        
        return when {
            simplified.isEmpty() -> throw IllegalStateException("Cannot simplify intersection to empty set")
            simplified.size == 1 -> simplified.first()
            else -> IntersectionType(simplified)
        }
    }
    
    /**
     * Check if a type can be considered compatible with a constraint.
     * This is more permissive than exact equality and considers subtyping.
     */
    fun isCompatibleWith(type: Type, constraint: Type): Boolean {
        return when {
            type.structurallyEquivalent(constraint) -> true
            type is IntersectionType && type.contains(constraint) -> true
            constraint is IntersectionType && constraint.contains(type) -> true
            // Could add more sophisticated compatibility checking here
            else -> false
        }
    }
    
    /**
     * Create an intersection type from a collection of types with automatic normalization.
     */
    fun createIntersection(types: Collection<Type>): Type {
        return when {
            types.isEmpty() -> throw IllegalArgumentException("Cannot create intersection from empty collection")
            types.size == 1 -> types.first()
            else -> IntersectionType.create(types.toSet())
        }
    }
    
    /**
     * Convert an intersection type to a human-readable string with proper formatting.
     */
    fun formatIntersection(intersectionType: IntersectionType): String {
        val sortedMembers = intersectionType.members.sortedBy { it.toString() }
        return sortedMembers.joinToString(" & ")
    }
    
    /**
     * Check if two intersection types have overlapping members.
     */
    fun hasOverlap(intersection1: IntersectionType, intersection2: IntersectionType): Boolean {
        return intersection1.members.any { member1 ->
            intersection2.members.any { member2 -> member1.structurallyEquivalent(member2) }
        }
    }
    
    /**
     * Merge two intersection types, combining their members.
     */
    fun merge(intersection1: IntersectionType, intersection2: IntersectionType): IntersectionType {
        val combinedMembers = intersection1.members + intersection2.members
        return IntersectionType.create(combinedMembers) as IntersectionType
    }
    
    /**
     * Extract all type variables from an intersection type.
     */
    fun extractTypeVariables(intersectionType: IntersectionType): Set<TypeVariable> {
        return intersectionType.members.flatMap { it.freeTypeVariables() }.toSet()
    }
    
    /**
     * Check if an intersection type contains only concrete types (no type variables).
     */
    fun isConcrete(intersectionType: IntersectionType): Boolean {
        return extractTypeVariables(intersectionType).isEmpty()
    }
    
    /**
     * Check if an intersection type is satisfiable (not bottom/empty).
     * An intersection is unsatisfiable if it contains contradictory constraints.
     */
    fun isSatisfiable(intersectionType: IntersectionType): Boolean {
        // Basic check: intersection of primitive types with different values is unsatisfiable
        val primitives = intersectionType.members.filterIsInstance<PrimitiveType>()
        if (primitives.size > 1) {
            // Multiple different primitive types cannot be satisfied simultaneously
            return false
        }
        
        val literals = intersectionType.members.filterIsInstance<LiteralStringType>()
        if (literals.size > 1) {
            // Multiple different string literals cannot be satisfied simultaneously
            return false
        }
        
        // For now, assume other combinations are satisfiable
        // TODO: Add more sophisticated satisfiability checking
        return true
    }
    
    /**
     * Find the most specific types in an intersection (remove redundant supertypes).
     */
    fun findMostSpecific(intersectionType: IntersectionType): Set<Type> {
        val members = intersectionType.members.toList()
        val mostSpecific = mutableSetOf<Type>()
        
        for (candidate in members) {
            val isRedundant = members.any { other ->
                other != candidate && isMoreSpecificThan(other, candidate)
            }
            
            if (!isRedundant) {
                mostSpecific.add(candidate)
            }
        }
        
        return mostSpecific
    }
    
    /**
     * Check if one type is more specific than another.
     * This is a simplified implementation for basic type relationships.
     */
    private fun isMoreSpecificThan(specific: Type, general: Type): Boolean {
        return when {
            specific is LiteralStringType && general == Types.String -> true
            specific is IntersectionType && specific.contains(general) -> true
            // TODO: Add more sophisticated specificity relationships
            else -> false
        }
    }
    
    /**
     * Get the upper bounds of an intersection type.
     * These are the types that the intersection must be a subtype of.
     */
    fun getUpperBounds(intersectionType: IntersectionType): Set<Type> {
        // For intersection types, all members are upper bounds
        return intersectionType.members.toSet()
    }
    
    /**
     * Check if an intersection represents a valid type constraint.
     * Invalid constraints include contradictory requirements.
     */
    fun isValidConstraint(intersectionType: IntersectionType): Boolean {
        return isSatisfiable(intersectionType) && intersectionType.members.size >= 2
    }
} 