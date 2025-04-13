# Data.String

This package contains a number of functions over strings.


## length(s: String): Int

Returns the length of the passed string.

```bendu-repl
> import "./lib/Data/String.bendu"

> length("")
0: Int

> length("hello world")
11: Int
```

## at(s: String, pos: Int): Option[Char]

Returns the character at `pos` in `s`.  Should `pos` be less than 0 or greater than the length of `pos`, then `None` is returned.

```bendu
> import "./lib/Data/Option.bendu"
> import "./lib/Data/String.bendu"

> at("", -1)
None(): Option[Char]

> at("hello world", 3)
Some('l'): Option[Char]

> at("hello world", 20)
None(): Option[Char]
```
