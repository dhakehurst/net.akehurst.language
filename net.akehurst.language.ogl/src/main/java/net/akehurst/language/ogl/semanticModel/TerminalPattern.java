package net.akehurst.language.ogl.semanticModel;

import java.util.regex.Pattern;

import net.akehurst.language.core.parser.INodeType;

public class TerminalPattern extends Terminal {
	
	public TerminalPattern(String value) {
		super(value);
		this.pattern = Pattern.compile(value, Pattern.MULTILINE);
	}
	
	Pattern pattern;
	
	public Pattern getPattern() {
		return this.pattern;
	}
	
	public INodeType getNodeType() {
		return new LeafNodeType(this);
	}
	
	@Override
	public <T, E extends Throwable> T accept(Visitor<T,E> visitor, Object... arg) throws E {
		return visitor.visit(this, arg);
	}
	
	//--- Object ---
	@Override
	public String toString() {
		return "\""+this.getValue()+"\"";
	}
	
	@Override
	public int hashCode() {
		return this.getValue().hashCode();
	}
	
	@Override
	public boolean equals(Object arg) {
		if (arg instanceof TerminalPattern) {
			TerminalPattern other = (TerminalPattern)arg;
			return this.getValue().equals(other.getValue());
		} else {
			return false;
		}
	}
}
