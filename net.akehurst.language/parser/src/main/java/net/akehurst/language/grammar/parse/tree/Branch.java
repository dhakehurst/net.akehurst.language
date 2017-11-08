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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import net.akehurst.language.core.parser.IBranch;
import net.akehurst.language.core.parser.INode;
import net.akehurst.language.core.parser.IParseTreeVisitor;
import net.akehurst.language.grammar.parser.ToStringVisitor;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;

public class Branch extends Node implements IBranch {

	public Branch(final RuntimeRule runtimeRule, final INode[] children) {
		super(runtimeRule);
		this.children = children;
		this.start = this.children.length == 0 ? -1 : this.children[0].getStartPosition();
		this.length = 0;
		// this.isEmpty = true;
		// this.firstLeaf = this.children.length==0 ? null : children[0].getFirstLeaf();
		for (final INode n : this.children) {
			// this.isEmpty &= n.getIsEmpty();
			this.length += n.getMatchedTextLength();
		}
		this.hashCode_cache = Objects.hash(this.runtimeRule.getRuleNumber(), this.start, this.length);
	}

	public INode[] children;
	int length;
	int start;

	// boolean isEmpty;
	// @Override
	// public boolean getIsEmpty() {
	// return isEmpty;
	// }

	@Override
	public String getName() {
		return this.runtimeRule.getName();
	}

	@Override
	public int getStartPosition() {
		return this.start;
	}

	@Override
	public int getNextInputPosition() {
		// TODO Auto-generated method stub
		return this.start + this.length;
	}
	// @Override
	// public int getEnd() {
	// return this.start + this.length;
	// }

	@Override
	public int getMatchedTextLength() {
		return this.length;
	}

	// ILeaf firstLeaf;
	// @Override
	// public ILeaf getFirstLeaf() {
	// return firstLeaf;
	// }

	@Override
	public String getMatchedText() {
		String str = "";
		for (final INode n : this.getChildren()) {
			str += n.getMatchedText();
		}
		return str;
	}

	@Override
	public <T, A, E extends Throwable> T accept(final IParseTreeVisitor<T, A, E> visitor, final A arg) throws E {
		return visitor.visit(this, arg);
	}

	@Override
	public boolean getIsEmptyLeaf() {
		return false;
	}

	@Override
	public boolean getIsEmpty() {
		if (this.getNonSkipChildren().isEmpty()) {
			return true;
		} else {
			if (this.getNonSkipChildren().get(0) instanceof EmptyLeaf) {
				return true;
			} else {
				return false;
			}
		}
	}

	@Override
	public List<INode> getChildren() {
		return Arrays.asList(this.children);
	}

	List<INode> nonSkipChildren_cache;

	@Override
	public List<INode> getNonSkipChildren() {
		if (null == this.nonSkipChildren_cache) {
			this.nonSkipChildren_cache = new ArrayList<>();
			for (final INode n : this.getChildren()) {
				if (n.getIsSkip()) {

				} else {
					this.nonSkipChildren_cache.add(n);
				}
			}
		}
		return this.nonSkipChildren_cache;
	}

	@Override
	public INode getChild(final int index) {
		final List<INode> children = this.getChildren();

		// get first non skip child
		int child = 0;
		INode n = children.get(child);
		while (n.getIsSkip() && child < children.size() - 1) {
			++child;
			n = children.get(child);
		}
		if (child >= children.size()) {
			return null;
		}
		int count = 0;

		while (count < index && child < children.size() - 1) {
			++child;
			n = children.get(child);
			while (n.getIsSkip()) {
				++child;
				n = children.get(child);
			}
			++count;
		}

		if (child < children.size()) {
			return n;
		} else {
			return null;
		}
	}

	@Override
	public IBranch getBranchChild(final int i) {
		final INode n = this.getChild(i);
		return (IBranch) n;
	}

	@Override
	public List<IBranch> getBranchNonSkipChildren() {
		final List<IBranch> res = this.getNonSkipChildren().stream().filter(IBranch.class::isInstance).map(IBranch.class::cast).collect(Collectors.toList());
		return res;
	}

	@Override
	public String getNonSkipMatchedText() {
		String str = "";
		for (final INode n : this.getNonSkipChildren()) {
			str += n.getNonSkipMatchedText();
		}
		return str;
	}

	@Override
	public List<IBranch> findBranches(final String name) {
		final List<IBranch> result = new ArrayList<>();
		if (Objects.equals(this.getName(), name)) {
			result.add(this);
		} else {
			for (final INode child : this.getChildren()) {
				result.addAll(child.findBranches(name));
			}
		}
		return result;
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

	int hashCode_cache;

	@Override
	public int hashCode() {
		return this.hashCode_cache;
	}

	@Override
	public boolean equals(final Object arg) {
		if (!(arg instanceof IBranch)) {
			return false;
		}
		final IBranch other = (IBranch) arg;
		if (this.getRuntimeRuleNumber() != other.getRuntimeRuleNumber()) {
			return false;
		}
		if (this.getStartPosition() != other.getStartPosition() || this.getMatchedTextLength() != other.getMatchedTextLength()) {
			return false;
		}
		return this.getChildren().equals(other.getChildren());
	}

}
