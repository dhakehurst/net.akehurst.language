package net.akehurst.language.grammar.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.akehurst.language.core.parser.IBranch;
import net.akehurst.language.core.parser.INode;
import net.akehurst.language.core.parser.INodeType;
import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.core.parser.IParser;
import net.akehurst.language.core.parser.ParseFailedException;
import net.akehurst.language.core.parser.ParseTreeException;
import net.akehurst.language.core.parser.RuleNotFoundException;
import net.akehurst.language.grammar.parse.tree.Leaf;
import net.akehurst.language.grammar.parser.converter.Converter;
import net.akehurst.language.grammar.parser.converter.Grammar2RuntimeRuleSet;
import net.akehurst.language.grammar.parser.forrest.Forrest3;
import net.akehurst.language.grammar.parser.forrest.Input3;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleSet;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleSetBuilder;
import net.akehurst.language.ogl.semanticStructure.ChoiceSimple;
import net.akehurst.language.ogl.semanticStructure.Concatenation;
import net.akehurst.language.ogl.semanticStructure.Grammar;
import net.akehurst.language.ogl.semanticStructure.Namespace;
import net.akehurst.language.ogl.semanticStructure.NonTerminal;
import net.akehurst.language.ogl.semanticStructure.Rule;
import net.akehurst.language.ogl.semanticStructure.TerminalLiteral;
import net.akehurst.language.parse.graph.GraphNodeRoot;
import net.akehurst.language.parse.graph.IGraphNode;
import net.akehurst.language.parse.graph.IParseGraph;
import net.akehurst.language.parse.graph.ParseGraph;
import net.akehurst.language.parse.graph.ParseTreeFromGraph;

public class ScannerLessParser3 implements IParser {

	public final static String START_SYMBOL = "\u0000";
	public final static TerminalLiteral START_SYMBOL_TERMINAL = new TerminalLiteral(START_SYMBOL);
	public final static String FINISH_SYMBOL = "\u0001";
	public final static TerminalLiteral FINISH_SYMBOL_TERMINAL = new TerminalLiteral(FINISH_SYMBOL);

	public ScannerLessParser3(RuntimeRuleSetBuilder runtimeFactory, Grammar grammar) {
		this.grammar = grammar;
		// this.findTerminal_cache = new HashMap<ITokenType, Terminal>();
		this.runtimeBuilder = runtimeFactory;
		this.converter = new Converter(this.runtimeBuilder); // TODO: might not be needed here as it is set elsewhere, below
	}

	Converter converter;
	RuntimeRuleSetBuilder runtimeBuilder;
	Grammar grammar;

	Grammar getGrammar() {
		return this.grammar;
	}

	Grammar pseudoGrammar;
	RuntimeRuleSet runtimeRuleSet;

	RuntimeRuleSet getRuntimeRuleSet() {
		return this.runtimeRuleSet;
	}

	@Override
	public void build(INodeType goalNodeType) {
		//this.createPseudoGrammar(goalNodeType);
		this.init();
		this.runtimeRuleSet.build();
	}

	void init() {
		try {

			this.runtimeRuleSet = this.converter.transformLeft2Right(Grammar2RuntimeRuleSet.class, this.grammar);
//			int pseudoGoalNumber = this.runtimeRuleSet.getRuleNumber(goalRule.getName());
//			RuntimeRule pseudoGoalRR = this.runtimeRuleSet.getRuntimeRule(pseudoGoalNumber);
//			return pseudoGoalRR;
		} catch (Exception e) {
			e.printStackTrace();

		}
	}
	
	RuntimeRule createPseudoGrammar(INodeType goal) {
		try {
			this.pseudoGrammar = new Grammar(new Namespace(grammar.getNamespace().getQualifiedName() + "::pseudo"), "Pseudo");
			this.pseudoGrammar.setExtends(Arrays.asList(new Grammar[] { this.grammar }));
			Rule goalRule = new Rule(this.pseudoGrammar, "$goal$");
			goalRule.setRhs(new ChoiceSimple(new Concatenation(new TerminalLiteral(START_SYMBOL), new NonTerminal(goal.getIdentity().asPrimitive()),
					new TerminalLiteral(FINISH_SYMBOL))));
			this.pseudoGrammar.getRule().add(goalRule);
			this.runtimeRuleSet = this.converter.transformLeft2Right(Grammar2RuntimeRuleSet.class, this.pseudoGrammar);
			int pseudoGoalNumber = this.runtimeRuleSet.getRuleNumber(goalRule.getName());
			RuntimeRule pseudoGoalRR = this.runtimeRuleSet.getRuntimeRule(pseudoGoalNumber);
			return pseudoGoalRR;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public List<INodeType> getNodeTypes() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IParseTree parse(String goalRuleName, CharSequence text) throws ParseFailedException, ParseTreeException, RuleNotFoundException {
		INodeType goal = this.getGrammar().findRule(goalRuleName).getNodeType();
		this.build(goal);
		return this.parse(goal, text);
	}

	@Override
	public IParseTree parse(INodeType goal, CharSequence text) throws ParseFailedException, ParseTreeException {
		try {
			// return this.doParse2(goal, text);
			IGraphNode gr = this.parse3(goal, text);
			IParseTree tree = new ParseTreeFromGraph(gr);
			// set the parent property of each child, these are not set during parsing
			this.setParentForChildren((IBranch) tree.getRoot());
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


	public IGraphNode parse3(INodeType goal, CharSequence text) throws ParseFailedException, RuleNotFoundException, ParseTreeException {
		if (null == this.runtimeRuleSet) {
			this.build(goal);
		}
		int goalRuleNumber = this.runtimeRuleSet.getRuleNumber(goal.getIdentity().asPrimitive());
		RuntimeRule goalRR = this.runtimeRuleSet.getRuntimeRule(goalRuleNumber);
		IGraphNode node = this.doParse3(goalRR, text);
//		GraphNodeRoot gr = new GraphNodeRoot(goalRR, node.getChildren());
		return node;
	}
	

	IGraphNode doParse3(RuntimeRule goalRule, CharSequence text) throws ParseFailedException, RuleNotFoundException, ParseTreeException {
		Input3 input = new Input3(this.runtimeBuilder, text);
		IParseGraph graph = new ParseGraph(goalRule,text.length());
		Forrest3 newForrest = new Forrest3(graph, this.getRuntimeRuleSet(), input, goalRule);
		newForrest.start(graph, goalRule, input);
		int seasons = 1;
		
		do {
			newForrest.grow();
			seasons++;
		} while(newForrest.getCanGrow());
		
		IGraphNode match = newForrest.getLongestMatch(text);
		return match;
	}
	
	

}
