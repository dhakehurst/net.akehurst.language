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

package net.akehurst.language.agl.default

import net.akehurst.language.agl.completionProvider.CompletionProviderAbstract
import net.akehurst.language.agl.syntaxAnalyser.ContextSimple
import net.akehurst.language.api.analyser.ScopeModel
import net.akehurst.language.api.asm.AsmSimple
import net.akehurst.language.api.grammar.*
import net.akehurst.language.api.processor.CompletionItem
import net.akehurst.language.typemodel.api.TypeModel

class CompletionProviderDefault(
    val targetGrammar: Grammar,
    val typeModel: TypeModel,
    val scopeModel: ScopeModel
) : CompletionProviderAbstract<AsmSimple, ContextSimple>() {

    override fun provide(nextExpected: Set<RuleItem>, context: ContextSimple?, options: Map<String, Any>): List<CompletionItem> {
        return if (null == context) {
            emptyList()
        } else {
            val items = nextExpected.flatMap { provideFor(it, 2, context) }
            items
        }
    }

    private fun provideFor(item: RuleItem, desiredDepth: Int, context: ContextSimple?): List<CompletionItem> {
        val rule = item.owningRule
        val cis = getItems(item, desiredDepth, context, emptySet())
        return cis.mapNotNull { it }.toSet().toList()
    }

    // uses null to indicate that there is an empty item
    private fun getItems(item: RuleItem, desiredDepth: Int, context: ContextSimple?, done: Set<RuleItem>): List<CompletionItem?> {
        //TODO: use scope to add real items to this list - maybe in a subclass
        return when {
            done.contains(item) -> emptyList()
            else -> when (item) {
                is EmptyRule -> listOf(null)
                is Choice -> item.alternative.flatMap { getItems(it, desiredDepth, context, done + item) }
                is Concatenation -> {
                    var items = getItems(item.items[0], desiredDepth, context, done + item)
                    var index = 1
                    while (index < item.items.size && items.any { it == null }) {
                        items = items.mapNotNull { it } + getItems(item.items[index], desiredDepth, context, done + item)
                        index++
                    }
                    items
                }

                is Terminal -> provideForTerminal(item, context)

                is NonTerminal -> {
                    //TODO: handle overridden vs embedded rules!
                    val refRule = item.referencedRuleOrNull(this.targetGrammar)
                    when (refRule) {
                        null -> emptyList()
                        else -> getItems(refRule.rhs, desiredDepth - 1, context, done + item)
                    }
                }

                is SeparatedList -> {
                    val items = getItems(item.item, desiredDepth, context, done + item)
                    if (item.min == 0) {
                        items + listOf(null)
                    } else {
                        items + emptyList<CompletionItem>()
                    }
                }

                is SimpleList -> {
                    val items = getItems(item.item, desiredDepth, context, done + item)
                    if (item.min == 0) {
                        items + listOf(null)
                    } else {
                        items + emptyList<CompletionItem>()
                    }
                }

                is Group -> getItems(item.groupedContent, desiredDepth, context, done + item)
                else -> error("not yet supported!")
            }
        }
    }
}