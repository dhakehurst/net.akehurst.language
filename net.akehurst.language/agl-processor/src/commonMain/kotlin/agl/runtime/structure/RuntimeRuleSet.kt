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
import net.akehurst.language.api.parser.ParseFailedException
import net.akehurst.language.api.parser.ParserConstructionFailedException
import net.akehurst.language.agl.runtime.graph.GrowingNode
import net.akehurst.language.agl.runtime.graph.PreviousInfo
import net.akehurst.language.collections.transitveClosure
import net.akehurst.language.parser.scannerless.InputFromCharSequence

class RuntimeRuleSet(rules: List<RuntimeRule>) {

    enum class ParseAction { HEIGHT, GRAFT, WIDTH }

    companion object {
        val GOAL_RULE_NUMBER = -1;
        val EOT_RULE_NUMBER = -2;
        val END_OF_TEXT = RuntimeRule(EOT_RULE_NUMBER, InputFromCharSequence.END_OF_TEXT, RuntimeRuleKind.TERMINAL, false, false)

        fun createGoal(userGoalRule: RuntimeRule): RuntimeRule {
            val gr = RuntimeRule(GOAL_RULE_NUMBER, "<GOAL>", RuntimeRuleKind.GOAL, false, false)
            gr.rhsOpt = RuntimeRuleItem(RuntimeRuleItemKind.CONCATENATION, -1, 0, arrayOf(userGoalRule, END_OF_TEXT))
            return gr
        }
    }

    private val nonTerminalRuleNumber: MutableMap<String, Int> = mutableMapOf()
    private val terminalRuleNumber: MutableMap<String, Int> = mutableMapOf()

    data class IndexCanGrowIntoAt(
        val childRuleNumber: Int,
        val ancesstorRuleNumber: Int,
        val at: Int
    ) {}

    private val canGrowIntoAt_cache: MutableMap<IndexCanGrowIntoAt, Boolean> = mutableMapOf()

    //TODO: are Arrays faster than Lists?
    val runtimeRules: Array<out RuntimeRule> by lazy {
        rules.sortedBy { it.number }.toTypedArray()
    }

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

    val isSkipTerminal: Array<Boolean> by lazy {
        this.runtimeRules.map {
            this.calcIsSkipTerminal(it)
        }.toTypedArray()
    }

    val terminalRules: Array<RuntimeRule> by lazy {
        this.runtimeRules.mapNotNull {
            if (it.isTerminal)
                it
            else
                null
        }.toTypedArray()
    }

    val firstTerminals: Array<Set<RuntimeRule>> by lazy {
        this.runtimeRules.map { this.calcFirstTerminals(it) }
            .toTypedArray()
    }

    val firstSkipRuleTerminalPositions: Set<RulePosition> by lazy {
        this.calcFirstTerminalSkipRulePositions()
    }

    val expectedSkipItemRulePositionsTransitive: Set<RulePosition> by lazy {
        calcExpectedSkipItemRulePositionTransitive()
    }

    val expectedTerminalRulePositions = lazyMap<RulePosition, Array<RulePosition>> {
        calcExpectedTerminalRulePositions(it).toTypedArray()
    }

    val expectedItemRulePositions = lazyMap<RulePosition, Set<RulePosition>> {
        calcExpectedItemRulePositions(it)
    }

    val expectedItemRulePositionsTransitive = lazyMap<RulePosition, Set<RulePosition>> {
        calcExpectedItemRulePositionTransitive(it)
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
        for (rr in rules) {
//            if (null == rrule) {
//                throw ParserConstructionFailedException("RuntimeRuleSet must not contain a null rule!")
//            }
            if (rr.isNonTerminal) {
                this.nonTerminalRuleNumber[rr.name] = rr.number
            } else {
                this.terminalRuleNumber[rr.name] = rr.number
            }
        }
    }

    fun buildCaches() {
        this.allSkipRules.size
        this.allSkipTerminals.size
        this.isSkipTerminal.size
        this.terminalRules.size
        this.firstTerminals.size
        this.firstSkipRuleTerminalPositions.size
        this.firstSuperNonTerminal.size
        this.subNonTerminals.size
        this.subTerminals.size
    }

    fun getActions(gn: GrowingNode, previous: Set<PreviousInfo>): List<ParseAction> {
        val result = mutableListOf<ParseAction>()
/*
        if (gn.hasCompleteChildren && !previous.isEmpty()) {
            result.add(ParseAction.HEIGHT)
        }

        var b = false
        val x = gn.isEmptyMatch && (info.node.runtimeRule.rhs.kind == RuntimeRuleItemKind.MULTI || info.node.runtimeRule.rhs.kind == RuntimeRuleItemKind.SEPARATED_LIST) && info.node.numNonSkipChildren != 0
        b = b or (info.node.expectsItemAt(gn.runtimeRule, info.atPosition) && !x)
        if (gn.hasCompleteChildren) {
            result.add(ParseAction.GRAFT)
        }

        if (gn.canGrowWidth) {
            result.add(ParseAction.WIDTH)
        }
*/
        return result;
    }

    fun findRuntimeRule(ruleName: String): RuntimeRule {
        val number = this.nonTerminalRuleNumber[ruleName]
            ?: throw ParseException("NonTerminal RuntimeRule '${ruleName}' not found")
        return this.runtimeRules[number]
    }

    fun findTerminalRule(pattern: String): RuntimeRule {
        val number = this.terminalRuleNumber[pattern]
            ?: throw ParseException("Terminal RuntimeRule ${pattern} not found")
        return this.runtimeRules[number]
    }

    /**
     * itemRule is the rule we use to increment rp
     */
    fun nextRulePosition(rp: RulePosition, itemRule: RuntimeRule): Array<RulePosition> {
        return if(RulePosition.END_OF_RULE == rp.position) {
            arrayOf()
        } else {
            when (rp.runtimeRule.rhs.kind) {
                RuntimeRuleItemKind.EMPTY -> throw ParseException("This should never happen!")
                RuntimeRuleItemKind.CHOICE_EQUAL -> arrayOf(RulePosition(rp.runtimeRule, RulePosition.END_OF_RULE, setOf()))
                RuntimeRuleItemKind.CHOICE_PRIORITY -> arrayOf(RulePosition(rp.runtimeRule, RulePosition.END_OF_RULE, setOf()))
                RuntimeRuleItemKind.CONCATENATION -> {
                    val np = rp.position + 1
                    if (np < rp.runtimeRule.rhs.items.size) {
                        arrayOf(RulePosition(rp.runtimeRule, np, setOf()))
                    } else {
                        //need to compute the set of rps that come after the end of this rule
                        //    go back to goal
                        arrayOf(RulePosition(rp.runtimeRule, RulePosition.END_OF_RULE, setOf()))
                        //if (prevNode.previous.isEmpty()) {
                        //    arrayOf(RulePosition(rp.runtimeRule, RulePosition.END_OF_RULE, setOf()))
                        //} else {
                        //    nextRulePosition(prevNode.rulePosition, rp.runtimeRule) //TODO: a hack!
                        //}
                    }
                }
                RuntimeRuleItemKind.MULTI -> when {
                    rp.runtimeRule.rhs.multiMin == 0 && itemRule == rp.runtimeRule.rhs.MULTI__emptyRule -> arrayOf(
                        RulePosition(rp.runtimeRule, RulePosition.END_OF_RULE, setOf())
                    )
                    itemRule == rp.runtimeRule.rhs.MULTI__repeatedItem -> arrayOf(
                        RulePosition(rp.runtimeRule, RuntimeRuleItem.MULTI__ITEM, setOf()),
                        RulePosition(rp.runtimeRule, RulePosition.END_OF_RULE, setOf())
                    )
                    else -> arrayOf() //throw ParseException("This should never happen!")
                }
                RuntimeRuleItemKind.SEPARATED_LIST -> when {
                    rp.runtimeRule.rhs.multiMin == 0 && itemRule == rp.runtimeRule.rhs.SLIST__emptyRule -> arrayOf(
                        RulePosition(rp.runtimeRule, RulePosition.END_OF_RULE, setOf())
                    )
                    itemRule == rp.runtimeRule.rhs.SLIST__repeatedItem -> arrayOf(
                        RulePosition(rp.runtimeRule, RuntimeRuleItem.SLIST__SEPARATOR, setOf())
                    )
                    itemRule == rp.runtimeRule.rhs.SLIST__separator -> arrayOf(
                        RulePosition(rp.runtimeRule, RuntimeRuleItem.SLIST__ITEM, setOf()),
                        RulePosition(rp.runtimeRule, RulePosition.END_OF_RULE, setOf())
                    )
                    else -> throw ParseException("This should never happen!")
                }
                RuntimeRuleItemKind.LEFT_ASSOCIATIVE_LIST -> throw ParseException("Not yet supported")
                RuntimeRuleItemKind.RIGHT_ASSOCIATIVE_LIST -> throw ParseException("Not yet supported")
                RuntimeRuleItemKind.UNORDERED -> throw ParseException("Not yet supported")
            }
        }
    }

    fun findNextExpectedItems(runtimeRule: RuntimeRule, nextItemIndex: Int): Set<RuntimeRule> {
        return runtimeRule.findNextExpectedItems(nextItemIndex)
    }

    fun findNextExpectedTerminals(runtimeRule: RuntimeRule, nextItemIndex: Int): Set<RuntimeRule> {
        val nextItems = this.findNextExpectedItems(runtimeRule, nextItemIndex)
        val result = mutableSetOf<RuntimeRule>()
        nextItems.forEach {
            result += this.firstTerminals[it.number]
            //result += this.calcFirstTerminals(it)
        }
        return result
    }

    private fun calcLookahead(rp: RulePosition): Set<RuntimeRule> {
        return setOf()
    }

    private fun calcExpectedItemRulePositions(rp: RulePosition): Set<RulePosition> {
        return rp.runtimeRule.calcExpectedRulePositions(rp.position)
    }

    private fun calcExpectedItemRulePositionTransitive(rp: RulePosition): Set<RulePosition> {
        var s = rp.runtimeRule.calcExpectedRulePositions(rp.position)

        return s.transitveClosure { rp ->
            if (RulePosition.END_OF_RULE == rp.position) {
                setOf<RulePosition>()
            } else {
                if (rp.runtimeRule.isTerminal) {
                    setOf<RulePosition>()
                } else {
                    rp.runtimeRule.itemsAt[rp.position].flatMap {
                        if (it.isTerminal) {
                            setOf(rp)
                        } else {
                            it.calcExpectedRulePositions(0)
                        }
                    }
                }
            }.toSet()
        }
    }

    private fun calcExpectedTerminalRulePositions(rp: RulePosition): Set<RulePosition> {
        val nextItems = this.calcExpectedItemRulePositionTransitive(rp)
        return nextItems.filter {
            if (it.runtimeRule.isTerminal) { //should never happen!
                false
            } else {
                if (RulePosition.END_OF_RULE == it.position) {
                    false
                } else {
                    it.runtimeRule.itemsAt[it.position].any { it.isTerminal }
                }
            }
        }.toSet() //TODO: cache ?
    }

    private fun calcExpectedSkipItemRulePositionTransitive() : Set<RulePosition> {
        val skipRuleStarts = allSkipRules.map {
            val x = firstTerminals[it.number]
            RulePosition(it, 0, setOf())
        }
        return skipRuleStarts.flatMap {
            this.calcExpectedItemRulePositionTransitive(it)
        }.toSet()
    }

    private fun calcFirstTerminalSkipRulePositions(): Set<RulePosition> {
        val skipRPs = calcExpectedSkipItemRulePositionTransitive()
        return skipRPs.filter {
            it.runtimeRule.itemsAt[it.position].any { it.isTerminal }
        }.toSet() //TODO: cache ?
    }

    private fun calcFirstSubRules(runtimeRule: RuntimeRule): Set<RuntimeRule> {
        return runtimeRule.findSubRulesAt(0)
    }

    private fun calcFirstTerminals(runtimeRule: RuntimeRule): Set<RuntimeRule> {
        var rr = runtimeRule.findTerminalAt(0)
        for (r in this.calcFirstSubRules(runtimeRule)) {
            rr += r.findTerminalAt(0)
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

    private fun calcIsSkipTerminal(rr: RuntimeRule): Boolean {
        val b = this.allSkipTerminals.contains(rr)
        return b
    }

    private fun calcFirstSuperNonTerminal(runtimeRule: RuntimeRule): List<RuntimeRule> {
        return this.runtimeRules.filter {
            it.isNonTerminal && it.couldHaveChild(runtimeRule, 0)
        } + if (runtimeRule.isEmptyRule) listOf(runtimeRule.ruleThatIsEmpty) else emptyList()
    }

    /**
     * return the set of SuperRules for which
     * childRule can grow (at some point)
     * into ancesstorRule
     * at position ancesstorItemIndex
     */
    fun calcGrowsInto(childRule: RuntimeRule, ancesstorRule: RuntimeRule, ancesstorItemIndex: Int): List<RuntimeRule> {
        return this.firstSuperNonTerminal[childRule.number].filter {
            this.calcCanGrowInto(it, ancesstorRule, ancesstorItemIndex)
        }
    }

    //TODO: should be private and only a cache is public
    fun calcCanGrowInto(childRule: RuntimeRule, ancesstorRule: RuntimeRule, ancesstorItemIndex: Int): Boolean {
        return if (-1 == ancesstorItemIndex) {
            false
        } else {
            //return canGrowIntoAt_cache[childRule.number][ancesstorRule.number][ancesstorItemIndex];
            val index = IndexCanGrowIntoAt(childRule.number, ancesstorRule.number, ancesstorItemIndex)
            var result = canGrowIntoAt_cache[index]
            if (null == result) {
                val nextExpectedForStacked = this.findNextExpectedItems(ancesstorRule, ancesstorItemIndex)
                if (nextExpectedForStacked.contains(childRule)) {
                    result = true
                } else {
                    result = false
                    for (rr in nextExpectedForStacked) {
                        if (rr.isNonTerminal) {
                            // todo..can we reduce the possibles!
                            val possibles = this.calcFirstSubRules(rr)
                            if (possibles.contains(childRule)) {
                                result = true
                                break
                            }
                        } else {
                            val possibles = this.firstTerminals[rr.number]
                            if (possibles.contains(childRule)) {
                                result = true
                                break
                            }
                        }
                    }
                }
                canGrowIntoAt_cache[index] = result ?: throw ParseException("Should never happen")
            }
            return result
        }
    }

    override fun toString(): String {
        val rulesStr = this.runtimeRules.map {
            "  " + it.toString()
        }.joinToString("\n")
        return """
RuntimeRuleSet {
${rulesStr}
}
        """.trimIndent()
    }
}