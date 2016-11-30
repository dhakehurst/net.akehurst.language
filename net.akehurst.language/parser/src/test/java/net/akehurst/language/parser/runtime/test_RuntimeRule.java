package net.akehurst.language.parser.runtime;

import org.junit.Assert;
import org.junit.Test;

import net.akehurst.language.core.parser.RuleNotFoundException;
import net.akehurst.language.grammar.parser.converter.Converter;
import net.akehurst.language.grammar.parser.converter.Grammar2RuntimeRuleSet;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleKind;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleSet;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleSetBuilder;
import net.akehurst.language.ogl.semanticStructure.Grammar;
import net.akehurst.language.ogl.semanticStructure.GrammarBuilder;
import net.akehurst.language.ogl.semanticStructure.Namespace;
import net.akehurst.language.ogl.semanticStructure.TerminalLiteral;
import net.akehurst.transform.binary.RelationNotFoundException;

public class test_RuntimeRule {

	@Test
	public void terminal() {
		try {
			RuntimeRuleSetBuilder runtimeRules = new RuntimeRuleSetBuilder();

			GrammarBuilder gb = new GrammarBuilder(new Namespace("test"), "Test");
			gb.rule("a").concatenation(new TerminalLiteral("a"));
			Grammar grammar = gb.get();
			Converter c = new Converter(runtimeRules);
			c.transformLeft2Right(Grammar2RuntimeRuleSet.class, grammar);

			RuntimeRule rr = runtimeRules.getRuntimeRule(new TerminalLiteral("a"));

			
			Assert.assertEquals(false, rr.couldHaveChild(rr, 0));
			Assert.assertEquals(1, rr.findTerminalAt(0).size());
			Assert.assertEquals(false, rr.getIsEmptyRule());
			Assert.assertEquals(false, rr.getIsSkipRule());
			Assert.assertEquals(RuntimeRuleKind.TERMINAL, rr.getKind());
			Assert.assertEquals("a",rr.getName());
			Assert.assertEquals("a",rr.getNodeTypeName());
			Assert.assertEquals(16,rr.getPatternFlags());
			Assert.assertEquals(null,rr.getRhs());
			Assert.assertEquals(-1,rr.getRhsIndexOf(rr));
			Assert.assertEquals(null,rr.getRhsItem(0));
			Assert.assertEquals(1,rr.getRuleNumber());
			Assert.assertEquals(null,rr.getRuleThatIsEmpty());
			Assert.assertEquals(null,rr.getSeparator());
			Assert.assertEquals("a",rr.getTerminalPatternText());

		} catch (RelationNotFoundException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void concatination() {
		try {
			RuntimeRuleSetBuilder runtimeRules = new RuntimeRuleSetBuilder();

			GrammarBuilder gb = new GrammarBuilder(new Namespace("test"), "Test");
			gb.rule("S").concatenation(new TerminalLiteral("a"), new TerminalLiteral("b"), new TerminalLiteral("c"));
			Grammar grammar = gb.get();
			Converter c = new Converter(runtimeRules);
			c.transformLeft2Right(Grammar2RuntimeRuleSet.class, grammar);

			RuntimeRule term_a = runtimeRules.getRuntimeRule(new TerminalLiteral("a"));
			RuntimeRule term_b = runtimeRules.getRuntimeRule(new TerminalLiteral("b"));
			RuntimeRule term_c = runtimeRules.getRuntimeRule(new TerminalLiteral("c"));
			RuntimeRule rr = runtimeRules.getRuntimeRule(grammar.findRule("S"));

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
			Assert.assertEquals("S",rr.getName());
			Assert.assertEquals("S",rr.getNodeTypeName());
			Assert.assertEquals(16,rr.getPatternFlags());
			Assert.assertNotNull(rr.getRhs());
			Assert.assertEquals(0,rr.getRhsIndexOf(term_a));
			Assert.assertEquals(1,rr.getRhsIndexOf(term_b));
			Assert.assertEquals(2,rr.getRhsIndexOf(term_c));
			Assert.assertEquals(term_a,rr.getRhsItem(0));
			Assert.assertEquals(term_b,rr.getRhsItem(1));
			Assert.assertEquals(term_c,rr.getRhsItem(2));
			Assert.assertEquals(0,rr.getRuleNumber());
			Assert.assertEquals(null,rr.getRuleThatIsEmpty());
			Assert.assertEquals(rr.getRhsItem(1),rr.getSeparator());
			Assert.assertEquals("S",rr.getTerminalPatternText());

		} catch (RelationNotFoundException | RuleNotFoundException e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void choice() {
		try {
			RuntimeRuleSetBuilder runtimeRules = new RuntimeRuleSetBuilder();

			GrammarBuilder gb = new GrammarBuilder(new Namespace("test"), "Test");
			gb.rule("S").choice(new TerminalLiteral("a"), new TerminalLiteral("b"), new TerminalLiteral("c"));
			Grammar grammar = gb.get();
			Converter c = new Converter(runtimeRules);
			c.transformLeft2Right(Grammar2RuntimeRuleSet.class, grammar);

			RuntimeRule term_a = runtimeRules.getRuntimeRule(new TerminalLiteral("a"));
			RuntimeRule term_b = runtimeRules.getRuntimeRule(new TerminalLiteral("b"));
			RuntimeRule term_c = runtimeRules.getRuntimeRule(new TerminalLiteral("c"));
			RuntimeRule rr = runtimeRules.getRuntimeRule(grammar.findRule("S"));

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
			Assert.assertEquals("S",rr.getName());
			Assert.assertEquals("S",rr.getNodeTypeName());
			Assert.assertEquals(16,rr.getPatternFlags());
			Assert.assertNotNull(rr.getRhs());
			Assert.assertEquals(0,rr.getRhsIndexOf(term_a));
			Assert.assertEquals(1,rr.getRhsIndexOf(term_b));
			Assert.assertEquals(2,rr.getRhsIndexOf(term_c));
			Assert.assertEquals(term_a,rr.getRhsItem(0));
			Assert.assertEquals(term_b,rr.getRhsItem(1));
			Assert.assertEquals(term_c,rr.getRhsItem(2));
			Assert.assertEquals(0,rr.getRuleNumber());
			Assert.assertEquals(null,rr.getRuleThatIsEmpty());
			Assert.assertEquals(rr.getRhsItem(1),rr.getSeparator());
			Assert.assertEquals("S",rr.getTerminalPatternText());

		} catch (RelationNotFoundException | RuleNotFoundException e) {
			Assert.fail(e.getMessage());
		}
	}
	
	@Test
	public void multi_0_m1() {
		try {
			RuntimeRuleSetBuilder runtimeRules = new RuntimeRuleSetBuilder();

			GrammarBuilder gb = new GrammarBuilder(new Namespace("test"), "Test");
			gb.rule("S").multi(0,-1,new TerminalLiteral("a"));
			Grammar grammar = gb.get();
			Converter c = new Converter(runtimeRules);
			c.transformLeft2Right(Grammar2RuntimeRuleSet.class, grammar);

			RuntimeRule term_a = runtimeRules.getRuntimeRule(new TerminalLiteral("a"));
			RuntimeRule rr = runtimeRules.getRuntimeRule(grammar.findAllRule("S"));
			RuntimeRule empty = runtimeRules.getRuntimeRuleSet().getEmptyRule(rr);
			
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
			Assert.assertEquals("S",rr.getName());
			Assert.assertEquals("S",rr.getNodeTypeName());
			Assert.assertEquals(16,rr.getPatternFlags());
			Assert.assertNotNull(rr.getRhs());
			Assert.assertEquals(0,rr.getRhsIndexOf(term_a));
			Assert.assertEquals(term_a,rr.getRhsItem(0));
			Assert.assertEquals(0,rr.getRuleNumber());
			Assert.assertEquals(null,rr.getRuleThatIsEmpty());
//			Assert.assertEquals(null,rr.getSeparator());
			Assert.assertEquals("S",rr.getTerminalPatternText());

		} catch (RelationNotFoundException | RuleNotFoundException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Test
	public void multi_1_m1() {
		try {
			RuntimeRuleSetBuilder runtimeRules = new RuntimeRuleSetBuilder();

			GrammarBuilder gb = new GrammarBuilder(new Namespace("test"), "Test");
			gb.rule("S").multi(1,-1,new TerminalLiteral("a"));
			Grammar grammar = gb.get();
			Converter c = new Converter(runtimeRules);
			c.transformLeft2Right(Grammar2RuntimeRuleSet.class, grammar);

			RuntimeRule term_a = runtimeRules.getRuntimeRule(new TerminalLiteral("a"));
			RuntimeRule rr = runtimeRules.getRuntimeRule(grammar.findAllRule("S"));
			RuntimeRule empty = runtimeRules.getRuntimeRuleSet().getEmptyRule(rr);
			
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
			Assert.assertEquals("S",rr.getName());
			Assert.assertEquals("S",rr.getNodeTypeName());
			Assert.assertEquals(16,rr.getPatternFlags());
			Assert.assertNotNull(rr.getRhs());
			Assert.assertEquals(0,rr.getRhsIndexOf(term_a));
			Assert.assertEquals(term_a,rr.getRhsItem(0));
			Assert.assertEquals(0,rr.getRuleNumber());
			Assert.assertEquals(null,rr.getRuleThatIsEmpty());
//			Assert.assertEquals(null,rr.getSeparator());
			Assert.assertEquals("S",rr.getTerminalPatternText());

		} catch (RelationNotFoundException | RuleNotFoundException e) {
			Assert.fail(e.getMessage());
		}
	}

}
