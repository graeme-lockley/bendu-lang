package io.littlelanguages.bendu

import io.littlelanguages.bendu.parser.Parser
import io.littlelanguages.bendu.parser.ParsingException
import io.littlelanguages.bendu.parser.Scanner
import io.littlelanguages.bendu.parser.Token
import io.littlelanguages.bendu.parser.Visitor
import io.littlelanguages.bendu.typeinference.Type
import io.littlelanguages.data.Tuple2
import io.littlelanguages.data.Union2
import io.littlelanguages.data.Union3
import io.littlelanguages.scanpiler.Location
import io.littlelanguages.scanpiler.LocationCoordinate
import java.io.StringReader

sealed class Statement

data class LetStatement(val id: StringLocation, val e: Expression) : Statement()
data class PrintStatement(val es: List<Expression>) : Statement()
data class PrintlnStatement(val es: List<Expression>) : Statement()
data class ExpressionStatement(val e: Expression) : Statement()

sealed class Expression(open var type: Type? = null)

data class LiteralIntExpression(val v: IntLocation, override var type: Type? = null) : Expression(type)
data class LowerIDExpression(val v: StringLocation, override var type: Type? = null) : Expression(type)
data class BinaryExpression(
    val e1: Expression,
    val op: OpLocation,
    val e2: Expression,
    override var type: Type? = null
) : Expression(type)

data class IntLocation(val value: Int, val location: Location)
data class StringLocation(val value: String, val location: Location)
data class OpLocation(val op: Op, val location: Location)

enum class Op { Plus, Minus, Multiply, Divide, Modulo, Power }

private class ParserVisitor :
    Visitor<List<Statement>, Statement, Expression, Expression, Expression, Expression, Expression> {
    override fun visitProgram(a: List<Tuple2<Statement, Token?>>): List<Statement> =
        a.map { it.a }

    override fun visitStatement1(a1: Token, a2: Token, a3: Token, a4: Expression): Statement =
        LetStatement(StringLocation(a2.lexeme, a2.location), a4)

    override fun visitStatement2(
        a1: Token,
        a2: Token,
        a3: Tuple2<Expression, List<Tuple2<Token, Expression>>>?,
        a4: Token
    ): Statement =
        if (a3 == null)
            PrintStatement(emptyList())
        else
            PrintStatement(listOf(a3.a, *a3.b.map { it.b }.toTypedArray()))

    override fun visitStatement3(
        a1: Token,
        a2: Token,
        a3: Tuple2<Expression, List<Tuple2<Token, Expression>>>?,
        a4: Token
    ): Statement =
        if (a3 == null)
            PrintlnStatement(emptyList())
        else
            PrintlnStatement(listOf(a3.a, *a3.b.map { it.b }.toTypedArray()))

    override fun visitStatement4(a: Expression): Statement =
        ExpressionStatement(a)

    override fun visitExpression(a: Expression): Expression =
        a

    override fun visitAdditive(
        a1: Expression,
        a2: List<Tuple2<Union2<Token, Token>?, Expression>>
    ): Expression =
        a2.fold(a1) { acc, e ->
            val op =
                if (e.a!!.isA()) Pair(Op.Plus, e.a.a().location)
                else Pair(Op.Minus, e.a.b().location)

            BinaryExpression(acc, OpLocation(op.first, op.second), e.b)
        }

    override fun visitMultiplicative(
        a1: Expression,
        a2: List<Tuple2<Union3<Token, Token, Token>?, Expression>>
    ): Expression =
        a2.fold(a1) { acc, e ->
            val op =
                if (e.a!!.isA()) Pair(Op.Multiply, e.a.a().location)
                else if (e.a.isB()) Pair(Op.Divide, e.a.b().location)
                else Pair(Op.Modulo, e.a.c().location)

            BinaryExpression(acc, OpLocation(op.first, op.second), e.b)
        }

    override fun visitPower(
        a1: Expression,
        a2: List<Tuple2<Token, Expression>>
    ): Expression =
        a2.fold(a1) { acc, e -> BinaryExpression(acc, OpLocation(Op.Power, e.a.location), e.b) }

    override fun visitFactor1(a1: Token, a2: Expression, a3: Token): Expression =
        a2

    override fun visitFactor2(a: Token): Expression =
        LiteralIntExpression(IntLocation(a.lexeme.toInt(), a.location))

    override fun visitFactor3(a: Token): Expression =
        LowerIDExpression(StringLocation(a.lexeme, a.location))
}

fun parse(scanner: Scanner, errors: Errors): List<Statement> {
    try {
        return Parser(scanner, ParserVisitor()).program()
    } catch (e: ParsingException) {
        errors.addError(ParsingError(e.found, e.expected))
        return emptyList()
    }
}

fun parse(input: String, errors: Errors): List<Statement> =
    parse(Scanner(StringReader(input)), errors)

fun parseExpression(input: String, errors: Errors): Expression {
    try {
        return Parser(Scanner(StringReader(input)), ParserVisitor()).expression()
    } catch (e: ParsingException) {
        errors.addError(ParsingError(e.found, e.expected))
        return LiteralIntExpression(IntLocation(0, LocationCoordinate(0, 0, 0)))
    }
}
