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
package net.akehurst.language.parser;

import java.util.ArrayList;
import java.util.List;

import net.akehurst.language.core.parser.INodeType;
import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.ogl.semanticModel.Grammar;
import net.akehurst.language.ogl.semanticModel.Rule;
import net.akehurst.language.ogl.semanticModel.RuleNotFoundException;
import net.akehurst.language.parser.forrest.ParseTreeBranch;
import net.akehurst.language.parser.forrest.SubParseTree;

public 	class Forrest {

	Forrest(INodeType goal, Grammar grammar) {
		this.goal = goal;
		this.grammar = grammar;
	}
	Grammar grammar;
	INodeType goal;
	List<SubParseTree> possibleTrees = new ArrayList<>();
	List<SubParseTree> completeTrees = new ArrayList<>();
	List<IParseTree> goalTrees = new ArrayList<>();
	List<SubParseTree> trees = new ArrayList<>();

	public Forrest clone() {
		Forrest clone = new Forrest(this.goal, this.grammar);
		clone.trees.addAll(this.trees);
		clone.possibleTrees.addAll(this.possibleTrees);
		clone.completeTrees.addAll(this.completeTrees);
		clone.goalTrees.addAll(this.goalTrees);
		return clone;
	}
	
	boolean growAndExpand() throws RuleNotFoundException {
		boolean changed = false;
		Forrest newForrest = new Forrest(this.goal, this.grammar);
		List<SubParseTree> trees1 = new ArrayList<>();
		
		List<SubParseTree> aTrees = this.trees;
		do {
			changed = false;
			List<SubParseTree> newTrees = new ArrayList<>();
			List<SubParseTree> oldTrees = new ArrayList<>();
			for (SubParseTree tree : aTrees) {
				boolean grown = false;
				boolean extended = false;
				if (tree.getIsComplete()) {
					List<Rule> applicableRules = this.grammar.getRule();
					for (Rule applicableRule : applicableRules) {
						try {
							List<IParseTree> newPossible = applicableRule.getRhs().accept(grower, tree);
							if (newPossible.isEmpty()) {
							} else {
								changed = true;
								grown = true;
								for (IParseTree pt : newPossible) {
									SubParseTree npt = (SubParseTree) pt;
									newTrees.add(npt);
								}
							}
						} catch (CannotGrowTreeException e) {
						}
					}
				}
				if (tree.getCanGrow() && tree instanceof ParseTreeBranch) {
					for (SubParseTree t : aTrees) {
						if (t!=tree && t.getIsComplete()) {
							try {
								ParseTreeBranch newPossible = this.tryExtendTree((ParseTreeBranch) tree, t);
								newTrees.add(newPossible);
								if (!t.getIsSkip()) {
									changed = true;
									extended = true;
								}
							} catch (CannotExtendTreeException e) {
								// do nothing, tree not extended
							}
						}
					}
				}
				if (!extended && !grown) {
					oldTrees.add(tree);
				}
			}
			aTrees = newTrees;
			
			for (SubParseTree t : newTrees) {
				trees1.add(t);
			}
			aTrees.addAll(oldTrees);
		} while(changed);
		this.setTrees(trees1);
		return changed;
	}
	
	ParseTreeBranch tryExtendTree(ParseTreeBranch possibleTree, SubParseTree completeTree) throws RuleNotFoundException, CannotExtendTreeException {
		if (possibleTree.getNextExpectedItem().getNodeType().equals(completeTree.getRoot().getNodeType())) {
			return possibleTree.extendWith(completeTree);
		} else if (completeTree.getIsSkip()) {
			return possibleTree.extendWith(completeTree);
		} else {
			throw new CannotExtendTreeException();
		}
	}
	
	void setTrees(List<SubParseTree> value) {
		trees = new ArrayList<>();
		possibleTrees = new ArrayList<>();
		completeTrees = new ArrayList<>();
		for(SubParseTree tree: value) {
			if (tree.getIsComplete() && goal.equals(((SubParseTree) tree).getRoot().getNodeType())) {
				goalTrees.add(tree.deepClone());
			}
			if (tree.getCanGrow()) {
				// tree is incomplete but still possible to grow it
				possibleTrees.add(tree);
				trees.add(tree);
			}
		}
	}
	
	void add(SubParseTree tree) {
		if (tree.getIsComplete()) {
			if (tree.getCanGrow()) { // if complete and can grow, then must clone it as we don't 'grow' a complete tree
				completeTrees.add(tree.deepClone());
				possibleTrees.add(tree);
				trees.add(tree);
			} else {
				completeTrees.add(tree);
				trees.add(tree);
			}
			if (goal.equals(((SubParseTree) tree).getRoot().getNodeType())) {
				goalTrees.add(tree.deepClone());
			}
		} else {
			if (tree.getCanGrow()) {
				// tree is incomplete but still possible to grow it
				possibleTrees.add(tree);
				trees.add(tree);
			} else {
				// drop tree
				int i = 0;
			}
		}
	}
}

