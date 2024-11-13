package io.littlelanguages.bendu

import io.littlelanguages.bendu.parser.Parser
import io.littlelanguages.bendu.parser.ParsingException
import io.littlelanguages.bendu.parser.Scanner
import io.littlelanguages.bendu.parser.TToken
import io.littlelanguages.bendu.parser.Token
import io.littlelanguages.bendu.parser.Visitor
import io.littlelanguages.bendu.typeinference.Subst
import io.littlelanguages.bendu.typeinference.Type
import io.littlelanguages.data.Tuple2
import io.littlelanguages.data.Tuple3
import io.littlelanguages.data.Tuple4
import io.littlelanguages.data.Union2
import io.littlelanguages.data.Union3
import io.littlelanguages.scanpiler.Location
import java.io.StringReader
import kotlin.collections.fold

sealed class Statement

data class LetStatement(val id: StringLocation, val e: Expression) : Statement()
data class PrintStatement(val es: List<Expression>) : Statement()
data class PrintlnStatement(val es: List<Expression>) : Statement()
data class ExpressionStatement(val e: Expression) : Statement()

sealed class Expression(open var type: Type? = null) {
    open fun apply(s: Subst, errors: Errors) {
        type = type!!.apply(s)
    }

    abstract fun location(): Location
}

data class IfExpression(
    val guards: List<Pair<Expression, Expression>>,
    val elseBranch: Expression?,
    override var type: Type? = null
) : Expression(type) {
    override fun apply(s: Subst, errors: Errors) {
        guards.forEach { (e1, e2) ->
            e1.apply(s, errors)
            e2.apply(s, errors)
        }
        elseBranch?.apply(s, errors)

        type = guards[0].second.type
    }

    override fun location(): Location {
        val result = guards.drop(1)
            .fold(guards.first().first.location() + guards.first().second.location()) { acc, (e1, e2) -> acc + e1.location() + e2.location() }

        return if (elseBranch == null) result else elseBranch.location() + result
    }
}

data class WhileExpression(val guard: Expression, val body: Expression, override var type: Type? = null) :
    Expression(type) {
    override fun apply(s: Subst, errors: Errors) {
        guard.apply(s, errors)
        body.apply(s, errors)
    }

    override fun location(): Location =
        guard.location() + body.location()
}

data class LiteralBoolExpression(val v: BoolLocation, override var type: Type? = null) : Expression(type) {
    override fun location(): Location =
        v.location
}

data class LiteralCharExpression(val v: CharLocation, override var type: Type? = null) : Expression(type) {
    override fun location(): Location =
        v.location
}

data class LiteralIntExpression(val v: IntLocation, override var type: Type? = null) : Expression(type) {
    override fun location(): Location =
        v.location
}

data class LiteralFloatExpression(val v: FloatLocation, override var type: Type? = null) : Expression(type) {
    override fun location(): Location =
        v.location
}

data class LiteralStringExpression(val v: StringLocation, override var type: Type? = null) : Expression(type) {
    override fun location(): Location =
        v.location
}

data class LiteralUnitExpression(val location: Location, override var type: Type? = null) : Expression(type) {
    override fun location(): Location =
        location
}

data class LowerIDExpression(val v: StringLocation, override var type: Type? = null) : Expression(type) {
    override fun location(): Location =
        v.location
}

data class BinaryExpression(
    val e1: Expression,
    val op: OpLocation,
    val e2: Expression,
    override var type: Type? = null
) : Expression(type) {
    override fun apply(s: Subst, errors: Errors) {
        super.apply(s, errors)
        e1.apply(s, errors)
        e2.apply(s, errors)
    }

    override fun location(): Location =
        e1.location() + e2.location()
}

data class UnaryExpression(
    val op: UnaryOpLocation,
    val e: Expression,
    override var type: Type? = null
) : Expression(type) {
    override fun apply(s: Subst, errors: Errors) {
        super.apply(s, errors)
        e.apply(s, errors)
    }

    override fun location(): Location =
        op.location + e.location()
}

data class BoolLocation(val value: Boolean, val location: Location)
data class CharLocation(val value: Char, val location: Location)
data class FloatLocation(val value: Float, val location: Location)
data class IntLocation(val value: Int, val location: Location)
data class StringLocation(val value: String, val location: Location)
data class OpLocation(val op: Op, val location: Location)
data class UnaryOpLocation(val op: UnaryOp, val location: Location)

enum class Op { Or, And, Plus, Minus, Multiply, Divide, Modulo, Power, EqualEqual, NotEqual, LessThan, LessEqual, GreaterThan, GreaterEqual }
enum class UnaryOp { Not }

private class ParserVisitor(val errors: Errors = Errors()) :
    Visitor<List<Statement>, Statement, Expression, Expression, Expression, Expression, Expression, OpLocation, Expression, Expression, Expression, Expression> {
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

    override fun visitExpression1(
        a1: Token,
        a2: Token?,
        a3: Expression,
        a4: Token,
        a5: Expression,
        a6: List<Tuple3<Token, Expression, Tuple2<Token, Expression>?>>
    ): Expression {
        val guards = mutableListOf(Pair(a3, a5))
        var elseExpression: Expression? = null

        a6.forEachIndexed { i, (a, b, c) ->
            if (c == null) {
                elseExpression = b
                if (i != a6.size - 1) {
                    errors.addError(ParsingError(a, setOf(TToken.TBar)))
                }
            } else {
                guards.add(Pair(b, c.b))
            }
        }

        return IfExpression(guards, elseExpression)
    }

    override fun visitExpression2(a1: Token, a2: Expression, a3: Token, a4: Expression): Expression =
        WhileExpression(a2, a4)

    override fun visitExpression3(a: Expression): Expression =
        a

    override fun visitOrExpression(a: Expression): Expression =
        a

    override fun visitOr(
        a1: Expression,
        a2: List<Tuple2<Token, Expression>>
    ): Expression =
        a2.fold(a1) { acc, e -> BinaryExpression(acc, OpLocation(Op.Or, e.a.location), e.b) }

    override fun visitAnd(
        a1: Expression,
        a2: List<Tuple2<Token, Expression>>
    ): Expression =
        a2.fold(a1) { acc, e -> BinaryExpression(acc, OpLocation(Op.And, e.a.location), e.b) }

    override fun visitEquality(
        a1: Expression,
        a2: Tuple2<OpLocation, Expression>?
    ): Expression =
        if (a2 == null) a1 else BinaryExpression(a1, a2.a, a2.b)

    override fun visitRelOp1(a: Token): OpLocation =
        OpLocation(Op.EqualEqual, a.location)

    override fun visitRelOp2(a: Token): OpLocation =
        OpLocation(Op.NotEqual, a.location)

    override fun visitRelOp3(a: Token): OpLocation =
        OpLocation(Op.LessThan, a.location)

    override fun visitRelOp4(a: Token): OpLocation =
        OpLocation(Op.LessEqual, a.location)

    override fun visitRelOp5(a: Token): OpLocation =
        OpLocation(Op.GreaterThan, a.location)

    override fun visitRelOp6(a: Token): OpLocation =
        OpLocation(Op.GreaterEqual, a.location)

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

    override fun visitFactor1(a1: Token, a2: Expression?, a3: Token): Expression =
        a2 ?: LiteralUnitExpression(a1.location + a3.location)

    override fun visitFactor2(a: Token): Expression =
        LowerIDExpression(StringLocation(a.lexeme, a.location))

    override fun visitFactor3(a: Token): Expression {
        if (a.lexeme.length == 3) {
            return LiteralCharExpression(CharLocation(a.lexeme[1], a.location))
        } else if (a.lexeme == "'\\n'") {
            return LiteralCharExpression(CharLocation('\n', a.location))
        } else if (a.lexeme == "'\\\\'") {
            return LiteralCharExpression(CharLocation('\\', a.location))
        } else {
            return LiteralCharExpression(CharLocation('\'', a.location))
        }
    }

    override fun visitFactor4(a: Token): Expression =
        try {
            LiteralFloatExpression(FloatLocation(a.lexeme.toFloat(), a.location))
        } catch (_: NumberFormatException) {
            errors.addError(InvalidLiteralError(a.lexeme, a.location))
            LiteralFloatExpression(FloatLocation(Float.MAX_VALUE, a.location))
        }

    override fun visitFactor5(a: Token): Expression =
        try {
            LiteralIntExpression(IntLocation(a.lexeme.toInt(), a.location))
        } catch (_: NumberFormatException) {
            errors.addError(InvalidLiteralError(a.lexeme, a.location))
            LiteralIntExpression(IntLocation(Int.MAX_VALUE, a.location))
        }

    override fun visitFactor6(a: Token): Expression {
        val sb = StringBuilder()

        var lp = 1
        while (true) {
            val c = a.lexeme[lp]
            if (c == '"') {
                break
            } else if (c == '\\') {
                val nc = a.lexeme[lp + 1]

                if (nc == 'x') {
                    lp += 2
                    val start = lp
                    while (a.lexeme[lp] != ';') {
                        lp += 1
                    }
                    val code = a.lexeme.substring(start, lp)
                    sb.append(code.toInt().toChar())
                    lp += 1
                } else {
                    when (nc) {
                        'n' -> sb.append('\n')
                        '\\' -> sb.append('\\')
                        '"' -> sb.append('"')
                    }
                    lp += 2
                }
            } else {
                sb.append(c)
                lp += 1
            }
        }
        return LiteralStringExpression(StringLocation(sb.toString(), a.location))
    }

    override fun visitFactor7(a: Token): Expression =
        LiteralBoolExpression(BoolLocation(true, a.location))

    override fun visitFactor8(a: Token): Expression =
        LiteralBoolExpression(BoolLocation(false, a.location))

    override fun visitFactor9(a1: Token, a2: Expression): Expression =
        UnaryExpression(UnaryOpLocation(UnaryOp.Not, a1.location), a2)
}

fun parse(scanner: Scanner, errors: Errors): List<Statement> {
    try {
        return Parser(scanner, ParserVisitor(errors)).program()
    } catch (e: ParsingException) {
        errors.addError(ParsingError(e.found, e.expected))
        return emptyList()
    }
}

fun parse(input: String, errors: Errors): List<Statement> =
    parse(Scanner(StringReader(input)), errors)
