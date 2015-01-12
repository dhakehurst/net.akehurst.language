package net.akehurst.language.parser;

import net.akehurst.language.core.parser.ILeaf;
import net.akehurst.language.core.parser.INodeType;
import net.akehurst.language.core.parser.IParseTreeVisitor;

public class Leaf extends AbstractNode implements ILeaf {

	public Leaf(INodeType nodeType, String matchedText) {
		super(nodeType);
		this.matchedText = matchedText;
		this.length = this.matchedText.length();
	}

	String matchedText;
	public String getMatchedText() {
		return this.matchedText;
	}
	
	@Override
	public <T, A, E extends Throwable> T accept(IParseTreeVisitor<T, A, E> visitor, A arg) throws E {
		return visitor.visit(this, arg);
	}
	
	public Leaf deepClone() {
		Leaf clone = new Leaf(this.nodeType, this.matchedText);
		clone.matchedText = this.matchedText;
		clone.length = this.length;
		return clone;
	}
	
	//--- Object ---
	@Override
	public String toString() {
		return "<" + this.getNodeType() + ">{"+this.getMatchedText()+"}";
	}
	
	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}
	
	@Override
	public boolean equals(Object arg) {
		if (arg instanceof Leaf) {
			Leaf other = (Leaf)arg;
			return this.toString().equals(other.toString());
		} else {
			return false;
		}
	}
}
