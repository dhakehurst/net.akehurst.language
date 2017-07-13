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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import net.akehurst.language.core.analyser.IRuleItem;

public class Concatenation extends RuleItem {

	public Concatenation(final ConcatenationItem... item) {
		if (item.length < 1) {
			throw new RuntimeException("A concatentation must have 1 or more items in it.");
		}
		this.item = Arrays.asList(item);
	}

	List<Integer> index;

	@Override
	public List<Integer> getIndex() {
		return this.index;
	}

	@Override
	public IRuleItem getSubItem(final int i) {
		if (i < this.getItem().size()) {
			return this.getItem().get(i);
		} else {
			return null;
		}
	}

	@Override
	public void setOwningRule(final Rule value, final List<Integer> index) {
		this.owningRule = value;
		this.index = index;
		int i = 0;
		for (final ConcatenationItem c : this.getItem()) {
			final ArrayList<Integer> nextIndex = new ArrayList<>(index);
			nextIndex.add(i++);
			c.setOwningRule(value, nextIndex);
		}
	}

	List<ConcatenationItem> item;

	public List<ConcatenationItem> getItem() {
		return this.item;
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
	// result.addAll( this.getItem().get(0).findFirstTangibleItem() );
	// return result;
	// }
	//
	@Override
	public Set<Terminal> findAllTerminal() {
		final Set<Terminal> result = new HashSet<>();
		for (final ConcatenationItem ti : this.getItem()) {
			result.addAll(ti.findAllTerminal());
		}
		return result;
	}

	@Override
	public Set<NonTerminal> findAllNonTerminal() {
		final Set<NonTerminal> result = new HashSet<>();
		for (final ConcatenationItem ti : this.getItem()) {
			result.addAll(ti.findAllNonTerminal());
		}
		return result;
	}

	// @Override
	// public boolean isMatchedBy(INode node) throws RuleNotFoundException {
	// if (node instanceof IBranch) {
	// IBranch branch = (IBranch)node;
	// boolean isMatched = branch.getChildren().size() == this.getItem().size();
	// if (isMatched) {
	// for(int i=0; i < branch.getChildren().size(); ++i) {
	// INode cn = branch.getChildren().get(i);
	// ConcatinationItem item = this.getItem().get(i);
	// if ( ! item.isMatchedBy(cn) ) {
	// return false;
	// }
	// }
	// }
	// return isMatched;
	// }
	// return false;
	// }

	// --- Object ---
	@Override
	public String toString() {
		String r = "";
		for (final RuleItem i : this.getItem()) {
			r += i.toString() + " ";
		}
		return r;
	}

	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}

	@Override
	public boolean equals(final Object arg) {
		if (arg instanceof Concatenation) {
			final Concatenation other = (Concatenation) arg;
			return Objects.equals(this.getOwningRule(), other.getOwningRule()) && Objects.equals(this.index, other.index);
		} else {
			return false;
		}
	}
}
