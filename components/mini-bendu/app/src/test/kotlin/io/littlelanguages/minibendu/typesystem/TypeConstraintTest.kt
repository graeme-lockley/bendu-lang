package io.littlelanguages.minibendu.typesystem

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Tests for TypeConstraint classes - Task 7 of Phase 1
 * 
 * This test suite defines the requirements for type constraints which are
 * fundamental to constraint-based type inference. Type constraints represent
 * relationships between types that must be satisfied during unification.
 * 
 * The constraint system supports:
 * - Equality constraints: T1 ~ T2 (types must be unified)
 * - Subtyping constraints: T1 <: T2 (T1 is a subtype of T2)
 * - Instance constraints: T : C (type T is an instance of type class/interface C)
 * 
 * Constraints have priorities for solver efficiency and can be simplified
 * and normalized for optimal solving.
 */
class TypeConstraintTest {
    
    @Test
    fun `equality constraint creation and properties`() {
        val var1 = TypeVariable.fresh()
        val var2 = TypeVariable.fresh()
        val constraint = EqualityConstraint(var1, var2, null, ConstraintOrigin.UNIFICATION)
        
        assertEquals(var1, constraint.type1, "Left type should be preserved")
        assertEquals(var2, constraint.type2, "Right type should be preserved")
        assertEquals(ConstraintOrigin.UNIFICATION, constraint.origin, "Origin should be preserved")
        assertEquals(ConstraintPriority.HIGH, constraint.priority, "Equality constraints should have high priority")
        assertTrue(constraint.isEquality(), "Should be identified as equality constraint")
        assertFalse(constraint.isSubtyping(), "Should not be identified as subtyping constraint")
    }
    
    @Test
    fun `equality constraint with primitive types`() {
        val constraint = EqualityConstraint(Types.Int, Types.String, null, ConstraintOrigin.INFERENCE)
        
        assertEquals(Types.Int, constraint.type1, "Left type should be Int")
        assertEquals(Types.String, constraint.type2, "Right type should be String")
        assertEquals(ConstraintOrigin.INFERENCE, constraint.origin, "Origin should be INFERENCE")
    }
    
    @Test
    fun `subtyping constraint creation and properties`() {
        val recordType = RecordType(mapOf("x" to Types.Int, "y" to Types.String))
        val superRecordType = RecordType(mapOf("x" to Types.Int))
        val constraint = SubtypingConstraint(recordType, superRecordType, null, ConstraintOrigin.SUBTYPING)
        
        assertEquals(recordType, constraint.subtype, "Subtype should be preserved")
        assertEquals(superRecordType, constraint.supertype, "Supertype should be preserved")
        assertEquals(ConstraintOrigin.SUBTYPING, constraint.origin, "Origin should be preserved")
        assertEquals(ConstraintPriority.MEDIUM, constraint.priority, "Subtyping constraints should have medium priority")
        assertFalse(constraint.isEquality(), "Should not be identified as equality constraint")
        assertTrue(constraint.isSubtyping(), "Should be identified as subtyping constraint")
    }
    
    @Test
    fun `instance constraint creation and properties`() {
        val typeVar = TypeVariable.fresh()
        val typeClass = "Comparable"
        val constraint = InstanceConstraint(typeVar, typeClass, ConstraintOrigin.TYPE_CLASS)
        
        assertEquals(typeVar, constraint.type, "Type should be preserved")
        assertEquals(typeClass, constraint.typeClass, "Type class should be preserved")
        assertEquals(ConstraintOrigin.TYPE_CLASS, constraint.origin, "Origin should be preserved")
        assertEquals(ConstraintPriority.LOW, constraint.priority, "Instance constraints should have low priority")
        assertFalse(constraint.isEquality(), "Should not be identified as equality constraint")
        assertFalse(constraint.isSubtyping(), "Should not be identified as subtyping constraint")
        assertTrue(constraint.isInstance(), "Should be identified as instance constraint")
    }
    
    @Test
    fun `constraint equality and hash code`() {
        val var1 = TypeVariable.fresh()
        val var2 = TypeVariable.fresh()
        
        val constraint1 = EqualityConstraint(var1, var2, null, ConstraintOrigin.UNIFICATION)
        val constraint2 = EqualityConstraint(var1, var2, null, ConstraintOrigin.UNIFICATION)
        val constraint3 = EqualityConstraint(var2, var1, null, ConstraintOrigin.UNIFICATION) // Flipped
        val constraint4 = EqualityConstraint(var1, Types.Int, null, ConstraintOrigin.UNIFICATION)
        
        assertEquals(constraint1, constraint2, "Identical constraints should be equal")
        assertEquals(constraint1.hashCode(), constraint2.hashCode(), "Equal constraints should have same hash code")
        assertNotEquals(constraint1, constraint3, "Flipped constraints should not be equal")
        assertNotEquals(constraint1, constraint4, "Different constraints should not be equal")
    }
    
    @Test
    fun `constraint involves type variable`() {
        val var1 = TypeVariable.fresh()
        val var2 = TypeVariable.fresh()
        val var3 = TypeVariable.fresh()
        
        val equalityConstraint = EqualityConstraint(var1, Types.Int, null, ConstraintOrigin.UNIFICATION)
        val subtypingConstraint = SubtypingConstraint(var2, Types.String, null, ConstraintOrigin.SUBTYPING)
        val instanceConstraint = InstanceConstraint(var3, "Comparable", ConstraintOrigin.TYPE_CLASS)
        
        assertTrue(equalityConstraint.involvesVariable(var1), "Should involve left variable")
        assertFalse(equalityConstraint.involvesVariable(var2), "Should not involve other variables")
        
        assertTrue(subtypingConstraint.involvesVariable(var2), "Should involve subtype variable")
        assertFalse(subtypingConstraint.involvesVariable(var1), "Should not involve other variables")
        
        assertTrue(instanceConstraint.involvesVariable(var3), "Should involve instance variable")
        assertFalse(instanceConstraint.involvesVariable(var1), "Should not involve other variables")
    }
    
    @Test
    fun `constraint applies substitution`() {
        val var1 = TypeVariable.fresh()
        val var2 = TypeVariable.fresh()
        
        val constraint = EqualityConstraint(var1, var2, null, ConstraintOrigin.UNIFICATION)
        val substitution = Substitution.builder()
            .add(var1, Types.Int)
            .add(var2, Types.String)
            .build()
        
        val substitutedConstraint = constraint.applySubstitution(substitution)
        
        assertTrue(substitutedConstraint is EqualityConstraint, "Should remain equality constraint")
        val eqConstraint = substitutedConstraint
        assertEquals(Types.Int, eqConstraint.type1, "Left type should be substituted")
        assertEquals(Types.String, eqConstraint.type2, "Right type should be substituted")
        assertEquals(constraint.origin, eqConstraint.origin, "Origin should be preserved")
    }
    
    @Test
    fun `constraint simplification and normalization`() {
        // Test constraint involving identical types
        val identicalConstraint = EqualityConstraint(Types.Int, Types.Int, null, ConstraintOrigin.UNIFICATION)
        val simplified = identicalConstraint.simplify()
        
        assertTrue(simplified.isEmpty(), "Constraint between identical types should simplify to empty")
        
        // Test constraint that cannot be simplified
        val var1 = TypeVariable.fresh()
        val unsimplifiableConstraint = EqualityConstraint(var1, Types.Int, null, ConstraintOrigin.UNIFICATION)
        val notSimplified = unsimplifiableConstraint.simplify()
        
        assertEquals(1, notSimplified.size, "Unsimplifiable constraint should return itself")
        assertEquals(unsimplifiableConstraint, notSimplified[0], "Should return the same constraint")
    }
    
    @Test
    fun `constraint ordering by priority`() {
        val var1 = TypeVariable.fresh()
        val var2 = TypeVariable.fresh()
        
        val equalityConstraint = EqualityConstraint(var1, Types.Int, null, ConstraintOrigin.UNIFICATION)
        val subtypingConstraint = SubtypingConstraint(var1, Types.String, null, ConstraintOrigin.SUBTYPING)
        val instanceConstraint = InstanceConstraint(var2, "Comparable", ConstraintOrigin.TYPE_CLASS)
        
        val constraints = listOf(instanceConstraint, subtypingConstraint, equalityConstraint)
        val sorted = constraints.sortedBy { it.priority.ordinal }
        
        assertEquals(equalityConstraint, sorted[0], "Equality constraint should have highest priority")
        assertEquals(subtypingConstraint, sorted[1], "Subtyping constraint should have medium priority")
        assertEquals(instanceConstraint, sorted[2], "Instance constraint should have lowest priority")
    }
    
    @Test
    fun `constraint free variables calculation`() {
        val var1 = TypeVariable.fresh()
        val var2 = TypeVariable.fresh()
        val var3 = TypeVariable.fresh()
        
        val functionType = FunctionType(var1, var2)
        val constraint = EqualityConstraint(functionType, var3, null, ConstraintOrigin.UNIFICATION)
        
        val freeVars = constraint.freeVariables()
        
        assertEquals(3, freeVars.size, "Should find all free variables")
        assertTrue(freeVars.contains(var1), "Should contain var1 from function domain")
        assertTrue(freeVars.contains(var2), "Should contain var2 from function codomain")
        assertTrue(freeVars.contains(var3), "Should contain var3 from right side")
    }
    
    @Test
    fun `constraint dependency tracking`() {
        val var1 = TypeVariable.fresh()
        val var2 = TypeVariable.fresh()
        val var3 = TypeVariable.fresh()
        
        val constraint1 = EqualityConstraint(var1, Types.Int, null, ConstraintOrigin.UNIFICATION)
        val constraint2 = EqualityConstraint(var2, var1, null, ConstraintOrigin.UNIFICATION)
        val constraint3 = EqualityConstraint(var3, Types.String, null, ConstraintOrigin.UNIFICATION)
        
        // constraint2 depends on constraint1 because they share var1
        assertTrue(constraint2.dependsOn(constraint1), "constraint2 should depend on constraint1")
        assertTrue(constraint1.dependsOn(constraint2), "Dependency should be symmetric for shared variables")
        assertFalse(constraint3.dependsOn(constraint1), "constraint3 should not depend on constraint1")
        assertFalse(constraint1.dependsOn(constraint3), "constraint1 should not depend on constraint3")
    }
    
    @Test
    fun `constraint with complex types`() {
        val var1 = TypeVariable.fresh()

        // Create complex types: {x: a, y: Int} ~ (a, String)
        val recordType = RecordType(mapOf("x" to var1, "y" to Types.Int))
        val tupleType = TupleType(listOf(var1, Types.String))
        
        val constraint = EqualityConstraint(recordType, tupleType, null, ConstraintOrigin.INFERENCE)
        
        val freeVars = constraint.freeVariables()
        assertEquals(1, freeVars.size, "Should find shared variable")
        assertTrue(freeVars.contains(var1), "Should contain the shared variable")
        
        // Apply substitution
        val substitution = Substitution.single(var1, Types.Bool)
        val substitutedConstraint = constraint.applySubstitution(substitution)
        
        assertTrue(substitutedConstraint is EqualityConstraint, "Should remain equality constraint")
        val eqConstraint = substitutedConstraint
        
        val substitutedRecord = eqConstraint.type1 as RecordType
        val substitutedTuple = eqConstraint.type2 as TupleType
        
        assertEquals(Types.Bool, substitutedRecord.fields["x"], "Record field should be substituted")
        assertEquals(Types.Bool, substitutedTuple.elements[0], "Tuple element should be substituted")
    }
}
