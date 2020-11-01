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

import net.akehurst.language.agl.runtime.graph.GrowingNode

data class HeightGraft(
        val parent: RulePosition,
        val parentNext: RulePosition,
        val lhs: LookaheadSet
)

class ParserState(
        val number: StateNumber,
        val rulePosition: RulePosition,
        val stateSet: ParserStateSet
) {

    companion object {
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
    }

    internal var transitions_cache: MutableMap<ParserState?, List<Transition>?> = mutableMapOf()

    val allBuiltTransitions: Set<Transition> get() = transitions_cache.values.filterNotNull().flatten().toSet()

    val runtimeRule: RuntimeRule
        inline get() {
            return this.rulePosition.runtimeRule
        }
    val choice: Int
        inline get() {
            return this.rulePosition.option
        }
    val position: Int
        inline get() {
            return this.rulePosition.position
        }

    val isAtEnd: Boolean
        inline get() {
            return this.rulePosition.isAtEnd
        }

    val parentRelations: Set<ParentRelation> by lazy {
        if (rulePosition.runtimeRule.kind == RuntimeRuleKind.GOAL) {
            emptySet()
        } else {
            this.stateSet.parentRelation(this.rulePosition.runtimeRule)
        }
    }

    internal fun createLookaheadSet(content: Set<RuntimeRule>): LookaheadSet {
        return this.stateSet.createLookaheadSet(content)
    }

    fun widthInto1(prevRp: RulePosition?): Set<Pair<RulePosition, LookaheadSet>> {
        val lhUp = when {
            null == prevRp -> LookaheadSet.UP
            else -> {
                val prevCls = this.stateSet.calcClosure(ClosureItem(null, prevRp, LookaheadSet.UP))
                val me = prevCls.filter { it.rulePosition.item == this.runtimeRule }
                this.createLookaheadSet(me.flatMap { it.lookaheadSet.content }.toSet())
            }
        }
        val cls = this.stateSet.calcClosure(ClosureItem(null, this.rulePosition, lhUp))
        val terminals = cls.filter {
            val rr = it.rulePosition.item
            when {
                null == rr -> false
                else -> when (rr.kind) {
                    RuntimeRuleKind.EMBEDDED, RuntimeRuleKind.TERMINAL -> true
                    else -> false
                }
            }
        }.toSet()
        val grouped = terminals.groupBy { it.rulePosition.item!! }.mapNotNull {
            val rr = it.key
            val lhsc = it.value.flatMap { it.lookaheadSet.content }.toSet()
            val lhs = this.createLookaheadSet(lhsc)
            Pair(RulePosition(rr, 0, RulePosition.END_OF_RULE), lhs)
        }.toSet()
        return grouped
    }

    fun widthInto(prevRp: RulePosition?): Set<Pair<RulePosition, LookaheadSet>> {
        val fst = when (this.runtimeRule.kind) {
            RuntimeRuleKind.GOAL -> this.stateSet.firstTerminals[this.stateSet.startState.rulePosition]
            RuntimeRuleKind.NON_TERMINAL -> this.stateSet.firstTerminals[this.rulePosition]
            RuntimeRuleKind.TERMINAL -> emptySet()
            RuntimeRuleKind.EMBEDDED -> emptySet()
        }
        val terms = fst.flatMap {
            val rp = RulePosition(it, 0, RulePosition.END_OF_RULE)
            val pr = this.stateSet.parentRelation(it)
            pr.map {
                Pair(rp, it.lookaheadSet)
            }
        }.toSet()
        return terms
    }

    //for graft, previous must match prevGuard, for height must not match
    fun heightOrGraftInto(topRp: RulePosition): Set<HeightGraft> {
        val pp = this.stateSet.parentPosition[this.runtimeRule]
        return pp.flatMap { pp ->
            val ppns = pp.next()
            ppns.map { ppn ->
                val fstOfPpn = this.stateSet.expectedAfter(ppn)
                val lhs = this.stateSet.createLookaheadSet(fstOfPpn)
                HeightGraft(pp, ppn, lhs)
            }
        }.toSet()
    }

    fun heightOrGraftInto2(prevRp: RulePosition?): Set<ParentRelation> {
        return when {
            null == prevRp -> this.parentRelations
            prevRp.isAtEnd -> emptySet()
            else -> this.parentRelations.filter { pr ->
                when {
                    pr.rulePosition == prevRp -> true
                    else -> {
                        val prevClosure = this.stateSet.calcClosure(ClosureItem(null, prevRp, pr.lookaheadSet))
                        prevClosure.any {
                            it.rulePosition == pr.rulePosition
                        }
                    }
                }
            }.toSet()
        }
    }


    fun growsInto(ancestor: ParserState): Boolean {
        return this.stateSet.growsInto(ancestor.rulePosition, this.rulePosition)
    }


    fun transitions(previousState: ParserState?): List<Transition> {
        val cache = this.transitions_cache[previousState]
        val trans = if (null == cache) {
            //check(this.stateSet.preBuilt.not(),{"Transitions not built for $this --> $previousState"})
            val transitions = this.calcFilteredTransitions(previousState).toList()
            //val transitions = this.calcTransitions(previousState).toList()

            // transitions.forEach {
            //     it.lookaheadGuard.content.forEach {
            //         check(it.isEmptyRule.not(),{"Empty rule found in lookahead"})
            //     }
            // }

            this.transitions_cache[previousState] = transitions
            transitions
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
                    Transition.ParseAction.WIDTH -> {
                        true
                    }
                    Transition.ParseAction.GRAFT -> {
                        previousState.rulePosition == t.prevGuard
                    }
                    Transition.ParseAction.HEIGHT -> {
                        //t.to.growsInto(previousState) &&
                                previousState.rulePosition != t.prevGuard
                    }
                    Transition.ParseAction.EMBED -> {
                        true
                    }
                    Transition.ParseAction.GOAL -> {
                        true
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

        val thisIsGoalState = this.runtimeRule.kind == RuntimeRuleKind.GOAL && null == previousState
        when {
            thisIsGoalState -> when {
                this.isAtEnd -> {
                    val action = Transition.ParseAction.GOAL
                    val to = this
                    __goalTransitions.add(Transition(this, to, action, LookaheadSet.EMPTY, null) { _, _ -> true })
                }
                else -> {
                    val widthInto = this.widthInto(previousState?.rulePosition)
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
                if (this.isAtEnd) {
                    val heightOrGraftInto = this.heightOrGraftInto(previousState.rulePosition)
                    for (hg in heightOrGraftInto) {
                        if (hg.parent.runtimeRule.kind == RuntimeRuleKind.GOAL) {
                            when {
                                //this.runtimeRule == this.stateSet.runtimeRuleSet.END_OF_TEXT -> {
                                //this.stateSet.possibleEndOfText.contains(this.runtimeRule) -> {
                                //    rp.next().forEach { nrp ->
                                //         val ts = this.createGraftTransition3(nrp, lhs, rp)
                                ////        __graftTransitions.addAll(ts)//, addLh, parentLh))
                                //    }
                                // }
                                (this.runtimeRule.kind === RuntimeRuleKind.GOAL && this.stateSet.isSkip) -> {
                                    // must be end of skip. TODO: can do something better than this!
                                    val action = Transition.ParseAction.GOAL
                                    val to = this
                                    __goalTransitions.add(Transition(this, to, action, LookaheadSet.EMPTY, null) { _, _ -> true })
                                }
                                else -> {
                                    val ts = this.createGraftTransition3(hg)
                                    __graftTransitions.addAll(ts)//, addLh, parentLh))
                                }
                            }
                        } else {
                            when (hg.parent.isAtStart) {
                                true -> {
                                    val ts = this.createHeightTransition3(hg)
                                    __heightTransitions.addAll(ts)
                                }
                                false -> {
                                    val ts = this.createGraftTransition3(hg)
                                    __graftTransitions.addAll(ts)
                                }
                            }
                        }
                    }
                } else {
                    val widthInto = this.widthInto(previousState.rulePosition)
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
        val toRp = RulePosition(rp.runtimeRule, rp.option, RulePosition.END_OF_RULE) //assumes rp is a terminal
        val to = this.stateSet.states[toRp]
        val lh = lookaheadSet
        return Transition(this, to, action, lh, null) { _, _ -> true }
    }

    private fun createHeightTransition3(hg: HeightGraft): Set<Transition> {
        val action = Transition.ParseAction.HEIGHT
        val to = this.stateSet.states[hg.parentNext]
        val trs = setOf(Transition(this, to, action, hg.lhs, hg.parent) { _, _ -> true })
        return trs
    }

    private fun createGraftTransition3(hg: HeightGraft): Set<Transition> {
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
        val action = Transition.ParseAction.GRAFT
        val to = this.stateSet.states[hg.parentNext]
        val trs = setOf(Transition(this, to, action, hg.lhs, hg.parent, runtimeGuard))
        return trs
    }

    private fun createEmbeddedTransition(rp: RulePosition, lookaheadSet: LookaheadSet): Transition {
        val action = Transition.ParseAction.EMBED
        val toRp = RulePosition(rp.runtimeRule, rp.option, RulePosition.END_OF_RULE)
        val to = this.stateSet.states[toRp]
        val lh = lookaheadSet
        return Transition(this, to, action, lh, null) { _, _ -> true }
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
        return "State(${this.number.value}/${this.stateSet.number}-${rulePosition})"
    }

}
