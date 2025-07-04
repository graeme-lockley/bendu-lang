# bendu-lang

Bendu is a statically typed programming language that is designed to be simple, easy to use, fast and efficient.

I love tinkering around with programming languages trying to understand the techniques used to implement them. I have been working on a number of projects in [little languages](https://github.com/littlelanguages) and [rebo-lang](https://github.com/graeme-lockley/rebo-lang) exploring these techniques.  In particular, Rebo was a very interesting project that I worked on as the following properties emerged:

- The start up time was very fast - it felt instantaneous.  This had the effect of being able to do things very quickly without any wait.  Psychologically this was very satisfying.
- The language is very simple and easy to use.  It is a joy to write code in Rebo.
- The testing cycle was very easy in that Rebo supported an executable markdown format that allowed tests to be written in a markdown file in a conversational style.  Consider the [language basics](https://github.com/graeme-lockley/rebo-lang/blob/main/docs/index.md) documentation for Rebo as an example.  Further take a look at [a parser](https://github.com/graeme-lockley/bytecode-lang/blob/main/src-compiler/parser.md) and it's [bytecode compiler](https://github.com/graeme-lockley/bytecode-lang/blob/main/src-compiler/compiler.md) as examples of how this works.  It is worth noting that executing the markdown files to verify the assertions in these three examples takes 11ms, 52ms and 59ms respectively on a 2017 i7 iMac.  This is very fast and makes the testing cycle very quick.

I have been thinking about how to combine these properties into a new language and Bendu is the result.  The language is designed to be simple and easy to use, fast and efficient and to support a conversational style of testing.

Primary influences on Bendu are:

- [Rebo](https://github.com/graeme-lockley/rebo-lang) and it's inspiration [Oak](https://oaklang.org)
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

The compiler is bootstrapped using Kotlin and the bytecode runtime is written in Zig.

## Examples

The first 1,000 prime numbers:

```bendu
let prime?(n) {
  let loop(i = 2) =
    if i * i > n -> True
     | n % i == 0 -> False
     | loop(i + 1)

  if n < 2 -> False
   | loop()
}

let primes(n) =
  range(2, n)
    |> filter(prime?)

println("The first 1,000 prime numbers are: ", primes(1000))
```

## See also

- Where does the name "Bendu" come from?  I am an annoyingly big fan of Star Wars and [Bendu](https://starwars.fandom.com/wiki/Bendu) is a character in Star Wars Rebels.  The character is a force sensitive being that is neither Jedi nor Sith; a neutral force that is neither good nor evil.  I thought this was a good name for a language that is designed to be simple and easy to use, fast and efficient, supports a conversational style of testing and sits between functional, imperative and object styles as well as interpreted and compiled.


