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
package net.akehurst.language.parser.forrest;

import java.util.List;
import java.util.Map;

import net.akehurst.language.core.parser.INode;
import net.akehurst.language.ogl.semanticStructure.RuleNotFoundException;
import net.akehurst.language.parser.runtime.Branch;
import net.akehurst.language.parser.runtime.Factory;
import net.akehurst.language.parser.runtime.Leaf;
import net.akehurst.language.parser.runtime.RuntimeRule;

public class ForrestFactory {
	
	public ForrestFactory(Factory runtimeFactory, CharSequence text) {
		this.runtimeFactory = runtimeFactory;
		this.input = new Input(this, text);
	}
	
	Factory runtimeFactory;
	Input input;

	Map<BranchIdentifier, Branch> branch_cache;
	
	public List<ParseTreeBud> createNewBuds(RuntimeRule[] possibleNextTerminals, int pos) throws RuleNotFoundException {
		return this.input.createNewBuds(possibleNextTerminals, pos);
	}
	
	public ParseTreeBud fetchOrCreateBud(Leaf leaf, AbstractParseTree stackedTree) {
		ParseTreeBud bud = new ParseTreeBud(this, leaf, stackedTree);
		return bud;
	}
	
	public ParseTreeBranch fetchOrCreateBranch(RuntimeRule target, INode[] children, AbstractParseTree stackedTree, int nextItemIndex) {
//		BranchIdentifier bid = new BranchIdentifier(target, );
//		Branch newBranch = this.branch_cache.get(bid);
//		if (null==newBranch) {
//			newBranch = this.runtimeFactory.createBranch(target, children);
//		}
		Branch newBranch = this.runtimeFactory.createBranch(target, children);
		ParseTreeBranch newTree = new ParseTreeBranch(this, newBranch, stackedTree, target, nextItemIndex);

		return newTree;
	}
	
}
