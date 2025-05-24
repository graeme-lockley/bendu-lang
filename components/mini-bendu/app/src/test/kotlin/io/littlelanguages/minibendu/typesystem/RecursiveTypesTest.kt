package io.littlelanguages.minibendu.typesystem

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import io.littlelanguages.minibendu.*
import io.littlelanguages.scanpiler.LocationCoordinate

/**
 * Test class for recursive types functionality.
 * 
 * Recursive types allow for self-referential type definitions,
 * enabling structures like linked lists, trees, and other
 * inductively defined data structures.
 * 
 * Tests cover:
 * - Defining recursive types
 * - Using recursive types
 * - Unification with recursive types
 * - Occurs check with recursive types
 * - Mutual recursion in type definitions
 */
class RecursiveTypesTest {

    private fun createStringLocation(value: String): StringLocation {
        return StringLocation(value, LocationCoordinate(0, 1, 1))
    }
    
    private fun createIntLocation(value: Int): IntLocation {
        return IntLocation(value, LocationCoordinate(0, 1, 1))
    }
    
    private fun createLocation(): LocationCoordinate = LocationCoordinate(0, 1, 1)

    // ===== DEFINING RECURSIVE TYPES TESTS =====

    @Test
    fun `simple recursive type definition - List`() {
        // type List = Nil | Cons(Int, List)
        // This is a classic recursive list definition
        
        val listTypeVar = TypeVariable.fresh()
        val nilType = Types.literal("Nil")
        val consType = RecordType(mapOf(
            "value" to Types.Int,
            "next" to listTypeVar
        ))
        
        val listType = UnionType.of(nilType, consType)
        
        // Create a recursive type definition
        val recursiveListType = RecursiveType("List", listTypeVar, listType)
        
        assertNotNull(recursiveListType.name, "Recursive type should have a name")
        assertEquals("List", recursiveListType.name)
        assertEquals(listTypeVar, recursiveListType.recursiveVar)
        assertTrue(recursiveListType.body is UnionType, "List body should be a union type")
        
        // Verify the body contains the recursive reference
        val bodyUnion = recursiveListType.body as UnionType
        assertTrue(bodyUnion.alternatives.contains(nilType), "List should contain Nil alternative")
        assertTrue(bodyUnion.alternatives.any { it is RecordType }, "List should contain Cons alternative")
    }

    @Test
    fun `tree recursive type definition`() {
        // type Tree = Leaf(Int) | Node(Tree, Int, Tree)
        
        val treeTypeVar = TypeVariable.fresh()
        val leafType = RecordType(mapOf("value" to Types.Int))
        val nodeType = RecordType(mapOf(
            "left" to treeTypeVar,
            "value" to Types.Int,
            "right" to treeTypeVar
        ))
        
        val treeType = UnionType.of(leafType, nodeType)
        val recursiveTreeType = RecursiveType("Tree", treeTypeVar, treeType)
        
        assertEquals("Tree", recursiveTreeType.name)
        assertEquals(treeTypeVar, recursiveTreeType.recursiveVar)
        
        // Check that the tree body has both leaf and node alternatives
        val bodyUnion = recursiveTreeType.body as UnionType
        assertEquals(2, bodyUnion.alternatives.size, "Tree should have exactly 2 alternatives")
    }

    @Test
    fun `nested recursive type definition`() {
        // type NestedList = Nil | Cons(NestedList, NestedList)
        // This creates a more complex recursive structure
        
        val nestedListVar = TypeVariable.fresh()
        val nilType = Types.literal("Nil")
        val consType = RecordType(mapOf(
            "first" to nestedListVar,
            "rest" to nestedListVar
        ))
        
        val nestedListType = UnionType.of(nilType, consType)
        val recursiveNestedListType = RecursiveType("NestedList", nestedListVar, nestedListType)
        
        assertEquals("NestedList", recursiveNestedListType.name)
        
        // Verify that the recursive variable appears multiple times in the body
        val bodyUnion = recursiveNestedListType.body as UnionType
        val consRecord = bodyUnion.alternatives.find { it is RecordType } as RecordType
        
        assertEquals(nestedListVar, consRecord.fields["first"])
        assertEquals(nestedListVar, consRecord.fields["rest"])
    }

    // ===== USING RECURSIVE TYPES TESTS =====

    @Test
    fun `construct values of recursive type`() {
        // Creating actual values of List type
        // nil = Nil
        // cons1 = Cons(42, nil)
        // cons2 = Cons(1, cons1)
        
        val listTypeVar = TypeVariable.fresh()
        val listType = RecursiveType("List", listTypeVar, UnionType.of(
            Types.literal("Nil"),
            RecordType(mapOf(
                "value" to Types.Int,
                "next" to listTypeVar
            ))
        ))
        
        // nil value
        val nilValue = LiteralStringExpr(createStringLocation("Nil"))
        
        // cons1 = Cons(42, nil)
        val cons1Value = RecordExpr(listOf(
            FieldExpr(createStringLocation("value"), LiteralIntExpr(createIntLocation(42))),
            FieldExpr(createStringLocation("next"), nilValue)
        ), createLocation())
        
        // cons2 = Cons(1, cons1)
        val cons2Value = RecordExpr(listOf(
            FieldExpr(createStringLocation("value"), LiteralIntExpr(createIntLocation(1))),
            FieldExpr(createStringLocation("next"), cons1Value)
        ), createLocation())
        
        // Type check these constructions
        val env = TypeEnvironment.empty()
        val generator = ConstraintGenerator(env)
        
        // Check nil
        val nilResult = generator.generateConstraints(nilValue)
        assertTrue(nilResult.isSuccess(), "Nil construction should succeed")
        
        // Check cons1 
        val cons1Result = generator.generateConstraints(cons1Value)
        assertTrue(cons1Result.isSuccess(), "Cons1 construction should succeed")
        
        // Check cons2
        val cons2Result = generator.generateConstraints(cons2Value)
        assertTrue(cons2Result.isSuccess(), "Cons2 construction should succeed")
    }

    @Test
    fun `pattern match on recursive type`() {
        // match list with
        // | Nil -> 0
        // | Cons(value, rest) -> value + length(rest)
        
        val listVar = VarExpr(createStringLocation("list"))
        
        val nilPattern = LiteralStringPattern(createStringLocation("Nil"))
        val nilResult = LiteralIntExpr(createIntLocation(0))
        
        val consPattern = RecordPattern(listOf(
            FieldPattern(createStringLocation("value"), VarPattern(createStringLocation("value"))),
            FieldPattern(createStringLocation("rest"), VarPattern(createStringLocation("rest"))),
        ), createLocation())
        
        // value + length(rest) - simplified as just value for testing
        val consResult = VarExpr(createStringLocation("value"))
        
        val matchExpr = MatchExpr(
            listVar,
            listOf(
                MatchCase(nilPattern, nilResult),
                MatchCase(consPattern, consResult)
            ),
            createLocation()
        )
        
        // Set up environment with list variable of recursive type
        val listTypeVar = TypeVariable.fresh()
        val listType = RecursiveType("List", listTypeVar, UnionType.of(
            Types.literal("Nil"),
            RecordType(mapOf(
                "value" to Types.Int,
                "next" to listTypeVar
            ))
        ))
        
        val env = TypeEnvironment.empty()
            .bind("list", TypeScheme.monomorphic(listType))
        
        val generator = ConstraintGenerator(env)
        val result = generator.generateConstraints(matchExpr)
        
        assertTrue(result.isSuccess(), "Pattern matching on recursive type should succeed")
    }

    @Test
    fun `recursive function on recursive type`() {
        // def length(list: List) -> Int = 
        //   match list with
        //   | Nil -> 0
        //   | Cons(_, rest) -> 1 + length(rest)
        
        val listParam = VarExpr(createStringLocation("list"))
        val lengthCall = VarExpr(createStringLocation("length")) // Recursive call
        val restVar = VarExpr(createStringLocation("rest"))
        
        val nilPattern = LiteralStringPattern(createStringLocation("Nil"))
        val nilCase = MatchCase(nilPattern, LiteralIntExpr(createIntLocation(0)))
        
        val consPattern = RecordPattern(listOf(
            FieldPattern(createStringLocation("value"), WildcardPattern(createLocation())),
            FieldPattern(createStringLocation("rest"), VarPattern(createStringLocation("rest")))
        ), createLocation())
        
        // 1 + length(rest) - simplified as just 1 for testing constraint generation
        val consCase = MatchCase(consPattern, LiteralIntExpr(createIntLocation(1)))
        
        val matchExpr = MatchExpr(listParam, listOf(nilCase, consCase), createLocation())
        
        // Set up recursive list type
        val listTypeVar = TypeVariable.fresh()
        val listType = RecursiveType("List", listTypeVar, UnionType.of(
            Types.literal("Nil"),
            RecordType(mapOf(
                "value" to Types.Int,
                "next" to listTypeVar
            ))
        ))
        
        val env = TypeEnvironment.empty()
            .bind("list", TypeScheme.monomorphic(listType))
        
        val generator = ConstraintGenerator(env)
        val result = generator.generateConstraints(matchExpr)
        
        assertTrue(result.isSuccess(), "Recursive function on recursive type should succeed")
        
        val solver = ConstraintSolver()
        val solverResult = solver.solve(result.constraints)
        
        if (solverResult is ConstraintSolverResult.Failure) {
            fail<Unit>("Constraint solving failed: ${solverResult.error}")
        }
        
        val finalType = (solverResult as ConstraintSolverResult.Success).substitution.apply(result.type)
        assertEquals(Types.Int, finalType, "Recursive function should return Int")
    }

    // ===== UNIFICATION WITH RECURSIVE TYPES TESTS =====

    @Test
    fun `unify recursive type with itself`() {
        val listTypeVar = TypeVariable.fresh()
        val listType = RecursiveType("List", listTypeVar, UnionType.of(
            Types.literal("Nil"),
            RecordType(mapOf(
                "value" to Types.Int,
                "next" to listTypeVar
            ))
        ))
        
        val result = Unification.unify(listType, listType)
        assertTrue(result.isSuccess(), "Recursive type should unify with itself")
        assertEquals(Substitution.empty, result.getSubstitution(), "Self-unification should yield empty substitution")
    }

    @Test
    fun `unify equivalent recursive types`() {
        // Two identical recursive type definitions should unify
        val listTypeVar1 = TypeVariable.fresh()
        val listType1 = RecursiveType("List", listTypeVar1, UnionType.of(
            Types.literal("Nil"),
            RecordType(mapOf(
                "value" to Types.Int,
                "next" to listTypeVar1
            ))
        ))
        
        val listTypeVar2 = TypeVariable.fresh()
        val listType2 = RecursiveType("List", listTypeVar2, UnionType.of(
            Types.literal("Nil"),
            RecordType(mapOf(
                "value" to Types.Int,
                "next" to listTypeVar2
            ))
        ))
        
        val result = Unification.unify(listType1, listType2)
        assertTrue(result.isSuccess(), "Equivalent recursive types should unify")
    }

    @Test
    fun `unify incompatible recursive types`() {
        // List and Tree should not unify
        val listTypeVar = TypeVariable.fresh()
        val listType = RecursiveType("List", listTypeVar, UnionType.of(
            Types.literal("Nil"),
            RecordType(mapOf(
                "value" to Types.Int,
                "next" to listTypeVar
            ))
        ))
        
        val treeTypeVar = TypeVariable.fresh()
        val treeType = RecursiveType("Tree", treeTypeVar, UnionType.of(
            RecordType(mapOf("value" to Types.Int)),
            RecordType(mapOf(
                "left" to treeTypeVar,
                "value" to Types.Int,
                "right" to treeTypeVar
            ))
        ))
        
        val result = Unification.unify(listType, treeType)
        assertTrue(result.isFailure(), "Incompatible recursive types should not unify")
    }

    @Test
    fun `unify union type with literal string`() {
        // Test that "Nil" | SomeOtherType can unify with "Nil"
        val unionType = UnionType.of(
            Types.literal("Nil"),
            Types.Int
        )
        
        val literalType = Types.literal("Nil")
        
        val result = Unification.unify(unionType, literalType)
        assertTrue(result.isSuccess(), "Union type should unify with matching literal")
    }

    // ===== OCCURS CHECK WITH RECURSIVE TYPES TESTS =====

    @Test
    fun `occurs check prevents infinite types`() {
        // α = α -> α should fail occurs check
        val alpha = TypeVariable.fresh()
        val infiniteType = FunctionType(alpha, alpha)
        
        val result = Unification.unify(alpha, infiniteType)
        assertTrue(result.isFailure(), "Occurs check should prevent infinite types")
        
        val error = result.getError()!!
        assertTrue(error.contains("occurs") || error.contains("infinite"), 
            "Error should mention occurs check or infinite type: $error")
    }

    @Test
    fun `occurs check with recursive type is allowed`() {
        // α = List where List = Nil | Cons(Int, α) should be allowed
        // because the recursion is guarded by the constructor
        
        val alpha = TypeVariable.fresh()
        val listType = RecursiveType("List", alpha, UnionType.of(
            Types.literal("Nil"),
            RecordType(mapOf(
                "value" to Types.Int,
                "next" to alpha
            ))
        ))
        
        val result = Unification.unify(alpha, listType)
        assertTrue(result.isSuccess(), "Occurs check should allow guarded recursion")
    }

    @Test
    fun `nested occurs check with recursive types`() {
        // α = (α, α) should fail even with recursive type context
        val alpha = TypeVariable.fresh()
        val tupleType = TupleType(listOf(alpha, alpha))
        
        val result = Unification.unify(alpha, tupleType)
        assertTrue(result.isFailure(), "Occurs check should still prevent unguarded infinite types")
    }

    // ===== MUTUAL RECURSION TESTS =====

    @Test
    fun `mutual recursion between two types`() {
        // type A = AValue(B) | ABase
        // type B = BValue(A) | BBase
        
        val aTypeVar = TypeVariable.fresh()
        val bTypeVar = TypeVariable.fresh()
        
        val aType = RecursiveType("A", aTypeVar, UnionType.of(
            RecordType(mapOf("b" to bTypeVar)),
            Types.literal("ABase")
        ))
        
        val bType = RecursiveType("B", bTypeVar, UnionType.of(
            RecordType(mapOf("a" to aTypeVar)),
            Types.literal("BBase")
        ))
        
        // Both types should be well-formed
        assertEquals("A", aType.name)
        assertEquals("B", bType.name)
        
        // Verify mutual references
        val aBody = aType.body as UnionType
        val bBody = bType.body as UnionType
        
        val aRecordAlt = aBody.alternatives.find { it is RecordType } as RecordType
        val bRecordAlt = bBody.alternatives.find { it is RecordType } as RecordType
        
        assertEquals(bTypeVar, aRecordAlt.fields["b"])
        assertEquals(aTypeVar, bRecordAlt.fields["a"])
    }

    @Test
    fun `complex mutual recursion - expression and statement types`() {
        // type Expr = IntLit(Int) | BinOp(Expr, String, Expr) | Block(Stmt)
        // type Stmt = Assign(String, Expr) | If(Expr, Stmt, Stmt) | Expression(Expr)
        
        val exprTypeVar = TypeVariable.fresh()
        val stmtTypeVar = TypeVariable.fresh()
        
        val exprType = RecursiveType("Expr", exprTypeVar, UnionType.of(
            RecordType(mapOf("value" to Types.Int)),
            RecordType(mapOf(
                "left" to exprTypeVar,
                "op" to Types.String,
                "right" to exprTypeVar
            )),
            RecordType(mapOf("stmt" to stmtTypeVar))
        ))
        
        val stmtType = RecursiveType("Stmt", stmtTypeVar, UnionType.of(
            RecordType(mapOf(
                "var" to Types.String,
                "value" to exprTypeVar
            )),
            RecordType(mapOf(
                "condition" to exprTypeVar,
                "thenStmt" to stmtTypeVar,
                "elseStmt" to stmtTypeVar
            )),
            RecordType(mapOf("expr" to exprTypeVar))
        ))
        
        assertEquals("Expr", exprType.name)
        assertEquals("Stmt", stmtType.name)
        
        // Verify the mutual references exist
        val exprBody = exprType.body as UnionType
        val stmtBody = stmtType.body as UnionType
        
        assertEquals(3, exprBody.alternatives.size, "Expr should have 3 alternatives")
        assertEquals(3, stmtBody.alternatives.size, "Stmt should have 3 alternatives")
    }

    @Test
    fun `unification with mutually recursive types`() {
        // Test that mutually recursive types can be unified correctly
        val aVar1 = TypeVariable.fresh()
        val bVar1 = TypeVariable.fresh()
        
        val aType1 = RecursiveType("A", aVar1, UnionType.of(
            RecordType(mapOf("b" to bVar1)),
            Types.literal("ABase")
        ))
        
        val bType1 = RecursiveType("B", bVar1, UnionType.of(
            RecordType(mapOf("a" to aVar1)),
            Types.literal("BBase")
        ))
        
        // Create equivalent types with different variables
        val aVar2 = TypeVariable.fresh()
        val bVar2 = TypeVariable.fresh()
        
        val aType2 = RecursiveType("A", aVar2, UnionType.of(
            RecordType(mapOf("b" to bVar2)),
            Types.literal("ABase")
        ))
        
        val bType2 = RecursiveType("B", bVar2, UnionType.of(
            RecordType(mapOf("a" to aVar2)),
            Types.literal("BBase")
        ))
        
        // These should unify since they have the same structure
        val result = Unification.unify(aType1, aType2)
        assertTrue(result.isSuccess(), "Equivalent mutually recursive types should unify")
    }
} 