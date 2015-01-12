package net.akehurst.language.ogl.semanticModel;

import net.akehurst.language.core.parser.INodeType;


public abstract class RuleItem implements Visitable {

	Rule owningRule;
	public Rule getOwningRule() {
		return this.owningRule;
	}
	public abstract void setOwningRule(Rule value);
	
//	public abstract INodeType getNodeType();
	
//	public abstract Set<TangibleItem> findFirstTangibleItem();
//	
//	public abstract Set<Terminal> findFirstTerminal() throws RuleNotFoundException;
//
//	public abstract boolean isMatchedBy(INode node) throws RuleNotFoundException;
}
