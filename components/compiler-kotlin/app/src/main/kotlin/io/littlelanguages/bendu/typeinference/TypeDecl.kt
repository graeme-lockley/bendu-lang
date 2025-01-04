package io.littlelanguages.bendu.typeinference

interface CustomDataType {
    fun type(pump: Pump): Type
}

interface Constructor {
    fun constructors(): List<Constructor>
    fun parameters(): List<Type>
    fun arity(): Int
}

class TypeDecl(val name: String, val parameters: List<Var>, constructorItems: List<Pair<String, List<Type>>>) :
    CustomDataType {
    val constructors = constructorItems.map { (name, parameters) ->
        TypeDeclConstructor(this, name, parameters)
    }

    fun constructor(name: String): TypeDeclConstructor? =
        constructors.find { it.name == name }

    override fun type(pump: Pump): Type =
        TCon(name, pump.nextN(parameters.size))
}

data class TypeDeclConstructor(val typeDecl: TypeDecl, val name: String, val parameters: List<Type>) : Constructor {
    override fun constructors(): List<Constructor> =
        typeDecl.constructors

    override fun parameters(): List<Type> =
        parameters

    override fun arity(): Int =
        parameters.size
}
