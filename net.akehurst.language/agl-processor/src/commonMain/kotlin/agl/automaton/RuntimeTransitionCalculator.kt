/**
 * Copyright (C) 2022 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

import net.akehurst.language.agl.automaton.ParserState.Companion.lhs
import net.akehurst.language.agl.runtime.graph.RuntimeState
import net.akehurst.language.agl.runtime.structure.RulePosition
import net.akehurst.language.agl.runtime.structure.RuntimeRuleKind

internal class RuntimeTransitionCalculator(
    val stateSet: ParserStateSet
) {

    private val __filteredTransitions = mutableSetOf<Transition>() // to save time allocating when calcFilteredTransitions is called
    internal fun calcFilteredTransitions(prevPrev: RuntimeState, previousState: RuntimeState, sourceState: RuntimeState): Set<Transition> {
        __filteredTransitions.clear()
        val transitions = this.calcTransitions(prevPrev, previousState,sourceState)//, gn.lookaheadStack.peek())
        for (tr in transitions) {
            val filter = when (tr.action) {
                Transition.ParseAction.GOAL -> true
                Transition.ParseAction.WIDTH -> true
                Transition.ParseAction.EMBED -> true
                Transition.ParseAction.HEIGHT -> true
                Transition.ParseAction.GRAFT -> {
                    tr.graftPrevGuard?.let {
                        previousState.state.rulePositions.containsAll(it)
                    } ?: true
                }
            }
            if (filter) __filteredTransitions.add(tr)
        }
        return __filteredTransitions
    }

    private val __transitions = mutableSetOf<Transition>() // to save time allocating when calcTransitions is called
    // must use previousState.rulePosition as starting point for finding
    // lookahead for height/graft, and previousLookahead to use if end up at End of rule
    // due to position or empty rules.
    internal fun calcTransitions(prevPrev: RuntimeState, previousState: RuntimeState, sourceState: RuntimeState): Set<Transition> {//TODO: add previous in order to filter parent relations
        __transitions.clear()
        when {
            sourceState.state.isGoal -> {
                val widthInto = this.stateSet.buildCache.widthInto(previousState,sourceState)
                for (wi in widthInto) {
                    when (wi.to.runtimeRule.kind) {
                        RuntimeRuleKind.TERMINAL -> __transitions.add(this.createWidthTransition(sourceState.state,wi))
                        RuntimeRuleKind.EMBEDDED -> __transitions.add(this.createEmbeddedTransition(sourceState.state,wi))
                        RuntimeRuleKind.GOAL, RuntimeRuleKind.NON_TERMINAL -> error("Should never happen")
                    }
                }
            }
            sourceState.isAtEnd -> {
                val heightOrGraftInto = this.stateSet.buildCache.heightOrGraftInto(prevPrev, previousState,sourceState)
                for (hg in heightOrGraftInto) {
                    val kind = hg.parent.first().runtimeRule.kind
                    if (kind == RuntimeRuleKind.GOAL) {
                        when {
                            (sourceState.state.isGoal && this.stateSet.isSkip) -> {
                                // must be end of skip. TODO: can do something better than this!
                                val to = sourceState.state
                                __transitions.add(Transition(sourceState.state, to, Transition.ParseAction.GOAL, setOf(Lookahead.EMPTY), null) { _, _ -> true })
                            }
                            else -> {
                                val ts = this.createGoalTransition3(sourceState.state)
                                __transitions.add(ts)//, addLh, parentLh))
                            }
                        }
                    } else {
                        val isAtStart = hg.parent.first().isAtStart //FIXME: what about things not the first?
                        when (isAtStart) {
                            true -> {
                                val ts = this.createHeightTransition3(sourceState.state,hg)
                                __transitions.add(ts)
                            }
                            false -> {
                                val ts = this.createGraftTransition3(sourceState.state,hg)
                                __transitions.add(ts)
                            }
                        }
                    }
                }
            }
            else -> {
                val widthInto = this.stateSet.buildCache.widthInto(previousState,sourceState)
                for (wi in widthInto) {
                    when (wi.to.runtimeRule.kind) {
                        RuntimeRuleKind.TERMINAL -> {
                            val ts = this.createWidthTransition(sourceState.state,wi)
                            __transitions.add(ts)
                        }
                        RuntimeRuleKind.EMBEDDED -> {
                            val ts = this.createEmbeddedTransition(sourceState.state,wi)
                            __transitions.add(ts)
                        }
                        else -> error("should never happen")
                    }
                }
            }
        }
        return __transitions.toSet()
    }

    /*
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
                        __goalTransitions.add(Transition(this, to, Transition.ParseAction.GOAL, LookaheadSet.EMPTY, setOf(LookaheadSet.EMPTY), null) { _, _ -> true })
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
                                        __goalTransitions.add(Transition(this, to, Transition.ParseAction.GOAL, LookaheadSet.EMPTY, setOf(LookaheadSet.EMPTY), null) { _, _ -> true })
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
    */
    private fun createWidthTransition(sourceState:ParserState, wi: WidthInfo): Transition {
        val rp = wi.to
        val toRp = RulePosition(rp.runtimeRule, rp.option, RulePosition.END_OF_RULE) //TODO: is this not passed in ? //assumes rp is a terminal
        val lookaheadInfo = Lookahead(wi.lookaheadSet.lhs(this.stateSet), LookaheadSet.EMPTY)
        val to = this.stateSet.fetchCompatibleOrCreateState(listOf(toRp))
        // upLookahead and prevGuard are unused
        return Transition(sourceState, to, Transition.ParseAction.WIDTH, setOf(lookaheadInfo), null,Transition.defaultRuntimeGuard)
    }

    private fun createEmbeddedTransition(sourceState:ParserState,wi: WidthInfo): Transition {
        val rp = wi.to
        val lookaheadInfo = Lookahead(wi.lookaheadSet.lhs(this.stateSet), LookaheadSet.EMPTY)
        val toRp = RulePosition(rp.runtimeRule, rp.option, RulePosition.END_OF_RULE) //TODO: is this not passed in ?
        val to = this.stateSet.fetchCompatibleOrCreateState(listOf(toRp))
        // upLookahead and prevGuard are unused
        return Transition(sourceState, to, Transition.ParseAction.EMBED, setOf(lookaheadInfo), null,Transition.defaultRuntimeGuard)
    }

    private fun createHeightTransition3(sourceState:ParserState,hg: HeightGraftInfo): Transition {
        val to = this.stateSet.fetchCompatibleOrCreateState(hg.parentNext)
        val lookaheadInfo = hg.lhs.map { Lookahead(it.guard.lhs(this.stateSet), it.up.lhs(this.stateSet)) }.toSet()
        val trs = Transition(sourceState, to, Transition.ParseAction.HEIGHT, lookaheadInfo, null,Transition.defaultRuntimeGuard)
        return trs
    }

    private fun createGraftTransition3(sourceState:ParserState,hg: HeightGraftInfo): Transition {
        val runtimeGuard = Transition.graftRuntimeGuard
        val to = this.stateSet.fetchCompatibleOrCreateState(hg.parentNext)
        val lookaheadInfo = hg.lhs.map { Lookahead(it.guard.lhs(this.stateSet), it.up.lhs(this.stateSet)) }.toSet()
        val trs = Transition(sourceState, to, Transition.ParseAction.GRAFT, lookaheadInfo, hg.parent.toSet(), runtimeGuard)
        return trs
    }

    private fun createGoalTransition3(sourceState:ParserState): Transition {
        val to = this.stateSet.finishState
        val trs = Transition(sourceState, to, Transition.ParseAction.GOAL, setOf(Lookahead(LookaheadSet.EOT, LookaheadSet.EMPTY)), null,Transition.defaultRuntimeGuard)
        return trs
    }

}