package net.akehurst.language.objectGrammar.comparisonTests;

import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.BailErrorStrategy;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import antlr4.Java8Parser;
import net.akehurst.language.core.analyser.UnableToAnalyseExeception;
import net.akehurst.language.core.grammar.RuleNotFoundException;
import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.core.parser.ParseFailedException;
import net.akehurst.language.core.parser.ParseTreeException;
import net.akehurst.language.core.processor.ILanguageProcessor;
import net.akehurst.language.ogl.semanticStructure.Grammar;
import net.akehurst.language.processor.LanguageProcessor;
import net.akehurst.language.processor.OGLanguageProcessor;

@RunWith(Parameterized.class)
public class Java8_Test2 {

	@Parameters(name = "{index}: {0}")
	public static Collection<Object[]> getFiles() {
		final ArrayList<Object[]> params = new ArrayList<>();
		try {
			final PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:**/*.java");

			Files.walkFileTree(Paths.get("src/test/resources/javac"), new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
					if (attrs.isRegularFile() && matcher.matches(file)) {
						params.add(new Object[] { file });
					}
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (final IOException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
		return params;
	}

	public Java8_Test2(final Path file) {
		this.file = file;
	}

	Path file;

	static OGLanguageProcessor processor;

	static {
		Java8_Test2.getOGLProcessor();
	}

	static OGLanguageProcessor getOGLProcessor() {
		if (null == Java8_Test2.processor) {
			Java8_Test2.processor = new OGLanguageProcessor();
			Java8_Test2.processor.getParser().build();
		}
		return Java8_Test2.processor;
	}

	static ILanguageProcessor javaProcessor;

	static {
		Java8_Test2.getJavaProcessor();
	}

	static ILanguageProcessor getJavaProcessor() {
		if (null == Java8_Test2.javaProcessor) {
			try {
				final FileReader reader = new FileReader(Paths.get("src/test/grammar/Java8.og").toFile());
				final Grammar javaGrammar = Java8_Test2.getOGLProcessor().process(reader, "grammarDefinition", Grammar.class);
				Java8_Test2.javaProcessor = new LanguageProcessor(javaGrammar, null);
				Java8_Test2.javaProcessor.getParser().build();
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
		return Java8_Test2.javaProcessor;
	}

	@Before
	public void setUp() {
		try {
			final byte[] bytes = Files.readAllBytes(this.file);
			Java8_Test2.og_input = new String(bytes);
			Java8_Test2.antlr_input = new ANTLRInputStream(new String(bytes));
		} catch (final IOException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	static String og_input;

	static IParseTree parseWithOG(final Path file) {
		try {
			final IParseTree tree = Java8_Test2.getJavaProcessor().getParser().parse("compilationUnit", new StringReader(Java8_Test2.og_input));
			return tree;
		} catch (ParseFailedException | ParseTreeException | RuleNotFoundException e) {
			System.out.println("Failed to parse: " + file);
			System.out.println(e.getMessage());
			// System.out.println("Longest Match: "+e.getLongestMatch().getRoot().getMatchedText());
			// Assert.fail(e.getMessage());
		}
		return null;
	}

	static CharStream antlr_input;

	static Java8Parser.CompilationUnitContext parseWithAntlr4(final Path file) {

		final Lexer lexer = new antlr4.Java8Lexer(Java8_Test2.antlr_input);
		final CommonTokenStream tokens = new CommonTokenStream(lexer);
		final Java8Parser p = new Java8Parser(tokens);
		p.setErrorHandler(new BailErrorStrategy());
		try {
			return p.compilationUnit();
		} catch (final Exception e) {
			return null;
		}
	}

	@Test
	public void og_compilationUnit() {

		final IParseTree tree = Java8_Test2.parseWithOG(this.file);
		Assert.assertNotNull("Failed to Parse", tree);
	}

	@Test
	public void antlr4_compilationUnit() {
		final Java8Parser.CompilationUnitContext tree = Java8_Test2.parseWithAntlr4(this.file);
		Assert.assertNotNull("Failed to Parse", tree);
	}

}
