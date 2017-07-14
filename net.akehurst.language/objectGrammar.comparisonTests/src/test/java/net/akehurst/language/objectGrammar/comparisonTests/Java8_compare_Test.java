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
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;

import org.antlr.v4.runtime.ANTLRInputStream;
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
public class Java8_compare_Test {

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

	public Java8_compare_Test(final Path file) {
		this.file = file;
	}

	Path file;

	static OGLanguageProcessor processor;

	static {
		Java8_compare_Test.getOGLProcessor();
	}

	static OGLanguageProcessor getOGLProcessor() {
		if (null == Java8_compare_Test.processor) {
			Java8_compare_Test.processor = new OGLanguageProcessor();
			Java8_compare_Test.processor.getParser().build();
		}
		return Java8_compare_Test.processor;
	}

	static ILanguageProcessor javaProcessor;

	static {
		Java8_compare_Test.getJavaProcessor();
	}

	static ILanguageProcessor getJavaProcessor() {
		if (null == Java8_compare_Test.javaProcessor) {
			try {
				// String grammarText = new String(Files.readAllBytes(Paths.get("src/test/grammar/Java8.og")));
				final FileReader reader = new FileReader("src/test/grammar/Java8.og");
				final Grammar javaGrammar = Java8_compare_Test.getOGLProcessor().process(reader, "grammarDefinition", Grammar.class);
				Java8_compare_Test.javaProcessor = new LanguageProcessor(javaGrammar, null);
				Java8_compare_Test.javaProcessor.getParser().build();
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
		return Java8_compare_Test.javaProcessor;
	}

	@Before
	public void setUp() {
		try {
			final byte[] bytes = Files.readAllBytes(this.file);
			Java8_compare_Test.og_input = new String(bytes);
			Java8_compare_Test.antlr_input = new ANTLRInputStream(new String(bytes));
		} catch (final IOException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	static String og_input;

	static IParseTree parseWithOG(final Path file) {
		try {
			final IParseTree tree = Java8_compare_Test.getJavaProcessor().getParser().parse("compliationUnit", new StringReader(Java8_compare_Test.og_input));
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

	static CharStream antlr_input;

	static Object parseWithAntlr4(final Path file) {

		final Lexer lexer = new antlr4.Java8Lexer(Java8_compare_Test.antlr_input);
		final CommonTokenStream tokens = new CommonTokenStream(lexer);
		final Java8Parser p = new Java8Parser(tokens);
		return p.compilationUnit();
	}

	@Test
	public void compare() {

		final Instant s1 = Instant.now();
		final IParseTree tree1 = Java8_compare_Test.parseWithOG(this.file);
		final Instant e1 = Instant.now();

		final Instant s2 = Instant.now();
		final Object tree2 = Java8_compare_Test.parseWithAntlr4(this.file);
		final Instant e2 = Instant.now();

		final Duration d1 = Duration.between(s1, e1);
		final Duration d2 = Duration.between(s2, e2);
		final boolean ogFaster = d1.compareTo(d2) < 0;

		final boolean totalInSignificant = Duration.ofMillis(200).compareTo(d1) > 0;

		Assert.assertTrue("Slower", totalInSignificant || ogFaster);

		Assert.assertNotNull("Failed to parse", tree1);
		Assert.assertNotNull(tree2);
	}

}
