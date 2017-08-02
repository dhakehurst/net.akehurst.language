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
	public boolean getIsEnd(final int pos) {
		return pos > this.text.length();
	}

	// @Override
	// public CharSequence get(final int start, final int end) {
	// return this.text.subSequence(start, end);
	// }

	// public int getLength() {
	// return this.text.length();
	// }

	// Map<IntPair, ParseTreeBud2> bud_cache;
	// public ParseTreeBud2 createBud(RuntimeRule terminalRule, int pos) {
	// int terminalTypeNumber = terminalRule.getRuleNumber();
	// IntPair key = new IntPair(terminalTypeNumber, pos);
	// ParseTreeBud2 bud = this.bud_cache.get(key);
	// if (null==bud) {
	// Leaf l = this.fetchOrCreateBud(terminalRule, pos);
	// if (NO_LEAF!=l) {
	// bud = new ParseTreeBud2(l);
	// this.bud_cache.put(key, bud);
	// return bud;
	// }
	// }
	// return bud;
	// }

	final Leaf NO_LEAF;

	class IntPair {
		public IntPair(final int nodeType, final int position) {
			this.nodeType = nodeType;
			this.position = position;
		}

		int nodeType;
		int position;

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
				final Leaf le = new EmptyLeaf(pos, terminalRule);
				this.leaf_cache.put(key, le);
				return le;
			} else {
				// tried to get it again, fail
				return null;
			}
		}

		if (null == l) {

			final Matcher m = Pattern.compile(terminalRule.getTerminalPatternText(), terminalRule.getPatternFlags()).matcher(this.text);
			m.region(pos, this.text.length());
			if (m.lookingAt()) {
				final String matchedText = m.group();
				final int start = m.start();
				final int end = m.end();
				final Leaf leaf = this.ffactory.createLeaf(matchedText, start, end, terminalRule);
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
