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
import kotlin.test.assertNotNull

internal class test_AhoSetiUlman_Ex_4_7_5 : test_AutomatonAbstract() {

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
    private val G = s0.runtimeRules.first()

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
            val s0 = state(RP(G, oN, SOR))      // G = . S
            val s1 = state(RP(d, oN, EOR))      // d
            val s2 = state(RP(b, oN, EOR))      // b
            val s3 = state(RP(rA, oN, EOR))     // A = d .
            val s4 = state(RP(rB, oN, EOR))     // B = d .
            val s5 = state(RP(S1, oN, 1))  // S1 = A . a
            val s6 = state(RP(a, oN, EOR))
            val s7 = state(RP(S1, oN, EOR))
            val s8 = state(RP(S, oN, EOR))
            val s9 = state(RP(G, oN, ER))
            val s10 = state(RP(S3, oN, 1))
            val s11 = state(RP(c, oN, ER))
            val s12 = state(RP(S3, oN, ER))
            val s13 = state(RP(S, o2, ER))
            val s14 = state(RP(S2, oN, 1), RP(S4, oN, 1))
            val s15 = state(RP(S2, oN, 2))
            val s16 = state(RP(S2, oN, ER))
            val s17 = state(RP(S, o1, ER))
            val s18 = state(RP(S4, oN, 2))
            val s19 = state(RP(S4, oN, ER))
            val s20 = state(RP(S, o3, ER))

            transition(s0, s0, s1, WIDTH, setOf(a, c), setOf(setOf(EOT)), setOf())
            transition(s0, s0, s2, WIDTH, setOf(d), setOf(setOf(EOT)), setOf())

            transition(s0, s1, s3, HEIGHT, setOf(a), setOf(setOf(a)), setOf(RP(rA, oN, 0)))
            transition(s0, s1, s4, HEIGHT, setOf(c), setOf(setOf(c)), setOf(RP(rB, oN, 0)))
            transition(s14, s1, s3, HEIGHT, setOf(c), setOf(setOf(c)), setOf(RP(rA, oN, 0)))
            transition(s14, s1, s4, HEIGHT, setOf(a), setOf(setOf(a)), setOf(RP(rB, oN, 0)))

            transition(s0, s2, s4, HEIGHT, setOf(a, c), setOf(setOf(EOT)), setOf())
            transition(s0, s0, s1, HEIGHT, setOf(a, c), setOf(setOf(EOT)), setOf())
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

        AutomatonTest.assertEquals(automaton_preBuild, automaton_noBuild)
    }

}