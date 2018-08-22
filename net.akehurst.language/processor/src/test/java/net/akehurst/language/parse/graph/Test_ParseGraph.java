package net.akehurst.language.parse.graph;

import java.util.Collections;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import net.akehurst.language.grammar.parser.converter.Converter;
import net.akehurst.language.grammar.parser.converter.rules.Grammar2RuntimeRuleSet;
import net.akehurst.language.grammar.parser.forrest.InputFromCharSequence;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleSetBuilder;
import net.akehurst.language.ogl.semanticStructure.GrammarDefault;
import net.akehurst.language.ogl.semanticStructure.GrammarBuilderDefault;
import net.akehurst.language.ogl.semanticStructure.NamespaceDefault;
import net.akehurst.language.ogl.semanticStructure.NonTerminalDefault;
import net.akehurst.language.ogl.semanticStructure.TerminalLiteralDefault;
import net.akehurst.language.parse.graph.IGrowingNode.PreviousInfo;
import net.akehurst.language.parser.sppf.Leaf;

public class Test_ParseGraph {

    @Before
    public void setup() {

    }

    @Test
    public void createStart() {
        final RuntimeRuleSetBuilder rules = new RuntimeRuleSetBuilder();
        final RuntimeRule terminalRule = rules.createRuntimeRule(new TerminalLiteralDefault("a"));

        final InputFromCharSequence input = new InputFromCharSequence(rules, "a");
        final IParseGraph graph = new ParseGraph(terminalRule, input);

        graph.createStart(terminalRule);
        Assert.assertNotNull(graph.getGrowingHead().iterator().next());
        Assert.assertEquals(terminalRule, graph.getGrowingHead().iterator().next().getRuntimeRule());
    }

    @Test
    public void createLeaf_doesNotExist() {
        final RuntimeRuleSetBuilder rules = new RuntimeRuleSetBuilder();
        final RuntimeRule terminalRule = rules.createRuntimeRule(new TerminalLiteralDefault("a"));

        final InputFromCharSequence input = new InputFromCharSequence(rules, "a");
        final IParseGraph graph = new ParseGraph(terminalRule, input);

        final Leaf l = input.fetchOrCreateBud(terminalRule, 0);
        final ICompleteNode n = graph.findOrCreateLeaf(l);

        Assert.assertNotNull(n);
        Assert.assertEquals(terminalRule, n.getRuntimeRule());
    }

    @Test
    public void createLeaf_alreadyExists() {
        final RuntimeRuleSetBuilder rules = new RuntimeRuleSetBuilder();

        final RuntimeRule terminalRule = rules.createRuntimeRule(new TerminalLiteralDefault("a"));

        final InputFromCharSequence input = new InputFromCharSequence(rules, "a");
        final IParseGraph graph = new ParseGraph(terminalRule, input);

        final Leaf l = input.fetchOrCreateBud(terminalRule, 0);
        final ICompleteNode n1 = graph.findOrCreateLeaf(l);

        final ICompleteNode n2 = graph.findOrCreateLeaf(l);

        Assert.assertNotNull(n1);
        Assert.assertNotNull(n2);
        Assert.assertTrue(n1 == n2);
    }

    @Test
    public void createWithFirstChild() {

        final RuntimeRuleSetBuilder rules = new RuntimeRuleSetBuilder();
        final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
        b.rule("A").concatenation(new TerminalLiteralDefault("a"));
        final GrammarDefault g = b.get();
        final Converter c = new Converter(rules);
        c.transformLeft2Right(Grammar2RuntimeRuleSet.class, g);

        final RuntimeRule terminalRule = rules.getRuntimeRule(g.findAllTerminal("a"));
        final RuntimeRule rule = rules.getRuntimeRule(g.findAllRule("A"));

        final InputFromCharSequence input = new InputFromCharSequence(rules, "a");
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
        final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
        b.rule("S").choice(new TerminalLiteralDefault("a"), new NonTerminalDefault("S$group"));
        b.rule("S$group").concatenation(new NonTerminalDefault("S"), new TerminalLiteralDefault("a"));
        final GrammarDefault g = b.get();
        final Converter c = new Converter(rules);
        c.transformLeft2Right(Grammar2RuntimeRuleSet.class, g);

        final RuntimeRule goalRule = rules.getRuntimeRule(g.findAllRule("S"));
        final InputFromCharSequence input = new InputFromCharSequence(rules, "aaa");
        final IParseGraph graph = new ParseGraph(goalRule, input);

        final RuntimeRule terminal_a = rules.getRuntimeRule(g.findAllTerminal("a"));
        final RuntimeRule rule_S = rules.getRuntimeRule(g.findAllRule("S"));
        final RuntimeRule rule_S_group = rules.getRuntimeRule(g.findAllRule("S$group"));

        // start
        graph.createStart(goalRule);

        // season 1
        final IGrowingNode gn = graph.getGrowingHead().iterator().next();
        graph.getGrowingHead().clear();
        final Set<IGrowingNode.PreviousInfo> previous = graph.pop(gn);
        // grow width
        final Leaf leaf_a = input.fetchOrCreateBud(terminal_a, 0);
        final ICompleteNode node_a = graph.findOrCreateLeaf(leaf_a);
        graph.pushToStackOf(node_a, gn, previous);

        Assert.fail("Incomplete test");

    }

    @Test
    public void empty_issues() {
        try {
            final RuntimeRuleSetBuilder rules = new RuntimeRuleSetBuilder();
            final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
            b.rule("S").concatenation(new NonTerminalDefault("B"), new NonTerminalDefault("B"));
            b.rule("B").multi(0, 1, new TerminalLiteralDefault("b"));
            final GrammarDefault g = b.get();
            final Converter c = new Converter(rules);
            c.transformLeft2Right(Grammar2RuntimeRuleSet.class, g);

            final RuntimeRule goalRule = rules.getRuntimeRule(g.findAllRule("S"));
            final InputFromCharSequence input = new InputFromCharSequence(rules, "b");
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
            // graph.getGrowable().clear();
            // graph.createWithFirstChild(rule_B, 0, node_b);
            // final IGraphNode node_B = graph.findNode(rule_B.getRuleNumber(), 1);
            // graft back
            // graph.getGrowable().clear();
            // graph.growNextChild(node_start_1, node_B, 1);
            // final IGraphNode node_start_2 = graph.findNode(rule_B.getRuleNumber(), 2);

            Assert.fail("Incomplete test");

        } catch (final Exception e) {
            Assert.fail(e.getMessage());
        }
    }
}