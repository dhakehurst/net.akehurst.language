package net.akehurst.language.parse.graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import net.akehurst.language.core.parser.INode;
import net.akehurst.language.grammar.parse.tree.IInput;
import net.akehurst.language.grammar.parse.tree.Leaf;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;
import net.akehurst.language.parse.graph.IGraphNode.PreviousInfo;

public class ParseGraph implements IParseGraph {

	public ParseGraph(final RuntimeRule goalRule, final IInput input) {
		this.goalRule = goalRule;
		// this.inputLength = inputLength;
		this.input = input;
		this.nodes = new HashMap<>();
		this.growable = new HashMap<>();
		this.goals = new ArrayList<>();
	}

	private final RuntimeRule goalRule;
	// private final int inputLength;
	private final IInput input;
	private final List<IGraphNode> goals;

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

	private final Map<NodeIndex, IGraphNode> nodes;

	public static final class GrowingNodeIndex {
		public GrowingNodeIndex(final int ruleNumber, final int startPosition, final int length, final int nextItemIndex, final boolean hasStack) {// , final
																																					// int[]
																																					// stack) {
			this.ruleNumber = ruleNumber;
			this.startPosition = startPosition;
			this.length = length;
			this.nextItemIndex = nextItemIndex;
			this.hasStack = hasStack;
			// this.stackHash = stack;
			this.hashCode_cache = Objects.hash(ruleNumber, startPosition, length, nextItemIndex, hasStack);// , Arrays.hashCode(stack));
		}

		private final int ruleNumber;
		private final int startPosition;
		private final int length;
		private final int nextItemIndex;
		private final boolean hasStack;
		// private final int[] stackHash;

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
					&& this.nextItemIndex == other.nextItemIndex && this.hasStack == other.hasStack;// && Arrays.equals(this.stackHash, other.stackHash);
		}

		@Override
		public String toString() {
			final StringBuilder b = new StringBuilder();
			b.append('(');
			b.append(this.ruleNumber);
			b.append(',');
			b.append(this.startPosition);
			b.append(',');
			b.append(this.length);
			b.append(',');
			b.append(this.nextItemIndex);
			b.append(',');
			b.append(this.hasStack);
			// b.append(',');
			// b.append(this.stackHash);
			b.append(')');
			return b.toString();
		}
	}

	private final Map<GrowingNodeIndex, IGraphNode> growable;

	@Override
	public List<IGraphNode> getGoals() {
		return this.goals;
	}

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
		// final int stackHash[] = value.getStackHash();
		final GrowingNodeIndex index = new GrowingNodeIndex(runtimeRuleNumber, startPos, length, nextItemIndex, !value.getPrevious().isEmpty());// ,
																																				// stackHash);
																																				// //
																																				// previousRRN);
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
		if (value.getCanGrow() || !this.input.getIsEnd(value.getMatchedTextLength())) {
			this.addGrowable(value);
		}
	}

	protected boolean getIsGoal(final IGraphNode gn) {
		return this.input.getIsEnd(gn.getMatchedTextLength()) && gn.getIsComplete() && !gn.getIsStacked()
				&& this.goalRule.getRuleNumber() == gn.getRuntimeRule().getRuleNumber();
	}

	@Override
	public void createStart(final RuntimeRule goalRule) {
		final IGraphNode gn = this.createBranch(goalRule, 0, 0, 0, 0, 0, Collections.EMPTY_SET);
		// gn.addHead(gn);
		this.tryAddGrowable(gn);
	}

	// @Override
	private IGraphNode createLeaf(final Leaf leaf, final RuntimeRule terminalRule, final int startPosition, final int machedTextLength) {
		final IGraphNode gn = new GraphNodeLeaf(this, leaf);
		this.registerCompleteNode(gn);
		return gn;
	}

	@Override
	public IGraphNode findOrCreateLeaf(final Leaf leaf, final RuntimeRule terminalRule, final int startPosition, final int machedTextLength) {
		// TODO: handle empty leaf here...create the new node above the empty leaf also, so that we don't get
		// both in the growable set..maybe? maybe no longer need to do that!
		// TODO: perhaps a separate cache of leaves?
		IGraphNode gn = this.findCompleteNode(terminalRule.getRuleNumber(), startPosition, machedTextLength);
		if (null == gn) {
			gn = this.createLeaf(leaf, terminalRule, startPosition, machedTextLength);
		}
		return gn;
	}

	// @Override
	// public IGraphNode findOrCreateBranch(final RuntimeRule rr, final int priority, final int startPosition, final int machedTextLength, final int
	// nextItemIndex,
	// final int height) {
	// IGraphNode gn = this.findCompleteNode(rr.getRuleNumber(), startPosition, machedTextLength);
	// if (null == gn) {
	// gn = this.createBranch(rr, priority, startPosition, machedTextLength, nextItemIndex, height);
	// return gn;
	// } else {
	// final IGraphNode gn2 = new GraphNodeBranch(this, gn.getRuntimeRule(), gn.getPriority(), gn.getStartPosition(), gn.getMatchedTextLength(),
	// gn.getNextItemIndex(), gn.getHeight());
	// gn2.getChildren().addAll(gn.getChildren());
	// return gn2;
	// }
	// }

	@Override
	public IGraphNode createBranch(final RuntimeRule rr, final int priority, final int startPosition, final int machedTextLength, final int nextItemIndex,
			final int height, final Set<PreviousInfo> previous) {
		final IGraphNode gn = new GraphNodeBranch(this, rr, priority, startPosition, machedTextLength, nextItemIndex, height);
		for (final PreviousInfo info : previous) {
			gn.addPrevious(info.node, info.atPosition);
		}

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
		firstChild.getPossibleParent().add(gn);
		for (final PreviousInfo info : firstChild.getPrevious()) {
			gn.addPrevious(info.node, info.atPosition);
		}

		// firstChild.addHead(gn);
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
		firstChild.getPossibleParent().add(gn);
		for (final PreviousInfo info : firstChild.getPrevious()) {
			gn.addPrevious(info.node, info.atPosition);
		}
		gn.addPrevious(stack, 0);

		// firstChild.addHead(gn);
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
