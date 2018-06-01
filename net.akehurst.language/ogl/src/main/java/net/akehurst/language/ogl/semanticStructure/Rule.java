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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import net.akehurst.language.core.grammar.IRule;
import net.akehurst.language.core.grammar.GrammarRuleNotFoundException;
import net.akehurst.language.core.grammar.INodeType;

public class Rule implements IRule {

	public Rule(final GrammarStructure grammar, final String name) {
		this.grammar = grammar;
		this.name = name;
	}

	private final GrammarStructure grammar;
	private final String name;
	private AbstractChoice rhs;

	public GrammarStructure getGrammar() {
		return this.grammar;
	}

	@Override
	public String getName() {
		return this.name;
	}

	public AbstractChoice getRhs() {
		return this.rhs;
	}

	public void setRhs(final AbstractChoice value) {
		this.rhs = value;
		final ArrayList<Integer> nextIndex0 = new ArrayList<>();
		nextIndex0.add(0);
		this.rhs.setOwningRule(this, nextIndex0);
	}

	public Set<Terminal> findAllSubTerminal() throws GrammarRuleNotFoundException {
		final Set<Terminal> result = new HashSet<>();
		result.addAll(this.getRhs().findAllTerminal());
		for (final Rule r : this.findAllSubRule()) {
			result.addAll(r.getRhs().findAllTerminal());
		}
		return result;
	}

	public Set<NonTerminal> findAllSubNonTerminal() throws GrammarRuleNotFoundException {
		final Set<NonTerminal> result = this.getRhs().findAllNonTerminal();
		Set<NonTerminal> oldResult = new HashSet<>();
		while (!oldResult.containsAll(result)) {
			oldResult = new HashSet<>();
			oldResult.addAll(result);
			for (final NonTerminal nt : oldResult) {
				final Set<NonTerminal> newNts = nt.getReferencedRule().getRhs().findAllNonTerminal();
				newNts.removeAll(result);
				result.addAll(newNts);
			}
		}
		return result;
	}

	public Set<Rule> findAllSubRule() throws GrammarRuleNotFoundException {
		final Set<Rule> result = new HashSet<>();
		for (final NonTerminal nt : this.findAllSubNonTerminal()) {
			result.add(nt.getReferencedRule());
		}
		return result;
	}

	public INodeType getNodeType() {
		return new RuleNodeType(this);
	}

	// --- Object ---
	@Override
	public String toString() {
		return this.getName() + " : " + this.getRhs();
	}

	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}

	@Override
	public boolean equals(final Object arg) {
		if (arg instanceof Rule) {
			final Rule other = (Rule) arg;
			return this.toString().equals(other.toString());
		} else {
			return false;
		}
	}
}
