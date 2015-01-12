package net.akehurst.language.ogl.abstractSyntax;

public class TerminalPattern extends Terminal {
	
	public TerminalPattern(String value) {
		super(value);
	}
	
	//--- Object ---
	@Override
	public String toString() {
		return "\""+this.getValue()+"\"";
	}
	
	@Override
	public int hashCode() {
		return this.getValue().hashCode();
	}
	
	@Override
	public boolean equals(Object arg) {
		if (arg instanceof TerminalPattern) {
			TerminalPattern other = (TerminalPattern)arg;
			return this.getValue().equals(other.getValue());
		} else {
			return false;
		}
	}
}
