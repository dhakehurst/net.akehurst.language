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
import net.akehurst.language.grammar.api.*
import net.akehurst.language.parser.api.RuntimeSpine
import net.akehurst.language.regex.agl.RegexValueProvider

internal abstract class SpineNodeAbstract(
    val ruleItem: RuleItem,
    override val nextChildNumber: Int
) : SpineNode {
    override val rule: GrammarRule get() = ruleItem.owningRule
    override val nextExpectedItems: Set<RuleItem>
        get() = ruleItem.itemsForChild(nextChildNumber)
    override val expectedNextLeafNonTerminalOrTerminal: Set<TangibleItem> get() = nextExpectedItems.flatMap { it.firstTangibleRecursive }.toSet()
    override val nextExpectedConcatenation: Set<Concatenation> get() = nextExpectedItems.flatMap { it.firstConcatenationRecursive }.toSet()

    override fun toString(): String = "($ruleItem)[$nextChildNumber]"
}

internal class SpineNodeRoot(
    private val _rootRuleItem: RuleItem
) : SpineNode {
    override val nextChildNumber: Int get() = 0
    override val rule: GrammarRule get() = _rootRuleItem.owningRule
    override val nextExpectedItems: Set<RuleItem> get() = setOf(_rootRuleItem)
    override val expectedNextLeafNonTerminalOrTerminal: Set<TangibleItem> get() = nextExpectedItems.flatMap { it.firstTangibleRecursive }.toSet()
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
    val mapToGrammar: (Int, Int) -> RuleItem?
) : Spine {

    override val expectedNextLeafNonTerminalOrTerminal: Set<TangibleItem> by lazy {
        //elements[0].expectedNextLeafNonTerminalOrTerminal
        //TODO: use the above but stop it recursing for ever
        runtimeSpine.expectedNextTerminals.map {
            val rr = it as RuntimeRule
            mapToGrammar(rr.runtimeRuleSetNumber, rr.ruleNumber) as TangibleItem
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
                    val gr = mapToGrammar.invoke(rr.runtimeRuleSetNumber, rr.ruleNumber) ?: error("No Grammar Rule item for root runtime-rule '$rr'")
                    SpineNodeRoot(gr)
                }

                else -> {
                    val rr = it.rule as RuntimeRule
                    val ri = mapToGrammar(rr.runtimeRuleSetNumber, rr.ruleNumber) ?: error("No Grammar Rule item for runtime-rule '$rr'")
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
            val sorted = filtered.sortedWith(
                compareByDescending(CompletionItem::kind)
                    .thenBy(CompletionItem::label)
                    .thenByDescending({ it.text.length })
                    .thenByDescending(CompletionItem::text)
            )
            return sorted
        }

        fun <ContextType : Any> provideDefaultForSpine(spine: Spine, options: CompletionProviderOptions<ContextType>): List<CompletionItem> {
            return provideForRuleItem(spine.expectedNextRuleItems, options) + when {
                options.path.isEmpty() -> provideForTangibles(spine.expectedNextLeafNonTerminalOrTerminal, options)
                else -> emptyList()
            }
        }

        fun <ContextType : Any> provideForConcatenations(concatenations: Set<Concatenation>, options: CompletionProviderOptions<ContextType>): List<CompletionItem> = when (options.depth) {
            0 -> emptyList()
            else -> concatenations.flatMap { concat ->
                val expanded = expand(options.depth, concat, options)
                expanded.map {
                    val label = it.name ?: concat.owningRule.name.value
                    val text = it.list//.joinToString(separator = " ") { textFor(it) }
                    CompletionItem(CompletionItemKind.SEGMENT, label, text)
                }
            }
        }

        fun <ContextType : Any> provideForRuleItem(ruleItems: Set<RuleItem>, options: CompletionProviderOptions<ContextType>): List<CompletionItem> {
            return ruleItems.map { ri ->
                val expanded = expand(options.depth, ri, options)
                val citems = expanded.mapIndexed {idx,it ->
                    val label = it.name ?: ri.owningRule.name.value
                    val text = it.list//.joinToString(separator = " ") { textFor(it) }
                    CompletionItem(CompletionItemKind.SEGMENT, label, text).also { it.id = idx }
                }
                citems
            }.flatten().toSet().toList()

        }

        fun <ContextType : Any> expand(curDepth: Int, item: RuleItem, options: CompletionProviderOptions<ContextType>): List<Expansion> = when {
            options.path.isEmpty() ->  expandItem(curDepth,item,options)
            else -> {
                var pthItem = item
                var lastItems = emptyList<Expansion>()
                for (i in options.path.indices) {
                    val pthStep = options.path[i]
                    var pthDpth = pthStep.first
                    var pthIdx = pthStep.second
                    lastItems = expandItem(pthDpth, pthItem, options)
                    val ni = lastItems.getOrNull(pthIdx)?.ruleItem
                    if (ni == null) {
                        break
                    }
                    pthItem = ni
                }
                lastItems
            }
        }

        fun <ContextType : Any> expandItem(curDepth: Int, item: RuleItem, options: CompletionProviderOptions<ContextType>): List<Expansion> = when (curDepth) {
            //0 -> emptyList()
            0 -> when (item) {
                is Choice ->
                    when {
                        item.alternative.any { it is NonTerminal } -> item.alternative.map { textFor(it, options) }
                        item.alternative.all { it is TangibleItem } -> listOf(Expansion(item, null, "<${item.alternative.joinToString(separator = "|") { textFor(it, options).list }}>"))
                        else -> item.alternative.map { textFor(it, options) }
                    }

                else -> listOf(textFor(item, options))
            }
            else -> when (item) {
                is Choice -> item.alternative.flatMap { expandItem(curDepth, it, options) }
                is Concatenation -> {
                    //FIXME: SPACE may not be valid skip rule !
                    var results = listOf<Expansion>(Expansion(item, null, ""))
                    val nameFunc = when {
                        item.items.count { (it is Terminal && it.isLiteral).not() } > 1 -> { exp: Expansion -> null }
                        else -> { exp: Expansion -> exp.name }
                    }
                    for (concItem in item.items) {
                        val expand = expandItem(curDepth, concItem, options)
                        val newResults = expand.flatMap { exp ->
                            val expName = nameFunc.invoke(exp)
                            val expRl = exp.ruleItem
                            results.map { res ->
                                when {
                                    exp.list.isBlank() -> res // do not concat blank/empty items
                                    res.list.isBlank() -> Expansion(expRl, res.name ?: expName, exp.list) // first item
                                    else -> Expansion(res.ruleItem, res.name ?: expName, "${res.list} ${exp.list}")
                                }
                            }
                        }
                        results = newResults
                    }
                    results
                }

                is ConcatenationItem -> when (item) {
                    is OptionalItem -> expandItem(curDepth, item.item, options).map { Expansion(item, it.name, "${it.list}?") }
                    is ListOfItems -> expandItem(curDepth, item.item, options)
                    is SimpleItem -> when (item) {
                        is Group -> expandItem(curDepth, item.groupedContent, options)
                        is TangibleItem -> when (item) {
                            is EmptyRule -> listOf(textFor(item, options))
                            is Terminal -> listOf(textFor(item, options))
                            is Embedded -> listOf(textFor(item, options)) //TODO: could expand this !
                            is NonTerminal -> {
                                val refRule = item.referencedRule(item.owningRule.grammar)
                                when {
                                    refRule.isLeaf -> listOf(textFor(item, options))
                                    else -> {
                                        expandItem(curDepth - 1, refRule.rhs, options).map { Expansion(it.ruleItem, it.name ?: item.ruleReference.value, it.list) }
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
                                options.provideValuesForPatternTerminals -> {
                                    val p = RegexValueProvider(item.value, 'X') //TODO: pass in the 'any value'
                                    val txt = p.provide()
                                    Expansion(item, null, txt)
                                }

                                else -> Expansion(item, null, "<${item.value}>")
                            }

                            else -> Expansion(item, null, item.value)
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

        fun <ContextType : Any> provideForTangibles(tangibles: Set<TangibleItem>, options: CompletionProviderOptions<ContextType>): List<CompletionItem> {
            return tangibles.flatMap { ri ->
                when {
                    ri.owningRule.isSkip -> emptyList() //make this an option to exclude skip stuff, this also needs to be extended/improved does not cover all cases
                    else -> provideForTangible(ri, options)
                }
            }
        }

        fun <ContextType : Any> provideForTangible(tangibleItem: TangibleItem, options: CompletionProviderOptions<ContextType>): List<CompletionItem> {
            return when (tangibleItem) {
                is NonTerminal -> { //must be a reference to leaf
                    val name = tangibleItem.ruleReference.value
                    val refRule = tangibleItem.referencedRule(tangibleItem.owningRule.grammar)
                    val text = refRule.compressedLeaf.value
                    listOf(CompletionItem(CompletionItemKind.PATTERN, text, "<$name>"))
                }

                is Terminal -> {
                    val name = when {
                        tangibleItem.owningRule.isLeaf -> tangibleItem.owningRule.name.value
                        else -> tangibleItem.value
                    }
                    when {
                        tangibleItem.isPattern -> when {
                            options.provideValuesForPatternTerminals -> {
                                val p = RegexValueProvider(tangibleItem.value, 'X') //TODO: pass in the 'any value'
                                val txt = p.provide()
                                listOf(CompletionItem(CompletionItemKind.PATTERN, tangibleItem.value, txt))
                            }

                            else -> listOf(CompletionItem(CompletionItemKind.PATTERN, tangibleItem.value, "<$name>"))
                        }

                        else -> listOf(CompletionItem(CompletionItemKind.LITERAL, "'$name'", tangibleItem.value))
                    }
                }

                else -> error("Not supported subtype of TangibleItem: ${tangibleItem::class.simpleName}")
            }
        }
    }

    override fun provide(nextExpected: Set<Spine>, options: CompletionProviderOptions<ContextType>): List<CompletionItem> {
        val items = nextExpected.flatMap { sp -> provideDefaultForSpine(sp, options) }
        return defaultSortAndFilter(items)
    }

    private fun provideForRuleItem(desiredDepth: Int, item: RuleItem, options: CompletionProviderOptions<ContextType>): List<CompletionItem> {
        val rule = item.owningRule
        return when {
            rule.isLeaf -> listOf(
                CompletionItem(CompletionItemKind.PATTERN, rule.compressedLeaf.value, "<${rule.name}>")
            )

            else -> {
                val cis = getItems(desiredDepth, item, options, emptySet())
                cis.mapNotNull { it }.toSet().toList()
            }
        }
    }

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