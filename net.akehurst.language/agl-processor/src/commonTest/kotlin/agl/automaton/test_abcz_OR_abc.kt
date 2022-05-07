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
import net.akehurst.language.agl.automaton.ParserState.Companion.lhs
import net.akehurst.language.agl.parser.ScanOnDemandParser
import net.akehurst.language.agl.runtime.structure.LookaheadSet
import net.akehurst.language.agl.runtime.structure.RulePosition
import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.processor.AutomatonKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class test_abcz_OR_abc : test_AutomatonAbstract() {

    // S =  ABCZ | ABC
    // ABCZ = a b c z
    // ABC = a b c

    // must be fresh per test or automaton is not correct for different parses (due to caching)
    private val rrs = runtimeRuleSet {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                ref("ABCZ")
                ref("ABC")
            }
            concatenation("ABCZ") { literal("a"); literal("b"); literal("c"); literal("z") }
            concatenation("ABC") { literal("a"); literal("b"); literal("c")}
        }
    private    val S = rrs.findRuntimeRule("S")
    private    val SM = rrs.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
    private    val G = SM.startState.runtimeRules.first()
    private    val ABCZ = rrs.findRuntimeRule("ABCZ")
    private    val ABC = rrs.findRuntimeRule("ABC")
    private    val a = rrs.findRuntimeRule("'a'")
    private    val b = rrs.findRuntimeRule("'b'")
    private    val c = rrs.findRuntimeRule("'c'")
    private    val z = rrs.findRuntimeRule("'z'")

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
        val actual = s0.widthInto(s0)

        val expected = setOf(
            WidthInfo(RP(a, 0, EOR), LHS(b))
        )
        assertEquals(expected, actual)
    }

    @Test
    fun s1_heightOrGraftInto_s0() {
        val s0 = SM.startState
        val s1 = SM.createState(listOf(RP(a, 0, EOR)))
        s0.widthInto(s0)
        val actual = s1.heightOrGraftInto(s0)

        val expected = setOf(
            HeightGraftInfo(
                Transition.ParseAction.HEIGHT,
                listOf(RP(ABCZ, 0, SOR), RP(ABC, 0, SOR)),
                listOf(RP(ABCZ, 0, 1), RP(ABC, 0, 1)),
                LHS(b),
                setOf(LHS(UP))
            )
        )
        assertEquals(expected, actual)

    }

    @Test
    fun s0_transitions() {
        val s0 = SM.startState
        val s1 = SM.createState(listOf(RP(a, 0, EOR)))
        val actual = s0.transitions(s0)
        val expected = listOf(
            Transition(s0, s1, WIDTH, LHS(b).lhs(SM), LookaheadSet.EMPTY, null) { _, _ -> true },
        )
        assertEquals(expected, actual)
    }

    @Test
    fun automaton_parse_abc() {
        val parser = ScanOnDemandParser(rrs)
        val(sppt,issues) = parser.parseForGoal("S", "abc", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))
        assertNotNull(sppt)
        assertEquals(0,issues.size)
        assertEquals(1,sppt.maxNumHeads)
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", 0, false) {
            val s0 = state(RP(G, 0, SOR))                                // G = . S
            val s1 = state(RP(a, 0, EOR))                                // a .
            val s2 = state(RP(ABCZ, 0, 1),RP(ABC,0,1))    // ABCZ = a . b c z , ABC = a . b c
            val s3 = state(RP(b, 0, EOR))                                // b .
            val s4 = state(RP(ABCZ, 0, 2),RP(ABC,0,2))    // ABCZ = a b . c z , ABC = a b . c
            val s5 = state(RP(c, 0, EOR))                                // c .
            val s6 = state(RP(ABC,0,EOR))                                // ABC = a b c .
            val s7 = state(RP(ABCZ, 0, 3))                          // ABCZ = a b c . z
            val s8 = state(RP(S, 1, EOR))                                // S = ABC .
            val s9 = state(RP(G, 0, EOR))                                // G = S .

            transition(null, s0, s1, WIDTH, setOf(b), emptySet(), null)
            transition(s0, s1, s2, HEIGHT, setOf(b), setOf(setOf(UP)), listOf(RP(ABCZ,0,0), RP(ABC,0,0)))
            transition(s0, s2, s3, WIDTH, setOf(c), setOf(), null)
            transition(s2, s3, s4, GRAFT, setOf(c), setOf(setOf(UP)), listOf(RP(ABCZ,0,1), RP(ABC,0,1)))
            transition(s0, s4, s5, WIDTH, setOf(UP,z), setOf(), null)
            transition(s4, s5, s6, GRAFT, setOf(UP), setOf(setOf(UP)), listOf(RP(ABC,0,2)))
            transition(s4, s5, s7, GRAFT, setOf(z), setOf(setOf(UP)), listOf(RP(ABCZ,0,2)))
            transition(s0, s6, s8, HEIGHT, setOf(UP), setOf(setOf(UP)), listOf(RP(S,1,SOR)))
            transition(s0, s8, s9, GRAFT, setOf(UP), setOf(setOf(UP)), listOf(RP(G,0,SOR)))
            transition(null, s9, s9, GOAL, setOf(), setOf(), null)
        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun automaton_parse_abcz() {
        val parser = ScanOnDemandParser(rrs)
        val(sppt,issues) = parser.parseForGoal("S", "abcz", AutomatonKind.LOOKAHEAD_1)
        assertNotNull(sppt)
        assertEquals(0,issues.size)
        assertEquals(1,sppt.maxNumHeads)
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))
        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", 0, false) {
            val s0 = state(RP(G, 0, SOR))                                // G = . S
            val s1 = state(RP(a, 0, EOR))                                // a .
            val s2 = state(RP(ABCZ, 0, 1),RP(ABC,0,1))    // ABCZ = a . b c z , ABC = a . b c
            val s3 = state(RP(b, 0, EOR))                                // b .
            val s4 = state(RP(ABCZ, 0, 2),RP(ABC,0,2))    // ABCZ = a b . c z , ABC = a b . c
            val s5 = state(RP(c, 0, EOR))                                // c .
            val s6 = state(RP(ABCZ, 0, 3))                          // ABCZ = a b c . z
            val s7 = state(RP(ABC,0,EOR))                                // ABC = a b c .
            val s8 = state(RP(z, 0, EOR))                                // z .
            val s9 = state(RP(ABCZ, 0, EOR))                             // ABCZ = a b c z .
            val s10 = state(RP(S, 0, EOR))                               // S = ABC .
            val s11 = state(RP(G, 0, EOR))                               // G = S .

            transition(null, s0, s1, WIDTH, setOf(b), emptySet(), null)
            transition(s0, s1, s2, HEIGHT, setOf(b), setOf(setOf(UP)), listOf(RP(ABCZ,0,0), RP(ABC,0,0)))
            transition(s0, s2, s3, WIDTH, setOf(c), setOf(), null)
            transition(s2, s3, s4, GRAFT, setOf(c), setOf(setOf(UP)), listOf(RP(ABCZ,0,1), RP(ABC,0,1)))
            transition(s0, s4, s5, WIDTH, setOf(UP,z), setOf(), null)
            transition(s4, s5, s6, GRAFT, setOf(z), setOf(setOf(UP)), listOf(RP(ABCZ,0,2)))
            transition(s4, s5, s7, GRAFT, setOf(UP), setOf(setOf(UP)), listOf(RP(ABC,0,2)))
            transition(s0, s6, s8, WIDTH, setOf(UP), setOf(), null)
            transition(s6, s8, s9, GRAFT, setOf(UP), setOf(setOf(UP)), listOf(RP(ABCZ,0,3)))
            transition(s0, s9, s10, HEIGHT, setOf(UP), setOf(setOf(UP)), listOf(RP(S,0,SOR)))
            transition(s0, s10, s11, GRAFT, setOf(UP), setOf(setOf(UP)), listOf(RP(G,0,SOR)))
            transition(null, s11, s11, GOAL, setOf(), setOf(), null)
        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun stateInfo() {
        val bc = BuildCacheLC1(SM)

        val actual = bc.stateInfo2()
    }

    @Test
    fun buildFor() {
        val actual = rrs.buildFor("S", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))

        val parser = ScanOnDemandParser(rrs)
        val sentences = listOf("abcz","abc")
        for (sent in sentences) {
            println("Parsing sentence '$sent'")
            val (sppt, issues) = parser.parseForGoal("S", sent, AutomatonKind.LOOKAHEAD_1)
            assertNotNull(sppt, issues.joinToString("\n") { it.toString() } )
            assertEquals(0, issues.size)
            assertEquals(1, sppt.maxNumHeads)
        }

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", 1, false) {

        }

        AutomatonTest.assertEquals(expected, actual)
    }
}