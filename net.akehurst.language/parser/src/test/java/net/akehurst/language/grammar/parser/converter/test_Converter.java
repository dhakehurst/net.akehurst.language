package net.akehurst.language.grammar.parser.converter;

import org.junit.Assert;
import org.junit.Test;

import net.akehurst.language.core.grammar.IRuleItem;
import net.akehurst.language.core.grammar.RuleNotFoundException;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleSetBuilder;
import net.akehurst.language.ogl.semanticStructure.ChoiceSimple;
import net.akehurst.language.ogl.semanticStructure.Concatenation;
import net.akehurst.language.ogl.semanticStructure.Grammar;
import net.akehurst.language.ogl.semanticStructure.GrammarBuilder;
import net.akehurst.language.ogl.semanticStructure.Group;
import net.akehurst.language.ogl.semanticStructure.Multi;
import net.akehurst.language.ogl.semanticStructure.Namespace;
import net.akehurst.language.ogl.semanticStructure.TerminalLiteral;

public class test_Converter {

	@Test
	public void createVirtualRule_group1() throws RuleNotFoundException {

		final RuntimeRuleSetBuilder builder = new RuntimeRuleSetBuilder();
		final Converter c = new Converter(builder);

		final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("S").concatenation(new Group(new ChoiceSimple(new Concatenation(new TerminalLiteral("a")))));
		final Grammar g = b.get();

		final Concatenation concatenation = ((ChoiceSimple) g.findAllRule("S").getRhs()).getAlternative().get(0);
		final Group group = (Group) concatenation.getItem().get(0);
		final RuntimeRule rr = c.createVirtualRule(group);

		Assert.assertEquals("$S.group.0.0.0", rr.getName());

		builder.createRuntimeRuleSet(0);
		final IRuleItem original = builder.getRuntimeRuleSet().getOriginalItem(rr, g);
		Assert.assertEquals(group, original);

	}

	@Test
	public void createVirtualRule_group2() throws RuleNotFoundException {

		final RuntimeRuleSetBuilder builder = new RuntimeRuleSetBuilder();
		final Converter c = new Converter(builder);

		final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("S").concatenation(new TerminalLiteral("a"), new Group(new ChoiceSimple(new Concatenation(new TerminalLiteral("a")))));
		final Grammar g = b.get();

		final Concatenation concatenation = ((ChoiceSimple) g.findAllRule("S").getRhs()).getAlternative().get(0);
		final Group group = (Group) concatenation.getItem().get(1);
		final RuntimeRule rr = c.createVirtualRule(group);

		Assert.assertEquals("$S.group.0.0.1", rr.getName());

		builder.createRuntimeRuleSet(0);
		final IRuleItem original = builder.getRuntimeRuleSet().getOriginalItem(rr, g);
		Assert.assertEquals(group, original);

	}

	@Test
	public void createVirtualRule_concatination() throws RuleNotFoundException {

		final RuntimeRuleSetBuilder builder = new RuntimeRuleSetBuilder();
		final Converter c = new Converter(builder);

		final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("S").concatenation(new TerminalLiteral("a"));
		final Grammar g = b.get();

		final Concatenation concatenation = ((ChoiceSimple) g.findAllRule("S").getRhs()).getAlternative().get(0);
		final RuntimeRule rr = c.createVirtualRule(concatenation);

		Assert.assertEquals("$S.concatenation.0.0", rr.getName());

		builder.createRuntimeRuleSet(0);
		final IRuleItem original = builder.getRuntimeRuleSet().getOriginalItem(rr, g);
		Assert.assertEquals(concatenation, original);

	}

	@Test
	public void createVirtualRule_multi() throws RuleNotFoundException {

		final RuntimeRuleSetBuilder builder = new RuntimeRuleSetBuilder();
		final Converter c = new Converter(builder);
		// builder.createRuntimeRuleSet(5); needed if multi has min==0

		final GrammarBuilder b = new GrammarBuilder(new Namespace("test"), "Test");
		b.rule("S").choice(new Multi(1, -1, new TerminalLiteral("a")));
		final Grammar g = b.get();

		final Concatenation concatenation = ((ChoiceSimple) g.findAllRule("S").getRhs()).getAlternative().get(0);
		final Multi multi = (Multi) concatenation.getItem().get(0);
		final RuntimeRule rr = c.createVirtualRule(multi);

		Assert.assertEquals("$S.multi.0.0.0", rr.getName());

		builder.createRuntimeRuleSet(0);
		final IRuleItem original = builder.getRuntimeRuleSet().getOriginalItem(rr, g);
		Assert.assertEquals(multi, original);

	}

}
