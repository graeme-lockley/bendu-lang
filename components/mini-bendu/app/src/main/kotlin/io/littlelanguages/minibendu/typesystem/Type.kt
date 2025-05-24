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
        
        // Both records must have the same "openness" - both open or both closed
        if ((rowVar != null) != (other.rowVar != null)) {
            return false // One is open, one is closed - not structurally equivalent
        }
        
        // For open records, we need the same fields and the same row variables
        if (rowVar != null && other.rowVar != null) {
            // Both are open - they're equivalent only if they have the same fields AND same row variables
            return fields.keys == other.fields.keys && rowVar == other.rowVar
        }
        
        // For closed records, field sets must be identical
        return fields.keys == other.fields.keys
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
 * Union types (A | B)
 */
class UnionType(val alternatives: Set<Type>) : Type() {
    init {
        require(alternatives.size >= 2) { "Union type must have at least 2 alternatives" }
    }
    
    override fun structurallyEquivalent(other: Type): Boolean {
        return other is UnionType && alternatives.size == other.alternatives.size &&
               alternatives.all { alt -> other.alternatives.any { it.structurallyEquivalent(alt) } }
    }
    
    override fun freeTypeVariables(): Set<TypeVariable> {
        return alternatives.flatMap { it.freeTypeVariables() }.toSet()
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