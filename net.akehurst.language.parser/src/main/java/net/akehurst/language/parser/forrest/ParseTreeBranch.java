package net.akehurst.language.parser.forrest;

import net.akehurst.language.core.parser.INode;
import net.akehurst.language.core.parser.ParseTreeException;
import net.akehurst.language.ogl.semanticModel.Choice;
import net.akehurst.language.ogl.semanticModel.Concatenation;
import net.akehurst.language.ogl.semanticModel.Multi;
import net.akehurst.language.ogl.semanticModel.RuleItem;
import net.akehurst.language.ogl.semanticModel.SeparatedList;
import net.akehurst.language.ogl.semanticModel.SkipNodeType;
import net.akehurst.language.parser.ToStringVisitor;
import net.akehurst.language.parser.runtime.Factory;
import net.akehurst.language.parser.runtime.RuntimeRule;

public class ParseTreeBranch extends AbstractParseTree {
	
	public ParseTreeBranch(Factory factory, Input input, Branch root, AbstractParseTree stackedTree, RuntimeRule rule, int nextItemIndex) {
		super(factory, input, root, stackedTree);
		this.rule = rule;
		this.nextItemIndex = nextItemIndex;
		this.canGrow = this.calculateCanGrow();
		this.complete = this.calculateIsComplete();
		this.canGrowWidth = this.calculateCanGrowWidth();
		this.hashCode_cache = this.getRoot().hashCode();
	}
	
	RuntimeRule rule;
	int nextItemIndex;
	boolean canGrow;
	boolean complete;
	boolean canGrowWidth;
	
	@Override
	public boolean getCanGrow() {
		return this.canGrow;
	}
	
	@Override
	public boolean getIsComplete() {
		return this.complete;
	}
	
	@Override
	public boolean getCanGraftBack() {
		return this.getIsComplete() ;
	}
	
	@Override
	public boolean getCanGrowWidth() {
		return this.canGrowWidth;
	}
	
	@Override
	public Branch getRoot() {
		return (Branch)super.getRoot();
	}
	
	public ParseTreeBranch extendWith(INode extension) throws ParseTreeException {
		Branch nb = (Branch)this.getRoot().addChild(extension);
//		Stack<AbstractParseTree> stack = new Stack<>();
//		stack.addAll(this.stackedRoots);
		if (extension.getNodeType() instanceof SkipNodeType) {
			ParseTreeBranch newBranch = new ParseTreeBranch(this.factory, this.input, nb, this.stackedTree, this.rule, this.nextItemIndex);
			return newBranch;			
		} else {
			ParseTreeBranch newBranch = new ParseTreeBranch(this.factory, this.input, nb, this.stackedTree, this.rule, this.nextItemIndex+1);
			return newBranch;
		}
	}
	
	@Override
	public RuntimeRule getNextExpectedItem() {
		switch(this.rule.getRhs().getKind()) {
		case CHOICE: {
			throw new RuntimeException("Internal Error: item is choice");
		}
		case CONCATENATION: {
			if (this.nextItemIndex >= this.rule.getRhs().getItems().length) {
				throw new RuntimeException("Internal Error: No NextExpectedItem");
			} else {
				return this.rule.getRhsItem(this.nextItemIndex);
			}
		}
		case MULTI: {
			return this.rule.getRhsItem(0);
		}
		case SEPARATED_LIST: {
			if ( (this.nextItemIndex % 2) == 1 ) {
				return this.rule.getSeparator();
			} else {
				return this.rule.getRhsItem(0);
			}
		}
		default:
			break;
		}
		throw new RuntimeException("Internal Error: rule kind not recognised");
	}

	boolean calculateIsComplete() {
		switch(this.rule.getRhs().getKind()) {
		case CHOICE:
			return true;

		case CONCATENATION: {
			return this.rule.getRhs().getItems().length <= this.nextItemIndex;
		}
		case MULTI: {
			int size = this.nextItemIndex;
			return size >= this.rule.getRhs().getMultiMin();
		}
		case SEPARATED_LIST: {
			int size = this.nextItemIndex;
			return (size % 2) == 1;
		}
		default:
			break;
		}
		throw new RuntimeException("Internal Error: rule kind not recognised");
	}
	boolean calculateCanGrow() {
		if (this.stackedTree!=null) return true;
		return this.calculateCanGrowWidth();
	}
	
	boolean calculateCanGrowWidth() {
		boolean reachedEnd = this.getRoot().getMatchedTextLength() >= this.input.getLength();
		if (reachedEnd)
			return false;
		switch(this.rule.getRhs().getKind()) {
		case CHOICE: {
			return false;
		}
		case CONCATENATION: {
			if ( this.nextItemIndex < this.rule.getRhs().getItems().length ) {
				return true;
			} else {
				return false; //!reachedEnd;
			}
		}
		case MULTI: {
			int size = this.nextItemIndex;
			int max = this.rule.getRhs().getMultiMax();
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
	
//	public ParseTreeBranch deepClone() {
//		Stack<AbstractParseTree> stack = new Stack<>();
//		stack.addAll(this.stackedRoots);
//		ParseTreeBranch clone = new ParseTreeBranch(this.input, this.getRoot(), stack, this.rule, this.nextItemIndex);
//		return clone;
//	}
	
	//--- Object ---
	static ToStringVisitor v = new ToStringVisitor();
	String toString_cache;
	@Override
	public String toString() {
		if (null==this.toString_cache) {
			this.toString_cache = this.accept(v, "");
		}
		return this.toString_cache;
	}
	
	int hashCode_cache;
	@Override
	public int hashCode() {
		return hashCode_cache;
	}
	
	@Override
	public boolean equals(Object arg) {
		if (!(arg instanceof ParseTreeBranch)) {
			return false;
		}
		ParseTreeBranch other = (ParseTreeBranch)arg;
		if ( this.getRoot().getStart() != other.getRoot().getStart() ) {
			return false;
		}
		if ( this.getRoot().getEnd() != other.getRoot().getEnd() ) {
			return false;
		}
		if ( this.getRoot().getRuntimeRule().getRuleNumber() != other.getRoot().getRuntimeRule().getRuleNumber() ) {
			return false;
		}
		if (this.complete != other.complete) {
			return false;
		}
		if (null==this.stackedTree && null==other.stackedTree) {
			return true;
		}
		if (!this.stackedTree.equals(other.stackedTree)) {
			return false;
		}
		return true;

	}
}
