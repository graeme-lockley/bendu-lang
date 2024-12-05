# Function Scenarios

Functions in Bendu are first class values. This means that functions can be
passed as arguments to other functions, returned from functions, and stored in
data structures. Functions can also be defined inside other functions. This
allows for a wide range of programming styles and patterns.

The scenarios listed are a gentle unfolding of functions tied to the
implementation of the language. The scenarios reflect the implementation
sequence where functions are implemented in a variety of ways with different
degrees of complexity and overhead.

## Package function without closure

The simplest form of a function is a function that is defined in a package and
does not need a closure to be created. This allows the invocation to be
performed in a very traditional style where the arguments are pushed onto the
stack and the invocation itself jumps to an offset within the same code block.

```bendu-repl
> let inc(n) = n + 1

> inc(1)
2: Int

> inc(10)
11: Int
```

The generated code is shown in the following snippet.

```bendu-dis
> let inc(n) = n + 1
> inc(1)
> inc(10)

 0: JMP 21
 5: PUSH_PARAMETER 0
10: PUSH_I32_LITERAL 1
15: ADD_I32
16: RET 1
21: PUSH_I32_LITERAL 1
26: CALL_LOCAL 5
31: DISCARD
32: PUSH_I32_LITERAL 10
37: CALL_LOCAL 5
```

## In package, recursive, non-higher order, private function without closure

This scenario refers to a type inference concern rather than a compilation or
execution concern. Nonetheless it is necessary working through this scenario
with the additional requirements:

- Simple recursion, and
- Mutual recursion.

### Simple recursion

This is where a procedure is dependent on its self. For example the procedure to
calculate factorial is an excellent example of this recursion.

```bendu-repl
> let factorial(n) =
.   if n < 2 -> 1
.    | n * factorial(n - 1)

> factorial(10)
3628800: Int
```

A classic recursive function is the Ackermann function.

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

### Mutual recursion

This is where multiple procedures are dependent upon each other. The following
example illustrates this.

```bendu-repl
> let odd(n) = if n == 0 -> False | even(n - 1)
. and even(n) = if n == 0 -> True | odd(n - 1)

> odd(5)
True: Bool

> even(5)
False: Bool
```

Looking at the generated code the the odd/even functions are implemented as follows:

```bendu-dis
> let odd(n) = if n == 0 -> False | even(n - 1)
. and even(n) = if n == 0 -> True | odd(n - 1)

> odd(5)
> even(5)

 0: JMP 48
 5: PUSH_PARAMETER 0
10: PUSH_I32_LITERAL 0
15: EQ_I32
16: JMP_FALSE 27
21: PUSH_BOOL_FALSE
22: JMP 43
27: PUSH_PARAMETER 0
32: PUSH_I32_LITERAL 1
37: SUB_I32
38: CALL_LOCAL 53
43: RET 1
48: JMP 96
53: PUSH_PARAMETER 0
58: PUSH_I32_LITERAL 0
63: EQ_I32
64: JMP_FALSE 75
69: PUSH_BOOL_TRUE
70: JMP 91
75: PUSH_PARAMETER 0
80: PUSH_I32_LITERAL 1
85: SUB_I32
86: CALL_LOCAL 5
91: RET 1
96: PUSH_I32_LITERAL 5
101: CALL_LOCAL 5
106: DISCARD
107: PUSH_I32_LITERAL 5
112: CALL_LOCAL 53
```
