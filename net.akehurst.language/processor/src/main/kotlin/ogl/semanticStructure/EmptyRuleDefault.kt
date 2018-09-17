package net.akehurst.language.ogl.semanticStructure

import net.akehurst.language.api.grammar.*
import net.akehurst.language.ogl.semanticStructure.RuleItemAbstract

class EmptyRuleDefault : RuleItemAbstract(), EmptyRule {

    override val name : String by lazy {
        "<empty>"
    }

    override val allTerminal: Set<Terminal> by lazy {
        emptySet<Terminal>()
    }

    override val allNonTerminal: Set<NonTerminal> by lazy {
        emptySet<NonTerminal>()
    }
    override fun setOwningRule(rule: Rule, indices: List<Int>) {
        this._owningRule = rule
        this.index = indices
    }

    override fun subItem(index: Int): RuleItem {
        throw GrammarRuleItemNotFoundException("subitem ${index} not found")
    }

    // --- GrammarVisitable ---

    override fun <T,A> accept(visitor: GrammarVisitor<T, A>, arg: A): T {
        return visitor.visit(this, arg);
    }
}