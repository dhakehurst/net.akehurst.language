/**
 * Copyright (C) 2022 Dr. David H. Akehurst (http://dr.david.h.akehurst.net)
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

internal class test_expressions_LLstyle : test_AutomatonAbstract() {

    // S = E
    // E = P
    //   | E '+' P

    // S = E
    // E = P | E1
    // E1 = E o P
    // P = a

    // must be fresh per test or automaton is not correct for different parses (due to caching)
    private val rrs = runtimeRuleSet {
        concatenation("S") { ref("E") }
        choice("E", RuntimeRuleChoiceKind.LONGEST_PRIORITY) {
            ref("P")
            ref("E1")
        }
        concatenation("E1") { ref("E"); literal("o"); ref("P") }
        concatenation("P") { literal("a") }
    }
    private val S = rrs.findRuntimeRule("S")
    private val SM = rrs.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
    private val G = SM.startState.runtimeRules.first()
    private val E = rrs.findRuntimeRule("E")
    private val E1 = rrs.findRuntimeRule("E1")
    private val P = rrs.findRuntimeRule("P")
    private val o = rrs.findRuntimeRule("'o'")
    private val a = rrs.findRuntimeRule("'a'")


    private val lhs_a = SM.createLookaheadSet(false, false, false, setOf(a))


    @Test
    override fun firstOf() {
        listOf(
            Triple(RP(G, 0, SOR), lhs_U, LHS(a)),       // G = . S
            Triple(RP(G, 0, EOR), lhs_U, LHS(UP)),      // G = S .
            Triple(RP(S, 0, SOR), lhs_U, LHS(a)),       // S = . ABC
            Triple(RP(S, 0, EOR), lhs_U, LHS(UP)),      // S = ABC .
            Triple(RP(S, 1, SOR), lhs_U, LHS(a)),       // S = . ABD
            Triple(RP(S, 1, EOR), lhs_U, LHS(UP)),      // S = ABD .

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
            WidthInfo(RP(a, 0, EOR), lhs_a.part)
        )
        assertEquals(expected.size, actual.size)
        for (i in 0 until actual.size) {
            assertEquals(expected[i], actual[i])
        }
    }

    @Test
    fun s1_heightOrGraftInto_s0() {
        val s0 = SM.startState
        val s1 = SM.states[listOf(RP(a, 0, EOR))]
        val actual = s1.heightOrGraftInto(s0).toList()

        val expected = listOf(
            HeightGraftInfo(
                listOf(G, S),
                listOf(RP(E, 0, SOR), RP(E, 0, SOR)),
                listOf(RP(E, 0, 1), RP(E, 0, 1)),
                lhs_a.part,
                setOf(LHS(UP))
            )
        )
        assertEquals(expected, actual)

    }

    @Test
    fun s0_transitions() {
        val s0 = SM.startState
        val s1 = SM.states[listOf(RP(a, 0, EOR))]
        val actual = s0.transitions(null)
        val expected = listOf<Transition>(
            Transition(s0, s1, Transition.ParseAction.WIDTH, lhs_a, LookaheadSet.EMPTY, null) { _, _ -> true },
            //    Transition(s0, s2, Transition.ParseAction.WIDTH, lhs_bcU, LookaheadSet.EMPTY, null) { _, _ -> true }
        )
        assertEquals(expected, actual)
    }

    @Test
    fun automaton_parse_aoa() {
        val parser = ScanOnDemandParser(rrs)
        val (sppt, issues) = parser.parseForGoal("S", "aoaoaoa", AutomatonKind.LOOKAHEAD_1)
        println(parser.runtimeRuleSet.usedAutomatonToString("S"))
        assertNotNull(sppt)
        assertEquals(0, issues.size)
        assertEquals(1, sppt.maxNumHeads)
        val actual = parser.runtimeRuleSet.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)

        val expected = automaton(rrs, AutomatonKind.LOOKAHEAD_1, "S", 0, false) {
            val s0 = state(RP(G, 0, SOR))     /* G = . S   */
            val s1 = state(RP(a, 0, EOR))     /* a .       */
            val s2 = state(RP(P, 0, EOR))     /* P = a . */
            val s3 = state(RP(E, 0, EOR))     /* E = P . */
            val s4 = state(RP(S, 0, EOR))     /* S = E . */
            val s5 = state(RP(E1, 0, 1)) /* E1 = E . o P */
            val s6 = state(RP(o, 0, EOR))     /* o . */
            val s7 = state(RP(E1, 0, 2)) /* E1 = E o . P */
            val s8 = state(RP(E1, 0, EOR))    /* E1 = E o P . */
            val s9 = state(RP(E, 1, EOR))     /* E = E1 . */
            val s10 = state(RP(G,0,EOR))      /* G = S . */

            transition(null, s0, s1, WIDTH, setOf(UP,o), setOf(), null)
            transition(listOf(s0,s7), s1, s2, HEIGHT, setOf(UP), setOf(UP), listOf(RP(P, 0, SOR)))
            //transition(s0, s2, s3, WIDTH, setOf(c, d), setOf(), null)
            //transition(s2, s3, s4, GRAFT, setOf(c, d), setOf(UP), listOf(RP(ABC, 0, 1), RP(ABD, 0, 1)))
            //transition(s0, s4, s5, WIDTH, setOf(UP), setOf(), null)
            //transition(s0, s4, s6, WIDTH, setOf(UP), setOf(), null)
            //transition(s4, s5, s7, GRAFT, setOf(UP), setOf(UP), listOf(RP(ABC, 0, 2)))
           // transition(s0, s7, s8, HEIGHT, setOf(UP), setOf(UP), listOf(RP(S, 0, 0)))
           // transition(s0, s8, s9, GRAFT, setOf(UP), setOf(UP), listOf(RP(G, 0, 0)))
           // transition(null, s9, s9, GOAL, setOf(), setOf(), null)
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