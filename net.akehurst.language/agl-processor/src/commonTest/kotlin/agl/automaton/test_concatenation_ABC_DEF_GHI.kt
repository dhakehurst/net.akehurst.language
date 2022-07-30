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
import net.akehurst.language.agl.runtime.structure.RulePosition
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.processor.AutomatonKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class test_concatenation_ABC_DEF_GHI : test_AutomatonAbstract() {
    // S = ABC DEF GHI
    // ABC =  AB C
    // AB = 'a' 'b'
    // C = 'c'
    // DEF = 'd' EF
    // EF = 'e' 'f'
    // GHI = 'g' 'h' 'i'

    // must be fresh per test or automaton is not correct for different parses (due to caching)
    private val rrs = runtimeRuleSet {
        concatenation("S") { ref("ABC"); ref("DEF"); ref("GHI") }
        concatenation("ABC") { ref("AB"); ref("C") }
        concatenation("DEF") { literal("d"); ref("EF") }
        concatenation("GHI") { literal("g");literal("h");literal("i") }
        concatenation("AB") { literal("a");literal("b") }
        concatenation("C") { literal("c") }
        concatenation("EF") { literal("e");literal("f") }
    }

    private val S = rrs.findRuntimeRule("S")
    private val SM = rrs.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
    private val G = SM.startState.runtimeRules.first()
    private val AB = rrs.findRuntimeRule("AB")
    private val ABC = rrs.findRuntimeRule("ABC")
    private val C = rrs.findRuntimeRule("C")
    private val DEF = rrs.findRuntimeRule("DEF")
    private val EF = rrs.findRuntimeRule("EF")

    private val a = rrs.findRuntimeRule("'a'")
    private val b = rrs.findRuntimeRule("'b'")
    private val c_T = rrs.findRuntimeRule("'c'")
    private val d = rrs.findRuntimeRule("'d'")
    private val e = rrs.findRuntimeRule("'e'")
    private val f = rrs.findRuntimeRule("'f'")
    private val g = rrs.findRuntimeRule("'g'")
    private val h = rrs.findRuntimeRule("'h'")
    private val i = rrs.findRuntimeRule("'i'")

    private val lhs_c = SM.createLookaheadSet(false, false, false, setOf(c_T))

    @Test
    fun parse_abcdefghi() {
        val parser = ScanOnDemandParser(rrs)
        parser.parseForGoal("S", "abcdefghi", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", 0, false) {
            val s0 = state(RP(G, 0, SOR))       // {}
            val s1 = state(RP(a, 0, EOR))       // {0}
            val s2 = state(RP(AB, 0, 1))   // {0}
            val s3 = state(RP(b, 0, EOR))       // {2}
            val s4 = state(RP(AB, 0, EOR))      // {0}
            val s5 = state(RP(ABC, 0, 1))  // {0}
            val s6 = state(RP(c_T, 0, EOR))       // {5}
            val s7 = state(RP(C, 0, EOR))       // {5}
            val s8 = state(RP(ABC, 0, EOR))     // {0}
            val s9 = state(RP(S, 0, 1))    // {0}
            val s10 = state(RP(d, 0, EOR))      // {9}
            val s11 = state(RP(DEF, 0, 1)) // {9}
            val s12 = state(RP(e, 0, EOR))      // {11}
            val s13 = state(RP(EF, 0, 1))  // {11}
            val s14 = state(RP(f, 0, EOR))      // {13}
            val s15 = state(RP(EF, 0, EOR))     // {11}
            val s16 = state(RP(c_T, 0, EOR))      // {}
            val s25 = state(RP(G, 0, EOR))      // {}

            transition(s0, s0, s1, WIDTH, setOf(b), setOf(), null)
            transition(s0, s1, s2, HEIGHT, setOf(b), setOf(setOf(EOT)), setOf(RP(S, 0, SOR)))
            transition(s0, s2, s3, WIDTH, setOf(c_T), setOf(), null)
            transition(s2, s3, s4, GRAFT, setOf(c_T), setOf(setOf(EOT)), setOf(RP(S, 0, 1)))
            transition(s0, s4, s5, WIDTH, setOf(EOT), setOf(), null)
            transition(s4, s5, s6, GRAFT, setOf(EOT), setOf(setOf(EOT)), setOf(RP(S, 0, 2)))
            transition(s0, s6, s7, GRAFT, setOf(EOT), setOf(setOf(EOT)), setOf(RP(G, 0, 0)))
        }
        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun buildFor() {
        val actual = rrs.buildFor("S", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))

        val parser = ScanOnDemandParser(rrs)
        parser.parseForGoal("S", "abcdefghi", AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", 0, false) {
            val s0 = state(RP(G, 0, SOR))
            val s1 = state(RP(a, 0, EOR))
            val s2 = state(RP(S, 0, 1))
            val s3 = state(RP(b, 0, EOR))
            val s4 = state(RP(S, 0, 2))
            val s5 = state(RP(c_T, 0, EOR))
            val s6 = state(RP(S, 0, EOR))
            val s7 = state(RP(G, 0, EOR))

            transition(s0, s0, s1, WIDTH, setOf(b), setOf(), null)
            transition(s0, s1, s2, HEIGHT, setOf(b), setOf(setOf(EOT)), setOf(RP(S, 0, SOR)))
            transition(s0, s2, s3, WIDTH, setOf(c_T), setOf(), null)
            transition(s2, s3, s4, GRAFT, setOf(c_T), setOf(setOf(EOT)), setOf(RP(S, 0, 1)))
            transition(s0, s4, s5, WIDTH, setOf(EOT), setOf(), null)
            transition(s4, s5, s6, GRAFT, setOf(EOT), setOf(setOf(EOT)), setOf(RP(S, 0, 2)))
            transition(s0, s6, s7, GRAFT, setOf(EOT), setOf(setOf(EOT)), setOf(RP(G, 0, 0)))
        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun compare() {
        val rrs_noBuild = runtimeRuleSet {
            concatenation("S") { ref("ABC"); ref("DEF"); ref("GHI") }
            concatenation("ABC") { ref("AB"); ref("C") }
            concatenation("DEF") { literal("d"); ref("EF") }
            concatenation("GHI") { literal("g");literal("h");literal("i") }
            concatenation("AB") { literal("a");literal("b") }
            concatenation("C") { literal("c") }
            concatenation("EF") { literal("e");literal("f") }
        }

        val rrs_preBuild = runtimeRuleSet {
            concatenation("S") { ref("ABC"); ref("DEF"); ref("GHI") }
            concatenation("ABC") { ref("AB"); ref("C") }
            concatenation("DEF") { literal("d"); ref("EF") }
            concatenation("GHI") { literal("g");literal("h");literal("i") }
            concatenation("AB") { literal("a");literal("b") }
            concatenation("C") { literal("c") }
            concatenation("EF") { literal("e");literal("f") }
        }

        val parser = ScanOnDemandParser(rrs_noBuild)
        val sentences = listOf("abcdefghi")
        for(sen in sentences) {
            val result = parser.parseForGoal("S", sen, AutomatonKind.LOOKAHEAD_1)
            if (result.issues.isNotEmpty())  result.issues.forEach { println(it) }
        }
        val automaton_noBuild = rrs_noBuild.usedAutomatonFor("S")
        val automaton_preBuild = rrs_preBuild.buildFor("S",AutomatonKind.LOOKAHEAD_1)

        println("--Pre Build--")
        println(rrs_preBuild.usedAutomatonToString("S"))
        println("--No Build--")
        println(rrs_noBuild.usedAutomatonToString("S"))

        AutomatonTest.assertEquals(automaton_preBuild, automaton_noBuild)
    }
}