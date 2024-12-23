package io.littlelanguages.bendu.typeinference

import io.littlelanguages.bendu.Errors
import io.littlelanguages.bendu.IdentifierRedefinitionError
import io.littlelanguages.bendu.StringLocation
import io.littlelanguages.scanpiler.Location

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