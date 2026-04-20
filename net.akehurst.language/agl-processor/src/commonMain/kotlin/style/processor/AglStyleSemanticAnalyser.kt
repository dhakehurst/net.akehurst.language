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
import net.akehurst.language.api.semanticAnalyser.SentenceContext
import net.akehurst.language.agl.syntaxAnalyser.LocationMapDefault
import net.akehurst.language.api.processor.ResolvedReference
import net.akehurst.language.api.processor.SemanticAnalysisOptions
import net.akehurst.language.api.processor.SemanticAnalysisResult
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyser
import net.akehurst.language.api.syntaxAnalyser.LocationMap
import net.akehurst.language.base.api.PossiblyQualifiedName
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.style.api.*

class AglStyleSemanticAnalyser() : SemanticAnalyser<AglStyleDomain, SentenceContext> {

    companion object {
        // TODO: AglGrammar.typesModel.findTypeForRule(GrammarRuleName("grammarRule"))
        private val grammarRuleQualifiedName = QualifiedName("net.akehurst.language.grammar.api.GrammarRule")

        fun findStyleSetOrNull(sentenceContext: SentenceContext, localNamespace: QualifiedName, nameOrQName: PossiblyQualifiedName): StyleSet? =
            sentenceContext.findItemsByQualifiedNameConformingTo(nameOrQName.asQualifiedName(localNamespace).parts.map { it.value }) { itemTypeName ->
                itemTypeName.value == StyleSet::class.simpleName!! //TODO: use qualified when kotlin.JS supports it
            }.firstOrNull()?.item as StyleSet?
    }

    val _issues = IssueHolder(LanguageProcessorPhase.SEMANTIC_ANALYSIS)
    private val _resolvedReferences = mutableListOf<ResolvedReference>()
    private var _locationMap: LocationMap? = null

    override fun clear() {
        _issues.clear()
        _resolvedReferences.clear()
        _locationMap = null
    }

    override fun analyse(
        sentenceIdentity:Any?,
        asm: AglStyleDomain,
        locationMap: LocationMap?,
        options: SemanticAnalysisOptions<SentenceContext>
    ): SemanticAnalysisResult {
        this._locationMap = locationMap ?: LocationMapDefault()
        val sentenceContext = options.sentenceContext
        val locMap = locationMap ?: LocationMapDefault()
        if (null != sentenceContext) {
            asm.namespace.forEach { ns ->
                ns.definition.forEach { ss ->
                    resolveStyleSetRefs(sentenceContext, ss)

                    ss.rules.forEach { rule ->
                        when (rule) {
                            is AglStyleMetaRule -> analyseMetaRule(rule, locMap, sentenceContext)
                            is AglStyleTagRule -> analyseTagRule(rule, locMap, sentenceContext)
                        }
                    }
                }
            }
         }

        return SemanticAnalysisResultDefault(_resolvedReferences,_issues)
    }

    private fun issueError(item: Any, message: String, data: Any?) {
        val location = this._locationMap?.get(item)
        _issues.error(location, message, data)
    }

    private fun resolveStyleSetRefs(sentenceContext: SentenceContext?, styleSet: StyleSet) {
        styleSet.extends.forEach { ref ->
            checkStyleSetExistsAndResolve(sentenceContext,ref)
        }
    }

    private fun checkStyleSetExistsAndResolve(sentenceContext: SentenceContext?, ref: StyleSetReference) {
        val resolvedSS = sentenceContext?.let {
            findStyleSetOrNull(it, ref.localNamespace.qualifiedName, ref.nameOrQName)
        }
        if (null == resolvedSS) {
            this.issueError(ref, "StyleSet '${ref.nameOrQName}' not found", null)
        } else {
            ref.resolveAs(resolvedSS)
        }
    }

    private fun analyseMetaRule(rule: AglStyleMetaRule, locMap: LocationMap, sentenceContext: SentenceContext) {
    }

    private fun analyseTagRule(rule: AglStyleTagRule, locMap: LocationMap, sentenceContext: SentenceContext) {
        rule.selector.forEach { sel ->
            val loc = locMap[sel]
            // TODO: user types
            when (sel.kind) {
                AglStyleSelectorKind.LITERAL -> {
                    if (sentenceContext.findItemsNamedConformingTo(sel.value) { it.value == "LITERAL" }.isEmpty()) {
                        _issues.error(loc, "Terminal Literal ${sel.value} not found for style rule")
                    }
                }

                AglStyleSelectorKind.PATTERN -> {
                    if (sentenceContext.findItemsNamedConformingTo(sel.value) { it.value == "PATTERN" }.isEmpty()) {
                        _issues.error(loc, "Terminal Pattern ${sel.value} not found for style rule")
                    }
                }

                AglStyleSelectorKind.RULE_NAME -> {
                    if (sentenceContext.findItemsNamedConformingTo(sel.value) { it == grammarRuleQualifiedName }.isEmpty()) {
                        _issues.error(loc, "Grammar Rule '${sel.value}' not found for style rule")
                    }
                }

                AglStyleSelectorKind.SPECIAL -> Unit
            }
        }
    }
}