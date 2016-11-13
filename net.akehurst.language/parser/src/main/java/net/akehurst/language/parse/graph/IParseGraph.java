package net.akehurst.language.parse.graph;

import java.util.List;

import net.akehurst.language.core.parser.INodeIdentity;
import net.akehurst.language.grammar.parser.forrest.NodeIdentifier;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;

public interface IParseGraph {

	IParseGraph shallowClone();

	List<IGraphNode> getGrowable();

	IGraphNode createLeaf(RuntimeRule terminalRule, int position);

	IGraphNode createBranch(RuntimeRule rr, int priority, IGraphNode firstChild, int nextItemIndex);

	
	/**
	 * find a node with the given identifier
	 * 
	 * @param identifier
	 * @return the node with the given identifier
	 */
	IGraphNode peek(NodeIdentifier identifier);
	
}
