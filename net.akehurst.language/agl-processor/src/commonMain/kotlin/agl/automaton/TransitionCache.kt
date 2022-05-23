/**
 * Copyright (C) 2021 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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
import net.akehurst.language.agl.runtime.structure.RulePosition

internal interface TransitionCache {
    val allBuiltTransitions: Set<Transition>
    val allPrevious : List<ParserState>

    fun createTransition(
        previousStates: List<ParserState>,
        from:ParserState,
        action: Transition.ParseAction,
        to: ParserState,
        lookahead: Set<Lookahead>,
        prevGuard: List<RulePosition>?,
        runtimeGuard: Transition.(current: GrowingNodeIndex, previous: List<RulePosition>?) -> Boolean
    )
    fun addTransition(previousStates: List<ParserState>, tr: Transition): Transition

    // List because we don't want to convert to Set filtered list at runtime
    fun findTransitionByPrevious(previous: ParserState): List<Transition>?
    fun previousFor(transition: Transition): List<ParserState?>
}

internal class TransitionCacheLC0 : TransitionCache {

    // transitions stored here
    private var _donePrev = mutableSetOf<ParserState?>()
    private var _transitions:MutableSet<Transition>? = null//mutableSetOf<Transition>()

    override val allBuiltTransitions: Set<Transition> get() = _transitions ?: emptySet()
    override val allPrevious: List<ParserState> = emptyList()

    override fun createTransition(
        previousStates: List<ParserState>,
        from: ParserState,
        action: Transition.ParseAction,
        to: ParserState,
        lookahead: Set<Lookahead>,
        prevGuard: List<RulePosition>?,
        runtimeGuard: Transition.(current: GrowingNodeIndex, previous: List<RulePosition>?) -> Boolean
    ) {
        TODO("not implemented")
    }

    // add the transition and return it, or return existing transition if it already exists
    override fun addTransition(previousStates: List<ParserState>, tr: Transition): Transition {
        if (null==_transitions) {
            _transitions = mutableSetOf()
        }
        var set = _transitions!!
        val exist = set.firstOrNull { it == tr }
        return if (null == exist) {
            set.add(tr)
            tr
        } else {
            exist
        }
    }

    override fun findTransitionByPrevious(previous: ParserState): List<Transition>? {
        return _transitions?.toList()
    }

    override fun previousFor(transition: Transition): List<ParserState?> = emptyList()
}

internal class TransitionCacheLC1 : TransitionCache {

    // transitions stored here
    private val _transitionsByTo = mutableMapOf<ParserState, MutableSet<Transition>>()

    // transitions referenced here
    private val _transitionsByPrevious: MutableMap<ParserState, MutableList<Transition>?> = mutableMapOf()

    override val allBuiltTransitions: Set<Transition> get() = _transitionsByTo.values.flatten().toSet()
    override val allPrevious: List<ParserState> get() = _transitionsByPrevious.keys.toList()

    // add the transition and return it, or return existing transition if it already exists
    override fun addTransition(previousStates: List<ParserState>, tr: Transition): Transition {
        var set = _transitionsByTo[tr.to]
        if (null == set) {
            set = mutableSetOf(tr)
            _transitionsByTo[tr.to] = set
        } else {
            set.add(tr)
        }
        for (pS in previousStates) {
            var set = this._transitionsByPrevious[pS]
            if (null == set) {
                set = mutableListOf(tr)
                this._transitionsByPrevious[pS] = set
            } else {
                val existing = set.firstOrNull { it==tr }
                if (null==existing) {
                    set.add(tr)
                } else {
                    // not added
                }
            }
        }
        return tr
    }

    override fun findTransitionByPrevious(previous: ParserState): List<Transition>? {
        return _transitionsByPrevious[previous]
    }

    override fun previousFor(transition: Transition): List<ParserState?> {
        return _transitionsByPrevious.entries.filter { it.value?.contains(transition) ?: false }.map { it.key }
    }

    override fun createTransition(
        previousStates: List<ParserState>,
        from:ParserState,
        action: Transition.ParseAction,
        to: ParserState,
        lookahead: Set<Lookahead>,
        prevGuard: List<RulePosition>?,
        runtimeGuard: Transition.(current: GrowingNodeIndex, previous: List<RulePosition>?) -> Boolean
    ) {
        val trans = Transition(from, to, action, lookahead, prevGuard, runtimeGuard)
        this.addTransition(previousStates, trans)
    }

}