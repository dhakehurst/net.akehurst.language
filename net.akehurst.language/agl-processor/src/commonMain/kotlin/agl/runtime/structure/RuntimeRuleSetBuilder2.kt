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

import net.akehurst.language.agl.api.runtime.*


internal fun runtimeRuleSet(init: RuntimeRuleSetBuilder2.() -> Unit): RuntimeRuleSet {
    val b = RuntimeRuleSetBuilder2()
    b.init()
    return b.build()
}

internal interface RuntimeRuleRhsBuilder {
    fun buildRhs(ruleMap: Map<String, RuntimeRule>): RuntimeRuleRhs
}

@RuntimeRuleSetDslMarker
internal class RuntimeRuleSetBuilder2 : RuleSetBuilder {

    internal val runtimeRuleSet = RuntimeRuleSet(RuntimeRuleSet.nextRuntimeRuleSetNumber++)

    private val _ruleBuilders = mutableMapOf<String, RuntimeRuleBuilder>()

    internal fun build(): RuntimeRuleSet {
        if (this.runtimeRuleSet.runtimeRules.isEmpty()) {
            val ruleMap = this._ruleBuilders.values.mapIndexedNotNull { ruleNumber, rb ->
                val rr = rb.buildRule(ruleNumber)
                rb.name?.let { Pair(it, rr) }
            }.associate { it }
            val rules = this._ruleBuilders.values.map { rb ->
                rb.buildRhs(ruleMap)
                rb.rule!!
            }
            this.runtimeRuleSet.setRules(rules)
        }
        return this.runtimeRuleSet
    }

    private fun _rule(
        name: String,
        isSkip: Boolean,
        build: (ruleMap: Map<String, RuntimeRule>) -> RuntimeRuleRhs
    ) {
        val rb = RuntimeRuleBuilder(this, name, isSkip) { ruleMap ->
            build(ruleMap)
        }
        this._ruleBuilders[name] = rb
    }

    fun literal(name: String?, value: String, isSkip: Boolean = false) {
        val tag = name ?: "'$value'"
        if (this._ruleBuilders.containsKey(tag)) {
            //do nothing
        } else {
            val rb = RuntimeRuleBuilder(this, name, isSkip) {
                RuntimeRuleRhsLiteral(value)
            }
            this._ruleBuilders[tag] = rb
        }
    }

    fun pattern(name: String?, pattern: String, isSkip: Boolean = false) {
        val tag = name ?: "\"$pattern\""
        if (this._ruleBuilders.containsKey(tag)) {
            //do nothing
        } else {
            val rb = RuntimeRuleBuilder(this, name, isSkip) {
                RuntimeRuleRhsPattern(pattern)
            }
            this._ruleBuilders[tag] = rb
        }
    }

    fun emptyRule(ruleThatIsEmptyTag: String, isSkip: Boolean = false): String {
        val tag = "§empty." + ruleThatIsEmptyTag
        if (this._ruleBuilders.containsKey(tag)) {
            //do nothing
        } else {
            val rb = RuntimeRuleBuilder(this, tag, isSkip) { ruleMap ->
                val rn = ruleMap[ruleThatIsEmptyTag] ?: error("Rule with tag $tag not found")
                RuntimeRuleRhsEmpty()
            }
            this._ruleBuilders[tag] = rb
        }
        return tag
    }

    override fun choiceLongest(ruleName: String, isSkip: Boolean, init: ChoiceBuilder.() -> Unit) = choice(ruleName, RuntimeRuleChoiceKind.LONGEST_PRIORITY, isSkip, init)

    override fun choicePriority(ruleName: String, isSkip: Boolean, init: ChoiceBuilder.() -> Unit) = choice(ruleName, RuntimeRuleChoiceKind.PRIORITY_LONGEST, isSkip, init)

    fun choice(name: String, choiceKind: RuntimeRuleChoiceKind, isSkip: Boolean = false, init: ChoiceBuilder.() -> Unit) {
        val b = RuntimeRuleChoiceBuilder(this)
        b.init()
        val rb = RuntimeRuleBuilder(this, name, isSkip) { ruleMap ->
            val options = b.choices.map { rhsB -> rhsB.buildRhs(ruleMap) }
            RuntimeRuleRhsChoice(choiceKind, options)
        }
        this._ruleBuilders[name] = rb
    }

    override fun concatenation(ruleName: String, isSkip: Boolean, init: ConcatenationBuilder.() -> Unit) {
        val b = RuntimeRuleConcatenationBuilder(this)
        b.init()
        _rule(ruleName, isSkip) { ruleMap ->
            b.buildRhs(ruleMap)
        }
    }

    fun multi(ruleName: String, min: Int, max: Int, itemRef: String, isSkip: Boolean = false) {
        _rule(ruleName, isSkip) { ruleMap ->
            val item = ruleMap[itemRef] ?: error("Rule with tag $ruleName not found")
            RuntimeRuleRhsListSimple(min, max, item)
        }
    }

    fun sList(ruleName: String, min: Int, max: Int, itemRef: String, sepRef: String, isSkip: Boolean = false) {
        _rule(ruleName, isSkip) { ruleMap ->
            val item = ruleMap[itemRef] ?: error("Rule with tag $ruleName not found")
            val sep = ruleMap[sepRef] ?: error("Rule with tag $ruleName not found")
            RuntimeRuleRhsListSeparated(min, max, item, sep)
        }
    }

    fun embedded(ruleName: String, embeddedRuleSet: RuleSet, startRule: Rule, isSkip: Boolean = false) {
        val rb = RuntimeRuleBuilder(this, ruleName, isSkip) {
            RuntimeRuleRhsEmbedded(embeddedRuleSet as RuntimeRuleSet, startRule as RuntimeRule)
        }
        this._ruleBuilders[ruleName] = rb
    }
}

@RuntimeRuleSetDslMarker
internal class RuntimeRuleBuilder(
    val rrsb: RuntimeRuleSetBuilder2,
    val name: String?,
    val isSkip: Boolean,
    val rhsBuilder: (ruleMap: Map<String, RuntimeRule>) -> RuntimeRuleRhs
) {
    var rule: RuntimeRule? = null

    fun buildRule(number: Int): RuntimeRule {
        if (null == this.rule) {
            this.rule = RuntimeRule(rrsb.runtimeRuleSet.number, number, name, isSkip)
        }
        return this.rule!!
    }

    fun buildRhs(ruleMap: Map<String, RuntimeRule>) {
        val rhs = rhsBuilder.invoke(ruleMap)
        this.rule!!.setRhs(rhs)
    }
}

internal data class RuntimeRuleRef(val tag: String)

@RuntimeRuleSetDslMarker
internal class RuntimeRuleConcatenationBuilder(
    val rrsb: RuntimeRuleSetBuilder2
) : ConcatenationBuilder, RuntimeRuleRhsBuilder {

    internal val itemRefs = mutableListOf<RuntimeRuleRef>()

    override fun empty(ruleName: String) {
        val tag = this.rrsb.emptyRule(ruleName)
        itemRefs.add(RuntimeRuleRef(tag))
        check(1 == itemRefs.size) { "'empty' must be the only item in a rhs" }
    }

    override fun literal(value: String) {
        val tag = "'$value'"
        this.rrsb.literal(null, value)
        itemRefs.add(RuntimeRuleRef(tag))
    }

    override fun pattern(pattern: String) {
        val tag = "\"$pattern\""
        this.rrsb.pattern(tag, pattern)
        itemRefs.add(RuntimeRuleRef(tag))
    }

    override fun ref(name: String) {
        val ref = RuntimeRuleRef(name)
        itemRefs.add(ref)
    }

    override fun buildRhs(ruleMap: Map<String, RuntimeRule>): RuntimeRuleRhs {
        val items = itemRefs.map { ruleMap[it.tag] ?: error("Rule with tag ${it.tag} not found") }
        return RuntimeRuleRhsConcatenation(items)
    }
}

internal class RuntimeRuleChoiceBuilder(
    val rrsb: RuntimeRuleSetBuilder2,
) : ChoiceBuilder {

    val choices = mutableListOf<RuntimeRuleRhsBuilder>()

    fun empty(ruleName: String) = concatenation { empty(ruleName) }

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