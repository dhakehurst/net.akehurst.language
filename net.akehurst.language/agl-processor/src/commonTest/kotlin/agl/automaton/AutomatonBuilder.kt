package agl.automaton

import net.akehurst.language.agl.automaton.*
import net.akehurst.language.agl.runtime.structure.RulePosition
import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.api.processor.AutomatonKind

@DslMarker
internal annotation class AglAutomatonDslMarker

internal fun automaton(
    rrs: RuntimeRuleSet,
    automatonKind: AutomatonKind,
    userGoalRule: String,
    stateSetNumber: Int,
    isSkip: Boolean,
    init: AutomatonBuilder.() -> Unit
): ParserStateSet {
    val b = AutomatonBuilder(rrs, automatonKind, userGoalRule, stateSetNumber, isSkip)
    b.init()
    return b.build()
}

@AglAutomatonDslMarker
internal class AutomatonBuilder(
    rrs: RuntimeRuleSet,
    automatonKind: AutomatonKind,
    userGoalRule: String,
    stateSetNumber: Int,
    isSkip: Boolean
) {

    private val result = ParserStateSet(stateSetNumber, rrs, rrs.findRuntimeRule(userGoalRule), isSkip, automatonKind)
    private var nextState = 0

    val GOAL = Transition.ParseAction.GOAL
    val WIDTH = Transition.ParseAction.WIDTH
    val HEIGHT = Transition.ParseAction.HEIGHT
    val GRAFT = Transition.ParseAction.GRAFT
    //   val GRAFT_OR_HEIGHT = Transition.ParseAction.GRAFT_OR_HEIGHT

    fun state(vararg rulePositions: RulePosition): ParserState {
        return result.createState(rulePositions.toList())
    }

    fun transition(
        previousState: ParserState,
        from: ParserState,
        to: ParserState,
        action: Transition.ParseAction,
        lookaheadGuardContent: Set<RuntimeRule>,
        upLookaheadContent: Set<Set<RuntimeRule>>,
        prevGuard: Set<RulePosition>?
    ): Transition = transition(setOf(previousState), from, to, action, lookaheadGuardContent, upLookaheadContent, prevGuard)

    fun transition(
        previousStates: Set<ParserState>,
        from: ParserState,
        to: ParserState,
        action: Transition.ParseAction,
        lookaheadGuardContent: Set<RuntimeRule>,
        upLookaheadContent: Set<Set<RuntimeRule>>,
        prevGuard: Set<RulePosition>?
    ): Transition {
        return transition1(previousStates, from, to, action, lookaheadGuardContent, upLookaheadContent, prevGuard)
    }

    fun transition(
        previousState: ParserState,
        from: ParserState,
        to: ParserState,
        action: Transition.ParseAction,
        prevGuard: Set<RulePosition>?,
        init: TransitionBuilder.() -> Unit
    ): Transition = transition(setOf(previousState), from, to, action, prevGuard, init)

    fun transition(
        previousStates: Set<ParserState>,
        from: ParserState,
        to: ParserState,
        action: Transition.ParseAction,
        prevGuard: Set<RulePosition>?,
        init: TransitionBuilder.() -> Unit
    ): Transition {
        val b = TransitionBuilder(result, action)
        b.ctx(previousStates)
        b.src(from)
        b.tgt(to)
        b.rtg(prevGuard)
        b.init()
        val trans = b.build()
        from.outTransitions.addTransition(previousStates, trans)
        return trans
    }

    fun transition(
        action: Transition.ParseAction,
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
        action: Transition.ParseAction,
        lookaheadGuardContent: Set<RuntimeRule>,
        upLookaheadContent: Set<Set<RuntimeRule>>,
        prevGuard: Set<RulePosition>?
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
        val trans = Transition(from, to, action, lh, prevGuard) { _, _ -> true }
        return from.outTransitions.addTransition(previousStates, trans)
    }

    fun build(): ParserStateSet {
        return result
    }
}

@AglAutomatonDslMarker
internal class TransitionBuilder(
    val stateSet: ParserStateSet,
    val action: Transition.ParseAction
) {

    private var _context = emptySet<ParserState>()
    private lateinit var _src: ParserState
    private lateinit var _tgt: ParserState
    private val _lhg = mutableSetOf<Lookahead>()
    private var _rtg: Set<RulePosition>? = null

    fun ctx(states: Set<ParserState>) {
        _context = states
    }

    fun ctx(vararg rulePositions: RulePosition) {
        val states = rulePositions.map{ this.stateSet.fetchOrCreateState(listOf(it)) }.toSet()
        this.ctx(states)
    }

    fun ctx(runtimeRule: RuntimeRule, option: Int, position: Int) {
        val state = this.stateSet.fetchOrCreateState(listOf(RulePosition(runtimeRule, option, position)))
        this.ctx(setOf(state))
    }

    fun src(runtimeRule: RuntimeRule) {
        check(this.action==Transition.ParseAction.HEIGHT || this.action==Transition.ParseAction.GRAFT)
        src(runtimeRule, 0, RulePosition.END_OF_RULE)
    }
    fun src(runtimeRule: RuntimeRule, option: Int, position: Int) {
        val state = this.stateSet.fetchOrCreateState(listOf(RulePosition(runtimeRule, option, position)))
        this.src(state)
    }

    fun tgt(runtimeRule: RuntimeRule) = tgt(runtimeRule, 0, RulePosition.END_OF_RULE)
    fun tgt(runtimeRule: RuntimeRule, option: Int, position: Int) {
        val state = this.stateSet.fetchOrCreateState(listOf(RulePosition(runtimeRule, option, position)))
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
        this._lhg.add(Lookahead(LookaheadSet.createFromRuntimeRules(this.stateSet, guard), LookaheadSet.createFromRuntimeRules(this.stateSet, up)))
    }

    /**
     * runtime guard (or prev guard)
     */
    fun rtg(runtimeGuard: Set<RulePosition>?) {
        _rtg = runtimeGuard
    }

    fun rtg(runtimeRule: RuntimeRule, option: Int, position: Int) {
        val set = setOf(RulePosition(runtimeRule, option, position))
        this.rtg(set)
    }

    fun build(): Transition {
        val trans = Transition(_src, _tgt, action, _lhg, _rtg) { _, _ -> true }
        _src.outTransitions.addTransition(_context, trans)
        return trans
    }
}