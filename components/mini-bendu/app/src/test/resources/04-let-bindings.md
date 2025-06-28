# Let and Let Rec Bindings

This chapter explores let bindings and recursive let bindings in Mini-Bendu, including type generalization, recursive functions, type annotations, and binding patterns.

---

### Simple let binding

Tests basic let binding with type inference.

```bendu
let x = 42 in x
-- Expected: Int
```

---

### Let binding with type annotation

Tests let binding with explicit type annotation.

```bendu
let x: Int = 42 in x
-- Expected: Int
```

---

### Let binding with string

Tests let binding with string literals.

```bendu
let message = "hello" in message
-- Expected: String
```

---

### Let binding with boolean

Tests let binding with boolean literals.

```bendu
let flag = True in flag
-- Expected: Bool
```

---

### Nested let bindings

Tests nested let expressions.

```bendu
let x = 5 in
let y = x + 3 in
y * 2
-- Expected: Int
```

---

### Let binding with function

Tests let binding with function values.

```bendu
let f = \x => x + 1 in f(5)
-- Expected: Int
```

---

### Let binding with polymorphic function

Tests let binding with polymorphic function and generalization.

```bendu
let identity = \x => x in
let result1 = identity(42) in
let result2 = identity("hello") in
result1
-- Expected: Int
```

---

### Multiple polymorphic applications

Tests polymorphic function used with different types.

```bendu
let identity = \x => x in
let result1 = identity(42) in
let result2 = identity("hello") in
(result1, result2)
-- Expected: (Int, String)
```

---

### Let binding with explicit polymorphic type

Tests let binding with explicit polymorphic type annotation.

```bendu
let identity[A]: A -> A = \x => x in
identity(42)
-- Expected: Int
```

---

### Let binding with record

Tests let binding with record literals.

```bendu
let person = { name = "Alice", age = 30 } in
person.name
-- Expected: String
```

---

### Let binding with tuple

Tests let binding with tuple literals.

```bendu
let pair = (42, "hello") in
match pair with (x, y) => x
-- Expected: Int
```

---

### Simple recursive function

Tests basic recursive function with let rec.

```bendu
let rec factorial = \n =>
    if n == 0 then 1 else n * factorial(n - 1) in
factorial(4)
-- Expected: Int
```

---

### Recursive function with named syntax

Tests recursive function using named function syntax.

```bendu
let rec factorial(n) =
    if n == 0 then 1 else n * factorial(n - 1) in
factorial(3)
-- Expected: Int
```

---

### Recursive function with type annotation

Tests recursive function with explicit type annotation.

```bendu
let rec factorial(n: Int): Int =
    if n == 0 then 1 else n * factorial(n - 1) in
factorial(5)
-- Expected: Int
```

---

### Recursive function with data structures

Tests recursive function working with recursive data types.

```bendu
type List[A] = {tag: "Cons", head: A, tail: List[A]} | {tag: "Nil"}
let rec length = \list =>
    match list with
      {tag = "Nil"} => 0
    | {tag = "Cons", head = h, tail = t} => 1 + length(t) in
length({tag = "Cons", head = 1, tail = {tag = "Nil"}})
-- Expected: Int
```

---

### Recursive function with polymorphic type

Tests recursive function with polymorphic type parameter.

```bendu
type List[A] = {tag: "Cons", head: A, tail: List[A]} | {tag: "Nil"}
let rec length[A](list: List[A]): Int =
    match list with
      {tag = "Nil"} => 0
    | {tag = "Cons", head = h, tail = t} => 1 + length(t) in
length({tag = "Cons", head = "hello", tail = {tag = "Nil"}})
-- Expected: Int
```

---

### Let binding with conditional

Tests let binding with conditional expressions.

```bendu
let result = if True then 42 else 17 in
result
-- Expected: Int
```

---

### Let binding with union type

Tests let binding that results in union type.

```bendu
let value = if True then 42 else "hello" in
value
-- Expected: Int | String
```

---

### Let binding with pattern matching

Tests let binding with pattern matching expressions.

```bendu
let result = match (42, "hello") with (x, y) => x in
result
-- Expected: Int
```

---

### Let binding with higher-order function

Tests let binding with higher-order functions.

```bendu
let apply = \f => \x => f(x) in
let double = \x => x * 2 in
apply(double)(5)
-- Expected: Int
```

---

### Let binding with function composition

Tests let binding with composed functions.

```bendu
let compose = \f => \g => \x => f(g(x)) in
let increment = \x => x + 1 in
let double = \x => x * 2 in
let incrementThenDouble = compose(double)(increment) in
incrementThenDouble(5)
-- Expected: Int
```

---

### Let binding with curried function

Tests let binding with curried function applications.

```bendu
let add = \x => \y => x + y in
let addFive = add(5) in
addFive(3)
-- Expected: Int
```

---

### Let binding with record manipulation

Tests let binding with record operations.

```bendu
let person = { name = "Bob", age = 25 } in
let updatedPerson = { ...person, age = 26 } in
updatedPerson.age
-- Expected: Int
```

---

### Let binding with type alias

Tests let binding with user-defined type aliases.

```bendu
type Person = { name: String, age: Int }
let alice: Person = { name = "Alice", age = 30 } in
alice.name
-- Expected: String
```

---

### Let binding with generic type

Tests let binding with generic type instantiation.

```bendu
type Option[A] = {tag: "Some", value: A} | {tag: "None"}
let someValue: Option[Int] = {tag = "Some", value = 42} in
match someValue with
  {tag = "Some", value = v} => v
| {tag = "None"} => 0
-- Expected: Int
```

---

### Recursive function with error handling

Tests recursive function with error handling patterns.

```bendu
type Result[A, B] = {tag: "Ok", value: A} | {tag: "Error", error: B}
let rec safeDivide = \x => \y =>
    if y == 0 then {tag = "Error", error = "Division by zero"}
    else {tag = "Ok", value = x / y} in
match safeDivide(10)(2) with
  {tag = "Ok", value = v} => v
| {tag = "Error", error = e} => 0
-- Expected: Int
```

---

### Let binding with row polymorphism

Tests let binding with row polymorphic records.

```bendu
let getName = \record => record.name in
let person = { name = "Charlie", age = 35, city = "Boston" } in
getName(person)
-- Expected: String
```

---

### Let binding with type constraints

Tests let binding where type is constrained by usage.

```bendu
let process = \x => x + 1 in
let result: Int = process(5) in
result
-- Expected: Int
```

---

### Let binding with complex generalization

Tests let binding with complex type generalization scenarios.

```bendu
let makeList = \x => {tag = "Cons", head = x, tail = {tag = "Nil"}} in
let intList = makeList(42) in
let stringList = makeList("hello") in
intList.head
-- Expected: Int
```

---

### Let binding with nested functions

Tests let binding with functions defined within other functions.

```bendu
let outer = \x =>
    let inner = \y => x + y in
    inner(10) in
outer(5)
-- Expected: Int
```

---

### Let binding with multiple type parameters

Tests let binding with functions having multiple type parameters.

```bendu
let pair[A, B] = \x => \y => (x, y) in
let result = pair(42)("hello") in
match result with (a, b) => a
-- Expected: Int
```

---

### Let binding type annotation mismatch

Tests error handling when let binding type annotation doesn't match.

```bendu
let x: String = 42 in x
-- Expected: Cannot unify Int with String
```

---

### Let binding with complex constraint solving

Tests let binding that requires complex constraint solving.

```bendu
let process = \f => \x => f(x) in
let double = \y => y * 2 in
let result = process(double)(5) in
result
-- Expected: Int
```

---

### Let binding with record field constraints

Tests let binding where record field access constrains types.

```bendu
let getAge = \person => person.age in
let alice = { name = "Alice", age = 30 } in
getAge(alice)
-- Expected: Int
```

---

### Let binding with function type annotation

Tests let binding with explicit function type annotation.

```bendu
let f: Int -> String = \x => "number" in
f(42)
-- Expected: String
```

---

### Let binding with partial application constraint

Tests let binding where partial application constrains the type.

```bendu
let add = \x => \y => x + y in
let addTen: Int -> Int = add(10) in
addTen(5)
-- Expected: Int
```

---

### Recursive function with simple arithmetic

Tests recursive function with simple arithmetic operations.

```bendu
let rec power = \base => \exp =>
    if exp == 0 then 1 else base * power(base)(exp - 1) in
power(2)(3)
-- Expected: Int
``` 

### Let binding with simple conditional recursion

Tests recursive function with simple conditional logic.

```bendu
let rec fib = \n =>
    if n == 0 then 0 else if n == 1 then 1 else fib(n - 1) + fib (n - 2) in
fib(5)
-- Expected: Int
``` 