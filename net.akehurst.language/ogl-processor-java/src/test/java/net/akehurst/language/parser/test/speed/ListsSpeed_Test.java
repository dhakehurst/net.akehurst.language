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
import net.akehurst.language.parser.AbstractParser_Test;

public class ListsSpeed_Test extends AbstractParser_Test {

	GrammarDefault as_rr() {
		final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
		b.rule("as").choice(new NonTerminalDefault("as$group1"), new NonTerminalDefault("a"));
		b.rule("as$group1").concatenation(new NonTerminalDefault("a"), new NonTerminalDefault("as"));
		b.rule("a").concatenation(new TerminalLiteralDefault("a"));
		return b.get();
	}

	GrammarDefault as_lr() {
		final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
		b.rule("as").choice(new NonTerminalDefault("as$group1"), new NonTerminalDefault("a"));
		b.rule("as$group1").concatenation(new NonTerminalDefault("as"), new NonTerminalDefault("a"));
		b.rule("a").concatenation(new TerminalLiteralDefault("a"));
		return b.get();
	}

	GrammarDefault as_multi() {
		final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
		b.rule("as").multi(1, -1, new NonTerminalDefault("a"));
		b.rule("a").concatenation(new TerminalLiteralDefault("a"));
		return b.get();
	}

	GrammarDefault as2_rr() {
		final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
		b.rule("as").choice(new NonTerminalDefault("as$group1"), new NonTerminalDefault("a"));
		b.rule("as$group1").concatenation(new NonTerminalDefault("a"), new TerminalLiteralDefault(","), new NonTerminalDefault("as"));
		b.rule("a").concatenation(new TerminalLiteralDefault("a"));
		return b.get();
	}

	GrammarDefault as2_lr() {
		final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
		b.rule("as").choice(new NonTerminalDefault("as$group1"), new NonTerminalDefault("a"));
		b.rule("as$group1").concatenation(new NonTerminalDefault("as"), new TerminalLiteralDefault(","), new NonTerminalDefault("a"));
		b.rule("a").concatenation(new TerminalLiteralDefault("a"));
		return b.get();
	}

	GrammarDefault as2_multi() {
		final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
		b.rule("as").multi(1, -1, new NonTerminalDefault("as$group1"));
		b.rule("as$group1").concatenation(new NonTerminalDefault("a"), new TerminalLiteralDefault(","));
		b.rule("a").concatenation(new TerminalLiteralDefault("a"));
		return b.get();
	}

	GrammarDefault as2_sl() {
		final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
		b.rule("as").separatedList(1, -1, new TerminalLiteralDefault(","), new TerminalLiteralDefault("a"));
		b.rule("a").concatenation(new TerminalLiteralDefault("a"));
		return b.get();
	}

	@Test
	public void rr_as_aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa() throws ParseFailedException {
		// grammar, goal, input

		final GrammarDefault g = this.as_rr();
		final String goal = "as";
		final String text = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

		final SharedPackedParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

	}

	@Test
	public void lr_as_aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa() throws ParseFailedException {
		// grammar, goal, input

		final GrammarDefault g = this.as_lr();
		final String goal = "as";
		final String text = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

		final SharedPackedParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

	}

	@Test
	public void multi_as_aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa() throws ParseFailedException {
		// grammar, goal, input

		final GrammarDefault g = this.as_multi();
		final String goal = "as";
		final String text = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

		final SharedPackedParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

	}

	@Test
	public void rr_as2_aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa() throws ParseFailedException {
		// grammar, goal, input

		final GrammarDefault g = this.as2_rr();
		final String goal = "as";
		final String text = "a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a";

		final SharedPackedParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

	}

	@Test
	public void lr_as2_aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa() throws ParseFailedException {
		// grammar, goal, input

		final GrammarDefault g = this.as2_lr();
		final String goal = "as";
		final String text = "a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a";

		final SharedPackedParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

	}

	@Test
	public void multi_as2_aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa() throws ParseFailedException {
		// grammar, goal, input

		final GrammarDefault g = this.as2_multi();
		final String goal = "as";
		final String text = "a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,";

		final SharedPackedParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

	}

	@Test
	public void sl_as2_aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa() throws ParseFailedException {
		// grammar, goal, input

		final GrammarDefault g = this.as2_sl();
		final String goal = "as";
		final String text = "a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a,a";

		final SharedPackedParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

	}

}
