/*
 * Copyright (C) 2026 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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
package net.akehurst.language.completion.processor

import net.akehurst.language.agl.completionProvider.CompletionProviderAbstract
import net.akehurst.language.agl.processor.ProcessResultDefault
import net.akehurst.language.api.processor.*
import net.akehurst.language.completion.api.*
import net.akehurst.language.completion.asm.AglCompletionDomainDefault
import net.akehurst.language.grammar.api.ListOfItems
import net.akehurst.language.grammar.api.NonTerminal
import net.akehurst.language.grammar.api.OptionalItem
import net.akehurst.language.grammar.api.RuleItem
import net.akehurst.language.grammar.api.Terminal
import net.akehurst.language.grammar.processor.contextFromGrammar
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder

/**
 * the provider for a language processor: [inner] (null for the default items), with the completion definitions
 * of the processor's configuration (completionString) applied to its items, when there are any.
 */
fun <AsmType : Any, ContextType : Any> completionProviderWithDefinitions(
    p: LanguageProcessor<AsmType, ContextType>,
    inner: CompletionProvider<AsmType, ContextType>?
): ProcessResult<CompletionProvider<AsmType, ContextType>> {
    val completionStr = p.configuration.completionString
    return when {
        null == completionStr || completionStr.value.isBlank() -> ProcessResultDefault(inner)
        else -> {
            val res = AglCompletionDomainDefault.fromString(p.grammarDomain?.let { contextFromGrammar(it) }, completionStr)
            val issues = IssueHolder(LanguageProcessorPhase.ALL).also { it.addAllFrom(res.allIssues) }
            val provider = res.asm?.let { CompletionProviderFromDomain(it, inner) } ?: inner
            ProcessResultDefault(provider, processIssues = issues)
        }
    }
}

/**
 * Applies the rules of [domain] to the items of [inner] (or the default items when [inner] is null).
 *
 * A rule applies to a position when its context (if any) is a rule that encloses the position, at any level.
 * - add, replace: when the rule (forItem) is expected next (or a generated item is for it), the rule's items are added
 * - replace, hide: generated items for the rule are left out (an item is for the rule named by its label, a literal for its leaf rule)
 * - decorate: generated items for the rule get the given label, text, description
 * - depth: the rule is expanded to the given depth wherever it is expanded
 */
class CompletionProviderFromDomain<AsmType : Any, ContextType : Any>(
    val domain: AglCompletionDomain,
    val inner: CompletionProvider<AsmType, ContextType>?
) : CompletionProvider<AsmType, ContextType> {

    companion object {
        /** the name of the rule an item is for, null if it is not for one */
        fun ruleNameOf(item: CompletionItem): String? = when (item.kind) {
            CompletionItemKind.SEGMENT, CompletionItemKind.PATTERN -> item.label
            CompletionItemKind.LITERAL -> (item.detail as? Terminal)?.owningRule?.let { if (it.isLeaf) it.name.value else null }
            CompletionItemKind.REFERRED -> null
        }

        /** the name of the rule an expected item is, null if it is not a rule (e.g. a literal of a non-leaf rule) */
        fun ruleNameOf(item: RuleItem): String? = when (item) {
            is NonTerminal -> item.ruleReference.value
            is Terminal -> if (item.owningRule.isLeaf) item.owningRule.name.value else null
            is ListOfItems -> ruleNameOf(item.item) // e.g. 'terminatedStatement+'
            is OptionalItem -> ruleNameOf(item.item)
            else -> null
        }
    }

    override fun provide(nextExpected: Set<Spine>, options: CompletionProviderOptions<ContextType>): List<CompletionItem> {
        val rules = domain.allCompletionRules
        return when {
            rules.isEmpty() || options.path.isNotEmpty() -> provideInner(nextExpected, options) //TODO: path mode with the rules
            else -> {
                val items = nextExpected.flatMap { spine -> provideForSpine(spine, rules, options) }
                CompletionProviderAbstract.defaultSortAndFilter(items.distinct())
            }
        }
    }

    private fun provideInner(nextExpected: Set<Spine>, options: CompletionProviderOptions<ContextType>): List<CompletionItem> =
        inner?.provide(nextExpected, options)
            ?: CompletionProviderAbstract.defaultSortAndFilter(nextExpected.flatMap { CompletionProviderAbstract.provideDefaultForSpine(it, options) })

    private fun provideForSpine(spine: Spine, rules: List<CompletionRule>, options: CompletionProviderOptions<ContextType>): List<CompletionItem> {
        val enclosing = spine.elements.map { it.rule.name.value }.toSet()
        val inContext = rules.filter { null == it.context || it.context in enclosing }
        val expected = (spine.expectedNextRuleItems + spine.expectedNextLeafNonTerminalOrTerminal).mapNotNull { ruleNameOf(it) }.toSet()

        val depths = inContext.mapNotNull { r -> (r.action as? CompletionActionDepth)?.let { Pair(r.forItem, it.depth) } }.toMap()
        val opts = when {
            depths.isEmpty() -> options
            else -> options.clone().also { it.ruleDepth = options.ruleDepth + depths }
        }
        val generated = provideInner(setOf(spine), opts)
        // also expected: rules that the generated items are for
        val expectedOrGenerated = expected + generated.mapNotNull { ruleNameOf(it) }

        val leftOut = inContext.filter { it.action is CompletionActionHide || it.action is CompletionActionReplace }.map { it.forItem }.toSet()
        val decorations = inContext.filter { it.action is CompletionActionDecorate }.groupBy({ it.forItem }, { it.action as CompletionActionDecorate })
        val kept = generated.filterNot { ruleNameOf(it) in leftOut }
        val decorated = kept.map { item ->
            when (val decs = decorations[ruleNameOf(item)]) {
                null -> item
                else -> decorate(item, decs)
            }
        }
        val added = inContext.filter { it.forItem in expectedOrGenerated }.flatMap { r ->
            when (val act = r.action) {
                is CompletionActionAdd -> act.items
                is CompletionActionReplace -> act.items
                else -> emptyList()
            }
        }.map { def ->
            CompletionItem(def.kind, def.label, def.text).also { it.description = def.description }
        }
        return decorated + added
    }

    private fun decorate(item: CompletionItem, decorations: List<CompletionActionDecorate>): CompletionItem {
        // later decorations win
        val label = decorations.lastOrNull { null != it.label }?.label ?: item.label
        val text = decorations.lastOrNull { null != it.text }?.text ?: item.text
        val description = decorations.lastOrNull { null != it.description }?.description ?: item.description
        return CompletionItem(item.kind, label, text).also {
            it.id = item.id
            it.description = description
            it.detail = item.detail
            it.offeredPosition = item.offeredPosition
        }
    }
}
