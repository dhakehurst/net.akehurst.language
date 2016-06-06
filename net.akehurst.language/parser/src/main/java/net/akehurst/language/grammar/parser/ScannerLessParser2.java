package net.akehurst.language.grammar.parser;

import java.awt.font.ImageGraphicAttribute;
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
import net.akehurst.language.core.parser.RuleNotFoundException;
import net.akehurst.language.grammar.parser.converter.Converter;
import net.akehurst.language.grammar.parser.converter.Grammar2RuntimeRuleSet;
import net.akehurst.language.grammar.parser.forrest.AbstractParseTree;
import net.akehurst.language.grammar.parser.forrest.AbstractParseTree2;
import net.akehurst.language.grammar.parser.forrest.Forrest;
import net.akehurst.language.grammar.parser.forrest.Forrest2;
import net.akehurst.language.grammar.parser.forrest.ForrestFactory;
import net.akehurst.language.grammar.parser.forrest.ForrestFactory2;
import net.akehurst.language.grammar.parser.forrest.NodeIdentifier;
import net.akehurst.language.grammar.parser.forrest.ParseTreeBranch;
import net.akehurst.language.grammar.parser.forrest.ParseTreeBranch2;
import net.akehurst.language.grammar.parser.forrest.ParseTreeBud;
import net.akehurst.language.grammar.parser.forrest.ParseTreeBud2;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleSet;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleSetBuilder;
import net.akehurst.language.graphStructuredStack.IGraphStructuredStack;
import net.akehurst.language.graphStructuredStack.impl.GraphStructuredStack;
import net.akehurst.language.ogl.semanticStructure.ChoiceSimple;
import net.akehurst.language.ogl.semanticStructure.Concatenation;
import net.akehurst.language.ogl.semanticStructure.Grammar;
import net.akehurst.language.ogl.semanticStructure.Namespace;
import net.akehurst.language.ogl.semanticStructure.NonTerminal;
import net.akehurst.language.ogl.semanticStructure.Rule;
import net.akehurst.language.ogl.semanticStructure.TerminalLiteral;

public class ScannerLessParser2 implements IParser {

	public final static String START_SYMBOL = "\uE000";
	public final static TerminalLiteral START_SYMBOL_TERMINAL = new TerminalLiteral(START_SYMBOL);
	public final static String FINISH_SYMBOL = "\uE001";
	public final static TerminalLiteral FINISH_SYMBOL_TERMINAL = new TerminalLiteral(FINISH_SYMBOL);

	public ScannerLessParser2(RuntimeRuleSetBuilder runtimeFactory, Grammar grammar) {
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
		this.createPseudoGrammar(goalNodeType);
		this.runtimeRuleSet.build();
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
			IParseTree tree = this.parse2(goal, text);
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

	public IParseTree parse2(INodeType goal, CharSequence text) throws ParseFailedException, RuleNotFoundException, ParseTreeException {
		if (null == this.runtimeRuleSet) {
			this.build(goal);
		}
		RuntimeRule pseudoGoalRule = this.createPseudoGrammar(goal); //TODO: do I need this here?
		int goalRuleNumber = this.runtimeRuleSet.getRuleNumber(goal.getIdentity().asPrimitive());
		RuntimeRule goalRR = this.runtimeRuleSet.getRuntimeRule(goalRuleNumber);
		CharSequence pseudoText = START_SYMBOL + text + FINISH_SYMBOL;

		ParseTreeBranch2 pseudoTree = (ParseTreeBranch2) this.doParse2(pseudoGoalRule, pseudoText);
		ForrestFactory ffactory = new ForrestFactory(this.runtimeBuilder, text);
		int s = pseudoTree.getRoot().getChildren().size();
		IBranch root = (IBranch) pseudoTree.getRoot().getChildren().stream().filter(n -> n.getName().equals(goal.getIdentity().asPrimitive())).findFirst()
				.get();
		int indexOfRoot = pseudoTree.getRoot().getChildren().indexOf(root);
		List<INode> before = pseudoTree.getRoot().getChildren().subList(1, indexOfRoot);
		ArrayList<INode> children = new ArrayList<>();
		List<INode> after = pseudoTree.getRoot().getChildren().subList(indexOfRoot + 1, s - 1);
		children.addAll(before);
		children.addAll(root.getChildren());
		children.addAll(after);
		ParseTreeBranch pt = ffactory.fetchOrCreateBranch(goalRR, children.toArray(new INode[children.size()]), null, -1); // Integer.MAX_VALUE);
		return pt;
	}

	IParseTree doParse2(RuntimeRule pseudoGoalRule, CharSequence text) throws ParseFailedException, RuleNotFoundException, ParseTreeException {
		ForrestFactory2 ff = new ForrestFactory2(this.runtimeBuilder, text);
		RuntimeRule sst = this.getRuntimeRuleSet().getForTerminal(START_SYMBOL_TERMINAL.getValue());
		ParseTreeBud2 startBud = ff.createNewBuds(new RuntimeRule[] { sst }, 0).get(0);
		Forrest2 newForrest = new Forrest2(ff, this.getRuntimeRuleSet());
		newForrest.newSeeds(Arrays.asList(startBud));

		int seasons = 1;
		//List<AbstractParseTree2> newTrees = newForrest.growHeight(startBud);
		
		Forrest2 oldForrest = null;
		do {
			oldForrest = newForrest.shallowClone(); // remove this later, its for debugging
			newForrest = oldForrest.grow();
			seasons++;
		} while(newForrest.getCanGrow());
		
		IParseTree match = newForrest.getLongestMatch(text);
		return match;
	}

}
