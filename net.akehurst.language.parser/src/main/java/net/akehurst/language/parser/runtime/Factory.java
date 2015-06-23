package net.akehurst.language.parser.runtime;

import java.util.List;

import net.akehurst.language.core.parser.INode;
import net.akehurst.language.ogl.semanticModel.Rule;
import net.akehurst.language.ogl.semanticModel.Terminal;
import net.akehurst.language.parser.forrest.Input;

public class Factory {

	public Factory() {
		
	}
	
	RuntimeRuleSet runtimeRuleSet;
	
	int nextRuleNumber;
	public RuntimeRule createRuntimeRule(Rule grammarRule, RuntimeRuleKind kind) {
		RuntimeRule rr = new RuntimeRule(this.runtimeRuleSet, nextRuleNumber, kind);
		rr.setGrammarRule(grammarRule);
		++nextRuleNumber;
		return rr;
	}
	
	public RuntimeRule createRuntimeRule(Terminal terminal, RuntimeRuleKind kind) {
		RuntimeRule rr = new RuntimeRule(this.runtimeRuleSet, nextRuleNumber, kind);
		rr.setTerminal(terminal);
		++nextRuleNumber;
		return rr;
	}
	
	public RuntimeRuleSet createRuntimeRuleSet(int totalRuleNumber) {
		if (null==this.runtimeRuleSet) {
			this.runtimeRuleSet = new RuntimeRuleSet(totalRuleNumber);
		}
		return this.runtimeRuleSet;
	}
	public RuntimeRuleSet getRuntimeRuleSet() {
		if (null==this.runtimeRuleSet) {
			throw new RuntimeException("Internal Error: must createRuntimeRuleSet before getting");
		} else {
			return this.runtimeRuleSet;
		}
	}
	
	public Branch createBranch(final RuntimeRule r, final INode[] children) {
		Branch b = new Branch(r, children);
		return b;
	}
	
	public Leaf createLeaf(Input input, int start, int end, RuntimeRule terminalRule) {
		Leaf l = new Leaf(input, start, end, terminalRule);
		return l;
	}
}
