# Pattern Matching

This chapter explores pattern matching in Mini-Bendu, including literal, variable, constructor, tuple, record, wildcard patterns, and checks for exhaustiveness.

---

### Match on integer literal

Tests matching on an integer literal pattern.

```bendu
match 42 with
  42 => "matched"
| _ => "not matched"
-- Expected: String
```

---

### Match on string literal

Tests matching on a string literal pattern.

```bendu
match "hello" with
  "hello" => 1
| _ => 0
-- Expected: Int
```

---

### Match on boolean literal

Tests matching on boolean literal patterns.

```bendu
match True with
  True => 1
| False => 0
-- Expected: Int
```

---

### Match with variable pattern

Tests variable binding in patterns.

```bendu
match 99 with
  x => x + 1
-- Expected: Int
```

---

### Match with tuple pattern

Tests tuple destructuring in patterns.

```bendu
match (1, "a") with
  (x, y) => y
-- Expected: String
```

---

### Match with record pattern

Tests record destructuring in patterns.

```bendu
let person = { name = "Alice", age = 30 } in
match person with
  { name = n, age = a } => n
-- Expected: String
```

---

### Match with nested record pattern

Tests nested record patterns.

```bendu
let data = { user = { name = "Bob" }, active = True } in
match data with
  { user = { name = n }, active = True } => n
| _ => "unknown"
-- Expected: String
```

---

### Match with wildcard pattern

Tests using the wildcard pattern.

```bendu
match 123 with
  _ => "anything"
-- Expected: String
```

---

### Match with constructor pattern (sum type)

Tests matching on user-defined constructors.

```bendu
type Option[A] = { tag: "Some", value: A } | { tag: "None" }
let x = { tag = "Some", value = 42 } in
match x with
  { tag = "Some", value = v } => v
| { tag = "None" } => 0
-- Expected: Int
```

---

### Match with overlapping patterns

Tests that the first matching pattern is chosen.

```bendu
match 5 with
  x => x * 2
| 5 => 100
-- Expected: Int
```

---

### Match with non-exhaustive patterns (should error)

Tests error for non-exhaustive match.

```bendu
match True with
  True => 1
-- Expected: non-exhaustive pattern match. Missing patterns
```

---

### Match with exhaustive patterns

Tests that all cases are covered.

```bendu
match False with
  True => 1
| False => 0
-- Expected: Int
```

---

### Match with nested tuple and record pattern

Tests complex nested patterns.

```bendu
let data = ((1, { tag = "A" }), 2) in
match data with
  ((x, { tag = t }), y) => t
-- Expected: String
```

---

### Match with pattern and type annotation

Tests pattern with type annotation.

```bendu
match 42 with
  x: Int => x
-- Expected: Int
```

---

### Match with multiple tuple patterns

Tests matching on different tuple structures.

```bendu
match (1, 2, 3) with
  (x, y, z) => x + y + z
| (x, y) => x + y
-- Expected: Int
```

---

### Match with record field omission

Tests that record patterns can omit fields.

```bendu
let person = { name = "Alice", age = 30, city = "NYC" } in
match person with
  { name = n, age = a } => n
-- Expected: String
```

---

### Match with complex nested patterns

Tests deeply nested pattern matching.

```bendu
let data = { 
  items = { 
    list = { 
      tag = "Cons", 
      head = { id = 1, value = "test" }, 
      tail = { tag = "Nil" } 
    } 
  } 
} in
match data with
  { items = { list = { tag = "Cons", head = { id = i, value = v }, tail = t } } } => v
| _ => "empty"
-- Expected: String
```

---

### Match with polymorphic type

Tests pattern matching on polymorphic types.

```bendu
type List[A] = { tag: "Cons", head: A, tail: List[A] } | { tag: "Nil" }
let rec length[A] = \list =>
  match list with
    { tag = "Nil" } => 0
  | { tag = "Cons", head = h, tail = t } => 1 + length(t) in
length({ tag = "Cons", head = "hello", tail = { tag = "Nil" } })
-- Expected: Int
```

---

### Match with union type patterns

Tests pattern matching on union types.

```bendu
let value: Int | String = 42 in
match value with
  x: Int => x * 2
| s: String => 0
-- Expected: Int
```

---

### Match with type refinement

Tests pattern matching with type refinement.

```bendu
let process = \value =>
  match value with
    x: Int => x * 2
  | s: String => 0 in
process(21)
-- Expected: Int
```

---

### Match with multiple wildcards

Tests multiple wildcard patterns.

```bendu
match (1, 2, 3) with
  (_, _, z) => z
| (_, y, _) => y
-- Expected: Int
```

---

### Match with literal and variable patterns

Tests mixing literal and variable patterns.

```bendu
match (1, "hello") with
  (1, s) => s
| (x, "world") => "other"
| (x, y) => y
-- Expected: String
```

---

### Match with type mismatch in pattern

Tests error when pattern type doesn't match value type.

```bendu
match 42 with
  s: String => s
-- Expected: Cannot unify Int with String
```

---

### Match with exhaustive boolean patterns

Tests exhaustive matching on boolean values.

```bendu
let flag: Bool = True in
match flag with
  True => "yes"
| False => "no"
-- Expected: String
```

---

### Match with recursive pattern matching

Tests recursive functions using pattern matching.

```bendu
type Tree[A] = { tag: "Leaf", value: A } | { tag: "Node", left: Tree[A], right: Tree[A] }
let rec sum = \tree =>
  match tree with
    { tag = "Leaf", value = v } => v
  | { tag = "Node", left = l, right = r } => sum(l) + sum(r) in
sum({ tag = "Leaf", value = 42 })
-- Expected: Int
```

---

### Match with overlapping literal patterns

Tests detection of overlapping literal patterns.

```bendu
match 42 with
  42 => "first"
| 42 => "second"
-- Expected: String
```

---

### Match with polymorphic record patterns

Tests pattern matching on polymorphic records.

```bendu
let getName[A] = \record: { name: String, ...A } =>
  match record with
    { name = n } => n in
getName({ name = "Bob", age = 25, city = "LA" })
-- Expected: String
```

---

### Match with tuple type annotation

Tests pattern matching with tuple type annotations.

```bendu
match (1, "hello") with
  pair: (Int, String) => pair
-- Expected: (Int, String)
```

---

### Match with record type annotation

Tests pattern matching with record type annotations.

```bendu
let person: { name: String, age: Int } = { name = "Alice", age = 30 } in
match person with
  p: { name: String, age: Int } => p.name
-- Expected: String
```

---

### Match with complex exhaustiveness

Tests complex exhaustiveness checking.

```bendu
type Status = { tag: "Pending" } | { tag: "Success", value: Int } | { tag: "Error", message: String }
let status: Status = { tag = "Success", value = 42 } in
match status with
  { tag = "Pending" } => "waiting"
| { tag = "Success", value = v } => "ok"
| { tag = "Error", message = m } => "failed"
-- Expected: String
```

---

### Match with non-exhaustive union

Tests non-exhaustive matching on union types.

```bendu
let value: Int | String | Bool = 42 in
match value with
  x: Int => "number"
| s: String => "string"
-- Expected: String
```

---

### Match with constructor pattern error

Tests error when matching on undefined constructor.

```bendu
let x = { tag = "Some", value = 42 } in
match x with
  { tag = "Invalid", value = v } => v
| _ => "hello"
-- Expected: Int
```

---

### Match with complex pattern combinations

Tests complex pattern combinations.

```bendu
let data = { 
  config = { 
    settings = { 
      enabled = True, 
      timeout = 5000 
    }, 
    name = "test" 
  } 
}

match data with
  { config = { settings = { enabled = True, timeout = t }, name = n } } => t
| { config = { settings = { enabled = False }, name = n } } => 0
| _ => 1000
-- Expected: Int
```

---

### Match with nested tuple patterns

Tests nested tuple pattern matching.

```bendu
let data = ((1, 2), (3, 4)) in
match data with
  ((a, b), (c, d)) => a + b + c + d
| ((x, y), _) => x + y
| _ => 0
-- Expected: Int
```

---

### Match with record field access in patterns

Tests accessing record fields within patterns.

```bendu
let person = { name = "Alice", details = { age = 30, city = "NYC" } } in
match person with
  { name = n, details = { age = a, city = c } } => n
| { name = n, details = d } => n
-- Expected: String
```

---

### Match with type parameter in patterns

Tests pattern matching with type parameters.

```bendu
type Result[A, B] = { tag: "Ok", value: A } | { tag: "Err", error: B }
let result: Result[Int, String] = { tag = "Ok", value = 42 } in
match result with
  { tag = "Ok", value = v } => v
| { tag = "Err", error = e } => 0
-- Expected: Int
```

---

### Match with complex type refinement

Tests complex type refinement in patterns.

```bendu
let process = \value =>
  match value with
    x: Int => x * 2
  | s: String => 1 in
process(5)
-- Expected: Int
```

---

### Match with record pattern and wildcard

Tests record patterns with wildcards.

```bendu
let data = { id = 1, name = "test", active = True, metadata = "extra" } in
match data with
  { id = i, name = n, active = True } => n
| { id = i, name = n, active = False } => "inactive"
| _ => "unknown"
-- Expected: String
``` 