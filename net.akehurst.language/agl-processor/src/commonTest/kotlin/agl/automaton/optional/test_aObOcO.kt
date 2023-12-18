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

import net.akehurst.language.agl.parser.LeftCornerParser
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.agl.scanner.ScannerOnDemand
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
        val parser = LeftCornerParser(ScannerOnDemand(rrs.nonSkipTerminals), rrs)
        val result = parser.parseForGoal("S", "a")
        println(rrs.usedAutomatonToString("S"))
        assertNotNull(result.sppt)
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(RP(G, o0, SR))     // G = . S
            state(RP(EMPTY, o0, ER)) // EMPTY .
            state(RP(a, o0, ER))     // a .
            state(RP(aOpt, OMI, ER)) // aOpt = [a] .
            state(RP(S, o0, p1))     // S = aOpt . bOpt cOpt
            state(RP(b, o0, ER))     // b .
            state(RP(bOpt, OME, ER)) // bOpt = [EMPTY] .
            state(RP(S, o0, p2))     // S = aOpt bOpt . cOpt
            state(RP(c, o0, ER))     // c .
            state(RP(cOpt, OME, ER)) // cOpt = [EMPTY] .
            state(RP(S, o0, ER))     // S = aOpt bOpt cOpt .
            state(RP(G, o0, ER))     // G = S .

            trans(WIDTH) { src(G, o0, SR); tgt(EMPTY); lhg(setOf(EOT, b, c)); ctx(G, o0, SR) }
            trans(WIDTH) { src(S, o0, p1); tgt(EMPTY); lhg(setOf(RT, c)); ctx(G, o0, SR) }
            trans(WIDTH) { src(S, o0, p2); tgt(EMPTY); lhg(setOf(RT)); ctx(G, o0, SR) }
            trans(WIDTH) { src(G, o0, SR); tgt(a); lhg(setOf(EOT, b, c)); ctx(G, o0, SR) }
            trans(WIDTH) { src(S, o0, p1); tgt(b); lhg(setOf(RT, c)); ctx(G, o0, SR) }
            trans(WIDTH) { src(S, o0, p2); tgt(c); lhg(setOf(RT)); ctx(G, o0, SR) }
            trans(GOAL) { src(S);tgt(G); lhg(EOT); ctx(G, o0, SR) }
            trans(GRAFT) { src(cOpt, OME, ER); tgt(S); lhg(RT); ctx(S, o0, p2) }
            trans(HEIGHT) { src(aOpt, OMI, ER); tgt(S, o0, p1); lhg(setOf(EOT, b, c), setOf(EOT)); ctx(G, o0, SR) }
            trans(GRAFT) { src(bOpt, OME, ER); tgt(S, o0, p2); lhg(setOf(RT, c)); ctx(S, o0, p1) }
            trans(HEIGHT) { src(a); tgt(aOpt, OMI, ER); lhg(setOf(EOT, b, c), setOf(EOT, b, c)); ctx(G, o0, SR) }
            trans(HEIGHT) { src(EMPTY); tgt(bOpt, OME, ER); lhg(setOf(RT, c), setOf(RT, c)); ctx(S, o0, p1) }
            trans(HEIGHT) { src(EMPTY); tgt(cOpt, OME, ER); lhg(setOf(RT), setOf(RT)); ctx(S, o0, p2) }
        }
        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun parse_b() {
        val parser = LeftCornerParser(ScannerOnDemand(rrs.nonSkipTerminals), rrs)
        val result = parser.parseForGoal("S", "b")
        println(rrs.usedAutomatonToString("S"))
        assertNotNull(result.sppt)
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(RP(G, o0, SR))     // G = . S
            state(RP(EMPTY, o0, ER)) // EMPTY .
            state(RP(a, o0, ER))     // a .
            state(RP(aOpt, OME, ER)) // aOpt = [EMPTY] .
            state(RP(S, o0, p1))     // S = aOpt . bOpt cOpt
            state(RP(b, o0, ER))     // b .
            state(RP(bOpt, OMI, ER)) // bOpt = [b] .
            state(RP(S, o0, p2))     // S = aOpt bOpt . cOpt
            state(RP(c, o0, ER))     // c .
            state(RP(cOpt, OME, ER)) // cOpt = [EMPTY] .
            state(RP(S, o0, ER))     // S = aOpt bOpt cOpt .
            state(RP(G, o0, ER))     // G = S .

            trans(WIDTH) { src(G, o0, SR); tgt(EMPTY); lhg(setOf(EOT, b, c)); ctx(G, o0, SR) }
            trans(WIDTH) { src(S, o0, p1); tgt(EMPTY); lhg(setOf(RT, c)); ctx(G, o0, SR) }
            trans(WIDTH) { src(S, o0, p2); tgt(EMPTY); lhg(setOf(RT)); ctx(G, o0, SR) }
            trans(WIDTH) { src(G, o0, SR); tgt(a); lhg(setOf(EOT, b, c)); ctx(G, o0, SR) }
            trans(WIDTH) { src(S, o0, p1); tgt(b); lhg(setOf(RT, c)); ctx(G, o0, SR) }
            trans(WIDTH) { src(S, o0, p2); tgt(c); lhg(setOf(RT)); ctx(G, o0, SR) }
            trans(GOAL) { src(S); tgt(G); lhg(EOT); ctx(G, o0, SR) }
            trans(GRAFT) { src(cOpt, OME, ER); tgt(S); lhg(RT); ctx(S, o0, p2) }
            trans(HEIGHT) { src(aOpt, OME, ER); tgt(S, o0, p1); lhg(setOf(EOT, b, c), setOf(EOT)); ctx(G, o0, SR) }
            trans(GRAFT) { src(bOpt, OMI, ER); tgt(S, o0, p2); lhg(setOf(RT, c)); ctx(S, o0, p1) }
            trans(HEIGHT) { src(EMPTY); tgt(aOpt, OME, ER); lhg(setOf(EOT, b, c), setOf(EOT, b, c)); ctx(G, o0, SR) }
            trans(HEIGHT) { src(b); tgt(bOpt, OMI, ER); lhg(setOf(RT, c), setOf(RT, c)); ctx(S, o0, p1) }
            trans(HEIGHT) { src(EMPTY); tgt(cOpt, OME, ER); lhg(setOf(RT), setOf(RT)); ctx(S, o0, p2) }
        }
        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun parse_c() {
        val parser = LeftCornerParser(ScannerOnDemand(rrs.nonSkipTerminals), rrs)
        val result = parser.parseForGoal("S", "c")
        println(rrs.usedAutomatonToString("S"))
        assertNotNull(result.sppt)
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(RP(G, o0, SR))     // G = . S
            state(RP(EMPTY, o0, ER)) // EMPTY .
            state(RP(a, o0, ER))     // a .
            state(RP(aOpt, OME, ER)) // aOpt = [EMPTY] .
            state(RP(S, o0, p1))     // S = aOpt . bOpt cOpt
            state(RP(b, o0, ER))     // b .
            state(RP(bOpt, OME, ER)) // bOpt = [EMPTY] .
            state(RP(S, o0, p2))     // S = aOpt bOpt . cOpt
            state(RP(c, o0, ER))     // c .
            state(RP(cOpt, OMI, ER)) // cOpt = [c] .
            state(RP(S, o0, ER))     // S = aOpt bOpt cOpt .
            state(RP(G, o0, ER))     // G = S .

            trans(WIDTH) { src(G, o0, SR); tgt(EMPTY); lhg(setOf(EOT, b, c)); ctx(G, o0, SR) }
            trans(WIDTH) { src(S, o0, p1); tgt(EMPTY); lhg(setOf(RT, c)); ctx(G, o0, SR) }
            trans(WIDTH) { src(S, o0, p2); tgt(EMPTY); lhg(setOf(RT)); ctx(G, o0, SR) }
            trans(WIDTH) { src(G, o0, SR); tgt(a); lhg(setOf(EOT, b, c)); ctx(G, o0, SR) }
            trans(WIDTH) { src(S, o0, p1); tgt(b); lhg(setOf(RT, c)); ctx(G, o0, SR) }
            trans(WIDTH) { src(S, o0, p2); tgt(c); lhg(setOf(RT)); ctx(G, o0, SR) }
            trans(GOAL) { src(S); tgt(G); lhg(EOT); ctx(G, o0, SR) }
            trans(GRAFT) { src(cOpt, OMI, ER); tgt(S); lhg(RT); ctx(S, o0, p2) }
            trans(HEIGHT) { src(aOpt, OME, ER); tgt(S, o0, p1); lhg(setOf(EOT, b, c), setOf(EOT)); ctx(G, o0, SR) }
            trans(GRAFT) { src(bOpt, OME, ER); tgt(S, o0, p2); lhg(setOf(RT, c)); ctx(S, o0, p1) }
            trans(HEIGHT) { src(EMPTY); tgt(aOpt, OME, ER); lhg(setOf(EOT, b, c), setOf(EOT, b, c)); ctx(G, o0, SR) }
            trans(HEIGHT) { src(EMPTY); tgt(bOpt, OME, ER); lhg(setOf(RT, c), setOf(RT, c)); ctx(S, o0, p1) }
            trans(HEIGHT) { src(c); tgt(cOpt, OMI, ER); lhg(setOf(RT), setOf(RT)); ctx(S, o0, p2) }
        }
        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun buildFor() {
        val actual = rrs.buildFor("S", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))

        val sentences = listOf("", "a", "b", "ab", "c", "ac", "bc", "abc")
        sentences.forEach {
            val parser = LeftCornerParser(ScannerOnDemand(rrs.nonSkipTerminals), rrs)
            val result = parser.parseForGoal("S", it)
            assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
            assertEquals(0, result.issues.size)
            assertEquals(1, result.sppt!!.maxNumHeads)
        }

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(RP(G, o0, SR))     // G = . S
            state(RP(G, o0, ER))     // G = S .
            state(RP(S, o0, p1))     // S = aOpt . bOpt cOpt
            state(RP(S, o0, p2))     // S = aOpt bOpt . cOpt
            state(RP(S, o0, ER))     // S = aOpt bOpt cOpt .
            state(RP(aOpt, OMI, ER)) // aOpt = [a] .
            state(RP(aOpt, OME, ER)) // aOpt = [EMPTY] .
            state(RP(EMPTY, o0, ER)) // EMPTY .
            state(RP(bOpt, OMI, ER)) // bOpt = [b] .
            state(RP(bOpt, OME, ER)) // bOpt = [EMPTY] .
            state(RP(cOpt, OMI, ER)) // cOpt = [c] .
            state(RP(cOpt, OME, ER)) // cOpt = [EMPTY] .
            state(RP(a, o0, ER))     // a .
            state(RP(EMPTY, o0, ER)) // EMPTY .
            state(RP(b, o0, ER))     // b .
            state(RP(c, o0, ER))     // c .

            trans(WIDTH) { src(G, o0, SR); tgt(EMPTY); lhg(setOf(EOT, b, c)); ctx(G, o0, SR) }
            trans(WIDTH) { src(S, o0, p1); tgt(EMPTY); lhg(setOf(EOT, c)); ctx(G, o0, SR) }
            trans(WIDTH) { src(S, o0, p2); tgt(EMPTY); lhg(setOf(EOT)); ctx(G, o0, SR) }
            trans(WIDTH) { src(G, o0, SR); tgt(a); lhg(setOf(EOT, b, c)); ctx(G, o0, SR) }
            trans(WIDTH) { src(S, o0, p1); tgt(b); lhg(setOf(EOT, c)); ctx(G, o0, SR) }
            trans(WIDTH) { src(S, o0, p2); tgt(c); lhg(setOf(EOT)); ctx(G, o0, SR) }
            trans(GOAL) { src(S); tgt(G); lhg(EOT); ctx(G, o0, SR) }
            trans(GRAFT) { src(cOpt, OME, ER); tgt(S); lhg(EOT); ctx(S, o0, p2) }
            trans(GRAFT) { src(cOpt, OMI, ER); tgt(S); lhg(EOT); ctx(S, o0, p2) }
            trans(HEIGHT) { src(aOpt, OME, ER); tgt(S, o0, p1); lhg(setOf(EOT, b, c), setOf(EOT)); ctx(G, o0, SR) }
            trans(HEIGHT) { src(aOpt, OMI, ER); tgt(S, o0, p1); lhg(setOf(EOT, b, c), setOf(EOT)); ctx(G, o0, SR) }
            trans(GRAFT) { src(bOpt, OME, ER); tgt(S, o0, p2); lhg(setOf(EOT, c)); ctx(S, o0, p1) }
            trans(GRAFT) { src(bOpt, OMI, ER); tgt(S, o0, p2); lhg(setOf(EOT, c)); ctx(S, o0, p1) }
            trans(HEIGHT) { src(EMPTY); tgt(aOpt, OME, ER); lhg(setOf(EOT, b, c), setOf(EOT, b, c)); ctx(G, o0, SR) }
            trans(HEIGHT) { src(a); tgt(aOpt, OMI, ER); lhg(setOf(EOT, b, c), setOf(EOT, b, c)); ctx(G, o0, SR) }
            trans(HEIGHT) { src(EMPTY); tgt(bOpt, OME, ER); lhg(setOf(EOT, c), setOf(EOT, c)); ctx(S, o0, p1) }
            trans(HEIGHT) { src(b); tgt(bOpt, OMI, ER); lhg(setOf(EOT, c), setOf(EOT, c)); ctx(S, o0, p1) }
            trans(HEIGHT) { src(EMPTY); tgt(cOpt, OME, ER); lhg(setOf(EOT), setOf(EOT)); ctx(S, o0, p2) }
            trans(HEIGHT) { src(c); tgt(cOpt, OMI, ER); lhg(setOf(EOT), setOf(EOT)); ctx(S, o0, p2) }
        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun compare() {
        val rrs_noBuild = rrs.clone()
        val rrs_preBuild = rrs.clone()

        val parser = LeftCornerParser(ScannerOnDemand(rrs_noBuild.nonSkipTerminals), rrs_noBuild)
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