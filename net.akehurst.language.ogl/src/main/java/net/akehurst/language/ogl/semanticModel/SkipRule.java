package net.akehurst.language.ogl.semanticModel;

import net.akehurst.language.core.parser.INodeType;

public class SkipRule extends Rule {

	public SkipRule(Grammar grammar, String name) {
		super(grammar, name);
	}
	
	@Override
	public INodeType getNodeType() {
		return new SkipNodeType(this);
	}
	
	//--- Object ---
	@Override
	public String toString() {
		return this.getName()+" ?= "+this.getRhs();
	}
	
	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}
	
	@Override
	public boolean equals(Object arg) {
		if (arg instanceof SkipRule) {
			SkipRule other = (SkipRule)arg;
			return this.toString().equals(other.toString());
		} else {
			return false;
		}
	}
}
