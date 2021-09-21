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
import net.akehurst.language.api.processor.AutomatonKind
import kotlin.test.Test
import kotlin.test.assertEquals

internal class test_aObOcO : test_Abstract() {
    /*
        S = a? b? c?;
     */
    private companion object {

        val rrs = runtimeRuleSet {
            concatenation("S") { ref("aOpt"); ref("bOpt"); ref("cOpt") }
            multi("aOpt", 0, 1, "'a'")
            multi("bOpt", 0, 1, "'b'")
            multi("cOpt", 0, 1, "'c'")
            literal("'a'", "a")
            literal("'b'", "b")
            literal("'c'", "c")
        }

        val S = rrs.findRuntimeRule("S")
        val SM = rrs.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        val aOpt = rrs.findRuntimeRule("aOpt")
        val aOpt_E = aOpt.rhs.items[RuntimeRuleItem.MULTI__EMPTY_RULE]
        val bOpt = rrs.findRuntimeRule("bOpt")
        val cOpt = rrs.findRuntimeRule("cOpt")
        val a = rrs.findRuntimeRule("'a'")
        val b = rrs.findRuntimeRule("'b'")
        val c = rrs.findRuntimeRule("'c'")
        val G = SM.startState.runtimeRules.first()

        val s0 = SM.startState
        val s1 = SM.states[listOf(RulePosition(a, 0, RulePosition.END_OF_RULE))]
        val s2 = SM.states[listOf(RulePosition(aOpt_E, 0, RulePosition.END_OF_RULE))]

        val lhs_bcU = SM.createLookaheadSet(setOf(b, c, UP))
    }

    override val SM: ParserStateSet
        get() = Companion.SM

    override val firstOf_data: List<Triple<RulePosition, LookaheadSet, Set<RuntimeRule>>>
        get() = listOf(
                Triple(RulePosition(cOpt, RuntimeRuleItem.MULTI__EMPTY_RULE, 0), lhs_U, setOf(UP)), // cOpt = . empty
                Triple(RulePosition(cOpt, RuntimeRuleItem.MULTI__ITEM, 0), lhs_U, setOf(c)),        // cOpt = . c
                Triple(RulePosition(bOpt, RuntimeRuleItem.MULTI__EMPTY_RULE, 0), lhs_U, setOf(UP)), // bOpt = . empty
                Triple(RulePosition(bOpt, RuntimeRuleItem.MULTI__ITEM, 0), lhs_U, setOf(b)),        // bOpt = . b
                Triple(RulePosition(aOpt, RuntimeRuleItem.MULTI__EMPTY_RULE, 0), lhs_U, setOf(UP)), // aOpt = . empty
                Triple(RulePosition(aOpt, RuntimeRuleItem.MULTI__ITEM, 0), lhs_U, setOf(a)),        // aOpt = . a
                Triple(RulePosition(S, 0, RulePosition.END_OF_RULE), lhs_U, setOf(UP)),              // S = a? b? c? .
                Triple(RulePosition(S, 0, 2), lhs_U, setOf(c, UP)),                           // S = a? b? . c?
                Triple(RulePosition(S, 0, 1), lhs_U, setOf(b, c, UP)),                        // S = a? . b? c?
                Triple(RulePosition(S, 0, 0), lhs_U, setOf(a, b, c, UP)),                     // S = a? . b? c?
                Triple(RulePosition(G, 0, RulePosition.END_OF_RULE), lhs_U, setOf(UP)),               // G = S .
                Triple(RulePosition(G, 0, 0), lhs_U, setOf(a, b, c, UP))                      // G = . S
        )


    override val s0_widthInto_expected: List<WidthInfo>
        get() = listOf(
            WidthInfo(RP(a, 0, EOR), lhs_bcU),
            WidthInfo(RP(aOpt_E, 0, EOR), lhs_bcU)
        )

    @Test
    fun s0_transitions() {
        val actual = s0.transitions(null)
        val expected = listOf(
                Transition(s0, s1, Transition.ParseAction.WIDTH, lhs_bcU, LookaheadSet.EMPTY, null) { _, _ -> true },
                Transition(s0, s2, Transition.ParseAction.WIDTH, lhs_bcU, LookaheadSet.EMPTY, null) { _, _ -> true }
        )
        assertEquals(expected, actual)
    }

    @Test
    fun s1_heightOrGraftInto_s0() {

        val actual = s1.heightOrGraftInto(s0).toList()

        val expected = listOf(
                HeightGraftInfo(listOf(RP(aOpt, 0, 0)), listOf(RP(aOpt, 0, EOR)), lhs_bcU, lhs_bcU)
        )
        assertEquals(expected, actual)

    }
}