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

import net.akehurst.language.agl.runtime.structure.*
import kotlin.test.Test
import kotlin.test.assertEquals

class test_leftRecursive : test_Abstract() {

    // S =  'a' | S1
    // S1 = S 'a'

    private companion object {

        val rrs = runtimeRuleSet {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                literal("a")
                ref("S1")
            }
            concatenation("S1") { ref("S"); literal("a") }
        }

        val S = rrs.findRuntimeRule("S")
        val SM = rrs.fetchStateSetFor(S)
        val G = SM.startState.runtimeRules.first()
        val S1 = rrs.findRuntimeRule("S1")
        val a = rrs.findRuntimeRule("'a'")

        val s0 = SM.startState
        val s1 = SM.states[listOf(RP(a, 0, EOR))]
        val s2 = SM.states[listOf(RP(S, 0, EOR))]
        val s3 = SM.states[listOf(RP(S1, 0, EOR))]
        val s4 = SM.states[listOf(RP(S1, 0, 1))]
        val s5 = SM.states[listOf(RP(G, 0, EOR))]
        val s6 = SM.states[listOf(RP(S, 1, EOR))]

        val lhs_a = SM.runtimeRuleSet.createLookaheadSet(setOf(a))
        val lhs_aU = SM.runtimeRuleSet.createLookaheadSet(setOf(a, UP))
    }

    override val SM: ParserStateSet
        get() = Companion.SM

    override val firstOf_data: List<Triple<RulePosition, LookaheadSet, Set<RuntimeRule>>>
        get() = listOf(
                Triple(RP(G, 0, SOR), lhs_U, setOf(a)), // G = . S
                Triple(RP(G, 0, EOR), lhs_U, setOf(UP)), // G = S .
                Triple(RP(S, 0, SOR), lhs_U, setOf(a)), // S = . a
                Triple(RP(S, 0, EOR), lhs_U, setOf(UP)), // S = a .
                Triple(RP(S1, 0, SOR), lhs_U, setOf(a)), // S1 = . S a
                Triple(RP(S1, 1, SOR), lhs_U, setOf(a)), // S1 = S . a
                Triple(RP(S1, 0, EOR), lhs_U, setOf(UP)) // S1 = S a .
        )

    @Test
    fun calcClosure_G_0_0() {
        val cl_G = ClosureItem(null, RP(G, 0, 0), null, lhs_U)
        val cl_G_So0 = ClosureItem(cl_G, RP(S, 0, 0), RP(S, 0, EOR), lhs_U)
        val cl_G_So1 = ClosureItem(cl_G, RP(S, 1, 0), RP(S, 1, EOR), lhs_U)
        val cl_G_So1_S1 = ClosureItem(cl_G_So1, RP(S1, 0, 0), RP(S1, 0, 1), lhs_a)
        val cl_G_So1_S1_So0 = ClosureItem(cl_G_So1_S1, RP(S, 0, 0), RP(S, 0, EOR), lhs_a)
        val cl_G_So1_S1_So1 = ClosureItem(cl_G_So1_S1, RP(S, 1, 0), RP(S, 1, EOR), lhs_a)
        val cl_G_So1_S1_So1_S1 = ClosureItem(cl_G_So1_S1_So1, RP(S1, 0, 0), RP(S1, 0, 1), lhs_a)

        val actual = SM.buildCache.calcClosure(RP(G, 0, 0), lhs_U)
        val expected = setOf(
                cl_G, cl_G_So0, cl_G_So1, cl_G_So1_S1, cl_G_So1_S1_So0, cl_G_So1_S1_So1, cl_G_So1_S1_So1_S1
        )
        assertEquals(expected, actual)
    }

    override val s0_widthInto_expected: List<WidthInfo>
        get() = listOf(
            WidthInfo(RP(a, 0, EOR), lhs_aU)
        )

    @Test
    fun s0_transitions() {
        val actual = s0.transitions(null)

        val expected = listOf(
                Transition(s0, s1, Transition.ParseAction.WIDTH, lhs_aU, LookaheadSet.EMPTY, null) { _, _ -> true }
        ).toList()
        assertEquals(expected.size, actual.size)
        for (i in actual.indices) {
            assertEquals(expected[i], actual[i])
        }
    }

    @Test
    fun s1_heightOrGraftInto_s0() {

        val actual = s1.heightOrGraftInto(s0).toList()

        val expected = listOf(
                HeightGraftInfo(
                    listOf(RP(S, 0, 0)),
                    listOf(RP(S, 0, EOR)),
                    lhs_aU,
                    lhs_U
                )
        )
        assertEquals(expected, actual)

    }

    @Test
    fun s1_transitions_s0() {
        val actual = s1.transitions(s0)

        val expected = listOf<Transition>(
                Transition(s1, s2, Transition.ParseAction.HEIGHT, lhs_aU, LookaheadSet.UP, null) { _, _ -> true }
//                Transition(s1, s3, Transition.ParseAction.GRAFT, lhs_aU, null) { _, _ -> true }
        )
        assertEquals(expected.size, actual.size)
        for (i in actual.indices) {
            assertEquals(expected[i], actual[i])
        }
    }

    @Test
    fun s2_heightOrGraftInto_s0() {
        val actual = s2.heightOrGraftInto(s0)

        val expected = setOf(
                HeightGraftInfo(
                    listOf(RP(G, 0, 0)),
                    listOf(RP(G, 0, EOR)),
                    lhs_U,
                    lhs_U
                ),
                HeightGraftInfo(
                    listOf(RP(S1, 0, 0)),
                    listOf(RP(S1, 0, 1)),
                    lhs_a,
                    lhs_aU
                )
        )
        assertEquals(expected, actual)
    }

    @Test
    fun s2_transitions_s0() {

        val actual = s2.transitions(s0)

        val expected = listOf<Transition>(
                Transition(s2, s4, Transition.ParseAction.HEIGHT, lhs_a, lhs_a, null) { _, _ -> true },
                Transition(s2, s5, Transition.ParseAction.GRAFT, lhs_U, lhs_U, null) { _, _ -> true }
        )
        assertEquals(expected, actual)
    }

    @Test
    fun s4_widthInto_s0() {
        // s4 | S1 = S . a
        val actual = s4.widthInto(s0).toList()

        val expected = listOf(
            WidthInfo(RP(a, 0, EOR), lhs_aU)
        )
        assertEquals(expected, actual)

    }

    @Test
    fun s4_transitions_s0() {

        val actual = s4.transitions(s0)
        val expected = listOf<Transition>(
                Transition(s4, s1, Transition.ParseAction.WIDTH, lhs_aU, lhs_aU, null) { _, _ -> true }
        )
        assertEquals(expected.size, actual.size)
        for (i in actual.indices) {
            assertEquals(expected[i], actual[i])
        }
    }


    @Test
    fun s1_heightOrGraftInto_s4() {

        val actual = s1.heightOrGraftInto(s4).toList()

        val expected = listOf(
                HeightGraftInfo(
                    listOf(RP(S1, 0, 1)),
                    listOf(RP(S1, 0, EOR)),
                    lhs_aU,
                    lhs_U
                )
        )
        assertEquals(expected, actual)

    }

    @Test
    fun s1_transitions_s4() {

        val actual = s1.transitions(s4)
        val expected = listOf<Transition>(
                Transition(s1, s3, Transition.ParseAction.GRAFT, lhs_aU, lhs_aU, null) { _, _ -> true }
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
                Transition(s3, s6, Transition.ParseAction.HEIGHT, lhs_aU, lhs_aU, null) { _, _ -> true }
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
                Transition(s6, s4, Transition.ParseAction.HEIGHT, lhs_a, lhs_a, null) { _, _ -> true },
                Transition(s6, s5, Transition.ParseAction.GRAFT, lhs_U, lhs_U, null) { _, _ -> true }
        )
        assertEquals(expected.size, actual.size)
        for (i in actual.indices) {
            assertEquals(expected[i], actual[i])
        }
    }
}