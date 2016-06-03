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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.akehurst.language.core.lexicalAnalyser.IToken;
import net.akehurst.language.core.lexicalAnalyser.ITokenType;
import net.akehurst.language.core.parser.INodeIdentity;
import net.akehurst.language.core.parser.INodeType;
import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.core.parser.IParser;
import net.akehurst.language.core.parser.ParseFailedException;
import net.akehurst.language.ogl.semanticModel.Grammar;
import net.akehurst.language.ogl.semanticModel.Rule;
import net.akehurst.language.ogl.semanticModel.RuleItem;
import net.akehurst.language.ogl.semanticModel.RuleNotFoundException;
import net.akehurst.language.ogl.semanticModel.TangibleItem;
import net.akehurst.language.ogl.semanticModel.Terminal;
import net.akehurst.language.parser.forrest.SubParseTree;

public class Parser implements IParser {

	public Parser(Grammar grammar) {
		this.grammar = grammar;
		this.findTerminal_cache = new HashMap<ITokenType, Terminal>();
	}

	Grammar grammar;

	public Grammar getGrammar() {
		return this.grammar;
	}

	@Override
	public List<INodeType> getNodeTypes() {
		List<INodeType> result = new ArrayList<INodeType>();
		for (Rule r : this.grammar.getRule()) {
			result.add(new INodeType() {
				@Override
				public INodeIdentity getIdentity() {
					return new INodeIdentity() {
						@Override
						public String asPrimitive() {
							return r.getName();
						}
					};
				}
			});
		}
		return result;
	}

	@Override
	public IParseTree parse(INodeType goal, List<IToken> tokens) throws ParseFailedException {
		try {
			return this.doParse(goal, tokens);
		} catch (RuleNotFoundException e) {
			// Should never happen!
			throw new RuntimeException("Should never happen");
		}
	}

	SubParseTree doParse(INodeType goal, List<IToken> tokens) throws ParseFailedException, RuleNotFoundException {
		int start = 0;
		int end = tokens.size();
		int pos = start;

		ParseTreeGrower grower = new ParseTreeGrower();

		List<SubParseTree> possibleTrees = new ArrayList<>();
		List<SubParseTree> completeTrees = new ArrayList<>();
		List<IParseTree> goalTrees = new ArrayList<>();
		while (pos < end) {
			IToken tok = tokens.get(pos);
			SubParseTree newBud = this.createBud(tok);
			completeTrees.add(newBud);

			boolean doneSomthing = false;
			do {
				doneSomthing = false;
				// process possibles
				List<SubParseTree> newPossibleTrees = new ArrayList<>();
				for (SubParseTree possibleTree : possibleTrees) {
					for(SubParseTree completeTree: completeTrees) {
						this.extendTree(possibleTree, completeTree);
					}
					// boolean extendSuccess = possibleTree.extendTree(newBud); // this may mark tree as dead or complete
					if (possibleTree.isComplete()) {
						completeTrees.add(possibleTree);
						if (goal.equals(((SubParseTree) possibleTree).getRoot().getNodeType())) {
							goalTrees.add(possibleTree);
						}
					} else if (possibleTree.isDead()) {
						//TODO implement when trees become dead...i.e. wrong token in a concatination
						// do nothing tree is dead
					} else {
						// tree must be incomplete still, so it is still possible
						newPossibleTrees.add(possibleTree);
					}
				}
				possibleTrees = newPossibleTrees;
				// process complete trees
				// TODO check if goal reached
				List<SubParseTree> newCompleteTrees = new ArrayList<>();
				for (SubParseTree completeTree : completeTrees) {
					List<Rule> applicableRules = this.grammar.getRule(); // this.findRulesThatStart(completeTree);
					if (applicableRules.isEmpty()) {
						// this completeTree must be dead (if goal not reached)
					} else {
						for (Rule applicableRule : applicableRules) {
							try {
								List<IParseTree> newPossible = applicableRule.getRhs().accept(grower, completeTree); // this.createNewPossibleBranches(completeTree,
																														// applicableRule);
								for(IParseTree npt:newPossible) {
									if (((SubParseTree) npt).isComplete()) {
										newCompleteTrees.add((SubParseTree) npt);
										if (goal.equals(((SubParseTree) npt).getRoot().getNodeType())) {
											goalTrees.add(npt);
										}
									} else {
										possibleTrees.add((SubParseTree) npt);
									}
									doneSomthing = true;
								}
							} catch (CannotGrowTreeException e) {

							}
						}
					}
				}
				completeTrees = newCompleteTrees;
			} while (doneSomthing);
			++pos;
		}

		if (!goalTrees.isEmpty() && goalTrees.size()==1) {
					return (SubParseTree)goalTrees.get(0);
		}

		throw new ParseFailedException();
	}

	private void extendTree(SubParseTree possibleTree, SubParseTree completeTree) throws RuleNotFoundException {
		if (possibleTree.getNextExpectedItem().getNodeType().equals(completeTree.getRoot().getNodeType())) {
			possibleTree.extendWith(completeTree);
		}
	}

	
	SubParseTree createBud(IToken tok) throws RuleNotFoundException {
		INodeType nodeType = this.findTerminal(tok).getNodeType();
		ParseTreeBud tree = new ParseTreeBud(nodeType, tok.getText());
		return tree;
	}


	Map<ITokenType, Terminal> findTerminal_cache;

	Terminal findTerminal(IToken token) {
		Terminal terminal = this.findTerminal_cache.get(token.getType());
		if (null == terminal) {
			for (Terminal term : this.getGrammar().getAllTerminal()) {
				if ((!token.getType().getIsRegEx() && term.getValue().equals(token.getType().getIdentity()))
						|| (token.getType().getIsRegEx() && term.getValue().equals(token.getType().getPatternString()))) {
					this.findTerminal_cache.put(token.getType(), term);
					return term;
				}
			}
		}
		return terminal;
	}

	// List<IParseTree> createNewPossibleBranches(SubParseTree oldBranch, Rule rule) throws RuleNotFoundException {
	// INodeType nodeType = rule.getNodeType();
	// RuleItem item = oldBranch.getRuleItem();
	// FirstParseTreesFinder f = new FirstParseTreesFinder();
	// List<ParseTree> trees = f.accept(rule.getRhs(), oldBranch);
	// ParseTreeBranch tree = new ParseTreeBranch(rule, oldBranch);
	// return tree;
	// }

	// List<Rule> findRulesThatStart(SubParseTree tree) throws RuleNotFoundException {
	// List<Rule> result = new ArrayList<>();
	// for (Rule rule : this.getGrammar().getRule()) {
	// if (this.ruleStartWith(tree.getRoot().getNodeType(), rule)) {
	// result.add(rule);
	// }
	// }
	// return result;
	// }

	// boolean ruleStartWith(INodeType nodeType, Rule rule) throws RuleNotFoundException {
	// for (TangibleItem ti : rule.findFirstTangibleItem()) {
	// if (nodeType.equals(ti.getNodeType())) {
	// return true;
	// }
	// }
	// return false;
	// }

}
