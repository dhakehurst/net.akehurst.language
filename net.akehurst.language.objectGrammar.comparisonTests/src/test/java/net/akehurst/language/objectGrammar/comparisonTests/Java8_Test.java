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
		if (null==processor) {
			processor = new OGLanguageProcessor();
			processor.getParser().build( processor.getDefaultGoal() );
		}
		return processor;
	}
	
	static ILanguageProcessor javaProcessor;
	static {
		getJavaProcessor();
	}
	static ILanguageProcessor getJavaProcessor() {
		if (null==javaProcessor) {
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
			IParseTree tree = getJavaProcessor().getParser().parse(getJavaProcessor().getDefaultGoal(), new String(bytes));
			System.out.println("Successfull Parse: "+file);
			return tree;
		} catch (ParseFailedException e) {
			System.out.println("Failed to parse: "+file);
		} catch (ParseTreeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	static String file1;
	static {
		try {
			file1 = new String(Files.readAllBytes(Paths.get("src/test/resources/File1.java")));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void og_compilationUnit() {
		
		try {
			IParseTree tree = getJavaProcessor().getParser().parse(getJavaProcessor().getDefaultGoal(), this.file1);
			Assert.assertNotNull(tree);
		} catch (ParseFailedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseTreeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	static antlr4.Java8Parser antlr4_parser;
	static {
		CharStream input = new ANTLRInputStream(file1);
		Lexer lexer = new antlr4.Java8Lexer(input);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		antlr4_parser = new Java8Parser(tokens);
	}
	
	static Object parseWithAntlr4(byte[] bytes) {
		CharStream input = new ANTLRInputStream(new String(bytes));
		Lexer lexer = new antlr4.Java8Lexer(input);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		Java8Parser p = new Java8Parser(tokens);
		return p.compilationUnit();
	}
	
	@Test
	public void antlr4_compilationUnit() {
	
		antlr4.Java8Parser.CompilationUnitContext cu = antlr4_parser.compilationUnit();
		Assert.assertNotNull(cu);
	}
	
	PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:**/*.java");
	
	@Test
	public void og_openjdk_javac_files() {
		
		try {
			Files.walkFileTree(Paths.get("src/test/resources/javac"), new SimpleFileVisitor<Path>(){
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					if (attrs.isRegularFile() && matcher.matches(file)) {						
						Object o = parseWithOG(file);
//						Assert.assertNotNull(o);
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
