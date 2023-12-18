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

import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeSpineDefault
import net.akehurst.language.api.language.grammar.*
import net.akehurst.language.api.parser.RuntimeSpine
import net.akehurst.language.api.processor.CompletionItem
import net.akehurst.language.api.processor.CompletionItemKind
import net.akehurst.language.api.processor.CompletionProvider
import net.akehurst.language.api.processor.Spine

internal class SpineDefault(
    private val runtimeSpine: RuntimeSpine,
    val mapToGrammar: (Int, Int) -> RuleItem?
) : Spine {

    override val expectedNextItems: Set<RuleItem> by lazy {
        runtimeSpine.expectedNextTerminals.mapNotNull {
            val rr = it as RuntimeRule
            mapToGrammar(rr.runtimeRuleSetNumber, rr.ruleNumber)
        }.toSet()
    }

    override val elements: List<RuleItem> by lazy {
        runtimeSpine.elements.mapNotNull {
            val rr = it as RuntimeRule
            mapToGrammar(rr.runtimeRuleSetNumber, rr.ruleNumber)
        }
    }

    override val nextChildNumber get() = (runtimeSpine as RuntimeSpineDefault).nextChildNumber

    override fun toString(): String = "Spine [$nextChildNumber]->${elements.joinToString(separator = "->") { it.toString() }}"
}

abstract class CompletionProviderAbstract<in AsmType, in ContextType> : CompletionProvider<AsmType, ContextType> {

    protected fun provideTerminalsForSpine(spine: Spine): List<CompletionItem> {
        return spine.expectedNextItems.flatMap { ri ->
            when {
                ri.owningRule.isSkip -> emptyList() //make this an option to exclude skip stuff, this also needs to be extended/improved does not cover all cases
                ri is Terminal -> provideForTerminal(ri)
                else -> provideForRuleItem(ri, 2)
            }
        }
    }

    protected fun provideForTerminal(terminalItem: Terminal): List<CompletionItem> {
        val name = when {
            terminalItem.owningRule.isLeaf -> terminalItem.owningRule.name
            else -> terminalItem.name
        }
        val ci = when {
            terminalItem.isPattern -> CompletionItem(CompletionItemKind.PATTERN, "<$name>", terminalItem.value)
            else -> CompletionItem(CompletionItemKind.LITERAL, terminalItem.value, name)
        }
        return listOf(ci)
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

                is Terminal -> provideForTerminal(item)

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