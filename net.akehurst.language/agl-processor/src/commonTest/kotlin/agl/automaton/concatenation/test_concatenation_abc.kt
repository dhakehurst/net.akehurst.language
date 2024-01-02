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
import net.akehurst.language.agl.parser.LeftCornerParser
import net.akehurst.language.agl.regex.RegexEnginePlatform
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.agl.scanner.ScannerOnDemand
import net.akehurst.language.api.processor.AutomatonKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class test_concatenation_abc : test_AutomatonAbstract() {
    // S =  'a' 'b' 'c' ;

    // must be fresh per test or automaton is not correct for different parses (due to caching)
    private val rrs = runtimeRuleSet {
        concatenation("S") { literal("a"); literal("b"); literal("c") }
    }

    private val S = rrs.findRuntimeRule("S")
    private val SM = rrs.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
    private val G = SM.startState.runtimeRules.first()

    private val a = rrs.findRuntimeRule("'a'")
    private val b = rrs.findRuntimeRule("'b'")
    private val c = rrs.findRuntimeRule("'c'")

    @Test
    fun parse_abc() {
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        val result = parser.parseForGoal("S", "abc")
        println(rrs.usedAutomatonToString("S"))
        assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)

        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(RP(G, o0, SR))
            state(RP(a, o0, EOR))
            state(RP(S, o0, p1))
            state(RP(b, o0, EOR))
            state(RP(S, o0, p2))
            state(RP(c, o0, EOR))
            state(RP(S, o0, EOR))
            state(RP(G, o0, EOR))

            trans(WIDTH) { src(G, o0, SOR); tgt(a); lhg(b); ctx(G, o0, SOR) }
            trans(WIDTH) { src(S, o0, p1); tgt(b); lhg(c); ctx(G, o0, SOR) }
            trans(WIDTH) { src(S, o0, p2); tgt(c); lhg(RT); ctx(G, o0, SOR) }
            trans(GOAL) { src(S); tgt(G); lhg(EOT); ctx(G, o0, SOR) }
            trans(GRAFT) { src(c); tgt(S); lhg(RT); gpg(S, o0, p2); ctx(S, o0, p2) }
            trans(HEIGHT) { src(a); tgt(S, o0, p1); lhg(setOf(b), setOf(EOT)); ctx(G, o0, SOR) }
            trans(GRAFT) { src(b); tgt(S, o0, p2); lhg(c); gpg(S, o0, p1); ctx(S, o0, p1) }
        }
        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun buildFor() {
        val actual = rrs.buildFor("S", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))

        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        val result = parser.parseForGoal("S", "abc")
        assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(RP(G, 0, SOR))
            state(RP(S, 0, 1))
            state(RP(S, 0, 2))
            state(RP(G, 0, EOR))
            state(RP(S, 0, EOR))
            state(RP(a, 0, EOR))
            state(RP(b, 0, EOR))
            state(RP(c, 0, EOR))

            trans(WIDTH) { ctx(G, o0, SOR); src(G, o0, SOR); tgt(a); lhg(b) }
            trans(WIDTH) { ctx(G, o0, SOR); src(S, o0, p1); tgt(b); lhg(c) }
            trans(WIDTH) { ctx(G, o0, SOR); src(S, o0, p2); tgt(c); lhg(EOT) }
            trans(GOAL) { ctx(G, o0, SOR); src(S); tgt(G); lhg(EOT) }
            trans(GRAFT) { ctx(S, o0, p2); src(c); tgt(S); lhg(EOT); gpg(S, o0, p2) }
            trans(HEIGHT) { ctx(G, o0, SOR); src(a); tgt(S, o0, p1); lhg(setOf(b), setOf(EOT)) }
            trans(GRAFT) { ctx(S, o0, p1); src(b); tgt(S, o0, p2); lhg(c); gpg(S, o0, p1) }
        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun compare() {
        val rrs_noBuild = runtimeRuleSet {
            concatenation("S") { literal("a");literal("b");literal("c") }
        }

        val rrs_preBuild = runtimeRuleSet {
            concatenation("S") { literal("a");literal("b");literal("c") }
        }

        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs_noBuild.nonSkipTerminals), rrs_noBuild)
        val sentences = listOf("abc")
        for (sen in sentences) {
            val result = parser.parseForGoal("S", sen)
            //val result = parser.parseForGoal("S", "", AutomatonKind.LOOKAHEAD_1)
            if (result.issues.isNotEmpty()) result.issues.forEach { println(it) }
        }
        val automaton_noBuild = rrs_noBuild.usedAutomatonFor("S")
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