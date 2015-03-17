package net.akehurst.language.parser.forrest;

import net.akehurst.language.core.parser.ILeaf;
import net.akehurst.language.core.parser.INodeType;
import net.akehurst.language.core.parser.IParseTreeVisitor;
import net.akehurst.language.core.parser.ParseTreeException;
import net.akehurst.language.ogl.semanticModel.RuleNotFoundException;
import net.akehurst.language.ogl.semanticModel.Terminal;
import net.akehurst.language.parser.ToStringVisitor;
import net.akehurst.language.parser.runtime.Factory;
import net.akehurst.language.parser.runtime.RuntimeRule;

public class Leaf extends Node implements ILeaf {

	public Leaf(Factory factory, Input input, int start, int end, RuntimeRule terminalRule) {
		super(factory, terminalRule);
		this.input = input;
		this.start = start;
		this.end = end;
		this.terminalRule = terminalRule;
	}
	
	Input input;
	public Input getInput() {
		return this.input;
	}
	
	int start;
	int end;
	RuntimeRule terminalRule;

	@Override
	public boolean getIsEmpty() {
		return false;
	}
	
	@Override
	public INodeType getNodeType() throws ParseTreeException {
		try {
			return this.terminalRule.getTerminal().getNodeType();
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
		Leaf clone = new Leaf(this.factory, this.input, this.start, this.end, this.terminalRule);
		return clone;
	}
	
	//--- Object ---
	static ToStringVisitor v = new ToStringVisitor();
	@Override
	public String toString() {
		return this.accept(v, "");
	}
	
	@Override
	public int hashCode() {
		return this.start ^ this.end;
	}
	
	@Override
	public boolean equals(Object arg) {
		if (!(arg instanceof Leaf) ) {
			return false;
		}
		Leaf other = (Leaf)arg;
		if (this.start!=other.start || this.end!=other.end) {
			return false;
		}
		return true;
	}
}
