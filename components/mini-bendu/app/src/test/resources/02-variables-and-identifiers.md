# Variables and Identifiers

This chapter explores variable binding, usage, scoping rules, and identifier resolution in Mini-Bendu, including shadowing behavior and scope boundaries.

---

### Simple variable binding

Tests basic variable binding with let expressions.

```bendu
let x = 42
x
-- Expected: Int
```

---

### Variable binding with type annotation

Tests variable binding with explicit type annotations.

```bendu
let x: Int = 42
x
-- Expected: Int
```

---

### String variable binding

Tests variable binding with string values.

```bendu
let message = "hello world"
message
-- Expected: String
```

---

### Boolean variable binding

Tests variable binding with boolean values.

```bendu
let flag = True
flag
-- Expected: Bool
```

---

### Chained variable bindings

Tests multiple variable bindings in sequence.

```bendu
let x = 10
let y = 20
let z = x + y
z
-- Expected: Int
```

---

### Variable usage in expressions

Tests using variables in arithmetic expressions.

```bendu
let a = 5
let b = 3
a * b + 2
-- Expected: Int
```

---

### Variable usage in string concatenation

Tests using variables in string operations.

```bendu
let greeting = "Hello"
let name = "Alice"
greeting + " " + name
-- Expected: String
```

---

### Variable binding with expression

Tests binding variables to complex expressions.

```bendu
let x = 2 + 3 * 4
x
-- Expected: Int
```

---

### Nested let expressions

Tests variable scoping in nested let expressions.

```bendu
let x = 10
let y = let z = 5 in x + z
y
-- Expected: Int
```

---

### Variable shadowing in nested scope

Tests that inner variables shadow outer variables.

```bendu
let x = 10
let result = let x = 20 in x
result
-- Expected: Int
```

---

### Outer variable access

Tests accessing outer scope variables from inner scope.

```bendu
let x = 10
let y = 20
let result = let z = 5 in x + y + z
result
-- Expected: Int
```

---

### Complex variable shadowing

Tests complex shadowing scenarios with multiple levels.

```bendu
let x = 1
let y = let x = 2 in let x = 3 in x
y
-- Expected: Int
```

---

### Variable scoping with lambda expressions

Tests variable scoping in lambda expressions.

```bendu
let x = 10
let f = \y => x + y
f(5)
-- Expected: Int
```

---

### Lambda parameter shadowing

Tests that lambda parameters shadow outer variables.

```bendu
let x = 10
let f = \x => x * 2
f(5)
-- Expected: Int
```

---

### Variable binding in lambda body

Tests variable binding within lambda expressions.

```bendu
let f = \x => let y = x * 2 in y + 1
f(5)
-- Expected: Int
```

---

### Nested lambda scoping

Tests variable scoping in nested lambda expressions.

```bendu
let x = 10
let f = \y => \z => x + y + z
f(5)(3)
-- Expected: Int
```

---

### Variable type inference

Tests type inference across variable bindings.

```bendu
let identity = \x => x
let result = identity(42)
result
-- Expected: Int
```

---

### Polymorphic variable usage

Tests polymorphic variables with different types.

```bendu
let identity = \x => x
let num = identity(42)
let str = identity("hello")
num
-- Expected: Int
```

---

### Variable in if expressions

Tests variable usage in conditional expressions.

```bendu
let x = 10
let y = 5
if x == y then "equal" else "not equal"
-- Expected: String
```

---

### Variable binding in if branches

Tests variable binding within if expression branches.

```bendu
let x = 10
if x == 10 then 
    let message = "correct" in message
else 
    let message = "incorrect" in message
-- Expected: String
```

---

### Variable scoping across if branches

Tests that variables in different branches don't interfere.

```bendu
let x = 10
let result = if True then 
    let y = 20 in x + y
else 
    let y = 30 in x + y
result
-- Expected: Int
```

---

### Recursive variable binding

Tests recursive variable binding with rec keyword.

```bendu
let rec factorial = \n => if n == 0 then 1 else n * factorial(n - 1)
factorial(3)
-- Expected: Int
```

---

### Self-referencing variable

Tests recursive functions referencing themselves.

```bendu
let rec countdown = \n => if n == 0 then "done" else "step " + countdown(n - 1)
countdown(2)
-- Expected: String
```

---

### Variable in record expressions

Tests variables used in record field values.

```bendu
let name = "Alice"
let age = 30
let person = { name = name, age = age }
person.name
-- Expected: String
```

---

### Variable binding with records

Tests binding variables to record values.

```bendu
let person = { name = "Bob", age = 25 }
let name = person.name
name
-- Expected: String
```

---

### Variable shadowing with records

Tests variable shadowing in record contexts.

```bendu
let name = "Alice"
let person = { name = "Bob" }
let result = let name = person.name in name
result
-- Expected: String
```

---

### Undefined variable error

Tests error handling for undefined variables.

```bendu
unknownVariable
-- Expected: Undefined variable
```

---

### Variable type mismatch

Tests error handling for type mismatches in variable binding.

```bendu
let x: String = 42
x
-- Expected: Cannot unify Int with String
```

---

### Variable in tuple expressions

Tests variables used in tuple construction.

```bendu
let x = 1 in
let y = "hello" in
let z = True in
(x, y, z)
-- Expected: (Int, String, Bool)
```

---

### Variable scoping in match expressions

Tests variable scoping in pattern matching.

```bendu
let x = 10
let result = match 5 with
    y => x + y
result
-- Expected: Int
```

---

### Pattern variable binding

Tests variable binding in pattern matching.

```bendu
let tuple = (1, "hello")
match tuple with
    (x, y) => x
-- Expected: Int
```

---

### Pattern variable shadowing

Tests that pattern variables shadow outer scope.

```bendu
let x = 10
let tuple = (20, 30)
match tuple with
    (x, y) => x
-- Expected: Int
```

---

### Complex variable scoping

Tests complex scoping rules with multiple constructs.

```bendu
let x = 1
let f = \y => 
    let x = 2 in 
    let g = \z => x + y + z in
    g(3)
f(4)
-- Expected: Int
```

---

### Variable with function composition

Tests variables in function composition scenarios.

```bendu
let double = \x => x * 2
let increment = \x => x + 1
let compose = \f => \g => \x => f(g(x))
let doubleAndIncrement = compose(increment)(double)
doubleAndIncrement(5)
-- Expected: Int
``` 