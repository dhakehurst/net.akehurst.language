package net.akehurst.language.parse.graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import net.akehurst.language.core.parser.INode;
import net.akehurst.language.grammar.parse.tree.Leaf;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;
import net.akehurst.language.parse.graph.IGraphNode.PreviousInfo;

public class ParseGraph implements IParseGraph {

	public ParseGraph(final RuntimeRule goalRule, final int inputLength) {
		this.goalRule = goalRule;
		this.inputLength = inputLength;
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
		public NodeIndex(final int ruleNumber, final int startPosition) { // , int length) {
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
		public boolean equals(final Object arg) {
			if (!(arg instanceof NodeIndex)) {
				return false;
			}
			final NodeIndex other = (NodeIndex) arg;
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
		return this.nodes.values();
	}

	@Override
	public IGraphNode findCompleteNode(final int ruleNumber, final int start, final int length) {
		final NodeIndex id = new NodeIndex(ruleNumber, start);// , length);// , l.getEnd(), -1);
		final IGraphNode gn = this.nodes.get(id);
		return gn;
	}

	public static final class GrowingNodeIndex {
		public GrowingNodeIndex(final int ruleNumber, final int startPosition, final int length, final int nextItemIndex, final int stackHash) {
			this.ruleNumber = ruleNumber;
			this.startPosition = startPosition;
			this.length = length;
			this.nextItemIndex = nextItemIndex;
			this.stackHash = stackHash;
			this.hashCode_cache = Objects.hash(ruleNumber, startPosition, length, nextItemIndex, stackHash);
		}

		private final int ruleNumber;
		private final int startPosition;
		private final int length;
		private final int nextItemIndex;
		private final int stackHash;

		private final int hashCode_cache;

		@Override
		public int hashCode() {
			return this.hashCode_cache;
		}

		@Override
		public boolean equals(final Object arg) {
			if (!(arg instanceof GrowingNodeIndex)) {
				return false;
			}
			final GrowingNodeIndex other = (GrowingNodeIndex) arg;
			return this.ruleNumber == other.ruleNumber && this.startPosition == other.startPosition && this.length == other.length
					&& this.nextItemIndex == other.nextItemIndex && this.stackHash == other.stackHash;
		}

		@Override
		public String toString() {
			return "(".concat(Integer.toString(this.ruleNumber)).concat(",").concat(Integer.toString(this.startPosition)).concat(",").concat(",")
					.concat(Integer.toString(this.length)).concat(Integer.toString(this.nextItemIndex)).concat(",").concat(Integer.toString(this.stackHash))
					.concat(")");
		}
	}

	Map<GrowingNodeIndex, IGraphNode> growable;

	@Override
	public Collection<IGraphNode> getGrowable() {
		return this.growable.values();
	}

	public void registerCompleteNode(final IGraphNode node) {
		final NodeIndex index = new NodeIndex(node.getRuntimeRule().getRuleNumber(), node.getStartPosition());// , node.getMatchedTextLength());
		final IGraphNode existing = this.nodes.get(index);
		// boolean isGoal = this.getIsGoal(node);
		if (null == existing || node.getMatchedTextLength() > existing.getMatchedTextLength()) {
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

	public void addGrowable(final IGraphNode value) {
		// TODO: merge with already growing
		final int runtimeRuleNumber = value.getRuntimeRule().getRuleNumber();
		final int startPos = value.getStartPosition();
		final int length = value.getMatchedTextLength();
		final int nextItemIndex = value.getNextItemIndex();
		// int previousRRN = value.getPrevious().isEmpty() ? -1 : value.getPrevious().get(0).node.getRuntimeRule().getRuleNumber();
		final int stackHash = value.getStackHash();
		final GrowingNodeIndex index = new GrowingNodeIndex(runtimeRuleNumber, startPos, length, nextItemIndex, stackHash); // previousRRN);
		// TODO: try comparing the stack not just its hash! maybe the hash is not unique
		final IGraphNode existing = this.growable.get(index);
		if (null == existing) {
			this.growable.put(index, value);

		} else {

			final int existingPriority = existing.getPriority();
			final int valuePriority = value.getPriority();
			if (valuePriority < existingPriority) {
				// System.out.println("dropped growable " + existing);
				this.growable.put(index, value);
			} else {
				// System.out.println("dropped growable " + value);
			}
		}
	}

	public void tryAddGrowable(final IGraphNode value) {
		if (value.getCanGrow() || value.getMatchedTextLength() < this.inputLength) {
			this.addGrowable(value);
		}
	}

	// @Override
	// public void removeGrowable(IGraphNode value) {
	// int runtimeRuleNumber = value.getRuntimeRule().getRuleNumber();
	// int startPos = value.getStartPosition();
	// int nextItemIndex = value.getNextItemIndex();
	// int previousRRN = value.getPrevious().isEmpty() ? -1 : value.getPrevious().get(0).node.getRuntimeRule().getRuleNumber();
	// GrowingNodeIndex index = new GrowingNodeIndex(runtimeRuleNumber, startPos, nextItemIndex, previousRRN);
	// this.growable.remove(index);
	// }

	protected boolean getIsGoal(final IGraphNode gn) {
		return gn.getMatchedTextLength() == this.inputLength && gn.getIsComplete() && !gn.getIsStacked()
				&& this.goalRule.getRuleNumber() == gn.getRuntimeRule().getRuleNumber();
	}

	@Override
	public IGraphNode createLeaf(final Leaf leaf, final RuntimeRule terminalRule, final int startPosition, final int machedTextLength) {
		final IGraphNode gn = new GraphNodeLeaf(this, leaf);
		// this.addGrowable(gn);
		// this.registerCompleteNode(gn);
		return gn;
	}

	@Override
	public IGraphNode findOrCreateLeaf(final Leaf leaf, final RuntimeRule terminalRule, final int startPosition, final int machedTextLength) {
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
	public IGraphNode findOrCreateBranch(final RuntimeRule rr, final int priority, final int startPosition, final int machedTextLength, final int nextItemIndex,
			final int height) {
		IGraphNode gn = this.findCompleteNode(rr.getRuleNumber(), startPosition, machedTextLength);
		if (null == gn) {
			gn = this.createBranch(rr, priority, startPosition, machedTextLength, nextItemIndex, height);
			return gn;
		} else {
			final IGraphNode gn2 = new GraphNodeBranch(this, gn.getRuntimeRule(), gn.getPriority(), gn.getStartPosition(), gn.getMatchedTextLength(),
					gn.getNextItemIndex(), gn.getHeight());
			gn2.getChildren().addAll(gn.getChildren());
			return gn2;
		}
	}

	@Override
	public IGraphNode createBranch(final RuntimeRule rr, final int priority, final int startPosition, final int machedTextLength, final int nextItemIndex,
			final int height) {
		final IGraphNode gn = new GraphNodeBranch(this, rr, priority, startPosition, machedTextLength, nextItemIndex, height);

		this.tryAddGrowable(gn);

		if (gn.getIsComplete()) {
			this.registerCompleteNode(gn);
		}
		return gn;
	}

	@Override
	public IGraphNode createWithFirstChild(final RuntimeRule runtimeRule, final int priority, final IGraphNode firstChild) {
		final IGraphNode gn = new GraphNodeBranch(this, runtimeRule, priority, firstChild.getStartPosition(), firstChild.getMatchedTextLength(), 1,
				firstChild.getHeight() + 1);
		gn.getChildren().add((INode) firstChild);
		for (final PreviousInfo info : firstChild.getPossibleParent()) {
			gn.addPrevious(info.node, info.atPosition);
		}
		// gn.getPrevious().addAll(firstChild.getPrevious());

		this.tryAddGrowable(gn);

		if (gn.getIsComplete()) {
			this.registerCompleteNode(gn);
		}
		return gn;
	}

	@Override
	public IGraphNode createWithFirstChildAndStack(final RuntimeRule runtimeRule, final int priority, final IGraphNode firstChild, final IGraphNode stack) {
		final IGraphNode gn = new GraphNodeBranch(this, runtimeRule, priority, firstChild.getStartPosition(), firstChild.getMatchedTextLength(), 1,
				firstChild.getHeight() + 1);
		gn.getChildren().add((INode) firstChild);
		for (final PreviousInfo info : firstChild.getPossibleParent()) {
			gn.addPrevious(info.node, info.atPosition);
		}
		gn.addPrevious(stack, 0);
		// gn.getPrevious().addAll(firstChild.getPrevious());

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
