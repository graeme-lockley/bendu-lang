package io.littlelanguages.minibendu.typesystem

/**
 * Constraint Set for Mini-Bendu Type System
 * 
 * This module provides efficient collection and management of type constraints
 * during constraint-based type inference. ConstraintSet supports set operations,
 * simplification, consistency checking, and constraint propagation.
 * 
 * The constraint set system includes:
 * - Efficient constraint storage and lookup
 * - Set operations: union, intersection, difference
 * - Constraint simplification and redundancy elimination
 * - Consistency checking and early failure detection
 * - Incremental constraint addition and removal
 * - Constraint propagation rules
 * - Priority-based constraint ordering
 * - Variable-based constraint filtering
 */

/**
 * Represents an inconsistency found in a constraint set
 */
data class ConstraintInconsistency(
    val conflictingConstraints: Set<TypeConstraint>,
    val reason: String
)

/**
 * Immutable collection of type constraints with efficient operations
 * 
 * ConstraintSet provides a foundation for constraint-based type inference
 * by managing collections of constraints with operations for simplification,
 * consistency checking, and propagation.
 */
data class ConstraintSet private constructor(
    private val constraints: Set<TypeConstraint>
) {
    
    companion object {
        /**
         * Create an empty constraint set
         */
        fun empty(): ConstraintSet = ConstraintSet(emptySet())
        
        /**
         * Create a constraint set from a single constraint
         */
        fun of(constraint: TypeConstraint): ConstraintSet = ConstraintSet(setOf(constraint))
        
        /**
         * Create a constraint set from multiple constraints
         */
        fun of(vararg constraints: TypeConstraint): ConstraintSet = ConstraintSet(constraints.toSet())
        
        /**
         * Create a constraint set from a collection of constraints
         */
        fun of(constraints: Collection<TypeConstraint>): ConstraintSet = ConstraintSet(constraints.toSet())
    }
    
    /**
     * Check if the constraint set is empty
     */
    fun isEmpty(): Boolean = constraints.isEmpty()
    
    /**
     * Get the number of constraints in the set
     */
    fun size(): Int = constraints.size
    
    /**
     * Check if the set contains a specific constraint
     */
    fun contains(constraint: TypeConstraint): Boolean = constraints.contains(constraint)
    
    /**
     * Get all constraints in the set
     */
    fun all(): List<TypeConstraint> = constraints.toList()
    
    /**
     * Add a constraint to the set
     */
    fun add(constraint: TypeConstraint): ConstraintSet {
        return ConstraintSet(constraints + constraint)
    }
    
    /**
     * Remove a constraint from the set
     */
    fun remove(constraint: TypeConstraint): ConstraintSet {
        return ConstraintSet(constraints - constraint)
    }
    
    /**
     * Union this constraint set with another
     */
    fun union(other: ConstraintSet): ConstraintSet {
        return ConstraintSet(constraints + other.constraints)
    }
    
    /**
     * Intersect this constraint set with another
     */
    fun intersection(other: ConstraintSet): ConstraintSet {
        return ConstraintSet(constraints.intersect(other.constraints))
    }
    
    /**
     * Difference between this constraint set and another
     */
    fun difference(other: ConstraintSet): ConstraintSet {
        return ConstraintSet(constraints - other.constraints)
    }
    
    /**
     * Simplify constraints in the set by removing trivially satisfied constraints
     * and applying basic simplification rules
     */
    fun simplify(): ConstraintSet {
        val simplified = mutableSetOf<TypeConstraint>()
        
        for (constraint in constraints) {
            val simplifiedConstraints = constraint.simplify()
            simplified.addAll(simplifiedConstraints)
        }
        
        return ConstraintSet(simplified)
    }
    
    /**
     * Check if the constraint set is consistent (no conflicting constraints)
     */
    fun isConsistent(): Boolean = findInconsistency() == null
    
    /**
     * Check if the constraint set is inconsistent (has conflicting constraints)
     */
    fun isInconsistent(): Boolean = !isConsistent()
    
    /**
     * Find an inconsistency in the constraint set, if any
     */
    fun findInconsistency(): ConstraintInconsistency? {
        // Group constraints by the variables they constrain
        val variableConstraints = mutableMapOf<TypeVariable, MutableList<TypeConstraint>>()
        
        for (constraint in constraints) {
            for (variable in constraint.freeVariables()) {
                variableConstraints.getOrPut(variable) { mutableListOf() }.add(constraint)
            }
        }
        
        // Check for conflicting equality constraints on the same variable
        for (variable in variableConstraints.keys) {
            val constraintsForVar = variableConstraints[variable]!!
            val equalityConstraints = constraintsForVar.filterIsInstance<EqualityConstraint>()
            
            // Find constraints that directly bind this variable to different types
            val directBindings = mutableMapOf<Type, EqualityConstraint>()
            
            for (constraint in equalityConstraints) {
                val bindingType = when {
                    constraint.leftType == variable && constraint.rightType !is TypeVariable -> constraint.rightType
                    constraint.rightType == variable && constraint.leftType !is TypeVariable -> constraint.leftType
                    else -> null
                }
                
                if (bindingType != null) {
                    val existingBinding = directBindings[bindingType]
                    if (existingBinding == null) {
                        directBindings[bindingType] = constraint
                    } else {
                        // Found another constraint binding the same variable to the same type
                        // This is actually consistent, continue
                    }
                }
            }
            
            // Check if we have multiple different type bindings for the same variable
            if (directBindings.size > 1) {
                val conflicting = directBindings.values.toSet()
                val types = directBindings.keys.joinToString(", ")
                return ConstraintInconsistency(
                    conflicting,
                    "Variable $variable is constrained to multiple incompatible types: $types"
                )
            }
        }
        
        // Check for other forms of inconsistency could be added here
        // (e.g., circular subtyping relationships, impossible instance constraints)
        
        return null
    }
    
    /**
     * Apply constraint propagation rules to derive new constraints
     */
    fun propagate(): ConstraintSet {
        var current = this
        var changed = true
        
        // Apply propagation rules until fixpoint
        while (changed) {
            val previous = current
            current = current.applyPropagationRules()
            changed = current != previous
        }
        
        return current
    }
    
    /**
     * Apply a single round of constraint propagation rules
     */
    private fun applyPropagationRules(): ConstraintSet {
        val newConstraints = mutableSetOf<TypeConstraint>()
        newConstraints.addAll(constraints)
        
        // Transitivity for equality constraints: if a ~ b and b ~ c, then a ~ c
        val equalityConstraints = constraints.filterIsInstance<EqualityConstraint>()
        
        for (constraint1 in equalityConstraints) {
            for (constraint2 in equalityConstraints) {
                if (constraint1 != constraint2) {
                    val newConstraint = deriveTransitiveEquality(constraint1, constraint2)
                    if (newConstraint != null) {
                        newConstraints.add(newConstraint)
                    }
                }
            }
        }
        
        return ConstraintSet(newConstraints)
    }
    
    /**
     * Derive a transitive equality constraint if possible
     */
    private fun deriveTransitiveEquality(
        constraint1: EqualityConstraint, 
        constraint2: EqualityConstraint
    ): EqualityConstraint? {
        // Check if constraints share a type that allows transitivity
        return when {
            constraint1.rightType == constraint2.leftType -> 
                EqualityConstraint(constraint1.leftType, constraint2.rightType, ConstraintOrigin.INFERENCE)
            
            constraint1.leftType == constraint2.rightType -> 
                EqualityConstraint(constraint1.rightType, constraint2.leftType, ConstraintOrigin.INFERENCE)
            
            constraint1.rightType == constraint2.rightType -> 
                EqualityConstraint(constraint1.leftType, constraint2.leftType, ConstraintOrigin.INFERENCE)
            
            constraint1.leftType == constraint2.leftType -> 
                EqualityConstraint(constraint1.rightType, constraint2.rightType, ConstraintOrigin.INFERENCE)
            
            else -> null
        }
    }
    
    /**
     * Check if a constraint can be derived from this set
     */
    fun canDerive(constraint: TypeConstraint): Boolean {
        // Check if constraint is directly present
        if (contains(constraint)) return true
        
        // Check if constraint can be derived through propagation
        val propagated = propagate()
        return propagated.contains(constraint)
    }
    
    /**
     * Sort constraints by priority for solver efficiency
     */
    fun sortedByPriority(): ConstraintSet {
        val sorted = constraints.sortedBy { it.priority.ordinal }
        return ConstraintSet(sorted.toSet())
    }
    
    /**
     * Filter to only equality constraints
     */
    fun filterEquality(): ConstraintSet {
        return ConstraintSet(constraints.filter { it.isEquality() }.toSet())
    }
    
    /**
     * Filter to only subtyping constraints
     */
    fun filterSubtyping(): ConstraintSet {
        return ConstraintSet(constraints.filter { it.isSubtyping() }.toSet())
    }
    
    /**
     * Filter to only instance constraints
     */
    fun filterInstance(): ConstraintSet {
        return ConstraintSet(constraints.filter { it.isInstance() }.toSet())
    }
    
    /**
     * Get all constraints that involve a specific type variable
     */
    fun getConstraintsInvolving(variable: TypeVariable): List<TypeConstraint> {
        return constraints.filter { it.involvesVariable(variable) }
    }
    
    /**
     * Apply a substitution to all constraints in the set
     */
    fun apply(substitution: Substitution): ConstraintSet {
        val substituted = constraints.map { it.apply(substitution) }.toSet()
        return ConstraintSet(substituted)
    }
    
    /**
     * Get all free variables mentioned in any constraint in the set
     */
    fun freeVariables(): Set<TypeVariable> {
        return constraints.flatMap { it.freeVariables() }.toSet()
    }
    
    /**
     * Check if any constraints have the given origin
     */
    fun hasOrigin(origin: ConstraintOrigin): Boolean {
        return constraints.any { it.origin == origin }
    }
    
    /**
     * Filter constraints by origin
     */
    fun filterByOrigin(origin: ConstraintOrigin): ConstraintSet {
        return ConstraintSet(constraints.filter { it.origin == origin }.toSet())
    }
    
    /**
     * Get constraints with specific priority
     */
    fun filterByPriority(priority: ConstraintPriority): ConstraintSet {
        return ConstraintSet(constraints.filter { it.priority == priority }.toSet())
    }
    
    override fun toString(): String {
        if (constraints.isEmpty()) {
            return "∅"
        }
        return constraints.joinToString(", ", "{", "}") { it.toString() }
    }
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ConstraintSet) return false
        return constraints == other.constraints
    }
    
    override fun hashCode(): Int {
        return constraints.hashCode()
    }
}
