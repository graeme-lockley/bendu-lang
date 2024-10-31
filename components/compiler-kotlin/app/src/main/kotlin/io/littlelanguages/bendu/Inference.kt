package io.littlelanguages.bendu

import io.littlelanguages.bendu.typeinference.Pump
import io.littlelanguages.bendu.typeinference.TypeEnv
import io.littlelanguages.bendu.typeinference.emptyTypeEnv
import io.littlelanguages.bendu.typeinference.typeInt

fun infer(script: List<Statement>, typeEnv: TypeEnv = emptyTypeEnv, pump: Pump = Pump()) {
    inferStatements(script, typeEnv, pump)
}

fun inferStatements(statements: List<Statement>, typeEnv: TypeEnv, pump: Pump) {
    statements.forEach { statement ->
        when (statement) {
            is ExpressionStatement -> {
                inferExpression(statement.e, typeEnv, pump)
            }

            is LetStatement -> {
                inferExpression(statement.e, typeEnv, pump)
            }

            is PrintStatement -> {
                statement.es.forEach { e ->
                    inferExpression(e, typeEnv, pump)
                }
            }

            is PrintlnStatement -> {
                statement.es.forEach { e ->
                    inferExpression(e, typeEnv, pump)
                }
            }
        }
    }
}

fun inferExpression(expression: Expression, typeEnv: TypeEnv, pump: Pump) {
    when (expression) {
        is BinaryExpression -> {
            inferExpression(expression.e1, typeEnv, pump)
            inferExpression(expression.e2, typeEnv, pump)
        }

        is LiteralIntExpression -> {
            expression.type = typeInt
        }

        is LowerIDExpression -> {
            val scheme = typeEnv[expression.v.value]

            if (scheme == null) {
                throw IllegalArgumentException("Unknown identifier: ${expression.v.location}: ${expression.v.value}")
            } else {
                expression.type = scheme.instantiate(pump)
            }
        }
    }
}
