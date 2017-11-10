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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import net.akehurst.language.core.sppf.IParseTreeVisitor;
import net.akehurst.language.core.sppf.ISPPFBranch;
import net.akehurst.language.core.sppf.ISPPFNode;
import net.akehurst.language.core.sppf.ISPPFNodeIdentity;
import net.akehurst.language.core.sppf.SPPFNodeIdentity;
import net.akehurst.language.grammar.parser.ToStringVisitor;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;

public class Branch extends Node implements ISPPFBranch {

	private final ISPPFNodeIdentity identity;
	private final ISPPFNode[] children;
	private int length;
	private List<ISPPFNode> nonSkipChildren_cache;

	public Branch(final RuntimeRule runtimeRule, final ISPPFNode[] children) {
		super(runtimeRule, children.length == 0 ? -1 : children[0].getStartPosition());
		this.children = children;
		this.length = 0;
		// this.isEmpty = true;
		// this.firstLeaf = this.children.length==0 ? null : children[0].getFirstLeaf();
		for (final ISPPFNode n : this.children) {
			// this.isEmpty &= n.getIsEmpty();
			this.length += n.getMatchedTextLength();
		}
		this.identity = new SPPFNodeIdentity(runtimeRule.getRuleNumber(), this.getStartPosition(), this.length);

	}

	// --- ISPPFBranch ---
	@Override
	public Set<List<ISPPFNode>> getChildrenAlternatives() {
		return null;
	}

	@Override
	public List<ISPPFNode> getChildren() {
		return Arrays.asList(this.children);
	}

	@Override
	public List<ISPPFNode> getNonSkipChildren() {
		if (null == this.nonSkipChildren_cache) {
			this.nonSkipChildren_cache = new ArrayList<>();
			for (final ISPPFNode n : this.getChildren()) {
				if (n.isSkip()) {

				} else {
					this.nonSkipChildren_cache.add(n);
				}
			}
		}
		return this.nonSkipChildren_cache;
	}

	@Override
	public ISPPFNode getChild(final int index) {
		final List<ISPPFNode> children = this.getChildren();

		// get first non skip child
		int child = 0;
		ISPPFNode n = children.get(child);
		while (n.isSkip() && child < children.size() - 1) {
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
			while (n.isSkip()) {
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
	public ISPPFBranch getBranchChild(final int i) {
		final ISPPFNode n = this.getChild(i);
		return (ISPPFBranch) n;
	}

	@Override
	public List<ISPPFBranch> getBranchNonSkipChildren() {
		final List<ISPPFBranch> res = this.getNonSkipChildren().stream().filter(ISPPFBranch.class::isInstance).map(ISPPFBranch.class::cast)
				.collect(Collectors.toList());
		return res;
	}

	// --- ISPPFNode ---
	@Override
	public ISPPFNodeIdentity getIdentity() {
		return this.identity;
	}

	@Override
	public int getMatchedTextLength() {
		return this.length;
	}

	@Override
	public String getMatchedText() {
		String str = "";
		for (final ISPPFNode n : this.getChildren()) {
			str += n.getMatchedText();
		}
		return str;
	}

	@Override
	public String getNonSkipMatchedText() {
		String str = "";
		for (final ISPPFNode n : this.getNonSkipChildren()) {
			str += n.getNonSkipMatchedText();
		}
		return str;
	}

	@Override
	public boolean isEmptyLeaf() {
		return false;
	}

	@Override
	public boolean isLeaf() {
		return false;
	}

	@Override
	public boolean isBranch() {
		return true;
	}

	@Override
	public boolean contains(final ISPPFNode other) {
		return this.equals(other);
	}

	// --- IParseTreeVisitable ---
	@Override
	public <T, A, E extends Throwable> T accept(final IParseTreeVisitor<T, A, E> visitor, final A arg) throws E {
		return visitor.visit(this, arg);
	}

	// @Override
	// public boolean getIsEmpty() {
	// if (this.getNonSkipChildren().isEmpty()) {
	// return true;
	// } else {
	// if (this.getNonSkipChildren().get(0) instanceof EmptyLeaf) {
	// return true;
	// } else {
	// return false;
	// }
	// }
	// }

	// @Override
	// public List<ISPPFBranch> findBranches(final String name) {
	// final List<ISPPFBranch> result = new ArrayList<>();
	// if (Objects.equals(this.getName(), name)) {
	// result.add(this);
	// } else {
	// for (final ISPPFNode child : this.getChildren()) {
	// result.addAll(child.findBranches(name));
	// }
	// }
	// return result;
	// }

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
		return this.getIdentity().hashCode();
	}

	@Override
	public boolean equals(final Object arg) {
		if (!(arg instanceof ISPPFBranch)) {
			return false;
		}
		final ISPPFBranch other = (ISPPFBranch) arg;
		if (!Objects.equals(this.getIdentity(), other.getIdentity())) {
			return false;
		}
		return this.getChildren().equals(other.getChildren());
	}

}
