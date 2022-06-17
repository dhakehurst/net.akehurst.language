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

import net.akehurst.language.api.parser.ParserException

@DslMarker
internal annotation class RuntimeRuleSetDslMarker

internal fun runtimeRuleSet(init: RuntimeRuleSetBuilder2.() -> Unit): RuntimeRuleSet {
    val b = RuntimeRuleSetBuilder2()
    b.init()
    return b.build()
}

@RuntimeRuleSetDslMarker
internal class RuntimeRuleSetBuilder2() {

    var runtimeRuleSet = RuntimeRuleSet()

    val ruleBuilders: MutableList<RuntimeRuleBuilder> = mutableListOf()

    fun findRuleBuilderByTag(tag: String): RuntimeRuleBuilder? {
        return this.ruleBuilders.firstOrNull {
            it.tag == tag
        }
    }

    fun build(): RuntimeRuleSet {
        if (this.runtimeRuleSet.runtimeRules.isEmpty()) {
            //build and validate
            val rules = this.ruleBuilders.mapIndexed { index, rb ->
                rb.buildRule(index)
            }
            val ruleMap = mutableMapOf<String, RuntimeRule>()
            rules.forEach { ruleMap[it.tag] = it }
            val rbs = this.ruleBuilders.toList() //to stop concurent modification
            rbs.forEach { rb ->
                when (rb.kind) {
                    RuntimeRuleKind.GOAL -> {
                        TODO()
                    }
                    RuntimeRuleKind.TERMINAL -> {
                        val rule = rb.rule!!
                        if (null != rule.rhsOpt) {
                            throw ParserException("Invalid Rule ${rule.tag}")
                        }
                    }
                    RuntimeRuleKind.NON_TERMINAL -> {
                        val rule = rb.buildRhs(ruleMap)
                        if (null == rule.rhsOpt) {
                            throw ParserException("Invalid Rule ${rule.tag}")
                        }
                    }
                    RuntimeRuleKind.EMBEDDED -> {
                        val rule = rb.rule!!
                        if (null == rule.embeddedRuntimeRuleSet) {
                            throw ParserException("Invalid Rule ${rule.tag}")
                        }
                        if (null == rule.embeddedStartRule) {
                            throw ParserException("Invalid Rule ${rule.tag}")
                        }
                    }
                }
            }
            this.runtimeRuleSet.setRules(ruleMap.values.toList())
        }
        return this.runtimeRuleSet
    }

    fun skip(tag: String, init: RuntimeRuleItemsBuilder.() -> Unit) {
        val rhsB = RuntimeRuleItemsBuilder(this, RuntimeRuleRhsItemsKind.CONCATENATION, RuntimeRuleChoiceKind.NONE, RuntimeRuleListKind.NONE,-1, 0, isSkip = true)
        rhsB.init()
        val rb = RuntimeRuleBuilder(this, tag, "", RuntimeRuleKind.NON_TERMINAL, false, true, rhsB)
        this.ruleBuilders.add(rb)
    }

    fun empty(ruleThatIsEmpty: RuntimeRule): RuntimeRuleBuilder {
        val name = "§empty." + ruleThatIsEmpty.tag
        val rhsB = RuntimeRuleItemsBuilder(this, RuntimeRuleRhsItemsKind.EMPTY, RuntimeRuleChoiceKind.NONE, RuntimeRuleListKind.NONE, -1, 0, false, false)
        rhsB.ref(ruleThatIsEmpty.tag)
        val rb = RuntimeRuleBuilder(this, name, name, RuntimeRuleKind.TERMINAL, false, false, rhsB)
        this.ruleBuilders.add(rb)
        return rb
    }

    //fun empty(name: String) {
    //    val rhsB = RuntimeRuleItemsBuilder(this, RuntimeRuleRhsItemsKind.EMPTY, RuntimeRuleChoiceKind.NONE, -1, 0, false, true)
    //    val rb = RuntimeRuleBuilder(this, name, name, RuntimeRuleKind.TERMINAL, false, false, rhsB)
    //    this.ruleBuilders.add(rb)
    //}

    fun literal(tag: String, value: String, isSkip: Boolean = false) {
        val existing = this.findRuleBuilderByTag(tag)
        if (null == existing) {
            val rb = RuntimeRuleBuilder(this, tag, value, RuntimeRuleKind.TERMINAL, false, isSkip)
            this.ruleBuilders.add(rb)
        } else {
            //do nothing // throw RuntimeException("Already got a rule with tag = $name")
        }
    }

    fun pattern(tag: String, pattern: String, isSkip: Boolean = false) {
        val existing = this.findRuleBuilderByTag(tag)
        if (null == existing) {
            val rb = RuntimeRuleBuilder(this, tag, pattern, RuntimeRuleKind.TERMINAL, true, isSkip)
            this.ruleBuilders.add(rb)
        } else {
            //do nothing //throw RuntimeException("Already got a rule with tag = $name")
        }
    }

    private fun _rule(
        tag: String,
        kind: RuntimeRuleRhsItemsKind,
        choiceKind: RuntimeRuleChoiceKind,
        listKind: RuntimeRuleListKind,
        min: Int = -1,
        max: Int = 0,
        init: RuntimeRuleItemsBuilder.() -> Unit
    ): RuntimeRuleBuilder {
        val rhsB = RuntimeRuleItemsBuilder(this, kind, choiceKind, listKind,min, max)
        when (listKind) {
            RuntimeRuleListKind.MULTI -> if (min == 0) rhsB.addEmptyRule = true
            RuntimeRuleListKind.SEPARATED_LIST -> if (min == 0) rhsB.addEmptyRule = true
            RuntimeRuleListKind.NONE -> Unit
            else -> TODO()
        }
        rhsB.init()
        val rb = RuntimeRuleBuilder(this, tag, "", RuntimeRuleKind.NON_TERMINAL, false, false, rhsB)
        this.ruleBuilders.add(rb)
        return rb
    }

    fun concatenation(name: String, init: RuntimeRuleItemsBuilder.() -> Unit): RuntimeRuleBuilder =
        _rule(name, RuntimeRuleRhsItemsKind.CONCATENATION, RuntimeRuleChoiceKind.NONE, RuntimeRuleListKind.NONE,-1, 0, init)

    fun choice(name: String, choiceKind: RuntimeRuleChoiceKind, init: RuntimeRuleItemsBuilder.() -> Unit): RuntimeRuleBuilder =
        _rule(name, RuntimeRuleRhsItemsKind.CHOICE, choiceKind, RuntimeRuleListKind.NONE, -1, 0, init)

    fun multi(name: String, min: Int, max: Int, itemRef: String): RuntimeRuleBuilder {
        return _rule(name, RuntimeRuleRhsItemsKind.LIST, RuntimeRuleChoiceKind.NONE, RuntimeRuleListKind.MULTI, min, max) {
            ref(itemRef)
            empty()
        }
    }

    fun sList(name: String, min: Int, max: Int, itemRef: String, sepRef: String): RuntimeRuleBuilder {
        return _rule(name, RuntimeRuleRhsItemsKind.LIST, RuntimeRuleChoiceKind.NONE, RuntimeRuleListKind.SEPARATED_LIST, min, max) {
            ref(itemRef)
            ref(sepRef)
            empty()
        }
    }

    fun embedded(tag: String, embeddedRuleSet: RuntimeRuleSet, startRule: RuntimeRule): RuntimeRuleBuilder {
        val rb = RuntimeRuleBuilder(this, tag, "", RuntimeRuleKind.EMBEDDED, false, false, null, embeddedRuleSet, startRule)
        this.ruleBuilders.add(rb)
        return rb
    }
}

@RuntimeRuleSetDslMarker
internal class RuntimeRuleBuilder(
    val rrsb: RuntimeRuleSetBuilder2,
    val tag: String,
    val value: String,
    val kind: RuntimeRuleKind,
    val isPattern: Boolean,
    val isSkip: Boolean,
    val rhsBuilder: RuntimeRuleItemsBuilder? = null,
    val embeddedRuleSet: RuntimeRuleSet? = null,
    val startRule: RuntimeRule? = null
) {
    var rule: RuntimeRule? = null

    fun buildRule(number: Int): RuntimeRule {
        if (null == this.rule) {
            this.rule = RuntimeRule(this.rrsb.runtimeRuleSet.number, number, tag, value, kind, isPattern, isSkip, embeddedRuleSet, startRule)
        }
        return this.rule!!
    }

    fun buildRhs(ruleMap: MutableMap<String, RuntimeRule>): RuntimeRule {
        val rhs = rhsBuilder!!.build(ruleMap, this.rule!!)
        this.rule!!.rhsOpt = rhs
        return this.rule!!
    }
}

internal data class RuntimeRuleRef(val tag: String)

@RuntimeRuleSetDslMarker
internal class RuntimeRuleItemsBuilder(
    val rrsb: RuntimeRuleSetBuilder2,
    val kind: RuntimeRuleRhsItemsKind,
    val choiceKind: RuntimeRuleChoiceKind,
    val listKind: RuntimeRuleListKind,
    val min: Int,
    val max: Int,
    val isSkip: Boolean = false,
    var addEmptyRule: Boolean = false
) {

    private val items = mutableListOf<RuntimeRuleRef>()

    fun empty() {
        addEmptyRule = true
    }

    fun literal(value: String): RuntimeRuleRef {
        val tag = "'$value'"
        this.rrsb.literal(tag, value)
        return ref(tag)
    }

    fun pattern(pattern: String): RuntimeRuleRef {
        val tag = "\"$pattern\""
        this.rrsb.pattern(tag, pattern)
        return ref(tag)
    }

    fun ref(name: String): RuntimeRuleRef {
        val ref = RuntimeRuleRef(name)
        items.add(ref)
        return ref
    }

    fun build(ruleMap: MutableMap<String, RuntimeRule>, rr: RuntimeRule): RuntimeRuleItem {
        val items2 = if (addEmptyRule) {
            val er = this.rrsb.empty(rr)
            val nextRuleNumber = ruleMap.size
            val r = er.buildRule(nextRuleNumber)
            r.rhsOpt = RuntimeRuleItem(RuntimeRuleRhsItemsKind.EMPTY, RuntimeRuleChoiceKind.NONE, RuntimeRuleListKind.NONE, -1, 0, arrayOf(rr))
            ruleMap[r.tag] = r
            items + RuntimeRuleRef(er.tag)
        } else {
            items
        }
        val rItems = items2.map {
            ruleMap[it.tag]
                ?: error("Rule ${it.tag} not found")
        }
        val rhs = RuntimeRuleItem(this.kind, this.choiceKind, this.listKind, this.min, this.max, rItems.toTypedArray())
        return rhs
    }
}
