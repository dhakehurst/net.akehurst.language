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

package net.akehurst.language.automaton.leftcorner.infixExpressions

import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.automaton.api.AutomatonKind
import net.akehurst.language.automaton.leftcorner.AutomatonTest
import net.akehurst.language.automaton.leftcorner.automaton
import net.akehurst.language.automaton.leftcorner.test_AutomatonAbstract
import net.akehurst.language.parser.leftcorner.LeftCornerParser
import net.akehurst.language.regex.agl.RegexEnginePlatform
import net.akehurst.language.scanner.common.ScannerOnDemand
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class test_bodmas_expreOpRules_root_choiceEqual : test_AutomatonAbstract() {

    // S =  E ;
    // E = R | M | A ;
    // R = 'v'           root = var
    // M = E 'm' E ;     mul = expr * expr
    // A = E 'a' E ;     add = expr + expr

    // must be fresh per test or automaton is not correct for different parses (due to caching)
    private val rrs = runtimeRuleSet {
        concatenation("S") { ref("E") }
        choice("E", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
            ref("R")
            ref("M")
            ref("A")
        }
        concatenation("R") { literal("v") }
        concatenation("M") { ref("E"); literal("m"); ref("E") }
        concatenation("A") { ref("E"); literal("a"); ref("E") }
    }

    private val S = rrs.findRuntimeRule("S")
    private val SM = rrs.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
    private val G = SM.startState.runtimeRules.first()
    private val E = rrs.findRuntimeRule("E")
    private val R = rrs.findRuntimeRule("R")
    private val rM = rrs.findRuntimeRule("M")
    private val rA = rrs.findRuntimeRule("A")
    private val m = rrs.findRuntimeRule("'m'")
    private val a = rrs.findRuntimeRule("'a'")
    private val v = rrs.findRuntimeRule("'v'")

    @Test
    fun automaton_parse_v() {
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        val result = parser.parseForGoal("S", "v")
        assertNotNull(result.sppt)
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))
        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(G, oN, SR)  // G = . S
            state(v, oN, ER)  // v .
            state(R, oN, ER)  // R = 'v' .
            state(E, oN, ER)  // E = R .
            state(S, oN, ER)  // S = E .
            state(rM, oN, p1)  // M = E . 'm' E
            state(rA, oN, p1)  // E . 'a' E
            state(G, oN, ER)  // G = S .

            trans(WIDTH) { src(G, oN, SR); tgt(v); lhg(setOf(EOT, m, a)); ctx(G, oN, SR) }
            trans(GOAL) { src(S); tgt(G); lhg(EOT); ctx(G, oN, SR) }
            trans(HEIGHT) { src(E); tgt(rA, oN, p1); lhg(setOf(a), setOf(EOT, m, a)); ctx(G, oN, SR) }
            trans(HEIGHT) { src(R); tgt(E); lhg(setOf(EOT, m, a), setOf(EOT, m, a)); ctx(G, oN, SR) }
            trans(HEIGHT) { src(E); tgt(rM, oN, p1); lhg(setOf(m), setOf(EOT, m, a)); ctx(G, oN, SR) }
            trans(HEIGHT) { src(v); tgt(R); lhg(setOf(EOT, m, a), setOf(EOT, m, a)); ctx(G, oN, SR) }
            trans(HEIGHT) { src(E); tgt(S); lhg(setOf(EOT), setOf(EOT)); ctx(G, oN, SR) }
        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun automaton_parse_vmv() {
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        val result = parser.parseForGoal("S", "vmv")
        println(rrs.usedAutomatonToString("S"))
        assertNotNull(result.sppt)
        assertEquals(0, result.issues.size)
        assertEquals(1, result.sppt!!.maxNumHeads)
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            state(G, oN, SR)  // G = . S
            state(v)  // v .
            state(R)  // R = 'v' .
            state(E, oN, ER)  // E = R .
            state(S)  // S = E .
            state(rM, oN, p1)  // M = E . 'm' E
            state(rA, oN, p1)  // E . 'a' E
            state(m)
            state(rM, oN, p2)
            state(rM)
            state(E, o1, ER)
            state(G, oN, ER)  // G = S .

            trans(WIDTH) { src(rM, oN, p1); tgt(m); lhg(v); ctx(G, oN, SR) }
            trans(WIDTH) { src(G, oN, SR); tgt(v); lhg(setOf(EOT, m, a)); ctx(G, oN, SR) }
            trans(WIDTH) { src(rM, oN, p2); tgt(v); lhg(setOf(RT, m, a)); ctx(G, oN, SR) }
            trans(GOAL) { src(S); tgt(G); lhg(EOT); ctx(G, oN, SR) }

            trans(HEIGHT) { src(E, oN, ER); tgt(rA, oN, p1); lhg(setOf(a), setOf(RT, EOT, m, a)); ctx(G, oN, SR) }
            trans(HEIGHT) { src(R); tgt(E); lhg(setOf(EOT, m, a), setOf(EOT, m, a)); ctx(G, oN, SR) }
            trans(HEIGHT) { src(E); tgt(rM, oN, p1); lhg(setOf(m), setOf(EOT, m, a)); ctx(G, oN, SR) }
            trans(HEIGHT) { src(v); tgt(R); lhg(setOf(EOT, m, a), setOf(EOT, m, a)); ctx(G, oN, SR) }
            trans(HEIGHT) { src(E); tgt(S); lhg(setOf(EOT), setOf(EOT)); ctx(G, oN, SR) }
            trans(HEIGHT) { src(E); tgt(S); lhg(setOf(EOT), setOf(EOT)); ctx(G, oN, SR) }
            trans(GRAFT) { src(E); tgt(S); lhg(setOf(EOT), setOf(EOT)); ctx(G, oN, SR) }
            trans(GRAFT) { src(E); tgt(S); lhg(setOf(EOT), setOf(EOT)); ctx(G, oN, SR) }
            trans(HEIGHT) { src(v); tgt(R); lhg(setOf(EOT, m, a), setOf(EOT, m, a)); ctx(G, oN, SR) }
            trans(HEIGHT) { src(E); tgt(S); lhg(setOf(EOT), setOf(EOT)); ctx(G, oN, SR) }
            trans(HEIGHT) { src(E); tgt(S); lhg(setOf(EOT), setOf(EOT)); ctx(G, oN, SR) }
        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun automaton_parse_sentences() {
        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
        val sentences = listOf("v", "vav", "vavav", "vavav", "vmv", "vmvmv", "vmvav", "vavmv")
        //val sentences = listOf( "vav")
        sentences.forEach {
            println(it)
            val result = parser.parseForGoal("S", it)
            assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
            assertEquals(0, result.issues.size)
            assertEquals(1, result.sppt!!.maxNumHeads)
        }
        println(rrs.usedAutomatonToString("S"))
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            val s0 = state(RP(G, oN, SOR))    // G = . S
            val s1 = state(RP(v, oN, EOR))    // v
            val s2 = state(RP(R, oN, EOR))    // R = v .
            val s3 = state(RP(E, oN, EOR))    // E = R .
            val s4 = state(RP(S, oN, EOR))    // S = E .
            val s5 = state(RP(rA, oN, p1))    // A = E . a E
            val s6 = state(RP(rM, oN, p1))    // M = E . m E
            val s7 = state(RP(G, oN, ER))    // G = S .
            val s8 = state(RP(a, oN, ER))    // a
            val s9 = state(RP(rA, oN, p2))    // A = E a . E
            val s10 = state(RP(rA, oN, ER))  // A = E a E .
            val s11 = state(RP(E, o2, ER))   // E = M .
            val s12 = state(RP(m, oN, ER))   // m
            val s13 = state(RP(rM, oN, p2))   // M = E m . E
            val s14 = state(RP(rM, oN, ER))  // M = E m E .
            val s15 = state(RP(E, o1, ER))   // E = A .

            // because GRAFT is done before HEIGHT, RP(A,0,2) never becomes context for this trans, even though prebuild allows for it
            trans(WIDTH) { ctx(RP(G, oN, SOR), RP(rM, oN, p2)); src(rA, oN, p1); tgt(a); lhg(v) }
            // because GRAFT is done before HEIGHT, RP(M,0,2) never becomes context for this trans, even though prebuild allows for it
            trans(WIDTH) { ctx(RP(G, oN, SOR), RP(rA, oN, p2)); src(rM, oN, p1); tgt(m); lhg(v) }
            trans(WIDTH) { ctx(G, oN, SOR); src(G, oN, SOR); tgt(v); lhg(setOf(EOT, m, a)) }
            trans(WIDTH) { ctx(RP(G, oN, SOR), RP(rM, oN, p2)); src(rA, oN, p2); tgt(v); lhg(setOf(EOT, a, m)) }
            trans(WIDTH) { ctx(RP(G, oN, SOR), RP(rA, oN, p2)); src(rM, oN, p2); tgt(v); lhg(setOf(EOT, a, m)) }
            trans(GOAL) { ctx(RP(G, oN, SOR)); src(S); tgt(G); lhg(EOT); }
            trans(GRAFT) { ctx(rA, oN, p2); src(E, oN, ER); tgt(rA); lhg(setOf(EOT)); gpg(rA, oN, p2) } // lhg == [EOT,a,m] for prebuild?
            trans(GRAFT) { ctx(rA, oN, p2); src(E, o1, ER); tgt(rA); lhg(setOf(EOT)); gpg(rA, oN, p2) } // lhg == [EOT,a,m] for prebuild?
            //transition(GRAFT) { ctx(rA, o0, p2); src(E,o2,EOR); tgt(rA); lhg(setOf(EOT)); rtg(rA, o0, p2) } //never gets created
            transition(setOf(s0, s9, s13), s3, s5, HEIGHT, null) { lhg(setOf(a), setOf(m)); lhg(setOf(a), setOf(a));lhg(setOf(a), setOf(EOT)) }
            transition(setOf(s0, s9), s15, s5, HEIGHT, null) { lhg(setOf(a), setOf(m)); lhg(setOf(a), setOf(a));lhg(setOf(a), setOf(EOT)) }
            transition(setOf(s0, s13), s11, s5, HEIGHT, null) { lhg(setOf(a), setOf(m)); lhg(setOf(a), setOf(a));lhg(setOf(a), setOf(EOT)) }
            transition(s5, s8, s9, GRAFT, setOf(RP(rA, oN, 1))) { lhg(setOf(v)) }
            trans(GRAFT) { ctx(rA, oN, p1); src(a); tgt(rA, oN, p2); lhg(v); gpg(rA, oN, p1) }
            trans(HEIGHT) { ctx(RP(G, oN, SOR), RP(rA, oN, p2), RP(rM, oN, p2)); src(R); tgt(E); lhg(m, m); lhg(EOT, EOT); lhg(a, a) }
            trans(HEIGHT) { ctx(RP(G, oN, SOR), RP(rA, oN, p2)); src(rM); tgt(E, o1, ER); lhg(m, m); lhg(EOT, EOT); lhg(a, a) }
            trans(HEIGHT) { ctx(RP(G, oN, SOR), RP(rM, oN, p2)); src(rA); tgt(E, o2, ER); lhg(m, m); lhg(EOT, EOT); lhg(a, a) }
            trans(GRAFT) { ctx(rM, oN, p2); src(E, oN, ER); tgt(rM); lhg(EOT); gpg(rM, oN, p2) }
            trans(GRAFT) { ctx(rM, oN, p2); src(E, o2, ER); tgt(rM); lhg(EOT); gpg(rM, oN, p2) }
            trans(HEIGHT) { ctx(RP(G, oN, SOR), RP(rA, oN, p2), RP(rM, oN, p2));src(E); tgt(rM, oN, p1); lhg(m, m); lhg(m, a); lhg(m, EOT) }
            trans(HEIGHT) { ctx(RP(G, oN, SOR), RP(rA, oN, p2)); src(E, o1, ER); tgt(rM, oN, p1); lhg(m, m); lhg(m, a); lhg(m, EOT) }
            trans(HEIGHT) { ctx(RP(G, oN, SOR), RP(rM, oN, p2)); src(E, o2, ER); tgt(rM, oN, p1); lhg(m, m); lhg(m, a); lhg(m, EOT) }
            trans(GRAFT) { ctx(rM, oN, p1); src(m); tgt(rM, oN, p2); lhg(v); gpg(rM, oN, p1) }
            trans(HEIGHT) { ctx(RP(G, oN, SOR), RP(rA, oN, p2), RP(rM, oN, p2)); src(v); tgt(R); lhg(m, m); lhg(EOT, EOT); lhg(a, a) }
            trans(HEIGHT) { ctx(G, oN, SOR); src(E, oN, ER); tgt(S); lhg(EOT, EOT) }
            trans(HEIGHT) { ctx(G, oN, SOR); src(E, o1, ER); tgt(S); lhg(EOT, EOT) }
            trans(HEIGHT) { ctx(G, oN, SOR); src(E, o2, ER); tgt(S); lhg(EOT, EOT) }
        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun buildFor() {
        val actual = rrs.buildFor("S", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))

        val sentences = listOf("v", "vav", "vavav", "vmv", "vmvmv", "vmvav", "vavmv")
        sentences.forEach {
            val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs.nonSkipTerminals), rrs)
            val result = parser.parseForGoal("S", it)
            assertNotNull(result.sppt, result.issues.joinToString("\n") { it.toString() })
            assertEquals(0, result.issues.size)
            assertEquals(1, result.sppt!!.maxNumHeads)
        }

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
            val s0 = state(RP(G, oN, SOR))    // G = . S
            val s1 = state(RP(G, oN, ER))    // G = S .
            val s2 = state(RP(S, oN, ER))    // S = E .
            val s3 = state(RP(E, oN, ER))    // E = R .
            val s4 = state(RP(E, o1, ER))    // E = M .
            val s5 = state(RP(E, o2, ER))    // E = A .
            val s6 = state(RP(rA, oN, ER))   // A = E a E .
            val s7 = state(RP(a, oN, ER))    // v
            val s8 = state(RP(rM, oN, ER))   // M = E m E .
            val s9 = state(RP(m, oN, ER))    // m
            val s10 = state(RP(R, oN, ER))   // R = v .
            val s11 = state(RP(v, oN, ER))   // v
            val s12 = state(RP(rA, oN, p1))   // A = E . a E
            val s13 = state(RP(rA, oN, p2))   // A = E a . E
            val s14 = state(RP(rM, oN, p1))   // M = E . m E
            val s15 = state(RP(rM, oN, p2))   // M = E m . E

            trans(WIDTH) { ctx(RP(G, oN, SOR), RP(rA, oN, p2), RP(rM, oN, p2)); src(rA, oN, p1); tgt(a); lhg(v) }
            trans(WIDTH) { ctx(RP(G, oN, SOR), RP(rA, oN, p2), RP(rM, oN, p2)); src(rM, oN, p1); tgt(m); lhg(v) }
            trans(WIDTH) { ctx(G, oN, SOR); src(G, oN, SOR); tgt(v); lhg(setOf(EOT, m, a)) }
            trans(WIDTH) { ctx(RP(G, oN, SOR), RP(rA, oN, p2), RP(rM, oN, p2)); src(rA, oN, p2); tgt(v); lhg(setOf(EOT, a, m)) }
            trans(WIDTH) { ctx(RP(G, oN, SOR), RP(rA, oN, p2), RP(rM, oN, p2)); src(rM, oN, p2); tgt(v); lhg(setOf(EOT, a, m)) }
            trans(GOAL) { ctx(RP(G, oN, SOR)); src(S); tgt(G); lhg(EOT) }
            trans(GRAFT) { ctx(rA, oN, p2); src(E, oN, ER); tgt(rA); lhg(setOf(EOT, a, m)); gpg(rA, oN, p2) }
            trans(GRAFT) { ctx(rA, oN, p2); src(E, o1, ER); tgt(rA); lhg(setOf(EOT, a, m)); gpg(rA, oN, p2) }
            trans(GRAFT) { ctx(rA, oN, p2); src(E, o2, ER); tgt(rA); lhg(setOf(EOT, a, m)); gpg(rA, oN, p2) }  //never gets created in on-demand-build !
            trans(HEIGHT) { ctx(RP(G, oN, SOR), RP(rA, oN, p2), RP(rM, oN, p2)); src(E, oN, ER); tgt(rA, oN, p1); lhg(setOf(a), setOf(EOT, a, m)) }
            trans(HEIGHT) { ctx(RP(G, oN, SOR), RP(rA, oN, p2), RP(rM, oN, p2)); src(E, o1, ER); tgt(rA, oN, p1); lhg(setOf(a), setOf(EOT, a, m)) }
            trans(HEIGHT) { ctx(RP(G, oN, SOR), RP(rA, oN, p2), RP(rM, oN, p2)); src(E, o2, ER); tgt(rA, oN, p1); lhg(setOf(a), setOf(EOT, a, m)) }
            trans(GRAFT) { ctx(rA, oN, p1); src(a); tgt(rA, oN, p2); lhg(v); gpg(rA, oN, p1) }
            trans(HEIGHT) { ctx(RP(G, oN, SOR), RP(rA, oN, p2), RP(rM, oN, p2)); src(R); tgt(E, oN, ER); lhg(EOT, EOT); lhg(m, m); lhg(a, a) }
            trans(HEIGHT) { ctx(RP(G, oN, SOR), RP(rA, oN, p2), RP(rM, oN, p2)); src(rM); tgt(E, o1, ER); lhg(EOT, EOT); lhg(m, m); lhg(a, a) }
            trans(HEIGHT) { ctx(RP(G, oN, SOR), RP(rA, oN, p2), RP(rM, oN, p2)); src(rA); tgt(E, o2, ER); lhg(EOT, EOT); lhg(m, m); lhg(a, a) }
            trans(GRAFT) { ctx(rM, oN, p2); src(E, oN, ER); tgt(rM); lhg(setOf(EOT, a, m)); gpg(rM, oN, p2) }
            trans(GRAFT) { ctx(rM, oN, p2); src(E, o1, ER); tgt(rM); lhg(setOf(EOT, a, m)); gpg(rM, oN, p2) }
            trans(GRAFT) { ctx(rM, oN, p2); src(E, o2, ER); tgt(rM); lhg(setOf(EOT, a, m)); gpg(rM, oN, p2) }
            trans(HEIGHT) { ctx(RP(G, oN, SOR), RP(rA, oN, p2), RP(rM, oN, p2)); src(E, oN, ER); tgt(rM, oN, p1); lhg(setOf(m), setOf(EOT, a, m)) }
            trans(HEIGHT) { ctx(RP(G, oN, SOR), RP(rA, oN, p2), RP(rM, oN, p2)); src(E, o1, ER); tgt(rM, oN, p1); lhg(setOf(m), setOf(EOT, a, m)) }
            trans(HEIGHT) { ctx(RP(G, oN, SOR), RP(rA, oN, p2), RP(rM, oN, p2)); src(E, o2, ER); tgt(rM, oN, p1); lhg(setOf(m), setOf(EOT, a, m)) }
            trans(GRAFT) { ctx(rM, oN, p1); src(m); tgt(rM, oN, p2); lhg(v); gpg(rM, oN, p1) }
            trans(HEIGHT) { ctx(RP(G, oN, SOR), RP(rA, oN, p2), RP(rM, oN, p2)); src(v); tgt(R); lhg(EOT, EOT); lhg(m, m); lhg(a, a) }
            trans(HEIGHT) { ctx(G, oN, SOR); src(E, oN, ER); tgt(S); lhg(EOT, EOT) }
            trans(HEIGHT) { ctx(G, oN, SOR); src(E, o1, ER); tgt(S); lhg(EOT, EOT) }
            trans(HEIGHT) { ctx(G, oN, SOR); src(E, o2, ER); tgt(S); lhg(EOT, EOT) }
        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun compare() {
        val rrs_noBuild = rrs.clone()
        val rrs_preBuild = rrs.clone()

        val parser = LeftCornerParser(ScannerOnDemand(RegexEnginePlatform, rrs_noBuild.nonSkipTerminals), rrs_noBuild)
        val sentences = listOf("v", "vav", "vavav", "vmv", "vmvmv", "vmvav", "vavmv")
        for (sen in sentences) {
            val result = parser.parseForGoal("S", sen)
            if (result.issues.isNotEmpty()) {
                println("Sentence: $sen")
                result.issues.forEach { println(it) }
            }
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