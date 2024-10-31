package io.littlelanguages.bendu.typeinference

data class Scheme(private val names: Set<Var>, private val type: Type) {
    fun apply(s: Subst): Scheme =
        Scheme(names, type.apply(s - names))

    fun ftv(): Set<Var> =
        type.ftv() - names

    fun instantiate(pump: Pump): Type =
        type.apply(Subst(names.toList().associateWith { pump.next() }))
}