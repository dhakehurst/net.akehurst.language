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
import testFixture.utils.AutomatonTest
import kotlin.test.Test

class test_AhoSetiUlman_4_54 : test_AutomatonAbstract() {
    // S = C C ;
    // C = c C | d ;
    //
    // S = C C ;
    // C = C1 | d ;
    // C1 = c C ;
    // must be fresh per test or automaton is not correct for different parses (due to caching)
    private val rrs = runtimeRuleSet {
        concatenation("S") { ref("C"); ref("C") }
        choice("C", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
            ref("C1")
            literal("d")
        }
        concatenation("C1") { literal("c"); ref("C") }
    }

    private val S = rrs.findRuntimeRule("S")
    private val SM = rrs.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

    private val C = rrs.findRuntimeRule("C")
    private val C1 = rrs.findRuntimeRule("C1")
    private val T_c = rrs.findRuntimeRule("'c'")
    private val T_d = rrs.findRuntimeRule("'d'")
    private val rG = SM.startState.runtimeRules.first()

    private val lhs_cd = SM.createLookaheadSet(false, false, false, setOf(T_c, T_d))

    @Test
    fun parse_dd() {
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        parser.parseForGoal("S", "dd")
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))
        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(rG,oN,SOR)
            state(T_c,oN,EOR)
            state(T_d,oN,EOR)
            state(C,o1,EOR)
            state(S,oN,1)
            state(S,oN,EOR)
            state(rG,oN,EOR)

            trans(WIDTH) { src(rG, oN, SOR); tgt(T_c); lhg(setOf(T_c, T_d)); ctx(rG, oN, SOR) }
            trans(WIDTH) { src(S, oN, 1); tgt(T_c); lhg(setOf(T_c, T_d)); ctx(rG, oN, SOR) }
            trans(WIDTH) { src(rG, oN, SOR); tgt(T_d); lhg(setOf(T_c, T_d)); ctx(rG, oN, SOR) }
            trans(WIDTH) { src(S, oN, 1); tgt(T_d); lhg(setOf(RT)); ctx(rG, oN, SOR) }

            trans(GOAL) { src(S); tgt(rG); lhg(setOf(EOT)); ctx(rG, oN, SOR); pctx(rG, oN, SOR) }

            trans(HEIGHT) { src(T_d); tgt(C, o1, EOR); lhg(setOf(T_c, T_d), setOf(T_c, T_d)); lhg(setOf(RT), setOf(RT)); ctx(RP(rG, oN, SOR), RP(S, oN, 1)); pctx(rG, oN, SOR) }
            trans(HEIGHT) { src(C, o1, EOR); tgt(S, oN, 1); lhg(setOf(T_c, T_d), setOf(EOT)); ctx(rG, oN, SOR); pctx(rG, oN, SOR) }
            trans(GRAFT) { src(C, o1, EOR); tgt(S); lhg(setOf(RT)); ctx(S, oN, 1); pctx(rG, oN, SOR) }

        }
        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun buildFor() {
        val actual = rrs.buildFor("S", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(rG,oN,SOR)
            state(rG,oN,EOR)
            state(S,oN,1)
            state(S,oN,EOR)
            state(C,o0,EOR)
            state(C,o1,EOR)
            state(C1,oN,1)
            state(C1,oN,EOR)
            state(T_c,oN,EOR)
            state(T_d,oN,EOR)

            trans(WIDTH) { src(rG, oN, SOR); tgt(T_c); lhg(setOf(T_c, T_d)); ctx(rG, oN, SOR) }
            trans(WIDTH) { src(C1, oN, 1); tgt(T_c); lhg(setOf(T_c, T_d)); ctx(RP(rG, oN, SOR), RP(C1, oN, 1), RP(S, oN, 1)) }
            trans(WIDTH) { src(S, oN, 1); tgt(T_c); lhg(setOf(T_c, T_d)); ctx(rG, oN, SOR) }
            trans(WIDTH) { src(rG, oN, SOR); tgt(T_d); lhg(setOf(T_c, T_d)); ctx(rG, oN, SOR) }
            trans(WIDTH) { src(C1, oN, 1); tgt(T_d); lhg(setOf(EOT, T_c, T_d)); ctx(RP(rG, oN, SOR), RP(C1, oN, 1), RP(S, oN, 1)) }
            trans(WIDTH) { src(S, oN, 1); tgt(T_d); lhg(setOf(EOT)); ctx(rG, oN, SOR) }

            trans(GOAL) { src(S); tgt(rG); lhg(setOf(EOT)); ctx(rG, oN, SOR); pctx(rG, oN, SOR) }

            trans(HEIGHT) { src(C1); tgt(C, o0, EOR); lhg(setOf(EOT, T_c, T_d), setOf(EOT, T_c, T_d)); ctx(RP(rG, oN, SOR), RP(C1, oN, 1), RP(S, oN, 1)); pctx(RP(rG, oN, SOR), RP(C1, oN, 1), RP(S, oN, 1)) }
            trans(HEIGHT) { src(T_d); tgt(C, o1, EOR); lhg(setOf(EOT, T_c, T_d), setOf(EOT, T_c, T_d)); ctx(RP(C1, oN, 1), RP(S, oN, 1), RP(rG, oN, SOR)); pctx(RP(C1, oN, 1), RP(S, oN, 1), RP(rG, oN, SOR)) }
            trans(HEIGHT) { src(T_c); tgt(C1, oN, 1); lhg(setOf(T_c, T_d), setOf(EOT, T_c, T_d)); ctx(RP(C1, oN, 1), RP(S, oN, 1), RP(rG, oN, SOR)); pctx(RP(C1, oN, 1), RP(S, oN, 1), RP(rG, oN, SOR)) }
            trans(GRAFT) { src(C, o0, EOR); tgt(C1); lhg(setOf(EOT, T_c, T_d)); ctx(C1, oN, 1); pctx(RP(rG, oN, SOR), RP(C1, oN, 1), RP(S, oN, 1)) }
            trans(GRAFT) { src(C, o1, EOR); tgt(C1); lhg(setOf(EOT, T_c, T_d)); ctx(C1, oN, 1); pctx(RP(rG, oN, SOR), RP(C1, oN, 1), RP(S, oN, 1)) }
            trans(HEIGHT) { src(C, o0, EOR); tgt(S, oN, 1); lhg(setOf(T_c, T_d), setOf(EOT)); ctx(rG, oN, SOR); pctx(rG, oN, SOR) }
            trans(HEIGHT) { src(C, o1, EOR); tgt(S, oN, 1); lhg(setOf(T_c, T_d), setOf(EOT)); ctx(rG, oN, SOR); pctx(rG, oN, SOR) }
            trans(GRAFT) { src(C, o0, EOR); tgt(S); lhg(setOf(EOT)); ctx(S, oN, 1); pctx(rG, oN, SOR) }
            trans(GRAFT) { src(C, o1, EOR); tgt(S); lhg(setOf(EOT)); ctx(S, oN, 1); pctx(rG, oN, SOR) }

        }

        AutomatonTest.assertEquals(expected, actual)
    }
}