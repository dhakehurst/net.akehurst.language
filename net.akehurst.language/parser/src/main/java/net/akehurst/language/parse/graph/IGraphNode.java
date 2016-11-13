package net.akehurst.language.parse.graph;

import java.util.List;

import net.akehurst.language.core.parser.INode;
import net.akehurst.language.grammar.parser.forrest.NodeIdentifier;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;
import net.akehurst.language.ogl.semanticStructure.Grammar;

public interface IGraphNode {

	NodeIdentifier getIdentifier();

	RuntimeRule getRuntimeRule();

	int getPriority();

	int getStartPosition();

	int getEndPosition();

	int getMatchedTextLength();
	
	boolean getIsEmpty();

	boolean getIsLeaf();


	/**
	 * isStacked then true else canGrowWidth
	 * 
	 * @return
	 */
	boolean getCanGrow();

	boolean getIsSkip();

	boolean getIsComplete();

	/**
	 * isComplete && isStacked
	 * 
	 * @return
	 */
	boolean getCanGraftBack();

	boolean getCanGrowWidth();

	boolean getIsStacked();

	boolean hasNextExpectedItem();

	RuntimeRule getNextExpectedItem();

	List<IGraphNode> getChildren();

	
	/**
	 * push this node onto the stack of next
	 * 
	 * @param next
	 * @return next with this node as its stack
	 */
	IGraphNode pushToStackOf(IGraphNode next);

	/**
	 * return list of things that are stacked previous to this one
	 * 
	 * @return
	 */
	List<IGraphNode> getPrevious();

	IGraphNode addChild(IGraphNode gn);

	IGraphNode addSkipChild(IGraphNode gn);

	IGraphNode replace(IGraphNode newNode);




}
