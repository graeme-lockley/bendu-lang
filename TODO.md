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

- [ ] Basic types
- [ ] Control flow
- [ ] Functions
- [ ] Type annotation
- [ ] Mutable variables
- [ ] Tuples
- [ ] Arrays
- [ ] Records
- [ ] Packages
- [ ] Type aliases
- [ ] Patterns
- [ ] Signals

# Scaffolding

- [ ] Pretty print errors during compilation
- [ ] Generate the Ops for the compiler and interpreter from the JSON description.

# Language

## assignment

- [ ] Implement mutable package variables
- [ ] Implement mutable local variables
- [ ] Implement mutable parameters variables

## if

- [ ] Implement the `if` expression in the BC interpreter
- [ ] Enforce syntactically that there MUST be at least one branch without a
      guard

## while

- [ ] Implement the `while` expression in the BC interpreter

# Data Types

## Bool

- [x] Add the `Bool` type to the language
- [x] Infer `Bool` literal values
- [x] Implement the `Bool` type in the BC interpreter
- [x] Implement not
- [x] Implement &&
- [x] Implement ||
- [x] Implement ==
- [x] Implement !=

## Char

- [x] Add the `Char` type to the language
- [x] Infer `Char` literal values
- [ ] Implement the `Char` type in the BC interpreter
- [ ] Implement +, -, *, **, /
- [ ] Implement ==, !=, <, <=, >, >=
- [ ] Handle divide by zero
- [ ] Add support for \x escape sequence

## Float

- [x] Add the `Float` type to the language
- [x] Infer `Float` literal values
- [ ] Implement the `Float` type in the BC interpreter
- [ ] Implement +, -, *, **, /
- [ ] Implement ==, !=, <, <=, >, >=
- [ ] Handle divide by zero

## Functions

- [ ] In package, non-recursive, non-higher order, private function without
      closure
- [ ] In package, singular recursive, non-higher order, private function without
      closure
- [ ] Change the parser to allow multiple ID declarations separated with `and`
- [ ] Extend type inference to across mutually recursive functions
- [ ] Embed the result of mutually recursive type inference into the test runner
- [ ] AST execute in package, mutually recursive, non-higher order, private
      function without closure
- [ ] Bytecode compile and execute in package, mutually recursive, non-higher
      order, private function without closure

## Int

- [x] Add the `Int` type to the language
- [x] Infer `Int` literal values
- [x] Implement the `Int` type in the BC interpreter
- [x] Implement +, -, *, /, %, **
- [x] Implement ==, !=, <, <=, >, >=
- [x] Verify that the 32-bit signed is correct in when reporting literal
      underflow and overflow
- [x] Gracefully handle divide by zero
- [x] Gracefully handle modulo zero

## Record

- [ ] Report an error when attempt is made to reference or assign to an unknown
      field
- [ ] Incorporate mutable fields

## String

- [x] Add the `String` type to the language
- [x] Infer `String` literal values
- [ ] Implement the `String` type in the BC interpreter
- [ ] Implement +
- [ ] Implement ==, !=, <, <=, >, >=

## Unit

- [ ] Implement the `Unit` type
- [ ] Implement the `Unit` type in the BC interpreter
- [ ] Integrate into main so that the value can be viewed.
- [ ] Implement ==, !=, <, <=, >, >=

# Operators

- [ ] (+): [a: Char | Float | Int | String] a -> a -> a
- [ ] (-): [a: Int | Float | Char] a -> a -> a
- [ ] (*): [a: Int | Float | Char] a -> a -> a
- [ ] (**): [a: Int | Float | Char] a -> a -> a
- [ ] (/): [a: Int | Float | Char] a -> a -> a
- [ ] (%): Int -> Int -> Int
- [ ] (==): [a] a -> a -> Bool
- [ ] (!=): [a] a -> a -> Bool
- [ ] (<): [a: Bool | Char | Float | Int | String] a -> a -> a
- [ ] (<=): [a: Bool | Char | Float | Int | String] a -> a -> a
- [ ] (>): [a: Bool | Char | Float | Int | String] a -> a -> a
- [ ] (>=): [a: Bool | Char | Float | Int | String] a -> a -> a
- [ ] (<<): [a] List a -> a -> List a
- [ ] (<!): [a] List a -> a -> List a
- [ ] (>>): [a] a -> List a -> List a
- [ ] (>!): [a] a -> List a -> List a
- [ ] (?): [a, b: a | Unit] b -> a -> a
- [ ] (!): Bool -> Bool
- [ ] (&&): Bool -> Bool -> Bool
- [ ] (||): Bool -> Bool -> Bool

# Typing

- [ ] Report an error when attempt is made to reference unknown type variable

# Builtins

- [ ] int: [a: Bool | Int | Float | Char | String] a -> Int
