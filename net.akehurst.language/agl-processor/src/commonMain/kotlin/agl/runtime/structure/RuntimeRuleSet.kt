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

import net.akehurst.language.agl.parser.InputFromCharSequence
import net.akehurst.language.agl.runtime.graph.GrowingNode
import net.akehurst.language.api.parser.ParserException
import net.akehurst.language.collections.lazyMap
import net.akehurst.language.collections.lazyMapNonNull
import net.akehurst.language.collections.transitiveClosure


class RuntimeRuleSet(rules: List<RuntimeRule>) {

    companion object {
        val GOAL_RULE_NUMBER = -1;
        val EOT_RULE_NUMBER = -2;
        val END_OF_TEXT = RuntimeRule(EOT_RULE_NUMBER, "<EOT>", InputFromCharSequence.END_OF_TEXT, RuntimeRuleKind.TERMINAL, false, false)

        fun createGoalRule(userGoalRule: RuntimeRule): RuntimeRule {
            return createGoalRule(userGoalRule, listOf(END_OF_TEXT))
        }

        fun createGoalRule(userGoalRule: RuntimeRule, possibleEndOfText: List<RuntimeRule>): RuntimeRule {
            val gr = RuntimeRule(GOAL_RULE_NUMBER, "<GOAL>", "", RuntimeRuleKind.GOAL, false, false)
            val items = listOf(userGoalRule) + possibleEndOfText
            gr.rhsOpt = RuntimeRuleItem(RuntimeRuleItemKind.CONCATENATION, RuntimeRuleChoiceKind.NONE, -1, 0, items.toTypedArray())
            return gr
        }

    }

    private val nonTerminalRuleNumber: MutableMap<String, Int> = mutableMapOf()
    private val terminalRuleNumber: MutableMap<String, Int> = mutableMapOf()
    private val embeddedRuleNumber: MutableMap<String, Int> = mutableMapOf()

    //TODO: are Arrays faster than Lists?
    val runtimeRules: Array<out RuntimeRule> by lazy {
        rules.sortedBy { it.number }.toTypedArray()
    }

    val skipRules: Array<RuntimeRule> by lazy {
        this.runtimeRules.filter { it.isSkip }.toTypedArray()
    }

    /*
    // used to add to lookahead
    val allSkipTerminals: Array<RuntimeRule> by lazy {
        this.allSkipRules.flatMap {
            if (it.isTerminal)
                listOf(it)
            else
                it.rhs.items.filter { it.isTerminal }
        }.toTypedArray()
    }
    */
    // used to add to lookahead
    val firstSkipTerminals: Array<RuntimeRule> by lazy {
        this.skipRules.flatMap {
            firstTerminals[it.number]
        }.toTypedArray()
    }

    /*
    val isSkipTerminal: Array<Boolean> by lazy {
        this.runtimeRules.map {
            this.calcIsSkipTerminal(it)
        }.toTypedArray()
    }
*/
    val nonSkipRules: Array<RuntimeRule> by lazy {
        this.runtimeRules.filter { it.isSkip.not() }.toTypedArray()
    }

    // used if scanning (excluding skip)
    val nonSkipTerminals: Array<RuntimeRule> by lazy {
        this.nonSkipRules.flatMap {
            when (it.kind) {
                RuntimeRuleKind.GOAL -> emptyList()
                RuntimeRuleKind.TERMINAL -> listOf(it)
                RuntimeRuleKind.NON_TERMINAL -> emptyList()//it.rhs.items.filter { it.kind == RuntimeRuleKind.TERMINAL }
                RuntimeRuleKind.EMBEDDED -> it.embeddedRuntimeRuleSet!!.nonSkipTerminals.toList()
            }
        }.toTypedArray()
    }

    // used if scanning (including skip)
    val terminalRules: Array<RuntimeRule> by lazy {
        this.runtimeRules.flatMap {
            when (it.kind) {
                RuntimeRuleKind.GOAL -> emptyList()
                RuntimeRuleKind.TERMINAL -> listOf(it)
                RuntimeRuleKind.NON_TERMINAL -> emptyList()
                RuntimeRuleKind.EMBEDDED -> it.embeddedRuntimeRuleSet!!.terminalRules.toList()
            }
        }.toTypedArray()
    }

    val firstTerminals: Array<Set<RuntimeRule>> by lazy {
        this.runtimeRules.map { this.calcFirstTerminals(it) }
                .toTypedArray()
    }

    val firstSkipRuleTerminalPositions: Set<RuntimeRule> by lazy {
        this.runtimeRules.filter { it.isSkip }.flatMap { this.calcFirstTerminals(it) }.toSet()
//        this.calcFirstTerminalSkipRulePositions()
    }

    // used when calculating lookahead
    val expectedTerminalRulePositions = lazyMap<RulePosition, Array<RulePosition>> {
        calcExpectedTerminalRulePositions(it).toTypedArray()
    }

    // used when calculating lookahead
    val firstTerminals2 = lazyMap<RulePosition, Set<RuntimeRule>> {
        val trps = expectedTerminalRulePositions[it] ?: arrayOf()
        trps.flatMap { it.items }.toSet()
    }

    // userGoalRule -> ParserStateSet
    private val states_cache = mutableMapOf<RuntimeRule, ParserStateSet>()
    private val skipStateSet = mutableMapOf<RuntimeRule, ParserStateSet>()

    private val skipTransitions_cache = lazyMapNonNull<RuntimeRule, MutableMap<ParserState, Set<Transition>>> { userGoalRule ->
        mutableMapOf()
    }

    //called from ParserStateSet, which adds the Goal Rule bits
    internal val parentPosition = lazyMapNonNull<RuntimeRule, Set<RulePosition>> { childRR ->
        this.runtimeRules.flatMap { rr ->
            val rps = rr.rulePositions
            val f = rps.filter { rp ->
                rp.items.contains(childRR)
            }
            f
        }.toSet()
    }

    init {
        for (rr in rules) {
            when (rr.kind) {
                RuntimeRuleKind.GOAL -> { /*do nothing*/
                }
                RuntimeRuleKind.TERMINAL -> this.terminalRuleNumber[rr.tag] = rr.number
                RuntimeRuleKind.NON_TERMINAL -> this.nonTerminalRuleNumber[rr.tag] = rr.number
                RuntimeRuleKind.EMBEDDED -> this.embeddedRuleNumber[rr.tag] = rr.number
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
    private fun createAllParserStates(userGoalRule: RuntimeRule, goalRP: RulePosition) {
        val stateMap = this.states_cache[userGoalRule]
        val startSet = goalRP.runtimeRule.rulePositions.map { rp ->
            val rps = stateMap!!.fetchOrCreateParseState(rp, emptySet())
            RulePositionWithLookahead(rp, emptySet())
        }.toSet()
        startSet.transitiveClosure { parent ->
            val parentRP = parent.rulePosition
            parentRP.items.flatMap { rr ->
                rr.rulePositions.mapNotNull { childRP ->
                    //val childRPEnd = RulePosition(childRP.runtimeRule, childRP.choice, RulePosition.END_OF_RULE)
                    //val elh = this.calcLookahead(parent, childRPEnd, parent.lookahead)
                    //val childEndState = stateMap!!.fetchOrCreateParseState(childRPEnd, elh)
                    //childEndState.addParentRelation(ParentRelation(parentRP, elh))
                    val lh = this.calcLookahead(parent, childRP, parent.lookahead)
                    //val rps = stateMap.fetchOrCreateParseState(childRP, lh)
                    RulePositionWithLookahead(childRP, lh)
                }
            }.toSet()
        }
    }
*/
    internal fun createAllSkipStates() {
        this.skipRules.forEach { skipRule ->
            val stateSet = ParserStateSet(this, skipRule, emptyList())
            this.skipStateSet[skipRule] = stateSet
            val startSet = skipRule.rulePositions.map { rp ->
                stateSet.fetchOrCreateParseState(rp)
                RulePositionWithLookahead(rp, emptySet())
            }.toSet()
            startSet.transitiveClosure { parent ->
                val parentRP = parent.rulePosition
                parentRP.items.flatMap { rr ->
                    rr.rulePositions.mapNotNull { childRP ->
                        val childRPEnd = RulePosition(childRP.runtimeRule, childRP.choice, RulePosition.END_OF_RULE)
                        //val elh = this.calcLookahead(parent, childRPEnd, parent.lookahead)
                        val childEndState = stateSet.fetchOrCreateParseState(childRPEnd) // create state!
                        val lh = this.calcLookahead(parent, childRP, parent.lookahead)
                        //childEndState.addParentRelation(ParentRelation(parentRP, elh))
                        RulePositionWithLookahead(childRP, lh)
                        //TODO: this seems different to the other closures!
                    }
                }.toSet()
            }
        }
    }

    fun fetchSkipStates(rulePosition: RulePosition): ParserState {
        return this.skipStateSet.values.mapNotNull { it.fetchOrNull(rulePosition) }.first() //TODO: maybe more than 1 !
    }

    internal fun calcLookahead(parent: RulePositionWithLookahead?, childRP: RulePosition, ifEmpty: Set<RuntimeRule>): Set<RuntimeRule> {
        return when (childRP.runtimeRule.kind) {
            RuntimeRuleKind.TERMINAL -> useParentLH(parent, ifEmpty)
            RuntimeRuleKind.EMBEDDED -> useParentLH(parent, ifEmpty)
            //val rr = childRP.runtimeRule
            //rr.embeddedRuntimeRuleSet!!.firstTerminals[rr.embeddedStartRule!!.number]
            //}
            RuntimeRuleKind.GOAL -> when (childRP.position) {
                0 -> childRP.runtimeRule.rhs.items.drop(1).toSet()
                else -> emptySet()
            }
            RuntimeRuleKind.NON_TERMINAL -> {
                when {
                    childRP.isAtEnd -> useParentLH(parent, ifEmpty)
                    else -> {
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
                                    val lh: Set<RuntimeRule> = this.firstTerminals2[nextChildRP]
                                            ?: error("should never happen")
                                    if (lh.isEmpty()) {
                                        error("should never happen")
                                    } else {
                                        lh
                                    }
                                }
                            }
                        }.toSet()
                    }
                }
            }
        }
    }

    private fun useParentLH(parent: RulePositionWithLookahead?, ifEmpty: Set<RuntimeRule>): Set<RuntimeRule> {
        return if (null == parent) {
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
                        val lh: Set<RuntimeRule> = this.firstTerminals2[nextRP] ?: error("should never happen")
                        if (lh.isEmpty()) {
                            error("should never happen")
                        } else {
                            lh
                        }
                    }
                }.toSet()
            }
        }
    }
/*
    private fun createClosure(rootRulePosition: RulePosition, parentRelation: ParentRelation?): RulePositionClosure {
        // create path from root down, it may include items from root up
        val firstParentLh = parentRelation?.lookahead ?: emptySet()
        val rootWlh = RulePositionWithLookahead(rootRulePosition, firstParentLh) //TODO: get real LH
        val closureSet = setOf(rootWlh).transitiveClosure { parent ->
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
*/
    private fun createWidthTransition(from: ParserState, closureRPlh: RulePositionWithLookahead): Transition {
        val action = Transition.ParseAction.WIDTH
        val lookaheadGuard = closureRPlh.lookahead
        val toRP = RulePosition(closureRPlh.runtimeRule, closureRPlh.choice, RulePosition.END_OF_RULE)
        val to = from.stateSet.fetchOrCreateParseState(toRP) //TODO: state also depends on lh (or parentRels!)
        return Transition(from, to, action, lookaheadGuard, null) { _, _ -> true }
    }

    private fun createHeightTransition(from: ParserState, parentRelation: ParentRelation): Set<Transition> {
        val parentRP = parentRelation.rulePosition
        val prevGuard = parentRP //for height, previous must not match prevGuard
        val action = Transition.ParseAction.HEIGHT

        val toSet = parentRP.next().map { from.stateSet.fetchOrCreateParseState(it) }
        //val filteredToSet = toSet.filter { this.canGrowInto(it, previous) }
        return toSet.flatMap { to ->
            if (to.parentRelations.isEmpty()) {
                val lookaheadGuard = this.calcLookahead(null, from.rulePosition, parentRelation.lookahead)
                setOf(Transition(from, to, action, lookaheadGuard, prevGuard) { _, _ -> true })
            } else {
                to.parentRelations.map { toParent ->
                    val lh = when {
                        to.isAtEnd -> toParent.lookahead
                        else -> to.stateSet.calcLookahead(parentRelation, from.rulePosition)
                    }
                    val lookaheadGuard = lh //parentRelation.lookahead //to.stateSet.calcLookahead(toParent,from.rulePosition) //parentRelation.lookahead //this.calcLookahead(RulePositionWithLookahead(to.rulePosition, parentRelation.lookahead), from.rulePosition, parentRelation.lookahead)
                    Transition(from, to, action, lookaheadGuard, prevGuard) { _, _ -> true }
                }
            }

        }.toSet()
    }

    val multiRuntimeGuard: Transition.(GrowingNode) -> Boolean = { gn: GrowingNode ->
        val previousRp = gn.currentState.rulePosition
        when {
            previousRp.isAtEnd -> gn.children.size + 1 >= gn.runtimeRule.rhs.multiMin
            previousRp.position == RulePosition.MULIT_ITEM_POSITION -> {
                //if the to state creates a complete node then min must be >= multiMin
                if (this.to.isAtEnd) {
                    val minSatisfied = gn.children.size + 1 >= gn.runtimeRule.rhs.multiMin
                    val maxSatisfied = -1 == gn.runtimeRule.rhs.multiMax || gn.children.size + 1 <= gn.runtimeRule.rhs.multiMax
                    minSatisfied && maxSatisfied
                } else {
                    -1 == gn.runtimeRule.rhs.multiMax || gn.children.size + 1 <= gn.runtimeRule.rhs.multiMax
                }

            }
            else -> true
        }
    }
    val sListRuntimeGuard: Transition.(GrowingNode) -> Boolean = { gn: GrowingNode ->
        val previousRp = gn.currentState.rulePosition
        when {
            previousRp.isAtEnd -> (gn.children.size / 2) + 1 >= gn.runtimeRule.rhs.multiMin
            previousRp.position == RulePosition.SLIST_ITEM_POSITION -> {
                //if the to state creates a complete node then min must be >= multiMin
                if (this.to.isAtEnd) {
                    val minSatisfied = (gn.children.size / 2) + 1 >= gn.runtimeRule.rhs.multiMin
                    val maxSatisfied = -1 == gn.runtimeRule.rhs.multiMax || (gn.children.size / 2) + 1 <= gn.runtimeRule.rhs.multiMax
                    minSatisfied && maxSatisfied
                } else {
                    -1 == gn.runtimeRule.rhs.multiMax || (gn.children.size / 2) + 1 <= gn.runtimeRule.rhs.multiMax
                }
            }
            previousRp.position == RulePosition.SLIST_SEPARATOR_POSITION -> {
                //if the to state creates a complete node then min must be >= multiMin
                if (this.to.isAtEnd) {
                    val minSatisfied = (gn.children.size / 2) + 1 >= gn.runtimeRule.rhs.multiMin
                    val maxSatisfied = -1 == gn.runtimeRule.rhs.multiMax || (gn.children.size / 2) + 1 < gn.runtimeRule.rhs.multiMax
                    minSatisfied && maxSatisfied
                } else {
                    -1 == gn.runtimeRule.rhs.multiMax || (gn.children.size / 2) + 1 < gn.runtimeRule.rhs.multiMax
                }
            }
            else -> true
        }
    }

    private fun createGraftTransition(from: ParserState, parentRelation: ParentRelation): Set<Transition> {
        val parentRP = parentRelation.rulePosition
        val prevGuard = parentRP //for graft, previous must match prevGuard
        val action = Transition.ParseAction.GRAFT
        val toSet = parentRP.next().map { from.stateSet.fetchOrCreateParseState(it) }
        //val filteredToSet = toSet.filter { this.canGrowInto(it, previous) }
        val runtimeGuard: Transition.(GrowingNode, RulePosition?) -> Boolean = { gn, previous ->
            if (null == previous) {
                true
            } else {
                when (previous.runtimeRule.rhs.kind) {
                    RuntimeRuleItemKind.MULTI -> multiRuntimeGuard.invoke(this, gn)
                    RuntimeRuleItemKind.SEPARATED_LIST -> sListRuntimeGuard.invoke(this, gn)
                    else -> true
                }
            }
        }
        return toSet.flatMap { to ->
            if (to.parentRelations.isEmpty()) {
                val lookaheadGuard = this.calcLookahead(null, from.rulePosition, parentRelation.lookahead)
                setOf(Transition(from, to, action, lookaheadGuard, prevGuard, runtimeGuard))
            } else {
                to.parentRelations.map { toParent ->
                    val lh = when {
                        to.isAtEnd -> toParent.lookahead
                        else -> to.stateSet.calcLookahead(parentRelation, from.rulePosition)
                    }
                    val lookaheadGuard = lh //toParent.lookahead //to.stateSet.calcLookahead(toParent,from.rulePosition)  //this.calcLookahead(RulePositionWithLookahead(toParent.rulePosition, toParent.lookahead), from.rulePosition, toParent.lookahead)
                    Transition(from, to, action, lookaheadGuard, prevGuard, runtimeGuard)
                }
            }

        }.toSet()

    }

    private fun createEmbeddedTransition(from: ParserState, closureRPlh: RulePositionWithLookahead): Transition {
        val action = Transition.ParseAction.EMBED
        val lookaheadGuard = closureRPlh.lookahead
        val toRP = RulePosition(closureRPlh.runtimeRule, closureRPlh.choice, RulePosition.END_OF_RULE)
        val to = from.stateSet.fetchOrCreateParseState(toRP)
        return Transition(from, to, action, lookaheadGuard, null) { _, _ -> true }
    }

    fun canGrowInto(pr: ParentRelation, prevRp: ParserState?): Boolean {
        return when {
            null == prevRp -> true
            prevRp.isAtEnd -> false
            pr.rulePosition == prevRp.rulePosition -> true
            else -> {
                val prevClosure = prevRp.createClosure(pr.lookahead) //this.createClosure(prevRp, null)
                prevClosure.content.any {
                    it.runtimeRule == pr.rulePosition.runtimeRule //&& it.lookahead == pr.lookahead
                }
            }
        }

    }

    internal fun calcTransitions(from: ParserState, previous: ParserState?): Set<Transition> { //TODO: add previous in order to filter parent relations
        val heightTransitions = mutableSetOf<Transition>()
        val graftTransitions = mutableSetOf<Transition>()
        val widthTransitions = mutableSetOf<Transition>()
        val goalTransitions = mutableSetOf<Transition>()
        val embeddedTransitions = mutableSetOf<Transition>()

        val goal = from.runtimeRule.kind == RuntimeRuleKind.GOAL && from.isAtEnd //from.rulePosition.position==1//from.isAtEnd
        if (goal) {
            val action = Transition.ParseAction.GOAL
            val to = from
            goalTransitions += Transition(from, to, action, emptySet(), null) { _, _ -> true }
        } else {
            if (from.isAtEnd) {
                if (from.parentRelations.isEmpty()) {
                    //end of skip
                    val action = Transition.ParseAction.GOAL
                    val to = from
                    goalTransitions += Transition(from, to, action, emptySet(), null) { _, _ -> true }
                } else {
                    val filteredRelations = when {
                        null != previous -> from.parentRelations.filter { pr -> this.canGrowInto(pr, previous) } //nonskip
                        else -> from.parentRelations //skip
                    }
                    for (parentRelation in filteredRelations) {
                        if (parentRelation.rulePosition.runtimeRule.kind == RuntimeRuleKind.GOAL) {
                            when {
                                from.runtimeRule == END_OF_TEXT -> {
                                    graftTransitions += this.createGraftTransition(from, parentRelation)
                                }
                                (parentRelation.lookahead.isEmpty()) -> {
                                    // must be end of skip. TODO: can do something better than this!
                                    val action = Transition.ParseAction.GOAL
                                    val to = from
                                    goalTransitions += Transition(from, to, action, emptySet(), null) { _, _ -> true }
                                }
                                else -> {
                                    graftTransitions += this.createGraftTransition(from, parentRelation)
                                }
                            }
                        } else {
                            val height = parentRelation.rulePosition.isAtStart
                            val graft = parentRelation.rulePosition.isAtStart.not()
                            if (height) {
                                heightTransitions += this.createHeightTransition(from, parentRelation)
                            }
                            if (graft) {
                                graftTransitions += this.createGraftTransition(from, parentRelation)
                            }
                        }
                    }
                }
            } else {
                //from.isAtEnd.not()
//                val parentRelations = from.stateSet.fetch(from.rulePosition.atEnd()).parentRelations
                val lhs = emptySet<RuntimeRule>() //TODO: what should the lhs be here ? maybe not matter
                val parentRelations = from.stateSet.fetchOrCreateParseState(from.rulePosition.atEnd()).parentRelations
                if (parentRelations.isEmpty()) {
                    //TODO: is this only when from is 'GOAL' ?
                    val afterGoalRp = RulePosition(from.runtimeRule, 0, 1)
                    val prLh = when (from.runtimeRule.kind) {
                        RuntimeRuleKind.GOAL -> from.rulePosition.next().flatMap { it.items }.toSet()
                        else -> emptySet<RuntimeRule>()
                    }
                    val closure = from.createClosure(prLh)
                    for (closureRPlh in closure.content) {
                        when (closureRPlh.runtimeRule.kind) {
                            RuntimeRuleKind.TERMINAL -> widthTransitions += this.createWidthTransition(from, closureRPlh)
                            RuntimeRuleKind.EMBEDDED -> embeddedTransitions += this.createEmbeddedTransition(from, closureRPlh)
                        }
                    }
                } else {
                    val filteredRelations = when {
                        null != previous -> from.parentRelations.filter { pr -> this.canGrowInto(pr, previous) } //nonskip
                        else -> from.parentRelations //skip
                    }
                    for (parentRelation in filteredRelations) {
                        val closure = from.createClosure(parentRelation.lookahead)
                        for (closureRPlh in closure.content) {
                            when (closureRPlh.runtimeRule.kind) {
                                RuntimeRuleKind.TERMINAL -> widthTransitions += this.createWidthTransition(from, closureRPlh)
                                RuntimeRuleKind.EMBEDDED -> embeddedTransitions += this.createEmbeddedTransition(from, closureRPlh)
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
                    val newTr = Transition(trh.from, trh.to, trh.action, trh.lookaheadGuard, trg.prevGuard, trh.runtimeGuard)
                    conflictHeightTransitionPairs.add(Pair(trh, trg))
                }
            }
        }

        val conflictHeightTransitions = conflictHeightTransitionPairs.map {
            val trh = it.first
            val trg = it.second
            Transition(trh.from, trh.to, trh.action, trh.lookaheadGuard, trg.prevGuard, trh.runtimeGuard)
        }
        //Note: need to do the conflict stuff to make multi work, at present!

        //val newHeightTransitions = (heightTransitions - conflictHeightTransitionPairs.map { it.first }) + conflictHeightTransitions

        val newHeightTransitions = heightTransitions
        val mergedWidthTransitions = widthTransitions.groupBy { it.to }.map {
            Transition(from, it.key, Transition.ParseAction.WIDTH, it.value.flatMap { it.lookaheadGuard }.toSet(), null) { _, _ -> true }
        }

        val mergedHeightTransitions = newHeightTransitions.groupBy { Pair(it.to, it.prevGuard) }.map {
            Transition(from, it.key.first, Transition.ParseAction.HEIGHT, it.value.flatMap { it.lookaheadGuard }.toSet(), it.key.second) { _, _ -> true }
        }

        val mergedGraftTransitions = graftTransitions.groupBy { Pair(it.to, it.prevGuard) }.map {
            Transition(from, it.key.first, Transition.ParseAction.GRAFT, it.value.flatMap { it.lookaheadGuard }.toSet(), it.key.second, it.value[0].runtimeGuard)
        }

        val transitions = mergedHeightTransitions + mergedGraftTransitions + mergedWidthTransitions + goalTransitions + embeddedTransitions
        return transitions.toSet()
    }

    fun buildCaches() {

    }

    fun startingState(userGoalRule: RuntimeRule, possibleEndOfText: List<RuntimeRule> = listOf(RuntimeRuleSet.END_OF_TEXT)): ParserState {
        var stateSet = this.states_cache[userGoalRule]
        if (null == stateSet) {
            stateSet = ParserStateSet(this, userGoalRule, possibleEndOfText)
            this.states_cache[userGoalRule] = stateSet
        }
        return stateSet.startState
    }

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
                ?: this.terminalRuleNumber[ruleName]
                ?: throw ParserException("NonTerminal RuntimeRule '${ruleName}' not found")
        return this.runtimeRules[number]
    }

    fun findTerminalRule(pattern: String): RuntimeRule {
        val number = this.terminalRuleNumber[pattern]
                ?: throw ParserException("Terminal RuntimeRule ${pattern} not found")
        return this.runtimeRules[number]
    }

    // used when calculating lookahead ?
    private fun calcExpectedItemRulePositionTransitive(rp: RulePosition): Set<RulePosition> {
        var s = setOf(rp)//rp.runtimeRule.calcExpectedRulePositions(rp.position)

        return s.transitiveClosure { rp ->
            if (RulePosition.END_OF_RULE == rp.position) {
                emptySet()
            } else {
                when (rp.runtimeRule.kind) {
                    RuntimeRuleKind.TERMINAL -> emptySet<RulePosition>()
                    RuntimeRuleKind.GOAL,
                    RuntimeRuleKind.NON_TERMINAL -> rp.runtimeRule.items(rp.choice, rp.position).flatMap {
                        when (it.kind) {
                            RuntimeRuleKind.GOAL -> TODO()
                            RuntimeRuleKind.TERMINAL -> setOf(rp)
                            RuntimeRuleKind.NON_TERMINAL -> it.calcExpectedRulePositions(0)
                            RuntimeRuleKind.EMBEDDED -> {
                                val embeddedStartRp = RulePosition(it.embeddedStartRule!!, 0, RulePosition.START_OF_RULE)
                                it.embeddedRuntimeRuleSet!!.expectedTerminalRulePositions[embeddedStartRp]!!.toSet()
                            }
                        }
                    }
                    RuntimeRuleKind.EMBEDDED -> TODO()
                }
            }.toSet()
        }
    }

    private fun calcExpectedTerminalRulePositions(rp: RulePosition): Set<RulePosition> {
        val nextItems = this.calcExpectedItemRulePositionTransitive(rp)
        return nextItems.filter {
            when (it.runtimeRule.kind) {
                RuntimeRuleKind.TERMINAL -> false
                else -> {
                    if (RulePosition.END_OF_RULE == it.position) {
                        false
                    } else {
                        it.items.any { it.kind == RuntimeRuleKind.TERMINAL }
                    }
                }
            }
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

    internal fun printAutomaton(goalRuleName: String): String {
        val b = StringBuilder()
        val gr = this.findRuntimeRule(goalRuleName)

        val s0 = this.startingState(gr, listOf(RuntimeRuleSet.END_OF_TEXT))

        val states = this.states_cache[gr]!!.states.values
        val transitions = states.flatMap { it.transitions_cache.values.flatMap { it ?: emptySet() }.toSet() }.toSet()


        states.forEach {
            b.append(it).append("\n")
        }
        transitions.forEach {
            b.append(it).append("\n")
        }


        return b.toString()
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