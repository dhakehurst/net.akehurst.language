package net.akehurst.language.parser.forrest;

import net.akehurst.language.core.parser.INode;
import net.akehurst.language.core.parser.INodeType;
import net.akehurst.language.core.parser.ParseTreeException;

abstract
public class Node implements INode {

	public Node(Factory factory, int nodeTypeNumber, INodeType nodeType) {
		this.factory = factory;
		this.nodeTypeNumber = nodeTypeNumber;
		this.nodeType = nodeType;
	}
	
	Factory factory;
	
	int nodeTypeNumber;
	public int getNodeTypeNumber() {
		return this.nodeTypeNumber;
	}

	INodeType nodeType;
	@Override
	public INodeType getNodeType() throws ParseTreeException {
		return this.nodeType;
	}
}
