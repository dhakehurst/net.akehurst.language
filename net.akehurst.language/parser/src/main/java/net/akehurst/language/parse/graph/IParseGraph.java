package net.akehurst.language.parse.graph;

import java.util.Collection;
import java.util.List;

import net.akehurst.language.grammar.parse.tree.Leaf;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;

public interface IParseGraph {

	List<IGraphNode> getGoals();
	Collection<IGraphNode> getGrowable();
	Collection<IGraphNode> getCompleteNodes();

	void removeGrowable(IGraphNode node);
	
	IGraphNode createLeaf(Leaf leaf, RuntimeRule terminalRule, int startPosition, int matchedLength);
	IGraphNode findOrCreateLeaf(Leaf leaf, RuntimeRule terminalRule, int startPosition, int matchedLength);

	IGraphNode createBranch(RuntimeRule rr, int priority, int startPosition, int length, int nextItemIndex, int height);
	IGraphNode findCompleteNode(int ruleNumber, int start, int length);
	IGraphNode findOrCreateBranch(RuntimeRule rr, int priority, int startPosition, int machedTextLength, int nextItemIndex, int height);

	/**
	 * Use to grow the height of a tree.
	 * Creates a new node.
	 * Add the child.
	 * Then add the stack of the child to the new node.
	 * 
	 * Will add to growable - if can grow
	 * Will register node - if it is complete
	 * 
	 * @param runtimeRule
	 * @param priority
	 * @param firstChild
	 * @return
	 */
	IGraphNode createWithFirstChild(RuntimeRule runtimeRule, int priority, IGraphNode firstChild);



//	IGraphNode fetchGrowing(int ruleNumber, int start, int nextItemIndex);
//
//	IGraphNode fetchNode(int ruleNumber, int start, int length);

}
