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

import net.akehurst.language.core.analyser.UnableToAnalyseExeception;
import net.akehurst.language.core.parser.IBranch;
import net.akehurst.language.core.parser.INodeType;
import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.core.parser.IParser;
import net.akehurst.language.core.parser.ParseFailedException;
import net.akehurst.language.core.parser.ParseTreeException;
import net.akehurst.language.core.parser.RuleNotFoundException;
import net.akehurst.language.grammar.parser.ScannerLessParser2;
import net.akehurst.language.grammar.parser.ScannerLessParser3;
import net.akehurst.language.grammar.parser.ToStringVisitor;
import net.akehurst.language.grammar.parser.forrest.ForrestFactory;
import net.akehurst.language.grammar.parser.forrest.ParseTreeBuilder;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleSetBuilder;
import net.akehurst.language.ogl.semanticStructure.Grammar;
import net.akehurst.language.ogl.semanticStructure.GrammarBuilder;
import net.akehurst.language.ogl.semanticStructure.Namespace;
import net.akehurst.language.ogl.semanticStructure.NonTerminal;
import net.akehurst.language.ogl.semanticStructure.TerminalLiteral;
import net.akehurst.language.ogl.semanticStructure.TerminalPattern;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class OGL_Java_Test {
	RuntimeRuleSetBuilder parseTreeFactory;

	@Before
	public void before() {
		this.parseTreeFactory = new RuntimeRuleSetBuilder();
	}

	String readFile(String path, Charset encoding) throws IOException {
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, encoding);
	}

	ParseTreeBuilder builder(Grammar grammar, String text, String goal) {
		ForrestFactory ff = new ForrestFactory(this.parseTreeFactory, text);
		return new ParseTreeBuilder(ff, grammar, goal, text);
	}

	IParseTree process(Grammar grammar, String text, String goalName) throws ParseFailedException {
		try {
			INodeType goal = grammar.findRule(goalName).getNodeType();
			IParser parser = new ScannerLessParser3(this.parseTreeFactory, grammar);
			IParseTree tree = parser.parse(goal, text);
			return tree;
		} catch (RuleNotFoundException e) {
			Assert.fail(e.getMessage());
			return null;
		} catch (ParseTreeException e) {
			Assert.fail(e.getMessage());
			return null;
		}
	}

	IParseTree parse(String grammarFile, String goal, String text) {
		try {
			OGLanguageProcessor proc = new OGLanguageProcessor();
			String grammarText = this.readFile(grammarFile, Charset.defaultCharset());
			Grammar grammar = proc.process(grammarText, Grammar.class);

			IParseTree tree = this.process(grammar, text, goal);

			return tree;
		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage());
		} catch (IOException e) {
			Assert.fail(e.getMessage());
		} catch (UnableToAnalyseExeception e) {
			Assert.fail(e.getMessage());
		}
		return null;
	}
	
	@Test
	public void java1_empty() {
		IParseTree tree = this.parse("src/test/resources/Java1.og", "compilationUnit", "");
		Assert.assertNotNull(tree);
	}
	
	@Test
	public void java1_comment() {
		IParseTree tree = this.parse("src/test/resources/Java1.og", "compilationUnit", "/* comment */");
		Assert.assertNotNull(tree);
	}
	
	@Test
	public void java1_package() {
		IParseTree tree = this.parse("src/test/resources/Java1.og", "compilationUnit", "package");
		Assert.assertNotNull(tree);
	}
	
	@Test
	public void java1_comment_package() {
		String text = "/* comment */  package";
		IParseTree tree = this.parse("src/test/resources/Java1.og", "compilationUnit", text);
		Assert.assertNotNull(tree);
	}
	
	@Test
	public void java1_package_comment() {
		String text = "package  /* comment */";
		IParseTree tree = this.parse("src/test/resources/Java1.og", "compilationUnit", text);
		Assert.assertNotNull(tree);
	}
	
	@Test
	public void java2_empty() {
		String text = "";
		IParseTree tree = this.parse("src/test/resources/Java2.og", "compilationUnit", text);
		Assert.assertNotNull(tree);
	}
	
	@Test
	public void java2_comment() {
		String text = "/* comment */";
		IParseTree tree = this.parse("src/test/resources/Java2.og", "compilationUnit", text);
		Assert.assertNotNull(tree);
	}
	
	@Test
	public void java2_comment_package() {
		String text = "/* comment */  package";
		IParseTree tree = this.parse("src/test/resources/Java2.og", "compilationUnit", text);
		Assert.assertNotNull(tree);
	}
	
	@Test
	public void java2_package_comment() {
		String text = "package  /* comment */";
		IParseTree tree = this.parse("src/test/resources/Java2.og", "compilationUnit", text);
		Assert.assertNotNull(tree);
	}
	
	@Test
	public void java2_import() {
		String text = "import";
		IParseTree tree = this.parse("src/test/resources/Java2.og", "compilationUnit", text);
		Assert.assertNotNull(tree);
	}
	
	@Test
	public void java2_type() {
		String text = "type";
		IParseTree tree = this.parse("src/test/resources/Java2.og", "compilationUnit", text);
		Assert.assertNotNull(tree);
	}
	
	@Test
	public void java3_empty() {
		String text = "";
		IParseTree tree = this.parse("src/test/resources/Java3.og", "compilationUnit", text);
		Assert.assertNotNull(tree);
	}
	
	@Test
	public void java3_comment() {
		String text = "/* comment */";
		IParseTree tree = this.parse("src/test/resources/Java3.og", "compilationUnit", text);
		Assert.assertNotNull(tree);
	}
	
	@Test
	public void java3_package() {
		String text = "package";
		IParseTree tree = this.parse("src/test/resources/Java3.og", "compilationUnit", text);
		Assert.assertNotNull(tree);
	}
	
	@Test
	public void java3_comment_package() {
		String text = "/* comment */  package";
		IParseTree tree = this.parse("src/test/resources/Java3.og", "compilationUnit", text);
		Assert.assertNotNull(tree);
	}
	
	@Test
	public void java3_package_comment() {
		String text = "package  /* comment */";
		IParseTree tree = this.parse("src/test/resources/Java3.og", "compilationUnit", text);
		Assert.assertNotNull(tree);
	}
	
	@Test
	public void java4_type_int() {
		String text = "int";
		IParseTree tree = this.parse("src/test/resources/Java4.og", "type", text);
		Assert.assertNotNull(tree);
	}
	
	@Test
	public void java4_type_Integer() {
		String text = "Integer";
		IParseTree tree = this.parse("src/test/resources/Java4.og", "type", text);
		Assert.assertNotNull(tree);
	}
	
	@Test
	public void java() {
		try {
			OGLanguageProcessor proc = new OGLanguageProcessor();
			Grammar g = proc.getGrammar();

			String text = this.readFile("src/test/resources/Java.og", Charset.defaultCharset());

			IParseTree tree = this.process(g, text, "grammarDefinition");

			Assert.assertNotNull(tree);

		} catch (ParseFailedException e) {
			Assert.fail(e.getMessage() + " matched length "+ e.getLongestMatch().getRoot().getMatchedTextLength());
		} catch (IOException e) {
			Assert.fail(e.getMessage());
		}
	}
}
