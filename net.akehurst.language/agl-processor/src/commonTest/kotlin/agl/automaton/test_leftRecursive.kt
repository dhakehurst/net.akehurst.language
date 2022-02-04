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
import net.akehurst.language.agl.runtime.structure.RuntimeRuleChoiceKind
import net.akehurst.language.agl.runtime.structure.runtimeRuleSet
import net.akehurst.language.api.processor.AutomatonKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

internal class test_leftRecursive : test_AutomatonAbstract() {

    // S =  'a' | S1
    // S1 = S 'a'

    // must be fresh per test or automaton is not correct for different parses (due to caching)
    private val rrs = runtimeRuleSet {
        choice("S", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
            literal("a")
            ref("S1")
        }
        concatenation("S1") { ref("S"); literal("a") }
    }

    private val S = rrs.findRuntimeRule("S")
    private val SM = rrs.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
    private val G = SM.startState.runtimeRules.first()
    private val S1 = rrs.findRuntimeRule("S1")
    private val a = rrs.findRuntimeRule("'a'")

    private val s0 = SM.startState
    private val s1 = SM.states[listOf(RP(a, 0, EOR))]
    private val s2 = SM.states[listOf(RP(S, 0, EOR))]
    private val s3 = SM.states[listOf(RP(S1, 0, EOR))]
    private val s4 = SM.states[listOf(RP(S1, 0, 1))]
    private val s5 = SM.states[listOf(RP(G, 0, EOR))]
    private val s6 = SM.states[listOf(RP(S, 1, EOR))]

    private val lhs_a = SM.createLookaheadSet(false, false, false, setOf(a))
    private val lhs_aU = SM.createLookaheadSet(true, false, false, setOf(a))

    @Test
    override fun firstOf() {
        listOf(
            Triple(RP(G, 0, SOR), lhs_U, LHS(a)), // G = . S
            Triple(RP(G, 0, EOR), lhs_U, LHS(UP)), // G = S .
            Triple(RP(S, 0, SOR), lhs_U, LHS(a)), // S = . a
            Triple(RP(S, 0, EOR), lhs_U, LHS(UP)), // S = a .
            Triple(RP(S1, 0, SOR), lhs_U, LHS(a)), // S1 = . S a
            Triple(RP(S1, 1, SOR), lhs_U, LHS(a)), // S1 = S . a
            Triple(RP(S1, 0, EOR), lhs_U, LHS(UP)) // S1 = S a .
        ).testAll { rp, lhs, expected ->
            val actual = SM.buildCache.firstOf(rp, lhs.part)
            assertEquals(expected, actual, "failed $rp")
        }
    }

    @Test
    override fun s0_widthInto() {
        val s0 = SM.startState
        val actual = s0.widthInto(null).toList()

        val expected = listOf(
            WidthInfo(RP(a, 0, EOR), lhs_aU.part)
        )
        assertEquals(expected.size, actual.size)
        for (i in 0 until actual.size) {
            assertEquals(expected[i], actual[i])
        }
    }

    @Test
    fun s0_transitions() {
        val actual = s0.transitions(null)

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
                listOf(G),
                listOf(RP(S, 0, 0)),
                listOf(RP(S, 0, EOR)),
                lhs_U.part,
                setOf(LHS(UP))
            ),
            HeightGraftInfo(
                listOf(G,S,S1),
                listOf(RP(S, 0, 0)),
                listOf(RP(S, 0, EOR)),
                lhs_a.part,
                setOf(LHS(a))
            )
        )
        assertEquals(expected, actual)

    }

    @Test
    fun s1_transitions_s0() {
        val actual = s1.transitions(s0)

        val expected = listOf<Transition>(
            Transition(s1, s2, Transition.ParseAction.HEIGHT, lhs_U, lhs_U, listOf(RP(S,0,0))) { _, _ -> true },
            Transition(s1, s2, Transition.ParseAction.HEIGHT, lhs_a, lhs_a,listOf(RP(S,0,0))) { _, _ -> true }
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
                emptyList(),
                listOf(RP(G, 0, 0)),
                listOf(RP(G, 0, EOR)),
                lhs_U.part,
                setOf(LHS(UP))
            ),
            HeightGraftInfo(
                listOf(G,S),
                listOf(RP(S1, 0, 0)),
                listOf(RP(S1, 0, 1)),
                lhs_a.part,
                setOf(LHS(UP))
            )
            ,
            HeightGraftInfo(
                listOf(G,S,S1,S),
                listOf(RP(S1, 0, 0)),
                listOf(RP(S1, 0, 1)),
                lhs_a.part,
                setOf(LHS(a))
            )
        )
        assertEquals(expected, actual)
    }

    @Test
    fun s2_transitions_s0() {

        val actual = s2.transitions(s0)

        val expected = listOf<Transition>(
            Transition(s2, s4, Transition.ParseAction.HEIGHT, lhs_a, lhs_U, listOf(RP(S1,0,SOR))) { _, _ -> true },
            Transition(s2, s4, Transition.ParseAction.HEIGHT, lhs_a, lhs_a, listOf(RP(S1,0,SOR))) { _, _ -> true },
            Transition(s2, s5, Transition.ParseAction.GRAFT, lhs_U, lhs_U, listOf(RP(G,0,SOR))) { _, _ -> true }
        )
        assertEquals(expected, actual)
    }

    @Test
    fun s4_widthInto_s0() {
        // s4 | S1 = S . a
        val actual = s4.widthInto(s0).toList()

        val expected = listOf(
            WidthInfo(RP(a, 0, EOR), lhs_aU.part)
        )
        assertEquals(expected, actual)

    }

    @Test
    fun s4_transitions_s0() {

        val actual = s4.transitions(s0)
        val expected = listOf<Transition>(
            Transition(s4, s1, Transition.ParseAction.WIDTH, lhs_aU, lhs_aU, null) { _, _ -> true }
        )
        assertEquals(expected.size, actual.size)
        for (i in actual.indices) {
            assertEquals(expected[i], actual[i])
        }
    }


    @Test
    fun s1_heightOrGraftInto_s4() {

        val actual = s1.heightOrGraftInto(s4).toList()

        val expected = listOf(
            HeightGraftInfo(
                emptyList(),
                listOf(RP(S1, 0, 1)),
                listOf(RP(S1, 0, EOR)),
                lhs_aU.part,
                setOf(LHS(UP))
            )
        )
        assertEquals(expected, actual)

    }

    @Test
    fun s1_transitions_s4() {

        val actual = s1.transitions(s4)
        val expected = listOf<Transition>(
            Transition(s1, s3, Transition.ParseAction.GRAFT, lhs_aU, lhs_aU, null) { _, _ -> true }
        )
        assertEquals(expected.size, actual.size)
        for (i in actual.indices) {
            assertEquals(expected[i], actual[i])
        }
    }

    @Test
    fun s3_transitions_s0() {

        val actual = s3.transitions(s0)

        val expected = listOf<Transition>(
            Transition(s3, s6, Transition.ParseAction.HEIGHT, lhs_aU, lhs_aU, null) { _, _ -> true }
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
            Transition(s6, s4, Transition.ParseAction.HEIGHT, lhs_a, lhs_a, null) { _, _ -> true },
            Transition(s6, s5, Transition.ParseAction.GRAFT, lhs_U, lhs_U, null) { _, _ -> true }
        )
        assertEquals(expected.size, actual.size)
        for (i in actual.indices) {
            assertEquals(expected[i], actual[i])
        }
    }

    @Test
    fun parse_a() {
        val parser = ScanOnDemandParser(rrs)
        val (sppt, issues) = parser.parseForGoal("S", "a", AutomatonKind.LOOKAHEAD_1)
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
    fun parse_aa() {
        val parser = ScanOnDemandParser(rrs)
        val (sppt, issues) = parser.parseForGoal("S", "aa", AutomatonKind.LOOKAHEAD_1)
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
    fun parse_aaa() {
        val parser = ScanOnDemandParser(rrs)
        val (sppt, issues) = parser.parseForGoal("S", "aaa", AutomatonKind.LOOKAHEAD_1)
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
    fun buildFor() {
        val actual = rrs.buildFor("S", AutomatonKind.LOOKAHEAD_1)
        println(rrs.usedAutomatonToString("S"))

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", 1, false) {

        }

        AutomatonTest.assertEquals(expected, actual)
    }
}