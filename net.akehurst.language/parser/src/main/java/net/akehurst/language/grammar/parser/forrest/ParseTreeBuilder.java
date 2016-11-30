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

import net.akehurst.language.core.parser.IBranch;
import net.akehurst.language.core.parser.ILeaf;
import net.akehurst.language.core.parser.INode;
import net.akehurst.language.core.parser.RuleNotFoundException;
import net.akehurst.language.grammar.parse.tree.Leaf;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleSetBuilder;
import net.akehurst.language.ogl.semanticStructure.Grammar;
import net.akehurst.language.ogl.semanticStructure.Rule;
import net.akehurst.language.ogl.semanticStructure.Terminal;
import net.akehurst.language.ogl.semanticStructure.TerminalEmpty;
import net.akehurst.language.ogl.semanticStructure.TerminalLiteral;

public class ParseTreeBuilder {

	public ParseTreeBuilder(RuntimeRuleSetBuilder runtimeRules, Grammar grammar, String goal, CharSequence text, int offset) {
		this.runtimeBuilder = runtimeRules;
		this.input = new Input3(runtimeRules, text);
		this.grammar = grammar;
		this.textAccumulator = "";
		this.textLength = 0;
		this.offset = offset;
	}

	RuntimeRuleSetBuilder runtimeBuilder;
	Grammar grammar;
	Input3 input;
	String textAccumulator;
	int textLength;
	int offset;

	public ILeaf leaf(String text) {
		return this.leaf(text, text);
	}
	
	public ILeaf leaf(String terminalPattern, String text) {
		int start = this.textLength+this.offset;
		this.textLength+=text.length();
		int end = this.textLength +this.offset;
		Terminal terminal = null;
		if (terminalPattern.isEmpty()) {
			terminal = new TerminalEmpty();
		} else {
			terminal = this.grammar.getAllTerminal().stream().filter(t -> t.getPattern().pattern().equals(terminalPattern)).findFirst().get();
		}
		RuntimeRule terminalRule = this.runtimeBuilder.getRuntimeRuleSet().getForTerminal(terminal.getValue());
		ILeaf l = this.runtimeBuilder.createLeaf(this.input, start, end, terminalRule);
		return l;
	}

	public ILeaf emptyLeaf(String ruleNameThatIsEmpty) {
		int start = this.textLength +this.offset;
		RuntimeRule ruleThatIsEmpty = this.runtimeBuilder.getRuntimeRuleSet().getRuntimeRule(ruleNameThatIsEmpty);
		RuntimeRule terminalRule = this.runtimeBuilder.getRuntimeRuleSet().getEmptyRule(ruleThatIsEmpty);
		return this.runtimeBuilder.createLeaf(this.input, start, start, terminalRule);
	}

	public IBranch branch(String ruleName, INode... children) {
		try {
			Rule rule = this.grammar.findRule(ruleName);
			RuntimeRule rr = this.runtimeBuilder.getRuntimeRuleSet().getRuntimeRule(rule);
			IBranch b = this.runtimeBuilder.createBranch(rr, children);
			return b;
		} catch (RuleNotFoundException e) {
			throw new RuntimeException("Error", e);
		}
	}

//	public IParseTree tree(IBranch root) {
//		try {
//			Rule rule = this.grammar.findRule(root.getName());
//			RuntimeRule rr = this.factory.getRuntimeRuleSet().getRuntimeRule(rule);
//			IParseTree t = new ParseTreeBranch(this.factory, input, (Branch)root, null, rr, Integer.MAX_VALUE);
//			return t;
//		} catch (RuleNotFoundException e) {
//			throw new RuntimeException("Error", e);
//		}
//	}

}
