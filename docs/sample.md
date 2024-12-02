This is a sample file to build bendu-test.


```bendu-dis
> let x = 10
. x + 5

 0: PUSH_I32_LITERAL 10
 5: PUSH_STACK 0
10: PUSH_I32_LITERAL 5
15: ADD_I32
```

```bendu-error
> x
Unknown Identifier: x at 1:13
```

```bendu-repl
> 'a'
'a': Char
```

```bendu-repl
> "Hello"
"Hello": String
```

```bendu-repl
> ()
(): Unit
```

```bendu-repl
> True
True: Bool

> False
False: Bool
```

```bendu-repl
> let identity(x) = x

> identity(10)
10: Int

> identity(3.14)
3.14: Float

> identity("Hello World")
"Hello World": String
```

```xbendu-repl
> let ackermann(m, n) = 
.   if m == 0 -> n + 1 
.    | n == 0 -> ackermann(m - 1, 1) 
.    | ackermann(m - 1, ackermann(m, n - 1))

> ackermann(1, 2)
4: Int

> ackermann(2, 3)
9: Int

> ackermann(3, 2)
29: Int
```

```sbendu-repl
> 40 + 2
42: Int

> 49 - 7
42: Int

> 6 * 7
42: Int

> 2 ** 8
256: Int

> 84 / 2
42: Int

> 84 % 5
4: Int

> 23 == (20 + 3)
True: Bool

> 23 == (20 + 2)
False: Bool

> 23 != (20 + 3)
False: Bool

> 23 != (20 + 2)
True: Bool

> 1 < 1
False: Bool

> 1 < 2
True: Bool

> 2 < 1
False: Bool

> 1 <= 1
True: Bool

> 1 <= 2
True: Bool

> 2 <= 1
False: Bool

> 1 > 1
False: Bool

> 1 > 2
False: Bool

> 2 > 1
True: Bool

> 1 >= 1
True: Bool

> 1 >= 2
False: Bool

> 2 >= 1
True: Bool
```