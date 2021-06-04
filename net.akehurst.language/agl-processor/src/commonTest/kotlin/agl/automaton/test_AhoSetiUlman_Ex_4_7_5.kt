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

import agl.automaton.automaton
import net.akehurst.language.agl.runtime.structure.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class test_AhoSetiUlman_Ex_4_7_5 : test_Abstract() {

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
    private companion object {

        val rrs = runtimeRuleSet {
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
        val S = rrs.findRuntimeRule("S")
        val S1 = rrs.findRuntimeRule("S1")
        val S2 = rrs.findRuntimeRule("S2")
        val S3 = rrs.findRuntimeRule("S3")
        val S4 = rrs.findRuntimeRule("S4")
        val rA = rrs.findRuntimeRule("A")
        val rB = rrs.findRuntimeRule("B")
        val a = rrs.findRuntimeRule("'a'")
        val b = rrs.findRuntimeRule("'b'")
        val c = rrs.findRuntimeRule("'c'")
        val d = rrs.findRuntimeRule("'d'")

        val SM = rrs.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        val s0 = SM.startState
        val G = s0.runtimeRules.first()
        val s1 = SM.states[listOf(RP(d, 0, EOR))]

    }

    override val SM: ParserStateSet get() = Companion.SM

    override val firstOf_data: List<Triple<RulePosition, LookaheadSet, Set<RuntimeRule>>>
        get() = listOf(
            Triple(RP(rA, 0, SOR), lhs_U, setOf(d)),     // A = . d
            Triple(RP(rA, 0, EOR), lhs_U, setOf(d)),     // A = d .
            Triple(RP(rB, 0, SOR), lhs_U, setOf(d)),     // B = . d
            Triple(RP(rB, 0, EOR), lhs_U, setOf(d)),     // B = d .

            Triple(RP(S1, 0, SOR), lhs_U, setOf(d)),     // S1 = . A a
            Triple(RP(S1, 0, SOR), lhs_U, setOf(d)),     // S1 = A . a
            Triple(RP(S1, 0, SOR), lhs_U, setOf(d)),     // S1 = A a .
            Triple(RP(S2, 0, SOR), lhs_U, setOf(d)),     // S2 = . b A c
            Triple(RP(S2, 0, SOR), lhs_U, setOf(d)),     // S2 = b . A c
            Triple(RP(S2, 0, SOR), lhs_U, setOf(d)),     // S2 = b A . c
            Triple(RP(S2, 0, SOR), lhs_U, setOf(d)),     // S2 = b A c .
            Triple(RP(S3, 0, SOR), lhs_U, setOf(d)),     // S3 = . B c
            Triple(RP(S3, 0, SOR), lhs_U, setOf(d)),     // S3 = B . c
            Triple(RP(S3, 0, SOR), lhs_U, setOf(d)),     // S3 = B c .
            Triple(RP(S4, 0, SOR), lhs_U, setOf(d)),     // S4 = . b B a
            Triple(RP(S4, 0, SOR), lhs_U, setOf(d)),     // S4 = b . B a
            Triple(RP(S4, 0, SOR), lhs_U, setOf(d)),     // S4 = b B . a
            Triple(RP(S4, 0, SOR), lhs_U, setOf(d)),     // S4 = b B a .

            Triple(RP(S, 0, SOR), lhs_U, setOf(d)),     // S = . S1
            Triple(RP(S, 0, EOR), lhs_U, setOf(UP)),     // S = S1 .
            Triple(RP(S, 1, SOR), lhs_U, setOf(b)),     // S = . S2
            Triple(RP(S, 1, EOR), lhs_U, setOf(UP)),     // S = S2 .
            Triple(RP(S, 2, SOR), lhs_U, setOf(d)),     // S = . S3
            Triple(RP(S, 2, EOR), lhs_U, setOf(UP)),     // S = S3 .
            Triple(RP(S, 3, SOR), lhs_U, setOf(b)),     // S = . S4
            Triple(RP(S, 3, EOR), lhs_U, setOf(UP)),     // S = S4 .

            Triple(RP(G, 0, SOR), lhs_U, setOf(d, b)),     // G = . S
            Triple(RP(G, 0, EOR), lhs_U, setOf(UP))        // G = S .
        )

    override val s0_widthInto_expected: List<WidthInfo>
        get() = listOf(
            WidthInfo(RP(d, 0, 0), lhs_T),
            WidthInfo(RP(b, 0, 0), lhs_T),
            WidthInfo(RP(d, 0, 0), lhs_T)
        )

    @Test
    fun s0_widthInto1() {
        val s0 = SM.startState
        val actual = s0.widthInto(null).toList()

        val expected = s0_widthInto_expected
        assertEquals(expected.size, actual.size)
        for (i in 0 until actual.size) {
            assertEquals(expected[i], actual[i])
        }
    }

    @Test
    fun createClosure_G00_UP() {
        val cl_G = ClosureItemLC1(null, RP(G, 0, 0), RP(G, 0, EOR), lhs_U)
        //val cl_G_S = ClosureItemLC1(cl_G, RP(S, 0, 0), RulePosition(S, 0, 1), lhs_bcU)
        //val cl_G_S_aOpt0 = ClosureItemLC1(cl_G_S, RP(aOpt, OMI, 0), RP(aOpt, OMI, EOR), lhs_bcU)
        //val cl_G_S_aOpt1 = ClosureItemLC1(cl_G_S, RP(aOpt, OME, 0), RP(aOpt, OME, EOR), lhs_bcU)

        TODO()
        //val actual = SM.buildCache.calcClosure(RP(G, 0, 0), lhs_U)
        //val expected = setOf(
        //    cl_G //, cl_G_S, cl_G_S_aOpt0, cl_G_S_aOpt1
        //)
        //assertEquals(expected, actual)
    }

    @Test
    fun s0_transitions() {
        val s0 = rrs.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1).startState

        val actual = s0.transitions(null)

        val s2 = s0.stateSet.fetch(listOf(RulePosition(b, 0, RulePosition.END_OF_RULE)))
        val s1 = s0.stateSet.fetch(listOf(RulePosition(d, 0, RulePosition.END_OF_RULE)))
        val expected = listOf(
            Transition(s0, s1, Transition.ParseAction.WIDTH, LookaheadSet(3, setOf(a, c)), LookaheadSet.EMPTY, null) { _, _ -> true },
            Transition(s0, s2, Transition.ParseAction.WIDTH, LookaheadSet(1, setOf(d)), LookaheadSet.EMPTY, null) { _, _ -> true }
        ).toList()
        assertEquals(expected.size, actual.size)
        for (i in 0 until actual.size) {
            assertEquals(expected[i], actual[i])
        }
    }

    @Test
    fun s1_heightOrGraftInto_s0() {
        val actual = s1.heightOrGraftInto(s0)

        assertNotNull(actual)
        val expected = emptySet<HeightGraftInfo>()

        assertEquals(expected, actual)
    }

    @Test
    fun s1_transitions_s0() {
        val actual = s1.transitions(s0)
        val s3 = s0.stateSet.fetch(listOf(RulePosition(rA, 0, RulePosition.END_OF_RULE)))
        val s4 = s0.stateSet.fetch(listOf(RulePosition(rB, 0, RulePosition.END_OF_RULE)))
        val expected = listOf<Transition>(
            Transition(s1, s3, Transition.ParseAction.HEIGHT, LookaheadSet(0, setOf(a)), LookaheadSet.EMPTY, null) { _, _ -> true },
            Transition(s1, s4, Transition.ParseAction.HEIGHT, LookaheadSet(1, setOf(c)), LookaheadSet.EMPTY, null) { _, _ -> true }
        )

        assertEquals(expected, actual)
    }

    @Test
    fun buildFor() {
        val actual = rrs.buildFor("S", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", false) {
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

            transition(null, s0, s1, WIDTH, setOf(a, c), setOf(UP), listOf())
            transition(null, s0, s2, WIDTH, setOf(d), setOf(UP), listOf())

            transition(s0, s1, s3, HEIGHT, setOf(a), setOf(a), listOf(RP(rA, 0, 0)))
            transition(s0, s1, s4, HEIGHT, setOf(c), setOf(c), listOf(RP(rB, 0, 0)))
            transition(s14, s1, s3, HEIGHT, setOf(c), setOf(c), listOf(RP(rA, 0, 0)))
            transition(s14, s1, s4, HEIGHT, setOf(a), setOf(a), listOf(RP(rB, 0, 0)))

            transition(s0, s2, s4, HEIGHT, setOf(a, c), setOf(UP), listOf())
            transition(s0, s0, s1, HEIGHT, setOf(a, c), setOf(UP), listOf())
        }

        assertEquals(expected, actual)
    }
/*
    @Test
    fun transitions() {
        // G(s0) -*-> ?
        val s0 = rrs.startingState(S)
        val actual_s0 = s0.transitions(null, LookaheadSet.EMPTY)

        val s1 = s0.stateSet.fetch(RulePosition(b, 0, RulePosition.END_OF_RULE))
        val s2 = s0.stateSet.fetch(RulePosition(d, 0, RulePosition.END_OF_RULE))
        val expected_s0 = listOf<Transition>(
                Transition(s0, s1, Transition.ParseAction.WIDTH, setOf(d), null, { _, _ -> true }),
                Transition(s0, s2, Transition.ParseAction.WIDTH, setOf(a,c), null, { _, _ -> true })
        )
        assertEquals(expected_s0, actual_s0)

        // G(s0) -WIDTH-> b(s1) -*-> ?
        val actual_s0_s1 = s1.transitions(s0)
        val s3 = s0.stateSet.fetch(RulePosition(S2, 0, 1))
        val s4 = s0.stateSet.fetch(RulePosition(S4, 0, 1))
        val expected_s0_s1 = listOf<Transition>(
                Transition(s1, s3, Transition.ParseAction.HEIGHT, setOf(d), null, { _, _ -> true }),
                Transition(s1, s4, Transition.ParseAction.HEIGHT, setOf(d), null, { _, _ -> true })
        )
        assertEquals(expected_s0_s1, actual_s0_s1)

        // G(s0) -WIDTH-> b(s1) -HEIGHT-> S2(s3) -*-> ?
        val actual_s0_s3 = s3.transitions(s0)
        val expected_s0_s3 = listOf<Transition>(
                Transition(s3, s2, Transition.ParseAction.WIDTH, setOf(c), null, { _, _ -> true })
        )
        assertEquals(expected_s0_s3, actual_s0_s3)

        // G(s0) -WIDTH-> b(s1) -HEIGHT-> S4(s4) -*-> ?
        val actual_s4 = s4.transitions(s0)
        val expected_s4 = listOf<Transition>(
                Transition(s4, s2, Transition.ParseAction.WIDTH, setOf(a), null, { _, _ -> true })
        )
        assertEquals(expected_s4, actual_s4)

        // G(s0) -WIDTH-> d(s2) -*-> ?
        val actual_s0_s2 = s2.transitions(s0)
        val s5 = s0.stateSet.fetch(RulePosition(rA, 0, RulePosition.END_OF_RULE))
        val s6 = s0.stateSet.fetch(RulePosition(rB, 0, RulePosition.END_OF_RULE))
        val expected_s0_s2 = listOf<Transition>(
                Transition(s2, s5, Transition.ParseAction.HEIGHT, setOf(a,c), null, { _, _ -> true }),
                Transition(s2, s6, Transition.ParseAction.HEIGHT, setOf(a,c), null, { _, _ -> true })
        )
        assertEquals(expected_s0_s2, actual_s0_s2)

        // G(s0) -WIDTH-> b(s1) -HEIGHT-> S2(s4) -WIDTH-> d(s2) -*-> ?
        val actual_s4_s2 = s2.transitions(s4)
        val expected_s4_s2 = listOf<Transition>(
                Transition(s2, s6, Transition.ParseAction.HEIGHT, setOf(a,c), null, { _, _ -> true })
        )
        assertEquals(expected_s4_s2, actual_s4_s2)

    }
*/

}