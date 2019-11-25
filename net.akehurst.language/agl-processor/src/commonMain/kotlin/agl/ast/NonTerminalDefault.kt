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

package net.akehurst.language.agl.ast

import kotlin.text.Regex
import net.akehurst.language.api.grammar.NonTerminal
import net.akehurst.language.api.grammar.Terminal
import net.akehurst.language.api.grammar.GrammarVisitor
import net.akehurst.language.api.grammar.Rule
import net.akehurst.language.api.grammar.RuleItem
import net.akehurst.language.api.grammar.GrammarRuleItemNotFoundException

class NonTerminalDefault(override val name: String) : RuleItemAbstract(), NonTerminal {

	override val referencedRule : Rule by lazy {
		this.owningRule.grammar.findAllRule(this.name)
	}

    override fun setOwningRule(rule: Rule, indices: List<Int>) {
		this._owningRule = rule
		this.index = indices
	}
	
	override fun subItem(index: Int): RuleItem {
		throw GrammarRuleItemNotFoundException("subitem ${index} not found")
	}
	
	override val allTerminal: Set<Terminal> by lazy {
		emptySet<Terminal>()
	}

	override val allNonTerminal: Set<NonTerminal> by lazy {
		setOf(this)
	}

	// --- GrammarVisitable ---

	override fun <T,A> accept(visitor: GrammarVisitor<T, A>, arg: A): T {
		return visitor.visit(this, arg);
	}

}
