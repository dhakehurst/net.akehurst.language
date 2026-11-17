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

package net.akehurst.language.agl.completionProvider

import net.akehurst.language.agl.runtime.structure.RulePositionRuntime
import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleRhsGoal
import net.akehurst.language.api.processor.*
import net.akehurst.language.api.processor.Spine
import net.akehurst.language.api.semanticAnalyser.SentenceContext
import net.akehurst.language.asmTransform.api.AsmTransformRuleSet
import net.akehurst.language.base.api.PossiblyQualifiedName
import net.akehurst.language.base.api.QualifiedName
import net.akehurst.language.expressions.api.CreateObjectExpression
import net.akehurst.language.reference.api.CrossReferenceDomain
import net.akehurst.language.reference.api.ReferenceExpressionProperty
import net.akehurst.language.types.api.TypesDomain
import net.akehurst.language.grammar.api.*
import net.akehurst.language.grammar.asm.NonTerminalDefault
import net.akehurst.language.parser.api.RuntimeSpine
import net.akehurst.language.regex.agl.RegexValueProvider
import kotlin.collections.flatMap
import kotlin.text.isBlank

internal abstract class SpineNodeAbstract(
    val ruleItem: RuleItem,
    override val nextChildNumber: Int
) : SpineNode {
    override val rule: GrammarRule get() = ruleItem.owningRule
    override val nextExpectedItems: Set<RuleItem>
        get() = ruleItem.itemsForChild(nextChildNumber)

    //override val expectedNextLeafNonTerminalOrTerminal: Set<TangibleItem> get() = nextExpectedItems.flatMap { it.firstTangibleRecursive }.toSet()
    override val nextExpectedConcatenation: Set<Concatenation> get() = nextExpectedItems.flatMap { it.firstConcatenationRecursive }.toSet()

    override fun toString(): String = "($ruleItem)[$nextChildNumber]"
}

internal class SpineNodeRoot(
    private val _rootRuleItem: RuleItem
) : SpineNode {
    override val nextChildNumber: Int get() = 0
    override val rule: GrammarRule get() = _rootRuleItem.owningRule
    override val nextExpectedItems: Set<RuleItem> get() = setOf(_rootRuleItem)

    //override val expectedNextLeafNonTerminalOrTerminal: Set<TangibleItem> get() = nextExpectedItems.flatMap { it.firstTangibleRecursive }.toSet()
    override val nextExpectedConcatenation: Set<Concatenation> get() = nextExpectedItems.flatMap { it.firstConcatenationRecursive }.toSet()
    override fun toString(): String = "GOAL"
}

internal class SpineNodeDefault(
    _ruleItem: RuleItem,
    _index: Int
) : SpineNodeAbstract(_ruleItem, _index) {
}

internal class SpineDefault(
    private val runtimeSpine: RuntimeSpine,
    val mapToGrammar: (Int, Int) -> RuleItem?,
    override val targetGrammar: Grammar? = null
) : Spine {

    override val expectedNextLeafNonTerminalOrTerminal: Set<TangibleItem> by lazy {
        //elements[0].expectedNextLeafNonTerminalOrTerminal
        //TODO: use the above but stop it recursing for ever
        runtimeSpine.expectedNextTerminals.mapNotNull {
            val rr = it as RuntimeRule
            val orig = mapToGrammar(rr.ruleSetNumber, rr.number)
            when (orig) {
                is TangibleItem -> orig as TangibleItem
                else -> orig?.let {
                    NonTerminalDefault(orig.owningRule.grammar.selfReference, orig.owningRule.name).also { nt ->
                        nt.setOwningRule(orig.owningRule, emptyList()) // don't really want to do this, but needed because later owningRule is accessed to determine if isSkip
                    }
                }
            }
        }.toSet()
    }

    override val expectedNextRuleItems: Set<RuleItem> by lazy {
        elements[0].nextExpectedItems
    }

    override val elements: List<SpineNode> by lazy {
        runtimeSpine.elements.map {
            val rp = it as RulePositionRuntime
            when {
                rp.isGoal -> {
                    val rr = (rp.rule.rhs as RuntimeRuleRhsGoal).userGoalRuleItem
                    val gr = mapToGrammar.invoke(rr.ruleSetNumber, rr.number) ?: error("No Grammar Rule item for root runtime-rule '$rr'")
                    SpineNodeRoot(gr)
                }

                else -> {
                    val rr = it.rule as RuntimeRule
                    val ri = mapToGrammar(rr.ruleSetNumber, rr.number) ?: error("No Grammar Rule item for runtime-rule '$rr'")
                    SpineNodeDefault(ri, it.position)
                }
            }
        }
    }

    override val nextChildNumber get() = runtimeSpine.nextChildNumber

    //FIXME:hashcode/equals !

    override fun toString(): String = "Spine ${elements.joinToString(separator = "->") { it.toString() }}"
}

data class Expansion(
    val ruleItem: RuleItem,
    val name: String?,
    val list: String
)

abstract class CompletionProviderAbstract<AsmType : Any, ContextType : Any> : CompletionProvider<AsmType, ContextType> {

    companion object {

        fun defaultSortAndFilter(items: List<CompletionItem>): List<CompletionItem> {
            val grouped = items.groupBy { it.text }
            val filtered = grouped.map {
                val list = it.value
                when (list.size) {
                    0 -> error("should not happen")
                    1 -> list.first()
                    else -> list.firstOrNull { it.kind == CompletionItemKind.REFERRED }
                        ?: list.firstOrNull { it.kind == CompletionItemKind.LITERAL }
                        ?: list.firstOrNull { it.kind == CompletionItemKind.PATTERN }
                        ?: list.first()
                }
            }
            // stable sort, so items with the same kind and label stay in expansion order
            val sorted = filtered.sortedWith(
                compareByDescending(CompletionItem::kind)
                    .thenBy(CompletionItem::label)
            )
            return sorted
        }

        /**
         * The key used by [provideReferencesForSpine] for a leaf item expected next:
         * the name of the leaf rule, or the id of a literal terminal.
         */
        fun referenceKeyFor(tangible: TangibleItem): String = when {
            tangible.owningRule.isLeaf -> tangible.owningRule.name.value
            tangible is Terminal -> tangible.id
            tangible is NonTerminal -> tangible.ruleReference.value
            else -> tangible.toString()
        }

        /**
         * Items that refer to elements in the [sentenceContext], for the leaf items expected next in the [spine].
         *
         * - the property a leaf item gives a value to comes from the asm-transform rule of the grammar rule that holds the leaf:
         *   its assignment for the child number of the leaf.
         *   When that rule's transform rule does not create an object (e.g. `context = 'in' IDENTIFIER` transformed to a String),
         *   the value is the value of the rule, so the assignment of the rule that holds it (for its child number) is used, and so on outwards
         *   (rules from the grammar, and rules of the spine). A rule with no transform rule stops the search.
         * - what that property refers to comes from the [crossReferenceDomain] ('property <p> refers-to <T>'),
         *   or, when given in an enclosing type (a navigation 'a.p', or a 'forall'), from the reference recorded for the owner of the last property
         * - an element of the context is offered when its type conforms to a referred to type (both types in the [typesDomain]),
         *   otherwise when its type name is the referred to name (a simple name matches the last part of the type name)
         * - an element is offered for a leaf only when its name matches the leaf (pattern or literal value), so it is valid there
         *
         * The key of the result is [referenceKeyFor] the leaf, only leaves with at least one item are in the result.
         */
        fun provideReferencesForSpine(
            spine: Spine,
            sentenceContext: SentenceContext,
            transformRuleSet: AsmTransformRuleSet,
            typesDomain: TypesDomain,
            crossReferenceDomain: CrossReferenceDomain
        ): Map<String, List<CompletionItem>> {
            val result = linkedMapOf<String, MutableList<CompletionItem>>()
            val visited = mutableSetOf<GrammarRule>()

            fun matchesType(itemTypeName: QualifiedName, refersTo: PossiblyQualifiedName): Boolean {
                val refType = typesDomain.findFirstDefinitionByPossiblyQualifiedNameOrNull(refersTo)
                val itemType = typesDomain.findByQualifiedNameOrNull(itemTypeName)
                return when {
                    null != refType && null != itemType -> itemType.conformsTo(refType)
                    refersTo is QualifiedName -> refersTo == itemTypeName
                    else -> refersTo.simpleName == itemTypeName.last
                }
            }

            fun matchesLeaf(text: String, leaf: Terminal): Boolean = when {
                leaf.isPattern -> runCatching { Regex(leaf.unescapedValue.value).matches(text) }.getOrDefault(true)
                else -> leaf.unescapedValue.value == text
            }

            // path: (rule, child number in that rule), innermost first
            fun lookup(path: List<Pair<GrammarRule, Int>>, leafKey: String, leaf: Terminal) {
                var found: Pair<CreateObjectExpression, Int>? = null
                var ownerTypeName: QualifiedName? = null
                for ((rule, childNumber) in path) {
                    val tr = transformRuleSet.findAllTrRuleForGrammarRuleNamedOrNull(rule.name) ?: return
                    val expr = tr.expression
                    if (expr is CreateObjectExpression) {
                        found = Pair(expr, childNumber)
                        ownerTypeName = tr.resolvedType.resolvedDefinition.qualifiedName
                        break
                    }
                }
                val (expr, childNumber) = found ?: return
                val propertyName = expr.propertyAssignments.firstOrNull { it.lhsGrammarRuleIndex == childNumber }?.variable?.name ?: return
                val owner = ownerTypeName ?: return
                val refersTo = crossReferenceDomain.referencesFor(typesDomain, owner)
                    .flatMap { it.candidates }
                    .filterIsInstance<ReferenceExpressionProperty>()
                    .filter { it.referringPropertyNavigation.asString() == propertyName }
                    .flatMap { it.refersToTypeName }
                    // a reference given in an enclosing type (e.g. 'property tableRef.ref' or in a 'forall'),
                    // recorded for the type that owns the last property of the navigation
                    .ifEmpty { crossReferenceDomain.referenceForProperty(owner, propertyName) }
                if (refersTo.isEmpty()) return
                val items = sentenceContext.findItemsConformingTo { itemTypeName -> refersTo.any { matchesType(itemTypeName, it) } }
                    .filter { matchesLeaf(it.referableName, leaf) }
                    .map { CompletionItem(CompletionItemKind.REFERRED, it.qualifiedTypeName.last.value, it.referableName) }
                if (items.isNotEmpty()) result.getOrPut(leafKey) { mutableListOf() }.addAll(items)
            }

            fun canBeEmpty(item: RuleItem): Boolean = when (item) {
                is OptionalItem -> true
                is ListOfItems -> 0 == item.min
                else -> false
            }

            lateinit var walkRhs: (List<Pair<GrammarRule, Int>>, GrammarRule, RuleItem) -> Unit
            // outer: the rules (and child numbers) that hold 'rule', innermost first
            fun walkItem(outer: List<Pair<GrammarRule, Int>>, rule: GrammarRule, childNumber: Int, item: RuleItem) {
                val path = listOf(Pair(rule, childNumber)) + outer
                when (item) {
                    is Terminal -> if (item.isLiteral) lookup(path, item.id, item)
                    is NonTerminal -> {
                        val ref = referencedRuleFor(item, spine.targetGrammar)
                        when {
                            ref.isLeaf -> lookup(path, ref.name.value, ref.compressedLeaf)
                            visited.add(ref) -> walkRhs(path, ref, ref.rhs)
                            else -> Unit
                        }
                    }

                    is OptionalItem -> walkItem(outer, rule, childNumber, item.item)
                    is ListOfItems -> walkItem(outer, rule, childNumber, item.item)
                    is Choice, is Concatenation -> walkRhs(outer, rule, item)
                    else -> Unit // e.g. a group is a child of its own
                }
            }
            walkRhs = { outer, rule, rhs ->
                when (rhs) {
                    is Choice -> rhs.alternative.forEach { walkRhs(outer, rule, it) }
                    is Concatenation -> for ((idx, it) in rhs.items.withIndex()) {
                        walkItem(outer, rule, idx, it)
                        if (canBeEmpty(it).not()) break
                    }

                    else -> walkItem(outer, rule, 0, rhs)
                }
            }

            // the rules of the spine (innermost first) with the child number expected next,
            // nodes for pseudo rules (optional, list, group) are left out, the node of the rule that holds them gives the child number
            val spineNodes = spine.elements.mapNotNull { node ->
                when {
                    node is SpineNodeAbstract && node.ruleItem !== node.rule.rhs -> null
                    else -> Pair(node.rule, node.nextChildNumber)
                }
            }

            fun nullable(item: RuleItem): Boolean = when (item) {
                is OptionalItem -> true
                is ListOfItems -> 0 == item.min
                is EmptyRule -> true
                else -> false
            }

            // (child number, item) for the children of the rule that can be first
            fun firstChildren(rhs: RuleItem): List<Pair<Int, RuleItem>> = when (rhs) {
                is Choice -> rhs.alternative.flatMap { firstChildren(it) }
                is Concatenation -> {
                    val res = mutableListOf<Pair<Int, RuleItem>>()
                    for ((idx, it) in rhs.items.withIndex()) {
                        res.add(Pair(idx, it))
                        if (nullable(it).not()) break
                    }
                    res
                }

                else -> listOf(Pair(0, rhs))
            }

            // the non-terminals that can be first in the item
            fun firstNonTerminals(item: RuleItem): List<NonTerminal> = when (item) {
                is NonTerminal -> listOf(item)
                is OptionalItem -> firstNonTerminals(item.item)
                is ListOfItems -> firstNonTerminals(item.item)
                is Group -> firstNonTerminals(item.groupedContent)
                is Choice, is Concatenation -> firstChildren(item).flatMap { firstNonTerminals(it.second) }
                else -> emptyList()
            }

            // the items that are the child 'childNumber' of the rhs
            fun itemsAt(rhs: RuleItem, childNumber: Int): List<RuleItem> = when (rhs) {
                is Choice -> rhs.alternative.flatMap { itemsAt(it, childNumber) }
                is Concatenation -> listOfNotNull(rhs.items.getOrNull(childNumber))
                else -> if (0 == childNumber) listOf(rhs) else emptyList()
            }

            // A rule whose first child is being parsed is not (yet) in the spine (left corner),
            // these are the chains of such rules (innermost first, with the child number) from the item down to the target rule
            fun chainsTo(item: RuleItem, target: GrammarRule, done: MutableSet<GrammarRule>): List<List<Pair<GrammarRule, Int>>> =
                firstNonTerminals(item).flatMap { nt ->
                    val ref = referencedRuleFor(nt, spine.targetGrammar)
                    when {
                        ref == target -> listOf(emptyList())
                        ref.isLeaf -> emptyList()
                        done.add(ref) -> firstChildren(ref.rhs).flatMap { (k, child) -> chainsTo(child, target, done).map { it + Pair(ref, k) } }
                        else -> emptyList()
                    }
                }

            // the paths (innermost first) of rules and child numbers from the innermost spine node outwards
            var paths: List<List<Pair<GrammarRule, Int>>> = listOfNotNull(spineNodes.firstOrNull()?.let { listOf(it) })
            // lookup uses a path only up to the first rule that creates an object or has no transform rule, so the paths stop there
            fun stops(rule: GrammarRule): Boolean = transformRuleSet.findAllTrRuleForGrammarRuleNamedOrNull(rule.name)
                .let { null == it || it.expression is CreateObjectExpression }
            for (i in 1 until spineNodes.size) {
                if (paths.all { p -> p.any { stops(it.first) } }) break
                val inner = spineNodes[i - 1].first
                val outer = spineNodes[i]
                val chains = itemsAt(outer.first.rhs, outer.second).flatMap { chainsTo(it, inner, mutableSetOf()) }.ifEmpty { listOf(emptyList()) }
                paths = paths.flatMap { p -> chains.map { c -> p + c + outer } }
            }

            for (path in paths) {
                val first = path.first()
                visited.clear()
                spine.expectedNextRuleItems.forEach { walkItem(path.drop(1), first.first, first.second, it) }
            }
            return result
        }

        /**
         * The default items for the [spine] ([provideDefaultForSpine]) together with the items from [provideReferencesForSpine]:
         * the default PATTERN and LITERAL items for a leaf that has referred to items are left out (they are replaced by the referred to items).
         * With no [sentenceContext], only the default items.
         */
        fun <ContextType : Any> provideDefaultWithReferencesForSpine(
            spine: Spine,
            options: CompletionProviderOptions<ContextType>,
            sentenceContext: SentenceContext?,
            transformRuleSet: AsmTransformRuleSet,
            typesDomain: TypesDomain,
            crossReferenceDomain: CrossReferenceDomain
        ): List<CompletionItem> {
            val default = provideDefaultForSpine(spine, options)
            return when (sentenceContext) {
                null -> default
                else -> {
                    val refs = provideReferencesForSpine(spine, sentenceContext, transformRuleSet, typesDomain, crossReferenceDomain)
                    // a segment that is only the placeholder of such a leaf (e.g. '<IDENTIFIER>') is left out too
                    val placeholders = refs.keys.map { "<$it>" }.toSet()
                    refs.values.flatten() + default.filter { item ->
                        val d = item.detail
                        val replaced = when (item.kind) {
                            CompletionItemKind.PATTERN, CompletionItemKind.LITERAL -> d is TangibleItem && referenceKeyFor(d) in refs.keys
                            CompletionItemKind.SEGMENT -> item.text in placeholders
                            else -> false
                        }
                        replaced.not()
                    }
                }
            }
        }

        // id is the index in [expansions] (used by CompletionProviderOptions.path), empty segments are not offered
        fun expansionToCompletionItem(expansions: List<Expansion>): List<CompletionItem> = expansions.mapIndexedNotNull { idx, exp ->
            when {
                exp.list.isBlank() -> null
                else -> {
                    val label = exp.name ?: "???"
                    val text = exp.list//.joinToString(separator = " ") { textFor(it) }
                    CompletionItem(CompletionItemKind.SEGMENT, label, text).also {
                        it.id = idx
                    }
                }
            }
        }.toSet().toList()

        /**
         * the rule referred to by [item].
         * Resolved in [targetGrammar] when the rule owning [item] is from it, or from a grammar it extends, so that overridden rules are used.
         * Otherwise (e.g. no target grammar, or an embedded grammar), resolved in the grammar owning [item].
         */
        fun referencedRuleFor(item: NonTerminal, targetGrammar: Grammar?): GrammarRule {
            val owningGrammar = item.owningRule.grammar
            val tg = targetGrammar?.takeIf { it == owningGrammar || it.allExtendsResolved.contains(owningGrammar) }
            return tg?.let { item.referencedRuleOrNull(it) } ?: item.referencedRule(owningGrammar)
        }

        fun <ContextType : Any> provideDefaultForSpine(spine: Spine, options: CompletionProviderOptions<ContextType>): List<CompletionItem> {
            val exps = provideForRuleItems(spine.expectedNextRuleItems, options, spine.targetGrammar)
            val cis = expansionToCompletionItem(exps) + provideForTangibles(spine.expectedNextLeafNonTerminalOrTerminal, options, spine.targetGrammar)
            return cis
        }

        fun <ContextType : Any> provideExpansionsForSpine(spine: Spine, options: CompletionProviderOptions<ContextType>): List<Expansion> {
            return provideForRuleItems(spine.expectedNextRuleItems, options, spine.targetGrammar)
        }

        fun <ContextType : Any> provideForConcatenations(concatenations: Set<Concatenation>, options: CompletionProviderOptions<ContextType>, targetGrammar: Grammar? = null): List<Expansion> = when (options.depth) {
            0 -> emptyList()
            else -> concatenations.flatMap { concat ->
                expand(options.depth, concat, options, targetGrammar)
            }
        }

        fun <ContextType : Any> provideForRuleItems(ruleItems: Set<RuleItem>, options: CompletionProviderOptions<ContextType>, targetGrammar: Grammar? = null): List<Expansion> =
            provideExpansionsForRuleItem(options.depth, ruleItems, options, targetGrammar)

        fun <ContextType : Any> provideExpansionsForRuleItem(curDepth: Int, ruleItems: Set<RuleItem>, options: CompletionProviderOptions<ContextType>, targetGrammar: Grammar? = null): List<Expansion> =
            ruleItems.flatMap { ri ->
                expand(curDepth, ri, options, targetGrammar)
            }

        fun <ContextType : Any> expand(curDepth: Int, ri: RuleItem, options: CompletionProviderOptions<ContextType>, targetGrammar: Grammar? = null): List<Expansion> =
            expandItem(curDepth, ri, options, targetGrammar).map { exp ->
                if (null == exp.name) {
                    Expansion(exp.ruleItem, ri.owningRule.name.value, exp.list)
                } else {
                    exp
                }
            }


        fun <ContextType : Any> expandItem(curDepth: Int, item: RuleItem, options: CompletionProviderOptions<ContextType>, targetGrammar: Grammar? = null): List<Expansion> = when (item) {
            is Choice -> item.alternative.flatMap { expandItem(curDepth, it, options, targetGrammar) }
            is Concatenation -> {
                //FIXME: SPACE may not be valid skip rule !
                // label (Expansion.name): the innermost rule whose right-hand side holds the whole segment.
                // A segment of one part keeps the name of that part, a segment of more parts gets its name from the enclosing rule.
                var results = listOf<Expansion>(Expansion(item, null, ""))
                for (concItem in item.items) {
                    val expand = expandItem(curDepth, concItem, options, targetGrammar)
                    // earlier items vary slowest, so the segments are in the order of the alternatives
                    val newResults = results.flatMap { res ->
                        expand.map { exp ->
                            when {
                                exp.list.isBlank() -> res // do not concat blank/empty items
                                res.list.isBlank() -> Expansion(exp.ruleItem, exp.name, exp.list) // first item
                                // more than one part, so the segment is the concatenation (used when it is expanded further, see CompletionProviderOptions.path)
                                else -> Expansion(item, null, "${res.list} ${exp.list}")
                            }
                        }
                    }
                    results = newResults
                }
                results
            }

            is ConcatenationItem -> when (item) {
                is OptionalItem -> when {
                    options.showOptionalItems -> expandItem(curDepth, item.item, options, targetGrammar) + Expansion(item, null, "")
                    else -> listOf(Expansion(item, null, ""))
                }
                is SimpleList -> {
                    // the minimum number of items, and also one item if the minimum is 0
                    val itemExps = expandItem(curDepth, item.item, options, targetGrammar)
                    when {
                        0 != item.min -> repeatExpansions(item, itemExps, item.min)
                        options.showOptionalItems -> listOf(Expansion(item, null, "")) + itemExps
                        else -> listOf(Expansion(item, null, ""))
                    }
                }

                is SeparatedList -> {
                    // the minimum number of items (also one item if the minimum is 0),
                    // and 'item sep item' if the maximum allows 2 items, so that the separator is shown
                    val itemExps = expandItem(curDepth, item.item, options, targetGrammar)
                    val sepExps = expandItem(curDepth, item.separator, options, targetGrammar)
                    val counts = when {
                        0 != item.min -> listOf(item.min)
                        options.showOptionalItems -> listOf(0, 1)
                        else -> listOf(0)
                    }
                    val withTwo = when {
                        counts == listOf(0) -> counts
                        counts.any { it >= 2 } -> counts
                        -1 == item.max || item.max >= 2 -> counts + 2
                        else -> counts
                    }
                    withTwo.flatMap { n ->
                        when (n) {
                            0 -> listOf(Expansion(item, null, ""))
                            else -> repeatExpansions(item, itemExps, n, sepExps)
                        }
                    }
                }

                is ListOfItems -> expandItem(curDepth, item.item, options, targetGrammar)
                is SimpleItem -> when (item) {
                    is Group -> expandItem(curDepth, item.groupedContent, options, targetGrammar)
                    is TangibleItem -> when (item) {
                        is EmptyRule -> listOf(textFor(item, options))
                        is Terminal -> listOf(textFor(item, options))
                        // expanded like a rule reference, its rule (and the rules it refers to) resolved in the embedded grammar
                        is Embedded -> when (val ruleDepth = options.ruleDepth[item.embeddedGoalName.value] ?: curDepth) {
                            0 -> listOf(textFor(item, options))
                            else -> {
                                val embeddedGrammar = item.embeddedGrammarReference.resolved
                                val embeddedRule = embeddedGrammar?.findAllResolvedGrammarRule(item.embeddedGoalName)
                                when {
                                    null == embeddedGrammar || null == embeddedRule || embeddedRule.isLeaf -> listOf(textFor(item, options))
                                    else -> expandItem(ruleDepth - 1, embeddedRule.rhs, options, embeddedGrammar).map { Expansion(it.ruleItem, it.name ?: item.embeddedGoalName.value, it.list) }
                                }
                            }
                        }
                        // a depth given for the rule (options.ruleDepth) is used instead of the current depth
                        is NonTerminal -> when (val ruleDepth = options.ruleDepth[item.ruleReference.value] ?: curDepth) {
                            0 -> listOf(textFor(item, options))
                            else -> {
                                val refRule = referencedRuleFor(item, targetGrammar)
                                when {
                                    refRule.isLeaf -> listOf(textFor(item, options))
                                    else -> {
                                        expandItem(ruleDepth - 1, refRule.rhs, options, targetGrammar).map { Expansion(it.ruleItem, it.name ?: item.ruleReference.value, it.list) }
                                    }
                                }
                            }
                        }

                        else -> error("Unsupported subtype of TangibleItem: ${item::class.simpleName}")
                    }

                    else -> error("Unsupported subtype of SimpleItem: ${item::class.simpleName}")
                }

                else -> error("Unsupported subtype of ConcatenationItem: ${item::class.simpleName}")
            }

            else -> error("Unsupported subtype of RuleItem: ${item::class.simpleName}")
        }

        /**
         * all combinations of [count] items, each one of [itemExpansions], separated by a space
         * (and by one of [separatorExpansions] if there are any).
         * A combination of more than one item refers to [list].
         */
        fun repeatExpansions(list: ListOfItems, itemExpansions: List<Expansion>, count: Int, separatorExpansions: List<Expansion> = emptyList()): List<Expansion> {
            fun join(res: Expansion, exp: Expansion) = when {
                exp.list.isBlank() -> res
                res.list.isBlank() -> exp
                else -> Expansion(list, null, "${res.list} ${exp.list}") // more than one part, named by the enclosing rule
            }
            var results = itemExpansions
            for (i in 1 until count) {
                if (separatorExpansions.isNotEmpty()) {
                    results = results.flatMap { res -> separatorExpansions.map { sep -> join(res, sep) } }
                }
                results = results.flatMap { res -> itemExpansions.map { exp -> join(res, exp) } }
            }
            return results
        }

        fun <ContextType : Any> textFor(item: RuleItem, options: CompletionProviderOptions<ContextType>): Expansion = when (item) {
            is Choice -> Expansion(item, null, "<${item.alternative.joinToString(separator = "|") { textFor(it, options).list }}>")
            is Concatenation -> Expansion(item, null, item.items.joinToString(separator = " ") { textFor(it, options).list }) //FIXME: SPACE may not be valid skip rule !
            is ConcatenationItem -> when (item) {
                is OptionalItem -> Expansion(item, null, textFor(item.item, options).list + "?")
                is ListOfItems -> textFor(item.item, options)
                is SimpleItem -> when (item) {
                    is Group -> textFor(item.groupedContent, options)
                    is TangibleItem -> when (item) {
                        is EmptyRule -> Expansion(item, null, "")
                        is Terminal -> when {
                            item.owningRule.isLeaf -> Expansion(item, null, "<${item.owningRule.name}>")
                            item.isPattern -> when {
                                options.provideValuesForPatternTerminals -> try {
                                    val p = RegexValueProvider(item.unescapedValue.value, 'X') //TODO: pass in the 'any value'
                                    val txt = p.provide()
                                    Expansion(item, null, txt)
                                } catch (t: Throwable) {
                                    t.printStackTrace() // should not happen, but bugs in AGL regex could cause it
                                    Expansion(item, null, "<${item.unescapedValue.value}>")
                                }

                                else -> Expansion(item, null, "<${item.unescapedValue.value}>")
                            }

                            else -> Expansion(item, null, item.unescapedValue.value)
                        }

                        is NonTerminal -> Expansion(item, item.ruleReference.value, "<${item.ruleReference.value}>")
                        is Embedded -> Expansion(item, null, "<${item.embeddedGrammarReference.nameOrQName.simpleName.value}::${item.embeddedGoalName.value}>")
                        else -> error("Unsupported subtype of TangibleItem: ${item::class.simpleName}")
                    }

                    else -> error("Unsupported subtype of SimpleItem: ${item::class.simpleName}")
                }

                else -> error("Unsupported subtype of ConcatenationItem: ${item::class.simpleName}")
            }

            else -> error("Unsupported subtype of RuleItem: ${item::class.simpleName}")
        }

        fun <ContextType : Any> provideForTangibles(tangibles: Set<TangibleItem>, options: CompletionProviderOptions<ContextType>, targetGrammar: Grammar? = null): List<CompletionItem> {
            return tangibles.flatMap { ri ->
                when {
                    ri.owningRule.isSkip -> emptyList() //make this an option to exclude skip stuff, this also needs to be extended/improved does not cover all cases
                    else -> provideForTangible(ri, options, targetGrammar)
                }
            }
        }

        // maybe should pass in a GrammarRule rather than TangibleItem
        fun <ContextType : Any> provideForTangible(tangibleItem: TangibleItem, options: CompletionProviderOptions<ContextType>, targetGrammar: Grammar? = null): List<CompletionItem> {
            return when (tangibleItem) {
                // Empty -> provides nothing
                // Embedded -> should not occur in spine as the embedded item is used.

                is NonTerminal -> { //must be a reference to leaf
                    val name = tangibleItem.ruleReference.value
                    val refRule = referencedRuleFor(tangibleItem, targetGrammar)
                    // label is the rule name, the description holds the pattern (regex)
                    val pattern = refRule.compressedLeaf.unescapedValue.value
                    listOf(CompletionItem(CompletionItemKind.PATTERN, name, "<$name>").apply { detail = tangibleItem; description = pattern })
                }

                is Terminal -> {
                    val name = when {
                        tangibleItem.owningRule.isLeaf -> tangibleItem.owningRule.name.value
                        else -> tangibleItem.escapedValue.value
                    }
                    // a pattern: label is the rule name, the description holds the pattern (regex)
                    when {
                        tangibleItem.isPattern -> when {
                            options.provideValuesForPatternTerminals -> try {
                                val p = RegexValueProvider(tangibleItem.unescapedValue.value, 'X') //TODO: pass in the 'any value'
                                val txt = p.provide()
                                listOf(CompletionItem(CompletionItemKind.PATTERN, name, txt).apply { detail = tangibleItem; description = tangibleItem.unescapedValue.value })
                            } catch (t: Throwable) {
                                t.printStackTrace() // should not happen, but bugs in AGL regex could cause it
                                listOf(CompletionItem(CompletionItemKind.PATTERN, name, "<$name>").apply { detail = tangibleItem; description = tangibleItem.unescapedValue.value })
                            }

                            else -> listOf(CompletionItem(CompletionItemKind.PATTERN, name, "<$name>").apply { detail = tangibleItem; description = tangibleItem.unescapedValue.value })
                        }

                        else -> listOf(CompletionItem(CompletionItemKind.LITERAL, "'$name'", tangibleItem.unescapedValue.value).apply { detail=tangibleItem })
                    }
                }

                else -> error("Not supported subtype of TangibleItem: ${tangibleItem::class.simpleName}")
            }
        }
    }

    override fun provide(nextExpected: Set<Spine>, options: CompletionProviderOptions<ContextType>): List<CompletionItem> {
        val items = nextExpected.flatMap { sp ->
            provideDefaultForSpine(sp, options)
        }
        return defaultSortAndFilter(items)
    }

//    private fun provideForRuleItem(desiredDepth: Int, item: RuleItem, options: CompletionProviderOptions<ContextType>): List<CompletionItem> {
//        val rule = item.owningRule
//        return when {
//            rule.isLeaf -> listOf(
//                CompletionItem(CompletionItemKind.PATTERN, rule.compressedLeaf.unescapedValue.value, "<${rule.name}>",rule)
//            )
//
//            else -> {
//                val cis = getItems(desiredDepth, item, options, emptySet())
//                cis.mapNotNull { it }.toSet().toList()
//            }
//        }
//    }

    // uses null to indicate that there is an empty item
    private fun getItems(depth: Int, item: RuleItem, options: CompletionProviderOptions<ContextType>, done: Set<RuleItem>): List<CompletionItem?> {
        //TODO: use scope to add real items to this list - maybe in a subclass
        return when {
            done.contains(item) -> emptyList()
            else -> when (item) {
                is EmptyRule -> listOf(null)
                is Choice -> item.alternative.flatMap { getItems(depth, it, options, done + item) }
                is Concatenation -> {
                    var items = getItems(depth, item.items[0], options, done + item)
                    var index = 1
                    while (index < item.items.size && items.any { it == null }) {
                        items = items.mapNotNull { it } + getItems(depth, item.items[index], options, done + item)
                        index++
                    }
                    items
                }

                is Terminal -> provideForTangible(item, options)

                is NonTerminal -> {
                    //TODO: handle overridden vs embedded rules!
                    val refRule = item.referencedRuleOrNull(item.owningRule.grammar)
                    when (refRule) {
                        null -> emptyList()
                        else -> getItems(depth - 1, refRule.rhs, options, done + item)
                    }
                }

                is SeparatedList -> {
                    val items = getItems(depth, item.item, options, done + item)
                    if (item.min == 0) {
                        items + listOf(null)
                    } else {
                        items + emptyList<CompletionItem>()
                    }
                }

                is SimpleList -> {
                    val items = getItems(depth, item.item, options, done + item)
                    if (item.min == 0) {
                        items + listOf(null)
                    } else {
                        items + emptyList<CompletionItem>()
                    }
                }

                is Group -> getItems(depth, item.groupedContent, options, done + item)
                else -> error("not yet supported!")
            }
        }
    }

}