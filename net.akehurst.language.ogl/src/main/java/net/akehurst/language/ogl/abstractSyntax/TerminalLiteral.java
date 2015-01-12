package net.akehurst.language.ogl.abstractSyntax;

// DataType
public class TerminalLiteral extends Terminal {
	
	public TerminalLiteral(String value) {
		super(value);
	}
	

	//--- Object ---
	@Override
	public String toString() {
		return "'"+this.getValue()+"'";
	}
	
	@Override
	public int hashCode() {
		return this.getValue().hashCode();
	}
	
	@Override
	public boolean equals(Object arg) {
		if (arg instanceof TerminalLiteral) {
			TerminalLiteral other = (TerminalLiteral)arg;
			return this.getValue().equals(other.getValue());
		} else {
			return false;
		}
	}
}
