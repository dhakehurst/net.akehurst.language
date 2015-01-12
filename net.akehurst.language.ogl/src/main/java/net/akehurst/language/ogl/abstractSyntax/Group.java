package net.akehurst.language.ogl.abstractSyntax;


public class Group extends Item {

	public Group(Choice choice) {
		this.choice = choice;
	}
	
	Choice choice;
	public Choice getChoice() {
		return this.choice;
	}
	
	//--- Object ---
	@Override
	public String toString() {
		return "(" + this.getChoice() + ")";
	}
	
	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}
	
	@Override
	public boolean equals(Object arg) {
		if (arg instanceof Group) {
			Group other = (Group)arg;
			return this.toString().equals(other.toString());
		} else {
			return false;
		}
	}
}
