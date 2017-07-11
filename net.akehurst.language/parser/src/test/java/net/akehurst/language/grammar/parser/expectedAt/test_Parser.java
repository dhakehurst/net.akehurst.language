package net.akehurst.language.grammar.parser.expectedAt;

import java.io.StringReader;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import net.akehurst.language.core.parser.IParser;
import net.akehurst.language.core.parser.ParseFailedException;
import net.akehurst.language.core.parser.ParseTreeException;
import net.akehurst.language.grammar.parser.ScannerLessParser3;
import net.akehurst.language.ogl.semanticStructure.Grammar;
import net.akehurst.language.ogl.semanticStructure.GrammarBuilder;
import net.akehurst.language.ogl.semanticStructure.Namespace;
import net.akehurst.language.ogl.semanticStructure.NonTerminal;
import net.akehurst.language.ogl.semanticStructure.TerminalLiteral;
import net.akehurst.language.parser.AbstractParser_Test;

public class test_Parser extends AbstractParser_Test {

	Grammar abc() {
		final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("abc").choice(new NonTerminal("a"), new NonTerminal("b"), new NonTerminal("c"));
		b.rule("a").concatenation(new TerminalLiteral("a"));
		b.rule("b").concatenation(new TerminalLiteral("b"));
		b.rule("c").concatenation(new TerminalLiteral("c"));
		return b.get();
	}

	@Test
	public void abc_abc_1() throws ParseFailedException, ParseTreeException {
		// grammar, goal, input

		final Grammar g = this.abc();
		final String goal = "abc";
		final String text = "a";

		final IParser parser = new ScannerLessParser3(this.runtimeRules, g);

		final List<?> list = parser.expectedAt(goal, new StringReader(text), 1);
		Assert.assertTrue(list.size() > 0);
	}
}
