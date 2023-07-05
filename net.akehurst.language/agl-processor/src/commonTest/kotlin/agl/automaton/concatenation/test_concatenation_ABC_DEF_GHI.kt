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

package net.akehurst.language.agl.automaton.concatenation

import net.akehurst.language.agl.automaton.AutomatonTest
import net.akehurst.language.agl.automaton.automaton
import net.akehurst.language.agl.automaton.test_AutomatonAbstract
import net.akehurst.language.agl.parser.ScanOnDemandParser
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.processor.AutomatonKind
import kotlin.test.Test

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
        val parser = ScanOnDemandParser(rrs)
        parser.parseForGoal("S", "abcdefghi")
        println(rrs.usedAutomatonToString("S"))
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(RP(G, o0, SOR))     // G=.S           ctx()
            state(RP(a, o0, ER))      // a              ctx(G=.S)
            state(RP(AB, o0, p1))     // AB=a.b         ctx(G=.S)
            state(RP(b, o0, ER))      // b              ctx(AB=a.b)
            state(RP(AB, o0, ER))     // AB=ab.         ctx(G=.S)
            state(RP(ABC, o0, p1))    // ABC=AB.C       ctx(G=.S)
            state(RP(cT, o0, ER))     // c              ctx(ABC=AB.C)
            state(RP(C, o0, ER))      // C=c.           ctx(ABC=AB.C)
            state(RP(ABC, o0, ER))    // ABC=AB C.      ctx(G=.S)
            state(RP(S, o0, p1))      // S=ABC. DEF GHI ctx(G=.S)
            state(RP(d, o0, ER))      // d              ctx(S=ABC. DEF GHI)
            state(RP(DEF, o0, p1))    // DEF=d.EF       ctx(S=ABC. DEF GHI)
            state(RP(e, o0, ER))      // e              ctx(DEF=d.E)
            state(RP(EF, o0, p1))     // EF=e.f         ctx(DEF=d.E)
            state(RP(f, o0, ER))      // f              ctx(DEF=d.E)
            state(RP(EF, o0, ER))     // EF=e f.        ctx(DEF=d.E)
            state(RP(DEF, o0, ER))    // DEF=d EF .     ctx(S=ABC. DEF GHI)
            state(RP(S, o0, p2))      // S=ABC DEF. GHI ctx(G=.S)
            state(gT, o0, ER)           // g              ctx(S=ABC DEF. GHI)
            state(GHI, o0, p1)          // GHI=g.hi       ctx(S=ABC DEF. GHI)
            state(h, o0, ER)            // g              ctx(S=ABC DEF. GHI)
            state(GHI, o0, p2)          // GHI=gh.i       ctx(S=ABC DEF. GHI)
            state(i, o0, ER)            // g              ctx(S=ABC DEF. GHI)
            state(GHI, o0, ER)          // GHI=ghi.       ctx(S=ABC DEF. GHI)
            state(RP(G, o0, EOR))     // G=S.           ctx()


            trans(WIDTH) { src(G, o0, SR); tgt(a); lhg(b); ctx(G, o0, SR) }
            trans(WIDTH) { src(AB, o0, p1); tgt(b); lhg(RT); ctx(G, o0, SR) }
            trans(WIDTH) { src(ABC, o0, p1); tgt(cT); lhg(RT); ctx(G, o0, SR) }
            trans(WIDTH) { src(S, o0, p1); tgt(d); lhg(e); ctx(G, o0, SR) }
            trans(WIDTH) { src(DEF, o0, p1); tgt(e); lhg(f); ctx(DEF, o0, p1) }
            trans(WIDTH) { src(EF, o0, p1); tgt(f); lhg(RT); ctx(S, o0, p1) }
            trans(WIDTH) { src(S, o0, p2); tgt(gT); lhg(h); ctx(G, o0, SR) }
            trans(WIDTH) { src(GHI, o0, p1); tgt(h); lhg(i); ctx(S, o0, p2) }
            trans(WIDTH) { src(GHI, o0, p2); tgt(i); lhg(RT); ctx(S, o0, p2) }
            trans(GOAL) { src(S); tgt(G); lhg(EOT); ctx(G, o0, SR) }

            trans(HEIGHT) { src(a); tgt(AB, o0, p1); lhg(setOf(b), setOf(cT)); ctx(G, o0, SR) }
            trans(GRAFT) { src(b); tgt(AB); lhg(RT); ctx(AB, o0, p1) }
            trans(HEIGHT) { src(AB); tgt(ABC, o0, p1); lhg(setOf(cT), setOf(d)); ctx(G, o0, SR) }
            trans(GRAFT) { src(C); tgt(ABC); lhg(RT); ctx(ABC, o0, p1) }
            trans(HEIGHT) { src(cT); tgt(C); lhg(setOf(RT), setOf(RT)); ctx(ABC, o0, p1) }
            trans(HEIGHT) { src(d); tgt(C); lhg(setOf(RT), setOf(RT)); ctx(ABC, o0, p1) }
            trans(GRAFT) { src(EF); tgt(C); lhg(setOf(RT), setOf(RT)); ctx(ABC, o0, p1) }
            trans(HEIGHT) { src(e); tgt(C); lhg(setOf(RT), setOf(RT)); ctx(ABC, o0, p1) }
            trans(GRAFT) { src(f); tgt(C); lhg(setOf(RT), setOf(RT)); ctx(ABC, o0, p1) }
            trans(HEIGHT) { src(gT); tgt(C); lhg(setOf(RT), setOf(RT)); ctx(ABC, o0, p1) }
            trans(GRAFT) { src(h); tgt(C); lhg(setOf(RT), setOf(RT)); ctx(ABC, o0, p1) }
            trans(GRAFT) { src(i); tgt(C); lhg(setOf(RT), setOf(RT)); ctx(ABC, o0, p1) }
            trans(HEIGHT) { src(ABC); tgt(S, o0, p2); lhg(setOf(d), setOf(RT)); ctx(G, o0, SR) }
            trans(GRAFT) { src(DEF); tgt(S, o0, p2); lhg(setOf(gT)); ctx(ABC, o0, p1) }
            trans(GRAFT) { src(GHI); tgt(S); lhg(setOf(RT)); ctx(S, o0, p2) }
        }
        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun buildFor() {
        val actual = rrs.buildFor("S", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))

        val parser = ScanOnDemandParser(rrs)
        parser.parseForGoal("S", "abcdefghi")

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            val s0 = state(RP(G, 0, SOR))
            val s1 = state(RP(a, 0, ER))
            val s2 = state(RP(S, 0, 1))
            val s3 = state(RP(b, 0, ER))
            val s4 = state(RP(S, 0, 2))
            val s5 = state(RP(cT, 0, ER))
            val s6 = state(RP(S, 0, ER))
            val s7 = state(RP(G, 0, ER))

            transition(s0, s0, s1, WIDTH, setOf(b), setOf(), null)
            transition(s0, s1, s2, HEIGHT, setOf(b), setOf(setOf(EOT)), setOf(RP(S, 0, SOR)))
            transition(s0, s2, s3, WIDTH, setOf(cT), setOf(), null)
            transition(s2, s3, s4, GRAFT, setOf(cT), setOf(setOf(EOT)), setOf(RP(S, 0, 1)))
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