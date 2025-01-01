# Package Scenarios

There are 3 styles of imports in Bendu:

- Import all public members into a script,
- Import all public members into a script under a distinct name, and
- Import specific members into a script.

Each of these scenarios is different in how they are implemented both in the compiler and in the bytecode interpreter.

To assist with the explanation, the following package is used:

```bendu
type Option*[a] = None | Some[a]

let pi* = 3.1415926

let identity*(x) = x

let constant*(x) = fn(y) = x

let valueA!* = 1

let funA!*(x) = x + 1 

let state*(n: Int) = (valueA, funA(n))

let doubleState(n: Int) = state(n * 2)
```

You will note that all of the declarations have a `*` following their name.  This qualifier is used to indicate that the identifier is public and can be imported into other scripts.

## Import all public members into a script

This is the simplest form of import.  All public members of the package are imported into the script.  The following script imports all public members of the example script.

```bendu-repl
> import "docs/example.bendu"

> pi
3.1415926: Float

> identity
fn: [a] (a) -> a

> identity("hello")
"hello": String

> constant(10)("hello")
10: Int

> valueA
1: Int

> funA(10)
11: Int

> None
fn: [a] () -> Option[a]

> None()
None(): [a] Option[a]

> Some
fn: [a] (a) -> Option[a]

> Some(10)
Some(10): Option[Int]
```

Even-though `valueA` and `funA` are defined with a `!` qualifier, they are still imported into the script and the consequence of assigning to them is visible across all uses of the package.

```bendu-repl
> import "docs/example.bendu"

> state(5)
(1, 6): Int * Int

> valueA := 2
2: Int

> valueA
2: Int

> state(3)
(2, 4): Int * Int

> funA := fn(n) = n + 2
fn: (Int) -> Int

> funA(10)
12: Int

> state(3)
(2, 5): Int * Int
```

The handling of calling `identity` and `funA` is quite different.  Looking at the generated code, you will notice that `identity` is a simple function call, while `funA` is a closure call.  This is because `funA` is variable and, on assignment, the closure value is updated to reflect the new value of `funA`.

```bendu-dis
> import "docs/example.bendu"
> identity(10)
> funA(10)

 0: PUSH_I32_LITERAL 10
 5: CALL_PACKAGE -1 69 1
18: DISCARD
19: LOAD_PACKAGE -1 2
28: PUSH_I32_LITERAL 10
33: CALL_CLOSURE 1
```

Some further comments on this code:

- The `import` statement itself does not generate any bytecode.
- The `CALL_PACKAGE` and `LOAD_PACKAGE` make reference to a package with a negative index.  This index is a reference into the package table in the bytecode interpreter and a negative value indicates that the reference has not yet been bound due to late binding.  The bytecode interpreter will update this value when the package is loaded.
- The `LOAD_PACKAGE` instruction is to load the variable `funA` into the stack.  This is a reference to the closure value of `funA`.  The 2 indicates that this is the 3 variable in the package's frame with the first two being used for `pi` and `valueA`.

More information about the bytecode format can be found in [Package Implementation](./package-implementation.md).

## Import using a name

This form of import is similar to the previous form, but all public members are imported under a distinct name.  The following script imports all public members of the example script under the name `E`.

```bendu-repl
> import "docs/example.bendu" as E

> E.pi
3.1415926: Float

> E.identity
fn: [a] (a) -> a

> E.identity("hello")
"hello": String

> E.constant(10)("hello")
10: Int

> E.valueA
1: Int

> E.funA(10)
11: Int

> E.None
fn: [a] () -> Option[a]

> E.None()
E.None(): [a] Option[a]

> E.Some
fn: [a] (a) -> Option[a]

> E.Some(10)
E.Some(10): Option[Int]
```

Using the same example as before, the following script shows that the mutation of `valueA` and `funA` is visible across all uses of the package.

```bendu-repl
> import "docs/example.bendu" as E

> E.state(5)
(1, 6): Int * Int

> E.valueA := 2
2: Int

> E.valueA
2: Int

> E.state(3)
(2, 4): Int * Int

> E.funA := fn(n) = n + 2
fn: (Int) -> Int

> E.funA(10)
12: Int

> E.state(3)
(2, 5): Int * Int
```

## Import specific members

This form of import is the most restrictive.  Only the specified members are imported into the script.  The following script imports only the `pi` and `identity` members of the example script.

```bendu-repl
> import "docs/example.bendu" exposing (pi, identity, Option)

> pi
3.1415926: Float

> identity
fn: [a] (a) -> a

> identity("hello")
"hello": String

> None
fn: [a] () -> Option[a]

> None()
None(): [a] Option[a]

> Some
fn: [a] (a) -> Option[a]

> Some(10)
Some(10): Option[Int]
```

It is possible to use an alias for each of the imported members.  This is helpful to avoid name clashes.

```bendu-repl
> import "docs/example.bendu" exposing (pi as pieConstant, identity as id)

> pieConstant
3.1415926: Float

> id
fn: [a] (a) -> a

> id("hello")
"hello": String
```

Finally, it is possible to import certain members and, at the same time, import the entire package under a distinct name.

```bendu-repl
> import "docs/example.bendu" as E exposing (pi, identity as id)

> E.pi
3.1415926: Float

> pi
3.1415926: Float

> id
fn: [a] (a) -> a

> id("hello")
"hello": String

> E.identity
fn: [a] (a) -> a

> E.identity("hello")
"hello": String

> E.constant(10)("hello")
10: Int

> E.None
fn: [a] () -> Option[a]

> E.None()
E.None(): [a] Option[a]

> E.Some
fn: [a] (a) -> Option[a]

> E.Some(10)
E.Some(10): Option[Int]

```

To close out this section, note that an attempt to access a value not exported will result in an error.

```bendu-error
> import "docs/example.bendu"

> doubleState(10)
Unknown Identifier: doubleState at 2:13-23
```

```bendu-error
> import "docs/example.bendu" as E

> E.doubleState(10)
Unknown Identifier: doubleState at 2:15-25
```

```bendu-error
> import "docs/example.bendu" exposing (doubleState)

> doubleState(10)
Identifier Not Exported: doubleState at 1:39-49 is not exported from docs/example.bendu
```