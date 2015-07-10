package net.akehurst.language.objectGrammar.comparisonTests;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.antlr.v4.runtime.ANTLRInputStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.TokenStream;
import org.junit.Assert;
import org.junit.Test;

import antlr4.Java8Parser;
import net.akehurst.language.core.ILanguageProcessor;
import net.akehurst.language.core.analyser.UnableToAnalyseExeception;
import net.akehurst.language.core.parser.ParseFailedException;
import net.akehurst.language.core.parser.ParseTreeException;
import net.akehurst.language.ogl.semanticStructure.Grammar;
import net.akehurst.language.processor.LanguageProcessor;
import net.akehurst.language.processor.OGLanguageProcessor;

public class Java8_Test {

	public Java8_Test() {
		try {
			this.file1 = new String(Files.readAllBytes(Paths.get("src/test/resources/File1.java")));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	OGLanguageProcessor processor;
	OGLanguageProcessor getOGLProcessor() {
		if (null==this.processor) {
			this.processor = new OGLanguageProcessor();
			this.processor.getParser().build( this.processor.getDefaultGoal() );
		}
		return this.processor;
	}
	
	ILanguageProcessor javaProcessor;
	ILanguageProcessor getJavaProcessor() {
		if (null==this.javaProcessor) {
			try {
				String grammarText = new String(Files.readAllBytes(Paths.get("src/test/grammar/Java8.og")));
				Grammar javaGrammar = this.getOGLProcessor().process(grammarText, Grammar.class);
				this.javaProcessor = new LanguageProcessor(javaGrammar, "compilationUnit", null);
				this.javaProcessor.getParser().build(this.javaProcessor.getDefaultGoal());
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
		return this.javaProcessor;
	}
	
	String file1;
	
	@Test
	public void og_compilationUnit() {
		
		try {
			this.getJavaProcessor().getParser().parse(this.getJavaProcessor().getDefaultGoal(), this.file1);
		} catch (ParseFailedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseTreeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

	@Test
	public void antlr4_compilationUnit() {
	
		CharStream input = new ANTLRInputStream(this.file1);
		Lexer lexer = new antlr4.Java8Lexer(input);
		CommonTokenStream tokens = new CommonTokenStream(lexer);
		antlr4.Java8Parser parser = new Java8Parser(tokens);
		
		antlr4.Java8Parser.CompilationUnitContext cu = parser.compilationUnit();
		
	}
	
}
