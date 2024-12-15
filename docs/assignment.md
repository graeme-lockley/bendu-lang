# Assignment Scenarios

In Bendu all variables are read-only unless they have been defined with a `!` modifier suffix. This syntax is a hat tip to Lisp which encouraged the use of including `!` as a suffix in an identifier name should the operation perform some destructive function or side-effect.  

## Package Variables

The following code shows how a package variable can be manipulated.

```bendu-repl
> let x! = 0

> x
0: Int

> x := 1
1: Int

> x
1: Int
```

A few points to notice:

- Although `x` is declared with the `!` modifier, when `x` is referenced or updated, the `!` is not part of the identifier's name.
- The `:=` operator is used to perform the assignment returning the expression result.

An attempt to change the type during the assignment will result in a unification error.

```bendu-error
> let x! = 0
> x := 1.3

Unification Error: Int 2:13, Float 2:18-20
```

Similarly, an attempt to assign a value to a non-mutable value, will result in an error.

```bendu-error
> let x = 0
> x := 1

Assignment Error: Attempt to assign a value at 2:13 to x declared at 1:5
```

Equally, an attempt to assign to an undefined identifier will result in an error.

```bendu-error
> x := 1

Unknown Identifier: x at 1:13
```

## Package Functions

This scenario is a little more complex in that it requires that, when the function is declared, that it be treated as a closure from the get-go.  Consider the following two functions - essentially the same however looking at the generated code the differences can be seen.

```bendu-dis
> let incA(n) = n + 1
> let incB!(n) = n + 1
> incA(2)
> incB(2)

  0: JMP 21
 5: LOAD 0 0
14: PUSH_I32_LITERAL 1
19: ADD_I32
20: RET
21: JMP 42
26: LOAD 0 0
35: PUSH_I32_LITERAL 1
40: ADD_I32
41: RET
42: PUSH_CLOSURE 26 0
51: STORE 0 0
60: PUSH_I32_LITERAL 2
65: CALL 5 1 0
78: DISCARD
79: LOAD 0 0
88: PUSH_I32_LITERAL 2
93: CALL_CLOSURE 1
```

As can been seen the output is as expected.

```bendu-repl
> let incA(n) = n + 1
> let incB!(n) = n + 1

> incA(2)
3: Int

> incB(2)
3: Int
```

Now, with this, we can do some "interesting" things.

```bendu-repl
> let inc!(n) = n + 1

> inc(2)
3: Int

> inc := fn(n) n * 2
fn: (Int) -> Int

> inc(2)
4: Int
```

Looking at the above code, I am reminded why assignments are positively evil!

For completeness, attempting to assign a value to an immutable function will result in an error.

```bendu-error
> let inc(n) = n + 1
> inc := fn(n) = n = 2

Assignment Error: Attempt to assign a value at 2:13-15 to inc declared at 1:5-7
```
