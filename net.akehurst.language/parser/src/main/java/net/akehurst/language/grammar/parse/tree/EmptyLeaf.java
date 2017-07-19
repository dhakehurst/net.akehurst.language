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

import net.akehurst.language.core.parser.ILeaf;
import net.akehurst.language.core.parser.IParseTreeVisitor;
import net.akehurst.language.grammar.parser.ToStringVisitor;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;

public class EmptyLeaf extends Leaf {

	public EmptyLeaf(final int pos, final RuntimeRule terminalRule) {
		super("", pos, pos, terminalRule);
	}

	@Override
	public boolean getIsEmptyLeaf() {
		return true;
	}

	@Override
	public String getName() {
		return this.getTerminalRule().getName();
	}

	@Override
	public int getMatchedTextLength() {
		return 0;
	}

	@Override
	public String getMatchedText() {
		return "";
	}

	@Override
	public <T, A, E extends Throwable> T accept(final IParseTreeVisitor<T, A, E> visitor, final A arg) throws E {
		return visitor.visit(this, arg);
	}

	// --- Object ---
	@Override
	public String toString() {
		final ToStringVisitor v = new ToStringVisitor();
		return this.accept(v, "");
	}

	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}

	@Override
	public boolean equals(final Object arg) {
		if (arg instanceof ILeaf) {
			final ILeaf other = (ILeaf) arg;
			return other.getIsEmptyLeaf();
		} else {
			return false;
		}
	}

	@Override
	public int getNumberOfLines() {
		return 0;
	}
}
