# `if` Scenarios

All conditions in an `if` statement are evaluated in order, and the first one that is `True` is executed. If none of the conditions are `True`, the `else` block is executed.

```bendu-repl
> if True -> 1 | 2
1: Int

> if False -> 1 | 2
2: Int

> if False -> 1 | True -> 2 | 3
2: Int

> if False -> 1 | False -> 2 | 3
3: Int

> if False -> 1 | False -> 2 | True -> 3 | 4
3: Int

> if False -> 1 | False -> 2 | False -> 3 | 4
4: Int
```

All conditions in an `if` statement need to unify with `Bool`.

```bendu-repl
> if 1 -> 1 | 2
Error: 1:4: Unification error: Unable to unify Bool with Int
```

Similarly all of the branches in an `if` statement need to have the same type.

```bendu-repl
> if True -> 1 | "hello"
Error: 1:16-22: Unification error: Unable to unify Int with String
```

From this we can see that the `if` has the following type rule.

$$\frac{\mathtt{e_{j 1}: {\rm Bool}, e_{j 2}: t, 1 \le j \le n}}{\mathtt{{\tt if}\ e_{1 1} \rightarrow e_{1 2} | \dots | e_{n 1} \rightarrow e_{n 2} : t}}$$

Syntactically it is possible to leave out the last condition and just have an `if` statement with just a branch.  From a typing perspective the optional guard is treated as `True`.
