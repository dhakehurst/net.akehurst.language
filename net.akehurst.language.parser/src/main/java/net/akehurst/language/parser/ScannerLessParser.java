package net.akehurst.language.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import net.akehurst.language.core.lexicalAnalyser.IToken;
import net.akehurst.language.core.lexicalAnalyser.ITokenType;
import net.akehurst.language.core.parser.IBranch;
import net.akehurst.language.core.parser.INode;
import net.akehurst.language.core.parser.INodeIdentity;
import net.akehurst.language.core.parser.INodeType;
import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.core.parser.IParser;
import net.akehurst.language.core.parser.ParseFailedException;
import net.akehurst.language.core.parser.ParseTreeException;
import net.akehurst.language.ogl.semanticModel.Concatination;
import net.akehurst.language.ogl.semanticModel.Grammar;
import net.akehurst.language.ogl.semanticModel.Namespace;
import net.akehurst.language.ogl.semanticModel.NonTerminal;
import net.akehurst.language.ogl.semanticModel.Rule;
import net.akehurst.language.ogl.semanticModel.RuleNotFoundException;
import net.akehurst.language.ogl.semanticModel.SkipRule;
import net.akehurst.language.ogl.semanticModel.Terminal;
import net.akehurst.language.ogl.semanticModel.TerminalLiteral;
import net.akehurst.language.parser.forrest.AbstractParseTree;
import net.akehurst.language.parser.forrest.Branch;
import net.akehurst.language.parser.forrest.Forrest;
import net.akehurst.language.parser.forrest.Input;
import net.akehurst.language.parser.forrest.ParseTreeBranch;
import net.akehurst.language.parser.forrest.ParseTreeBud;

public class ScannerLessParser implements IParser {

	public final static String START_SYMBOL = "\uE000";
	public final static String FINISH_SYMBOL = "\uE001";
	
	
	public ScannerLessParser(Grammar grammar) {
		this.grammar = grammar;
		this.pseudoGrammar = new Grammar(new Namespace(grammar.getNamespace().getQualifiedName()+"::pseudo"), "Pseudo");
		this.pseudoGrammar.setExtends(Arrays.asList(new Grammar[]{this.grammar}));
		this.findTerminal_cache = new HashMap<ITokenType, Terminal>();
	}

	Grammar grammar;
	Grammar pseudoGrammar;

	Grammar getGrammar() {
		return this.pseudoGrammar;
	}

	List<Rule> allRules_cache;
	List<Rule> getAllRules() {
		if (null==this.allRules_cache) {
			this.allRules_cache = this.getGrammar().getAllRule();
		}
		return this.allRules_cache;
	}
	
	List<SkipRule> allSkipRules_cache;
	List<SkipRule> getAllSkipRules() {
		if (null==this.allSkipRules_cache) {
			this.allSkipRules_cache = new ArrayList<>();
			for(Rule r: this.getAllRules()) {
				if (r instanceof SkipRule) {
					this.allSkipRules_cache.add((SkipRule)r);
				}
			}
		}
		return this.allSkipRules_cache;
	}
	
	@Override
	public List<INodeType> getNodeTypes() {
		List<INodeType> result = new ArrayList<INodeType>();
		for (Rule r : this.getAllRules()) {
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
			//return this.doParse2(goal, text);
			return this.parse2(goal, text);
		} catch (RuleNotFoundException e) {
			// Should never happen!
			throw new RuntimeException("Should never happen", e);
		}
	}

	public IParseTree parse2(INodeType goal, CharSequence text) throws ParseFailedException, RuleNotFoundException, ParseTreeException {
			Rule goalRule = new Rule(this.pseudoGrammar, "$goal$");
			goalRule.setRhs(new Concatination(new TerminalLiteral(START_SYMBOL), new NonTerminal(goal.getIdentity().asPrimitive()), new TerminalLiteral(FINISH_SYMBOL)));
			this.pseudoGrammar.setRule(Arrays.asList(new Rule[]{
					goalRule
			}));
			
			CharSequence pseudoText = START_SYMBOL + text + FINISH_SYMBOL;
			
			ParseTreeBranch pseudoTree = (ParseTreeBranch)this.doParse2(goalRule.getNodeType(), pseudoText);
			//return pseudoTree;
			Rule r = this.getGrammar().findAllRule(goal.getIdentity().asPrimitive());
			Input inp = new Input(text);
			int s = pseudoTree.getRoot().getChildren().size();
			IBranch root = (IBranch)pseudoTree.getRoot().getChildren().stream().filter(n -> n.getName().equals(goal.getIdentity().asPrimitive())).findFirst().get();
			int indexOfRoot = pseudoTree.getRoot().getChildren().indexOf(root);
			List<INode> before = pseudoTree.getRoot().getChildren().subList(1, indexOfRoot);
			ArrayList<INode> children = new ArrayList<>();
			List<INode> after = pseudoTree.getRoot().getChildren().subList(indexOfRoot+1, s-1);
			children.addAll(before);
			children.addAll(root.getChildren());
			children.addAll(after);
			Branch nb = new Branch(root.getNodeType(), children);
			ParseTreeBranch pt = new ParseTreeBranch(inp, nb, new Stack<>(), r, Integer.MAX_VALUE);
			return pt;
	}
	
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
	IParseTree doParse2(INodeType goal, CharSequence text) throws ParseFailedException, RuleNotFoundException, ParseTreeException {
		Input input = new Input(text);
		
		Forrest newForrest = new Forrest(goal, this.getGrammar(), this.getAllRules() ,input);
		Forrest oldForrest = null;
		
		List<ParseTreeBud> buds = input.createNewBuds(this.getGrammar().getAllTerminal(), 0);
		for(ParseTreeBud bud : buds) {
			Set<AbstractParseTree> newTrees = bud.growHeight(this.getAllRules());
			newForrest.addAll(newTrees);
		}
		
		while (newForrest.getCanGrow() ) {
			oldForrest=newForrest.shallowClone();
			newForrest = oldForrest.grow();
		} 

		IParseTree match = newForrest.getLongestMatch(text);

		return match;
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


	
}
