package io.littlelanguages.bendu

import io.littlelanguages.bendu.typeinference.*

fun infer(
    script: String,
    typeEnv: TypeEnv = emptyTypeEnv,
    pump: Pump = Pump(),
    errors: Errors = Errors(),
    constraints: Constraints = Constraints()
): List<Expression> {
    val ast = parse(script, errors)

    if (errors.hasErrors()) {
        return emptyList()
    }

    inferStatements(ast, Environment(typeEnv, pump, errors, constraints))

    return ast
}


private fun inferStatements(statements: List<Expression>, env: Environment) =
    statements.forEach { statement -> inferStatement(statement, env) }

private fun inferStatement(statement: Expression, env: Environment) {
    env.resetConstraints()

    inferExpression(statement, env)

    statement.apply(env.solveConstraints(), env.errors)

    if (statement is LetStatement) {
        statement.terms.forEach { term -> env.bind(term.id.value, Scheme(emptySet(), term.e.type!!)) }
    }
}

private fun inferExpression(expression: Expression, env: Environment) {
    when (expression) {
        is AbortStatement -> {
            inferPrintArguments(expression.es, env)

            expression.type = typeUnit.withLocation(expression.location())
        }

        is ApplyExpression -> {
            inferExpression(expression.f, env)

            expression.arguments.forEach { argument ->
                inferExpression(argument, env)
            }

            val tv = env.nextVar()
            val domain = expression.arguments.map { it.type!! }

            env.addConstraint(expression.f.type!!, TArr(domain, tv))
            expression.type = tv

        }

        is BinaryExpression -> {
            inferExpression(expression.e1, env)
            inferExpression(expression.e2, env)

            val tv = env.nextVar()
            expression.type = tv

            val u1 = TArr(listOf(expression.e1.type!!), TArr(listOf(expression.e2.type!!), tv))
            val u2 = env.instantiateScheme(binaryOperatorSignatures[expression.op.op] ?: Scheme(setOf(), typeError))

            env.addConstraint(u1, u2)
        }

        is IfExpression -> {
            expression.guards.forEach { guard ->
                inferExpression(guard.first, env)
                inferExpression(guard.second, env)

                env.addConstraint(guard.first.type!!, typeBool)
                env.addConstraint(guard.second.type!!, expression.guards[0].second.type!!)
            }
            expression.type = expression.guards[0].second.type

            if (expression.elseBranch == null) {
                env.addConstraint(expression.type!!, typeUnit)
            } else {
                inferExpression(expression.elseBranch, env)
                env.addConstraint(expression.type!!, expression.elseBranch.type!!)
            }
        }

        is LetStatement -> {
            val tv = env.nextVars(expression.terms.size)
            expression.terms.forEachIndexed { i, term ->
                val scheme = Scheme(emptySet(), tv[i])
                env.bind(term.id.value, scheme)
            }

            val declarationType = fix(
                LiteralFunctionExpression(
                    listOf(StringLocation("_bob", expression.location())),
                    LiteralTupleExpression(expression.terms.map { it.e })
                ), env
            )
            env.addConstraint(declarationType, TTuple(tv))

            expression.type = typeUnit.withLocation(expression.location())
        }

        is LiteralBoolExpression ->
            expression.type = typeBool.withLocation(expression.location())

        is LiteralCharExpression ->
            expression.type = typeChar.withLocation(expression.location())

        is LiteralFloatExpression ->
            expression.type = typeFloat.withLocation(expression.location())

        is LiteralIntExpression ->
            expression.type = typeInt.withLocation(expression.location())

        is LiteralFunctionExpression -> {
            env.openTypeEnv()

            val tv = env.nextVar()
            val domain = env.nextVars(expression.parameters.size)

            expression.parameters.forEachIndexed { index, parameter ->
                env.bind(parameter.value, Scheme(emptySet(), domain[index]))
            }

            inferExpression(expression.body, env)

            env.closeTypeEnv()

            val result = TArr(domain, expression.body.type!!)
            env.addConstraint(tv, result)

            expression.type = tv
        }

        is LiteralStringExpression ->
            expression.type = typeString.withLocation(expression.location())

        is LiteralTupleExpression -> {
            expression.es.forEach { e ->
                inferExpression(e, env)
            }

            expression.type = TTuple(expression.es.map { it.type!! }).withLocation(expression.location())
        }

        is LiteralUnitExpression ->
            expression.type = typeUnit.withLocation(expression.location())

        is LowerIDExpression -> {
            val scheme = env[expression.v.value]

            if (scheme == null) {
                env.errors.addError(UnknownIdentifierError(expression.v))
                expression.type = typeError.withLocation(expression.location())
            } else {
                expression.type = env.instantiateScheme(scheme).withLocation(expression.location())
            }
        }

        is PrintStatement -> {
            inferPrintArguments(expression.es, env)

            expression.type = typeUnit.withLocation(expression.location())
        }

        is PrintlnStatement -> {
            inferPrintArguments(expression.es, env)

            expression.type = typeUnit.withLocation(expression.location())
        }

        is UnaryExpression -> {
            inferExpression(expression.e, env)

            val tv = env.nextVar()
            expression.type = tv

            val u1 = TArr(listOf(expression.e.type!!), tv)
            val u2 = env.instantiateScheme(unaryOperatorSignatures[expression.op.op] ?: Scheme(setOf(), typeError))

            env.addConstraint(u1, u2)
        }

        else -> TODO(expression.toString())
    }
}

private fun inferPrintArguments(es: List<Expression>, env: Environment) {
    es.forEach { e ->
        inferExpression(e, env)
    }
    val s = env.solveConstraints()

    es.forEach { e ->
        e.apply(s, env.errors)
    }
}

private fun fix(e: Expression, env: Environment): Type {
    inferExpression(e, env)
    val tv = env.nextVar()

    env.addConstraint(TArr(listOf(tv), tv), e.type!!)

    return tv
}

private val binaryOperatorSignatures = mapOf(
    Pair(Op.And, Scheme(setOf(), TArr(listOf(typeBool), TArr(listOf(typeBool), typeBool)))),
    Pair(Op.Or, Scheme(setOf(), TArr(listOf(typeBool), TArr(listOf(typeBool), typeBool)))),

    Pair(Op.EqualEqual, Scheme(setOf(0), TArr(listOf(TVar(0)), TArr(listOf(TVar(0)), typeBool)))),
    Pair(Op.NotEqual, Scheme(setOf(0), TArr(listOf(TVar(0)), TArr(listOf(TVar(0)), typeBool)))),
    Pair(Op.LessThan, Scheme(setOf(0), TArr(listOf(TVar(0)), TArr(listOf(TVar(0)), typeBool)))),
    Pair(Op.LessEqual, Scheme(setOf(0), TArr(listOf(TVar(0)), TArr(listOf(TVar(0)), typeBool)))),
    Pair(Op.GreaterThan, Scheme(setOf(0), TArr(listOf(TVar(0)), TArr(listOf(TVar(0)), typeBool)))),
    Pair(Op.GreaterEqual, Scheme(setOf(0), TArr(listOf(TVar(0)), TArr(listOf(TVar(0)), typeBool)))),

    Pair(Op.Plus, Scheme(setOf(0), TArr(listOf(TVar(0)), TArr(listOf(TVar(0)), TVar(0))))),
    Pair(Op.Minus, Scheme(setOf(0), TArr(listOf(TVar(0)), TArr(listOf(TVar(0)), TVar(0))))),
    Pair(Op.Multiply, Scheme(setOf(0), TArr(listOf(TVar(0)), TArr(listOf(TVar(0)), TVar(0))))),
    Pair(Op.Divide, Scheme(setOf(0), TArr(listOf(TVar(0)), TArr(listOf(TVar(0)), TVar(0))))),
    Pair(Op.Modulo, Scheme(setOf(0), TArr(listOf(TVar(0)), TArr(listOf(TVar(0)), TVar(0))))),
    Pair(Op.Power, Scheme(setOf(0), TArr(listOf(TVar(0)), TArr(listOf(TVar(0)), TVar(0)))))
)

private val unaryOperatorSignatures = mapOf(
    Pair(UnaryOp.Not, Scheme(setOf(), TArr(listOf(typeBool), typeBool))),
    Pair(UnaryOp.TypeOf, Scheme(setOf(0), TArr(listOf(TVar(0)), typeString)))
)

data class Environment(
    private var typeEnv: TypeEnv,
    private val pump: Pump,
    val errors: Errors,
    private val constraints: Constraints
) {
    private val typeEnvs = mutableListOf(typeEnv)

//    fun bind(name: String, type: Type) {
//        typeEnv = typeEnv + (name to typeEnv.generalise(type))
//    }

    fun bind(name: String, scheme: Scheme) {
        typeEnv = typeEnv + (name to scheme)
    }

    fun openTypeEnv() {
        typeEnvs.add(typeEnv)
    }

    fun closeTypeEnv() {
        typeEnv = typeEnvs.removeAt(typeEnvs.size - 1)
    }

    operator fun get(name: String): Scheme? = typeEnv[name]

    fun solveConstraints(): Subst = constraints.solve(errors)

    fun nextVar(): TVar = pump.next()

    fun nextVars(n: Int): List<TVar> = pump.nextN(n)

    fun instantiateScheme(scheme: Scheme): Type = scheme.instantiate(pump)

    fun resetConstraints() {
        constraints.reset()
    }

    fun addConstraint(t1: Type, t2: Type) {
        constraints.add(t1, t2)
    }
}

