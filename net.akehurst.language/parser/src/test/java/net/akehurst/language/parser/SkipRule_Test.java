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
import net.akehurst.language.ogl.semanticStructure.TerminalPattern;
import net.akehurst.language.parser.sppf.SharedPackedParseTree;

public class SkipRule_Test extends AbstractParser_Test {

	Grammar as() {
		final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.skip("WS").concatenation(new TerminalPattern("\\s+"));

		b.rule("as").multi(1, -1, new NonTerminal("a"));
		b.rule("a").concatenation(new TerminalLiteral("a"));
		return b.get();
	}

	@Test
	public void as_as_a() {
		// grammar, goal, input
		try {
			final Grammar g = this.as();
			final String goal = "as";
			final String text = "a";

			final IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);

			final ParseTreeBuilder b = this.builder(g, text, goal);
			;
			final ISPPFBranch expected = b.branch("as", b.branch("a", b.leaf("a", "a")));

			Assert.assertEquals(expected, tree.getRoot());

		} catch (final ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void as_as_WSa() {
		// grammar, goal, input
		try {
			final Grammar g = this.as();
			final String goal = "as";
			final String text = " a";

			final IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);

			final ParseTreeBuilder b = this.builder(g, text, goal);
			;
			final ISPPFBranch expected = b.branch("as", b.branch("WS", b.leaf("\\s+", " ")), b.branch("a", b.leaf("a", "a")));
			Assert.assertEquals(expected, tree.getRoot());

		} catch (final ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void as_as_aaa() {
		// grammar, goal, input
		try {
			final Grammar g = this.as();
			final String goal = "as";
			final String text = "aaa";

			final IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);

			final ParseTreeBuilder b = this.builder(g, text, goal);
			;
			final ISPPFBranch expected = b.branch("as", b.branch("a", b.leaf("a", "a")), b.branch("a", b.leaf("a", "a")), b.branch("a", b.leaf("a", "a")));
			Assert.assertEquals(expected, tree.getRoot());

		} catch (final ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void as_as_aWSaWSa() {
		// grammar, goal, input
		try {
			final Grammar g = this.as();
			final String goal = "as";
			final String text = "a a a";

			final IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);

			final ParseTreeBuilder b = this.builder(g, text, goal);
			;
			final ISPPFBranch expected = b.branch("as", b.branch("a", b.leaf("a", "a"), b.branch("WS", b.leaf("\\s+", " "))),
					b.branch("a", b.leaf("a", "a"), b.branch("WS", b.leaf("\\s+", " "))), b.branch("a", b.leaf("a", "a")));
			Assert.assertEquals(expected, tree.getRoot());

		} catch (final ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void as_as_WSaWSaWSa() {
		// grammar, goal, input
		try {
			final Grammar g = this.as();
			final String goal = "as";
			final String text = " a a a";

			final IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);

			final ParseTreeBuilder b = this.builder(g, text, goal);
			;
			final ISPPFBranch expected = b.branch("as", b.branch("WS", b.leaf("\\s+", " ")), b.branch("a", b.leaf("a", "a"), b.branch("WS", b.leaf("\\s+", " "))),
					b.branch("a", b.leaf("a", "a"), b.branch("WS", b.leaf("\\s+", " "))), b.branch("a", b.leaf("a", "a")));
			Assert.assertEquals(expected, tree.getRoot());
		} catch (final ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void as_as_aWSaWSaWS() {
		// grammar, goal, input
		try {
			final Grammar g = this.as();
			final String goal = "as";
			final String text = "a a a ";

			final IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);

			final ParseTreeBuilder b = this.builder(g, text, goal);
			;
			final ISPPFBranch expected = b.branch("as", b.branch("a", b.leaf("a", "a"), b.branch("WS", b.leaf("\\s+", " "))),
					b.branch("a", b.leaf("a", "a"), b.branch("WS", b.leaf("\\s+", " "))), b.branch("a", b.leaf("a", "a"), b.branch("WS", b.leaf("\\s+", " "))));
			Assert.assertEquals(expected, tree.getRoot());
		} catch (final ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void as_as_WSaWSaWSaWS() {
		// grammar, goal, input
		try {
			final Grammar g = this.as();
			final String goal = "as";
			final String text = " a a a ";

			final IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);

			final ParseTreeBuilder b = this.builder(g, text, goal);
			;
			final ISPPFBranch expected = b.branch("as", b.branch("WS", b.leaf("\\s+", " ")), b.branch("a", b.leaf("a", "a"), b.branch("WS", b.leaf("\\s+", " "))),
					b.branch("a", b.leaf("a", "a"), b.branch("WS", b.leaf("\\s+", " "))), b.branch("a", b.leaf("a", "a"), b.branch("WS", b.leaf("\\s+", " "))));
			Assert.assertEquals(expected, tree.getRoot());

		} catch (final ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	Grammar asDot() {
		final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.skip("WS").concatenation(new TerminalPattern("\\s+"));

		b.rule("as").multi(1, -1, new NonTerminal("a_dot"));
		b.rule("a_dot").concatenation(new NonTerminal("a"), new TerminalLiteral("."));
		b.rule("a").concatenation(new TerminalLiteral("a"));
		return b.get();
	}

	@Test
	public void asDot_as_a() {
		// grammar, goal, input
		try {
			final Grammar g = this.asDot();
			final String goal = "as";
			final String text = "a.";

			final IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);

			final ParseTreeBuilder b = this.builder(g, text, goal);
			;
			final ISPPFBranch expected = b.branch("as", b.branch("a_dot", b.branch("a", b.leaf("a", "a")), b.leaf(".", ".")));

			Assert.assertEquals(expected, tree.getRoot());

		} catch (final ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void asDot_as_aaa() {
		// grammar, goal, input
		try {
			final Grammar g = this.asDot();
			final String goal = "as";
			final String text = "a.a.a.";

			final IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);

			final ParseTreeBuilder b = this.builder(g, text, goal);
			;
			final ISPPFBranch expected = b.branch("as", b.branch("a_dot", b.branch("a", b.leaf("a", "a")), b.leaf(".", ".")),
					b.branch("a_dot", b.branch("a", b.leaf("a", "a")), b.leaf(".", ".")), b.branch("a_dot", b.branch("a", b.leaf("a", "a")), b.leaf(".", ".")));

			Assert.assertEquals(expected, tree.getRoot());

		} catch (final ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void asDot_as_aWS() {
		// grammar, goal, input
		try {
			final Grammar g = this.asDot();
			final String goal = "as";
			final String text = "a. ";

			final IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);

			final ParseTreeBuilder b = this.builder(g, text, goal);

			final IParseTree expected = new SharedPackedParseTree(
					b.branch("as", b.branch("a_dot", b.branch("a", b.leaf("a", "a")), b.leaf(".", "."), b.branch("WS", b.leaf("\\s+", " ")))));

			Assert.assertEquals(expected, tree);

		} catch (final ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	Grammar S() {
		final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.skip("WS").concatenation(new TerminalPattern("\\s+"));

		b.rule("S").concatenation(new TerminalLiteral("a"));
		return b.get();
	}

	@Test
	public void S_S_a() {
		// grammar, goal, input
		try {
			final Grammar g = this.S();
			final String goal = "S";
			final String text = "a";

			final IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);

			final ParseTreeBuilder b = this.builder(g, text, goal);
			;
			final ISPPFBranch expected = b.branch("S", b.leaf("a"));

			Assert.assertEquals(expected, tree.getRoot());

		} catch (final ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void S_S_WSa() {
		// grammar, goal, input
		try {
			final Grammar g = this.S();
			final String goal = "S";
			final String text = " a";

			final IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);

			final ParseTreeBuilder b = this.builder(g, text, goal);
			;
			final ISPPFBranch expected = b.branch("S", b.branch("WS", b.leaf("\\s+", " ")), b.leaf("a"));
			Assert.assertEquals(expected, tree.getRoot());

		} catch (final ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void S_S_aWS() throws ParseFailedException {
		// grammar, goal, input

		final Grammar g = this.S();
		final String goal = "S";
		final String text = "a ";

		final IParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);

		final ISPPFBranch expected = b.branch("S", b.leaf("a"), b.branch("WS", b.leaf("\\s+", " ")));
		Assert.assertEquals(expected, tree.getRoot());

	}

	@Test
	public void S_S_WSaWS() {
		// grammar, goal, input
		try {
			final Grammar g = this.as();
			final String goal = "as";
			final String text = " a ";

			final IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);

			final ParseTreeBuilder b = this.builder(g, text, goal);
			;
			final ISPPFBranch expected = b.branch("as", b.branch("WS", b.leaf("\\s+", " ")), b.branch("a", b.leaf("a", "a"), b.branch("WS", b.leaf("\\s+", " "))));
			Assert.assertEquals(expected, tree.getRoot());

		} catch (final ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
}
