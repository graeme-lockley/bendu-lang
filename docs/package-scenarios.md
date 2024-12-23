# Package Scenarios

There are 3 styles of imports in Bendu:

- Import all public members into a script,
- Import all public members into a script under a distinct name, and
- Import specific members into a script.

Each of these scenarios is different in how they are implemented both in the compiler and in the bytecode interpreter.

To assist with the explanation, the following package is used:

```bendu
let pi = 3.1415926

let identity(x) = x

let constant(x) = fn(y) = x

let valueA! = 1

let funA!(x) = x + 1

let state(n: Int) = (valueA, funA(n))
```

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
```

Even-though `valueA` and `funA` are defined with a `!` qualifier, they are still imported into the script and the consequence of assigning to them is visible across all uses of the package.

```bendu-repl
> import "docs/example.bendu"

> state(5)
(1, 6): Int * Int

> valueA := 2
> valueA
2: Int

> state(3)
(2, 4): Int * Int

> funA := fn(n) = n + 2
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
 5: CALL_PACKAGE -1 19 1
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