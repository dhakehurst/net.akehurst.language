package net.akehurst.language.parse.graph;

import java.util.ArrayList;
import java.util.List;

import net.akehurst.language.core.parser.INode;
import net.akehurst.language.core.parser.INodeIdentity;
import net.akehurst.language.grammar.parser.forrest.NodeIdentifier;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;
import net.akehurst.language.ogl.semanticStructure.Grammar;

public class GraphNodeBranch implements IGraphNode {

	public GraphNodeBranch(IParseGraph graph, RuntimeRule rr, int priority, IGraphNode firstChild, int nextItemIndex) {
		this.graph = graph;
		this.runtimeRule = rr;
		this.priority = priority;
		this.children = new ArrayList<>();
		this.children.add(firstChild);
		this.nextItemIndex = nextItemIndex;
		this.identifier = new NodeIdentifier(rr.getRuleNumber(), firstChild.getStartPosition(), firstChild.getEndPosition(), nextItemIndex);
		this.previous = new ArrayList<>();
	}

	IParseGraph graph;
	RuntimeRule runtimeRule;
	int priority;
	List<IGraphNode> children;
	int nextItemIndex;

	List<IGraphNode> previous;

	NodeIdentifier identifier;

	@Override
	public NodeIdentifier getIdentifier() {
		return this.identifier;
	}

	@Override
	public boolean getIsLeaf() {
		return false;
	}

	@Override
	public boolean getIsEmpty() {
		boolean empty = true;
		for (IGraphNode c : this.children) {
			empty &= c.getIsEmpty();
		}
		return empty;
	}

	@Override
	public RuntimeRule getRuntimeRule() {
		return this.runtimeRule;
	}

	@Override
	public int getPriority() {
		return this.priority;
	}

	@Override
	public int getStartPosition() {
		return this.children.get(0).getStartPosition();
	}

	@Override
	public int getEndPosition() {
		return this.children.get(this.children.size() - 1).getEndPosition();
	}

	@Override
	public int getMatchedTextLength() {
		return getEndPosition() - getStartPosition();
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
				return true;
			case PRIORITY_CHOICE:
				return true;
			case CONCATENATION: {
				return this.getRuntimeRule().getRhs().getItems().length <= this.nextItemIndex || this.nextItemIndex == -1; // the -1 is used when creating dummy
																															// // test here!
			}
			case MULTI: {
				int size = this.nextItemIndex;
				return size >= this.getRuntimeRule().getRhs().getMultiMin() || this.nextItemIndex == -1; // the -1 is used when creating dummy branch...should
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
		return this.getIsComplete() && this.getIsStacked();
	}

	@Override
	public boolean getCanGrowWidth() {
		// boolean reachedEnd = this.getMatchedTextLength() >= inputLength;
		// if (reachedEnd)
		// return false;
		if (this.getIsComplete() && this.getIsEmpty()) {
			return false;
		}
		switch (this.getRuntimeRule().getRhs().getKind()) {
			case EMPTY: {
				return false;
			}
			case CHOICE: {
				return false;
			}
			case PRIORITY_CHOICE: {
				return false;
			}
			case CONCATENATION: {
				if (this.nextItemIndex < this.getRuntimeRule().getRhs().getItems().length) {
					return true;
				} else {
					return false; // !reachedEnd;
				}
			}
			case MULTI: {
				int size = this.nextItemIndex;
				int max = this.getRuntimeRule().getRhs().getMultiMax();
				return -1 == max || size < max;
			}
			case SEPARATED_LIST: {
				return true;
			}
			default:
			break;
		}
		throw new RuntimeException("Internal Error: rule kind not recognised");

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
				return false;
			case PRIORITY_CHOICE:
				return false;
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
	public RuntimeRule getNextExpectedItem() {
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
				if (this.nextItemIndex >= this.getRuntimeRule().getRhs().getItems().length) {
					throw new RuntimeException("Internal Error: No NextExpectedItem");
				} else {
					return this.getRuntimeRule().getRhsItem(this.nextItemIndex);
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
	public List<IGraphNode> getChildren() {
		return this.children;
	}

	@Override
	public IGraphNode pushToStackOf(IGraphNode next) {
		next.getPrevious().add(this);
		return next;
	}

	@Override
	public List<IGraphNode> getPrevious() {
		return this.previous;
	}

	@Override
	public IGraphNode addChild(IGraphNode gn) {
		this.children.add(gn);
		this.nextItemIndex++;
		if (this.getCanGrow()) {
			this.graph.getGrowable().add(this);
		}
		return this;
	}

	@Override
	public IGraphNode addSkipChild(IGraphNode gn) {
		this.children.add(gn);
		return this;
	}

	@Override
	public IGraphNode replace(IGraphNode newNode) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String toString() {
		return this.getIdentifier().toString();
	}
}
