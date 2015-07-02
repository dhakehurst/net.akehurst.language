package net.akehurst.language.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import net.akehurst.language.core.parser.IBranch;
import net.akehurst.language.core.parser.INode;
import net.akehurst.language.core.parser.INodeIdentity;
import net.akehurst.language.core.parser.INodeType;
import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.core.parser.IParser;
import net.akehurst.language.core.parser.ParseFailedException;
import net.akehurst.language.core.parser.ParseTreeException;
import net.akehurst.language.ogl.semanticModel.Concatenation;
import net.akehurst.language.ogl.semanticModel.Grammar;
import net.akehurst.language.ogl.semanticModel.Namespace;
import net.akehurst.language.ogl.semanticModel.NonTerminal;
import net.akehurst.language.ogl.semanticModel.Rule;
import net.akehurst.language.ogl.semanticModel.RuleNotFoundException;
import net.akehurst.language.ogl.semanticModel.TerminalLiteral;
import net.akehurst.language.parser.converter.Converter;
import net.akehurst.language.parser.converter.Grammar2RuntimeRuleSet;
import net.akehurst.language.parser.forrest.AbstractParseTree;
import net.akehurst.language.parser.runtime.Factory;
import net.akehurst.language.parser.forrest.Forrest;
import net.akehurst.language.parser.forrest.ForrestFactory;
import net.akehurst.language.parser.forrest.Input;
import net.akehurst.language.parser.forrest.ParseTreeBranch;
import net.akehurst.language.parser.forrest.ParseTreeBud;
import net.akehurst.language.parser.runtime.Branch;
import net.akehurst.language.parser.runtime.RuntimeRule;
import net.akehurst.language.parser.runtime.RuntimeRuleSet;

public class ScannerLessParser implements IParser {

	public final static String START_SYMBOL = "\uE000";
	public final static TerminalLiteral START_SYMBOL_TERMINAL = new TerminalLiteral(START_SYMBOL);
	public final static String FINISH_SYMBOL = "\uE001";
	public final static TerminalLiteral FINISH_SYMBOL_TERMINAL = new TerminalLiteral(FINISH_SYMBOL);
	
	
	public ScannerLessParser(Factory runtimeFactory, Grammar grammar) {
		this.grammar = grammar;
//		this.findTerminal_cache = new HashMap<ITokenType, Terminal>();
		this.runtimeFactory = runtimeFactory;
		this.converter = new Converter(this.runtimeFactory);
	}
	
	Converter converter;
	Factory runtimeFactory;
	Grammar grammar;
	Grammar pseudoGrammar;
		
	RuntimeRule createPseudoGrammar(INodeType goal) {
		try {
			this.pseudoGrammar = new Grammar(new Namespace(grammar.getNamespace().getQualifiedName()+"::pseudo"), "Pseudo");
			this.pseudoGrammar.setExtends(Arrays.asList(new Grammar[]{this.grammar}));
			Rule goalRule = new Rule(this.pseudoGrammar, "$goal$");
			goalRule.setRhs(new Concatenation(new TerminalLiteral(START_SYMBOL), new NonTerminal(goal.getIdentity().asPrimitive()), new TerminalLiteral(FINISH_SYMBOL)));
			this.pseudoGrammar.getRule().add(goalRule);
			this.runtimeRuleSet = this.converter.transformLeft2Right(Grammar2RuntimeRuleSet.class, this.pseudoGrammar);
			int pseudoGoalNumber = this.runtimeRuleSet.getRuleNumber(goalRule.getNodeType());
			RuntimeRule pseudoGoalRR = this.runtimeRuleSet.getRuntimeRule(pseudoGoalNumber);
	//		
	//		this.allRules_cache = null;
	//		this.getAllRules();
	//		this.allRules_cache.add(goalRule);
			return pseudoGoalRR;
		}catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	Grammar getGrammar() {
		return this.grammar;
	}

	RuntimeRuleSet runtimeRuleSet;
	RuntimeRuleSet getRuntimeRuleSet() {
		return this.runtimeRuleSet;
	}
	
//	Rule findRule(String ruleName) throws RuleNotFoundException {
//		for(Rule r : this.getAllRules()) {
//			if (r.getName().equals(ruleName)) {
//				return r;
//			}
//		}
//		throw new RuleNotFoundException(ruleName);
//	}
	
//	Set<Terminal> allTerminal;
//	public Set<Terminal> getAllTerminal() {
//		if (null==this.allTerminal) {
//			this.allTerminal = this.findAllTerminal();
//			this.allTerminal.add(START_SYMBOL_TERMINAL);
//			this.allTerminal.add(FINISH_SYMBOL_TERMINAL);
//			
//		}
//		return this.allTerminal;
//	}
//	Set<Terminal> findAllTerminal() {
//		Set<Terminal> result = new HashSet<>();
//		for (Rule rule : this.getAllRules()) {
//			RuleItem ri = rule.getRhs();
//			result.addAll(this.findAllTerminal(0, rule, ri ) );
//		}
//		return result;
//	}
	
//	Set<Terminal> findAllTerminal(final int totalItems, final Rule rule, RuleItem item) {
//		Set<Terminal> result = new HashSet<>();
//		if (item instanceof Terminal) {
//			Terminal t = (Terminal) item;
//			result.add(t);
//		} else if (item instanceof Multi) {
//			result.addAll( this.findAllTerminal( totalItems, rule, ((Multi)item).getItem() ) );
//		} else if (item instanceof Choice) {
//			for(TangibleItem ti : ((Choice)item).getAlternative()) {
//				result.addAll( this.findAllTerminal( totalItems, rule, ti ) );
//			}
//		} else if (item instanceof Concatenation) {
//			for(TangibleItem ti : ((Concatenation)item).getItem()) {
//				result.addAll( this.findAllTerminal( totalItems, rule, ti ) );
//			}
//		} else if (item instanceof SeparatedList) {
//			result.addAll(this.findAllTerminal(totalItems, rule,((SeparatedList)item).getSeparator()));
//			result.addAll( this.findAllTerminal(totalItems, rule, ((SeparatedList)item).getConcatination() ) );
//		}
//		return result;
//	}
	
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
			//return this.doParse2(goal, text);
			return this.parse2(goal, text);
		} catch (RuleNotFoundException e) {
			// Should never happen!
			throw new RuntimeException("Should never happen", e);
		}
	}

	public IParseTree parse2(INodeType goal, CharSequence text) throws ParseFailedException, RuleNotFoundException, ParseTreeException {
			RuntimeRule pseudoGoalRule = this.createPseudoGrammar(goal);
			Rule goalRule = this.grammar.findAllRule(goal.getIdentity().asPrimitive());
			int goalRuleNumber = this.runtimeRuleSet.getRuleNumber(goal);
			RuntimeRule goalRR = this.runtimeRuleSet.getRuntimeRule(goalRuleNumber);
			CharSequence pseudoText = START_SYMBOL + text + FINISH_SYMBOL;
			
			ParseTreeBranch pseudoTree = (ParseTreeBranch)this.doParse2(pseudoGoalRule, pseudoText);
			//return pseudoTree;
			//Rule r = this.findRule(goal.getIdentity().asPrimitive());
			ForrestFactory ffactory = new ForrestFactory(this.runtimeFactory, text);
			int s = pseudoTree.getRoot().getChildren().size();
			IBranch root = (IBranch)pseudoTree.getRoot().getChildren().stream().filter(n -> n.getName().equals(goal.getIdentity().asPrimitive())).findFirst().get();
			int indexOfRoot = pseudoTree.getRoot().getChildren().indexOf(root);
			List<INode> before = pseudoTree.getRoot().getChildren().subList(1, indexOfRoot);
			ArrayList<INode> children = new ArrayList<>();
			List<INode> after = pseudoTree.getRoot().getChildren().subList(indexOfRoot+1, s-1);
			children.addAll(before);
			children.addAll(root.getChildren());
			children.addAll(after);
//			Branch nb = (Branch)this.factory.createBranch(goalRR, children.toArray(new INode[children.size()]));
//			ParseTreeBranch pt = new ParseTreeBranch(this.factory, inp, nb, null, goalRR, Integer.MAX_VALUE);
			ParseTreeBranch pt = ffactory.fetchOrCreateBranch(goalRR, children.toArray(new INode[children.size()]), null, Integer.MAX_VALUE);
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
	IParseTree doParse2(RuntimeRule pseudoGoalRule, CharSequence text) throws ParseFailedException, RuleNotFoundException, ParseTreeException {
		ForrestFactory ff = new ForrestFactory(this.runtimeFactory, text);
		
		Forrest newForrest = new Forrest(pseudoGoalRule, this.getRuntimeRuleSet());
		Forrest oldForrest = null;
		
		//List<ParseTreeBud> buds = input.createNewBuds(this.getAllTerminal(), 0);
		//for(ParseTreeBud bud : buds) {
		//	Set<AbstractParseTree> newTrees = bud.growHeight(this.getAllRules());
		//	newForrest.addAll(newTrees);
		//}
		RuntimeRule sst = this.getRuntimeRuleSet().getForTerminal(START_SYMBOL_TERMINAL);
		ParseTreeBud startBud = ff.createNewBuds(new RuntimeRule[] {sst}, 0).get(0);
		RuntimeRule[] terminalRules = runtimeRuleSet.getPossibleSubTerminal(sst);

		
		ArrayList<AbstractParseTree> newTrees = startBud.growHeight(terminalRules, this.runtimeRuleSet);//new RuntimeRule[] {pseudoGoalRule});
		newForrest.addAll(newTrees);
		
		while (newForrest.getCanGrow() ) {
			++numberOfSeasons;
//			System.out.println(this.numberOfSeasons);
			oldForrest=newForrest.shallowClone();
			newForrest = oldForrest.grow();
		} 
System.out.println(this.numberOfSeasons);
		IParseTree match = newForrest.getLongestMatch(text);
		System.out.println(((ParseTreeBranch)match).getIdString());

		return match;
	}

//	Map<ITokenType, Terminal> findTerminal_cache;
//
//	Terminal findTerminal(IToken token) {
//		Terminal terminal = this.findTerminal_cache.get(token.getType());
//		if (null == terminal) {
//			for (Terminal term : this.getAllTerminal()) {
//				if ((!token.getType().getIsRegEx() && term.getValue().equals(token.getType().getIdentity()))
//						|| (token.getType().getIsRegEx() && term.getValue().equals(token.getType().getPatternString()))) {
//					this.findTerminal_cache.put(token.getType(), term);
//					return term;
//				}
//			}
//		}
//		return terminal;
//	}


	
}
