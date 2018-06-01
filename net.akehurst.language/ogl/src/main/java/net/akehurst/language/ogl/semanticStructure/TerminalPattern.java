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
package net.akehurst.language.ogl.semanticStructure;

import java.util.regex.Pattern;

import net.akehurst.language.core.grammar.INodeType;

public class TerminalPattern extends Terminal {

	public TerminalPattern(final String value) {
		super(value);
		this.pattern = Pattern.compile(value, Pattern.MULTILINE);
	}

	Pattern pattern;

	@Override
	public Pattern getPattern() {
		return this.pattern;
	}

	@Override
	public boolean isPattern() {
		return true;
	}

	@Override
	public INodeType getNodeType() {
		return new LeafNodeType(this);
	}

	@Override
	public <T, E extends Throwable> T accept(final Visitor<T, E> visitor, final Object... arg) throws E {
		return visitor.visit(this, arg);
	}

	// --- Object ---
	@Override
	public String toString() {
		return "\"" + this.getValue() + "\"";
	}

	@Override
	public int hashCode() {
		return this.getValue().hashCode();
	}

	@Override
	public boolean equals(final Object arg) {
		if (arg instanceof TerminalPattern) {
			final TerminalPattern other = (TerminalPattern) arg;
			return this.getValue().equals(other.getValue());
		} else {
			return false;
		}
	}
}
