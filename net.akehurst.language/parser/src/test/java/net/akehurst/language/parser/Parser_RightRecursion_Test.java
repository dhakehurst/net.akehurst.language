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

import net.akehurst.language.core.parser.IBranch;
import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.core.parser.ParseFailedException;
import net.akehurst.language.grammar.parser.forrest.ParseTreeBuilder;
import net.akehurst.language.ogl.semanticStructure.Grammar;
import net.akehurst.language.ogl.semanticStructure.GrammarBuilder;
import net.akehurst.language.ogl.semanticStructure.Namespace;
import net.akehurst.language.ogl.semanticStructure.NonTerminal;
import net.akehurst.language.ogl.semanticStructure.TerminalLiteral;

public class Parser_RightRecursion_Test extends AbstractParser_Test {

	Grammar as() {
		final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("as").choice(new NonTerminal("as$group1"), new NonTerminal("a"));
		b.rule("as$group1").concatenation(new NonTerminal("a"), new NonTerminal("as"));
		b.rule("a").concatenation(new TerminalLiteral("a"));
		return b.get();
	}

	@Test
	public void as_as_a() throws ParseFailedException {
		// grammar, goal, input

		final Grammar g = this.as();
		final String goal = "as";
		final String text = "a";

		final IParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		;
		final IBranch expected = b.branch("as", b.branch("a", b.leaf("a", "a")));
		Assert.assertEquals(expected, tree.getRoot());

	}

	@Test
	public void as_as_aa() throws ParseFailedException {
		// grammar, goal, input

		final Grammar g = this.as();
		final String goal = "as";
		final String text = "aa";

		final IParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		;
		final IBranch expected = b.branch("as", b.branch("as$group1", b.branch("a", b.leaf("a", "a")), b.branch("as", b.branch("a", b.leaf("a", "a")))));
		Assert.assertEquals(expected, tree.getRoot());

	}

	@Test
	public void as_as_aaa() throws ParseFailedException {
		// grammar, goal, input

		final Grammar g = this.as();
		final String goal = "as";
		final String text = "aaa";

		final IParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		;
		final IBranch expected = b.branch("as", b.branch("as$group1", b.branch("a", b.leaf("a", "a")),
				b.branch("as", b.branch("as$group1", b.branch("a", b.leaf("a", "a")), b.branch("as", b.branch("a", b.leaf("a", "a")))))));
		Assert.assertEquals(expected, tree.getRoot());

	}

	// E = 'a' | E '+a' Bm ;
	// Bm = 'b'?
	Grammar hidden1() {
		final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("S").choice(new NonTerminal("E"));
		b.rule("E").choice(new TerminalLiteral("a"), new NonTerminal("E2"));
		b.rule("E2").concatenation(new NonTerminal("E"), new TerminalLiteral("+a"), new NonTerminal("Bm"));
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
		b.define("  E{");
		b.define("    'a'");
		b.define("  }");
		b.define("}");
		final IParseTree expected = b.build();
		Assert.assertEquals(expected, tree);
	}

	@Test
	public void hidden1_S_apapa() throws ParseFailedException {
		// grammar, goal, input
		final Grammar g = this.hidden1();
		final String goal = "S";
		final String text = "a+a+a";

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
	public void hidden1_S_bapa() throws ParseFailedException {
		// grammar, goal, input
		final Grammar g = this.hidden1();
		final String goal = "S";
		final String text = "a+ab";

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

	// E = 'a' Fm Bm;
	// Fm = F? ;
	// F = '+' E ;
	// Bm = 'b'?
	Grammar trailingEmpty() {
		final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("S").choice(new NonTerminal("E"));
		b.rule("E").concatenation(new TerminalLiteral("a"), new NonTerminal("Fm"), new NonTerminal("Bm"));
		b.rule("Fm").multi(0, 1, new NonTerminal("F"));
		b.rule("F").concatenation(new TerminalLiteral("+"), new NonTerminal("E"));
		b.rule("Bm").multi(0, 1, new TerminalLiteral("b"));
		return b.get();
	}

	@Test
	public void trailingEmpty_S_a() throws ParseFailedException {
		// grammar, goal, input
		final Grammar g = this.trailingEmpty();
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
	public void trailingEmpty_S_apa() throws ParseFailedException {
		// grammar, goal, input
		final Grammar g = this.trailingEmpty();
		final String goal = "S";
		final String text = "a+a";

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
}
