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
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.processor.AutomatonKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class test_b_aSc : test_AutomatonAbstract() {

    /*
        S = b | a S c ;
     */

    // must be fresh per test or automaton is not correct for different parses (due to caching)
    private val rrs = runtimeRuleSet {
        choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
            concatenation { literal("b") }
            concatenation { literal("a"); ref("S"); literal("c") }
        }
    }
    private val S = rrs.findRuntimeRule("S")
    private val SM = rrs.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
    private val G = SM.startState.runtimeRules.first()
    private val a = rrs.findRuntimeRule("'a'")
    private val b = rrs.findRuntimeRule("'b'")
    private val c = rrs.findRuntimeRule("'c'")

    @Test
    fun automaton_parse_b() {
        //given
        val parser = ScanOnDemandParser(rrs)
        val result = parser.parseForGoal("S", "b", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))
        assertNotNull(result.sppt, result.issues.toString())
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)
        //when
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(RP(G, o0, SOR))     /* G = . S   */
            state(RP(b, o0, EOR))     /* b . */
            state(RP(a, o0, EOR))     /* a .       */
            state(RP(S, o0, EOR))     /* S = b . */
            state(RP(G, o0, EOR))     /* G = S .   */

            trans(WIDTH) { src(G, o0, SR); tgt(a); lhg(setOf(b, a)); ctx(G, o0, SR) }
            trans(WIDTH) { src(G, o0, SR); tgt(b); lhg(EOT); ctx(G, o0, SR) }
            trans(GOAL) { src(S); tgt(G); lhg(EOT); ctx(G, o0, SR) }
            trans(HEIGHT) { src(b); tgt(S, o0, ER); lhg(setOf(EOT), setOf(EOT)); ctx(G, o0, SR) }
        }

        //then
        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun automaton_parse_abc() {
        //given
        val parser = ScanOnDemandParser(rrs)
        val result = parser.parseForGoal("S", "abc", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))
        assertNotNull(result.sppt, result.issues.toString())
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)
        //when
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(RP(G, o0, SR))      // G = . S
            state(RP(b, o0, ER))      // b .
            state(RP(a, o0, ER))      // a .
            state(RP(S, o1, p1))       // S = a . S c
            state(RP(S, o0, ER))      // S = b .
            state(RP(S, o1, p2))       // S = a S . c
            state(RP(c, o0, ER))      // c .
            state(RP(S, o1, ER))      // S = a S c .
            state(RP(G, o0, EOR))      // G = S .

            trans(WIDTH) { src(G, o0, SR); tgt(a); lhg(setOf(b, a)); ctx(G, o0, SR) }
            trans(WIDTH) { src(S, o1, p1); tgt(a); lhg(setOf(b, a)); ctx(G, o0, SR) }
            trans(WIDTH) { src(G, o0, SR); tgt(b); lhg(EOT); ctx(G, o0, SR) }
            trans(WIDTH) { src(S, o1, p1); tgt(b); lhg(c); ctx(G, o0, SR) }
            trans(WIDTH) { src(S, o1, p2); tgt(c); lhg(RT); ctx(G, o0, SR) }
            trans(GOAL) { src(S, o1, ER); tgt(G); lhg(EOT); ctx(G, o0, SR) }
            trans(HEIGHT) { src(b); tgt(S); lhg(setOf(c), setOf(c)); ctx(S, o1, p1) }
            trans(GRAFT) { src(c); tgt(S, o1, ER); lhg(RT); ctx(S, o1, p2) }
            trans(HEIGHT) { src(a); tgt(S, o1, p1); lhg(setOf(b, a), setOf(EOT)); ctx(G, o0, SR) }
            trans(GRAFT) { src(S); tgt(S, o1, p2); lhg(c); ctx(S, o1, p1) }
        }

        //then
        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun automaton_parse_b_abc() {
        //given
        val parser = ScanOnDemandParser(rrs)
        parser.parseForGoal("S", "b", AutomatonKind.LOOKAHEAD_1)
        val result = parser.parseForGoal("S", "abc", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))
        assertNotNull(result.sppt, result.issues.toString())
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)
        //when
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(RP(G, o0, SR))      // G = . S
            state(RP(b, o0, ER))      // b .
            state(RP(a, o0, ER))      // a .
            state(RP(S, o0, ER))      // S = b .
            state(RP(G, o0, EOR))      // G = S .
            state(RP(S, o1, p1))       // S = a . S c
            state(RP(S, o1, p2))       // S = a S . c
            state(RP(c, o0, ER))      // c .
            state(RP(S, o1, ER))      // S = a S c .

            trans(WIDTH) { src(G, o0, SR); tgt(a); lhg(setOf(b, a)); ctx(G, o0, SR) }
            trans(WIDTH) { src(S, o1, p1); tgt(a); lhg(setOf(b, a)); ctx(G, o0, SR) }
            trans(WIDTH) { src(G, o0, SR); tgt(b); lhg(EOT); ctx(G, o0, SR) }
            trans(WIDTH) { src(S, o1, p1); tgt(b); lhg(c); ctx(G, o0, SR) }
            trans(WIDTH) { src(S, o1, p2); tgt(c); lhg(RT); ctx(G, o0, SR) }
            trans(GOAL) { src(S, o0, ER); tgt(G); lhg(EOT); ctx(G, o0, SR) }
            trans(GOAL) { src(S, o1, ER); tgt(G); lhg(EOT); ctx(G, o0, SR) }
            trans(HEIGHT) { src(b); tgt(S); lhg(setOf(c), setOf(c)); lhg(setOf(EOT), setOf(EOT)); ctx(RP(S, o1, p1), RP(G, o0, SR)) }
            trans(GRAFT) { src(c); tgt(S, o1, ER); lhg(RT); ctx(S, o1, p2) }
            trans(HEIGHT) { src(a); tgt(S, o1, p1); lhg(setOf(b, a), setOf(EOT)); ctx(G, o0, SR) }
            trans(GRAFT) { src(S); tgt(S, o1, p2); lhg(c); ctx(S, o1, p1) }
        }

        //then
        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun automaton_parse_aabcc() {
        //given
        val parser = ScanOnDemandParser(rrs)
        val result = parser.parseForGoal("S", "aabcc", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))
        assertNotNull(result.sppt, result.issues.toString())
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)
        //when
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(RP(G, o0, SR))      // G = . S
            state(RP(b, o0, ER))      // b .
            state(RP(a, o0, ER))      // a .
            state(RP(S, o1, p1))      // S = a . S c
            state(RP(S, o0, ER))      // S = b .
            state(RP(S, o1, p2))      // S = a S . c
            state(RP(c, o0, ER))      // c .
            state(RP(S, o1, ER))      // S = a S c .
            state(RP(G, o0, EOR))     // G = S .

            trans(WIDTH) { src(G, o0, SR); tgt(a); lhg(setOf(b, a)); ctx(G, o0, SR) }
            trans(WIDTH) { src(S, o1, p1); tgt(a); lhg(setOf(b, a)); ctx(RP(G, o0, SR), RP(S, o1, p1)) }
            trans(WIDTH) { src(G, o0, SR); tgt(b); lhg(EOT); ctx(G, o0, SR) }
            trans(WIDTH) { src(S, o1, p1); tgt(b); lhg(c); ctx(RP(G, o0, SR), RP(S, o1, p1)) }
            trans(WIDTH) { src(S, o1, p2); tgt(c); lhg(RT); ctx(RP(G, o0, SR), RP(S, o1, p1)) }
            trans(GOAL) { src(S, o1, ER); tgt(G); lhg(EOT); ctx(G, o0, SR) }
            trans(HEIGHT) { src(b); tgt(S); lhg(setOf(c), setOf(c)); ctx(RP(S, o1, p1)) }
            trans(GRAFT) { src(c); tgt(S, o1, ER); lhg(RT); ctx(S, o1, p2) }
            trans(HEIGHT) { src(a); tgt(S, o1, p1); lhg(setOf(b, a), setOf(EOT, c)); ctx(RP(G, o0, SR), RP(S, o1, p1)) }
            trans(GRAFT) { src(S); tgt(S, o1, p2); lhg(c); ctx(S, o1, p1) }
            trans(GRAFT) { src(S, o1, ER); tgt(S, o1, p2); lhg(c); ctx(S, o1, p1) }
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
        val result = parser.parseForGoal("S", "aabcc", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))
        assertNotNull(result.sppt, result.issues.toString())
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)
        //when
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(RP(G, o0, SR))      // G = . S
            state(RP(b, o0, ER))      // b .
            state(RP(a, o0, ER))      // a .
            state(RP(S, o0, ER))      // S = b .
            state(RP(G, o0, ER))      // G = S .
            state(RP(S, o1, p1))      // S = a . S c
            state(RP(S, o1, p2))      // S = a S . c
            state(RP(c, o0, ER))      // c .
            state(RP(S, o1, ER))      // S = a S c .

            trans(WIDTH) { src(G, o0, SR); tgt(a); lhg(setOf(b, a)); ctx(G, o0, SR) }
            trans(WIDTH) { src(S, o1, p1); tgt(a); lhg(setOf(b, a)); ctx(RP(G, o0, SR), RP(S, o1, p1)) }
            trans(WIDTH) { src(G, o0, SR); tgt(b); lhg(EOT); ctx(G, o0, SR) }
            trans(WIDTH) { src(S, o1, p1); tgt(b); lhg(c); ctx(RP(G, o0, SR), RP(S, o1, p1)) }
            trans(WIDTH) { src(S, o1, p2); tgt(c); lhg(RT); ctx(RP(G, o0, SR), RP(S, o1, p1)) }
            trans(GOAL) { src(S, o0, ER); tgt(G); lhg(EOT); ctx(G, o0, SR) }
            trans(GOAL) { src(S, o1, ER); tgt(G); lhg(EOT); ctx(G, o0, SR) }
            trans(HEIGHT) { src(b); tgt(S); lhg(setOf(c), setOf(c)); lhg(setOf(EOT), setOf(EOT)); ctx(RP(G, o0, SR), RP(S, o1, p1)) }
            trans(HEIGHT) { src(a); tgt(S, o1, p1); lhg(setOf(b, a), setOf(EOT, c)); ctx(RP(G, o0, SR), RP(S, o1, p1)) }
            trans(GRAFT) { src(S); tgt(S, o1, p2); lhg(c); ctx(S, o1, p1) }
            trans(GRAFT) { src(S, o1, ER); tgt(S, o1, p2); lhg(c); ctx(S, o1, p1) }
            trans(GRAFT) { src(c); tgt(S, o1, ER); lhg(RT); ctx(S, o1, p2) }
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
            val result = parser.parseForGoal("S", it, AutomatonKind.LOOKAHEAD_1)
            assertNotNull(result.sppt, result.issues.toString())
            assertEquals(0, result.issues.size)
            assertEquals(1, result.sppt!!.maxNumHeads)
        }

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(RP(G, o0, SR))      // G = . S
            state(RP(G, o0, ER))      // G = S .
            state(RP(S, o0, ER))      // S = b .
            state(RP(S, o1, p1))      // S = a . S c
            state(RP(S, o1, p2))      // S = a S . c
            state(RP(S, o1, ER))      // S = a S c .
            state(RP(b, o0, ER))      // b .
            state(RP(a, o0, ER))      // a .
            state(RP(c, o0, ER))      // c .

            trans(WIDTH) { src(G, o0, SR); tgt(a); lhg(setOf(b, a)); ctx(G, o0, SR) }
            trans(WIDTH) { src(S, o1, p1); tgt(a); lhg(setOf(b, a)); ctx(RP(G, o0, SR), RP(S, o1, p1)) }
            trans(WIDTH) { src(G, o0, SR); tgt(b); lhg(EOT); ctx(G, o0, SR) }
            trans(WIDTH) { src(S, o1, p1); tgt(b); lhg(c); ctx(RP(G, o0, SR), RP(S, o1, p1)) }
            trans(WIDTH) { src(S, o1, p2); tgt(c); lhg(setOf(EOT, c)); ctx(RP(G, o0, SR), RP(S, o1, p1)) }
            trans(GOAL) { src(S, o0, ER); tgt(G); lhg(EOT); ctx(G, o0, SR) }
            trans(GOAL) { src(S, o1, ER); tgt(G); lhg(EOT); ctx(G, o0, SR) }
            trans(HEIGHT) { src(b); tgt(S); lhg(setOf(c), setOf(c)); lhg(setOf(EOT), setOf(EOT)); ctx(RP(G, o0, SR), RP(S, o1, p1)) }
            trans(HEIGHT) { src(a); tgt(S, o1, p1); lhg(setOf(b, a), setOf(EOT, c)); ctx(RP(G, o0, SR), RP(S, o1, p1)) }
            trans(GRAFT) { src(S); tgt(S, o1, p2); lhg(c); ctx(S, o1, p1) }
            trans(GRAFT) { src(S, o1, ER); tgt(S, o1, p2); lhg(c); ctx(S, o1, p1) }
            trans(GRAFT) { src(c); tgt(S, o1, ER); lhg(setOf(EOT, c)); ctx(S, o1, p2) }
        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun compare() {
        val rrs_noBuild = rrs.clone()
        val rrs_preBuild = rrs.clone()

        val parser = ScanOnDemandParser(rrs_noBuild)
        val sentences = listOf("b", "abc", "aabcc", "aaabccc")
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

        AutomatonTest.assertEquals(automaton_preBuild, automaton_noBuild)
    }
}
