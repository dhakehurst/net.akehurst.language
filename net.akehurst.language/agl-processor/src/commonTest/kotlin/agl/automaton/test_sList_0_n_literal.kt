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

import net.akehurst.language.agl.runtime.structure.LookaheadSet
import net.akehurst.language.agl.runtime.structure.RulePosition
import net.akehurst.language.agl.runtime.structure.RuntimeRule
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import kotlin.test.Test
import kotlin.test.assertEquals

class test_sList_0_n_literal : test_Abstract() {
    // S =  ['a' / ',']* ;

    private companion object {
        val rrs = runtimeRuleSet {
            sList("S", 0, -1, "'a'", "','")
            literal("'a'", "a")
            literal("','", ",")
        }
        val S = rrs.findRuntimeRule("S")
        val a = rrs.findRuntimeRule("'a'")
        val _c = rrs.findRuntimeRule("','")
        val G = rrs.startingState(S).runtimeRules.first()

        val SM = rrs.fetchStateSetFor(S)
        val s0 = SM.startState

        val lhs_a = LookaheadSet(1, setOf(a))
    }

    override val SM: ParserStateSet get() = Companion.SM

    override val firstOf_data: List<Triple<RulePosition, LookaheadSet, Set<RuntimeRule>>>
        get() = TODO("not implemented")

    override val s0_widthInto_expected: List<WidthInfo>
        get() = listOf(
            WidthInfo(RulePosition(a, 0, 0), lhs_T)
        )

    @Test
    fun s0_transitions() {
        val actual = s0.transitions(null)
        val s1 = s0.stateSet.fetch(listOf(RulePosition(a, 0, RulePosition.END_OF_RULE)))

        val expected = listOf(
                Transition(s0, s1, Transition.ParseAction.WIDTH, lhs_T, LookaheadSet.EMPTY, null) { _, _ -> true },
                Transition(s0, s1, Transition.ParseAction.WIDTH, lhs_a, LookaheadSet.EMPTY, null) { _, _ -> true }
        ).toList()
        assertEquals(expected.size, actual.size)
        for (i in actual.indices) {
            assertEquals(expected[i], actual[i])
        }
    }

}