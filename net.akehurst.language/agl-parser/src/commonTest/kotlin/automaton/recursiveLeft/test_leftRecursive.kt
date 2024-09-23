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

internal class test_leftRecursive : test_AutomatonAbstract() {

    // S =  'a' | S1
    // S1 = S 'a'

    // must be fresh per test or automaton is not correct for different parses (due to caching)
    private val rrs = runtimeRuleSet {
        choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
            literal("a")
            ref("S1")
        }
        concatenation("S1") { ref("S"); literal("a") }
    }

    private val S = rrs.findRuntimeRule("S")
    private val G = rrs.goalRuleFor[S]
    private val S1 = rrs.findRuntimeRule("S1")
    private val a = rrs.findRuntimeRule("'a'")

    @Test
    fun parse_a() {
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        val result = parser.parseForGoal("S", "a")
        println(rrs.usedAutomatonToString("S"))
        assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(RP(G, o0, SR))     /* {}     G = . S    */
            state(RP(G, o0, EOR))     /* {}     G = S .    */
            state(RP(S, o0, EOR))     /* {0}    S = a .    */
            state(RP(a, o0, EOR))     /* {0,5}  a .        */
            state(RP(S1, o0, p1))     /* {0}    S1 = S . a */

            trans(WIDTH) { src(G, o0, SOR); tgt(a); lhg(setOf(EOT, a)); ctx(G, o0, SOR) }
            trans(GOAL) { src(S); tgt(G); lhg(setOf(EOT)); ctx(G, o0, SOR) }
            trans(HEIGHT) { src(a); tgt(S); lhg(setOf(EOT, a), setOf(EOT, a)); ctx(G, o0, SOR) }
            trans(HEIGHT) { src(S); tgt(S1, o0, p1); lhg(setOf(a), setOf(EOT, a)); ctx(G, o0, SOR) }
        }
        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun parse_aa() {
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        val result = parser.parseForGoal("S", "aa")
        println(rrs.usedAutomatonToString("S"))
        assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(RP(G, o0, SOR))     /* {}     G = . S    */
            state(RP(G, o0, EOR))     /* {}     G = S .    */
            state(RP(S, o0, EOR))     /* {0}    S = a .    */
            state(RP(S, o1, EOR))     /* {0}    S = S1 .   */
            state(RP(a, o0, EOR))     /* {0,5}  a .        */
            state(RP(S1, o0, p1))     /* {0}    S1 = S . a */
            state(RP(S1, o0, EOR))    /* {0}    S1 = S a . */

            trans(WIDTH) { ctx(G, o0, SOR); src(G, o0, SOR); tgt(a); lhg(setOf(EOT, a)) }
            trans(WIDTH) { ctx(G, o0, SOR); src(S1, o0, p1); tgt(a); lhg(setOf(RT)) }
            trans(GOAL) { ctx(G, o0, SOR); src(S); tgt(G); lhg(setOf(EOT)) }
            trans(GOAL) { ctx(G, o0, SOR); src(S, o1, EOR); tgt(G); lhg(setOf(EOT)) }
            trans(HEIGHT) { ctx(G, o0, SOR); src(a); tgt(S); lhg(setOf(EOT, a), setOf(EOT, a)) }
            trans(HEIGHT) { ctx(G, o0, SOR); src(S1); tgt(S, o1, ER); lhg(setOf(RT, a), setOf(RT, a)) }
            trans(GRAFT) { ctx(S1, o0, p1); src(a); tgt(S1); lhg(setOf(RT)) }
            trans(HEIGHT) { ctx(G, o0, SOR); src(S); tgt(S1, o0, p1); lhg(setOf(a), setOf(EOT, a)) }
            trans(HEIGHT) { ctx(G, o0, SOR); src(S, o1, ER); tgt(S1, o0, p1); lhg(setOf(a), setOf(RT, a)) }
        }
        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun parse_aaa() {
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        val result = parser.parseForGoal("S", "aaa")
        println(rrs.usedAutomatonToString("S"))
        assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)

        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(RP(G, o0, SOR))     /* {}     G = . S    */
            state(RP(G, o0, ER))     /* {}     G = S .    */
            state(RP(S, o0, ER))     /* {0}    S = a .    */
            state(RP(S, o1, ER))     /* {0}    S = S1 .   */
            state(RP(a, o0, ER))     /* {0,5}  a .        */
            state(RP(S1, o0, p1))     /* {0}    S1 = S . a */
            state(RP(S1, o0, ER))    /* {0}    S1 = S a . */

            trans(WIDTH) { ctx(G, o0, SOR); src(G, o0, SOR); tgt(a); lhg(setOf(EOT, a)) }
            trans(WIDTH) { ctx(G, o0, SOR); src(S1, o0, p1); tgt(a); lhg(setOf(RT)) }
            trans(GOAL) { ctx(G, o0, SOR); src(S); tgt(G); lhg(setOf(EOT)) }
            trans(GOAL) { ctx(G, o0, SOR); src(S, o1, ER); tgt(G); lhg(setOf(EOT)) }
            trans(HEIGHT) { ctx(G, o0, SOR); src(a); tgt(S); lhg(setOf(EOT, a), setOf(EOT, a)) }
            trans(HEIGHT) { ctx(G, o0, SOR); src(S1); tgt(S, o1, ER); lhg(setOf(RT, a), setOf(RT, a)) }
            trans(GRAFT) { ctx(S1, o0, p1); src(a); tgt(S1); lhg(setOf(RT)) }
            trans(HEIGHT) { ctx(G, o0, SOR); src(S); tgt(S1, o0, p1); lhg(setOf(a), setOf(EOT, a)) }
            trans(HEIGHT) { ctx(G, o0, SOR); src(S, o1, ER); tgt(S1, o0, p1); lhg(setOf(a), setOf(RT, a)) }
        }
        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun buildFor() {
        val actual = rrs.buildFor("S", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(RP(G, o0, SOR))     /* {}     G = . S    */
            state(RP(G, o0, ER))     /* {}     G = S .    */
            state(RP(S, o0, ER))     /* {0}    S = a .    */
            state(RP(S, o1, ER))     /* {0}    S = S1 .   */
            state(RP(a, o0, ER))     /* {0,5}  a .        */
            state(RP(S1, o0, p1))     /* {0}    S1 = S . a */
            state(RP(S1, o0, ER))    /* {0}    S1 = S a . */

            trans(WIDTH) { ctx(G, o0, SOR); src(G, o0, SOR); tgt(a); lhg(setOf(EOT, a)) }
            trans(WIDTH) { ctx(G, o0, SOR); src(S1, o0, p1); tgt(a); lhg(setOf(EOT, a)) }
            trans(GOAL) { ctx(G, o0, SOR); src(S); tgt(G); lhg(setOf(EOT)) }
            trans(GOAL) { ctx(G, o0, SOR); src(S, o1, ER); tgt(G); lhg(setOf(EOT)) }
            trans(HEIGHT) { ctx(G, o0, SOR); src(a); tgt(S); lhg(setOf(EOT, a), setOf(EOT, a)) }
            trans(HEIGHT) { ctx(G, o0, SOR); src(S1); tgt(S, o1, ER); lhg(setOf(EOT, a), setOf(EOT, a)) }
            trans(GRAFT) { ctx(S1, o0, p1); src(a); tgt(S1); lhg(setOf(EOT, a)) }
            trans(HEIGHT) { ctx(G, o0, SOR); src(S); tgt(S1, o0, p1); lhg(setOf(a), setOf(EOT, a)) }
            trans(HEIGHT) { ctx(G, o0, SOR); src(S, o1, ER); tgt(S1, o0, p1); lhg(setOf(a), setOf(EOT, a)) }
        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun compare() {
        val rrs_noBuild = rrs.clone()
        val rrs_preBuild = rrs.clone()

        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs_noBuild.nonSkipTerminals), rrs_noBuild)
        val sentences = listOf("a", "aa", "aaa", "aaaa")
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
                no_lookahead_compare = true
            )
        )
    }
}