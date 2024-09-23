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
import net.akehurst.language.automaton.api.ParseAction

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
    fun findTransitionForCompleteByPrevious(previous: ParserState, prevPrev: ParserState): List<Transition>?
    fun findTransitionForIncompleteByPrevious(previous: ParserState): List<Transition>?
    fun findTransitionByKey(key: TransitionPrevInfoKey): List<Transition>?

    fun previousFor(transition: Transition): List<TransitionPrevInfoKey>
}

interface TransitionPrevInfoKey

data class CompleteKey(
    val previous: ParserState,
    val prevPrev: ParserState
) : TransitionPrevInfoKey {
    override fun toString(): String = "${prevPrev.number.value}-${previous.number.value}"
}

data class IncompleteKey(
    val previous: ParserState
) : TransitionPrevInfoKey {
    override fun toString(): String = "${previous.number.value}"
}

class TransitionCacheLC1 : TransitionCache {

    companion object {
        private fun merge(automaton: ParserStateSet, set: Set<Transition>): Transition {
            val grouped = set.groupBy { listOf(it.from, it.action, it.to) }
            val merged = grouped.map { me ->
                val from = me.key[0] as ParserState
                val action = me.key[1] as ParseAction
                val to = me.key[2] as ParserState
                val lhs = Lookahead.merge(automaton, me.value.flatMap { it.lookahead }.toSet())
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
    private val _transitionsCompleteByPrevious: MutableMap<CompleteKey, MutableList<Transition>> = mutableMapOf()
    private val _transitionsIncompleteByPrevious: MutableMap<ParserState, MutableList<Transition>> = mutableMapOf()

    private val _allBuiltIncomplete get() = _transitionsIncompleteByPrevious.values.flatten()
    private val _allBuiltComplete get() = _transitionsCompleteByPrevious.values.flatten()

    private val _allPreviousIncomplete get() = _transitionsIncompleteByPrevious.keys.map { IncompleteKey(it) }
    private val _allPreviousComplete get() = _transitionsCompleteByPrevious.keys.map { it }

    override val allBuiltTransitions: Set<Transition> get() = (_allBuiltIncomplete + _allBuiltComplete).toSet()
    override val allPrevious: Set<TransitionPrevInfoKey> get() = (_allPreviousIncomplete + _allPreviousComplete).toSet()

    override fun addTransitionForComplete(previous: ParserState, prevPrev: ParserState, tr: Transition): Transition {
        val automaton = tr.from.stateSet
        val key = Pair(tr.action, tr.to)
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
        val automaton = tr.from.stateSet
        val key = Pair(tr.action, tr.to)
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
            var trans = this._transitionsIncompleteByPrevious[pS]
            if (null == trans) {
                trans = mutableListOf(newTransition)
                this._transitionsIncompleteByPrevious[pS] = trans
            } else {
                if (null != oldTransition) trans.remove(oldTransition)
                trans.add(newTransition)
            }
        }
    }

    private fun updateCompleteByPrevious(prevKeySet: Set<CompleteKey>, oldTransition: Transition?, newTransition: Transition) {
        for (pk in prevKeySet) {
            var trans = this._transitionsCompleteByPrevious[pk]
            if (null == trans) {
                trans = mutableListOf(newTransition)
                this._transitionsCompleteByPrevious[pk] = trans
            } else {
                if (null != oldTransition) trans.remove(oldTransition)
                trans.add(newTransition)
            }
        }
    }

    override fun findTransitionForCompleteByPrevious(previous: ParserState, prevPrev: ParserState): List<Transition>? =
        _transitionsCompleteByPrevious[CompleteKey(previous, prevPrev)]

    override fun findTransitionForIncompleteByPrevious(previous: ParserState): List<Transition>? =
        _transitionsIncompleteByPrevious[previous]

    override fun findTransitionByKey(key: TransitionPrevInfoKey): List<Transition>? {
        TODO("not implemented")
    }

    override fun previousFor(transition: Transition): List<TransitionPrevInfoKey> {
        return when {
            transition.from.isAtEnd -> _transitionsCompleteByPrevious.entries.filter { it.value.contains(transition) }.map { it.key }
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