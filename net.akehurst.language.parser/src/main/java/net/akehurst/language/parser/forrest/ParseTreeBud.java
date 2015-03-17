package net.akehurst.language.parser.forrest;

import net.akehurst.language.core.parser.ILeaf;
import net.akehurst.language.core.parser.INode;
import net.akehurst.language.ogl.semanticModel.TangibleItem;
import net.akehurst.language.parser.ToStringVisitor;
import net.akehurst.language.parser.runtime.Factory;
import net.akehurst.language.parser.runtime.RuntimeRule;

public class ParseTreeBud extends AbstractParseTree {

	ParseTreeBud(Factory factory, Input input, Leaf root, AbstractParseTree stackedTree) {
		super(factory, input, root, stackedTree);
	}

	@Override
	public boolean getCanGrow() {
		if (null!=this.stackedTree) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public boolean getIsComplete() {
		return true;
	}

	@Override
	public boolean getCanGraftBack() {
		return this.stackedTree!=null;
	}
	
	@Override
	public boolean getCanGrowWidth() {
		return false;
	}
	
	@Override
	public Leaf getRoot() {
		return (Leaf) super.getRoot();
	}

	@Override
	public RuntimeRule getNextExpectedItem() {
		throw new RuntimeException("Internal Error: Should never happen");
	}

	public ParseTreeBranch extendWith(INode extension)  {
		throw new RuntimeException("Should not happen, cannot extend a bud");
	}
	
//	public ParseTreeBud deepClone() {
//		Stack<AbstractParseTree> stack = new Stack<>();
//		stack.addAll(this.stackedRoots);
//		ParseTreeBud clone = new ParseTreeBud(this.input, this.getRoot(), stack);
//		return clone;
//	}

	// --- Object ---
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
		if (arg instanceof ParseTreeBud) {
			ParseTreeBud other = (ParseTreeBud) arg;
			return this.toString().equals(other.toString());
		} else {
			return false;
		}
	}
}
