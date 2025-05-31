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
class ConstraintSolver(
    private val typeAliasRegistry: TypeAliasRegistry = TypeAliasRegistry()
) {
    
    // Track the current substitution during solving
    private var currentSubstitution = Substitution.empty
    
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
        currentSubstitution = Substitution.empty
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
                is RecordTypeConstraint -> solveRecordTypeConstraint(substitutedConstraint)
                is MergeConstraint -> solveMergeConstraint(substitutedConstraint)
                is UnionCompatibilityConstraint -> solveUnionCompatibilityConstraint(substitutedConstraint)
                is ExhaustivenessConstraint -> solveExhaustivenessConstraint(substitutedConstraint)
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
            
            // Union type subtyping: T <: (A | B) if T <: A or T <: B
            constraint.supertype is UnionType -> {
                solveUnionSupertypeSubtyping(constraint.subtype, constraint.supertype, constraint.sourceLocation)
            }
            
            // Union type subtyping: (A | B) <: T if A <: T and B <: T
            constraint.subtype is UnionType -> {
                solveUnionSubtypeSubtyping(constraint.subtype, constraint.supertype, constraint.sourceLocation)
            }
            
            // Intersection type subtyping: (A & B) <: T if A <: T or B <: T
            constraint.subtype is IntersectionType -> {
                solveIntersectionSubtypeSubtyping(constraint.subtype, constraint.supertype, constraint.sourceLocation)
            }
            
            // Intersection type subtyping: T <: (A & B) if T <: A and T <: B
            constraint.supertype is IntersectionType -> {
                solveIntersectionSupertypeSubtyping(constraint.subtype, constraint.supertype, constraint.sourceLocation)
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
     * Solve union supertype subtyping: T <: (A | B)
     * This succeeds if T can be unified with any alternative in the union
     */
    private fun solveUnionSupertypeSubtyping(subtype: Type, superUnion: UnionType, location: SourceLocation?): Substitution {
        // Try to unify the subtype with each alternative in the union
        for (alternative in superUnion.alternatives) {
            try {
                // If unification succeeds with any alternative, the constraint is satisfied
                return unify(subtype, alternative, location)
            } catch (e: UnificationException) {
                // Continue trying other alternatives
                continue
            }
        }
        
        // If no alternative works, check if subtype is a type variable that can be constrained
        if (subtype is TypeVariable) {
            // For now, fail - more sophisticated handling could create fresh variables
            throw UnificationException(
                "Type variable $subtype cannot be constrained to union type $superUnion" +
                if (location != null) " at $location" else ""
            )
        }
        
        throw UnificationException(
            "Type $subtype is not a subtype of union $superUnion" +
            if (location != null) " at $location" else ""
        )
    }
    
    /**
     * Solve union subtype subtyping: (A | B) <: T
     * This succeeds if all alternatives in the union are subtypes of T
     */
    private fun solveUnionSubtypeSubtyping(unionSubtype: UnionType, supertype: Type, location: SourceLocation?): Substitution {
        var combinedSubstitution = Substitution.empty
        
        // Each alternative in the union must be a subtype of the supertype
        for (alternative in unionSubtype.alternatives) {
            try {
                val altSubstitution = unify(alternative, supertype, location)
                combinedSubstitution = altSubstitution.compose(combinedSubstitution)
            } catch (e: UnificationException) {
                throw UnificationException(
                    "Union alternative $alternative is not a subtype of $supertype in union subtyping" +
                    if (location != null) " at $location" else ""
                )
            }
        }
        
                return combinedSubstitution
    }
    
    /**
     * Solve intersection subtype subtyping: (A & B) <: T
     * This succeeds if any member in the intersection is a subtype of T
     */
    private fun solveIntersectionSubtypeSubtyping(intersectionSubtype: IntersectionType, supertype: Type, location: SourceLocation?): Substitution {
        // An intersection is a subtype of T if any of its members is a subtype of T
        // because the intersection represents a value that satisfies ALL constraints
        for (member in intersectionSubtype.members) {
            try {
                // If any member can be unified with the supertype, the constraint is satisfied
                return unify(member, supertype, location)
            } catch (e: UnificationException) {
                // Continue trying other members
                continue
            }
        }
        
        throw UnificationException(
            "Intersection $intersectionSubtype is not a subtype of $supertype" +
            if (location != null) " at $location" else ""
        )
    }
    
    /**
     * Solve intersection supertype subtyping: T <: (A & B)
     * This succeeds if T is a subtype of all members in the intersection
     */
    private fun solveIntersectionSupertypeSubtyping(subtype: Type, superIntersection: IntersectionType, location: SourceLocation?): Substitution {
        var combinedSubstitution = Substitution.empty
        
        // The subtype must be a subtype of all members in the intersection
        for (member in superIntersection.members) {
            try {
                val memberSubstitution = unify(subtype, member, location)
                combinedSubstitution = memberSubstitution.compose(combinedSubstitution)
            } catch (e: UnificationException) {
                throw UnificationException(
                    "Type $subtype is not a subtype of intersection member $member in intersection subtyping" +
                    if (location != null) " at $location" else ""
                )
            }
        }
        
        return combinedSubstitution
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
    
    /**
     * Solve record type constraint: ensure type is a record type
     */
    private fun solveRecordTypeConstraint(constraint: RecordTypeConstraint): Substitution {
        return when (constraint.type) {
            is RecordType -> {
                // Already a record type, constraint is satisfied
                Substitution.empty
            }
            is TypeVariable -> {
                // Unify the type variable with a fresh open record type
                val rowVariable = TypeVariable.fresh()
                val emptyRecord = RecordType(emptyMap(), rowVariable)
                unify(constraint.type, emptyRecord, constraint.sourceLocation)
            }
            else -> {
                throw UnificationException(
                    "Type ${constraint.type} cannot be constrained to be a record type" +
                    if (constraint.sourceLocation != null) " at ${constraint.sourceLocation}" else ""
                )
            }
        }
    }
    
    /**
     * Solve merge constraint: result = merge(spreadTypes, explicitFields)
     */
    private fun solveMergeConstraint(constraint: MergeConstraint): Substitution {
        val mergedFields = mutableMapOf<String, Type>()
        var combinedSubstitution = Substitution.empty
        
        // First, collect fields from all spread types
        for (spreadType in constraint.spreadTypes) {
            val appliedSpreadType = combinedSubstitution.apply(spreadType)
            when (appliedSpreadType) {
                is RecordType -> {
                    // Check for field conflicts before merging
                    for ((fieldName, fieldType) in appliedSpreadType.fields) {
                        val existingType = mergedFields[fieldName]
                        if (existingType != null) {
                            // Try to unify conflicting field types
                            try {
                                val fieldUnification = unify(existingType, fieldType, constraint.sourceLocation)
                                combinedSubstitution = fieldUnification.compose(combinedSubstitution)
                                // Use the unified type
                                mergedFields[fieldName] = combinedSubstitution.apply(fieldType)
                            } catch (e: UnificationException) {
                                throw UnificationException(
                                    "Cannot merge field '$fieldName': conflicting types $existingType and $fieldType" +
                                    if (constraint.sourceLocation != null) " at ${constraint.sourceLocation}" else ""
                                )
                            }
                        } else {
                            mergedFields[fieldName] = combinedSubstitution.apply(fieldType)
                        }
                    }
                }
                is TypeVariable -> {
                    // For type variables, we need to constrain them to be record types
                    // Create a fresh record type variable and unify
                    val freshRecordType = RecordType(emptyMap(), TypeVariable.fresh())
                    try {
                        val varUnification = unify(appliedSpreadType, freshRecordType, constraint.sourceLocation)
                        combinedSubstitution = varUnification.compose(combinedSubstitution)
                    } catch (e: UnificationException) {
                        throw UnificationException(
                            "Type variable ${appliedSpreadType} cannot be constrained to record type in merge operation" +
                            if (constraint.sourceLocation != null) " at ${constraint.sourceLocation}" else ""
                        )
                    }
                }
                else -> {
                    throw UnificationException(
                        "Cannot spread non-record type ${appliedSpreadType} in merge operation" +
                        if (constraint.sourceLocation != null) " at ${constraint.sourceLocation}" else ""
                    )
                }
            }
        }
        
        // Add explicit fields, checking for type compatibility with spread fields
        for ((fieldName, fieldType) in constraint.explicitFields) {
            val appliedFieldType = combinedSubstitution.apply(fieldType)
            val existingType = mergedFields[fieldName]
            if (existingType != null) {
                // Field override: explicit field overrides spread field, but types must be unifiable
                try {
                    val fieldUnification = unify(existingType, appliedFieldType, constraint.sourceLocation)
                    combinedSubstitution = fieldUnification.compose(combinedSubstitution)
                    // Use the unified type (explicit field wins, but must be compatible)
                    mergedFields[fieldName] = combinedSubstitution.apply(appliedFieldType)
                } catch (e: UnificationException) {
                    throw UnificationException(
                        "Cannot override field '$fieldName': incompatible types $existingType and $appliedFieldType" +
                        if (constraint.sourceLocation != null) " at ${constraint.sourceLocation}" else ""
                    )
                }
            } else {
                mergedFields[fieldName] = appliedFieldType
            }
        }
        
        // Create the result record type with a fresh row variable to maintain openness
        val rowVariable = TypeVariable.fresh()
        val resultRecordType = RecordType(mergedFields, rowVariable)
        
        // Unify the result type with the merged record type
        val unificationResult = unify(constraint.resultType, resultRecordType, constraint.sourceLocation)
        return unificationResult.compose(combinedSubstitution)
    }

    /**
     * Solve union compatibility constraint: scrutineeType ~∪ patternType
     * 
     * This constraint allows the scrutinee type to be inferred as a union type
     * that can accommodate the pattern type. This is essential for discriminated
     * union pattern matching where different patterns have conflicting constraints.
     */
    private fun solveUnionCompatibilityConstraint(constraint: UnionCompatibilityConstraint): Substitution {
        val scrutineeType = constraint.scrutineeType
        val patternType = constraint.patternType
        val location = constraint.sourceLocation
        
        // Case 1: If types are already compatible, no substitution needed
        if (scrutineeType.structurallyEquivalent(patternType)) {
            return Substitution.empty
        }
        
        // Case 2: If scrutinee is a type variable, we can substitute it with the pattern type
        // This allows the scrutinee to be inferred as a union that includes the pattern type
        if (scrutineeType is TypeVariable) {
            // For discriminated unions, we want to allow the scrutinee to be a union
            // that includes the pattern type. For now, we'll unify directly.
            // In a more sophisticated implementation, we would collect all pattern types
            // and create a union type.
            return unify(scrutineeType, patternType, location)
        }
        
        // Case 3: If scrutinee is already a union type, check if pattern is compatible
        if (scrutineeType is UnionType) {
            // Check if any member of the union can be unified with the pattern
            for (unionMember in scrutineeType.alternatives) {
                try {
                    return unify(unionMember, patternType, location)
                } catch (e: UnificationException) {
                    // Continue trying other union members
                    continue
                }
            }
            
            // If no existing member is compatible, we could extend the union
            // For now, we'll fail - more sophisticated handling would create a new union
            throw UnificationException(
                "Pattern type $patternType is not compatible with union type $scrutineeType" +
                if (location != null) " at $location" else ""
            )
        }
        
        // Case 4: If pattern is a union type, check if scrutinee is compatible with any member
        if (patternType is UnionType) {
            for (unionMember in patternType.alternatives) {
                try {
                    return unify(scrutineeType, unionMember, location)
                } catch (e: UnificationException) {
                    // Continue trying other union members
                    continue
                }
            }
            
            throw UnificationException(
                "Scrutinee type $scrutineeType is not compatible with union pattern $patternType" +
                if (location != null) " at $location" else ""
            )
        }
        
        // Case 5: Handle discriminated unions with record types
        if (scrutineeType is RecordType && patternType is RecordType) {
            return solveRecordUnionCompatibility(scrutineeType, patternType, location)
        }
        
        // Case 6: For discriminated unions, we need to be more flexible
        // If the scrutinee and pattern types are incompatible but could form a union,
        // we should allow this for union compatibility
        if (canFormDiscriminatedUnion(scrutineeType, patternType)) {
            // For union compatibility, we don't need to unify the types directly
            // We just need to ensure they can coexist in a union
            return Substitution.empty
        }
        
        // Case 7: Both are concrete types - try direct unification as a last resort
        try {
            return unify(scrutineeType, patternType, location)
        } catch (e: UnificationException) {
            // If direct unification fails, we need to create a union type
            // For now, we'll fail - more sophisticated handling would create a union
            throw UnificationException(
                "Cannot establish union compatibility between $scrutineeType and $patternType" +
                if (location != null) " at $location" else ""
            )
        }
    }
    
    /**
     * Solve union compatibility for record types, particularly for discriminated unions.
     * This handles cases like {typ: String, ...} vs {typ: "user", ...}
     */
    private fun solveRecordUnionCompatibility(scrutineeRecord: RecordType, patternRecord: RecordType, location: SourceLocation?): Substitution {
        var combinedSubstitution = Substitution.empty
        
        // For each field in the pattern, check compatibility with the scrutinee
        for ((fieldName, patternFieldType) in patternRecord.fields) {
            val scrutineeFieldType = scrutineeRecord.fields[fieldName]
            
            if (scrutineeFieldType == null) {
                // Pattern has a field that scrutinee doesn't have
                // This is okay for union compatibility - the scrutinee type can be extended
                continue
            }
            
            // Try to make the fields compatible
            try {
                // Special case: String type vs literal string type
                if (scrutineeFieldType == Types.String && patternFieldType is LiteralStringType) {
                    // String can accommodate any literal string - this is compatible
                    continue
                } else if (patternFieldType == Types.String && scrutineeFieldType is LiteralStringType) {
                    // Literal string can be widened to String - this is compatible
                    continue
                } else if (canFormUnion(scrutineeFieldType, patternFieldType)) {
                    // Fields can form a union - this is compatible for union compatibility
                    continue
                } else {
                    // Try direct unification only if the types might be unifiable
                    val fieldSubstitution = unify(scrutineeFieldType, patternFieldType, location)
                    combinedSubstitution = fieldSubstitution.compose(combinedSubstitution)
                }
            } catch (e: UnificationException) {
                // Fields are not directly unifiable - check if they can form a union
                if (canFormUnion(scrutineeFieldType, patternFieldType)) {
                    // Fields can form a union - this is compatible for union compatibility
                    continue
                } else {
                    throw UnificationException(
                        "Field '$fieldName' is not compatible: $scrutineeFieldType vs $patternFieldType" +
                        if (location != null) " at $location" else ""
                    )
                }
            }
        }
        
        return combinedSubstitution
    }
    
    /**
     * Check if two types can form a union (are compatible for union type creation).
     */
    private fun canFormUnion(type1: Type, type2: Type): Boolean {
        return when {
            // Same types can always form a union (though it would be simplified)
            type1.structurallyEquivalent(type2) -> true
            
            // String and literal string types can form unions
            (type1 == Types.String && type2 is LiteralStringType) -> true
            (type2 == Types.String && type1 is LiteralStringType) -> true
            
            // Different literal string types can form unions
            (type1 is LiteralStringType && type2 is LiteralStringType) -> true
            
            // Type variables can form unions with anything
            type1 is TypeVariable || type2 is TypeVariable -> true
            
            // Records with compatible structures can form unions
            type1 is RecordType && type2 is RecordType -> {
                // Check if they have compatible field structures
                val commonFields = type1.fields.keys.intersect(type2.fields.keys)
                commonFields.all { fieldName ->
                    val field1 = type1.fields[fieldName]!!
                    val field2 = type2.fields[fieldName]!!
                    canFormUnion(field1, field2)
                }
            }
            
            // Other types can potentially form unions
            else -> true
        }
    }

    /**
     * Check if two types can form a discriminated union.
     * This is more permissive than canFormUnion and specifically handles
     * discriminated unions where types have different structures but share
     * a discriminator field.
     */
    private fun canFormDiscriminatedUnion(type1: Type, type2: Type): Boolean {
        return when {
            // Same types can always form a union
            type1.structurallyEquivalent(type2) -> true
            
            // Type variables can form unions with anything
            type1 is TypeVariable || type2 is TypeVariable -> true
            
            // Records can form discriminated unions if they have compatible discriminator fields
            type1 is RecordType && type2 is RecordType -> {
                canFormDiscriminatedRecordUnion(type1, type2)
            }
            
            // Different concrete types can form unions
            else -> true
        }
    }
    
    /**
     * Check if two record types can form a discriminated union.
     * This checks if they have compatible discriminator fields (like 'tag', 'type', etc.)
     */
    private fun canFormDiscriminatedRecordUnion(record1: RecordType, record2: RecordType): Boolean {
        // Find potential discriminator fields (fields that exist in both records)
        val commonFields = record1.fields.keys.intersect(record2.fields.keys)
        
        // If there are no common fields, they can still form a union
        if (commonFields.isEmpty()) {
            return true
        }
        
        // Check if common fields can form unions (e.g., literal string types)
        return commonFields.all { fieldName ->
            val field1 = record1.fields[fieldName]!!
            val field2 = record2.fields[fieldName]!!
            
            // For discriminated unions, we're particularly interested in literal string discriminators
            when {
                field1 is LiteralStringType && field2 is LiteralStringType -> {
                    // Different literal strings can form a union (this is the discriminator)
                    true
                }
                field1 == Types.String || field2 == Types.String -> {
                    // String type can accommodate literal strings
                    true
                }
                else -> {
                    // Other field types should be compatible
                    canFormUnion(field1, field2)
                }
            }
        }
    }

    /**
     * Solve an exhaustiveness constraint by checking if the pattern match is exhaustive.
     * If the pattern match is not exhaustive, this will throw a UnificationException.
     */
    private fun solveExhaustivenessConstraint(constraint: ExhaustivenessConstraint): Substitution {
        // Apply the current substitution to resolve type variables
        val resolvedScrutineeType = currentSubstitution.apply(constraint.scrutineeType)
        
        // Normalize the type to resolve type aliases
        val normalizedType = typeAliasRegistry.normalizeType(resolvedScrutineeType)
        
        // Only check exhaustiveness for types where it makes sense
        val shouldCheck = when (normalizedType) {
            Types.Bool -> true  // Boolean has exactly 2 values
            is UnionType -> true  // Union types should be exhaustively covered
            is LiteralStringType -> true  // Literal string types have exactly 1 value
            is RecursiveType -> {
                // Check if the recursive type unfolds to something that should be exhaustively checked
                val unfoldedType = typeAliasRegistry.normalizeType(normalizedType.body)
                unfoldedType is UnionType || unfoldedType == Types.Bool || unfoldedType is LiteralStringType
            }
            // Primitive types like Int, String have infinite values, so exhaustiveness doesn't make sense
            Types.Int, Types.String, Types.Unit -> false
            is PrimitiveType -> false
            is FunctionType -> false
            is TypeVariable -> false  // Still unresolved
            is RecordType -> false  // Concrete record types don't need exhaustiveness checking
            is TupleType -> false   // Concrete tuple types don't need exhaustiveness checking
            is IntersectionType -> false  // Complex type, skip for now
            is TypeAlias -> false  // This should not happen after normalization
            else -> false
        }
        
        if (!shouldCheck) {
            // Exhaustiveness checking doesn't apply to this type
            return Substitution.empty
        }
        
        // Check exhaustiveness using the normalized type
        val exhaustivenessResult = ExhaustivenessChecker.check(constraint.matchExpr, normalizedType)
        
        if (exhaustivenessResult.isExhaustive) {
            // Pattern match is exhaustive - constraint is satisfied
            return Substitution.empty
        } else {
            // Pattern match is not exhaustive - this is a type error
            val missingPatterns = exhaustivenessResult.missingPatterns.joinToString(", ") { it.toString() }
            throw UnificationException(
                "non-exhaustive pattern match. Missing patterns: $missingPatterns" +
                if (constraint.sourceLocation != null) " at ${constraint.sourceLocation}" else ""
            )
        }
    }

}

 