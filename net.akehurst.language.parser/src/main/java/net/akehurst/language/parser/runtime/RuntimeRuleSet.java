package net.akehurst.language.parser.runtime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.akehurst.language.core.parser.INodeIdentity;
import net.akehurst.language.core.parser.INodeType;
import net.akehurst.language.ogl.semanticModel.Rule;
import net.akehurst.language.ogl.semanticModel.SkipRule;
import net.akehurst.language.ogl.semanticModel.Terminal;
import net.akehurst.language.ogl.semanticModel.TerminalLiteral;

public class RuntimeRuleSet {

	/*
	 * necessary to know total number of rules up front,
	 * so it can be used in RuntimeRuleItem
	 */
	public RuntimeRuleSet(int totalRuleNumber) {
		this.totalRuleNumber = totalRuleNumber;
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
		this.runtimeRules = new RuntimeRule[numberOfRules];
		this.nodeTypes = new ArrayList<>(Arrays.asList(new INodeType[numberOfRules]));
		this.ruleNumbers = new HashMap<>();
		this.terminalMap = new HashMap<>();

		for(RuntimeRule rrule: value) {
			int i = rrule.getRuleNumber();
			this.runtimeRules[i] = rrule;
			if (RuntimeRuleKind.NON_TERMINAL == rrule.getKind()) {
				this.nodeTypes.set(i, rrule.getGrammarRule().getNodeType());
				this.ruleNumbers.put(rrule.getGrammarRule().getNodeType(), i);
			} else {
				this.terminalMap.put(rrule.getTerminal(), rrule);
			}
		}
	}
	public RuntimeRule getRuntimeRule(int index) {
		return this.runtimeRules[index];
	}
	public RuntimeRule getRuntimeRule(Rule rule) {
		int index = this.getRuleNumber(rule.getNodeType());
		return this.getRuntimeRule(index);
	}
	
	public Rule getRule(int ruleNumber) {
		return this.getRuntimeRule(ruleNumber).getGrammarRule();
	}
	
	List<RuntimeRule> allSkipRules_cache;
	List<RuntimeRule> getAllSkipRules() {
		if (null==this.allSkipRules_cache) {
			this.allSkipRules_cache = new ArrayList<>();
			for(RuntimeRule r: this.getAllRules()) {
				if (r.getIsSkipRule()) {
					this.allSkipRules_cache.add(r);
				}
			}
		}
		return this.allSkipRules_cache;
	}
	
	Set<RuntimeRule> getAllSkipTerminals() {
		Set<RuntimeRule> result = new HashSet<>();
		for(RuntimeRule r: this.getAllSkipRules()) {
			for(RuntimeRule rr: r.getRhs().getItems()) {
				if (RuntimeRuleKind.TERMINAL==rr.getKind()) {
					result.add(rr);
				}
			}
		}
		return result;
	}
	
	ArrayList<INodeType> nodeTypes;
	public INodeType getNodeType(int nodeTypeNumber) {
		return this.nodeTypes.get(nodeTypeNumber);
	}
	
	Map<INodeType, Integer> ruleNumbers;
	public int getRuleNumber(INodeType rule) {
		return this.ruleNumbers.get(rule);
	}

	RuntimeRule[][] possibleSubTerminal;
	RuntimeRule[][] possibleFirstTerminals;
	RuntimeRule[][] possibleSubRule;
	RuntimeRule[][] possibleFirstRule;
	RuntimeRule[][] possibleSuperRule;
	
	public RuntimeRule[] getPossibleSubRule(RuntimeRule runtimeRule) {
		RuntimeRule[] result = this.possibleSubRule[runtimeRule.getRuleNumber()];
		if (null==result) {
			Set<RuntimeRule> rr = runtimeRule.findSubRules();
			result = rr.toArray(new RuntimeRule[rr.size()]);
			this.possibleSubRule[runtimeRule.getRuleNumber()] = result;
		}
		return result;
	}

	public RuntimeRule[] getPossibleFirstSubRule(RuntimeRule runtimeRule) {
		RuntimeRule[] result = this.possibleFirstRule[runtimeRule.getRuleNumber()];
		if (null==result) {
			Set<RuntimeRule> rr = runtimeRule.findSubRulesAt(0);
			result = rr.toArray(new RuntimeRule[rr.size()]);
			this.possibleFirstRule[runtimeRule.getRuleNumber()] = result;
		}
		return result;
	}
	
	public RuntimeRule[] getPossibleSuperRule(RuntimeRule runtimeRule) {
		RuntimeRule[] result = this.possibleSuperRule[runtimeRule.getRuleNumber()];
		if (null == result) {
			result = this.findAllSuperRule(runtimeRule);
			this.possibleSuperRule[runtimeRule.getRuleNumber()] = result;
		}
		return result;
	}
	
	RuntimeRule[] findAllSuperRule(RuntimeRule runtimeRule) {
		Set<RuntimeRule> result = new HashSet<>();
		for (RuntimeRule r : this.runtimeRules) {
			if (
					RuntimeRuleKind.NON_TERMINAL == runtimeRule.getKind() && r.findAllNonTerminal().contains(runtimeRule)
					|| RuntimeRuleKind.TERMINAL == runtimeRule.getKind() && r.findAllTerminal().contains(runtimeRule)
			) {
				result.add(r);
			}
		}
		return result.toArray(new RuntimeRule[result.size()]);
	}

	public RuntimeRule[] getPossibleSubTerminal(RuntimeRule runtimeRule) {
		RuntimeRule[] result = this.possibleSubTerminal[runtimeRule.getRuleNumber()];
		if (null == result) {
			Set<RuntimeRule> rr = runtimeRule.findAllTerminal();
			for(RuntimeRule r : this.getPossibleSubRule(runtimeRule)) {
				rr.addAll( r.findAllTerminal() );
			}
			Set<RuntimeRule> skipTerminal = this.getAllSkipTerminals();
			rr.addAll( skipTerminal );
			result = rr.toArray(new RuntimeRule[rr.size()]);
			this.possibleSubTerminal[runtimeRule.getRuleNumber()] = result;
		}
		return result;
	}

	public RuntimeRule[] getPossibleFirstTerminals(RuntimeRule runtimeRule) {
		RuntimeRule[] result = this.possibleFirstTerminals[runtimeRule.getRuleNumber()];
		if (null == result) {
			Set<RuntimeRule> rr = runtimeRule.findTerminalAt(0);
			for(RuntimeRule r : this.getPossibleFirstSubRule(runtimeRule)) {
				rr.addAll( r.findTerminalAt(0) );
			}
			Set<RuntimeRule> skipTerminal = this.getAllSkipTerminals();
			rr.addAll( skipTerminal );
			result = rr.toArray(new RuntimeRule[rr.size()]);
			this.possibleFirstTerminals[runtimeRule.getRuleNumber()] = result;
		}
		return result;
	}
	
	Map<Terminal, RuntimeRule> terminalMap;
	public RuntimeRule getForTerminal(Terminal terminal) {
		RuntimeRule rr = this.terminalMap.get(terminal);
		return rr;
	}
	
	String toString_cache;
	@Override
	public String toString() {
		if (null==this.toString_cache) {
			this.toString_cache = "";
			for(RuntimeRule r : this.getAllRules()) {
				this.toString_cache += r + System.lineSeparator();
			}
		}
		return this.toString_cache;
	}

	
}
