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

import net.akehurst.language.agl.runtime.graph.GrowingNode
import net.akehurst.language.agl.runtime.structure.*

data class HeightGraft(
        val prev: RulePosition?,
        val parent: List<RulePosition>,
        val parentNext: List<RulePosition>,
        val lhs: LookaheadSet,
        val upLhs: LookaheadSet
)

class ParserState(
        val number: StateNumber,
        //val rulePosition: RulePosition,
        val rulePositions: List<RulePosition>, //must be a list so that we can index against Growing children
        val stateSet: ParserStateSet
) {

    companion object {
        val multiRuntimeGuard: Transition.(GrowingNode) -> Boolean = { gn: GrowingNode ->
            val previousRp = gn.currentState.rulePositions.first() //FIXME:
            val runtimeRule = gn.runtimeRules.first()
            when {
                previousRp.isAtEnd -> gn.children.numberNonSkip + 1 >= runtimeRule.rhs.multiMin
                previousRp.position == RulePosition.MULIT_ITEM_POSITION -> {
                    //if the to state creates a complete node then min must be >= multiMin
                    if (this.to.rulePositions.first().isAtEnd) {
                        val minSatisfied = gn.children.numberNonSkip + 1 >= runtimeRule.rhs.multiMin
                        val maxSatisfied = -1 == runtimeRule.rhs.multiMax || gn.children.numberNonSkip + 1 <= runtimeRule.rhs.multiMax
                        minSatisfied && maxSatisfied
                    } else {
                        -1 == runtimeRule.rhs.multiMax || gn.children.numberNonSkip + 1 <= runtimeRule.rhs.multiMax
                    }

                }
                else -> true
            }
        }
        val sListRuntimeGuard: Transition.(GrowingNode) -> Boolean = { gn: GrowingNode ->
            val previousRp = gn.currentState.rulePositions.first() //FIXME:
            val runtimeRule = gn.runtimeRules.first()
            when {
                previousRp.isAtEnd -> (gn.children.numberNonSkip / 2) + 1 >= runtimeRule.rhs.multiMin
                previousRp.position == RulePosition.SLIST_ITEM_POSITION -> {
                    //if the to state creates a complete node then min must be >= multiMin
                    if (this.to.rulePositions.first().isAtEnd) {
                        val minSatisfied = (gn.children.numberNonSkip / 2) + 1 >= runtimeRule.rhs.multiMin
                        val maxSatisfied = -1 == runtimeRule.rhs.multiMax || (gn.children.numberNonSkip / 2) + 1 <= runtimeRule.rhs.multiMax
                        minSatisfied && maxSatisfied
                    } else {
                        -1 == runtimeRule.rhs.multiMax || (gn.children.numberNonSkip / 2) + 1 <= runtimeRule.rhs.multiMax
                    }
                }
                previousRp.position == RulePosition.SLIST_SEPARATOR_POSITION -> {
                    //if the to state creates a complete node then min must be >= multiMin
                    if (this.to.rulePositions.first().isAtEnd) {
                        val minSatisfied = (gn.children.numberNonSkip / 2) + 1 >= runtimeRule.rhs.multiMin
                        val maxSatisfied = -1 == runtimeRule.rhs.multiMax || (gn.children.numberNonSkip / 2) + 1 < runtimeRule.rhs.multiMax
                        minSatisfied && maxSatisfied
                    } else {
                        -1 == runtimeRule.rhs.multiMax || (gn.children.numberNonSkip / 2) + 1 < runtimeRule.rhs.multiMax
                    }
                }
                else -> true
            }
        }
    }

    // transitions stored here
    private val _transitionsByTo = mutableMapOf<ParserState, MutableSet<Transition>>()

    // transitions referenced here
    private val _transitionsByPrevious: MutableMap<ParserState?, MutableList<Transition>?> = mutableMapOf()

    val allBuiltTransitions: Set<Transition> get() = _transitionsByTo.values.flatten().toSet()
    val transitionsByPrevious: Map<ParserState?,List<Transition>?> get() = _transitionsByPrevious

    val runtimeRules: Set<RuntimeRule> = this.rulePositions.map { it.runtimeRule }.toSet()
    val terminalRule = runtimeRules.first()

    val isAtEnd: Boolean = this.rulePositions.first().isAtEnd //either all are atEnd or none are

    val isGoal = this.runtimeRules.first().kind==RuntimeRuleKind.GOAL

    fun firstOf(ifReachedEnd:Set<RuntimeRule>): Set<RuntimeRule> = this.rulePositions.flatMap {
        stateSet.firstOf(it, ifReachedEnd)
    }.toSet()

    // add the transition and return it, or return existing transition if it already exists
    internal fun addTransition(previousState: ParserState?, tr:Transition) : Transition{
        var set = _transitionsByTo[tr.to]
        val exist = if (null==set) {
            set = mutableSetOf(tr)
            _transitionsByTo[tr.to] = set
            tr
        } else {
            val exist = set.firstOrNull { it == tr }
            if (null==exist) {
                set.add(tr)
                tr
            } else {
                exist
            }
        }
        var list = this._transitionsByPrevious[previousState]
        if (null==list) {
            list = mutableListOf(exist)
            this._transitionsByPrevious[previousState] = list
        } else {
            list.add(exist)
        }
        return exist
    }

    internal fun createLookaheadSet(content: Set<RuntimeRule>): LookaheadSet {
        return this.stateSet.createLookaheadSet(content)
    }

    fun widthInto(prevRps: List<RulePosition>?): Set<Pair<RulePosition, LookaheadSet>> {
        // get lh by closure on prev
        val upLhs = when (prevRps) {
            null -> LookaheadSet.UP
            else -> {
                val upCls = prevRps.flatMap { this.stateSet.calcClosure(it, LookaheadSet.UP) }.toSet()
                val upFilt = upCls.filter { this.runtimeRules.contains(it.rulePosition.item) }
                val lhsc = upFilt.flatMap { it.lookaheadSet.content }.toSet() //TODO: should we combine these or keep sepraate?
                this.createLookaheadSet(lhsc)
            }
        }
        val cls = this.rulePositions.flatMap { this.stateSet.calcClosure(it, upLhs) }
        val filt = cls.filter { it.rulePosition.item!!.kind == RuntimeRuleKind.TERMINAL || it.rulePosition.item!!.kind == RuntimeRuleKind.EMBEDDED }
        val grouped = filt.groupBy { it.rulePosition.item!! }.map {
            val rr = it.key
            val rp = RulePosition(rr, 0, RulePosition.END_OF_RULE)
            val lhsc = it.value.flatMap { it.lookaheadSet.content }.toSet()
            val lhs = this.createLookaheadSet(lhsc)
            Pair(rp, lhs)
        }.toSet()
        //don't group them, because we need the info on the lookahead for the runtime calc of next lookaheads
        //val grouped = filt.map {
        //    val rr = it.rulePosition.item!!
        //    val rp = RulePosition(rr, 0, RulePosition.END_OF_RULE)
        //val lhsc = it.value.flatMap { it.lookaheadSet.content }.toSet()
        //val lhs = this.createLookaheadSet(lhsc)
        //    Pair(rp, it.lookaheadSet)
        //}.toSet()
        return grouped
    }

    //for graft, previous must match prevGuard, for height must not match
    fun heightOrGraftInto(topRps: List<RulePosition>): Set<HeightGraft> {
        // have to ensure somehow that this grows into prev
        //have to do closure down from prev,
        //or we have to check this grows into prev after

        val cls = topRps.flatMap {  this.stateSet.calcClosure(it, LookaheadSet.UP) }.toSet()
        val filt = cls.filter {this.runtimeRules.contains(it.rulePosition.item)  }
        val res = filt.flatMap { clsItem ->
            val prev = null//clsItem.parentItem?.parentNext//clsItem.prev.firstOrNull() //TODO:support full set
            val parent = clsItem.rulePosition
            //val lhs = clsItem.lookaheadSet
            val upLhs = clsItem.parentItem?.lookaheadSet ?: LookaheadSet.UP
            val pns = parent.next()
            pns.map { parentNext ->
                val lhsc = this.stateSet.firstOf(parentNext, upLhs.content)// this.stateSet.expectedAfter(parentNext)
                val lhs = this.createLookaheadSet(lhsc)
                HeightGraft(prev, listOf(parent), listOf(parentNext), lhs, upLhs)
            }
        }
        val grouped = res.groupBy { listOf(it.prev, it.parent, it.parentNext) }//, it.lhs) }
                .map {
                    val prev = it.key[0] as RulePosition?
                    val parent = it.key[1] as List<RulePosition>
                    val parentNext = it.key[2] as List<RulePosition>
                    val lhs = createLookaheadSet(it.value.flatMap { it.lhs.content }.toSet())
                    val upLhs = createLookaheadSet(it.value.flatMap { it.upLhs.content }.toSet())
                    HeightGraft(prev, (parent), (parentNext), lhs, upLhs)
                }
        val grouped2 = grouped.groupBy { listOf(it.parent.first().isAtEnd, it.lhs, it.upLhs) }
                .map {
                    val prev = null
                    val parent = it.value.flatMap { it.parent }.toSet().toList()
                    val parentNext = it.value.flatMap { it.parentNext }.toSet().toList()
                    val lhs = it.key[1] as LookaheadSet
                    val upLhs = it.key[2] as LookaheadSet
                    HeightGraft(prev, parent, parentNext, lhs, upLhs)
                }
        return grouped2.toSet()
    }

    fun transitions(previousState: ParserState?): List<Transition> {
        val cache = this._transitionsByPrevious[previousState]
        val trans = if (null == cache) {
           // check(this.stateSet.preBuilt.not(),{"Transitions not built for $this --> $previousState"})
            val filteredTransitions = this.calcFilteredTransitions(previousState).toList()
            //val transitions = this.calcTransitions(previousState).toList()

            // transitions.forEach {
            //     it.lookaheadGuard.content.forEach {
            //         check(it.isEmptyRule.not(),{"Empty rule found in lookahead"})
            //     }
            // }
            val storedTrans = filteredTransitions.map { addTransition(previousState, it) }
            storedTrans
        } else {
            cache
        }
        // val filtered = this.growsInto(previous)
        return trans
    }

    private val __filteredTransitions = mutableSetOf<Transition>()
    internal fun calcFilteredTransitions(previousState: ParserState?): Set<Transition> {
        return if (null == previousState) {
            val transitions = this.calcTransitions(null)
            transitions
        } else {
            __filteredTransitions.clear()
            val transitions = this.calcTransitions(previousState)//, gn.lookaheadStack.peek())
            for (t in transitions) {
                val filter = when (t.action) {
                    Transition.ParseAction.GOAL ->  true
                    Transition.ParseAction.WIDTH -> true
                    Transition.ParseAction.EMBED -> true
                    Transition.ParseAction.GRAFT -> {
                        t.prevGuard?.let {
                            previousState.rulePositions.containsAll(it)
                        } ?: true
//                        previousState.rulePositions == t.prevGuard
                    }
                    Transition.ParseAction.HEIGHT -> {
                        //t.to.growsInto(previousState) &&
                        previousState.rulePositions != t.prevGuard
                    }
                }
                if (filter) __filteredTransitions.add(t)
            }
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
        val isAtEnd = this.rulePositions.first().isAtEnd
        when {
            thisIsGoalState -> when {
                isAtEnd -> {
                    val action = Transition.ParseAction.GOAL
                    val to = this
                    __goalTransitions.add(Transition(this, to, action, LookaheadSet.EMPTY, LookaheadSet.EMPTY, null) { _, _ -> true })
                }
                else -> {
                    val widthInto = this.widthInto(previousState?.rulePositions)
                    for (p in widthInto) {
                        val rp = p.first//rulePosition
                        val lhs = p.second//lookaheadSet
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
                    val heightOrGraftInto = this.heightOrGraftInto(previousState.rulePositions)
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
                                    val action = Transition.ParseAction.GOAL
                                    val to = this
                                    __goalTransitions.add(Transition(this, to, action, LookaheadSet.EMPTY, LookaheadSet.EMPTY, null) { _, _ -> true })
                                }
                                else -> {
                                    val ts = this.createGraftTransition3(hg)
                                    __graftTransitions.add(ts)//, addLh, parentLh))
                                }
                            }
                        } else {
                            val isAtStart = hg.parent.first().isAtStart
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
                    val widthInto = this.widthInto(previousState.rulePositions)
                    for (p in widthInto) {
                        val rp = p.first//rulePosition
                        val lhs = p.second//lookaheadSet
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
        val action = Transition.ParseAction.WIDTH
        val toRp = RulePosition(rp.runtimeRule, rp.option, RulePosition.END_OF_RULE) //TODO: is this not passed in ? //assumes rp is a terminal
        val to = this.stateSet.states[listOf(toRp)]
        val lh = lookaheadSet
        // upLookahead and prevGuard are unused
        return Transition(this, to, action, lh, LookaheadSet.EMPTY, null) { _, _ -> true }
    }

    private fun createEmbeddedTransition(rp: RulePosition, lookaheadSet: LookaheadSet): Transition {
        val action = Transition.ParseAction.EMBED
        val toRp = RulePosition(rp.runtimeRule, rp.option, RulePosition.END_OF_RULE) //TODO: is this not passed in ?
        val to = this.stateSet.states[listOf(toRp)]
        val lh = lookaheadSet
        // upLookahead and prevGuard are unused
        return Transition(this, to, action, lh, LookaheadSet.EMPTY, null) { _, _ -> true }
    }

    private fun createHeightTransition3(hg: HeightGraft): Transition {
        val action = Transition.ParseAction.HEIGHT
        val to = this.stateSet.states[hg.parentNext]
        val trs = Transition(this, to, action, hg.lhs, hg.upLhs, hg.parent) { _, _ -> true }
        return trs
    }

    private fun createGraftTransition3(hg: HeightGraft): Transition {
        val runtimeGuard: Transition.(GrowingNode, List<RulePosition>?) -> Boolean = { gn, previous ->
            if (null == previous) {
                true
            } else {
                when (previous.first().runtimeRule.rhs.kind) { //FIXME
                    RuntimeRuleItemKind.MULTI -> multiRuntimeGuard.invoke(this, gn)
                    RuntimeRuleItemKind.SEPARATED_LIST -> sListRuntimeGuard.invoke(this, gn)
                    else -> true
                }
            }
        }
        val action = Transition.ParseAction.GRAFT
        val to = this.stateSet.states[hg.parentNext]
        val trs = Transition(this, to, action, hg.lhs, hg.upLhs, hg.parent, runtimeGuard)
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
