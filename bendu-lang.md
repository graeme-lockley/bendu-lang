# Bendu Language Definition

## Abstract

This document presents a formal definition of Bendu, a statically typed functional programming language designed for expressiveness, safety, and performance. Bendu combines the rigor of Hindley-Milner type inference with modern language features including algebraic data types, pattern matching, and row polymorphism. The definition covers the language's theoretical foundations, syntactic structure, static semantics, and standard library architecture, providing a comprehensive reference for both language implementers and users.

## 1. Introduction and Design Philosophy

Bendu is a statically typed programming language that synthesizes multiple programming paradigms with an emphasis on functional programming. The language is designed to be simple, expressive, and efficient, with a focus on developer productivity and code reliability.

### 1.1 Design Principles

The core principles that guide Bendu's design are:

1. **Static Safety**: A strong, static type system that catches errors at compile time
2. **Type Inference**: Minimizing explicit type annotations through Hindley-Milner type inference
3. **Expressiveness**: Supporting high-level abstractions without sacrificing readability
4. **Performance**: Efficient execution through bytecode compilation and interpretation
5. **Interoperability**: Support for interaction with existing libraries and systems

### 1.2 Influences and Theoretical Foundations

Bendu draws inspiration from several languages and type systems:

- **ML Family**: The core type system and inference mechanism derive from Standard ML and OCaml (Milner et al., 1997)
- **Haskell**: Algebraic data types and pattern matching (Hudak et al., 2007)
- **Elm**: Record system with row polymorphism (Czaplicki, 2012)
- **OCaml**: Module system for code organization
- **Scheme/Lisp**: First-class functions and closures (Sussman & Steele, 1975)

The language adopts a functional-first approach, emphasizing immutable data structures and referential transparency, while still allowing imperative constructs where they enhance expressiveness or performance.

## 2. Syntax

Bendu's syntax aims for clarity and conciseness, with a focus on readability. This section defines the grammatical structure of the language.

### 2.1 Lexical Structure

#### 2.1.1 Comments

Bendu supports three forms of comments:

```
// Single line comment
# Alternative single line comment
/* Block comment that
   can span multiple lines */
```

Block comments can be nested, allowing for selective comment exclusion.

#### 2.1.2 Identifiers

Identifiers in Bendu follow these rules:

- Variable identifiers start with a lowercase letter (`lowerID`)
- Type identifiers start with an uppercase letter (`UpperID`)
- Both can contain letters, digits, and underscores

```
lowerID = 'a'-'z' {'A'-'Z' | 'a'-'z' | '0'-'9' | '_'}
UpperID = 'A'-'Z' {'A'-'Z' | 'a'-'z' | '0'-'9' | '_'}
```

#### 2.1.3 Literals

Bendu supports the following literal forms:

```
LiteralInt = ['-'] digits
LiteralFloat = ['-'] digits '.' digits [('e' | 'E') ['-'] digits]
LiteralChar = "'" (char | '"' | '\' ('\' | 'n' | "'")) "'"
LiteralString = '"' {char | "'" | '\' ('\' | 'n' | '"' | ("x" hexDigits ';'))} '"'
```

Where:
- `digits` represents one or more decimal digits
- `char` represents any character except control characters
- `hexDigits` represents one or more hexadecimal digits

### 2.2 Grammar

The following grammar is presented in an extended Backus-Naur Form (EBNF), where:
- `{X}` means zero or more occurrences of X
- `[X]` means X is optional
- `X | Y` means X or Y

#### 2.2.1 Programs

```
Program
    : {ImportStatement [";"]} {Declaration [";"]}
    ;

ImportStatement
    : "import" LiteralString ["as" UpperID] ["exposing" "(" ImportDeclaration {"," ImportDeclaration} ")"]
    ;

ImportDeclaration
    : LowerID ["as" LowerID]
    | UpperID ["as" UpperID]
    ;

Declaration
    : "type" TypeDeclaration {"and" TypeDeclaration}
    | Expression
    ;
```

#### 2.2.2 Types and Type Declarations

```
TypeDeclaration
    : UpperID ["*" | "-"] [TypeParameters] "=" TypeConstructor {"|" TypeConstructor}
    ;

TypeConstructor
    : UpperID ["[" TypeTerm {"," TypeTerm} "]"]
    ;

TypeParameters
    : "[" LowerID {"," LowerID} "]"
    ;

TypeQualifier
    : ":" TypeTerm
    ;

TypeTerm
    : TypeFactor {"->" TypeFactor}
    ;

TypeFactor
    : LowerID
    | UpperID ["[" TypeTerm {"," TypeTerm} "]"]
    | "(" TypeTerm {"," TypeTerm} ")"
    | "[" TypeTerm "]"
    | "{" [LowerID ":" TypeTerm {"," LowerID ":" TypeTerm}] ["," LowerID] "}"
    ;
```

#### 2.2.3 Expressions

```
Expression
    : "let" LetDeclaration {"and" LetDeclaration}
    | "print" "(" [Expression {"," Expression}] ")"
    | "println" "(" [Expression {"," Expression}] ")"
    | "abort" "(" [Expression {"," Expression}] ")"
    | "if" ["|"] Expression "->" Expression {"|" Expression ["->" Expression]}
    | "while" Expression "->" Expression
    | "match" Expression "with" "|" Pattern "->" Expression {"|" Pattern "->" Expression}
    | OrExpression
    ;

LetDeclaration
    : LowerID ["!"] ["*"] [TypeParameters] [FunctionParameters] [TypeQualifier] "=" Expression
    ;

FunctionParameters
    : "(" [FunctionParameter {"," FunctionParameter}] ")"
    ;

FunctionParameter
    : LowerID [TypeQualifier]
    ;
```

#### 2.2.4 Pattern Matching

```
Pattern
    : PatternFactor ["," Expression]
    | "_" ["@" LowerID]
    | LowerID [":" TypeTerm]
    | UpperID ["." (LowerID | UpperID)] ["(" [Pattern {"," Pattern}] ")"]
    | LiteralChar
    | LiteralFloat
    | LiteralInt
    | LiteralString
    | "True"
    | "False"
    | "(" [Pattern {"," Pattern}] ")"
    | "[" [Pattern {"," Pattern}] "]"
    | "{" [LowerID ":" Pattern {"," LowerID ":" Pattern}] ["," "..."] "}"
    ;

PatternFactor
    : LowerID [":" TypeTerm]
    | "_"
    ;
```

#### 2.2.5 Operators and Expressions

```
OrExpression
    : AndE {"||" AndE}
    ;

AndE
    : Equality {"&&" Equality}
    ;

Equality
    : Starpend [RelOp Starpend]
    ;

RelOp
    : "=="
    | "!="
    | "<"
    | "<="
    | ">"
    | ">="
    ;

Starpend
    : Additive {StarpendOp Additive}
    ;

StarpendOp
    : ">>"
    | ">!"
    | "<<"
    | "<!"
    ;

Additive
    : Multiplicative {("+" | "-") Multiplicative}
    ;

Multiplicative
    : Power {("*" | "/" | "%") Power}
    ;

Power
    : TypedExpression {"**" TypedExpression}
    ;

TypedExpression
    : Assignment [":" TypeTerm]
    ;

Assignment
    : QualifiedExpression [":=" Expression]
    ;

QualifiedExpression
    : Factor {QualifiedExpressionSuffix}
    ;

QualifiedExpressionSuffix
    : "(" [Expression {"," Expression}] ")"
    | "." LowerID
    | "!" (QualifiedExpression [":" [QualifiedExpression]] | ":" [QualifiedExpression])
    ;

Factor
    : "(" [Expression {"," Expression}] ")"
    | LowerID
    | UpperID ["." (LowerID | UpperID)]
    | LiteralChar
    | LiteralFloat
    | LiteralInt
    | LiteralString
    | "True"
    | "False"
    | "[" [Expression {"," Expression}] "]"
    | "{" [LowerID ":" Expression {"," LowerID ":" Expression}] ["," "..." Expression] "}"
    | "{" Expression "|" LowerID ":" Expression {"," LowerID ":" Expression} "}"
    | "fn" "(" [FunctionParameter {"," FunctionParameter}] ")" [TypeQualifier] "=" Expression
    ;
```

### 2.3 Syntactic Sugar and Notational Conventions

Bendu provides several syntactic conveniences:

#### 2.3.1 Function Application

Function application is denoted by juxtaposition with parentheses:
```
function(arg1, arg2, ...)
```

#### 2.3.2 Type Annotations

Type annotations use the colon notation:
```
expression : type
```

#### 2.3.3 Anonymous Functions

Anonymous functions use the `fn` keyword:
```
fn(x, y) = x + y
```

#### 2.3.4 Record Update

The syntax for record update is:
```
{record | field1: value1, field2: value2}
```

#### 2.3.5 Record Spread

The syntax for record spread is:
```
{...record, field: value}
```

## 3. Type System

Bendu employs a Hindley-Milner type system with extensions for algebraic data types, row polymorphism, and parametric polymorphism. The type system ensures strong static guarantees about program correctness.

### 3.1 Basic Types

The language provides the following elementary types:

- `Bool`: Boolean values (`True` or `False`)
- `Char`: Unicode character values
- `Float`: IEEE 754 floating-point numbers
- `Int`: Signed integer values
- `String`: UTF-8 encoded string values
- `Unit`: The unit type, represented by `()`

### 3.2 Composite Types

Bendu supports several kinds of composite types:

#### 3.2.1 Function Types

Function types are denoted as `τ₁ -> τ₂`, where `τ₁` is the input type and `τ₂` is the output type.

#### 3.2.2 Array Types

Arrays are homogeneous collections denoted as `[τ]`, where `τ` is the type of elements.

#### 3.2.3 Tuple Types

Tuples are heterogeneous fixed-size collections denoted as `(τ₁, τ₂, ..., τₙ)`.

#### 3.2.4 Record Types

Records are labeled collections with the syntax `{l₁: τ₁, l₂: τ₂, ..., lₙ: τₙ}`, where `lᵢ` are field labels and `τᵢ` are types.

#### 3.2.5 Algebraic Data Types (ADTs)

Custom algebraic data types are defined with the `type` keyword:
```
type Option[a] = None | Some[a]
```

### 3.3 Type Variables and Polymorphism

Bendu supports parametric polymorphism through type variables, denoted by lowercase identifiers:

```
[a, b, c]
```

These variables can be used in type signatures to indicate that a function can operate on values of different types.

### 3.4 Row Polymorphism

The type system supports row polymorphism for records, allowing functions to accept records with at least certain fields:

```
{name: String, age: Int | ρ}
```

Where `ρ` represents any additional fields. This is often written in type annotations as:

```
{name: String, age: Int, a}
```

Where `a` is a row type variable.

### 3.5 Type Rules

The type system is defined by a set of typing judgments and inference rules. The main judgment `Γ ⊢ e : τ` means "in typing environment Γ, expression e has type τ."

#### 3.5.1 Literals

```
Γ ⊢ True : Bool
Γ ⊢ False : Bool
Γ ⊢ n : Int   (where n is an integer literal)
Γ ⊢ f : Float  (where f is a float literal)
Γ ⊢ c : Char   (where c is a character literal)
Γ ⊢ s : String (where s is a string literal)
```

#### 3.5.2 Variables and Let Bindings

```
x : τ ∈ Γ
─────────── (T-Var)
Γ ⊢ x : τ

Γ ⊢ e₁ : τ₁    Γ, x : τ₁ ⊢ e₂ : τ₂
───────────────────────────────── (T-Let)
Γ ⊢ let x = e₁ in e₂ : τ₂
```

#### 3.5.3 Functions

```
Γ, x : τ₁ ⊢ e : τ₂
────────────────────────────── (T-Abs)
Γ ⊢ fn(x) = e : τ₁ -> τ₂

Γ ⊢ e₁ : τ₁ -> τ₂    Γ ⊢ e₂ : τ₁
───────────────────────────── (T-App)
Γ ⊢ e₁(e₂) : τ₂
```

#### 3.5.4 Arrays

```
Γ ⊢ e₁ : τ    Γ ⊢ e₂ : τ    ...    Γ ⊢ eₙ : τ
────────────────────────────────────────── (T-Array)
Γ ⊢ [e₁, e₂, ..., eₙ] : [τ]

Γ ⊢ e : [τ]    Γ ⊢ i : Int
─────────────────────────── (T-Array-Index)
Γ ⊢ e!i : τ
```

#### 3.5.5 Tuples

```
Γ ⊢ e₁ : τ₁    Γ ⊢ e₂ : τ₂    ...    Γ ⊢ eₙ : τₙ
───────────────────────────────────────────── (T-Tuple)
Γ ⊢ (e₁, e₂, ..., eₙ) : (τ₁, τ₂, ..., τₙ)
```

#### 3.5.6 Records

```
Γ ⊢ e₁ : τ₁    Γ ⊢ e₂ : τ₂    ...    Γ ⊢ eₙ : τₙ
───────────────────────────────────────────────────── (T-Record)
Γ ⊢ {l₁: e₁, l₂: e₂, ..., lₙ: eₙ} : {l₁: τ₁, l₂: τ₂, ..., lₙ: τₙ}

Γ ⊢ e : {l₁: τ₁, l₂: τ₂, ..., lᵢ: τᵢ, ..., lₙ: τₙ}
────────────────────────────────────────────── (T-Record-Proj)
Γ ⊢ e.lᵢ : τᵢ
```

#### 3.5.7 Algebraic Data Types

```
type T[α₁, α₂, ...] = ... | C[τ₁, τ₂, ...] | ...    Γ ⊢ e₁ : τ₁[τ'₁/α₁, ...]    Γ ⊢ e₂ : τ₂[τ'₁/α₁, ...]    ...
────────────────────────────────────────────────────────────────────────────────────────────────────────── (T-Constructor)
Γ ⊢ C(e₁, e₂, ...) : T[τ'₁, τ'₂, ...]
```

#### 3.5.8 Pattern Matching

```
Γ ⊢ e : τ₀    Γ ⊢ p₁ : τ₀ ⇒ Γ₁    Γ, Γ₁ ⊢ e₁ : τ    ...    Γ ⊢ pₙ : τ₀ ⇒ Γₙ    Γ, Γₙ ⊢ eₙ : τ
──────────────────────────────────────────────────────────────────────────────────────────── (T-Match)
Γ ⊢ match e with | p₁ -> e₁ | ... | pₙ -> eₙ : τ
```

#### 3.5.9 Generalization and Instantiation

```
Γ ⊢ e : τ    α ∉ FV(Γ)
─────────────────────── (T-Gen)
Γ ⊢ e : ∀α. τ

Γ ⊢ e : ∀α. τ
────────────── (T-Inst)
Γ ⊢ e : τ[τ'/α]
```

### 3.6 Type Inference

Bendu employs Algorithm W (Damas-Milner algorithm) for type inference (Damas & Milner, 1982). The algorithm uses unification to infer the most general type of an expression without requiring explicit type annotations.

For row polymorphism, the algorithm is extended with row variables and constraints on record fields (Wand, 1987; Rémy, 1989).

### 3.7 Abstract and Opaque Types

Bendu supports two forms of information hiding for algebraic data types:

1. **Abstract Types** (marked with `*`): The implementation details are hidden, but constructors are accessible
2. **Opaque Types** (marked with `-`): The constructors are not accessible outside the defining module

```
type List*[a] = Nil | Cons[a, List[a]]
type Set-[a] = Empty | Node[a, Set[a], Set[a]]
```

## 4. Runtime Semantics

Bendu is implemented as a bytecode-compiled language with an efficient interpreter. This section outlines the operational semantics and execution model.

### 4.1 Execution Model

Bendu uses a stack-based virtual machine with a bytecode interpreter. The execution model consists of:

1. **Compilation**: Source code is parsed and compiled to bytecode
2. **Interpretation**: The bytecode is executed by the interpreter
3. **Memory Management**: Values are managed with reference counting for deterministic cleanup

### 4.2 Evaluation Strategy

Bendu employs strict (eager) evaluation semantics. Expressions are evaluated from left to right, and function arguments are evaluated before function application.

### 4.3 Pattern Matching

Pattern matching is compiled into a decision tree that optimizes the number of tests performed. The pattern matching algorithm uses a variant of the matrix-based approach described by Maranget (2008).

### 4.4 Memory Management

The runtime uses reference counting for memory management, with optimizations for common cases:

1. Local variables that do not escape their scope are stack-allocated
2. Small primitive values (booleans, small integers) are unboxed when possible
3. String interning is used to reduce memory usage for repeated strings

## 5. Standard Library

Bendu provides a standard library organized into modules, covering common programming tasks.

### 5.1 Library Structure

The standard library is organized as follows:

```
lib/
  Data/           # Core data structures
    Option.bendu  # Option type and operations
    String.bendu  # String operations
  Math/           # Mathematical operations
  IO/             # Input/output operations
  System/         # System interaction
```

### 5.2 Core Libraries

#### 5.2.1 Option Module

The `Option` module provides the Maybe monad for handling potentially missing values:

```bendu
type Option[a] = None | Some[a]

let map(f, option) =
  match option with
  | None() -> None()
  | Some(value) -> Some(f(value))

let withDefault(defaultValue, option) =
  match option with
  | None() -> defaultValue
  | Some(value) -> value
```

#### 5.2.2 String Module

The `String` module provides string manipulation functions:

```bendu
let length(s) = /* native implementation */
let concat(s1, s2) = /* native implementation */
let substring(s, start, length) = /* native implementation */
```

### 5.3 Native Functions

Bendu provides a set of native functions for operations that require system interaction or efficiency:

1. IO operations (`print`, `println`, `readLine`)
2. Mathematical operations (`sqrt`, `sin`, `cos`)
3. Type reflection (`@` operator to get type information)
4. System operations (`exit`, `getEnv`)

### 5.4 Extension Mechanism

Bendu supports extending the language through a foreign function interface (FFI) that allows interaction with code written in other languages, particularly C for system interaction.

## 6. Implementation Considerations

This section discusses specific aspects of the Bendu implementation.

### 6.1 Compiler Architecture

The Bendu compiler is implemented in Kotlin and follows a traditional compiler architecture:

1. Lexical analysis
2. Parsing
3. Type checking and inference
4. Optimization
5. Code generation

### 6.2 Bytecode Interpreter

The bytecode interpreter is implemented in Zig for efficiency and control over memory layout. It provides:

1. A stack-based execution model
2. Efficient representation of values
3. Optimized operations for common patterns

### 6.3 Performance Optimizations

Several optimizations are employed in the implementation:

1. Tail call optimization for recursive functions
2. Specialized bytecode instructions for common operations
3. Inline caching for method dispatch
4. Unboxing of primitive values where possible

## 7. Conclusion

Bendu combines the safety of a strong static type system with the expressiveness of functional programming paradigms. Its Hindley-Milner type system with extensions for row polymorphism and algebraic data types enables writing concise, correct code while maintaining excellent performance characteristics.

The language's design philosophy emphasizes simplicity, expressiveness, and efficiency, making it suitable for a wide range of applications from scripting to systems programming. The combination of compilation to efficient bytecode and a carefully designed standard library provides a solid foundation for building robust software.

## References

Czaplicki, E. (2012). Elm: Concurrent FRP for Functional GUIs. Harvard University.

Damas, L., & Milner, R. (1982). Principal type-schemes for functional programs. In Proceedings of the 9th ACM SIGPLAN-SIGACT symposium on Principles of programming languages (pp. 207-212).

Hudak, P., Hughes, J., Peyton Jones, S., & Wadler, P. (2007). A history of Haskell: being lazy with class. In Proceedings of the third ACM SIGPLAN conference on History of programming languages (pp. 12-1-12-55).

Leijen, D. (2005). Extensible records with scoped labels. In Proceedings of the 2005 Symposium on Trends in Functional Programming (pp. 297-312).

Maranget, L. (2008). Compiling pattern matching to good decision trees. In Proceedings of the 2008 ACM SIGPLAN workshop on ML (pp. 35-46).

Milner, R., Tofte, M., Harper, R., & MacQueen, D. (1997). The Definition of Standard ML (Revised). MIT Press.

Rémy, D. (1989). Records and variants as a natural extension of ML. In Proceedings of the 16th ACM SIGPLAN-SIGACT symposium on Principles of programming languages (pp. 77-88).

Sussman, G. J., & Steele, G. L. (1975). Scheme: A interpreter for extended lambda calculus. Higher-Order and Symbolic Computation, 11(4), 405-439.

Wand, M. (1987). Complete type inference for simple objects. In Proceedings of the 2nd IEEE Symposium on Logic in Computer Science (pp. 37-44).

## Appendix A: Language Update Prompt

To update this language reference with new information, please provide the following details:

1. **Update Category**: Specify whether you're updating syntax, type system, standard library, or implementation details
2. **Current Content**: Quote the relevant section(s) that need updating
3. **Proposed Changes**: Provide the new content in academic style, maintaining consistency with existing text
4. **Supporting References**: Include any academic references that support the changes
5. **Rationale**: Explain why this update is necessary or beneficial

For example:

```
Update Category: Type System
Current Content: "Bendu supports row polymorphism for records, allowing..."
Proposed Changes: "Bendu implements bounded row polymorphism for records, allowing..."
Supporting References: [Author, Year: Reference details]
Rationale: This clarification better represents the constrained nature of Bendu's row polymorphism implementation.
```
