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

package net.akehurst.language.format.processor

import net.akehurst.language.agl.processor.SemanticAnalysisResultDefault
import net.akehurst.language.agl.simple.SentenceContextAny
import net.akehurst.language.agl.syntaxAnalyser.LocationMapDefault
import net.akehurst.language.api.processor.SemanticAnalysisOptions
import net.akehurst.language.api.processor.SemanticAnalysisResult
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyser
import net.akehurst.language.api.syntaxAnalyser.LocationMap
import net.akehurst.language.automaton.api.AutomatonKind
import net.akehurst.language.base.api.PossiblyQualifiedName
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.formatter.api.AglFormatDomain
import net.akehurst.language.formatter.api.FormatSet
import net.akehurst.language.formatter.api.FormatSetReference
import net.akehurst.language.grammar.api.Grammar
import net.akehurst.language.grammar.api.GrammarDomain
import net.akehurst.language.grammar.api.GrammarReference
import net.akehurst.language.grammar.processor.AglGrammarSemanticAnalyser.Companion.findGrammarOrNull
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder

class AglFormatSemanticAnalyser : SemanticAnalyser<AglFormatDomain, SentenceContextAny> {

    companion object {
        fun findFormatSetOrNull(context: SentenceContextAny, localNamespace: QualifiedName, nameOrQName: PossiblyQualifiedName): FormatSet? =
            context.findItemsByQualifiedNameConformingTo(nameOrQName.asQualifiedName(localNamespace).parts.map { it.value }) { itemTypeName ->
                true
            }.firstOrNull()?.item as? FormatSet
    }

    private val _issues = IssueHolder(LanguageProcessorPhase.FORMAT)
    private var _locationMap: LocationMap? = null

    override fun clear() {

    }

//    override fun configure(configurationContext: SentenceContext<GrammarItem>, configuration: Map<String, Any>): List<LanguageIssue> {
//        return emptyList()
//    }

    override fun analyse(
        sentenceIdentity: Any?,
        asm: AglFormatDomain,
        locationMap: LocationMap?,
        options: SemanticAnalysisOptions<SentenceContextAny>
    ): SemanticAnalysisResult {
        this._locationMap = locationMap ?: LocationMapDefault()
        val context = options.sentenceContext
        if (null == context) {
            issueWarn(null, "No ContextFromGrammarRegistry supplied, grammar references cannot be resolved", null)
        } else {
            checkFormatDomain(context,asm)
        }
        return SemanticAnalysisResultDefault(emptyList(), IssueHolder(LanguageProcessorPhase.SEMANTIC_ANALYSIS))
    }

    private fun issueWarn(item: Any?, message: String, data: Any?) {
        val location = this._locationMap?.get(item)
        _issues.warn(location, message, data)
    }

    private fun issueError(item: Any, message: String, data: Any?) {
        val location = this._locationMap?.get(item)
        _issues.error(location, message, data)
    }

    private fun checkFormatDomain(context: SentenceContextAny?, domain: AglFormatDomain) {
        domain.namespace.forEach { ns ->
            ns.definition.forEach { def ->
                this.resolveRefs(context, def)
            }
        }
    }

    private fun resolveRefs(context: SentenceContextAny?, def: FormatSet) {
        def.extends.forEach { checkGrammarExistsAndResolve(context, it) }
    }

    private fun checkGrammarExistsAndResolve(context: SentenceContextAny?, ref: FormatSetReference) {
        val g = context?.let { findFormatSetOrNull(it, ref.localNamespace.qualifiedName, ref.nameOrQName) }
        if (null == g) {
            this.issueError(ref, "Grammar '${ref.nameOrQName}' not found", null)
        } else {
            ref.resolveAs(g)
        }
    }

}