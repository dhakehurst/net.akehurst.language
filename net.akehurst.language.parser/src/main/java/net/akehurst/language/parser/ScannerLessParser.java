package net.akehurst.language.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;

import net.akehurst.language.core.lexicalAnalyser.IToken;
import net.akehurst.language.core.lexicalAnalyser.ITokenType;
import net.akehurst.language.core.parser.INodeIdentity;
import net.akehurst.language.core.parser.INodeType;
import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.core.parser.IParser;
import net.akehurst.language.core.parser.ParseFailedException;
import net.akehurst.language.core.parser.ParseTreeException;
import net.akehurst.language.ogl.semanticModel.Grammar;
import net.akehurst.language.ogl.semanticModel.Rule;
import net.akehurst.language.ogl.semanticModel.RuleItem;
import net.akehurst.language.ogl.semanticModel.RuleNotFoundException;
import net.akehurst.language.ogl.semanticModel.SkipNodeType;
import net.akehurst.language.ogl.semanticModel.TangibleItem;
import net.akehurst.language.ogl.semanticModel.Terminal;

public class ScannerLessParser implements IParser {

	public ScannerLessParser(Grammar grammar) {
		this.grammar = grammar;
		this.findTerminal_cache = new HashMap<ITokenType, Terminal>();
		this.buddingTerminals = new HashMap<>();
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
	public IParseTree parse(INodeType goal, CharSequence text) throws ParseFailedException, ParseTreeException {
		try {
			return this.doParse2(goal, text);
		} catch (RuleNotFoundException e) {
			// Should never happen!
			throw new RuntimeException("Should never happen");
		}
	}

	/**
	 * <code>
	 * starting tree is an empty node
	 * while(something can grow) { //i.e. a possible tree has not reached the end of the input text
	 *   for all trees in forrest
	 *     try and grow the tree
	 *       pos = length of tree match
	 *       grab all possible next tokens from pos forwards
	 *       for each possible next token
	 *         try and grow the tree up or extend outwards
	 * }
	 * 
	 * find the longest match of the goal
	 * </code>
	 * 
	 * @param goal
	 * @param text
	 * @return
	 * @throws ParseFailedException
	 * @throws RuleNotFoundException
	 * @throws ParseTreeException 
	 */
	IParseTree doParse2(INodeType goal, CharSequence text) throws ParseFailedException, RuleNotFoundException, ParseTreeException {

		Forrest2 forrest = new Forrest2(goal, this.grammar);
		forrest.canGrow = true;
		forrest.add(new ParseTreeStartBud(text.length()));
		while (forrest.getCanGrow()) {
			forrest = forrest.grow(text);
		}

		IParseTree match = forrest.getLongestMatch(text);

		return match;
	}

	IParseTree doParse(INodeType goal, CharSequence text) throws ParseFailedException, RuleNotFoundException {
		int start = 0;
		int end = text.length();
		int pos = start;

		Forrest forrest = new Forrest(goal, this.grammar);
		forrest.add(new ParseTreeEmptyBud(text.length()));
		List<ParseTreeBud2> buddingTrees = new ArrayList<>();

		while (pos < end) {
			Character c = text.charAt(pos);
			for (ParseTreeBud2 bud : buddingTrees) {
				this.extendTree(bud, c);
			}

			// TODO: this must grab a 'full' token ?
			List<ParseTreeBud2> newBuds = this.createBuds(c);
			List<ParseTreeBud2> newBudding = new ArrayList<>();
			buddingTrees.addAll(newBuds);
			for (ParseTreeBud2 bud : buddingTrees) {
				if (bud.getIsComplete()) {
					forrest.add(bud);
				}
				if (bud.getCanGrow()) {
					newBudding.add(bud);
				}
			}
			buddingTrees = newBudding;

			forrest.growAndExpand();

			++pos;
		}

		if (!forrest.goalTrees.isEmpty() && forrest.goalTrees.size() >= 1) {
			IParseTree lt = (SubParseTree) forrest.goalTrees.get(0);
			for (IParseTree gt : forrest.goalTrees) {
				if (gt.getRoot().getLength() > lt.getRoot().getLength()) {
					lt = gt;
				}
			}
			if (lt.getRoot().getLength() < text.length()) {
				throw new ParseFailedException("Goal does not match full text");
			} else {
				return lt;
			}
		} else {
			throw new ParseFailedException("Could not match goal");
		}

	}

	boolean growCompleteTrees(Forrest forrest) {
		boolean grownTree = false;
		List<SubParseTree> completeTrees = forrest.completeTrees;
		while (!completeTrees.isEmpty()) {
			List<SubParseTree> newCompleteTrees = new ArrayList<>();
			for (SubParseTree completeTree : completeTrees) {
				List<Rule> applicableRules = this.grammar.getRule();
				for (Rule applicableRule : applicableRules) {
					try {
						List<IParseTree> newPossible = applicableRule.getRhs().accept(grower, completeTree);
						if (newPossible.isEmpty()) {
						} else {
							grownTree = true;
							for (IParseTree pt : newPossible) {
								SubParseTree npt = (SubParseTree) pt;
								newCompleteTrees.add(npt);
							}
						}
					} catch (CannotGrowTreeException e) {
					}
				}
			}
			completeTrees = newCompleteTrees;
			for (SubParseTree t : newCompleteTrees) {
				forrest.add(t);
			}
		}
		return grownTree;
	}

	boolean extendPossibleTrees(Forrest forrest) throws RuleNotFoundException {
		boolean extendedTree = false;
		List<SubParseTree> possibleTrees = forrest.possibleTrees;
		while (!possibleTrees.isEmpty()) {
			List<SubParseTree> newPossibleTrees = new ArrayList<>();
			for (SubParseTree possibleTree : possibleTrees) {
				for (SubParseTree completeTree : forrest.completeTrees) {
					// TODO: this need to mark trees as notAble to grow! ?maybe done
					try {
						ParseTreeBranch newPossible = this.tryExtendTree((ParseTreeBranch) possibleTree, completeTree);
						newPossibleTrees.add(newPossible);
						extendedTree = true;
					} catch (CannotExtendTreeException e) {
						// do nothing, tree not extended
					}
				}
				// newPossibleTrees.add(possibleTree);
			}
			possibleTrees = newPossibleTrees;
			for (SubParseTree possibleTree : newPossibleTrees) {
				forrest.add(possibleTree);
			}
		}
		return extendedTree;
	}

	private ParseTreeBranch tryExtendTree(ParseTreeBranch possibleTree, SubParseTree completeTree) throws RuleNotFoundException, CannotExtendTreeException {
		if (possibleTree.getNextExpectedItem().getNodeType().equals(completeTree.getRoot().getNodeType())) {
			return possibleTree.extendWith(completeTree);
		} else if (this.isSkipNode(completeTree.getRoot().getNodeType())) {
			return possibleTree.extendWith(completeTree);
		} else {
			throw new CannotExtendTreeException();
		}
	}

	private void extendTree(ParseTreeBud2 possibleTree, Character character) throws RuleNotFoundException {
		possibleTree.tryExtendWith(character);
	}

	boolean isSkipNode(INodeType nodeType) {
		return nodeType instanceof SkipNodeType;
	}

	Map<Character, List<Terminal>> buddingTerminals;

	List<ParseTreeBud2> createBuds(Character c) {
		List<Terminal> terms = this.buddingTerminals.get(c);
		if (null == terms) {
			terms = new ArrayList<>();
			Set<Terminal> allTerms = this.getGrammar().getAllTerminal();
			for (Terminal t : allTerms) {
				if (this.acceptsAsFirst(t, c)) {
					terms.add(t);
				}
			}
			this.buddingTerminals.put(c, terms);
		}

		List<ParseTreeBud2> buds = new ArrayList<>();
		for (Terminal t : terms) {
			ParseTreeBud2 bud = new ParseTreeBud2(t, c);
			buds.add(bud);
		}
		return buds;
	}

	boolean acceptsAsFirst(Terminal t, Character c) {
		Matcher m = t.getPattern().matcher(c.toString());
		boolean isComplete = m.matches();
		boolean isPossible = m.hitEnd();
		return isComplete || isPossible;
	}

	SubParseTree createBud(Terminal terminal, Character c) throws RuleNotFoundException {
		ParseTreeBud2 tree = new ParseTreeBud2(terminal, c);
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
