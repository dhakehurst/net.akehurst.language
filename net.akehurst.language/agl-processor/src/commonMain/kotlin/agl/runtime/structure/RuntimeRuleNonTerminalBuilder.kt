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

import net.akehurst.language.api.parser.ParseException

class RuntimeRuleNonTerminalBuilder(val rrsb: RuntimeRuleSetBuilder, val name: String) {

    private var kind: RuntimeRuleKind = RuntimeRuleKind.NON_TERMINAL
    private var isPattern: Boolean = false
    private var isSkip: Boolean = false
    private var rule: RuntimeRule? = null

    private fun runtimeRuleItemBuilder(kind: RuntimeRuleItemKind, items: Array<out RuntimeRule>): RuntimeRuleItemBuilder {
        if (items.isEmpty()) {
            throw  ParseException("The rule must have some items")
        }
        return RuntimeRuleItemBuilder(this, kind, items)
    }

    internal fun build(): RuntimeRule {
        if (null == this.rule) {
            val rr = RuntimeRule(this.rrsb.rules.size, name, kind, isPattern, isSkip)
            this.rrsb.rules.add(rr)
            this.rule = rr
            return rr
        } else {
            return this.rule ?: throw ParseException("Should not happen")
        }
    }

    fun withRhs(rhs: RuntimeRuleItem) : RuntimeRule {
        val rr = this.build()
        rr.rhsOpt = rhs
        return rr
    }

    fun skip(value: Boolean) : RuntimeRuleNonTerminalBuilder {
        this.isSkip = value
        return this
    }

    fun empty(): RuntimeRule {
        val rr = this.build()
        val e = RuntimeRuleTerminalBuilder(this.rrsb).empty(rr)
        rr.rhsOpt = RuntimeRuleItem(RuntimeRuleItemKind.CONCATENATION, 0, 0, arrayOf(e))
        return rr
    }

    fun choiceEqual(vararg items: RuntimeRule): RuntimeRule {
        return this.runtimeRuleItemBuilder(RuntimeRuleItemKind.CHOICE_EQUAL, items).build()
    }

    fun choicePriority(vararg items: RuntimeRule): RuntimeRule {
        return this.runtimeRuleItemBuilder(RuntimeRuleItemKind.CHOICE_PRIORITY, items).build()
    }

    fun concatenation(vararg items: RuntimeRule): RuntimeRule {
        return this.runtimeRuleItemBuilder(RuntimeRuleItemKind.CONCATENATION, items).build()
    }

    fun unordered(vararg items: RuntimeRule): RuntimeRuleItemBuilder {
        return RuntimeRuleItemBuilder(this, RuntimeRuleItemKind.UNORDERED, items)
    }

    fun multi(min: Int, max: Int, item: RuntimeRule): RuntimeRule {
        val items = if (0==min) {
            val rr = this.build()
            val e = RuntimeRuleTerminalBuilder(this.rrsb).empty(rr)
            arrayOf(item, e)
        } else {
            arrayOf(item)
        }
        return this.runtimeRuleItemBuilder(RuntimeRuleItemKind.MULTI, items).min(min).max(max).build()
    }

    fun separatedList(min: Int, max: Int, separator: RuntimeRule, item: RuntimeRule): RuntimeRule {
        val items = if (0==min) {
            val rr = this.build()
            val e = RuntimeRuleTerminalBuilder(this.rrsb).empty(rr)
            arrayOf(item, separator, e)
        } else {
            arrayOf(item,separator)
        }
        return this.runtimeRuleItemBuilder(RuntimeRuleItemKind.SEPARATED_LIST, items).min(min).max(max).build()
    }

    fun leftAssociativeList(min: Int, max: Int, separator: RuntimeRule, item: RuntimeRule): RuntimeRuleItemBuilder {
        return this.runtimeRuleItemBuilder(RuntimeRuleItemKind.LEFT_ASSOCIATIVE_LIST, arrayOf(item, separator)).min(min).max(max)
    }

    fun rightAssociativeList(min: Int, max: Int, separator: RuntimeRule, item: RuntimeRule): RuntimeRuleItemBuilder {
        return this.runtimeRuleItemBuilder(RuntimeRuleItemKind.RIGHT_ASSOCIATIVE_LIST, arrayOf(item, separator)).min(min).max(max)
    }
}