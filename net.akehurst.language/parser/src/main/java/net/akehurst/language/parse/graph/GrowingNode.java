package net.akehurst.language.parse.graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.akehurst.language.grammar.parser.runtime.RuntimeRule;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleKind;

public class GrowingNode implements IGrowingNode {

	public GrowingNode(final RuntimeRule runtimeRule, final int startPosition, final int endPosition, final int nextItemIndex, final int priority,
			final List<ICompleteNode> children) {
		this.runtimeRule = runtimeRule;
		this.startPosition = startPosition;
		this.endPosition = endPosition;
		this.nextItemIndex = nextItemIndex;
		this.priority = priority;
		this.children = children;
		this.previous = new HashSet<>();
		this.next = new HashSet<>();
	}

	private final RuntimeRule runtimeRule;
	private final int startPosition;
	private final int endPosition;
	private final int nextItemIndex;
	private final int priority;
	private final List<ICompleteNode> children;
	private Set<PreviousInfo> previous;
	private final Set<IGrowingNode> next;

	@Override
	public RuntimeRule getRuntimeRule() {
		return this.runtimeRule;
	}

	@Override
	public int getRuntimeRuleNumber() {
		return this.runtimeRule.getRuleNumber();
	}

	@Override
	public int getStartPosition() {
		return this.startPosition;
	}

	@Override
	public int getEndPosition() {
		return this.endPosition;
	}

	@Override
	public int getNextItemIndex() {
		return this.nextItemIndex;
	}

	@Override
	public int getPriority() {
		return this.priority;
	}

	@Override
	public int getMatchedTextLength() {
		return this.endPosition - this.startPosition;
	}

	@Override
	public boolean getIsSkip() {
		return this.getRuntimeRule().getIsSkipRule();
	}

	@Override
	public boolean getHasCompleteChildren() {
		if (RuntimeRuleKind.TERMINAL == this.getRuntimeRule().getKind()) {
			return true;
		} else {
			switch (this.getRuntimeRule().getRhs().getKind()) {
				case EMPTY:
					return true;
				case CHOICE:
					// a choice can only have one child
					// TODO: should never be 1, should always be -1 if we create nodes correctly
					return this.nextItemIndex == 1 || this.nextItemIndex == -1;
				case PRIORITY_CHOICE:
					// a choice can only have one child
					// TODO: should never be 1, should always be -1 if we create nodes correctly
					return this.nextItemIndex == 1 || this.nextItemIndex == -1;
				case CONCATENATION: {
					return this.getRuntimeRule().getRhs().getItems().length <= this.nextItemIndex || this.nextItemIndex == -1; // the -1 is used when creating
																																// dummy
																																// // test here!
				}
				case MULTI: {
					boolean res = false;
					if (0 == this.getRuntimeRule().getRhs().getMultiMin() && this.nextItemIndex == 1) {
						// complete if we have an empty node as child
						res = this.getGrowingChildren().isEmpty() ? false : this.getGrowingChildren().get(0).getRuntimeRule().getIsEmptyRule();
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
					throw new RuntimeException("Internal Error: rule kind not recognised");
			}
		}
	}

	@Override
	public boolean getCanGrowWidth() {
		if (this.getIsLeaf()) {
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
				if (!this.getGrowingChildren().isEmpty() && this.getGrowingChildren().get(0).getRuntimeRule().getIsEmptyRule()) {
					return false;
				}
				final int size = this.nextItemIndex;
				final int max = this.getRuntimeRule().getRhs().getMultiMax();
				return -1 != size && (-1 == max || size < max);
			}
			case SEPARATED_LIST: {
				if (!this.getGrowingChildren().isEmpty() && this.getGrowingChildren().get(0).getRuntimeRule().getIsEmptyRule()) {
					return false;
				}
				final int size = this.nextItemIndex;
				final int max = this.getRuntimeRule().getRhs().getMultiMax();
				final int x = size / 2;
				return -1 != size && (-1 == max || x < max);
			}
			default:
				throw new RuntimeException("Internal Error: rule kind not recognised");
		}
	}

	@Override
	public boolean getCanGraftBack(final Set<IGrowingNode.PreviousInfo> previous) {
		if (previous.isEmpty()) {
			return false;
		}
		boolean b = false;
		for (final PreviousInfo info : previous) {
			b = b || info.node.getExpectsItemAt(this.getRuntimeRule(), info.atPosition);
		}
		return b && this.getHasCompleteChildren();// && this.getIsStacked();
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
	public int getNextInputPosition() {
		return this.endPosition;
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
				throw new RuntimeException("Internal Error: rule kind not recognised");
		}
	}

	@Override
	public boolean getExpectsItemAt(final RuntimeRule runtimeRule, final int atPosition) {
		return this.getRuntimeRule().couldHaveChild(runtimeRule, atPosition);
	}

	// @Override
	// public boolean getIsStacked() {
	// return !this.getPrevious().isEmpty();
	// }

	@Override
	public Set<PreviousInfo> getPrevious() {
		return this.previous;
	}

	@Override
	public void newPrevious() {
		this.previous = new HashSet<>();
	}

	@Override
	public void addPrevious(final IGrowingNode previousNode, final int atPosition) {
		final PreviousInfo info = new PreviousInfo(previousNode, atPosition);
		this.previous.add(info);
		previousNode.addNext(this);
	}

	@Override
	public Set<IGrowingNode> getNext() {
		return this.next;
	}

	@Override
	public void addNext(final IGrowingNode value) {
		this.next.add(value);
	}

	@Override
	public void removeNext(final IGrowingNode value) {
		this.next.remove(value);
	}

	@Override
	public List<RuntimeRule> getNextExpectedItem() {
		switch (this.getRuntimeRule().getRhs().getKind()) {
			case EMPTY:
				return Collections.emptyList();
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
				throw new RuntimeException("Internal Error: rule kind not recognised");
		}
	}

	@Override
	public boolean getIsLeaf() {
		return this.getRuntimeRule().getIsEmptyRule() || this.getRuntimeRule().getKind() == RuntimeRuleKind.TERMINAL;
	}

	@Override
	public List<ICompleteNode> getGrowingChildren() {
		return this.children;
	}

	private String previousToString(final Set<GrowingNode> visited) {
		visited.add(this);
		String s = "";
		if (this.getPrevious().isEmpty()) {
			//
		} else {
			final GrowingNode prev = (GrowingNode) this.getPrevious().iterator().next().node;
			if (visited.contains(prev)) {
				s = "...";
			} else if (this.getPrevious().size() == 1) {
				s = " --> " + prev.previousToString(visited);
			} else {
				final int sz = this.getPrevious().size();
				s = " -" + sz + "> " + prev.previousToString(visited);
			}
		}
		String r = "";
		r += this.getStartPosition() + ",";
		r += this.getEndPosition() + ",";
		r += -1 == this.getNextItemIndex() ? "C" : this.getNextItemIndex();
		r += ":" + this.getRuntimeRule().getNodeTypeName() + "(" + this.getRuntimeRule().getRuleNumber() + ")";
		r += s;
		return r;
	}

	// --- Object ---
	@Override
	public String toString() {
		final HashSet<GrowingNode> visited = new HashSet<>();
		final String s = this.previousToString(visited);

		return s;
	}
}
