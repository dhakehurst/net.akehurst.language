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
        // S = CC ;
        // C = cC | d ;
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
    }

    @Test
    fun s0_closureLR0() {
        //given
        val s0 = rrs.startingState(S)

        //when
        val actual = s0.calcClosureLR0().toList()

        //then
        val lhs0 = LookaheadSet(0, s0, setOf(RuntimeRuleSet.END_OF_TEXT))
        val lhs1 = LookaheadSet(1, s0, setOf(dT, cT))
        val expected = setOf<ClosureItemWithLookaheadList>(
                ClosureItemWithLookaheadList(RulePosition(G, 0, 0), listOf(lhs0)),
                ClosureItemWithLookaheadList(RulePosition(S, 0, 0), listOf(lhs0, lhs1)),
                ClosureItemWithLookaheadList(RulePosition(C, 0, 0), listOf(lhs0, lhs1, LookaheadSet.EMPTY)),
                ClosureItemWithLookaheadList(RulePosition(C1, 0, 0), listOf(lhs0, lhs1, LookaheadSet.EMPTY, lhs1)),
                ClosureItemWithLookaheadList(RulePosition(cT, 0, 0), listOf(lhs0, lhs1, LookaheadSet.EMPTY, lhs1, LookaheadSet.EMPTY)),
                ClosureItemWithLookaheadList(RulePosition(C, 1, 0), listOf(lhs0, lhs1, LookaheadSet.EMPTY)),
                ClosureItemWithLookaheadList(RulePosition(dT, 0, 0), listOf(lhs0, lhs1, LookaheadSet.EMPTY, LookaheadSet.EMPTY))
        ).toList()

        assertEquals(expected.size, actual.size)
        for (i in actual.indices) {
            assertEquals(expected[i], actual[i])
        }
    }
}