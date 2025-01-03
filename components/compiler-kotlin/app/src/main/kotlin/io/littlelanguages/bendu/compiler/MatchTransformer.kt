package io.littlelanguages.bendu.compiler

import io.littlelanguages.bendu.*
import io.littlelanguages.bendu.typeinference.*

fun transformMatchExpression(e: MatchExpression, symbolTable: SymbolTable): Expression {
    fun transform(e: Expression): Expression = when (e) {
//        is AppExpression -> AppExpression(transform(e.e1), transform(e.e2))
//        is CaseExpression -> e
//        ErrorExpression -> e
//        FailExpression -> e
//        is FatBarExpression -> FatBarExpression(transform(e.left), transform(e.right))
//        is IfExpression -> IfExpression(transform(e.e1), transform(e.e2), transform(e.e3))
//        is LBoolExpression -> e
//        is LIntExpression -> e
//        is LStringExpression -> e
//        is LTupleExpression -> LTupleExpression(e.es.map { transform(it) })
//        LUnitExpression -> e
//        is LamExpression -> LamExpression(e.n, transform(e.e))
//        is LetExpression -> LetExpression(
//            e.decls.map { Declaration(it.n, transform(it.e)) }, if (e.expr == null) null else transform(e.expr)
//        )
//
//        is LetRecExpression -> LetRecExpression(
//            e.decls.map { Declaration(it.n, transform(it.e)) }, if (e.expr == null) null else transform(e.expr)
//        )
//
//        is MatchExpression -> {
//            val matchExpression = transform(e.e)
//            val variable = if (matchExpression is VarExpression) matchExpression.name else "_x"
//
//            val match = fullMatch(
//                listOf(variable), e.cases.map { Equation(listOf(it.pattern), transform(it.expr)) }, ErrorExpression, PatternEnvironment(env)
//            )
//
//            if (matchExpression is VarExpression) match
//            else LetExpression(listOf(Declaration(variable, matchExpression)), match)
//        }
//
//        is OpExpression -> OpExpression(transform(e.e1), transform(e.e2), e.op)
//        is VarExpression -> e
        is AbortStatement -> TODO()
        is ApplyExpression -> TODO()
        is ArrayElementProjectionExpression -> TODO()
        is ArrayRangeProjectionExpression -> TODO()
        is AssignmentExpression -> TODO()

        is BinaryExpression ->
            BinaryExpression(transform(e.e1), e.op, transform(e.e2), e.type)

        is BlockExpression -> TODO()
        is CaseExpression -> TODO()
        is ErrorExpression -> TODO()
        is FailExpression -> TODO()
        is FatBarExpression -> TODO()
        is IfExpression -> TODO()
        is LetStatement -> TODO()
        is LiteralArrayExpression -> TODO()

        is LiteralBoolExpression -> e

        is LiteralCharExpression -> e

        is LiteralFloatExpression -> e

        is LiteralFunctionExpression -> TODO()

        is LiteralIntExpression -> e

        is LiteralStringExpression -> e

        is LiteralTupleExpression -> TODO()

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
        is UpperIDExpression -> TODO()
        is WhileExpression -> TODO()
    }

    return transform(e)
}

data class PatternEnvironment(private var counter: Int = 0) {
    fun arity(constructor: String): Int =
        TODO()

//        typeEnv.findConstructor(constructor)?.first?.arity ?: throw Exception("Unknown constructor: $constructor")

    fun constructors(name: String): List<String> =
        TODO()

//    if (name == TUPLE_DATA_NAME) listOf(TUPLE_DATA_NAME)
//    else typeEnv.findConstructor(name)?.second?.constructors?.map { it.name } ?: throw Exception("Unknown ADT: $name")

    fun makeVar(): String = "_u${counter++}"
}

data class Equation(val patterns: List<Pattern>, val body: Expression, val guard: Expression? = null) {
    fun isVar(): Boolean = patterns.isNotEmpty() && patterns[0] is LowerIDPattern

    fun isCon(): Boolean = patterns.isNotEmpty() && patterns[0] is ConstructorPattern

    fun getCon(): String = (patterns[0] as ConstructorPattern).id.value
}

fun canError(e: Expression): Boolean = when (e) {
//    is AppExpression -> canError(e.e1) || canError(e.e2)

    is BinaryExpression ->
        canError(e.e1) || canError(e.e2)

    is CaseExpression ->
        e.clauses.any { canError(it.expression) }

    is ErrorExpression ->
        true

    is FatBarExpression ->
        canError(e.left) || canError(e.right)

//    is IfExpression -> canError(e.e1) || canError(e.e2) || canError(e.e3)
//    is OpExpression -> canError(e.e1) || canError(e.e2)
//    is LTupleExpression -> e.es.any { canError(it) }
//    is LetExpression -> if (e.expr == null) false else canError(e.expr)
//    is LetRecExpression -> if (e.expr == null) false else canError(e.expr)

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
//            is PDataPattern -> PDataPattern(pattern.name, pattern.args.map { removeLiterals(it) })
//            is PIntPattern -> {
//                val name = nextVarName()
//                val expr = OpExpression(VarExpression(name), LIntExpression(pattern.v), Op.Equals)
//
//                guard = if (guard == null) expr else IfExpression(guard!!, expr, LBoolExpression(false))
//
//                PVarPattern(name)
//            }
//
//            is PStringPattern -> {
//                val name = nextVarName()
//                val expr = AppExpression(AppExpression(VarExpression("string_equal"), VarExpression(name)), LStringExpression(pattern.v))
//
//                guard = if (guard == null) expr else IfExpression(guard!!, expr, LBoolExpression(false))
//
//                PVarPattern(name)
//            }
//
//            is PTuplePattern -> PDataPattern(TUPLE_DATA_NAME, pattern.values.map { removeLiterals(it) })
//            is PUnitPattern -> PVarPattern(nextVarName())
//            is PVarPattern -> pattern
//            is PBoolPattern -> {
//                val name = nextVarName()
//                val expr = if (pattern.v) VarExpression(name)
//                else IfExpression(VarExpression(name), LBoolExpression(false), LBoolExpression(true))
//
//                guard = if (guard == null) expr else IfExpression(guard!!, expr, LBoolExpression(false))
//
//                PVarPattern(name)
//            }
            is ConstructorPattern -> TODO()

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

//            is PIntPattern -> {
//                val name = nextVarName()
//                val expr = OpExpression(VarExpression(name), LIntExpression(pattern.v), Op.Equals)
//
//                guard = if (guard == null) expr else IfExpression(guard!!, expr, LBoolExpression(false))
//
//                PVarPattern(name)
//            }

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
            is TuplePattern -> TODO()
            is TypedPattern -> TODO()

            // is PWildcardPattern -> PVarPattern(nextVarName())
            is WildcardPattern ->
                LowerIDPattern(StringLocation(nextVarName(), pattern.location()), pattern.type)
        }

        return Equation(equation.patterns.map { removeLiterals(it) }, equation.body, guard)
    }

    return equations.map { removeLiterals(it) }
}

fun tidyUpFails(e: Expression): Expression {
    fun replaceFailWithError(ep: Expression): Expression = when (ep) {
//        is AppExpression -> AppExpression(replaceFailWithError(ep.e1), replaceFailWithError(ep.e2))

        is CaseExpression -> CaseExpression(
            ep.variable,
            ep.clauses.map { Clause(it.constructor, it.variables, replaceFailWithError(it.expression)) },
            ep.location
        )

        is FailExpression -> ErrorExpression(ep.location)

        is FatBarExpression -> FatBarExpression(ep.left, replaceFailWithError(ep.right), ep.location)
//        is IfExpression -> IfExpression(replaceFailWithError(ep.e1), replaceFailWithError(ep.e2), replaceFailWithError(ep.e3))
//        is OpExpression -> OpExpression(replaceFailWithError(ep.e1), replaceFailWithError(ep.e2), ep.op)
//        is LTupleExpression -> LTupleExpression(ep.es.map { replaceFailWithError(it) })
//        is LamExpression -> LamExpression(ep.n, replaceFailWithError(ep.e))
//        is LetExpression -> if (ep.expr == null) ep else LetExpression(
//            ep.decls.map { Declaration(it.n, replaceFailWithError(it.e)) }, replaceFailWithError(ep.expr)
//        )
//
//        is LetRecExpression -> if (ep.expr == null) ep else LetRecExpression(
//            ep.decls.map { Declaration(it.n, replaceFailWithError(it.e)) }, replaceFailWithError(ep.expr)
//        )

        is MatchExpression -> throw IllegalStateException("Match expressions should be desugared")
        else -> TODO() // ep
    }

    return when (e) {
//        is AppExpression -> AppExpression(tidyUpFails(e.e1), tidyUpFails(e.e2))

//        is OpExpression -> OpExpression(tidyUpFails(e.e1), tidyUpFails(e.e2), e.op)
        is BinaryExpression ->
            BinaryExpression(tidyUpFails(e.e1), e.op, tidyUpFails(e.e2), e.type)

        is CaseExpression -> CaseExpression(
            e.variable,
            e.clauses.map { Clause(it.constructor, it.variables, tidyUpFails(it.expression)) }, e.location
        )

        is FatBarExpression -> when (e.right) {
            is ErrorExpression -> tidyUpFails(replaceFailWithError(e.left))
            is FailExpression -> tidyUpFails(e.left)
            else -> FatBarExpression(tidyUpFails(e.left), tidyUpFails(e.right), e.location)
        }

//        is IfExpression -> IfExpression(tidyUpFails(e.e1), tidyUpFails(e.e2), tidyUpFails(e.e3))
        is IfExpression -> IfExpression(
            e.guards.map { Pair(tidyUpFails(it.first), tidyUpFails(it.second)) },
            e.elseBranch?.let { tidyUpFails(it) },
            e.type
        )

//        is LTupleExpression -> LTupleExpression(e.es.map { tidyUpFails(it) })
//        is LamExpression -> LamExpression(e.n, tidyUpFails(e.e))
//        is LetExpression -> if (e.expr == null) e else LetExpression(e.decls.map {
//            Declaration(
//                it.n,
//                tidyUpFails(it.e)
//            )
//        }, tidyUpFails(e.expr))

//        is LetRecExpression -> if (e.expr == null) e else LetRecExpression(e.decls.map {
//            Declaration(
//                it.n,
//                tidyUpFails(it.e)
//            )
//        }, tidyUpFails(e.expr))

        is UnaryExpression ->
            UnaryExpression(e.op, tidyUpFails(e.e), e.type)

        is ErrorExpression -> e
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
//        is AppExpression -> variables(e.e1) + variables(e.e2)
        is CaseExpression -> {
            setOf(e.variable) + e.clauses.flatMap {
                variables(it.expression) - it.variables.filterNotNull().toSet()
            }.toSet()
        }

        is FatBarExpression -> variables(e.left) + variables(e.right)
//        is IfExpression -> variables(e.e1) + variables(e.e2) + variables(e.e3)
//        is OpExpression -> variables(e.e1) + variables(e.e2)
//        is VarExpression -> setOf(e.name)
//        is LTupleExpression -> e.es.flatMap { variables(it) }.toSet()
//        is LamExpression -> variables(e.e) - setOf(e.n)
//        is LetExpression -> if (e.expr == null) emptySet() else variables(e.expr) + e.decls.flatMap {
//            variables(it.e) - setOf(
//                it.n
//            )
//        }.toSet()
//
//        is LetRecExpression -> if (e.expr == null) emptySet() else variables(e.expr) + e.decls.flatMap {
//            variables(it.e) - setOf(
//                it.n
//            )
//        }.toSet()
//
        is MatchExpression -> throw IllegalStateException("Match expressions should be desugared")
        else -> TODO(e.toString()) // emptySet()
    }

    return when (e) {
//        is AppExpression -> AppExpression(removeUnusedVariables(e.e1), removeUnusedVariables(e.e2))

//      is OpExpression -> OpExpression(removeUnusedVariables(e.e1), removeUnusedVariables(e.e2), e.op)
        is BinaryExpression ->
            BinaryExpression(removeUnusedVariables(e.e1), e.op, removeUnusedVariables(e.e2), e.type)

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
//        is IfExpression -> IfExpression(
//            removeUnusedVariables(e.e1),
//            removeUnusedVariables(e.e2),
//            removeUnusedVariables(e.e3)
//        )

        is IfExpression ->
            IfExpression(
                e.guards.map { Pair(removeUnusedVariables(it.first), removeUnusedVariables(it.second)) },
                e.elseBranch?.let { removeUnusedVariables(it) },
                e.type
            )

//        is LTupleExpression -> LTupleExpression(e.es.map { removeUnusedVariables(it) })
//        is LamExpression -> LamExpression(e.n, removeUnusedVariables(e.e))
//        is LetExpression -> LetExpression(
//            e.decls.map { Declaration(it.n, removeUnusedVariables(it.e)) },
//            if (e.expr == null) null else removeUnusedVariables(e.expr)
//        )
//
//        is LetRecExpression -> LetRecExpression(
//            e.decls.map { Declaration(it.n, removeUnusedVariables(it.e)) },
//            if (e.expr == null) null else removeUnusedVariables(e.expr)
//        )

        is UnaryExpression ->
            UnaryExpression(e.op, removeUnusedVariables(e.e), e.type)

        is ErrorExpression -> e
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
//        is AppExpression -> AppExpression(subst(e.e1, old, new), subst(e.e2, old, new))

        is BinaryExpression ->
            BinaryExpression(subst(e.e1, old, new), e.op, subst(e.e2, old, new), e.type)

        is CaseExpression -> CaseExpression(if (e.variable == old) new else e.variable, e.clauses.map {
            Clause(
                it.constructor,
                it.variables,
                if (it.variables.contains(old)) it.expression else subst(it.expression, old, new)
            )
        }, e.location)

        is FatBarExpression -> FatBarExpression(subst(e.left, old, new), subst(e.right, old, new), e.location)

//        is IfExpression -> IfExpression(subst(e.e1, old, new), subst(e.e2, old, new), subst(e.e3, old, new))
//        is LTupleExpression -> LTupleExpression(e.es.map { subst(it, old, new) })
//        is LamExpression -> if (e.n == old) e else LamExpression(e.n, subst(e.e, old, new))
//        is LetExpression -> LetExpression(
//            e.decls.map { Declaration(it.n, subst(it.e, old, new)) },
//            if (e.expr == null) null else subst(e.expr, old, new)
//        )

//        is LetRecExpression -> LetRecExpression(
//            e.decls.map { Declaration(it.n, subst(it.e, old, new)) },
//            if (e.expr == null) null else subst(e.expr, old, new)
//        )

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

    fun matchClause(constructor: String, variables: List<String>, equations: List<Equation>, e: Expression): Clause {
        val us = variables.drop(1)

        val kp = env.arity(constructor)
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

        fun choose(constructor: String, equations: List<Equation>): List<Equation> =
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
                    }, e.location()
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
            ) /*IfExpression(guard, equation.body, expr)*/
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
