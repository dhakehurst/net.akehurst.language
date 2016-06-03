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

import java.util.List;
import java.util.Map;

import net.akehurst.language.core.parser.INode;
import net.akehurst.language.core.parser.RuleNotFoundException;
import net.akehurst.language.grammar.parse.tree.Branch;
import net.akehurst.language.grammar.parse.tree.Leaf;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleSetBuilder;

public class ForrestFactory2 {
	
	public ForrestFactory2(RuntimeRuleSetBuilder runtimeFactory, CharSequence text) {
		this.runtimeBuilder = runtimeFactory;
		this.input = new Input2(this, text);
	}
	
	RuntimeRuleSetBuilder runtimeBuilder;
	public Leaf createLeaf(Input2 input, int start, int end, RuntimeRule terminalRule) {
		return this.runtimeBuilder.createLeaf(input, start, end, terminalRule);
	}
	public Branch createBranch(final RuntimeRule r, final INode[] children) {
		return this.runtimeBuilder.createBranch(r, children);
	}
	
	Input2 input;

	Map<NodeIdentifier, Branch> branch_cache;
	
	public List<ParseTreeBud2> createNewBuds(RuntimeRule[] possibleNextTerminals, int pos) throws RuleNotFoundException {
		return this.input.createNewBuds(possibleNextTerminals, pos);
	}
	
	public ParseTreeBud2 fetchOrCreateBud(Leaf leaf) {
		ParseTreeBud2 bud = new ParseTreeBud2(leaf);
		return bud;
	}
	
	public ParseTreeBranch2 fetchOrCreateBranch(RuntimeRule runtimeRule, INode[] children, int nextItemIndex) {
		Branch newBranch = this.runtimeBuilder.createBranch(runtimeRule, children);
		ParseTreeBranch2 newTree = new ParseTreeBranch2(newBranch, nextItemIndex, this.input.getLength());
		return newTree;
	}
	
}
