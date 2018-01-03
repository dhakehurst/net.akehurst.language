package net.akehurst.language.parse.graph;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import net.akehurst.language.grammar.parser.runtime.RuntimeRule;
import net.akehurst.language.parser.sppf.Leaf;

public interface IParseGraph {

	List<ICompleteNode> getGoals();

	Collection<IGrowingNode> getGrowing();

	Collection<IGrowingNode> getGrowingHead();

	Collection<ICompleteNode> getCompleteNodes();

	ICompleteNode getCompleteNode(IGrowingNode gn);

	void createStart(RuntimeRule goalRule);

	/**
	 * Used to grow the width, i.e. to consume more input (in parser speak this is know as a shift). The next leaf is consumed from an IInput and provided as
	 * input here so that the graph can return an already exiting graph node if one exists.
	 *
	 * @param leaf
	 * @return
	 */
	ICompleteNode findOrCreateLeaf(Leaf leaf);

	ICompleteNode findNode(int ruleNumber, int start, int length);

	ICompleteNode complete(IGrowingNode growing);

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

	/**
	 * add a node to be a head of the stack, should only be called with nodes that were in a 'previous' set, but nothing happened to them
	 *
	 * @param node
	 */
	void makeHead(IGrowingNode node);

	// IGraphNode fetchGrowing(int ruleNumber, int start, int nextItemIndex);
	//
	// IGraphNode fetchNode(int ruleNumber, int start, int length);

}
