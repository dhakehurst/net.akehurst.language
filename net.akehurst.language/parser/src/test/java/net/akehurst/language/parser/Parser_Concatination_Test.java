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
import net.akehurst.language.core.sppt.ISPBranch;
import net.akehurst.language.core.sppt.ISharedPackedParseTree;
import net.akehurst.language.grammar.parser.forrest.ParseTreeBuilder;
import net.akehurst.language.ogl.semanticStructure.Grammar;
import net.akehurst.language.ogl.semanticStructure.GrammarBuilder;
import net.akehurst.language.ogl.semanticStructure.Namespace;
import net.akehurst.language.ogl.semanticStructure.NonTerminal;
import net.akehurst.language.ogl.semanticStructure.TerminalLiteral;
import net.akehurst.language.parser.sppf.SharedPackedParseTree;

public class Parser_Concatination_Test extends AbstractParser_Test {

	Grammar aempty() {
		final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("a").choice();
		return b.get();
	}

	Grammar a() {
		final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("a").concatenation(new TerminalLiteral("a"));
		return b.get();
	}

	Grammar abc() {
		final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("abc").concatenation(new TerminalLiteral("abc"));
		return b.get();
	}

	Grammar a_b() {
		final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("ab").concatenation(new NonTerminal("a"), new NonTerminal("b"));
		b.rule("a").concatenation(new TerminalLiteral("a"));
		b.rule("b").concatenation(new TerminalLiteral("b"));
		return b.get();
	}

	Grammar a_b_c() {
		final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.skip("SPACE").concatenation(new TerminalLiteral(" "));
		b.rule("abc").concatenation(new NonTerminal("a"), new NonTerminal("b"), new NonTerminal("c"));
		b.rule("a").concatenation(new TerminalLiteral("a"));
		b.rule("b").concatenation(new TerminalLiteral("b"));
		b.rule("c").concatenation(new TerminalLiteral("c"));
		return b.get();
	}

	Grammar a_b__c() {
		final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("abc").concatenation(new NonTerminal("ab"), new NonTerminal("c"));
		b.rule("ab").concatenation(new NonTerminal("a"), new NonTerminal("b"));
		b.rule("a").concatenation(new TerminalLiteral("a"));
		b.rule("b").concatenation(new TerminalLiteral("b"));
		b.rule("c").concatenation(new TerminalLiteral("c"));
		return b.get();
	}

	Grammar a__b_c() {
		final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("abc").concatenation(new NonTerminal("a"), new NonTerminal("bc"));
		b.rule("bc").concatenation(new NonTerminal("b"), new NonTerminal("c"));
		b.rule("a").concatenation(new TerminalLiteral("a"));
		b.rule("b").concatenation(new TerminalLiteral("b"));
		b.rule("c").concatenation(new TerminalLiteral("c"));
		return b.get();
	}

	@Test
	public void aempty_a_empty() {
		// grammar, goal, input
		try {
			final Grammar g = this.aempty();
			final String goal = "a";
			final String text = "";

			final ISharedPackedParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);

			final ParseTreeBuilder b = this.builder(g, text, goal);
			final ISPBranch expected = b.branch("a", b.emptyLeaf("a"));
			Assert.assertEquals(expected, tree.getRoot());

		} catch (final ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void a_a_a() throws ParseFailedException {
		// grammar, goal, input
		final Grammar g = this.a();
		final String goal = "a";
		final String text = "a";

		final ISharedPackedParseTree actual = this.process(g, text, goal);
		Assert.assertNotNull(actual);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		b.define(" a {");
		b.define("  'a'");
		b.define(" }");
		final ISharedPackedParseTree expected = b.buildAndAdd();

		Assert.assertEquals(expected, actual);

	}

	@Test(expected = ParseFailedException.class)
	public void a_a_b() throws ParseFailedException {
		// grammar, goal, input

		final Grammar g = this.a();
		final String goal = "a";
		final String text = "b";

		final ISharedPackedParseTree tree = this.process(g, text, goal);

		Assert.fail("This parse should fail");
	}

	@Test
	public void abc1_abc_abc() throws ParseFailedException {
		// grammar, goal, input

		final Grammar g = this.abc();
		final String goal = "abc";
		final String text = "abc";

		final ISharedPackedParseTree actual = this.process(g, text, goal);
		Assert.assertNotNull(actual);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		b.define(" abc {");
		b.define("  'abc'");
		b.define(" }");
		final ISharedPackedParseTree expected = b.buildAndAdd();

		Assert.assertEquals(expected, actual);

	}

	@Test
	public void ab_a_a() {
		// grammar, goal, input
		try {
			final Grammar g = this.a_b();
			final String goal = "a";
			final String text = "a";

			final ISharedPackedParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);

			final ParseTreeBuilder b = this.builder(g, text, goal);
			;
			final ISPBranch expected = b.branch("a", b.leaf("a", "a"));
			Assert.assertEquals(expected, tree.getRoot());

		} catch (final ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void ab_a_b() {
		// grammar, goal, input
		try {
			final Grammar g = this.a_b();
			final String goal = "a";
			final String text = "b";

			final ISharedPackedParseTree tree = this.process(g, text, goal);
			Assert.fail("This parse should fail");

		} catch (final ParseFailedException e) {
			// this should occur
		}
	}

	@Test
	public void ab_b_b() {
		// grammar, goal, input
		try {
			final Grammar g = this.a_b();
			final String goal = "b";
			final String text = "b";

			final ISharedPackedParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);

			final ParseTreeBuilder b = this.builder(g, text, goal);
			;
			final ISPBranch expected = b.branch("b", b.leaf("b", "b"));
			Assert.assertEquals(expected, tree.getRoot());

		} catch (final ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void ab_ab_ab() {
		// grammar, goal, input
		try {
			final Grammar g = this.a_b();
			final String goal = "ab";
			final String text = "ab";

			final ISharedPackedParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);

			final ParseTreeBuilder b = this.builder(g, text, goal);
			;
			final ISPBranch expected = b.branch("ab", b.branch("a", b.leaf("a", "a")), b.branch("b", b.leaf("b", "b")));
			Assert.assertEquals(expected, tree.getRoot());

		} catch (final ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void ab_ab_aa() {
		// grammar, goal, input
		try {
			final Grammar g = this.a_b();
			final String goal = "ab";
			final String text = "aa";

			final ISharedPackedParseTree tree = this.process(g, text, goal);
			Assert.fail("This parse should fail");

		} catch (final ParseFailedException e) {
			//
		}
	}

	@Test
	public void ab_ab_b() {
		// grammar, goal, input
		try {
			final Grammar g = this.a_b();
			final String goal = "ab";
			final String text = "b";

			final ISharedPackedParseTree tree = this.process(g, text, goal);
			Assert.fail("This parse should fail");

		} catch (final ParseFailedException e) {
			//
		}
	}

	@Test
	public void abc_abc_abc() throws ParseFailedException {
		// grammar, goal, input

		final Grammar g = this.a_b__c();
		final String goal = "abc";
		final String text = "abc";

		final ISharedPackedParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		;
		final ISPBranch expected = b.branch("abc", b.branch("ab", b.branch("a", b.leaf("a", "a")), b.branch("b", b.leaf("b", "b"))),
				b.branch("c", b.leaf("c", "c")));
		Assert.assertEquals(expected, tree.getRoot());

	}

	@Test
	public void abc2_abc_abc() {
		// grammar, goal, input
		try {
			final Grammar g = this.a_b_c();
			final String goal = "abc";
			final String text = "abc";

			final ISharedPackedParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);

			final ParseTreeBuilder b = this.builder(g, text, goal);
			;
			final ISPBranch expected = b.branch("abc", b.branch("a", b.leaf("a", "a")), b.branch("b", b.leaf("b", "b")), b.branch("c", b.leaf("c", "c")));
			Assert.assertEquals(expected, tree.getRoot());

		} catch (final ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void abc2_abc_aSPbSPc() throws ParseFailedException {
		// grammar, goal, input

		final Grammar g = this.a_b_c();
		final String goal = "abc";
		final String text = "a b c";

		final ISharedPackedParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);

		final ISharedPackedParseTree expected = new SharedPackedParseTree(b.branch("abc", b.branch("a", b.leaf("a", "a"), b.branch("SPACE", b.leaf(" ", " "))),
				b.branch("b", b.leaf("b", "b"), b.branch("SPACE", b.leaf(" ", " "))), b.branch("c", b.leaf("c", "c"))));
		Assert.assertEquals(expected, tree);

	}

	@Test(expected = ParseFailedException.class)
	public void abc_abc_acb() throws ParseFailedException {
		// grammar, goal, input

		final Grammar g = this.a_b__c();
		final String goal = "abc";
		final String text = "acb";

		final ISharedPackedParseTree tree = this.process(g, text, goal);
		Assert.fail("This parse should fail");

	}

	@Test
	public void abc_abc_abcd() {
		// grammar, goal, input
		final Grammar g = this.a_b_c();
		final String goal = "abc";
		final String text = "abcd";
		try {

			final ISharedPackedParseTree tree = this.process(g, text, goal);
			Assert.fail("This parse should fail");

		} catch (final ParseFailedException e) {
			final ISharedPackedParseTree tree = e.getLongestMatch();
			final ParseTreeBuilder b = this.builder(g, text, goal);

			final ISPBranch expected = b.branch("abc", b.branch("a", b.leaf("a", "a")), b.branch("b", b.leaf("b", "b")), b.branch("c", b.leaf("c", "c")));
			Assert.assertEquals(expected, tree.getRoot());
		}
	}

	@Test
	public void abc4_abc_abc() {
		// grammar, goal, input
		try {
			final Grammar g = this.a__b_c();
			final String goal = "abc";
			final String text = "abc";

			final ISharedPackedParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);

			final ParseTreeBuilder b = this.builder(g, text, goal);
			;
			final ISPBranch expected = b.branch("abc", b.branch("a", b.leaf("a", "a")),
					b.branch("bc", b.branch("b", b.leaf("b", "b")), b.branch("c", b.leaf("c", "c"))));
			Assert.assertEquals(expected, tree.getRoot());

		} catch (final ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

}
