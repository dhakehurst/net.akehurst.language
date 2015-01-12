package net.akehurst.language.ogl.abstractSyntax;

import java.util.Arrays;
import java.util.List;

public class Concatination {

	public Concatination(Item... item) {
		this.item = Arrays.asList(item);
	}
	
	List<Item> item;
	public List<Item> getItem() {
		return this.item;
	}
	
	//--- Object ---
	@Override
	public String toString() {
		String r = "";
		for(Item i : this.getItem()) {
			r += i.toString() + " ";
		}
		return r;
	}
	
	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}
	
	@Override
	public boolean equals(Object arg) {
		if (arg instanceof Concatination) {
			Concatination other = (Concatination)arg;
			return this.toString().equals(other.toString());
		} else {
			return false;
		}
	}
}
