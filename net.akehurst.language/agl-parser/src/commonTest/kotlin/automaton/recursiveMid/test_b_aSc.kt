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

class test_b_aSc : test_AutomatonAbstract() {

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
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        val result = parser.parseForGoal("S", "b")
        println(rrs.usedAutomatonToString("S"))
        assertNotNull(result.sppt, result.issues.toString())
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)
        //when
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(RP(G, oN, SOR))     /* G = . S   */
            state(RP(b, oN, EOR))     /* b . */
            state(RP(a, oN, EOR))     /* a .       */
            state(RP(S, oN, EOR))     /* S = b . */
            state(RP(G, oN, EOR))     /* G = S .   */

            trans(WIDTH) { src(G, oN, SR); tgt(a); lhg(setOf(b, a)); ctx(G, oN, SR) }
            trans(WIDTH) { src(G, oN, SR); tgt(b); lhg(EOT); ctx(G, oN, SR) }
            trans(GOAL) { src(S); tgt(G); lhg(EOT); ctx(G, oN, SR) }
            trans(HEIGHT) { src(b); tgt(S, oN, ER); lhg(setOf(EOT), setOf(EOT)); ctx(G, oN, SR) }
        }

        //then
        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun automaton_parse_abc() {
        //given
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        val result = parser.parseForGoal("S", "abc")
        println(rrs.usedAutomatonToString("S"))
        assertNotNull(result.sppt, result.issues.toString())
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)
        //when
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(RP(G, oN, SR))      // G = . S
            state(RP(b, oN, ER))      // b .
            state(RP(a, oN, ER))      // a .
            state(RP(S, o1, p1))       // S = a . S c
            state(RP(S, oN, ER))      // S = b .
            state(RP(S, o1, p2))       // S = a S . c
            state(RP(c, oN, ER))      // c .
            state(RP(S, o1, ER))      // S = a S c .
            state(RP(G, oN, EOR))      // G = S .

            trans(WIDTH) { src(G, oN, SR); tgt(a); lhg(setOf(b, a)); ctx(G, oN, SR) }
            trans(WIDTH) { src(S, o1, p1); tgt(a); lhg(setOf(b, a)); ctx(G, oN, SR) }
            trans(WIDTH) { src(G, oN, SR); tgt(b); lhg(EOT); ctx(G, oN, SR) }
            trans(WIDTH) { src(S, o1, p1); tgt(b); lhg(c); ctx(G, oN, SR) }
            trans(WIDTH) { src(S, o1, p2); tgt(c); lhg(RT); ctx(G, oN, SR) }
            trans(GOAL) { src(S, o1, ER); tgt(G); lhg(EOT); ctx(G, oN, SR) }
            trans(HEIGHT) { src(b); tgt(S); lhg(setOf(c), setOf(c)); ctx(S, o1, p1) }
            trans(GRAFT) { src(c); tgt(S, o1, ER); lhg(RT); ctx(S, o1, p2) }
            trans(HEIGHT) { src(a); tgt(S, o1, p1); lhg(setOf(b, a), setOf(EOT)); ctx(G, oN, SR) }
            trans(GRAFT) { src(S); tgt(S, o1, p2); lhg(c); ctx(S, o1, p1) }
        }

        //then
        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun automaton_parse_b_abc() {
        //given
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        parser.parseForGoal("S", "b")
        val result = parser.parseForGoal("S", "abc")
        println(rrs.usedAutomatonToString("S"))
        assertNotNull(result.sppt, result.issues.toString())
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)
        //when
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(RP(G, oN, SR))      // G = . S
            state(RP(b, oN, ER))      // b .
            state(RP(a, oN, ER))      // a .
            state(RP(S, oN, ER))      // S = b .
            state(RP(G, oN, EOR))      // G = S .
            state(RP(S, o1, p1))       // S = a . S c
            state(RP(S, o1, p2))       // S = a S . c
            state(RP(c, oN, ER))      // c .
            state(RP(S, o1, ER))      // S = a S c .

            trans(WIDTH) { src(G, oN, SR); tgt(a); lhg(setOf(b, a)); ctx(G, oN, SR) }
            trans(WIDTH) { src(S, o1, p1); tgt(a); lhg(setOf(b, a)); ctx(G, oN, SR) }
            trans(WIDTH) { src(G, oN, SR); tgt(b); lhg(EOT); ctx(G, oN, SR) }
            trans(WIDTH) { src(S, o1, p1); tgt(b); lhg(c); ctx(G, oN, SR) }
            trans(WIDTH) { src(S, o1, p2); tgt(c); lhg(RT); ctx(G, oN, SR) }
            trans(GOAL) { src(S, oN, ER); tgt(G); lhg(EOT); ctx(G, oN, SR) }
            trans(GOAL) { src(S, o1, ER); tgt(G); lhg(EOT); ctx(G, oN, SR) }
            trans(HEIGHT) { src(b); tgt(S); lhg(setOf(c), setOf(c)); lhg(setOf(EOT), setOf(EOT)); ctx(RP(S, o1, p1), RP(G, oN, SR)) }
            trans(GRAFT) { src(c); tgt(S, o1, ER); lhg(RT); ctx(S, o1, p2) }
            trans(HEIGHT) { src(a); tgt(S, o1, p1); lhg(setOf(b, a), setOf(EOT)); ctx(G, oN, SR) }
            trans(GRAFT) { src(S); tgt(S, o1, p2); lhg(c); ctx(S, o1, p1) }
        }

        //then
        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun automaton_parse_aabcc() {
        //given
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        val result = parser.parseForGoal("S", "aabcc")
        println(rrs.usedAutomatonToString("S"))
        assertNotNull(result.sppt, result.issues.toString())
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)
        //when
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(RP(G, oN, SR))      // G = . S
            state(RP(b, oN, ER))      // b .
            state(RP(a, oN, ER))      // a .
            state(RP(S, o1, p1))      // S = a . S c
            state(RP(S, oN, ER))      // S = b .
            state(RP(S, o1, p2))      // S = a S . c
            state(RP(c, oN, ER))      // c .
            state(RP(S, o1, ER))      // S = a S c .
            state(RP(G, oN, EOR))     // G = S .

            trans(WIDTH) { src(G, oN, SR); tgt(a); lhg(setOf(b, a)); ctx(G, oN, SR) }
            trans(WIDTH) { src(S, o1, p1); tgt(a); lhg(setOf(b, a)); ctx(RP(G, oN, SR), RP(S, o1, p1)) }
            trans(WIDTH) { src(G, oN, SR); tgt(b); lhg(EOT); ctx(G, oN, SR) }
            trans(WIDTH) { src(S, o1, p1); tgt(b); lhg(c); ctx(RP(G, oN, SR), RP(S, o1, p1)) }
            trans(WIDTH) { src(S, o1, p2); tgt(c); lhg(RT); ctx(RP(G, oN, SR), RP(S, o1, p1)) }
            trans(GOAL) { src(S, o1, ER); tgt(G); lhg(EOT); ctx(G, oN, SR) }
            trans(HEIGHT) { src(b); tgt(S); lhg(setOf(c), setOf(c)); ctx(RP(S, o1, p1)) }
            trans(GRAFT) { src(c); tgt(S, o1, ER); lhg(RT); ctx(S, o1, p2) }
            trans(HEIGHT) { src(a); tgt(S, o1, p1); lhg(setOf(b, a), setOf(EOT, c)); ctx(RP(G, oN, SR), RP(S, o1, p1)) }
            trans(GRAFT) { src(S); tgt(S, o1, p2); lhg(c); ctx(S, o1, p1) }
            trans(GRAFT) { src(S, o1, ER); tgt(S, o1, p2); lhg(c); ctx(S, o1, p1) }
        }

        //then
        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun automaton_parse_b_abc_aabcc() {
        //given
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        parser.parseForGoal("S", "b")
        parser.parseForGoal("S", "abc")
        val result = parser.parseForGoal("S", "aabcc")
        println(rrs.usedAutomatonToString("S"))
        assertNotNull(result.sppt, result.issues.toString())
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)
        //when
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(RP(G, oN, SR))      // G = . S
            state(RP(b, oN, ER))      // b .
            state(RP(a, oN, ER))      // a .
            state(RP(S, oN, ER))      // S = b .
            state(RP(G, oN, ER))      // G = S .
            state(RP(S, o1, p1))      // S = a . S c
            state(RP(S, o1, p2))      // S = a S . c
            state(RP(c, oN, ER))      // c .
            state(RP(S, o1, ER))      // S = a S c .

            trans(WIDTH) { src(G, oN, SR); tgt(a); lhg(setOf(b, a)); ctx(G, oN, SR) }
            trans(WIDTH) { src(S, o1, p1); tgt(a); lhg(setOf(b, a)); ctx(RP(G, oN, SR), RP(S, o1, p1)) }
            trans(WIDTH) { src(G, oN, SR); tgt(b); lhg(EOT); ctx(G, oN, SR) }
            trans(WIDTH) { src(S, o1, p1); tgt(b); lhg(c); ctx(RP(G, oN, SR), RP(S, o1, p1)) }
            trans(WIDTH) { src(S, o1, p2); tgt(c); lhg(RT); ctx(RP(G, oN, SR), RP(S, o1, p1)) }
            trans(GOAL) { src(S, oN, ER); tgt(G); lhg(EOT); ctx(G, oN, SR) }
            trans(GOAL) { src(S, o1, ER); tgt(G); lhg(EOT); ctx(G, oN, SR) }
            trans(HEIGHT) { src(b); tgt(S); lhg(setOf(c), setOf(c)); lhg(setOf(EOT), setOf(EOT)); ctx(RP(G, oN, SR), RP(S, o1, p1)) }
            trans(HEIGHT) { src(a); tgt(S, o1, p1); lhg(setOf(b, a), setOf(EOT, c)); ctx(RP(G, oN, SR), RP(S, o1, p1)) }
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
            val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
            val result = parser.parseForGoal("S", it)
            assertNotNull(result.sppt, result.issues.toString())
            assertEquals(0, result.issues.size)
            assertEquals(1, result.sppt!!.maxNumHeads)
        }

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(RP(G, oN, SR))      // G = . S
            state(RP(G, oN, ER))      // G = S .
            state(RP(S, oN, ER))      // S = b .
            state(RP(S, o1, p1))      // S = a . S c
            state(RP(S, o1, p2))      // S = a S . c
            state(RP(S, o1, ER))      // S = a S c .
            state(RP(b, oN, ER))      // b .
            state(RP(a, oN, ER))      // a .
            state(RP(c, oN, ER))      // c .

            trans(WIDTH) { src(G, oN, SR); tgt(a); lhg(setOf(b, a)); ctx(G, oN, SR) }
            trans(WIDTH) { src(S, o1, p1); tgt(a); lhg(setOf(b, a)); ctx(RP(G, oN, SR), RP(S, o1, p1)) }
            trans(WIDTH) { src(G, oN, SR); tgt(b); lhg(EOT); ctx(G, oN, SR) }
            trans(WIDTH) { src(S, o1, p1); tgt(b); lhg(c); ctx(RP(G, oN, SR), RP(S, o1, p1)) }
            trans(WIDTH) { src(S, o1, p2); tgt(c); lhg(setOf(EOT, c)); ctx(RP(G, oN, SR), RP(S, o1, p1)) }
            trans(GOAL) { src(S, oN, ER); tgt(G); lhg(EOT); ctx(G, oN, SR) }
            trans(GOAL) { src(S, o1, ER); tgt(G); lhg(EOT); ctx(G, oN, SR) }
            trans(HEIGHT) { src(b); tgt(S); lhg(setOf(c), setOf(c)); lhg(setOf(EOT), setOf(EOT)); ctx(RP(G, oN, SR), RP(S, o1, p1)) }
            trans(HEIGHT) { src(a); tgt(S, o1, p1); lhg(setOf(b, a), setOf(EOT, c)); ctx(RP(G, oN, SR), RP(S, o1, p1)) }
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

        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs_noBuild.nonSkipTerminals), rrs_noBuild)
        val sentences = listOf("b", "abc", "aabcc", "aaabccc")
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
