package net.akehurst.language.grammar.parse.tree;

import net.akehurst.language.grammar.parser.runtime.RuntimeRule;

public interface IInput {

	Leaf fetchOrCreateBud(RuntimeRule rr, int nextInputPosition);

	boolean getIsStart(int pos);

	boolean getIsEnd(int pos);

	// CharSequence get(int start, int end);

}
