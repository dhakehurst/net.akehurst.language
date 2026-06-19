/*
 * Copyright (C) 2025 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

import net.akehurst.language.agl.Agl
import net.akehurst.language.expressions.api.*
import net.akehurst.language.expressions.asm.RootExpressionDefault
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.objectgraph.api.*
import net.akehurst.language.types.api.*
import net.akehurst.language.types.asm.StdLibDefault
import net.akehurst.language.types.asm.TypeArgumentNamedSimple

//TODO: merge with other
open class ExpressionsInterpreterOverTypedObjectSuspending(
    val objectGraph: ObjectGraphAccessorMutator
) {
    val issues: IssueHolder get() = objectGraph.issues
    val typeModel = objectGraph.typesDomain
    //val typeResolver = ExpressionTypeResolver(typeModel, issues)

    /**
     * if more than one value is to be passed in as an 'evaluation-context'
     * self can contain a 'tuple' of all the necessary named values
     */
    suspend fun evaluateStr(evc: EvaluationContext, expression: String): TypedObject {
        val result = Agl.registry.agl.expressions.processor!!.process(expression)
        check(result.allIssues.errors.isEmpty()) { result.allIssues.toString() }
        val asm = result.asm!!
        return this.evaluateExpression(evc, asm)
    }

    /**
     * if more than one value is to be passed in as an 'evaluation-context'
     * self can contain a 'tuple' of all the necessary named values
     */
    open suspend fun evaluateExpression(evc: EvaluationContext, expression: Expression): TypedObject = when (expression) {
        is RootExpression -> this.evaluateRootExpression(evc, expression)
        is LiteralExpression -> this.evaluateLiteralExpression(expression)
        is CreateObjectExpression -> this.evaluateCreateObject(evc, expression)
        is FunctionCall -> this.evaluateFunctionCall(evc, expression)
        is CreateTupleExpression -> this.evaluateCreateTuple(evc, expression)
        is OnExpression -> TODO()
        is NavigationExpression -> this.evaluateNavigation(evc, expression)
        is LambdaExpression -> this.evaluateLambda(evc, expression)
        is WithExpression -> this.evaluateWith(evc, expression)
        is WhenExpression -> this.evaluateWhen(evc, expression)
        is TernaryConditionExpression -> this.evaluateTernaryCondition(evc, expression)
        is InfixExpression -> this.evaluateInfix(evc, expression)
        is CastExpression -> this.evaluateCast(evc, expression)
        is TypeTestExpression -> this.evaluateTypeTest(evc, expression)
        is GroupExpression -> this.evaluateGroup(evc, expression)
        else -> error("Subtype of Expression not handled in evaluateExpression '${expression::class.simpleName}'")
    }

    private suspend fun evaluateRootExpression(evc: EvaluationContext, expression: RootExpression): TypedObject {
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
                    issues.error(null, "Evaluation Context does not contain '${expression.name}' and there is no 'self' object with that property name")
                    objectGraph.nothing()
                }
        }
    }

    private suspend fun evaluateSpecial(evc: EvaluationContext, name: String): TypedObject {
        // the name must exist as a property of the self which must be a tuple
        return evc.getOrInParent(name)
            ?: evc.self?.let { evaluatePropertyName(it, PropertyName(name)) }
            ?: error("Evaluation Context does not contain '$name' and there is no 'self' object with that property name")
    }

    private fun evaluateLiteralExpression(expression: LiteralExpression): TypedObject =
        objectGraph.createPrimitiveValue(expression.qualifiedTypeName, expression.value)

    private suspend fun evaluateFunctionCall(evc: EvaluationContext, expression: FunctionCall): TypedObject {
        val argValues = expression.arguments.map {
            evaluateExpression(evc, it)
        }
        return objectGraph.callFunction(expression.possiblyQualifiedName.value, argValues) { tr -> evaluateTypeReference(tr) }
    }

    private suspend fun evaluateNavigation(evc: EvaluationContext, expression: NavigationExpression): TypedObject {
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

    private suspend fun evaluatePropertyName(obj: TypedObject, propertyName: PropertyName): TypedObject {
        return when {
            objectGraph.isNothing(obj) -> {
                issues.warn(null, $$"Cannot get property '$${propertyName.value}' on $nothing, returning $nothing.")
                objectGraph.nothing()
            }

            else -> {
                obj.getPropertySuspend(propertyName.value)
            }
        }
    }

    private suspend fun evaluateMethodCall(evc: EvaluationContext, obj: TypedObject, methodName: MethodName, args: List<Expression>): TypedObject {
        return when {
            objectGraph.isNothing(obj) -> {
                issues.warn(null, $$"Cannot call method '$${methodName.value}' on on $nothing, returning $nothing.")
                objectGraph.nothing()
            }

            else -> {
                val argValues = args.map {
                    evaluateExpression(evc, it)
                }
                obj.executeMethodSuspend(methodName.value, argValues)
            }
        }
    }

    private suspend fun evaluateIndexOperation(evc: EvaluationContext, obj: TypedObject, indices: List<Expression>): TypedObject {
        return when {
            obj.type.resolvedDefinition.conformsTo(StdLibDefault.List) -> evaluateIndexOperationOnList(evc, obj, indices)
            obj.type.resolvedDefinition.conformsTo(StdLibDefault.Map) -> evaluateIndexOperationOnMap(evc, obj, indices)
            else -> {
                issues.error(null, "Index operation on non List value is not possible: ${obj.asString()}")
                objectGraph.nothing()
            }
        }
    }

    private suspend fun evaluateIndexOperationOnList(evc: EvaluationContext, obj: TypedObject, indices: List<Expression>): TypedObject {
        return when (indices.size) {
            1 -> {
                val idx = evaluateExpression(evc, indices[0])
                when {
                    idx.type.conformsTo(StdLibDefault.Integer) -> {
                        val listElementType = obj.type.typeArguments.getOrNull(0)?.type
                            ?: StdLibDefault.AnyType
                        val i = objectGraph.valueOf(idx) as Long
                        val elem = objectGraph.getFromListWithIndex(obj, i.toInt())
                        when {
                            objectGraph.nothing() == elem -> objectGraph.nothing()
                            else -> {
                                val elemType = typeModel.findByQualifiedNameOrNull(elem.type.qualifiedTypeName)?.type(elem.type.typeArguments)
                                when {
                                    null == elemType -> {
                                        issues.error(null, "Cannot find type '${elem.type.qualifiedTypeName}' of List element '$elem'")
                                        objectGraph.cast(elem, listElementType)
                                    }

                                    elemType.resolvedDefinition is TupleType -> objectGraph.cast(elem, elemType)
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

    private suspend fun evaluateIndexOperationOnMap(evc: EvaluationContext, obj: TypedObject, indices: List<Expression>): TypedObject {
        return when (indices.size) {
            1 -> {
                val idx = evaluateExpression(evc, indices[0])
                val keyType = obj.type.typeArguments.getOrNull(0)?.type
                val valueType = obj.type.typeArguments.getOrNull(1)?.type
                when {
                    null == keyType -> {
                        issues.error(null, "Key type not found for object with type '${obj.type.signature(null, 0)}'")
                        objectGraph.nothing()
                    }

                    null == valueType -> {
                        issues.error(null, "Value type not found for object with type '${obj.type.signature(null, 0)}'")
                        objectGraph.nothing()
                    }

                    idx.type.conformsTo(keyType) -> {
                        val elem = objectGraph.getFromMapWithKey(obj, idx)
                        when {
                            objectGraph.nothing() == elem -> objectGraph.nothing()
                            else -> {
                                val elemType = typeModel.findByQualifiedNameOrNull(elem.type.qualifiedTypeName)?.type()
                                when {
                                    null == elemType -> {
                                        issues.error(null, "Cannot find type '${elem.type.qualifiedTypeName}' of Map element '$elem'")
                                        objectGraph.cast(elem, valueType)
                                    }

                                    elemType.resolvedDefinition is TupleType -> objectGraph.cast(elem, elemType)
                                    elemType.conformsTo(valueType) -> objectGraph.cast(elem, elemType)
                                    else -> {
                                        issues.warn(
                                            null,
                                            "Map element '$elem' does not conform to the expected Map element value type of '${valueType}', using '$elemType'."
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
                issues.error(null, "Only one index value should be used for Maps")
                objectGraph.nothing()
            }
        }
    }

    private suspend fun evaluateTernaryCondition(evc: EvaluationContext, expression: TernaryConditionExpression): TypedObject {
        val condValue = evaluateExpression(evc, expression.condition)
        return when (condValue.type) {
            StdLibDefault.Boolean -> {
                if (objectGraph.valueOf(condValue) as Boolean) {
                    evaluateExpression(evc, expression.trueExpression)
                } else {
                    evaluateExpression(evc, expression.falseExpression)
                }
            }

            else -> error("The condition in a ternary conditional expression must result in a Boolean value: '${expression.condition}' is of type '${condValue.type}' = ${condValue.self}")
        }
    }

    private suspend fun evaluateInfix(evc: EvaluationContext, expression: InfixExpression): TypedObject {
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
    private fun evaluateInfixOperator(lhs: TypedObject, op: String, rhs: TypedObject): TypedObject = when (op) {
        // logical
        "and" -> when {
            // both nothing -> nothing
            objectGraph.isNothing(lhs) && objectGraph.isNothing(rhs) -> objectGraph.nothing()
            // either false -> false
            lhs.type.conformsTo(StdLibDefault.Boolean) && (objectGraph.valueOf(lhs) as Boolean).not() -> lhs
            rhs.type.conformsTo(StdLibDefault.Boolean) && (objectGraph.valueOf(rhs) as Boolean).not() -> rhs
            // both boolean -> lhs && rhs
            lhs.type.conformsTo(StdLibDefault.Boolean) && rhs.type.conformsTo(StdLibDefault.Boolean) -> {
                val lhsv = objectGraph.valueOf(lhs) as Boolean
                val rhsv = objectGraph.valueOf(rhs) as Boolean
                objectGraph.createPrimitiveValue(StdLibDefault.Boolean.qualifiedTypeName, lhsv && rhsv)
            }

            else -> {
                issues.error(null, "'$op' not supported for types '${lhs.type.qualifiedTypeName} and ${rhs.type.qualifiedTypeName}'")
                objectGraph.nothing()
            }
        }

        "or" -> when {
            // both nothing -> nothing
            objectGraph.isNothing(lhs) && objectGraph.isNothing(rhs) -> objectGraph.nothing()
            // either true -> true
            lhs.type.conformsTo(StdLibDefault.Boolean) && (objectGraph.valueOf(lhs) as Boolean) -> lhs
            rhs.type.conformsTo(StdLibDefault.Boolean) && (objectGraph.valueOf(rhs) as Boolean) -> rhs
            // both boolean -> lhs || rhs
            lhs.type.conformsTo(StdLibDefault.Boolean) && rhs.type.conformsTo(StdLibDefault.Boolean) -> {
                val lhsv = objectGraph.valueOf(lhs) as Boolean
                val rhsv = objectGraph.valueOf(rhs) as Boolean
                objectGraph.createPrimitiveValue(StdLibDefault.Boolean.qualifiedTypeName, lhsv || rhsv)
            }

            else -> {
                issues.error(null, "'$op' not supported for types '${lhs.type.qualifiedTypeName} and ${rhs.type.qualifiedTypeName}'")
                objectGraph.nothing()
            }
        }

        "xor" -> when {
            // both nothing -> nothing
            objectGraph.isNothing(lhs) && objectGraph.isNothing(rhs) -> objectGraph.nothing()
            // both boolean -> lhs xor rhs
            lhs.type.conformsTo(StdLibDefault.Boolean) && rhs.type.conformsTo(StdLibDefault.Boolean) -> {
                val lhsv = objectGraph.valueOf(lhs) as Boolean
                val rhsv = objectGraph.valueOf(rhs) as Boolean
                objectGraph.createPrimitiveValue(StdLibDefault.Boolean.qualifiedTypeName, lhsv xor rhsv)
            }

            else -> {
                issues.error(null, "'$op' not supported for types '${lhs.type.qualifiedTypeName} and ${rhs.type.qualifiedTypeName}'")
                objectGraph.nothing()
            }
        }

        // comparison
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

            else -> objectGraph.createPrimitiveValue(StdLibDefault.Boolean.qualifiedTypeName, false)
        }

        "!=" -> when {
            lhs.type == rhs.type -> {
                val lhsv = objectGraph.valueOf(lhs)
                val rhsv = objectGraph.valueOf(rhs)
                if (lhsv != rhsv) {
                    objectGraph.createPrimitiveValue(StdLibDefault.Boolean.qualifiedTypeName, true)
                } else {
                    objectGraph.createPrimitiveValue(StdLibDefault.Boolean.qualifiedTypeName, false)
                }
            }

            else -> objectGraph.createPrimitiveValue(StdLibDefault.Boolean.qualifiedTypeName, false)
        }

        // arithmetic
        "/" -> when {
            lhs.type.conformsTo(StdLibDefault.Integer) && rhs.type.conformsTo(StdLibDefault.Integer) -> {
                val lhsv = objectGraph.valueOf(lhs) as Long
                val rhsv = objectGraph.valueOf(rhs) as Long
                objectGraph.createPrimitiveValue(StdLibDefault.Integer.qualifiedTypeName, lhsv / rhsv)
            }

            lhs.type.conformsTo(StdLibDefault.Real) && rhs.type.conformsTo(StdLibDefault.Integer) -> {
                val lhsv = objectGraph.valueOf(lhs) as Double
                val rhsv = objectGraph.valueOf(rhs) as Double
                objectGraph.createPrimitiveValue(StdLibDefault.Real.qualifiedTypeName, lhsv / rhsv)
            }

            else -> {
                issues.error(null, "'$op' not supported for types '${lhs.type.qualifiedTypeName} and ${rhs.type.qualifiedTypeName}'")
                objectGraph.nothing()
            }
        }

        "*" -> when {
            lhs.type.conformsTo(StdLibDefault.Integer) && rhs.type.conformsTo(StdLibDefault.Integer) -> {
                val lhsv = objectGraph.valueOf(lhs) as Long
                val rhsv = objectGraph.valueOf(rhs) as Long
                objectGraph.createPrimitiveValue(StdLibDefault.Integer.qualifiedTypeName, lhsv * rhsv)
            }

            lhs.type.conformsTo(StdLibDefault.Real) && rhs.type.conformsTo(StdLibDefault.Real) -> {
                val lhsv = objectGraph.valueOf(lhs) as Double
                val rhsv = objectGraph.valueOf(rhs) as Double
                objectGraph.createPrimitiveValue(StdLibDefault.Integer.qualifiedTypeName, lhsv * rhsv)
            }

            else -> {
                issues.error(null, "'$op' not supported for types '${lhs.type.qualifiedTypeName} and ${rhs.type.qualifiedTypeName}'")
                objectGraph.nothing()
            }
        }

        "%" -> when {
            lhs.type.conformsTo(StdLibDefault.Integer) && rhs.type.conformsTo(StdLibDefault.Integer) -> {
                val lhsv = objectGraph.valueOf(lhs) as Long
                val rhsv = objectGraph.valueOf(rhs) as Long
                objectGraph.createPrimitiveValue(StdLibDefault.Integer.qualifiedTypeName, lhsv % rhsv)
            }

            lhs.type.conformsTo(StdLibDefault.Real) && rhs.type.conformsTo(StdLibDefault.Real) -> {
                val lhsv = objectGraph.valueOf(lhs) as Double
                val rhsv = objectGraph.valueOf(rhs) as Double
                objectGraph.createPrimitiveValue(StdLibDefault.Real.qualifiedTypeName, lhsv % rhsv)
            }

            else -> {
                issues.error(null, "'$op' not supported for types '${lhs.type.qualifiedTypeName} and ${rhs.type.qualifiedTypeName}'")
                objectGraph.nothing()
            }
        }

        "+" -> when {
            objectGraph.isNothing(lhs) && objectGraph.isNothing(rhs) -> objectGraph.nothing()
            objectGraph.isNothing(lhs) -> rhs
            objectGraph.isNothing(rhs) -> lhs
            lhs.type.conformsTo(StdLibDefault.String) && rhs.type.conformsTo(StdLibDefault.String) -> {
                val lhsv = objectGraph.valueOf(lhs) as String
                val rhsv = objectGraph.valueOf(rhs) as String
                objectGraph.createPrimitiveValue(StdLibDefault.String.qualifiedTypeName, lhsv + rhsv)
            }

            lhs.type.conformsTo(StdLibDefault.Integer) && rhs.type.conformsTo(StdLibDefault.Integer) -> {
                val lhsv = objectGraph.valueOf(lhs) as Long
                val rhsv = objectGraph.valueOf(rhs) as Long
                objectGraph.createPrimitiveValue(StdLibDefault.Integer.qualifiedTypeName, lhsv + rhsv)
            }

            lhs.type.conformsTo(StdLibDefault.Real) && rhs.type.conformsTo(StdLibDefault.Real) -> {
                val lhsv = objectGraph.valueOf(lhs) as Double
                val rhsv = objectGraph.valueOf(rhs) as Double
                objectGraph.createPrimitiveValue(StdLibDefault.Real.qualifiedTypeName, lhsv + rhsv)
            }

            else -> {
                issues.error(null, "'$op' not supported for types '${lhs.type.qualifiedTypeName} and ${rhs.type.qualifiedTypeName}'")
                objectGraph.nothing()
            }
        }

        "-" -> when {
            lhs.type.conformsTo(StdLibDefault.Integer) && rhs.type.conformsTo(StdLibDefault.Integer) -> {
                val lhsv = objectGraph.valueOf(lhs) as Long
                val rhsv = objectGraph.valueOf(rhs) as Long
                objectGraph.createPrimitiveValue(StdLibDefault.Integer.qualifiedTypeName, lhsv - rhsv)
            }

            lhs.type.conformsTo(StdLibDefault.Real) && rhs.type.conformsTo(StdLibDefault.Real) -> {
                val lhsv = objectGraph.valueOf(lhs) as Double
                val rhsv = objectGraph.valueOf(rhs) as Double
                objectGraph.createPrimitiveValue(StdLibDefault.Real.qualifiedTypeName, lhsv - rhsv)
            }

            else -> {
                issues.error(null, "'$op' not supported for types '${lhs.type.qualifiedTypeName} and ${rhs.type.qualifiedTypeName}'")
                objectGraph.nothing()
            }
        }

        else -> {
            issues.error(null, "'$op' not supported for types '${lhs.type.qualifiedTypeName} and ${rhs.type.qualifiedTypeName}'")
            objectGraph.nothing()
        }
    }

    private suspend fun evaluateWith(evc: EvaluationContext, expression: WithExpression): TypedObject {
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

    private suspend fun evaluateWhen(evc: EvaluationContext, expression: WhenExpression): TypedObject {
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

                else -> error("Conditions/Options in a when expression must result in a Boolean value: '${opt.condition}'")
            }
        }
        return evaluateExpression(evc, expression.elseOption.expression)
    }

    private suspend fun evaluateLambda(evc: EvaluationContext, expression: LambdaExpression): TypedObject {
        val lambda = objectGraph.createLambdaValueSuspend { it ->
            val newEvc = evc.child(mapOf("it" to it))
            evaluateExpression(newEvc, expression.expression)
        }
        return lambda
    }

    private suspend fun evaluateCast(evc: EvaluationContext, expression: CastExpression): TypedObject {
        //TODO: do we need a type check? or can we assume it is already done in semantic analysis!
        val exprResult = evaluateExpression(evc, expression.expression)
        val tgtType = evaluateTypeReference(expression.targetType)
        return objectGraph.cast(exprResult, tgtType)
    }

    private suspend fun evaluateTypeTest(evc: EvaluationContext, expression: TypeTestExpression): TypedObject {
        //TODO: do we need a type check? or can we assume it is already done in semantic analysis!
        val exprResult = evaluateExpression(evc, expression.expression)
        val tgtType = evaluateTypeReference(expression.targetType)
        val res = exprResult.type.conformsTo(tgtType)
        return objectGraph.createPrimitiveValue(StdLibDefault.Boolean.qualifiedTypeName, res)
    }

    private suspend fun evaluateGroup(evc: EvaluationContext, expression: GroupExpression): TypedObject {
        return evaluateExpression(evc, expression.expression)
    }

    fun evaluateTypeReference(typeReference: TypeReference): TypeInstance {
        //TODO: issues rather than exceptions!
        val decl = typeModel.findFirstDefinitionByPossiblyQualifiedNameOrNull(typeReference.possiblyQualifiedName) ?: error("Type not found ${typeReference.possiblyQualifiedName}")
        val targs = typeReference.typeArguments.map { evaluateTypeReference(it).asTypeArgument }
        return decl.type(targs, typeReference.isNullable)
    }

    // mutation
    private suspend fun evaluateCreateTuple(evc: EvaluationContext, expression: CreateTupleExpression): TypedObject {
        val typeArgs = mutableListOf<TypeArgumentNamed>()
        val tuple = objectGraph.createTupleValue(typeArgs)
        expression.propertyAssignments.forEach {
            val value = evaluateExpression(evc, it.rhs)
            tuple.setPropertySuspend(it.variable.name, value)
            typeArgs.add(TypeArgumentNamedSimple(PropertyName(it.variable.name), value.type))
        }
        return tuple
    }

    private suspend fun evaluateCreateObject(evc: EvaluationContext, expression: CreateObjectExpression): TypedObject {
        return constructObject(evc, expression).also { self ->
            propertyAssignmentBlock(evc, self, expression.propertyAssignments)
        }
    }

    /**
     * Only construct the object, do not execute the property assignment block.
     * Separation of construct and setProperties needed for M2m interpreter
     */
    suspend fun constructObject(evc: EvaluationContext, expression: CreateObjectExpression): TypedObject {
        val typeDef = typeModel.findFirstDefinitionByPossiblyQualifiedNameOrNull(expression.possiblyQualifiedTypeName)
        return when (typeDef) {
            null -> error("Type not found ${expression.possiblyQualifiedTypeName}")
            is DataType, is ValueType -> {
                val args = expression.constructorArguments.map { evaluateExpression(evc, it.rhs) }
                val constructorArgs = when {
                    args.isNotEmpty() -> {
                        val constructors = when (typeDef) {
                            is DataType -> typeDef.constructors
                            is ValueType -> typeDef.constructors
                            else -> error("Type '${typeDef.qualifiedName.value}' has no constructors")
                        }
                        val constructor = constructors.firstOrNull { cons ->
                            cons.parameters.size == args.size && cons.parameters.zip(args).all { (p, a) -> a.type.conformsTo(p.typeInstance) }
                        } ?: error("Constructor not found for '${typeDef.qualifiedName.value}' with arguments ${args.joinToString { it.type.qualifiedTypeName.value }}")

                        // val consProps = typeDecl.property.filter { it.characteristics.contains(PropertyCharacteristic.CONSTRUCTOR) }
                        // if (consProps.size != args.size) error("Wrong number of constructor arguments for ${typeDecl.qualifiedName}")

                        constructor.parameters.mapIndexed { idx, pd -> Pair(pd.name.value, args[idx]) }.toMap()
                    }

                    else -> emptyMap()
                }
                objectGraph.createStructureValueSuspend(expression.possiblyQualifiedTypeName, constructorArgs)
            }

            else -> error("Cannot create an object of type '${typeDef.qualifiedName.value}'")
        }
    }

    /**
     * Execute a property assignment block for self.
     * Separation of construct and setProperties needed for M2m interpreter
     */
    suspend fun propertyAssignmentBlock(evc: EvaluationContext, self: TypedObject, propertyAssignments: List<VariableAssignmentStatement>) {
        propertyAssignments.forEach {
            val value = evaluateExpression(evc, it.rhs)
            self.setPropertySuspend( it.variable.name, value)
        }
    }
}