package net.akehurst.language.grammar.parser.converter;

import net.akehurst.language.ogl.semanticStructure.AbstractChoice;
import net.akehurst.language.ogl.semanticStructure.Grammar;
import net.akehurst.language.ogl.semanticStructure.Rule;

public class RuleForGroup extends Rule {

	public RuleForGroup(Grammar grammar, String name, AbstractChoice choice) {
		super(grammar, name);
		this.choice = choice;
	}
	
	AbstractChoice choice;
	
	@Override
	public AbstractChoice getRhs() {
		return this.choice;
	}
	
}
