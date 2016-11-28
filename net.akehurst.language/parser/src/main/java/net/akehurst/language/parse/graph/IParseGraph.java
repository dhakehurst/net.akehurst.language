package net.akehurst.language.parse.graph;

import java.util.Collection;
import java.util.Set;

import net.akehurst.language.grammar.parse.tree.Leaf;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;

public interface IParseGraph {

	Collection<IGraphNode> getGrowable();
	Collection<IGraphNode> getCompleteNodes();

	void removeGrowable(IGraphNode node);
	
	IGraphNode createLeaf(Leaf leaf, RuntimeRule terminalRule, int startPosition, int matchedLength);
	IGraphNode findOrCreateLeaf(Leaf leaf, RuntimeRule terminalRule, int startPosition, int matchedLength);

	IGraphNode createBranch(RuntimeRule rr, int priority, int startPosition, int length, int nextItemIndex, int height);
	IGraphNode findCompleteNode(int ruleNumber, int start, int length);
	IGraphNode findOrCreateBranch(RuntimeRule rr, int priority, int startPosition, int machedTextLength, int nextItemIndex, int height);

	IGraphNode createWithFirstChild(RuntimeRule runtimeRule, int priority, IGraphNode firstChild);


//	IGraphNode fetchGrowing(int ruleNumber, int start, int nextItemIndex);
//
//	IGraphNode fetchNode(int ruleNumber, int start, int length);

}
