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
    private val G = SM.startState.runtimeRules.first()

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
            state(RP(G, oN, SR))     // G = . S
            state(RP(EMPTY, oN, ER)) // EMPTY .
            state(RP(a, oN, ER))     // a .
            state(RP(aOpt, oLI, ER)) // aOpt = [a] .
            state(RP(S, oN, p1))     // S = aOpt . bOpt cOpt
            state(RP(b, oN, ER))     // b .
            state(RP(bOpt, oLE, ER)) // bOpt = [EMPTY] .
            state(RP(S, oN, p2))     // S = aOpt bOpt . cOpt
            state(RP(c, oN, ER))     // c .
            state(RP(cOpt, oLE, ER)) // cOpt = [EMPTY] .
            state(RP(S, oN, ER))     // S = aOpt bOpt cOpt .
            state(RP(G, oN, ER))     // G = S .

            trans(WIDTH) { src(G, oN, SR); tgt(EMPTY); lhg(setOf(EOT, b, c)); ctx(G, oN, SR) }
            trans(WIDTH) { src(S, oN, p1); tgt(EMPTY); lhg(setOf(RT, c)); ctx(G, oN, SR) }
            trans(WIDTH) { src(S, oN, p2); tgt(EMPTY); lhg(setOf(RT)); ctx(G, oN, SR) }
            trans(WIDTH) { src(G, oN, SR); tgt(a); lhg(setOf(EOT, b, c)); ctx(G, oN, SR) }
            trans(WIDTH) { src(S, oN, p1); tgt(b); lhg(setOf(RT, c)); ctx(G, oN, SR) }
            trans(WIDTH) { src(S, oN, p2); tgt(c); lhg(setOf(RT)); ctx(G, oN, SR) }
            trans(GOAL) { src(S);tgt(G); lhg(EOT); ctx(G, oN, SR) }
            trans(GRAFT) { src(cOpt, oLE, ER); tgt(S); lhg(RT); ctx(S, oN, p2) }
            trans(HEIGHT) { src(aOpt, oLI, ER); tgt(S, oN, p1); lhg(setOf(EOT, b, c), setOf(EOT)); ctx(G, oN, SR) }
            trans(GRAFT) { src(bOpt, oLE, ER); tgt(S, oN, p2); lhg(setOf(RT, c)); ctx(S, oN, p1) }
            trans(HEIGHT) { src(a); tgt(aOpt, oLI, ER); lhg(setOf(EOT, b, c), setOf(EOT, b, c)); ctx(G, oN, SR) }
            trans(HEIGHT) { src(EMPTY); tgt(bOpt, oLE, ER); lhg(setOf(RT, c), setOf(RT, c)); ctx(S, oN, p1) }
            trans(HEIGHT) { src(EMPTY); tgt(cOpt, oLE, ER); lhg(setOf(RT), setOf(RT)); ctx(S, oN, p2) }
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
            state(RP(G, oN, SR))     // G = . S
            state(RP(EMPTY, oN, ER)) // EMPTY .
            state(RP(a, oN, ER))     // a .
            state(RP(aOpt, oLE, ER)) // aOpt = [EMPTY] .
            state(RP(S, oN, p1))     // S = aOpt . bOpt cOpt
            state(RP(b, oN, ER))     // b .
            state(RP(bOpt, oLI, ER)) // bOpt = [b] .
            state(RP(S, oN, p2))     // S = aOpt bOpt . cOpt
            state(RP(c, oN, ER))     // c .
            state(RP(cOpt, oLE, ER)) // cOpt = [EMPTY] .
            state(RP(S, oN, ER))     // S = aOpt bOpt cOpt .
            state(RP(G, oN, ER))     // G = S .

            trans(WIDTH) { src(G, oN, SR); tgt(EMPTY); lhg(setOf(EOT, b, c)); ctx(G, oN, SR) }
            trans(WIDTH) { src(S, oN, p1); tgt(EMPTY); lhg(setOf(RT, c)); ctx(G, oN, SR) }
            trans(WIDTH) { src(S, oN, p2); tgt(EMPTY); lhg(setOf(RT)); ctx(G, oN, SR) }
            trans(WIDTH) { src(G, oN, SR); tgt(a); lhg(setOf(EOT, b, c)); ctx(G, oN, SR) }
            trans(WIDTH) { src(S, oN, p1); tgt(b); lhg(setOf(RT, c)); ctx(G, oN, SR) }
            trans(WIDTH) { src(S, oN, p2); tgt(c); lhg(setOf(RT)); ctx(G, oN, SR) }
            trans(GOAL) { src(S); tgt(G); lhg(EOT); ctx(G, oN, SR) }
            trans(GRAFT) { src(cOpt, oLE, ER); tgt(S); lhg(RT); ctx(S, oN, p2) }
            trans(HEIGHT) { src(aOpt, oLE, ER); tgt(S, oN, p1); lhg(setOf(EOT, b, c), setOf(EOT)); ctx(G, oN, SR) }
            trans(GRAFT) { src(bOpt, oLI, ER); tgt(S, oN, p2); lhg(setOf(RT, c)); ctx(S, oN, p1) }
            trans(HEIGHT) { src(EMPTY); tgt(aOpt, oLE, ER); lhg(setOf(EOT, b, c), setOf(EOT, b, c)); ctx(G, oN, SR) }
            trans(HEIGHT) { src(b); tgt(bOpt, oLI, ER); lhg(setOf(RT, c), setOf(RT, c)); ctx(S, oN, p1) }
            trans(HEIGHT) { src(EMPTY); tgt(cOpt, oLE, ER); lhg(setOf(RT), setOf(RT)); ctx(S, oN, p2) }
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
            state(RP(G, oN, SR))     // G = . S
            state(RP(EMPTY, oN, ER)) // EMPTY .
            state(RP(a, oN, ER))     // a .
            state(RP(aOpt, oLE, ER)) // aOpt = [EMPTY] .
            state(RP(S, oN, p1))     // S = aOpt . bOpt cOpt
            state(RP(b, oN, ER))     // b .
            state(RP(bOpt, oLE, ER)) // bOpt = [EMPTY] .
            state(RP(S, oN, p2))     // S = aOpt bOpt . cOpt
            state(RP(c, oN, ER))     // c .
            state(RP(cOpt, oLI, ER)) // cOpt = [c] .
            state(RP(S, oN, ER))     // S = aOpt bOpt cOpt .
            state(RP(G, oN, ER))     // G = S .

            trans(WIDTH) { src(G, oN, SR); tgt(EMPTY); lhg(setOf(EOT, b, c)); ctx(G, oN, SR) }
            trans(WIDTH) { src(S, oN, p1); tgt(EMPTY); lhg(setOf(RT, c)); ctx(G, oN, SR) }
            trans(WIDTH) { src(S, oN, p2); tgt(EMPTY); lhg(setOf(RT)); ctx(G, oN, SR) }
            trans(WIDTH) { src(G, oN, SR); tgt(a); lhg(setOf(EOT, b, c)); ctx(G, oN, SR) }
            trans(WIDTH) { src(S, oN, p1); tgt(b); lhg(setOf(RT, c)); ctx(G, oN, SR) }
            trans(WIDTH) { src(S, oN, p2); tgt(c); lhg(setOf(RT)); ctx(G, oN, SR) }
            trans(GOAL) { src(S); tgt(G); lhg(EOT); ctx(G, oN, SR) }
            trans(GRAFT) { src(cOpt, oLI, ER); tgt(S); lhg(RT); ctx(S, oN, p2) }
            trans(HEIGHT) { src(aOpt, oLE, ER); tgt(S, oN, p1); lhg(setOf(EOT, b, c), setOf(EOT)); ctx(G, oN, SR) }
            trans(GRAFT) { src(bOpt, oLE, ER); tgt(S, oN, p2); lhg(setOf(RT, c)); ctx(S, oN, p1) }
            trans(HEIGHT) { src(EMPTY); tgt(aOpt, oLE, ER); lhg(setOf(EOT, b, c), setOf(EOT, b, c)); ctx(G, oN, SR) }
            trans(HEIGHT) { src(EMPTY); tgt(bOpt, oLE, ER); lhg(setOf(RT, c), setOf(RT, c)); ctx(S, oN, p1) }
            trans(HEIGHT) { src(c); tgt(cOpt, oLI, ER); lhg(setOf(RT), setOf(RT)); ctx(S, oN, p2) }
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
            state(RP(G, oN, SR))     // G = . S
            state(RP(G, oN, ER))     // G = S .
            state(RP(S, oN, p1))     // S = aOpt . bOpt cOpt
            state(RP(S, oN, p2))     // S = aOpt bOpt . cOpt
            state(RP(S, oN, ER))     // S = aOpt bOpt cOpt .
            state(RP(aOpt, oLI, ER)) // aOpt = [a] .
            state(RP(aOpt, oLE, ER)) // aOpt = [EMPTY] .
            state(RP(EMPTY, oN, ER)) // EMPTY .
            state(RP(bOpt, oLI, ER)) // bOpt = [b] .
            state(RP(bOpt, oLE, ER)) // bOpt = [EMPTY] .
            state(RP(cOpt, oLI, ER)) // cOpt = [c] .
            state(RP(cOpt, oLE, ER)) // cOpt = [EMPTY] .
            state(RP(a, oN, ER))     // a .
            state(RP(EMPTY, oN, ER)) // EMPTY .
            state(RP(b, oN, ER))     // b .
            state(RP(c, oN, ER))     // c .

            trans(WIDTH) { src(G, oN, SR); tgt(EMPTY); lhg(setOf(EOT, b, c)); ctx(G, oN, SR) }
            trans(WIDTH) { src(S, oN, p1); tgt(EMPTY); lhg(setOf(EOT, c)); ctx(G, oN, SR) }
            trans(WIDTH) { src(S, oN, p2); tgt(EMPTY); lhg(setOf(EOT)); ctx(G, oN, SR) }
            trans(WIDTH) { src(G, oN, SR); tgt(a); lhg(setOf(EOT, b, c)); ctx(G, oN, SR) }
            trans(WIDTH) { src(S, oN, p1); tgt(b); lhg(setOf(EOT, c)); ctx(G, oN, SR) }
            trans(WIDTH) { src(S, oN, p2); tgt(c); lhg(setOf(EOT)); ctx(G, oN, SR) }
            trans(GOAL) { src(S); tgt(G); lhg(EOT); ctx(G, oN, SR) }
            trans(GRAFT) { src(cOpt, oLE, ER); tgt(S); lhg(EOT); ctx(S, oN, p2) }
            trans(GRAFT) { src(cOpt, oLI, ER); tgt(S); lhg(EOT); ctx(S, oN, p2) }
            trans(HEIGHT) { src(aOpt, oLE, ER); tgt(S, oN, p1); lhg(setOf(EOT, b, c), setOf(EOT)); ctx(G, oN, SR) }
            trans(HEIGHT) { src(aOpt, oLI, ER); tgt(S, oN, p1); lhg(setOf(EOT, b, c), setOf(EOT)); ctx(G, oN, SR) }
            trans(GRAFT) { src(bOpt, oLE, ER); tgt(S, oN, p2); lhg(setOf(EOT, c)); ctx(S, oN, p1) }
            trans(GRAFT) { src(bOpt, oLI, ER); tgt(S, oN, p2); lhg(setOf(EOT, c)); ctx(S, oN, p1) }
            trans(HEIGHT) { src(EMPTY); tgt(aOpt, oLE, ER); lhg(setOf(EOT, b, c), setOf(EOT, b, c)); ctx(G, oN, SR) }
            trans(HEIGHT) { src(a); tgt(aOpt, oLI, ER); lhg(setOf(EOT, b, c), setOf(EOT, b, c)); ctx(G, oN, SR) }
            trans(HEIGHT) { src(EMPTY); tgt(bOpt, oLE, ER); lhg(setOf(EOT, c), setOf(EOT, c)); ctx(S, oN, p1) }
            trans(HEIGHT) { src(b); tgt(bOpt, oLI, ER); lhg(setOf(EOT, c), setOf(EOT, c)); ctx(S, oN, p1) }
            trans(HEIGHT) { src(EMPTY); tgt(cOpt, oLE, ER); lhg(setOf(EOT), setOf(EOT)); ctx(S, oN, p2) }
            trans(HEIGHT) { src(c); tgt(cOpt, oLI, ER); lhg(setOf(EOT), setOf(EOT)); ctx(S, oN, p2) }
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