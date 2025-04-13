package io.littlelanguages.bendu.cache

import io.littlelanguages.bendu.CacheParsingError
import io.littlelanguages.bendu.Errors
import io.littlelanguages.bendu.cache.parser.*
import io.littlelanguages.bendu.typeinference.*
import io.littlelanguages.data.Tuple2
import io.littlelanguages.data.Tuple3
import io.littlelanguages.data.Tuple4
import java.io.StringReader

private class ParserVisitor :
    Visitor<List<ScriptExport>, ScriptExport, ConstructorExportData, Scheme, Unit, Type, Type> {

    val typeParameters = mutableMapOf<String, Var>()
    var typeParametersIdx = 0

    override fun visitProgram(a: Tuple2<ScriptExport, List<Tuple2<Token, ScriptExport>>>?): List<ScriptExport> =
        if (a == null) emptyList() else listOf(a.a) + a.b.map { it.b }

    override fun visitDeclaration1(
        a1: Token, a2: Token, a3: Token?, a4: Token, a5: Scheme, a6: Token, a7: Token
    ): ScriptExport =
        ValueExport(a2.lexeme, a3 != null, a5, a7.lexeme.toInt())

    override fun visitDeclaration2(
        a1: Token,
        a2: Token,
        a3: Token?,
        a4: Token,
        a5: Scheme,
        a6: Token,
        a7: Token,
        a8: Token?
    ): ScriptExport =
        FunctionExport(a2.lexeme, a3 != null, a5, a7.lexeme.toInt(), a8?.lexeme?.toInt())

    override fun visitScheme(a1: Unit?, a2: Type): Scheme =
        Scheme(typeParameters.values.toSet(), a2)

    override fun visitDeclaration3(
        a1: Token,
        a2: Token,
        a3: Unit?,
        a4: Tuple3<Token, ConstructorExportData, List<Tuple2<Token, ConstructorExportData>>>?
    ): ScriptExport =
        CustomTypeExport(
            a2.lexeme,
            false,
            typeParameters.values.toList(),
            if (a4 == null) emptyList() else (listOf(a4.b) + a4.c.map { it.b })
        )

    override fun visitTypeConstructor(
        a1: Token,
        a2: Tuple4<Token, Type, List<Tuple2<Token, Type>>, Token>?,
        a3: Token,
        a4: Token
    ): ConstructorExportData =
        ConstructorExportData(
            a1.lexeme,
            if (a2 == null) emptyList() else (listOf(a2.b) + a2.c.map { it.b }),
            a4.lexeme.toInt()
        )

    override fun visitTypeParameters(a1: Token, a2: Token, a3: List<Tuple2<Token, Token>>, a4: Token) {}

    override fun visitTypeTerm(a1: Type, a2: List<Tuple2<Token, Type>>): Type =
        if (a2.isEmpty()) a1 else TTuple(listOf(a1) + a2.map { it.b })

    override fun visitTypeFactor1(
        a1: Token, a2: Tuple2<Type, List<Tuple2<Token, Type>>>?, a3: Token, a4: Tuple2<Token, Type>?
    ): Type =
        when (a4) {
            null -> {
                when {
                    a2 == null -> typeUnit
                    a2.b.isEmpty() -> a2.a
                    else -> TODO("Syntax Error")
                }
            }

            else ->
                if (a2 == null)
                    TArr(emptyList(), a4.b)
                else
                    TArr(listOf(a2.a) + a2.b.map { it.b }, a4.b)
        }

    override fun visitTypeFactor2(a1: Token, a2: Tuple4<Token, Type, List<Tuple2<Token, Type>>, Token>?): Type =
        TCon(a1.lexeme, if (a2 == null) emptyList() else (listOf(a2.b) + a2.c.map { it.b }))

    override fun visitTypeFactor3(a: Token): Type {
        val type = typeParameters[a.lexeme]

        if (type == null) {
            typeParameters[a.lexeme] = typeParametersIdx++
        }

        return TVar(typeParameters[a.lexeme]!!)
    }
}

private fun parse(scanner: Scanner, errors: Errors): List<ScriptExport> {
    try {
        return Parser(scanner, ParserVisitor()).program()
    } catch (e: ParsingException) {
        errors.addError(CacheParsingError(e.found, e.expected))
        return emptyList()
    }
}

fun parse(input: String, errors: Errors): List<ScriptExport> =
    parse(Scanner(StringReader(input)), errors)

