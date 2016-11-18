package net.akehurst.language.parse.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import net.akehurst.language.grammar.parser.forrest.NodeIdentifier;

abstract public class AbstractGraphNode implements IGraphNode {

	public AbstractGraphNode() {
		this.previous = new ArrayList<>();
		this.parents = new HashMap<>();
	}

	private List<PreviousInfo> previous;

	@Override
	public List<PreviousInfo> getPrevious() {
		return this.previous;
	}

	@Override
	public IGraphNode pushToStackOf(IGraphNode next, int atPosition) {
		next.getPrevious().add(new PreviousInfo(this, atPosition));
		return next;
	}

	Map<ParentsIndex, IGraphNode> parents;

	public Map<ParentsIndex, IGraphNode> getParents() {
		return this.parents;
	}

	public static final class ParentsIndex {
		public ParentsIndex(int ruleNumber, int startPosition, int childIndex) {
			this.ruleNumber = ruleNumber;
			this.startPosition = startPosition;
			this.childIndex = childIndex;
			this.hashCode_cache = Objects.hash(ruleNumber, startPosition, childIndex);
		}

		int ruleNumber;
		int startPosition;
		int childIndex;

		int hashCode_cache;

		@Override
		public int hashCode() {
			return this.hashCode_cache;
		}

		@Override
		public boolean equals(Object arg) {
			if (!(arg instanceof ParentsIndex)) {
				return false;
			}
			ParentsIndex other = (ParentsIndex) arg;
			return this.childIndex == other.childIndex && this.ruleNumber == other.ruleNumber && this.startPosition==other.startPosition;
		}

		@Override
		public String toString() {
			return "(".concat(Integer.toString(this.ruleNumber)).concat(",")
					.concat(Integer.toString(this.startPosition)).concat(",")
					.concat(Integer.toString(this.childIndex)).concat(")");
		}
	}

	
	protected void addParent(GraphNodeBranch parent, int childIndex) {
//		if ( parent.getIsComplete()) {
//			int i=0;
////			throw new RuntimeException("Should this happen?");
//		}
//
//		ParentsIndex index = new ParentsIndex(parent.getRuntimeRule().getRuleNumber(), parent.getStartPosition(), childIndex);
//		IGraphNode existing = this.parents.get(index);
//		if (null == existing) {
//			this.parents.put(index, parent);
//			parent.currentLength+= this.getMatchedTextLength();
//		} else {
//			int i = 0;
//		}

	}

	int hashCode_cache;

	@Override
	public int hashCode() {
		return hashCode_cache;
	}

	@Override
	public boolean equals(Object obj) {
		return super.equals(obj);
	}

}
