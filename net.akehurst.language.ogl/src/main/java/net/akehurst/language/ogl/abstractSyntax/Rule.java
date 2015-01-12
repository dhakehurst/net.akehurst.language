package net.akehurst.language.ogl.abstractSyntax;

public class Rule {

	public Rule(String name, Choice rhs) {
		this.name = name;
		this.rhs = rhs;
	}
	
	String name;
	public String getName(){
		return name;
	}

	Choice rhs;
	public Choice getRhs(){
		return rhs;
	}
	
	//--- Object ---
	@Override
	public String toString() {
		return this.getName()+" = "+this.getRhs();
	}
	
	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}
	
	@Override
	public boolean equals(Object arg) {
		if (arg instanceof Rule) {
			Rule other = (Rule)arg;
			return this.toString().equals(other.toString());
		} else {
			return false;
		}
	}
}
