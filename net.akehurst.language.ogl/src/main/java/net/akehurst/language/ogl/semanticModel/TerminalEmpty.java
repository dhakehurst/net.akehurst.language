package net.akehurst.language.ogl.semanticModel;

import net.akehurst.language.core.parser.INodeType;

// DataType
public class TerminalEmpty extends TerminalLiteral {
	
	public TerminalEmpty() {
		super("");
		this.nodeType = new LeafNodeType();
	}
	
	LeafNodeType nodeType;
	public INodeType getNodeType() {
		return this.nodeType;
	}
	
	//--- Object ---
	@Override
	public String toString() {
		return "<empty>";
	}
	
	@Override
	public int hashCode() {
		return this.getValue().hashCode();
	}
	
	@Override
	public boolean equals(Object arg) {
		if (arg instanceof TerminalEmpty) {
			TerminalEmpty other = (TerminalEmpty)arg;
			return this.getValue().equals(other.getValue());
		} else {
			return false;
		}
	}

}
