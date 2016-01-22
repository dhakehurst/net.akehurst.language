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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.core.parser.ParseFailedException;
import net.akehurst.language.core.parser.ParseTreeException;
import net.akehurst.language.grammar.parser.ScannerLessParser;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleSet;
import net.akehurst.language.ogl.semanticStructure.RuleNotFoundException;

public class Forrest {

	public Forrest(RuntimeRule goalRRule, RuntimeRuleSet runtimeRuleSet) {
		this.goalRRule = goalRRule;
		this.runtimeRuleSet = runtimeRuleSet;
		this.canGrow = false;
		this.longestMatch = null;
		this.done = new HashMap<>();
	}

	RuntimeRule goalRRule;
	RuntimeRuleSet runtimeRuleSet;


	Input input;
	ArrayList<AbstractParseTree> possibleTrees = new ArrayList<>();
	Set<IParseTree> goalTrees = new HashSet<>();
	ArrayList<IParseTree> gt = new ArrayList<>();
	AbstractParseTree longestMatch;

	Map<BranchIdentifier, AbstractParseTree> done;

	public int size() {
		return this.possibleTrees.size();
	}
	
	Forrest newForrest() {
		Forrest newForrest = new Forrest(this.goalRRule, this.runtimeRuleSet);
		newForrest.goalTrees.addAll(this.goalTrees);
		newForrest.longestMatch = this.longestMatch;
		newForrest.done = this.done;
		return newForrest;
	}
	
	public ArrayList<IParseTree> getGT() {
		gt = new ArrayList<>();
		gt.addAll(this.goalTrees);
		return gt;
	}

	boolean canGrow;

	public boolean getCanGrow() {
		return this.canGrow;
	}

	public IParseTree getLongestMatch(CharSequence text) throws ParseFailedException {
		if (!this.goalTrees.isEmpty() && this.goalTrees.size() >= 1) {
			IParseTree lt = this.getGT().get(0);
			for (IParseTree gt : this.goalTrees) {
				if (gt.getRoot().getMatchedTextLength() > lt.getRoot().getMatchedTextLength()) {
					lt = gt;
				}
			}
			if (lt.getRoot().getMatchedTextLength() < text.length()) {
				throw new ParseFailedException("Goal does not match full text", this.longestMatch);
			} else {
				return lt;
			}
		} else {
			throw new ParseFailedException("Could not match goal", this.longestMatch);
		}
	}

	/**
	 * <code>
	 *  for each growable tree in the forrest
	 *    create new buds for that tree (grab all possible next tokens)
	 *    grow buds into all possible complete branches
	 *    try expand possible tree by those branches
	 *    add expanded trees to new Forrest (this will sort them into goal matches, possibles, or dropped)
	 * </code>
	 * 
	 * @return
	 * @throws RuleNotFoundException
	 * @throws ParseTreeException
	 */
	public Forrest grow() throws RuleNotFoundException, ParseTreeException {
		return this.growBreadthFirst();
		// return this.growDepthFirst();
	}

	public Forrest growBreadthFirst() throws RuleNotFoundException, ParseTreeException {
//		 System.out.println("posibles: "+this.possibleTrees.size());
		Forrest newForrest = this.newForrest();
		for (AbstractParseTree tree : this.possibleTrees) {
			ArrayList<AbstractParseTree> newSkipBranches = tree.growWidthWithSkipRules(this.runtimeRuleSet);
			if (!newSkipBranches.isEmpty()) {
				newForrest.addAll(newSkipBranches);
			} else {
	
					ArrayList<AbstractParseTree> newBranches = tree.growWidthAndHeight(this.runtimeRuleSet);
//				ArrayList<AbstractParseTree> newBranches = tree.growWidthAndHeightUntilProgress(this.runtimeRuleSet);
					newForrest.addAll(newBranches);
					
					//TODO: should have some kind of merge so we don't continue with existing trees
					// i.e. if head is present in the stack of an existing tree!
					
			}
		}

		return newForrest;
	}

	Forrest growDepthFirst() throws RuleNotFoundException, ParseTreeException {
		Forrest newForrest = new Forrest(this.goalRRule, this.runtimeRuleSet);
		newForrest.goalTrees.addAll(this.goalTrees);
		newForrest.longestMatch = this.longestMatch;

		for (AbstractParseTree tree : this.possibleTrees) {
			// System.out.println(tree.getIdString());
			RuntimeRule treeRR = tree.getRoot().getRuntimeRule();

			AbstractParseTree ntree = tree;
			while (null != tree && tree.getCanGrow()) {
				ntree = null;

				while (tree.getCanGraftBack()) {
					AbstractParseTree nt = tree.tryGraftBack();
					if (null != nt) {
						ntree = nt;
						tree = ntree;
					} else {
						break;
					}
				}

				while (tree.getIsComplete()) {
					ArrayList<AbstractParseTree> nts = tree.growHeight(this.runtimeRuleSet);
					if (!nts.isEmpty()) {
						ntree = nts.get(0);
						tree = ntree;
						for (int i = 0; i < nts.size(); ++i) {
							newForrest.add(nts.get(i));
						}
					} else {
						break;
					}
				}

				if (tree.getCanGrowWidth()) {
					if (tree.getIsEmpty() || (tree.getIsComplete() && !tree.getCanGrow())) {
						// don't grow width

					} else {
						ArrayList<AbstractParseTree> nts = tree.growWidth(this.runtimeRuleSet);
						if (!nts.isEmpty()) {
							ntree = nts.get(0);
							tree = ntree;
							for (int i = 1; i < nts.size(); ++i) {
								newForrest.add(nts.get(i));
							}
						} else {
							break;
						}
					}
				}
				tree = ntree;
			}
			if (null != ntree) {
				newForrest.add(ntree);
			}
		}

		return newForrest;
	}

	public void addAll(Collection<? extends AbstractParseTree> trees) throws ParseTreeException {
		for (AbstractParseTree tree : trees) {
			this.add(tree);
		}
	}

	int overwrites;

	
	public void add(AbstractParseTree tree) throws ParseTreeException {
		AbstractParseTree old = this.done.get(tree.identifier);
		if (null!=old) {
			return;
		} else {
			this.done.put(tree.identifier, tree);
		}
		if (tree.getIsComplete()) {
			// this.newGrownBranches.add(tree);
			if (null == tree.stackedTree
					&& this.goalRRule.getRuleNumber() == tree.getRoot().getRuntimeRule().getRuleNumber()) {
				this.goalTrees.add(tree);
			}
		}
		if (tree.getHasPotential(this.runtimeRuleSet)) {
			// tree is incomplete but still possible to grow it
			// if (this.possibleTrees.contains(tree)) {
			// overwrites++;
			// // throw new ParseTreeException("Overwriting and existing tree",
			// null);
			// }
			if (this.possibleTrees.contains(tree)) {
				// don't add
				int i=0;
			} else {
				this.possibleTrees.add(tree);
				this.canGrow |= tree.getCanGrow();
			}
		} else {
			// drop tree
			int i = 0;
		}
		if (null == this.longestMatch
				|| tree.getRoot().getMatchedTextLength() > this.longestMatch.getRoot().getMatchedTextLength()) {
			if (tree.getRoot().getName().equals(ScannerLessParser.START_SYMBOL_TERMINAL.getValue()) || tree.getRoot()
					.getName().contains("$") /* don't use non user rules */) {
				// don't use
			} else {
				this.longestMatch = tree;
			}
		}
	}

	public Forrest shallowClone() {
		Forrest clone = new Forrest(this.goalRRule, this.runtimeRuleSet);
		clone.canGrow = this.canGrow;
		clone.goalTrees.addAll(this.goalTrees);
		clone.possibleTrees.addAll(this.possibleTrees);
		// clone.newGrownBranches.addAll(this.newGrownBranches);
		// clone.possibleSubRule = this.possibleSubRule;
		// clone.possibleTerminal = this.possibleTerminal;
		// clone.allSkipTerminal_cache = this.allSkipTerminal_cache;
		clone.longestMatch = this.longestMatch;
		clone.done = this.done;
		return clone;
	}

	// --- Object ---
	@Override
	public String toString() {
		String s = "Forrest {";
		s += this.possibleTrees.size();
		s += ", " + this.possibleTrees;
		s += "}";
		return s;
	}

	@Override
	public int hashCode() {
		return super.hashCode();
	}

	@Override
	public boolean equals(Object arg) {
		if (arg instanceof Forrest) {
			Forrest other = (Forrest) arg;
			return this.possibleTrees.equals(other.possibleTrees);
		} else {
			return false;
		}
	}
}
