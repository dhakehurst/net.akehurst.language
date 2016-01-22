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
package net.akehurst.language.grammar.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.akehurst.language.core.parser.IBranch;
import net.akehurst.language.core.parser.INode;
import net.akehurst.language.core.parser.INodeIdentity;
import net.akehurst.language.core.parser.INodeType;
import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.core.parser.IParser;
import net.akehurst.language.core.parser.ParseFailedException;
import net.akehurst.language.core.parser.ParseTreeException;
import net.akehurst.language.grammar.parser.converter.Converter;
import net.akehurst.language.grammar.parser.converter.Grammar2RuntimeRuleSet;
import net.akehurst.language.grammar.parser.forrest.AbstractParseTree;
import net.akehurst.language.grammar.parser.forrest.Forrest;
import net.akehurst.language.grammar.parser.forrest.ForrestFactory;
import net.akehurst.language.grammar.parser.forrest.ParseTreeBranch;
import net.akehurst.language.grammar.parser.forrest.ParseTreeBud;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleSet;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleSetBuilder;
import net.akehurst.language.ogl.semanticStructure.ChoiceSimple;
import net.akehurst.language.ogl.semanticStructure.Concatenation;
import net.akehurst.language.ogl.semanticStructure.Grammar;
import net.akehurst.language.ogl.semanticStructure.Namespace;
import net.akehurst.language.ogl.semanticStructure.NonTerminal;
import net.akehurst.language.ogl.semanticStructure.Rule;
import net.akehurst.language.ogl.semanticStructure.RuleNotFoundException;
import net.akehurst.language.ogl.semanticStructure.TerminalLiteral;

public class ScannerLessParser implements IParser {

	public final static String START_SYMBOL = "\uE000";
	public final static TerminalLiteral START_SYMBOL_TERMINAL = new TerminalLiteral(START_SYMBOL);
	public final static String FINISH_SYMBOL = "\uE001";
	public final static TerminalLiteral FINISH_SYMBOL_TERMINAL = new TerminalLiteral(FINISH_SYMBOL);

	public ScannerLessParser(RuntimeRuleSetBuilder runtimeFactory, Grammar grammar) {
		this.grammar = grammar;
		// this.findTerminal_cache = new HashMap<ITokenType, Terminal>();
		this.runtimeBuilder = runtimeFactory;
		this.converter = new Converter(this.runtimeBuilder);
	}

	Converter converter;
	RuntimeRuleSetBuilder runtimeBuilder;
	Grammar grammar;
	Grammar pseudoGrammar;

	Grammar getGrammar() {
		return this.grammar;
	}

	RuntimeRuleSet runtimeRuleSet;

	RuntimeRuleSet getRuntimeRuleSet() {
		return this.runtimeRuleSet;
	}

	// public void build(String goalRuleName) throws RuleNotFoundException {
	// INodeType goalNodeType =
	// this.getGrammar().findRule(goalRuleName).getNodeType();
	// this.build(goalNodeType);
	// }

	public void build(INodeType goalNodeType) {
		this.createPseudoGrammar(goalNodeType);
		this.runtimeRuleSet.build();
	}

	RuntimeRule createPseudoGrammar(INodeType goal) {
		try {
			this.pseudoGrammar = new Grammar(new Namespace(grammar.getNamespace().getQualifiedName() + "::pseudo"),
					"Pseudo");
			this.pseudoGrammar.setExtends(Arrays.asList(new Grammar[] { this.grammar }));
			Rule goalRule = new Rule(this.pseudoGrammar, "$goal$");
			goalRule.setRhs(
					new ChoiceSimple(
							new Concatenation(
									new TerminalLiteral(START_SYMBOL),
									new NonTerminal(goal.getIdentity().asPrimitive()),
									new TerminalLiteral(FINISH_SYMBOL)
							)
					)
			);
			this.pseudoGrammar.getRule().add(goalRule);
			this.runtimeRuleSet = this.converter.transformLeft2Right(Grammar2RuntimeRuleSet.class, this.pseudoGrammar);
			int pseudoGoalNumber = this.runtimeRuleSet.getRuleNumber(goalRule.getName());
			RuntimeRule pseudoGoalRR = this.runtimeRuleSet.getRuntimeRule(pseudoGoalNumber);
			//
			// this.allRules_cache = null;
			// this.getAllRules();
			// this.allRules_cache.add(goalRule);
			return pseudoGoalRR;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	// Rule findRule(String ruleName) throws RuleNotFoundException {
	// for(Rule r : this.getAllRules()) {
	// if (r.getName().equals(ruleName)) {
	// return r;
	// }
	// }
	// throw new RuleNotFoundException(ruleName);
	// }

	// Set<Terminal> allTerminal;
	// public Set<Terminal> getAllTerminal() {
	// if (null==this.allTerminal) {
	// this.allTerminal = this.findAllTerminal();
	// this.allTerminal.add(START_SYMBOL_TERMINAL);
	// this.allTerminal.add(FINISH_SYMBOL_TERMINAL);
	//
	// }
	// return this.allTerminal;
	// }
	// Set<Terminal> findAllTerminal() {
	// Set<Terminal> result = new HashSet<>();
	// for (Rule rule : this.getAllRules()) {
	// RuleItem ri = rule.getRhs();
	// result.addAll(this.findAllTerminal(0, rule, ri ) );
	// }
	// return result;
	// }

	// Set<Terminal> findAllTerminal(final int totalItems, final Rule rule,
	// RuleItem item) {
	// Set<Terminal> result = new HashSet<>();
	// if (item instanceof Terminal) {
	// Terminal t = (Terminal) item;
	// result.add(t);
	// } else if (item instanceof Multi) {
	// result.addAll( this.findAllTerminal( totalItems, rule,
	// ((Multi)item).getItem() ) );
	// } else if (item instanceof Choice) {
	// for(TangibleItem ti : ((Choice)item).getAlternative()) {
	// result.addAll( this.findAllTerminal( totalItems, rule, ti ) );
	// }
	// } else if (item instanceof Concatenation) {
	// for(TangibleItem ti : ((Concatenation)item).getItem()) {
	// result.addAll( this.findAllTerminal( totalItems, rule, ti ) );
	// }
	// } else if (item instanceof SeparatedList) {
	// result.addAll(this.findAllTerminal(totalItems,
	// rule,((SeparatedList)item).getSeparator()));
	// result.addAll( this.findAllTerminal(totalItems, rule,
	// ((SeparatedList)item).getConcatination() ) );
	// }
	// return result;
	// }

	@Override
	public List<INodeType> getNodeTypes() {
		List<INodeType> result = new ArrayList<INodeType>();
		for (Rule r : this.grammar.getAllRule()) {
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
			// return this.doParse2(goal, text);
			IParseTree tree = this.parse2(goal, text);
			// set the parent property of each child, these are not set during parsing
			this.setParentForChildren((IBranch)tree.getRoot());
			return tree;
		} catch (RuleNotFoundException e) {
			// Should never happen!
			throw new RuntimeException("Should never happen", e);
		}
	}

	void setParentForChildren(IBranch node) {
		IBranch parent = (IBranch) node;
		for (INode child : parent.getChildren()) {
			child.setParent(parent);
			if (child instanceof IBranch) {
				this.setParentForChildren((IBranch) child);
			}
		}
	}

	public IParseTree parse2(INodeType goal, CharSequence text)
			throws ParseFailedException, RuleNotFoundException, ParseTreeException {
		RuntimeRule pseudoGoalRule = this.createPseudoGrammar(goal);
		Rule goalRule = this.grammar.findAllRule(goal.getIdentity().asPrimitive());
		int goalRuleNumber = this.runtimeRuleSet.getRuleNumber(goal.getIdentity().asPrimitive());
		RuntimeRule goalRR = this.runtimeRuleSet.getRuntimeRule(goalRuleNumber);
		CharSequence pseudoText = START_SYMBOL + text + FINISH_SYMBOL;

		ParseTreeBranch pseudoTree = (ParseTreeBranch) this.doParse2(pseudoGoalRule, pseudoText);
		// return pseudoTree;
		// Rule r = this.findRule(goal.getIdentity().asPrimitive());
		ForrestFactory ffactory = new ForrestFactory(this.runtimeBuilder, text);
		int s = pseudoTree.getRoot().getChildren().size();
		IBranch root = (IBranch) pseudoTree.getRoot().getChildren().stream()
				.filter(n -> n.getName().equals(goal.getIdentity().asPrimitive())).findFirst().get();
		int indexOfRoot = pseudoTree.getRoot().getChildren().indexOf(root);
		List<INode> before = pseudoTree.getRoot().getChildren().subList(1, indexOfRoot);
		ArrayList<INode> children = new ArrayList<>();
		List<INode> after = pseudoTree.getRoot().getChildren().subList(indexOfRoot + 1, s - 1);
		children.addAll(before);
		children.addAll(root.getChildren());
		children.addAll(after);
		// Branch nb = (Branch)this.factory.createBranch(goalRR,
		// children.toArray(new INode[children.size()]));
		// ParseTreeBranch pt = new ParseTreeBranch(this.factory, inp, nb, null,
		// goalRR, Integer.MAX_VALUE);
		ParseTreeBranch pt = ffactory.fetchOrCreateBranch(goalRR, children.toArray(new INode[children.size()]), null,
				Integer.MAX_VALUE);
		return pt;
	}

	int numberOfSeasons;

	/**
	 * <code>
	 * starting tree is an empty node
	 * while(something can grow) { //i.e. a possible tree has not reached the end of the input text
	 *   for all trees in forrest
	 *     try and grow the tree
	
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
	IParseTree doParse2(RuntimeRule pseudoGoalRule, CharSequence text)
			throws ParseFailedException, RuleNotFoundException, ParseTreeException {
		ForrestFactory ff = new ForrestFactory(this.runtimeBuilder, text);

		Forrest newForrest = new Forrest(pseudoGoalRule, this.getRuntimeRuleSet());
		Forrest oldForrest = null;

		RuntimeRule sst = this.getRuntimeRuleSet().getForTerminal(START_SYMBOL_TERMINAL.getValue());
		ParseTreeBud startBud = ff.createNewBuds(new RuntimeRule[] { sst }, 0).get(0);
		RuntimeRule[] terminalRules = runtimeRuleSet.getPossibleSubTerminal(sst);

		ArrayList<AbstractParseTree> newTrees = startBud.growHeight(this.runtimeRuleSet);
		newForrest.addAll(newTrees);

		int max = 0;
		while (newForrest.getCanGrow()) {
			++numberOfSeasons;
			// System.out.println(this.numberOfSeasons);
			oldForrest = newForrest.shallowClone();
			newForrest = oldForrest.grow();
			max = Math.max(max, newForrest.size());
		}
		// System.out.println(this.numberOfSeasons);
		System.out.println("Max "+max);
		IParseTree match = newForrest.getLongestMatch(text);
		// System.out.println(((ParseTreeBranch)match).getIdString());

		return match;
	}

	// Map<ITokenType, Terminal> findTerminal_cache;
	//
	// Terminal findTerminal(IToken token) {
	// Terminal terminal = this.findTerminal_cache.get(token.getType());
	// if (null == terminal) {
	// for (Terminal term : this.getAllTerminal()) {
	// if ((!token.getType().getIsRegEx() &&
	// term.getValue().equals(token.getType().getIdentity()))
	// || (token.getType().getIsRegEx() &&
	// term.getValue().equals(token.getType().getPatternString()))) {
	// this.findTerminal_cache.put(token.getType(), term);
	// return term;
	// }
	// }
	// }
	// return terminal;
	// }

}
