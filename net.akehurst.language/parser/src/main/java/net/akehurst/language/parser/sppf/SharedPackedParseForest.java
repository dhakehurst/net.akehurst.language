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
package net.akehurst.language.parser.sppf;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import net.akehurst.language.core.sppf.ISPPFNode;
import net.akehurst.language.core.sppf.IParseTree;
import net.akehurst.language.core.sppf.IParseTreeVisitor;
import net.akehurst.language.core.sppf.ISPPFNode;
import net.akehurst.language.core.sppf.ISharedPackedParseForest;
import net.akehurst.language.grammar.parser.ParseTreeToInputText;
import net.akehurst.language.grammar.parser.ToStringVisitor;

public class SharedPackedParseForest implements ISharedPackedParseForest, IParseTree {

	private final Set<ISPPFNode> roots;

	public SharedPackedParseForest(final Set<ISPPFNode> roots) {
		this.roots = roots;
	}

	public SharedPackedParseForest(final ISPPFNode... roots) {
		this.roots = new HashSet<>();
		for (final ISPPFNode root : roots) {
			this.roots.add(root);
		}
	}

	// --- IParseTree ---
	@Override
	public ISPPFNode getRoot() {
		return (ISPPFNode) this.roots.iterator().next();
	}

	@Override
	public String asString() {
		final ParseTreeToInputText visitor = new ParseTreeToInputText();
		final String s = this.accept(visitor, "");
		return s;
	}

	// --- ISharedPackedParseForest ---
	@Override
	public Set<ISPPFNode> getRoots() {
		final Set<ISPPFNode> set = new HashSet<>();
		set.add(this.getRoot());
		return set;
	}

	@Override
	public boolean contains(final ISharedPackedParseForest other) {
		boolean result = true; // if other is empty then the result is true
		for (final ISPPFNode otherRoot : other.getRoots()) {
			boolean matchedOtherRoot = false;
			for (final ISPPFNode root : this.getRoots()) {
				matchedOtherRoot |= root.contains(otherRoot);
			}
			result &= matchedOtherRoot;
		}
		return result;
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
