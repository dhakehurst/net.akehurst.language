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

class test_multi_0_n_literal {

    companion object {
        // S =  'a'* ;
        val rrs = runtimeRuleSet {
            multi("S",0,-1,"'a'")
            literal("'a'","a")
        }
        val S = rrs.findRuntimeRule("S")
        val a = rrs.findRuntimeRule("'a'")
        val G = rrs.startingState(S).runtimeRule

        val s0 = rrs.startingState(S)

        val lhsE = LookaheadSet.EMPTY
        val lhs0 = LookaheadSet(0, setOf(RuntimeRuleSet.END_OF_TEXT))
        val lhs1 = LookaheadSet(1, setOf(a))
    }

    @Test
    fun s0_calcClosureLR0() {

        val actual1 = s0.calcClosureLR1(lhsE)
        val actual = s0.calcClosureLR0().toList()

        val cl_G = ClosureItemWithLookaheadList(null, RulePosition(G, 0, 0), listOf(lhs0))



        val expected = listOf(
                cl_G
        )
        assertEquals(expected, actual)
    }


}