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
import net.akehurst.language.core.grammar.RuleNotFoundException;
import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.core.parser.IParser;
import net.akehurst.language.core.parser.ParseFailedException;
import net.akehurst.language.core.parser.ParseTreeException;
import net.akehurst.language.core.processor.ILanguageProcessor;
import net.akehurst.language.grammar.parser.ParseTreeToString;
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

	String toString(final Path file) {
		try {
			final byte[] bytes = Files.readAllBytes(file);
			final String str = new String(bytes);
			return str;
		} catch (final IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	static IParseTree parse(final String input) {
		try {

			final IParseTree tree = Java8_Tests.getJavaProcessor().getParser().parse("compilationUnit", new StringReader(input));
			return tree;
		} catch (final ParseFailedException e) {
			return null;// e.getLongestMatch();
		} catch (final ParseTreeException e) {
			e.printStackTrace();
		} catch (final RuleNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	static IParseTree parse(final String goalName, final String input) {
		try {
			final IParser parser = Java8_Tests.getJavaProcessor().getParser();
			final IParseTree tree = parser.parse(goalName, new StringReader(input));
			return tree;
		} catch (final ParseFailedException e) {
			return null;// e.getLongestMatch();
		} catch (final Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Test
	public void emptyCompilationUnit() {

		final String input = "";
		final IParseTree tree = Java8_Tests.parse(input);
		Assert.assertNotNull(tree);

		final ParseTreeToString x = new ParseTreeToString();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void ifReturn() {

		String input = "class Test {";
		input += "void test() {";
		for (int i = 0; i < 1; ++i) {
			input += "  if(" + i + ") return " + i + ";";
		}
		input += "}";
		input += "}";
		final IParseTree tree = Java8_Tests.parse(input);
		Assert.assertNotNull(tree);

		final ParseTreeToString x = new ParseTreeToString();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void ifThenStatement_1() {

		final String input = "if(i==1) return 1;";

		final IParseTree tree = Java8_Tests.parse("ifThenStatement", input);
		Assert.assertNotNull(tree);

		final ParseTreeToString x = new ParseTreeToString();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void ifThenStatement_2() {

		final String input = "if(i==1) {return 1;}";

		final IParseTree tree = Java8_Tests.parse("ifThenStatement", input);
		Assert.assertNotNull(tree);

		final ParseTreeToString x = new ParseTreeToString();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void ifThenElseStatement_1() {

		final String input = "if(i==1) return 1; else return 2;";

		final IParseTree tree = Java8_Tests.parse("ifThenElseStatement", input);
		Assert.assertNotNull(tree);

		final ParseTreeToString x = new ParseTreeToString();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void ifThenElseStatement_2() {

		final String input = "if(i==1) {return 1;} else {return 2;}";

		final IParseTree tree = Java8_Tests.parse("ifThenElseStatement", input);
		Assert.assertNotNull(tree);

		final ParseTreeToString x = new ParseTreeToString();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void statement_ifThenElseStatement_1() {

		final String input = "if(i==1) return 1; else return 2;";

		final IParseTree tree = Java8_Tests.parse("statement", input);
		Assert.assertNotNull(tree);

		final ParseTreeToString x = new ParseTreeToString();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void statement_ifThenElseStatement_2() {

		final String input = "if(i==1) {return 1;} else {return 2;}";

		final IParseTree tree = Java8_Tests.parse("statement", input);
		Assert.assertNotNull(tree);

		final ParseTreeToString x = new ParseTreeToString();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void ifThenElseIf_1() {

		final String input = "if(i==1) return 1; else if (false) return 2;";

		final IParseTree tree = Java8_Tests.parse("ifThenElseStatement", input);
		Assert.assertNotNull(tree);

		final ParseTreeToString x = new ParseTreeToString();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void ifThenElseIf_2() {

		final String input = "if(i==1) {return 1;} else if (false) {return 2;}";

		final IParseTree tree = Java8_Tests.parse("ifThenElseStatement", input);
		Assert.assertNotNull(tree);

		final ParseTreeToString x = new ParseTreeToString();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void trycatch0() {

		String input = "try {";
		input += "      if(i==1) return 1;";
		input += "    } catch(E e) {";
		input += "       if(i==1) return 1;";
		input += "    }";
		final IParseTree tree = Java8_Tests.parse("tryStatement", input);
		Assert.assertNotNull(tree);

		final ParseTreeToString x = new ParseTreeToString();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void tryfinally0() {

		String input = "try {";
		input += "      if(i==1) return 1;";
		input += "    } finally {";
		input += "       if(i==1) return 1;";
		input += "    }";
		final IParseTree tree = Java8_Tests.parse("tryStatement", input);
		Assert.assertNotNull(tree);

		final ParseTreeToString x = new ParseTreeToString();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void tryfinally1() {

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
		final IParseTree tree = Java8_Tests.parse(input);
		Assert.assertNotNull(tree);

		final ParseTreeToString x = new ParseTreeToString();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void tryfinally2() {

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
		final IParseTree tree = Java8_Tests.parse(input);
		Assert.assertNotNull(tree);

		final ParseTreeToString x = new ParseTreeToString();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void multipleFields() {

		String input = "class Test {";
		input += "  Integer i1;";
		input += "  Integer i2;";
		input += "  Integer i3;";
		input += "  Integer i4;";
		input += "}";
		final IParseTree tree = Java8_Tests.parse(input);
		Assert.assertNotNull(tree);

		final ParseTreeToString x = new ParseTreeToString();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void manyFields() {

		String input = "class Test {";
		for (int i = 0; i < 8; ++i) {
			input += "  Integer i" + i + ";";
		}
		input += "}";
		final IParseTree tree = Java8_Tests.parse(input);
		Assert.assertNotNull(tree);

		final ParseTreeToString x = new ParseTreeToString();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void formalParameter() {

		final String input = "Visitor<E> v";
		IParseTree tree = null;
		try {
			tree = Java8_Tests.parse("formalParameter", input);
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
		Assert.assertNotNull(tree);

		final ParseTreeToString x = new ParseTreeToString();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void formalParameters1() {

		final String input = "Visitor v";
		IParseTree tree = null;
		try {
			tree = Java8_Tests.parse("formalParameters", input);
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
		Assert.assertNotNull(tree);

		final ParseTreeToString x = new ParseTreeToString();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void formalParameters2() {

		final String input = "Visitor v, Type p2";
		IParseTree tree = null;
		try {
			tree = Java8_Tests.parse("formalParameters", input);
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
		Assert.assertNotNull(tree);

		final ParseTreeToString x = new ParseTreeToString();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void formalParameters() {

		final String input = "Visitor<E> v";
		IParseTree tree = null;
		try {
			tree = Java8_Tests.parse("formalParameters", input);
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
		Assert.assertNotNull(tree);

		final ParseTreeToString x = new ParseTreeToString();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void typeArguments() {

		final String input = "<E>";
		IParseTree tree = null;
		try {
			tree = Java8_Tests.parse("typeArguments", input);
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
		Assert.assertNotNull(tree);

		final ParseTreeToString x = new ParseTreeToString();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void ISO8859encoding() {
		String input = "";
		input += "class T6302184 {";
		input += "  int ������ = 1;";
		input += "}";
		final IParseTree tree = Java8_Tests.parse(input);
		Assert.assertNotNull(tree);

		final ParseTreeToString x = new ParseTreeToString();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void classBody() {

		final String input = "{ int i1; int i2; int i3; int i4; int i5; int i6; int i7; int i8; }";
		IParseTree tree = null;
		try {
			tree = Java8_Tests.parse("classBody", input);
		} catch (final Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
		Assert.assertNotNull(tree);

		final ParseTreeToString x = new ParseTreeToString();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);

	}

	@Test
	public void methodDeclaration1() {
		final String input = "void f();";
		final IParseTree tree = Java8_Tests.parse("methodDeclaration", input);
		Assert.assertNotNull(tree);

		final ParseTreeToString x = new ParseTreeToString();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void methodDeclaration2() {
		final String input = "public void f();";
		final IParseTree tree = Java8_Tests.parse("methodDeclaration", input);
		Assert.assertNotNull(tree);

		final ParseTreeToString x = new ParseTreeToString();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void methodDeclaration3() {
		final String input = "public void f(Visitor v);";
		final IParseTree tree = Java8_Tests.parse("methodDeclaration", input);
		Assert.assertNotNull(tree);

		final ParseTreeToString x = new ParseTreeToString();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void methodDeclaration4() {
		final String input = "public abstract void f(Visitor v);";
		final IParseTree tree = Java8_Tests.parse("methodDeclaration", input);
		Assert.assertNotNull(tree);

		final ParseTreeToString x = new ParseTreeToString();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void methodDeclaration5() {
		final String input = "public abstract <T> void f(Visitor v);";
		final IParseTree tree = Java8_Tests.parse("methodDeclaration", input);
		Assert.assertNotNull(tree);

		final ParseTreeToString x = new ParseTreeToString();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void methodDeclaration6() {
		final String input = "public abstract <T> void f(Visitor<T> v);";
		final IParseTree tree = Java8_Tests.parse("methodDeclaration", input);
		Assert.assertNotNull(tree);

		final ParseTreeToString x = new ParseTreeToString();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void methodDeclaration7() {
		final String input = "void f(Visitor<T> v);";
		final IParseTree tree = Java8_Tests.parse("methodDeclaration", input);
		Assert.assertNotNull(tree);

		final ParseTreeToString x = new ParseTreeToString();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void methodDeclaration8() {

		final String input = "public abstract <E extends Throwable> void accept(Visitor<E> v);";

		final IParseTree tree = Java8_Tests.parse("methodDeclaration", input);
		Assert.assertNotNull(tree);

		final ParseTreeToString x = new ParseTreeToString();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void multipleMethods() {

		String input = "class Test {";
		input += "  public abstract <E extends Throwable> void accept(Visitor<E> v);";
		input += "  public abstract <E extends Throwable> void accept(Visitor<E> v);";
		input += "  public abstract <E extends Throwable> void accept(Visitor<E> v);";
		input += "  public abstract <E extends Throwable> void accept(Visitor<E> v);";
		input += "}";
		final IParseTree tree = Java8_Tests.parse(input);
		Assert.assertNotNull(tree);

		final ParseTreeToString x = new ParseTreeToString();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);

	}

	@Test
	public void abstractGeneric() {

		String input = "class Test {";
		input += "  /** Visit this tree with a given visitor.";
		input += "  */";
		input += "  public abstract <E extends Throwable> void accept(Visitor<E> v) throws E;";
		input += "}";
		final IParseTree tree = Java8_Tests.parse(input);
		Assert.assertNotNull(tree);

		final ParseTreeToString x = new ParseTreeToString();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void genericVisitorMethod() {

		String input = "class Test {";
		input += "  /** A generic visitor class for trees.";
		input += "  */";
		input += "  public static abstract class Visitor<E extends Throwable> {";
		input += "      public void visitTree(Tree that)                   throws E { assert false; }";
		input += "  }";
		input += "}";
		final IParseTree tree = Java8_Tests.parse(input);
		Assert.assertNotNull(tree);

		final ParseTreeToString x = new ParseTreeToString();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void tree() {

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
		final IParseTree tree = Java8_Tests.parse(input);

		Assert.assertNotNull(tree);

		final ParseTreeToString x = new ParseTreeToString();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void stringLiteral() {
		String input = "";
		input += "\"xxxx\"";
		final IParseTree tree = Java8_Tests.parse("StringLiteral", input);
		Assert.assertNotNull(tree);

		final ParseTreeToString x = new ParseTreeToString();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void localStringVariableDeclarationStatement() {
		String input = "";
		input += "String s = \"xxxx\";";
		final IParseTree tree = Java8_Tests.parse("localVariableDeclarationStatement", input);
		Assert.assertNotNull(tree);

		final ParseTreeToString x = new ParseTreeToString();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void stringMemberInitialised() {
		String input = "";
		input += "public class Test {";
		input += "  String s = \"xxxx\";";
		input += "}";
		final IParseTree tree = Java8_Tests.parse(input);
		Assert.assertNotNull(tree);

		final ParseTreeToString x = new ParseTreeToString();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void T6257443_1() {

		String input = "";
		input += "import java.net.URL;";
		input += "public class T6257443 {";
		input += "  public static void main(String[] args) {";
		input += "  }";
		input += "}";
		final IParseTree tree = Java8_Tests.parse(input);
		Assert.assertNotNull(tree);

		final ParseTreeToString x = new ParseTreeToString();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void T6257443_2() {

		String input = "";
		input += "import java.net.URL;";
		input += "public class T6257443 {";
		input += "  public static void main(String[] args) {";
		input += "    if (args.length != 2)";
		input += "       throw new Error(\"wrong number of args\");";
		input += "  }";
		input += "}";
		final IParseTree tree = Java8_Tests.parse(input);
		Assert.assertNotNull(tree);

		final ParseTreeToString x = new ParseTreeToString();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void T6257443_3() {

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
		final IParseTree tree = Java8_Tests.parse(input);
		Assert.assertNotNull(tree);

		final ParseTreeToString x = new ParseTreeToString();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void T6257443_4() {

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
		final IParseTree tree = Java8_Tests.parse(input);
		Assert.assertNotNull(tree);

		final ParseTreeToString x = new ParseTreeToString();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void T6257443_5() {

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

		final IParseTree tree = Java8_Tests.parse("block", input);
		Assert.assertNotNull("null==tree", tree);

		final ParseTreeToString x = new ParseTreeToString();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void T6257443_6() {

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
		final IParseTree tree = Java8_Tests.parse(input);
		Assert.assertNotNull(tree);

		final ParseTreeToString x = new ParseTreeToString();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void T6257443() {

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
		final IParseTree tree = Java8_Tests.parse(input);
		Assert.assertNotNull(tree);

		final ParseTreeToString x = new ParseTreeToString();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void expression() {
		final String input = "i++";

		final IParseTree tree = Java8_Tests.parse("expression", input);
		Assert.assertNotNull(tree);

		final ParseTreeToString x = new ParseTreeToString();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void blockStatement() {
		final String input = "i++;";

		final IParseTree tree = Java8_Tests.parse("blockStatement", input);
		Assert.assertNotNull(tree);

		final ParseTreeToString x = new ParseTreeToString();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void blockStatements() {
		final String input = "i++;";

		final IParseTree tree = Java8_Tests.parse("blockStatements", input);
		Assert.assertNotNull(tree);

		final ParseTreeToString x = new ParseTreeToString();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void switchLabel() {
		String input = "";
		input += "  case 1:";
		final IParseTree tree = Java8_Tests.parse("switchLabel", input);
		Assert.assertNotNull(tree);

		final ParseTreeToString x = new ParseTreeToString();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void switchBlockStatementGroup() {
		final String input = "case 1 : i++;";

		final IParseTree tree = Java8_Tests.parse("switchBlockStatementGroup", input);
		Assert.assertNotNull(tree);

		final ParseTreeToString x = new ParseTreeToString();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void switchBlock() {
		final String input = "{case 1:i++;}";

		final IParseTree tree = Java8_Tests.parse("switchBlock", input);
		Assert.assertNotNull(tree);

		final ParseTreeToString x = new ParseTreeToString();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void switchStatement1() {
		String input = "";
		input += "switch (i) {";
		input += "  case 1: ";
		input += "  default:";
		input += "}";

		final IParseTree tree = Java8_Tests.parse("switchStatement", input);
		Assert.assertNotNull(tree);

		final ParseTreeToString x = new ParseTreeToString();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void switchStatement() {
		String input = "";
		input += "switch (i) {";
		input += "  case 1:";
		input += "    i++;";
		input += "    // fallthrough" + System.lineSeparator();
		input += "  default:";
		input += "}";

		final IParseTree tree = Java8_Tests.parse("switchStatement", input);
		Assert.assertNotNull(tree);

		final ParseTreeToString x = new ParseTreeToString();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void T6304921() {
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
		final IParseTree tree = Java8_Tests.parse(input);
		Assert.assertNotNull(tree);

		final ParseTreeToString x = new ParseTreeToString();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void singleLineComment() {
		String input = "";
		input += "//single line comment" + System.lineSeparator();
		input += "class Test {";
		input += "}";
		final IParseTree tree = Java8_Tests.parse(input);
		Assert.assertNotNull(tree);

		final ParseTreeToString x = new ParseTreeToString();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void HexFloatLiteral() {
		final String input = "check(+0Xfffffffffffffbcp-59D, Double.parseDouble(\"+0Xfffffffffffffbcp-59D\"));";

		final IParseTree tree = Java8_Tests.parse("blockStatement", input);

		Assert.assertNotNull(tree);

		final ParseTreeToString x = new ParseTreeToString();
		final String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}
}