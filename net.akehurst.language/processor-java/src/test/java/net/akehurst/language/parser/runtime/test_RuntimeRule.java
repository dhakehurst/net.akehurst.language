package net.akehurst.language.parser.runtime;

import org.junit.Assert;
import org.junit.Test;

import net.akehurst.language.grammar.parser.converter.Converter;
import net.akehurst.language.grammar.parser.converter.rules.Grammar2RuntimeRuleSet;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleKind;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleSetBuilder;
import net.akehurst.language.ogl.semanticStructure.GrammarDefault;
import net.akehurst.language.ogl.semanticStructure.GrammarBuilderDefault;
import net.akehurst.language.ogl.semanticStructure.NamespaceDefault;
import net.akehurst.language.ogl.semanticStructure.TerminalLiteralDefault;

public class test_RuntimeRule {

	@Test
	public void terminal() throws Exception {

		final RuntimeRuleSetBuilder runtimeRules = new RuntimeRuleSetBuilder();

		final GrammarBuilderDefault gb = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
		gb.rule("a").concatenation(new TerminalLiteralDefault("a"));
		final GrammarDefault grammar = gb.get();
		final Converter c = new Converter(runtimeRules);
		c.transformLeft2Right(Grammar2RuntimeRuleSet.class, grammar);

		final RuntimeRule rr = runtimeRules.getRuntimeRule(new TerminalLiteralDefault("a"));

		Assert.assertEquals(false, rr.couldHaveChild(rr, 0));
		Assert.assertEquals(1, rr.findTerminalAt(0).size());
		Assert.assertEquals(false, rr.getIsEmptyRule());
		Assert.assertEquals(false, rr.getIsSkipRule());
		Assert.assertEquals(RuntimeRuleKind.TERMINAL, rr.getKind());
		Assert.assertEquals("a", rr.getName());
		Assert.assertEquals("a", rr.getNodeTypeName());
		Assert.assertEquals(16, rr.getPatternFlags());
		Assert.assertEquals(null, rr.getRhs());
		Assert.assertEquals(-1, rr.getRhsIndexOf(rr));
		Assert.assertEquals(null, rr.getRhsItem(0));
		Assert.assertEquals(1, rr.getRuleNumber());
		Assert.assertEquals(null, rr.getRuleThatIsEmpty());
		Assert.assertEquals(null, rr.getSeparator());
		Assert.assertEquals("a", rr.getTerminalPatternText());

	}

	@Test
	public void concatination() throws Exception {

		final RuntimeRuleSetBuilder runtimeRules = new RuntimeRuleSetBuilder();

		final GrammarBuilderDefault gb = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
		gb.rule("S").concatenation(new TerminalLiteralDefault("a"), new TerminalLiteralDefault("b"), new TerminalLiteralDefault("c"));
		final GrammarDefault grammar = gb.get();
		final Converter c = new Converter(runtimeRules);
		c.transformLeft2Right(Grammar2RuntimeRuleSet.class, grammar);

		final RuntimeRule term_a = runtimeRules.getRuntimeRule(new TerminalLiteralDefault("a"));
		final RuntimeRule term_b = runtimeRules.getRuntimeRule(new TerminalLiteralDefault("b"));
		final RuntimeRule term_c = runtimeRules.getRuntimeRule(new TerminalLiteralDefault("c"));
		final RuntimeRule rr = runtimeRules.getRuntimeRule(grammar.findRule("S"));

		Assert.assertEquals(false, rr.couldHaveChild(rr, 0));
		Assert.assertEquals(true, rr.couldHaveChild(term_a, 0));
		Assert.assertEquals(true, rr.couldHaveChild(term_b, 1));
		Assert.assertEquals(true, rr.couldHaveChild(term_c, 2));
		Assert.assertEquals(1, rr.findTerminalAt(0).size());
		Assert.assertEquals(1, rr.findTerminalAt(1).size());
		Assert.assertEquals(1, rr.findTerminalAt(2).size());
		Assert.assertEquals(false, rr.getIsEmptyRule());
		Assert.assertEquals(false, rr.getIsSkipRule());
		Assert.assertEquals(RuntimeRuleKind.NON_TERMINAL, rr.getKind());
		Assert.assertEquals("S", rr.getName());
		Assert.assertEquals("S", rr.getNodeTypeName());
		Assert.assertEquals(16, rr.getPatternFlags());
		Assert.assertNotNull(rr.getRhs());
		Assert.assertEquals(0, rr.getRhsIndexOf(term_a));
		Assert.assertEquals(1, rr.getRhsIndexOf(term_b));
		Assert.assertEquals(2, rr.getRhsIndexOf(term_c));
		Assert.assertEquals(term_a, rr.getRhsItem(0));
		Assert.assertEquals(term_b, rr.getRhsItem(1));
		Assert.assertEquals(term_c, rr.getRhsItem(2));
		Assert.assertEquals(0, rr.getRuleNumber());
		Assert.assertEquals(null, rr.getRuleThatIsEmpty());
		Assert.assertEquals(rr.getRhsItem(1), rr.getSeparator());
		Assert.assertEquals("S", rr.getTerminalPatternText());

	}

	@Test
	public void choice() throws Exception {

		final RuntimeRuleSetBuilder runtimeRules = new RuntimeRuleSetBuilder();

		final GrammarBuilderDefault gb = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
		gb.rule("S").choice(new TerminalLiteralDefault("a"), new TerminalLiteralDefault("b"), new TerminalLiteralDefault("c"));
		final GrammarDefault grammar = gb.get();
		final Converter c = new Converter(runtimeRules);
		c.transformLeft2Right(Grammar2RuntimeRuleSet.class, grammar);

		final RuntimeRule term_a = runtimeRules.getRuntimeRule(new TerminalLiteralDefault("a"));
		final RuntimeRule term_b = runtimeRules.getRuntimeRule(new TerminalLiteralDefault("b"));
		final RuntimeRule term_c = runtimeRules.getRuntimeRule(new TerminalLiteralDefault("c"));
		final RuntimeRule rr = runtimeRules.getRuntimeRule(grammar.findRule("S"));

		Assert.assertEquals(false, rr.couldHaveChild(rr, 0));
		Assert.assertEquals(true, rr.couldHaveChild(term_a, 0));
		Assert.assertEquals(true, rr.couldHaveChild(term_b, 0));
		Assert.assertEquals(false, rr.couldHaveChild(term_b, 1));
		Assert.assertEquals(true, rr.couldHaveChild(term_c, 0));
		Assert.assertEquals(false, rr.couldHaveChild(term_c, 2));
		Assert.assertEquals(3, rr.findTerminalAt(0).size());
		Assert.assertEquals(0, rr.findTerminalAt(1).size());
		Assert.assertEquals(0, rr.findTerminalAt(2).size());
		Assert.assertEquals(false, rr.getIsEmptyRule());
		Assert.assertEquals(false, rr.getIsSkipRule());
		Assert.assertEquals(RuntimeRuleKind.NON_TERMINAL, rr.getKind());
		Assert.assertEquals("S", rr.getName());
		Assert.assertEquals("S", rr.getNodeTypeName());
		Assert.assertEquals(16, rr.getPatternFlags());
		Assert.assertNotNull(rr.getRhs());
		Assert.assertEquals(0, rr.getRhsIndexOf(term_a));
		Assert.assertEquals(1, rr.getRhsIndexOf(term_b));
		Assert.assertEquals(2, rr.getRhsIndexOf(term_c));
		Assert.assertEquals(term_a, rr.getRhsItem(0));
		Assert.assertEquals(term_b, rr.getRhsItem(1));
		Assert.assertEquals(term_c, rr.getRhsItem(2));
		Assert.assertEquals(0, rr.getRuleNumber());
		Assert.assertEquals(null, rr.getRuleThatIsEmpty());
		Assert.assertEquals(rr.getRhsItem(1), rr.getSeparator());
		Assert.assertEquals("S", rr.getTerminalPatternText());

	}

	@Test
	public void multi_0_m1() throws Exception {

		final RuntimeRuleSetBuilder runtimeRules = new RuntimeRuleSetBuilder();

		final GrammarBuilderDefault gb = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
		gb.rule("S").multi(0, -1, new TerminalLiteralDefault("a"));
		final GrammarDefault grammar = gb.get();
		final Converter c = new Converter(runtimeRules);
		c.transformLeft2Right(Grammar2RuntimeRuleSet.class, grammar);

		final RuntimeRule term_a = runtimeRules.getRuntimeRule(new TerminalLiteralDefault("a"));
		final RuntimeRule rr = runtimeRules.getRuntimeRule(grammar.findAllRule("S"));
		final RuntimeRule empty = runtimeRules.getRuntimeRuleSet().getEmptyRule(rr);

		Assert.assertEquals(false, rr.couldHaveChild(rr, 0));
		Assert.assertEquals(true, rr.couldHaveChild(empty, 0));
		Assert.assertEquals(false, rr.couldHaveChild(empty, 1));
		Assert.assertEquals(true, rr.couldHaveChild(term_a, 0));
		Assert.assertEquals(true, rr.couldHaveChild(term_a, 1));
		Assert.assertEquals(true, rr.couldHaveChild(term_a, 2));

		Assert.assertEquals(2, rr.findTerminalAt(0).size());
		Assert.assertEquals(1, rr.findTerminalAt(1).size());
		Assert.assertEquals(false, rr.getIsEmptyRule());
		Assert.assertEquals(false, rr.getIsSkipRule());
		Assert.assertEquals(RuntimeRuleKind.NON_TERMINAL, rr.getKind());
		Assert.assertEquals("S", rr.getName());
		Assert.assertEquals("S", rr.getNodeTypeName());
		Assert.assertEquals(16, rr.getPatternFlags());
		Assert.assertNotNull(rr.getRhs());
		Assert.assertEquals(0, rr.getRhsIndexOf(term_a));
		Assert.assertEquals(term_a, rr.getRhsItem(0));
		Assert.assertEquals(0, rr.getRuleNumber());
		Assert.assertEquals(null, rr.getRuleThatIsEmpty());
		// Assert.assertEquals(null,rr.getSeparator());
		Assert.assertEquals("S", rr.getTerminalPatternText());

	}

	@Test
	public void multi_1_m1() throws Exception {

		final RuntimeRuleSetBuilder runtimeRules = new RuntimeRuleSetBuilder();

		final GrammarBuilderDefault gb = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
		gb.rule("S").multi(1, -1, new TerminalLiteralDefault("a"));
		final GrammarDefault grammar = gb.get();
		final Converter c = new Converter(runtimeRules);
		c.transformLeft2Right(Grammar2RuntimeRuleSet.class, grammar);

		final RuntimeRule term_a = runtimeRules.getRuntimeRule(new TerminalLiteralDefault("a"));
		final RuntimeRule rr = runtimeRules.getRuntimeRule(grammar.findAllRule("S"));
		final RuntimeRule empty = runtimeRules.getRuntimeRuleSet().getEmptyRule(rr);

		Assert.assertNull(empty);
		Assert.assertEquals(false, rr.couldHaveChild(rr, 0));
		Assert.assertEquals(true, rr.couldHaveChild(term_a, 0));
		Assert.assertEquals(true, rr.couldHaveChild(term_a, 1));
		Assert.assertEquals(true, rr.couldHaveChild(term_a, 2));

		Assert.assertEquals(1, rr.findTerminalAt(0).size());
		Assert.assertEquals(1, rr.findTerminalAt(1).size());
		Assert.assertEquals(false, rr.getIsEmptyRule());
		Assert.assertEquals(false, rr.getIsSkipRule());
		Assert.assertEquals(RuntimeRuleKind.NON_TERMINAL, rr.getKind());
		Assert.assertEquals("S", rr.getName());
		Assert.assertEquals("S", rr.getNodeTypeName());
		Assert.assertEquals(16, rr.getPatternFlags());
		Assert.assertNotNull(rr.getRhs());
		Assert.assertEquals(0, rr.getRhsIndexOf(term_a));
		Assert.assertEquals(term_a, rr.getRhsItem(0));
		Assert.assertEquals(0, rr.getRuleNumber());
		Assert.assertEquals(null, rr.getRuleThatIsEmpty());
		// Assert.assertEquals(null,rr.getSeparator());
		Assert.assertEquals("S", rr.getTerminalPatternText());

	}

}
