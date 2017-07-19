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

import net.akehurst.language.core.grammar.ITerminal;
import net.akehurst.language.core.parser.INode;
import net.akehurst.language.grammar.parse.tree.Branch;
import net.akehurst.language.grammar.parse.tree.Factory;
import net.akehurst.language.grammar.parse.tree.Leaf;
import net.akehurst.language.ogl.semanticStructure.Rule;
import net.akehurst.language.ogl.semanticStructure.TerminalPattern;

public class RuntimeRuleSetBuilder {

	public RuntimeRuleSetBuilder() {
		this.parseTreeFactory = new Factory();
		// this.emptyRule = createRuntimeRule(new TerminalEmpty());
		// this.emptyRule.setIsSkipRule(false);
		// this.emptyRule.setIsEmptyRule(true);
	}

	Factory parseTreeFactory;
	RuntimeRuleSet runtimeRuleSet;

	// RuntimeRule emptyRule;
	// public RuntimeRule getEmptyRule(RuntimeRule ruleThatIsEmpty) {
	// return this.emptyRule;
	// }

	public RuntimeRule getRuntimeRule(final ITerminal terminal) {
		return this.runtimeRuleSet.getForTerminal(terminal.getValue());
	}

	public RuntimeRule getRuntimeRule(final Rule rule) {
		return this.runtimeRuleSet.getRuntimeRule(rule);
	}

	int nextRuleNumber;

	public RuntimeRule createRuntimeRule(final Rule grammarRule) {
		final RuntimeRule rr = new RuntimeRule(this.runtimeRuleSet, grammarRule.getName(), this.nextRuleNumber, RuntimeRuleKind.NON_TERMINAL, Pattern.LITERAL);
		++this.nextRuleNumber;
		return rr;
	}

	public RuntimeRule createRuntimeRule(final ITerminal terminal) {
		final int patternFlags = terminal instanceof TerminalPattern ? Pattern.MULTILINE : Pattern.LITERAL;
		final RuntimeRule rr = new RuntimeRule(this.runtimeRuleSet, terminal.getValue(), this.nextRuleNumber, RuntimeRuleKind.TERMINAL, patternFlags);
		++this.nextRuleNumber;
		return rr;
	}

	public RuntimeRule createEmptyRule(final RuntimeRule ruleThatIsEmpty) {
		final RuntimeRule rr = new RuntimeRule(this.runtimeRuleSet, "$empty." + ruleThatIsEmpty.getName() + "$", this.nextRuleNumber, RuntimeRuleKind.TERMINAL,
				Pattern.LITERAL);
		++this.nextRuleNumber;
		final RuntimeRuleItem emptyRhs = this.createRuntimeRuleItem(RuntimeRuleItemKind.EMPTY);
		rr.setRhs(emptyRhs);
		emptyRhs.setItems(new RuntimeRule[] { ruleThatIsEmpty });
		return rr;
	}

	public RuntimeRuleSet createRuntimeRuleSet(final int totalRuleNumber) {
		if (null == this.runtimeRuleSet) {
			this.runtimeRuleSet = new RuntimeRuleSet(totalRuleNumber);// , this.getEmptyRule().getRuleNumber());
		}
		return this.runtimeRuleSet;
	}

	public RuntimeRuleSet getRuntimeRuleSet() {
		if (null == this.runtimeRuleSet) {
			throw new RuntimeException("Internal Error: must createRuntimeRuleSet before getting");
		} else {
			return this.runtimeRuleSet;
		}
	}

	public Branch createBranch(final RuntimeRule r, final INode[] children) {
		return this.parseTreeFactory.createBranch(r, children);
	}

	public Leaf createLeaf(final String text, final int start, final int end, final RuntimeRule terminalRule) {
		return this.parseTreeFactory.createLeaf(text, start, end, terminalRule);
	}

	public Leaf createEmptyLeaf(final int pos, final RuntimeRule terminalRule) {
		return this.parseTreeFactory.createEmptyLeaf(pos, terminalRule);
	}

	public RuntimeRuleItem createRuntimeRuleItem(final RuntimeRuleItemKind kind) {
		final int maxRuleRumber = this.getRuntimeRuleSet().getTotalRuleNumber();
		return new RuntimeRuleItem(kind, maxRuleRumber);
	}
}
