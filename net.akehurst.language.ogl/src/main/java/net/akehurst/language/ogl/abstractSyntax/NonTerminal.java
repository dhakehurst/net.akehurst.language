package net.akehurst.language.ogl.abstractSyntax;

public class NonTerminal extends Item {

	public NonTerminal(String ruleName) {
		this.ruleName = ruleName;
	}
	
	String ruleName;
	public String getRuleName() {
		return this.ruleName;
	}

	//--- Object ---
	@Override
	public String toString() {
		return this.getRuleName();
	}
	
	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}
	
	@Override
	public boolean equals(Object arg) {
		if (arg instanceof NonTerminal) {
			NonTerminal other = (NonTerminal)arg;
			return this.toString().equals(other.toString());
		} else {
			return false;
		}
	}
}
