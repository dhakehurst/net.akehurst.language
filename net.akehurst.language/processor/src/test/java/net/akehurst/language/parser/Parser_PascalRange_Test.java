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
import net.akehurst.language.grammar.parser.forrest.ParseTreeBuilder;
import net.akehurst.language.ogl.semanticStructure.GrammarDefault;
import net.akehurst.language.ogl.semanticStructure.GrammarBuilderDefault;
import net.akehurst.language.ogl.semanticStructure.NamespaceDefault;
import net.akehurst.language.ogl.semanticStructure.NonTerminalDefault;
import net.akehurst.language.ogl.semanticStructure.TerminalLiteralDefault;
import net.akehurst.language.ogl.semanticStructure.TerminalPatternDefault;

public class Parser_PascalRange_Test extends AbstractParser_Test {
	/*
	 * expr : range | real ; range: integer '..' integer ; integer : "[0-9]+" ; real : "([0-9]+[.][0-9]*)|([.][0-9]+)" ;
	 *
	 */
	GrammarDefault pascal() {
		final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
		b.rule("expr").choice(new NonTerminalDefault("range"), new NonTerminalDefault("real"));
		b.rule("range").concatenation(new NonTerminalDefault("integer"), new TerminalLiteralDefault(".."), new NonTerminalDefault("integer"));
		b.rule("integer").concatenation(new TerminalPatternDefault("[0-9]+"));
		b.rule("real").concatenation(new TerminalPatternDefault("([0-9]+[.][0-9]*)|([.][0-9]+)"));

		return b.get();
	}

	/**
	 * @param
	 * @throws ParseFailedException
	 */
	@Test
	public void pascal_expr_p5() throws ParseFailedException {
		// grammar, goal, input

		final GrammarDefault g = this.pascal();
		final String goal = "expr";
		final String text = ".5";

		final SharedPackedParseTree actual = this.process(g, text, goal);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		b.define("expr {");
		b.define("  real { '([0-9]+[.][0-9]*)|([.][0-9]+)' : '.5' }");
		b.define("}");
		final SharedPackedParseTree expected = b.buildAndAdd();

		Assert.assertNotNull(actual);
		Assert.assertEquals(expected, actual);

	}

	@Test
	public void pascal_expr_1p() throws ParseFailedException {
		// grammar, goal, input

		final GrammarDefault g = this.pascal();
		final String goal = "expr";
		final String text = "1.";

		final SharedPackedParseTree actual = this.process(g, text, goal);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		b.define("expr {");
		b.define("  real { '([0-9]+[.][0-9]*)|([.][0-9]+)' : '1.' }");
		b.define("}");
		final SharedPackedParseTree expected = b.buildAndAdd();

		Assert.assertNotNull(actual);
		Assert.assertEquals(expected, actual);

	}

	@Test
	public void pascal_expr_1to5() throws ParseFailedException {
		// grammar, goal, input

		final GrammarDefault g = this.pascal();
		final String goal = "expr";
		final String text = "1..5";

		final SharedPackedParseTree actual = this.process(g, text, goal);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		b.define("expr {");
		b.define("  range {");
		b.define("    integer { '[0-9]+' : '1' }");
		b.define("    '..'");
		b.define("    integer { '[0-9]+' : '5' }");
		b.define("  }");
		b.define("}");
		final SharedPackedParseTree expected = b.buildAndAdd();

		Assert.assertNotNull(actual);
		Assert.assertEquals(expected, actual);

	}
}
