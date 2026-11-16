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

package net.akehurst.language.agl.completionProvider

import net.akehurst.language.api.processor.Spine
import net.akehurst.language.grammar.api.*
import net.akehurst.language.parser.api.RulePosition
import net.akehurst.language.regex.agl.RegexValueProvider

/**
 * Generates the shortest text that completes the rules that are open at the end of a spine,
 * so that the text before a position (that may not parse on its own) plus this text is a complete sentence.
 *
 * Used to find the definitions in the sentence being completed (e.g. a type defined earlier in the same sentence).
 * NOTE: a work-around, it could be replaced by error recovery in the parser that gives a partial tree.
 *
 * - optional items are left out, lists get their minimum number of items, a choice its shortest alternative
 * - a pattern gets a generated value that matches it
 * - tokens are separated by a space (assumes a whitespace skip rule)
 */
internal object SentenceCompletion {

    private const val SEPARATOR = " "

    /** text that completes the open rules of [spine], null if none can be generated */
    fun completionTextFor(spine: Spine, targetGrammar: Grammar?): String? {
        val parts = mutableListOf<String>()
        for ((idx, node) in spine.elements.withIndex()) {
            val ruleItem = (node as? SpineNodeAbstract)?.ruleItem ?: continue // the goal (root) needs nothing more
            // the innermost node expects its child at the position, an outer node is filling the child at the position
            val from = if (0 == idx) node.nextChildNumber else node.nextChildNumber + 1
            val remainder = remainderOf(ruleItem, node.nextChildNumber, from, 0 == idx, targetGrammar) ?: return null
            if (remainder.isNotBlank()) parts.add(remainder)
        }
        return parts.joinToString(SEPARATOR)
    }

    private fun remainderOf(ruleItem: RuleItem, position: Int, from: Int, innermost: Boolean, targetGrammar: Grammar?): String? = when (ruleItem) {
        is Concatenation -> join(ruleItem.items.drop(from).map { shortest(it, targetGrammar, emptySet()) })
        is SeparatedList -> when {
            // a separator was the last thing, an item is needed
            innermost && RulePosition.POSITION_SLIST_ITEM == position -> shortest(ruleItem.item, targetGrammar, emptySet())
            innermost && RulePosition.START_OF_RULE == position -> shortest(ruleItem, targetGrammar, emptySet())
            else -> ""
        }

        is ListOfItems -> when {
            innermost && RulePosition.START_OF_RULE == position -> shortest(ruleItem, targetGrammar, emptySet())
            else -> ""
        }

        else -> when {
            innermost && RulePosition.START_OF_RULE == position -> shortest(ruleItem, targetGrammar, emptySet())
            else -> ""
        }
    }

    private fun join(parts: List<String?>): String? = when {
        parts.any { null == it } -> null
        else -> parts.filterNotNull().filter { it.isNotEmpty() }.joinToString(SEPARATOR)
    }

    /** the shortest text for [item], null if none (e.g. only recursive alternatives) */
    private fun shortest(item: RuleItem, targetGrammar: Grammar?, visiting: Set<GrammarRule>): String? = when (item) {
        is EmptyRule -> ""
        is Terminal -> when {
            item.isPattern -> runCatching { RegexValueProvider(item.unescapedValue.value, 'X').provide() }.getOrNull()
            else -> item.unescapedValue.value
        }

        is NonTerminal -> {
            val rule = CompletionProviderAbstract.referencedRuleFor(item, targetGrammar)
            when {
                rule.isLeaf -> shortest(rule.compressedLeaf, targetGrammar, visiting)
                rule in visiting -> null
                else -> shortest(rule.rhs, targetGrammar, visiting + rule)
            }
        }

        is Choice -> item.alternative.mapNotNull { shortest(it, targetGrammar, visiting) }.minByOrNull { it.length }
        is Concatenation -> join(item.items.map { shortest(it, targetGrammar, visiting) })
        is OptionalItem -> ""
        is SeparatedList -> when {
            item.min <= 0 -> ""
            else -> {
                val i = shortest(item.item, targetGrammar, visiting)
                val s = shortest(item.separator, targetGrammar, visiting)
                when {
                    null == i || null == s -> null
                    else -> join(List(item.min) { i }.flatMapIndexed { idx, it -> if (0 == idx) listOf(it) else listOf(s, it) })
                }
            }
        }

        is ListOfItems -> when {
            item.min <= 0 -> ""
            else -> shortest(item.item, targetGrammar, visiting)?.let { i -> join(List(item.min) { i }) }
        }

        is Group -> shortest(item.groupedContent, targetGrammar, visiting)
        else -> null // e.g. embedded
    }
}
