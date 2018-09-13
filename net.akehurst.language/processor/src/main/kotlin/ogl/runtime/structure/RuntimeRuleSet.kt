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
    val runtimeRules: Array<out RuntimeRule> by lazy {
        rules.sortedBy { it.number }.toTypedArray()
    }
    private val nonTerminalRuleNumber: MutableMap<String, Int> = mutableMapOf()
    private val terminalRuleNumber: MutableMap<String, Int> = mutableMapOf()

    val allSkipRules: Array<RuntimeRule> by lazy {
        this.runtimeRules.filter { it.isSkip }.toTypedArray()
    }

    val allSkipTerminals: Array<RuntimeRule> by lazy {
        this.allSkipRules.flatMap {
            if (it.isTerminal)
                listOf(it)
            else
                it.rhs.items.filter { it.isTerminal }
        }.toTypedArray()
    }

    val firstTerminals: Array<Set<RuntimeRule>> by lazy {
        this.runtimeRules.map { this.findFirstTerminals(it) }
                .toTypedArray()
    }

    val firstSkipRuleTerminals: Array<Set<RuntimeRule>> by lazy {
        this.runtimeRules.map { if (it.isSkip) this.findFirstTerminals(it) else emptySet() }
                .toTypedArray()
    }

    val subTerminals: Array<Set<RuntimeRule>> by lazy {
        this.runtimeRules.map {
            var rr = it.findAllTerminal()
            for (r in this.subNonTerminals[it.number]) {
                rr += r.findAllTerminal()
            }
            rr += this.allSkipTerminals
            rr
        }.toTypedArray()
    }

    val subNonTerminals: Array<Set<RuntimeRule>> by lazy {
        this.runtimeRules.map { it.findSubRules() }.toTypedArray()
    }

    //val superRules: Array<List<RuntimeRule>> by lazy {
    //    this.runtimeRules.map { this.calcSuperRule(it) }.toTypedArray()
    //}

    val firstSuperNonTerminal by lazy {
        this.runtimeRules.map { this.calcFirstSuperNonTerminal(it) }.toTypedArray()
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
                ?: this.terminalRuleNumber[ruleName]
                ?: throw ParseException("NonTerminal RuntimeRule '${ruleName}' not found")
        return this.runtimeRules[number] ?: throw ParseException("NonTerminal RuntimeRule ${ruleName} not found")
    }

    fun findTerminalRule(pattern: String): RuntimeRule {
        val number = this.terminalRuleNumber[pattern]
                ?: throw ParseException("Terminal RuntimeRule ${pattern} not found")
        return this.runtimeRules[number] ?: throw ParseException("Terminal RuntimeRule ${pattern} not found")
    }

    fun findNextExpectedItems(runtimeRule: RuntimeRule, nextItemIndex: Int, numNonSkipChildren: Int): Set<RuntimeRule> {
        return runtimeRule.findNextExpectedItems(nextItemIndex, numNonSkipChildren)
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

    /*
    private fun calcSuperRule(runtimeRule: RuntimeRule): List<RuntimeRule> {
        val result = this.runtimeRules.filter {
            runtimeRule.isNonTerminal && it.findSubRules().contains(runtimeRule)
                    || runtimeRule.isTerminal && it.findAllTerminal().contains(runtimeRule)
        }
        return result
    }
    */

    fun calcIsSkipTerminal(rr: RuntimeRule): Boolean {
        val b = this.allSkipTerminals.contains(rr)
        return b
    }

    private fun calcFirstSuperNonTerminal(runtimeRule: RuntimeRule): List<RuntimeRule> {
        return this.runtimeRules.filter {
            it.isNonTerminal && it.couldHaveChild(runtimeRule, 0)
        } + if (runtimeRule.isEmptyRule) listOf(runtimeRule.emptyRuleItem) else emptyList()
    }

    /**
     * return the set of SuperRules for which childRule can grow (at some point) into ancesstorRule at position ancesstorItemIndex
     */
    fun calcGrowsInto(childRule: RuntimeRule, ancesstorRule: RuntimeRule, ancesstorItemIndex: Int): List<RuntimeRule> {
        return this.firstSuperNonTerminal[childRule.number].filter {
            this.canGrowInto(it, ancesstorRule, ancesstorItemIndex)
        }
    }

    private fun canGrowInto(childRule: RuntimeRule, ancesstorRule: RuntimeRule, ancesstorItemIndex: Int): Boolean {
        return if (-1 == ancesstorItemIndex) {
            false
        } else {
            val nextExpectedForStacked = this.findNextExpectedItems(ancesstorRule, ancesstorItemIndex, 0)
            if (nextExpectedForStacked.contains(ancesstorRule)) {
                true
            } else {
                for (rr in nextExpectedForStacked) {
                    if (rr.isNonTerminal) {
                        // todo..can we reduce the possibles!
                        val possibles = this.findFirstSubRules(rr)
                        if (possibles.contains(ancesstorRule)) {
                            return true
                        }
                    } else {
                        val possibles = this.firstTerminals[rr.number]
                        if (possibles.contains(ancesstorRule)) {
                            return true
                        }
                    }
                }
                false
            }
        }
    }
}