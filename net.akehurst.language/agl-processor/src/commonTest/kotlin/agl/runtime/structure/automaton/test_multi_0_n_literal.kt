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
            multi("S", 0, -1, "'a'")
            literal("'a'", "a")
        }
        val S = rrs.findRuntimeRule("S")
        val eS = S.rhs.items[RuntimeRuleItem.MULTI__EMPTY_RULE]
        val a = rrs.findRuntimeRule("'a'")
        val G = rrs.startingState(S, emptySet()).runtimeRule

        val s0 = rrs.startingState(S, emptySet())

        val lhs_E = LookaheadSet.EMPTY
        val lhs_T = LookaheadSet(1, setOf(rrs.END_OF_TEXT))
        val lhs_a = LookaheadSet(0, setOf(a))
        val lhs_aT = LookaheadSet(1, setOf(a, rrs.END_OF_TEXT))
    }

    @Test
    fun parentPosition() {

    }

    @Test
    fun fetchOrCreateFirstAt() {

    }

    fun lookahead() {

    }

    @Test
    fun s0_widthInto() {
    }

    @Test
    fun s0_transitions() {
        val actual = s0.transitions(null)
        val s1 = s0.stateSet.fetch(RulePosition(a, 0, RulePosition.END_OF_RULE))
        val s2 = s0.stateSet.fetch(RulePosition(eS,0,RulePosition.END_OF_RULE))

        val expected = listOf(
                Transition(s0, s1, Transition.ParseAction.WIDTH,  lhs_a, null) { _, _ -> true },
                Transition(s0, s2, Transition.ParseAction.WIDTH, lhs_a, null) { _, _ -> true }
        ).toList()
        assertEquals(expected.size, actual.size)
        for (i in actual.indices) {
            assertEquals(expected[i], actual[i])
        }
    }


}