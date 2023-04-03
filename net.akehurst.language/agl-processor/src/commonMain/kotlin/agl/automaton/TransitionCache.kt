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

import net.akehurst.language.agl.api.automaton.ParseAction
import net.akehurst.language.agl.runtime.structure.RulePosition
import net.akehurst.language.agl.util.Debug

internal interface TransitionCache {
    val allBuiltTransitions: Set<Transition>
    val allPrevious : List<ParserState>

    fun createTransition(
        previousStates: Set<ParserState>,
        from:ParserState,
        action: ParseAction,
        to: ParserState,
        lookahead: Set<Lookahead>
    )
    fun addTransition(previousStates: Set<ParserState>, tr: Transition): Transition

    // List because we don't want to convert to Set filtered list at runtime
    fun findTransitionByPrevious(previous: ParserState): List<Transition>?
    fun previousFor(transition: Transition): List<ParserState>
}

internal class TransitionCacheLC0 : TransitionCache {

    // transitions stored here
    private var _donePrev = mutableSetOf<ParserState?>()
    private var _transitions:MutableSet<Transition>? = null//mutableSetOf<Transition>()

    override val allBuiltTransitions: Set<Transition> get() = _transitions ?: emptySet()
    override val allPrevious: List<ParserState> = emptyList()

    override fun createTransition(
        previousStates: Set<ParserState>,
        from: ParserState,
        action: ParseAction,
        to: ParserState,
        lookahead: Set<Lookahead>
    ) {
        TODO("not implemented")
    }

    // add the transition and return it, or return existing transition if it already exists
    override fun addTransition(previousStates: Set<ParserState>, tr: Transition): Transition {
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

    override fun previousFor(transition: Transition): List<ParserState> = emptyList()
}

internal class TransitionCacheLC1 : TransitionCache {

    companion object {
        private fun merge(automaton:ParserStateSet, set:Set<Transition>):Transition {
            val grouped = set.groupBy { listOf(it.from, it.action, it.to) }
            val merged = grouped.map { me ->
                val from = me.key[0] as ParserState
                val action = me.key[1] as ParseAction
                val to = me.key[2] as ParserState
                val lhs = Lookahead.merge(automaton, me.value.flatMap { it.lookahead }.toSet())
                Transition(from,to,action,lhs)
            }
            return if(merged.size ==1 ) {
                 merged.first()
            } else {
                if (Debug.OUTPUT_RUNTIME_BUILD) Debug.debug(Debug.IndentDelta.NONE) { "Tried to Merge:\n  ${set.joinToString(separator = "\n  ") { it.toString() }}" }
                error("Internal Error: transitions not merged\n${merged.joinToString(separator = "\n") { it.toString() }}")
            }
        }
    }

    // transitions stored here
    // (action,to) --> Pair<previous, transition>
    private val _transitionsByTo = mutableMapOf<Pair<ParseAction,ParserState>, Pair<Set<ParserState>, Transition>>()

    // transitions referenced here
    private val _transitionsByPrevious: MutableMap<ParserState, MutableList<Transition>> = mutableMapOf()

    override val allBuiltTransitions: Set<Transition> get() = _transitionsByPrevious.values.flatten().toSet()
    override val allPrevious: List<ParserState> get() = _transitionsByPrevious.keys.toList()

    // add the transition and return it, or return existing transition if it already exists
    override fun addTransition(previousStates: Set<ParserState>, tr: Transition): Transition {
        val automaton = tr.from.stateSet
        val key = Pair(tr.action, tr.to)
        val existing = _transitionsByTo[key]
        return if (null == existing) {
            _transitionsByTo[key] = Pair(previousStates, tr)
            this.updateByPrevious(previousStates, null, tr)
            tr
        } else {
            val toMerge = setOf(tr, existing.second)
            val mergedTr = merge(automaton, toMerge)
            val mergedPrev = existing.first + previousStates
            _transitionsByTo[key] = Pair(mergedPrev, mergedTr)
            this.updateByPrevious(mergedPrev, existing.second, mergedTr)
            if (Debug.OUTPUT_RUNTIME_BUILD) Debug.debug(Debug.IndentDelta.NONE) { "Merged:\n  ${toMerge.joinToString(separator = "\n  ") { it.toString() }}" }
            if (Debug.OUTPUT_RUNTIME_BUILD) Debug.debug(Debug.IndentDelta.NONE) { "Into:\n  $mergedTr" }

            mergedTr
        }
    }
    private fun updateByPrevious(previousStates: Set<ParserState>, oldTransition: Transition?, newTransition: Transition) {
        for (pS in previousStates) {
            var trans = this._transitionsByPrevious[pS]
            if (null == trans) {
                trans = mutableListOf(newTransition)
                this._transitionsByPrevious[pS] = trans
            } else {
                if(null!=oldTransition) trans.remove(oldTransition)
                trans.add(newTransition)
            }
        }
    }

    override fun findTransitionByPrevious(previous: ParserState): List<Transition>? {
        return _transitionsByPrevious[previous]
    }

    override fun previousFor(transition: Transition): List<ParserState> {
        return _transitionsByPrevious.entries.filter { it.value?.contains(transition) ?: false }.map { it.key }
    }

    override fun createTransition(
        previousStates: Set<ParserState>,
        from:ParserState,
        action: ParseAction,
        to: ParserState,
        lookahead: Set<Lookahead>
    ) {
        val trans = Transition(from, to, action, lookahead)
        this.addTransition(previousStates, trans)
    }

}