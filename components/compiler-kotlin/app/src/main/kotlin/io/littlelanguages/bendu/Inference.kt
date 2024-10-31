package io.littlelanguages.bendu

import io.littlelanguages.bendu.typeinference.typeInt

fun infer(script: List<Statement>) {
    inferStatements(script)
}

fun inferStatements(statements: List<Statement>) {
    statements.forEach { statement ->
        when (statement) {
            is ExpressionStatement -> {
                inferExpression(statement.e)
            }

            is LetStatement -> {
                inferExpression(statement.e)
            }

            is PrintStatement -> {
                statement.es.forEach { e ->
                    inferExpression(e)
                }
            }

            is PrintlnStatement -> {
                statement.es.forEach { e ->
                    inferExpression(e)
                }
            }
        }
    }
}

fun inferExpression(expression: Expression) {
    when (expression) {
        is BinaryExpression -> {
            inferExpression(expression.e1)
            inferExpression(expression.e2)
        }

        is LiteralIntExpression -> {
            expression.type = typeInt
        }

        is LowerIDExpression -> {
            // Do nothing
        }
    }
}
