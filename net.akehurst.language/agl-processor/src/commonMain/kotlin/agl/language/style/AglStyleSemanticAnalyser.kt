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

package net.akehurst.language.agl.language.style

import net.akehurst.language.agl.language.grammar.ContextFromGrammar
import net.akehurst.language.agl.processor.Agl
import net.akehurst.language.agl.processor.IssueHolder
import net.akehurst.language.agl.processor.SemanticAnalysisResultDefault
import net.akehurst.language.api.grammarTypeModel.GrammarTypeNamespace
import net.akehurst.language.api.parser.InputLocation
import net.akehurst.language.api.processor.LanguageProcessorPhase
import net.akehurst.language.api.processor.SemanticAnalysisOptions
import net.akehurst.language.api.processor.SemanticAnalysisResult
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyser
import net.akehurst.language.api.style.AglStyleModel
import net.akehurst.language.api.style.AglStyleSelectorKind

class AglStyleSemanticAnalyser() : SemanticAnalyser<AglStyleModel, ContextFromGrammar> {

    companion object {
        private val aglGrammarQualifiedName = Agl.registry.agl.grammar.processor!!.grammar!!.qualifiedName
        private val aglGrammarTypeModel = Agl.registry.agl.grammar.processor!!.typeModel
        private val aglGrammarNamespace: GrammarTypeNamespace
            get() = aglGrammarTypeModel.namespace[aglGrammarQualifiedName] as GrammarTypeNamespace? ?: error("Internal error")

        private val grammarRule = aglGrammarNamespace.findTypeUsageForRule("grammarRule") ?: error("Internal error: type for 'grammarRule' not found")
    }

    override fun clear() {

    }

    override fun analyse(
        asm: AglStyleModel,
        locationMap: Map<Any, InputLocation>?,
        context: ContextFromGrammar?,
        options: SemanticAnalysisOptions<AglStyleModel, ContextFromGrammar>
    ): SemanticAnalysisResult {
        val locMap = locationMap ?: mapOf()
        val issues = IssueHolder(LanguageProcessorPhase.SEMANTIC_ANALYSIS)
        if (null != context) {
            asm.rules.forEach { rule ->
                rule.selector.forEach { sel ->
                    val loc = locMap[sel]
                    // TODO: user types
                    when (sel.kind) {
                        AglStyleSelectorKind.LITERAL -> {
                            if (context.rootScope.isMissing(sel.value, "LITERAL")) {
                                issues.error(loc, "Terminal Literal ${sel.value} not found for style rule")
                            }
                        }

                        AglStyleSelectorKind.PATTERN -> {
                            if (context.rootScope.isMissing(sel.value, "PATTERN")) {
                                issues.error(loc, "Terminal Pattern ${sel.value} not found for style rule")
                            }
                        }

                        AglStyleSelectorKind.RULE_NAME -> {
                            if (AglStyleSyntaxAnalyser.KEYWORD_STYLE_ID == sel.value) {
                                // its OK
                            } else {
                                if (context.rootScope.isMissing(sel.value, grammarRule.type.qualifiedName)) {
                                    issues.error(loc, "Grammar Rule '${sel.value}' not found for style rule")
                                }
                            }
                        }

                        AglStyleSelectorKind.META -> Unit // nothing to check
                    }
                }
            }
        }

        return SemanticAnalysisResultDefault(issues)
    }
}