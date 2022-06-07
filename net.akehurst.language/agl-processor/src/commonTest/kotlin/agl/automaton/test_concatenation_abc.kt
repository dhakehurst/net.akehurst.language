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
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.processor.AutomatonKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class test_concatenation_abc : test_AutomatonAbstract() {
    // S =  'a' 'b' 'c' ;

    // must be fresh per test or automaton is not correct for different parses (due to caching)
    private val rrs = runtimeRuleSet {
        concatenation("S") { literal("a");literal("b");literal("c") }
    }

    private val S = rrs.findRuntimeRule("S")
    private val SM = rrs.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
    private val G = SM.startState.runtimeRules.first()

    private val a = rrs.findRuntimeRule("'a'")
    private val b = rrs.findRuntimeRule("'b'")
    private val c = rrs.findRuntimeRule("'c'")

    private val lhs_b = SM.createLookaheadSet(false, false, false, setOf(b))
    private val lhs_c = SM.createLookaheadSet(false, false, false, setOf(c))

    @Test
    fun parse_abc() {
        val parser = ScanOnDemandParser(rrs)
        val (sppt, issues) = parser.parseForGoal("S", "abc", AutomatonKind.LOOKAHEAD_1)
        assertNotNull(sppt)
        assertEquals(0, issues.size)
        assertEquals(1, sppt.maxNumHeads)
        println(rrs.usedAutomatonToString("S"))

        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", 0, false) {
            val s0 = state(RP(G, 0, SOR))
            val s1 = state(RP(a, 0, EOR))
            val s2 = state(RP(S, 0, 1))
            val s3 = state(RP(b, 0, EOR))
            val s4 = state(RP(S, 0, 2))
            val s5 = state(RP(c, 0, EOR))
            val s6 = state(RP(S, 0, EOR))
            val s7 = state(RP(G, 0, EOR))

            transition(s0, s0, s1, WIDTH, setOf(b), setOf(), null)
            transition(s0, s1, s2, HEIGHT, setOf(b), setOf(setOf(UP)), listOf(RP(S, 0, SOR)))
            transition(s0, s2, s3, WIDTH, setOf(c), setOf(), null)
            transition(s2, s3, s4, GRAFT, setOf(c), setOf(setOf(UP)), listOf(RP(S, 0, 1)))
            transition(s0, s4, s5, WIDTH, setOf(UP), setOf(), null)
            transition(s4, s5, s6, GRAFT, setOf(UP), setOf(setOf(UP)), listOf(RP(S, 0, 2)))
            transition(s0, s6, s7, GRAFT, setOf(UP), setOf(setOf(UP)), listOf(RP(G, 0, 0)))
            transition(s0, s7, s7, GOAL, setOf(), setOf(), null)
        }
        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun buildFor() {
        val actual = rrs.buildFor("S", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))

        val parser = ScanOnDemandParser(rrs)
        val (sppt, issues) = parser.parseForGoal("S", "abc", AutomatonKind.LOOKAHEAD_1)
        assertNotNull(sppt) { issues.joinToString("\n") { it.toString() } }
        assertEquals(0, issues.size)
        assertEquals(1, sppt.maxNumHeads)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", 0, false) {
            val s0 = state(RP(G, 0, SOR))
            val s1 = state(RP(S, 0, 1))
            val s2 = state(RP(S, 0, 2))
            val s3 = state(RP(G, 0, EOR))
            val s4 = state(RP(S, 0, EOR))
            val s5 = state(RP(a, 0, EOR))
            val s6 = state(RP(b, 0, EOR))
            val s7 = state(RP(c, 0, EOR))

            transition(s0, s0, s5, WIDTH, setOf(b), setOf(), null)
            transition(s0, s1, s6, WIDTH, setOf(c), setOf(), null)
            transition(s0, s2, s7, WIDTH, setOf(UP), setOf(), null)
            transition(s0, s4, s3, GOAL, setOf(UP), setOf(), listOf(RP(G, 0, 0)))
            transition(s0, s5, s1, HEIGHT, setOf(b), setOf(setOf(UP)), listOf(RP(S, 0, SOR)))
            transition(s1, s6, s2, GRAFT, setOf(c), setOf(setOf(UP)), listOf(RP(S, 0, 1)))
            transition(s2, s7, s4, GRAFT, setOf(UP), setOf(setOf(UP)), listOf(RP(S, 0, 2)))
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

        val parser = ScanOnDemandParser(rrs_noBuild)
        val sentences = listOf("abc")
        for(sen in sentences) {
            val (sppt, issues) = parser.parseForGoal("S", sen, AutomatonKind.LOOKAHEAD_1)
            if (issues.isNotEmpty())  issues.forEach { println(it) }
        }
        val automaton_noBuild = rrs_noBuild.usedAutomatonFor("S")
        val automaton_preBuild = rrs_preBuild.buildFor("S",AutomatonKind.LOOKAHEAD_1)

        println("--Pre Build--")
        println(rrs_preBuild.usedAutomatonToString("S"))
        println("--No Build--")
        println(rrs_noBuild.usedAutomatonToString("S"))

        AutomatonTest.assertEquals(automaton_preBuild, automaton_noBuild)
    }
}