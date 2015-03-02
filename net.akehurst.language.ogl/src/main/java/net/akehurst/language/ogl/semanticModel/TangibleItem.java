package net.akehurst.language.ogl.semanticModel;

import java.util.Set;

import net.akehurst.language.core.parser.INodeType;

public abstract class TangibleItem extends ConcatinationItem {

	@Override
	public void setOwningRule(Rule value) {
		this.owningRule = value;
	}
	
	public abstract INodeType getNodeType() throws RuleNotFoundException;

}
