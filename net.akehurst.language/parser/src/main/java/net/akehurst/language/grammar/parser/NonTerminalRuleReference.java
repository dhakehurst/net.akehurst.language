package net.akehurst.language.grammar.parser;

import java.util.Objects;

import net.akehurst.language.core.grammar.IGrammar;
import net.akehurst.language.core.grammar.INonTerminal;
import net.akehurst.language.core.grammar.IRule;
import net.akehurst.language.core.grammar.IRuleItem;
import net.akehurst.language.core.grammar.GrammarRuleNotFoundException;
import net.akehurst.language.ogl.semanticStructure.Visitable;
import net.akehurst.language.ogl.semanticStructure.Visitor;

public class NonTerminalRuleReference implements INonTerminal, Visitable {

	public NonTerminalRuleReference(final IGrammar grammar, final String ruleName) {
		this.grammar = grammar;
		this.ruleName = ruleName;
	}

	private final IGrammar grammar;

	private final String ruleName;

	@Override
	public IRuleItem getSubItem(final int i) {
		// Terminals and NonTerminals do not have sub items
		return null;
	}

	@Override
	public IRule getOwningRule() {
		// there is no owning rule!
		return null;
	}

	@Override
	public IRule getReferencedRule() throws GrammarRuleNotFoundException {
		return this.grammar.findAllRule(this.ruleName);
	}

	@Override
	public String getName() {
		return this.ruleName;
	}

	// --- Visitable ---
	@Override
	public <T, E extends Throwable> T accept(final Visitor<T, E> visitor, final Object... arg) throws E {
		return visitor.visit(this, arg);
	}

	// --- Object ---
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
