package io.littlelanguages.minibendu.typesystem

/**
 * Constraint Solver for Mini-Bendu Type System
 * 
 * This module provides constraint resolution through unification and subtyping.
 * The constraint solver takes a set of constraints and attempts to find a
 * substitution that satisfies all constraints.
 * 
 * Features:
 * - Unification-based constraint solving
 * - Priority-based constraint ordering
 * - Occurs check for infinite type prevention
 * - Detailed error reporting with source locations
 * - Performance optimizations for large constraint sets
 */

/**
 * Result of constraint solving
 */
sealed class ConstraintSolverResult {
    /**
     * Successful constraint solving with resulting substitution
     */
    data class Success(val substitution: Substitution) : ConstraintSolverResult() {
        fun isSuccess(): Boolean = true
        fun isFailure(): Boolean = false
    }
    
    /**
     * Failed constraint solving with error message
     */
    data class Failure(val error: String) : ConstraintSolverResult() {
        fun isSuccess(): Boolean = false
        fun isFailure(): Boolean = true
    }
}

/**
 * Constraint solver that resolves type constraints through unification
 */
class ConstraintSolver {
    
    /**
     * Solve a set of constraints, returning either a successful substitution
     * or an error message.
     */
    fun solve(constraints: ConstraintSet): ConstraintSolverResult {
        return try {
            val substitution = solveConstraints(constraints)
            ConstraintSolverResult.Success(substitution)
        } catch (e: UnificationException) {
            ConstraintSolverResult.Failure(e.message ?: "Constraint solving failed")
        } catch (e: Exception) {
            ConstraintSolverResult.Failure("Internal error during constraint solving: ${e.message}")
        }
    }
    
    /**
     * Internal constraint solving implementation
     */
    private fun solveConstraints(constraints: ConstraintSet): Substitution {
        var currentSubstitution = Substitution.empty
        val remainingConstraints = constraints.all().toMutableList()
        
        // Sort constraints by priority (equality first, then subtyping, then instance)
        remainingConstraints.sortBy { it.priority.ordinal }
        
        while (remainingConstraints.isNotEmpty()) {
            val constraint = remainingConstraints.removeAt(0)
            
            // Apply current substitution to the constraint
            val substitutedConstraint = constraint.applySubstitution(currentSubstitution)
            
            // Try to solve this constraint
            val newSubstitution = when (substitutedConstraint) {
                is EqualityConstraint -> solveEqualityConstraint(substitutedConstraint)
                is SubtypingConstraint -> solveSubtypingConstraint(substitutedConstraint)
                is InstanceConstraint -> solveInstanceConstraint(substitutedConstraint)
                else -> throw UnificationException("Unknown constraint type: ${substitutedConstraint::class.simpleName}")
            }
            
            // Compose the new substitution with the current one
            currentSubstitution = newSubstitution.compose(currentSubstitution)
            
            // Apply the new substitution to all remaining constraints
            for (i in remainingConstraints.indices) {
                remainingConstraints[i] = remainingConstraints[i].applySubstitution(newSubstitution)
            }
        }
        
        return currentSubstitution
    }
    
    /**
     * Solve an equality constraint through unification
     */
    private fun solveEqualityConstraint(constraint: EqualityConstraint): Substitution {
        return unify(constraint.type1, constraint.type2, constraint.sourceLocation)
    }
    
    /**
     * Solve a subtyping constraint
     */
    private fun solveSubtypingConstraint(constraint: SubtypingConstraint): Substitution {
        // For now, implement subtyping as equality for simple types
        // More sophisticated subtyping will be added in later tasks
        return when {
            // Trivial case: same types
            constraint.subtype.structurallyEquivalent(constraint.supertype) -> {
                Substitution.empty
            }
            
            // Record width subtyping: {a: A, b: B} <: {a: A}
            constraint.subtype is RecordType && constraint.supertype is RecordType -> {
                solveRecordSubtyping(constraint.subtype, constraint.supertype, constraint.sourceLocation)
            }
            
            // Function subtyping: (A1) -> B1 <: (A2) -> B2 iff A2 <: A1 and B1 <: B2
            constraint.subtype is FunctionType && constraint.supertype is FunctionType -> {
                solveFunctionSubtyping(constraint.subtype, constraint.supertype, constraint.sourceLocation)
            }
            
            // Variables can be unified with their supertypes
            constraint.subtype is TypeVariable -> {
                unify(constraint.subtype, constraint.supertype, constraint.sourceLocation)
            }
            
            constraint.supertype is TypeVariable -> {
                unify(constraint.subtype, constraint.supertype, constraint.sourceLocation)
            }
            
            else -> {
                throw UnificationException(
                    "Cannot establish subtyping relationship ${constraint.subtype} <: ${constraint.supertype}" +
                    if (constraint.sourceLocation != null) " at ${constraint.sourceLocation}" else ""
                )
            }
        }
    }
    
    /**
     * Solve record subtyping constraint
     */
    private fun solveRecordSubtyping(subtype: RecordType, supertype: RecordType, location: SourceLocation?): Substitution {
        var substitution = Substitution.empty
        
        // Check that all fields in supertype exist in subtype with compatible types
        for ((fieldName, supertypeFieldType) in supertype.fields) {
            val subtypeFieldType = subtype.fields[fieldName]
                ?: throw UnificationException(
                    "Missing field '$fieldName' in record subtyping" +
                    if (location != null) " at $location" else ""
                )
            
            // Field types must be unifiable (for now, using equality)
            val fieldSubstitution = unify(subtypeFieldType, supertypeFieldType, location)
            substitution = fieldSubstitution.compose(substitution)
        }
        
        return substitution
    }
    
    /**
     * Solve function subtyping constraint: (A1) -> B1 <: (A2) -> B2
     */
    private fun solveFunctionSubtyping(subtype: FunctionType, supertype: FunctionType, location: SourceLocation?): Substitution {
        // Function subtyping is contravariant in parameters and covariant in results
        // (A1) -> B1 <: (A2) -> B2 iff A2 <: A1 and B1 <: B2
        
        val paramSubstitution = unify(supertype.domain, subtype.domain, location) // Contravariant
        val resultSubstitution = unify(subtype.codomain, supertype.codomain, location) // Covariant
        
        return resultSubstitution.compose(paramSubstitution)
    }
    
    /**
     * Solve instance constraint (placeholder for future type class support)
     */
    private fun solveInstanceConstraint(constraint: InstanceConstraint): Substitution {
        // For now, just check built-in type classes
        when (constraint.typeClass) {
            "Printable" -> {
                // All types are printable for now
                return Substitution.empty
            }
            "Comparable" -> {
                // Only primitive types are comparable for now
                if (constraint.type is PrimitiveType) {
                    return Substitution.empty
                } else {
                    throw UnificationException(
                        "Type ${constraint.type} is not comparable" +
                        if (constraint.sourceLocation != null) " at ${constraint.sourceLocation}" else ""
                    )
                }
            }
            else -> {
                throw UnificationException(
                    "Unknown type class: ${constraint.typeClass}" +
                    if (constraint.sourceLocation != null) " at ${constraint.sourceLocation}" else ""
                )
            }
        }
    }
    
    /**
     * Unify two types, producing a substitution that makes them equal
     */
    private fun unify(type1: Type, type2: Type, location: SourceLocation?): Substitution {
        // Use the sophisticated unification algorithm that handles row polymorphism
        val result = Unification.unify(type1, type2)
        
        if (result.isSuccess()) {
            return result.getSubstitution()
        } else {
            throw UnificationException(
                result.getError() + 
                if (location != null) " at $location" else ""
            )
        }
    }
    

}

 