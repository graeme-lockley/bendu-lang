# `if` Scenarios

All conditions in an `if` statement are evaluated in order, and the first one
that is `True` is executed. If none of the conditions are `True`, the `else`
block is executed. In order to demonstrate that the flow of the `if` statement
is as expected, we can use division by zero to show that the branches are not
evaluated until they are needed.

```bendu-repl
> if True -> 1 | (2 / 0)
1: Int

> if False -> (1 / 0) | 2
2: Int

> if False -> (1 / 0) | True -> 2 | (3 / 0)
2: Int

> if False -> (1 / 0) | False -> (2 / 0) | 3
3: Int

> if False -> (1 / 0) | False -> (2 / 0) | True -> 3 | (4 / 0)
3: Int

> if False -> (1 / 0) | False -> (2 / 0) | False -> (3 / 0) | 4
4: Int
```

All conditions in an `if` statement need to unify with `Bool`.

```bendu-error
> if 1 -> 1 | 2
Unification Error: Int 1:16, Bool
```

Similarly all of the branches in an `if` statement need to have the same type.

```bendu-error
> if True -> 1 | "hello"
Unification Error: Int 1:24, String 1:28-34
```

From this we can see that the `if` has the following type rule.

$$\frac{\mathtt{e_{j 1}: {\rm Bool}, e_{j 2}: t, 1 \le j \le n}}{\mathtt{{\tt if}\ e_{1 1} \rightarrow e_{1 2} | \dots | e_{n 1} \rightarrow e_{n 2} : t}}$$

Syntactically it is possible to leave out the last condition and just have an
`if` statement with just a branch. From a typing perspective the optional guard
is treated as `True`.
