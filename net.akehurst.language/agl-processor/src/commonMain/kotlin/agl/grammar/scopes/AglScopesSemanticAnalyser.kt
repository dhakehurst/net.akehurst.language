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

package net.akehurst.language.agl.agl.grammar.scopes

import net.akehurst.language.agl.grammar.scopes.*
import net.akehurst.language.agl.processor.IssueHolder
import net.akehurst.language.agl.processor.SemanticAnalysisResultDefault
import net.akehurst.language.agl.semanticAnalyser.ContextFromTypeModel
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.processor.LanguageProcessorPhase
import net.akehurst.language.api.processor.SemanticAnalysisOptions
import net.akehurst.language.api.processor.SemanticAnalysisResult
import net.akehurst.language.api.semanticAnalyser.Scope
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyser
import net.akehurst.language.api.semanticAnalyser.SentenceContext

class AglScopesSemanticAnalyser : SemanticAnalyser<ScopeModelAgl, SentenceContext<String>> {

    private val issues = IssueHolder(LanguageProcessorPhase.SEMANTIC_ANALYSIS)
    private var locationMap: Map<Any, InputLocation> = emptyMap()

    override fun clear() {
        locationMap = emptyMap()
        issues.clear()
    }

    override fun analyse(
        asm: ScopeModelAgl,
        locMap: Map<Any, InputLocation>?,
        context: SentenceContext<String>?,
        options: SemanticAnalysisOptions<ScopeModelAgl, SentenceContext<String>>
    ): SemanticAnalysisResult {
        this.locationMap = locMap ?: mapOf()
        if (null != context) {
            asm.scopes.forEach { (_, scope) ->
                val msgStart = if (ScopeModelAgl.ROOT_SCOPE_TYPE_NAME == scope.scopeFor) {
                    //do nothing
                    "In root scope"
                } else {
                    if (context.rootScope.isMissing(scope.scopeFor, ContextFromTypeModel.TYPE_NAME_FOR_TYPES)) {
                        raiseError(AglScopesSyntaxAnalyser.PropertyValue(scope, "typeReference"), "Type '${scope.scopeFor}' not found in scope")
                    } else {
                        //OK
                    }
                    "In scope for '${scope.scopeFor}'"
                }
                scope.identifiables.forEach { identifiable ->
                    val typeScope = context.rootScope.childScopes[identifiable.typeName]
                    when {
                        null == typeScope -> {
                            raiseError(AglScopesSyntaxAnalyser.PropertyValue(identifiable, "typeReference"), "Type '${identifiable.typeName}' not found in scope")
                        }

                        identifiable.navigation.isNothing -> Unit
                        else -> {
                            // only check this if the typeName is valid - else it is always invalid
                            //TODO: check this in context of typeName GrammarRule
                            for (part in identifiable.navigation.value) {
                                if (typeScope.isMissing(part, ContextFromTypeModel.TYPE_NAME_FOR_PROPERTIES)) {
                                    raiseError(
                                        AglScopesSyntaxAnalyser.PropertyValue(identifiable, "propertyName"),
                                        "$msgStart, '${identifiable.navigation}' not found for identifying property of '${identifiable.typeName}'"
                                    )
                                } else {
                                    //OK
                                }
                            }
                        }
                    }
                }
            }

            asm.references.forEach { ref ->
                if (context.rootScope.isMissing(ref.inTypeName, ContextFromTypeModel.TYPE_NAME_FOR_TYPES)) {
                    raiseError(
                        AglScopesSyntaxAnalyser.PropertyValue(ref, "in"),
                        "Referring type '${ref.inTypeName}' not found in scope"
                    )
                } else {
                    val typeScope = context.rootScope.childScopes[ref.inTypeName]
                    if (null == typeScope) {
                        raiseError(
                            AglScopesSyntaxAnalyser.PropertyValue(ref, "propertyReference"),
                            "Child scope '${ref.inTypeName}' not found"
                        )
                    } else {
                        for (refExpr in ref.referenceExpressionList) {
                            checkReferenceExpression(typeScope, ref, refExpr)
                        }
                    }
                }
            }
        }

        return SemanticAnalysisResultDefault(issues)
    }

    private fun raiseError(obj: Any, message: String) {
        val loc = locationMap[obj]
        issues.error(loc, message)
    }

    private fun checkReferenceExpression(typeScope: Scope<String>, ref: ReferenceDefinition, refExpr: ReferenceExpression) = when (refExpr) {
        is PropertyReferenceExpression -> checkPropertyReferenceExpression(typeScope, ref, refExpr)
        is CollectionReferenceExpression -> checkCollectionReferenceExpression(typeScope, ref, refExpr)
        else -> error("subtype of 'ReferenceExpression' not handled: '${refExpr::class.simpleName}'")
    }

    private fun checkCollectionReferenceExpression(
        typeScope: Scope<String>,
        ref: ReferenceDefinition,
        refExpr: CollectionReferenceExpression
    ) {
        for (re in ref.referenceExpressionList) {
            checkReferenceExpression(typeScope, ref, re)
        }
    }

    private fun checkPropertyReferenceExpression(typeScope: Scope<String>, ref: ReferenceDefinition, refExpr: PropertyReferenceExpression) {
        if (typeScope.isMissing(refExpr.referringPropertyName, ContextFromTypeModel.TYPE_NAME_FOR_PROPERTIES)) {
            raiseError(
                AglScopesSyntaxAnalyser.PropertyValue(refExpr, "propertyReference"),
                "For reference in '${ref.inTypeName}' referring property '${refExpr.referringPropertyName}' not found"
            )
        }
        refExpr.refersToTypeName.forEachIndexed { i, n ->
            if (typeScope.isMissing(n, ContextFromTypeModel.TYPE_NAME_FOR_TYPES)) {
                raiseError(
                    AglScopesSyntaxAnalyser.PropertyValue(ref, "typeReferences[$i]"),
                    "For reference in '${ref.inTypeName}' referred to type '$n' not found"
                )
            }
        }
    }


}