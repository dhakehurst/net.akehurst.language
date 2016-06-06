package net.akehurst.language.grammar.parser.forrest;

import net.akehurst.language.grammar.parse.tree.Branch;
import net.akehurst.language.grammar.parse.tree.Node;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;

public class ParseTreeBranch2 extends AbstractParseTree2 {

	public ParseTreeBranch2(Node root, int nextItemIndex, int inputLength) {
		super(root, nextItemIndex);
		this.isComplete = this.calculateIsComplete();
		this.canGrowWidth = this.calculateCanGrowWidth(inputLength);
	}

	@Override
	public Branch getRoot() {
		return (Branch)super.getRoot();
	}
	
	boolean isComplete;
	@Override
	public boolean getIsComplete() {
		return this.isComplete;
	}
	
	boolean canGrowWidth;
	@Override
	public boolean getCanGrowWidth() {
		return this.canGrowWidth;
	}
	
	boolean calculateCanGrowWidth(int inputLength) {
		boolean reachedEnd = this.getRoot().getMatchedTextLength() >= inputLength;
		if (reachedEnd)
			return false;
		if (this.getIsComplete() && this.getRoot().getIsEmpty()) {
			return false;
		}
		switch(this.getRuntimeRule().getRhs().getKind()) {
		case EMPTY : {
			return false;
		}
		case CHOICE: {
			return false;
		}
		case CONCATENATION: {
			if ( this.nextItemIndex < this.getRuntimeRule().getRhs().getItems().length ) {
				return true;
			} else {
				return false; //!reachedEnd;
			}
		}
		case MULTI: {
			int size = this.nextItemIndex;
			int max = this.getRuntimeRule().getRhs().getMultiMax();
			return -1==max || size < max;
		}
		case SEPARATED_LIST: {
			return true;
		}
		default:
			break;
		}
		throw new RuntimeException("Internal Error: rule kind not recognised");
	}

	boolean calculateIsComplete() {
		switch(this.getRuntimeRule().getRhs().getKind()) {
		case CHOICE:
			return true;

		case CONCATENATION: {
			return this.getRuntimeRule().getRhs().getItems().length <= this.nextItemIndex || this.nextItemIndex==-1; //the -1 is used when creating dummy branch...should really need the test here!
		}
		case MULTI: {
			int size = this.nextItemIndex;
			return size >= this.getRuntimeRule().getRhs().getMultiMin() || this.nextItemIndex==-1; //the -1 is used when creating dummy branch...should really need the test here!
		}
		case SEPARATED_LIST: {
			int size = this.nextItemIndex;
			return (size % 2) == 1 || this.nextItemIndex==-1; //the -1 is used when creating dummy branch...should really need the test here!
		}
		default:
			break;
		}
		throw new RuntimeException("Internal Error: rule kind not recognised");
	}
	
	@Override
	public RuntimeRule getNextExpectedItem() {
		switch(this.getRuntimeRule().getRhs().getKind()) {
		case CHOICE: {
			throw new RuntimeException("Internal Error: item is choice");
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
			if ( (this.nextItemIndex % 2) == 1 ) {
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
}
