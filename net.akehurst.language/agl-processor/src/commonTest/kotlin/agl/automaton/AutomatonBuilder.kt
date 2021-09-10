package agl.automaton

import net.akehurst.language.agl.automaton.ParserState
import net.akehurst.language.agl.automaton.ParserStateSet
import net.akehurst.language.agl.automaton.Transition
import net.akehurst.language.agl.runtime.structure.RulePosition
import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.api.processor.AutomatonKind

@DslMarker
internal annotation class AglAutomatonDslMarker

internal fun automaton(rrs: RuntimeRuleSet, automatonKind: AutomatonKind, userGoalRule: String, isSkip: Boolean, init: AutomatonBuilder.() -> Unit): ParserStateSet {
    val b = AutomatonBuilder(rrs, automatonKind, userGoalRule, isSkip)
    b.init()
    return b.build()
}

@AglAutomatonDslMarker
internal class AutomatonBuilder(
    rrs: RuntimeRuleSet,
    automatonKind: AutomatonKind,
    userGoalRule: String,
    isSkip: Boolean
) {

    private val result = ParserStateSet(-1, rrs, rrs.findRuntimeRule(userGoalRule), isSkip, automatonKind)
    private var nextState = 0

    val GOAL = Transition.ParseAction.GOAL
    val WIDTH = Transition.ParseAction.WIDTH
    val HEIGHT = Transition.ParseAction.HEIGHT
    val GRAFT = Transition.ParseAction.GRAFT
    val GRAFT_OR_HEIGHT = Transition.ParseAction.GRAFT_OR_HEIGHT

    fun state(vararg rulePositions: RulePosition): ParserState {
        return result.states[rulePositions.toList()]
    }

    fun transition(
        previousState: ParserState?,
        from: ParserState,
        to: ParserState,
        action: Transition.ParseAction,
        lookaheadGuardContent: Set<RuntimeRule>,
        upLookaheadContent: Set<RuntimeRule>,
        prevGuard: List<RulePosition>?
    ): Transition = transition(listOf(previousState),from, to, action, lookaheadGuardContent, upLookaheadContent, prevGuard)

    fun transition(
        previousStates: List<ParserState?>,
        from: ParserState,
        to: ParserState,
        action: Transition.ParseAction,
        lookaheadGuardContent: Set<RuntimeRule>,
        upLookaheadContent: Set<RuntimeRule>,
        prevGuard: List<RulePosition>?
    ): Transition {
        val lookaheadGuard = result.createLookaheadSet(lookaheadGuardContent)
        val upLookahead = result.createLookaheadSet(upLookaheadContent)
        val trans = Transition(from, to, action, lookaheadGuard, upLookahead, prevGuard) { _, _ -> true }
        return from.outTransitions.addTransition(previousStates, trans)
    }

    fun build(): ParserStateSet {
        return result
    }
}