package io.littlelanguages.minibendu.typesystem

/**
 * Type Constraint Infrastructure for Mini-Bendu Type System
 * 
 * This module provides the foundation for constraint-based type inference,
 * supporting equality constraints, subtyping constraints, and instance constraints.
 * Constraints are used to represent relationships between types that must be
 * satisfied during unification and type checking.
 * 
 * The constraint system includes:
 * - TypeConstraint: Base class for all constraints
 * - EqualityConstraint: T1 ~ T2 (types must be unified)
 * - SubtypingConstraint: T1 <: T2 (T1 is a subtype of T2)
 * - InstanceConstraint: T : C (type T is an instance of type class C)
 * 
 * Constraints support:
 * - Priority-based ordering for solver efficiency
 * - Substitution application
 * - Simplification and normalization
 * - Dependency tracking for constraint ordering
 */

/**
 * The origin of a constraint - tracks where/why the constraint was generated
 */
enum class ConstraintOrigin {
    UNIFICATION,    // From unification during type checking
    INFERENCE,      // From type inference rules
    SUBTYPING,      // From subtyping relationships
    TYPE_CLASS,     // From type class/interface requirements
    USER_ANNOTATION // From explicit type annotations
}

/**
 * Priority levels for constraint solving
 * Higher priority constraints are solved first for efficiency
 */
enum class ConstraintPriority {
    HIGH,    // Equality constraints - solve first
    MEDIUM,  // Subtyping constraints - solve after equalities
    LOW      // Instance constraints - solve last
}

/**
 * Base class for all type constraints
 * 
 * A constraint represents a relationship between types that must be satisfied
 * during type checking. Constraints are generated during type inference and
 * are solved by the constraint solver to determine the final types.
 */
sealed class TypeConstraint {
    abstract val origin: ConstraintOrigin
    abstract val priority: ConstraintPriority
    
    /**
     * Check if this is an equality constraint
     */
    open fun isEquality(): Boolean = false
    
    /**
     * Check if this is a subtyping constraint
     */
    open fun isSubtyping(): Boolean = false
    
    /**
     * Check if this is an instance constraint
     */
    open fun isInstance(): Boolean = false
    
    /**
     * Check if this constraint involves the given type variable
     */
    abstract fun involvesVariable(variable: TypeVariable): Boolean
    
    /**
     * Apply a substitution to this constraint, returning a new constraint
     */
    abstract fun apply(substitution: Substitution): TypeConstraint
    
    /**
     * Get all free type variables mentioned in this constraint
     */
    abstract fun freeVariables(): Set<TypeVariable>
    
    /**
     * Simplify this constraint, potentially eliminating it or breaking it into simpler constraints
     * Returns a list of constraints (empty if the constraint is trivially satisfied)
     */
    abstract fun simplify(): List<TypeConstraint>
    
    /**
     * Check if this constraint depends on another constraint
     * (shares type variables that could be affected by solving the other constraint)
     */
    fun dependsOn(other: TypeConstraint): Boolean {
        val ourVars = freeVariables()
        val otherVars = other.freeVariables()
        return ourVars.intersect(otherVars).isNotEmpty()
    }
}

/**
 * Equality constraint: T1 ~ T2
 * 
 * Represents that two types must be unified (made equal).
 * This is the most common type of constraint in type inference.
 * 
 * Examples:
 * - x : Int ~ x : String (type error)
 * - f(x) : a ~ f(x) : Int -> Bool (a must be Int -> Bool)
 */
data class EqualityConstraint(
    val leftType: Type,
    val rightType: Type,
    override val origin: ConstraintOrigin
) : TypeConstraint() {
    
    override val priority: ConstraintPriority = ConstraintPriority.HIGH
    
    override fun isEquality(): Boolean = true
    
    override fun involvesVariable(variable: TypeVariable): Boolean {
        return leftType.freeTypeVariables().contains(variable) ||
               rightType.freeTypeVariables().contains(variable)
    }
    
    override fun apply(substitution: Substitution): TypeConstraint {
        return EqualityConstraint(
            substitution.apply(leftType),
            substitution.apply(rightType),
            origin
        )
    }
    
    override fun freeVariables(): Set<TypeVariable> {
        return leftType.freeTypeVariables() + rightType.freeTypeVariables()
    }
    
    override fun simplify(): List<TypeConstraint> {
        // If both types are structurally equivalent, the constraint is satisfied
        return if (leftType.structurallyEquivalent(rightType)) {
            emptyList() // Constraint is trivially satisfied
        } else {
            listOf(this) // Cannot be simplified further
        }
    }
    
    override fun toString(): String = "$leftType ~ $rightType"
}

/**
 * Subtyping constraint: T1 <: T2
 * 
 * Represents that T1 must be a subtype of T2.
 * Used for structural subtyping, especially with records and functions.
 * 
 * Examples:
 * - {x: Int, y: String} <: {x: Int} (width subtyping)
 * - Int -> String <: Int -> Any (covariant return type)
 * - (Any) -> Int <: (Int) -> Int (contravariant parameter type)
 */
data class SubtypingConstraint(
    val subtype: Type,
    val supertype: Type,
    override val origin: ConstraintOrigin
) : TypeConstraint() {
    
    override val priority: ConstraintPriority = ConstraintPriority.MEDIUM
    
    override fun isSubtyping(): Boolean = true
    
    override fun involvesVariable(variable: TypeVariable): Boolean {
        return subtype.freeTypeVariables().contains(variable) ||
               supertype.freeTypeVariables().contains(variable)
    }
    
    override fun apply(substitution: Substitution): TypeConstraint {
        return SubtypingConstraint(
            substitution.apply(subtype),
            substitution.apply(supertype),
            origin
        )
    }
    
    override fun freeVariables(): Set<TypeVariable> {
        return subtype.freeTypeVariables() + supertype.freeTypeVariables()
    }
    
    override fun simplify(): List<TypeConstraint> {
        // If subtype is structurally equivalent to supertype, constraint is satisfied
        if (subtype.structurallyEquivalent(supertype)) {
            return emptyList()
        }
        
        // Check for obvious subtyping relationships
        when {
            // Any type is a subtype of itself
            subtype == supertype -> return emptyList()
            
            // Record width subtyping: {a: A, b: B} <: {a: A}
            subtype is RecordType && supertype is RecordType -> {
                // Check if all fields in supertype exist in subtype with compatible types
                val canBeSubtype = supertype.fields.all { (name, superFieldType) ->
                    val subFieldType = subtype.fields[name]
                    subFieldType != null && subFieldType.structurallyEquivalent(superFieldType)
                }
                
                if (canBeSubtype && subtype.fields.size >= supertype.fields.size) {
                    return emptyList() // Subtyping relationship is satisfied
                }
            }
            
            // Function subtyping: (A1) -> B1 <: (A2) -> B2 
            // iff A2 <: A1 (contravariant) and B1 <: B2 (covariant)
            subtype is FunctionType && supertype is FunctionType -> {
                return listOf(
                    SubtypingConstraint(supertype.domain, subtype.domain, origin), // Contravariant
                    SubtypingConstraint(subtype.codomain, supertype.codomain, origin) // Covariant
                )
            }
        }
        
        return listOf(this) // Cannot be simplified further
    }
    
    override fun toString(): String = "$subtype <: $supertype"
}

/**
 * Instance constraint: T : C
 * 
 * Represents that type T must be an instance of type class/interface C.
 * Used for ad-hoc polymorphism and type class constraints.
 * 
 * Examples:
 * - a : Comparable (type variable 'a' must support comparison)
 * - List[a] : Functor (List must be a functor)
 * - {x: Int} : Printable (record must be printable)
 */
data class InstanceConstraint(
    val type: Type,
    val typeClass: String,
    override val origin: ConstraintOrigin
) : TypeConstraint() {
    
    override val priority: ConstraintPriority = ConstraintPriority.LOW
    
    override fun isInstance(): Boolean = true
    
    override fun involvesVariable(variable: TypeVariable): Boolean {
        return type.freeTypeVariables().contains(variable)
    }
    
    override fun apply(substitution: Substitution): TypeConstraint {
        return InstanceConstraint(
            substitution.apply(type),
            typeClass,
            origin
        )
    }
    
    override fun freeVariables(): Set<TypeVariable> {
        return type.freeTypeVariables()
    }
    
    override fun simplify(): List<TypeConstraint> {
        // Instance constraints generally cannot be simplified without knowledge
        // of the type class hierarchy and instance declarations
        
        // However, we can check for some built-in type classes
        when (typeClass) {
            "Printable" -> {
                // All primitive types are printable
                if (type is PrimitiveType) {
                    return emptyList() // Satisfied
                }
            }
            
            "Comparable" -> {
                // Primitive types like Int, String are comparable
                if (type is PrimitiveType && type.name in setOf("Int", "String", "Bool")) {
                    return emptyList() // Satisfied
                }
            }
        }
        
        return listOf(this) // Cannot be simplified further
    }
    
    override fun toString(): String = "$type : $typeClass"
}
