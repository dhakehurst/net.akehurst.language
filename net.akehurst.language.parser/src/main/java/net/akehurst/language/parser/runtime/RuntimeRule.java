package net.akehurst.language.parser.runtime;

import java.util.HashSet;
import java.util.Set;

import net.akehurst.language.ogl.semanticModel.Rule;
import net.akehurst.language.ogl.semanticModel.Terminal;

public class RuntimeRule {

	RuntimeRule(RuntimeRuleSet runtimeRuleSet, int ruleNumber, RuntimeRuleKind kind) {
		this.runtimeRuleSet = runtimeRuleSet;
		this.ruleNumber = ruleNumber;
		this.kind = kind;
	}
	
	RuntimeRuleSet runtimeRuleSet;
	public RuntimeRuleSet getRuntimeRuleSet() {
		return this.runtimeRuleSet;
	}
	
	int ruleNumber;
	public int getRuleNumber() {
		return this.ruleNumber;
	}
	
	RuntimeRuleKind kind;
	public RuntimeRuleKind getKind(){
		return this.kind;
	}
	
	boolean isSkipRule;
	public boolean getIsSkipRule() {
		return this.isSkipRule;
	}
	public void setIsSkipRule(boolean value) {
		this.isSkipRule = value;
	}
	
	RuntimeRuleItem rhs;
	public void setRhs(RuntimeRuleItem value) {
		this.rhs = value;
	}
	public RuntimeRuleItem getRhs() {
		return rhs;
	}
	public RuntimeRule getRhsItem(int index) {
		return this.getRhs().getItems()[index];
	}
	public RuntimeRule getSeparator() {
		return this.getRhsItem(1);
	}
	
	Set<RuntimeRule> findAllNonTerminal() {
		Set<RuntimeRule> result = new HashSet<>();
		if (RuntimeRuleKind.TERMINAL == this.getKind()) {
			return result;
		}
		for(RuntimeRule item : this.getRhs().getItems()) {
			if (RuntimeRuleKind.NON_TERMINAL == item.getKind()) {
				result.add(item);
			}
		}
		return result;
	}
	
	Set<RuntimeRule> findAllNonTerminalAt(int n) {
		Set<RuntimeRule> result = new HashSet<>();
		if (RuntimeRuleKind.TERMINAL == this.getKind()) {
			return result;
		}
		RuntimeRule[] items = this.getRhs().getItemAt(n);
		for(RuntimeRule item: items) {
			if (RuntimeRuleKind.NON_TERMINAL == item.getKind()) {
				result.add(item);
			}
		}
		return result;
	}
	
	
	Set<RuntimeRule> findAllTerminal() {
		Set<RuntimeRule> result = new HashSet<>();
		if (RuntimeRuleKind.TERMINAL == this.getKind()) {
			return result;
		}
		for(RuntimeRule item : this.getRhs().getItems()) {
			if (RuntimeRuleKind.TERMINAL == item.getKind()) {
				result.add(item);
			}
		}
		return result;
	}
	
	Set<RuntimeRule> findSubRules() {
		Set<RuntimeRule> result = this.findAllNonTerminal();
		Set<RuntimeRule> oldResult = new HashSet<>();
		while (!oldResult.containsAll(result)) {
			oldResult = new HashSet<>();
			oldResult.addAll(result);
			for(RuntimeRule nt: oldResult) {
				Set<RuntimeRule> newNts = nt.findAllNonTerminal();
				newNts.removeAll(result);
				result.addAll(newNts);
			}
		}
		return result;
	}
	
	Set<RuntimeRule> findSubRulesAt(int n) {
		Set<RuntimeRule> result = this.findAllNonTerminalAt(n);
		Set<RuntimeRule> oldResult = new HashSet<>();
		while (!oldResult.containsAll(result)) {
			oldResult = new HashSet<>();
			oldResult.addAll(result);
			for(RuntimeRule nt: oldResult) {
				Set<RuntimeRule> newNts = nt.findAllNonTerminalAt(n);
				newNts.removeAll(result);
				result.addAll(newNts);
			}
		}
		return result;
	}
	
	public Set<RuntimeRule> findTerminalAt(int n) {
		Set<RuntimeRule> result = new HashSet<>();
		if (RuntimeRuleKind.TERMINAL == this.getKind()) {
			return result;
		}
		RuntimeRule[] firstItems = this.getRhs().getItemAt(n);
		for(RuntimeRule item: firstItems) {
			if (RuntimeRuleKind.TERMINAL == item.getKind()) {
				result.add( item );
			}
		}
		return result;
	}
	
	Rule grammarRule;
	public Rule getGrammarRule() {
		return this.grammarRule;
	}
	public void setGrammarRule(Rule value) {
		this.grammarRule = value;
	}
	
	
	Terminal terminal;
	public Terminal getTerminal() {
		return this.terminal;
	}
	public void setTerminal(Terminal value) {
		this.terminal = value;
	}
	
	String toString_cache;
	@Override
	public String toString() {
		if (null==this.toString_cache) {
			if (RuntimeRuleKind.NON_TERMINAL == this.getKind()) {
				this.toString_cache = this.getGrammarRule().toString();
			} else {
				this.toString_cache = this.getTerminal().toString();
			}
		}
		return this.toString_cache;
	}
	
	@Override
	public int hashCode() {
		return this.getRuleNumber();
	}
	
	@Override
	public boolean equals(Object arg) {
		if (!(arg instanceof RuntimeRule)) {
			return false;
		}
		RuntimeRule other = (RuntimeRule)arg;
		return this.getRuleNumber() == other.getRuleNumber();
	}

	
}
