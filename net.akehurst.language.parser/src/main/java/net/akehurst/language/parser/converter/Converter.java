package net.akehurst.language.parser.converter;


import net.akehurst.language.parser.runtime.Factory;
import net.akehurst.transform.binary.AbstractTransformer;
import net.akehurst.transform.binary.Relation;

public class Converter extends AbstractTransformer {

	public Converter(Factory factory) {
		this.factory = factory;
		
		this.registerRule(Grammar2RuntimeRuleSet.class);
		this.registerRule(Rule2RuntimeRule.class);
		this.registerRule((Class<? extends Relation<?, ?>>) AbstractRuleItem2RuntimeRuleItem.class);
		this.registerRule(Choice2RuntimeRuleItem.class);
		this.registerRule(Concatenation2RuntimeRuleItem.class);
		this.registerRule(Multi2RuntimeRuleItem.class);
		this.registerRule(SeparatedList2RuntimeRuleItem.class);
		this.registerRule((Class<? extends Relation<?, ?>>)AbstractTangibleItem2RuntimeRule.class);
		this.registerRule(NonTerminal2RuntimeRule.class);
		this.registerRule(Terminal2RuntimeRule.class);
	}
	
	Factory factory;
	public Factory getFactory() {
		return this.factory;
	}
	
}
