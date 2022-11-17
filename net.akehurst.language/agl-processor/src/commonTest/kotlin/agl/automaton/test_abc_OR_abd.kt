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

import net.akehurst.language.agl.parser.ScanOnDemandParser
import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.RuntimeRuleSet
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
    private val SM = rrs.fetchStateSetFor("S", AutomatonKind.LOOKAHEAD_1)
    private val G = SM.startState.runtimeRules.first()
    private val ABC = rrs.findRuntimeRule("ABC")
    private val ABD = rrs.findRuntimeRule("ABD")
    private val a = rrs.findRuntimeRule("'a'")
    private val b = rrs.findRuntimeRule("'b'")
    private val c = rrs.findRuntimeRule("'c'")
    private val d = rrs.findRuntimeRule("'d'")

    @Test
    fun automaton_parse_abc() {
        val parser = ScanOnDemandParser(rrs as RuntimeRuleSet)
        val result = parser.parseForGoal("S", "abc", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))
        assertNotNull(result.sppt)
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            val s0 = state(G, 0, SOR)     /* G = . S   */
            val s1 = state(a, 0, EOR)     /* a .       */
            val s2 = state(ABC, 0, p1)    /* ABC = a . bc */
            val s21 = state(ABD, 0, p1)    /* ABD = a . bd */
            val s3 = state(b, 0, EOR)     /* b . */
            val s4 = state(ABC, 0, p2)    /* ABC = ab . c  */
            val s41 = state(ABD, 0, p2)    /*  ABD = ab . d */
            val s5 = state(c, 0, EOR)     /* c . */
            val s6 = state(d, 0, EOR)     /* c . */
            val s7 = state(ABC, 0, EOR)   /* ABC = abc . */
            val s8 = state(S, 0, EOR)     /* S = ABC . */
            val s9 = state(G, 0, EOR)     /* G = S .   */

            transition(WIDTH) { src(s0); tgt(s1); lhg(b); ctx(setOf(s0)) }
            transition(HEIGHT) { src(s1); tgt(s2); lhg(setOf(b), setOf(EOT)); ctx(setOf(s0)) }


            transition(s0, s2, s3, WIDTH, setOf(c, d), setOf(), null)
            transition(s2, s3, s4, GRAFT, setOf(c, d), setOf(setOf(EOT)), setOf(RP(ABC, 0, 1), RP(ABD, 0, 1)))
            transition(s0, s4, s5, WIDTH, setOf(EOT), setOf(), null)
            transition(s0, s4, s6, WIDTH, setOf(EOT), setOf(), null)
            transition(s4, s5, s7, GRAFT, setOf(EOT), setOf(setOf(EOT)), setOf(RP(ABC, 0, 2)))
            transition(s0, s7, s8, HEIGHT, setOf(EOT), setOf(setOf(EOT)), setOf(RP(S, 0, 0)))
            transition(s0, s8, s9, GRAFT, setOf(EOT), setOf(setOf(EOT)), setOf(RP(G, 0, 0)))

        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun automaton_parse_abd() {
        val parser = ScanOnDemandParser(rrs)
        val result = parser.parseForGoal("S", "abd", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))
        assertNotNull(result.sppt)
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
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

            transition(s0, s0, s1, WIDTH, setOf(b), setOf(), null)
            transition(s0, s1, s2, HEIGHT, setOf(b), setOf(setOf(EOT)), setOf(RP(ABC, 0, SOR), RP(ABD, 0, SOR)))
            transition(s0, s2, s3, WIDTH, setOf(c, d), setOf(), null)
            transition(s2, s3, s4, GRAFT, setOf(c, d), setOf(setOf(EOT)), setOf(RP(ABC, 0, 1), RP(ABD, 0, 1)))
            transition(s0, s4, s5, WIDTH, setOf(EOT), setOf(), null)
            transition(s0, s4, s6, WIDTH, setOf(EOT), setOf(), null)
            transition(s4, s6, s7, GRAFT, setOf(EOT), setOf(setOf(EOT)), setOf(RP(ABD, 0, 2)))
            transition(s0, s7, s8, HEIGHT, setOf(EOT), setOf(setOf(EOT)), setOf(RP(S, 1, 0)))
            transition(s0, s8, s9, GRAFT, setOf(EOT), setOf(setOf(EOT)), setOf(RP(G, 0, 0)))
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
            val result = parser.parseForGoal("S", it, AutomatonKind.LOOKAHEAD_1)
            assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
            assertEquals(0, result.issues.size)
            assertEquals(1, result.sppt!!.maxNumHeads)
        }

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            val s0 = state(RP(G, 0, SOR))     /* G = . S   */
            val s1 = state(RP(G, 0, EOR))     /* G = S .   */
            val s2 = state(RP(S, 0, EOR))     /* S = ABC . */
            val s3 = state(RP(S, 1, EOR))     /* S = ABD . */
            val s4 = state(RP(ABD, 0, EOR))     /* ABD = abd . */
            val s5 = state(RP(d, 0, EOR))     /* c . */
            val s6 = state(RP(b, 0, EOR))     /* b . */
            val s7 = state(RP(a, 0, EOR))     /* a .       */
            val s8 = state(RP(ABC, 0, EOR))     /* ABC = abc . */
            val s9 = state(RP(c, 0, EOR))     /* c . */
            val s10 = state(RP(ABC, 0, 1), RP(ABD, 0, 1)) /* ABC = a . bc | ABD = a . bd */
            val s11 = state(RP(ABC, 0, 2), RP(ABD, 0, 2)) /* ABC = ab . c | ABD = ab . d */

            transition(s0, s0, s7, WIDTH, setOf(b), setOf(), null)
            transition(s0, s2, s1, GOAL, setOf(EOT), setOf(setOf(EOT)), null)
            transition(s0, s3, s1, GOAL, setOf(EOT), setOf(setOf(EOT)), null)
            transition(s0, s4, s3, HEIGHT, setOf(EOT), setOf(setOf(EOT)), null)
            transition(s11, s5, s4, GRAFT, setOf(EOT), setOf(setOf(EOT)), setOf(RP(ABD, 0, 2)))
            transition(s10, s6, s11, GRAFT, setOf(d), setOf(setOf(EOT)), setOf(RP(ABD, 0, 1)))
            transition(s10, s6, s11, GRAFT, setOf(c), setOf(setOf(EOT)), setOf(RP(ABC, 0, 1)))
            transition(s0, s7, s10, HEIGHT, setOf(b), setOf(setOf(EOT)), null)
            transition(s0, s8, s2, HEIGHT, setOf(EOT), setOf(setOf(EOT)), null)
            transition(s11, s9, s8, GRAFT, setOf(EOT), setOf(setOf(EOT)), setOf(RP(ABC, 0, 2)))
            transition(s0, s10, s6, WIDTH, setOf(c, d), setOf(), null)
            transition(s0, s11, s5, WIDTH, setOf(EOT), setOf(), null)
            transition(s0, s11, s9, WIDTH, setOf(EOT), setOf(), null)
        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun compare() {
        val rrs_noBuild = (rrs as RuntimeRuleSet).clone()
        val rrs_preBuild = (rrs as RuntimeRuleSet).clone()

        val parser = ScanOnDemandParser(rrs_noBuild)
        val sentences = listOf("abc", "abd")
        for (sen in sentences) {
            val result = parser.parseForGoal("S", sen, AutomatonKind.LOOKAHEAD_1)
            if (result.issues.isNotEmpty()) result.issues.forEach { println(it) }
        }
        val automaton_noBuild = rrs_noBuild.usedAutomatonFor("S")
        val automaton_preBuild = rrs_preBuild.buildFor("S", AutomatonKind.LOOKAHEAD_1)

        println("--Pre Build--")
        println(rrs_preBuild.usedAutomatonToString("S"))
        println("--No Build--")
        println(rrs_noBuild.usedAutomatonToString("S"))

        AutomatonTest.assertMatches(
            automaton_preBuild, automaton_noBuild, AutomatonTest.MatchConfiguration(
                in_actual_substitue_lookahead_RT_with = setOf(EOT)
            )
        )
    }
}