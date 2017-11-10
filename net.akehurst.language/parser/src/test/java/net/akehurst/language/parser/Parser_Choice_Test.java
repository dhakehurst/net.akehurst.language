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
import net.akehurst.language.core.sppf.IParseTree;
import net.akehurst.language.core.sppf.ISPPFBranch;
import net.akehurst.language.core.sppf.ISharedPackedParseForest;
import net.akehurst.language.grammar.parser.forrest.ParseTreeBuilder;
import net.akehurst.language.ogl.semanticStructure.Grammar;
import net.akehurst.language.ogl.semanticStructure.GrammarBuilder;
import net.akehurst.language.ogl.semanticStructure.Namespace;
import net.akehurst.language.ogl.semanticStructure.NonTerminal;
import net.akehurst.language.ogl.semanticStructure.TerminalLiteral;
import net.akehurst.language.parser.sppf.SharedPackedParseForest;

public class Parser_Choice_Test extends AbstractParser_Test {

	Grammar abc() {
		final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("abc").choice(new NonTerminal("a"), new NonTerminal("b"), new NonTerminal("c"));
		b.rule("a").concatenation(new TerminalLiteral("a"));
		b.rule("b").concatenation(new TerminalLiteral("b"));
		b.rule("c").concatenation(new TerminalLiteral("c"));
		return b.get();
	}

	Grammar aempty() {
		final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("a").choice();
		return b.get();
	}

	@Test
	public void aempty_a_empty() throws ParseFailedException {
		// grammar, goal, input

		final Grammar g = this.aempty();
		final String goal = "a";
		final String text = "";

		final ISharedPackedParseForest tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		// b.define(" a {");
		// b.define(" empty");
		// b.define(" }");
		final IParseTree expected = new SharedPackedParseForest(b.branch("a", b.emptyLeaf("a")));
		// final IBranch expected = ;
		Assert.assertEquals(expected, tree);

	}

	@Test
	public void abc_abc_a() throws ParseFailedException {
		// grammar, goal, input

		final Grammar g = this.abc();
		final String goal = "abc";
		final String text = "a";

		final ISharedPackedParseForest tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		b.define("abc {");
		b.define(" a {");
		b.define(" 'a'");
		b.define(" }");
		b.define("}");
		// final IParseTree expected = new SharedPackedParseForest(b.branch("abc", b.branch("a", b.leaf("a"))));
		final IParseTree expected = b.build();
		Assert.assertTrue(tree.contains(expected));

	}

	@Test
	public void abc_abc_b() throws ParseFailedException {
		// grammar, goal, input

		final Grammar g = this.abc();
		final String goal = "abc";
		final String text = "b";

		final ISharedPackedParseForest tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		final ISPPFBranch expected = b.branch("abc", b.branch("b", b.leaf("b", "b")));

		Assert.assertEquals(expected, tree.getRoot());

	}

	@Test
	public void abc_abc_c() throws ParseFailedException {
		// grammar, goal, input

		final Grammar g = this.abc();
		final String goal = "abc";
		final String text = "c";

		final ISharedPackedParseForest tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		final ISPPFBranch expected = b.branch("abc", b.branch("c", b.leaf("c", "c")));

		Assert.assertEquals(expected, tree.getRoot());

	}
}
