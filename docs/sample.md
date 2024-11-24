This is a sample file to build bendu-test.

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

```bendu-repl
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