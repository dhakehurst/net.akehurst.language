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

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class ChoiceSimple extends AbstractChoice {

	public ChoiceSimple(final Concatenation... alternative) {
		super(alternative);
	}

	// @Override
	// public INodeType getNodeType() {
	// return new RuleNodeType(this.getOwningRule());
	// }

	@Override
	public <T, E extends Throwable> T accept(final Visitor<T, E> visitor, final Object... arg) throws E {
		return visitor.visit(this, arg);
	}

	// public Set<TangibleItem> findFirstTangibleItem() {
	// Set<TangibleItem> result = new HashSet<>();
	// for(Concatination c : this.getAlternative()) {
	// Set<TangibleItem> ft = c.findFirstTangibleItem();
	// result.addAll(ft);
	// } return result;
	// }
	//
	@Override
	public Set<Terminal> findAllTerminal() {
		final Set<Terminal> result = new HashSet<>();
		for (final Concatenation c : this.getAlternative()) {
			final Set<Terminal> ft = c.findAllTerminal();
			result.addAll(ft);
		}
		return result;
	}

	@Override
	public Set<NonTerminal> findAllNonTerminal() {
		final Set<NonTerminal> result = new HashSet<>();
		for (final Concatenation c : this.getAlternative()) {
			final Set<NonTerminal> ft = c.findAllNonTerminal();
			result.addAll(ft);
		}
		return result;
	}

	// --- Object ---
	@Override
	public String toString() {
		String r = "";
		for (final Concatenation a : this.getAlternative()) {
			r += a.toString() + " | ";
		}
		return r;
	}

	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}

	@Override
	public boolean equals(final Object arg) {
		if (arg instanceof ChoiceSimple) {
			final ChoiceSimple other = (ChoiceSimple) arg;
			return Objects.equals(this.getOwningRule(), other.getOwningRule()) && Objects.equals(this.getIndex(), other.getIndex());
		} else {
			return false;
		}
	}
}
