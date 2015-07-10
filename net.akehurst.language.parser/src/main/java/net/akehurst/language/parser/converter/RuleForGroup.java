package net.akehurst.language.parser.converter;

import net.akehurst.language.ogl.semanticStructure.AbstractChoice;
import net.akehurst.language.ogl.semanticStructure.ChoiceSimple;
import net.akehurst.language.ogl.semanticStructure.Concatenation;
import net.akehurst.language.ogl.semanticStructure.Grammar;
import net.akehurst.language.ogl.semanticStructure.Rule;

public class RuleForGroup extends Rule {

	public RuleForGroup(Grammar grammar, AbstractChoice choice) {
		super(grammar, "$group$");
		this.choice = choice;
	}
	
	AbstractChoice choice;
	
	@Override
	public AbstractChoice getRhs() {
		return this.choice;
	}
	
}
