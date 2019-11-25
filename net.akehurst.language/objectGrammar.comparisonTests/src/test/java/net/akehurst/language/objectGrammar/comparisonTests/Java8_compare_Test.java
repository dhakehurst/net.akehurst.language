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

import net.akehurst.language.api.analyser.UnableToAnalyseExeception;
import net.akehurst.language.api.grammar.GrammarRuleNotFoundException;
import net.akehurst.language.api.parser.ParseFailedException;
import net.akehurst.language.api.parser.ParseTreeException;
import net.akehurst.language.api.processor.LanguageProcessor;
import net.akehurst.language.api.sppt.SharedPackedParseTree;
import net.akehurst.language.processor.LanguageProcessorDefault;
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

    static LanguageProcessor javaProcessor;

    static {
        Java8_compare_Test.getJavaProcessor();
    }

    static LanguageProcessor getJavaProcessor() {
        if (null == Java8_compare_Test.javaProcessor) {
            try {
                // String grammarText = new String(Files.readAllBytes(Paths.get("src/test/grammar/Java8.og")));
                final FileReader reader = new FileReader("src/test/grammar/Java8Optm1.og");
                final net.akehurst.language.api.grammar.Grammar javaGrammar = Java8_compare_Test.getOGLProcessor().process(reader, "grammarDefinition",
                        net.akehurst.language.api.grammar.Grammar.class);
                Java8_compare_Test.javaProcessor = new LanguageProcessorDefault(javaGrammar, null);
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

    static SharedPackedParseTree parseWithOG(final Path file) {
        try {
            final SharedPackedParseTree tree = Java8_compare_Test.getJavaProcessor().getParser().parse("compilationUnit",
                    new StringReader(Java8_compare_Test.og_input));
            return tree;
        } catch (final ParseFailedException e) {
            return null;// e.getLongestMatch();
        } catch (final ParseTreeException e) {
            e.printStackTrace();
        } catch (final GrammarRuleNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    static CharStream antlr_input;

    static Object parseWithAntlr4(final Path file) {

        final Lexer lexer = new antlr4.spec.Java8Lexer(Java8_compare_Test.antlr_input);
        final CommonTokenStream tokens = new CommonTokenStream(lexer);
        final antlr4.spec.Java8Parser p = new antlr4.spec.Java8Parser(tokens);
        return p.compilationUnit();
    }

    @Test
    public void compare() {

        final Instant sOg = Instant.now();
        final SharedPackedParseTree tree1 = Java8_compare_Test.parseWithOG(this.file);
        final Instant eOg = Instant.now();

        final Instant sAntlr = Instant.now();
        final Object tree2 = Java8_compare_Test.parseWithAntlr4(this.file);
        final Instant eAntlr = Instant.now();

        final Duration dOg = Duration.between(sOg, eOg);
        final Duration dAntlr = Duration.between(sAntlr, eAntlr);
        final boolean ogFaster = dOg.compareTo(dAntlr) < 0;

        final boolean totalInSignificant = Duration.ofMillis(200).compareTo(dOg) > 0;

        Assert.assertTrue(String.format("Slower (antlr=%s, og=%s)", dAntlr, dOg), totalInSignificant || ogFaster);

        Assert.assertNotNull("Failed to parse", tree1);
        Assert.assertNotNull(tree2);
    }

}
