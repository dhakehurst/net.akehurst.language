package net.akehurst.language.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
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
import net.akehurst.language.ogl.semanticModel.Choice;
import net.akehurst.language.ogl.semanticModel.Concatenation;
import net.akehurst.language.ogl.semanticModel.Grammar;
import net.akehurst.language.ogl.semanticModel.Multi;
import net.akehurst.language.ogl.semanticModel.Namespace;
import net.akehurst.language.ogl.semanticModel.NonTerminal;
import net.akehurst.language.ogl.semanticModel.Rule;
import net.akehurst.language.ogl.semanticModel.RuleItem;
import net.akehurst.language.ogl.semanticModel.RuleNotFoundException;
import net.akehurst.language.ogl.semanticModel.SeparatedList;
import net.akehurst.language.ogl.semanticModel.SkipRule;
import net.akehurst.language.ogl.semanticModel.TangibleItem;
import net.akehurst.language.ogl.semanticModel.Terminal;
import net.akehurst.language.ogl.semanticModel.TerminalLiteral;
import net.akehurst.language.parser.forrest.AbstractParseTree;
import net.akehurst.language.parser.forrest.Branch;
import net.akehurst.language.parser.forrest.Factory;
import net.akehurst.language.parser.forrest.Forrest;
import net.akehurst.language.parser.forrest.Input;
import net.akehurst.language.parser.forrest.ParseTreeBranch;
import net.akehurst.language.parser.forrest.ParseTreeBud;

public class ScannerLessParser implements IParser {

	public final static String START_SYMBOL = "\uE000";
	public final static TerminalLiteral START_SYMBOL_TERMINAL = new TerminalLiteral(START_SYMBOL);
	public final static String FINISH_SYMBOL = "\uE001";
	public final static TerminalLiteral FINISH_SYMBOL_TERMINAL = new TerminalLiteral(FINISH_SYMBOL);
	
	
	public ScannerLessParser(Factory parseTreeFactory, Grammar grammar) {
		this.grammar = grammar;
		this.findTerminal_cache = new HashMap<ITokenType, Terminal>();
		this.factory = parseTreeFactory;
	}
	
	Factory factory;
	Grammar grammar;
	Grammar pseudoGrammar;
		
	Rule createPseudoGrammar(INodeType goal) {
		this.pseudoGrammar = new Grammar(new Namespace(grammar.getNamespace().getQualifiedName()+"::pseudo"), "Pseudo");
		this.pseudoGrammar.setExtends(Arrays.asList(new Grammar[]{this.grammar}));
		Rule goalRule = new Rule(this.pseudoGrammar, "$goal$");
		goalRule.setRhs(new Concatenation(new TerminalLiteral(START_SYMBOL), new NonTerminal(goal.getIdentity().asPrimitive()), new TerminalLiteral(FINISH_SYMBOL)));
		this.pseudoGrammar.getRule().add(goalRule);
		this.allRules_cache = null;
		this.getAllRules();
		this.allRules_cache.add(goalRule);
		return goalRule;
	}
	
	Grammar getGrammar() {
		return this.grammar;
	}

	Set<Rule> allRules_cache;
	Set<Rule> getAllRules() {
		if (null==this.allRules_cache) {
			this.allRules_cache = new HashSet<>();
			this.allRules_cache.addAll(this.getGrammar().getAllRule());
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
	
	Rule findRule(String ruleName) throws RuleNotFoundException {
		for(Rule r : this.getAllRules()) {
			if (r.getName().equals(ruleName)) {
				return r;
			}
		}
		throw new RuleNotFoundException(ruleName);
	}
	
	Set<Terminal> allTerminal;
	public Set<Terminal> getAllTerminal() {
		if (null==this.allTerminal) {
			this.allTerminal = this.findAllTerminal();
			this.allTerminal.add(START_SYMBOL_TERMINAL);
			this.allTerminal.add(FINISH_SYMBOL_TERMINAL);
			
		}
		return this.allTerminal;
	}
	Set<Terminal> findAllTerminal() {
		Set<Terminal> result = new HashSet<>();
		for (Rule rule : this.getAllRules()) {
			RuleItem ri = rule.getRhs();
			result.addAll(this.findAllTerminal(0, rule, ri ) );
		}
		return result;
	}
	
	Set<Terminal> findAllTerminal(final int totalItems, final Rule rule, RuleItem item) {
		Set<Terminal> result = new HashSet<>();
		if (item instanceof Terminal) {
			Terminal t = (Terminal) item;
			result.add(t);
		} else if (item instanceof Multi) {
			result.addAll( this.findAllTerminal( totalItems, rule, ((Multi)item).getItem() ) );
		} else if (item instanceof Choice) {
			for(TangibleItem ti : ((Choice)item).getAlternative()) {
				result.addAll( this.findAllTerminal( totalItems, rule, ti ) );
			}
		} else if (item instanceof Concatenation) {
			for(TangibleItem ti : ((Concatenation)item).getItem()) {
				result.addAll( this.findAllTerminal( totalItems, rule, ti ) );
			}
		} else if (item instanceof SeparatedList) {
			result.addAll(this.findAllTerminal(totalItems, rule,((SeparatedList)item).getSeparator()));
			result.addAll( this.findAllTerminal(totalItems, rule, ((SeparatedList)item).getConcatination() ) );
		}
		return result;
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
			Rule goalRule = this.createPseudoGrammar(goal);
			CharSequence pseudoText = START_SYMBOL + text + FINISH_SYMBOL;
			
			ParseTreeBranch pseudoTree = (ParseTreeBranch)this.doParse2(goalRule.getNodeType(), pseudoText);
			//return pseudoTree;
			Rule r = this.findRule(goal.getIdentity().asPrimitive());
			Input inp = new Input(this.factory, text);
			int s = pseudoTree.getRoot().getChildren().size();
			IBranch root = (IBranch)pseudoTree.getRoot().getChildren().stream().filter(n -> n.getName().equals(goal.getIdentity().asPrimitive())).findFirst().get();
			int indexOfRoot = pseudoTree.getRoot().getChildren().indexOf(root);
			List<INode> before = pseudoTree.getRoot().getChildren().subList(1, indexOfRoot);
			ArrayList<INode> children = new ArrayList<>();
			List<INode> after = pseudoTree.getRoot().getChildren().subList(indexOfRoot+1, s-1);
			children.addAll(before);
			children.addAll(root.getChildren());
			children.addAll(after);
			IBranch nb = this.factory.createBranch(root.getNodeType(), children);
			ParseTreeBranch pt = new ParseTreeBranch(this.factory, inp, nb, null, r, Integer.MAX_VALUE);
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
	IParseTree doParse2(INodeType goal, CharSequence text) throws ParseFailedException, RuleNotFoundException, ParseTreeException {
		Input input = new Input(this.factory, text);
		
		Forrest newForrest = new Forrest(goal, this.getAllRules() ,input);
		Forrest oldForrest = null;
		
		List<ParseTreeBud> buds = input.createNewBuds(this.getAllTerminal(), 0);
		for(ParseTreeBud bud : buds) {
			Set<AbstractParseTree> newTrees = bud.growHeight(this.getAllRules());
			newForrest.addAll(newTrees);
		}
		
		while (newForrest.getCanGrow() ) {
			++numberOfSeasons;
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
			for (Terminal term : this.getAllTerminal()) {
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
