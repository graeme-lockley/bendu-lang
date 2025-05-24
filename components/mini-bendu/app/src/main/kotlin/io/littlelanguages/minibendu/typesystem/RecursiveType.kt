package io.littlelanguages.minibendu.typesystem

/**
 * Recursive type representation.
 * 
 * A recursive type μα.T represents a type T that may contain
 * recursive references to itself through the variable α.
 * 
 * Examples:
 * - List = μα.("Nil" | {value: Int, next: α})
 * - Tree = μα.({value: Int} | {left: α, value: Int, right: α})
 */
data class RecursiveType(
    val name: String,
    val recursiveVar: TypeVariable,
    val body: Type
) : Type() {
    
    override fun structurallyEquivalent(other: Type): Boolean {
        return other is RecursiveType && 
               name == other.name &&
               body.structurallyEquivalent(other.body)
    }
    
    override fun freeTypeVariables(): Set<TypeVariable> {
        return body.freeTypeVariables() - recursiveVar
    }
    
    override fun toString(): String = "μ$recursiveVar.$body"
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RecursiveType) return false
        return name == other.name && recursiveVar == other.recursiveVar && body == other.body
    }
    
    override fun hashCode(): Int = 31 * (31 * name.hashCode() + recursiveVar.hashCode()) + body.hashCode()
} 