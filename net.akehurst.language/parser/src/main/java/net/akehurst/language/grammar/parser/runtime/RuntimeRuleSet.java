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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import net.akehurst.language.core.parser.INodeType;
import net.akehurst.language.ogl.semanticStructure.Rule;
import net.akehurst.language.ogl.semanticStructure.Terminal;

public class RuntimeRuleSet {

	/*
	 * necessary to know total number of rules up front, so it can be used in
	 * RuntimeRuleItem
	 */
	public RuntimeRuleSet(int totalRuleNumber) { // , int emptyRuleNumber) {
		this.totalRuleNumber = totalRuleNumber;
		// this.emptyRuleNumber = emptyRuleNumber;
	}

	// int emptyRuleNumber;
	RuntimeRule[] emptyRulesFor;

	public RuntimeRule getEmptyRule(RuntimeRule ruleThatIsEmpty) {
		return this.emptyRulesFor[ruleThatIsEmpty.getRuleNumber()];
	}

	int totalRuleNumber;

	public int getTotalRuleNumber() {
		return this.totalRuleNumber;
	}

	RuntimeRule[] runtimeRules;

	public RuntimeRule[] getAllRules() {
		return this.runtimeRules;
	}

	public void setRuntimeRules(List<? extends RuntimeRule> value) {
		int numberOfRules = value.size();
		this.possibleSubTerminal = new RuntimeRule[numberOfRules][];
		this.possibleFirstTerminals = new RuntimeRule[numberOfRules][];
		this.possibleSubRule = new RuntimeRule[numberOfRules][];
		this.possibleFirstRule = new RuntimeRule[numberOfRules][];
		this.possibleSuperRule = new RuntimeRule[numberOfRules][];
		this.possibleSuperRuleInfo = new SuperRuleInfo[numberOfRules][];
		this.runtimeRules = new RuntimeRule[numberOfRules];
		this.nodeTypes = new ArrayList<>(Arrays.asList(new String[numberOfRules]));
		this.ruleNumbers = new HashMap<>();
		this.terminalMap = new HashMap<>();
		this.emptyRulesFor = new RuntimeRule[numberOfRules];

		for (RuntimeRule rrule : value) {
			if (null == rrule) {
				throw new RuntimeException("RuntimeRuleSet must not containan  null rule!");
			}
			int i = rrule.getRuleNumber();
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

	public RuntimeRule getRuntimeRule(String ruleName) {
		int index = this.getRuleNumber(ruleName);
		return this.runtimeRules[index];
	}

	public RuntimeRule getRuntimeRule(int index) {
		return this.runtimeRules[index];
	}

	public RuntimeRule getRuntimeRule(Rule rule) {
		int index = this.getRuleNumber(rule.getName());
		return this.getRuntimeRule(index);
	}

	// public Rule getRule(int ruleNumber) {
	// return this.getRuntimeRule(ruleNumber).getGrammarRule();
	// }

	List<RuntimeRule> allSkipRules_cache;

	private List<RuntimeRule> getAllSkipRules() {
		if (null == this.allSkipRules_cache) {
			this.allSkipRules_cache = new ArrayList<>();
			for (RuntimeRule r : this.getAllRules()) {
				if (r.getIsSkipRule()) {
					this.allSkipRules_cache.add(r);
				}
			}
		}
		return this.allSkipRules_cache;
	}

	Set<RuntimeRule> allSkipTerminals;

	public Set<RuntimeRule> getAllSkipTerminals() {
		if (null == this.allSkipTerminals) {
			Set<RuntimeRule> result = new HashSet<>();
			for (RuntimeRule r : this.getAllSkipRules()) {
				for (RuntimeRule rr : r.getRhs().getItems()) {
					if (RuntimeRuleKind.TERMINAL == rr.getKind()) {
						result.add(rr);
					}
				}
			}
			this.allSkipTerminals = result;
		}
		return this.allSkipTerminals;
	}

	ArrayList<String> nodeTypes;

	public String getNodeType(int nodeTypeNumber) {
		return this.nodeTypes.get(nodeTypeNumber);
	}

	Map<String, Integer> ruleNumbers;

	public int getRuleNumber(String rule) {
		return this.ruleNumbers.get(rule);
	}

	RuntimeRule[][] possibleSubRule;

	public RuntimeRule[] getPossibleSubRule(RuntimeRule runtimeRule) {
		RuntimeRule[] result = this.possibleSubRule[runtimeRule.getRuleNumber()];
		if (null == result) {
			Set<RuntimeRule> rr = runtimeRule.findSubRules();
			result = rr.toArray(new RuntimeRule[rr.size()]);
			this.possibleSubRule[runtimeRule.getRuleNumber()] = result;
		}
		return result;
	}

	RuntimeRule[][] possibleFirstRule;

	public RuntimeRule[] getPossibleFirstSubRule(RuntimeRule runtimeRule) {
		RuntimeRule[] result = this.possibleFirstRule[runtimeRule.getRuleNumber()];
		if (null == result) {
			Set<RuntimeRule> rr = runtimeRule.findSubRulesAt(0);
			result = rr.toArray(new RuntimeRule[rr.size()]);
			this.possibleFirstRule[runtimeRule.getRuleNumber()] = result;
		}
		return result;
	}

	RuntimeRule[][] possibleSuperRule;

	public RuntimeRule[] getPossibleSuperRule(RuntimeRule runtimeRule) {
		RuntimeRule[] result = this.possibleSuperRule[runtimeRule.getRuleNumber()];
		if (null == result) {
			result = this.findAllSuperRule(runtimeRule);
			this.possibleSuperRule[runtimeRule.getRuleNumber()] = result;
		}
		return result;
	}

	SuperRuleInfo[][] possibleSuperRuleInfo;
	public SuperRuleInfo[] getPossibleSuperRuleInfo(RuntimeRule runtimeRule) {
		SuperRuleInfo[] result = this.possibleSuperRuleInfo[runtimeRule.getRuleNumber()];
		if (null == result) {
			result = this.findAllSuperRuleInfo(runtimeRule);
			this.possibleSuperRuleInfo[runtimeRule.getRuleNumber()] = result;
		}
		return result;
	}
	
	RuntimeRule[][] possibleSubTerminal;

	public RuntimeRule[] getPossibleSubTerminal(RuntimeRule runtimeRule) {
		RuntimeRule[] result = this.possibleSubTerminal[runtimeRule.getRuleNumber()];
		if (null == result) {
			Set<RuntimeRule> rr = runtimeRule.findAllTerminal();
			for (RuntimeRule r : this.getPossibleSubRule(runtimeRule)) {
				rr.addAll(r.findAllTerminal());
			}
			Set<RuntimeRule> skipTerminal = this.getAllSkipTerminals();
			rr.addAll(skipTerminal);
			result = rr.toArray(new RuntimeRule[rr.size()]);
			this.possibleSubTerminal[runtimeRule.getRuleNumber()] = result;
		}
		return result;
	}

	RuntimeRule[][] possibleFirstTerminals;

	public RuntimeRule[] getPossibleFirstTerminals(RuntimeRule runtimeRule) {
		RuntimeRule[] result = this.possibleFirstTerminals[runtimeRule.getRuleNumber()];
		if (null == result) {
			Set<RuntimeRule> rr = runtimeRule.findTerminalAt(0);
			for (RuntimeRule r : this.getPossibleFirstSubRule(runtimeRule)) {
				rr.addAll(r.findTerminalAt(0));
			}
			// Set<RuntimeRule> skipTerminal = this.getAllSkipTerminals();
			// rr.addAll( skipTerminal );
			result = rr.toArray(new RuntimeRule[rr.size()]);
			this.possibleFirstTerminals[runtimeRule.getRuleNumber()] = result;
		}
		return result;
	}

	RuntimeRule[] possibleFirstSkipTerminals;

	public RuntimeRule[] getPossibleFirstSkipTerminals() {
		RuntimeRule[] result = this.possibleFirstSkipTerminals;
		if (null == result) {
			Set<RuntimeRule> skipTerminal = this.getAllSkipTerminals();
			result = skipTerminal.toArray(new RuntimeRule[skipTerminal.size()]);
			this.possibleFirstSkipTerminals = result;
		}
		return result;
	}

	public void build() {
		for (RuntimeRule runtimeRule : this.getAllRules()) {
			this.getPossibleSubTerminal(runtimeRule);
			this.getPossibleFirstTerminals(runtimeRule);
			this.getPossibleSubRule(runtimeRule);
			this.getPossibleFirstSubRule(runtimeRule);
			this.getPossibleSuperRule(runtimeRule);
			this.getPossibleSuperRuleInfo(runtimeRule);
		}
	}

	public static final class SuperRuleInfo {
		public SuperRuleInfo(RuntimeRule runtimeRule, int index) {
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
		public boolean equals(Object arg) {
			if (!(arg instanceof SuperRuleInfo)) {
				return false;
			}
			SuperRuleInfo other = (SuperRuleInfo)arg;
			return this.index == other.index && this.runtimeRule==other.runtimeRule;
		}
		@Override
		public String toString() {
			return "(".concat(this.runtimeRule.toString()).concat(",").concat(Integer.toString(this.index)).concat(")");
		}
	}
	SuperRuleInfo[] findAllSuperRuleInfo(RuntimeRule runtimeRule) {
		Set<SuperRuleInfo> result = new HashSet<>();
		for (RuntimeRule r : this.runtimeRules) {
			if (RuntimeRuleKind.TERMINAL == r.getKind()) {
//				if (r.equals(runtimeRule)) {
//					int index = 0;
//					result.add(new SuperRuleInfo(r, index));
//				}
			} else {
				List<RuntimeRule> rhs = Arrays.asList(r.getRhs().getItems());
				if (  rhs.contains(runtimeRule) ) {
					int index = rhs.indexOf(runtimeRule);
					result.add(new SuperRuleInfo(r, index));
				}
			}
		}
		return result.toArray(new SuperRuleInfo[result.size()]);
	}
	
	RuntimeRule[] findAllSuperRule(RuntimeRule runtimeRule) {
		Set<RuntimeRule> result = new HashSet<>();
		for (RuntimeRule r : this.runtimeRules) {
			if (RuntimeRuleKind.NON_TERMINAL == runtimeRule.getKind() && r.findAllNonTerminal().contains(runtimeRule)
					|| RuntimeRuleKind.TERMINAL == runtimeRule.getKind() && r.findAllTerminal().contains(runtimeRule)) {
				result.add(r);
			}
		}
		return result.toArray(new RuntimeRule[result.size()]);
	}

	Map<String, RuntimeRule> terminalMap;

	public RuntimeRule getForTerminal(String terminal) {
		RuntimeRule rr = this.terminalMap.get(terminal);
		return rr;
	}

	public void putForTerminal(String terminal, RuntimeRule runtimeRule) {
		this.terminalMap.put(terminal, runtimeRule);
	}

	String toString_cache;

	@Override
	public String toString() {
		if (null == this.toString_cache) {
			this.toString_cache = "";
			for (RuntimeRule r : this.getAllRules()) {
				this.toString_cache += r + System.lineSeparator();
			}
		}
		return this.toString_cache;
	}

}