package net.akehurst.language.ogl.abstractSyntax;


public class SeparatedList extends Item {

	public SeparatedList(int min, TerminalLiteral separator, Concatination concatination) {
		this.min = min;
		this.separator = separator;
		this.concatination = concatination;
	}
	
	int min;
	public int getMin() {
		return this.min;
	}
	
	TerminalLiteral separator;
	public TerminalLiteral getSeparator() {
		return this.separator;
	}
	
	Concatination concatination;
	public Concatination getConcatination() {
		return this.concatination;
	}
	
	//--- Object ---
	@Override
	public String toString() {
		return "( "+this.getConcatination()+" / "+this.getSeparator()+" )"+(this.min==0?"*":"+");
	}
	
	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}
	
	@Override
	public boolean equals(Object arg) {
		if (arg instanceof SeparatedList) {
			SeparatedList other = (SeparatedList)arg;
			return this.toString().equals(other.toString());
		} else {
			return false;
		}
	}
	
}
