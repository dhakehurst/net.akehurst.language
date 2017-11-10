package net.akehurst.language.parse.graph;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import net.akehurst.language.grammar.parser.runtime.RuntimeRule;
import net.akehurst.language.parse.graph.IGraphNode.PreviousInfo;
import net.akehurst.language.parser.sppf.Leaf;

public interface IParseGraph {

	List<ICompleteNode> getGoals();

	Collection<IGrowingNode> getGrowable();

	Collection<ICompleteNode> getCompleteNodes();

	ICompleteNode getCompleteNode(IGrowingNode gn);

	// void removeGrowable(IGraphNode node);

	// IGraphNode createLeaf(Leaf leaf, RuntimeRule terminalRule, int startPosition, int matchedLength);

	void createStart(RuntimeRule goalRule);

	ICompleteNode findOrCreateLeaf(Leaf leaf);

	// ICompleteNode findOrCreateBranch(RuntimeRule rr, int priority, int startPosition, int endPosition);

	ICompleteNode findNode(int ruleNumber, int start, int length);

	ICompleteNode complete(IGrowingNode growing);

	// IGraphNode findOrCreateBranch(RuntimeRule rr, int priority, int startPosition, int machedTextLength, int nextItemIndex, int height);

	void reuseWithOtherStack(IGraphNode node, Set<PreviousInfo> previous);

	/**
	 * Use to grow the height of a tree. Creates a new node. Add the child. Then add the stack of the child to the new node.
	 *
	 * Will add to growable - if can grow Will register node - if it is complete
	 *
	 * @param runtimeRule
	 * @param priority
	 * @param firstChild
	 * @return
	 */
	void createWithFirstChild(RuntimeRule runtimeRule, int priority, ICompleteNode firstChild, Set<IGrowingNode.PreviousInfo> previous);

	// void createWithFirstChildAndStack(RuntimeRule runtimeRule, int priority, IGraphNode firstChild, IGraphNode stack);

	/**
	 * adds the next child to the parent
	 *
	 * @param parent
	 * @param nextChild
	 * @return
	 */
	void growNextChild(IGrowingNode parent, ICompleteNode nextChild, int position);

	void growNextSkipChild(IGrowingNode parent, ICompleteNode skipChild);

	void pushToStackOf(ICompleteNode leafNode, IGrowingNode stack, Set<IGrowingNode.PreviousInfo> previous);

	/**
	 *
	 * @param gn
	 * @return previous of gn after giving gn a new Set of previous (clear)
	 */
	Set<IGrowingNode.PreviousInfo> pop(IGrowingNode gn);

	// IGraphNode fetchGrowing(int ruleNumber, int start, int nextItemIndex);
	//
	// IGraphNode fetchNode(int ruleNumber, int start, int length);

}
