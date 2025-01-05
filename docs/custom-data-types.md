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

The real magic of custom data types is when they are destructed using pattern matching.  To show off pattern matching, it is best to work through a number of scenarios to see how they work.

```bendu-repl
> type List[a] = Nil | Cons[a, List[a]]

> match Nil() with
. | Nil() -> "Nil"
. | Cons(_, _) -> "Cons"
"Nil": String

> match Cons(1, Nil()) with
. | Nil() -> "Nil"
. | Cons(_, _) -> "Cons"
"Cons": String
```

Being able to select the correct constructor is very helpful.  Next is the ability to be able to access the components of the constructor.

```bendu-repl
> type List[a] = Nil | Cons[a, List[a]]

> match Cons(1, Nil()) with
. | Nil() -> 0
. | Cons(x, xs) -> x
1: Int

> match Cons("Hello", Nil()) with
. | Nil() -> "none"
. | Cons(x, _) -> x
"Hello": String
```

Using this pattern matching, we can now start to create some wonderful recursive functions.

```bendu-repl
> type List[a] = Nil | Cons[a, List[a]]

> let range(n) = {
.   let helper(i) =
.     if i > n -> Nil() 
.      | Cons(i, helper(i + 1))
.
.   helper(0)
. }

> range(10)
 Cons(0, Cons(1, Cons(2, Cons(3, Cons(4, Cons(5, Cons(6, Cons(7, Cons(8, Cons(9, Cons(10, Nil()))))))))))): List[Int]

> let sum(xs) =
.   match xs with
.   | Nil() -> 0
.   | Cons(c, cs) -> c + sum(cs)

> sum(range(10))
55: Int
```
