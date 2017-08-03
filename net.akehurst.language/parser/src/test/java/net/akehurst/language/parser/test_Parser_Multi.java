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
import net.akehurst.language.grammar.parse.tree.ParseTree;
import net.akehurst.language.grammar.parser.forrest.ParseTreeBuilder;
import net.akehurst.language.ogl.semanticStructure.Grammar;
import net.akehurst.language.ogl.semanticStructure.GrammarBuilder;
import net.akehurst.language.ogl.semanticStructure.Namespace;
import net.akehurst.language.ogl.semanticStructure.NonTerminal;
import net.akehurst.language.ogl.semanticStructure.TerminalLiteral;

public class test_Parser_Multi extends AbstractParser_Test {

	Grammar ab01() {
		final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("ab01").concatenation(new NonTerminal("a"), new NonTerminal("b01"));
		b.rule("b01").multi(0, 1, new NonTerminal("b"));
		b.rule("a").concatenation(new TerminalLiteral("a"));
		b.rule("b").concatenation(new TerminalLiteral("b"));

		return b.get();
	}

	Grammar ab01_2() {
		final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("ab01$group1").concatenation(new NonTerminal("a"), new NonTerminal("b"));
		b.rule("ab01").choice(new NonTerminal("ab01$group1"), new NonTerminal("a"));
		b.rule("a").concatenation(new TerminalLiteral("a"));
		b.rule("b").concatenation(new TerminalLiteral("b"));

		return b.get();
	}

	Grammar as13() {
		final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("as").multi(1, 3, new NonTerminal("a"));
		b.rule("a").concatenation(new TerminalLiteral("a"));

		return b.get();
	}

	Grammar as0n() {
		final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("as").multi(0, -1, new NonTerminal("a"));
		b.rule("a").concatenation(new TerminalLiteral("a"));

		return b.get();
	}

	Grammar as0nbs0n() {
		final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("asbs").concatenation(new NonTerminal("as"), new NonTerminal("bs"));
		b.rule("as").multi(0, -1, new NonTerminal("a"));
		b.rule("bs").multi(0, -1, new NonTerminal("b"));
		b.rule("a").concatenation(new TerminalLiteral("a"));
		b.rule("b").concatenation(new TerminalLiteral("b"));

		return b.get();
	}

	Grammar abs1m1() {
		final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("abs").multi(1, -1, new NonTerminal("ab"));
		b.rule("ab").choice(new NonTerminal("a"), new NonTerminal("b"));
		b.rule("a").concatenation(new TerminalLiteral("a"));
		b.rule("b").concatenation(new TerminalLiteral("b"));

		return b.get();
	}

	@Test
	public void as0n_as_empty() throws ParseFailedException {
		// grammar, goal, input

		final Grammar g = this.as0n();
		final String goal = "as";
		final String text = "";

		final IParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);

		final IBranch expected = b.branch("as", b.emptyLeaf("as"));
		Assert.assertEquals(expected, tree.getRoot());

	}

	@Test
	public void as0nbs0n_asbs_empty() throws ParseFailedException {
		// grammar, goal, input

		final Grammar g = this.as0nbs0n();
		final String goal = "asbs";
		final String text = "";

		final IParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);

		final IBranch expected = b.branch("asbs", b.branch("as", b.emptyLeaf("as")), b.branch("bs", b.emptyLeaf("bs")));
		Assert.assertEquals(expected, tree.getRoot());

	}

	@Test
	public void as0nbs0n_asbs_b() throws ParseFailedException {
		// grammar, goal, input

		final Grammar g = this.as0nbs0n();
		final String goal = "asbs";
		final String text = "b";

		final IParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		;
		final IBranch expected = b.branch("asbs", b.branch("as", b.emptyLeaf("as")), b.branch("bs", b.branch("b", b.leaf("b"))));
		Assert.assertEquals(expected, tree.getRoot());

	}

	@Test
	public void as0nbs0n_asbs_bb() throws ParseFailedException {
		// grammar, goal, input

		final Grammar g = this.as0nbs0n();
		final String goal = "asbs";
		final String text = "bb";

		final IParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		;
		final IBranch expected = b.branch("asbs", b.branch("as", b.emptyLeaf("as")), b.branch("bs", b.branch("b", b.leaf("b")), b.branch("b", b.leaf("b"))));
		Assert.assertEquals(expected, tree.getRoot());

	}

	@Test
	public void as13_as_a() throws ParseFailedException {
		// grammar, goal, input

		final Grammar g = this.as13();
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
	public void as13_as_aa() throws ParseFailedException {
		// grammar, goal, input

		final Grammar g = this.as13();
		final String goal = "as";
		final String text = "aa";

		final IParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		;
		final IBranch expected = b.branch("as", b.branch("a", b.leaf("a", "a")), b.branch("a", b.leaf("a", "a")));
		Assert.assertEquals(expected, tree.getRoot());

	}

	@Test
	public void as13_as_aaa() throws ParseFailedException {
		// grammar, goal, input

		final Grammar g = this.as13();
		final String goal = "as";
		final String text = "aaa";

		final IParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);

		final IBranch expected = b.branch("as", b.branch("a", b.leaf("a", "a")), b.branch("a", b.leaf("a", "a")), b.branch("a", b.leaf("a", "a")));
		Assert.assertEquals(expected, tree.getRoot());

	}

	@Test
	public void ab01_ab01_a() throws ParseFailedException {
		// grammar, goal, input

		final Grammar g = this.ab01();
		final String goal = "ab01";
		final String text = "a";

		final IParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		;
		final IBranch expected = b.branch("ab01", b.branch("a", b.leaf("a", "a")), b.branch("b01", b.emptyLeaf("b01")));
		Assert.assertEquals(expected, tree.getRoot());

	}

	@Test
	public void ab01_2_ab01_a() throws ParseFailedException {
		// grammar, goal, input

		final Grammar g = this.ab01_2();
		final String goal = "ab01";
		final String text = "a";

		final IParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		;
		final IBranch expected = b.branch("ab01", b.branch("a", b.leaf("a", "a")));
		Assert.assertEquals(expected, tree.getRoot());

	}

	@Test
	public void ab01_2_ab01_ab() throws ParseFailedException {
		// grammar, goal, input

		final Grammar g = this.ab01_2();
		final String goal = "ab01";
		final String text = "ab";

		final IParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);

		final IParseTree expected = new ParseTree(b.branch("ab01", b.branch("ab01$group1", b.branch("a", b.leaf("a", "a")), b.branch("b", b.leaf("b", "b")))));
		Assert.assertEquals(expected, tree);

	}

	@Test
	public void ab01_ab01_ab() throws ParseFailedException {
		// grammar, goal, input

		final Grammar g = this.ab01();
		final String goal = "ab01";
		final String text = "ab";

		final IParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);

		final IBranch expected = b.branch("ab01", b.branch("a", b.leaf("a", "a")), b.branch("b01", b.branch("b", b.leaf("b", "b"))));
		Assert.assertEquals(expected, tree.getRoot());

	}

	@Test
	public void ab01_ab01_aa() {
		// grammar, goal, input
		try {
			final Grammar g = this.ab01();
			final String goal = "ab01";
			final String text = "aa";

			final IParseTree tree = this.process(g, text, goal);

			Assert.fail("This parse should fail");

		} catch (final ParseFailedException e) {
			// this should occur
		}
	}

	@Test
	public void abs1m1_abs_ababababababab() throws ParseFailedException {
		// grammar, goal, input

		final Grammar g = this.abs1m1();
		final String goal = "abs";
		final String text = "ababababababababababababababababababababababababababababababababababababababababababababababababab";

		final IParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);

		final IBranch expected = b.branch("abs", b.branch("ab", b.branch("a", b.leaf("a"))), b.branch("ab", b.branch("b", b.leaf("b"))),
				b.branch("ab", b.branch("a", b.leaf("a"))), b.branch("ab", b.branch("b", b.leaf("b"))), b.branch("ab", b.branch("a", b.leaf("a"))),
				b.branch("ab", b.branch("b", b.leaf("b"))), b.branch("ab", b.branch("a", b.leaf("a"))), b.branch("ab", b.branch("b", b.leaf("b"))),
				b.branch("ab", b.branch("a", b.leaf("a"))), b.branch("ab", b.branch("b", b.leaf("b"))), b.branch("ab", b.branch("a", b.leaf("a"))),
				b.branch("ab", b.branch("b", b.leaf("b"))), b.branch("ab", b.branch("a", b.leaf("a"))), b.branch("ab", b.branch("b", b.leaf("b"))),
				b.branch("ab", b.branch("a", b.leaf("a"))), b.branch("ab", b.branch("b", b.leaf("b"))), b.branch("ab", b.branch("a", b.leaf("a"))),
				b.branch("ab", b.branch("b", b.leaf("b"))), b.branch("ab", b.branch("a", b.leaf("a"))), b.branch("ab", b.branch("b", b.leaf("b"))),
				b.branch("ab", b.branch("a", b.leaf("a"))), b.branch("ab", b.branch("b", b.leaf("b"))), b.branch("ab", b.branch("a", b.leaf("a"))),
				b.branch("ab", b.branch("b", b.leaf("b"))), b.branch("ab", b.branch("a", b.leaf("a"))), b.branch("ab", b.branch("b", b.leaf("b"))),
				b.branch("ab", b.branch("a", b.leaf("a"))), b.branch("ab", b.branch("b", b.leaf("b"))), b.branch("ab", b.branch("a", b.leaf("a"))),
				b.branch("ab", b.branch("b", b.leaf("b"))), b.branch("ab", b.branch("a", b.leaf("a"))), b.branch("ab", b.branch("b", b.leaf("b"))),
				b.branch("ab", b.branch("a", b.leaf("a"))), b.branch("ab", b.branch("b", b.leaf("b"))), b.branch("ab", b.branch("a", b.leaf("a"))),
				b.branch("ab", b.branch("b", b.leaf("b"))), b.branch("ab", b.branch("a", b.leaf("a"))), b.branch("ab", b.branch("b", b.leaf("b"))),
				b.branch("ab", b.branch("a", b.leaf("a"))), b.branch("ab", b.branch("b", b.leaf("b"))),

				b.branch("ab", b.branch("a", b.leaf("a"))), b.branch("ab", b.branch("b", b.leaf("b"))), b.branch("ab", b.branch("a", b.leaf("a"))),
				b.branch("ab", b.branch("b", b.leaf("b"))), b.branch("ab", b.branch("a", b.leaf("a"))), b.branch("ab", b.branch("b", b.leaf("b"))),
				b.branch("ab", b.branch("a", b.leaf("a"))), b.branch("ab", b.branch("b", b.leaf("b"))), b.branch("ab", b.branch("a", b.leaf("a"))),
				b.branch("ab", b.branch("b", b.leaf("b"))), b.branch("ab", b.branch("a", b.leaf("a"))), b.branch("ab", b.branch("b", b.leaf("b"))),
				b.branch("ab", b.branch("a", b.leaf("a"))), b.branch("ab", b.branch("b", b.leaf("b"))), b.branch("ab", b.branch("a", b.leaf("a"))),
				b.branch("ab", b.branch("b", b.leaf("b"))), b.branch("ab", b.branch("a", b.leaf("a"))), b.branch("ab", b.branch("b", b.leaf("b"))),
				b.branch("ab", b.branch("a", b.leaf("a"))), b.branch("ab", b.branch("b", b.leaf("b"))),

				b.branch("ab", b.branch("a", b.leaf("a"))), b.branch("ab", b.branch("b", b.leaf("b"))), b.branch("ab", b.branch("a", b.leaf("a"))),
				b.branch("ab", b.branch("b", b.leaf("b"))), b.branch("ab", b.branch("a", b.leaf("a"))), b.branch("ab", b.branch("b", b.leaf("b"))),
				b.branch("ab", b.branch("a", b.leaf("a"))), b.branch("ab", b.branch("b", b.leaf("b"))), b.branch("ab", b.branch("a", b.leaf("a"))),
				b.branch("ab", b.branch("b", b.leaf("b"))), b.branch("ab", b.branch("a", b.leaf("a"))), b.branch("ab", b.branch("b", b.leaf("b"))),
				b.branch("ab", b.branch("a", b.leaf("a"))), b.branch("ab", b.branch("b", b.leaf("b"))), b.branch("ab", b.branch("a", b.leaf("a"))),
				b.branch("ab", b.branch("b", b.leaf("b"))), b.branch("ab", b.branch("a", b.leaf("a"))), b.branch("ab", b.branch("b", b.leaf("b"))),
				b.branch("ab", b.branch("a", b.leaf("a"))), b.branch("ab", b.branch("b", b.leaf("b"))),

				b.branch("ab", b.branch("a", b.leaf("a"))), b.branch("ab", b.branch("b", b.leaf("b"))), b.branch("ab", b.branch("a", b.leaf("a"))),
				b.branch("ab", b.branch("b", b.leaf("b"))), b.branch("ab", b.branch("a", b.leaf("a"))), b.branch("ab", b.branch("b", b.leaf("b"))),
				b.branch("ab", b.branch("a", b.leaf("a"))), b.branch("ab", b.branch("b", b.leaf("b"))), b.branch("ab", b.branch("a", b.leaf("a"))),
				b.branch("ab", b.branch("b", b.leaf("b"))), b.branch("ab", b.branch("a", b.leaf("a"))), b.branch("ab", b.branch("b", b.leaf("b"))),
				b.branch("ab", b.branch("a", b.leaf("a"))), b.branch("ab", b.branch("b", b.leaf("b"))), b.branch("ab", b.branch("a", b.leaf("a"))),
				b.branch("ab", b.branch("b", b.leaf("b"))), b.branch("ab", b.branch("a", b.leaf("a"))), b.branch("ab", b.branch("b", b.leaf("b"))));
		Assert.assertEquals(expected, tree.getRoot());

	}

	Grammar nested() {
		final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("top").multi(1, -1, new NonTerminal("level1"));
		b.rule("level1").multi(0, 1, new TerminalLiteral("a"));
		return b.get();
	}

	@Test
	public void nested_top_aa() throws ParseFailedException {
		// grammar, goal, input

		final Grammar g = this.nested();
		final String goal = "top";
		final String text = "aa";

		final IParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		final IParseTree expected = new ParseTree(b.branch("top", b.branch("level1", b.leaf("a")), b.branch("level1", b.leaf("a"))));
		Assert.assertEquals(expected, tree);

	}

}
