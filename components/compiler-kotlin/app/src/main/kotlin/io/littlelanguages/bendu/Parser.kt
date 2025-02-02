package io.littlelanguages.bendu

import io.littlelanguages.bendu.parser.*
import io.littlelanguages.data.*
import java.io.StringReader

private class ParserVisitor(val errors: Errors = Errors()) :
    Visitor<Script, Import, ImportDeclaration, Declaration, TypeDeclaration, TypeConstructor, Expression, LetStatementTerm, Expression, Expression, Expression, Expression, OpLocation, Expression, OpLocation, Expression, Expression, Expression, Expression, Expression, Expression, (Expression) -> Expression, Expression, List<FunctionParameter>, FunctionParameter, List<StringLocation>, TypeTerm, TypeTerm, TypeTerm, MatchCase, Pattern, Pattern> {
    override fun visitProgram(a1: List<Tuple2<Import, Token?>>, a2: List<Tuple2<Declaration, Token?>>): Script =
        Script(a1.map { it.a }, a2.map { it.a })

    override fun visitImportStatement(
        a1: Token,
        a2: Token,
        a3: Tuple2<Token, Token>?,
        a4: Tuple5<Token, Token, ImportDeclaration, List<Tuple2<Token, ImportDeclaration>>, Token>?
    ): Import =
        if (a3 == null && a4 == null)
            ImportAll(StringLocation(parseLiteralString(a2.lexeme), a2.location), a1.location + a2.location)
        else
            ImportList(
                StringLocation(parseLiteralString(a2.lexeme), a2.location), a1.location + a2.location,
                a3?.let { StringLocation(it.b.lexeme, it.b.location) },
                if (a4 == null) emptyList() else listOf(a4.c) + a4.d.map { it.b }
            )

    override fun visitImportDeclaration1(a1: Token, a2: Tuple2<Token, Token>?): ImportDeclaration =
        ImportDeclaration(
            StringLocation(a1.lexeme, a1.location),
            a2?.let { StringLocation(it.b.lexeme, it.b.location) })

    override fun visitImportDeclaration2(a1: Token, a2: Tuple2<Token, Token>?): ImportDeclaration =
        ImportDeclaration(
            StringLocation(a1.lexeme, a1.location),
            a2?.let { StringLocation(it.b.lexeme, it.b.location) })

    override fun visitDeclaration1(
        a1: Token,
        a2: TypeDeclaration,
        a3: List<Tuple2<Token, TypeDeclaration>>
    ): Declaration =
        DeclarationType(listOf(a2) + a3.map { it.b })

    override fun visitDeclaration2(a: Expression): Declaration =
        DeclarationExpression(a)

    override fun visitTypeDeclaration(
        a1: Token,
        a2: Union2<Token, Token>?,
        a3: List<StringLocation>?,
        a4: Token,
        a5: TypeConstructor,
        a6: List<Tuple2<Token, TypeConstructor>>
    ): TypeDeclaration =
        TypeDeclaration(
            StringLocation(a1.lexeme, a1.location),
            if (a2 == null) TypeVisibility.Private else if (a2.isA()) TypeVisibility.Public else TypeVisibility.Opaque,
            a3 ?: emptyList(),
            listOf(a5) + a6.map { it.b })

    override fun visitTypeConstructor(
        a1: Token,
        a2: Tuple4<Token, TypeTerm, List<Tuple2<Token, TypeTerm>>, Token>?
    ): TypeConstructor =
        if (a2 == null)
            TypeConstructor(StringLocation(a1.lexeme, a1.location), emptyList())
        else
            TypeConstructor(StringLocation(a1.lexeme, a1.location), listOf(a2.b) + a2.c.map { it.b })

    override fun visitExpression1(
        a1: Token,
        a2: LetStatementTerm,
        a3: List<Tuple2<Token, LetStatementTerm>>
    ): Expression =
        LetStatement(listOf(a2) + a3.map { it.b })

    override fun visitExpression2(
        a1: Token,
        a2: Token,
        a3: Tuple2<Expression, List<Tuple2<Token, Expression>>>?,
        a4: Token
    ): Expression =
        if (a3 == null)
            PrintStatement(emptyList(), a1.location + a4.location)
        else
            PrintStatement(listOf(a3.a, *a3.b.map { it.b }.toTypedArray()), a1.location + a4.location)

    override fun visitExpression3(
        a1: Token,
        a2: Token,
        a3: Tuple2<Expression, List<Tuple2<Token, Expression>>>?,
        a4: Token
    ): Expression =
        if (a3 == null)
            PrintlnStatement(emptyList(), a1.location + a4.location)
        else
            PrintlnStatement(listOf(a3.a, *a3.b.map { it.b }.toTypedArray()), a1.location + a4.location)

    override fun visitExpression4(
        a1: Token,
        a2: Token,
        a3: Tuple2<Expression, List<Tuple2<Token, Expression>>>?,
        a4: Token
    ): Expression =
        if (a3 == null)
            AbortStatement(emptyList(), a1.location + a4.location)
        else
            AbortStatement(listOf(a3.a, *a3.b.map { it.b }.toTypedArray()), a1.location + a4.location)

    override fun visitExpression5(
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

    override fun visitExpression6(a1: Token, a2: Expression, a3: Token, a4: Expression): Expression =
        WhileExpression(a2, a4)

    override fun visitExpression7(a: Expression): Expression =
        a

    override fun visitLetDeclaration(
        a1: Token,
        a2: Token?,
        a3: Token?,
        a4: List<StringLocation>?,
        a5: List<FunctionParameter>?,
        a6: TypeTerm?,
        a7: Token,
        a8: Expression
    ): LetStatementTerm =
        if (a5 == null)
            LetValueStatementTerm(
                StringLocation(a1.lexeme, a1.location),
                a2 != null,
                a3 != null,
                a4 ?: emptyList(),
                a6,
                a8,
                a1.location + a8.location()
            )
        else
            LetFunctionStatementTerm(
                StringLocation(a1.lexeme, a1.location),
                a2 != null,
                a3 != null,
                a4 ?: emptyList(),
                a5,
                a6,
                a8,
                a1.location + a8.location()
            )

    override fun visitOrExpression(a: Expression): Expression =
        a

    override fun visitOr(
        a1: Expression,
        a2: List<Tuple2<Token, Expression>>
    ): Expression =
        a2.fold(a1) { acc, e -> BinaryExpression(acc, OpLocation(Op.Or, e.a.location), e.b) }

    override fun visitAndE(
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
        a2: List<Tuple2<Union2<Token, Token>, Expression>>
    ): Expression =
        a2.fold(a1) { acc, e ->
            val op =
                if (e.a.isA()) Pair(Op.Plus, e.a.a().location)
                else Pair(Op.Minus, e.a.b().location)

            BinaryExpression(acc, OpLocation(op.first, op.second), e.b)
        }

    override fun visitStarpendOp1(a: Token): OpLocation =
        OpLocation(Op.GreaterGreater, a.location)

    override fun visitStarpendOp2(a: Token): OpLocation =
        OpLocation(Op.GreaterBang, a.location)

    override fun visitStarpendOp3(a: Token): OpLocation =
        OpLocation(Op.LessLess, a.location)

    override fun visitStarpendOp4(a: Token): OpLocation =
        OpLocation(Op.LessBang, a.location)

    override fun visitStarpend(a1: Expression, a2: List<Tuple2<OpLocation, Expression>>): Expression =
        a2.fold(a1) { acc, e -> BinaryExpression(acc, e.a, e.b) }

    override fun visitMultiplicative(
        a1: Expression,
        a2: List<Tuple2<Union3<Token, Token, Token>, Expression>>
    ): Expression =
        a2.fold(a1) { acc, e ->
            val op =
                if (e.a.isA()) Pair(Op.Multiply, e.a.a().location)
                else if (e.a.isB()) Pair(Op.Divide, e.a.b().location)
                else Pair(Op.Modulo, e.a.c().location)

            BinaryExpression(acc, OpLocation(op.first, op.second), e.b)
        }

    override fun visitPower(
        a1: Expression,
        a2: List<Tuple2<Token, Expression>>
    ): Expression =
        a2.fold(a1) { acc, e -> BinaryExpression(acc, OpLocation(Op.Power, e.a.location), e.b) }

    override fun visitTypedExpression(a1: Expression, a2: Tuple2<Token, TypeTerm>?): Expression =
        if (a2 == null)
            a1
        else
            TypedExpression(a1, a2.b)

    override fun visitAssignment(a1: Expression, a2: Tuple2<Token, Expression>?): Expression =
        if (a2 == null)
            a1
        else
            AssignmentExpression(a1, a2.b)

    override fun visitQualifiedExpression(a1: Expression, a2: List<(Expression) -> Expression>): Expression =
        a2.fold(a1) { acc, e -> e(acc) }

    override fun visitQualifiedExpressionSuffix1(
        a1: Token,
        a2: Tuple2<Expression, List<Tuple2<Token, Expression>>>?,
        a3: Token
    ): (Expression) -> Expression =
        { it: Expression -> ApplyExpression(it, if (a2 == null) emptyList() else listOf(a2.a) + a2.b.map { it.b }) }

    override fun visitQualifiedExpressionSuffix2(
        a1: Token,
        a2: Union2<Tuple2<Expression, Tuple2<Token, Expression?>?>, Tuple2<Token, Expression?>>
    ): (Expression) -> Expression =
        { it: Expression ->
            if (a2.isA()) {
                if (a2.a().b == null)
                    ArrayElementProjectionExpression(it, a2.a().a)
                else
                    ArrayRangeProjectionExpression(it, a2.a().a, a2.a().b?.b)
            } else {
                ArrayRangeProjectionExpression(it, null, a2.b().b)
            }
        }

    override fun visitFactor1(
        a1: Token,
        a2: Tuple2<Expression, List<Tuple2<Token, Expression>>>?,
        a3: Token
    ): Expression =
        when {
            a2 == null -> LiteralUnitExpression(a1.location + a3.location)
            a2.b.isEmpty() -> a2.a
            else -> LiteralTupleExpression(listOf(a2.a) + a2.b.map { it.b })
        }

    override fun visitFactor2(a: Token): Expression =
        LowerIDExpression(StringLocation(a.lexeme, a.location))

    override fun visitFactor3(a1: Token, a2: Tuple2<Token, Union2<Token, Token>>?): Expression =
        if (a2 == null)
            UpperIDExpression(StringLocation(a1.lexeme, a1.location))
        else if (a2.b.isA())
            ModuleReferenceExpression(
                StringLocation(a1.lexeme, a1.location),
                StringLocation(a2.b.a().lexeme, a2.b.a().location)
            )
        else
            ModuleReferenceExpression(
                StringLocation(a1.lexeme, a1.location),
                StringLocation(a2.b.b().lexeme, a2.b.b().location)
            )

    override fun visitFactor4(a: Token): Expression =
        when {
            a.lexeme.length == 3 -> LiteralCharExpression(CharLocation(a.lexeme[1], a.location))
            a.lexeme == "'\\n'" -> LiteralCharExpression(CharLocation('\n', a.location))
            a.lexeme == "'\\\\'" -> LiteralCharExpression(CharLocation('\\', a.location))
            else -> LiteralCharExpression(CharLocation('\'', a.location))
        }

    override fun visitFactor5(a: Token): Expression =
        try {
            LiteralFloatExpression(FloatLocation(a.lexeme.toFloat(), a.location))
        } catch (_: NumberFormatException) {
            errors.addError(InvalidLiteralError(a.lexeme, a.location))
            LiteralFloatExpression(FloatLocation(Float.MAX_VALUE, a.location))
        }

    override fun visitFactor6(a: Token): Expression =
        try {
            LiteralIntExpression(IntLocation(a.lexeme.toInt(), a.location))
        } catch (_: NumberFormatException) {
            errors.addError(InvalidLiteralError(a.lexeme, a.location))
            LiteralIntExpression(IntLocation(Int.MAX_VALUE, a.location))
        }

    override fun visitFactor7(a: Token): Expression =
        LiteralStringExpression(StringLocation(parseLiteralString(a.lexeme), a.location))

    override fun visitFactor8(a: Token): Expression =
        LiteralBoolExpression(BoolLocation(true, a.location))

    override fun visitFactor9(a: Token): Expression =
        LiteralBoolExpression(BoolLocation(false, a.location))

    override fun visitFactor10(a1: Token, a2: Expression): Expression =
        UnaryExpression(UnaryOpLocation(UnaryOp.Not, a1.location), a2)

    override fun visitFactor11(
        a1: Token,
        a2: Token,
        a3: Token,
        a4: Tuple2<Expression, List<Tuple2<Token, Expression>>>?,
        a5: Token
    ): Expression =
        BuiltinExpression(
            StringLocation(parseLiteralString(a2.lexeme), a2.location),
            a4?.let { listOf(a4.a) + a4.b.map { it.b } } ?: emptyList())

    override fun visitFactor12(a1: Token, a2: Expression): Expression =
        UnaryExpression(UnaryOpLocation(UnaryOp.TypeOf, a1.location), a2)

    override fun visitFactor13(
        a1: Token,
        a2: List<StringLocation>?,
        a3: List<FunctionParameter>,
        a4: TypeTerm?,
        a5: Token?,
        a6: Expression
    ): Expression =
        LiteralFunctionExpression(a2 ?: emptyList(), a3, a4, a6)

    override fun visitFactor14(a1: Token, a2: List<Tuple2<Expression, Token?>>, a3: Token): Expression =
        BlockExpression(a2.map { it.a }, a1.location + a3.location)

    override fun visitFactor15(
        a1: Token,
        a2: Tuple3<Token?, Expression, List<Tuple3<Token, Token?, Expression>>>?,
        a3: Token
    ): Expression =
        if (a2 == null)
            LiteralArrayExpression(emptyList(), a1.location + a3.location)
        else
            LiteralArrayExpression(
                listOf(Pair(a2.b, a2.a != null)) + a2.c.map { Pair(it.c, it.b != null) },
                a1.location + a3.location
            )

    override fun visitFactor16(
        a1: Token,
        a2: Expression,
        a3: Token,
        a4: Token?,
        a5: MatchCase,
        a6: List<Tuple2<Token, MatchCase>>
    ): Expression =
        MatchExpression(a2, listOf(a5) + a6.map { it.b })

    override fun visitFunctionParameters(
        a1: Token,
        a2: Tuple2<FunctionParameter, List<Tuple2<Token, FunctionParameter>>>?,
        a3: Token
    ): List<FunctionParameter> =
        if (a2 == null)
            emptyList()
        else
            listOf(a2.a) + a2.b.map { it.b }

    override fun visitFunctionParameter1(a1: Token, a2: Token?, a3: TypeTerm?): FunctionParameter =
        LowerIDFunctionParameter(a1.lexeme, a1.location, a2 != null, a3)

    override fun visitFunctionParameter2(a1: Token, a2: TypeTerm?): FunctionParameter =
        WildcardFunctionParameter(a2, a1.location)

    override fun visitFunctionParameter3(
        a1: Token,
        a2: FunctionParameter,
        a3: List<Tuple2<Token, FunctionParameter>>,
        a4: Token,
        a5: TypeTerm?
    ): FunctionParameter =
        TupleFunctionParameter(listOf(a2) + a3.map { it.b }, a5, a1.location + (a5?.location() ?: a4.location))


    override fun visitTypeParameters(
        a1: Token,
        a2: Token,
        a3: List<Tuple2<Token, Token>>,
        a4: Token
    ): List<StringLocation> =
        listOf(StringLocation(a2.lexeme, a2.location)) +
                a3.map { StringLocation(it.b.lexeme, it.b.location) }

    override fun visitTypeQualifier(a1: Token, a2: TypeTerm): TypeTerm =
        a2

    override fun visitTypeTerm(a1: TypeTerm, a2: List<Tuple2<Token, TypeTerm>>): TypeTerm =
        if (a2.isEmpty())
            a1
        else
            TupleType(listOf(a1) + a2.map { it.b }, a1.location() + a2.last().b.location())

    override fun visitTypeFactor1(
        a1: Token,
        a2: Tuple2<TypeTerm, List<Tuple2<Token, TypeTerm>>>?,
        a3: Token,
        a4: Tuple2<Token, TypeTerm>?
    ): TypeTerm =
        when (a4) {
            null -> {
                when {
                    a2 == null -> UpperIDType(
                        StringLocation("Unit", a1.location),
                        emptyList(),
                        a1.location + a3.location
                    )

                    a2.b.isEmpty() -> a2.a
                    else -> TODO("Syntax Error")
                }
            }

            else -> FunctionType(listOf(a2!!.a) + a2.b.map { it.b }, a4.b, a1.location + a4.b.location())
        }

    override fun visitTypeFactor2(
        a1: Token,
        a2: Tuple4<Token, TypeTerm, List<Tuple2<Token, TypeTerm>>, Token>?
    ): TypeTerm =
        if (a2 == null)
            UpperIDType(StringLocation(a1.lexeme, a1.location), emptyList(), a1.location)
        else
            UpperIDType(
                StringLocation(a1.lexeme, a1.location),
                listOf(a2.b) + a2.c.map { it.b },
                a1.location + a2.d.location
            )

    override fun visitTypeFactor3(a: Token): TypeTerm =
        LowerIDType(StringLocation(a.lexeme, a.location))

    override fun visitCase(a1: Pattern, a2: Tuple2<Token, Expression>?, a3: Token, a4: Expression): MatchCase =
        MatchCase(a1, a2?.b, a4)

    override fun visitPattern(a1: Pattern, a2: TypeTerm?, a3: Tuple2<Token, Token>?): Pattern {
        var result = a1

        if (a2 != null) {
            result = TypedPattern(result, a2)
        }
        if (a3 != null) {
            result = NamedPattern(result, StringLocation(a3.b.lexeme, a3.b.location))
        }

        return result
    }

    override fun visitPatternFactor1(
        a1: Token,
        a2: Tuple2<Pattern, List<Tuple2<Token, Pattern>>>?,
        a3: Token
    ): Pattern =
        if (a2 == null) LiteralUnitPattern(a1.location + a3.location)
        else if (a2.b.isEmpty()) a2.a
        else TuplePattern(listOf(a2.a) + a2.b.map { it.b }, a1.location + a3.location)

    override fun visitPatternFactor2(a: Token): Pattern =
        LiteralCharPattern(CharLocation(a.lexeme[1], a.location))

    override fun visitPatternFactor3(a: Token): Pattern =
        LiteralFloatPattern(FloatLocation(a.lexeme.toFloat(), a.location))

    override fun visitPatternFactor4(a: Token): Pattern =
        LiteralIntPattern(IntLocation(a.lexeme.toInt(), a.location))

    override fun visitPatternFactor5(a: Token): Pattern =
        LiteralStringPattern(StringLocation(parseLiteralString(a.lexeme), a.location))

    override fun visitPatternFactor6(a: Token): Pattern =
        LiteralBoolPattern(BoolLocation(true, a.location))

    override fun visitPatternFactor7(a: Token): Pattern =
        LiteralBoolPattern(BoolLocation(false, a.location))

    override fun visitPatternFactor8(a: Token): Pattern =
        LowerIDPattern(StringLocation(a.lexeme, a.location))

    override fun visitPatternFactor9(a: Token): Pattern =
        WildcardPattern(a.location)

    override fun visitPatternFactor10(
        a1: Token,
        a2: Tuple2<Token, Token>?,
        a3: Token,
        a4: Tuple2<Pattern, List<Tuple2<Token, Pattern>>>?,
        a5: Token
    ): Pattern {
        val arguments = if (a4 == null) emptyList() else listOf(a4.a) + a4.b.map { it.b }
        val location = a1.location + a5.location

        return if (a2 == null)
            ConstructorPattern(null, StringLocation(a1.lexeme, a1.location), arguments, location)
        else
            ConstructorPattern(
                StringLocation(a1.lexeme, a1.location),
                StringLocation(a2.b.lexeme, a2.b.location),
                arguments,
                location
            )
    }
}

fun parseLiteralString(s: String): String {
    val sb = StringBuilder()

    var lp = 1
    while (true) {
        val c = s[lp]
        if (c == '"') {
            break
        } else if (c == '\\') {
            val nc = s[lp + 1]

            if (nc == 'x') {
                lp += 2
                val start = lp
                while (s[lp] != ';') {
                    lp += 1
                }
                val code = s.substring(start, lp)
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

    return sb.toString()
}

fun parse(scanner: Scanner, errors: Errors): Script {
    try {
        return Parser(scanner, ParserVisitor(errors)).program()
    } catch (e: ParsingException) {
        errors.addError(ParsingError(e.found, e.expected))
        return Script(emptyList(), emptyList())
    }
}

fun parse(input: String, errors: Errors): Script =
    parse(Scanner(StringReader(input)), errors)
