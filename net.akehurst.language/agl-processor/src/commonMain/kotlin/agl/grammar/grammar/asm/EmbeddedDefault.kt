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

class EmbeddedDefault(
    override val embeddedGoalName: String,
    override val embeddedGrammar: Grammar
) : RuleItemAbstract(), Embedded {

    override val name: String get() = this.embeddedGoalName

    override fun referencedRule(targetGrammar: Grammar): Rule {
        return targetGrammar.findNonTerminalRule(this.name) ?: error("Grammar Rule '$name' not found in grammar '${targetGrammar.name}'")
    }

    override fun setOwningRule(rule: Rule, indices: List<Int>) {
        this._owningRule = rule
        this.index = indices
    }

    override fun subItem(index: Int): RuleItem {
        throw GrammarRuleItemNotFoundException("subitem ${index} not found")
    }

    override val allTerminal: Set<Terminal>  get() = emptySet()

    override val allNonTerminal: Set<NonTerminal>  get() = emptySet()

    override val allEmbedded: Set<Embedded> get() = setOf(this)

    override fun toString(): String = "${embeddedGrammar.name}.$embeddedGoalName"
}
