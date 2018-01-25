package net.akehurst.language.parser.sppf;

import net.akehurst.language.grammar.parser.runtime.RuntimeRule;

public interface IInput {

	Leaf fetchOrCreateBud(RuntimeRule rr, int nextInputPosition);

	boolean getIsStart(int pos);

	boolean getIsEnd(int pos);

	CharSequence getText();

	// CharSequence get(int start, int end);

}