This document serves as the functional description of Bendu in that it:

- Describes the language scenarios through examples,
- Acts as the definitive guide for the language's behavior, and
- Is the automated test pack for the language.

It is rather helpful in that, as the implementation evolves, the scenarios can
be used to verify that the implementations are not introducing any new defects.
It should be pointed out the this document will also show off the error
scenarios as well as the successful scenarios.

Let's get started.

```bendu-repl
> let x = 42
42: Int

> x
42: Int
```

# Data Types

You have seen the `Int` type in the previous example. Bendu has a number of
builtin data types. These are:

| Type   | Description                      |
| ------ | -------------------------------- |
| Unit   | A type with a single value, `()` |
| Bool   | A boolean value                  |
| Int    | A 63-bit signed integer.         |
| Float  | A 64-bit floating point number   |
| Char   | A single character               |
| String | A sequence of characters         |

## Unit

The `Unit` type is a type with a single value, `()`. It is used to represent the
absence of a value.

```bendu-repl
> ()
(): Unit
```

The unit literal is typed as follows:

$$\frac{}{\mathtt{(): {\rm Unit}}}$$

## Bool

The `Bool` type is a boolean value with two values `True` and `False`.  This type is built into the language however it could be defined in the prelude as:

```bendu
type Bool = True | False
```

The `Bool` type is used to represent the truth value of an expression.

```bendu-repl
> True
True: Bool

> False
False: Bool
```

Boolean literals are typed as follows:

$$\frac{}{\mathtt{{\tt True}: {\rm Bool}}} \ \ \ \frac{}{\mathtt{{\tt False}: {\rm Bool}}}$$


# Error Reporting

There are a number of error scenarios that need to be tested.

## Duplicate Declaration

A duplicate declaration occurs when a value is declared more than once. There
are a number of situations where this can occur:

- A value's name is used within the current scope,
- A type's name is used within the current scope, and
- A type variable's name is used within the current scheme.

```bendu-repl
> let x = 12
12: Int

> let x = 13
Error: 1:14-23: Duplicate declaration: x
```

## Lexical Errors

A lexer errors when the scanner encounters a character that it does not
recognize. The scanner reports the error and stops processing the input.

```bendu-repl
> '\1'
Error: 1:1-2: Lexical error: '\
```

## Parser Error

A parser error occurs when the parser encounters a token that it does not
expect. The parser reports the error and stops processing the input.

```bendu-repl
> let x = inc)
Error: 1:12: Syntax error: Found ")", expected one of '[', '{', identifier, '(', false, true, literal char, literal float, literal int, literal string, fn
```

## Unknown Identifier

There are a number of scenarios where an identifier is not defined:

- A value declaration is unknown,
- A field value is unknown,
- A reference to an unknown type, and
- A reference to an unknown type variable

```bendu-repl
> x
Error: 1:1: Unknown name: x
```

# Scratch Pad

$$\frac{\mathtt{t \to t'}}{\mathtt{t_1 t_2 \to t_1' t_2}}  (E-APP-1)$$

