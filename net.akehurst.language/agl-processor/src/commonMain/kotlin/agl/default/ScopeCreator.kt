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

package net.akehurst.language.agl.agl.default

import net.akehurst.language.agl.asm.isStdString
import net.akehurst.language.agl.language.expressions.ExpressionsInterpreterOverAsmSimple
import net.akehurst.language.agl.language.reference.CrossReferenceModelDefault
import net.akehurst.language.agl.processor.IssueHolder
import net.akehurst.language.api.asm.*
import net.akehurst.language.api.language.expressions.Expression
import net.akehurst.language.api.language.expressions.Navigation
import net.akehurst.language.api.language.expressions.RootExpression
import net.akehurst.language.api.language.reference.Scope
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.collections.mutableStackOf
import net.akehurst.language.typemodel.api.TypeModel

class ScopeCreator(
    val typeModel: TypeModel,
    val crossReferenceModel: CrossReferenceModelDefault,
    val rootScope: Scope<AsmPath>,
    val locationMap: Map<Any, InputLocation>,
    val issues: IssueHolder
) : AsmTreeWalker {

    private val _interpreter = ExpressionsInterpreterOverAsmSimple(typeModel)

    val currentScope = mutableStackOf(rootScope)

    override fun beforeRoot(root: AsmValue) {
        when (root) {
            is AsmStructure -> addToScope(currentScope.peek(), root)
            else -> Unit
        }
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

    private fun createScope(scope: Scope<AsmPath>, el: AsmStructure): Scope<AsmPath> {
        val exp = crossReferenceModel.identifyingExpressionFor(scope.forTypeName, el.qualifiedTypeName)
        return if (null != exp && crossReferenceModel.isScopeDefinedFor(el.typeName)) {
            val refInParent = exp.createReferenceLocalToScope(scope, el)
            when {
                refInParent is AsmNothing -> scope.createOrGetChildScope(el.typeName, el.typeName, el.path)
                refInParent is AsmPrimitive && refInParent.isStdString -> scope.createOrGetChildScope((refInParent.value as String), el.typeName, el.path)
                else -> {
                    issues.error(this.locationMap[el], "Cannot create a local reference in '$scope' for '$el' because its identifying expression evaluates to $refInParent")
                    scope
                }
            }
        } else {
            scope
        }
    }

    private fun addToScope(scope: Scope<AsmPath>, el: AsmStructure) {
        val exp = crossReferenceModel.identifyingExpressionFor(scope.forTypeName, el.qualifiedTypeName)
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
                    val added = scope.addToScope(el.typeName, el.qualifiedTypeName, contextRef).not()
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

                else -> {
                    issues.error(this.locationMap[el], "Cannot create a local reference in '$scope' for '$el' because its identifying expression evaluates to $scopeLocalReference")
                }
            }

        } else {
            // no need to add it to scope
        }
    }

    private fun Expression.createReferenceLocalToScope(scope: Scope<AsmPath>, element: AsmStructure): AsmValue = when (this) {
        is RootExpression -> this.createReferenceLocalToScope(scope, element)
        is Navigation -> this.createReferenceLocalToScope(scope, element)
        else -> error("Subtype of Expression not handled in 'createReferenceLocalToScope'")
    }

    private fun RootExpression.createReferenceLocalToScope(scope: Scope<AsmPath>, element: AsmStructure): AsmValue =
        _interpreter.evaluateExpression(element, this)


    private fun Navigation.createReferenceLocalToScope(scope: Scope<AsmPath>, element: AsmStructure): AsmValue =
        _interpreter.evaluateExpression(element, this)


}