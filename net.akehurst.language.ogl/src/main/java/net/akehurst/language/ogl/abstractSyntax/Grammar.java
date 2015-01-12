package net.akehurst.language.ogl.abstractSyntax;

import java.util.Arrays;
import java.util.List;

public class Grammar {

	public Grammar(Namespace namespace, String name, Grammar[] extends_, Rule... rule) {
		this.namespace = namespace;
		this.name = name;
		this.extends_ = Arrays.asList(extends_);
		this.rule = Arrays.asList(rule);
	}
	
	Namespace namespace;
	public Namespace getNamespace() {
		return this.namespace;
	}
	
	String name;
	public String getName() {
		return this.name;
	}
	
	List<Grammar> extends_;
	public List<Grammar> getExtends() {
		return this.extends_;
	}
	
	List<Rule> rule;
	public List<Rule> getRule() {
		return this.rule;
	}
	
	//--- Object ---
	@Override
	public String toString() {
		String r = this.getNamespace() + System.lineSeparator();
		r += "grammar "+this.getName() + "{" + System.lineSeparator();
		for(Rule i : this.getRule()) {
			r += i.toString() + System.lineSeparator();
		}
		r+="}";
		return r;
	}
	
	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}
	
	@Override
	public boolean equals(Object arg) {
		if (arg instanceof Grammar) {
			Grammar other = (Grammar)arg;
			return this.toString().equals(other.toString());
		} else {
			return false;
		}
	}
}
