package io.littlelanguages.bendu

import io.littlelanguages.bendu.parser.Parser
import io.littlelanguages.bendu.parser.Scanner
import io.littlelanguages.bendu.parser.Token
import io.littlelanguages.bendu.parser.Visitor
import io.littlelanguages.data.Tuple2
import io.littlelanguages.scanpiler.Location
import java.io.StringReader

sealed class Statement

data class LetStatement(val id: StringLocation, val e: Expression) : Statement()

sealed class Expression

data class LiteralIntExpression(val v: IntLocation) : Expression()
data class LowerIDExpression(val v: StringLocation) : Expression()

data class IntLocation(val value: Int, val location: Location)
data class StringLocation(val value: String, val location: Location)

class ParserVisitor : Visitor<List<Statement>, Statement, Expression, Expression> {
    override fun visitProgram(a: List<Tuple2<Statement, Token?>>): List<Statement> {
        return a.map { it.a }
    }

    override fun visitStatement(a1: Token, a2: Token, a3: Token, a4: Expression): Statement {
        return LetStatement(StringLocation(a2.lexeme, a2.location), a4)
    }

    override fun visitExpression(a: Expression): Expression {
        return a
    }

    override fun visitFactor1(a1: Token, a2: Expression, a3: Token): Expression {
        return a2
    }

    override fun visitFactor2(a: Token): Expression {
        return LiteralIntExpression(IntLocation(a.lexeme.toInt(), a.location))
    }

    override fun visitFactor3(a: Token): Expression {
        return LowerIDExpression(StringLocation(a.lexeme, a.location))
    }
}

fun parse(scanner: Scanner): List<Statement> =
    Parser(scanner, ParserVisitor()).program()

fun parse(input: String): List<Statement> =
    parse(Scanner(StringReader(input)))
