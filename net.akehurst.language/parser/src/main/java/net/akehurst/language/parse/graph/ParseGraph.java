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
		this.growingHead = new HashMap<>();
		this.growing = new HashMap<>();
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

	private final Map<GrowingNodeIndex, IGrowingNode> growingHead;
	private final Map<GrowingNodeIndex, IGrowingNode> growing;

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
		return this.growingHead.values();
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

	private IGrowingNode findOrCreateGrowingLeaf(final ICompleteNode leafNode, final IGrowingNode stack, final Set<IGrowingNode.PreviousInfo> previous) {
		this.addGrowing(stack, previous);
		final int ruleNumber = leafNode.getRuntimeRuleNumber();
		final int startPosition = leafNode.getStartPosition();
		final int endPosition = leafNode.getEndPosition();
		final int nextItemIndex = -1;
		final GrowingNodeIndex gnindex = new GrowingNodeIndex(ruleNumber, startPosition, endPosition, nextItemIndex);
		final IGrowingNode existing = this.growing.get(gnindex);
		if (null == existing) {
			final RuntimeRule runtimeRule = leafNode.getRuntimeRule();
			final IGrowingNode nn = new GrowingNode(runtimeRule, startPosition, endPosition, nextItemIndex, 0, Collections.EMPTY_LIST);
			nn.addPrevious(stack, stack.getNextItemIndex());
			// this.growing.put(gnindex, nn);
			this.addGrowingHead(gnindex, nn);
			return nn;
		} else {
			existing.addPrevious(stack, stack.getNextItemIndex());
			this.addGrowingHead(gnindex, existing);
			return existing;
		}
	}

	private IGrowingNode findOrCreateGrowingNode(final RuntimeRule runtimeRule, final int startPosition, final int endPosition, final int nextItemIndex,
			final int priority, final List<ICompleteNode> children, final Set<IGrowingNode.PreviousInfo> previous) {
		final int ruleNumber = runtimeRule.getRuleNumber();
		final GrowingNodeIndex gnindex = new GrowingNodeIndex(ruleNumber, startPosition, endPosition, nextItemIndex);
		final IGrowingNode existing = this.growing.get(gnindex);
		if (null == existing) {
			final IGrowingNode nn = new GrowingNode(runtimeRule, startPosition, endPosition, nextItemIndex, priority, children);
			for (final IGrowingNode.PreviousInfo info : previous) {
				nn.addPrevious(info.node, info.atPosition);
			}
			// this.growing.put(gnindex, nn);
			this.addGrowingHead(gnindex, nn);
			if (nn.getHasCompleteChildren()) {
				this.complete(nn);
			}
			return nn;
		} else {
			for (final IGrowingNode.PreviousInfo info : previous) {
				existing.addPrevious(info.node, info.atPosition);
			}
			this.addGrowingHead(gnindex, existing);
			return existing;
		}
	}

	private void addGrowing(final IGrowingNode gn, final Set<IGrowingNode.PreviousInfo> previous) {
		final int ruleNumber = gn.getRuntimeRuleNumber();
		final int startPosition = gn.getStartPosition();
		final int endPosition = gn.getEndPosition();
		final int nextItemIndex = gn.getNextItemIndex();
		final GrowingNodeIndex gnindex = new GrowingNodeIndex(ruleNumber, startPosition, endPosition, nextItemIndex);
		final IGrowingNode existing = this.growing.get(gnindex);
		if (null == existing) {
			for (final IGrowingNode.PreviousInfo info : previous) {
				gn.addPrevious(info.node, info.atPosition);
			}
			this.growing.put(gnindex, gn);
		} else {
			// merge
			for (final IGrowingNode.PreviousInfo info : previous) {
				existing.addPrevious(info.node, info.atPosition);
			}
		}
	}

	private void removeGrowing(final IGrowingNode gn) {
		final int ruleNumber = gn.getRuntimeRuleNumber();
		final int startPosition = gn.getStartPosition();
		final int endPosition = gn.getEndPosition();
		final int nextItemIndex = gn.getNextItemIndex();
		final GrowingNodeIndex gnindex = new GrowingNodeIndex(ruleNumber, startPosition, endPosition, nextItemIndex);
		this.growing.remove(gnindex);
	}

	private void addGrowingHead(final GrowingNodeIndex gnindex, final IGrowingNode gn) {
		if (this.growing.containsKey(gnindex)) {
			// don't add the head
		} else {
			final IGrowingNode existing = this.growingHead.get(gnindex);
			if (null == existing) {
				this.growingHead.put(gnindex, gn);
			} else {
				// merge
				for (final IGrowingNode.PreviousInfo info : gn.getPrevious()) {
					existing.addPrevious(info.node, info.atPosition);
				}
			}
		}
	}

	private void removeGrowingHead(final IGrowingNode gn) {
		final int ruleNumber = gn.getRuntimeRuleNumber();
		final int startPosition = gn.getStartPosition();
		final int endPosition = gn.getEndPosition();
		final int nextItemIndex = gn.getNextItemIndex();
		final GrowingNodeIndex gnindex = new GrowingNodeIndex(ruleNumber, startPosition, endPosition, nextItemIndex);
		this.growingHead.remove(gnindex);
	}

	private boolean getIsGoal(final ICompleteNode gn) {
		final boolean isStart = this.input.getIsStart(gn.getStartPosition());
		final boolean isEnd = this.input.getIsEnd(gn.getEndPosition());
		final boolean isGoalRule = this.goalRule.getRuleNumber() == gn.getRuntimeRule().getRuleNumber();
		return isStart && isEnd && isGoalRule;
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
		final IGrowingNode gn = this.findOrCreateGrowingNode(goalRule, 0, 0, 0, 0, Collections.EMPTY_LIST, Collections.EMPTY_SET);// this.createBranch(goalRule,
																																	// 0, 0, 0, 0);
		// gn.addHead(gn);
		// this.tryAddGrowable(gn);
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
				nextItemIndex = firstChild.getIsEmptyLeaf() ? -1 : 1 == runtimeRule.getRhs().getMultiMax() ? -1 : 1;
			break;
			case PRIORITY_CHOICE:
				nextItemIndex = -1;
			break;
			case SEPARATED_LIST:
				nextItemIndex = firstChild.getIsEmptyLeaf() ? -1 : 1 == runtimeRule.getRhs().getMultiMax() ? -1 : 1;
			break;
			default:
				throw new RuntimeException("Internal Error: Unknown RuleKind " + runtimeRule.getRhs().getKind());
		}
		final List<ICompleteNode> children = new ArrayList<>();
		children.add(firstChild);

		final IGrowingNode gn = this.findOrCreateGrowingNode(runtimeRule, startPosition, endPosition, nextItemIndex, priority, children, previous);

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

		final Set<IGrowingNode.PreviousInfo> previous = parent.getPrevious();
		final IGrowingNode newParent = this.findOrCreateGrowingNode(runtimeRule, startPosition, endPosition, nextItemIndex, priority, children, previous);

		// maybe?
		if (parent.getNext().isEmpty()) {
			this.removeGrowing(parent);
		}

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
		final Set<IGrowingNode.PreviousInfo> previous = parent.getPrevious();
		final IGrowingNode newParent = this.findOrCreateGrowingNode(runtimeRule, startPosition, endPosition, nextItemIndex, priority, children, previous);

		if (parent.getNext().isEmpty()) {
			this.removeGrowing(parent);
		}

	}

	@Override
	public void pushToStackOf(final ICompleteNode leafNode, final IGrowingNode stack, final Set<IGrowingNode.PreviousInfo> previous) {
		final IGrowingNode growing = this.findOrCreateGrowingLeaf(leafNode, stack, previous);

	}

	@Override
	public Set<IGrowingNode.PreviousInfo> pop(final IGrowingNode gn) {
		for (final IGrowingNode.PreviousInfo pi : gn.getPrevious()) {
			pi.node.removeNext(gn);
		}
		final Set<IGrowingNode.PreviousInfo> previous = gn.getPrevious();
		gn.newPrevious();
		return previous;
	}

	@Override
	public String toString() {
		return this.goals + System.lineSeparator() + this.growingHead.size() + "-" + Arrays.toString(this.growingHead.values().toArray());
	}

}
