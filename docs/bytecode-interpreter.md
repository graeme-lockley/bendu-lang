Bendu has a bytecode interpreter, written in Zig, which allows Bendu code to be executed quickly and efficiently.  Using the interpreter, the feedback loop of writing code and testing is very fast.

Note: this document is describes the bytecode interpreter in its current state rather than a target state.  The document will be updated as the interpreter is developed.

# File Layout

The file layout is a flat structure composed of instructions written out sequentially.

# Instruction Set

Can be found in [bytecode-instructions.json](bytecode-instructions.json).