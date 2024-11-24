Bendu has a bytecode interpreter, written in Zig, which allows Bendu code to be executed quickly and efficiently.  Using the interpreter, the feedback loop of writing code and testing is very fast.

Note: this document is describes the bytecode interpreter in its current state rather than a target state.  The document will be updated as the interpreter is developed.

# File Layout

The file layout is a flat structure composed of instructions written out sequentially.

# Instruction Set

Can be found in [bytecode-instructions.json](bytecode-instructions.json).

# Stack Layout

The interpreter supports two kinds of function calls:

- Package functions where a call is made to a function in the same package.  In this scenario the stack is shared between the caller and the callee.
- Closure functions where a call is made to a function that includes a closure.  In this scenario the callee's stack is separated from the caller's stack.

## Package Function Stack Layout

The stack layout for a package function call is as follows:

```
+-----------------+
| Argument 1      |
+-----------------+
| Argument 2      |
+-----------------+
| ...             |
+-----------------+
| Argument N      | <- FP
+-----------------+
| Return Address  |
+-----------------+
| Previous FP     |
+-----------------+
| ...             | <- SP
+-----------------+
```

A number of comments on this layout:

- The stack grows down.
- The return address is the address of the next instruction following the call instruction.
- The previous frame pointer (FP) is the frame pointer of the caller.
