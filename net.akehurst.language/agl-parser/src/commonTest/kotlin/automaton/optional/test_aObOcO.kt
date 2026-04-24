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

import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.automaton.api.AutomatonKind
import net.akehurst.language.parser.leftcorner.LeftCornerParser
import net.akehurst.language.regex.agl.RegexEnginePlatform
import net.akehurst.language.scanner.common.ScannerOnDemand
import testFixture.utils.AutomatonTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class test_aObOcO : test_AutomatonAbstract() {
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
    private val rG = SM.startState.runtimeRules.first()

    private val lhs_bcU = SM.createLookaheadSet(true, false, false, setOf(b, c))


    @Test
    fun parse_a() {
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        val result = parser.parseForGoal("S", "a")
        println(rrs.usedAutomatonToString("S"))
        assertNotNull(result.sppt)
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(RP(rG, oN, SOR))       // G = . S
            state(RP(EMPTY_LIST, oN, EOR)) // EMPTY_LIST .
            state(RP(a, oN, EOR))       // a .
            state(RP(aOpt, oLI, EOR)) // aOpt = [a] .
            state(RP(S, oN, p1))     // S = aOpt . bOpt cOpt
            state(RP(b, oN, EOR))     // b .
            state(RP(bOpt, oLE, EOR)) // bOpt = [EMPTY_LIST] .
            state(RP(S, oN, p2))     // S = aOpt bOpt . cOpt
            state(RP(c, oN, EOR))     // c .
            state(RP(cOpt, oLE, EOR)) // cOpt = [EMPTY_LIST] .
            state(RP(S, oN, EOR))     // S = aOpt bOpt cOpt .
            state(RP(rG, oN, EOR))     // G = S .

            trans(WIDTH) { src(rG, oN, SOR); tgt(EMPTY_LIST); lhg(setOf(EOT, b, c)); ctx(rG, oN, SOR) }
            trans(WIDTH) { src(S, oN, p1); tgt(EMPTY_LIST); lhg(setOf(RT, c)); ctx(rG, oN, SOR) }
            trans(WIDTH) { src(S, oN, p2); tgt(EMPTY_LIST); lhg(setOf(RT)); ctx(rG, oN, SOR) }
            trans(WIDTH) { src(rG, oN, SOR); tgt(a); lhg(setOf(EOT, b, c)); ctx(rG, oN, SOR) }
            trans(WIDTH) { src(S, oN, p1); tgt(b); lhg(setOf(RT, c)); ctx(rG, oN, SOR) }
            trans(WIDTH) { src(S, oN, p2); tgt(c); lhg(setOf(RT)); ctx(rG, oN, SOR) }
            trans(GOAL) { src(S);tgt(rG); lhg(EOT); ctx(rG, oN, SOR) }
            trans(GRAFT) { src(cOpt, oLE, EOR); tgt(S); lhg(RT); ctx(S, oN, p2) }
            trans(HEIGHT) { src(aOpt, oLI, EOR); tgt(S, oN, p1); lhg(setOf(EOT, b, c), setOf(EOT)); ctx(rG, oN, SOR) }
            trans(GRAFT) { src(bOpt, oLE, EOR); tgt(S, oN, p2); lhg(setOf(RT, c)); ctx(S, oN, p1) }
            trans(HEIGHT) { src(a); tgt(aOpt, oLI, EOR); lhg(setOf(EOT, b, c), setOf(EOT, b, c)); ctx(rG, oN, SOR) }
            trans(HEIGHT) { src(EMPTY_LIST); tgt(bOpt, oLE, EOR); lhg(setOf(RT, c), setOf(RT, c)); ctx(S, oN, p1) }
            trans(HEIGHT) { src(EMPTY_LIST); tgt(cOpt, oLE, EOR); lhg(setOf(RT), setOf(RT)); ctx(S, oN, p2) }
        }
        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun parse_b() {
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        val result = parser.parseForGoal("S", "b")
        println(rrs.usedAutomatonToString("S"))
        assertNotNull(result.sppt)
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(RP(rG, oN, SOR))       // G = . S
            state(RP(EMPTY_LIST, oN, EOR)) // EMPTY_LIST .
            state(RP(a, oN, EOR))       // a .
            state(RP(aOpt, oLE, EOR)) // aOpt = [EMPTY_LIST] .
            state(RP(S, oN, p1))     // S = aOpt . bOpt cOpt
            state(RP(b, oN, EOR))     // b .
            state(RP(bOpt, oLI, EOR)) // bOpt = [b] .
            state(RP(S, oN, p2))     // S = aOpt bOpt . cOpt
            state(RP(c, oN, EOR))     // c .
            state(RP(cOpt, oLE, EOR)) // cOpt = [EMPTY_LIST] .
            state(RP(S, oN, EOR))     // S = aOpt bOpt cOpt .
            state(RP(rG, oN, EOR))     // G = S .

            trans(WIDTH) { src(rG, oN, SOR); tgt(EMPTY_LIST); lhg(setOf(EOT, b, c)); ctx(rG, oN, SOR) }
            trans(WIDTH) { src(S, oN, p1); tgt(EMPTY_LIST); lhg(setOf(RT, c)); ctx(rG, oN, SOR) }
            trans(WIDTH) { src(S, oN, p2); tgt(EMPTY_LIST); lhg(setOf(RT)); ctx(rG, oN, SOR) }
            trans(WIDTH) { src(rG, oN, SOR); tgt(a); lhg(setOf(EOT, b, c)); ctx(rG, oN, SOR) }
            trans(WIDTH) { src(S, oN, p1); tgt(b); lhg(setOf(RT, c)); ctx(rG, oN, SOR) }
            trans(WIDTH) { src(S, oN, p2); tgt(c); lhg(setOf(RT)); ctx(rG, oN, SOR) }
            trans(GOAL) { src(S); tgt(rG); lhg(EOT); ctx(rG, oN, SOR) }
            trans(GRAFT) { src(cOpt, oLE, EOR); tgt(S); lhg(RT); ctx(S, oN, p2) }
            trans(HEIGHT) { src(aOpt, oLE, EOR); tgt(S, oN, p1); lhg(setOf(EOT, b, c), setOf(EOT)); ctx(rG, oN, SOR) }
            trans(GRAFT) { src(bOpt, oLI, EOR); tgt(S, oN, p2); lhg(setOf(RT, c)); ctx(S, oN, p1) }
            trans(HEIGHT) { src(EMPTY_LIST); tgt(aOpt, oLE, EOR); lhg(setOf(EOT, b, c), setOf(EOT, b, c)); ctx(rG, oN, SOR) }
            trans(HEIGHT) { src(b); tgt(bOpt, oLI, EOR); lhg(setOf(RT, c), setOf(RT, c)); ctx(S, oN, p1) }
            trans(HEIGHT) { src(EMPTY_LIST); tgt(cOpt, oLE, EOR); lhg(setOf(RT), setOf(RT)); ctx(S, oN, p2) }
        }
        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun parse_c() {
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        val result = parser.parseForGoal("S", "c")
        println(rrs.usedAutomatonToString("S"))
        assertNotNull(result.sppt)
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(RP(rG, oN, SOR))       // G = . S
            state(RP(EMPTY_LIST, oN, EOR)) // EMPTY_LIST .
            state(RP(a, oN, EOR))       // a .
            state(RP(aOpt, oLE, EOR)) // aOpt = [EMPTY_LIST] .
            state(RP(S, oN, p1))     // S = aOpt . bOpt cOpt
            state(RP(b, oN, EOR))     // b .
            state(RP(bOpt, oLE, EOR)) // bOpt = [EMPTY_LIST] .
            state(RP(S, oN, p2))     // S = aOpt bOpt . cOpt
            state(RP(c, oN, EOR))     // c .
            state(RP(cOpt, oLI, EOR)) // cOpt = [c] .
            state(RP(S, oN, EOR))     // S = aOpt bOpt cOpt .
            state(RP(rG, oN, EOR))     // G = S .

            trans(WIDTH) { src(rG, oN, SOR); tgt(EMPTY_LIST); lhg(setOf(EOT, b, c)); ctx(rG, oN, SOR) }
            trans(WIDTH) { src(S, oN, p1); tgt(EMPTY_LIST); lhg(setOf(RT, c)); ctx(rG, oN, SOR) }
            trans(WIDTH) { src(S, oN, p2); tgt(EMPTY_LIST); lhg(setOf(RT)); ctx(rG, oN, SOR) }
            trans(WIDTH) { src(rG, oN, SOR); tgt(a); lhg(setOf(EOT, b, c)); ctx(rG, oN, SOR) }
            trans(WIDTH) { src(S, oN, p1); tgt(b); lhg(setOf(RT, c)); ctx(rG, oN, SOR) }
            trans(WIDTH) { src(S, oN, p2); tgt(c); lhg(setOf(RT)); ctx(rG, oN, SOR) }
            trans(GOAL) { src(S); tgt(rG); lhg(EOT); ctx(rG, oN, SOR) }
            trans(GRAFT) { src(cOpt, oLI, EOR); tgt(S); lhg(RT); ctx(S, oN, p2) }
            trans(HEIGHT) { src(aOpt, oLE, EOR); tgt(S, oN, p1); lhg(setOf(EOT, b, c), setOf(EOT)); ctx(rG, oN, SOR) }
            trans(GRAFT) { src(bOpt, oLE, EOR); tgt(S, oN, p2); lhg(setOf(RT, c)); ctx(S, oN, p1) }
            trans(HEIGHT) { src(EMPTY_LIST); tgt(aOpt, oLE, EOR); lhg(setOf(EOT, b, c), setOf(EOT, b, c)); ctx(rG, oN, SOR) }
            trans(HEIGHT) { src(EMPTY_LIST); tgt(bOpt, oLE, EOR); lhg(setOf(RT, c), setOf(RT, c)); ctx(S, oN, p1) }
            trans(HEIGHT) { src(c); tgt(cOpt, oLI, EOR); lhg(setOf(RT), setOf(RT)); ctx(S, oN, p2) }
        }
        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun buildFor() {
        val actual = rrs.buildFor("S", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))

        val sentences = listOf("", "a", "b", "ab", "c", "ac", "bc", "abc")
        sentences.forEach {
            val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
            val result = parser.parseForGoal("S", it)
            assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
            assertEquals(0, result.issues.size)
            assertEquals(1, result.sppt!!.maxNumHeads)
        }

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(RP(rG, oN, SOR))       // G = . S
            state(RP(rG, oN, EOR))       // G = S .
            state(RP(S, oN, p1))     // S = aOpt . bOpt cOpt
            state(RP(S, oN, p2))     // S = aOpt bOpt . cOpt
            state(RP(S, oN, EOR))     // S = aOpt bOpt cOpt .
            state(RP(aOpt, oLI, EOR)) // aOpt = [a] .
            state(RP(aOpt, oLE, EOR)) // aOpt = [EMPTY_LIST] .
            state(RP(EMPTY_LIST, oN, EOR)) // EMPTY_LIST .
            state(RP(bOpt, oLI, EOR)) // bOpt = [b] .
            state(RP(bOpt, oLE, EOR)) // bOpt = [EMPTY_LIST] .
            state(RP(cOpt, oLI, EOR)) // cOpt = [c] .
            state(RP(cOpt, oLE, EOR)) // cOpt = [EMPTY_LIST] .
            state(RP(a, oN, EOR))       // a .
            state(RP(EMPTY_LIST, oN, EOR)) // EMPTY_LIST .
            state(RP(b, oN, EOR))       // b .
            state(RP(c, oN, EOR))       // c .

            trans(WIDTH) { src(rG, oN, SOR); tgt(EMPTY_LIST); lhg(setOf(EOT, b, c)); ctx(rG, oN, SOR) }
            trans(WIDTH) { src(S, oN, p1); tgt(EMPTY_LIST); lhg(setOf(EOT, c)); ctx(rG, oN, SOR) }
            trans(WIDTH) { src(S, oN, p2); tgt(EMPTY_LIST); lhg(setOf(EOT)); ctx(rG, oN, SOR) }
            trans(WIDTH) { src(rG, oN, SOR); tgt(a); lhg(setOf(EOT, b, c)); ctx(rG, oN, SOR) }
            trans(WIDTH) { src(S, oN, p1); tgt(b); lhg(setOf(EOT, c)); ctx(rG, oN, SOR) }
            trans(WIDTH) { src(S, oN, p2); tgt(c); lhg(setOf(EOT)); ctx(rG, oN, SOR) }
            trans(GOAL) { src(S); tgt(rG); lhg(EOT); ctx(rG, oN, SOR) }
            trans(GRAFT) { src(cOpt, oLE, EOR); tgt(S); lhg(EOT); ctx(S, oN, p2) }
            trans(GRAFT) { src(cOpt, oLI, EOR); tgt(S); lhg(EOT); ctx(S, oN, p2) }
            trans(HEIGHT) { src(aOpt, oLE, EOR); tgt(S, oN, p1); lhg(setOf(EOT, b, c), setOf(EOT)); ctx(rG, oN, SOR) }
            trans(HEIGHT) { src(aOpt, oLI, EOR); tgt(S, oN, p1); lhg(setOf(EOT, b, c), setOf(EOT)); ctx(rG, oN, SOR) }
            trans(GRAFT) { src(bOpt, oLE, EOR); tgt(S, oN, p2); lhg(setOf(EOT, c)); ctx(S, oN, p1) }
            trans(GRAFT) { src(bOpt, oLI, EOR); tgt(S, oN, p2); lhg(setOf(EOT, c)); ctx(S, oN, p1) }
            trans(HEIGHT) { src(EMPTY_LIST); tgt(aOpt, oLE, EOR); lhg(setOf(EOT, b, c), setOf(EOT, b, c)); ctx(rG, oN, SOR) }
            trans(HEIGHT) { src(a); tgt(aOpt, oLI, EOR); lhg(setOf(EOT, b, c), setOf(EOT, b, c)); ctx(rG, oN, SOR) }
            trans(HEIGHT) { src(EMPTY_LIST); tgt(bOpt, oLE, EOR); lhg(setOf(EOT, c), setOf(EOT, c)); ctx(S, oN, p1) }
            trans(HEIGHT) { src(b); tgt(bOpt, oLI, EOR); lhg(setOf(EOT, c), setOf(EOT, c)); ctx(S, oN, p1) }
            trans(HEIGHT) { src(EMPTY_LIST); tgt(cOpt, oLE, EOR); lhg(setOf(EOT), setOf(EOT)); ctx(S, oN, p2) }
            trans(HEIGHT) { src(c); tgt(cOpt, oLI, EOR); lhg(setOf(EOT), setOf(EOT)); ctx(S, oN, p2) }
        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun compare() {
        val rrs_noBuild = rrs.clone()
        val rrs_preBuild = rrs.clone()

        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs_noBuild.nonSkipTerminals), rrs_noBuild)
        val sentences = listOf("", "a", "b", "ab", "c", "ac", "bc", "abc")
        for (sen in sentences) {
            val result = parser.parseForGoal("S", sen)
            if (result.issues.isNotEmpty()) {
                println("Sentence: $sen")
                result.issues.forEach { println(it) }
            }
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