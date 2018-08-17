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

public class GrammarBuilderDefault {

	public GrammarBuilderDefault(final NamespaceDefault namespace, final String name) {
		this.grammar = new GrammarDefault(namespace, name);
	}

	GrammarDefault grammar;

	public GrammarDefault get() {
		return this.grammar;
	}

	public RuleBuilder rule(final String name) {
		return new RuleBuilder(name, this.grammar);
	}

	public class RuleBuilder {

		public RuleBuilder(final String name, final GrammarDefault grammar) {
			this.rule = new RuleDefault(grammar, name);
			grammar.getRule().add(this.rule);
		}

		RuleDefault rule;

		public void concatenation(final ConcatenationItemAbstract... sequence) {
			this.rule.setRhs(new ChoiceSimpleDefault(new ConcatenationDefault(sequence)));
		}

		public void choice(final ConcatenationItemAbstract... alternative) {
			final ConcatenationDefault[] alternativeConcats = new ConcatenationDefault[alternative.length];
			for (int i = 0; i < alternative.length; ++i) {
				alternativeConcats[i] = new ConcatenationDefault(alternative[i]);
			}
			this.rule.setRhs(new ChoiceSimpleDefault(alternativeConcats));
		}

		public void priorityChoice(final ConcatenationItemAbstract... alternative) {
			final ConcatenationDefault[] alternativeConcats = new ConcatenationDefault[alternative.length];
			for (int i = 0; i < alternative.length; ++i) {
				alternativeConcats[i] = new ConcatenationDefault(alternative[i]);
			}
			this.rule.setRhs(new ChoicePriorityDefault(alternativeConcats));
		}

		public void multi(final int min, final int max, final TangibleItemAbstract item) {
			this.rule.setRhs(new ChoiceSimpleDefault(new ConcatenationDefault(new MultiDefault(min, max, item))));
		}

		public void separatedList(final int min, final int max, final TerminalLiteralDefault separator, final TangibleItemAbstract item) {
			this.rule.setRhs(new ChoiceSimpleDefault(new ConcatenationDefault(new SeparatedListDefault(min, max, separator, item))));
		}
	}

	public SkipRuleBuilder skip(final String name) {
		return new SkipRuleBuilder(name, this.grammar);
	}

	public class SkipRuleBuilder {

		public SkipRuleBuilder(final String name, final GrammarDefault grammar) {
			this.rule = new SkipRuleDefault(grammar, name);
			grammar.getRule().add(this.rule);
		}

		RuleDefault rule;

		public void concatenation(final TangibleItemAbstract... sequence) {
			this.rule.setRhs(new ChoiceSimpleDefault(new ConcatenationDefault(sequence)));
		}

		public void choice(final ConcatenationItemAbstract... alternative) {
			final ConcatenationDefault[] alternativeConcats = new ConcatenationDefault[alternative.length];
			for (int i = 0; i < alternative.length; ++i) {
				alternativeConcats[i] = new ConcatenationDefault(alternative[i]);
			}
			this.rule.setRhs(new ChoiceSimpleDefault(alternativeConcats));
		}

		public void multi(final int min, final int max, final TangibleItemAbstract item) {
			this.rule.setRhs(new ChoiceSimpleDefault(new ConcatenationDefault(new MultiDefault(min, max, item))));
		}

		public void separatedList(final int min, final int max, final TerminalLiteralDefault separator, final TangibleItemAbstract item) {
			this.rule.setRhs(new ChoiceSimpleDefault(new ConcatenationDefault(new SeparatedListDefault(min, max, separator, item))));
		}

	}
}
