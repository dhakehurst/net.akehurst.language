package net.akehurst.language.grammar.parser.converter;

import org.junit.Assert;
import org.junit.Test;

import net.akehurst.language.api.grammar.GrammarRuleNotFoundException;
import net.akehurst.language.api.grammar.RuleItem;
import net.akehurst.language.grammar.parser.runtime.RuntimeRule;
import net.akehurst.language.grammar.parser.runtime.RuntimeRuleSetBuilder;
import net.akehurst.language.ogl.semanticStructure.ChoiceSimpleDefault;
import net.akehurst.language.ogl.semanticStructure.ConcatenationDefault;
import net.akehurst.language.ogl.semanticStructure.GrammarDefault;
import net.akehurst.language.ogl.semanticStructure.GrammarBuilderDefault;
import net.akehurst.language.ogl.semanticStructure.GroupDefault;
import net.akehurst.language.ogl.semanticStructure.MultiDefault;
import net.akehurst.language.ogl.semanticStructure.NamespaceDefault;
import net.akehurst.language.ogl.semanticStructure.TerminalLiteralDefault;

public class test_Converter {

    @Test
    public void createVirtualRule_group1() throws GrammarRuleNotFoundException {

        final RuntimeRuleSetBuilder builder = new RuntimeRuleSetBuilder();
        final Converter c = new Converter(builder);

        final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
        b.rule("S").concatenation(new GroupDefault(new ChoiceSimpleDefault(new ConcatenationDefault(new TerminalLiteralDefault("a")))));
        final GrammarDefault g = b.get();

        final ConcatenationDefault concatenation = ((ChoiceSimpleDefault) g.findAllRule("S").getRhs()).getAlternative().get(0);
        final GroupDefault group = (GroupDefault) concatenation.getItem().get(0);
        final RuntimeRule rr = c.createVirtualRule(group);

        Assert.assertEquals("$S.group.0.0.0", rr.getName());

        builder.createRuntimeRuleSet(0);
        final RuleItem original = builder.getRuntimeRuleSet().getOriginalItem(rr, g);
        Assert.assertEquals(group, original);

    }

    @Test
    public void createVirtualRule_group2() throws GrammarRuleNotFoundException {

        final RuntimeRuleSetBuilder builder = new RuntimeRuleSetBuilder();
        final Converter c = new Converter(builder);

        final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
        b.rule("S").concatenation(new TerminalLiteralDefault("a"), new GroupDefault(new ChoiceSimpleDefault(new ConcatenationDefault(new TerminalLiteralDefault("a")))));
        final GrammarDefault g = b.get();

        final ConcatenationDefault concatenation = ((ChoiceSimpleDefault) g.findAllRule("S").getRhs()).getAlternative().get(0);
        final GroupDefault group = (GroupDefault) concatenation.getItem().get(1);
        final RuntimeRule rr = c.createVirtualRule(group);

        Assert.assertEquals("$S.group.0.0.1", rr.getName());

        builder.createRuntimeRuleSet(0);
        final RuleItem original = builder.getRuntimeRuleSet().getOriginalItem(rr, g);
        Assert.assertEquals(group, original);

    }

    @Test
    public void createVirtualRule_concatination() throws GrammarRuleNotFoundException {

        final RuntimeRuleSetBuilder builder = new RuntimeRuleSetBuilder();
        final Converter c = new Converter(builder);

        final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
        b.rule("S").concatenation(new TerminalLiteralDefault("a"));
        final GrammarDefault g = b.get();

        final ConcatenationDefault concatenation = ((ChoiceSimpleDefault) g.findAllRule("S").getRhs()).getAlternative().get(0);
        final RuntimeRule rr = c.createVirtualRule(concatenation);

        Assert.assertEquals("$S.concatenation.0.0", rr.getName());

        builder.createRuntimeRuleSet(0);
        final RuleItem original = builder.getRuntimeRuleSet().getOriginalItem(rr, g);
        Assert.assertEquals(concatenation, original);

    }

    @Test
    public void createVirtualRule_multi() throws GrammarRuleNotFoundException {

        final RuntimeRuleSetBuilder builder = new RuntimeRuleSetBuilder();
        final Converter c = new Converter(builder);
        // builder.createRuntimeRuleSet(5); needed if multi has min==0

        final GrammarBuilderDefault b = new GrammarBuilderDefault(new NamespaceDefault("test"), "Test");
        b.rule("S").choice(new MultiDefault(1, -1, new TerminalLiteralDefault("a")));
        final GrammarDefault g = b.get();

        final ConcatenationDefault concatenation = ((ChoiceSimpleDefault) g.findAllRule("S").getRhs()).getAlternative().get(0);
        final MultiDefault multi = (MultiDefault) concatenation.getItem().get(0);
        final RuntimeRule rr = c.createVirtualRule(multi);

        Assert.assertEquals("$S.multi.0.0.0", rr.getName());

        builder.createRuntimeRuleSet(0);
        final RuleItem original = builder.getRuntimeRuleSet().getOriginalItem(rr, g);
        Assert.assertEquals(multi, original);

    }

}
