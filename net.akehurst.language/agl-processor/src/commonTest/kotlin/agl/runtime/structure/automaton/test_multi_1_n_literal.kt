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

class test_multi_1_n_literal {

    companion object {
        // S =  'a'+ ;
        val rrs = runtimeRuleSet {
            multi("S",1,-1,"'a'")
            literal("'a'","a")
        }
        val S = rrs.findRuntimeRule("S")
        val a = rrs.findRuntimeRule("'a'")
        val G = rrs.startingState(S).runtimeRule

        val s0 = rrs.startingState(S)

        val lhsE = LookaheadSet.EMPTY
        val lhs0 = LookaheadSet(0, setOf(rrs.END_OF_TEXT))
        val lhs1 = LookaheadSet(1, setOf(a))
    }

    @Test
    fun s0_calcClosureLR0() {

        val actual1 = s0.calcClosureLR1(lhsE)
        val actual = s0.calcClosureLR0_1().toList()

        val cl_G = ClosureItemWithLookaheadList(null, RulePosition(G, 0, 0), listOf(lhs0))
        val cl_G_S = ClosureItemWithLookaheadList(cl_G, RulePosition(S, 0, 0), listOf(lhs0, lhs1))
        val cl_G_S_a = ClosureItemWithLookaheadList(cl_G_S, RulePosition(a, 0, 0), listOf(lhs0, lhs1))
        val cl_G_Sb = ClosureItemWithLookaheadList(cl_G, RulePosition(S, 0, 0), listOf(lhs0))
        val cl_G_Sb_a = ClosureItemWithLookaheadList(cl_G_Sb, RulePosition(a, 0, 0), listOf(lhs0))

        val expected = listOf(
                cl_G, cl_G_S, cl_G_S_a, cl_G_Sb, cl_G_Sb_a
        )
        assertEquals(expected, actual)
    }


}