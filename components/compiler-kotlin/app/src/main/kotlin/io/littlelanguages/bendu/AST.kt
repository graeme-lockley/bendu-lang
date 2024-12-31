package io.littlelanguages.bendu

import io.littlelanguages.bendu.cache.ScriptExport
import io.littlelanguages.bendu.typeinference.*
import io.littlelanguages.scanpiler.Location
import io.littlelanguages.scanpiler.LocationCoordinate

class Script(val imports: List<Import>, val decs: List<Declaration>) {
    fun es(): List<Expression> =
        decs.filterIsInstance<DeclarationExpression>().map { it.e }
}

sealed class Import(open val path: StringLocation, open val location: Location, open var entry: CacheEntry? = null)

data class ImportAll(
    override val path: StringLocation,
    override val location: Location,
    override var entry: CacheEntry? = null
) : Import(path, location, entry)

data class ImportList(
    override val path: StringLocation,
    override val location: Location,
    val name: StringLocation?,
    val ids: List<ImportDeclaration>,
    override var entry: CacheEntry? = null
) : Import(path, location, entry)

data class ImportDeclaration(
    val id: StringLocation,
    val alias: StringLocation? = null
)

sealed class Declaration

data class DeclarationExpression(val e: Expression) : Declaration()

data class DeclarationType(
    val declarations: List<TypeDeclaration>
) : Declaration() {
    fun location(): Location =
        declarations.map { it.location() }.fold(declarations[0].location(), Location::plus)
}

data class TypeDeclaration(
    val id: StringLocation,
    val visibility: TypeVisibility,
    val typeParameters: List<StringLocation>,
    val constructors: List<TypeConstructor>,
    var typeDecl: TypeDecl? = null
) {
    fun location(): Location =
        constructors.map { it.id.location }.fold(id.location, Location::plus)
}

enum class TypeVisibility { Public, Opaque, Private }

data class TypeConstructor(val id: StringLocation, val parameters: List<TypeTerm>)

sealed class Expression(open var type: Type? = null) {
    open fun apply(s: Subst, errors: Errors) {
        type = type!!.apply(s)
    }

    abstract fun location(): Location
}

data class AbortStatement(val es: List<Expression>, private val location: Location, override var type: Type? = null) :
    Expression(type) {
    override fun apply(s: Subst, errors: Errors) {
        es.forEach { it.apply(s, errors) }
    }

    override fun location(): Location =
        location
}

data class ApplyExpression(val f: Expression, val arguments: List<Expression>, override var type: Type? = null) :
    Expression(type) {
    override fun apply(s: Subst, errors: Errors) {
        super.apply(s, errors)
        f.apply(s, errors)
        arguments.forEach { it.apply(s, errors) }
    }

    override fun location(): Location =
        arguments.map { it.location() }.fold(f.location(), Location::plus)
}

data class ArrayElementProjectionExpression(
    val array: Expression,
    val index: Expression,
    override var type: Type? = null
) :
    Expression(type) {
    override fun apply(s: Subst, errors: Errors) {
        super.apply(s, errors)

        array.apply(s, errors)
        index.apply(s, errors)
    }

    override fun location(): Location =
        array.location() + index.location()
}

data class ArrayRangeProjectionExpression(
    val array: Expression,
    val start: Expression?,
    val end: Expression?,
    override var type: Type? = null
) :
    Expression(type) {
    override fun apply(s: Subst, errors: Errors) {
        super.apply(s, errors)

        array.apply(s, errors)
        start?.apply(s, errors)
        end?.apply(s, errors)
    }

    override fun location(): Location =
        array.location() + (end?.location() ?: start?.location() ?: array.location())
}

data class AssignmentExpression(val lhs: Expression, val rhs: Expression, override var type: Type? = null) :
    Expression(type) {
    override fun apply(s: Subst, errors: Errors) {
        super.apply(s, errors)

        lhs.apply(s, errors)
        rhs.apply(s, errors)
    }

    override fun location(): Location =
        lhs.location() + rhs.location()
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

data class BlockExpression(val es: List<Expression>, val location: Location, override var type: Type? = null) :
    Expression(type) {
    override fun apply(s: Subst, errors: Errors) {
        super.apply(s, errors)
        es.forEach { it.apply(s, errors) }
    }

    override fun location(): Location =
        location
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

data class LetStatement(val terms: List<LetStatementTerm>, override var type: Type? = null) : Expression(type) {
    override fun apply(s: Subst, errors: Errors) {
        terms.forEach { t -> t.apply(s, errors) }
    }

    override fun location(): Location =
        terms.fold(terms[0].location) { acc, t -> acc + t.location }
}

sealed class LetStatementTerm(
    open val id: StringLocation,
    open val mutable: Boolean,
    open val exported: Boolean,
    open val typeVariables: List<StringLocation>,
    open val typeQualifier: TypeTerm?,
    open val location: Location,
    open var type: Type? = null
) {
    abstract fun apply(s: Subst, errors: Errors)
}

data class LetValueStatementTerm(
    override val id: StringLocation,
    override val mutable: Boolean,
    override val exported: Boolean,
    override val typeVariables: List<StringLocation>,
    override val typeQualifier: TypeTerm?,
    val e: Expression,
    override val location: Location, override var type: Type? = null
) : LetStatementTerm(id, mutable, exported, typeVariables, typeQualifier, location, type) {
    override fun apply(s: Subst, errors: Errors) {
        type = type!!.apply(s)
        e.apply(s, errors)
    }
}

data class LetFunctionStatementTerm(
    override val id: StringLocation,
    override val mutable: Boolean,
    override val exported: Boolean,
    override val typeVariables: List<StringLocation>,
    val parameters: List<FunctionParameter>,
    override val typeQualifier: TypeTerm?,
    val body: Expression,
    override val location: Location,
    override var type: Type? = null
) : LetStatementTerm(id, mutable, exported, typeVariables, typeQualifier, location, type) {
    override fun apply(s: Subst, errors: Errors) {
        type = type!!.apply(s)
        body.apply(s, errors)
    }
}

data class LiteralArrayExpression(
    val es: List<Pair<Expression, Boolean>>,
    val location: Location,
    override var type: Type? = null
) :
    Expression(type) {
    override fun location(): Location =
        location
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

data class LiteralFunctionExpression(
    val typeParameters: List<StringLocation>,
    val parameters: List<FunctionParameter>,
    val returnTypeQualifier: TypeTerm?,
    val body: Expression,
    override var type: Type? = null
) : Expression(type) {
    override fun apply(s: Subst, errors: Errors) {
        super.apply(s, errors)

        body.apply(s, errors)
    }

    override fun location(): Location =
        parameters.map { it.location }.fold(body.location(), Location::plus)
}

data class LiteralStringExpression(val v: StringLocation, override var type: Type? = null) : Expression(type) {
    override fun location(): Location =
        v.location
}

data class LiteralTupleExpression(val es: List<Expression>, override var type: Type? = null) : Expression(type) {
    override fun apply(s: Subst, errors: Errors) {
        es.forEach { it.apply(s, errors) }
    }

    override fun location(): Location =
        if (es.isEmpty())
            LocationCoordinate(0, 0, 0)
        else
            es.drop(1).map { it.location() }.fold(es[0].location(), Location::plus)
}

data class LiteralUnitExpression(val location: Location, override var type: Type? = null) : Expression(type) {
    override fun location(): Location =
        location
}

data class LowerIDExpression(val v: StringLocation, override var type: Type? = null, var binding: Binding? = null) :
    Expression(type) {
    override fun location(): Location =
        v.location
}

data class ModuleReferenceExpression(
    val moduleID: StringLocation,
    val id: StringLocation,
    override var type: Type? = null,
    var importID: Int? = null,
    var declaration: ScriptExport? = null
) : Expression(type) {
    override fun location(): Location =
        moduleID.location + id.location
}

data class PrintStatement(val es: List<Expression>, private val location: Location, override var type: Type? = null) :
    Expression(type) {
    override fun apply(s: Subst, errors: Errors) {
        es.forEach { it.apply(s, errors) }
    }

    override fun location(): Location =
        location
}

data class PrintlnStatement(val es: List<Expression>, private val location: Location, override var type: Type? = null) :
    Expression(type) {
    override fun apply(s: Subst, errors: Errors) {
        es.forEach { it.apply(s, errors) }
    }

    override fun location(): Location =
        location
}

data class TypedExpression(val e: Expression, val typeQualifier: TypeTerm, override var type: Type? = null) :
    Expression(type) {
    override fun apply(s: Subst, errors: Errors) {
        super.apply(s, errors)
        e.apply(s, errors)
    }

    override fun location(): Location =
        e.location() + typeQualifier.location()
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

data class UpperIDExpression(val v: StringLocation, override var type: Type? = null) :
    Expression(type) {
    override fun location(): Location =
        v.location
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

data class BoolLocation(val value: Boolean, val location: Location)
data class CharLocation(val value: Char, val location: Location)
data class FloatLocation(val value: Float, val location: Location)
data class IntLocation(val value: Int, val location: Location)
data class StringLocation(val value: String, val location: Location)
data class OpLocation(val op: Op, val location: Location)
data class UnaryOpLocation(val op: UnaryOp, val location: Location)

sealed class FunctionParameter(open val typeQualifier: TypeTerm?, open val location: Location)

data class LowerIDFunctionParameter(
    val value: String,
    override val location: Location,
    val mutable: Boolean,
    override val typeQualifier: TypeTerm?
) : FunctionParameter(typeQualifier, location)

data class TupleFunctionParameter(
    val parameters: List<FunctionParameter>,
    override val typeQualifier: TypeTerm?,
    override val location: Location
) : FunctionParameter(typeQualifier, location)

data class WildcardFunctionParameter(override val typeQualifier: TypeTerm?, override val location: Location) :
    FunctionParameter(typeQualifier, location)

enum class Op {
    Or, And,
    Plus, Minus, Multiply, Divide, Modulo, Power,
    EqualEqual, NotEqual, LessThan, LessEqual, GreaterThan, GreaterEqual,
    LessLess, LessBang, GreaterGreater, GreaterBang,
}

enum class UnaryOp { Not, TypeOf }

sealed class TypeTerm {
    abstract fun location(): Location

    abstract fun toType(env: ASTTypeToTypeEnvironment): Type
}

data class FunctionType(val parameters: List<TypeTerm>, val returnType: TypeTerm, val location: Location) :
    TypeTerm() {
    override fun location(): Location =
        location

    override fun toType(env: ASTTypeToTypeEnvironment): Type =
        TArr(parameters.map { it.toType(env) }, returnType.toType(env))
}

data class LowerIDType(val v: StringLocation) : TypeTerm() {
    override fun location(): Location =
        v.location

    override fun toType(env: ASTTypeToTypeEnvironment): Type {
        val parameter = env.parameter(v.value)

        if (parameter == null) {
            env.addError(UnknownTypeVariableError(v.value, v.location))
            return typeError
        } else {
            return parameter
        }
    }
}

data class TupleType(val types: List<TypeTerm>, val location: Location) : TypeTerm() {
    override fun location(): Location =
        location

    override fun toType(env: ASTTypeToTypeEnvironment): Type =
        TTuple(types.map { it.toType(env) })
}

data class UpperIDType(val v: StringLocation, val parameters: List<TypeTerm>, val location: Location) : TypeTerm() {
    override fun location(): Location =
        location

    override fun toType(env: ASTTypeToTypeEnvironment): Type {
        val typeDecl = env.typeDecl(v.value)

        if (typeDecl == null) {
            env.addError(UnknownTypeError(v.value, v.location))
            return typeError
        } else {
            return TCon(v.value, parameters.map { it.toType(env) }, location = location)
        }
    }
}

