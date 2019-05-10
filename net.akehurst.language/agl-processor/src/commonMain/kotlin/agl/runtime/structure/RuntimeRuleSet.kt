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

    private fun createAllParserStates(userGoalRule: RuntimeRule, goalRP: RulePosition) {
        val stateMap = this.states_cache[userGoalRule]
        val startSet = goalRP.runtimeRule.rulePositions.map { rp ->
            val rps = stateMap.fetchOrCreateParseState(rp)
            RulePositionWithLookahead(rp, emptySet())
        }.toSet()
        startSet.transitveClosure { parent ->
            val parentRP = parent.rulePosition
            parentRP.items.flatMap { rr ->
                rr.rulePositions.mapNotNull { childRP ->
                    val childRPEnd = RulePosition(childRP.runtimeRule, childRP.choice, RulePosition.END_OF_RULE)
                    val childEndState = stateMap.fetchOrCreateParseState(childRPEnd)
                    val elh = this.calcLookahead(parent, childRPEnd, parent.lookahead)
                    childEndState.addParentRelation(ParentRelation(parentRP, elh))

                    val rps = stateMap.fetchOrCreateParseState(childRP)
                    val lh = this.calcLookahead(parent, childRP, parent.lookahead)
                    RulePositionWithLookahead(childRP, lh)
                }
            }.toSet()
        }
    }

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
                    childEndState.addParentRelation(ParentRelation(parentRP, elh))
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
    private fun createClosure(rootRulePosition: RulePosition, parentRelation: ParentRelation?): RulePositionClosure {
        // create path from root down, it may include items from root up
        val firstParentLh = parentRelation?.lookahead ?: emptySet()
        val rootWlh = RulePositionWithLookahead(rootRulePosition, firstParentLh) //TODO: get real LH
        val closureSet = setOf(rootWlh).transitveClosure { parent ->
            val parentRP = parent.rulePosition
            val parentLh = parent.lookahead
            parentRP.items.flatMap { rr ->
                val childrenRP = rr.calcExpectedRulePositions(0)
                childrenRP.map { childRP ->
                    val lh = this.calcLookahead(parent, childRP, parentLh)
                    RulePositionWithLookahead(childRP, lh)
                }
            }.toSet()
        }
        return RulePositionClosure(ClosureNumber(-1), rootRulePosition, closureSet)
    }

    private fun createClosure(root: ParserState, parentRelation: ParentRelation?): RulePositionClosure {
        // create path from root down, it may include items from root up
        val stateMap = root.stateSet
        val firstParentLh = parentRelation?.lookahead ?: emptySet()
        val rootWlh = RulePositionWithLookahead(root.rulePosition, firstParentLh) //TODO: get real LH
        val closureSet = setOf(rootWlh).transitveClosure { parent ->
            val parentRP = parent.rulePosition
            val parentLh = parent.lookahead
            parentRP.items.flatMap { rr ->
                val childrenRP = rr.calcExpectedRulePositions(0)
                childrenRP.map { childRP ->
                    //TODO: uncomment this if createAll is not called
                    val childRPEnd = RulePosition(childRP.runtimeRule, childRP.choice, RulePosition.END_OF_RULE)
                    val childEndState = stateMap.fetchOrCreateParseState(childRPEnd)
                    val elh = this.calcLookahead(parent, childRPEnd, parent.lookahead)
                    childEndState.addParentRelation(ParentRelation(parentRP, elh))
                    val lh = this.calcLookahead(parent, childRP, parentLh)
                    RulePositionWithLookahead(childRP, lh)
                }
            }.toSet()
        }
        return RulePositionClosure(ClosureNumber(-1), root.rulePosition, closureSet)
    }

    private fun createWidthTransition(from: ParserState, closureRPlh: RulePositionWithLookahead): Transition {
        val action = Transition.ParseAction.WIDTH
        val lookaheadGuard = closureRPlh.lookahead
        val to = from.stateSet.fetchOrCreateParseState(closureRPlh.rulePosition)
        return Transition(from, to, action, lookaheadGuard, null)
    }

    private fun createHeightTransition(from: ParserState, parentRelation: ParentRelation, previous: RulePosition?): Set<Transition> {
        val parentRP = parentRelation.rulePosition
        val prevGuard = parentRP //for height, previous must not match prevGuard
        val action = Transition.ParseAction.HEIGHT

        val toSet = parentRP.next().map { from.stateSet.fetchOrCreateParseState(it) }
        //val filteredToSet = toSet.filter { this.canGrowInto(it, previous) }
        return toSet.flatMap { to ->
            if (to.parentRelations.isEmpty()) {
                val lookaheadGuard = this.calcLookahead(null, from.rulePosition, parentRelation.lookahead)
                setOf(Transition(from, to, action, lookaheadGuard, prevGuard))
            } else {
                to.parentRelations.map { toParent ->
                    val lookaheadGuard = this.calcLookahead(RulePositionWithLookahead(toParent.rulePosition, toParent.lookahead), from.rulePosition, toParent.lookahead)
                    Transition(from, to, action, lookaheadGuard, prevGuard)
                }
            }

        }.toSet()
    }

    private fun createGraftTransition(from: ParserState, parentRelation: ParentRelation, previous: RulePosition?): Set<Transition> {
        val parentRP = parentRelation.rulePosition
        val prevGuard = parentRP //for graft, previous must match prevGuard
        val action = Transition.ParseAction.GRAFT
        val toSet = parentRP.next().map { from.stateSet.fetchOrCreateParseState(it) }
        //val filteredToSet = toSet.filter { this.canGrowInto(it, previous) }
        return toSet.flatMap { to ->
            if (to.parentRelations.isEmpty()) {
                val lookaheadGuard = this.calcLookahead(null, from.rulePosition, parentRelation.lookahead)
                setOf(Transition(from, to, action, lookaheadGuard, prevGuard))
            } else {
                to.parentRelations.map { toParent ->
                    val lookaheadGuard = this.calcLookahead(RulePositionWithLookahead(toParent.rulePosition, toParent.lookahead), from.rulePosition, toParent.lookahead)
                    Transition(from, to, action, lookaheadGuard, prevGuard)
                }
            }

        }.toSet()

    }

    private fun canGrowInto(pr: ParentRelation, prevRp: RulePosition?): Boolean {
        return when {
            null == prevRp -> true
            prevRp.isAtEnd -> false
            pr.rulePosition == prevRp -> true
            else -> {
                val prevClosure = this.createClosure(prevRp, null)
                prevClosure.content.any {
                    it.runtimeRule == pr.rulePosition.runtimeRule //&& it.lookahead == pr.lookahead
                }
            }
        }

    }
/*
    fun calcCanGrowInto(childRule: RuntimeRule, ancesstorRule: RuntimeRule, ancesstorItemIndex: Int): Boolean {
        return if (-1 == ancesstorItemIndex) {
            false
        } else {
            //return canGrowIntoAt_cache[childRule.number][ancesstorRule.number][ancesstorItemIndex];
            val index = IndexCanGrowIntoAt(childRule.number, ancesstorRule.number, ancesstorItemIndex)
            var result = canGrowIntoAt_cache[index]
            if (null==result) {
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
                                result =  true
                                break
                            }
                        } else {
                            val possibles = this.firstTerminals[rr.number]
                            if (possibles.contains(childRule)) {
                                result =  true
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
    */

    internal fun calcTransitions(from: ParserState, previous: RulePosition?): Set<Transition> { //TODO: add previous in order to filter parent relations
        val heightTransitions = mutableSetOf<Transition>()
        val graftTransitions = mutableSetOf<Transition>()
        val widthTransitions = mutableSetOf<Transition>()
        val goalTransitions = mutableSetOf<Transition>()

        val goal = from.runtimeRule.isGoal && from.isAtEnd
        if (goal) {
            val action = Transition.ParseAction.GOAL
            val to = from
            goalTransitions += Transition(from, to, action, emptySet(), null)
        } else {
            if (from.isAtEnd) {
                if (from.parentRelations.isEmpty()) {
                    //end of skip
                    val action = Transition.ParseAction.GOAL
                    val to = from
                    goalTransitions += Transition(from, to, action, emptySet(), null)
                } else {
                    val filteredRelations = from.parentRelations.filter { pr -> this.canGrowInto(pr, previous) }
                    for (parentRelation in filteredRelations) {
                        if (parentRelation.rulePosition.runtimeRule.isGoal) {
                            graftTransitions += this.createGraftTransition(from, parentRelation, previous)
                        } else {
                            val height = parentRelation.rulePosition.isAtStart
                            val graft = parentRelation.rulePosition.isAtStart.not()

                            if (height) {
                                heightTransitions += this.createHeightTransition(from, parentRelation, previous)
                            }
                            if (graft) {
                                graftTransitions += this.createGraftTransition(from, parentRelation, previous)
                            }
                        }
                    }
                }
            } else { //from.isAtEnd.not()
//                val parentRelations = from.stateSet.fetch(from.rulePosition.atEnd()).parentRelations
                val parentRelations = from.stateSet.fetchOrCreateParseState(from.rulePosition.atEnd()).parentRelations
                if (parentRelations.isEmpty()) {
                    val closure = this.createClosure(from, null)
                    for (closureRPlh in closure.content) {
                        val width = closureRPlh.runtimeRule.isTerminal

                        if (width) {
                            widthTransitions += this.createWidthTransition(from, closureRPlh)
                        }
                    }
                } else {
                    val filteredRelations = parentRelations.filter { pr -> this.canGrowInto(pr, previous) }
                    for (parentRelation in filteredRelations) {
                        val closure = this.createClosure(from, parentRelation)
                        for (closureRPlh in closure.content) {
                            val width = closureRPlh.runtimeRule.isTerminal

                            if (width) {
                                widthTransitions += this.createWidthTransition(from, closureRPlh)
                            }
                        }
                    }
                }
            }
        }

        //TODO: merge transitions with everything duplicate except lookahead (merge lookaheads)
        //not sure if this should be before or after the h/g conflict test.

        val conflictHeightTransitionPairs = mutableSetOf<Pair<Transition, Transition>>()
        for (trh in heightTransitions) {
            for (trg in graftTransitions) {
                if (trg.lookaheadGuard.containsAll(trh.lookaheadGuard)) {
                    val newTr = Transition(trh.from, trh.to, trh.action, trh.lookaheadGuard, trg.prevGuard)
                    conflictHeightTransitionPairs.add(Pair(trh, trg))
                }
            }
        }

        val conflictHeightTransitions = conflictHeightTransitionPairs.map {
            val trh = it.first
            val trg = it.second
            Transition(trh.from, trh.to, trh.action, trh.lookaheadGuard, trg.prevGuard)
        }
        //Note: need to do the conflict stuff to make multi work, at present!

        val newHeightTransitions =  (heightTransitions - conflictHeightTransitionPairs.map { it.first }) + conflictHeightTransitions

        //val newHeightTransitions = heightTransitions
        val mergedWidthTransitions = widthTransitions.groupBy { it.to }.map {
            Transition(from, it.key, Transition.ParseAction.WIDTH, it.value.flatMap { it.lookaheadGuard }.toSet(), null)
        }

        val mergedHeightTransitions = newHeightTransitions.groupBy { Pair(it.to, it.prevGuard) }.map {
            Transition(from, it.key.first, Transition.ParseAction.HEIGHT, it.value.flatMap { it.lookaheadGuard }.toSet(), it.key.second)
        }

        val mergedGraftTransitions = graftTransitions.groupBy { Pair(it.to, it.prevGuard) }.map {
            Transition(from, it.key.first, Transition.ParseAction.GRAFT, it.value.flatMap { it.lookaheadGuard }.toSet(), it.key.second)
        }

        val transitions = mergedHeightTransitions + mergedGraftTransitions + mergedWidthTransitions + goalTransitions
        return transitions.toSet()
    }

    fun buildCaches() {

    }

    fun startingState(userGoalRule: RuntimeRule): ParserState {
        val goalRule = RuntimeRuleSet.createGoalRule(userGoalRule)
        val goalRP = RulePosition(goalRule, 0, 0)
        //this.createAllParserStates(userGoalRule, goalRP)
        this.createAllSkipStates(userGoalRule)
        val stateSet = this.states_cache[userGoalRule]
        val startState = stateSet.fetchOrCreateParseState(goalRP)
        return startState
    }

    /*
        fun transitions(userGoalRule: RuntimeRule, from: ParserState): Set<Transition> {
            val transitions = this.transitions_cache[userGoalRule][from]
            if (null == transitions) {
                val t = this.calcTransitions(from)
                this.transitions_cache[userGoalRule][from] = t
                return t
            } else {
                return transitions
            }
        }
    */
    fun skipTransitions(userGoalRule: RuntimeRule, from: ParserState, previous: RulePosition): Set<Transition> {
        val transitions = this.skipTransitions_cache[userGoalRule][from]
        if (null == transitions) {
            val t = this.calcTransitions(from, null)//previous)
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
        val skipRuleStarts = allSkipRules.flatMap {
            val x = firstTerminals[it.number]
            RulePosition(it, 0, 0)
            it.rulePositionsAt[0]
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