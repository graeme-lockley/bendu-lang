package io.littlelanguages.bendu.typeinference

import io.littlelanguages.bendu.Errors
import io.littlelanguages.bendu.MultipleUnificationError
import io.littlelanguages.bendu.SingleUnificationError

typealias Constraint = Pair<Type, Type>

data class Constraints(private val constraints: MutableList<Constraint> = mutableListOf()) {
    fun add(t1: Type, t2: Type) {
        constraints.add(Pair(t1, t2))
    }

    fun solve(errors: Errors): Subst =
        solver(constraints, errors)

    override fun toString(): String = constraints.joinToString(", ") { "${it.first} ~ ${it.second}" }

//    fun clone(): Constraints =
//        Constraints(constraints.toMutableList())

    fun reset() {
        constraints.clear()
    }
}

private data class Unifier(val subst: Subst, val constraints: List<Constraint>)

private val emptyUnifier = Unifier(nullSubst, emptyList())

private fun bind(name: Var, type: Type): Unifier =
    Unifier(Subst(mapOf(Pair(name, type))), emptyList())

private fun unifies(t1: Type, t2: Type, errors: Errors): Unifier =
    when {
        t1 == t2 -> emptyUnifier
        t1.isSimilar(t2) -> emptyUnifier
        t1 is TVar -> bind(t1.name, t2)
        t2 is TVar -> bind(t2.name, t1)
        t1 is TArr && t2 is TArr -> unifyMany(listOf(t1.domain, t1.range), listOf(t2.domain, t2.range), errors)
        t1 is TTuple && t2 is TTuple -> unifyMany(t1.types, t2.types, errors)
        t1 is TCon && t2 is TCon && t1.name == t2.name -> unifyMany(t1.args, t2.args, errors)
        else -> {
            errors.addError(SingleUnificationError(t1, t2))
            emptyUnifier
        }
    }

private fun applyTypes(s: Subst, ts: List<Type>): List<Type> =
    ts.map { it.apply(s) }

private fun unifyMany(ta: List<Type>, tb: List<Type>, errors: Errors): Unifier =
    if (ta.size != tb.size) {
        errors.addError( MultipleUnificationError(ta, tb))
        emptyUnifier
    }
    else if (ta.isEmpty()) emptyUnifier
    else {
        val t1 = ta[0]
        val ts1 = ta.drop(1)

        val t2 = tb[0]
        val ts2 = tb.drop(1)

        val (su1, cs1) = unifies(t1, t2, errors)
        val (su2, cs2) = unifyMany(applyTypes(su1, ts1), applyTypes(su1, ts2), errors)

        Unifier(su2 compose su1, cs1 + cs2)
    }

private fun solver(constraints: List<Constraint>, errors: Errors): Subst {
    var su = nullSubst
    var cs = constraints.toList()

    while (cs.isNotEmpty()) {
        val (t1, t2) = cs[0]
        val cs0 = cs.drop(1)

        val (su1, cs1) = unifies(t1, t2, errors)

        su = su1 compose su
        cs = cs1 + cs0.map { Pair(it.first.apply(su1), it.second.apply(su1)) }
    }

    return su
}
