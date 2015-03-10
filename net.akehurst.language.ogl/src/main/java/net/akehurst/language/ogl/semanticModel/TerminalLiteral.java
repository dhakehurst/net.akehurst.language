package net.akehurst.language.ogl.semanticModel;

import java.util.regex.Pattern;

import net.akehurst.language.core.parser.INodeType;

// DataType
public class TerminalLiteral extends Terminal {
	
	public TerminalLiteral(String value) {
		super(value);
		this.pattern = Pattern.compile(value, Pattern.LITERAL);
		this.nodeType = new LeafNodeType(this);
	}
	
	Pattern pattern;
	
	public Pattern getPattern() {
		return this.pattern;
	}
	
	LeafNodeType nodeType;
	public INodeType getNodeType() {
		return this.nodeType;
	}
	
	@Override
	public <T, E extends Throwable> T accept(Visitor<T,E> visitor, Object... arg) throws E {
		return visitor.visit(this, arg);
	}
	
	//--- Object ---
	@Override
	public String toString() {
		return "'"+this.getValue()+"'";
	}
	
	@Override
	public int hashCode() {
		return this.getValue().hashCode();
	}
	
	@Override
	public boolean equals(Object arg) {
		if (arg instanceof TerminalLiteral) {
			TerminalLiteral other = (TerminalLiteral)arg;
			return this.getValue().equals(other.getValue());
		} else {
			return false;
		}
	}

}
