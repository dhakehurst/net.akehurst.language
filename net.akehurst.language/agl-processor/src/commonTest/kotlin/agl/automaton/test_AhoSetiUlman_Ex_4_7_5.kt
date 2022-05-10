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
import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.processor.AutomatonKind
import kotlin.test.Test
import kotlin.test.assertEquals
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
    override fun firstOf() {
        listOf(
            Triple(RP(rA, 0, SOR), lhs_U, LHS(d)),     // A = . d
            Triple(RP(rA, 0, EOR), lhs_U, LHS(d)),     // A = d .
            Triple(RP(rB, 0, SOR), lhs_U, LHS(d)),     // B = . d
            Triple(RP(rB, 0, EOR), lhs_U, LHS(d)),     // B = d .

            Triple(RP(S1, 0, SOR), lhs_U, LHS(d)),     // S1 = . A a
            Triple(RP(S1, 0, SOR), lhs_U, LHS(d)),     // S1 = A . a
            Triple(RP(S1, 0, SOR), lhs_U, LHS(d)),     // S1 = A a .
            Triple(RP(S2, 0, SOR), lhs_U, LHS(d)),     // S2 = . b A c
            Triple(RP(S2, 0, SOR), lhs_U, LHS(d)),     // S2 = b . A c
            Triple(RP(S2, 0, SOR), lhs_U, LHS(d)),     // S2 = b A . c
            Triple(RP(S2, 0, SOR), lhs_U, LHS(d)),     // S2 = b A c .
            Triple(RP(S3, 0, SOR), lhs_U, LHS(d)),     // S3 = . B c
            Triple(RP(S3, 0, SOR), lhs_U, LHS(d)),     // S3 = B . c
            Triple(RP(S3, 0, SOR), lhs_U, LHS(d)),     // S3 = B c .
            Triple(RP(S4, 0, SOR), lhs_U, LHS(d)),     // S4 = . b B a
            Triple(RP(S4, 0, SOR), lhs_U, LHS(d)),     // S4 = b . B a
            Triple(RP(S4, 0, SOR), lhs_U, LHS(d)),     // S4 = b B . a
            Triple(RP(S4, 0, SOR), lhs_U, LHS(d)),     // S4 = b B a .

            Triple(RP(S, 0, SOR), lhs_U, LHS(d)),     // S = . S1
            Triple(RP(S, 0, EOR), lhs_U, LHS(UP)),     // S = S1 .
            Triple(RP(S, 1, SOR), lhs_U, LHS(b)),     // S = . S2
            Triple(RP(S, 1, EOR), lhs_U, LHS(UP)),     // S = S2 .
            Triple(RP(S, 2, SOR), lhs_U, LHS(d)),     // S = . S3
            Triple(RP(S, 2, EOR), lhs_U, LHS(UP)),     // S = S3 .
            Triple(RP(S, 3, SOR), lhs_U, LHS(b)),     // S = . S4
            Triple(RP(S, 3, EOR), lhs_U, LHS(UP)),     // S = S4 .

            Triple(RP(G, 0, SOR), lhs_U, LHS(d, b)),     // G = . S
            Triple(RP(G, 0, EOR), lhs_U, LHS(UP))        // G = S .
        ).testAll { rp, lhs, expected ->
            val actual = SM.buildCache.firstOf(rp, lhs.part)
            assertEquals(expected, actual, "failed $rp")
        }
    }

    override fun s0_widthInto() {
        val s0 = SM.startState
        val actual = s0.widthInto(s0).toList()

        val expected = listOf(
            WidthInfo(RP(d, 0, 0), lhs_T.part),
            WidthInfo(RP(b, 0, 0), lhs_T.part),
            WidthInfo(RP(d, 0, 0), lhs_T.part)
        )
        assertEquals(expected.size, actual.size)
        for (i in 0 until actual.size) {
            assertEquals(expected[i], actual[i])
        }
    }

    @Test
    fun s0_transitions() {
        val s0 = rrs.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1).startState

        val actual = s0.transitions(s0)

        val s2 = s0.stateSet.fetchCompatibleOrCreateState(listOf(RulePosition(b, 0, RulePosition.END_OF_RULE)))
        val s1 = s0.stateSet.fetchCompatibleOrCreateState(listOf(RulePosition(d, 0, RulePosition.END_OF_RULE)))
        val expected = listOf(
            Transition(s0, s1, Transition.ParseAction.WIDTH, lhs_ac, LookaheadSet.EMPTY, null) { _, _ -> true },
            Transition(s0, s2, Transition.ParseAction.WIDTH, lhs_d, LookaheadSet.EMPTY, null) { _, _ -> true }
        ).toList()
        assertEquals(expected.size, actual.size)
        for (i in 0 until actual.size) {
            assertEquals(expected[i], actual[i])
        }
    }

    @Test
    fun s1_heightOrGraftInto_s0() {
        val s1 = SM.createState(listOf(RP(d, 0, EOR)))
        val actual = s1.heightOrGraftInto(s0)

        assertNotNull(actual)
        val expected = emptySet<HeightGraftInfo>()

        assertEquals(expected, actual)
    }

    @Test
    fun s1_transitions_s0() {
        val s1 = SM.createState(listOf(RP(d, 0, EOR)))
        val actual = s1.transitions(s0)
        val s3 = s0.stateSet.fetchCompatibleOrCreateState(listOf(RulePosition(rA, 0, RulePosition.END_OF_RULE)))
        val s4 = s0.stateSet.fetchCompatibleOrCreateState(listOf(RulePosition(rB, 0, RulePosition.END_OF_RULE)))
        val expected = listOf<Transition>(
            Transition(s1, s3, Transition.ParseAction.HEIGHT, lhs_a, LookaheadSet.EMPTY, null) { _, _ -> true },
            Transition(s1, s4, Transition.ParseAction.HEIGHT, lhs_c, LookaheadSet.EMPTY, null) { _, _ -> true }
        )

        assertEquals(expected, actual)
    }

    @Test
    fun parse_da() {
        val parser = ScanOnDemandParser(rrs)
        parser.parseForGoal("S", "da", AutomatonKind.LOOKAHEAD_1)
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))
        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", 0, false) {


        }
        AutomatonTest.assertEquals(expected, actual)
    }

    @Test
    fun buildFor() {
        val actual = rrs.buildFor("S", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))

        val sentences = setOf("da","bdc","dc","bda")
        val parser = ScanOnDemandParser(rrs)
        sentences.forEach {
            val (sppt,issues) = parser.parseForGoal("S", it, AutomatonKind.LOOKAHEAD_1)
            assertNotNull(sppt)
        }

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", 1, false) {
            val s0 = state(RP(G, 0, SOR))      // G = . S
            val s1 = state(RP(d, 0, EOR))      // d
            val s2 = state(RP(b, 0, EOR))      // b
            val s3 = state(RP(rA, 0, EOR))     // A = d .
            val s4 = state(RP(rB, 0, EOR))     // B = d .
            val s5 = state(RP(S1, 0, 1))  // S1 = A . a
            val s6 = state(RP(a, 0, EOR))
            val s7 = state(RP(S1, 0, EOR))
            val s8 = state(RP(S, 0, EOR))
            val s9 = state(RP(G, 0, EOR))
            val s10 = state(RP(S3, 0, 1))
            val s11 = state(RP(c, 0, EOR))
            val s12 = state(RP(S3, 0, EOR))
            val s13 = state(RP(S, 2, EOR))
            val s14 = state(RP(S2, 0, 1), RP(S4, 0, 1))
            val s15 = state(RP(S2, 0, 2))
            val s16 = state(RP(S2, 0, EOR))
            val s17 = state(RP(S, 1, EOR))
            val s18 = state(RP(S4, 0, 2))
            val s19 = state(RP(S4, 0, EOR))
            val s20 = state(RP(S, 3, EOR))

            transition(s0, s0, s1, WIDTH, setOf(a, c), setOf(setOf(UP)), listOf())
            transition(s0, s0, s2, WIDTH, setOf(d), setOf(setOf(UP)), listOf())

            transition(s0, s1, s3, HEIGHT, setOf(a), setOf(setOf(a)), listOf(RP(rA, 0, 0)))
            transition(s0, s1, s4, HEIGHT, setOf(c), setOf(setOf(c)), listOf(RP(rB, 0, 0)))
            transition(s14, s1, s3, HEIGHT, setOf(c), setOf(setOf(c)), listOf(RP(rA, 0, 0)))
            transition(s14, s1, s4, HEIGHT, setOf(a), setOf(setOf(a)), listOf(RP(rB, 0, 0)))

            transition(s0, s2, s4, HEIGHT, setOf(a, c), setOf(setOf(UP)), listOf())
            transition(s0, s0, s1, HEIGHT, setOf(a, c), setOf(setOf(UP)), listOf())
        }

        AutomatonTest.assertEquals(expected, actual)
    }

}