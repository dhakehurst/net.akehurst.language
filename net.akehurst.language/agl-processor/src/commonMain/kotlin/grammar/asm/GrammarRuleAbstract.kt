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

package net.akehurst.language.grammar.asm

import net.akehurst.language.base.api.SimpleName
import net.akehurst.language.grammar.api.*
import net.akehurst.language.regex.agl.regexMatcher
import net.akehurst.language.regex.api.EscapedValue
import net.akehurst.language.regex.api.UnescapedPattern
import net.akehurst.language.regex.api.UnescapedValue

abstract class GrammarItemAbstract() : GrammarItem {
    //override lateinit var grammar: Grammar
}

abstract class GrammarRuleAbstract() : GrammarItemAbstract(), GrammarRule {

    companion object {
        class CompressedLeafRule(
            override val id: String,
            override val unescapedValue: UnescapedValue,
            override val isPattern: Boolean
        ) : Terminal, RuleItemAbstract() {

            override val isLiteral: Boolean get() = isPattern.not()
            override val escapedValue: EscapedValue get() = unescapedValue.escapedFoAgl
            override val allTerminal: Set<Terminal> = setOf(this)
            override val allNonTerminal: Set<NonTerminal> = emptySet()
            override val allEmbedded: Set<Embedded> = emptySet()
            override val firstTerminal: Set<Terminal> get() = emptySet()
            override val firstTangible: Set<TangibleItem> get() = emptySet()
            override val firstTangibleRecursive: Set<Terminal> get() = emptySet()
            override val firstConcatenationRecursive: Set<Concatenation> get() = emptySet()
            override fun setOwningRule(rule: GrammarRule, indices: List<Int>) {
                TODO("not implemented")
            }
            override fun subItem(index: Int): RuleItem {
                TODO("not implemented")
            }

            override fun itemsForChild(childNumber: Int): Set<RuleItem>  = emptySet()

        }

        /**
         * The fewest and most characters a leaf rule item can match. [max] is -1 when there is no maximum, or it is not known.
         */
        data class LeafLength(val min: Int, val max: Int) {
            val isFixed: Boolean get() = -1 != max && min == max

            companion object {
                val UNKNOWN = LeafLength(0, -1)
            }
        }

        fun leafLength(item: RuleItem): LeafLength = leafLength(item, emptySet())

        private fun leafLength(item: RuleItem, visiting: Set<GrammarRule>): LeafLength {
            fun plus(a: Int, b: Int) = if (-1 == a || -1 == b) -1 else a + b
            return when (item) {
                is Terminal -> when {
                    item.isPattern -> {
                        val m = runCatching { regexMatcher(item.unescapedValue.value) }.getOrNull()
                        when {
                            null == m || -1 == m.minLength -> LeafLength.UNKNOWN
                            else -> LeafLength(m.minLength, m.maxLength)
                        }
                    }

                    else -> LeafLength(item.unescapedValue.value.length, item.unescapedValue.value.length)
                }

                is Concatenation -> item.items.map { leafLength(it, visiting) }.fold(LeafLength(0, 0)) { acc, it -> LeafLength(acc.min + it.min, plus(acc.max, it.max)) }

                is Choice -> {
                    val ls = item.alternative.map { leafLength(it, visiting) }
                    LeafLength(ls.minOf { it.min }, if (ls.any { -1 == it.max }) -1 else ls.maxOf { it.max })
                }

                is OptionalItem -> LeafLength(0, leafLength(item.item, visiting).max)

                is SimpleList -> {
                    val l = leafLength(item.item, visiting)
                    val max = when {
                        0 == l.max -> 0
                        -1 == item.max || -1 == l.max -> -1
                        else -> item.max * l.max
                    }
                    LeafLength(item.min * l.min, max)
                }

                is Group -> leafLength(item.groupedContent, visiting)

                is NonTerminal -> {
                    val rule = runCatching { item.referencedRule(item.owningRule.grammar) }.getOrNull()
                    when {
                        null == rule || visiting.contains(rule) -> LeafLength.UNKNOWN
                        else -> leafLength(rule.rhs, visiting + rule)
                    }
                }

                else -> LeafLength.UNKNOWN
            }
        }

        /**
         * The alternatives of a longest choice ('|') in the order to put them in a regex.
         * A regex takes the first alternative that matches, so when every alternative has a fixed length
         * they are put longest first (stable, so equal lengths keep their order) and the longest one that matches is taken.
         * Otherwise, and for other kinds of choice, the order as written is kept.
         */
        fun leafChoiceRegexOrder(item: Choice): List<RuleItem> = when {
            item !is ChoiceLongest -> item.alternative
            else -> {
                val lengths = item.alternative.map { leafLength(it) }
                when {
                    lengths.all { it.isFixed } -> item.alternative.zip(lengths).sortedByDescending { it.second.max }.map { it.first }
                    else -> item.alternative
                }
            }
        }

        private fun toRegEx(value: String): String {
            return Regex.escape(value)
        }

        fun compressRuleItem(compressedName: String, item: RuleItem): CompressedLeafRule {
            val grammar = item.owningRule.grammar
            val cr = when (item) {
                is Terminal -> when {
                    item.isPattern -> CompressedLeafRule(compressedName, item.unescapedValue, true)
                    //else -> CompressedLeafRule(compressedName, "(${toRegEx(item.value)})", true)
                    else -> {
                        //TODO: not escape literals if not needed ! asRegexLiteral ?
                        val unescapedValue = UnescapedPattern(toRegEx(item.unescapedValue.value))
                        CompressedLeafRule(compressedName, unescapedValue, false)
                    }
                }

                is Concatenation -> {
                    if (1 == item.items.size) {
                        this.compressRuleItem(compressedName, item.items[0])
                    } else {
                        val items = item.items.mapIndexed { idx, it -> this.compressRuleItem("$compressedName$idx", it) }
                        val pattern = items.joinToString(separator = "") { "(${it.unescapedValue.value})" }
                        CompressedLeafRule(compressedName, UnescapedPattern(pattern), true)
                        //throw GrammarExeception("GrammarRule ${rhs.owningRule.name}, compressing ${rhs::class} to leaf is not yet supported", null)
                    }
                }

                is Choice -> when (item.alternative.size) {
                    1 -> this.compressRuleItem(compressedName, item.alternative[0])
                    else -> {
                        val ct = leafChoiceRegexOrder(item).mapIndexed { idx, it -> this.compressRuleItem("$compressedName$idx", it) }
                        val pattern = ct.joinToString(separator = "|") { "(${it.unescapedValue.value})" }
                        CompressedLeafRule(compressedName, UnescapedPattern(pattern), true)
                    }
                }

                is OptionalItem -> {
                    val ct = this.compressRuleItem("${compressedName}List", item.item)
                    val pattern = "(${ct.unescapedValue.value})?"
                    CompressedLeafRule(compressedName, UnescapedPattern(pattern), true)
                }

                is SimpleList -> {
                    val ct = this.compressRuleItem("${compressedName}List", item.item)
                    val min = item.min
                    val max = if (-1 == item.max) "" else item.max
                    val pattern = "(${ct.unescapedValue.value}){${min},${max}}"
                    CompressedLeafRule(compressedName, UnescapedPattern(pattern), true)
                }

                is Group -> {
                    val ct = this.compressRuleItem("${compressedName}Group", item.groupedContent)
                    val pattern = "(${ct.unescapedValue.value})"
                    CompressedLeafRule(compressedName, UnescapedPattern(pattern), true)
                }

                is NonTerminal -> {
                    //TODO: handle overridden vs embedded rules!
                    //TODO: need to catch the recursion before this
                    this.compressRuleItem(compressedName, item.referencedRule(grammar).rhs)
                }

                else -> error("GrammarRule ${item.owningRule.name}, compressing ${item::class.simpleName} to leaf is not yet supported")
            }
            //cr.grammar = grammar
            return cr
        }

    }

    override val qualifiedName get() = this.grammar.qualifiedName.append(SimpleName(this.name.value))

    override val isOneEmbedded: Boolean get() = this.rhs is Embedded || (this.rhs is Concatenation) && (this.rhs as Concatenation).items[0] is Embedded

    override val compressedLeaf: Terminal by lazy { compressRuleItem(this.name.value, this.rhs) }
}
