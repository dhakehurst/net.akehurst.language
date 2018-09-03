package net.akehurst.language.ogl.runtime.converter

import net.akehurst.language.api.grammar.Grammar
import net.akehurst.language.ogl.runtime.structure.RuntimeRule
import net.akehurst.language.ogl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.ogl.runtime.structure.RuntimeRuleSetBuilder

class Converter(val grammar: Grammar) {

    val builder: RuntimeRuleSetBuilder= RuntimeRuleSetBuilder()

    fun transform(): RuntimeRuleSet {
        //TODO:
        return this.builder.ruleSet()
    }
}