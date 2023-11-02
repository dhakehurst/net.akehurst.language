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

package net.akehurst.language.agl.language.grammar

import net.akehurst.language.api.language.grammar.*

/**
 * creates the names of pseudo rules for a real rule.
 * It is necessary that the names of the pseudo rules, used
 * when constructing the parse tree, map to the names of pseudo rule types
 * constructed as part of the TypeModelFromGrammar.
 * So that if a property type is 'ANY' then the actual type
 * for the parse tree node can be deduced.
 */
internal class PseudoRuleNames(val grammar: Grammar) {

    private val _nextEmbeddedNumber = mutableMapOf<String, Int>()
    private val _nextGroupNumber = mutableMapOf<String, Int>()
    private val _nextChoiceNumber = mutableMapOf<String, Int>()
    private val _nextSimpleListNumber = mutableMapOf<String, Int>()
    private val _nextSeparatedListNumber = mutableMapOf<String, Int>()

    private val _nameForRuleItem = mutableMapOf<RuleItem, String>()
    private val _itemForPseudoRuleName = mutableMapOf<String, RuleItem>()

    init {
        grammar.allResolvedNonTerminalRule.forEach {
            when {
                it.isLeaf -> Unit
                it.isOneEmbedded -> Unit
                else -> {
                    val pseudoRuleNames = pseudoRulesFor(it.rhs)
                    pseudoRuleNames.forEach {
                        _nameForRuleItem[it.first] = it.second
                        _itemForPseudoRuleName[it.second] = it.first
                    }
                }
            }
        }
    }

    fun nameForRuleItem(item: RuleItem): String {
        return _nameForRuleItem[item] ?: error("Internal Error: name for RuleItem '$item' not found")
    }

    fun itemForPseudoRuleName(name: String): RuleItem {
        return _itemForPseudoRuleName[name] ?: error("Internal Error: RuleItem with name '$name' not found")
    }

    private fun pseudoRulesFor(item: RuleItem): Set<Pair<RuleItem, String>> {
        return when (item) {
            is Embedded -> setOf(Pair(item, createEmbeddedRuleName(item.embeddedGrammarReference.resolved!!.name, item.embeddedGoalName)))
            is Terminal -> emptySet()
            is NonTerminal -> emptySet()
            is EmptyRule -> emptySet()
            is Choice -> item.alternative.flatMap { pseudoRulesFor(it) }.toSet() + Pair(item, createChoiceRuleName(item.owningRule.name))
            is Concatenation -> item.items.flatMap { pseudoRulesFor(it) }.toSet()
            //is Concatenation -> when (item.items.size) {
            //    1 -> item.items.flatMap { pseudoRulesFor(it) }.toSet()
            //    else -> item.items.flatMap { pseudoRulesFor(it) }.toSet() + Pair(item, createChoiceRuleName(item.owningRule.name))
            // }
            is OptionalItem -> pseudoRulesFor(item.item) + Pair(item, createOptionalItemRuleName(item.owningRule.name))
            is SimpleList -> pseudoRulesFor(item.item) + Pair(item, createSimpleListRuleName(item.owningRule.name))
            is SeparatedList -> pseudoRulesFor(item.item) + pseudoRulesFor(item.separator) + Pair(item, createSeparatedListRuleName(item.owningRule.name))
            is Group -> when (item.groupedContent) {
                is Choice -> pseudoRulesFor(item.groupedContent) + Pair(item, createChoiceRuleName(item.owningRule.name))
                else -> pseudoRulesFor(item.groupedContent) + Pair(item, createGroupRuleName(item.owningRule.name))
            }

            else -> error("Internal Error: subtype of ${RuleItem::class.simpleName} ${item::class.simpleName} not handled")
        }
    }

    private fun createEmbeddedRuleName(grammarName: String, goalName: String): String {
        val baseName = "$grammarName§$goalName"
        var n = _nextEmbeddedNumber[baseName] ?: 0
        n++
        _nextEmbeddedNumber[baseName] = n
        return "§${baseName.removePrefix("§")}§embedded$n" //TODO: include original rule name fo easier debug
    }

    private fun createGroupRuleName(parentRuleName: String): String {
        var n = _nextGroupNumber[parentRuleName] ?: 0
        n++
        _nextGroupNumber[parentRuleName] = n
        return "§${parentRuleName.removePrefix("§")}§group$n" //TODO: include original rule name fo easier debug
    }

    private fun createChoiceRuleName(parentRuleName: String): String {
        var n = _nextChoiceNumber[parentRuleName] ?: 0
        n++
        _nextChoiceNumber[parentRuleName] = n
        return "§${parentRuleName.removePrefix("§")}§choice$n" //TODO: include original rule name fo easier debug
    }


    private fun createOptionalItemRuleName(parentRuleName: String): String {
        var n = _nextSimpleListNumber[parentRuleName] ?: 0
        n++
        _nextSimpleListNumber[parentRuleName] = n
        return "§${parentRuleName.removePrefix("§")}§opt$n" //TODO: include original rule name fo easier debug
    }

    private fun createSimpleListRuleName(parentRuleName: String): String {
        var n = _nextSimpleListNumber[parentRuleName] ?: 0
        n++
        _nextSimpleListNumber[parentRuleName] = n
        return "§${parentRuleName.removePrefix("§")}§multi$n" //TODO: include original rule name fo easier debug
    }

    private fun createSeparatedListRuleName(parentRuleName: String): String {
        var n = _nextSeparatedListNumber[parentRuleName] ?: 0
        n++
        _nextSeparatedListNumber[parentRuleName] = n
        return "§${parentRuleName.removePrefix("§")}§sepList$n" //TODO: include original rule name fo easier debug
    }

}