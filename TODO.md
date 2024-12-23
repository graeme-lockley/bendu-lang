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
- The feature is compiled and executed in the interpreter
- No memory leaks for both positive and negative tests

# Big Picture

The following is the list of big features that need to be implemented.

- [x] Basic types
- [x] Functions
- [x] Type annotation
- [x] Mutable variables
- [x] Control flow
- [x] Tuples
- [x] Arrays
- [ ] Packages
- [ ] Garbage collector
- [ ] ADTs
- [ ] Records
- [ ] Type aliases
- [ ] Patterns
- [ ] Signals

# Scaffolding

- [x] Pretty print errors during compilation
- [x] Generate the Ops for the compiler and interpreter from the JSON description
- [x] Produce more accurate errors for binary operator permissible types
- [ ] Produce accurate runtime errors by including the line number in the error as
      well as the file name
- [ ] Add a utility that will run tests and include the type signature in the result
- [x] Pretty print type variables

# Language

## assignment

- [x] Add qualifier into grammar
- [x] Implement mutable package variables
- [x] Implement mutable local variables
- [x] Implement mutable parameters variables

## if

- [x] Implement the `if` expression in the BC interpreter

## Packages

- [x] High-level design
- [ ] Implement package into runtime:
      - [x] Package table
      - [x] Load package
      - [x] Store package
      - [x] Push package closure
      - [x] Call package
- [ ] Implement package into compiler:
      - [x] Bendu cache
      - [x] Write out script into new binary format
      - [x] Have interpreter read in new binary format
      - [x] Parse import
      - [ ] Add nested errors for import errors
      - [ ] Enhance binding locations to include package details
      - [x] Implement import
      - [ ] Parse import as
      - [ ] Implement import as
      - [ ] Parse import exposing
      - [ ] Implement import exposing
      - [ ] Implement public qualifiers
      
## Sequence Blocks

- [x] Implement the sequence block

## while

- [x] Implement the `while` expression in the BC interpreter

# Data Types

## Arrays

- [x] Parse literal
- [x] Parse type
- [x] Infer array type
- [x] Compile literal
- [x] Incorporate ... into a literal array
- [x] Print tuple value
- [x] Implement == and !=
- [x] Project element using ! #
- [x] Project range using ! #:#
- [x] Project range using !:#
- [x] Project range using !#:
- [x] Project range using !#
- [x] Assignment using ! #
- [x] Assign to range using ! #:#
- [x] Assign to range using ! :#
- [x] Assign to range using ! #:
- [x] Assign to range using ! :
- [x] Append using >>
- [x] Append mutation using >!
- [x] Prepend using <<
- [x] Prepend mutation using <!

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
- [x] Implement the `Char` type in the BC interpreter
- [x] Implement +, -, *, **, /
- [x] Implement ==, !=, <, <=, >, >=
- [x] Handle divide by zero
- [ ] Add support for \x escape sequence

## Float

- [x] Add the `Float` type to the language
- [x] Infer `Float` literal values
- [x] Implement the `Float` type in the BC interpreter
- [x] Implement +, -, *, **, /
- [x] Implement ==, !=, <, <=, >, >=
- [ ] Handle divide by zero

## Functions

- [x] In package, non-higher order, private function without closure declaration
- [x] Invoke in package, non-higher order, private function without closure declaration
- [x] Invoke in package, *higher order*, private function with closure declaration
- [ ] Implement |> operator
- [ ] Implement <| operator
- [ ] Implement default argument values
- [ ] Implement "..." on arguments

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
- [x] Implement the `String` type in the BC interpreter
- [x] Implement +
- [x] Implement ==, !=, <, <=, >, >=

# Tuple

- [x] Parse literal
- [x] Parse type
- [x] Compile literal
- [x] Print tuple value
- [x] Implement == and !=
- [x] Destructive parameter

## Unit

- [x] Implement the `Unit` type
- [x] Implement the `Unit` type in the BC interpreter
- [x] Implement ==, !=, <, <=, >, >=

# Operators

- [x] (+): [a: Char | Float | Int | String] a -> a -> a
- [x] (-): [a: Int | Float | Char] a -> a -> a
- [x] (*): [a: Int | Float | Char] a -> a -> a
- [x] (**): [a: Int | Float | Char] a -> a -> a
- [x] (/): [a: Int | Float | Char] a -> a -> a
- [x] (%): Int -> Int -> Int
- [ ] (==): [a] a -> a -> Bool
- [ ] (!=): [a] a -> a -> Bool
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
- [x] (&&): Bool -> Bool -> Bool
- [x] (||): Bool -> Bool -> Bool

# Typing

- [ ] Report an error when attempt is made to reference unknown type variable

# Builtins

- [ ] int: [a: Bool | Int | Float | Char | String] a -> Int
