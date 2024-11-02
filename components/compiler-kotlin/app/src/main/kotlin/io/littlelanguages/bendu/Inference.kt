package io.littlelanguages.bendu

import io.littlelanguages.bendu.typeinference.Constraints
import io.littlelanguages.bendu.typeinference.Pump
import io.littlelanguages.bendu.typeinference.Scheme
import io.littlelanguages.bendu.typeinference.TArr
import io.littlelanguages.bendu.typeinference.TVar
import io.littlelanguages.bendu.typeinference.Type
import io.littlelanguages.bendu.typeinference.TypeEnv
import io.littlelanguages.bendu.typeinference.emptyTypeEnv
import io.littlelanguages.bendu.typeinference.typeBool
import io.littlelanguages.bendu.typeinference.typeError
import io.littlelanguages.bendu.typeinference.typeInt

fun infer(
    script: String,
    typeEnv: TypeEnv = emptyTypeEnv,
    pump: Pump = Pump(),
    errors: Errors = Errors(),
    constraints: Constraints = Constraints()
): List<Statement> {
    val ast = parse(script, errors)

    if (errors.hasErrors()) {
        return emptyList()
    }

    inferStatements(ast, Environment(typeEnv, pump, errors, constraints))

    return ast
}


private fun inferStatements(statements: List<Statement>, env: Environment) =
    statements.forEach { statement -> inferStatement(statement, env) }

private fun inferStatement(statement: Statement, env: Environment) {
    env.constraints.reset()

    when (statement) {
        is ExpressionStatement -> {
            inferExpression(statement.e, env)

            statement.e.apply(env.constraints.solve())
        }

        is LetStatement -> {
            inferExpression(statement.e, env)

            statement.e.apply(env.constraints.solve())

            env.bind(statement.id.value, statement.e.type!!)
        }

        is PrintStatement -> {
            statement.es.forEach { e ->
                inferExpression(e, env)
            }

            val s = env.constraints.solve()

            statement.es.forEach { e ->
                e.apply(s)
            }
        }

        is PrintlnStatement -> {
            statement.es.forEach { e ->
                inferExpression(e, env)
            }
            val s = env.constraints.solve()

            statement.es.forEach { e ->
                e.apply(s)
            }
        }
    }
}

private fun inferExpression(expression: Expression, env: Environment) {
    when (expression) {
        is BinaryExpression -> {
            inferExpression(expression.e1, env)
            inferExpression(expression.e2, env)

            val tv = env.pump.next()
            expression.type = tv

            val u1 = TArr(expression.e1.type!!, TArr(expression.e2.type!!, tv))
            val u2 = (operatorSignatures[expression.op.op] ?: Scheme(setOf(), typeError)).instantiate(env.pump)

            env.constraints.add(u1, u2)
        }

        is LiteralBoolExpression ->
            expression.type = typeBool

        is LiteralIntExpression ->
            expression.type = typeInt

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

private val operatorSignatures = mapOf(
    Pair(Op.Plus, Scheme(setOf(0), TArr(TVar(0), TArr(TVar(0), TVar(0))))),
    Pair(Op.Minus, Scheme(setOf(0), TArr(TVar(0), TArr(TVar(0), TVar(0))))),
    Pair(Op.Multiply, Scheme(setOf(0), TArr(TVar(0), TArr(TVar(0), TVar(0))))),
    Pair(Op.Divide, Scheme(setOf(0), TArr(TVar(0), TArr(TVar(0), TVar(0))))),
    Pair(Op.Modulo, Scheme(setOf(0), TArr(TVar(0), TArr(TVar(0), TVar(0))))),
    Pair(Op.Power, Scheme(setOf(0), TArr(TVar(0), TArr(TVar(0), TVar(0)))))
)

data class Environment(var typeEnv: TypeEnv, val pump: Pump, val errors: Errors, val constraints: Constraints) {
    fun bind(name: String, type: Type) {
        typeEnv = typeEnv + (name to typeEnv.generalise(type))
    }
}

