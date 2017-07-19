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

public class test_Parser_LeftRecursion extends AbstractParser_Test {

	Grammar as() {
		final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("as").choice(new NonTerminal("as$group1"), new NonTerminal("a"));
		b.rule("as$group1").concatenation(new NonTerminal("as"), new NonTerminal("a"));
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
			final IBranch expected = b.branch("as", b.branch("a", b.leaf("a", "a")));
			Assert.assertEquals(expected, tree.getRoot());

		} catch (final ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
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
		final IParseTree expected = new ParseTree(
				b.branch("as", b.branch("as$group1", b.branch("as", b.branch("a", b.leaf("a", "a"))), b.branch("a", b.leaf("a", "a")))));
		Assert.assertEquals(expected, tree);

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
		final IBranch expected = b.branch("as",
				b.branch("as$group1", b.branch("as", b.branch("as$group1", b.branch("as", b.branch("a", b.leaf("a", "a"))), b.branch("a", b.leaf("a", "a")))),
						b.branch("a", b.leaf("a", "a"))));
		Assert.assertEquals(expected, tree.getRoot());

	}
}
