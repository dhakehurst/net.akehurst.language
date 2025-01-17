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
import net.akehurst.language.grammar.processor.ContextFromGrammarRegistry
import net.akehurst.language.parser.api.RuntimeSpine

internal abstract class SpineNodeAbstract(
    val ruleItem: RuleItem,
    override val nextChildNumber: Int
) : SpineNode {
    override val rule: GrammarRule get() = ruleItem.owningRule
    override val nextExpectedItem: RuleItem
        get() = ruleItem.itemForChild(nextChildNumber)
            ?: error("should never happen")
    override val expectedNextLeafNonTerminalOrTerminal: Set<TangibleItem> get() = nextExpectedItem.firstTangibleRecursive
    override val nextExpectedConcatenation: Set<Concatenation> get() = nextExpectedItem.firstConcatenationRecursive

    override fun toString(): String = "($ruleItem)[$nextChildNumber]"
}

internal class SpineNodeRoot(
    val rootRuleItem: RuleItem
) : SpineNode {
    override val nextChildNumber: Int get() = 0
    override val rule: GrammarRule get() = rootRuleItem.owningRule
    override val nextExpectedItem: RuleItem get() = rootRuleItem
    override val expectedNextLeafNonTerminalOrTerminal: Set<TangibleItem> get() = nextExpectedItem.firstTangibleRecursive
    override val nextExpectedConcatenation: Set<Concatenation> get() = nextExpectedItem.firstConcatenationRecursive
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
        elements[0].expectedNextLeafNonTerminalOrTerminal
        //runtimeSpine.expectedNextTerminals.map {
        //    val rr = it as RuntimeRule
        //    mapToGrammar(rr.runtimeRuleSetNumber, rr.ruleNumber) as Terminal
        //}.toSet()
    }

    override val expectedNextConcatenation: Set<Concatenation> by lazy {
        elements[0].nextExpectedConcatenation
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

    override fun toString(): String = "Spine ${elements.joinToString(separator = "->") { it.toString() }}"
}

data class Expansion(
    val name: String?,
    val list: String
)

abstract class CompletionProviderAbstract<AsmType : Any, ContextType : Any> : CompletionProvider<AsmType, ContextType> {

    companion object {
        fun provideForTerminalsAndConcatenations(depth: Int, concatenations: Set<Concatenation>, tangibles: Set<TangibleItem>) =
            provideForConcatenations(depth, concatenations) + provideForTangibles(tangibles)

        fun provideForConcatenations(depth: Int, concatenations: Set<Concatenation>): List<CompletionItem> = when (depth) {
            0 -> emptyList()
            else -> concatenations.flatMap { concat ->
                val expanded = expand(depth, concat)
                expanded.map {
                    val label = it.name ?: concat.owningRule.name.value
                    val text = it.list//.joinToString(separator = " ") { textFor(it) }
                    CompletionItem(CompletionItemKind.SEGMENT, text, label)
                }
            }
        }

        fun expand(depth: Int, item: RuleItem): List<Expansion> = when (depth) {
            0 -> emptyList()
            1 -> when (item) {
                is Choice ->
                    when {
                        item.alternative.any { it is NonTerminal } -> item.alternative.map { textFor(it) }
                        item.alternative.all { it is TangibleItem } -> listOf(Expansion(null, "<${item.alternative.joinToString(separator = "|") { textFor(it).list }}>"))
                        else -> item.alternative.map { textFor(it) }
                    }
                else -> listOf(textFor(item))
            }
            else -> {
                when (item) {
                    is Choice -> item.alternative.flatMap { expand(depth, it) }
                    is Concatenation -> {
                        //FIXME: SPACE may not be valid skip rule !
                        var results = listOf<Expansion>( Expansion(null,""))
                        val nameFunc = when {
                            item.items.count { (it is Terminal && it.isLiteral).not() } > 1 -> { exp:Expansion -> null }
                            else -> { exp:Expansion -> exp.name }
                        }
                        for (it in item.items) {
                            val expand = expand(depth, it)
                            val newResults = expand.flatMap { exp ->
                                val expName = nameFunc.invoke(exp)
                                results.map {
                                    when {
                                        it.list.isBlank() -> Expansion(it.name?:expName,exp.list)
                                        else ->  Expansion(it.name?:expName,"${it.list} ${exp.list}")
                                    }
                                }
                            }
                            results = newResults
                        }
                        results
                    }

                    is ConcatenationItem -> when (item) {
                        is OptionalItem -> expand(depth, item.item).map { Expansion(it.name,"${it.list}?") }
                        is ListOfItems -> expand(depth, item.item)
                        is SimpleItem -> when (item) {
                            is Group -> expand(depth, item.groupedContent)
                            is TangibleItem -> when (item) {
                                is EmptyRule -> listOf( textFor(item))
                                is Terminal -> listOf( textFor(item))
                                is Embedded -> listOf(textFor(item)) //TODO: could expand this !
                                is NonTerminal -> {
                                    val refRule = item.referencedRule(item.owningRule.grammar)
                                    when {
                                        refRule.isLeaf -> listOf( textFor(item))
                                        else -> expand(depth - 1, refRule.rhs).map { Expansion(it.name?:item.ruleReference.value, it.list) }
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
        }

        fun textFor(item: RuleItem): Expansion = when (item) {
            is Choice -> Expansion(null, "<${item.alternative.joinToString(separator = "|") { textFor(it).list }}>")
            is Concatenation -> Expansion(null, item.items.joinToString(separator = " ") { textFor(it).list } ) //FIXME: SPACE may not be valid skip rule !
            is ConcatenationItem -> when (item) {
                is OptionalItem -> Expansion(null,textFor(item.item).list + "?")
                is ListOfItems -> textFor(item.item)
                is SimpleItem -> when (item) {
                    is Group -> textFor(item.groupedContent)
                    is TangibleItem -> when (item) {
                        is EmptyRule -> Expansion(null,"")
                        is Terminal -> when {
                            item.owningRule.isLeaf ->Expansion( null,"<${item.owningRule.name}>")
                            item.isPattern ->Expansion(null, "<${item.value}>")
                            else ->Expansion(null,  item.value)
                        }

                        is NonTerminal -> Expansion(item.ruleReference.value, "<${item.ruleReference.value}>")
                        is Embedded -> Expansion(null, "<${item.embeddedGrammarReference.nameOrQName.simpleName.value}::${item.embeddedGoalName.value}>")
                        else -> error("Unsupported subtype of TangibleItem: ${item::class.simpleName}")
                    }

                    else -> error("Unsupported subtype of SimpleItem: ${item::class.simpleName}")
                }

                else -> error("Unsupported subtype of ConcatenationItem: ${item::class.simpleName}")
            }

            else -> error("Unsupported subtype of RuleItem: ${item::class.simpleName}")
        }

        fun provideForTangibles(tangibles: Set<TangibleItem>): List<CompletionItem> {
            return tangibles.flatMap { ri ->
                when {
                    ri.owningRule.isSkip -> emptyList() //make this an option to exclude skip stuff, this also needs to be extended/improved does not cover all cases
                    else -> provideForTangible(ri)
                }
            }
        }

        fun provideForTangible(tangibleItem: TangibleItem): List<CompletionItem> {
            return when (tangibleItem) {
                is NonTerminal -> { //must be a reference to leaf
                    val name = tangibleItem.ruleReference.value
                    val refRule = tangibleItem.referencedRule(tangibleItem.owningRule.grammar)
                    val text = refRule.compressedLeaf.value
                    listOf(CompletionItem(CompletionItemKind.PATTERN, "<$name>", text))
                }

                is Terminal -> {
                    val name = when {
                        tangibleItem.owningRule.isLeaf -> tangibleItem.owningRule.name.value
                        else -> tangibleItem.value
                    }
                    when {
                        tangibleItem.isPattern -> listOf(CompletionItem(CompletionItemKind.PATTERN, "<$name>", tangibleItem.value))
                        else -> listOf(CompletionItem(CompletionItemKind.LITERAL, tangibleItem.value, "'$name'"))
                    }
                }

                else -> error("Not supported subtype of TangibleItem: ${tangibleItem::class.simpleName}")
            }
        }
    }

    override fun provide(nextExpected: Set<Spine>, options: CompletionProviderOptions<ContextType>): List<CompletionItem> {
        return nextExpected.flatMap { sp -> provideForTerminalsAndConcatenations(options.depth, sp.expectedNextConcatenation, sp.expectedNextLeafNonTerminalOrTerminal) }
            .toSet()
            .toList()
            .sortedBy { it.kind }
    }

    private fun provideForRuleItem(item: RuleItem, desiredDepth: Int): List<CompletionItem> {
        val rule = item.owningRule
        return when {
            rule.isLeaf -> listOf(
                CompletionItem(CompletionItemKind.PATTERN, "<${rule.name}>", rule.compressedLeaf.value)
            )

            else -> {
                val cis = getItems(item, desiredDepth, emptySet())
                cis.mapNotNull { it }.toSet().toList()
            }
        }
    }

    // uses null to indicate that there is an empty item
    private fun getItems(item: RuleItem, desiredDepth: Int, done: Set<RuleItem>): List<CompletionItem?> {
        //TODO: use scope to add real items to this list - maybe in a subclass
        return when {
            done.contains(item) -> emptyList()
            else -> when (item) {
                is EmptyRule -> listOf(null)
                is Choice -> item.alternative.flatMap { getItems(it, desiredDepth, done + item) }
                is Concatenation -> {
                    var items = getItems(item.items[0], desiredDepth, done + item)
                    var index = 1
                    while (index < item.items.size && items.any { it == null }) {
                        items = items.mapNotNull { it } + getItems(item.items[index], desiredDepth, done + item)
                        index++
                    }
                    items
                }

                is Terminal -> provideForTangible(item)

                is NonTerminal -> {
                    //TODO: handle overridden vs embedded rules!
                    val refRule = item.referencedRuleOrNull(item.owningRule.grammar)
                    when (refRule) {
                        null -> emptyList()
                        else -> getItems(refRule.rhs, desiredDepth - 1, done + item)
                    }
                }

                is SeparatedList -> {
                    val items = getItems(item.item, desiredDepth, done + item)
                    if (item.min == 0) {
                        items + listOf(null)
                    } else {
                        items + emptyList<CompletionItem>()
                    }
                }

                is SimpleList -> {
                    val items = getItems(item.item, desiredDepth, done + item)
                    if (item.min == 0) {
                        items + listOf(null)
                    } else {
                        items + emptyList<CompletionItem>()
                    }
                }

                is Group -> getItems(item.groupedContent, desiredDepth, done + item)
                else -> error("not yet supported!")
            }
        }
    }

}