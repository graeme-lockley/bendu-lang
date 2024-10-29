package io.littlelanguages.bendu

import io.littlelanguages.bendu.compiler.ByteBuilder
import io.littlelanguages.bendu.compiler.Instructions
import java.lang.IllegalArgumentException

fun compile(script: List<Statement>): ByteArray {
    val compiler = Compiler()
    compiler.compile(script)

    return compiler.byteBuilder.toByteArray()
}

private class Compiler {
    val byteBuilder = ByteBuilder()
    val bindings = mutableMapOf<String, Int>()
    var offset = 0

    fun compile(script: List<Statement>) {
        compileStatements(script)
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
                    statement.es.forEach { e ->
                        compileExpression(e)
                        byteBuilder.appendInstruction(Instructions.PRINT_I32)
                    }
                }

                is PrintlnStatement -> {
                    statement.es.forEach { e ->
                        compileExpression(e)
                        byteBuilder.appendInstruction(Instructions.PRINT_I32)
                    }
                    byteBuilder.appendInstruction(Instructions.PRINTLN)
                }
            }
        }
    }

    private fun compileExpression(expression: Expression) {
        when (expression) {
            is LiteralIntExpression -> {
                byteBuilder.appendInstruction(Instructions.PUSH_I32_LITERAL)
                byteBuilder.appendInt(expression.v.value)
            }

            is LowerIDExpression -> {
                val offset = bindings[expression.v.value]
                    ?: throw IllegalArgumentException("${expression.v.value} referenced at ${expression.v.location} not found")

                byteBuilder.appendInstruction(Instructions.PUSH_I32_STACK)
                byteBuilder.appendInt(offset)
            }

            else -> TODO()
        }
    }
}


