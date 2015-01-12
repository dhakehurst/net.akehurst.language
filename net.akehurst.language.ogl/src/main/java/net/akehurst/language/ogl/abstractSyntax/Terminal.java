package net.akehurst.language.ogl.abstractSyntax;

public abstract class Terminal extends Item {

	public Terminal(String value) {
		this.value = value;
	}
	
	String value;
	public String getValue() {
		return this.value;
	}
	
}
