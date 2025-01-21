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

import net.akehurst.language.asm.api.*
import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.collections.mutableStackOf
import net.akehurst.language.issues.api.LanguageIssueKind
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.reference.api.CrossReferenceModel
import net.akehurst.language.scope.api.Scope
import net.akehurst.language.sentence.api.InputLocation
import net.akehurst.language.typemodel.api.TypeModel

class ScopeCreator<ItemInScopeType>(
    val typeModel: TypeModel,
    val crossReferenceModel: CrossReferenceModel,
    val rootScope: Scope<ItemInScopeType>,
    var replaceIfItemAlreadyExistsInScope: Boolean,
    var ifItemAlreadyExistsInScopeIssueKind: LanguageIssueKind?,
    val identifyingValueInFor: (inTypeName:SimpleName, item:AsmStructure) -> Any?,
    val createItemInScopeFunction: ((referableName: String, item: AsmStructure) -> ItemInScopeType),
    val locationMap: Map<Any, InputLocation>,
    val issues: IssueHolder
) : AsmTreeWalker {


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

    private fun createScope(parentScope: Scope<ItemInScopeType>, el: AsmStructure): Scope<ItemInScopeType> {
        val inTypeName = parentScope.forTypeName.last
        val refInParent = identifyingValueInFor.invoke(inTypeName, el)
        return when {
                // Nothing
                null==refInParent  -> {
                    TODO("Is this used ?")
                    val ref = el.typeName.value  // use type name as ref
                    val scopeItem = createItemInScopeFunction.invoke(ref, el)
                    parentScope.createOrGetChildScope(ref, el.qualifiedTypeName, scopeItem)
                }
                // String
                refInParent is String -> {
                    val ref = refInParent
                    val scopeItem = createItemInScopeFunction.invoke(ref, el)
                    parentScope.createOrGetChildScope(ref, el.qualifiedTypeName, scopeItem)
                }
                // List<String>
                refInParent is List<*> && refInParent.isNotEmpty() && refInParent.all { it is String} -> {
                    TODO("Think this needs fixing!")
                    val exp = crossReferenceModel.identifyingExpressionFor(inTypeName, el.qualifiedTypeName)
                    val scopeDefined = crossReferenceModel.isScopeDefinedFor(el.qualifiedTypeName)
                    val idExprDefinedInScope = crossReferenceModel.identifyingExpressionFor(el.qualifiedTypeName.last, el.qualifiedTypeName)
                    when {
                        // and scope defined with the same expression
                        scopeDefined && exp == idExprDefinedInScope -> {
                            // child scopes should have been created in addToScope
                            // return the deepest child created
                            val refList = refInParent.map { it as String }
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
    }

    private fun addToScope(scope: Scope<ItemInScopeType>, el: AsmStructure) {
        val inTypeName = scope.forTypeName.last
        val scopeLocalReference = identifyingValueInFor.invoke(inTypeName, el)
            when {
                null==scopeLocalReference -> {
                    TODO("Is this used ?")
                    val ref =el.qualifiedTypeName.value
                    addToScopeAs(scope, el, ref)
                }

                scopeLocalReference is String -> {
                    val ref = scopeLocalReference
                    addToScopeAs(scope, el, ref)
                }

                // List<String>
                scopeLocalReference is List<*> && scopeLocalReference.isNotEmpty() && scopeLocalReference.all { it is String } -> {
                    TODO("Think this needs fixing!")
                    val exp = crossReferenceModel.identifyingExpressionFor(inTypeName, el.qualifiedTypeName)
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
                            val refList = scopeLocalReference.map { it as String }
                            var nextScope = scope
                            for (ref in refList) {
                                addToScopeAs(nextScope, el, ref)
                                val itemInScope = createItemInScopeFunction.invoke(ref, el)
                                nextScope = nextScope.createOrGetChildScope(ref, el.qualifiedTypeName, itemInScope)
                            }
                        }
                    }
                }

                else -> {
                    issues.error(
                        this.locationMap[el],
                        "Cannot create a local reference in '$scope' for '$el' because its identifying expression evaluates to a ${scopeLocalReference}"
                    )
                }
            }
    }

    private fun addToScopeAs(scope: Scope<ItemInScopeType>, el: AsmStructure, referableName: String) {
        val scopeItem = createItemInScopeFunction.invoke(referableName, el)
        val itAlreadyExists = scope.contains(referableName, el.qualifiedTypeName) { itemTypeName, requiredTypeName ->
            val itemType = typeModel.findByQualifiedNameOrNull(itemTypeName) ?: error("Type not found '${itemTypeName.value}'")
            val requireType = typeModel.findByQualifiedNameOrNull(requiredTypeName) ?: error("Type not found '${requiredTypeName.value}'")
            itemType.conformsTo(requireType)
        }
        val added = scope.addToScope(referableName, el.qualifiedTypeName, scopeItem, replaceIfItemAlreadyExistsInScope)
        //when (added) {
        //    false -> Unit
        //    true -> createReferableFunction.invoke(referableName, el)
        //}
        when (itAlreadyExists) {
            false -> Unit
            true -> this.ifItemAlreadyExistsInScopeIssueKind?.let {
                issues.raise(
                    it, LanguageProcessorPhase.SEMANTIC_ANALYSIS,
                    this.locationMap[el], "(${el.typeName},${el.qualifiedTypeName}) already exists in scope $scope"
                )
            }
        }

    }

/*
    private fun Expression.createReferenceLocalToScope(scope: Scope<ItemInScopeType>, element: AsmStructure): AsmValue = when (this) {
        is RootExpression -> this.createReferenceLocalToScope(scope, element)
        is NavigationExpression -> this.createReferenceLocalToScope(scope, element)
        else -> error("Subtype of Expression not handled in 'createReferenceLocalToScope'")
    }

    private fun RootExpression.createReferenceLocalToScope(scope: Scope<ItemInScopeType>, element: AsmStructure): AsmValue {
        val elType = typeModel.findByQualifiedNameOrNull(element.qualifiedTypeName)?.type() ?: StdLibDefault.AnyType
        return _interpreter.evaluateExpression(EvaluationContext.ofSelf(element.toTypedObject(elType)), this).asmValue
    }

    private fun NavigationExpression.createReferenceLocalToScope(scope: Scope<ItemInScopeType>, element: AsmStructure): AsmValue {
        val elType = typeModel.findByQualifiedNameOrNull(element.qualifiedTypeName)?.type() ?: StdLibDefault.AnyType
        return _interpreter.evaluateExpression(EvaluationContext.ofSelf(element.toTypedObject(elType)), this).asmValue
    }
*/

}