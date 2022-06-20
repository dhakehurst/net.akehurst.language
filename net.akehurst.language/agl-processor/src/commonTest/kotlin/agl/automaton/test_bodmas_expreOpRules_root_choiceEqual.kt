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

package net.akehurst.language.agl.automaton

import agl.automaton.AutomatonTest
import agl.automaton.automaton
import net.akehurst.language.agl.parser.ScanOnDemandParser
import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.processor.AutomatonKind
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
        val parser = ScanOnDemandParser(rrs)
        val (sppt, issues) = parser.parseForGoal("S", "v", AutomatonKind.LOOKAHEAD_1)
        assertNotNull(sppt)
        assertEquals(0, issues.size)
        assertEquals(1, sppt.maxNumHeads)
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))
        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", 0, false) {

        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun automaton_parse_vmv() {
        val parser = ScanOnDemandParser(rrs)
        val (sppt, issues) = parser.parseForGoal("S", "vmv", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))
        assertNotNull(sppt)
        assertEquals(0, issues.size)
        assertEquals(1, sppt.maxNumHeads)
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", 0, false) {
            val s0 = state(RP(G, 0, SOR))   // G = . S
            val s1 = state(RP(v, 0, EOR))   // v
            val s2 = state(RP(R, 0, EOR))   // R = v .
            val s3 = state(RP(E, 0, EOR))   // E = R .
            val s4 = state(RP(S, 0, EOR))   // S = E .
            val s5 = state(RP(rA, 0, 1))   // M = E . m E
            val s6 = state(RP(rM, 0, 1))   // A = E . a E
            val s7 = state(RP(m, 0, EOR))   // m
            val s8 = state(RP(rM, 0, 2))   // M = E m . E
            val s9 = state(RP(rM, 0, EOR))   // M = E m E .
            val s10 = state(RP(E, 1, EOR))   // E = M .
            val s11 = state(RP(G, 0, EOR))   // G = S .

            transition(s0, s6, s7, WIDTH, null) { lhg(setOf(v)) }
            transition(s0, s0, s1, WIDTH, null) { lhg(setOf(UP, m, a)) }
            transition(s0, s8, s1, WIDTH, null) { lhg(setOf(UP, m, a)) }
            transition(s0, s4, s11, GOAL, null) { lhg(setOf(UP)) }
            transition(setOf(s0, s8), s3, s6, HEIGHT, null) { lhg(setOf(a), setOf(m)); lhg(setOf(a), setOf(a));lhg(setOf(a), setOf(UP)) }
            transition(s0, s10, s5, HEIGHT, null) { lhg(setOf(a), setOf(m)); lhg(setOf(a), setOf(a));lhg(setOf(a), setOf(UP)) }
            transition(setOf(s0, s8), s2, s3, HEIGHT, null) { lhg(setOf(m), setOf(m)); lhg(setOf(a), setOf(a));lhg(setOf(UP), setOf(UP)) }
            transition(s0, s9, s10, HEIGHT, null) { lhg(setOf(m), setOf(m)); lhg(setOf(UP), setOf(UP));lhg(setOf(a), setOf(a)) }
            transition(s8, s3, s9, GRAFT, setOf(RP(rM, 0, 2))) { lhg(setOf(UP)) }
            transition(setOf(s0, s8), s3, s6, HEIGHT, null) { lhg(setOf(m), setOf(m)); lhg(setOf(m), setOf(a));lhg(setOf(m), setOf(UP)) }
            transition(s0, s10, s6, HEIGHT, null) { lhg(setOf(m), setOf(m)); lhg(setOf(m), setOf(a));lhg(setOf(m), setOf(UP)) }
            transition(s6, s7, s8, GRAFT, setOf(RP(rM, 0, 2))) { lhg(setOf(v)) }
            transition(setOf(s0, s8), s1, s2, HEIGHT, null) { lhg(setOf(m), setOf(m)); lhg(setOf(a), setOf(a));lhg(setOf(UP), setOf(UP)) }
            transition(s0, s3, s4, HEIGHT, null) { lhg(setOf(UP), setOf(UP)) }
            transition(s0, s10, s4, HEIGHT, null) { lhg(setOf(UP), setOf(UP)) }
        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun automaton_parse_sentences() {
        val parser = ScanOnDemandParser(rrs)
        val sentences = listOf("v", "vav", "vavav", "vmv", "vmvmv", "vmvav", "vavmv")
        sentences.forEach {
            val (sppt, issues) = parser.parseForGoal("S", it, AutomatonKind.LOOKAHEAD_1)
            assertNotNull(sppt, issues.joinToString("\n") { it.toString() })
            assertEquals(0, issues.size)
            assertEquals(1, sppt.maxNumHeads)
        }
        println(rrs.usedAutomatonToString("S"))
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", 0, false) {
            val s0 = state(RP(G, 0, SOR))   // G = . S
            val s1 = state(RP(v, 0, EOR))   // v
            val s2 = state(RP(R, 0, EOR))   // R = v .
            val s3 = state(RP(E, 0, EOR))   // E = R .
            val s4 = state(RP(S, 0, EOR))   // S = E .
            val s5 = state(RP(rA, 0, 1))   // A = E . a E
            val s6 = state(RP(rM, 0, 1))   // M = E . m E
            val s7 = state(RP(G, 0, EOR))   // G = S .
            val s8 = state(RP(a, 0, EOR))   // a
            val s9 = state(RP(rA, 0, 2))   // A = E a . E
            val s10 = state(RP(rA, 0, EOR))   // A = E a E .
            val s11 = state(RP(E, 2, EOR))   // E = M .
            val s12 = state(RP(m, 0, EOR))   // m
            val s13 = state(RP(rM, 0, 2))   // M = E m . E
            val s14 = state(RP(rM, 0, EOR))   // M = E m E .
            val s15 = state(RP(E, 1, EOR))   // E = A .

            transition(WIDTH) { ctx(RP(G, 0, SOR), RP(rM, 0, 2)); src(rA, 0, 1); tgt(a, 0, EOR); lhg(setOf(v)) }
            transition(setOf(s0, s9), s6, s12, WIDTH, null) { lhg(setOf(v)) }
            transition(s0, s0, s1, WIDTH, null) { lhg(setOf(UP, m, a)) }
            transition(setOf(s0, s13), s9, s1, WIDTH, null) { lhg(setOf(UP, m, a)) }
            transition(setOf(s0, s9), s13, s1, WIDTH, null) { lhg(setOf(UP, m, a)) }
            transition(s0, s4, s7, GOAL, null) { lhg(setOf(UP)) }
            transition(s9, s3, s10, GRAFT, setOf(RP(rA, 0, 2))) { lhg(setOf(UP)) }
            transition(s9, s15, s10, GRAFT, setOf(RP(rA, 0, 2))) { lhg(setOf(UP)) }
            transition(setOf(s0, s9, s13), s3, s5, HEIGHT, null) { lhg(setOf(a), setOf(m)); lhg(setOf(a), setOf(a));lhg(setOf(a), setOf(UP)) }
            transition(setOf(s0, s9), s15, s5, HEIGHT, null) { lhg(setOf(a), setOf(m)); lhg(setOf(a), setOf(a));lhg(setOf(a), setOf(UP)) }
            transition(setOf(s0, s13), s11, s5, HEIGHT, null) { lhg(setOf(a), setOf(m)); lhg(setOf(a), setOf(a));lhg(setOf(a), setOf(UP)) }
            transition(s5, s8, s9, GRAFT, setOf(RP(rA, 0, 1))) { lhg(setOf(v)) }
            transition(GRAFT) { ctx(rA, 0, 1); src(a, 0, EOR); tgt(rA, 0, 2); lhg(v); rtg(rA, 0, 1) }
            transition(HEIGHT) { ctx(RP(),RP(),RP()); src(R,0,EOR); tgt(E,0,EOR); lhg(setOf(m), setOf(m));lhg(setOf(UP), setOf(UP));lhg(setOf(a), setOf(a)) }

            transition(setOf(s0, s8), s3, s6, HEIGHT, null) { lhg(setOf(a), setOf(m)); lhg(setOf(a), setOf(a));lhg(setOf(a), setOf(UP)) }
            transition(s0, s10, s5, HEIGHT, null) { lhg(setOf(a), setOf(m)); lhg(setOf(a), setOf(a));lhg(setOf(a), setOf(UP)) }
            transition(setOf(s0, s8), s2, s3, HEIGHT, null) { lhg(setOf(m), setOf(m)); lhg(setOf(a), setOf(a));lhg(setOf(UP), setOf(UP)) }
            transition(s0, s9, s10, HEIGHT, null) { lhg(setOf(m), setOf(m)); lhg(setOf(UP), setOf(UP));lhg(setOf(a), setOf(a)) }
            transition(s8, s3, s9, GRAFT, setOf(RP(rM, 0, 2))) { lhg(setOf(UP)) }
            transition(setOf(s0, s8), s3, s6, HEIGHT, null) { lhg(setOf(m), setOf(m)); lhg(setOf(m), setOf(a));lhg(setOf(m), setOf(UP)) }
            transition(s0, s10, s6, HEIGHT, null) { lhg(setOf(m), setOf(m)); lhg(setOf(m), setOf(a));lhg(setOf(m), setOf(UP)) }
            transition(s6, s7, s8, GRAFT, setOf(RP(rM, 0, 2))) { lhg(setOf(v)) }
            transition(setOf(s0, s8), s1, s2, HEIGHT, null) { lhg(setOf(m), setOf(m)); lhg(setOf(a), setOf(a));lhg(setOf(UP), setOf(UP)) }
            transition(s0, s3, s4, HEIGHT, null) { lhg(setOf(UP), setOf(UP)) }
            transition(s0, s10, s4, HEIGHT, null) { lhg(setOf(UP), setOf(UP)) }
        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun buildFor() {
        val actual = rrs.buildFor("S", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))

        val sentences = listOf("v", "vav", "vavav", "vmv", "vmvmv", "vmvav", "vavmv")
        sentences.forEach {
            val parser = ScanOnDemandParser(rrs)
            val (sppt, issues) = parser.parseForGoal("S", it, AutomatonKind.LOOKAHEAD_1)
            assertNotNull(sppt, issues.joinToString("\n") { it.toString() })
            assertEquals(0, issues.size)
            assertEquals(1, sppt.maxNumHeads)
        }

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", 1, false) {
            val s0 = state(RP(G, 0, SOR))   // G = . S
            val s1 = state(RP(G, 0, EOR))   // G = S .
            val s2 = state(RP(S, 0, EOR))   // S = E .
            val s3 = state(RP(E, 0, EOR))   // E = R .
            val s4 = state(RP(E, 1, EOR))   // E = M .
            val s5 = state(RP(E, 2, EOR))   // E = A .
            val s6 = state(RP(rA, 0, EOR))   // M = E m E .
            val s7 = state(RP(a, 0, EOR))   // v
            val s8 = state(RP(rM, 0, EOR))   // A = E a E .
            val s9 = state(RP(m, 0, EOR))   // m
            val s10 = state(RP(R, 0, EOR))   // R = v .
            val s11 = state(RP(v, 0, EOR))   // v
            val s12 = state(RP(rA, 0, 1))   // A = E . a E
            val s13 = state(RP(rA, 0, 2))   // A = E a . E
            val s14 = state(RP(rM, 0, 1))   // M = E . m E
            val s15 = state(RP(rM, 0, 2))   // M = E m . E

            transition(setOf(s0, s13, s15), s12, s7, WIDTH, null) { lhg(setOf(v)) }
            transition(setOf(s0, s13, s15), s14, s9, WIDTH, null) { lhg(setOf(v)) }
            transition(s0, s0, s11, WIDTH, null) { lhg(setOf(UP, m, a)) }
            transition(setOf(s0, s13, s15), s13, s11, WIDTH, null) { lhg(setOf(UP, m, a)) }
            transition(setOf(s0, s13, s15), s15, s11, WIDTH, null) { lhg(setOf(UP, m, a)) }
            transition(s0, s2, s1, GOAL, null) { lhg(setOf(UP)) }
            transition(s13, s3, s6, GRAFT, setOf(RP(rA, 0, 2))) { lhg(setOf(UP, a, m)) }
            transition(s13, s4, s6, GRAFT, setOf(RP(rA, 0, 2))) { lhg(setOf(UP, a, m)) }
            transition(s13, s5, s6, GRAFT, setOf(RP(rA, 0, 2))) { lhg(setOf(UP, a, m)) }
            transition(setOf(s0, s13, s15), s3, s12, HEIGHT, null) { lhg(setOf(a), setOf(m)); lhg(setOf(a), setOf(a));lhg(setOf(a), setOf(UP)) }
            transition(setOf(s0, s13, s15), s4, s12, HEIGHT, null) { lhg(setOf(a), setOf(m)); lhg(setOf(a), setOf(a));lhg(setOf(a), setOf(UP)) }
            transition(setOf(s0, s13, s15), s5, s12, HEIGHT, null) { lhg(setOf(a), setOf(m)); lhg(setOf(a), setOf(a));lhg(setOf(a), setOf(UP)) }
            transition(s12, s7, s13, GRAFT, setOf(RP(rA, 0, 1))) { lhg(setOf(v)) }
            transition(setOf(s0, s13, s15), s10, s3, HEIGHT, null) { lhg(setOf(a), setOf(a)); lhg(setOf(m), setOf(m));lhg(setOf(UP), setOf(UP)) }
            transition(setOf(s0, s13, s15), s8, s4, HEIGHT, null) { lhg(setOf(a), setOf(a)); lhg(setOf(m), setOf(m));lhg(setOf(UP), setOf(UP)) }
            transition(setOf(s0, s13, s15), s6, s5, HEIGHT, null) { lhg(setOf(a), setOf(a)); lhg(setOf(m), setOf(m));lhg(setOf(UP), setOf(UP)) }
            transition(s13, s3, s8, GRAFT, setOf(RP(rM, 0, 2))) { lhg(setOf(UP, a, m)) }
            transition(s13, s4, s8, GRAFT, setOf(RP(rM, 0, 2))) { lhg(setOf(UP, a, m)) }
            transition(s13, s5, s8, GRAFT, setOf(RP(rM, 0, 2))) { lhg(setOf(UP, a, m)) }
            transition(setOf(s0, s13, s15), s3, s14, HEIGHT, null) { lhg(setOf(m), setOf(a)); lhg(setOf(m), setOf(m));lhg(setOf(m), setOf(UP)) }
            transition(setOf(s0, s13, s15), s4, s14, HEIGHT, null) { lhg(setOf(m), setOf(a)); lhg(setOf(m), setOf(m));lhg(setOf(m), setOf(UP)) }
            transition(setOf(s0, s13, s15), s5, s14, HEIGHT, null) { lhg(setOf(m), setOf(a)); lhg(setOf(m), setOf(m));lhg(setOf(m), setOf(UP)) }
            transition(s14, s9, s15, GRAFT, setOf(RP(rM, 0, 1))) { lhg(setOf(v)) }
            transition(setOf(s0, s13, s15), s11, s10, HEIGHT, null) { lhg(setOf(a), setOf(a)); lhg(setOf(m), setOf(m));lhg(setOf(UP), setOf(UP)) }
            transition(s0, s3, s2, HEIGHT, null) { lhg(setOf(UP), setOf(UP)) }
            transition(s0, s4, s2, HEIGHT, null) { lhg(setOf(UP), setOf(UP)) }
            transition(s0, s5, s2, HEIGHT, null) { lhg(setOf(UP), setOf(UP)) }
        }

        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun compare() {
        val rrs_noBuild = rrs.clone()
        val rrs_preBuild = rrs.clone()

        val parser = ScanOnDemandParser(rrs_noBuild)
        val sentences = listOf("v", "vav", "vavav", "vmv", "vmvmv", "vmvav", "vavmv")
        for (sen in sentences) {
            val (sppt, issues) = parser.parseForGoal("S", sen, AutomatonKind.LOOKAHEAD_1)
            if (issues.isNotEmpty()) {
                println("Sentence: $sen")
                issues.forEach { println(it) }
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