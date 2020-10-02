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

class test_AhoSetiUlman_4_5_5 {

    companion object {
        // S = C C ;
        // C = c C | d ;
        //
        // S = C C ;
        // C = C1 | d ;
        // C1 = c C ;
        val rrs = runtimeRuleSet {
            concatenation("S") { ref("C"); ref("C") }
            choice("C", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("C1")
                literal("d")
            }
            concatenation("C1") { literal("c"); ref("C") }
        }
        val S = rrs.findRuntimeRule("S")
        val C = rrs.findRuntimeRule("C")
        val C1 = rrs.findRuntimeRule("C1")
        val cT = rrs.findRuntimeRule("'c'")
        val dT = rrs.findRuntimeRule("'d'")
        val G = rrs.startingState(S).runtimeRule

        val s0 = rrs.startingState(S)

        val lhs0 = LookaheadSet(0, setOf(RuntimeRuleSet.END_OF_TEXT))
        val lhs1 = LookaheadSet(1, setOf(dT, cT))
    }

    @Test
    fun s0_closureLR0() {
        //given

        //when
        val actual = s0.calcClosureLR0().toList()

        //then
        val cl_G = ClosureItemWithLookaheadList(null, RulePosition(G, 0, 0), listOf(lhs0))
        val cl_G_S = ClosureItemWithLookaheadList(cl_G, RulePosition(S, 0, 0), listOf(lhs0, lhs1))
        val cl_G_S_C0 = ClosureItemWithLookaheadList(cl_G_S, RulePosition(C, 0, 0), listOf(lhs0, lhs1))
        val cl_G_S_C0_C1 = ClosureItemWithLookaheadList(cl_G_S_C0, RulePosition(C1, 0, 0), listOf(lhs0, lhs1, lhs1))
        val cl_G_S_C0_C1_c = ClosureItemWithLookaheadList(cl_G_S_C0_C1, RulePosition(cT, 0, 0), listOf(lhs0, lhs1,  lhs1))
        val cl_G_S_C1 = ClosureItemWithLookaheadList(cl_G_S, RulePosition(C, 1, 0), listOf(lhs0, lhs1))
        val cl_G_S_C1_d = ClosureItemWithLookaheadList(cl_G_S_C1, RulePosition(dT, 0, 0), listOf(lhs0, lhs1))

        val expected = setOf<ClosureItemWithLookaheadList>(
                cl_G, cl_G_S, cl_G_S_C0, cl_G_S_C0_C1, cl_G_S_C0_C1_c,
                              cl_G_S_C1, cl_G_S_C1_d
        ).toList()

        assertEquals(expected.size, actual.size)
        for (i in actual.indices) {
            assertEquals(expected[i], actual[i])
        }
    }
}