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

package net.akehurst.language.ogl.semanticStructure

import net.akehurst.language.api.grammar.Grammar;
import net.akehurst.language.api.grammar.GrammarRuleNotFoundException;
import net.akehurst.language.api.grammar.NodeType;
import net.akehurst.language.api.grammar.NonTerminal;
import net.akehurst.language.api.grammar.Rule;
import net.akehurst.language.api.grammar.RuleItem;
import net.akehurst.language.api.grammar.Terminal;

public class RuleDefault(override val grammar: GrammarDefault, override val name: String) : Rule {
	
	init {
		this.grammar.rule.add(this)
	}
	
	private var _rhs: RuleItem? = null
	override var rhs: RuleItem?
		get() = this._rhs
		set(value) {
			value?.setOwningRule(this, listOf(0))
			this._rhs = value
		}

	override val nodeType: NodeType = NodeTypeDefault(this.name)

}
