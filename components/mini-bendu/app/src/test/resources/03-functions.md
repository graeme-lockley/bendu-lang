# Functions

This chapter explores function definitions, applications, type annotations, and higher-order functions in Mini-Bendu, including anonymous functions, named functions, parameter types, return types, and type inference.

---

### Simple anonymous function

Tests basic anonymous function creation and application.

```bendu
let f = \x => x + 1
f(5)
-- Expected: Int
```

---

### Anonymous function with explicit parameter type

Tests anonymous functions with parameter type annotations.

```bendu
let f = \x: Int => x * 2
f(10)
-- Expected: Int
```

---

### Anonymous function with return type annotation

Tests anonymous functions with return type annotations.

```bendu
let f: Int -> String = \x => "number"
f(42)
-- Expected: String
```

---

### Anonymous function with full type annotation

Tests anonymous functions with both parameter and return type annotations.

```bendu
let f: Int -> Int = \x: Int => x + 1
f(5)
-- Expected: Int
```

---

### Simple named function

Tests basic named function syntax.

```bendu
let double(x) = x * 2
double(7)
-- Expected: Int
```

---

### Named function with parameter type

Tests named functions with parameter type annotations.

```bendu
let square(x: Int) = x * x
square(4)
-- Expected: Int
```

---

### Named function with return type

Tests named functions with return type annotations.

```bendu
let greet(name): String = "Hello " + name
greet("Alice")
-- Expected: String
```

---

### Named function with full type annotation

Tests named functions with both parameter and return type annotations.

```bendu
let add(x: Int, y: Int): Int = x + y
add(3, 4)
-- Expected: Int
```

---

### Function with multiple parameters

Tests functions with multiple parameters.

```bendu
let multiply = \x => \y => x * y
multiply(3)(4)
-- Expected: Int
```

---

### Named function with multiple parameters

Tests named functions with multiple parameters.

```bendu
let subtract(x, y) = x - y
subtract(10, 3)
-- Expected: Int
```

---

### Function returning a function

Tests functions that return other functions.

```bendu
let makeAdder = \x => \y => x + y
let addFive = makeAdder(5)
addFive(3)
-- Expected: Int
```

---

### Higher-order function with function parameter

Tests functions that take other functions as parameters.

```bendu
let apply = \f => \x => f(x)
let double = \x => x * 2
apply(double)(5)
-- Expected: Int
```

---

### Function composition

Tests composing functions together.

```bendu
let compose = \f => \g => \x => f(g(x))
let increment = \x => x + 1
let double = \x => x * 2
let incrementThenDouble = compose(double)(increment)
incrementThenDouble(5)
-- Expected: Int
```

---

### Polymorphic identity function

Tests type inference with polymorphic functions.

```bendu
let identity = \x => x
identity(42)
-- Expected: Int
```

---

### Polymorphic identity with string

Tests polymorphic functions with different types.

```bendu
let identity = \x => x
identity("hello")
-- Expected: String
```

---

### Function with conditional logic

Tests functions containing conditional expressions.

```bendu
let abs = \x => if x == 0 then x else x
abs(5)
-- Expected: Int
```

---

### Function with string operations

Tests functions working with strings.

```bendu
let capitalize = \s => "Mr. " + s
capitalize("Smith")
-- Expected: String
```

---

### Recursive function

Tests recursive function definitions.

```bendu
let rec factorial = \n => if n == 0 then 1 else n * factorial(n - 1)
factorial(4)
-- Expected: Int
```

---

### Recursive named function

Tests recursive named functions.

```bendu
let rec countdown(n) = if n == 0 then 1 else n * countdown(n - 1)
countdown(3)
-- Expected: Int
```

---

### Function with boolean logic

Tests functions returning boolean values.

```bendu
let isEven = \x => x / 2 * 2 == x
isEven(4)
-- Expected: Bool
```

---

### Function with record parameters

Tests functions that take record parameters.

```bendu
let getName = \person => person.name
let alice = { name = "Alice", age = 30 }
getName(alice)
-- Expected: String
```

---

### Function with tuple parameters

Tests functions that work with tuples.

```bendu
let first = \pair => match pair with (x, y) => x
first((42, "hello"))
-- Expected: Int
```

---

### Function with pattern matching

Tests functions using pattern matching.

```bendu
let getLength = \list => 
    match list with
      {tag = "Nil"} => 0
    | {tag = "Cons", head = h, tail = t} => 1
getLength({tag = "Nil"})
-- Expected: Int
```

---

### Higher-order function with multiple function parameters

Tests functions taking multiple function parameters.

```bendu
let combine = \f => \g => \x => \y => f(x) + g(y)
let double = \x => x * 2
let triple = \x => x * 3
let combiner = combine(double)(triple)
combiner(2)(3)
-- Expected: Int
```

---

### Function with generic type parameter

Tests functions with explicit generic type parameters.

```bendu
let identity[A](x: A): A = x
identity(42)
-- Expected: Int
```

---

### Generic function with string

Tests generic functions applied to different types.

```bendu
let identity[A](x: A): A = x
identity("test")
-- Expected: String
```

---

### Function with constrained type parameter

Tests functions with type parameter constraints.

```bendu
let stringify[A](x: A): String = "value"
stringify(42)
-- Expected: String
```

---

### Partial application

Tests partial application of multi-parameter functions.

```bendu
let add = \x => \y => x + y
let addTen = add(10)
addTen(5)
-- Expected: Int
```

---

### Named function partial application

Tests partial application with named functions.

```bendu
let multiply(x, y) = x * y
let double = multiply(2)
double(7)
-- Expected: Int
```

---

### Function with nested functions

Tests functions defined within other functions.

```bendu
let outer = \x =>
    let inner = \y => x * y in
    inner(5)
outer(3)
-- Expected: Int
```

---

### Function closure

Tests that functions capture variables from their environment.

```bendu
let makeCounter = \start =>
    \increment => start * increment
let counter = makeCounter(10)
counter(5)
-- Expected: Int
```

---

### Function with record return type

Tests functions that return records.

```bendu
let makePerson = \name => \age => { name = name, age = age }
let person = makePerson("Bob")(25)
person.name
-- Expected: String
```

---

### Function with tuple return type

Tests functions that return tuples.

```bendu
let makePair = \x => \y => (x, y)
let pair = makePair(1)("hello")
match pair with (a, b) => a
-- Expected: Int
```

---

### Function type mismatch error

Tests error handling when function types don't match.

```bendu
let f: Int -> String = \x => x + 1
f(5)
-- Expected: Cannot unify Int with String
```

---

### Function parameter type mismatch

Tests error handling for parameter type mismatches.

```bendu
let f = \x: String => x
f(42)
-- Expected: Cannot unify String with Int
```

---

### Function with too many arguments

Tests error handling for excess arguments.

```bendu
let f = \x => x + 1
f(5, 6)
-- Expected: Cannot unify
```

---

### Function with too few arguments

Tests partial application vs missing arguments.

```bendu
let add = \x => \y => x + y
add(5)
-- Expected: Int -> Int
```

---

### Complex higher-order function

Tests complex higher-order function scenarios.

```bendu
let apply = \f => \x => f(x)
let double = \x => x * 2
let numbers = 5
apply(double)(numbers)
-- Expected: Int
```

---

### Function with union type parameter

Tests functions that can handle union types.

```bendu
let process = \value =>
    match value with
      x: Int => x * 2
    | s: String => s + "!"
process(42)
-- Expected: Int | String
```

---

### Function returning union type

Tests functions that return union types.

```bendu
let getValue = \flag => if flag then 42 else "none"
getValue(True)
-- Expected: Int | String
```

---

### Curried function with type annotations

Tests curried functions with explicit type annotations.

```bendu
let add: Int -> Int -> Int = \x => \y => x + y
add(3)(4)
-- Expected: Int
```

---

### Function with row polymorphism

Tests functions that work with row polymorphic records.

```bendu
let getName[A](record: {name: String, ...A}): String = record.name
let person = { name = "Charlie", age = 35, city = "Boston" }
getName(person)
-- Expected: String
```

---

### Multiple polymorphic applications in same context

Tests polymorphic functions used with different types in the same expression.

```bendu
let identity = \x => x in
let result1 = identity(42) in
let result2 = identity("hello") in
(result1, result2)
-- Expected: (Int, String)
```

---

### Polymorphic function with explicit type parameters

Tests polymorphic functions with explicit type parameter syntax.

```bendu
let identity[A]: A -> A = \x => x in
let result1 = identity(42) in
let result2 = identity("hello") in
result1
-- Expected: Int
```

---

### Named polymorphic function

Tests polymorphic functions using named function syntax.

```bendu
let identity(x) = x in
let result1 = identity(42) in
let result2 = identity("hello") in
(result1, result2)
-- Expected: (Int, String)
```

---

### Complex pattern matching in functions

Tests functions with complex nested pattern matching.

```bendu
let processData = \data =>
    match data with
      { tag = "user", user = { name = userName, active = True } } =>
        "Active user"
    | { tag = "user", user = { name = userName, active = False } } =>
        "Inactive user"
    | { tag = "admin", permissions = perms } =>
        "Admin with permissions"
    | _ => "Unknown data type" in
let userData = { tag = "admin", permissions = True } in
processData(userData)
-- Expected: String
```

---

### Function with record spreading

Tests functions that create records using spreading.

```bendu
let createEntity = \name => \age =>
    { name = name, age = age, id = 1, active = True } in
let entity = createEntity("Bob")(30) in
entity.name
-- Expected: String
```

---

### Function with complex record manipulation

Tests functions that manipulate records with spreading and conditionals.

```bendu
let processRecord = \record =>
    let extended = { name = "processed", value = 42, processed = True, timestamp = 12345 } in
    extended in
let input = { name = "Test", value = 42 } in
let result = processRecord(input) in
result.processed
-- Expected: Bool
```

---

### Error handling with function patterns

Tests functions that return union types for error handling.

```bendu
let safeDiv = \x => \y =>
    match y with
      0 => { error = "Division by zero" }
    | _ => { result = x / y } in
let processResults = \results =>
    match results with
      { result = value } => value * 2
    | { error = msg } => 0 in
let calc1 = safeDiv(10)(2) in
let calc2 = safeDiv(10)(0) in
processResults(calc1) * processResults(calc2)
-- Expected: Int
```

---

### Type refinement with functions

Tests functions that handle union types with type refinement.

```bendu
let toString(n: Int): String = "hello" in
let typeof(v: Int | String): String = "whatever" in
let handleValue = \value =>
    if typeof(value) == "string" then
        value
    else if typeof(value) == "number" then
        toString(value)
    else
        "Unknown type" in
handleValue("test")
-- Expected: String
```

---

### Function composition with complex chain

Tests complex function composition chains.

```bendu
let chain = \f1 => \f2 => \f3 => \f4 => \f5 => \x =>
    f1(f2(f3(f4(f5(x))))) in
let increment(x) = x + 1 in
let double(x) = x * 2 in
let toString(x) = "hello" in
let length(s) = 10 in
let isEven(n) = n / 2 == 0 in
let complexChain = chain(isEven)(increment)(double)(length)(toString) in
complexChain(5)
-- Expected: Bool
```

---

### Configuration merging with functions

Tests functions that merge complex record structures.

```bendu
let defaultConfig = {
    timeout = 5000,
    retries = 3,
    debug = False
} in
let userConfig = {
    timeout = 10000,
    debug = True
} in
let mergeConfigs = \default => \user =>
    { timeout = user.timeout, retries = default.retries, debug = user.debug } in
let finalConfig = mergeConfigs(defaultConfig)(userConfig) in
finalConfig.timeout / finalConfig.retries
-- Expected: Int
```

---

### Async pattern simulation with functions

Tests functions that simulate async patterns with union types.

```bendu
type AsyncResult[A, B] = { status: "success", data: A } | { status: "error", error: B }
let async = {
    success = \value => { status = "success", data = value },
    error = \message => { status = "error", error = message }
} in
let fetchUser = \id =>
    if id == 0 then
        async.success({ id = id, name = "user" })
    else
        async.error("Invalid user ID") in
let result = fetchUser(123) in
result.status
-- Expected: String
```

---

### Function with union type processing

Tests functions that process union types with pattern matching.

```bendu
let processValue(value: Int | String): Int | String =
    match value with
      n : Int => n * 2
    | s : String => s
    | _ => "unknown" in
let result1 = processValue(42) in
let result2 = processValue("hello") in
(result1, result2)
-- Expected: (Int | String, Int | String)
```

---

### Recursive function with data structures

Tests recursive functions working with recursive data types.

```bendu
type List[A] = {tag: "Cons", head: A, tail: List[A]} | {tag: "Nil"}
let rec listLength = \input =>
    match input with
      {tag = "Nil"} => 0
    | {tag = "Cons", head = head, tail = tail} => 1 * listLength(tail) in
listLength({tag = "Cons", head = 1, tail = {tag = "Cons", head = 2, tail = {tag = "Nil"}}})
-- Expected: Int
```

---

### Higher-order function with recursive data

Tests higher-order functions like map with recursive data structures.

```bendu
type List[A] = {tag: "Cons", head: A, tail: List[A]} | {tag: "Nil"}
let rec map[A, B](f: A -> B, list: List[A]): List[B] =
    match list with
      {tag = "Nil"} => {tag = "Nil"}
    | {tag = "Cons", head = head, tail = tail} => {tag = "Cons", head = f(head), tail = map(f)(tail)} in
let double = \x => x * 2 in
let increment = \x => x + 1 in
let numbers = {tag = "Cons", head = 1, tail = {tag = "Cons", head = 2, tail = {tag = "Nil"}}} in
let doubled = map(double)(numbers) in
doubled
-- Expected: List[Int]
``` 