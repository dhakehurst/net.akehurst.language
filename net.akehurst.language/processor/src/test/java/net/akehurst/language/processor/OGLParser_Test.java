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

import java.io.StringReader;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import net.akehurst.language.api.grammar.Grammar;
import net.akehurst.language.api.grammar.GrammarRuleNotFoundException;
import net.akehurst.language.api.parser.ParseFailedException;
import net.akehurst.language.api.parser.ParseTreeException;
import net.akehurst.language.api.parser.Parser;
import net.akehurst.language.api.sppt.SPPTBranch;
import net.akehurst.language.api.sppt.SharedPackedParseTree;
import net.akehurst.language.grammar.parser.ScannerLessParser3;
import net.akehurst.language.grammar.parser.forrest.ParseTreeBuilder;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleSetBuilder;
import net.akehurst.language.ogl.semanticStructure.GrammarDefault;
import net.akehurst.language.ogl.semanticStructure.GrammarBuilderDefault;
import net.akehurst.language.ogl.semanticStructure.NamespaceDefault;
import net.akehurst.language.ogl.semanticStructure.NonTerminalDefault;
import net.akehurst.language.ogl.semanticStructure.TerminalLiteralDefault;
import net.akehurst.language.ogl.semanticStructure.TerminalPatternDefault;

public class OGLParser_Test {
	RuntimeRuleSetBuilder parseTreeFactory;

	@Before
	public void before() {
		this.parseTreeFactory = new RuntimeRuleSetBuilder();
	}

	ParseTreeBuilder builder(final GrammarDefault grammar, final String text, final String goal) {
		return new ParseTreeBuilder(this.parseTreeFactory, grammar, goal, text, 0);
	}

	SharedPackedParseTree process(final Grammar grammar, final String text, final String goalName) throws ParseFailedException {
		try {
			final Parser parser = new ScannerLessParser3(this.parseTreeFactory, grammar);
			final SharedPackedParseTree tree = parser.parse(goalName, new StringReader(text));
			return tree;
		} catch (final GrammarRuleNotFoundException e) {
			Assert.fail(e.getMessage());
			return null;
		} catch (final ParseTreeException e) {
			Assert.fail(e.getMessage());
			return null;
		}
	}

	GrammarDefault ns1() {
		final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("net::akehurst::language::ogl::grammar"), "OGL");
		b.skip("WHITESPACE").concatenation(new TerminalPatternDefault("\\s+"));
		b.skip("COMMENT").concatenation(new TerminalPatternDefault("(?s)/\\*.*?\\*/"));

		b.rule("grammarDefinition").concatenation(new NonTerminalDefault("namespace"));
		b.rule("namespace").concatenation(new TerminalLiteralDefault("namespace"), new NonTerminalDefault("IDENTIFIER"), new TerminalLiteralDefault(";"));

		b.rule("IDENTIFIER").concatenation(new TerminalPatternDefault("[a-zA-Z_][a-zA-Z_0-9]*"));

		return b.get();
	}

	@Test
	public void ns1_namespace_test() {
		try {
			final GrammarDefault g = this.ns1();
			final String goal = "grammarDefinition";
			final String text = "namespace test;" + System.lineSeparator();

			final SharedPackedParseTree tree = this.process(g, text, goal);

			Assert.assertNotNull(tree);

			final ParseTreeBuilder b = this.builder(g, text, goal);
			final SPPTBranch expected = b.branch("grammarDefinition", b.branch("namespace", b.leaf("namespace"), b.branch("WHITESPACE", b.leaf("\\s+", " ")),
					b.branch("IDENTIFIER", b.leaf("[a-zA-Z_][a-zA-Z_0-9]*", "test")), b.leaf(";", ";")));
			Assert.assertEquals(expected, tree.getRoot());

		} catch (final ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	GrammarDefault g1() {
		final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("net::akehurst::language::ogl::grammar"), "OGL");
		b.skip("WHITESPACE").concatenation(new TerminalPatternDefault("\\s+"));
		b.skip("COMMENT").concatenation(new TerminalPatternDefault("(?s)/\\*.*?\\*/"));

		b.rule("grammarDefinition").concatenation(new NonTerminalDefault("namespace"), new NonTerminalDefault("grammar"));
		b.rule("namespace").concatenation(new TerminalLiteralDefault("namespace"), new NonTerminalDefault("IDENTIFIER"), new TerminalLiteralDefault(";"));
		b.rule("grammar").concatenation(new TerminalLiteralDefault("grammar"), new NonTerminalDefault("IDENTIFIER"), new TerminalLiteralDefault("{"), new TerminalLiteralDefault("}"));

		b.rule("IDENTIFIER").concatenation(new TerminalPatternDefault("[a-zA-Z_][a-zA-Z_0-9]*"));

		return b.get();
	}

	@Test
	public void g1_noRules() {
		try {
			final Grammar g = this.g1();
			String text = "namespace test;" + System.lineSeparator();
			text += "grammar A {" + System.lineSeparator();
			text += "}";

			final SharedPackedParseTree tree = this.process(g, text, "grammarDefinition");

			Assert.assertNotNull(tree);

		} catch (final ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void g1_emptyRule() {
		try {
			final OGLanguageProcessor proc = new OGLanguageProcessor();
			final Grammar g = proc.getGrammar();

			String text = "namespace test;" + System.lineSeparator();
			text += "grammar A {" + System.lineSeparator();
			text += " a :  ;" + System.lineSeparator();
			text += "}";

			final SharedPackedParseTree tree = this.process(g, text, "grammarDefinition");

			Assert.assertNotNull(tree);

		} catch (final ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	GrammarDefault qualifiedName() {
		final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("net::akehurst::language::ogl::grammar"), "OGL");
		b.skip("WHITESPACE").concatenation(new TerminalPatternDefault("\\s+"));

		b.rule("qualifiedName").separatedList(1, -1, new TerminalLiteralDefault("::"), new NonTerminalDefault("IDENTIFIER"));

		b.rule("IDENTIFIER").concatenation(new TerminalPatternDefault("[a-zA-Z_][a-zA-Z_0-9]*"));

		return b.get();
	}

	@Test
	public void qualifiedName_a() {
		try {
			final GrammarDefault g = this.qualifiedName();
			final String goal = "qualifiedName";
			final String text = "a";

			final SharedPackedParseTree tree = this.process(g, text, goal);

			Assert.assertNotNull(tree);

		} catch (final ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void qualifiedName__a_b() {
		try {
			final GrammarDefault g = this.qualifiedName();
			final String goal = "qualifiedName";
			final String text = "a::b";

			final SharedPackedParseTree tree = this.process(g, text, goal);

			Assert.assertNotNull(tree);

		} catch (final ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void qualifiedName__a_b_c_d() {
		try {
			final GrammarDefault g = this.qualifiedName();
			final String goal = "qualifiedName";
			final String text = "a::b::c::d";

			final SharedPackedParseTree tree = this.process(g, text, goal);

			Assert.assertNotNull(tree);

		} catch (final ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	GrammarDefault noRules() {
		final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("net::akehurst::language::ogl::grammar"), "OGL");
		b.skip("WHITESPACE").concatenation(new TerminalPatternDefault("\\s+"));
		b.skip("COMMENT").concatenation(new TerminalPatternDefault("(?s)/\\*.*?\\*/"));

		b.rule("grammarDefinition").concatenation(new NonTerminalDefault("namespace"), new NonTerminalDefault("grammar"));
		b.rule("namespace").concatenation(new TerminalLiteralDefault("namespace"), new NonTerminalDefault("qualifiedName"), new TerminalLiteralDefault(";"));
		b.rule("grammar").concatenation(new TerminalLiteralDefault("grammar"), new NonTerminalDefault("IDENTIFIER"), new TerminalLiteralDefault("{"), new TerminalLiteralDefault("}"));

		b.rule("qualifiedName").separatedList(1, -1, new TerminalLiteralDefault("::"), new NonTerminalDefault("IDENTIFIER"));

		b.rule("IDENTIFIER").concatenation(new TerminalPatternDefault("[a-zA-Z_][a-zA-Z_0-9]*"));

		return b.get();
	}

	@Test
	public void noRules_1() {
		try {
			final GrammarDefault g = this.noRules();
			final String goal = "grammarDefinition";
			String text = "namespace test::ns::nss;" + System.lineSeparator();
			text += "grammar A {" + System.lineSeparator();
			text += "}";

			final SharedPackedParseTree tree = this.process(g, text, goal);

			Assert.assertNotNull(tree);

		} catch (final ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	GrammarDefault nonTerminalOnly() {
		final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("net::akehurst::language::ogl::grammar"), "OGL");
		b.skip("WHITESPACE").concatenation(new TerminalPatternDefault("\\s+"));
		b.skip("COMMENT").concatenation(new TerminalPatternDefault("(?s)/\\*.*?\\*/"));

		b.rule("grammarDefinition").concatenation(new NonTerminalDefault("namespace"), new NonTerminalDefault("grammar"));
		b.rule("namespace").concatenation(new TerminalLiteralDefault("namespace"), new NonTerminalDefault("qualifiedName"), new TerminalLiteralDefault(";"));
		b.rule("grammar").concatenation(new TerminalLiteralDefault("grammar"), new NonTerminalDefault("IDENTIFIER"), new TerminalLiteralDefault("{"), new NonTerminalDefault("rules"),
				new TerminalLiteralDefault("}"));
		b.rule("rules").multi(1, -1, new NonTerminalDefault("rule"));
		b.rule("rule").concatenation(new NonTerminalDefault("IDENTIFIER"), new TerminalLiteralDefault("="), new NonTerminalDefault("nonTerminal"), new TerminalLiteralDefault(";"));
		b.rule("nonTerminal").choice(new NonTerminalDefault("IDENTIFIER"));

		b.rule("qualifiedName").separatedList(1, -1, new TerminalLiteralDefault("::"), new NonTerminalDefault("IDENTIFIER"));

		b.rule("IDENTIFIER").concatenation(new TerminalPatternDefault("[a-zA-Z_][a-zA-Z_0-9]*"));

		return b.get();
	}

	@Test
	public void nonTerminalOnly_1() throws ParseFailedException {

		final Grammar g = this.nonTerminalOnly();
		final String goal = "grammarDefinition";
		String text = "namespace test;" + System.lineSeparator();
		text += "grammar A {" + System.lineSeparator();
		text += "a = b ;";
		text += "}";

		final SharedPackedParseTree tree = this.process(g, text, goal);

		Assert.assertNotNull(tree);

	}

	@Test
	public void a() throws ParseFailedException {

		final OGLanguageProcessor proc = new OGLanguageProcessor();
		final Grammar g = proc.getGrammar();

		String text = "namespace test;" + System.lineSeparator();
		text += "grammar A {" + System.lineSeparator();
		text += " skip SP : ' ' ;" + System.lineSeparator();
		text += " a : 'a' ;" + System.lineSeparator();
		text += "}";

		final SharedPackedParseTree tree = this.process(g, text, "grammarDefinition");

		Assert.assertNotNull(tree);

	}

	@Test
	public void a2() throws ParseFailedException {
		final OGLanguageProcessor proc = new OGLanguageProcessor();
		final Grammar g = proc.getGrammar();

		final String text = "namespace test; grammar A { skip SP : ' ' ; a : 'a' ; }";

		final SharedPackedParseTree tree = this.process(g, text, "grammarDefinition");

		Assert.assertNotNull(tree);

	}

	@Test
	public void separatedList() throws ParseFailedException {

		final OGLanguageProcessor proc = new OGLanguageProcessor();
		final Grammar g = proc.getGrammar();

		final String text = "['a' / ',']+";

		final SharedPackedParseTree tree = this.process(g, text, "separatedList");

		Assert.assertNotNull(tree);

	}

	@Test
	public void separatedList_normalRule() {
		try {
			final OGLanguageProcessor proc = new OGLanguageProcessor();
			final Grammar g = proc.getGrammar();

			final String text = "sepList:['a'/',']+;";

			final SharedPackedParseTree tree = this.process(g, text, "normalRule");

			Assert.assertNotNull(tree);

		} catch (final ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void separatedList_normalRule_withWS() {
		try {
			final OGLanguageProcessor proc = new OGLanguageProcessor();
			final Grammar g = proc.getGrammar();

			final String text = "sepList : ['a' / ',']+;";

			final SharedPackedParseTree tree = this.process(g, text, "normalRule");

			Assert.assertNotNull(tree);

		} catch (final ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void separatedList_rules() {
		try {
			final OGLanguageProcessor proc = new OGLanguageProcessor();
			final Grammar g = proc.getGrammar();

			final String text = "sepList:['a'/',']+;";

			final SharedPackedParseTree tree = this.process(g, text, "rules");

			Assert.assertNotNull(tree);

		} catch (final ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void separatedList_rules_withWS() {
		try {
			final OGLanguageProcessor proc = new OGLanguageProcessor();
			final Grammar g = proc.getGrammar();

			final String text = "sepList : ['a' / ',']+;";

			final SharedPackedParseTree tree = this.process(g, text, "rules");

			Assert.assertNotNull(tree);
			Assert.assertEquals(text.length(), tree.getRoot().getMatchedTextLength());

		} catch (final ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void separatedList_grammar() {
		try {
			final OGLanguageProcessor proc = new OGLanguageProcessor();
			final Grammar g = proc.getGrammar();

			final String text = "grammarA{sepList:['a'/',']+;}";

			final SharedPackedParseTree tree = this.process(g, text, "grammar");

			Assert.assertNotNull(tree);
			Assert.assertEquals(text.length(), tree.getRoot().getMatchedTextLength());

		} catch (final ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void separatedList_grammar_withWS() {
		try {
			final OGLanguageProcessor proc = new OGLanguageProcessor();
			final Grammar g = proc.getGrammar();

			final String text = "grammar A { sepList : ['a' / ',']+; }";

			final SharedPackedParseTree tree = this.process(g, text, "grammar");

			Assert.assertNotNull(tree);

		} catch (final ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void separatedList_1n() {
		try {
			final OGLanguageProcessor proc = new OGLanguageProcessor();
			final Grammar g = proc.getGrammar();

			final String text = "namespace test; grammar A { sepList : ['a' / ',']+; }";

			final SharedPackedParseTree tree = this.process(g, text, "grammarDefinition");

			Assert.assertNotNull(tree);

		} catch (final ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void separatedList_0n() {
		try {
			final OGLanguageProcessor proc = new OGLanguageProcessor();
			final Grammar g = proc.getGrammar();

			final String text = "namespace test; grammar A { sepList : ['a' / ',']*; }";

			final SharedPackedParseTree tree = this.process(g, text, "grammarDefinition");

			Assert.assertNotNull(tree);

		} catch (final ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void multiLineComment1() {
		// this fails because skip rules can't be a goal at present!
		try {
			final OGLanguageProcessor proc = new OGLanguageProcessor();
			final Grammar g = proc.getGrammar();

			final String text = "/* this is a comment */";

			final SharedPackedParseTree tree = this.process(g, text, "MULTI_LINE_COMMENT");

			Assert.assertNotNull(tree);

		} catch (final ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void singleLineComment() {
		// this fails because skip rules can't be a goal at present!
		try {
			final OGLanguageProcessor proc = new OGLanguageProcessor();
			final Grammar g = proc.getGrammar();

			final String text = "// this is a comment" + System.lineSeparator();

			final SharedPackedParseTree tree = this.process(g, text, "SINGLE_LINE_COMMENT");

			Assert.assertNotNull(tree);

		} catch (final ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void normalRule_1() {
		try {
			final OGLanguageProcessor proc = new OGLanguageProcessor();
			final Grammar g = proc.getGrammar();
			final String text = "classType:'.'annotation*;";

			final SharedPackedParseTree tree = this.process(g, text, "normalRule");

		} catch (final ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void normalRule_2() {
		try {
			final OGLanguageProcessor proc = new OGLanguageProcessor();
			final Grammar g = proc.getGrammar();
			final String text = "classType :	annotation* Identifier typeArguments? |	classOrInterfaceType '.' annotation* Identifier typeArguments?;";

			final SharedPackedParseTree tree = this.process(g, text, "normalRule");

		} catch (final ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void choice() {
		try {
			final OGLanguageProcessor proc = new OGLanguageProcessor();
			final Grammar g = proc.getGrammar();
			final String text = "type : primitiveType | referenceType ;";

			final SharedPackedParseTree tree = this.process(g, text, "normalRule");

		} catch (final ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void priorityChoice() {
		try {
			final OGLanguageProcessor proc = new OGLanguageProcessor();
			final Grammar g = proc.getGrammar();
			final String text = "type : primitiveType < referenceType ;";

			final SharedPackedParseTree tree = this.process(g, text, "normalRule");

		} catch (final ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void java8_part1() {

	}

}
