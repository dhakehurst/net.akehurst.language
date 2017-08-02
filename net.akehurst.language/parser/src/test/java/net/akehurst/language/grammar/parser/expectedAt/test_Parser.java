package net.akehurst.language.grammar.parser.expectedAt;

import java.io.StringReader;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import net.akehurst.language.core.grammar.IRuleItem;
import net.akehurst.language.core.grammar.ITangibleItem;
import net.akehurst.language.core.parser.IParser;
import net.akehurst.language.core.parser.ParseFailedException;
import net.akehurst.language.core.parser.ParseTreeException;
import net.akehurst.language.grammar.parser.ScannerLessParser3;
import net.akehurst.language.ogl.grammar.OGLGrammar;
import net.akehurst.language.ogl.semanticStructure.Grammar;
import net.akehurst.language.ogl.semanticStructure.GrammarBuilder;
import net.akehurst.language.ogl.semanticStructure.Namespace;
import net.akehurst.language.ogl.semanticStructure.NonTerminal;
import net.akehurst.language.ogl.semanticStructure.TerminalLiteral;
import net.akehurst.language.parser.AbstractParser_Test;

public class test_Parser extends AbstractParser_Test {

	Grammar abc() {
		final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("multi").multi(0, -1, new NonTerminal("abc"));
		b.rule("abc").choice(new NonTerminal("a"), new NonTerminal("b"), new NonTerminal("c"));
		b.rule("a").concatenation(new TerminalLiteral("a"));
		b.rule("b").concatenation(new TerminalLiteral("b"));
		b.rule("c").concatenation(new TerminalLiteral("c"));
		return b.get();
	}

	@Test
	public void abc_multi_a_1() throws ParseFailedException, ParseTreeException {
		// grammar, goal, input

		final Grammar g = this.abc();
		final String goal = "multi";
		final String text = "a";
		final int position = 1;
		final IParser parser = new ScannerLessParser3(this.runtimeRules, g);

		final List<IRuleItem> list = parser.expectedAt(goal, new StringReader(text), position);
		Assert.assertTrue(list.size() == 1);
		Assert.assertEquals("abc", ((ITangibleItem) list.get(0)).getName());

	}

	@Test
	public void oglgrammar1() throws ParseFailedException, ParseTreeException {
		// grammar, goal, input
		final OGLGrammar g = new OGLGrammar();
		// final Grammar g = this.abc();
		final String goal = "grammarDefinition";
		final String text = "";
		final int position = 0;

		final IParser parser = new ScannerLessParser3(this.runtimeRules, g);

		final List<IRuleItem> list = parser.expectedAt(goal, new StringReader(text), position);
		Assert.assertTrue(list.size() == 4);
		Assert.assertEquals("namespace", ((ITangibleItem) list.get(0)).getName());
	}

	@Test
	public void oglgrammar2() throws ParseFailedException, ParseTreeException {
		// grammar, goal, input
		final OGLGrammar g = new OGLGrammar();
		// final Grammar g = this.abc();
		final String goal = "grammarDefinition";
		final String text = "namespace";
		final int position = 2;

		final IParser parser = new ScannerLessParser3(this.runtimeRules, g);

		final List<IRuleItem> list = parser.expectedAt(goal, new StringReader(text), position);
		Assert.assertEquals(3, list.size());
		Assert.fail("this currently fails because we don't handle expectedAt from the middle of a token, yet");
		Assert.assertEquals("namespace", ((ITangibleItem) list.get(0)).getName());
	}

	@Test
	public void oglgrammar3() throws ParseFailedException, ParseTreeException {
		// grammar, goal, input
		final OGLGrammar g = new OGLGrammar();
		// final Grammar g = this.abc();
		final String goal = "grammarDefinition";
		final String text = "namespace";
		final int position = 9;

		final IParser parser = new ScannerLessParser3(this.runtimeRules, g);

		final List<IRuleItem> list = parser.expectedAt(goal, new StringReader(text), position);
		Assert.assertTrue(list.size() == 4);
		Assert.assertEquals("qualifiedName", ((ITangibleItem) list.get(0)).getName());
	}

	@Test
	public void oglgrammar4() throws ParseFailedException, ParseTreeException {
		// grammar, goal, input
		final OGLGrammar g = new OGLGrammar();
		// final Grammar g = this.abc();
		final String goal = "grammarDefinition";
		final String text = "namespace a";
		final int position = 11;

		final IParser parser = new ScannerLessParser3(this.runtimeRules, g);

		final List<IRuleItem> list = parser.expectedAt(goal, new StringReader(text), position);
		Assert.assertEquals(5, list.size());
		Assert.assertEquals("::", ((ITangibleItem) list.get(0)).getName());
	}

	@Test
	public void oglgrammar5() throws ParseFailedException, ParseTreeException {
		// grammar, goal, input
		final OGLGrammar g = new OGLGrammar();
		// final Grammar g = this.abc();
		final String goal = "grammarDefinition";
		final String text = "namespace a::";
		final int position = 13;

		final IParser parser = new ScannerLessParser3(this.runtimeRules, g);

		final List<IRuleItem> list = parser.expectedAt(goal, new StringReader(text), position);
		Assert.assertEquals(4, list.size());
		Assert.assertEquals("IDENTIFIER", ((ITangibleItem) list.get(0)).getName());
	}
}
