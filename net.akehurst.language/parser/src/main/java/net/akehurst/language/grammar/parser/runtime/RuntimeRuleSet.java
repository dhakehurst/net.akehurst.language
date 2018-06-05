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
package net.akehurst.language.grammar.parser.runtime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import net.akehurst.language.api.grammar.Grammar;
import net.akehurst.language.api.grammar.GrammarRuleNotFoundException;
import net.akehurst.language.api.grammar.Rule;
import net.akehurst.language.api.grammar.RuleItem;
import net.akehurst.language.grammar.parser.NonTerminalRuleReference;

public class RuntimeRuleSet {

	// TODO: compute this not guess !!!
	// private final int maxNextItemIndex = 200;

	private RuntimeRule[] emptyRulesFor;
	private final int totalRuleNumber;
	private RuntimeRule[] runtimeRules;
	private List<RuntimeRule> allSkipRules_cache;
	private Set<RuntimeRule> allSkipTerminals;
	private int[] isSkipTerminal;
	private ArrayList<String> nodeTypes;
	private Map<String, Integer> ruleNumbers;
	private RuntimeRule[][] possibleSubRule;
	private Set<RuntimeRule>[] possibleFirstRule;
	private RuntimeRule[][] possibleSuperRule;
	private SuperRuleInfo[][] possibleSuperRuleInfo;
	private RuntimeRule[][] possibleSubTerminal;
	private Set<RuntimeRule>[] possibleFirstTerminals;
	private RuntimeRule[] possibleFirstSkipTerminals;
	private Map<String, RuntimeRule> terminalMap;
	private int[][][] doHeight;
	private Set<RuntimeRule>[][] nextExpectedItem;
	private int[][] nextRuleItemIndex;
	private String toString_cache;

	/*
	 * necessary to know total number of rules up front, so it can be used in RuntimeRuleItem
	 */
	public RuntimeRuleSet(final int totalRuleNumber) { // , int emptyRuleNumber) {
		this.totalRuleNumber = totalRuleNumber;
		// this.emptyRuleNumber = emptyRuleNumber;
	}

	// int emptyRuleNumber;

	public RuntimeRule getEmptyRule(final RuntimeRule ruleThatIsEmpty) {
		return this.emptyRulesFor[ruleThatIsEmpty.getRuleNumber()];
	}

	public int getTotalRuleNumber() {
		return this.totalRuleNumber;
	}

	public RuntimeRule[] getAllRules() {
		return this.runtimeRules;
	}

	public void setRuntimeRules(final List<? extends RuntimeRule> value) {
		final int numberOfRules = value.size();
		this.possibleSubTerminal = new RuntimeRule[numberOfRules][];
		this.possibleFirstTerminals = new Set[numberOfRules];
		this.possibleSubRule = new RuntimeRule[numberOfRules][];
		this.possibleFirstRule = new Set[numberOfRules];
		this.possibleSuperRule = new RuntimeRule[numberOfRules][];
		this.possibleSuperRuleInfo = new SuperRuleInfo[numberOfRules][];
		this.runtimeRules = new RuntimeRule[numberOfRules];
		this.nodeTypes = new ArrayList<>(Arrays.asList(new String[numberOfRules]));
		this.ruleNumbers = new HashMap<>();
		this.terminalMap = new HashMap<>();
		this.emptyRulesFor = new RuntimeRule[numberOfRules];
		this.isSkipTerminal = new int[numberOfRules];
		this.nextExpectedItem = new Set[numberOfRules][2];
		this.doHeight = new int[numberOfRules][numberOfRules][1];

		for (final RuntimeRule rrule : value) {
			if (null == rrule) {
				throw new RuntimeException("RuntimeRuleSet must not containan  null rule!");
			}
			final int i = rrule.getRuleNumber();
			this.runtimeRules[i] = rrule;
			if (RuntimeRuleKind.NON_TERMINAL == rrule.getKind()) {
				this.nodeTypes.set(i, rrule.getNodeTypeName());
				this.ruleNumbers.put(rrule.getNodeTypeName(), i);
			} else {
				this.terminalMap.put(rrule.getTerminalPatternText(), rrule);
				if (rrule.getIsEmptyRule()) {
					this.emptyRulesFor[rrule.getRuleThatIsEmpty().getRuleNumber()] = rrule;
				}
			}
		}
	}

	public RuntimeRule getRuntimeRule(final String ruleName) {
		final int index = this.getRuleNumber(ruleName);
		return this.runtimeRules[index];
	}

	public RuntimeRule getRuntimeRule(final int index) {
		return this.runtimeRules[index];
	}

	public RuntimeRule getRuntimeRule(final Rule rule) {
		final int index = this.getRuleNumber(rule.getName());
		return this.getRuntimeRule(index);
	}

	// public Rule getRule(int ruleNumber) {
	// return this.getRuntimeRule(ruleNumber).getGrammarRule();
	// }

	public List<RuntimeRule> getAllSkipRules() {
		if (null == this.allSkipRules_cache) {
			this.allSkipRules_cache = new ArrayList<>();
			for (final RuntimeRule r : this.getAllRules()) {
				if (r.getIsSkipRule()) {
					this.allSkipRules_cache.add(r);
				}
			}
		}
		return this.allSkipRules_cache;
	}

	public Set<RuntimeRule> getAllSkipTerminals() {
		if (null == this.allSkipTerminals) {
			final Set<RuntimeRule> result = new HashSet<>();
			for (final RuntimeRule r : this.getAllSkipRules()) {
				for (final RuntimeRule rr : r.getRhs().getItems()) {
					if (RuntimeRuleKind.TERMINAL == rr.getKind()) {
						result.add(rr);
					}
				}
			}
			this.allSkipTerminals = result;
		}
		return this.allSkipTerminals;
	}

	public boolean isSkipTerminal(final RuntimeRule rr) {
		final int res = this.isSkipTerminal[rr.getRuleNumber()];
		if (res == 0) {
			final boolean b = this.getAllSkipTerminals().contains(rr);
			this.isSkipTerminal[rr.getRuleNumber()] = b ? 1 : -1;
			return b;
		}
		return res == 1;
	}

	public String getNodeType(final int nodeTypeNumber) {
		return this.nodeTypes.get(nodeTypeNumber);
	}

	public int getRuleNumber(final String rule) {
		return this.ruleNumbers.get(rule);
	}

	public RuntimeRule[] getPossibleSubRule(final RuntimeRule runtimeRule) {
		RuntimeRule[] result = this.possibleSubRule[runtimeRule.getRuleNumber()];
		if (null == result) {
			final Set<RuntimeRule> rr = runtimeRule.findSubRules();
			result = rr.toArray(new RuntimeRule[rr.size()]);
			this.possibleSubRule[runtimeRule.getRuleNumber()] = result;
		}
		return result;
	}

	public Set<RuntimeRule> getPossibleFirstSubRule(final RuntimeRule runtimeRule) {
		Set<RuntimeRule> result = this.possibleFirstRule[runtimeRule.getRuleNumber()];
		if (null == result) {
			final Set<RuntimeRule> rr = runtimeRule.findSubRulesAt(0);
			result = rr;// .toArray(new RuntimeRule[rr.size()]);
			this.possibleFirstRule[runtimeRule.getRuleNumber()] = result;
		}
		return result;
	}

	public RuntimeRule[] getPossibleSuperRule(final RuntimeRule runtimeRule) {
		RuntimeRule[] result = this.possibleSuperRule[runtimeRule.getRuleNumber()];
		if (null == result) {
			result = this.findAllSuperRule(runtimeRule);
			this.possibleSuperRule[runtimeRule.getRuleNumber()] = result;
		}
		return result;
	}

	/**
	 * For the given runtimeRule, find all other rules that could have it as a first child
	 *
	 */
	public SuperRuleInfo[] getPossibleSuperRuleInfo(final RuntimeRule runtimeRule) {
		SuperRuleInfo[] result = this.possibleSuperRuleInfo[runtimeRule.getRuleNumber()];
		if (null == result) {
			result = this.findAllSuperRuleInfo(runtimeRule);
			this.possibleSuperRuleInfo[runtimeRule.getRuleNumber()] = result;
		}
		return result;
	}

	public RuntimeRule[] getPossibleSubTerminal(final RuntimeRule runtimeRule) {
		RuntimeRule[] result = this.possibleSubTerminal[runtimeRule.getRuleNumber()];
		if (null == result) {
			final Set<RuntimeRule> rr = runtimeRule.findAllTerminal();
			for (final RuntimeRule r : this.getPossibleSubRule(runtimeRule)) {
				rr.addAll(r.findAllTerminal());
			}
			final Set<RuntimeRule> skipTerminal = this.getAllSkipTerminals();
			rr.addAll(skipTerminal);
			result = rr.toArray(new RuntimeRule[rr.size()]);
			this.possibleSubTerminal[runtimeRule.getRuleNumber()] = result;
		}
		return result;
	}

	public Set<RuntimeRule> getPossibleFirstTerminals(final RuntimeRule runtimeRule) {
		Set<RuntimeRule> result = this.possibleFirstTerminals[runtimeRule.getRuleNumber()];
		if (null == result) {
			final Set<RuntimeRule> rr = runtimeRule.findTerminalAt(0);
			for (final RuntimeRule r : this.getPossibleFirstSubRule(runtimeRule)) {
				rr.addAll(r.findTerminalAt(0));
			}
			// Set<RuntimeRule> skipTerminal = this.getAllSkipTerminals();
			// rr.addAll( skipTerminal );
			result = rr;
			this.possibleFirstTerminals[runtimeRule.getRuleNumber()] = result;
		}
		return result;
	}

	public RuntimeRule[] getPossibleFirstSkipTerminals() {
		RuntimeRule[] result = this.possibleFirstSkipTerminals;
		if (null == result) {
			final Set<RuntimeRule> skipTerminal = this.getAllSkipTerminals();
			result = skipTerminal.toArray(new RuntimeRule[skipTerminal.size()]);
			this.possibleFirstSkipTerminals = result;
		}
		return result;
	}

	public void build() {
		for (final RuntimeRule runtimeRule : this.getAllRules()) {
			this.getPossibleSubTerminal(runtimeRule);
			this.getPossibleFirstTerminals(runtimeRule);
			this.getPossibleSubRule(runtimeRule);
			this.getPossibleFirstSubRule(runtimeRule);
			this.getPossibleSuperRule(runtimeRule);
			this.getPossibleSuperRuleInfo(runtimeRule);
			this.isSkipTerminal(runtimeRule);
		}
	}

	public static final class SuperRuleInfo {
		public SuperRuleInfo(final RuntimeRule runtimeRule, final int index) {
			this.runtimeRule = runtimeRule;
			this.index = index;
			this.hashCode_cache = Objects.hash(runtimeRule, index);
		}

		RuntimeRule runtimeRule;
		int index;

		public RuntimeRule getRuntimeRule() {
			return this.runtimeRule;
		}

		public int getIndex() {
			return this.index;
		}

		int hashCode_cache;

		@Override
		public int hashCode() {
			return this.hashCode_cache;
		}

		@Override
		public boolean equals(final Object arg) {
			if (!(arg instanceof SuperRuleInfo)) {
				return false;
			}
			final SuperRuleInfo other = (SuperRuleInfo) arg;
			return this.index == other.index && this.runtimeRule == other.runtimeRule;
		}

		@Override
		public String toString() {
			return "(".concat(this.runtimeRule.toString()).concat(",").concat(Integer.toString(this.index)).concat(")");
		}
	}

	private SuperRuleInfo[] findAllSuperRuleInfo(final RuntimeRule runtimeRule) {
		final Set<SuperRuleInfo> result = new HashSet<>();
		for (final RuntimeRule r : this.runtimeRules) {
			if (RuntimeRuleKind.TERMINAL == r.getKind()) {
				// if (r.equals(runtimeRule)) {
				// int index = 0;
				// result.add(new SuperRuleInfo(r, index));
				// }
			} else {
				// final List<RuntimeRule> rhs = Arrays.asList(r.getRhs().getItems());
				// if (rhs.contains(runtimeRule)) {
				// final int index = rhs.indexOf(runtimeRule);
				// result.add(new SuperRuleInfo(r, index));
				// }
				if (r.couldHaveChild(runtimeRule, 0)) {
					// if (r.getRhs().getItems()[0].getRuleNumber() == runtimeRule.getRuleNumber()) {
					result.add(new SuperRuleInfo(r, 0));
				}
			}
		}
		if (runtimeRule.getIsEmptyRule()) {
			result.add(new SuperRuleInfo(runtimeRule.getRuleThatIsEmpty(), 0));
		}
		return result.toArray(new SuperRuleInfo[result.size()]);
	}

	RuntimeRule[] findAllSuperRule(final RuntimeRule runtimeRule) {
		final Set<RuntimeRule> result = new HashSet<>();
		for (final RuntimeRule r : this.runtimeRules) {
			if (RuntimeRuleKind.NON_TERMINAL == runtimeRule.getKind() && r.findAllNonTerminal().contains(runtimeRule)
					|| RuntimeRuleKind.TERMINAL == runtimeRule.getKind() && r.findAllTerminal().contains(runtimeRule)) {
				result.add(r);
			}
		}
		return result.toArray(new RuntimeRule[result.size()]);
	}

	public RuntimeRule getForTerminal(final String terminal) {
		final RuntimeRule rr = this.terminalMap.get(terminal);
		return rr;
	}

	public void putForTerminal(final String terminal, final RuntimeRule runtimeRule) {
		this.terminalMap.put(terminal, runtimeRule);
	}

	public RuleItem getOriginalItem(final RuntimeRule rr, final Grammar grammar) throws GrammarRuleNotFoundException {
		final String name = rr.getName();
		if (name.startsWith("$")) {
			// decode it (see Converter) and RuleItem.setOwningRule
			final String[] split = name.split("[.]");
			final String ruleName = split[0].substring(1);
			final RuleItem rhs = grammar.findAllRule(ruleName).getRhs();
			final String type = split[1];
			final int[] index = new int[split.length - 3];
			for (int i = 3; i < split.length; ++i) {
				final int ix = Integer.parseInt(split[i]);
				index[i - 3] = ix;
			}
			RuleItem item = rhs;
			for (final int i : index) {
				item = item.getSubItem(i);
			}

			return item;
		} else {
			// find grammar rule
			return new NonTerminalRuleReference(grammar, name);
		}
	}

	@Override
	public String toString() {
		if (null == this.toString_cache) {
			this.toString_cache = "";
			for (final RuntimeRule r : this.getAllRules()) {
				this.toString_cache += r + System.lineSeparator();
			}
		}
		return this.toString_cache;
	}

	/**
	 *
	 * return the set of SuperRuleInfo for which childRule can grow (at some point) into ancesstorRule at position ancesstorItemIndex
	 */
	public Set<SuperRuleInfo> growsInto(final RuntimeRule childRule, final RuntimeRule ancesstorRule, final int ancesstorItemIndex) {
		final Set<SuperRuleInfo> result = new HashSet<>(); // growsInto[childRule.getRuleNumber()][ancesstorRule.getRuleNumber()][cacheIndex];
		// create the growsInto cache!
		final SuperRuleInfo[] infos = this.getPossibleSuperRuleInfo(childRule);
		for (final SuperRuleInfo info : infos) {
			final RuntimeRule newParentRule = info.getRuntimeRule();
			final boolean canGrowInto = this.doHeight(newParentRule, ancesstorRule, ancesstorItemIndex);
			if (canGrowInto) {
				result.add(info);
			}
		}
		return result;
	}

	public boolean doHeight(final RuntimeRule parentRule, final RuntimeRule prevRule, final int nextItemIndex) {

		int value = 0;// this.doHeight[parentRule.getRuleNumber()][prevRule.getRuleNumber()][nextItemIndex];
		if (0 == value) {
			// // not set
			boolean res = false;
			if (-1 == nextItemIndex) {
				res = false;
			} else {

				final Set<RuntimeRule> nextExpectedForStacked = this.getNextExpectedItems(prevRule, nextItemIndex, 0);
				if (nextExpectedForStacked.contains(parentRule)) {
					res = true;
				} else {
					for (final RuntimeRule rr : nextExpectedForStacked) {
						if (rr.getKind() == RuntimeRuleKind.NON_TERMINAL) {
							// todo..can we reduce the possibles!
							final Set<RuntimeRule> possibles = this.getPossibleFirstSubRule(rr);
							if (possibles.contains(parentRule)) {
								res = true;
								break;
							}
						} else {
							final Set<RuntimeRule> possibles = this.getPossibleFirstTerminals(rr);
							if (possibles.contains(parentRule)) {
								res = true;
								break;
							}
						}
					}
				}
			}
			value = res ? 1 : -1;
			// this.doHeight[parentRule.getRuleNumber()][prevRule.getRuleNumber()][0] = value;
		}
		return value == 1; // -1 for false
		// TODO: make sure rule numbers start above 0
	}

	private int[] getNextRuleItemIndex(final RuntimeRule rr, final int nextItemIndex) {
		final int[] result = this.nextRuleItemIndex[rr.getRuleNumber()];
		if (null == result) {

			switch (rr.getRhs().getKind()) {
				case EMPTY:
					result = new int[] {};
				case CHOICE: {
					if (nextItemIndex == 0) {
						result = new int[rr.getRhs().getItems().length];
						for (int i = 0; i > result.length; ++i) {
							result[i] = i;
						}
					} else {
						result = new int[] {};
					}
				}
				case PRIORITY_CHOICE: {
					if (nextItemIndex == 0) {
						result = new int[rr.getRhs().getItems().length];
						for (int i = 0; i > result.length; ++i) {
							result[i] = i;
						}
					} else {
						result = new int[] {};
					}
					// throw new RuntimeException("Internal Error: item is priority choice");
				}
				case CONCATENATION: {
					if (nextItemIndex >= rr.getRhs().getItems().length) {
						throw new RuntimeException("Internal Error: No NextExpectedItem");
					} else {
						if (-1 == nextItemIndex) {
							result = new int[] {};
						} else {
							result = new int[] { nextItemIndex };
						}
					}
				}
				case MULTI: {
					if (0 == nextItemIndex && 0 == rr.getRhs().getMultiMin()) {
						result = Arrays.asList(rr.getRhsItem(0), rr.getRuntimeRuleSet().getEmptyRule(rr));
					} else {
						result = new int[] { 0 };
					}
				}
				case SEPARATED_LIST: {
					if (nextItemIndex % 2 == 1) {
						result = new int[] { 1 };
					} else {
						if (0 == nextItemIndex && 0 == rr.getRhs().getMultiMin()) {
							result = Arrays.asList(rr.getRhsItem(0), rr.getRuntimeRuleSet().getEmptyRule(rr));
						} else {
							result = new int[] { 0 };
						}
					}
				}
				default:
					throw new RuntimeException("Internal Error: rule kind not recognised");
			}
			this.nextRuleItemIndex[rr.getRuleNumber()] = result;
		}
		return result;
	}

	public int cacheIndex(final RuntimeRule rr, final int nextItemIndex, final int currentNumChildren) {
		int index = -1;
		switch (rr.getRhs().getKind()) {
			case EMPTY: {
				index = 0;
			}
			break;
			case CHOICE: {
				if (nextItemIndex == 0) {
					index = 0;
				} else {
					index = 1;
				}
			}
			break;
			case PRIORITY_CHOICE: {
				if (nextItemIndex == 0) {
					index = 0;
				} else {
					index = 1;
				}
				// throw new RuntimeException("Internal Error: item is priority choice");
			}
			break;
			case CONCATENATION: {
				if (currentNumChildren >= rr.getRhs().getItems().length) {
					throw new RuntimeException("Internal Error: No NextExpectedItem");
				} else {
					if (-1 == nextItemIndex) {
						index = 0;
					} else {
						index = 1;
					}
				}
			}
			break;
			case MULTI: {
				if (currentNumChildren == nextItemIndex && 0 == rr.getRhs().getMultiMin()) {
					index = 0;
				} else {
					index = 1;
				}
			}
			break;
			case SEPARATED_LIST: {
				if (currentNumChildren % 2 == 1) {
					index = 0;
				} else {
					if (currentNumChildren == nextItemIndex && 0 == rr.getRhs().getMultiMin()) {
						index = 1;
					} else {
						index = 2;
					}
				}
			}
			break;
			default:
				throw new RuntimeException("Internal Error: rule kind not recognised");
		}
		return index;
	}

	public Set<RuntimeRule> getNextExpectedItems(final RuntimeRule rr, final int nextItemIndex, final int currentNumChildren) {
		final int cacheIndex = this.cacheIndex(rr, nextItemIndex, currentNumChildren);
		Set<RuntimeRule> result = cacheIndex == 2 ? null : this.nextExpectedItem[rr.getRuleNumber()][cacheIndex];
		if (null == result) {
			switch (rr.getRhs().getKind()) {
				case EMPTY: {
					result = Collections.emptySet();
					this.nextExpectedItem[rr.getRuleNumber()][0] = result;
				}
				break;
				case CHOICE: {
					if (nextItemIndex == 0) {
						result = new HashSet<>(Arrays.asList(rr.getRhs().getItems()));
						this.nextExpectedItem[rr.getRuleNumber()][0] = result;
					} else {
						result = Collections.emptySet();
						this.nextExpectedItem[rr.getRuleNumber()][1] = result;
					}
					// throw new RuntimeException("Internal Error: item is choice");
				}
				break;
				case PRIORITY_CHOICE: {
					if (nextItemIndex == 0) {
						result = new HashSet<>(Arrays.asList(rr.getRhs().getItems()));
						this.nextExpectedItem[rr.getRuleNumber()][0] = result;
					} else {
						result = Collections.emptySet();
						this.nextExpectedItem[rr.getRuleNumber()][1] = result;
					}
					// throw new RuntimeException("Internal Error: item is priority choice");
				}
				break;
				case CONCATENATION: {
					if (currentNumChildren >= rr.getRhs().getItems().length) {
						throw new RuntimeException("Internal Error: No NextExpectedItem");
					} else {
						if (-1 == nextItemIndex) {
							result = Collections.emptySet();
							this.nextExpectedItem[rr.getRuleNumber()][0] = result;
						} else {
							result = new HashSet<>();
							result.add(rr.getRhsItem(nextItemIndex));
							// can't cache this, it depends on nextItemIndex directly
							// this.nextExpectedItem[rr.getRuleNumber()][1] = result;
						}
					}
				}
				break;
				case MULTI: {
					if (0 == currentNumChildren && 0 == rr.getRhs().getMultiMin()) {
						result = new HashSet<>();
						result.add(rr.getRhsItem(0));
						result.add(rr.getRuntimeRuleSet().getEmptyRule(rr));
						this.nextExpectedItem[rr.getRuleNumber()][0] = result;
					} else {
						result = new HashSet<>();
						result.add(rr.getRhsItem(0));
						this.nextExpectedItem[rr.getRuleNumber()][1] = result;
					}
				}
				break;
				case SEPARATED_LIST: {
					if (currentNumChildren % 2 == 1) {
						result = new HashSet<>();
						result.add(rr.getSeparator());
						this.nextExpectedItem[rr.getRuleNumber()][0] = result;
					} else {
						if (0 == currentNumChildren && 0 == rr.getRhs().getMultiMin()) {
							result = new HashSet<>();
							result.add(rr.getRhsItem(0));
							result.add(rr.getRuntimeRuleSet().getEmptyRule(rr));
							this.nextExpectedItem[rr.getRuleNumber()][1] = result;
						} else {
							// TODO: is it worth caching this result at the cost of index = 2!
							result = new HashSet<>();
							result.add(rr.getRhsItem(0));
						}
					}
				}
				break;
				default:
					throw new RuntimeException("Internal Error: rule kind not recognised");
			}
		}
		return result;
	}

}