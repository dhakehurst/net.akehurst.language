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

internal class test_abc_OR_abd : test_Abstract() {

    // S =  ABC | ABD
    // ABC = a b c
    // ABD = a b d

    private companion object {
        val rrs = runtimeRuleSet {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("ABC")
                ref("ABD")
            }
            concatenation("ABC") { literal("a"); literal("b"); literal("c") }
            concatenation("ABD") { literal("a"); literal("b"); literal("d") }
        }
        val S = rrs.findRuntimeRule("S")
        val SM = rrs.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        val G = SM.startState.runtimeRules.first()
        val ABC = rrs.findRuntimeRule("ABC")
        val ABD = rrs.findRuntimeRule("ABD")
        val a = rrs.findRuntimeRule("'a'")
        val b = rrs.findRuntimeRule("'b'")
        val c = rrs.findRuntimeRule("'c'")
        val d = rrs.findRuntimeRule("'d'")

        val s0 = SM.startState
        val s1 = SM.states[listOf(RP(a, 0, RulePosition.END_OF_RULE))]
        val s2 = SM.states[listOf(RP(S, 0, RulePosition.POSITION_MULIT_ITEM))]
        val s3 = SM.states[listOf(RP(S, 0, RulePosition.END_OF_RULE))]

        val lhs_a = SM.createLookaheadSet(setOf(a))
        val lhs_b = SM.createLookaheadSet(setOf(b))
        val lhs_aU = SM.createLookaheadSet(setOf(a, UP))
        val lhs_aT = SM.createLookaheadSet(setOf(a, EOT))
    }

    @Test
    override fun firstOf() {
        listOf(
            Triple(RP(G, 0, SOR), lhs_U, setOf(a)),       // G = . S
            Triple(RP(G, 0, EOR), lhs_U, setOf(UP)),      // G = S .
            Triple(RP(S, 0, SOR), lhs_U, setOf(a)),       // S = . ABC
            Triple(RP(S, 0, EOR), lhs_U, setOf(UP)),      // S = ABC .
            Triple(RP(S, 1, SOR), lhs_U, setOf(a)),       // S = . ABD
            Triple(RP(S, 1, EOR), lhs_U, setOf(UP)),      // S = ABD .
            Triple(RP(ABC, 0, SOR), lhs_U, setOf(a)),     // ABC = . a b c
            Triple(RP(ABC, 0, 1), lhs_U, setOf(b)),  // ABC = a . b c
            Triple(RP(ABC, 0, 2), lhs_U, setOf(c)),  // ABC = a b . c
            Triple(RP(ABC, 0, EOR), lhs_U, setOf(UP)),    // ABC = a b c .
            Triple(RP(ABD, 0, SOR), lhs_U, setOf(a)),     // ABD = . a b d
            Triple(RP(ABD, 0, 1), lhs_U, setOf(b)),  // ABD = a . b d
            Triple(RP(ABD, 0, 2), lhs_U, setOf(d)),  // ABD = a b . d
            Triple(RP(ABD, 0, EOR), lhs_U, setOf(UP))     // ABD = a b d .
        ).testAll { rp, lhs, expected ->
            val actual = SM.buildCache.firstOf(rp, lhs)
            assertEquals(expected, actual, "failed $rp")
        }
    }

    @Test
    override fun s0_widthInto() {
        val s0 = SM.startState
        val actual = s0.widthInto(null).toList()

        val expected = listOf(
            WidthInfo(RP(a, 0, EOR), lhs_b)
        )
        assertEquals(expected.size, actual.size)
        for (i in 0 until actual.size) {
            assertEquals(expected[i], actual[i])
        }
    }

    @Test
    fun s1_heightOrGraftInto_s0() {

        val actual = s1.heightOrGraftInto(s0).toList()

        val expected = listOf(
            HeightGraftInfo(
                    listOf(RP(ABC, 0, SOR),RP(ABD, 0, SOR)),
                    listOf(RP(ABC, 0, 1),RP(ABD, 0, 1)),
                    lhs_b,
                    lhs_U
                )
        )
        assertEquals(expected, actual)

    }

    @Test
    fun s0_transitions() {
        val actual = s0.transitions(null)
        val expected = listOf<Transition>(
        //    Transition(s0, s1, Transition.ParseAction.WIDTH, lhs_bcU, LookaheadSet.EMPTY, null) { _, _ -> true },
        //    Transition(s0, s2, Transition.ParseAction.WIDTH, lhs_bcU, LookaheadSet.EMPTY, null) { _, _ -> true }
        )
        assertEquals(expected, actual)
    }
}