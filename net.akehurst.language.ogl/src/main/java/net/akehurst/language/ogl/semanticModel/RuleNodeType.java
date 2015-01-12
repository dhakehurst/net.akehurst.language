package net.akehurst.language.ogl.semanticModel;

import net.akehurst.language.core.parser.INodeIdentity;
import net.akehurst.language.core.parser.INodeType;

public class RuleNodeType implements INodeType {
	
	public RuleNodeType(Rule rule) {
		this.identity = new NodeIdentity(rule.getName());
	}

	INodeIdentity identity;
	@Override
	public INodeIdentity getIdentity() {
		return this.identity;
	}

	//--- Object ---
	@Override
	public String toString() {
		return this.getIdentity().asPrimitive();
	}
	
	@Override
	public int hashCode() {
		return this.getIdentity().hashCode();
	}
	
	@Override
	public boolean equals(Object arg) {
		if (arg instanceof RuleNodeType) {
			RuleNodeType other = (RuleNodeType)arg;
			return this.getIdentity().equals(other.getIdentity());
		} else {
			return false;
		}
	}
}
