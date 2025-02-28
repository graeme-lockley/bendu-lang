# Array

An array is a data structure that stores a collection of elements of the same data type in contiguous memory locations. Each element in the array can be accessed using an index (or subscript), which represents its position in the array.  Arrays are 0 indexed.

Literal arrays are described using the standard notation.

```bendu-repl
> []
[]: [a] Array[a]

> [1, 2]
[1, 2]: Array[Int]

> ["Hello", "World"]
["Hello", "World"]: Array[String]

> [(1, 2), (3, 4), (5, 6)]
[(1, 2), (3, 4), (5, 6)]: Array[Int * Int]
```

Bendu also supports the `...` notation to insert arrays into a literal array.

```bendu-repl
> let x = ["a", "b"]

> ["X", ...x, "Y", ...x, "Z"]
["X", "a", "b", "Y", "a", "b", "Z"]: Array[String]
```

Elements of an array can be extracted using array projection.

```bendu-repl
> [1, 2, 3]!0
1: Int

> [1, 2, 3]!1
2: Int

> [1, 2, 3]!2
3: Int

> ["Hello", "World"]!1
"World": String
```

An attempt to project beyond the end of the array will result in a fatal error and the application terminating.  In future, array projection will return an optional value.

```bendu-error
> [1, 2]!10

Error: Index out of bounds: index: 10, length: 2
```

Use a range expression, it is possible to create a new array by projecting out a sequence of elements.

```bendu-repl
> [1, 2, 3, 4, 5, 6]!2:4
[3, 4]: Array[Int]
```

Range expressions are forgiving in that they can never fail and, if out of range, will default to the array dimensions.

```bendu-repl
> let x = [1, 2, 3, 4, 5]

> x!(-2):2
[1, 2]: Array[Int]

> x!4:100
[5]: Array[Int]

> x!10:100
[]: Array[Int]

> x!2:2
[]: Array[Int]
```

It is also possible to not specify the start or end index of a range.

```bendu-repl
> let x = [0, 1, 2, 3, 4, 5]

> x!:2
[0, 1]: Array[Int]

> x!2:
[2, 3, 4, 5]: Array[Int]

> x!:
[0, 1, 2, 3, 4, 5]: Array[Int]
```

Note that the last form returns a copy of the original array.

Arrays are mutable structures by design.  So, using the projection notation, it is possible to change then elements in an array.

```bendu-repl
> let x = [0, 1, 2, 3, 4, 5]

> x!2 := 20
20: Int

> x
[0, 1, 20, 3, 4, 5]: Array[Int]

> let y = ["a", "b", "c", "d"]

> y!2 := "C"
"C": String

> y
["a", "b", "C", "d"]: Array[String]
```

It is also possible to change ranges of elements.

```bendu-repl
> let x = ["a", "b", "c", "d", "e"]

> x!1:2 := ["X", "Y", "Z"]
["X", "Y", "Z"]: Array[String]

> x
["a", "X", "Y", "Z", "c", "d", "e"]: Array[String]

> x!:3 := ["L"]
> x
["L", "Z", "c", "d", "e"]: Array[String]

> x!2: := ["M"]
> x
["L", "Z", "M"]: Array[String]

> x!: := []
> x
[]: Array[String]
```

Finally, there are a number of operators that simplify the assembling and mutation of arrays.

```bendu-repl
> let x = [1, 2, 3]

> x << 4
[1, 2, 3, 4]: Array[Int]

> x
[1, 2, 3]: Array[Int]

> 0 >> x
[0, 1, 2, 3]: Array[Int]

> x
[1, 2, 3]: Array[Int]

> x <! 4
[1, 2, 3, 4]: Array[Int]

> x
[1, 2, 3, 4]: Array[Int]

> 0 >! x
[0, 1, 2, 3, 4]: Array[Int]

> x
[0, 1, 2, 3, 4]: Array[Int]
```