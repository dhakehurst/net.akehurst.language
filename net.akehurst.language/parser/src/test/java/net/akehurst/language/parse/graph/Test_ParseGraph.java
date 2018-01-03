package net.akehurst.language.parse.graph;

import java.util.Collections;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

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
import net.akehurst.language.parse.graph.IGrowingNode.PreviousInfo;
import net.akehurst.language.parser.sppf.Leaf;
import net.akehurst.transform.binary.RuleNotFoundException;
import net.akehurst.transform.binary.TransformException;

public class Test_ParseGraph {

	@Before
	public void setup() {

	}

	@Test
	public void createStart() {
		final RuntimeRuleSetBuilder rules = new RuntimeRuleSetBuilder();
		final RuntimeRule terminalRule = rules.createRuntimeRule(new TerminalLiteral("a"));

		final Input3 input = new Input3(rules, "a");
		final IParseGraph graph = new ParseGraph(terminalRule, input);

		graph.createStart(terminalRule);
		Assert.assertNotNull(graph.getGrowingHead().iterator().next());
		Assert.assertEquals(terminalRule, graph.getGrowingHead().iterator().next().getRuntimeRule());
	}

	@Test
	public void createLeaf_doesNotExist() {
		final RuntimeRuleSetBuilder rules = new RuntimeRuleSetBuilder();
		final RuntimeRule terminalRule = rules.createRuntimeRule(new TerminalLiteral("a"));

		final Input3 input = new Input3(rules, "a");
		final IParseGraph graph = new ParseGraph(terminalRule, input);

		final Leaf l = input.fetchOrCreateBud(terminalRule, 0);
		final ICompleteNode n = graph.findOrCreateLeaf(l);

		Assert.assertNotNull(n);
		Assert.assertEquals(terminalRule, n.getRuntimeRule());
	}

	@Test
	public void createLeaf_alreadyExists() {
		final RuntimeRuleSetBuilder rules = new RuntimeRuleSetBuilder();

		final RuntimeRule terminalRule = rules.createRuntimeRule(new TerminalLiteral("a"));

		final Input3 input = new Input3(rules, "a");
		final IParseGraph graph = new ParseGraph(terminalRule, input);

		final Leaf l = input.fetchOrCreateBud(terminalRule, 0);
		final ICompleteNode n1 = graph.findOrCreateLeaf(l);

		final ICompleteNode n2 = graph.findOrCreateLeaf(l);

		Assert.assertNotNull(n1);
		Assert.assertNotNull(n2);
		Assert.assertTrue(n1 == n2);
	}

	@Test
	public void createWithFirstChild() throws RuleNotFoundException, TransformException, net.akehurst.language.core.grammar.RuleNotFoundException {

		final RuntimeRuleSetBuilder rules = new RuntimeRuleSetBuilder();
		final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("A").concatenation(new TerminalLiteral("a"));
		final Grammar g = b.get();
		final Converter c = new Converter(rules);
		c.transformLeft2Right(Grammar2RuntimeRuleSet.class, g);

		final RuntimeRule terminalRule = rules.getRuntimeRule(g.findAllTerminal("a"));
		final RuntimeRule rule = rules.getRuntimeRule(g.findAllRule("A"));

		final Input3 input = new Input3(rules, "a");
		final IParseGraph graph = new ParseGraph(rule, input);

		// grow leaf
		final Leaf l = input.fetchOrCreateBud(terminalRule, 0);
		final ICompleteNode ln = graph.findOrCreateLeaf(l);

		final RuntimeRule runtimeRule = rule;
		final int priority = 0;
		final ICompleteNode firstChild = ln;
		final Set<PreviousInfo> previous = Collections.emptySet();
		graph.createWithFirstChild(runtimeRule, priority, firstChild, previous);

		Assert.assertEquals(1, graph.getGrowingHead().size());
		Assert.assertEquals(rule, graph.getGrowingHead().iterator().next().getRuntimeRule());
		Assert.assertEquals(1, graph.getGrowingHead().iterator().next().getGrowingChildren().size());
		// TODO: more checks
	}

	@Test
	public void left_recursion() throws Exception {

		final RuntimeRuleSetBuilder rules = new RuntimeRuleSetBuilder();
		final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("S").choice(new TerminalLiteral("a"), new NonTerminal("S$group"));
		b.rule("S$group").concatenation(new NonTerminal("S"), new TerminalLiteral("a"));
		final Grammar g = b.get();
		final Converter c = new Converter(rules);
		c.transformLeft2Right(Grammar2RuntimeRuleSet.class, g);

		final RuntimeRule goalRule = rules.getRuntimeRule(g.findAllRule("S"));
		final Input3 input = new Input3(rules, "aaa");
		final IParseGraph graph = new ParseGraph(goalRule, input);

		final RuntimeRule terminal_a = rules.getRuntimeRule(g.findAllTerminal("a"));
		final RuntimeRule rule_S = rules.getRuntimeRule(g.findAllRule("S"));
		final RuntimeRule rule_S_group = rules.getRuntimeRule(g.findAllRule("S$group"));

		// start
		final ICompleteNode node_start = graph.findOrCreateBranch(goalRule, 0, 0, 0, 0);

		// grow width
		graph.getGrowable().clear();
		final Leaf leaf_a = input.fetchOrCreateBud(terminal_a, 0);
		final ICompleteNode node_a = graph.findOrCreateLeaf(leaf_a);
		node_start.pushToStackOf(node_a, 0);

		Assert.fail("Incomplete test");

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
			final Input3 input = new Input3(rules, "b");
			final IParseGraph graph = new ParseGraph(goalRule, input);

			final RuntimeRule terminal_b = rules.getRuntimeRule(g.findAllTerminal("b"));
			final RuntimeRule rule_B = rules.getRuntimeRule(g.findAllRule("B"));
			final RuntimeRule rule_B_empty = rules.getRuntimeRuleSet().getEmptyRule(rule_B);
			// create leaf at same position as existing, but with different stack?
			// e.g. parse empty B followed by 'b' B (see special test)

			// start
			graph.createStart(goalRule);

			// grow width
			final IGrowingNode gn = graph.getGrowingHead().iterator().next();
			final Leaf leaf = input.fetchOrCreateBud(terminal_b, 0);
			final ICompleteNode leafNode = graph.findOrCreateLeaf(leaf);
			final IGrowingNode stack = gn;
			final Set<PreviousInfo> previous = Collections.emptySet();
			graph.pushToStackOf(leafNode, stack, previous);

			// grow height
			// graft back

			// grow width

			// grow height
			graph.getGrowable().clear();
			graph.createWithFirstChild(rule_B, 0, node_b);
			final IGraphNode node_B = graph.findNode(rule_B.getRuleNumber(), 1);
			// graft back
			graph.getGrowable().clear();
			graph.growNextChild(node_start_1, node_B, 1);
			final IGraphNode node_start_2 = graph.findNode(rule_B.getRuleNumber(), 2);

			Assert.fail("Incomplete test");

		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}
}
