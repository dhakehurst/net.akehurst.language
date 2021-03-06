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

import net.akehurst.language.api.grammar.Concatenation;
import net.akehurst.language.api.grammar.ConcatenationItem;
import net.akehurst.language.api.grammar.NonTerminal;
import net.akehurst.language.api.grammar.RuleItem;
import net.akehurst.language.api.grammar.Terminal;
import net.akehurst.language.api.grammar.Rule;
import net.akehurst.language.api.grammar.GrammarVisitor;

class ConcatenationDefault(override val items: List<ConcatenationItem>) : RuleItemAbstract(), Concatenation {

   override fun setOwningRule(rule: Rule, indices: List<Int>) {
		this._owningRule = rule
		this.index = indices
		var i: Int = 0
		this.items.forEach {
			val nextIndex: List<Int> = indices + (i++)
			it.setOwningRule(rule, nextIndex)
		}
	}
	
	override fun subItem(index: Int): RuleItem {
		return this.items.get(index)
	}
	
	override val allTerminal: Set<Terminal> by lazy {
		this.items.flatMap { it.allTerminal }.toSet()
	}

	override val allNonTerminal: Set<NonTerminal> by lazy {
		this.items.flatMap { it.allNonTerminal }.toSet()
	}

	// --- GrammarVisitable ---

	override fun <T,A> accept(visitor: GrammarVisitor<T, A>, arg: A): T {
		return visitor.visit(this, arg);
	}

}
