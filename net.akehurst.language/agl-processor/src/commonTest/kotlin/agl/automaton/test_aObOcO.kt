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
import net.akehurst.language.agl.runtime.structure.RulePosition
import net.akehurst.language.agl.runtime.structure.RuntimeRuleItem
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.processor.AutomatonKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class test_aObOcO : test_AutomatonAbstract() {
    /*
        S = a? b? c?;
     */
    // must be fresh per test or automaton is not correct for different parses (due to caching)
    private val rrs = runtimeRuleSet {
        concatenation("S") { ref("aOpt"); ref("bOpt"); ref("cOpt") }
        multi("aOpt", 0, 1, "'a'")
        multi("bOpt", 0, 1, "'b'")
        multi("cOpt", 0, 1, "'c'")
        literal("'a'", "a")
        literal("'b'", "b")
        literal("'c'", "c")
    }

    private val S = rrs.findRuntimeRule("S")
    private val SM = rrs.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
    private val aOpt = rrs.findRuntimeRule("aOpt")
    private val aOpt_E = aOpt.rhs.items[RuntimeRuleItem.MULTI__EMPTY_RULE]
    private val bOpt = rrs.findRuntimeRule("bOpt")
    private val bOpt_E = bOpt.rhs.items[RuntimeRuleItem.MULTI__EMPTY_RULE]
    private val cOpt = rrs.findRuntimeRule("cOpt")
    private val cOpt_E = cOpt.rhs.items[RuntimeRuleItem.MULTI__EMPTY_RULE]
    private val a = rrs.findRuntimeRule("'a'")
    private val b = rrs.findRuntimeRule("'b'")
    private val c = rrs.findRuntimeRule("'c'")
    private val G = SM.startState.runtimeRules.first()

    private val lhs_bcU = SM.createLookaheadSet(true, false, false, setOf(b, c))

    @Test
    override fun firstOf() {
        listOf(
            Triple(RulePosition(cOpt, RuntimeRuleItem.MULTI__EMPTY_RULE, 0), lhs_U, LHS(UP)), // cOpt = . empty
            Triple(RulePosition(cOpt, RuntimeRuleItem.MULTI__ITEM, 0), lhs_U, LHS(c)),        // cOpt = . c
            Triple(RulePosition(bOpt, RuntimeRuleItem.MULTI__EMPTY_RULE, 0), lhs_U, LHS(UP)), // bOpt = . empty
            Triple(RulePosition(bOpt, RuntimeRuleItem.MULTI__ITEM, 0), lhs_U, LHS(b)),        // bOpt = . b
            Triple(RulePosition(aOpt, RuntimeRuleItem.MULTI__EMPTY_RULE, 0), lhs_U, LHS(UP)), // aOpt = . empty
            Triple(RulePosition(aOpt, RuntimeRuleItem.MULTI__ITEM, 0), lhs_U, LHS(a)),        // aOpt = . a
            Triple(RulePosition(S, 0, RulePosition.END_OF_RULE), lhs_U, LHS(UP)),              // S = a? b? c? .
            Triple(RulePosition(S, 0, 2), lhs_U, LHS(c, UP)),                           // S = a? b? . c?
            Triple(RulePosition(S, 0, 1), lhs_U, LHS(b, c, UP)),                        // S = a? . b? c?
            Triple(RulePosition(S, 0, 0), lhs_U, LHS(a, b, c, UP)),                     // S = a? . b? c?
            Triple(RulePosition(G, 0, RulePosition.END_OF_RULE), lhs_U, LHS(UP)),               // G = S .
            Triple(RulePosition(G, 0, 0), lhs_U, LHS(a, b, c, UP))                      // G = . S
        ).testAll { rp, lhs, expected ->
            val actual = SM.buildCache.firstOf(rp, lhs.part)
            assertEquals(expected, actual, "failed $rp")
        }
    }

    @Test
    override fun s0_widthInto() {
        val s0 = SM.startState
        val actual = s0.widthInto(s0)

        val expected = setOf(
            WidthInfo(RP(aOpt_E, 0, EOR),  LHS(UP, b, c)),
            WidthInfo(RP(a, 0, EOR), LHS(UP, b, c)),
        )
        assertEquals(expected, actual)

    }

    @Test
    fun s0_transitions() {
         val s0 = SM.startState
         val s1 = SM.createState(listOf(RP(a, 0, EOR)))
         val s2 = SM.createState(listOf(RP(aOpt_E, 0, EOR)))
        val actual = s0.transitions(s0)
        val expected = listOf(
            Transition(s0, s1, Transition.ParseAction.WIDTH, lhs_bcU, LookaheadSet.EMPTY, null) { _, _ -> true },
            Transition(s0, s2, Transition.ParseAction.WIDTH, lhs_bcU, LookaheadSet.EMPTY, null) { _, _ -> true }
        )
        assertEquals(expected, actual)
    }

    @Test
    fun parse_a() {
        val parser = ScanOnDemandParser(rrs)
        val (sppt, issues) = parser.parseForGoal("S", "a", AutomatonKind.LOOKAHEAD_1)
        assertNotNull(sppt)
        assertEquals(0, issues.size)
        assertEquals(1, sppt.maxNumHeads)
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", 0, false) {
            val s0 = state(RP(G, 0, SOR))
            val s1 = state(RP(a, 0, EOR))
            val s2 = state(RP(aOpt_E, 0, EOR))
            val s3 = state(RP(aOpt, 0, EOR))
            val s4 = state(RP(S, 0, 1))
            val s5 = state(RP(b, 0, EOR))
            val s6 = state(RP(bOpt_E, 0, EOR))
            val s7 = state(RP(bOpt, 1, EOR))
            val s8 = state(RP(S, 0, 2))
            val s9 = state(RP(c, 0, EOR))
            val s10 = state(RP(cOpt_E, 0, EOR))
            val s11 = state(RP(cOpt, 1, EOR))
            val s12 = state(RP(S, 0, EOR))
            val s13 = state(RP(G, 0, EOR))

            transition(s0, s0, s1, WIDTH, setOf(UP,b,c), setOf(), null)
            transition(s0, s0, s2, WIDTH, setOf(UP,b,c), setOf(), null)
            transition(s0, s1, s3, HEIGHT, setOf(UP,b,c), setOf(setOf(UP,b,c)), listOf( RP(aOpt, 0, SOR)))
            transition(s0, s3, s4, HEIGHT, setOf(UP,b,c), setOf(setOf(UP)), listOf( RP(S, 0, SOR)))
            transition(s0, s4, s5, WIDTH, setOf(UP,c), setOf(), null)
            transition(s0, s4, s6, WIDTH, setOf(UP,c), setOf(), null)
            transition(s4, s6, s7, HEIGHT, setOf(UP,c), setOf(setOf(UP,c)), listOf( RP(bOpt, 1, SOR)))
            transition(s4, s7, s8, GRAFT, setOf(UP,c), setOf(setOf(UP)), listOf( RP(S, 0, 1)))
            transition(s0, s8, s9, WIDTH, setOf(UP), setOf(), null)
            transition(s0, s8, s10, WIDTH, setOf(UP), setOf(), null)
            transition(s8, s10, s11, HEIGHT, setOf(UP), setOf(setOf(UP)), listOf( RP(cOpt, 1, SOR)))
            transition(s8, s11, s12, GRAFT, setOf(UP), setOf(setOf(UP)), listOf( RP(S, 0, 2)))
            transition(s0, s12, s13, GOAL, setOf(UP), setOf(setOf(UP)), null)
        }
        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun parse_b() {
        val parser = ScanOnDemandParser(rrs)
        val (sppt, issues) = parser.parseForGoal("S", "b", AutomatonKind.LOOKAHEAD_1)
        assertNotNull(sppt)
        assertEquals(0, issues.size)
        assertEquals(1, sppt.maxNumHeads)
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", 0, false) {
            val s0 = state(RP(G, 0, SOR))
            val s1 = state(RP(a, 0, EOR))
            val s2 = state(RP(aOpt_E, 0, EOR))
            val s3 = state(RP(aOpt, 1, EOR))
            val s4 = state(RP(S, 0, 1))
            val s5 = state(RP(b, 0, EOR))
            val s6 = state(RP(bOpt_E, 0, EOR))
            val s7 = state(RP(bOpt, 0, EOR))
            val s8 = state(RP(S, 0, 2))
            val s9 = state(RP(c, 0, EOR))
            val s10 = state(RP(cOpt_E, 0, EOR))
            val s11 = state(RP(cOpt, 1, EOR))
            val s12 = state(RP(S, 0, EOR))
            val s13 = state(RP(G, 0, EOR))

            transition(s0, s0, s1, WIDTH, setOf(UP,b,c), setOf(), null)
            transition(s0, s0, s2, WIDTH, setOf(UP,b,c), setOf(), null)
            transition(s0, s2, s3, HEIGHT, setOf(UP,b,c), setOf(setOf(UP,b,c)), listOf( RP(aOpt, 1, SOR)))
            transition(s0, s3, s4, HEIGHT, setOf(UP,b,c), setOf(setOf(UP)), listOf( RP(S, 0, SOR)))
            transition(s0, s4, s5, WIDTH, setOf(UP,c), setOf(), null)
            transition(s0, s4, s6, WIDTH, setOf(UP,c), setOf(), null)
            transition(s4, s5, s7, HEIGHT, setOf(UP,c), setOf(setOf(UP,c)), listOf( RP(bOpt, 0, SOR)))
            transition(s4, s7, s8, GRAFT, setOf(UP,c), setOf(setOf(UP)), listOf( RP(S, 0, 1)))
            transition(s0, s8, s9, WIDTH, setOf(UP), setOf(), null)
            transition(s0, s8, s10, WIDTH, setOf(UP), setOf(), null)
            transition(s8, s10, s11, HEIGHT, setOf(UP), setOf(setOf(UP)), listOf( RP(cOpt, 1, SOR)))
            transition(s8, s11, s12, GRAFT, setOf(UP), setOf(setOf(UP)), listOf( RP(S, 0, 2)))
            transition(s0, s12, s13, GOAL, setOf(UP), setOf(setOf(UP)),null)
        }
        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun parse_c() {
        val parser = ScanOnDemandParser(rrs)
        val (sppt, issues) = parser.parseForGoal("S", "c", AutomatonKind.LOOKAHEAD_1)
        assertNotNull(sppt)
        assertEquals(0, issues.size)
        assertEquals(1, sppt.maxNumHeads)
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", 0, false) {
            val s0 = state(RP(G, 0, SOR))
            val s1 = state(RP(a, 0, EOR))
            val s2 = state(RP(aOpt_E, 0, EOR))
            val s3 = state(RP(aOpt, 1, EOR))
            val s4 = state(RP(S, 0, 1))
            val s5 = state(RP(b, 0, EOR))
            val s6 = state(RP(bOpt_E, 0, EOR))
            val s7 = state(RP(bOpt, 1, EOR))
            val s8 = state(RP(S, 0, 2))
            val s9 = state(RP(c, 0, EOR))
            val s10 = state(RP(cOpt_E, 0, EOR))
            val s11 = state(RP(cOpt, 0, EOR))
            val s12 = state(RP(S, 0, EOR))
            val s13 = state(RP(G, 0, EOR))

            transition(s0, s0, s1, WIDTH, setOf(UP,b,c), setOf(), null)
            transition(s0, s0, s2, WIDTH, setOf(UP,b,c), setOf(), null)
            transition(s0, s2, s3, HEIGHT, setOf(UP,b,c), setOf(setOf(UP,b,c)), listOf( RP(aOpt, 1, SOR)))
            transition(s0, s3, s4, HEIGHT, setOf(UP,b,c), setOf(setOf(UP)), listOf( RP(S, 0, SOR)))
            transition(s0, s4, s5, WIDTH, setOf(UP,c), setOf(), null)
            transition(s0, s4, s6, WIDTH, setOf(UP,c), setOf(), null)
            transition(s4, s6, s7, HEIGHT, setOf(UP,c), setOf(setOf(UP,c)), listOf( RP(bOpt, 1, SOR)))
            transition(s4, s7, s8, GRAFT, setOf(UP,c), setOf(setOf(UP)), listOf( RP(S, 0, 1)))
            transition(s0, s8, s9, WIDTH, setOf(UP), setOf(), null)
            transition(s0, s8, s10, WIDTH, setOf(UP), setOf(), null)
            transition(s8, s9, s11, HEIGHT, setOf(UP), setOf(setOf(UP)), listOf( RP(cOpt, 0, SOR)))
            transition(s8, s11, s12, GRAFT, setOf(UP), setOf(setOf(UP)), listOf( RP(S, 0, 2)))
            transition(s0, s12, s13, GOAL, setOf(UP), setOf(setOf(UP)),null)
        }
        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun stateInfo() {
        val bc = BuildCacheLC1(SM)

        val actual = bc.stateInfo2()
    }

    @Test
    fun buildFor() {
        val actual = rrs.buildFor("S", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))

        val sentences = listOf("","a", "b","ab","c","ac","bc","abc")
        sentences.forEach {
            val parser = ScanOnDemandParser(rrs)
            val (sppt, issues) = parser.parseForGoal("S", it, AutomatonKind.LOOKAHEAD_1)
            assertNotNull(sppt, issues.joinToString("\n") { it.toString() } )
            assertEquals(0, issues.size)
            assertEquals(1, sppt.maxNumHeads)
        }

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", 0, false) {
            val s0 = state(RP(G, 0, SOR))
            val s1 = state(RP(a, 0, EOR))
            val s2 = state(RP(aOpt_E, 0, EOR))
            val s3 = state(RP(aOpt, 0, EOR))
            val s4 = state(RP(aOpt, 1, EOR))
            val s5 = state(RP(S, 0, 1))
            val s6 = state(RP(b, 0, EOR))
            val s7 = state(RP(bOpt_E, 0, EOR))
            val s8 = state(RP(bOpt, 0, EOR))
            val s9 = state(RP(bOpt, 1, EOR))
            val s10 = state(RP(S, 0, 2))
            val s11 = state(RP(c, 0, EOR))
            val s12 = state(RP(cOpt_E, 0, EOR))
            val s13 = state(RP(cOpt, 0, EOR))
            val s14 = state(RP(cOpt, 1, EOR))
            val s15 = state(RP(S, 0, EOR))
            val s16 = state(RP(G, 0, EOR))

            transition(s0, s0, s1, WIDTH, setOf(UP,b,c), setOf(), null)
            transition(s0, s0, s2, WIDTH, setOf(UP,b,c), setOf(), null)
            transition(s0, s1, s3, HEIGHT, setOf(UP,b,c), setOf(setOf(UP,b,c)), null)
            transition(s0, s2, s4, HEIGHT, setOf(UP,b,c), setOf(setOf(UP,b,c)), null)
            transition(s0, s3, s5, HEIGHT, setOf(UP,b,c), setOf(setOf(UP)), null)
            transition(s0, s4, s5, HEIGHT, setOf(UP,b,c), setOf(setOf(UP)), null)
            transition(s0, s5, s6, WIDTH, setOf(UP,c), setOf(), null)
            transition(s0, s5, s7, WIDTH, setOf(UP,c), setOf(), null)
            transition(s5, s6, s8, HEIGHT, setOf(UP,c), setOf(setOf(UP,c)), null)
            transition(s5, s7, s9, HEIGHT, setOf(UP,c), setOf(setOf(UP,c)),null)
            transition(s5, s8, s10, GRAFT, setOf(UP,c), setOf(setOf(UP)), listOf( RP(S, 0, 1)))
            transition(s5, s9, s10, GRAFT, setOf(UP,c), setOf(setOf(UP)), listOf( RP(S, 0, 1)))
            transition(s0, s10, s11, WIDTH, setOf(UP), setOf(), null)
            transition(s0, s10, s12, WIDTH, setOf(UP), setOf(), null)
            transition(s10, s11, s13, HEIGHT, setOf(UP), setOf(setOf(UP)), null)
            transition(s10, s12, s14, HEIGHT, setOf(UP), setOf(setOf(UP)), null)
            transition(s10, s13, s15, GRAFT, setOf(UP), setOf(setOf(UP)), listOf( RP(S, 0, 2)))
            transition(s10, s14, s15, GRAFT, setOf(UP), setOf(setOf(UP)), listOf( RP(S, 0, 2)))
            transition(s0, s15, s16, GOAL, setOf(UP), setOf(),null)
        }

        AutomatonTest.assertEquals(expected, actual)
    }
}