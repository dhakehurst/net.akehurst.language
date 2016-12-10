package net.akehurst.language.processor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.Test;

import net.akehurst.language.core.ILanguageProcessor;
import net.akehurst.language.core.analyser.UnableToAnalyseExeception;
import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.core.parser.IParser;
import net.akehurst.language.core.parser.ParseFailedException;
import net.akehurst.language.core.parser.ParseTreeException;
import net.akehurst.language.grammar.parser.ParseTreeToString;
import net.akehurst.language.ogl.semanticStructure.Grammar;

public class Java8_Tests {

	static OGLanguageProcessor processor;

	static {
		getOGLProcessor();
	}

	static OGLanguageProcessor getOGLProcessor() {
		if (null == processor) {
			processor = new OGLanguageProcessor();
			processor.getParser().build(processor.getDefaultGoal());
		}
		return processor;
	}

	static ILanguageProcessor javaProcessor;

	static {
		getJavaProcessor();
	}

	static ILanguageProcessor getJavaProcessor() {
		if (null == javaProcessor) {
			try {
				String grammarText = new String(Files.readAllBytes(Paths.get("src/test/resources/Java8_all.og")));
				Grammar javaGrammar = getOGLProcessor().process(grammarText, Grammar.class);
				javaProcessor = new LanguageProcessor(javaGrammar, "compilationUnit", null);
				javaProcessor.getParser().build(javaProcessor.getDefaultGoal());
			} catch (IOException e) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			} catch (ParseFailedException e) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			} catch (UnableToAnalyseExeception e) {
				e.printStackTrace();
				Assert.fail(e.getMessage());
			}

		}
		return javaProcessor;
	}

	static ILanguageProcessor getJavaProcessor(String goalName) {
		try {
			String grammarText = new String(Files.readAllBytes(Paths.get("src/test/resources/Java8_all.og")));
			Grammar javaGrammar = getOGLProcessor().process(grammarText, Grammar.class);
			LanguageProcessor jp = new LanguageProcessor(javaGrammar, goalName, null);
			jp.getParser().build(jp.getDefaultGoal());
			return jp;
		} catch (IOException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} catch (ParseFailedException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} catch (UnableToAnalyseExeception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
		return null;
	}

	String toString(Path file) {
		try {
			byte[] bytes = Files.readAllBytes(file);
			String str = new String(bytes);
			return str;
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	static IParseTree parse(String input) {
		try {

			IParseTree tree = getJavaProcessor().getParser().parse(getJavaProcessor().getDefaultGoal(), input);
			return tree;
		} catch (ParseFailedException e) {
			return null;// e.getLongestMatch();
		} catch (ParseTreeException e) {
			e.printStackTrace();
		}
		return null;
	}

	static IParseTree parse(String goalName, String input) {
		try {
			IParser parser = getJavaProcessor().getParser();
			IParseTree tree = parser.parse(goalName, input);
			return tree;
		} catch (ParseFailedException e) {
			return null;// e.getLongestMatch();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Test
	public void emptyCompilationUnit() {

		String input = "";
		IParseTree tree = parse(input);
		Assert.assertNotNull(tree);

		ParseTreeToString x = new ParseTreeToString();
		String output = x.visit(tree, null);
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
		IParseTree tree = parse(input);
		Assert.assertNotNull(tree);

		ParseTreeToString x = new ParseTreeToString();
		String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void ifThenStatement_1() {

		String input = "if(i==1) return 1;";

		IParseTree tree = parse("ifThenStatement", input);
		Assert.assertNotNull(tree);

		ParseTreeToString x = new ParseTreeToString();
		String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void ifThenStatement_2() {

		String input = "if(i==1) {return 1;}";

		IParseTree tree = parse("ifThenStatement", input);
		Assert.assertNotNull(tree);

		ParseTreeToString x = new ParseTreeToString();
		String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}
	
	@Test
	public void ifThenElseStatement_1() {

		String input = "if(i==1) return 1; else return 2;";

		IParseTree tree = parse("ifThenElseStatement", input);
		Assert.assertNotNull(tree);

		ParseTreeToString x = new ParseTreeToString();
		String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void ifThenElseStatement_2() {

		String input = "if(i==1) {return 1;} else {return 2;}";

		IParseTree tree = parse("ifThenElseStatement", input);
		Assert.assertNotNull(tree);

		ParseTreeToString x = new ParseTreeToString();
		String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}
	
	@Test
	public void statement_ifThenElseStatement_1() {

		String input = "if(i==1) return 1; else return 2;";

		IParseTree tree = parse("statement", input);
		Assert.assertNotNull(tree);

		ParseTreeToString x = new ParseTreeToString();
		String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void statement_ifThenElseStatement_2() {

		String input = "if(i==1) {return 1;} else {return 2;}";

		IParseTree tree = parse("statement", input);
		Assert.assertNotNull(tree);

		ParseTreeToString x = new ParseTreeToString();
		String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}
	
	@Test
	public void ifThenElseIf_1() {

		String input = "if(i==1) return 1; else if (false) return 2;";

		IParseTree tree = parse("ifThenElseStatement", input);
		Assert.assertNotNull(tree);

		ParseTreeToString x = new ParseTreeToString();
		String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void ifThenElseIf_2() {

		String input = "if(i==1) {return 1;} else if (false) {return 2;}";

		IParseTree tree = parse("ifThenElseStatement", input);
		Assert.assertNotNull(tree);

		ParseTreeToString x = new ParseTreeToString();
		String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}
	
	@Test
	public void trycatch0() {

		String input = "try {";
		input += "      if(i==1) return 1;";
		input += "    } catch(E e) {";
		input += "       if(i==1) return 1;";
		input += "    }";
		IParseTree tree = parse("tryStatement", input);
		Assert.assertNotNull(tree);

		ParseTreeToString x = new ParseTreeToString();
		String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void tryfinally0() {

		String input = "try {";
		input += "      if(i==1) return 1;";
		input += "    } finally {";
		input += "       if(i==1) return 1;";
		input += "    }";
		IParseTree tree = parse("tryStatement", input);
		Assert.assertNotNull(tree);

		ParseTreeToString x = new ParseTreeToString();
		String output = x.visit(tree, null);
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
		IParseTree tree = parse(input);
		Assert.assertNotNull(tree);

		ParseTreeToString x = new ParseTreeToString();
		String output = x.visit(tree, null);
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
		IParseTree tree = parse(input);
		Assert.assertNotNull(tree);

		ParseTreeToString x = new ParseTreeToString();
		String output = x.visit(tree, null);
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
		IParseTree tree = parse(input);
		Assert.assertNotNull(tree);

		ParseTreeToString x = new ParseTreeToString();
		String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void manyFields() {

		String input = "class Test {";
		for (int i = 0; i < 8; ++i) {
			input += "  Integer i" + i + ";";
		}
		input += "}";
		IParseTree tree = parse(input);
		Assert.assertNotNull(tree);

		ParseTreeToString x = new ParseTreeToString();
		String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void formalParameter() {

		String input = "Visitor<E> v";
		IParseTree tree = null;
		try {
			tree = parse("formalParameter", input);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
		Assert.assertNotNull(tree);


		ParseTreeToString x = new ParseTreeToString();
		String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}
	
	@Test
	public void formalParameters1() {

		String input = "Visitor v";
		IParseTree tree = null;
		try {
			tree = parse("formalParameters", input);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
		Assert.assertNotNull(tree);


		ParseTreeToString x = new ParseTreeToString();
		String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}
	
	@Test
	public void formalParameters2() {

		String input = "Visitor v, Type p2";
		IParseTree tree = null;
		try {
			tree = parse("formalParameters", input);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
		Assert.assertNotNull(tree);


		ParseTreeToString x = new ParseTreeToString();
		String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}
	
	@Test
	public void formalParameters() {

		String input = "Visitor<E> v";
		IParseTree tree = null;
		try {
			tree = parse("formalParameters", input);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
		Assert.assertNotNull(tree);


		ParseTreeToString x = new ParseTreeToString();
		String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void typeArguments() {

		String input = "<E>";
		IParseTree tree = null;
		try {
			tree = parse("typeArguments", input);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
		Assert.assertNotNull(tree);


		ParseTreeToString x = new ParseTreeToString();
		String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}
	
	@Test
	public void ISO8859encoding() {
		String input = "";
		input += "class T6302184 {";
		input += "  int ������ = 1;";
		input += "}";
		IParseTree tree = parse(input);
		Assert.assertNotNull(tree);

		ParseTreeToString x = new ParseTreeToString();
		String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void classBody() {

		String input = "{ int i1; int i2; int i3; int i4; int i5; int i6; int i7; int i8; }";
		IParseTree tree = null;
		try {
			tree = parse("classBody", input);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
		Assert.assertNotNull(tree);

		ParseTreeToString x = new ParseTreeToString();
		String output = x.visit(tree, null);
		Assert.assertEquals(input, output);

	}

	@Test
	public void methodDeclaration1() {
		String input = "void f();";
		IParseTree tree = parse("methodDeclaration",input);
		Assert.assertNotNull(tree);
		
		ParseTreeToString x = new ParseTreeToString();
		String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}
	
	@Test
	public void methodDeclaration2() {
		String input = "public void f();";
		IParseTree tree = parse("methodDeclaration",input);
		Assert.assertNotNull(tree);
		
		ParseTreeToString x = new ParseTreeToString();
		String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}
	
	@Test
	public void methodDeclaration3() {
		String input = "public void f(Visitor v);";
		IParseTree tree = parse("methodDeclaration",input);
		Assert.assertNotNull(tree);
		
		ParseTreeToString x = new ParseTreeToString();
		String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}
	
	@Test
	public void methodDeclaration4() {
		String input = "public abstract void f(Visitor v);";
		IParseTree tree = parse("methodDeclaration",input);
		Assert.assertNotNull(tree);
		
		ParseTreeToString x = new ParseTreeToString();
		String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}
	
	@Test
	public void methodDeclaration5() {
		String input = "public abstract <T> void f(Visitor v);";
		IParseTree tree = parse("methodDeclaration",input);
		Assert.assertNotNull(tree);
		
		ParseTreeToString x = new ParseTreeToString();
		String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}
	
	@Test
	public void methodDeclaration6() {
		String input = "public abstract <T> void f(Visitor<T> v);";
		IParseTree tree = parse("methodDeclaration",input);
		Assert.assertNotNull(tree);
		
		ParseTreeToString x = new ParseTreeToString();
		String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}
	
	
	@Test
	public void methodDeclaration7() {
		String input = "void f(Visitor<T> v);";
		IParseTree tree = parse("methodDeclaration",input);
		Assert.assertNotNull(tree);
		
		ParseTreeToString x = new ParseTreeToString();
		String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}
	
	@Test
	public void methodDeclaration8() {

		String input = "public abstract <E extends Throwable> void accept(Visitor<E> v);";

		IParseTree tree = parse("methodDeclaration",input);
		Assert.assertNotNull(tree);

		ParseTreeToString x = new ParseTreeToString();
		String output = x.visit(tree, null);
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
		IParseTree tree = parse(input);
		Assert.assertNotNull(tree);

		ParseTreeToString x = new ParseTreeToString();
		String output = x.visit(tree, null);
		Assert.assertEquals(input, output);

	}

	@Test
	public void abstractGeneric() {

		String input = "class Test {";
		input += "  /** Visit this tree with a given visitor.";
		input += "  */";
		input += "  public abstract <E extends Throwable> void accept(Visitor<E> v) throws E;";
		input += "}";
		IParseTree tree = parse(input);
		Assert.assertNotNull(tree);

		ParseTreeToString x = new ParseTreeToString();
		String output = x.visit(tree, null);
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
		IParseTree tree = parse(input);
		Assert.assertNotNull(tree);

		ParseTreeToString x = new ParseTreeToString();
		String output = x.visit(tree, null);
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
		IParseTree tree = parse(input);

		Assert.assertNotNull(tree);

		ParseTreeToString x = new ParseTreeToString();
		String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void stringLiteral() {
		String input = "";
		input += "\"xxxx\"";
		IParseTree tree = parse("StringLiteral", input);
		Assert.assertNotNull(tree);
		
		ParseTreeToString x = new ParseTreeToString();
		String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void localStringVariableDeclarationStatement() {
		String input = "";
		input += "String s = \"xxxx\";";
		IParseTree tree = parse("localVariableDeclarationStatement", input);
		Assert.assertNotNull(tree);
		
		ParseTreeToString x = new ParseTreeToString();
		String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void stringMemberInitialised() {
		String input = "";
		input += "public class Test {";
		input += "  String s = \"xxxx\";";
		input += "}";
		IParseTree tree = parse(input);
		Assert.assertNotNull(tree);
		
		ParseTreeToString x = new ParseTreeToString();
		String output = x.visit(tree, null);
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
		IParseTree tree = parse(input);
		Assert.assertNotNull(tree);

		ParseTreeToString x = new ParseTreeToString();
		String output = x.visit(tree, null);
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
		IParseTree tree = parse(input);
		Assert.assertNotNull(tree);

		ParseTreeToString x = new ParseTreeToString();
		String output = x.visit(tree, null);
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
		IParseTree tree = parse(input);
		Assert.assertNotNull(tree);

		ParseTreeToString x = new ParseTreeToString();
		String output = x.visit(tree, null);
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
		IParseTree tree = parse(input);
		Assert.assertNotNull(tree);

		ParseTreeToString x = new ParseTreeToString();
		String output = x.visit(tree, null);
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

		IParseTree tree = parse("block",input);
		Assert.assertNotNull("null==tree", tree);

		ParseTreeToString x = new ParseTreeToString();
		String output = x.visit(tree, null);
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
		IParseTree tree = parse(input);
		Assert.assertNotNull(tree);

		ParseTreeToString x = new ParseTreeToString();
		String output = x.visit(tree, null);
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
		IParseTree tree = parse(input);
		Assert.assertNotNull(tree);

		ParseTreeToString x = new ParseTreeToString();
		String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void expression() {
		String input = "i++";

		IParseTree tree = parse("expression", input);
		Assert.assertNotNull(tree);
		
		ParseTreeToString x = new ParseTreeToString();
		String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}
	
	@Test
	public void blockStatement() {
		String input = "i++;";

		IParseTree tree = parse("blockStatement", input);
		Assert.assertNotNull(tree);
		
		ParseTreeToString x = new ParseTreeToString();
		String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}
	
	@Test
	public void blockStatements() {
		String input = "i++;";

		IParseTree tree = parse("blockStatements", input);
		Assert.assertNotNull(tree);
		
		ParseTreeToString x = new ParseTreeToString();
		String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}
	
	@Test
	public void switchLabel() {
		String input = "";
		input += "  case 1:";
		IParseTree tree = parse("switchLabel", input);
		Assert.assertNotNull(tree);
		
		ParseTreeToString x = new ParseTreeToString();
		String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void switchBlockStatementGroup() {
		String input = "case 1 : i++;";

		IParseTree tree = parse("switchBlockStatementGroup", input);
		Assert.assertNotNull(tree);
		
		ParseTreeToString x = new ParseTreeToString();
		String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}
	
	@Test
	public void switchBlock() {
		String input = "{case 1:i++;}";

		IParseTree tree = parse("switchBlock", input);
		Assert.assertNotNull(tree);
		
		ParseTreeToString x = new ParseTreeToString();
		String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}
	
	@Test
	public void switchStatement1() {
		String input = "";
		input += "switch (i) {";
		input += "  case 1: ";
		input += "  default:";
		input += "}";

		IParseTree tree = parse("switchStatement", input);
		Assert.assertNotNull(tree);
		
		ParseTreeToString x = new ParseTreeToString();
		String output = x.visit(tree, null);
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

		IParseTree tree = parse("switchStatement", input);
		Assert.assertNotNull(tree);
		
		ParseTreeToString x = new ParseTreeToString();
		String output = x.visit(tree, null);
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
		IParseTree tree = parse(input);
		Assert.assertNotNull(tree);
		
		ParseTreeToString x = new ParseTreeToString();
		String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}

	@Test
	public void singleLineComment() {
		String input = "";
		input += "//single line comment" + System.lineSeparator();
		input += "class Test {";
		input += "}";
		IParseTree tree = parse(input);
		Assert.assertNotNull(tree);
		
		ParseTreeToString x = new ParseTreeToString();
		String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}
	
	@Test
	public void HexFloatLiteral() {
		String input = "check(+0Xfffffffffffffbcp-59D, Double.parseDouble(\"+0Xfffffffffffffbcp-59D\"));";

		IParseTree tree = parse("blockStatement",input);
		
		Assert.assertNotNull(tree);
		
		ParseTreeToString x = new ParseTreeToString();
		String output = x.visit(tree, null);
		Assert.assertEquals(input, output);
	}
}