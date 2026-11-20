package net.akehurst.language.expressions.processor

import net.akehurst.language.expressions.asm.PropertyCallDefault
import net.akehurst.language.agl.Agl
import net.akehurst.language.expressions.api.*
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.types.api.*
import net.akehurst.language.types.asm.StdFunctionLib
import net.akehurst.language.types.asm.StdLibDefault
import net.akehurst.language.types.asm.TypeArgumentNamedSimple

class ExpressionTypeResolver(
    val typesDomain: TypesDomain,
    val contextNamespace: TypesNamespace,
    val issues: IssueHolder
) {

    fun typeFor(expression: Expression, self: TypeInstance): TypeInstance =
        expression.typeOfExpressionFor(self)

    fun typeOfExpressionStr(expression: String, self: TypeDefinition): TypeInstance {
        val result = Agl.registry.agl.expressions.processor!!.process(expression)
        check(result.allIssues.errors.isEmpty()) { result.allIssues.toString() }
        val asm = result.asm!!
        return asm.typeOfExpressionFor(self.type())
    }

    fun Expression.typeOfExpressionFor(self: TypeInstance): TypeInstance = when (this) {
        is RootExpression -> this.typeOfRootExpressionFor(self)
        is LiteralExpression -> this.typeOfLiteralExpressionFor(self)
        is CreateObjectExpression -> this.typeOfCreateObjectExpressionFor(self)
        is CreateTupleExpression -> this.typeOfCreateTupleExpressionFor(self)
        is OnExpression -> this.typeOfOnExpressionFor(self)
        is NavigationExpression -> this.typeOfNavigationExpressionFor(self)
        is LambdaExpression -> this.typeOfLambdaExpressionFor(self)
        is WithExpression -> this.typeOfWithExpressionFor(self)
        is WhenExpression -> this.typeOfWhenExpressionFor(self)
        is TernaryConditionExpression -> this.typeOfTernaryConditionFor(self)
        is InfixExpression -> this.typeOfInfixExpressionFor(self)
        is CastExpression -> this.typeOfCastExpressionFor(self)
        is GroupExpression -> this.typeOfGroupExpressionFor(self)
        is FunctionCall -> this.typeOfFunctionCallFor(self)
        else -> error("Subtype of Expression not handled in 'typeOfExpressionFor' : '${this::class.simpleName}'")

    }

    fun LiteralExpression.typeOfLiteralExpressionFor(self: TypeInstance): TypeInstance =
        typesDomain.findByQualifiedNameOrNull(this.qualifiedTypeName)?.type() ?: StdLibDefault.NothingType

    fun RootExpression.typeOfRootExpressionFor(self: TypeInstance): TypeInstance = when {
        this.isNothing -> StdLibDefault.NothingType
        this.isSelf -> self
        else -> {
            when (self.resolvedDefinition) {
                is StructuredType -> {
                    self.allResolvedProperty[PropertyName(this.name)]?.typeInstance ?: run {
                        issues.error(null, "'$self' has no property named '${this.name}'")
                        StdLibDefault.NothingType
                    }
                }

                else -> {
                    issues.error(null, "'$self' is not a StructuredType cannot access property named '${this.name}'")
                    StdLibDefault.NothingType
                }
            }
        }
    }

    fun NavigationExpression.typeOfNavigationExpressionFor(self: TypeInstance): TypeInstance {
        val st = this.start
        val start = when (st) {
            is RootExpression -> when {
                st.isNothing -> StdLibDefault.NothingType
                st.isSelf -> self
                else -> {
                    self.allResolvedProperty[PropertyName(st.name)]?.typeInstance
                }
            }

            // e.g. a literal, '(when { ... }).p', 'List(...).size', 'T() { ... }.p'
            else -> st.typeOfExpressionFor(self)
        }
        val r = this.parts.fold(start) { acc, it ->
            when (acc) {
                null -> StdLibDefault.NothingType
                else -> it.typeOfNavigationPartFor(acc)
            }
        }
        return r ?: StdLibDefault.NothingType
    }

    fun NavigationPart.typeOfNavigationPartFor(self: TypeInstance): TypeInstance = when (this) {
        is PropertyCall -> this.typeOfPropertyCallFor(self)
        is MethodCall -> this.typeOfMethodCallFor(self)
        is IndexOperation -> this.typeOfIndexOperationFor(self)
        else -> error("subtype of NavigationPart not handled")

    }

    fun lastPropertyDeclarationFor(expr: NavigationExpression, self: TypeInstance): PropertyDeclaration? {
        val st = expr.start
        var pd: PropertyDeclaration? = when (st) {
            is LiteralExpression -> null
            is RootExpression -> when {
                st.isNothing -> null
                st.isSelf -> null
                else -> {
                    self.allResolvedProperty[PropertyName(st.name)]
                }
            }

            // the start is not a property (e.g. '(when { ... }).p'): no declaration
            else -> null
        }
        var acc = pd?.typeInstance
        expr.parts.forEach {
            when (it) {
                is PropertyCall -> {
                    pd = acc?.let { ti -> resolvedOrIssue(ti) }?.allResolvedProperty?.get(PropertyName(it.propertyName))
                    acc = pd?.typeInstance
                }
                // an index into a collection (e.g. 'a.b[0].c'): the element type, the last property stays the collection property
                is IndexOperation -> acc = acc?.typeArguments?.firstOrNull()?.type
                else -> {
                    //everything else must be property calls or index operations
                    pd = null
                    acc = null
                }
            }
        }
        return pd
    }

    /** the type, or null with an error issue if it does not resolve (e.g. a missing import) */
    private fun resolvedOrIssue(ti: TypeInstance): TypeInstance? = when (ti.resolvedDefinitionOrNull) {
        null -> {
            issues.error(null, "Cannot resolve type '${ti.typeName.value}' in namespace '${ti.namespace.qualifiedName.value}', is an import needed?")
            null
        }

        else -> ti
    }

    fun PropertyCall.typeOfPropertyCallFor(self: TypeInstance?): TypeInstance = when {
        // the special property '.\$type' of every value
        PropertyCallDefault.TYPE == this.propertyName && null != self -> StdLibDefault.Type.type()
        else -> self?.allResolvedProperty?.get(PropertyName(this.propertyName))?.typeInstance ?: StdLibDefault.NothingType
    }

    fun MethodCall.typeOfMethodCallFor(self: TypeInstance?): TypeInstance =
        self?.allResolvedMethod?.get(MethodName(this.methodName))?.returnType ?: StdLibDefault.NothingType

    fun IndexOperation.typeOfIndexOperationFor(self: TypeInstance?): TypeInstance =
        self?.typeArguments?.firstOrNull()?.type ?: StdLibDefault.NothingType

    fun WithExpression.typeOfWithExpressionFor(self: TypeInstance): TypeInstance {
        val ctxType = this.withContext.typeOfExpressionFor(self)
        return this.expression.typeOfExpressionFor(ctxType)
    }

    fun WhenExpression.typeOfWhenExpressionFor(self: TypeInstance): TypeInstance {
        val opts = this.options.map {
            it.expression.typeOfExpressionFor(self)
        } + this.elseOption.expression.typeOfExpressionFor(self)
        // a '\$nothing' option does not contribute to the type, unless all options are '\$nothing'
        val notNothing = opts.filter { it != StdLibDefault.NothingType }
        return commonSuperTypeOf(notNothing.ifEmpty { opts })
    }

    fun TernaryConditionExpression.typeOfTernaryConditionFor(self: TypeInstance): TypeInstance {
        val opts = listOf(
            trueExpression.typeOfExpressionFor(self),
            falseExpression.typeOfExpressionFor(self),
        )
        return commonSuperTypeOf(opts)
    }

    //TODO: add operator functions to StdLib
    fun InfixExpression.typeOfInfixExpressionFor(self: TypeInstance): TypeInstance {
        //TODO: Operator precedence
        var result = this.expressions.first().typeOfExpressionFor(self)
        for (i in this.operators.indices) {
            val op = this.operators[i]
            val rhsExpr = this.expressions[i + 1]
            val rhs = rhsExpr.typeOfExpressionFor(self)
            result = typeOfInfixOperator(result, op, rhs)
        }
        return result
    }

    private fun typeOfInfixOperator(lhs: TypeInstance, op: String, rhs: TypeInstance): TypeInstance = when (op) {
        "==", "!=", "<", "<=", ">", ">=", "and", "or", "xor" -> StdLibDefault.Boolean
        "+", "-", "*", "/", "%" -> lhs
        else -> error("Unsupported Operator '$op'")
    }

    fun CreateObjectExpression.typeOfCreateObjectExpressionFor(self: TypeInstance): TypeInstance =
        typesDomain.findFirstDefinitionByPossiblyQualifiedNameOrNull(this.possiblyQualifiedTypeName)?.type() ?: StdLibDefault.NothingType

    fun CreateTupleExpression.typeOfCreateTupleExpressionFor(self: TypeInstance): TypeInstance {
        val args = this.propertyAssignments.map {
            val t = typeFor(it.rhs, self)
            val n = PropertyName(it.variable.name)
            TypeArgumentNamedSimple(n, t)
        }
        return StdLibDefault.TupleType.typeTuple(args)
    }

    // the result of an OnExpression is the object its expression gives, with the properties set
    fun OnExpression.typeOfOnExpressionFor(self: TypeInstance): TypeInstance =
        this.expression.typeOfExpressionFor(self)

    fun LambdaExpression.typeOfLambdaExpressionFor(self: TypeInstance): TypeInstance {
        TODO()
    }

    fun CastExpression.typeOfCastExpressionFor(self: TypeInstance): TypeInstance =
        this.targetType.typeOfTypeReference()

    fun GroupExpression.typeOfGroupExpressionFor(self: TypeInstance): TypeInstance =
        this.expression.typeOfExpressionFor(self)

    /**
     * List(a, b, ...) and Set(a, b, ...) are a List/Set of the common supertype of the argument types (Any if there are no arguments),
     * Pair(a, b) is a Pair of the argument types, other functions have their declared return type.
     * A function that is not in the std function library is given by the user of the interpreter, so its type is Any.
     */
    fun FunctionCall.typeOfFunctionCallFor(self: TypeInstance): TypeInstance {
        val argTypes = this.arguments.map { it.typeOfExpressionFor(self) }
        fun elementType(): TypeInstance {
            // a '\$nothing' argument does not contribute to the element type, unless all arguments are '\$nothing'
            val notNothing = argTypes.filter { it != StdLibDefault.NothingType }
            return when {
                argTypes.isEmpty() -> StdLibDefault.AnyType
                else -> commonSuperTypeOf(notNothing.ifEmpty { argTypes })
            }
        }
        val funcName = this.possiblyQualifiedName.value
        return when (funcName) {
            "List" -> StdLibDefault.List.type(listOf(elementType().asTypeArgument))
            "Set" -> StdLibDefault.Set.type(listOf(elementType().asTypeArgument))
            "Pair" -> when (argTypes.size) {
                2 -> StdLibDefault.Pair.type(listOf(argTypes[0].asTypeArgument, argTypes[1].asTypeArgument))
                else -> StdLibDefault.Pair.type(listOf(StdLibDefault.AnyType.asTypeArgument, StdLibDefault.AnyType.asTypeArgument))
            }
            else -> StdFunctionLib.findFirstFunctionNamed(funcName)?.returnTypeReference?.typeOfTypeReference()
                ?: StdLibDefault.AnyType
        }
    }

    fun TypeReference.typeOfTypeReference(): TypeInstance {
        //val td = typeModel.findFirstByPossiblyQualifiedOrNull(this.possiblyQualifiedName) ?: StdLibDefault.NothingType.resolvedDeclaration
        val targs = this.typeArguments.map {
            val argTArgs = it.typeArguments.map { it.typeOfTypeReference().asTypeArgument }
            contextNamespace.createTypeInstance(null, it.possiblyQualifiedName, argTArgs, it.isNullable)
                .asTypeArgument
        }
        return contextNamespace.createTypeInstance(null, this.possiblyQualifiedName, targs, this.isNullable)

    }

    fun commonSuperTypeOf(types: List<TypeInstance>): TypeInstance {
        return when {
            types.toSet().size == 1 -> types.first()
            else -> types.reduce { acc, it -> acc.commonSuperType(it) }
        }
    }
}