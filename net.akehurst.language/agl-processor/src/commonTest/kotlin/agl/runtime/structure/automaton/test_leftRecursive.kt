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

        val s0 = rrs.startingState(S)

        val lhs_E = LookaheadSet.EMPTY
        val lhs_T = LookaheadSet(0, setOf(rrs.END_OF_TEXT))
        val lhs_a = LookaheadSet(1, setOf(a))
        val lhs_aT = LookaheadSet(2, setOf(a, rrs.END_OF_TEXT))
    }

    @Test
    fun s0_calcClosureLR0() {

        val actual1 = s0.calcClosure(LookaheadSet.EMPTY).toList()
        val actual = s0.calcClosureLR0().toList()

        val cl_G = ClosureItemWithLookaheadList(null, RulePosition(G, 0, 0), listOf(lhs_T))
        val cl_G_S0 = ClosureItemWithLookaheadList(cl_G, RulePosition(S, 0, 0), listOf(lhs_T))
        val cl_G_S0_a = ClosureItemWithLookaheadList(cl_G_S0, RulePosition(a, 0, 0), listOf(lhs_T))
        val cl_G_S1 = ClosureItemWithLookaheadList(cl_G, RulePosition(S, 1, 0), listOf(lhs_T))
        val cl_G_S1_S1 = ClosureItemWithLookaheadList(cl_G_S1, RulePosition(S1, 0, 0), listOf(lhs_T, lhs_a))
        val cl_G_S1_S1_S0 = ClosureItemWithLookaheadList(cl_G_S1_S1, RulePosition(S, 0, 0), listOf(lhs_T, lhs_a))
        val cl_G_S1_S1_S0_a = ClosureItemWithLookaheadList(cl_G_S1_S1_S0, RulePosition(a, 0, 0), listOf(lhs_T, lhs_a))


        val expected = listOf(
                cl_G, cl_G_S0, cl_G_S0_a,
                cl_G_S1, cl_G_S1_S1, cl_G_S1_S1_S0, cl_G_S1_S1_S0_a
        )
        assertEquals(expected, actual)
        for (i in 0 until actual.size) {
            assertEquals(expected[i].lookaheadSetList, actual[i].lookaheadSetList)
        }
    }

    @Test
    fun s0_widthInto() {

        val actual = s0.widthInto2()

        val cl_G = ClosureItemWithLookaheadList(null, RulePosition(G, 0, 0), listOf(lhs_T))
        val cl_G_S0 = ClosureItemWithLookaheadList(cl_G, RulePosition(S, 0, 0), listOf(lhs_T))
        val cl_G_S0_a = ClosureItemWithLookaheadList(cl_G_S0, RulePosition(a, 0, 0), listOf(lhs_T))
        val cl_G_S1 = ClosureItemWithLookaheadList(cl_G, RulePosition(S, 1, 0), listOf(lhs_T))
        val cl_G_S1_S1 = ClosureItemWithLookaheadList(cl_G_S1, RulePosition(S1, 0, 0), listOf(lhs_T, lhs_a))
        val cl_G_S1_S1_S0 = ClosureItemWithLookaheadList(cl_G_S1_S1, RulePosition(S, 0, 0), listOf(lhs_T, lhs_a))
        val cl_G_S1_S1_S0_a = ClosureItemWithLookaheadList(cl_G_S1_S1_S0, RulePosition(a, 0, 0), listOf(lhs_T, lhs_a))

        val expected = listOf(
                cl_G_S0_a, cl_G_S1_S1_S0_a
        )
        assertEquals(expected.size, actual.size)
        for (i in 0 until actual.size) {
            assertEquals(expected[i].lookaheadSetList, actual[i].lookaheadSetList)
        }
    }

    @Test
    fun s0_transitions() {
        val actual = s0.transitions(null)
        val s1 = s0.stateSet.fetch(RulePosition(a, 0, RulePosition.END_OF_RULE))

        val expected = listOf(
                Transition(s0, s1, Transition.ParseAction.WIDTH, listOf(lhs_T), lhs_T, null) { _, _ -> true },
                Transition(s0, s1, Transition.ParseAction.WIDTH, listOf(lhs_T, lhs_a), lhs_a, null) { _, _ -> true }
        ).toList()
        assertEquals(expected.size, actual.size)
        for (i in actual.indices) {
            assertEquals(expected[i], actual[i])
        }
    }

    @Test
    fun a_parentPositions() {
        val actual = s0.stateSet.parentPosition[a]
        val expected = setOf(
                RulePosition(S, 0, 0),
                RulePosition(S1, 0, 1)
        )
        assertEquals(expected, actual)
    }

    @Test
    fun S_0_E_fetchOrCreateNext() {
        val rp = RulePosition(S, 0, RulePosition.END_OF_RULE)
        val actual = s0.stateSet.fetchOrCreateNext(rp)
        val expected = setOf(a, rrs.END_OF_TEXT)
        assertEquals(expected, actual)
    }

    @Test
    fun S1_0_E_fetchOrCreateNext() {
        val rp = RulePosition(S1, 0, RulePosition.END_OF_RULE)
        val actual = s0.stateSet.fetchOrCreateNext(rp)
        val expected = setOf(a, rrs.END_OF_TEXT)
        assertEquals(expected, actual)
    }

    @Test
    fun s1_heightOrGraftInto() {

        s0.transitions(null)
        val s1 = s0.stateSet.fetch(RulePosition(a, 0, RulePosition.END_OF_RULE))

        val actual5 = s1.heightOrGraftInto5().toList()

        val actual1 = s1.heightOrGraftInto(s0).toList()
        val actual2 = s1.stateSet.parentRelation(s1.rulePosition.runtimeRule).toList()
        val actual = s1.heightOrGraftInto4().toList()


        val expected = listOf(
                Pair(RulePosition(S, 0, 0), setOf(a, rrs.END_OF_TEXT)),
                Pair(RulePosition(S1, 0, 1), setOf(a, rrs.END_OF_TEXT))
        )
        assertEquals(expected, actual)

    }

    @Test
    fun s1_transitions() {
        s0.transitions(null) // create transitions and to-states
        val s1 = s0.stateSet.fetch(RulePosition(a, 0, RulePosition.END_OF_RULE))

        val actual = s1.transitions(s0)
        val s2 = s0.stateSet.fetch(RulePosition(S, 0, RulePosition.END_OF_RULE))
        val s3 = s0.stateSet.fetch(RulePosition(S1, 0, RulePosition.END_OF_RULE))
        val expected = listOf<Transition>(
                Transition(s1, s2, Transition.ParseAction.HEIGHT, listOf(lhs_aT), lhs_aT, null) { _, _ -> true },
                Transition(s1, s3, Transition.ParseAction.GRAFT, listOf(lhs_aT), lhs_a, null) { _, _ -> true }
        )
        assertEquals(expected.size, actual.size)
        for (i in actual.indices) {
            assertEquals(expected[i], actual[i])
        }
    }


    @Test
    fun S_parentPositions() {
        val actual = s0.stateSet.parentPosition[S]
        val expected = setOf(
                RulePosition(G, 0, 0),
                RulePosition(S1, 0, 0)
        )
        assertEquals(expected, actual)
    }

    @Test
    fun G_0_1_fetchOrCreateNext() {
        val rp = RulePosition(G, 0, 1)
        val actual = s0.stateSet.fetchOrCreateNext(rp)
        val expected = setOf(rrs.END_OF_TEXT)
        assertEquals(expected, actual)
    }

    @Test
    fun S1_0_1_fetchOrCreateNext() {
        val rp = RulePosition(S1, 0, 1)
        val actual = s0.stateSet.fetchOrCreateNext(rp)
        val expected = setOf(a)
        assertEquals(expected, actual)
    }

    @Test
    fun s2_heightOrGraftInto() {
        s0.transitions(null)
        val s1 = s0.stateSet.fetch(RulePosition(a, 0, RulePosition.END_OF_RULE))
        s1.transitions(s0)
        val s2 = s0.stateSet.fetch(RulePosition(S, 0, RulePosition.END_OF_RULE))

        val actual = s2.heightOrGraftInto3()

        val expected = setOf(
                RulePosition(G, 0, 0),
                RulePosition(S1, 0, 0)
        )
        assertEquals(expected, actual)
    }

    @Test
    fun s2_parentRelations() {
        s0.transitions(null)
        val s1 = s0.stateSet.fetch(RulePosition(a, 0, RulePosition.END_OF_RULE))
        s1.transitions(s0)
        val s2 = s0.stateSet.fetch(RulePosition(S, 0, RulePosition.END_OF_RULE))

        val actual = s2.parentRelations

        val expected = setOf(
                ParentRelation(s0.stateSet, 2, RulePosition(G, 0, 0), lhs_T.content),
                ParentRelation(s0.stateSet, 3, RulePosition(S1, 0, 0), lhs_a.content)
        )
        assertEquals(expected, actual)
    }

    @Test
    fun s2_transitions() {
        s0.transitions(null)
        val s1 = s0.stateSet.fetch(RulePosition(a, 0, RulePosition.END_OF_RULE))
        s1.transitions(s0)
        val s2 = s0.stateSet.fetch(RulePosition(S, 0, RulePosition.END_OF_RULE))

        val actual = s2.transitions(s0)
        val s5 = s0.stateSet.fetch(RulePosition(G, 0, 1))
        val s4 = s0.stateSet.fetch(RulePosition(S1, 0, 1))
        val expected = listOf<Transition>(
                Transition(s2, s4, Transition.ParseAction.HEIGHT, listOf(lhs_a), lhs_a, null) { _, _ -> true },
                Transition(s2, s5, Transition.ParseAction.GRAFT, listOf(lhs_T), lhs_T, null) { _, _ -> true }
        )
        assertEquals(expected, actual)
    }

    @Test
    fun s4_widthInto() {
        s0.transitions(null)
        val s1 = s0.stateSet.fetch(RulePosition(a, 0, RulePosition.END_OF_RULE))
        s1.transitions(s0)
        val s2 = s0.stateSet.fetch(RulePosition(S, 0, RulePosition.END_OF_RULE))
        s2.transitions(s0)
        val s4 = s0.stateSet.fetch(RulePosition(S1, 0, 1))
        val s5 = s0.stateSet.fetch(RulePosition(G, 0, 1))

        val actual = s4.widthInto2()

        val cl_G = ClosureItemWithLookaheadList(null, RulePosition(G, 0, 0), listOf(lhs_T))
        val expected = listOf(
                cl_G
        )
        assertEquals(expected.size, actual.size)
        for (i in actual.indices) {
            assertEquals(expected[i], actual[i])
        }
    }

    @Test
    fun s4_transitions() {
        s0.transitions(null)
        val s1 = s0.stateSet.fetch(RulePosition(a, 0, RulePosition.END_OF_RULE))
        s1.transitions(s0)
        val s2 = s0.stateSet.fetch(RulePosition(S, 0, RulePosition.END_OF_RULE))
        s2.transitions(s0)
        val s4 = s0.stateSet.fetch(RulePosition(S1, 0, 1))
        val s5 = s0.stateSet.fetch(RulePosition(G, 0, 1))

        val actual = s4.transitions(s0)
        val expected = listOf<Transition>(
                Transition(s4, s1, Transition.ParseAction.WIDTH, listOf(lhs_aT), lhs_a, null) { _, _ -> true }
        )
        assertEquals(expected.size, actual.size)
        for (i in actual.indices) {
            assertEquals(expected[i], actual[i])
        }
    }

    @Test
    fun s3_transitions() {
        s0.transitions(null)
        val s1 = s0.stateSet.fetch(RulePosition(a, 0, RulePosition.END_OF_RULE))
        s1.transitions(s0)
        val s2 = s0.stateSet.fetch(RulePosition(S, 0, RulePosition.END_OF_RULE))
        val s3 = s0.stateSet.fetch(RulePosition(S1, 0, RulePosition.END_OF_RULE))
        s2.transitions(s0)
        val s4 = s0.stateSet.fetch(RulePosition(S1, 0, 1))
        val s5 = s0.stateSet.fetch(RulePosition(G, 0, 1))

        val actual = s3.transitions(s0)
        val s6 = s0.stateSet.fetch(RulePosition(S, 1, RulePosition.END_OF_RULE))
        val expected = listOf<Transition>(
                Transition(s3, s6, Transition.ParseAction.HEIGHT, listOf(lhs_aT), lhs_a, null) { _, _ -> true }
        )
        assertEquals(expected.size, actual.size)
        for (i in actual.indices) {
            assertEquals(expected[i], actual[i])
        }
    }

    @Test
    fun s6_transitions() {
        s0.transitions(null)
        val s1 = s0.stateSet.fetch(RulePosition(a, 0, RulePosition.END_OF_RULE))
        s1.transitions(s0)
        val s2 = s0.stateSet.fetch(RulePosition(S, 0, RulePosition.END_OF_RULE))
        val s3 = s0.stateSet.fetch(RulePosition(S1, 0, RulePosition.END_OF_RULE))
        s2.transitions(s0)
        val s4 = s0.stateSet.fetch(RulePosition(S1, 0, 1))
        val s5 = s0.stateSet.fetch(RulePosition(G, 0, 1))
        s3.transitions(s0)
        val s6 = s0.stateSet.fetch(RulePosition(S, 1, RulePosition.END_OF_RULE))

        val actual = s6.transitions(s0)

        val expected = listOf<Transition>(
                Transition(s6, s4, Transition.ParseAction.HEIGHT, listOf(lhs_a), lhs_a, null) { _, _ -> true },
                Transition(s6, s5, Transition.ParseAction.GRAFT, listOf(lhs_T), lhs_a, null) { _, _ -> true }
        )
        assertEquals(expected.size, actual.size)
        for (i in actual.indices) {
            assertEquals(expected[i], actual[i])
        }
    }
}