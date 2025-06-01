package io.littlelanguages.minibendu

import io.littlelanguages.minibendu.parser.*
import io.littlelanguages.data.Tuple2
import io.littlelanguages.data.Tuple3
import java.io.StringReader


/**
 * ParserVisitor for mini-bendu that converts the parse tree into an AST
 */
class ParserVisitor(val errors: Errors = Errors()) :
    Visitor<Program, TopLevel, TypeAliasDecl, ExprStmt, Expr, Expr, List<Parameter>, Parameter, Expr,
            Expr, Expr, Expr, BinaryOp, Expr, BinaryOp,
            Expr, BinaryOp, Expr, Expr, Expr, MatchCase,
            Expr, Expr, Expr, Expr, SpreadOrField, TypeExpr, TypeExpr,
            TypeExpr, TypeExpr, BaseTypeExpr, ParserVisitor.GenericArgs, TypeExpr, ParserVisitor.RecordBodyType, TypeField,
            TypeExpr, List<TypeParam>, TypeParam, Pattern, Pattern, RecordPattern, FieldPattern,
            VarPattern, WildcardPattern, LiteralPattern> {

    // Program
    override fun visitProgram(a: List<TopLevel>): Program =
        Program(a)

    // TopLevel
    override fun visitTopLevel1(a: TypeAliasDecl): TopLevel = a
    override fun visitTopLevel2(a: ExprStmt): TopLevel = a

    // TypeAliasDecl
    override fun visitTypeAliasDecl(
        a1: Token,
        a2: Token,
        a3: List<TypeParam>?,
        a4: Token,
        a5: TypeExpr
    ): TypeAliasDecl =
        TypeAliasDecl(
            StringLocation(a2.lexeme, a2.location),
            a3,
            a5
        )

    // ExprStmt
    override fun visitExprStmt(a: Expr): ExprStmt = ExprStmt(a)

    // Expr
    override fun visitExpr1(a: Expr): Expr = a
    override fun visitExpr2(a: Expr): Expr = a
    override fun visitExpr3(a: Expr): Expr = a
    override fun visitExpr4(a: Expr): Expr = a
    override fun visitExpr5(a: Expr): Expr = a

    // LetExpr
    override fun visitLetExpr(
        a1: Token,
        a2: Token?,
        a3: Token,
        a4: List<TypeParam>?,
        a5: List<Parameter>?,
        a6: Tuple2<Token, TypeExpr>?,
        a7: Token,
        a8: Expr,
        a9: Tuple2<Token, Expr>?
    ): Expr =
        LetExpr(
            recursive = a2 != null,
            id = StringLocation(a3.lexeme, a3.location),
            typeParams = a4,
            parameters = a5,
            typeAnnotation = a6?.b,
            value = a8,
            body = a9?.b
        )

    override fun visitParameters(
        a1: Token,
        a2: Tuple2<Parameter, List<Tuple2<Token, Parameter>>>?,
        a3: Token
    ): List<Parameter> =
        if (a2 == null) emptyList() else listOf(a2.a) + a2.b.map { it.b }

    override fun visitParameterType(
        a1: Token,
        a2: Tuple2<Token, TypeExpr>?
    ): Parameter =
        Parameter(id = StringLocation(a1.lexeme, a1.location), type = a2?.b)

    // LambdaExpr
    override fun visitLambdaExpr(
        a1: Token, a2: List<TypeParam>?, a3: Token,
        a4: Tuple2<Token, TypeExpr>?, a5: Token, a6: Expr
    ): Expr =
        LambdaExpr(
            typeParams = a2,
            param = StringLocation(a3.lexeme, a3.location),
            paramType = a4?.b,
            body = a6,
            location = a1.location
        )

    // Binary Operators (LogicalOr, LogicalAnd, Equality, Additive, Multiplicative)
    override fun visitLogicalOrExpr(a1: Expr, a2: List<Tuple2<Token, Expr>>): Expr =
        if (a2.isEmpty()) {
            a1
        } else {
            a2.fold(a1) { acc, (token, right) ->
                BinaryOpExpr(acc, BinaryOp.Or, right, acc.location() + right.location())
            }
        }

    override fun visitLogicalAndExpr(a1: Expr, a2: List<Tuple2<Token, Expr>>): Expr =
        if (a2.isEmpty()) {
            a1
        } else {
            a2.fold(a1) { acc, (token, right) ->
                BinaryOpExpr(acc, BinaryOp.And, right, acc.location() + right.location())
            }
        }

    override fun visitEqualityExpr(a1: Expr, a2: List<Tuple2<BinaryOp, Expr>>): Expr =
        if (a2.isEmpty()) {
            a1
        } else {
            a2.fold(a1) { acc, (op, right) ->
                BinaryOpExpr(acc, op, right, acc.location() + right.location())
            }
        }

    override fun visitEqualityOp1(a: Token): BinaryOp = BinaryOp.EqualEqual
    override fun visitEqualityOp2(a: Token): BinaryOp = BinaryOp.NotEqual

    override fun visitAdditiveExpr(a1: Expr, a2: List<Tuple2<BinaryOp, Expr>>): Expr =
        if (a2.isEmpty()) {
            a1
        } else {
            a2.fold(a1) { acc, (op, right) ->
                BinaryOpExpr(acc, op, right, acc.location() + right.location())
            }
        }

    override fun visitAdditiveOp1(a: Token): BinaryOp = BinaryOp.Plus
    override fun visitAdditiveOp2(a: Token): BinaryOp = BinaryOp.Minus

    override fun visitMultiplicativeExpr(a1: Expr, a2: List<Tuple2<BinaryOp, Expr>>): Expr =
        if (a2.isEmpty())
            a1
        else
            a2.fold(a1) { acc, (op, right) ->
                BinaryOpExpr(acc, op, right, acc.location() + right.location())
            }

    override fun visitMultiplicativeOp1(a: Token): BinaryOp = BinaryOp.Star
    override fun visitMultiplicativeOp2(a: Token): BinaryOp = BinaryOp.Slash

    // ApplicationExpr
    override fun visitApplicationExpr(
        a1: Expr,
        a2: List<Tuple3<Token, Tuple2<Expr, List<Tuple2<Token, Expr>>>?, Token>>
    ): Expr =
        if (a2.isEmpty())
            a1
        else
            a2.fold(a1) { acc, (token, args, right) ->
                ApplicationExpr(
                    function = acc,
                    arguments = if (args == null) emptyList() else listOf(args.a) + args.b.map { it.b },
                    location = acc.location() + right.location
                )
            }

    // IfExpr
    override fun visitIfExpr(a1: Token, a2: Expr, a3: Token, a4: Expr, a5: Token, a6: Expr): Expr =
        IfExpr(a2, a4, a6, a1.location)

    // MatchExpr
    override fun visitMatchExpr(
        a1: Token,
        a2: Expr,
        a3: Token,
        a4: MatchCase,
        a5: List<Tuple2<Token, MatchCase>>
    ): Expr =
        MatchExpr(a2, listOf(a4) + a5.map { it.b }, a1.location)

    // MatchCase
    override fun visitMatchCase(a1: Pattern, a2: Token, a3: Expr): MatchCase =
        MatchCase(a1, a3)

    // SimpleExpr
    override fun visitSimpleExpr(a1: Expr, a2: List<Tuple2<Token, Token>>): Expr =
        a2.fold(a1) { acc, (token, field) ->
            ProjectionExpr(acc, StringLocation(field.lexeme, field.location), acc.location() + field.location)
        }

    // PrimaryExpr
    override fun visitPrimaryExpr1(a: Token): Expr =
        try {
            LiteralIntExpr(IntLocation(a.lexeme.toInt(), a.location))
        } catch (_: NumberFormatException) {
            errors.addError(InvalidLiteralError(a.lexeme, a.location))
            LiteralIntExpr(IntLocation(Int.MAX_VALUE, a.location))
        }

    override fun visitPrimaryExpr2(a: Token): Expr =
        LiteralStringExpr(StringLocation(parseLiteralString(a.lexeme), a.location))

    override fun visitPrimaryExpr3(a: Token): Expr =
        LiteralBoolExpr(BoolLocation(true, a.location))

    override fun visitPrimaryExpr4(a: Token): Expr =
        LiteralBoolExpr(BoolLocation(false, a.location))

    override fun visitPrimaryExpr5(a: Expr): Expr = a

    override fun visitPrimaryExpr6(a: Expr): Expr = a

    override fun visitPrimaryExpr7(a1: Token, a2: Expr, a3: List<Tuple2<Token, Expr>>, a4: Token): Expr =
        if (a3.isEmpty())
            a2  // A single expression in parentheses
        else
            TupleExpr(listOf(a2) + a3.map { it.b }, a1.location + a4.location)

    // Var
    override fun visitVariable(a: Token): Expr =
        VarExpr(StringLocation(a.lexeme, a.location))

    // Record
    override fun visitRecord(
        a1: Token,
        a2: Tuple2<SpreadOrField, List<Tuple2<Token, SpreadOrField>>>?,
        a3: Token
    ): Expr =
        RecordExpr(
            if (a2 == null) {
                emptyList()
            } else {
                listOf(a2.a) + a2.b.map { it.b }
            },
            a1.location + a3.location
        )

    // SpreadOrField
    override fun visitSpreadOrField1(a1: Token, a2: Token, a3: Expr): SpreadOrField =
        FieldExpr(StringLocation(a1.lexeme, a1.location), a3)

    override fun visitSpreadOrField2(a1: Token, a2: Expr): SpreadOrField =
        SpreadExpr(a2)

    // TypeExpr
    override fun visitTypeExpr(a1: TypeExpr, a2: Tuple2<Token, TypeExpr>?): TypeExpr =
        if (a2 == null) {
            a1
        } else {
            FunctionTypeExpr(a1, a2.b, a1.location() + a2.b.location())
        }

    // MergeType
    override fun visitMergeType(a1: TypeExpr, a2: List<Tuple2<Token, TypeExpr>>): TypeExpr =
        if (a2.isEmpty()) {
            a1
        } else {
            a2.fold(a1) { acc, (token, right) ->
                MergeTypeExpr(acc, right, acc.location() + right.location())
            }
        }

    // UnionType
    override fun visitUnionType(a1: TypeExpr, a2: List<Tuple2<Token, TypeExpr>>): TypeExpr =
        if (a2.isEmpty()) {
            a1
        } else {
            a2.fold(a1) { acc, (token, right) ->
                UnionTypeExpr(acc, right, acc.location() + right.location())
            }
        }

    // TypeExpr
    override fun visitPrimaryType1(a: BaseTypeExpr): TypeExpr = a
    override fun visitPrimaryType2(a: TypeExpr): TypeExpr = a
    override fun visitPrimaryType3(a: TypeExpr): TypeExpr = a
    override fun visitPrimaryType4(a: Token): TypeExpr =
        LiteralStringTypeExpr(StringLocation(parseLiteralString(a.lexeme), a.location))

    // BaseType
    override fun visitBaseType(a1: Token, a2: GenericArgs?): BaseTypeExpr =
        BaseTypeExpr(
            StringLocation(a1.lexeme, a1.location),
            a2?.types
        )

    // GenericArgs
    override fun visitGenericArgs(a1: Token, a2: TypeExpr, a3: List<Tuple2<Token, TypeExpr>>, a4: Token): GenericArgs =
        GenericArgs(listOf(a2) + a3.map { it.b })

    // RecordBodyType
    override fun visitRecordBodyType1(a1: Token, a2: Token): RecordBodyType {
        // "..." UpperID - only extension
        return RecordBodyType.OnlyExtension(StringLocation(a2.lexeme, a2.location))
    }

    override fun visitRecordBodyType2(
        a1: TypeField,
        a2: Tuple2<Token, RecordBodyType>?
    ): RecordBodyType {
        // TypeField ["," RecordBodyType] - field with optional rest
        return RecordBodyType.FieldWithOptionalRecordBodyType(a1, a2?.b)
    }

    // RecordType
    override fun visitRecordType(a1: Token, a2: RecordBodyType?, a3: Token): TypeExpr {
        return when (a2) {
            null -> {
                // Empty record type: {}
                RecordTypeExpr(
                    fields = emptyList(),
                    extension = null,
                    location = a1.location + a3.location
                )
            }

            is RecordBodyType.OnlyExtension -> {
                // Only extension: { ...A }
                RecordTypeExpr(
                    fields = emptyList(),
                    extension = a2.extension,
                    location = a1.location + a3.location
                )
            }

            is RecordBodyType.FieldWithOptionalRecordBodyType -> {
                // Field with optional rest: { name: String } or { name: String, ...A } or { name: String, age: Int }
                val (fields, extension) = collectFieldsAndExtension(a2)
                RecordTypeExpr(
                    fields = fields,
                    extension = extension,
                    location = a1.location + a3.location
                )
            }
        }
    }

    private fun collectFieldsAndExtension(recordBodyType: RecordBodyType.FieldWithOptionalRecordBodyType): Pair<List<TypeField>, StringLocation?> {
        val fields = mutableListOf<TypeField>()
        var current: RecordBodyType? = recordBodyType

        while (current != null) {
            when (current) {
                is RecordBodyType.OnlyExtension -> {
                    return Pair(fields, current.extension)
                }

                is RecordBodyType.FieldWithOptionalRecordBodyType -> {
                    fields.add(current.field)
                    current = current.rest
                }
            }
        }

        return Pair(fields, null)
    }

    // TypeField
    override fun visitTypeField(a1: Token, a2: Token, a3: TypeExpr): TypeField =
        TypeField(StringLocation(a1.lexeme, a1.location), a3)

    // TupleType
    override fun visitTupleType(a1: Token, a2: TypeExpr, a3: List<Tuple2<Token, TypeExpr>>, a4: Token): TypeExpr =
        TupleTypeExpr(
            listOf(a2) + a3.map { it.b },
            a1.location + a4.location
        )

    // TypeParams
    override fun visitTypeParams(
        a1: Token,
        a2: TypeParam,
        a3: List<Tuple2<Token, TypeParam>>,
        a4: Token
    ): List<TypeParam> =
        listOf(a2) + a3.map { it.b }

    // TypeParam
    override fun visitTypeParam(a1: Token, a2: Tuple2<Token, TypeExpr>?): TypeParam =
        TypeParam(
            StringLocation(a1.lexeme, a1.location),
            a2?.b
        )

    // Pattern - handles BasePattern [":" TypeExpr]
    override fun visitPattern(a1: Pattern, a2: Tuple2<Token, TypeExpr>?): Pattern {
        val typeAnnotation = a2?.b
        return when (a1) {
            is RecordPattern -> a1.copy(typeAnnotation = typeAnnotation)
            is VarPattern -> a1.copy(typeAnnotation = typeAnnotation)
            is WildcardPattern -> a1.copy(typeAnnotation = typeAnnotation)
            is LiteralIntPattern -> a1.copy(typeAnnotation = typeAnnotation)
            is LiteralStringPattern -> a1.copy(typeAnnotation = typeAnnotation)
            is LiteralBoolPattern -> a1.copy(typeAnnotation = typeAnnotation)
            is TuplePattern -> a1.copy(typeAnnotation = typeAnnotation)
        }
    }

    // BasePattern alternatives
    override fun visitBasePattern1(a: RecordPattern): Pattern = a
    override fun visitBasePattern2(a: VarPattern): Pattern = a
    override fun visitBasePattern3(a: WildcardPattern): Pattern = a
    override fun visitBasePattern4(a: LiteralPattern): Pattern = a
    override fun visitBasePattern5(a1: Token, a2: Pattern, a3: List<Tuple2<Token, Pattern>>, a4: Token): Pattern =
        TuplePattern(
            listOf(a2) + a3.map { it.b },
            a1.location + a4.location
        )

    // RecordPattern
    override fun visitRecordPattern(
        a1: Token,
        a2: Tuple2<FieldPattern, List<Tuple2<Token, FieldPattern>>>?,
        a3: Token
    ): RecordPattern =
        RecordPattern(
            if (a2 == null) emptyList() else listOf(a2.a) + a2.b.map { it.b },
            a1.location + a3.location
        )

    // FieldPattern
    override fun visitFieldPattern(a1: Token, a2: Token, a3: Pattern): FieldPattern =
        FieldPattern(StringLocation(a1.lexeme, a1.location), a3)

    // VarPattern
    override fun visitVarPattern(a: Token): VarPattern =
        VarPattern(StringLocation(a.lexeme, a.location))

    // Wildcard
    override fun visitWildcard(a: Token): WildcardPattern =
        WildcardPattern(a.location)

    // LiteralPattern
    override fun visitLiteralPattern1(a: Token): LiteralPattern =
        try {
            LiteralIntPattern(IntLocation(a.lexeme.toInt(), a.location))
        } catch (_: NumberFormatException) {
            errors.addError(InvalidLiteralError(a.lexeme, a.location))
            LiteralIntPattern(IntLocation(Int.MAX_VALUE, a.location))
        }

    override fun visitLiteralPattern2(a: Token): LiteralPattern =
        LiteralStringPattern(StringLocation(parseLiteralString(a.lexeme), a.location))

    override fun visitLiteralPattern3(a: Token): LiteralPattern =
        LiteralBoolPattern(BoolLocation(true, a.location))

    override fun visitLiteralPattern4(a: Token): LiteralPattern =
        LiteralBoolPattern(BoolLocation(false, a.location))

    // Helper classes for parser intermediate types
    data class GenericArgs(val types: List<TypeExpr>)

    // Helper sealed class for RecordBodyType
    sealed class RecordBodyType {
        data class OnlyExtension(val extension: StringLocation) : RecordBodyType()
        data class FieldWithOptionalRecordBodyType(
            val field: TypeField,
            val rest: RecordBodyType?
        ) : RecordBodyType()
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