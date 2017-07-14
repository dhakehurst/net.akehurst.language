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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import net.akehurst.language.core.grammar.IRuleItem;

public class Group extends SimpleItem {

	public Group(final AbstractChoice choice) {
		this.choice = choice;
	}

	AbstractChoice choice;

	public AbstractChoice getChoice() {
		return this.choice;
	}

	@Override
	public String getName() {
		return "$group";
	}

	List<Integer> index;

	@Override
	public List<Integer> getIndex() {
		return this.index;
	}

	@Override
	public IRuleItem getSubItem(final int i) {
		if (0 == i) {
			return this.getChoice();
		} else {
			return null;
		}
	}

	@Override
	public void setOwningRule(final Rule value, final List<Integer> index) {
		this.owningRule = value;
		this.index = index;
		final ArrayList<Integer> nextIndex0 = new ArrayList<>(index);
		nextIndex0.add(0);
		this.getChoice().setOwningRule(value, nextIndex0);
	}

	@Override
	public <T, E extends Throwable> T accept(final Visitor<T, E> visitor, final Object... arg) throws E {
		return visitor.visit(this, arg);
	}

	// public Set<TangibleItem> findFirstTangibleItem() {
	// Set<TangibleItem> result = new HashSet<>();
	// result.add( this );
	// return result;
	// }
	//
	@Override
	public Set<Terminal> findAllTerminal() {
		final Set<Terminal> result = this.choice.findAllTerminal();
		return result;
	}

	@Override
	public Set<NonTerminal> findAllNonTerminal() {
		final Set<NonTerminal> result = this.choice.findAllNonTerminal();
		return result;
	}

	//
	// @Override
	// public boolean isMatchedBy(INode node) throws RuleNotFoundException {
	// return node.getNodeType().equals(this.getNodeType());
	// }

	// --- Object ---
	@Override
	public String toString() {
		return "(" + this.choice + ")";
	}

	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}

	@Override
	public boolean equals(final Object arg) {
		if (arg instanceof Group) {
			final Group other = (Group) arg;
			return this.toString().equals(other.toString());
		} else {
			return false;
		}
	}
}
