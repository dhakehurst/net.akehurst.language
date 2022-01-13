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
import net.akehurst.language.agl.runtime.structure.LookaheadSet
import net.akehurst.language.agl.runtime.structure.RulePosition
import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.processor.AutomatonKind
import kotlin.test.Test
import kotlin.test.assertEquals

internal class test_abcz_OR_abc : test_AutomatonAbstract() {

    // S =  ABCZ | ABCY
    // ABCZ = a b c z
    // ABC = a b c

    private companion object {
        val rrs = runtimeRuleSet {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("ABCZ")
                ref("ABC")
            }
            concatenation("ABCZ") { literal("a"); literal("b"); literal("c"); literal("z") }
            concatenation("ABC") { literal("a"); literal("b"); literal("c")}
        }
        val S = rrs.findRuntimeRule("S")
        val SM = rrs.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        val G = SM.startState.runtimeRules.first()
        val ABCZ = rrs.findRuntimeRule("ABCZ")
        val ABC = rrs.findRuntimeRule("ABC")
        val a = rrs.findRuntimeRule("'a'")
        val b = rrs.findRuntimeRule("'b'")
        val c = rrs.findRuntimeRule("'c'")
        val z = rrs.findRuntimeRule("'z'")

        val s0 = SM.startState
        val s1 = SM.states[listOf(RP(a, 0, RulePosition.END_OF_RULE))]
        //val s2 = SM.states[listOf(RP(S, 0, RulePosition.POSITION_MULIT_ITEM))]
        //val s3 = SM.states[listOf(RP(S, 0, RulePosition.END_OF_RULE))]

        val lhs_a = SM.createLookaheadSet(false,false, false,setOf(a))
        val lhs_b = SM.createLookaheadSet(false,false, false,setOf(b))
        val lhs_aU = SM.createLookaheadSet(true,false, false,setOf(a))
        val lhs_aT = SM.createLookaheadSet(false,true, false,setOf(a))
    }

    @Test
    override fun firstOf() {
        listOf(
            Triple(RP(G, 0, SOR), lhs_U, LHS(a)),       // G = . S
            Triple(RP(G, 0, EOR), lhs_U, LHS(UP)),      // G = S .
            Triple(RP(S, 0, SOR), lhs_U, LHS(a)),       // S = . ABCZ
            Triple(RP(S, 0, EOR), lhs_U, LHS(UP)),      // S = ABCZ .
            Triple(RP(S, 1, SOR), lhs_U, LHS(a)),       // S = . ABCY
            Triple(RP(S, 1, EOR), lhs_U, LHS(UP)),      // S = ABCY .
            Triple(RP(ABCZ, 0, SOR), lhs_U, LHS(a)),     // ABCZ = . a b c z
            Triple(RP(ABCZ, 0, 1), lhs_U, LHS(b)),  // ABCZ = a . b c z
            Triple(RP(ABCZ, 0, 2), lhs_U, LHS(c)),  // ABCZ = a b . c z
            Triple(RP(ABCZ, 0, 3), lhs_U, LHS(z)),  // ABCZ = a b c . z
            Triple(RP(ABCZ, 0, EOR), lhs_U, LHS(UP)),    // ABCZ = a b c z .
            Triple(RP(ABC, 0, SOR), lhs_U, LHS(a)),     // ABCY = . a b c
            Triple(RP(ABC, 0, 1), lhs_U, LHS(b)),  // ABCY = a . b c
            Triple(RP(ABC, 0, 2), lhs_U, LHS(c)),  // ABCY = a b . c
            Triple(RP(ABC, 0, EOR), lhs_U, LHS(UP))     // ABCY = a b c .
        ).testAll { rp, lhs, expected ->
            val actual = SM.buildCache.firstOf(rp, lhs.part)
            assertEquals(expected, actual, "failed $rp")
        }
    }

    @Test
    override fun s0_widthInto() {
        val s0 = SM.startState
        val actual = s0.widthInto(null).toList()

        val expected = listOf(
            WidthInfo(RP(a, 0, EOR), lhs_b.part)
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
                listOf(G, S),
                listOf(RP(ABCZ, 0, SOR), RP(ABC, 0, SOR)),
                listOf(RP(ABCZ, 0, 1), RP(ABC, 0, 1)),
                lhs_b.part,
                lhs_U.part
            )
        )
        assertEquals(expected, actual)

    }

    @Test
    fun s0_transitions() {
        val actual = s0.transitions(null)
        val expected = listOf(
            Transition(s0, s1, Transition.ParseAction.WIDTH, lhs_b, LookaheadSet.EMPTY, null) { _, _ -> true },
        )
        assertEquals(expected, actual)
    }

    @Test
    fun automaton_parse_abc() {
        val parser = ScanOnDemandParser(rrs)
        parser.parseForGoal("S", "abc", AutomatonKind.LOOKAHEAD_1)
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))
        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", 0, false) {
            val s0 = state(RP(G, 0, SOR))                                // G = . S
            val s1 = state(RP(a, 0, EOR))                                // a .
            val s2 = state(RP(ABCZ, 0, 1),RP(ABC,0,1))    // ABCZ = a . b c z , ABCY = a . b c y
            val s3 = state(RP(b, 0, EOR))                                // b .
            val s4 = state(RP(ABCZ, 0, 2),RP(ABC,0,2))    // ABCZ = a b . c z , ABCY = a b . c y
            val s5 = state(RP(c, 0, EOR))                                // c .
            val s6 = state(RP(ABCZ, 0, 3),RP(ABC,0,3))    // ABCZ = a b c . z , ABCY = a b c . y
            val s7 = state(RP(z, 0, EOR))                                // z .

            transition(null, s0, s1, WIDTH, setOf(b), setOf(), null)
            transition(s0, s1, s2, HEIGHT, setOf(b), setOf(UP), listOf(RP(ABCZ,0,0), RP(ABC,0,0)))
            transition(s0, s2, s3, WIDTH, setOf(c), setOf(), null)
            transition(s2, s3, s4, GRAFT, setOf(c), setOf(UP), listOf(RP(ABCZ,0,1), RP(ABC,0,1)))
            transition(s0, s4, s5, WIDTH, setOf(z), setOf(), null)
            transition(s4, s5, s6, GRAFT, setOf(z), setOf(UP), listOf(RP(ABCZ,0,2), RP(ABC,0,2)))
            transition(s0, s6, s7, WIDTH, setOf(UP), setOf(), null)
        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun buildFor() {
        val actual = rrs.buildFor("S", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", 1, false) {

        }

        AutomatonTest.assertEquals(expected, actual)
    }
}