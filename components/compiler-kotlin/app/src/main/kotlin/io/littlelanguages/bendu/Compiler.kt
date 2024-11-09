package io.littlelanguages.bendu

import io.littlelanguages.bendu.compiler.ByteBuilder
import io.littlelanguages.bendu.compiler.Instructions
import io.littlelanguages.bendu.typeinference.typeBool
import io.littlelanguages.bendu.typeinference.typeChar
import io.littlelanguages.bendu.typeinference.typeFloat
import io.littlelanguages.bendu.typeinference.typeInt
import io.littlelanguages.bendu.typeinference.typeString
import java.lang.IllegalArgumentException

fun compile(script: List<Statement>, errors: Errors): ByteArray {
    val compiler = Compiler(errors)
    compiler.compile(script)

    return compiler.byteBuilder.toByteArray()
}

private class Compiler(val errors: Errors) {
    val byteBuilder = ByteBuilder()
    val bindings = mutableMapOf<String, Int>()
    var offset = 0

    fun compile(script: List<Statement>) {
        if (errors.hasNoErrors()) {
            compileStatements(script)
        }
    }

    private fun compileStatements(statements: List<Statement>) {
        statements.forEach { statement ->
            when (statement) {
                is LetStatement -> {
                    compileExpression(statement.e)
                    bindings.put(statement.id.value, offset)
                    offset += 1
                }

                is PrintStatement -> {
                    compilePrintExpressions(statement.es)
                }

                is PrintlnStatement -> {
                    compilePrintExpressions(statement.es)
                    byteBuilder.appendInstruction(Instructions.PRINTLN)
                }

                is ExpressionStatement -> {
                    compileExpression(statement.e)
                }
            }
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
            else
                errors.addError(UnificationError(e.type!!, setOf(typeBool, typeChar, typeFloat, typeInt, typeString)))
        }
    }

    private fun compileExpression(expression: Expression) {
        when (expression) {
            is BinaryExpression -> {
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
                                    setOf(typeBool),
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
                                    setOf(typeChar, typeFloat, typeInt),
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
                                    setOf(typeChar, typeFloat, typeInt),
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
                                    setOf(typeChar, typeFloat, typeInt),
                                    expression.e1.location()
                                )
                            )
                        }
                    } else {
                        errors.addError(
                            OperatorOperandTypeError(
                                expression.op.op,
                                expression.e1.type!!,
                                setOf(typeChar, typeFloat, typeInt),
                                expression.e1.location()
                            )
                        )
                    }
                }
            }

            is LiteralBoolExpression -> {
                if (expression.v.value)
                    byteBuilder.appendInstruction(Instructions.PUSH_BOOL_TRUE)
                else
                    byteBuilder.appendInstruction(Instructions.PUSH_BOOL_FALSE)
            }

            is LiteralCharExpression -> {
                byteBuilder.appendInstruction(Instructions.PUSH_U8_LITERAL)
                byteBuilder.appendChar(expression.v.value.code)
            }

            is LiteralFloatExpression -> {
                byteBuilder.appendInstruction(Instructions.PUSH_F32_LITERAL)
                byteBuilder.appendFloat(expression.v.value)
            }

            is LiteralIntExpression -> {
                byteBuilder.appendInstruction(Instructions.PUSH_I32_LITERAL)
                byteBuilder.appendInt(expression.v.value)
            }

            is LiteralStringExpression -> {
                byteBuilder.appendInstruction(Instructions.PUSH_STRING_LITERAL)
                byteBuilder.appendInt(expression.v.value.length)
                byteBuilder.append(expression.v.value.toByteArray())
            }

            is LowerIDExpression -> {
                val offset = bindings[expression.v.value]
                    ?: throw IllegalArgumentException("${expression.v.value} referenced at ${expression.v.location} not found")

                byteBuilder.appendInstruction(Instructions.PUSH_STACK)
                byteBuilder.appendInt(offset)
            }

            is UnaryExpression -> {
                compileExpression(expression.e)
                byteBuilder.appendInstruction(Instructions.NOT_BOOL)
            }

            else -> TODO()
        }
    }
}


