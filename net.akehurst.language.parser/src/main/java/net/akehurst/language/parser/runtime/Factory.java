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
package net.akehurst.language.parser.runtime;

import net.akehurst.language.core.parser.INode;
import net.akehurst.language.ogl.semanticModel.Rule;
import net.akehurst.language.ogl.semanticModel.Terminal;
import net.akehurst.language.ogl.semanticModel.TerminalEmpty;
import net.akehurst.language.parser.forrest.Input;

public class Factory {

	public Factory() {
		this.emptyRule = createRuntimeRule(new TerminalEmpty(), RuntimeRuleKind.TERMINAL);
		this.emptyRule.setIsSkipRule(false);
	}
	
	RuntimeRuleSet runtimeRuleSet;
	
	RuntimeRule emptyRule;
	public RuntimeRule getEmptyRule() {
		return this.emptyRule;
	}
	
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
			this.runtimeRuleSet = new RuntimeRuleSet(totalRuleNumber, this.getEmptyRule().getRuleNumber());
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
