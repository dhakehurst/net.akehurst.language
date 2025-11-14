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

package net.akehurst.language.automaton.leftcorner

import net.akehurst.language.agl.runtime.structure.RulePositionRuntime
import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.automaton.api.*
import net.akehurst.language.parser.api.OptionNum
import net.akehurst.language.parser.api.Rule
import net.akehurst.language.parser.api.RulePosition
import net.akehurst.language.parser.api.RuleSet

fun aut(rrs: RuleSet,
        automatonKind: AutomatonKind,
        userGoalRule: String,
        isSkip: Boolean,
        init: AutomatonBuilder.() -> Unit
):Automaton = automaton(rrs as RuntimeRuleSet, automatonKind, userGoalRule, isSkip, init)

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

    private val result = ParserStateSet(rrs.nextStateSetNumber++, rrs, rrs.findRuntimeRule(userGoalRuleName), isSkip, automatonKind, true)
    private var nextState = 0

    val GOAL = ParseAction.GOAL
    val WIDTH = ParseAction.WIDTH
    val HEIGHT = ParseAction.HEIGHT
    val GRAFT = ParseAction.GRAFT

    override fun state(ruleNumber: Int, option: OptionNum, position: Int) { //TODO: maybe use OptionNum in the signature?
        when {
            RuntimeRuleSet.GOAL_RULE_NUMBER == ruleNumber -> state(result.goalRule, option, position)
            else -> state(rrs.runtimeRules[ruleNumber],  option, position)
        }
    }

    override fun state(rp: RulePosition) {
        result.createState(listOf(rp as RulePositionRuntime))
    }
    internal fun state(rule: Rule) = state(rule, RulePosition.OPTION_NONE, RulePosition.END_OF_RULE)
    internal fun state(rule: Rule, option: OptionNum, position: Int) = result.createState(listOf(RulePositionRuntime(rule as RuntimeRule, option, position)))

    internal fun state(vararg rulePositions: RulePositionRuntime) = result.createState(rulePositions.toList())

    internal fun transition(
        previousState: ParserState,
        from: ParserState,
        to: ParserState,
        action: ParseAction,
        lookaheadGuardContent: Set<RuntimeRule>,
        upLookaheadContent: Set<Set<RuntimeRule>>,
        prevGuard: Set<RulePositionRuntime>?
    ) = transition(setOf(previousState), from, to, action, lookaheadGuardContent, upLookaheadContent, prevGuard)

    internal fun transition(
        previousStates: Set<ParserState>,
        from: ParserState,
        to: ParserState,
        action: ParseAction,
        lookaheadGuardContent: Set<RuntimeRule>,
        upLookaheadContent: Set<Set<RuntimeRule>>,
        prevGuard: Set<RulePositionRuntime>?
    ) {
        transition1(previousStates, from, to, action, lookaheadGuardContent, upLookaheadContent, prevGuard)
    }

    internal fun transition(
        previousState: ParserState,
        from: ParserState,
        to: ParserState,
        action: ParseAction,
        prevGuard: Set<RulePositionRuntime>?,
        init: TransitionBuilderDefault.() -> Unit
    ) = transition(setOf(previousState), from, to, action, prevGuard, init)

    internal fun transition(
        previousStates: Set<ParserState>,
        from: ParserState,
        to: ParserState,
        action: ParseAction,
        prevGuard: Set<RulePositionRuntime>?,
        init: TransitionBuilderDefault.() -> Unit
    ) {
        val b = TransitionBuilderDefault(result, action)
        b.ctx(previousStates)
        b.src(from)
        b.tgt(to)
        b.gpg(prevGuard)
        b.init()
        val trans = b.build()
        TODO()
        //from.outTransitions.addTransition(previousStates, trans)
    }

    override fun transition(action: ParseAction, init: TransitionBuilder.() -> Unit) {
        this.trans(action, init as (TransitionBuilderDefault.() -> Unit))
    }

    fun trans(action: ParseAction, init: TransitionBuilderDefault.() -> Unit) {
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
        prevGuard: Set<RulePositionRuntime>?
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
        TODO()
//        from.outTransitions.addTransition(previousStates, trans)
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

    override fun source(stateNumber: Int) = this.src(stateSet.allBuiltStates[stateNumber])
    override fun target(stateNumber: Int) = this.tgt(stateSet.allBuiltStates[stateNumber])

    fun ctx(states: Set<ParserState>) {
        _context = states
    }

    fun ctx(runtimeRule: RuntimeRule, option: OptionNum, position: Int) = ctx(RulePositionRuntime(runtimeRule, option, position))
    fun ctx(vararg rulePositions: RulePositionRuntime) {
        val states = rulePositions.map { this.stateSet.fetchState(listOf(it)) ?: error("State for $it not defined") }.toSet()
        this.ctx(states)
    }

    fun ctx(vararg rulePositions: Set<RulePositionRuntime>) {
        val states = rulePositions.map { this.stateSet.fetchState(it.toList()) ?: error("State for $it not defined") }.toSet()
        this.ctx(states)
    }

    fun src(runtimeRule: RuntimeRule) {
        check(this.action == ParseAction.HEIGHT || this.action == ParseAction.GRAFT || this.action == ParseAction.GOAL)
        src(runtimeRule, RulePosition.OPTION_NONE, RulePosition.END_OF_RULE)
    }

    fun src(runtimeRule: RuntimeRule, option: OptionNum, position: Int) = src(setOf(RulePositionRuntime(runtimeRule, option, position)))
    fun src(rulePositions: Set<RulePositionRuntime>) {
        val state = this.stateSet.fetchState(rulePositions.toList()) ?: error("State for $rulePositions not defined")
        this.src(state)
    }

    fun src(state: ParserState) {
        _src = state
    }

    fun tgt(runtimeRule: RuntimeRule) = tgt(runtimeRule, RulePosition.OPTION_NONE, RulePosition.END_OF_RULE)
    fun tgt(runtimeRule: RuntimeRule, option: OptionNum, position: Int) = tgt(setOf(RulePositionRuntime(runtimeRule, option, position)))
    fun tgt(rulePositions: Set<RulePositionRuntime>) {
        val state = this.stateSet.fetchState(rulePositions.toList()) ?: error("State for $rulePositions not defined")
        this.tgt(state)
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
    fun gpg(runtimeGuard: Set<RulePositionRuntime>?) {

    }

    /**
     * graft prev guard
     */
    fun gpg(runtimeRule: RuntimeRule, option: OptionNum, position: Int) {
        val set = setOf(RulePositionRuntime(runtimeRule, option, position))
        this.gpg(set)
    }

    internal fun build(): Transition {
        val trans = Transition(_src as ParserState, _tgt as ParserState, action, _lhg)
        TODO()
//        (_src as ParserState).outTransitions.addTransition(_context as Set<ParserState>, trans)
        //      return trans
    }
}