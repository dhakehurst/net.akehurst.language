package agl.automaton

import net.akehurst.language.agl.automaton.ParserState
import net.akehurst.language.agl.automaton.ParserStateSet
import net.akehurst.language.agl.automaton.Transition
import kotlin.test.fail

internal object AutomatonTest {

    fun assertEquals(expected: ParserStateSet, actual: ParserStateSet) {
        val expected_states = expected.allBuiltStates.map { it.rulePositions }.toSet()
        val actual_states = actual.allBuiltStates.map { it.rulePositions }.toSet()

        kotlin.test.assertEquals(expected_states, actual_states, "States do not match")
/*
        for (i in expected_states.indices) {
            val expected_state = expected_states[i]
            val actual_state = actual_states[i]
            assertEquals(expected_state.rulePositions, actual_state.rulePositions, "RulePositions do not match")
        }
*/
        kotlin.test.assertEquals(expected.allBuiltTransitions.size, actual.allBuiltTransitions.size, "Number of Transitions do not match")
        for (exp in expected.allBuiltStates) {
            val act = actual.fetchState(exp.rulePositions) ?: fail("Actual state $exp not found")
            assertEquals(exp, act)
        }
    }

    fun assertEquals(expected: ParserState, actual: ParserState) {
        kotlin.test.assertEquals(expected.rulePositions, actual.rulePositions, "RulePositions do not match")


        kotlin.test.assertEquals(expected.outTransitions.allPrevious, actual.outTransitions.allPrevious, "Previous States for Transitions outgoing from ${expected} do not match")
        for(expected_prev in expected.outTransitions.allPrevious) {
            val expected_trs = expected.outTransitions.findTransitionByPrevious(expected_prev) ?: emptyList()
            val actual_trs = actual.outTransitions.findTransitionByPrevious(expected_prev)?: emptyList()
            kotlin.test.assertEquals(expected_trs.size, actual_trs.size, "Number of Transitions outgoing from ${expected_prev} -> ${expected} do not match")
            for (i in expected_trs.indices) {
                assertEquals(expected_prev, expected_trs[i], actual_trs[i])
            }
        }
        /*
        val expected_trans = expected.outTransitions.transitionsByPrevious
        val actual_trans = actual.outTransitions.transitionsByPrevious
        assertEquals(expected_trans.keys.map { it?.rulePositions }, actual_trans.keys.map { it?.rulePositions }, "Previous States for Transitions outgoing from ${expected} do not match")
        assertEquals(expected_trans.size, actual_trans.size, "Number of Transitions outgoing from ${expected} do not match")

        for (entry in expected_trans.entries) {
            val actual_key = if (null==entry.key) null else actual.stateSet.states[entry.key!!.rulePositions]
            val expected_outgoing = expected_trans[entry.key] ?: emptyList()
            val actual_outgoing = actual_trans[actual_key] ?: emptyList()
            assertEquals(expected_outgoing.size, actual_outgoing.size, "Number of Transitions outgoing from ${entry.key} -> ${expected} do not match")

            for (i in expected_outgoing.indices) {
                assertEquals(entry.key, expected_outgoing[i], actual_outgoing[i])
            }
        }
         */
    }

    fun assertEquals(expPrev: ParserState?, expected: Transition, actual: Transition) {
        kotlin.test.assertEquals(expected.from.rulePositions, actual.from.rulePositions, "From state does not match for ${expPrev} -> $expected")
        kotlin.test.assertEquals(expected.to.rulePositions, actual.to.rulePositions, "To state does not match for ${expPrev} -> $expected")
        kotlin.test.assertEquals(expected.action, actual.action, "Action does not match for ${expPrev} -> $expected")
        kotlin.test.assertEquals(expected.lookaheadGuard.totalContent.map { it.tag }, actual.lookaheadGuard.totalContent.map { it.tag }, "Lookahead content does not match for ${expPrev} -> $expected")
       //TODO kotlin.test.assertEquals(expected.upLookahead.includesUP, actual.upLookahead.includesUP, "Up lookahead content does not match for ${expPrev} -> $expected")
        kotlin.test.assertEquals(expected.upLookahead, actual.upLookahead, "Up lookahead content does not match for ${expPrev} -> $expected")
        kotlin.test.assertEquals(expected.prevGuard, actual.prevGuard, "Previous guard does not match for ${expPrev} -> $expected")
    }

}