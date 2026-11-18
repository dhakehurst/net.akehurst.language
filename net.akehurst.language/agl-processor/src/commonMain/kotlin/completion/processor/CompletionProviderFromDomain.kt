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
import net.akehurst.language.grammar.api.ListOfItems
import net.akehurst.language.grammar.api.NonTerminal
import net.akehurst.language.grammar.api.OptionalItem
import net.akehurst.language.grammar.api.RuleItem
import net.akehurst.language.grammar.api.Terminal
import net.akehurst.language.agl.syntaxAnalyser.LocationMapDefault
import net.akehurst.language.expressions.api.Expression
import net.akehurst.language.objectgraph.api.EvaluationContext
import net.akehurst.language.expressions.processor.ExpressionsInterpreterOverTypedObjectCombined
import net.akehurst.language.expressions.processor.ObjectGraphAccessorMutatorByReflection
import net.akehurst.language.objectgraph.api.TypedObject
import net.akehurst.language.issues.api.LanguageProcessorPhase
import net.akehurst.language.issues.ram.IssueHolder
import net.akehurst.language.types.asm.StdLibDefault
import net.akehurst.language.types.builder.typesDomain

/**
 * the provider for a language processor: [inner] (null for the default items), with the completion definitions
 * of the processor (its completionDomain, from the configuration's completionString) applied to its items, when there are any.
 * (Issues found in the completion definitions are recorded by the processor when it resolves its completionDomain.)
 */
fun <AsmType : Any, ContextType : Any> completionProviderWithDefinitions(
    p: LanguageProcessor<AsmType, ContextType>,
    inner: CompletionProvider<AsmType, ContextType>?
): ProcessResult<CompletionProvider<AsmType, ContextType>> {
    val domain = p.completionDomain
    val provider = when {
        null == domain || domain.allCompletionRules.isEmpty() -> inner
        else -> CompletionProviderFromDomain(domain, inner)
    }
    return ProcessResultDefault(provider)
}

/**
 * Applies the rules of [domain] to the items of [inner] (or the default items when [inner] is null).
 *
 * A rule is for a rule name (an item is for the rule named by its label, a literal for its leaf rule),
 * or for a literal (an item is for it when it is a LITERAL item with that text).
 * A rule applies to a position when its context (if any) is a rule that encloses the position, at any level.
 * - add, replace: when the rule (forItem) is expected next (or a generated item is for it), the rule's items are added
 * - replace, hide: generated items for the rule are left out
 * - decorate: generated items for the rule get the given label, text, description
 * - depth: the rule is expanded to the given depth wherever it is expanded
 *
 * In path mode (an item chosen to be expanded further, see [CompletionProviderOptions.path]) the rules apply in the same way
 * to the items of the expansion, except that add and replace only add items when a generated item is for their rule
 * (what is expected at the position is not offered in path mode).
 *
 * The values of the items and decorations are expressions, evaluated here (see [CompletionItemDefinition]);
 * a value that does not give a String is not used (a decoration keeps the generated value, an item without a text is left out).
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

        /** the text of the literal an item is for, null if it is not a literal */
        fun literalOf(item: CompletionItem): String? = when (item.kind) {
            CompletionItemKind.LITERAL -> when (val d = item.detail) {
                is Terminal -> if (d.isLiteral) d.unescapedValue.value else null
                else -> item.text
            }

            else -> null
        }

        /** the text of an expected literal, null if it is not a literal */
        fun literalOf(item: RuleItem): String? = when (item) {
            is Terminal -> if (item.isLiteral) item.unescapedValue.value else null
            else -> null
        }

        /** true if [rule] is for [item] */
        fun isFor(rule: CompletionRule, item: CompletionItem): Boolean = when {
            rule.forLiteral -> rule.forItem == literalOf(item)
            else -> rule.forItem == ruleNameOf(item)
        }

        private val valuesTypesDomain by lazy { typesDomain("CompletionValues", true) {} }
    }

    override fun provide(nextExpected: Set<Spine>, options: CompletionProviderOptions<ContextType>): List<CompletionItem> {
        val rules = domain.allCompletionRules
        return when {
            rules.isEmpty() -> provideInner(nextExpected, options)
            options.path.isNotEmpty() -> {
                // the path indexes the expansions of all spines together, so the items are generated for all of them at once
                val enclosing = nextExpected.flatMap { sp -> sp.elements.map { it.rule.name.value } }
                val inContext = rules.filter { null == it.context || it.context in enclosing }
                val items = applyRules(nextExpected, inContext, emptySet(), emptySet(), enclosing.distinct(), options)
                CompletionProviderAbstract.defaultSortAndFilter(items.distinct())
            }

            else -> {
                val items = nextExpected.flatMap { spine ->
                    val enclosing = spine.elements.map { it.rule.name.value }
                    val inContext = rules.filter { null == it.context || it.context in enclosing }
                    val allExpected = spine.expectedNextRuleItems + spine.expectedNextLeafNonTerminalOrTerminal
                    val expectedRules = allExpected.mapNotNull { ruleNameOf(it) }.toSet()
                    val expectedLiterals = allExpected.mapNotNull { literalOf(it) }.toSet()
                    applyRules(setOf(spine), inContext, expectedRules, expectedLiterals, enclosing, options)
                }
                CompletionProviderAbstract.defaultSortAndFilter(items.distinct())
            }
        }
    }

    private fun provideInner(nextExpected: Set<Spine>, options: CompletionProviderOptions<ContextType>): List<CompletionItem> =
        inner?.provide(nextExpected, options)
            ?: CompletionProviderAbstract.defaultSortAndFilter(nextExpected.flatMap { CompletionProviderAbstract.provideDefaultForSpine(it, options) })

    /**
     * the generated items for [spines], with the rules [inContext] applied.
     * [expectedRules], [expectedLiterals] are what is expected next (rules that add items apply when their rule is expected, or a generated item is for it),
     * [enclosing] are the names of the rules that enclose the position, innermost first.
     */
    private fun applyRules(
        spines: Set<Spine>,
        inContext: List<CompletionRule>,
        expectedRules: Set<String>,
        expectedLiterals: Set<String>,
        enclosing: List<String>,
        options: CompletionProviderOptions<ContextType>
    ): List<CompletionItem> {
        val depths = inContext.filter { !it.forLiteral }.mapNotNull { r -> (r.action as? CompletionActionDepth)?.let { Pair(r.forItem, it.depth) } }.toMap()
        val opts = when {
            depths.isEmpty() -> options
            else -> options.clone().also { it.ruleDepth = options.ruleDepth + depths }
        }
        val generated = provideInner(spines, opts)
        val evaluator = ValueEvaluator(enclosing)

        val kept = generated.filterNot { item -> inContext.any { (it.action is CompletionActionHide || it.action is CompletionActionReplace) && isFor(it, item) } }
        val decorated = kept.map { item ->
            val decs = inContext.filter { it.action is CompletionActionDecorate && isFor(it, item) }
            when {
                decs.isEmpty() -> item
                else -> decorate(item, decs, evaluator)
            }
        }
        val added = inContext.filter { r ->
            when {
                r.forLiteral -> r.forItem in expectedLiterals || generated.any { isFor(r, it) }
                else -> r.forItem in expectedRules || generated.any { isFor(r, it) }
            }
        }.flatMap { r ->
            val defs = when (val act = r.action) {
                is CompletionActionAdd -> act.items
                is CompletionActionReplace -> act.items
                else -> emptyList()
            }
            defs.mapNotNull { def -> evaluator.item(def, r) }
        }
        return decorated + added
    }

    private fun decorate(item: CompletionItem, decorations: List<CompletionRule>, evaluator: ValueEvaluator): CompletionItem {
        // later decorations win, each sees the item as decorated by the earlier ones
        var label = item.label
        var text = item.text
        var description = item.description
        for (r in decorations) {
            val dec = r.action as CompletionActionDecorate
            val self = CompletionItem(item.kind, label, text).also { it.description = description }
            label = dec.label?.let { evaluator.string(it, r, self) } ?: label
            text = dec.text?.let { evaluator.string(it, r, self) } ?: text
            description = dec.description?.let { evaluator.string(it, r, self) } ?: description
        }
        return CompletionItem(item.kind, label, text).also {
            it.id = item.id
            it.description = description
            it.detail = item.detail
            it.offeredPosition = item.offeredPosition
        }
    }

    /**
     * evaluates the expressions of item definitions and decorations.
     * `$expected` is the forItem of the completion rule, `$rules` the names of [enclosing] rules (innermost first),
     * `$self` (in a decoration) the generated item.
     */
    private class ValueEvaluator(val enclosing: List<String>) {
        private val issues = IssueHolder(LanguageProcessorPhase.ALL)
        private val objectGraph = ObjectGraphAccessorMutatorByReflection(valuesTypesDomain, issues, LocationMapDefault())
        private val interpreter = ExpressionsInterpreterOverTypedObjectCombined(objectGraph)
        private val rules: TypedObject by lazy {
            objectGraph.createCollection(StdLibDefault.List.type(listOf(StdLibDefault.String.asTypeArgument)), enclosing.map { str(it) })
        }

        private fun str(value: String): TypedObject = objectGraph.createPrimitiveValue(StdLibDefault.String.qualifiedTypeName, value)

        private fun itemValue(item: CompletionItem): TypedObject = objectGraph.createTupleValue(
            mapOf(
                "kind" to str(item.kind.name),
                "label" to str(item.label),
                "text" to str(item.text),
                "description" to str(item.description),
            )
        )

        /** the String the expression gives, null if it does not give a String */
        fun string(expression: Expression, rule: CompletionRule, self: CompletionItem?): String? {
            val named = mutableMapOf("\$expected" to str(rule.forItem), "\$rules" to rules)
            val evc = when (self) {
                null -> EvaluationContext.of(named)
                else -> EvaluationContext.ofSelf(itemValue(self), named)
            }
            val result = runCatching { interpreter.evaluateExpression(evc, expression) }.getOrNull()
            return when {
                null == result || result.isNothing -> null
                result.type.conformsTo(StdLibDefault.String) -> objectGraph.untyped(result) as? String
                else -> null
            }
        }

        /** the item for [def], null if its text does not give a String */
        fun item(def: CompletionItemDefinition, rule: CompletionRule): CompletionItem? {
            val text = string(def.text, rule, null) ?: return null
            val label = def.label?.let { string(it, rule, null) } ?: text
            val description = def.description?.let { string(it, rule, null) } ?: ""
            return CompletionItem(def.kind, label, text).also { it.description = description }
        }
    }
}
