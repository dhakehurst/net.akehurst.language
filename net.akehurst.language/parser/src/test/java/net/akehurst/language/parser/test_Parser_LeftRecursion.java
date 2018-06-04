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

import org.junit.Assert;
import org.junit.Test;

import net.akehurst.language.api.parser.ParseFailedException;
import net.akehurst.language.api.sppt.SPPTBranch;
import net.akehurst.language.api.sppt.SharedPackedParseTree;
import net.akehurst.language.grammar.parser.forrest.ParseTreeBuilder;
import net.akehurst.language.ogl.semanticStructure.GrammarBuilderDefault;
import net.akehurst.language.ogl.semanticStructure.GrammarDefault;
import net.akehurst.language.ogl.semanticStructure.NamespaceDefault;
import net.akehurst.language.ogl.semanticStructure.NonTerminalDefault;
import net.akehurst.language.ogl.semanticStructure.TerminalLiteralDefault;
import net.akehurst.language.parser.sppf.SharedPackedParseTreeSimple;

public class test_Parser_LeftRecursion extends AbstractParser_Test {

    GrammarDefault Sas() {
        final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
        b.rule("S").choice(new NonTerminalDefault("S_a"), new TerminalLiteralDefault("a"));
        b.rule("S_a").concatenation(new NonTerminalDefault("S"), new TerminalLiteralDefault("a"));
        return b.get();
    }

    @Test
    public void Sas_S_a() throws ParseFailedException {
        // grammar, goal, input

        final GrammarDefault g = this.Sas();
        final String goal = "S";
        final String text = "a";

        final SharedPackedParseTree tree = this.process(g, text, goal);
        Assert.assertNotNull(tree);

        final ParseTreeBuilder b = this.builder(g, text, goal);

        final SPPTBranch expected = b.branch("S", b.leaf("a", "a"));
        Assert.assertEquals(expected, tree.getRoot());

    }

    @Test
    public void SasS_aa() throws ParseFailedException {
        // grammar, goal, input

        final GrammarDefault g = this.Sas();
        final String goal = "S";
        final String text = "aa";

        final SharedPackedParseTree tree = this.process(g, text, goal);
        Assert.assertNotNull(tree);

        final ParseTreeBuilder b = this.builder(g, text, goal);
        final SharedPackedParseTree expected = new SharedPackedParseTreeSimple(b.branch("S", b.branch("S_a", b.branch("S", b.leaf("a")), b.leaf("a"))));
        Assert.assertEquals(expected, tree);

    }

    @Test
    public void Sas_S_aaa() throws ParseFailedException {
        // grammar, goal, input
        final GrammarDefault g = this.Sas();
        final String goal = "S";
        final String text = "aaa";

        final SharedPackedParseTree tree = this.process(g, text, goal);
        Assert.assertNotNull(tree);

        final ParseTreeBuilder b = this.builder(g, text, goal);

        final SPPTBranch expected = b.branch("S", b.branch("S_a", b.branch("S", b.branch("S_a", b.branch("S", b.leaf("a")), b.leaf("a"))), b.leaf("a")));
        Assert.assertEquals(expected, tree.getRoot());
    }

    @Test
    public void Sas_S_a10() throws ParseFailedException {
        // grammar, goal, input
        final GrammarDefault g = this.Sas();
        final String goal = "S";
        String text = "";
        for (int i = 0; i < 10; ++i) {
            text += "a";
        }

        final SharedPackedParseTree tree = this.process(g, text, goal);
        Assert.assertNotNull(tree);

        final ParseTreeBuilder b = this.builder(g, text, goal);

        final SharedPackedParseTree expected = new SharedPackedParseTreeSimple(
                b.branch("S", b.branch("S_a", b.branch("S", b.branch("S_a", b.branch("S", b.leaf("a")), b.leaf("a"))), b.leaf("a"))));
        Assert.assertEquals(expected, tree);
    }

    GrammarDefault Sas2() {
        final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
        b.rule("S").choice(new NonTerminalDefault("S_a"), new NonTerminalDefault("an"));
        b.rule("S_a").concatenation(new NonTerminalDefault("S"), new NonTerminalDefault("an"));
        b.rule("an").choice(new TerminalLiteralDefault("a"));
        return b.get();
    }

    @Test
    public void Sas2_S_a10() throws ParseFailedException {
        // grammar, goal, input
        final GrammarDefault g = this.Sas2();
        final String goal = "S";
        String text = "";
        for (int i = 0; i < 10; ++i) {
            text += "a";
        }

        final SharedPackedParseTree tree = this.process(g, text, goal);
        Assert.assertNotNull(tree);

        final ParseTreeBuilder b = this.builder(g, text, goal);
        b.define("S {");
        b.define("  S_a {");
        b.define("    S {");
        b.define("      S { 'a' } ");
        b.define("      'a'");
        b.define("    }");
        b.define("    'a'");
        b.define("  }");
        b.define("}");
        final SharedPackedParseTree expected = b.buildAndAdd();

        Assert.assertEquals(expected.toStringAll(), tree.toStringAll());
    }

    // Some of these test grammars are based on those listed in [https://github.com/PhilippeSigaud/Pegged/wiki/Left-Recursion]
    GrammarDefault direct() {
        final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
        b.rule("S").choice(new NonTerminalDefault("E"));
        b.rule("E").choice(new NonTerminalDefault("E1"), new NonTerminalDefault("E2"));
        b.rule("E1").concatenation(new NonTerminalDefault("E"), new TerminalLiteralDefault("+a"));
        b.rule("E2").concatenation(new TerminalLiteralDefault("a"));
        return b.get();
    }

    @Test
    public void direct_S_a() throws ParseFailedException {
        // grammar, goal, input

        final GrammarDefault g = this.direct();
        final String goal = "S";
        final String text = "a";

        final SharedPackedParseTree tree = this.process(g, text, goal);
        Assert.assertNotNull(tree);

        final ParseTreeBuilder b = this.builder(g, text, goal);
        b.define("S {");
        b.define("  E {");
        b.define("    E2 { 'a' }");
        b.define("  }");
        b.define("}");
        final SharedPackedParseTree expected = b.buildAndAdd();
        Assert.assertEquals(expected, tree);

    }

    @Test
    public void direct_S_apa() throws ParseFailedException {
        // grammar, goal, input

        final GrammarDefault g = this.direct();
        final String goal = "S";
        final String text = "a+a";

        final SharedPackedParseTree tree = this.process(g, text, goal);
        Assert.assertNotNull(tree);

        final ParseTreeBuilder b = this.builder(g, text, goal);
        b.define("S {");
        b.define("  E {");
        b.define("    E1 {");
        b.define("      E { E2 { 'a' } }");
        b.define("      '+a'");
        b.define("    }");
        b.define("  }");
        b.define("}");
        final SharedPackedParseTree expected = b.buildAndAdd();
        Assert.assertEquals(expected, tree);

    }

    @Test
    public void direct_S_apapa() throws ParseFailedException {
        // grammar, goal, input

        final GrammarDefault g = this.direct();
        final String goal = "S";
        final String text = "a+a+a";

        final SharedPackedParseTree tree = this.process(g, text, goal);
        Assert.assertNotNull(tree);

        final ParseTreeBuilder b = this.builder(g, text, goal);
        b.define("S {");
        b.define("  E {");
        b.define("    E1 {");
        b.define("      E {");
        b.define("        E1 {");
        b.define("          E { E2 { 'a' } }");
        b.define("          '+a'");
        b.define("        }");
        b.define("      }");
        b.define("      '+a'");
        b.define("    }");
        b.define("  }");
        b.define("}");
        final SharedPackedParseTree expected = b.buildAndAdd();
        Assert.assertEquals(expected, tree);

    }

    // E = Bm E '+a' | 'a' ;
    // Bm = 'b'?
    GrammarDefault hidden1() {
        final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
        b.rule("S").choice(new NonTerminalDefault("E"));
        b.rule("E").choice(new NonTerminalDefault("E1"), new TerminalLiteralDefault("a"));
        b.rule("E1").concatenation(new NonTerminalDefault("Bm"), new NonTerminalDefault("E"), new TerminalLiteralDefault("+a"));
        b.rule("Bm").multi(0, 1, new TerminalLiteralDefault("b"));
        return b.get();
    }

    @Test
    public void hidden1_S_a() throws ParseFailedException {
        // grammar, goal, input
        final GrammarDefault g = this.hidden1();
        final String goal = "S";
        final String text = "a";

        final SharedPackedParseTree tree = this.process(g, text, goal);
        Assert.assertNotNull(tree);

        final ParseTreeBuilder b = this.builder(g, text, goal);
        b.define("S {");
        b.define("  E{");
        b.define("    'a'");
        b.define("  }");
        b.define("}");
        final SharedPackedParseTree expected = b.buildAndAdd();
        Assert.assertEquals(expected, tree);
    }

    @Test
    public void hidden1_S_apa() throws ParseFailedException {
        // grammar, goal, input
        final GrammarDefault g = this.hidden1();
        final String goal = "S";
        final String text = "a+a";

        final SharedPackedParseTree tree = this.process(g, text, goal);
        Assert.assertNotNull(tree);

        final ParseTreeBuilder b = this.builder(g, text, goal);
        b.define("S {");
        b.define("  E {");
        b.define("    E1 {");
        b.define("      Bm { $empty }");
        b.define("      E{ 'a' }");
        b.define("      '+a'");
        b.define("    }");
        b.define("  }");
        b.define("}");
        final SharedPackedParseTree expected = b.buildAndAdd();
        Assert.assertEquals(expected, tree);
    }

    @Test
    public void hidden1_S_bapa() throws ParseFailedException {
        // grammar, goal, input
        final GrammarDefault g = this.hidden1();
        final String goal = "S";
        final String text = "ba+a";

        final SharedPackedParseTree tree = this.process(g, text, goal);
        Assert.assertNotNull(tree);

        final ParseTreeBuilder b = this.builder(g, text, goal);
        b.define("S {");
        b.define("  E {");
        b.define("    E1 {");
        b.define("      Bm { 'b' }");
        b.define("      E{ 'a' }");
        b.define("      '+a'");
        b.define("    }");
        b.define("  }");
        b.define("}");
        final SharedPackedParseTree expected = b.buildAndAdd();
        Assert.assertEquals(expected, tree);
    }

    // E = B E '+a' | 'a' ;
    // B = 'b' 'c' | 'd'* ;
    GrammarDefault hidden2() {
        final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
        b.rule("S").choice(new NonTerminalDefault("E"));
        b.rule("E").choice(new NonTerminalDefault("E1"), new NonTerminalDefault("E2"));
        b.rule("E1").concatenation(new NonTerminalDefault("B"), new NonTerminalDefault("E"), new TerminalLiteralDefault("+a"));
        b.rule("E2").concatenation(new TerminalLiteralDefault("a"));
        b.rule("B").choice(new NonTerminalDefault("B1"), new NonTerminalDefault("B2"));
        b.rule("B1").concatenation(new TerminalLiteralDefault("b"), new TerminalLiteralDefault("c"));
        b.rule("B2").multi(0, -1, new TerminalLiteralDefault("d"));
        return b.get();
    }

    // E = F '+a' | 'a' ;
    // F = 'g' 'h' | J ;
    // J = 'k' | E 'l' ;
    GrammarDefault indirect() {
        final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
        b.rule("S").choice(new NonTerminalDefault("E"));
        b.rule("E").choice(new NonTerminalDefault("E1"), new NonTerminalDefault("E2"));
        b.rule("E1").concatenation(new NonTerminalDefault("F"), new TerminalLiteralDefault("+a"));
        b.rule("E2").concatenation(new TerminalLiteralDefault("a"));
        b.rule("F").choice(new NonTerminalDefault("F1"), new NonTerminalDefault("F2"));
        b.rule("F1").concatenation(new TerminalLiteralDefault("g"), new TerminalLiteralDefault("h"));
        b.rule("F2").concatenation(new NonTerminalDefault("J"));
        b.rule("J").choice(new NonTerminalDefault("J1"), new NonTerminalDefault("J2"));
        b.rule("J1").concatenation(new TerminalLiteralDefault("k"));
        b.rule("J2").concatenation(new NonTerminalDefault("K"), new TerminalLiteralDefault("l"));
        return b.get();
    }

    // E = F 'n' | 'n'
    // F = E '+' I* | G '-'
    // G = H 'm' | E
    // H = G 'l'
    // I = '(' A+ ')'
    // A = 'a'

    GrammarDefault interlocking() {
        final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
        b.rule("S").choice(new NonTerminalDefault("E"));
        b.rule("E").choice(new NonTerminalDefault("E1"), new NonTerminalDefault("E2"));
        b.rule("E1").concatenation(new NonTerminalDefault("F"), new TerminalLiteralDefault("n"));
        b.rule("E2").concatenation(new TerminalLiteralDefault("n"));
        b.rule("F").choice(new NonTerminalDefault("F1"), new NonTerminalDefault("F2"));
        b.rule("F1").concatenation(new NonTerminalDefault("E"), new TerminalLiteralDefault("+"), new NonTerminalDefault("Is"));
        b.rule("F2").concatenation(new NonTerminalDefault("G"), new TerminalLiteralDefault("-"));
        b.rule("Is").multi(0, -1, new NonTerminalDefault("I"));
        b.rule("G").choice(new NonTerminalDefault("G1"), new NonTerminalDefault("G2"));
        b.rule("G1").concatenation(new NonTerminalDefault("H"), new TerminalLiteralDefault("m"));
        b.rule("G2").concatenation(new NonTerminalDefault("E"));
        b.rule("H").concatenation(new NonTerminalDefault("G"), new TerminalLiteralDefault("l"));
        b.rule("I").concatenation(new TerminalLiteralDefault("("), new NonTerminalDefault("As"), new TerminalLiteralDefault(")"));
        b.rule("As").multi(1, -1, new NonTerminalDefault("A"));
        b.rule("A").choice(new TerminalLiteralDefault("a"));
        return b.get();
    }

    @Test
    public void interlocking_S_() throws ParseFailedException {
        // grammar, goal, input

        final GrammarDefault g = this.interlocking();
        final String goal = "S";
        final String text = "nlm-n+(aaa)n";

        final SharedPackedParseTree tree = this.process(g, text, goal);
        Assert.assertNotNull(tree);

        final ParseTreeBuilder b = this.builder(g, text, goal);
        final SharedPackedParseTree expected = null;
        // final IBranch expected = b.branch("as", b.branch("a", b.leaf("a", "a")));
        Assert.assertEquals(expected, tree);

    }

    // S = X
    // X = P '.x' / 'x'
    // P = P '(n)' / X
    GrammarDefault interlocking2() {
        final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
        b.rule("S").choice(new NonTerminalDefault("E"));
        b.rule("E").choice(new NonTerminalDefault("E1"), new NonTerminalDefault("E2"));
        b.rule("E1").concatenation(new NonTerminalDefault("F"), new TerminalLiteralDefault("n"));
        b.rule("E2").concatenation(new TerminalLiteralDefault("n"));
        b.rule("F").choice(new NonTerminalDefault("F1"), new NonTerminalDefault("F2"));
        b.rule("F1").concatenation(new NonTerminalDefault("E"), new TerminalLiteralDefault("+"), new NonTerminalDefault("Is"));
        b.rule("F2").concatenation(new NonTerminalDefault("G"), new TerminalLiteralDefault("-"));
        b.rule("Is").multi(0, -1, new NonTerminalDefault("I"));
        b.rule("G").choice(new NonTerminalDefault("G1"), new NonTerminalDefault("G2"));
        b.rule("G1").concatenation(new NonTerminalDefault("H"), new TerminalLiteralDefault("m"));
        b.rule("G2").concatenation(new NonTerminalDefault("E"));
        b.rule("H").concatenation(new NonTerminalDefault("G"), new TerminalLiteralDefault("l"));
        b.rule("I").concatenation(new TerminalLiteralDefault("("), new NonTerminalDefault("As"), new TerminalLiteralDefault(")"));
        b.rule("As").multi(1, -1, new NonTerminalDefault("A"));
        b.rule("A").choice(new TerminalLiteralDefault("a"));
        return b.get();
    }

    // S = b | S S | SSS
    GrammarDefault SSSSS() {
        final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
        b.rule("S").choice(new NonTerminalDefault("S1"), new NonTerminalDefault("S2"), new NonTerminalDefault("S3"));
        // TODO:
        return b.get();
    }

    // S = C a | d
    // B = <empty> | a
    // C = b | BCb | b b
    GrammarDefault SBC() {
        final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
        b.rule("S").choice(new NonTerminalDefault("S1"), new NonTerminalDefault("S2"));
        // TODO:
        return b.get();
    }

    // S = b | SSA
    // A = S | <empty>
    GrammarDefault SA() {
        final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
        b.rule("S").choice(new NonTerminalDefault("S1"), new NonTerminalDefault("S2"));
        b.rule("S1").choice(new TerminalLiteralDefault("b"));
        b.rule("S2").concatenation(new NonTerminalDefault("S"), new NonTerminalDefault("S"), new NonTerminalDefault("A"));
        b.rule("A").choice(new NonTerminalDefault("A1"), new NonTerminalDefault("A2"));
        b.rule("A1").concatenation(new NonTerminalDefault("S"));
        b.rule("A2").choice();
        // TODO:
        return b.get();
    }

    @Test
    public void SA_S_b1() throws ParseFailedException {
        // grammar, goal, input

        final GrammarDefault g = this.SA();
        final String goal = "S";
        final String text = "b";

        final SharedPackedParseTree tree = this.process(g, text, goal);
        Assert.assertNotNull(tree);

        final ParseTreeBuilder b = this.builder(g, text, goal);
        b.define("S {");
        b.define("  S1 {");
        b.define("    'b'");
        b.define("  }");
        b.define("}");
        final SharedPackedParseTree expected = b.buildAndAdd();
        Assert.assertEquals(expected.toStringAll(), tree.toStringAll());

    }

    @Test
    public void SA_S_b2() throws ParseFailedException {
        // grammar, goal, input

        final GrammarDefault g = this.SA();
        final String goal = "S";
        final String text = "bb";

        final SharedPackedParseTree tree = this.process(g, text, goal);
        Assert.assertNotNull(tree);

        final ParseTreeBuilder b = this.builder(g, text, goal);
        b.define("S {");
        b.define("  S2 {");
        b.define("    S { S1 { 'b' } }");
        b.define("    S { S1 { 'b' } }");
        b.define("    A { A2 { $empty } }");
        b.define("  }");
        b.define("}");
        final SharedPackedParseTree expected = b.buildAndAdd();
        Assert.assertEquals(expected.toStringAll(), tree.toStringAll());

    }

    @Test
    public void SA_S_b3() throws ParseFailedException {
        // grammar, goal, input

        final GrammarDefault g = this.SA();
        final String goal = "S";
        final String text = "bbb";

        final SharedPackedParseTree tree = this.process(g, text, goal);
        Assert.assertNotNull(tree);

        final ParseTreeBuilder b = this.builder(g, text, goal);
        b.define("S {");
        b.define("  S2 {");
        b.define("    S { S1 { 'b' } }");
        b.define("    S { S1 { 'b' } }");
        b.define("    A { A1 { S { S1 { 'b' } } } }");
        b.define("  }");
        b.define("}");
        b.buildAndAdd();
        b.define("S {");
        b.define("  S2 {");
        b.define("    S { S1 { 'b' } }");
        b.define("    S {");
        b.define("      S2 {");
        b.define("        S { S1 { 'b' } }");
        b.define("        S { S1 { 'b' } }");
        b.define("        A { A2 { $empty } }");
        b.define("      }");
        b.define("    }");
        b.define("    A { A2 { $empty } }");
        b.define("  }");
        b.define("}");
        b.buildAndAdd();
        b.define("S {");
        b.define(" S2 {");
        b.define("   S {");
        b.define("     S2 {");
        b.define("       S { S1 { 'b' } }");
        b.define("       S { S1 { 'b' } }");
        b.define("       A { A2 { $empty } }");
        b.define("     }");
        b.define("   }");
        b.define("   S { S1 { 'b' } }");
        b.define("   A { A2 { $empty } }");
        b.define(" }");
        b.define("}");
        final SharedPackedParseTree expected = b.buildAndAdd();
        Assert.assertEquals(expected.toStringAll(), tree.toStringAll());

    }

    @Test
    public void SA_S_b4() throws ParseFailedException {
        // grammar, goal, input

        final GrammarDefault g = this.SA();
        final String goal = "S";
        final String text = "bbbb";

        final SharedPackedParseTree tree = this.process(g, text, goal);
        Assert.assertNotNull(tree);

        final ParseTreeBuilder b = this.builder(g, text, goal);
        b.define("S {");
        b.define("  S1 {");
        b.define("    'b'");
        b.define("  }");
        b.define("}");
        final SharedPackedParseTree expected = b.buildAndAdd();
        Assert.assertEquals(expected.toStringAll(), tree.toStringAll());

    }

    @Test
    public void SA_S_b5() throws ParseFailedException {
        // grammar, goal, input

        final GrammarDefault g = this.SA();
        final String goal = "S";
        final String text = "bbbbb";

        final SharedPackedParseTree tree = this.process(g, text, goal);
        Assert.assertNotNull(tree);

        final ParseTreeBuilder b = this.builder(g, text, goal);
        b.define("S {");
        b.define("  S1 {");
        b.define("    'b'");
        b.define("  }");
        b.define("}");
        final SharedPackedParseTree expected = b.buildAndAdd();
        Assert.assertEquals(expected.toStringAll(), tree.toStringAll());

    }

    @Test
    public void SA_S_b6() throws ParseFailedException {
        // grammar, goal, input

        final GrammarDefault g = this.SA();
        final String goal = "S";
        final String text = "bbbbbb";

        final SharedPackedParseTree tree = this.process(g, text, goal);
        Assert.assertNotNull(tree);

        final ParseTreeBuilder b = this.builder(g, text, goal);
        b.define("S {");
        b.define("  S1 {");
        b.define("    'b'");
        b.define("  }");
        b.define("}");
        final SharedPackedParseTree expected = b.buildAndAdd();
        Assert.assertEquals(expected.toStringAll(), tree.toStringAll());

    }

    @Test
    public void SA_S_b7() throws ParseFailedException {
        // grammar, goal, input

        final GrammarDefault g = this.SA();
        final String goal = "S";
        final String text = "bbbbbbb";

        final SharedPackedParseTree tree = this.process(g, text, goal);
        Assert.assertNotNull(tree);

        final ParseTreeBuilder b = this.builder(g, text, goal);
        b.define("S {");
        b.define("  S1 {");
        b.define("    'b'");
        b.define("  }");
        b.define("}");
        final SharedPackedParseTree expected = b.buildAndAdd();
        Assert.assertEquals(expected.toStringAll(), tree.toStringAll());

    }

    @Test
    public void SA_S_b8() throws ParseFailedException {
        // grammar, goal, input

        final GrammarDefault g = this.SA();
        final String goal = "S";
        final String text = "bbbbbbbb";

        final SharedPackedParseTree tree = this.process(g, text, goal);
        Assert.assertNotNull(tree);

        final ParseTreeBuilder b = this.builder(g, text, goal);
        b.define("S {");
        b.define("  S1 {");
        b.define("    'b'");
        b.define("  }");
        b.define("}");
        final SharedPackedParseTree expected = b.buildAndAdd();
        Assert.assertEquals(expected.toStringAll(), tree.toStringAll());

    }

    @Test
    public void SA_S_b9() throws ParseFailedException {
        // grammar, goal, input

        final GrammarDefault g = this.SA();
        final String goal = "S";
        final String text = "bbbbbbbbb";

        final SharedPackedParseTree tree = this.process(g, text, goal);
        Assert.assertNotNull(tree);

        final ParseTreeBuilder b = this.builder(g, text, goal);
        b.define("S {");
        b.define("  S1 {");
        b.define("    'b'");
        b.define("  }");
        b.define("}");
        final SharedPackedParseTree expected = b.buildAndAdd();
        Assert.assertEquals(expected.toStringAll(), tree.toStringAll());

    }

    @Test
    public void SA_S_b10() throws ParseFailedException {
        // grammar, goal, input

        final GrammarDefault g = this.SA();
        final String goal = "S";
        final String text = "bbbbbbbbbb";

        final SharedPackedParseTree tree = this.process(g, text, goal);
        Assert.assertNotNull(tree);

        final ParseTreeBuilder b = this.builder(g, text, goal);
        b.define("S {");
        b.define("  S1 {");
        b.define("    'b'");
        b.define("  }");
        b.define("}");
        final SharedPackedParseTree expected = b.buildAndAdd();
        Assert.assertEquals(expected.toStringAll(), tree.toStringAll());

    }

    @Test
    public void SA_S_b20() throws ParseFailedException {
        // grammar, goal, input

        final GrammarDefault g = this.SA();
        final String goal = "S";
        String text = "";
        // TODO: make this 300 (30)
        for (int i = 0; i < 20; i++) {
            text += "b";
        }

        final SharedPackedParseTree tree = this.process(g, text, goal);
        Assert.assertNotNull(tree);

        // final ParseTreeBuilder b = this.builder(g, text, goal);
        // b.define("S {");
        // b.define(" S1 {");
        // b.define(" 'b'");
        // b.define(" }");
        // b.define("}");
        // final ISharedPackedParseTree expected = b.build();
        // Assert.assertEquals(expected, tree);

    }
}
