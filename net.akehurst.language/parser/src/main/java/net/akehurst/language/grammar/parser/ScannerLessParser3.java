package net.akehurst.language.grammar.parser;

import java.io.BufferedReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.akehurst.language.core.analyser.IGrammar;
import net.akehurst.language.core.analyser.IRuleItem;
import net.akehurst.language.core.parser.IBranch;
import net.akehurst.language.core.parser.INode;
import net.akehurst.language.core.parser.INodeType;
import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.core.parser.IParser;
import net.akehurst.language.core.parser.ParseFailedException;
import net.akehurst.language.core.parser.ParseTreeException;
import net.akehurst.language.core.parser.RuleNotFoundException;
import net.akehurst.language.grammar.parser.converter.Converter;
import net.akehurst.language.grammar.parser.converter.Grammar2RuntimeRuleSet;
import net.akehurst.language.grammar.parser.forrest.Forrest3;
import net.akehurst.language.grammar.parser.forrest.Input3;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleKind;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleSet;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleSetBuilder;
import net.akehurst.language.ogl.semanticStructure.Grammar;
import net.akehurst.language.ogl.semanticStructure.TerminalLiteral;
import net.akehurst.language.parse.graph.IGraphNode;
import net.akehurst.language.parse.graph.IParseGraph;
import net.akehurst.language.parse.graph.ParseGraph;
import net.akehurst.language.parse.graph.ParseTreeFromGraph;

public class ScannerLessParser3 implements IParser {

	public final static String START_SYMBOL = "\u0000";
	public final static TerminalLiteral START_SYMBOL_TERMINAL = new TerminalLiteral(ScannerLessParser3.START_SYMBOL);
	public final static String FINISH_SYMBOL = "\u0001";
	public final static TerminalLiteral FINISH_SYMBOL_TERMINAL = new TerminalLiteral(ScannerLessParser3.FINISH_SYMBOL);

	public ScannerLessParser3(final RuntimeRuleSetBuilder runtimeFactory, final IGrammar grammar) {
		this.grammar = grammar;
		// this.findTerminal_cache = new HashMap<ITokenType, Terminal>();
		this.runtimeBuilder = runtimeFactory;
		this.converter = new Converter(this.runtimeBuilder); // TODO: might not be needed here as it is set elsewhere, below
	}

	Converter converter;
	RuntimeRuleSetBuilder runtimeBuilder;
	IGrammar grammar;

	IGrammar getGrammar() {
		return this.grammar;
	}

	Grammar pseudoGrammar;
	RuntimeRuleSet runtimeRuleSet;

	RuntimeRuleSet getRuntimeRuleSet() {
		return this.runtimeRuleSet;
	}

	@Override
	public void build() {
		this.init();
		this.runtimeRuleSet.build();
	}

	void init() {
		try {

			this.runtimeRuleSet = this.converter.transformLeft2Right(Grammar2RuntimeRuleSet.class, (Grammar) this.grammar);
			// int pseudoGoalNumber = this.runtimeRuleSet.getRuleNumber(goalRule.getName());
			// RuntimeRule pseudoGoalRR = this.runtimeRuleSet.getRuntimeRule(pseudoGoalNumber);
			// return pseudoGoalRR;
		} catch (final Exception e) {
			e.printStackTrace();

		}
	}

	// RuntimeRule createPseudoGrammar(final INodeType goal) {
	// try {
	// this.pseudoGrammar = new Grammar(new Namespace(this.grammar.getNamespace().getQualifiedName() + "::pseudo"), "Pseudo");
	// this.pseudoGrammar.setExtends(Arrays.asList(new Grammar[] { this.grammar }));
	// final Rule goalRule = new Rule(this.pseudoGrammar, "$goal$");
	// goalRule.setRhs(new ChoiceSimple(new Concatenation(new TerminalLiteral(ScannerLessParser3.START_SYMBOL),
	// new NonTerminal(goal.getIdentity().asPrimitive()), new TerminalLiteral(ScannerLessParser3.FINISH_SYMBOL))));
	// this.pseudoGrammar.getRule().add(goalRule);
	// this.runtimeRuleSet = this.converter.transformLeft2Right(Grammar2RuntimeRuleSet.class, this.pseudoGrammar);
	// final int pseudoGoalNumber = this.runtimeRuleSet.getRuleNumber(goalRule.getName());
	// final RuntimeRule pseudoGoalRR = this.runtimeRuleSet.getRuntimeRule(pseudoGoalNumber);
	// return pseudoGoalRR;
	// } catch (final Exception e) {
	// e.printStackTrace();
	// return null;
	// }
	// }

	@Override
	public Set<INodeType> getNodeTypes() {
		return this.getGrammar().findAllNodeType();
	}

	// @Override
	// public IParseTree parse(String goalRuleName, CharSequence text) throws ParseFailedException, ParseTreeException, RuleNotFoundException {
	// INodeType goal = this.getGrammar().findRule(goalRuleName).getNodeType();
	// Reader r = new CharArrayReader(text.)
	// return this.parse(goal, StringReader);
	// }

	@Override
	public IParseTree parse(final String goalRuleName, final Reader inputReader) throws ParseFailedException, ParseTreeException {
		try {
			final INodeType goal = ((Grammar) this.getGrammar()).findRule(goalRuleName).getNodeType();
			final IGraphNode gr = this.parse3(goal, inputReader);
			final IParseTree tree = new ParseTreeFromGraph(gr);
			// set the parent property of each child, these are not set during parsing
			// TODO: don't know if we need this, probably not
			this.setParentForChildren((IBranch) tree.getRoot());
			return tree;
		} catch (final RuleNotFoundException e) {
			// Should never happen!
			throw new RuntimeException("Should never happen", e);
		}
	}

	void setParentForChildren(final IBranch node) {
		final IBranch parent = node;
		for (final INode child : parent.getChildren()) {
			child.setParent(parent);
			if (child instanceof IBranch) {
				this.setParentForChildren((IBranch) child);
			}
		}
	}

	public IGraphNode parse3(final INodeType goal, final Reader inputReader) throws ParseFailedException, RuleNotFoundException, ParseTreeException {
		if (null == this.runtimeRuleSet) {
			this.build();
		}
		final int goalRuleNumber = this.runtimeRuleSet.getRuleNumber(goal.getIdentity().asPrimitive());
		final RuntimeRule goalRR = this.runtimeRuleSet.getRuntimeRule(goalRuleNumber);
		final IGraphNode node = this.doParse3(goalRR, inputReader);
		// GraphNodeRoot gr = new GraphNodeRoot(goalRR, node.getChildren());
		return node;
	}

	IGraphNode doParse3(final RuntimeRule goalRule, final Reader inputReader) throws ParseFailedException, RuleNotFoundException, ParseTreeException {
		// TODO: handle reader directly without converting to string
		final BufferedReader br = new BufferedReader(inputReader);
		final String text = br.lines().collect(Collectors.joining(System.lineSeparator()));

		final Input3 input = new Input3(this.runtimeBuilder, text);
		final IParseGraph graph = new ParseGraph(goalRule, text.length());
		final Forrest3 newForrest = new Forrest3(graph, this.getRuntimeRuleSet(), input, goalRule);
		newForrest.start(graph, goalRule, input);
		int seasons = 1;

		do {
			newForrest.grow();
			seasons++;
		} while (newForrest.getCanGrow());

		final IGraphNode match = newForrest.getLongestMatch(text);
		return match;
	}

	@Override
	public List<IRuleItem> expectedAt(final String goalRuleName, final Reader inputReader, final int position) throws ParseFailedException, ParseTreeException {
		try {
			final INodeType goal = ((Grammar) this.getGrammar()).findRule(goalRuleName).getNodeType();
			if (null == this.runtimeRuleSet) {
				this.build();
			}
			final int goalRuleNumber = this.runtimeRuleSet.getRuleNumber(goal.getIdentity().asPrimitive());
			final RuntimeRule goalRule = this.runtimeRuleSet.getRuntimeRule(goalRuleNumber);

			final BufferedReader br = new BufferedReader(inputReader);
			String text = br.lines().collect(Collectors.joining(System.lineSeparator()));
			text = text.substring(0, Math.min(position, text.length()));
			final Input3 input = new Input3(this.runtimeBuilder, text);
			final IParseGraph graph = new ParseGraph(goalRule, text.length());
			final Forrest3 newForrest = new Forrest3(graph, this.getRuntimeRuleSet(), input, goalRule);
			newForrest.start(graph, goalRule, input);
			int seasons = 1;
			final int length = text.length();
			final List<IGraphNode> matches = new ArrayList<>();
			do {
				newForrest.grow();
				seasons++;
				for (final IGraphNode gn : newForrest.getLastGrown()) {
					if (length == gn.getNextInputPosition()) {
						matches.add(gn);
					}
				}
			} while (newForrest.getCanGrow());

			if (matches.isEmpty()) {

			}

			// TODO: when the last leaf is followed by the next expected leaf, if the result could be the last leaf
			// must reject the next expected

			final Set<RuntimeRule> expected = new HashSet<>();
			for (final IGraphNode ep : matches) {
				final IGraphNode gn = ep;
				boolean done = false;
				// while (!done) {
				if (gn.getCanGrowWidth()) {
					expected.addAll(gn.getNextExpectedItem());
					done = true;
					// TODO: sum from all parents
					// gn = gn.getPossibleParent().get(0).node;// .getNextExpectedItem();
				} else {
					// if has height potential?
					// gn = gn.getPossibleParent().get(0).node;

				}
				// }
			}
			// final List<RuntimeRule> expected = longest.getNextExpectedItem();
			final List<IRuleItem> ruleItems = new ArrayList<>();
			for (final RuntimeRule rr : expected) {
				if (RuntimeRuleKind.NON_TERMINAL == rr.getKind()) {
					final IRuleItem ri = this.runtimeRuleSet.getOriginalItem(rr, this.getGrammar());
					ruleItems.add(ri);
				} else {
					ruleItems.add(this.getGrammar().findAllTerminal(rr.getTerminalPatternText()));
				}
			}
			// add skip rules at end
			for (final RuntimeRule rr : this.runtimeRuleSet.getAllSkipRules()) {
				// final IRule r = this.grammar.findAllRule(rr.getName());
				final IRuleItem ri = this.runtimeRuleSet.getOriginalItem(rr, this.getGrammar());
				ruleItems.add(ri);
				// rules.add(new NonTerminalRuleReference(this.getGrammar(), rr.getName()));
			}
			return ruleItems;
		} catch (final RuleNotFoundException e) {
			// Should never happen!
			throw new RuntimeException("Should never happen", e);
		}
	}
}
