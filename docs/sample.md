This is a sample file to build bendu-test.

```bendu-dis
> let inc(x) = x + 1
> inc(4)

 0: JMP 21
 5: LOAD 0 0
14: PUSH_I32_LITERAL 1
19: ADD_I32
20: RET
21: PUSH_I32_LITERAL 4
26: CALL 5 1 0
```

Now we need to get the actual code working...

```bendu-repl
> let inc(x) = x + 1

> inc(4)
5: Int
```

```bendu-dis
> let concat(x, y) = "" + x + y
> concat(concat("hello", " "), "world")

 0: JMP 31
 5: PUSH_STRING_LITERAL
10: LOAD 0 0
19: ADD_STRING
20: LOAD 0 1
29: ADD_STRING
30: RET
31: PUSH_STRING_LITERAL hello
41: PUSH_STRING_LITERAL
47: CALL 5 2 0
60: PUSH_STRING_LITERAL world
70: CALL 5 2 0
```

```bendu-repl
> let concat(x, y) = "" + x + y

> concat(concat("hello", " "), "world")
"hello world": String
```

```bendu-repl
> let factorial(n) = if n < 2 -> 1 | n * factorial(n - 1)

> factorial(3)
6: Int

> factorial(10)
3628800: Int
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
> let odd(n) = if n == 0 -> False | even(n - 1)
. and even(n) = if n == 0 -> True | odd(n - 1)

> odd(5)
True: Bool

> even(5)
False: Bool
```

```bendu-dis
> let odd(n) = if n == 0 -> False | even(n - 1)
. and even(n) = if n == 0 -> True | odd(n - 1)

> odd(5)
> even(5)

 0: JMP 60
 5: LOAD 0 0
14: PUSH_I32_LITERAL 0
19: EQ_I32
20: JMP_FALSE 31
25: PUSH_BOOL_FALSE
26: JMP 59
31: LOAD 0 0
40: PUSH_I32_LITERAL 1
45: SUB_I32
46: CALL 65 1 1
59: RET
60: JMP 120
65: LOAD 0 0
74: PUSH_I32_LITERAL 0
79: EQ_I32
80: JMP_FALSE 91
85: PUSH_BOOL_TRUE
86: JMP 119
91: LOAD 0 0
100: PUSH_I32_LITERAL 1
105: SUB_I32
106: CALL 5 1 1
119: RET
120: PUSH_I32_LITERAL 5
125: CALL 5 1 0
138: DISCARD
139: PUSH_I32_LITERAL 5
144: CALL 65 1 0
```
