package agl.automaton

import net.akehurst.language.agl.automaton.Lookahead
import net.akehurst.language.agl.automaton.ParserState
import net.akehurst.language.agl.automaton.ParserStateSet
import net.akehurst.language.agl.automaton.Transition
import kotlin.test.fail

internal object AutomatonTest {

    fun assertEquals(expected: ParserStateSet, actual: ParserStateSet) {
        val expected_states = expected.allBuiltStates
        val actual_states = actual.allBuiltStates

        val foundExpected = mutableListOf<ParserState>()
        val foundActual = mutableListOf<ParserState>()
        for (i in expected_states.indices) {
            val expected_state = expected_states[i]
            val actual_state = actual.fetchState(expected_state.rulePositions)
            if(null!=actual_state) {
                foundExpected.add(expected_state)
                foundActual.add(actual_state)
            }
        }
        when {
            expected_states.size > foundExpected.size   ->kotlin.test.fail( "States do not match, missing ${expected_states-foundExpected}")
            actual_states.size > foundActual.size   ->kotlin.test.fail( "States do not match, missing ${actual_states-foundActual}")
            else -> Unit
        }

        kotlin.test.assertEquals(expected.allBuiltTransitions.size, actual.allBuiltTransitions.size, "Number of Transitions do not match")
        for (exp in expected.allBuiltStates) {
            val act = actual.fetchState(exp.rulePositions) ?: fail("Actual state $exp not found")
            assertEquals(exp, act)
        }
    }

    fun assertEquals(expected: ParserState, actual: ParserState) {
        kotlin.test.assertEquals(expected.rulePositions.toSet(), actual.rulePositions.toSet(), "RulePositions do not match")

        kotlin.test.assertEquals(expected.outTransitions.allPrevious.size, actual.outTransitions.allPrevious.size, "Previous States for Transitions outgoing from ${expected} do not match")
        for(expected_prev in expected.outTransitions.allPrevious) {
            val expected_trs = expected.outTransitions.findTransitionByPrevious(expected_prev) ?: emptyList()
            val actual_prev = actual.stateSet.fetchState(expected_prev.rulePositions) ?: fail("actual State not found for: ${expected_prev}")
            val actual_trs = actual.outTransitions.findTransitionByPrevious(actual_prev)?: emptyList()
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
        kotlin.test.assertEquals(expected.from.rulePositions.toSet(), actual.from.rulePositions.toSet(), "From state does not match for ${expPrev} -> $expected")
        kotlin.test.assertEquals(expected.to.rulePositions.toSet(), actual.to.rulePositions.toSet(), "To state does not match for ${expPrev} -> $expected")
        kotlin.test.assertEquals(expected.action, actual.action, "Action does not match for ${expPrev} -> $expected")
        assertEquals(expected, expected.lookahead, actual.lookahead)//, "Lookahead content does not match for ${expPrev} -> $expected")
       //TODO kotlin.test.assertEquals(expected.upLookahead.includesUP, actual.upLookahead.includesUP, "Up lookahead content does not match for ${expPrev} -> $expected")
        kotlin.test.assertEquals(expected.prevGuard, actual.prevGuard, "Previous guard does not match for ${expPrev} -> $expected")

    }

    fun assertEquals(expTrans:Transition, expected:Set<Lookahead>, actual: Set<Lookahead>) {
        kotlin.test.assertEquals(expected.size, actual.size,"Lookahead does not match for ${expTrans}")
        val expSorted = expected.sortedBy { it.guard.fullContent.map { it.tag }.joinToString() }
        val actSorted = actual.sortedBy { it.guard.fullContent.map { it.tag }.joinToString() }
        for(i in expSorted.indices) {
            assertEquals(expTrans,expSorted[i],actSorted[i])
        }
    }

    fun assertEquals(expTrans:Transition, expected:Lookahead, actual: Lookahead) {
        kotlin.test.assertEquals(expected.guard.fullContent.map { it.tag }.toSet(), actual.guard.fullContent.map { it.tag }.toSet(), "Lookahead guard content does not match for ${expTrans}\n$expected")
        kotlin.test.assertEquals(expected.up.fullContent.map { it.tag }.toSet(), actual.up.fullContent.map { it.tag }.toSet(), "Lookahead up content does not match for ${expTrans}\n$expected")
    }
}