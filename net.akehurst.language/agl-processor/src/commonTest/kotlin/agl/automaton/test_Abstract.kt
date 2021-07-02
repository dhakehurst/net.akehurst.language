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

import net.akehurst.language.agl.runtime.structure.LookaheadSet
import net.akehurst.language.agl.runtime.structure.RulePosition
import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import kotlin.test.Test
import kotlin.test.assertEquals

internal abstract class test_AutomatonUtilsAbstract {
    companion object {
        val EOT = RuntimeRuleSet.END_OF_TEXT
        val UP = RuntimeRuleSet.USE_PARENT_LOOKAHEAD
        val ANY = RuntimeRuleSet.ANY_LOOKAHEAD

        val EOR = RulePosition.END_OF_RULE
        val SOR = RulePosition.START_OF_RULE

        val OMI = RulePosition.OPTION_MULTI_ITEM
        val OME = RulePosition.OPTION_MULTI_EMPTY
        val OLE = RulePosition.OPTION_SLIST_EMPTY
        val OLI = RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR
        val OLS = RulePosition.OPTION_SLIST_ITEM_OR_SEPERATOR

        val PMI = RulePosition.POSITION_MULIT_ITEM
        val PLI = RulePosition.POSITION_SLIST_ITEM
        val PLS = RulePosition.POSITION_SLIST_SEPARATOR

        val WIDTH = Transition.ParseAction.WIDTH
        val HEIGHT = Transition.ParseAction.HEIGHT
        val GRAFT = Transition.ParseAction.GRAFT
        val GOAL = Transition.ParseAction.GOAL
        val GRAFT_OR_HEIGHT = Transition.ParseAction.GRAFT_OR_HEIGHT

        val lhs_E = LookaheadSet.EMPTY
        val lhs_U = LookaheadSet.UP
        val lhs_T = LookaheadSet.EOT

        fun RP(rr: RuntimeRule, opt: Int, pos: Int): RulePosition = RulePosition(rr, opt, pos)
    }

    fun assertEquals(expected: ParserStateSet, actual: ParserStateSet) {
        val expected_states = expected.states.values.map { it.rulePositions }.toSet()
        val actual_states = actual.states.values.map { it.rulePositions }.toSet()

        assertEquals(expected_states, actual_states, "States do not match")
/*
        for (i in expected_states.indices) {
            val expected_state = expected_states[i]
            val actual_state = actual_states[i]
            assertEquals(expected_state.rulePositions, actual_state.rulePositions, "RulePositions do not match")
        }
*/
        assertEquals(expected.allBuiltTransitions.size, actual.allBuiltTransitions.size, "Number of Transitions do not match")
        for (exp in expected.states.values) {
            val act = actual.states[exp.rulePositions]
            assertEquals(exp, act)
        }
    }

    fun assertEquals(expected: ParserState, actual: ParserState) {
        assertEquals(expected.rulePositions, actual.rulePositions, "RulePositions do not match")


        assertEquals(expected.outTransitions.allPrevious, actual.outTransitions.allPrevious,"Previous States for Transitions outgoing from ${expected} do not match")
        for(expected_prev in expected.outTransitions.allPrevious) {
            val expected_trs = expected.outTransitions.findTransitionByPrevious(expected_prev) ?: emptyList()
            val actual_trs = actual.outTransitions.findTransitionByPrevious(expected_prev)?: emptyList()
            assertEquals(expected_trs.size, actual_trs.size, "Number of Transitions outgoing from ${expected_prev} -> ${expected} do not match")
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

    fun assertEquals(expPrev:ParserState?, expected: Transition, actual: Transition) {
        assertEquals(expected.from.rulePositions, actual.from.rulePositions, "From state does not match for ${expPrev} -> $expected")
        assertEquals(expected.to.rulePositions, actual.to.rulePositions, "To state does not match for ${expPrev} -> $expected")
        assertEquals(expected.action, actual.action, "Action does not match for ${expPrev} -> $expected")
        assertEquals(expected.lookaheadGuard.content, actual.lookaheadGuard.content, "Lookahead content does not match for ${expPrev} -> $expected")
        assertEquals(expected.upLookahead.content, actual.upLookahead.content, "Up lookahead content does not match for ${expPrev} -> $expected")
        assertEquals(expected.prevGuard, actual.prevGuard, "Previous guard does not match for ${expPrev} -> $expected")
    }
}

internal abstract class test_Abstract : test_AutomatonUtilsAbstract() {

    abstract val SM: ParserStateSet
    abstract val firstOf_data: List<Triple<RulePosition, LookaheadSet, Set<RuntimeRule>>>

    @Test
    fun firstOf() {

        for (t in firstOf_data) {
            val rp = t.first
            val lhs = t.second
            val expected = t.third

            val actual = SM.buildCache.firstOf(rp, lhs)

            assertEquals(expected, actual, "failed $rp")
        }
    }

    abstract val s0_widthInto_expected: List<WidthInfo>

    @Test
    fun s0_widthInto() {
        val s0 = SM.startState
        val actual = s0.widthInto(null).toList()

        val expected = s0_widthInto_expected
        assertEquals(expected.size, actual.size)
        for (i in 0 until actual.size) {
            assertEquals(expected[i], actual[i])
        }
    }


}