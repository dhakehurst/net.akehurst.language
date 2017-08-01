package net.akehurst.language.parse.graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import net.akehurst.language.core.parser.IBranch;
import net.akehurst.language.core.parser.INode;
import net.akehurst.language.core.parser.IParseTreeVisitor;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleKind;

public class GraphNodeBranch extends AbstractGraphNode implements IGraphNode, IBranch {

	public GraphNodeBranch(final ParseGraph graph, final RuntimeRule rr, final int priority, final int startPosition, final int height) {
		super(graph, rr, startPosition);
		this.priority = priority;
		this.nextItemIndex = 0;
		this.growingChildren = new ArrayList<>();
		this.childrenOption = new ArrayList<>();
		this.height = height;
	}

	private final int priority;
	private List<IGraphNode> growingChildren;
	private final List<ChildrenOption> childrenOption;
	private int nextItemIndex;
	private final int height;
	protected int finalMatchedTextLength;
	protected int growingMatchedTextLength;

	// @Override
	// public IGraphNode duplicateWithNextChild(final IGraphNode nextChild) {
	// final int newLength = this.getMatchedTextLength() + nextChild.getMatchedTextLength();
	// int newNextItemIndex = 0;
	// switch (this.getRuntimeRule().getRhs().getKind()) {
	// case CHOICE:
	// newNextItemIndex = -1;
	// break;
	// case CONCATENATION:
	// newNextItemIndex = this.getRuntimeRule().getRhs().getItems().length == this.getNextItemIndex() + 1 ? -1 : this.getNextItemIndex() + 1;
	// break;
	// case EMPTY:
	// newNextItemIndex = -1;
	// break;
	// case MULTI:
	// newNextItemIndex = this.getNextItemIndex() + 1;
	// break;
	// case PRIORITY_CHOICE:
	// newNextItemIndex = -1;
	// break;
	// case SEPARATED_LIST:
	// newNextItemIndex = this.getNextItemIndex() + 1;
	// break;
	// default:
	// throw new RuntimeException("Internal Error: Unknown RuleKind " + this.runtimeRule.getRhs().getKind());
	// }
	//
	// // if duplicate will be complete && if its id already exists
	// // if (parents are the same) return already existing
	// final IGraphNode gn = this.graph.findNode(this.runtimeRule.getRuleNumber(), this.startPosition);
	//
	// // GraphNodeBranch duplicate = (GraphNodeBranch)this.graph.createBranch(this.runtimeRule, this.priority, this.startPosition, newLength,
	// // newNextItemIndex, this.height);
	// int pri = this.priority;
	// if (nextChild.getRuntimeRule().getIsEmptyRule()) {
	// pri = nextChild.getPriority();
	// }
	// // final GraphNodeBranch duplicate = new GraphNodeBranch(this.graph, this.runtimeRule, pri, this.startPosition, newLength, newNextItemIndex,
	// // this.height);
	// final GraphNodeBranch duplicate = (GraphNodeBranch) this.graph.findOrCreateBranch(this.runtimeRule, pri, this.startPosition, newLength,
	// newNextItemIndex, this.height);
	// duplicate.getChildren().addAll(this.getChildren());
	// duplicate.getChildren().add((INode) nextChild);
	// nextChild.getPossibleParent().add(duplicate);
	//
	// for (final PreviousInfo info : this.getPrevious()) {
	// duplicate.addPrevious(info.node, info.atPosition);
	// }
	// //
	// // this.graph.tryAddGrowable(duplicate);
	// /// nextChild.addHead(duplicate);
	// this.graph.tryAddGrowable(duplicate);
	//
	// if (duplicate.getIsComplete()) {
	// this.graph.registerCompleteNode(duplicate);
	// }
	//
	// return duplicate;
	// }
	//
	// @Override
	// public IGraphNode duplicateWithNextSkipChild(final IGraphNode nextChild) {
	// final int newLength = this.getMatchedTextLength() + nextChild.getMatchedTextLength();
	// final int newNextItemIndex = this.getNextItemIndex();
	// int pri = this.priority;
	// if (nextChild.getRuntimeRule().getIsEmptyRule()) {
	// pri = nextChild.getPriority();
	// }
	// final GraphNodeBranch duplicate = (GraphNodeBranch) this.graph.findOrCreateBranch(this.runtimeRule, pri, this.startPosition, newLength,
	// newNextItemIndex, this.height);
	// duplicate.getChildren().addAll(this.getChildren());
	// duplicate.getChildren().add((INode) nextChild);
	// nextChild.getPossibleParent().add(duplicate);
	//
	// for (final PreviousInfo info : this.getPrevious()) {
	// duplicate.addPrevious(info.node, info.atPosition);
	// }
	//
	// // nextChild.addHead(duplicate);
	// this.graph.tryAddGrowable(duplicate);
	//
	// if (duplicate.getIsComplete()) {
	// this.graph.registerCompleteNode(duplicate);
	// }
	//
	// return duplicate;
	// }
	//
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
	public int getNextItemIndex() {
		return this.nextItemIndex;
	}

	@Override
	public boolean getIsLeaf() {
		return false;
	}

	@Override
	public int getPriority() {
		return this.priority;
	}

	@Override
	public int getHeight() {
		return this.height;
	}

	@Override
	public boolean getCanGrow() {
		if (this.getIsStacked()) {
			return true;
		} else {
			return this.getCanGrowWidth();
		}
	}

	@Override
	public boolean getIsSkip() {
		return this.getRuntimeRule().getIsSkipRule();
	}

	@Override
	public boolean getIsComplete() {
		switch (this.getRuntimeRule().getRhs().getKind()) {
			case EMPTY:
			break;
			case CHOICE:
				// a choice can only have one child
				// TODO: should never be 1, should always be -1 if we create nodes correctly
				return this.nextItemIndex == 1 || this.nextItemIndex == -1;
			case PRIORITY_CHOICE:
				// a choice can only have one child
				// TODO: should never be 1, should always be -1 if we create nodes correctly
				return this.nextItemIndex == 1 || this.nextItemIndex == -1;
			case CONCATENATION: {
				return this.getRuntimeRule().getRhs().getItems().length <= this.nextItemIndex || this.nextItemIndex == -1; // the -1 is used when creating dummy
																															// // test here!
			}
			case MULTI: {
				boolean res = false;
				if (0 == this.getRuntimeRule().getRhs().getMultiMin() && this.nextItemIndex == 1) {
					// complete if we have an empty node as child
					res = this.getChildren().isEmpty() ? false : this.getChildAt(0).getRuntimeRule().getIsEmptyRule();
				}
				final int size = this.nextItemIndex;
				return res || size > 0 && size >= this.getRuntimeRule().getRhs().getMultiMin() || this.nextItemIndex == -1; // the -1 is used when creating
																															// dummy branch...should
				// really need the test here!
			}
			case SEPARATED_LIST: {
				final int size = this.nextItemIndex;
				return size % 2 == 1 || this.nextItemIndex == -1; // the -1 is used when creating dummy branch...should really need the test here!
			}
			default:
			break;
		}
		throw new RuntimeException("Internal Error: rule kind not recognised");

	}

	@Override
	public boolean getCanGraftBack() {
		if (this.getPrevious().isEmpty()) {
			return this.getIsComplete();
		}
		boolean b = false;
		for (final PreviousInfo info : this.getPrevious()) {
			b = b || info.node.getExpectsItemAt(this.getRuntimeRule(), info.atPosition);
		}
		return b && this.getIsComplete() && this.getIsStacked();
	}

	@Override
	public boolean getCanGrowWidth() {
		// boolean reachedEnd = this.getMatchedTextLength() >= inputLength;
		// if (reachedEnd)
		// return false;
		if (this.getIsComplete() && this.getRuntimeRule().getIsEmptyRule()) {
			return false;
		}
		switch (this.getRuntimeRule().getRhs().getKind()) {
			case EMPTY: {
				return false;
			}
			case CHOICE: {
				return this.nextItemIndex == 0;
			}
			case PRIORITY_CHOICE: {
				return this.nextItemIndex == 0;
			}
			case CONCATENATION: {
				if (this.nextItemIndex != -1 && this.nextItemIndex < this.getRuntimeRule().getRhs().getItems().length) {
					return true;
				} else {
					return false; // !reachedEnd;
				}
			}
			case MULTI: {
				// not sure we need the test for isEmpty, because if it is empty it should be complete or NOT!???
				if (!this.growingChildren.isEmpty() && this.growingChildren.get(0).getRuntimeRule().getIsEmptyRule()) {
					return false;
				}
				final int size = this.nextItemIndex;
				final int max = this.getRuntimeRule().getRhs().getMultiMax();
				return -1 == max || size < max;
			}
			case SEPARATED_LIST: {
				if (!this.growingChildren.isEmpty() && this.growingChildren.get(0).getRuntimeRule().getIsEmptyRule()) {
					return false;
				}
				return true;
			}
			default:
			break;
		}
		throw new RuntimeException("Internal Error: rule kind not recognised");

	}

	@Override
	public boolean getCanGrowWidthWithSkip() {
		return !this.getRuntimeRule().getIsEmptyRule() && this.getRuntimeRule().getKind() == RuntimeRuleKind.NON_TERMINAL;
	}

	@Override
	public boolean hasNextExpectedItem() {
		switch (this.getRuntimeRule().getRhs().getKind()) {
			case EMPTY:
				return false;
			case CHOICE:
				return this.nextItemIndex == 0;
			case PRIORITY_CHOICE:
				return this.nextItemIndex == 0;
			case CONCATENATION: {
				if (this.nextItemIndex >= this.getRuntimeRule().getRhs().getItems().length) {
					return false;
				} else {
					return true;
				}
			}
			case MULTI: {
				return true;
			}
			case SEPARATED_LIST:
				return true;
			default:
			break;
		}
		throw new RuntimeException("Internal Error: rule kind not recognised");
	}

	@Override
	public List<RuntimeRule> getNextExpectedItem() {
		switch (this.getRuntimeRule().getRhs().getKind()) {
			case EMPTY:
			break;
			case CHOICE: {
				if (this.nextItemIndex == 0) {
					return Arrays.asList(this.getRuntimeRule().getRhs().getItems());
				} else {
					return Collections.emptyList();
				}
				// throw new RuntimeException("Internal Error: item is choice");
			}
			case PRIORITY_CHOICE: {
				if (this.nextItemIndex == 0) {
					return Arrays.asList(this.getRuntimeRule().getRhs().getItems());
				} else {
					return Collections.emptyList();
				}
				// throw new RuntimeException("Internal Error: item is priority choice");
			}
			case CONCATENATION: {
				if (this.nextItemIndex >= this.getRuntimeRule().getRhs().getItems().length) {
					throw new RuntimeException("Internal Error: No NextExpectedItem");
				} else {
					if (-1 == this.nextItemIndex) {
						return Collections.emptyList();
					} else {
						return Arrays.asList(this.getRuntimeRule().getRhsItem(this.nextItemIndex));
					}
				}
			}
			case MULTI: {
				if (0 == this.nextItemIndex && 0 == this.getRuntimeRule().getRhs().getMultiMin()) {
					return Arrays.asList(this.getRuntimeRule().getRhsItem(0), this.getRuntimeRule().getRuntimeRuleSet().getEmptyRule(this.getRuntimeRule()));
				} else {
					return Arrays.asList(this.getRuntimeRule().getRhsItem(0));
				}
			}
			case SEPARATED_LIST: {
				if (this.nextItemIndex % 2 == 1) {
					return Arrays.asList(this.getRuntimeRule().getSeparator());
				} else {
					if (0 == this.nextItemIndex && 0 == this.getRuntimeRule().getRhs().getMultiMin()) {
						return Arrays.asList(this.getRuntimeRule().getRhsItem(0),
								this.getRuntimeRule().getRuntimeRuleSet().getEmptyRule(this.getRuntimeRule()));
					} else {
						return Arrays.asList(this.getRuntimeRule().getRhsItem(0));
					}
				}
			}
			default:
			break;
		}
		throw new RuntimeException("Internal Error: rule kind not recognised");
	}

	@Override
	public List<RuntimeRule> getNextExpectedTerminals() {
		// TODO: cache this
		final List<RuntimeRule> nextItem = this.getNextExpectedItem();
		final ArrayList<RuntimeRule> l = new ArrayList<>();
		for (final RuntimeRule r : nextItem) {
			l.addAll(Arrays.asList(this.getRuntimeRule().getRuntimeRuleSet().getPossibleFirstTerminals(r)));
		}
		// add a possible empty rule
		if (0 == this.getNextItemIndex()) {
			switch (this.getRuntimeRule().getRhs().getKind()) {
				case EMPTY:
				break;

				case CHOICE:
				break;
				case PRIORITY_CHOICE:
				break;
				case CONCATENATION:
				break;
				case MULTI: {
					if (this.getRuntimeRule().getRhs().getMultiMin() == 0) {
						l.add(this.getRuntimeRule().getRuntimeRuleSet().getEmptyRule(this.getRuntimeRule()));
					}
				}
				break;
				case SEPARATED_LIST: {
					if (this.getRuntimeRule().getRhs().getMultiMin() == 0) {
						l.add(this.getRuntimeRule().getRuntimeRuleSet().getEmptyRule(this.getRuntimeRule()));
					}
				}
				break;
				default:
				break;
			}
		}
		return l;
	}

	@Override
	public boolean getExpectsItemAt(final RuntimeRule item, final int atPosition) {
		return this.getRuntimeRule().couldHaveChild(item, atPosition);

		// switch (this.getRuntimeRule().getRhs().getKind()) {
		// case EMPTY:
		// break;
		// case CHOICE: {
		// if (0 ==atPosition) {
		// return this.getRuntimeRule().couldHaveChild(possibleChild, atPosition)
		// }
		// throw new RuntimeException("Internal Error: item is choice");
		// }
		// case PRIORITY_CHOICE: {
		// throw new RuntimeException("Internal Error: item is priority choice");
		// }
		// case CONCATENATION: {
		// if (atPosition >= this.getRuntimeRule().getRhs().getItems().length) {
		// throw new RuntimeException("Internal Error: No Item at position "+atPosition);
		// } else {
		// return this.getRuntimeRule().getRhsItem(atPosition).getRuleNumber() == item.getRuleNumber();
		// }
		// }
		// case MULTI: {
		// return this.getRuntimeRule().getRhsItem(0).getRuleNumber() == item.getRuleNumber();
		// }
		// case SEPARATED_LIST: {
		// if ((this.nextItemIndex % 2) == 1) {
		// return this.getRuntimeRule().getSeparator().getRuleNumber() == item.getRuleNumber();
		// } else {
		// return this.getRuntimeRule().getRhsItem(0).getRuleNumber() == item.getRuleNumber();
		// }
		// }
		// default:
		// break;
		// }
		// throw new RuntimeException("Internal Error: rule kind not recognised");
	}

	@Override
	public RuntimeRule getExpectedItemAt(final int atPosition) {
		switch (this.getRuntimeRule().getRhs().getKind()) {
			case EMPTY:
			break;
			case CHOICE: {
				throw new RuntimeException("Internal Error: item is choice");
			}
			case PRIORITY_CHOICE: {
				throw new RuntimeException("Internal Error: item is priority choice");
			}
			case CONCATENATION: {
				if (atPosition >= this.getRuntimeRule().getRhs().getItems().length) {
					throw new RuntimeException("Internal Error: No Item at position " + atPosition);
				} else {
					return this.getRuntimeRule().getRhsItem(atPosition);
				}
			}
			case MULTI: {
				return this.getRuntimeRule().getRhsItem(0);
			}
			case SEPARATED_LIST: {
				if (this.nextItemIndex % 2 == 1) {
					return this.getRuntimeRule().getSeparator();
				} else {
					return this.getRuntimeRule().getRhsItem(0);
				}
			}
			default:
			break;
		}
		throw new RuntimeException("Internal Error: rule kind not recognised");
	}

	@Override
	public boolean getIsEmptyLeaf() {
		return false;
	}

	@Override
	public boolean getIsEmpty() {
		if (this.getNonSkipChildren().isEmpty()) {
			return true;
		} else {
			if (this.getNonSkipChildren().get(0).getIsEmptyLeaf()) {
				return true;
			} else {
				return false;
			}
		}
	}

	@Override
	public int getNextInputPosition() {
		return this.startPosition + this.growingMatchedTextLength;
	}

	public List<IGraphNode> getGrowingChildren() {
		return this.growingChildren;
	}

	@Override
	public void addNextGrowingChild(final IGraphNode nextChild, final int nextItemIndex) {
		this.growingChildren.add(nextChild);
		this.nextItemIndex = nextItemIndex;
		this.growingMatchedTextLength += nextChild.getMatchedTextLength();
		if (this.getIsComplete()) {
			this.addChildrenOption(this.growingChildren, this.growingMatchedTextLength);
			this.growingChildren = new ArrayList<>();
		}
	}

	private void addChildrenOption(final List<IGraphNode> children, final int length) {
		if (this.getChildrenOption().isEmpty()) {
			final ChildrenOption opt = new ChildrenOption();
			opt.matchedLength = length;
			opt.nodes = children;
			this.getChildrenOption().add(opt);
			this.finalMatchedTextLength = length;
		} else {

			// sort out priorities!
			throw new UnsupportedOperationException();
		}
	}

	@Override
	public List<ChildrenOption> getChildrenOption() {
		return this.childrenOption;
	}

	// --- IBranch ---
	@Override
	public int getMatchedTextLength() {
		return this.finalMatchedTextLength;
	}

	@Override
	public List<INode> getChildren() {
		if (this.getChildrenOption().isEmpty()) {
			return Collections.emptyList();
		} else {
			final ChildrenOption opt = this.getChildrenOption().get(0);
			return (List<INode>) (List<?>) opt.nodes;
		}
	}

	public IGraphNode getChildAt(final int index) {
		return (IGraphNode) this.getChildren().get(index);
	}

	@Override
	public INode getChild(final int index) {
		final List<INode> children = this.getChildren();

		// get first non skip child
		int child = 0;
		INode n = children.get(child);
		while (n.getIsSkip() && child < children.size() - 1) {
			++child;
			n = children.get(child);
		}
		if (child >= children.size()) {
			return null;
		}
		int count = 0;

		while (count < index && child < children.size() - 1) {
			++child;
			n = children.get(child);
			while (n.getIsSkip()) {
				++child;
				n = children.get(child);
			}
			++count;
		}

		if (child < children.size()) {
			return n;
		} else {
			return null;
		}
	}

	@Override
	public IBranch getBranchChild(final int i) {
		final INode n = this.getChild(i);
		return (IBranch) n;
	}

	@Override
	public List<IBranch> getBranchNonSkipChildren() {
		final List<IBranch> res = this.getNonSkipChildren().stream().filter(IBranch.class::isInstance).map(IBranch.class::cast).collect(Collectors.toList());
		return res;
	}

	@Override
	public String getMatchedText() {
		String str = "";
		for (final INode n : this.getChildren()) {
			str += n.getMatchedText();
		}
		return str;
	}

	List<INode> nonSkipChildren_cache;

	@Override
	public List<INode> getNonSkipChildren() {
		if (null == this.nonSkipChildren_cache) {
			this.nonSkipChildren_cache = new ArrayList<>();
			for (final INode n : this.getChildren()) {
				if (n.getIsSkip()) {

				} else {
					this.nonSkipChildren_cache.add(n);
				}
			}
		}
		return this.nonSkipChildren_cache;
	}

	@Override
	public <T, A, E extends Throwable> T accept(final IParseTreeVisitor<T, A, E> visitor, final A arg) throws E {
		return visitor.visit(this, arg);
	}

	@Override
	public String toString() {
		String prev = "";
		if (this.getPrevious().isEmpty()) {
			// nothing
		} else if (this.getPrevious().size() == 1) {
			prev = " --> " + this.getPrevious().iterator().next();
		} else {
			prev = " -*> " + this.getPrevious().iterator().next();
		}
		String r = "";
		r += this.getStartPosition() + ",";
		r += (this.growingChildren.isEmpty() ? this.getMatchedTextLength() : this.growingMatchedTextLength) + ",";
		r += -1 == this.getNextItemIndex() ? "C" : this.getNextItemIndex();
		r += ":" + this.getRuntimeRule().getNodeTypeName() + "(" + this.getRuntimeRule().getRuleNumber() + ")";
		r += prev;
		return r;
		// return this.getRuntimeRule().getNodeTypeName() + "(" + this.getRuntimeRule().getRuleNumber() + "," + this.getStartPosition() + ","
		// + this.getMatchedTextLength() + "," + this.getNextItemIndex() + ")" + prev;
	}
}
