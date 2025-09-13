/*
 * Copyright (C) 2023 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.akehurst.language.expressions.processor

//import net.akehurst.language.asmTransform.processor.AsmTransformInterpreter
import net.akehurst.language.agl.Agl
import net.akehurst.language.api.processor.EvaluationContext
import net.akehurst.language.asm.api.AsmValue
import net.akehurst.language.base.api.PossiblyQualifiedName
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.expressions.api.*
import net.akehurst.language.expressions.asm.RootExpressionDefault
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.types.api.*
import net.akehurst.language.types.asm.StdLibDefault
import net.akehurst.language.types.asm.TypeArgumentNamedSimple



interface TypedObject<out SelfType:Any> {
    val self: SelfType
    val type: TypeInstance
    fun asString(): String
}

interface ObjectGraph<SelfType:Any> {
    var typesDomain: TypesDomain

    val createdStructuresByType:Map<TypeInstance, List<SelfType>>

    fun typeFor(obj: SelfType?): TypeInstance
    fun toTypedObject(obj:SelfType?) : TypedObject<SelfType>

    fun isNothing(obj: TypedObject<SelfType>): Boolean
    fun equalTo(lhs: TypedObject<SelfType>, rhs: TypedObject<SelfType>): Boolean

    fun nothing(): TypedObject<SelfType>
    fun any(value: Any): TypedObject<SelfType>
    fun createPrimitiveValue(qualifiedTypeName: QualifiedName, value: Any): TypedObject<SelfType>
    fun createStructureValue(possiblyQualifiedTypeName: PossiblyQualifiedName, constructorArgs: Map<String, TypedObject<SelfType>>): TypedObject<SelfType>
    fun createCollection(qualifiedTypeName: QualifiedName, collection:Iterable<TypedObject<SelfType>>): TypedObject<SelfType>
    fun createTupleValue(typeArgs: List<TypeArgumentNamed>): TypedObject<SelfType>
    fun createLambdaValue(lambda: (it: TypedObject<SelfType>) -> TypedObject<SelfType>): TypedObject<SelfType>

    fun valueOf(value: TypedObject<SelfType>): Any

    // would like to use Long as index to be compatible with Integer implemented as Long - but index in underlying kotlin is always an Int
    fun getIndex(tobj: TypedObject<SelfType>, index: Int): TypedObject<SelfType>

    /**
     * value of the given PropertyDeclaration or Nothing if no such property exists
     */
    fun getProperty(tobj: TypedObject<SelfType>, propertyName: String): TypedObject<SelfType>
    fun setProperty(tobj: TypedObject<SelfType>, propertyName: String, value: TypedObject<SelfType>)

    fun executeMethod(tobj: TypedObject<SelfType>, methodName: String, args: List<TypedObject<SelfType>>): TypedObject<SelfType>
    fun cast(tobj: TypedObject<SelfType>, newType: TypeInstance): TypedObject<SelfType>
}

////val TypedObject<SelfType>.asmValue
//    get() = when (this) {
//        is TypedObjectAsmValue -> this.self
//        else -> error("Not possible to convert ${this::class.simpleName} to AsmValue")
//    }

open class ExpressionsInterpreterOverTypedObject<SelfType:Any>(
    val objectGraph: ObjectGraph<SelfType>,
    val issues: IssueHolder
) {
    val typeModel = objectGraph.typesDomain
    //val typeResolver = ExpressionTypeResolver(typeModel, issues)

    /**
     * if more than one value is to be passed in as an 'evaluation-context'
     * self can contain a 'tuple' of all the necessary named values
     */
    fun evaluateStr(evc: EvaluationContext<SelfType>, expression: String): TypedObject<SelfType> {
        val result = Agl.registry.agl.expressions.processor!!.process(expression)
        check(result.allIssues.errors.isEmpty()) { result.allIssues.toString() }
        val asm = result.asm!!
        return this.evaluateExpression(evc, asm)
    }

    /**
     * if more than one value is to be passed in as an 'evaluation-context'
     * self can contain a 'tuple' of all the necessary named values
     */
    open fun evaluateExpression(evc: EvaluationContext<SelfType>, expression: Expression): TypedObject<SelfType> = when (expression) {
        is RootExpression -> this.evaluateRootExpression(evc, expression)
        is LiteralExpression -> this.evaluateLiteralExpression(expression)
        is CreateObjectExpression -> this.evaluateCreateObject(evc, expression)
        is CreateTupleExpression -> this.evaluateCreateTuple(evc, expression)
        is OnExpression -> TODO()
        is NavigationExpression -> this.evaluateNavigation(evc, expression)
        is LambdaExpression -> this.evaluateLambda(evc, expression)
        is WithExpression -> this.evaluateWith(evc, expression)
        is WhenExpression -> this.evaluateWhen(evc, expression)
        is InfixExpression -> this.evaluateInfix(evc, expression)
        is CastExpression -> this.evaluateCast(evc, expression)
        is TypeTestExpression -> this.evaluateTypeTest(evc, expression)
        is GroupExpression -> this.evaluateGroup(evc, expression)
        else -> error("Subtype of Expression not handled in evaluateExpression '${expression::class.simpleName}'")
    }

    private fun evaluateRootExpression(evc: EvaluationContext<SelfType>, expression: RootExpression): TypedObject<SelfType> {
        return when {
            expression.isNothing -> objectGraph.nothing()
            expression.isSelf -> {
                //_issues.error(null, "evaluation of 'self' only works if self is a String, got an object of type '${self::class.simpleName}'")
                evc.self
                    ?: error("No '\$self' value defined in Evaluation Context: $evc")
            }

            expression.name.startsWith("\$") -> evaluateSpecial(evc, expression.name)
            else -> evc.getOrInParent(expression.name)
                ?: evc.self?.let { evaluatePropertyName(it, PropertyName(expression.name)) }
                ?: let {
                    issues.error(null,"Evaluation Context does not contain '${expression.name}' and there is no 'self' object with that property name")
                    objectGraph.nothing()
                }
        }
    }

    private fun evaluateSpecial(evc: EvaluationContext<SelfType>, name: String): TypedObject<SelfType> {
        // the name must exist as a property of the self which must be a tuple
        return evc.getOrInParent(name)
            ?: evc.self?.let { evaluatePropertyName(it, PropertyName(name)) }
            ?: error("Evaluation Context does not contain '$name' and there is no 'self' object with that property name")
    }

    private fun evaluateLiteralExpression(expression: LiteralExpression): TypedObject<SelfType> =
        objectGraph.createPrimitiveValue(expression.qualifiedTypeName, expression.value)

    private fun evaluateNavigation(evc: EvaluationContext<SelfType>, expression: NavigationExpression): TypedObject<SelfType> {
        // start should be a RootExpression or LiteralExpression
        val st = expression.start
        val start = evaluateExpression(evc, st)
        val result = expression.parts.fold(start) { acc, it ->
            when (it) {
                is PropertyCall -> evaluatePropertyName(acc, PropertyName(it.propertyName))
                is MethodCall -> evaluateMethodCall(evc, acc, MethodName(it.methodName), it.arguments)
                is IndexOperation -> evaluateIndexOperation(evc, acc, it.indices)
                else -> error("should not happen")
            }
        }
        return result
    }

    private fun evaluatePropertyName(obj: TypedObject<SelfType>, propertyName: PropertyName): TypedObject<SelfType> {
        val type = obj.type
        return objectGraph.getProperty(obj, propertyName.value)
    }

    private fun evaluateMethodCall(evc: EvaluationContext<SelfType>, obj: TypedObject<SelfType>, methodName: MethodName, args: List<Expression>): TypedObject<SelfType> {
        val type = obj.type
        val md = type.resolvedDeclaration.findAllMethodOrNull(methodName)
        return when (md) {
            null -> {
                issues.error(null, "Method '$methodName' not found on type '${obj.type.typeName}'")
                objectGraph.nothing()
            }

            else -> {
                val argValues = args.map {
                    evaluateExpression(evc, it)
                }
                objectGraph.executeMethod(obj, methodName.value, argValues)
            }
        }
    }

    private fun evaluateIndexOperation(evc: EvaluationContext<SelfType>, obj: TypedObject<SelfType>, indices: List<Expression>): TypedObject<SelfType> {
        return when {
            obj.type.resolvedDeclaration.conformsTo(StdLibDefault.List) -> {
                when (indices.size) {
                    1 -> {
                        val idx = evaluateExpression(evc, indices[0])
                        when {
                            idx.type.conformsTo(StdLibDefault.Integer) -> {
                                val listElementType = obj.type.typeArguments.getOrNull(0)?.type
                                    ?: StdLibDefault.AnyType
                                val i = objectGraph.valueOf(idx) as Long
                                val elem = objectGraph.getIndex(obj, i.toInt())
                                when {
                                    objectGraph.nothing() == elem -> objectGraph.nothing()
                                    else -> {
                                        val elemType = typeModel.findByQualifiedNameOrNull(elem.type.qualifiedTypeName)?.type()
                                        when {
                                            null == elemType -> {
                                                issues.error(null, "Cannot find type '${elem.type.qualifiedTypeName}' of List element '$elem'")
                                                objectGraph.cast(elem, listElementType)
                                            }

                                            elemType.resolvedDeclaration is TupleType -> objectGraph.cast(elem, elemType)
                                            elemType.conformsTo(listElementType) -> objectGraph.cast(elem, elemType)
                                            else -> {
                                                issues.error(
                                                    null,
                                                    "List element '$elem' of type '${elem.type.qualifiedTypeName}' does not conform to the expected List element type of '${listElementType}'"
                                                )
                                                objectGraph.cast(elem, elemType)
                                            }
                                        }
                                    }
                                }
                            }

                            else -> {
                                issues.error(null, "Index value must evaluate to an Integer for Lists")
                                objectGraph.nothing()
                            }
                        }
                    }

                    else -> {
                        issues.error(null, "Only one index value should be used for Lists")
                        objectGraph.nothing()
                    }
                }
            }

            else -> {
                issues.error(null, "Index operation on non List value is not possible: ${obj.asString()}")
                objectGraph.nothing()
            }
        }
    }

    private fun evaluateInfix(evc: EvaluationContext<SelfType>, expression: InfixExpression): TypedObject<SelfType> {
        //TODO: Operator precedence
        var result = evaluateExpression(evc, expression.expressions.first())
        for (i in expression.operators.indices) {
            val op = expression.operators[i]
            val rhsExpr = expression.expressions[i + 1]
            val rhs = evaluateExpression(evc, rhsExpr)
            result = evaluateInfixOperator(result, op, rhs)
        }
        return result
    }

    //TODO: add operator functions to StdLib
    private fun evaluateInfixOperator(lhs: TypedObject<SelfType>, op: String, rhs: TypedObject<SelfType>): TypedObject<SelfType> = when (op) {
        "==" -> when {
            lhs.type == rhs.type -> {
                val lhsv = objectGraph.valueOf(lhs)
                val rhsv = objectGraph.valueOf(rhs)
                if (lhsv == rhsv) {
                    objectGraph.createPrimitiveValue(StdLibDefault.Boolean.qualifiedTypeName, true)
                } else {
                    objectGraph.createPrimitiveValue(StdLibDefault.Boolean.qualifiedTypeName, false)
                }
            }

            else -> error("'$op' must have same type for lhs and rhs")
        }

        "+" -> when {
            lhs.type.conformsTo(StdLibDefault.String) && rhs.type.conformsTo(StdLibDefault.String) -> {
                val lhsv = objectGraph.valueOf(lhs) as String
                val rhsv = objectGraph.valueOf(rhs) as String
                objectGraph.createPrimitiveValue(StdLibDefault.String.qualifiedTypeName, lhsv + rhsv)
            }

            else -> error("'$op' not supported for '${lhs.type.qualifiedTypeName}' && '${rhs.type.qualifiedTypeName}'")
        }

        else -> error("Unsupported Operator '$op'")
    }

    private fun evaluateWith(evc: EvaluationContext<SelfType>, expression: WithExpression): TypedObject<SelfType> {
        val newSelf = evaluateExpression(evc, expression.withContext)
        return when {
            objectGraph.nothing() == newSelf -> newSelf
            else -> {
                val newEvc = evc.child(mapOf(RootExpressionDefault.SELF.name to newSelf))
                val result = evaluateExpression(newEvc, expression.expression)
                result
            }
        }
    }

    private fun evaluateWhen(evc: EvaluationContext<SelfType>, expression: WhenExpression): TypedObject<SelfType> {
        for (opt in expression.options) {
            val condValue = evaluateExpression(evc, opt.condition)
            when (condValue.type) {
                StdLibDefault.Boolean -> {
                    if (objectGraph.valueOf(condValue) as Boolean) {
                        val result = evaluateExpression(evc, opt.expression)
                        return result // return after first condition found that is true
                    } else {
                        //condition not true
                    }
                }

                else -> error("Conditions/Options in a when expression must result in a Boolean value")
            }
        }
        return evaluateExpression(evc, expression.elseOption.expression)
    }

    private fun evaluateCreateTuple(evc: EvaluationContext<SelfType>, expression: CreateTupleExpression): TypedObject<SelfType> {
        val typeArgs = mutableListOf<TypeArgumentNamed>()
        val tuple = objectGraph.createTupleValue(typeArgs)
        expression.propertyAssignments.forEach {
            val value = evaluateExpression(evc, it.rhs)
            objectGraph.setProperty(tuple, it.lhsPropertyName, value)
            typeArgs.add(TypeArgumentNamedSimple(PropertyName(it.lhsPropertyName), value.type))
        }
        return tuple
    }

    private fun evaluateCreateObject(evc: EvaluationContext<SelfType>, expression: CreateObjectExpression): TypedObject<SelfType> {
        val typeDef = typeModel.findFirstDefinitionByPossiblyQualifiedNameOrNull(expression.possiblyQualifiedTypeName)
            ?: error("Type not found ${expression.possiblyQualifiedTypeName}")
        return when (typeDef) {
            is DataType, is ValueType -> {
                val args = expression.constructorArguments.map { evaluateExpression(evc, it.rhs) }
                val constructorArgs = when {
                    args.isNotEmpty() -> {
                        val constructors = when(typeDef) {
                            is DataType -> typeDef.constructors
                            is ValueType -> typeDef.constructors
                            else -> error("Type '${typeDef.qualifiedName.value}' has no constructors")
                        }
                        val constructor = constructors.firstOrNull { cons ->
                            cons.parameters.size == args.size && cons.parameters.zip(args).all { (p, a) -> a.type.conformsTo(p.typeInstance) }
                        } ?: error("Constructor not found for '${typeDef.qualifiedName.value}' with arguments ${args.joinToString { it.type.qualifiedTypeName.value }}")

                        // val consProps = typeDecl.property.filter { it.characteristics.contains(PropertyCharacteristic.CONSTRUCTOR) }
                        // if (consProps.size != args.size) error("Wrong number of constructor arguments for ${typeDecl.qualifiedName}")

                        constructor.parameters.mapIndexed { idx, pd -> Pair(pd.name.value, args[idx]) }.associate { it }
                    }
                    else -> emptyMap()
                }

                val obj = objectGraph.createStructureValue(expression.possiblyQualifiedTypeName, constructorArgs)
                expression.propertyAssignments.forEach {
                    val value = evaluateExpression(evc, it.rhs)
                    objectGraph.setProperty(obj, it.lhsPropertyName, value)
                }
                return obj
            }
            else -> error("Cannot create an object of type '${typeDef.qualifiedName.value}'")
        }
    }

    private fun evaluateLambda(evc: EvaluationContext<SelfType>, expression: LambdaExpression): TypedObject<SelfType> {
        val lambda = objectGraph.createLambdaValue { it ->
            val newEvc = evc.child(mapOf("it" to it))
            evaluateExpression(newEvc, expression.expression)
        }
        return lambda
    }

    private fun evaluateCast(evc: EvaluationContext<SelfType>, expression: CastExpression): TypedObject<SelfType> {
        //TODO: do we need a type check? or can we assume it is already done in semantic analysis!
        val exprResult = evaluateExpression(evc, expression.expression)
        val tgtType = evaluateTypeReference(expression.targetType)
        return objectGraph.cast(exprResult, tgtType)
    }

    private fun evaluateTypeTest(evc: EvaluationContext<SelfType>, expression: TypeTestExpression): TypedObject<SelfType> {
        //TODO: do we need a type check? or can we assume it is already done in semantic analysis!
        val exprResult = evaluateExpression(evc, expression.expression)
        val tgtType = evaluateTypeReference(expression.targetType)
        val res = exprResult.type.conformsTo(tgtType)
        return objectGraph.toTypedObject(res as SelfType)
    }

    private fun evaluateGroup(evc: EvaluationContext<SelfType>, expression: GroupExpression): TypedObject<SelfType> {
        return evaluateExpression(evc, expression.expression)
    }

     fun evaluateTypeReference(typeReference: TypeReference): TypeInstance {
        //TODO: issues rather than exceptions!
        val decl = typeModel.findFirstDefinitionByPossiblyQualifiedNameOrNull(typeReference.possiblyQualifiedName) ?: error("Type not found ${typeReference.possiblyQualifiedName}")
        val targs = typeReference.typeArguments.map { evaluateTypeReference(it).asTypeArgument }
        return decl.type(targs, typeReference.isNullable)
    }

}

