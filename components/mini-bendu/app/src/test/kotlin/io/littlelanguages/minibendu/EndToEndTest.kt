package io.littlelanguages.minibendu

import io.littlelanguages.minibendu.typesystem.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.fail

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

                // Use the new typeCheckProgram method that handles both type aliases and expressions
                val incrementalResult = typeChecker.typeCheckProgram(program)
                
                if (incrementalResult.hasErrors) {
                    // Return the first error
                    incrementalResult.errors.first()
                } else {
                    // Return the result of the last expression (skip type alias declarations)
                    val expressionResults = incrementalResult.results.zip(program.topLevels)
                        .filter { (_, topLevel) -> topLevel is ExprStmt }
                        .map { (result, _) -> result }
                    
                    if (expressionResults.isEmpty()) {
                        TypeCheckResult.Failure("No expressions found in program", SourceLocation(1, 1))
                    } else {
                        expressionResults.last()
                    }
                }
            }
        } catch (e: Exception) {
            TypeCheckResult.Failure("Parse error: ${e.message}", SourceLocation(1, 1))
        }
    }

    private fun assertTypeCheckSuccess(source: String, expectedType: Any? = null) {
        val result = parseAndTypeCheck(source.trimIndent())

        if (result is TypeCheckResult.Failure) {
            fail("Type check failed: ${result.error}")
        }

        assertTrue(result is TypeCheckResult.Success, "Expected type check to succeed for: $source")
        if (expectedType != null) {
            val actualType = result.getFinalType().toString()
            when (expectedType) {
                is String -> assertEquals(expectedType, actualType, "Type mismatch for source: $source\nExpected: $expectedType\nActual: $actualType")
                is Regex -> assertTrue(expectedType.matches(actualType), "Type mismatch for source: $source\nExpected to match: $expectedType\nActual: $actualType")
                else -> fail("Unsupported expectedType: $expectedType")
            }
        }
    }

    private fun assertTypeCheckFailure(source: String, expectedErrorContains: String? = null) {
        val result = parseAndTypeCheck(source.trimIndent())
        assertTrue(result is TypeCheckResult.Failure, "Expected type check to fail for: $source")
        if (expectedErrorContains != null) {
            assertTrue(
                result.error.contains(expectedErrorContains),
                "Expected error to contain '$expectedErrorContains' but got: ${result.error}"
            )
        }
    }

    @Test
    fun testSimpleArithmeticProgram() {
        assertTypeCheckSuccess(
            """
                let x = 5 + 3 in
                let y = x * 2 in
                y
            """, "Int")
    }

    @Test
    fun testRecursiveFunction() {
        assertTypeCheckSuccess(
            """
                let rec fib = \n =>
                    if n == 0 then 0 else if n == 1 then 1 else fib(n - 1) + fib (n - 2) in
                fib(5)
            """, "Int")
    }

    @Test
    fun testPolymorphicIdentityFunction() {
        assertTypeCheckSuccess(
            """
                let identity = \x => x in
                let result1 = identity(42) in
                let result2 = identity("hello") in
                result1
            """, "Int")

        assertTypeCheckSuccess(
            """
                let identity[A]: A -> A = \x => x in
                let result1 = identity(42) in
                let result2 = identity("hello") in
                result1
            """, "Int")

        assertTypeCheckSuccess(
            """
                let identity = \x => x in
                let result1 = identity(42) in
                let result2 = identity("hello") in
                (result1, result2)
            """, "(Int, String)")

        assertTypeCheckSuccess(
            """
                let identity(x) = x in
                let result1 = identity(42) in
                let result2 = identity("hello") in
                result1
            """, "Int")

        assertTypeCheckSuccess(
            """
                let identity(x) = x in
                let result1 = identity(42) in
                let result2 = identity("hello") in
                (result1, result2)
            """, "(Int, String)")

        assertTypeCheckSuccess(
            """
                let identity[A](x: A): A = x in
                let result1 = identity(42) in
                let result2 = identity("hello") in
                (result1, result2)
            """, "(Int, String)")

        assertTypeCheckSuccess(
            """
                let identity = \x => x
                
                let result1 = identity(42) in
                let result2 = identity("hello") in
                result1
            """, "Int")

        assertTypeCheckSuccess(
            """
                let identity(x) = x
                
                let result1 = identity(42) in
                let result2 = identity("hello") in
                result1
            """, "Int")

        assertTypeCheckSuccess(
            """
                let identity[A](x: A): A = x
                
                let result1 = identity(42) in
                let result2 = identity("hello") in
                result1
            """, "Int")
    }

    @Test
    fun testRecordManipulationProgram() {
        assertTypeCheckSuccess(
            """
                let person = { name = "Alice", age = 30 } in
                let updatedPerson = { ...person, age = 31 } in
                updatedPerson.age
            """, "Int")

        assertTypeCheckSuccess(
            """
                let person = { name = "Alice", age = 30 }
                let updatedPerson = { ...person, age = 31 }

                updatedPerson.age
            """, "Int")

        assertTypeCheckSuccess(
            """
                type Person = { name: String, age: Int }
                
                let person: Person = { name = "Alice", age = 30 }
                let updatedPerson = { ...person, age = 31, initials = "AS" }
                
                updatedPerson
            """, "\\{name: String, age: Int, initials: String \\| τ\\d+}".toRegex())
    }

    @Test
    fun testRowPolymorphismWithFunctions() {
        assertTypeCheckSuccess(
            """
                let getName(record): String = record.name

                let person = { name = "Bob", age = 25 } in
                let company = { name = "TechCorp", employees = 100 } in

                (getName(person), getName(company))
            """, "(String, String)")

        assertTypeCheckSuccess(
            """
                let getName[A](record: {name: String, ...A}): String = record.name

                let person = { name = "Bob", age = 25 } in
                let company = { name = "TechCorp", employees = 100 } in

                (getName(person), getName(company))
            """, "(String, String)")

        assertTypeCheckSuccess(
            """
                let getName[A, B](record: {name: B, ...A}): B = record.name

                let person = { name = "Bob", age = 25 } in
                let company = { name = "TechCorp", employees = 100 } in

                (getName(person), getName(company))
            """, "(String, String)")

        assertTypeCheckFailure(
            """
                let getName[A](record: {name: B, ...A}): B = record.name

                let person = { name = "Bob", age = 25 } in
                let company = { name = "TechCorp", employees = 100 } in

                (getName(person), getName(company))
            """, "Undefined type variable")

        assertTypeCheckSuccess(
            """
                let getName[A, B](record: {name: B, ...A}): B = 
                    let name: B = record.name
                    in name
                
                let person = { name = "Bob", age = 25 } in
                let company = { name = "TechCorp", employees = 100 } in
                
                (getName(person), getName(company))
            """, "(String, String)")
    }

    @Test
    fun testComplexUnionTypeProgram() {

        assertTypeCheckSuccess(
            """
                let processValue(value: Int | String): Int | String =
                    match value with
                      n : Int => n + 1
                    | s : String => s
                    | _ => "unknown"
                in
                let result1 = processValue(42) in
                let result2 = processValue("hello") in
                (result1, result2)
            """, "(Int | String, Int | String)")
    }

    @Test
    fun testRecursiveDataStructures() {
        assertTypeCheckSuccess(
            """
                type List[A] = {tag: "Cons", head: A, tail: List[A]} | {tag: "Nil"}
                
                let rec list = \input =>
                    match input with
                      {tag = "Nil"} => 0
                    | {tag = "Cons", head = head, tail = tail} => 1 + list(tail)
                in
                list({tag = "Cons", head = 1, tail = {tag = "Cons", head = 2, tail = {tag = "Nil"}}})
            """, "Int")
    }

    @Test
    fun testHigherOrderFunctionComposition() {
        assertTypeCheckSuccess(
            """
                type List[A] = {tag: "Cons", head: A, tail: List[A]} | {tag: "Nil"}

                let rec map[A, B](f: A -> B, list: List[A]): List[B] =
                    match list with
                      {tag = "Nil"} => {tag = "Nil"}
                    | {tag = "Cons", head = head, tail = tail} => {tag = "Cons", head = f(head), tail = map(f)(tail)}
                in
                let double = \x => x * 2 in
                let increment = \x => x + 1 in
                let numbers = {tag = "Cons", head = 1, tail = {tag = "Cons", head = 2, tail = {tag = "Cons", head = 3, tail = {tag = "Nil"}}}} in
                let doubled = map(double)(numbers) in
                let incremented: List[Int] = map(increment)(doubled) in
                incremented
            """
        )
    }

    @Test
    fun testComplexPatternMatching() {
        assertTypeCheckSuccess(
            """
                let processData = \data =>
                    match data with
                      { tag = "user", user = { name = userName, active = True } } =>
                        "Active user"
                    | { tag = "user", user = { name = userName, active = False } } =>
                        "Inactive user"
                    | { tag = "admin", permissions = perms } =>
                        "Admin with permissions"
                    | _ => "Unknown data type"
                in
                let userData = { tag = "admin", permissions = True } in
                processData(userData)
            """, "String")
    }

    @Test
    fun testIntersectionTypesInPractice() {
        val source = """
            let createEntity = \name => \age =>
                { name = name, age = age, ...{ id = 1, active = True } }
            in
            let entity = createEntity("Bob")(30) in
            if entity.active then entity.name else "Inactive"
        """
        assertTypeCheckSuccess(source, "String")
    }

    @Test
    fun testTypeRefinementInConditions() {
        val source = """
            let toString(n: Int): String = "hello"
            let typeof(v: Int | String): String = "whatever"
            
            let handleValue = \value =>
                if typeof(value) == "string" then
                    value
                else if typeof(value) == "number" then
                    toString(value)
                else
                    "Unknown type"
            in
            handleValue "test"
        """
        assertTypeCheckSuccess(source, "String")
    }

    @Test
    fun testComplexRecordInheritancePattern() {
        val source = """
            let toString(n: Int): String = "hello"
            let concat(a: String, b: String): String = a
            
            let baseConfig = { timeout = 30, retries = 3 } in
            let httpConfig = { ...baseConfig, method = "GET", url = "https://api.example.com" } in
            let apiConfig = { ...httpConfig, apiKey = "secret", version = "v1" } in
            let makeRequest = \config =>
                concat(concat(concat(concat(concat("Making ", config.method), " request to "), config.url),
                " with timeout "), toString(config.timeout))
            in
            makeRequest(apiConfig)
        """
        assertTypeCheckSuccess(source, "String")
    }

    @Test
    fun testErrorPropagationAndRecovery() {
        assertTypeCheckSuccess(
            """
                let safeDiv = \x => \y =>
                    match y with
                      0 => { error = "Division by zero" }
                    | _ => { result = x / y }
                in
                let processResults = \results =>
                    match results with
                      { result = value } => value * 2
                    | { error = msg } => 0
                in
                let calc1 = safeDiv(10)(2) in
                let calc2 = safeDiv(10)(0) in
                processResults(calc1) + processResults(calc2)
            """, "Int")

        assertTypeCheckSuccess(
            """
                type Result[A, B] = { error: A } | { result: B}
                
                let safeDiv(x: Int, y: Int): Result[String, Int] =
                  if y == 0 then { error = "Division by zero" } else { result = x / y }
                in
                let processResults(results: Result[String, Int]) =
                  match results with
                    { result = value } => value * 2
                  | { error = msg } => 0
                in
                let calc1 = safeDiv(10, 2) in
                let calc2 = safeDiv(10, 0) in
                processResults(calc1) + processResults(calc2)
            """, "Int")
    }

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
//        """
//        assertTypeCheckSuccess(source, "Bool")
//    }
//
    @Test
    fun testComplexConstraintSolving() {
        val source = """
            let processRecord = \record =>
                let extended = { ...record, processed = True, timestamp = 12345 } in
                let validated = if extended.name != "" then extended else { ...extended, error = "Invalid name" } in
                validated
            in
            let input = { name = "Test", value = 42 } in
            let result = processRecord(input) in
            result.processed
        """
        assertTypeCheckSuccess(source, "Bool")
    }

    @Test
    fun testSample() {
        assertTypeCheckSuccess(
            """
                type List[A] = {tag: "Cons", head: A, tail: List[A]} | {tag: "Nil"}

                let numbers: List[Int] = {tag = "Cons", head = 1, tail = {tag = "Nil"}} in

                let rec utilities = {
                    sum = \list:List[Int] =>
                        match list with
                          {tag = "Nil"} => 0
                        | {tag = "Cons", head = head, tail = tail} => head + utilities.sum(tail)
                } in
                let total = utilities.sum(numbers) in
                total
            """)
    }

    @Test
    fun testSample2() {
        assertTypeCheckSuccess(
            """
                type Option[A] = {tag: "Some", value: A} | {tag: "None"}

                let number: Option[Int] = {tag = "Some", value = 1}

                let utilities = {
                    withDefault = \list:Option[Int] =>
                        match list with
                          {tag = "None"} => 0
                        | {tag = "Some", value = value} => value
                }
                
                utilities.withDefault(number)
            """, "Int")
    }

    @Test
    fun testTypeInferenceAcrossModuleBoundaries() {
        assertTypeCheckSuccess(
            """
                type List[A] = {tag: "Cons", head: A, tail: List[A]} | {tag: "Nil"}

                let toString(value: Int): String = "hello"

                // Non-recursive utilities to avoid edge case with recursive records + polymorphic operators
                let rec utilities = {
                    stringify = \value => toString(value),
                    compose = \f => \g => \x => f(g(x)),
                    sum = \list: List[Int] =>
                        match list with
                          {tag = "Nil"} => 1
                        | {tag = "Cons", head = head, tail = tail} => head + utilities.sum(tail)
                } in
                
                // Separate recursive function (using multiplication to avoid polymorphic + edge case)
                
                let numbers: List[Int] = {tag = "Cons", head = 1, tail = {tag = "Nil"}} in
                let total = utilities.sum(numbers) in
                let double = \x => x * 2 in
                let increment = \x => x + 1 in
                let doubleAndIncrement = utilities.compose(increment)(double) in
                let processed = doubleAndIncrement(total) in
                utilities.stringify(processed)
            """, "String")

        assertTypeCheckSuccess(
            """
                let double(x: Int): Int = x * 2
                let increment(x: Int): Int = x + 1
                
                let rec utilities = {
                    compose = \f => \g => \x => f(g(x))
                } in
                let result = utilities.compose(increment)(double)(200) in
                result
            """, "Int")
    }

    // Error cases for regression testing

    @Test
    fun testTypeErrorInArithmetic() {
        val source = """
            let x = 5 + "hello" in
            x
        """
        assertTypeCheckFailure(source, "Cannot unify")
    }

    @Test
    fun testUndefinedVariableError() {
        val source = """
            let x = unknownVariable + 5 in
            x
        """
        assertTypeCheckFailure(source, "Undefined variable")
    }

    @Test
    fun testIncompletePatternMatchError() {
        val source = """
            type Option[A] = {tag: "Some", value: A} | {tag: "None"}
            
            let processOption(opt: Option[Int]): Int =
                match opt with
                  {tag = "Some", value = value} => value
                // Missing None case
            in
            processOption({tag = "Some", value = 42})
        """
        assertTypeCheckFailure(source, "non-exhaustive")
    }

    @Test
    fun testRecordFieldAccessError() {
        val source = """
            let person = { name = "Alice", age = 30 } in
            person.invalidField
        """
        assertTypeCheckFailure(source, "field")
    }

    @Test
    fun testFunctionArityError() {
        val source = """
            let add = \x => \y => x + y in
            add(5)(3)(2)  // Too many arguments
        """
        assertTypeCheckFailure(source)
    }

//    @Test
//    fun testCircularTypeReference() {
//        val source = """
//            type rec BadType = BadType
//            let x : BadType = x in
//            x
//        """
//        assertTypeCheckFailure(source, "circular")
//    }

    // Performance stress tests

    @Test
    fun testLargeRecordTypeInference() {
        val fields = (1..50).joinToString(", ") { "field$it = $it" }
        assertTypeCheckSuccess(
            """
                let largeRecord = { $fields } in
                largeRecord.field25
            """, "Int")
    }

    @Test
    fun testComplexConstraintChain() {
        val source = """
            let chain = \f1 => \f2 => \f3 => \f4 => \f5 => \x =>
                f1(f2(f3(f4(f5(x)))))
            in
            let increment(x) = x + 1 in
            let double(x) = x * 2 in
            let toString(x) = "hello" in
            let length(s) = 10 in
            let isEven(n) = n / 2 == 0 in
            let complexChain = chain(isEven)(increment)(double)(length)(toString) in
            complexChain(5)
        """
        assertTypeCheckSuccess(source, "Bool")
    }

    // Real-world usage patterns

    @Test
    fun testConfigurationMerging() {
        val source = """
            let defaultConfig = {
                timeout = 5000,
                retries = 3,
                debug = False,
                endpoints = { api = "localhost:8080", ws = "localhost:8081" }
            } in
            let userConfig = {
                timeout = 10000,
                debug = True,
                endpoints = { api = "production.api.com" }
            } in
            let mergeConfigs = \default => \user =>
                { ...default, ...user, endpoints = { ...default.endpoints, ...user.endpoints } }
            in
            let finalConfig = mergeConfigs(defaultConfig)(userConfig) in
            finalConfig.timeout + finalConfig.retries
        """
        assertTypeCheckSuccess(source, "Int")
    }

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
//        """
//        assertTypeCheckSuccess(source, "Int")
//    }

    @Test
    fun testAsyncPatternSimulation() {
        val source = """
            let toString(n: Int): String = "hello"
            
            type AsyncResult[A, B] = { status: "success", data: A } | { status: "error", error: B }
            
            let async = {
                success = \value => { status = "success", data = value },
                error = \message => { status = "error", error = message },

                thens = \asyncResult => \successCallback => \errorCallback =>
                    match asyncResult with
                      { status = "success", data = value } => successCallback(value)
                    | { status = "error", error = message } => errorCallback(message)
                    | _ => errorCallback("Invalid async result")
            } in
            let fetchUser = \id =>
                if id == 0 then
                    async.success({ id = id, name = toString(id) })
                else
                    async.error("Invalid user ID")
            in
            let processUser = \user =>
                async.success (user.name)
            in
            let handleError = \error =>
                async.success (error)
            in
            let result = fetchUser(123) in
            let processed = async.thens(result)(processUser)(handleError) in
            async
        """
        assertTypeCheckSuccess(source)
    }

    @Test
    fun testReturnTypeAnnotationInFunctionSyntax() {
        // This should work: function syntax with return type annotation
        assertTypeCheckSuccess(
            """
                let getName(record): String = record.name in
                let person = { name = "Bob", age = 25 } in
                let company = { name = "TechCorp", employees = 100 } in
                let name1 = getName(person) in
                let name2 = getName(company) in
                name1
            """, "String")
    }

    @Test
    fun testRowPolymorphismWithoutReturnTypeAnnotation() {
        // This works without return type annotation - for comparison
        assertTypeCheckSuccess(
            """
                let getName(record) = record.name in
                let person = { name = "Bob", age = 25 } in
                let company = { name = "TechCorp", employees = 100 } in
                let name1 = getName(person) in
                let name2 = getName(company) in
                name1
            """, "String")
    }

    @Test
    fun testSimpleRecursiveTypeAlias() {
        val source = """
            type List[A] = {tag: "Cons", head: A, tail: List[A]} | {tag: "Nil"}
            
            let test: List[Int] = {tag = "Nil"}
            
            test
        """
        assertTypeCheckSuccess(source)
    }

    @Test
    fun testSimpleFunctionComposition() {
        assertTypeCheckSuccess(
            """
                let compose = \f => \g => \x => f(g(x)) in
                let double = \x => x * 2 in
                let increment = \x => x + 1 in
                let doubleAndIncrement = compose(increment)(double) in
                let result = doubleAndIncrement(5) in
                result
            """, "Int")
    }

    @Test
    fun testRecursiveRecordIssue() {
        // This should pass (no rec)
        assertTypeCheckSuccess(
            """
                let double(x: Int): Int = x * 2
                let increment(x: Int): Int = x + 1
                
                let utilities = {
                    compose = \f => \g => \x => f(g(x))
                } in 
                let result = utilities.compose(increment)(double)(200) in
                result
            """, "Int")
            
        // This should pass (rec but no in)
        assertTypeCheckSuccess(
            """
                let double(x: Int): Int = x * 2
                let increment(x: Int): Int = x + 1
                
                let rec utilities = {
                    compose = \f => \g => \x => f(g(x))
                }
                
                utilities.compose(increment)(double)(200)
            """, "Int")
            
        // This should fail (rec + in) - the problematic case
        assertTypeCheckSuccess(
            """
                let double(x: Int): Int = x * 2
                let increment(x: Int): Int = x + 1
                
                let rec utilities = {
                    compose = \f => \g => \x => f(g(x))
                } in 
                let result = utilities.compose(increment)(double)(200) in
                result
            """, "Int")
    }
}