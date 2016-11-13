package net.akehurst.language.grammar.parser.forrest;

import net.akehurst.language.grammar.parse.tree.Leaf;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;

public class ParseTreeBud2 extends AbstractParseTree2 {

	public ParseTreeBud2(Leaf root) {
		super(root, 0, -1);
	}

	@Override
	public boolean getIsComplete() {
		return true;
	}

	@Override
	public boolean getCanGrowWidth() {
		return false;
	}

	@Override
	public Leaf getRoot() {
		return (Leaf)super.getRoot();
	}
	
	@Override
	public boolean hasNextExpectedItem() {
		return false;
	}
	
	@Override
	public RuntimeRule getNextExpectedItem() {
		throw new RuntimeException("Internal Error: Should never happen");
	}

	@Override
	public String toString() {
		return this.identifier + this.getRuntimeRule().getTerminalPatternText();
	}
	
}
