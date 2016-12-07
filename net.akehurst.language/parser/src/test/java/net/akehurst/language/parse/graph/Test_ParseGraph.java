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
		RuntimeRuleSetBuilder rules = new RuntimeRuleSetBuilder();
		RuntimeRule terminalRule = rules.createRuntimeRule(new TerminalLiteral("a"));

		IParseGraph graph = new ParseGraph(terminalRule,1);
		Input3 input = new Input3(rules, "a");

		int startPosition = 0;
		int matchedLength = 1;
		Leaf l = input.fetchOrCreateBud(terminalRule, 0);
		IGraphNode n = graph.findOrCreateLeaf(l, terminalRule, startPosition, matchedLength);

		Assert.assertNotNull(n);
		Assert.assertEquals(terminalRule, n.getRuntimeRule());
	}

	@Test
	public void createLeaf_alreadyExists() {
		RuntimeRuleSetBuilder rules = new RuntimeRuleSetBuilder();

		RuntimeRule terminalRule = rules.createRuntimeRule(new TerminalLiteral("a"));

		IParseGraph graph = new ParseGraph(terminalRule,1);
		Input3 input = new Input3(rules, "a");

		int startPosition = 0;
		int matchedLength = 1;
		Leaf l = input.fetchOrCreateBud(terminalRule, 0);
		IGraphNode n1 = graph.findOrCreateLeaf(l, terminalRule, startPosition, matchedLength);

		IGraphNode n2 = graph.findOrCreateLeaf(l, terminalRule, startPosition, matchedLength);

		Assert.assertNotNull(n1);
		Assert.assertNotNull(n2);
		Assert.assertTrue(n1 == n2);
	}

	@Test
	public void createBranch() {
		try {
			RuntimeRuleSetBuilder rules = new RuntimeRuleSetBuilder();
			GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
			b.rule("a").concatenation(new TerminalLiteral("a"));
			Grammar g = b.get();
			Converter c = new Converter(rules);
			c.transformLeft2Right(Grammar2RuntimeRuleSet.class, g);

			RuntimeRule terminalRule = rules.getRuntimeRule(g.findAllTerminal("a"));
			RuntimeRule rule = rules.getRuntimeRule(g.findAllRule("a"));

			IParseGraph graph = new ParseGraph(rule,1);
			Input3 input = new Input3(rules, "a");

			int startPosition = 0;
			int length = 1;
			int nextItemIndex = 1;
			int height = 1;
			IGraphNode n = graph.createBranch(rule, 0, startPosition, length, nextItemIndex, height);

			Assert.assertNotNull(n);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void addChild_resultDoesNotExist() {

		try {
			RuntimeRuleSetBuilder rules = new RuntimeRuleSetBuilder();
			GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
			b.rule("a").concatenation(new TerminalLiteral("a"));
			Grammar g = b.get();
			Converter c = new Converter(rules);
			c.transformLeft2Right(Grammar2RuntimeRuleSet.class, g);

			RuntimeRule terminalRule = rules.getRuntimeRule(g.findAllTerminal("a"));
			RuntimeRule rule = rules.getRuntimeRule(g.findAllRule("a"));

			IParseGraph graph = new ParseGraph(rule,1);
			Input3 input = new Input3(rules, "a");

			int startPosition = 0;
			int length = 1;
			int nextItemIndex = 1;
			int height = 1;
			IGraphNode n = graph.createBranch(rule, 0, startPosition, length, nextItemIndex, height);
			Leaf l = input.fetchOrCreateBud(terminalRule, 0);
			IGraphNode n2 = graph.findOrCreateLeaf(l, terminalRule, startPosition, l.getMatchedTextLength());

			IGraphNode n3 = n.duplicateWithNextChild(n2);

			Assert.assertNotNull(n3);
			Assert.assertTrue(n3 != n);
			Assert.assertTrue(n3.getChildren().size() == 1);
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}

	}

	@Test
	public void left_recursion() {
		try {
			RuntimeRuleSetBuilder rules = new RuntimeRuleSetBuilder();
			GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
			b.rule("S").choice(new TerminalLiteral("a"), new NonTerminal("S$group"));
			b.rule("S$group").concatenation(new NonTerminal("S"), new TerminalLiteral("a"));
			Grammar g = b.get();
			Converter c = new Converter(rules);
			c.transformLeft2Right(Grammar2RuntimeRuleSet.class, g);

			RuntimeRule goalRule = rules.getRuntimeRule(g.findAllRule("S"));
			IParseGraph graph = new ParseGraph(goalRule,1);
			Input3 input = new Input3(rules, "aaa");
			
			RuntimeRule terminal_a = rules.getRuntimeRule(g.findAllTerminal("a"));
			RuntimeRule rule_S = rules.getRuntimeRule(g.findAllRule("S"));
			RuntimeRule rule_S_group = rules.getRuntimeRule(g.findAllRule("S$group"));
			// create leaf at same position as existing, but with different stack?
			// e.g. parse empty B followed by 'b' B (see special test)
			
			//start
			IGraphNode node_start = graph.createBranch(goalRule, 0, 0, 0, 0, 0);
			
			//grow width
			graph.getGrowable().clear();
			Leaf leaf_a = input.fetchOrCreateBud(terminal_a, 0);
			IGraphNode node_a = graph.findOrCreateLeaf(leaf_a, terminal_a, 0, leaf_a.getMatchedTextLength());
			node_start.pushToStackOf(node_a, 0);
			
			//grow height
			graph.getGrowable().clear();
			IGraphNode node_B_empty = graph.createWithFirstChild(rule_B, 0, node_empty);
	
			//graft back
			graph.getGrowable().clear();
			IGraphNode node_start_1 = node_start.duplicateWithNextChild(node_B_empty);
			
			//grow width
			graph.getGrowable().clear();
			Leaf leaf_b = input.fetchOrCreateBud(terminal_b, 0);
			IGraphNode node_b = graph.findOrCreateLeaf(leaf_b, terminal_b, 0, leaf_b.getMatchedTextLength());
			node_start_1.pushToStackOf(node_b, 1);
			
			//grow height
			graph.getGrowable().clear();
			IGraphNode node_B = graph.createWithFirstChild(rule_B, 0, node_b);

			//graft back
			graph.getGrowable().clear();
			IGraphNode node_start_2 = node_start_1.duplicateWithNextChild(node_B);
			
			Assert.assertTrue(false);
			
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void empty_issues() {
		try {
			RuntimeRuleSetBuilder rules = new RuntimeRuleSetBuilder();
			GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
			b.rule("S").concatenation(new NonTerminal("B"), new NonTerminal("B"));
			b.rule("B").multi(0, 1, new TerminalLiteral("b"));
			Grammar g = b.get();
			Converter c = new Converter(rules);
			c.transformLeft2Right(Grammar2RuntimeRuleSet.class, g);

			RuntimeRule goalRule = rules.getRuntimeRule(g.findAllRule("S"));
			IParseGraph graph = new ParseGraph(goalRule,1);
			Input3 input = new Input3(rules, "b");
			
			RuntimeRule terminal_b = rules.getRuntimeRule(g.findAllTerminal("b"));
			RuntimeRule rule_B = rules.getRuntimeRule(g.findAllRule("B"));
			RuntimeRule rule_B_empty = rules.getRuntimeRuleSet().getEmptyRule(rule_B);
			// create leaf at same position as existing, but with different stack?
			// e.g. parse empty B followed by 'b' B (see special test)
			
			//start
			IGraphNode node_start = graph.createBranch(goalRule, 0, 0, 0, 0, 0);
			
			//grow width
			graph.getGrowable().clear();
			Leaf leaf_empty = input.fetchOrCreateBud(rule_B_empty, 0);
			IGraphNode node_empty = graph.findOrCreateLeaf(leaf_empty, rule_B_empty, 0, leaf_empty.getMatchedTextLength());
			node_start.pushToStackOf(node_empty, 0);
			
			//grow height
			graph.getGrowable().clear();
			IGraphNode node_B_empty = graph.createWithFirstChild(rule_B, 0, node_empty);
	
			//graft back
			graph.getGrowable().clear();
			IGraphNode node_start_1 = node_start.duplicateWithNextChild(node_B_empty);
			
			//grow width
			graph.getGrowable().clear();
			Leaf leaf_b = input.fetchOrCreateBud(terminal_b, 0);
			IGraphNode node_b = graph.findOrCreateLeaf(leaf_b, terminal_b, 0, leaf_b.getMatchedTextLength());
			node_start_1.pushToStackOf(node_b, 1);
			
			//grow height
			graph.getGrowable().clear();
			IGraphNode node_B = graph.createWithFirstChild(rule_B, 0, node_b);

			//graft back
			graph.getGrowable().clear();
			IGraphNode node_start_2 = node_start_1.duplicateWithNextChild(node_B);
			
			Assert.assertTrue(false);
			
		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}
	}
}
