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

package net.akehurst.language.agl.runtime.structure

import kotlin.test.Test
import kotlin.test.assertEquals

class test_leftRecursive {

    companion object {
        // S =  'a' | S1 ;
        // S1 = S 'a' ;
        val rrs = runtimeRuleSet {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                literal("a")
                ref("S1")
            }
            concatenation("S1") { ref("S"); literal("a") }
        }
        val S = rrs.findRuntimeRule("S")
        val S1 = rrs.findRuntimeRule("S1")
        val a = rrs.findRuntimeRule("'a'")
        val G = rrs.startingState(S).runtimeRule
    }

    @Test
    fun s0_closureLR0() {
        val s0 = rrs.startingState(S)
        val actual = s0.calcClosureLR0(LookaheadSet.EMPTY).toList()

        val i0 = ClosureItemWithLookahead(null, RulePosition(G, 0, 0), setOf(RuntimeRuleSet.END_OF_TEXT))
        val i01 = ClosureItemWithLookahead(i0, RulePosition(S, 0, 0), setOf(RuntimeRuleSet.END_OF_TEXT, a))
        val i011 = ClosureItemWithLookahead(i01, RulePosition(a, 0, 0), setOf(RuntimeRuleSet.END_OF_TEXT, a))
        val i02 = ClosureItemWithLookahead(i0, RulePosition(S, 1, 0), setOf(RuntimeRuleSet.END_OF_TEXT,a))
        val i021 = ClosureItemWithLookahead(i02, RulePosition(S1, 0, 0), setOf(a))

        val expected = setOf<ClosureItemWithLookahead>(
                i0, i01, i011,
                i02, i021
        ).toList()

        assertEquals(expected.size, actual.size)
        for(i in actual.indices) {
            assertEquals(expected[i],actual[i])
        }
    }

    @Test
    fun s0_widthInto() {
        val s0 = rrs.startingState(S)

        val actual = s0.widthInto(LookaheadSet.EMPTY)
        val expected = listOf(
                Pair(RulePosition(a, 0, 0), LookaheadSet(0, s0, listOf(RuntimeRuleSet.END_OF_TEXT))),
                Pair(RulePosition(a, 0, 0), LookaheadSet(1, s0, listOf(a)))
        )
        assertEquals(expected, actual)
        for (i in 0 until actual.size) {
            assertEquals(expected[i].second.content, actual[i].second.content)
        }
    }

    @Test
    fun s0_transitions() {
        val s0 = rrs.startingState(S)

        val actual = s0.transitions(null, LookaheadSet.EMPTY)

        val s1 = s0.stateSet.fetch(RulePosition(a, 0, RulePosition.END_OF_RULE))
        val expected = listOf(
                Transition(s0, s1, Transition.ParseAction.WIDTH, LookaheadSet(0, s0, listOf(RuntimeRuleSet.END_OF_TEXT)), null, { _, _ -> true }),
                Transition(s0, s1, Transition.ParseAction.WIDTH, LookaheadSet(1, s0, listOf(a)), null, { _, _ -> true })
        ).toList()
        assertEquals(expected.size, actual.size)
        for (i in actual.indices) {
            assertEquals(expected[i], actual[i])
        }
    }

    @Test
    fun s1_heightOrGraftInto() {
        val s0 = rrs.startingState(S)
        val s0_trans = s0.transitions(null, LookaheadSet.EMPTY)
        val t_EOT_a = s0_trans[0]
        val s1 = t_EOT_a.to
        val actual = s1.heightOrGraftInto(s0, t_EOT_a.lookaheadGuard)
        val expected = listOf(
                Pair(RulePosition(S, 0, 0), LookaheadSet(0, s1, listOf(RuntimeRuleSet.END_OF_TEXT))),
                Pair(RulePosition(S, 0, 0), LookaheadSet(1, s1, listOf(a)))
        )
        assertEquals(expected, actual)
        for (i in actual.indices) {
            assertEquals(expected[i].second.content, actual[i].second.content)
        }
    }

    @Test
    fun s1_transitions_EOT() {
        val s0 = rrs.startingState(S)
        val s0_trans = s0.transitions(null, LookaheadSet.EMPTY)
        val t_EOT_a = s0_trans[0]
        val s1 = t_EOT_a.to

        val actual = s1.transitions(s0, t_EOT_a.lookaheadGuard)
        val s2 = s0.stateSet.fetch(RulePosition(S,0,RulePosition.END_OF_RULE))
        val expected = listOf<Transition>(
                Transition(s1, s2, Transition.ParseAction.HEIGHT, LookaheadSet(0, s1, listOf(RuntimeRuleSet.END_OF_TEXT)), null, { _, _ -> true }),
                Transition(s1, s2, Transition.ParseAction.HEIGHT, LookaheadSet(1, s1, listOf(a)), null, { _, _ -> true })
        )
        assertEquals(expected, actual)
        for (i in actual.indices) {
            assertEquals(expected[i], actual[i])
        }
    }

    @Test
    fun s1_transitions_a() {
        val s0 = rrs.startingState(S)
        val s0_trans = s0.transitions(null, LookaheadSet.EMPTY)
        val t_a_a = s0_trans[1]
        val s1 = t_a_a.to

        val actual = s1.transitions(s0, t_a_a.lookaheadGuard)
        val s2 = s0.stateSet.fetch(RulePosition(S,0,RulePosition.END_OF_RULE))
        val expected = listOf<Transition>(
                Transition(s1, s2, Transition.ParseAction.HEIGHT, LookaheadSet(0, s1, listOf(RuntimeRuleSet.END_OF_TEXT)), null, { _, _ -> true }),
                Transition(s1, s2, Transition.ParseAction.HEIGHT, LookaheadSet(1, s1, listOf(a)), null, { _, _ -> true })
        )
        assertEquals(expected, actual)
        for (i in actual.indices) {
            assertEquals(expected[i], actual[i])
        }
    }

    @Test
    fun s2_transitions_EOT() {
        val s0 = rrs.startingState(S)
        val s0_trans = s0.transitions(null, LookaheadSet.EMPTY)
        val s0_EOT_s1 = s0_trans[0]
        val s1 = s0_EOT_s1.to
        val s1_trans = s1.transitions(s0, s0_EOT_s1.lookaheadGuard)
        val s1_EOT_s2 = s1_trans[0]
        val s2 = s1_EOT_s2.to

        val actual = s2.transitions(s0, s1_EOT_s2.lookaheadGuard)
        val s3 = s0.stateSet.fetch(RulePosition(G,0,1))
        val s4 = s0.stateSet.fetch(RulePosition(S1,0,1))
        val expected = listOf<Transition>(
                Transition(s2, s4, Transition.ParseAction.HEIGHT, LookaheadSet(1, s2, listOf(a)), null, { _, _ -> true }),
                Transition(s2, s3, Transition.ParseAction.GRAFT, LookaheadSet(0, s2, listOf(RuntimeRuleSet.END_OF_TEXT)), null, { _, _ -> true })
        )
        assertEquals(expected.size, actual.size)
        for (i in actual.indices) {
            assertEquals(expected[i], actual[i])
        }
    }

    @Test
    fun s2_transitions_a() {
        val s0 = rrs.startingState(S)
        val s0_trans = s0.transitions(null, LookaheadSet.EMPTY)
        val s0_a_s1 = s0_trans[0]
        val s1 = s0_a_s1.to
        val s1_trans = s1.transitions(s0, s0_a_s1.lookaheadGuard)
        val s1_a_s2 = s1_trans[0]
        val s2 = s1_a_s2.to

        val actual = s2.transitions(s0, s1_a_s2.lookaheadGuard)
        val s3 = s0.stateSet.fetch(RulePosition(G,0,1))
        val s4 = s0.stateSet.fetch(RulePosition(S1,0,1))
        val expected = listOf<Transition>(
                Transition(s2, s4, Transition.ParseAction.HEIGHT, LookaheadSet(1, s2, listOf(a)), null, { _, _ -> true }),
                Transition(s2, s3, Transition.ParseAction.GRAFT, LookaheadSet(0, s2, listOf(RuntimeRuleSet.END_OF_TEXT)), null, { _, _ -> true })
        )
        assertEquals(expected.size, actual.size)
        for (i in actual.indices) {
            assertEquals(expected[i], actual[i])
        }
    }

    @Test
    fun s4_transitions() {
        val s0 = rrs.startingState(S)
        val s0_trans = s0.transitions(null, LookaheadSet.EMPTY)
        val s0_a_s1 = s0_trans[0]
        val s1 = s0_a_s1.to
        val s1_trans = s1.transitions(s0, s0_a_s1.lookaheadGuard)
        val s1_a_s2 = s1_trans[0]
        val s2 = s1_a_s2.to
        val s2_trans = s2.transitions(s0, s1_a_s2.lookaheadGuard)
        val s2_a_s4 = s2_trans[0]
        val s4 = s0.stateSet.fetch(RulePosition(S1,0,1))

        val actual = s4.transitions(s0, s2_a_s4.lookaheadGuard)
        val expected = listOf<Transition>(
                Transition(s4, s1, Transition.ParseAction.WIDTH, LookaheadSet(1, s2, listOf(a)), null, { _, _ -> true })
        )
        assertEquals(expected.size, actual.size)
        for (i in actual.indices) {
            assertEquals(expected[i], actual[i])
        }
    }
}