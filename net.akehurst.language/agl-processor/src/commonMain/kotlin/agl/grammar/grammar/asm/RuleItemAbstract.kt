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

abstract class RuleItemAbstract : RuleItem {

	protected var _owningRule : Rule? = null

	override val owningRule: Rule get() {
		return this._owningRule ?: throw GrammarRuleNotFoundException("Internal Error: owningRule must be set")
	}
	
	var index: List<Int>? = null

	abstract override val allTerminal: Set<Terminal>

	abstract override val allNonTerminal: Set<NonTerminal>

	
	
}
