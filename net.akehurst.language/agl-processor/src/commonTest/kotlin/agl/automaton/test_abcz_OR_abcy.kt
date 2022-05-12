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
import net.akehurst.language.agl.runtime.structure.RulePosition
import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.processor.AutomatonKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class test_abcz_OR_abcy : test_AutomatonAbstract() {

    // S =  ABCZ | ABCY
    // ABCZ = a b c z
    // ABCY = a b c y

    // must be fresh per test or automaton is not correct for different parses (due to caching)
    private val rrs = runtimeRuleSet {
        choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
            ref("ABCZ")
            ref("ABCY")
        }
        concatenation("ABCZ") { literal("a"); literal("b"); literal("c"); literal("z") }
        concatenation("ABCY") { literal("a"); literal("b"); literal("c"); literal("y") }
    }
    private val S = rrs.findRuntimeRule("S")
    private val SM = rrs.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
    private val G = SM.startState.runtimeRules.first()
    private val ABCZ = rrs.findRuntimeRule("ABCZ")
    private val ABCY = rrs.findRuntimeRule("ABCY")
    private val a = rrs.findRuntimeRule("'a'")
    private val b = rrs.findRuntimeRule("'b'")
    private val c = rrs.findRuntimeRule("'c'")
    private val z = rrs.findRuntimeRule("'z'")
    private val y = rrs.findRuntimeRule("'y'")

    private val lhs_b = SM.createLookaheadSet(false, false, false, setOf(b))


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
            Triple(RP(ABCY, 0, SOR), lhs_U, LHS(a)),     // ABCY = . a b c y
            Triple(RP(ABCY, 0, 1), lhs_U, LHS(b)),  // ABCY = a . b c y
            Triple(RP(ABCY, 0, 2), lhs_U, LHS(c)),  // ABCY = a b . c y
            Triple(RP(ABCY, 0, 3), lhs_U, LHS(y)),  // ABCY = a b c . y
            Triple(RP(ABCY, 0, EOR), lhs_U, LHS(UP))     // ABCY = a b c y .
        ).testAll { rp, lhs, expected ->
            val actual = SM.buildCache.firstOf(rp, lhs.part)
            assertEquals(expected, actual, "failed $rp")
        }
    }

    @Test
    override fun s0_widthInto() {
        val s0 = SM.startState
        val actual = s0.widthInto(s0).toList()

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
        val s0 = SM.startState
        val s1 = SM.createState(listOf(RP(a, 0, RulePosition.END_OF_RULE)))
        val actual = s1.heightOrGraftInto(s0).toList()

        val expected = listOf(
            HeightGraftInfo(
                Transition.ParseAction.HEIGHT,
                listOf(RP(ABCZ, 0, SOR), RP(ABCY, 0, SOR)),
                listOf(RP(ABCZ, 0, 1), RP(ABCY, 0, 1)),
                setOf(LookaheadInfoPart(LHS(b), LHS(UP)))
            )
        )
        assertEquals(expected, actual)

    }

    @Test
    fun s0_transitions() {
        val s0 = SM.startState
        val s1 = SM.createState(listOf(RP(a, 0, RulePosition.END_OF_RULE)))
        val actual = s0.transitions(s0)
        val expected = listOf(
            Transition(s0, s1, Transition.ParseAction.WIDTH, lhs_b, LookaheadSet.EMPTY, null) { _, _ -> true },
        )
        assertEquals(expected, actual)
    }

    @Test
    fun automaton_parse_abcz() {
        val parser = ScanOnDemandParser(rrs)
        val (sppt, issues) = parser.parseForGoal("S", "abcz", AutomatonKind.LOOKAHEAD_1)
        assertNotNull(sppt)
        assertEquals(0, issues.size)
        assertEquals(1, sppt.maxNumHeads)
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", 0, false) {
            val s0 = state(RP(G, 0, SOR))                                   // G = . S
            val s1 = state(RP(a, 0, EOR))                                   // a .
            val s2 = state(RP(ABCZ, 0, 1), RP(ABCY, 0, 1))    // ABCZ = a . b c z , ABCY = a . b c y
            val s3 = state(RP(b, 0, EOR))                                   // b .
            val s4 = state(RP(ABCZ, 0, 2), RP(ABCY, 0, 2))    // ABCZ = a b . c z , ABCY = a b . c y
            val s5 = state(RP(c, 0, EOR))                                   // c .
            val s6 = state(RP(ABCZ, 0, 3), RP(ABCY, 0, 3))    // ABCZ = a b c . z , ABCY = a b c . y
            val s7 = state(RP(z, 0, EOR))                                   // z .
            val s8 = state(RP(y, 0, EOR))                                   // y .
            val s9 = state(RP(ABCZ, 0, EOR))                                // ABCZ = abcz .
            val s10 = state(RP(S, 0, EOR))                                  // S = ABCZ .
            val s11 = state(RP(G, 0, EOR))                                  // G = S .

            transition(s0, s0, s1, WIDTH, setOf(b), setOf(), null)
            transition(s0, s1, s2, HEIGHT, setOf(b), setOf(setOf(UP)), listOf(RP(ABCZ, 0, 0), RP(ABCY, 0, 0)))
            transition(s0, s2, s3, WIDTH, setOf(c), setOf(), null)
            transition(s2, s3, s4, GRAFT, setOf(c), setOf(setOf(UP)), listOf(RP(ABCZ, 0, 1), RP(ABCY, 0, 1)))
            transition(s0, s4, s5, WIDTH, setOf(z, y), setOf(), null)
            transition(s4, s5, s6, GRAFT, setOf(z, y), setOf(setOf(UP)), listOf(RP(ABCZ, 0, 2), RP(ABCY, 0, 2)))
            transition(s0, s6, s7, WIDTH, setOf(UP), setOf(), null)
            transition(s0, s6, s8, WIDTH, setOf(UP), setOf(), null)
            transition(s6, s7, s9, GRAFT, setOf(UP), setOf(setOf(UP)), listOf(RP(ABCZ, 0, 3)))
            transition(s0, s9, s10, HEIGHT, setOf(UP), setOf(setOf(UP)), listOf(RP(S, 0, 0)))
            transition(s0, s10, s11, GRAFT, setOf(UP), setOf(setOf(UP)), listOf(RP(G, 0, 0)))
        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun automaton_parse_abcy() {
        val parser = ScanOnDemandParser(rrs)
        val (sppt, issues) = parser.parseForGoal("S", "abcy", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))
        assertNotNull(sppt)
        assertEquals(0, issues.size)
        assertEquals(1, sppt.maxNumHeads)
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", 0, false) {
            val s0 = state(RP(G, 0, SOR))                                   // G = . S
            val s1 = state(RP(a, 0, EOR))                                   // a .
            val s2 = state(RP(ABCZ, 0, 1), RP(ABCY, 0, 1))    // ABCZ = a . b c z , ABCY = a . b c y
            val s3 = state(RP(b, 0, EOR))                                   // b .
            val s4 = state(RP(ABCZ, 0, 2), RP(ABCY, 0, 2))    // ABCZ = a b . c z , ABCY = a b . c y
            val s5 = state(RP(c, 0, EOR))                                   // c .
            val s6 = state(RP(ABCZ, 0, 3), RP(ABCY, 0, 3))    // ABCZ = a b c . z , ABCY = a b c . y
            val s7 = state(RP(z, 0, EOR))                                   // z .
            val s8 = state(RP(y, 0, EOR))                                   // y .
            val s9 = state(RP(ABCY, 0, EOR))                                // ABCY = abcy .
            val s10 = state(RP(S, 1, EOR))                                  // S = ABCY .
            val s11 = state(RP(G, 0, EOR))                                  // G = S .

            transition(s0, s0, s1, WIDTH, setOf(b), setOf(), null)
            transition(s0, s1, s2, HEIGHT, setOf(b), setOf(setOf(UP)), listOf(RP(ABCZ, 0, 0), RP(ABCY, 0, 0)))
            transition(s0, s2, s3, WIDTH, setOf(c), setOf(), null)
            transition(s2, s3, s4, GRAFT, setOf(c), setOf(setOf(UP)), listOf(RP(ABCZ, 0, 1), RP(ABCY, 0, 1)))
            transition(s0, s4, s5, WIDTH, setOf(z, y), setOf(), null)
            transition(s4, s5, s6, GRAFT, setOf(z, y), setOf(setOf(UP)), listOf(RP(ABCZ, 0, 2), RP(ABCY, 0, 2)))
            transition(s0, s6, s7, WIDTH, setOf(UP), setOf(), null)
            transition(s0, s6, s8, WIDTH, setOf(UP), setOf(), null)
            transition(s6, s8, s9, GRAFT, setOf(UP), setOf(setOf(UP)), listOf(RP(ABCY, 0, 3)))
            transition(s0, s9, s10, HEIGHT, setOf(UP), setOf(setOf(UP)), listOf(RP(S, 1, 0)))
            transition(s0, s10, s11, GRAFT, setOf(UP), setOf(setOf(UP)), listOf(RP(G, 0, 0)))

        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun buildFor() {
        val actual = rrs.buildFor("S", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))

        val sentences = listOf("abcz","abcy")
        sentences.forEach {
            val parser = ScanOnDemandParser(rrs)
            val (sppt, issues) = parser.parseForGoal("S", it, AutomatonKind.LOOKAHEAD_1)
            assertNotNull(sppt, issues.joinToString("\n") { it.toString() } )
            assertEquals(0, issues.size)
            assertEquals(1, sppt.maxNumHeads)
        }

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", 1, false) {
            val s0 = state(RP(G, 0, SOR))                                   // G = . S
            val s1 = state(RP(G, 0, EOR))                                  // G = S .
            val s2 = state(RP(S, 0, EOR))                                  // S = ABCZ .
            val s3 = state(RP(S, 1, EOR))                                  // S = ABCY .
            val s4 = state(RP(ABCY, 0, EOR))                                // ABCY = abcy .
            val s5 = state(RP(y, 0, EOR))                                   // y .
            val s6 = state(RP(c, 0, EOR))                                   // c .
            val s7 = state(RP(b, 0, EOR))                                   // b .
            val s8 = state(RP(a, 0, EOR))                                   // a .
            val s9 = state(RP(ABCZ, 0, EOR))                                // ABCZ = abcz .
            val s10 = state(RP(z, 0, EOR))                                   // z .
            val s11 = state(RP(ABCZ, 0, 1), RP(ABCY, 0, 1))    // ABCZ = a . b c z , ABCY = a . b c y
            val s12 = state(RP(ABCZ, 0, 2), RP(ABCY, 0, 2))    // ABCZ = a b . c z , ABCY = a b . c y
            val s13 = state(RP(ABCZ, 0, 3), RP(ABCY, 0, 3))    // ABCZ = a b c . z , ABCY = a b c . y
        }

        AutomatonTest.assertEquals(expected, actual)
    }
}