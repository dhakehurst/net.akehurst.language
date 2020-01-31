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

import net.akehurst.language.api.analyser.GrammarExeception
import net.akehurst.language.api.grammar.*

data class RuleDefault(
		override val grammar: GrammarDefault,
		override val name: String,
		override val isSkip: Boolean,
		override val isLeaf: Boolean
) : Rule {
	
	init {
		this.grammar.rule.add(this)
	}
	
	private var _rhs: RuleItem? = null
	override var rhs: RuleItem
		get() {
			return this._rhs ?: throw GrammarExeception("rhs of rule must be set",null)
		}
		set(value) {
			value.setOwningRule(this, listOf(0))
			this._rhs = value
		}

	override val nodeType: NodeType = NodeTypeDefault(this.name)

	// --- GrammarVisitable ---

	override fun <T,A> accept(visitor: GrammarVisitor<T, A>, arg: A): T {
		return visitor.visit(this, arg);
	}
}
