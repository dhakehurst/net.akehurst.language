package net.akehurst.language.parser.forrest;

import java.util.Stack;

import net.akehurst.language.core.parser.IBranch;
import net.akehurst.language.core.parser.INode;
import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.core.parser.ParseTreeException;
import net.akehurst.language.ogl.semanticModel.Choice;
import net.akehurst.language.ogl.semanticModel.Concatination;
import net.akehurst.language.ogl.semanticModel.Multi;
import net.akehurst.language.ogl.semanticModel.Rule;
import net.akehurst.language.ogl.semanticModel.RuleItem;
import net.akehurst.language.ogl.semanticModel.SeparatedList;
import net.akehurst.language.ogl.semanticModel.SkipNodeType;
import net.akehurst.language.ogl.semanticModel.TangibleItem;
import net.akehurst.language.parser.ToStringVisitor;

public class ParseTreeBranch extends AbstractParseTree {

	public ParseTreeBranch(Input input, IBranch root, Stack<AbstractParseTree> stack, Rule rule, int nextItemIndex) {
		super(input, root, stack);
		this.rule = rule;
		this.nextItemIndex = nextItemIndex;
		this.canGrow = this.calculateCanGrow();
		this.complete = this.calculateIsComplete();
	}
	
//	public ParseTreeBranch(INodeType nodeType, IParseTree branchTree, Rule rule, Input input) {
//		super(input);
//		List<INode> children = new ArrayList<>();
//		children.add(branchTree.getRoot());
//		Branch branch = new Branch(nodeType,children);
//		this.rule = rule;
//		this.root = branch;
//		this.nextItemIndex = 1;
//		this.length = 1;
//		super.canGrow = this.calculateCanGrow();
//		super.complete = this.calculateIsComplete();
//	}
	
	Rule rule;
	int nextItemIndex;
	boolean canGrow;
	boolean complete;
	
	@Override
	boolean getCanGrow() {
		return this.canGrow;
	}
	
	@Override
	public boolean getIsComplete() {
		return this.complete;
	}
	
	@Override
	public Branch getRoot() {
		return (Branch)super.getRoot();
	}
	
	public ParseTreeBranch extendWith(INode extension) throws ParseTreeException {
		IBranch nb = this.getRoot().addChild(extension);
		Stack<AbstractParseTree> stack = new Stack<>();
		stack.addAll(this.stackedRoots);
		if (extension.getNodeType() instanceof SkipNodeType) {
			ParseTreeBranch newBranch = new ParseTreeBranch(this.input, nb, stack, this.rule, this.nextItemIndex);
			return newBranch;			
		} else {
			ParseTreeBranch newBranch = new ParseTreeBranch(this.input, nb, stack, this.rule, this.nextItemIndex+1);
			return newBranch;
		}
	}
	
	@Override
	public TangibleItem getNextExpectedItem() {
		RuleItem item = this.rule.getRhs();
		if (item instanceof Concatination) {
			Concatination c = (Concatination)item;
			return c.getItem().get(this.nextItemIndex);
		} else if (item instanceof Multi) {
			Multi m = (Multi)item;
			return m.getItem();
		} else if (item instanceof SeparatedList) {
			SeparatedList sl = (SeparatedList)item;
			if ( (this.nextItemIndex % 2) == 1 ) {
				return sl.getSeparator();
			} else {
				return sl.getConcatination();
			}
		} else {
			throw new RuntimeException("Should never happen");
		}
	}

	boolean calculateIsComplete() {
		RuleItem item = this.rule.getRhs();
		if (item instanceof Concatination) {
			Concatination c = (Concatination)item;
			return c.getItem().size() <= this.nextItemIndex;
		} else if (item instanceof Choice) {
			return true;
		} else if (item instanceof Multi) {
			Multi m = (Multi)item;
			int size = this.nextItemIndex;
			return m.getMin() <= size && (size <= m.getMax() || -1 == m.getMax());
		} else if (item instanceof SeparatedList) {
			SeparatedList sl = (SeparatedList)item;
			int size = this.nextItemIndex;
			return (size % 2) == 1;
		} else {
			throw new RuntimeException("Should never happen");
		}
	}
	boolean calculateCanGrow() {
		RuleItem item = this.rule.getRhs();
		boolean reachedEnd = this.getRoot().getMatchedTextLength() >= this.input.getLength();
		if (reachedEnd) return false;
		if (!this.stackedRoots.isEmpty()) return true;
		if (item instanceof Concatination) {
			Concatination c = (Concatination)item;
			return this.nextItemIndex < c.getItem().size();
		} else if (item instanceof Choice) {
			return false;
		} else if (item instanceof Multi) {
			Multi m = (Multi)item;
			int size = this.nextItemIndex;
			return size < m.getMax();
		} else if (item instanceof SeparatedList) {
			SeparatedList sl = (SeparatedList)item;
			int size = this.nextItemIndex;
			return true;
		} else {
			throw new RuntimeException("Should never happen");
		}
	}
	
	public ParseTreeBranch deepClone() {
		Stack<AbstractParseTree> stack = new Stack<>();
		stack.addAll(this.stackedRoots);
		ParseTreeBranch clone = new ParseTreeBranch(this.input, this.getRoot(), stack, this.rule, this.nextItemIndex);
		return clone;
	}
	
	//--- Object ---
	@Override
	public String toString() {
		ToStringVisitor v = new ToStringVisitor();
		return this.accept(v, "");
	}
	
	@Override
	public int hashCode() {
		return this.getRoot().hashCode();
	}
	
	@Override
	public boolean equals(Object arg) {
		if (arg instanceof ParseTreeBranch) {
			ParseTreeBranch other = (ParseTreeBranch)arg;
			return this.getRoot().equals(other.getRoot());
		} else {
			return false;
		}
	}
}
