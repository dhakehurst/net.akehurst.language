package net.akehurst.language.parser;

import net.akehurst.language.core.parser.INode;
import net.akehurst.language.core.parser.INodeType;

public abstract class AbstractNode implements INode {

	public AbstractNode(INodeType nodeType) {
		this.nodeType = nodeType;
	}
	
	INodeType nodeType;
	@Override
	public INodeType getNodeType() {
		return this.nodeType;
	}
	
	String name;
	@Override
	public String getName() {
		return this.getNodeType().getIdentity().asPrimitive();
	}

	int length;
	@Override
	public int getLength() {
		return this.length;
	}

	public abstract AbstractNode deepClone();
}
