# Function Scenarios

Functions in Bendu are first class values. This means that functions can be
passed as arguments to other functions, returned from functions, and stored in
data structures. Functions can also be defined inside other functions. This
allows for a wide range of programming styles and patterns.

The scenarios listed are a gentle unfolding of functions tied to the
implementation of the language. The scenarios reflect the implementation
sequence where functions are implemented in a variety of ways with different
degrees of complexity and overhead.

## In package, non-recursive, non-higher order, private function without closure

The simplest form of a function is a function that is defined in a package and
does not need a closure to be created. This allows the invocation to be
performed in a very traditional style where the arguments are pushed onto the
stack and the invocation itself jumps to an offset within the same code block.
Equally, from a type inference, by being non-recursive, the type inference is
very simple.

```rebo-repl
> let inc(n) = n + 1
fn(n)

> inc(1)
2

> inc(10)
11
```
