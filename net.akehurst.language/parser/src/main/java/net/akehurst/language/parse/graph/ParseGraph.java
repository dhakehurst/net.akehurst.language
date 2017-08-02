package net.akehurst.language.parse.graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import net.akehurst.language.grammar.parse.tree.IInput;
import net.akehurst.language.grammar.parse.tree.Leaf;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleItemKind;
import net.akehurst.language.parse.graph.IGraphNode.PreviousInfo;

public class ParseGraph implements IParseGraph {

	public ParseGraph(final RuntimeRule goalRule, final IInput input) {
		this.goalRule = goalRule;
		// this.inputLength = inputLength;
		this.input = input;
		this.nodes = new HashMap<>();
		this.leaves = new HashMap<>();
		this.growable = new HashMap<>();
		this.goals = new ArrayList<>();
	}

	private final RuntimeRule goalRule;
	// private final int inputLength;
	private final IInput input;
	private final List<ICompleteNode> goals;

	public static final class NodeIndex {
		public NodeIndex(final int ruleNumber, final int startPosition, final int endPosition) {
			this.ruleNumber = ruleNumber;
			this.startPosition = startPosition;
			this.endPosition = endPosition;
			this.hashCode_cache = Objects.hash(ruleNumber, startPosition, endPosition);
		}

		int ruleNumber;
		int startPosition;
		int endPosition;

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
			return this.ruleNumber == other.ruleNumber && this.startPosition == other.startPosition && this.endPosition == other.endPosition;
		}

		@Override
		public String toString() {
			return "(".concat(Integer.toString(this.ruleNumber)).concat(",").concat(Integer.toString(this.startPosition)).concat(",")
					.concat(Integer.toString(this.endPosition)).concat(")");
		}
	}

	private final Map<NodeIndex, ICompleteNode> nodes;

	public static final class GrowingNodeIndex {
		public GrowingNodeIndex(final int ruleNumber, final int startPosition, final int endPosition, final int nextItemIndex) {// , final boolean hasStack) {//
																																// ,
																																// final
			// int[]
			// stack) {
			this.ruleNumber = ruleNumber;
			this.startPosition = startPosition;
			this.endPosition = endPosition;
			this.nextItemIndex = nextItemIndex;
			// this.hasStack = hasStack;
			// this.stackHash = stack;
			this.hashCode_cache = Objects.hash(ruleNumber, startPosition, endPosition, nextItemIndex);// , hasStack);// , Arrays.hashCode(stack));
		}

		private final int ruleNumber;
		private final int startPosition;
		private final int endPosition;
		private final int nextItemIndex;
		// private final boolean hasStack;
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
			return this.ruleNumber == other.ruleNumber && this.startPosition == other.startPosition && this.endPosition == other.endPosition
					&& this.nextItemIndex == other.nextItemIndex;// && this.hasStack == other.hasStack;// && Arrays.equals(this.stackHash, other.stackHash);
		}

		@Override
		public String toString() {
			final StringBuilder b = new StringBuilder();
			b.append('(');
			b.append(this.ruleNumber);
			b.append(',');
			b.append(this.startPosition);
			b.append(',');
			b.append(this.endPosition);
			b.append(',');
			b.append(this.nextItemIndex);
			// b.append(',');
			// b.append(this.hasStack);
			// b.append(',');
			// b.append(this.stackHash);
			b.append(')');
			return b.toString();
		}
	}

	private final Map<GrowingNodeIndex, IGrowingNode> growable;

	private final Map<NodeIndex, ICompleteNode> leaves;

	@Override
	public List<ICompleteNode> getGoals() {
		return this.goals;
	}

	@Override
	public Collection<ICompleteNode> getCompleteNodes() {
		return this.nodes.values();
	}

	@Override
	public ICompleteNode findNode(final int ruleNumber, final int start, final int endPosition) {
		final NodeIndex id = new NodeIndex(ruleNumber, start, endPosition);// , l.getEnd(), -1);
		final ICompleteNode gn = this.nodes.get(id);
		return gn;
	}

	@Override
	public Collection<IGrowingNode> getGrowable() {
		return this.growable.values();
	}

	private void checkForGoal(final ICompleteNode node) {
		if (this.getIsGoal(node)) {
			// TODO: maybe need to not have duplicates!
			this.goals.add(node);
		}
	}

	private IGrowingNode resolvePriority(final IGrowingNode existing, final IGrowingNode newNode) {
		final int existingPriority = existing.getPriority();
		final int valuePriority = newNode.getPriority();
		if (valuePriority < existingPriority) {
			return existing;
		} else {
			return newNode;
		}
	}

	private IGrowingNode findOrCreateGrowingNode(final RuntimeRule runtimeRule, final int startPosition, final int endPosition, final int nextItemIndex,
			final int priority, final List<ICompleteNode> children) {
		final int ruleNumber = runtimeRule.getRuleNumber();
		final GrowingNodeIndex gnindex = new GrowingNodeIndex(ruleNumber, startPosition, endPosition, nextItemIndex);
		final IGrowingNode existing = this.growable.get(gnindex);
		if (null == existing) {
			final IGrowingNode nn = new GrowingNode(runtimeRule, startPosition, endPosition, nextItemIndex, priority, children);
			this.growable.put(gnindex, nn);
			return nn;
		} else {
			return existing;
		}
	}

	private void addGrowable(final IGrowingNode value) {
		// TODO: merge with already growing
		final int ruleNumber = value.getRuntimeRule().getRuleNumber();
		final int startPosition = value.getStartPosition();
		final int endPosition = value.getEndPosition();
		final int nextItemIndex = value.getNextItemIndex();
		// // int previousRRN = value.getPrevious().isEmpty() ? -1 : value.getPrevious().get(0).node.getRuntimeRule().getRuleNumber();
		// // final int stackHash[] = value.getStackHash();
		// final GrowingNodeIndex index = new GrowingNodeIndex(ruleNumber, startPosition, length, nextItemIndex, !value.getPrevious().isEmpty());// ,
		// stackHash);
		// // //
		// previousRRN);
		// TODO: try comparing the stack not just its hash! maybe the hash is not unique
		final GrowingNodeIndex gnindex = new GrowingNodeIndex(ruleNumber, startPosition, endPosition, nextItemIndex);
		final IGrowingNode existing = this.growable.get(gnindex);
		if (null == existing) {
			this.growable.put(gnindex, value);

		} else {
			final IGrowingNode preferred = this.resolvePriority(existing, value);
			this.growable.put(gnindex, preferred);
		}
	}

	private void tryAddGrowable(final IGrowingNode value) {
		// if (value.getCanGrow() || !this.input.getIsEnd(value.getMatchedTextLength())) {
		if (!this.input.getIsEnd(value.getEndPosition())) {
			this.addGrowable(value);
		}
	}

	private boolean getIsGoal(final ICompleteNode gn) {
		return this.input.getIsEnd(gn.getEndPosition() + 1) // && gn.getIsComplete() && !gn.getIsStacked()
				&& this.goalRule.getRuleNumber() == gn.getRuntimeRule().getRuleNumber();
	}

	@Override
	public ICompleteNode complete(final IGrowingNode growing) {
		if (growing.getHasCompleteChildren()) {
			final RuntimeRule runtimeRule = growing.getRuntimeRule();
			final int priority = growing.getPriority();
			final int startPosition = growing.getStartPosition();
			final int endPosition = growing.getEndPosition();
			ICompleteNode cn = this.findNode(runtimeRule.getRuleNumber(), startPosition, endPosition);
			if (null == cn) {
				cn = this.createBranchNoChildren(runtimeRule, priority, startPosition, endPosition);
			} else {
			}
			if (growing.getIsLeaf()) {
				// dont try and add children...can't for a leaf
			} else {
				final ICompleteNode.ChildrenOption opt = new ICompleteNode.ChildrenOption();
				opt.matchedLength = growing.getMatchedTextLength();
				opt.nodes = growing.getGrowingChildren();
				cn.getChildrenOption().add(opt);
			}
			this.checkForGoal(cn);
			return cn;
		} else {
			return null;
		}
	}

	@Override
	public void createStart(final RuntimeRule goalRule) {
		final IGrowingNode gn = this.findOrCreateGrowingNode(goalRule, 0, 0, 0, 0, new ArrayList<>());// this.createBranch(goalRule, 0, 0, 0, 0);
		// gn.addHead(gn);
		this.tryAddGrowable(gn);
	}

	// @Override
	private ICompleteNode createLeaf(final Leaf leaf) {
		final ICompleteNode gn = new GraphNodeLeaf(this, leaf);
		final NodeIndex nindex = new NodeIndex(leaf.getRuntimeRuleNumber(), leaf.getStartPosition(), leaf.getEndPosition());
		// this.registerCompleteNode(gn);
		this.leaves.put(nindex, gn);
		this.nodes.put(nindex, gn);
		this.checkForGoal(gn);
		return gn;
	}

	private ICompleteNode createBranchNoChildren(final RuntimeRule runtimeRule, final int priority, final int startPosition, final int endPosition) {
		final ICompleteNode gn = new GraphNodeBranch(this, runtimeRule, priority, startPosition, endPosition);
		final NodeIndex nindex = new NodeIndex(runtimeRule.getRuleNumber(), startPosition, endPosition);
		this.nodes.put(nindex, gn);
		return gn;
	}

	@Override
	public ICompleteNode findOrCreateLeaf(final Leaf leaf) {
		// TODO: handle empty leaf here...create the new node above the empty leaf also, so that we don't get
		// both in the growable set..maybe? maybe no longer need to do that!
		// TODO: perhaps a separate cache of leaves?
		// IGraphNode gn = this.findCompleteNode(terminalRule.getRuleNumber(), startPosition, machedTextLength);

		final NodeIndex nindex = new NodeIndex(leaf.getRuntimeRuleNumber(), leaf.getStartPosition(), leaf.getEndPosition());
		ICompleteNode existingLeaf = this.leaves.get(nindex);
		if (null == existingLeaf) {
			existingLeaf = this.createLeaf(leaf);
		}
		return existingLeaf;
	}

	@Override
	public void createWithFirstChild(final RuntimeRule runtimeRule, final int priority, final ICompleteNode firstChild,
			final Set<IGrowingNode.PreviousInfo> previous) {
		final int startPosition = firstChild.getStartPosition();
		final int endPosition = firstChild.getEndPosition();
		int nextItemIndex = 0;
		switch (runtimeRule.getRhs().getKind()) {
			case CHOICE:
				nextItemIndex = -1;
			break;
			case CONCATENATION:
				nextItemIndex = runtimeRule.getRhs().getItems().length == 1 ? -1 : 1;
			break;
			case EMPTY:
				nextItemIndex = -1;
			break;
			case MULTI:
				nextItemIndex = 1 == runtimeRule.getRhs().getMultiMax() ? -1 : 1;
			break;
			case PRIORITY_CHOICE:
				nextItemIndex = -1;
			break;
			case SEPARATED_LIST:
				nextItemIndex = 1 == runtimeRule.getRhs().getMultiMax() ? -1 : 1;
			break;
			default:
				throw new RuntimeException("Internal Error: Unknown RuleKind " + runtimeRule.getRhs().getKind());
		}
		final List<ICompleteNode> children = new ArrayList<>();
		children.add(firstChild);

		final IGrowingNode gn = this.findOrCreateGrowingNode(runtimeRule, startPosition, endPosition, nextItemIndex, priority, children);
		// final IGraphNode gn = new GraphNodeBranch(this, runtimeRule, priority, startPosition, textLength, nextItemIndex, height);
		for (final IGrowingNode.PreviousInfo info : previous) {
			gn.addPrevious(info.node, info.atPosition);
		}
	}

	@Override
	public void reuseWithOtherStack(final IGraphNode node, final Set<PreviousInfo> previous) {
		// node.getPrevious().addAll(previous);
		// this.tryAddGrowable(node);
		throw new UnsupportedOperationException();
	}

	@Override
	public void growNextChild(final IGrowingNode parent, final ICompleteNode nextChild, final int position) {
		if (0 != position && parent.getRuntimeRule().getRhs().getKind() == RuntimeRuleItemKind.MULTI) {
			final ICompleteNode prev = parent.getGrowingChildren().get(position - 1);
			if (prev == nextChild) {
				// dont add same child twice to a multi
				return;
			}
		}

		// final int newLength = parent.getMatchedTextLength() + nextChild.getMatchedTextLength();
		int newNextItemIndex = 0;
		switch (parent.getRuntimeRule().getRhs().getKind()) {
			case CHOICE:
				newNextItemIndex = -1;
			break;
			case CONCATENATION:
				newNextItemIndex = parent.getRuntimeRule().getRhs().getItems().length == parent.getNextItemIndex() + 1 ? -1 : parent.getNextItemIndex() + 1;
			break;
			case EMPTY:
				newNextItemIndex = -1;
			break;
			case MULTI:
				newNextItemIndex = parent.getNextItemIndex() + 1;
			break;
			case PRIORITY_CHOICE:
				newNextItemIndex = -1;
			break;
			case SEPARATED_LIST:
				newNextItemIndex = parent.getNextItemIndex() + 1;
			break;
			default:
				throw new RuntimeException("Internal Error: Unknown RuleKind " + parent.getRuntimeRule().getRhs().getKind());
		}

		final RuntimeRule runtimeRule = parent.getRuntimeRule();
		final int priority = parent.getPriority();
		final int startPosition = parent.getStartPosition();
		final int endPosition = nextChild.getEndPosition();
		final int nextItemIndex = newNextItemIndex;
		final List<ICompleteNode> children = new ArrayList<>(parent.getGrowingChildren());
		children.add(nextChild);
		final IGrowingNode newParent = this.findOrCreateGrowingNode(runtimeRule, startPosition, endPosition, nextItemIndex, priority, children);
		if (newParent.getHasCompleteChildren()) {
			this.complete(newParent);
		}
		// this.tryAddGrowable(newParent);
	}

	@Override
	public void growNextSkipChild(final IGrowingNode parent, final ICompleteNode nextChild) {
		final RuntimeRule runtimeRule = parent.getRuntimeRule();
		final int priority = parent.getPriority();
		final int startPosition = parent.getStartPosition();
		final int endPosition = nextChild.getEndPosition();
		final int nextItemIndex = parent.getNextItemIndex();
		final List<ICompleteNode> children = new ArrayList<>(parent.getGrowingChildren());
		children.add(nextChild);
		final IGrowingNode newParent = this.findOrCreateGrowingNode(runtimeRule, startPosition, endPosition, nextItemIndex, priority, children);
		if (newParent.getHasCompleteChildren()) {
			this.complete(newParent);
		}
		this.tryAddGrowable(parent);
	}

	@Override
	public void pushToStackOf(final ICompleteNode leafNode, final IGrowingNode stack) {
		final RuntimeRule runtimeRule = leafNode.getRuntimeRule();
		final int startPosition = leafNode.getStartPosition();
		final int endPosition = leafNode.getEndPosition();
		final int nextItemIndex = stack.getNextItemIndex();
		final IGrowingNode growing = this.findOrCreateGrowingNode(runtimeRule, startPosition, endPosition, nextItemIndex, 0, new ArrayList<>());
		growing.addPrevious(stack, nextItemIndex);
	}

	@Override
	public String toString() {
		return this.goals + System.lineSeparator() + this.growable.size() + "-" + Arrays.toString(this.growable.values().toArray());
	}

}
