# Data.Option

An Option can help you with optional arguments, error handling, and records with optional fields.


## map2[a, b, c](f: (a, b) -> c, op1: Option[a], op2: Option[b]): Option[c]

Apply a function if all the arguments are `Some` value.

```bendu-repl
> import "./lib/Data/Option.bendu"
> let add(a, b): Int = a + b

> map2(add, None(), Some(20))
None(): Option[Int]

> map2(add, Some(10), None())
None(): Option[Int]

> map2(add, Some(10), Some(20))
Some(30): Option[Int]
```