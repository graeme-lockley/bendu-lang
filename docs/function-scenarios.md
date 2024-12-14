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
 5: LOAD 0 0
14: PUSH_I32_LITERAL 1
19: ADD_I32
20: RET
21: PUSH_I32_LITERAL 1
26: CALL 5 1 0
39: DISCARD
40: PUSH_I32_LITERAL 10
45: CALL 5 1 0
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

> factorial(2)
2: Int

> factorial(3)
6: Int

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

## Higher order functions

Higher order functions are functions that take other functions as arguments.  Firstly, let's look at a simple example of a higher order function.

```bendu-dis
> let inc(n) = n + 1
> let x = inc
> x(10)

 0: JMP 21
 5: LOAD 0 0
14: PUSH_I32_LITERAL 1
19: ADD_I32
20: RET
21: PUSH_CLOSURE 5 0
30: STORE 0 0
39: LOAD 0 0
48: PUSH_I32_LITERAL 10
53: CALL_CLOSURE 1
```

```bendu-repl
> let inc(n) = n + 1
> let x = inc
> x(10)
11: Int
```

The classic function composition can be illustrated as follows.

```bendu-repl
> let compose(f, g, n) = f(g(n))
> let double(n) = 2 * n
> let inc(n) = n + 1

> compose(double, inc, 2)
6: Int

> compose(inc, double, 2)
5: Int
```

Looking at the byte-code.

```bendu-dis
> let compose(f, g, n) = f(g(n))
> let double(n) = 2 * n
> let inc(n) = n + 1

> compose(double, inc, 2)
> compose(inc, double, 2)

  0: JMP 43
  5: LOAD 0 0
 14: LOAD 0 1
 23: LOAD 0 2
 32: CALL_CLOSURE 1
 37: CALL_CLOSURE 1
 42: RET
 43: JMP 64
 48: PUSH_I32_LITERAL 2
 53: LOAD 0 0
 62: MUL_I32
 63: RET
 64: JMP 85
 69: LOAD 0 0
 78: PUSH_I32_LITERAL 1
 83: ADD_I32
 84: RET
 85: PUSH_CLOSURE 48 0
 94: PUSH_CLOSURE 69 0
103: PUSH_I32_LITERAL 2
108: CALL 5 3 0
121: DISCARD
122: PUSH_CLOSURE 69 0
131: PUSH_CLOSURE 48 0
140: PUSH_I32_LITERAL 2
145: CALL 5 3 0
```
