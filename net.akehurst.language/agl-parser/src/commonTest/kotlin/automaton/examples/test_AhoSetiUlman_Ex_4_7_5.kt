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
import kotlin.test.assertNotNull

class test_AhoSetiUlman_Ex_4_7_5 : test_AutomatonAbstract() {

    // This grammar is LR(1) but not LALR(1)
    // TODO...from where?

    // S = A a | b A c | B c | b B a ;
    // A = d ;
    // B = d ;
    //
    // S = S1 | S2 | S3 | S4
    // S1 = A a ;
    // S2 = b A c ;
    // S3 = B c ;
    // S4 = b B a ;
    // A = d ;
    // B = d ;

    // must be fresh per test or automaton is not correct for different parses (due to caching)
    private val rrs = runtimeRuleSet {
        choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
            ref("S1")
            ref("S2")
            ref("S3")
            ref("S4")
        }
        concatenation("S1") { ref("A"); literal("a") }
        concatenation("S2") { literal("b"); ref("A"); literal("c") }
        concatenation("S3") { ref("B"); literal("c") }
        concatenation("S4") { literal("b"); ref("B"); literal("a") }
        concatenation("A") { literal("d") }
        concatenation("B") { literal("d") }
    }
    private val S = rrs.findRuntimeRule("S")
    private val S1 = rrs.findRuntimeRule("S1")
    private val S2 = rrs.findRuntimeRule("S2")
    private val S3 = rrs.findRuntimeRule("S3")
    private val S4 = rrs.findRuntimeRule("S4")
    private val rA = rrs.findRuntimeRule("A")
    private val rB = rrs.findRuntimeRule("B")
    private val a = rrs.findRuntimeRule("'a'")
    private val b = rrs.findRuntimeRule("'b'")
    private val c = rrs.findRuntimeRule("'c'")
    private val d = rrs.findRuntimeRule("'d'")

    private val SM = rrs.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
    private val s0 = SM.startState
    private val rG = s0.runtimeRules.first()

    private val lhs_ac = SM.createLookaheadSet(false, false, false, setOf(a, c))
    private val lhs_a = SM.createLookaheadSet(false, false, false, setOf(a))
    private val lhs_c = SM.createLookaheadSet(false, false, false, setOf(c))
    private val lhs_d = SM.createLookaheadSet(false, false, false, setOf(d))


    @Test
    fun parse_da() {
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        parser.parseForGoal("S", "da")
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))
        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(rG,oN,SOR)
            state(d,oN,EOR)
            state(b,oN,EOR)
            state(rB,oN,EOR)
            state(rA,oN,EOR)
            state(S1,oN,1)
            state(a,oN,EOR)
            state(S1,oN,EOR)
            state(S,o0,EOR)
            state(rG,oN,EOR)

            trans(WIDTH) { src(S1, oN, 1); tgt(a); lhg(RT); ctx(rG, oN, SOR) }
            trans(WIDTH) { src(rG, oN, SOR); tgt(b); lhg(d); ctx(rG, oN, SOR) }
            trans(WIDTH) { src(rG, oN, SOR); tgt(d); lhg(setOf(a, c)); ctx(rG, oN, SOR) }

            trans(GOAL) { src(S, o0, EOR); tgt(rG, oN, EOR); lhg(EOT); ctx(rG, oN, SOR); pctx(rG, oN, SOR) }

            trans(HEIGHT) { src(d); tgt(rA); lhg(a, a); ctx(rG, oN, SOR); pctx(rG, oN, SOR) }
            trans(HEIGHT) { src(d); tgt(rB); lhg(c, c); ctx(rG, oN, SOR); pctx(rG, oN, SOR) }
            trans(HEIGHT) { src(S1); tgt(S, o0, EOR); lhg(RT, RT); ctx(rG, oN, SOR); pctx(rG, oN, SOR) }
            trans(HEIGHT) { src(rA); tgt(S1, oN, 1); lhg(a, EOT); ctx(rG, oN, SOR); pctx(rG, oN, SOR) }

            trans(GRAFT) { src(a); tgt(S1); lhg(RT); ctx(S1, oN, 1); pctx(rG, oN, SOR) }
        }
        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun buildFor() {
        val actual = rrs.buildFor("S", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))

        val sentences = setOf("da", "bdc", "dc", "bda")
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        sentences.forEach {
            val result = parser.parseForGoal("S", it)
            assertNotNull(result.sppt)
        }

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(RP(rG, oN, SR))
            state(RP(rG, oN, ER))
            state(RP(S, o0, ER))
            state(RP(S, o1, ER))
            state(RP(S, o2, ER))
            state(RP(S, o3, ER))
            state(RP(S1, oN, 1))
            state(RP(S1, oN, ER))
            state(RP(rA, oN, ER))
            state(RP(S2, oN, 1))
            state(RP(S2, oN, 2))
            state(RP(S2, oN, ER))
            state(RP(S3, oN, 1))
            state(RP(S3, oN, ER))
            state(RP(rB, oN, ER))
            state(RP(S4, oN, 1))
            state(RP(S4, oN, 2))
            state(RP(S4, oN, ER))
            state(RP(d, oN, ER))
            state(RP(a, oN, ER))
            state(RP(b, oN, ER))
            state(RP(c, oN, ER))

            trans(WIDTH) { src(S1, oN, 1); tgt(a); lhg(EOT); ctx(rG, oN, SOR) }
            trans(WIDTH) { src(S4, oN, 2); tgt(a); lhg(EOT); ctx(rG, oN, SOR) }
            trans(WIDTH) { src(rG, oN, SOR); tgt(b); lhg(d); ctx(rG, oN, SOR) }
            trans(WIDTH) { src(S2, oN, 2); tgt(c); lhg(EOT); ctx(rG, oN, SOR) }
            trans(WIDTH) { src(S3, oN, 1); tgt(c); lhg(EOT); ctx(rG, oN, SOR) }
            trans(WIDTH) { src(rG, oN, SOR); tgt(d); lhg(setOf(a, c)); ctx(rG, oN, SOR) }
            trans(WIDTH) { src(S2, oN, 1); tgt(d); lhg(c); ctx(rG, oN, SOR) }
            trans(WIDTH) { src(S4, oN, 1); tgt(d); lhg(a); ctx(rG, oN, SOR) }

            trans(GOAL) { src(S, o0, EOR); tgt(rG, oN, EOR); lhg(EOT); ctx(rG, oN, SOR); pctx(rG, oN, SOR) }
            trans(GOAL) { src(S, o1, EOR); tgt(rG, oN, EOR); lhg(EOT); ctx(rG, oN, SOR); pctx(rG, oN, SOR) }
            trans(GOAL) { src(S, o2, EOR); tgt(rG, oN, EOR); lhg(EOT); ctx(rG, oN, SOR); pctx(rG, oN, SOR) }
            trans(GOAL) { src(S, o3, EOR); tgt(rG, oN, EOR); lhg(EOT); ctx(rG, oN, SOR); pctx(rG, oN, SOR) }

            trans(HEIGHT) { src(d); tgt(rA); lhg(setOf(a, c), setOf(a, c)); ctx(RP(rG, oN, SOR), RP(S2, oN, 1)); pctx(rG, oN, SOR) }
            trans(HEIGHT) { src(d); tgt(rB); lhg(setOf(a, c), setOf(a, c)); ctx(RP(rG, oN, SOR), RP(S4, oN, 1)); pctx(rG, oN, SOR) }
            trans(HEIGHT) { src(S1); tgt(S, o0, EOR); lhg(EOT, EOT); ctx(rG, oN, SOR); pctx(rG, oN, SOR) }
            trans(HEIGHT) { src(S2); tgt(S, o1, EOR); lhg(EOT, EOT); ctx(rG, oN, SOR); pctx(rG, oN, SOR) }
            trans(HEIGHT) { src(S3); tgt(S, o2, EOR); lhg(EOT, EOT); ctx(rG, oN, SOR); pctx(rG, oN, SOR) }
            trans(HEIGHT) { src(S4); tgt(S, o3, EOR); lhg(EOT, EOT); ctx(rG, oN, SOR); pctx(rG, oN, SOR) }
            trans(HEIGHT) { src(rA); tgt(S1, oN, 1); lhg(a, EOT); ctx(rG, oN, SOR); pctx(rG, oN, SOR) }
            trans(HEIGHT) { src(b); tgt(S2, oN, 1); lhg(d, EOT); ctx(rG, oN, SOR); pctx(rG, oN, SOR) }
            trans(HEIGHT) { src(rB); tgt(S3, oN, 1); lhg(c, EOT); ctx(rG, oN, SOR); pctx(rG, oN, SOR) }
            trans(HEIGHT) { src(b); tgt(S4, oN, 1); lhg(d, EOT); ctx(rG, oN, SOR); pctx(rG, oN, SOR) }

            trans(GRAFT) { src(a); tgt(S1); lhg(EOT); ctx(S1, oN, 1); pctx(rG, oN, SOR) }
            trans(GRAFT) { src(rA); tgt(S2, oN, 2); lhg(c); ctx(S2, oN, 1); pctx(rG, oN, SOR) }
            trans(GRAFT) { src(c); tgt(S2); lhg(EOT); ctx(S2, oN, 2); pctx(rG, oN, SOR) }
            trans(GRAFT) { src(c); tgt(S3); lhg(EOT); ctx(S3, oN, 1); pctx(rG, oN, SOR) }
            trans(GRAFT) { src(rB); tgt(S4, oN, 2); lhg(a); ctx(S4, oN, 1); pctx(rG, oN, SOR) }
            trans(GRAFT) { src(a); tgt(S4); lhg(EOT); ctx(S4, oN, 2); pctx(rG, oN, SOR) }
        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun compare() {
        val rrs_noBuild = rrs.clone()
        val rrs_preBuild = rrs.clone()

        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs_noBuild.nonSkipTerminals), rrs_noBuild)
        val sentences = listOf("da", "bdc", "dc", "bda")
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

        AutomatonTest.assertEquals(automaton_preBuild, automaton_noBuild, AutomatonTest.MatchConfiguration(no_lookahead_compare = true))
    }

}