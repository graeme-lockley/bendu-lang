package io.littlelanguages.minibendu

import io.littlelanguages.minibendu.typesystem.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class EndToEndTest {

    private fun parseAndTypeCheck(source: String): TypeCheckResult {
        return try {
            val errors = Errors()
            val program = parse(source, errors)

            if (errors.hasErrors()) {
                val errorMessages = mutableListOf<String>()
                for (error in errors) {
                    errorMessages.add(error.toString())
                }
                TypeCheckResult.Failure("Parse error: ${errorMessages.joinToString("; ")}", SourceLocation(1, 1))
            } else {
                val typeChecker = TypeChecker()

                // Extract the main expression from the program
                val expressions = program.expressions()
                if (expressions.isEmpty()) {
                    TypeCheckResult.Failure("No expressions found in program", SourceLocation(1, 1))
                } else {
                    // Type check the last expression (main result)
                    val mainExpr = expressions.last()
                    typeChecker.typeCheck(mainExpr)
                }
            }
        } catch (e: Exception) {
            TypeCheckResult.Failure("Parse error: ${e.message}", SourceLocation(1, 1))
        }
    }

    private fun assertTypeCheckSuccess(source: String, expectedType: String? = null) {
        val result = parseAndTypeCheck(source)

        if (result is TypeCheckResult.Failure) {
            println("Error: ${result.error}")
        }

        assertTrue(result is TypeCheckResult.Success, "Expected type check to succeed for: $source")
        if (expectedType != null) {
            assertEquals(expectedType, result.getFinalType().toString())
        }
    }

//    private fun assertTypeCheckFailure(source: String, expectedErrorContains: String? = null) {
//        val result = parseAndTypeCheck(source)
//        assertTrue(result is TypeCheckResult.Failure, "Expected type check to fail for: $source")
//        if (expectedErrorContains != null && result is TypeCheckResult.Failure) {
//            assertTrue(
//                result.error.contains(expectedErrorContains),
//                "Expected error to contain '$expectedErrorContains' but got: ${result.error}"
//            )
//        }
//    }

    @Test
    fun testSimpleArithmeticProgram() {
        val source = """
            let x = 5 + 3 in
            let y = x * 2 in
            y
        """.trimIndent()
        assertTypeCheckSuccess(source, "Int")
    }

    @Test
    fun testPolymorphicIdentityFunction() {
        val source = """
            let identity = \x => x in
            let result1 = identity(42) in
            let result2 = identity("hello") in
            result1
        """.trimIndent()
        assertTypeCheckSuccess(source, "Int")
    }

//    @Test
//    fun testRecordManipulationProgram() {
//        val source = """
//            let person = { name = "Alice", age = 30 } in
//            let updatedPerson = { ...person, age = 31 } in
//            updatedPerson.age
//        """.trimIndent()
//        assertTypeCheckSuccess(source, "Int")
//    }
//
//    @Test
//    fun testRowPolymorphismWithFunctions() {
//        val source = """
//            let getName = \record => record.name in
//            let person = { name = "Bob", age = 25 } in
//            let company = { name = "TechCorp", employees = 100 } in
//            let name1 = getName person in
//            let name2 = getName company in
//            name1
//        """.trimIndent()
//        assertTypeCheckSuccess(source, "String")
//    }
//
//    @Test
//    fun testComplexUnionTypeProgram() {
//        val source = """
//            let processValue = \value =>
//                match value with
//                | n : Int => n + 1
//                | s : String => s + " processed"
//                | _ => "unknown"
//            in
//            let result1 = processValue 42 in
//            let result2 = processValue "hello" in
//            result1
//        """.trimIndent()
//        assertTypeCheckSuccess(source)
//    }
//
//    @Test
//    fun testRecursiveDataStructures() {
//        val source = """
//            let rec list =
//                match input with
//                | [] => 0
//                | head :: tail => 1 + (list tail)
//            in
//            list [1, 2, 3, 4]
//        """.trimIndent()
//        assertTypeCheckSuccess(source, "Int")
//    }
//
//    @Test
//    fun testHigherOrderFunctionComposition() {
//        val source = """
//            let map = \f => \list =>
//                match list with
//                | [] => []
//                | head :: tail => (f head) :: (map f tail)
//            in
//            let double = \x => x * 2 in
//            let increment = \x => x + 1 in
//            let numbers = [1, 2, 3] in
//            let doubled = map double numbers in
//            let incremented = map increment doubled in
//            incremented
//        """.trimIndent()
//        assertTypeCheckSuccess(source)
//    }
//
//    @Test
//    fun testComplexPatternMatching() {
//        val source = """
//            let processData = \data =>
//                match data with
//                | { type = "user", user = { name = userName, active = true } } =>
//                    "Active user = " + userName
//                | { type = "user", user = { name = userName, active = false } } =>
//                    "Inactive user = " + userName
//                | { type = "admin", permissions = perms } =>
//                    "Admin with permissions"
//                | _ => "Unknown data type"
//            in
//            let userData = { type = "user", user = { name = "Alice", active = true } } in
//            processData userData
//        """.trimIndent()
//        assertTypeCheckSuccess(source, "String")
//    }
//
//    @Test
//    fun testIntersectionTypesInPractice() {
//        val source = """
//            let createEntity = \name => \age =>
//                { name = name, age = age } & { id = 1, active = true }
//            in
//            let entity = createEntity "Bob" 30 in
//            entity.name + " is " + (if entity.active then "active" else "inactive")
//        """.trimIndent()
//        assertTypeCheckSuccess(source, "String")
//    }
//
//    @Test
//    fun testTypeRefinementInConditions() {
//        val source = """
//            let handleValue = \value =>
//                if typeof value == "string" then
//                    value + " is a string"
//                else if typeof value == "number" then
//                    "Number: " + toString value
//                else
//                    "Unknown type"
//            in
//            handleValue "test"
//        """.trimIndent()
//        assertTypeCheckSuccess(source, "String")
//    }
//
//    @Test
//    fun testComplexRecordInheritancePattern() {
//        val source = """
//            let baseConfig = { timeout = 30, retries = 3 } in
//            let httpConfig = { ...baseConfig, method = "GET", url = "https://api.example.com" } in
//            let apiConfig = { ...httpConfig, apiKey = "secret", version = "v1" } in
//            let makeRequest = \config =>
//                "Making " + config.method + " request to " + config.url +
//                " with timeout " + toString config.timeout
//            in
//            makeRequest apiConfig
//        """.trimIndent()
//        assertTypeCheckSuccess(source, "String")
//    }
//
//    @Test
//    fun testErrorPropagationAndRecovery() {
//        val source = """
//            let safeDiv = \x => \y =>
//                if y == 0 then
//                    { error = "Division by zero" }
//                else
//                    { result = x / y }
//            in
//            let processResults = \results =>
//                match results with
//                | { result = value } => value * 2
//                | { error = msg } => 0
//            in
//            let calc1 = safeDiv 10 2 in
//            let calc2 = safeDiv 10 0 in
//            (processResults calc1) + (processResults calc2)
//        """.trimIndent()
//        assertTypeCheckSuccess(source, "Int")
//    }
//
//    @Test
//    fun testMutualRecursionBetweenFunctions() {
//        val source = """
//            let rec isEven = \n =>
//                if n == 0 then true
//                else isOdd (n - 1)
//            and isOdd = \n =>
//                if n == 0 then false
//                else isEven (n - 1)
//            in
//            let result1 = isEven 4 in
//            let result2 = isOdd 4 in
//            result1 && !result2
//        """.trimIndent()
//        assertTypeCheckSuccess(source, "Bool")
//    }
//
//    @Test
//    fun testComplexConstraintSolving() {
//        val source = """
//            let processRecord = \record =>
//                let extended = { ...record, processed = true, timestamp = 12345 } in
//                let validated = if extended.name != "" then extended else { ...extended, error = "Invalid name" } in
//                validated
//            in
//            let input = { name = "Test", value = 42 } in
//            let result = processRecord input in
//            result.processed
//        """.trimIndent()
//        assertTypeCheckSuccess(source, "Bool")
//    }
//
//    @Test
//    fun testTypeInferenceAcrossModuleBoundaries() {
//        val source = """
//            let utilities = {
//                stringify = \value => toString value,
//                sum = \list =>
//                    match list with
//                    | [] => 0
//                    | head :: tail => head + (utilities.sum tail),
//                compose = \f => \g => \x => f (g x)
//            } in
//            let numbers = [1, 2, 3, 4, 5] in
//            let total = utilities.sum numbers in
//            let double = \x => x * 2 in
//            let increment = \x => x + 1 in
//            let doubleAndIncrement = utilities.compose increment double in
//            let processed = doubleAndIncrement total in
//            utilities.stringify processed
//        """.trimIndent()
//        assertTypeCheckSuccess(source, "String")
//    }
//
//    // Error cases for regression testing
//
//    @Test
//    fun testTypeErrorInArithmetic() {
//        val source = """
//            let x = 5 + "hello" in
//            x
//        """.trimIndent()
//        assertTypeCheckFailure(source, "Cannot unify")
//    }
//
//    @Test
//    fun testUndefinedVariableError() {
//        val source = """
//            let x = unknownVariable + 5 in
//            x
//        """.trimIndent()
//        assertTypeCheckFailure(source, "Undefined variable")
//    }
//
//    @Test
//    fun testIncompletePatternMatchError() {
//        val source = """
//            let processOption = \opt =>
//                match opt with
//                | Some value => value
//                // Missing None case
//            in
//            processOption (Some 42)
//        """.trimIndent()
//        assertTypeCheckFailure(source, "non-exhaustive")
//    }
//
//    @Test
//    fun testRecordFieldAccessError() {
//        val source = """
//            let person = { name = "Alice", age = 30 } in
//            person.invalidField
//        """.trimIndent()
//        assertTypeCheckFailure(source, "field")
//    }
//
//    @Test
//    fun testFunctionArityError() {
//        val source = """
//            let add = \x => \y => x + y in
//            add 5 3 2  // Too many arguments
//        """.trimIndent()
//        assertTypeCheckFailure(source)
//    }
//
//    @Test
//    fun testCircularTypeReference() {
//        val source = """
//            type rec BadType = BadType
//            let x : BadType = x in
//            x
//        """.trimIndent()
//        assertTypeCheckFailure(source, "circular")
//    }
//
//    // Performance stress tests
//
//    @Test
//    fun testLargeRecordTypeInference() {
//        val fields = (1..50).map { "field$it: $it" }.joinToString(", ")
//        val source = """
//            let largeRecord = { $fields } in
//            largeRecord.field25
//        """.trimIndent()
//        assertTypeCheckSuccess(source, "Int")
//    }
//
//    @Test
//    fun testDeepFunctionNesting() {
//        val nestedLambdas = (1..20).fold("x") { acc, _ -> "\f$acc => f$acc ($acc)" }
//        val source = """
//            let deepNesting = $nestedLambdas in
//            let identity = \x => x in
//            deepNesting identity 42
//        """.trimIndent()
//        assertTypeCheckSuccess(source, "Int")
//    }
//
//    @Test
//    fun testComplexConstraintChain() {
//        val source = """
//            let chain = \f1 => \f2 => \f3 => \f4 => \f5 => \x =>
//                f1 (f2 (f3 (f4 (f5 x))))
//            in
//            let increment = \x => x + 1 in
//            let double = \x => x * 2 in
//            let toString' = \x => toString x in
//            let length = \s => s.length in
//            let isEven = \n => n % 2 == 0 in
//            let complexChain = chain increment double toString' length isEven in
//            complexChain 5
//        """.trimIndent()
//        assertTypeCheckSuccess(source, "Bool")
//    }
//
//    // Real-world usage patterns
//
//    @Test
//    fun testConfigurationMerging() {
//        val source = """
//            let defaultConfig = {
//                timeout = 5000,
//                retries = 3,
//                debug = false,
//                endpoints = { api = "localhost:8080", ws = "localhost:8081" }
//            } in
//            let userConfig = {
//                timeout = 10000,
//                debug = true,
//                endpoints = { api = "production.api.com" }
//            } in
//            let mergeConfigs = \default => \user =>
//                { ...default, ...user, endpoints = { ...default.endpoints, ...user.endpoints } }
//            in
//            let finalConfig = mergeConfigs defaultConfig userConfig in
//            finalConfig.timeout + finalConfig.retries
//        """.trimIndent()
//        assertTypeCheckSuccess(source, "Int")
//    }
//
//    @Test
//    fun testGenericDataPipeline() {
//        val source = """
//            let pipeline = {
//                map = \f => \list =>
//                    match list with
//                    | [] => []
//                    | head :: tail => (f head) :: (pipeline.map f tail),
//
//                filter = \predicate => \list =>
//                    match list with
//                    | [] => []
//                    | head :: tail =>
//                        if predicate head then
//                            head :: (pipeline.filter predicate tail)
//                        else
//                            pipeline.filter predicate tail,
//
//                reduce = \combiner => \initial => \list =>
//                    match list with
//                    | [] => initial
//                    | head :: tail =>
//                        pipeline.reduce combiner (combiner initial head) tail
//            } in
//            let numbers = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10] in
//            let evens = pipeline.filter (\x => x % 2 == 0) numbers in
//            let doubled = pipeline.map (\x => x * 2) evens in
//            let sum = pipeline.reduce (\acc => \x => acc + x) 0 doubled in
//            sum
//        """.trimIndent()
//        assertTypeCheckSuccess(source, "Int")
//    }
//
//    @Test
//    fun testAsyncPatternSimulation() {
//        val source = """
//            let async = {
//                success = \value => { status = "success", data = value },
//                error = \message => { status = "error", error = message },
//
//                then = \asyncResult => \successCallback => \errorCallback =>
//                    match asyncResult with
//                    | { status = "success", data = value } => successCallback value
//                    | { status = "error", error = message } => errorCallback message
//                    | _ => async.error "Invalid async result"
//            } in
//            let fetchUser = \id =>
//                if id > 0 then
//                    async.success { id = id, name = "User " + toString id }
//                else
//                    async.error "Invalid user ID"
//            in
//            let processUser = \user =>
//                async.success ("Processing user: " + user.name)
//            in
//            let handleError = \error =>
//                async.success ("Error handled: " + error)
//            in
//            let result = fetchUser 123 in
//            let processed = async.then result processUser handleError in
//            processed
//        """.trimIndent()
//        assertTypeCheckSuccess(source)
//    }
} 