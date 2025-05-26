package io.littlelanguages.minibendu

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ParserTest {
    @Test
    fun `let declaration`() {
        // Test cases for basic let declarations
        listOf(
            "let x = 1 let y = x",
            "let x = (1) let y = x",
            "let x = (((1))) let y = ((x))"
        ).forEach { input ->
            val statements = successfulParse(input, 2)
            
            // First statement assertions (let x = ...)
            assertIs<ExprStmt>(statements[0], "First statement should be an ExprStmt")
            val firstExprStmt = statements[0] as ExprStmt
            assertIs<LetExpr>(firstExprStmt.expr, "Expected LetExpr in first statement")
            val firstLetExpr = firstExprStmt.expr

            assertEquals("x", firstLetExpr.id.value, "First let should declare variable 'x'")
            assertFalse(firstLetExpr.recursive, "Declaration should not be recursive")
            assertIs<LiteralIntExpr>(firstLetExpr.value, "Value should be a literal integer")
            val intExpr = firstLetExpr.value
            assertEquals(1, intExpr.value.value, "Integer value should be 1")

            // Second statement assertions (let y = ...)
            assertIs<ExprStmt>(statements[1], "Second statement should be an ExprStmt")
            val secondExprStmt = statements[1] as ExprStmt
            assertIs<LetExpr>(secondExprStmt.expr, "Expected LetExpr in second statement")
            val secondLetExpr = secondExprStmt.expr

            assertEquals("y", secondLetExpr.id.value, "Second let should declare variable 'y'")
            assertIs<VarExpr>(secondLetExpr.value, "Value should be a variable reference")
            val varExpr = secondLetExpr.value
            assertEquals("x", varExpr.id.value, "Variable reference should be 'x'")
        }

        // Test case for function declaration without type parameters
        listOf("let bob() = 1").forEach { input ->
            val statements = successfulParse(input, 1)

            assertIs<ExprStmt>(statements[0], "Statement should be an ExprStmt")
            val exprStmt = statements[0] as ExprStmt
            assertIs<LetExpr>(exprStmt.expr, "Expected LetExpr")
            val letExpr = exprStmt.expr

            assertEquals("bob", letExpr.id.value, "Function name should be 'bob'")
            assertNotNull(letExpr.parameters, "Parameters should not be null")
            assertTrue(letExpr.parameters.isEmpty(), "Parameters list should be empty")
            assertIs<LiteralIntExpr>(letExpr.value, "Function body should be a literal integer")
            val intExpr = letExpr.value
            assertEquals(1, intExpr.value.value, "Function return value should be 1")
        }

        // Test case for function declaration with type parameters and arguments
        listOf("let add[A](a: X, b: X): X = f(a, b)").forEach { input ->
            val statements = successfulParse(input, 1)

            assertIs<ExprStmt>(statements[0], "Statement should be an ExprStmt")
            val exprStmt = statements[0] as ExprStmt
            assertIs<LetExpr>(exprStmt.expr, "Expected LetExpr")
            val letExpr = exprStmt.expr

            assertEquals("add", letExpr.id.value, "Function name should be 'add'")

            // Verify type parameters
            assertNotNull(letExpr.typeParams, "Type parameters should not be null")
            assertEquals(1, letExpr.typeParams.size, "Should have one type parameter")
            assertEquals("A", letExpr.typeParams[0].id.value, "Type parameter should be 'A'")

            // Verify function parameters
            assertNotNull(letExpr.parameters, "Parameters should not be null")
            assertEquals(2, letExpr.parameters.size, "Should have two parameters")
            assertEquals("a", letExpr.parameters[0].id.value, "First parameter should be 'a'")
            assertEquals("b", letExpr.parameters[1].id.value, "Second parameter should be 'b'")

            // Verify type annotation
            assertNotNull(letExpr.typeAnnotation, "Type annotation should not be null")
            assertIs<BaseTypeExpr>(letExpr.typeAnnotation, "Return type should be a BaseTypeExpr")
            val returnType = letExpr.typeAnnotation
            assertEquals("X", returnType.id.value, "Return type should be 'X'")

            // Verify function body is a function application
            assertIs<ApplicationExpr>(letExpr.value, "Function body should be an application expression")
            val appExpr = letExpr.value
            assertIs<VarExpr>(appExpr.function, "Function should be a variable reference")
            val funcVar = appExpr.function
            assertEquals("f", funcVar.id.value, "Function name should be 'f'")

            // Verify function arguments
            assertEquals(2, appExpr.arguments.size, "Should have two arguments")
            assertIs<VarExpr>(appExpr.arguments[0], "First argument should be a variable reference")
            assertEquals("a", (appExpr.arguments[0] as VarExpr).id.value, "First argument should be 'a'")
            assertIs<VarExpr>(appExpr.arguments[1], "Second argument should be a variable reference")
            assertEquals("b", (appExpr.arguments[1] as VarExpr).id.value, "Second argument should be 'b'")
        }
    }

    @Test
    fun `apply expression`() {
        val testCases = listOf(
            Pair("f()", 0),
            Pair("f(1)", 1),
            Pair("f(1, 2)", 2),
            Pair("f(1, 2, 3)", 3),
            Pair("f(1, 2, 3, 4)", 4),
            Pair("f(1, 2, 3, 4, 5)", 5)
        )
        
        testCases.forEach { (input, expectedArgCount) ->
            val statements = successfulParse(input, 1)
            
            // Verify statement structure
            assertIs<ExprStmt>(statements[0], "Top level should be an ExprStmt")
            val exprStmt = statements[0] as ExprStmt
            
            // Verify it's an application expression
            assertIs<ApplicationExpr>(exprStmt.expr, "Expression should be an ApplicationExpr")
            val appExpr = exprStmt.expr
            
            // Verify the function being called
            assertIs<VarExpr>(appExpr.function, "Function should be a variable reference")
            val funcVar = appExpr.function
            assertEquals("f", funcVar.id.value, "Function name should be 'f'")
            
            // Verify argument count
            assertEquals(expectedArgCount, appExpr.arguments.size, 
                "Expected $expectedArgCount arguments in '$input', found ${appExpr.arguments.size}")
            
            // Verify each argument if there are any
            if (expectedArgCount > 0) {
                appExpr.arguments.forEachIndexed { index, arg ->
                    assertIs<LiteralIntExpr>(arg, "Argument $index should be a LiteralIntExpr")
                    val intArg = arg
                    assertEquals(index + 1, intArg.value.value, 
                        "Argument $index should have value ${index + 1}")
                }
            }
            
            // Verify location information exists
            assertNotNull(appExpr.location, "Application expression should have location information")
            assertNotNull(funcVar.location(), "Function variable should have location information")
        }
        
        // Test nested function applications
        val nestedInput = "f(g(1), h(2, 3))"
        val statements = successfulParse(nestedInput, 1)
        
        assertIs<ExprStmt>(statements[0])
        val exprStmt = statements[0] as ExprStmt
        assertIs<ApplicationExpr>(exprStmt.expr)
        val appExpr = exprStmt.expr
        
        // Verify outer function
        assertIs<VarExpr>(appExpr.function)
        assertEquals("f", appExpr.function.id.value)
        assertEquals(2, appExpr.arguments.size)
        
        // Verify first nested call: g(1)
        assertIs<ApplicationExpr>(appExpr.arguments[0])
        val firstArg = appExpr.arguments[0] as ApplicationExpr
        assertIs<VarExpr>(firstArg.function)
        assertEquals("g", firstArg.function.id.value)
        assertEquals(1, firstArg.arguments.size)
        assertIs<LiteralIntExpr>(firstArg.arguments[0])
        assertEquals(1, (firstArg.arguments[0] as LiteralIntExpr).value.value)
        
        // Verify second nested call: h(2, 3)
        assertIs<ApplicationExpr>(appExpr.arguments[1])
        val secondArg = appExpr.arguments[1] as ApplicationExpr
        assertIs<VarExpr>(secondArg.function)
        assertEquals("h", secondArg.function.id.value)
        assertEquals(2, secondArg.arguments.size)
        assertIs<LiteralIntExpr>(secondArg.arguments[0])
        assertEquals(2, (secondArg.arguments[0] as LiteralIntExpr).value.value)
        assertIs<LiteralIntExpr>(secondArg.arguments[1])
        assertEquals(3, (secondArg.arguments[1] as LiteralIntExpr).value.value)
    }

    @Test
    fun `binary op expressions`() {
        listOf(
            Pair("1 + 2", BinaryOp.Plus),
            Pair("1 - 2", BinaryOp.Minus),
            Pair("1 * 2", BinaryOp.Star),
            Pair("1 / 2", BinaryOp.Slash),
            Pair("\"a\" + \"b\"", BinaryOp.Plus),
            Pair("a && b", BinaryOp.And),
            Pair("a || b", BinaryOp.Or)
        ).forEach { input ->
            val statements = successfulParse(input.first, 1)
            
            // Verify the statement structure
            assertIs<ExprStmt>(statements[0])
            val exprStmt = statements[0] as ExprStmt
            
            // Verify this is a binary expression
            assertIs<BinaryOpExpr>(exprStmt.expr)
            val binOpExpr = exprStmt.expr
            
            // Verify the operator matches what we expect
            assertEquals(input.second, binOpExpr.op, "Expected operator ${input.second} but found ${binOpExpr.op} in expression ${input.first}")
            
            // Verify that left and right operands are not null
            assertNotNull(binOpExpr.left, "Left operand is null in ${input.first}")
            assertNotNull(binOpExpr.right, "Right operand is null in ${input.first}")
        }
    }

    @Test
    fun `parser error`() {
        val input = "let x = (1; let y = z"
        val errors = Errors()
        parse(input, errors)

        assertTrue(errors.hasErrors())
        assertIs<ParsingError>(errors[0])
    }

    @Test
    fun `literal over and under flows`() {
        listOf(
            "2147483648",
            "-2147483649"
        ).forEach { input ->
            val errors = Errors()
            parse(input, errors)
            assertTrue(errors.hasErrors())
            assertIs<InvalidLiteralError>(errors[0])
        }
    }

    @Test
    fun `literal string`() {
        listOf(
            Pair("\"\"", ""),
            Pair("\"'hello'\"", "'hello'"),
            Pair("\"hello world\"", "hello world"),
            Pair("\"[\\n]\"", "[\n]"),
            Pair("\"[\\\\]\"", "[\\]"),
            Pair("\"[\\\"]\"", "[\"]"),
            Pair("\"[\\x32;]\"", "[ ]"),
        ).forEach { input ->
            val statements = successfulParse(input.first, 1)

            assertIs<ExprStmt>(statements[0])
            val exprStmt = statements[0] as ExprStmt
            assertIs<LiteralStringExpr>(exprStmt.expr)
            val literalStringExpr = exprStmt.expr
            assertEquals(literalStringExpr.value.value, input.second)
        }
    }
    
    @Test
    fun `boolean literals`() {
        listOf(
            Pair("True", true),
            Pair("False", false)
        ).forEach { (input, expected) ->
            val statements = successfulParse(input, 1)
            
            assertIs<ExprStmt>(statements[0], "Statement should be an ExprStmt")
            val exprStmt = statements[0] as ExprStmt
            assertIs<LiteralBoolExpr>(exprStmt.expr, "Expression should be a LiteralBoolExpr")
            val boolExpr = exprStmt.expr
            assertEquals(expected, boolExpr.value.value, "Boolean value should match expected")
        }
    }
    
    @Test
    fun `if expressions`() {
        val input = "if x then y else z"
        val statements = successfulParse(input, 1)
        
        assertIs<ExprStmt>(statements[0], "Statement should be an ExprStmt")
        val exprStmt = statements[0] as ExprStmt
        assertIs<IfExpr>(exprStmt.expr, "Expression should be an IfExpr")
        val ifExpr = exprStmt.expr
        
        // Test condition
        assertIs<VarExpr>(ifExpr.condition, "Condition should be a variable")
        assertEquals("x", ifExpr.condition.id.value, "Condition variable should be 'x'")
        
        // Test then branch
        assertIs<VarExpr>(ifExpr.thenBranch, "Then branch should be a variable")
        assertEquals("y", ifExpr.thenBranch.id.value, "Then branch variable should be 'y'")
        
        // Test else branch
        assertIs<VarExpr>(ifExpr.elseBranch, "Else branch should be a variable")
        assertEquals("z", ifExpr.elseBranch.id.value, "Else branch variable should be 'z'")
        
        // Test nested if expressions
        val nestedInput = "if a then if b then c else d else e"
        val nestedStatements = successfulParse(nestedInput, 1)
        
        assertIs<ExprStmt>(nestedStatements[0])
        val nestedExprStmt = nestedStatements[0] as ExprStmt
        assertIs<IfExpr>(nestedExprStmt.expr)
        val nestedIfExpr = nestedExprStmt.expr
        
        assertIs<VarExpr>(nestedIfExpr.condition)
        assertEquals("a", nestedIfExpr.condition.id.value)
        
        assertIs<IfExpr>(nestedIfExpr.thenBranch)
        val thenIfExpr = nestedIfExpr.thenBranch
        assertIs<VarExpr>(thenIfExpr.condition)
        assertEquals("b", thenIfExpr.condition.id.value)
        assertIs<VarExpr>(thenIfExpr.thenBranch)
        assertEquals("c", thenIfExpr.thenBranch.id.value)
        assertIs<VarExpr>(thenIfExpr.elseBranch)
        assertEquals("d", thenIfExpr.elseBranch.id.value)
        
        assertIs<VarExpr>(nestedIfExpr.elseBranch)
        assertEquals("e", nestedIfExpr.elseBranch.id.value)
    }
    
    @Test
    fun `match expressions`() {
        val input = "match x with a => 1 | b => 2 | _ => 3"
        val statements = successfulParse(input, 1)
        
        assertIs<ExprStmt>(statements[0], "Statement should be an ExprStmt")
        val exprStmt = statements[0] as ExprStmt
        assertIs<MatchExpr>(exprStmt.expr, "Expression should be a MatchExpr")
        val matchExpr = exprStmt.expr
        
        // Test scrutinee
        assertIs<VarExpr>(matchExpr.scrutinee, "Scrutinee should be a variable")
        assertEquals("x", matchExpr.scrutinee.id.value, "Scrutinee should be 'x'")
        
        // Test cases
        assertEquals(3, matchExpr.cases.size, "Should have 3 match cases")
        
        // First case
        assertIs<VarPattern>(matchExpr.cases[0].pattern, "First pattern should be a variable pattern")
        assertEquals("a", (matchExpr.cases[0].pattern as VarPattern).id.value, "First pattern variable should be 'a'")
        assertIs<LiteralIntExpr>(matchExpr.cases[0].body, "First case body should be an integer literal")
        assertEquals(1, (matchExpr.cases[0].body as LiteralIntExpr).value.value, "First case should return 1")
        
        // Second case
        assertIs<VarPattern>(matchExpr.cases[1].pattern, "Second pattern should be a variable pattern")
        assertEquals("b", (matchExpr.cases[1].pattern as VarPattern).id.value, "Second pattern variable should be 'b'")
        assertIs<LiteralIntExpr>(matchExpr.cases[1].body, "Second case body should be an integer literal")
        assertEquals(2, (matchExpr.cases[1].body as LiteralIntExpr).value.value, "Second case should return 2")
        
        // Third case (wildcard)
        assertIs<WildcardPattern>(matchExpr.cases[2].pattern, "Third pattern should be a wildcard pattern")
        assertIs<LiteralIntExpr>(matchExpr.cases[2].body, "Third case body should be an integer literal")
        assertEquals(3, (matchExpr.cases[2].body as LiteralIntExpr).value.value, "Third case should return 3")
        
        // Test pattern types
        val patternInput = "match x with 1 => \"int\" | \"hello\" => \"string\" | True => \"bool\" | _ => \"unknown\""
        val patternStatements = successfulParse(patternInput, 1)
        
        val patternExprStmt = patternStatements[0] as ExprStmt
        val patternMatchExpr = patternExprStmt.expr as MatchExpr
        
        assertEquals(4, patternMatchExpr.cases.size, "Should have 4 match cases")
        
        assertIs<LiteralIntPattern>(patternMatchExpr.cases[0].pattern, "First pattern should be an int pattern")
        assertEquals(1, (patternMatchExpr.cases[0].pattern as LiteralIntPattern).value.value)
        
        assertIs<LiteralStringPattern>(patternMatchExpr.cases[1].pattern, "Second pattern should be a string pattern")
        assertEquals("hello", (patternMatchExpr.cases[1].pattern as LiteralStringPattern).value.value)
        
        assertIs<LiteralBoolPattern>(patternMatchExpr.cases[2].pattern, "Third pattern should be a bool pattern")
        assertEquals(true, (patternMatchExpr.cases[2].pattern as LiteralBoolPattern).value.value)
    }
    
    @Test
    fun `record expressions`() {
        val input = "{ x = 1, y = 2 }"
        val statements = successfulParse(input, 1)
        
        assertIs<ExprStmt>(statements[0], "Statement should be an ExprStmt")
        val exprStmt = statements[0] as ExprStmt
        assertIs<RecordExpr>(exprStmt.expr, "Expression should be a RecordExpr")
        val recordExpr = exprStmt.expr
        
        assertEquals(2, recordExpr.fields.size, "Record should have 2 fields")
        
        // Check first field
        assertIs<FieldExpr>(recordExpr.fields[0], "First field should be a FieldExpr")
        val field1 = recordExpr.fields[0] as FieldExpr
        assertEquals("x", field1.id.value, "First field name should be 'x'")
        assertIs<LiteralIntExpr>(field1.value, "First field value should be an integer literal")
        assertEquals(1, field1.value.value.value, "First field value should be 1")
        
        // Check second field
        assertIs<FieldExpr>(recordExpr.fields[1], "Second field should be a FieldExpr")
        val field2 = recordExpr.fields[1] as FieldExpr
        assertEquals("y", field2.id.value, "Second field name should be 'y'")
        assertIs<LiteralIntExpr>(field2.value, "Second field value should be an integer literal")
        assertEquals(2, field2.value.value.value, "Second field value should be 2")
        
        // Test record with spread
        val spreadInput = "{ ...a, b = 3 }"
        val spreadStatements = successfulParse(spreadInput, 1)
        
        val spreadExprStmt = spreadStatements[0] as ExprStmt
        val spreadRecordExpr = spreadExprStmt.expr as RecordExpr
        
        assertEquals(2, spreadRecordExpr.fields.size, "Record should have 2 fields")
        
        assertIs<SpreadExpr>(spreadRecordExpr.fields[0], "First field should be a SpreadExpr")
        val spreadField = spreadRecordExpr.fields[0] as SpreadExpr
        assertIs<VarExpr>(spreadField.expr, "Spread expression should be a variable")
        assertEquals("a", spreadField.expr.id.value, "Spread variable should be 'a'")
        
        assertIs<FieldExpr>(spreadRecordExpr.fields[1], "Second field should be a FieldExpr")
        assertEquals("b", (spreadRecordExpr.fields[1] as FieldExpr).id.value, "Field name should be 'b'")
    }
    
    @Test
    fun `field access expressions`() {
        val input = "a.b"
        val statements = successfulParse(input, 1)
        
        assertIs<ExprStmt>(statements[0], "Statement should be an ExprStmt")
        val exprStmt = statements[0] as ExprStmt
        assertIs<ProjectionExpr>(exprStmt.expr, "Expression should be a ProjectionExpr")
        val projExpr = exprStmt.expr
        
        assertIs<VarExpr>(projExpr.target, "Target should be a variable")
        assertEquals("a", projExpr.target.id.value, "Target variable should be 'a'")
        assertEquals("b", projExpr.field.value, "Field name should be 'b'")
        
        // Test chained field access
        val chainedInput = "a.b.c"
        val chainedStatements = successfulParse(chainedInput, 1)
        
        val chainedExprStmt = chainedStatements[0] as ExprStmt
        val chainedProjExpr = chainedExprStmt.expr as ProjectionExpr
        
        assertEquals("c", chainedProjExpr.field.value, "Outer field name should be 'c'")
        
        assertIs<ProjectionExpr>(chainedProjExpr.target, "Inner expression should be a ProjectionExpr")
        val innerProjExpr = chainedProjExpr.target
        assertEquals("b", innerProjExpr.field.value, "Inner field name should be 'b'")
        assertIs<VarExpr>(innerProjExpr.target, "Innermost target should be a variable")
        assertEquals("a", innerProjExpr.target.id.value, "Innermost target should be 'a'")
    }
    
    @Test
    fun `tuple expressions`() {
        val input = "(1, \"hello\", True)"
        val statements = successfulParse(input, 1)
        
        assertIs<ExprStmt>(statements[0], "Statement should be an ExprStmt")
        val exprStmt = statements[0] as ExprStmt
        assertIs<TupleExpr>(exprStmt.expr, "Expression should be a TupleExpr")
        val tupleExpr = exprStmt.expr
        
        assertEquals(3, tupleExpr.elements.size, "Tuple should have 3 elements")
        
        // Check first element
        assertIs<LiteralIntExpr>(tupleExpr.elements[0], "First element should be an integer literal")
        assertEquals(1, (tupleExpr.elements[0] as LiteralIntExpr).value.value, "First element should be 1")
        
        // Check second element
        assertIs<LiteralStringExpr>(tupleExpr.elements[1], "Second element should be a string literal")
        assertEquals("hello", (tupleExpr.elements[1] as LiteralStringExpr).value.value, "Second element should be 'hello'")
        
        // Check third element
        assertIs<LiteralBoolExpr>(tupleExpr.elements[2], "Third element should be a boolean literal")
        assertEquals(true, (tupleExpr.elements[2] as LiteralBoolExpr).value.value, "Third element should be True")
    }
    
    @Test
    fun `lambda expressions`() {
        val input = "\\x => x + 1"
        val statements = successfulParse(input, 1)
        
        assertIs<ExprStmt>(statements[0], "Statement should be an ExprStmt")
        val exprStmt = statements[0] as ExprStmt
        assertIs<LambdaExpr>(exprStmt.expr, "Expression should be a LambdaExpr")
        val lambdaExpr = exprStmt.expr
        
        assertEquals("x", lambdaExpr.param.value, "Parameter name should be 'x'")
        assertNull(lambdaExpr.typeParams, "Type parameters should be null")
        assertNull(lambdaExpr.paramType, "Parameter type should be null")
        
        assertIs<BinaryOpExpr>(lambdaExpr.body, "Body should be a binary expression")
        val bodyExpr = lambdaExpr.body
        assertEquals(BinaryOp.Plus, bodyExpr.op, "Operator should be Plus")
        assertIs<VarExpr>(bodyExpr.left, "Left operand should be a variable")
        assertEquals("x", bodyExpr.left.id.value, "Left operand should be 'x'")
        assertIs<LiteralIntExpr>(bodyExpr.right, "Right operand should be an integer literal")
        assertEquals(1, bodyExpr.right.value.value, "Right operand should be 1")
        
        // Test lambda with type parameter and type annotation
        val typedInput = "\\[A] x: A => x"
        val typedStatements = successfulParse(typedInput, 1)
        
        val typedExprStmt = typedStatements[0] as ExprStmt
        val typedLambdaExpr = typedExprStmt.expr as LambdaExpr
        
        assertNotNull(typedLambdaExpr.typeParams, "Type parameters should not be null")
        assertEquals(1, typedLambdaExpr.typeParams.size, "Should have 1 type parameter")
        assertEquals("A", typedLambdaExpr.typeParams[0].id.value, "Type parameter should be 'A'")
        
        assertNotNull(typedLambdaExpr.paramType, "Parameter type should not be null")
        assertIs<BaseTypeExpr>(typedLambdaExpr.paramType, "Parameter type should be a BaseTypeExpr")
        assertEquals("A", typedLambdaExpr.paramType.id.value, "Parameter type should be 'A'")
    }
    
    @Test
    fun `type alias declarations`() {
        val input = "type User = { name: String, age: Int }"
        val statements = successfulParse(input, 1)
        
        assertIs<TypeAliasDecl>(statements[0], "Top level should be a TypeAliasDecl")
        val typeDecl = statements[0] as TypeAliasDecl
        
        assertEquals("User", typeDecl.id.value, "Type name should be 'User'")
        assertNull(typeDecl.typeParams, "Type parameters should be null")
        
        assertIs<RecordTypeExpr>(typeDecl.typeExpr, "Type expression should be a RecordTypeExpr")
        val recordType = typeDecl.typeExpr
        
        assertEquals(2, recordType.fields.size, "Record type should have 2 fields")
        
        assertEquals("name", recordType.fields[0].id.value, "First field name should be 'name'")
        assertIs<BaseTypeExpr>(recordType.fields[0].type, "First field type should be a BaseTypeExpr")
        assertEquals("String", (recordType.fields[0].type as BaseTypeExpr).id.value, "First field type should be 'String'")
        
        assertEquals("age", recordType.fields[1].id.value, "Second field name should be 'age'")
        assertIs<BaseTypeExpr>(recordType.fields[1].type, "Second field type should be a BaseTypeExpr")
        assertEquals("Int", (recordType.fields[1].type as BaseTypeExpr).id.value, "Second field type should be 'Int'")
        
        // Test generic type alias
        val genericInput = "type List[A] = Nil | Cons[A, List[A]]"
        val genericStatements = successfulParse(genericInput, 1)
        
        val genericTypeDecl = genericStatements[0] as TypeAliasDecl
        
        assertEquals("List", genericTypeDecl.id.value, "Type name should be 'List'")
        assertNotNull(genericTypeDecl.typeParams, "Type parameters should not be null")
        assertEquals(1, genericTypeDecl.typeParams.size, "Should have 1 type parameter")
        assertEquals("A", genericTypeDecl.typeParams[0].id.value, "Type parameter should be 'A'")
        
        assertIs<UnionTypeExpr>(genericTypeDecl.typeExpr, "Type expression should be a UnionTypeExpr")
    }

    @Test
    fun `equality expressions`() {
        listOf(
            Pair("a == b", BinaryOp.EqualEqual),
            Pair("a != b", BinaryOp.NotEqual),
            Pair("1 == 2", BinaryOp.EqualEqual),
            Pair("\"hello\" != \"world\"", BinaryOp.NotEqual),
            Pair("True == False", BinaryOp.EqualEqual)
        ).forEach { (input, expectedOp) ->
            val statements = successfulParse(input, 1)
            
            // Verify the statement structure
            assertIs<ExprStmt>(statements[0], "Statement should be an ExprStmt")
            val exprStmt = statements[0] as ExprStmt
            
            // Verify this is a binary expression
            assertIs<BinaryOpExpr>(exprStmt.expr, "Expression should be a BinaryOpExpr")
            val binOpExpr = exprStmt.expr
            
            // Verify the operator
            assertEquals(expectedOp, binOpExpr.op, 
                "Expected operator $expectedOp but found ${binOpExpr.op} in expression $input")
            
            // Verify operands are not null
            assertNotNull(binOpExpr.left, "Left operand should not be null")
            assertNotNull(binOpExpr.right, "Right operand should not be null")
            
            // Verify locations
            assertNotNull(binOpExpr.location, "Binary expression should have location information")
        }
        
        // Test chained equality expressions
        val chainedInput = "a == b && c != d"
        val statements = successfulParse(chainedInput, 1)
        
        assertIs<ExprStmt>(statements[0])
        val exprStmt = statements[0] as ExprStmt
        assertIs<BinaryOpExpr>(exprStmt.expr)
        val andExpr = exprStmt.expr
        
        assertEquals(BinaryOp.And, andExpr.op, "Outer operator should be And")
        
        // Left side of AND should be == expression
        assertIs<BinaryOpExpr>(andExpr.left)
        val leftEqExpr = andExpr.left
        assertEquals(BinaryOp.EqualEqual, leftEqExpr.op, "Left side operator should be EqualEqual")
        assertIs<VarExpr>(leftEqExpr.left)
        assertEquals("a", leftEqExpr.left.id.value, "Left operand should be 'a'")
        assertIs<VarExpr>(leftEqExpr.right)
        assertEquals("b", leftEqExpr.right.id.value, "Right operand should be 'b'")
        
        // Right side of AND should be != expression
        assertIs<BinaryOpExpr>(andExpr.right)
        val rightEqExpr = andExpr.right
        assertEquals(BinaryOp.NotEqual, rightEqExpr.op, "Right side operator should be NotEqual")
        assertIs<VarExpr>(rightEqExpr.left)
        assertEquals("c", rightEqExpr.left.id.value, "Left operand should be 'c'")
        assertIs<VarExpr>(rightEqExpr.right)
        assertEquals("d", rightEqExpr.right.id.value, "Right operand should be 'd'")
    }

    @Test
    fun `function type expressions`() {
        // Basic function type
        val input = "type Func = Int -> String"
        val statements = successfulParse(input, 1)
        
        assertIs<TypeAliasDecl>(statements[0], "Top level should be a TypeAliasDecl")
        val typeDecl = statements[0] as TypeAliasDecl
        
        assertEquals("Func", typeDecl.id.value, "Type name should be 'Func'")
        
        assertIs<FunctionTypeExpr>(typeDecl.typeExpr, "Type expression should be a FunctionTypeExpr")
        val funcType = typeDecl.typeExpr
        
        assertIs<BaseTypeExpr>(funcType.from, "From type should be a BaseTypeExpr")
        assertEquals("Int", funcType.from.id.value, "From type should be 'Int'")
        
        assertIs<BaseTypeExpr>(funcType.to, "To type should be a BaseTypeExpr")
        assertEquals("String", funcType.to.id.value, "To type should be 'String'")
        
        // Multi-parameter function type
        val multiInput = "type MultiFunc = Int -> String -> Bool"
        val multiStatements = successfulParse(multiInput, 1)
        
        val multiTypeDecl = multiStatements[0] as TypeAliasDecl
        val multiFuncType = multiTypeDecl.typeExpr as FunctionTypeExpr
        
        // First parameter should be Int
        assertIs<BaseTypeExpr>(multiFuncType.from)
        assertEquals("Int", multiFuncType.from.id.value, "First parameter should be 'Int'")
        
        // To part should be another function type (String -> Bool)
        assertIs<FunctionTypeExpr>(multiFuncType.to, "To type should be another FunctionTypeExpr")
        val nestedFuncType = multiFuncType.to
        
        // Second parameter should be String
        assertIs<BaseTypeExpr>(nestedFuncType.from)
        assertEquals("String", nestedFuncType.from.id.value, "Second parameter should be 'String'")
        
        // Return type should be Bool
        assertIs<BaseTypeExpr>(nestedFuncType.to)
        assertEquals("Bool", nestedFuncType.to.id.value, "Return type should be 'Bool'")
        
        // Complex function type with tuple parameter and generic return
        val complexInput = "type ComplexFunc = (Int, String) -> List[A]"
        val complexStatements = successfulParse(complexInput, 1)
        
        val complexTypeDecl = complexStatements[0] as TypeAliasDecl
        val complexFuncType = complexTypeDecl.typeExpr as FunctionTypeExpr
        
        // Parameter should be a tuple type
        assertIs<TupleTypeExpr>(complexFuncType.from, "From type should be a TupleTypeExpr")
        val tupleType = complexFuncType.from
        
        assertEquals(2, tupleType.types.size, "Tuple should have 2 elements")
        assertIs<BaseTypeExpr>(tupleType.types[0])
        assertEquals("Int", (tupleType.types[0] as BaseTypeExpr).id.value, "First tuple element should be 'Int'")
        assertIs<BaseTypeExpr>(tupleType.types[1])
        assertEquals("String", (tupleType.types[1] as BaseTypeExpr).id.value, "Second tuple element should be 'String'")
        
        // Return type should be a generic type
        assertIs<BaseTypeExpr>(complexFuncType.to, "To type should be a BaseTypeExpr")
        val returnType = complexFuncType.to
        assertEquals("List", returnType.id.value, "Return type should be 'List'")
        assertNotNull(returnType.args, "Generic arguments should not be null")
        assertEquals(1, returnType.args.size, "Should have 1 generic argument")
        assertIs<BaseTypeExpr>(returnType.args[0])
        assertEquals("A", (returnType.args[0] as BaseTypeExpr).id.value, "Generic argument should be 'A'")
    }

    @Test
    fun `tuple patterns`() {
        val input = "match p with (x, y) => x + y | _ => 0"
        val statements = successfulParse(input, 1)
        
        assertIs<ExprStmt>(statements[0], "Statement should be an ExprStmt")
        val exprStmt = statements[0] as ExprStmt
        assertIs<MatchExpr>(exprStmt.expr, "Expression should be a MatchExpr")
        val matchExpr = exprStmt.expr
        
        // Test scrutinee
        assertIs<VarExpr>(matchExpr.scrutinee, "Scrutinee should be a variable")
        assertEquals("p", matchExpr.scrutinee.id.value, "Scrutinee should be 'p'")
        
        // Test cases
        assertEquals(2, matchExpr.cases.size, "Should have 2 match cases")
        
        // First case with tuple pattern
        assertIs<TuplePattern>(matchExpr.cases[0].pattern, "First pattern should be a tuple pattern")
        val tuplePattern = matchExpr.cases[0].pattern as TuplePattern
        
        assertEquals(2, tuplePattern.elements.size, "Tuple pattern should have 2 elements")
        
        // Check tuple elements
        assertIs<VarPattern>(tuplePattern.elements[0], "First tuple element should be a variable pattern")
        assertEquals("x", (tuplePattern.elements[0] as VarPattern).id.value, "First variable should be 'x'")
        
        assertIs<VarPattern>(tuplePattern.elements[1], "Second tuple element should be a variable pattern")
        assertEquals("y", (tuplePattern.elements[1] as VarPattern).id.value, "Second variable should be 'y'")
        
        // Check body of first case
        assertIs<BinaryOpExpr>(matchExpr.cases[0].body, "First case body should be a binary expression")
        val bodyExpr = matchExpr.cases[0].body as BinaryOpExpr
        assertEquals(BinaryOp.Plus, bodyExpr.op, "Operator should be Plus")
        assertIs<VarExpr>(bodyExpr.left, "Left operand should be a variable")
        assertEquals("x", bodyExpr.left.id.value, "Left operand should be 'x'")
        assertIs<VarExpr>(bodyExpr.right, "Right operand should be a variable")
        assertEquals("y", bodyExpr.right.id.value, "Right operand should be 'y'")
        
        // Second case (wildcard)
        assertIs<WildcardPattern>(matchExpr.cases[1].pattern, "Second pattern should be a wildcard pattern")
        assertIs<LiteralIntExpr>(matchExpr.cases[1].body, "Second case body should be an integer literal")
        assertEquals(0, (matchExpr.cases[1].body as LiteralIntExpr).value.value, "Second case should return 0")
        
        // Test nested tuple patterns
        val nestedInput = "match t with (a, (b, c)) => a + b + c | _ => 0"
        val nestedStatements = successfulParse(nestedInput, 1)
        
        val nestedMatchExpr = (nestedStatements[0] as ExprStmt).expr as MatchExpr
        
        // Check the nested tuple pattern
        val outerTuple = nestedMatchExpr.cases[0].pattern as TuplePattern
        assertEquals(2, outerTuple.elements.size, "Outer tuple should have 2 elements")
        
        assertIs<VarPattern>(outerTuple.elements[0], "First element should be a variable pattern")
        assertEquals("a", (outerTuple.elements[0] as VarPattern).id.value, "First variable should be 'a'")
        
        assertIs<TuplePattern>(outerTuple.elements[1], "Second element should be a tuple pattern")
        val innerTuple = outerTuple.elements[1] as TuplePattern
        assertEquals(2, innerTuple.elements.size, "Inner tuple should have 2 elements")
        
        assertIs<VarPattern>(innerTuple.elements[0], "First inner element should be a variable pattern")
        assertEquals("b", (innerTuple.elements[0] as VarPattern).id.value, "First inner variable should be 'b'")
        
        assertIs<VarPattern>(innerTuple.elements[1], "Second inner element should be a variable pattern")
        assertEquals("c", (innerTuple.elements[1] as VarPattern).id.value, "Second inner variable should be 'c'")
    }

    @Test
    fun `record patterns`() {
        val input = "match person with { name = n, age = a } => n + \" is \" + a | _ => \"unknown\""
        val statements = successfulParse(input, 1)
        
        assertIs<ExprStmt>(statements[0], "Statement should be an ExprStmt")
        val exprStmt = statements[0] as ExprStmt
        assertIs<MatchExpr>(exprStmt.expr, "Expression should be a MatchExpr")
        val matchExpr = exprStmt.expr
        
        // Test scrutinee
        assertIs<VarExpr>(matchExpr.scrutinee, "Scrutinee should be a variable")
        assertEquals("person", matchExpr.scrutinee.id.value, "Scrutinee should be 'person'")
        
        // Test cases
        assertEquals(2, matchExpr.cases.size, "Should have 2 match cases")
        
        // First case with record pattern
        assertIs<RecordPattern>(matchExpr.cases[0].pattern, "First pattern should be a record pattern")
        val recordPattern = matchExpr.cases[0].pattern as RecordPattern
        
        assertEquals(2, recordPattern.fields.size, "Record pattern should have 2 fields")
        
        // Check record fields
        assertEquals("name", recordPattern.fields[0].id.value, "First field should be 'name'")
        assertIs<VarPattern>(recordPattern.fields[0].pattern, "First field pattern should be a variable")
        assertEquals("n", (recordPattern.fields[0].pattern as VarPattern).id.value, "First binding should be 'n'")
        
        assertEquals("age", recordPattern.fields[1].id.value, "Second field should be 'age'")
        assertIs<VarPattern>(recordPattern.fields[1].pattern, "Second field pattern should be a variable")
        assertEquals("a", (recordPattern.fields[1].pattern as VarPattern).id.value, "Second binding should be 'a'")
        
        // Second case (wildcard)
        assertIs<WildcardPattern>(matchExpr.cases[1].pattern, "Second pattern should be a wildcard pattern")
        assertIs<LiteralStringExpr>(matchExpr.cases[1].body, "Second case body should be a string literal")
        assertEquals("unknown", (matchExpr.cases[1].body as LiteralStringExpr).value.value, "Second case should return 'unknown'")
        
        // Test nested record patterns
        val nestedInput = "match user with { info = { name = n, age = a }, active = True } => n | _ => \"inactive\""
        val nestedStatements = successfulParse(nestedInput, 1)
        
        val nestedMatchExpr = (nestedStatements[0] as ExprStmt).expr as MatchExpr
        
        // Check the nested record pattern
        val outerRecord = nestedMatchExpr.cases[0].pattern as RecordPattern
        assertEquals(2, outerRecord.fields.size, "Outer record should have 2 fields")
        
        assertEquals("info", outerRecord.fields[0].id.value, "First field should be 'info'")
        assertIs<RecordPattern>(outerRecord.fields[0].pattern, "First field pattern should be a record pattern")
        
        val innerRecord = outerRecord.fields[0].pattern as RecordPattern
        assertEquals(2, innerRecord.fields.size, "Inner record should have 2 fields")
        
        assertEquals("name", innerRecord.fields[0].id.value, "First inner field should be 'name'")
        assertIs<VarPattern>(innerRecord.fields[0].pattern, "First inner pattern should be a variable")
        assertEquals("n", (innerRecord.fields[0].pattern as VarPattern).id.value, "First inner binding should be 'n'")
        
        assertEquals("age", innerRecord.fields[1].id.value, "Second inner field should be 'age'")
        assertIs<VarPattern>(innerRecord.fields[1].pattern, "Second inner pattern should be a variable")
        assertEquals("a", (innerRecord.fields[1].pattern as VarPattern).id.value, "Second inner binding should be 'a'")
        
        assertEquals("active", outerRecord.fields[1].id.value, "Second field should be 'active'")
        assertIs<LiteralBoolPattern>(outerRecord.fields[1].pattern, "Second field pattern should be a boolean literal")
        assertEquals(true, (outerRecord.fields[1].pattern as LiteralBoolPattern).value.value, "Boolean value should be True")
    }

    @Test
    fun `complex nested expressions`() {
        // Simple nested expression
        val input = "if a == 1 then f(x) else g(y)"
        val statements = successfulParse(input, 1)
        
        assertIs<ExprStmt>(statements[0], "Statement should be an ExprStmt")
        val exprStmt = statements[0] as ExprStmt
        assertIs<IfExpr>(exprStmt.expr, "Expression should be an IfExpr")
        val ifExpr = exprStmt.expr
        
        // Test condition (a == 1)
        assertIs<BinaryOpExpr>(ifExpr.condition, "Condition should be a binary expression")
        val condExpr = ifExpr.condition
        assertEquals(BinaryOp.EqualEqual, condExpr.op, "Operator should be EqualEqual")
        assertIs<VarExpr>(condExpr.left, "Left operand should be a variable")
        assertEquals("a", condExpr.left.id.value, "Left operand should be 'a'")
        assertIs<LiteralIntExpr>(condExpr.right, "Right operand should be an integer literal")
        assertEquals(1, condExpr.right.value.value, "Right operand should be 1")
        
        // Test then branch (f(x))
        assertIs<ApplicationExpr>(ifExpr.thenBranch, "Then branch should be an application")
        val thenApp = ifExpr.thenBranch
        assertIs<VarExpr>(thenApp.function, "Function should be a variable")
        assertEquals("f", thenApp.function.id.value, "Function should be 'f'")
        assertEquals(1, thenApp.arguments.size, "Should have 1 argument")
        assertIs<VarExpr>(thenApp.arguments[0], "Argument should be a variable")
        assertEquals("x", (thenApp.arguments[0] as VarExpr).id.value, "Argument should be 'x'")
        
        // Test else branch (g(y))
        assertIs<ApplicationExpr>(ifExpr.elseBranch, "Else branch should be an application")
        val elseApp = ifExpr.elseBranch
        assertIs<VarExpr>(elseApp.function, "Function should be a variable")
        assertEquals("g", elseApp.function.id.value, "Function should be 'g'")
        assertEquals(1, elseApp.arguments.size, "Should have 1 argument")
        assertIs<VarExpr>(elseApp.arguments[0], "Argument should be a variable")
        assertEquals("y", (elseApp.arguments[0] as VarExpr).id.value, "Argument should be 'y'")
        
        // Test nested application with record
        val nestedAppInput = "f(g(x), { y = 1 })"
        val nestedAppStatements = successfulParse(nestedAppInput, 1)
        
        val nestedAppExpr = (nestedAppStatements[0] as ExprStmt).expr as ApplicationExpr
        
        // Check outer function
        assertIs<VarExpr>(nestedAppExpr.function, "Function should be a variable")
        assertEquals("f", nestedAppExpr.function.id.value, "Function should be 'f'")
        assertEquals(2, nestedAppExpr.arguments.size, "Should have 2 arguments")
        
        // Check first argument (g(x))
        assertIs<ApplicationExpr>(nestedAppExpr.arguments[0], "First argument should be an application")
        val firstArg = nestedAppExpr.arguments[0] as ApplicationExpr
        assertIs<VarExpr>(firstArg.function, "Function should be a variable")
        assertEquals("g", firstArg.function.id.value, "Function should be 'g'")
        assertEquals(1, firstArg.arguments.size, "Should have 1 argument")
        assertIs<VarExpr>(firstArg.arguments[0], "Argument should be a variable")
        assertEquals("x", (firstArg.arguments[0] as VarExpr).id.value, "Argument should be 'x'")
        
        // Check second argument ({ y = 1 })
        assertIs<RecordExpr>(nestedAppExpr.arguments[1], "Second argument should be a record")
        val secondArg = nestedAppExpr.arguments[1] as RecordExpr
        assertEquals(1, secondArg.fields.size, "Record should have 1 field")
        assertIs<FieldExpr>(secondArg.fields[0], "Field should be a FieldExpr")
        val fieldExpr = secondArg.fields[0] as FieldExpr
        assertEquals("y", fieldExpr.id.value, "Field name should be 'y'")
        assertIs<LiteralIntExpr>(fieldExpr.value, "Field value should be an integer literal")
        assertEquals(1, fieldExpr.value.value.value, "Field value should be 1")
    }

    @Test
    fun `recursive function declarations`() {
        val input = "let rec factorial(n: Int): Int = if n == 0 then 1 else n * factorial(n - 1)"
        val statements = successfulParse(input, 1)
        
        assertIs<ExprStmt>(statements[0], "Statement should be an ExprStmt")
        val exprStmt = statements[0] as ExprStmt
        assertIs<LetExpr>(exprStmt.expr, "Expression should be a LetExpr")
        val letExpr = exprStmt.expr
        
        // Verify recursive flag
        assertTrue(letExpr.recursive, "Function should be recursive")
        
        // Verify function name
        assertEquals("factorial", letExpr.id.value, "Function name should be 'factorial'")
        
        // Verify parameters
        assertNotNull(letExpr.parameters, "Parameters should not be null")
        assertEquals(1, letExpr.parameters.size, "Should have 1 parameter")
        assertEquals("n", letExpr.parameters[0].id.value, "Parameter name should be 'n'")
        
        // Verify parameter type
        assertNotNull(letExpr.parameters[0].type, "Parameter type should not be null")
        assertIs<BaseTypeExpr>(letExpr.parameters[0].type, "Parameter type should be a BaseTypeExpr")
        assertEquals("Int", (letExpr.parameters[0].type as BaseTypeExpr).id.value, "Parameter type should be 'Int'")
        
        // Verify return type
        assertNotNull(letExpr.typeAnnotation, "Return type should not be null")
        assertIs<BaseTypeExpr>(letExpr.typeAnnotation, "Return type should be a BaseTypeExpr")
        assertEquals("Int", letExpr.typeAnnotation.id.value, "Return type should be 'Int'")
        
        // Verify function body is an if expression
        assertIs<IfExpr>(letExpr.value, "Function body should be an if expression")
        val ifExpr = letExpr.value
        
        // Verify condition (n == 0)
        assertIs<BinaryOpExpr>(ifExpr.condition, "Condition should be a binary expression")
        val condExpr = ifExpr.condition
        assertEquals(BinaryOp.EqualEqual, condExpr.op, "Operator should be EqualEqual")
        assertIs<VarExpr>(condExpr.left, "Left operand should be a variable")
        assertEquals("n", condExpr.left.id.value, "Left operand should be 'n'")
        assertIs<LiteralIntExpr>(condExpr.right, "Right operand should be an integer literal")
        assertEquals(0, condExpr.right.value.value, "Right operand should be 0")
        
        // Verify then branch (1)
        assertIs<LiteralIntExpr>(ifExpr.thenBranch, "Then branch should be an integer literal")
        assertEquals(1, ifExpr.thenBranch.value.value, "Then branch should be 1")
        
        // Verify else branch (n * factorial(n - 1))
        assertIs<BinaryOpExpr>(ifExpr.elseBranch, "Else branch should be a binary expression")
        val elseExpr = ifExpr.elseBranch
        assertEquals(BinaryOp.Star, elseExpr.op, "Operator should be Star")
        
        // Left side of multiplication: n
        assertIs<VarExpr>(elseExpr.left, "Left operand should be a variable")
        assertEquals("n", elseExpr.left.id.value, "Left operand should be 'n'")
        
        // Right side of multiplication: factorial(n - 1)
        assertIs<ApplicationExpr>(elseExpr.right, "Right operand should be a function application")
        val appExpr = elseExpr.right
        
        // Function name
        assertIs<VarExpr>(appExpr.function, "Function should be a variable")
        assertEquals("factorial", appExpr.function.id.value, "Function should be 'factorial'")
        
        // Argument (n - 1)
        assertEquals(1, appExpr.arguments.size, "Should have 1 argument")
        assertIs<BinaryOpExpr>(appExpr.arguments[0], "Argument should be a binary expression")
        val argExpr = appExpr.arguments[0] as BinaryOpExpr
        
        assertEquals(BinaryOp.Minus, argExpr.op, "Operator should be Minus")
        assertIs<VarExpr>(argExpr.left, "Left operand should be a variable")
        assertEquals("n", argExpr.left.id.value, "Left operand should be 'n'")
        assertIs<LiteralIntExpr>(argExpr.right, "Right operand should be an integer literal")
        assertEquals(1, argExpr.right.value.value, "Right operand should be 1")
        
        // Test mutual recursion
        val mutualInput = "let rec isEven(n: Int): Bool = if n == 0 then True else isOdd(n - 1) let rec isOdd(n: Int): Bool = if n == 0 then False else isEven(n - 1)"
        val mutualStatements = successfulParse(mutualInput, 2)
        
        // Verify isEven function
        assertIs<ExprStmt>(mutualStatements[0])
        val isEvenStmt = mutualStatements[0] as ExprStmt
        assertIs<LetExpr>(isEvenStmt.expr)
        val isEvenExpr = isEvenStmt.expr
        
        assertEquals("isEven", isEvenExpr.id.value, "Function name should be 'isEven'")
        assertTrue(isEvenExpr.recursive, "Function should be recursive")
        
        // Verify isOdd function
        assertIs<ExprStmt>(mutualStatements[1])
        val isOddStmt = mutualStatements[1] as ExprStmt
        assertIs<LetExpr>(isOddStmt.expr)
        val isOddExpr = isOddStmt.expr
        
        assertEquals("isOdd", isOddExpr.id.value, "Function name should be 'isOdd'")
        assertTrue(isOddExpr.recursive, "Function should be recursive")
    }

    @Test
    fun `union and merge type expressions`() {
        // Test union type
        val unionInput = "type Result = Success | Failure"
        val unionStatements = successfulParse(unionInput, 1)
        
        assertIs<TypeAliasDecl>(unionStatements[0], "Top level should be a TypeAliasDecl")
        val typeDecl = unionStatements[0] as TypeAliasDecl
        
        assertEquals("Result", typeDecl.id.value, "Type name should be 'Result'")
        
        // Union types are constructed right-associatively, so we get:
        // Success | Failure => (Success | Failure)
        assertIs<UnionTypeExpr>(typeDecl.typeExpr, "Type expression should be a UnionTypeExpr")
        val unionType = typeDecl.typeExpr
        
        // The left side should be a BaseTypeExpr for Success
        assertIs<BaseTypeExpr>(unionType.left, "Left type should be a BaseTypeExpr")
        assertEquals("Success", unionType.left.id.value, "Left type should be 'Success'")
        
        // The right side should be a BaseTypeExpr for Failure
        assertIs<BaseTypeExpr>(unionType.right, "Right type should be a BaseTypeExpr")
        assertEquals("Failure", unionType.right.id.value, "Right type should be 'Failure'")
        
        // Test merge type expression (intersection type)
        val mergeInput = "type Combined = A & B"
        val mergeStatements = successfulParse(mergeInput, 1)
        
        val mergeTypeDecl = mergeStatements[0] as TypeAliasDecl
        
        assertEquals("Combined", mergeTypeDecl.id.value, "Type name should be 'Combined'")
        
        assertIs<MergeTypeExpr>(mergeTypeDecl.typeExpr, "Type expression should be a MergeTypeExpr")
        val mergeType = mergeTypeDecl.typeExpr
        
        assertIs<BaseTypeExpr>(mergeType.left, "Left type should be a BaseTypeExpr")
        assertEquals("A", mergeType.left.id.value, "Left type should be 'A'")
        
        assertIs<BaseTypeExpr>(mergeType.right, "Right type should be a BaseTypeExpr")
        assertEquals("B", mergeType.right.id.value, "Right type should be 'B'")
    }

    @Test
    fun `tuple type expressions`() {
        val input = "type Point = (Int, Int)"
        val statements = successfulParse(input, 1)
        
        assertIs<TypeAliasDecl>(statements[0], "Top level should be a TypeAliasDecl")
        val typeDecl = statements[0] as TypeAliasDecl
        
        assertEquals("Point", typeDecl.id.value, "Type name should be 'Point'")
        
        assertIs<TupleTypeExpr>(typeDecl.typeExpr, "Type expression should be a TupleTypeExpr")
        val tupleType = typeDecl.typeExpr
        
        assertEquals(2, tupleType.types.size, "Tuple should have 2 elements")
        
        // Check tuple elements
        assertIs<BaseTypeExpr>(tupleType.types[0], "First type should be a BaseTypeExpr")
        assertEquals("Int", (tupleType.types[0] as BaseTypeExpr).id.value, "First type should be 'Int'")
        
        assertIs<BaseTypeExpr>(tupleType.types[1], "Second type should be a BaseTypeExpr")
        assertEquals("Int", (tupleType.types[1] as BaseTypeExpr).id.value, "Second type should be 'Int'")
        
        // Test heterogeneous tuple types
        val heterogeneousInput = "type Triple = (Int, String, Bool)"
        val heterogeneousStatements = successfulParse(heterogeneousInput, 1)
        
        val heterogeneousTypeDecl = heterogeneousStatements[0] as TypeAliasDecl
        val heterogeneousTupleType = heterogeneousTypeDecl.typeExpr as TupleTypeExpr
        
        assertEquals(3, heterogeneousTupleType.types.size, "Tuple should have 3 elements")
        
        assertIs<BaseTypeExpr>(heterogeneousTupleType.types[0])
        assertEquals("Int", (heterogeneousTupleType.types[0] as BaseTypeExpr).id.value, "First type should be 'Int'")
        
        assertIs<BaseTypeExpr>(heterogeneousTupleType.types[1])
        assertEquals("String", (heterogeneousTupleType.types[1] as BaseTypeExpr).id.value, "Second type should be 'String'")
        
        assertIs<BaseTypeExpr>(heterogeneousTupleType.types[2])
        assertEquals("Bool", (heterogeneousTupleType.types[2] as BaseTypeExpr).id.value, "Third type should be 'Bool'")
        
        // Test nested tuple types
        val nestedInput = "type Nested = (Int, (String, Bool))"
        val nestedStatements = successfulParse(nestedInput, 1)
        
        val nestedTypeDecl = nestedStatements[0] as TypeAliasDecl
        val nestedTupleType = nestedTypeDecl.typeExpr as TupleTypeExpr
        
        assertEquals(2, nestedTupleType.types.size, "Outer tuple should have 2 elements")
        
        assertIs<BaseTypeExpr>(nestedTupleType.types[0])
        assertEquals("Int", (nestedTupleType.types[0] as BaseTypeExpr).id.value, "First type should be 'Int'")
        
        assertIs<TupleTypeExpr>(nestedTupleType.types[1], "Second type should be a TupleTypeExpr")
        val innerTupleType = nestedTupleType.types[1] as TupleTypeExpr
        
        assertEquals(2, innerTupleType.types.size, "Inner tuple should have 2 elements")
        assertIs<BaseTypeExpr>(innerTupleType.types[0])
        assertEquals("String", (innerTupleType.types[0] as BaseTypeExpr).id.value, "First inner type should be 'String'")
        assertIs<BaseTypeExpr>(innerTupleType.types[1])
        assertEquals("Bool", (innerTupleType.types[1] as BaseTypeExpr).id.value, "Second inner type should be 'Bool'")
    }

    @Test
    fun `record type expressions`() {
        val input = "type User = { name: String, age: Int }"
        val statements = successfulParse(input, 1)
        
        assertIs<TypeAliasDecl>(statements[0], "Top level should be a TypeAliasDecl")
        val typeDecl = statements[0] as TypeAliasDecl
        
        assertEquals("User", typeDecl.id.value, "Type name should be 'User'")
        
        assertIs<RecordTypeExpr>(typeDecl.typeExpr, "Type expression should be a RecordTypeExpr")
        val recordType = typeDecl.typeExpr
        
        assertEquals(2, recordType.fields.size, "Record should have 2 fields")
        assertNull(recordType.extension, "Record should not have an extension")
        
        // Check fields
        assertEquals("name", recordType.fields[0].id.value, "First field name should be 'name'")
        assertIs<BaseTypeExpr>(recordType.fields[0].type, "First field type should be a BaseTypeExpr")
        assertEquals("String", (recordType.fields[0].type as BaseTypeExpr).id.value, "First field type should be 'String'")
        
        assertEquals("age", recordType.fields[1].id.value, "Second field name should be 'age'")
        assertIs<BaseTypeExpr>(recordType.fields[1].type, "Second field type should be a BaseTypeExpr")
        assertEquals("Int", (recordType.fields[1].type as BaseTypeExpr).id.value, "Second field type should be 'Int'")
        
        // Test record type with simple field types
        val simpleInput = "type Point = { x: Int, y: Int }"
        val simpleStatements = successfulParse(simpleInput, 1)
        
        val simpleTypeDecl = simpleStatements[0] as TypeAliasDecl
        val simpleRecordType = simpleTypeDecl.typeExpr as RecordTypeExpr
        
        assertEquals(2, simpleRecordType.fields.size, "Record should have 2 fields")
        assertNull(simpleRecordType.extension, "Record should not have an extension")
        
        assertEquals("x", simpleRecordType.fields[0].id.value, "First field name should be 'x'")
        assertIs<BaseTypeExpr>(simpleRecordType.fields[0].type)
        assertEquals("Int", (simpleRecordType.fields[0].type as BaseTypeExpr).id.value, "First field type should be 'Int'")
        
        assertEquals("y", simpleRecordType.fields[1].id.value, "Second field name should be 'y'")
        assertIs<BaseTypeExpr>(simpleRecordType.fields[1].type)
        assertEquals("Int", (simpleRecordType.fields[1].type as BaseTypeExpr).id.value, "Second field type should be 'Int'")
    }

    @Test
    fun `string literal type expressions`() {
        // Test a single string literal type
        val singleInput = "type Status = \"active\""
        val singleStatements = successfulParse(singleInput, 1)
        
        assertIs<TypeAliasDecl>(singleStatements[0], "Top level should be a TypeAliasDecl")
        val singleTypeDecl = singleStatements[0] as TypeAliasDecl
        
        assertEquals("Status", singleTypeDecl.id.value, "Type name should be 'Status'")
        
        assertIs<LiteralStringTypeExpr>(singleTypeDecl.typeExpr, "Type expression should be a LiteralStringTypeExpr")
        val stringType = singleTypeDecl.typeExpr
        assertEquals("active", stringType.value.value, "String literal should be 'active'")
        
        // Test union of string literals (Status = "success" | "error")
        val unionInput = "type Status = \"success\" | \"error\""
        val unionStatements = successfulParse(unionInput, 1)
        
        val unionTypeDecl = unionStatements[0] as TypeAliasDecl
        assertIs<UnionTypeExpr>(unionTypeDecl.typeExpr, "Type expression should be a UnionTypeExpr")
        val unionType = unionTypeDecl.typeExpr
        
        assertIs<LiteralStringTypeExpr>(unionType.left, "Left type should be a LiteralStringTypeExpr")
        assertEquals("success", unionType.left.value.value, "Left literal should be 'success'")
        
        assertIs<LiteralStringTypeExpr>(unionType.right, "Right type should be a LiteralStringTypeExpr")
        assertEquals("error", unionType.right.value.value, "Right literal should be 'error'")
    }

    @Test
    fun `parsing edge cases`() {
        // Test deeply nested expressions
        val nestedExpr = "let x = 1 + 2 * 3"
        val nestedStatements = successfulParse(nestedExpr, 1)
        
        assertIs<ExprStmt>(nestedStatements[0], "Statement should be an ExprStmt")
        val exprStmt = nestedStatements[0] as ExprStmt
        assertIs<LetExpr>(exprStmt.expr, "Expression should be a LetExpr")
        val letExpr = exprStmt.expr
        
        assertEquals("x", letExpr.id.value, "Variable name should be 'x'")
        
        // Check that it's a binary operation (1 + (2 * 3))
        assertIs<BinaryOpExpr>(letExpr.value, "Value should be a BinaryOpExpr")
        val binaryOpExpr = letExpr.value
        assertEquals(BinaryOp.Plus, binaryOpExpr.op)
        
        // Left operand should be literal 1
        assertIs<LiteralIntExpr>(binaryOpExpr.left)
        assertEquals(1, binaryOpExpr.left.value.value)
        
        // Right operand should be another binary op (2 * 3)
        assertIs<BinaryOpExpr>(binaryOpExpr.right)
        val rightBinaryOp = binaryOpExpr.right
        assertEquals(BinaryOp.Star, rightBinaryOp.op)
        assertIs<LiteralIntExpr>(rightBinaryOp.left)
        assertEquals(2, rightBinaryOp.left.value.value)
        assertIs<LiteralIntExpr>(rightBinaryOp.right)
        assertEquals(3, rightBinaryOp.right.value.value)
        
        // Test expressions with empty records and tuple expressions
        val simpleStructures = "let a = {} let b = (1, 2)"
        val structureStatements = successfulParse(simpleStructures, 2)
        
        // Check empty record
        val aExpr = (structureStatements[0] as ExprStmt).expr as LetExpr
        assertEquals("a", aExpr.id.value, "First variable name should be 'a'")
        assertIs<RecordExpr>(aExpr.value, "Value should be a RecordExpr")
        val recordExpr = aExpr.value
        assertEquals(0, recordExpr.fields.size, "Record should have 0 fields")
        
        // Check tuple with two elements
        val bExpr = (structureStatements[1] as ExprStmt).expr as LetExpr
        assertEquals("b", bExpr.id.value, "Second variable name should be 'b'")
        assertIs<TupleExpr>(bExpr.value, "Value should be a TupleExpr")
        val tupleExpr = bExpr.value
        assertEquals(2, tupleExpr.elements.size, "Tuple should have 2 elements")
        assertIs<LiteralIntExpr>(tupleExpr.elements[0])
        assertEquals(1, (tupleExpr.elements[0] as LiteralIntExpr).value.value)
        assertIs<LiteralIntExpr>(tupleExpr.elements[1])
        assertEquals(2, (tupleExpr.elements[1] as LiteralIntExpr).value.value)
    }

    @Test
    fun `complex pattern matching`() {
        // Test pattern matching with a few simple patterns
        val input = "match value with 0 => \"zero\" | True => \"true\" | _ => \"unknown\""
        val statements = successfulParse(input, 1)
        
        assertIs<ExprStmt>(statements[0], "Statement should be an ExprStmt")
        val exprStmt = statements[0] as ExprStmt
        assertIs<MatchExpr>(exprStmt.expr, "Expression should be a MatchExpr")
        val matchExpr = exprStmt.expr
        
        // Test scrutinee
        assertIs<VarExpr>(matchExpr.scrutinee, "Scrutinee should be a variable")
        assertEquals("value", matchExpr.scrutinee.id.value, "Scrutinee should be 'value'")
        
        // Test cases
        assertEquals(3, matchExpr.cases.size, "Should have 3 match cases")
        
        // Case 1: 0 => "zero"
        assertIs<LiteralIntPattern>(matchExpr.cases[0].pattern, "First pattern should be an integer literal pattern")
        assertEquals(0, (matchExpr.cases[0].pattern as LiteralIntPattern).value.value, "Pattern value should be 0")
        assertIs<LiteralStringExpr>(matchExpr.cases[0].body, "Body should be a string literal")
        assertEquals("zero", (matchExpr.cases[0].body as LiteralStringExpr).value.value, "Body value should be 'zero'")
        
        // Case 2: True => "true"
        assertIs<LiteralBoolPattern>(matchExpr.cases[1].pattern, "Second pattern should be a boolean literal pattern")
        assertEquals(true, (matchExpr.cases[1].pattern as LiteralBoolPattern).value.value, "Pattern value should be true")
        assertIs<LiteralStringExpr>(matchExpr.cases[1].body, "Body should be a string literal")
        assertEquals("true", (matchExpr.cases[1].body as LiteralStringExpr).value.value, "Body value should be 'true'")
        
        // Case 3: _ => "unknown"
        assertIs<WildcardPattern>(matchExpr.cases[2].pattern, "Third pattern should be a wildcard pattern")
        assertIs<LiteralStringExpr>(matchExpr.cases[2].body, "Body should be a string literal")
        assertEquals("unknown", (matchExpr.cases[2].body as LiteralStringExpr).value.value, "Body value should be 'unknown'")
        
        // Test with tuple and record patterns
        val structInput = "match val with (x, y) => x + y | { a = 1 } => 2 | _ => 0"
        val structStatements = successfulParse(structInput, 1)
        
        val structMatchExpr = (structStatements[0] as ExprStmt).expr as MatchExpr
        assertEquals(3, structMatchExpr.cases.size, "Should have 3 match cases")
        
        // Case 1: (x, y) => x + y
        assertIs<TuplePattern>(structMatchExpr.cases[0].pattern, "First pattern should be a tuple pattern")
        val tuplePattern = structMatchExpr.cases[0].pattern as TuplePattern
        assertEquals(2, tuplePattern.elements.size, "Tuple should have 2 elements")
        
        assertIs<VarPattern>(tuplePattern.elements[0], "First element should be a variable pattern")
        assertEquals("x", (tuplePattern.elements[0] as VarPattern).id.value, "First variable should be 'x'")
        
        assertIs<VarPattern>(tuplePattern.elements[1], "Second element should be a variable pattern")
        assertEquals("y", (tuplePattern.elements[1] as VarPattern).id.value, "Second variable should be 'y'")
        
        assertIs<BinaryOpExpr>(structMatchExpr.cases[0].body, "First case body should be a binary expression")
        
        // Case 2: { a = 1 } => 2
        assertIs<RecordPattern>(structMatchExpr.cases[1].pattern, "Second pattern should be a record pattern")
        val recordPattern = structMatchExpr.cases[1].pattern as RecordPattern
        assertEquals(1, recordPattern.fields.size, "Record should have 1 field")
        
        assertEquals("a", recordPattern.fields[0].id.value, "Field name should be 'a'")
        assertIs<LiteralIntPattern>(recordPattern.fields[0].pattern, "Field pattern should be an integer literal")
        assertEquals(1, (recordPattern.fields[0].pattern as LiteralIntPattern).value.value, "Field value should be 1")
        
        assertIs<LiteralIntExpr>(structMatchExpr.cases[1].body, "Second case body should be an integer literal")
        assertEquals(2, (structMatchExpr.cases[1].body as LiteralIntExpr).value.value, "Body value should be 2")
        
        // Case 3: _ => 0
        assertIs<WildcardPattern>(structMatchExpr.cases[2].pattern, "Third pattern should be a wildcard pattern")
        assertIs<LiteralIntExpr>(structMatchExpr.cases[2].body, "Third case body should be an integer literal")
        assertEquals(0, (structMatchExpr.cases[2].body as LiteralIntExpr).value.value, "Body value should be 0")
    }

    @Test
    fun `record type parsing with TypeScript-style open records`() {
        // Test parsing of open record type with TypeScript-style syntax
        val openRecordInput = "type Person[A] = { name: String, age: Int, ...A }"
        val openRecordStatements = successfulParse(openRecordInput, 1)
        
        assertIs<TypeAliasDecl>(openRecordStatements[0], "Statement should be a TypeAliasDecl")
        val typeAlias = openRecordStatements[0] as TypeAliasDecl
        assertEquals("Person", typeAlias.id.value, "Type name should be 'Person'")
        assertEquals(1, typeAlias.typeParams?.size, "Should have one type parameter")
        assertEquals("A", typeAlias.typeParams?.get(0)?.id?.value, "Type parameter should be 'A'")
        
        assertIs<RecordTypeExpr>(typeAlias.typeExpr, "Type expression should be a RecordTypeExpr")
        val recordType = typeAlias.typeExpr
        assertEquals(2, recordType.fields.size, "Record should have 2 fields")
        assertEquals("name", recordType.fields[0].id.value, "First field should be 'name'")
        assertEquals("age", recordType.fields[1].id.value, "Second field should be 'age'")
        assertEquals("A", recordType.extension?.value, "Extension should be 'A'")

        // Test parsing of closed record type
        val closedRecordInput = "type Person = { name: String, age: Int }"
        val closedRecordStatements = successfulParse(closedRecordInput, 1)
        
        val closedTypeAlias = closedRecordStatements[0] as TypeAliasDecl
        assertEquals("Person", closedTypeAlias.id.value, "Type name should be 'Person'")
        
        val closedRecordType = closedTypeAlias.typeExpr as RecordTypeExpr
        assertEquals(2, closedRecordType.fields.size, "Record should have 2 fields")
        assertEquals("name", closedRecordType.fields[0].id.value, "First field should be 'name'")
        assertEquals("age", closedRecordType.fields[1].id.value, "Second field should be 'age'")
        assertNull(closedRecordType.extension, "Extension should be null for closed record")

        // Test parsing of record type with only extension
        val onlyExtensionInput = "type ExtendedRecord[A] = { ...A }"
        val onlyExtensionStatements = successfulParse(onlyExtensionInput, 1)
        
        val extendedTypeAlias = onlyExtensionStatements[0] as TypeAliasDecl
        assertEquals("ExtendedRecord", extendedTypeAlias.id.value, "Type name should be 'ExtendedRecord'")
        
        val extendedRecordType = extendedTypeAlias.typeExpr as RecordTypeExpr
        assertEquals(0, extendedRecordType.fields.size, "Record should have 0 fields")
        assertEquals("A", extendedRecordType.extension?.value, "Extension should be 'A'")

        // Test parsing of empty record type
        val emptyRecordInput = "type EmptyRecord = { }"
        val emptyRecordStatements = successfulParse(emptyRecordInput, 1)
        
        val emptyTypeAlias = emptyRecordStatements[0] as TypeAliasDecl
        assertEquals("EmptyRecord", emptyTypeAlias.id.value, "Type name should be 'EmptyRecord'")
        
        val emptyRecordType = emptyTypeAlias.typeExpr as RecordTypeExpr
        assertEquals(0, emptyRecordType.fields.size, "Record should have 0 fields")
        assertNull(emptyRecordType.extension, "Extension should be null for empty record")
    }

    private fun successfulParse(input: String, numberOfStatements: Int): List<TopLevel> {
        val errors = Errors()
        val script = parse(input, errors)
        assertTrue(errors.hasNoErrors())
        assertEquals(script.topLevels.size, numberOfStatements)

        return script.topLevels
    }
}
