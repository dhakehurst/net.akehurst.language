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

package net.akehurst.language.agl.default

import net.akehurst.language.agl.processor.IssueHolder
import net.akehurst.language.agl.syntaxAnalyser.ScopeSimple
import net.akehurst.language.api.analyser.ScopeModel
import net.akehurst.language.api.asm.*
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.collections.mutableStackOf

typealias ResolveFunction = (ref: AsmElementPath) -> AsmElementSimple?

class ReferenceResolverDefault(
    val scopeModel: ScopeModel,
    val rootScope: ScopeSimple<AsmElementPath>,
    val resolveFunction: ResolveFunction?,
    private val _locationMap: Map<*, InputLocation>?,
    private val _issues: IssueHolder
) : AsmSimpleTreeWalker {

    val scopeStack = mutableStackOf(rootScope)

    private fun error(element: AsmElementSimple, message: String) {
        _issues.error(
            _locationMap?.get(element),//TODO: should be property location
            message
        )
    }

    override fun root(root: AsmElementSimple) {
        val elScope = rootScope.rootScope.scopeMap[root.asmPath] ?: rootScope
        scopeStack.push(elScope)
    }

    override fun beforeElement(propertyName: String?, element: AsmElementSimple) {
        val parentScope = scopeStack.peek()
        val elScope = parentScope.rootScope.scopeMap[element.asmPath] ?: parentScope
        scopeStack.push(elScope)
    }

    override fun afterElement(propertyName: String?, element: AsmElementSimple) {
        scopeStack.pop()
    }

    override fun property(element: AsmElementSimple, property: AsmElementProperty) {
        if (property.isReference) {
            handleProperty(scopeModel, element, property)
        } else {
            // do nothing
        }
    }

    private fun handleProperty(scopeModel: ScopeModel, element: AsmElementSimple, property: AsmElementProperty) {
        val elScope = scopeStack.peek()
        val v = property.value
        if (null == v) {
            //can't set reference, but no issue
        } else if (v is AsmElementReference) {
            val typeNames = scopeModel.getReferredToTypeNameFor(element.typeName, property.name)
            val referredList: List<AsmElementPath> = typeNames.mapNotNull {
                elScope.findOrNull(v.reference, it)
            }
            if (1 < referredList.size) {
                this.error(element, "Multiple options for '${v.reference}' as reference for '${element.typeName}.${property.name}'")
            } else {
                val referred = referredList.firstOrNull()
                if (null == referred) {
                    this.error(element, "Cannot find '${v.reference}' as reference for '${element.typeName}.${property.name}'")
                } else {
                    if (null != resolveFunction) {
                        val rel = resolveFunction.invoke(referred)
                        if (null == rel) {
                            this.error(element, "Asm does not contain element '${v.reference}' as reference for '${element.typeName}.${property.name}'")
                        } else {
                            v.value = rel
                        }
                    } else {
                        // no resolve function so do not resolve, maybe intentional so do not warn or error
                    }
                }
            }
        } else {
            this.error(element, "Cannot resolve reference property '${element.typeName}.${property.name}' because it is not defined as a reference")
        }
    }

}