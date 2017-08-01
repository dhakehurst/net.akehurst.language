package net.akehurst.language.grammar.parser;

import java.io.BufferedReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import net.akehurst.language.core.grammar.IGrammar;
import net.akehurst.language.core.grammar.IRuleItem;
import net.akehurst.language.core.grammar.RuleNotFoundException;
import net.akehurst.language.core.parser.IBranch;
import net.akehurst.language.core.parser.INode;
import net.akehurst.language.core.parser.INodeType;
import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.core.parser.IParser;
import net.akehurst.language.core.parser.ParseFailedException;
import net.akehurst.language.core.parser.ParseTreeException;
import net.akehurst.language.grammar.parse.tree.IInput;
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
		this.runtimeBuilder = runtimeFactory;
		this.converter = new Converter(this.runtimeBuilder); // TODO: might not be needed here as it is set elsewhere, below
	}

	private final Converter converter;
	private final RuntimeRuleSetBuilder runtimeBuilder;
	private final IGrammar grammar;
	// Grammar pseudoGrammar;
	private RuntimeRuleSet runtimeRuleSet;

	private IGrammar getGrammar() {
		return this.grammar;
	}

	private RuntimeRuleSet getRuntimeRuleSet() {
		if (null == this.runtimeRuleSet) {
			this.build();
		}
		return this.runtimeRuleSet;
	}

	private void init() {
		try {
			this.runtimeRuleSet = this.converter.transformLeft2Right(Grammar2RuntimeRuleSet.class, (Grammar) this.grammar);
		} catch (final Exception e) {
			e.printStackTrace();

		}
	}

	private IParseTree parse2(final String goalRuleName, final IInput input) throws ParseFailedException, ParseTreeException {
		try {
			final INodeType goal = ((Grammar) this.getGrammar()).findRule(goalRuleName).getNodeType();
			final IGraphNode gr = this.parse3(goal, input);
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

	private void setParentForChildren(final IBranch node) {
		final IBranch parent = node;
		for (final INode child : parent.getChildren()) {
			child.setParent(parent);
			if (child instanceof IBranch) {
				this.setParentForChildren((IBranch) child);
			}
		}
	}

	private IGraphNode parse3(final INodeType goal, final IInput input) throws ParseFailedException, RuleNotFoundException, ParseTreeException {

		final int goalRuleNumber = this.getRuntimeRuleSet().getRuleNumber(goal.getIdentity().asPrimitive());
		final RuntimeRule goalRR = this.getRuntimeRuleSet().getRuntimeRule(goalRuleNumber);
		final IGraphNode node = this.doParse3(goalRR, input);
		// GraphNodeRoot gr = new GraphNodeRoot(goalRR, node.getChildren());
		return node;
	}

	private IGraphNode doParse3(final RuntimeRule goalRule, final IInput input) throws ParseFailedException, RuleNotFoundException, ParseTreeException {
		// TODO: handle reader directly without converting to string
		final IParseGraph graph = new ParseGraph(goalRule, input);
		final Forrest3 newForrest = new Forrest3(graph, this.getRuntimeRuleSet(), input, goalRule);
		newForrest.start(graph, goalRule, input);
		int seasons = 1;

		do {
			newForrest.grow();
			seasons++;
		} while (newForrest.getCanGrow());

		final IGraphNode match = newForrest.getLongestMatch();
		return match;
	}

	private List<IRuleItem> expectedAt1(final String goalRuleName, final IInput input, final int position) throws ParseFailedException, ParseTreeException {
		try {
			final INodeType goal = ((Grammar) this.getGrammar()).findRule(goalRuleName).getNodeType();
			if (null == this.runtimeRuleSet) {
				this.build();
			}
			final int goalRuleNumber = this.runtimeRuleSet.getRuleNumber(goal.getIdentity().asPrimitive());
			final RuntimeRule goalRule = this.runtimeRuleSet.getRuntimeRule(goalRuleNumber);

			final IParseGraph graph = new ParseGraph(goalRule, input);
			final Forrest3 newForrest = new Forrest3(graph, this.getRuntimeRuleSet(), input, goalRule);
			newForrest.start(graph, goalRule, input);
			int seasons = 1;
			// final int length = text.length();
			final List<IGraphNode> matches = new ArrayList<>();
			do {
				newForrest.grow();
				seasons++;
				for (final IGraphNode gn : newForrest.getLastGrown()) {
					// may need to change this to finalInputPos!
					if (input.getIsEnd(gn.getNextInputPosition())) {
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

	// --- IParser ---
	@Override
	public void build() {
		this.init();
		this.runtimeRuleSet.build();
	}

	@Override
	public Set<INodeType> getNodeTypes() {
		return this.getGrammar().findAllNodeType();
	}

	@Override
	public IParseTree parse(final String goalRuleName, final CharSequence inputText) throws ParseFailedException, ParseTreeException, RuleNotFoundException {
		final IInput input = new Input3(this.runtimeBuilder, inputText);
		final IParseTree tree = this.parse2(goalRuleName, input);
		return tree;
	}

	@Override
	public IParseTree parse(final String goalRuleName, final Reader inputText) throws ParseFailedException, ParseTreeException, RuleNotFoundException {
		// TODO: find a way to parse straight from the input, without reading it all
		final BufferedReader br = new BufferedReader(inputText);
		final String text = br.lines().collect(Collectors.joining(System.lineSeparator()));

		final Input3 input = new Input3(this.runtimeBuilder, text);
		final IParseTree tree = this.parse2(goalRuleName, input);
		return tree;
	}

	@Override
	public List<IRuleItem> expectedAt(final String goalRuleName, final CharSequence inputText, final int position)
			throws ParseFailedException, ParseTreeException {
		final CharSequence text = inputText.subSequence(0, Math.min(position, inputText.length()));
		final Input3 input = new Input3(this.runtimeBuilder, text);
		return this.expectedAt1(goalRuleName, input, position);
	}

	@Override
	public List<IRuleItem> expectedAt(final String goalRuleName, final Reader inputReader, final int position) throws ParseFailedException, ParseTreeException {
		final BufferedReader br = new BufferedReader(inputReader);
		String text = br.lines().collect(Collectors.joining(System.lineSeparator()));
		text = text.substring(0, Math.min(position, text.length()));
		final Input3 input = new Input3(this.runtimeBuilder, text);
		return this.expectedAt1(goalRuleName, input, position);
	}
}
