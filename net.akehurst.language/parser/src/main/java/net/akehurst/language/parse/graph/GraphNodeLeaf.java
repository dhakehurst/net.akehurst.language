package net.akehurst.language.parse.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.akehurst.language.grammar.parse.tree.Leaf;
import net.akehurst.language.grammar.parser.forrest.NodeIdentifier;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;

public class GraphNodeLeaf extends AbstractGraphNode implements IGraphNode {

	public GraphNodeLeaf(Leaf leaf) {
		this.leaf = leaf;
		this.nextInputPosition = leaf.getEnd();
	}
	
	@Override
	public IGraphNode duplicate() {
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
		// TODO Auto-generated method stub
		return false;
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

	@Override
	public IGraphNode addNextChild(IGraphNode gn) {
		throw new RuntimeException("Internal Error: Should never happen");
	}

	@Override
	public IGraphNode addSkipChild(IGraphNode gn) {
		throw new RuntimeException("Internal Error: Should never happen");
	}

	@Override
	public IGraphNode replace(IGraphNode newParent) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String toString() {
		return "'"+this.getRuntimeRule().getTerminalPatternText()+"'"
	    + "("+this.leaf.getRuntimeRule().getRuleNumber()+"," +this.leaf.getStart()+","+this.leaf.getMatchedTextLength()+")"
		+ (this.getPrevious().isEmpty() ? "" : " -> " + this.getPrevious().get(0));
	}
	
}
