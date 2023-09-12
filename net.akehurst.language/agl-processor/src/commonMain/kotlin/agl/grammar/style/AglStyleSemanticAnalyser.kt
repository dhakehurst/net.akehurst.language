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

package net.akehurst.language.agl.agl.grammar.style

import net.akehurst.language.agl.grammar.style.AglStyleSyntaxAnalyser
import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.agl.processor.IssueHolder
import net.akehurst.language.agl.processor.SemanticAnalysisResultDefault
import net.akehurst.language.api.analyser.SemanticAnalyser
import net.akehurst.language.api.grammar.GrammarItem
import net.akehurst.language.api.grammarTypeModel.GrammarTypeNamespace
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.processor.LanguageIssue
import net.akehurst.language.api.processor.LanguageProcessorPhase
import net.akehurst.language.api.processor.SemanticAnalysisResult
import net.akehurst.language.api.processor.SentenceContext
import net.akehurst.language.api.style.AglStyleModel

class AglStyleSemanticAnalyser() : SemanticAnalyser<AglStyleModel, SentenceContext<String>> {

    private val aglGrammarTypeModel = Agl.registry.agl.grammar.processor!!.typeModel
    private val namespace: GrammarTypeNamespace
        get() =
            aglGrammarTypeModel.namespace[Agl.registry.agl.grammar.processor!!.grammar!!.qualifiedName] as GrammarTypeNamespace? ?: error("")

    override fun clear() {

    }

    override fun configure(configurationContext: SentenceContext<GrammarItem>, configuration: Map<String, Any>): List<LanguageIssue> {
        return emptyList()
    }

    override fun analyse(asm: AglStyleModel, locationMap: Map<Any, InputLocation>?, context: SentenceContext<String>?, options: Map<String, Any>): SemanticAnalysisResult {
        val locMap = locationMap ?: mapOf()
        val issues = IssueHolder(LanguageProcessorPhase.SEMANTIC_ANALYSIS)
        if (null != context) {
            asm.rules.forEach { rule ->
                rule.selector.forEach { sel ->
                    if (AglStyleSyntaxAnalyser.KEYWORD_STYLE_ID == sel) {
                        //it is ok
                    } else {
                        val typeName = ""
                        TODO()
                        //TODO: give selectors a type/kind pattern/literal/identifier...
                        if (context.rootScope.isMissing(sel, typeName)) {
                            val loc = locMap[rule]
                            if (sel.startsWith("'") && sel.endsWith("'")) {
                                issues.error(loc, "Terminal Literal ${sel} not found for style rule")
                            } else if (sel.startsWith("\"") && sel.endsWith("\"")) {
                                issues.error(loc, "Terminal Pattern ${sel} not found for style rule")

                            } else {
                                issues.error(loc, "GrammarRule '${sel}' not found for style rule")
                            }
                        } else {
                            //no issues
                        }
                    }
                }
            }
        }

        return SemanticAnalysisResultDefault(issues)
    }
}