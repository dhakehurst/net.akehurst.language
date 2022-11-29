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

import net.akehurst.language.agl.api.automaton.*
import net.akehurst.language.agl.api.runtime.Rule
import net.akehurst.language.agl.runtime.structure.RulePosition
import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.api.processor.AutomatonKind

internal fun automaton(
    rrs: RuntimeRuleSet,
    automatonKind: AutomatonKind,
    userGoalRule: String,
    isSkip: Boolean,
    init: AutomatonBuilderDefault.() -> Unit
): ParserStateSet {
    val b = AutomatonBuilderDefault(rrs, automatonKind, userGoalRule, isSkip)
    b.init()
    return b.build()
}

@AglAutomatonDslMarker
internal class AutomatonBuilderDefault(
    val rrs: RuntimeRuleSet,
    automatonKind: AutomatonKind,
    userGoalRuleName: String,
    isSkip: Boolean
) : AutomatonBuilder {

    private val result = ParserStateSet(rrs.nextStateSetNumber++, rrs, rrs.findRuntimeRule(userGoalRuleName), isSkip, automatonKind)
    private var nextState = 0

    val GOAL = ParseAction.GOAL
    val WIDTH = ParseAction.WIDTH
    val HEIGHT = ParseAction.HEIGHT
    val GRAFT = ParseAction.GRAFT

    override fun state(ruleNumber: Int, option: Int, position: Int) {
        state(rrs.runtimeRules[ruleNumber], option, position)
    }

    fun state(rule: Rule, option: Int, position: Int) = result.createState(listOf(RulePosition(rule as RuntimeRule, option, position)))

    fun state(vararg rulePositions: RulePosition) = result.createState(rulePositions.toList())

    internal fun transition(
        previousState: ParserState,
        from: ParserState,
        to: ParserState,
        action: ParseAction,
        lookaheadGuardContent: Set<RuntimeRule>,
        upLookaheadContent: Set<Set<RuntimeRule>>,
        prevGuard: Set<RulePosition>?
    ) = transition(setOf(previousState), from, to, action, lookaheadGuardContent, upLookaheadContent, prevGuard)

    internal fun transition(
        previousStates: Set<ParserState>,
        from: ParserState,
        to: ParserState,
        action: ParseAction,
        lookaheadGuardContent: Set<RuntimeRule>,
        upLookaheadContent: Set<Set<RuntimeRule>>,
        prevGuard: Set<RulePosition>?
    ) {
        transition1(previousStates, from, to, action, lookaheadGuardContent, upLookaheadContent, prevGuard)
    }

    internal fun transition(
        previousState: ParserState,
        from: ParserState,
        to: ParserState,
        action: ParseAction,
        prevGuard: Set<RulePosition>?,
        init: TransitionBuilderDefault.() -> Unit
    ) = transition(setOf(previousState), from, to, action, prevGuard, init)

    internal fun transition(
        previousStates: Set<ParserState>,
        from: ParserState,
        to: ParserState,
        action: ParseAction,
        prevGuard: Set<RulePosition>?,
        init: TransitionBuilderDefault.() -> Unit
    ) {
        val b = TransitionBuilderDefault(result, action)
        b.ctx(previousStates)
        b.src(from)
        b.tgt(to)
        b.gpg(prevGuard)
        b.init()
        val trans = b.build()
        from.outTransitions.addTransition(previousStates, trans)
    }

    //override fun transition(action: ParseAction, init: TransitionBuilder.() -> Unit) {
    //    this.transition(action, init as (TransitionBuilderDefault.() -> Unit))
    //}
    fun transition(action: ParseAction, init: TransitionBuilderDefault.() -> Unit) {
        val b = TransitionBuilderDefault(result, action)
        b.init()
        b.build()
    }

    internal fun transition1(
        previousStates: Set<ParserState>,
        from: ParserState,
        to: ParserState,
        action: ParseAction,
        lookaheadGuardContent: Set<RuntimeRule>,
        upLookaheadContent: Set<Set<RuntimeRule>>,
        prevGuard: Set<RulePosition>?
    ) {
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
        from.outTransitions.addTransition(previousStates, trans)
    }

    internal fun build(): ParserStateSet {
        return result
    }
}

@AglAutomatonDslMarker
internal class TransitionBuilderDefault internal constructor(
    private val stateSet: ParserStateSet,
    val action: ParseAction
) : TransitionBuilder {

    private var _context = emptySet<ParserState>()
    private lateinit var _src: ParserState
    private lateinit var _tgt: ParserState
    private val _lhg = mutableSetOf<Lookahead>()

    override fun ctx(vararg stateNumbers: Int) {
        TODO("not implemented")
    }

    override fun src(stateNumber: Int) {
        TODO("not implemented")
    }

    override fun tgt(stateNumber: Int) {
        TODO("not implemented")
    }

    fun ctx(states: Set<ParserState>) {
        _context = states
    }

    fun ctx(vararg rulePositions: RulePosition) {
        val states = rulePositions.map { this.stateSet.fetchState(listOf(it)) ?: error("State for $it not defined") }.toSet()
        this.ctx(states)
    }

    fun ctx(runtimeRule: RuntimeRule, option: Int, position: Int) {
        val rp = RulePosition(runtimeRule, option, position)
        val state = this.stateSet.fetchState(listOf(rp)) ?: error("State for $rp not defined")
        this.ctx(setOf(state))
    }

    fun src(runtimeRule: RuntimeRule) {
        check(this.action == ParseAction.HEIGHT || this.action == ParseAction.GRAFT || this.action == ParseAction.GOAL)
        src(runtimeRule, 0, RulePosition.END_OF_RULE)
    }

    fun src(runtimeRule: RuntimeRule, option: Int, position: Int) {
        val rp = RulePosition(runtimeRule, option, position)
        val state = this.stateSet.fetchState(listOf(rp)) ?: error("State for $rp not defined")
        this.src(state)
    }

    fun tgt(runtimeRule: RuntimeRule) = tgt(runtimeRule, 0, RulePosition.END_OF_RULE)
    fun tgt(runtimeRule: RuntimeRule, option: Int, position: Int) {
        val rp = RulePosition(runtimeRule, option, position)
        val state = this.stateSet.fetchState(listOf(rp)) ?: error("State for $rp not defined")
        this.tgt(state)
    }

    fun src(state: ParserState) {
        _src = state
    }

    fun tgt(state: ParserState) {
        _tgt = state
    }

    fun lhg(guard: RuntimeRule) = lhg(setOf(guard))

    fun lhg(guard: RuntimeRule, up: RuntimeRule) = lhg(setOf(guard), setOf(up))

    fun lhg(guard: Set<RuntimeRule>) {
        this._lhg.add(Lookahead(LookaheadSet.createFromRuntimeRules(this.stateSet, guard), LookaheadSet.EMPTY))
    }

    fun lhg(guard: Set<RuntimeRule>, up: Set<RuntimeRule>) {
        this._lhg.add(
            Lookahead(
                LookaheadSet.createFromRuntimeRules(this.stateSet, guard),
                LookaheadSet.createFromRuntimeRules(this.stateSet, up)
            )
        )
    }

    /**
     * graft prev guard
     */
    fun gpg(runtimeGuard: Set<RulePosition>?) {

    }

    /**
     * graft prev guard
     */
    fun gpg(runtimeRule: RuntimeRule, option: Int, position: Int) {
        val set = setOf(RulePosition(runtimeRule, option, position))
        this.gpg(set)
    }

    internal fun build(): Transition {
        val trans = Transition(_src as ParserState, _tgt as ParserState, action, _lhg)
        (_src as ParserState).outTransitions.addTransition(_context as Set<ParserState>, trans)
        return trans
    }
}