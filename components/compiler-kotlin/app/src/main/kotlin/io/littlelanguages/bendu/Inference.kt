package io.littlelanguages.bendu

import io.littlelanguages.bendu.cache.*
import io.littlelanguages.bendu.typeinference.*
import io.littlelanguages.scanpiler.Location

private const val LITERAL_FIX_NAME = "[bob]"

fun infer(
    entry: CacheEntry,
    typeEnv: TypeEnv = emptyTypeEnv,
    pump: Pump = Pump(),
    errors: Errors = Errors(),
    constraints: Constraints = Constraints()
): Script {
    val ast = parse(entry.script(), errors)

    if (errors.hasErrors()) {
        return Script(emptyList(), emptyList())
    }

    val env = Environment(typeEnv, pump, errors, constraints)
    inferImports(entry, ast.imports, env)
    inferDeclarations(ast.decs, env)

    return ast
}

private fun inferImports(entry: CacheEntry, imports: List<Import>, env: Environment) =
    imports.forEachIndexed { index, import -> inferImport(entry, index, import, env) }

private fun inferImport(entry: CacheEntry, index: Int, import: Import, env: Environment) {
    import.entry = entry.relativeEntry(import.path.value)

    val importEntry = import.entry!!

    if (!importEntry.isUptoDate()) {
        importEntry.compile(env.errors)
    }

    if (env.errors.hasErrors()) {
        return
    }

    return when (import) {
        is ImportAll ->
            inferScriptExports(importEntry.declarations, import.location, env)

        is ImportList -> {
            if (import.name != null) {
                if (env.hasImport(import.name.value)) {
                    env.errors.addError(IdentifierRedefinitionError(import.name, import.location))
                }

                env.addImport(
                    import.name.value,
                    index,
                    importEntry.expandedExportDeclarations()
                )
            }

            import.ids.forEach { name ->
                val scriptExport = importEntry[name.id.value]

                if (scriptExport != null) {
                    inferScriptExport(scriptExport, name.alias?.value ?: name.id.value, import.location, env)
                } else {
                    env.errors.addError(IdentifierNotExported(name.id, import.path.value))
                    env.bind(name.id.value, import.location, false, Scheme(emptySet(), typeError))
                }
            }
        }
    }
}

private fun inferScriptExports(scriptExports: ScriptExports, location: Location, env: Environment) =
    scriptExports.exports.forEach { scriptExport -> inferScriptExport(scriptExport, scriptExport.name, location, env) }

private fun inferScriptExport(scriptExport: ScriptExport, alias: String, location: Location, env: Environment) {
    when (scriptExport) {
        is CustomTypeExport -> {
            env.bindTypeDecl(
                alias,
                TypeDecl(
                    scriptExport.name,
                    scriptExport.parameters,
                    scriptExport.constructors.map { Constructor(it.name, it.parameters) })
            )

            inferScriptExports(scriptExport.constructorExports(), location, env)
        }

        is FunctionExport ->
            env.bind(alias, location, scriptExport.mutable, scriptExport.scheme)

        is ValueExport ->
            env.bind(alias, location, scriptExport.mutable, scriptExport.scheme)
    }
}

private fun inferDeclarations(decls: List<Declaration>, env: Environment) =
    decls.forEach { decl -> inferDeclaration(decl, env) }

private fun inferDeclaration(declaration: Declaration, env: Environment) {
    when (declaration) {
        is DeclarationExpression -> inferStatement(declaration.e, env)

        is DeclarationType -> {
            val inferEnv = ASTInference(env)
            val tvss = mutableListOf<Pair<List<TVar>, List<Var>>>()

            declaration.declarations.forEach { decl ->
                val tvs = inferEnv.setParameters(decl.typeParameters)
                val tvsIds = tvs.map { it.name }
                tvss.add(Pair(tvs, tvsIds))

                inferEnv.bindTypeDecl(decl.id.value, TypeDecl(decl.id.value, tvsIds, emptyList()))
            }

            declaration.declarations.forEachIndexed { idx, decl ->
                val (tvs, tvsIds) = tvss[idx]

                inferEnv.resetParameters(decl.typeParameters, tvs)

                val typeDecl = TypeDecl(
                    decl.id.value,
                    tvsIds,
                    decl.constructors.map {
                        Constructor(
                            it.id.value,
                            it.parameters.map { tt -> tt.toType(inferEnv) })
                    })
                env.bindTypeDecl(decl.id.value, typeDecl)
                decl.typeDecl = typeDecl

                typeDecl.constructors.forEach { constructor ->
                    env.bind(
                        constructor.name,
                        declaration.location(),
                        false,
                        Scheme(tvsIds.toSet(), TArr(constructor.parameters, TCon(decl.id.value, tvs)))
                    )
                }
            }
        }
    }
}

class ASTInference(private val env: Environment) : ASTTypeToTypeEnvironment {
    private val typeVariables = mutableMapOf<String, Pair<Location, Type>>()
    private val typeDecls = mutableMapOf<String, TypeDecl>()

    override fun parameter(name: String): Type? =
        typeVariables[name]?.second

    fun setParameters(parameters: List<StringLocation>): List<TVar> {
        typeVariables.clear()

        return parameters.map { parameter ->
            bindParameter(
                parameter.value,
                parameter.location
            )
        }
    }

    fun resetParameters(parameters: List<StringLocation>, types: List<Type>) {
        typeVariables.clear()

        parameters.forEachIndexed { index, parameter ->
            typeVariables[parameter.value] = Pair(parameter.location, types[index])
        }
    }

    override fun typeDecl(name: String): TypeDecl? =
        typeDecls[name] ?: env.typeDecl(name)

    override fun bindParameter(name: String, location: Location): TVar {
        if (typeVariables.containsKey(name)) {
            addError(IdentifierRedefinitionError(StringLocation(name, location), typeVariables[name]!!.first))
        }

        val variable = env.nextVar()
        typeVariables[name] = Pair(location, variable)

        return variable
    }

    override fun bindTypeDecl(name: String, typeDecl: TypeDecl) {
        typeDecls[name] = typeDecl
    }

    override fun addError(error: BenduError) =
        env.addError(error)
}

private fun inferStatement(statement: Expression, env: Environment) {
    env.resetConstraints()

    inferExpression(statement, env)

    statement.apply(env.solveConstraints(), env.errors)

    if (statement is LetStatement) {
        statement.terms.forEach { term -> env.rebind(term.id.value, env.generalise(term.type!!)) }
    }
}

private fun inferExpression(expression: Expression, env: Environment) {
    when (expression) {
        is AbortStatement -> {
            inferPrintArguments(expression.es, env)

            expression.type = typeUnit.withLocation(expression.location())
        }

        is ApplyExpression -> {
            inferScopedExpression(expression.f, env)

            expression.arguments.forEach { argument ->
                inferScopedExpression(argument, env)
            }

            val tv = env.nextVar()
            val domain = expression.arguments.map { it.type!! }

            env.addConstraint(expression.f.type!!, TArr(domain, tv))
            expression.type = tv
        }

        is ArrayElementProjectionExpression -> {
            inferScopedExpression(expression.array, env)
            inferScopedExpression(expression.index, env)

            val tv = env.nextVar()
            val arrayType = TCon("Array", listOf(tv))

            env.addConstraint(expression.array.type!!, arrayType)
            env.addConstraint(expression.index.type!!, typeInt)

            expression.type = tv
        }

        is ArrayRangeProjectionExpression -> {
            inferScopedExpression(expression.array, env)

            val arrayType = TCon("Array", listOf(env.nextVar()))
            env.addConstraint(expression.array.type!!, arrayType)

            if (expression.start != null) {
                inferScopedExpression(expression.start, env)
                env.addConstraint(expression.start.type!!, typeInt)
            }

            if (expression.end != null) {
                inferScopedExpression(expression.end, env)
                env.addConstraint(expression.end.type!!, typeInt)
            }

            expression.type = arrayType
        }

        is AssignmentExpression -> {
            inferScopedExpression(expression.lhs, env)

            when {
                expression.lhs is LowerIDExpression -> {
                    val binding = expression.lhs.binding
                    if (binding != null && !binding.mutable) {
                        env.errors.addError(
                            IdentifierImmutableError(
                                StringLocation(expression.lhs.v.value, binding.location),
                                expression.lhs.location()
                            )
                        )
                    }
                }

                expression.lhs is ModuleReferenceExpression ->
                    if (expression.lhs.declaration?.mutable == false) {
                        env.errors.addError(AssignmentError(expression.lhs.location()))
                    }

                expression.lhs !is ArrayElementProjectionExpression && expression.lhs !is ArrayRangeProjectionExpression ->
                    env.errors.addError(AssignmentError(expression.lhs.location()))
            }

            inferScopedExpression(expression.rhs, env)

            env.addConstraint(expression.lhs.type!!, expression.rhs.type!!)
            expression.type = expression.rhs.type!!.withLocation(expression.location())
        }

        is BinaryExpression -> {
            inferScopedExpression(expression.e1, env)
            inferScopedExpression(expression.e2, env)

            val tv = env.nextVar()
            expression.type = tv

            val u1 = TArr(listOf(expression.e1.type!!), TArr(listOf(expression.e2.type!!), tv))
            val u2 = env.instantiateScheme(binaryOperatorSignatures[expression.op.op] ?: Scheme(setOf(), typeError))

            env.addConstraint(u1, u2)
        }

        is BlockExpression -> {
            env.openTypeEnv()

            expression.es.forEach { e ->
                inferExpression(e, env)
                e.apply(env.solveConstraints(), env.errors)

                if (e is LetStatement) {
                    e.terms.forEach { term -> env.rebind(term.id.value, env.generalise(term.type!!)) }
                }
            }
            env.closeTypeEnv()

            expression.type =
                if (expression.es.isEmpty())
                    typeUnit.withLocation(expression.location())
                else
                    expression.es.last().type
        }

        is CaseExpression -> throw IllegalArgumentException("CaseExpression should not be in the AST")

        is ErrorExpression -> throw IllegalArgumentException("ErrorExpression should not be in the AST")

        is FailExpression -> throw IllegalArgumentException("FailExpression should not be in the AST")

        is FatBarExpression -> throw IllegalArgumentException("FatBarExpression should not be in the AST")

        is IfExpression -> {
            expression.guards.forEach { guard ->
                inferScopedExpression(guard.first, env)
                inferScopedExpression(guard.second, env)

                env.addConstraint(guard.first.type!!, typeBool)
                env.addConstraint(guard.second.type!!, expression.guards[0].second.type!!)
            }
            expression.type = expression.guards[0].second.type

            if (expression.elseBranch == null) {
                env.addConstraint(expression.type!!, typeUnit)
            } else {
                inferScopedExpression(expression.elseBranch, env)
                env.addConstraint(expression.type!!, expression.elseBranch.type!!)
            }
        }

        is LetStatement -> {
            val tv = env.nextVars(expression.terms.size)
            expression.terms.forEachIndexed { i, term ->
                val scheme = Scheme(emptySet(), tv[i])
                env.bind(term.id.value, term.id.location, term.mutable, scheme)
                expression.terms[i].type = tv[i]

                if (term is LetValueStatementTerm && term.typeQualifier != null) {
                    env.addConstraint(tv[i], term.typeQualifier!!.toType(env))
                }
            }

            val declarationType = fix(
                LiteralFunctionExpression(
                    emptyList(),
                    listOf(LowerIDFunctionParameter(LITERAL_FIX_NAME, expression.location(), false, null)),
                    null,
                    LiteralTupleExpression(expression.terms.map {
                        when (it) {
                            is LetValueStatementTerm -> it.e
                            is LetFunctionStatementTerm -> LiteralFunctionExpression(
                                it.typeVariables,
                                it.parameters,
                                it.typeQualifier,
                                it.body
                            )
                        }
                    })
                ), env
            )
            env.addConstraint(declarationType, TTuple(tv))

            expression.type = typeUnit.withLocation(expression.location())
        }

        is LiteralArrayExpression -> {
            val tv = env.nextVar()
            val arrayType = TCon("Array", listOf(tv)).withLocation(expression.location())

            expression.es.forEach { e ->
                inferScopedExpression(e.first, env)
                if (e.second) {
                    env.addConstraint(e.first.type!!, arrayType)
                } else {
                    env.addConstraint(e.first.type!!, tv)
                }
            }

            expression.type = arrayType
        }

        is LiteralBoolExpression ->
            expression.type = typeBool.withLocation(expression.location())

        is LiteralCharExpression ->
            expression.type = typeChar.withLocation(expression.location())

        is LiteralFloatExpression ->
            expression.type = typeFloat.withLocation(expression.location())

        is LiteralIntExpression ->
            expression.type = typeInt.withLocation(expression.location())

        is LiteralFunctionExpression -> {
            fun inferFunctionParameters(type: Type, parameter: FunctionParameter) {
                when (parameter) {
                    is LowerIDFunctionParameter -> {
                        if (parameter.value != LITERAL_FIX_NAME) {
                            env.bind(parameter.value, parameter.location, parameter.mutable, Scheme(emptySet(), type))
                        }
                    }

                    is TupleFunctionParameter -> {
                        val tupleTypes = env.nextVars(parameter.parameters.size)
                        parameter.parameters.forEachIndexed { index, p ->
                            inferFunctionParameters(tupleTypes[index], p)
                        }
                        env.addConstraint(type, TTuple(tupleTypes))
                    }

                    is WildcardFunctionParameter -> {}
                }
                if (parameter.typeQualifier != null) {
                    env.addConstraint(type, parameter.typeQualifier!!.toType(env))
                }
            }

            env.openTypeEnv()

            val tv = env.nextVar()
            val domain = env.nextVars(expression.parameters.size)

            expression.typeParameters.forEach { parameter ->
                env.bindParameter(parameter.value, parameter.location)
            }

            expression.parameters.forEachIndexed { index, parameter ->
                inferFunctionParameters(domain[index], parameter)
            }

            inferScopedExpression(expression.body, env)

            if (expression.returnTypeQualifier != null) {
                env.addConstraint(expression.body.type!!, expression.returnTypeQualifier.toType(env))
            }

            env.closeTypeEnv()

            val result = TArr(domain, expression.body.type!!)
            env.addConstraint(tv, result)

            expression.type = tv
        }

        is LiteralStringExpression ->
            expression.type = typeString.withLocation(expression.location())

        is LiteralTupleExpression -> {
            expression.es.forEach { e ->
                inferScopedExpression(e, env)
            }

            expression.type = TTuple(expression.es.map { it.type!! }).withLocation(expression.location())
        }

        is LiteralUnitExpression ->
            expression.type = typeUnit.withLocation(expression.location())

        is LowerIDExpression -> {
            val binding = env.binding(expression.v.value)

            if (binding == null) {
                env.errors.addError(UnknownIdentifierError(expression.v))
                expression.type = typeError.withLocation(expression.location())
            } else {
                expression.binding = binding
                expression.type = env.instantiateScheme(binding.scheme).withLocation(expression.location())
            }
        }

        is MatchExpression -> {
            inferScopedExpression(expression.e, env)

            val tv = env.nextVar()

            expression.cases.forEach { case ->
                env.openTypeEnv()

                inferPattern(case.pattern, env)
                env.addConstraint(case.pattern.type!!, expression.e.type!!)

                if (case.guard != null) {
                    inferScopedExpression(case.guard, env)
                    env.addConstraint(case.guard.type!!, typeBool)
                }

                inferExpression(case.body, env)
                env.addConstraint(case.body.type!!, tv)

                env.closeTypeEnv()
            }

            expression.type = tv
        }

        is ModuleReferenceExpression -> {
            val binding = env.getImport(expression.moduleID.value)

            if (binding == null) {
                env.errors.addError(UnknownIdentifierError(expression.moduleID))
                expression.type = typeError.withLocation(expression.location())
            } else {
                when (val declaration = binding.second.find(expression.id.value)) {
                    null -> {
                        env.errors.addError(UnknownIdentifierError(expression.id))
                        expression.type = typeError.withLocation(expression.location())
                    }

                    is CustomTypeExport -> TODO()

                    is FunctionExport -> {
                        expression.importID = binding.first
                        expression.declaration = declaration
                        expression.type = env.instantiateScheme(declaration.scheme)
                    }

                    is ValueExport -> {
                        expression.importID = binding.first
                        expression.declaration = declaration
                        expression.type = env.instantiateScheme(declaration.scheme)
                    }
                }
            }
        }

        is PrintStatement -> {
            inferPrintArguments(expression.es, env)

            expression.type = typeUnit.withLocation(expression.location())
        }

        is PrintlnStatement -> {
            inferPrintArguments(expression.es, env)

            expression.type = typeUnit.withLocation(expression.location())
        }

        is TypedExpression -> {
            inferScopedExpression(expression.e, env)

            expression.type = expression.typeQualifier.toType(env)

            env.addConstraint(expression.e.type!!, expression.type!!)
        }

        is UnaryExpression -> {
            inferScopedExpression(expression.e, env)

            val tv = env.nextVar()
            expression.type = tv

            val u1 = TArr(listOf(expression.e.type!!), tv)
            val u2 = env.instantiateScheme(unaryOperatorSignatures[expression.op.op] ?: Scheme(setOf(), typeError))

            env.addConstraint(u1, u2)
        }

        is UpperIDExpression -> {
            val binding = env.binding(expression.v.value)

            if (binding == null) {
                env.errors.addError(UnknownIdentifierError(expression.v))
                expression.type = typeError.withLocation(expression.location())
            } else {
                expression.type = env.instantiateScheme(binding.scheme).withLocation(expression.location())
            }
        }

        is WhileExpression -> {
            inferScopedExpression(expression.guard, env)
            inferScopedExpression(expression.body, env)

            env.addConstraint(expression.guard.type!!, typeBool)

            expression.type = typeUnit.withLocation(expression.location())
        }
    }
}

private fun inferScopedExpression(expression: Expression, env: Environment) {
    if (expression is LetStatement) {
        env.openTypeEnv()
        inferExpression(expression, env)
        env.closeTypeEnv()
    } else
        inferExpression(expression, env)
}

private fun inferPrintArguments(es: List<Expression>, env: Environment) {
    es.forEach { e ->
        inferExpression(e, env)
    }
    val s = env.solveConstraints()

    es.forEach { e ->
        e.apply(s, env.errors)
    }
}

private fun fix(e: Expression, env: Environment): Type {
    inferExpression(e, env)
    val tv = env.nextVar()

    env.addConstraint(TArr(listOf(tv), tv), e.type!!)

    return tv
}

private val binaryOperatorSignatures = mapOf(
    Pair(Op.And, Scheme(setOf(), TArr(listOf(typeBool), TArr(listOf(typeBool), typeBool)))),
    Pair(Op.Or, Scheme(setOf(), TArr(listOf(typeBool), TArr(listOf(typeBool), typeBool)))),

    Pair(Op.EqualEqual, Scheme(setOf(0), TArr(listOf(TVar(0)), TArr(listOf(TVar(0)), typeBool)))),
    Pair(Op.NotEqual, Scheme(setOf(0), TArr(listOf(TVar(0)), TArr(listOf(TVar(0)), typeBool)))),
    Pair(Op.LessThan, Scheme(setOf(0), TArr(listOf(TVar(0)), TArr(listOf(TVar(0)), typeBool)))),
    Pair(Op.LessEqual, Scheme(setOf(0), TArr(listOf(TVar(0)), TArr(listOf(TVar(0)), typeBool)))),
    Pair(Op.GreaterThan, Scheme(setOf(0), TArr(listOf(TVar(0)), TArr(listOf(TVar(0)), typeBool)))),
    Pair(Op.GreaterEqual, Scheme(setOf(0), TArr(listOf(TVar(0)), TArr(listOf(TVar(0)), typeBool)))),

    Pair(Op.Plus, Scheme(setOf(0), TArr(listOf(TVar(0)), TArr(listOf(TVar(0)), TVar(0))))),
    Pair(Op.Minus, Scheme(setOf(0), TArr(listOf(TVar(0)), TArr(listOf(TVar(0)), TVar(0))))),
    Pair(Op.Multiply, Scheme(setOf(0), TArr(listOf(TVar(0)), TArr(listOf(TVar(0)), TVar(0))))),
    Pair(Op.Divide, Scheme(setOf(0), TArr(listOf(TVar(0)), TArr(listOf(TVar(0)), TVar(0))))),
    Pair(Op.Modulo, Scheme(setOf(0), TArr(listOf(TVar(0)), TArr(listOf(TVar(0)), TVar(0))))),
    Pair(Op.Power, Scheme(setOf(0), TArr(listOf(TVar(0)), TArr(listOf(TVar(0)), TVar(0))))),

    Pair(
        Op.LessLess,
        Scheme(
            setOf(0),
            TArr(listOf(TCon("Array", listOf(TVar(0)))), TArr(listOf(TVar(0)), TCon("Array", listOf(TVar(0)))))
        )
    ),
    Pair(
        Op.LessBang,
        Scheme(
            setOf(0),
            TArr(listOf(TCon("Array", listOf(TVar(0)))), TArr(listOf(TVar(0)), TCon("Array", listOf(TVar(0)))))
        )
    ),
    Pair(
        Op.GreaterGreater,
        Scheme(
            setOf(0),
            TArr(listOf(TVar(0)), TArr(listOf(TCon("Array", listOf(TVar(0)))), TCon("Array", listOf(TVar(0)))))
        )
    ),
    Pair(
        Op.GreaterBang,
        Scheme(
            setOf(0),
            TArr(listOf(TVar(0)), TArr(listOf(TCon("Array", listOf(TVar(0)))), TCon("Array", listOf(TVar(0)))))
        )
    )
)

private val unaryOperatorSignatures = mapOf(
    Pair(UnaryOp.Not, Scheme(setOf(), TArr(listOf(typeBool), typeBool))),
    Pair(UnaryOp.TypeOf, Scheme(setOf(0), TArr(listOf(TVar(0)), typeString)))
)

private fun inferPattern(pattern: Pattern, env: Environment) {
    when (pattern) {
        is ConstructorPattern -> {
            if (pattern.moduleID == null) {
                val decl = env.typeDeclConstructor(pattern.id.value)

                inferConstructorPattern(
                    pattern,
                    decl?.let { Pair({ pump: Pump -> it.first.type(pump) }, it.second.parameters) },
                    env
                )
            } else {
                val importBinding = env.getImport(pattern.moduleID.value)

                if (importBinding == null) {
                    env.errors.addError(UnknownIdentifierError(pattern.moduleID))
                    pattern.type = typeError.withLocation(pattern.location())
                } else {
                    val module = importBinding.second
                    val decl = module.findConstructor(pattern.id.value)

                    inferConstructorPattern(
                        pattern,
                        decl?.let { Pair({ pump: Pump -> it.first.type(pump) }, it.second.parameters) },
                        env
                    )
                }
            }
        }

        is LiteralBoolPattern ->
            pattern.type = typeBool.withLocation(pattern.location())

        is LiteralCharPattern ->
            pattern.type = typeChar.withLocation(pattern.location())

        is LiteralFloatPattern ->
            pattern.type = typeFloat.withLocation(pattern.location())

        is LiteralIntPattern ->
            pattern.type = typeInt.withLocation(pattern.location())

        is LiteralStringPattern ->
            pattern.type = typeString.withLocation(pattern.location())

        is LiteralUnitPattern ->
            pattern.type = typeUnit.withLocation(pattern.location())

        is LowerIDPattern -> {
            val binding = env.binding(pattern.v.value)

            if (binding == null) {
                val type = env.nextVar()
                pattern.type = type
                env.bind(pattern.v.value, pattern.location(), false, Scheme(emptySet(), type))
            } else {
                env.addError(IdentifierRedefinitionError(pattern.v, binding.location))
            }
        }

        is NamedPattern -> {
            inferPattern(pattern.pattern, env)

            val binding = env.binding(pattern.id.value)
            if (binding == null) {
                pattern.type = pattern.pattern.type
                env.bind(pattern.id.value, pattern.location(), false, Scheme(emptySet(), pattern.type!!))
            } else {
                env.addError(IdentifierRedefinitionError(pattern.id, binding.location))
            }
        }

        is TuplePattern -> {
            pattern.patterns.forEach { p ->
                inferPattern(p, env)
            }

            pattern.type = TTuple(pattern.patterns.map { it.type!! }).withLocation(pattern.location())
        }

        is TypedPattern -> {
            inferPattern(pattern.pattern, env)
            val type = pattern.typeQualifier.toType(env)

            env.addConstraint(pattern.pattern.type!!, type)
            pattern.type = type
        }

        is WildcardPattern ->
            pattern.type = env.nextVar(pattern.location())
    }
}

private fun inferConstructorPattern(
    pattern: ConstructorPattern,
    decl: Pair<(Pump) -> Type, List<Type>>?,
    env: Environment
) {
    if (decl == null) {
        env.errors.addError(UnknownIdentifierError(pattern.id))
        pattern.type = typeError.withLocation(pattern.location())
    } else {
        val (cdtType, constructorParameters) = decl

        if (constructorParameters.size == pattern.patterns.size) {
            constructorParameters.forEachIndexed { index, type ->
                inferPattern(pattern.patterns[index], env)
                env.addConstraint(pattern.patterns[index].type!!, type)
            }
        } else {
            env.errors.addError(
                ConstructorPatternArityError(
                    pattern.id.value,
                    constructorParameters.size,
                    pattern.patterns.size,
                    pattern.location()
                )
            )
        }

        pattern.type = cdtType(env.pump)
    }
}
