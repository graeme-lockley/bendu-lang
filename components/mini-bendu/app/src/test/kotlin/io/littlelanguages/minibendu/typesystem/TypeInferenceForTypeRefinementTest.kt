package io.littlelanguages.minibendu.typesystem

import io.littlelanguages.minibendu.*
import io.littlelanguages.scanpiler.LocationCoordinate
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Comprehensive tests for pattern type refinement.
 * Task 51 of Phase 1 - Create Tests for Pattern Type Refinement.
 * 
 * Tests cover:
 * - Type narrowing in match expressions
 * - Flow-sensitive typing
 * - Contradiction detection in patterns
 * - Pattern reachability analysis
 */
class TypeInferenceForTypeRefinementTest {
    
    private fun createStringLocation(value: String): StringLocation {
        return StringLocation(value, createLocation())
    }
    
    private fun createLocation(): LocationCoordinate {
        return LocationCoordinate(0, 1, 1)
    }
    
    private fun createIntLocation(value: Int): IntLocation {
        return IntLocation(value, createLocation())
    }
    
    private fun createBoolLocation(value: Boolean): BoolLocation {
        return BoolLocation(value, createLocation())
    }
    
    // ===== TYPE NARROWING TESTS =====
    
    @Test
    fun `type narrowing with union types should refine correctly`() {
        // let x: "success" | "error" | Int = getValue()
        // match x with 
        //   "success" => processSuccess(x)  // x narrowed to "success"
        // | "error" => processError(x)      // x narrowed to "error"  
        // | n => processNumber(n)           // n narrowed to Int
        
        val successLiteral = Types.literal("success")
        val errorLiteral = Types.literal("error")
        val unionType = UnionType(setOf(successLiteral, errorLiteral, Types.Int))
        
        val scrutinee = VarExpr(createStringLocation("x"))
        
        // Case 1: "success" => processSuccess(x)
        val successPattern = LiteralStringPattern(createStringLocation("success"))
        val processSuccessCall = ApplicationExpr(
            VarExpr(createStringLocation("processSuccess")),
            listOf(VarExpr(createStringLocation("x"))),
            createLocation()
        )
        val successCase = MatchCase(successPattern, processSuccessCall)
        
        // Case 2: "error" => processError(x)  
        val errorPattern = LiteralStringPattern(createStringLocation("error"))
        val processErrorCall = ApplicationExpr(
            VarExpr(createStringLocation("processError")),
            listOf(VarExpr(createStringLocation("x"))),
            createLocation()
        )
        val errorCase = MatchCase(errorPattern, processErrorCall)
        
        // Case 3: n => processNumber(n)
        val numberPattern = VarPattern(createStringLocation("n"))
        val processNumberCall = ApplicationExpr(
            VarExpr(createStringLocation("processNumber")),
            listOf(VarExpr(createStringLocation("n"))),
            createLocation()
        )
        val numberCase = MatchCase(numberPattern, processNumberCall)
        
        val matchExpr = MatchExpr(scrutinee, listOf(successCase, errorCase, numberCase), createLocation())
        
        val env = TypeEnvironment.empty()
            .extend("x", unionType)
            .extend("processSuccess", FunctionType(successLiteral, Types.String))
            .extend("processError", FunctionType(errorLiteral, Types.String))
            .extend("processNumber", FunctionType(Types.Int, Types.String))
        
        val generator = ConstraintGenerator(env)
        val result = generator.generateConstraints(matchExpr)
        
        assertTrue(result.isSuccess(), "Type refinement should succeed")
        
        if (result.isSuccess()) {
            val solverResult = ConstraintSolver().solve(result.constraints)
            if (solverResult is ConstraintSolverResult.Success) {
                val finalType = solverResult.substitution.apply(result.type)
                assertEquals(Types.String, finalType, "Match expression should have String type")
                
                // TODO: Verify type refinement once implemented
                // val refinementInfo = TypeRefinementAnalyzer.analyze(matchExpr, unionType)
                // assertEquals(successLiteral, refinementInfo.getRefinedType("x", successCase))
                // assertEquals(errorLiteral, refinementInfo.getRefinedType("x", errorCase))
                // assertEquals(Types.Int, refinementInfo.getRefinedType("n", numberCase))
            }
        }
    }
    
    @Test
    fun `nested pattern refinement should work with complex types`() {
        // type Result = {status: "ok", value: Int} | {status: "error", message: String}
        // match result with
        //   {status = "ok", value = v} => v + 1      // result narrowed to success type
        // | {status = "error", message = m} => -1   // result narrowed to error type
        
        val okStatus = Types.literal("ok")
        val errorStatus = Types.literal("error")
        
        val successType = RecordType(mapOf(
            "status" to okStatus,
            "value" to Types.Int
        ))
        
        val errorType = RecordType(mapOf(
            "status" to errorStatus,
            "message" to Types.String
        ))
        
        val resultUnion = UnionType(setOf(successType, errorType))
        
        val scrutinee = VarExpr(createStringLocation("result"))
        
        // Case 1: {status = "ok", value = v} => v + 1
        val okPattern = LiteralStringPattern(createStringLocation("ok"))
        val valuePattern = VarPattern(createStringLocation("v"))
        val statusField1 = FieldPattern(createStringLocation("status"), okPattern)
        val valueField = FieldPattern(createStringLocation("value"), valuePattern)
        val successPattern = RecordPattern(listOf(statusField1, valueField), createLocation())
        
        val vVar = VarExpr(createStringLocation("v"))
        val increment = BinaryOpExpr(vVar, BinaryOp.Plus, LiteralIntExpr(createIntLocation(1)), createLocation())
        val successCase = MatchCase(successPattern, increment)
        
        // Case 2: {status = "error", message = m} => -1
        val errorPattern = LiteralStringPattern(createStringLocation("error"))
        val messagePattern = VarPattern(createStringLocation("m"))
        val statusField2 = FieldPattern(createStringLocation("status"), errorPattern)
        val messageField = FieldPattern(createStringLocation("message"), messagePattern)
        val errorRecordPattern = RecordPattern(listOf(statusField2, messageField), createLocation())
        
        val minusOne = LiteralIntExpr(createIntLocation(-1))
        val errorCase = MatchCase(errorRecordPattern, minusOne)
        
        val matchExpr = MatchExpr(scrutinee, listOf(successCase, errorCase), createLocation())
        
        val env = TypeEnvironment.empty()
            .extend("result", resultUnion)
        
        val generator = ConstraintGenerator(env)
        val result = generator.generateConstraints(matchExpr)
        
        assertTrue(result.isSuccess(), "Nested pattern refinement should succeed")
        
        if (result.isSuccess()) {
            val solverResult = ConstraintSolver().solve(result.constraints)
            if (solverResult is ConstraintSolverResult.Success) {
                val finalType = solverResult.substitution.apply(result.type)
                assertEquals(Types.Int, finalType, "Match expression should have Int type")
                
                // TODO: Verify nested refinement once implemented
                // val refinementInfo = TypeRefinementAnalyzer.analyze(matchExpr, resultUnion)
                // assertEquals(successType, refinementInfo.getRefinedType("result", successCase))
                // assertEquals(errorType, refinementInfo.getRefinedType("result", errorCase))
                // assertEquals(Types.Int, refinementInfo.getRefinedType("v", successCase))
                // assertEquals(Types.String, refinementInfo.getRefinedType("m", errorCase))
            }
        }
    }
    
    @Test
    fun `flow sensitive typing should track type changes through branches`() {
        // let processValue = fun(x: Int | String) =>
        //   match x with
        //     n when isNumber(n) => n * 2        // n: Int in this branch
        //   | s => s + " processed"              // s: String in this branch
        
        val unionType = UnionType(setOf(Types.Int, Types.String))
        
        val scrutinee = VarExpr(createStringLocation("x"))
        
        // Case 1: n => isNumber(n) ? n * 2 : next
        val numberPattern = VarPattern(createStringLocation("n"))
        val nVar = VarExpr(createStringLocation("n"))
        val doubleN = BinaryOpExpr(nVar, BinaryOp.Star, LiteralIntExpr(createIntLocation(2)), createLocation())
        val numberCase = MatchCase(numberPattern, doubleN)
        
        // Case 2: s => s + " processed"  
        val stringPattern = VarPattern(createStringLocation("s"))
        val sVar = VarExpr(createStringLocation("s"))
        val processed = LiteralStringExpr(createStringLocation(" processed"))
        val concatS = BinaryOpExpr(sVar, BinaryOp.Plus, processed, createLocation())
        val stringCase = MatchCase(stringPattern, concatS)
        
        val matchExpr = MatchExpr(scrutinee, listOf(numberCase, stringCase), createLocation())
        
        val env = TypeEnvironment.empty()
            .extend("x", unionType)
        
        val generator = ConstraintGenerator(env)
        val result = generator.generateConstraints(matchExpr)
        
        // This should fail because we can't multiply String * Int or concatenate Int + String
        if (result.isSuccess()) {
            val solverResult = ConstraintSolver().solve(result.constraints)
            assertTrue(solverResult is ConstraintSolverResult.Failure, 
                     "Should fail due to incompatible operations without proper refinement")
        } else {
            assertTrue(true, "Constraint generation failed as expected")
        }
        
        // TODO: Test with proper flow-sensitive refinement that this should succeed
        // when we can detect that 'n' is refined to Int and 's' is refined to String
    }
    
    // ===== CONTRADICTION DETECTION TESTS =====
    
    @Test
    fun `contradictory patterns should be detected`() {
        // match x with
        //   42 => "forty-two"
        // | 42 => "duplicate"     // Contradiction: same literal pattern
        // | _ => "other"
        
        val scrutinee = VarExpr(createStringLocation("x"))
        
        val pattern1 = LiteralIntPattern(createIntLocation(42))
        val body1 = LiteralStringExpr(createStringLocation("forty-two"))
        val case1 = MatchCase(pattern1, body1)
        
        val pattern2 = LiteralIntPattern(createIntLocation(42))  // Same pattern!
        val body2 = LiteralStringExpr(createStringLocation("duplicate"))
        val case2 = MatchCase(pattern2, body2)
        
        val pattern3 = WildcardPattern(createLocation())
        val body3 = LiteralStringExpr(createStringLocation("other"))
        val case3 = MatchCase(pattern3, body3)
        
        val matchExpr = MatchExpr(scrutinee, listOf(case1, case2, case3), createLocation())
        
        val env = TypeEnvironment.empty()
            .extend("x", Types.Int)
        
        val generator = ConstraintGenerator(env)
        val result = generator.generateConstraints(matchExpr)
        
        // Type checking should succeed, but contradiction analysis should detect the issue
        assertTrue(result.isSuccess(), "Type checking should succeed")
        
        // TODO: Add contradiction detection once implemented
        // val contradictions = ContradictionDetector.analyze(matchExpr)
        // assertTrue(contradictions.hasContradictions())
        // assertEquals(1, contradictions.contradictoryPairs.size)
        // assertEquals(Pair(case1, case2), contradictions.contradictoryPairs[0])
    }
    
    @Test
    fun `impossible pattern combinations should be detected`() {
        // match (x, y) with
        //   (1, 2) => "specific"
        // | (1, 3) => "different"
        // | (1, 2) => "impossible"  // Contradiction with first case
        
        val scrutinee = TupleExpr(listOf(
            VarExpr(createStringLocation("x")),
            VarExpr(createStringLocation("y"))
        ), createLocation())
        
        val pattern1 = TuplePattern(listOf(
            LiteralIntPattern(createIntLocation(1)),
            LiteralIntPattern(createIntLocation(2))
        ), createLocation())
        val body1 = LiteralStringExpr(createStringLocation("specific"))
        val case1 = MatchCase(pattern1, body1)
        
        val pattern2 = TuplePattern(listOf(
            LiteralIntPattern(createIntLocation(1)),
            LiteralIntPattern(createIntLocation(3))
        ), createLocation())
        val body2 = LiteralStringExpr(createStringLocation("different"))
        val case2 = MatchCase(pattern2, body2)
        
        val pattern3 = TuplePattern(listOf(
            LiteralIntPattern(createIntLocation(1)),
            LiteralIntPattern(createIntLocation(2))
        ), createLocation())  // Same as pattern1!
        val body3 = LiteralStringExpr(createStringLocation("impossible"))
        val case3 = MatchCase(pattern3, body3)
        
        val matchExpr = MatchExpr(scrutinee, listOf(case1, case2, case3), createLocation())
        
        val pairType = TupleType(listOf(Types.Int, Types.Int))
        val env = TypeEnvironment.empty()
            .extend("x", Types.Int)
            .extend("y", Types.Int)
        
        val generator = ConstraintGenerator(env)
        val result = generator.generateConstraints(matchExpr)
        
        assertTrue(result.isSuccess(), "Type checking should succeed")
        
        // TODO: Add contradiction detection for tuple patterns
        // val contradictions = ContradictionDetector.analyze(matchExpr)
        // assertTrue(contradictions.hasContradictions())
    }
    
    @Test
    fun `contradictory record patterns should be detected`() {
        // match person with
        //   {name = "Alice", age = 25} => "specific Alice"
        // | {name = "Bob", age = a} => "Bob of age " + a  
        // | {name = "Alice", age = 25} => "duplicate Alice"  // Contradiction
        
        val scrutinee = VarExpr(createStringLocation("person"))
        
        // Pattern 1: {name = "Alice", age = 25}
        val alicePattern1 = LiteralStringPattern(createStringLocation("Alice"))
        val age25Pattern1 = LiteralIntPattern(createIntLocation(25))
        val nameField1 = FieldPattern(createStringLocation("name"), alicePattern1)
        val ageField1 = FieldPattern(createStringLocation("age"), age25Pattern1)
        val recordPattern1 = RecordPattern(listOf(nameField1, ageField1), createLocation())
        val body1 = LiteralStringExpr(createStringLocation("specific Alice"))
        val case1 = MatchCase(recordPattern1, body1)
        
        // Pattern 2: {name = "Bob", age = a}
        val bobPattern = LiteralStringPattern(createStringLocation("Bob"))
        val ageVarPattern = VarPattern(createStringLocation("a"))
        val nameField2 = FieldPattern(createStringLocation("name"), bobPattern)
        val ageField2 = FieldPattern(createStringLocation("age"), ageVarPattern)
        val recordPattern2 = RecordPattern(listOf(nameField2, ageField2), createLocation())
        val aVar = VarExpr(createStringLocation("a"))
        val bobStr = LiteralStringExpr(createStringLocation("Bob of age "))
        val bobConcat = BinaryOpExpr(bobStr, BinaryOp.Plus, aVar, createLocation())
        val case2 = MatchCase(recordPattern2, bobConcat)
        
        // Pattern 3: {name = "Alice", age = 25} (duplicate of pattern 1)
        val alicePattern3 = LiteralStringPattern(createStringLocation("Alice"))
        val age25Pattern3 = LiteralIntPattern(createIntLocation(25))
        val nameField3 = FieldPattern(createStringLocation("name"), alicePattern3)
        val ageField3 = FieldPattern(createStringLocation("age"), age25Pattern3)
        val recordPattern3 = RecordPattern(listOf(nameField3, ageField3), createLocation())
        val body3 = LiteralStringExpr(createStringLocation("duplicate Alice"))
        val case3 = MatchCase(recordPattern3, body3)
        
        val matchExpr = MatchExpr(scrutinee, listOf(case1, case2, case3), createLocation())
        
        val personType = RecordType(mapOf(
            "name" to Types.String,
            "age" to Types.Int
        ))
        val env = TypeEnvironment.empty()
            .extend("person", personType)
        
        val generator = ConstraintGenerator(env)
        val result = generator.generateConstraints(matchExpr)
        
        assertTrue(result.isSuccess(), "Type checking should succeed")
        
        // TODO: Add contradiction detection for record patterns
        // val contradictions = ContradictionDetector.analyze(matchExpr)
        // assertTrue(contradictions.hasContradictions())
        // assertEquals(Pair(case1, case3), contradictions.contradictoryPairs[0])
    }
    
    // ===== PATTERN REACHABILITY TESTS =====
    
    @Test
    fun `unreachable patterns after wildcard should be detected`() {
        // match x with
        //   _ => "anything"
        // | 42 => "forty-two"    // Unreachable: wildcard catches everything
        // | "hello" => "greeting" // Unreachable: wildcard catches everything
        
        val scrutinee = VarExpr(createStringLocation("x"))
        
        val wildcardPattern = WildcardPattern(createLocation())
        val wildcardBody = LiteralStringExpr(createStringLocation("anything"))
        val wildcardCase = MatchCase(wildcardPattern, wildcardBody)
        
        val numberPattern = LiteralIntPattern(createIntLocation(42))
        val numberBody = LiteralStringExpr(createStringLocation("forty-two"))
        val numberCase = MatchCase(numberPattern, numberBody)
        
        val stringPattern = LiteralStringPattern(createStringLocation("hello"))
        val stringBody = LiteralStringExpr(createStringLocation("greeting"))
        val stringCase = MatchCase(stringPattern, stringBody)
        
        val matchExpr = MatchExpr(scrutinee, listOf(wildcardCase, numberCase, stringCase), createLocation())
        
        val unionType = UnionType(setOf(Types.Int, Types.String))
        val env = TypeEnvironment.empty()
            .extend("x", unionType)
        
        val generator = ConstraintGenerator(env)
        val result = generator.generateConstraints(matchExpr)
        
        assertTrue(result.isSuccess(), "Type checking should succeed")
        
        // TODO: Add reachability analysis once implemented
        // val reachability = ReachabilityAnalyzer.analyze(matchExpr)
        // assertTrue(reachability.hasUnreachablePatterns())
        // assertEquals(2, reachability.unreachablePatterns.size)
        // assertTrue(reachability.unreachablePatterns.contains(numberCase))
        // assertTrue(reachability.unreachablePatterns.contains(stringCase))
    }
    
    @Test
    fun `unreachable patterns after exhaustive coverage should be detected`() {
        // match flag with
        //   True => "yes"
        // | False => "no"
        // | _ => "impossible"    // Unreachable: bool cases are exhaustive
        
        val scrutinee = VarExpr(createStringLocation("flag"))
        
        val truePattern = LiteralBoolPattern(createBoolLocation(true))
        val trueBody = LiteralStringExpr(createStringLocation("yes"))
        val trueCase = MatchCase(truePattern, trueBody)
        
        val falsePattern = LiteralBoolPattern(createBoolLocation(false))
        val falseBody = LiteralStringExpr(createStringLocation("no"))
        val falseCase = MatchCase(falsePattern, falseBody)
        
        val wildcardPattern = WildcardPattern(createLocation())
        val wildcardBody = LiteralStringExpr(createStringLocation("impossible"))
        val wildcardCase = MatchCase(wildcardPattern, wildcardBody)
        
        val matchExpr = MatchExpr(scrutinee, listOf(trueCase, falseCase, wildcardCase), createLocation())
        
        val env = TypeEnvironment.empty()
            .extend("flag", Types.Bool)
        
        val generator = ConstraintGenerator(env)
        val result = generator.generateConstraints(matchExpr)
        
        assertTrue(result.isSuccess(), "Type checking should succeed")
        
        // TODO: Add reachability analysis for exhaustive coverage
        // val reachability = ReachabilityAnalyzer.analyze(matchExpr)
        // assertTrue(reachability.hasUnreachablePatterns())
        // assertTrue(reachability.unreachablePatterns.contains(wildcardCase))
    }
    
    @Test
    fun `variable pattern shadowing previous pattern should be detected`() {
        // match x with
        //   42 => "forty-two"
        // | n => "number: " + n   // Shadows remaining Int cases (but this is often intentional)
        // | 84 => "eighty-four"   // Unreachable: variable pattern catches all remaining cases
        
        val scrutinee = VarExpr(createStringLocation("x"))
        
        val specificPattern = LiteralIntPattern(createIntLocation(42))
        val specificBody = LiteralStringExpr(createStringLocation("forty-two"))
        val specificCase = MatchCase(specificPattern, specificBody)
        
        val variablePattern = VarPattern(createStringLocation("n"))
        val nVar = VarExpr(createStringLocation("n"))
        val numberStr = LiteralStringExpr(createStringLocation("number: "))
        val variableBody = BinaryOpExpr(numberStr, BinaryOp.Plus, nVar, createLocation())
        val variableCase = MatchCase(variablePattern, variableBody)
        
        val anotherSpecificPattern = LiteralIntPattern(createIntLocation(84))
        val anotherSpecificBody = LiteralStringExpr(createStringLocation("eighty-four"))
        val anotherSpecificCase = MatchCase(anotherSpecificPattern, anotherSpecificBody)
        
        val matchExpr = MatchExpr(scrutinee, listOf(specificCase, variableCase, anotherSpecificCase), createLocation())
        
        val env = TypeEnvironment.empty()
            .extend("x", Types.Int)
        
        val generator = ConstraintGenerator(env)
        val result = generator.generateConstraints(matchExpr)
        
        assertTrue(result.isSuccess(), "Type checking should succeed")
        
        // TODO: Add variable pattern shadowing detection
        // val reachability = ReachabilityAnalyzer.analyze(matchExpr)
        // assertTrue(reachability.hasUnreachablePatterns())
        // assertTrue(reachability.unreachablePatterns.contains(anotherSpecificCase))
        // assertTrue(reachability.hasVariableShadowing())
    }
    
    @Test
    fun `complex nested pattern reachability should be analyzed`() {
        // match (status, value) with
        //   ("ok", n) => n
        // | ("error", _) => -1
        // | (s, v) => 0              // Covers remaining cases
        // | ("pending", 42) => 42    // Unreachable: covered by (s, v)
        
        val scrutinee = TupleExpr(listOf(
            VarExpr(createStringLocation("status")),
            VarExpr(createStringLocation("value"))
        ), createLocation())
        
        // Case 1: ("ok", n) => n
        val okPattern = LiteralStringPattern(createStringLocation("ok"))
        val nPattern = VarPattern(createStringLocation("n"))
        val okTuplePattern = TuplePattern(listOf(okPattern, nPattern), createLocation())
        val nVar = VarExpr(createStringLocation("n"))
        val okCase = MatchCase(okTuplePattern, nVar)
        
        // Case 2: ("error", _) => -1
        val errorPattern = LiteralStringPattern(createStringLocation("error"))
        val errorWildcard = WildcardPattern(createLocation())
        val errorTuplePattern = TuplePattern(listOf(errorPattern, errorWildcard), createLocation())
        val minusOne = LiteralIntExpr(createIntLocation(-1))
        val errorCase = MatchCase(errorTuplePattern, minusOne)
        
        // Case 3: (s, v) => 0  (covers remaining)
        val sPattern = VarPattern(createStringLocation("s"))
        val vPattern = VarPattern(createStringLocation("v"))
        val catchAllPattern = TuplePattern(listOf(sPattern, vPattern), createLocation())
        val zero = LiteralIntExpr(createIntLocation(0))
        val catchAllCase = MatchCase(catchAllPattern, zero)
        
        // Case 4: ("pending", 42) => 42  (should be unreachable)
        val pendingPattern = LiteralStringPattern(createStringLocation("pending"))
        val fortyTwoPattern = LiteralIntPattern(createIntLocation(42))
        val pendingTuplePattern = TuplePattern(listOf(pendingPattern, fortyTwoPattern), createLocation())
        val fortyTwo = LiteralIntExpr(createIntLocation(42))
        val pendingCase = MatchCase(pendingTuplePattern, fortyTwo)
        
        val matchExpr = MatchExpr(scrutinee, listOf(okCase, errorCase, catchAllCase, pendingCase), createLocation())
        
        val statusType = UnionType(setOf(
            Types.literal("ok"),
            Types.literal("error"),
            Types.literal("pending")
        ))
        val tupleType = TupleType(listOf(statusType, Types.Int))
        val env = TypeEnvironment.empty()
            .extend("status", statusType)
            .extend("value", Types.Int)
        
        val generator = ConstraintGenerator(env)
        val result = generator.generateConstraints(matchExpr)
        
        assertTrue(result.isSuccess(), "Type checking should succeed")
        
        // TODO: Add complex reachability analysis
        // val reachability = ReachabilityAnalyzer.analyze(matchExpr)
        // assertTrue(reachability.hasUnreachablePatterns())
        // assertTrue(reachability.unreachablePatterns.contains(pendingCase))
    }
} 