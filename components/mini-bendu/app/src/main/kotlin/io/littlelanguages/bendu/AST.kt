package io.littlelanguages.bendu

import io.littlelanguages.scanpiler.Location

// Program is the root of the AST
data class Program(val topLevels: List<TopLevel>) {
    fun expressions(): List<Expr> = topLevels.filterIsInstance<ExprStmt>().map { it.expr }
}

// Top-level declarations
sealed class TopLevel

data class TypeAliasDecl(
    val id: StringLocation,
    val typeParams: List<TypeParam>?,
    val typeExpr: TypeExpr
) : TopLevel()

data class ExprStmt(val expr: Expr) : TopLevel()

// Expressions
sealed class Expr {
    abstract fun location(): Location
}

data class LetExpr(
    val recursive: Boolean,
    val id: StringLocation,
    val typeParams: List<TypeParam>?,
    val parameters: List<Parameter>?,
    val typeAnnotation: TypeExpr?,
    val value: Expr,
    val body: Expr?
) : Expr() {
    override fun location(): Location = id.location
}

data class Parameter(
    val id: StringLocation,
    val type: TypeExpr?
)

data class LambdaExpr(
    val typeParams: List<TypeParam>?,
    val param: StringLocation,
    val paramType: TypeExpr?,
    val body: Expr,
    val location: Location
) : Expr() {
    override fun location(): Location = location
}

data class BinaryOpExpr(
    val left: Expr,
    val op: BinaryOp,
    val right: Expr,
    val location: Location
) : Expr() {
    override fun location(): Location = location
}

enum class BinaryOp {
    // Logical operators
    Or,       // ||
    And,      // &&
    
    // Equality operators
    EqualEqual, // ==
    NotEqual,   // !=
    
    // Arithmetic operators
    Plus,     // +
    Minus,    // -
    Star,     // *
    Slash     // /
}

data class ApplicationExpr(
    val function: Expr,
    val arguments: List<Expr>,
    val location: Location
) : Expr() {
    override fun location(): Location = location
}

data class IfExpr(
    val condition: Expr,
    val thenBranch: Expr,
    val elseBranch: Expr,
    val location: Location
) : Expr() {
    override fun location(): Location = location
}

data class MatchExpr(
    val scrutinee: Expr,
    val cases: List<MatchCase>,
    val location: Location
) : Expr() {
    override fun location(): Location = location
}

data class MatchCase(
    val pattern: Pattern,
    val body: Expr
)

// Simple expressions
data class VarExpr(val id: StringLocation) : Expr() {
    override fun location(): Location = id.location
}

data class RecordExpr(
    val fields: List<SpreadOrField>,
    val location: Location
) : Expr() {
    override fun location(): Location = location
}

sealed class SpreadOrField

data class FieldExpr(
    val id: StringLocation,
    val value: Expr
) : SpreadOrField()

data class SpreadExpr(
    val expr: Expr
) : SpreadOrField()

data class ProjectionExpr(
    val target: Expr,
    val field: StringLocation,
    val location: Location
) : Expr() {
    override fun location(): Location = location
}

data class TupleExpr(
    val elements: List<Expr>,
    val location: Location
) : Expr() {
    override fun location(): Location = location
}

// Literal expressions
data class LiteralIntExpr(val value: IntLocation) : Expr() {
    override fun location(): Location = value.location
}

data class LiteralStringExpr(val value: StringLocation) : Expr() {
    override fun location(): Location = value.location
}

data class LiteralBoolExpr(val value: BoolLocation) : Expr() {
    override fun location(): Location = value.location
}

// Type expressions
sealed class TypeExpr {
    abstract fun location(): Location
}

data class FunctionTypeExpr(
    val from: TypeExpr,
    val to: TypeExpr,
    val location: Location
) : TypeExpr() {
    override fun location(): Location = location
}

data class MergeTypeExpr(
    val left: TypeExpr,
    val right: TypeExpr,
    val location: Location
) : TypeExpr() {
    override fun location(): Location = location
}

data class UnionTypeExpr(
    val left: TypeExpr,
    val right: TypeExpr,
    val location: Location
) : TypeExpr() {
    override fun location(): Location = location
}

data class BaseTypeExpr(
    val id: StringLocation,
    val args: List<TypeExpr>?
) : TypeExpr() {
    override fun location(): Location = id.location
}

data class RecordTypeExpr(
    val fields: List<TypeField>,
    val extension: StringLocation?,
    val location: Location
) : TypeExpr() {
    override fun location(): Location = location
}

data class TypeField(
    val id: StringLocation,
    val type: TypeExpr
)

data class TupleTypeExpr(
    val types: List<TypeExpr>,
    val location: Location
) : TypeExpr() {
    override fun location(): Location = location
}

data class LiteralStringTypeExpr(
    val value: StringLocation
) : TypeExpr() {
    override fun location(): Location = value.location
}

// Type parameters
data class TypeParam(
    val id: StringLocation,
    val constraint: TypeExpr?
)

// Pattern matching
sealed class Pattern {
    abstract fun location(): Location
}

data class RecordPattern(
    val fields: List<FieldPattern>,
    val location: Location
) : Pattern() {
    override fun location(): Location = location
}

data class FieldPattern(
    val id: StringLocation,
    val pattern: Pattern
)

data class VarPattern(
    val id: StringLocation
) : Pattern() {
    override fun location(): Location = id.location
}

data class WildcardPattern(
    val location: Location
) : Pattern() {
    override fun location(): Location = location
}

sealed class LiteralPattern : Pattern()

data class LiteralIntPattern(
    val value: IntLocation
) : LiteralPattern() {
    override fun location(): Location = value.location
}

data class LiteralStringPattern(
    val value: StringLocation
) : LiteralPattern() {
    override fun location(): Location = value.location
}

data class LiteralBoolPattern(
    val value: BoolLocation
) : LiteralPattern() {
    override fun location(): Location = value.location
}

data class TuplePattern(
    val elements: List<Pattern>,
    val location: Location
) : Pattern() {
    override fun location(): Location = location
}

// Location-aware value wrappers
data class BoolLocation(val value: Boolean, val location: Location)
data class IntLocation(val value: Int, val location: Location)
data class StringLocation(val value: String, val location: Location)
