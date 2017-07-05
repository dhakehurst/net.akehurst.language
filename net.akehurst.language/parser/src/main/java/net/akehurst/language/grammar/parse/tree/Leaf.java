/**
 * Copyright (C) 2015 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.akehurst.language.grammar.parse.tree;

import java.util.regex.Pattern;

import net.akehurst.language.core.parser.ILeaf;
import net.akehurst.language.core.parser.IParseTreeVisitor;
import net.akehurst.language.grammar.parser.ToStringVisitor;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;

public class Leaf extends Node implements ILeaf {

	Leaf(final IInput input, final int start, final int end, final RuntimeRule terminalRule) {
		super(terminalRule);
		this.input = input;
		this.start = start;
		this.end = end;
		this.terminalRule = terminalRule;
	}

	IInput input;

	IInput getInput() {
		return this.input;
	}

	int start;
	int end;
	RuntimeRule terminalRule;

	@Override
	public boolean getIsEmptyLeaf() {
		return false;
	}

	// @Override
	// public boolean getIsEmpty() {
	// return false;
	// }

	@Override
	public String getName() {
		return this.terminalRule.getName();
	}

	@Override
	public boolean isPattern() {
		return this.terminalRule.getPatternFlags() != Pattern.LITERAL;
	}

	@Override
	public int getStartPosition() {
		return this.start;
	}

	// @Override
	// public int getEnd() {
	// return this.end;
	// }

	@Override
	public int getMatchedTextLength() {
		return this.end - this.start;
	}

	// @Override
	// public ILeaf getFirstLeaf() {
	// return this;
	// }

	@Override
	public <T, A, E extends Throwable> T accept(final IParseTreeVisitor<T, A, E> visitor, final A arg) throws E {
		return visitor.visit(this, arg);
	}

	@Override
	public String getMatchedText() {
		return this.input.get(this.start, this.end).toString();
	}

	// public Leaf deepClone() {
	// Leaf clone = new Leaf(this.input, this.start, this.end, this.terminalRule);
	// return clone;
	// }

	// --- Object ---
	static ToStringVisitor v = new ToStringVisitor();

	@Override
	public String toString() {
		return this.accept(Leaf.v, "");
	}

	@Override
	public int hashCode() {
		return this.start ^ this.end;
	}

	@Override
	public boolean equals(final Object arg) {
		if (!(arg instanceof ILeaf)) {
			return false;
		}
		final ILeaf other = (ILeaf) arg;
		if (this.getStartPosition() != other.getStartPosition() || this.getMatchedTextLength() != other.getMatchedTextLength()) {
			return false;
		}
		return this.getRuntimeRuleNumber() == other.getRuntimeRuleNumber();
	}

}
