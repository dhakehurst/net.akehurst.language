/**
 * Copyright (C) 2015 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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
package net.akehurst.language.ogl.semanticStructure;

public class GrammarBuilder {

	public GrammarBuilder(final Namespace namespace, final String name) {
		this.grammar = new Grammar(namespace, name);
	}

	Grammar grammar;

	public Grammar get() {
		return this.grammar;
	}

	public RuleBuilder rule(final String name) {
		return new RuleBuilder(name, this.grammar);
	}

	public class RuleBuilder {

		public RuleBuilder(final String name, final Grammar grammar) {
			this.rule = new Rule(grammar, name);
			grammar.getRule().add(this.rule);
		}

		Rule rule;

		public void concatenation(final ConcatenationItem... sequence) {
			this.rule.setRhs(new ChoiceSimple(new Concatenation(sequence)));
		}

		public void choice(final ConcatenationItem... alternative) {
			final Concatenation[] alternativeConcats = new Concatenation[alternative.length];
			for (int i = 0; i < alternative.length; ++i) {
				alternativeConcats[i] = new Concatenation(alternative[i]);
			}
			this.rule.setRhs(new ChoiceSimple(alternativeConcats));
		}

		public void priorityChoice(final ConcatenationItem... alternative) {
			final Concatenation[] alternativeConcats = new Concatenation[alternative.length];
			for (int i = 0; i < alternative.length; ++i) {
				alternativeConcats[i] = new Concatenation(alternative[i]);
			}
			this.rule.setRhs(new ChoicePriority(alternativeConcats));
		}

		public void multi(final int min, final int max, final TangibleItem item) {
			this.rule.setRhs(new ChoiceSimple(new Concatenation(new Multi(min, max, item))));
		}

		public void separatedList(final int min, final int max, final TerminalLiteral separator, final TangibleItem item) {
			this.rule.setRhs(new ChoiceSimple(new Concatenation(new SeparatedList(min, max, separator, item))));
		}
	}

	public SkipRuleBuilder skip(final String name) {
		return new SkipRuleBuilder(name, this.grammar);
	}

	public class SkipRuleBuilder {

		public SkipRuleBuilder(final String name, final Grammar grammar) {
			this.rule = new SkipRule(grammar, name);
			grammar.getRule().add(this.rule);
		}

		Rule rule;

		public void concatination(final TangibleItem... sequence) {
			this.rule.setRhs(new ChoiceSimple(new Concatenation(sequence)));
		}

		public void choice(final Concatenation... alternative) {
			this.rule.setRhs(new ChoiceSimple(alternative));
		}

		public void multi(final int min, final int max, final TangibleItem item) {
			this.rule.setRhs(new ChoiceSimple(new Concatenation(new Multi(min, max, item))));
		}

		public void separatedList(final int min, final int max, final TerminalLiteral separator, final TangibleItem item) {
			this.rule.setRhs(new ChoiceSimple(new Concatenation(new SeparatedList(min, max, separator, item))));
		}

	}
}
