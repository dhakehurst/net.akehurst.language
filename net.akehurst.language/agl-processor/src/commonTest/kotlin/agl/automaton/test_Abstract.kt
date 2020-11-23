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
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
import kotlin.test.Test
import kotlin.test.assertEquals

abstract class test_Abstract {

    companion object {
        val EOT = RuntimeRuleSet.END_OF_TEXT
        val UP = RuntimeRuleSet.USE_PARENT_LOOKAHEAD

        val EOR = RulePosition.END_OF_RULE
        val SOR = RulePosition.START_OF_RULE
        val OMI = RulePosition.OPTION_MULTI_ITEM
        val OME = RulePosition.OPTION_MULTI_EMPTY
        val PMI = RulePosition.MULIT_ITEM_POSITION

        val lhs_E = LookaheadSet.EMPTY
        val lhs_U = LookaheadSet.UP
        val lhs_T = LookaheadSet.EOT

        fun RP(rr: RuntimeRule, opt: Int, pos: Int): RulePosition = RulePosition(rr, opt, pos)
    }

    abstract val SM: ParserStateSet
    abstract val firstOf_data: List<Triple<RulePosition, LookaheadSet, Set<RuntimeRule>>>

    @Test
    fun firstOf() {

        for (t in firstOf_data) {
            val rp = t.first
            val lhs = t.second
            val expected = t.third

            val actual = SM.firstOf(rp, lhs.content)

            assertEquals(expected, actual, "failed $rp")
        }
    }

    abstract val s0_widthInto_expected: List<Pair<RulePosition, LookaheadSet>>

    @Test
    fun s0_widthInto() {
        val s0 = SM.startState
        val actual = s0.widthInto(null).toList()

        val expected = s0_widthInto_expected
        assertEquals(expected.size, actual.size)
        for (i in 0 until actual.size) {
            assertEquals(expected[i], actual[i])
        }
    }
}