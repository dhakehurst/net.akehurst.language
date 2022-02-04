package agl.automaton

import net.akehurst.language.agl.automaton.ParserState
import net.akehurst.language.agl.automaton.ParserStateSet
import net.akehurst.language.agl.automaton.Transition
import net.akehurst.language.agl.runtime.structure.LookaheadSet
import net.akehurst.language.agl.runtime.structure.RulePosition
import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import net.akehurst.language.api.processor.AutomatonKind

@DslMarker
internal annotation class AglAutomatonDslMarker

internal fun automaton(rrs: RuntimeRuleSet, automatonKind: AutomatonKind, userGoalRule: String, stateSetNumber:Int, isSkip: Boolean, init: AutomatonBuilder.() -> Unit): ParserStateSet {
    val b = AutomatonBuilder(rrs, automatonKind, userGoalRule, stateSetNumber, isSkip)
    b.init()
    return b.build()
}

@AglAutomatonDslMarker
internal class AutomatonBuilder(
    rrs: RuntimeRuleSet,
    automatonKind: AutomatonKind,
    userGoalRule: String,
    stateSetNumber:Int,
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
        return transition(previousStates,from,to,action,lookaheadGuardContent,setOf(upLookaheadContent),prevGuard)
    }
    fun transition(
        previousStates: List<ParserState?>,
        from: ParserState,
        to: ParserState,
        action: Transition.ParseAction,
        lookaheadGuardContent: Set<RuntimeRule>,
        upLookaheadContent: Set<Set<RuntimeRule>>,
        prevGuard: List<RulePosition>?
    ): Transition {
        val lookaheadGuard = result.createLookaheadSet(
            lookaheadGuardContent.contains(RuntimeRuleSet.USE_PARENT_LOOKAHEAD),
            lookaheadGuardContent.contains(RuntimeRuleSet.END_OF_TEXT),
            lookaheadGuardContent.contains(RuntimeRuleSet.ANY_LOOKAHEAD),
            lookaheadGuardContent.minus(RuntimeRuleSet.USE_PARENT_LOOKAHEAD).minus(RuntimeRuleSet.END_OF_TEXT).minus(RuntimeRuleSet.ANY_LOOKAHEAD)
        )
        val upLookahead = upLookaheadContent.map {
            result.createLookaheadSet(
                it.contains(RuntimeRuleSet.USE_PARENT_LOOKAHEAD),
                it.contains(RuntimeRuleSet.END_OF_TEXT),
                it.contains(RuntimeRuleSet.ANY_LOOKAHEAD),
                it.minus(RuntimeRuleSet.USE_PARENT_LOOKAHEAD).minus(RuntimeRuleSet.END_OF_TEXT).minus(RuntimeRuleSet.ANY_LOOKAHEAD)
            )
        }.toSet()
        val trans = Transition(from, to, action, lookaheadGuard, upLookahead, prevGuard) { _, _ -> true }
        return from.outTransitions.addTransition(previousStates, trans)
    }

    fun build(): ParserStateSet {
        return result
    }
}