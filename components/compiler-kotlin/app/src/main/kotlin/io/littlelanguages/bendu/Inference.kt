package io.littlelanguages.bendu

import io.littlelanguages.bendu.typeinference.*
import io.littlelanguages.scanpiler.Location

private const val LITERAL_FIX_NAME = "[bob]"

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
        statement.terms.forEach { term ->
            env.rebind(
                term.id.value,
                env.generalise(term.type!!)
            )
        }
    }
}

private fun inferExpression(expression: Expression, env: Environment) {
    when (expression) {
        is AbortStatement -> {
            inferPrintArguments(expression.es, env)

            expression.type = typeUnit.withLocation(expression.location())
        }

        is ApplyExpression -> {
            inferScopedExpression(expression.f, env)

            expression.arguments.forEach { argument ->
                inferScopedExpression(argument, env)
            }

            val tv = env.nextVar()
            val domain = expression.arguments.map { it.type!! }

            env.addConstraint(expression.f.type!!, TArr(domain, tv))
            expression.type = tv
        }

        is ArrayElementProjectionExpression -> {
            inferScopedExpression(expression.array, env)
            inferScopedExpression(expression.index, env)

            val tv = env.nextVar()
            val arrayType = TCon("Array", listOf(tv))

            env.addConstraint(expression.array.type!!, arrayType)
            env.addConstraint(expression.index.type!!, typeInt)

            expression.type = tv
        }

        is ArrayRangeProjectionExpression -> {
            inferScopedExpression(expression.array, env)

            val arrayType = TCon("Array", listOf(env.nextVar()))
            env.addConstraint(expression.array.type!!, arrayType)

            if (expression.start != null) {
                inferScopedExpression(expression.start, env)
                env.addConstraint(expression.start.type!!, typeInt)
            }

            if (expression.end != null) {
                inferScopedExpression(expression.end, env)
                env.addConstraint(expression.end.type!!, typeInt)
            }

            expression.type = arrayType
        }

        is AssignmentExpression -> {
            if (expression.lhs is LowerIDExpression) {
                val binding = env.binding(expression.lhs.v.value)
                if (binding != null && !binding.mutable) {
                    env.errors.addError(
                        IdentifierImmutableError(
                            StringLocation(
                                expression.lhs.v.value,
                                binding.location
                            ), expression.lhs.location()
                        )
                    )
                }
            } else {
                env.errors.addError(AssignmentError(expression.lhs.location()))
            }
            inferScopedExpression(expression.lhs, env)
            inferScopedExpression(expression.rhs, env)

            env.addConstraint(expression.lhs.type!!, expression.rhs.type!!)
            expression.type = expression.rhs.type!!.withLocation(expression.location())
        }

        is BinaryExpression -> {
            inferScopedExpression(expression.e1, env)
            inferScopedExpression(expression.e2, env)

            val tv = env.nextVar()
            expression.type = tv

            val u1 = TArr(listOf(expression.e1.type!!), TArr(listOf(expression.e2.type!!), tv))
            val u2 = env.instantiateScheme(binaryOperatorSignatures[expression.op.op] ?: Scheme(setOf(), typeError))

            env.addConstraint(u1, u2)
        }

        is BlockExpression -> {
            env.openTypeEnv()

            expression.es.forEach { e ->
                inferExpression(e, env)
                e.apply(env.solveConstraints(), env.errors)

                if (e is LetStatement) {
                    e.terms.forEach { term ->
                        env.rebind(
                            term.id.value,
                            env.generalise(term.type!!)
                        )
                    }
                }
            }
            env.closeTypeEnv()

            expression.type =
                if (expression.es.isEmpty())
                    typeUnit.withLocation(expression.location())
                else
                    expression.es.last().type
        }

        is IfExpression -> {
            expression.guards.forEach { guard ->
                inferScopedExpression(guard.first, env)
                inferScopedExpression(guard.second, env)

                env.addConstraint(guard.first.type!!, typeBool)
                env.addConstraint(guard.second.type!!, expression.guards[0].second.type!!)
            }
            expression.type = expression.guards[0].second.type

            if (expression.elseBranch == null) {
                env.addConstraint(expression.type!!, typeUnit)
            } else {
                inferScopedExpression(expression.elseBranch, env)
                env.addConstraint(expression.type!!, expression.elseBranch.type!!)
            }
        }

        is LetStatement -> {
            val tv = env.nextVars(expression.terms.size)
            expression.terms.forEachIndexed { i, term ->
                val scheme = Scheme(emptySet(), tv[i])
                env.bind(term.id.value, term.id.location, term.mutable, scheme)
                expression.terms[i].type = tv[i]

                if (term is LetValueStatementTerm && term.typeQualifier != null) {
                    env.addConstraint(tv[i], term.typeQualifier!!.toType(env))
                }
            }

            val declarationType = fix(
                LiteralFunctionExpression(
                    emptyList(),
                    listOf(LowerIDFunctionParameter(LITERAL_FIX_NAME, expression.location(), false, null)),
                    null,
                    LiteralTupleExpression(expression.terms.map {
                        when (it) {
                            is LetValueStatementTerm -> it.e
                            is LetFunctionStatementTerm -> LiteralFunctionExpression(
                                it.typeVariables,
                                it.parameters,
                                it.typeQualifier,
                                it.body
                            )
                        }
                    })
                ), env
            )
            env.addConstraint(declarationType, TTuple(tv))

            expression.type = typeUnit.withLocation(expression.location())
        }

        is LiteralArrayExpression -> {
            val tv = env.nextVar()

            expression.es.forEach { e ->
                inferScopedExpression(e, env)
                env.addConstraint(e.type!!, tv)
            }

            expression.type = TCon("Array", listOf(tv)).withLocation(expression.location())
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
            fun inferFunctionParameters(type: Type, parameter: FunctionParameter) {
                when (parameter) {
                    is LowerIDFunctionParameter -> {
                        if (parameter.value != LITERAL_FIX_NAME) {
                            env.bind(parameter.value, parameter.location, parameter.mutable, Scheme(emptySet(), type))
                        }
                    }

                    is TupleFunctionParameter -> {
                        val tupleTypes = env.nextVars(parameter.parameters.size)
                        parameter.parameters.forEachIndexed { index, p ->
                            inferFunctionParameters(tupleTypes[index], p)
                        }
                        env.addConstraint(type, TTuple(tupleTypes))
                    }

                    is WildcardFunctionParameter -> {}
                }
                if (parameter.typeQualifier != null) {
                    env.addConstraint(type, parameter.typeQualifier!!.toType(env))
                }
            }

            env.openTypeEnv()

            val tv = env.nextVar()
            val domain = env.nextVars(expression.parameters.size)

            expression.typeParameters.forEach { parameter ->
                env.bindParameter(parameter.value, parameter.location)
            }

            expression.parameters.forEachIndexed { index, parameter ->
                inferFunctionParameters(domain[index], parameter)
            }

            inferScopedExpression(expression.body, env)

            if (expression.returnTypeQualifier != null) {
                env.addConstraint(expression.body.type!!, expression.returnTypeQualifier.toType(env))
            }

            env.closeTypeEnv()

            val result = TArr(domain, expression.body.type!!)
            env.addConstraint(tv, result)

            expression.type = tv
        }

        is LiteralStringExpression ->
            expression.type = typeString.withLocation(expression.location())

        is LiteralTupleExpression -> {
            expression.es.forEach { e ->
                inferScopedExpression(e, env)
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

        is TypedExpression -> {
            inferScopedExpression(expression.e, env)

            expression.type = expression.typeQualifier.toType(env)

            env.addConstraint(expression.e.type!!, expression.type!!)
        }

        is UnaryExpression -> {
            inferScopedExpression(expression.e, env)

            val tv = env.nextVar()
            expression.type = tv

            val u1 = TArr(listOf(expression.e.type!!), tv)
            val u2 = env.instantiateScheme(unaryOperatorSignatures[expression.op.op] ?: Scheme(setOf(), typeError))

            env.addConstraint(u1, u2)
        }

        is WhileExpression -> {
            inferScopedExpression(expression.guard, env)
            inferScopedExpression(expression.body, env)

            env.addConstraint(expression.guard.type!!, typeBool)

            expression.type = typeUnit.withLocation(expression.location())
        }
    }
}

private fun inferScopedExpression(expression: Expression, env: Environment) {
    if (expression is LetStatement) {
        env.openTypeEnv()
        inferExpression(expression, env)
        env.closeTypeEnv()
    } else
        inferExpression(expression, env)
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
    private val typeVariables = mutableListOf<MutableMap<String, Pair<Location, Type>>>(mutableMapOf())

    fun bind(name: String, location: Location, mutable: Boolean, scheme: Scheme) {
        val binding = typeEnv[name]

        if (binding != null) {
            errors.addError(IdentifierRedefinitionError(StringLocation(name, location), binding.location))
        }

        typeEnv += (name to Binding(location, mutable, scheme))
    }

    fun rebind(name: String, scheme: Scheme) {
        val binding = typeEnv[name]!!
        typeEnv += (name to Binding(binding.location, binding.mutable, scheme))
    }

    fun bindParameter(name: String, location: Location) {
        val variables = typeVariables[typeVariables.size - 1]

        if (variables.containsKey(name)) {
            errors.addError(IdentifierRedefinitionError(StringLocation(name, location), variables[name]!!.first))
        }

        variables[name] = Pair(location, nextVar())
    }

    fun openTypeEnv() {
        typeEnvs.add(typeEnv)
        typeVariables.add(mutableMapOf())
    }

    fun closeTypeEnv() {
        typeEnv = typeEnvs.removeAt(typeEnvs.size - 1)
        typeVariables.removeAt(typeVariables.size - 1)
    }

    operator fun get(name: String): Scheme? =
        binding(name)?.scheme

    fun binding(name: String): Binding? = typeEnv[name]

    fun parameter(name: String): Type? {
        var i = typeVariables.size - 1
        while (i >= 0) {
            val type = typeVariables[i][name]

            if (type != null) {
                return type.second
            }

            i -= 1
        }

        return null
    }

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

    fun generalise(type: Type): Scheme =
        typeEnv.generalise(type)
}
