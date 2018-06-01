package net.akehurst.language.grammar.parser.runtime;

import net.akehurst.language.api.grammar.Grammar;
import net.akehurst.language.ogl.semanticStructure.ChoiceAbstract;
import net.akehurst.language.ogl.semanticStructure.RuleDefault;

public class RuleForGroup extends RuleDefault {

    public RuleForGroup(final Grammar grammar, final String name, final ChoiceAbstract choice) {
        super(grammar, name);
        this.choice = choice;
    }

    private final ChoiceAbstract choice;

    @Override
    public ChoiceAbstract getRhs() {
        return this.choice;
    }

}
