package io.littlelanguages.minibendu.typesystem

/**
 * Result of a unification attempt.
 * Contains either a successful substitution or an error message.
 */
sealed class UnificationResult {
    abstract fun isSuccess(): Boolean
    abstract fun isFailure(): Boolean
    abstract fun getSubstitution(): Substitution
    abstract fun getError(): String?
    
    data class Success(private val substitution: Substitution) : UnificationResult() {
        override fun isSuccess(): Boolean = true
        override fun isFailure(): Boolean = false
        override fun getSubstitution(): Substitution = substitution
        override fun getError(): String? = null
    }
    
    data class Failure(private val error: String) : UnificationResult() {
        override fun isSuccess(): Boolean = false
        override fun isFailure(): Boolean = true
        override fun getSubstitution(): Substitution = Substitution.empty
        override fun getError(): String = error
    }
}

/**
 * Core unification algorithm for the mini-bendu type system.
 * 
 * This implements Robinson's unification algorithm extended with support for:
 * - Structural typing (records, tuples, unions)
 * - Occurs check to prevent infinite types
 * - Meaningful error messages
 * - Integration with substitutions
 */
object Unification {
    
    /**
     * Unify two types, returning either a successful substitution or an error.
     */
    fun unify(type1: Type, type2: Type, existingSubst: Substitution = Substitution.empty): UnificationResult {
        return try {
            val substitution = unifyInternal(type1, type2, existingSubst)
            UnificationResult.Success(substitution)
        } catch (e: UnificationException) {
            UnificationResult.Failure(e.message ?: "Unification failed")
        }
    }
    
    /**
     * Internal unification implementation that throws exceptions on failure.
     */
    private fun unifyInternal(type1: Type, type2: Type, substitution: Substitution): Substitution {
        // Apply existing substitution to both types
        val t1 = substitution.apply(type1)
        val t2 = substitution.apply(type2)
        
        return when {
            // Identical types
            t1 == t2 -> substitution
            
            // Structurally equivalent types (but skip for record types to ensure row variable unification)
            t1.structurallyEquivalent(t2) && !(t1 is RecordType && t2 is RecordType) -> substitution
            
            // Type variable cases
            t1 is TypeVariable -> unifyVariable(t1, t2, substitution)
            t2 is TypeVariable -> unifyVariable(t2, t1, substitution)
            
            // Function types
            t1 is FunctionType && t2 is FunctionType -> unifyFunctionTypes(t1, t2, substitution)
            
            // Tuple types
            t1 is TupleType && t2 is TupleType -> unifyTupleTypes(t1, t2, substitution)
            
            // Record types
            t1 is RecordType && t2 is RecordType -> unifyRecordTypes(t1, t2, substitution)
            
            // Recursive types
            t1 is RecursiveType && t2 is RecursiveType -> unifyRecursiveTypes(t1, t2, substitution)
            
            // Recursive type with non-recursive type - unfold and try again  
            t1 is RecursiveType -> unifyRecursiveWithType(t1, t2, substitution)
            t2 is RecursiveType -> unifyRecursiveWithType(t2, t1, substitution)
            
            // Union types
            t1 is UnionType && t2 is UnionType -> unifyUnionTypes(t1, t2, substitution)
            
            // Union type with non-union type - try to match against one of the alternatives
            t1 is UnionType -> unifyUnionWithType(t1, t2, substitution)
            t2 is UnionType -> unifyUnionWithType(t2, t1, substitution)
            
            // Intersection types
            t1 is IntersectionType && t2 is IntersectionType -> unifyIntersectionTypes(t1, t2, substitution)
            
            // String and LiteralStringType compatibility for discriminated unions
            t1 == Types.String && t2 is LiteralStringType -> substitution
            t2 == Types.String && t1 is LiteralStringType -> substitution
            
            // Different types that cannot be unified
            else -> throw UnificationException("Cannot unify $t1 with $t2")
        }
    }
    
    /**
     * Unify a type variable with another type.
     * Performs occurs check to prevent infinite types.
     */
    private fun unifyVariable(typeVar: TypeVariable, type: Type, substitution: Substitution): Substitution {
        // Check if variable already has a binding
        val existingBinding = substitution.lookup(typeVar)
        if (existingBinding != null) {
            // Recursively unify the existing binding with the new type
            return unifyInternal(existingBinding, type, substitution)
        }
        
        // If type is also a variable, check if it has a binding
        if (type is TypeVariable) {
            val typeBinding = substitution.lookup(type)
            if (typeBinding != null) {
                return unifyInternal(typeVar, typeBinding, substitution)
            }
        }
        
        // Occurs check: ensure the variable doesn't occur in the type
        if (occursCheck(typeVar, type)) {
            throw UnificationException("Occurs check failed: $typeVar occurs in $type (would create infinite type)")
        }
        
        // Create new substitution with the binding and compose correctly
        val newBinding = Substitution.single(typeVar, type)
        return newBinding.compose(substitution)
    }
    
    /**
     * Unify two function types by unifying their domains and codomains.
     */
    private fun unifyFunctionTypes(func1: FunctionType, func2: FunctionType, substitution: Substitution): Substitution {
        // Unify domains first
        val domainSubst = unifyInternal(func1.domain, func2.domain, substitution)
        
        // Then unify codomains with the result of domain unification
        return unifyInternal(func1.codomain, func2.codomain, domainSubst)
    }
    
    /**
     * Unify two tuple types by unifying their elements pairwise.
     */
    private fun unifyTupleTypes(tuple1: TupleType, tuple2: TupleType, substitution: Substitution): Substitution {
        if (tuple1.elements.size != tuple2.elements.size) {
            throw UnificationException("Cannot unify tuples of different lengths: ${tuple1.elements.size} vs ${tuple2.elements.size}")
        }
        
        return tuple1.elements.zip(tuple2.elements).fold(substitution) { currentSubst, (elem1, elem2) ->
            unifyInternal(elem1, elem2, currentSubst)
        }
    }
    
    /**
     * Unify two record types by unifying corresponding fields.
     * Handles row polymorphism with appropriate row variable unification.
     */
    private fun unifyRecordTypes(record1: RecordType, record2: RecordType, substitution: Substitution): Substitution {
        // First check if any record has fields that conflict with the other record's fields
        val commonFieldNames = record1.fields.keys.intersect(record2.fields.keys)
        
        // For common fields, check if the types are compatible
        val fieldSubst = commonFieldNames.fold(substitution) { currentSubst, fieldName ->
            val fieldType1 = record1.fields[fieldName]!!
            val fieldType2 = record2.fields[fieldName]!!
            try {
                unifyInternal(fieldType1, fieldType2, currentSubst)
            } catch (e: UnificationException) {
                throw UnificationException("Cannot unify field '$fieldName': ${e.message}")
            }
        }
        
        // Apply the field substitution to both records (important for subsequent checks)
        val sub1Record1 = fieldSubst.apply(record1) as RecordType
        val sub1Record2 = fieldSubst.apply(record2) as RecordType
        
        // Handle row variables based on the various cases
        return when {
            // Both records have row variables
            sub1Record1.rowVar != null && sub1Record2.rowVar != null -> {
                // First collect the unique fields from each record
                val uniqueFields1 = sub1Record1.fields.filterKeys { it !in sub1Record2.fields }
                val uniqueFields2 = sub1Record2.fields.filterKeys { it !in sub1Record1.fields }
                
                if (uniqueFields1.isEmpty() && uniqueFields2.isEmpty()) {
                    // If there are no unique fields on either side, just unify the row variables
                    unifyInternal(sub1Record1.rowVar, sub1Record2.rowVar, fieldSubst)
                } else {
                    // Create a fresh row variable for the unified tail
                    val freshRowVar = TypeVariable.fresh()
                    
                    // Create record types for the remaining unique fields with the fresh row variable
                    val record1RowType = RecordType(uniqueFields2, freshRowVar)
                    val record2RowType = RecordType(uniqueFields1, freshRowVar)
                    
                    // Unify the row variables with their corresponding record types
                    val subst2 = unifyInternal(sub1Record1.rowVar, record1RowType, fieldSubst)
                    unifyInternal(sub1Record2.rowVar, record2RowType, subst2)
                }
            }
            
            // Only record1 has a row variable - record1 is open, record2 is closed
            sub1Record1.rowVar != null -> {
                // Check if record1 has concrete fields not in record2
                val extraFields1 = sub1Record1.fields.filterKeys { it !in sub1Record2.fields }
                if (extraFields1.isNotEmpty()) {
                    throw UnificationException("Cannot unify open record with closed record: open record has extra fields ${extraFields1.keys} not present in closed record")
                }
                
                // Collect fields in record2 that aren't in record1
                val uniqueFields2 = sub1Record2.fields.filterKeys { it !in sub1Record1.fields }
                
                // The row variable should absorb any extra fields from the closed record
                val remainingRecord = RecordType(uniqueFields2)
                
                // Unify the row variable with the remaining fields
                unifyInternal(sub1Record1.rowVar, remainingRecord, fieldSubst)
            }
            
            // Only record2 has a row variable - record2 is open, record1 is closed
            sub1Record2.rowVar != null -> {
                // Check if record2 has required fields that record1 doesn't have
                val missingFields = sub1Record2.fields.filterKeys { it !in sub1Record1.fields }
                if (missingFields.isNotEmpty()) {
                    throw UnificationException("Cannot unify closed record with open record: closed record is missing required fields ${missingFields.keys}")
                }
                
                // Collect fields in record1 that aren't in record2
                val uniqueFields1 = sub1Record1.fields.filterKeys { it !in sub1Record2.fields }
                
                // The row variable should absorb any extra fields from the closed record
                val remainingRecord = RecordType(uniqueFields1)
                
                // Unify the row variable with the remaining fields
                unifyInternal(sub1Record2.rowVar, remainingRecord, fieldSubst)
            }
            
            // Neither record has a row variable - both are closed records
            else -> {
                // For closed records, the field sets must be identical
                if (sub1Record1.fields.keys != sub1Record2.fields.keys) {
                    val missingInRecord1 = sub1Record2.fields.keys - sub1Record1.fields.keys
                    val missingInRecord2 = sub1Record1.fields.keys - sub1Record2.fields.keys
                    
                    val errorMessage = when {
                        missingInRecord1.isNotEmpty() && missingInRecord2.isEmpty() -> 
                            "Cannot unify closed records: missing required fields $missingInRecord1"
                        missingInRecord2.isNotEmpty() && missingInRecord1.isEmpty() -> 
                            "Cannot unify closed records: extra fields $missingInRecord2 not allowed"
                        else -> 
                            "Cannot unify closed records: missing fields $missingInRecord1, extra fields $missingInRecord2"
                    }
                    
                    throw UnificationException(errorMessage)
                }
                
                fieldSubst
            }
        }
    }
    
    /**
     * Unify two union types by checking that they have the same alternatives.
     * Enhanced to support more sophisticated union unification patterns.
     */
    private fun unifyUnionTypes(union1: UnionType, union2: UnionType, substitution: Substitution): Substitution {
        // Apply current substitution to both unions
        val sub1 = substitution.apply(union1)
        val sub2 = substitution.apply(union2)
        
        // If they're not union types after substitution, delegate to general unification
        if (sub1 !is UnionType || sub2 !is UnionType) {
            return unifyInternal(sub1, sub2, substitution)
        }
        
        // Normalize the unions by removing duplicates and flattening
        val normalizedUnion1 = UnionType.create(sub1.alternatives) as? UnionType ?: return unifyInternal(sub1, sub2, substitution)
        val normalizedUnion2 = UnionType.create(sub2.alternatives) as? UnionType ?: return unifyInternal(sub1, sub2, substitution)
        
        // First try simple structural equivalence
        if (normalizedUnion1.structurallyEquivalent(normalizedUnion2)) {
            return substitution
        }
        
        // If sizes differ after normalization, they cannot be unified directly
        if (normalizedUnion1.alternatives.size != normalizedUnion2.alternatives.size) {
            throw UnificationException("Cannot unify unions with different number of alternatives: ${normalizedUnion1.alternatives.size} vs ${normalizedUnion2.alternatives.size}")
        }
        
        // Try to find a bijective mapping between alternatives
        val used = mutableSetOf<Type>()
        var currentSubst = substitution
        
        for (alt1 in normalizedUnion1.alternatives) {
            var unified = false
            
            for (alt2 in normalizedUnion2.alternatives) {
                if (alt2 in used) continue
                
                try {
                    // Try to unify this pair of alternatives
                    val altSubst = unifyInternal(alt1, alt2, currentSubst)
                    used.add(alt2)
                    currentSubst = altSubst
                    unified = true
                    break
                } catch (e: UnificationException) {
                    // Try next alternative
                    continue
                }
            }
            
            if (!unified) {
                throw UnificationException("Cannot unify union alternative $alt1 with any alternative in $normalizedUnion2")
            }
        }
        
        return currentSubst
    }
    
    /**
     * Unify two intersection types by checking that they have the same members.
     * Enhanced to support more sophisticated intersection unification patterns.
     */
    private fun unifyIntersectionTypes(intersection1: IntersectionType, intersection2: IntersectionType, substitution: Substitution): Substitution {
        // First try simple structural equivalence
        if (intersection1.structurallyEquivalent(intersection2)) {
            return substitution
        }
        
        // If sizes differ, they cannot be unified directly
        if (intersection1.members.size != intersection2.members.size) {
            throw UnificationException("Cannot unify intersections with different number of members: ${intersection1.members.size} vs ${intersection2.members.size}")
        }
        
        // Try to find a bijective mapping between members
        val used = mutableSetOf<Type>()
        var currentSubst = substitution
        
        for (member1 in intersection1.members) {
            var unified = false
            
            for (member2 in intersection2.members) {
                if (member2 in used) continue
                
                try {
                    // Try to unify this pair of members
                    val memberSubst = unifyInternal(member1, member2, currentSubst)
                    used.add(member2)
                    currentSubst = memberSubst
                    unified = true
                    break
                } catch (e: UnificationException) {
                    // Try next member
                    continue
                }
            }
            
            if (!unified) {
                throw UnificationException("Cannot unify intersection member $member1 with any member in $intersection2")
            }
        }
        
        return currentSubst
    }
    
    /**
     * Occurs check: determine if a type variable occurs within a type.
     * This prevents the creation of infinite types like τ = τ -> Int.
     */
    private fun occursCheck(typeVar: TypeVariable, type: Type): Boolean {
        return when (type) {
            is TypeVariable -> typeVar == type
            is FunctionType -> occursCheck(typeVar, type.domain) || occursCheck(typeVar, type.codomain)
            is TupleType -> type.elements.any { occursCheck(typeVar, it) }
            is RecordType -> {
                type.fields.values.any { occursCheck(typeVar, it) } ||
                (type.rowVar != null && occursCheck(typeVar, type.rowVar))
            }
            is UnionType -> type.alternatives.any { occursCheck(typeVar, it) }
            is IntersectionType -> type.members.any { occursCheck(typeVar, it) }
            is RecursiveType -> {
                // For recursive types, we need to be careful about the occurs check
                // The recursive variable itself is bound within the type, so we shouldn't
                // consider it a problematic occurrence if it matches the type's own recursive var
                if (typeVar == type.recursiveVar) {
                    // This is fine - the type variable is bound by this recursive type
                    false
                } else {
                    // Check for occurrences in the body, but exclude the recursive variable
                    // since it's bound within this scope
                    occursCheck(typeVar, type.body)
                }
            }
            is TypeAlias -> type.typeArguments.any { occursCheck(typeVar, it) }
            is PrimitiveType, is LiteralStringType -> false
        }
    }
    
    /**
     * Unify two recursive types using equi-recursive approach.
     * Two recursive types unify if their unfolded bodies unify.
     */
    private fun unifyRecursiveTypes(rec1: RecursiveType, rec2: RecursiveType, substitution: Substitution): Substitution {
        // For equi-recursive types, we check if the types have the same structure
        // by checking if their names match and their bodies are equivalent
        if (rec1.name != rec2.name) {
            throw UnificationException("Cannot unify recursive types with different names: ${rec1.name} vs ${rec2.name}")
        }
        
        // Create a mapping from rec2's recursive variable to rec1's recursive variable
        // This allows us to compare the bodies with aligned recursive references
        val variableMapping = Substitution.single(rec2.recursiveVar, rec1.recursiveVar)
        val alignedBody2 = variableMapping.apply(rec2.body)
        
        // Now unify the bodies with the recursive variables aligned
        return unifyInternal(rec1.body, alignedBody2, substitution)
    }
    
    /**
     * Unify a union type with a non-union type.
     * The non-union type must match one of the alternatives in the union.
     */
    private fun unifyUnionWithType(union: UnionType, type: Type, substitution: Substitution): Substitution {
        // Try to unify the type with each alternative in the union
        for (alternative in union.alternatives) {
            try {
                // If unification succeeds with this alternative, return the result
                return unifyInternal(alternative, type, substitution)
            } catch (e: UnificationException) {
                // Try next alternative
                continue
            }
        }
        
        // If none of the alternatives unify, the unification fails
        throw UnificationException("Cannot unify $type with any alternative in union $union")
    }
    
    /**
     * Unify a recursive type with a non-recursive type.
     * This unfolds the recursive type and attempts unification with its body.
     */
    private fun unifyRecursiveWithType(recursiveType: RecursiveType, type: Type, substitution: Substitution): Substitution {
        // Unfold the recursive type to its body
        val unfoldedBody = recursiveType.body
        
        // Try to unify the unfolded body with the other type
        return unifyInternal(unfoldedBody, type, substitution)
    }
}

/**
 * Exception thrown when unification fails.
 */
class UnificationException(message: String) : Exception(message)
