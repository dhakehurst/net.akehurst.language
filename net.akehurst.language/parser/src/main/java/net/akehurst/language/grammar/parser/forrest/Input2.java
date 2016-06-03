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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.akehurst.language.core.parser.INodeType;
import net.akehurst.language.core.parser.RuleNotFoundException;
import net.akehurst.language.grammar.parse.tree.EmptyLeaf;
import net.akehurst.language.grammar.parse.tree.IInput;
import net.akehurst.language.grammar.parse.tree.Leaf;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;

public class Input2 implements IInput {

	public Input2(ForrestFactory2 ffactory, CharSequence text) {
		this.ffactory = ffactory;
		this.NO_LEAF = this.ffactory.createLeaf(null, -1, -1, null);
		this.text = text;
		this.leaf_cache = new HashMap<>();
		this.bud_cache = new HashMap<>();
	}
	ForrestFactory2 ffactory;
	public CharSequence text;
	
	public CharSequence get(int start, int end) {
		return this.text.subSequence(start, end);
	}

	public int getLength() {
		return text.length();
	}

	public List<ParseTreeBud2> createNewBuds(RuntimeRule[] possibleNextTerminals, int pos) throws RuleNotFoundException {
		List<ParseTreeBud2> buds = new ArrayList<>();
//		buds.add(new ParseTreeEmptyBud(this,pos)); // always add empty bud as a new bud
		for (RuntimeRule terminalRule : possibleNextTerminals) {
			ParseTreeBud2 newBud = this.createBud(terminalRule, pos);
			if (null!=newBud) {
				buds.add(newBud);
			}
		}
		return buds;
	}
	
	Map<IntPair, ParseTreeBud2> bud_cache;
	public ParseTreeBud2 createBud(RuntimeRule terminalRule, int pos) {
		int terminalTypeNumber = terminalRule.getRuleNumber();
		IntPair key = new IntPair(terminalTypeNumber, pos);
		ParseTreeBud2 bud = this.bud_cache.get(key);
		if (null==bud) {
			Leaf l = this.fetchOrCreateBud(terminalRule, pos);
			if (NO_LEAF!=l) {
				bud = new ParseTreeBud2(l);
				this.bud_cache.put(key, bud);
				return bud;
			}
		}
		return bud;
	}
	
	final Leaf NO_LEAF;
	class IntPair {
		public IntPair(int nodeType, int position) {
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
		public boolean equals(Object arg) {
			if (!(arg instanceof IntPair)) {
				return false;
			}
			IntPair other = (IntPair)arg;
			return this.nodeType == other.nodeType && this.position==other.position;
		}
		@Override
		public String toString() {
			return "(".concat(Integer.toString(this.nodeType)).concat(",").concat(Integer.toString(this.position)).concat(")");
		}
	}
	Map<IntPair, Leaf> leaf_cache;
	
	public Leaf fetchOrCreateBud(RuntimeRule terminalRule, int pos) {
		int terminalTypeNumber = terminalRule.getRuleNumber();
		IntPair key = new IntPair(terminalTypeNumber, pos);
		Leaf l = this.leaf_cache.get(key);
		if (null==l) {
			if (terminalRule.getIsEmptyRule()) {
				return new EmptyLeaf(this, pos, terminalRule);
			}
			Matcher m = Pattern.compile(terminalRule.getTerminalPatternText(),terminalRule.getPatternFlags()).matcher(this.text);
			m.region(pos, this.text.length());
			if (m.lookingAt()) {
				String matchedText = m.group();
				int start = m.start();
				int end = m.end();
				Leaf leaf = this.ffactory.createLeaf(this, start, end, terminalRule);
				this.leaf_cache.put(key, leaf);
				return leaf;
			} else {
				this.leaf_cache.put(key, NO_LEAF);
				return NO_LEAF;
			}
		} else {
			return l;
		}
	}
	
	int nextI;
	Map<INodeType, Integer> map;
	private int getNodeType(INodeType nodeType) {
		Integer i = this.map.get(nodeType);
		if (null==i) {
			i = nextI++;
			this.map.put(nodeType, i);
		}
		return i;
	}
}
