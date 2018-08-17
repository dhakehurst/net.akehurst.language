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
package net.akehurst.language.parser.test.speed;

import org.junit.Assert;
import org.junit.Test;

import net.akehurst.language.api.parser.ParseFailedException;
import net.akehurst.language.api.sppt.SharedPackedParseTree;
import net.akehurst.language.ogl.semanticStructure.GrammarDefault;
import net.akehurst.language.ogl.semanticStructure.GrammarBuilderDefault;
import net.akehurst.language.ogl.semanticStructure.NamespaceDefault;
import net.akehurst.language.ogl.semanticStructure.NonTerminalDefault;
import net.akehurst.language.ogl.semanticStructure.TerminalLiteralDefault;
import net.akehurst.language.ogl.semanticStructure.TerminalPatternDefault;
import net.akehurst.language.parser.AbstractParser_Test;

public class Speed_Test extends AbstractParser_Test {

	GrammarDefault abcds() {
		final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
		b.rule("abcds").choice(new NonTerminalDefault("abcd"), new NonTerminalDefault("abcds.group1"));
		b.rule("abcds.group1").concatenation(new NonTerminalDefault("abcd"), new NonTerminalDefault("abcds"));
		b.rule("abcd").concatenation(new NonTerminalDefault("abs"), new NonTerminalDefault("cds"));
		b.rule("abs").choice(new NonTerminalDefault("ab"), new NonTerminalDefault("abs.group1"));
		b.rule("abs.group1").concatenation(new NonTerminalDefault("ab"), new NonTerminalDefault("abs"));
		b.rule("cds").choice(new NonTerminalDefault("cd"), new NonTerminalDefault("cds.group1"));
		b.rule("cds.group1").concatenation(new NonTerminalDefault("cd"), new NonTerminalDefault("cds"));
		b.rule("ab").concatenation(new NonTerminalDefault("a"), new NonTerminalDefault("b"));
		b.rule("cd").concatenation(new NonTerminalDefault("c"), new NonTerminalDefault("d"));
		b.rule("a").choice(new TerminalLiteralDefault("a"));
		b.rule("b").choice(new TerminalLiteralDefault("b"));
		b.rule("c").choice(new TerminalLiteralDefault("c"));
		b.rule("d").choice(new TerminalLiteralDefault("d"));
		return b.get();
	}

	@Test
	public void abcds_abcds_abcd() {
		// grammar, goal, input
		try {
			final GrammarDefault g = this.abcds();
			final String goal = "abcds";
			final String text = "abcd";

			final SharedPackedParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);

		} catch (final ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void abcds_abcds_abcdabcd() {
		// grammar, goal, input
		try {
			final GrammarDefault g = this.abcds();
			final String goal = "abcds";
			final String text = "abcdabcd";

			final SharedPackedParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);

		} catch (final ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void abcds_abcds_ababcdcdababcdcd() {
		// grammar, goal, input
		try {
			final GrammarDefault g = this.abcds();
			final String goal = "abcds";
			final String text = "ababcdcdababcdcd";

			final SharedPackedParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);

		} catch (final ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void abcds_abcds_abababcdcdcdabababcdcdcdabababcdcdcd() {
		// grammar, goal, input
		try {
			final GrammarDefault g = this.abcds();
			final String goal = "abcds";
			final String text = "abababcdcdcdabababcdcdcdabababcdcdcd";

			final SharedPackedParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);

		} catch (final ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	// TODO: make these longer
	@Test
	public void abcds_abcds_abababcdcdcdabababcdcdcdabababcdcdcdabababcdcdcdabababcdcdcd() {
		// grammar, goal, input
		try {
			final GrammarDefault g = this.abcds();
			final String goal = "abcds";
			final String text = "abababcdcdcdabababcdcdcdabababcdcdcdabababcdcdcdabababcdcdcd";

			final SharedPackedParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);

		} catch (final ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void abcds_abcds_abababcdcdcdabababcdcdcdabababcdcdcdabababcdcdcdabababcdcdcdabababcdcdcdabababcdcdcdabababcdcdcd() {
		// grammar, goal, input
		try {
			final GrammarDefault g = this.abcds();
			final String goal = "abcds";
			final String text = "abababcdcdcdabababcdcdcdabababcdcdcdabababcdcdcdabababcdcdcdabababcdcdcdabababcdcdcdabababcdcdcd";

			final SharedPackedParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);

		} catch (final ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void abcds_abcds_2_x_abababcdcdcdabababcdcdcdabababcdcdcdabababcdcdcdabababcdcdcdabababcdcdcdabababcdcdcdabababcdcdcd() {
		// grammar, goal, input
		try {
			final GrammarDefault g = this.abcds();
			final String goal = "abcds";
			final String text = "abababcdcdcdabababcdcdcdabababcdcdcdabababcdcdcdabababcdcdcdabababcdcdcdabababcdcdcdabababcdcdcdabababcdcdcdabababcdcdcdabababcdcdcdabababcdcdcdabababcdcdcdabababcdcdcdabababcdcdcdabababcdcdcd";

			final SharedPackedParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);

		} catch (final ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	GrammarDefault params() {
		final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
		b.rule("params").concatenation(new TerminalLiteralDefault("("), new NonTerminalDefault("paramList"), new TerminalLiteralDefault(")"));
		b.rule("paramList").separatedList(0, -1, new TerminalLiteralDefault(","), new NonTerminalDefault("param"));
		b.rule("param").concatenation(new NonTerminalDefault("type"), new NonTerminalDefault("id"));
		b.rule("type").choice(new TerminalLiteralDefault("int"));
		b.rule("id").choice(new TerminalPatternDefault("[a-zA-Z_][a-zA-Z_0-9]*"));
		b.skip("WS").concatenation(new TerminalPatternDefault("\\s+"));
		return b.get();
	}

	@Test
	public void params_1() {
		// grammar, goal, input
		try {
			final GrammarDefault g = this.params();
			final String goal = "params";
			final String text = "(int a)";

			final SharedPackedParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);

		} catch (final ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void params_2() {
		// grammar, goal, input
		try {
			final GrammarDefault g = this.params();
			final String goal = "params";
			final String text = "(int a,int b)";

			final SharedPackedParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);

		} catch (final ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void params_3() throws ParseFailedException {
		// grammar, goal, input

		final GrammarDefault g = this.params();
		final String goal = "params";
		final String text = "(int a, int b, int c)";

		final SharedPackedParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

	}
}
