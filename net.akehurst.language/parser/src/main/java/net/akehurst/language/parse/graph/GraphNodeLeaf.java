package net.akehurst.language.parse.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import net.akehurst.language.grammar.parse.tree.Leaf;
import net.akehurst.language.grammar.parser.forrest.NodeIdentifier;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;

public class GraphNodeLeaf extends AbstractGraphNode implements IGraphNode {

	public GraphNodeLeaf(IParseGraph graph, IGraphNode parent, Leaf leaf) {
		super(graph, parent);
		this.parent = parent;
//		int parentRuleNumber = null==parent ? -1 : parent.getRuntimeRule().getRuleNumber();
		int parentRuleNumber = null==parent ? -1 : parent.hashCode();
		this.hashCode_cache = Objects.hash(leaf.getRuntimeRule().getRuleNumber(), leaf.getStart(), leaf.getMatchedTextLength(),parentRuleNumber);
		this.leaf = leaf;
		this.nextInputPosition = leaf.getEnd();
	}
	
	@Override
	public IGraphNode duplicateWithNextChild(IGraphNode nextChild) {
		throw new RuntimeException("Internal Error: Should never happen");
	}
	@Override
	public IGraphNode duplicateWithNextSkipChild(IGraphNode nextChild) {
		throw new RuntimeException("Internal Error: Should never happen");
	}
	
	Leaf leaf;

	int nextInputPosition;
	

	@Override
	public boolean getIsLeaf() {
		return true;
	}

	@Override
	public int getNextItemIndex() {
		return -1;
	}
	
	@Override
	public boolean getIsEmpty() {
		return this.getRuntimeRule().getIsEmptyRule();
	}

	@Override
	public RuntimeRule getRuntimeRule() {
		return leaf.getRuntimeRule();
	}

	@Override
	public int getPriority() {
		return 0;
	}

	@Override
	public int getStartPosition() {
		return this.leaf.getStart();
	}

//	@Override
//	public int getEndPosition() {
//		return this.leaf.getEnd();
//	}

	@Override
	public int getNextInputPosition() {
		return this.nextInputPosition;
	}
	
	@Override
	public int getMatchedTextLength() {
		return this.leaf.getMatchedTextLength();
	}
	
	@Override
	public boolean getCanGrow() {
		return this.getIsStacked();
	}

	@Override
	public boolean getIsSkip() {
		return this.getRuntimeRule().getIsSkipRule();
	}

	@Override
	public boolean getIsComplete() {
		return true;
	}

	@Override
	public boolean getCanGraftBack() {
		return this.getIsComplete() && this.getIsStacked();
	}

	@Override
	public boolean getCanGrowWidth() {
		return false;
	}

	@Override
	public boolean getIsStacked() {
		return !this.getPrevious().isEmpty();
	}

	@Override
	public boolean hasNextExpectedItem() {
		return false;
	}


	@Override
	public RuntimeRule getNextExpectedItem() {
		throw new RuntimeException("Internal Error: Should never happen");
	}
	
	@Override
	public RuntimeRule getExpectedItemAt(int atPosition) {
		throw new RuntimeException("Internal Error: Should never happen");
	}
	
	@Override
	public List<IGraphNode> getChildren() {
		return Collections.emptyList();
	}

//	@Override
//	public IGraphNode addNextChild(IGraphNode gn) {
//		throw new RuntimeException("Internal Error: Should never happen");
//	}
//
//	@Override
//	public IGraphNode addSkipChild(IGraphNode gn) {
//		throw new RuntimeException("Internal Error: Should never happen");
//	}
//
//	@Override
//	public IGraphNode replace(IGraphNode newParent) {
//		// TODO Auto-generated method stub
//		return null;
//	}

	@Override
	public String toString() {
		return "'"+this.getRuntimeRule().getTerminalPatternText()+"'"
	    + "("+this.leaf.getRuntimeRule().getRuleNumber()+"," +this.leaf.getStart()+","+this.leaf.getMatchedTextLength()+")"
		+ (this.getPrevious().isEmpty() ? "" : " -> " + this.getPrevious().get(0));
	}
	
}
