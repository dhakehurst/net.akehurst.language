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

import net.akehurst.language.api.grammar.Namespace
import net.akehurst.language.api.grammar.Grammar
import net.akehurst.language.api.grammar.Rule

class GrammarBuilderDefault(val namespace: Namespace, val name: String) {

	val grammar: GrammarDefault

	init {
		this.grammar = GrammarDefault(namespace, name);
	}

	fun rule(name: String): RuleBuilder {
		return RuleBuilder(RuleDefault(grammar, name, false))
	}

	fun skip(name: String): RuleBuilder {
		return RuleBuilder(RuleDefault(this.grammar, name, true))
	}
	
	class RuleBuilder(val rule: Rule) {

		fun concatenation(vararg sequence: ConcatenationItemAbstract) {
			this.rule.rhs = ChoiceSimpleDefault(ConcatenationDefault(sequence));
		}

		fun choice(vararg alternative: ConcatenationItemAbstract) {
			val alternativeConcats = alternative.map { ConcatenationDefault(it) }
			this.rule.rhs = ChoiceSimpleDefault(alternativeConcats.toArray());
		}

		fun priorityChoice(vararg alternative: ConcatenationItemAbstract) {
			val alternativeConcats = alternative.map { ConcatenationDefault(it) }
			this.rule.rhs = ChoicePriorityDefault(alternativeConcats.toArray());
		}

		fun multi(min: Long, max: Long, item: TangibleItemAbstract) {
			this.rule.rhs = ChoiceSimpleDefault(ConcatenationDefault(MultiDefault(min, max, item)));
		}

		fun separatedList(min: Long, max: Long, separator: TerminalLiteralDefault, item: TangibleItemAbstract) {
			this.rule.rhs = ChoiceSimpleDefault(ConcatenationDefault(SeparatedListDefault(min, max, separator, item)));
		}
	}
}
