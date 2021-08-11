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


import net.akehurst.language.api.grammar.*

internal abstract class ChoiceAbstract(override val alternative: List<Concatenation>) : RuleItemAbstract(), Choice {

	override fun setOwningRule(rule: Rule, indices: List<Int>) {
		this._owningRule = rule
		this.index = indices
		var i: Int = 0
		this.alternative.forEach {
			val nextIndex: List<Int> = indices + (i++)
			it.setOwningRule(rule, nextIndex)
		}
	}

	override fun subItem(index: Int): RuleItem {
//		 return if (index < this.alternative.size) this.alternative.get(index) else null
		return this.alternative.get(index)
	}

	override val allTerminal: Set<Terminal> by lazy {
		this.alternative.flatMap { it.allTerminal }.toSet()
	}

	override val allNonTerminal: Set<NonTerminal> by lazy {
		this.alternative.flatMap { it.allNonTerminal }.toSet()
	}

}
