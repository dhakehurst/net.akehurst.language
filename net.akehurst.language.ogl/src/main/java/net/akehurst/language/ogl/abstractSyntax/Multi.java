package net.akehurst.language.ogl.abstractSyntax;

public class Multi extends Item {

	public Multi(int min, int max, Item item) {
		this.min = min;
		this.max = max;
		this.item = item;
	}
	
	int min;
	public int getMin() {
		return this.min;
	}
	
	int max;
	public int getMax() {
		return this.max;
	}
	
	Item item;
	public Item getItem() {
		return this.item;
	}
	
	//--- Object ---
	@Override
	public String toString() {
		return this.getItem() + (0==min?(0==max?"*":"?"):"+");
	}
	
	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}
	
	@Override
	public boolean equals(Object arg) {
		if (arg instanceof Multi) {
			Multi other = (Multi)arg;
			return this.toString().equals(other.toString());
		} else {
			return false;
		}
	}
}
