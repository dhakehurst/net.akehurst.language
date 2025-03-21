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
import testFixture.utils.AutomatonTest
import net.akehurst.language.automaton.leftcorner.automaton
import net.akehurst.language.automaton.leftcorner.test_AutomatonAbstract
import net.akehurst.language.parser.leftcorner.LeftCornerParser
import net.akehurst.language.regex.agl.RegexEnginePlatform
import net.akehurst.language.scanner.common.ScannerOnDemand
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
            val s0 = state(RP(G, oN, SOR))     /* G = . S   */
            val s1 = state(RP(a, oN, EOR))     /* a .       */
            val s2 = state(RP(P, oN, EOR))     /* P = a . */
            val s3 = state(RP(E, oN, EOR))     /* E = P . */
            val s4 = state(RP(S, oN, EOR))     /* S = E . */
            val s5 = state(RP(E1, oN, 1)) /* E1 = E . o P */
            val s6 = state(RP(o, oN, EOR))     /* o . */
            val s7 = state(RP(E1, oN, 2)) /* E1 = E o . P */
            val s8 = state(RP(E1, oN, EOR))    /* E1 = E o P . */
            val s9 = state(RP(E, o1, EOR))     /* E = E1 . */
            val s10 = state(RP(G, oN, EOR))      /* G = S . */

            transition(s0, s0, s1, WIDTH, setOf(EOT, o), setOf(), null)
            transition(setOf(s0, s7), s1, s2, HEIGHT, setOf(EOT), setOf(setOf(EOT)), setOf(RP(P, oN, SOR)))
            //transition(s0, s2, s3, WIDTH, setOf(c, d), setOf(), null)
            //transition(s2, s3, s4, GRAFT, setOf(c, d), setOf(EOT), listOf(RP(ABC, oN, 1), RP(ABD, oN, 1)))
            //transition(s0, s4, s5, WIDTH, setOf(EOT), setOf(), null)
            //transition(s0, s4, s6, WIDTH, setOf(EOT), setOf(), null)
            //transition(s4, s5, s7, GRAFT, setOf(EOT), setOf(EOT), listOf(RP(ABC, oN, 2)))
            // transition(s0, s7, s8, HEIGHT, setOf(EOT), setOf(EOT), listOf(RP(S, oN, 0)))
            // transition(s0, s8, s9, GRAFT, setOf(EOT), setOf(EOT), listOf(RP(G, oN, 0)))
            // transition(null, s9, s9, GOAL, setOf(), setOf(), null)
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

        AutomatonTest.assertEquals(automaton_preBuild, automaton_noBuild)
    }
}