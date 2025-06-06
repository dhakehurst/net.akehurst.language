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

package net.akehurst.language.style.processor

import net.akehurst.language.agl.processor.SemanticAnalysisResultDefault
import net.akehurst.language.agl.simple.ContextWithScope
import net.akehurst.language.agl.syntaxAnalyser.LocationMapDefault
import net.akehurst.language.api.processor.ResolvedReference
import net.akehurst.language.api.processor.SemanticAnalysisOptions
import net.akehurst.language.api.processor.SemanticAnalysisResult
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyser
import net.akehurst.language.api.syntaxAnalyser.LocationMap
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.style.api.AglStyleMetaRule
import net.akehurst.language.style.api.AglStyleModel
import net.akehurst.language.style.api.AglStyleSelectorKind
import net.akehurst.language.style.api.AglStyleTagRule

class AglStyleSemanticAnalyser() : SemanticAnalyser<AglStyleModel, ContextWithScope<Any,Any>> {

    companion object {
        // TODO: AglGrammar.typesModel.findTypeForRule(GrammarRuleName("grammarRule"))
        private val grammarRuleQualifiedName = QualifiedName("net.akehurst.language.grammar.api.GrammarRule")
    }

    val _issues = IssueHolder(LanguageProcessorPhase.SEMANTIC_ANALYSIS)
    private val _resolvedReferences = mutableListOf<ResolvedReference>()

    override fun clear() {
        _issues.clear()
        _resolvedReferences.clear()
    }

    override fun analyse(
        sentenceIdentity:Any?,
        asm: AglStyleModel,
        locationMap: LocationMap?,
        options: SemanticAnalysisOptions<ContextWithScope<Any,Any>>
    ): SemanticAnalysisResult {
        val context = options.context
        val locMap = locationMap ?: LocationMapDefault()
        if (null != context) {
            asm.allDefinitions.forEach { ss ->
                ss.rules.forEach { rule ->
                    when (rule) {
                        is AglStyleMetaRule -> analyseMetaRule(rule, locMap, context)
                        is AglStyleTagRule -> analyseTagRule(rule, locMap, context)
                    }
                }
            }
        }

        return SemanticAnalysisResultDefault(_resolvedReferences,_issues)
    }

    private fun analyseMetaRule(rule: AglStyleMetaRule, locMap: LocationMap, context: ContextWithScope<Any,Any>) {
    }

    private fun analyseTagRule(rule: AglStyleTagRule, locMap: LocationMap, context: ContextWithScope<Any,Any>) {
        rule.selector.forEach { sel ->
            val loc = locMap[sel]
            // TODO: user types
            when (sel.kind) {
                AglStyleSelectorKind.LITERAL -> {
                    if (context.findItemsNamedConformingTo(sel.value) { it.value == "LITERAL" }.isEmpty()) {
                        _issues.error(loc, "Terminal Literal ${sel.value} not found for style rule")
                    }
                }

                AglStyleSelectorKind.PATTERN -> {
                    if (context.findItemsNamedConformingTo(sel.value) { it.value == "PATTERN" }.isEmpty()) {
                        _issues.error(loc, "Terminal Pattern ${sel.value} not found for style rule")
                    }
                }

                AglStyleSelectorKind.RULE_NAME -> {
                    if (context.findItemsNamedConformingTo(sel.value) { it == grammarRuleQualifiedName }.isEmpty()) {
                        _issues.error(loc, "Grammar Rule '${sel.value}' not found for style rule")
                    }
                }

                AglStyleSelectorKind.SPECIAL -> Unit
            }
        }
    }
}