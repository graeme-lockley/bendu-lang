# TODO: Extract mini-bendu Type System into Standalone Module

**Phase**: 1 - Weeks 1-2  
**Complexity**: HIGH  
**Risk**: MEDIUM  
**Estimated Duration**: 2 weeks  

## Overview
Extract the advanced type system from mini-bendu and create a standalone module within the compiler-kotlin project. This involves copying core type classes, adapting interfaces, and creating compatibility layers for gradual migration.

## Detailed Task List

### 1. Set up Advanced Types Module Structure

**Task**: Create the module structure and build configuration for the advanced type system.

**Details**: 
- Create new Gradle submodule `advanced-types` in compiler-kotlin project
- Set up proper dependencies and module isolation
- Configure build scripts for the new module

**AI Execution Prompt**:
```
Create a new Gradle submodule called 'advanced-types' in the compiler-kotlin project:

1. Create directory structure:
   - components/compiler-kotlin/advanced-types/
   - components/compiler-kotlin/advanced-types/src/main/kotlin/io/littlelanguages/bendu/types/
   - components/compiler-kotlin/advanced-types/src/test/kotlin/io/littlelanguages/bendu/types/
   - components/compiler-kotlin/advanced-types/build.gradle.kts

2. Create build.gradle.kts with minimal dependencies:
   - Kotlin stdlib
   - JUnit for testing
   - No dependencies on main compiler or mini-bendu initially

3. Update settings.gradle.kts in compiler-kotlin root to include the new module

4. Create initial package structure with placeholder files
```

**Acceptance Criteria**:
- [ ] Module builds successfully in isolation
- [ ] Can run tests (even if empty)
- [ ] No circular dependencies with main compiler module

---

### 2. Extract Core Type Hierarchy Classes

**Task**: Copy and adapt the fundamental type system classes from mini-bendu.

**Details**:
- Extract the sealed `Type` class and all its implementations
- Remove dependencies on mini-bendu specific AST and parser classes
- Adapt method signatures to be self-contained

**AI Execution Prompt**:
```
Extract the core type hierarchy from mini-bendu into the advanced-types module:

1. Copy these classes from components/mini-bendu/app/src/main/kotlin/io/littlelanguages/minibendu/typesystem/Type.kt:
   - sealed class Type (with all abstract methods)
   - class PrimitiveType
   - class LiteralStringType
   - class FunctionType
   - class TupleType
   - class RecordType
   - class UnionType
   - class IntersectionType
   - class TypeAlias
   - class TypeVariable
   - object Types (with predefined types)

2. Remove all dependencies on:
   - mini-bendu AST classes
   - mini-bendu parser classes
   - Any mini-bendu specific imports

3. Ensure all methods are self-contained and don't reference external types

4. Create new file: advanced-types/src/main/kotlin/io/littlelanguages/bendu/types/Type.kt

5. Add proper package declaration: package io.littlelanguages.bendu.types

6. Ensure the code compiles without errors in the new module
```

**Acceptance Criteria**:
- [ ] All core type classes compile without errors
- [ ] No external dependencies beyond Kotlin stdlib
- [ ] All abstract methods have implementations
- [ ] Types object provides all primitive types

---

### 3. Extract Type Variable Management System

**Task**: Create a standalone type variable management system adapted from mini-bendu.

**Details**:
- Extract TypeVariable class with proper ID generation
- Create TypeVariableFactory for consistent variable creation
- Implement proper scoping and naming conventions

**AI Execution Prompt**:
```
Create a standalone type variable management system:

1. Extract TypeVariable implementation from mini-bendu, ensuring:
   - Unique ID generation (thread-safe)
   - Proper toString() implementation
   - Equality and hashCode methods
   - Name generation for display purposes

2. Create TypeVariableFactory class with methods:
   - fresh(): TypeVariable (creates new variable with unique ID)
   - freshWithName(name: String): TypeVariable
   - reset(): Unit (resets ID counter for testing)

3. Create file: advanced-types/src/main/kotlin/io/littlelanguages/bendu/types/TypeVariable.kt

4. Ensure thread-safety for concurrent type checking

5. Add comprehensive unit tests for:
   - Unique ID generation
   - Name handling
   - Equality semantics
   - Thread safety
```

**Acceptance Criteria**:
- [ ] TypeVariable generates unique IDs consistently
- [ ] Factory methods work correctly
- [ ] Thread-safe implementation
- [ ] Comprehensive test coverage (>90%)

---

### 4. Extract Advanced Type Utilities

**Task**: Copy and adapt the utility classes for union, intersection, and record type operations.

**Details**:
- Extract UnionTypeUtils, IntersectionTypeUtils, RecordTypeUtils
- Adapt methods to work with the new type hierarchy
- Ensure all utility methods are pure functions without side effects

**AI Execution Prompt**:
```
Extract and adapt advanced type utilities from mini-bendu:

1. Copy these utility classes, removing mini-bendu dependencies:
   - UnionTypeUtils from UnionTypeUtils.kt
   - IntersectionTypeUtils from IntersectionTypeUtils.kt
   - Create RecordTypeUtils for record-specific operations

2. Adapt all methods to use the new Type hierarchy

3. Ensure methods are pure functions (no side effects)

4. Key methods to include:
   - UnionTypeUtils: create(), simplify(), intersect(), difference()
   - IntersectionTypeUtils: create(), simplify(), isCompatibleWith()
   - RecordTypeUtils: isSubtypeOf(), canUnify(), extractRowVariable()

5. Create file: advanced-types/src/main/kotlin/io/littlelanguages/bendu/types/TypeUtils.kt

6. Add comprehensive unit tests for all utility methods

7. Ensure all operations handle edge cases (empty sets, null values, etc.)
```

**Acceptance Criteria**:
- [ ] All utility methods compile and work correctly
- [ ] No side effects in utility functions
- [ ] Edge cases properly handled
- [ ] Unit tests cover all methods with >95% coverage

---

### 5. Create Type Environment Abstraction

**Task**: Design and implement an abstraction layer for type environments that can work with both systems.

**Details**:
- Create interfaces that abstract environment operations
- Design adapter pattern to bridge mini-bendu and compiler-kotlin environments
- Ensure the abstraction is flexible enough for future changes

**AI Execution Prompt**:
```
Create a type environment abstraction layer:

1. Design interface: TypeEnvironment with methods:
   - bindType(name: String, type: Type): Unit
   - lookupType(name: String): Type?
   - bindTypeVariable(name: String, variable: TypeVariable): Unit
   - lookupTypeVariable(name: String): TypeVariable?
   - openScope(): Unit
   - closeScope(): Unit

2. Create interface: TypeConstraintSolver with methods:
   - addConstraint(type1: Type, type2: Type): Unit
   - solve(): ConstraintSolution
   - hasErrors(): Boolean
   - getErrors(): List<TypeError>

3. Create data classes:
   - ConstraintSolution(substitution: Substitution, errors: List<TypeError>)
   - TypeError(message: String, location: SourceLocation?)
   - Substitution(mapping: Map<TypeVariable, Type>)

4. Create file: advanced-types/src/main/kotlin/io/littlelanguages/bendu/types/Environment.kt

5. Create adapter implementations:
   - CompilerKotlinEnvironmentAdapter
   - MiniBenduEnvironmentAdapter

6. Ensure the abstraction doesn't leak implementation details
```

**Acceptance Criteria**:
- [ ] Clean interface design with no implementation leakage
- [ ] Adapter pattern properly implemented
- [ ] Can be used by both systems without modification
- [ ] Proper error handling and type safety

---

### 6. Extract Unification Algorithm

**Task**: Extract and adapt the unification algorithm from mini-bendu for the new type system.

**Details**:
- Copy the unification logic and adapt it to the new type hierarchy
- Ensure proper handling of all type combinations
- Implement occurs check and error reporting

**AI Execution Prompt**:
```
Extract the unification algorithm from mini-bendu:

1. Copy unification logic from components/mini-bendu/app/src/main/kotlin/io/littlelanguages/minibendu/typesystem/Unification.kt

2. Adapt to work with new Type hierarchy:
   - Update all type pattern matching
   - Remove dependencies on mini-bendu specific classes
   - Ensure all type combinations are handled

3. Key components to include:
   - unify(type1: Type, type2: Type): UnificationResult
   - occursCheck(variable: TypeVariable, type: Type): Boolean
   - unifyRecords(record1: RecordType, record2: RecordType): UnificationResult
   - unifyUnions(union1: UnionType, union2: UnionType): UnificationResult
   - unifyIntersections(intersection1: IntersectionType, intersection2: IntersectionType): UnificationResult

4. Create sealed class UnificationResult:
   - Success(substitution: Substitution)
   - Failure(error: String, location: SourceLocation?)

5. Create file: advanced-types/src/main/kotlin/io/littlelanguages/bendu/types/Unification.kt

6. Add extensive unit tests covering:
   - All type combinations
   - Occurs check scenarios
   - Complex nested structures
   - Error cases
```

**Acceptance Criteria**:
- [ ] Unification handles all type combinations correctly
- [ ] Occurs check prevents infinite types
- [ ] Comprehensive error reporting
- [ ] Test coverage >95% including edge cases

---

### 7. Create Substitution System

**Task**: Implement the type substitution system for applying unification results.

**Details**:
- Extract substitution logic from mini-bendu
- Ensure proper composition and application of substitutions
- Handle complex nested type structures correctly

**AI Execution Prompt**:
```
Create the type substitution system:

1. Extract Substitution class from mini-bendu with methods:
   - apply(type: Type): Type (applies substitution to a type)
   - compose(other: Substitution): Substitution
   - get(variable: TypeVariable): Type?
   - isEmpty(): Boolean
   - domain(): Set<TypeVariable>
   - range(): Set<Type>

2. Implement substitution for all type variants:
   - TypeVariable: direct substitution or identity
   - FunctionType: apply to domain and codomain
   - TupleType: apply to all elements
   - RecordType: apply to all field types and row variable
   - UnionType: apply to all alternatives
   - IntersectionType: apply to all members
   - TypeAlias: apply to type arguments
   - PrimitiveType/LiteralStringType: identity

3. Create companion object with factory methods:
   - empty: Substitution (empty substitution)
   - single(variable: TypeVariable, type: Type): Substitution
   - fromMap(mapping: Map<TypeVariable, Type>): Substitution

4. Create file: advanced-types/src/main/kotlin/io/littlelanguages/bendu/types/Substitution.kt

5. Add unit tests covering:
   - Substitution application to all type variants
   - Composition properties (associativity, identity)
   - Complex nested structures
   - Edge cases (empty substitutions, cycles)
```

**Acceptance Criteria**:
- [ ] Substitution applies correctly to all type variants
- [ ] Composition properties hold (mathematical correctness)
- [ ] Handles complex nested structures
- [ ] Test coverage >95%

---

### 8. Create Feature Flag System

**Task**: Implement a feature flag system to control which advanced type features are enabled.

**Details**:
- Design flexible feature flag system
- Allow runtime toggling of features
- Provide clear interfaces for checking feature status

**AI Execution Prompt**:
```
Create a feature flag system for gradual type system migration:

1. Create object TypeSystemFeatures with properties:
   - ADVANCED_TYPES_ENABLED: Boolean
   - RECORDS_ENABLED: Boolean
   - ROW_POLYMORPHISM_ENABLED: Boolean
   - UNION_TYPES_ENABLED: Boolean
   - INTERSECTION_TYPES_ENABLED: Boolean
   - LITERAL_STRING_TYPES_ENABLED: Boolean

2. Implement flag resolution from:
   - System properties (-Dbendu.type.records=true)
   - Environment variables (BENDU_TYPE_RECORDS=true)
   - Configuration files (if present)
   - Default values (all false initially)

3. Create interface FeatureFlag with:
   - isEnabled(): Boolean
   - description(): String
   - dependsOn(): List<FeatureFlag>

4. Implement dependency checking (e.g., row polymorphism requires records)

5. Create file: advanced-types/src/main/kotlin/io/littlelanguages/bendu/types/FeatureFlags.kt

6. Add utility methods:
   - checkFeatureEnabled(feature: FeatureFlag): Unit (throws if disabled)
   - whenFeatureEnabled(feature: FeatureFlag, block: () -> T): T?

7. Create comprehensive tests for flag resolution and dependency checking
```

**Acceptance Criteria**:
- [ ] Flags can be set via multiple methods (properties, env vars)
- [ ] Dependency checking works correctly
- [ ] Clear error messages when features are disabled
- [ ] Test coverage for all flag resolution paths

---

### 9. Create Compatibility Layer

**Task**: Design and implement adapters to bridge between the old and new type systems.

**Details**:
- Create bidirectional conversion between type representations
- Ensure no information loss during conversion
- Handle cases where advanced features aren't available in old system

**AI Execution Prompt**:
```
Create compatibility layer between old and new type systems:

1. Create interface TypeSystemBridge with methods:
   - convertToAdvanced(oldType: io.littlelanguages.bendu.typeinference.Type): io.littlelanguages.bendu.types.Type
   - convertFromAdvanced(newType: io.littlelanguages.bendu.types.Type): io.littlelanguages.bendu.typeinference.Type
   - isConvertible(type: Type): Boolean

2. Implement conversion for supported types:
   - Primitive types (Int, String, Bool, Unit, Char, Float)
   - Function types (TArr -> FunctionType)
   - Tuple types (TTuple -> TupleType)
   - Type constructors (TCon -> TypeAlias for simple cases)
   - Type variables (TVar -> TypeVariable)

3. Handle advanced features gracefully:
   - Records: Convert to TCon with special name
   - Unions: Convert to supertype or throw if impossible
   - Intersections: Convert to one member or throw
   - Literal strings: Convert to String type

4. Create error types for unsupported conversions:
   - UnsupportedTypeConversion(type: Type, reason: String)

5. Create file: advanced-types/src/main/kotlin/io/littlelanguages/bendu/types/Compatibility.kt

6. Add comprehensive tests for:
   - Bidirectional conversion roundtrips
   - Error cases for unsupported features
   - Edge cases (empty tuples, complex nested types)
```

**Acceptance Criteria**:
- [ ] Bidirectional conversion works for supported types
- [ ] Clear errors for unsupported conversions
- [ ] No information loss for convertible types
- [ ] Comprehensive test coverage for all conversion paths

---

### 10. Extract Error System Foundation

**Task**: Extract and adapt the structured error system from mini-bendu for type errors.

**Details**:
- Copy the CompilerError hierarchy focusing on type errors
- Adapt error classes to work with new type system
- Ensure error messages are clear and actionable

**AI Execution Prompt**:
```
Extract structured error system for type errors:

1. Copy error hierarchy from mini-bendu STRUCTURED_ERROR_SYSTEM.md, focusing on:
   - sealed class TypeError
   - class TypeMismatch(expected: Type, actual: Type, context: String?)
   - class UndefinedVariable(variableName: String)
   - class UndefinedTypeVariable(variableName: String)
   - class OccursCheckFailure(variable: TypeVariable, type: Type)
   - class UnificationFailure(type1: Type, type2: Type, reason: String)

2. Adapt to new type system:
   - Update type references to use new hierarchy
   - Remove dependencies on mini-bendu AST classes
   - Add SourceLocation for error positioning

3. Create interface TypeErrorReporter:
   - report(error: TypeError): Unit
   - hasErrors(): Boolean
   - getErrors(): List<TypeError>
   - clear(): Unit

4. Implement error formatting:
   - Human-readable error messages
   - Type information in errors
   - Suggestions for common mistakes

5. Create file: advanced-types/src/main/kotlin/io/littlelanguages/bendu/types/Errors.kt

6. Add unit tests for:
   - Error message formatting
   - Error reporter functionality
   - Error hierarchy completeness
```

**Acceptance Criteria**:
- [ ] Error hierarchy covers all type error cases
- [ ] Clear, actionable error messages
- [ ] Proper source location tracking
- [ ] Error reporter works correctly

---

### 11. Create Basic Integration Tests

**Task**: Implement integration tests to verify the extracted type system works correctly.

**Details**:
- Test type checking of simple programs using the new system
- Verify unification and substitution work together correctly
- Test error reporting and recovery

**AI Execution Prompt**:
```
Create integration tests for the extracted type system:

1. Create test class TypeSystemIntegrationTest with tests for:
   - Basic type inference (let x = 5, infer x: Int)
   - Function type inference (let f = (x) => x + 1)
   - Simple unification (f(5) where f: (Int) -> Int)
   - Error cases (type mismatches, undefined variables)

2. Create helper methods:
   - typeCheck(expression: String): TypeCheckResult
   - assertTypeEquals(expected: Type, actual: Type)
   - assertTypeError(expression: String, errorType: Class<out TypeError>)

3. Test scenarios:
   - Primitive type inference
   - Function application and composition
   - Variable binding and lookup
   - Type variable unification
   - Error reporting and recovery

4. Create file: advanced-types/src/test/kotlin/io/littlelanguages/bendu/types/IntegrationTest.kt

5. Use property-based testing where appropriate:
   - Generate random type expressions
   - Test unification properties (symmetry, transitivity)
   - Test substitution composition properties

6. Ensure all tests pass and provide good coverage of type system functionality
```

**Acceptance Criteria**:
- [ ] All integration tests pass
- [ ] Tests cover major type system functionality
- [ ] Property-based tests verify mathematical properties
- [ ] Clear test failure messages

---

### 12. Create Migration Documentation

**Task**: Document the extracted type system and how to use it for migration.

**Details**:
- Document the API for the new type system
- Provide migration guide from old to new system
- Include examples and best practices

**AI Execution Prompt**:
```
Create comprehensive documentation for the extracted type system:

1. Create API documentation file: advanced-types/README.md with sections:
   - Overview of the type system
   - Core classes and their purposes
   - Usage examples for common operations
   - Feature flag configuration
   - Migration guide from old system

2. Document key classes:
   - Type hierarchy and when to use each type
   - TypeEnvironment interface and implementations
   - Unification algorithm usage
   - Substitution system
   - Error handling patterns

3. Create examples:
   - Basic type checking workflow
   - Adding new type variants
   - Using feature flags
   - Converting between old and new systems

4. Create migration checklist:
   - Steps to integrate with existing compiler
   - Testing recommendations
   - Performance considerations
   - Common pitfalls and solutions

5. Add code examples for all major APIs

6. Include troubleshooting section for common issues

7. Document the compatibility layer and its limitations
```

**Acceptance Criteria**:
- [ ] Complete API documentation with examples
- [ ] Clear migration guide with step-by-step instructions
- [ ] Troubleshooting section covers common issues
- [ ] Code examples are tested and work correctly

---

### 13. Performance Baseline Testing

**Task**: Establish performance baselines for the new type system to track regressions.

**Details**:
- Create benchmarks for type checking operations
- Measure memory usage and CPU time
- Compare with mini-bendu performance where applicable

**AI Execution Prompt**:
```
Create performance baseline tests for the extracted type system:

1. Create benchmark test class TypeSystemBenchmark with tests for:
   - Type unification performance (various type combinations)
   - Substitution application performance
   - Type environment lookup performance
   - Large type expression handling

2. Use JMH (Java Microbenchmark Harness) for accurate measurements:
   - Configure proper warmup and measurement iterations
   - Measure both throughput and latency
   - Track memory allocation

3. Create test scenarios:
   - Small programs (10-50 type operations)
   - Medium programs (100-500 type operations)
   - Large programs (1000+ type operations)
   - Deeply nested types (10+ levels)
   - Wide types (100+ fields/alternatives)

4. Create file: advanced-types/src/test/kotlin/io/littlelanguages/bendu/types/Benchmarks.kt

5. Set up CI integration:
   - Run benchmarks on every major change
   - Track performance trends over time
   - Alert on significant regressions (>20% slowdown)

6. Document baseline numbers:
   - Typical operation costs
   - Memory usage patterns
   - Performance characteristics

7. Compare with mini-bendu where possible to ensure no major regressions
```

**Acceptance Criteria**:
- [ ] Benchmarks run consistently and produce reliable results
- [ ] Baseline numbers documented for future comparison
- [ ] CI integration working for performance tracking
- [ ] Performance characteristics understood and documented

---

### 14. Final Integration Validation

**Task**: Validate that the extracted type system can be successfully integrated with the compiler-kotlin project.

**Details**:
- Test integration with existing compiler components
- Verify feature flags work correctly
- Ensure compatibility layer functions properly

**AI Execution Prompt**:
```
Perform final validation of the extracted type system:

1. Create integration test that uses the new type system with compiler-kotlin:
   - Import advanced-types module in main compiler
   - Test basic type checking using new system
   - Verify feature flags control behavior correctly
   - Test compatibility layer conversion

2. Test scenarios:
   - Type check simple Bendu programs using new system
   - Convert types between old and new systems
   - Toggle features on/off and verify behavior
   - Error reporting flows correctly to compiler

3. Create validation checklist:
   - All extracted components compile without errors
   - No circular dependencies between modules
   - Feature flags work as expected
   - Compatibility layer handles all supported conversions
   - Error messages are properly formatted
   - Performance is acceptable (within 2x of targets)

4. Create file: advanced-types/VALIDATION.md documenting:
   - Integration test results
   - Known limitations and workarounds
   - Next steps for full migration
   - Recommendations for Phase 2

5. Run full test suite and ensure all tests pass

6. Verify module can be published and consumed by other modules

7. Document any issues found and their solutions
```

**Acceptance Criteria**:
- [ ] Integration with compiler-kotlin works correctly
- [ ] All validation tests pass
- [ ] Performance meets acceptable thresholds
- [ ] Documentation is complete and accurate
- [ ] Ready for Phase 2 implementation

---

## Summary

This task list provides a detailed roadmap for extracting the mini-bendu type system into a standalone module. Each task includes specific deliverables, acceptance criteria, and detailed prompts that can be executed by AI agents.

**Key Dependencies**:
- Tasks 1-4 should be completed first (module setup and core extraction)
- Tasks 5-8 can be done in parallel after core extraction
- Tasks 9-11 depend on earlier tasks being complete
- Tasks 12-14 are final validation and documentation

**Risk Mitigation**:
- Each task has clear acceptance criteria
- Integration testing is performed throughout
- Performance is monitored from the beginning
- Documentation is created alongside implementation

**Success Metrics**:
- All tasks completed with acceptance criteria met
- New module compiles and tests pass
- Performance within acceptable bounds
- Clear path to Phase 2 (record type integration) 