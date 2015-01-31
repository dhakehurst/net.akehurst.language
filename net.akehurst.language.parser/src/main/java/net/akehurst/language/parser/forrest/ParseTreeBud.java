package net.akehurst.language.parser.forrest;

import java.util.Stack;

import net.akehurst.language.core.parser.ILeaf;
import net.akehurst.language.core.parser.INode;
import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.ogl.semanticModel.TangibleItem;
import net.akehurst.language.parser.CannotExtendTreeException;
import net.akehurst.language.parser.ToStringVisitor;

public class ParseTreeBud extends AbstractParseTree {

	ParseTreeBud(Input input, ILeaf root, Stack<AbstractParseTree> stack) {
		super(input, root, stack);
	}

	@Override
	boolean getCanGrow() {
		if (!this.stackedRoots.isEmpty()) {
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
	public ILeaf getRoot() {
		return (ILeaf) super.getRoot();
	}

	@Override
	public TangibleItem getNextExpectedItem() {
		throw new RuntimeException("Should never happen");
	}

	public ParseTreeBranch extendWith(INode extension) throws CannotExtendTreeException {
		throw new CannotExtendTreeException();
	}

	public ParseTreeBud deepClone() {
		Stack<AbstractParseTree> stack = new Stack<>();
		stack.addAll(this.stackedRoots);
		ParseTreeBud clone = new ParseTreeBud(this.input, this.getRoot(), stack);
		return clone;
	}

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
			return this.getRoot().equals(other.getRoot());
		} else {
			return false;
		}
	}
}
