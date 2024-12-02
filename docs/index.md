Bendu is a statically typed programming language that is designed to be simple,
easy to use, fast and efficient.

I love tinkering around with programming languages trying to understand the
techniques used to implement them. I have been working on a number of projects
in [little languages](https://github.com/littlelanguages) and
[rebo-lang](https://github.com/graeme-lockley/rebo-lang) exploring these
techniques. In particular, Rebo was a very interesting project that I worked on
as the following properties emerged:

- The start up time was very fast - it felt instantaneous. This had the effect
  of being able to do things very quickly without any wait. Psychologically this
  was very satisfying.
- The language is very simple and easy to use. It is a joy to write code in
  Rebo.
- The testing cycle was very easy in that Rebo supported an executable markdown
  format that allowed tests to be written in a markdown file in a conversational
  style. Consider the
  [language basics](https://github.com/graeme-lockley/rebo-lang/blob/main/docs/index.md)
  documentation for Rebo as an example. Further take a look at
  [a parser](https://github.com/graeme-lockley/bytecode-lang/blob/main/src-compiler/parser.md)
  and it's
  [bytecode compiler](https://github.com/graeme-lockley/bytecode-lang/blob/main/src-compiler/compiler.md)
  as examples of how this works. It is worth noting that executing the markdown
  files to verify the assertions in these three examples takes 11ms, 52ms and
  59ms respectively on a 2017 i7 iMac. This is very fast and makes the testing
  cycle very quick.

I have been thinking about how to combine these properties into a new language
and Bendu is the result. The language is designed to be simple and easy to use,
fast and efficient and to support a conversational style of testing.

Primary influences on Bendu are:

- [Rebo](https://github.com/graeme-lockley/rebo-lang) and it's inspiration
  [Oak](https://oaklang.org)
- [Grain](https://grain-lang.org)
- [Roc](https://roc-lang.org)
- [Gleam](https://gleam.run)

Bendu has the following features:

- Statically typed with type inference
- Local and remote packages
- First class functions
- Pattern matching
- Algebraic data types
- Script based

The tooling is written in Zig with ancillary tools written in Bendu.

# Core Language

Let's get started by getting a feeling for Bendu code.

The goal here is to become familiar with values, functions and control
statements so you will be more confident reading Bendu code in the libraries,
tools and demos.

All the code snippets in this section are valid Bendu code. You can try them out
in the [Bendu Playground](https://bendu-lang.org/playground) or using the Bendu
REPL. The REPL is a great way to experiment with Bendu code.

```bash
$ bendu repl
>
```

## Values

The smallest building block in Bendu is called a **value**. A value is a piece
of data that can be manipulated by a program. There are 9 of different types of
values in Bendu: unit, boolean, char, integer, float, string, function,
sequence, and record types.

Let's start by looking at the simplest value, the unit value. The unit value is
written as `()` and represents the absence of a value. It is used in situations
where a value is required but there is no value to provide.

```bendu-repl
> ()
(): Unit
```

## Functions

A function value is a piece of code that can be executed. It is written as
`fn(args) = expr` where `args` is a comma separated list of arguments each with
an optional default value and `expr` is an expression. The `=` character used in
the definition of a function is optional. Idiomatically it is used when the
function body is a single expression.

### See also

- [Function scenarios](function-scenarios.md) for a breakdown of the
  different ways functions can be used in Bendu and the scenarios that are used
  to implement them.
- [Function implementation](function-implementation.md) for a detailed
  description of how functions are implemented in Bendu's runtime system. This
  note is essentially a description of the stack layout, closure structure and
  the execution model.
