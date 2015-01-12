package net.akehurst.language.parser;

import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.ogl.semanticModel.RuleNotFoundException;
import net.akehurst.language.ogl.semanticModel.TangibleItem;

public class ParseTreeStartBud extends SubParseTree {

	public ParseTreeStartBud(int inputLength) {
		super(inputLength);
		this.root = new EmptyLeaf();
		this.canGrow = true;
		this.complete = false;
	}
	
	@Override
	public TangibleItem getNextExpectedItem() {
		return null;
	}

	@Override
	public ParseTreeStartBud deepClone() {
		return new ParseTreeStartBud(this.inputLength);
	}

	public ParseTreeBranch extendWith(IParseTree extension) throws CannotExtendTreeException {
		throw new CannotExtendTreeException();
	}
	
	@Override
	public SubParseTree expand(SubParseTree newBranch) throws CannotExtendTreeException, RuleNotFoundException {
		return newBranch;
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
		if (arg instanceof ParseTreeStartBud) {
			return true;
		} else {
			return false;
		}
	}
}
