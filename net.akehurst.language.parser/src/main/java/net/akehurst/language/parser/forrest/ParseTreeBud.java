package net.akehurst.language.parser.forrest;

import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.ogl.semanticModel.TangibleItem;
import net.akehurst.language.parser.CannotExtendTreeException;
import net.akehurst.language.parser.ToStringVisitor;

public class ParseTreeBud extends AbstractParseTree {

	ParseTreeBud(Leaf leaf, Input input) {
		super(input);
		this.root = leaf;
		super.canGrow = false;
		super.complete = true;
	}
	
	@Override
	public Leaf getRoot() {
		return (Leaf)super.getRoot();
	}
	
	@Override
	public TangibleItem getNextExpectedItem() {
		// TODO Auto-generated method stub
		return null;
	}

	public ParseTreeBranch extendWith(IParseTree extension) throws CannotExtendTreeException {
		throw new CannotExtendTreeException();
	}
	
	public ParseTreeBud deepClone() {
		ParseTreeBud clone = new ParseTreeBud(this.getRoot().deepClone(), this.input);
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
		if (arg instanceof ParseTreeBud) {
			ParseTreeBud other = (ParseTreeBud)arg;
			return this.getRoot().equals(other.getRoot());
		} else {
			return false;
		}
	}
}
