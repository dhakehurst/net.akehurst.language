/**
 * Copyright (C) 2018 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.akehurst.language.agl.runtime.structure

import net.akehurst.language.agl.api.language.base.QualifiedName
import net.akehurst.language.agl.api.runtime.*

internal fun runtimeRuleSet(qualifiedName: String = "Grammar", init: RuntimeRuleSetBuilder2.() -> Unit): RuntimeRuleSet {
    val b = RuntimeRuleSetBuilder2(QualifiedName(qualifiedName))
    b.init()
    return b.build()
}

internal interface RuntimeRuleRhsBuilder {
    fun buildRhs(rule: RuntimeRule, ruleMap: Map<String, RuntimeRule>): RuntimeRuleRhs
}

internal data class RuntimeRuleRef(val tag: String) {
    fun resolve(ruleMap: Map<String, RuntimeRule>) = when (tag) {
        RuntimeRuleSet.EMPTY_RULE_TAG -> RuntimeRuleSet.EMPTY
        RuntimeRuleSet.EMPTY_LIST_RULE_TAG -> RuntimeRuleSet.EMPTY_LIST
        else -> ruleMap[tag] ?: error("Rule with tag $tag not found")
    }
}

@RuntimeRuleSetDslMarker
internal class RuntimeRuleSetBuilder2(
    val qualifiedName: QualifiedName
) : RuleSetBuilder {

    private val ruleSetNumber = RuntimeRuleSet.nextRuntimeRuleSetNumber++
    private val _ruleBuilders = mutableMapOf<String, RuntimeRuleBuilder>()
    private val _precedenceRuleBuilders = mutableListOf<PrecedenceRuleBuilder>()

    internal fun build(): RuntimeRuleSet {
        val ruleMap = this._ruleBuilders.values.mapIndexed { ruleNumber, rb ->
            val rr = rb.buildRule(ruleNumber)
            Pair(rb.tag, rr)
        }
            .plus(Pair("<EMPTY>", RuntimeRuleSet.EMPTY))
            .plus(Pair("<EMPTY_LIST>", RuntimeRuleSet.EMPTY_LIST))
            .plus(Pair("<EOT>", RuntimeRuleSet.END_OF_TEXT))
            .associate { it }
        val rules = this._ruleBuilders.values.map { rb ->
            rb.buildRhs(ruleMap)
            rb.rule!!
        }
        val precRules = _precedenceRuleBuilders.map {
            it.build(ruleMap)
        }
        return RuntimeRuleSet(ruleSetNumber, qualifiedName, rules, precRules)
    }

    private fun _rule(
        name: String,
        isSkip: Boolean,
        isPseudo: Boolean,
        build: (rule: RuntimeRule, ruleMap: Map<String, RuntimeRule>) -> RuntimeRuleRhs
    ) {
        val tag = name
        val rb = RuntimeRuleBuilder(ruleSetNumber, name, tag, isSkip, isPseudo) { rule, ruleMap ->
            build(rule, ruleMap)
        }
        this._ruleBuilders[name] = rb
    }

    fun literal(literalUnescaped: String, isSkip: Boolean = false) = literal(null, literalUnescaped, isSkip)
    fun literal(name: String?, literalUnescaped: String, isSkip: Boolean = false) {
        val tag = name ?: "'$literalUnescaped'"
        if (this._ruleBuilders.containsKey(tag)) {
            //do nothing
        } else {
            val rb = RuntimeRuleBuilder(ruleSetNumber, name, tag, isSkip, false) { rule, _ ->
                RuntimeRuleRhsLiteral(rule, literalUnescaped)
            }
            this._ruleBuilders[tag] = rb
        }
    }

    fun pattern(value: String, isSkip: Boolean = false) = pattern(null, value, isSkip)
    fun pattern(name: String?, patternUnescaped: String, isSkip: Boolean = false) {
        val tag = name ?: "\"$patternUnescaped\""
        if (this._ruleBuilders.containsKey(tag)) {
            //do nothing
        } else {
            val rb = RuntimeRuleBuilder(ruleSetNumber, name, tag, isSkip, false) { rule, _ ->
                RuntimeRuleRhsPattern(rule, patternUnescaped)
            }
            this._ruleBuilders[tag] = rb
        }
    }

    override fun choiceLongest(ruleName: String, isSkip: Boolean, isPseudo: Boolean, init: ChoiceBuilder.() -> Unit) =
        choice(ruleName, RuntimeRuleChoiceKind.LONGEST_PRIORITY, isSkip, isPseudo, init)

    override fun choicePriority(ruleName: String, isSkip: Boolean, isPseudo: Boolean, init: ChoiceBuilder.() -> Unit) =
        choice(ruleName, RuntimeRuleChoiceKind.PRIORITY_LONGEST, isSkip, isPseudo, init)

    fun choice(name: String, choiceKind: RuntimeRuleChoiceKind, isSkip: Boolean = false, isPseudo: Boolean = false, init: ChoiceBuilder.() -> Unit) {
        val b = RuntimeRuleChoiceBuilder(this)
        b.init()
        val tag = name
        val rb = RuntimeRuleBuilder(ruleSetNumber, name, tag, isSkip, isPseudo) { rule, ruleMap ->
            val options = b.choices.map { rhsB -> rhsB.buildRhs(rule, ruleMap) }
            RuntimeRuleRhsChoice(rule, choiceKind, options)
        }
        this._ruleBuilders[name] = rb
    }

    override fun concatenation(ruleName: String, isSkip: Boolean, isPseudo: Boolean, init: ConcatenationBuilder.() -> Unit) {
        val b = RuntimeRuleConcatenationBuilder(this)
        b.init()
        _rule(ruleName, isSkip, isPseudo) { rule, ruleMap ->
            b.buildRhs(rule, ruleMap)
        }
    }

    override fun optional(ruleName: String, itemRef: String, isSkip: Boolean, isPseudo: Boolean) {
        _rule(ruleName, isSkip, isPseudo) { rule, ruleMap ->
            val item = RuntimeRuleRef(itemRef).resolve(ruleMap)
            RuntimeRuleRhsOptional(rule, item)
        }
    }

    override fun multi(ruleName: String, min: Int, max: Int, itemRef: String, isSkip: Boolean, isPseudo: Boolean) {
        _rule(ruleName, isSkip, isPseudo) { rule, ruleMap ->
            val item = RuntimeRuleRef(itemRef).resolve(ruleMap)
            RuntimeRuleRhsListSimple(rule, min, max, item)
        }
    }

    override fun sList(ruleName: String, min: Int, max: Int, itemRef: String, sepRef: String, isSkip: Boolean, isPseudo: Boolean) {
        _rule(ruleName, isSkip, isPseudo) { rule, ruleMap ->
            val item = RuntimeRuleRef(itemRef).resolve(ruleMap)
            val sep = RuntimeRuleRef(sepRef).resolve(ruleMap)
            RuntimeRuleRhsListSeparated(rule, min, max, item, sep)
        }
    }

    override fun embedded(ruleName: String, embeddedRuleSet: RuleSet, startRuleName: String, isSkip: Boolean, isPseudo: Boolean) {
        val startRule = (embeddedRuleSet as RuntimeRuleSet).findRuntimeRule(startRuleName)
        val tag = ruleName
        val rb = RuntimeRuleBuilder(ruleSetNumber, ruleName, tag, isSkip, false) { rule, _ ->
            RuntimeRuleRhsEmbedded(rule, embeddedRuleSet as RuntimeRuleSet, startRule as RuntimeRule)
        }
        this._ruleBuilders[ruleName] = rb
    }

    fun preferenceFor(precedenceContextRuleName: String, init: PrecedenceRuleBuilder.() -> Unit) {
        val b = PrecedenceRuleBuilder(precedenceContextRuleName)
        b.init()
        this._precedenceRuleBuilders.add(b)
    }
}

@RuntimeRuleSetDslMarker
internal class RuntimeRuleBuilder(
    val ruleSetNumber: Int,
    val name: String?,
    val tag: String,
    val isSkip: Boolean,
    val isPseudo: Boolean,
    val rhsBuilder: (rule: RuntimeRule, ruleMap: Map<String, RuntimeRule>) -> RuntimeRuleRhs
) {
    var rule: RuntimeRule? = null

    fun buildRule(number: Int): RuntimeRule {
        if (null == this.rule) {
            this.rule = RuntimeRule(ruleSetNumber, number, name, isSkip, isPseudo)
        }
        return this.rule!!
    }

    fun buildRhs(ruleMap: Map<String, RuntimeRule>) {
        val rhs = rhsBuilder.invoke(rule!!, ruleMap)
        this.rule!!.setRhs(rhs)
    }
}

@RuntimeRuleSetDslMarker
internal class RuntimeRuleConcatenationBuilder(
    val rrsb: RuntimeRuleSetBuilder2
) : ConcatenationBuilder, RuntimeRuleRhsBuilder {

    internal val itemRefs = mutableListOf<RuntimeRuleRef>()

    override fun empty() {
        itemRefs.add(RuntimeRuleRef(RuntimeRuleSet.EMPTY.tag))
        /* only used in testing no need to - if (Debug.CHECK) */ check(1 == itemRefs.size) { "'empty' must be the only item in a rhs" }
    }

    override fun emptyList() {
        itemRefs.add(RuntimeRuleRef(RuntimeRuleSet.EMPTY_LIST.tag))
        /* only used in testing no need to - if (Debug.CHECK) */ check(1 == itemRefs.size) { "'emptyList' must be the only item in a rhs" }
    }

    override fun literal(value: String) {
        val tag = "'$value'"
        this.rrsb.literal(null, value)
        itemRefs.add(RuntimeRuleRef(tag))
    }

    override fun pattern(pattern: String) {
        val tag = "\"$pattern\""
        this.rrsb.pattern(null, pattern)
        itemRefs.add(RuntimeRuleRef(tag))
    }

    override fun ref(name: String) {
        val ref = RuntimeRuleRef(name)
        itemRefs.add(ref)
    }

    override fun buildRhs(rule: RuntimeRule, ruleMap: Map<String, RuntimeRule>): RuntimeRuleRhs {
        val items = itemRefs.map { it.resolve(ruleMap) }
        return RuntimeRuleRhsConcatenation(rule, items)
    }
}

@RuntimeRuleSetDslMarker
internal class RuntimeRuleChoiceBuilder(
    val rrsb: RuntimeRuleSetBuilder2,
) : ChoiceBuilder {

    val choices = mutableListOf<RuntimeRuleRhsBuilder>()

    fun empty() = concatenation { empty() }

    override fun literal(value: String) = concatenation { literal(value) }

    override fun pattern(pattern: String) = concatenation { pattern(pattern) }

    override fun ref(name: String) = concatenation { ref(name) }

    override fun concatenation(init: ConcatenationBuilder.() -> Unit) = concat(init as RuntimeRuleConcatenationBuilder.() -> Unit)

    private fun concat(init: RuntimeRuleConcatenationBuilder.() -> Unit) {
        val b = RuntimeRuleConcatenationBuilder(rrsb)
        b.init()
        choices.add(b)
    }
}

@RuntimeRuleSetDslMarker
internal class PrecedenceRuleBuilder(
    val contextRuleName: String
) {

    data class Quad<out A, out B, out C, out D>(val first: A, val second: B, val third: C, val fourth: D)

    private val _rules = mutableListOf<Quad<String, Int, Set<String>, RuntimePreferenceRule.Assoc>>()

    /**
     * indicate that @param ruleName is left-associative
     */
    fun none(ruleName: String) {
        _rules.add(Quad(ruleName, 0, emptySet(), RuntimePreferenceRule.Assoc.NONE))
    }

    /**
     * indicate that @param ruleName is left-associative
     */
    fun left(ruleName: String, operatorRuleNames: Set<String>) {
        _rules.add(Quad(ruleName, 0, operatorRuleNames, RuntimePreferenceRule.Assoc.LEFT))
    }

    fun leftOption(ruleName: String, option: Int, operatorRuleNames: Set<String>) {
        _rules.add(Quad(ruleName, option, operatorRuleNames, RuntimePreferenceRule.Assoc.LEFT))
    }

    /**
     * indicate that @param ruleName is right-associative
     */
    fun right(ruleName: String, operatorRuleNames: Set<String>) {
        _rules.add(Quad(ruleName, 0, operatorRuleNames, RuntimePreferenceRule.Assoc.RIGHT))
    }

    fun rightOption(ruleName: String, option: Int, operatorRuleNames: Set<String>) {
        _rules.add(Quad(ruleName, option, operatorRuleNames, RuntimePreferenceRule.Assoc.RIGHT))
    }

    fun build(ruleMap: Map<String, RuntimeRule>): RuntimePreferenceRule {
        val contextRule = ruleMap[contextRuleName] ?: error("Cannot find rule named '$contextRuleName' as a context rule for precedence definitions")
        val rules = _rules.mapIndexed { idx, it ->
            val r = ruleMap[it.first] ?: error("Cannot find rule named '${it.first}' for target rule in precedence definitions")
            val ops = it.third.map { ruleMap[it] ?: error("Cannot find rule named '${it}' for operator in precedence definitions") }
            RuntimePreferenceRule.RuntimePreferenceOption(idx, r, it.second, ops.toSet(), it.fourth)
        }
        return RuntimePreferenceRule(contextRule, rules)
    }
}