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

import net.akehurst.language.agl.language.reference.CrossReferenceModelDefault
import net.akehurst.language.agl.processor.IssueHolder
import net.akehurst.language.agl.semanticAnalyser.createReferenceLocalToScope
import net.akehurst.language.api.asm.AsmElementPath
import net.akehurst.language.api.asm.AsmElementProperty
import net.akehurst.language.api.asm.AsmElementSimple
import net.akehurst.language.api.asm.AsmSimpleTreeWalker
import net.akehurst.language.api.language.reference.Scope
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.collections.mutableStackOf

class ScopeCreator(
    val crossReferenceModel: CrossReferenceModelDefault,
    val rootScope: Scope<AsmElementPath>,
    val locationMap: Map<Any, InputLocation>,
    val issues: IssueHolder
) : AsmSimpleTreeWalker {

    val currentScope = mutableStackOf(rootScope)

    override fun root(root: AsmElementSimple) {
        addToScope(currentScope.peek(), root)
    }

    override fun beforeElement(propertyName: String?, element: AsmElementSimple) {
        val scope = currentScope.peek()
        addToScope(scope, element)
        val chScope = createScope(scope, element)
        currentScope.push(chScope)
    }

    override fun afterElement(propertyName: String?, element: AsmElementSimple) {
        currentScope.pop()
    }

    override fun property(element: AsmElementSimple, property: AsmElementProperty) {
        // do nothing
    }

    private fun createScope(scope: Scope<AsmElementPath>, el: AsmElementSimple): Scope<AsmElementPath> {
        val exp = crossReferenceModel.identifyingExpressionFor(scope.forTypeName, el.typeName)
        return if (null != exp && crossReferenceModel.isScopeDefinedFor(el.typeName)) {
            val refInParent = exp.createReferenceLocalToScope(scope, el)
            if (null != refInParent) {
                val newScope = scope.createOrGetChildScope(refInParent, el.typeName, el.asmPath)
                //_scopeMap[el.asmPath] = newScope
                newScope
            } else {
                issues.warn(
                    this.locationMap[el],
                    "Trying to create child scope but cannot create a reference for '$el' because its identifying expression evaluates to null. Using type name as identifier."
                )
                val newScope = scope.createOrGetChildScope(el.typeName, el.typeName, el.asmPath)
                //_scopeMap[el.asmPath] = newScope
                newScope
            }
        } else {
            scope
        }
    }

    private fun addToScope(scope: Scope<AsmElementPath>, el: AsmElementSimple) {
        val exp = crossReferenceModel.identifyingExpressionFor(scope.forTypeName, el.typeName)
        if (null != exp) {
            //val reference = _scopeModel!!.createReferenceFromRoot(scope, el)
            val scopeLocalReference = exp.createReferenceLocalToScope(scope, el)
            if (null != scopeLocalReference) {
                val contextRef = el.asmPath
                scope.addToScope(scopeLocalReference, el.typeName, contextRef)
            } else {
                issues.warn(
                    this.locationMap[el],
                    "Cannot create a local reference in '$scope' for '$el' because its identifying expression evaluates to null. Using type name as identifier."
                )
                val contextRef = el.asmPath
                scope.addToScope(el.typeName, el.typeName, contextRef)
            }
        } else {
            // no need to add it to scope
        }
    }

}