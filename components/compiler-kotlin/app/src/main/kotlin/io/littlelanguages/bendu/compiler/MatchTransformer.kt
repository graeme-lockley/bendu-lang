package io.littlelanguages.bendu.compiler

import io.littlelanguages.bendu.*
import io.littlelanguages.bendu.typeinference.*

fun transformMatchExpression(e: MatchExpression): Expression {
    fun transform(e: Expression): Expression = when (e) {
        is AbortStatement ->
            AbortStatement(e.es.map { transform(it) }, e.location(), e.type)

        is ApplyExpression ->
            ApplyExpression(transform(e.f), e.arguments.map { transform(it) }, e.type)

        is ArrayElementProjectionExpression ->
            ArrayElementProjectionExpression(transform(e.array), transform(e.index), e.type)

        is ArrayRangeProjectionExpression ->
            ArrayRangeProjectionExpression(
                transform(e.array),
                e.start?.let { transform(it) },
                e.end?.let { transform(it) },
                e.type
            )

        is AssignmentExpression -> TODO()

        is BinaryExpression ->
            BinaryExpression(transform(e.e1), e.op, transform(e.e2), e.type)

        is BlockExpression ->
            BlockExpression(e.es.map { transform(it) }, e.location(), e.type)

        is CaseExpression -> TODO()
        is ErrorExpression -> TODO()
        is FailExpression -> TODO()
        is FatBarExpression -> TODO()

        is IfExpression ->
            IfExpression(
                e.guards.map { Pair(transform(it.first), transform(it.second)) },
                e.elseBranch?.let { transform(it) },
                e.type
            )

        is LetStatement -> TODO()

        is LiteralArrayExpression ->
            LiteralArrayExpression(e.es.map { Pair(transform(it.first), it.second) }, e.location, e.type)

        is LiteralBoolExpression -> e

        is LiteralCharExpression -> e

        is LiteralFloatExpression -> e

        is LiteralFunctionExpression -> TODO()

        is LiteralIntExpression -> e

        is LiteralStringExpression -> e

        is LiteralTupleExpression ->
            LiteralTupleExpression(e.es.map { transform(it) }, e.type)

        is LiteralUnitExpression -> e

        is LowerIDExpression ->
            e

        is MatchExpression -> {
            val matchExpression = transform(e.e)
            val variable = if (matchExpression is LowerIDExpression) matchExpression.v.value else "_x"

            val match = fullMatch(
                listOf(variable),
                e.cases.map { Equation(listOf(it.pattern), transform(it.body)) },
                ErrorExpression(e.location()),
                PatternEnvironment()
            )

            if (matchExpression is LowerIDExpression)
                match
            else {
                val valueName = StringLocation(variable, e.location())
                val letValueStatementTerm =
                    LetValueStatementTerm(
                        id = valueName,
                        mutable = false,
                        exported = false,
                        typeVariables = emptyList(),
                        typeQualifier = null,
                        e = matchExpression,
                        location = e.location()
                    )

                BlockExpression(listOf(LetStatement(listOf(letValueStatementTerm)), match), e.location(), e.type)
            }
        }

        is ModuleReferenceExpression -> TODO()
        is PrintStatement -> TODO()
        is PrintlnStatement -> TODO()
        is TypedExpression -> TODO()
        is UnaryExpression -> TODO()
        is UpperIDExpression -> e
        is WhileExpression -> TODO()
    }

    return transform(e)
}

data class PatternEnvironment(private var counter: Int = 0) {
    fun constructors(constructor: Constructor): List<Constructor> =
        constructor.constructors()

    fun makeVar(): String = "_u${counter++}"
}

data class Equation(val patterns: List<Pattern>, val body: Expression, val guard: Expression? = null) {
    fun isVar(): Boolean = patterns.isNotEmpty() && patterns[0] is LowerIDPattern

    fun isCon(): Boolean = patterns.isNotEmpty() && patterns[0] is ConstructorPattern

    fun getCon(): Constructor = (patterns[0] as ConstructorPattern).constructor!!
}

fun canError(e: Expression): Boolean = when (e) {
    is AbortStatement ->
        e.es.any { canError(it) }

    is ApplyExpression ->
        canError(e.f) || e.arguments.any { canError(it) }

    is ArrayElementProjectionExpression ->
        canError(e.array) || canError(e.index)

    is ArrayRangeProjectionExpression ->
        canError(e.array) || e.start?.let { canError(it) } == true || e.end?.let { canError(it) } == true

    is BinaryExpression ->
        canError(e.e1) || canError(e.e2)

    is BlockExpression ->
        e.es.any { canError(it) }

    is CaseExpression ->
        e.clauses.any { canError(it.expression) }

    is ErrorExpression ->
        true

    is FatBarExpression ->
        canError(e.left) || canError(e.right)

    is IfExpression ->
        e.guards.any { canError(it.first) || canError(it.second) } || e.elseBranch?.let { canError(it) } == true

    is LiteralBoolExpression ->
        false

    is LiteralCharExpression ->
        false

    is LiteralFloatExpression ->
        false

    is LiteralIntExpression ->
        false

    is LiteralStringExpression ->
        false

    is LowerIDExpression ->
        false

    else -> TODO(e.toString()) //false
}

fun <T> partition(list: List<T>, predicate: (T) -> Boolean): List<List<T>> {
    fun combine(e: T, acc: List<List<T>>): List<List<T>> {
        if (acc.isEmpty()) return listOf(listOf(e))

        val v = acc.first().first()

        if (predicate(v) == predicate(e)) return listOf(listOf(e) + acc.first()) + acc.drop(1)

        return listOf(listOf(e)) + acc
    }

    return list.foldRight(emptyList()) { e, acc -> combine(e, acc) }
}

fun removeLiterals(equations: List<Equation>): List<Equation> {
    var counter = 0

    fun nextVarName(): String = "_l${counter++}"

    fun removeLiterals(equation: Equation): Equation {
        var guard = equation.guard

        fun appendToGuard(expr: Expression) {
            guard = if (guard == null) expr else IfExpression(
                listOf(Pair(guard!!, expr)),
                LiteralBoolExpression(BoolLocation(false, expr.location()), typeBool),
                typeBool
            )
        }

        fun removeLiterals(pattern: Pattern): Pattern = when (pattern) {
            is ConstructorPattern -> ConstructorPattern(
                pattern.moduleID,
                pattern.id,
                pattern.patterns.map { removeLiterals(it) },
                pattern.location,
                pattern.type,
                pattern.constructor
            )

            is LiteralBoolPattern -> {
                val name = nextVarName()

                val expr = if (pattern.v.value)
                    LowerIDExpression(StringLocation(name, pattern.location()), typeBool, null)
                else
                    UnaryExpression(
                        UnaryOpLocation(UnaryOp.Not, pattern.location()),
                        LowerIDExpression(StringLocation(name, pattern.location()), typeBool, null),
                        typeBool
                    )


                appendToGuard(expr)
                LowerIDPattern(StringLocation(name, pattern.location()), pattern.type)
            }

            is LiteralCharPattern -> {
                val name = nextVarName()
                val expr = BinaryExpression(
                    LowerIDExpression(StringLocation(name, pattern.location()), typeChar, null),
                    OpLocation(Op.EqualEqual, pattern.location()),
                    LiteralCharExpression(pattern.v, typeChar),
                    typeBool
                )

                appendToGuard(expr)
                LowerIDPattern(StringLocation(name, pattern.location()), pattern.type)
            }

            is LiteralFloatPattern -> {
                val name = nextVarName()
                val expr = BinaryExpression(
                    LowerIDExpression(StringLocation(name, pattern.location()), typeFloat, null),
                    OpLocation(Op.EqualEqual, pattern.location()),
                    LiteralFloatExpression(pattern.v, typeFloat),
                    typeBool
                )

                appendToGuard(expr)
                LowerIDPattern(StringLocation(name, pattern.location()), pattern.type)
            }

            is LiteralIntPattern -> {
                val name = nextVarName()
                val expr = BinaryExpression(
                    LowerIDExpression(StringLocation(name, pattern.location()), typeInt, null),
                    OpLocation(Op.EqualEqual, pattern.location()),
                    LiteralIntExpression(pattern.v, typeInt),
                    typeBool
                )

                appendToGuard(expr)
                LowerIDPattern(StringLocation(name, pattern.location()), pattern.type)
            }

            is LiteralStringPattern -> {
                val name = nextVarName()
                val expr = BinaryExpression(
                    LowerIDExpression(StringLocation(name, pattern.location()), typeString, null),
                    OpLocation(Op.EqualEqual, pattern.location()),
                    LiteralStringExpression(pattern.v, typeString),
                    typeBool
                )

                appendToGuard(expr)
                LowerIDPattern(StringLocation(name, pattern.location()), pattern.type)
            }

            is LiteralUnitPattern ->
                LowerIDPattern(StringLocation(nextVarName(), pattern.location()), pattern.type)

            is LowerIDPattern ->
                pattern

            is NamedPattern -> TODO()

            is TuplePattern ->
                TuplePattern(pattern.patterns.map { removeLiterals(it) }, pattern.location(), pattern.type)

            is TypedPattern -> TODO()

            is WildcardPattern ->
                LowerIDPattern(StringLocation(nextVarName(), pattern.location()), pattern.type)
        }

        return Equation(equation.patterns.map { removeLiterals(it) }, equation.body, guard)
    }

    return equations.map { removeLiterals(it) }
}

fun tidyUpFails(e: Expression): Expression {
    fun replaceFailWithError(ep: Expression): Expression = when (ep) {
        is AbortStatement ->
            AbortStatement(ep.es.map { replaceFailWithError(it) }, ep.location(), ep.type)

        is ApplyExpression ->
            ApplyExpression(replaceFailWithError(ep.f), ep.arguments.map { replaceFailWithError(it) }, ep.type)

        is ArrayElementProjectionExpression ->
            ArrayElementProjectionExpression(replaceFailWithError(ep.array), replaceFailWithError(ep.index), ep.type)

        is ArrayRangeProjectionExpression ->
            ArrayRangeProjectionExpression(
                replaceFailWithError(ep.array),
                ep.start?.let { replaceFailWithError(it) },
                ep.end?.let { replaceFailWithError(it) },
                ep.type
            )

        is BinaryExpression ->
            BinaryExpression(replaceFailWithError(ep.e1), ep.op, replaceFailWithError(ep.e2), ep.type)

        is BlockExpression ->
            BlockExpression(ep.es.map { replaceFailWithError(it) }, ep.location(), ep.type)

        is CaseExpression -> CaseExpression(
            ep.variable,
            ep.clauses.map { Clause(it.constructor, it.variables, replaceFailWithError(it.expression)) },
            ep.location
        )

        is FailExpression -> ErrorExpression(ep.location)

        is FatBarExpression -> FatBarExpression(ep.left, replaceFailWithError(ep.right), ep.location)

        is IfExpression ->
            IfExpression(
                ep.guards.map { Pair(replaceFailWithError(it.first), replaceFailWithError(it.second)) },
                ep.elseBranch?.let { replaceFailWithError(it) },
                ep.type
            )

        is LiteralBoolExpression -> ep
        is LiteralCharExpression -> ep
        is LiteralFloatExpression -> ep
        is LiteralIntExpression -> ep
        is LiteralStringExpression -> ep
        is LiteralUnitExpression -> ep
        is LowerIDExpression -> ep

        is MatchExpression -> throw IllegalStateException("Match expressions should be desugared")
        else -> TODO(ep.toString()) // ep
    }

    return when (e) {
        is AbortStatement ->
            AbortStatement(e.es.map { tidyUpFails(it) }, e.location(), e.type)

        is ApplyExpression ->
            ApplyExpression(tidyUpFails(e.f), e.arguments.map { tidyUpFails(it) }, e.type)

        is ArrayElementProjectionExpression ->
            ArrayElementProjectionExpression(tidyUpFails(e.array), tidyUpFails(e.index), e.type)

        is ArrayRangeProjectionExpression ->
            ArrayRangeProjectionExpression(
                tidyUpFails(e.array),
                e.start?.let { tidyUpFails(it) },
                e.end?.let { tidyUpFails(it) },
                e.type
            )

        is BinaryExpression ->
            BinaryExpression(tidyUpFails(e.e1), e.op, tidyUpFails(e.e2), e.type)

        is BlockExpression ->
            BlockExpression(e.es.map { tidyUpFails(it) }, e.location(), e.type)

        is CaseExpression -> CaseExpression(
            e.variable,
            e.clauses.map { Clause(it.constructor, it.variables, tidyUpFails(it.expression)) },
            e.location
        )

        is FatBarExpression -> when (e.right) {
            is ErrorExpression -> tidyUpFails(replaceFailWithError(e.left))
            is FailExpression -> tidyUpFails(e.left)
            else -> FatBarExpression(tidyUpFails(e.left), tidyUpFails(e.right), e.location)
        }

        is IfExpression -> IfExpression(
            e.guards.map { Pair(tidyUpFails(it.first), tidyUpFails(it.second)) },
            e.elseBranch?.let { tidyUpFails(it) },
            e.type
        )

        is UnaryExpression ->
            UnaryExpression(e.op, tidyUpFails(e.e), e.type)

        is ErrorExpression -> e
        is FailExpression -> e
        is LiteralBoolExpression -> e
        is LiteralCharExpression -> e
        is LiteralFloatExpression -> e
        is LiteralIntExpression -> e
        is LiteralStringExpression -> e
        is LowerIDExpression -> e

        is MatchExpression ->
            throw IllegalStateException("Match expressions should be desugared")

        else -> TODO(e.toString()) //e
    }
}

fun removeUnusedVariables(e: Expression): Expression {
    fun variables(e: Expression): Set<String> = when (e) {
        is AbortStatement ->
            e.es.flatMap { variables(it) }.toSet()

        is ApplyExpression ->
            variables(e.f) + e.arguments.flatMap { variables(it) }.toSet()

        is ArrayElementProjectionExpression ->
            variables(e.array) + variables(e.index)

        is ArrayRangeProjectionExpression ->
            variables(e.array) + (e.start?.let { variables(it) } ?: emptySet()) + (e.end?.let { variables(it) }
                ?: emptySet())

        is BinaryExpression ->
            variables(e.e1) + variables(e.e2)

        is BlockExpression ->
            e.es.flatMap { variables(it) }.toSet()

        is CaseExpression -> {
            setOf(e.variable) +
                    e.clauses.flatMap { variables(it.expression) - it.variables.filterNotNull().toSet() }.toSet()
        }

        is FailExpression -> emptySet()

        is FatBarExpression -> variables(e.left) + variables(e.right)

        is IfExpression ->
            e.guards.flatMap { variables(it.first) + variables(it.second) }.toSet() +
                    (e.elseBranch?.let { variables(it) } ?: emptySet())

        is LiteralBoolExpression -> emptySet()
        is LiteralCharExpression -> emptySet()
        is LiteralFloatExpression -> emptySet()
        is LiteralIntExpression -> emptySet()
        is LiteralStringExpression -> emptySet()
        is LiteralUnitExpression -> emptySet()

        is LowerIDExpression -> setOf(e.v.value)

        is MatchExpression -> throw IllegalStateException("Match expressions should be desugared")
        else -> TODO(e.toString()) // emptySet()
    }

    return when (e) {
        is AbortStatement ->
            AbortStatement(e.es.map { removeUnusedVariables(it) }, e.location(), e.type)

        is ApplyExpression ->
            ApplyExpression(removeUnusedVariables(e.f), e.arguments.map { removeUnusedVariables(it) }, e.type)

        is ArrayElementProjectionExpression ->
            ArrayElementProjectionExpression(removeUnusedVariables(e.array), removeUnusedVariables(e.index), e.type)

        is ArrayRangeProjectionExpression ->
            ArrayRangeProjectionExpression(
                removeUnusedVariables(e.array),
                e.start?.let { removeUnusedVariables(it) },
                e.end?.let { removeUnusedVariables(it) },
                e.type
            )

        is BinaryExpression ->
            BinaryExpression(removeUnusedVariables(e.e1), e.op, removeUnusedVariables(e.e2), e.type)

        is BlockExpression ->
            BlockExpression(e.es.map { removeUnusedVariables(it) }, e.location(), e.type)

        is CaseExpression -> CaseExpression(e.variable, e.clauses.map {
            val vs = variables(it.expression)
            Clause(
                it.constructor,
                it.variables.map { v -> if (vs.contains(v)) v else null },
                removeUnusedVariables(it.expression)
            )
        }, e.location)

        is FatBarExpression -> FatBarExpression(
            removeUnusedVariables(e.left),
            removeUnusedVariables(e.right),
            e.location
        )

        is IfExpression ->
            IfExpression(
                e.guards.map { Pair(removeUnusedVariables(it.first), removeUnusedVariables(it.second)) },
                e.elseBranch?.let { removeUnusedVariables(it) },
                e.type
            )

        is UnaryExpression ->
            UnaryExpression(e.op, removeUnusedVariables(e.e), e.type)

        is ErrorExpression -> e
        is FailExpression -> e
        is LiteralBoolExpression -> e
        is LiteralCharExpression -> e
        is LiteralFloatExpression -> e
        is LiteralIntExpression -> e
        is LiteralStringExpression -> e
        is LowerIDExpression -> e

        is MatchExpression -> throw IllegalStateException("Match expressions should be desugared")

        else -> TODO(e.toString()) // e
    }
}


fun match(variables: List<String>, equations: List<Equation>, e: Expression, env: PatternEnvironment): Expression {
    fun subst(e: Expression, old: String, new: String): Expression = when (e) {
        is AbortStatement ->
            AbortStatement(e.es.map { subst(it, old, new) }, e.location(), e.type)

        is ApplyExpression -> ApplyExpression(subst(e.f, old, new), e.arguments.map { subst(it, old, new) }, e.type)

        is ArrayElementProjectionExpression ->
            ArrayElementProjectionExpression(subst(e.array, old, new), subst(e.index, old, new), e.type)

        is ArrayRangeProjectionExpression ->
            ArrayRangeProjectionExpression(
                subst(e.array, old, new),
                e.start?.let { subst(it, old, new) },
                e.end?.let { subst(it, old, new) },
                e.type
            )

        is BinaryExpression ->
            BinaryExpression(subst(e.e1, old, new), e.op, subst(e.e2, old, new), e.type)

        is BlockExpression ->
            BlockExpression(e.es.map { subst(it, old, new) }, e.location(), e.type)

        is CaseExpression -> CaseExpression(if (e.variable == old) new else e.variable, e.clauses.map {
            Clause(
                it.constructor,
                it.variables,
                if (it.variables.contains(old)) it.expression else subst(it.expression, old, new)
            )
        }, e.location)

        is FatBarExpression -> FatBarExpression(subst(e.left, old, new), subst(e.right, old, new), e.location)

        is IfExpression ->
            IfExpression(
                e.guards.map { Pair(subst(it.first, old, new), subst(it.second, old, new)) },
                e.elseBranch?.let { subst(it, old, new) },
                e.type
            )

        is LowerIDExpression ->
            if (e.v.value == old) LowerIDExpression(StringLocation(new, e.v.location), e.type, e.binding) else e

        is MatchExpression ->
            throw IllegalStateException("Match expressions should be desugared")

        is UnaryExpression ->
            UnaryExpression(e.op, subst(e.e, old, new), e.type)

        is LiteralBoolExpression -> e
        is LiteralCharExpression -> e
        is LiteralFloatExpression -> e
        is LiteralIntExpression -> e
        is LiteralStringExpression -> e

        else -> TODO(e.toString()) // e
    }

    fun matchVar(variables: List<String>, equations: List<Equation>, e: Expression): Expression {
        val u = variables.first()
        val us = variables.drop(1)

        return match(us, equations.map {
            val variable = (it.patterns[0] as LowerIDPattern).v.value

            Equation(
                it.patterns.drop(1),
                subst(it.body, variable, u),
                if (it.guard == null) null else subst(it.guard, variable, u)
            )
        }, e, env)
    }

    fun matchClause(
        constructor: Constructor,
        variables: List<String>,
        equations: List<Equation>,
        e: Expression
    ): Clause {
        val us = variables.drop(1)

        val kp = constructor.arity()
        val usp = List(kp) { env.makeVar() }

        return Clause(
            constructor, usp, match(usp + us, equations.map {
                val args = (it.patterns[0] as ConstructorPattern).patterns

                Equation(args + it.patterns.drop(1), it.body, it.guard)
            }, e, env)
        )
    }

    fun matchCon(variables: List<String>, equations: List<Equation>, e: Expression): Expression {
        val u = variables.first()

        fun choose(constructor: Constructor, equations: List<Equation>): List<Equation> =
            equations.filter { it.isCon() && it.getCon() == constructor }

        return FatBarExpression(
            CaseExpression(
                u,
                env.constructors(equations.first().getCon())
                    .map { constructor ->
                        matchClause(
                            constructor,
                            variables,
                            choose(constructor, equations),
                            FailExpression(e.location())
                        )
                    },
                e.location()
            ), e, e.location()
        )
    }

    fun matchVarCon(variables: List<String>, equations: List<Equation>, e: Expression): Expression = when {
        equations.first().isVar() -> matchVar(variables, equations, e)
        equations.first().isCon() -> matchCon(variables, equations, e)
        else -> throw IllegalArgumentException("First equation must be a variable or constructor: ${equations.first()}")
    }

    if (variables.isEmpty()) {
        fun combine(equation: Equation, expr: Expression): Expression {
            val guard = equation.guard

            return if (guard != null) IfExpression(
                listOf(Pair(guard, equation.body)),
                expr,
                typeBool.withLocation(e.location())
            )
            else if (canError(equation.body)) FatBarExpression(equation.body, expr, e.location())
            else equation.body
        }

        return equations.foldRight(e) { equation, expr -> combine(equation, expr) }
    }

    val partitions = partition(equations) { it.isVar() }
    return partitions.foldRight(e) { eqns, ep -> matchVarCon(variables, eqns, ep) }
}

fun fullMatch(variables: List<String>, equations: List<Equation>, e: Expression, env: PatternEnvironment): Expression {
    val eqn1 = removeLiterals(equations)
    val e2 = match(variables, eqn1, e, env)
    val e3 = tidyUpFails(e2)

    return removeUnusedVariables(e3)
}
