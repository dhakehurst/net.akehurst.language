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
import net.akehurst.language.agl.runtime.structure.RuntimeRuleRhs
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.processor.AutomatonKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class test_aObOcO : test_AutomatonAbstract() {
    /*
        S = a? b? c?;
     */
    // must be fresh per test or automaton is not correct for different parses (due to caching)
    private val rrs = runtimeRuleSet {
        concatenation("S") { ref("aOpt"); ref("bOpt"); ref("cOpt") }
        multi("aOpt", 0, 1, "'a'")
        multi("bOpt", 0, 1, "'b'")
        multi("cOpt", 0, 1, "'c'")
        literal("'a'", "a")
        literal("'b'", "b")
        literal("'c'", "c")
    }

    private val S = rrs.findRuntimeRule("S")
    private val SM = rrs.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
    private val aOpt = rrs.findRuntimeRule("aOpt")
    private val aOpt_E = EMPTY
    private val bOpt = rrs.findRuntimeRule("bOpt")
    private val bOpt_E = EMPTY
    private val cOpt = rrs.findRuntimeRule("cOpt")
    private val cOpt_E = EMPTY
    private val a = rrs.findRuntimeRule("'a'")
    private val b = rrs.findRuntimeRule("'b'")
    private val c = rrs.findRuntimeRule("'c'")
    private val G = SM.startState.runtimeRules.first()

    private val lhs_bcU = SM.createLookaheadSet(true, false, false, setOf(b, c))


    @Test
    fun parse_a() {
        val parser = ScanOnDemandParser(rrs)
        val result = parser.parseForGoal("S", "a", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))
        assertNotNull(result.sppt)
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            val s0 = state(RP(G, 0, SOR))
            val s1 = state(RP(a, 0, EOR))
            val s2 = state(RP(aOpt_E, 0, EOR))
            val s3 = state(RP(aOpt, 0, EOR))
            val s4 = state(RP(S, 0, 1))
            val s5 = state(RP(b, 0, EOR))
            val s6 = state(RP(bOpt_E, 0, EOR))
            val s7 = state(RP(bOpt, 1, EOR))
            val s8 = state(RP(S, 0, 2))
            val s9 = state(RP(c, 0, EOR))
            val s10 = state(RP(cOpt_E, 0, EOR))
            val s11 = state(RP(cOpt, 1, EOR))
            val s12 = state(RP(S, 0, EOR))
            val s13 = state(RP(G, 0, EOR))

            transition(s0, s0, s1, WIDTH, setOf(EOT,b,c), setOf(), null)
            transition(s0, s0, s2, WIDTH, setOf(EOT,b,c), setOf(), null)
            transition(s0, s1, s3, HEIGHT, setOf(EOT,b,c), setOf(setOf(EOT,b,c)), setOf( RP(aOpt, 0, SOR)))
            transition(s0, s3, s4, HEIGHT, setOf(EOT,b,c), setOf(setOf(EOT)), setOf( RP(S, 0, SOR)))
            transition(s0, s4, s5, WIDTH, setOf(EOT,c), setOf(), null)
            transition(s0, s4, s6, WIDTH, setOf(EOT,c), setOf(), null)
            transition(s4, s6, s7, HEIGHT, setOf(EOT,c), setOf(setOf(EOT,c)), setOf( RP(bOpt, 1, SOR)))
            transition(s4, s7, s8, GRAFT, setOf(EOT,c), setOf(setOf(EOT)), setOf( RP(S, 0, 1)))
            transition(s0, s8, s9, WIDTH, setOf(EOT), setOf(), null)
            transition(s0, s8, s10, WIDTH, setOf(EOT), setOf(), null)
            transition(s8, s10, s11, HEIGHT, setOf(EOT), setOf(setOf(EOT)), setOf( RP(cOpt, 1, SOR)))
            transition(s8, s11, s12, GRAFT, setOf(EOT), setOf(setOf(EOT)), setOf( RP(S, 0, 2)))
            transition(s0, s12, s13, GOAL, setOf(EOT), setOf(), null)
        }
        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun parse_b() {
        val parser = ScanOnDemandParser(rrs)
        val result = parser.parseForGoal("S", "b", AutomatonKind.LOOKAHEAD_1)
        assertNotNull(result.sppt)
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            val s0 = state(RP(G, 0, SOR))
            val s1 = state(RP(a, 0, EOR))
            val s2 = state(RP(aOpt_E, 0, EOR))
            val s3 = state(RP(aOpt, 1, EOR))
            val s4 = state(RP(S, 0, 1))
            val s5 = state(RP(b, 0, EOR))
            val s6 = state(RP(bOpt_E, 0, EOR))
            val s7 = state(RP(bOpt, 0, EOR))
            val s8 = state(RP(S, 0, 2))
            val s9 = state(RP(c, 0, EOR))
            val s10 = state(RP(cOpt_E, 0, EOR))
            val s11 = state(RP(cOpt, 1, EOR))
            val s12 = state(RP(S, 0, EOR))
            val s13 = state(RP(G, 0, EOR))

            transition(s0, s0, s1, WIDTH, setOf(EOT,b,c), setOf(), null)
            transition(s0, s0, s2, WIDTH, setOf(EOT,b,c), setOf(), null)
            transition(s0, s2, s3, HEIGHT, setOf(EOT,b,c), setOf(setOf(EOT,b,c)), setOf( RP(aOpt, 1, SOR)))
            transition(s0, s3, s4, HEIGHT, setOf(EOT,b,c), setOf(setOf(EOT)), setOf( RP(S, 0, SOR)))
            transition(s0, s4, s5, WIDTH, setOf(EOT,c), setOf(), null)
            transition(s0, s4, s6, WIDTH, setOf(EOT,c), setOf(), null)
            transition(s4, s5, s7, HEIGHT, setOf(EOT,c), setOf(setOf(EOT,c)), setOf( RP(bOpt, 0, SOR)))
            transition(s4, s7, s8, GRAFT, setOf(EOT,c), setOf(setOf(EOT)), setOf( RP(S, 0, 1)))
            transition(s0, s8, s9, WIDTH, setOf(EOT), setOf(), null)
            transition(s0, s8, s10, WIDTH, setOf(EOT), setOf(), null)
            transition(s8, s10, s11, HEIGHT, setOf(EOT), setOf(setOf(EOT)), setOf( RP(cOpt, 1, SOR)))
            transition(s8, s11, s12, GRAFT, setOf(EOT), setOf(setOf(EOT)), setOf( RP(S, 0, 2)))
            transition(s0, s12, s13, GOAL, setOf(EOT), setOf(),null)
        }
        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun parse_c() {
        val parser = ScanOnDemandParser(rrs)
        val result = parser.parseForGoal("S", "c", AutomatonKind.LOOKAHEAD_1)
        assertNotNull(result.sppt)
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            val s0 = state(RP(G, 0, SOR))
            val s1 = state(RP(a, 0, EOR))
            val s2 = state(RP(aOpt_E, 0, EOR))
            val s3 = state(RP(aOpt, 1, EOR))
            val s4 = state(RP(S, 0, 1))
            val s5 = state(RP(b, 0, EOR))
            val s6 = state(RP(bOpt_E, 0, EOR))
            val s7 = state(RP(bOpt, 1, EOR))
            val s8 = state(RP(S, 0, 2))
            val s9 = state(RP(c, 0, EOR))
            val s10 = state(RP(cOpt_E, 0, EOR))
            val s11 = state(RP(cOpt, 0, EOR))
            val s12 = state(RP(S, 0, EOR))
            val s13 = state(RP(G, 0, EOR))

            transition(s0, s0, s1, WIDTH, setOf(EOT,b,c), setOf(), null)
            transition(s0, s0, s2, WIDTH, setOf(EOT,b,c), setOf(), null)
            transition(s0, s2, s3, HEIGHT, setOf(EOT,b,c), setOf(setOf(EOT,b,c)), setOf( RP(aOpt, 1, SOR)))
            transition(s0, s3, s4, HEIGHT, setOf(EOT,b,c), setOf(setOf(EOT)), setOf( RP(S, 0, SOR)))
            transition(s0, s4, s5, WIDTH, setOf(EOT,c), setOf(), null)
            transition(s0, s4, s6, WIDTH, setOf(EOT,c), setOf(), null)
            transition(s4, s6, s7, HEIGHT, setOf(EOT,c), setOf(setOf(EOT,c)), setOf( RP(bOpt, 1, SOR)))
            transition(s4, s7, s8, GRAFT, setOf(EOT,c), setOf(setOf(EOT)), setOf( RP(S, 0, 1)))
            transition(s0, s8, s9, WIDTH, setOf(EOT), setOf(), null)
            transition(s0, s8, s10, WIDTH, setOf(EOT), setOf(), null)
            transition(s8, s9, s11, HEIGHT, setOf(EOT), setOf(setOf(EOT)), setOf( RP(cOpt, 0, SOR)))
            transition(s8, s11, s12, GRAFT, setOf(EOT), setOf(setOf(EOT)), setOf( RP(S, 0, 2)))
            transition(s0, s12, s13, GOAL, setOf(EOT), setOf(),null)
        }
        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun buildFor() {
        val actual = rrs.buildFor("S", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))

        val sentences = listOf("","a", "b","ab","c","ac","bc","abc")
        sentences.forEach {
            val parser = ScanOnDemandParser(rrs)
            val result = parser.parseForGoal("S", it, AutomatonKind.LOOKAHEAD_1)
            assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() } )
            assertEquals(0, result.issues.size)
            assertEquals(1, result.sppt!!.maxNumHeads)
        }

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            val s0 = state(RP(G, 0, SOR))
            val s1 = state(RP(a, 0, EOR))
            val s2 = state(RP(aOpt_E, 0, EOR))
            val s3 = state(RP(aOpt, 0, EOR))
            val s4 = state(RP(aOpt, 1, EOR))
            val s5 = state(RP(S, 0, 1))
            val s6 = state(RP(b, 0, EOR))
            val s7 = state(RP(bOpt_E, 0, EOR))
            val s8 = state(RP(bOpt, 0, EOR))
            val s9 = state(RP(bOpt, 1, EOR))
            val s10 = state(RP(S, 0, 2))
            val s11 = state(RP(c, 0, EOR))
            val s12 = state(RP(cOpt_E, 0, EOR))
            val s13 = state(RP(cOpt, 0, EOR))
            val s14 = state(RP(cOpt, 1, EOR))
            val s15 = state(RP(S, 0, EOR))
            val s16 = state(RP(G, 0, EOR))

            transition(s0, s0, s1, WIDTH, setOf(EOT,b,c), setOf(), null)
            transition(s0, s0, s2, WIDTH, setOf(EOT,b,c), setOf(), null)
            transition(s0, s1, s3, HEIGHT, setOf(EOT,b,c), setOf(setOf(EOT,b,c)), null)
            transition(s0, s2, s4, HEIGHT, setOf(EOT,b,c), setOf(setOf(EOT,b,c)), null)
            transition(s0, s3, s5, HEIGHT, setOf(EOT,b,c), setOf(setOf(EOT)), null)
            transition(s0, s4, s5, HEIGHT, setOf(EOT,b,c), setOf(setOf(EOT)), null)
            transition(s0, s5, s6, WIDTH, setOf(EOT,c), setOf(), null)
            transition(s0, s5, s7, WIDTH, setOf(EOT,c), setOf(), null)
            transition(s5, s6, s8, HEIGHT, setOf(EOT,c), setOf(setOf(EOT,c)), null)
            transition(s5, s7, s9, HEIGHT, setOf(EOT,c), setOf(setOf(EOT,c)),null)
            transition(s5, s8, s10, GRAFT, setOf(EOT,c), setOf(setOf(EOT)), setOf( RP(S, 0, 1)))
            transition(s5, s9, s10, GRAFT, setOf(EOT,c), setOf(setOf(EOT)), setOf( RP(S, 0, 1)))
            transition(s0, s10, s11, WIDTH, setOf(EOT), setOf(), null)
            transition(s0, s10, s12, WIDTH, setOf(EOT), setOf(), null)
            transition(s10, s11, s13, HEIGHT, setOf(EOT), setOf(setOf(EOT)), null)
            transition(s10, s12, s14, HEIGHT, setOf(EOT), setOf(setOf(EOT)), null)
            transition(s10, s13, s15, GRAFT, setOf(EOT), setOf(setOf(EOT)), setOf( RP(S, 0, 2)))
            transition(s10, s14, s15, GRAFT, setOf(EOT), setOf(setOf(EOT)), setOf( RP(S, 0, 2)))
            transition(s0, s15, s16, GOAL, setOf(EOT), setOf(),null)
        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun compare() {
        val rrs_noBuild = rrs.clone()
        val rrs_preBuild = rrs.clone()

        val parser = ScanOnDemandParser(rrs_noBuild)
        val sentences = listOf("","a", "b","ab","c","ac","bc","abc")
        for(sen in sentences) {
            val result = parser.parseForGoal("S", sen, AutomatonKind.LOOKAHEAD_1)
            if (result.issues.isNotEmpty()) {
                println("Sentence: $sen")
                result.issues.forEach { println(it) }
            }
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