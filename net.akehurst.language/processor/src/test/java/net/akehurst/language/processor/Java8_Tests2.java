package net.akehurst.language.processor;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import net.akehurst.language.core.analyser.UnableToAnalyseExeception;
import net.akehurst.language.core.grammar.GrammarRuleNotFoundException;
import net.akehurst.language.core.parser.IParser;
import net.akehurst.language.core.parser.ParseFailedException;
import net.akehurst.language.core.parser.ParseTreeException;
import net.akehurst.language.core.processor.ILanguageProcessor;
import net.akehurst.language.core.sppt.ISharedPackedParseTree;
import net.akehurst.language.ogl.semanticStructure.Grammar;

@RunWith(Parameterized.class)
public class Java8_Tests2 {

	static OGLanguageProcessor processor;

	static {
		Java8_Tests2.getOGLProcessor();
	}

	static OGLanguageProcessor getOGLProcessor() {
		if (null == Java8_Tests2.processor) {
			Java8_Tests2.processor = new OGLanguageProcessor();
			Java8_Tests2.processor.getParser().build();
		}
		return Java8_Tests2.processor;
	}

	static ILanguageProcessor javaProcessor;

	static {
		Java8_Tests2.getJavaProcessor();
	}

	static ILanguageProcessor getJavaProcessor() {
		if (null == Java8_Tests2.javaProcessor) {
			try {
				final FileReader reader = new FileReader(Paths.get("src/test/resources/Java8_all.og").toFile());
				final Grammar javaGrammar = Java8_Tests2.getOGLProcessor().process(reader, "grammarDefinition", Grammar.class);
				Java8_Tests2.javaProcessor = new LanguageProcessor(javaGrammar, null);
				Java8_Tests2.javaProcessor.getParser().build();
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
		return Java8_Tests2.javaProcessor;
	}

	static ILanguageProcessor getJavaProcessor(final String goalName) {
		try {
			final FileReader reader = new FileReader(Paths.get("src/test/resources/Java8_all.og").toFile());
			final Grammar javaGrammar = Java8_Tests2.getOGLProcessor().process(reader, "grammarDefinition", Grammar.class);
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

	// static ISharedPackedParseTree parse(final String input) throws ParseFailedException, ParseTreeException, RuleNotFoundException {
	//
	// final ISharedPackedParseTree tree = Java8_Tests2.getJavaProcessor().getParser().parse("compilationUnit", new StringReader(input));
	// return tree;
	//
	// }

	static ISharedPackedParseTree parse(final String goalName, final String input) throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {

		final IParser parser = Java8_Tests2.getJavaProcessor().getParser();
		final ISharedPackedParseTree tree = parser.parse(goalName, input);
		return tree;

	}

	static private String clean(final String str) {
		String res = str.replaceAll(System.lineSeparator(), " ");
		res = res.trim();
		return res;
	}

	static class Data {
		public Data(final String title, final String grammarRule, final String queryStr) {
			this.grammarRule = grammarRule;
			this.queryStr = queryStr;
			this.title = title;
		}

		public Data(final String title, final String grammarRule, final Supplier<String> queryStr) {
			this.grammarRule = grammarRule;
			this.queryStr = queryStr.get();
			this.title = title;
		}

		public final String grammarRule;
		public final String queryStr;
		public final String title;

		// --- Object ---
		@Override
		public String toString() {
			if (null == this.title || this.title.isEmpty()) {
				return this.grammarRule + " : " + this.queryStr;
			} else {
				return this.grammarRule + " : " + this.title;
			}
		}
	}

	@Parameters(name = "{0}")
	public static Collection<Object[]> data() {
		final List<Object[]> col = new ArrayList<>();

		col.add(new Object[] { new Data("", "DecimalIntegerLiteral", "12") });
		col.add(new Object[] { new Data("", "IntegerLiteral", "12345") });
		col.add(new Object[] { new Data("", "compilationUnit", "") });
		col.add(new Object[] { new Data("", "ifThenStatement", "if(i==1) return 1;") });
		col.add(new Object[] { new Data("", "annotation", "@AnAnnotation") });
		col.add(new Object[] { new Data("", "annotation", "@AnAnnotation(1)") });
		col.add(new Object[] { new Data("", "annotation", "@AnAnnotation(@AnAnnotation2)") });
		col.add(new Object[] { new Data("", "annotation", "@CompilerAnnotationTest(@CompilerAnnotationTest2(name=\"test\",name2=\"test2\"))") });
		col.add(new Object[] { new Data("", "interfaceModifier", "@CompilerAnnotationTest(@CompilerAnnotationTest2(name=\"test\",name2=\"test2\"))") });
		col.add(new Object[] { new Data("", "annotationTypeDeclaration", "@CAT(@CAT2(name=\"test\",name2=\"test2\")) @interface CAT { }") });
		col.add(new Object[] { new Data("", "annotationTypeDeclaration", "@CAT(@CAT2(name=\"test\",name2=\"test2\")) @interface CAT { CAT2[] value(); }") });
		col.add(new Object[] { new Data("", "interfaceDeclaration", "@CAT(@CAT2(name=\"test\",name2=\"test2\")) @interface CAT { CAT2[] value(); }") });
		col.add(new Object[] { new Data("", "typeDeclaration", "@CAT(@CAT2(name=\"test\",name2=\"test2\")) @interface CAT { CAT2[] value(); }") });
		col.add(new Object[] { new Data("", "compilationUnit", "interface An {  }") });
		col.add(new Object[] { new Data("", "typeDeclaration", "@An interface An {  }") });
		col.add(new Object[] { new Data("", "compilationUnit", "@An interface An {  }") });
		col.add(new Object[] { new Data("", "annotation", "@An()") });
		col.add(new Object[] { new Data("", "typeDeclaration", "interface An {  }") });
		col.add(new Object[] { new Data("", "typeDeclaration", "@An() interface An {  }") });
		col.add(new Object[] { new Data("", "compilationUnit", "@An() interface An {  }") });
		col.add(new Object[] { new Data("", "compilationUnit", "@An() class An {  }") });
		col.add(new Object[] { new Data("", "compilationUnit", "import x; @An() interface An {  }") });
		col.add(new Object[] { new Data("", "compilationUnit", "@An(@An) interface An {  }") });
		col.add(new Object[] { new Data("", "compilationUnit", "interface An { An[] value(); }") });
		col.add(new Object[] { new Data("", "compilationUnit", "@An(@An) interface An { An[] value(); }") });
		col.add(new Object[] { new Data("", "compilationUnit", "@An(@An) @interface An { An[] value(); }") });
		col.add(new Object[] { new Data("", "compilationUnit", "@An(@An(name=\"test\",name2=\"test2\")) @interface An { An[] value(); }") });
		col.add(new Object[] { new Data("", "compilationUnit", "package x; @CAT(@CAT2(name=\"test\",name2=\"test2\")) @interface CAT { CAT2[] value(); }") });
		col.add(new Object[] { new Data("", "compilationUnit", "package x; @interface CAT { CAT2[] value(); }") });
		col.add(new Object[] {
				new Data("", "compilationUnit", "public class ConstructorAccess { class Inner { private Inner() { if (x.i != 42 || x.c != 'x') { } } } }") });
		col.add(new Object[] { new Data("", "block", "{ (a)=(b)=1; }") });
		col.add(new Object[] { new Data("", "block", "{ (a) = (b) = 1; }") });
		col.add(new Object[] { new Data("", "block", "{ ls.add(\"Smalltalk rules!\"); }") });
		col.add(new Object[] { new Data("", "block", "{ this.j = i; this.b = true; this.c = c; ConstructorAccess.this.i = i; }") });

		col.add(new Object[] { new Data("'many ifthen'", "compilationUnit", () -> {
			String input = "class Test {";
			input += "void test() {";
			for (int i = 0; i < 10; ++i) {
				input += "  if(" + i + ") return " + i + ";";
			}
			input += "}";
			input += "}";
			return input;
		}) });

		return col;
	}

	@Parameter
	public Data data;

	@Test
	public void test() throws ParseFailedException, ParseTreeException, GrammarRuleNotFoundException {
		try {
			final String queryStr = this.data.queryStr;
			final String grammarRule = this.data.grammarRule;
			final ISharedPackedParseTree tree = Java8_Tests2.parse(grammarRule, queryStr);
			Assert.assertNotNull(tree);
			final String resultStr = Java8_Tests2.clean(tree.asString());
			Assert.assertEquals(queryStr, resultStr);
		} catch (final ParseFailedException ex) {
			System.out.println(ex.getMessage());
			System.out.println(ex.getLongestMatch());
			throw ex;
		}
	}

}