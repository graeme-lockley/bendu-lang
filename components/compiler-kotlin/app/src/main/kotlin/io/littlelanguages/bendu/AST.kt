package io.littlelanguages.bendu

import io.littlelanguages.bendu.typeinference.*
import io.littlelanguages.scanpiler.Location
import io.littlelanguages.scanpiler.LocationCoordinate

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

data class BlockExpression(val es: List<Expression>, val location: Location, override var type: Type? = null) : Expression(type) {
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

data class LetStatement(
    val terms: List<LetStatementTerm>,
    override var type: Type? = null
) : Expression(type) {
    override fun apply(s: Subst, errors: Errors) {
        terms.forEach { t -> t.apply(s, errors) }
    }

    override fun location(): Location =
        terms.fold(terms[0].location) { acc, t -> acc + t.location }
}

sealed class LetStatementTerm(
    open val id: StringLocation,
    open val mutable: Boolean,
    open val typeVariables: List<StringLocation>,
    open val typeQualifier: TypeFactor?,
    open val location: Location,
    open var type: Type? = null
) {
    abstract fun apply(s: Subst, errors: Errors)
}

data class LetValueStatementTerm(
    override val id: StringLocation,
    override val mutable: Boolean,
    override val typeVariables: List<StringLocation>,
    override val typeQualifier: TypeFactor?,
    val e: Expression,
    override val location: Location, override var type: Type? = null
) : LetStatementTerm(id, mutable, typeVariables, typeQualifier, location, type) {
    override fun apply(s: Subst, errors: Errors) {
        type = type!!.apply(s)
        e.apply(s, errors)
    }
}

data class LetFunctionStatementTerm(
    override val id: StringLocation,
    override val mutable: Boolean,
    override val typeVariables: List<StringLocation>,
    val parameters: List<TypeQualifiedIDLocation>,
    override val typeQualifier: TypeFactor?,
    val body: Expression,
    override val location: Location,
    override var type: Type? = null
) : LetStatementTerm(id, mutable, typeVariables, typeQualifier, location, type) {
    override fun apply(s: Subst, errors: Errors) {
        type = type!!.apply(s)
        body.apply(s, errors)
    }
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
    val parameters: List<TypeQualifiedIDLocation>,
    val returnTypeQualifier: TypeFactor?,
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

data class LowerIDExpression(val v: StringLocation, override var type: Type? = null) : Expression(type) {
    override fun location(): Location =
        v.location
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

data class TypedExpression(val e: Expression, val typeQualifier: TypeFactor, override var type: Type? = null) :
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

data class TypeQualifiedIDLocation(val value: String, val location: Location, val mutable: Boolean, val typeQualifier: TypeFactor?)

enum class Op { Or, And, Plus, Minus, Multiply, Divide, Modulo, Power, EqualEqual, NotEqual, LessThan, LessEqual, GreaterThan, GreaterEqual }
enum class UnaryOp { Not, TypeOf }

sealed class TypeFactor {
    abstract fun location(): Location

    abstract fun toType(env: Environment): Type
}

data class LowerIDType(val v: StringLocation) : TypeFactor() {
    override fun location(): Location =
        v.location

    override fun toType(env: Environment): Type {
        val parameter = env.parameter(v.value)

        if (parameter == null) {
            env.errors.addError(UnknownTypeVariableError(v.value, v.location))
            return typeError
        } else {
            return parameter
        }
    }
}

data class UpperIDType(val v: StringLocation) : TypeFactor() {
    override fun location(): Location =
        v.location

    override fun toType(env: Environment): Type =
        TCon(v.value, emptyList(), v.location)
}

data class FunctionType(val parameters: List<TypeFactor>, val returnType: TypeFactor, val location: Location) :
    TypeFactor() {
    override fun location(): Location =
        location

    override fun toType(env: Environment): Type =
        TArr(parameters.map { it.toType(env) }, returnType.toType(env))
}
