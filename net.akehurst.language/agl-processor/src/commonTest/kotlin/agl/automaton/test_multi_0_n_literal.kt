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

class test_multi_0_n_literal : test_Abstract() {

    // S =  'a'* ;

    companion object {
        val rrs = runtimeRuleSet {
            multi("S", 0, -1, "'a'")
            literal("'a'", "a")
        }

        val S = rrs.findRuntimeRule("S")
        val SM = rrs.fetchStateSetFor(S)
        val G = SM.startState.runtimeRules.first()
        val eS = S.rhs.items[RuntimeRuleItem.MULTI__EMPTY_RULE]
        val a = rrs.findRuntimeRule("'a'")

        val s0 = SM.startState
        val s1 = SM.states[listOf(RulePosition(a, 0, RulePosition.END_OF_RULE))]
        val s2 = SM.states[listOf(RulePosition(eS, 0, RulePosition.END_OF_RULE))]
        val s3 = SM.states[listOf(RulePosition(S, 0, RulePosition.MULIT_ITEM_POSITION))]
        val s4 = SM.states[listOf(RulePosition(S, 0, RulePosition.END_OF_RULE))]

        val lhs_a = SM.runtimeRuleSet.createLookaheadSet(setOf(a))
        val lhs_aU = SM.runtimeRuleSet.createLookaheadSet(setOf(a, UP))
        val lhs_aT = SM.runtimeRuleSet.createLookaheadSet(setOf(a, RuntimeRuleSet.END_OF_TEXT))
    }

    override val SM: ParserStateSet
        get() = Companion.SM

    override val firstOf_data: List<Triple<RulePosition, LookaheadSet, Set<RuntimeRule>>>
        get() = listOf(
                Triple(RP(G, 0, SOR), lhs_U, setOf(a, UP)), // G = . S
                Triple(RP(G, 0, EOR), lhs_U, setOf(UP)), // G = S .
                Triple(RP(S, 1, SOR), lhs_U, setOf(UP)), // S = . (a.empty)
                Triple(RP(S, 1, EOR), lhs_U, setOf(UP)), // S = (a.empty) .
                Triple(RP(S, 0, SOR), lhs_a, setOf(a)), // S = . a*
                Triple(RP(S, 0, RulePosition.MULIT_ITEM_POSITION), lhs_a, setOf(a)), // S = a . a*
                Triple(RP(S, 0, EOR), lhs_U, setOf(UP)) // S = a* .
        )

    override val s0_widthInto_expected: List<Pair<RulePosition, LookaheadSet>>
        get() = TODO("not implemented")

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

        val actual = s1.heightOrGraftInto(s0.rulePositions).toList()

        val expected = listOf(
                HeightGraft(
                        RulePosition(test_leftRecursive.G, 0, 0),
                        listOf(RulePosition(S, 0, RulePosition.START_OF_RULE)),
                        listOf(RulePosition(S, 0, RulePosition.MULIT_ITEM_POSITION)),
                        lhs_a, lhs_U),
                HeightGraft(
                        RulePosition(test_leftRecursive.G, 0, 0),
                        listOf(RulePosition(S, 0, RulePosition.START_OF_RULE)),
                        listOf(RulePosition(S, 0, RulePosition.END_OF_RULE)),
                        lhs_U, lhs_U)
        )
        assertEquals(expected, actual)

    }

    @Test
    fun s1_transitions_s0() {
        val actual = s1.transitions(s0)

        val expected = listOf<Transition>(
                Transition(s1, s3, Transition.ParseAction.HEIGHT, lhs_a, LookaheadSet.EMPTY, null) { _, _ -> true },
                Transition(s1, s4, Transition.ParseAction.HEIGHT, lhs_U, LookaheadSet.EMPTY, null) { _, _ -> true }
        )
        assertEquals(expected.size, actual.size)
        for (i in actual.indices) {
            assertEquals(expected[i], actual[i])
        }
    }

}