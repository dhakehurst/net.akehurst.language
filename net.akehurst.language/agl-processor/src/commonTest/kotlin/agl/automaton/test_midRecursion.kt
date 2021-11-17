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

import agl.automaton.AutomatonTest
import agl.automaton.automaton
import net.akehurst.language.agl.parser.ScanOnDemandParser
import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.processor.AutomatonKind
import kotlin.test.Test
import kotlin.test.assertEquals

internal class test_midRecursion : test_AutomatonAbstract() {

    /*
        S = b | a S c ;
        ---------------
        S = b | S1
        S1 = a S c
     */

    private companion object {

        val rrs = runtimeRuleSet {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                literal("b")
                ref("S1")
            }
            concatenation("S1") { literal("a"); ref("S"); literal("c") }
        }

        val S = rrs.findRuntimeRule("S")
        val SM = rrs.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        val s0 = SM.startState
        val G = s0.runtimeRules.first()
        val S1 = rrs.findRuntimeRule("S1")
        val a = rrs.findRuntimeRule("'a'")
        val b = rrs.findRuntimeRule("'b'")
        val c = rrs.findRuntimeRule("'c'")

        val lhs_ab = SM.createLookaheadSet(setOf(a, b))
    }

    @Test
    override fun firstOf() {
        listOf(
            Triple(RP(G, 0, SOR), lhs_U, setOf(a, b)),     // G = . S
            Triple(RP(G, 0, EOR), lhs_U, setOf(UP)),      // G = S .
            Triple(RP(S, 0, SOR), lhs_U, setOf(b)),       // S = . b
            Triple(RP(S, 0, EOR), lhs_U, setOf(UP)),      // S = b .
            Triple(RP(S, 1, SOR), lhs_U, setOf(a)),       // S = . S1
            Triple(RP(S, 1, EOR), lhs_U, setOf(UP)),      // S = S1 .
            Triple(RP(S1, 0, SOR), lhs_U, setOf(a)),      // S1 = . a S c
            Triple(RP(S1, 0, 1), lhs_U, setOf(a, b)), // S1 = a . S c
            Triple(RP(S1, 0, 2), lhs_U, setOf(c)),   // S1 = a S . c
            Triple(RP(S1, 0, EOR), lhs_U, setOf(UP))      // S1 = a S c .
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
            WidthInfo(RP(b, 0, EOR), lhs_U),
            WidthInfo(RP(a, 0, EOR), lhs_ab)
        )
        assertEquals(expected.size, actual.size)
        for (i in 0 until actual.size) {
            assertEquals(expected[i], actual[i])
        }
    }

    @Test
    fun parse_b() {
        val parser = ScanOnDemandParser(rrs)
        parser.parseForGoal("S", "b", AutomatonKind.LOOKAHEAD_1)
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", 0, false) {
            val s0 = state(RP(G, 0, SOR))      // G = . S
            val s1 = state(RP(b, 0, EOR))      // b .
            val s2 = state(RP(a, 0, EOR))      // a .
            val s3 = state(RP(S, 0, EOR))      // S = b .
            val s4 = state(RP(G, 0, EOR))      // G = S .

            transition(null, s0, s1, WIDTH, setOf(UP), setOf(), null)
            transition(null, s0, s2, WIDTH, setOf(a, b), setOf(), null)
            transition(s0, s1, s3, HEIGHT, setOf(UP), setOf(UP), listOf(RP(S, 0, SOR)))
            transition(s0, s3, s4, GRAFT, setOf(UP), setOf(UP), listOf(RP(G, 0, SOR)))
            transition(null, s4, s4, GOAL, setOf(), setOf(), null)
        }

        AutomatonTest.assertEquals(expected, actual)

    }

    @Test
    fun parse_abc() {
        val parser = ScanOnDemandParser(rrs)
        parser.parseForGoal("S", "abc", AutomatonKind.LOOKAHEAD_1)
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))
        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", 0, false) {
            val s0 = state(RP(G, 0, SOR))      // G = . S
            val s1 = state(RP(b, 0, EOR))      // b .
            val s2 = state(RP(a, 0, EOR))      // a .
            val s3 = state(RP(S1, 0, 1))  // S1 = a . S c
            val s4 = state(RP(S, 0, EOR))      // S = b .
            val s5 = state(RP(S1, 0, 2))  // S1 = a S . c
            val s6 = state(RP(c, 0, EOR))      // c .
            val s7 = state(RP(S1, 0, EOR))     // S1 = a S c .
            val s8 = state(RP(S, 1, EOR))      // S = S1 .
            val s9 = state(RP(G, 0, EOR))      // G = S .

            transition(null, s0, s1, WIDTH, setOf(UP), setOf(), null)
            transition(null, s0, s2, WIDTH, setOf(a, b), setOf(), null)
            transition(s3, s1, s4, HEIGHT, setOf(c), setOf(c), listOf(RP(S, 0, 0)))
            transition(s0, s2, s3, HEIGHT, setOf(a, b), setOf(UP), listOf(RP(S1, 0, SOR)))
            transition(s0, s3, s1, WIDTH, setOf(c), setOf(), null)
            transition(s0, s3, s2, WIDTH, setOf(a, b), setOf(), null)
            transition(s3, s4, s5, GRAFT, setOf(c), setOf(UP), listOf(RP(S1, 0, 1)))
            transition(s0, s5, s6, WIDTH, setOf(UP), setOf(), null)
            transition(s5, s6, s7, GRAFT, setOf(UP), setOf(UP), listOf(RP(S1, 0, 2)))
            transition(s0, s7, s8, HEIGHT, setOf(UP), setOf(UP), listOf(RP(S, 1, SOR)))
            transition(s0, s8, s9, GRAFT, setOf(UP), setOf(UP), listOf(RP(G, 0, SOR)))
            transition(null, s9, s9, GOAL, setOf(), setOf(), null)
        }

        AutomatonTest.assertEquals(expected, actual)

    }
}