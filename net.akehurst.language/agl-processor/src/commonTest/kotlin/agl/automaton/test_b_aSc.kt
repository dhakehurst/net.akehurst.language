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
import kotlin.test.assertNotNull

internal class test_b_aSc : test_AutomatonAbstract() {

    /*
        S = b | a S c ;

        S = b | S1
        S1 = a S c
     */

    // must be fresh per test or automaton is not correct for different parses (due to caching)
    private val rrs = runtimeRuleSet {
        choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
            literal("b")
            ref("S1")
        }
        concatenation("S1") { literal("a"); ref("S"); literal("c") }
    }
    private val S = rrs.findRuntimeRule("S")
    private val SM = rrs.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
    private val G = SM.startState.runtimeRules.first()
    private val S1 = rrs.findRuntimeRule("S1")
    private val a = rrs.findRuntimeRule("'a'")
    private val b = rrs.findRuntimeRule("'b'")
    private val c = rrs.findRuntimeRule("'c'")

    @Test
    fun closures() {
        val ffc = FirstFollowCache(SM)

        val actual = ffc.calcAllClosures(FirstFollowCache.Companion.ClosureItemRoot(RP(G, 0, SOR), RP(S1, 0, 2), emptyList()))
        val expected = setOf(
            FirstFollowCache.Companion.ClosureItemRoot(RP(G, 0, SOR), RP(G, 0, SOR), emptyList())
        )

        assertEquals(expected, actual)
    }

    @Test
    fun automaton_parse_b() {
        //given
        val parser = ScanOnDemandParser(rrs)
        val (sppt, issues) = parser.parseForGoal("S", "b", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))
        assertNotNull(sppt)
        assertEquals(0, issues.size)
        assertEquals(1, sppt.maxNumHeads)
        //when
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", 0, false) {
            val s0 = state(RP(G, 0, SOR))     /* G = . S   */
            val s1 = state(RP(b, 0, EOR))     /* b . */
            val s2 = state(RP(a, 0, EOR))     /* a .       */
            val s3 = state(RP(S, 0, EOR))     /* S = b . */
            val s4 = state(RP(G, 0, EOR))     /* G = S .   */

            transition(s0, s0, s1, WIDTH, setOf(EOT), setOf(), null)
            transition(s0, s0, s2, WIDTH, setOf(b, a), setOf(), null)
            transition(s0, s1, s3, HEIGHT, setOf(EOT), setOf(setOf(EOT)), setOf(RP(S, 0, 0)))
            transition(s0, s3, s4, GOAL, setOf(EOT), setOf(setOf(EOT)), setOf(RP(G, 0, 0)))
        }

        //then
        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun automaton_parse_abc() {
        //given
        val parser = ScanOnDemandParser(rrs)
        val (sppt, issues) = parser.parseForGoal("S", "abc", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))
        assertNotNull(sppt)
        assertEquals(0, issues.size)
        assertEquals(1, sppt.maxNumHeads)
        //when
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

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

            transition(s0, s0, s1, WIDTH, setOf(EOT), setOf(), null)
            transition(s0, s0, s2, WIDTH, setOf(a, b), setOf(), null)
            transition(s3, s1, s4, HEIGHT, setOf(c), setOf(setOf(c)), setOf(RP(S, 0, 0)))
            transition(s0, s2, s3, HEIGHT, setOf(a, b), setOf(setOf(EOT)), setOf(RP(S1, 0, SOR)))
            transition(s0, s3, s1, WIDTH, setOf(c), setOf(), null)
            transition(s0, s3, s2, WIDTH, setOf(a, b), setOf(), null)
            transition(s3, s4, s5, GRAFT, setOf(c), setOf(setOf(EOT)), setOf(RP(S1, 0, 1)))
            transition(s0, s5, s6, WIDTH, setOf(EOT), setOf(), null)
            transition(s5, s6, s7, GRAFT, setOf(EOT), setOf(setOf(EOT)), setOf(RP(S1, 0, 2)))
            transition(s0, s7, s8, HEIGHT, setOf(EOT), setOf(setOf(EOT)), setOf(RP(S, 1, SOR)))
            transition(s0, s8, s9, GRAFT, setOf(EOT), setOf(setOf(EOT)), setOf(RP(G, 0, SOR)))

        }

        //then
        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun automaton_parse_b_abc() {
        //given
        val parser = ScanOnDemandParser(rrs)
        parser.parseForGoal("S", "b", AutomatonKind.LOOKAHEAD_1)
        val (sppt, issues) = parser.parseForGoal("S", "abc", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))
        assertNotNull(sppt)
        assertEquals(0, issues.size)
        assertEquals(1, sppt.maxNumHeads)
        //when
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", 0, false) {
            val s0 = state(RP(G, 0, SOR))     /* G = . S   */
            val s1 = state(RP(b, 0, EOR))     /* b . */
            val s2 = state(RP(a, 0, EOR))     /* a .       */
            val s3 = state(RP(S, 0, EOR))     /* S = b . */
            val s4 = state(RP(G, 0, EOR))     /* G = S .   */
            val s5 = state(RP(S1, 0, 1)) /* S1 = a . S c  */
            val s6 = state(RP(S1, 0, 2)) /* S1 = a S . c  */
            val s7 = state(RP(c, 0, EOR))     /* c .  */
            val s8 = state(RP(S1, 0, EOR))    /* S1 = a S c . */
            val s9 = state(RP(S, 1, EOR))    /*  S = S1 . */

            transition(s0, s0, s1, WIDTH, setOf(EOT), setOf(), null)
            transition(s0, s0, s2, WIDTH, setOf(b, a), setOf(), null)
            transition(s0, s1, s3, HEIGHT, setOf(EOT), setOf(setOf(EOT)), setOf(RP(S, 0, 0)))
            transition(s0, s1, s3, HEIGHT, setOf(c), setOf(setOf(c)), setOf(RP(S, 0, 0)))
            transition(s0, s2, s5, HEIGHT, setOf(b,a), setOf(setOf(EOT)), setOf(RP(S1, 0, 0)))
            transition(s0, s3, s4, GOAL, setOf(EOT), setOf(setOf(EOT)), null)
            transition(s5, s3, s6, GRAFT,  setOf(RP(G, 0, 0))) { lhg(setOf(c), setOf(c)); lhg(setOf(c), setOf(EOT)) }
            transition(s0, s5, s2, WIDTH, setOf(b, a), setOf(), null)
            transition(s0, s5, s1, WIDTH, setOf(c), setOf(), null)
            transition(s0, s6, s7, WIDTH, setOf(EOT), setOf(), null)
            transition(s6, s7, s8, GRAFT,  setOf(RP(S1, 0, 2))) { lhg(setOf(EOT), setOf(EOT)) }
            transition(s0, s8, s9, HEIGHT, setOf(RP(S, 1, 0))) { lhg(setOf(EOT), setOf(EOT)) }
            transition(s0, s9, s4, GOAL, null) { lhg(setOf(EOT), setOf()) }
        }

        //then
        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun automaton_parse_aabcc() {
        //given
        val parser = ScanOnDemandParser(rrs)
        val (sppt, issues) = parser.parseForGoal("S", "aabcc", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))
        assertNotNull(sppt, issues.joinToString(separator = "\n") { "$it" })
        assertEquals(0, issues.size)
        assertEquals(1, sppt.maxNumHeads)
        //when
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", 0, false) {
            val s0 = state(RP(G, 0, SOR))     /* G = . S   */
            val s1 = state(RP(a, 0, EOR))     /* b . */
            val s2 = state(RP(b, 0, EOR))     /* a .       */
            val s4 = state(RP(S, 0, EOR))     /* S = b . */
            val s9 = state(RP(G, 0, EOR))     /* G = S .   */
            val s3 = state(RP(S1, 0, 1)) /* S1 = a . S c  */
            val s5 = state(RP(S1, 0, 2)) /* S1 = a S . c  */
            val s6 = state(RP(c, 0, EOR))     /* c .  */
            val s7 = state(RP(S1, 0, EOR))    /* S1 = a S c . */
            val s8 = state(RP(S, 1, EOR))    /*  S = S1 . */

            transition(s0, s0, s1, WIDTH, null) { lhg(setOf(b,a)) }
            transition(s0, s0, s2, WIDTH, null) { lhg(setOf(EOT)) }
            transition(s0, s1, s3, HEIGHT, setOf(b,a), setOf(setOf(EOT)), setOf(RP(S1, 0, 0)))
            transition(s3, s1, s3, HEIGHT, setOf(b,a), setOf(setOf(c)), setOf(RP(S1, 0, 0)))
            transition(s0, s2, s4, HEIGHT, setOf(c), setOf(setOf(c)), setOf(RP(S, 0, 0)))
            transition(setOf(s0,s3), s3, s1, WIDTH, null) { lhg(setOf(b,a)) }
            transition(setOf(s0,s3), s3, s2, WIDTH,  null) { lhg(setOf(c)) }
            transition(s3, s4, s5, GRAFT, null) { lhg(setOf(c), setOf(EOT)); lhg(setOf(c), setOf(c)) }
            transition(setOf(s0, s3), s5, s6, WIDTH, null) { lhg(setOf(EOT)) }
            transition(s5, s6, s7, GRAFT, setOf(EOT), setOf(setOf(EOT)), null)
            transition(s0, s7, s8, HEIGHT,  setOf(RP(S, 1, 0))) { lhg(setOf(EOT), setOf(EOT)) }
            transition(s3, s7, s8, HEIGHT, setOf(RP(S, 1, 0))) { lhg(setOf(c), setOf(c)) }
            transition(s3, s8, s5, GRAFT, setOf(RP(S1, 0, 1))){ lhg(setOf(c), setOf(EOT)) }
            transition(s0, s8, s9, GOAL, setOf(RP(S1, 0, 1))){ lhg(setOf(EOT)) }
        }

        //then
        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun automaton_parse_b_abc_aabcc() {
        //given
        val parser = ScanOnDemandParser(rrs)
        parser.parseForGoal("S", "b", AutomatonKind.LOOKAHEAD_1)
        parser.parseForGoal("S", "abc", AutomatonKind.LOOKAHEAD_1)
        val (sppt, issues) = parser.parseForGoal("S", "aabcc", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))
        assertNotNull(sppt, issues.joinToString(separator = "\n") { "$it" })
        assertEquals(0, issues.size)
        assertEquals(1, sppt.maxNumHeads)
        //when
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", 0, false) {
            val s0 = state(RP(G, 0, SOR))     /* G = . S   */
            val s1 = state(RP(b, 0, EOR))     /* b . */
            val s2 = state(RP(a, 0, EOR))     /* a .       */
            val s3 = state(RP(S1, 0, 1))     /* S1 = a . S c */
            val s4 = state(RP(S, 0, EOR))     /* S = b . */
            val s5 = state(RP(S1, 0, 2))     /* S1 = a S . c */
            val s6 = state(RP(c, 0, EOR))     /* c .       */
            val s7 = state(RP(S1, 0, EOR))     /* S1 = a S c . */
            val s8 = state(RP(S, 1, EOR))     /* S = S1 . */
            val s9 = state(RP(G, 0, EOR))     /* G = S .   */

            transition(s0, s0, s1, WIDTH, setOf(EOT), setOf(), null)
            transition(s0, s0, s2, WIDTH, setOf(b, a), setOf(), null)
            transition(s3, s1, s4, HEIGHT, setOf(c), setOf(setOf(c)), setOf(RP(S, 0, 0)))
            transition(s0, s3, s4, GRAFT, setOf(EOT), setOf(setOf(EOT)), setOf(RP(G, 0, 0)))
            transition(s0, s4, s4, GOAL, setOf(), setOf(), null)
        }

        //then
        AutomatonTest.assertEquals(expected, actual)
    }


    @Test
    fun buildFor() {
        val actual = rrs.buildFor("S", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))

        val sentences = setOf("b", "abc", "aabcc")
        sentences.forEach {
            println(it)
            val parser = ScanOnDemandParser(rrs)
            val (sppt, issues) = parser.parseForGoal("S", it, AutomatonKind.LOOKAHEAD_1)
            assertNotNull(sppt, issues.joinToString(separator = "\n") { "$it" })
            assertEquals(0, issues.size)
            assertEquals(1, sppt.maxNumHeads)
        }

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", 0, false) {
            val s0 = state(RP(G, 0, SOR))     /* G = . S   */
            val s1 = state(RP(G, 0, EOR))     /* G = S .   */
            val s2 = state(RP(S, 0, EOR))     /* S = b . */
            val s3 = state(RP(S, 1, EOR))     /* S = S1 . */
            val s4 = state(RP(S1, 0, EOR))     /* S1 = a S c . */
            val s5 = state(RP(c, 0, EOR))     /* c .       */
            val s6 = state(RP(a, 0, EOR))     /* a .       */
            val s7 = state(RP(b, 0, EOR))     /* b . */
            val s8 = state(RP(S1, 0, 1))     /* S1 = a . S c */
            val s9 = state(RP(S1, 0, 2))     /* S1 = a S . c */

            transition(s0, s0, s7, WIDTH, setOf(EOT), setOf(), null)
            transition(s0, s0, s6, WIDTH, setOf(b, a), setOf(), null)
            transition(s0, s2, s1, GOAL, setOf(EOT), setOf(), null)
            transition(s8, s2, s9, GRAFT, setOf(c), setOf(setOf(EOT)), setOf(RP(S1,0,1)))
            transition(s0, s3, s1, GOAL, setOf(EOT), setOf(), null)
            transition(s8, s3, s9, GRAFT, setOf(c), setOf(setOf(EOT)), setOf(RP(S1,0,1)))// ** c-c
            transition(setOf(s0), s4, s3, HEIGHT, setOf(EOT), setOf(setOf(EOT)), null)
            transition(setOf(s8), s4, s3, HEIGHT, setOf(c), setOf(setOf(c)), null)
            transition(setOf(s9), s5, s4, GRAFT, setOf(RP(S1, 0, 2))) { lhg(setOf(EOT), setOf(EOT)); lhg(setOf(c), setOf(c)) }
            transition(setOf(s8), s6, s8, HEIGHT, null) { lhg(setOf(a, b), setOf(c)) }
            transition(setOf(s0), s6, s8, HEIGHT, null) { lhg(setOf(a, b), setOf(EOT)) }
            transition(setOf(s8), s7, s2, HEIGHT, null) { lhg(setOf(c), setOf(c)) }
            transition(setOf(s0), s7, s2, HEIGHT, null) { lhg(setOf(EOT), setOf(EOT)) }
            transition(setOf(s0, s8), s8, s7, WIDTH, setOf(c), setOf(), null)
            transition(setOf(s0, s8), s8, s6, WIDTH, setOf(b, a), setOf(), null)
            transition(setOf(s0, s8), s9, s5, WIDTH, setOf(EOT, c), setOf(), null)
        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun compare() {
        val rrs_noBuild = rrs.clone()
        val rrs_preBuild = rrs.clone()

        val parser = ScanOnDemandParser(rrs_noBuild)
        val sentences = listOf("b","abc","aabcc","aaabccc")
        for(sen in sentences) {
            val (sppt, issues) = parser.parseForGoal("S", sen, AutomatonKind.LOOKAHEAD_1)
            if (issues.isNotEmpty())  issues.forEach { println(it) }
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
