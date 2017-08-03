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

import java.util.Objects;

import net.akehurst.language.core.parser.INode;
import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.core.parser.IParseTreeVisitor;
import net.akehurst.language.grammar.parser.ParseTreeToString;
import net.akehurst.language.grammar.parser.ToStringVisitor;

public class ParseTree implements IParseTree {

	public ParseTree(final INode root) {
		this.root = root;
	}

	private final INode root;

	@Override
	public INode getRoot() {
		return this.root;
	}

	@Override
	public String asString() {
		final ParseTreeToString visitor = new ParseTreeToString();
		final String s = this.accept(visitor, "");
		return s;
	}

	// --- IParseTreeVisitable ---
	@Override
	public <T, A, E extends Throwable> T accept(final IParseTreeVisitor<T, A, E> visitor, final A arg) throws E {
		return visitor.visit(this, arg);
	}

	// --- Object ---
	static ToStringVisitor v = new ToStringVisitor();
	String toString_cache;

	@Override
	public String toString() {
		if (null == this.toString_cache) {
			this.toString_cache = this.accept(Branch.v, "");
		}
		return this.toString_cache;
	}

	@Override
	public int hashCode() {
		return this.getRoot().hashCode();
	}

	@Override
	public boolean equals(final Object arg) {
		if (arg instanceof IParseTree) {
			final IParseTree other = (IParseTree) arg;
			return Objects.equals(this.getRoot(), other.getRoot());
		} else {
			return false;
		}
	}
}
