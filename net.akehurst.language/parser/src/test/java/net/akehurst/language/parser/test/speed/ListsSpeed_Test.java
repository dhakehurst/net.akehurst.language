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

import net.akehurst.language.core.parser.ParseFailedException;
import net.akehurst.language.core.sppt.ISharedPackedParseTree;
import net.akehurst.language.ogl.semanticStructure.Grammar;
import net.akehurst.language.ogl.semanticStructure.GrammarBuilder;
import net.akehurst.language.ogl.semanticStructure.Namespace;
import net.akehurst.language.ogl.semanticStructure.NonTerminal;
import net.akehurst.language.ogl.semanticStructure.TerminalLiteral;
import net.akehurst.language.parser.AbstractParser_Test;

public class ListsSpeed_Test extends AbstractParser_Test {

	Grammar as_rr() {
		final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("as").choice(new NonTerminal("as$group1"), new NonTerminal("a"));
		b.rule("as$group1").concatenation(new NonTerminal("a"), new NonTerminal("as"));
		b.rule("a").concatenation(new TerminalLiteral("a"));
		return b.get();
	}

	Grammar as_lr() {
		final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("as").choice(new NonTerminal("as$group1"), new NonTerminal("a"));
		b.rule("as$group1").concatenation(new NonTerminal("as"), new NonTerminal("a"));
		b.rule("a").concatenation(new TerminalLiteral("a"));
		return b.get();
	}

	Grammar as_multi() {
		final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("as").multi(1, -1, new NonTerminal("a"));
		b.rule("a").concatenation(new TerminalLiteral("a"));
		return b.get();
	}

	Grammar as2_rr() {
		final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("as").choice(new NonTerminal("as$group1"), new NonTerminal("a"));
		b.rule("as$group1").concatenation(new NonTerminal("a"), new TerminalLiteral(","), new NonTerminal("as"));
		b.rule("a").concatenation(new TerminalLiteral("a"));
		return b.get();
	}

	Grammar as2_lr() {
		final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("as").choice(new NonTerminal("as$group1"), new NonTerminal("a"));
		b.rule("as$group1").concatenation(new NonTerminal("as"), new TerminalLiteral(","), new NonTerminal("a"));
		b.rule("a").concatenation(new TerminalLiteral("a"));
		return b.get();
	}

	Grammar as2_multi() {
		final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("as").multi(1, -1, new NonTerminal("as$group1"));
		b.rule("as$group1").concatenation(new NonTerminal("a"), new TerminalLiteral(","));
		b.rule("a").concatenation(new TerminalLiteral("a"));
		return b.get();
	}

	Grammar as2_sl() {
		final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("as").separatedList(1, -1, new TerminalLiteral(","), new TerminalLiteral("a"));
		b.rule("a").concatenation(new TerminalLiteral("a"));
		return b.get();
	}

	@Test
	public void rr_as_aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa() throws ParseFailedException {
		// grammar, goal, input

		final Grammar g = this.as_rr();
		final String goal = "as";
		final String text = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

		final ISharedPackedParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

	}

	@Test
	public void lr_as_aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa() throws ParseFailedException {
		// grammar, goal, input

		final Grammar g = this.as_lr();
		final String goal = "as";
		final String text = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

		final ISharedPackedParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

	}

	@Test
	public void multi_as_aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa() throws ParseFailedException {
		// grammar, goal, input

		final Grammar g = this.as_multi();
		final String goal = "as";
		final String text = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

		final ISharedPackedParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

	}

	@Test
	public void rr_as2_aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa() throws ParseFailedException {
		// grammar, goal, input

		final Grammar g = this.as2_rr();
		final String goal = "as";
		final String text = "a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a";

		final ISharedPackedParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

	}

	@Test
	public void lr_as2_aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa() throws ParseFailedException {
		// grammar, goal, input

		final Grammar g = this.as2_lr();
		final String goal = "as";
		final String text = "a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a";

		final ISharedPackedParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

	}

	@Test
	public void multi_as2_aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa() throws ParseFailedException {
		// grammar, goal, input

		final Grammar g = this.as2_multi();
		final String goal = "as";
		final String text = "a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,";

		final ISharedPackedParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

	}

	@Test
	public void sl_as2_aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa() throws ParseFailedException {
		// grammar, goal, input

		final Grammar g = this.as2_sl();
		final String goal = "as";
		final String text = "a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a";

		final ISharedPackedParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

	}

}
