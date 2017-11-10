package net.akehurst.language.processor;

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
import net.akehurst.language.core.sppf.IParseTree;

@RunWith(Parameterized.class)
public class test_VistraqQuery {

	static class Data {
		public Data(final String grammarRule, final String queryStr) {
			this.grammarRule = grammarRule;
			this.queryStr = queryStr;
		}

		public final String grammarRule;
		public final String queryStr;

		// --- Object ---
		@Override
		public String toString() {
			return this.grammarRule + " : " + this.queryStr;
		}
	}

	@Parameters(name = "{0}")
	public static Collection<Object[]> data() {
		final List<Object[]> col = new ArrayList<>();
		col.add(new Object[] { new Data("linkSelector", "LINKED VIA * TO *") });
		col.add(new Object[] { new Data("artefactSelector", "*") });
		col.add(new Object[] { new Data("artefactSelector", "Requirement") });
		col.add(new Object[] { new Data("artefactSelector", "Requirement AS r") });
		col.add(new Object[] { new Data("artefactSelector", "Requirement AS r WHERE") });
		col.add(new Object[] { new Data("artefactSelector", "Requirement LINKED VIA * TO *") });
		col.add(new Object[] { new Data("artefactSelector", "* LINKED VIA * TO *") });
		col.add(new Object[] { new Data("returnDefinition", "RETURN r AS Req") });
		col.add(new Object[] { new Data("columnDefinition", "r AS Req") });
		col.add(new Object[] { new Data("query", "MATCH *") });
		col.add(new Object[] { new Data("query", "MATCH * AS n") });
		col.add(new Object[] { new Data("query", "MATCH Requirement AS r") });
		col.add(new Object[] { new Data("query", "MATCH Requirement AS r RETURN r AS Req") });
		col.add(new Object[] { new Data("query", "MATCH Requirement AS r RETURN r.identity AS Id") });
		col.add(new Object[] { new Data("expression", "r.type.name") });
		col.add(new Object[] { new Data("columnDefinition", "r.type.name AS Type") });
		col.add(new Object[] { new Data("returnDefinition", "RETURN r.type.name AS Type") });
		col.add(new Object[] { new Data("query", "MATCH Requirement AS r RETURN r.identity AS Id r.type.name AS Type") });
		col.add(new Object[] { new Data("query", "MATCH * LINKED VIA * TO *") });
		col.add(new Object[] { new Data("query", "MATCH * AS r LINKED VIA * TO * AS t") });
		col.add(new Object[] { new Data("query", "MATCH Requirement LINKED VIA * TO *") });
		col.add(new Object[] { new Data("query", "MATCH Requirement AS r LINKED VIA * TO * AS t") });
		col.add(new Object[] { new Data("query", "MATCH Requirement LINKED VIA * TO Test") });
		col.add(new Object[] { new Data("query", "MATCH Requirement AS r LINKED VIA * TO Test AS t") });
		col.add(new Object[] { new Data("query", "MATCH Requirement AS r LINKED VIA testedBy TO Test AS t") });
		col.add(new Object[] { new Data("query", "MATCH Requirement AS r LINKED VIA testedBy TO Test AS t") });
		col.add(new Object[] { new Data("query", "MATCH * LINKED VIA * TO * LINKED VIA * TO *") });
		col.add(new Object[] { new Data("query", "MATCH * AS n RETURN COUNT n AS count") });
		col.add(new Object[] { new Data("query", "MATCH * NOT LINKED VIA * TO *") });
		col.add(new Object[] { new Data("query", "MATCH * NOT LINKED VIA * FROM *") });
		col.add(new Object[] { new Data("query", "MATCH * ( LINKED VIA * TO * AND LINKED VIA * TO *  )") });
		col.add(new Object[] { new Data("query", "FROM query1") });
		col.add(new Object[] { new Data("query", "FROM query1 RETURN r AS Req") });
		col.add(new Object[] { new Data("query", "FROM qs1.query1 RETURN r AS Req") });
		return col;
	}

	@Parameter
	public Data data;

	private LanguageProcessor processor;

	private String clean(final String str) {
		String res = str.replaceAll(System.lineSeparator(), " ");
		res = res.trim();
		return res;
	}

	@Before
	public void setup() {
		try {
			final OGLanguageProcessor oglProc = new OGLanguageProcessor();
			final InputStreamReader reader = new InputStreamReader(test_VistraqQuery.class.getResourceAsStream("/VistraqQuery.ogl"));
			final IGrammar grammar = oglProc.process(reader, "grammarDefinition", IGrammar.class);

			this.processor = new LanguageProcessor(grammar, null);
		} catch (final ParseFailedException e) {
			Assert.fail("Parsing query grammar failed " + e.getLongestMatch());
		} catch (final UnableToAnalyseExeception e) {
			Assert.fail("Analysing query grammar failed");
		}
	}

	@Test
	public void test() throws ParseFailedException, ParseTreeException, RuleNotFoundException {

		final String queryStr = this.data.queryStr;
		final String grammarRule = this.data.grammarRule;
		final IParseTree tree = this.processor.getParser().parse(grammarRule, queryStr);
		Assert.assertNotNull(tree);
		final String resultStr = this.clean(tree.asString());
		Assert.assertEquals(queryStr, resultStr);

	}

}
