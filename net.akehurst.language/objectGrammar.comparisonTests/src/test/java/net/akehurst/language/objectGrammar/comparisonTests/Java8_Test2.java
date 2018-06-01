/**
 * Copyright (C) 2015 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

import net.akehurst.language.api.analyser.UnableToAnalyseExeception;
import net.akehurst.language.api.grammar.Grammar;
import net.akehurst.language.api.grammar.GrammarRuleNotFoundException;
import net.akehurst.language.api.parser.ParseFailedException;
import net.akehurst.language.api.parser.ParseTreeException;
import net.akehurst.language.api.processor.LanguageProcessor;
import net.akehurst.language.api.sppt.SharedPackedParseTree;
import net.akehurst.language.processor.LanguageProcessorDefault;
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

    static CharStream antlr_input;
    static String og_input;
    static OGLanguageProcessor processor;
    static LanguageProcessor oglSpecJava8Processor;
    static LanguageProcessor oglOptmAntlrJava8Processor;
    static LanguageProcessor oglOptm1Java8Processor;

    static {
        Java8_Test2.getOGLProcessor();
        Java8_Test2.getOglSpecJava8Processor();
        Java8_Test2.getOglOptmAntlr8JavaProcessor();
        Java8_Test2.getOglOptm1Java8Processor();
    }

    static OGLanguageProcessor getOGLProcessor() {
        if (null == Java8_Test2.processor) {
            Java8_Test2.processor = new OGLanguageProcessor();
            Java8_Test2.processor.getParser().build();
        }
        return Java8_Test2.processor;
    }

    static LanguageProcessor getOglSpecJava8Processor() {
        if (null == Java8_Test2.oglSpecJava8Processor) {
            try {
                final FileReader reader = new FileReader(Paths.get("src/test/grammar/Java8Spec.og").toFile());
                final Grammar javaGrammar = Java8_Test2.getOGLProcessor().process(reader, "grammarDefinition", Grammar.class);
                Java8_Test2.oglSpecJava8Processor = new LanguageProcessorDefault(javaGrammar, null);
                Java8_Test2.oglSpecJava8Processor.getParser().build();
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
        return Java8_Test2.oglSpecJava8Processor;
    }

    static LanguageProcessor getOglOptmAntlr8JavaProcessor() {
        if (null == Java8_Test2.oglOptmAntlrJava8Processor) {
            try {
                final FileReader reader = new FileReader(Paths.get("src/test/grammar/Java8OptmAntlr.og").toFile());
                final Grammar javaGrammar = Java8_Test2.getOGLProcessor().process(reader, "grammarDefinition", Grammar.class);
                Java8_Test2.oglOptmAntlrJava8Processor = new LanguageProcessorDefault(javaGrammar, null);
                Java8_Test2.oglOptmAntlrJava8Processor.getParser().build();
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
        return Java8_Test2.oglOptmAntlrJava8Processor;
    }

    static LanguageProcessor getOglOptm1Java8Processor() {
        if (null == Java8_Test2.oglOptm1Java8Processor) {
            try {
                final FileReader reader = new FileReader(Paths.get("src/test/grammar/Java8Optm1.og").toFile());
                final Grammar javaGrammar = Java8_Test2.getOGLProcessor().process(reader, "grammarDefinition", Grammar.class);
                Java8_Test2.oglOptm1Java8Processor = new LanguageProcessorDefault(javaGrammar, null);
                Java8_Test2.oglOptm1Java8Processor.getParser().build();
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
        return Java8_Test2.oglOptm1Java8Processor;
    }

    static SharedPackedParseTree parseWithOglJava8Spec(final Path file) {
        try {
            final SharedPackedParseTree tree = Java8_Test2.getOglSpecJava8Processor().getParser().parse("compilationUnit",
                    new StringReader(Java8_Test2.og_input));
            return tree;
        } catch (ParseFailedException | ParseTreeException | GrammarRuleNotFoundException e) {
            System.out.println("Failed to parse: " + file);
            System.out.println(e.getMessage());
            // System.out.println("Longest Match: "+e.getLongestMatch().getRoot().getMatchedText());
            // Assert.fail(e.getMessage());
        }
        return null;
    }

    static SharedPackedParseTree parseWithOglJava8OptmAntlr(final Path file) {
        try {
            final SharedPackedParseTree tree = Java8_Test2.getOglOptmAntlr8JavaProcessor().getParser().parse("compilationUnit",
                    new StringReader(Java8_Test2.og_input));
            return tree;
        } catch (ParseFailedException | ParseTreeException | GrammarRuleNotFoundException e) {
            System.out.println("Failed to parse: " + file);
            System.out.println(e.getMessage());
            // System.out.println("Longest Match: "+e.getLongestMatch().getRoot().getMatchedText());
            // Assert.fail(e.getMessage());
        }
        return null;
    }

    static SharedPackedParseTree parseWithOglJava8Optm1(final Path file) {
        try {
            final SharedPackedParseTree tree = Java8_Test2.getOglOptm1Java8Processor().getParser().parse("compilationUnit",
                    new StringReader(Java8_Test2.og_input));
            return tree;
        } catch (ParseFailedException | ParseTreeException | GrammarRuleNotFoundException e) {
            System.out.println("Failed to parse: " + file);
            System.out.println(e.getMessage());
            // System.out.println("Longest Match: "+e.getLongestMatch().getRoot().getMatchedText());
            // Assert.fail(e.getMessage());
        }
        return null;
    }

    static antlr4.spec.Java8Parser.CompilationUnitContext parseWithAntlr4Spec(final Path file) {

        final Lexer lexer = new antlr4.spec.Java8Lexer(Java8_Test2.antlr_input);
        final CommonTokenStream tokens = new CommonTokenStream(lexer);
        final antlr4.spec.Java8Parser p = new antlr4.spec.Java8Parser(tokens);
        p.setErrorHandler(new BailErrorStrategy());
        try {
            return p.compilationUnit();
        } catch (final Exception e) {
            return null;
        }
    }

    static antlr4.optm.Java8Parser.CompilationUnitContext parseWithAntlr4Optm(final Path file) {

        final Lexer lexer = new antlr4.optm.Java8Lexer(Java8_Test2.antlr_input);
        final CommonTokenStream tokens = new CommonTokenStream(lexer);
        final antlr4.optm.Java8Parser p = new antlr4.optm.Java8Parser(tokens);
        p.setErrorHandler(new BailErrorStrategy());
        try {
            return p.compilationUnit();
        } catch (final Exception e) {
            return null;
        }
    }

    final Path file;

    public Java8_Test2(final Path file) {
        this.file = file;
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

    @Test
    public void ogl_spec_compilationUnit() {
        final SharedPackedParseTree tree = Java8_Test2.parseWithOglJava8Spec(this.file);
        Assert.assertNotNull("Failed to Parse", tree);
    }

    @Test
    public void ogl_optmAntlr_compilationUnit() {
        final SharedPackedParseTree tree = Java8_Test2.parseWithOglJava8OptmAntlr(this.file);
        Assert.assertNotNull("Failed to Parse", tree);
    }

    @Test
    public void ogl_optm1_compilationUnit() {
        final SharedPackedParseTree tree = Java8_Test2.parseWithOglJava8Optm1(this.file);
        Assert.assertNotNull("Failed to Parse", tree);
    }

    @Test
    public void antlr4_spec_compilationUnit() {
        final antlr4.spec.Java8Parser.CompilationUnitContext tree = Java8_Test2.parseWithAntlr4Spec(this.file);
        Assert.assertNotNull("Failed to Parse", tree);
    }

    @Test
    public void antlr4_optm_compilationUnit() {
        final antlr4.optm.Java8Parser.CompilationUnitContext tree = Java8_Test2.parseWithAntlr4Optm(this.file);
        Assert.assertNotNull("Failed to Parse", tree);
    }

    void temp() {

        final int i = 0;
        final int i2 = 1_9;
        final double d = 1.;
        final double d2 = .1;

    }
}
