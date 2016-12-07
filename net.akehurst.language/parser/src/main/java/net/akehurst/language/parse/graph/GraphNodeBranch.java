package net.akehurst.language.parse.graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import net.akehurst.language.core.parser.IBranch;
import net.akehurst.language.core.parser.INode;
import net.akehurst.language.core.parser.IParseTreeVisitor;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleKind;

public class GraphNodeBranch extends AbstractGraphNode implements IGraphNode, IBranch {

	public GraphNodeBranch(ParseGraph graph, RuntimeRule rr, int priority, int startPosition, int textLength, int nextItemIndex, int height) {
		super(graph, rr, startPosition, textLength);
		this.priority = priority;
		this.nextItemIndex = nextItemIndex;
		this.children = new ArrayList<>();
		this.height = height;
	}

	int priority;
	List<INode> children;
	int nextItemIndex;
	int height;

	@Override
	public IGraphNode duplicateWithNextChild(IGraphNode nextChild) {
		int newLength = this.getMatchedTextLength() + nextChild.getMatchedTextLength();
		int newNextItemIndex = this.getNextItemIndex() + 1;

		// if duplicate will be complete && if its id already exists
		// if (parents are the same) return already existing
		IGraphNode gn = this.graph.findCompleteNode(this.runtimeRule.getRuleNumber(), startPosition, newLength);

		// GraphNodeBranch duplicate = (GraphNodeBranch)this.graph.createBranch(this.runtimeRule, this.priority, this.startPosition, newLength,
		// newNextItemIndex, this.height);
		GraphNodeBranch duplicate = new GraphNodeBranch(graph, this.runtimeRule, this.priority, this.startPosition, newLength, newNextItemIndex, this.height);
		duplicate.children = new ArrayList<>(this.children);
		duplicate.children.add((INode)nextChild);

		for(PreviousInfo info: this.getPrevious()) {
			duplicate.addPrevious(info.node, info.atPosition);
		}

			this.graph.tryAddGrowable(duplicate);

		if (duplicate.getIsComplete()) {
			this.graph.registerCompleteNode(duplicate);
		}

		return duplicate;
	}

	@Override
	public IGraphNode duplicateWithNextSkipChild(IGraphNode nextChild) {
		int newLength = this.getMatchedTextLength() + nextChild.getMatchedTextLength();
		int newNextItemIndex = this.getNextItemIndex();
		GraphNodeBranch duplicate = (GraphNodeBranch) this.graph.createBranch(this.runtimeRule, this.priority, this.startPosition, newLength, newNextItemIndex,
				this.height);
		duplicate.children = new ArrayList<>(this.children);
		duplicate.children.add((INode)nextChild);

		for(PreviousInfo info: this.getPrevious()) {
			duplicate.addPrevious(info.node, info.atPosition);
		}

			this.graph.tryAddGrowable(duplicate);

		if (duplicate.getIsComplete()) {
			this.graph.registerCompleteNode(duplicate);
		}

		return duplicate;
	}

	@Override
	public IGraphNode duplicateWithOtherStack(int priority, List<PreviousInfo> previous) {
		GraphNodeBranch duplicate = (GraphNodeBranch) this.graph.createBranch(this.getRuntimeRule(), priority, this.getStartPosition(), this.getMatchedTextLength(), this.getNextItemIndex(),
				this.getHeight());
		duplicate.children = new ArrayList<>(this.children);

		for(PreviousInfo info: previous) {
			duplicate.addPrevious(info.node, info.atPosition);
		}
			this.graph.tryAddGrowable(duplicate);

		
		if (duplicate.getIsComplete()) {
			this.graph.registerCompleteNode(duplicate);
		}

		return duplicate;
	}
	
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
				return this.nextItemIndex == 1;
			case PRIORITY_CHOICE:
				// a choice can only have one child
				return this.nextItemIndex == 1;
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
				int size = this.nextItemIndex;
				return res || (size > 0 && size >= this.getRuntimeRule().getRhs().getMultiMin()) || this.nextItemIndex == -1; // the -1 is used when creating dummy branch...should
																											// really need the test here!
			}
			case SEPARATED_LIST: {
				int size = this.nextItemIndex;
				return (size % 2) == 1 || this.nextItemIndex == -1; // the -1 is used when creating dummy branch...should really need the test here!
			}
			default:
			break;
		}
		throw new RuntimeException("Internal Error: rule kind not recognised");

	}

	@Override
	public boolean getCanGraftBack() {
		if (getPrevious().isEmpty()) {
			return this.getIsComplete();
		}
		PreviousInfo info = this.getPrevious().get(0);
		return this.getIsComplete() && this.getIsStacked() && info.node.getExpectsItemAt(this.getRuntimeRule(), info.atPosition);
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
				if (this.nextItemIndex < this.getRuntimeRule().getRhs().getItems().length) {
					return true;
				} else {
					return false; // !reachedEnd;
				}
			}
			case MULTI: {
				if (!this.getChildren().isEmpty() && this.getChildAt(0).getRuntimeRule().getIsEmptyRule()) {
					return false;
				}
				int size = this.nextItemIndex;
				int max = this.getRuntimeRule().getRhs().getMultiMax();
				return -1 == max || size < max;
			}
			case SEPARATED_LIST: {
				if (!this.getChildren().isEmpty() && this.getChildAt(0).getRuntimeRule().getIsEmptyRule()) {
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
		return !this.getRuntimeRule().getIsEmptyRule() && this.getRuntimeRule().getKind()==RuntimeRuleKind.NON_TERMINAL;
	}
	
	@Override
	public boolean getIsStacked() {
		return !this.getPrevious().isEmpty();
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
				if (nextItemIndex == 0) {
					return Arrays.asList(this.getRuntimeRule().getRhs().getItems());
				} else {
					return Collections.emptyList();
				}
				// throw new RuntimeException("Internal Error: item is choice");
			}
			case PRIORITY_CHOICE: {
				if (nextItemIndex == 0) {
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
					return Arrays.asList(this.getRuntimeRule().getRhsItem(this.nextItemIndex));
				}
			}
			case MULTI: {
				return Arrays.asList(this.getRuntimeRule().getRhsItem(0));
			}
			case SEPARATED_LIST: {
				if ((this.nextItemIndex % 2) == 1) {
					return Arrays.asList(this.getRuntimeRule().getSeparator());
				} else {
					return Arrays.asList(this.getRuntimeRule().getRhsItem(0));
				}
			}
			default:
			break;
		}
		throw new RuntimeException("Internal Error: rule kind not recognised");
	}

	@Override
	public List<RuntimeRule> getNextExpectedTerminals() {
		//TODO: cache this
		List<RuntimeRule> nextItem = this.getNextExpectedItem();
		ArrayList<RuntimeRule> l = new ArrayList<>();
		for (RuntimeRule r : nextItem) {
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
	public boolean getExpectsItemAt(RuntimeRule item, int atPosition) {
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
	public RuntimeRule getExpectedItemAt(int atPosition) {
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
				if ((this.nextItemIndex % 2) == 1) {
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
	public List<INode> getChildren() {
		return this.children;
	}
	
	public IGraphNode getChildAt(int index) {
		return (IGraphNode)this.children.get(index);
	}

	@Override
	public INode getChild(int index) {
		List<INode> children = this.getChildren();

		//get first non skip child
		int child=0;
		INode n = children.get(child);
		while(n.getIsSkip() && child < (children.size()-1)) {
			++child;
			n = children.get(child);
		}
		if (child >= children.size()) {
			return null;
		}
		int count=0;

		while(count < index && child < (children.size()-1)) {
			++child;
			n = children.get(child);
			while(n.getIsSkip()) {
				++child;
				n = children.get(child);
			}
			++count;
		}

		if (child < children.size()) {
			return n;
		} else {
			return null;
		}}
	
	@Override
	public String getMatchedText() {
		String str = "";
		for(INode n: this.getChildren()) {
			str += n.getMatchedText();
		}
		return str;
	}
	
	List<INode> nonSkipChildren_cache;
	@Override
	public List<INode> getNonSkipChildren() {
		if (null==nonSkipChildren_cache) {
			this.nonSkipChildren_cache = new ArrayList<>();
			for (INode n:this.getChildren()) {
				if (n.getIsSkip()) {
					
				} else {
					this.nonSkipChildren_cache.add(n);
				}
			}
		}
		return this.nonSkipChildren_cache;
	}
	
	@Override
	public <T, A, E extends Throwable> T accept(IParseTreeVisitor<T, A, E> visitor, A arg) throws E {
		return visitor.visit(this, arg);
	}
	
	@Override
	public String toString() {
		return this.getRuntimeRule().getNodeTypeName() + "(" + this.getRuntimeRule().getRuleNumber() + "," + this.getStartPosition() + ","
				+ this.getMatchedTextLength() + "," + this.getNextItemIndex() + ")" + (this.getPrevious().isEmpty() ? "" : " -> " + this.getPrevious().get(0));
	}
}
