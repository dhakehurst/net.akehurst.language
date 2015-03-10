package net.akehurst.language.ogl.semanticModel;

import net.akehurst.language.core.parser.INodeIdentity;
import net.akehurst.language.core.parser.INodeType;

public class LeafNodeType implements INodeType {

	public LeafNodeType() {
		this.identity = new NodeIdentity("''");
	}
	
	public LeafNodeType(TerminalLiteral terminal) {
		this.identity = new NodeIdentity("'"+terminal.getValue()+"'");
	}
	
	public LeafNodeType(TerminalPattern terminal) {
		this.identity = new NodeIdentity("\""+terminal.getValue()+"\"");
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
		if (arg instanceof LeafNodeType) {
			LeafNodeType other = (LeafNodeType)arg;
			return this.getIdentity().equals(other.getIdentity());
		} else {
			return false;
		}
	}
}
