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

import net.akehurst.language.agl.api.runtime.Rule
import net.akehurst.language.agl.api.runtime.RuleSet
import net.akehurst.language.agl.api.runtime.RuleSetBuilder
import net.akehurst.language.agl.api.runtime.RuntimeRuleChoiceKind
import net.akehurst.language.collections.lazyMutableMapNonNull

@DslMarker
internal annotation class RuntimeRuleSetDslMarker

fun runtimeRuleSet(init: RuntimeRuleSetBuilder2.() -> Unit): RuleSet {
    val b = RuntimeRuleSetBuilder2()
    b.init()
    return b.build()
}

@RuntimeRuleSetDslMarker
class RuntimeRuleSetBuilder2 : RuleSetBuilder {

    internal val runtimeRuleSet = RuntimeRuleSet(RuntimeRuleSet.nextRuntimeRuleSetNumber++)

    private val _ruleBuilders = lazyMutableMapNonNull<String, MutableList<RuntimeRuleBuilder>> { mutableListOf() }

    internal fun build(): RuntimeRuleSet {
        if (this.runtimeRuleSet.runtimeRules.isEmpty()) {
            val ruleMap = this._ruleBuilders.values.flatMapIndexed { ruleNumber, it ->
                it.mapIndexedNotNull { optionNumber, rb ->
                    val rr = rb.buildRule(ruleNumber, optionNumber)
                    rb.name?.let { Pair(it, rr) }
                }
            }.associate { it }
            val rules = this._ruleBuilders.values.flatMapIndexed { ruleNumber, it ->
                it.mapIndexed { optionNumber, rb ->
                    rb.buildRhs(ruleMap)
                    rb.rule!!
                }
            }
            this.runtimeRuleSet.setRules(rules)
        }
        return this.runtimeRuleSet
    }

    private fun _rule(
        name: String,
        isSkip: Boolean,
        init: RuntimeRuleRhsBuilder.() -> Unit,
        build: (ruleMap: Map<String, Int>, itemsRef: List<RuntimeRuleRef>) -> RuntimeRuleRhs
    ) {
        val rhsB = RuntimeRuleRhsBuilder(this)
        rhsB.init()
        val rb = RuntimeRuleBuilder(this, name, RuntimeRuleChoiceKind.NONE, isSkip) { ruleMap ->
            build(ruleMap, rhsB.itemRefs)
        }
        this._ruleBuilders[name].add(rb)
    }

    fun literal(name: String?, value: String, isSkip: Boolean = false) {
        val tag = name ?: "'$value'"
        if (this._ruleBuilders.containsKey(tag)) {
            //do nothing
        } else {
            val rb = RuntimeRuleBuilder(this, name, RuntimeRuleChoiceKind.NONE, isSkip) {
                RuntimeRuleRhsLiteral(value)
            }
            this._ruleBuilders[tag].add(rb)
        }
    }

    fun pattern(name: String?, pattern: String, isSkip: Boolean = false) {
        val tag = name ?: "\"$pattern\""
        if (this._ruleBuilders.containsKey(tag)) {
            //do nothing
        } else {
            val rb = RuntimeRuleBuilder(this, name, RuntimeRuleChoiceKind.NONE, isSkip) {
                RuntimeRuleRhsPattern(pattern)
            }
            this._ruleBuilders[tag].add(rb)
        }
    }

    fun emptyRule(ruleThatIsEmptyTag: String, isSkip: Boolean = false): String {
        val tag = "§empty." + ruleThatIsEmptyTag
        if (this._ruleBuilders.containsKey(tag)) {
            //do nothing
        } else {
            val rb = RuntimeRuleBuilder(this, tag, RuntimeRuleChoiceKind.NONE, isSkip) { ruleMap ->
                val rn = ruleMap[ruleThatIsEmptyTag] ?: error("Rule with tag $tag not found")
                RuntimeRuleRhsEmpty(rn)
            }
            this._ruleBuilders[tag].add(rb)
        }
        return tag
    }

    fun choice(name: String, choiceKind: RuntimeRuleChoiceKind, isSkip: Boolean = false, init: RuntimeRuleChoiceBuilder.() -> Unit)  {
        val b = RuntimeRuleChoiceBuilder(this)
        b.init()
        b.choices.forEach { itemRefs ->
            val rb = RuntimeRuleBuilder(this, name, choiceKind, isSkip) { ruleMap ->
                val items = itemRefs.map { ruleMap[it.tag] ?: error("Rule with tag $name not found") }
                RuntimeRuleRhsConcatenation(items)
            }
            this._ruleBuilders[name].add(rb)
        }
    }

    fun concatenation(name: String, isSkip: Boolean = false, init: RuntimeRuleRhsBuilder.() -> Unit) =
        _rule(name, isSkip, init) { ruleMap, itemRefs ->
            val items = itemRefs.map { ruleMap[it.tag] ?: error("Rule with tag $name not found") }
            RuntimeRuleRhsConcatenation(runtimeRuleSet, items)
        }

    fun multi(name: String, min: Int, max: Int, itemRef: String, isSkip: Boolean = false) {
        if (min == 0) {
            val erName = emptyRule(name)
            // the empty option is lower priority
            _rule(name, isSkip, {}) { ruleMap, itemRefs ->
                val er = ruleMap[erName] ?: error("Rule with tag $erName not found")
                RuntimeRuleRhsEmpty(runtimeRuleSet, er)
            }
            _rule(name, isSkip, {}) { ruleMap, _ ->
                val item = ruleMap[itemRef] ?: error("Rule with tag $name not found")
                RuntimeRuleRhsListSimple(runtimeRuleSet, min, max, item)
            }
        } else {
            _rule(name, isSkip, {}) { ruleMap, _ ->
                val item = ruleMap[itemRef] ?: error("Rule with tag $name not found")
                RuntimeRuleRhsListSimple(runtimeRuleSet, min, max, item)
            }
        }
    }

    fun sList(name: String, min: Int, max: Int, itemRef: String, sepRef: String, isSkip: Boolean = false) {
        if (min == 0) {
            val erName = emptyRule(name)
            _rule(name, isSkip, {}) { ruleMap, itemRefs ->
                val er = ruleMap[erName] ?: error("Rule with tag $erName not found")
                RuntimeRuleRhsEmpty(runtimeRuleSet, er)
            }
            _rule(name, isSkip, {}) { ruleMap, _ ->
                val item = ruleMap[itemRef] ?: error("Rule with tag $name not found")
                val sep = ruleMap[sepRef] ?: error("Rule with tag $name not found")
                RuntimeRuleRhsListSeparated(runtimeRuleSet, min, max, item, sep)
            }
        } else {
            _rule(name, isSkip, {}) { ruleMap, _ ->
                val item = ruleMap[itemRef] ?: error("Rule with tag $name not found")
                val sep = ruleMap[sepRef] ?: error("Rule with tag $name not found")
                RuntimeRuleRhsListSeparated(runtimeRuleSet, min, max, item, sep)
            }
        }
    }

    fun embedded(name: String, embeddedRuleSet: RuleSet, startRule: Rule, isSkip: Boolean = false) {
        val rb = RuntimeRuleBuilder(this, name, RuntimeRuleChoiceKind.NONE, isSkip) {
            RuntimeRuleRhsEmbedded(embeddedRuleSet as RuntimeRuleSet, startRule as RuntimeRule)
        }
        this._ruleBuilders[name].add(rb)
    }
}

@RuntimeRuleSetDslMarker
internal class RuntimeRuleBuilder(
    val rrsb: RuntimeRuleSetBuilder2,
    val name: String?,
    val choiceKind: RuntimeRuleChoiceKind,
    val isSkip: Boolean,
    val rhsBuilder: (ruleMap: Map<String, RuntimeRule>) -> RuntimeRuleRhs
) {
    var rule: RuntimeRule? = null

    fun buildRule(number: Int, option: Int): RuntimeRule {
        if (null == this.rule) {
            this.rule = RuntimeRule(rrsb.runtimeRuleSet.number, number, option, name, choiceKind, isSkip)
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
class RuntimeRuleRhsBuilder(
    val rrsb: RuntimeRuleSetBuilder2
) {

    internal val itemRefs = mutableListOf<RuntimeRuleRef>()

    fun empty(ruleName:String) {
        val tag = this.rrsb.emptyRule(ruleName)
        itemRefs.add( RuntimeRuleRef(tag))
        check(1==itemRefs.size) { "'empty' must be the only item in a rhs" }
    }

    fun literal(value: String) {
        val tag = "'$value'"
        this.rrsb.literal(null, value)
        itemRefs.add( RuntimeRuleRef(tag))
    }

    fun pattern(pattern: String) {
        val tag = "\"$pattern\""
        this.rrsb.pattern(tag, pattern)
        itemRefs.add( RuntimeRuleRef(tag))
    }

    fun ref(name: String) {
        val ref = RuntimeRuleRef(name)
        itemRefs.add(ref)
    }
}

internal class RuntimeRuleChoiceBuilder(
    val rrsb: RuntimeRuleSetBuilder2,
) {
    val choices = mutableListOf<List<RuntimeRuleRef>>()

    fun literal(value: String) {
        val tag = "'$value'"
        this.rrsb.literal(null, value)
        choices.add( listOf(RuntimeRuleRef(tag)))
    }

    fun pattern(pattern: String) {
        val tag = "\"$pattern\""
        this.rrsb.pattern(tag, pattern)
        choices.add( listOf(RuntimeRuleRef(tag)))
    }

    fun ref(name: String) {
        val ref = RuntimeRuleRef(name)
        choices.add(listOf( ref))
    }

    fun concatenation(init: RuntimeRuleRhsBuilder.() -> Unit) {
        val b = RuntimeRuleRhsBuilder(rrsb)
        b.init()
        choices.add(b.itemRefs)
    }
}