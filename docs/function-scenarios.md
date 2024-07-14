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

```bendu-repl
> let inc(n) = n + 1
fn: (Int) -> Int

> inc(1)
2: Int

> inc(10)
11: Int
```

The exact mechanism surrounding function declaration is a little more complex as
it is necessary to describe the layout of the stack frame. It is worthwhile to
note that there are essentially 2 registers that are used to manage the
interpreter's execution - the instruction pointer (`IP`) and local base pointer
(`LBP`).

- `IP` is the instruction pointer and is used to keep track of the current
  instruction being executed.
- `LBP` is the local base pointer and is used to keep track of the current stack
  frame.

The stack frame is a collection of values placed onto the stack used to store
the return state when, the function's results, function arguments and function
local variables. The stack frame is created when a function is called and is
destroyed when the function returns.

Using some ASCII art, and the stack growing downwards, a typical stack frame
might look like this:

```
+-------------------------------------+
| Argument 1                          | <- LBP - 2
+-------------------------------------+
| Argument 2                          | <- LBP - 1
+-------------------------------------+
| IP of instruction following return  | <- LBP
+-------------------------------------+
| Previous LBP                        | <- LBP + 1
+-------------------------------------+
| Local 1                             | <- LBP + 2
+-------------------------------------+
| Local 2                             | <- LBP + 3
+-------------------------------------+
| Local 3                             | <- LBP + 4
+-------------------------------------+
```

It is the responsibility of the compiler to generate the correct instructions to
manage the stack frame. The bytecode will need to include instructions to create
the function result and push each of the arguments. Invoking `CALL_LOCAL` will
automatically cause the function result, `IP` and `LBP` to be pushed and the
setting of `IP` and `LBP` in the function. The operation `RET` will pop the `IP`
and `LBP` and arguments off of the stack and return to the calling function with
the function result on the top of the stack.

The above might seem a little strange as it implies that it specifically refers
to the compiler. However it is worthwhile to note that the interpreter uses the
same stack structure and, rather than using recursion when performing a call,
continues to iterate and uses this runtime stack to manage the execution.
