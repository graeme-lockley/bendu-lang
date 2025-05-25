# Mini-Bendu Type System Implementation Tasks

This document outlines the step-by-step process for implementing the type system for mini-bendu using a Test-Driven Development (TDD) approach. The implementation is divided into two phases:

- **Phase 1**: Core type system with structural typing, row polymorphism, and open record types
- **Phase 2**: Higher-kinded types extension building on the foundation from Phase 1

Each feature will begin with a failing test that defines the expected behavior, followed by the implementation to make the test pass.

# PHASE 1: Core Type System Foundation

This phase implements the foundational type system that leverages mini-bendu's recursive open record structures. The design emphasizes structural typing, row polymorphism, and extensible record types that will provide the perfect foundation for higher-kinded types in Phase 2.

## 1. Foundation Infrastructure

### 1.1 Basic Type Representation

1. **Create TypeVariable class (Test)**
   - Write test that verifies creation of type variables with unique IDs
   - Test equality based on ID, not object identity
   - Test fresh variable generation for instantiation
   - Test type variable levels for rank-n polymorphism (Phase 2 preparation)

2. **Implement TypeVariable class**
   - Create class for representing type variables in substitutions and unification
   - Implement unique ID generation
   - Add methods for creating fresh instances
   - Include level tracking for future rank-n polymorphism support

3. **Create Substitution class (Test)**
   - Test substitution creation and composition 
   - Test application of substitutions to types
   - Test substitution on complex types
   - Test idempotent operations and identity substitutions
   - Test structural preservation for record types

4. **Implement Substitution class**
   - Create substitution map from type variables to types
   - Implement composition of substitutions
   - Implement application to various type expressions
   - Add validation and optimization for common cases
   - Ensure proper handling of structural types for Phase 2

5. **Create Type Scheme class (Test)**
   - Write test for type schemes that verify quantified variables and types
   - Test proper instantiation of fresh type variables
   - Test generalization and specialization operations
   - Test preparation for higher-kinded quantification

6. **Implement Type Scheme class**
   - Implement type schemes for polymorphic functions
   - Add methods for instantiation and generalization
   - Support alpha-equivalence and renaming
   - Design with extensibility for higher-kinded quantification

### 1.2 Type Constraints Infrastructure

7. **Create TypeConstraint classes (Test)**
   - Test creation of equality constraints
   - Test creation of subtyping constraints
   - Test creation of instance constraints for type classes
   - Test constraint composition and simplification
   - Test constraint dependencies and ordering

8. **Implement TypeConstraint classes**
   - Create base constraint class and specific constraint types
   - Implement constraint normalization and simplification
   - Add support for constraint dependencies and ordering
   - Include constraint priority system for solver efficiency

9. **Create ConstraintSet class (Test)**
   - Test constraint set operations (union, intersection)
   - Test constraint simplification within sets
   - Test constraint consistency checking
   - Test constraint propagation rules

10. **Implement ConstraintSet class**
    - Create efficient constraint collection and management
    - Implement constraint simplification and redundancy elimination
    - Add consistency checking and early failure detection
    - Support incremental constraint addition

### 1.3 Type Environment

11. **Create TypeEnvironment class (Test)**
    - Test environment construction with predefined types
    - Test lookup of variables in environment
    - Test extending environment with new bindings
    - Test generalization of type expressions
    - Test scoping and shadowing behavior

12. **Implement TypeEnvironment class**
    - Create environment for mapping identifiers to types
    - Implement scoping and variable lookup
    - Add support for generalization of type expressions
    - Include proper shadowing and lexical scoping

## 2. Unification and Constraint Solving

### 2.1 Unification Algorithm

13. **Create Unification Tests**
    - Test unification of basic types (Int with Int, etc.)
    - Test unification of type variables
    - Test unification of function types
    - Test unification with substitutions
    - Test occurs check to prevent infinite types
    - Test unification of complex nested types

14. **Implement Unification**
    - Implement unification for identical types
    - Implement unification of type variables
    - Implement unification for function types
    - Add occurs check to prevent circular references
    - Implement failure handling with meaningful errors
    - Add support for structural unification

### 2.2 Constraint Generation and Solving

15. **Create Constraint Generation Tests**
    - Test constraint generation for simple expressions
    - Test constraint generation for function applications
    - Test constraint generation for complex nested expressions
    - Test constraint dependency tracking

16. **Implement Constraint Generation**
    - Create constraint generation visitor for AST nodes
    - Generate appropriate constraints for each expression type
    - Track constraint dependencies and ordering
    - Include source location information in constraints

17. **Create ConstraintSolver class (Test)**
    - Test constraint solving for simple cases
    - Test constraint solving with variables
    - Test constraint solving with functions
    - Test error handling for unsolvable constraints
    - Test solver performance on complex constraint sets

18. **Implement ConstraintSolver class**
    - Create mechanism for constraint resolution
    - Implement unification as constraint solving
    - Add error reporting for constraint violations
    - Include solver optimizations and heuristics

### 2.3 Type Alias and Cycle Detection

19. **Create Type Alias Tests**
    - Test simple type alias definitions
    - Test recursive type alias detection
    - Test type alias expansion and normalization
    - Test circular dependency detection

20. **Implement Type Alias Support**
    - Add type alias representation and expansion
    - Implement cycle detection for recursive aliases
    - Add proper error reporting for circular definitions
    - Include type alias normalization for performance

## 3. Type Inference

### 3.1 Type Inference for Basic Expressions

21. **Create Type Inference Tests for Literals**
    - Test inferring types for integer literals
    - Test inferring types for string literals
    - Test inferring types for boolean literals
    - Test string literal types and unions

22. **Implement Type Inference for Literals**
    - Add type inference for integer literals
    - Add type inference for string literals
    - Add type inference for boolean literals
    - Support string literal types

23. **Create Type Inference Tests for Variables**
    - Test inferring types for variable references
    - Test handling of undefined variables
    - Test variable shadowing in nested scopes
    - Test polymorphic variable instantiation

24. **Implement Type Inference for Variables**
    - Add lookup of variables in environment
    - Handle undefined variables with useful error messages
    - Support variable shadowing in nested scopes
    - Implement proper instantiation of polymorphic types

### 3.2 Type Inference for Binary Operations

25. **Create Type Inference Tests for Arithmetic Operations**
    - Test type inference for addition, subtraction, etc.
    - Test type checking for operations with incompatible types
    - Test error messages for type mismatches
    - Test operator overloading for different types

26. **Implement Type Inference for Arithmetic Operations**
    - Add type constraints for arithmetic operations
    - Ensure operands have compatible types
    - Generate appropriate error messages for mismatches
    - Support operator overloading where appropriate

27. **Create Type Inference Tests for Comparison Operations**
    - Test type inference for equality, inequality, etc.
    - Test type checking for comparisons with incompatible types
    - Test structural equality for complex types

28. **Implement Type Inference for Comparison Operations**
    - Add type constraints for comparison operations
    - Handle equality between different types (if supported)
    - Generate appropriate error messages
    - Support structural comparison where needed

29. **Create Type Inference Tests for Logical Operations**
    - Test type inference for AND, OR, NOT
    - Test error handling for non-boolean operands
    - Test short-circuit evaluation typing

30. **Implement Type Inference for Logical Operations**
    - Add type constraints ensuring boolean operands
    - Generate appropriate error messages for non-boolean values
    - Handle short-circuit evaluation properly

### 3.3 Type Inference for Complex Expressions

31. **Create Type Inference Tests for Let Expressions**
    - Test type inference for simple let bindings
    - Test type generalization in let bindings
    - Test recursive let bindings
    - Test polymorphic let bindings
    - Test mutual recursion

32. **Implement Type Inference for Let Expressions**
    - Add support for non-recursive let bindings
    - Add support for recursive let bindings
    - Implement generalization of let-bound variables
    - Support mutual recursion detection and typing

33. **Create Type Inference Tests for Lambda Expressions**
    - Test type inference for simple lambda functions
    - Test lambdas with explicit parameter types
    - Test lambdas with complex body expressions
    - Test higher-order functions

34. **Implement Type Inference for Lambda Expressions**
    - Add support for inferring lambda parameter types
    - Handle explicit type annotations on parameters
    - Infer return types from body expressions
    - Support higher-order function typing

35. **Create Type Inference Tests for Function Application**
    - Test application of functions to arguments
    - Test application with too few/many arguments
    - Test application of polymorphic functions
    - Test error reporting for type mismatches
    - Test curried function applications

36. **Implement Type Inference for Function Application**
    - Add constraint generation for function application
    - Handle curried functions and partial application
    - Instantiate polymorphic functions with fresh variables
    - Provide clear error messages for mismatched arguments

37. **Create Type Inference Tests for Conditional Expressions**
    - Test type inference for if-then-else expressions
    - Test type checking for condition expressions
    - Test type unification of then/else branches
    - Test nested conditional expressions

38. **Implement Type Inference for Conditional Expressions**
    - Add type checking for boolean conditions
    - Unify types of then/else branches
    - Generate appropriate error messages
    - Support nested conditionals properly

### 3.4 Type Inference for Data Structures

39. **Create Type Inference Tests for Records**
    - Test inference for record literals
    - Test record field access
    - Test record extension and spread operations
    - Test width and depth subtyping
    - Test record inheritance patterns
    - Test structural interface matching (Phase 2 preparation)

40. **Implement Type Inference for Records**
    - Add inference for record literals
    - Add type checking for field access
    - Add support for record extension and spread
    - Implement width and depth subtyping
    - Support record inheritance where applicable
    - Design structural matching for future HKT interface compatibility

41. **Create Type Inference Tests for Tuples**
    - Test inference for tuple literals
    - Test destructuring tuples
    - Test type checking for tuples of different sizes/types
    - Test nested tuple structures

42. **Implement Type Inference for Tuples**
    - Add inference for tuple literals
    - Add type checking for tuple destructuring
    - Generate errors for mismatched tuple operations
    - Support nested tuple type inference

## 4. Advanced Type System Features

### 4.1 Union and Intersection Types

43. **Create Tests for Union Types**
    - Test defining union types
    - Test inferring union types
    - Test subtyping with union types
    - Test exhaustiveness checking with unions
    - Test union type normalization and simplification

44. **Implement Union Types**
    - Add representation for union types
    - Add subtyping rules for union types
    - Integrate with pattern matching and exhaustiveness checking
    - Implement union type normalization

45. **Create Tests for Intersection Types**
    - Test defining intersection types
    - Test inferring intersection types
    - Test subtyping with intersection types
    - Test intersection type simplification

46. **Implement Intersection Types**
    - Add representation for intersection types
    - Add subtyping rules for intersection types
    - Integrate with constraint solving
    - Implement intersection type simplification

47. **Create Tests for Merge Type Operations**
    - Test merge operator type checking
    - Test merge with compatible and incompatible types
    - Test merge associativity and precedence
    - Test merge with complex nested types

48. **Implement Merge Type Operations**
    - Add type inference for merge operations
    - Implement merge type compatibility checking
    - Add proper error reporting for incompatible merges
    - Support merge operation optimization

### 4.2 Pattern Matching and Type Refinement

49. **Create Tests for Pattern Matching Type Checking**
    - Test type checking for various pattern types
    - Test exhaustiveness checking
    - Test type refinement in match branches
    - Test nested patterns
    - Test wildcard and variable patterns

50. **Implement Pattern Matching Type Checking**
    - Add type checking for different pattern types
    - Implement exhaustiveness checking
    - Add type refinement within match branches
    - Support nested pattern matching
    - Handle wildcard and variable pattern binding

51. **Create Tests for Pattern Type Refinement**
    - Test type narrowing in match expressions
    - Test flow-sensitive typing
    - Test contradiction detection in patterns
    - Test pattern reachability analysis

52. **Implement Pattern Type Refinement**
    - Add type narrowing based on pattern matches
    - Implement flow-sensitive type checking
    - Detect unreachable patterns and contradictions
    - Add pattern reachability warnings

### 4.3 Row Polymorphism

53. **Create Tests for Row Polymorphism**
    - Test type inference with row variables
    - Test record extension with row polymorphism
    - Test subtyping with row polymorphism
    - Test row constraint solving

54. **Implement Row Polymorphism**
    - Add representation for row variables
    - Add constraint generation for row types
    - Implement row variable unification
    - Add subtyping for row types

55. **Create Tests for Row Operations**
    - Test row concatenation and extension
    - Test row restriction and projection
    - Test row unification with conflicts
    - Test row polymorphic function types

56. **Implement Row Operations**
    - Add row concatenation and extension operations
    - Implement row restriction and projection
    - Handle row unification conflicts properly
    - Support row polymorphic function signatures

### 4.4 Recursive Types

57. **Create Tests for Recursive Types**
    - Test defining recursive types
    - Test using recursive types
    - Test unification with recursive types
    - Test occurs check with recursive types
    - Test mutual recursion in type definitions

58. **Implement Recursive Types**
    - Add support for recursive type definitions
    - Handle recursive references in unification
    - Ensure termination with occurs check
    - Add equi-recursive type equivalence
    - Support mutual recursion in types

## 5. Integrating Type System with Parser

59. **Create Integration Tests for Type Checking AST**
    - Test type checking complete AST nodes
    - Test type inference on complete programs
    - Test error reporting with source locations
    - Test incremental type checking

60. **Implement Type Checker class**
    - Create main type checker class
    - Add visitor pattern for traversing AST
    - Connect constraint generation with AST nodes
    - Integrate with parser to create complete pipeline

61. **Create Tests for Source Location Tracking**
    - Test error messages include proper source locations
    - Test location tracking through transformations
    - Test location preservation in type inference
    - Test location-aware error recovery

62. **Implement Source Location Tracking**
    - Add source location information to type errors
    - Preserve location information through inference
    - Implement location-aware error recovery
    - Add source span tracking for complex expressions

## 6. Error Reporting and Diagnostics

63. **Create Tests for Error Reporting**
    - Test error messages for type mismatches
    - Test error messages for undefined variables
    - Test error messages for incomplete patterns
    - Test source location tracking in errors
    - Test error message clarity and helpfulness

64. **Implement Error Reporting**
    - Add detailed error messages for type errors
    - Add source location tracking
    - Implement suggestion system for common errors
    - Include contextual information in error messages

65. **Create Tests for Error Recovery**
    - Test type checking continues after errors
    - Test error cascading prevention
    - Test multiple error reporting
    - Test error prioritization and filtering

66. **Implement Error Recovery**
    - Add error recovery mechanisms
    - Prevent error cascading in type checking
    - Implement intelligent error filtering
    - Support multiple error reporting with priorities

67. **Create Tests for Diagnostic Features**
    - Test type hints and suggestions
    - Test performance warnings
    - Test style recommendations
    - Test IDE integration features

68. **Implement Diagnostic Features**
    - Add type hints and auto-completion support
    - Implement performance analysis warnings
    - Add code style recommendations
    - Prepare IDE integration capabilities

## 7. End-to-End Integration and Testing

69. **Create End-to-End Test Suite**
    - Test complete programs with complex type scenarios
    - Test interaction between different type features
    - Test performance on realistic codebases
    - Test regression prevention

70. **Implement Integration Test Framework**
    - Create comprehensive test programs
    - Add performance benchmarking
    - Implement regression test automation
    - Include stress testing for complex scenarios

## 8. Documentation and Examples

76. **Create Documentation Tests**
    - Test that examples in documentation are valid
    - Test edge cases mentioned in documentation
    - Test tutorial examples work correctly
    - Test API documentation examples

77. **Update Documentation**
    - Document type system implementation
    - Provide examples of type error messages
    - Document performance characteristics and limitations
    - Create comprehensive API documentation

78. **Create Example Programs**
    - Develop comprehensive example programs
    - Include common patterns and idioms
    - Add examples of advanced type features
    - Create educational progression of examples

79. **Implement Tutorial and Learning Materials**
    - Create step-by-step type system tutorial
    - Add interactive examples and exercises
    - Document best practices and patterns
    - Include troubleshooting guides

# PHASE 2: Higher-Kinded Types Extension

## Overview and Context

Phase 2 builds on the solid foundation of Phase 1 to add higher-kinded types (HKTs) to mini-bendu. This extension leverages mini-bendu's recursive open record structures and structural typing system to provide more flexible and powerful abstractions than traditional HKT implementations.

### Key Design Principles for Phase 2

**Structural HKTs with Open Records**: Unlike traditional ADT-based HKTs, mini-bendu's approach works with open record structures. This means:
- Type constructors work with any record that has the required structural shape
- Records can be extended while preserving HKT interface compatibility  
- Duck typing allows multiple record types to implement the same HKT interface
- Row polymorphism enables preserving extra fields through HKT operations

**Integration with Phase 1 Foundation**: The HKT implementation will build directly on:
- Row polymorphism (Tasks 53-56) for flexible record operations
- Structural typing for records (Tasks 39-40) for interface matching
- Type variable levels (Tasks 1-2) for proper quantification scoping
- Constraint solving (Tasks 15-18) extended with kind constraints
- Pattern matching and type refinement (Tasks 49-52) for HKT pattern support

### Practical Applications Enabled

Phase 2 will enable real-world patterns like:
- Generic container operations that work with any "mappable" record structure
- Unified error handling across different result types (HTTP, DB, File operations)
- Composable data transformation pipelines
- Configuration and dependency injection with effect abstractions
- Testing with different execution contexts (sync/async, pure/effectful)

### Implementation Approach

The HKT extension will include:
1. **Kind System**: Adding kinds (* -> *, * -> * -> *, etc.) with kind inference and checking
2. **Type Constructor Support**: Extending the type system to handle parameterized types  
3. **Higher-Kinded Type Variables**: Supporting type variables that range over type constructors
4. **Structural Interface Matching**: Allowing any record with compatible structure to implement HKT interfaces
5. **Kind-Polymorphic Functions**: Functions that abstract over type constructors

### Grammar Extensions Required

Phase 2 will require minimal grammar changes to support HKT syntax:
- Kind annotations: `F[_]` syntax for higher-kinded type variables
- Type constructor applications with kind checking
- Optional explicit kind annotations for complex cases

### Compatibility and Migration

The HKT extension is designed to be:
- **Fully backward compatible** with Phase 1 code
- **Opt-in**: Existing code continues to work without changes
- **Incremental**: HKT features can be adopted gradually
- **Structural**: Works naturally with mini-bendu's open record design

This design makes HKTs more practical and accessible than traditional implementations while providing the same expressive power for generic programming, library design, and abstraction over computational contexts.

## Phase 1 Execution Strategy

For each task in Phase 1, follow this TDD process:

1. **Write a failing test** that defines the expected behavior
2. **Run the test** to see it fail (Red phase)
3. **Implement the minimal code** to make the test pass (Green phase)
4. **Refactor the code** while ensuring tests continue to pass (Refactor phase)
5. **Document the implemented feature** and update API documentation

### Implementation Guidelines

- **Use small, incremental steps** to build up complexity gradually
- **Tasks should be tackled in roughly the order presented**, as later tasks depend on the infrastructure created by earlier ones
- **Each task should be completable in 1-4 hours** to maintain momentum
- **Write comprehensive tests** that cover both positive and negative cases
- **Include edge cases and error conditions** in test coverage
- **Design with Phase 2 extensibility in mind** - avoid decisions that would require major refactoring for HKTs

### Test Strategy

- **Unit tests** for individual components (types, constraints, unification)
- **Integration tests** for complete type checking scenarios
- **End-to-end tests** for full programs with complex type interactions
- **Performance tests** for scalability and optimization validation
- **Regression tests** to prevent breaking existing functionality

### Quality Assurance

- **Code reviews** for all major implementations
- **Performance profiling** after optimization tasks
- **Documentation reviews** to ensure clarity and completeness
- **User testing** with realistic mini-bendu programs
- **Continuous integration** to catch regressions early

### Dependencies and Ordering for Phase 1

The Phase 1 tasks are organized with careful attention to dependencies:

1. **Foundation (Tasks 1-12)**: Core infrastructure designed for extensibility
2. **Unification (Tasks 13-20)**: Essential algorithms with structural type support
3. **Basic Inference (Tasks 21-42)**: Type inference for fundamental language constructs
4. **Advanced Features (Tasks 43-58)**: Complex type system features emphasizing structural typing
5. **Integration (Tasks 59-70)**: Connecting components and testing interactions
6. **Optimization (Tasks 73-75)**: Performance and scalability improvements
7. **Documentation (Tasks 76-79)**: Comprehensive documentation and examples

### Phase 1 Success Criteria

Phase 1 is considered complete when:
- All 79 tasks pass their tests
- The type system correctly handles mini-bendu's open record structures
- Row polymorphism works seamlessly with record operations
- Structural typing enables flexible interface matching
- Performance is acceptable for typical mini-bendu programs
- Documentation provides clear examples of all features
- The foundation is ready for Phase 2 HKT extension

### Risk Management

- **Parallel development tracks** where dependencies allow
- **Regular checkpoint reviews** to assess progress and adjust plans
- **Prototype implementations** for complex or uncertain features
- **Fallback strategies** for features that prove too complex
- **Incremental delivery** to provide value throughout development
- **Phase 2 compatibility checks** at major milestones

For integration testing, create a comprehensive suite of mini-bendu programs that exercise different features of the type system, including both valid programs and programs with various type errors. These should cover:

- Simple programs with basic types and operations
- Complex programs with advanced type features (focusing on record operations)
- Programs that demonstrate type system edge cases
- Programs that should produce specific error messages
- Performance stress tests with large type expressions
- Real-world usage patterns and common programming idioms
- Examples that will naturally extend to HKT patterns in Phase 2

This comprehensive plan provides a structured approach to implementing a production-quality type system for mini-bendu while maintaining high code quality and thorough testing throughout the development process. The design ensures Phase 1 creates an optimal foundation for the higher-kinded types extension in Phase 2.

## 7. Performance Optimization and Scalability

73. **Create Benchmarks for Type Checking**
    - Measure performance on various program sizes
    - Identify bottlenecks in type checking process
    - Test memory usage patterns
    - Benchmark constraint solving performance

74. **Optimize Type Checker**
    - Implement memoization for type checking expressions
    - Optimize constraint solving algorithm
    - Reduce memory usage for type representations
    - Add incremental type checking capabilities

75. **Create Tests for Scalability**
    - Test type checking on large codebases
    - Test memory usage under load
    - Test incremental compilation performance

## 8. Documentation and Examples

76. **Create Documentation Tests**
    - Test that examples in documentation are valid
    - Test edge cases mentioned in documentation
    - Test tutorial examples work correctly
    - Test API documentation examples

77. **Update Documentation**
    - Document type system implementation
    - Provide examples of type error messages
    - Document performance characteristics and limitations
    - Create comprehensive API documentation

78. **Create Example Programs**
    - Develop comprehensive example programs
    - Include common patterns and idioms
    - Add examples of advanced type features
    - Create educational progression of examples

79. **Implement Tutorial and Learning Materials**
    - Create step-by-step type system tutorial
    - Add interactive examples and exercises
    - Document best practices and patterns
    - Include troubleshooting guides
