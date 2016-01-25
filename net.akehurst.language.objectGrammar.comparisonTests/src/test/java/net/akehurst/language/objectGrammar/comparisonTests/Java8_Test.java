package net.akehurst.language.objectGrammar.comparisonTests;

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
import net.akehurst.language.core.ILanguageProcessor;
import net.akehurst.language.core.analyser.UnableToAnalyseExeception;
import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.core.parser.ParseFailedException;
import net.akehurst.language.core.parser.ParseTreeException;
import net.akehurst.language.ogl.semanticStructure.Grammar;
import net.akehurst.language.processor.LanguageProcessor;
import net.akehurst.language.processor.OGLanguageProcessor;

public class Java8_Test {

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

	static IParseTree parseWithOG(Path file) {
		try {
			byte[] bytes = Files.readAllBytes(file);
			String text = new String(bytes);
			
			IParseTree tree = getJavaProcessor().getParser().parse(getJavaProcessor().getDefaultGoal(), text);
			
			return tree;
		} catch (ParseFailedException e) {
			
		} catch (ParseTreeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	static Object parseWithAntlr4(Path file) {
		try {
			byte[] bytes = Files.readAllBytes(file);
			CharStream input = new ANTLRInputStream(new String(bytes));
			Lexer lexer = new antlr4.Java8Lexer(input);
			CommonTokenStream tokens = new CommonTokenStream(lexer);
			Java8Parser p = new Java8Parser(tokens);
			return p.compilationUnit();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	@Test
	public void og_compilationUnit() {

		Path file = Paths.get("src/test/resources/File1.java");
		IParseTree tree = parseWithOG(file);
		Assert.assertNotNull(tree);
	}

	@Test
	public void og_longName() {
		Path file = Paths.get("src/test/resources/javac/limits/LongName.java");
		IParseTree tree = parseWithOG(file);
		Assert.assertNotNull(tree);
	}
	
	@Test
	public void og_numArgs1() {
		Path file = Paths.get("src/test/resources/javac/limits/NumArgs1.java");
		IParseTree tree = parseWithOG(file);
		Assert.assertNotNull(tree);
	}
	
	@Test
	public void antlr4_compilationUnit() {
		Path file = Paths.get("src/test/resources/File1.java");
		Object tree = parseWithAntlr4(file);
		Assert.assertNotNull(tree);
	}

	PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:**/*.java");

	@Test
	public void og_openjdk_javac_files() {

		try {
			Files.walkFileTree(Paths.get("src/test/resources/javac"), new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					if (attrs.isRegularFile() && matcher.matches(file)) {
						System.out.print("Parse: " + file + "    ");
						Object o = parseWithOG(file);
						if (null!=o) {
							System.out.println("Success");
						} else {
							System.out.println("Failed");
						}
						// Assert.assertNotNull(o);
					}
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Test
	public void antlr_openjdk_javac_files() {

		try {
			Files.walkFileTree(Paths.get("src/test/resources/javac"), new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					if (attrs.isRegularFile() && matcher.matches(file)) {
						System.out.print("Parse: " + file + "    ");
						Object o = parseWithAntlr4(file);
						if (null!=o) {
							System.out.println("Success");
						} else {
							System.out.println("Failed");
						}
					}
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
