package net.akehurst.language.parser.forrest;

import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.ogl.semanticModel.TangibleItem;
import net.akehurst.language.parser.CannotExtendTreeException;
import net.akehurst.language.parser.forrest.EmptyLeaf;

public class ParseTreeEmptyBud extends AbstractParseTree {

	public ParseTreeEmptyBud(Input input) {
		super(input);
		this.root = new EmptyLeaf();
		this.canGrow = false;
		this.complete = true;
	}
	
	@Override
	public TangibleItem getNextExpectedItem() {
		return null;
	}

	@Override
	public ParseTreeEmptyBud deepClone() {
		return new ParseTreeEmptyBud(this.input);
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
		if (arg instanceof ParseTreeEmptyBud) {
			return true;
		} else {
			return false;
		}
	}
}
