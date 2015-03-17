package net.akehurst.language.parser.forrest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import net.akehurst.language.core.parser.ILeaf;
import net.akehurst.language.core.parser.INodeType;
import net.akehurst.language.ogl.semanticModel.RuleNotFoundException;
import net.akehurst.language.ogl.semanticModel.Terminal;
import net.akehurst.language.parser.runtime.Factory;
import net.akehurst.language.parser.runtime.RuntimeRule;

public class Input {

	public Input(Factory factory, CharSequence text) {
		this.factory = factory;
		this.text = text;
		this.leaf_cache = new HashMap<>();
		this.bud_cache = new HashMap<>();
	}
	Factory factory;
	public CharSequence text;
	
	public CharSequence get(int start, int end) {
		return this.text.subSequence(start, end);
	}

	public int getLength() {
		return text.length();
	}

	public List<ParseTreeBud> createNewBuds(RuntimeRule[] possibleNextTerminals, int pos) throws RuleNotFoundException {
		List<ParseTreeBud> buds = new ArrayList<>();
//		buds.add(new ParseTreeEmptyBud(this,pos)); // always add empty bud as a new bud
		for (RuntimeRule terminalRule : possibleNextTerminals) {
			ParseTreeBud newBud = this.createBud(terminalRule, pos);
			if (null!=newBud) {
				buds.add(newBud);
			}
		}
		return buds;
	}
	
	Map<IntPair, ParseTreeBud> bud_cache;
	public ParseTreeBud createBud(RuntimeRule terminalRule, int pos) {
		int terminalTypeNumber = terminalRule.getRuleNumber();
		IntPair key = new IntPair(terminalTypeNumber, pos);
		ParseTreeBud bud = this.bud_cache.get(key);
		if (null==bud) {
			Leaf l = this.fetchOrCreateBud(terminalRule, pos);
			if (NO_LEAF!=l) {
				bud = new ParseTreeBud(this.factory, this, l, null );
				this.bud_cache.put(key, bud);
				return bud;
			}
		}
		return bud;
	}
	
	static final Leaf NO_LEAF = new Leaf(null, null, -1, -1, null);
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
			Matcher m = terminalRule.getTerminal().getPattern().matcher(this.text);
			m.region(pos, this.text.length());
			if (m.lookingAt()) {
				String matchedText = m.group();
				int start = m.start();
				int end = m.end();
				Leaf leaf = new Leaf(this.factory, this, start, end, terminalRule);
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
