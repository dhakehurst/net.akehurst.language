package net.akehurst.language.parse.graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import net.akehurst.language.grammar.parse.tree.Leaf;
import net.akehurst.language.grammar.parser.forrest.Input3;
import net.akehurst.language.grammar.parser.forrest.NodeIdentifier;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleSetBuilder;

public class ParseGraph implements IParseGraph {

	public ParseGraph(RuntimeRule goalRule, int inputLength) {
		this.goalRule = goalRule;
		this. inputLength =  inputLength;
		this.nodes = new HashMap<>();
		this.growable = new HashMap<>();
		this.goals = new ArrayList<>();
	}

	RuntimeRule goalRule;
	int inputLength;
	List<IGraphNode> goals;

	@Override
	public List<IGraphNode> getGoals() {
		return this.goals;
	}

	public static final class NodeIndex {
		public NodeIndex(int ruleNumber, int startPosition) { // , int length) {
			this.ruleNumber = ruleNumber;
			this.startPosition = startPosition;
			// this.length = length;
			this.hashCode_cache = Objects.hash(ruleNumber, startPosition); // , length);
		}

		int ruleNumber;
		int startPosition;
		// int length;

		int hashCode_cache;

		@Override
		public int hashCode() {
			return this.hashCode_cache;
		}

		@Override
		public boolean equals(Object arg) {
			if (!(arg instanceof NodeIndex)) {
				return false;
			}
			NodeIndex other = (NodeIndex) arg;
			return this.ruleNumber == other.ruleNumber && this.startPosition == other.startPosition
			// && this.length == other.length
			;
		}

		@Override
		public String toString() {
			return "(".concat(Integer.toString(this.ruleNumber)).concat(",").concat(Integer.toString(this.startPosition)).concat(",")
					// .concat(Integer.toString(this.length))
					.concat(")");
		}
	}

	Map<NodeIndex, IGraphNode> nodes;

	@Override
	public Collection<IGraphNode> getCompleteNodes() {
		return nodes.values();
	}

	@Override
	public IGraphNode findCompleteNode(int ruleNumber, int start, int length) {
		NodeIndex id = new NodeIndex(ruleNumber, start);// , length);// , l.getEnd(), -1);
		IGraphNode gn = this.nodes.get(id);
		return gn;
	}

	public static final class GrowingNodeIndex {
		public GrowingNodeIndex(int ruleNumber, int startPosition, int nextItemIndex, int previousRRN) {
			this.ruleNumber = ruleNumber;
			this.startPosition = startPosition;
			this.nextItemIndex = nextItemIndex;
			this.previousRRN = previousRRN;
			this.hashCode_cache = Objects.hash(ruleNumber, startPosition, nextItemIndex, previousRRN);
		}

		int ruleNumber;
		int startPosition;
		int nextItemIndex;
		int previousRRN;

		int hashCode_cache;

		@Override
		public int hashCode() {
			return this.hashCode_cache;
		}

		@Override
		public boolean equals(Object arg) {
			if (!(arg instanceof GrowingNodeIndex)) {
				return false;
			}
			GrowingNodeIndex other = (GrowingNodeIndex) arg;
			return this.ruleNumber == other.ruleNumber && this.startPosition == other.startPosition && this.nextItemIndex == other.nextItemIndex
					&& this.previousRRN == other.previousRRN;
		}

		@Override
		public String toString() {
			return "(".concat(Integer.toString(this.ruleNumber)).concat(",").concat(Integer.toString(this.startPosition)).concat(",")
					.concat(Integer.toString(this.nextItemIndex)).concat(",").concat(Integer.toString(this.previousRRN)).concat(")");
		}
	}

	Map<GrowingNodeIndex, IGraphNode> growable;

	@Override
	public Collection<IGraphNode> getGrowable() {
		return this.growable.values();
	}

	public void registerCompleteNode(IGraphNode node) {
		NodeIndex index = new NodeIndex(node.getRuntimeRule().getRuleNumber(), node.getStartPosition());// , node.getMatchedTextLength());
		IGraphNode existing = this.nodes.get(index);
		//boolean isGoal = this.getIsGoal(node);
		if ( null == existing || node.getMatchedTextLength() > existing.getMatchedTextLength()) {
			this.nodes.put(index, node);

		} else {
			// drop
			// System.out.println("complete node " + existing);
			// System.out.println("is longer than " + node);
		}
		if (this.getIsGoal(node)) {
			// TODO: maybe need to not have duplicates!
			this.goals.add(node);
		}
	}

	public void addGrowable(IGraphNode value) {
		// TODO: merge with already growing
		int runtimeRuleNumber = value.getRuntimeRule().getRuleNumber();
		int startPos = value.getStartPosition();
		int nextItemIndex = value.getNextItemIndex();
//		int previousRRN = value.getPrevious().isEmpty() ? -1 : value.getPrevious().get(0).node.getRuntimeRule().getRuleNumber();
		int stackHash = value.getStackHash();
		GrowingNodeIndex index = new GrowingNodeIndex(runtimeRuleNumber, startPos, nextItemIndex, stackHash); //previousRRN);
		if (this.growable.containsKey(index)) {
			// System.out.println("merging growable " + value);
		} else {
			this.growable.put(index, value);
		}
	}

	public void tryAddGrowable(IGraphNode value) {
		if (value.getCanGrow() || value.getMatchedTextLength() < this.inputLength) {
			this.addGrowable(value);
		}
	}

	@Override
	public void removeGrowable(IGraphNode value) {
		int runtimeRuleNumber = value.getRuntimeRule().getRuleNumber();
		int startPos = value.getStartPosition();
		int nextItemIndex = value.getNextItemIndex();
		int previousRRN = value.getPrevious().isEmpty() ? -1 : value.getPrevious().get(0).node.getRuntimeRule().getRuleNumber();
		GrowingNodeIndex index = new GrowingNodeIndex(runtimeRuleNumber, startPos, nextItemIndex, previousRRN);
		this.growable.remove(index);
	}

	protected boolean getIsGoal(IGraphNode gn) {
		return gn.getMatchedTextLength()==this.inputLength && gn.getIsComplete() && !gn.getIsStacked() && (this.goalRule.getRuleNumber() == gn.getRuntimeRule().getRuleNumber());
	}

	@Override
	public IGraphNode createLeaf(Leaf leaf, RuntimeRule terminalRule, int startPosition, int machedTextLength) {
		IGraphNode gn = new GraphNodeLeaf(this, leaf);
		// this.addGrowable(gn);
		// this.registerCompleteNode(gn);
		return gn;
	}

	@Override
	public IGraphNode findOrCreateLeaf(Leaf leaf, RuntimeRule terminalRule, int startPosition, int machedTextLength) {
		// TODO: handle empty leaf here...create the new node above the empty leaf also, so that we don't get
		// both in the growable set
		IGraphNode gn = this.findCompleteNode(terminalRule.getRuleNumber(), startPosition, machedTextLength);
		if (null == gn) {
			gn = new GraphNodeLeaf(this, leaf);
			this.addGrowable(gn);
			// this.registerCompleteNode(gn);
		}
		return gn;
	}

	@Override
	public IGraphNode findOrCreateBranch(RuntimeRule rr, int priority, int startPosition, int machedTextLength, int nextItemIndex, int height) {
		IGraphNode gn = this.findCompleteNode(rr.getRuleNumber(), startPosition, machedTextLength);
		if (null == gn) {
			gn = this.createBranch(rr, priority, startPosition, machedTextLength, nextItemIndex, height);
			return gn;
		} else {
			IGraphNode gn2 = new GraphNodeBranch(this, gn.getRuntimeRule(), gn.getPriority(), gn.getStartPosition(), gn.getMatchedTextLength(),
					gn.getNextItemIndex(), gn.getHeight());
			gn2.getChildren().addAll(gn.getChildren());
			return gn2;
		}
	}

	@Override
	public IGraphNode createBranch(RuntimeRule rr, int priority, int startPosition, int machedTextLength, int nextItemIndex, int height) {
		IGraphNode gn = new GraphNodeBranch(this, rr, priority, startPosition, machedTextLength, nextItemIndex, height);

		this.tryAddGrowable(gn);

		if (gn.getIsComplete()) {
			this.registerCompleteNode(gn);
		}
		return gn;
	}

	@Override
	public IGraphNode createWithFirstChild(RuntimeRule runtimeRule, int priority, IGraphNode firstChild) {
		IGraphNode gn = new GraphNodeBranch(this, runtimeRule, priority, firstChild.getStartPosition(), firstChild.getMatchedTextLength(), 1,
				firstChild.getHeight() + 1);
		gn.getChildren().add(firstChild);
		gn.getPrevious().addAll(firstChild.getPrevious());

		this.tryAddGrowable(gn);

		if (gn.getIsComplete()) {
			this.registerCompleteNode(gn);
		}
		return gn;
	}

	@Override
	public String toString() {
		return this.goals + System.lineSeparator() + this.growable.size() + "-" + Arrays.toString(this.growable.values().toArray());
	}

}
