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

import agl.automaton.automaton
import net.akehurst.language.agl.runtime.structure.*
import kotlin.test.Test
import kotlin.test.assertEquals

class test_AhoSetiUlman_4_54 : test_Abstract() {
    // S = C C ;
    // C = c C | d ;
    //
    // S = C C ;
    // C = C1 | d ;
    // C1 = c C ;
    private companion object {
        val rrs = runtimeRuleSet {
            concatenation("S") { ref("C"); ref("C") }
            choice("C", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("C1")
                literal("d")
            }
            concatenation("C1") { literal("c"); ref("C") }
        }

        val S = rrs.findRuntimeRule("S")
        val SM = rrs.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val C = rrs.findRuntimeRule("C")
        val C1 = rrs.findRuntimeRule("C1")
        val T_c = rrs.findRuntimeRule("'c'")
        val T_d = rrs.findRuntimeRule("'d'")
        val G = SM.startState.runtimeRules.first()

        val s0 = SM.startState
        val s1 = SM.states[listOf(RP(T_c, 0, EOR))]
        val s2 = SM.states[listOf(RP(T_d, 0, EOR))]

        val lhs_cd = LookaheadSet(0, setOf(T_c, T_d))
    }

    override val SM: ParserStateSet
        get() = Companion.SM

    override val firstOf_data: List<Triple<RulePosition, LookaheadSet, Set<RuntimeRule>>>
        get() = listOf(
                Triple(RP(C1, 0, SOR), lhs_U, setOf(T_c)),          // C1 = . c C
                Triple(RP(C1, 0, 1), lhs_U, setOf(T_c, T_d)),  // C1 = c . C
                Triple(RP(C1, 0, EOR), lhs_U, setOf(UP)),           // C1 = c C .
                Triple(RP(C, 1, SOR), lhs_U, setOf(T_d)),           // C = . d
                Triple(RP(C, 1, EOR), lhs_U, setOf(UP)),            // C = d .
                Triple(RP(C, 0, SOR), lhs_U, setOf(T_c)),           // C = . C1
                Triple(RP(C, 0, EOR), lhs_U, setOf(UP)),            // C = C1 .
                Triple(RP(S, 0, SOR), lhs_U, setOf(T_c, T_d)),      // S = . C C
                Triple(RP(S, 0, 1), lhs_U, setOf(T_c, T_d)),   // S = C . C
                Triple(RP(S, 0, EOR), lhs_U, setOf(UP)),            // S = C C .
                Triple(RP(G, 0, SOR), lhs_U, setOf(T_c, T_d)),      // G = . S
                Triple(RP(G, 0, EOR), lhs_U, setOf(UP))             // G = S .
        )

    override val s0_widthInto_expected: List<WidthInfo>
        get() = listOf(
            WidthInfo(RP(T_c, 0, EOR), lhs_cd),
            WidthInfo(RP(T_d, 0, EOR), lhs_cd)
        )



    @Test
    fun s0_transitions() {
        val s0 = rrs.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1).startState

        val actual = s0.transitions(null)

        val expected = listOf(
                Transition(s0, s1, Transition.ParseAction.WIDTH, LookaheadSet(0, setOf(T_c, T_d)), lhs_E, null) { _, _ -> true },
                Transition(s0, s2, Transition.ParseAction.WIDTH, LookaheadSet(0, setOf(T_c, T_d)), lhs_E, null) { _, _ -> true }
        ).toList()
        assertEquals(expected.size, actual.size)
        for (i in 0 until actual.size) {
            assertEquals(expected[i], actual[i])
        }
    }


    @Test
    fun buildFor() {
        val actual = rrs.buildFor("S", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            val s0 = state(RP(G, 0, SOR))       // G = . S
            val s1 = state(RP(T_c, 0, EOR))     // c
            val s2 = state(RP(T_d, 0, EOR))     // d
            val s3 = state(RP(C1, 0, 1))   // C1 = c . C
            val s4 = state(RP(C, 1, EOR))       // C = C1 .
            val s5 = state(RP(C1, 0, EOR))      // C1 = c C .
            val s6 = state(RP(C, 0, EOR))       // C = d .
            val s7 = state(RP(S, 0, 1))    // S = C . C
            val s8 = state(RP(S, 0, EOR))       // S = C C .
            val s9 = state(RP(G, 0, EOR))       // G = S .

            transition(null, s0, s1, WIDTH, setOf(T_c, T_d), emptySet(), null)
            transition(null, s0, s2, WIDTH, setOf(T_c, T_d), emptySet(), null)

            transition(s0, s1, s3, HEIGHT, setOf(T_c, T_d), setOf(T_c, T_d), listOf(RP(C1, 0, 0)))
            transition(s3, s1, s3, HEIGHT, setOf(T_c, T_d), setOf(UP), listOf(RP(C1, 0, 0)))
            transition(s7, s1, s3, HEIGHT, setOf(T_c, T_d), setOf(UP), listOf(RP(C1, 0, 0)))

            transition(s3, s2, s4, HEIGHT, setOf(UP), setOf(UP), listOf(RP(C,1,0)))
            transition(s7, s2, s4, HEIGHT, setOf(UP), setOf(UP), listOf(RP(C,1,0)))
            transition(s0, s2, s4, HEIGHT, setOf(T_c, T_d), setOf(T_c, T_d), listOf(RP(C,1,0)))

            transition(s0, s3, s1, WIDTH, setOf(T_c, T_d), emptySet(), null)
            transition(s0, s3, s2, WIDTH, setOf(T_c, T_d), emptySet(), null)
            transition(s3, s3, s1, WIDTH, setOf(T_c, T_d), emptySet(), null)
            transition(s3, s3, s2, WIDTH, setOf(UP), emptySet(), null)
            transition(s7, s3, s1, WIDTH, setOf(T_c, T_d), emptySet(), null)
            transition(s7, s3, s2, WIDTH, setOf(UP), emptySet(), null)

            transition(s3, s4, s5, GRAFT, setOf(UP), setOf(UP), listOf(RP(C1,0,1)))
            transition(s7, s4, s8, GRAFT, setOf(UP), setOf(UP), listOf(RP(S,0,1)))
            transition(s0, s4, s7, HEIGHT, setOf(T_c, T_d), setOf(UP), listOf(RP(S,0,0)))

            transition(s3, s5, s6, HEIGHT, setOf(UP), setOf(UP), listOf(RP(C,0,0)))
            transition(s0, s5, s6, HEIGHT, setOf(T_c, T_d), setOf(T_c, T_d), listOf(RP(C,0,0)))

            transition(s3, s6, s5, GRAFT, setOf(UP), setOf(UP), listOf(RP(C1,0,1)))
            transition(s0, s6, s7, HEIGHT, setOf(T_c, T_d), setOf(UP), listOf(RP(S,0,0)))

            transition(s0, s7, s1, WIDTH, setOf(T_c, T_d), emptySet(), null)
            transition(s0, s7, s2, WIDTH, setOf(UP), emptySet(), null)

            transition(s0, s8, s9, GRAFT, setOf(UP), setOf(UP), listOf(RP(G,0,0)))

            transition(null, s9, s9, GOAL, setOf(), setOf(), null)
        }

        assertEquals(expected, actual)
    }
}