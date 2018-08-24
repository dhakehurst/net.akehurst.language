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
import net.akehurst.language.parser.sppf.ParseTreeBuilder;
import net.akehurst.language.ogl.semanticStructure.GrammarDefault;
import net.akehurst.language.ogl.semanticStructure.GrammarBuilderDefault;
import net.akehurst.language.ogl.semanticStructure.NamespaceDefault;
import net.akehurst.language.ogl.semanticStructure.NonTerminalDefault;
import net.akehurst.language.ogl.semanticStructure.TerminalLiteralDefault;
import net.akehurst.language.parser.sppf.SharedPackedParseTreeSimple;

public class Parser_Concatination_Test extends AbstractParser_Test {

	GrammarDefault aempty() {
		final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
		b.rule("a").choice();
		return b.get();
	}

	GrammarDefault a() {
		final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
		b.rule("a").concatenation(new TerminalLiteralDefault("a"));
		return b.get();
	}

	GrammarDefault abc() {
		final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
		b.rule("abc").concatenation(new TerminalLiteralDefault("abc"));
		return b.get();
	}

	GrammarDefault a_b() {
		final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
		b.rule("ab").concatenation(new NonTerminalDefault("a"), new NonTerminalDefault("b"));
		b.rule("a").concatenation(new TerminalLiteralDefault("a"));
		b.rule("b").concatenation(new TerminalLiteralDefault("b"));
		return b.get();
	}

	GrammarDefault a_b_c() {
		final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
		b.skip("SPACE").concatenation(new TerminalLiteralDefault(" "));
		b.rule("abc").concatenation(new NonTerminalDefault("a"), new NonTerminalDefault("b"), new NonTerminalDefault("c"));
		b.rule("a").concatenation(new TerminalLiteralDefault("a"));
		b.rule("b").concatenation(new TerminalLiteralDefault("b"));
		b.rule("c").concatenation(new TerminalLiteralDefault("c"));
		return b.get();
	}

	GrammarDefault a_b__c() {
		final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
		b.rule("abc").concatenation(new NonTerminalDefault("ab"), new NonTerminalDefault("c"));
		b.rule("ab").concatenation(new NonTerminalDefault("a"), new NonTerminalDefault("b"));
		b.rule("a").concatenation(new TerminalLiteralDefault("a"));
		b.rule("b").concatenation(new TerminalLiteralDefault("b"));
		b.rule("c").concatenation(new TerminalLiteralDefault("c"));
		return b.get();
	}

	GrammarDefault a__b_c() {
		final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
		b.rule("abc").concatenation(new NonTerminalDefault("a"), new NonTerminalDefault("bc"));
		b.rule("bc").concatenation(new NonTerminalDefault("b"), new NonTerminalDefault("c"));
		b.rule("a").concatenation(new TerminalLiteralDefault("a"));
		b.rule("b").concatenation(new TerminalLiteralDefault("b"));
		b.rule("c").concatenation(new TerminalLiteralDefault("c"));
		return b.get();
	}

	@Test
	public void aempty_a_empty() {
		// grammar, goal, input
		try {
			final GrammarDefault g = this.aempty();
			final String goal = "a";
			final String text = "";

			final SharedPackedParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);

			final ParseTreeBuilder b = this.builder(g, text, goal);
			final SPPTBranch expected = b.branch("a", b.emptyLeaf("a"));
			Assert.assertEquals(expected, tree.getRoot());

		} catch (final ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void a_a_a() throws ParseFailedException {
		// grammar, goal, input
		final GrammarDefault g = this.a();
		final String goal = "a";
		final String text = "a";

		final SharedPackedParseTree actual = this.process(g, text, goal);
		Assert.assertNotNull(actual);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		b.define(" a {");
		b.define("  'a'");
		b.define(" }");
		final SharedPackedParseTree expected = b.buildAndAdd();

		Assert.assertEquals(expected, actual);

	}

	@Test(expected = ParseFailedException.class)
	public void a_a_b() throws ParseFailedException {
		// grammar, goal, input

		final GrammarDefault g = this.a();
		final String goal = "a";
		final String text = "b";

		final SharedPackedParseTree tree = this.process(g, text, goal);

		Assert.fail("This parse should fail");
	}

	@Test
	public void abc1_abc_abc() throws ParseFailedException {
		// grammar, goal, input

		final GrammarDefault g = this.abc();
		final String goal = "abc";
		final String text = "abc";

		final SharedPackedParseTree actual = this.process(g, text, goal);
		Assert.assertNotNull(actual);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		b.define(" abc {");
		b.define("  'abc'");
		b.define(" }");
		final SharedPackedParseTree expected = b.buildAndAdd();

		Assert.assertEquals(expected, actual);

	}

	@Test
	public void ab_a_a() {
		// grammar, goal, input
		try {
			final GrammarDefault g = this.a_b();
			final String goal = "a";
			final String text = "a";

			final SharedPackedParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);

			final ParseTreeBuilder b = this.builder(g, text, goal);
			;
			final SPPTBranch expected = b.branch("a", b.leaf("a", "a"));
			Assert.assertEquals(expected, tree.getRoot());

		} catch (final ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void ab_a_b() {
		// grammar, goal, input
		try {
			final GrammarDefault g = this.a_b();
			final String goal = "a";
			final String text = "b";

			final SharedPackedParseTree tree = this.process(g, text, goal);
			Assert.fail("This parse should fail");

		} catch (final ParseFailedException e) {
			// this should occur
		}
	}

	@Test
	public void ab_b_b() {
		// grammar, goal, input
		try {
			final GrammarDefault g = this.a_b();
			final String goal = "b";
			final String text = "b";

			final SharedPackedParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);

			final ParseTreeBuilder b = this.builder(g, text, goal);
			;
			final SPPTBranch expected = b.branch("b", b.leaf("b", "b"));
			Assert.assertEquals(expected, tree.getRoot());

		} catch (final ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void ab_ab_ab() {
		// grammar, goal, input
		try {
			final GrammarDefault g = this.a_b();
			final String goal = "ab";
			final String text = "ab";

			final SharedPackedParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);

			final ParseTreeBuilder b = this.builder(g, text, goal);
			;
			final SPPTBranch expected = b.branch("ab", b.branch("a", b.leaf("a", "a")), b.branch("b", b.leaf("b", "b")));
			Assert.assertEquals(expected, tree.getRoot());

		} catch (final ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void ab_ab_aa() {
		// grammar, goal, input
		try {
			final GrammarDefault g = this.a_b();
			final String goal = "ab";
			final String text = "aa";

			final SharedPackedParseTree tree = this.process(g, text, goal);
			Assert.fail("This parse should fail");

		} catch (final ParseFailedException e) {
			//
		}
	}

	@Test
	public void ab_ab_b() {
		// grammar, goal, input
		try {
			final GrammarDefault g = this.a_b();
			final String goal = "ab";
			final String text = "b";

			final SharedPackedParseTree tree = this.process(g, text, goal);
			Assert.fail("This parse should fail");

		} catch (final ParseFailedException e) {
			//
		}
	}

	@Test
	public void abc_abc_abc() throws ParseFailedException {
		// grammar, goal, input

		final GrammarDefault g = this.a_b__c();
		final String goal = "abc";
		final String text = "abc";

		final SharedPackedParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		;
		final SPPTBranch expected = b.branch("abc", b.branch("ab", b.branch("a", b.leaf("a", "a")), b.branch("b", b.leaf("b", "b"))),
				b.branch("c", b.leaf("c", "c")));
		Assert.assertEquals(expected, tree.getRoot());

	}

	@Test
	public void abc2_abc_abc() {
		// grammar, goal, input
		try {
			final GrammarDefault g = this.a_b_c();
			final String goal = "abc";
			final String text = "abc";

			final SharedPackedParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);

			final ParseTreeBuilder b = this.builder(g, text, goal);
			;
			final SPPTBranch expected = b.branch("abc", b.branch("a", b.leaf("a", "a")), b.branch("b", b.leaf("b", "b")), b.branch("c", b.leaf("c", "c")));
			Assert.assertEquals(expected, tree.getRoot());

		} catch (final ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void abc2_abc_aSPbSPc() throws ParseFailedException {
		// grammar, goal, input

		final GrammarDefault g = this.a_b_c();
		final String goal = "abc";
		final String text = "a b c";

		final SharedPackedParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);

		final SharedPackedParseTree expected = new SharedPackedParseTreeSimple(b.branch("abc", b.branch("a", b.leaf("a", "a"), b.branch("SPACE", b.leaf(" ", " "))),
				b.branch("b", b.leaf("b", "b"), b.branch("SPACE", b.leaf(" ", " "))), b.branch("c", b.leaf("c", "c"))));
		Assert.assertEquals(expected, tree);

	}

	@Test(expected = ParseFailedException.class)
	public void abc_abc_acb() throws ParseFailedException {
		// grammar, goal, input

		final GrammarDefault g = this.a_b__c();
		final String goal = "abc";
		final String text = "acb";

		final SharedPackedParseTree tree = this.process(g, text, goal);
		Assert.fail("This parse should fail");

	}

	@Test
	public void abc_abc_abcd() {
		// grammar, goal, input
		final GrammarDefault g = this.a_b_c();
		final String goal = "abc";
		final String text = "abcd";
		try {

			final SharedPackedParseTree tree = this.process(g, text, goal);
			Assert.fail("This parse should fail");

		} catch (final ParseFailedException e) {
			final SharedPackedParseTree tree = e.getLongestMatch();
			final ParseTreeBuilder b = this.builder(g, text, goal);

			final SPPTBranch expected = b.branch("abc", b.branch("a", b.leaf("a", "a")), b.branch("b", b.leaf("b", "b")), b.branch("c", b.leaf("c", "c")));
			Assert.assertEquals(expected, tree.getRoot());
		}
	}

	@Test
	public void abc4_abc_abc() {
		// grammar, goal, input
		try {
			final GrammarDefault g = this.a__b_c();
			final String goal = "abc";
			final String text = "abc";

			final SharedPackedParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);

			final ParseTreeBuilder b = this.builder(g, text, goal);
			;
			final SPPTBranch expected = b.branch("abc", b.branch("a", b.leaf("a", "a")),
					b.branch("bc", b.branch("b", b.leaf("b", "b")), b.branch("c", b.leaf("c", "c"))));
			Assert.assertEquals(expected, tree.getRoot());

		} catch (final ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

}
