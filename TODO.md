The following is the implementation TODO list. It is a list of the things that
need to be implemented in the Bendu language. The list is not exhaustive and
will be updated as the implementation progresses. The intent of this list is to
create a roadmap that is based on individual language features rather than the
typical compiler pipeline. This is to ensure that there is gradual progress from
lexical analysis to code generation one language feature at a time.

The following _definition of done_ is used to determine when a feature is
complete:

- The feature is implemented in the language
- The feature is documented in the language documentation
- The feature is tested in the language test suite
- Both the AST interpreter and BC interpreter are updated to support the feature
- No memory leaks for both positive and negative tests

# Big Picture

The following is the list of big features that need to be implemented.

- [X] Basic types
- [ ] Functions
- [ ] Mutable variables
- [ ] Tuples
- [ ] Arrays
- [ ] Records
- [ ] Packages
- [ ] Type aliases
- [ ] Patterns
- [ ] Signals

# Scaffolding

- [x] Have lexical errors propagate through to main
- [x] Have syntax errors propagate through to main

# Language

## assignment

- [ ] Implement mutable package variables
- [ ] Implement mutable local variables
- [ ] Implement mutable parameters variables

## if

- [x] Implement the `if` expression in the AST interpreter
- [x] Implement the `if` expression in the BC interpreter
- [x] Enforce syntactically that there MUST be at least one branch without a guard

## while

- [ ] Implement the `while` expression in the AST interpreter
- [ ] Implement the `while` expression in the BC interpreter

# Data Types

## Unit

- [x] Implement the `Unit` type
- [x] Implement the `Unit` type in the AST interpreter
- [x] Implement the `Unit` type in the BC interpreter
- [x] Integrate into main so that the value can be viewed.
- [x] Implement ==, !=, <, <=, >, >=

## Bool

- [x] Add the `Bool` type to the language
- [x] Infer `Bool` literal values
- [x] Implement the `Bool` type in the AST interpreter
- [x] Implement the `Bool` type in the BC interpreter
- [x] Integrate into main so that the value can be viewed.
- [x] Implement not
- [x] Implement &&
- [x] Implement ||
- [x] Implement ==
- [x] Implement !=

## Int

- [x] Add the `Int` type to the language
- [x] Infer `Int` literal values
- [x] Implement the `Int` type in the AST interpreter
- [x] Implement the `Int` type in the BC interpreter
- [x] Integrate into main so that the value can be viewed.
- [x] Implement +, -, *, /, %, **
- [x] Implement ==, !=, <, <=, >, >=
- [ ] Verify that the 63-bit signed is correct in when reporting literal
      underflow and overflow
- [ ] Verify that the 63-bit signed is correct in when encountering underflow
      and overflow during +, -, *, /, **, % operations
- [ ] Handle divide by zero
- [ ] Handle modulo zero

## Float

- [x] Add the `Float` type to the language
- [x] Infer `Float` literal values
- [x] Implement the `Float` type in the AST interpreter
- [x] Implement the `Float` type in the BC interpreter
- [x] Integrate into main so that the value can be viewed.
- [x] Implement +, -, *, **, /
- [x] Implement ==, !=, <, <=, >, >=
- [ ] Handle divide by zero

## Char

- [x] Add the `Char` type to the language
- [x] Infer `Char` literal values
- [x] Implement the `Char` type in the AST interpreter
- [x] Implement the `Char` type in the BC interpreter
- [x] Integrate into main so that the value can be viewed.
- [x] Implement +, -, *, **, /
- [x] Implement ==, !=, <, <=, >, >=
- [ ] Handle divide by zero

## String

- [x] Add the `String` type to the language
- [x] Infer `String` literal values
- [x] Implement the `String` type in the AST interpreter
- [x] Implement the `String` type in the BC interpreter
- [x] Integrate into main so that the value can be viewed.
- [x] Implement +
- [x] Implement ==, !=, <, <=, >, >=

## Record

- [ ] Report an error when attempt is made to reference or assign to an unknown
      field
- [ ] Incorporate mutable fields

# Operators

- [x] (+): [a: Char | Float | Int | String] a -> a -> a
- [x] (-): [a: Int | Float | Char] a -> a -> a
- [x] (*): [a: Int | Float | Char] a -> a -> a
- [x] (**): [a: Int | Float | Char] a -> a -> a
- [x] (/): [a: Int | Float | Char] a -> a -> a
- [x] (%): Int -> Int -> Int
- [x] (==): [a] a -> a -> Bool
- [x] (!=): [a] a -> a -> Bool
- [x] (<): [a: Bool | Char | Float | Int | String] a -> a -> a
- [x] (<=): [a: Bool | Char | Float | Int | String] a -> a -> a
- [x] (>): [a: Bool | Char | Float | Int | String] a -> a -> a
- [x] (>=): [a: Bool | Char | Float | Int | String] a -> a -> a
- [ ] (<<): [a] List a -> a -> List a
- [ ] (<!): [a] List a -> a -> List a
- [ ] (>>): [a] a -> List a -> List a
- [ ] (>!): [a] a -> List a -> List a
- [ ] (?): [a, b: a | Unit] b -> a -> a
- [x] (!): Bool -> Bool
- [ ] (&&): Bool -> Bool -> Bool
- [ ] (||): Bool -> Bool -> Bool

# Typing

- [ ] Report an error when attempt is made to reference unknown type variable

# Builtins

- [ ] int: [a: Bool | Int | Float | Char | String] a -> Int
