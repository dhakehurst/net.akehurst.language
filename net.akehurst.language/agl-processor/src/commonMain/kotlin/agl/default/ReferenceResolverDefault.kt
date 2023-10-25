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

import net.akehurst.language.agl.language.scopes.CollectionReferenceExpressionDefault
import net.akehurst.language.agl.language.scopes.PropertyReferenceExpressionDefault
import net.akehurst.language.agl.processor.IssueHolder
import net.akehurst.language.agl.semanticAnalyser.ScopeSimple
import net.akehurst.language.agl.semanticAnalyser.evaluateFor
import net.akehurst.language.agl.semanticAnalyser.propertyFor
import net.akehurst.language.api.asm.*
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.semanticAnalyser.ReferenceExpression
import net.akehurst.language.api.semanticAnalyser.Scope
import net.akehurst.language.api.semanticAnalyser.ScopeModel
import net.akehurst.language.collections.mutableStackOf

typealias ResolveFunction = (ref: AsmElementPath) -> AsmElementSimple?

data class ReferenceExpressionContext(
    val element: AsmElementSimple,
    val scope: Scope<AsmElementPath>
)

/**
 * will check and resolve (if resolveFunction is not null) references.
 * Properties in the asm that are references
 */
class ReferenceResolverDefault(
    val scopeModel: ScopeModel,
    val rootScope: ScopeSimple<AsmElementPath>,
    val resolveFunction: ResolveFunction?,
    private val _locationMap: Map<Any, InputLocation>,
    private val _issues: IssueHolder
) : AsmSimpleTreeWalker {

    private val scopeStack = mutableStackOf(rootScope)
    private val scopeForElement = mutableMapOf<AsmElementSimple, Scope<AsmElementPath>>()
    private fun raiseError(element: AsmElementSimple, message: String) {
        _issues.error(
            _locationMap.get(element),//TODO: should be property location
            message
        )
    }

    override fun root(root: AsmElementSimple) {
        val elScope = rootScope.rootScope.scopeMap[root.asmPath] ?: rootScope
        scopeStack.push(elScope)
        scopeForElement[root] = elScope
    }

    override fun beforeElement(propertyName: String?, element: AsmElementSimple) {
        val parentScope = scopeStack.peek()
        val elScope = parentScope.rootScope.scopeMap[element.asmPath] ?: parentScope
        scopeStack.push(elScope)
        scopeForElement[element] = elScope

        val references = scopeModel.referencesFor(element.typeName)
        for (refExpr in references) {
            handleReferenceExpression(refExpr, ReferenceExpressionContext(element, elScope), element)
        }
    }

    override fun afterElement(propertyName: String?, element: AsmElementSimple) {
        scopeStack.pop()
    }

    override fun property(element: AsmElementSimple, property: AsmElementProperty) {

    }

    private fun handleReferenceExpression(refExpr: ReferenceExpression, context: ReferenceExpressionContext, self: AsmElementSimple) {
        when (refExpr) {
            is PropertyReferenceExpressionDefault -> handlePropertyReferenceExpression(refExpr, context, self)
            is CollectionReferenceExpressionDefault -> handleCollectionReferenceExpression(refExpr, context, self)
            else -> error("subtype of 'ReferenceExpression' not handled: '${refExpr::class.simpleName}'")
        }
    }

    private fun handlePropertyReferenceExpression(refExpr: PropertyReferenceExpressionDefault, context: ReferenceExpressionContext, self: AsmElementSimple) {
        // 'in' typeReference '{' referenceExpression* '}'
        // 'property' navigation 'refers-to' typeReferences from? ;
        //check referred to item exists
        // resolve reference & convert property value to reference
        val scope = when (refExpr.fromNavigation) {
            null -> scopeStack.peek()
            else -> {
                //scope for result of navigation
                val fromEl = refExpr.fromNavigation.evaluateFor(context.element)
                when (fromEl) {
                    null -> error("Cannot get scope for result of '${context.element}.${refExpr.fromNavigation}' in is null")
                    is AsmElementSimple -> scopeForElement[fromEl]!!
                    is AsmElementReference -> {
                        val v = fromEl.value ?: error("'${fromEl.reference}' not resolved, can't get its scope")
                        scopeForElement[v] ?: error("Scope for '${v}' not found !")
                    }

                    else -> error("Cannot get scope for result of '${context.element}.${refExpr.fromNavigation}' in is not an AsmElementSimple, rather it is a '${fromEl::class.simpleName}'")
                }
            }
        }
        var referringValue = refExpr.referringPropertyNavigation.evaluateFor(self)
        if (referringValue is AsmElementReference) {
            referringValue = referringValue.reference
        }
        when (referringValue) {
            is String -> {
                val targets = refExpr.refersToTypeName.mapNotNull { scope.findOrNull(referringValue, it) }
                when {
                    targets.isEmpty() -> {
                        raiseError(self, "No target of type(s) ${refExpr.refersToTypeName} found for referring value '$referringValue' in scope of element '$self'")
                        val referringProperty = refExpr.referringPropertyNavigation.propertyFor(self)
                        referringProperty.convertToReferenceTo(null)
                    }

                    1 < targets.size -> {
                        val msg = "Multiple target of type(s) ${refExpr.refersToTypeName} found for referring value '$referringValue' in scope of element '$self': $targets"
                        raiseError(self, msg)
                        val referringProperty = refExpr.referringPropertyNavigation.propertyFor(self)
                        referringProperty.convertToReferenceTo(null)
                    }

                    else -> {
                        val referred = targets.first() // already checked for empty and > 1, so must be only one
                        if (null != resolveFunction) {
                            val rel = resolveFunction.invoke(referred)
                            if (null == rel) {
                                this.raiseError(self, "Asm does not contain element '$referred' as reference for '${self.typeName}.${refExpr.referringPropertyNavigation}'")
                            }
                            val referringProperty = refExpr.referringPropertyNavigation.propertyFor(self)
                            referringProperty.convertToReferenceTo(rel)
                        } else {
                            // no resolve function so do not resolve, maybe intentional so do not warn or error
                        }
                    }
                }
            }

            else -> raiseError(self, "Referring value '${self.typeName}.${refExpr.referringPropertyNavigation}=$referringValue' on element $self is not a String")
        }

    }

    private fun handleCollectionReferenceExpression(refExpr: CollectionReferenceExpressionDefault, context: ReferenceExpressionContext, self: AsmElementSimple) {
        val coll = refExpr.navigation.evaluateFor(self)
        for (re in refExpr.referenceExpressionList) {
            when (coll) {
                null -> Unit //do nothing
                is Iterable<*> -> {
                    for (lv in coll) {
                        when (lv) {
                            null -> Unit //do nothing
                            is AsmElementSimple -> {
                                //TODO: val ofType = _grammarNamespace?.findTypeNamed(it)
                                //TODO: need type model to check for subtypes, just check type name for now
                                val isCorrectType = null == refExpr.ofType || refExpr.ofType == lv.typeName
                                when {
                                    isCorrectType -> handleReferenceExpression(re, context, lv)
                                    else -> Unit
                                }
                            }

                            else -> error("list element of navigation should be an AsmElementSimple, got '${lv::class.simpleName}'")
                        }
                    }
                }

                else -> error("result of navigation should be a List<AsmElementSimple>, got '${coll::class.simpleName}'")
            }
        }
    }
}
