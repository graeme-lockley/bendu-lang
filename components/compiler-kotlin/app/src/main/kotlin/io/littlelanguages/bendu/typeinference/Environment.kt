package io.littlelanguages.bendu.typeinference

import io.littlelanguages.bendu.BenduError
import io.littlelanguages.bendu.Errors
import io.littlelanguages.bendu.IdentifierRedefinitionError
import io.littlelanguages.bendu.StringLocation
import io.littlelanguages.bendu.cache.ScriptExport
import io.littlelanguages.scanpiler.Location

interface ASTTypeToTypeEnvironment {
    fun parameter(name: String): Type?
    fun typeDecl(name: String): TypeDecl?
    fun bindParameter(name: String, location: Location): TVar

    fun addError(error: BenduError)
}

data class Environment(
    private var typeEnv: TypeEnv,
    private val pump: Pump,
    val errors: Errors,
    private val constraints: Constraints
) : ASTTypeToTypeEnvironment {
    private val typeEnvs = mutableListOf(typeEnv)
    private val typeVariables = mutableListOf<MutableMap<String, Pair<Location, Type>>>(mutableMapOf())
    private val typeDecls = mutableMapOf<String, TypeDecl>()
    private val imports = mutableMapOf<String, ImportBinding>()

    init {
        typeDecls["Bool"] = TypeDecl("Bool", emptyList(), emptyList())
        typeDecls["Char"] = TypeDecl("Char", emptyList(), emptyList())
        typeDecls["Float"] = TypeDecl("Float", emptyList(), emptyList())
        typeDecls["Int"] = TypeDecl("Int", emptyList(), emptyList())
        typeDecls["String"] = TypeDecl("String", emptyList(), emptyList())
        typeDecls["Unit"] = TypeDecl("Unit", emptyList(), emptyList())
        typeDecls["Error"] = TypeDecl("Error", emptyList(), emptyList())

        typeDecls["Array"] = TypeDecl("Array", listOf(1), listOf(Constructor("Array", listOf(TVar(1)))))
    }

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

    override fun bindParameter(name: String, location: Location): TVar {
        val variables = typeVariables[typeVariables.size - 1]

        if (variables.containsKey(name)) {
            errors.addError(IdentifierRedefinitionError(StringLocation(name, location), variables[name]!!.first))
        }

        val variable = nextVar()
        variables[name] = Pair(location, variable)

        return variable
    }

    fun bindTypeDecl(name: String, typeDecl: TypeDecl) {
        typeDecls[name] = typeDecl
    }

    fun openTypeEnv() {
        typeEnvs.add(typeEnv)
        typeVariables.add(mutableMapOf())
    }

    fun closeTypeEnv() {
        typeEnv = typeEnvs.removeAt(typeEnvs.size - 1)
        typeVariables.removeAt(typeVariables.size - 1)
    }

    fun binding(name: String): Binding? = typeEnv[name]

    override fun parameter(name: String): Type? {
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

    override fun typeDecl(name: String): TypeDecl? =
        typeDecls[name]

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

    fun hasImport(value: String): Boolean =
        imports.containsKey(value)

    fun addImport(name: String, packageID: Int, declarations: List<ScriptExport>) {
        imports[name] = Pair(packageID, declarations.associateBy { it.name })
    }

    fun getImport(value: String): ImportBinding? =
        imports[value]

    override fun addError(error: BenduError) {
        errors.addError(error)
    }
}

typealias ImportBinding = Pair<Int, Map<String, ScriptExport>>