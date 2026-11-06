/*
 * Copyright (C) 2026 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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
import net.akehurst.language.api.syntaxAnalyser.LocationMap
import net.akehurst.language.base.api.PossiblyQualifiedName
import net.akehurst.language.expressions.api.*
import net.akehurst.language.expressions.asm.RootExpressionDefault
import net.akehurst.language.objectgraph.api.*
import net.akehurst.language.types.api.*
import net.akehurst.language.types.asm.StdLibDefault

/**
 * One interpreter with both a non-suspending and a suspending API.
 *
 * Same approach as ObjectGraphAccessorMutatorByReflection: each part of the evaluation is written once,
 * as a 'private inline fun xxxInternal', and called from a non-suspending and a suspending wrapper.
 * What differs between the two (recursive evaluation, getProperty, setProperty, executeMethod, createStructureValue)
 * is passed in as lambda parameters. Because the internal function is inlined, the lambdas passed by the
 * suspending wrapper may call suspend functions.
 *
 * Inline functions cannot be recursive, so recursion always goes through the 'eval' lambda,
 * which calls the (non-inline) evaluateExpression or evaluateExpressionSuspend.
 */
open class ExpressionsInterpreterOverTypedObjectCombined(
    val objectGraph: ObjectGraphAccessorMutator,
    val customFunctions: FunctionLib? = null
) {

    val issues get() = objectGraph.issues
    val locationMap: LocationMap get() = objectGraph.locationMap
    val typesDomain get() = objectGraph.typesDomain

    fun issueError(item: Any?, message: String, data: Any? = null) {
        val location = item?.let { this.locationMap[item] }
        issues.error(location, message, data)
    }

    fun issueErrorReturnNothing(item: Any?, message: String, data: Any? = null): TypedObject {
        issueError(item, message, data)
        return objectGraph.nothing()
    }

    fun issueWarn(item: Any?, message: String, data: Any? = null) {
        val location = item?.let { this.locationMap[item] }
        issues.warn(location, message, data)
    }

    // --- Non-suspending API ---

    /**
     * if more than one value is to be passed in as an 'evaluation-context'
     * self can contain a 'tuple' of all the necessary named values
     */
    fun evaluateStr(evc: EvaluationContext, expression: String): TypedObject =
        this.evaluateExpression(evc, parseExpression(expression))

    /**
     * if more than one value is to be passed in as an 'evaluation-context'
     * self can contain a 'tuple' of all the necessary named values
     */
    open fun evaluateExpression(evc: EvaluationContext, expression: Expression): TypedObject = when (expression) {
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
        is StatementBlockExpression -> this.evaluateStatementBlockExpression(evc, expression)
        else -> error("Subtype of Expression not handled in evaluateExpression '${expression::class.simpleName}'")
    }

    /**
     * Only construct the object, do not execute the property assignment block.
     * Separation of construct and setProperties needed for M2m interpreter
     */
    fun constructObject(evc: EvaluationContext, expression: CreateObjectExpression): TypedObject =
        constructObjectInternal(evc, expression, { c, x -> evaluateExpression(c, x) }) { tn, args -> objectGraph.createStructureValue(tn, args) }

    /**
     * Execute a property assignment block for self.
     * Separation of construct and setProperties needed for M2m interpreter
     */
    fun propertyAssignmentBlock(evc: EvaluationContext, self: TypedObject, propertyAssignments: List<VariableAssignmentStatement>) =
        propertyAssignmentBlockInternal(evc, self, propertyAssignments, { c, x -> evaluateExpression(c, x) }) { o, n, v -> o.setProperty(n, v) }

    protected open fun evaluateFunctionCall(evc: EvaluationContext, expression: FunctionCall): TypedObject =
        evaluateFunctionCallInternal(evc, expression, { c, x -> evaluateExpression(c, x) }) { f, a, r -> objectGraph.callFunction(f, a, r) }

    private fun evaluateRootExpression(evc: EvaluationContext, expression: RootExpression): TypedObject =
        evaluateRootExpressionInternal(evc, expression) { o, n -> o.getProperty(n) }

    private fun evaluateNavigation(evc: EvaluationContext, expression: NavigationExpression): TypedObject =
        evaluateNavigationInternal(evc, expression, { c, x -> evaluateExpression(c, x) }, { o, n -> o.getProperty(n) }) { o, n, a -> o.executeMethod(n, a) }

    private fun evaluateTernaryCondition(evc: EvaluationContext, expression: TernaryConditionExpression): TypedObject =
        evaluateTernaryConditionInternal(evc, expression) { c, x -> evaluateExpression(c, x) }

    private fun evaluateInfix(evc: EvaluationContext, expression: InfixExpression): TypedObject =
        evaluateInfixInternal(evc, expression) { c, x -> evaluateExpression(c, x) }

    private fun evaluateWith(evc: EvaluationContext, expression: WithExpression): TypedObject =
        evaluateWithInternal(evc, expression) { c, x -> evaluateExpression(c, x) }

    private fun evaluateWhen(evc: EvaluationContext, expression: WhenExpression): TypedObject =
        evaluateWhenInternal(evc, expression) { c, x -> evaluateExpression(c, x) }

    // the body of the lambda is captured by createLambdaValue, so it cannot share an inline body
    private fun evaluateLambda(evc: EvaluationContext, expression: LambdaExpression): TypedObject {
        val lambda = objectGraph.createLambdaValue { it ->
            val newEvc = evc.child(mapOf("it" to it))
            evaluateExpression(newEvc, expression.expression)
        }
        return lambda
    }

    private fun evaluateCast(evc: EvaluationContext, expression: CastExpression): TypedObject =
        evaluateCastInternal(evc, expression) { c, x -> evaluateExpression(c, x) }

    private fun evaluateTypeTest(evc: EvaluationContext, expression: TypeTestExpression): TypedObject =
        evaluateTypeTestInternal(evc, expression) { c, x -> evaluateExpression(c, x) }

    private fun evaluateGroup(evc: EvaluationContext, expression: GroupExpression): TypedObject =
        evaluateExpression(evc, expression.expression)

    private fun evaluateStatementBlockExpression(evc: EvaluationContext, expression: StatementBlockExpression): TypedObject =
        evaluateStatementBlockExpressionInternal(evc, expression, { c, x -> evaluateExpression(c, x) }) { o, n, v -> o.setProperty(n, v) }

    private fun evaluateCreateTuple(evc: EvaluationContext, expression: CreateTupleExpression): TypedObject =
        evaluateCreateTupleInternal(evc, expression) { c, x -> evaluateExpression(c, x) }

    private fun evaluateCreateObject(evc: EvaluationContext, expression: CreateObjectExpression): TypedObject =
        constructObject(evc, expression).also { self ->
            propertyAssignmentBlock(evc, self, expression.propertyAssignments)
        }

    // --- Suspending API ---

    /**
     * if more than one value is to be passed in as an 'evaluation-context'
     * self can contain a 'tuple' of all the necessary named values
     */
    suspend fun evaluateStrSuspend(evc: EvaluationContext, expression: String): TypedObject =
        this.evaluateExpressionSuspend(evc, parseExpression(expression))

    /**
     * if more than one value is to be passed in as an 'evaluation-context'
     * self can contain a 'tuple' of all the necessary named values
     */
    open suspend fun evaluateExpressionSuspend(evc: EvaluationContext, expression: Expression): TypedObject = when (expression) {
        is RootExpression -> this.evaluateRootExpressionSuspend(evc, expression)
        is LiteralExpression -> this.evaluateLiteralExpression(expression)
        is CreateObjectExpression -> this.evaluateCreateObjectSuspend(evc, expression)
        is FunctionCall -> this.evaluateFunctionCallSuspend(evc, expression)
        is CreateTupleExpression -> this.evaluateCreateTupleSuspend(evc, expression)
        is OnExpression -> TODO()
        is NavigationExpression -> this.evaluateNavigationSuspend(evc, expression)
        is LambdaExpression -> this.evaluateLambdaSuspend(evc, expression)
        is WithExpression -> this.evaluateWithSuspend(evc, expression)
        is WhenExpression -> this.evaluateWhenSuspend(evc, expression)
        is TernaryConditionExpression -> this.evaluateTernaryConditionSuspend(evc, expression)
        is InfixExpression -> this.evaluateInfixSuspend(evc, expression)
        is CastExpression -> this.evaluateCastSuspend(evc, expression)
        is TypeTestExpression -> this.evaluateTypeTestSuspend(evc, expression)
        is GroupExpression -> this.evaluateGroupSuspend(evc, expression)
        is StatementBlockExpression -> this.evaluateStatementBlockExpressionSuspend(evc, expression)
        else -> error("Subtype of Expression not handled in evaluateExpressionSuspend '${expression::class.simpleName}'")
    }

    /**
     * Only construct the object, do not execute the property assignment block.
     * Separation of construct and setProperties needed for M2m interpreter
     */
    suspend fun constructObjectSuspend(evc: EvaluationContext, expression: CreateObjectExpression): TypedObject =
        constructObjectInternal(evc, expression, { c, x -> evaluateExpressionSuspend(c, x) }) { tn, args -> objectGraph.createStructureValueSuspend(tn, args) }

    /**
     * Execute a property assignment block for self.
     * Separation of construct and setProperties needed for M2m interpreter
     */
    suspend fun propertyAssignmentBlockSuspend(evc: EvaluationContext, self: TypedObject, propertyAssignments: List<VariableAssignmentStatement>) =
        propertyAssignmentBlockInternal(evc, self, propertyAssignments, { c, x -> evaluateExpressionSuspend(c, x) }) { o, n, v -> o.setPropertySuspend(n, v) }

    protected open suspend fun evaluateFunctionCallSuspend(evc: EvaluationContext, expression: FunctionCall): TypedObject =
        evaluateFunctionCallInternal(evc, expression, { c, x -> evaluateExpressionSuspend(c, x) }) { f, a, r -> objectGraph.callFunctionSuspend(f, a, r) }

    private suspend fun evaluateRootExpressionSuspend(evc: EvaluationContext, expression: RootExpression): TypedObject =
        evaluateRootExpressionInternal(evc, expression) { o, n -> o.getPropertySuspend(n) }

    private suspend fun evaluateNavigationSuspend(evc: EvaluationContext, expression: NavigationExpression): TypedObject =
        evaluateNavigationInternal(evc, expression, { c, x -> evaluateExpressionSuspend(c, x) }, { o, n -> o.getPropertySuspend(n) }) { o, n, a -> o.executeMethodSuspend(n, a) }

    private suspend fun evaluateTernaryConditionSuspend(evc: EvaluationContext, expression: TernaryConditionExpression): TypedObject =
        evaluateTernaryConditionInternal(evc, expression) { c, x -> evaluateExpressionSuspend(c, x) }

    private suspend fun evaluateInfixSuspend(evc: EvaluationContext, expression: InfixExpression): TypedObject =
        evaluateInfixInternal(evc, expression) { c, x -> evaluateExpressionSuspend(c, x) }

    private suspend fun evaluateWithSuspend(evc: EvaluationContext, expression: WithExpression): TypedObject =
        evaluateWithInternal(evc, expression) { c, x -> evaluateExpressionSuspend(c, x) }

    private suspend fun evaluateWhenSuspend(evc: EvaluationContext, expression: WhenExpression): TypedObject =
        evaluateWhenInternal(evc, expression) { c, x -> evaluateExpressionSuspend(c, x) }

    // the body of the lambda is captured by createLambdaValueSuspend, so it cannot share an inline body
    private suspend fun evaluateLambdaSuspend(evc: EvaluationContext, expression: LambdaExpression): TypedObject {
        val lambda = objectGraph.createLambdaValueSuspend { it ->
            val newEvc = evc.child(mapOf("it" to it))
            evaluateExpressionSuspend(newEvc, expression.expression)
        }
        return lambda
    }

    private suspend fun evaluateCastSuspend(evc: EvaluationContext, expression: CastExpression): TypedObject =
        evaluateCastInternal(evc, expression) { c, x -> evaluateExpressionSuspend(c, x) }

    private suspend fun evaluateTypeTestSuspend(evc: EvaluationContext, expression: TypeTestExpression): TypedObject =
        evaluateTypeTestInternal(evc, expression) { c, x -> evaluateExpressionSuspend(c, x) }

    private suspend fun evaluateGroupSuspend(evc: EvaluationContext, expression: GroupExpression): TypedObject =
        evaluateExpressionSuspend(evc, expression.expression)

    private suspend fun evaluateStatementBlockExpressionSuspend(evc: EvaluationContext, expression: StatementBlockExpression): TypedObject =
        evaluateStatementBlockExpressionInternal(evc, expression, { c, x -> evaluateExpressionSuspend(c, x) }) { o, n, v -> o.setPropertySuspend(n, v) }

    private suspend fun evaluateCreateTupleSuspend(evc: EvaluationContext, expression: CreateTupleExpression): TypedObject =
        evaluateCreateTupleInternal(evc, expression) { c, x -> evaluateExpressionSuspend(c, x) }

    private suspend fun evaluateCreateObjectSuspend(evc: EvaluationContext, expression: CreateObjectExpression): TypedObject =
        constructObjectSuspend(evc, expression).also { self ->
            propertyAssignmentBlockSuspend(evc, self, expression.propertyAssignments)
        }

    // --- Shared, not dependent on suspend ---

    private fun parseExpression(expression: String): Expression {
        val result = Agl.registry.agl.expressions.processor!!.process(expression)
        check(result.allIssues.errors.isEmpty()) { result.allIssues.toString() }
        return result.asm!!
    }

    private fun evaluateLiteralExpression(expression: LiteralExpression): TypedObject =
        objectGraph.createPrimitiveValue(expression.qualifiedTypeName, expression.value)

    fun evaluateTypeReference(typeReference: TypeReference): TypeInstance {
        //TODO: issues rather than exceptions!
        val decl = typesDomain.findFirstDefinitionByPossiblyQualifiedNameOrNull(typeReference.possiblyQualifiedName) ?: error("Type not found ${typeReference.possiblyQualifiedName}")
        val targs = typeReference.typeArguments.map { evaluateTypeReference(it).asTypeArgument }
        return decl.type(targs, typeReference.isNullable)
    }

    private fun castListElement(obj: TypedObject, elem: TypedObject): TypedObject {
        val listElementType = obj.type.typeArguments.getOrNull(0)?.type
            ?: StdLibDefault.AnyType
        return when {
            objectGraph.nothing() == elem -> objectGraph.nothing()
            else -> {
                val elemType = typesDomain.findByQualifiedNameOrNull(elem.type.qualifiedTypeName)?.type(elem.type.typeArguments)
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

    private fun castMapElement(elem: TypedObject, valueType: TypeInstance): TypedObject {
        return when {
            objectGraph.nothing() == elem -> objectGraph.nothing()
            else -> {
                val elemType = typesDomain.findByQualifiedNameOrNull(elem.type.qualifiedTypeName)?.type(elem.type.typeArguments)
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
                val lhsv = objectGraph.untyped(lhs) as Long
                val rhsv = objectGraph.untyped(rhs) as Long
                objectGraph.createPrimitiveValue(StdLibDefault.Integer.qualifiedTypeName, lhsv * rhsv)
            }

            lhs.type.conformsTo(StdLibDefault.Real) && rhs.type.conformsTo(StdLibDefault.Real) -> {
                val lhsv = objectGraph.untyped(lhs) as Double
                val rhsv = objectGraph.untyped(rhs) as Double
                objectGraph.createPrimitiveValue(StdLibDefault.Real.qualifiedTypeName, lhsv * rhsv)
            }

            else -> {
                issues.error(null, "'$op' not supported for types '${lhs.type.qualifiedTypeName} and ${rhs.type.qualifiedTypeName}'")
                objectGraph.nothing()
            }
        }

        "%" -> when {
            lhs.type.conformsTo(StdLibDefault.Integer) && rhs.type.conformsTo(StdLibDefault.Integer) -> {
                val lhsv = objectGraph.untyped(lhs) as Long
                val rhsv = objectGraph.untyped(rhs) as Long
                objectGraph.createPrimitiveValue(StdLibDefault.Integer.qualifiedTypeName, lhsv % rhsv)
            }

            lhs.type.conformsTo(StdLibDefault.Real) && rhs.type.conformsTo(StdLibDefault.Real) -> {
                val lhsv = objectGraph.untyped(lhs) as Double
                val rhsv = objectGraph.untyped(rhs) as Double
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
            lhs.type.conformsTo(StdLibDefault.String) -> when {
                rhs.type.conformsTo(StdLibDefault.String) -> {
                    val lhsv = objectGraph.untyped(lhs) as String
                    val rhsv = objectGraph.untyped(rhs) as String
                    objectGraph.createPrimitiveValue(StdLibDefault.String.qualifiedTypeName, lhsv + rhsv)
                }

                else -> {
                    val lhsv = objectGraph.untyped(lhs) as String
                    val rhsv = objectGraph.untyped(rhs).toString()
                    objectGraph.createPrimitiveValue(StdLibDefault.String.qualifiedTypeName, lhsv + rhsv)
                }
            }

            lhs.type.conformsTo(StdLibDefault.Integer) && rhs.type.conformsTo(StdLibDefault.Integer) -> {
                val lhsv = objectGraph.untyped(lhs) as Long
                val rhsv = objectGraph.untyped(rhs) as Long
                objectGraph.createPrimitiveValue(StdLibDefault.Integer.qualifiedTypeName, lhsv + rhsv)
            }

            lhs.type.conformsTo(StdLibDefault.Real) && rhs.type.conformsTo(StdLibDefault.Real) -> {
                val lhsv = objectGraph.untyped(lhs) as Double
                val rhsv = objectGraph.untyped(rhs) as Double
                objectGraph.createPrimitiveValue(StdLibDefault.Real.qualifiedTypeName, lhsv + rhsv)
            }

            else -> {
                issues.error(null, "'$op' not supported for types '${lhs.type.qualifiedTypeName} and ${rhs.type.qualifiedTypeName}'")
                objectGraph.nothing()
            }
        }

        "-" -> when {
            lhs.type.conformsTo(StdLibDefault.Integer) && rhs.type.conformsTo(StdLibDefault.Integer) -> {
                val lhsv = objectGraph.untyped(lhs) as Long
                val rhsv = objectGraph.untyped(rhs) as Long
                objectGraph.createPrimitiveValue(StdLibDefault.Integer.qualifiedTypeName, lhsv - rhsv)
            }

            lhs.type.conformsTo(StdLibDefault.Real) && rhs.type.conformsTo(StdLibDefault.Real) -> {
                val lhsv = objectGraph.untyped(lhs) as Double
                val rhsv = objectGraph.untyped(rhs) as Double
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

    // --- Internal, shared by both APIs ---

    private inline fun evaluateRootExpressionInternal(
        evc: EvaluationContext,
        expression: RootExpression,
        getProperty: (obj: TypedObject, name: String) -> TypedObject
    ): TypedObject {
        return when {
            expression.isNothing -> objectGraph.nothing()
            expression.isSelf -> {
                //_issues.error(null, "evaluation of 'self' only works if self is a String, got an object of type '${self::class.simpleName}'")
                evc.self
                    ?: error("No '\$self' value defined in Evaluation Context: $evc")
            }

            expression.name.startsWith("\$") -> {
                // special value: the name must exist as a property of the self which must be a tuple
                val name = expression.name
                evc.getOrInParent(name)
                    ?: evc.self?.let { evaluatePropertyNameInternal(it, PropertyName(name), getProperty) }
                    ?: error($$"Evaluation Context does not contain special value '$$name' and there is no '$self' object with that property name")
            }

            else -> evc.getOrInParent(expression.name)
                ?: evc.self?.let { evaluatePropertyNameInternal(it, PropertyName(expression.name), getProperty) }
                ?: let {
                    issues.warn(null, $$"Evaluation Context does not contain root expression '$${expression.name}' and there is no '$self' object with that property name, using value '$nothing'")
                    objectGraph.nothing()
                }
        }
    }

    private inline fun evaluateFunctionCallInternal(
        evc: EvaluationContext,
        expression: FunctionCall,
        eval: (evc: EvaluationContext, expression: Expression) -> TypedObject,
        callFunction: (function: FunctionDefinitionFloating, args: List<TypedObject>, typeReferenceResolver: (TypeReference) -> TypeInstance) -> TypedObject
    ): TypedObject {
        val argValues = expression.arguments.map {
            eval(evc, it)
        }
        val funcName = expression.possiblyQualifiedName.value
        val funcDefinition = customFunctions?.findFirstFunctionNamed(funcName)
            ?: objectGraph.functionLib.findFirstFunctionNamed(funcName)
        val result = funcDefinition?.let {
            callFunction(funcDefinition, argValues) { tr -> evaluateTypeReference(tr) }
        } ?: run {
            issueErrorReturnNothing(null, "No function named '${funcName}' was declared, using value \$nothing.")
            objectGraph.nothing()
        }
        return result
    }

    private inline fun evaluateNavigationInternal(
        evc: EvaluationContext,
        expression: NavigationExpression,
        eval: (evc: EvaluationContext, expression: Expression) -> TypedObject,
        getProperty: (obj: TypedObject, name: String) -> TypedObject,
        executeMethod: (obj: TypedObject, name: String, args: List<TypedObject>) -> TypedObject
    ): TypedObject {
        // start should be a RootExpression or LiteralExpression
        val st = expression.start
        val start = eval(evc, st)
        val result = expression.parts.fold(start) { acc, it ->
            when (it) {
                is PropertyCall -> evaluatePropertyNameInternal(acc, PropertyName(it.propertyName), getProperty)
                is MethodCall -> evaluateMethodCallInternal(evc, acc, MethodName(it.methodName), it.arguments, eval, executeMethod)
                is IndexOperation -> evaluateIndexOperationInternal(evc, acc, it.indices, eval)
                else -> error("should not happen")
            }
        }
        return result
    }

    private inline fun evaluatePropertyNameInternal(
        obj: TypedObject,
        propertyName: PropertyName,
        getProperty: (obj: TypedObject, name: String) -> TypedObject
    ): TypedObject {
        return when {
            objectGraph.isNothing(obj) -> {
                issues.warn(null, $$"Cannot get property '$${propertyName.value}' on $nothing, returning $nothing.")
                objectGraph.nothing()
            }

            else -> {
                getProperty(obj, propertyName.value)
            }
        }
    }

    private inline fun evaluateMethodCallInternal(
        evc: EvaluationContext,
        obj: TypedObject,
        methodName: MethodName,
        args: List<Expression>,
        eval: (evc: EvaluationContext, expression: Expression) -> TypedObject,
        executeMethod: (obj: TypedObject, name: String, args: List<TypedObject>) -> TypedObject
    ): TypedObject {
        return when {
            objectGraph.isNothing(obj) -> {
                issues.warn(null, $$"Cannot call method '$${methodName.value}' on on $nothing, returning $nothing.")
                objectGraph.nothing()
            }

            else -> {
                val argValues = args.map {
                    eval(evc, it)
                }
                executeMethod(obj, methodName.value, argValues)
            }
        }
    }

    private inline fun evaluateIndexOperationInternal(
        evc: EvaluationContext,
        obj: TypedObject,
        indices: List<Expression>,
        eval: (evc: EvaluationContext, expression: Expression) -> TypedObject
    ): TypedObject {
        return when {
            obj.type.resolvedDefinition.conformsTo(StdLibDefault.List) -> when (indices.size) {
                1 -> {
                    val idx = eval(evc, indices[0])
                    evaluateIndexOperationOnList(obj, idx)
                }

                else -> {
                    issues.error(null, "Only one index value should be used for Lists")
                    objectGraph.nothing()
                }
            }

            obj.type.resolvedDefinition.conformsTo(StdLibDefault.Map) -> when (indices.size) {
                1 -> {
                    val idx = eval(evc, indices[0])
                    evaluateIndexOperationOnMap(obj, idx)
                }

                else -> {
                    issues.error(null, "Only one index value should be used for Maps")
                    objectGraph.nothing()
                }
            }

            else -> {
                issues.error(null, "Index operation on non List value is not possible: '${obj.asString()}'")
                objectGraph.nothing()
            }
        }
    }

    private fun evaluateIndexOperationOnList(obj: TypedObject, idx: TypedObject): TypedObject {
        return when {
            idx.type.conformsTo(StdLibDefault.Integer) -> {
                val i = objectGraph.valueOf(idx) as Long
                val elem = objectGraph.getFromListWithIndex(obj, i.toInt())
                castListElement(obj, elem)
            }

            else -> {
                issues.error(null, "Index value must evaluate to an Integer for Lists")
                objectGraph.nothing()
            }
        }
    }

    private fun evaluateIndexOperationOnMap(obj: TypedObject, idx: TypedObject): TypedObject {
        val keyType = obj.type.typeArguments.getOrNull(0)?.type
        val valueType = obj.type.typeArguments.getOrNull(1)?.type
        return when {
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
                castMapElement(elem, valueType)
            }

            else -> {
                issues.error(null, "Index value must evaluate to an Integer for Lists")
                objectGraph.nothing()
            }
        }
    }

    private inline fun evaluateTernaryConditionInternal(
        evc: EvaluationContext,
        expression: TernaryConditionExpression,
        eval: (evc: EvaluationContext, expression: Expression) -> TypedObject
    ): TypedObject {
        val condValue = eval(evc, expression.condition)
        return when (condValue.type) {
            StdLibDefault.Boolean -> {
                if (objectGraph.valueOf(condValue) as Boolean) {
                    eval(evc, expression.trueExpression)
                } else {
                    eval(evc, expression.falseExpression)
                }
            }

            else -> issueErrorReturnNothing(
                expression.condition,
                "Conditions/Options in a ternary condition expression must result in a Boolean value: '${expression.condition}' is of type '${condValue.type}' = ${condValue.self}"
            )
        }
    }

    private inline fun evaluateInfixInternal(
        evc: EvaluationContext,
        expression: InfixExpression,
        eval: (evc: EvaluationContext, expression: Expression) -> TypedObject
    ): TypedObject {
        //TODO: Operator precedence
        var result = eval(evc, expression.expressions.first())
        for (i in expression.operators.indices) {
            val op = expression.operators[i]
            val rhsExpr = expression.expressions[i + 1]
            val rhs = eval(evc, rhsExpr)
            result = evaluateInfixOperator(result, op, rhs)
        }
        return result
    }

    private inline fun evaluateWithInternal(
        evc: EvaluationContext,
        expression: WithExpression,
        eval: (evc: EvaluationContext, expression: Expression) -> TypedObject
    ): TypedObject {
        val newSelf = eval(evc, expression.withContext)
        return when {
            objectGraph.nothing() == newSelf -> newSelf
            else -> {
                val newEvc = evc.child(mapOf(RootExpressionDefault.SELF.name to newSelf))
                val result = eval(newEvc, expression.expression)
                result
            }
        }
    }

    private inline fun evaluateWhenInternal(
        evc: EvaluationContext,
        expression: WhenExpression,
        eval: (evc: EvaluationContext, expression: Expression) -> TypedObject
    ): TypedObject {
        for (opt in expression.options) {
            val condValue = eval(evc, opt.condition)
            when (condValue.type) {
                StdLibDefault.Boolean -> {
                    if (objectGraph.valueOf(condValue) as Boolean) {
                        val result = eval(evc, opt.expression)
                        return result // return after first condition found that is true
                    } else {
                        //condition not true
                    }
                }

                else -> error("Conditions/Options in a when expression must result in a Boolean value: '${opt.condition}' is of type '${condValue.type}' = ${condValue.self}")
            }
        }
        return eval(evc, expression.elseOption.expression)
    }

    private inline fun evaluateCastInternal(
        evc: EvaluationContext,
        expression: CastExpression,
        eval: (evc: EvaluationContext, expression: Expression) -> TypedObject
    ): TypedObject {
        //TODO: do we need a type check? or can we assume it is already done in semantic analysis!
        val exprResult = eval(evc, expression.expression)
        val tgtType = evaluateTypeReference(expression.targetType)
        return objectGraph.cast(exprResult, tgtType)
    }

    private inline fun evaluateTypeTestInternal(
        evc: EvaluationContext,
        expression: TypeTestExpression,
        eval: (evc: EvaluationContext, expression: Expression) -> TypedObject
    ): TypedObject {
        //TODO: do we need a type check? or can we assume it is already done in semantic analysis!
        val exprResult = eval(evc, expression.expression)
        val tgtType = evaluateTypeReference(expression.targetType)
        val res = exprResult.type.conformsTo(tgtType)
        return objectGraph.createPrimitiveValue(StdLibDefault.Boolean.qualifiedTypeName, res)
    }

    private inline fun evaluateStatementBlockExpressionInternal(
        evc: EvaluationContext,
        expression: StatementBlockExpression,
        eval: (evc: EvaluationContext, expression: Expression) -> TypedObject,
        setProperty: (obj: TypedObject, name: String, value: TypedObject) -> Unit
    ): TypedObject {
        val newEvc = evc.child(emptyMap())
        val self = evc.self
        val selfTypeDef = self?.type?.resolvedDefinitionOrNull
        for (ass in expression.assignment) {
            val rhsValue = eval(newEvc, ass.rhs)
            val selfProp = selfTypeDef?.findAllPropertyOrNull(PropertyName(ass.variable.name))
            when {
                // create new variable with this name
                selfProp == null -> newEvc.setNamedValue(ass.variable.name, rhsValue)
                // set the property
                else -> setProperty(self, ass.variable.name, rhsValue)
            }
        }
        val result = eval(newEvc, expression.expression)
        return result
    }

    // mutation
    private inline fun evaluateCreateTupleInternal(
        evc: EvaluationContext,
        expression: CreateTupleExpression,
        eval: (evc: EvaluationContext, expression: Expression) -> TypedObject
    ): TypedObject {
        val props = expression.propertyAssignments.associate {
            val value = eval(evc, it.rhs)
            Pair(it.variable.name, value)
        }
        val tuple = objectGraph.createTupleValue(props)
        return tuple
    }

    private inline fun constructObjectInternal(
        evc: EvaluationContext,
        expression: CreateObjectExpression,
        eval: (evc: EvaluationContext, expression: Expression) -> TypedObject,
        createStructureValue: (possiblyQualifiedTypeName: PossiblyQualifiedName, constructorArgs: Map<String, TypedObject>) -> TypedObject
    ): TypedObject {
        val typeDef = typesDomain.findFirstDefinitionByPossiblyQualifiedNameOrNull(expression.possiblyQualifiedTypeName)
        return when (typeDef) {
            null -> error("Type not found ${expression.possiblyQualifiedTypeName}")
            is DataType, is ValueType -> {
                val args = expression.constructorArguments.map { eval(evc, it.rhs) }
                val constructorArgs = constructorArgumentsFor(typeDef, expression.constructorArguments.map { it.variable.name }, args)
                createStructureValue(expression.possiblyQualifiedTypeName, constructorArgs)
            }

            else -> error("Cannot create an object of type '${typeDef.qualifiedName.value}'")
        }
    }

    /**
     * Chooses the declared constructor of typeDef for the given constructor arguments,
     * first by parameter name, otherwise by position.
     * Returns the arguments keyed by parameter name.
     * With no arguments: a declared 0 parameter constructor is required, unless the type declares no constructors.
     */
    private fun constructorArgumentsFor(typeDef: TypeDefinition, argNames: List<String>, args: List<TypedObject>): Map<String, TypedObject> {
        val constructors = when (typeDef) {
            is DataType -> typeDef.constructors
            is ValueType -> typeDef.constructors
            else -> emptyList()
        }
        val declared = { constructors.joinToString { c -> "(${c.parameters.joinToString { p -> "${p.name.value}: ${p.typeInstance.signature(null, 0)}" }})" } }
        return when {
            args.isEmpty() -> when {
                constructors.isEmpty() || constructors.any { it.parameters.isEmpty() } -> emptyMap()
                else -> error("Type '${typeDef.qualifiedName.value}' has no declared constructor with 0 parameters, declared constructors are: ${declared()}")
            }

            else -> {
                val argsByName = argNames.zip(args).toMap()
                val byName = constructors.firstOrNull { cons ->
                    cons.parameters.size == args.size && cons.parameters.all { p ->
                        argsByName[p.name.value]?.let { a -> constructorArgumentConforms(a.type, p.typeInstance) } ?: false
                    }
                }
                when {
                    null != byName -> byName.parameters.associate { p -> Pair(p.name.value, argsByName[p.name.value]!!) }
                    else -> {
                        val byPosition = constructors.firstOrNull { cons ->
                            cons.parameters.size == args.size && cons.parameters.zip(args).all { (p, a) -> constructorArgumentConforms(a.type, p.typeInstance) }
                        } ?: error(
                            "Constructor not found for '${typeDef.qualifiedName.value}' with arguments (${argNames.zip(args).joinToString { (n, a) -> "$n: ${a.type.signature(null, 0)}" }}), declared constructors are: ${declared()}"
                        )
                        byPosition.parameters.mapIndexed { idx, pd -> Pair(pd.name.value, args[idx]) }.toMap()
                    }
                }
            }
        }
    }

    /**
     * conformsTo is covariant in the type arguments,
     * in addition, if either type has no type arguments (e.g. parameter type 'List'), only the type definitions are compared
     */
    private fun constructorArgumentConforms(argType: TypeInstance, paramType: TypeInstance): Boolean = when {
        argType.conformsTo(paramType) -> true
        argType.typeArguments.isEmpty() || paramType.typeArguments.isEmpty() -> argType.resolvedDefinition.conformsTo(paramType.resolvedDefinition)
        else -> false
    }

    private inline fun propertyAssignmentBlockInternal(
        evc: EvaluationContext,
        self: TypedObject,
        propertyAssignments: List<VariableAssignmentStatement>,
        eval: (evc: EvaluationContext, expression: Expression) -> TypedObject,
        setProperty: (obj: TypedObject, name: String, value: TypedObject) -> Unit
    ) {
        propertyAssignments.forEach {
            val value = eval(evc, it.rhs)
            setProperty(self, it.variable.name, value)
        }
    }
}
