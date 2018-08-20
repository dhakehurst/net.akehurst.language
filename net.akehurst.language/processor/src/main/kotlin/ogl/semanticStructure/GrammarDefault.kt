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

import net.akehurst.language.api.grammar.Grammar
import net.akehurst.language.api.grammar.GrammarRuleNotFoundException
import net.akehurst.language.api.grammar.Namespace
import net.akehurst.language.api.grammar.NodeType
import net.akehurst.language.api.grammar.Rule
import net.akehurst.language.api.grammar.RuleItem
import net.akehurst.language.api.grammar.Terminal

class GrammarDefault(override val namespace: Namespace, override val name: String, override val rule: MutableList<Rule>) : GrammarAbstract(namespace, name, rule) {

}

abstract class GrammarAbstract(override val namespace: Namespace, override val name: String, override val rule: List<Rule>) : Grammar {

	override val extends: MutableList<Grammar> = mutableListOf<Grammar>();


	override val allRule: List<Rule> by lazy {
		this.extends.flatMap { it.allRule }.plus(this.rule)
	}

	override val allTerminal: Set<Terminal> by lazy {
		this.allRule.toSet().flatMap { it.rhs?.allTerminal ?: setOf() }.toSet()
	}

	override val allNodeType: Set<NodeType> by lazy {
		this.allRule.map { NodeTypeDefault(it.name) }.toSet()
	}

	override fun findAllRule(ruleName: String): Rule {
		val all = this.allRule.filter { it.name == ruleName }
		when {
			all.isEmpty() -> throw GrammarRuleNotFoundException("${ruleName} in Grammar(${this.name}).findAllRule")
			all.size > 1 -> throw GrammarRuleNotFoundException("More than one rule named ${ruleName} in Grammar(${this.name}).findAllRule")
		}
		return all.first()
	}

	override fun findAllTerminal(terminalPattern: String): Terminal {
		val all = this.allTerminal.filter { it.value == terminalPattern }
		when {
			all.isEmpty() -> throw GrammarRuleNotFoundException("${terminalPattern} in Grammar(${this.name}).findAllRule")
			all.size > 1 -> throw GrammarRuleNotFoundException("More than one rule named ${terminalPattern} in Grammar(${this.name}).findAllRule")
		}
		return all.first()
	}
}
