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

package net.akehurst.language.agl.grammar.grammar.asm

import net.akehurst.language.api.grammar.*

class SeparatedListDefault(
    override val min: Int,
    override val max: Int,
    override val item: SimpleItem,
    override val separator: SimpleItem,
    //override val associativity: SeparatedListKind
) : RuleItemAbstract(), SeparatedList {

    override fun setOwningRule(rule: Rule, indices: List<Int>) {
        this._owningRule = rule
        this.index = indices
        val nextIndex: List<Int> = indices + 0
        this.item.setOwningRule(rule, nextIndex)
        this.separator.setOwningRule(rule, nextIndex)
    }

    override fun subItem(index: Int): RuleItem {
        return when (index) {
            0 -> this.item
            1 -> this.separator
            else -> throw GrammarRuleItemNotFoundException("subitem ${index} not found")
        }
    }

    override val allTerminal: Set<Terminal> get() = this.item.allTerminal + this.separator.allTerminal

    override val allNonTerminal: Set<NonTerminal> get() = this.item.allNonTerminal + this.separator.allNonTerminal

    override val allEmbedded: Set<Embedded> get() = this.item.allEmbedded + this.separator.allEmbedded

    override fun toString(): String {
        val mult = when {
            0 == min && 1 == max -> "?"
            0 == min && -1 == max -> "*"
            1 == min && -1 == max -> "+"
            -1 == max -> " $min+"
            else -> " $min..$max"
        }
        return "[$item / $separator]$mult"
    }
}