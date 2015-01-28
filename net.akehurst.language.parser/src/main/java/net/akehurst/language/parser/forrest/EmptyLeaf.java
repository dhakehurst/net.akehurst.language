package net.akehurst.language.parser.forrest;

import net.akehurst.language.core.parser.ILeaf;
import net.akehurst.language.core.parser.INodeType;
import net.akehurst.language.core.parser.IParseTreeVisitor;
import net.akehurst.language.core.parser.ParseTreeException;
import net.akehurst.language.ogl.semanticModel.LeafNodeType;
import net.akehurst.language.parser.ToStringVisitor;

public class EmptyLeaf implements ILeaf {

	public EmptyLeaf(int start) {
		this.start = start;
	}
	
	int start;
	
	@Override
	public INodeType getNodeType() throws ParseTreeException {
		return new LeafNodeType();
	}

	@Override
	public String getName() {
		return "";
	}

	@Override
	public int getStart() {
		return this.start;
	}
	
	@Override
	public int getEnd() {
		return this.start;
	}
	
	@Override
	public int getMatchedTextLength() {
		return 0;
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
	public ILeaf deepClone() {
		return new EmptyLeaf(this.start);
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
