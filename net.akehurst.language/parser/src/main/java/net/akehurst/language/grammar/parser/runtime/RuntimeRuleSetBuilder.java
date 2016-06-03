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

import java.util.regex.Pattern;

import net.akehurst.language.core.parser.INode;
import net.akehurst.language.grammar.parse.tree.Branch;
import net.akehurst.language.grammar.parse.tree.Factory;
import net.akehurst.language.grammar.parse.tree.IInput;
import net.akehurst.language.grammar.parse.tree.Leaf;
import net.akehurst.language.grammar.parser.forrest.Input;
import net.akehurst.language.ogl.semanticStructure.Rule;
import net.akehurst.language.ogl.semanticStructure.Terminal;
import net.akehurst.language.ogl.semanticStructure.TerminalEmpty;
import net.akehurst.language.ogl.semanticStructure.TerminalPattern;

public class RuntimeRuleSetBuilder {

	public RuntimeRuleSetBuilder() {
		this.parseTreeFactory = new Factory();
//		this.emptyRule = createRuntimeRule(new TerminalEmpty());
//		this.emptyRule.setIsSkipRule(false);
//		this.emptyRule.setIsEmptyRule(true);
	}
	
	Factory parseTreeFactory;
	RuntimeRuleSet runtimeRuleSet;
	
//	RuntimeRule emptyRule;
//	public RuntimeRule getEmptyRule(RuntimeRule ruleThatIsEmpty) {
//		return this.emptyRule;
//	}
	
	int nextRuleNumber;
	public RuntimeRule createRuntimeRule(Rule grammarRule) {
		RuntimeRule rr = new RuntimeRule(this.runtimeRuleSet, grammarRule.getName(), nextRuleNumber, RuntimeRuleKind.NON_TERMINAL, Pattern.LITERAL);
		++nextRuleNumber;
		return rr;
	}
	
	public RuntimeRule createRuntimeRule(Terminal terminal) {
		int patternFlags = (terminal instanceof TerminalPattern) ? Pattern.MULTILINE : Pattern.LITERAL;
		RuntimeRule rr = new RuntimeRule(this.runtimeRuleSet, terminal.getValue(), nextRuleNumber, RuntimeRuleKind.TERMINAL, patternFlags);
		++nextRuleNumber;
		return rr;
	}
	
	public RuntimeRule createEmptyRule(RuntimeRule ruleThatIsEmpty) {
		RuntimeRule rr = new RuntimeRule(this.runtimeRuleSet, "$empty."+ruleThatIsEmpty.getName()+"$", nextRuleNumber, RuntimeRuleKind.TERMINAL, Pattern.LITERAL);
		++nextRuleNumber;
		RuntimeRuleItem emptyRhs = this.createRuntimeRuleItem(RuntimeRuleItemKind.EMPTY);
		rr.setRhs(emptyRhs);
		emptyRhs.setItems(new RuntimeRule[]{ruleThatIsEmpty});
		return rr;
	}
	
	public RuntimeRuleSet createRuntimeRuleSet(int totalRuleNumber) {
		if (null==this.runtimeRuleSet) {
			this.runtimeRuleSet = new RuntimeRuleSet(totalRuleNumber);//, this.getEmptyRule().getRuleNumber());
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
		return this.parseTreeFactory.createBranch(r, children);
	}
	
	public Leaf createLeaf(IInput input, int start, int end, RuntimeRule terminalRule) {
		return this.parseTreeFactory.createLeaf(input, start, end, terminalRule);
	}

	public RuntimeRuleItem createRuntimeRuleItem(RuntimeRuleItemKind kind) {
		int maxRuleRumber = this.getRuntimeRuleSet().getTotalRuleNumber();
		return new RuntimeRuleItem(kind,maxRuleRumber);
	}
}
