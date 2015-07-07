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
package net.akehurst.language.ogl.semanticModel;

public class GrammarBuilder {

	public GrammarBuilder(Namespace namespace, String name) {
		this.grammar = new Grammar(namespace, name);
	}

	Grammar grammar;

	public Grammar get() {
		return this.grammar;
	}

	public RuleBuilder rule(String name) {
		return new RuleBuilder(name, this.grammar);
	}

	public class RuleBuilder {

		public RuleBuilder(String name, Grammar grammar) {
			this.rule = new Rule(grammar, name);
			grammar.getRule().add(this.rule);
		}

		Rule rule;

		public void concatenation(TangibleItem... sequence) {
			this.rule.setRhs(new Concatenation(sequence));
		}

		public void choice(TangibleItem... alternative) {
			this.rule.setRhs(new Choice(alternative));
		}

		public void multi(int min, int max, TangibleItem item) {
			this.rule.setRhs(new Multi(min, max, item));
		}

		public void separatedList(int min, int max, TerminalLiteral separator, TangibleItem item) {
			this.rule.setRhs(new SeparatedList(min, max, separator, item));
		}
	}

	public SkipRuleBuilder skip(String name) {
		return new SkipRuleBuilder(name, this.grammar);
	}

	public class SkipRuleBuilder {

		public SkipRuleBuilder(String name, Grammar grammar) {
			this.rule = new SkipRule(grammar, name);
			grammar.getRule().add(this.rule);
		}

		Rule rule;

		public void concatination(TangibleItem... sequence) {
			this.rule.setRhs(new Concatenation(sequence));
		}

		public void choice(TangibleItem... alternative) {
			this.rule.setRhs(new Choice(alternative));
		}

		public void multi(int min, int max, TangibleItem item) {
			this.rule.setRhs(new Multi(min, max, item));
		}

		public void separatedList(int min, int max, TerminalLiteral separator, TangibleItem item) {
			this.rule.setRhs(new SeparatedList(min, max, separator, item));
		}

	}
}
