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

class test_nested_optionals : test_AutomatonAbstract() {
    /*
        S = 'i' 'a' Rs 'z' ;
        Rs = R+ ;
        R = Os 'i' 't' ;
        Os = Bo Co Do ;
        Bo = 'b'?
        Co = 'c'?
        Do = 'd'?
     */
    // must be fresh per test or automaton is not correct for different parses (due to caching)
    private val rrs = runtimeRuleSet {
        concatenation("S") { literal("i"); literal("a"); ref("Rs"); literal("z") }
        multi("Rs", 1, -1, "R")
        concatenation("R") { ref("Os"); literal("i"); literal("t") }
        concatenation("Os") { ref("Bo"); ref("Co"); ref("Do") }
        multi("Bo", 0, 1, "'b'")
        multi("Co", 0, 1, "'c'")
        multi("Do", 0, 1, "'d'")
        literal("'b'", "b")
        literal("'c'", "c")
        literal("'d'", "d")
    }

    private val S = rrs.findRuntimeRule("S")
    private val SM = rrs.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
    private val rG = SM.startState.runtimeRules.first()
    private val Rs = rrs.findRuntimeRule("Rs")
    private val R = rrs.findRuntimeRule("R")
    private val Os = rrs.findRuntimeRule("Os")
    private val Bo = rrs.findRuntimeRule("Bo")
    private val Bo_E = EMPTY
    private val Co = rrs.findRuntimeRule("Co")
    private val Co_E = EMPTY
    private val Do = rrs.findRuntimeRule("Do")
    private val Do_E = EMPTY
    private val a = rrs.findRuntimeRule("'a'")
    private val b = rrs.findRuntimeRule("'b'")
    private val c = rrs.findRuntimeRule("'c'")
    private val d = rrs.findRuntimeRule("'d'")
    private val i = rrs.findRuntimeRule("'i'")
    private val t = rrs.findRuntimeRule("'t'")
    private val z = rrs.findRuntimeRule("'z'")

    @Test
    fun parse_iaitz() {
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        val result = parser.parseForGoal("S", "iaitz")
        println(rrs.usedAutomatonToString("S"))
        assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(RP(rG, oN, SOR))
            state(RP(i, oN, EOR))
            state(RP(S, oN, p1))
            state(RP(a, oN, EOR))
            state(RP(S, oN, p2))
            state(RP(b, oN, EOR))
            state(RP(EMPTY_LIST, oN, EOR))
            state(RP(Bo, oLE, EOR))
            state(RP(Os, oN, p1))
            state(RP(c, oN, EOR))
            state(RP(Co, oLE, EOR))
            state(RP(Os, oN, p2))
            state(RP(d, oN, EOR))
            state(RP(Do, oLE, EOR))
            state(RP(Os, oN, EOR))
            state(RP(R, oN, p1))
            state(RP(R, oN, p2))
            state(RP(t, oN, EOR))
            state(RP(R, oN, EOR))
            state(RP(Rs, oLI, EOR))
            state(RP(Rs, oLI, PMI))
            state(RP(S, oN, p3))
            state(RP(z, oN, EOR))
            state(RP(S, oN, EOR))
            state(RP(rG, oN, EOR))

            trans(WIDTH) { src(Os, oN, p1); tgt(EMPTY_LIST); lhg(setOf(RT, d)); ctx(S, oN, p2) }
            trans(WIDTH) { src(Os, oN, p2); tgt(EMPTY_LIST); lhg(setOf(RT)); ctx(S, oN, p2) }
            trans(WIDTH) { src(S, oN, p2); tgt(EMPTY_LIST); lhg(setOf(c, d, i)); ctx(rG, oN, SOR) }
            trans(WIDTH) { src(S, oN, p1); tgt(a); lhg(setOf(b, c, d, i)); ctx(rG, oN, SOR) }
            trans(WIDTH) { src(S, oN, p2); tgt(b); lhg(setOf(c, d, i)); ctx(rG, oN, SOR) }
            trans(WIDTH) { src(Os, oN, p1); tgt(c); lhg(setOf(RT, d)); ctx(S, oN, p2) }
            trans(WIDTH) { src(Os, oN, p2); tgt(d); lhg(setOf(RT)); ctx(S, oN, p2) }
            trans(WIDTH) { src(rG, oN, SOR); tgt(i); lhg(a); ctx(rG, oN, SOR) }
            trans(WIDTH) { src(R, oN, p1); tgt(i); lhg(t); ctx(S, oN, p2) }
            trans(WIDTH) { src(R, oN, p2); tgt(t); lhg(RT); ctx(S, oN, p2) }
            trans(WIDTH) { src(S, oN, p3); tgt(z); lhg(RT); ctx(rG, oN, SOR) }

            trans(GOAL) { src(S); tgt(rG, oN, EOR); lhg(EOT); ctx(rG, oN, SOR); pctx(rG, oN, SOR) }

            trans(HEIGHT) { src(EMPTY_LIST); tgt(Bo, oLE, EOR); lhg(setOf(c, d, i), setOf(c, d, i)); ctx(S, oN, p2); pctx(rG, oN, SOR) }
            trans(HEIGHT) { src(EMPTY_LIST); tgt(Co, oLE, EOR); lhg(setOf(RT, d), setOf(RT, d)); ctx(Os, oN, p1); pctx(S, oN, p2) }
            trans(HEIGHT) { src(EMPTY_LIST); tgt(Do, oLE, EOR); lhg(setOf(RT), setOf(RT)); ctx(Os, oN, p2); pctx(S, oN, p2) }
            trans(HEIGHT) { src(Bo, oLE, EOR); tgt(Os, oN, p1); lhg(setOf(c, d, i), setOf(i)); ctx(S, oN, p2); pctx(rG, oN, SOR) }
            trans(HEIGHT) { src(Os); tgt(R, oN, p1); lhg(setOf(i), setOf(b, c, d, i, z)); ctx(S, oN, p2); pctx(rG, oN, SOR) }
            trans(HEIGHT) { src(R); tgt(Rs, oLI, EOR); lhg(z, z); ctx(S, oN, p2); pctx(rG, oN, SOR) }
            trans(HEIGHT) { src(R); tgt(Rs, oLI, PMI); lhg(setOf(b, c, d, i), setOf(z)); ctx(S, oN, p2); pctx(rG, oN, SOR) }
            trans(HEIGHT) { src(i); tgt(S, oN, p1); lhg(a, EOT); ctx(rG, oN, SOR); pctx(rG, oN, SOR) }

            trans(GRAFT) { src(Co, oLE, EOR); tgt(Os, oN, p2); lhg(setOf(RT, d)); ctx(Os, oN, p1); pctx(S, oN, p2) }
            trans(GRAFT) { src(Do, oLE, EOR); tgt(Os); lhg(RT); ctx(Os, oN, p2); pctx(S, oN, p2) }
            trans(GRAFT) { src(i); tgt(R, oN, p2); lhg(t); ctx(R, oN, p1); pctx(S, oN, p2) }
            trans(GRAFT) { src(t); tgt(R); lhg(RT); ctx(R, oN, p2); pctx(S, oN, p2) }
            trans(GRAFT) { src(a); tgt(S, oN, p2); lhg(setOf(b, c, d, i)); ctx(S, oN, p1); pctx(rG, oN, SOR) }
            trans(GRAFT) { src(Rs, oLI, EOR); tgt(S, oN, p3); lhg(z); ctx(S, oN, p2); pctx(rG, oN, SOR) }
            trans(GRAFT) { src(z); tgt(S); lhg(RT); ctx(S, oN, p3); pctx(rG, oN, SOR) }

        }
        AutomatonTest.assertEquals(expected, actual)
    }


    @Test
    fun buildFor() {
        val actual = rrs.buildFor("S", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))

        val sentences = listOf("iaitz", "iabitz", "iacitz", "iaditz", "iabcditz", "iaititz", "iaitititz")
        sentences.forEach {
            val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
            val result = parser.parseForGoal("S", it)
            assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
            assertEquals(0, result.issues.size)
            assertEquals(1, result.sppt!!.maxNumHeads)
        }

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(RP(rG, oN, SOR))
            state(RP(rG, oN, EOR))
            state(RP(S, oN, p1))
            state(RP(S, oN, p2))
            state(RP(S, oN, p3))
            state(RP(S, oN, EOR))
            state(RP(Rs, oLI, PMI))
            state(RP(Rs, oLI, EOR))
            state(RP(R, oN, p1))
            state(RP(R, oN, p2))
            state(RP(R, oN, EOR))
            state(RP(Os, oN, p1))
            state(RP(Os, oN, p2))
            state(RP(Os, oN, EOR))
            state(RP(Bo, oLI, EOR))
            state(RP(Bo, oLE, EOR))
            state(RP(Co, oLI, EOR))
            state(RP(Co, oLE, EOR))
            state(RP(Do, oLI, EOR))
            state(RP(Do, oLE, EOR))
            state(RP(i, oN, EOR))
            state(RP(a, oN, EOR))
            state(RP(b, oN, EOR))
            state(RP(EMPTY_LIST, oN, EOR))
            state(RP(c, oN, EOR))
            state(RP(d, oN, EOR))
            state(RP(t, oN, EOR))
            state(RP(z, oN, EOR))

            trans(WIDTH) { src(Os, oN, p1); tgt(EMPTY_LIST); lhg(setOf(d, i)); ctx(RP(S, oN, p2), RP(Rs, oLI, PMI)) }
            trans(WIDTH) { src(Os, oN, p2); tgt(EMPTY_LIST); lhg(i); ctx(RP(S, oN, p2), RP(Rs, oLI, PMI)) }
            trans(WIDTH) { src(Rs, oLI, PMI); tgt(EMPTY_LIST); lhg(setOf(c, d, i)); ctx(S, oN, p2) }
            trans(WIDTH) { src(S, oN, p2); tgt(EMPTY_LIST); lhg(setOf(c, d, i)); ctx(rG, oN, SOR) }
            trans(WIDTH) { src(S, oN, p1); tgt(a); lhg(setOf(b, c, d, i)); ctx(rG, oN, SOR) }
            trans(WIDTH) { src(Rs, oLI, PMI); tgt(b); lhg(setOf(c, d, i)); ctx(S, oN, p2) }
            trans(WIDTH) { src(S, oN, p2); tgt(b); lhg(setOf(c, d, i)); ctx(rG, oN, SOR) }
            trans(WIDTH) { src(Os, oN, p1); tgt(c); lhg(setOf(d, i)); ctx(RP(S, oN, p2), RP(Rs, oLI, PMI)) }
            trans(WIDTH) { src(Os, oN, p2); tgt(d); lhg(i); ctx(RP(S, oN, p2), RP(Rs, oLI, PMI)) }
            trans(WIDTH) { src(rG, oN, SOR); tgt(i); lhg(a); ctx(rG, oN, SOR) }
            trans(WIDTH) { src(R, oN, p1); tgt(i); lhg(t); ctx(RP(Rs, oLI, PMI), RP(S, oN, p2)) }
            trans(WIDTH) { src(R, oN, p2); tgt(t); lhg(setOf(b, c, d, i, z)); ctx(RP(S, oN, p2), RP(Rs, oLI, PMI)) }
            trans(WIDTH) { src(S, oN, p3); tgt(z); lhg(EOT); ctx(rG, oN, SOR) }

            trans(GOAL) { src(S); tgt(rG, oN, EOR); lhg(EOT); ctx(rG, oN, SOR); pctx(rG, oN, SOR) }

            trans(HEIGHT) { src(EMPTY_LIST); tgt(Bo, oLE, EOR); lhg(setOf(c, d, i), setOf(c, d, i)); ctx(RP(S, oN, p2), RP(Rs, oLI, PMI)); pctx(RP(rG, oN, SOR), RP(S, oN, p2)) }
            trans(HEIGHT) { src(b); tgt(Bo, oLI, EOR); lhg(setOf(c, d, i), setOf(c, d, i)); ctx(RP(S, oN, p2), RP(Rs, oLI, PMI)); pctx(RP(rG, oN, SOR), RP(S, oN, p2)) }
            trans(HEIGHT) { src(EMPTY_LIST); tgt(Co, oLE, EOR); lhg(setOf(d, i), setOf(d, i)); ctx(Os, oN, p1); pctx(RP(S, oN, p2), RP(Rs, oLI, PMI)) }
            trans(HEIGHT) { src(c); tgt(Co, oLI, EOR); lhg(setOf(d, i), setOf(d, i)); ctx(Os, oN, p1); pctx(RP(S, oN, p2), RP(Rs, oLI, PMI)) }
            trans(HEIGHT) { src(EMPTY_LIST); tgt(Do, oLE, EOR); lhg(i, i); ctx(Os, oN, p2); pctx(RP(S, oN, p2), RP(Rs, oLI, PMI)) }
            trans(HEIGHT) { src(d); tgt(Do, oLI, EOR); lhg(i, i); ctx(Os, oN, p2); pctx(RP(S, oN, p2), RP(Rs, oLI, PMI)) }
            trans(HEIGHT) { src(Bo, oLE, EOR); tgt(Os, oN, p1); lhg(setOf(c, d, i), setOf(i)); ctx(RP(Rs, oLI, PMI), RP(S, oN, p2)); pctx(RP(S, oN, p2), RP(rG, oN, SOR)) }
            trans(HEIGHT) { src(Bo, oLI, EOR); tgt(Os, oN, p1); lhg(setOf(c, d, i), setOf(i)); ctx(RP(S, oN, p2), RP(Rs, oLI, PMI)); pctx(RP(rG, oN, SOR), RP(S, oN, p2)) }
            trans(HEIGHT) { src(Os); tgt(R, oN, p1); lhg(setOf(i), setOf(b, c, d, i, z)); ctx(RP(S, oN, p2), RP(Rs, oLI, PMI)); pctx(RP(rG, oN, SOR), RP(S, oN, p2)) }
            trans(HEIGHT) { src(R); tgt(Rs, oLI, EOR); lhg(z, z); ctx(S, oN, p2); pctx(rG, oN, SOR) }
            trans(HEIGHT) { src(R); tgt(Rs, oLI, PMI); lhg(setOf(b, c, d, i), setOf(z)); ctx(S, oN, p2); pctx(rG, oN, SOR) }
            trans(HEIGHT) { src(i); tgt(S, oN, p1); lhg(a, EOT); ctx(rG, oN, SOR); pctx(rG, oN, SOR) }

            trans(GRAFT) { src(Co, oLE, EOR); tgt(Os, oN, p2); lhg(setOf(d, i)); ctx(Os, oN, p1); pctx(RP(S, oN, p2), RP(Rs, oLI, PMI)) }
            trans(GRAFT) { src(Co, oLI, EOR); tgt(Os, oN, p2); lhg(setOf(d, i)); ctx(Os, oN, p1); pctx(RP(S, oN, p2), RP(Rs, oLI, PMI)) }
            trans(GRAFT) { src(Do, oLE, EOR); tgt(Os); lhg(i); ctx(Os, oN, p2); pctx(RP(S, oN, p2), RP(Rs, oLI, PMI)) }
            trans(GRAFT) { src(Do, oLI, EOR); tgt(Os); lhg(i); ctx(Os, oN, p2); pctx(RP(S, oN, p2), RP(Rs, oLI, PMI)) }
            trans(GRAFT) { src(i); tgt(R, oN, p2); lhg(t); ctx(R, oN, p1); pctx(RP(Rs, oLI, PMI), RP(S, oN, p2)) }
            trans(GRAFT) { src(t); tgt(R); lhg(setOf(b, c, d, i, z)); ctx(R, oN, p2); pctx(RP(S, oN, p2), RP(Rs, oLI, PMI)) }
            trans(GRAFT) { src(R); tgt(Rs, oLI, EOR); lhg(z); ctx(Rs, oLI, PMI); pctx(S, oN, p2) }
            trans(GRAFT) { src(R); tgt(Rs, oLI, PMI); lhg(setOf(b, c, d, i)); ctx(Rs, oLI, PMI); pctx(S, oN, p2) }
            trans(GRAFT) { src(a); tgt(S, oN, p2); lhg(setOf(b, c, d, i)); ctx(S, oN, p1); pctx(rG, oN, SOR) }
            trans(GRAFT) { src(Rs, oLI, EOR); tgt(S, oN, p3); lhg(z); ctx(S, oN, p2); pctx(rG, oN, SOR) }
            trans(GRAFT) { src(z); tgt(S); lhg(EOT); ctx(S, oN, p3); pctx(rG, oN, SOR) }
        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun compare() {
        val rrs_dmdBuild = rrs.clone()
        val rrs_preBuild = rrs.clone()

        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs_dmdBuild.nonSkipTerminals), rrs_dmdBuild)
        val sentences = listOf("iaitz", "iabitz", "iacitz", "iaditz", "iabcditz")
        for (sen in sentences) {
            val result = parser.parseForGoal("S", sen)
            if (result.issues.isNotEmpty()) {
                println("Sentence: $sen")
                result.issues.forEach { println(it) }
            }
        }
        val automaton_noBuild = rrs_dmdBuild.usedAutomatonFor("S")
        val automaton_preBuild = rrs_preBuild.buildFor("S", AutomatonKind.LOOKAHEAD_1)

        println("--Pre Build--")
        println(rrs_preBuild.usedAutomatonToString("S"))
        println("--Build-on-Demand--")
        println(rrs_dmdBuild.usedAutomatonToString("S"))

        val noBuildStates = (automaton_noBuild as ParserStateSet).allBuiltStates
        val preBuildStates = (automaton_preBuild as ParserStateSet).allBuiltStates
        assertEquals(preBuildStates.size, noBuildStates.size)
    }
}