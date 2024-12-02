# Function Implementation

The interpreter supports two kinds of function calls:

- *Package Functions* where a call is made without a closure. In this scenario the
  stack is used to store arguments and the callee's local variables.
- *Closure Functions* where a call is made with a closure. In this scenario the
  callee's arguments and local variables are stored in a frame with a link from this frame to the enclosing frame.  A closure is the link list of frames.

## Package Function Stack Layout

The stack layout for a Package Function call is as follows:

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
- The return address is the address of the next instruction following the call
  instruction.
- The previous frame pointer (FP) is the frame pointer of the caller.

## Closure Function Stack Layout

This form is far more dynamic as the stack is not a single linear structure but
rather a collection of heap allocated frames linked together. We then end up
with a stack for temporary calculations and frames to store the arguments, local
bindings and a link to the enclosing frame. The administration of return
addresses and the results of temporary calculations uses the call stack.

In this context a stack frame is a _nothing_ more than an array of values. The
stack frame therefore has the following layout.

```
+-----------------+
| Enclosing Frame |
+-----------------+
| Argument 1      |
+-----------------+
| Argument 2      |
+-----------------+
| ...             |
+-----------------+
| Argument N      |
+-----------------+
| Local 1         |
+-----------------+
| Local 2         |
+-----------------+
| ...             |
+-----------------+
| Local M         |
+-----------------+
```

This is a dynamic structure as the number of local variables cannot be
determined at compile time. The frame is created when a function is called and
is destroyed through garbage collection when the frame is no longer required.

The call stack is used to manage the return addresses and temporary results of a
function call. The call stack is a stack of return addresses and temporary.

```
+-----------------+
| Frame           | <- FP
+-----------------+
```

Values are loaded from and stored into the frame using the instructions `LOAD` and `STORE` respectively, and the functions are called using `CALL`.  These instructions are defined as follows:

- `LOAD <frame> <offset>`: Load the value at the offset from the frame onto the stack.  The frame, starting at 0 indicates how many frames to traverse to get to the correct frame.  The offset is the offset from the start of the frame.

- `STORE <frame> <offset>`: Store the value on the stack into the frame at the offset.  The frame, starting at 0 indicates how many frames to traverse to get to the correct frame.  The offset is the offset from the start of the frame.

- `CALL <function> <arity> <frame>`: Call the function with the arity number of arguments from the stack.  The frame, starting at 0 indicates how many frames to traverse to get to the correct frame.  A new frame is created for the function call and the arguments are stored in the frame.  These arguments are removed from the call stack and replaced with the return address and the new frame pointer.

Using these operations we can now compile a piece of code that uses a closure.

```bendu
> let factorial(n) {
.   let loop(i) =
.     if i > n -> 1
.      | loop(i + 1) * i
. 
.   loop(1)
. }

> factorial(10)
3628800: Int
```

This is a rather contrived example however it does show off the nested frames and the requirement to access the enclosing frame to get the value of `n`.

```
  JMP main

loop:
  LOAD 0 0       # i
  LOAD 1 0       # n
  GT_INT
  JUMP_FALSE loop_else
  PUSH_INT 1
  RET            # i > n -> 1
loop_else:
  LOAD 0 0       # i
  PUSH_INT 1
  ADD_INT        # i + 1
  CALL loop 1 1  # loop(i + 1)
  LOAD 0 0       # i
  MUL_INT        # loop(i + 1) * i
  RET  

factorial:
  PUSH_INT 1
  CALL loop 1 0  # loop(1)


main:
  PUSH_INT 10
  CALL factorial 1 0
```

Now let's take a second example that shows how frames can be used to implement a closure.  A closure is a function that captures the environment in which it was created.  This is done by storing the environment in the frame of the function.  The following example shows how a closure is implemented.

```bendu
> let mkPlus(n) =
.   fn(m) n + m

> let inc = mkPlus(1)
> let dec = mkPlus(-1)

> inc(10)
11: Int
```

This is a little less contrived than the previous but, equally, it shows how the environment is captured in the frame of the function.  The following is the bytecode for the above code.

```
  JMP main
  
mkPlus:
  CREATE_CLOSURE anon_0
  RET

anon_0:
  LOAD 1 0
  LOAD 0 0
  ADD_INT
  RET

main:
  PUSH_INT 1
  CALL mkPlus 1 0
  STORE 0 0

  PUSH_INT -1
  CALL mkPlus 1 0
  STORE 0 1

  LOAD 0 0
  PUSH_INT 10 
  CALL_CLOSURE 1
```

The only new instruction `CALL_CLOSURE` is defined as follows:

- `CALL_CLOSURE <arity>`: Call the closure with the arity number of arguments from the stack.  The closure is stored in the frame at offset 0.  The arguments are removed from the call stack and replaced with the return address and the new frame pointer.
