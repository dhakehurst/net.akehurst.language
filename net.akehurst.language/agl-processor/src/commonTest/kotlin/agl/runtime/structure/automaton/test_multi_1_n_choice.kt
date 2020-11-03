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

package net.akehurst.language.agl.runtime.structure

import kotlin.test.Test
import kotlin.test.assertEquals

class test_multi_1_n_choice {

    // S =  AB+
    // AB = a | b

    companion object {
        val rrs = runtimeRuleSet {
            multi("S",1,-1,"AB")
            choice("AB",RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                literal("a")
                literal("b")
            }
        }
        val S = rrs.findRuntimeRule("S")
        val SM = rrs.fetchStateSetFor(S)
        val G = SM.startState.runtimeRule
        val AB = rrs.findRuntimeRule("AB")
        val a = rrs.findRuntimeRule("'a'")
        val b = rrs.findRuntimeRule("'b'")
        val EOT = RuntimeRuleSet.END_OF_TEXT
        val UP = RuntimeRuleSet.USE_PARENT_LOOKAHEAD

        val s0 = SM.startState
        val s1 = SM.states[RulePosition(a, 0, RulePosition.END_OF_RULE)]
        val s2 = SM.states[RulePosition(b, 0, RulePosition.END_OF_RULE)]
        val s3 = SM.states[RulePosition(AB, 0, RulePosition.END_OF_RULE)]
        val s4 = SM.states[RulePosition(S, 0, RulePosition.END_OF_RULE)]

        val lhs_E = LookaheadSet.EMPTY
        val lhs_U = LookaheadSet.UP
        val lhs_T = LookaheadSet.EOT
        val lhs_a = SM.runtimeRuleSet.createLookaheadSet(setOf(a))
        val lhs_abU = SM.runtimeRuleSet.createLookaheadSet(setOf(a, b,UP))
        val lhs_aT = SM.runtimeRuleSet.createLookaheadSet(setOf(a, RuntimeRuleSet.END_OF_TEXT))
    }

    @Test
    fun firstOf() {
        val rulePositions = listOf(
                Triple(RulePosition(G, 0, RulePosition.START_OF_RULE), lhs_U, setOf(a,b)), // G = . S
                Triple(RulePosition(G, 0, RulePosition.END_OF_RULE), lhs_U, setOf(UP)), // G = S .
                Triple(RulePosition(S, 0, RulePosition.START_OF_RULE), lhs_a, setOf(a,b)), // S = . a+
                Triple(RulePosition(S, 0, RulePosition.MULIT_ITEM_POSITION), lhs_a, setOf(a,b)), // S = a . a+
                Triple(RulePosition(S, 0, RulePosition.END_OF_RULE), lhs_U, setOf(UP)) // S = a+ .
        )


        for (t in rulePositions) {
            val rp = t.first
            val lhs = t.second
            val expected = t.third

            val actual = SM.firstOf(rp, lhs.content)

            assertEquals(expected, actual, "failed $rp")
        }
    }

    @Test
    fun expectedAfter() {
        val rulePositions = listOf(
                Pair(RulePosition(G, 0, RulePosition.START_OF_RULE), setOf(a,b)), // G = . S
                Pair(RulePosition(G, 0, RulePosition.END_OF_RULE), setOf(UP)), // G = S .
                Pair(RulePosition(S, 0, RulePosition.START_OF_RULE), setOf(a,b)), // S = . a+
                Pair(RulePosition(S, 0, RulePosition.MULIT_ITEM_POSITION), setOf(a,b)), // S = a . a+
                Pair(RulePosition(S, 0, RulePosition.END_OF_RULE), setOf(UP)) // S = a+ .
        )


        for (t in rulePositions) {
            val rp = t.first
            val expected = t.second

            val actual = SM.expectedAfter(rp)

            assertEquals(expected, actual, "failed $rp")
        }
    }

    @Test
    fun s0_widthInto() {
        val actual = s0.widthInto(null)
        val expected = setOf(
                Pair(RulePosition(a, 0, RulePosition.END_OF_RULE), lhs_abU),
                Pair(RulePosition(b, 0, RulePosition.END_OF_RULE), lhs_abU)
        )
        assertEquals(expected, actual)
    }

    @Test
    fun s0_transitions() {
        val actual = s0.transitions(null)

        val expected = listOf(
                Transition(s0, s1, Transition.ParseAction.WIDTH, lhs_abU, LookaheadSet.EMPTY,null) { _, _ -> true },
                Transition(s0, s2, Transition.ParseAction.WIDTH, lhs_abU, LookaheadSet.EMPTY,null) { _, _ -> true }
        ).toList()
        assertEquals(expected.size, actual.size)
        for (i in actual.indices) {
            assertEquals(expected[i], actual[i])
        }
    }

    @Test
    fun s1_heightOrGraftInto_s0() {

        val actual = s1.heightOrGraftInto(s0.rulePosition).toList()

        val expected = listOf(
                HeightGraft(RulePosition(test_leftRecursive.G, 0, 0),RulePosition(AB, 0, 0), RulePosition(AB, 0, RulePosition.END_OF_RULE),lhs_abU, lhs_U),
                HeightGraft(RulePosition(test_leftRecursive.G, 0, 0),RulePosition(S, 0, 0), RulePosition(S, 0, RulePosition.END_OF_RULE),lhs_U, lhs_U)
        )
        assertEquals(expected, actual)

    }
}