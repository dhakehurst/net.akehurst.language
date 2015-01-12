package net.akehurst.language.parser;

import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.ogl.semanticModel.TangibleItem;

public class ParseTreeBud extends SubParseTree {

	public ParseTreeBud(Leaf leaf, int inputLength) {
		super(inputLength);
		this.root = leaf;
		this.complete = true;
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
	
	public ParseTreeBud deepClone() {
		ParseTreeBud clone = new ParseTreeBud(this.getRoot(), this.inputLength);
		return clone;
	}
	
	public ParseTreeBranch extendWith(IParseTree extension) throws CannotExtendTreeException {
		throw new CannotExtendTreeException();
	}
	
	//--- Object ---
	@Override
	public String toString() {
		return this.getRoot().toString();
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
