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

//import net.akehurst.language.agl.runtime.graph.GrowingNodeIndex
//import net.akehurst.language.agl.runtime.graph.RuntimeState
import net.akehurst.language.agl.runtime.graph.RuntimeState
import net.akehurst.language.agl.runtime.structure.*
import net.akehurst.language.agl.util.Debug
import net.akehurst.language.api.processor.AutomatonKind

internal class ParserState(
    val number: StateNumber,
    //val rulePosition: RulePosition,
    val rulePositions: List<RulePosition>, //must be a list so that we can index against Growing children
    val stateSet: ParserStateSet
) {

    companion object {
        fun LookaheadSetPart.lhs(stateSet: ParserStateSet): LookaheadSet {
            return stateSet.createLookaheadSet(this.includesUP, this.includesEOT, this.matchANY, this.content)
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
    val isChoice: Boolean by lazy { this.choiceKindList.isNotEmpty() } // it should be empty if not a choice

    val firstRuleChoiceKind by lazy {
        if (Debug.CHECK) check(1 == this.choiceKindList.size)
        this.choiceKindList[0]
    }

    val firstRule get() = runtimeRules.first()

    val isLeaf: Boolean get() = this.firstRule.kind == RuntimeRuleKind.TERMINAL //should only be one RP if it is a leaf

    val isAtEnd: Boolean get()= this.rulePositions.any { it.isAtEnd } //all in state should be either atEnd or notAtEnd
    val isNotAtEnd: Boolean get()= this.rulePositions.any { it.isAtEnd.not() } //all in state should be either atEnd or notAtEnd

    val isGoal = this.firstRule.kind == RuntimeRuleKind.GOAL
    val isUserGoal = this.firstRule == this.stateSet.userGoalRule

    fun firstOf(ifReachedEnd: LookaheadSet): LookaheadSetPart = this.rulePositions.map {
        stateSet.buildCache.expectedAt(it, LookaheadSetPart(ifReachedEnd.includesUP, ifReachedEnd.includesEOT, ifReachedEnd.matchANY, ifReachedEnd.content))
    }.fold(LookaheadSetPart.EMPTY) { acc, e -> acc.union(e) }

    internal fun createLookaheadSet(includesUP: Boolean, includeEOT: Boolean, matchAny: Boolean, content: Set<RuntimeRule>): LookaheadSet =
        this.stateSet.createLookaheadSet(includesUP, includeEOT, matchAny, content)

    fun transitions(previousState: RuntimeState, sourceState: RuntimeState): List<Transition> {
        val cache: List<Transition>? = this.outTransitions.findTransitionByPrevious(previousState.state)
        val trans = if (null == cache) {
            check(this.stateSet.preBuilt.not(), { "Transitions not built for $this -previous-> $previousState" })
            val filteredTransitions = this.stateSet.runtimeTransitionCalculator.calcFilteredTransitions(previousState,sourceState).toList()
            val storedTrans = filteredTransitions.map { this.outTransitions.addTransition(listOf(previousState.state), it) }
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
