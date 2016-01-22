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
import net.akehurst.language.core.ILanguageProcessor;
import net.akehurst.language.core.analyser.UnableToAnalyseExeception;
import net.akehurst.language.core.parser.IParseTree;
import net.akehurst.language.core.parser.ParseFailedException;
import net.akehurst.language.core.parser.ParseTreeException;
import net.akehurst.language.ogl.semanticStructure.Grammar;
import net.akehurst.language.processor.LanguageProcessor;
import net.akehurst.language.processor.OGLanguageProcessor;

@RunWith(Parameterized.class)
public class Java8_compare_Test {

	@Parameters(name = "{index}: {0}")
	public static Collection<Object[]> getFiles() {
		ArrayList<Object[]> params = new ArrayList<>();
		try {
			PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:**/*.java");

			Files.walkFileTree(Paths.get("src/test/resources/javac"), new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					if (attrs.isRegularFile() && matcher.matches(file)) {
						params.add(new Object[] { file });
					}
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return params;
	}

	public Java8_compare_Test(Path file) {
		this.file = file;
	}

	Path file;

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

	@Before
	public void setUp() {
		try {
			byte[] bytes = Files.readAllBytes(file);
			og_input = new String(bytes);
			antlr_input = new ANTLRInputStream(new String(bytes));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	static String og_input;
	static IParseTree parseWithOG(Path file) {
		try {
			IParseTree tree = getJavaProcessor().getParser().parse(getJavaProcessor().getDefaultGoal(), og_input);
			return tree;
		} catch (ParseFailedException e) {
			return e.getLongestMatch();
		} catch (ParseTreeException e) {
			e.printStackTrace();
		}
		return null;
	}

	static CharStream antlr_input;

	static Object parseWithAntlr4(Path file) {

		Lexer lexer = new antlr4.Java8Lexer(antlr_input);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		Java8Parser p = new Java8Parser(tokens);
		return p.compilationUnit();
	}

	@Test
	public void compare() {

		Instant s1 = Instant.now();
		IParseTree tree1 = parseWithOG(this.file);
		Instant e1 = Instant.now();
		
		Instant s2 = Instant.now();
		Object tree2 = parseWithAntlr4(this.file);
		Instant e2 = Instant.now();

		Assert.assertTrue( Duration.between(s1, e1).compareTo(Duration.between(s2, e2)) < 0 );
		
		Assert.assertNotNull(tree1);
		Assert.assertNotNull(tree2);
	}


}
