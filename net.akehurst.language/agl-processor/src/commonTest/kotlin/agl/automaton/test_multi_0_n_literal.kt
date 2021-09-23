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
import net.akehurst.language.api.processor.AutomatonKind
import kotlin.test.Test
import kotlin.test.assertEquals

internal class test_multi_0_n_literal : test_AutomatonAbstract() {

    // S =  'a'* ;

    private companion object {
        val rrs = runtimeRuleSet {
            multi("S", 0, -1, "'a'")
            literal("'a'", "a")
        }

        val S = rrs.findRuntimeRule("S")
        val SM = rrs.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        val G = SM.startState.runtimeRules.first()
        val eS = S.rhs.items[RuntimeRuleItem.MULTI__EMPTY_RULE]
        val a = rrs.findRuntimeRule("'a'")

        val s0 = SM.startState
        val s1 = SM.states[listOf(RulePosition(a, 0, RulePosition.END_OF_RULE))]
        val s2 = SM.states[listOf(RulePosition(eS, 0, RulePosition.END_OF_RULE))]
        val s3 = SM.states[listOf(RulePosition(S, 0, RulePosition.POSITION_MULIT_ITEM))]
        val s4 = SM.states[listOf(RulePosition(S, 0, RulePosition.END_OF_RULE))]

        val lhs_a = SM.createLookaheadSet(setOf(a))
        val lhs_aU = SM.createLookaheadSet(setOf(a, UP))
        val lhs_aT = SM.createLookaheadSet(setOf(a, RuntimeRuleSet.END_OF_TEXT))
    }

    @Test
    override fun firstOf() {
        listOf(
            Triple(RP(G, 0, SOR), lhs_U, setOf(a, UP)), // G = . S
            Triple(RP(G, 0, EOR), lhs_U, setOf(UP)), // G = S .
            Triple(RP(S, 1, SOR), lhs_U, setOf(UP)), // S = . (a.empty)
            Triple(RP(S, 1, EOR), lhs_U, setOf(UP)), // S = (a.empty) .
            Triple(RP(S, 0, SOR), lhs_a, setOf(a)), // S = . a*
            Triple(RP(S, 0, PMI), lhs_a, setOf(a)), // S = a . a*
            Triple(RP(S, 0, EOR), lhs_U, setOf(UP)) // S = a* .
        ).testAll { rp, lhs, expected ->
            val actual = SM.buildCache.firstOf(rp, lhs)
            assertEquals(expected, actual, "failed $rp")
        }
    }

    @Test
    override fun s0_widthInto() {
        val s0 = SM.startState
        val actual = s0.widthInto(null).toList()

        val expected = listOf<WidthInfo>()
        assertEquals(expected.size, actual.size)
        for (i in 0 until actual.size) {
            assertEquals(expected[i], actual[i])
        }
    }

    @Test
    fun s0_transitions() {
        val actual = s0.transitions(null)

        val expected = listOf(
                Transition(s0, s1, Transition.ParseAction.WIDTH, lhs_aU, LookaheadSet.EMPTY, null) { _, _ -> true },
                Transition(s0, s2, Transition.ParseAction.WIDTH, lhs_U, LookaheadSet.EMPTY, null) { _, _ -> true }
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
                HeightGraftInfo(listOf(RP(S, 0, SOR)), listOf(RP(S, OMI, PMI)), lhs_a, lhs_U),
                HeightGraftInfo(listOf(RP(S, 0, SOR)), listOf(RP(S, OMI, EOR)), lhs_U, lhs_U)
        )
        assertEquals(expected, actual)

    }

    @Test
    fun s1_transitions_s0() {
        val actual = s1.transitions(s0)

        val expected = listOf<Transition>(
                Transition(s1, s3, Transition.ParseAction.HEIGHT, lhs_a, lhs_U, listOf(RP(S,OMI,SOR))) { _, _ -> true },
                Transition(s1, s4, Transition.ParseAction.HEIGHT, lhs_U, lhs_U, listOf(RP(S,OMI,SOR))) { _, _ -> true }
        )
        assertEquals(expected.size, actual.size)
        for (i in actual.indices) {
            assertEquals(expected[i], actual[i])
        }
    }

}