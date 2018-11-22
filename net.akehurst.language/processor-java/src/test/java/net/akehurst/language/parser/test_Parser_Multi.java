/**
 * Copyright (C) 2015 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.akehurst.language.parser;

import java.util.Objects;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import net.akehurst.language.api.parser.ParseFailedException;
import net.akehurst.language.api.sppt.SPPTBranch;
import net.akehurst.language.api.sppt.SharedPackedParseTree;
import net.akehurst.language.parser.sppf.ParseTreeBuilder;
import net.akehurst.language.ogl.semanticStructure.GrammarBuilderDefault;
import net.akehurst.language.ogl.semanticStructure.GrammarDefault;
import net.akehurst.language.ogl.semanticStructure.NamespaceDefault;
import net.akehurst.language.ogl.semanticStructure.NonTerminalDefault;
import net.akehurst.language.ogl.semanticStructure.TerminalLiteralDefault;
import net.akehurst.language.parser.sppf.SharedPackedParseTreeSimple;

public class test_Parser_Multi extends AbstractParser_Test {

    @Rule
    public TestName testName = new TestName();

    GrammarDefault ab01() {
        final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
        b.rule("ab01").concatenation(new NonTerminalDefault("a"), new NonTerminalDefault("b01"));
        b.rule("b01").multi(0, 1, new NonTerminalDefault("b"));
        b.rule("a").concatenation(new TerminalLiteralDefault("a"));
        b.rule("b").concatenation(new TerminalLiteralDefault("b"));

        return b.get();
    }

    GrammarDefault ab01_2() {
        final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
        b.rule("ab01$group1").concatenation(new NonTerminalDefault("a"), new NonTerminalDefault("b"));
        b.rule("ab01").choice(new NonTerminalDefault("ab01$group1"), new NonTerminalDefault("a"));
        b.rule("a").concatenation(new TerminalLiteralDefault("a"));
        b.rule("b").concatenation(new TerminalLiteralDefault("b"));

        return b.get();
    }

    GrammarDefault as13() {
        final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
        b.rule("as").multi(1, 3, new NonTerminalDefault("a"));
        b.rule("a").concatenation(new TerminalLiteralDefault("a"));

        return b.get();
    }

    GrammarDefault as0n() {
        final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
        b.rule("as").multi(0, -1, new NonTerminalDefault("a"));
        b.rule("a").concatenation(new TerminalLiteralDefault("a"));

        return b.get();
    }

    GrammarDefault as0nbs0n() {
        final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
        b.rule("asbs").concatenation(new NonTerminalDefault("as"), new NonTerminalDefault("bs"));
        b.rule("as").multi(0, -1, new NonTerminalDefault("a"));
        b.rule("bs").multi(0, -1, new NonTerminalDefault("b"));
        b.rule("a").concatenation(new TerminalLiteralDefault("a"));
        b.rule("b").concatenation(new TerminalLiteralDefault("b"));

        return b.get();
    }

    GrammarDefault abs1m1() {
        final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
        b.rule("abs").multi(1, -1, new NonTerminalDefault("ab"));
        b.rule("ab").choice(new NonTerminalDefault("a"), new NonTerminalDefault("b"));
        b.rule("a").concatenation(new TerminalLiteralDefault("a"));
        b.rule("b").concatenation(new TerminalLiteralDefault("b"));

        return b.get();
    }

    @Test
    public void as0n_as_empty() throws ParseFailedException {
        // grammar, goal, input

        final GrammarDefault g = this.as0n();
        final String goal = "as";
        final String text = "";

        final SharedPackedParseTree tree = this.process(g, text, goal);
        Assert.assertNotNull(tree);

        final ParseTreeBuilder b = this.builder(g, text, goal);

        final SPPTBranch expected = b.branch("as", b.emptyLeaf("as"));
        Assert.assertEquals(expected, tree.getRoot());

    }

    @Test
    public void as0nbs0n_asbs_empty() throws ParseFailedException {
        // grammar, goal, input

        final GrammarDefault g = this.as0nbs0n();
        final String goal = "asbs";
        final String text = "";

        final SharedPackedParseTree tree = this.process(g, text, goal);
        Assert.assertNotNull(tree);

        final ParseTreeBuilder b = this.builder(g, text, goal);

        final SPPTBranch expected = b.branch("asbs", b.branch("as", b.emptyLeaf("as")), b.branch("bs", b.emptyLeaf("bs")));
        Assert.assertEquals(expected, tree.getRoot());

    }

    @Test
    public void as0nbs0n_asbs_b() throws ParseFailedException {
        // grammar, goal, input

        final GrammarDefault g = this.as0nbs0n();
        final String goal = "asbs";
        final String text = "b";

        final SharedPackedParseTree tree = this.process(g, text, goal);
        Assert.assertNotNull(tree);

        final ParseTreeBuilder b = this.builder(g, text, goal);
        ;
        final SPPTBranch expected = b.branch("asbs", b.branch("as", b.emptyLeaf("as")), b.branch("bs", b.branch("b", b.leaf("b"))));
        Assert.assertEquals(expected, tree.getRoot());

    }

    @Test
    public void as0nbs0n_asbs_bb() throws ParseFailedException {
        // grammar, goal, input

        final GrammarDefault g = this.as0nbs0n();
        final String goal = "asbs";
        final String text = "bb";

        final SharedPackedParseTree tree = this.process(g, text, goal);
        Assert.assertNotNull(tree);

        final ParseTreeBuilder b = this.builder(g, text, goal);
        ;
        final SPPTBranch expected = b.branch("asbs", b.branch("as", b.emptyLeaf("as")), b.branch("bs", b.branch("b", b.leaf("b")), b.branch("b", b.leaf("b"))));
        Assert.assertEquals(expected, tree.getRoot());

    }

    @Test
    public void as13_as_a() throws ParseFailedException {
        // grammar, goal, input

        final GrammarDefault g = this.as13();
        final String goal = "as";
        final String text = "a";

        final SharedPackedParseTree tree = this.process(g, text, goal);
        Assert.assertNotNull(tree);

        final ParseTreeBuilder b = this.builder(g, text, goal);
        ;
        final SPPTBranch expected = b.branch("as", b.branch("a", b.leaf("a", "a")));
        Assert.assertEquals(expected, tree.getRoot());

    }

    @Test
    public void as13_as_aa() throws ParseFailedException {
        // grammar, goal, input

        final GrammarDefault g = this.as13();
        final String goal = "as";
        final String text = "aa";

        final SharedPackedParseTree tree = this.process(g, text, goal);
        Assert.assertNotNull(tree);

        final ParseTreeBuilder b = this.builder(g, text, goal);
        ;
        final SPPTBranch expected = b.branch("as", b.branch("a", b.leaf("a", "a")), b.branch("a", b.leaf("a", "a")));
        Assert.assertEquals(expected, tree.getRoot());

    }

    @Test
    public void as13_as_aaa() throws ParseFailedException {
        // grammar, goal, input

        final GrammarDefault g = this.as13();
        final String goal = "as";
        final String text = "aaa";

        final SharedPackedParseTree tree = this.process(g, text, goal);
        Assert.assertNotNull(tree);

        final ParseTreeBuilder b = this.builder(g, text, goal);

        final SPPTBranch expected = b.branch("as", b.branch("a", b.leaf("a", "a")), b.branch("a", b.leaf("a", "a")), b.branch("a", b.leaf("a", "a")));
        Assert.assertEquals(expected, tree.getRoot());

    }

    @Test
    public void ab01_ab01_a() throws ParseFailedException {
        // grammar, goal, input

        final GrammarDefault g = this.ab01();
        final String goal = "ab01";
        final String text = "a";

        final SharedPackedParseTree tree = this.process(g, text, goal);
        Assert.assertNotNull(tree);

        final ParseTreeBuilder b = this.builder(g, text, goal);
        ;
        final SPPTBranch expected = b.branch("ab01", b.branch("a", b.leaf("a", "a")), b.branch("b01", b.emptyLeaf("b01")));
        Assert.assertEquals(expected, tree.getRoot());

    }

    @Test
    public void ab01_2_ab01_a() throws ParseFailedException {
        // grammar, goal, input

        final GrammarDefault g = this.ab01_2();
        final String goal = "ab01";
        final String text = "a";

        final SharedPackedParseTree tree = this.process(g, text, goal);
        Assert.assertNotNull(tree);

        final ParseTreeBuilder b = this.builder(g, text, goal);
        ;
        final SPPTBranch expected = b.branch("ab01", b.branch("a", b.leaf("a", "a")));
        Assert.assertEquals(expected, tree.getRoot());

    }

    @Test
    public void ab01_2_ab01_ab() throws ParseFailedException {
        // grammar, goal, input

        final GrammarDefault g = this.ab01_2();
        final String goal = "ab01";
        final String text = "ab";

        final SharedPackedParseTree tree = this.process(g, text, goal);
        Assert.assertNotNull(tree);

        final ParseTreeBuilder b = this.builder(g, text, goal);

        final SharedPackedParseTree expected = new SharedPackedParseTreeSimple(
                b.branch("ab01", b.branch("ab01$group1", b.branch("a", b.leaf("a", "a")), b.branch("b", b.leaf("b", "b")))),-1);
        Assert.assertEquals(expected, tree);

    }

    @Test
    public void ab01_ab01_ab() throws ParseFailedException {
        // grammar, goal, input

        final GrammarDefault g = this.ab01();
        final String goal = "ab01";
        final String text = "ab";

        final SharedPackedParseTree tree = this.process(g, text, goal);
        Assert.assertNotNull(tree);

        final ParseTreeBuilder b = this.builder(g, text, goal);

        final SPPTBranch expected = b.branch("ab01", b.branch("a", b.leaf("a", "a")), b.branch("b01", b.branch("b", b.leaf("b", "b"))));
        Assert.assertEquals(expected, tree.getRoot());

    }

    @Test
    public void ab01_ab01_aa() {
        // grammar, goal, input
        try {
            final GrammarDefault g = this.ab01();
            final String goal = "ab01";
            final String text = "aa";

            final SharedPackedParseTree tree = this.process(g, text, goal);

            Assert.fail("This parse should fail");

        } catch (final ParseFailedException e) {
            // this should occur
        }
    }

    private final int abs1m1_abs_ab5000_length = 5000;
    private String abs1m1_abs_ab5000_text = "";
    private SharedPackedParseTree abs1m1_abs_ab5000_expected;

    @Before
    public void abs1m1_abs_ab5000_setup() {
        if (Objects.equals("abs1m1_abs_ab5000", this.testName.getMethodName())) {
            final GrammarDefault g = this.abs1m1();
            final String text = this.abs1m1_abs_ab5000_text;
            final ParseTreeBuilder b = this.builder(g, text, "abs");
            b.define("abs {");
            for (int i = 0; i < this.abs1m1_abs_ab5000_length; ++i) {
                this.abs1m1_abs_ab5000_text += "ab";
                b.define("  ab { a {'a'} }");
                b.define("  ab { b {'b'} }");
            }
            b.define("}");
            this.abs1m1_abs_ab5000_expected = b.buildAndAdd();
        }
    }

    @Test
    public void abs1m1_abs_ab5000() throws ParseFailedException {
        // grammar, goal, input

        final GrammarDefault g = this.abs1m1();
        final String goal = "abs";
        final String text = this.abs1m1_abs_ab5000_text;

        final SharedPackedParseTree actual = this.process(g, text, goal);

        final SharedPackedParseTree expected = this.abs1m1_abs_ab5000_expected;

        Assert.assertNotNull(actual);
        Assert.assertEquals(expected, actual);

    }

    GrammarDefault nested() {
        final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
        b.rule("top").multi(1, -1, new NonTerminalDefault("level1"));
        b.rule("level1").multi(0, 1, new TerminalLiteralDefault("a"));
        return b.get();
    }

    @Test
    public void nested_top_aa() throws ParseFailedException {
        // grammar, goal, input

        final GrammarDefault g = this.nested();
        final String goal = "top";
        final String text = "aa";

        final SharedPackedParseTree tree = this.process(g, text, goal);
        Assert.assertNotNull(tree);

        final ParseTreeBuilder b = this.builder(g, text, goal);
        final SharedPackedParseTree expected = new SharedPackedParseTreeSimple(
                b.branch("top", b.branch("level1", b.leaf("a")), b.branch("level1", b.leaf("a"))),-1);
        Assert.assertEquals(expected, tree);

    }

    // S = F? G 'a' A;
    // F = A ;
    // G = H | J ;
    // H = A ;
    // J = G '.' A ;
    // A = 'a' ;
    GrammarDefault x() {
        final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
        b.rule("S").concatenation(new NonTerminalDefault("Fm"), new NonTerminalDefault("G"), new TerminalLiteralDefault("a"), new NonTerminalDefault("A"));
        b.rule("Fm").multi(0, 1, new NonTerminalDefault("F"));
        b.rule("F").choice(new NonTerminalDefault("A"));
        b.rule("G").choice(new NonTerminalDefault("H"), new NonTerminalDefault("J"));
        b.rule("A").choice(new TerminalLiteralDefault("a"));
        b.rule("H").choice(new NonTerminalDefault("A"));
        b.rule("J").concatenation(new NonTerminalDefault("G"), new TerminalLiteralDefault("."), new NonTerminalDefault("A"));
        return b.get();
    }

    @Test
    public void x_S_aaa() throws ParseFailedException {
        // grammar, goal, input

        final GrammarDefault g = this.x();
        final String goal = "S";
        final String text = "aaa";

        final SharedPackedParseTree tree = this.process(g, text, goal);
        Assert.assertNotNull(tree);

        final ParseTreeBuilder b = this.builder(g, text, goal);
        b.define("S{");
        b.define("  Fm { $empty }");
        b.define("  G { H { A { 'a' } } }");
        b.define("  'a'");
        b.define("  A {'a'}");
        b.define("}");
        final SharedPackedParseTree expected = b.buildAndAdd();
        Assert.assertEquals(expected, tree);

    }

}
