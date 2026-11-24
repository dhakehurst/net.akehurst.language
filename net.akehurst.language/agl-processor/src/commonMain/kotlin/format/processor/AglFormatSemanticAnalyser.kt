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
import net.akehurst.language.api.semanticAnalyser.SentenceContext
import net.akehurst.language.agl.syntaxAnalyser.LocationMapDefault
import net.akehurst.language.api.processor.SemanticAnalysisOptions
import net.akehurst.language.api.processor.SemanticAnalysisResult
import net.akehurst.language.api.semanticAnalyser.SemanticAnalyser
import net.akehurst.language.api.syntaxAnalyser.LocationMap
import net.akehurst.language.automaton.api.AutomatonKind
import net.akehurst.language.base.api.PossiblyQualifiedName
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.formatter.api.AglFormatDomain
import net.akehurst.language.formatter.api.FormatFunctionDefinition
import net.akehurst.language.formatter.api.FormatSet
import net.akehurst.language.formatter.api.FormatSetReference
import net.akehurst.language.formatter.api.FormatExpressionTemplate
import net.akehurst.language.formatter.api.TemplateElementExpressionProperty
import net.akehurst.language.types.api.DataType
import net.akehurst.language.types.api.PropertyName
import net.akehurst.language.types.api.TypesDomain
import net.akehurst.language.grammar.api.Grammar
import net.akehurst.language.grammar.api.GrammarDomain
import net.akehurst.language.grammar.api.GrammarReference
import net.akehurst.language.grammar.processor.AglGrammarSemanticAnalyser.Companion.findGrammarOrNull
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder

class AglFormatSemanticAnalyser : SemanticAnalyser<AglFormatDomain, SentenceContext> {

    companion object {
        fun findFormatSetOrNull(context: SentenceContext, localNamespace: QualifiedName, nameOrQName: PossiblyQualifiedName): FormatSet? =
            context.findItemsByQualifiedNameConformingTo(nameOrQName.asQualifiedName(localNamespace).parts.map { it.value }) { itemTypeName ->
                true
            }.firstOrNull()?.item as? FormatSet
    }

    private val _issues = IssueHolder(LanguageProcessorPhase.SEMANTIC_ANALYSIS)
    private var _locationMap: LocationMap? = null

    override fun clear() {
        _issues.clear()
    }

//    override fun configure(configurationContext: SentenceContext<GrammarItem>, configuration: Map<String, Any>): List<LanguageIssue> {
//        return emptyList()
//    }

    override fun analyse(
        sentenceIdentity: Any?,
        asm: AglFormatDomain,
        locationMap: LocationMap?,
        options: SemanticAnalysisOptions<SentenceContext>
    ): SemanticAnalysisResult {
        this._locationMap = locationMap ?: LocationMapDefault()
        val context = options.sentenceContext
        if (null == context) {
            issueWarn(null, "No ContextFromGrammarRegistry supplied, grammar references cannot be resolved", null)
        } else {
            checkFormatDomain(context,asm)
        }
        return SemanticAnalysisResultDefault(emptyList(), _issues)
    }

    private fun issueWarn(item: Any?, message: String, data: Any?) {
        val location = this._locationMap?.get(item)
        _issues.warn(location, message, data)
    }

    private fun issueError(item: Any, message: String, data: Any?) {
        val location = this._locationMap?.get(item)
        _issues.error(location, message, data)
    }

    private fun checkFormatDomain(context: SentenceContext?, domain: AglFormatDomain) {
        domain.namespace.forEach { ns ->
            ns.definition.forEach { def ->
                when (def) {
                    is FormatSet -> {
                        this.resolveRefs(context, def)
                        this.checkRuleTypes(context, def)
                    }
                    is FormatFunctionDefinition -> Unit //TODO
                }

            }
        }
    }

    private fun resolveRefs(context: SentenceContext?, def: FormatSet) {
        def.extends.forEach { checkGrammarExistsAndResolve(context, it) }
    }

    /**
     * the type of each format rule must be in the types of the language (otherwise formatting fails),
     * a '$name' in the rule's template must be a property of that type: a warning, because formatting then gives empty text for it.
     * Only checked for a data type without subtypes, as the property may be on a subtype otherwise.
     */
    private fun checkRuleTypes(context: SentenceContext?, def: FormatSet) {
        val typesDomains = context?.findItemsConformingTo { it.value == TypesDomain::class.simpleName }?.mapNotNull { it.item as? TypesDomain } ?: emptyList()
        if (typesDomains.isEmpty()) return // nothing to check against
        for (rule in def.rules) {
            val typeName = rule.forTypeName.possiblyQualifiedName
            val td = typesDomains.firstNotNullOfOrNull { it.findFirstDefinitionByPossiblyQualifiedNameOrNull(typeName) }
            when {
                null == td -> issueError(rule, "Type '${typeName.value}' of format rule '${typeName.value} -> ...' not found in the types of the language", null)
                td is DataType && td.subtypes.isEmpty() -> {
                    val expr = rule.formatExpression
                    if (expr is FormatExpressionTemplate) {
                        val props = td.type().allResolvedProperty
                        expr.content.filterIsInstance<TemplateElementExpressionProperty>().forEach { el ->
                            if (null == props[PropertyName(el.propertyName)]) {
                                issueWarn(rule, "Format rule for '${typeName.value}': type '${td.name.value}' has no property '${el.propertyName}', so '$${el.propertyName}' gives no text", null)
                            }
                        }
                    }
                }

                else -> Unit
            }
        }
    }

    private fun checkGrammarExistsAndResolve(context: SentenceContext?, ref: FormatSetReference) {
        val g = context?.let { findFormatSetOrNull(it, ref.localNamespace.qualifiedName, ref.nameOrQName) }
        if (null == g) {
            this.issueError(ref, "FormatSet '${ref.nameOrQName.value}' to extend not found", null)
        } else {
            ref.resolveAs(g)
        }
    }

}