# Package Implementation

Bendu programs can end up being a rather large collection of files which, depending in the program flow, might not be referenced.  For example a library can contain a function that parses a Bendu script and then executes that script by dynamically loading it into the runtime system.  However, should that library be referenced but this particular function not used, then an entire compiler will be dragged in for no practical purpose.

One of the design principles of Bendu is that its start-up time should be negligible.  Performing a cascading loading of packages would violate this principle.

Before continuing, a handful of definitions:

- A script is Bendu source code to be executed,
- A program is the main script with all its dependent parts,
- A package is a Bendu script that is imported into another script, and
- A package can be in source or binary form.

This then leads to the following requirements:

- When a program is run, the compiler must be able to quickly determine that all dependent packages are up-to-date and, where they are not, compile them into a binary form.
- When a binary package is loaded, dependent binary packages are not loaded until they are used.  This implies late binding.
- Once a package is bound, there must be no identifier lookup - all references must be offset based.

## High-level Thinking

Each script, when compiled, creates multiple assets:

- `.bc` is the binary content,
- `.bc.deps` are all of the dependent source files that the script is dependent on and a timestamp when the source file was compiled, and
- `.bc.sig` all of the type a value declarations that this script exposes

To avoid clutter, these files are not placed alongside the source files in the source directory, but placed in a `.bendu` "cache" inside the logged on user's home directory.

Bendu's runtime system has an array which contains every imported package.  The offsets into this array are critical in that, when a script is bound to a dependent package, this offset is used to determine the location of the package's frame and the package's binary code.

The following bytecode operations make reference to a package:

| Opcode | Parameters | Description |
|--------|------------|-------------|
| `LOAD_PACKAGE` | `package_id`, `offset` | Pushes a value onto the stack from the referenced package and offset into the package's frame. |
| `STORE_PACKAGE` | `package_id`, `offset` | Pops the top value off of the stack and stores it at the offset into the package's frame. |
| `PUSH_PACKAGE_CLOSURE` | `package_id`, `offset` | Pushes a closure made up with the package's frame and offset into the package's binary code. |
| `CALL_PACKAGE` | `package_id`, `offset`, `arity` | Calls a function within a package located at offset into the package's binary code with arity number of arguments.  |

The `package_id`, when negative, indicates that the package has not been bound.  The absolute value is the offset into the script's import table.  This is resolved into a reference into the runtime system's package array and then stored back, into the script's binary code, as a positive value.

### Format of `.bc`

The format of this file is a series of bytes that represent the binary code of the script.  The binary code is a series of instructions that are executed by the runtime system.  The format of the binary code is:

- 4-byte magic number: `0x48 0x57 0x00 0x01` - Hello World followed by the version number
- 4-byte offset into the file where the script's binary code starts
- 4-byte length of the number of entries in the import table.
- Lookup table entries where each import table entry is made up of the following:
  - 4-byte indicator of string length
  - absolute path to the binary file in the Bendu cache
- The binary code starts at this point: a series of instructions that are executed by the runtime system.

### Format of `.bc.deps`

The format of this file is a series of bytes that represent the source files that the script is dependent on.  The format of the binary code is:

- 4-byte magic number: `0x48 0x57 0x00 0x01` - Hello World followed by the version number
- 4-byte indicating number of dependencies
- A series of entries where each entry is made up of the following:
  - 4-byte indicating the length of the path to the source file
  - The absolute path to the source file
  - 8-byte timestamp of when the source file was compiled

### Format of `.bc.sig`

The collection of type and values signatures is a text file which is parsed by the compiler.  This file is made up of a series of lines where each line is a type or value declaration.

## Structure of Bendu Cache

