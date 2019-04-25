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
import net.akehurst.language.collections.transitveClosure
import net.akehurst.language.parser.scannerless.InputFromCharSequence

class RuntimeRuleSet(rules: List<RuntimeRule>) {

    companion object {
        val GOAL_RULE_NUMBER = -1;
        val EOT_RULE_NUMBER = -2;
        val END_OF_TEXT = RuntimeRule(EOT_RULE_NUMBER, InputFromCharSequence.END_OF_TEXT, RuntimeRuleKind.TERMINAL, false, false)

        fun createGoalRule(userGoalRule: RuntimeRule): RuntimeRule {
            val gr = RuntimeRule(GOAL_RULE_NUMBER, "<GOAL>", RuntimeRuleKind.GOAL, false, false)
            gr.rhsOpt = RuntimeRuleItem(RuntimeRuleItemKind.CONCATENATION, -1, 0, arrayOf(userGoalRule, END_OF_TEXT))
            return gr
        }

    }

    private val nonTerminalRuleNumber: MutableMap<String, Int> = mutableMapOf()
    private val terminalRuleNumber: MutableMap<String, Int> = mutableMapOf()

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

    val expectedTerminalRulePositions = lazyMap<RulePosition, Array<RulePosition>> {
        calcExpectedTerminalRulePositions(it).toTypedArray()
    }

    val firstTerminals2 = lazyMap<RulePosition, Set<RuntimeRule>> {
        val trps = expectedTerminalRulePositions[it] ?: arrayOf()
        trps.flatMap { it.items }.toSet()
    }

    /** Map of userGoalRule -> next Closure number **/
    private val nextClosure = mutableMapOf<RuntimeRule, ClosureNumber>()

    private val states_cache = lazyMapNonNull<RuntimeRule, ParserStateSet> {
        ParserStateSet()
    }
    private val skipPaths = ParserStateSet()

    private val transitions_cache = lazyMapNonNull<RuntimeRule, MutableMap<ParserState, Set<Transition>>> { userGoalRule ->
        mutableMapOf()
    }
    private val skipTransitions_cache = lazyMapNonNull<RuntimeRule, MutableMap<ParserState, Set<Transition>>> { userGoalRule ->
        mutableMapOf()
    }

    init {
        for (rr in rules) {
            if (rr.isNonTerminal) {
                this.nonTerminalRuleNumber[rr.name] = rr.number
            } else {
                this.terminalRuleNumber[rr.name] = rr.number
            }
        }
    }
/*
    fun fetchNextStates(state: ParserState): Set<ParserState> {
        if (null == state.directParent) {
            return state.rulePositionWlh.rulePosition.next().map { nextRP ->
                val childRPS = RulePositionWithLookahead(nextRP, emptySet())
                state.stateMap.fetchNextParseState(childRPS, null)
            }.toSet()
        } else {
            val parentRPwl = state.directParent.rulePositionWlh
            val possibleParents = state.stateMap.fetchAll(parentRPwl.rulePosition)
            return state.rulePositionWlh.rulePosition.next().flatMap { nextRP ->
                possibleParents.flatMap { posParentState ->
                    val posParentRPwl = posParentState.rulePositionWlh
                    val posParentLH = posParentRPwl.graftLookahead
                    val lh = this.calcLookahead(posParentRPwl, nextRP, posParentLH)
                    val childRPS = RulePositionWithLookahead(nextRP, lh)
                    state.stateMap.fetchAll(childRPS)
                }
            }.toSet()
        }
    }
*/
    /*
        private fun createAllParserStates(userGoalRule: RuntimeRule, goalRP: RulePositionWithLookahead) {
            val stateMap = this.states_cache[userGoalRule]
            val startSet = goalRP.runtimeRule.rulePositions.map { rp ->
                val rps = RulePositionWithLookahead(rp, emptySet())
                val rpp = stateMap.fetchOrCreateParseState(rps, null)
                rpp
            }.toSet()
            startSet.transitveClosure { parent ->
                val parentRP = parent.rulePositionWlh
                parentRP.items.flatMap { rr ->
                    rr.rulePositions.mapNotNull { childRP ->
                        val lh = this.calcLookahead(parentRP, childRP, parentRP.graftLookahead)
                        //val nlh = this.calcNextLookahead(parentRP, childRP, parentRP.lookahead)
                        val childRPS = RulePositionWithLookahead(childRP, lh)
                        val rpp = stateMap.fetchOrCreateParseState(childRPS, parent)
                        rpp
                    }
                }.toSet()
            }
        }
    */
    private fun createAllSkipStates(userGoalRule: RuntimeRule) {
        val stateMap = this.skipPaths
        val startSet = this.allSkipRules.flatMap { skipRule ->
            skipRule.rulePositions.map { rp ->
                stateMap.fetchOrCreateParseState(rp)
                RulePositionWithLookahead(rp, emptySet())
            }
        }.toSet()
        startSet.transitveClosure { parent ->
            val parentRP = parent.rulePosition
            parentRP.items.flatMap { rr ->
                rr.rulePositions.mapNotNull { childRP ->
                    val childRPEnd = RulePosition(childRP.runtimeRule, childRP.choice, RulePosition.END_OF_RULE)
                    val childEndState = stateMap.fetchOrCreateParseState(childRPEnd)
                    val elh = this.calcLookahead(parent, childRPEnd, parent.lookahead)
                    childEndState.parentRelations.add(ParentRelation(parentRP, elh))
                    val lh = this.calcLookahead(parent, childRP, parent.lookahead)
                    RulePositionWithLookahead(childRP, lh)
                }
            }.toSet()
        }
    }

    private fun calcNextClosureNumber(userGoalRule: RuntimeRule): ClosureNumber {
        val num: ClosureNumber = this.nextClosure[userGoalRule] ?: ClosureNumber(0)
        this.nextClosure[userGoalRule] = ClosureNumber(num.value + 1)
        return num
    }

    fun fetchSkipStates(rulePosition: RulePosition): ParserState {
        return this.skipPaths.fetch(rulePosition)
    }

    private fun calcNextLookahead(parent: RulePositionWithLookahead?, childRP: RulePosition, ifEmpty: Set<RuntimeRule>): Set<RuntimeRule> {
        return if (childRP.isAtEnd) {
            // lh should be FIRST of next item in parent, so it can be tested against this childRP
            if (null == parent) {
                ifEmpty
            } else {
                if (parent.isAtEnd) {
                    parent.lookahead
                } else {
                    val nextRPs = parent.rulePosition.next() //nextRulePosition(parent.rulePosition, childRP.runtimeRule)
                    nextRPs.flatMap { nextRP ->
                        if (nextRP.isAtEnd) {
                            calcLookahead(null, parent.rulePosition, parent.lookahead)
                        } else {
                            val lh: Set<RuntimeRule> = this.firstTerminals2[nextRP] ?: throw ParseException("should never happen")
                            if (lh.isEmpty()) {
                                throw ParseException("should never happen")
                            } else {
                                lh
                            }
                        }
                    }.toSet()
                }
            }
        } else {
            //this childRP will not itself be applied to Height or GRAFT,
            // however it should carry the FIRST of next in the child,
            // so that this childs children can use it if needed
            childRP.items.flatMap { fstChildItem ->
                val nextRPs = childRP.next() //nextRulePosition(childRP, fstChildItem)
                nextRPs.flatMap { nextChildRP ->
                    if (nextChildRP.isAtEnd) {
                        if (null == parent) {
                            ifEmpty
                        } else {
                            calcLookahead(null, parent.rulePosition, parent.lookahead)
                        }
                    } else {

                        nextChildRP.items.flatMap { sndChildItem ->
                            val nextNextRPs = nextChildRP.next() //nextRulePosition(nextChildRP, sndChildItem)
                            nextNextRPs.flatMap { nextNextChildRP ->
                                if (nextNextChildRP.isAtEnd) {
                                    if (null == parent) {
                                        ifEmpty
                                    } else {
                                        calcLookahead(null, parent.rulePosition, parent.lookahead)
                                    }
                                } else {
                                    val lh: Set<RuntimeRule> = this.firstTerminals2[nextNextChildRP] ?: throw ParseException("should never happen")
                                    if (lh.isEmpty()) {
                                        throw ParseException("should never happen")
                                    } else {
                                        lh
                                    }
                                }
                            }
                        }
                    }
                }
            }.toSet()
        }
    }

    private fun calcLookahead(parent: RulePositionWithLookahead?, childRP: RulePosition, ifEmpty: Set<RuntimeRule>): Set<RuntimeRule> {
        return if (childRP.isAtEnd) {
            // lh should be FIRST of next item in parent, so it can be tested against this childRP
            if (null == parent) {
                ifEmpty
            } else {
                if (parent.isAtEnd) {
                    parent.lookahead
                } else {
                    val nextRPs = parent.rulePosition.next()//nextRulePosition(parent.rulePosition, childRP.runtimeRule)
                    nextRPs.flatMap { nextRP ->
                        if (nextRP.isAtEnd) {
                            calcLookahead(null, parent.rulePosition, parent.lookahead)
                        } else {
                            val lh: Set<RuntimeRule> = this.firstTerminals2[nextRP] ?: throw ParseException("should never happen")
                            if (lh.isEmpty()) {
                                throw ParseException("should never happen")
                            } else {
                                lh
                            }
                        }
                    }.toSet()
                }
            }
        } else {
            //this childRP will not itself be applied to Height or GRAFT,
            // however it should carry the FIRST of next in the child,
            // so that this childs children can use it if needed
            childRP.items.flatMap { fstChildItem ->
                val nextRPs = childRP.next() //nextRulePosition(childRP, fstChildItem)
                nextRPs.flatMap { nextChildRP ->
                    if (nextChildRP.isAtEnd) {
                        if (null == parent) {
                            ifEmpty
                        } else {
                            calcLookahead(null, parent.rulePosition, parent.lookahead)
                        }
                    } else {
                        val lh: Set<RuntimeRule> = this.firstTerminals2[nextChildRP] ?: throw ParseException("should never happen")
                        if (lh.isEmpty()) {
                            throw ParseException("should never happen")
                        } else {
                            lh
                        }
                    }
                }
            }.toSet()
        }
    }

    /*
        private fun createClosure(userGoalRule: RuntimeRule, root: ParserState): RulePositionClosure {
            // create path from root down, it may include items from root up
            val states = root.stateMap
            val tempRoot = TemporaryParserState(null, root.rulePosition)
            val closureSet = setOf(tempRoot).transitveClosure { parentState ->
                val parentRP = parentState.rulePositionWlh
                val ancestors =  parentRP
                parentRP.items.flatMap { rr ->
                    val childrenRP = rr.calcExpectedRulePositions(0)
                    childrenRP.mapNotNull { childRP ->
                        //val childAncestors = root.ancestorRPs + ancestorRPs
                        val lh = this.calcLookahead(parentRP, childRP, parentRP.lookahead)
                        //val nlh = this.calcNextLookahead(parentRP, childRP, parentRP.lookahead)
                        val childRP = RulePositionWithLookahead(childRP, lh)
                        val childState = TemporaryParserState(ancestors, childRP)
                        childState
                    }
                }.toSet()
            }

            val ancestorMap = mutableMapOf<RulePositionWithLookahead, ParserState?>()
            val rootParent = root.directParent ?: null
            val rootParentRPwl = rootParent?.rulePositionWlh
            val rootParentGlh = rootParent?.rulePositionWlh?.lookahead ?: emptySet()
            ancestorMap[tempRoot.rulePositionWlh] = rootParent
            val extendedStates = mutableSetOf<ParserState>()

            for(tps in closureSet) {
                if (null==tps.directParent) {
                    //must be tempRoot, and a state for this already exists, so only need to try create next states
                    tps.rulePositionWlh.rulePosition.next().forEach { nextRP ->
                        val lh = this.calcLookahead(rootParentRPwl, nextRP, rootParentGlh)
                        //val nlh = this.calcNextLookahead(rootParentRPwl, nextRP, rootParentGlh)
                        val childRPwlh = RulePositionWithLookahead(nextRP, lh)
                        states.fetchOrCreateParseState(childRPwlh, rootParent)
                    }
                } else {
                    val parentParent = ancestorMap[tps.directParent]// ?: throw ParseException("should never be null")
                    val parent = states.fetch(tps.directParent, parentParent)
                    val ps = states.fetchOrCreateParseState(tps.rulePositionWlh, parent)
                    ancestorMap[ps.rulePositionWlh] = ps.directParent
                    extendedStates.add(ps)
                    //try create next states
                    val parentRPl = parent.rulePositionWlh
                    tps.rulePositionWlh.rulePosition.next().forEach { nextRP ->
                        val lh = this.calcLookahead(parentRPl, nextRP, parentRPl.lookahead)
                        //val nlh = this.calcNextLookahead(parentRPl, nextRP, parentRPl.lookahead)
                        val nextRPwlh = RulePositionWithLookahead(nextRP, lh)
                        states.fetchOrCreateParseState(nextRPwlh, parent)
                    }
                }
            }
            val closureNumber = this.calcNextClosureNumber(userGoalRule)
            return RulePositionClosure(closureNumber, root.rulePositionWlh.rulePosition, extendedStates)
        }
    */
    private fun createClosure(userGoalRule: RuntimeRule, root: ParserState): RulePositionClosure {
        // create path from root down, it may include items from root up
        val stateMap = root.stateMap
        val rootWlh = RulePositionWithLookahead(root.rulePosition, emptySet()) //TODO: get real LH
        val closureSet = setOf(rootWlh).transitveClosure { parent ->
            val parentRP = parent.rulePosition
            val parentLh = parent.lookahead
            parentRP.items.flatMap { rr ->
                val childrenRP = rr.calcExpectedRulePositions(0)
                childrenRP.mapNotNull { childRP ->
                    val childRPEnd = RulePosition(childRP.runtimeRule, childRP.choice, RulePosition.END_OF_RULE)
                    val childEndState = stateMap.fetchOrCreateParseState(childRPEnd)
                    val elh = this.calcLookahead(parent, childRPEnd, parent.lookahead)
                    childEndState.parentRelations.add(ParentRelation(parentRP, elh))
                    val lh = this.calcLookahead(parent, childRP, parentLh)
                    RulePositionWithLookahead(childRP, lh)
                }
            }.toSet()
        }
        val closureNumber = this.calcNextClosureNumber(userGoalRule)
        return RulePositionClosure(closureNumber, root.rulePosition, closureSet)
    }

    private fun createWidthTransition(from: ParserState, closureRPlh: RulePositionWithLookahead): Transition {
        val action = Transition.ParseAction.WIDTH
        val item = closureRPlh.runtimeRule
        val lookaheadGuard = closureRPlh.lookahead
        val to = from.stateMap.fetchOrCreateParseState(closureRPlh.rulePosition)
        return Transition(from, to, action, item, lookaheadGuard, null)
    }

    private fun createHeightTransition(from: ParserState, parentRelation: ParentRelation): Set<Transition> {
        val parentRP = parentRelation.rulePosition
        val prevGuard = parentRP //for height, previous must not match prevGuard
        val action = Transition.ParseAction.HEIGHT
        val item = from.runtimeRule
        val lookaheadGuard = parentRelation.lookahead
        val toSet = parentRP.next().map { from.stateMap.fetchOrCreateParseState(it) }
        return toSet.map { to ->
            Transition(from, to, action, item, lookaheadGuard, prevGuard)
        }.toSet()
    }

    private fun createGraftTransition(from: ParserState, parentRelation: ParentRelation): Set<Transition> {
        val parentRP = parentRelation.rulePosition
        val prevGuard = parentRP //for graft, previous must match prevGuard
        val action = Transition.ParseAction.GRAFT
        val item = from.runtimeRule
        val lookaheadGuard = parentRelation.lookahead
        val toSet = parentRP.next().map { from.stateMap.fetchOrCreateParseState(it) }
        return toSet.map { to ->
            Transition(from, to, action, item, lookaheadGuard, prevGuard)
        }.toSet()

    }

    private fun calcTransitions(userGoalRule: RuntimeRule, from: ParserState): Set<Transition> {
        val transitions = mutableSetOf<Transition>()

        val goal = from.runtimeRule.isGoal && from.isAtEnd
        if (goal) {
            val action = Transition.ParseAction.GOAL
            val to = from
            transitions += Transition(from, to, action, RuntimeRuleSet.END_OF_TEXT, emptySet(), null)
        } else {
            if (from.isAtEnd) {
                if (from.parentRelations.isEmpty()) {
                    //end of skip
                    val action = Transition.ParseAction.GOAL
                    val to = from
                    transitions += Transition(from, to, action, RuntimeRuleSet.END_OF_TEXT, emptySet(), null)
                } else {
                    for (parentRelation in from.parentRelations) {
                        if (parentRelation.rulePosition.runtimeRule.isGoal) {
                            transitions += this.createGraftTransition(from,parentRelation)
                        } else {
                            val height = parentRelation.rulePosition.isAtStart
                            val graft = parentRelation.rulePosition.isAtStart.not()

                            if (height) {
                                transitions += this.createHeightTransition(from,parentRelation)
                            }
                            if (graft) {
                                transitions += this.createGraftTransition(from,parentRelation)
                            }
                        }
                    }
                }
            } else { //from.isAtEnd.not()
                val closure = this.createClosure(userGoalRule, from)
                for (closureRPlh in closure.content) {
                    val width = closureRPlh.runtimeRule.isTerminal

                    if (width) {
                        transitions += this.createWidthTransition(from, closureRPlh)
                    }
                }
            }
        }
        return transitions
    }

    fun buildCaches() {

    }

    fun startingState(userGoalRule: RuntimeRule): ParserState {
        val goalRule = RuntimeRuleSet.createGoalRule(userGoalRule)
        val goalRP = RulePosition(goalRule, 0, 0)
        this.createAllSkipStates(userGoalRule)
        val stateSet = this.states_cache[userGoalRule]
        val startState = stateSet.fetchOrCreateParseState(goalRP)
        return startState
    }

    fun transitions(userGoalRule: RuntimeRule, from: ParserState): Set<Transition> {
        val transitions = this.transitions_cache[userGoalRule][from]
        if (null == transitions) {
            val t = this.calcTransitions(userGoalRule, from)
            this.transitions_cache[userGoalRule][from] = t
            return t
        } else {
            return transitions
        }
    }

    fun skipTransitions(userGoalRule: RuntimeRule, from: ParserState): Set<Transition> {
        val transitions = this.skipTransitions_cache[userGoalRule][from]
        if (null == transitions) {
            val t = this.calcTransitions(userGoalRule, from)
            this.skipTransitions_cache[userGoalRule][from] = t
            return t
        } else {
            return transitions
        }
    }

    // ---

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