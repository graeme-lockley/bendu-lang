The following is the implementation TODO list.  It is a list of the things that need to be implemented in the Bendu language.  The list is not exhaustive and will be updated as the implementation progresses.  The intent of this list is to create a roadmap that is based on individual language features rather than the typical compiler pipeline.  This is to ensure that there is gradual progress from lexical analysis to code generation one language feature at a time.

The following *definition of done* is used to determine when a feature is complete:

- The feature is implemented in the language
- The feature is documented in the language documentation
- The feature is tested in the language test suite
- Both the AST interpreter and BC interpreter are updated to support the feature
- No memory leaks for both positive and negative tests

# Scaffolding

- [X] Have lexical errors propagate through to main
- [X] Have syntax errors propagate through to main

# Language

## assignment

- [ ] Implement mutable package variables
- [ ] Implement mutable local variables
- [ ] Implement mutable parameters variables
   
## if

- [ ] Implement the `if` expression in the AST interpreter
- [ ] Implement the `if` expression in the BC interpreter

## while

- [ ] Implement the `while` expression in the AST interpreter
- [ ] Implement the `while` expression in the BC interpreter

# Data Types

## Unit

- [X] Implement the `Unit` type
- [X] Implement the `Unit` type in the AST interpreter
- [X] Implement the `Unit` type in the BC interpreter
- [X] Integrate into main so that the value can be viewed.

## Bool

- [X] Add the `Bool` type to the language
- [X] Infer `Bool` literal values
- [X] Implement the `Bool` type in the AST interpreter
- [X] Implement the `Bool` type in the BC interpreter
- [X] Integrate into main so that the value can be viewed.
- [ ] Implement not
- [ ] Implement &&
- [ ] Implement ||
- [ ] Implement ==

## Int

- [X] Add the `Int` type to the language
- [X] Infer `Int` literal values
- [X] Implement the `Int` type in the AST interpreter
- [X] Implement the `Int` type in the BC interpreter
- [X] Integrate into main so that the value can be viewed.
- [ ] Implement +, -, *, /, %, **
- [ ] Implement ==, !=, <, <=, >, >=
- [ ] Verify that the 63-bit signed is correct in when reporting literal underflow and overflow

## Float

- [X] Add the `Float` type to the language
- [X] Infer `Float` literal values
- [X] Implement the `Float` type in the AST interpreter
- [X] Implement the `Float` type in the BC interpreter
- [X] Integrate into main so that the value can be viewed.
- [ ] Implement +, -, *, /, %
- [ ] Implement ==, !=, <, <=, >, >=

## Char

- [X] Add the `Char` type to the language
- [X] Infer `Char` literal values
- [X] Implement the `Char` type in the AST interpreter
- [X] Implement the `Char` type in the BC interpreter
- [X] Integrate into main so that the value can be viewed.
- [ ] Implement ==, !=, <, <=, >, >=

## String

- [X] Add the `String` type to the language
- [X] Infer `String` literal values
- [X] Implement the `String` type in the AST interpreter
- [X] Implement the `String` type in the BC interpreter
- [X] Integrate into main so that the value can be viewed.
- [ ] Implement +, *
- [ ] Implement ==, !=

## Record

- [ ] Report an error when attempt is made to reference or assign to an unknown field
- [ ] Incorporate mutable fields

# Operators

- [ ] (+): [a: Int | Float | Char] a -> a -> a
- [ ] (-): [a: Int | Float | Char] a -> a -> a
- [ ] (*): [a: Int | Float | Char] a -> a -> a
- [ ] (**): Int -> Int -> Int
- [ ] (/): [a: Int | Float | Char] a -> a -> a
- [ ] (%): Int -> Int -> Int
- [ ] (==): [a] a -> a -> Bool
- [ ] (!=): [a] a -> a -> Bool
- [ ] (<): [a: Int | Float | Char | String] a -> a -> a
- [ ] (<=): [a: Int | Float | Char | String] a -> a -> a
- [ ] (>): [a: Int | Float | Char | String] a -> a -> a
- [ ] (>=): [a: Int | Float | Char | String] a -> a -> a
- [ ] (<<): [a] List a -> a -> List a
- [ ] (<!): [a] List a -> a -> List a
- [ ] (>>): [a] a -> List a -> List a
- [ ] (>!): [a] a -> List a -> List a
- [ ] (?): [a, b: a | Unit] b -> a -> a
- [X] (!): Bool -> Bool
- [ ] (&&): Bool -> Bool -> Bool
- [ ] (||): Bool -> Bool -> Bool

# Typing

- [ ] Report an error when attempt is made to reference unknown type variable

# Builtins

- [ ] int: [a: Bool | Int | Float | Char | String] a -> Int
