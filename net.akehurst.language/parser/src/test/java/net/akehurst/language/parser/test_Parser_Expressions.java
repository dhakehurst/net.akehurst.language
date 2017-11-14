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
import net.akehurst.language.core.sppt.ISPBranch;
import net.akehurst.language.core.sppt.ISharedPackedParseTree;
import net.akehurst.language.grammar.parser.forrest.ParseTreeBuilder;
import net.akehurst.language.ogl.semanticStructure.Grammar;
import net.akehurst.language.ogl.semanticStructure.GrammarBuilder;
import net.akehurst.language.ogl.semanticStructure.Namespace;
import net.akehurst.language.ogl.semanticStructure.NonTerminal;
import net.akehurst.language.ogl.semanticStructure.TerminalLiteral;
import net.akehurst.language.ogl.semanticStructure.TerminalPattern;

public class test_Parser_Expressions extends AbstractParser_Test {

	Grammar expression_plus_multiply() {
		final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("S").concatenation(new NonTerminal("e"));
		b.rule("e").priorityChoice(new NonTerminal("variable"), new NonTerminal("multiply"), new NonTerminal("plus"));
		b.rule("plus").concatenation(new NonTerminal("e"), new TerminalLiteral("+"), new NonTerminal("e"));
		b.rule("multiply").concatenation(new NonTerminal("e"), new TerminalLiteral("*"), new NonTerminal("e"));
		b.rule("variable").concatenation(new TerminalLiteral("a"));

		return b.get();
	}

	@Test
	public void emp_S_a() {
		// grammar, goal, input
		try {
			final Grammar g = this.expression_plus_multiply();
			final String goal = "S";
			final String text = "a";

			final ISharedPackedParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);

			final ParseTreeBuilder b = this.builder(g, text, goal);
			final ISPBranch expected = b.branch("S", b.branch("e", b.branch("variable", b.leaf("a"))));
			Assert.assertEquals(expected, tree.getRoot());

		} catch (final ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void emp_S_apa() {
		// grammar, goal, input
		try {
			final Grammar g = this.expression_plus_multiply();
			final String goal = "S";
			final String text = "a+a";

			final ISharedPackedParseTree tree = this.process(g, text, goal);
			Assert.assertNotNull(tree);

			final ParseTreeBuilder b = this.builder(g, text, goal);
			;
			final ISPBranch expected = b.branch("S", b.branch("e",
					b.branch("plus", b.branch("e", b.branch("variable", b.leaf("a"))), b.leaf("+"), b.branch("e", b.branch("variable", b.leaf("a"))))));
			Assert.assertEquals(expected, tree.getRoot());

		} catch (final ParseFailedException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void emp_S_ama() throws ParseFailedException {
		// grammar, goal, input

		final Grammar g = this.expression_plus_multiply();
		final String goal = "S";
		final String text = "a*a";

		final ISharedPackedParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		;
		final ISPBranch expected = b.branch("S", b.branch("e",
				b.branch("multiply", b.branch("e", b.branch("variable", b.leaf("a"))), b.leaf("*"), b.branch("e", b.branch("variable", b.leaf("a"))))));
		Assert.assertEquals(expected, tree.getRoot());

	}

	@Test
	public void emp_S_apama() throws ParseFailedException {
		// grammar, goal, input

		final Grammar g = this.expression_plus_multiply();
		final String goal = "S";
		final String text = "a+a*a";

		final ISharedPackedParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		b.define("S {");
		b.define("  e {");
		b.define("    plus {");
		b.define("      e { variable { 'a' } }");
		b.define("      '+'");
		b.define("      e {");
		b.define("        multiply {");
		b.define("          e { variable { 'a' } }");
		b.define("          '*'");
		b.define("          e { variable { 'a' } }");
		b.define("        }");
		b.define("      }");
		b.define("    }");
		b.define("  }");
		b.define("}");
		final ISharedPackedParseTree expected = b.buildAndAdd();
		// final IBranch expected = b.branch("S", b.branch("e", b.branch("plus", b.branch("e", b.branch("variable", b.leaf("a"))), b.leaf("+"),
		// b.branch("e",
		// b.branch("multiply", b.branch("e", b.branch("variable", b.leaf("a"))), b.leaf("*"), b.branch("e", b.branch("variable", b.leaf("a"))))))));
		Assert.assertEquals(expected, tree);

	}

	@Test
	public void emp_S_amapa() throws ParseFailedException {
		// grammar, goal, input

		final Grammar g = this.expression_plus_multiply();
		final String goal = "S";
		final String text = "a*a+a";

		final ISharedPackedParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		b.define("S {");
		b.define("  e {");
		b.define("    plus {");
		b.define("      e {");
		b.define("        multiply {");
		b.define("          e { variable { 'a' } }");
		b.define("          '*'");
		b.define("          e { variable { 'a' } }");
		b.define("        }");
		b.define("      }");
		b.define("      '+'");
		b.define("      e { variable { 'a' } }");
		b.define("    }");
		b.define("  }");
		b.define("}");
		final ISharedPackedParseTree expected = b.buildAndAdd();

		Assert.assertEquals(expected, tree);

	}

	Grammar expression_if_then_else() {
		final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("S").concatenation(new NonTerminal("e"));
		b.skip("WS").concatenation(new TerminalPattern("\\s+"));
		b.rule("e").priorityChoice(new NonTerminal("variable"), new NonTerminal("multiply"), new NonTerminal("plus"), new NonTerminal("ifthenelse"),
				new NonTerminal("ifthen"));
		b.rule("ifthen").concatenation(new TerminalLiteral("if"), new NonTerminal("e"), new TerminalLiteral("then"), new NonTerminal("e"));
		b.rule("ifthenelse").concatenation(new TerminalLiteral("if"), new NonTerminal("e"), new TerminalLiteral("then"), new NonTerminal("e"),
				new TerminalLiteral("else"), new NonTerminal("e"));
		b.rule("plus").concatenation(new NonTerminal("e"), new TerminalLiteral("+"), new NonTerminal("e"));
		b.rule("multiply").concatenation(new NonTerminal("e"), new TerminalLiteral("*"), new NonTerminal("e"));
		b.rule("variable").concatenation(new TerminalLiteral("a"));

		return b.get();
	}

	@Test
	public void eite_S_a() throws ParseFailedException {
		// grammar, goal, input

		final Grammar g = this.expression_if_then_else();
		final String goal = "S";
		final String text = "a";

		final ISharedPackedParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		;
		final ISPBranch expected = b.branch("S", b.branch("e", b.branch("variable", b.leaf("a"))));
		Assert.assertEquals(expected, tree.getRoot());

	}

	@Test
	public void eite_S_ifathenaelsea() throws ParseFailedException {
		// grammar, goal, input

		final Grammar g = this.expression_if_then_else();
		final String goal = "S";
		final String text = "if a then a else a";

		final ISharedPackedParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		final ISPBranch expected = b.branch("S",
				b.branch("e",
						b.branch("ifthenelse", b.leaf("if"), b.branch("WS", b.leaf("\\s+", " ")),
								b.branch("e", b.branch("variable", b.leaf("a"), b.branch("WS", b.leaf("\\s+", " ")))), b.leaf("then"),
								b.branch("WS", b.leaf("\\s+", " ")), b.branch("e", b.branch("variable", b.leaf("a"), b.branch("WS", b.leaf("\\s+", " ")))),
								b.leaf("else"), b.branch("WS", b.leaf("\\s+", " ")), b.branch("e", b.branch("variable", b.leaf("a"))))));
		Assert.assertEquals(expected, tree.getRoot());

	}

	@Test
	public void eite_S_ifathena() throws ParseFailedException {
		// grammar, goal, input
		final Grammar g = this.expression_if_then_else();
		final String goal = "S";
		final String text = "if a then a";

		final ISharedPackedParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);

		// final ISharedPackedParseTree expected = new ParseTree(b.branch("S",
		// b.branch("e",
		// b.branch("ifthen", b.leaf("if"), b.branch("WS", b.leaf("\\s+", " ")),
		// b.branch("e", b.branch("variable", b.leaf("a"), b.branch("WS", b.leaf("\\s+", " ")))), b.leaf("then"),
		// b.branch("WS", b.leaf("\\s+", " ")), b.branch("e", b.branch("variable", b.leaf("a")))))));
		b.define("S {");
		b.define("  e {");
		b.define("    ifthen {");
		b.define("      'if' WS { '\\s+':' '}");
		b.define("      e { variable { 'a' WS {'\\s+':' '} } }");
		b.define("      'then'  WS { '\\s+':' '}");
		b.define("      e { variable { 'a' } }");
		b.define("    }");
		b.define("  }");
		b.define("}");
		final ISharedPackedParseTree expected = b.buildAndAdd();
		Assert.assertEquals(expected, tree);

	}

	@Test
	public void eite_S_ifAthenAelseifAthenA() throws ParseFailedException {
		// grammar, goal, input
		final Grammar g = this.expression_if_then_else();
		final String goal = "S";
		final String text = "if a then a else if a then a";

		final ISharedPackedParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		b.define("S {");
		b.define("  e {");
		b.define("    ifthenelse {");
		b.define("      'if' WS { '\\s+':' '}");
		b.define("      e { variable { 'a'  WS {'\\s+':' '} } }");
		b.define("      'then'  WS { '\\s+':' '}");
		b.define("      e { variable { 'a' WS {'\\s+':' '} } }");
		b.define("      'else'  WS { '\\s+':' '}");
		b.define("      e {");
		b.define("        ifthen {");
		b.define("          'if' WS { '\\s+':' '}");
		b.define("          e { variable { 'a' WS {'\\s+':' '} } }");
		b.define("          'then'  WS { '\\s+':' '}");
		b.define("          e { variable { 'a' } }");
		b.define("        }");
		b.define("      }");
		b.define("    }");
		b.define("  }");
		b.define("}");
		final ISharedPackedParseTree expected = b.buildAndAdd();

		Assert.assertEquals(expected, tree);

	}

	Grammar statement_if_then_else() {
		final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("S").concatenation(new NonTerminal("statements"));
		b.skip("WS").concatenation(new TerminalPattern("\\s+"));
		b.rule("statements").multi(1, -1, new NonTerminal("statement"));
		b.rule("statement").choice(new TerminalLiteral("return"), new NonTerminal("ifthenelse"), new NonTerminal("ifthen"));
		b.rule("e").priorityChoice(new NonTerminal("variable"), new NonTerminal("multiply"), new NonTerminal("plus"));
		b.rule("ifthen").concatenation(new TerminalLiteral("if"), new NonTerminal("e"), new TerminalLiteral("then"), new NonTerminal("statements"));
		b.rule("ifthenelse").concatenation(new TerminalLiteral("if"), new NonTerminal("e"), new TerminalLiteral("then"), new NonTerminal("statements"),
				new TerminalLiteral("else"), new NonTerminal("statements"));
		b.rule("plus").concatenation(new NonTerminal("e"), new TerminalLiteral("+"), new NonTerminal("e"));
		b.rule("multiply").concatenation(new NonTerminal("e"), new TerminalLiteral("*"), new NonTerminal("e"));
		b.rule("variable").concatenation(new TerminalLiteral("a"));

		return b.get();
	}

	public void t() throws ParseFailedException {
		// grammar, goal, input
		final Grammar g = this.expression_if_then_else();
		final String goal = "S";
		// final String text = "if(i==1) return 1; else if (false) return 2;";
		final String text = "if(i==1) return 1; else if (false) return 2;";

		final ISharedPackedParseTree tree = this.process(g, text, goal);
		Assert.assertNotNull(tree);

	}

	Grammar javaPrimary() {
		final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.skip("WS").concatenation(new TerminalPattern("\\s+"));
		b.rule("block").concatenation(new TerminalLiteral("{"), new NonTerminal("blockStatements"), new TerminalLiteral("}"));
		b.rule("blockStatements").multi(0, -1, new NonTerminal("blockStatement"));
		b.rule("blockStatement").choice(new NonTerminal("block"), new NonTerminal("emptyStatement"), new NonTerminal("expressionStatement"));
		b.rule("emptyStatement").choice(new TerminalLiteral(";"));
		b.rule("expressionStatement").concatenation(new NonTerminal("expression"), new TerminalLiteral(";"));
		b.rule("statement").choice(new NonTerminal("block"), new NonTerminal("block"));
		b.rule("postIncrementExpression").concatenation(new NonTerminal("postfixExpression"), new TerminalLiteral("++"));
		b.rule("postfixExpression").concatenation(new NonTerminal("postfixExpression_group1"), new NonTerminal("postfixExpression_group2"));
		b.rule("postfixExpression_group1").choice(new NonTerminal("primary"), new NonTerminal("expressionName"));
		b.rule("postfixExpression_group2").multi(0, -1, new TerminalLiteral("++"));
		b.rule("primary").choice(new NonTerminal("literal"), new NonTerminal("typeNameClass"), new NonTerminal("unannPrimitiveTypeClass"),
				new NonTerminal("voidClass"), new TerminalLiteral("this"), new NonTerminal("typeNameThis"), new NonTerminal("parenthExpr"));
		b.rule("voidClass").concatenation(new TerminalLiteral("void"), new TerminalLiteral("."), new TerminalLiteral("class"));
		b.rule("typeNameClass").concatenation(new NonTerminal("typeName"), new NonTerminal("multiBracketPairs"), new TerminalLiteral("."),
				new TerminalLiteral("class"));
		b.rule("unannPrimitiveTypeClass").concatenation(new NonTerminal("unannPrimitiveType"), new NonTerminal("multiBracketPairs"), new TerminalLiteral("."),
				new TerminalLiteral("class"));
		b.rule("multiBracketPairs").multi(0, -1, new NonTerminal("bracketPair"));
		b.rule("bracketPair").concatenation(new TerminalLiteral("["), new TerminalLiteral("]"));
		b.rule("typeNameThis").concatenation(new NonTerminal("typeName"), new TerminalLiteral("."), new TerminalLiteral("this"));
		b.rule("parenthExpr").concatenation(new TerminalLiteral("("), new NonTerminal("expression"), new TerminalLiteral(")"));

		b.rule("expression").choice(new NonTerminal("postfixExpression"));

		b.rule("expressionName").priorityChoice(new NonTerminal("Identifier"), new NonTerminal("ambiguousName"));
		b.rule("ambiguousName").choice(new NonTerminal("Identifier"), new NonTerminal("ambiguousName_choice2"));
		b.rule("ambiguousName_choice2").concatenation(new NonTerminal("ambiguousName"), new TerminalLiteral("."), new NonTerminal("Identifier"));
		b.rule("Identifier").choice(new TerminalPattern("[a-zA-Z_][a-zA-Z0-9_]*"));
		b.rule("literal").choice(new TerminalLiteral("1"));
		b.rule("typeName").choice(new NonTerminal("Identifier"));
		b.rule("unannPrimitiveType").choice(new TerminalLiteral("int"), new TerminalLiteral("float"), new TerminalLiteral("boolean"));
		return b.get();
	}

	@Test
	public void postIncrementExpression() throws ParseFailedException {
		// grammar, goal, input
		final Grammar g = this.javaPrimary();
		final String goal = "postIncrementExpression";
		final String text = "i++";

		final ISharedPackedParseTree actual = this.process(g, text, goal);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		b.define("  postIncrementExpression {");
		b.define("    postfixExpression {");
		b.define("      postfixExpression_group1 {");
		b.define("        expressionName {");
		b.define("          Identifier { '[a-zA-Z_][a-zA-Z0-9_]*' : 'i' }");
		b.define("        }");
		b.define("      }");
		b.define("      postfixExpression_group2 { $empty }");
		b.define("    }");
		b.define("    '++'");
		b.define("  }");

		final ISharedPackedParseTree expected = b.buildAndAdd();

		Assert.assertNotNull(actual);
		Assert.assertEquals(expected, actual);
	}

	@Test
	public void primary_arrayClass() throws ParseFailedException {
		// grammar, goal, input
		final Grammar g = this.javaPrimary();
		final String goal = "primary";
		final String text = "MyClass[].class";

		final ISharedPackedParseTree actual = this.process(g, text, goal);

		final ParseTreeBuilder b = this.builder(g, text, goal);

		b.define("    primary {");
		b.define("      typeNameClass {");
		b.define("        typeName {");
		b.define("          Identifier { '[a-zA-Z_][a-zA-Z0-9_]*' : 'MyClass' }");
		b.define("        }");
		b.define("        multiBracketPairs { bracketPair { '[' ']' } }");
		b.define("        '.'");
		b.define("        'class'");
		b.define("      }");
		b.define("    }");

		final ISharedPackedParseTree expected = b.buildAndAdd();

		Assert.assertNotNull(actual);
		Assert.assertEquals(expected, actual);

	}

	@Test
	public void blockStatement() throws ParseFailedException {
		// grammar, goal, input
		final Grammar g = this.javaPrimary();
		final String goal = "blockStatement";
		final String text = "i++;";

		final ISharedPackedParseTree actual = this.process(g, text, goal);

		final ParseTreeBuilder b = this.builder(g, text, goal);
		b.define("blockStatement {");
		b.define("expressionStatement {");
		b.define("  expression {");
		b.define("    postfixExpression {");
		b.define("      postfixExpression_group1 {");
		b.define("        expressionName {");
		b.define("          Identifier { '[a-zA-Z_][a-zA-Z0-9_]*' : 'i' }");
		b.define("        }");
		b.define("      }");
		b.define("      postfixExpression_group2 { '++' }");
		b.define("    }");
		b.define("    ");
		b.define("  }");
		b.define("  ';'");
		b.define("}");
		b.define("}");

		final ISharedPackedParseTree expected = b.buildAndAdd();

		Assert.assertNotNull(actual);
		Assert.assertEquals(expected, actual);

	}
}
