package io.littlelanguages.bendu.typeinference

data class Scheme(private val names: Set<Var>, private val type: Type) {
    fun ftv(): Set<Var> =
        type.ftv() - names

    fun instantiate(pump: Pump): Type =
        type.apply(Subst(names.toList().associateWith { pump.next() }))

    override fun toString(): String =
        type.toString()
}