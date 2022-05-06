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
import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.processor.AutomatonKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class test_abc_OR_abd : test_AutomatonAbstract() {

    // S =  ABC | ABD
    // ABC = a b c
    // ABD = a b d

    // must be fresh per test or automaton is not correct for different parses (due to caching)
    private val rrs = runtimeRuleSet {
        choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
            ref("ABC")
            ref("ABD")
        }
        concatenation("ABC") { literal("a"); literal("b"); literal("c") }
        concatenation("ABD") { literal("a"); literal("b"); literal("d") }
    }
    private val S = rrs.findRuntimeRule("S")
    private val SM = rrs.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
    private val G = SM.startState.runtimeRules.first()
    private val ABC = rrs.findRuntimeRule("ABC")
    private val ABD = rrs.findRuntimeRule("ABD")
    private val a = rrs.findRuntimeRule("'a'")
    private val b = rrs.findRuntimeRule("'b'")
    private val c = rrs.findRuntimeRule("'c'")
    private val d = rrs.findRuntimeRule("'d'")

    private val lhs_b = SM.createLookaheadSet(false, false, false, setOf(b))


    @Test
    override fun firstOf() {
        listOf(
            Triple(RP(G, 0, SOR), lhs_U, LHS(a)),       // G = . S
            Triple(RP(G, 0, EOR), lhs_U, LHS(UP)),      // G = S .
            Triple(RP(S, 0, SOR), lhs_U, LHS(a)),       // S = . ABC
            Triple(RP(S, 0, EOR), lhs_U, LHS(UP)),      // S = ABC .
            Triple(RP(S, 1, SOR), lhs_U, LHS(a)),       // S = . ABD
            Triple(RP(S, 1, EOR), lhs_U, LHS(UP)),      // S = ABD .
            Triple(RP(ABC, 0, SOR), lhs_U, LHS(a)),     // ABC = . a b c
            Triple(RP(ABC, 0, 1), lhs_U, LHS(b)),  // ABC = a . b c
            Triple(RP(ABC, 0, 2), lhs_U, LHS(c)),  // ABC = a b . c
            Triple(RP(ABC, 0, EOR), lhs_U, LHS(UP)),    // ABC = a b c .
            Triple(RP(ABD, 0, SOR), lhs_U, LHS(a)),     // ABD = . a b d
            Triple(RP(ABD, 0, 1), lhs_U, LHS(b)),  // ABD = a . b d
            Triple(RP(ABD, 0, 2), lhs_U, LHS(d)),  // ABD = a b . d
            Triple(RP(ABD, 0, EOR), lhs_U, LHS(UP))     // ABD = a b d .
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
        val s1 = SM.createState(listOf(RP(a, 0, EOR)))
        s0.widthInto(s0)
        val actual = s1.heightOrGraftInto(s0)

        val expected = setOf(
            HeightGraftInfo(
                Transition.ParseAction.HEIGHT,
                listOf(RP(ABC, 0, SOR), RP(ABD, 0, SOR)),
                listOf(RP(ABC, 0, 1), RP(ABD, 0, 1)),
                lhs_b.part,
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
        val expected = listOf<Transition>(
            Transition(s0, s1, Transition.ParseAction.WIDTH, lhs_b, LookaheadSet.EMPTY, null) { _, _ -> true },
            //    Transition(s0, s2, Transition.ParseAction.WIDTH, lhs_bcU, LookaheadSet.EMPTY, null) { _, _ -> true }
        )
        assertEquals(expected, actual)
    }

    @Test
    fun automaton_parse_abc() {
        val parser = ScanOnDemandParser(rrs)
        val (sppt, issues) = parser.parseForGoal("S", "abc", AutomatonKind.LOOKAHEAD_1)
        assertNotNull(sppt)
        assertEquals(0, issues.size)
        assertEquals(1, sppt.maxNumHeads)
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", 0, false) {
            val s0 = state(RP(G, 0, SOR))     /* G = . S   */
            val s1 = state(RP(a, 0, EOR))     /* a .       */
            val s2 = state(RP(ABC, 0, 1), RP(ABD, 0, 1)) /* ABC = a . bc | ABD = a . bd */
            val s3 = state(RP(b, 0, EOR))     /* b . */
            val s4 = state(RP(ABC, 0, 2), RP(ABD, 0, 2)) /* ABC = ab . c | ABD = ab . d */
            val s5 = state(RP(c, 0, EOR))     /* c . */
            val s6 = state(RP(d, 0, EOR))     /* c . */
            val s7 = state(RP(ABC, 0, EOR))     /* ABC = abc . */
            val s8 = state(RP(S, 0, EOR))     /* S = ABC . */
            val s9 = state(RP(G, 0, EOR))     /* G = S .   */

            transition(null, s0, s1, WIDTH, setOf(b), setOf(), null)
            transition(s0, s1, s2, HEIGHT, setOf(b), setOf(setOf(UP)), listOf(RP(ABC, 0, SOR), RP(ABD, 0, SOR)))
            transition(s0, s2, s3, WIDTH, setOf(c, d), setOf(), null)
            transition(s2, s3, s4, GRAFT, setOf(c, d), setOf(setOf(UP)), listOf(RP(ABC, 0, 1), RP(ABD, 0, 1)))
            transition(s0, s4, s5, WIDTH, setOf(UP), setOf(), null)
            transition(s0, s4, s6, WIDTH, setOf(UP), setOf(), null)
            transition(s4, s5, s7, GRAFT, setOf(UP), setOf(setOf(UP)), listOf(RP(ABC, 0, 2)))
            transition(s0, s7, s8, HEIGHT, setOf(UP), setOf(setOf(UP)), listOf(RP(S, 0, 0)))
            transition(s0, s8, s9, GRAFT, setOf(UP), setOf(setOf(UP)), listOf(RP(G, 0, 0)))
            transition(null, s9, s9, GOAL, setOf(), setOf(), null)
        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun automaton_parse_abd() {
        val parser = ScanOnDemandParser(rrs)
        val (sppt, issues) = parser.parseForGoal("S", "abd", AutomatonKind.LOOKAHEAD_1)
        assertNotNull(sppt)
        assertEquals(0, issues.size)
        assertEquals(1, sppt.maxNumHeads)
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", 0, false) {
            val s0 = state(RP(G, 0, SOR))     /* G = . S   */
            val s1 = state(RP(a, 0, EOR))     /* a .       */
            val s2 = state(RP(ABC, 0, 1), RP(ABD, 0, 1)) /* ABC = a . bc | ABD = a . bd */
            val s3 = state(RP(b, 0, EOR))     /* b . */
            val s4 = state(RP(ABC, 0, 2), RP(ABD, 0, 2)) /* ABC = ab . c | ABD = ab . d */
            val s5 = state(RP(c, 0, EOR))     /* c . */
            val s6 = state(RP(d, 0, EOR))     /* c . */
            val s7 = state(RP(ABD, 0, EOR))     /* ABD = abd . */
            val s8 = state(RP(S, 1, EOR))     /* S = ABD . */
            val s9 = state(RP(G, 0, EOR))     /* G = S .   */

            transition(null, s0, s1, WIDTH, setOf(b), setOf(), null)
            transition(s0, s1, s2, HEIGHT, setOf(b), setOf(setOf(UP)), listOf(RP(ABC, 0, SOR), RP(ABD, 0, SOR)))
            transition(s0, s2, s3, WIDTH, setOf(c, d), setOf(), null)
            transition(s2, s3, s4, GRAFT, setOf(c, d), setOf(setOf(UP)), listOf(RP(ABC, 0, 1), RP(ABD, 0, 1)))
            transition(s0, s4, s5, WIDTH, setOf(UP), setOf(), null)
            transition(s0, s4, s6, WIDTH, setOf(UP), setOf(), null)
            transition(s4, s6, s7, GRAFT, setOf(UP), setOf(setOf(UP)), listOf(RP(ABD, 0, 2)))
            transition(s0, s7, s8, HEIGHT, setOf(UP), setOf(setOf(UP)), listOf(RP(S, 1, 0)))
            transition(s0, s8, s9, GRAFT, setOf(UP), setOf(setOf(UP)), listOf(RP(G, 0, 0)))
            transition(null, s9, s9, GOAL, setOf(), setOf(), null)
        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun buildFor() {
        val actual = rrs.buildFor("S", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))

        val sentences = setOf("abc", "abd")
        sentences.forEach {
            val parser = ScanOnDemandParser(rrs)
            val (sppt, issues) = parser.parseForGoal("S", it, AutomatonKind.LOOKAHEAD_1)
            assertNotNull(sppt, issues.joinToString("\n") { it.toString() } )
            assertEquals(0, issues.size)
            assertEquals(1, sppt.maxNumHeads)
        }

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", 0, false) {
            val s0 = state(RP(G, 0, SOR))     /* G = . S   */
            val s1 = state(RP(a, 0, EOR))     /* a .       */
            val s2 = state(RP(ABC, 0, 1), RP(ABD, 0, 1)) /* ABC = a . bc | ABD = a . bd */
            val s3 = state(RP(b, 0, EOR))     /* b . */
            val s4 = state(RP(ABC, 0, 2), RP(ABD, 0, 2)) /* ABC = ab . c | ABD = ab . d */
            val s5 = state(RP(c, 0, EOR))     /* c . */
            val s6 = state(RP(d, 0, EOR))     /* c . */
            val s7 = state(RP(ABC, 0, EOR))     /* ABC = abc . */
            val s8 = state(RP(ABD, 0, EOR))     /* ABD = abd . */
            val s9 = state(RP(S, 0, EOR))     /* S = ABC . */
            val s10 = state(RP(S, 1, EOR))     /* S = ABD . */
            val s11 = state(RP(G, 0, EOR))     /* G = S .   */

            transition(null, s0, s1, WIDTH, setOf(b), setOf(), null)
            transition(s0, s1, s2, HEIGHT, setOf(b), setOf(setOf(UP)), listOf(RP(ABC, 0, SOR), RP(ABD, 0, SOR)))
            transition(s0, s2, s3, WIDTH, setOf(c, d), setOf(), null)
            transition(s2, s3, s4, GRAFT, setOf(c, d), setOf(setOf(UP)), listOf(RP(ABC, 0, 1), RP(ABD, 0, 1)))
            transition(s0, s4, s5, WIDTH, setOf(UP), setOf(), null)
            transition(s0, s4, s6, WIDTH, setOf(UP), setOf(), null)
            transition(s4, s5, s7, GRAFT, setOf(UP), setOf(setOf(UP)), listOf(RP(ABC, 0, 2)))
            transition(s4, s6, s8, GRAFT, setOf(UP), setOf(setOf(UP)), listOf(RP(ABD, 0, 2)))
            transition(s0, s7, s9, HEIGHT, setOf(UP), setOf(setOf(UP)), listOf(RP(S, 0, 0)))
            transition(s0, s8, s10, HEIGHT, setOf(UP), setOf(setOf(UP)), listOf(RP(S, 1, 0)))
            transition(s0, s9, s11, GRAFT, setOf(UP), setOf(setOf(UP)), listOf(RP(G, 0, 0)))
            transition(s0, s10, s11, GRAFT, setOf(UP), setOf(setOf(UP)), listOf(RP(G, 0, 0)))
            transition(null, s11, s11, GOAL, setOf(), setOf(), null)
        }

        AutomatonTest.assertEquals(expected, actual)
    }
}