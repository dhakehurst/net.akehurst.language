package net.akehurst.language.grammar.parser.forrest;

import net.akehurst.language.parser.sppf.ParseTreeBuilder;
import org.junit.Assert;
import org.junit.Test;

import net.akehurst.language.api.sppt.SPPTLeaf;
import net.akehurst.language.grammar.parser.converter.Converter;
import net.akehurst.language.grammar.parser.converter.rules.Grammar2RuntimeRuleSet;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleSetBuilder;
import net.akehurst.language.ogl.semanticStructure.GrammarDefault;
import net.akehurst.language.ogl.semanticStructure.GrammarBuilderDefault;
import net.akehurst.language.ogl.semanticStructure.NamespaceDefault;
import net.akehurst.language.ogl.semanticStructure.TerminalLiteralDefault;

public class test_ParseTreeBuilder {

	@Test
	public void leaf() {
		try {
			final String text = "a";
			final RuntimeRuleSetBuilder runtimeRules = new RuntimeRuleSetBuilder();

			final GrammarBuilderDefault gb = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
			gb.rule("a").concatenation(new TerminalLiteralDefault("a"));
			final GrammarDefault grammar = gb.get();
			final Converter c = new Converter(runtimeRules);
			c.transformLeft2Right(Grammar2RuntimeRuleSet.class, grammar);

			final ParseTreeBuilder b = new ParseTreeBuilder(runtimeRules, grammar, "a", text, 0);
			final SPPTLeaf l = b.leaf("a");

			Assert.assertEquals(0, l.getStartPosition());
			// Assert.assertEquals(1, l.getEnd());
			Assert.assertEquals(1, l.getMatchedTextLength());
			Assert.assertEquals(0, l.getNumberOfLines());
			// Assert.assertEquals(false, l.getIsEmpty());
			Assert.assertEquals(false, l.isSkip());
			Assert.assertEquals("a", l.getMatchedText());
			Assert.assertEquals("a", l.getName());

		} catch (final Exception e) {
			Assert.fail(e.getMessage());
		}
	}

}
