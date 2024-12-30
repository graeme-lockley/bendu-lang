package io.littlelanguages.bendu

import io.littlelanguages.bendu.cache.*
import io.littlelanguages.bendu.compiler.*
import io.littlelanguages.bendu.typeinference.*

class CompiledScript(
    val dependencies: List<ScriptDependency>,
    val exports: ScriptExports,
    val imports: List<String>,
    val bytecode: ByteArray
)

fun compile(entry: CacheEntry, script: Script, errors: Errors): CompiledScript {
    val compiler = Compiler(errors)
    compiler.compile(entry, script)

    return CompiledScript(
        compiler.dependencies.toList(),
        ScriptExports(compiler.exports),
        compiler.imports,
        compiler.byteBuilder.toByteArray()
    )
}

private class Compiler(val errors: Errors) {
    val dependencies = mutableSetOf<ScriptDependency>()
    val imports = mutableListOf<String>()
    val exports = mutableListOf<ScriptExport>()
    val byteBuilder = ByteBuilder()
    val symbolTable = SymbolTable(byteBuilder)

    fun compile(entry: CacheEntry, script: Script) {
        if (errors.hasNoErrors()) {
            script.imports.forEachIndexed { index, import ->
                import.entry!!.includeDependencies(dependencies)
                imports.add(import.entry!!.byteCodeFileName())

                when (import) {
                    is ImportAll -> {
                        import.entry!!.declarations.forEach {
                            val packageID = -index - 1

                            when (it) {
                                is FunctionExport ->
                                    symbolTable.bindFunctionExport(it.name, packageID, it.codeOffset, it.frameOffset)

                                is ValueExport ->
                                    symbolTable.bindIdentifierExport(it.name, packageID, it.frameOffset)
                            }
                        }
                    }

                    is ImportList -> {
                        import.ids.forEach { id ->
                            val importEntry = import.entry!![id.id.value]
                            val aliasName = id.alias?.value ?: importEntry?.name
                            val packageID = -index - 1

                            when (importEntry) {
                                is FunctionExport ->
                                    symbolTable.bindFunctionExport(
                                        aliasName!!,
                                        packageID,
                                        importEntry.codeOffset,
                                        importEntry.frameOffset
                                    )

                                is ValueExport ->
                                    symbolTable.bindIdentifierExport(aliasName!!, packageID, importEntry.frameOffset)

                                null -> TODO("Internal Error: importEntry is null")
                            }
                        }
                    }
                }
            }

            dependencies.add(ScriptDependency.from(entry))

            compileDeclarations(script.decs)

            symbolTable.closeScope()
        }
    }

    private fun compileDeclarations(declarations: List<Declaration>) {
        val lastStatementIdx = declarations.dropLastWhile { it !is DeclarationExpression }.size - 1

        declarations.forEachIndexed { index, declaration ->
            when (declaration) {
                is DeclarationExpression -> {
                    val keepExpressionResult = index == lastStatementIdx

                    compileExpression(declaration.e, keepExpressionResult)

                    if (declaration.e is LetStatement) {
                        val typeEnv = TypeEnv(emptyMap())

                        declaration.e.terms.forEach { t ->
                            if (t is LetFunctionStatementTerm) {
                                val functionBinding = symbolTable.find(t.id.value) as FunctionBinding

                                if (t.exported) {
                                    exports.add(
                                        FunctionExport(
                                            t.id.value,
                                            t.mutable,
                                            typeEnv.generalise(t.type!!),
                                            functionBinding.codeOffset,
                                            functionBinding.frameOffset
                                        )
                                    )
                                }
                            } else if (t is LetValueStatementTerm) {
                                val identifierBinding = symbolTable.find(t.id.value) as IdentifierBinding

                                if (t.exported) {
                                    exports.add(
                                        ValueExport(
                                            t.id.value,
                                            t.mutable,
                                            typeEnv.generalise(t.type!!),
                                            identifierBinding.frameOffset
                                        )
                                    )
                                }
                            }
                        }

                    }
                }

                is DeclarationType -> TODO()
            }
        }

    }

    private fun compileStatements(statements: List<Expression>, keepResult: Boolean) {
        val numberOfStatements = statements.size

        if (numberOfStatements == 0) {
            byteBuilder.appendInstruction(Instructions.PUSH_UNIT_LITERAL)
        } else {
            statements.forEachIndexed { index, statement ->
                val keepExpressionResult = keepResult && index == numberOfStatements - 1

                compileExpression(statement, keepExpressionResult)
            }
        }
    }

    private fun compileExpression(expression: Expression, keepResult: Boolean = true) {
        when (expression) {
            is AbortStatement -> compileAbortExpression(expression, keepResult)
            is ApplyExpression -> compileApplyExpression(expression, keepResult)
            is ArrayElementProjectionExpression -> compileArrayElementProjectionExpression(expression, keepResult)
            is ArrayRangeProjectionExpression -> compileArrayRangeProjectionExpression(expression, keepResult)
            is AssignmentExpression -> compileAssignmentExpression(expression, keepResult)
            is BinaryExpression -> compileBinaryExpression(expression, keepResult)
            is BlockExpression -> compileStatements(expression.es, keepResult)
            is IfExpression -> compileIfExpression(expression, keepResult)
            is LetStatement -> compileLetExpression(expression, keepResult)
            is LiteralArrayExpression -> compileLiteralArrayExpression(expression, keepResult)
            is LiteralBoolExpression -> compileLiteralBoolExpression(expression, keepResult)
            is LiteralCharExpression -> compileLiteralCharExpression(expression, keepResult)
            is LiteralFloatExpression -> compileLiteralFloatExpression(expression, keepResult)
            is LiteralFunctionExpression -> compileLiteralFunctionExpression(expression, keepResult)
            is LiteralIntExpression -> compileLiteralIntExpression(expression, keepResult)
            is LiteralStringExpression -> compileLiteralStringExpression(expression, keepResult)
            is LiteralTupleExpression -> compileLiteralTupleExpression(expression, keepResult)
            is LiteralUnitExpression -> compileLiteralUnitExpression(expression, keepResult)
            is LowerIDExpression -> compileLowerIDExpression(expression, keepResult)
            is ModuleReferenceExpression -> compileModuleReferenceExpression(expression, keepResult)
            is PrintStatement -> compilePrintExpression(expression, keepResult)
            is PrintlnStatement -> compilePrintlnExpression(expression, keepResult)
            is TypedExpression -> compileExpression(expression.e, keepResult)
            is UnaryExpression -> compileUnaryExpression(expression, keepResult)
            is WhileExpression -> compileWhileExpression(expression, keepResult)
        }
    }

    private fun compileAbortExpression(e: AbortStatement, keepResult: Boolean) {
        compilePrintExpressions(e.es)
        byteBuilder.appendInstruction(Instructions.PRINTLN)
        byteBuilder.appendInstruction(Instructions.ABORT)
        byteBuilder.appendInt(1)

        if (keepResult) {
            byteBuilder.appendInstruction(Instructions.PUSH_UNIT_LITERAL)
        }
    }

    private fun compileApplyExpression(expression: ApplyExpression, keepResult: Boolean) {
        if (expression.f is LowerIDExpression) {
            val (binding, depth) = symbolTable.findIndexed(expression.f.v.value)!!

            if (binding is FunctionBinding && binding.frameOffset == null) {
                expression.arguments.forEach { e ->
                    compileExpression(e)
                }
                byteBuilder.appendInstruction(Instructions.CALL)
                binding.addPatch(byteBuilder.size())
                byteBuilder.appendInt(0)
                byteBuilder.appendInt(expression.arguments.size)
                byteBuilder.appendInt(depth)

                if (!keepResult) {
                    byteBuilder.appendInstruction(Instructions.DISCARD)
                }

                return
            } else if (binding is ImportedFunctionBinding && binding.frameOffset == null) {
                expression.arguments.forEach { e ->
                    compileExpression(e)
                }
                byteBuilder.appendInstruction(Instructions.CALL_PACKAGE)
                byteBuilder.appendInt(binding.packageID)
                byteBuilder.appendInt(binding.codeOffset)
                byteBuilder.appendInt(expression.arguments.size)

                if (!keepResult) {
                    byteBuilder.appendInstruction(Instructions.DISCARD)
                }

                return
            }
        } else if (expression.f is ModuleReferenceExpression && expression.f.declaration?.mutable != true) {
            val b = expression.f.declaration!! as FunctionExport

            expression.arguments.forEach { e ->
                compileExpression(e)
            }
            byteBuilder.appendInstruction(Instructions.CALL_PACKAGE)
            byteBuilder.appendInt(-(expression.f.importID ?: 0) - 1)
            byteBuilder.appendInt(b.codeOffset)
            byteBuilder.appendInt(expression.arguments.size)

            return
        }

        compileExpression(expression.f)
        expression.arguments.forEach { e ->
            compileExpression(e)
        }
        byteBuilder.appendInstruction(Instructions.CALL_CLOSURE)
        byteBuilder.appendInt(expression.arguments.size)

        if (!keepResult) {
            byteBuilder.appendInstruction(Instructions.DISCARD)
        }
    }

    private fun compileArrayElementProjectionExpression(
        expression: ArrayElementProjectionExpression,
        keepResult: Boolean
    ) {
        compileExpression(expression.array)
        compileExpression(expression.index)

        byteBuilder.appendInstruction(Instructions.PUSH_ARRAY_ELEMENT)

        if (!keepResult) {
            byteBuilder.appendInstruction(Instructions.DISCARD)
        }
    }

    private fun compileArrayRangeProjectionExpression(expression: ArrayRangeProjectionExpression, keepResult: Boolean) {
        compileExpression(expression.array)

        if (expression.start != null) {
            compileExpression(expression.start)
        }
        if (expression.end != null) {
            compileExpression(expression.end)
        }

        when {
            expression.start != null && expression.end != null -> byteBuilder.appendInstruction(Instructions.PUSH_ARRAY_RANGE)
            expression.start != null -> byteBuilder.appendInstruction(Instructions.PUSH_ARRAY_RANGE_FROM)
            expression.end != null -> byteBuilder.appendInstruction(Instructions.PUSH_ARRAY_RANGE_TO)
            else -> {
                byteBuilder.appendInstruction(Instructions.PUSH_I32_LITERAL)
                byteBuilder.appendInt(0)
                byteBuilder.appendInstruction(Instructions.PUSH_ARRAY_RANGE_FROM)
            }
        }

        if (!keepResult) {
            byteBuilder.appendInstruction(Instructions.DISCARD)
        }
    }

    private fun compileAssignmentExpression(expression: AssignmentExpression, keepResult: Boolean) {
        if (expression.lhs is LowerIDExpression) {
            val symbol = symbolTable.findIndexed(expression.lhs.v.value)!!
            val (binding, depth) = symbol

            compileExpression(expression.rhs)
            if (keepResult) {
                byteBuilder.appendInstruction(Instructions.DUP)
            }

            when (binding) {
                is FunctionBinding -> {
                    byteBuilder.appendInstruction(Instructions.STORE)
                    byteBuilder.appendInt(depth)
                    byteBuilder.appendInt(binding.frameOffset!!)
                }

                is ImportedFunctionBinding -> {
                    byteBuilder.appendInstruction(Instructions.STORE_PACKAGE)
                    byteBuilder.appendInt(binding.packageID)
                    byteBuilder.appendInt(binding.frameOffset!!)
                }

                is ImportedIdentifierBinding -> {
                    byteBuilder.appendInstruction(Instructions.STORE_PACKAGE)
                    byteBuilder.appendInt(binding.packageID)
                    byteBuilder.appendInt(binding.frameOffset)
                }

                is IdentifierBinding -> {
                    byteBuilder.appendInstruction(Instructions.STORE)
                    byteBuilder.appendInt(depth)
                    byteBuilder.appendInt(binding.frameOffset)
                }
            }
        } else if (expression.lhs is ArrayElementProjectionExpression) {
            compileExpression(expression.lhs.array)
            compileExpression(expression.lhs.index)
            compileExpression(expression.rhs)

            byteBuilder.appendInstruction(Instructions.STORE_ARRAY_ELEMENT)

            if (!keepResult) {
                byteBuilder.appendInstruction(Instructions.DISCARD)
            }
        } else if (expression.lhs is ArrayRangeProjectionExpression) {
            compileExpression(expression.lhs.array)

            if (expression.lhs.start != null) {
                compileExpression(expression.lhs.start)
            }
            if (expression.lhs.end != null) {
                compileExpression(expression.lhs.end)
            }
            if (expression.lhs.start == null && expression.lhs.end == null) {
                byteBuilder.appendInstruction(Instructions.PUSH_I32_LITERAL)
                byteBuilder.appendInt(0)
            }

            compileExpression(expression.rhs)

            when {
                expression.lhs.start != null && expression.lhs.end != null -> byteBuilder.appendInstruction(Instructions.STORE_ARRAY_RANGE)
                expression.lhs.end != null -> byteBuilder.appendInstruction(Instructions.STORE_ARRAY_RANGE_TO)
                else -> byteBuilder.appendInstruction(Instructions.STORE_ARRAY_RANGE_FROM)
            }

            if (!keepResult) {
                byteBuilder.appendInstruction(Instructions.DISCARD)
            }
        } else if (expression.lhs is ModuleReferenceExpression) {
            compileExpression(expression.rhs)
            if (keepResult) {
                byteBuilder.appendInstruction(Instructions.DUP)
            }

            byteBuilder.appendInstruction(Instructions.STORE_PACKAGE)
            byteBuilder.appendInt(-(expression.lhs.importID ?: 0) - 1)
            when (val declaration = expression.lhs.declaration) {
                is FunctionExport -> byteBuilder.appendInt(declaration.frameOffset ?: 0)
                is ValueExport -> byteBuilder.appendInt(declaration.frameOffset)
                null -> {}
            }
        } else {
            errors.addError(AssignmentError(expression.lhs.location()))
        }
    }

    private fun compileBinaryExpression(expression: BinaryExpression, keepResult: Boolean) {
        when (expression.op.op) {
            Op.And -> {
                compileExpression(expression.e1)
                byteBuilder.appendInstruction(Instructions.JMP_DUP_FALSE)
                val jmpFalse = byteBuilder.size()
                byteBuilder.appendInt(0)
                byteBuilder.appendInstruction(Instructions.DISCARD)
                compileExpression(expression.e2)
                byteBuilder.writeIntAtPosition(jmpFalse, byteBuilder.size())
            }

            Op.Or -> {
                compileExpression(expression.e1)
                byteBuilder.appendInstruction(Instructions.JMP_DUP_TRUE)
                val jmpFalse = byteBuilder.size()
                byteBuilder.appendInt(0)
                byteBuilder.appendInstruction(Instructions.DISCARD)
                compileExpression(expression.e2)
                byteBuilder.writeIntAtPosition(jmpFalse, byteBuilder.size())
            }

            Op.GreaterGreater -> {
                compileExpression(expression.e1)
                compileExpression(expression.e2)
                byteBuilder.appendInstruction(Instructions.ARRAY_PREPEND_ELEMENT_DUPLICATE)
            }

            Op.GreaterBang -> {
                compileExpression(expression.e1)
                compileExpression(expression.e2)
                byteBuilder.appendInstruction(Instructions.ARRAY_PREPEND_ELEMENT)
            }

            Op.LessLess -> {
                compileExpression(expression.e1)
                compileExpression(expression.e2)
                byteBuilder.appendInstruction(Instructions.ARRAY_APPEND_ELEMENT_DUPLICATE)
            }

            Op.LessBang -> {
                compileExpression(expression.e1)
                compileExpression(expression.e2)
                byteBuilder.appendInstruction(Instructions.ARRAY_APPEND_ELEMENT)
            }

            else -> {
                compileExpression(expression.e1)
                compileExpression(expression.e2)

                if (expression.e1.type!!.isBool()) {
                    when (expression.op.op) {
                        Op.EqualEqual -> byteBuilder.appendInstruction(Instructions.EQ_BOOL)
                        Op.NotEqual -> byteBuilder.appendInstruction(Instructions.NEQ_BOOL)
                        else -> errors.addError(
                            OperatorOperandTypeError(
                                expression.op.op,
                                expression.e1.type!!,
                                validBinaryOperatorArguments[expression.op.op]!!,
                                expression.e1.location()
                            )
                        )
                    }
                } else if (expression.e1.type!!.isChar()) {
                    when (expression.op.op) {
                        Op.Plus -> byteBuilder.appendInstruction(Instructions.ADD_U8)
                        Op.Minus -> byteBuilder.appendInstruction(Instructions.SUB_U8)
                        Op.Multiply -> byteBuilder.appendInstruction(Instructions.MUL_U8)
                        Op.Divide -> byteBuilder.appendInstruction(Instructions.DIV_U8)
                        Op.EqualEqual -> byteBuilder.appendInstruction(Instructions.EQ_U8)
                        Op.NotEqual -> byteBuilder.appendInstruction(Instructions.NEQ_U8)
                        Op.LessThan -> byteBuilder.appendInstruction(Instructions.LT_U8)
                        Op.LessEqual -> byteBuilder.appendInstruction(Instructions.LE_U8)
                        Op.GreaterThan -> byteBuilder.appendInstruction(Instructions.GT_U8)
                        Op.GreaterEqual -> byteBuilder.appendInstruction(Instructions.GE_U8)
                        else -> errors.addError(
                            OperatorOperandTypeError(
                                expression.op.op,
                                expression.e1.type!!,
                                validBinaryOperatorArguments[expression.op.op]!!,
                                expression.e1.location()
                            )
                        )
                    }
                } else if (expression.e1.type!!.isFloat()) {
                    when (expression.op.op) {
                        Op.Plus -> byteBuilder.appendInstruction(Instructions.ADD_F32)
                        Op.Minus -> byteBuilder.appendInstruction(Instructions.SUB_F32)
                        Op.Multiply -> byteBuilder.appendInstruction(Instructions.MUL_F32)
                        Op.Divide -> byteBuilder.appendInstruction(Instructions.DIV_F32)
                        Op.Power -> byteBuilder.appendInstruction(Instructions.POW_F32)
                        Op.EqualEqual -> byteBuilder.appendInstruction(Instructions.EQ_F32)
                        Op.NotEqual -> byteBuilder.appendInstruction(Instructions.NEQ_F32)
                        Op.LessThan -> byteBuilder.appendInstruction(Instructions.LT_F32)
                        Op.LessEqual -> byteBuilder.appendInstruction(Instructions.LE_F32)
                        Op.GreaterThan -> byteBuilder.appendInstruction(Instructions.GT_F32)
                        Op.GreaterEqual -> byteBuilder.appendInstruction(Instructions.GE_F32)
                        else -> errors.addError(
                            OperatorOperandTypeError(
                                expression.op.op,
                                expression.e1.type!!,
                                validBinaryOperatorArguments[expression.op.op]!!,
                                expression.e1.location()
                            )
                        )
                    }
                } else if (expression.e1.type!!.isInt()) {
                    when (expression.op.op) {
                        Op.Plus -> byteBuilder.appendInstruction(Instructions.ADD_I32)
                        Op.Minus -> byteBuilder.appendInstruction(Instructions.SUB_I32)
                        Op.Multiply -> byteBuilder.appendInstruction(Instructions.MUL_I32)
                        Op.Divide -> byteBuilder.appendInstruction(Instructions.DIV_I32)
                        Op.Modulo -> byteBuilder.appendInstruction(Instructions.MOD_I32)
                        Op.Power -> byteBuilder.appendInstruction(Instructions.POW_I32)
                        Op.EqualEqual -> byteBuilder.appendInstruction(Instructions.EQ_I32)
                        Op.NotEqual -> byteBuilder.appendInstruction(Instructions.NEQ_I32)
                        Op.LessThan -> byteBuilder.appendInstruction(Instructions.LT_I32)
                        Op.LessEqual -> byteBuilder.appendInstruction(Instructions.LE_I32)
                        Op.GreaterThan -> byteBuilder.appendInstruction(Instructions.GT_I32)
                        Op.GreaterEqual -> byteBuilder.appendInstruction(Instructions.GE_I32)
                        else -> errors.addError(
                            OperatorOperandTypeError(
                                expression.op.op,
                                expression.e1.type!!,
                                validBinaryOperatorArguments[expression.op.op]!!,
                                expression.e1.location()
                            )
                        )
                    }
                } else if (expression.e1.type!!.isString()) {
                    when (expression.op.op) {
                        Op.Plus -> byteBuilder.appendInstruction(Instructions.ADD_STRING)
                        Op.EqualEqual -> byteBuilder.appendInstruction(Instructions.EQ_STRING)
                        Op.NotEqual -> byteBuilder.appendInstruction(Instructions.NEQ_STRING)
                        Op.LessThan -> byteBuilder.appendInstruction(Instructions.LT_STRING)
                        Op.LessEqual -> byteBuilder.appendInstruction(Instructions.LE_STRING)
                        Op.GreaterThan -> byteBuilder.appendInstruction(Instructions.GT_STRING)
                        Op.GreaterEqual -> byteBuilder.appendInstruction(Instructions.GE_STRING)
                        else -> errors.addError(
                            OperatorOperandTypeError(
                                expression.op.op,
                                expression.e1.type!!,
                                validBinaryOperatorArguments[expression.op.op]!!,
                                expression.e1.location()
                            )
                        )
                    }
                } else if (expression.e1.type!!.isUnit()) {
                    when (expression.op.op) {
                        Op.EqualEqual -> byteBuilder.appendInstruction(Instructions.EQ_UNIT)
                        Op.NotEqual -> byteBuilder.appendInstruction(Instructions.NEQ_UNIT)
                        Op.LessThan -> byteBuilder.appendInstruction(Instructions.NEQ_UNIT)
                        Op.LessEqual -> byteBuilder.appendInstruction(Instructions.EQ_UNIT)
                        Op.GreaterThan -> byteBuilder.appendInstruction(Instructions.NEQ_UNIT)
                        Op.GreaterEqual -> byteBuilder.appendInstruction(Instructions.EQ_UNIT)
                        else -> errors.addError(
                            OperatorOperandTypeError(
                                expression.op.op,
                                expression.e1.type!!,
                                validBinaryOperatorArguments[expression.op.op]!!,
                                expression.e1.location()
                            )
                        )
                    }
                } else if (expression.op.op == Op.EqualEqual) {
                    byteBuilder.appendInstruction(Instructions.EQ)
                } else if (expression.op.op == Op.NotEqual) {
                    byteBuilder.appendInstruction(Instructions.NEQ)
                } else {
                    errors.addError(
                        OperatorOperandTypeError(
                            expression.op.op,
                            expression.e1.type!!,
                            validBinaryOperatorArguments[expression.op.op]!!,
                            expression.e1.location()
                        )
                    )
                }
            }
        }

        if (!keepResult) {
            byteBuilder.appendInstruction(Instructions.DISCARD)
        }
    }

    private fun compileIfExpression(expression: IfExpression, keepResult: Boolean) {
        var guardJump: Int? = null
        val endJumpOffsets = mutableListOf<Int>()

        expression.guards.forEach { guard ->
            if (guardJump != null) {
                byteBuilder.writeIntAtPosition(guardJump!!, byteBuilder.size())
            }
            compileExpression(guard.first)
            byteBuilder.appendInstruction(Instructions.JMP_FALSE)
            guardJump = byteBuilder.size()
            byteBuilder.appendInt(0)

            compileExpression(guard.second)
            byteBuilder.appendInstruction(Instructions.JMP)
            endJumpOffsets.add(byteBuilder.size())
            byteBuilder.appendInt(0)
        }

        if (guardJump != null) {
            byteBuilder.writeIntAtPosition(guardJump!!, byteBuilder.size())
        }
        if (expression.elseBranch != null) {
            compileExpression(expression.elseBranch)
        } else {
            byteBuilder.appendInstruction(Instructions.PUSH_UNIT_LITERAL)
        }
        endJumpOffsets.forEach { offset ->
            byteBuilder.writeIntAtPosition(offset, byteBuilder.size())
        }

        if (!keepResult) {
            byteBuilder.appendInstruction(Instructions.DISCARD)
        }
    }

    private fun compileLetExpression(e: LetStatement, keepResult: Boolean) {
        e.terms.forEach { t ->
            if (t is LetFunctionStatementTerm) {
                symbolTable.bindFunction(t.id.value, t.mutable)
            }
        }
        e.terms.forEach { t -> compileLetStatementTerm(t, keepResult) }
    }

    private fun compileLetStatementTerm(e: LetStatementTerm, keepResult: Boolean) {
        when (e) {
            is LetValueStatementTerm -> {
                compileExpression(e.e)
                val binding = symbolTable.bindIdentifier(e.id.value)
                byteBuilder.appendInstruction(Instructions.STORE)
                byteBuilder.appendInt(0)
                byteBuilder.appendInt(binding.frameOffset)
            }

            is LetFunctionStatementTerm -> {
                byteBuilder.appendInstruction(Instructions.JMP)
                val jumpOffset = byteBuilder.size()
                byteBuilder.appendInt(0)

                symbolTable.openScope()

                val binding = symbolTable.find(e.id.value)!! as FunctionBinding
                binding.codeOffset = byteBuilder.size()

                compileFunctionParameters(e.parameters)

                compileExpression(e.body)
                byteBuilder.appendInstruction(Instructions.RET)

                byteBuilder.writeIntAtPosition(jumpOffset, byteBuilder.size())

                if (e.mutable) {
                    byteBuilder.appendInstruction(Instructions.PUSH_CLOSURE)
                    byteBuilder.appendInt(binding.codeOffset)
                    byteBuilder.appendInt(0)

                    byteBuilder.appendInstruction(Instructions.STORE)
                    byteBuilder.appendInt(0)
                    byteBuilder.appendInt(binding.frameOffset!!)
                }

                symbolTable.closeScope()
            }
        }

        if (keepResult) {
            byteBuilder.appendInstruction(Instructions.PUSH_UNIT_LITERAL)
        }
    }

    private fun compileFunctionParameters(parameters: List<FunctionParameter>) {
        parameters.forEachIndexed { index, parameter ->
            when (parameter) {
                is LowerIDFunctionParameter ->
                    symbolTable.bindIdentifier(parameter.value)

                is TupleFunctionParameter ->
                    symbolTable.bindIdentifier("[${index}]") // need to do this to get the index right

                is WildcardFunctionParameter ->
                    symbolTable.bindIdentifier("_") // need to do this to get the index right
            }
        }

        parameters.forEachIndexed { index, parameter ->
            if (parameter is TupleFunctionParameter) {
                val binding = symbolTable.find("[${index}]") as IdentifierBinding

                byteBuilder.appendInstruction(Instructions.LOAD)
                byteBuilder.appendInt(0)
                byteBuilder.appendInt(binding.frameOffset)

                compileFunctionTupleParameters(parameter.parameters)
            }
        }
    }

    private fun compileFunctionTupleParameters(parameters: List<FunctionParameter>) {
        parameters.forEachIndexed { index, parameter ->
            if (index < parameters.size - 1) {
                byteBuilder.appendInstruction(Instructions.DUP)
            }

            when (parameter) {
                is LowerIDFunctionParameter -> {
                    val binding = symbolTable.bindIdentifier(parameter.value)
                    byteBuilder.appendInstruction(Instructions.PUSH_TUPLE_COMPONENT)
                    byteBuilder.appendInt(index)
                    byteBuilder.appendInstruction(Instructions.STORE)
                    byteBuilder.appendInt(0)
                    byteBuilder.appendInt(binding.frameOffset)
                }

                is TupleFunctionParameter -> {
                    symbolTable.bindIdentifier("_") // need to do this to get the index right

                    byteBuilder.appendInstruction(Instructions.PUSH_TUPLE_COMPONENT)
                    byteBuilder.appendInt(index)

                    compileFunctionTupleParameters(parameter.parameters)
                }

                is WildcardFunctionParameter -> {
                    symbolTable.bindIdentifier("_") // need to do this to get the index right
                    byteBuilder.appendInstruction(Instructions.DISCARD)
                }
            }
        }
    }

    private fun compileLiteralArrayExpression(expression: LiteralArrayExpression, keepResult: Boolean) {
        var foundAppendArray = false

        expression.es.forEachIndexed { i, e ->
            if (e.second && !foundAppendArray) {
                byteBuilder.appendInstruction(Instructions.PUSH_ARRAY)
                byteBuilder.appendInt(i)
                foundAppendArray = true
            }
            compileExpression(e.first)

            if (foundAppendArray) {
                if (e.second) {
                    byteBuilder.appendInstruction(Instructions.ARRAY_APPEND_ARRAY)
                } else {
                    byteBuilder.appendInstruction(Instructions.ARRAY_APPEND_ELEMENT)
                }
            }
        }
        if (!foundAppendArray) {
            byteBuilder.appendInstruction(Instructions.PUSH_ARRAY)
            byteBuilder.appendInt(expression.es.size)
        }

        if (!keepResult) {
            byteBuilder.appendInstruction(Instructions.DISCARD)
        }
    }

    private fun compileLiteralBoolExpression(expression: LiteralBoolExpression, keepResult: Boolean) {
        if (keepResult) {
            if (expression.v.value)
                byteBuilder.appendInstruction(Instructions.PUSH_BOOL_TRUE)
            else
                byteBuilder.appendInstruction(Instructions.PUSH_BOOL_FALSE)
        }
    }

    private fun compileLiteralCharExpression(expression: LiteralCharExpression, keepResult: Boolean) {
        if (keepResult) {
            byteBuilder.appendInstruction(Instructions.PUSH_U8_LITERAL)
            byteBuilder.appendChar(expression.v.value.code)
        }
    }

    private fun compileLiteralFloatExpression(expression: LiteralFloatExpression, keepResult: Boolean) {
        if (keepResult) {
            byteBuilder.appendInstruction(Instructions.PUSH_F32_LITERAL)
            byteBuilder.appendFloat(expression.v.value)
        }
    }

    private fun compileLiteralFunctionExpression(expression: LiteralFunctionExpression, keepResult: Boolean) {
        if (keepResult) {
            byteBuilder.appendInstruction(Instructions.PUSH_CLOSURE)
            val jumpOffset = byteBuilder.size()
            byteBuilder.appendInt(0)
            byteBuilder.appendInt(0)

            byteBuilder.appendInstruction(Instructions.JMP)
            val jumpOffset2 = byteBuilder.size()
            byteBuilder.appendInt(0)

            symbolTable.openScope()

            byteBuilder.writeIntAtPosition(jumpOffset, byteBuilder.size())

            compileFunctionParameters(expression.parameters)

            compileExpression(expression.body)
            byteBuilder.appendInstruction(Instructions.RET)

            symbolTable.closeScope()

            byteBuilder.writeIntAtPosition(jumpOffset2, byteBuilder.size())
        }
    }

    private fun compileLiteralIntExpression(expression: LiteralIntExpression, keepResult: Boolean) {
        if (keepResult) {
            byteBuilder.appendInstruction(Instructions.PUSH_I32_LITERAL)
            byteBuilder.appendInt(expression.v.value)
        }
    }

    private fun compileLiteralStringExpression(expression: LiteralStringExpression, keepResult: Boolean) {
        if (keepResult) {
            byteBuilder.appendInstruction(Instructions.PUSH_STRING_LITERAL)
            byteBuilder.appendInt(expression.v.value.length)
            byteBuilder.append(expression.v.value.toByteArray())
        }
    }

    private fun compileLiteralTupleExpression(expression: LiteralTupleExpression, keepResult: Boolean) {
        expression.es.forEach { e ->
            compileExpression(e)
        }
        byteBuilder.appendInstruction(Instructions.PUSH_TUPLE)
        byteBuilder.appendInt(expression.es.size)

        if (!keepResult) {
            byteBuilder.appendInstruction(Instructions.DISCARD)
        }
    }

    private fun compileLiteralUnitExpression(
        @Suppress("UNUSED_PARAMETER") expression: LiteralUnitExpression,
        keepResult: Boolean
    ) {
        if (keepResult) {
            byteBuilder.appendInstruction(Instructions.PUSH_UNIT_LITERAL)
        }
    }

    private fun compileLowerIDExpression(expression: LowerIDExpression, keepResult: Boolean) {
        if (keepResult) {
            val symbol = symbolTable.findIndexed(expression.v.value)
            if (symbol == null) {
                throw IllegalArgumentException("${expression.v.value} referenced at ${expression.v.location} not found")
            } else {
                val (binding, depth) = symbol

                when (binding) {
                    is FunctionBinding -> {
                        if (binding.frameOffset == null) {
                            byteBuilder.appendInstruction(Instructions.PUSH_CLOSURE)
                            binding.addPatch(byteBuilder.size())
                            byteBuilder.appendInt(0)
                            byteBuilder.appendInt(depth)
                        } else {
                            byteBuilder.appendInstruction(Instructions.LOAD)
                            byteBuilder.appendInt(depth)
                            byteBuilder.appendInt(binding.frameOffset)
                        }
                    }

                    is ImportedFunctionBinding -> {
                        if (binding.frameOffset == null) {
                            byteBuilder.appendInstruction(Instructions.PUSH_PACKAGE_CLOSURE)
                            byteBuilder.appendInt(binding.packageID)
                            byteBuilder.appendInt(binding.codeOffset)
                        } else {
                            byteBuilder.appendInstruction(Instructions.LOAD_PACKAGE)
                            byteBuilder.appendInt(binding.packageID)
                            byteBuilder.appendInt(binding.frameOffset)
                        }
                    }

                    is ImportedIdentifierBinding -> {
                        byteBuilder.appendInstruction(Instructions.LOAD_PACKAGE)
                        byteBuilder.appendInt(binding.packageID)
                        byteBuilder.appendInt(binding.frameOffset)
                    }

                    is IdentifierBinding -> {
                        byteBuilder.appendInstruction(Instructions.LOAD)
                        byteBuilder.appendInt(depth)
                        byteBuilder.appendInt(binding.frameOffset)
                    }
                }
            }
        }
    }

    private fun compileModuleReferenceExpression(expression: ModuleReferenceExpression, keepResult: Boolean) {
        if (keepResult) {
            when (val declaration = expression.declaration) {
                null -> {}

                is FunctionExport -> {
                    if (declaration.mutable) {
                        byteBuilder.appendInstruction(Instructions.LOAD_PACKAGE)
                        byteBuilder.appendInt(-(expression.importID ?: 0) - 1)
                        byteBuilder.appendInt(declaration.frameOffset ?: 0)
                    } else {
                        byteBuilder.appendInstruction(Instructions.PUSH_PACKAGE_CLOSURE)
                        byteBuilder.appendInt(-(expression.importID ?: 0) - 1)
                        byteBuilder.appendInt(declaration.codeOffset)
                    }
                }

                is ValueExport -> {
                    byteBuilder.appendInstruction(Instructions.LOAD_PACKAGE)
                    byteBuilder.appendInt(-(expression.importID ?: 0) - 1)
                    byteBuilder.appendInt(declaration.frameOffset)
                }
            }
        }
    }

    private fun compilePrintExpression(e: PrintStatement, keepResult: Boolean) {
        compilePrintExpressions(e.es)

        if (keepResult) {
            byteBuilder.appendInstruction(Instructions.PUSH_UNIT_LITERAL)
        }
    }

    private fun compilePrintlnExpression(e: PrintlnStatement, keepResult: Boolean) {
        compilePrintExpressions(e.es)
        byteBuilder.appendInstruction(Instructions.PRINTLN)

        if (keepResult) {
            byteBuilder.appendInstruction(Instructions.PUSH_UNIT_LITERAL)
        }
    }

    private fun compilePrintExpressions(es: List<Expression>) {
        es.forEach { e ->
            compileExpression(e)

            printValue(e.type!!)
        }
    }

    private fun printValue(type: Type) {
        if (type.isBool())
            byteBuilder.appendInstruction(Instructions.PRINT_BOOL)
        else if (type.isChar())
            byteBuilder.appendInstruction(Instructions.PRINT_U8)
        else if (type.isFloat())
            byteBuilder.appendInstruction(Instructions.PRINT_F32)
        else if (type.isInt())
            byteBuilder.appendInstruction(Instructions.PRINT_I32)
        else if (type.isString())
            byteBuilder.appendInstruction(Instructions.PRINT_STRING)
        else if (type.isUnit())
            byteBuilder.appendInstruction(Instructions.PRINT_UNIT)
        else
            byteBuilder.appendInstruction(Instructions.PRINT)
    }

    private fun compileUnaryExpression(expression: UnaryExpression, keepResult: Boolean) {
        when (expression.op.op) {
            UnaryOp.Not -> {
                compileExpression(expression.e)
                byteBuilder.appendInstruction(Instructions.NOT_BOOL)

                if (!keepResult) {
                    byteBuilder.appendInstruction(Instructions.DISCARD)
                }
            }

            UnaryOp.TypeOf ->
                if (keepResult) {
                    val s = expression.e.type!!.toString()
                    byteBuilder.appendInstruction(Instructions.PUSH_STRING_LITERAL)
                    byteBuilder.appendInt(s.length)
                    byteBuilder.append(s.toByteArray())
                }
        }
    }

    private fun compileWhileExpression(expression: WhileExpression, keepResult: Boolean) {
        val start = byteBuilder.size()

        compileExpression(expression.guard)
        byteBuilder.appendInstruction(Instructions.JMP_FALSE)
        val end = byteBuilder.size()
        byteBuilder.appendInt(0)

        compileExpression(expression.body, false)
        byteBuilder.appendInstruction(Instructions.JMP)
        byteBuilder.appendInt(start)

        byteBuilder.writeIntAtPosition(end, byteBuilder.size())

        if (keepResult) {
            byteBuilder.appendInstruction(Instructions.PUSH_UNIT_LITERAL)
        }
    }
}

private val validBinaryOperatorArguments = mapOf(
    Pair(Op.EqualEqual, setOf(typeBool, typeChar, typeFloat, typeInt, typeString, typeUnit)),
    Pair(Op.NotEqual, setOf(typeBool, typeChar, typeFloat, typeInt, typeString, typeUnit)),
    Pair(Op.LessThan, setOf(typeChar, typeFloat, typeInt, typeString, typeUnit)),
    Pair(Op.LessEqual, setOf(typeChar, typeFloat, typeInt, typeString, typeUnit)),
    Pair(Op.GreaterThan, setOf(typeChar, typeFloat, typeInt, typeString, typeUnit)),
    Pair(Op.GreaterEqual, setOf(typeChar, typeFloat, typeInt, typeString, typeUnit)),
    Pair(Op.Plus, setOf(typeChar, typeFloat, typeInt, typeString)),
    Pair(Op.Minus, setOf(typeChar, typeFloat, typeInt)),
    Pair(Op.Multiply, setOf(typeChar, typeFloat, typeInt)),
    Pair(Op.Divide, setOf(typeChar, typeFloat, typeInt)),
    Pair(Op.Power, setOf(typeChar, typeFloat, typeInt)),
    Pair(Op.Modulo, setOf(typeInt)),
)
