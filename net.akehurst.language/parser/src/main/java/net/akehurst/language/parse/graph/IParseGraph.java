package net.akehurst.language.parse.graph;

import java.util.Collection;
import java.util.List;

import net.akehurst.language.grammar.parser.forrest.NodeIdentifier;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;

public interface IParseGraph {

	IParseGraph shallowClone();

	Collection<IGraphNode> getGrowable();
	void addGrowable(IGraphNode value);

	IGraphNode createLeaf(IGraphNode parent, RuntimeRule terminalRule, int position);

	IGraphNode createBranch(IGraphNode parent, RuntimeRule rr, int priority, int startPosition, int length, int nextItemIndex);

	
	/**
	 * find a node with the given identifier
	 * 
	 * @param identifier
	 * @return the node with the given identifier
	 */
	IGraphNode peek(NodeIdentifier identifier);

//	List<IGraphNode> getChildren(IGraphNode parent);

	void registerCompleteNode(IGraphNode node);

	List<IGraphNode> getNodes();
	
}
