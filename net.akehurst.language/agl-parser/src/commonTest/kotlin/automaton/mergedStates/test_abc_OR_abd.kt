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

class test_abc_OR_abd : test_AutomatonAbstract() {

    // S =  ABC | ABD
    // ABC = a b c
    // ABD = a b d

    // must be fresh per test or automaton is not correct for different parses (due to caching)
    private val rrs = runtimeRuleSet {
        choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
            ref("ABC")
            ref("ABD")
        }
        concatenation("ABC") { literal("a"); literal("b"); literal("c") }
        concatenation("ABD") { literal("a"); literal("b"); literal("d") }
    }
    private val S = rrs.findRuntimeRule("S")
    private val SM = rrs.fetchStateSetFor("S", AutomatonKind.LOOKAHEAD_1)
    private val G = SM.startState.runtimeRules.first()
    private val ABC = rrs.findRuntimeRule("ABC")
    private val ABD = rrs.findRuntimeRule("ABD")
    private val a = rrs.findRuntimeRule("'a'")
    private val b = rrs.findRuntimeRule("'b'")
    private val c = rrs.findRuntimeRule("'c'")
    private val d = rrs.findRuntimeRule("'d'")

    @Test
    fun automaton_parse_abc() {
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        val result = parser.parseForGoal("S", "abc")
        println(rrs.usedAutomatonToString("S"))
        assertNotNull(result.sppt)
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(G, oN, SR)      /* G = . S   */
            state(a, oN, ER)      /* a .       */
            state(RP(ABC, oN, p1), RP(ABD, oN, p1))    /* ABC = a . bc, ABD = a . bd */
            state(b, oN, EOR)     /* b . */
            state(ABD, oN, p2)    /* ABD = ab . d  */
            state(ABC, oN, p2)    /* ABC = ab . c  */
            state(c, oN, EOR)     /* c . */
            state(ABC, oN, EOR)   /* ABC = abc . */
            state(S, oN, EOR)     /* S = ABC . */
            state(G, oN, EOR)     /* G = S .   */

            trans(WIDTH) { src(G, oN, SR); tgt(a); lhg(b); ctx(G, oN, SR) }
            trans(WIDTH) { src(setOf(RP(ABC, oN, p1), RP(ABD, oN, p1))); tgt(b); lhg(setOf(d, c)); ctx(G, oN, SR) }
            trans(WIDTH) { src(ABC, oN, p2); tgt(c); lhg(RT); ctx(G, oN, SR) }

            trans(GOAL) { src(S); tgt(G); lhg(EOT); ctx(G, oN, SR) }

            trans(GRAFT) { src(b); tgt(ABC, oN, p2); lhg(c); ctx(setOf(RP(ABC, oN, p1), RP(ABD, oN, p1))) }
            trans(GRAFT) { src(c); tgt(ABC); lhg(RT); ctx(ABC, oN, p2) }
            trans(HEIGHT) { src(a); tgt(setOf(RP(ABC, oN, p1), RP(ABD, oN, p1))); lhg(setOf(b), setOf(EOT)); ctx(G, oN, SR) }
            trans(GRAFT) { src(b); tgt(ABD, oN, p2); lhg(d); ctx(setOf(RP(ABC, oN, p1), RP(ABD, oN, p1))) }
            trans(HEIGHT) { src(ABC); tgt(S); lhg(setOf(RT), setOf(RT)); ctx(G, oN, SR) }

        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun automaton_parse_abd() {
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        val result = parser.parseForGoal("S", "abd")
        println(rrs.usedAutomatonToString("S"))
        assertNotNull(result.sppt)
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(G, oN, SR)     /* G = . S   */
            state(a, oN, ER)     /* a .       */
            state(ABD, oN, p1)   /* ABD = a . bd */
            state(ABC, oN, p1)   /* ABC = a . bc */
            state(b, oN, ER)     /* b . */
            state(ABD, oN, p2)   /* ABD = ab . d  */
            state(d, oN, ER)     /* d . */
            state(ABD, oN, ER)   /* ABD = abd . */
            state(S, o1, ER)     /* S = ABD . */
            state(G, oN, ER)     /* G = S .   */

            trans(WIDTH) { src(G, oN, SR); tgt(a); lhg(b); ctx(G, oN, SR) }
            trans(WIDTH) { src(ABC, oN, p1); tgt(b); lhg(c); ctx(G, oN, SR) }
            trans(WIDTH) { src(ABD, oN, p1); tgt(b); lhg(d); ctx(G, oN, SR) }
            trans(WIDTH) { src(ABD, oN, p2); tgt(d); lhg(RT); ctx(G, oN, SR) }

            trans(GOAL) { src(S, o1, ER); tgt(G); lhg(EOT); ctx(G, oN, SR) }

            trans(HEIGHT) { src(a); tgt(ABC, oN, p1); lhg(setOf(b), setOf(EOT)); ctx(G, oN, SR) }
            trans(GRAFT) { src(d); tgt(ABD); lhg(RT); ctx(ABD, oN, p2) }
            trans(HEIGHT) { src(a); tgt(ABD, oN, p1); lhg(setOf(b), setOf(EOT)); ctx(G, oN, SR) }
            trans(GRAFT) { src(b); tgt(ABD, oN, p2); lhg(d); ctx(ABD, oN, p1) }
            trans(HEIGHT) { src(ABD); tgt(S, o1, ER); lhg(setOf(RT), setOf(RT)); ctx(G, oN, SR) }
        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun buildFor() {
        val actual = rrs.buildFor("S", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))

        val sentences = setOf("abc", "abd")
        sentences.forEach {
            val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
            val result = parser.parseForGoal("S", it)
            assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
            assertEquals(0, result.issues.size)
            assertEquals(1, result.sppt!!.maxNumHeads)
        }

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(G, oN, SR)     /* G = . S   */
            state(G, oN, ER)     /* G = S .   */
            state(S, oN, ER)     /* S = ABC . */
            state(S, o1, ER)     /* S = ABD . */
            state(ABC, oN, p1)   /* ABC = a . bc */
            state(ABC, oN, p2)   /* ABC = ab . c */
            state(ABC, oN, ER)   /* ABC = abc . */
            state(ABD, oN, p1)   /* ABD = a . bd */
            state(ABD, oN, p2)   /* ABD = ab . d  */
            state(ABD, oN, ER)   /* ABD = abd . */
            state(a, oN, ER)     /* a .       */
            state(b, oN, ER)     /* b . */
            state(c, oN, ER)     /* c . */
            state(d, oN, ER)     /* d . */

            trans(WIDTH) { src(G, oN, SR); tgt(a); lhg(b); ctx(G, oN, SR) }
            trans(WIDTH) { src(ABC, oN, p1); tgt(b); lhg(c); ctx(G, oN, SR) }
            trans(WIDTH) { src(ABD, oN, p1); tgt(b); lhg(d); ctx(G, oN, SR) }
            trans(WIDTH) { src(ABC, oN, p2); tgt(c); lhg(EOT); ctx(G, oN, SR) }
            trans(WIDTH) { src(ABD, oN, p2); tgt(d); lhg(EOT); ctx(G, oN, SR) }

            trans(GOAL) { src(S, oN, ER); tgt(G); lhg(EOT); ctx(G, oN, SR) }
            trans(GOAL) { src(S, o1, ER); tgt(G); lhg(EOT); ctx(G, oN, SR) }

            trans(GRAFT) { src(c); tgt(ABC); lhg(EOT); ctx(ABC, oN, p2) }
            trans(HEIGHT) { src(a); tgt(ABC, oN, p1); lhg(setOf(b), setOf(EOT)); ctx(G, oN, SR) }
            trans(GRAFT) { src(b); tgt(ABC, oN, p2); lhg(c); ctx(ABC, oN, p1) }
            trans(GRAFT) { src(d); tgt(ABD); lhg(EOT); ctx(ABD, oN, p2) }
            trans(HEIGHT) { src(a); tgt(ABD, oN, p1); lhg(setOf(b), setOf(EOT)); ctx(G, oN, SR) }
            trans(GRAFT) { src(b); tgt(ABD, oN, p2); lhg(d); ctx(ABD, oN, p1) }
            trans(HEIGHT) { src(ABC); tgt(S, oN, ER); lhg(setOf(EOT), setOf(EOT)); ctx(G, oN, SR) }
            trans(HEIGHT) { src(ABD); tgt(S, o1, ER); lhg(setOf(EOT), setOf(EOT)); ctx(G, oN, SR) }
        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun compare() {
        val rrs_noBuild = (rrs).clone()
        val rrs_preBuild = (rrs).clone()

        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs_noBuild.nonSkipTerminals), rrs_noBuild)
        val sentences = listOf("abc", "abd")
        for (sen in sentences) {
            val result = parser.parseForGoal("S", sen)
            if (result.issues.isNotEmpty()) result.issues.forEach { println(it) }
        }
        val automaton_noBuild = rrs_noBuild.usedAutomatonFor("S")as ParserStateSet
        val automaton_preBuild = rrs_preBuild.buildFor("S", AutomatonKind.LOOKAHEAD_1)

        println("--Pre Build--")
        println(rrs_preBuild.usedAutomatonToString("S"))
        println("--No Build--")
        println(rrs_noBuild.usedAutomatonToString("S"))

        AutomatonTest.assertMatches(
            automaton_preBuild, automaton_noBuild, AutomatonTest.MatchConfiguration(
                in_actual_substitue_lookahead_RT_with = setOf(EOT)
            )
        )
    }
}