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

package net.akehurst.language.agl.simple

import net.akehurst.language.asm.simple.isStdString
import net.akehurst.language.expressions.processor.EvaluationContext
import net.akehurst.language.expressions.processor.ExpressionsInterpreterOverTypedObject
import net.akehurst.language.expressions.processor.asmValue
import net.akehurst.language.expressions.processor.toTypedObject
import net.akehurst.language.reference.asm.CrossReferenceModelDefault
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.asm.api.*
import net.akehurst.language.expressions.api.Expression
import net.akehurst.language.expressions.api.NavigationExpression
import net.akehurst.language.expressions.api.RootExpression
import net.akehurst.language.scope.api.Scope
import net.akehurst.language.collections.mutableStackOf
import net.akehurst.language.sentence.api.InputLocation
import net.akehurst.language.typemodel.api.TypeModel
import net.akehurst.language.typemodel.asm.SimpleTypeModelStdLib

class ScopeCreator(
    val typeModel: TypeModel,
    val crossReferenceModel: CrossReferenceModelDefault,
    val rootScope: Scope<AsmPath>,
    val locationMap: Map<Any, InputLocation>,
    val issues: IssueHolder
) : AsmTreeWalker {

    private val _interpreter = ExpressionsInterpreterOverTypedObject(typeModel)

    val currentScope = mutableStackOf(rootScope)

    override fun beforeRoot(root: AsmValue) {
//        when (root) {
//            is AsmStructure -> addToScope(currentScope.peek(), root)
//            else -> Unit
//        }
    }

    override fun afterRoot(root: AsmValue) {

    }

    override fun onNothing(owningProperty: AsmStructureProperty?, value: AsmNothing) {}

    override fun onPrimitive(owningProperty: AsmStructureProperty?, value: AsmPrimitive) {}

    override fun beforeStructure(owningProperty: AsmStructureProperty?, value: AsmStructure) {
        val scope = currentScope.peek()

        addToScope(scope, value)
        val chScope = createScope(scope, value)
        currentScope.push(chScope)
    }

    override fun onProperty(owner: AsmStructure, property: AsmStructureProperty) {}

    override fun afterStructure(owningProperty: AsmStructureProperty?, value: AsmStructure) {
        currentScope.pop()
    }

    override fun beforeList(owningProperty: AsmStructureProperty?, value: AsmList) {}

    override fun afterList(owningProperty: AsmStructureProperty?, value: AsmList) {}

    private fun createScope(parentScope: Scope<AsmPath>, el: AsmStructure): Scope<AsmPath> {
        val exp = crossReferenceModel.identifyingExpressionFor(parentScope.forTypeName.last, el.qualifiedTypeName)
        return if (null != exp && crossReferenceModel.isScopeDefinedFor(el.qualifiedTypeName)) {
            val refInParent = exp.createReferenceLocalToScope(parentScope, el)
            when {
                // Nothing
                refInParent is AsmNothing -> parentScope.createOrGetChildScope(el.typeName.value, el.qualifiedTypeName, el.path)
                // String
                refInParent is AsmPrimitive && refInParent.isStdString -> parentScope.createOrGetChildScope((refInParent.value as String), el.qualifiedTypeName, el.path)
                // List<String>
                refInParent is AsmList && refInParent.isNotEmpty && refInParent.elements.all { it is AsmPrimitive && it.isStdString } -> {
                    val scopeDefined = crossReferenceModel.isScopeDefinedFor(el.qualifiedTypeName)
                    val idExprDefinedInScope = crossReferenceModel.identifyingExpressionFor(el.qualifiedTypeName.last, el.qualifiedTypeName)
                    when {
                        // and scope defined with the same expression
                        scopeDefined && exp == idExprDefinedInScope -> {
                            // child scopes should have been created in addToScope
                            // return the deepest child created
                            val refList = refInParent.elements.map { (it as AsmPrimitive).value as String }
                            var childScope = parentScope
                            for (ref in refList) {
                                childScope = childScope.getChildScopeOrNull(ref) ?: error("Internal error: should never be null as already created these scopes in addToScope")
                            }
                            childScope
                        }

                        else -> {
                            // issue already reported in addToScope
                            parentScope
                        }
                    }
                }

                else -> {
                    issues.error(this.locationMap[el], "Cannot create a child scope in '$parentScope' for '$el' because its identifying expression evaluates to $refInParent")
                    parentScope
                }
            }
        } else {
            parentScope
        }
    }

    private fun addToScope(scope: Scope<AsmPath>, el: AsmStructure) {
        val exp = crossReferenceModel.identifyingExpressionFor(scope.forTypeName.last, el.qualifiedTypeName)
        if (null != exp) {
            //val reference = _scopeModel!!.createReferenceFromRoot(scope, el)
            val scopeLocalReference = exp.createReferenceLocalToScope(scope, el)
            when {
                scopeLocalReference is AsmNothing -> {
//                issues.warn(
//                    this.locationMap[el],
//                    "Cannot create a local reference in '$scope' for '$el' because its identifying expression evaluates to Nothing. Using type name as identifier."
//                )
                    val contextRef = el.path
                    val added = scope.addToScope(el.qualifiedTypeName.value, el.qualifiedTypeName, contextRef)
                    when (added) {
                        true -> Unit
                        else -> issues.error(this.locationMap[el], "(${el.typeName},${el.qualifiedTypeName}) already exists in scope $scope")
                    }
                }

                scopeLocalReference is AsmPrimitive && scopeLocalReference.isStdString -> {
                    val contextRef = el.path
                    val ref = (scopeLocalReference.value) as String
                    val added = scope.addToScope(ref, el.qualifiedTypeName, contextRef)
                    when (added) {
                        true -> Unit
                        else -> issues.error(this.locationMap[el], "($ref,${el.qualifiedTypeName}) already exists in scope $scope")
                    }
                }

                // List<String>
                scopeLocalReference is AsmList && scopeLocalReference.isNotEmpty && scopeLocalReference.elements.all { it is AsmPrimitive && it.isStdString } -> {
                    val scopeDefined = crossReferenceModel.isScopeDefinedFor(el.qualifiedTypeName)
                    val idExprDefinedInScope = crossReferenceModel.identifyingExpressionFor(el.typeName, el.qualifiedTypeName)
                    when {
                        scopeDefined.not() -> {
                            issues.error(
                                this.locationMap[el],
                                "Cannot create a local reference in '$scope' for '$el' because there is no scope defined for ${el.qualifiedTypeName} although its identifying expression evaluates to a List<String>"
                            )
                        }

                        scopeDefined && null == idExprDefinedInScope -> {
                            issues.error(
                                this.locationMap[el],
                                "Cannot create a local reference in '$scope' for '$el' because it has no identifying expression in the scope (which should evaluate to a List<String>)"
                            )
                        }

                        scopeDefined && exp != idExprDefinedInScope -> {
                            issues.error(
                                this.locationMap[el],
                                "Cannot create a local reference in '$scope' for '$el' because the identifying expression is different in the scope and the parent scope"
                            )
                        }
                        //and scope defined
                        else -> {
                            val refList = scopeLocalReference.elements.map { (it as AsmPrimitive).value as String }
                            val contextRef = el.path
                            var nextScope = scope
                            for (ref in refList) {
                                val added = nextScope.addToScope(ref, el.qualifiedTypeName, contextRef)
                                when (added) {
                                    true -> Unit
                                    else -> issues.error(this.locationMap[el], "($ref,${el.qualifiedTypeName}) already exists in scope $scope")
                                }

                                nextScope = nextScope.createOrGetChildScope(ref, el.qualifiedTypeName, contextRef)
                            }
                        }
                    }
                }

                else -> {
                    issues.error(
                        this.locationMap[el],
                        "Cannot create a local reference in '$scope' for '$el' because its identifying expression evaluates to a ${scopeLocalReference.typeName}"
                    )
                }
            }

        } else {
            // no need to add it to scope
        }
    }

    private fun Expression.createReferenceLocalToScope(scope: Scope<AsmPath>, element: AsmStructure): AsmValue = when (this) {
        is RootExpression -> this.createReferenceLocalToScope(scope, element)
        is NavigationExpression -> this.createReferenceLocalToScope(scope, element)
        else -> error("Subtype of Expression not handled in 'createReferenceLocalToScope'")
    }

    private fun RootExpression.createReferenceLocalToScope(scope: Scope<AsmPath>, element: AsmStructure): AsmValue {
        val elType = typeModel.findByQualifiedNameOrNull(element.qualifiedTypeName)?.type() ?: SimpleTypeModelStdLib.AnyType
        return _interpreter.evaluateExpression(EvaluationContext.ofSelf(element.toTypedObject(elType)), this).asmValue
    }


    private fun NavigationExpression.createReferenceLocalToScope(scope: Scope<AsmPath>, element: AsmStructure): AsmValue {
        val elType = typeModel.findByQualifiedNameOrNull(element.qualifiedTypeName)?.type() ?: SimpleTypeModelStdLib.AnyType
        return _interpreter.evaluateExpression(EvaluationContext.ofSelf(element.toTypedObject(elType)), this).asmValue
    }


}