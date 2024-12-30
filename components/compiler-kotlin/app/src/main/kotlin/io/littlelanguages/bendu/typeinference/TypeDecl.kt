package io.littlelanguages.bendu.typeinference

data class TypeDecl(
    val name: String,
    val parameters: List<Var>,
    val constructors: List<Constructor>
)

data class Constructor(val name: String, val parameters: List<Type>)
