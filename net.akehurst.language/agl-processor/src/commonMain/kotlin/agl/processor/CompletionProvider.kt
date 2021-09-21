/**
 * Copyright (C) 2018 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.akehurst.language.agl.processor

import net.akehurst.language.api.grammar.*
import net.akehurst.language.api.processor.CompletionItem

class CompletionProvider(
        val targetGrammar:Grammar
) {

    fun provideFor(item: RuleItem, desiredDepth: Int): List<CompletionItem> {
        val rule = item.owningRule
        val cis = getItems(item, desiredDepth, emptySet())
        return cis.mapNotNull { it }.toSet().toList()//.map { CompletionItem(item.owningRule, it.text) }
    }

    // uses null to indicate that there is an empty item
    fun getItems(item: RuleItem, desiredDepth: Int, done: Set<RuleItem>): List<CompletionItem?> {
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
                is Terminal -> when {
                    item.owningRule.isLeaf -> listOf(CompletionItem(item.owningRule, item.owningRule.name))  //TODO: generate text/example from regEx
                    item.isPattern -> listOf(CompletionItem(item.owningRule, item.name)) //TODO: generate text/example from regEx
                    else -> listOf(CompletionItem(item.owningRule, item.value))
                }
                is NonTerminal -> {
                    //TODO: handle overridden vs embedded rules!
                    getItems(item.referencedRule.rhs, desiredDepth - 1, done + item)
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
                is Group -> getItems(item.choice, desiredDepth, done + item)
                else -> error("not yet supported!")
            }
        }
    }
}