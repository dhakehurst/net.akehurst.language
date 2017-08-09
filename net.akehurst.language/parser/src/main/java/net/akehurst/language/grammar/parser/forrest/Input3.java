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
package net.akehurst.language.grammar.parser.forrest;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.akehurst.language.core.parser.INodeType;
import net.akehurst.language.grammar.parse.tree.EmptyLeaf;
import net.akehurst.language.grammar.parse.tree.IInput;
import net.akehurst.language.grammar.parse.tree.Leaf;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleSetBuilder;

public class Input3 implements IInput {

	public Input3(final RuntimeRuleSetBuilder ffactory, final CharSequence text) {
		this.ffactory = ffactory;
		this.NO_LEAF = this.ffactory.createLeaf(null, -1, -1, null);
		this.text = text;
		this.leaf_cache = new HashMap<>();
		// this.bud_cache = new HashMap<>();
	}

	private final RuntimeRuleSetBuilder ffactory;
	private final CharSequence text;

	@Override
	public boolean getIsStart(final int pos) {
		// TODO what if we want t0 parse part?, e.g. sub grammar
		return 0 == pos;
	}

	@Override
	public boolean getIsEnd(final int pos) {
		return pos >= this.text.length();
	}

	final Leaf NO_LEAF;

	static class IntPair {
		public IntPair(final int nodeType, final int position) {
			this.nodeType = nodeType;
			this.position = position;
		}

		private final int nodeType;
		private final int position;

		@Override
		public int hashCode() {
			return this.nodeType ^ this.position;
		}

		@Override
		public boolean equals(final Object arg) {
			if (!(arg instanceof IntPair)) {
				return false;
			}
			final IntPair other = (IntPair) arg;
			return this.nodeType == other.nodeType && this.position == other.position;
		}

		@Override
		public String toString() {
			return "(".concat(Integer.toString(this.nodeType)).concat(",").concat(Integer.toString(this.position)).concat(")");
		}
	}

	Map<IntPair, Leaf> leaf_cache;

	@Override
	public Leaf fetchOrCreateBud(final RuntimeRule terminalRule, final int pos) {
		if (pos > this.text.length()) {
			// TODO: should we need to do this?
			return null;
		}
		final int terminalTypeNumber = terminalRule.getRuleNumber();
		final IntPair key = new IntPair(terminalTypeNumber, pos);
		final Leaf l = this.leaf_cache.get(key);
		if (terminalRule.getIsEmptyRule()) {
			if (null == l) {
				// first time we tried to get this empty token from this possion, OK.
				// FIXME: this is incorrect..need to be able to do this
				final Leaf le = new EmptyLeaf(pos, terminalRule);
				this.leaf_cache.put(key, le);
				return le;
			} else {
				// tried to get it again, fail
				return l;
			}
		}

		if (null == l) {

			final Matcher m = Pattern.compile(terminalRule.getTerminalPatternText(), terminalRule.getPatternFlags()).matcher(this.text);
			m.region(pos, this.text.length());
			if (m.lookingAt()) {
				final String matchedText = m.group();
				final int start = m.start();
				final int nextInputPosition = m.end();
				final Leaf leaf = this.ffactory.createLeaf(matchedText, start, nextInputPosition, terminalRule);
				this.leaf_cache.put(key, leaf);
				return leaf;
			} else {
				// TODO: why cache the null?
				this.leaf_cache.put(key, null);
				return null;
			}
		} else {
			return l;
		}
	}

	int nextI;
	Map<INodeType, Integer> map;

	private int getNodeType(final INodeType nodeType) {
		Integer i = this.map.get(nodeType);
		if (null == i) {
			i = this.nextI++;
			this.map.put(nodeType, i);
		}
		return i;
	}
}
