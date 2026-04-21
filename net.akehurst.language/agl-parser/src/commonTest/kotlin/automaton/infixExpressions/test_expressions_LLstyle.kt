/**
 * Copyright (C) 2022 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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
package net.akehurst.language.automaton.leftcorner.infixExpressions

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.automaton.api.AutomatonKind
import net.akehurst.language.automaton.leftcorner.automaton
import net.akehurst.language.automaton.leftcorner.test_AutomatonAbstract
import net.akehurst.language.parser.leftcorner.LeftCornerParser
import net.akehurst.language.regex.agl.RegexEnginePlatform
import net.akehurst.language.scanner.common.ScannerOnDemand
import testFixture.utils.AutomatonTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class test_expressions_LLstyle : test_AutomatonAbstract() {

    // S = E
    // E = P
    //   | E '+' P

    // S = E
    // E = P | E1
    // E1 = E o P
    // P = a

    // must be fresh per test or automaton is not correct for different parses (due to caching)
    private val rrs = runtimeRuleSet {
        concatenation("S") { ref("E") }
        choice("E", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
            ref("P")
            ref("E1")
        }
        concatenation("E1") { ref("E"); literal("o"); ref("P") }
        concatenation("P") { literal("a") }
    }
    private val S = rrs.findRuntimeRule("S")
    private val SM = rrs.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
    private val G = SM.startState.runtimeRules.first()
    private val E = rrs.findRuntimeRule("E")
    private val E1 = rrs.findRuntimeRule("E1")
    private val P = rrs.findRuntimeRule("P")
    private val o = rrs.findRuntimeRule("'o'")
    private val a = rrs.findRuntimeRule("'a'")


    private val lhs_a = SM.createLookaheadSet(false, false, false, setOf(a))

    @Test
    fun automaton_parse_aoa() {
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        val result = parser.parseForGoal("S", "aoaoaoa")
        println(parser.runtimeRuleSet.usedAutomatonToString("S"))
        assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(G,oN,SOR)
            state(a,oN,EOR)
            state(P,oN,EOR)
            state(E,o0,EOR)
            state(S,oN,EOR)
            state(E1,oN,1)
            state(o,oN,EOR)
            state(E1,oN,2)
            state(E1,oN,EOR)
            state(E,o1,EOR)
            state(G,oN,EOR)

            trans(WIDTH) { src(G, oN, SOR); tgt(a); lhg(setOf(EOT, o)); ctx(G, oN, SOR) }
            trans(WIDTH) { src(E1, oN, 2); tgt(a); lhg(RT); ctx(G, oN, SOR) }
            trans(WIDTH) { src(E1, oN, 1); tgt(o); lhg(a); ctx(G, oN, SOR) }

            trans(GOAL) { src(S); tgt(G, oN, EOR); lhg(EOT); ctx(G, oN, SOR); pctx(G, oN, SOR) }

            trans(HEIGHT) { src(P); tgt(E, o0, EOR); lhg(setOf(EOT, o), setOf(EOT, o)); ctx(G, oN, SOR); pctx(G, oN, SOR) }
            trans(HEIGHT) { src(E1); tgt(E, o1, EOR); lhg(setOf(RT, o), setOf(RT, o)); ctx(G, oN, SOR); pctx(G, oN, SOR) }
            trans(HEIGHT) { src(E, o0, EOR); tgt(E1, oN, 1); lhg(setOf(o), setOf(EOT, o)); ctx(G, oN, SOR); pctx(G, oN, SOR) }
            trans(HEIGHT) { src(E, o1, EOR); tgt(E1, oN, 1); lhg(setOf(o), setOf(RT, o)); ctx(G, oN, SOR); pctx(G, oN, SOR) }
            trans(HEIGHT) { src(a); tgt(P); lhg(setOf(EOT, o), setOf(EOT, o)); lhg(setOf(RT), setOf(RT)); ctx(RP(G, oN, SOR), RP(E1, oN, 2)); pctx(G, oN, SOR) }
            trans(HEIGHT) { src(E, o0, EOR); tgt(S); lhg(setOf(EOT), setOf(EOT)); ctx(G, oN, SOR); pctx(G, oN, SOR) }
            trans(HEIGHT) { src(E, o1, EOR); tgt(S); lhg(setOf(RT), setOf(RT)); ctx(G, oN, SOR); pctx(G, oN, SOR) }

            trans(GRAFT) { src(o); tgt(E1, oN, 2); lhg(a); ctx(E1, oN, 1); pctx(G, oN, SOR) }
            trans(GRAFT) { src(P); tgt(E1); lhg(RT); ctx(E1, oN, 2); pctx(G, oN, SOR) }
        }

        AutomatonTest.assertEquals(expected, actual)
    }


    @Test
    fun buildFor() {
        val actual = rrs.buildFor("S", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))

        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        val result = parser.parseForGoal("S", "aoaoaoa")
        assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(G,oN,SOR)
            state(G,oN,EOR)
            state(S,oN,EOR)
            state(E,o0,EOR)
            state(E,o1,EOR)
            state(P,oN,EOR)
            state(E1,oN,1)
            state(E1,oN,2)
            state(E1,oN,EOR)
            state(a,oN,EOR)
            state(o,oN,EOR)

            trans(WIDTH) { src(G, oN, SOR); tgt(a); lhg(setOf(EOT, o)); ctx(G, oN, SOR) }
            trans(WIDTH) { src(E1, oN, 2); tgt(a); lhg(setOf(EOT, o)); ctx(G, oN, SOR) }
            trans(WIDTH) { src(E1, oN, 1); tgt(o); lhg(a); ctx(G, oN, SOR) }

            trans(GOAL) { src(S); tgt(G, oN, EOR); lhg(EOT); ctx(G, oN, SOR); pctx(G, oN, SOR) }

            trans(HEIGHT) { src(P); tgt(E, o0, EOR); lhg(setOf(EOT, o), setOf(EOT, o)); ctx(G, oN, SOR); pctx(G, oN, SOR) }
            trans(HEIGHT) { src(E1); tgt(E, o1, EOR); lhg(setOf(EOT, o), setOf(EOT, o)); ctx(G, oN, SOR); pctx(G, oN, SOR) }
            trans(HEIGHT) { src(E, o0, EOR); tgt(E1, oN, 1); lhg(setOf(o), setOf(EOT, o)); ctx(G, oN, SOR); pctx(G, oN, SOR) }
            trans(HEIGHT) { src(E, o1, EOR); tgt(E1, oN, 1); lhg(setOf(o), setOf(EOT, o)); ctx(G, oN, SOR); pctx(G, oN, SOR) }
            trans(HEIGHT) { src(a); tgt(P); lhg(setOf(EOT, o), setOf(EOT, o)); ctx(RP(G, oN, SOR), RP(E1, oN, 2)); pctx(G, oN, SOR) }
            trans(HEIGHT) { src(E, o0, EOR); tgt(S); lhg(EOT, EOT); ctx(G, oN, SOR); pctx(G, oN, SOR) }
            trans(HEIGHT) { src(E, o1, EOR); tgt(S); lhg(EOT, EOT); ctx(G, oN, SOR); pctx(G, oN, SOR) }

            trans(GRAFT) { src(o); tgt(E1, oN, 2); lhg(a); ctx(E1, oN, 1); pctx(G, oN, SOR) }
            trans(GRAFT) { src(P); tgt(E1); lhg(setOf(EOT, o)); ctx(E1, oN, 2); pctx(G, oN, SOR) }
        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun compare() {
        val rrs_noBuild = rrs.clone()
        val rrs_preBuild = rrs.clone()

        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs_noBuild.nonSkipTerminals), rrs_noBuild)
        val sentences = listOf("a", "aoa", "aoaoa", "aoaoaoa")
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