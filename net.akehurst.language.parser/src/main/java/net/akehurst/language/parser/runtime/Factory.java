package net.akehurst.language.parser.runtime;

import java.util.List;

import net.akehurst.language.core.parser.INode;
import net.akehurst.language.ogl.semanticModel.Rule;
import net.akehurst.language.ogl.semanticModel.Terminal;
import net.akehurst.language.parser.forrest.Branch;

public class Factory {

	public Factory() {
		this.runtimeRuleSet = new RuntimeRuleSet();
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
	
	public RuntimeRuleSet createRuntimeRuleSet() {
		return this.runtimeRuleSet;
	}
	
	public Branch createBranch(RuntimeRule r, List<INode> children) {
		Branch b = new Branch(this, r, children);
		return b;
	}
	
}
