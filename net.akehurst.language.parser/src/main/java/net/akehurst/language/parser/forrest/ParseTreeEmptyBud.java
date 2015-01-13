package net.akehurst.language.parser.forrest;


public class ParseTreeEmptyBud extends ParseTreeBud {

	public ParseTreeEmptyBud(Input input) {
		super(input, new EmptyLeaf());
	}
	

	@Override
	public ParseTreeEmptyBud deepClone() {
		return new ParseTreeEmptyBud(this.input);
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
