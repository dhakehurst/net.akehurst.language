package net.akehurst.language.processor;

import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.Test;

import net.akehurst.language.core.analyser.UnableToAnalyseExeception;
import net.akehurst.language.core.grammar.GrammarRuleNotFoundException;
import net.akehurst.language.core.parser.IParser;
import net.akehurst.language.core.parser.ParseFailedException;
import net.akehurst.language.core.parser.ParseTreeException;
import net.akehurst.language.core.processor.ILanguageProcessor;
import net.akehurst.language.core.sppt.ISharedPackedParseTree;
import net.akehurst.language.grammar.parser.ParseTreeToInputText;
import net.akehurst.language.ogl.semanticStructure.Grammar;

public class Java8_Tests {

	static OGLanguageProcessor processor;

	static {
		Java8_Tests.getOGLProcessor();
	}

	static OGLanguageProcessor getOGLProcessor() {
		if (null == Java8_Tests.processor) {
			Java8_Tests.processor = new OGLanguageProcessor();
			Java8_Tests.processor.getParser().build();
		}
		return Java8_Tests.processor;
	}

	static ILanguageProcessor javaProcessor;

	static {
		Java8_Tests.getJavaProcessor();
	}

	static ILanguageProcessor getJavaProcessor() {
		if (null == Java8_Tests.javaProcessor) {
			try {
				final FileReader reader = new FileReader(Paths.get("src/test/resources/Java8_all.og").toFile());
				final Grammar javaGrammar = Java8_Tests.getOGLProcessor().process(reader, "grammarDefinition", Grammar.class);
				Java8_Tests.javaProcessor = new LanguageProcessor(javaGrammar, null);
				Java8_Tests.javaProcessor.getParser().build();
			} catch (final IOException e) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			} catch (final ParseFailedException e) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			} catch (final UnableToAnalyseExeception e) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			}

		}
		return Java8_Tests.javaProcessor;
	}

	static ILanguageProcessor getJavaProcessor(final String goalName) {
		try {
			final FileReader reader = new FileReader(Paths.get("src/test/resources/Java8_all.og").toFile());
			final Grammar javaGrammar = Java8_Tests.getOGLProcessor().process(reader, "grammarDefinition", Grammar.class);
			final LanguageProcessor jp = new LanguageProcessor(javaGrammar, null);
			jp.getParser().build();
			return jp;
		} catch (final IOException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} catch (final ParseFailedException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} catch (final UnableToAnalyseExeception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
		return null;
	}

	static String toString(final Path file) {
		try {
			final byte[] bytes = Files.readAllBytes(file);
			final String str = new String(bytes);
			return str;
		} catch (final IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	static ISharedPackedParseTree parse(final String input) throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {

		final ISharedPackedParseTree tree = Java8_Tests.getJavaProcessor().getParser().parse("compilationUnit", new StringReader(input));
		return tree;

	}

	static ISharedPackedParseTree parse(final String goalName, final String input) throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {

		final IParser parser = Java8_Tests.getJavaProcessor().getParser();
		final ISharedPackedParseTree tree = parser.parse(goalName, input);
		return tree;

	}

	@Test
	public void ifThenStatement_2() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {

		final String input = "if(i==1) {return 1;}";

		final ISharedPackedParseTree tree = Java8_Tests.parse("ifThenStatement", input);
		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void ifThenElseStatement_1() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {

		final String input = "if(i==1) return 1; else return 2;";

		final ISharedPackedParseTree tree = Java8_Tests.parse("ifThenElseStatement", input);
		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void ifThenElseStatement_2() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {

		final String input = "if(i==1) {return 1;} else {return 2;}";

		final ISharedPackedParseTree tree = Java8_Tests.parse("ifThenElseStatement", input);
		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void statement_ifThenElseStatement_1() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {

		final String input = "if(i==1) return 1; else return 2;";

		final ISharedPackedParseTree tree = Java8_Tests.parse("statement", input);
		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void statement_ifThenElseStatement_2() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {

		final String input = "if(i==1) {return 1;} else {return 2;}";

		final ISharedPackedParseTree tree = Java8_Tests.parse("statement", input);
		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void ifThenElseIf_1() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {

		final String input = "if(i==1) return 1; else if (false) return 2;";

		final ISharedPackedParseTree tree = Java8_Tests.parse("ifThenElseStatement", input);
		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void ifThenElseIf_2() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {

		final String input = "if(i==1) {return 1;} else if (false) {return 2;}";

		final ISharedPackedParseTree tree = Java8_Tests.parse("ifThenElseStatement", input);
		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void trycatch0() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {

		String input = "try {";
		input += "      if(i==1) return 1;";
		input += "    } catch(E e) {";
		input += "       if(i==1) return 1;";
		input += "    }";
		final ISharedPackedParseTree tree = Java8_Tests.parse("tryStatement", input);
		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void tryfinally0() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {

		String input = "try {";
		input += "      if(i==1) return 1;";
		input += "    } finally {";
		input += "       if(i==1) return 1;";
		input += "    }";
		final ISharedPackedParseTree tree = Java8_Tests.parse("tryStatement", input);
		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void tryfinally1() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {

		String input = "class Test {";
		input += "  void test() {";
		input += "    try {";
		input += "      if(i==1) return 1;";
		input += "    } finally {";
		for (int i = 0; i < 100; ++i) {
			input += "       if(" + i + ") return " + i + ";";
		}
		input += "    }";
		input += "  }";
		input += "}";
		final ISharedPackedParseTree tree = Java8_Tests.parse(input);
		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void tryfinally2() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {

		String input = "class Test {";
		input += "  void test() {";
		input += "    try {";
		input += "      if(i==1) return 1;";
		input += "    } finally {";
		input += "      try {";
		for (int i = 0; i < 100; ++i) {
			input += "       if(" + i + ") return " + i + ";";
		}
		input += "      } finally {";
		input += "      }";
		input += "    }";
		input += "  }";
		input += "}";
		final ISharedPackedParseTree tree = Java8_Tests.parse(input);
		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void multipleFields() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {

		String input = "class Test {";
		input += "  Integer i1;";
		input += "  Integer i2;";
		input += "  Integer i3;";
		input += "  Integer i4;";
		input += "}";
		final ISharedPackedParseTree tree = Java8_Tests.parse(input);
		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void manyFields() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {

		String input = "class Test {";
		for (int i = 0; i < 8; ++i) {
			input += "  Integer i" + i + ";";
		}
		input += "}";
		final ISharedPackedParseTree tree = Java8_Tests.parse(input);
		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void formalParameter() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {

		final String input = "Visitor<E> v";
		ISharedPackedParseTree tree = null;

		tree = Java8_Tests.parse("formalParameter", input);

		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void formalParameters1() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {

		final String input = "Visitor v";
		ISharedPackedParseTree tree = null;

		tree = Java8_Tests.parse("formalParameters", input);

		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void formalParameters2() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {

		final String input = "Visitor v, Type p2";
		ISharedPackedParseTree tree = null;

		tree = Java8_Tests.parse("formalParameters", input);

		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void formalParameters() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {

		final String input = "Visitor<E> v";
		ISharedPackedParseTree tree = null;

		tree = Java8_Tests.parse("formalParameters", input);

		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void typeArguments1() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {

		final String input = "<E>";
		ISharedPackedParseTree tree = null;

		tree = Java8_Tests.parse("typeArguments", input);

		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void typeArguments2() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {

		final String input = "<E,F>";
		ISharedPackedParseTree tree = null;

		tree = Java8_Tests.parse("typeArguments", input);

		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void typeArguments3() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {

		final String input = "<E,F,G>";
		ISharedPackedParseTree tree = null;

		tree = Java8_Tests.parse("typeArguments", input);

		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void ISO8859encoding() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {
		String input = "";
		input += "class T6302184 {";
		input += "  int ������ = 1;";
		input += "}";
		final ISharedPackedParseTree tree = Java8_Tests.parse(input);
		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void classBody() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {

		final String input = "{ int i1; int i2; int i3; int i4; int i5; int i6; int i7; int i8; }";
		ISharedPackedParseTree tree = null;

		tree = Java8_Tests.parse("classBody", input);

		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);

	}

	@Test
	public void methodDeclaration1() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {
		final String input = "void f();";
		final ISharedPackedParseTree tree = Java8_Tests.parse("methodDeclaration", input);
		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void methodDeclaration2() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {
		final String input = "public void f();";
		final ISharedPackedParseTree tree = Java8_Tests.parse("methodDeclaration", input);
		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void methodDeclaration3() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {
		final String input = "public void f(Visitor v);";
		final ISharedPackedParseTree tree = Java8_Tests.parse("methodDeclaration", input);
		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void methodDeclaration4() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {
		final String input = "public abstract void f(Visitor v);";
		final ISharedPackedParseTree tree = Java8_Tests.parse("methodDeclaration", input);
		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void methodDeclaration5() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {
		final String input = "public abstract <T> void f(Visitor v);";
		final ISharedPackedParseTree tree = Java8_Tests.parse("methodDeclaration", input);
		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void methodDeclaration6() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {
		final String input = "public abstract <T> void f(Visitor<T> v);";
		final ISharedPackedParseTree tree = Java8_Tests.parse("methodDeclaration", input);
		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void methodDeclaration7() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {
		final String input = "void f(Visitor<T> v);";
		final ISharedPackedParseTree tree = Java8_Tests.parse("methodDeclaration", input);
		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void methodDeclaration8() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {

		final String input = "public abstract <E extends Throwable> void accept(Visitor<E> v);";

		final ISharedPackedParseTree tree = Java8_Tests.parse("methodDeclaration", input);
		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void multipleMethods() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {

		String input = "class Test {";
		input += "  public abstract <E extends Throwable> void accept(Visitor<E> v);";
		input += "  public abstract <E extends Throwable> void accept(Visitor<E> v);";
		input += "  public abstract <E extends Throwable> void accept(Visitor<E> v);";
		input += "  public abstract <E extends Throwable> void accept(Visitor<E> v);";
		input += "}";
		final ISharedPackedParseTree tree = Java8_Tests.parse(input);
		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);

	}

	@Test
	public void abstractGeneric() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {

		String input = "class Test {";
		input += "  /** Visit this tree with a given visitor.";
		input += "  */";
		input += "  public abstract <E extends Throwable> void accept(Visitor<E> v) throws E;";
		input += "}";
		final ISharedPackedParseTree tree = Java8_Tests.parse(input);
		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void genericVisitorMethod() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {

		String input = "class Test {";
		input += "  /** A generic visitor class for trees.";
		input += "  */";
		input += "  public static abstract class Visitor<E extends Throwable> {";
		input += "      public void visitTree(Tree that)                   throws E { assert false; }";
		input += "  }";
		input += "}";
		final ISharedPackedParseTree tree = Java8_Tests.parse(input);
		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void tree() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {

		String input = "class Test {";
		input += "  /** Visit this tree with a given visitor.";
		input += "  */";
		input += "  public abstract <E extends Throwable> void accept(Visitor<E> v) throws E;";
		input += "";
		input += "  /** A generic visitor class for trees.";
		input += "  */";
		input += "  public static abstract class Visitor<E extends Throwable> {";
		input += "      public void visitTree(Tree that)                   throws E { assert false; }";
		input += "  }";
		input += "}";
		final ISharedPackedParseTree tree = Java8_Tests.parse(input);

		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void stringLiteral() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {
		String input = "";
		input += "\"xxxx\"";
		final ISharedPackedParseTree tree = Java8_Tests.parse("StringLiteral", input);
		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void localStringVariableDeclarationStatement() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {
		String input = "";
		input += "String s = \"xxxx\";";
		final ISharedPackedParseTree tree = Java8_Tests.parse("localVariableDeclarationStatement", input);
		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void stringMemberInitialised() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {
		String input = "";
		input += "public class Test {";
		input += "  String s = \"xxxx\";";
		input += "}";
		final ISharedPackedParseTree tree = Java8_Tests.parse(input);
		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void StringLiteral1() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {

		final String input = "\"wrong number of args\"";

		final ISharedPackedParseTree tree = Java8_Tests.parse("StringLiteral", input);
		Assert.assertNotNull("null==tree", tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void String_expression() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {

		final String input = "\"file \" + file + \" found unexpectedly\"";

		final ISharedPackedParseTree tree = Java8_Tests.parse("expression", input);
		Assert.assertNotNull("null==tree", tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void throwStatement() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {

		final String input = "throw new Error(\"file \" + file + \" found unexpectedly\");";

		final ISharedPackedParseTree tree = Java8_Tests.parse("throwStatement", input);
		Assert.assertNotNull("null==tree", tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void ifThenStatement() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {

		final String input = "if (u != \"file \") throw new Error(\"file \" + file + \" found unexpectedly\");";

		final ISharedPackedParseTree tree = Java8_Tests.parse("ifThenStatement", input);
		Assert.assertNotNull("null==tree", tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void T6257443_1() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {

		String input = "";
		input += "import java.net.URL;";
		input += "public class T6257443 {";
		input += "  public static void main(String[] args) {";
		input += "  }";
		input += "}";
		final ISharedPackedParseTree tree = Java8_Tests.parse(input);
		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void T6257443_2() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {

		String input = "";
		input += "import java.net.URL;";
		input += "public class T6257443 {";
		input += "  public static void main(String[] args) {";
		input += "    if (args.length != 2)";
		input += "       throw new Error(\"wrong number of args\");";
		input += "  }";
		input += "}";
		final ISharedPackedParseTree tree = Java8_Tests.parse(input);
		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void T6257443_3() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {

		String input = "";
		input += "import java.net.URL;";
		input += "public class T6257443 {";
		input += "  public static void main(String[] args) {";
		input += "    if (args.length != 2)";
		input += "       throw new Error(\"wrong number of args\");";
		input += "    String state = args[0];";
		input += "    String file = args[1];";
		input += "  }";
		input += "}";
		final ISharedPackedParseTree tree = Java8_Tests.parse(input);
		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void T6257443_4() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {

		String input = "";
		input += "import java.net.URL;";
		input += "public class T6257443 {";
		input += "  public static void main(String[] args) {";
		input += "    if (args.length != 2)";
		input += "       throw new Error(\"wrong number of args\");";
		input += "    String state = args[0];";
		input += "    String file = args[1];";
		input += "    if (state.equals(\"-no\")) {";
		input += "       URL u = find(file);";
		input += "       if (u != null) throw new Error(\"file \" + file + \" found unexpectedly\");";
		input += "    }";
		input += "  }";
		input += "}";
		final ISharedPackedParseTree tree = Java8_Tests.parse(input);
		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void T6257443_5() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {

		String input = "";
		input += "{";
		input += "    if (1)";
		input += "       return 1;";

		input += "    if (1) {";
		input += "       URL uuu = f(1);";
		input += "       if (1) return 1;";
		input += "    } else  {";
		input += "    }";
		input += "}";

		final ISharedPackedParseTree tree = Java8_Tests.parse("block", input);
		Assert.assertNotNull("null==tree", tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void T6257443_5_1() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {

		String input = "";
		input += "{";
		input += "    if (s.equals(\"-no\")) {";
		input += "       throw new E(\"fie \" + 1 + \"\");";
		input += "    }";
		input += "}";

		final ISharedPackedParseTree tree = Java8_Tests.parse("block", input);
		Assert.assertNotNull("null==tree", tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void T6257443_5_2() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {

		String input = "";
		input += "{";
		input += "    if (args.length != 2)";
		input += "       throw new Error(\"wrong number of args\");";
		input += "    String state = args[0];";
		input += "    String file = args[1];";
		input += "    if (state.equals(\"-no\")) {";
		input += "       URL u = find(file);";
		input += "       if (u != null) throw new Error(\"file \" + file + \" found unexpectedly\");";
		input += "    }";
		input += "}";

		final ISharedPackedParseTree tree = Java8_Tests.parse("block", input);
		Assert.assertNotNull("null==tree", tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void T6257443_6() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {

		String input = "";
		input += "import java.net.URL;";
		input += "public class T6257443 {";
		input += "  public static void main(String[] args) {";
		input += "    if (args.length != 2)";
		input += "       throw new Error(\"wrong number of args\");";
		input += "    String state = args[0];";
		input += "    String file = args[1];";
		input += "    if (state.equals(\"-no\")) {";
		input += "       URL u = find(file);";
		input += "       if (u != null) throw new Error(\"file \" + file + \" found unexpectedly\");";
		input += "    } else {";
		input += "    }";
		input += "  }";
		input += "}";
		final ISharedPackedParseTree tree = Java8_Tests.parse(input);
		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void T6257443() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {

		String input = "";
		input += "import java.net.URL;";
		input += "public class T6257443 {";
		input += "  public static void main(String[] args) {";
		input += "    if (args.length != 2)";
		input += "       throw new Error(\"wrong number of args\");";
		input += "    String state = args[0];";
		input += "    String file = args[1];";
		input += "    if (state.equals(\"-no\")) {";
		input += "       URL u = find(file);";
		input += "       if (u != null) throw new Error(\"file \" + file + \" found unexpectedly\");";
		input += "    } else if (state.equals(\"-yes\")) {";
		input += "       URL u = find(file);";
		input += "       if (u == null) throw new Error(\"file \" + file + \" not found\");";
		input += "    } else throw new Error(\"bad args\");";
		input += "  }";
		input += "}";
		final ISharedPackedParseTree tree = Java8_Tests.parse(input);
		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void preIncrementExpression() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {
		final String input = "++i";

		final ISharedPackedParseTree tree = Java8_Tests.parse("preIncrementExpression", input);
		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void postfixExpression() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {
		final String input = "i";

		final ISharedPackedParseTree tree = Java8_Tests.parse("postfixExpression", input);
		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void postfixExpressionpp() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {
		final String input = "i++";

		final ISharedPackedParseTree tree = Java8_Tests.parse("postfixExpression", input);
		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void postIncrementExpression() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {
		final String input = "i++ ++";

		final ISharedPackedParseTree tree = Java8_Tests.parse("postIncrementExpression", input);
		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void primary_literal() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {
		final String input = "1";

		final ISharedPackedParseTree tree = Java8_Tests.parse("primary", input);
		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void primary_arrayClass() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {
		final String input = "MyClass[].class";

		final ISharedPackedParseTree tree = Java8_Tests.parse("primary", input);
		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void primary_voidClass() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {
		final String input = "void.class";

		final ISharedPackedParseTree tree = Java8_Tests.parse("primary", input);
		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void primary_this() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {
		final String input = "this";

		final ISharedPackedParseTree tree = Java8_Tests.parse("primary", input);
		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void primary_typeNameThis() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {
		final String input = "MyClass.this";

		final ISharedPackedParseTree tree = Java8_Tests.parse("primary", input);
		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void primary_parenth_expression() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {
		final String input = "(1 + 1)";

		final ISharedPackedParseTree tree = Java8_Tests.parse("primary", input);
		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void primary_classInstanceCreationExpression() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {
		final String input = "new MyClass()";

		final ISharedPackedParseTree tree = Java8_Tests.parse("primary", input);
		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void primary_fieldAccess() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {
		final String input = "super.field";

		final ISharedPackedParseTree tree = Java8_Tests.parse("primary", input);
		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void expression() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {
		final String input = "i++";

		final ISharedPackedParseTree tree = Java8_Tests.parse("expression", input);
		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void blockStatement() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {
		final String input = "i++;";

		final ISharedPackedParseTree tree = Java8_Tests.parse("blockStatement", input);
		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void blockStatements() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {
		final String input = "i++;";

		final ISharedPackedParseTree tree = Java8_Tests.parse("blockStatements", input);
		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void switchLabel() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {
		String input = "";
		input += "  case 1:";
		final ISharedPackedParseTree tree = Java8_Tests.parse("switchLabel", input);
		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void switchBlockStatementGroup() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {
		final String input = "case 1 : i++;";

		final ISharedPackedParseTree tree = Java8_Tests.parse("switchBlockStatementGroup", input);
		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void switchBlock() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {
		final String input = "{case 1:i++;}";

		final ISharedPackedParseTree tree = Java8_Tests.parse("switchBlock", input);
		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void switchStatement1() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {
		String input = "";
		input += "switch (i) {";
		input += "  case 1: ";
		input += "  default:";
		input += "}";

		final ISharedPackedParseTree tree = Java8_Tests.parse("switchStatement", input);
		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void switchStatement() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {
		String input = "";
		input += "switch (i) {";
		input += "  case 1:";
		input += "    i++;";
		input += "    // fallthrough" + System.lineSeparator();
		input += "  default:";
		input += "}";

		final ISharedPackedParseTree tree = Java8_Tests.parse("switchStatement", input);
		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void T6304921() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {
		String input = "";
		input += "import java.util.ArrayList;";
		input += "import java.util.List;";
		input += "class T6304921 {";
		input += "    void m1(int i) {";
		input += "      switch (i) {";
		input += "        case 1:";
		input += "           i++;";
		input += "           // fallthrough" + System.lineSeparator();
		input += "        default:";
		input += "      }";
		input += "    }";
		input += "    void m2() {";
		input += "      List<Integer> list = new ArrayList();";
		input += "    }";
		input += "}";
		input += "class X {";
		input += "    void m1() {";
		input += "      System.err.println(\"abc\"); // name not found" + System.lineSeparator();
		input += "    }";
		input += "    boolean m2() {";
		input += "      return 123 + true; // bad binary expression" + System.lineSeparator();
		input += "    }";
		input += "}";
		final ISharedPackedParseTree tree = Java8_Tests.parse(input);
		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void singleLineComment() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {
		String input = "";
		input += "//single line comment" + System.lineSeparator();
		input += "class Test {";
		input += "}";
		final ISharedPackedParseTree tree = Java8_Tests.parse(input);
		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void HexFloatLiteral() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {
		final String input = "0Xfffffffffffffbcp-59D";

		final ISharedPackedParseTree tree = Java8_Tests.parse("HexadecimalFloatingPointLiteral", input);

		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void unaryExpression_HexFloatLiteral() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {
		final String input = "+0Xfffffffffffffbcp-59D";

		final ISharedPackedParseTree tree = Java8_Tests.parse("unaryExpression", input);

		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void HexFloatLiteral_in_check_call() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {
		final String input = "check(+0Xfffffffffffffbcp-59D, Double.parseDouble(\"+0Xfffffffffffffbcp-59D\"));";

		final ISharedPackedParseTree tree = Java8_Tests.parse("blockStatement", input);

		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void compilationUnit() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {
		final String input = "package p1; public class A { public static char c = 'A'; }";
		final ISharedPackedParseTree tree = Java8_Tests.parse("compilationUnit", input);

		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void classDeclaration() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {
		final String input = "public class A { public static char c = 'A'; }";
		final ISharedPackedParseTree tree = Java8_Tests.parse("classDeclaration", input);

		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void classDeclaration2() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {
		final String input = "public class A { }";
		final ISharedPackedParseTree tree = Java8_Tests.parse("classDeclaration", input);

		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void classBody2() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {
		final String input = "{ public static char c = 'A'; }";
		final ISharedPackedParseTree tree = Java8_Tests.parse("classBody", input);

		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void fieldDeclaration() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {
		final String input = "public static char c = 'A';";
		final ISharedPackedParseTree tree = Java8_Tests.parse("fieldDeclaration", input);

		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void variableDeclarator() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {
		final String input = "c = 'A'";
		final ISharedPackedParseTree tree = Java8_Tests.parse("variableDeclarator", input);

		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void CharacterLiteral() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {
		final String input = "'A'";
		final ISharedPackedParseTree tree = Java8_Tests.parse("CharacterLiteral", input);

		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void SingleCharacter() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {
		final String input = "A";
		final ISharedPackedParseTree tree = Java8_Tests.parse("SingleCharacter", input);

		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void variableDeclaratorId() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {
		final String input = "c";
		final ISharedPackedParseTree tree = Java8_Tests.parse("variableDeclaratorId", input);

		Assert.assertNotNull(tree);

		final ParseTreeToInputText x = new ParseTreeToInputText();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void test1() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {

		final String queryStr = "@An() class An {  }";
		final String grammarRule = "compilationUnit";
		// Log.on = true;
		final ISharedPackedParseTree tree = Java8_Tests.parse(grammarRule, queryStr);
		Assert.assertNotNull(tree);
		// final String resultStr = Java8_Tests2.clean(tree.asString());
		// Assert.assertEquals(queryStr, resultStr);

	}
}