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
import net.akehurst.language.ogl.semanticStructure.GrammarDefault;
import net.akehurst.language.ogl.semanticStructure.GrammarBuilderDefault;
import net.akehurst.language.ogl.semanticStructure.NamespaceDefault;
import net.akehurst.language.ogl.semanticStructure.NonTerminalDefault;
import net.akehurst.language.ogl.semanticStructure.TerminalLiteralDefault;
import net.akehurst.language.ogl.semanticStructure.TerminalPatternDefault;
import net.akehurst.language.parser.sppf.SharedPackedParseTreeSimple;

public class SkipRule_Test extends AbstractParser_Test {

	GrammarDefault as() {
		final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
		b.skip("WS").concatenation(new TerminalPatternDefault("\\s+"));

		b.rule("as").multi(1, -1, new NonTerminalDefault("a"));
		b.rule("a").concatenation(new TerminalLiteralDefault("a"));
		return b.get();
	}

	@Test
	public void as_as_a() {
		// grammar, goal, input
		try {
			final GrammarDefault g = this.as();
			final String goal = "as";
			final String text = "a";

			final SharedPackedParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);

			final ParseTreeBuilder b = this.builder(g, text, goal);
			;
			final SPPTBranch expected = b.branch("as", b.branch("a", b.leaf("a", "a")));

			Assert.assertEquals(expected, tree.getRoot());

		} catch (final ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void as_as_WSa() {
		// grammar, goal, input
		try {
			final GrammarDefault g = this.as();
			final String goal = "as";
			final String text = " a";

			final SharedPackedParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);

			final ParseTreeBuilder b = this.builder(g, text, goal);
			;
			final SPPTBranch expected = b.branch("as", b.branch("WS", b.leaf("\\s+", " ")), b.branch("a", b.leaf("a", "a")));
			Assert.assertEquals(expected, tree.getRoot());

		} catch (final ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void as_as_aaa() {
		// grammar, goal, input
		try {
			final GrammarDefault g = this.as();
			final String goal = "as";
			final String text = "aaa";

			final SharedPackedParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);

			final ParseTreeBuilder b = this.builder(g, text, goal);
			;
			final SPPTBranch expected = b.branch("as", b.branch("a", b.leaf("a", "a")), b.branch("a", b.leaf("a", "a")), b.branch("a", b.leaf("a", "a")));
			Assert.assertEquals(expected, tree.getRoot());

		} catch (final ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void as_as_aWSaWSa() {
		// grammar, goal, input
		try {
			final GrammarDefault g = this.as();
			final String goal = "as";
			final String text = "a a a";

			final SharedPackedParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);

			final ParseTreeBuilder b = this.builder(g, text, goal);
			;
			final SPPTBranch expected = b.branch("as", b.branch("a", b.leaf("a", "a"), b.branch("WS", b.leaf("\\s+", " "))),
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
			final GrammarDefault g = this.as();
			final String goal = "as";
			final String text = " a a a";

			final SharedPackedParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);

			final ParseTreeBuilder b = this.builder(g, text, goal);
			;
			final SPPTBranch expected = b.branch("as", b.branch("WS", b.leaf("\\s+", " ")),
					b.branch("a", b.leaf("a", "a"), b.branch("WS", b.leaf("\\s+", " "))), b.branch("a", b.leaf("a", "a"), b.branch("WS", b.leaf("\\s+", " "))),
					b.branch("a", b.leaf("a", "a")));
			Assert.assertEquals(expected, tree.getRoot());
		} catch (final ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void as_as_aWSaWSaWS() {
		// grammar, goal, input
		try {
			final GrammarDefault g = this.as();
			final String goal = "as";
			final String text = "a a a ";

			final SharedPackedParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);

			final ParseTreeBuilder b = this.builder(g, text, goal);
			;
			final SPPTBranch expected = b.branch("as", b.branch("a", b.leaf("a", "a"), b.branch("WS", b.leaf("\\s+", " "))),
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
			final GrammarDefault g = this.as();
			final String goal = "as";
			final String text = " a a a ";

			final SharedPackedParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);

			final ParseTreeBuilder b = this.builder(g, text, goal);
			;
			final SPPTBranch expected = b.branch("as", b.branch("WS", b.leaf("\\s+", " ")),
					b.branch("a", b.leaf("a", "a"), b.branch("WS", b.leaf("\\s+", " "))), b.branch("a", b.leaf("a", "a"), b.branch("WS", b.leaf("\\s+", " "))),
					b.branch("a", b.leaf("a", "a"), b.branch("WS", b.leaf("\\s+", " "))));
			Assert.assertEquals(expected, tree.getRoot());

		} catch (final ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	GrammarDefault asDot() {
		final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
		b.skip("WS").concatenation(new TerminalPatternDefault("\\s+"));

		b.rule("as").multi(1, -1, new NonTerminalDefault("a_dot"));
		b.rule("a_dot").concatenation(new NonTerminalDefault("a"), new TerminalLiteralDefault("."));
		b.rule("a").concatenation(new TerminalLiteralDefault("a"));
		return b.get();
	}

	@Test
	public void asDot_as_a() {
		// grammar, goal, input
		try {
			final GrammarDefault g = this.asDot();
			final String goal = "as";
			final String text = "a.";

			final SharedPackedParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);

			final ParseTreeBuilder b = this.builder(g, text, goal);
			;
			final SPPTBranch expected = b.branch("as", b.branch("a_dot", b.branch("a", b.leaf("a", "a")), b.leaf(".", ".")));

			Assert.assertEquals(expected, tree.getRoot());

		} catch (final ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void asDot_as_aaa() {
		// grammar, goal, input
		try {
			final GrammarDefault g = this.asDot();
			final String goal = "as";
			final String text = "a.a.a.";

			final SharedPackedParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);

			final ParseTreeBuilder b = this.builder(g, text, goal);
			;
			final SPPTBranch expected = b.branch("as", b.branch("a_dot", b.branch("a", b.leaf("a", "a")), b.leaf(".", ".")),
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
			final GrammarDefault g = this.asDot();
			final String goal = "as";
			final String text = "a. ";

			final SharedPackedParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);

			final ParseTreeBuilder b = this.builder(g, text, goal);

			final SharedPackedParseTree expected = new SharedPackedParseTreeSimple(
					b.branch("as", b.branch("a_dot", b.branch("a", b.leaf("a", "a")), b.leaf(".", "."), b.branch("WS", b.leaf("\\s+", " ")))));

			Assert.assertEquals(expected, tree);

		} catch (final ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	GrammarDefault S() {
		final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
		b.skip("WS").concatenation(new TerminalPatternDefault("\\s+"));

		b.rule("S").concatenation(new TerminalLiteralDefault("a"));
		return b.get();
	}

	@Test
	public void S_S_a() {
		// grammar, goal, input
		try {
			final GrammarDefault g = this.S();
			final String goal = "S";
			final String text = "a";

			final SharedPackedParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);

			final ParseTreeBuilder b = this.builder(g, text, goal);
			;
			final SPPTBranch expected = b.branch("S", b.leaf("a"));

			Assert.assertEquals(expected, tree.getRoot());

		} catch (final ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void S_S_WSa() {
		// grammar, goal, input
		try {
			final GrammarDefault g = this.S();
			final String goal = "S";
			final String text = " a";

			final SharedPackedParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);

			final ParseTreeBuilder b = this.builder(g, text, goal);
			;
			final SPPTBranch expected = b.branch("S", b.branch("WS", b.leaf("\\s+", " ")), b.leaf("a"));
			Assert.assertEquals(expected, tree.getRoot());

		} catch (final ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void S_S_aWS() throws ParseFailedException {
		// grammar, goal, input

		final GrammarDefault g = this.S();
		final String goal = "S";
		final String text = "a ";

		final SharedPackedParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);

		final SPPTBranch expected = b.branch("S", b.leaf("a"), b.branch("WS", b.leaf("\\s+", " ")));
		Assert.assertEquals(expected, tree.getRoot());

	}

	@Test
	public void S_S_WSaWS() {
		// grammar, goal, input
		try {
			final GrammarDefault g = this.as();
			final String goal = "as";
			final String text = " a ";

			final SharedPackedParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);

			final ParseTreeBuilder b = this.builder(g, text, goal);
			;
			final SPPTBranch expected = b.branch("as", b.branch("WS", b.leaf("\\s+", " ")),
					b.branch("a", b.leaf("a", "a"), b.branch("WS", b.leaf("\\s+", " "))));
			Assert.assertEquals(expected, tree.getRoot());

		} catch (final ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}
}
