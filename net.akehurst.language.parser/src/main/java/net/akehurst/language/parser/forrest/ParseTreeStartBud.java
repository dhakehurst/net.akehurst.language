package net.akehurst.language.parser.forrest;

import java.util.Stack;

import net.akehurst.language.parser.ToStringVisitor;

public class ParseTreeStartBud extends ParseTreeBud {

	public ParseTreeStartBud(Factory f, Input input) {
		super(input, new EmptyLeaf(0), null);
	}
	
	@Override
	public boolean getCanGrow() {
		return true;
	}

//	@Override
//	public ParseTreeStartBud deepClone() {
//		return new ParseTreeStartBud(this.input);
//	}
	
//	@Override
//	public AbstractParseTree expand(AbstractParseTree newBranch) throws CannotExtendTreeException, RuleNotFoundException {
//		return newBranch;
//	}

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
		if (arg instanceof ParseTreeStartBud) {
			return true;
		} else {
			return false;
		}
	}
}
