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

Elements of the array can be extracted using array projection.

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

An attempt to project beyond the end of the array will result in a fatal error and your application terminating.  In future, array projection will return an optional value.

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
```