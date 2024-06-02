# Parser

The parser is responsible for converting the input source code into an abstract syntax tree (AST). The AST is a tree representation of the source code that can be used to generate the output code.



## Scanner

The scanner is responsible for converting the input source code into a sequence of tokens. The scanner reads the input source code character by character and groups the characters into tokens.

The scanner definition is described below:

```
tokens
    LowerID = lowerID {digit | id};
    UpperID = upperID {digit | id};
    LiteralFloat = ['-'] digits '.' digits;
    LiteralInt = ['-'] digits;
    LiteralString = '"' {char | '\' ('n' | '"' | ("x" digits ';'))} '"';

comments
   "//" {!cr};

whitespace
  chr(0)-' ';

fragments
  digit = '0'-'9';
  digits = digit {digit};
  lowerID = 'a'-'z';
  upperID = 'A'-'Z';
  id = lowerID + upperID;
  cr = chr(10);
  char = chr(32)-chr(38) + chr(40)-chr(91) + chr(93)-chr(255);
```

The following scenarios illustrate the scanner's behavior.

```rebo-repl
> let { scan } = import("./parser.rebo")

> scan("123 -10 0 123.0 -10.0 0.0")
[ { kind: "LiteralInt", value: 123, start: 0, end: 3 }
, { kind: "LiteralInt", value: -10, start: 4, end: 7 }
, { kind: "LiteralInt", value: 0, start: 8, end: 9 }
, { kind: "LiteralFloat", value: 123.0, start: 10, end: 15 }
, { kind: "LiteralFloat", value: -10.0, start: 16, end: 21 }
, { kind: "LiteralFloat", value: 0.0, start: 22, end: 25 }
]

> scan("a Hello a1 A1")
[ { kind: "LowerID", value: "a", start: 0, end: 1 }
, { kind: "UpperID", value: "Hello", start: 2, end: 7 }
, { kind: "LowerID", value: "a1", start: 8, end: 10 }
, { kind: "UpperID", value: "A1", start: 11, end: 13 }
]
```

A number of tokens are pushed into the scanner from the grammar which are loosely described as symbols and keywords.

```rebo-repl
> let scan(input) =
.    import("./parser.rebo").scan(input) 
.    |> map(fn (token) = token.kind)

> scan(", = + * { } [ ] ( ) : ; | ->")
[ "Comma", "Equals", "Plus", "Star", "LCurley", "RCurley", "LBracket", "RBracket", "LParen", "RParen", "Colon", "Semicolon", "Bar", "MinusGreater" ]

> scan("let type")
[ "Let", "Type" ]
```

The scanner also supports comments and whitespace. Comments are ignored by the scanner, and whitespace is used to separate tokens.

```rebo-repl
> let { scan } = import("./parser.rebo")

> scan("123 // comment\n 456")
[ { kind: "LiteralInt", value: 123, start: 0, end: 3 }
, { kind: "LiteralInt", value: 456, start: 16, end: 19 }
]
```

## Grammar

The grammar for this language is described using the following definition with the lexical definitions from above.

```
program: {declaration} EOF;

declaration: typeDecl | valueDecl | expression;

typeDecl: "type" UpperID "=" type;

valueDecl: "let" LowerID [typeNameBindings] ["(" [LowerID [":" type] {"," LowerID [":" type] }] ")"] [":" type] "=" expression;

typeNameBindings: "[" LowerID [":" type] {"," LowerID [":" type]} "]";

type: typeFunction {"|" typeFunction};
typeFunction: typeTuple {"->" typeFunction};
typeTuple: typeTerm {"*" typeTerm};
typeTerm: LowerID | UpperID | "(" type ")";

expression: addExpression [":" type];
addExpression: term {"+" term};
term: LiteralFloat | LiteralInt | LowerID ["(" [expression {"," expression}])] | "(" expression ")";
```

The grammar defines the structure of the language. The grammar is used by the parser to generate the abstract syntax tree (AST) from the input source code.  The parser is a recursive descent parser with a function a function for each of the non-terminal symbols in the grammar.  These functions accept a `Scanner` as input and returns the AST node for the non-terminal symbol.

### Program

```rebo-repl
> let Parser = import("./parser.rebo")

> Parser.using("type Num = Int ; let x = 1 ; 2", Parser.program)
[ { kind: "TypeDecl"
  , name: "Num"
  , type: { kind: "Constructor", name: "Int" }
  }
, { kind: "ValueDecl"
  , name: "x"
  , expr: { kind: "LiteralInt", value: 1 }
  }
, { kind: "LiteralInt", value: 2 }
]
```

### Declaration

```rebo-repl
> let Parser = import("./parser.rebo")

> Parser.using("type Num = Int", Parser.declaration)
{ kind: "TypeDecl"
, name: "Num"
, type: { kind: "Constructor", name: "Int" }
}

> Parser.using("let x = 1", Parser.declaration)
{ kind: "ValueDecl"
, name: "x"
, expr: { kind: "LiteralInt", value: 1 }
}

> Parser.using("1", Parser.declaration)
{ kind: "LiteralInt", value: 1 }
```

### Type Decl

```rebo-repl
> let Parser = import("./parser.rebo")

> Parser.using("type Num = Int | Float", Parser.typeDecl)
{ kind: "TypeDecl"
, name: "Num"
, type: { kind: "Or"
  , args: 
    [ { kind: "Constructor", name: "Int" }
    , { kind: "Constructor", name: "Float" }
    ]
  }
}
```

### Value Decl

```rebo-repl
> let Parser = import("./parser.rebo")

> Parser.using("let x = 1", Parser.valueDecl)
{ kind: "ValueDecl"
, name: "x"
, expr: { kind: "LiteralInt", value: 1 }
}

> Parser.using("let x: Int = 1", Parser.valueDecl)
{ kind: "ValueDecl"
, name: "x"
, type: { kind: "Constructor", name: "Int" }
, expr: { kind: "LiteralInt", value: 1 }
}

> Parser.using("let x[a: Int]: a = 1", Parser.valueDecl)
{ kind: "ValueDecl"
, name: "x"
, bindings: [ 
    { name: "a", type: { kind: "Constructor", name: "Int" }}
  ]
, type: { kind: "Variable", name: "a" }
, expr: { kind: "LiteralInt", value: 1 }
}

> Parser.using("let add(a, b) = a + b", Parser.valueDecl)
{ kind: "ValueDecl"
, name: "add"
, args: [{ name: "a" }, { name: "b" }]
, expr: 
  { kind: "Add"
  , lhs: { kind: "Identifier", value: "a" }
  , rhs: { kind: "Identifier", value: "b" } 
  }
}

> Parser.using("let add(a: Int, b) = a + b", Parser.valueDecl)
{ kind: "ValueDecl"
, name: "add"
, args: 
  [ { name: "a", type: { kind: "Constructor", name: "Int" } }
  , { name: "b" }
  ]
, expr: 
  { kind: "Add"
  , lhs: { kind: "Identifier", value: "a" }
  , rhs: { kind: "Identifier", value: "b" } 
  }
}

> Parser.using("let add[a: Num](x: a, y: a): a = x + y", Parser.valueDecl)
{ kind: "ValueDecl"
, name: "add"
, bindings: [ { name: "a", type: { kind: "Constructor", name: "Num" } } ]
, args: 
  [ { name: "x", type: { kind: "Variable", name: "a" } }
  , { name: "y", type: { kind: "Variable", name: "a" } }
  ]
, type: { kind: "Variable", name: "a" }
, expr: 
  { kind: "Add"
  , lhs: { kind: "Identifier", value: "x" }
  , rhs: { kind: "Identifier", value: "y" } 
  }
}
```

### Type Name Bindings

```rebo-repl
> let Parser = import("./parser.rebo")

> Parser.using("[a]", Parser.typeNameBindings)
[ { name: "a" }
] 

> Parser.using("[a: Int]", Parser.typeNameBindings)
[ { name: "a", type: { kind: "Constructor", name: "Int" } }
] 

> Parser.using("[a: Int, b: a -> Int]", Parser.typeNameBindings)
[ { name: "a", type: { kind: "Constructor", name: "Int" } }
, { name: "b"
  , type: 
    { kind: "Function"
    , domain: { kind: "Variable", name: "a" }
    , range: { kind: "Constructor", name: "Int" }
    } 
  }
] 
```

### Type

```rebo-repl
> let Parser = import("./parser.rebo")

> Parser.using("Int", Parser.type)
{ kind: "Constructor", name: "Int" }

> Parser.using("Int | Float", Parser.type)
{ kind: "Or"
, args: 
  [ { kind: "Constructor", name: "Int" }
  , { kind: "Constructor", name: "Float" }
  ]
}
```

### Type Function

```rebo-repl
> let Parser = import("./parser.rebo")

> Parser.using("a", Parser.typeFunction)
{ kind: "Variable", name: "a" }

> Parser.using("a -> b", Parser.typeFunction)
{ kind: "Function"
, domain: { kind: "Variable", name: "a" }
, range: { kind: "Variable", name: "b" }
}

> Parser.using("a -> b -> c", Parser.typeFunction)
{ kind: "Function"
, domain:
  { kind: "Function"
  , domain: { kind: "Variable", name: "a" }
  , range: { kind: "Variable", name: "b" }
  }
, range: { kind: "Variable", name: "c" }
}
```

### Type Tuple

```rebo-repl
> let Parser = import("./parser.rebo")

> Parser.using("Int", Parser.typeTuple)
{ kind: "Constructor", name: "Int" }

> Parser.using("Int * Float", Parser.typeTuple)
{ kind: "Tuple"
, types: 
  [ { kind: "Constructor", name: "Int" }
  , { kind: "Constructor", name: "Float" }
  ]
}
```

### Type Term

```rebo-repl
> let Parser = import("./parser.rebo")

> Parser.using("Int", Parser.typeTerm)
{ kind: "Constructor", name: "Int" }

> Parser.using("Float", Parser.typeTerm)
{ kind: "Constructor", name: "Float" }

> Parser.using("a", Parser.typeTerm)
{ kind: "Variable", name: "a" }

> Parser.using("Num", Parser.typeTerm)
{ kind: "Constructor", name: "Num" }

> Parser.using("(Num)", Parser.typeTerm)
{ kind: "Constructor", name: "Num" }
```

### Expression

```rebo-repl
> let Parser = import("./parser.rebo")

> Parser.using("x", Parser.expression)
{ kind: "Identifier", value: "x" }

> Parser.using("x: Num", Parser.expression)
{ kind: "Typed"
, expr: { kind: "Identifier", value: "x" }
, type: { kind: "Constructor", name: "Num" }
}
```

### Add Expression

```rebo-repl
> let Parser = import("./parser.rebo")

> Parser.using("1", Parser.addExpression)
{ kind: "LiteralInt", value: 1 }

> Parser.using("1 + 2", Parser.addExpression)
{ kind: "Add"
, lhs: { kind: "LiteralInt", value: 1 }
, rhs: { kind: "LiteralInt", value: 2 } 
}
```

### Term

```rebo-repl
> let Parser = import("./parser.rebo")

> Parser.using("123.4", Parser.term)
{ kind: "LiteralFloat", value: 123.4 }

> Parser.using("123", Parser.term)
{ kind: "LiteralInt", value: 123 }

> Parser.using("x", Parser.term)
{ kind: "Identifier", value: "x" }

> Parser.using("add()", Parser.term)
{ kind: "Call", name: "add", args: [] }

> Parser.using("add(1)", Parser.term)
{ kind: "Call", name: "add", args: [{ kind: "LiteralInt", value: 1 }] }

> Parser.using("add(1, 2.1)", Parser.term)
{ kind: "Call", name: "add", args: [{ kind: "LiteralInt", value: 1 }, { kind: "LiteralFloat", value: 2.1 }] }

> Parser.using("(x)", Parser.term)
{ kind: "Identifier", value: "x" }
```
