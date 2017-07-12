package net.akehurst.language.grammar.parser.runtime;

import net.akehurst.language.ogl.semanticStructure.AbstractChoice;
import net.akehurst.language.ogl.semanticStructure.Grammar;
import net.akehurst.language.ogl.semanticStructure.Rule;

public class RuleForGroup extends Rule {

	public RuleForGroup(final Grammar grammar, final String name, final AbstractChoice choice) {
		super(grammar, name);
		this.choice = choice;
	}

	private final AbstractChoice choice;

	@Override
	public AbstractChoice getRhs() {
		return this.choice;
	}

}
