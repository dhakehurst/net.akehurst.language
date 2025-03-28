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
import testFixture.utils.AutomatonTest
import kotlin.test.Test

class test_AhoSetiUlman_4_54 : test_AutomatonAbstract() {
    // S = C C ;
    // C = c C | d ;
    //
    // S = C C ;
    // C = C1 | d ;
    // C1 = c C ;
    // must be fresh per test or automaton is not correct for different parses (due to caching)
    private val rrs = runtimeRuleSet {
        concatenation("S") { ref("C"); ref("C") }
        choice("C", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
            ref("C1")
            literal("d")
        }
        concatenation("C1") { literal("c"); ref("C") }
    }

    private val S = rrs.findRuntimeRule("S")
    private val SM = rrs.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

    private val C = rrs.findRuntimeRule("C")
    private val C1 = rrs.findRuntimeRule("C1")
    private val T_c = rrs.findRuntimeRule("'c'")
    private val T_d = rrs.findRuntimeRule("'d'")
    private val G = SM.startState.runtimeRules.first()

    private val lhs_cd = SM.createLookaheadSet(false, false, false, setOf(T_c, T_d))

    @Test
    fun parse_dd() {
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        parser.parseForGoal("S", "dd")
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))
        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {


        }
        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun buildFor() {
        val actual = rrs.buildFor("S", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            val s0 = state(RP(G, oN, SOR))       // G = . S
            val s1 = state(RP(T_c, oN, EOR))     // c
            val s2 = state(RP(T_d, oN, EOR))     // d
            val s3 = state(RP(C1, oN, 1))   // C1 = c . C
            val s4 = state(RP(C, o1, EOR))       // C = C1 .
            val s5 = state(RP(C1, oN, EOR))      // C1 = c C .
            val s6 = state(RP(C, oN, EOR))       // C = d .
            val s7 = state(RP(S, oN, 1))    // S = C . C
            val s8 = state(RP(S, oN, EOR))       // S = C C .
            val s9 = state(RP(G, oN, EOR))       // G = S .

            transition(s0, s0, s1, WIDTH, setOf(T_c, T_d), emptySet(), null)
            transition(s0, s0, s2, WIDTH, setOf(T_c, T_d), emptySet(), null)

            transition(s0, s1, s3, HEIGHT, setOf(T_c, T_d), setOf(setOf(T_c, T_d)), setOf(RP(C1, oN, 0)))
            transition(s3, s1, s3, HEIGHT, setOf(T_c, T_d), setOf(setOf(EOT)), setOf(RP(C1, oN, 0)))
            transition(s7, s1, s3, HEIGHT, setOf(T_c, T_d), setOf(setOf(EOT)), setOf(RP(C1, oN, 0)))

            transition(s3, s2, s4, HEIGHT, setOf(EOT), setOf(setOf(EOT)), setOf(RP(C, o1, 0)))
            transition(s7, s2, s4, HEIGHT, setOf(EOT), setOf(setOf(EOT)), setOf(RP(C, o1, 0)))
            transition(s0, s2, s4, HEIGHT, setOf(T_c, T_d), setOf(setOf(T_c, T_d)), setOf(RP(C, o1, 0)))

            transition(s0, s3, s1, WIDTH, setOf(T_c, T_d), emptySet(), null)
            transition(s0, s3, s2, WIDTH, setOf(T_c, T_d), emptySet(), null)
            transition(s3, s3, s1, WIDTH, setOf(T_c, T_d), emptySet(), null)
            transition(s3, s3, s2, WIDTH, setOf(EOT), emptySet(), null)
            transition(s7, s3, s1, WIDTH, setOf(T_c, T_d), emptySet(), null)
            transition(s7, s3, s2, WIDTH, setOf(EOT), emptySet(), null)

            transition(s3, s4, s5, GRAFT, setOf(EOT), setOf(setOf(EOT)), setOf(RP(C1, oN, 1)))
            transition(s7, s4, s8, GRAFT, setOf(EOT), setOf(setOf(EOT)), setOf(RP(S, oN, 1)))
            transition(s0, s4, s7, HEIGHT, setOf(T_c, T_d), setOf(setOf(EOT)), setOf(RP(S, oN, 0)))

            transition(s3, s5, s6, HEIGHT, setOf(EOT), setOf(setOf(EOT)), setOf(RP(C, oN, 0)))
            transition(s0, s5, s6, HEIGHT, setOf(T_c, T_d), setOf(setOf(T_c, T_d)), setOf(RP(C, oN, 0)))

            transition(s3, s6, s5, GRAFT, setOf(EOT), setOf(setOf(EOT)), setOf(RP(C1, oN, 1)))
            transition(s0, s6, s7, HEIGHT, setOf(T_c, T_d), setOf(setOf(EOT)), setOf(RP(S, oN, 0)))

            transition(s0, s7, s1, WIDTH, setOf(T_c, T_d), emptySet(), null)
            transition(s0, s7, s2, WIDTH, setOf(EOT), emptySet(), null)

            transition(s0, s8, s9, GRAFT, setOf(EOT), setOf(setOf(EOT)), setOf(RP(G, oN, 0)))

        }

        AutomatonTest.assertEquals(expected, actual)
    }
}