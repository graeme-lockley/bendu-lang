package io.littlelanguages.minibendu.typesystem

/**
 * Base class for all types in the mini-bendu type system.
 * This forms the foundation for structural typing with row polymorphism.
 */
sealed class Type {
    
    /**
     * Check if this type is structurally equivalent to another type.
     * Two types are structurally equivalent if they have the same structure,
     * regardless of nominal differences.
     */
    abstract fun structurallyEquivalent(other: Type): Boolean
    
    /**
     * Get all free type variables in this type.
     * Used for generalization and instantiation.
     */
    open fun freeTypeVariables(): Set<TypeVariable> = emptySet()
}

/**
 * Primitive types (Int, String, Bool, Unit)
 */
class PrimitiveType(val name: String) : Type() {
    override fun structurallyEquivalent(other: Type): Boolean {
        return other is PrimitiveType && name == other.name
    }
    
    override fun toString(): String = name
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PrimitiveType) return false
        return name == other.name
    }
    
    override fun hashCode(): Int = name.hashCode()
}

/**
 * String literal types ("success", "error", etc.)
 * Used for discriminated unions and typed enums
 */
class LiteralStringType(val value: String) : Type() {
    override fun structurallyEquivalent(other: Type): Boolean {
        return other is LiteralStringType && value == other.value
    }
    
    override fun freeTypeVariables(): Set<TypeVariable> = emptySet()
    
    override fun toString(): String = "\"$value\""
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LiteralStringType) return false
        return value == other.value
    }
    
    override fun hashCode(): Int = value.hashCode()
}

/**
 * Function types (A -> B)
 */
class FunctionType(val domain: Type, val codomain: Type) : Type() {
    override fun structurallyEquivalent(other: Type): Boolean {
        return other is FunctionType && 
               domain.structurallyEquivalent(other.domain) &&
               codomain.structurallyEquivalent(other.codomain)
    }
    
    override fun freeTypeVariables(): Set<TypeVariable> {
        return domain.freeTypeVariables() + codomain.freeTypeVariables()
    }
    
    override fun toString(): String = "($domain -> $codomain)"
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is FunctionType) return false
        return domain == other.domain && codomain == other.codomain
    }
    
    override fun hashCode(): Int = 31 * domain.hashCode() + codomain.hashCode()
}

/**
 * Record types { field1: Type1, field2: Type2, ... }
 * Supports row polymorphism through extension types
 */
class RecordType(val fields: Map<String, Type>, val rowVar: TypeVariable? = null) : Type() {
    override fun structurallyEquivalent(other: Type): Boolean {
        if (other !is RecordType) return false
        
        // Check if all our fields are compatible with the other record
        for ((name, type) in fields) {
            val otherType = other.fields[name] ?: return false
            if (!type.structurallyEquivalent(otherType)) return false
        }
        
        // Check if the other record has any fields we don't have
        for ((name, _) in other.fields) {
            if (name !in fields) return false
        }
        
        // Both records must have the same "openness" - both open or both closed
        // For structural equivalence, we don't require the same row variable identity,
        // just that both are open or both are closed
        return (rowVar != null) == (other.rowVar != null)
    }
    
    override fun freeTypeVariables(): Set<TypeVariable> {
        val fieldVars = fields.values.flatMap { it.freeTypeVariables() }.toSet()
        return if (rowVar != null) fieldVars + rowVar else fieldVars
    }
    
    override fun toString(): String {
        val fieldStr = fields.entries.joinToString(", ") { "${it.key}: ${it.value}" }
        val rowStr = if (rowVar != null) " | $rowVar" else ""
        return "{$fieldStr$rowStr}"
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RecordType) return false
        return fields == other.fields && rowVar == other.rowVar
    }
    
    override fun hashCode(): Int = 31 * fields.hashCode() + (rowVar?.hashCode() ?: 0)
}

/**
 * Tuple types (A, B, C)
 */
class TupleType(val elements: List<Type>) : Type() {
    override fun structurallyEquivalent(other: Type): Boolean {
        return other is TupleType && 
               elements.size == other.elements.size &&
               elements.zip(other.elements).all { (a, b) -> a.structurallyEquivalent(b) }
    }
    
    override fun freeTypeVariables(): Set<TypeVariable> {
        return elements.flatMap { it.freeTypeVariables() }.toSet()
    }
    
    override fun toString(): String = "(${elements.joinToString(", ")})"
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TupleType) return false
        return elements == other.elements
    }
    
    override fun hashCode(): Int = elements.hashCode()
}

/**
 * Intersection types (A & B)
 */
class IntersectionType(val members: Set<Type>) : Type() {
    init {
        require(members.size >= 2) { "Intersection type must have at least 2 members" }
    }
    
    companion object {
        /**
         * Create a normalized intersection type from a set of types.
         * Automatically handles flattening, deduplication, and simplification.
         */
        fun create(types: Set<Type>): Type {
            val normalized = normalize(types)
            return when {
                normalized.isEmpty() -> throw IllegalArgumentException("Cannot create intersection from empty set")
                normalized.size == 1 -> normalized.first()
                else -> IntersectionType(normalized)
            }
        }
        
        /**
         * Create an intersection from multiple types with automatic normalization.
         */
        fun of(vararg types: Type): Type = create(types.toSet())
        
        /**
         * Normalize a set of types for intersection creation by:
         * - Flattening nested intersections
         * - Removing duplicates
         * - Eliminating redundant types
         */
        private fun normalize(types: Set<Type>): Set<Type> {
            val result = mutableSetOf<Type>()
            
            for (type in types) {
                when (type) {
                    is IntersectionType -> {
                        // Flatten nested intersections
                        result.addAll(type.members)
                    }
                    else -> {
                        result.add(type)
                    }
                }
            }
            
            // Remove duplicates through Set semantics
            // TODO: Could add more sophisticated redundancy elimination
            // e.g., if we have both Int and (Int & String), keep only (Int & String)
            
            return result
        }
    }
    
    override fun structurallyEquivalent(other: Type): Boolean {
        return other is IntersectionType && members.size == other.members.size &&
               members.all { member -> other.members.any { it.structurallyEquivalent(member) } }
    }
    
    override fun freeTypeVariables(): Set<TypeVariable> {
        return members.flatMap { it.freeTypeVariables() }.toSet()
    }
    
    /**
     * Check if this intersection contains a specific type as a member.
     */
    fun contains(type: Type): Boolean {
        return members.any { it.structurallyEquivalent(type) }
    }
    
    /**
     * Check if this intersection is a subtype of another type.
     * An intersection (A & B) is a subtype of T if (A & B) <: T, which means
     * both A <: T and B <: T (since the intersection must satisfy both constraints).
     */
    fun isSubtypeOf(supertype: Type): Boolean {
        // An intersection is a subtype of a type if any of its members is a subtype
        // because the intersection represents a value that satisfies ALL constraints
        return members.any { member ->
            member.structurallyEquivalent(supertype) ||
            (supertype is IntersectionType && supertype.contains(member))
        }
    }
    
    /**
     * Check if a type is a supertype of this intersection.
     * A type T is a supertype of intersection (A & B) if T is a supertype of the intersection.
     * This means T must be a supertype of both A and B.
     */
    fun isSupertypeOf(subtype: Type): Boolean {
        // A type is a supertype of an intersection if it's a supertype of all members
        return when (subtype) {
            is IntersectionType -> subtype.members.all { subtypeMember ->
                members.any { member -> member.structurallyEquivalent(subtypeMember) }
            }
            else -> members.any { member -> member.structurallyEquivalent(subtype) }
        }
    }
    
    /**
     * Simplify this intersection by removing redundant members.
     * Returns a simplified type (might not be an intersection if simplified to one member).
     */
    fun simplify(): Type {
        val simplified = members.toSet() // For now, just ensure no duplicates
        
        return when {
            simplified.isEmpty() -> throw IllegalStateException("Intersection cannot be empty")
            simplified.size == 1 -> simplified.first()
            else -> IntersectionType(simplified)
        }
    }
    
    /**
     * Get the intersection of this intersection with another type.
     */
    fun intersect(other: Type): Type {
        val newMembers = when (other) {
            is IntersectionType -> members + other.members
            else -> members + other
        }
        return create(newMembers)
    }
    
    override fun toString(): String = members.joinToString(" & ")
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is IntersectionType) return false
        return members == other.members
    }
    
    override fun hashCode(): Int = members.hashCode()
}

/**
 * Union types (A | B)
 */
class UnionType(val alternatives: Set<Type>) : Type() {
    init {
        require(alternatives.size >= 2) { "Union type must have at least 2 alternatives" }
    }
    
    companion object {
        /**
         * Create a normalized union type from a set of types.
         * Automatically handles flattening, deduplication, and simplification.
         */
        fun create(types: Set<Type>): Type {
            val normalized = normalize(types)
            return when {
                normalized.isEmpty() -> throw IllegalArgumentException("Cannot create union from empty set")
                normalized.size == 1 -> normalized.first()
                else -> UnionType(normalized)
            }
        }
        
        /**
         * Create a union from multiple types with automatic normalization.
         */
        fun of(vararg types: Type): Type = create(types.toSet())
        
        /**
         * Normalize a set of types for union creation by:
         * - Flattening nested unions
         * - Removing duplicates
         * - Eliminating redundant types
         */
        private fun normalize(types: Set<Type>): Set<Type> {
            val result = mutableSetOf<Type>()
            
            for (type in types) {
                when (type) {
                    is UnionType -> {
                        // Flatten nested unions
                        result.addAll(type.alternatives)
                    }
                    else -> {
                        result.add(type)
                    }
                }
            }
            
            // Remove duplicates through Set semantics
            // TODO: Could add more sophisticated redundancy elimination
            // e.g., if we have both Int and (Int | String), keep only (Int | String)
            
            return result
        }
    }
    
    override fun structurallyEquivalent(other: Type): Boolean {
        return other is UnionType && alternatives.size == other.alternatives.size &&
               alternatives.all { alt -> other.alternatives.any { it.structurallyEquivalent(alt) } }
    }
    
    override fun freeTypeVariables(): Set<TypeVariable> {
        return alternatives.flatMap { it.freeTypeVariables() }.toSet()
    }
    
    /**
     * Check if this union contains a specific type as an alternative.
     */
    fun contains(type: Type): Boolean {
        return alternatives.any { it.structurallyEquivalent(type) }
    }
    
    /**
     * Check if this union is a subtype of another type.
     * A union (A | B) is a subtype of T if both A <: T and B <: T.
     */
    fun isSubtypeOf(supertype: Type): Boolean {
        return alternatives.all { alternative ->
            // Simplified subtype check - could be enhanced with proper subtype logic
            alternative.structurallyEquivalent(supertype) ||
            (supertype is UnionType && supertype.contains(alternative))
        }
    }
    
    /**
     * Check if a type is a subtype of this union.
     * A type T is a subtype of union (A | B) if T <: A or T <: B.
     */
    fun isSupertypeOf(subtype: Type): Boolean {
        return alternatives.any { alternative ->
            // Simplified subtype check
            subtype.structurallyEquivalent(alternative) ||
            (subtype is UnionType && subtype.isSubtypeOf(alternative))
        }
    }
    
    /**
     * Simplify this union by removing redundant alternatives.
     * Returns a simplified type (might not be a union if simplified to one alternative).
     */
    fun simplify(): Type {
        val simplified = alternatives.toSet() // For now, just ensure no duplicates
        
        return when {
            simplified.isEmpty() -> throw IllegalStateException("Union cannot be empty")
            simplified.size == 1 -> simplified.first()
            else -> UnionType(simplified)
        }
    }
    
    /**
     * Get the union of this union with another type.
     */
    fun union(other: Type): Type {
        val newAlternatives = when (other) {
            is UnionType -> alternatives + other.alternatives
            else -> alternatives + other
        }
        return create(newAlternatives)
    }
    
    override fun toString(): String = alternatives.joinToString(" | ")
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UnionType) return false
        return alternatives == other.alternatives
    }
    
    override fun hashCode(): Int = alternatives.hashCode()
}

/**
 * Type alias references (Name[A, B, ...])
 * Used to reference defined type aliases with optional type parameters
 */
class TypeAlias(val name: String, val typeArguments: List<Type> = emptyList()) : Type() {
    
    override fun structurallyEquivalent(other: Type): Boolean {
        return other is TypeAlias && 
               name == other.name && 
               typeArguments.size == other.typeArguments.size &&
               typeArguments.zip(other.typeArguments).all { (a, b) -> a.structurallyEquivalent(b) }
    }
    
    override fun freeTypeVariables(): Set<TypeVariable> {
        return typeArguments.flatMap { it.freeTypeVariables() }.toSet()
    }
    
    override fun toString(): String {
        return if (typeArguments.isEmpty()) {
            name
        } else {
            "$name[${typeArguments.joinToString(", ")}]"
        }
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TypeAlias) return false
        return name == other.name && typeArguments == other.typeArguments
    }
    
    override fun hashCode(): Int = 31 * name.hashCode() + typeArguments.hashCode()
}

// Predefined primitive types
object Types {
    val Int = PrimitiveType("Int")
    val String = PrimitiveType("String") 
    val Bool = PrimitiveType("Bool")
    val Unit = PrimitiveType("Unit")
    
    // Factory method for literal string types
    fun literal(value: String): LiteralStringType = LiteralStringType(value)
    
    // Common literal string types
    val Success = LiteralStringType("success")
    val Error = LiteralStringType("error")
    val Ok = LiteralStringType("ok")
}