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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.akehurst.language.core.sppf.ILeaf;
import net.akehurst.language.core.sppf.ISPPFBranch;
import net.akehurst.language.core.sppf.ISPPFNode;
import net.akehurst.language.core.sppf.ISPPFNodeIdentity;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;

abstract public class Node implements ISPPFNode {

	private final RuntimeRule runtimeRule;
	private final int startPosition;
	private ISPPFBranch parent;

	public Node(final RuntimeRule runtimeRule, final int startPosition) {
		this.runtimeRule = runtimeRule;
		this.startPosition = startPosition;
	}

	public RuntimeRule getRuntimeRule() {
		return this.runtimeRule;
	}

	// --- ISPPFNode ---

	@Override
	public abstract ISPPFNodeIdentity getIdentity();

	@Override
	public String getName() {
		return this.runtimeRule.getName();
	}

	@Override
	public int getRuntimeRuleNumber() {
		return this.runtimeRule.getRuleNumber();
	}

	@Override
	public int getStartPosition() {
		return this.startPosition;
	}

	@Override
	public abstract int getMatchedTextLength();

	@Override
	public abstract String getMatchedText();

	@Override
	public abstract String getNonSkipMatchedText();

	@Override
	public int getNumberOfLines() {
		final String str = this.getMatchedText();
		final Pattern p = Pattern.compile(System.lineSeparator());
		final Matcher m = p.matcher(str);
		int count = 0;
		while (m.find()) {
			count += 1;
		}
		return count;
	}

	@Override
	public abstract boolean isEmptyLeaf();

	@Override
	public abstract boolean isLeaf();

	@Override
	public abstract boolean isBranch();

	@Override
	public boolean isSkip() {
		return this.runtimeRule.getIsSkipRule();
	}

	@Override
	public ILeaf asLeaf() {
		if (this instanceof ILeaf) {
			return (ILeaf) this;
		} else {
			return null;
		}
	}

	@Override
	public ISPPFBranch asBranch() {
		if (this instanceof ISPPFBranch) {
			return (ISPPFBranch) this;
		} else {
			return null;
		}
	}

	@Override
	public ISPPFBranch getParent() {
		return this.parent;
	}

	@Override
	public void setParent(final ISPPFBranch value) {
		this.parent = value;
	}

	// --- Object ---
	@Override
	public abstract int hashCode();

	@Override
	public abstract boolean equals(Object obj);

	@Override
	public abstract String toString();

}
