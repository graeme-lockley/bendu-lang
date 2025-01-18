package io.littlelanguages.bendu.compiler

import io.littlelanguages.bendu.*
import java.io.StringWriter

fun pp(s: Script): List<String> {
    val sw = StringWriter()

    render(vcat(s.es().map { pp(it) }), sw)

    return sw.toString().split("\n")
}

fun pp(e: Expression): Doc = when (e) {
    is ApplyExpression -> hcat(listOf(pp(e.f), "(", hsep(e.arguments.map(::pp), ", "), ")"))
    is BinaryExpression -> hcat(listOf("(", pp(e.e1), " ", e.op.op.toString(), " ", pp(e.e2), ")"))
    is BlockExpression -> vcat(listOf("{", nest(2, vcat(e.es.map { pp(it) })), "}"))
    is CaseExpression -> vcat(listOf(hcat(listOf("case ", e.variable, " with"))) + e.clauses.map {
        hcat(
            listOf(
                "| ",
                it.constructor.name,
                "(",
                it.variables.joinToString(", ") { it1 -> it1 ?: "_" },
                ") -> ",
                nest(2, pp(it.expression))
            )
        )
    })

    is ErrorExpression -> text("Error")
    is FatBarExpression -> hcat(listOf("[] ", pp(e.left), " => ", nest(2, pp(e.right))))
    is FailExpression -> text("fail")
    is IfExpression -> {
        fun pp(prefix: String, e: Pair<Expression, Expression>): Doc =
            hcat(listOf(prefix, pp(e.first), " -> ", nest(2, pp(e.second))))

        if (e.elseBranch == null)
            vcat(e.guards.mapIndexed { i, it -> pp(if (i == 0) "if " else " | ", it) })
        else
            vcat(e.guards.mapIndexed { i, it -> pp(if (i == 0) "if " else " | ", it) } + listOf(
                hcat(
                    listOf(
                        " | ",
                        nest(2, pp(e.elseBranch!!))
                    )
                )
            ))
    }

    is LetStatement -> vcat(e.terms.mapIndexed { i, term -> pp(term, if (i == 0) "let " else "and ") })

    is LiteralBoolExpression -> text(e.v.value.toString())
    is LiteralIntExpression -> text(e.v.value.toString())
    is LiteralStringExpression -> text("\"${e.v.value}\"")
    is LiteralTupleExpression -> hcat(listOf("Tuple(", hsep(e.es.map(::pp), ", "), ")"))
    is LowerIDExpression -> text(e.v.value)

    is MatchExpression -> vcat(listOf(hcat(listOf("match ", pp(e.e), " with"))) + e.cases.map {
        hcat(
            listOf(
                "| ",
                pp(it.pattern),
                " -> ",
                nest(2, pp(it.body))
            )
        )
    })

    is UpperIDExpression -> text(e.v.value)

    else -> text(e.toString())
}

fun pp(lst: LetStatementTerm, prefix: String): Doc =
    when (lst) {
        is LetValueStatementTerm -> hcat(
            listOf(
                prefix,
                lst.id.value,
                ": ",
                lst.type?.toString() ?: "?",
                " = ",
                nest(2, pp(lst.e))
            )
        )

        is LetFunctionStatementTerm -> hcat(listOf(prefix, lst.id.value))
    }

fun pp(p: Pattern): Doc =
    when (p) {
        is ConstructorPattern -> hcat(listOf(p.id.value, "(", hsep(p.patterns.map(::pp), ", "), ")"))
        is LiteralIntPattern -> text(p.v.value.toString())
        is WildcardPattern -> text("_")
        else -> text(p.toString())
    }
