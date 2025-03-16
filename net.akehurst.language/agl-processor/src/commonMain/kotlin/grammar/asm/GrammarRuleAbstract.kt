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

abstract class GrammarItemAbstract() : GrammarItem {
    //override lateinit var grammar: Grammar
}

abstract class GrammarRuleAbstract() : GrammarItemAbstract(), GrammarRule {

    companion object {
        class CompressedLeafRule(
            override val id: String,
            override val value: String,
            override val isPattern: Boolean
        ) : Terminal, RuleItemAbstract() {

            override val isLiteral: Boolean get() = isPattern.not()
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

        private fun toRegEx(value: String): String {
            return Regex.escape(value)
        }

        fun compressRuleItem(compressedName: String, item: RuleItem): CompressedLeafRule {
            val grammar = item.owningRule.grammar
            val cr = when (item) {
                is Terminal -> when {
                    item.isPattern -> CompressedLeafRule(compressedName, item.value, true)
                    //else -> CompressedLeafRule(compressedName, "(${toRegEx(item.value)})", true)
                    else -> CompressedLeafRule(compressedName, toRegEx(item.value), false) //TODO: not escape literals if not needed ! asRegexLiteral ?
                }

                is Concatenation -> {
                    if (1 == item.items.size) {
                        this.compressRuleItem(compressedName, item.items[0])
                    } else {
                        val items = item.items.mapIndexed { idx, it -> this.compressRuleItem("$compressedName$idx", it) }
                        val pattern = items.joinToString(separator = "") { "(${it.value})" }
                        CompressedLeafRule(compressedName, pattern, true)
                        //throw GrammarExeception("GrammarRule ${rhs.owningRule.name}, compressing ${rhs::class} to leaf is not yet supported", null)
                    }
                }

                is Choice -> when (item.alternative.size) {
                    1 -> this.compressRuleItem(compressedName, item.alternative[0])
                    else -> {
                        val ct = item.alternative.mapIndexed { idx, it -> this.compressRuleItem("$compressedName$idx", it) }
                        val pattern = ct.joinToString(separator = "|") { "(${it.value})" }
                        CompressedLeafRule(compressedName, pattern, true)
                    }
                }

                is OptionalItem -> {
                    val ct = this.compressRuleItem("${compressedName}List", item.item)
                    val pattern = "(${ct.value})?"
                    CompressedLeafRule(compressedName, pattern, true)
                }

                is SimpleList -> {
                    val ct = this.compressRuleItem("${compressedName}List", item.item)
                    val min = item.min
                    val max = if (-1 == item.max) "" else item.max
                    val pattern = "(${ct.value}){${min},${max}}"
                    CompressedLeafRule(compressedName, pattern, true)
                }

                is Group -> {
                    val ct = this.compressRuleItem("${compressedName}Group", item.groupedContent)
                    val pattern = "(${ct.value})"
                    CompressedLeafRule(compressedName, pattern, true)
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
