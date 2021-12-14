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

package net.akehurst.language.agl.automaton

import net.akehurst.language.agl.runtime.graph.GrowingNodeIndex
import net.akehurst.language.agl.runtime.structure.*
import net.akehurst.language.api.processor.AutomatonKind

internal class ParserState(
    val number: StateNumber,
    //val rulePosition: RulePosition,
    val rulePositions: List<RulePosition>, //must be a list so that we can index against Growing children
    val stateSet: ParserStateSet
) {

    companion object {
        val multiRuntimeGuard: Transition.(GrowingNodeIndex) -> Boolean = { gn: GrowingNodeIndex ->
            val previousRp = gn.state.rulePositions.first() //FIXME: first rule may not be correct
            val runtimeRule = gn.state.firstRule //FIXME:
            when {
                previousRp.isAtEnd -> gn.numNonSkipChildren + 1 >= runtimeRule.rhs.multiMin
                previousRp.position == RulePosition.POSITION_MULIT_ITEM -> {
                    //if the to state creates a complete node then min must be >= multiMin
                    if (this.to.rulePositions.first().isAtEnd) {
                        val minSatisfied = gn.numNonSkipChildren + 1 >= runtimeRule.rhs.multiMin
                        val maxSatisfied = -1 == runtimeRule.rhs.multiMax || gn.numNonSkipChildren + 1 <= runtimeRule.rhs.multiMax
                        minSatisfied && maxSatisfied
                    } else {
                        -1 == runtimeRule.rhs.multiMax || gn.numNonSkipChildren + 1 <= runtimeRule.rhs.multiMax
                    }

                }
                else -> true
            }
        }
        val sListRuntimeGuard: Transition.(GrowingNodeIndex) -> Boolean = { gn: GrowingNodeIndex ->
            val previousRp = gn.state.rulePositions.first() //FIXME: first rule may not be correct
            val runtimeRule = gn.state.firstRule //FIXME:
            when {
                previousRp.isAtEnd -> (gn.numNonSkipChildren / 2) + 1 >= runtimeRule.rhs.multiMin
                previousRp.position == RulePosition.POSITION_SLIST_ITEM -> {
                    //if the to state creates a complete node then min must be >= multiMin
                    if (this.to.rulePositions.first().isAtEnd) {
                        val minSatisfied = (gn.numNonSkipChildren / 2) + 1 >= runtimeRule.rhs.multiMin
                        val maxSatisfied = -1 == runtimeRule.rhs.multiMax || (gn.numNonSkipChildren / 2) + 1 <= runtimeRule.rhs.multiMax
                        minSatisfied && maxSatisfied
                    } else {
                        -1 == runtimeRule.rhs.multiMax || (gn.numNonSkipChildren / 2) + 1 <= runtimeRule.rhs.multiMax
                    }
                }
                previousRp.position == RulePosition.POSITION_SLIST_SEPARATOR -> {
                    //if the to state creates a complete node then min must be >= multiMin
                    if (this.to.rulePositions.first().isAtEnd) {
                        val minSatisfied = (gn.numNonSkipChildren / 2) + 1 >= runtimeRule.rhs.multiMin
                        val maxSatisfied = -1 == runtimeRule.rhs.multiMax || (gn.numNonSkipChildren / 2) + 1 < runtimeRule.rhs.multiMax
                        minSatisfied && maxSatisfied
                    } else {
                        -1 == runtimeRule.rhs.multiMax || (gn.numNonSkipChildren / 2) + 1 < runtimeRule.rhs.multiMax
                    }
                }
                else -> true
            }
        }
    }

    val outTransitions: TransitionCache = when (this.stateSet.automatonKind) {
        AutomatonKind.LOOKAHEAD_NONE -> TransitionCacheLC1()//TransitionCacheLC0()
        AutomatonKind.LOOKAHEAD_SIMPLE -> TODO()
        AutomatonKind.LOOKAHEAD_1 -> TransitionCacheLC1()
    }

    val rulePositionIdentity = rulePositions.map { it.identity }.toSet()

    //TODO: fast at runtime if not lazy
    val runtimeRules: List<RuntimeRule> by lazy { this.rulePositions.map { it.runtimeRule }.toList() }
    val runtimeRulesSet: Set<RuntimeRule> by lazy { this.rulePositions.map { it.runtimeRule }.toSet() }
    val optionList: List<Int> by lazy { this.rulePositions.map { it.option } }
    val positionList: List<Int> by lazy { this.rulePositions.map { it.position }.toList() }
    val priorityList: List<Int> by lazy { this.rulePositions.map { it.priority }.toList() }
    val choiceKindList: List<RuntimeRuleChoiceKind> by lazy {
        this.rulePositions.mapNotNull {
            when {
                it.runtimeRule.kind != RuntimeRuleKind.NON_TERMINAL -> null
                it.runtimeRule.rhs.itemsKind != RuntimeRuleRhsItemsKind.CHOICE -> null
                else -> it.runtimeRule.rhs.choiceKind
            }
        }.toSet().toList()
    }
    val isChoice: Boolean by lazy {
        this.choiceKindList.isNotEmpty()
    } // it should be empty if not a choice

    val firstRuleChoiceKind by lazy {
        check(1 == this.choiceKindList.size)
        this.choiceKindList[0]
    }

    val firstRule get() = runtimeRules.first()

    val isLeaf: Boolean get() = this.firstRule.kind == RuntimeRuleKind.TERMINAL //should only be one RP if it is a leaf

    val isAtEnd: Boolean = this.rulePositions.any { it.isAtEnd } //all in state should be either atEnd or notAtEnd
    val isNotAtEnd: Boolean = this.rulePositions.any { it.isAtEnd.not() } //all in state should be either atEnd or notAtEnd

    val isGoal = this.firstRule.kind == RuntimeRuleKind.GOAL
    val isUserGoal = this.firstRule == this.stateSet.userGoalRule

    fun firstOf(ifReachedEnd: LookaheadSet): Set<RuntimeRule> = this.rulePositions.flatMap {
        stateSet.buildCache.firstOf(it, ifReachedEnd)
    }.toSet()


    internal fun createLookaheadSet(content: Set<RuntimeRule>): LookaheadSet {
        return this.stateSet.createLookaheadSet(content)
    }

    fun widthInto(prevState: ParserState?): Set<WidthInfo> = this.stateSet.buildCache.widthInto(this.rulePositions)

    //for graft, previous must match prevGuard, for height must not match
    // (allow this to take 'null' so can use it for LC0
    fun heightOrGraftInto(prevState: ParserState): Set<HeightGraftInfo> {
        val rulePositionsAtEnd = this.rulePositions.filter { it.isAtEnd }
        val runtimeRulesToReduce = rulePositionsAtEnd.map { it.runtimeRule }.toSet()
        return when {
            runtimeRulesToReduce.isEmpty() -> emptySet()
            else -> this.stateSet.buildCache.heightGraftInto(prevState.rulePositions, runtimeRulesToReduce.toList())
        }
    }

    fun transitions(previousState: ParserState?): List<Transition> {
        val cache: List<Transition>? = this.outTransitions.findTransitionByPrevious(previousState)
        val trans = if (null == cache) {
            check(this.stateSet.preBuilt.not(), { "Transitions not built for $this --> $previousState" })
            val filteredTransitions = this.calcFilteredTransitions(previousState).toList()
            //val transitions = this.calcTransitions(previousState).toList()

            // transitions.forEach {
            //     it.lookaheadGuard.content.forEach {
            //         check(it.isEmptyRule.not(),{"Empty rule found in lookahead"})
            //     }
            // }
            val storedTrans = filteredTransitions.map { this.outTransitions.addTransition(listOf(previousState), it) }
            storedTrans
        } else {
            cache
        }
        // val filtered = this.growsInto(previous)
        return trans
    }

    private val __filteredTransitions = mutableSetOf<Transition>()
    private val __mergedTransitions = mutableSetOf<Transition>()
    internal fun calcFilteredTransitions(previousState: ParserState?): Set<Transition> {
        return if (null == previousState) {
            val transitions = this.calcTransitions(null)
            transitions
        } else {
            __filteredTransitions.clear()
            val transitions = this.calcTransitions(previousState)//, gn.lookaheadStack.peek())
            for (tr in transitions) {
                val filter = when (tr.action) {
                    Transition.ParseAction.GOAL -> true
                    Transition.ParseAction.WIDTH -> true
                    Transition.ParseAction.EMBED -> true
                    Transition.ParseAction.HEIGHT -> true
                    Transition.ParseAction.GRAFT -> {
                        tr.prevGuard?.let {
                            previousState.rulePositions.containsAll(it)
                        } ?: true
//                        previousState.rulePositions == t.prevGuard
                    }
                    //                   Transition.ParseAction.GRAFT_OR_HEIGHT -> true //should never happen at this point
                }
                if (filter) __filteredTransitions.add(tr)
            }
            //val grouped = __filteredTransitions.groupBy { it.to.runtimeRules }
            //grouped.forEach {
            //    when {
            //        1==it.value.size-> __mergedTransitions.add(it.value[0])
            //        else -> {
            //            TODO()
            //        }
            //    }
            //}
            //__mergedTransitions
            __filteredTransitions
        }
    }

    private val __heightTransitions = mutableSetOf<Transition>()
    private val __graftTransitions = mutableSetOf<Transition>()
    private val __widthTransitions = mutableSetOf<Transition>()
    private val __goalTransitions = mutableSetOf<Transition>()
    private val __embeddedTransitions = mutableSetOf<Transition>()
    private val __transitions = mutableSetOf<Transition>()

    // must use previousState.rulePosition as starting point for finding
    // lookahead for height/graft, and previousLookahead to use if end up at End of rule
    // due to position or empty rules.
    internal fun calcTransitions(previousState: ParserState?): Set<Transition> {//TODO: add previous in order to filter parent relations
        __heightTransitions.clear()
        __graftTransitions.clear()
        __widthTransitions.clear()
        __goalTransitions.clear()
        __embeddedTransitions.clear()
        __transitions.clear()

        val thisIsGoalState = this.isGoal && null == previousState
        when {
            thisIsGoalState -> when {
                this.rulePositions.first().isAtEnd -> { //should be only one RP !
                    val to = this
                    __goalTransitions.add(Transition(this, to, Transition.ParseAction.GOAL, LookaheadSet.EMPTY, LookaheadSet.EMPTY, null) { _, _ -> true })
                }
                else -> {
                    val widthInto = this.widthInto(previousState)
                    for (p in widthInto) {
                        val rp = p.to
                        val lhs = p.lookaheadSet
                        when (rp.runtimeRule.kind) {
                            RuntimeRuleKind.TERMINAL -> {
                                __widthTransitions.add(this.createWidthTransition(rp, lhs))
                            }
                            RuntimeRuleKind.EMBEDDED -> {
                                __embeddedTransitions.add(this.createEmbeddedTransition(rp, lhs))
                            }
                        }
                    }
                }
            }
            null != previousState -> {
                val heightOrGraftInto = this.heightOrGraftInto(previousState)
                for (hg in heightOrGraftInto) {
                    val kind = hg.parent.first().runtimeRule.kind
                    if (kind == RuntimeRuleKind.GOAL) {
                        when {
                            //this.runtimeRule == this.stateSet.runtimeRuleSet.END_OF_TEXT -> {
                            //this.stateSet.possibleEndOfText.contains(this.runtimeRule) -> {
                            //    rp.next().forEach { nrp ->
                            //         val ts = this.createGraftTransition3(nrp, lhs, rp)
                            ////        __graftTransitions.addAll(ts)//, addLh, parentLh))
                            //    }
                            // }
                            (isGoal && this.stateSet.isSkip) -> {
                                // must be end of skip. TODO: can do something better than this!
                                val to = this
                                __goalTransitions.add(Transition(this, to, Transition.ParseAction.GOAL, LookaheadSet.EMPTY, LookaheadSet.EMPTY, null) { _, _ -> true })
                            }
                            else -> {
                                val ts = this.createGraftTransition3(hg)
                                __graftTransitions.add(ts)//, addLh, parentLh))
                            }
                        }
                    } else {
                        val isAtStart = hg.parent.first().isAtStart //FIXME: what about things not the first?
                        when (isAtStart) {
                            true -> {
                                val ts = this.createHeightTransition3(hg)
                                __heightTransitions.add(ts)
                            }
                            false -> {
                                val ts = this.createGraftTransition3(hg)
                                __graftTransitions.add(ts)
                            }
                        }
                    }
                }
                val widthInto = this.widthInto(previousState)
                for (p in widthInto) {
                    val rp = p.to
                    val lhs = p.lookaheadSet
                    when (rp.runtimeRule.kind) {
                        RuntimeRuleKind.TERMINAL -> {
                            val ts = this.createWidthTransition(rp, lhs)
                            __widthTransitions.add(ts)
                        }
                        RuntimeRuleKind.EMBEDDED -> {
                            val ts = this.createEmbeddedTransition(rp, lhs)
                            __embeddedTransitions.add(ts)
                        }
                    }
                }
            }
            else -> error("Internal Error: previousState should not be null if this is not goalState")
        }
        __transitions.addAll(__widthTransitions)
        __transitions.addAll(__heightTransitions)
        __transitions.addAll(__graftTransitions)

        __transitions.addAll(__goalTransitions)
        __transitions.addAll(__embeddedTransitions)
        return __transitions.toSet()
    }

    internal fun calcTransitions1(previousState: ParserState?): Set<Transition> {//TODO: add previous in order to filter parent relations
        __heightTransitions.clear()
        __graftTransitions.clear()
        __widthTransitions.clear()
        __goalTransitions.clear()
        __embeddedTransitions.clear()
        __transitions.clear()

        val thisIsGoalState = this.isGoal && null == previousState
        val isAtEnd = this.rulePositions.first().isAtEnd
        when {
            thisIsGoalState -> when {
                isAtEnd -> {
                    val to = this
                    __goalTransitions.add(Transition(this, to, Transition.ParseAction.GOAL, LookaheadSet.EMPTY, LookaheadSet.EMPTY, null) { _, _ -> true })
                }
                else -> {
                    val widthInto = this.widthInto(previousState)
                    for (p in widthInto) {
                        val rp = p.to
                        val lhs = p.lookaheadSet
                        when (rp.runtimeRule.kind) {
                            RuntimeRuleKind.TERMINAL -> {
                                __widthTransitions.add(this.createWidthTransition(rp, lhs))
                            }
                            RuntimeRuleKind.EMBEDDED -> {
                                __embeddedTransitions.add(this.createEmbeddedTransition(rp, lhs))
                            }
                        }
                    }
                }
            }
            null != previousState -> {
                if (isAtEnd) {
                    val heightOrGraftInto = this.heightOrGraftInto(previousState)
                    for (hg in heightOrGraftInto) {
                        val kind = hg.parent.first().runtimeRule.kind
                        if (kind == RuntimeRuleKind.GOAL) {
                            when {
                                //this.runtimeRule == this.stateSet.runtimeRuleSet.END_OF_TEXT -> {
                                //this.stateSet.possibleEndOfText.contains(this.runtimeRule) -> {
                                //    rp.next().forEach { nrp ->
                                //         val ts = this.createGraftTransition3(nrp, lhs, rp)
                                ////        __graftTransitions.addAll(ts)//, addLh, parentLh))
                                //    }
                                // }
                                (isGoal && this.stateSet.isSkip) -> {
                                    // must be end of skip. TODO: can do something better than this!
                                    val to = this
                                    __goalTransitions.add(Transition(this, to, Transition.ParseAction.GOAL, LookaheadSet.EMPTY, LookaheadSet.EMPTY, null) { _, _ -> true })
                                }
                                else -> {
                                    val ts = this.createGraftTransition3(hg)
                                    __graftTransitions.add(ts)//, addLh, parentLh))
                                }
                            }
                        } else {
                            val isAtStart = hg.parent.first().isAtStart //FIXME: what about things not the first?
                            when (isAtStart) {
                                true -> {
                                    val ts = this.createHeightTransition3(hg)
                                    __heightTransitions.add(ts)
                                }
                                false -> {
                                    val ts = this.createGraftTransition3(hg)
                                    __graftTransitions.add(ts)
                                }
                            }
                        }
                    }
                } else {
                    val widthInto = this.widthInto(previousState)
                    for (p in widthInto) {
                        val rp = p.to
                        val lhs = p.lookaheadSet
                        when (rp.runtimeRule.kind) {
                            RuntimeRuleKind.TERMINAL -> {
                                val ts = this.createWidthTransition(rp, lhs)
                                __widthTransitions.add(ts)
                            }
                            RuntimeRuleKind.EMBEDDED -> {
                                val ts = this.createEmbeddedTransition(rp, lhs)
                                __embeddedTransitions.add(ts)
                            }
                        }
                    }
                }
            }
            else -> error("Internal Error: previousState should not be null if this is not goalState")
        }

        //TODO: merge transitions with everything duplicate except lookahead (merge lookaheads)
        //not sure if this should be before or after the h/g conflict test.
/*
        val groupedWidthTransitions = __widthTransitions.groupBy { Pair(it.to, it.lookaheadGuard) }
        val mergedWidthTransitions = groupedWidthTransitions.map {
            val mLh = if (it.value.size > 1) {
                val mLhC = it.value.map { it.lookaheadGuard.content.toSet() }.reduce { acc, lhc -> acc.union(lhc) }
                this.createLookaheadSet(mLhC)
            } else {
                it.value[0].lookaheadGuard
            }
            val addLh = it.value[0].lookaheadGuard
            Transition(this, it.key.first, Transition.ParseAction.WIDTH, addLh, mLh, null) { _, _ -> true }
        }

        val groupedHeightTransitions = __heightTransitions.groupBy { Triple(it.to, it.prevGuard, it.additionalLookaheads) }
        val mergedHeightTransitions = groupedHeightTransitions.map {
            val mLh = if (it.value.size > 1) {
                val mLhC = it.value.map { it.lookaheadGuard.content.toSet() }.reduce { acc, lhc -> acc.union(lhc) }
                this.createLookaheadSet(mLhC)
            } else {
                it.value[0].lookaheadGuard
            }
            val addLh = it.value[0].additionalLookaheads
            Transition(this, it.key.first, Transition.ParseAction.HEIGHT, addLh, mLh, it.key.second) { _, _ -> true }
        }

        val groupedGraftTransitions = __graftTransitions.groupBy { Triple(it.to, it.prevGuard, it.additionalLookaheads) }
        val mergedGraftTransitions = groupedGraftTransitions.map {
            val mLh = if (it.value.size > 1) {
                val mLhC = it.value.map { it.lookaheadGuard.content.toSet() }.reduce { acc, lhc -> acc.union(lhc) }
                this.createLookaheadSet(mLhC)
            } else {
                it.value[0].lookaheadGuard
            }
            val addLh = it.value[0].additionalLookaheads
            Transition(this, it.key.first, Transition.ParseAction.GRAFT, addLh, mLh, it.key.second, it.value[0].runtimeGuard)
        }
*/
        __transitions.addAll(__widthTransitions)//mergedHeightTransitions)
        __transitions.addAll(__heightTransitions)//mergedGraftTransitions)
        __transitions.addAll(__graftTransitions)//mergedWidthTransitions)

        __transitions.addAll(__goalTransitions)
        __transitions.addAll(__embeddedTransitions)
        return __transitions.toSet()
    }

    private fun createWidthTransition(rp: RulePosition, lookaheadSet: LookaheadSet): Transition {
        val toRp = RulePosition(rp.runtimeRule, rp.option, RulePosition.END_OF_RULE) //TODO: is this not passed in ? //assumes rp is a terminal
        val to = this.stateSet.states[listOf(toRp)]
        val lh = lookaheadSet
        // upLookahead and prevGuard are unused
        return Transition(this, to, Transition.ParseAction.WIDTH, lh, LookaheadSet.EMPTY, null) { _, _ -> true }
    }

    private fun createEmbeddedTransition(rp: RulePosition, lookaheadSet: LookaheadSet): Transition {
        val toRp = RulePosition(rp.runtimeRule, rp.option, RulePosition.END_OF_RULE) //TODO: is this not passed in ?
        val to = this.stateSet.states[listOf(toRp)]
        val lh = lookaheadSet
        // upLookahead and prevGuard are unused
        return Transition(this, to, Transition.ParseAction.EMBED, lh, LookaheadSet.EMPTY, null) { _, _ -> true }
    }

    private fun createHeightTransition3(hg: HeightGraftInfo): Transition {
        val to = this.stateSet.states[hg.parentNext]
        val trs = Transition(this, to, Transition.ParseAction.HEIGHT, hg.lhs, hg.upLhs, hg.parent) { _, _ -> true }
        return trs
    }

    private fun createGraftTransition3(hg: HeightGraftInfo): Transition {
        val runtimeGuard: Transition.(GrowingNodeIndex, List<RulePosition>?) -> Boolean = { gn, previous ->
            if (null == previous) {
                true
            } else {
                val rr = previous.first().runtimeRule //FIXME: possibly more than one!!
                when (rr.rhs.itemsKind) {
                    RuntimeRuleRhsItemsKind.LIST -> when (rr.rhs.listKind) {
                        RuntimeRuleListKind.MULTI -> multiRuntimeGuard.invoke(this, gn)
                        RuntimeRuleListKind.SEPARATED_LIST -> sListRuntimeGuard.invoke(this, gn)
                        else -> TODO()
                    }
                    else -> true
                }
            }
        }
        val to = this.stateSet.states[hg.parentNext]
        val trs = Transition(this, to, Transition.ParseAction.GRAFT, hg.lhs, hg.upLhs, hg.parent, runtimeGuard)
        return trs
    }


    // --- Any ---

    override fun hashCode(): Int {
        return this.number.value + this.stateSet.number * 31
    }

    override fun equals(other: Any?): Boolean {
        return if (other is ParserState) {
            this.stateSet.number == other.stateSet.number && this.number.value == other.number.value
        } else {
            false
        }
    }

    override fun toString(): String {
        return "State(${this.number.value}/${this.stateSet.number}-${rulePositions})"
    }

}
