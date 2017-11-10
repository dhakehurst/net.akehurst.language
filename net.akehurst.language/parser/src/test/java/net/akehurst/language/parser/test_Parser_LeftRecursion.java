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

import net.akehurst.language.core.parser.ParseFailedException;
import net.akehurst.language.core.sppf.ISPPFBranch;
import net.akehurst.language.core.sppf.IParseTree;
import net.akehurst.language.grammar.parser.forrest.ParseTreeBuilder;
import net.akehurst.language.ogl.semanticStructure.Grammar;
import net.akehurst.language.ogl.semanticStructure.GrammarBuilder;
import net.akehurst.language.ogl.semanticStructure.Namespace;
import net.akehurst.language.ogl.semanticStructure.NonTerminal;
import net.akehurst.language.ogl.semanticStructure.TerminalLiteral;
import net.akehurst.language.parser.sppf.SharedPackedParseForest;

public class test_Parser_LeftRecursion extends AbstractParser_Test {

	Grammar Sas() {
		final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("S").choice(new NonTerminal("S_a"), new TerminalLiteral("a"));
		b.rule("S_a").concatenation(new NonTerminal("S"), new TerminalLiteral("a"));
		return b.get();
	}

	@Test
	public void Sas_S_a() throws ParseFailedException {
		// grammar, goal, input

		final Grammar g = this.Sas();
		final String goal = "S";
		final String text = "a";

		final IParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);

		final ISPPFBranch expected = b.branch("S", b.leaf("a", "a"));
		Assert.assertEquals(expected, tree.getRoot());

	}

	@Test
	public void SasS_aa() throws ParseFailedException {
		// grammar, goal, input

		final Grammar g = this.Sas();
		final String goal = "S";
		final String text = "aa";

		final IParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		final IParseTree expected = new SharedPackedParseForest(b.branch("S", b.branch("S_a", b.branch("S", b.leaf("a")), b.leaf("a"))));
		Assert.assertEquals(expected, tree);

	}

	@Test
	public void Sas_S_aaa() throws ParseFailedException {
		// grammar, goal, input
		final Grammar g = this.Sas();
		final String goal = "S";
		final String text = "aaa";

		final IParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);

		final ISPPFBranch expected = b.branch("S", b.branch("S_a", b.branch("S", b.branch("S_a", b.branch("S", b.leaf("a")), b.leaf("a"))), b.leaf("a")));
		Assert.assertEquals(expected, tree.getRoot());
	}

	@Test
	public void Sas_S_a10() throws ParseFailedException {
		// grammar, goal, input
		final Grammar g = this.Sas();
		final String goal = "S";
		String text = "";
		for (int i = 0; i < 10; ++i) {
			text += "a";
		}

		final IParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);

		final IParseTree expected = new SharedPackedParseForest(
				b.branch("S", b.branch("S_a", b.branch("S", b.branch("S_a", b.branch("S", b.leaf("a")), b.leaf("a"))), b.leaf("a"))));
		Assert.assertEquals(expected, tree);
	}

	Grammar Sas2() {
		final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("S").choice(new NonTerminal("S_a"), new NonTerminal("an"));
		b.rule("S_a").concatenation(new NonTerminal("S"), new NonTerminal("an"));
		b.rule("an").choice(new TerminalLiteral("a"));
		return b.get();
	}

	@Test
	public void Sas2_S_a10() throws ParseFailedException {
		// grammar, goal, input
		final Grammar g = this.Sas2();
		final String goal = "S";
		String text = "";
		for (int i = 0; i < 10; ++i) {
			text += "a";
		}

		final IParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);

		final IParseTree expected = new SharedPackedParseForest(
				b.branch("S", b.branch("S_a", b.branch("S", b.branch("S_a", b.branch("S", b.leaf("a")), b.leaf("a"))), b.leaf("a"))));
		Assert.assertEquals(expected, tree);
	}

	// Some of these test grammars are based on those listed in [https://github.com/PhilippeSigaud/Pegged/wiki/Left-Recursion]
	Grammar direct() {
		final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("S").choice(new NonTerminal("E"));
		b.rule("E").choice(new NonTerminal("E1"), new NonTerminal("E2"));
		b.rule("E1").concatenation(new NonTerminal("E"), new TerminalLiteral("+a"));
		b.rule("E2").concatenation(new TerminalLiteral("a"));
		return b.get();
	}

	@Test
	public void direct_S_a() throws ParseFailedException {
		// grammar, goal, input

		final Grammar g = this.direct();
		final String goal = "S";
		final String text = "a";

		final IParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		b.define("S {");
		b.define("  E {");
		b.define("    E2 { 'a' }");
		b.define("  }");
		b.define("}");
		final IParseTree expected = b.build();
		Assert.assertEquals(expected, tree);

	}

	@Test
	public void direct_S_apa() throws ParseFailedException {
		// grammar, goal, input

		final Grammar g = this.direct();
		final String goal = "S";
		final String text = "a+a";

		final IParseTree tree = this.process(g, text, goal);
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
		final IParseTree expected = b.build();
		Assert.assertEquals(expected, tree);

	}

	@Test
	public void direct_S_apapa() throws ParseFailedException {
		// grammar, goal, input

		final Grammar g = this.direct();
		final String goal = "S";
		final String text = "a+a+a";

		final IParseTree tree = this.process(g, text, goal);
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
		final IParseTree expected = b.build();
		Assert.assertEquals(expected, tree);

	}

	// E = Bm E '+a' | 'a' ;
	// Bm = 'b'?
	Grammar hidden1() {
		final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("S").choice(new NonTerminal("E"));
		b.rule("E").choice(new NonTerminal("E1"), new TerminalLiteral("a"));
		b.rule("E1").concatenation(new NonTerminal("Bm"), new NonTerminal("E"), new TerminalLiteral("+a"));
		b.rule("Bm").multi(0, 1, new TerminalLiteral("b"));
		return b.get();
	}

	@Test
	public void hidden1_S_a() throws ParseFailedException {
		// grammar, goal, input
		final Grammar g = this.hidden1();
		final String goal = "S";
		final String text = "a";

		final IParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		b.define("S {");
		b.define("  E{");
		b.define("    'a'");
		b.define("  }");
		b.define("}");
		final IParseTree expected = b.build();
		Assert.assertEquals(expected, tree);
	}

	@Test
	public void hidden1_S_apa() throws ParseFailedException {
		// grammar, goal, input
		final Grammar g = this.hidden1();
		final String goal = "S";
		final String text = "a+a";

		final IParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		b.define("S {");
		b.define("  E {");
		b.define("    E1 {");
		b.define("      Bm { empty }");
		b.define("      E{ 'a' }");
		b.define("      '+a'");
		b.define("    }");
		b.define("  }");
		b.define("}");
		final IParseTree expected = b.build();
		Assert.assertEquals(expected, tree);
	}

	@Test
	public void hidden1_S_bapa() throws ParseFailedException {
		// grammar, goal, input
		final Grammar g = this.hidden1();
		final String goal = "S";
		final String text = "ba+a";

		final IParseTree tree = this.process(g, text, goal);
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
		final IParseTree expected = b.build();
		Assert.assertEquals(expected, tree);
	}

	// E = B E '+a' | 'a' ;
	// B = 'b' 'c' | 'd'* ;
	Grammar hidden2() {
		final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("S").choice(new NonTerminal("E"));
		b.rule("E").choice(new NonTerminal("E1"), new NonTerminal("E2"));
		b.rule("E1").concatenation(new NonTerminal("B"), new NonTerminal("E"), new TerminalLiteral("+a"));
		b.rule("E2").concatenation(new TerminalLiteral("a"));
		b.rule("B").choice(new NonTerminal("B1"), new NonTerminal("B2"));
		b.rule("B1").concatenation(new TerminalLiteral("b"), new TerminalLiteral("c"));
		b.rule("B2").multi(0, -1, new TerminalLiteral("d"));
		return b.get();
	}

	// E = F '+a' | 'a' ;
	// F = 'g' 'h' | J ;
	// J = 'k' | E 'l' ;
	Grammar indirect() {
		final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("S").choice(new NonTerminal("E"));
		b.rule("E").choice(new NonTerminal("E1"), new NonTerminal("E2"));
		b.rule("E1").concatenation(new NonTerminal("F"), new TerminalLiteral("+a"));
		b.rule("E2").concatenation(new TerminalLiteral("a"));
		b.rule("F").choice(new NonTerminal("F1"), new NonTerminal("F2"));
		b.rule("F1").concatenation(new TerminalLiteral("g"), new TerminalLiteral("h"));
		b.rule("F2").concatenation(new NonTerminal("J"));
		b.rule("J").choice(new NonTerminal("J1"), new NonTerminal("J2"));
		b.rule("J1").concatenation(new TerminalLiteral("k"));
		b.rule("J2").concatenation(new NonTerminal("K"), new TerminalLiteral("l"));
		return b.get();
	}

	// E = F 'n' | 'n'
	// F = E '+' I* | G '-'
	// G = H 'm' | E
	// H = G 'l'
	// I = '(' A+ ')'
	// A = 'a'

	Grammar interlocking() {
		final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("S").choice(new NonTerminal("E"));
		b.rule("E").choice(new NonTerminal("E1"), new NonTerminal("E2"));
		b.rule("E1").concatenation(new NonTerminal("F"), new TerminalLiteral("n"));
		b.rule("E2").concatenation(new TerminalLiteral("n"));
		b.rule("F").choice(new NonTerminal("F1"), new NonTerminal("F2"));
		b.rule("F1").concatenation(new NonTerminal("E"), new TerminalLiteral("+"), new NonTerminal("Is"));
		b.rule("F2").concatenation(new NonTerminal("G"), new TerminalLiteral("-"));
		b.rule("Is").multi(0, -1, new NonTerminal("I"));
		b.rule("G").choice(new NonTerminal("G1"), new NonTerminal("G2"));
		b.rule("G1").concatenation(new NonTerminal("H"), new TerminalLiteral("m"));
		b.rule("G2").concatenation(new NonTerminal("E"));
		b.rule("H").concatenation(new NonTerminal("G"), new TerminalLiteral("l"));
		b.rule("I").concatenation(new TerminalLiteral("("), new NonTerminal("As"), new TerminalLiteral(")"));
		b.rule("As").multi(1, -1, new NonTerminal("A"));
		b.rule("A").choice(new TerminalLiteral("a"));
		return b.get();
	}

	@Test
	public void interlocking_S_() throws ParseFailedException {
		// grammar, goal, input

		final Grammar g = this.interlocking();
		final String goal = "S";
		final String text = "nlm-n+(aaa)n";

		final IParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		final IParseTree expected = null;
		// final IBranch expected = b.branch("as", b.branch("a", b.leaf("a", "a")));
		Assert.assertEquals(expected, tree);

	}

	// S = X
	// X = P '.x' / 'x'
	// P = P '(n)' / X
	Grammar interlocking2() {
		final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("S").choice(new NonTerminal("E"));
		b.rule("E").choice(new NonTerminal("E1"), new NonTerminal("E2"));
		b.rule("E1").concatenation(new NonTerminal("F"), new TerminalLiteral("n"));
		b.rule("E2").concatenation(new TerminalLiteral("n"));
		b.rule("F").choice(new NonTerminal("F1"), new NonTerminal("F2"));
		b.rule("F1").concatenation(new NonTerminal("E"), new TerminalLiteral("+"), new NonTerminal("Is"));
		b.rule("F2").concatenation(new NonTerminal("G"), new TerminalLiteral("-"));
		b.rule("Is").multi(0, -1, new NonTerminal("I"));
		b.rule("G").choice(new NonTerminal("G1"), new NonTerminal("G2"));
		b.rule("G1").concatenation(new NonTerminal("H"), new TerminalLiteral("m"));
		b.rule("G2").concatenation(new NonTerminal("E"));
		b.rule("H").concatenation(new NonTerminal("G"), new TerminalLiteral("l"));
		b.rule("I").concatenation(new TerminalLiteral("("), new NonTerminal("As"), new TerminalLiteral(")"));
		b.rule("As").multi(1, -1, new NonTerminal("A"));
		b.rule("A").choice(new TerminalLiteral("a"));
		return b.get();
	}

	// S = b | S S | SSS
	Grammar SSSSS() {
		final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("S").choice(new NonTerminal("S1"), new NonTerminal("S2"), new NonTerminal("S3"));
		// TODO:
		return b.get();
	}

	// S = C a | d
	// B = <empty> | a
	// C = b | BCb | b b
	Grammar SBC() {
		final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("S").choice(new NonTerminal("S1"), new NonTerminal("S2"));
		// TODO:
		return b.get();
	}

	// S = b | SSA
	// A = S | <empty>
	Grammar SA() {
		final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("S").choice(new NonTerminal("S1"), new NonTerminal("S2"));
		b.rule("S1").choice(new TerminalLiteral("b"));
		b.rule("S2").concatenation(new NonTerminal("S"), new NonTerminal("S"), new NonTerminal("A"));
		b.rule("A").choice(new NonTerminal("A1"), new NonTerminal("A2"));
		b.rule("A1").concatenation(new NonTerminal("S"));
		b.rule("A2").choice();
		// TODO:
		return b.get();
	}

	@Test
	public void SA_S_b() throws ParseFailedException {
		// grammar, goal, input

		final Grammar g = this.SA();
		final String goal = "S";
		final String text = "b";

		final IParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		b.define("S {");
		b.define("  S1 {");
		b.define("    'b'");
		b.define("  }");
		b.define("}");
		final IParseTree expected = b.build();
		Assert.assertEquals(expected, tree);

	}

	@Test
	public void SA_S_bb() throws ParseFailedException {
		// grammar, goal, input

		final Grammar g = this.SA();
		final String goal = "S";
		final String text = "bb";

		final IParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		b.define("S {");
		b.define("  S1 {");
		b.define("    'b'");
		b.define("  }");
		b.define("}");
		final IParseTree expected = b.build();
		Assert.assertEquals(expected, tree);

	}

	@Test
	public void SA_S_bbb() throws ParseFailedException {
		// grammar, goal, input

		final Grammar g = this.SA();
		final String goal = "S";
		final String text = "bbb";

		final IParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		b.define("S {");
		b.define("  S1 {");
		b.define("    'b'");
		b.define("  }");
		b.define("}");
		final IParseTree expected = b.build();
		Assert.assertEquals(expected, tree);

	}

	@Test
	public void SA_S_b300() throws ParseFailedException {
		// grammar, goal, input

		final Grammar g = this.SA();
		final String goal = "S";
		String text = "";
		// TODO: make this 300 (30)
		for (int i = 0; i < 50; i++) {
			text += "b";
		}

		final IParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		// final ParseTreeBuilder b = this.builder(g, text, goal);
		// b.define("S {");
		// b.define(" S1 {");
		// b.define(" 'b'");
		// b.define(" }");
		// b.define("}");
		// final IParseTree expected = b.build();
		// Assert.assertEquals(expected, tree);

	}
}
