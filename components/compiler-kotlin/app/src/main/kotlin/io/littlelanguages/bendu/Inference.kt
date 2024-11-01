package io.littlelanguages.bendu

import io.littlelanguages.bendu.typeinference.Pump
import io.littlelanguages.bendu.typeinference.TypeEnv
import io.littlelanguages.bendu.typeinference.emptyTypeEnv
import io.littlelanguages.bendu.typeinference.typeError
import io.littlelanguages.bendu.typeinference.typeInt

fun infer(script: List<Statement>, typeEnv: TypeEnv = emptyTypeEnv, pump: Pump = Pump(), errors: Errors = Errors()) {
    inferStatements(script, Environment(typeEnv, pump, errors))
}

data class Environment(val typeEnv: TypeEnv, val pump: Pump, val errors: Errors)

fun inferStatements(statements: List<Statement>, env: Environment) {
    statements.forEach { statement ->
        when (statement) {
            is ExpressionStatement -> {
                inferExpression(statement.e, env)
            }

            is LetStatement -> {
                inferExpression(statement.e, env)
            }

            is PrintStatement -> {
                statement.es.forEach { e ->
                    inferExpression(e, env)
                }
            }

            is PrintlnStatement -> {
                statement.es.forEach { e ->
                    inferExpression(e, env)
                }
            }
        }
    }
}

fun inferExpression(expression: Expression, env: Environment) {
    when (expression) {
        is BinaryExpression -> {
            inferExpression(expression.e1, env)
            inferExpression(expression.e2, env)
        }

        is LiteralIntExpression -> {
            expression.type = typeInt
        }

        is LowerIDExpression -> {
            val scheme = env.typeEnv[expression.v.value]

            if (scheme == null) {
                env.errors.addError(UnknownIdentifierError(expression.v))
                expression.type = typeError
            } else {
                expression.type = scheme.instantiate(env.pump)
            }
        }
    }
}
