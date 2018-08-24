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
import net.akehurst.language.api.sppt.SharedPackedParseTree;
import net.akehurst.language.parser.sppf.ParseTreeBuilder;
import net.akehurst.language.ogl.semanticStructure.GrammarDefault;
import net.akehurst.language.ogl.semanticStructure.GrammarBuilderDefault;
import net.akehurst.language.ogl.semanticStructure.NamespaceDefault;
import net.akehurst.language.ogl.semanticStructure.NonTerminalDefault;
import net.akehurst.language.ogl.semanticStructure.TerminalLiteralDefault;

public class Parser_Empty_Test extends AbstractParser_Test {

	GrammarDefault empty() {
		final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
		b.rule("S").choice();
		return b.get();
	}

	@Test
	public void empty_S_empty() throws ParseFailedException {
		// grammar, goal, input

		final GrammarDefault g = this.empty();
		final String goal = "S";
		final String text = "";

		final SharedPackedParseTree actual = this.process(g, text, goal);
		Assert.assertNotNull(actual);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		b.define("S {");
		b.define("  $empty");
		b.define("}");
		final SharedPackedParseTree expected = b.buildAndAdd();

		Assert.assertEquals(expected, actual);
	}

	@Test(expected = ParseFailedException.class)
	public void empty_S_a() throws ParseFailedException {
		// grammar, goal, input

		final GrammarDefault g = this.empty();
		final String goal = "S";
		final String text = "a";

		final SharedPackedParseTree tree = this.process(g, text, goal);
		Assert.fail("This parse should fail");

	}

	GrammarDefault multi0m1() {
		final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
		b.rule("S").multi(0, -1, new TerminalLiteralDefault("a"));
		return b.get();
	}

	@Test
	public void multi0_S_empty() throws ParseFailedException {
		// grammar, goal, input

		final GrammarDefault g = this.multi0m1();
		final String goal = "S";
		final String text = "";

		final SharedPackedParseTree actual = this.process(g, text, goal);
		Assert.assertNotNull(actual);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		b.define("S {");
		b.define("  $empty");
		b.define("}");
		final SharedPackedParseTree expected = b.buildAndAdd();

		Assert.assertEquals(expected, actual);

	}

	@Test
	public void multi0_S_a() throws ParseFailedException {
		// grammar, goal, input

		final GrammarDefault g = this.multi0m1();
		final String goal = "S";
		final String text = "a";

		final SharedPackedParseTree actual = this.process(g, text, goal);
		Assert.assertNotNull(actual);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		b.define("S {");
		b.define("  'a'");
		b.define("}");
		final SharedPackedParseTree expected = b.buildAndAdd();

		Assert.assertEquals(expected, actual);

	}

	GrammarDefault aeas() {
		final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
		b.rule("S").concatenation(new NonTerminalDefault("ae"), new NonTerminalDefault("as"));
		b.rule("ae").multi(0, 1, new TerminalLiteralDefault("a"));
		b.rule("as").multi(0, -1, new TerminalLiteralDefault("a"));
		return b.get();
	}

	@Test
	public void aas_S_empty() throws ParseFailedException {
		// grammar, goal, input

		final GrammarDefault g = this.aeas();
		final String goal = "S";
		final String text = "a";

		final SharedPackedParseTree actual = this.process(g, text, goal);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		b.define("S {");
		b.define("  ae { 'a' }");
		b.define("  as { $empty }");
		b.define("}");
		b.buildAndAdd();

		b.define("S {");
		b.define("  ae { $empty }");
		b.define("  as { 'a' }");
		b.define("}");
		final SharedPackedParseTree expected = b.buildAndAdd();

		Assert.assertNotNull(actual);
		Assert.assertEquals(expected, actual);

	}
}
