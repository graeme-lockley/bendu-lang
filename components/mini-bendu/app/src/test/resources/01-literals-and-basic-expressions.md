# Literals and Basic Expressions

This chapter explores literal values and basic expression evaluation in Mini-Bendu, including type inference for primitive values, unit values, and literal expressions.

---

### Integer literal

Tests that integer literals are correctly typed as Int.

```bendu
42
-- Expected: Int
```

---

### Negative integer literal

Tests that negative integer literals are correctly typed as Int.

```bendu
-42
-- Expected: Int
```

---

### Zero integer literal

Tests that zero is correctly typed as Int.

```bendu
0
-- Expected: Int
```

---

### Large integer literal

Tests that large integer values are correctly typed as Int.

```bendu
1000000
-- Expected: Int
```

---

### String literal

Tests that string literals are correctly typed as String.

```bendu
"hello"
-- Expected: String
```

---

### Empty string literal

Tests that empty strings are correctly typed as String.

```bendu
""
-- Expected: String
```

---

### String with spaces

Tests that strings containing spaces are correctly typed as String.

```bendu
"hello world"
-- Expected: String
```

---

### String with special characters

Tests that strings with special characters are correctly typed as String.

```bendu
"hello\nworld"
-- Expected: String
```

---

### Boolean literal true

Tests that True literal is correctly typed as Bool.

```bendu
True
-- Expected: Bool
```

---

### Boolean literal false

Tests that False literal is correctly typed as Bool.

```bendu
False
-- Expected: Bool
```

---

### Mixed arithmetic expression

Tests type inference for arithmetic expressions with literals.

```bendu
1 + 2 * 3
-- Expected: Int
```

---

### Arithmetic subtraction

Tests type inference for subtraction operations.

```bendu
10 - 5
-- Expected: Int
```

---

### Boolean expression with literals

Tests type inference for boolean operations.

```bendu
True && False
-- Expected: Bool
```

---

### Boolean or expression

Tests type inference for boolean OR operations.

```bendu
True || False
-- Expected: Bool
```

---

### Comparison with literals

Tests type inference for comparison operations.

```bendu
42 == 42
-- Expected: Bool
```

---

### String comparison

Tests type inference for string comparison.

```bendu
"hello" == "world"
-- Expected: Bool
```

---

### Not equal comparison

Tests type inference for inequality comparison.

```bendu
42 != 17
-- Expected: Bool
```

---

### Mixed comparison types

Tests error handling when comparing different types.

```bendu
42 == "hello"
-- Expected: Cannot unify Int with String
```

---

### String concatenation with literals

Tests that string concatenation works with the + operator.

```bendu
"hello" + " world"
-- Expected: String
```

---

### String concatenation chaining

Tests that multiple string concatenations work correctly.

```bendu
"hello" + " " + "world"
-- Expected: String
```

---

### Empty string concatenation

Tests concatenation with empty strings.

```bendu
"hello" + ""
-- Expected: String
```

---

### String concatenation with variables

Tests string concatenation with bound variables.

```bendu
let greeting = "hello"
let target = "world"
greeting + " " + target
-- Expected: String
```

---

### Mixed type addition error

Tests that adding different types still produces an error.

```bendu
"hello" + 42
-- Expected: Cannot unify String with Int
```

---

### Nested arithmetic

Tests type inference for nested arithmetic expressions.

```bendu
(1 + 2) * (3 + 4)
-- Expected: Int
```

---

### Boolean logic with comparison

Tests complex boolean expressions with comparisons.

```bendu
(1 + 1 == 2) && (3 * 2 == 6)
-- Expected: Bool
```

---

### Division operation

Tests type inference for division operations.

```bendu
10 / 2
-- Expected: Int
```

---

### Literal in variable binding

Tests type inference when literals are bound to variables.

```bendu
let x = 42
x
-- Expected: Int
```

---

### Multiple variable bindings with literals

Tests type inference across multiple variable bindings.

```bendu
let x = 42
let y = "hello"
let z = True
x
-- Expected: Int
```

---

### Literal type annotation consistency

Tests that explicit type annotations match inferred types.

```bendu
let x: Int = 42
x
-- Expected: Int
```

---

### String literal type annotation

Tests that string type annotations work correctly.

```bendu
let greeting: String = "hello"
greeting
-- Expected: String
```

---

### Boolean literal type annotation

Tests that boolean type annotations work correctly.

```bendu
let flag: Bool = True
flag
-- Expected: Bool
```

---

### Type annotation mismatch

Tests error handling when type annotations don't match literals.

```bendu
let x: String = 42
x
-- Expected: Cannot unify Int with String
```

---

### Parenthesized literals

Tests that parentheses don't affect literal typing.

```bendu
(42)
-- Expected: Int
```

---

### Parenthesized string

Tests that parentheses don't affect string literal typing.

```bendu
("hello")
-- Expected: String
```

---

### Complex parenthesized expression

Tests type inference with complex parenthesized expressions.

```bendu
((1 + 2) * 3)
-- Expected: Int
``` 