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

class test_leftRecursive {
    // S =  'a' | S1 ;
    // S1 = S 'a' ;
    companion object {

        val rrs = runtimeRuleSet {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                literal("a")
                ref("S1")
            }
            concatenation("S1") { ref("S"); literal("a") }
        }

        val S = rrs.findRuntimeRule("S")
        val SM = rrs.fetchStateSetFor(S)
        val G = SM.startState.runtimeRule
        val S1 = rrs.findRuntimeRule("S1")
        val a = rrs.findRuntimeRule("'a'")
        val EOT = RuntimeRuleSet.END_OF_TEXT
        val UP = RuntimeRuleSet.USE_PARENT_LOOKAHEAD

        val s0 = SM.startState
        val s1 = SM.states[RulePosition(a, 0, RulePosition.END_OF_RULE)]
        val s2 = SM.states[RulePosition(S, 0, RulePosition.END_OF_RULE)]
        val s3 = SM.states[RulePosition(S1, 0, RulePosition.END_OF_RULE)]
        val s4 = SM.states[RulePosition(S1, 0, 1)]
        val s5 = SM.states[RulePosition(G, 0, RulePosition.END_OF_RULE)]
        val s6 = SM.states[RulePosition(S, 1, RulePosition.END_OF_RULE)]

        val lhs_E = LookaheadSet.EMPTY
        val lhs_U = LookaheadSet.UP
        val lhs_T = LookaheadSet.EOT
        val lhs_a = SM.runtimeRuleSet.createLookaheadSet(setOf(a))
        val lhs_aU = SM.runtimeRuleSet.createLookaheadSet(setOf(a, UP))
    }

    @Test
    fun firstOf() {
        val rulePositions = listOf(
                Triple(RulePosition(G, 0, RulePosition.START_OF_RULE), lhs_U, setOf(a)), // G = . S
                Triple(RulePosition(G, 0, RulePosition.END_OF_RULE), lhs_U, setOf(UP)), // G = S .
                Triple(RulePosition(S, 0, RulePosition.START_OF_RULE), lhs_U, setOf(a)), // S = . a
                Triple(RulePosition(S, 0, RulePosition.END_OF_RULE), lhs_U, setOf(UP)), // S = a .
                Triple(RulePosition(S1, 0, RulePosition.START_OF_RULE), lhs_U, setOf(a)), // S1 = . S a
                Triple(RulePosition(S1, 1, RulePosition.START_OF_RULE), lhs_U, setOf(a)), // S1 = S . a
                Triple(RulePosition(S1, 0, RulePosition.END_OF_RULE), lhs_U, setOf(UP)) // S1 = S a .
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
    fun calcClosure() {
        val cl_G = ClosureItem(null, RulePosition(G, 0, 0), (lhs_U))
        val cl_G_So0 = ClosureItem(cl_G, RulePosition(S, 0, 0), (lhs_U))
        val cl_G_So1 = ClosureItem(cl_G, RulePosition(S, 1, 0), (lhs_U))
        val cl_G_So1_S1 = ClosureItem(cl_G_So1, RulePosition(S1, 0, 0), (lhs_a))
        val cl_G_So1_S1_So0 = ClosureItem(cl_G_So1_S1, RulePosition(S, 0, 0), (lhs_a))
        val cl_G_So1_S1_So1 = ClosureItem(cl_G_So1_S1, RulePosition(S, 1, 0), (lhs_a))

        val actual = SM.calcClosure(ClosureItem(null, RulePosition(G, 0, 0), lhs_U))
        val expected = setOf(
                cl_G, cl_G_So0, cl_G_So1, cl_G_So1_S1, cl_G_So1_S1_So0, cl_G_So1_S1_So1
        )
        assertEquals(expected, actual)
    }

    @Test
    fun s0_widthInto() {

        val actual = s0.widthInto(null).toList()

        val expected = listOf(
                Pair(RulePosition(a, 0, RulePosition.END_OF_RULE), lhs_aU)
        )
        assertEquals(expected, actual)

    }

    @Test
    fun s0_transitions() {
        val actual = s0.transitions(null)

        val expected = listOf(
                Transition(s0, s1, Transition.ParseAction.WIDTH, lhs_aU, null) { _, _ -> true }
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
                HeightGraft(RulePosition(S, 0, 0), RulePosition(S, 0, RulePosition.END_OF_RULE),lhs_aU)
        )
        assertEquals(expected, actual)

    }

    @Test
    fun s1_transitions_s0() {
        val actual = s1.transitions(s0)

        val expected = listOf<Transition>(
                Transition(s1, s2, Transition.ParseAction.HEIGHT, lhs_aU, null) { _, _ -> true }
//                Transition(s1, s3, Transition.ParseAction.GRAFT, lhs_aU, null) { _, _ -> true }
        )
        assertEquals(expected.size, actual.size)
        for (i in actual.indices) {
            assertEquals(expected[i], actual[i])
        }
    }

    @Test
    fun s2_heightOrGraftInto_s0() {
        val actual = s2.heightOrGraftInto(s0.rulePosition)

        val expected = setOf(
                HeightGraft(RulePosition(G, 0, 0), RulePosition(G, 0, RulePosition.END_OF_RULE),lhs_U),
                HeightGraft(RulePosition(S1, 0, 0), RulePosition(S1, 0, 1),lhs_a)
        )
        assertEquals(expected, actual)
    }

    @Test
    fun s2_transitions_s0() {

        val actual = s2.transitions(s0)

        val expected = listOf<Transition>(
                Transition(s2, s4, Transition.ParseAction.HEIGHT, lhs_a, null) { _, _ -> true },
                Transition(s2, s5, Transition.ParseAction.GRAFT, lhs_U, null) { _, _ -> true }
        )
        assertEquals(expected, actual)
    }

    @Test
    fun s4_widthInto_s0() {
        // s4 | S1 = S . a
        val actual = s4.widthInto(s0.rulePosition).toList()

        val expected = listOf(
                Pair(RulePosition(a, 0, RulePosition.END_OF_RULE), lhs_aU)
        )
        assertEquals(expected, actual)

    }

    @Test
    fun s4_transitions_s0() {

        val actual = s4.transitions(s0)
        val expected = listOf<Transition>(
                Transition(s4, s1, Transition.ParseAction.WIDTH, lhs_aU, null) { _, _ -> true }
        )
        assertEquals(expected.size, actual.size)
        for (i in actual.indices) {
            assertEquals(expected[i], actual[i])
        }
    }


    @Test
    fun s1_heightOrGraftInto_s4() {

        val actual = s1.heightOrGraftInto(s4.rulePosition).toList()

        val expected = listOf(
                HeightGraft(RulePosition(S1, 0, 1), RulePosition(S1, 0, RulePosition.END_OF_RULE),lhs_aU)
        )
        assertEquals(expected, actual)

    }

    @Test
    fun s1_transitions_s4() {

        val actual = s1.transitions(s4)
        val expected = listOf<Transition>(
                Transition(s1, s3, Transition.ParseAction.GRAFT, lhs_aU, null) { _, _ -> true }
        )
        assertEquals(expected.size, actual.size)
        for (i in actual.indices) {
            assertEquals(expected[i], actual[i])
        }
    }

    @Test
    fun s3_transitions_s0() {

        val actual = s3.transitions(s0)

        val expected = listOf<Transition>(
                Transition(s3, s6, Transition.ParseAction.HEIGHT, lhs_aU, null) { _, _ -> true }
        )
        assertEquals(expected.size, actual.size)
        for (i in actual.indices) {
            assertEquals(expected[i], actual[i])
        }
    }


    @Test
    fun s6_transitions_s0() {

        val actual = s6.transitions(s0)

        val expected = listOf<Transition>(
                Transition(s6, s4, Transition.ParseAction.HEIGHT, lhs_a, null) { _, _ -> true },
                Transition(s6, s5, Transition.ParseAction.GRAFT, lhs_U, null) { _, _ -> true }
        )
        assertEquals(expected.size, actual.size)
        for (i in actual.indices) {
            assertEquals(expected[i], actual[i])
        }
    }
}