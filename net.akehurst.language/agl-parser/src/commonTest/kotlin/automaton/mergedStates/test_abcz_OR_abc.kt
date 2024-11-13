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

package net.akehurst.language.automaton.leftcorner

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.automaton.api.AutomatonKind
import net.akehurst.language.parser.leftcorner.LeftCornerParser
import net.akehurst.language.regex.agl.RegexEnginePlatform
import net.akehurst.language.scanner.common.ScannerOnDemand
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class test_abcz_OR_abc : test_AutomatonAbstract() {

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
        concatenation("ABC") { literal("a"); literal("b"); literal("c") }
    }
    private val S = rrs.findRuntimeRule("S")
    private val SM = rrs.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
    private val G = SM.startState.runtimeRules.first()
    private val ABCZ = rrs.findRuntimeRule("ABCZ")
    private val ABC = rrs.findRuntimeRule("ABC")
    private val a = rrs.findRuntimeRule("'a'")
    private val b = rrs.findRuntimeRule("'b'")
    private val c = rrs.findRuntimeRule("'c'")
    private val z = rrs.findRuntimeRule("'z'")

    @Test
    fun automaton_parse_abc() {
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        val result = parser.parseForGoal("S", "abc")
        println(rrs.usedAutomatonToString("S"))
        assertNotNull(result.sppt)
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(RP(G, oN, SOR))                       // G = . S
            state(RP(a, oN, EOR))                       // a .
            state(RP(ABCZ, oN, p1), RP(ABC, oN, p1))    // ABCZ = a . b c z , ABC = a . b c
            state(RP(b, oN, EOR))                       // b .
            state(RP(ABCZ, oN, p2), RP(ABC, oN, p2))    // ABCZ = a b . c z , ABC = a b . c
            state(RP(c, oN, EOR))                       // c .
            state(RP(ABC, oN, EOR))                     // ABC = a b c .
            state(RP(ABCZ, oN, p3))                     // ABCZ = a b c . z
            state(RP(S, o1, EOR))                       // S = ABC .
            state(RP(G, oN, EOR))                       // G = S .


        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun automaton_parse_abcz() {
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        val result = parser.parseForGoal("S", "abcz")
        assertNotNull(result.sppt)
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))
        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            val s0 = state(RP(G, oN, SOR))                                // G = . S
            val s1 = state(RP(a, oN, ER))                                // a .
            val s2 = state(RP(ABCZ, oN, 1), RP(ABC, oN, 1))    // ABCZ = a . b c z , ABC = a . b c
            val s3 = state(RP(b, oN, ER))                                // b .
            val s4 = state(RP(ABCZ, oN, 2), RP(ABC, oN, 2))    // ABCZ = a b . c z , ABC = a b . c
            val s5 = state(RP(c, oN, ER))                                // c .
            val s6 = state(RP(ABCZ, oN, 3))                          // ABCZ = a b c . z
            val s7 = state(RP(ABC, oN, ER))                                // ABC = a b c .
            val s8 = state(RP(z, oN, ER))                                // z .
            val s9 = state(RP(ABCZ, oN, ER))                             // ABCZ = a b c z .
            val s10 = state(RP(S, oN, ER))                               // S = ABC .
            val s11 = state(RP(G, oN, ER))                               // G = S .

            transition(s0, s0, s1, WIDTH, setOf(b), emptySet(), null)
            transition(s0, s1, s2, HEIGHT, setOf(b), setOf(setOf(EOT)), setOf(RP(ABCZ, oN, 0), RP(ABC, oN, 0)))
            transition(s0, s2, s3, WIDTH, setOf(c), setOf(), null)
            transition(s2, s3, s4, GRAFT, setOf(c), setOf(setOf(EOT)), setOf(RP(ABCZ, oN, 1), RP(ABC, oN, 1)))
            transition(s0, s4, s5, WIDTH, setOf(EOT, z), setOf(), null)
            transition(s4, s5, s6, GRAFT, setOf(z), setOf(setOf(EOT)), setOf(RP(ABCZ, oN, 2)))
            transition(s4, s5, s7, GRAFT, setOf(EOT), setOf(setOf(EOT)), setOf(RP(ABC, oN, 2)))
            transition(s0, s6, s8, WIDTH, setOf(EOT), setOf(), null)
            transition(s6, s8, s9, GRAFT, setOf(EOT), setOf(setOf(EOT)), setOf(RP(ABCZ, oN, 3)))
            transition(s0, s9, s10, HEIGHT, setOf(EOT), setOf(setOf(EOT)), setOf(RP(S, oN, SOR)))
            transition(s0, s10, s11, GRAFT, setOf(EOT), setOf(setOf(EOT)), setOf(RP(G, oN, SOR)))

        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun stateInfo() {
        val bc = BuildCacheLC1(SM)

        val actual = bc.stateInfo()
    }

    @Test
    fun buildFor() {
        val actual = rrs.buildFor("S", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))

        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        val sentences = listOf("abcz", "abc")
        for (sent in sentences) {
            println("Parsing sentence '$sent'")
            val result = parser.parseForGoal("S", sent)
            assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
            assertEquals(0, result.issues.size)
            assertEquals(1, result.sppt!!.maxNumHeads)
        }

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            val s0 = state(RP(G, oN, SOR))                                // G = . S
            val s1 = state(RP(G, oN, ER))                               // G = S .
            val s2 = state(RP(S, oN, ER))                               // S = ABCZ .
            val s3 = state(RP(S, o1, ER))                               // S = ABC .
            val s4 = state(RP(ABC, oN, ER))                                // ABC = a b c .
            val s5 = state(RP(c, oN, ER))                                // c .
            val s6 = state(RP(b, oN, ER))                                // b .
            val s7 = state(RP(a, oN, ER))                                // a .
            val s8 = state(RP(ABCZ, oN, ER))                             // ABCZ = a b c z .
            val s9 = state(RP(z, oN, ER))                                // z .
            val s10 = state(RP(ABCZ, oN, 1), RP(ABC, oN, 1))    // ABCZ = a . b c z , ABC = a . b c
            val s11 = state(RP(ABCZ, oN, 2), RP(ABC, oN, 2))    // ABCZ = a b . c z , ABC = a b . c
            val s12 = state(RP(ABCZ, oN, 3))                          // ABCZ = a b c . z

            transition(s0, s0, s7, WIDTH, setOf(b), emptySet(), null)
            transition(s0, s2, s1, GOAL, setOf(EOT), setOf(setOf()), null)
            transition(s0, s3, s1, GOAL, setOf(EOT), setOf(setOf()), null)
            transition(s0, s4, s3, HEIGHT, setOf(EOT), setOf(setOf(EOT)), null)
            transition(s11, s5, s4, GRAFT, setOf(EOT), setOf(setOf(EOT)), setOf(RP(ABCZ, oN, 2), RP(ABC, oN, 2)))
            transition(s11, s5, s12, GRAFT, setOf(z), setOf(setOf(EOT)), setOf(RP(ABCZ, oN, 2), RP(ABC, oN, 2)))
            transition(s10, s6, s11, GRAFT, setOf(c), setOf(setOf(EOT)), setOf(RP(ABCZ, oN, 1), RP(ABC, oN, 1)))
            transition(s0, s7, s10, HEIGHT, setOf(b), setOf(setOf(EOT)), null)
            transition(s0, s8, s2, HEIGHT, setOf(EOT), setOf(setOf(EOT)), null)
            transition(s12, s9, s8, GRAFT, setOf(EOT), setOf(setOf(EOT)), setOf(RP(ABCZ, oN, 3)))
            transition(s0, s10, s6, WIDTH, setOf(c), setOf(), null)
            transition(s0, s11, s5, WIDTH, setOf(EOT, z), setOf(), null)
            transition(s0, s12, s9, WIDTH, setOf(EOT), setOf(), null)
        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun compare() {
        val rrs_noBuild = rrs.clone()
        val rrs_preBuild = rrs.clone()

        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs_noBuild.nonSkipTerminals), rrs_noBuild)
        val sentences = listOf("abc", "abcz")
        for (sen in sentences) {
            val result = parser.parseForGoal("S", sen)
            if (result.issues.isNotEmpty()) result.issues.forEach { println(it) }
        }
        val automaton_noBuild = rrs_noBuild.usedAutomatonFor("S")
        val automaton_preBuild = rrs_preBuild.buildFor("S", AutomatonKind.LOOKAHEAD_1)

        println("--Pre Build--")
        println(rrs_preBuild.usedAutomatonToString("S"))
        println("--No Build--")
        println(rrs_noBuild.usedAutomatonToString("S"))

        AutomatonTest.assertEquals(automaton_preBuild, automaton_noBuild)
    }
}