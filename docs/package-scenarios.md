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
