# Assignment Scenarios

In Bendu all variables are read-only unless they have been defined with a `!` modifier suffix. This syntax is a hat tip to Lisp which encouraged the use of including `!` as a suffix in an identifier name should the operation perform some destructive function or side-effect.  

## Package variables

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
