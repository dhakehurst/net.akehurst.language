package net.akehurst.language.grammar.parse.tree;

import net.akehurst.language.core.parser.INode;
import net.akehurst.language.grammar.parser.forrest.Input;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;

public class Factory {
	
	public Branch createBranch(final RuntimeRule r, final INode[] children) {
		Branch b = new Branch(r, children);
		return b;
	}
	
	public Leaf createLeaf(Input input, int start, int end, RuntimeRule terminalRule) {
		Leaf l = new Leaf(input, start, end, terminalRule);
		return l;
	}
	
}
