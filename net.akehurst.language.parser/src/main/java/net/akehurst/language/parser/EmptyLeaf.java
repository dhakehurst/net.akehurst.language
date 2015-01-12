package net.akehurst.language.parser;

import net.akehurst.language.core.parser.ILeaf;
import net.akehurst.language.core.parser.IParseTreeVisitor;
import net.akehurst.language.ogl.semanticModel.LeafNodeType;

public class EmptyLeaf extends AbstractNode implements ILeaf {

	public EmptyLeaf() {
		super(new LeafNodeType());
	}

	@Override
	public String getMatchedText() {
		return "";
	}

	@Override
	public <T, A, E extends Throwable> T accept(IParseTreeVisitor<T, A, E> visitor, A arg) throws E {
		return visitor.visit(this, arg);
	}
	
	@Override
	public EmptyLeaf deepClone() {
		return new EmptyLeaf();
	}

	//--- Object ---
	@Override
	public String toString() {
		ToStringVisitor v = new ToStringVisitor();
		return this.accept(v, "");
	}
	
	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}
	
	@Override
	public boolean equals(Object arg) {
		if (arg instanceof EmptyLeaf) {
			return true;
		} else {
			return false;
		}
	}
}
