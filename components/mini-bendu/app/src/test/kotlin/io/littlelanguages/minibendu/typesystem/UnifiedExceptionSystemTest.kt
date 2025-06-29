package io.littlelanguages.minibendu.typesystem

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Test the unified exception system that replaces ConstraintGenerationException, UnificationException, etc.
 * with a single CompilerErrorException that preserves structured error information.
 */
class UnifiedExceptionSystemTest {

    @Test
    fun `CompilerErrorException preserves structured error information`() {
        // Create a structured error
        val undefinedVarError = TypeError.UndefinedVariable("myVar")
        val exception = CompilerErrorException(undefinedVarError)
        
        // Verify the structured error is preserved
        assertEquals(undefinedVarError, exception.compilerError)
        assertEquals("Undefined variable: myVar", exception.message)
        assertEquals(ErrorCategory.TYPE, exception.compilerError.getCategory())
        assertEquals(CompilerErrorSeverity.ERROR, exception.compilerError.getSeverity())
    }

    @Test
    fun `Factory methods create appropriate structured errors`() {
        // Test undefined variable factory
        val undefinedVarException = CompilerErrorException.undefinedVariable("testVar")
        assertTrue(undefinedVarException.compilerError is TypeError.UndefinedVariable)
        assertEquals("testVar", (undefinedVarException.compilerError as TypeError.UndefinedVariable).variableName)
        
        // Test unification failure factory
        val unificationException = CompilerErrorException.unificationFailure("Cannot unify Int with String")
        assertTrue(unificationException.compilerError is TypeError.TypeMismatch)
        assertEquals("Cannot unify Int with String", (unificationException.compilerError as TypeError.TypeMismatch).context!!)
        
        // Test non-exhaustive pattern factory
        val patternException = CompilerErrorException.nonExhaustivePattern(listOf("None", "Some(_)"))
        assertTrue(patternException.compilerError is TypeError.NonExhaustivePatternMatch)
        assertEquals(listOf("None", "Some(_)"), (patternException.compilerError as TypeError.NonExhaustivePatternMatch).missingPatterns)
        
        // Test syntax error factory
        val syntaxException = CompilerErrorException.syntaxError("Unexpected token", "line 5")
        assertTrue(syntaxException.compilerError is SyntaxError.GenericSyntaxError)
        assertEquals("Unexpected token", (syntaxException.compilerError as SyntaxError.GenericSyntaxError).errorMessage)
        assertEquals("line 5", (syntaxException.compilerError as SyntaxError.GenericSyntaxError).position!!)
    }

    @Test
    fun `Backward compatibility with string messages`() {
        // String constructor should create a CompilerBug
        val stringException = CompilerErrorException("Some legacy error message")
        assertTrue(stringException.compilerError is InternalError.CompilerBug)
        assertEquals("Some legacy error message", (stringException.compilerError as InternalError.CompilerBug).errorMessage)
        assertEquals("Internal compiler error: Some legacy error message", stringException.message)
    }

    @Test
    fun `Exception message flows correctly`() {
        // Test that Exception.message matches CompilerError.getMessage()
        val typeMismatch = TypeError.TypeMismatch(Types.Int, Types.String, "arithmetic operation")
        val exception = CompilerErrorException(typeMismatch)
        
        assertEquals(typeMismatch.getMessage(), exception.message)
        assertTrue(exception.message?.contains("Cannot unify") == true)
        assertTrue(exception.message?.contains("arithmetic operation") == true)
    }

    @Test
    fun `Different error categories are handled correctly`() {
        // Syntax error
        val syntaxError = SyntaxError.UnexpectedToken("}", "identifier")
        val syntaxException = CompilerErrorException(syntaxError)
        assertEquals(ErrorCategory.SYNTAX, syntaxException.compilerError.getCategory())
        
        // Type error
        val typeError = TypeError.UndefinedVariable("x")
        val typeException = CompilerErrorException(typeError)
        assertEquals(ErrorCategory.TYPE, typeException.compilerError.getCategory())
        
        // Semantic error
        val semanticError = SemanticError.DuplicateDefinition("foo", "function")
        val semanticException = CompilerErrorException(semanticError)
        assertEquals(ErrorCategory.SEMANTIC, semanticException.compilerError.getCategory())
        
        // Internal error
        val internalError = InternalError.UnimplementedFeature("generic types")
        val internalException = CompilerErrorException(internalError)
        assertEquals(ErrorCategory.INTERNAL, internalException.compilerError.getCategory())
    }

    @Test
    fun `Warnings are handled correctly`() {
        val warning = CompilerWarning.UnusedVariable("temp")
        val warningException = CompilerErrorException(warning)
        
        assertEquals(CompilerErrorSeverity.WARNING, warningException.compilerError.getSeverity())
        assertEquals("Unused variable: temp", warningException.message)
    }

    @Test
    fun `Complex error types preserve all information`() {
        // Test function application error with complex types
        val functionType = FunctionType(Types.Int, Types.String)
        val argumentTypes = listOf(Types.Bool, Types.String)
        val functionError = TypeError.FunctionApplicationError(
            functionType, 
            argumentTypes, 
            "Wrong number of arguments"
        )
        val exception = CompilerErrorException(functionError)
        
        assertTrue(exception.compilerError is TypeError.FunctionApplicationError)
        val error = exception.compilerError as TypeError.FunctionApplicationError
        assertEquals(functionType, error.functionType)
        assertEquals(argumentTypes, error.argumentTypes)
        assertEquals("Wrong number of arguments", error.details!!)
    }
} 