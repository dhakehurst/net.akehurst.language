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

class NonTerminalDefault(
        override val name: String
) : RuleItemAbstract(), NonTerminal {

    override fun referencedRuleOrNull(targetGrammar: Grammar) : GrammarRule? = targetGrammar.findNonTerminalRule(this.name)

    override fun referencedRule(targetGrammar: Grammar): GrammarRule  {
       return referencedRuleOrNull(targetGrammar) ?: error("Grammar GrammarRule ($name) not found in grammar (${targetGrammar.name})")
    }

    override fun setOwningRule(rule: GrammarRule, indices: List<Int>) {
        this._owningRule = rule
        this.index = indices
    }

    override fun subItem(index: Int): RuleItem {
        throw GrammarRuleItemNotFoundException("subitem ${index} not found")
    }

    override val allTerminal: Set<Terminal> get() = emptySet()

    override val allNonTerminal: Set<NonTerminal> get() = setOf(this)

    override val allEmbedded: Set<Embedded> get() = emptySet()

    override fun toString(): String = name
}
