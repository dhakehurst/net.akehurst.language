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

package net.akehurst.language.automaton.leftcorner

import net.akehurst.language.agl.runtime.structure.*
import net.akehurst.language.agl.util.Debug
import net.akehurst.language.automaton.api.AutomatonKind

class ParserState(
    val number: StateNumber,
    val rulePositions: List<RulePositionRuntime>, //must be a list (not a set) so that we can index against Growing children
    val stateSet: ParserStateSet
) {

    companion object {
        fun LookaheadSetPart.lhs(stateSet: ParserStateSet): LookaheadSet {
            return stateSet.createLookaheadSet(this.includesRT, this.includesEOT, this.matchANY, this.content)
        }
    }

    val outTransitions: TransitionCache = when (this.stateSet.automatonKind) {
        AutomatonKind.LOOKAHEAD_NONE -> TODO() //TransitionCacheLC0()
        AutomatonKind.LOOKAHEAD_SIMPLE -> TODO()
        AutomatonKind.LOOKAHEAD_1 -> TransitionCacheLC1()
    }

    //TODO: fast at runtime if not lazy
    val rulePositionIdentity = rulePositions.map { it.identity }.toSet()
    val runtimeRules: List<RuntimeRule> by lazy { this.rulePositions.map { it.rule as RuntimeRule }.toList() }
    val runtimeRulesAsSet: Set<RuntimeRule> by lazy { this.rulePositions.map { it.rule as RuntimeRule }.toSet() }
    val optionList: List<Int> by lazy { this.rulePositions.map { it.option }.toList() }
    val priorityList: List<Int> get() = optionList
    val positionList: List<Int> by lazy { this.rulePositions.map { it.position }.toList() }
    val choiceKindList: List<RuntimeRuleChoiceKind> by lazy {
        this.rulePositions.mapNotNull {
            val rhs = it.rule.rhs
            when (rhs) {
                is RuntimeRuleRhsChoice -> rhs.choiceKind
                else -> null
            }
        }.toSet().toList()
    }
    val isChoice: Boolean by lazy { this.firstRule.isChoice }

    val firstRuleChoiceKind by lazy {
        if (Debug.CHECK) check(1 == this.choiceKindList.size)
        this.choiceKindList[0]
    }

    val firstRule get() = runtimeRules.first()

    val isLeaf: Boolean get() = this.firstRule.isTerminal

    val isAtEnd: Boolean get() = this.rulePositions.any { it.isAtEnd } //all in state should be either atEnd or notAtEnd
    val isNotAtEnd: Boolean get() = this.rulePositions.any { it.isAtEnd.not() } //all in state should be either atEnd or notAtEnd

    val isGoal = this.firstRule.isGoal
    val isUserGoal = this.firstRule == this.stateSet.userGoalRule

    internal fun createLookaheadSet(includesUP: Boolean, includeEOT: Boolean, matchAny: Boolean, content: Set<RuntimeRule>): LookaheadSet =
        this.stateSet.createLookaheadSet(includesUP, includeEOT, matchAny, content)

    fun transitionsGoal(previous: ParserState): List<Transition> {
        val cache: List<Transition>? = this.outTransitions.findTransitionForIncompleteByPrevious(previous)
        val trans = if (null == cache) {
            if (Debug.CHECK) check(this.stateSet.preBuilt.not()) { "Transitions not built for $this -previous-> $previous" }
            //do not pass RuntimeState (in particular runtimeLookahead) into transition calculator
            // or trans cannot be cached by ParserState. if cache by runtimeLookahead then explosion of data like LR(1)
            val filteredTransitions = this.stateSet.runtimeTransitionCalculator.calTransitionsForGoal(this, previous).toList()
            val storedTrans = filteredTransitions.map { this.outTransitions.addTransitionForIncomplete(previous, it) }
            storedTrans
        } else {
            cache
        }
        return trans
    }

    fun transitionsInComplete(previous: ParserState): List<Transition> {
        val cache: List<Transition>? = this.outTransitions.findTransitionForIncompleteByPrevious(previous)
        val trans = if (null == cache) {
            if (Debug.CHECK) check(this.stateSet.preBuilt.not()) { "Transitions not built for $this -previous-> $previous" }
            //do not pass RuntimeState (in particular runtimeLookahead) into transition calculator
            // or trans cannot be cached by ParserState. if cache by runtimeLookahead then explosion of data like LR(1)
            val filteredTransitions = this.stateSet.runtimeTransitionCalculator.calcTransitionsForInComplete(this, previous).toList()
            val storedTrans = filteredTransitions.map { this.outTransitions.addTransitionForIncomplete(previous, it) }
            storedTrans
        } else {
            cache
        }
        return trans
    }

    fun transitionsComplete(previous: ParserState, prevPrev: ParserState): List<Transition> {
        val cache: List<Transition>? = this.outTransitions.findTransitionForCompleteByPrevious(previous, prevPrev)
        val trans = if (null == cache) {
            if (Debug.CHECK) check(this.stateSet.preBuilt.not()) { "Transitions not built for $this -previous-> $previous" }
            //do not pass RuntimeState (in particular runtimeLookahead) into transition calculator
            // or trans cannot be cached by ParserState. if cache by runtimeLookahead then explosion of data like LR(1)
            val filteredTransitions = this.stateSet.runtimeTransitionCalculator.calcTransitionsForComplete(this, previous, prevPrev).toList()
            val storedTrans = filteredTransitions.map { this.outTransitions.addTransitionForComplete(previous, prevPrev, it) }
            storedTrans
        } else {
            cache
        }
        return trans
    }

    // --- Any ---
    override fun hashCode(): Int = this.number.value + this.stateSet.number * 31

    override fun equals(other: Any?): Boolean {
        return if (other is ParserState) {
            this.stateSet.number == other.stateSet.number && this.number.value == other.number.value
        } else {
            false
        }
    }

    override fun toString(): String = "State(${this.number.value}/${this.stateSet.number}-${rulePositions})"

}
