package net.akehurst.language.ogl.semanticModel;

import java.util.Set;



public abstract class RuleItem implements Visitable {

	Rule owningRule;
	public Rule getOwningRule() {
		return this.owningRule;
	}
	public abstract void setOwningRule(Rule value);
	
	public abstract Set<Terminal> findAllTerminal();
	public abstract Set<NonTerminal> findAllNonTerminal();
	
}
