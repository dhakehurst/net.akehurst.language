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
import net.akehurst.language.agl.runtime.structure.LookaheadSet
import net.akehurst.language.agl.runtime.structure.RulePosition
import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.processor.AutomatonKind
import kotlin.test.Test
import kotlin.test.assertEquals

internal class test_rightRecursive : test_AutomatonAbstract() {

    // S =  'a' | S1 ;
    // S1 = 'a' S ;

    private companion object {

        val rrs = runtimeRuleSet {
            choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
                literal("a")
                ref("S1")
            }
            concatenation("S1") { literal("a"); ref("S") }
        }
        val S = rrs.findRuntimeRule("S")
        val SM = rrs.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        val G = SM.startState.runtimeRules.first()
        val S1 = rrs.findRuntimeRule("S1")
        val a = rrs.findRuntimeRule("'a'")

        val s0 = SM.startState
        val s1 = SM.createState(listOf(RulePosition(a, 0, RulePosition.END_OF_RULE)))
        val s2 = SM.createState(listOf(RulePosition(S, 0, RulePosition.END_OF_RULE)))
        val s3 = SM.createState(listOf(RulePosition(S1, 0, 1)))
        val s4 = SM.createState(listOf(RulePosition(S1, 0, RulePosition.END_OF_RULE)))
        val s5 = SM.createState(listOf(RulePosition(G, 0, RulePosition.END_OF_RULE)))
        val s6 = SM.createState(listOf(RulePosition(S, 1, RulePosition.END_OF_RULE)))

        val lhs_a = SM.createLookaheadSet(false, false, false,setOf(a))
        val lhs_aU = SM.createLookaheadSet(true, false, false,setOf(a))
    }

    @Test
    override fun firstOf() {
        listOf(
            Triple(RulePosition(G, 0, RulePosition.START_OF_RULE), lhs_U, LHS(a)), // G = . S
            Triple(RulePosition(G, 0, RulePosition.END_OF_RULE), lhs_U, LHS(UP)), // G = S .
            Triple(RulePosition(S, 0, RulePosition.START_OF_RULE), lhs_U, LHS(a)), // S = . a
            Triple(RulePosition(S, 0, RulePosition.END_OF_RULE), lhs_U, LHS(UP)), // S = a .
            Triple(RulePosition(S, 1, RulePosition.START_OF_RULE), lhs_U, LHS(a)), // S = . S1
            Triple(RulePosition(S, 1, RulePosition.END_OF_RULE), lhs_U, LHS(UP)), // S = S1 .
            Triple(RulePosition(S1, 0, RulePosition.START_OF_RULE), lhs_U, LHS(a)), // S1 = . a S
            Triple(RulePosition(S1, 1, RulePosition.START_OF_RULE), lhs_U, LHS(a)), // S1 = a . S
            Triple(RulePosition(S1, 0, RulePosition.END_OF_RULE), lhs_U, LHS(UP)) // S1 = a S .
        ).testAll { rp, lhs, expected ->
            val actual = SM.buildCache.firstOf(rp, lhs.part)
            assertEquals(expected, actual, "failed $rp")
        }
    }

    @Test
    override fun s0_widthInto() {
        val s0 = SM.startState
        val actual = s0.widthInto(s0).toList()

        val expected = listOf(
            WidthInfo(RulePosition(a, 0, RulePosition.END_OF_RULE), lhs_aU.part)
        )
        assertEquals(expected.size, actual.size)
        for (i in 0 until actual.size) {
            assertEquals(expected[i], actual[i])
        }
    }

    @Test
    fun s0_transitions() {
        val actual = s0.transitions(s0)
        val expected = listOf(
            Transition(s0, s1, Transition.ParseAction.WIDTH, lhs_aU, LookaheadSet.EMPTY, null) { _, _ -> true }
        ).toList()

        assertEquals(expected.size, actual.size)
        for (i in actual.indices) {
            assertEquals(expected[i], actual[i])
        }
    }

    @Test
    fun s1_heightOrGraftInto_s0() {
        val actual = s1.heightOrGraftInto(s0).toList()

        val expected = listOf(
            HeightGraftInfo(
                Transition.ParseAction.HEIGHT,
                listOf(RulePosition(S, 0, 0)),
                listOf(RulePosition(S, 0, RulePosition.END_OF_RULE)),
                lhs_U.part,
                setOf(LHS(UP))
            ),
            HeightGraftInfo(
                Transition.ParseAction.HEIGHT,
                listOf(RulePosition(S1, 0, 0)),
                listOf(RulePosition(S1, 0, 1)),
                lhs_a.part,
                setOf(LHS(UP))
            )
        )
        assertEquals(expected, actual)

    }

    @Test
    fun s1_transitions() {

        val actual = s1.transitions(s0)

        val expected = listOf<Transition>(
            Transition(s1, s2, Transition.ParseAction.HEIGHT, lhs_U, LookaheadSet.EMPTY, null) { _, _ -> true },
            Transition(s1, s3, Transition.ParseAction.HEIGHT, lhs_a, LookaheadSet.EMPTY, null) { _, _ -> true }
        )
        assertEquals(expected.size, actual.size)
        for (i in actual.indices) {
            assertEquals(expected[i], actual[i])
        }
    }

    @Test
    fun s2_heightOrGraftInto_s0() {

        val actual = s2.heightOrGraftInto(s0)

        val expected = setOf(
            HeightGraftInfo(
                Transition.ParseAction.HEIGHT,
                listOf(RulePosition(G, 0, 0)),
                listOf(RulePosition(G, 0, RulePosition.END_OF_RULE)),
                lhs_U.part,
                setOf(LHS(UP))
            )
        )
        assertEquals(expected, actual)
    }

    @Test
    fun s2_heightOrGraftInto_s3() {

        val actual = s2.heightOrGraftInto(s3)

        val expected = setOf(
            HeightGraftInfo(
                Transition.ParseAction.HEIGHT,
                listOf(RulePosition(S1, 0, 1)),
                listOf(RulePosition(S1, 0, RulePosition.END_OF_RULE)),
                lhs_U.part,
                setOf(LHS(UP))
            )
        )
        assertEquals(expected, actual)
    }

    @Test
    fun s2_transitions_s0() {
        val actual = s2.transitions(s0)
        val expected = listOf<Transition>(
            Transition(s2, s5, Transition.ParseAction.GRAFT, lhs_U, LookaheadSet.EMPTY, null) { _, _ -> true }
        )
        assertEquals(expected, actual)
    }

    @Test
    fun s2_transitions_s3() {
        val actual = s2.transitions(s3)
        val expected = listOf<Transition>(
            Transition(s2, s4, Transition.ParseAction.GRAFT, lhs_U, LookaheadSet.EMPTY, null) { _, _ -> true }
        )
        assertEquals(expected, actual)
    }

    @Test
    fun s4_heightOrGraftInto_s3() {

        val actual = s4.heightOrGraftInto(s3)


        val expected = setOf(
            HeightGraftInfo(
                Transition.ParseAction.HEIGHT,
                listOf(RulePosition(S, 1, 0)),
                listOf(RulePosition(S, 1, RulePosition.END_OF_RULE)),
                lhs_U.part,
                setOf(LHS(UP))
            )
        )
        assertEquals(expected, actual)

    }

    @Test
    fun s4_transitions() {

        val actual = s4.transitions(s0)
        val expected = listOf<Transition>(
            Transition(s4, s6, Transition.ParseAction.HEIGHT, lhs_U, LookaheadSet.EMPTY, null) { _, _ -> true }
        )
        assertEquals(expected.size, actual.size)
        for (i in actual.indices) {
            assertEquals(expected[i], actual[i])
        }
    }


    @Test
    fun s3_transitions() {

        val actual = s3.transitions(s0)

        val expected = listOf<Transition>(
            Transition(s3, s1, Transition.ParseAction.WIDTH, lhs_aU, LookaheadSet.EMPTY, null) { _, _ -> true }
        )
        assertEquals(expected.size, actual.size)
        for (i in actual.indices) {
            assertEquals(expected[i], actual[i])
        }
    }

    @Test
    fun s6_transitions_s0() {

        val actual = s6.transitions(s0)

        val expected = listOf<Transition>(
            Transition(s6, s5, Transition.ParseAction.GRAFT, lhs_U, LookaheadSet.EMPTY, null) { _, _ -> true }
        )
        assertEquals(expected.size, actual.size)
        for (i in actual.indices) {
            assertEquals(expected[i], actual[i])
        }
    }

    @Test
    fun s6_transitions_s3() {

        val actual = s6.transitions(s3)

        val expected = listOf<Transition>(
            Transition(s6, s4, Transition.ParseAction.GRAFT, lhs_U, LookaheadSet.EMPTY, null) { _, _ -> true }
        )
        assertEquals(expected.size, actual.size)
        for (i in actual.indices) {
            assertEquals(expected[i], actual[i])
        }
    }

    @Test
    fun parse_aba() {
        val parser = ScanOnDemandParser(rrs)
        parser.parseForGoal("S", "aba", AutomatonKind.LOOKAHEAD_1)
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

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", 1, false) {

        }

        AutomatonTest.assertEquals(expected, actual)
    }
}