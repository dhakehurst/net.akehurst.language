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
    /*
        val expectedItemRulePositions = lazyMap<RulePosition, Set<RulePosition>> {
            calcExpectedItemRulePositions(it)
        }

        val expectedItemRulePositionsTransitive = lazyMap<RulePosition, Set<RulePosition>> {
            calcExpectedItemRulePositionTransitive(it)
        }
    */
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

    /** Map of goalRule -> next State number **/
    private val nextState = mutableMapOf<RuntimeRule, Int>()

    private val states = lazyMapNonNull<RuntimeRule, MutableMap<Pair<RulePosition, Set<RuntimeRule>>, RulePositionState>> { userGoalRule ->
        mutableMapOf()
    }
    private val lookahead2 = lazyMap<Pair<RulePosition, Set<RuntimeRule>>, Map<RulePosition, Set<RuntimeRule>>> { startingAt ->
        lazyMap { rp ->
            calcLookahead2(startingAt, rp)
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

    fun fetchOrCreateRulePositionState(userGoalRule: RuntimeRule, rulePosition: RulePosition, heightLookahead: Set<RuntimeRule>, graftLookahead: Set<RuntimeRule>): RulePositionState {
        val currentStates = this.states[userGoalRule]
        val key = Pair(rulePosition, heightLookahead)
        return if (currentStates.containsKey(key)) {
            currentStates[key] ?: throw Exception("Should never happen")
        } else {
            val rps = createRulePositionState(userGoalRule, rulePosition, heightLookahead, graftLookahead)
            currentStates[key] = rps
            rps
        }
    }

    private fun createRulePositionState(goalRule: RuntimeRule, rulePosition: RulePosition, heightLookahead: Set<RuntimeRule>, graftLookahead: Set<RuntimeRule>): RulePositionState {
        val stateNumber: Int = this.nextState[goalRule] ?: 0
        this.nextState[goalRule] = stateNumber + 1
        return RulePositionState(stateNumber, rulePosition, heightLookahead, graftLookahead)
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
                if (rp.isAtEnd) {
                    //no need to do for end-of-rule (at present)
                } else {
                    //TODO: this.lookahead[rp]?.get(rp)
                    this.firstTerminals2[rp]
                }
            }
        }
    }

    /**
     * return the set of RulePositions that rule could grow into
     * the goal rule must be passed, as it is parse specific
     */
    fun growsInto(userGoalRule: RuntimeRule, startingAt: RulePositionState, lookingFor: RulePositionState): Set<RulePositionState> {
        val parents = calcParents(userGoalRule, startingAt, lookingFor)
        return parents.filter { it.second.runtimeRule == lookingFor.runtimeRule }.map { it.first }.toSet()
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
                RuntimeRuleItemKind.CHOICE_PRIORITY -> when {
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
                RuntimeRuleItemKind.MULTI -> when (rp.choice) {
                    RuntimeRuleItem.MULTI__EMPTY_RULE -> when {
                        0 == rp.position && rp.runtimeRule.rhs.multiMin == 0 && itemRule == rp.runtimeRule.rhs.MULTI__emptyRule -> setOf(
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
                RuntimeRuleItemKind.SEPARATED_LIST -> when (rp.choice) {
                    RuntimeRuleItem.SLIST__EMPTY_RULE -> when {
                        0 == rp.position && rp.runtimeRule.rhs.multiMin == 0 && itemRule == rp.runtimeRule.rhs.SLIST__emptyRule -> setOf(
                            RulePosition(rp.runtimeRule, rp.choice, RulePosition.END_OF_RULE)
                        )
                        else -> emptySet() //throw ParseException("This should never happen!")
                    }
                    RuntimeRuleItem.SLIST__ITEM -> when {
                        0 == rp.position && (rp.runtimeRule.rhs.multiMax == 1) && itemRule == rp.runtimeRule.rhs.SLIST__repeatedItem -> setOf(
                            RulePosition(rp.runtimeRule, RuntimeRuleItem.SLIST__ITEM, RulePosition.END_OF_RULE)
                        )
                        0 == rp.position && (rp.runtimeRule.rhs.multiMax > 1 || -1 == rp.runtimeRule.rhs.multiMax) && itemRule == rp.runtimeRule.rhs.SLIST__repeatedItem -> setOf(
                            RulePosition(rp.runtimeRule, RuntimeRuleItem.SLIST__SEPARATOR, 1),
                            RulePosition(rp.runtimeRule, RuntimeRuleItem.SLIST__ITEM, RulePosition.END_OF_RULE)
                        )
                        2 == rp.position && (rp.runtimeRule.rhs.multiMax > 1 || -1 == rp.runtimeRule.rhs.multiMax) && itemRule == rp.runtimeRule.rhs.SLIST__repeatedItem -> setOf(
                            RulePosition(rp.runtimeRule, RuntimeRuleItem.SLIST__SEPARATOR, 1),
                            RulePosition(rp.runtimeRule, RuntimeRuleItem.SLIST__ITEM, RulePosition.END_OF_RULE)
                        )
                        else -> emptySet() //throw ParseException("This should never happen!")
                    }
                    RuntimeRuleItem.SLIST__SEPARATOR -> when {
                        1 == rp.position && (rp.runtimeRule.rhs.multiMax > 1 || -1 == rp.runtimeRule.rhs.multiMax) && itemRule == rp.runtimeRule.rhs.SLIST__separator -> setOf(
                            RulePosition(rp.runtimeRule, RuntimeRuleItem.SLIST__ITEM, 2)
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

    fun startingRulePositionState(userGoalRule: RuntimeRule): RulePositionState {
        val goalRule = RuntimeRuleSet.createGoal(userGoalRule)
        val goalRp = RulePosition(goalRule, 0, 0)
        val goalLookahead = setOf(RuntimeRuleSet.END_OF_TEXT)
        return createRulePositionState(goalRule, goalRp, goalLookahead, goalLookahead)
    }

    fun possibleChildRulePositionStates(goalRule: RuntimeRule, current: RulePositionState, providedLookahead: Set<RuntimeRule>): Set<RulePositionState> {
        return current.items.flatMap { rule: RuntimeRule ->
            val rps: Set<RulePosition> = rule.calcExpectedRulePositions(0)
            rps.flatMap { rp ->
                val items = rp.items
                if (items.isEmpty()) {
                    setOf(fetchOrCreateRulePositionState(goalRule, rp, emptySet(), providedLookahead))
                } else {
                    rp.items.flatMap { r ->
                        nextRulePosition(rp, r)
                            .map {
                                val lh = this.firstTerminals2[it] ?: throw ParseException("should never happen")
                                fetchOrCreateRulePositionState(goalRule, rp, lh, providedLookahead)
                            }
                    }
                }
            }
        }.toSet()
    }

    fun currentPossibleRulePositionStates(goalRule: RuntimeRule, current: RulePositionState, previous: RulePositionState): Set<RulePositionState> {
        return setOf(current).transitveClosure { parentRPS ->
            parentRPS.items.flatMap { rule: RuntimeRule ->
                val rps: Set<RulePosition> = rule.calcExpectedRulePositions(0)
                rps.flatMap { rp ->
                    val items = rp.items
                    if (items.isEmpty()) {
                        setOf(fetchOrCreateRulePositionState(goalRule, rp, previous.heightLookahead, previous.graftLookahead))
                    } else {
                        rp.items.flatMap { r ->
                            nextRulePosition(rp, r)
                                .map {
                                    val lh = this.firstTerminals2[it] ?: throw ParseException("should never happen")
                                    if (lh.isEmpty()) {
                                        fetchOrCreateRulePositionState(goalRule, rp, previous.heightLookahead, previous.graftLookahead)
                                    } else {
                                        fetchOrCreateRulePositionState(goalRule, rp, lh, previous.graftLookahead)
                                    }
                                }
                        }
                    }
                }
            }.toSet()
        }
    }

    fun nextPossibleRulePositionStates(goalRule: RuntimeRule, current: RulePositionState, providedLookahead: Set<RuntimeRule>): Set<RulePositionState> {
/*
        val z = currentPossibleRulePositionStates(goalRule, current, providedLookahead)

        val nexts = z.flatMap { rps ->
            rps.items.flatMap { rr ->
                val nextRPs = nextRulePosition(rps.rulePosition, rr)
                nextRPs.map { nextRP ->
                    fetchOrCreateRulePositionState(goalRule, nextRP, rps.heightLookahead, rps.graftLookahead) //TODO: not sure lh is correct here!
                }
            }
        }.toSet()
*/


        val z = setOf(current)//currentPossibleRulePositionStates(goalRule, current, providedLookahead)

        val nexts = z.flatMap { rps ->
            rps.items.flatMap { rr ->
                val nextRPs = nextRulePosition(rps.rulePosition, rr)
                nextRPs.flatMap { nextRP ->
                    val nextNextItems = nextRP.items
                    val hlh = rps.heightLookahead //if (nextRP.isAtStart) rps.heightLookahead else rps.graftLookahead
                    if (nextNextItems.isEmpty()) {
                        setOf(fetchOrCreateRulePositionState(goalRule, nextRP, hlh, rps.graftLookahead)) //TODO: not sure lh is correct here!
                    } else {
                        val nextNext: Set<RulePosition> = nextNextItems.flatMap { nextItem -> nextRulePosition(nextRP, nextItem) }.toSet()
                        nextNext.map { nn ->
                            val lh: Set<RuntimeRule> = this.firstTerminals2[nn]
                                ?: throw ParseException("should never happen")
                            if (lh.isEmpty()) {
                                fetchOrCreateRulePositionState(goalRule, nextRP, hlh, rps.graftLookahead)
                            } else {
                                fetchOrCreateRulePositionState(goalRule, nextRP, hlh, lh) //TODO: not sure lh is correct here!
                            }
                        }.toSet()
                    }
                }
            }
        }.toSet()

        return nexts
    }

    private fun calcLookahead(parentRP: RulePosition, childRP: RulePosition, nextItem: RuntimeRule, ifEmpty: Set<RuntimeRule>): Set<RuntimeRule> {
        val nextRPs = nextRulePosition(childRP, nextItem)
        return nextRPs.flatMap { nextRP ->
            if (nextRP.isAtEnd) {
                val nextInParentRPs = nextRulePosition(parentRP, childRP.runtimeRule)
                nextInParentRPs.flatMap { npRP ->
                    if (npRP.isAtEnd) {
                        ifEmpty
                    } else {
                        val lh: Set<RuntimeRule> = this.firstTerminals2[npRP] ?: throw ParseException("should never happen")
                        return if (lh.isEmpty()) {
                            ifEmpty
                        } else {
                            lh
                        }
                    }
                }
                ifEmpty
            } else {
                val lh: Set<RuntimeRule> = this.firstTerminals2[nextRP] ?: throw ParseException("should never happen")
                return if (lh.isEmpty()) {
                    ifEmpty
                } else {
                    lh
                }
            }
        }.toSet()
    }

    /**
     * return Pair(parent,child)
     */
    private fun calcParents(userGoalRule: RuntimeRule, startingAt: RulePositionState, current: RulePositionState): Set<Pair<RulePositionState, RulePositionState>> {

        val firstParentRP = startingAt.rulePosition
        val rps: Set<RulePosition> = firstParentRP.items.flatMap {
            it.calcExpectedRulePositions(0)
        }.toSet()
        val rpsStates: Set<RulePositionState> = rps.flatMap { childRP ->
            val childAtEndRP = RulePosition(childRP.runtimeRule, childRP.choice, RulePosition.END_OF_RULE)
            if (childAtEndRP.runtimeRule.isTerminal) {
                setOf(fetchOrCreateRulePositionState(userGoalRule, childAtEndRP, startingAt.heightLookahead, startingAt.graftLookahead))
            } else {
                childRP.items.map { rr ->
                    val lh = calcLookahead(firstParentRP, childRP, rr, startingAt.heightLookahead)
                    fetchOrCreateRulePositionState(userGoalRule, childAtEndRP, lh, startingAt.graftLookahead)
                }
            }
        }.toSet()
        val start = rpsStates.map { Pair(startingAt, it) }.toSet()
        val parents = start.transitveClosure { parentChild ->
            val oldChildRP = parentChild.second.rulePosition
            val oldChildRPS = parentChild.second
            val newParentRP = RulePosition(oldChildRP.runtimeRule, oldChildRP.choice, 0) // could be a problem with multi/slist!
            val newChildRPs = newParentRP.items.flatMap {
                it.calcExpectedRulePositions(0)
            } //filter out if not at start!
            val newChildStates: Set<RulePositionState> = newChildRPs.flatMap { childRP ->
                val childAtEndRP = RulePosition(childRP.runtimeRule, childRP.choice, RulePosition.END_OF_RULE)
                if (childAtEndRP.runtimeRule.isTerminal) {
                    setOf(fetchOrCreateRulePositionState(userGoalRule, childAtEndRP, oldChildRPS.heightLookahead, startingAt.graftLookahead))
                } else {
                    childRP.items.map { rr ->
                        val lh = calcLookahead(newParentRP, childRP, rr, oldChildRPS.heightLookahead)
                        fetchOrCreateRulePositionState(userGoalRule, childAtEndRP, lh, startingAt.graftLookahead)
                    }
                }
            }.toSet()
            val newParentRPS = fetchOrCreateRulePositionState(userGoalRule, newParentRP, oldChildRPS.heightLookahead, oldChildRPS.graftLookahead)
            newChildStates.map { newChild -> Pair(newParentRPS, newChild) }.toSet()
        }
        return parents
    }

    private fun calcLookahead1(startingAt: Pair<RulePosition, Set<RuntimeRule>>, lookingFor: RulePosition): Set<RuntimeRule> {
        //startingFrom the tgtRPs, find the rp calculating lh as we go
        val start = setOf(startingAt)
        val closure = setOf(start).transitveClosure { seq ->
            //in kotlin setOf and toSet give a LinkedSet which is ordered
            val last = seq.last()
            if (last.first == lookingFor) {
                setOf(seq)
            } else {
                val x = last.first.items.flatMap { rule ->
                    rule.rulePositions
                }
                val y = x.flatMap { rp: RulePosition ->
                    rp.items.flatMap { r -> nextRulePosition(rp, r).map { Pair(rp, it) } }
                }
                val z = y.map { p: Pair<RulePosition, RulePosition> ->
                    val rplh = this.firstTerminals2[p.second] ?: throw ParseException("should never happen")
                    if (rplh.isEmpty()) {
                        seq + Pair(p.first, last.second)
                    } else {
                        seq + Pair(p.first, rplh)
                    }
                }.toSet()
                z
            }
        }

        val lhs = closure.filter { it.last().first == lookingFor }
        val lh: Set<RuntimeRule> = when (lhs.size) {
            0 -> emptySet()
            1 -> lhs.first().last().second
            else -> lhs.flatMap { it.last().second }.toSet()
        }
        return lh
    }

    private fun calcLookahead2(startingAt: Pair<RulePosition, Set<RuntimeRule>>, lookingFor: RulePosition): Set<RuntimeRule> {
        //startingFrom the tgtRPs, find the rp calculating lh as we go
        val closure = setOf(startingAt).transitveClosure {
            it.first.items.flatMap { it2 ->
                //nextRulePosition(it.first, it2).toSet()
                it2.rulePositions
            }.map { rp ->
                val x = rp.items.flatMap { rr -> rr.rulePositions.flatMap { rp2 -> nextRulePosition(rp2, rr) } }
                val y: Set<RuntimeRule> = x.flatMap { firstTerminals2[it] ?: emptySet() }.toSet()
                val rplh = y//this.firstTerminals2[rp] ?: throw ParseException("should never happen")
                if (rplh.isEmpty()) {
                    Pair(rp, it.second)
                } else {
                    Pair(rp, rplh)
                }
            }.toSet()
        }

        val lhs = closure.filter { it.first == lookingFor }
        val lh: Set<RuntimeRule> = when (lhs.size) {
            0 -> emptySet()
            1 -> lhs.first().second
            else -> {
                TODO();
            }
        }
        return lh
    }

    private fun calcExpectedItemRulePositions(rp: RulePosition): Set<RulePosition> {
        return rp.runtimeRule.calcExpectedRulePositions(rp.position)
    }

    private fun calcExpectedItemRulePositionTransitive(rp: RulePosition): Set<RulePosition> {
        var s = setOf(rp)//rp.runtimeRule.calcExpectedRulePositions(rp.position)

        return s.transitveClosure { rp ->
            if (RulePosition.END_OF_RULE == rp.position) {
                emptySet()
            } else {
                if (rp.runtimeRule.isTerminal) {
                    emptySet<RulePosition>()
                } else {
                    rp.runtimeRule.items(rp.choice, rp.position).flatMap {
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

    private fun calcGrowsInto(ruleNumber: Int): Set<RulePosition> {
        val rule = this.runtimeRules[ruleNumber]
        return this.runtimeRules.filter {
            it.isNonTerminal && it.rhs.items.contains(rule)
        }.flatMap {
            when (it.rhs.kind) {
                RuntimeRuleItemKind.EMPTY -> emptySet<RulePosition>()
                RuntimeRuleItemKind.CONCATENATION -> it.rhs.items.mapIndexedNotNull { index, item ->
                    if (item == rule) {
                        RulePosition(it, 0, index)
                    } else {
                        null
                    }
                }
                RuntimeRuleItemKind.CHOICE_EQUAL -> setOf(RulePosition(it, it.rhs.items.indexOf(rule), 0))
                RuntimeRuleItemKind.CHOICE_PRIORITY -> setOf(RulePosition(it, it.rhs.items.indexOf(rule), 0))
                RuntimeRuleItemKind.UNORDERED -> TODO()
                RuntimeRuleItemKind.MULTI -> setOf(RulePosition(it, RuntimeRuleItem.MULTI__ITEM, 0)) //do we also need empty and/or position 1?
                RuntimeRuleItemKind.SEPARATED_LIST -> it.rhs.items.mapIndexedNotNull { index, item ->
                    if (item == rule) {
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

    private fun calcIsSkipTerminal(rr: RuntimeRule): Boolean {
        val b = this.allSkipTerminals.contains(rr)
        return b
    }

    private fun calcFirstSuperNonTerminal(runtimeRule: RuntimeRule): List<RuntimeRule> {
        return this.runtimeRules.filter {
            it.isNonTerminal && it.couldHaveChild(runtimeRule, 0)
        } + if (runtimeRule.isEmptyRule) listOf(runtimeRule.ruleThatIsEmpty) else emptyList()
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