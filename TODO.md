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
- [x] Packages
- [x] Garbage collector
- [x] Custom Data Types
- [ ] Records
- [ ] Type aliases
- [x] Patterns
- [ ] Signals
- [ ] Builtins
- [ ] Integrate libuv

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

## match Expression

- [x] Incorporate the match expression into the parser and AST
- [x] Incorporate the match expression into type inference
- [x] Compile match expression

## Packages

- [x] High-level design
- [x] Implement package into runtime:
      - [x] Package table
      - [x] Load package
      - [x] Store package
      - [x] Push package closure
      - [x] Call package
- [x] Implement package into compiler:
      - [x] Bendu cache
      - [x] Write out script into new binary format
      - [x] Have interpreter read in new binary format
      - [x] Parse import
      - [x] Record and use script dependencies
      - [x] Improve compiler info to show when each file is being compiled in the same format as the bendu script
      - [x] Add nested errors for import errors
      - [x] Implement import
      - [x] Parse import as
      - [x] Implement import as
      - [x] Parse import exposing
      - [x] Implement import exposing
      - [x] Implement public qualifiers on let declarations
      
## Sequence Blocks

- [x] Implement the sequence block

## Type Qualifiers

- [x] Implement the type qualifier
- [ ] Implement type qualifier with import as

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

## Custom Data Types

- [x] Parse Custom Data Type
- [x] Incorporate Custom Data Type into type inference
- [x] Compile Custom Data Type
- [x] Recursive Custom Data Types
      - [x] Parse recursive Custom Data Type
      - [x] Incorporate recursive Custom Data Type into type inference
      - [x] Compile recursive Custom Data Types
- [x] Make an Custom Data Type exportable
      - [x] Enhance cache grammar to incorporate Custom Data Types
      - [x] Enhance lang grammar to allow Custom Data Types to be exported
      - [x] Incorporate Custom Data Types into the sig file
      - [x] Include Custom Data Types in import all
      - [x] Include Custom Data Types in import as
      - [x] Include Custom Data Types in import exposing

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
- [x] (==): [a] a -> a -> Bool
- [x] (!=): [a] a -> a -> Bool
- [x] (<): [a: Bool | Char | Float | Int | String] a -> a -> a
- [x] (<=): [a: Bool | Char | Float | Int | String] a -> a -> a
- [x] (>): [a: Bool | Char | Float | Int | String] a -> a -> a
- [x] (>=): [a: Bool | Char | Float | Int | String] a -> a -> a
- [x] (<<): [a] Array a -> a -> Array a
- [x] (<!): [a] Array a -> a -> Array a
- [x] (>>): [a] a -> Array a -> Array a
- [x] (>!): [a] a -> Array a -> Array a
- [ ] (?): [a, b: a | Unit] b -> a -> a
- [x] (!): Bool -> Bool
- [x] (&&): Bool -> Bool -> Bool
- [x] (||): Bool -> Bool -> Bool

# Typing

- [x] Report an error when attempt is made to reference unknown type variable

# Builtins

- [ ] int: [a: Bool | Int | Float | Char | String] a -> Int
