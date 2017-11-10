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

import java.util.Objects;
import java.util.regex.Pattern;

import net.akehurst.language.core.sppf.ILeaf;
import net.akehurst.language.core.sppf.IParseTreeVisitor;
import net.akehurst.language.core.sppf.ISPPFNode;
import net.akehurst.language.core.sppf.ISPPFNodeIdentity;
import net.akehurst.language.core.sppf.SPPFNodeIdentity;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;

public class Leaf extends Node implements ILeaf {

	private final ISPPFNodeIdentity identity;
	private final String text;
	private final int nextInputPosition;
	private final RuntimeRule terminalRule;

	Leaf(final String text, final int startPosition, final int nextInputPosition, final RuntimeRule terminalRule) {
		super(terminalRule, startPosition);
		this.text = text;
		this.nextInputPosition = nextInputPosition;
		this.terminalRule = terminalRule;
		// TODO: fix factory passing 'null' as runtimeRule
		this.identity = null == this.terminalRule ? null
				: new SPPFNodeIdentity(this.terminalRule.getRuleNumber(), this.getStartPosition(), this.nextInputPosition - this.getStartPosition());
	}

	public int getNextInputPosition() {
		return this.nextInputPosition;
	}

	// --- ILeaf ---
	@Override
	public boolean isPattern() {
		return this.getRuntimeRule().getPatternFlags() != Pattern.LITERAL;
	}

	// --- ISPPFNode ---
	@Override
	public ISPPFNodeIdentity getIdentity() {
		return this.identity;
	}

	@Override
	public int getMatchedTextLength() {
		return this.text.length();
	}

	@Override
	public String getMatchedText() {
		return this.text;
	}

	@Override
	public String getNonSkipMatchedText() {
		if (this.isSkip()) {
			return "";
		} else {
			return this.getMatchedText();
		}
	}

	@Override
	public boolean isEmptyLeaf() {
		return false;
	}

	@Override
	public boolean isLeaf() {
		return true;
	}

	@Override
	public boolean isBranch() {
		return false;
	}

	@Override
	public boolean contains(final ISPPFNode other) {
		return this.equals(other);
	}

	//

	// @Override
	// public List<ISPPFBranch> findBranches(final String name) {
	// return Collections.emptyList();
	// }

	// --- IParseTreeVisitable ---
	@Override
	public <T, A, E extends Throwable> T accept(final IParseTreeVisitor<T, A, E> visitor, final A arg) throws E {
		return visitor.visit(this, arg);
	}

	// --- Object ---
	@Override
	public String toString() {
		final String s = this.getName() + " : \"" + this.getMatchedText().replace("\n", new String(Character.toChars(0x23CE))) + "\"";
		return s;
	}

	@Override
	public int hashCode() {
		return this.getIdentity().hashCode();
	}

	@Override
	public boolean equals(final Object obj) {
		if (obj instanceof ILeaf) {
			final ILeaf other = (ILeaf) obj;
			return Objects.equals(this.getIdentity(), other.getIdentity());
		} else {
			return false;
		}
	}

}
