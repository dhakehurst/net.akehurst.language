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
import net.akehurst.language.core.sppt.ISharedPackedParseTree;
import net.akehurst.language.grammar.parser.forrest.ParseTreeBuilder;
import net.akehurst.language.ogl.semanticStructure.Grammar;
import net.akehurst.language.ogl.semanticStructure.GrammarBuilder;
import net.akehurst.language.ogl.semanticStructure.Namespace;
import net.akehurst.language.ogl.semanticStructure.NonTerminal;
import net.akehurst.language.ogl.semanticStructure.TerminalLiteral;
import net.akehurst.language.ogl.semanticStructure.TerminalPattern;

public class test_Special extends AbstractParser_Test {
	/**
	 * <code>
	 * S : 'a' S B B | 'a' ;
	 * B : 'b' ? ;
	 * </code>
	 */
	Grammar S() {
		final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		// b.rule("S").choice(new NonTerminal("S1"), new NonTerminal("S2"));
		b.rule("S_group1").concatenation(new TerminalLiteral("a"), new NonTerminal("S"), new NonTerminal("B"), new NonTerminal("B"));
		b.rule("S").choice(new NonTerminal("S_group1"), new TerminalLiteral("a"));
		// b.rule("B").choice(new NonTerminal("B1"), new NonTerminal("B2"));
		b.rule("B").multi(0, 1, new TerminalLiteral("b"));
		return b.get();
	}

	@Test
	public void S_S_aab() throws ParseFailedException {
		// grammar, goal, input

		final Grammar g = this.S();
		final String goal = "S";
		final String text = "aab";

		final ISharedPackedParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		b.define("S {");
		b.define("  S_group1 {");
		b.define("    'a'");
		b.define("    S { 'a' }");
		b.define("    B { 'b' }");
		b.define("    B { $empty }");
		b.define("  }");
		b.define("}");
		b.buildAndAdd();

		b.define("S {");
		b.define("  S_group1 {");
		b.define("    'a'");
		b.define("    S { 'a' }");
		b.define("    B { $empty }");
		b.define("    B { 'b' }");
		b.define("  }");
		b.define("}");
		final ISharedPackedParseTree expected = b.buildAndAdd();

		Assert.assertEquals(expected, tree);

	}

	/**
	 * <code>
	 * S : S S | 'a' ;
	 * </code>
	 */
	Grammar S2() {
		final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("S").choice(new NonTerminal("S1"), new TerminalLiteral("a"));
		b.rule("S1").concatenation(new NonTerminal("S"), new NonTerminal("S"));
		return b.get();
	}

	@Test
	public void S2_S_aaa() throws ParseFailedException {
		// grammar, goal, input

		final Grammar g = this.S2();
		final String goal = "S";
		final String text = "aaa";

		final ISharedPackedParseTree actual = this.process(g, text, goal);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		b.define("S {");
		b.define("  S1 {");
		b.define("    S {");
		b.define("      S1 {");
		b.define("        S {'a'}");
		b.define("        S {'a'}");
		b.define("      }");
		b.define("    }");
		b.define("    S {'a'}");
		b.define("  }");
		b.define("}");
		b.buildAndAdd();
		b.define("S {");
		b.define("  S1 {");
		b.define("    S {'a'}");
		b.define("    S {");
		b.define("      S1 {");
		b.define("        S {'a'}");
		b.define("        S {'a'}");
		b.define("      }");
		b.define("    }");
		b.define("  }");
		b.define("}");
		final ISharedPackedParseTree expected = b.buildAndAdd();

		Assert.assertNotNull("Parse failed", actual);
		Assert.assertEquals(expected, actual);

	}

	/**
	 * <code>
	 * S : S S S | S S | 'a' ;
	 * </code>
	 */
	Grammar S3() {
		final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("S").choice(new NonTerminal("S1"), new NonTerminal("S2"), new TerminalLiteral("a"));
		b.rule("S1").concatenation(new NonTerminal("S"), new NonTerminal("S"), new NonTerminal("S"));
		b.rule("S2").concatenation(new NonTerminal("S"), new NonTerminal("S"));
		return b.get();
	}

	@Test
	public void S3_S_aaa() throws ParseFailedException {
		// grammar, goal, input

		final Grammar g = this.S3();
		final String goal = "S";
		final String text = "aaa";

		final ISharedPackedParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		b.define("S {");
		b.define("  S1 {");
		b.define("    S {'a'}");
		b.define("    S {'a'}");
		b.define("    S {'a'}");
		b.define("  }");
		b.define("}");
		b.buildAndAdd();

		b.define("S {");
		b.define("  S2 {");
		b.define("    S {'a'}");
		b.define("    S {");
		b.define("      S2 {");
		b.define("        S {'a'}");
		b.define("        S {'a'}");
		b.define("      }");
		b.define("    }");
		b.define("  }");
		b.define("}");
		b.buildAndAdd();

		b.define("S {");
		b.define("  S2 {");
		b.define("    S {");
		b.define("      S2 {");
		b.define("        S {'a'}");
		b.define("        S {'a'}");
		b.define("      }");
		b.define("    }");
		b.define("    S {'a'}");
		b.define("  }");
		b.define("}");

		final ISharedPackedParseTree expected = b.buildAndAdd();

		Assert.assertTrue(tree.contains(expected));
		Assert.assertEquals(expected, tree);

	}

	Grammar parametersG() {
		final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");

		b.rule("S").priorityChoice(new NonTerminal("fpfps"), new NonTerminal("rpfps"));
		b.rule("fpfps").concatenation(new NonTerminal("fp"), new NonTerminal("fpList1"));
		b.rule("rpfps").concatenation(new NonTerminal("rp"), new NonTerminal("fpList2"));
		b.rule("fpList1").multi(0, -1, new NonTerminal("cmrFp1"));
		b.rule("cmrFp1").concatenation(new TerminalLiteral(","), new NonTerminal("fp"));
		b.rule("fpList2").multi(0, -1, new NonTerminal("cmrFp2"));
		b.rule("cmrFp2").concatenation(new TerminalLiteral(","), new NonTerminal("fp"));
		b.rule("fp").concatenation(new NonTerminal("vms"), new NonTerminal("unannType"));
		b.rule("rp").concatenation(new NonTerminal("anns"), new NonTerminal("unannType"), new TerminalLiteral("this"));
		b.rule("unannType").choice(new NonTerminal("unannReferenceType"));
		b.rule("unannReferenceType").choice(new NonTerminal("unannClassOrInterfaceType"));
		b.rule("unannClassOrInterfaceType").choice(new NonTerminal("unannClassType_lfno_unannClassOrInterfaceType"));
		b.rule("unannClassType_lfno_unannClassOrInterfaceType").concatenation(new NonTerminal("Id"), new NonTerminal("typeArgs"));
		b.rule("vms").multi(0, -1, new NonTerminal("vm"));
		b.rule("vm").choice(new TerminalLiteral("final"), new NonTerminal("ann"));
		b.rule("anns").multi(0, -1, new NonTerminal("ann"));
		b.rule("ann").choice(new TerminalLiteral("@"), new NonTerminal("Id"));
		b.rule("typeArgs").multi(0, 1, new NonTerminal("typeArgList"));
		b.rule("typeArgList").concatenation(new TerminalLiteral("<"), new TerminalLiteral(">"));
		b.rule("Id").choice(new TerminalLiteral("a"));

		return b.get();
	}

	@Test
	public void parameters() throws ParseFailedException {
		// FIXME: This test repeats work.
		// the fp and rp nodes duplicate the parsing, we only want to do it once
		// grammar, goal, input

		final Grammar g = this.parametersG();
		final String goal = "S";
		final String text = "a";

		final ISharedPackedParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		b.define("S {");
		b.define("fpfps {");
		b.define("  fp {");
		b.define("    vms { $empty }");
		b.define("    unannType {");
		b.define("      unannReferenceType {");
		b.define("        unannClassOrInterfaceType {");
		b.define("          unannClassType_lfno_unannClassOrInterfaceType {");
		b.define("            Id { 'a' }");
		b.define("            typeArgs { $empty }");
		b.define("          }");
		b.define("        }");
		b.define("      }");
		b.define("    }");
		b.define("  }");
		b.define("  fpList1 { $empty }");
		b.define("}");
		b.define("}");
		final ISharedPackedParseTree expected = b.buildAndAdd();
		// final IBranch expected = b.branch("S",
		// b.branch("fpfps",
		// b.branch("fp", b.branch("vms", b.emptyLeaf("vms")), b.branch("unannType",
		// b.branch("unannReferenceType",
		// b.branch("unannClassOrInterfaceType", b.branch("unannClassType_lfno_unannClassOrInterfaceType",
		// b.branch("Id", b.leaf("a")), b.branch("typeArgs", b.emptyLeaf("typeArgs")))))))),
		// b.branch("fpList1", b.emptyLeaf("fpList1")));
		Assert.assertEquals(expected, tree);
	}

	// S = NP VP | S PP ;
	// NP = n | det n | NP PP ;
	// PP = p NP ;
	// VP = v NP ;
	// n = 'I' | 'man' | 'telescope' | 'park' ;
	// v = 'saw' | ;
	// p = 'in' | 'with' ;
	// det = 'a' | 'the' ;
	Grammar tomita() {
		final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.skip("WS").concatenation(new TerminalPattern("\\s+"));
		b.rule("S").choice(new NonTerminal("S1"), new NonTerminal("S2"));
		b.rule("S1").concatenation(new NonTerminal("NP"), new NonTerminal("VP"));
		b.rule("S2").concatenation(new NonTerminal("S"), new NonTerminal("PP"));
		b.rule("NP").choice(new NonTerminal("n"), new NonTerminal("NP2"), new NonTerminal("NP3"));
		b.rule("NP2").concatenation(new NonTerminal("det"), new NonTerminal("n"));
		b.rule("NP3").concatenation(new NonTerminal("NP"), new NonTerminal("PP"));
		b.rule("PP").concatenation(new NonTerminal("p"), new NonTerminal("NP"));
		b.rule("VP").concatenation(new NonTerminal("v"), new NonTerminal("NP"));
		b.rule("n").choice(new TerminalLiteral("I"), new TerminalLiteral("man"), new TerminalLiteral("telescope"), new TerminalLiteral("park"));
		b.rule("v").choice(new TerminalLiteral("saw"));
		b.rule("p").choice(new TerminalLiteral("in"), new TerminalLiteral("with"));
		b.rule("det").choice(new TerminalLiteral("a"), new TerminalLiteral("the"));
		return b.get();
	}

	@Test
	public void tomita_S_sentence() throws ParseFailedException {
		// grammar, goal, input

		final Grammar g = this.tomita();
		final String goal = "S";
		final String text = "I saw a man in the park with a telescope";

		final ISharedPackedParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		b.define("S {");
		b.define("n { 'I' } ");
		b.define("v { 'saw' }");
		b.define("det { 'a' }");
		b.define("n { 'man' } ");
		b.define("p { 'in' }");
		b.define("det { 'the' }");
		b.define("n { 'park' } ");
		b.define("p { 'with' }");
		b.define("  NP {");
		b.define("    NP2 {");
		b.define("      det { 'a' }");
		b.define("      n { 'telescope' } ");
		b.define("    }");
		b.define("  }");
		b.define("}");
		b.buildAndAdd();

		b.define("S {");
		b.define("n { 'I' } ");
		b.define("v { 'saw' }");
		b.define("det { 'a' }");
		b.define("n { 'man' } ");
		b.define("p { 'in' }");
		b.define("det { 'the' }");
		b.define("n { 'park' } ");
		b.define("p { 'with' }");
		b.define("det { 'a' }");
		b.define("n { 'telescope' } ");
		b.define("}");

		final ISharedPackedParseTree expected = b.buildAndAdd();
		Assert.assertEquals(expected, tree);

	}

	// S = Ab | A2c
	// A2 = A2a | a
	// A = aA | Aa | a

	// S = T
	// T = Ab | TTT
	// A = T bAAA | TTb | <empty>
}
