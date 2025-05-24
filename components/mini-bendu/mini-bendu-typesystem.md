# Mini-Bendu Type System

This document provides a comprehensive overview of the type system implemented in the mini-bendu language, a statically typed functional language. The type system combines traditional Hindley-Milner type inference with modern extensions such as union types, intersection types, and row polymorphism.

## Core Type System Architecture

### Static and Strong Typing

Mini-bendu employs a static type system with strong typing semantics. This means:

- All expressions have a type that is determined at compile time
- Type checking is performed before program execution
- Type errors are detected early, before the program runs
- The language does not perform implicit type coercions
- Runtime type safety is guaranteed for well-typed programs

### Type Inference

Mini-bendu features a sophisticated type inference system inspired by Hindley-Milner:

- Types are inferred automatically without requiring explicit annotations in most contexts
- The system reconstructs the most general type for expressions
- Optional type annotations can be provided to guide inference or for documentation
- Type annotations are especially useful at function boundaries and module interfaces
- Type inference reduces verbosity while maintaining type safety

The inference algorithm operates by:

1. Assigning type variables to expressions without explicit annotations
2. Generating constraints based on how expressions are used
3. Unifying constraints to solve for concrete types
4. Applying the solved types back to the AST
5. Generalizing types at let-bindings to enable polymorphism

### Parametric Polymorphism

The type system supports parametric polymorphism, allowing functions and data types to operate uniformly over values of different types:

- Type parameters (generics) are represented in square brackets: `[A]`, `[K, V]`
- Type parameters can have constraints to limit the types they can accept
- Polymorphic functions allow code reuse without sacrificing type safety
- Type inference automatically determines when polymorphism is needed

Example of a polymorphic function type:
```
type Mapper[A, B] = (A) -> B
```

### Type Expressions

The type language in mini-bendu is expressive and allows for a variety of type constructs:

#### Basic Types

- Base types: `Int`, `String`, `Bool`
- Type constructors: `List[A]`, `Option[A]`, `Result[E, A]`
- Type constructors with multiple parameters: `Map[K, V]`

#### Function Types

Function types are represented using the arrow notation:

```
(Int) -> String      // A function taking an Int and returning a String
(A) -> (B) -> C      // A curried function: A -> B -> C
(A, B) -> C          // A function taking a tuple (A, B) and returning C
```

Function types are right-associative, meaning `A -> B -> C` is parsed as `A -> (B -> C)`.

#### Record Types

Record types describe object-like structures with named fields:

```
{ name: String, age: Int }
{ x: Int, y: Int, z: Int }
```

Record types support structural subtyping:

- Width subtyping: A record with more fields is a subtype of a record with fewer fields
- Depth subtyping: If type A is a subtype of type B, then `{field: A}` is a subtype of `{field: B}`

Records can also be extended through inheritance:
```
{ x: Int, y: Int | Point2D }
```

#### Tuple Types

Tuple types represent fixed-size heterogeneous collections:

```
(Int, String)            // A pair of Int and String
(A, B, C)                // A triple of arbitrary types
()                       // The unit type (empty tuple)
```

#### Union Types

Union types (sum types) represent values that could be one of several possibilities:

```
Success | Failure                     // Either Success or Failure
Int | String | Bool                   // Either Int, String, or Bool
None | Some[A]                        // Option type implementation
```

Union types are particularly useful for:
- Error handling (`Result[Error, Value]`)
- Optional values (`None | Some[A]`)
- Algebraic data types
- Typed pattern matching

#### Intersection Types

Intersection types represent values that satisfy multiple type constraints simultaneously:

```
Printable & Comparable      // Both Printable and Comparable
A & B                       // Both A and B
```

Intersection types are useful for:
- Type refinement
- Mixin-style composition
- Expressing complex constraints on type parameters

#### Literal Types

String literals can be used as types, enabling a form of singleton types:

```
"success"                   // The literal string "success" as a type
"error"                     // The literal string "error" as a type
"pending" | "fulfilled"     // Union of string literals
```

Literal types allow for:
- Typed enumerations
- Discriminated unions
- Precise type-level constraints

## Type Checking Process

The type checking process in mini-bendu consists of several phases:

### 1. Environment Construction

- Build an initial environment containing primitive types (`Int`, `String`, `Bool`)
- Add predefined functions and their types to the environment
- Process type alias declarations
- Create scopes for variable bindings

### 2. Constraint Generation

During this phase, the type checker:

- Traverses the AST in a bottom-up fashion
- Assigns fresh type variables to expressions without explicit types
- Generates equality constraints between expected and actual types
- Handles subtyping relationships for records and other complex types
- Tracks the location of expressions for error reporting

Constraints are generated for each kind of expression:

- Variables: Look up the type in the environment
- Literals: Assign the corresponding literal type
- Function applications: Ensure the function type and argument types are compatible
- Record expressions: Infer the type of each field
- Pattern matching: Ensure patterns are exhaustive and type-compatible

### 3. Constraint Solving

The constraint solver:

- Applies unification to solve type equality constraints
- Handles type variable substitutions
- Resolves subtyping constraints
- Performs occurs check to prevent infinite types
- Detects and reports type errors with meaningful messages

### 4. Type Application

After solving constraints:

- Apply the solved types back to the AST
- Generalize let-bound expressions to create polymorphic type schemes
- Instantiate type schemes with fresh type variables when used

### 5. Error Reporting

The type checker provides detailed error messages:

- Shows the exact location of the type error
- Explains the expected and actual types
- Offers suggestions for fixing the error
- Tracks the flow of inference to explain how conflicting types were derived

## Advanced Type System Features

### Row Polymorphism

Mini-bendu supports row polymorphism for records, enabling:

- Functions that accept records with a minimum set of fields
- Flexible record operations with type safety
- Record extension and restriction with static typing

Example of row polymorphism:
```
type WithName = { name: String | r }  // Any record containing a name field
```

### Pattern Matching and Exhaustiveness Checking

The type system enhances pattern matching with:

- Exhaustiveness checking to ensure all cases are covered
- Type refinement within match branches
- Specialized handling for union types
- Analysis of nested patterns

Example:
```
match value with
  0 => "zero"
  n if n > 0 => "positive"
  _ => "negative"
```

The type checker verifies that all possible values are matched.

### Type Refinement

Types are refined through:

- Equality checks
- Pattern matching
- Predicate functions
- Assertion expressions

This allows the type checker to track more precise types within specific contexts.

### Recursive Types

The type system supports recursive types for expressing:

- Linked lists
- Tree structures
- Other self-referential data structures

Example:
```
type List[A] = Nil | Cons(A, List[A])
```

## Polymorphic Let Bindings

Mini-bendu implements "let-polymorphism," a key feature of Hindley-Milner type systems:

```
let id = \x => x  // Inferred as [A](A) -> A
```

The `id` function can be used with different types in the same scope:

```
let a = id(5)         // a: Int
let b = id("hello")   // b: String
```

## Type Classes and Ad-hoc Polymorphism

Though not explicitly implemented as Haskell-style type classes, mini-bendu supports ad-hoc polymorphism through:

- Intersection types
- Constrained type parameters
- Structural typing for interfaces

Example of constrained type parameters:
```
type Sortable[A: Comparable] = List[A]
```

## Implementation Considerations

### Type Representation

The implementation uses the following structures:

- `TypeExpr` - The base abstract class for all type expressions
- `BaseTypeExpr` - For named types like `Int` or user-defined types
- `FunctionTypeExpr` - For function types `A -> B`
- `TupleTypeExpr` - For tuple types `(A, B, C)`
- `RecordTypeExpr` - For record types `{a: A, b: B}`
- `UnionTypeExpr` - For union types `A | B`
- `MergeTypeExpr` - For intersection types `A & B`
- `LiteralStringTypeExpr` - For string literal types

### Unification Algorithm

The unification algorithm is responsible for:

- Solving type equality constraints
- Handling type variable substitutions
- Preventing infinite types via occurs check
- Reporting meaningful type errors

### Environment Management

Type checking requires:

- Maintaining a type environment mapping variables to their types
- Handling lexical scoping correctly
- Managing type variable freshening during instantiation
- Processing recursive types

### Subtyping Rules

Subtyping is particularly important for:

- Record types (width and depth subtyping)
- Union types (a type is a subtype of a union containing it)
- Intersection types (an intersection is a subtype of its components)
- Function types (contravariant in argument types, covariant in return type)

## Conclusion

The mini-bendu type system combines the safety and inference capabilities of Hindley-Milner with modern features like union and intersection types. This provides a balance between:

- Type safety and early error detection
- Inference and reduced annotation burden
- Expressiveness and practical utility
- Runtime safety and performance

The type system is sophisticated enough to handle complex functional programming patterns while remaining pragmatic and user-friendly.
