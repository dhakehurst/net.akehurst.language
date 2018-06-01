package net.akehurst.language.grammar.parser.expectedAt;

import java.io.StringReader;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import net.akehurst.language.api.grammar.RuleItem;
import net.akehurst.language.api.grammar.TangibleItem;
import net.akehurst.language.api.parser.ParseFailedException;
import net.akehurst.language.api.parser.ParseTreeException;
import net.akehurst.language.api.parser.Parser;
import net.akehurst.language.grammar.parser.ScannerLessParser3;
import net.akehurst.language.ogl.grammar.OGLGrammar;
import net.akehurst.language.ogl.semanticStructure.GrammarDefault;
import net.akehurst.language.ogl.semanticStructure.GrammarBuilderDefault;
import net.akehurst.language.ogl.semanticStructure.NamespaceDefault;
import net.akehurst.language.ogl.semanticStructure.NonTerminalDefault;
import net.akehurst.language.ogl.semanticStructure.TerminalLiteralDefault;
import net.akehurst.language.parser.AbstractParser_Test;

public class test_Parser extends AbstractParser_Test {

	GrammarDefault abc() {
		final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
		b.rule("multi").multi(0, -1, new NonTerminalDefault("abc"));
		b.rule("abc").choice(new NonTerminalDefault("a"), new NonTerminalDefault("b"), new NonTerminalDefault("c"));
		b.rule("a").concatenation(new TerminalLiteralDefault("a"));
		b.rule("b").concatenation(new TerminalLiteralDefault("b"));
		b.rule("c").concatenation(new TerminalLiteralDefault("c"));
		return b.get();
	}

	@Test
	public void abc_multi_a_1() throws ParseFailedException, ParseTreeException {
		// grammar, goal, input

		final GrammarDefault g = this.abc();
		final String goal = "multi";
		final String text = "a";
		final int position = 1;
		final Parser parser = new ScannerLessParser3(this.runtimeRules, g);

		final List<RuleItem> list = parser.expectedAt(goal, new StringReader(text), position);
		Assert.assertTrue(list.size() == 1);
		Assert.assertEquals("abc", ((TangibleItem) list.get(0)).getName());

	}

	@Test
	public void oglgrammar1() throws ParseFailedException, ParseTreeException {
		// grammar, goal, input
		final OGLGrammar g = new OGLGrammar();
		// final Grammar g = this.abc();
		final String goal = "grammarDefinition";
		final String text = "";
		final int position = 0;

		final Parser parser = new ScannerLessParser3(this.runtimeRules, g);

		final List<RuleItem> list = parser.expectedAt(goal, new StringReader(text), position);
		Assert.assertTrue(list.size() == 4);
		Assert.assertEquals("namespace", ((TangibleItem) list.get(0)).getName());
	}

	@Test
	public void oglgrammar2() throws ParseFailedException, ParseTreeException {
		// grammar, goal, input
		final OGLGrammar g = new OGLGrammar();
		// final Grammar g = this.abc();
		final String goal = "grammarDefinition";
		final String text = "namespace";
		final int position = 2;

		final Parser parser = new ScannerLessParser3(this.runtimeRules, g);

		final List<RuleItem> list = parser.expectedAt(goal, new StringReader(text), position);
		Assert.assertEquals(3, list.size());
		Assert.fail("this currently fails because we don't handle expectedAt from the middle of a token, yet");
		Assert.assertEquals("namespace", ((TangibleItem) list.get(0)).getName());
	}

	@Test
	public void oglgrammar3() throws ParseFailedException, ParseTreeException {
		// grammar, goal, input
		final OGLGrammar g = new OGLGrammar();
		// final Grammar g = this.abc();
		final String goal = "grammarDefinition";
		final String text = "namespace";
		final int position = 9;

		final Parser parser = new ScannerLessParser3(this.runtimeRules, g);

		final List<RuleItem> list = parser.expectedAt(goal, new StringReader(text), position);
		Assert.assertTrue(list.size() == 4);
		Assert.assertEquals("qualifiedName", ((TangibleItem) list.get(0)).getName());
	}

	@Test
	public void oglgrammar4() throws ParseFailedException, ParseTreeException {
		// grammar, goal, input
		final OGLGrammar g = new OGLGrammar();
		// final Grammar g = this.abc();
		final String goal = "grammarDefinition";
		final String text = "namespace a";
		final int position = 11;

		final Parser parser = new ScannerLessParser3(this.runtimeRules, g);

		final List<RuleItem> list = parser.expectedAt(goal, new StringReader(text), position);
		Assert.assertEquals(5, list.size());
		Assert.assertEquals("::", ((TangibleItem) list.get(0)).getName());
	}

	@Test
	public void oglgrammar5() throws ParseFailedException, ParseTreeException {
		// grammar, goal, input
		final OGLGrammar g = new OGLGrammar();
		// final Grammar g = this.abc();
		final String goal = "grammarDefinition";
		final String text = "namespace a::";
		final int position = 13;

		final Parser parser = new ScannerLessParser3(this.runtimeRules, g);

		final List<RuleItem> list = parser.expectedAt(goal, new StringReader(text), position);
		Assert.assertEquals(4, list.size());
		Assert.assertEquals("IDENTIFIER", ((TangibleItem) list.get(0)).getName());
	}
}
