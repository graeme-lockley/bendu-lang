package io.littlelanguages.bendu.typeinference

data class TypeDecl(val name: String, val parameters: List<Var>, val constructors: List<Constructor>) {
    fun constructor(name: String): Constructor? =
        constructors.find { it.name == name }

    fun type(pump: Pump): Type =
        TCon(name, pump.nextN(parameters.size))
}

data class Constructor(val name: String, val parameters: List<Type>)
