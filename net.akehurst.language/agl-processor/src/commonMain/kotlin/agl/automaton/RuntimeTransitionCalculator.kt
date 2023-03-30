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

import net.akehurst.language.agl.api.automaton.ParseAction
import net.akehurst.language.agl.automaton.ParserState.Companion.lhs
import net.akehurst.language.agl.runtime.structure.RuntimeRule

internal class RuntimeTransitionCalculator(
    val stateSet: ParserStateSet
) {

    //internal fun calcFilteredTransitions(prevPrev: ParserState, previousState: ParserState, sourceState: ParserState): Set<Transition> {
    //    val transitions = this.calcTransitions(prevPrev, previousState, sourceState)//, gn.lookaheadStack.peek())
    //    return transitions
    //}

    private val __transitions = mutableSetOf<Transition>() // to save time allocating when calcTransitions is called

    fun calTransitionsForGoal(sourceState: ParserState, previousState: ParserState): Set<Transition> {
        __transitions.clear()
        val widthInto = this.stateSet.buildCache.widthInto(previousState, sourceState)
        for (wi in widthInto) {
            val rr = wi.to.rule as RuntimeRule
            when {
                rr.isTerminal -> __transitions.add(this.createWidthOrEmbeddedTransition(sourceState, wi))
                else -> error("Should never happen")
            }
        }
        return __transitions
    }

    fun calcTransitionsForComplete(sourceState: ParserState, previousState: ParserState, prevPrev: ParserState): Set<Transition> {
        __transitions.clear()
        val heightOrGraftInto = this.stateSet.buildCache.heightOrGraftInto(prevPrev, previousState, sourceState)
        for (hg in heightOrGraftInto) {
            when (hg.action) {
                ParseAction.GOAL -> {
                    when {
                        (sourceState.isGoal && this.stateSet.isSkip) -> {
                            // must be end of skip. TODO: can do something better than this!
                            val to = sourceState
                            __transitions.add(Transition(sourceState, to, ParseAction.GOAL, setOf(Lookahead.EMPTY)))
                        }

                        else -> {
                            //must pass in hg, because for embedded rules, GOAL does not end with EOT
                            val ts = this.createGoalTransition3(sourceState, hg)
                            __transitions.add(ts)//, addLh, parentLh))
                        }
                    }
                }

                ParseAction.HEIGHT -> {
                    val ts = this.createHeightTransition3(sourceState, hg)
                    __transitions.add(ts)
                }

                ParseAction.GRAFT -> {
                    val ts = this.createGraftTransition3(sourceState, hg)
                    __transitions.add(ts)
                }

                else -> error("")
            }
        }
        return __transitions
    }

    fun calcTransitionsForInComplete(source: ParserState, previous: ParserState): Set<Transition> {
        __transitions.clear()
        val widthInto = this.stateSet.buildCache.widthInto(previous, source)
        for (wi in widthInto) {
            val ts = this.createWidthOrEmbeddedTransition(source, wi)
            __transitions.add(ts)
        }
        return __transitions
    }

    // must use previousState.rulePosition as starting point for finding
    // lookahead for height/graft, and previousLookahead to use if end up at End of rule
    // due to position or empty rules.
    internal fun calcTransitions(
        prevPrev: ParserState,
        previousState: ParserState,
        sourceState: ParserState
    ): Set<Transition> {//TODO: add previous in order to filter parent relations
        __transitions.clear()
        when {
            sourceState.isGoal -> {
                val widthInto = this.stateSet.buildCache.widthInto(previousState, sourceState)
                for (wi in widthInto) {
                    val rr = wi.to.rule as RuntimeRule
                    when {
                        rr.isTerminal -> __transitions.add(this.createWidthOrEmbeddedTransition(sourceState, wi))
                        else -> error("Should never happen")
                    }
                }
            }

            sourceState.isAtEnd -> {
                val transInfos = this.stateSet.buildCache.heightOrGraftInto(prevPrev, previousState, sourceState)
                for (hg in transInfos) {
                    when (hg.action) {
                        ParseAction.GOAL -> {
                            when {
                                (sourceState.isGoal && this.stateSet.isSkip) -> {
                                    // must be end of skip. TODO: can do something better than this!
                                    val to = sourceState
                                    __transitions.add(Transition(sourceState, to, ParseAction.GOAL, setOf(Lookahead.EMPTY)))
                                }

                                else -> {
                                    //must pass in hg, because for embedded rules, GOAL does not end with EOT
                                    val ts = this.createGoalTransition3(sourceState, hg)
                                    __transitions.add(ts)//, addLh, parentLh))
                                }
                            }
                        }

                        ParseAction.HEIGHT -> {
                            val ts = this.createHeightTransition3(sourceState, hg)
                            __transitions.add(ts)
                        }

                        ParseAction.GRAFT -> {
                            val ts = this.createGraftTransition3(sourceState, hg)
                            __transitions.add(ts)
                        }

                        else -> error("")
                    }
                }
            }

            else -> {
                val widthInto = this.stateSet.buildCache.widthInto(previousState, sourceState)
                for (wi in widthInto) {
                    val ts = this.createWidthOrEmbeddedTransition(sourceState, wi)
                    __transitions.add(ts)
                }
            }
        }

        return __transitions
    }

    internal fun createWidthTransFor(from: ParserState, widthInto: Set<WidthInfo>): Set<Transition> {
        __transitions.clear()
        for (wi in widthInto) {
            val ts = this.createWidthOrEmbeddedTransition(from, wi)
            __transitions.add(ts)
        }
        return __transitions
    }

    private fun createWidthOrEmbeddedTransition(sourceState: ParserState, wi: WidthInfo): Transition {
        val rp = wi.to
        val lookaheadInfo = Lookahead(wi.lookaheadSet.lhs(this.stateSet), LookaheadSet.EMPTY)
        val to = this.stateSet.fetchCompatibleOrCreateState(listOf(rp))
        // upLookahead and prevGuard are unused
        return Transition(sourceState, to, wi.action, setOf(lookaheadInfo))
    }

    private fun createHeightTransition3(sourceState: ParserState, hg: TransInfo): Transition {
        val to = this.stateSet.fetchCompatibleOrCreateState(hg.to.toList())
        val lookaheadInfo = hg.lookahead.map { Lookahead(it.guard.lhs(this.stateSet), it.up.lhs(this.stateSet)) }.toSet()
        val trs = Transition(sourceState, to, ParseAction.HEIGHT, lookaheadInfo)
        return trs
    }

    private fun createGraftTransition3(sourceState: ParserState, hg: TransInfo): Transition {
        val to = this.stateSet.fetchCompatibleOrCreateState(hg.to.toList())
        val lookaheadInfo = hg.lookahead.map { Lookahead(it.guard.lhs(this.stateSet), LookaheadSet.EMPTY) }.toSet()
        val trs = Transition(sourceState, to, ParseAction.GRAFT, lookaheadInfo)
        return trs
    }

    private fun createGoalTransition3(sourceState: ParserState, hg: TransInfo): Transition {
        val to = this.stateSet.finishState
        //// must compute lookaheadInfo, because for embedded grammars, guard for completion of GOAL is not necessarily EOT
        //val lookaheadInfo = hg.lhs.map { Lookahead(it.guard.lhs(this.stateSet), LookaheadSet.EMPTY) }.toSet()
        val lookaheadInfo = hg.lookahead.map { Lookahead(LookaheadSet.EOT, LookaheadSet.EMPTY) }.toSet()
        val trs = Transition(sourceState, to, ParseAction.GOAL, lookaheadInfo)
        return trs
    }

}