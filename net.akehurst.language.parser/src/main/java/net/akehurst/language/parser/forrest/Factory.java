package net.akehurst.language.parser.forrest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.akehurst.language.core.parser.IBranch;
import net.akehurst.language.core.parser.INode;
import net.akehurst.language.core.parser.INodeType;

public class Factory {

	public Factory() {
		this.map = new HashMap<>();
		this.nextI = 0;
	}
	
	int nextI;
	Map<INodeType, Integer> map;
	private int getNodeType(INodeType nodeType) {
		Integer i = this.map.get(nodeType);
		if (null==i) {
			i = nextI++;
			this.map.put(nodeType, i);
		}
		return i;
	}
	
	public IBranch createBranch(INodeType nodeType, List<INode> children) {
		int nodeTypeNumber = this.getNodeType(nodeType);
		Branch b = new Branch(this, nodeTypeNumber, nodeType, children);
		return b;
	}
	
}
