# Custom Data Types

Custom Data Types, also known as Abstract Data Types (ADT) or Tagged Unions, are a way to define a new type that can have multiple different values. Each value is tagged with a unique identifier that is used to determine which value is stored in the type.

The following is an example of a custom data type in Bendu:

```bendu-repl
> type List[a] = Nil | Cons[a, List[a]]

> Nil
fn: [a] () -> List[a]

> Cons
fn: [a] (a, List[a]) -> List[a]

> Nil()
Nil(): [a] List[a]

> Cons(1, Nil())
Cons(1, Nil()): List[Int]
```
