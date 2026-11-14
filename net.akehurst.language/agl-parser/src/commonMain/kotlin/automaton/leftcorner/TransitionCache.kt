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

package net.akehurst.language.automaton.leftcorner

import net.akehurst.language.agl.util.Debug
import net.akehurst.language.agl.util.CopyOnWriteMap
import net.akehurst.language.automaton.api.ParseAction
import net.akehurst.language.automaton.api.TransitionContext

interface TransitionCache {
    val allBuiltTransitions: Set<Transition>
    val allPrevious: Set<TransitionPrevInfoKey>

    fun createTransitionForComplete(
        previous: ParserState,
        prevPrev: ParserState,
        from: ParserState,
        action: ParseAction,
        to: ParserState,
        lookahead: Set<Lookahead>
    )

    fun createTransitionForIncomplete(
        previous: ParserState,
        from: ParserState,
        action: ParseAction,
        to: ParserState,
        lookahead: Set<Lookahead>
    )

    //fun addTransition(previousStates: Set<ParserState>, tr: Transition): Transition
    fun addTransitionForIncomplete(previous: ParserState, tr: Transition): Transition
    fun addTransitionForComplete(previous: ParserState, prevPrev: ParserState, tr: Transition): Transition

    // List because we don't want to convert to Set filtered list at runtime
    /**
     * the transitions for the context, null if [calculatedComplete]/[calculatedIncomplete] has not been called for it
     * (an empty list if calculated and there are none).
     * The returned list does not change, when transitions are merged a new list replaces it.
     * Can be called while another thread adds transitions (under AutomatonBuildLock):
     * the transitions of a context are added one at a time, so they are only found once all are added.
     */
    fun findTransitionForCompleteByPrevious(previous: ParserState, prevPrev: ParserState): List<Transition>?
    fun findTransitionForIncompleteByPrevious(previous: ParserState): List<Transition>?

    /** true if transitions were added for the context (e.g. when the automaton was built), call under AutomatonBuildLock */
    fun hasTransitionForComplete(previous: ParserState, prevPrev: ParserState): Boolean
    fun hasTransitionForIncomplete(previous: ParserState): Boolean

    /**
     * Call (under AutomatonBuildLock) when all transitions for the context have been added,
     * so they can be found by [findTransitionForCompleteByPrevious]/[findTransitionForIncompleteByPrevious]
     * (and a context without transitions is not calculated again).
     * Returns the transitions for the context.
     */
    fun calculatedComplete(previous: ParserState, prevPrev: ParserState): List<Transition>
    fun calculatedIncomplete(previous: ParserState): List<Transition>
    fun findTransitionByKey(key: TransitionPrevInfoKey): List<Transition>?

    fun previousFor(transition: Transition): List<TransitionPrevInfoKey>
}

interface TransitionPrevInfoKey {
    val previous: ParserState
}

data class CompleteKey(
    override val previous: ParserState,
    override val prevPrev: ParserState
) : TransitionPrevInfoKey, TransitionContext {
    /** [TransitionContext.prev] alias for [previous] (same value, different name in the public API). */
    override val prev: ParserState get() = previous
    override fun toString(): String = "${prevPrev.number.value}-${previous.number.value}"
}

data class IncompleteKey(
    override val previous: ParserState
) : TransitionPrevInfoKey {
    override fun toString(): String = "${previous.number.value}"
}

class TransitionCacheLC1 : TransitionCache {

    companion object {
        private fun merge(automaton: ParserStateSet, set: Set<Transition>): Transition {
            val grouped = set.groupBy { listOf(it.source, it.action, it.target) }
            val merged = grouped.map { me ->
                val from = me.key[0] as ParserState
                val action = me.key[1] as ParseAction
                val to = me.key[2] as ParserState
                val lhs = Lookahead.merge(automaton, me.value.flatMap { it.lookaheadGuard }.toSet())
                Transition(from, to, action, lhs)
            }
            return if (merged.size == 1) {
                merged.first()
            } else {
                if (Debug.OUTPUT_RUNTIME_BUILD) Debug.debug(Debug.IndentDelta.NONE) { "Tried to Merge:\n  ${set.joinToString(separator = "\n  ") { it.toString() }}" }
                error("Internal Error: transitions not merged\n${merged.joinToString(separator = "\n") { it.toString() }}")
            }
        }
    }

    // transitions stored here
    // (action,to) --> Pair<previous, transition>
    //private val _transitionsByTo = mutableMapOf<Pair<ParseAction,ParserState>, Pair<Set<ParserState>, Transition>>()
    private val _transitionsCompleteByTo = mutableMapOf<Pair<ParseAction, ParserState>, Pair<Set<CompleteKey>, Transition>>()
    private val _transitionsIncompleteByTo = mutableMapOf<Pair<ParseAction, ParserState>, Pair<Set<ParserState>, Transition>>()

    // transitions referenced here
    //private val _transitionsByPrevious: MutableMap<ParserState, MutableList<Transition>> = mutableMapOf()
    // written under AutomatonBuildLock, read without it (by parses running in parallel), so values are never changed, only replaced
    private val _transitionsCompleteByPrevious = CopyOnWriteMap<CompleteKey, List<Transition>>()
    private val _transitionsIncompleteByPrevious = CopyOnWriteMap<ParserState, List<Transition>>()

    // contexts whose transitions have all been added
    private val _calculatedComplete = CopyOnWriteMap<CompleteKey, Boolean>()
    private val _calculatedIncomplete = CopyOnWriteMap<ParserState, Boolean>()

    private val _allBuiltIncomplete get() = _transitionsIncompleteByPrevious.values.flatten()
    private val _allBuiltComplete get() = _transitionsCompleteByPrevious.values.flatten()

    private val _allPreviousIncomplete get() = _transitionsIncompleteByPrevious.keys.map { IncompleteKey(it) }
    private val _allPreviousComplete get() = _transitionsCompleteByPrevious.keys.map { it }

    override val allBuiltTransitions: Set<Transition> get() = (_allBuiltIncomplete + _allBuiltComplete).toSet()
    override val allPrevious: Set<TransitionPrevInfoKey> get() = (_allPreviousIncomplete + _allPreviousComplete).toSet()

    override fun addTransitionForComplete(previous: ParserState, prevPrev: ParserState, tr: Transition): Transition {
        val automaton = tr.source.stateSet
        val key = Pair(tr.action, tr.target)
        val existing = _transitionsCompleteByTo[key]
        val ck = CompleteKey(previous, prevPrev)
        return if (null == existing) {
            _transitionsCompleteByTo[key] = Pair(setOf(ck), tr)
            this.updateCompleteByPrevious(setOf(ck), null, tr)
            tr
        } else {
            val toMerge = setOf(tr, existing.second)
            val mergedTr = merge(automaton, toMerge)
            val mergedPrev = existing.first + ck
            _transitionsCompleteByTo[key] = Pair(mergedPrev, mergedTr)
            this.updateCompleteByPrevious(mergedPrev, existing.second, mergedTr)
            if (Debug.OUTPUT_RUNTIME_BUILD) Debug.debug(Debug.IndentDelta.NONE) { "Merged:\n  ${toMerge.joinToString(separator = "\n  ") { it.toString() }}" }
            if (Debug.OUTPUT_RUNTIME_BUILD) Debug.debug(Debug.IndentDelta.NONE) { "Into:\n  $mergedTr" }

            mergedTr
        }
    }

    override fun addTransitionForIncomplete(previous: ParserState, tr: Transition): Transition {
        val automaton = tr.source.stateSet
        val key = Pair(tr.action, tr.target)
        val existing = _transitionsIncompleteByTo[key]
        return if (null == existing) {
            _transitionsIncompleteByTo[key] = Pair(setOf(previous), tr)
            this.updateIncompleteByPrevious(setOf(previous), null, tr)
            tr
        } else {
            val toMerge = setOf(tr, existing.second)
            val mergedTr = merge(automaton, toMerge)
            val mergedPrev = existing.first + previous
            _transitionsIncompleteByTo[key] = Pair(mergedPrev, mergedTr)
            this.updateIncompleteByPrevious(mergedPrev, existing.second, mergedTr)
            if (Debug.OUTPUT_RUNTIME_BUILD) Debug.debug(Debug.IndentDelta.NONE) { "Merged:\n  ${toMerge.joinToString(separator = "\n  ") { it.toString() }}" }
            if (Debug.OUTPUT_RUNTIME_BUILD) Debug.debug(Debug.IndentDelta.NONE) { "Into:\n  $mergedTr" }
            mergedTr
        }
    }

    private fun updateIncompleteByPrevious(previousSet: Set<ParserState>, oldTransition: Transition?, newTransition: Transition) {
        for (pS in previousSet) {
            val trans = this._transitionsIncompleteByPrevious[pS]
            if (null == trans) {
                this._transitionsIncompleteByPrevious[pS] = listOf(newTransition)
            } else {
                val without = if (null != oldTransition) trans - oldTransition else trans
                this._transitionsIncompleteByPrevious[pS] = without + newTransition
            }
        }
    }

    private fun updateCompleteByPrevious(prevKeySet: Set<CompleteKey>, oldTransition: Transition?, newTransition: Transition) {
        for (pk in prevKeySet) {
            val trans = this._transitionsCompleteByPrevious[pk]
            if (null == trans) {
                this._transitionsCompleteByPrevious[pk] = listOf(newTransition)
            } else {
                val without = if (null != oldTransition) trans - oldTransition else trans
                this._transitionsCompleteByPrevious[pk] = without + newTransition
            }
        }
    }

    override fun findTransitionForCompleteByPrevious(previous: ParserState, prevPrev: ParserState): List<Transition>? {
        val key = CompleteKey(previous, prevPrev)
        return when {
            _calculatedComplete.containsKey(key) -> _transitionsCompleteByPrevious[key] ?: emptyList()
            else -> null
        }
    }

    override fun findTransitionForIncompleteByPrevious(previous: ParserState): List<Transition>? = when {
        _calculatedIncomplete.containsKey(previous) -> _transitionsIncompleteByPrevious[previous] ?: emptyList()
        else -> null
    }

    override fun hasTransitionForComplete(previous: ParserState, prevPrev: ParserState): Boolean =
        _transitionsCompleteByPrevious.containsKey(CompleteKey(previous, prevPrev))

    override fun hasTransitionForIncomplete(previous: ParserState): Boolean =
        _transitionsIncompleteByPrevious.containsKey(previous)

    override fun calculatedComplete(previous: ParserState, prevPrev: ParserState): List<Transition> {
        val key = CompleteKey(previous, prevPrev)
        _calculatedComplete[key] = true
        return _transitionsCompleteByPrevious[key] ?: emptyList()
    }

    override fun calculatedIncomplete(previous: ParserState): List<Transition> {
        _calculatedIncomplete[previous] = true
        return _transitionsIncompleteByPrevious[previous] ?: emptyList()
    }

    override fun findTransitionByKey(key: TransitionPrevInfoKey): List<Transition>? {
        TODO("not implemented")
    }

    override fun previousFor(transition: Transition): List<TransitionPrevInfoKey> {
        return when {
            transition.source.isAtEnd -> _transitionsCompleteByPrevious.entries.filter { it.value.contains(transition) }.map { it.key }
            else -> _transitionsIncompleteByPrevious.entries.filter { it.value.contains(transition) }.map { IncompleteKey(it.key) }
        }
    }

    override fun createTransitionForComplete(previous: ParserState, prevPrev: ParserState, from: ParserState, action: ParseAction, to: ParserState, lookahead: Set<Lookahead>) {
        val trans = Transition(from, to, action, lookahead)
        this.addTransitionForComplete(previous, prevPrev, trans)
    }

    override fun createTransitionForIncomplete(previous: ParserState, from: ParserState, action: ParseAction, to: ParserState, lookahead: Set<Lookahead>) {
        val trans = Transition(from, to, action, lookahead)
        this.addTransitionForIncomplete(previous, trans)
    }
}