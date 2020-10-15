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

class test_sList_0_n_literal {

    companion object {
        // S =  ['a' / ',']* ;
        val rrs = runtimeRuleSet {
            sList("S",0,-1,"'a'", "','")
            literal("'a'","a")
            literal("','",",")
        }
        val S = rrs.findRuntimeRule("S")
        val a = rrs.findRuntimeRule("'a'")
        val _c = rrs.findRuntimeRule("','")
        val G = rrs.startingState(S).runtimeRule

        val s0 = rrs.startingState(S)

        val lhsE = LookaheadSet.EMPTY
        val lhs0 = LookaheadSet(0, setOf(rrs.END_OF_TEXT))
        val lhs1 = LookaheadSet(1, setOf(a))
    }

    @Test
    fun s0_widthInto() {

        val actual1 = test_leftRecursive.s0.widthInto2()
        val actual = test_leftRecursive.s0.widthInto4().toList()

        val cl_G = ClosureItemWithLookaheadList(null, RulePosition(test_leftRecursive.G, 0, 0), listOf(test_leftRecursive.lhs_T))
        val cl_G_S0 = ClosureItemWithLookaheadList(cl_G, RulePosition(test_leftRecursive.S, 0, 0), listOf(test_leftRecursive.lhs_T))
        val cl_G_S0_a = ClosureItemWithLookaheadList(cl_G_S0, RulePosition(test_leftRecursive.a, 0, 0), listOf(test_leftRecursive.lhs_T))
        val cl_G_S1 = ClosureItemWithLookaheadList(cl_G, RulePosition(test_leftRecursive.S, 1, 0), listOf(test_leftRecursive.lhs_T))
        val cl_G_S1_S1 = ClosureItemWithLookaheadList(cl_G_S1, RulePosition(test_leftRecursive.S1, 0, 0), listOf(test_leftRecursive.lhs_T, test_leftRecursive.lhs_a))
        val cl_G_S1_S1_S0 = ClosureItemWithLookaheadList(cl_G_S1_S1, RulePosition(test_leftRecursive.S, 0, 0), listOf(test_leftRecursive.lhs_T, test_leftRecursive.lhs_a))
        val cl_G_S1_S1_S0_a = ClosureItemWithLookaheadList(cl_G_S1_S1_S0, RulePosition(test_leftRecursive.a, 0, 0), listOf(test_leftRecursive.lhs_T, test_leftRecursive.lhs_a))

        val expected = listOf(
                RulePosition(test_leftRecursive.a, 0, 0)
        )
        assertEquals(expected.size, actual.size)
        for (i in 0 until actual.size) {
            assertEquals(expected[i], actual[i])
        }
    }

    @Test
    fun s0_transitions() {
        val actual = test_leftRecursive.s0.transitions(null)
        val s1 = test_leftRecursive.s0.stateSet.fetch(RulePosition(test_leftRecursive.a, 0, RulePosition.END_OF_RULE))

        val expected = listOf(
                Transition(test_leftRecursive.s0, s1, Transition.ParseAction.WIDTH, listOf(test_leftRecursive.lhs_T), test_leftRecursive.lhs_T, null) { _, _ -> true },
                Transition(test_leftRecursive.s0, s1, Transition.ParseAction.WIDTH, listOf(test_leftRecursive.lhs_T, test_leftRecursive.lhs_a), test_leftRecursive.lhs_a, null) { _, _ -> true }
        ).toList()
        assertEquals(expected.size, actual.size)
        for (i in actual.indices) {
            assertEquals(expected[i], actual[i])
        }
    }

}