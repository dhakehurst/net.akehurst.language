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

import net.akehurst.language.agl.grammar.scopes.AglScopesSyntaxAnalyser
import net.akehurst.language.agl.grammar.scopes.ScopeModelAgl
import net.akehurst.language.agl.processor.IssueHolder
import net.akehurst.language.agl.processor.SemanticAnalysisResultDefault
import net.akehurst.language.agl.semanticAnalyser.ContextFromTypeModel
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.processor.LanguageProcessorPhase
import net.akehurst.language.api.processor.SemanticAnalysisOptions
import net.akehurst.language.api.processor.SemanticAnalysisResult
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyser
import net.akehurst.language.api.semanticAnalyser.SentenceContext

class AglScopesSemanticAnalyser : SemanticAnalyser<ScopeModelAgl, SentenceContext<String>> {

    override fun clear() {

    }

//    override fun configure(configurationContext: SentenceContext<GrammarItem>, configuration: Map<String, Any>): List<LanguageIssue> {
//        return emptyList()
//    }

    override fun analyse(
        asm: ScopeModelAgl,
        locationMap: Map<Any, InputLocation>?,
        context: SentenceContext<String>?,
        options: SemanticAnalysisOptions<ScopeModelAgl, SentenceContext<String>>
    ): SemanticAnalysisResult {
        val locMap = locationMap ?: mapOf()
        val issues = IssueHolder(LanguageProcessorPhase.SEMANTIC_ANALYSIS)
        if (null != context) {
            asm.scopes.forEach { (_, scope) ->
                val msgStart = if (ScopeModelAgl.ROOT_SCOPE_TYPE_NAME == scope.scopeFor) {
                    //do nothing
                    "In root scope"
                } else {
                    if (context.rootScope.isMissing(scope.scopeFor, ContextFromTypeModel.TYPE_NAME_FOR_TYPES)) {
                        val loc = locMap[AglScopesSyntaxAnalyser.PropertyValue(scope, "typeReference")]
                        issues.error(loc, "Type '${scope.scopeFor}' not found in scope")
                    } else {
                        //OK
                    }
                    "In scope for '${scope.scopeFor}'"
                }
                scope.identifiables.forEach { identifiable ->
                    val typeScope = context.rootScope.childScopes[identifiable.typeName]
                    when {
                        null == typeScope -> {
                            val loc = locMap[AglScopesSyntaxAnalyser.PropertyValue(identifiable, "typeReference")]
                            issues.error(loc, "Type '${identifiable.typeName}' not found in scope")
                        }

                        ScopeModelAgl.IDENTIFY_BY_NOTHING == identifiable.propertyName -> Unit
                        else -> {
                            // only check this if the typeName is valid - else it is always invalid
                            //TODO: check this in context of typeName GrammarRule
                            if (typeScope.isMissing(identifiable.propertyName, ContextFromTypeModel.TYPE_NAME_FOR_PROPERTIES)) {
                                val loc = locMap[AglScopesSyntaxAnalyser.PropertyValue(identifiable, "propertyName")]
                                issues.error(
                                    loc,
                                    "$msgStart, '${identifiable.propertyName}' not found for identifying property of '${identifiable.typeName}'"
                                )
                            } else {
                                //OK
                            }
                        }
                    }
                }
            }

            asm.references.forEach { ref ->
                if (context.rootScope.isMissing(ref.inTypeName, ContextFromTypeModel.TYPE_NAME_FOR_TYPES)) {
                    val loc = locMap[AglScopesSyntaxAnalyser.PropertyValue(ref, "in")]
                    issues.error(loc, "Referring type '${ref.inTypeName}' not found in scope")
                } else {
                    val typeScope = context.rootScope.childScopes[ref.inTypeName]
                    if (null == typeScope) {
                        val loc = locMap[AglScopesSyntaxAnalyser.PropertyValue(ref, "propertyReference")]
                        issues.error(loc, "Child scope '${ref.inTypeName}' not found")
                    } else {
                        if (typeScope.isMissing(ref.referringPropertyName, ContextFromTypeModel.TYPE_NAME_FOR_PROPERTIES)) {
                            val loc = locMap[AglScopesSyntaxAnalyser.PropertyValue(ref, "propertyReference")]
                            issues.error(loc, "For reference in '${ref.inTypeName}' referring property '${ref.referringPropertyName}' not found")
                        }
                    }
                }
                ref.refersToTypeName.forEachIndexed { i, n ->
                    if (context.rootScope.isMissing(n, ContextFromTypeModel.TYPE_NAME_FOR_TYPES)) {
                        val loc = locMap[AglScopesSyntaxAnalyser.PropertyValue(ref, "typeReferences[$i]")]
                        issues.error(loc, "For reference in '${ref.inTypeName}' referred to type '$n' not found")
                    }
                }
            }
        }

        return SemanticAnalysisResultDefault(issues)
    }
}