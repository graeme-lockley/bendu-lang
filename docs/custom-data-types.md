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

We can now rewrite `sum` using a `fold` function.

```bendu-repl
> type List[a] = Nil | Cons[a, List[a]]

> let fold(xs, f, z) =
.   match xs with
.   | Nil() -> z
.   | Cons(c, cs) -> fold(cs, f, f(z, c))

> let range(n) = {
.   let helper(i) =
.     if i > n -> Nil() 
.      | Cons(i, helper(i + 1))
.
.   helper(0)
. }

> let sum(xs) =
.   fold(xs, fn(a, b) = a + b, 0)


> sum(range(10))
55: Int
```

## Additional Pattern  Scenarios

The pattern matching has three additional features that are worth highlighting:

- A pattern can have a type qualifier,
- A component of a pattern can be assigned an identifier, and
- A expression can be added to a pattern and incorporated into the guard.

#### Pattern Type Qualifier

```bendu-repl
> let f(n) =
.   match n with
.   | v: Int -> v
fn: (Int) -> Int
```

## Custom Data Type Scenarios

The following is a systematic list of the scenarios that can be used to test custom data types.

### Literal Values

#### Boolean
```bendu-repl
> let f(n) = 
.   match n with
.   | True -> "True"
.   | False -> "False"
fn: (Bool) -> String

> f(True)
"True": String

> f(False)
"False": String
```

#### Character

```bendu-repl
> let f(n) = 
.   match n with
.   | 'a' -> "A"
.   | 'b' -> "B"
.   | _ -> "Other"
fn: (Char) -> String

> f('a')
"A": String

> f('b')
"B": String

> f('c')
"Other": String
```

#### Float

```bendu-repl
> let f(n) = 
.   match n with
.   | 1.0 -> "One"
.   | 2.0 -> "Two"
.   | _ -> "Big"
fn: (Float) -> String

> f(1.0)
"One": String

> f(2.0)
"Two": String

> f(3.0)
"Big": String
```

#### Integer

```bendu-repl
> let f(n) = 
.   match n with
.   | 1 -> "One"
.   | 2 -> "Two"
.   | _ -> "Big"
fn: (Int) -> String

> f(1)
"One": String

> f(2)
"Two": String

> f(3)
"Big": String
```

#### String

```bendu-repl
> let f(n) = 
.   match n with
.   | "One" -> 1
.   | "Two" -> 2
.   | _ -> 180
fn: (String) -> Int

> f("One")
1: Int

> f("Two")
2: Int

> f("Hello")
180: Int
```

#### Tuple

```bendu-repl
> let f(n) = 
.   match n with
.   | (1, 2) -> "One"
.   | (3, 4) -> "Two"
.   | _ -> "Other"
fn: (Int * Int) -> String

> f((1, 2))
"One": String

> f((3, 4))
"Two": String

> f((5, 6))
"Other": String
```

#### Unit

```bendu-repl
> let f(n) = 
.   match n with
.   | () -> "Unit"
fn: (Unit) -> String

> f(())
"Unit": String
```

### Transform Custom Data Type Case Expressions

The matching algorithm needs to transform all custom data type case expressions.  The following scenarios consider each expression type as a case expression and ensures that the transformation is correct by executing the compiled code.

#### Abort Expression

```bendu-repl
> type List[a] = Nil | Cons[a, List[a]]

> let f(n) =
.   match n with
.   | Nil() -> "Nil"
.   | Cons(x, _) -> abort("Abort: ", x)
fn: [a] (List[a]) -> String

> f(Nil())
"Nil": String
```

```bendu-err
> type List[a] = Nil | Cons[a, List[a]]

> let f(n) =
.   match n with
.   | Nil() -> "Nil"
.   | Cons(x, _) -> abort("Abort: ", x)
fn: [a] (List[a]) -> String

> f(Cons(1, Nil()))
Abort: 1
```

#### Apply Expression

```bendu-repl
> type List[a] = Nil | Cons[a, List[a]]

> let inc(n) = n + 1

> let f(n) =
.   match n with
.   | Nil() -> 0
.   | Cons(x, _) -> inc(x)
fn: (List[Int]) -> Int

> f(Nil())
0: Int

> f(Cons(1, Nil()))
2: Int

> f(Cons(2, Nil()))
3: Int
```

#### Array Element Projection Expression

```bendu-repl
> type Optional[a] = None | Some[a]

> let f(n) =
.   match n with
.   | None() -> 0
.   | Some(x) -> x!3
fn: (Optional[Array[Int]]) -> Int

> f(None())
0: Int

> f(Some([1, 2, 3, 4, 5]))
4: Int
```

#### Array Range Projection Expression

```bendu-repl
> type Optional[a] = None | Some[a]
> type Option = Left | Right | Both

> let f(n, op) =
.   match (n, op) with
.   | (None(), _) -> []
.   | (Some(x), Left()) -> x!:1
.   | (Some(x), Right()) -> x!3:
.   | (Some(x), Both()) -> x!1:3
fn: [a] (Optional[Array[a]], Option) -> Array[a]

> f(None(), Left())
[]: [a] Array[a]

> f(Some([1, 2, 3, 4, 5]), Left())
[1]: Array[Int]

> f(Some([1, 2, 3, 4, 5]), Right())
[4, 5]: Array[Int]

> f(Some([1, 2, 3, 4, 5]), Both())
[2, 3]: Array[Int]
```

#### Assignment Expression

```bendu-repl
> let x! = 0

> let f(n) =
.   match n with
.   | 0 -> x := 100
.   | 1 -> x := x + 1
.   | _ -> x := n

> f(0)
100: Int

> x
100: Int

> f(1)
101: Int

> x
101: Int

> f(10)
10: Int

> x
10: Int
```

#### Binary Op Expression

```bendu-repl
> let f(a, b) =
.   match a with
.   | 0 -> b * b
.   | _ -> a * b
fn: (Int, Int) -> Int

> f(0, 5)
25: Int

> f(2, 7)
14: Int
```

#### Block Expression

```bendu-repl
> let f(n) =
.   match n with
.   | 0 -> { 1 }
.   | v -> { v * v }
fn: (Int) -> Int

> f(0)
1: Int

> f(10)
100: Int
```

#### If Expression

```bendu-repl
> type List[a] = Nil | Cons[a, List[a]]

> let f(n) =
.   match n with
.   | Nil() -> "Nil"
.   | Cons(x, _) -> if x == 1 -> "One" | "Other"
fn: (List[Int]) -> String

> f(Nil())
"Nil": String

> f(Cons(1, Nil()))
"One": String

> f(Cons(2, Nil()))
"Other": String
```

#### Let Expression

```bendu-repl
> let f(n) =
.   match n with
.   | 0 -> { 1 }
.   | v -> { let r = v * v ; r }
fn: (Int) -> Int

> f(0)
1: Int

> f(10)
100: Int
```

```bendu-repl
> let f(n) =
.   match n with
.   | 0 -> { 1 }
.   | v -> { let double(a: Int) = a * a ; double(v) }
fn: (Int) -> Int

> f(0)
1: Int

> f(10)
100: Int
```

#### Literal Array Expression

```bendu-repl
> let f(n) =
.   match n with
.   | 0 -> [1]
.   | v -> [1, 2, v]
fn: (Int) -> Array[Int]

> f(0)
[1]: Array[Int]

> f(10)
[1, 2, 10]: Array[Int]
```

#### Literal Function Expression

```bendu-repl
> let f(n) =
.   match n with
.   | 0 -> fn(v) = v###
.   | v -> fn(vv) = (vv - v) * 2
fn: (Int) -> (Int) -> Int

> f(0)(10)
10: Int

> f(10)(3)
-14: Int
```

#### Literal Tuple Expression

```bendu-repl
> let f(n) =
.   match n with
.   | 0 -> (0, 0)
.   | v -> (n, n * 2)
fn: (Int) -> Int * Int

> f(0)
(0, 0): Int * Int

> f(10)
(10, 20): Int * Int
```

#### Literal Type Expression

```bendu-repl
> type Optional[a] = None | Some[a]

> let f(n) =
.   match n with
.   | None() -> 0
.   | Some((a, b)) -> a + b
fn: (Optional[Int * Int]) -> Int

> f(None())
0: Int

> f(Some((1, 2)))
3: Int
```

#### Match Expression

```bendu-repl
> type Optional[a] = None | Some[a]

> let f(n) =
.   match n with
.   | None() -> 0
.   | Some(a) -> 
.       match a with
.       | None() -> 1
.       | Some(b) -> b
fn: (Optional[Optional[Int]]) -> Int

> f(None())
0: Int

> f(Some(None()))
1: Int

> f(Some(Some(10)))
10: Int
```