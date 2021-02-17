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

class test_multi_1_n_literal : test_Abstract() {

    // S =  'a'+ ;

    private companion object {
        val rrs = runtimeRuleSet {
            multi("S", 1, -1, "'a'")
            literal("'a'", "a")
        }
        val S = rrs.findRuntimeRule("S")
        val SM = rrs.fetchStateSetFor(S)
        val G = SM.startState.runtimeRules.first()
        val a = rrs.findRuntimeRule("'a'")

        val s0 = SM.startState
        val s1 = SM.states[listOf(RulePosition(a, 0, RulePosition.END_OF_RULE))]
        val s2 = SM.states[listOf(RulePosition(S, 0, RulePosition.POSITION_MULIT_ITEM))]
        val s3 = SM.states[listOf(RulePosition(S, 0, RulePosition.END_OF_RULE))]

        val lhs_a = SM.runtimeRuleSet.createLookaheadSet(setOf(a))
        val lhs_aU = SM.runtimeRuleSet.createLookaheadSet(setOf(a, UP))
        val lhs_aT = SM.runtimeRuleSet.createLookaheadSet(setOf(a, RuntimeRuleSet.END_OF_TEXT))
    }

    override val SM: ParserStateSet
        get() = Companion.SM

    override val firstOf_data: List<Triple<RulePosition, LookaheadSet, Set<RuntimeRule>>>
        get() = listOf(
                Triple(RulePosition(G, 0, RulePosition.START_OF_RULE), lhs_U, setOf(a)), // G = . S
                Triple(RulePosition(G, 0, RulePosition.END_OF_RULE), lhs_U, setOf(UP)), // G = S .
                Triple(RulePosition(S, 0, RulePosition.START_OF_RULE), lhs_a, setOf(a)), // S = . a+
                Triple(RulePosition(S, 0, RulePosition.POSITION_MULIT_ITEM), lhs_a, setOf(a)), // S = a . a+
                Triple(RulePosition(S, 0, RulePosition.END_OF_RULE), lhs_U, setOf(UP)) // S = a+ .
        )

    override val s0_widthInto_expected: List<WidthInfo>
        get() = listOf(
            WidthInfo(RP(a,0,EOR), lhs_aU)
        )

    @Test
    fun s1_heightOrGraftInto_s0() {

        val actual = s1.heightOrGraftInto(s0).toList()

        val expected = listOf(
                HeightGraftInfo(
                    listOf(RulePosition(S, 0, 0)),
                    listOf(RulePosition(S, 0, PMI)),
                    lhs_a,
                    lhs_U
                ),
                HeightGraftInfo(
                    listOf(RulePosition(S, 0, 0)),
                    listOf(RulePosition(S, 0, EOR)),
                    lhs_U,
                    lhs_U
                )
        )
        assertEquals(expected, actual)

    }
}