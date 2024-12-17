# Tuple

A tuple is a lightweight, immutable data structure ideal for grouping related, unchangeable data. Its simplicity, speed, and ability to hold heterogeneous data make it versatile and useful in a wide range of programming tasks.

```bendu-repl
> (1, 2.3, "Hello")
(1, 2.3, "Hello"): Int * Float * String
```

In order to gain access to the tuple elements, there are two forms of destructing.

## Parameter Destructing

A function parameter is able to destruct a tuple. The following is the classic Haskell `fst` and `snd` with full type signatures.

```bendu-repl
> let fst[a, b]((x, _): a * b): a = x
> let snd[a, b]((_, y): a * b): b = y

> fst((1, "Hello"))
1: Int

> snd((1, "Hello"))
"Hello": String
```

Thankfully, with type inference, this can be rewritten as follows with the inferred type signatures shown.

```bendu-repl
> let fst((x, _)) = x
> let snd((_, y)) = y

> fst((1, "Hello"))
1: Int

> snd((1, "Hello"))
"Hello": String

> fst
fn: [a, b] (a * b) -> a

> snd
fn: [a, b] (a * b) -> b
```
