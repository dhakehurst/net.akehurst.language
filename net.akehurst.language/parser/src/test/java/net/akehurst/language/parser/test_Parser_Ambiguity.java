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
import net.akehurst.language.ogl.semanticStructure.TerminalPattern;

public class test_Parser_Ambiguity extends AbstractParser_Test {

	Grammar am() {
		final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("S").multi(0, -1, new TerminalLiteral("a"));
		return b.get();
	}

	Grammar aq() {
		final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("S").multi(0, 1, new TerminalLiteral("a"));
		return b.get();
	}

	Grammar aab() {
		final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("S").choice(new TerminalLiteral("a"), new NonTerminal("ab"));
		b.rule("ab").concatenation(new TerminalLiteral("a"), new TerminalLiteral("b"));
		return b.get();
	}

	Grammar ae() {
		final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("S").choice(new TerminalLiteral("a"), new NonTerminal("nothing"));
		b.rule("nothing").choice();
		return b.get();
	}

	Grammar amq() {
		final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("S").multi(0, 1, new NonTerminal("am"));
		b.rule("am").multi(0, -1, new TerminalLiteral("a"));
		return b.get();
	}

	Grammar x() {
		final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("S").multi(0, 1, new NonTerminal("aaa"));
		b.rule("aaa").choice(new NonTerminal("a1"), new NonTerminal("a2"), new NonTerminal("a3"));
		b.rule("a1").multi(0, 1, new TerminalLiteral("a"));
		b.rule("a2").multi(0, 2, new TerminalLiteral("a"));
		b.rule("a3").multi(0, 3, new TerminalLiteral("a"));
		return b.get();
	}

	Grammar tg() {
		final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.skip("WS").concatenation(new TerminalPattern("\\s+"));
		b.rule("fps").choice(new NonTerminal("fps.choice1"), new NonTerminal("fps.choice2"));
		b.rule("fps.choice1").concatenation(new NonTerminal("fp"), new NonTerminal("fps.choice1.group.multi"));
		b.rule("fps.choice1.group.multi").multi(0, -1, new NonTerminal("fps.choice1.group"));
		b.rule("fps.choice1.group").concatenation(new TerminalLiteral(","), new NonTerminal("fp"));
		b.rule("fps.choice2").concatenation(new NonTerminal("rp"), new NonTerminal("fps.choice1.group.multi"));
		b.rule("fp").concatenation(new NonTerminal("t"), new NonTerminal("name"));
		b.rule("rp").concatenation(new NonTerminal("name"), new NonTerminal("rp.multi"), new TerminalLiteral("this"));
		b.rule("rp.multi").multi(0, 1, new NonTerminal("rp.multi.group"));
		b.rule("rp.multi.group").concatenation(new NonTerminal("name"), new TerminalLiteral("."));
		b.rule("t").choice(new NonTerminal("bt"), new NonTerminal("gt"));
		b.rule("bt").concatenation(new NonTerminal("name"));
		b.rule("gt").concatenation(new NonTerminal("name"), new TerminalLiteral("("), new NonTerminal("name"), new TerminalLiteral(")"));
		b.rule("name").choice(new TerminalPattern("[a-zA-Z]+"));
		return b.get();
	}

	@Test
	public void am_S_empty() throws ParseFailedException {
		// grammar, goal, input

		final Grammar g = this.am();
		final String goal = "S";
		final String text = "";

		final IParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		final IParseTree expected = new ParseTree(b.branch("S", b.emptyLeaf("S")));
		Assert.assertEquals(expected, tree);

	}

	@Test
	public void am_S_a() throws ParseFailedException {
		// grammar, goal, input

		final Grammar g = this.am();
		final String goal = "S";
		final String text = "a";

		final IParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		// b.define("S { 'a' }");
		final IParseTree expected = new ParseTree(b.branch("S", b.leaf("a")));
		// final IBranch expected = b.branch("S", b.leaf("a"));
		Assert.assertEquals(expected, tree);

	}

	@Test
	public void am_S_aa() throws ParseFailedException {
		// grammar, goal, input

		final Grammar g = this.am();
		final String goal = "S";
		final String text = "aa";

		final IParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		final IBranch expected = b.branch("S", b.leaf("a"), b.leaf("a"));
		Assert.assertEquals(expected, tree.getRoot());

	}

	@Test
	public void x_S_a() throws ParseFailedException {
		// grammar, goal, input
		final Grammar g = this.x();
		final String goal = "S";
		final String text = "a";

		final IParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		final IParseTree expected = new ParseTree(b.branch("S", b.branch("aaa", b.branch("a1", b.leaf("a")))));
		Assert.assertEquals(expected, tree);
	}

	@Test
	public void x_S_aa() throws ParseFailedException {
		// grammar, goal, input
		final Grammar g = this.x();
		final String goal = "S";
		final String text = "aa";

		final IParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		final IParseTree expected = new ParseTree(b.branch("S", b.branch("aaa", b.branch("a2", b.leaf("a"), b.leaf("a")))));
		Assert.assertEquals(expected, tree);
	}

	@Test
	public void x_S_aaa() throws ParseFailedException {
		// grammar, goal, input
		final Grammar g = this.x();
		final String goal = "S";
		final String text = "aaa";

		final IParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		final IParseTree expected = new ParseTree(b.branch("S", b.branch("aaa", b.branch("a3", b.leaf("a"), b.leaf("a"), b.leaf("a")))));
		Assert.assertEquals(expected, tree);
	}

	@Test
	public void tg_fp_V() throws ParseFailedException {
		// grammar, goal, input

		final Grammar g = this.tg();
		final String goal = "fp";
		final String text = "V v";

		final IParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		final IBranch expected = b.branch("fp", b.branch("t", b.branch("bt", b.branch("name", b.leaf("[a-zA-Z]+", "V"), b.branch("WS", b.leaf("\\s+", " "))))),
				b.branch("name", b.leaf("[a-zA-Z]+", "v")));
		Assert.assertEquals(expected, tree.getRoot());

	}

	@Test
	public void tg_fp_VE() throws ParseFailedException {
		// grammar, goal, input

		final Grammar g = this.tg();
		final String goal = "fp";
		final String text = "V(E) v";

		final IParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		final IBranch expected = b.branch("fp", b.branch("t", b.branch("gt", b.branch("name", b.leaf("[a-zA-Z]+", "V")), b.leaf("("),
				b.branch("name", b.leaf("[a-zA-Z]+", "E")), b.leaf(")"), b.branch("WS", b.leaf("\\s+", " ")))), b.branch("name", b.leaf("[a-zA-Z]+", "v")));
		Assert.assertEquals(expected, tree.getRoot());

	}

	@Test
	public void tg_fps_choice1_VE() throws ParseFailedException {
		// grammar, goal, input

		final Grammar g = this.tg();
		final String goal = "fps.choice1";
		final String text = "V(E) v";

		final IParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		final IBranch expected = b.branch("fps.choice1",
				b.branch("fp",
						b.branch("t",
								b.branch("gt", b.branch("name", b.leaf("[a-zA-Z]+", "V")), b.leaf("("), b.branch("name", b.leaf("[a-zA-Z]+", "E")), b.leaf(")"),
										b.branch("WS", b.leaf("\\s+", " ")))),
						b.branch("name", b.leaf("[a-zA-Z]+", "v"))),
				b.branch("fps.choice1.group.multi", b.emptyLeaf("fps.choice1.group.multi")));
		Assert.assertEquals(expected, tree.getRoot());

	}

	@Test
	public void tg_fps_VE() throws ParseFailedException {
		// grammar, goal, input

		final Grammar g = this.tg();
		final String goal = "fps";
		final String text = "V(E) v";

		final IParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		final IParseTree expected = new ParseTree(b.branch("fps",
				b.branch("fps.choice1",
						b.branch("fp",
								b.branch("t",
										b.branch("gt", b.branch("name", b.leaf("[a-zA-Z]+", "V")), b.leaf("("), b.branch("name", b.leaf("[a-zA-Z]+", "E")),
												b.leaf(")"), b.branch("WS", b.leaf("\\s+", " ")))),
								b.branch("name", b.leaf("[a-zA-Z]+", "v"))),
						b.branch("fps.choice1.group.multi", b.emptyLeaf("fps.choice1.group.multi")))));
		Assert.assertEquals(expected, tree);

	}

	@Test
	public void tg_fps_V_this() throws ParseFailedException {
		// grammar, goal, input

		final Grammar g = this.tg();
		final String goal = "fps";
		final String text = "V A.this";

		final IParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		final IBranch expected = b.branch("fps",
				b.branch("fps.choice2",
						b.branch("rp", b.branch("name", b.leaf("[a-zA-Z]+", "V"), b.branch("WS", b.leaf("\\s+", " "))),
								b.branch("rp.multi", b.branch("rp.multi.group", b.branch("name", b.leaf("[a-zA-Z]+", "A")), b.leaf("."))), b.leaf("this")),
						b.branch("fps.choice1.group.multi", b.emptyLeaf("fps.choice1.group.multi"))));
		Assert.assertEquals(expected, tree.getRoot());

	}

	Grammar caseBlock() {
		final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.skip("WS").concatenation(new TerminalPattern("\\s+"));
		b.rule("block").concatenation(new TerminalLiteral("{"), new NonTerminal("group1"), new NonTerminal("group2"), new TerminalLiteral("}"));
		b.rule("group1").multi(0, -1, new NonTerminal("labelBlock"));
		b.rule("group2").multi(0, -1, new NonTerminal("label"));
		b.rule("labelBlock").concatenation(new NonTerminal("labels"), new TerminalLiteral("{"), new TerminalLiteral("}"));
		b.rule("labels").multi(0, -1, new NonTerminal("label"));
		b.rule("label").concatenation(new TerminalLiteral("case"), new NonTerminal("int"), new TerminalLiteral(":"));
		b.rule("int").choice(new TerminalPattern("[0-9]+"));
		return b.get();
	}

	@Test
	public void ambiguity_int() throws ParseFailedException {
		// grammar, goal, input

		final Grammar g = this.caseBlock();
		final String goal = "int";
		final String text = "1";

		final IParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		final IBranch expected = b.branch("int", b.leaf("[0-9]+", "1"));
		Assert.assertEquals(expected, tree.getRoot());

	}

	@Test
	public void ambiguity_label() throws ParseFailedException {
		// grammar, goal, input

		final Grammar g = this.caseBlock();
		final String goal = "label";
		final String text = "case 1 :";

		final IParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		final IParseTree expected = new ParseTree(b.branch("label", b.leaf("case"), b.branch("WS", b.leaf("\\s+", " ")),
				b.branch("int", b.leaf("[0-9]+", "1"), b.branch("WS", b.leaf("\\s+", " "))), b.leaf(":")));
		Assert.assertEquals(expected, tree);

	}

	@Test
	public void ambiguity_labels() throws ParseFailedException {
		// grammar, goal, input

		final Grammar g = this.caseBlock();
		final String goal = "labels";
		final String text = "case 1 :";

		final IParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		final IParseTree expected = new ParseTree(b.branch("labels", b.branch("label", b.leaf("case"), b.branch("WS", b.leaf("\\s+", " ")),
				b.branch("int", b.leaf("[0-9]+", "1"), b.branch("WS", b.leaf("\\s+", " "))), b.leaf(":"))));
		Assert.assertEquals(expected, tree);

	}

	@Test
	public void ambiguity_labelBlock() throws ParseFailedException {
		// grammar, goal, input

		final Grammar g = this.caseBlock();
		final String goal = "labelBlock";
		final String text = "case 1 : { }";

		final IParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		final IBranch expected = b.branch("labelBlock",
				b.branch("labels",
						b.branch("label", b.leaf("case"), b.branch("WS", b.leaf("\\s+", " ")),
								b.branch("int", b.leaf("[0-9]+", "1"), b.branch("WS", b.leaf("\\s+", " "))), b.leaf(":"), b.branch("WS", b.leaf("\\s+", " ")))),
				b.leaf("{"), b.branch("WS", b.leaf("\\s+", " ")), b.leaf("}"));
		Assert.assertEquals(expected, tree.getRoot());

	}

	@Test
	public void ambiguity_block() throws ParseFailedException {
		// grammar, goal, input

		final Grammar g = this.caseBlock();
		final String goal = "block";
		final String text = "{ case 1 : { } }";

		final IParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		final IBranch expected = b.branch("block", b.leaf("{"), b.branch("WS", b.leaf("\\s+", " ")),
				b.branch("group1",
						b.branch("labelBlock", b.branch("labels", b.branch("label", b.leaf("case"), b.branch("WS", b.leaf("\\s+", " ")),
								b.branch("int", b.leaf("[0-9]+", "1"), b.branch("WS", b.leaf("\\s+", " "))), b.leaf(":"), b.branch("WS", b.leaf("\\s+", " ")))),
								b.leaf("{"), b.branch("WS", b.leaf("\\s+", " ")), b.leaf("}"), b.branch("WS", b.leaf("\\s+", " ")))),
				b.branch("group2", b.emptyLeaf("group2")), b.leaf("}"));
		Assert.assertEquals(expected, tree.getRoot());

	}

	@Test
	public void ambiguity_block2() throws ParseFailedException {
		// grammar, goal, input

		final Grammar g = this.caseBlock();
		final String goal = "block";
		final String text = "{ case 1 : }";

		final IParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		final IParseTree expected = new ParseTree(b.branch("block", b.leaf("{"), b.branch("WS", b.leaf("\\s+", " ")), b.branch("group1", b.emptyLeaf("group1")),
				b.branch("group2",
						b.branch("label", b.leaf("case"), b.branch("WS", b.leaf("\\s+", " ")),
								b.branch("int", b.leaf("[0-9]+", "1"), b.branch("WS", b.leaf("\\s+", " "))), b.leaf(":"), b.branch("WS", b.leaf("\\s+", " ")))),
				b.leaf("}")));
		Assert.assertEquals(expected, tree);

	}

	Grammar varDeclBlock() {
		final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.skip("WS").concatenation(new TerminalPattern("\\s+"));
		b.rule("block").concatenation(new TerminalLiteral("{"), new NonTerminal("decls"), new TerminalLiteral("}"));
		b.rule("decls").multi(0, -1, new NonTerminal("decl"));
		b.rule("decl").concatenation(new NonTerminal("type"), new NonTerminal("name"), new TerminalLiteral(";"));
		b.rule("type").priorityChoice(new NonTerminal("name"), new TerminalLiteral("int"));
		b.rule("name").choice(new TerminalPattern("[a-zA-Z0-9]+"));
		return b.get();
	}

	@Test
	public void varDeclBlock_block_empty() {
		// grammar, goal, input
		try {
			final Grammar g = this.varDeclBlock();
			final String goal = "block";
			final String text = "{}";

			final IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);

			final ParseTreeBuilder b = this.builder(g, text, goal);
			final IBranch expected = b.branch("block", b.leaf("{"), b.branch("decls", b.emptyLeaf("decls")), b.leaf("}"));
			Assert.assertEquals(expected, tree.getRoot());

		} catch (final ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void varDeclBlock_block_1() {
		// grammar, goal, input
		try {
			final Grammar g = this.varDeclBlock();
			final String goal = "block";
			final String text = "{ int i; }";

			final IParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);

			final ParseTreeBuilder b = this.builder(g, text, goal);
			final IParseTree expected = new ParseTree(
					b.branch(
							"block", b.leaf("{"), b.branch("WS", b.leaf("\\s+", " ")), b
									.branch("decls",
											b.branch("decl", b.branch("type", b.leaf("int"), b.branch("WS", b.leaf("\\s+", " "))),
													b.branch("name", b.leaf("[a-zA-Z0-9]+", "i")), b.leaf(";"), b.branch("WS", b.leaf("\\s+", " ")))),
							b.leaf("}")));
			Assert.assertEquals(expected, tree);

		} catch (final ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void varDeclBlock_block_2() throws ParseFailedException {
		// grammar, goal, input

		final Grammar g = this.varDeclBlock();
		final String goal = "block";
		final String text = "{int i1;int i2;}";

		final IParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		b.define("block {");
		b.define("  '{'");
		b.define("  decls {");
		b.define("    decl {");
		b.define("      type { 'int' WS { '\\s+':' '} }");
		b.define("      name { '[a-zA-Z0-9]+':'i1' }");
		b.define("      ';'");
		b.define("    }");
		b.define("    decl {");
		b.define("      type { 'int' WS { '\\s+':' '} }");
		b.define("      name { '[a-zA-Z0-9]+':'i2' }");
		b.define("      ';'");
		b.define("    }");
		b.define("  }");
		b.define("  '}'");
		b.define("}");
		final IParseTree expected = b.build();
		Assert.assertEquals(expected, tree);

	}

	@Test
	public void varDeclBlock_block_8() throws ParseFailedException {
		// grammar, goal, input

		final Grammar g = this.varDeclBlock();
		final String goal = "block";
		final String text = "{ int i1; int i2; int i3; int i4; int i5; int i6; int i7; int i8; }";

		final IParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		final IBranch expected = b.branch("block", b.leaf("{"), b.branch("WS", b.leaf("\\s+", " ")),
				b.branch("decls",
						b.branch("decl", b.branch("type", b.leaf("int"), b.branch("WS", b.leaf("\\s+", " "))), b.branch("name", b.leaf("[a-zA-Z0-9]+", "i1")),
								b.leaf(";"), b.branch("WS", b.leaf("\\s+", " "))),
						b.branch("decl", b.branch("type", b.leaf("int"), b.branch("WS", b.leaf("\\s+", " "))), b.branch("name", b.leaf("[a-zA-Z0-9]+", "i2")),
								b.leaf(";"), b.branch("WS", b.leaf("\\s+", " "))),
						b.branch("decl", b.branch("type", b.leaf("int"), b.branch("WS", b.leaf("\\s+", " "))), b.branch("name", b.leaf("[a-zA-Z0-9]+", "i3")),
								b.leaf(";"), b.branch("WS", b.leaf("\\s+", " "))),
						b.branch("decl", b.branch("type", b.leaf("int"), b.branch("WS", b.leaf("\\s+", " "))), b.branch("name", b.leaf("[a-zA-Z0-9]+", "i4")),
								b.leaf(";"), b.branch("WS", b.leaf("\\s+", " "))),
						b.branch("decl", b.branch("type", b.leaf("int"), b.branch("WS", b.leaf("\\s+", " "))), b.branch("name", b.leaf("[a-zA-Z0-9]+", "i5")),
								b.leaf(";"), b.branch("WS", b.leaf("\\s+", " "))),
						b.branch("decl", b.branch("type", b.leaf("int"), b.branch("WS", b.leaf("\\s+", " "))), b.branch("name", b.leaf("[a-zA-Z0-9]+", "i6")),
								b.leaf(";"), b.branch("WS", b.leaf("\\s+", " "))),
						b.branch("decl", b.branch("type", b.leaf("int"), b.branch("WS", b.leaf("\\s+", " "))), b.branch("name", b.leaf("[a-zA-Z0-9]+", "i7")),
								b.leaf(";"), b.branch("WS", b.leaf("\\s+", " "))),
						b.branch("decl", b.branch("type", b.leaf("int"), b.branch("WS", b.leaf("\\s+", " "))), b.branch("name", b.leaf("[a-zA-Z0-9]+", "i8")),
								b.leaf(";"), b.branch("WS", b.leaf("\\s+", " ")))),
				b.leaf("}"));
		Assert.assertEquals(expected, tree.getRoot());

	}

	// abstraction of weird postfix rules from Java8 grammar
	Grammar notGreedy() {
		final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("S").concatenation(new NonTerminal("postfix"), new TerminalLiteral("++"));
		b.rule("postfix").concatenation(new NonTerminal("expr"), new NonTerminal("multiPPs"));
		b.rule("multiPPs").multi(0, -1, new TerminalLiteral("++"));
		b.rule("expr").choice(new TerminalLiteral("a"));
		return b.get();
	}

	@Test
	public void notGreedy_S_app() throws ParseFailedException {
		final Grammar g = this.notGreedy();
		final String goal = "S";
		final String text = "a++";
		final IParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);
	}
}
