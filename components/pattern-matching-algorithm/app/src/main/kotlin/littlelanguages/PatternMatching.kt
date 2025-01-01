package littlelanguages

typealias Variable = String
typealias Constructor = String

sealed class Pattern
data class VarPattern(val variable: Variable) : Pattern()
data class LiteralPattern(val value: String) : Pattern()
data class ConPattern(val constructor: Constructor, val args: List<Pattern>) : Pattern()

data class AbstractDataType(val name: String, val constructors: List<Pair<Constructor, Int>>)

data class Environment(private val types: List<AbstractDataType>) {
    fun arity(constructor: Constructor): Int =
        types.flatMap { it.constructors }.first { it.first == constructor }.second

    fun constructors(name: String): List<Constructor> =
        types.first { it.constructors.any { c -> c.first == name } }.constructors.map { it.first }

    private var counter = 0

    fun makeVar(): Variable = "_u${counter++}"
}

sealed class Expression

data class And(val left: Expression, val right: Expression) : Expression()

data class App(val function: Expression, val argument: Expression) : Expression()

data class Case(val variable: Variable, val clauses: List<Clause>) : Expression()

data class Eq(val left: Expression, val right: Expression) : Expression()

object ERROR : Expression()
object FAIL : Expression()

data class FATBAR(val left: Expression, val right: Expression) : Expression()

data class If(val condition: Expression, val thenBranch: Expression, val elseBranch: Expression) : Expression()

data class Literal(val value: String) : Expression()

data class Var(val variable: Variable) : Expression()

fun prettyPrint(e: Expression, pretty: Boolean = false): String {
    val sb = StringBuilder()

    fun pp(e: Expression, indent: Int) {
        when (e) {
            is And -> {
                sb.append("(")
                pp(e.left, indent)
                sb.append(" && ")
                pp(e.right, indent)
                sb.append(")")
            }

            is App -> {
                sb.append("(")
                pp(e.function, indent)
                sb.append(" ")
                pp(e.argument, indent)
                sb.append(")")
            }

            is Case -> {
                sb.append("case ")
                sb.append(e.variable)
                if (pretty)
                    sb.append(" of\n")
                else
                    sb.append(" of")
                e.clauses.forEachIndexed { index, clause ->
                    if (pretty)
                        for (i in 0 until indent) sb.append("    ")
                    if (index > 0 || pretty)
                        sb.append(" | ")
                    else
                        sb.append(" ")
                    sb.append(clause.constructor)
                    sb.append(clause.variables.joinToString("") { " $it" })
                    sb.append(" -> ")
                    pp(clause.expression, indent + 1)
                    if (pretty)
                        sb.append("\n")
                }
            }

            is Eq -> {
                sb.append("(")
                pp(e.left, indent)
                sb.append(" == ")
                pp(e.right, indent)
                sb.append(")")
            }

            ERROR -> sb.append("ERROR")

            FAIL -> sb.append("FAIL")

            is FATBAR -> {
                sb.append("(")
                pp(e.left, indent)
                if (pretty)
                    for (i in 0 until indent) sb.append("    ")
                sb.append(") [] ")
                pp(e.right, indent)
            }

            is If -> {
                sb.append("if ")
                pp(e.condition, indent)
                sb.append(" then ")
                pp(e.thenBranch, indent)
                sb.append(" else ")
                pp(e.elseBranch, indent)
            }

            is Literal -> sb.append("\"${e.value}\"")

            is Var -> sb.append(e.variable)
        }
    }

    pp(e, 0)

    return sb.toString()
}

data class Clause(val constructor: Constructor, val variables: List<Variable?>, val expression: Expression)

data class Equation(val patterns: List<Pattern>, val body: Expression, val guard: Expression? = null) {
    fun isVar(): Boolean =
        patterns.isNotEmpty() && patterns[0] is VarPattern

    fun isCon(): Boolean =
        patterns.isNotEmpty() && patterns[0] is ConPattern

    fun getCon(): String =
        (patterns[0] as ConPattern).constructor
}

fun canError(e: Expression): Boolean =
    when (e) {
        is And -> canError(e.left) || canError(e.right)
        is App -> false
        is Case -> e.clauses.any { canError(it.expression) }
        is Eq -> false
        ERROR -> true
        FAIL -> false
        is FATBAR -> canError(e.left) || canError(e.right)
        is If -> canError(e.condition) || canError(e.thenBranch) || canError(e.elseBranch)
        is Literal -> false
        is Var -> false
    }

fun <T> partition(list: List<T>, predicate: (T) -> Boolean): List<List<T>> {
    fun combine(e: T, acc: List<List<T>>): List<List<T>> {
        if (acc.isEmpty())
            return listOf(listOf(e))

        val v = acc.first().first()

        if (predicate(v) == predicate(e))
            return listOf(listOf(e) + acc.first()) + acc.drop(1)

        return listOf(listOf(e)) + acc
    }

    return list.foldRight(emptyList()) { e, acc -> combine(e, acc) }
}

fun removeLiterals(equations: List<Equation>): List<Equation> {
    var counter = 0

    fun removeLiterals(equation: Equation): Equation {
        var guard = equation.guard

        fun removeLiterals(pattern: Pattern): Pattern =
            when (pattern) {
                is ConPattern -> ConPattern(pattern.constructor, pattern.args.map { removeLiterals(it) })
                is LiteralPattern -> {
                    val n = "_l${counter++}"

                    guard = if (guard == null)
                        Eq(Var(n), Literal(pattern.value))
                    else
                        And(Eq(Var(n), Literal(pattern.value)), guard!!)

                    VarPattern(n)
                }

                is VarPattern -> pattern
            }

        return Equation(equation.patterns.map { removeLiterals(it) }, equation.body, guard)
    }

    return equations.map { removeLiterals(it) }
}

fun tidyUpFails(e: Expression): Expression {
    fun replaceFailWithError(ep: Expression): Expression =
        when (ep) {
            is And -> And(replaceFailWithError(ep.left), replaceFailWithError(ep.right))
            is App -> App(replaceFailWithError(ep.function), replaceFailWithError(ep.argument))
            is Case -> Case(ep.variable, ep.clauses.map { Clause(it.constructor, it.variables, replaceFailWithError(it.expression)) })
            is Eq -> Eq(replaceFailWithError(ep.left), replaceFailWithError(ep.right))
            ERROR -> ERROR
            FAIL -> ERROR
            is FATBAR -> FATBAR(ep.left, replaceFailWithError(ep.right))
            is If -> If(replaceFailWithError(ep.condition), replaceFailWithError(ep.thenBranch), replaceFailWithError(ep.elseBranch))
            is Literal -> ep
            is Var -> ep
        }

    return when (e) {
        is And -> And(tidyUpFails(e.left), tidyUpFails(e.right))
        is App -> App(tidyUpFails(e.function), tidyUpFails(e.argument))
        is Case -> Case(e.variable, e.clauses.map { Clause(it.constructor, it.variables, tidyUpFails(it.expression)) })
        is Eq -> Eq(tidyUpFails(e.left), tidyUpFails(e.right))
        ERROR -> ERROR
        FAIL -> FAIL
        is FATBAR -> {
            when (e.right) {
                ERROR -> tidyUpFails(replaceFailWithError(e.left))
                FAIL -> tidyUpFails(e.left)
                else -> FATBAR(tidyUpFails(e.left), tidyUpFails(e.right))
            }
        }

        is If -> If(tidyUpFails(e.condition), tidyUpFails(e.thenBranch), tidyUpFails(e.elseBranch))
        is Literal -> e
        is Var -> e
    }
}

fun removeUnusedVariables(e: Expression): Expression {
    fun variables(e: Expression): Set<Variable> =
        when (e) {
            is And -> variables(e.left) + variables(e.right)
            is App -> variables(e.function) + variables(e.argument)
            is Case -> {
                setOf(e.variable) + e.clauses.flatMap {
                    variables(it.expression) - it.variables.filterNotNull().toSet()
                }.toSet()
            }

            is Eq -> variables(e.left) + variables(e.right)
            ERROR -> emptySet()
            FAIL -> emptySet()
            is FATBAR -> variables(e.left) + variables(e.right)
            is If -> variables(e.condition) + variables(e.thenBranch) + variables(e.elseBranch)
            is Literal -> emptySet()
            is Var -> setOf(e.variable)
        }

    return when (e) {
        is And -> And(removeUnusedVariables(e.left), removeUnusedVariables(e.right))
        is App -> App(removeUnusedVariables(e.function), removeUnusedVariables(e.argument))
        is Case ->
            Case(e.variable, e.clauses.map {
                val vs = variables(it.expression)
                Clause(it.constructor, it.variables.map { v -> if (vs.contains(v)) v else null }, removeUnusedVariables(it.expression))
            })

        is Eq -> Eq(removeUnusedVariables(e.left), removeUnusedVariables(e.right))
        ERROR -> ERROR
        FAIL -> FAIL
        is FATBAR -> FATBAR(removeUnusedVariables(e.left), removeUnusedVariables(e.right))
        is If -> If(removeUnusedVariables(e.condition), removeUnusedVariables(e.thenBranch), removeUnusedVariables(e.elseBranch))
        is Literal -> e
        is Var -> e
    }
}


fun match(variables: List<Variable>, equations: List<Equation>, e: Expression, env: Environment): Expression {
    fun subst(e: Expression, old: Variable, new: Variable): Expression =
        when (e) {
            is And -> And(subst(e.left, old, new), subst(e.right, old, new))
            is App -> App(subst(e.function, old, new), subst(e.argument, old, new))
            is Case -> Case(
                if (e.variable == old) new else e.variable,
                e.clauses.map {
                    Clause(
                        it.constructor,
                        it.variables,
                        if (it.variables.contains(old)) it.expression else subst(it.expression, old, new)
                    )
                }
            )

            is Eq -> Eq(subst(e.left, old, new), subst(e.right, old, new))
            ERROR -> ERROR
            FAIL -> FAIL
            is FATBAR -> FATBAR(subst(e.left, old, new), subst(e.right, old, new))
            is If -> If(subst(e.condition, old, new), subst(e.thenBranch, old, new), subst(e.elseBranch, old, new))
            is Literal -> e
            is Var -> if (e.variable == old) Var(new) else e
        }

    fun matchVar(variables: List<Variable>, equations: List<Equation>, e: Expression): Expression {
        val u = variables.first()
        val us = variables.drop(1)

        return match(us, equations.map {
            val variable = (it.patterns[0] as VarPattern).variable

            Equation(it.patterns.drop(1), subst(it.body, variable, u), if (it.guard == null) null else subst(it.guard, variable, u))
        }, e, env)
    }

    fun matchClause(constructor: Constructor, variables: List<Variable>, equations: List<Equation>, e: Expression): Clause {
        val us = variables.drop(1)

        val kp = env.arity(constructor)
        val usp = List(kp) { env.makeVar() }

        return Clause(
            constructor,
            usp,
            match(usp + us, equations.map {
                val args = (it.patterns[0] as ConPattern).args

                Equation(args + it.patterns.drop(1), it.body, it.guard)
            }, e, env)
        )
    }

    fun matchCon(variables: List<Variable>, equations: List<Equation>, e: Expression): Expression {
        val u = variables.first()

        fun choose(constructor: Constructor, equations: List<Equation>): List<Equation> =
            equations.filter { it.isCon() && it.getCon() == constructor }

        return FATBAR(
            Case(
                u,
                env.constructors(equations.first().getCon())
                    .map { constructor -> matchClause(constructor, variables, choose(constructor, equations), FAIL) }), e
        )
    }

    fun matchVarCon(variables: List<Variable>, equations: List<Equation>, e: Expression): Expression =
        when {
            equations.first().isVar() -> matchVar(variables, equations, e)
            equations.first().isCon() -> matchCon(variables, equations, e)
            else -> throw IllegalArgumentException("First equation must be a variable or constructor: ${equations.first()}")
        }

    if (variables.isEmpty()) {
        fun combine(equation: Equation, expr: Expression): Expression {
            val guard = equation.guard

            return if (guard != null)
                If(guard, equation.body, expr)
            else if (canError(equation.body))
                FATBAR(equation.body, expr)
            else
                equation.body
        }

        return equations.foldRight(e) { equation, expr -> combine(equation, expr) }
    }

    val partitions = partition(equations) { it.isVar() }
    return partitions.foldRight(e) { eqns, ep -> matchVarCon(variables, eqns, ep) }
}

fun fullMatch(variables: List<Variable>, equations: List<Equation>, e: Expression, env: Environment): Expression =
    removeUnusedVariables(tidyUpFails(match(variables, removeLiterals(equations), e, env)))
