package net.akehurst.language.agl.api.runtime

import net.akehurst.language.agl.runtime.structure.runtimeRuleSet

interface RuleSet {

    companion object {
        fun build(init:RuleSetBuilder.()->Unit) : RuleSet = runtimeRuleSet(init)
    }

}

interface Rule {
}

@DslMarker
internal annotation class RuntimeRuleSetDslMarker

@RuntimeRuleSetDslMarker
interface RuleSetBuilder {

    fun concatenation(ruleName:String, isSkip: Boolean = false, init: ConcatenationBuilder.() -> Unit)
    fun choiceLongest(ruleName:String, isSkip: Boolean = false, init: ChoiceBuilder.() -> Unit)
    fun choicePriority(ruleName:String, isSkip: Boolean = false, init: ChoiceBuilder.() -> Unit)

}

@RuntimeRuleSetDslMarker
interface ConcatenationBuilder {
    fun empty(ruleName: String)
    fun literal(value: String)
    fun pattern(pattern: String)
    fun ref(name:String)
}

@RuntimeRuleSetDslMarker
interface ChoiceBuilder {
    fun concatenation(init: ConcatenationBuilder.() -> Unit)
    fun ref(ruleName: String)
    fun literal(value: String)
    fun pattern(pattern: String)
}