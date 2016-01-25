package net.akehurst.language.objectGrammar.comparisonTests;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.Test;

import net.akehurst.language.core.ILanguageProcessor;
import net.akehurst.language.core.analyser.UnableToAnalyseExeception;
import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.core.parser.ParseFailedException;
import net.akehurst.language.core.parser.ParseTreeException;
import net.akehurst.language.ogl.semanticStructure.Grammar;
import net.akehurst.language.processor.LanguageProcessor;
import net.akehurst.language.processor.OGLanguageProcessor;

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
				String grammarText = new String(Files.readAllBytes(Paths.get("src/test/grammar/Java8.og")));
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
			return e.getLongestMatch();
		} catch (ParseTreeException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Test
	public void emptyCompilationUnit() {

		String input = "";
		IParseTree tree = parse(input);
		Assert.assertNotNull(tree);

	}

	@Test
	public void ifReturn() {

		String input = "class Test {";
		input += "void test() {";
		for (int i = 0; i < 100; ++i) {
			input += "  if(" + i + ") return " + i + ";";
		}
		input += "}";
		input += "}";
		IParseTree tree = parse(input);
		Assert.assertNotNull(tree);

	}

	@Test
	public void tryfinally0() {

		String input = "class Test {";
		input += "  static public int test(int i) {";
		input += "    try {";
		input += "      if(i==1) return 1;";
		input += "    } finally {";
		input += "       if(i==1) return 1;";
		input += "    }";
		input += "  }";
		input += "}";
		IParseTree tree = parse(input);
		Assert.assertNotNull(tree);

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

	}

	@Test
	public void multipleFields() {

		String input = "class Test {";
		input += "  Integer i1";
		input += "  Integer i2";
		input += "  Integer i3";
		input += "  Integer i4";
		input += "}";
		IParseTree tree = parse(input);
		Assert.assertNotNull(tree);

	}
	
	@Test
	public void multipleMethods() {

		String input = "class Test {";
//		input += "  public abstract <E extends Throwable> void accept(Visitor<E> v);";
//		input += "  public abstract <E extends Throwable> void accept(Visitor<E> v);";
		input += "  public abstract <E extends Throwable> void accept(Visitor<E> v);";
		input += "  public abstract <E extends Throwable> void accept(Visitor<E> v);";
		input += "}";
		IParseTree tree = parse(input);
		Assert.assertNotNull(tree);

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

	}
}