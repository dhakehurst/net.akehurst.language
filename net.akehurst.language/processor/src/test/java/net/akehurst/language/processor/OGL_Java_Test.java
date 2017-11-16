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
package net.akehurst.language.processor;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import net.akehurst.language.core.analyser.UnableToAnalyseExeception;
import net.akehurst.language.core.grammar.IGrammar;
import net.akehurst.language.core.grammar.RuleNotFoundException;
import net.akehurst.language.core.parser.IParser;
import net.akehurst.language.core.parser.ParseFailedException;
import net.akehurst.language.core.parser.ParseTreeException;
import net.akehurst.language.core.sppt.ISharedPackedParseTree;
import net.akehurst.language.grammar.parser.ScannerLessParser3;
import net.akehurst.language.grammar.parser.forrest.ParseTreeBuilder;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleSetBuilder;
import net.akehurst.language.ogl.semanticStructure.Grammar;

public class OGL_Java_Test {

	RuntimeRuleSetBuilder parseTreeFactory;

	@Before
	public void before() {
		this.parseTreeFactory = new RuntimeRuleSetBuilder();
	}

	ParseTreeBuilder builder(final Grammar grammar, final String text, final String goal) {
		return new ParseTreeBuilder(this.parseTreeFactory, grammar, goal, text, 0);
	}

	ISharedPackedParseTree process(final IGrammar grammar, final Reader reader, final String goalName) throws ParseFailedException {
		try {
			final IParser parser = new ScannerLessParser3(this.parseTreeFactory, grammar);
			final ISharedPackedParseTree tree = parser.parse(goalName, reader);
			return tree;
		} catch (final RuleNotFoundException e) {
			Assert.fail(e.getMessage());
			return null;
		} catch (final ParseTreeException e) {
			Assert.fail(e.getMessage());
			return null;
		}
	}

	ISharedPackedParseTree parse(final String grammarFile, final String goal, final String text)
			throws FileNotFoundException, ParseFailedException, UnableToAnalyseExeception {

		final OGLanguageProcessor proc = new OGLanguageProcessor();
		final FileReader reader = new FileReader(Paths.get(grammarFile).toFile());
		final Grammar grammar = proc.process(reader, "grammarDefinition", Grammar.class);

		final ISharedPackedParseTree tree = this.process(grammar, new StringReader(text), goal);

		return tree;

	}

	@Test
	public void java1_empty() throws FileNotFoundException, ParseFailedException, UnableToAnalyseExeception {
		final ISharedPackedParseTree tree = this.parse("src/test/resources/Java1.og", "compilationUnit", "");
		Assert.assertNotNull(tree);
	}

	@Test
	public void java1_comment() throws FileNotFoundException, ParseFailedException, UnableToAnalyseExeception {
		final ISharedPackedParseTree tree = this.parse("src/test/resources/Java1.og", "compilationUnit", "/* comment */");
		Assert.assertNotNull(tree);
	}

	@Test
	public void java1_package() throws FileNotFoundException, ParseFailedException, UnableToAnalyseExeception {
		final ISharedPackedParseTree tree = this.parse("src/test/resources/Java1.og", "compilationUnit", "package");
		Assert.assertNotNull(tree);
	}

	@Test
	public void java1_comment_package() throws FileNotFoundException, ParseFailedException, UnableToAnalyseExeception {
		final String text = "/* comment */  package";
		final ISharedPackedParseTree tree = this.parse("src/test/resources/Java1.og", "compilationUnit", text);
		Assert.assertNotNull(tree);
	}

	@Test
	public void java1_package_comment() throws FileNotFoundException, ParseFailedException, UnableToAnalyseExeception {
		final String text = "package  /* comment */";
		final ISharedPackedParseTree tree = this.parse("src/test/resources/Java1.og", "compilationUnit", text);
		Assert.assertNotNull(tree);
	}

	@Test
	public void java2_empty() throws FileNotFoundException, ParseFailedException, UnableToAnalyseExeception {
		final String text = "";
		final ISharedPackedParseTree tree = this.parse("src/test/resources/Java2.og", "compilationUnit", text);
		Assert.assertNotNull(tree);
	}

	@Test
	public void java2_comment() throws FileNotFoundException, ParseFailedException, UnableToAnalyseExeception {
		final String text = "/* comment */";
		final ISharedPackedParseTree tree = this.parse("src/test/resources/Java2.og", "compilationUnit", text);
		Assert.assertNotNull(tree);
	}

	@Test
	public void java2_comment_package() throws FileNotFoundException, ParseFailedException, UnableToAnalyseExeception {
		final String text = "/* comment */  package";
		final ISharedPackedParseTree tree = this.parse("src/test/resources/Java2.og", "compilationUnit", text);
		Assert.assertNotNull(tree);
	}

	@Test
	public void java2_package_comment() throws FileNotFoundException, ParseFailedException, UnableToAnalyseExeception {
		final String text = "package  /* comment */";
		final ISharedPackedParseTree tree = this.parse("src/test/resources/Java2.og", "compilationUnit", text);
		Assert.assertNotNull(tree);
	}

	@Test
	public void java2_import() throws FileNotFoundException, ParseFailedException, UnableToAnalyseExeception {
		final String text = "import";
		final ISharedPackedParseTree tree = this.parse("src/test/resources/Java2.og", "compilationUnit", text);
		Assert.assertNotNull(tree);
	}

	@Test
	public void java2_type() throws FileNotFoundException, ParseFailedException, UnableToAnalyseExeception {
		final String text = "type";
		final ISharedPackedParseTree tree = this.parse("src/test/resources/Java2.og", "compilationUnit", text);
		Assert.assertNotNull(tree);
	}

	@Test
	public void java3_empty() throws FileNotFoundException, ParseFailedException, UnableToAnalyseExeception {
		final String text = "";
		final ISharedPackedParseTree tree = this.parse("src/test/resources/Java3.og", "compilationUnit", text);
		Assert.assertNotNull(tree);
	}

	@Test
	public void java3_comment() throws FileNotFoundException, ParseFailedException, UnableToAnalyseExeception {
		final String text = "/* comment */";
		final ISharedPackedParseTree tree = this.parse("src/test/resources/Java3.og", "compilationUnit", text);
		Assert.assertNotNull(tree);
	}

	@Test
	public void java3_package() throws FileNotFoundException, ParseFailedException, UnableToAnalyseExeception {
		final String text = "package";
		final ISharedPackedParseTree tree = this.parse("src/test/resources/Java3.og", "compilationUnit", text);
		Assert.assertNotNull(tree);
	}

	@Test
	public void java3_comment_package() throws FileNotFoundException, ParseFailedException, UnableToAnalyseExeception {
		final String text = "/* comment */  package";
		final ISharedPackedParseTree tree = this.parse("src/test/resources/Java3.og", "compilationUnit", text);
		Assert.assertNotNull(tree);
	}

	@Test
	public void java3_package_comment() throws FileNotFoundException, ParseFailedException, UnableToAnalyseExeception {
		final String text = "package  /* comment */";
		final ISharedPackedParseTree tree = this.parse("src/test/resources/Java3.og", "compilationUnit", text);
		Assert.assertNotNull(tree);
	}

	@Test
	public void java4_type_int() throws FileNotFoundException, ParseFailedException, UnableToAnalyseExeception {
		final String text = "int";
		final ISharedPackedParseTree tree = this.parse("src/test/resources/Java4.og", "type", text);
		Assert.assertNotNull(tree);
	}

	@Test
	public void java4_type_Integer() throws FileNotFoundException, ParseFailedException, UnableToAnalyseExeception {
		final String text = "Integer";
		final ISharedPackedParseTree tree = this.parse("src/test/resources/Java4.og", "type", text);
		Assert.assertNotNull(tree);
	}

	@Test
	public void java6_switchBlock() throws FileNotFoundException, ParseFailedException, UnableToAnalyseExeception {
		final String text = "{ case 1 : 2; }";
		final ISharedPackedParseTree tree = this.parse("src/test/resources/Java6.og", "switchBlock", text);
		Assert.assertNotNull(tree);
	}

	@Test
	public void Java8_partial_7_postIncrementExpression() throws FileNotFoundException, ParseFailedException, UnableToAnalyseExeception {
		final String text = "i++";
		// final ISharedPackedParseTree tree = this.parse("src/test/resources/Java8_partial_7.og", "postIncrementExpression", text);
		final ISharedPackedParseTree tree = this.parse("src/test/resources/Java8_all.og", "postIncrementExpression", text);
		Assert.assertNotNull(tree);
	}

	@Test
	public void Java8_partial_8_block() throws FileNotFoundException, ParseFailedException, UnableToAnalyseExeception {
		final String text = "i++";
		final ISharedPackedParseTree tree = this.parse("src/test/resources/Java8_partial_8.og", "statementExpression", text);
		Assert.assertNotNull(tree);
	}

	@Test
	public void Java8_partial_9_block() throws FileNotFoundException, ParseFailedException, UnableToAnalyseExeception {
		String input = "";
		input += "{";
		input += "    if (s.equals(\"-no\")) {";
		input += "       throw new E(\"fie \" + 1 + \"\");";
		input += "    }";
		input += "}";
		final ISharedPackedParseTree tree = this.parse("src/test/resources/Java8_partial_9.og", "block", input);
		Assert.assertNotNull(tree);
	}

	@Test
	public void Java8_partial_9_block2() throws FileNotFoundException, ParseFailedException, UnableToAnalyseExeception {
		String input = "";
		input += "{";
		input += "    if (s.f(1)) {";
		input += "       throw new E(\"f\" + 1 + \"g\");";
		input += "    }";
		input += "}";
		final ISharedPackedParseTree tree = this.parse("src/test/resources/Java8_partial_9.og", "block", input);
		Assert.assertNotNull(tree);
	}
	// public void java7_formalParameters()

	@Test
	public void java5_4980495_static_Test() throws ParseFailedException, UnableToAnalyseExeception, IOException {

		final String text = new String(Files.readAllBytes(Paths.get("src/test/resources/Test.txt")));
		final ISharedPackedParseTree tree = this.parse("src/test/resources/Java5.og", "compilationUnit", text);
		Assert.assertNotNull(tree);

	}

	@Test
	public void java() {
		try {
			final OGLanguageProcessor proc = new OGLanguageProcessor();
			final IGrammar g = proc.getGrammar();

			final FileReader reader = new FileReader("src/test/resources/Java.og");
			final ISharedPackedParseTree tree = this.process(g, reader, "grammarDefinition");

			Assert.assertNotNull(tree);

		} catch (final ParseFailedException e) {
			Assert.fail(e.getMessage() + " matched length " + e.getLongestMatch().getRoot().getMatchedTextLength());
		} catch (final IOException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void java8_all() {
		try {
			final OGLanguageProcessor proc = new OGLanguageProcessor();
			final IGrammar g = proc.getGrammar();

			final FileReader reader = new FileReader("src/test/resources/Java8_all.og");
			final ISharedPackedParseTree tree = this.process(g, reader, "grammarDefinition");

			Assert.assertNotNull(tree);

		} catch (final ParseFailedException e) {
			Assert.fail(e.getMessage() + " matched length " + e.getLongestMatch().getRoot().getMatchedTextLength());
		} catch (final IOException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void java_parts() {
		try {
			final OGLanguageProcessor proc = new OGLanguageProcessor();
			final IGrammar g = proc.getGrammar();

			final FileReader reader = new FileReader("src/test/resources/Java8_part2.og");
			final ISharedPackedParseTree tree = this.process(g, reader, "grammarDefinition");

			Assert.assertNotNull(tree);

		} catch (final ParseFailedException e) {
			Assert.fail(e.getMessage() + " matched length " + e.getLongestMatch().getRoot().getMatchedTextLength());
		} catch (final IOException e) {
			Assert.fail(e.getMessage());
		}
	}

}
