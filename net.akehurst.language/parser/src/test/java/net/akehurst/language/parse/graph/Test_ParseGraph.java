package net.akehurst.language.parse.graph;

import org.junit.Assert;
import org.junit.Test;

import net.akehurst.language.grammar.parse.tree.Leaf;
import net.akehurst.language.grammar.parser.converter.Converter;
import net.akehurst.language.grammar.parser.converter.Grammar2RuntimeRuleSet;
import net.akehurst.language.grammar.parser.forrest.Input3;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleSetBuilder;
import net.akehurst.language.ogl.semanticStructure.Grammar;
import net.akehurst.language.ogl.semanticStructure.GrammarBuilder;
import net.akehurst.language.ogl.semanticStructure.Namespace;
import net.akehurst.language.ogl.semanticStructure.NonTerminal;
import net.akehurst.language.ogl.semanticStructure.TerminalLiteral;

public class Test_ParseGraph {

	@Test
	public void createLeaf_doesNotExist() {
		final RuntimeRuleSetBuilder rules = new RuntimeRuleSetBuilder();
		final RuntimeRule terminalRule = rules.createRuntimeRule(new TerminalLiteral("a"));

		final IParseGraph graph = new ParseGraph(terminalRule, 1);
		final Input3 input = new Input3(rules, "a");

		final int startPosition = 0;
		final int matchedLength = 1;
		final Leaf l = input.fetchOrCreateBud(terminalRule, 0);
		final IGraphNode n = graph.findOrCreateLeaf(l, terminalRule, startPosition, matchedLength);

		Assert.assertNotNull(n);
		Assert.assertEquals(terminalRule, n.getRuntimeRule());
	}

	@Test
	public void createLeaf_alreadyExists() {
		final RuntimeRuleSetBuilder rules = new RuntimeRuleSetBuilder();

		final RuntimeRule terminalRule = rules.createRuntimeRule(new TerminalLiteral("a"));

		final IParseGraph graph = new ParseGraph(terminalRule, 1);
		final Input3 input = new Input3(rules, "a");

		final int startPosition = 0;
		final int matchedLength = 1;
		final Leaf l = input.fetchOrCreateBud(terminalRule, 0);
		final IGraphNode n1 = graph.findOrCreateLeaf(l, terminalRule, startPosition, matchedLength);

		final IGraphNode n2 = graph.findOrCreateLeaf(l, terminalRule, startPosition, matchedLength);

		Assert.assertNotNull(n1);
		Assert.assertNotNull(n2);
		Assert.assertTrue(n1 == n2);
	}

	@Test
	public void createBranch() {
		try {
			final RuntimeRuleSetBuilder rules = new RuntimeRuleSetBuilder();
			final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
			b.rule("a").concatenation(new TerminalLiteral("a"));
			final Grammar g = b.get();
			final Converter c = new Converter(rules);
			c.transformLeft2Right(Grammar2RuntimeRuleSet.class, g);

			final RuntimeRule terminalRule = rules.getRuntimeRule(g.findAllTerminal("a"));
			final RuntimeRule rule = rules.getRuntimeRule(g.findAllRule("a"));

			final IParseGraph graph = new ParseGraph(rule, 1);
			final Input3 input = new Input3(rules, "a");

			final int startPosition = 0;
			final int length = 1;
			final int nextItemIndex = 1;
			final int height = 1;
			final IGraphNode n = graph.createBranch(rule, 0, startPosition, length, nextItemIndex, height);

			Assert.assertNotNull(n);
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void addChild_resultDoesNotExist() {

		try {
			final RuntimeRuleSetBuilder rules = new RuntimeRuleSetBuilder();
			final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
			b.rule("a").concatenation(new TerminalLiteral("a"));
			final Grammar g = b.get();
			final Converter c = new Converter(rules);
			c.transformLeft2Right(Grammar2RuntimeRuleSet.class, g);

			final RuntimeRule terminalRule = rules.getRuntimeRule(g.findAllTerminal("a"));
			final RuntimeRule rule = rules.getRuntimeRule(g.findAllRule("a"));

			final IParseGraph graph = new ParseGraph(rule, 1);
			final Input3 input = new Input3(rules, "a");

			final int startPosition = 0;
			final int length = 1;
			final int nextItemIndex = 1;
			final int height = 1;
			final IGraphNode n = graph.createBranch(rule, 0, startPosition, length, nextItemIndex, height);
			final Leaf l = input.fetchOrCreateBud(terminalRule, 0);
			final IGraphNode n2 = graph.findOrCreateLeaf(l, terminalRule, startPosition, l.getMatchedTextLength());

			final IGraphNode n3 = n.duplicateWithNextChild(n2);

			Assert.assertNotNull(n3);
			Assert.assertTrue(n3 != n);
			Assert.assertTrue(n3.getChildren().size() == 1);
		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}

	}

	@Test
	public void left_recursion() {
		try {
			final RuntimeRuleSetBuilder rules = new RuntimeRuleSetBuilder();
			final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
			b.rule("S").choice(new TerminalLiteral("a"), new NonTerminal("S$group"));
			b.rule("S$group").concatenation(new NonTerminal("S"), new TerminalLiteral("a"));
			final Grammar g = b.get();
			final Converter c = new Converter(rules);
			c.transformLeft2Right(Grammar2RuntimeRuleSet.class, g);

			final RuntimeRule goalRule = rules.getRuntimeRule(g.findAllRule("S"));
			final IParseGraph graph = new ParseGraph(goalRule, 1);
			final Input3 input = new Input3(rules, "aaa");

			final RuntimeRule terminal_a = rules.getRuntimeRule(g.findAllTerminal("a"));
			final RuntimeRule rule_S = rules.getRuntimeRule(g.findAllRule("S"));
			final RuntimeRule rule_S_group = rules.getRuntimeRule(g.findAllRule("S$group"));

			// start
			final IGraphNode node_start = graph.createBranch(goalRule, 0, 0, 0, 0, 0);

			// grow width
			graph.getGrowable().clear();
			final Leaf leaf_a = input.fetchOrCreateBud(terminal_a, 0);
			final IGraphNode node_a = graph.findOrCreateLeaf(leaf_a, terminal_a, 0, leaf_a.getMatchedTextLength());
			node_start.pushToStackOf(node_a, 0);

			Assert.assertTrue(false);

		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void empty_issues() {
		try {
			final RuntimeRuleSetBuilder rules = new RuntimeRuleSetBuilder();
			final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
			b.rule("S").concatenation(new NonTerminal("B"), new NonTerminal("B"));
			b.rule("B").multi(0, 1, new TerminalLiteral("b"));
			final Grammar g = b.get();
			final Converter c = new Converter(rules);
			c.transformLeft2Right(Grammar2RuntimeRuleSet.class, g);

			final RuntimeRule goalRule = rules.getRuntimeRule(g.findAllRule("S"));
			final IParseGraph graph = new ParseGraph(goalRule, 1);
			final Input3 input = new Input3(rules, "b");

			final RuntimeRule terminal_b = rules.getRuntimeRule(g.findAllTerminal("b"));
			final RuntimeRule rule_B = rules.getRuntimeRule(g.findAllRule("B"));
			final RuntimeRule rule_B_empty = rules.getRuntimeRuleSet().getEmptyRule(rule_B);
			// create leaf at same position as existing, but with different stack?
			// e.g. parse empty B followed by 'b' B (see special test)

			// start
			final IGraphNode node_start = graph.createBranch(goalRule, 0, 0, 0, 0, 0);

			// grow width
			graph.getGrowable().clear();
			final Leaf leaf_empty = input.fetchOrCreateBud(rule_B_empty, 0);
			final IGraphNode node_empty = graph.findOrCreateLeaf(leaf_empty, rule_B_empty, 0, leaf_empty.getMatchedTextLength());
			node_start.pushToStackOf(node_empty, 0);

			// grow height
			graph.getGrowable().clear();
			final IGraphNode node_B_empty = graph.createWithFirstChild(rule_B, 0, node_empty);

			// graft back
			graph.getGrowable().clear();
			final IGraphNode node_start_1 = node_start.duplicateWithNextChild(node_B_empty);

			// grow width
			graph.getGrowable().clear();
			final Leaf leaf_b = input.fetchOrCreateBud(terminal_b, 0);
			final IGraphNode node_b = graph.findOrCreateLeaf(leaf_b, terminal_b, 0, leaf_b.getMatchedTextLength());
			node_start_1.pushToStackOf(node_b, 1);

			// grow height
			graph.getGrowable().clear();
			final IGraphNode node_B = graph.createWithFirstChild(rule_B, 0, node_b);

			// graft back
			graph.getGrowable().clear();
			final IGraphNode node_start_2 = node_start_1.duplicateWithNextChild(node_B);

			Assert.assertTrue(false);

		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}
}
