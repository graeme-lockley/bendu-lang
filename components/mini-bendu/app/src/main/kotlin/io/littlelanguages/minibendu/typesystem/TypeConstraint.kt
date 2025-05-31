package io.littlelanguages.minibendu.typesystem

import io.littlelanguages.minibendu.MatchExpr

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
     * Optional source location information for error reporting.
     */
    abstract val sourceLocation: SourceLocation?
    
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
     * Get all type variables mentioned in this constraint.
     */
    abstract fun typeVariables(): Set<TypeVariable>
    
    /**
     * Apply a substitution to this constraint, replacing type variables.
     */
    abstract fun applySubstitution(substitution: Substitution): TypeConstraint
    
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
    val type1: Type,
    val type2: Type,
    override val sourceLocation: SourceLocation? = null,
    override val origin: ConstraintOrigin = ConstraintOrigin.INFERENCE
) : TypeConstraint() {
    
    override val priority: ConstraintPriority = ConstraintPriority.HIGH
    
    override fun isEquality(): Boolean = true
    
    override fun involvesVariable(variable: TypeVariable): Boolean {
        return type1.freeTypeVariables().contains(variable) ||
               type2.freeTypeVariables().contains(variable)
    }
    
    override fun typeVariables(): Set<TypeVariable> {
        return type1.freeTypeVariables() + type2.freeTypeVariables()
    }
    
    override fun applySubstitution(substitution: Substitution): EqualityConstraint {
        return EqualityConstraint(
            substitution.apply(type1),
            substitution.apply(type2),
            sourceLocation,
            origin
        )
    }
    
    override fun freeVariables(): Set<TypeVariable> {
        return type1.freeTypeVariables() + type2.freeTypeVariables()
    }
    
    override fun simplify(): List<TypeConstraint> {
        // If both types are structurally equivalent, the constraint is satisfied
        return if (type1.structurallyEquivalent(type2)) {
            emptyList() // Constraint is trivially satisfied
        } else {
            listOf(this) // Cannot be simplified further
        }
    }
    
    override fun toString(): String = "$type1 ~ $type2"
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
    override val sourceLocation: SourceLocation? = null,
    override val origin: ConstraintOrigin = ConstraintOrigin.SUBTYPING
) : TypeConstraint() {
    
    override val priority: ConstraintPriority = ConstraintPriority.MEDIUM
    
    override fun isSubtyping(): Boolean = true
    
    override fun involvesVariable(variable: TypeVariable): Boolean {
        return subtype.freeTypeVariables().contains(variable) ||
               supertype.freeTypeVariables().contains(variable)
    }
    
    override fun typeVariables(): Set<TypeVariable> {
        return subtype.freeTypeVariables() + supertype.freeTypeVariables()
    }
    
    override fun applySubstitution(substitution: Substitution): SubtypingConstraint {
        return SubtypingConstraint(
            substitution.apply(subtype),
            substitution.apply(supertype),
            sourceLocation,
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
                    SubtypingConstraint(supertype.domain, subtype.domain, sourceLocation), // Contravariant
                    SubtypingConstraint(subtype.codomain, supertype.codomain, sourceLocation) // Covariant
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
    override val origin: ConstraintOrigin,
    override val sourceLocation: SourceLocation? = null
) : TypeConstraint() {
    
    override val priority: ConstraintPriority = ConstraintPriority.LOW
    
    override fun isInstance(): Boolean = true
    
    override fun involvesVariable(variable: TypeVariable): Boolean {
        return type.freeTypeVariables().contains(variable)
    }
    
    override fun typeVariables(): Set<TypeVariable> {
        return type.freeTypeVariables()
    }
    
    override fun applySubstitution(substitution: Substitution): InstanceConstraint {
        return InstanceConstraint(
            substitution.apply(type),
            typeClass,
            origin,
            sourceLocation
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

/**
 * Simple source location information for error reporting.
 */
data class SourceLocation(
    val line: Int,
    val column: Int,
    val filename: String? = null
) {
    override fun toString(): String {
        return if (filename != null) "$filename:$line:$column" else "$line:$column"
    }
}

/**
 * Record type constraint: T must be a record type.
 * 
 * Used to ensure that a type is a record type, particularly
 * for spread operations where we need to merge record fields.
 */
data class RecordTypeConstraint(
    val type: Type,
    override val sourceLocation: SourceLocation? = null,
    override val origin: ConstraintOrigin = ConstraintOrigin.INFERENCE
) : TypeConstraint() {
    
    override val priority: ConstraintPriority = ConstraintPriority.HIGH
    
    override fun involvesVariable(variable: TypeVariable): Boolean {
        return type.freeTypeVariables().contains(variable)
    }
    
    override fun typeVariables(): Set<TypeVariable> {
        return type.freeTypeVariables()
    }
    
    override fun applySubstitution(substitution: Substitution): RecordTypeConstraint {
        return RecordTypeConstraint(
            substitution.apply(type),
            sourceLocation,
            origin
        )
    }
    
    override fun freeVariables(): Set<TypeVariable> {
        return type.freeTypeVariables()
    }
    
    override fun simplify(): List<TypeConstraint> {
        // If type is already a record type, constraint is satisfied
        return if (type is RecordType) {
            emptyList()
        } else {
            listOf(this) // Cannot be simplified further
        }
    }
    
    override fun toString(): String = "$type must be record"
}

/**
 * Merge constraint: result = merge(spreadTypes, explicitFields)
 * 
 * Represents that the result type should be a record containing
 * all fields from the spread types plus the explicit fields.
 * Later fields override earlier ones in case of conflicts.
 */
data class MergeConstraint(
    val resultType: Type,
    val spreadTypes: List<Type>,
    val explicitFields: Map<String, Type>,
    override val sourceLocation: SourceLocation? = null,
    override val origin: ConstraintOrigin = ConstraintOrigin.INFERENCE
) : TypeConstraint() {
    
    override val priority: ConstraintPriority = ConstraintPriority.MEDIUM
    
    override fun involvesVariable(variable: TypeVariable): Boolean {
        return resultType.freeTypeVariables().contains(variable) ||
               spreadTypes.any { it.freeTypeVariables().contains(variable) } ||
               explicitFields.values.any { it.freeTypeVariables().contains(variable) }
    }
    
    override fun typeVariables(): Set<TypeVariable> {
        return resultType.freeTypeVariables() +
               spreadTypes.flatMap { it.freeTypeVariables() }.toSet() +
               explicitFields.values.flatMap { it.freeTypeVariables() }.toSet()
    }
    
    override fun applySubstitution(substitution: Substitution): MergeConstraint {
        return MergeConstraint(
            substitution.apply(resultType),
            spreadTypes.map { substitution.apply(it) },
            explicitFields.mapValues { (_, type) -> substitution.apply(type) },
            sourceLocation,
            origin
        )
    }
    
    override fun freeVariables(): Set<TypeVariable> {
        return resultType.freeTypeVariables() +
               spreadTypes.flatMap { it.freeTypeVariables() }.toSet() +
               explicitFields.values.flatMap { it.freeTypeVariables() }.toSet()
    }
    
    override fun simplify(): List<TypeConstraint> {
        // Can't simplify without knowing the concrete types
        return listOf(this)
    }
    
    override fun toString(): String = "$resultType = merge(${spreadTypes.joinToString(", ")}, {${explicitFields.entries.joinToString(", ") { "${it.key}: ${it.value}" }}})"
}

/**
 * Union compatibility constraint: T1 ~ T2 | ...
 * 
 * Represents that type T1 must be compatible with type T2, meaning T1 can be
 * unified with T2 or T1 can be part of a union type that includes T2.
 * This is used for discriminated union pattern matching where the scrutinee type
 * needs to accommodate multiple different pattern types.
 * 
 * Examples:
 * - In pattern matching with { typ = "user", ... } and { typ = "admin", ... },
 *   the scrutinee type should be compatible with both patterns
 * - Allows the constraint solver to infer union types like:
 *   { typ: "user" | "admin", ... }
 */
data class UnionCompatibilityConstraint(
    val scrutineeType: Type,
    val patternType: Type,
    override val sourceLocation: SourceLocation? = null,
    override val origin: ConstraintOrigin = ConstraintOrigin.INFERENCE
) : TypeConstraint() {
    
    override val priority: ConstraintPriority = ConstraintPriority.MEDIUM
    
    override fun involvesVariable(variable: TypeVariable): Boolean {
        return scrutineeType.freeTypeVariables().contains(variable) ||
               patternType.freeTypeVariables().contains(variable)
    }
    
    override fun typeVariables(): Set<TypeVariable> {
        return scrutineeType.freeTypeVariables() + patternType.freeTypeVariables()
    }
    
    override fun applySubstitution(substitution: Substitution): UnionCompatibilityConstraint {
        return UnionCompatibilityConstraint(
            substitution.apply(scrutineeType),
            substitution.apply(patternType),
            sourceLocation,
            origin
        )
    }
    
    override fun freeVariables(): Set<TypeVariable> {
        return scrutineeType.freeTypeVariables() + patternType.freeTypeVariables()
    }
    
    override fun simplify(): List<TypeConstraint> {
        // If the types are already compatible (structurally equivalent), constraint is satisfied
        if (scrutineeType.structurallyEquivalent(patternType)) {
            return emptyList()
        }
        
        // If scrutinee is already a union type, check if pattern type is compatible
        if (scrutineeType is UnionType) {
            val isCompatible = scrutineeType.alternatives.any { unionMember ->
                unionMember.structurallyEquivalent(patternType) || 
                canUnifyTypes(unionMember, patternType)
            }
            if (isCompatible) {
                return emptyList() // Already compatible
            }
        }
        
        // If pattern is a union type, check if scrutinee is compatible with any member
        if (patternType is UnionType) {
            val isCompatible = patternType.alternatives.any { unionMember ->
                scrutineeType.structurallyEquivalent(unionMember) ||
                canUnifyTypes(scrutineeType, unionMember)
            }
            if (isCompatible) {
                return emptyList() // Already compatible
            }
        }
        
        // Cannot be simplified further - let the constraint solver handle it
        return listOf(this)
    }
    
    /**
     * Check if two types can potentially be unified.
     * This is a conservative check for compatibility.
     */
    private fun canUnifyTypes(type1: Type, type2: Type): Boolean {
        return when {
            // Type variables can unify with anything
            type1 is TypeVariable || type2 is TypeVariable -> true
            
            // Same concrete types can unify
            type1::class == type2::class -> {
                when (type1) {
                    is RecordType -> {
                        if (type2 !is RecordType) return false
                        // Records can unify if they have compatible field structures
                        val commonFields = type1.fields.keys.intersect(type2.fields.keys)
                        commonFields.all { fieldName ->
                            val field1 = type1.fields[fieldName]!!
                            val field2 = type2.fields[fieldName]!!
                            canUnifyTypes(field1, field2)
                        }
                    }
                    is FunctionType -> {
                        if (type2 !is FunctionType) return false
                        canUnifyTypes(type1.domain, type2.domain) && 
                        canUnifyTypes(type1.codomain, type2.codomain)
                    }
                    is TupleType -> {
                        if (type2 !is TupleType) return false
                        type1.elements.size == type2.elements.size &&
                        type1.elements.zip(type2.elements).all { (elem1, elem2) ->
                            canUnifyTypes(elem1, elem2)
                        }
                    }
                    else -> type1.structurallyEquivalent(type2)
                }
            }
            
            else -> false
        }
    }
    
    override fun toString(): String = "$scrutineeType ~∪ $patternType"
}

/**
 * Exhaustiveness constraint: match expression must be exhaustive
 * 
 * Represents that a match expression must exhaustively cover all possible values
 * of the scrutinee type. This constraint is checked after type inference to ensure
 * that pattern matching is complete.
 * 
 * Examples:
 * - match bool with True => ... | False => ... (exhaustive)
 * - match option with Some(x) => ... (non-exhaustive, missing None case)
 */
data class ExhaustivenessConstraint(
    val matchExpr: MatchExpr,
    val scrutineeType: Type,
    override val sourceLocation: SourceLocation? = null,
    override val origin: ConstraintOrigin = ConstraintOrigin.INFERENCE
) : TypeConstraint() {
    
    override val priority: ConstraintPriority = ConstraintPriority.LOW
    
    override fun involvesVariable(variable: TypeVariable): Boolean {
        return scrutineeType.freeTypeVariables().contains(variable)
    }
    
    override fun typeVariables(): Set<TypeVariable> {
        return scrutineeType.freeTypeVariables()
    }
    
    override fun applySubstitution(substitution: Substitution): ExhaustivenessConstraint {
        return ExhaustivenessConstraint(
            matchExpr,
            substitution.apply(scrutineeType),
            sourceLocation,
            origin
        )
    }
    
    override fun freeVariables(): Set<TypeVariable> {
        return scrutineeType.freeTypeVariables()
    }
    
    override fun simplify(): List<TypeConstraint> {
        // Exhaustiveness checking can only be done once the scrutinee type is fully resolved
        // If the scrutinee type still contains type variables, we can't check exhaustiveness yet
        if (scrutineeType.freeTypeVariables().isNotEmpty()) {
            return listOf(this) // Keep the constraint for later
        }
        
        // If the scrutinee type is fully resolved, the constraint solver should handle this
        return listOf(this)
    }
    
    override fun toString(): String = "exhaustive($matchExpr, $scrutineeType)"
}
