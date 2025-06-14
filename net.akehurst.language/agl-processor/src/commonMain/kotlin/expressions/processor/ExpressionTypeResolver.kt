package net.akehurst.language.expressions.processor

import net.akehurst.language.agl.Agl
import net.akehurst.language.expressions.api.*
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.typemodel.api.*
import net.akehurst.language.typemodel.asm.StdLibDefault
import net.akehurst.language.typemodel.asm.TypeArgumentNamedSimple

class ExpressionTypeResolver(
    val typeModel: TypeModel,
    val contextNamespace: TypeNamespace,
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
        is InfixExpression -> this.typeOfInfixExpressionFor(self)
        is CastExpression -> this.typeOfCastExpressionFor(self)
        is GroupExpression -> this.typeOfGroupExpressionFor(self)
        else -> error("Subtype of Expression not handled in 'typeOfExpressionFor' : '${this::class.simpleName}'")

    }

    fun LiteralExpression.typeOfLiteralExpressionFor(self: TypeInstance): TypeInstance =
        typeModel.findByQualifiedNameOrNull(this.qualifiedTypeName)?.type() ?: StdLibDefault.NothingType

    fun RootExpression.typeOfRootExpressionFor(self: TypeInstance): TypeInstance = when {
        this.isNothing -> StdLibDefault.NothingType
        this.isSelf -> self
        else -> {
            when (self.resolvedDeclaration) {
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
            is LiteralExpression -> TODO()
            is RootExpression -> when {
                st.isNothing -> StdLibDefault.NothingType
                st.isSelf -> self
                else -> {
                    self.allResolvedProperty[PropertyName(st.name)]?.typeInstance
                }
            }

            else -> error("type of Navigation.start not handled")
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

            else -> error("type of Navigation.start not handled")
        }
        var acc = pd?.typeInstance
        val r = expr.parts.forEach {
            pd = when (it) {
                is PropertyCall -> acc?.allResolvedProperty?.get(PropertyName(it.propertyName))
                else -> null //everything must be property calls
            }
            acc = pd?.typeInstance
        }
        return pd
    }

    fun PropertyCall.typeOfPropertyCallFor(self: TypeInstance?): TypeInstance =
        self?.allResolvedProperty?.get(PropertyName(this.propertyName))?.typeInstance ?: StdLibDefault.NothingType

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
        }
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
        "==" -> StdLibDefault.Boolean
        "+" -> lhs
        else -> error("Unsupported Operator '$op'")
    }

    fun CreateObjectExpression.typeOfCreateObjectExpressionFor(self: TypeInstance): TypeInstance =
        typeModel.findFirstDefinitionByPossiblyQualifiedNameOrNull(this.possiblyQualifiedTypeName)?.type() ?: StdLibDefault.NothingType

    fun CreateTupleExpression.typeOfCreateTupleExpressionFor(self: TypeInstance): TypeInstance {
        val args = this.propertyAssignments.map {
            val t = typeFor(it.rhs, self)
            val n = PropertyName(it.lhsPropertyName)
            TypeArgumentNamedSimple(n, t)
        }
        return StdLibDefault.TupleType.typeTuple(args)
    }

    fun OnExpression.typeOfOnExpressionFor(self: TypeInstance): TypeInstance {
        TODO()
    }

    fun LambdaExpression.typeOfLambdaExpressionFor(self: TypeInstance): TypeInstance {
        TODO()
    }

    fun CastExpression.typeOfCastExpressionFor(self: TypeInstance): TypeInstance =
        this.targetType.typeOfTypeReference()

    fun GroupExpression.typeOfGroupExpressionFor(self: TypeInstance): TypeInstance =
        this.expression.typeOfExpressionFor(self)

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