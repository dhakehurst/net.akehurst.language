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

import net.akehurst.language.agl.api.runtime.RuleSet
import net.akehurst.language.collections.lazyMutableMapNonNull

@DslMarker
internal annotation class RuntimeRuleSetDslMarker

internal fun runtimeRuleSet(init: RuntimeRuleSetBuilder2.() -> Unit): RuleSet {
    val b = RuntimeRuleSetBuilder2()
    b.init()
    return b.build()
}

@RuntimeRuleSetDslMarker
internal class RuntimeRuleSetBuilder2() {

    val runtimeRuleSet = RuntimeRuleSet(RuntimeRuleSet.nextRuntimeRuleSetNumber++)

    private val _ruleBuilders = lazyMutableMapNonNull<String, MutableList<RuntimeRuleBuilder>> { mutableListOf() }

    fun build(): RuntimeRuleSet {
        if (this.runtimeRuleSet.runtimeRules.isEmpty()) {
            val rbs = this.ruleBuilders.values.flatten()
            val ruleMap = rbs.mapIndexedNotNull { index, rb -> rb.name?.let { Pair(rb.name, index) } }.associate { it }
            val rules = rbs.mapIndexed { index, rb -> rb.buildRule(index, ruleMap) }
            this.runtimeRuleSet.setRules(rules)
        }
        return this.runtimeRuleSet
    }

    private fun _rule(
        name: String,
        option: Int,
        isSkip: Boolean,
        init: RuntimeRuleRhsBuilder.() -> Unit,
        build: (ruleMap: Map<String, Int>, itemsRef: List<RuntimeRuleRef>) -> RuntimeRuleRhs
    ): RuntimeRuleBuilder {
        val rhsB = RuntimeRuleRhsBuilder(this, isSkip)
        rhsB.init()
        val rb = RuntimeRuleBuilder(this, option = option, name, isSkip) { ruleMap ->
            build(ruleMap, rhsB.itemRefs)
        }
        this.ruleBuilders[name].add(rb)
        return rb
    }

    fun literal(name: String?, value: String, isSkip: Boolean = false) {
        val tag = name ?: "'$value'"
        if (this.ruleBuilders.containsKey(tag)) {
            //do nothing
        } else {
            val rb = RuntimeRuleBuilder(this, option = 0, name, isSkip) {
                RuntimeRuleRhsLiteral(value)
            }
            this.ruleBuilders[tag].add(rb)
        }
    }

    fun pattern(name: String?, pattern: String, isSkip: Boolean = false) {
        val tag = name ?: "\"$pattern\""
        if (this.ruleBuilders.containsKey(tag)) {
            //do nothing
        } else {
            val rb = RuntimeRuleBuilder(this, option = 0, name, isSkip) {
                RuntimeRuleRhsPattern(pattern)
            }
            this.ruleBuilders[tag].add(rb)
        }
    }

    fun emptyRule(ruleThatIsEmptyTag: String, isSkip: Boolean = false): String {
        val tag = "§empty." + ruleThatIsEmptyTag
        if (this.ruleBuilders.containsKey(tag)) {
            //do nothing
        } else {
            val rb = RuntimeRuleBuilder(this, option = 0, tag, isSkip) { ruleMap ->
                val rn = ruleMap[ruleThatIsEmptyTag] ?: error("Rule with tag $tag not found")
                RuntimeRuleRhsEmpty(runtimeRuleSet, rn)
            }
            this.ruleBuilders[tag].add(rb)
        }
        return tag
    }

    fun concatenation(name: String, option: Int = 0, isSkip: Boolean = false, init: RuntimeRuleRhsBuilder.() -> Unit) =
        _rule(name, option, isSkip, init) { ruleMap, itemRefs ->
            val items = itemRefs.map { ruleMap[it.tag] ?: error("Rule with tag $name not found") }
            RuntimeRuleRhsConcatenation(runtimeRuleSet, items)
        }

    fun multi(name: String, min: Int, max: Int, itemRef: String, isSkip: Boolean = false) {
        if (min == 0) {
            val erName = emptyRule(name)
            // the empty option is lower priority
            _rule(name, 0, isSkip, {}) { ruleMap, itemRefs ->
                val er = ruleMap[erName] ?: error("Rule with tag $erName not found")
                RuntimeRuleRhsEmpty(runtimeRuleSet, er)
            }
            _rule(name, 1, isSkip, {}) { ruleMap, _ ->
                val item = ruleMap[itemRef] ?: error("Rule with tag $name not found")
                RuntimeRuleRhsListSimple(runtimeRuleSet, min, max, item)
            }
        } else {
            _rule(name, 0, isSkip, {}) { ruleMap, _ ->
                val item = ruleMap[itemRef] ?: error("Rule with tag $name not found")
                RuntimeRuleRhsListSimple(runtimeRuleSet, min, max, item)
            }
        }
    }

    fun sList(name: String, min: Int, max: Int, itemRef: String, sepRef: String, isSkip: Boolean = false) {
        if (min == 0) {
            val erName = emptyRule(name)
            _rule(name, 0, isSkip, {}) { ruleMap, itemRefs ->
                val er = ruleMap[erName] ?: error("Rule with tag $erName not found")
                RuntimeRuleRhsEmpty(runtimeRuleSet, er)
            }
            _rule(name, 1, isSkip, {}) { ruleMap, _ ->
                val item = ruleMap[itemRef] ?: error("Rule with tag $name not found")
                val sep = ruleMap[sepRef] ?: error("Rule with tag $name not found")
                RuntimeRuleRhsListSeparated(runtimeRuleSet, min, max, item, sep)
            }
        } else {
            _rule(name, 0, isSkip, {}) { ruleMap, _ ->
                val item = ruleMap[itemRef] ?: error("Rule with tag $name not found")
                val sep = ruleMap[sepRef] ?: error("Rule with tag $name not found")
                RuntimeRuleRhsListSeparated(runtimeRuleSet, min, max, item, sep)
            }
        }
    }

    fun embedded(name: String, embeddedRuleSet: RuntimeRuleSet, startRule: RuntimeRule, isSkip: Boolean = false) {
        val rb = RuntimeRuleBuilder(this, option = 0, name, isSkip) {
            RuntimeRuleRhsEmbedded(embeddedRuleSet, startRule)
        }
        this.ruleBuilders[name].add(rb)
    }
}

@RuntimeRuleSetDslMarker
internal class RuntimeRuleBuilder(
    val rrsb: RuntimeRuleSetBuilder2,
    val option: Int,
    val name: String?,
    val isSkip: Boolean,
    val rhs: (ruleMap: Map<String, Int>) -> RuntimeRuleRhs
) {
    var rule: RuntimeRule? = null

    fun buildRule(number: Int, ruleMap: Map<String, Int>): RuntimeRule {
        if (null == this.rule) {
            val rhs = rhs.invoke(ruleMap)
            this.rule = RuntimeRule(number, option, name, isSkip, rhs)
        }
        return this.rule!!
    }
}

internal data class RuntimeRuleRef(val tag: String)

@RuntimeRuleSetDslMarker
internal class RuntimeRuleRhsBuilder(
    val rrsb: RuntimeRuleSetBuilder2,
    val isSkip: Boolean = false
) {

    val itemRefs = mutableListOf<RuntimeRuleRef>()

    fun literal(value: String): RuntimeRuleRef {
        val tag = "'$value'"
        this.rrsb.literal(null, value)
        return ref(tag)
    }

    fun pattern(pattern: String): RuntimeRuleRef {
        val tag = "\"$pattern\""
        this.rrsb.pattern(tag, pattern)
        return ref(tag)
    }

    fun ref(name: String): RuntimeRuleRef {
        val ref = RuntimeRuleRef(name)
        itemRefs.add(ref)
        return ref
    }
}
