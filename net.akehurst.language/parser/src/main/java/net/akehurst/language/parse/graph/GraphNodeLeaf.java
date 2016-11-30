package net.akehurst.language.parse.graph;

import java.util.Collections;
import java.util.List;

import net.akehurst.language.grammar.parse.tree.Leaf;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;

public class GraphNodeLeaf extends AbstractGraphNode implements IGraphNode {

	// public GraphNodeLeaf(ParseGraph graph, RuntimeRule runtimeRule, int startPosition, int machedTextLength) {
	public GraphNodeLeaf(ParseGraph graph, Leaf leaf) {
		super(graph, leaf.getRuntimeRule(), leaf.getStart(), leaf.getMatchedTextLength());
		this.leaf = leaf;
	}

	Leaf leaf;

	@Override
	public IGraphNode duplicateWithNextChild(IGraphNode nextChild) {
		throw new RuntimeException("Internal Error: Should never happen");
	}

	@Override
	public IGraphNode duplicateWithNextSkipChild(IGraphNode nextChild) {
		throw new RuntimeException("Internal Error: Should never happen");
	}

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
	public int getPriority() {
		return 0;
	}

	@Override
	public int getHeight() {
		return 1;
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
	public List<RuntimeRule> getNextExpectedTerminals() {
		return Collections.emptyList();
	}

	@Override
	public List<RuntimeRule> getNextExpectedItem() {
		throw new RuntimeException("Internal Error: Should never happen");
	}

	@Override
	public RuntimeRule getExpectedItemAt(int atPosition) {
		throw new RuntimeException("Internal Error: Should never happen");
	}

	@Override
	public boolean getExpectsItemAt(RuntimeRule item, int atPosition) {
		return false;
	}
	
	@Override
	public List<IGraphNode> getChildren() {
		return Collections.emptyList();
	}

	@Override
	public String toString() {
		return "'" + this.getRuntimeRule().getTerminalPatternText() + "'" + "(" + this.getRuntimeRule().getRuleNumber() + "," + this.getStartPosition() + ","
				+ this.getMatchedTextLength() + ")" + (this.getPrevious().isEmpty() ? "" : " -> " + this.getPrevious().get(0));
	}

}
