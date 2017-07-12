package net.akehurst.language.grammar.parser;

import java.util.Objects;

import net.akehurst.language.core.analyser.IGrammar;
import net.akehurst.language.core.analyser.INonTerminal;
import net.akehurst.language.core.analyser.IRule;
import net.akehurst.language.core.parser.RuleNotFoundException;

public class NonTerminalRuleReference implements INonTerminal {

	public NonTerminalRuleReference(final IGrammar grammar, final String ruleName) {
		this.grammar = grammar;
		this.ruleName = ruleName;
	}

	private final IGrammar grammar;

	private final String ruleName;

	@Override
	public IRule getOwningRule() {
		// there is no owning rule!
		return null;
	}

	@Override
	public IRule getReferencedRule() throws RuleNotFoundException {
		return this.grammar.findAllRule(this.ruleName);
	}

	@Override
	public String getName() {
		return this.ruleName;
	}

	@Override
	public int hashCode() {
		return this.ruleName.hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof INonTerminal) {
			final INonTerminal other = (INonTerminal) obj;
			return Objects.equals(this.getName(), other.getName());
		} else {
			return false;
		}
	}

	@Override
	public String toString() {
		return this.ruleName;
	}
}
