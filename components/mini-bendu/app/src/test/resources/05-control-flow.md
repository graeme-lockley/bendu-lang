# Control Flow

This chapter explores control flow expressions in Mini-Bendu, including `if` expressions, condition evaluation, branch typing, and the behavior of nested and inferred conditionals.

---

### If expression with matching branches

This scenario confirms that when both branches of an `if` return the same type, the expression is well-typed.

```bendu
if True then 42 else 17
-- Expected: Int
```

---

### If expression with different branch types

This tests the behavior when the branches return different types. Mini-Bendu allows union types.

```bendu
if True then 42 else "forty-two"
-- Expected: Int | String
```

---

### If expression in a typed context

This tests that the `if` expression's inferred type is constrained by its usage context.

```bendu
let x: String = if True then "yes" else "no"
x
-- Expected: String
```

---

### If expression where condition is not Boolean

This tests rejection of non-Boolean conditions.

```bendu
if 123 then "yes" else "no"
-- Expected: Cannot unify Int with Bool at 1:1
```

---

### Nested if expressions

Verifies nested `if` expressions and correct branch resolution.

```bendu
if True then if False then 0 else 1 else 2
-- Expected: Int
```

---

### If expression with union of unions

Demonstrates nested union result from branch types.

```bendu
if True then 1 else if True then "one" else False
-- Expected: Int | String | Bool
```

---

### If expression with complex condition

Tests logical operations in conditions and their type checking.

```bendu
let x = 5
let y = 10
if x == y && y == 0 then "valid" else "invalid"
-- Expected: String
```

---

### If expression with function calls

Tests type inference when branches contain function calls.

```bendu
let double(x: Int): Int = x * 2
let square(x: Int): Int = x * x
if True then double(5) else square(5)
-- Expected: Int
```

---

### If expression with record access

Tests type inference with record field access in branches.

```bendu
let person = { name = "Alice", age = 30 }
if True then person.name else person.age
-- Expected: String | Int
```

---

### If expression with pattern matching

Tests integration with pattern matching in branches.

```bendu
type Option[A] = {tag: "Some", value: A} | {tag: "None"}
let opt: Option[Int] = {tag = "Some", value = 42}
if True then 
    match opt with
        {tag = "Some", value = v} => v
        | {tag = "None"} => 0
else 0
-- Expected: Int
```

---

### If expression with type aliases

Tests type inference with user-defined types in branches.

```bendu
type Status = "active" | "inactive"
let status: Status = "active"
if status == "active" then "online" else "offline"
-- Expected: String
```

---

### If expression with error handling

Tests type inference in error handling scenarios.

```bendu
let safeDiv(x: Int, y: Int): Int | String =
    if y == 0 then "division by zero" else x / y
safeDiv(10, 0)
-- Expected: String | Int
```

---

### If expression with recursive types

Tests type inference with recursive type definitions.

```bendu
type List[A] = {tag: "Cons", head: A, tail: List[A]} | {tag: "Nil"}
let list: List[Int] = {tag = "Cons", head = 1, tail = {tag = "Nil"}}
if True then list else {tag = "Nil"}
-- Expected: List[Int]
```

---

### If expression with polymorphic functions

Tests type inference with polymorphic functions in branches.

```bendu
let identity[A](x: A): A = x
if True then identity(42) else identity("hello")
-- Expected: Int | String
```

---

### If expression with row polymorphism

Tests type inference with row polymorphic records.

```bendu
let getName[A](record: {name: String, ...A}): String = record.name
let person = { name = "Bob", age = 25 }
let company = { name = "TechCorp", employees = 100 }
if True then getName(person) else getName(company)
-- Expected: String
```
