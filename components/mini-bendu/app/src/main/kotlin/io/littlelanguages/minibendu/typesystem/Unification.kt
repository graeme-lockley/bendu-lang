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
            
            // Structurally equivalent types
            t1.structurallyEquivalent(t2) -> substitution
            
            // Type variable cases
            t1 is TypeVariable -> unifyVariable(t1, t2, substitution)
            t2 is TypeVariable -> unifyVariable(t2, t1, substitution)
            
            // Function types
            t1 is FunctionType && t2 is FunctionType -> unifyFunctionTypes(t1, t2, substitution)
            
            // Tuple types
            t1 is TupleType && t2 is TupleType -> unifyTupleTypes(t1, t2, substitution)
            
            // Record types
            t1 is RecordType && t2 is RecordType -> unifyRecordTypes(t1, t2, substitution)
            
            // Union types
            t1 is UnionType && t2 is UnionType -> unifyUnionTypes(t1, t2, substitution)
            
            // Intersection types
            t1 is IntersectionType && t2 is IntersectionType -> unifyIntersectionTypes(t1, t2, substitution)
            
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
            unifyInternal(fieldType1, fieldType2, currentSubst)
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
                // Check if record1 has fields not in record2, which would be an error for closed record2
                val uniqueFields1 = sub1Record1.fields.filterKeys { it !in sub1Record2.fields }
                if (uniqueFields1.isNotEmpty()) {
                    throw UnificationException("Cannot unify open record with closed record: open record has fields ${uniqueFields1.keys} not present in closed record")
                }
                
                // Collect fields in record2 that aren't in record1
                val uniqueFields2 = sub1Record2.fields.filterKeys { it !in sub1Record1.fields }
                
                // Create a record type for the remaining fields in record2
                val remainingRecord = RecordType(uniqueFields2)
                
                // Unify the row variable with the remaining fields
                unifyInternal(sub1Record1.rowVar, remainingRecord, fieldSubst)
            }
            
            // Only record2 has a row variable - record2 is open, record1 is closed
            sub1Record2.rowVar != null -> {
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
                    throw UnificationException("Cannot unify closed records with different fields: ${sub1Record1.fields.keys} vs ${sub1Record2.fields.keys}")
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
        // First try simple structural equivalence
        if (union1.structurallyEquivalent(union2)) {
            return substitution
        }
        
        // If sizes differ, they cannot be unified directly
        if (union1.alternatives.size != union2.alternatives.size) {
            throw UnificationException("Cannot unify unions with different number of alternatives: ${union1.alternatives.size} vs ${union2.alternatives.size}")
        }
        
        // Try to find a bijective mapping between alternatives
        val used = mutableSetOf<Type>()
        var currentSubst = substitution
        
        for (alt1 in union1.alternatives) {
            var unified = false
            
            for (alt2 in union2.alternatives) {
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
                throw UnificationException("Cannot unify union alternative $alt1 with any alternative in $union2")
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
            is TypeAlias -> type.typeArguments.any { occursCheck(typeVar, it) }
            is PrimitiveType, is LiteralStringType -> false
        }
    }
}

/**
 * Exception thrown when unification fails.
 */
class UnificationException(message: String) : Exception(message)
