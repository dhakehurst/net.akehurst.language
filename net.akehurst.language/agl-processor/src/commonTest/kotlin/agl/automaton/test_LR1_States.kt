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

import net.akehurst.language.agl.runtime.structure.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class test_LR1_States : test_Abstract() {

    companion object {
        // This grammar is LR(1) but not LALR(1)

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

        val SM = rrs.fetchStateSetFor(S)
        val s0 = SM.startState
        val G = s0.runtimeRules.first()

    }

    override val SM: ParserStateSet
        get() = Companion.SM

    override val firstOf_data: List<Triple<RulePosition, LookaheadSet, Set<RuntimeRule>>>
        get() = TODO("not implemented")

    override val s0_widthInto_expected: List<Pair<RulePosition, LookaheadSet>>
        get() = listOf(
                Pair(RP(d, 0, 0), lhs_T),
                Pair(RP(b, 0, 0), lhs_T),
                Pair(RP(d, 0, 0), lhs_T)
        )


    @Test
    fun s0_transitions() {
        val s0 = rrs.startingState(S)

        val actual = s0.transitions(null)

        val s2 = s0.stateSet.fetch(listOf(RulePosition(b, 0, RulePosition.END_OF_RULE)))
        val s1 = s0.stateSet.fetch(listOf(RulePosition(d, 0, RulePosition.END_OF_RULE)))
        val expected = listOf(
                Transition(s0, s1, Transition.ParseAction.WIDTH, LookaheadSet(0, setOf(a)), LookaheadSet.EMPTY, null) { _, _ -> true },
                Transition(s0, s2, Transition.ParseAction.WIDTH, LookaheadSet(1, setOf(d)), LookaheadSet.EMPTY, null) { _, _ -> true },
                Transition(s0, s1, Transition.ParseAction.WIDTH, LookaheadSet(2, setOf(c)), LookaheadSet.EMPTY, null) { _, _ -> true }
        ).toList()
        assertEquals(expected.size, actual.size)
        for (i in 0 until actual.size) {
            assertEquals(expected[i], actual[i])
        }
    }

    @Test
    fun s1_heightOrGraftInto() {
        // G
        val s0 = rrs.startingState(S)
        val trans_s0 = s0.transitions(null)
        val tr_a_d = trans_s0.first { it.lookaheadGuard.content.contains(a) }
        val tr_d_b = trans_s0.first { it.lookaheadGuard.content.contains(d) }
        val tr_c_d = trans_s0.first { it.lookaheadGuard.content.contains(c) }
        // - WIDTH -> d
        val s1 = s0.stateSet.fetch(listOf(RulePosition(d, 0, RulePosition.END_OF_RULE)))
        val actual = s1.heightOrGraftInto(s1.rulePositions)
        assertNotNull(actual)
        val expected = emptySet<HeightGraft>()

        assertEquals(expected, actual)
    }

    @Test
    fun s1_transitions() {
        val s0 = rrs.startingState(S)
        val s0_trans = s0.transitions(null)
        val s1 = s0.stateSet.fetch(listOf(RulePosition(d, 0, RulePosition.END_OF_RULE)))
        val tr_a_d = s0_trans.first { it.lookaheadGuard.content.contains(a) }

        val actual = s1.transitions(s0)
        val s3 = s0.stateSet.fetch(listOf(RulePosition(rA, 0, RulePosition.END_OF_RULE)))
        val s4 = s0.stateSet.fetch(listOf(RulePosition(rB, 0, RulePosition.END_OF_RULE)))
        val expected = listOf<Transition>(
                Transition(s1, s3, Transition.ParseAction.HEIGHT, LookaheadSet(0, setOf(a)), LookaheadSet.EMPTY, null) { _, _ -> true },
                Transition(s1, s4, Transition.ParseAction.HEIGHT, LookaheadSet(1, setOf(c)), LookaheadSet.EMPTY, null) { _, _ -> true }
        )
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