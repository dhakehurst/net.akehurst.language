package net.akehurst.language.ogl.abstractSyntax;

import java.util.Arrays;
import java.util.List;


public class Choice {
	
	public Choice(Concatination... alternative) {
		this.alternative = Arrays.asList(alternative);
	}
	
	List<Concatination> alternative;
	public List<Concatination> getAlternative() {
		return this.alternative;
	}
	
	//--- Object ---
	@Override
	public String toString() {
		String r = "";
		for(Concatination a : this.getAlternative()) {
			r += a.toString() + " | ";
		}
		return r;
	}
	
	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}
	
	@Override
	public boolean equals(Object arg) {
		if (arg instanceof Choice) {
			Choice other = (Choice)arg;
			return this.toString().equals(other.toString());
		} else {
			return false;
		}
	}
}
