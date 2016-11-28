package net.akehurst.language.parse.graph;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import net.akehurst.language.core.parser.INodeType;
import net.akehurst.language.core.parser.RuleNotFoundException;
import net.akehurst.language.grammar.parse.tree.Leaf;
import net.akehurst.language.grammar.parser.ScannerLessParser3;
import net.akehurst.language.grammar.parser.converter.Converter;
import net.akehurst.language.grammar.parser.converter.Grammar2RuntimeRuleSet;
import net.akehurst.language.grammar.parser.forrest.Input3;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleSetBuilder;
import net.akehurst.language.ogl.semanticStructure.Grammar;
import net.akehurst.language.ogl.semanticStructure.GrammarBuilder;
import net.akehurst.language.ogl.semanticStructure.Namespace;
import net.akehurst.language.ogl.semanticStructure.NonTerminal;
import net.akehurst.language.ogl.semanticStructure.Rule;
import net.akehurst.language.ogl.semanticStructure.Terminal;
import net.akehurst.language.ogl.semanticStructure.TerminalLiteral;
import net.akehurst.language.ogl.semanticStructure.Visitor;

public class Test_ParseGraph {

	RuntimeRuleSetBuilder rules;

	@Before
	public void before() {
		this.rules = new RuntimeRuleSetBuilder();
	}

	@Test
	public void createLeaf_doesNotExist() {

		RuntimeRule terminalRule = rules.createRuntimeRule(new TerminalLiteral("a"));

		IParseGraph graph = new ParseGraph();
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

		RuntimeRule terminalRule = rules.createRuntimeRule(new TerminalLiteral("a"));

		IParseGraph graph = new ParseGraph();
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
			GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
			b.rule("a").concatenation(new TerminalLiteral("a"));
			Grammar g = b.get();
			Converter c = new Converter(this.rules);
			c.transformLeft2Right(Grammar2RuntimeRuleSet.class, g);

			RuntimeRule terminalRule = rules.getRuntimeRule(g.findAllTerminal("a"));
			RuntimeRule rule = rules.getRuntimeRule(g.findAllRule("a"));

			IParseGraph graph = new ParseGraph();
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
			GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
			b.rule("a").concatenation(new TerminalLiteral("a"));
			Grammar g = b.get();
			Converter c = new Converter(this.rules);
			c.transformLeft2Right(Grammar2RuntimeRuleSet.class, g);

			RuntimeRule terminalRule = rules.getRuntimeRule(g.findAllTerminal("a"));
			RuntimeRule rule = rules.getRuntimeRule(g.findAllRule("a"));

			IParseGraph graph = new ParseGraph();
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
}
