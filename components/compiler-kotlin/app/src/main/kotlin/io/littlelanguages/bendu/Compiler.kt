package io.littlelanguages.bendu

import io.littlelanguages.bendu.compiler.ByteBuilder
import io.littlelanguages.bendu.compiler.Instructions
import io.littlelanguages.bendu.typeinference.*

fun compile(script: List<Expression>, errors: Errors): ByteArray {
    val compiler = Compiler(errors)
    compiler.compile(script)

    return compiler.byteBuilder.toByteArray()
}

private sealed class NatureOfBinding {
    open fun patch(byteBuilder: ByteBuilder) {}
}

private data class PackageBinding(val offset: Int) : NatureOfBinding()
private data class ParameterBinding(val offset: Int) : NatureOfBinding()
private data class PackageFunction(var offset: Int = 0, val patches: MutableList<Int> = mutableListOf()) :
    NatureOfBinding() {
    fun addPatch(patch: Int) =
        patches.add(patch)

    override fun patch(byteBuilder: ByteBuilder) {
        patches.forEach { byteBuilder.writeIntAtPosition(it, offset) }
    }
}

private class Compiler(val errors: Errors) {
    val byteBuilder = ByteBuilder()
    val bindings = mutableMapOf<String, NatureOfBinding>()
    var offset = 0

    fun compile(script: List<Expression>) {
        if (errors.hasNoErrors()) {
            compileStatements(script)

            bindings.forEach { _, binding -> binding.patch(byteBuilder) }
        }
    }

    private fun compileStatements(statements: List<Expression>) {
        val numberOfStatements = statements.size

        if (numberOfStatements == 0) {
            byteBuilder.appendInstruction(Instructions.PUSH_UNIT_LITERAL)
        } else {
            statements.forEachIndexed { index, statement ->
                val keepResult = index == numberOfStatements - 1

                compileExpression(statement, keepResult)
            }
        }
    }

    private fun compileExpression(expression: Expression, keepResult: Boolean = true) {
        when (expression) {
            is AbortStatement -> compileAbortExpression(expression, keepResult)
            is ApplyExpression -> compileApplyExpression(expression, keepResult)
            is BinaryExpression -> compileBinaryExpression(expression, keepResult)
            is IfExpression -> compileIfExpression(expression, keepResult)
            is LetStatement -> compileLetExpression(expression, keepResult)
            is LiteralBoolExpression -> compileLiteralBoolExpression(expression, keepResult)
            is LiteralCharExpression -> compileLiteralCharExpression(expression, keepResult)
            is LiteralFloatExpression -> compileLiteralFloatExpression(expression, keepResult)
            is LiteralIntExpression -> compileLiteralIntExpression(expression, keepResult)
            is LiteralStringExpression -> compileLiteralStringExpression(expression, keepResult)
            is LiteralUnitExpression -> compileLiteralUnitExpression(expression, keepResult)
            is LowerIDExpression -> compileLowerIDExpression(expression, keepResult)
            is PrintStatement -> compilePrintExpression(expression, keepResult)
            is PrintlnStatement -> compilePrintlnExpression(expression, keepResult)
            is UnaryExpression -> compileUnaryExpression(expression, keepResult)

            else -> TODO(expression.toString())
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
            val binding = bindings[expression.f.v.value] as PackageFunction
            expression.arguments.forEach { e ->
                compileExpression(e)
            }
            byteBuilder.appendInstruction(Instructions.CALL_LOCAL)
            binding.addPatch(byteBuilder.size())
            byteBuilder.appendInt(0)
        } else {
            TODO("Not implemented yet")
        }

        if (!keepResult) {
            byteBuilder.appendInstruction(Instructions.DISCARD)
        }
    }

    private fun compileBinaryExpression(expression: BinaryExpression, keepResult: Boolean) {
        if (expression.op.op == Op.And) {
            compileExpression(expression.e1)
            byteBuilder.appendInstruction(Instructions.JMP_DUP_FALSE)
            val jmpFalse = byteBuilder.size()
            byteBuilder.appendInt(0)
            byteBuilder.appendInstruction(Instructions.DISCARD)
            compileExpression(expression.e2)
            byteBuilder.writeIntAtPosition(jmpFalse, byteBuilder.size())
        } else if (expression.op.op == Op.Or) {
            compileExpression(expression.e1)
            byteBuilder.appendInstruction(Instructions.JMP_DUP_TRUE)
            val jmpFalse = byteBuilder.size()
            byteBuilder.appendInt(0)
            byteBuilder.appendInstruction(Instructions.DISCARD)
            compileExpression(expression.e2)
            byteBuilder.writeIntAtPosition(jmpFalse, byteBuilder.size())
        } else {
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

        if (!keepResult) {
            byteBuilder.appendInstruction(Instructions.DISCARD)
        }
    }

    private fun compileIfExpression(expression: IfExpression, keepResult: Boolean) {
        var guardJump: Int? = null
        val endJumpOffsets = mutableListOf<Int>()

        expression.guards.forEach { guard ->
            if (guardJump != null) {
                byteBuilder.writeIntAtPosition(guardJump, byteBuilder.size())
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
            byteBuilder.writeIntAtPosition(guardJump, byteBuilder.size())
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
        if (e.e is LiteralFunctionExpression) {
            byteBuilder.appendInstruction(Instructions.JMP)
            val jumpOffset = byteBuilder.size()
            byteBuilder.appendInt(0)

            e.e.parameters.forEachIndexed { index, parameter ->
                bindings.put(parameter.value, ParameterBinding(index - (e.e.parameters.size - 1)))
            }

            bindings.put(e.id.value, PackageFunction(byteBuilder.size()))

            compileExpression(e.e.body)
            byteBuilder.appendInstruction(Instructions.RET)
            byteBuilder.appendInt(e.e.parameters.size)

            byteBuilder.writeIntAtPosition(jumpOffset, byteBuilder.size())
        } else {
            compileExpression(e.e)
            bindings.put(e.id.value, PackageBinding(offset))
            offset += 1

            if (keepResult) {
                byteBuilder.appendInstruction(Instructions.PUSH_UNIT_LITERAL)
            }
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
            when (val binding = bindings[expression.v.value]) {
                null -> throw IllegalArgumentException("${expression.v.value} referenced at ${expression.v.location} not found")

                is PackageBinding -> {
                    byteBuilder.appendInstruction(Instructions.PUSH_STACK)
                    byteBuilder.appendInt(binding.offset)
                }

                is ParameterBinding -> {
                    byteBuilder.appendInstruction(Instructions.PUSH_PARAMETER)
                    byteBuilder.appendInt(binding.offset)
                }

                is PackageFunction ->
                    TODO("Not implemented yet")
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

            if (e.type!!.isBool())
                byteBuilder.appendInstruction(Instructions.PRINT_BOOL)
            else if (e.type!!.isChar())
                byteBuilder.appendInstruction(Instructions.PRINT_U8)
            else if (e.type!!.isFloat())
                byteBuilder.appendInstruction(Instructions.PRINT_F32)
            else if (e.type!!.isInt())
                byteBuilder.appendInstruction(Instructions.PRINT_I32)
            else if (e.type!!.isString())
                byteBuilder.appendInstruction(Instructions.PRINT_STRING)
            else if (e.type!!.isUnit())
                byteBuilder.appendInstruction(Instructions.PRINT_UNIT)
            else
                errors.addError(UnificationError(e.type!!, setOf(typeBool, typeChar, typeFloat, typeInt, typeString)))
        }
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



