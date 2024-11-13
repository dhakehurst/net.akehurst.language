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

package net.akehurst.language.automaton.leftcorner.concatenation

import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.automaton.api.AutomatonKind
import net.akehurst.language.automaton.leftcorner.AutomatonTest
import net.akehurst.language.automaton.leftcorner.automaton
import net.akehurst.language.automaton.leftcorner.test_AutomatonAbstract
import net.akehurst.language.parser.leftcorner.LeftCornerParser
import net.akehurst.language.regex.agl.RegexEnginePlatform
import net.akehurst.language.scanner.common.ScannerOnDemand
import kotlin.test.Test

class test_concatenation_ABC_DEF_GHI : test_AutomatonAbstract() {
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
    private val GHI = rrs.findRuntimeRule("GHI")

    private val a = rrs.findRuntimeRule("'a'")
    private val b = rrs.findRuntimeRule("'b'")
    private val cT = rrs.findRuntimeRule("'c'")
    private val d = rrs.findRuntimeRule("'d'")
    private val e = rrs.findRuntimeRule("'e'")
    private val f = rrs.findRuntimeRule("'f'")
    private val gT = rrs.findRuntimeRule("'g'")
    private val h = rrs.findRuntimeRule("'h'")
    private val i = rrs.findRuntimeRule("'i'")

    @Test
    fun parse_abcdefghi() {
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        parser.parseForGoal("S", "abcdefghi")
        println(rrs.usedAutomatonToString("S"))
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(RP(G, oN, SOR))     // G=.S           ctx()
            state(RP(a, oN, ER))      // a              ctx(G=.S)
            state(RP(AB, oN, p1))     // AB=a.b         ctx(G=.S)
            state(RP(b, oN, ER))      // b              ctx(AB=a.b)
            state(RP(AB, oN, ER))     // AB=ab.         ctx(G=.S)
            state(RP(ABC, oN, p1))    // ABC=AB.C       ctx(G=.S)
            state(RP(cT, oN, ER))     // c              ctx(ABC=AB.C)
            state(RP(C, oN, ER))      // C=c.           ctx(ABC=AB.C)
            state(RP(ABC, oN, ER))    // ABC=AB C.      ctx(G=.S)
            state(RP(S, oN, p1))      // S=ABC. DEF GHI ctx(G=.S)
            state(RP(d, oN, ER))      // d              ctx(S=ABC. DEF GHI)
            state(RP(DEF, oN, p1))    // DEF=d.EF       ctx(S=ABC. DEF GHI)
            state(RP(e, oN, ER))      // e              ctx(DEF=d.E)
            state(RP(EF, oN, p1))     // EF=e.f         ctx(DEF=d.E)
            state(RP(f, oN, ER))      // f              ctx(DEF=d.E)
            state(RP(EF, oN, ER))     // EF=e f.        ctx(DEF=d.E)
            state(RP(DEF, oN, ER))    // DEF=d EF .     ctx(S=ABC. DEF GHI)
            state(RP(S, oN, p2))      // S=ABC DEF. GHI ctx(G=.S)
            state(gT, oN, ER)           // g              ctx(S=ABC DEF. GHI)
            state(GHI, oN, p1)          // GHI=g.hi       ctx(S=ABC DEF. GHI)
            state(h, oN, ER)            // g              ctx(S=ABC DEF. GHI)
            state(GHI, oN, p2)          // GHI=gh.i       ctx(S=ABC DEF. GHI)
            state(i, oN, ER)            // g              ctx(S=ABC DEF. GHI)
            state(GHI, oN, ER)          // GHI=ghi.       ctx(S=ABC DEF. GHI)
            state(RP(G, oN, EOR))     // G=S.           ctx()


            trans(WIDTH) { src(G, oN, SR); tgt(a); lhg(b); ctx(G, oN, SR) }
            trans(WIDTH) { src(AB, oN, p1); tgt(b); lhg(RT); ctx(G, oN, SR) }
            trans(WIDTH) { src(ABC, oN, p1); tgt(cT); lhg(RT); ctx(G, oN, SR) }
            trans(WIDTH) { src(S, oN, p1); tgt(d); lhg(e); ctx(G, oN, SR) }
            trans(WIDTH) { src(DEF, oN, p1); tgt(e); lhg(f); ctx(DEF, oN, p1) }
            trans(WIDTH) { src(EF, oN, p1); tgt(f); lhg(RT); ctx(S, oN, p1) }
            trans(WIDTH) { src(S, oN, p2); tgt(gT); lhg(h); ctx(G, oN, SR) }
            trans(WIDTH) { src(GHI, oN, p1); tgt(h); lhg(i); ctx(S, oN, p2) }
            trans(WIDTH) { src(GHI, oN, p2); tgt(i); lhg(RT); ctx(S, oN, p2) }
            trans(GOAL) { src(S); tgt(G); lhg(EOT); ctx(G, oN, SR) }

            trans(HEIGHT) { src(a); tgt(AB, oN, p1); lhg(setOf(b), setOf(cT)); ctx(G, oN, SR) }
            trans(GRAFT) { src(b); tgt(AB); lhg(RT); ctx(AB, oN, p1) }
            trans(HEIGHT) { src(AB); tgt(ABC, oN, p1); lhg(setOf(cT), setOf(d)); ctx(G, oN, SR) }
            trans(GRAFT) { src(C); tgt(ABC); lhg(RT); ctx(ABC, oN, p1) }
            trans(HEIGHT) { src(cT); tgt(C); lhg(setOf(RT), setOf(RT)); ctx(ABC, oN, p1) }
            trans(HEIGHT) { src(d); tgt(C); lhg(setOf(RT), setOf(RT)); ctx(ABC, oN, p1) }
            trans(GRAFT) { src(EF); tgt(C); lhg(setOf(RT), setOf(RT)); ctx(ABC, oN, p1) }
            trans(HEIGHT) { src(e); tgt(C); lhg(setOf(RT), setOf(RT)); ctx(ABC, oN, p1) }
            trans(GRAFT) { src(f); tgt(C); lhg(setOf(RT), setOf(RT)); ctx(ABC, oN, p1) }
            trans(HEIGHT) { src(gT); tgt(C); lhg(setOf(RT), setOf(RT)); ctx(ABC, oN, p1) }
            trans(GRAFT) { src(h); tgt(C); lhg(setOf(RT), setOf(RT)); ctx(ABC, oN, p1) }
            trans(GRAFT) { src(i); tgt(C); lhg(setOf(RT), setOf(RT)); ctx(ABC, oN, p1) }
            trans(HEIGHT) { src(ABC); tgt(S, oN, p2); lhg(setOf(d), setOf(RT)); ctx(G, oN, SR) }
            trans(GRAFT) { src(DEF); tgt(S, oN, p2); lhg(setOf(gT)); ctx(ABC, oN, p1) }
            trans(GRAFT) { src(GHI); tgt(S); lhg(setOf(RT)); ctx(S, oN, p2) }
        }
        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun buildFor() {
        val actual = rrs.buildFor("S", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))

        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        parser.parseForGoal("S", "abcdefghi")

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            val s0 = state(RP(G, oN, SOR))
            val s1 = state(RP(a, oN, ER))
            val s2 = state(RP(S, oN, 1))
            val s3 = state(RP(b, oN, ER))
            val s4 = state(RP(S, oN, 2))
            val s5 = state(RP(cT, oN, ER))
            val s6 = state(RP(S, oN, ER))
            val s7 = state(RP(G, oN, ER))

            transition(s0, s0, s1, WIDTH, setOf(b), setOf(), null)
            transition(s0, s1, s2, HEIGHT, setOf(b), setOf(setOf(EOT)), setOf(RP(S, oN, SOR)))
            transition(s0, s2, s3, WIDTH, setOf(cT), setOf(), null)
            transition(s2, s3, s4, GRAFT, setOf(cT), setOf(setOf(EOT)), setOf(RP(S, oN, 1)))
            transition(s0, s4, s5, WIDTH, setOf(EOT), setOf(), null)
            transition(s4, s5, s6, GRAFT, setOf(EOT), setOf(setOf(EOT)), setOf(RP(S, oN,2)))
            transition(s0, s6, s7, GRAFT, setOf(EOT), setOf(setOf(EOT)), setOf(RP(G, oN, 0)))
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

        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs_noBuild.nonSkipTerminals), rrs_noBuild)
        val sentences = listOf("abcdefghi")
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