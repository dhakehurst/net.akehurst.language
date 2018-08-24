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
import net.akehurst.language.parser.sppf.ParseTreeBuilder;
import net.akehurst.language.ogl.semanticStructure.GrammarDefault;
import net.akehurst.language.ogl.semanticStructure.GrammarBuilderDefault;
import net.akehurst.language.ogl.semanticStructure.NamespaceDefault;
import net.akehurst.language.ogl.semanticStructure.NonTerminalDefault;
import net.akehurst.language.ogl.semanticStructure.TerminalLiteralDefault;
import net.akehurst.language.ogl.semanticStructure.TerminalPatternDefault;

public class test_Parser_Expressions extends AbstractParser_Test {

    GrammarDefault expression_plus_multiply() {
        final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
        b.rule("S").concatenation(new NonTerminalDefault("e"));
        b.rule("e").priorityChoice(new NonTerminalDefault("variable"), new NonTerminalDefault("multiply"), new NonTerminalDefault("plus"));
        b.rule("plus").concatenation(new NonTerminalDefault("e"), new TerminalLiteralDefault("+"), new NonTerminalDefault("e"));
        b.rule("multiply").concatenation(new NonTerminalDefault("e"), new TerminalLiteralDefault("*"), new NonTerminalDefault("e"));
        b.rule("variable").concatenation(new TerminalLiteralDefault("a"));

        return b.get();
    }

    @Test
    public void emp_S_a() {
        // grammar, goal, input
        try {
            final GrammarDefault g = this.expression_plus_multiply();
            final String goal = "S";
            final String text = "a";

            final SharedPackedParseTree tree = this.process(g, text, goal);
            Assert.assertNotNull(tree);

            final ParseTreeBuilder b = this.builder(g, text, goal);
            final SPPTBranch expected = b.branch("S", b.branch("e", b.branch("variable", b.leaf("a"))));
            Assert.assertEquals(expected, tree.getRoot());

        } catch (final ParseFailedException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void emp_S_apa() {
        // grammar, goal, input
        try {
            final GrammarDefault g = this.expression_plus_multiply();
            final String goal = "S";
            final String text = "a+a";

            final SharedPackedParseTree tree = this.process(g, text, goal);
            Assert.assertNotNull(tree);

            final ParseTreeBuilder b = this.builder(g, text, goal);
            ;
            final SPPTBranch expected = b.branch("S", b.branch("e",
                    b.branch("plus", b.branch("e", b.branch("variable", b.leaf("a"))), b.leaf("+"), b.branch("e", b.branch("variable", b.leaf("a"))))));
            Assert.assertEquals(expected, tree.getRoot());

        } catch (final ParseFailedException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Test
    public void emp_S_ama() throws ParseFailedException {
        // grammar, goal, input

        final GrammarDefault g = this.expression_plus_multiply();
        final String goal = "S";
        final String text = "a*a";

        final SharedPackedParseTree tree = this.process(g, text, goal);
        Assert.assertNotNull(tree);

        final ParseTreeBuilder b = this.builder(g, text, goal);

        final SPPTBranch expected = b.branch("S", b.branch("e",
                b.branch("multiply", b.branch("e", b.branch("variable", b.leaf("a"))), b.leaf("*"), b.branch("e", b.branch("variable", b.leaf("a"))))));
        Assert.assertEquals(expected, tree.getRoot());

    }

    @Test
    public void emp_S_apama() throws ParseFailedException {
        // grammar, goal, input

        final GrammarDefault g = this.expression_plus_multiply();
        final String goal = "S";
        final String text = "a+a*a";

        final SharedPackedParseTree tree = this.process(g, text, goal);
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
        final SharedPackedParseTree expected = b.buildAndAdd();

        Assert.assertEquals(expected.toStringAll(), tree.toStringAll());

    }

    @Test
    public void emp_S_amapa() throws ParseFailedException {
        // grammar, goal, input

        final GrammarDefault g = this.expression_plus_multiply();
        final String goal = "S";
        final String text = "a*a+a";

        final SharedPackedParseTree tree = this.process(g, text, goal);
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
        final SharedPackedParseTree expected = b.buildAndAdd();

        Assert.assertEquals(expected.toStringAll(), tree.toStringAll());

    }

    GrammarDefault expression_if_then_else() {
        final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
        b.rule("S").concatenation(new NonTerminalDefault("e"));
        b.skip("WS").concatenation(new TerminalPatternDefault("\\s+"));
        b.rule("e").priorityChoice(new NonTerminalDefault("variable"), new NonTerminalDefault("multiply"), new NonTerminalDefault("plus"), new NonTerminalDefault("ifthenelse"),
                new NonTerminalDefault("ifthen"));
        b.rule("ifthen").concatenation(new TerminalLiteralDefault("if"), new NonTerminalDefault("e"), new TerminalLiteralDefault("then"), new NonTerminalDefault("e"));
        b.rule("ifthenelse").concatenation(new TerminalLiteralDefault("if"), new NonTerminalDefault("e"), new TerminalLiteralDefault("then"), new NonTerminalDefault("e"),
                new TerminalLiteralDefault("else"), new NonTerminalDefault("e"));
        b.rule("plus").concatenation(new NonTerminalDefault("e"), new TerminalLiteralDefault("+"), new NonTerminalDefault("e"));
        b.rule("multiply").concatenation(new NonTerminalDefault("e"), new TerminalLiteralDefault("*"), new NonTerminalDefault("e"));
        b.rule("variable").concatenation(new TerminalLiteralDefault("a"));

        return b.get();
    }

    @Test
    public void eite_S_a() throws ParseFailedException {
        // grammar, goal, input

        final GrammarDefault g = this.expression_if_then_else();
        final String goal = "S";
        final String text = "a";

        final SharedPackedParseTree tree = this.process(g, text, goal);
        Assert.assertNotNull(tree);

        final ParseTreeBuilder b = this.builder(g, text, goal);
        ;
        final SPPTBranch expected = b.branch("S", b.branch("e", b.branch("variable", b.leaf("a"))));
        Assert.assertEquals(expected, tree.getRoot());

    }

    @Test
    public void eite_S_ifathenaelsea() throws ParseFailedException {
        // grammar, goal, input

        final GrammarDefault g = this.expression_if_then_else();
        final String goal = "S";
        final String text = "if a then a else a";

        final SharedPackedParseTree tree = this.process(g, text, goal);
        Assert.assertNotNull(tree);

        final ParseTreeBuilder b = this.builder(g, text, goal);
        final SPPTBranch expected = b.branch("S",
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
        final GrammarDefault g = this.expression_if_then_else();
        final String goal = "S";
        final String text = "if a then a";

        final SharedPackedParseTree tree = this.process(g, text, goal);
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
        final SharedPackedParseTree expected = b.buildAndAdd();
        Assert.assertEquals(expected, tree);

    }

    @Test
    public void eite_S_ifAthenAelseifAthenA() throws ParseFailedException {
        // grammar, goal, input
        final GrammarDefault g = this.expression_if_then_else();
        final String goal = "S";
        final String text = "if a then a else if a then a";

        final SharedPackedParseTree tree = this.process(g, text, goal);
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
        final SharedPackedParseTree expected = b.buildAndAdd();

        Assert.assertEquals(expected, tree);

    }

    GrammarDefault statement_if_then_else() {
        final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
        b.rule("S").concatenation(new NonTerminalDefault("statements"));
        b.skip("WS").concatenation(new TerminalPatternDefault("\\s+"));
        b.rule("statements").multi(1, -1, new NonTerminalDefault("statement"));
        b.rule("statement").choice(new TerminalLiteralDefault("return"), new NonTerminalDefault("ifthenelse"), new NonTerminalDefault("ifthen"));
        b.rule("e").priorityChoice(new NonTerminalDefault("variable"), new NonTerminalDefault("multiply"), new NonTerminalDefault("plus"));
        b.rule("ifthen").concatenation(new TerminalLiteralDefault("if"), new NonTerminalDefault("e"), new TerminalLiteralDefault("then"), new NonTerminalDefault("statements"));
        b.rule("ifthenelse").concatenation(new TerminalLiteralDefault("if"), new NonTerminalDefault("e"), new TerminalLiteralDefault("then"), new NonTerminalDefault("statements"),
                new TerminalLiteralDefault("else"), new NonTerminalDefault("statements"));
        b.rule("plus").concatenation(new NonTerminalDefault("e"), new TerminalLiteralDefault("+"), new NonTerminalDefault("e"));
        b.rule("multiply").concatenation(new NonTerminalDefault("e"), new TerminalLiteralDefault("*"), new NonTerminalDefault("e"));
        b.rule("variable").concatenation(new TerminalLiteralDefault("a"));

        return b.get();
    }

    public void t() throws ParseFailedException {
        // grammar, goal, input
        final GrammarDefault g = this.expression_if_then_else();
        final String goal = "S";
        // final String text = "if(i==1) return 1; else if (false) return 2;";
        final String text = "if(i==1) return 1; else if (false) return 2;";

        final SharedPackedParseTree tree = this.process(g, text, goal);
        Assert.assertNotNull(tree);

    }

    GrammarDefault javaPrimary() {
        final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
        b.skip("WS").concatenation(new TerminalPatternDefault("\\s+"));
        b.rule("block").concatenation(new TerminalLiteralDefault("{"), new NonTerminalDefault("blockStatements"), new TerminalLiteralDefault("}"));
        b.rule("blockStatements").multi(0, -1, new NonTerminalDefault("blockStatement"));
        b.rule("blockStatement").choice(new NonTerminalDefault("block"), new NonTerminalDefault("emptyStatement"), new NonTerminalDefault("expressionStatement"));
        b.rule("emptyStatement").choice(new TerminalLiteralDefault(";"));
        b.rule("expressionStatement").concatenation(new NonTerminalDefault("expression"), new TerminalLiteralDefault(";"));
        b.rule("statement").choice(new NonTerminalDefault("block"), new NonTerminalDefault("block"));
        b.rule("postIncrementExpression").concatenation(new NonTerminalDefault("postfixExpression"), new TerminalLiteralDefault("++"));
        b.rule("postfixExpression").concatenation(new NonTerminalDefault("postfixExpression_group1"), new NonTerminalDefault("postfixExpression_group2"));
        b.rule("postfixExpression_group1").choice(new NonTerminalDefault("primary"), new NonTerminalDefault("expressionName"));
        b.rule("postfixExpression_group2").multi(0, -1, new TerminalLiteralDefault("++"));
        b.rule("primary").choice(new NonTerminalDefault("literal"), new NonTerminalDefault("typeNameClass"), new NonTerminalDefault("unannPrimitiveTypeClass"),
                new NonTerminalDefault("voidClass"), new TerminalLiteralDefault("this"), new NonTerminalDefault("typeNameThis"), new NonTerminalDefault("parenthExpr"));
        b.rule("voidClass").concatenation(new TerminalLiteralDefault("void"), new TerminalLiteralDefault("."), new TerminalLiteralDefault("class"));
        b.rule("typeNameClass").concatenation(new NonTerminalDefault("typeName"), new NonTerminalDefault("multiBracketPairs"), new TerminalLiteralDefault("."),
                new TerminalLiteralDefault("class"));
        b.rule("unannPrimitiveTypeClass").concatenation(new NonTerminalDefault("unannPrimitiveType"), new NonTerminalDefault("multiBracketPairs"), new TerminalLiteralDefault("."),
                new TerminalLiteralDefault("class"));
        b.rule("multiBracketPairs").multi(0, -1, new NonTerminalDefault("bracketPair"));
        b.rule("bracketPair").concatenation(new TerminalLiteralDefault("["), new TerminalLiteralDefault("]"));
        b.rule("typeNameThis").concatenation(new NonTerminalDefault("typeName"), new TerminalLiteralDefault("."), new TerminalLiteralDefault("this"));
        b.rule("parenthExpr").concatenation(new TerminalLiteralDefault("("), new NonTerminalDefault("expression"), new TerminalLiteralDefault(")"));

        b.rule("expression").choice(new NonTerminalDefault("postfixExpression"));

        b.rule("expressionName").priorityChoice(new NonTerminalDefault("Identifier"), new NonTerminalDefault("ambiguousName"));
        b.rule("ambiguousName").choice(new NonTerminalDefault("Identifier"), new NonTerminalDefault("ambiguousName_choice2"));
        b.rule("ambiguousName_choice2").concatenation(new NonTerminalDefault("ambiguousName"), new TerminalLiteralDefault("."), new NonTerminalDefault("Identifier"));
        b.rule("Identifier").choice(new TerminalPatternDefault("[a-zA-Z_][a-zA-Z0-9_]*"));
        b.rule("literal").choice(new TerminalLiteralDefault("1"));
        b.rule("typeName").choice(new NonTerminalDefault("Identifier"));
        b.rule("unannPrimitiveType").choice(new TerminalLiteralDefault("int"), new TerminalLiteralDefault("float"), new TerminalLiteralDefault("boolean"));
        return b.get();
    }

    @Test
    public void postIncrementExpression() throws ParseFailedException {
        // grammar, goal, input
        final GrammarDefault g = this.javaPrimary();
        final String goal = "postIncrementExpression";
        final String text = "i++";

        final SharedPackedParseTree actual = this.process(g, text, goal);

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

        final SharedPackedParseTree expected = b.buildAndAdd();

        Assert.assertNotNull(actual);
        Assert.assertEquals(expected, actual);
    }

    @Test
    public void primary_arrayClass() throws ParseFailedException {
        // grammar, goal, input
        final GrammarDefault g = this.javaPrimary();
        final String goal = "primary";
        final String text = "MyClass[].class";

        final SharedPackedParseTree actual = this.process(g, text, goal);

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

        final SharedPackedParseTree expected = b.buildAndAdd();

        Assert.assertNotNull(actual);
        Assert.assertEquals(expected, actual);

    }

    @Test
    public void blockStatement() throws ParseFailedException {
        // grammar, goal, input
        final GrammarDefault g = this.javaPrimary();
        final String goal = "blockStatement";
        final String text = "i++;";

        final SharedPackedParseTree actual = this.process(g, text, goal);

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

        final SharedPackedParseTree expected = b.buildAndAdd();

        Assert.assertNotNull(actual);
        Assert.assertEquals(expected, actual);

    }
}
