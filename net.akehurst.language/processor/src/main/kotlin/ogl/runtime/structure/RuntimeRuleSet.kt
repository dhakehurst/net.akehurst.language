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

package net.akehurst.language.ogl.runtime.structure

import net.akehurst.language.api.parser.ParseException
import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.api.parser.ParserConstructionFailedException

class RuntimeRuleSet(rules: List<RuntimeRule>) {

    //TODO: are Arrays faster than Lists?
    private val runtimeRules: Array<out RuntimeRule> by lazy {
        val result = ArrayList<RuntimeRule>(rules.size)
        rules.forEach { result[it.number] = it }
        result.toTypedArray()
    }
    private val nonTerminalRuleNumber: MutableMap<String, Int> = mutableMapOf()
    private val terminalRuleNumber: MutableMap<String, Int> = mutableMapOf()

    val allSkipRules: Array<RuntimeRule> by lazy {
        this.runtimeRules.filter { it.isSkip }.toTypedArray()
    }

    val firstTerminals: Array<Set<RuntimeRule>> by lazy {
        val result = ArrayList<Set<RuntimeRule>>(this.runtimeRules.size)
        this.runtimeRules.forEach { result[it.number] = this.findFirstTerminals(it) }
        result.toTypedArray()
    }

    val firstSkipRuleTerminals: Array<Set<RuntimeRule>> by lazy {
        val result = ArrayList<Set<RuntimeRule>>(this.runtimeRules.size)
        // rules that are not skip rules will not have a value set
        this.runtimeRules.forEach { if(it.isSkip) result[it.number] = this.findFirstTerminals(it) }
        result.toTypedArray()
    }

    init {
        for (rrule in rules) {
//            if (null == rrule) {
//                throw ParserConstructionFailedException("RuntimeRuleSet must not contain a null rule!")
//            }
            if (RuntimeRuleKind.NON_TERMINAL == rrule.kind) {
                //                  this.nodeTypes.set(i, rrule.name)
                this.nonTerminalRuleNumber[rrule.name] = rrule.number
            } else {
                terminalRuleNumber[rrule.name] = rrule.number
            }
        }
    }

    fun findRuntimeRule(ruleName: String): RuntimeRule {
        val number = this.nonTerminalRuleNumber[ruleName]
                ?: throw ParseException("NonTerminal RuntimeRule ${ruleName} not found")
        return this.runtimeRules[number] ?: throw ParseException("NonTerminal RuntimeRule ${ruleName} not found")
    }

    fun findTerminalRule(pattern: String): RuntimeRule {
        val number = this.terminalRuleNumber[pattern]
                ?: throw ParseException("Terminal RuntimeRule ${pattern} not found")
        return this.runtimeRules[number] ?: throw ParseException("Terminal RuntimeRule ${pattern} not found")
    }

    fun findNextExpectedItems(runtimeRule: RuntimeRule, nextItemIndex: Int, numNonSkipChildren: Int): Set<RuntimeRule> {
        return runtimeRule.findNextExpectedItems(nextItemIndex, numNonSkipChildren, this)
    }

    fun findNextExpectedTerminals(runtimeRule: RuntimeRule, nextItemIndex: Int, numNonSkipChildren: Int): Set<RuntimeRule> {
        val nextItems = this.findNextExpectedItems(runtimeRule, nextItemIndex, numNonSkipChildren)
        val result = mutableSetOf<RuntimeRule>()
        nextItems.forEach {
            result += this.findFirstTerminals(it)
        }
        return result
    }

    fun findFirstSubRules(runtimeRule: RuntimeRule): Set<RuntimeRule> {
        return runtimeRule.findSubRulesAt(0)
    }

    fun findFirstTerminals(runtimeRule: RuntimeRule): Set<RuntimeRule> {
         var rr = runtimeRule.findTerminalAt(0, this)
        for (r in this.findFirstSubRules(runtimeRule)) {
            rr += r.findTerminalAt(0, this)
        }
        return rr
    }
}