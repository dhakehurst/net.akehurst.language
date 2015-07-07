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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.core.parser.ParseFailedException;
import net.akehurst.language.core.parser.ParseTreeException;
import net.akehurst.language.ogl.semanticModel.Rule;
import net.akehurst.language.ogl.semanticModel.RuleNotFoundException;
import net.akehurst.language.ogl.semanticModel.Terminal;
import net.akehurst.language.parser.ScannerLessParser;
import net.akehurst.language.parser.runtime.RuntimeRule;
import net.akehurst.language.parser.runtime.RuntimeRuleSet;

public class Forrest {

	public Forrest(RuntimeRule goalRRule, RuntimeRuleSet runtimeRuleSet) {
		// this.goal = goal;
		// this.grammar = grammar;
		// this.allTerminals = allTerminals;
		this.goalRRule = goalRRule;
		this.runtimeRuleSet = runtimeRuleSet;
		// this.allRules = allRules;
		// this.input = input;
		this.canGrow = false;
		// this.newGrownBranches = new HashSet<>();
		// this.possibleSubRule = new HashMap<>();
		// this.possibleTerminal = new HashMap<>();
		// this.possibleSuperRule = new HashMap<>();
		this.longestMatch = null;// new ParseTreeBud(factory, input, new
									// EmptyLeaf(0), null);
	}

	RuntimeRule goalRRule;
	RuntimeRuleSet runtimeRuleSet;

	// Grammar grammar;
	// Set<Terminal> allTerminals;
	// Set<Rule> allRules;
	// INodeType goal;
	Input input;
	ArrayList<AbstractParseTree> possibleTrees = new ArrayList<>();
	Set<IParseTree> goalTrees = new HashSet<>();
	ArrayList<IParseTree> gt = new ArrayList<>();
	AbstractParseTree longestMatch;

	// Set<AbstractParseTree> newGrownBranches;

	// Map<Rule, Set<Terminal>> possibleTerminal;
	// Map<Rule, Set<Rule>> possibleSubRule;
	// Map<Rule, Set<Rule>> possibleSuperRule;

	// Set<Rule> getPossibleSuperRule(AbstractParseTree tree) throws
	// RuleNotFoundException {
	// if (tree instanceof ParseTreeBranch) {
	// ParseTreeBranch bTree = (ParseTreeBranch) tree;
	// Rule treeRule = bTree.rule;
	// Set<Rule> result = this.possibleSuperRule.get(treeRule);
	// if (null == result) {
	// result = this.findAllSuperRule(treeRule);
	// this.possibleSuperRule.put(treeRule, result);
	// }
	// return result;
	// } else {
	// return new HashSet<>();
	// }
	// }

	// Set<Rule> findAllSuperRule(Rule rule) throws RuleNotFoundException {
	// Set<Rule> result = new HashSet<>();
	// for (Rule r : this.allRules) {
	// if (r.findAllSubNonTerminal().contains(new NonTerminal(rule.getName())))
	// {
	// result.add(r);
	// }
	// }
	// return result;
	// }

	// Set<Rule> getPossibleSubRule(AbstractParseTree tree) throws
	// RuleNotFoundException {
	// if (tree instanceof ParseTreeBranch) {
	// ParseTreeBranch bTree = (ParseTreeBranch) tree;
	// Rule treeRule = bTree.rule;
	// Set<Rule> result = this.possibleSubRule.get(treeRule);
	// if (null == result) {
	// result = this.findPossibleSubRule(treeRule);
	// this.possibleSubRule.put(treeRule, result);
	// }
	// return result;
	// } else {
	// return new HashSet<>();
	// }
	// }
	//
	// Set<Rule> findPossibleSubRule(Rule rule) throws RuleNotFoundException {
	// Set<Rule> result = rule.findAllSubRule();
	// result.addAll(this.getAllSkipRule());
	// return result;
	// }
	//
	// Set<Terminal> getPossibleSubTerminal(AbstractParseTree tree) throws
	// RuleNotFoundException {
	// if (tree instanceof ParseTreeBranch) {
	// ParseTreeBranch bTree = (ParseTreeBranch) tree;
	// Rule treeRule = bTree.rule;
	// Set<Terminal> result = this.possibleTerminal.get(treeRule);
	// if (null == result) {
	// result = this.findPossibleTerminal(treeRule);
	// this.possibleTerminal.put(treeRule, result);
	// }
	// return result;
	// } else {
	// return new HashSet<>();
	// }
	// }

	// Set<Rule> allSkipRule_cache;
	//
	// Set<Rule> getAllSkipRule() throws RuleNotFoundException {
	// if (null == this.allSkipRule_cache) {
	// Set<Rule> result = new HashSet<>();
	// for (Rule r : this.allRules) {
	// if (r instanceof SkipRule) {
	// result.add(r);
	// }
	// }
	// this.allSkipRule_cache = result;
	// }
	// return this.allSkipRule_cache;
	// }

	// Set<Terminal> findPossibleTerminal(Rule rule) throws
	// RuleNotFoundException {
	// Set<Terminal> result = rule.findAllSubTerminal();
	// result.addAll(this.getAllSkipTerminal());
	// return result;
	// }

	// Set<Terminal> allSkipTerminal_cache;
	//
	// Set<Terminal> getAllSkipTerminal() throws RuleNotFoundException {
	// if (null == this.allSkipTerminal_cache) {
	// Set<Terminal> result = new HashSet<>();
	// for (Rule r : this.getAllSkipRule()) {
	// result.addAll(r.findAllSubTerminal());
	// }
	// this.allSkipTerminal_cache = result;
	// }
	// return this.allSkipTerminal_cache;
	// }

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
//		return this.growDepthFirst();
	}

	public Forrest growBreadthFirst() throws RuleNotFoundException, ParseTreeException {
//		System.out.println("posibles: "+this.possibleTrees.size());
		Forrest newForrest = new Forrest(this.goalRRule, this.runtimeRuleSet);
		newForrest.goalTrees.addAll(this.goalTrees);
		newForrest.longestMatch = this.longestMatch;

		for (AbstractParseTree tree : this.possibleTrees) {
//			System.out.println(tree.getIdString());
			RuntimeRule treeRR = tree.getRoot().getRuntimeRule();
			RuntimeRule[] possibleSubTerminals = this.runtimeRuleSet.getPossibleSubTerminal(treeRR);

			if(tree.getIsComplete()) {
				ArrayList<AbstractParseTree> nts = tree.growHeight(possibleSubTerminals, this.runtimeRuleSet);
				newForrest.addAll(nts);
			}

			if (tree.getCanGraftBack()) {
				AbstractParseTree nt = tree.tryGraftBack();
				if (null != nt) {
					newForrest.add(nt);
				}
			}
			
			if (tree.getCanGrow()) {
				int i = 1;

				if (tree.getIsEmpty() || (tree.getIsComplete() && !tree.getCanGrow())) {
					// don't grow width
				} else {
					ArrayList<AbstractParseTree> newBranches = tree.growWidth(possibleSubTerminals, this.runtimeRuleSet);
					newForrest.addAll(newBranches);
				}
			}

		}

		return newForrest;
	}

	Forrest growDepthFirst() throws RuleNotFoundException, ParseTreeException {
		Forrest newForrest = new Forrest(this.goalRRule, this.runtimeRuleSet);
		newForrest.goalTrees.addAll(this.goalTrees);
		newForrest.longestMatch = this.longestMatch;

		for (AbstractParseTree tree : this.possibleTrees) {
//			System.out.println(tree.getIdString());
			RuntimeRule treeRR = tree.getRoot().getRuntimeRule();
			RuntimeRule[] possibleSubTerminals = this.runtimeRuleSet.getPossibleSubTerminal(treeRR);
			
			AbstractParseTree ntree = tree;
			while(null!=tree && tree.getCanGrow()) {
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
				
				while(tree.getIsComplete()) {
					ArrayList<AbstractParseTree> nts = tree.growHeight(possibleSubTerminals, this.runtimeRuleSet);
					if(!nts.isEmpty()) {
						ntree = nts.get(0);
						tree = ntree;
						for(int i=0;i<nts.size();++i) {
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
						ArrayList<AbstractParseTree> nts = tree.growWidth(possibleSubTerminals, this.runtimeRuleSet);
						if(!nts.isEmpty()) {
							ntree = nts.get(0);
							tree = ntree;
							for(int i=1;i<nts.size();++i) {
								newForrest.add(nts.get(i));
							}
						} else {
							break;
						}
					}
				}
				tree = ntree;
			}
			if(null!=ntree) {
				newForrest.add(ntree);
			}
		}

		return newForrest;
	}

	public void addNonGrowing(AbstractParseTree tree) {
		possibleTrees.add(tree);
	}

	public void addAll(Collection<? extends AbstractParseTree> trees) throws ParseTreeException {
		for (AbstractParseTree tree : trees) {
			this.add(tree);
		}
	}

	int overwrites;

	public void add(AbstractParseTree tree) throws ParseTreeException {
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
				//don't add
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
			if (tree.getRoot().getNodeType().equals(ScannerLessParser.START_SYMBOL_TERMINAL.getNodeType())
					|| tree.getRoot().getName().contains("$") /* don't use non user rules*/) {
				// don't use
			} else {
				this.longestMatch = tree;
			}
		}
	}

	void addIfGoal(AbstractParseTree tree) throws ParseTreeException {
		if (tree.getIsComplete()) {
			if (this.goalRRule.getRuleNumber() == tree.getRoot().getRuntimeRule().getRuleNumber()) {
				goalTrees.add(tree);
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
		return clone;
	}

	// --- Object ---
	@Override
	public String toString() {
		String s = "Forrest {";
		s += "goal==" + this.goalRRule.getGrammarRule().getNodeType();
		s += ", canGrow==" + this.possibleTrees;
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
