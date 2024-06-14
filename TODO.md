The following is the implementation TODO list.  It is a list of the things that need to be implemented in the Bendu language.  The list is not exhaustive and will be updated as the implementation progresses.  The intent of this list is to create a roadmap that is based on individual language features rather than the typical compiler pipeline.  This is to ensure that there is gradual progress from lexical analysis to code generation one language feature at a time.

The following *definition of done* is used to determine when a feature is complete:

- The feature is implemented in the language
- The feature is documented in the language documentation
- The feature is tested in the language test suite
- Both the AST interpreter and BC interpreter are updated to support the feature
- No memory leaks for both positive and negative tests

# Scaffolding

- [X] Have lexical errors propagate through to main
- [X] Have syntax errors propagate through to main

# Language

# Data Types

## Unit

- [X] Implement the `Unit` type
- [X] Implement the `Unit` type in the AST interpreter
- [X] Implement the `Unit` type in the BC interpreter
- [X] Integrate into main so that the value can be viewed.

## Record

- [ ] Report an error when attempt is made to reference or assign to an unknown field
- [ ] Incorporate mutable fields

# Typing

- [ ] Report an error when attempt is made to reference unknown type variable
