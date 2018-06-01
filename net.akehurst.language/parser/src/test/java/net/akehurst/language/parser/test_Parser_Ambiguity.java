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
import net.akehurst.language.api.sppt.SPPTBranch;
import net.akehurst.language.api.sppt.SharedPackedParseTree;
import net.akehurst.language.grammar.parser.forrest.ParseTreeBuilder;
import net.akehurst.language.ogl.semanticStructure.GrammarDefault;
import net.akehurst.language.ogl.semanticStructure.GrammarBuilderDefault;
import net.akehurst.language.ogl.semanticStructure.NamespaceDefault;
import net.akehurst.language.ogl.semanticStructure.NonTerminalDefault;
import net.akehurst.language.ogl.semanticStructure.TerminalLiteralDefault;
import net.akehurst.language.ogl.semanticStructure.TerminalPatternDefault;
import net.akehurst.language.parser.sppf.SharedPackedParseTreeSimple;

public class test_Parser_Ambiguity extends AbstractParser_Test {

	GrammarDefault am() {
		final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
		b.rule("S").multi(0, -1, new TerminalLiteralDefault("a"));
		return b.get();
	}

	GrammarDefault aq() {
		final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
		b.rule("S").multi(0, 1, new TerminalLiteralDefault("a"));
		return b.get();
	}

	GrammarDefault aab() {
		final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
		b.rule("S").choice(new TerminalLiteralDefault("a"), new NonTerminalDefault("ab"));
		b.rule("ab").concatenation(new TerminalLiteralDefault("a"), new TerminalLiteralDefault("b"));
		return b.get();
	}

	GrammarDefault ae() {
		final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
		b.rule("S").choice(new TerminalLiteralDefault("a"), new NonTerminalDefault("nothing"));
		b.rule("nothing").choice();
		return b.get();
	}

	GrammarDefault amq() {
		final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
		b.rule("S").multi(0, 1, new NonTerminalDefault("am"));
		b.rule("am").multi(0, -1, new TerminalLiteralDefault("a"));
		return b.get();
	}

	GrammarDefault x() {
		final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
		b.rule("S").multi(0, 1, new NonTerminalDefault("aaa"));
		b.rule("aaa").choice(new NonTerminalDefault("a1"), new NonTerminalDefault("a2"), new NonTerminalDefault("a3"));
		b.rule("a1").multi(0, 1, new TerminalLiteralDefault("a"));
		b.rule("a2").multi(0, 2, new TerminalLiteralDefault("a"));
		b.rule("a3").multi(0, 3, new TerminalLiteralDefault("a"));
		return b.get();
	}

	GrammarDefault tg() {
		final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
		b.skip("WS").concatenation(new TerminalPatternDefault("\\s+"));
		b.rule("fps").choice(new NonTerminalDefault("fps.choice1"), new NonTerminalDefault("fps.choice2"));
		b.rule("fps.choice1").concatenation(new NonTerminalDefault("fp"), new NonTerminalDefault("fps.choice1.group.multi"));
		b.rule("fps.choice1.group.multi").multi(0, -1, new NonTerminalDefault("fps.choice1.group"));
		b.rule("fps.choice1.group").concatenation(new TerminalLiteralDefault(","), new NonTerminalDefault("fp"));
		b.rule("fps.choice2").concatenation(new NonTerminalDefault("rp"), new NonTerminalDefault("fps.choice1.group.multi"));
		b.rule("fp").concatenation(new NonTerminalDefault("t"), new NonTerminalDefault("name"));
		b.rule("rp").concatenation(new NonTerminalDefault("name"), new NonTerminalDefault("rp.multi"), new TerminalLiteralDefault("this"));
		b.rule("rp.multi").multi(0, 1, new NonTerminalDefault("rp.multi.group"));
		b.rule("rp.multi.group").concatenation(new NonTerminalDefault("name"), new TerminalLiteralDefault("."));
		b.rule("t").choice(new NonTerminalDefault("bt"), new NonTerminalDefault("gt"));
		b.rule("bt").concatenation(new NonTerminalDefault("name"));
		b.rule("gt").concatenation(new NonTerminalDefault("name"), new TerminalLiteralDefault("("), new NonTerminalDefault("name"), new TerminalLiteralDefault(")"));
		b.rule("name").choice(new TerminalPatternDefault("[a-zA-Z]+"));
		return b.get();
	}

	@Test
	public void am_S_empty() throws ParseFailedException {
		// grammar, goal, input

		final GrammarDefault g = this.am();
		final String goal = "S";
		final String text = "";

		final SharedPackedParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		final SharedPackedParseTree expected = new SharedPackedParseTreeSimple(b.branch("S", b.emptyLeaf("S")));
		Assert.assertEquals(expected, tree);

	}

	@Test
	public void am_S_a() throws ParseFailedException {
		// grammar, goal, input

		final GrammarDefault g = this.am();
		final String goal = "S";
		final String text = "a";

		final SharedPackedParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		// b.define("S { 'a' }");
		final SharedPackedParseTree expected = new SharedPackedParseTreeSimple(b.branch("S", b.leaf("a")));
		// final IBranch expected = b.branch("S", b.leaf("a"));
		Assert.assertEquals(expected, tree);

	}

	@Test
	public void am_S_aa() throws ParseFailedException {
		// grammar, goal, input

		final GrammarDefault g = this.am();
		final String goal = "S";
		final String text = "aa";

		final SharedPackedParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		final SPPTBranch expected = b.branch("S", b.leaf("a"), b.leaf("a"));
		Assert.assertEquals(expected, tree.getRoot());

	}

	@Test
	public void x_S_a() throws ParseFailedException {
		// grammar, goal, input
		final GrammarDefault g = this.x();
		final String goal = "S";
		final String text = "a";

		final SharedPackedParseTree actual = this.process(g, text, goal);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		b.define("S{ aaa { a1{'a'} } }");
		b.buildAndAdd();

		b.define("S{ aaa { a2{'a'} } }");
		b.buildAndAdd();

		b.define("S{ aaa { a3{'a'} } }");
		final SharedPackedParseTree expected = b.buildAndAdd();

		Assert.assertNotNull(actual);
		Assert.assertEquals(expected, actual);
	}

	@Test
	public void x_S_aa() throws ParseFailedException {
		// grammar, goal, input
		final GrammarDefault g = this.x();
		final String goal = "S";
		final String text = "aa";

		final SharedPackedParseTree actual = this.process(g, text, goal);
		Assert.assertNotNull(actual);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		b.define("S{ aaa { a2{'a' 'a'} } }");
		b.buildAndAdd();

		b.define("S{ aaa { a3{'a' 'a'} } }");
		final SharedPackedParseTree expected = b.buildAndAdd();

		Assert.assertNotNull(actual);
		Assert.assertEquals(expected, actual);
	}

	@Test
	public void x_S_aaa() throws ParseFailedException {
		// grammar, goal, input
		final GrammarDefault g = this.x();
		final String goal = "S";
		final String text = "aaa";

		final SharedPackedParseTree actual = this.process(g, text, goal);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		b.define("S{ aaa { a3{'a' 'a' 'a'} } }");
		final SharedPackedParseTree expected = b.buildAndAdd();

		Assert.assertNotNull(actual);
		Assert.assertEquals(expected, actual);

	}

	@Test
	public void tg_fp_V() throws ParseFailedException {
		// grammar, goal, input

		final GrammarDefault g = this.tg();
		final String goal = "fp";
		final String text = "V v";

		final SharedPackedParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		final SPPTBranch expected = b.branch("fp",
				b.branch("t", b.branch("bt", b.branch("name", b.leaf("[a-zA-Z]+", "V"), b.branch("WS", b.leaf("\\s+", " "))))),
				b.branch("name", b.leaf("[a-zA-Z]+", "v")));
		Assert.assertEquals(expected, tree.getRoot());

	}

	@Test
	public void tg_fp_VE() throws ParseFailedException {
		// grammar, goal, input

		final GrammarDefault g = this.tg();
		final String goal = "fp";
		final String text = "V(E) v";

		final SharedPackedParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		final SPPTBranch expected = b.branch("fp", b.branch("t", b.branch("gt", b.branch("name", b.leaf("[a-zA-Z]+", "V")), b.leaf("("),
				b.branch("name", b.leaf("[a-zA-Z]+", "E")), b.leaf(")"), b.branch("WS", b.leaf("\\s+", " ")))), b.branch("name", b.leaf("[a-zA-Z]+", "v")));
		Assert.assertEquals(expected, tree.getRoot());

	}

	@Test
	public void tg_fps_choice1_VE() throws ParseFailedException {
		// grammar, goal, input

		final GrammarDefault g = this.tg();
		final String goal = "fps.choice1";
		final String text = "V(E) v";

		final SharedPackedParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		final SPPTBranch expected = b.branch("fps.choice1",
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

		final GrammarDefault g = this.tg();
		final String goal = "fps";
		final String text = "V(E) v";

		final SharedPackedParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		final SharedPackedParseTree expected = new SharedPackedParseTreeSimple(b.branch("fps",
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

		final GrammarDefault g = this.tg();
		final String goal = "fps";
		final String text = "V A.this";

		final SharedPackedParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		final SPPTBranch expected = b.branch("fps",
				b.branch("fps.choice2",
						b.branch("rp", b.branch("name", b.leaf("[a-zA-Z]+", "V"), b.branch("WS", b.leaf("\\s+", " "))),
								b.branch("rp.multi", b.branch("rp.multi.group", b.branch("name", b.leaf("[a-zA-Z]+", "A")), b.leaf("."))), b.leaf("this")),
						b.branch("fps.choice1.group.multi", b.emptyLeaf("fps.choice1.group.multi"))));
		Assert.assertEquals(expected, tree.getRoot());

	}

	GrammarDefault caseBlock() {
		final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
		b.skip("WS").concatenation(new TerminalPatternDefault("\\s+"));
		b.rule("block").concatenation(new TerminalLiteralDefault("{"), new NonTerminalDefault("group1"), new NonTerminalDefault("group2"), new TerminalLiteralDefault("}"));
		b.rule("group1").multi(0, -1, new NonTerminalDefault("labelBlock"));
		b.rule("group2").multi(0, -1, new NonTerminalDefault("label"));
		b.rule("labelBlock").concatenation(new NonTerminalDefault("labels"), new TerminalLiteralDefault("{"), new TerminalLiteralDefault("}"));
		b.rule("labels").multi(0, -1, new NonTerminalDefault("label"));
		b.rule("label").concatenation(new TerminalLiteralDefault("case"), new NonTerminalDefault("int"), new TerminalLiteralDefault(":"));
		b.rule("int").choice(new TerminalPatternDefault("[0-9]+"));
		return b.get();
	}

	@Test
	public void ambiguity_int() throws ParseFailedException {
		// grammar, goal, input

		final GrammarDefault g = this.caseBlock();
		final String goal = "int";
		final String text = "1";

		final SharedPackedParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		final SPPTBranch expected = b.branch("int", b.leaf("[0-9]+", "1"));
		Assert.assertEquals(expected, tree.getRoot());

	}

	@Test
	public void ambiguity_label() throws ParseFailedException {
		// grammar, goal, input

		final GrammarDefault g = this.caseBlock();
		final String goal = "label";
		final String text = "case 1 :";

		final SharedPackedParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		final SharedPackedParseTree expected = new SharedPackedParseTreeSimple(b.branch("label", b.leaf("case"), b.branch("WS", b.leaf("\\s+", " ")),
				b.branch("int", b.leaf("[0-9]+", "1"), b.branch("WS", b.leaf("\\s+", " "))), b.leaf(":")));
		Assert.assertEquals(expected, tree);

	}

	@Test
	public void ambiguity_labels() throws ParseFailedException {
		// grammar, goal, input

		final GrammarDefault g = this.caseBlock();
		final String goal = "labels";
		final String text = "case 1 :";

		final SharedPackedParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		final SharedPackedParseTree expected = new SharedPackedParseTreeSimple(b.branch("labels", b.branch("label", b.leaf("case"),
				b.branch("WS", b.leaf("\\s+", " ")), b.branch("int", b.leaf("[0-9]+", "1"), b.branch("WS", b.leaf("\\s+", " "))), b.leaf(":"))));
		Assert.assertEquals(expected, tree);

	}

	@Test
	public void ambiguity_labelBlock() throws ParseFailedException {
		// grammar, goal, input

		final GrammarDefault g = this.caseBlock();
		final String goal = "labelBlock";
		final String text = "case 1 : { }";

		final SharedPackedParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		final SPPTBranch expected = b.branch("labelBlock",
				b.branch("labels",
						b.branch("label", b.leaf("case"), b.branch("WS", b.leaf("\\s+", " ")),
								b.branch("int", b.leaf("[0-9]+", "1"), b.branch("WS", b.leaf("\\s+", " "))), b.leaf(":"), b.branch("WS", b.leaf("\\s+", " ")))),
				b.leaf("{"), b.branch("WS", b.leaf("\\s+", " ")), b.leaf("}"));
		Assert.assertEquals(expected, tree.getRoot());

	}

	@Test
	public void ambiguity_block() throws ParseFailedException {
		// grammar, goal, input

		final GrammarDefault g = this.caseBlock();
		final String goal = "block";
		final String text = "{ case 1 : { } }";

		final SharedPackedParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		final SPPTBranch expected = b.branch("block", b.leaf("{"), b.branch("WS", b.leaf("\\s+", " ")),
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

		final GrammarDefault g = this.caseBlock();
		final String goal = "block";
		final String text = "{ case 1 : }";

		final SharedPackedParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		final SharedPackedParseTree expected = new SharedPackedParseTreeSimple(
				b.branch("block", b.leaf("{"), b.branch("WS", b.leaf("\\s+", " ")), b.branch("group1", b.emptyLeaf("group1")),
						b.branch("group2", b.branch("label", b.leaf("case"), b.branch("WS", b.leaf("\\s+", " ")),
								b.branch("int", b.leaf("[0-9]+", "1"), b.branch("WS", b.leaf("\\s+", " "))), b.leaf(":"), b.branch("WS", b.leaf("\\s+", " ")))),
						b.leaf("}")));
		Assert.assertEquals(expected, tree);

	}

	GrammarDefault varDeclBlock() {
		final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
		b.skip("WS").concatenation(new TerminalPatternDefault("\\s+"));
		b.rule("block").concatenation(new TerminalLiteralDefault("{"), new NonTerminalDefault("decls"), new TerminalLiteralDefault("}"));
		b.rule("decls").multi(0, -1, new NonTerminalDefault("decl"));
		b.rule("decl").concatenation(new NonTerminalDefault("type"), new NonTerminalDefault("name"), new TerminalLiteralDefault(";"));
		b.rule("type").priorityChoice(new TerminalLiteralDefault("int"), new NonTerminalDefault("name"));
		b.rule("name").choice(new TerminalPatternDefault("[a-zA-Z0-9]+"));
		return b.get();
	}

	@Test
	public void varDeclBlock_block_empty() throws ParseFailedException {
		// grammar, goal, input

		final GrammarDefault g = this.varDeclBlock();
		final String goal = "block";
		final String text = "{}";

		final SharedPackedParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		final SPPTBranch expected = b.branch("block", b.leaf("{"), b.branch("decls", b.emptyLeaf("decls")), b.leaf("}"));
		Assert.assertEquals(expected, tree.getRoot());

	}

	@Test
	public void varDeclBlock_block_1() throws ParseFailedException {
		// grammar, goal, input

		final GrammarDefault g = this.varDeclBlock();
		final String goal = "block";
		final String text = "{ int i; }";

		final SharedPackedParseTree actual = this.process(g, text, goal);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		b.define("block {");
		b.define("  '{'");
		b.define("  WS { '\\s+' : ' ' }");
		b.define("  decls {");
		b.define("    decl {");
		b.define("      type { 'int' WS { '\\s+' : ' ' } }");
		b.define("      name { '[a-zA-Z0-9]+' : 'i' }");
		b.define("      ';'");
		b.define("      WS { '\\s+' : ' ' }");
		b.define("    }");
		b.define("  }");
		b.define("  '}'");
		b.define("}");

		final SharedPackedParseTree expected = b.buildAndAdd();

		Assert.assertNotNull(actual);
		Assert.assertEquals(expected, actual);

	}

	@Test
	public void varDeclBlock_block_2() throws ParseFailedException {
		// grammar, goal, input

		final GrammarDefault g = this.varDeclBlock();
		final String goal = "block";
		final String text = "{int i1;int i2;}";

		final SharedPackedParseTree actual = this.process(g, text, goal);
		Assert.assertNotNull(actual);

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
		final SharedPackedParseTree expected = b.buildAndAdd();

		Assert.assertNotNull(actual);
		Assert.assertEquals(expected, actual);

	}

	@Test
	public void varDeclBlock_block_8() throws ParseFailedException {
		// grammar, goal, input

		final GrammarDefault g = this.varDeclBlock();
		final String goal = "block";
		final String text = "{ int i1; int i2; int i3; int i4; int i5; int i6; int i7; int i8; }";

		final SharedPackedParseTree actual = this.process(g, text, goal);
		Assert.assertNotNull(actual);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		b.define("block {");
		b.define("  '{'");
		b.define("  WS { '\\s+':' '}");
		b.define("  decls {");
		b.define("    decl {");
		b.define("      type { 'int' WS { '\\s+':' '} }");
		b.define("      name { '[a-zA-Z0-9]+':'i1' }");
		b.define("      ';'");
		b.define("      WS { '\\s+' : ' ' }");
		b.define("    }");
		b.define("    decl {");
		b.define("      type { 'int' WS { '\\s+':' '} }");
		b.define("      name { '[a-zA-Z0-9]+':'i2' }");
		b.define("      ';'");
		b.define("      WS { '\\s+' : ' ' }");
		b.define("    }");
		b.define("    decl {");
		b.define("      type { 'int' WS { '\\s+':' '} }");
		b.define("      name { '[a-zA-Z0-9]+':'i2' }");
		b.define("      ';'");
		b.define("      WS { '\\s+' : ' ' }");
		b.define("    }");
		b.define("    decl {");
		b.define("      type { 'int' WS { '\\s+':' '} }");
		b.define("      name { '[a-zA-Z0-9]+':'i2' }");
		b.define("      ';'");
		b.define("      WS { '\\s+' : ' ' }");
		b.define("    }");
		b.define("    decl {");
		b.define("      type { 'int' WS { '\\s+':' '} }");
		b.define("      name { '[a-zA-Z0-9]+':'i1' }");
		b.define("      ';'");
		b.define("      WS { '\\s+' : ' ' }");
		b.define("    }");
		b.define("    decl {");
		b.define("      type { 'int' WS { '\\s+':' '} }");
		b.define("      name { '[a-zA-Z0-9]+':'i2' }");
		b.define("      ';'");
		b.define("      WS { '\\s+' : ' ' }");
		b.define("    }");
		b.define("    decl {");
		b.define("      type { 'int' WS { '\\s+':' '} }");
		b.define("      name { '[a-zA-Z0-9]+':'i2' }");
		b.define("      ';'");
		b.define("      WS { '\\s+' : ' ' }");
		b.define("    }");
		b.define("    decl {");
		b.define("      type { 'int' WS { '\\s+':' '} }");
		b.define("      name { '[a-zA-Z0-9]+':'i2' }");
		b.define("      ';'");
		b.define("      WS { '\\s+' : ' ' }");
		b.define("    }");
		b.define("  }");
		b.define("  '}'");
		b.define("}");
		final SharedPackedParseTree expected = b.buildAndAdd();

		Assert.assertNotNull(actual);
		Assert.assertEquals(expected, actual);

	}

	// abstraction of weird postfix rules from Java8 grammar
	GrammarDefault notGreedy() {
		final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
		b.rule("S").concatenation(new NonTerminalDefault("postfix"), new TerminalLiteralDefault("++"));
		b.rule("postfix").concatenation(new NonTerminalDefault("expr"), new NonTerminalDefault("multiPPs"));
		b.rule("multiPPs").multi(0, -1, new TerminalLiteralDefault("++"));
		b.rule("expr").choice(new TerminalLiteralDefault("a"));
		return b.get();
	}

	@Test
	public void notGreedy_S_app() throws ParseFailedException {
		final GrammarDefault g = this.notGreedy();
		final String goal = "S";
		final String text = "a++";
		final SharedPackedParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);
	}

	// S = pd? td? ;
	// pd = pm? 'p' ;
	// pm = an ;
	// td = cd ;
	// cd = cm? 'c' ;
	// cm = an ;
	// an = 'a' ;
	GrammarDefault xxx() {
		final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
		b.skip("WS").choice(new TerminalPatternDefault("\\s+"));
		b.rule("S").concatenation(new NonTerminalDefault("packageDeclaration_m"), new NonTerminalDefault("importDeclaration_m"), new NonTerminalDefault("typeDeclaration_m"));
		b.rule("packageDeclaration_m").multi(0, 1, new NonTerminalDefault("packageDeclaration"));
		b.rule("packageDeclaration").concatenation(new NonTerminalDefault("packageModifier_m"), new TerminalLiteralDefault("package"));
		b.rule("importDeclaration_m").multi(0, -1, new NonTerminalDefault("importDeclaration"));
		b.rule("importDeclaration").concatenation(new TerminalLiteralDefault("import"), new TerminalLiteralDefault(";"));
		b.rule("packageModifier_m").multi(0, -1, new NonTerminalDefault("packageModifier"));
		b.rule("packageModifier").choice(new NonTerminalDefault("annotation"));
		b.rule("typeDeclaration_m").multi(0, -1, new NonTerminalDefault("typeDeclaration"));
		b.rule("typeDeclaration").choice(new NonTerminalDefault("classDeclaration"), new NonTerminalDefault("interfaceDeclaration"));
		b.rule("classDeclaration").concatenation(new NonTerminalDefault("classModifier_m"), new TerminalLiteralDefault("class"));
		b.rule("classModifier_m").multi(0, -1, new NonTerminalDefault("classModifier"));
		b.rule("classModifier").choice(new NonTerminalDefault("annotation"));
		b.rule("interfaceDeclaration").concatenation(new NonTerminalDefault("interfaceModifier_m"), new TerminalLiteralDefault("interface"));
		b.rule("interfaceModifier_m").multi(0, -1, new NonTerminalDefault("interfaceModifier"));
		b.rule("interfaceModifier").choice(new NonTerminalDefault("annotation"));
		b.rule("annotation").choice(new NonTerminalDefault("normalAnnotation"), new NonTerminalDefault("markerAnnotation"), new NonTerminalDefault("singleElementAnnotation"));
		b.rule("normalAnnotation").concatenation(new TerminalLiteralDefault("@"), new NonTerminalDefault("Identifier"), new TerminalLiteralDefault("("),
				new NonTerminalDefault("elementValuePairList_m"), new TerminalLiteralDefault(")"));
		b.rule("markerAnnotation").concatenation(new TerminalLiteralDefault("@"), new NonTerminalDefault("Identifier"));
		b.rule("singleElementAnnotation").concatenation(new TerminalLiteralDefault("@"), new NonTerminalDefault("Identifier"), new TerminalLiteralDefault("("),
				new TerminalLiteralDefault("value"), new TerminalLiteralDefault(")"));
		b.rule("elementValuePairList_m").multi(0, 1, new NonTerminalDefault("elementValuePairList"));
		b.rule("elementValuePairList").choice(new NonTerminalDefault("elementValuePair"));
		b.rule("elementValuePair").concatenation(new TerminalLiteralDefault("element"), new TerminalLiteralDefault("="), new TerminalLiteralDefault("value"));
		b.rule("Identifier").choice(new TerminalPatternDefault("[a-zA-Z][a-zA-z0-9]*"));
		return b.get();
	}

	@Test
	public void xxx_S_apac() throws ParseFailedException {
		final GrammarDefault g = this.xxx();
		final String goal = "S";
		final String text = "@An package @An class";
		final SharedPackedParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);
	}

	@Test
	public void xxx_S_apac2() throws ParseFailedException {
		final GrammarDefault g = this.xxx();
		final String goal = "S";
		final String text = "@An() package @An class";
		final SharedPackedParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);
	}

	@Test
	public void xxx_S_apac3() throws ParseFailedException {
		final GrammarDefault g = this.xxx();
		final String goal = "S";
		final String text = "@An() package @An() class";
		final SharedPackedParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);
	}

	@Test
	public void xxx_S_p() throws ParseFailedException {
		final GrammarDefault g = this.xxx();
		final String goal = "S";
		final String text = "package";
		final SharedPackedParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);
	}

	@Test
	public void xxx_S_ap() throws ParseFailedException {
		final GrammarDefault g = this.xxx();
		final String goal = "S";
		final String text = "@An package";
		final SharedPackedParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);
	}

	@Test
	public void xxx_S_ap2() throws ParseFailedException {
		final GrammarDefault g = this.xxx();
		final String goal = "S";
		final String text = "@An() package";
		final SharedPackedParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);
	}

	@Test
	public void xxx_S_apc() throws ParseFailedException {
		final GrammarDefault g = this.xxx();
		final String goal = "S";
		final String text = "@An package class";
		final SharedPackedParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);
	}

	@Test
	public void xxx_S_apc2() throws ParseFailedException {
		final GrammarDefault g = this.xxx();
		final String goal = "S";
		final String text = "@An() package class";
		final SharedPackedParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);
	}

	@Test
	public void xxx_S_ac() throws ParseFailedException {
		final GrammarDefault g = this.xxx();
		final String goal = "S";
		final String text = "@An class";
		final SharedPackedParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);
	}

	@Test
	public void xxx_S_ac2() throws ParseFailedException {
		final GrammarDefault g = this.xxx();
		final String goal = "S";
		final String text = "@An() class";
		final SharedPackedParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);
	}

	@Test
	public void xxx_S_c() throws ParseFailedException {
		final GrammarDefault g = this.xxx();
		final String goal = "S";
		final String text = "class";
		final SharedPackedParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);
	}

	@Test
	public void xxx_S_ai() throws ParseFailedException {
		final GrammarDefault g = this.xxx();
		final String goal = "S";
		final String text = "@An interface";
		final SharedPackedParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);
	}

	@Test
	public void xxx_S_ai2() throws ParseFailedException {
		final GrammarDefault g = this.xxx();
		final String goal = "S";
		final String text = "@An() interface";
		final SharedPackedParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);
	}

	// S = pd? td? ;
	// pd = pm? 'p' ;
	// pm = an ;
	// td = cd ;
	// cd = cm? 'c' ;
	// cm = an ;
	// an = 'a' ;
	GrammarDefault xxx2() {
		final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
		b.skip("WS").choice(new TerminalPatternDefault("\\s+"));
		b.rule("S").concatenation(new NonTerminalDefault("packageDeclaration_m"), new NonTerminalDefault("importDeclaration_m"), new NonTerminalDefault("typeDeclaration_m"));
		b.rule("packageDeclaration_m").multi(0, 1, new NonTerminalDefault("packageDeclaration"));
		b.rule("packageDeclaration").concatenation(new NonTerminalDefault("packageModifier_m"), new TerminalLiteralDefault("package"));
		b.rule("importDeclaration_m").multi(0, -1, new NonTerminalDefault("importDeclaration"));
		b.rule("importDeclaration").concatenation(new TerminalLiteralDefault("import"), new TerminalLiteralDefault(";"));
		b.rule("packageModifier_m").multi(0, -1, new NonTerminalDefault("packageModifier"));
		b.rule("packageModifier").choice(new NonTerminalDefault("annotation"));
		b.rule("typeDeclaration_m").multi(0, -1, new NonTerminalDefault("typeDeclaration"));
		b.rule("typeDeclaration").choice(new NonTerminalDefault("classDeclaration"), new NonTerminalDefault("interfaceDeclaration"));
		b.rule("classDeclaration").choice(new NonTerminalDefault("normalClassDeclaration"), new NonTerminalDefault("enumDeclaration"));
		b.rule("normalClassDeclaration").concatenation(new NonTerminalDefault("classModifier_m"), new TerminalLiteralDefault("class"), new NonTerminalDefault("Identifier"),
				new NonTerminalDefault("typeParameters_m"), new NonTerminalDefault("superclass_m"), new NonTerminalDefault("superinterfaces_m"), new NonTerminalDefault("classBody"));
		b.rule("classModifier_m").multi(0, -1, new NonTerminalDefault("classModifier"));
		b.rule("classModifier").choice(new NonTerminalDefault("annotation"));
		b.rule("interfaceDeclaration").concatenation(new NonTerminalDefault("interfaceModifier_m"), new TerminalLiteralDefault("interface"), new NonTerminalDefault("Identifier"));
		b.rule("interfaceModifier_m").multi(0, -1, new NonTerminalDefault("interfaceModifier"));
		b.rule("interfaceModifier").choice(new NonTerminalDefault("annotation"));
		b.rule("annotation").choice(new NonTerminalDefault("normalAnnotation"), new NonTerminalDefault("markerAnnotation"), new NonTerminalDefault("singleElementAnnotation"));
		b.rule("normalAnnotation").concatenation(new TerminalLiteralDefault("@"), new NonTerminalDefault("Identifier"), new TerminalLiteralDefault("("),
				new NonTerminalDefault("elementValuePairList_m"), new TerminalLiteralDefault(")"));
		b.rule("markerAnnotation").concatenation(new TerminalLiteralDefault("@"), new NonTerminalDefault("Identifier"));
		b.rule("singleElementAnnotation").concatenation(new TerminalLiteralDefault("@"), new NonTerminalDefault("Identifier"), new TerminalLiteralDefault("("),
				new TerminalLiteralDefault("value"), new TerminalLiteralDefault(")"));
		b.rule("elementValuePairList_m").multi(0, 1, new NonTerminalDefault("elementValuePairList"));
		b.rule("elementValuePairList").choice(new NonTerminalDefault("elementValuePair"));
		b.rule("elementValuePair").concatenation(new TerminalLiteralDefault("element"), new TerminalLiteralDefault("="), new TerminalLiteralDefault("value"));
		b.rule("Identifier").choice(new TerminalPatternDefault("[a-zA-Z][a-zA-z0-9]*"));
		b.rule("typeParameters_m").multi(0, -1, new NonTerminalDefault("typeParameters"));
		b.rule("typeParameters").concatenation(new TerminalLiteralDefault("<"), new TerminalLiteralDefault(">"));
		b.rule("superclass_m").multi(0, -1, new NonTerminalDefault("superclass"));
		b.rule("superclass").concatenation(new TerminalLiteralDefault("extends"), new NonTerminalDefault("Identifier"));
		b.rule("superinterfaces_m").multi(0, -1, new NonTerminalDefault("superinterfaces"));
		b.rule("superinterfaces").concatenation(new TerminalLiteralDefault("implements"), new NonTerminalDefault("interfaceTypeList"));
		b.rule("interfaceTypeList").separatedList(0, -1, new TerminalLiteralDefault(","), new NonTerminalDefault("Identifier"));
		b.rule("classBody").concatenation(new TerminalLiteralDefault("{"), new NonTerminalDefault("classBodyDeclaration_m"), new TerminalLiteralDefault("}"));
		b.rule("classBodyDeclaration_m").multi(0, -1, new NonTerminalDefault("classBodyDeclaration"));
		b.rule("classBodyDeclaration").concatenation(new TerminalLiteralDefault("body"));
		b.rule("enumDeclaration").concatenation(new NonTerminalDefault("classModifier_m"), new TerminalLiteralDefault("enum"), new NonTerminalDefault("Identifier"),
				new NonTerminalDefault("superinterfaces_m"), new NonTerminalDefault("enumBody"));
		b.rule("enumBody").concatenation(new TerminalLiteralDefault("{"), new TerminalLiteralDefault("}"));

		return b.get();
	}

	@Test
	public void xxx2_S_ac() throws ParseFailedException {
		final GrammarDefault g = this.xxx2();
		final String goal = "S";
		final String text = "@An() class An { }";
		final SharedPackedParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);
	}
}
