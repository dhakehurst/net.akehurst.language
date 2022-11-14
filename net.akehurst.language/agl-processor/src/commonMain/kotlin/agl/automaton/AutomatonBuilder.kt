/**
 * Copyright (C) 2020 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

import net.akehurst.language.agl.api.automaton.Automaton
import net.akehurst.language.agl.api.automaton.ParseAction
import net.akehurst.language.agl.api.automaton.State
import net.akehurst.language.agl.api.runtime.Rule
import net.akehurst.language.agl.api.runtime.RulePosition
import net.akehurst.language.agl.api.runtime.RuleSet
import net.akehurst.language.agl.runtime.structure.RuleOptionPosition
import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.api.processor.AutomatonKind

@DslMarker
internal annotation class AglAutomatonDslMarker

fun automaton(
    rrs: RuleSet,
    automatonKind: AutomatonKind,
    userGoalRule: String,
    stateSetNumber: Int,
    isSkip: Boolean,
    init: AutomatonBuilder.() -> Unit
): Automaton {
    val b = AutomatonBuilder(rrs as RuntimeRuleSet, automatonKind, userGoalRule, stateSetNumber, isSkip)
    b.init()
    return b.build()
}

@AglAutomatonDslMarker
class AutomatonBuilder(
    rrs: RuleSet,
    automatonKind: AutomatonKind,
    userGoalRule: String,
    stateSetNumber: Int,
    isSkip: Boolean
) {

    private val result = ParserStateSet(stateSetNumber, rrs as RuntimeRuleSet, rrs.goalRuleFor[userGoalRule] as RuntimeRule, isSkip, automatonKind)
    private var nextState = 0

    val GOAL = ParseAction.GOAL
    val WIDTH = ParseAction.WIDTH
    val HEIGHT = ParseAction.HEIGHT
    val GRAFT = ParseAction.GRAFT

    fun state(rule:Rule, position:Int):State =result.createState(listOf(RuleOptionPosition(rule as RuntimeRule, 0, position)))

    fun state(vararg rulePositions: RulePosition): State {
        return result.createState(rulePositions.toList())
    }

    fun transition(
        previousState: ParserState,
        from: ParserState,
        to: ParserState,
        action: ParseAction,
        lookaheadGuardContent: Set<RuntimeRule>,
        upLookaheadContent: Set<Set<RuntimeRule>>,
        prevGuard: Set<RuleOptionPosition>?
    ): Transition = transition(setOf(previousState), from, to, action, lookaheadGuardContent, upLookaheadContent, prevGuard)

    fun transition(
        previousStates: Set<ParserState>,
        from: ParserState,
        to: ParserState,
        action: ParseAction,
        lookaheadGuardContent: Set<RuntimeRule>,
        upLookaheadContent: Set<Set<RuntimeRule>>,
        prevGuard: Set<RuleOptionPosition>?
    ): Transition {
        return transition1(previousStates, from, to, action, lookaheadGuardContent, upLookaheadContent, prevGuard)
    }

    fun transition(
        previousState: ParserState,
        from: ParserState,
        to: ParserState,
        action: ParseAction,
        prevGuard: Set<RuleOptionPosition>?,
        init: TransitionBuilder.() -> Unit
    ): Transition = transition(setOf(previousState), from, to, action, prevGuard, init)

    fun transition(
        previousStates: Set<ParserState>,
        from: ParserState,
        to: ParserState,
        action: ParseAction,
        prevGuard: Set<RuleOptionPosition>?,
        init: TransitionBuilder.() -> Unit
    ): Transition {
        val b = TransitionBuilder(result, action)
        b.ctx(previousStates)
        b.src(from)
        b.tgt(to)
        b.gpg(prevGuard)
        b.init()
        val trans = b.build()
        from.outTransitions.addTransition(previousStates, trans)
        return trans
    }

    fun transition(
        action: ParseAction,
        init: TransitionBuilder.() -> Unit
    ): Transition {
        val b = TransitionBuilder(result, action)
        b.init()
        return b.build()
    }

    fun transition1(
        previousStates: Set<ParserState>,
        from: ParserState,
        to: ParserState,
        action: ParseAction,
        lookaheadGuardContent: Set<RuntimeRule>,
        upLookaheadContent: Set<Set<RuntimeRule>>,
        prevGuard: Set<RuleOptionPosition>?
    ): Transition {
        //TODO: fix builder here to build Set<Lookahead>
        val guard = when {
            lookaheadGuardContent.isEmpty() -> LookaheadSet.EMPTY
            else -> LookaheadSet.createFromRuntimeRules(result, lookaheadGuardContent)
        }
        val up = when {
            upLookaheadContent.isEmpty() -> setOf(LookaheadSet.EMPTY)
            else -> upLookaheadContent.map { LookaheadSet.createFromRuntimeRules(result, it) }.toSet()
        }
        val lh = up.map { Lookahead(guard, it) }.toSet()
        val trans = Transition(from, to, action, lh)
        return from.outTransitions.addTransition(previousStates, trans)
    }

    fun build(): ParserStateSet {
        return result
    }
}

@AglAutomatonDslMarker
internal class TransitionBuilder(
    val stateSet: ParserStateSet,
    val action: ParseAction
) {

    private var _context = emptySet<State>()
    private lateinit var _src: State
    private lateinit var _tgt: State
    private val _lhg = mutableSetOf<Lookahead>()

    fun ctx(states: Set<State>) {
        _context = states
    }

    fun ctx(vararg rulePositions: RuleOptionPosition) {
        val states = rulePositions.map { this.stateSet.fetchState(listOf(it)) ?: error("State for $it not defined") }.toSet()
        this.ctx(states)
    }

    fun ctx(runtimeRule: RuntimeRule, option: Int, position: Int) {
        val rp = RuleOptionPosition(runtimeRule, option, position)
        val state = this.stateSet.fetchState(listOf(rp)) ?: error("State for $rp not defined")
        this.ctx(setOf(state))
    }

    fun src(runtimeRule: RuntimeRule) {
        check(this.action == ParseAction.HEIGHT || this.action == ParseAction.GRAFT || this.action == ParseAction.GOAL)
        src(runtimeRule, 0, RuleOptionPosition.END_OF_RULE)
    }

    fun src(runtimeRule: RuntimeRule, option: Int, position: Int) {
        val rp = RuleOptionPosition(runtimeRule, option, position)
        val state = this.stateSet.fetchState(listOf(rp)) ?: error("State for $rp not defined")
        this.src(state)
    }

    fun tgt(runtimeRule: RuntimeRule) = tgt(runtimeRule, 0, RuleOptionPosition.END_OF_RULE)
    fun tgt(runtimeRule: RuntimeRule, option: Int, position: Int) {
        val rp = RuleOptionPosition(runtimeRule, option, position)
        val state = this.stateSet.fetchState(listOf(rp)) ?: error("State for $rp not defined")
        this.tgt(state)
    }

    fun src(state: State) {
        _src = state
    }

    fun tgt(state: State) {
        _tgt = state
    }

    fun lhg(guard: Rule) = lhg(setOf(guard))

    fun lhg(guard: RuntimeRule, up: RuntimeRule) = lhg(setOf(guard), setOf(up))

    fun lhg(guard: Set<Rule>) {
        this._lhg.add(Lookahead(LookaheadSet.createFromRuntimeRules(this.stateSet, guard as Set<RuntimeRule>), LookaheadSet.EMPTY))
    }

    fun lhg(guard: Set<Rule>, up: Set<Rule>) = lhg(guard as Set<RuntimeRule> , up as Set<RuntimeRule>)
    fun lhg(guard: Set<RuntimeRule>, up: Set<RuntimeRule>) {
        this._lhg.add(Lookahead(LookaheadSet.createFromRuntimeRules(this.stateSet, guard), LookaheadSet.createFromRuntimeRules(this.stateSet, up)))
    }

    /**
     * graft prev guard
     */
    fun gpg(runtimeGuard: Set<RuleOptionPosition>?) {

    }

    /**
     * graft prev guard
     */
    fun gpg(runtimeRule: RuntimeRule, option: Int, position: Int) {
        val set = setOf(RuleOptionPosition(runtimeRule, option, position))
        this.gpg(set)
    }

    fun build(): Transition {
        val trans = Transition(_src as ParserState, _tgt as ParserState, action, _lhg)
        (_src as ParserState).outTransitions.addTransition(_context as Set<ParserState>, trans)
        return trans
    }
}