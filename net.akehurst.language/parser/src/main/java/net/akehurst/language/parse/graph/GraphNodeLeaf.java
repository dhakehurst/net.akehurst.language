package net.akehurst.language.parse.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.akehurst.language.core.parser.INode;
import net.akehurst.language.core.parser.INodeIdentity;
import net.akehurst.language.grammar.parse.tree.Leaf;
import net.akehurst.language.grammar.parser.forrest.NodeIdentifier;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;

public class GraphNodeLeaf implements IGraphNode {

	public GraphNodeLeaf(Leaf leaf) {
		this.leaf = leaf;
		this.identifier = new NodeIdentifier(leaf.getRuntimeRule().getRuleNumber(), leaf.getStart(), leaf.getEnd(), -1);
		this.previous = new ArrayList<>();
	}

	Leaf leaf;
	NodeIdentifier identifier;

	List<IGraphNode> previous;
	
	@Override
	public NodeIdentifier getIdentifier() {
		return this.identifier;
	}

	@Override
	public boolean getIsLeaf() {
		return true;
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

	@Override
	public int getEndPosition() {
		return this.leaf.getEnd();
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
	public List<IGraphNode> getChildren() {
		return Collections.emptyList();
	}
	
	@Override
	public IGraphNode pushToStackOf(IGraphNode next) {
		next.getPrevious().add(this);
		return next;
	}

	@Override
	public List<IGraphNode> getPrevious() {
		return this.previous;
	}

	@Override
	public IGraphNode addChild(IGraphNode gn) {
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
		return this.getIdentifier().toString();
	}
	
}
