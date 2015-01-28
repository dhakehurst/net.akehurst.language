package net.akehurst.language.parser.forrest;

import net.akehurst.language.parser.ToStringVisitor;


public class ParseTreeEmptyBud extends ParseTreeBud {

	public ParseTreeEmptyBud(Input input, int start) {
		super(input, new EmptyLeaf(start));
	}
	

	@Override
	public ParseTreeEmptyBud deepClone() {
		return new ParseTreeEmptyBud(this.input, this.getRoot().getStart());
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
		if (arg instanceof ParseTreeEmptyBud) {
			return true;
		} else {
			return false;
		}
	}
}
