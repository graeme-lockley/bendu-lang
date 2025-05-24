package io.littlelanguages.minibendu.typesystem

import java.util.concurrent.atomic.AtomicInteger

/**
 * Represents a type variable in the type system.
 * Type variables are used for polymorphism and type inference.
 * Each type variable has a unique ID and an optional level for rank-n polymorphism.
 */
class TypeVariable private constructor(
    val id: Int,
    val level: Int = 0
) : Type() {
    
    companion object {
        private val nextId = AtomicInteger(0)
        
        /**
         * Creates a fresh type variable with a unique ID.
         */
        fun fresh(level: Int = 0): TypeVariable {
            return TypeVariable(nextId.getAndIncrement(), level)
        }
    }
    
    override fun freeTypeVariables(): Set<TypeVariable> {
        return setOf(this)
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TypeVariable) return false
        return id == other.id
    }
    
    override fun hashCode(): Int {
        return id
    }
    
    override fun toString(): String {
        return if (level == 0) {
            "τ$id"
        } else {
            "τ$id@$level"
        }
    }
    
    override fun structurallyEquivalent(other: Type): Boolean {
        return this == other
    }
}
