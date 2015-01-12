package net.akehurst.language.ogl.semanticModel;

import net.akehurst.language.core.parser.INodeIdentity;

public class NodeIdentity implements INodeIdentity {

	public NodeIdentity(String value) {
		this.primitive = value;
	}

	String primitive;
	@Override
	public String asPrimitive() {
		return this.primitive;
	}

	//--- Object ---
	@Override
	public String toString() {
		return this.asPrimitive();
	}
	
	@Override
	public int hashCode() {
		return this.asPrimitive().hashCode();
	}
	
	@Override
	public boolean equals(Object arg) {
		if (arg instanceof NodeIdentity) {
			NodeIdentity other = (NodeIdentity)arg;
			return this.asPrimitive().equals(other.asPrimitive());
		} else {
			return false;
		}
	}
}
