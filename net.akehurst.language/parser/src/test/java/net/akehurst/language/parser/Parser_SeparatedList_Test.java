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
import net.akehurst.language.core.sppt.SPPTBranch;
import net.akehurst.language.core.sppt.SharedPackedParseTree;
import net.akehurst.language.grammar.parser.forrest.ParseTreeBuilder;
import net.akehurst.language.ogl.semanticStructure.GrammarStructure;
import net.akehurst.language.ogl.semanticStructure.GrammarBuilder;
import net.akehurst.language.ogl.semanticStructure.Namespace;
import net.akehurst.language.ogl.semanticStructure.NonTerminal;
import net.akehurst.language.ogl.semanticStructure.TerminalLiteral;
import net.akehurst.language.parser.sppf.SharedPackedParseTreeSimple;

public class Parser_SeparatedList_Test extends AbstractParser_Test {

	GrammarStructure as1() {
		final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("as").separatedList(1, -1, new TerminalLiteral(","), new TerminalLiteral("a"));

		return b.get();
	}

	GrammarStructure as2() {
		final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("as").separatedList(1, -1, new TerminalLiteral(","), new NonTerminal("a"));
		b.rule("a").concatenation(new TerminalLiteral("a"));

		return b.get();
	}

	GrammarStructure asSP() {
		final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.skip("SP").concatenation(new TerminalLiteral(" "));
		b.rule("as").separatedList(1, -1, new TerminalLiteral(","), new NonTerminal("a"));
		b.rule("a").concatenation(new TerminalLiteral("a"));

		return b.get();
	}

	GrammarStructure asb() {
		final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("asb").concatenation(new NonTerminal("as"), new NonTerminal("b"));
		b.rule("as").separatedList(1, -1, new TerminalLiteral(","), new NonTerminal("b"));
		b.rule("a").concatenation(new TerminalLiteral("a"));
		b.rule("b").concatenation(new TerminalLiteral("b"));

		return b.get();
	}

	GrammarStructure as0n() {
		final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("as").separatedList(0, -1, new TerminalLiteral(","), new NonTerminal("a"));
		b.rule("a").concatenation(new TerminalLiteral("a"));

		return b.get();
	}

	@Test
	public void as0n_as_empty() throws ParseFailedException {
		// grammar, goal, input

		final GrammarStructure g = this.as0n();
		final String goal = "as";
		final String text = "";

		final SharedPackedParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);

		final SPPTBranch expected = b.branch("as", b.emptyLeaf("as"));
		Assert.assertEquals(expected, tree.getRoot());

	}

	@Test
	public void as1_as_a() {
		// grammar, goal, input
		try {
			final GrammarStructure g = this.as1();
			final String goal = "as";
			final String text = "a";

			final SharedPackedParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);

			final ParseTreeBuilder b = this.builder(g, text, goal);
			;
			final SPPTBranch expected = b.branch("as", b.leaf("a", "a"));
			Assert.assertEquals(expected, tree.getRoot());

		} catch (final ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void as1_as_aa() throws ParseFailedException {
		// grammar, goal, input

		final GrammarStructure g = this.as1();
		final String goal = "as";
		final String text = "a,a";

		final SharedPackedParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);

		final SPPTBranch expected = b.branch("as", b.leaf("a", "a"), b.leaf(",", ","), b.leaf("a", "a"));
		Assert.assertEquals(expected, tree.getRoot());

	}

	@Test
	public void as1_as_aaa() throws ParseFailedException {
		// grammar, goal, input

		final GrammarStructure g = this.as1();
		final String goal = "as";
		final String text = "a,a,a";

		final SharedPackedParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);

		final SPPTBranch expected = b.branch("as", b.leaf("a", "a"), b.leaf(",", ","), b.leaf("a", "a"), b.leaf(",", ","), b.leaf("a", "a"));
		Assert.assertEquals(expected, tree.getRoot());

	}

	@Test
	public void as2_as_a() throws ParseFailedException {
		// grammar, goal, input

		final GrammarStructure g = this.as2();
		final String goal = "as";
		final String text = "a";

		final SharedPackedParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);

		final SPPTBranch expected = b.branch("as", b.branch("a", b.leaf("a", "a")));
		Assert.assertEquals(expected, tree.getRoot());

	}

	@Test
	public void as2_as_aa() {
		// grammar, goal, input
		try {
			final GrammarStructure g = this.as2();
			final String goal = "as";
			final String text = "a,a";

			final SharedPackedParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);

			final ParseTreeBuilder b = this.builder(g, text, goal);
			;
			final SPPTBranch expected = b.branch("as", b.branch("a", b.leaf("a", "a")), b.leaf(",", ","), b.branch("a", b.leaf("a", "a")));
			Assert.assertEquals(expected, tree.getRoot());

		} catch (final ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void as2_as_aaa() {
		// grammar, goal, input
		try {
			final GrammarStructure g = this.as2();
			final String goal = "as";
			final String text = "a,a,a";

			final SharedPackedParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);

			final ParseTreeBuilder b = this.builder(g, text, goal);
			;
			final SPPTBranch expected = b.branch("as", b.branch("a", b.leaf("a", "a")), b.leaf(",", ","), b.branch("a", b.leaf("a", "a")), b.leaf(",", ","),
					b.branch("a", b.leaf("a", "a")));
			Assert.assertEquals(expected, tree.getRoot());

		} catch (final ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void asSP_as_aaa() {
		// grammar, goal, input
		try {
			final GrammarStructure g = this.asSP();
			final String goal = "as";
			final String text = "a, a, a";

			final SharedPackedParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);

			final ParseTreeBuilder b = this.builder(g, text, goal);
			;
			final SPPTBranch expected = b.branch("as", b.branch("a", b.leaf("a", "a")), b.leaf(",", ","), b.branch("SP", b.leaf(" ", " ")),
					b.branch("a", b.leaf("a", "a")), b.leaf(",", ","), b.branch("SP", b.leaf(" ", " ")), b.branch("a", b.leaf("a", "a")));
			Assert.assertEquals(expected, tree.getRoot());

		} catch (final ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void asSP_as_aaaa() {
		// grammar, goal, input
		try {
			final GrammarStructure g = this.asSP();
			final String goal = "as";
			final String text = "a, a, a, a";

			final SharedPackedParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);

			final ParseTreeBuilder b = this.builder(g, text, goal);
			;
			final SPPTBranch expected = b.branch("as", b.branch("a", b.leaf("a", "a")), b.leaf(",", ","), b.branch("SP", b.leaf(" ", " ")),
					b.branch("a", b.leaf("a", "a")), b.leaf(",", ","), b.branch("SP", b.leaf(" ", " ")), b.branch("a", b.leaf("a", "a")), b.leaf(",", ","),
					b.branch("SP", b.leaf(" ", " ")), b.branch("a", b.leaf("a", "a")));
			Assert.assertEquals(expected, tree.getRoot());
		} catch (final ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void asSP_as_aaa2() throws ParseFailedException {
		// grammar, goal, input

		final GrammarStructure g = this.asSP();
		final String goal = "as";
		final String text = "a , a , a ";

		final SharedPackedParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		final SharedPackedParseTree expected = new SharedPackedParseTreeSimple(b.branch("as", b.branch("a", b.leaf("a", "a"), b.branch("SP", b.leaf(" ", " "))),
				b.leaf(",", ","), b.branch("SP", b.leaf(" ", " ")), b.branch("a", b.leaf("a", "a"), b.branch("SP", b.leaf(" ", " "))), b.leaf(",", ","),
				b.branch("SP", b.leaf(" ", " ")), b.branch("a", b.leaf("a", "a"), b.branch("SP", b.leaf(" ", " ")))));
		Assert.assertEquals(expected, tree);

	}

	GrammarStructure as03() {
		final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("as").separatedList(0, 3, new TerminalLiteral(","), new NonTerminal("a"));
		b.rule("a").concatenation(new TerminalLiteral("a"));

		return b.get();
	}

	@Test
	public void as03_as_empty() throws ParseFailedException {
		// grammar, goal, input

		final GrammarStructure g = this.as0n();
		final String goal = "as";
		final String text = "";

		final SharedPackedParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);

		final SPPTBranch expected = b.branch("as", b.emptyLeaf("as"));
		Assert.assertEquals(expected, tree.getRoot());

	}

	@Test
	public void as03_as_a() throws ParseFailedException {
		// grammar, goal, input

		final GrammarStructure g = this.as03();
		final String goal = "as";
		final String text = "a";

		final SharedPackedParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);

		final SPPTBranch expected = b.branch("as", b.branch("a", b.leaf("a")));
		Assert.assertEquals(expected, tree.getRoot());

	}

	@Test(expected = ParseFailedException.class)
	public void as03_as_a4() throws ParseFailedException {
		// grammar, goal, input

		final GrammarStructure g = this.as03();
		final String goal = "as";
		final String text = "a,a,a,a";

		final SharedPackedParseTree tree = this.process(g, text, goal);

		Assert.fail("this should fail");
	}
}
