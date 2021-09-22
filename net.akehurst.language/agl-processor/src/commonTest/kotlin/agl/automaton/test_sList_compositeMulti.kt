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
import net.akehurst.language.api.processor.AutomatonKind
import kotlin.test.Test
import kotlin.test.assertEquals

internal class test_sList_compositeMulti : test_Abstract() {

    // S = [nl / ';']*
    // nl = N cnm
    // cnm = cn*
    // cn = ',' N
    // N = "[0-9]+"

    private companion object {

        val rrs = runtimeRuleSet {
            sList("S", 0, -1, "nl", "SMI")
            concatenation("nl") { ref("N"); ref("cnm") }
            multi("cnm", 0, -1, "cn")
            concatenation("cn") { ref("CMR"); ref("N"); }
            literal("CMR", ",")
            literal("SMI", ";")
            pattern("N", "[0-9]+")
        }

        val S = rrs.findRuntimeRule("S")
        val Se = S.rhs.items[RuntimeRuleItem.SLIST__EMPTY_RULE]
        val SM = rrs.fetchStateSetFor(S, AutomatonKind.LOOKAHEAD_1)
        val G = SM.startState.runtimeRules.first()
        val nl = rrs.findRuntimeRule("nl")
        val cnm = rrs.findRuntimeRule("cnm")
        val cn = rrs.findRuntimeRule("cn")
        val c = rrs.findRuntimeRule("CMR")
        val i = rrs.findRuntimeRule("SMI")
        val n = rrs.findRuntimeRule("N")

        val lhs_n = SM.createLookaheadSet(setOf(n))
        val lhs_c = SM.createLookaheadSet(setOf(c))
        val lhs_i = SM.createLookaheadSet(setOf(i))
        val lhs_nU = SM.createLookaheadSet(setOf(n, UP))
        val lhs_ciU = SM.createLookaheadSet(setOf(c, i, UP))

        val s0 = SM.startState
        val s1 = SM.states[listOf(RP(n, 0, EOR))]
        val s2 = SM.states[listOf(RP(Se, 0, EOR))]
    }

    @Test
    override fun firstOf() {
        listOf(
            Triple(RP(G, 0, SOR), lhs_U, setOf(n, UP)), // G = . S
            Triple(RP(G, 0, EOR), lhs_U, setOf(UP)),        // G = S .
            Triple(RP(S, OLI, SOR), lhs_U, setOf(UP)),          // So0 = . [nl . / ';']*
            Triple(RP(S, OLS, PLS), lhs_U, setOf(UP)),          // So0 =  nl . [nl . / ';']*
            Triple(RP(S, OLI, PLI), lhs_U, setOf(UP)),          // So0 = nl ';' . [nl . / ';']*
            Triple(RP(S, OLI, EOR), lhs_U, setOf(UP)),          // So0 = [nl . / ';']* .
            Triple(RP(S, OLE, SOR), lhs_U, setOf(UP)),          // So2 = . E
            Triple(RP(S, OLE, EOR), lhs_U, setOf(UP)),          // So2 = E .
            Triple(RP(nl, 0, SOR), lhs_U, setOf(n)),        // nl = . N cmn
            Triple(RP(nl, 0, 1), lhs_U, setOf(UP)),    // nl = N . cnm
            Triple(RP(cnm, OMI, SOR), lhs_U, setOf(UP)),      // cnm = . cn*
            Triple(RP(cnm, OMI, PMI), lhs_U, setOf(UP)),      // cnm = cn . cn*
            Triple(RP(cnm, OMI, EOR), lhs_U, setOf(UP)),      // cnm = cn* .
            Triple(RP(cnm, OME, SOR), lhs_U, setOf(UP)),      // cnm = . E
            Triple(RP(cnm, OME, SOR), lhs_U, setOf(UP)),      // cnm = E .
            Triple(RP(cn, 0, SOR), lhs_U, setOf(UP))        // cn = . ',' N
        ).testAll { rp, lhs, expected ->
            val actual = SM.buildCache.firstOf(rp, lhs)
            assertEquals(expected, actual, "failed $rp")
        }
    }

    @Test
    override fun s0_widthInto() {
        val s0 = SM.startState
        val actual = s0.widthInto(null).toList()

        val expected = listOf(
            WidthInfo(RP(n, 0, EOR), lhs_ciU),
            WidthInfo(RP(Se, 0, EOR), lhs_U)
        )
        assertEquals(expected.size, actual.size)
        for (i in 0 until actual.size) {
            assertEquals(expected[i], actual[i])
        }
    }

    @Test
    fun calcClosure_G_0_0() {
        val cl_G = ClosureItemLC1(null, RP(G, 0, 0), RP(G, 0, 0), lhs_U)
        val cl_G_So0 = ClosureItemLC1(cl_G, RP(S, 0, 0), RP(S, OLS, PLS), lhs_i)
        val cl_G_So0_nl = ClosureItemLC1(cl_G_So0, RP(nl, 0, 0), RP(nl, 0, 1), lhs_i)
        val cl_G_So1 = ClosureItemLC1(cl_G, RP(S, 1, 0), RP(G, 0, 0), lhs_U)
TODO()
        //val actual = SM.buildCache.calcClosure(ClosureItemLC1(null, RP(G, 0, 0), null, lhs_U))
       // val expected = setOf(
       //         cl_G, cl_G_So0, cl_G_So1
       // )
       // assertEquals(expected, actual)
    }

    @Test
    fun s0_transitions() {
        val actual = s0.transitions(null)

        val expected = listOf(
                Transition(s0, s1, Transition.ParseAction.WIDTH, lhs_ciU, LookaheadSet.EMPTY, null) { _, _ -> true },
                Transition(s0, s2, Transition.ParseAction.WIDTH, lhs_U, LookaheadSet.EMPTY, null) { _, _ -> true }
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
                    listOf(RP(S, 0, 0)),
                    listOf(RP(S, 0, EOR)),
                    lhs_U,
                    lhs_U
                )
        )
        assertEquals(expected, actual)

    }

    @Test
    fun s1_transitions_s0() {
        val actual = s1.transitions(s0)

        val expected = listOf<Transition>(
                Transition(s1, s2, Transition.ParseAction.HEIGHT, lhs_U, LookaheadSet.UP, null) { _, _ -> true }
//                Transition(s1, s3, Transition.ParseAction.GRAFT, lhs_aU, null) { _, _ -> true }
        )
        assertEquals(expected.size, actual.size)
        for (i in actual.indices) {
            assertEquals(expected[i], actual[i])
        }
    }

}