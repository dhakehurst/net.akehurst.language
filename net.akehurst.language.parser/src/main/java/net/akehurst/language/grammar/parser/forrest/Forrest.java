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

import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.akehurst.language.core.parser.IBranch;
import net.akehurst.language.core.parser.INode;
import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.core.parser.ParseFailedException;
import net.akehurst.language.core.parser.ParseTreeException;
import net.akehurst.language.core.parser.RuleNotFoundException;
import net.akehurst.language.grammar.parse.tree.Branch;
import net.akehurst.language.grammar.parser.ScannerLessParser;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleSet;

public class Forrest {

	public Forrest(RuntimeRule goalRRule, RuntimeRuleSet runtimeRuleSet) {
		this.goalRRule = goalRRule;
		this.runtimeRuleSet = runtimeRuleSet;
		this.canGrow = false;
		this.longestMatch = null;
		this.containedTrees = new HashMap<>();
		this.fails = new HashSet<>();
	}

	RuntimeRule goalRRule;
	RuntimeRuleSet runtimeRuleSet;


	Input input;
//	ArrayList<AbstractParseTree> possibleTrees = new ArrayList<>();
	Map<NodeIdentifier, AbstractParseTree> possTrees2 = new HashMap<>();
	Set<IParseTree> goalTrees = new HashSet<>();
	ArrayList<IParseTree> gt = new ArrayList<>();
	AbstractParseTree longestMatch;

	Map<NodeIdentifier, AbstractParseTree> containedTrees;
	public Set<NodeIdentifier> fails;

	public int size() {
//		return this.possibleTrees.size();
		return this.possTrees2.size();
	}
	
	public AbstractParseTree[] getPossibles() {
		return this.possTrees2.values().toArray(new AbstractParseTree[this.possTrees2.size()]);
	}
	
	Forrest newForrest() {
		Forrest newForrest = new Forrest(this.goalRRule, this.runtimeRuleSet);
		newForrest.goalTrees.addAll(this.goalTrees);
		newForrest.longestMatch = this.longestMatch;
		newForrest.fails = this.fails;
//		newForrest.done = this.done;
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

	AbstractParseTree extractLongestMatch() {
		if (this.longestMatch.getRuntimeRule().equals(this.goalRRule)) {
			
			RuntimeRule target = null;
			IBranch root = (IBranch) this.longestMatch.getRoot();
			if (root.getChildren().size() <=1) {
				return null;
			} else {
			Branch nr =(Branch) root.getChildren().get(1);
			ParseTreeBranch lm = new ParseTreeBranch(this.longestMatch.ffactory, nr, null, nr.getRuntimeRule(), -1);
			return lm;
			}
		} else {
			return this.longestMatch;
		}
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
				throw new ParseFailedException("Goal does not match full text", this.extractLongestMatch());
			} else {
				return lt;
			}
		} else {
			throw new ParseFailedException("Could not match goal", this.extractLongestMatch());
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
		// System.out.println("posibles: "+this.possibleTrees.size());
		Forrest newForrest = this.newForrest();
//		for (AbstractParseTree tree : this.possibleTrees) {
		for (AbstractParseTree tree : this.possTrees2.values()) {
//			ArrayList<AbstractParseTree> newBranches = tree.growWidthAndHeightUntilProgress(this.runtimeRuleSet);
//			ArrayList<AbstractParseTree> newBranches = tree.growWidthAndHeight(this.runtimeRuleSet);
			ArrayList<AbstractParseTree> newBranches = this.growTreeWidthAndHeight(tree);
			newForrest.addAll(newBranches);
		}

		return newForrest;
	}
	
	public ArrayList<AbstractParseTree> growTreeWidthAndHeight(AbstractParseTree tree) throws RuleNotFoundException, ParseTreeException {
		ArrayList<AbstractParseTree> result = new ArrayList<AbstractParseTree>();

		ArrayList<AbstractParseTree> newSkipBranches = tree.growWidthWithSkipRules(runtimeRuleSet);
		if (!newSkipBranches.isEmpty()) {
			result.addAll(newSkipBranches);
		} else {
		
		if (tree.getIsSkip()) {
			ArrayList<AbstractParseTree> nts = tree.tryGraftBack();
//			if (nts.isEmpty() && !tree.getHasGoalRoot(this.runtimeRuleSet)) {
//				this.fails.add(tree.identifier);
//			} else {
				for (AbstractParseTree nt : nts) {
					if (nt.getHasPotential(runtimeRuleSet)) {
						result.add(nt);
					} else {
						// drop it
					}
				}
//			}
		} else {

			if (tree.getIsComplete()) {
				ArrayList<AbstractParseTree> nts = tree.growHeight(runtimeRuleSet);
//				if (nts.isEmpty() && !tree.getHasGoalRoot(this.runtimeRuleSet)) {
//					this.fails.add(tree.identifier);
//				} else {
					for (AbstractParseTree nt : nts) {
						if (nt.getHasPotential(runtimeRuleSet)) {
							result.add(nt);
						} else {
							// drop it
						}
					}
//				}
			}

			if (tree.getCanGraftBack()) {
				ArrayList<AbstractParseTree> nts = tree.tryGraftBack();
				for (AbstractParseTree nt : nts) {
					if (nt.getHasPotential(runtimeRuleSet) || nt.getIsGoal(runtimeRuleSet) ) {
						result.add(nt);
					} else {
						// drop it
					}
				}
			}

			if (tree.getCanGrowWidth()) {
				int i = 1;

				// if (tree.getIsEmpty() || (tree.getIsComplete() &&
				// !tree.getCanGrow())) {
				if ((tree.getIsComplete() && !tree.getCanGrowWidth())) {
					// don't grow width
					// this never happens!
				} else {
					ArrayList<AbstractParseTree> newBranches = tree.growWidth(runtimeRuleSet);
					for (AbstractParseTree nt : newBranches) {
						if (nt.getHasPotential(runtimeRuleSet)) {
							result.add(nt);
						} else {
							// drop it
						}
					}
					
					
				}
			}
		}
		}
		return result;
	}
/*
	Forrest growDepthFirst() throws RuleNotFoundException, ParseTreeException {
		Forrest newForrest = new Forrest(this.goalRRule, this.runtimeRuleSet);
		newForrest.goalTrees.addAll(this.goalTrees);
		newForrest.longestMatch = this.longestMatch;

//		for (AbstractParseTree tree : this.possibleTrees) {
		for (AbstractParseTree tree : this.possTrees2.values()) {
			// System.out.println(tree.getIdString());
			RuntimeRule treeRR = tree.getRuntimeRule();

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
*/
	public void addAll(Collection<? extends AbstractParseTree> trees) throws ParseTreeException {
		for (AbstractParseTree tree : trees) {
			this.add(tree);
		}
	}

	int overwrites;

	void mergeContainedTree(AbstractParseTree tree) {
		AbstractParseTree old = this.containedTrees.get(tree.identifier);
		if (null==old) {
			this.containedTrees.put(tree.identifier, tree);
			AbstractParseTree st = tree.stackedTree;
			if(null==st) {
				//do nothing
			} else {
				this.mergeContainedTree(st);
			}
		} else {
			if (tree == old) {
				//don't add self
			} else {
				old.duplicateRoots.add(tree);
			}
		}

	}
	
	public void add(AbstractParseTree tree) throws ParseTreeException {
		//To keep all possible parses, if already done (rule,start,end) then merge stackedTrees
//		AbstractParseTree t = tree;
//		while (null!=t) {
//			if (this.fails.contains(t.identifier)) {
//				return;
//			}
//			t = t.stackedTree;
//		}
		if (tree.getHasPotential(this.runtimeRuleSet)) {
//			AbstractParseTree old = this.containedTrees.get(tree.identifier);
//			this.mergeContainedTree(tree);
			this.canGrow |= tree.getCanGrow();
//			if (null==old) {
				AbstractParseTree poss = this.possTrees2.get(tree.identifier);
				if (null==poss) {
					this.possTrees2.put(tree.identifier, tree);
				} else {
					poss.duplicateRoots.add(tree); //prob no need to do this as it is added in the merge?
				}
//			} else {
				//has been merged into existing tree
//			}
		} else {
			//drop no potential
		}
		if (tree.getIsComplete()) {
			// this.newGrownBranches.add(tree);
			if (!tree.getIsStacked()
					&& this.goalRRule.getRuleNumber() == tree.getRuntimeRule().getRuleNumber()) {
				this.goalTrees.add(tree);
			}
		}
//		if (tree.getHasPotential(this.runtimeRuleSet)) {
//			// tree is incomplete but still possible to grow it
//			// if (this.possibleTrees.contains(tree)) {
//			// overwrites++;
//			// // throw new ParseTreeException("Overwriting and existing tree",
//			// null);
//			// }
//			if (this.possTrees2.containsKey(tree.identifier)) {
////				if (tree.getIsComplete()) {
////				this.possTrees2.get(tree.identifier).duplicateRoots.add(tree);
////				} else {
//					int i=0;
////				}
//			} else {
//				this.possTrees2.put(tree.identifier, tree);
//			}
//			this.canGrow |= tree.getCanGrow();
//			
////			if (this.possibleTrees.contains(tree)) {
////				// don't add
////				int i=0;
////			} else {
////				this.possibleTrees.add(tree);
////				this.canGrow |= tree.getCanGrow();
////			}
//		} else {
//			// drop tree
//			int i = 0;
//		}
		if (null == this.longestMatch
				|| tree.getRoot().getMatchedTextLength() > this.longestMatch.getRoot().getMatchedTextLength()) {
//			if (tree.getRoot().getName().equals(ScannerLessParser.START_SYMBOL_TERMINAL.getValue()) || tree.getRoot()
//					.getName().contains("$") /* don't use non user rules */) {
//				// don't use
//			} else {
				this.longestMatch = tree;
//			}
		}
	}

	public Forrest shallowClone() {
		Forrest clone = new Forrest(this.goalRRule, this.runtimeRuleSet);
		clone.canGrow = this.canGrow;
		clone.goalTrees.addAll(this.goalTrees);
//		clone.possibleTrees.addAll(this.possibleTrees);
		clone.possTrees2 = new HashMap<>(this.possTrees2);
		// clone.newGrownBranches.addAll(this.newGrownBranches);
		// clone.possibleSubRule = this.possibleSubRule;
		// clone.possibleTerminal = this.possibleTerminal;
		// clone.allSkipTerminal_cache = this.allSkipTerminal_cache;
		clone.longestMatch = this.longestMatch;
		clone.containedTrees = this.containedTrees;
		clone.fails = this.fails;
		return clone;
	}

	// --- Object ---
	@Override
	public String toString() {
		String s = "Forrest {";
//		s += this.possibleTrees.size();
//		s += ", " + this.possibleTrees;
		s += this.possTrees2.size();
		s += ", " + this.possTrees2.values();
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
//			return this.possibleTrees.equals(other.possibleTrees);
			return this.possTrees2.equals(other.possTrees2);
		} else {
			return false;
		}
	}
}
