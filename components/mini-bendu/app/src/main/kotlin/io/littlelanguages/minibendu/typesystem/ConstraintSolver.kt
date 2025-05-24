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
        return when {
            // Same types
            type1 == type2 -> Substitution.empty
            
            // Type variable unification
            type1 is TypeVariable -> unifyVariable(type1, type2, location)
            type2 is TypeVariable -> unifyVariable(type2, type1, location)
            
            // Function type unification
            type1 is FunctionType && type2 is FunctionType -> {
                val domainSubst = unify(type1.domain, type2.domain, location)
                val codomainSubst = unify(
                    domainSubst.apply(type1.codomain), 
                    domainSubst.apply(type2.codomain), 
                    location
                )
                codomainSubst.compose(domainSubst)
            }
            
            // Record type unification
            type1 is RecordType && type2 is RecordType -> {
                unifyRecords(type1, type2, location)
            }
            
            // Tuple type unification
            type1 is TupleType && type2 is TupleType -> {
                unifyTuples(type1, type2, location)
            }
            
            // Union type unification  
            type1 is UnionType && type2 is UnionType -> {
                unifyUnionTypes(type1, type2, location)
            }
            
            // Primitive types
            type1 is PrimitiveType && type2 is PrimitiveType -> {
                if (type1.name == type2.name) {
                    Substitution.empty
                } else {
                    throw UnificationException(
                        "Cannot unify primitive types ${type1.name} and ${type2.name}" +
                        if (location != null) " at $location" else ""
                    )
                }
            }
            
            else -> {
                throw UnificationException(
                    "Cannot unify types $type1 and $type2" +
                    if (location != null) " at $location" else ""
                )
            }
        }
    }
    
    /**
     * Unify a type variable with another type
     */
    private fun unifyVariable(variable: TypeVariable, type: Type, location: SourceLocation?): Substitution {
        // Occurs check: prevent infinite types
        if (type.freeTypeVariables().contains(variable)) {
            throw UnificationException(
                "Occurs check failed: cannot unify $variable with $type (would create infinite type)" +
                if (location != null) " at $location" else ""
            )
        }
        
        return Substitution.single(variable, type)
    }
    
    /**
     * Unify two record types
     */
    private fun unifyRecords(record1: RecordType, record2: RecordType, location: SourceLocation?): Substitution {
        // Records must have the same fields with unifiable types
        if (record1.fields.keys != record2.fields.keys) {
            throw UnificationException(
                "Cannot unify records with different field sets: ${record1.fields.keys} vs ${record2.fields.keys}" +
                if (location != null) " at $location" else ""
            )
        }
        
        var substitution = Substitution.empty
        for ((fieldName, fieldType1) in record1.fields) {
            val fieldType2 = record2.fields[fieldName]!!
            val fieldSubstitution = unify(
                substitution.apply(fieldType1), 
                substitution.apply(fieldType2), 
                location
            )
            substitution = fieldSubstitution.compose(substitution)
        }
        
        return substitution
    }
    
    /**
     * Unify two tuple types
     */
    private fun unifyTuples(tuple1: TupleType, tuple2: TupleType, location: SourceLocation?): Substitution {
        if (tuple1.elements.size != tuple2.elements.size) {
            throw UnificationException(
                "Cannot unify tuples of different sizes: ${tuple1.elements.size} vs ${tuple2.elements.size}" +
                if (location != null) " at $location" else ""
            )
        }
        
        var substitution = Substitution.empty
        for (i in tuple1.elements.indices) {
            val elementSubstitution = unify(
                substitution.apply(tuple1.elements[i]), 
                substitution.apply(tuple2.elements[i]), 
                location
            )
            substitution = elementSubstitution.compose(substitution)
        }
        
        return substitution
    }
    
    /**
     * Unify two union types
     */
    private fun unifyUnionTypes(union1: UnionType, union2: UnionType, location: SourceLocation?): Substitution {
        // For now, union types must be exactly the same
        // More sophisticated union type unification can be added later
        if (union1.alternatives.size != union2.alternatives.size) {
            throw UnificationException(
                "Cannot unify union types with different numbers of alternatives" +
                if (location != null) " at $location" else ""
            )
        }
        
        // Check if all alternatives in union1 can be unified with some alternative in union2
        for (alt1 in union1.alternatives) {
            val canUnify = union2.alternatives.any { alt2 ->
                try {
                    unify(alt1, alt2, location)
                    true
                } catch (e: UnificationException) {
                    false
                }
            }
            if (!canUnify) {
                throw UnificationException(
                    "Cannot unify union types: no matching alternative for $alt1" +
                    if (location != null) " at $location" else ""
                )
            }
        }
        
        return Substitution.empty
    }
}

 