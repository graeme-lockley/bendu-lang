# Type Inference

The type inference algorithm is an algorithm that determines the type of a variable for a rather simple language. The language's syntax is described in the [parser](./parser.md) documentation. The type inference algorithm is an algorithm that uses a set of rules to determine the type of a variable. The algorithm is implemented in the `infer` function in the `inference.rebo` module.

The objective is to infer the type of the following function:

```
type Num = Int | Float

let add[a: Num](x: a, y: a): a = x + y
```

Particularly I expect the following to be well typed:

```
add(1, 2)
add(1.0, 2.0)
```

However I do expect the following examples to fail:

```
add(1, 2.0)
add(1.0, 2)
```
