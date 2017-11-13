package net.akehurst.language.objectGrammar.comparisonTests;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.junit.Assert;
import org.junit.Test;

import antlr4.Java8Parser;
import net.akehurst.language.core.analyser.UnableToAnalyseExeception;
import net.akehurst.language.core.grammar.RuleNotFoundException;
import net.akehurst.language.core.parser.ParseFailedException;
import net.akehurst.language.core.parser.ParseTreeException;
import net.akehurst.language.core.processor.ILanguageProcessor;
import net.akehurst.language.core.sppt.ISharedPackedParseTree;
import net.akehurst.language.ogl.semanticStructure.Grammar;
import net.akehurst.language.processor.LanguageProcessor;
import net.akehurst.language.processor.OGLanguageProcessor;

public class Java8_Test {

	static OGLanguageProcessor processor;

	static {
		Java8_Test.getOGLProcessor();
	}

	static OGLanguageProcessor getOGLProcessor() {
		if (null == Java8_Test.processor) {
			Java8_Test.processor = new OGLanguageProcessor();
			Java8_Test.processor.getParser().build();
		}
		return Java8_Test.processor;
	}

	static ILanguageProcessor javaProcessor;

	static {
		Java8_Test.getJavaProcessor();
	}

	static ILanguageProcessor getJavaProcessor() {
		if (null == Java8_Test.javaProcessor) {
			try {
				// String grammarText = new String(Files.readAllBytes(Paths.get("src/test/grammar/Java8.og")));
				final FileReader reader = new FileReader("src/test/grammar/Java8.og");
				final Grammar javaGrammar = Java8_Test.getOGLProcessor().process(reader, "grammarDefinition", Grammar.class);
				Java8_Test.javaProcessor = new LanguageProcessor(javaGrammar, null);
				Java8_Test.javaProcessor.getParser().build();
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
		return Java8_Test.javaProcessor;
	}

	static ISharedPackedParseTree parseWithOG(final Path file) {
		try {
			final byte[] bytes = Files.readAllBytes(file);
			final FileReader reader = new FileReader(file.toFile());

			final ISharedPackedParseTree tree = Java8_Test.getJavaProcessor().getParser().parse("compilationUnit", reader);

			return tree;
		} catch (ParseFailedException | ParseTreeException | IOException | RuleNotFoundException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
		return null;
	}

	static Object parseWithAntlr4(final Path file) {
		try {
			final byte[] bytes = Files.readAllBytes(file);
			final CharStream input = new ANTLRInputStream(new String(bytes));
			final Lexer lexer = new antlr4.Java8Lexer(input);
			final CommonTokenStream tokens = new CommonTokenStream(lexer);
			final Java8Parser p = new Java8Parser(tokens);
			return p.compilationUnit();
		} catch (final IOException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
		return null;
	}

	@Test
	public void og_compilationUnit() {

		final Path file = Paths.get("src/test/resources/File1.java");
		final ISharedPackedParseTree tree = Java8_Test.parseWithOG(file);
		Assert.assertNotNull(tree);
	}

	@Test
	public void og_longName() {
		final Path file = Paths.get("src/test/resources/javac/limits/LongName.java");
		final ISharedPackedParseTree tree = Java8_Test.parseWithOG(file);
		Assert.assertNotNull(tree);
	}

	@Test
	public void og_numArgs1() {
		final Path file = Paths.get("src/test/resources/javac/limits/NumArgs1.java");
		final ISharedPackedParseTree tree = Java8_Test.parseWithOG(file);
		Assert.assertNotNull(tree);
	}

	@Test
	public void antlr4_compilationUnit() {
		final Path file = Paths.get("src/test/resources/File1.java");
		final Object tree = Java8_Test.parseWithAntlr4(file);
		Assert.assertNotNull(tree);
	}

	PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:**/*.java");

	@Test
	public void og_openjdk_javac_files() {

		try {
			Files.walkFileTree(Paths.get("src/test/resources/javac"), new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
					if (attrs.isRegularFile() && Java8_Test.this.matcher.matches(file)) {
						System.out.print("Parse: " + file + "    ");
						final Object o = Java8_Test.parseWithOG(file);
						if (null != o) {
							System.out.println("Success");
						} else {
							System.out.println("Failed");
						}
						// Assert.assertNotNull(o);
					}
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (final IOException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}

	}

	@Test
	public void antlr_openjdk_javac_files() {

		try {
			Files.walkFileTree(Paths.get("src/test/resources/javac"), new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
					if (attrs.isRegularFile() && Java8_Test.this.matcher.matches(file)) {
						System.out.print("Parse: " + file + "    ");
						final Object o = Java8_Test.parseWithAntlr4(file);
						if (null != o) {
							System.out.println("Success");
						} else {
							System.out.println("Failed");
						}
					}
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (final IOException e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}

	}
}
