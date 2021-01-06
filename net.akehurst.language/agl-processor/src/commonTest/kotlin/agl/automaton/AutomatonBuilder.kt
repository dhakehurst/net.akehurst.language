package agl.automaton

import net.akehurst.language.agl.automaton.ParserState
import net.akehurst.language.agl.automaton.ParserStateSet
import net.akehurst.language.agl.automaton.Transition
import net.akehurst.language.agl.runtime.structure.RulePosition
import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet

@DslMarker
annotation class AglAutomatonDslMarker

fun automaton(rrs: RuntimeRuleSet, userGoalRule: String, isSkip: Boolean, init: AutomatonBuilder.() -> Unit): ParserStateSet {
    val b = AutomatonBuilder(rrs, userGoalRule, isSkip)
    b.init()
    return b.build()
}

@AglAutomatonDslMarker
class AutomatonBuilder(
        rrs: RuntimeRuleSet,
        userGoalRule: String,
        isSkip: Boolean
) {

    private val result = ParserStateSet(-1, rrs, rrs.findRuntimeRule(userGoalRule), isSkip)
    private var nextState = 0

    val GOAL = Transition.ParseAction.GOAL
    val WIDTH = Transition.ParseAction.WIDTH
    val HEIGHT = Transition.ParseAction.HEIGHT
    val GRAFT = Transition.ParseAction.GRAFT
    val GRAFT_OR_HEIGHT = Transition.ParseAction.GRAFT_OR_HEIGHT

    fun state(vararg rulePositions: RulePosition): ParserState {
        return result.states[rulePositions.toList()]
    }

    fun transition(previousState: ParserState?, from: ParserState, to: ParserState, action: Transition.ParseAction, lookaheadGuardContent: Set<RuntimeRule>, upLookaheadContent: Set<RuntimeRule>, prevGuard: List<RulePosition>?): Transition {
        val lookaheadGuard = result.runtimeRuleSet.createLookaheadSet(lookaheadGuardContent)
        val upLookahead = result.runtimeRuleSet.createLookaheadSet(upLookaheadContent)
        val trans = Transition(from, to, action, lookaheadGuard, upLookahead, prevGuard) { _, _ -> true }
        return from.addTransition(previousState, trans)
    }

    fun build(): ParserStateSet {
        return result
    }
}