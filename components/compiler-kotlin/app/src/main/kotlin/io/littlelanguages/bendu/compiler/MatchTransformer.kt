package io.littlelanguages.bendu.compiler

import io.littlelanguages.bendu.*
import io.littlelanguages.bendu.typeinference.*

data class TransformState(var counter: Int = 0) {
    fun makeVar(): String = "_u${counter++}"
}

fun transformMatchExpression(e: MatchExpression): Expression =
    transform(e, TransformState())

fun transform(e: Expression, state: TransformState): Expression =
    when (e) {
        is AbortStatement ->
            AbortStatement(e.es.map { transform(it, state) }, e.location(), e.type)

        is ApplyExpression ->
            ApplyExpression(transform(e.f, state), e.arguments.map { transform(it, state) }, e.type)

        is ArrayElementProjectionExpression ->
            ArrayElementProjectionExpression(transform(e.array, state), transform(e.index, state), e.type)

        is ArrayRangeProjectionExpression ->
            ArrayRangeProjectionExpression(
                transform(e.array, state),
                e.start?.let { transform(it, state) },
                e.end?.let { transform(it, state) },
                e.type
            )

        is AssignmentExpression ->
            AssignmentExpression(transform(e.lhs, state), transform(e.rhs, state), e.type)

        is BinaryExpression ->
            BinaryExpression(transform(e.e1, state), e.op, transform(e.e2, state), e.type)

        is BlockExpression ->
            BlockExpression(e.es.map { transform(it, state) }, e.location(), e.type)

        is CaseExpression ->
            CaseExpression(
                e.variable,
                e.clauses.map { Clause(it.constructor, it.variables, transform(it.expression, state)) },
                e.location,
                e.type
            )

        is FatBarExpression ->
            FatBarExpression(transform(e.left, state), transform(e.right, state), e.location)

        is IfExpression ->
            IfExpression(
                e.guards.map { Pair(transform(it.first, state), transform(it.second, state)) },
                e.elseBranch?.let { transform(it, state) },
                e.type
            )

        is LetStatement ->
            LetStatement(e.terms.map {
                when (it) {
                    is LetValueStatementTerm ->
                        LetValueStatementTerm(
                            it.id,
                            it.mutable,
                            it.exported,
                            it.typeVariables,
                            it.typeQualifier,
                            transform(it.e, state),
                            it.location,
                            it.type
                        )

                    is LetFunctionStatementTerm ->
                        LetFunctionStatementTerm(
                            it.id,
                            it.mutable,
                            it.exported,
                            it.typeVariables,
                            it.parameters,
                            it.typeQualifier,
                            transform(it.body, state),
                            it.location,
                            it.type
                        )
                }
            })

        is LiteralArrayExpression ->
            LiteralArrayExpression(e.es.map { Pair(transform(it.first, state), it.second) }, e.location, e.type)

        is LiteralTupleExpression ->
            LiteralTupleExpression(e.es.map { transform(it, state) }, e.type)

        is LiteralFunctionExpression ->
            LiteralFunctionExpression(
                e.typeParameters,
                e.parameters,
                e.returnTypeQualifier,
                transform(e.body, state),
                e.type
            )

        is MatchExpression -> {
            val matchExpression = transform(e.e, state)
            val variable = if (matchExpression is LowerIDExpression) matchExpression.v.value else state.makeVar()

            val match = fullMatch(
                listOf(variable),
                e.cases.map { Equation(listOf(it.pattern), transform(it.body, state), it.guard) },
                ErrorExpression(e.location()),
                state
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
                        location = e.location(),
                        type = e.e.type
                    )

                BlockExpression(listOf(LetStatement(listOf(letValueStatementTerm)), match), e.location(), e.type)
            }
        }

        is PrintStatement ->
            PrintStatement(e.es.map { transform(it, state) }, e.location(), e.type)

        is PrintlnStatement ->
            PrintlnStatement(e.es.map { transform(it, state) }, e.location(), e.type)

        is TypedExpression ->
            TypedExpression(transform(e.e, state), e.typeQualifier, e.type)

        is UnaryExpression ->
            UnaryExpression(e.op, transform(e.e, state), e.type)

        is WhileExpression ->
            WhileExpression(transform(e.guard, state), transform(e.body, state), e.type)

        else -> e
    }

data class TuplePseudoConstructor(val arity: Int) : Constructor {
    override val name: String = "_Tuple$arity"

    override fun constructors(): List<Constructor> = listOf(this)

    override fun parameters(): List<Type> =
        List(arity) { TVar(it) }

    override fun arity(): Int =
        arity
}

data class Equation(val patterns: List<Pattern>, val body: Expression, val guard: Expression? = null) {
    fun isVar(): Boolean = patterns.isNotEmpty() && patterns[0] is LowerIDPattern
    fun isCon(): Boolean = patterns.isNotEmpty() && (patterns[0] is ConstructorPattern || patterns[0] is TuplePattern)
    fun isNamed(): Boolean = patterns.isNotEmpty() && patterns[0] is NamedPattern

    fun getVar(): String = (patterns[0] as LowerIDPattern).v.value

    fun getCon(): Constructor =
        if (patterns[0] is ConstructorPattern)
            (patterns[0] as ConstructorPattern).constructor!!
        else
            TuplePseudoConstructor((patterns[0] as TuplePattern).patterns.size)

    fun getNamed(): NamedPattern = patterns[0] as NamedPattern
}

private fun canError(e: Expression): Boolean =
    when (e) {
        is AbortStatement ->
            e.es.any { canError(it) }

        is ApplyExpression ->
            canError(e.f) || e.arguments.any { canError(it) }

        is ArrayElementProjectionExpression ->
            canError(e.array) || canError(e.index)

        is ArrayRangeProjectionExpression ->
            canError(e.array) || e.start?.let { canError(it) } == true || e.end?.let { canError(it) } == true

        is AssignmentExpression ->
            canError(e.lhs) || canError(e.rhs)

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

        is LetStatement ->
            e.terms.any {
                when (it) {
                    is LetValueStatementTerm -> canError(it.e)
                    is LetFunctionStatementTerm -> canError(it.body)
                }
            }

        is LiteralArrayExpression ->
            e.es.any { canError(it.first) }

        is LiteralFunctionExpression ->
            canError(e.body)

        is LiteralTupleExpression ->
            e.es.any { canError(it) }

        is MatchExpression ->
            canError(e.e) || e.cases.any { canError(it.body) }

        is PrintStatement ->
            e.es.any { canError(it) }

        is PrintlnStatement ->
            e.es.any { canError(it) }

        is TypedExpression ->
            canError(e.e)

        is UnaryExpression ->
            canError(e.e)

        is WhileExpression ->
            canError(e.guard) || canError(e.body)

        else ->
            false
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

private fun transformLiteralsAndTypedPatterns(equations: List<Equation>): List<Equation> {
    var counter = 0

    fun nextVarName(): String = "_l${counter++}"

    fun transformEquation(equation: Equation): Equation {
        var guard = equation.guard

        fun appendToGuard(expr: Expression) {
            guard = if (guard == null) expr else BinaryExpression(
                expr,
                OpLocation(Op.And, expr.location()),
                guard!!,
                typeBool
            )
        }

        fun transformPattern(pattern: Pattern): Pattern = when (pattern) {
            is ConstructorPattern -> ConstructorPattern(
                pattern.moduleID,
                pattern.id,
                pattern.patterns.map { transformPattern(it) },
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

            is NamedPattern ->
                NamedPattern(transformPattern(pattern.pattern), pattern.id, pattern.type)

            is TuplePattern ->
                TuplePattern(pattern.patterns.map { transformPattern(it) }, pattern.location(), pattern.type)

            is TypedPattern ->
                transformPattern(pattern.pattern)

            is WildcardPattern ->
                LowerIDPattern(StringLocation(nextVarName(), pattern.location()), pattern.type)
        }

        return Equation(equation.patterns.map { transformPattern(it) }, equation.body, guard)
    }

    return equations.map { transformEquation(it) }
}

private fun replaceFailWithE(ep: Expression, expr: Expression): Expression =
    when (ep) {
        is AbortStatement ->
            AbortStatement(ep.es.map { replaceFailWithE(it, expr) }, ep.location(), ep.type)

        is ApplyExpression ->
            ApplyExpression(replaceFailWithE(ep.f, expr), ep.arguments.map { replaceFailWithE(it, expr) }, ep.type)

        is ArrayElementProjectionExpression ->
            ArrayElementProjectionExpression(
                replaceFailWithE(ep.array, expr),
                replaceFailWithE(ep.index, expr),
                ep.type
            )

        is ArrayRangeProjectionExpression ->
            ArrayRangeProjectionExpression(
                replaceFailWithE(ep.array, expr),
                ep.start?.let { replaceFailWithE(it, expr) },
                ep.end?.let { replaceFailWithE(it, expr) },
                ep.type
            )

        is AssignmentExpression ->
            AssignmentExpression(replaceFailWithE(ep.lhs, expr), replaceFailWithE(ep.rhs, expr), ep.type)

        is BinaryExpression ->
            BinaryExpression(replaceFailWithE(ep.e1, expr), ep.op, replaceFailWithE(ep.e2, expr), ep.type)

        is BlockExpression ->
            BlockExpression(ep.es.map { replaceFailWithE(it, expr) }, ep.location(), ep.type)

        is CaseExpression ->
            CaseExpression(
                ep.variable,
                ep.clauses.map { Clause(it.constructor, it.variables, replaceFailWithE(it.expression, expr)) },
                ep.location,
                ep.type
            )

        is FailExpression ->
            expr

        is FatBarExpression ->
            FatBarExpression(ep.left, replaceFailWithE(ep.right, expr), ep.location)

        is IfExpression ->
            IfExpression(
                ep.guards.map { Pair(replaceFailWithE(it.first, expr), replaceFailWithE(it.second, expr)) },
                ep.elseBranch?.let { replaceFailWithE(it, expr) },
                ep.type
            )

        is LiteralArrayExpression ->
            LiteralArrayExpression(
                ep.es.map { Pair(replaceFailWithE(it.first, expr), it.second) },
                ep.location,
                ep.type
            )

        is MatchExpression ->
            throw IllegalStateException("Match expressions should be desugared")

        is LetStatement ->
            LetStatement(ep.terms.map {
                when (it) {
                    is LetValueStatementTerm ->
                        LetValueStatementTerm(
                            it.id,
                            it.mutable,
                            it.exported,
                            it.typeVariables,
                            it.typeQualifier,
                            replaceFailWithE(it.e, expr),
                            it.location,
                            it.type
                        )

                    is LetFunctionStatementTerm ->
                        LetFunctionStatementTerm(
                            it.id,
                            it.mutable,
                            it.exported,
                            it.typeVariables,
                            it.parameters,
                            it.typeQualifier,
                            replaceFailWithE(it.body, expr),
                            it.location,
                            it.type
                        )
                }
            })

        is LiteralFunctionExpression ->
            LiteralFunctionExpression(
                ep.typeParameters,
                ep.parameters,
                ep.returnTypeQualifier,
                replaceFailWithE(ep.body, expr),
                ep.type
            )

        is LiteralTupleExpression ->
            LiteralTupleExpression(ep.es.map { replaceFailWithE(it, expr) }, ep.type)

        is PrintStatement ->
            PrintStatement(ep.es.map { replaceFailWithE(it, expr) }, ep.location(), ep.type)

        is PrintlnStatement ->
            PrintlnStatement(ep.es.map { replaceFailWithE(it, expr) }, ep.location(), ep.type)

        is TypedExpression ->
            TypedExpression(replaceFailWithE(ep.e, expr), ep.typeQualifier, ep.type)

        is UnaryExpression ->
            UnaryExpression(ep.op, replaceFailWithE(ep.e, expr), ep.type)

        is WhileExpression ->
            WhileExpression(replaceFailWithE(ep.guard, expr), replaceFailWithE(ep.body, expr), ep.type)

        else -> ep
    }

private fun tidyUpFails(e: Expression): Expression =
    when (e) {
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

        is AssignmentExpression ->
            AssignmentExpression(tidyUpFails(e.lhs), tidyUpFails(e.rhs), e.type)

        is BinaryExpression ->
            BinaryExpression(tidyUpFails(e.e1), e.op, tidyUpFails(e.e2), e.type)

        is BlockExpression ->
            BlockExpression(e.es.map { tidyUpFails(it) }, e.location(), e.type)

        is CaseExpression -> CaseExpression(
            e.variable,
            e.clauses.map { Clause(it.constructor, it.variables, tidyUpFails(it.expression)) },
            e.location,
            e.type
        )

        is FatBarExpression -> when (e.right) {
            is ErrorExpression -> tidyUpFails(replaceFailWithE(e.left, ErrorExpression(e.right.location)))
            is FailExpression -> tidyUpFails(e.left)
            else -> replaceFailWithE(
                tidyUpFails(e.left),
                tidyUpFails(e.right)
            ) // FatBarExpression(tidyUpFails(e.left), tidyUpFails(e.right), e.location)
        }

        is IfExpression -> IfExpression(
            e.guards.map { Pair(tidyUpFails(it.first), tidyUpFails(it.second)) },
            e.elseBranch?.let { tidyUpFails(it) },
            e.type
        )

        is LetStatement ->
            LetStatement(e.terms.map {
                when (it) {
                    is LetValueStatementTerm ->
                        LetValueStatementTerm(
                            it.id,
                            it.mutable,
                            it.exported,
                            it.typeVariables,
                            it.typeQualifier,
                            tidyUpFails(it.e),
                            it.location,
                            it.type
                        )

                    is LetFunctionStatementTerm ->
                        LetFunctionStatementTerm(
                            it.id,
                            it.mutable,
                            it.exported,
                            it.typeVariables,
                            it.parameters,
                            it.typeQualifier,
                            tidyUpFails(it.body),
                            it.location,
                            it.type
                        )
                }
            })

        is LiteralArrayExpression ->
            LiteralArrayExpression(e.es.map { Pair(tidyUpFails(it.first), it.second) }, e.location, e.type)

        is MatchExpression ->
            throw IllegalStateException("Match expressions should be desugared")

        is LiteralFunctionExpression ->
            LiteralFunctionExpression(
                e.typeParameters,
                e.parameters,
                e.returnTypeQualifier,
                tidyUpFails(e.body),
                e.type
            )

        is LiteralTupleExpression ->
            LiteralTupleExpression(e.es.map { tidyUpFails(it) }, e.type)

        is PrintStatement ->
            PrintStatement(e.es.map { tidyUpFails(it) }, e.location(), e.type)

        is PrintlnStatement ->
            PrintlnStatement(e.es.map { tidyUpFails(it) }, e.location(), e.type)

        is TypedExpression ->
            TypedExpression(tidyUpFails(e.e), e.typeQualifier, e.type)

        is UnaryExpression ->
            UnaryExpression(e.op, tidyUpFails(e.e), e.type)

        is WhileExpression ->
            WhileExpression(tidyUpFails(e.guard), tidyUpFails(e.body), e.type)

        else -> e
    }

private fun variables(parameter: FunctionParameter): Set<String> =
    when (parameter) {
        is LowerIDFunctionParameter ->
            setOf(parameter.value)

        is TupleFunctionParameter ->
            parameter.parameters.flatMap { variables(it) }.toSet()

        is WildcardFunctionParameter ->
            emptySet()
    }

private fun variables(parameters: List<FunctionParameter>): Set<String> =
    parameters.flatMap { variables(it) }.toSet()

private fun variables(e: Expression): Set<String> =
    when (e) {
        is AbortStatement ->
            e.es.flatMap { variables(it) }.toSet()

        is ApplyExpression ->
            variables(e.f) + e.arguments.flatMap { variables(it) }.toSet()

        is ArrayElementProjectionExpression ->
            variables(e.array) + variables(e.index)

        is ArrayRangeProjectionExpression ->
            variables(e.array) + (e.start?.let { variables(it) } ?: emptySet()) + (e.end?.let { variables(it) }
                ?: emptySet())

        is AssignmentExpression ->
            variables(e.lhs) + variables(e.rhs)

        is BinaryExpression ->
            variables(e.e1) + variables(e.e2)

        is BlockExpression ->
            e.es.flatMap { variables(it) }.toSet()

        is CaseExpression ->
            setOf(e.variable) +
                    e.clauses.flatMap { variables(it.expression) - it.variables.filterNotNull().toSet() }.toSet()

        is FatBarExpression ->
            variables(e.left) + variables(e.right)

        is IfExpression ->
            e.guards.flatMap { variables(it.first) + variables(it.second) }.toSet() +
                    (e.elseBranch?.let { variables(it) } ?: emptySet())

        is LiteralArrayExpression ->
            e.es.flatMap { variables(it.first) }.toSet()

        is LowerIDExpression ->
            setOf(e.v.value)

        is MatchExpression ->
            throw IllegalStateException("Match expressions should be desugared")

        is LetStatement ->
            e.terms.flatMap {
                when (it) {
                    is LetValueStatementTerm ->
                        variables(it.e) - it.id.value

                    is LetFunctionStatementTerm ->
                        variables(it.body) - it.id.value - variables(it.parameters)
                }
            }.toSet()

        is LiteralFunctionExpression ->
            variables(e.body) - e.parameters.flatMap { parameter -> variables(parameter) }.toSet()

        is LiteralTupleExpression ->
            e.es.flatMap { variables(it) }.toSet()

        is PrintStatement ->
            e.es.flatMap { variables(it) }.toSet()

        is PrintlnStatement ->
            e.es.flatMap { variables(it) }.toSet()

        is TypedExpression ->
            variables(e.e)

        is UnaryExpression ->
            variables(e.e)

        is UpperIDExpression ->
            emptySet()

        is WhileExpression ->
            variables(e.guard) + variables(e.body)

        else -> emptySet()
    }

private fun removeUnusedVariables(e: Expression): Expression =
    when (e) {
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

        is AssignmentExpression ->
            AssignmentExpression(removeUnusedVariables(e.lhs), removeUnusedVariables(e.rhs), e.type)

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
        }, e.location, e.type)

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

        is LetStatement ->
            LetStatement(e.terms.map {
                when (it) {
                    is LetValueStatementTerm ->
                        LetValueStatementTerm(
                            it.id,
                            it.mutable,
                            it.exported,
                            it.typeVariables,
                            it.typeQualifier,
                            removeUnusedVariables(it.e),
                            it.location,
                            it.type
                        )

                    is LetFunctionStatementTerm ->
                        LetFunctionStatementTerm(
                            it.id,
                            it.mutable,
                            it.exported,
                            it.typeVariables,
                            it.parameters,
                            it.typeQualifier,
                            removeUnusedVariables(it.body),
                            it.location,
                            it.type
                        )
                }
            })

        is LiteralArrayExpression ->
            LiteralArrayExpression(e.es.map { Pair(removeUnusedVariables(it.first), it.second) }, e.location, e.type)

        is MatchExpression -> throw IllegalStateException("Match expressions should be desugared")

        is LiteralFunctionExpression ->
            LiteralFunctionExpression(
                e.typeParameters,
                e.parameters,
                e.returnTypeQualifier,
                removeUnusedVariables(e.body),
                e.type
            )

        is LiteralTupleExpression ->
            LiteralTupleExpression(e.es.map { removeUnusedVariables(it) }, e.type)

        is PrintStatement ->
            PrintStatement(e.es.map { removeUnusedVariables(it) }, e.location(), e.type)

        is PrintlnStatement ->
            PrintlnStatement(e.es.map { removeUnusedVariables(it) }, e.location(), e.type)

        is TypedExpression ->
            TypedExpression(removeUnusedVariables(e.e), e.typeQualifier, e.type)

        is WhileExpression ->
            WhileExpression(removeUnusedVariables(e.guard), removeUnusedVariables(e.body), e.type)

        else -> e
    }

private fun subst(e: Expression, old: String, new: String): Expression =
    when (e) {
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

        is AssignmentExpression ->
            AssignmentExpression(subst(e.lhs, old, new), subst(e.rhs, old, new), e.type)

        is BinaryExpression ->
            BinaryExpression(subst(e.e1, old, new), e.op, subst(e.e2, old, new), e.type)

        is BlockExpression ->
            BlockExpression(e.es.map { subst(it, old, new) }, e.location(), e.type)

        is CaseExpression -> CaseExpression(
            if (e.variable == old) new else e.variable,
            e.clauses.map {
                Clause(
                    it.constructor,
                    it.variables,
                    if (it.variables.contains(old)) it.expression else subst(it.expression, old, new)
                )
            },
            e.location,
            e.type
        )

        is FatBarExpression ->
            FatBarExpression(subst(e.left, old, new), subst(e.right, old, new), e.location)

        is IfExpression ->
            IfExpression(
                e.guards.map { Pair(subst(it.first, old, new), subst(it.second, old, new)) },
                e.elseBranch?.let { subst(it, old, new) },
                e.type
            )

        is LetStatement -> {
            val terms = e.terms.map {
                when (it) {
                    is LetValueStatementTerm ->
                        if (it.id.value == old)
                            it
                        else
                            LetValueStatementTerm(
                                it.id,
                                it.mutable,
                                it.exported,
                                it.typeVariables,
                                it.typeQualifier,
                                subst(it.e, old, new),
                                it.location,
                                it.type
                            )

                    is LetFunctionStatementTerm ->
                        if (it.id.value == old || variables(it.parameters).contains(old))
                            it
                        else
                            LetFunctionStatementTerm(
                                it.id,
                                it.mutable,
                                it.exported,
                                it.typeVariables,
                                it.parameters,
                                it.typeQualifier,
                                subst(it.body, old, new),
                                it.location,
                                it.type
                            )
                }
            }

            LetStatement(terms)
        }

        is LowerIDExpression ->
            if (e.v.value == old) LowerIDExpression(StringLocation(new, e.v.location), e.type, e.binding) else e

        is MatchExpression ->
            throw IllegalStateException("Match expressions should be desugared")

        is UnaryExpression ->
            UnaryExpression(e.op, subst(e.e, old, new), e.type)

        is LiteralArrayExpression ->
            LiteralArrayExpression(e.es.map { Pair(subst(it.first, old, new), it.second) }, e.location, e.type)

        is LiteralFunctionExpression ->
            if (variables(e.parameters).contains(old))
                e
            else
                LiteralFunctionExpression(
                    e.typeParameters,
                    e.parameters,
                    e.returnTypeQualifier,
                    subst(e.body, old, new),
                    e.type
                )

        is LiteralTupleExpression ->
            LiteralTupleExpression(e.es.map { subst(it, old, new) }, e.type)

        is PrintStatement ->
            PrintStatement(e.es.map { subst(it, old, new) }, e.location(), e.type)

        is PrintlnStatement ->
            PrintlnStatement(e.es.map { subst(it, old, new) }, e.location(), e.type)

        is TypedExpression ->
            TypedExpression(subst(e.e, old, new), e.typeQualifier, e.type)

        is WhileExpression ->
            WhileExpression(subst(e.guard, old, new), subst(e.body, old, new), e.type)

        else -> e
    }

private fun patternType(equations: List<Equation>): Type? =
    if (equations.isEmpty())
        null
    else if (equations.first().patterns.isEmpty())
        patternType(equations.drop(1))
    else
        equations.first().patterns[0].type

fun match(variables: List<String>, equations: List<Equation>, e: Expression, env: TransformState): Expression {
    fun matchVar(variables: List<String>, equations: List<Equation>, e: Expression): Expression {
        val u = variables.first()
        val us = variables.drop(1)

        return match(us, equations.map {
            val variable = it.getVar()

            Equation(
                it.patterns.drop(1),
                subst(it.body, variable, u),
                it.guard?.let { guard -> subst(guard, variable, u) }
            )
        }, e, env)
    }

    fun matchClause(
        constructor: Constructor,
        variables: List<String>,
        equations: List<Equation>,
        e: Expression
    ): Clause {
        fun patterns(pattern: Pattern): List<Pattern> =
            when (pattern) {
                is ConstructorPattern -> pattern.patterns
                is TuplePattern -> pattern.patterns
                else -> throw IllegalArgumentException("Pattern must be a constructor or tuple: $pattern")
            }

        val us = variables.drop(1)

        val kp = constructor.arity()
        val usp = List(kp) { env.makeVar() }

        return Clause(
            constructor, usp, match(usp + us, equations.map {
                val args = patterns(it.patterns[0])

                Equation(args + it.patterns.drop(1), it.body, it.guard)
            }, e, env)
        )
    }

    fun matchCon(variables: List<String>, equations: List<Equation>, e: Expression): Expression {
        val u = variables.first()

        fun choose(constructor: Constructor, equations: List<Equation>): List<Equation> =
            equations.filter { it.isCon() && it.getCon() == constructor }

        val left = CaseExpression(
            u,
            equations.first().getCon().constructors()
                .map { constructor ->
                    matchClause(
                        constructor,
                        variables,
                        choose(constructor, equations),
                        FailExpression(e.location())
                    )
                },
            e.location(),
            patternType(equations)
        )

        return FatBarExpression(left, e, e.location())
    }

    fun matchVarCon(variables: List<String>, equations: List<Equation>, e: Expression): Expression {
        fun matchNamed(variables: List<String>, equations: List<Equation>, e: Expression): Expression {
            val u = equations.first().getNamed()
            val us = equations.drop(1)

            val newEquation = Equation(
                listOf(u.pattern) + equations.first().patterns.drop(1),
                equations.first().body,
                equations.first().guard
            )
            val newEquations = listOf(newEquation) + us

            return BlockExpression(
                listOf(
                    LetStatement(
                        listOf(
                            LetValueStatementTerm(
                                u.id,
                                mutable = false,
                                exported = false,
                                typeVariables = emptyList(),
                                typeQualifier = null,
                                e = LowerIDExpression(StringLocation(variables[0], u.location()), u.type),
                                location = u.location(),
                                type = u.type
                            )
                        )
                    ),
                    matchVarCon(variables, newEquations, e)
                ), e.location(), e.type
            )
        }

        return when {
            equations.first().isNamed() -> matchNamed(variables, equations, e)
            equations.first().isVar() -> matchVar(variables, equations, e)
            equations.first().isCon() -> matchCon(variables, equations, e)
            else -> throw IllegalArgumentException("First equation must be a variable or constructor: ${equations.first()}")
        }
    }


    if (variables.isEmpty()) {
        fun combine(equation: Equation, expr: Expression): Expression {
            val guard = equation.guard

            return if (guard != null) {
                if (expr is IfExpression)
                    IfExpression(listOf(Pair(guard, equation.body)) + expr.guards, expr.elseBranch, expr.type)
                else
                    IfExpression(listOf(Pair(guard, equation.body)), expr, typeBool.withLocation(e.location()))
            } else if (canError(equation.body))
                FatBarExpression(equation.body, expr, e.location())
            else
                equation.body
        }

        return equations.foldRight(e) { equation, expr -> combine(equation, expr) }
    }

    val partitions = partition(equations) { it.isVar() }
    return partitions.foldRight(e) { eqns, ep -> matchVarCon(variables, eqns, ep) }
}

fun fullMatch(variables: List<String>, equations: List<Equation>, e: Expression, env: TransformState): Expression {
    val eqn1 = transformLiteralsAndTypedPatterns(equations)
    val e2 = match(variables, eqn1, e, env)
    val e3 = tidyUpFails(e2)

    return removeUnusedVariables(e3)
}
