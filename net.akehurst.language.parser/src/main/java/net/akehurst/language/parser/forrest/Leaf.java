package net.akehurst.language.parser.forrest;

import net.akehurst.language.core.parser.ILeaf;
import net.akehurst.language.core.parser.INodeType;
import net.akehurst.language.core.parser.IParseTreeVisitor;
import net.akehurst.language.core.parser.ParseTreeException;
import net.akehurst.language.ogl.semanticModel.Grammar;
import net.akehurst.language.ogl.semanticModel.RuleNotFoundException;
import net.akehurst.language.ogl.semanticModel.Terminal;
import net.akehurst.language.parser.ToStringVisitor;

public class Leaf implements ILeaf {

	public Leaf(Input input, int start, int end, Terminal terminal) {
		this.input = input;
		this.start = start;
		this.end = end;
		this.terminal = terminal;
	}
	
	Input input;
	public Input getInput() {
		return this.input;
	}
	
	int start;
	int end;
	Terminal terminal;

	@Override
	public boolean getIsEmpty() {
		return false;
	}
	
	@Override
	public INodeType getNodeType() throws ParseTreeException {
		try {
			return this.terminal.getNodeType();
		} catch (RuleNotFoundException e) {
			throw new ParseTreeException("Rule Not Found",e);
		}
	}

	@Override
	public String getName() {
		try {
			return this.getNodeType().getIdentity().asPrimitive();
		} catch (ParseTreeException e) {
			throw new RuntimeException("", e);
		}
	}

	@Override
	public int getStart() {
		return this.start;
	}
	
	@Override
	public int getEnd() {
		return this.end;
	}
	
	@Override
	public int getMatchedTextLength() {
		return end - start;
	}

	@Override
	public <T, A, E extends Throwable> T accept(IParseTreeVisitor<T, A, E> visitor, A arg) throws E {
		return visitor.visit(this, arg);
	}

	@Override
	public String getMatchedText() {
		return this.input.get(this.start, this.end).toString();
	}
	
	public Leaf deepClone() {
		Leaf clone = new Leaf(this.input, this.start, this.end, this.terminal);
		return clone;
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
		if (arg instanceof Leaf) {
			Leaf other = (Leaf)arg;
			return this.toString().equals(other.toString());
		} else {
			return false;
		}
	}
}
