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
import net.akehurst.language.api.grammar.Rule
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

    val firstTerminals2 = lazyMap<RulePosition, Set<RuntimeRule>> {
        val trps = expectedTerminalRulePositions[it] ?: arrayOf()
        trps.flatMap { it.items }.toSet()
    }

    private val growsInto: LazyArray<Set<RulePosition>> = lazyArray(runtimeRules.size) {
        calcGrowsInto(it)
    }

    private val lookahead = lazyMap<RuntimeRule,Map<RulePosition, Set<RuntimeRule>>> { goalRule ->
        lazyMap<RulePosition, Set<RuntimeRule>> { rp ->
            calcLookahead(goalRule, rp)
        }
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

        this.runtimeRules.forEach { rule ->
            this.growsInto[rule.number]
            rule.rulePositions.forEach { rp ->
                if (rp.isAtEnd ) {
                    //no need to do for end-of-rule (at present)
                } else {
                    this.lookahead[rule]?.get(rp)
                    this.firstTerminals2[rp]
                }
            }
        }
    }

    /**
     * return the set of RulePositions that rule could grow into
     * the goal rule must be passed, as it is parse specific
     */
    fun growsInto(rule: RuntimeRule, goalRule: RuntimeRule): Set<RulePosition> {
        val result = when {
            (rule.isGoal) -> emptySet<RulePosition>()
            else -> growsInto[rule.number].toSet()
        } + if (goalRule.couldHaveChild(rule, 0)) {
            setOf<RulePosition>(RulePosition(goalRule, 0, 0))
        } else {
            emptySet()
        }
        return result
    }

    /**
     * return the set of terminal RuntimeRules that the given RulePosition could grow into
     * the goal rule must be passed, as it is parse specific
     */
    fun lookahead(rp: RulePosition, goalRule: RuntimeRule): Set<RuntimeRule> {
        return this.lookahead[goalRule]?.get(rp) ?: emptySet()
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
    fun nextRulePosition(rp: RulePosition, itemRule: RuntimeRule): Set<RulePosition> { //TODO: cache this
        return if (RulePosition.END_OF_RULE == rp.position) {
            emptySet() //TODO: use goal rule to find next position? maybe
        } else {
            when (rp.runtimeRule.rhs.kind) {
                RuntimeRuleItemKind.EMPTY -> throw ParseException("This should never happen!")
                RuntimeRuleItemKind.CHOICE_EQUAL -> when {
                    itemRule == rp.runtimeRule.rhs.items[rp.choice] -> setOf(RulePosition(rp.runtimeRule, rp.choice, RulePosition.END_OF_RULE))
                    else -> emptySet() //throw ParseException("This should never happen!")
                }
                RuntimeRuleItemKind.CHOICE_PRIORITY ->when {
                    itemRule == rp.runtimeRule.rhs.items[rp.choice] -> setOf(RulePosition(rp.runtimeRule, rp.choice, RulePosition.END_OF_RULE))
                    else -> emptySet() //throw ParseException("This should never happen!")
                }
                RuntimeRuleItemKind.CONCATENATION -> {
                    val np = rp.position + 1
                    if (np < rp.runtimeRule.rhs.items.size) {
                        setOf(RulePosition(rp.runtimeRule, 0, np))
                    } else {
                        setOf(RulePosition(rp.runtimeRule, 0, RulePosition.END_OF_RULE))
                    }
                }
                RuntimeRuleItemKind.MULTI -> when(rp.choice) {
                    RuntimeRuleItem.MULTI__EMPTY_RULE -> when {
                        0==rp.position && rp.runtimeRule.rhs.multiMin == 0 && itemRule == rp.runtimeRule.rhs.MULTI__emptyRule -> setOf(
                            RulePosition(rp.runtimeRule, RuntimeRuleItem.MULTI__EMPTY_RULE, RulePosition.END_OF_RULE)
                        )
                        else -> emptySet() //throw ParseException("This should never happen!")
                    }
                    RuntimeRuleItem.MULTI__ITEM -> when {
                        itemRule == rp.runtimeRule.rhs.MULTI__repeatedItem -> setOf(
                            RulePosition(rp.runtimeRule, RuntimeRuleItem.MULTI__ITEM, 1),
                            RulePosition(rp.runtimeRule, RuntimeRuleItem.MULTI__ITEM, RulePosition.END_OF_RULE)
                        )
                        else -> emptySet() //throw ParseException("This should never happen!")
                    }
                    else -> throw ParseException("This should never happen!")
                }
                RuntimeRuleItemKind.SEPARATED_LIST -> when(rp.choice) {
                    RuntimeRuleItem.SLIST__EMPTY_RULE -> when {
                        0==rp.position && rp.runtimeRule.rhs.multiMin == 0 && itemRule == rp.runtimeRule.rhs.SLIST__emptyRule -> setOf(
                            RulePosition(rp.runtimeRule, rp.choice, RulePosition.END_OF_RULE)
                        )
                        else -> emptySet() //throw ParseException("This should never happen!")
                    }
                    RuntimeRuleItem.SLIST__ITEM -> when {
                        0==rp.position && (rp.runtimeRule.rhs.multiMax ==1 ) && itemRule == rp.runtimeRule.rhs.SLIST__repeatedItem -> setOf(
                            RulePosition(rp.runtimeRule, RuntimeRuleItem.SLIST__ITEM, RulePosition.END_OF_RULE)
                        )
                        0==rp.position && (rp.runtimeRule.rhs.multiMax > 1 || -1==rp.runtimeRule.rhs.multiMax ) && itemRule == rp.runtimeRule.rhs.SLIST__repeatedItem -> setOf(
                            RulePosition(rp.runtimeRule, RuntimeRuleItem.SLIST__SEPARATOR,1),
                            RulePosition(rp.runtimeRule, RuntimeRuleItem.SLIST__ITEM, RulePosition.END_OF_RULE)
                        )
                        2==rp.position && (rp.runtimeRule.rhs.multiMax > 1 || -1==rp.runtimeRule.rhs.multiMax ) && itemRule == rp.runtimeRule.rhs.SLIST__repeatedItem -> setOf(
                            RulePosition(rp.runtimeRule, RuntimeRuleItem.SLIST__SEPARATOR,1),
                            RulePosition(rp.runtimeRule, RuntimeRuleItem.SLIST__ITEM, RulePosition.END_OF_RULE)
                        )
                        else -> emptySet() //throw ParseException("This should never happen!")
                    }
                    RuntimeRuleItem.SLIST__SEPARATOR -> when {
                        1==rp.position && (rp.runtimeRule.rhs.multiMax > 1 || -1==rp.runtimeRule.rhs.multiMax ) && itemRule == rp.runtimeRule.rhs.SLIST__separator -> setOf(
                            RulePosition(rp.runtimeRule, RuntimeRuleItem.SLIST__ITEM,2)
                        )
                        else -> emptySet() //throw ParseException("This should never happen!")
                    }
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

    private fun calcLookahead(goalRule: RuntimeRule, rp: RulePosition): Set<RuntimeRule> {
        val result = mutableSetOf<RuntimeRule>()
        val items = rp.items
        for (item in items) {
//            if (item.isTerminal) {
                val nextRps = nextRulePosition(rp, item)
                for (nextRp in nextRps) {
                    if (nextRp.isAtEnd) {
                        val growsInto = growsInto(nextRp.runtimeRule, goalRule)
                        val gin = growsInto.flatMap { it.items.flatMap { it2 -> nextRulePosition(it, it2).toSet() }.toSet() }.toSet()
                        val gi = gin.transitveClosure {
                            when {
                                (it.runtimeRule.isGoal) -> emptySet<RulePosition>()
                                it.isAtEnd -> {
                                    val x = growsInto(it.runtimeRule, goalRule)
                                    val x1 = x.flatMap { it.items.flatMap { it2 -> nextRulePosition(it, it2).toSet() }.toSet() }
                                    x1.toSet()
                                }
                                else -> setOf(it)
                            }
                        }
                        val terms = gi.flatMap {
                            when {
                                (it.isAtEnd) -> setOf(RuntimeRuleSet.END_OF_TEXT)
                                else -> firstTerminals2[it] ?: emptySet()
                            }

                        }.toSet()
                        result.addAll(terms)

                    } else {
                        val lhItems = nextRp.items
                        for (lhItem in lhItems) {
                            val x = firstTerminals[lhItem.number]
                            result.addAll(x)
                        }
                    }
                }
//            } else {
//                item.calcExpectedRulePositions(0).forEach {
//                    val s = calcLookaheadNT(it)
//                    result.addAll(s)
//                }
//            }
        }
        return result
    }
/*
    private fun calcLookaheadNT(rp: RulePosition): Set<RuntimeRule> {
        val lhrps = setOf(rp).transitveClosure {
            it.items.flatMap { item->
                if (item.isTerminal) {
                    setOf(rp)
                } else {
                    item.calcExpectedRulePositions( 0 )
                }
            }.toSet()
        }
        return lhrps.map { it.runtimeRule }.toSet()
    }
*/

    private fun calcExpectedItemRulePositions(rp: RulePosition): Set<RulePosition> {
        return rp.runtimeRule.calcExpectedRulePositions(rp.position)
    }

    private fun calcExpectedItemRulePositionTransitive(rp: RulePosition): Set<RulePosition> {
        var s = setOf(rp)//rp.runtimeRule.calcExpectedRulePositions(rp.position)

        return s.transitveClosure { rp ->
            if (RulePosition.END_OF_RULE == rp.position) {
                setOf<RulePosition>()
            } else {
                if (rp.runtimeRule.isTerminal) {
                    setOf<RulePosition>()
                } else {
                    rp.runtimeRule.items(rp.choice,rp.position).flatMap {
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
                    it.items.any { it.isTerminal }
                }
            }
        }.toSet() //TODO: cache ?
    }

    private fun calcExpectedSkipItemRulePositionTransitive(): Set<RulePosition> {
        val skipRuleStarts = allSkipRules.map {
            val x = firstTerminals[it.number]
            RulePosition(it, 0, 0)
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

    //Used by lookahead
    private fun calcGrowsInto(ruleNumber: Int): Set<RulePosition> {
        val rule = this.runtimeRules[ruleNumber]
        return this.runtimeRules.filter {
            it.isNonTerminal && it.rhs.items.contains(rule)
        }.flatMap {
            when (it.rhs.kind) {
                RuntimeRuleItemKind.EMPTY -> emptySet<RulePosition>()
                RuntimeRuleItemKind.CONCATENATION -> it.rhs.items.mapIndexedNotNull { index, item ->
                    if (item==rule) {
                        RulePosition(it, 0,index)
                    } else {
                        null
                    }
                }
                RuntimeRuleItemKind.CHOICE_EQUAL -> setOf(RulePosition(it, it.rhs.items.indexOf(rule), 0))
                RuntimeRuleItemKind.CHOICE_PRIORITY -> setOf(RulePosition(it, it.rhs.items.indexOf(rule), 0))
                RuntimeRuleItemKind.UNORDERED -> TODO()
                RuntimeRuleItemKind.MULTI -> setOf(RulePosition(it, RuntimeRuleItem.MULTI__ITEM,0)) //do we also need empty and/or position 1?
                RuntimeRuleItemKind.SEPARATED_LIST -> it.rhs.items.mapIndexedNotNull { index, item ->
                    if (item==rule) {
                        when (index) {
                            RuntimeRuleItem.SLIST__ITEM -> RulePosition(it, index, 0) //TODO: might this also be pos=2?
                            RuntimeRuleItem.SLIST__SEPARATOR -> RulePosition(it, index, 1)
                            RuntimeRuleItem.SLIST__EMPTY_RULE -> RulePosition(it, index, 0)
                            else -> throw ParseException("Should never happen")
                        }
                    } else {
                        null
                    }
                }
                RuntimeRuleItemKind.RIGHT_ASSOCIATIVE_LIST -> TODO()
                RuntimeRuleItemKind.LEFT_ASSOCIATIVE_LIST -> TODO()
            }
        }.toSet()
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
                //TODO: try using RulePositions to do this calculation
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