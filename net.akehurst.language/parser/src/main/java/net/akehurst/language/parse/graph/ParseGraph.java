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
	private final List<IGraphNode> goals;

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

	private final Map<NodeIndex, IGraphNode> growable;

	private final Map<NodeIndex, IGraphNode> leaves;

	@Override
	public List<IGraphNode> getGoals() {
		return this.goals;
	}

	@Override
	public Collection<IGraphNode> getCompleteNodes() {
		return this.nodes.values();
	}

	@Override
	public IGraphNode findNode(final int ruleNumber, final int start, final int length) {
		final NodeIndex id = new NodeIndex(ruleNumber, start, length);// , l.getEnd(), -1);
		final IGraphNode gn = this.nodes.get(id);
		return gn;
	}

	@Override
	public Collection<IGraphNode> getGrowable() {
		return this.growable.values();
	}

	// public void registerCompleteNode(final IGraphNode node) {
	// final NodeIndex index = new NodeIndex(node.getRuntimeRule().getRuleNumber(), node.getStartPosition());// , node.getMatchedTextLength());
	// final IGraphNode existing = this.nodes.get(index);
	// // boolean isGoal = this.getIsGoal(node);
	// if (null == existing || node.getMatchedTextLength() > existing.getMatchedTextLength()) {
	// this.nodes.put(index, node);
	//
	// } else {
	// // drop
	// // System.out.println("complete node " + existing);
	// // System.out.println("is longer than " + node);
	// }
	// if (this.getIsGoal(node)) {
	// // TODO: maybe need to not have duplicates!
	// this.goals.add(node);
	// }
	// }

	private void checkForGoal(final IGraphNode node) {
		if (this.getIsGoal(node)) {
			// TODO: maybe need to not have duplicates!
			this.goals.add(node);
		}
	}

	public void addGrowable(final IGraphNode value) {
		// TODO: merge with already growing
		final int ruleNumber = value.getRuntimeRule().getRuleNumber();
		final int startPosition = value.getStartPosition();
		final int endPosition = value.getGrowingEndPosition();
		// final int nextItemIndex = value.getNextItemIndex();
		// // int previousRRN = value.getPrevious().isEmpty() ? -1 : value.getPrevious().get(0).node.getRuntimeRule().getRuleNumber();
		// // final int stackHash[] = value.getStackHash();
		// final GrowingNodeIndex index = new GrowingNodeIndex(ruleNumber, startPosition, length, nextItemIndex, !value.getPrevious().isEmpty());// ,
		// stackHash);
		// // //
		// previousRRN);
		// TODO: try comparing the stack not just its hash! maybe the hash is not unique
		final NodeIndex nindex = new NodeIndex(ruleNumber, startPosition, endPosition);
		final IGraphNode existing = this.growable.get(nindex);
		if (null == existing) {
			this.growable.put(nindex, value);

		} else {

			final int existingPriority = existing.getPriority();
			final int valuePriority = value.getPriority();
			if (valuePriority < existingPriority) {
				// System.out.println("dropped growable " + existing);
				this.growable.put(nindex, value);
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
		final IGraphNode gn = this.createBranch(goalRule, 0, 0, 0, 0);
		// gn.addHead(gn);
		this.tryAddGrowable(gn);
	}

	// @Override
	private IGraphNode createLeaf(final Leaf leaf) {
		final IGraphNode gn = new GraphNodeLeaf(this, leaf);
		final NodeIndex nindex = new NodeIndex(leaf.getRuntimeRuleNumber(), leaf.getStartPosition(), leaf.getEndPosition());
		// this.registerCompleteNode(gn);
		this.leaves.put(nindex, gn);
		this.nodes.put(nindex, gn);
		this.checkForGoal(gn);
		return gn;
	}

	@Override
	public IGraphNode findOrCreateLeaf(final Leaf leaf) {
		// TODO: handle empty leaf here...create the new node above the empty leaf also, so that we don't get
		// both in the growable set..maybe? maybe no longer need to do that!
		// TODO: perhaps a separate cache of leaves?
		// IGraphNode gn = this.findCompleteNode(terminalRule.getRuleNumber(), startPosition, machedTextLength);

		final NodeIndex nindex = new NodeIndex(leaf.getRuntimeRuleNumber(), leaf.getStartPosition(), leaf.getEndPosition());
		IGraphNode existingLeaf = this.leaves.get(nindex);
		if (null == existingLeaf) {
			existingLeaf = this.createLeaf(leaf);
		}
		return existingLeaf;
	}

	@Override
	public IGraphNode findOrCreateBranch(final RuntimeRule runtimeRule, final int priority, final int startPosition, final int endPosition, final int height) {
		IGraphNode gn = this.findNode(runtimeRule.getRuleNumber(), startPosition, endPosition);
		if (null == gn) {
			gn = this.createBranch(runtimeRule, priority, startPosition, height, endPosition);
			return gn;
		} else {
			// final IGraphNode gn2 = new GraphNodeBranch(this, gn.getRuntimeRule(), gn.getPriority(), gn.getStartPosition(), gn.getMatchedTextLength(),
			// gn.getNextItemIndex(), gn.getHeight());
			// gn2.getChildren().addAll(gn.getChildren());
			// return gn2;
			return gn;
		}
	}

	private IGraphNode createBranch(final RuntimeRule runtimeRule, final int priority, final int startPosition, final int endPosition, final int height) {
		final IGraphNode gn = new GraphNodeBranch(this, runtimeRule, priority, startPosition, height);
		// for (final PreviousInfo info : previous) {
		// gn.addPrevious(info.node, info.atPosition);
		// }

		final NodeIndex nindex = new NodeIndex(runtimeRule.getRuleNumber(), startPosition, endPosition);
		// if (gn.getIsComplete()) {
		// this.registerCompleteNode(gn);
		// }
		this.nodes.put(nindex, gn);
		return gn;
	}

	@Override
	public void createWithFirstChild(final RuntimeRule runtimeRule, final int priority, final IGraphNode firstChild) {
		final int startPosition = firstChild.getStartPosition();
		final int endPosition = firstChild.getEndPosition();
		// final int machedTextLength = 0;// firstChild.getMatchedTextLength();
		// final int nextItemIndex = 0;
		// switch (runtimeRule.getRhs().getKind()) {
		// case CHOICE:
		// nextItemIndex = -1;
		// break;
		// case CONCATENATION:
		// nextItemIndex = 1 == runtimeRule.getRhs().getItems().length ? -1 : 1;
		// break;
		// case EMPTY:
		// nextItemIndex = -1;
		// break;
		// case MULTI:
		// nextItemIndex = firstChild.getRuntimeRule().getIsEmptyRule() ? -1 : 1;
		// break;
		// case PRIORITY_CHOICE:
		// nextItemIndex = -1;
		// break;
		// case SEPARATED_LIST:
		// nextItemIndex = firstChild.getRuntimeRule().getIsEmptyRule() ? -1 : 1;
		// break;
		// default:
		// throw new RuntimeException("Internal Error: Unknown RuleKind " + runtimeRule.getRhs().getKind());
		// }
		final int height = firstChild.getHeight() + 1;
		final IGraphNode gn = this.findOrCreateBranch(runtimeRule, priority, startPosition, endPosition, height);
		// final IGraphNode gn = new GraphNodeBranch(this, runtimeRule, priority, startPosition, textLength, nextItemIndex, height);
		for (final PreviousInfo info : firstChild.getPrevious()) {
			gn.addPrevious(info.node, info.atPosition);
		}
		this.growNextChild(gn, firstChild, 0);

		// gn.getChildren().add((INode) firstChild);
		// firstChild.getPossibleParent().add(gn);

		//
		// // firstChild.addHead(gn);
		// this.tryAddGrowable(gn);
		//
		// if (gn.getIsComplete()) {
		// this.registerCompleteNode(gn);
		// }
		// return gn;
	}

	// @Override
	// public void createWithFirstChildAndStack(final RuntimeRule runtimeRule, final int priority, final IGraphNode firstChild, final IGraphNode stack) {
	// final int startPosition = firstChild.getStartPosition();
	// final int textLength = firstChild.getMatchedTextLength();
	// int nextItemIndex = 0;
	// switch (runtimeRule.getRhs().getKind()) {
	// case CHOICE:
	// nextItemIndex = -1;
	// break;
	// case CONCATENATION:
	// nextItemIndex = 1;
	// break;
	// case EMPTY:
	// nextItemIndex = -1;
	// break;
	// case MULTI:
	// nextItemIndex = firstChild.getRuntimeRule().getIsEmptyRule() ? -1 : 1;
	// break;
	// case PRIORITY_CHOICE:
	// nextItemIndex = -1;
	// break;
	// case SEPARATED_LIST:
	// nextItemIndex = firstChild.getRuntimeRule().getIsEmptyRule() ? -1 : 1;
	// break;
	// default:
	// throw new RuntimeException("Internal Error: Unknown RuleKind " + runtimeRule.getRhs().getKind());
	// }
	// final int height = firstChild.getHeight() + 1;
	// final IGraphNode gn = new GraphNodeBranch(this, runtimeRule, priority, startPosition, textLength, nextItemIndex, height);
	//
	// gn.getChildren().add((INode) firstChild);
	// firstChild.getPossibleParent().add(gn);
	// for (final PreviousInfo info : firstChild.getPrevious()) {
	// gn.addPrevious(info.node, info.atPosition);
	// }
	// gn.addPrevious(stack, 0);
	//
	// // firstChild.addHead(gn);
	// this.tryAddGrowable(gn);
	//
	// if (gn.getIsComplete()) {
	// this.registerCompleteNode(gn);
	// }
	//
	// }

	@Override
	public void reuseWithOtherStack(final IGraphNode node, final Set<PreviousInfo> previous) {
		node.getPrevious().addAll(previous);
		this.tryAddGrowable(node);

	}

	@Override
	public void growNextChild(final IGraphNode parent, final IGraphNode nextChild, final int position) {
		if (0 != position && parent.getRuntimeRule().getRhs().getKind() == RuntimeRuleItemKind.MULTI) {
			final IGraphNode prev = ((GraphNodeBranch) parent).getGrowingChildren().get(position - 1);
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

		final RuntimeRule runtimeRule;
		final int priority;
		final int startPosition;
		final int length;
		final int height;
		final IGraphNode duplicate = this.findOrCreateBranch(runtimeRule, priority, startPosition, length, height)

		parent.addNextGrowingChild(nextChild, newNextItemIndex);
		if (parent.getIsComplete()) {
			this.checkForGoal(parent);
		}

		this.tryAddGrowable(parent);

		// // if duplicate will be complete && if its id already exists
		// // if (parents are the same) return already existing
		// final IGraphNode gn = this.findNode(parent.getRuntimeRule().getRuleNumber(), parent.getStartPosition());
		//
		// // GraphNodeBranch duplicate = (GraphNodeBranch)this.graph.createBranch(this.runtimeRule, this.priority, this.startPosition, newLength,
		// // newNextItemIndex, this.height);
		// int pri = parent.getPriority();
		// if (nextChild.getRuntimeRule().getIsEmptyRule()) {
		// pri = nextChild.getPriority();
		// }
		// // final GraphNodeBranch duplicate = new GraphNodeBranch(this.graph, this.runtimeRule, pri, this.startPosition, newLength, newNextItemIndex,
		// // this.height);
		// final GraphNodeBranch duplicate = (GraphNodeBranch) this.findOrCreateBranch(parent.getRuntimeRule(), pri, parent.getStartPosition(), newLength,
		// newNextItemIndex, parent.getHeight());
		// duplicate.getChildren().addAll(parent.getChildren());
		// duplicate.getChildren().add((INode) nextChild);
		// nextChild.getPossibleParent().add(duplicate);
		//
		// for (final PreviousInfo info : parent.getPrevious()) {
		// duplicate.addPrevious(info.node, info.atPosition);
		// }
		// //
		// // this.graph.tryAddGrowable(duplicate);
		// /// nextChild.addHead(duplicate);
		// this.tryAddGrowable(duplicate);
		//
		// if (duplicate.getIsComplete()) {
		// this.registerCompleteNode(duplicate);
		// }

	}

	@Override
	public void growNextSkipChild(final IGraphNode parent, final IGraphNode nextChild) {
		// final int newLength = parent.getMatchedTextLength() + nextChild.getMatchedTextLength();
		final int newNextItemIndex = parent.getNextItemIndex();

		parent.addNextGrowingChild(nextChild, newNextItemIndex);
		if (parent.getIsComplete()) {
			this.checkForGoal(parent);
			// clear growing children, add children to list of alternate children

		}

		this.tryAddGrowable(parent);

		// int pri = parent.getPriority();
		// if (nextChild.getRuntimeRule().getIsEmptyRule()) {
		// pri = nextChild.getPriority();
		// }
		// final GraphNodeBranch duplicate = (GraphNodeBranch) this.findOrCreateBranch(parent.getRuntimeRule(), pri, parent.getStartPosition(), newLength,
		// newNextItemIndex, parent.getHeight());
		// duplicate.getChildren().addAll(parent.getChildren());
		// duplicate.getChildren().add((INode) nextChild);
		// nextChild.getPossibleParent().add(duplicate);
		//
		// for (final PreviousInfo info : parent.getPrevious()) {
		// duplicate.addPrevious(info.node, info.atPosition);
		// }
		//
		// // nextChild.addHead(duplicate);
		// this.tryAddGrowable(duplicate);
		//
		// if (duplicate.getIsComplete()) {
		// this.registerCompleteNode(duplicate);
		// }
	}

	// @Override
	// public IGraphNode duplicateWithOtherStack(final int priority, final Set<PreviousInfo> previous) {
	// final GraphNodeBranch duplicate = (GraphNodeBranch) this.graph.findOrCreateBranch(this.getRuntimeRule(), priority, this.getStartPosition(),
	// this.getMatchedTextLength(), this.getNextItemIndex(), this.getHeight());
	// duplicate.getChildren().addAll(this.getChildren());
	//
	// for (final PreviousInfo info : previous) {
	// duplicate.addPrevious(info.node, info.atPosition);
	// }
	// // nextChild.setHead(duplicate);
	// this.graph.tryAddGrowable(duplicate);
	//
	// if (duplicate.getIsComplete()) {
	// this.graph.registerCompleteNode(duplicate);
	// }
	//
	// return duplicate;
	// }

	@Override
	public String toString() {
		return this.goals + System.lineSeparator() + this.growable.size() + "-" + Arrays.toString(this.growable.values().toArray());
	}

}
