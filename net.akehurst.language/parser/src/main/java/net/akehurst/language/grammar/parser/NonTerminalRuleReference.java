package net.akehurst.language.grammar.parser;

import java.util.Objects;

import net.akehurst.language.api.grammar.Grammar;
import net.akehurst.language.api.grammar.GrammarRuleNotFoundException;
import net.akehurst.language.api.grammar.NonTerminal;
import net.akehurst.language.api.grammar.Rule;
import net.akehurst.language.api.grammar.RuleItem;
import net.akehurst.language.ogl.semanticStructure.GramarVisitable;
import net.akehurst.language.ogl.semanticStructure.GrammarVisitor;

public class NonTerminalRuleReference implements NonTerminal, GramarVisitable {

    private final Grammar grammar;

    private final String ruleName;

    public NonTerminalRuleReference(final Grammar grammar, final String ruleName) {
        this.grammar = grammar;
        this.ruleName = ruleName;
    }

    @Override
    public RuleItem getSubItem(final int i) {
        // Terminals and NonTerminals do not have sub items
        return null;
    }

    @Override
    public Rule getOwningRule() {
        // there is no owning rule!
        return null;
    }

    @Override
    public Rule getReferencedRule() throws GrammarRuleNotFoundException {
        return this.grammar.findAllRule(this.ruleName);
    }

    @Override
    public String getName() {
        return this.ruleName;
    }

    // --- Visitable ---
    @Override
    public <T, E extends Throwable> T accept(final GrammarVisitor<T, E> visitor, final Object... arg) throws E {
        return visitor.visit(this, arg);
    }

    // --- Object ---
    @Override
    public int hashCode() {
        return this.ruleName.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj instanceof NonTerminal) {
            final NonTerminal other = (NonTerminal) obj;
            return Objects.equals(this.getName(), other.getName());
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return this.ruleName;
    }
}
