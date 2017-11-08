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

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

import net.akehurst.language.core.parser.IBranch;
import net.akehurst.language.core.parser.ILeaf;
import net.akehurst.language.core.parser.IParseTreeVisitor;
import net.akehurst.language.grammar.parser.ToStringVisitor;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;

public class Leaf extends Node implements ILeaf {

	Leaf(final String text, final int start, final int nextInputPosition, final RuntimeRule terminalRule) {
		super(terminalRule);
		// this.input = input;
		this.text = text;
		this.start = start;
		this.nextInputPosition = nextInputPosition;
		this.terminalRule = terminalRule;
	}

	// IInput input;
	//
	// IInput getInput() {
	// return this.input;
	// }

	private final String text;
	private final int start;
	private final int nextInputPosition;
	private final RuntimeRule terminalRule;

	public RuntimeRule getTerminalRule() {
		return this.terminalRule;
	}

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
		return this.getTerminalRule().getName();
	}

	@Override
	public boolean isPattern() {
		return this.getTerminalRule().getPatternFlags() != Pattern.LITERAL;
	}

	@Override
	public int getStartPosition() {
		return this.start;
	}

	@Override
	public int getNextInputPosition() {
		return this.nextInputPosition;
	}
	// @Override
	// public int getEnd() {
	// return this.end;
	// }

	@Override
	public int getMatchedTextLength() {
		return this.nextInputPosition - this.start;
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
		return this.text;
		// return this.input.get(this.start, this.end).toString();
	}

	@Override
	public String getNonSkipMatchedText() {
		return this.getIsSkip() ? "" : this.getMatchedText();
	}

	@Override
	public List<IBranch> findBranches(final String name) {
		return Collections.emptyList();
	}

	// --- Object ---
	static ToStringVisitor v = new ToStringVisitor();

	@Override
	public String toString() {
		return this.accept(Leaf.v, "");
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.getRuntimeRuleNumber(), this.start, this.nextInputPosition);
	}

	@Override
	public boolean equals(final Object arg) {
		if (!(arg instanceof ILeaf)) {
			return false;
		}
		final ILeaf other = (ILeaf) arg;
		if (this.getStartPosition() != other.getStartPosition() || this.getNextInputPosition() != other.getNextInputPosition()) {
			return false;
		}
		return this.getRuntimeRuleNumber() == other.getRuntimeRuleNumber();
	}

}
