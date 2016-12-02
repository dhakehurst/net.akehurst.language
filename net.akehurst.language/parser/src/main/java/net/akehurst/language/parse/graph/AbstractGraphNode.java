package net.akehurst.language.parse.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import net.akehurst.language.grammar.parser.forrest.NodeIdentifier;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;

abstract public class AbstractGraphNode implements IGraphNode {

	public AbstractGraphNode(ParseGraph graph, RuntimeRule runtimeRule, int startPosition, int matchedTextLength) {
		this.graph = graph;
		this.runtimeRule = runtimeRule;
		this.startPosition = startPosition;
		this.matchedTextLength = matchedTextLength;
		this.previous = new ArrayList<>();
	}

	protected ParseGraph graph;
	protected RuntimeRule runtimeRule;
	protected int startPosition;
	protected int matchedTextLength;
	private List<PreviousInfo> previous;
	int stackHash;

	@Override
	public RuntimeRule getRuntimeRule() {
		return this.runtimeRule;
	}

	@Override
	public int getStartPosition() {
		return this.startPosition;
	}

	@Override
	public int getMatchedTextLength() {
		return this.matchedTextLength;
	}

	@Override
	public int getNextInputPosition() {
		return this.startPosition + this.matchedTextLength;
	}

	@Override
	public int getStackHash() {
		//TODO: pre-cache this value when stack changes
		if (0==this.stackHash && !this.getPrevious().isEmpty()) {
			for(PreviousInfo prev: this.getPrevious()) {
				this.stackHash = Objects.hash(prev.node.getRuntimeRule().getRuleNumber(), prev.node.getStackHash());
			}
		}
		return this.stackHash;
	}
	
	@Override
	public List<PreviousInfo> getPrevious() {
		return this.previous;
	}
	//
	// IGraphNode parent;
	// @Override
	// public IGraphNode getParent() {
	// return this.parent;
	// }

	@Override
	public IGraphNode pushToStackOf(IGraphNode next, int atPosition) {
		next.getPrevious().add(new PreviousInfo(this, atPosition));

		this.graph.tryAddGrowable(next);

		return next;
	}

	@Override
	public int hashCode() {
		throw new RuntimeException("GraphNodes are not comparible");
	}

	@Override
	public boolean equals(Object obj) {
		throw new RuntimeException("GraphNodes are not comparible");
	}

}
