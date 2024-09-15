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

import net.akehurst.language.agl.Agl
import net.akehurst.language.agl.language.grammar.ContextFromGrammar
import net.akehurst.language.agl.language.style.asm.AglStyleModelDefault
import net.akehurst.language.agl.processor.IssueHolder
import net.akehurst.language.agl.processor.SemanticAnalysisResultDefault
import net.akehurst.language.api.grammarTypeModel.GrammarTypeNamespace
import net.akehurst.language.api.language.grammar.GrammarRuleName
import net.akehurst.language.api.language.style.AglStyleModel
import net.akehurst.language.api.language.style.AglStyleSelectorKind
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.api.processor.SemanticAnalysisOptions
import net.akehurst.language.api.processor.SemanticAnalysisResult
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyser
import net.akehurst.language.parser.api.InputLocation

class AglStyleSemanticAnalyser() : SemanticAnalyser<AglStyleModel, ContextFromGrammar> {

    companion object {
        private val aglGrammarQualifiedName = Agl.registry.agl.grammar.processor!!.grammar!!.qualifiedName
        private val aglGrammarTypeModel = Agl.registry.agl.grammar.processor!!.typeModel
        private val aglGrammarNamespace: GrammarTypeNamespace
            get() = aglGrammarTypeModel.findNamespaceOrNull(aglGrammarQualifiedName) as GrammarTypeNamespace? ?: error("Internal error")

        private val grammarRule = aglGrammarNamespace.findTypeForRule(GrammarRuleName("grammarRule")) ?: error("Internal error: type for 'grammarRule' not found")
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
            asm.allDefinitions.forEach { rule ->
                rule.selector.forEach { sel ->
                    val loc = locMap[sel]
                    // TODO: user types
                    when (sel.kind) {
                        AglStyleSelectorKind.LITERAL -> {
                            if (context.rootScope.findItemsNamedConformingTo(sel.value) { it.value == "LITERAL" }.isEmpty()) {
                                issues.error(loc, "Terminal Literal ${sel.value} not found for style rule")
                            }
                        }

                        AglStyleSelectorKind.PATTERN -> {
                            if (context.rootScope.findItemsNamedConformingTo(sel.value) { it.value == "PATTERN" }.isEmpty()) {
                                issues.error(loc, "Terminal Pattern ${sel.value} not found for style rule")
                            }
                        }

                        AglStyleSelectorKind.RULE_NAME -> {
                            if (AglStyleModelDefault.KEYWORD_STYLE_ID == sel.value) { //TODO: redundant check I think!
                                // its OK
                            } else {
                                if (context.rootScope.findItemsNamedConformingTo(sel.value) { it == grammarRule.declaration.qualifiedName }.isEmpty()) {
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