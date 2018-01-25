package net.akehurst.language.processor.vistraq;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import net.akehurst.language.core.analyser.UnableToAnalyseExeception;
import net.akehurst.language.core.grammar.IGrammar;
import net.akehurst.language.core.grammar.RuleNotFoundException;
import net.akehurst.language.core.parser.ParseFailedException;
import net.akehurst.language.core.parser.ParseTreeException;
import net.akehurst.language.core.sppt.ISharedPackedParseTree;
import net.akehurst.language.processor.LanguageProcessor;
import net.akehurst.language.processor.OGLanguageProcessor;

@RunWith(Parameterized.class)
public class test_QueryParserInValid {

    static class Data {
        public Data(final String queryStr) {
            this.queryStr = queryStr;
        }

        public final String queryStr;

        // --- Object ---
        @Override
        public String toString() {
            return this.queryStr;
        }
    }

    @Parameters(name = "{0}")
    public static Collection<Object[]> data() throws IOException {
        final List<Object[]> col = new ArrayList<>();
        final InputStream is = test_QueryParserInValid.class.getResourceAsStream("/vistraq/sampleInvalidQueries.txt");
        final BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line = br.readLine();
        while (null != line) {
            col.add(new Object[] { new Data(line) });
            line = br.readLine();
        }
        return col;
    }

    @Parameter
    public Data data;

    private LanguageProcessor processor;

    private String toString(final ISharedPackedParseTree tree) {
        String res = tree.asString().replaceAll(System.lineSeparator(), " ");
        res = res.trim();
        return res;
    }

    @Before
    public void setup() {
        try {
            final OGLanguageProcessor oglProc = new OGLanguageProcessor();
            final InputStreamReader reader = new InputStreamReader(test_QueryParserInValid.class.getResourceAsStream("/vistraq/Query.ogl"));
            final IGrammar grammar = oglProc.process(reader, "grammarDefinition", IGrammar.class);
            this.processor = new LanguageProcessor(grammar, null);
        } catch (final ParseFailedException e) {
            Assert.fail("Parsing query grammar failed " + e.getLongestMatch());
        } catch (final UnableToAnalyseExeception e) {
            Assert.fail("Analysing query grammar failed");
        }
    }

    @Test(expected = ParseFailedException.class)
    public void test() throws ParseFailedException, ParseTreeException, RuleNotFoundException {

        final String queryStr = this.data.queryStr;
        final ISharedPackedParseTree result = this.processor.getParser().parse("query", queryStr);
        Assert.assertNotNull(result);
        final String resultStr = this.toString(result);
        Assert.assertEquals(queryStr, resultStr);

    }

}
