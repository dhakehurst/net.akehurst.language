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

class test_ParserState_closureLR0_leftRecursive {

    companion object {
        // S =  'a' | S1 ;
        // S1 = S 'a' ;
        val rrs = runtimeRuleSet {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                literal("a")
                ref("S1")
            }
            concatenation("S1") { ref("S"); literal("a") }
        }
        val S = rrs.findRuntimeRule("S")
        val S1 = rrs.findRuntimeRule("S1")
        val a = rrs.findRuntimeRule("'a'")
        val G = rrs.startingState(S).runtimeRule
    }

    @Test
    fun s0_closureLR0() {
        val s0 = rrs.startingState(S)
        val actual = s0.calcClosureLR0().toList()

        val i0 = ClosureItemWithLookaheadList(null, RulePosition(G, 0, 0), emptyList())
        val i01 = ClosureItemWithLookaheadList(i0, RulePosition(S, 0, 0), emptyList())
        val i011 = ClosureItemWithLookaheadList(i01, RulePosition(a, 0, 0), emptyList())
        val i02 = ClosureItemWithLookaheadList(i0, RulePosition(S, 1, 0), emptyList())
        val i021 = ClosureItemWithLookaheadList(i02, RulePosition(S1, 0, 0), emptyList())

        val expected = setOf<ClosureItemWithLookaheadList>(
                i0, i01, i011,
                i02, i021
        ).toList()

        assertEquals(expected.size, actual.size)
        for (i in actual.indices) {
            assertEquals(expected[i], actual[i])
        }
    }

}